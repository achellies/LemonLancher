/**  
 * 文 件 名:  ContentListener.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：  黄悦                     
 * 版    本:  1.0  
 * 创建时间:   2011-1-25
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-1-25        黄悦       1.0          1.0 Version  
 */

package com.mogoo.launcher2;

import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_BitmapCache;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.provider.CallLog;
import android.provider.CallLog.Calls;
import android.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class Mogoo_ContentListener extends ContentObserver {
    private static final String TAG = "Launcher.Mogoo_ContentListener";
    
    public static final int SMS_INDEX = 0;
    public static final int TELEPHONE_INDEX = 1;
    public static final int MARKET_INDEX = 2;
    
    private int[] mCounts = new int[3];

    private Launcher launcher;

    private Mogoo_BitmapCache mCache;

    private HashMap<Integer, HashMap<ComponentName, Mogoo_BubbleTextView>> listenerViews = new HashMap<Integer, HashMap<ComponentName, Mogoo_BubbleTextView>>();

    // denglixia add 2011.4.14
    private MarketBroadcastReceiver mMarketBroadcastReceiver = null;

    // denglixia add end 2011.4.14
    public Mogoo_ContentListener(Handler handler, Launcher cxt) {
        super(handler);
        this.launcher = cxt;
        // denglixia add 2011.4.14
        mMarketBroadcastReceiver = new MarketBroadcastReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(MarketBroadcastReceiver.ACTION_APP_UPDATE);
        
        try{
            cxt.registerReceiver(mMarketBroadcastReceiver, filter);
        }catch (Exception e) {
            Log.w(TAG, e);
        }
        // denglixia add end 2011.4.14
    }

    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        updateShorcutshowNum();
    }

    /**
     * 检查是否是监听类型 @ author: 黄悦
     * 
     * @param type
     * @return
     */
    public boolean isListenType(int type) {
        return type == LauncherSettings.Favorites.APP_TYPE_MARKET
                || type == LauncherSettings.Favorites.APP_TYPE_SMS
                || type == LauncherSettings.Favorites.APP_TYPE_TELEPHONE;
    }

    /**
     * 增加监视对象 @ author: 黄悦
     * 
     * @param key
     * @param item
     */
    public void addItem(int key, Mogoo_BubbleTextView item) {
        // if(!isListenType(key)){
        // return;
        // }

        if (!listenerViews.containsKey(key)) {
            listenerViews.put(key, new HashMap<ComponentName, Mogoo_BubbleTextView>());
        }

        ShortcutInfo info = (ShortcutInfo) item.getTag();

        this.listenerViews.get(key).put(info.getIntent().getComponent(), item);

        info = null;
    }

    /**
     * 删除监视对象 @ author: 黄悦
     * 
     * @param key
     * @param item
     */
    public void removeItem(int key, Mogoo_BubbleTextView item) {
        if (!listenerViews.containsKey(key)) {
            return;
        }
        ShortcutInfo info = (ShortcutInfo) item.getTag();
        this.listenerViews.get(key).remove(info.getIntent().getComponent());

        info = null;
    }

    /**
     * 获得监听种类的对应数量 @ author: 黄悦
     * 
     * @param type
     * @return
     */
    public int getCountByType(int type) {
        Cursor cursor = null;
        int sum = 0;
        switch (type) {
            case LauncherSettings.Favorites.APP_TYPE_SMS:
                cursor = getSMSCursor();
                break;
            case LauncherSettings.Favorites.APP_TYPE_TELEPHONE:
                cursor = getPhoneCursor();
                break;
            // case LauncherSettings.Favorites.APP_TYPE_EMAIL:
            // cursor = getEmailCursor();
            // break;
        }

        if (cursor != null) {
            sum = cursor.getCount();
            cursor.close();
            cursor = null;
        }

        return sum;
    }

    public void setCache(Mogoo_BitmapCache mCache) {
        this.mCache = mCache;
    }

    /*************************************
     * 查询并设置 未读邮件， 未接电话， 未读短信 等的数据， 用于显示在图标的右上角 方法的功能描述 @ author: fancheng
     */
    private void updateShorcutshowNum() {

        if (Mogoo_GlobalConfig.LOG_DEBUG) {
            Log.d(TAG, "updateShorcutshowNum...");
        }

        Cursor cursor = null;
        try {
            // 读取收件箱中指定号码的短信

            // cursor = launcher.managedQuery(Uri.parse("content://sms/inbox"),
            // new String[] {
            // "read"
            // }, " read=?", new String[] {
            // "0"
            // }, "date desc");

            cursor = getSMSCursor();

            mCounts[SMS_INDEX] = setCountIcon(cursor, LauncherSettings.Favorites.APP_TYPE_SMS);
            
            // add by 张永辉 2010-01-07
            if (cursor != null) {
                cursor.close();
                cursor = null;
              }

            cursor = getPhoneCursor();

            mCounts[TELEPHONE_INDEX] = setCountIcon(cursor, LauncherSettings.Favorites.APP_TYPE_TELEPHONE);

            // cursor = getEmailCursor();
            //
            // setCountIcon(cursor, LauncherSettings.Favorites.APP_TYPE_EMAIL);
            settingNum(LauncherSettings.Favorites.APP_TYPE_MARKET, mCounts[MARKET_INDEX]);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // add by 张永辉 2010-01-07
          if (cursor != null) {
                cursor.close();
                cursor = null;
            }
            // end
        }
    }

    // private Cursor getEmailCursor() {
    // Cursor cursor;
    // cursor = launcher.getContentResolver()
    // .query(Uri.parse("content://com.android.email.provider/message"), null,
    // null,
    // null, null);
    // return cursor;
    // }

    private Cursor getPhoneCursor() {
		/**
		 * Warning: Do not call close() on a cursor obtained using this method,
		 * because the activity will do that for you at the appropriate time.
		 * However, if you call stopManagingCursor(Cursor) on a cursor from a
		 * managed query, the system will not automatically close the cursor
		 * and, in that case, you must call close().
		 */
//        Cursor cursor = launcher.managedQuery(CallLog.Calls.CONTENT_URI, new String[] {
//            Calls.TYPE
//        }, " type=? and new=?", new String[] {
//                Calls.MISSED_TYPE + "", "1"
//        }, "date desc");
        Cursor cursor = launcher.getContentResolver().query(CallLog.Calls.CONTENT_URI, new String[] {
                Calls.TYPE
            }, " type=? and new=?", new String[] {
                    Calls.MISSED_TYPE + "", "1"
            }, "date desc");
        return cursor;
    }

    private Cursor getSMSCursor() {
        Cursor cursor = null;
		/**
		 * Warning: Do not call close() on a cursor obtained using this method,
		 * because the activity will do that for you at the appropriate time.
		 * However, if you call stopManagingCursor(Cursor) on a cursor from a
		 * managed query, the system will not automatically close the cursor
		 * and, in that case, you must call close().
		 */
        /*
         * cursor = launcher.managedQuery(Uri.parse("content://sms/inbox"), new
         * String[] { "seen" }, " seen=?", new String[] { "0" }, "date desc");
         */
//        cursor = launcher.managedQuery(Uri.parse("content://sms/inbox"), new String[] {
//            "read"
//        }, " read=?", new String[] {
//            "0"
//        }, "date desc");
        cursor = launcher.getContentResolver().query(Uri.parse("content://sms/inbox"), new String[] {
            "read"
        }, " read=?", new String[] {
            "0"
        }, "date desc");
        return cursor;
    }

    private int setCountIcon(Cursor cursor, int key) {
        int num = 0;
        if (cursor != null) {
            num = cursor.getCount();

            settingNum(key, num);
        }
        
        return num;
    }

    private void settingNum(int key, int num) {
        if (Mogoo_GlobalConfig.LOG_DEBUG) {
            Log.d(TAG, "setCountIcon---" + num);
         }

        if (listenerViews.containsKey(key)) {
            Collection<Mogoo_BubbleTextView> views = listenerViews.get(key).values();

           for (Mogoo_BubbleTextView vv : views) {
              if (vv != null) {
                    vv.stopVibrate();
                    vv.setCountIcon(mCache, num, key);
                    vv.startVibrate(mCache, 0);
                    vv.invalidate();
                }
            }
        }
    }

    public HashMap<ComponentName, Mogoo_BubbleTextView> getCellEntryByType(int type) {
        return listenerViews.get(type);
    }
    
    public int[] getCountsArray(){
        return mCounts;
    }

    public void unRegisterMarketReceiver()
    {
    	if(mMarketBroadcastReceiver != null)
    	{
    	    launcher.unregisterReceiver(mMarketBroadcastReceiver);
    	}
    }
    public class MarketBroadcastReceiver extends BroadcastReceiver {
        private static final String ACTION_APP_UPDATE = "com.motone.market.app_update";
        private static final String KEY_APP_UPDATE = "app_update";

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();
            if (ACTION_APP_UPDATE.equals(action)) {
                int appCount = intent.getIntExtra(KEY_APP_UPDATE, 0);
                settingNum(LauncherSettings.Favorites.APP_TYPE_MARKET, appCount);
                
                mCounts[MARKET_INDEX] = appCount;
            }
        }

    }

}
