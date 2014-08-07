package com.mogoo.launcher2.taskManager.ui;

///*
//* Copyright (C), 2005-2011, Motone Tech. Co., Ltd.
//* FileName: Mogoo_TaskMusicPanel.java
//* Author: 袁业奔      
//* Description: 任务栏中的音乐播放器控制面板
//* Version:  1.0
//* Date:2011-9-1
//*/


import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mogoo.launcher.R;

public class Mogoo_TaskMusicPanel extends LinearLayout{
	
	private final static String ACTION_INTENT_TASKMUSIC="com.mogoo.taskmusic"; 
	
	private final static String TYPE="type";
	
	private final static int PREVIOUS=0;
	private final static int PLAY=1;
	private final static int PAUSE=2;
	private final static int RESTART=3;
	private final static int NEXT=4;
	private final static int STATE=5;
	
	private final static long SLEEP_TIME=600;
	
	private String mMusicName;
	
	private TextView tv_Name;
    private ImageView bt_lock;
    private ImageView bt_previous;
    private ImageView bt_play;
    private ImageView bt_next;
    private ImageView bt_main;
    private Context mcontext;
    private Resources resources;
  
	public Mogoo_TaskMusicPanel(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		LayoutInflater mInflater=LayoutInflater.from(context);
		mInflater.inflate(R.layout.mogoo_task_music_panel, this,true);
		mcontext=context;
		resources=context.getResources();
		
 		findViews();
		setlistener();
		
		getState();
		
		getLockState();
	}	
	public void setPlayState(String state){
		if(state.equals("play")){
			bt_play.setTag(0);
	  	    bt_play.setImageDrawable(resources.getDrawable(R.drawable.mogoo_task_music_pause));
			
		}else if(state.equals("pause")){
			bt_play.setTag(1);
        	bt_play.setImageDrawable(resources.getDrawable(R.drawable.mogoo_task_music_play));
		}
	}
	public void displayMusicName(String musicName){
		mMusicName=musicName;
		tv_Name.setText(musicName);
//		tv_Name.setBackgroundColor(Color.RED);
	}
	//发送获取播放器状态的广播STATE=5
    private void getState(){
        Intent intent=new Intent(ACTION_INTENT_TASKMUSIC);
        intent.putExtra(TYPE, STATE);
        mcontext.sendBroadcast(intent);
    }
    //获取重力感应器状态1-启用，0-禁用
    private void getLockState(){
    	int lockState=0;
    	try {
    		lockState=Settings.System.getInt(mcontext.getContentResolver(), 
    				Settings.System.ACCELEROMETER_ROTATION);
		} catch (Exception e) {
			// TODO: handle exception
		}
    	
    	if(lockState==1){
    		 bt_lock.setTag(0);
    	  	 bt_lock.setImageDrawable(resources.getDrawable(R.drawable.mogoo_task_music_unlock));
    	}else if(lockState==0){
    		bt_lock.setTag(1);
          	bt_lock.setImageDrawable(resources.getDrawable(R.drawable.mogoo_task_music_lock));
    	}
    }
    
    private void findViews(){
  	    bt_lock=(ImageView)findViewById(R.id.mogooTaskMusicLockbt);
  	    bt_lock.setTag(0);
  	    bt_lock.setImageDrawable(resources.getDrawable(R.drawable.mogoo_task_music_unlock));
  	
  	    bt_previous=(ImageView)findViewById(R.id.mogooTaskMusicPreviousbt);
  	
  	    bt_play=(ImageView)findViewById(R.id.mogooTaskMusicPlaybt);
  	    bt_play.setTag(1);
  	    bt_play.setImageDrawable(resources.getDrawable(R.drawable.mogoo_task_music_play));
  	
  	    bt_next=(ImageView)findViewById(R.id.mogooTaskMusicNextbt);
  	
  	    bt_main=(ImageView)findViewById(R.id.mogooTaskMusicMainbt);
  	
  	    tv_Name=(TextView)findViewById(R.id.mogooTaskMusicDisplaytv);
     }
  
  private void setlistener(){
      bt_lock.setOnClickListener(new OnClickListener() {
			
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
                if((Integer)bt_lock.getTag()==1){           	
                	bt_lock.setTag(0);
                  	bt_lock.setImageDrawable(resources.getDrawable(R.drawable.mogoo_task_music_unlock));
                  	tv_Name.setText(resources.getString(R.string.music_panel_unlocked));
                  	
                  	Settings.System.putInt(mcontext.getContentResolver(),
                            Settings.System.ACCELEROMETER_ROTATION,
                            1);
                  	
                  	new Thread() {
						public void run() {
							try {
								Thread.sleep(SLEEP_TIME);
								handle.sendEmptyMessage(0);
							} catch (Exception e) {
								// TODO: handle exception
							}
						}
					}.start();
                }
                else{
                	bt_lock.setTag(1);
                  	bt_lock.setImageDrawable(resources.getDrawable(R.drawable.mogoo_task_music_lock));
                  	tv_Name.setText(resources.getString(R.string.music_panel_locked));
                  	
                  	Settings.System.putInt(mcontext.getContentResolver(),
                            Settings.System.ACCELEROMETER_ROTATION,
                            0);
                  	
                  	new Thread() {
						public void run() {
							try {
								Thread.sleep(SLEEP_TIME);
								handle.sendEmptyMessage(0);
							} catch (Exception e) {
								// TODO: handle exception
							}
						}
					}.start();
                }           
			}
		});
//      
      bt_previous.setOnClickListener(new OnClickListener() {
			
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
                Intent intent=new Intent(ACTION_INTENT_TASKMUSIC);
                intent.putExtra(TYPE, PREVIOUS);
                mcontext.sendBroadcast(intent);
			}
		});
      //注册双击事件监听器
//      bt_previous.setOnTouchListener(new previousClick());
      //
      bt_play.setOnClickListener(new OnClickListener() {
			
			public void onClick(View arg0) {
				Intent intent=new Intent();
				//1为播放，0为停止
                if((Integer)bt_play.getTag()==1){
                	//发送 播放音乐的广播通知
    				intent.setAction(ACTION_INTENT_TASKMUSIC);
    				intent.putExtra(TYPE, PLAY);
    				mcontext.sendBroadcast(intent);
                	bt_play.setTag(0);
                	bt_play.setImageDrawable(resources.getDrawable(R.drawable.mogoo_task_music_pause));
                }else{
                	//发送 暂停音乐的广播通知
    				intent.setAction(ACTION_INTENT_TASKMUSIC);
    				intent.putExtra(TYPE, PAUSE);
    				mcontext.sendBroadcast(intent);
                	bt_play.setTag(1);
                	bt_play.setImageDrawable(resources.getDrawable(R.drawable.mogoo_task_music_play));
                }
				
			}
		});
      bt_next.setOnClickListener(new OnClickListener() {
			
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
                Intent intent=new Intent(ACTION_INTENT_TASKMUSIC);
                intent.putExtra(TYPE, NEXT);
                mcontext.sendBroadcast(intent);
			}
		});
      
      bt_main.setOnClickListener(new OnClickListener() {
			
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent intent= new Intent();
				intent.setAction("android.intent.action.MUSIC_PLAYER");
				mcontext.startActivity(intent);
			}
		});
  }
    private Handler handle = new Handler(){
    	public void handleMessage(android.os.Message msg) {
    		tv_Name.setText(mMusicName);    		
		};
    };
}
