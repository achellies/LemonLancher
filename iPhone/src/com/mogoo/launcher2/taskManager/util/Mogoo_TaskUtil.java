/**  
 * 文 件 名:  Util.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：                       
 * 版    本:  1.0  
 * 创建时间:   2011-5-26
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-5-26        张永辉       1.0          1.0 Version  
 */  

package com.mogoo.launcher2.taskManager.util;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.util.Log;
import android.util.TypedValue;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_BitmapUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Mogoo_TaskUtil {
    
    private static final String TAG = "Launcher.Mogoo_TaskUtil"   ;
    
    private static List<ComponentName> componentNameCache = null ;
    
    //删除图标
    private static Bitmap delIcon ;
    
    
    /**
     * 打开任务
     *@author: 张永辉
     *@Date：2011-5-26
     *@param context
     *@param componentName
     */
    public static void openTask(Context context,ComponentName componentName){
        Intent intent = new Intent() ;
//        intent.setPackage(componentName.getPackageName()) ;
        List<ComponentName> cns = getAllDeskApp(context) ;
        if(cns.contains(componentName)){
            intent.setComponent(componentName) ;
        }else{
            for(ComponentName cn:cns){
                if(cn.getPackageName().equals(componentName.getPackageName())) {
                    intent.setComponent(cn) ;
                    break ;
                }
            }
        }
//        intent.setAction(Intent.ACTION_MAIN) ;
//        intent.setAction(Intent.CATEGORY_LAUNCHER) ;
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try{
            context.startActivity(intent) ;
        }catch(Exception e){
            e.printStackTrace() ;
        }
    }
    
    /**
     * 打开任务
     *@author: 张永辉
     *@Date：2011-5-26
     *@param context
     *@param componentName
     */
    public static void openTask(Context context,Intent intent){
        if(intent==null) return ;
        intent.setFlags((intent.getFlags()&~Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY);
        try{
            context.startActivity(intent) ;
        }catch(SecurityException se){
            openTask(context, intent.getComponent()) ;
        }catch(Exception e){
            e.printStackTrace() ;
        }
    }

    /**
     * kill任务
     *@author: 张永辉
     *@Date：2011-5-26
     *@param context
     *@param componentName
     */
    public static void killTask(Context context,ComponentName componentName){
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "------------killTask()---------packageName="+componentName.getPackageName()) ;
        }
        if(context==null) return  ;
        try{
            ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE) ;
//            am.killBackgroundProcesses(componentName.getPackageName()) ;
//            am.restartPackage(componentName.getPackageName()) ;
//            android.os.Process.killProcess(android.os.Process.myPid()); 
            // TODO: disabled by achellies
//            am.forceStopPackage(componentName.getPackageName()) ;  
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 取得桌面上所有的应用程序
     *@author: 张永辉
     *@Date：2011-6-13
     *@param context
     *@return
     */
    public static List<ComponentName> getAllDeskApp(Context context){
        
        if(componentNameCache!=null){
            return componentNameCache ;
        }
        
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final PackageManager packageManager = context.getPackageManager();

        List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        
        componentNameCache = new ArrayList<ComponentName>() ;
        
        if(apps!=null){
            for(ResolveInfo info:apps){
                componentNameCache.add(new ComponentName(info.activityInfo.applicationInfo.packageName,info.activityInfo.name)) ;
            }
        }
        
        return componentNameCache ;
    }
    
    /**
     * 根据ComponentName取得任务或服务的名称
     *@author: 张永辉
     *@Date：2011-5-26
     *@param context
     *@param componentName
     *@return
     */
    public static String getTitle(Context context,ComponentName componentName){
        String title = componentName.getClassName() ;
                       
        PackageManager pm = context.getPackageManager() ;
        
        try {
            CharSequence cs =  null ;
//            if(getAllDeskApp(context).contains(componentName)){
//                ActivityInfo info = pm.getActivityInfo(componentName, 0) ;
//                cs =  info.loadLabel(pm) ;
//            }else{
//                ApplicationInfo appInfo =  pm.getApplicationInfo(componentName.getPackageName(), 0) ;
//                cs = appInfo.loadLabel(pm) ;
//            }
            //----------------------modify by weijingchun 2011-12-9-------------------                 
//          ActivityInfo info = pm.getActivityInfo(componentName, 0) ;
//          cs =  info.loadLabel(pm) ;
////          ApplicationInfo appInfo =  pm.getApplicationInfo(componentName.getPackageName(), 0) ;
////          cs = appInfo.loadLabel(pm) ;
//          if(cs!=null){
//              title = cs.toString() ;
//          }
//                     String t= Mogoo_BitmapUtils.getApplicationTitle(componentName);
          
          String t= Mogoo_BitmapUtils.getApplicationTitle(componentName);
          Log.i("Mogoo_TaskUtil", "componentName==" + componentName.getPackageName() + " getTitle=" + t);
          if(t!=null && !t.equals(""))
          {
        	  title =t;
          }else
          {
          	 ActivityInfo info = pm.getActivityInfo(componentName, 0) ;
               cs =  info.loadLabel(pm) ;
//               ApplicationInfo appInfo =  pm.getApplicationInfo(componentName.getPackageName(), 0) ;
//               cs = appInfo.loadLabel(pm) ;
               if(cs!=null){
                   title = cs.toString() ;
               }
          }
         
          
          //---------------------end-------------------------
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage()) ;
        }
        
        return title ;
    }
    
    /**
     * 根据ComponentName取得任务图标
     *@author: 张永辉
     *@Date：2011-5-26
     *@param context
     *@param componentName
     *@return
     */
    public static Bitmap getIcon(Context context,ComponentName componentName){
        PackageManager pm = context.getPackageManager() ;
        Log.i("Mogoo_TaskUtil", "componentName==" + componentName.getPackageName() + "   geticon");
        try {
            Drawable icon = pm.getActivityIcon(componentName) ;
           return  drawable2Bitmap(icon) ;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
            Log.d(TAG,e.getMessage()) ;
            return drawable2Bitmap(pm.getDefaultActivityIcon()) ;
        }
    }
    
    /**
     * 获取正在运行中的任务
     *@author: 张永辉
     *@Date：2011-5-26
     *@param context
     *@return
     */
    public static  List<ActivityManager.RunningTaskInfo> getRunningTask(Context context,int maxNum){
        
        if(context==null) return null ;
        
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE) ;
        
        List<ActivityManager.RunningTaskInfo> runningTasks =  am.getRunningTasks(maxNum) ;
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            if(runningTasks!=null){
                Log.d(TAG, "--------------------------Running Task:------------------------------") ;
                for(ActivityManager.RunningTaskInfo task:runningTasks){
                    Log.d(TAG, "baseActivity:"+task.baseActivity) ;
                    Log.d(TAG, "topActivity:"+task.topActivity) ;
                }
                Log.d(TAG, "--------------------------Running Task:------------------------------") ;
            }
        }
        
        return runningTasks ;
    }
    
    /**
     * 获取正在运行中的服务
     *@author: 张永辉
     *@Date：2011-5-26
     *@param context
     *@param maxNum
     *@return
     */
    public static List<ActivityManager.RunningServiceInfo> getRunningService(Context context,int maxNum){
        if(context==null) return null ;
        
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE) ;
        
        return am.getRunningServices(maxNum) ;
    }
    
    public static List<ActivityManager.RunningAppProcessInfo> getRunningAppProcess(Context context){
        if(context==null) return null ;
        
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE) ;
        
        List<ActivityManager.RunningAppProcessInfo> processes =  am.getRunningAppProcesses() ;
        
//        if(processes!=null){
//            for(ActivityManager.RunningAppProcessInfo process:processes){
//                Log.d(TAG, "importanceReasonComponent:"+process.importanceReasonComponent) ;
//                Log.d(TAG, "processName:"+process.processName) ;
//            }
//        }
        
        return processes ;
    }
    
    public static List<ActivityManager.RecentTaskInfo> getRecentTask(Context context,int maxNum){
        if(context==null) return null ;
        
        ActivityManager am = (ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE) ;
        
        List<ActivityManager.RecentTaskInfo> recentTasks =  am.getRecentTasks(maxNum, 0) ;
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            if(recentTasks!=null){
                Log.d(TAG, "--------------------------Recent Task:------------------------------") ;
                for(ActivityManager.RecentTaskInfo task:recentTasks){
                    Log.d(TAG, "baseIntent.getComponent():"+task.baseIntent.getComponent()) ;
                    Log.d(TAG, "origActivity:"+task.origActivity) ;
                }
                Log.d(TAG, "--------------------------Recent Task:------------------------------") ;
            }
        }
        
        return recentTasks ;
    }
    
    /**
     * 回收bitmap
     *@author: 张永辉
     *@Date：2011-5-25
     *@param bitmap
     */
    public static void recycle(Bitmap bitmap){
        if(bitmap!=null&&!bitmap.isRecycled()){
            bitmap.recycle() ;
            bitmap = null ;
        }
    }
    
    /**
     * 获取删除图标
     *@author: 张永辉
     *@Date：2011-5-27
     *@param context
     *@return
     */
    public static Bitmap getDelIcon(Context context){
        if(delIcon==null){
            delIcon = decodeResource(context.getResources(), R.drawable.mogoo_del) ;
        }
        return delIcon ;
    }
    
    /**
     * Drawable转bitmap
     *@author: 张永辉
     *@Date：2011-5-26
     *@param drawable
     *@return
     */
    public static Bitmap drawable2Bitmap(Drawable drawable){
    	if (drawable instanceof BitmapDrawable){
    		return  ((BitmapDrawable)drawable).getBitmap() ;
    	}else if(drawable instanceof NinePatchDrawable){
    		Bitmap bitmap = Bitmap.createBitmap(  
                                    drawable.getIntrinsicWidth(),  
                                    drawable.getIntrinsicHeight(),  
                                    drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888  
                                                    : Bitmap.Config.RGB_565);  
    		Canvas canvas = new Canvas(bitmap);  
		    //canvas.setBitmap(bitmap);  
		    drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());  
		    drawable.draw(canvas);  
		    return bitmap; 
    	}
    	return null ;
    }
    
    /**
     * bitmap转drawable
     *@author: 张永辉
     *@Date：2011-5-30
     *@param bitmap
     *@return
     */
    public static Drawable bitmap2Drawable(Bitmap bitmap){
        return  new BitmapDrawable(bitmap) ;
    }
    
    /**
     * 通过资源文件创建位图
     * @author: 魏景春
     * @param res
     * @param resId
     * @return
     */
    public static Bitmap decodeResource(Resources res, int resId) {
        Bitmap bm = null;

        if (bm == null || bm.isRecycled()) {
            InputStream is = null;
            BitmapFactory.Options opts = new BitmapFactory.Options();

            try {
                final TypedValue value = new TypedValue();
                is = res.openRawResource(resId, value);
                opts.inTargetDensity = value.density;
                bm = BitmapFactory.decodeResourceStream(res, value, is, null, opts);
            } catch (Exception e) {

            } finally {
                try {
                    if (is != null)
                        is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }

        return bm;

    }
}
