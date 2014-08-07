package com.mogoo.launcher2.utils;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Process;

/**
 * 检测工具类
 * @author 张永辉
 * @Date 2012-3-26
 */
public class CheckUtils {
	
	private static final String PREF_NAME = "ibe_tag" ;
	private static final String KEY_TAG = "tag" ;
	
	/**
	 * 检测，如果检测失败，则终止应用
	 * @author 张永辉
	 * @date 2012-3-26
	 */
	public static void check(Context context){
		SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) ;
		Editor editor = sp.edit() ;
		editor.putBoolean(KEY_TAG, true) ;
		editor.commit() ;
		new CheckThread(context).start() ;
	}
	
	/**
	 * 检测是否已经检测过
	 * @author 张永辉
	 * @date 2012-3-26
	 * @param context
	 */
	public static void reCheck(Context context){
		if(!isCheck(context)){
			Process.killProcess(Process.myPid()) ;
		}
	}
	
	/**
	 * 判断是否检测过
	 * @author 张永辉
	 * @date 2012-3-26
	 * @param context
	 * @return
	 */
	private static boolean isCheck(Context context){
		SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE) ;
		return sp.getBoolean(KEY_TAG, false) ;
	}
	
	private static class CheckThread extends Thread{
		private Context context ;
		
		private List<String> list  = new ArrayList<String>() ;
		
		public CheckThread(Context context){
			this.context = context ;
			
			list.add("com.mogoo.component.ad.AdViewLayout") ;
			list.add("com.mogoo.component.download.MogooDownloadManager") ;
			
			list.add("android.ibe.acp.AppConnectManager") ;
			list.add("android.ibe.acp.AppConnectService") ;
			list.add("android.ibe.base.IbeClientMessage") ;
			list.add("android.ibe.base.function.ad.AdSystem") ;
			//list.add("android.ibe.base.function.appclick.AppClickStatistics") ;
			list.add("android.ibe.base.function.bookmark.BookmarkManager") ;
			list.add("android.ibe.base.function.sales.SalesStatistics") ;
			list.add("android.ibe.base.function.update.UpdateService") ;
			list.add("android.ibe.bmc.BusinessManageService") ;
			list.add("android.ibe.netconnector.HeartBeat") ;
		}
		
		@Override
		public void run() {
			if(list!=null){
				for(String str:list){
					try{
						Class.forName(str);
					}catch(Exception e){
						Process.killProcess(Process.myPid()) ;
					}
				}
			}
		}
	}
}
