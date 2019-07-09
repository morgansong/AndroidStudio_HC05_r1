package com.mth.btclient;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.*;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import java.io.*;
import java.util.UUID;

//import android.view.Menu;            //如使用菜单加入此三包
//import android.view.MenuInflater;
//import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

	private final static int REQUEST_CONNECT_DEVICE = 1;    //宏定义查询设备句柄

	private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB";   //SPP服务UUID号

	private InputStream is;    //输入流，用来接收蓝牙数据
	private EditText edit0;    //发送数据输入句柄
	private TextView tv_in;       //接收数据显示句柄
	private ScrollView sv;      //翻页句柄
	private String smsg = "";    //显示用数据缓存
	private String fmsg = "";    //保存用数据缓存

	public String filename=""; //用来保存存储的文件名
	BluetoothDevice _device = null;     //蓝牙设备
	BluetoothSocket _socket = null;      //蓝牙通信socket
	boolean bRun = true;
	boolean bThread = false;

	private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();    //获取本地蓝牙适配器，即蓝牙设备

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);   //设置画面为主画面 main.xml

				        /* 解决兼容性问题，6.0以上使用新的API*/
		final int MY_PERMISSION_ACCESS_COARSE_LOCATION = 11;
		final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
			if(this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
				requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},MY_PERMISSION_ACCESS_COARSE_LOCATION);
				Log.e("11111","ACCESS_COARSE_LOCATION");
			}
			if(this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
				requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSION_ACCESS_FINE_LOCATION);
				Log.e("11111","ACCESS_FINE_LOCATION");
			}
		}
		sv = (ScrollView)findViewById(R.id.ScrollView01);  //得到翻页句柄
		tv_in = (TextView) findViewById(R.id.in);      //得到数据显示句柄

		//如果打开本地蓝牙设备不成功，提示信息，结束程序
		if (_bluetooth == null){
			Toast.makeText(this, "无法打开手机蓝牙，请确认手机是否有蓝牙功能！", Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		// 设置设备可以被搜索
		new Thread(){
			public void run(){
				if(_bluetooth.isEnabled()==false){
					_bluetooth.enable();
				}
			}
		}.start();
	}


	//接收活动结果，响应startActivityForResult()
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch(requestCode){
			case REQUEST_CONNECT_DEVICE:     //连接结果，由DeviceListActivity设置返回
				// 响应返回结果
				if (resultCode == Activity.RESULT_OK) {   //连接成功，由DeviceListActivity设置返回
					// MAC地址，由DeviceListActivity设置返回
					String address = data.getExtras()
							.getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
					// 得到蓝牙设备句柄
					_device = _bluetooth.getRemoteDevice(address);

					// 用服务号得到socket
					try{
						_socket = _device.createRfcommSocketToServiceRecord(UUID.fromString(MY_UUID));
					}catch(IOException e){
						Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
					}
					//连接socket
					Button btn = (Button) findViewById(R.id.BtnConnect);
					try{
						_socket.connect();
						Toast.makeText(this, "连接"+_device.getName()+"成功！", Toast.LENGTH_SHORT).show();
						btn.setText("断开");
					}catch(IOException e){
						try{
							Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
							_socket.close();
							_socket = null;
						}catch(IOException ee){
							Toast.makeText(this, "连接失败！", Toast.LENGTH_SHORT).show();
						}

						return;
					}

					//打开接收线程
					try{
						is = _socket.getInputStream();   //得到蓝牙数据输入流
					}catch(IOException e){
						Toast.makeText(this, "接收数据失败！", Toast.LENGTH_SHORT).show();
						return;
					}
					if(bThread==false){
						readThread.start();
						bThread=true;
					}else{
						bRun = true;
					}
				}
				break;
			default:break;
		}
	}

	//接收数据线程
	Thread readThread=new Thread(){

		public void run(){
			int num = 0;
			byte[] buffer = new byte[1024];
			byte[] buffer_new = new byte[1024];
			int i = 0;
			int n = 0;
			bRun = true;
			//接收线程
			while(true){
				try{
					while(is.available()==0){
						while(bRun == false){}
					}
					while(true){
						if(!bThread)//跳出循环
							return;

						num = is.read(buffer);         //读入数据
						n=0;

						String s0 = new String(buffer,0,num);
						fmsg+=s0;    //保存收到数据
						for(i=0;i<num;i++){
							if((buffer[i] == 0x0d)&&(buffer[i+1]==0x0a)){
								buffer_new[n] = 0x0a;
								i++;
							}else{
								buffer_new[n] = buffer[i];
							}
							n++;
						}
						String s = new String(buffer_new,0,n);
						smsg+=s;   //写入接收缓存
						if(is.available()==0)break;  //短时间没有数据才跳出进行显示
					}
					//发送显示消息，进行显示刷新
					handler.sendMessage(handler.obtainMessage());
				}catch(IOException e){
				}
			}
		}
	};

	//消息处理队列
	Handler handler= new Handler(){
		public void handleMessage(Message msg){
			super.handleMessage(msg);
			tv_in.setText(smsg);   //显示数据
			sv.scrollTo(0,tv_in.getMeasuredHeight()); //跳至数据最后一页
		}
	};

	//关闭程序掉用处理部分
	public void onDestroy(){
		super.onDestroy();
		if(_socket!=null)  //关闭连接socket
			try{
				_socket.close();
			}catch(IOException e){}
		//	_bluetooth.disable();  //关闭蓝牙服务
	}

	//菜单处理部分
  /* @Override
    public boolean onCreateOptionsMenu(Menu menu) {//建立菜单
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }*/

  /*  @Override
    public boolean onOptionsItemSelected(MenuItem item) { //菜单响应函数
        switch (item.getItemId()) {
        case R.id.scan:
        	if(_bluetooth.isEnabled()==false){
        		Toast.makeText(this, "Open BT......", Toast.LENGTH_LONG).show();
        		return true;
        	}
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.quit:
            finish();
            return true;
        case R.id.clear:
        	smsg="";
        	ls.setText(smsg);
        	return true;
        case R.id.save:
        	Save();
        	return true;
        }
        return false;
    }*/

	//连接按键响应函数
	public void onConnectButtonClicked(View v){

		if(_bluetooth.isEnabled()==false){  //如果蓝牙服务不可用则提示
			Toast.makeText(this, " 打开蓝牙中...", Toast.LENGTH_LONG).show();
			return;
		}


		//如未连接设备则打开DeviceListActivity进行设备搜索
		Button btn = (Button) findViewById(R.id.BtnConnect);
		if(_socket==null){
			Intent serverIntent = new Intent(this, DeviceListActivity.class); //跳转程序设置
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);  //设置返回宏定义
		}
		else{
			//关闭连接socket
			try{
				bRun = false;
				Thread.sleep(2000);

				is.close();
				_socket.close();
				_socket = null;

				btn.setText("连接");
			}catch(IOException e){}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		return;
	}

	//保存按键响应函数
	public void onSaveButtonClicked(View v){
		Save();
	}

	//清除按键响应函数
	public void onClearButtonClicked(View v){
		smsg="";
		fmsg="";
		tv_in.setText(smsg);
		return;
	}

	//退出按键响应函数
	public void onQuitButtonClicked(View v){

		//---安全关闭蓝牙连接再退出，避免报异常----//
		if(_socket!=null){
			//关闭连接socket
			try{
				bRun = false;
				Thread.sleep(2000);

				is.close();
				_socket.close();
				_socket = null;
			}catch(IOException e){}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		finish();
	}

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Log.v("MTH_Morgan","Permission: "+permissions[0]+ "was "+grantResults[0]);
            //resume tasks needing this permission
        }
    }
	//保存功能实现
	private void Save() {
		//显示对话框输入文件名
		LayoutInflater factory = LayoutInflater.from(MainActivity.this);  //图层模板生成器句柄
		final View DialogView =  factory.inflate(R.layout.sname, null);  //用sname.xml模板生成视图模板
		new AlertDialog.Builder(MainActivity.this)
				.setTitle("文件名")
				.setView(DialogView)   //设置视图模板
				.setPositiveButton("确定",
						new DialogInterface.OnClickListener() //确定按键响应函数
						{
							public void onClick(DialogInterface dialog, int whichButton){
								EditText text1 = (EditText)DialogView.findViewById(R.id.sname);  //得到文件名输入框句柄
								filename = text1.getText().toString();  //得到文件名

								try{
									if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){  //如果SD卡已准备好

										filename =filename+".txt";   //在文件名末尾加上.txt
										File sdCardDir = Environment.getExternalStorageDirectory().getAbsoluteFile();  //得到SD卡根目录
										Log.i("MTH_Morgan", "sdCardDir: " + sdCardDir.toString());


										File BuildDir = new File(sdCardDir+File.separator+ "mth_logging_data");   //打开data目录，如不存在则生成
										Log.i("MTH_Morgan", "BuildDir: "+ BuildDir.toString());

                                                  BuildDir.mkdirs();


										//File saveFile =new File(BuildDir, filename);  //新建文件句柄，如已存在仍新建文档
										//Log.i("MTH_Morgan", "folder saveFile "+ saveFile.toString());

										File saveFile = new File(sdCardDir, filename);
										Log.i("MTH_Morgan", "filename saveFile: "+ saveFile.toString());

										Log.i("MTH_Morgan", "file exist? "+ saveFile.exists());

										if(saveFile.exists()){
											Log.d("MTH_Morgan", "The file " + saveFile.getName() + " exists!");
										}else{
											Log.d("MTH_Morgan", "The file no longer exists!");
											saveFile.createNewFile();
											Log.d("MTH_Morgan", "create the new file");
										}
										Log.i("MTH_Morgan", "1");

										Log.i("MTH_Morgan", fmsg);

										FileOutputStream stream = new FileOutputStream(saveFile);  //打开文件输入流

										stream.write(fmsg.getBytes());
										Log.i("MTH_Morgan", "2");

										stream.close();
										Toast.makeText(MainActivity.this, "存储成功！\n\r"+saveFile, Toast.LENGTH_LONG).show();
										Log.i("MTH_Morgan", "3");
									}else{
										Toast.makeText(MainActivity.this, "没有存储卡！", Toast.LENGTH_LONG).show();
									}

								}catch(IOException e){
									Log.i("MTH_Morgan", e.toString());

								}



							}
						})
				.setNegativeButton("取消",   //取消按键响应函数,直接退出对话框不做任何处理
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
							}
						}).show();  //显示对话框
	}
}