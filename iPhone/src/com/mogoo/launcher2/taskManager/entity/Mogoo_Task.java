/**  
 * 文 件 名:  Task.java  
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

package com.mogoo.launcher2.taskManager.entity;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
/**
 * 正在运行的任务
 */
public class Mogoo_Task {
    private ComponentName componentName ;
    private Intent intent ;
    private String title ;
    private Bitmap icon ;
    
    public ComponentName getComponentName() {
        return componentName;
    }
    public void setComponentName(ComponentName componentName) {
        this.componentName = componentName;
    }
    public String getTitle() {
        return title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public Bitmap getIcon() {
        return icon;
    }
    public void setIcon(Bitmap icon) {
        this.icon = icon;
    }
    public Intent getIntent() {
        return intent;
    }
    public void setIntent(Intent intent) {
        this.intent = intent;
    }
    
}
