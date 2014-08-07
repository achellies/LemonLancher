
package com.mogoo.launcher2.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.TypedValue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.IconCache;
import com.mogoo.launcher2.LauncherApplication;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;

/**
 * application 级生命周期bitmap的缓存工具类
 * 
 * @author 黄悦
 */ 
public class Mogoo_BitmapCache extends IconCache {
    public static final int RECYCLE_RESID_NOMAL = 0;

    public static final int RECYCLE_RESID_SCALE = 1;

    public static final int RECYCLE_COMPONENT_NAME_ALL = -1;

    public static final int RECYCLE_COMPONENT_NAME_NOMAL = 0;

    public static final int RECYCLE_COMPONENT_NAME_REFLECTION = 1;

    public static final int RECYCLE_COMPONENT_NAME_VIBRATION = 2;

    private long memSize;

    private Random random = new Random();

    private LauncherApplication application;

    private final ArrayList<HashMap<Integer, Bitmap>> resIdCache = new ArrayList<HashMap<Integer, Bitmap>>();

    private final ArrayList<HashMap<ComponentName, Bitmap>> componentCache = new ArrayList<HashMap<ComponentName, Bitmap>>();

    private final HashMap<ComponentName, Bitmap[]> vibrationCache = new HashMap<ComponentName, Bitmap[]>(
            INITIAL_ICON_CACHE_CAPACITY);

    private final HashMap<String, Bitmap> mollacCache = new HashMap<String, Bitmap>(
            INITIAL_ICON_CACHE_CAPACITY);

    private final long BITMAP_BUD_MEMERY = 8 * 1024 * 1024;

    private final float MEMERY_LOW_POINT = 0.8f;

    private Matrix matrix;

    public Mogoo_BitmapCache(LauncherApplication context) {
        super(context);
        application = context;
        initCache();
    }

    // /**
    // * 通过图片id获得bitmap
    // * @author 黄悦
    // * @param resId 图片资源id
    // * @return
    // */
    // public Bitmap getBitmap(Context cxt, int resId) {
    // Bitmap bitmap = getBitmapFromCache(resIdCache, resId,
    // RECYCLE_RESID_NOMAL);
    //
    // if(bitmap == null || bitmap.isRecycled()){
    // bitmap = BitmapFactory.decodeResource(cxt.getResources(), resId);
    // addMemSize(bitmap);
    // resIdCache.get(RECYCLE_RESID_NOMAL).put(resId, bitmap);
    // }
    //
    // return bitmap;
    // }

    /**
     * 通过组建名称获得shortcut的对应抖动动画帧数组
     * 
     * @author 黄悦
     * @param componentName
     * @return
     */
    public synchronized Bitmap[] getIconWithAngle(ComponentName componentName, Bitmap drawCache) {
        if (vibrationCache.containsKey(componentName)) {
            return vibrationCache.get(componentName);
        }

        float[] angles = Mogoo_GlobalConfig.getFrameAngle();
        Bitmap[] temp = new Bitmap[angles.length];
        for (int i = 0; i < temp.length; i++) {
            temp[i] = createRotaImg(angles[i], drawCache);
            if(temp[i] == null){
            	clearBitmapArray(temp);
            	return null;
            }
        }

        vibrationCache.put(componentName, temp);
        angles = null;

        return temp;
    }
    
    private void clearBitmapArray(Bitmap[] bitmaps){
    	
    }

    /**
     * 通过组建名称获得shortcut的倒影bitmap
     * 
     * @author 魏景春
     * @param intent
     * @return
     */
    public Bitmap getIconWithReflection(Intent intent) {
        Bitmap bitmap = getBitmapFromCache(componentCache, intent.getComponent(),
                RECYCLE_COMPONENT_NAME_REFLECTION);

        if (bitmap == null || bitmap.isRecycled()) {
            // CacheEntry entry = mCache.get(intent.getComponent());
            bitmap = Mogoo_BitmapUtils.createReflection(getIcon(intent));
            addMemSize(bitmap);
            componentCache.get(RECYCLE_COMPONENT_NAME_REFLECTION)
                    .put(intent.getComponent(), bitmap);
        }

        return bitmap;
    }

    /**
     * 通过组建名称获得shortcut的倒影bitmap
     * 
     * @author 黄悦
     * @param componentName
     * @return
     */
    public Bitmap getIconWithReflection(ComponentName componentName) {
        Bitmap bitmap = getBitmapFromCache(componentCache, componentName,
                RECYCLE_COMPONENT_NAME_REFLECTION);

        if (bitmap == null || bitmap.isRecycled()) {
            CacheEntry entry = mCache.get(componentName);
            bitmap = Mogoo_BitmapUtils.createReflection(entry.icon);
            addMemSize(bitmap);
            componentCache.get(RECYCLE_COMPONENT_NAME_REFLECTION).put(componentName, bitmap);
        }

        return bitmap;
    }

    /**
     * 获得与key相关的bitmap @ author: 黄悦
     * 
     * @param key
     */
    public Bitmap getBitmapByKey(String key, int width, int height, Config config) {
        Bitmap temp = mollacCache.get(key);

        if (temp == null || temp.isRecycled()) {
            temp = Bitmap.createBitmap(width, height, config);
            addMemSize(temp);
            mollacCache.put(key, temp);
        }

        return temp;
    }

    /**
     * 获得指定资源的缩放图片
     * 
     * @author 黄悦
     * @param resId
     * @param width
     * @param height
     * @return
     */
    public Bitmap getScaleBitmap(int resId, int width, int height) {
        Bitmap bitmap = getBitmapFromCache(resIdCache, resId, RECYCLE_RESID_SCALE);

        if (bitmap == null || bitmap.isRecycled()) {
            CacheEntry entry = mCache.get(resId);
            bitmap = Mogoo_BitmapUtils.createScale(entry.icon, width, height);
            addMemSize(bitmap);
            resIdCache.get(RECYCLE_RESID_SCALE).put(resId, bitmap);
        }

        return bitmap;
    }

    /**
     * 用于在制式图标上展示短信，未接电话等数量
     * 
     * @author 黄悦
     * @param count
     * @return
     */
    public Bitmap getDigitalIcon(int count) {
        if (count > 0) {
            if (count < 10) {
                return Mogoo_BitmapUtils.drawIconCountInfo(mContext.getResources().getDrawable(R.drawable.mogoo_noblackicon_1),
                        count + "");
            } else if (count >= 10 && count < 100) {
                return Mogoo_BitmapUtils.drawIconCountInfo(mContext.getResources().getDrawable(R.drawable.mogoo_noblackicon_2),
                        count + "");
            } else if (count < 1000 && count >= 100) {
                return Mogoo_BitmapUtils.drawIconCountInfo(mContext.getResources().getDrawable(R.drawable.mogoo_noblackicon_3),
                        count + "");
            } else {
                return Mogoo_BitmapUtils.drawIconCountInfo(mContext.getResources().getDrawable(R.drawable.mogoo_noblackicon_3),
                        "999");
            }
        }
        return null;
    }

    /**
     * 回收由resId获取的对象 类型有: 未改变bitmap RECYCLE_RESID_NOMAL 缩放bitmap
     * RECYCLE_RESID_SCALE
     * 
     * @author 黄悦
     * @param resId
     * @param recycleType
     */
    public void recycle(int resId, int recycleType) {
        recycleBitmap(resIdCache.get(recycleType), resId);
    }

    /**
     * 回收由ComponentName获取的对象 recycleType类型有: RECYCLE_COMPONENT_NAME_NOMAL:
     * 未改变bitmap RECYCLE_COMPONENT_NAME_REFLECTION: 倒影bitmap
     * RECYCLE_COMPONENT_NAME_ANGLE: 抖动帧组 RECYCLE_COMPONENT_NAME_ALL:
     * 删除所有与名称相关内容
     * 
     * @author 黄悦
     * @param resId
     * @param recycleType
     */
    public void recycle(ComponentName componentName, int recycleType) {
        if (RECYCLE_COMPONENT_NAME_VIBRATION == recycleType) {
            recycleCBitmap(vibrationCache, componentName);
        } else if (RECYCLE_COMPONENT_NAME_ALL == recycleType) {
            recycleAll(componentName);
        } else {
            recycleCBitmap(componentCache.get(recycleType), componentName);
        }
    }

    /**
     * 申请 bitmap 空间回收 @ author: 黄悦
     * 
     * @param bitmapKey
     */
    public void recycle(String bitmapKey) {
        Bitmap temp = mollacCache.remove(bitmapKey);

        if (temp != null) {
            subMemSize(temp);
            temp.recycle();
        }

        temp = null;
    }

    /**
     * 返回已用byte总数 @ author: 黄悦
     * 
     * @return
     */
    public long getMemoryCount() {
        return memSize / 4;
    }

    /**
     * 当前图片缓存剩余内存状态是否为low @ author: 黄悦
     * 
     * @return
     */
    public boolean isLowMemery() {
        return (memSize / 4) / BITMAP_BUD_MEMERY > MEMERY_LOW_POINT;
    }
    
    /**
     * 将图标的获取改成现有的方式
     */
    @Override
    protected CacheEntry cacheLocked(ComponentName componentName, ResolveInfo info) {
        CacheEntry entry = mCache.get(componentName);
        if (entry == null) {
            entry = new CacheEntry();

            mCache.put(componentName, entry);

            entry.title = info.loadLabel(mPackageManager).toString();
            if (entry.title == null) {
                entry.title = info.activityInfo.name;
            }

            // entry.icon =
            // MT_BitmapUtils.createIconBitmap(info.activityInfo.loadIcon(mPackageManager),
            // mContext, application.getModel().isSystemApp(componentName));
            entry.icon = CalendarUtils.getCalendarIcon(info, this);
            if(entry.icon == null)
           {
  	          //modify by weijingchun 2011-12-8
//	            entry.icon = Mogoo_BitmapUtils.createIconBitmap(loadIcon(mPackageManager, info), mContext,
//	                    application.getModel().isSystemApp(componentName));
            	String title = Mogoo_BitmapUtils.getApplicationTitle(componentName);
            	if(title!=null && !title.equals(""))
            	{
                	entry.title = Mogoo_BitmapUtils.getApplicationTitle(componentName);
            	}

	            entry.icon = Mogoo_BitmapUtils.getApplicationIcon(componentName);
	            if(entry.icon == null)
	            {
	          	    Log.d("Mogoo_BitmapCache", "componentName==" + componentName + "  entry.icon is null");
	          	    
	            	entry.icon = Mogoo_BitmapUtils.createIconBitmap(loadIcon(mPackageManager, info), mContext,
		                    application.getModel().isSystemApp(componentName));
	            }else
	            {
	            	Log.d("Mogoo_BitmapCache", "componentName==" + componentName);
	            }
	            //-----------------end---------------
             }
            addMemSize(entry.icon);
        }
        return entry;
    }
    
    protected Bitmap getFolderIcon(ComponentName componentName) {
        CacheEntry entry = mCache.get(componentName);
        if (entry == null) {
//            MT_BitmapUtils.createFolderBitmap(mContext, this);
            return null;
        }
        return entry.icon;
    }

    private void initCache() {
        // 初始化resIdCache 两种类型的缓存区
        resIdCache.add(new HashMap<Integer, Bitmap>());
        resIdCache.add(new HashMap<Integer, Bitmap>());

        // 初始化componentCache 两种类型的缓存区
        componentCache.add(new HashMap<ComponentName, Bitmap>());
        componentCache.add(new HashMap<ComponentName, Bitmap>());
    }

    // 防止缓存类型空指针
    private Bitmap getBitmapFromCache(ArrayList<HashMap<Integer, Bitmap>> cache, int resId, int type) {
        if (cache.get(type) == null) {
            cache.add(type, new HashMap<Integer, Bitmap>());
        }

        return cache.get(type).get(resId);
    }

    // 防止缓存类型空指针
    private Bitmap getBitmapFromCache(ArrayList<HashMap<ComponentName, Bitmap>> cache,
            ComponentName componentName, int type) {
        if (cache.get(type) == null) {
            cache.add(type, new HashMap<ComponentName, Bitmap>());
        }

        return cache.get(type).get(componentName);
    }

    /*
     * 回收resId bitmap处理
     */
    private void recycleBitmap(HashMap<Integer, ?> cache, int id) {
        if (cache == null) {
            return;
        }

        Object obj = cache.remove(id);

        if (obj instanceof Bitmap) {
            subMemSize((Bitmap) obj);
            ((Bitmap) obj).recycle();
        }

        obj = null;
    }

    private void addMemSize(Bitmap bitmap) {
        if (bitmap != null) {
            memSize += bitmap.getHeight() * bitmap.getWidth();
        }
    }

    private void subMemSize(Bitmap bitmap) {
        if (bitmap != null) {
            memSize -= bitmap.getHeight() * bitmap.getWidth();
        }
    }

    /*
     * 回收ComponentName bitmap处理
     */
    private void recycleCBitmap(HashMap<ComponentName, ?> cache, ComponentName componentName) {
        if (cache == null) {
            return;
        }

        Object obj = cache.remove(componentName);

        if (obj instanceof Bitmap[]) {
            for (int i = 0; i < ((Bitmap[]) obj).length; i++) {
                ((Bitmap[]) obj)[i].recycle();
            }
        } else if (obj instanceof Bitmap) {
            ((Bitmap) obj).recycle();
        }

        obj = null;
    }

    private Bitmap createRotaImg(Float angle, Bitmap b) {
        int w = b.getWidth();
        int h = b.getHeight();

        Matrix matrix = new Matrix();
        matrix.setRotate(angle, w / 2, h / 2);
        Bitmap res = null;
        
        try {
        	res = Bitmap.createBitmap(b, 0, 0, w, h, matrix, true);
        	matrix = null;
		} catch (Exception e) {
			return null;
		} catch (OutOfMemoryError e) {
			return null;
		}

        return res;
    }

    /**
     * 通过图片id获得bitmap
     * 
     * @author 黄悦
     * @param resId 图片资源id
     * @return
     */
    public Bitmap getBitmap(int resId) {
        Bitmap bitmap = getBitmapFromCache(resIdCache, resId, RECYCLE_RESID_NOMAL);

        if (bitmap == null || bitmap.isRecycled()) {
            bitmap = Mogoo_BitmapUtils.decodeResource(mContext.getResources(), resId);
            addMemSize(bitmap);
            resIdCache.get(RECYCLE_RESID_NOMAL).put(resId, bitmap);
        }

        return bitmap;
    }

    /**
     * 清空名称对应的位图 @ author: 黄悦
     * 
     * @param name
     */
    private void recycleAll(ComponentName name) {
        recycle(name, RECYCLE_COMPONENT_NAME_VIBRATION);
        for (HashMap<ComponentName, Bitmap> m : componentCache) {
            recycleCBitmap(m, name);
        }
    }
    
    public void recycleAllByType(int type){
    	Set<ComponentName> sets = mCache.keySet();
        if(sets != null){
            Iterator<ComponentName> itr = sets.iterator();
            while(itr.hasNext()){
                recycle(itr.next(), type);
            }
         }
    }
    
    public void recycleAll(){
        Set<ComponentName> sets = mCache.keySet();
        if(sets != null){
            Iterator<ComponentName> itr = sets.iterator();
            while(itr.hasNext()){
                recycleAll(itr.next());
            }
         }
        
        flush();
    }
    
    public Bitmap getIcon(Intent intent){
        ComponentName component = intent.getComponent();
        String str = component.getClassName();
        
        if(str.startsWith(Mogoo_Utilities.DEFAULT_CLASS_NAME_PREFIX)){
            CacheEntry entry = mCache.get(intent.getComponent());
            if(entry == null){
                return null;
            }
            return entry.icon;
        }else{
            return super.getIcon(intent);
        }
    }
    
    /**
     * 
     * 存放图标文件夹缩略图
     * @ author: 黄悦
     *@param bitmap
     *@param intent
     */
    public void putFodlerIcon(Bitmap bitmap, Intent intent){
        CacheEntry entry = new CacheEntry();
        entry.icon = bitmap;
        mCache.put(intent.getComponent(), entry);
    }

    public Drawable loadIcon(PackageManager pm, ResolveInfo info) {
        if (pm == null || info == null) {
            return null;
        }

        ComponentInfo ci = info.activityInfo != null ? info.activityInfo : info.serviceInfo;
        ApplicationInfo appInfo = ci.applicationInfo;
        Drawable icon = null;
        
        try {
            if (info.getIconResource() != 0) {
                Resources r = pm.getResourcesForApplication(appInfo);
                icon = Mogoo_BitmapUtils.createFromResource(r, info.getIconResource());
            } else {
            	  icon= pm.getDefaultActivityIcon();
            }

        } catch (NameNotFoundException e) {
            Log.w("PackageManager", "Failure retrieving resources for" + appInfo.packageName);
        } catch (RuntimeException e) {
            // If an exception was thrown, fall through to return default icon.
            Log.w("PackageManager", "Failure retrieving icon 0x" + Integer.toHexString(info.icon)
                    + " in package " + info.activityInfo.packageName, e);
        }
        
        if(icon == null){
        	icon= pm.getDefaultActivityIcon();
        }

        return icon;
    }

}
