/**  
 * 文 件 名:  MT_UncaughtExceptionHandler.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：  邓丽霞                    
 * 版    本:  1.0  
 * 创建时间:   2011-4-14
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-4-14       邓丽霞       1.0          1.0 Version  
 */        
package com.mogoo.launcher2.restore;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.mogoo.launcher2.LauncherApplication;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;

import java.lang.Thread.UncaughtExceptionHandler;

public class Mogoo_UncaughtExceptionHandler implements UncaughtExceptionHandler {
    private static String TAG = "Mogoo_UncaughtExceptionHandler";
    private final Mogoo_RestoreController mRestoreController;
    private Context context;
    
    public Mogoo_UncaughtExceptionHandler(Mogoo_RestoreController controller,Context context)
    {
    	mRestoreController = controller;
    	this.context = context;
    }
	public void uncaughtException(Thread thread, Throwable ex) {
		// TODO Auto-generated method stub
		if(Mogoo_GlobalConfig.LOG_DEBUG)
		{
			Log.d(TAG,"getClass: "+ ex.getClass().toString());
	        Throwable throwable = ex.getCause();
	        if(throwable != null)
	        {
			    Log.d(TAG, "getCause: "+throwable.toString());
	        }
	        String message = ex.getMessage();
	        if(message != null)
	        {
			    Log.d(TAG, "getMessage: "+message);
	        }
		}
        //MT_RestoreController restoreController = MT_RestoreController.getInstance();
        Class exceptionClass = ex.getClass();
        Throwable causeThrowable = ex.getCause();
        mRestoreController.restoreData(exceptionClass);
        while(causeThrowable != null)
        {
            Class causeClass = causeThrowable.getClass();
            mRestoreController.restoreData(causeClass);
            causeThrowable = causeThrowable.getCause();
    		if(Mogoo_GlobalConfig.LOG_DEBUG)
    		{
    			Log.d(TAG,"causeClass: "+ causeClass.toString());
    		}
        }
		if(Mogoo_GlobalConfig.LOG_DEBUG)
		{
			Log.d(TAG,"begin sleeping");
		}
        SharedPreferences settings = context.getSharedPreferences(LauncherApplication.PREFERENCES, 0);
        if(settings != null)
        {
        	Editor editor = settings.edit();
        	editor.putBoolean(LauncherApplication.RESTORE, true);
        	editor.commit();
        }
		//重启设备
//		rebootSystem();
        //Sleep一会后结束程序  
        try {  
            Thread.sleep(1000);  
        } catch (InterruptedException e) {  
            e.printStackTrace();
        }  
        Log.e(TAG, ex.toString(), ex);
        android.os.Process.killProcess(android.os.Process.myPid());  
        System.exit(10);  
	}

//	private void rebootSystem()
//	{
//		Intent intent = new Intent(Intent.ACTION_REBOOT);
//		intent.putExtra("nowait", 1);
//		intent.putExtra("interval", 1);
//		intent.putExtra("window", 0);
//		context.sendBroadcast(intent);
//	}
}
