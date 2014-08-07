/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mogoo.launcher2;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;

import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_BitmapUtils;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Cache of application icons.  Icons can be made from any thread.
 */
public abstract class IconCache {
    private static final String TAG = "Launcher.IconCache";

    //------------- alter by huangyue -------------------
    //将以下变量范围有private 改为 protected    
    protected static final int INITIAL_ICON_CACHE_CAPACITY = 50;
    public static final String FOLDER_FLAG = "isFolder";
    private Handler handler = new Handler();
    
    // Change  from private to protected of all
    protected static class CacheEntry {
        public CacheEntry() {}
        public Bitmap icon;
        public String title;
        public Bitmap titleBitmap;
    }

    protected final Bitmap mDefaultIcon;
    protected final LauncherApplication mContext;
    protected final PackageManager mPackageManager;
    protected final Utilities.BubbleText mBubble;
    protected final HashMap<ComponentName, CacheEntry> mCache =
            new HashMap<ComponentName, CacheEntry>(INITIAL_ICON_CACHE_CAPACITY);

    //------------ end ----------------------
    
    public IconCache(LauncherApplication context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
        mBubble = new Utilities.BubbleText(context);
        mDefaultIcon = makeDefaultIcon();
    }

    private Bitmap makeDefaultIcon() {
//        Drawable d = mPackageManager.getDefaultActivityIcon();
//        Bitmap b = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1),
//                Math.max(d.getIntrinsicHeight(), 1),
//                Bitmap.Config.ARGB_8888);
//        Canvas c = new Canvas(b);
//        d.setBounds(0, 0, b.getWidth(), b.getHeight());
//        d.draw(c);
//        return b;
      Drawable d = mPackageManager.getDefaultActivityIcon();
      Bitmap b = Mogoo_BitmapUtils.drawable2Bitmap(d,Mogoo_GlobalConfig.getIconWidth(),
    		  Mogoo_GlobalConfig.getIconHeight(),new Rect(0,0,Mogoo_GlobalConfig.getIconWidth(),
    	    		  Mogoo_GlobalConfig.getIconHeight()));
      return b;
    }

    /**
     * Remove any records for the supplied ComponentName.
     */
    public void remove(ComponentName componentName) {
        synchronized (mCache) {
        	final CacheEntry entry = mCache.remove(componentName);
//        	if(entry.icon != null){
//        		entry.icon.recycle();
//        	}
        	handler.postDelayed(new Runnable() {
				public void run() {
		        	if(entry != null && entry.icon != null){
		        		entry.icon.recycle();
		        	}
				}
			}, 5000);
        }
    }

    /**
     * Empty out the cache.
     */
    public void flush() {
        synchronized (mCache) {
        	mCache.clear();
        	Iterator<ComponentName> iterator = mCache.keySet().iterator();
        	while(iterator.hasNext()){
            	CacheEntry entry = mCache.remove(iterator.next());
            	if(entry.icon != null){
            		entry.icon.recycle();
            	}
            	
            	entry = null;
        	}
        }
    }

    /**
     * Fill in "application" with the icon and label for "info."
     */
    public void getTitleAndIcon(ApplicationInfo application, ResolveInfo info) {
        synchronized (mCache) {
            CacheEntry entry = this.cacheLocked(application.componentName, info);
            if (entry.titleBitmap == null) {
                entry.titleBitmap = mBubble.createTextBitmap(entry.title);
            }

            application.title = entry.title;
            application.titleBitmap = entry.titleBitmap;
            application.iconBitmap = entry.icon;
        }
    }

    public Bitmap getIcon(Intent intent) {
        synchronized (mCache) {
            final ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, 0);
            ComponentName component = intent.getComponent();

            if (resolveInfo == null || component == null) {
                return mDefaultIcon;
            }

            CacheEntry entry = this.cacheLocked(component, resolveInfo);
            return entry.icon;
        }
    }

    public Bitmap getIcon(ComponentName component, ResolveInfo resolveInfo) {
        synchronized (mCache) {
            if (resolveInfo == null || component == null) {
                return null;
            }

            CacheEntry entry = this.cacheLocked(component, resolveInfo);
            return entry.icon;
        }
    }
    //--------------  delete by huangyue ------------------
    public LauncherApplication getContext(){
        return mContext;
    }
    //------------------------------ end ----------------------

    /**
     * 从缓存获得制式图标或者且当缓存无制式图标时填充图标到缓存 
     * @ author: 黄悦
     *@return
     */
    protected abstract CacheEntry cacheLocked(ComponentName componentName, ResolveInfo info);
}
