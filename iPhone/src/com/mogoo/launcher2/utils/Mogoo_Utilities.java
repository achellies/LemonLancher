/**  
 * 文 件 名:  MT_Utilities.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：                       
 * 版    本:  1.0  
 * 创建时间:   2010-01-19
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2010-12-14        张永辉       1.0          1.0 Version  
 */  

package com.mogoo.launcher2.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.mogoo.launcher2.IconCache;
import com.mogoo.launcher2.LauncherApplication;
import com.mogoo.launcher2.LauncherSettings;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;

public class Mogoo_Utilities {
    
    private static final String TAG = "Mogoo_Utilities" ;
    
    public static final String DEFAULT_PACKAGE_NAME_PREFIX = "com.motone" ;
    public static final String DEFAULT_CLASS_NAME_PREFIX = "Mogoo_Folder" ;
    /**
     * 
     * 坐标转换
     * @ author: 张永辉  2010-12-28 
     *@param point
     *@return
     */
    public static int [] switchPoint(int [] point){
        if(Mogoo_GlobalConfig.isPortrait()){//如果是竖屏，则不进行坐标转换
            return point ;
        }else{ //如果为横屏，则进行坐标转换
            int x = point[0];
            int y = point[1] ;
            int [] temp  = new int [2];
            temp[0] = (y*Mogoo_GlobalConfig.getWorkspaceLongAxisCellsLand()+x)%Mogoo_GlobalConfig.getWorkspaceLongAxisCellsPort() ;
            temp[1] = (y*Mogoo_GlobalConfig.getWorkspaceLongAxisCellsLand()+x)/Mogoo_GlobalConfig.getWorkspaceLongAxisCellsPort() ;
            return temp ;
        }
    }
    
    /**
     * 
     * @param context
     * @param strs
     */
    public static void sortForStart(Context context, String[] strs){
		Cursor c = context.getContentResolver().query(
				LauncherSettings.Favorites.ACTIVE_URI_NO_NOTIFICATION, null,
				null, null, null);
		try {
			File file = new File(
					"/data/data/com.mogoo.launcher/files/start_mark.bc");
			// 数据库没有的情况
			if (c == null || c.getCount() == 0) {
				if (file.exists()) {
					if (c != null)
						c.close();
					return;
				}
			} else {
				if (c != null)
					c.close();
				return;
			}

			PackageManager pm = context.getPackageManager();
			Intent intent = new Intent(Intent.ACTION_MAIN, null);
			intent.addCategory(Intent.CATEGORY_LAUNCHER);

			List<ResolveInfo> apps = pm.queryIntentActivities(intent, 0);
			ArrayList<String> temps = new ArrayList<String>();
			for (ResolveInfo app : apps) {
				temps.add(PasswordEncryption
						.getMD5Password(app.activityInfo.packageName));
			}

			for (String str : strs) {
				if (str != null && !temps.contains(str.toLowerCase())) {
					LauncherApplication app = (LauncherApplication) context.getApplicationContext();
					app.setFilter(true);
					return;
				}
			}

			writeMark(context, file);
		} finally {
			if (c != null) {
				c.close();
			}
		}
    }

	private static void writeMark(Context context, File file) {
		FileOutputStream fos = null;
		try {
			fos = context.openFileOutput(file.getName(), Context.MODE_WORLD_WRITEABLE);
			fos.write(0);
			fos.flush();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(fos != null){
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
    
    /**
     * 判断array整型数组中是否包含value值
     * @ author: 张永辉
     *@param array 整型数组
     *@param value 
     *@return
     */
    public static boolean intArrayContain(int[] array,int value){
        if(array==null){
            return false ;
        }
        
        for(int e :array){
            if(e == value){
                return true ;
            }
        }
        
        return false ;
        
    }
    
    public static Intent generateMtFolderIntent(long id){
    	Intent intent = new Intent() ;
    	intent.setComponent(new ComponentName(DEFAULT_PACKAGE_NAME_PREFIX+id, DEFAULT_CLASS_NAME_PREFIX+id)) ;
    	intent.putExtra(IconCache.FOLDER_FLAG, true);
    	return intent ;
    }
    
    public static String generateMtFolderIntentStr(long id){
    	Intent intent = new Intent() ;
    	intent.setComponent(new ComponentName(DEFAULT_PACKAGE_NAME_PREFIX+id, DEFAULT_CLASS_NAME_PREFIX+id)) ;
    	intent.putExtra(IconCache.FOLDER_FLAG, true);
    	return intent.toUri(0) ;
    }
    
    public static int getIndexByCellXY(int cellX, int cellY) {
        int index = Mogoo_GlobalConfig.getWorkspaceLongAxisCells(Mogoo_GlobalConfig.isLandscape()) * cellY + cellX;
        return index;
    }
    
    public static int[] convertToCell(int index) {
        int xCount = Mogoo_GlobalConfig.getWorkspaceLongAxisCells(Mogoo_GlobalConfig.isLandscape());
        return new int[] {
                index % xCount, index / xCount
        };
    }
    
    public static byte[] bitmap2Bytes(Bitmap bm){  
        ByteArrayOutputStream baos = new ByteArrayOutputStream();    
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);    
        return baos.toByteArray();  
    } 
    
    /**
     * 取得当前壁纸图片 
     *@author: 张永辉
     *@return
     */
    public static  Bitmap getWallpagerImage(Context context){
        // 获取当前壁纸
        WallpaperManager wm = WallpaperManager.getInstance(context);
        
        Drawable wallpaper = wm.getDrawable();
        Bitmap wallpagerImage = ((BitmapDrawable) wallpaper).getBitmap();
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "wallpagerImage width:"+wallpagerImage.getWidth()) ;
            Log.d(TAG, "wallpagerImage height:"+wallpagerImage.getHeight());
        }
        
        return wallpagerImage ;
    }
    
}
