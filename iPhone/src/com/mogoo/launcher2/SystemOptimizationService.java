package com.mogoo.launcher2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ActivityManager;
import android.app.Service;
import android.app.ActivityManager.MemoryInfo;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.IBinder;
import android.provider.Browser;
import android.util.Log;

/**
 * ϵͳ�ڴ��Ż�����
 * �����ڵ��ڴ�ʱ�������÷���
 * @author xiejianxiong 
 * @date 2012-4-9
 *
 */
public class SystemOptimizationService extends Service{

	private static String TAG = "SystemOptimizationService";
	private static ArrayList<String> proccessNames;
	private static ArrayList<String> protectApps;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void onCreate() {
		// TODO Auto-generated method stub
		proccessNames = getProccessNames();
        protectApps = getProtectApps();
		super.onCreate();
	}
	
	@Override
	public void onStart(Intent intent, int startId) {
		// TODO Auto-generated method stub
		synchronized (this) {
			startOptimization();
			if(getAvailMemory() <= 24){				
				startOptimization();
			}
		}
		
		super.onStart(intent, startId);
	}
	
    /**
     * ��ݰ������жϸó����Ƿ���Ա�Kill
     * @param packageName
     * @param paramContext
     * @return
     */
    private boolean iskill(String packageName){
    	
      PackageManager pm = getApplicationContext().getPackageManager();
      try
      {
        packageName = pm.getApplicationInfo(packageName, PackageManager.GET_DISABLED_COMPONENTS).packageName;
        int result = pm.checkPermission("android.permission.BIND_DEVICE_ADMIN", packageName);
        if (result != 0){
        	if(!isImportance(packageName)){
        		return true;
        	}
        }
      }
      catch (Exception localException)
      {
        System.out.println("i have trouble when judgekill");
        localException.printStackTrace();
      }
      return false;
    }
    
    /**
     * ��ʼ�Ż��ڴ�
     */
    private void startOptimization(){
    	
    	Context context = getApplicationContext();
    	
    	
    	long killBeforeMemory = getAvailMemory();
//    	Log.i(TAG, "xjx--before availMemory:"+getAvailMemory()+" MB/"+getTotalMemory()+" MB");
    	
    	ActivityManager activityManager = (ActivityManager)this.getSystemService(Context.ACTIVITY_SERVICE);
        List<RunningAppProcessInfo> runApps = activityManager.getRunningAppProcesses();
        
        int length = runApps.size();
        for(int i=0; i<length; i++){
        	
        	if(runApps.get(i).importance > RunningAppProcessInfo.IMPORTANCE_SERVICE){
        		
        		String pkgs[] = runApps.get(i).pkgList;
	        	for(String packageName: pkgs){
	        		if(iskill(packageName)){
	        			Log.i(TAG, "xjx--kill packageName:"+packageName);
	        			activityManager.killBackgroundProcesses(packageName);
	        		}
	        	}
        	}
        }
        
        Browser.clearHistory(context.getContentResolver());
        Browser.clearSearches(context.getContentResolver());
//      new SearchRecentSuggestions(context, "browser", SearchRecentSuggestions.QUERIES_PROJECTION_DATE_INDEX).clearHistory();
        
        long releaseCount = 0;
        releaseCount = getAvailMemory() - killBeforeMemory;
        
        Log.i(TAG, "xjx--after availMemory:"+getAvailMemory()+" MB/"+getTotalMemory()+" MB");
        Log.i(TAG, "xjx--release memory:"+ releaseCount+" MB");
        
    }
    /**
     * �����ܱ�����Ӧ��
     * @return
     */
    private ArrayList<String> getProtectApps(){
        ArrayList<String> arryList = new ArrayList<String>();
        arryList.add("android.process.acore");
        arryList.add("android.process.media");
        arryList.add("com.google.process.gapps");
        arryList.add("com.android.alarmclock");
        arryList.add("com.android.mms");
        arryList.add("com.google.android.gm");
        arryList.add("com.android.phone");
        arryList.add("com.android.providers.telephony");
        arryList.add("com.android.contacts");
        arryList.add("com.android.providers.contacts");
        arryList.add("com.android.launcher");
        arryList.add("com.android.providers.applications");
        arryList.add("com.android.providers.media");
        arryList.add("com.android.providers.downloads");
        arryList.add("com.google.android.providers.gmail");
        arryList.add("com.android.voicedialer");        
        arryList.add("com.android.bluetooth");
        arryList.add("com.android.settings");
        arryList.add("com.android.providers.userdictionary");
        arryList.add("com.android.providers.drm");
        arryList.add("com.svox.pico");
        arryList.add("com.android.providers.downloads");
        arryList.add("com.android.googlesearch");
        arryList.add("com.mogoo.launcher");
        
        return arryList;
    }
    
    /**
     * ��ȡ��ؽ�̵����
     * @return
     */
    private ArrayList<String> getProccessNames(){
	    	Context context = getApplicationContext();
	    	Intent intent = null;    	   
    	    List<ResolveInfo> listmain = null;
    	    PackageManager localPackageManager = context.getPackageManager();
    	    
    	    intent = new Intent("android.intent.action.MAIN");
    	    intent.addCategory("android.intent.category.LAUNCHER");
    	    listmain = localPackageManager.queryBroadcastReceivers(intent, 512);    	    
    	    
    	    intent = new Intent("com.android.launcher.action.UNINSTALL_SHORTCUT");    	    
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512)) ;    	    
    	    
    	    intent = new Intent("android.provider.Telephony.SMS_RECEIVED");
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));    	    
    	    
    	    intent = new Intent("android.intent.action.NEW_OUTGOING_CALL");    	    
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));    	    
    	    
    	    intent = new Intent("android.intent.action.WALLPAPER_CHANGED");    	    
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));    	    
    	    
    	    intent = new Intent("android.intent.action.ACTION_POWER_CONNECTED");    	    
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));    	    
    	    
    	    intent = new Intent("android.intent.action.PROVIDER_CHANGED");
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));    	    
    	    
    	    intent = new Intent("android.intent.action.ACTION_SHUTDOWN");
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));    	    

    	    intent = new Intent("android.intent.action.GTALK_CONNECTED");
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));
    	    
    	    intent = new Intent("android.intent.action.SCREEN_OFF");
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));    	    
    	    
    	    intent = new Intent("android.intent.action.INPUT_METHOD_CHANGED");
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));    	    
    	    
    	    intent = new Intent("android.intent.action.PACKAGE_DATA_CLEARED");
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));    	    
    	    
    	    intent = new Intent("android.intent.action.REBOOT");
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));    	    
    	    
    	    intent = new Intent("android.intent.action.CALL");
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));    	    
    	        	    
    	    intent = new Intent("android.intent.action.TIMEZONE_CHANGED");
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));    	    
    	    
    	    intent = new Intent("android.intent.action.TIME_SET");
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));    	    
    	    
    	    intent = new Intent("android.intent.action.TIME_TICK");
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));    	    
    	    
    	    intent = new Intent("android.intent.action.DATE_CHANGED");
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));
    	    
    	    //��appwidget���
    	    intent = new Intent("android.appwidget.action.APPWIDGET_UPDATE");
    	    listmain.addAll(localPackageManager.queryBroadcastReceivers(intent, 512));
    	    
    	    ArrayList<String> proccessNames = new ArrayList<String>();
    	    for(int i=0; i<listmain.size(); i++){    	    	
    	    	String name = listmain.get(i).activityInfo.processName;
    	    	proccessNames.add(name);
    	    }
    	    return proccessNames;
    }
    
    /**
     * �ж�ĳ��Ӧ���Ƿ���Ҫ
     * @return
     */
    private boolean isImportance(String packageName){
    	if(!proccessNames.contains(packageName) && ! protectApps.contains(packageName)){
    		return false;
    	}
    	else{    		
    		return true;
    	}
    }
    
    /**
     * ��ȡ���ڴ�Ĵ�С
     * @param context
     * @return
     */
     
     private long getTotalMemory(){
         String str1 = "/proc/meminfo";// ϵͳ�ڴ���Ϣ�ļ� 
         String str2;
         String[] arrayOfString;
         long initial_memory = 0;

         try 
         {
             FileReader localFileReader = new FileReader(str1);
             BufferedReader localBufferedReader = new BufferedReader(
             localFileReader, 8192);
             str2 = localBufferedReader.readLine();// ��ȡmeminfo��һ�У�ϵͳ���ڴ��С 

             arrayOfString = str2.split("\\s+");
             for (String num : arrayOfString) {
                 Log.i(str2, num + "\t");
             }

             initial_memory = Integer.valueOf(arrayOfString[1]).intValue() * 1024;// ���ϵͳ���ڴ棬��λ��KB������1024ת��ΪByte 
             localBufferedReader.close();

         } catch (IOException e) {
         }
         //return Formatter.formatFileSize(context, initial_memory);// Byteת��ΪKB����MB���ڴ��С��� 
         return initial_memory/(1024*1024);
     }
     
     /**
      * ��ȡ��ǰϵͳ�����ڴ�Ĵ�С
      * @param context
      * @return
      */
     private long getAvailMemory(){
    	 Context context = getApplicationContext();
    	 
         // ��ȡandroid��ǰ�����ڴ��С 
         ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
         MemoryInfo mi = new MemoryInfo();
         am.getMemoryInfo(mi);        
         //return Formatter.formatFileSize(context, mi.availMem);// ����ȡ���ڴ��С��� 
         return mi.availMem/(1024*1024);
     }

}
