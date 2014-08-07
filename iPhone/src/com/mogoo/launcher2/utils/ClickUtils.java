package com.mogoo.launcher2.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.NetworkInfo.State;
import android.os.Handler;
//import android.os.Mogoo_DataSyncManager;
//import android.os.MessageListener;
//import android.ibe.Mogoo_IbeCommonApi;
import android.util.Log;

import com.mogoo.launcher2.LauncherModel;
import com.mogoo.launcher2.LauncherProvider;
import com.mogoo.launcher2.LauncherSettings;

/**
 * 点击发送工具
 * @author 黄悦
 * 2011-10-14
 */
public class ClickUtils {
	private final static String URL = "http://192.168.0.107/AD/sendApp.action";
	
	private static final int ACTIVE_SEND_SUCCESS = 1;
	private static final int ACTIVE_SEND_FAILED = 2;
	
	public static final String ACTION_APP_CLICK = "com.mogoo.action.APP_CLICK";
	public static final String ACTION_APP_CLICK_RESULT = "com.mogoo.action.APP_CLICK_RESULT";
	
	/**
	 * 初次点击工具
	 * @author 黄悦
	 * 2011-10-14
	 */
	public static void firstClick(final Context context, final Intent intent){
		Runnable runable = new Runnable() {
			public void run() {
				LauncherModel.addClickItemToDatabase(context, intent.getComponent().getPackageName());
				
//				if(!isConnected(context)){
//					return;
//				}
				
				Cursor cursor = null;

				try {
					final ContentResolver cr = context.getContentResolver();
					cursor = cr.query(LauncherSettings.Favorites.ACTIVE_URI_NO_NOTIFICATION,
							null, "isUpload=?", new String[]{"false"}, null);
					
					if(cursor.getCount() == 0 ){
						cursor.close();
						return;
					}
					
					int packageIndex = cursor.getColumnIndex(LauncherSettings.Favorites.PACKAGE);
					int activeDateIndex = cursor.getColumnIndex(LauncherSettings.Favorites.ACTIVE_DATE);
					
					
					JSONObject json = new JSONObject();
					JSONArray jsonArray = new JSONArray();
					final int[] ids = new int[cursor.getCount()];
					int index = 0;
					
					while(cursor.moveToNext()){
						JSONObject temp = new JSONObject();
						temp.put("package", cursor.getString(packageIndex));
						temp.put("reqTime", cursor.getLong(activeDateIndex));
						temp.put("screen", "1");
						jsonArray.put(temp);
						ids[index++] = cursor.getInt(0);
					};
					
					json.put("body", jsonArray);
					
					
					//edit by yeben 2012-2-17
//					mogooDataSync.registerCallback(callback);//注册接口
//					mogooDataSync.submitAppMessage(json.toString());//上传数据接口，json为要上传的json数据。			
					Intent itent = new Intent();
					itent.setAction(ACTION_APP_CLICK);
					itent.putExtra("MESSAGE", json.toString());
					itent.putExtra("IDS", ids);
					context.sendBroadcast(itent);
					//end
					
				} catch (Exception e) {
					Log.e("ClickUtils", null, e);
				} finally {
					if (cursor != null) {
						cursor.close();
					}
				}
			}
		};
		
		Thread t = new Thread(runable);
		t.start();
	}

	private static void updateDate2Loaded(final ContentResolver cr, int[] ids) {
		final ContentValues values = new ContentValues();
		values.put(LauncherSettings.Favorites.IS_UPLOAD, true);

		for (int id : ids) {
			cr.update(LauncherSettings.Favorites.getContentActiveUri(id),
					values, null, null);
		}
	}
	
    public static boolean isConnected(Context context) {   
        boolean result = false;   
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);   
        State mobile = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState();   
        State wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();   
        result = (mobile == State.CONNECTED || wifi == State.CONNECTED);   
        return result;   
    }   
    
    public static class IBEBroadcastReceiver extends BroadcastReceiver{
		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				ContentResolver cr = context.getContentResolver();
				String action = intent.getAction();
				if(action.equals(ACTION_APP_CLICK_RESULT)){
				    String data = intent.getStringExtra("MESSAGE");
				    int[] ids = intent.getIntArrayExtra("IDS");
					if(data == null){
					    return;
				    }
				
				    String[] sessions = data.split("\\|");
				
				    Log.i("ClickUtils", data);
				
				    if(sessions.length == 2 && "3000".equals(sessions[1])){
					    updateDate2Loaded(cr, ids);
				    }
				}
			} catch (Exception e) {
				// TODO: handle exception
			}
		}
    	
    }
}
