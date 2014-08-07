package com.mogoo.launcher2.lockScreen;
///*
// * Copyright (C), 2005-2011, Motone Tech. Co., Ltd.
// * FileName: LockScreenService.java
// * Author: 邓丽霞      
// * Description: 如果锁屏服务被关闭，则重新启动
// * Version:  1.0
//* Date:2011.3.25
// * Function List:   
// *   1. onCreate  初始化锁屏服务监听
// * History:        
// *     <author>   <time>   <version >   <desc>
// *    
//*/
//package com.mogoo.launcher2.lockScreen;
//
//import android.app.Service;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.os.IBinder;
//
//public class LockScreenServiceListener extends Service {
//	private static String LOCKSCREENSERVICE_CLOSED = "lockScreenServiceClose";
//	@Override
//	public IBinder onBind(Intent arg0) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	 @Override
//	 public void onCreate()
//	 {
//		super.onCreate();
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(LOCKSCREENSERVICE_CLOSED);
//        this.registerReceiver(mLsClosedReceiver, filter);
//	 }
//	 @Override
//	 public void onDestroy ()
//	 {
//		  super.onDestroy();
//		  unregisterReceiver(mLsClosedReceiver);
//	 }
//	 //如果锁屏服务被关闭，则重新启动
//	 private BroadcastReceiver mLsClosedReceiver = new BroadcastReceiver()
//	 {
//			@Override
//			public void onReceive(Context context, Intent intent) {
//				// TODO Auto-generated method stub
//				String action = intent.getAction();
//				if(action.equals(LOCKSCREENSERVICE_CLOSED))
//				{
//					Intent service = new Intent();
//					service.setAction("android.intent.action.LOCKSCREENSERVICE");
//					context.startService(service);
//				}
//			}
//	    	
//		};
//}
