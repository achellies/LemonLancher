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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningTaskInfo;
import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.CheckThirdAppUtils;
import com.mogoo.launcher2.utils.Mogoo_BitmapCache;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;

//import dalvik.system.VMRuntime;

public class LauncherApplication extends Application {
    private LauncherModel mModel;
    //---- alter by huangyue ----
    //将原有的 IconCache 改成了 MT_BitmapCache
    private Mogoo_BitmapCache mIconCache;
    private Locale location;
    private boolean filter = false;
	//----- end --------------
    //denglixia add 2011.4.27
    public static final String PREFERENCES = "Launcher_Preferences";
    public static final String RESTORE = "restore";
    //denglixia add end 2011.4.27

    @Override
    public void onCreate() {
        //VMRuntime.getRuntime().setMinimumHeapSize(8 * 1024 * 1024);
        try {
            Class<?> cl = Class.forName("dalvik.system.VMRuntime");
            Method getRt = cl.getMethod("getRuntime", new Class[0]);
            Object runtime = getRt.invoke(null, new Object[0]);
            
            final Class<?>[] longArgsClass = new Class[1];
            longArgsClass[0] = Long.TYPE;
            Method setMiniHeaP = runtime.getClass().getMethod("setMinimumHeapSize", longArgsClass);
            setMiniHeaP.invoke(runtime, 8 * 1024 * 1024);
            
            final Class<?>[] floatArgsClass = new Class[1];
            floatArgsClass[0] = Float.TYPE;
            Method setTargetHeapUtilization = runtime.getClass().getMethod("setTargetHeapUtilization", floatArgsClass);
            setTargetHeapUtilization.invoke(runtime, 0.75f);
        } catch (ClassNotFoundException e) {
        } catch (SecurityException e) {
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }
        super.onCreate();
        new Mogoo_GlobalConfig();
        Mogoo_GlobalConfig.initWindowManager(this);
//        startTaskManager();
//        try{
//        	Thread.sleep(10000);
//        }catch (Exception e) {
//			// TODO: handle exception
//		}
        //add by  袁业奔 2011-9-21 设置资源文件中的dimens值 
        initDimens();
        //end
        //---- alter by huangyue ----
        //将原有的 IconCache 改成了 MT_BitmapCache
        mIconCache = new Mogoo_BitmapCache(this);
        //----- end --------
        mModel = new LauncherModel(this, mIconCache);

        // Register intent receivers
        IntentFilter filter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        filter.addDataScheme("package");
        registerReceiver(mModel, filter);
        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE);
        filter.addAction(Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE);
        registerReceiver(mModel, filter);
        //add by huangyue 
        CheckThirdAppUtils.sortForStart(this);
        //end
        // Register for changes to the favorites
        ContentResolver resolver = getContentResolver();
        resolver.registerContentObserver(LauncherSettings.Favorites.CONTENT_URI, true,
                mFavoritesObserver);
        
        Configuration config = getResources().getConfiguration();
        location = config.locale;
        
    }

    private void startTaskManager() {  
        try{
            if(!isTaskOpened()){
                Intent intent = new Intent() ;
                intent.setClassName("com.motone.taskManager", "com.motone.taskManager.TaskManager") ;
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.addFlags(Intent.FLAG_FROM_BACKGROUND);
                
                startActivity(intent);
            }
            
        }catch (Exception e) {
            Log.w("LauncherApplication", e);
        }
    }
    
    //判断task manager是否打开
    private boolean isTaskOpened(){
        List<RunningTaskInfo> taskInfos = null;
        try{
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE) ;
            taskInfos = am.getRunningTasks(Integer.MAX_VALUE);
            
            for(RunningTaskInfo info : taskInfos){
                if(info.baseActivity.getPackageName().equals("com.motone.taskManager")){
                    return true;
                }
            }
            
        }catch (Exception e) {
            e.printStackTrace();
        }finally{
            if(taskInfos != null){
                taskInfos .clear();
            }
            
            taskInfos = null;
        }
        
        return false;
    }

    /**
     * There's no guarantee that this function is ever called.
     */
    @Override
    public void onTerminate() {
        super.onTerminate();

        unregisterReceiver(mModel);
        Mogoo_ComponentBus.getInstance().clearAll();

        ContentResolver resolver = getContentResolver();
        resolver.unregisterContentObserver(mFavoritesObserver);
    }

    /**
     * Receives notifications whenever the user favorites have changed.
     */
    private final ContentObserver mFavoritesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            mModel.startLoader(LauncherApplication.this, false);
        }
    };

    LauncherModel setLauncher(Launcher launcher) {
        mModel.initialize(launcher);
        return mModel;
    }

    public Mogoo_BitmapCache getIconCache() {
        return mIconCache;
    }

    public LauncherModel getModel() {
        return mModel;
    }

    public Locale getLocation() {
        return location;
    }
    
    public boolean isFilter() {
		return filter;
	}

	public void setFilter(boolean filter) {
		this.filter = filter;
	}
	
	@Override
	public void onLowMemory() {
		// TODO Auto-generated method stub
		//add by yeben 2012-4-10
		startService(new Intent(this,SystemOptimizationService.class));
		//end
		super.onLowMemory();
	}
    
	/**
	 * 初始化资源文件中的dimens值
	 * @author: 袁业奔 
	 * @Date：2011-9-21  
	 */ 
	private void initDimens(){
//		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.ICON_WIDTH,57);
//		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.ICON_HEIGHT,57);
//		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.INDICATOR_MARGIN, 3);
//		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.ICON_FODLER_BG_WIDTH,66);
//		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.ICON_FODLER_BG_HEIGHT,66);
//		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.SCALE_ICON_SIZE,10);
//		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.REFLECTION_WIDTH_PORT,57);
//		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.REFLECTION_HEIGHT_PORT,24);
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.ICON_WIDTH,(int)getResources().getDimension(R.dimen.icon_width));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.ICON_HEIGHT,(int)getResources().getDimension(R.dimen.icon_height));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.INDICATOR_MARGIN, (int)getResources().getDimension(R.dimen.indicator_margin));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.ICON_FODLER_BG_WIDTH,(int)getResources().getDimension(R.dimen.icon_folder_bg_width));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.ICON_FODLER_BG_HEIGHT,(int)getResources().getDimension(R.dimen.icon_folder_bg_height));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.SCALE_ICON_SIZE,(int)getResources().getDimension(R.dimen.scale_icon_size));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.SCALE_CELL_SIZE,getResources().getDimension(R.dimen.scale_cell_size));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.FOLDER_PADDING_SIZE,getResources().getDimension(R.dimen.folder_padding_size));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.SCALE_ICON_PADDING_SIZE,getResources().getDimension(R.dimen.scall_icon_padding_size));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.REFLECTION_WIDTH_PORT,(int)getResources().getDimension(R.dimen.reflection_width_port));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.REFLECTION_HEIGHT_PORT,(int)getResources().getDimension(R.dimen.reflection_height_port));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.RADII, (int)getResources().getDimension(R.dimen.radii));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.ICON_SCALE_SIZE, (int)getResources().getDimension(R.dimen.icon_scale_size));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.ICON_COUNT_INFO_TEXT_SIZE, getResources().getDimension(R.dimen.icon_count_text_size));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.DATE_TEXT_SIZE, (int)getResources().getDimension(R.dimen.date_text_size));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.DAY_TEXT_SIZE, (int)getResources().getDimension(R.dimen.day_text_size));
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.DATE_HEIGHT_FIX_VALUE, (int)getResources().getDimension(R.dimen.date_height_fix_value));
	}
}
