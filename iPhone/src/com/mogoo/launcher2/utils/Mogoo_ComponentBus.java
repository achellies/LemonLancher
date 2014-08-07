
package com.mogoo.launcher2.utils;

import java.util.HashMap;
import java.util.Iterator;

import android.app.Activity;
import android.content.Context;
import android.view.View;

/**
 * 获得已注册view的总线类
 * @author huangyue
 *
 */
public class Mogoo_ComponentBus {
    private static Mogoo_ComponentBus cBus;
    
    private final HashMap<Context, HashMap<Integer, View>> viewContext;
    
    private Mogoo_ComponentBus(){
        viewContext = new HashMap<Context, HashMap<Integer,View>>();
    }
    
    /**
     * 
     * 获得Bus实例
     * @ author: 黄悦
     *@return
     */
    public static Mogoo_ComponentBus getInstance(){
        if(cBus == null){
            cBus = new Mogoo_ComponentBus();
        }
        
        return cBus;
    }
    
    /**
     * 注册view到总线缓存中
     * @author huangyue
     * @param layoutId
     * @param view
     * @param context
     */
    public void addActivityComp(int resId, View view, Context context){
        initViewMap(context);
        viewContext.get(context).put(resId, view);
    }
    
    /**
     * 由id 和 Activity 获得所需组建view
     * @author huangyue
     * @param resId
     * @param context
     * @return View
     */
    public View getActivityComp(int resId, Context context){
        HashMap<Integer, View> temp = getViewMap(context);
        
        if(temp != null && temp.containsKey(resId)){
            try{
                return temp.get(resId);
            }finally{
                temp = null;
            }
        }
        
        return null;
    }
    
    /**
     * 清理所有的已注册view
     * @author huangyue
     */
    public void clearAll(){
        Iterator<Context> itr = viewContext.keySet().iterator();
        
        while(itr.hasNext()){
            clear(itr.next());
        }
        
        viewContext.clear();
        
        cBus = null;
    }
    
    /**
     * 清理当前activity相关的引用
     * @author huangyue
     * @param context
     */
    public void clear(Context context){
        if(context == null){
            return;
        }
        
        HashMap<Integer, View> temp = viewContext.remove(context);
        
        if(temp == null){
            return;
        }
        
        Iterator<View> viewIterator = temp.values().iterator();
        
        View tempView = null;
        
        while(viewIterator.hasNext()){
            tempView = viewIterator.next();
            if(tempView != null && tempView instanceof Mogoo_ClearBase){
                ((Mogoo_ClearBase)tempView).onClear();
            }
        }
        
        temp.clear();
        temp = null;
    }
    
    private HashMap<Integer, View> getViewMap(Context context){
        if(!viewContext.containsKey(context)){
            return null;
        }
        
        return viewContext.get(context);
    }
    
    /*
     * 新增view时初始化map
     */
    private void initViewMap(Context context){
        if(!viewContext.containsKey(context)){
            viewContext.put(context, new HashMap<Integer, View>());
        }
    }
}
