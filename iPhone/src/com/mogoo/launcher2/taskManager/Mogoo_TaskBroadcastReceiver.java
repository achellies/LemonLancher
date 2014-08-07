package com.mogoo.launcher2.taskManager;

import com.mogoo.launcher2.config.Mogoo_GlobalConfig;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class Mogoo_TaskBroadcastReceiver extends BroadcastReceiver{
	private static final String TAG = "Launcher.Mogoo_TaskBroadcastReceiver";
	
	private Mogoo_TaskManager mTaskManagerDialog;
	
	private Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			String[] a=(String[])msg.obj;
            if(!a[0].equals("")){
            	mTaskManagerDialog.displayMusicName(a[0]);
            }
            if(!a[1].equals("")){
            	mTaskManagerDialog.setPlayState(a[1]);
            }
			
		};
	};
	@Override
	public void onReceive(Context context, Intent intent) {
		// TODO Auto-generated method stub
		Message msg = new Message();
		String musicName = intent.getStringExtra("musicName");
		String state = intent.getStringExtra("state");
		String[] a=new String[2];
		if(musicName == null){
			a[0] = "";
		}else{
			a[0] = musicName;
		}
		if(state==null){
			a[1] = "";
		}else{
			a[1] = state;
		}

		
		msg.obj =a;
		if(Mogoo_GlobalConfig.LOG_DEBUG){
			Log.d(TAG, "--------musicName---------:"+intent.getStringExtra("musicName")+"---");
			Log.d(TAG, "--------state---------:"+intent.getStringExtra("state")+"---");
		}
		mHandler.sendMessage(msg);
	}

	public void setTaskManager(Mogoo_TaskManager taskManagerDialog) {
		this.mTaskManagerDialog = taskManagerDialog;
	}
	
	
}

