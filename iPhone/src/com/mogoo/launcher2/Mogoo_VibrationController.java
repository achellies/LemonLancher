/**  
 * 文 件 名:  MT_VibrationController.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：  黄悦                     
 * 版    本:  1.0  
 * 创建时间:   2011-1-22
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-1-22        黄悦       1.0          1.0 Version  
 */        

package com.mogoo.launcher2;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.taskManager.ui.Mogoo_TaskWorkspace;
import com.mogoo.launcher2.utils.CheckUtils;
import com.mogoo.launcher2.utils.Mogoo_BitmapCache;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;
import com.mogoo.launcher2.utils.Mogoo_WorkspaceInface;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.security.acl.Group;

public class Mogoo_VibrationController implements Runnable {
    private static final String TAG = "Mogoo_VibrationController";
    //全局抖动标志
    public static boolean isVibrate;
    //抖动启动完成标示
    public static boolean setVibrateFinish = false;
    public static boolean isLoading = false;
    //抖动参与组件
    private int[] resIds;
    //图片缓存
    private Mogoo_BitmapCache bitmapCache;
    private Context context;
    private Handler handler = new Handler();
    
    //开启抖动线程
//    private Runnable startVibrateRunable = new Runnable() {
        public void startVibrateRunable() {
            Mogoo_ComponentBus bus = Mogoo_ComponentBus.getInstance();
            if(bus.getActivityComp(R.id.titleEdit, context) != null){
                bus.getActivityComp(R.id.titleEdit, context).setVisibility(View.VISIBLE);
                bus.getActivityComp(R.id.title, context).setVisibility(View.GONE);
            }
            new Thread(Mogoo_VibrationController.this).start();
//            handler.removeCallbacks(startVibrateRunable);
            setVibrateFinish = true;
        }
//    };
    
    public Mogoo_VibrationController(Mogoo_BitmapCache cache, int[] resIds)
    {
        bitmapCache = cache;
        this.resIds = resIds;
//        MT_GlobalConfig.getVibrationViewID()
    }
    
    public void setResIds(int[] resIds){
        this.resIds = resIds;
    }
    
    /**
     * 
     * 启动图标抖动
     * @ author: 黄悦
     *@param cache
     */
    public void startVibrate(Context context){
        if(isVibrate){
            return;
        }
        
        bitmapCache.recycleAllByType(Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_VIBRATION);
        this.context = context;
        isVibrate = true;
        isLoading = true;
        oprateVibrationFlag(true);
        startVibrateRunable();
        
        if(context instanceof Launcher){
        	((Launcher)context).unLockClick();
        }
//        handler.post(startVibrateRunable);
    }
    
    /**
     * 
     * 停止图标抖动
     * @ author: 黄悦
     */
    public void stopVibrate()
    {
		// add by 袁业奔 2011-9-14 
    	Workspace workspace = (Workspace)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.workspace, context);
    	if(workspace!=null){
        	workspace.refreshWorkspace();
        	CheckUtils.reCheck(workspace.getContext());
    	}
		// end
    	
    	handler.post(new Runnable() {
			public void run() {
				if(!isVibrate){
		            return;
		        }
				isVibrate = false;
				Mogoo_ComponentBus bus = Mogoo_ComponentBus.getInstance();
				if(bus.getActivityComp(R.id.titleEdit, context) != null){
    	            bus.getActivityComp(R.id.titleEdit, context).setVisibility(View.GONE);
    	            bus.getActivityComp(R.id.title, context).setVisibility(View.VISIBLE);
				}
		        setVibrateFinish = false;
		        oprateVibrationFlag(false);
		        context = null;
		        System.gc();
			}
		});
    }
    
    /**
     * 抖动激发
     * @ author: 黄悦
     */
    public void run() 
    {
        while(true){
            Mogoo_ComponentBus bus = Mogoo_ComponentBus.getInstance();
            View view = null;
            Workspace worksapce = null;
            for(int resId : resIds)
            {
                view = bus.getActivityComp(resId, context);
                
                if(view == null || view.getVisibility() != View.VISIBLE)
                {
                    continue;
                }
                
                if (view instanceof Workspace) 
                {
                    worksapce = (Workspace) view;
                    int index = worksapce.getCurrentScreen();
                    int count = worksapce.getChildCount();
                    int searchScreen = Mogoo_GlobalConfig.getSearchScreen();
                    activeIconVibrate(worksapce.getChildAt(index));
                    if(index + 1 < count && index + 1 != searchScreen){
                        activeIconVibrate(worksapce.getChildAt(index + 1));
                    	}
                    
                    if(index - 1 != searchScreen && index - 1 >= 0){
                        activeIconVibrate(worksapce.getChildAt(index - 1));
                    	}
                } 
                else 
                { 
                    activeIconVibrate(view);
                }
            }
            worksapce = null;
            bus = null;
            
            try {
                Thread.sleep(90);
            } catch (InterruptedException e) {
                Log.e("Mogoo_VibrationController", "", e);
            }
            
            if(!isVibrate){
                break;
            }
        }
    }
    
    /*
     * 操作抖动标志入口
     */
    private void oprateVibrationFlag(final boolean start)
    {
        Mogoo_ComponentBus bus = Mogoo_ComponentBus.getInstance();
        ViewGroup viewGroup = null;
        Mogoo_WorkspaceInface wsTemp = null;
        for(int resId : resIds)
        {
            viewGroup = (ViewGroup) bus.getActivityComp(resId, context) ;
            
            if(viewGroup == null){
                continue;
            }
            
            if(!(bus.getActivityComp(resId, context) instanceof ViewGroup)){
                return;
            }
            
            if(viewGroup instanceof Mogoo_WorkspaceInface && start){
                wsTemp = (Mogoo_WorkspaceInface)viewGroup;
                excuteWorkspaceFlag(wsTemp);
                final Mogoo_WorkspaceInface ws = wsTemp;
                new Thread(){
                    public void run() {
                        excuteWorkspaceOtherFlag(ws);
                    };
                }.start();
            }else{
                oprateVibrationFlag(viewGroup, start);
            }
        }
        
        bus = null;
        viewGroup = null;
        wsTemp = null;
    }
    
    private boolean isWorkSpaceType(ViewGroup vg){
        return vg instanceof Workspace || vg instanceof Mogoo_TaskWorkspace;
    }
    
    private void excuteWorkspaceFlag(Mogoo_WorkspaceInface ws){
        if(ws == null){
            return;
        }
        
        oprateVibrationFlag(((ViewGroup)ws).getChildAt(ws.getCurrentScreen()), true);
        isLoading = false;
    }
    
	public void excuteWorkspaceOtherFlag(Mogoo_WorkspaceInface ws) {
		if (ws == null) {
			return;
		}

		ViewGroup vg = (ViewGroup) ws;
		int screenIndex = ws.getCurrentScreen();

		for (int i = 0; i < vg.getChildCount(); i++) {
			if (i == screenIndex || i == (screenIndex + 1) || i == (screenIndex - 1)) {
				oprateVibrationFlag(vg.getChildAt(i), true);
			} else {
				oprateVibrationFlag(vg.getChildAt(i), false);
			}
		}
	}
    
    /*
     * 操作抖动标志
     */
    private void oprateVibrationFlag(View view, boolean start){
        if(!(view instanceof ViewGroup)){
            return;
        }
        
        ViewGroup viewGroup = (ViewGroup) view;
        View v = null;
        int size = viewGroup.getChildCount();
        ShortcutInfo info = null;
        
        for(int i = 0; i < size; i++){
            v = viewGroup.getChildAt(i);
            if(v instanceof ViewGroup){
                oprateVibrationFlag(v, start);
            }
            else if(v instanceof Mogoo_BubbleTextView)
            {
                if(start)
                {
                     if(v.getVisibility() != View.VISIBLE){
                         if(Mogoo_GlobalConfig.LOG_DEBUG){
                             Log.d(TAG, "Drag View start vibrate");
                         }
                         final View temp = v;
                         handler.post(new Runnable() {
                            public void run() {
                                ((Mogoo_BubbleTextView)temp).startVibrate(bitmapCache, 0);
                            	}
                         	});
                     } else {
                         ((Mogoo_BubbleTextView)v).startVibrate(bitmapCache, i % 4);
                     }
                }
                else
                {
                    ((Mogoo_BubbleTextView)v).stopVibrate();
                    info = (ShortcutInfo) ((Mogoo_BubbleTextView)v).getTag();
                    bitmapCache.recycle(info.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_VIBRATION);
                }
            } 
            else 
            {
                return;
            }
        }
        
        info = null;
        v = null;
    }
    
    /*
     * 图标刷新
     */
    private void activeIconVibrate(View view)
    {
        if(view instanceof ViewGroup)
        {
            ViewGroup viewGroup = (ViewGroup) view;
            int size = viewGroup.getChildCount();
            View child = null;
            
            for(int i = 0; i < size; i++)
            {
                child = viewGroup.getChildAt(i);
                if(child instanceof Mogoo_BubbleTextView && child != null && ((Mogoo_BubbleTextView)child).isVibration() && child.getVisibility() == View.VISIBLE){
                    child.postInvalidate();
                }else if(view instanceof ViewGroup){
                    activeIconVibrate(child);
                }
            }
            
            viewGroup = null;
            child = null;
        }
    }
    
    

}
