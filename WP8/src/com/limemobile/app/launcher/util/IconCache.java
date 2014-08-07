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

package com.limemobile.app.launcher.util;

import java.util.HashMap;
import java.util.Iterator;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.limemobile.app.launcher.LauncherApplication;
import com.limemobile.app.launcher.entity.ApplicationInfo;
import com.limemobile.app.launcher.util.Utilities.BubbleText;

/**
 * Cache of application icons.  Icons can be made from any thread.
 */
public class IconCache {
    private static final String TAG = "Launcher.IconCache";

    private static final int INITIAL_ICON_CACHE_CAPACITY = 50;
    private static final int INITIAL_HOTSEAT_ICON_CASE_CAPACITY = 4;

    private static class CacheEntry {
        public Bitmap icon;
        public String title;
        public Bitmap titleBitmap;
    }

    private final Bitmap mDefaultIcon;
    private final LauncherApplication mContext;
    private final PackageManager mPackageManager;
    private final BubbleText mBubble;
    private final HashMap<ComponentName, CacheEntry> mCache =
            new HashMap<ComponentName, CacheEntry>(INITIAL_ICON_CACHE_CAPACITY);
    
    private final HashMap<String, CacheEntry> mHotseatCache =
            new HashMap<String, CacheEntry>(INITIAL_HOTSEAT_ICON_CASE_CAPACITY);

    public IconCache(LauncherApplication context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
        mBubble = new Utilities.BubbleText(context);
        mDefaultIcon = makeDefaultIcon();
    }

    private Bitmap makeDefaultIcon() {
        Drawable d = mPackageManager.getDefaultActivityIcon();
        Bitmap b = Bitmap.createBitmap(Math.max(d.getIntrinsicWidth(), 1),
                Math.max(d.getIntrinsicHeight(), 1),
                Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        d.setBounds(0, 0, b.getWidth(), b.getHeight());
        d.draw(c);
        return b;
    }

    /**
     * Remove any records for the supplied ComponentName.
     */
    public void remove(ComponentName componentName) {
        synchronized (mCache) {
            mCache.remove(componentName);
        }
    }

    /**
     * Empty out the cache.
     */
    public void flush() {
        synchronized (mCache) {
            Iterator<ComponentName> iterator = mCache.keySet().iterator();
            while(iterator.hasNext()){
                CacheEntry entry = mCache.get(iterator.next());
                if (entry.icon != null && !entry.icon.isRecycled())
                    entry.icon.recycle();
                if (entry.titleBitmap != null && !entry.titleBitmap.isRecycled())
                    entry.titleBitmap.recycle();
                entry = null;
            }
            mCache.clear();
        }
        
        synchronized (mHotseatCache) {
            Iterator<String> iterator = mHotseatCache.keySet().iterator();
            while (iterator.hasNext()) {
                CacheEntry entry = mHotseatCache.get(iterator.next());
                if (entry.icon != null && !entry.icon.isRecycled())
                    entry.icon.recycle();
                if (entry.titleBitmap != null && !entry.titleBitmap.isRecycled())
                    entry.titleBitmap.recycle();
                entry = null;
            }
            mHotseatCache.clear();
        }
    }
    
    public void flushHotseat() {
        synchronized (mHotseatCache) {
            Iterator<String> iterator = mHotseatCache.keySet().iterator();
            while (iterator.hasNext()) {
                CacheEntry entry = mHotseatCache.get(iterator.next());
                if (entry.icon != null && !entry.icon.isRecycled())
                    entry.icon.recycle();
                if (entry.titleBitmap != null && !entry.titleBitmap.isRecycled())
                    entry.titleBitmap.recycle();
                entry = null;
            }
            mHotseatCache.clear();
        }
    }

    /**
     * Fill in "application" with the icon and label for "info."
     */
    public void getTitleAndIcon(ApplicationInfo application, ResolveInfo info) {
        synchronized (mCache) {
            CacheEntry entry = cacheLocked(application.componentName, info);
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

            CacheEntry entry = cacheLocked(component, resolveInfo);
            return entry.icon;
        }
    }

    public Bitmap getIcon(ComponentName component, ResolveInfo resolveInfo) {
        synchronized (mCache) {
            if (resolveInfo == null || component == null) {
                return null;
            }

            CacheEntry entry = cacheLocked(component, resolveInfo);
            return entry.icon;
        }
    }

    public boolean isDefaultIcon(Bitmap icon) {
        return mDefaultIcon == icon;
    }

    private CacheEntry cacheLocked(ComponentName componentName, ResolveInfo info) {
        CacheEntry entry = mCache.get(componentName);
        if (entry == null) {
            entry = new CacheEntry();

            mCache.put(componentName, entry);

            entry.title = info.loadLabel(mPackageManager).toString();
            if (entry.title == null) {
                entry.title = info.activityInfo.name;
            }
            entry.icon = Utilities.createIconBitmap(
                    info.activityInfo.loadIcon(mPackageManager), mContext);
        }
        return entry;
    }
    
    public Bitmap getReflectionIcon(Intent intent) {
        synchronized (mCache) {
            final ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, 0);
            
            CacheEntry entry = mHotseatCache.get(intent.toString());
            if (entry == null) {
                entry = new CacheEntry();

                mHotseatCache.put(intent.toString(), entry);

                if (resolveInfo != null) {
                    entry.title = resolveInfo.loadLabel(mPackageManager).toString();
                    if (entry.title == null) {
                        entry.title = resolveInfo.activityInfo.name;
                    }
                }
                
                Bitmap temp = (resolveInfo == null) ? mDefaultIcon : Utilities.createIconBitmap(
                        resolveInfo.activityInfo.loadIcon(mPackageManager), mContext);
                
                entry.icon = createReflection(temp, 0,
                        temp.getHeight() - temp.getHeight() / 2, temp.getWidth(),
                        temp.getHeight() / 2);
                
                temp.recycle();
            }
            return entry.icon;
        }
    }
    
    public Bitmap getReflectionIcon(Intent intent, Drawable icon) {
        synchronized (mCache) {
            final ResolveInfo resolveInfo = mPackageManager.resolveActivity(intent, 0);
            
            Configuration config = mContext.getResources().getConfiguration();
            
            CacheEntry entry = mHotseatCache.get(intent.toString());
            if (entry == null) {
                entry = new CacheEntry();

                mHotseatCache.put(intent.toString(), entry);

                if (resolveInfo != null) {
                    entry.title = resolveInfo.loadLabel(mPackageManager).toString();
                    if (entry.title == null) {
                        entry.title = resolveInfo.activityInfo.name;
                    }
                }
                
                Bitmap temp = (resolveInfo == null) ? mDefaultIcon : Utilities.createIconBitmap(icon, mContext);
                
                if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    entry.icon = createReflection(temp, temp.getWidth() - temp.getWidth() * 2 / 4, 0, temp.getWidth() * 2 / 4, 0);
                } else {
                    entry.icon = createReflection(temp, 0,
                            temp.getHeight() - temp.getHeight() / 2, 0,
                            temp.getHeight() / 2);
                }
                
                temp.recycle();
            }
            return entry.icon;
        }
    }
    
    public static Bitmap createReflection(Bitmap bitmap, int reflectionX, int reflectionY,
            int reflectionWidth, int reflectionHeight) {
        return createReflection(bitmap, reflectionX, reflectionY,
            reflectionWidth, reflectionHeight, 90);
    }
    
    public static Bitmap createReflection(Bitmap bitmap, int reflectionX, int reflectionY,
            int reflectionWidth, int reflectionHeight, int aplha) {

        Bitmap bitmapWithReflection = null;
        if (bitmap != null) {
            try {
                boolean landscape = reflectionWidth > 0;
                int space = landscape ? 5 : 0;
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();

                Matrix matrix = new Matrix();
                if (landscape)
                    matrix.preScale(-1, 1);
                else
                    matrix.preScale(1, -1);

                Bitmap reflectionImage = Bitmap.createBitmap(bitmap, reflectionX, reflectionY,
                        landscape ? reflectionWidth : width,
                                landscape ? height : reflectionHeight, matrix, false);
                bitmapWithReflection = Bitmap.createBitmap(width + reflectionWidth + (landscape ? space : 0), height + reflectionHeight + (landscape ? 0 : space),
                        Config.ARGB_8888);
                bitmapWithReflection.setDensity(Bitmap.DENSITY_NONE);

                Canvas canvasTemp = new Canvas(bitmapWithReflection);

                Paint paint = new Paint();

                Rect src = new Rect(0, 0, width, height);
                RectF dst = new RectF(src);
                canvasTemp.drawBitmap(bitmap, src, dst, paint);

                paint.setAlpha(aplha);
                src.left = 0;
                src.top = 0;
                src.right = reflectionImage.getWidth();
                src.bottom = reflectionImage.getHeight();

                if (landscape) {
                    dst.left = width + space;
                    dst.right = dst.left + reflectionWidth;
                } else {
                    dst.top = height + space;
                    dst.bottom = dst.top + reflectionHeight;
                }
                canvasTemp.drawBitmap(reflectionImage, src, dst, paint);
                reflectionImage.recycle();

                reflectionImage = null;
                matrix = null;
                paint = null;
                canvasTemp = null;
            } catch (OutOfMemoryError e) {
            }

            return bitmapWithReflection;
        } else {
            return null;
        }
    }
}
