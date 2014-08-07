/**  
 * 文 件 名:  AppsSource.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：                       
 * 版    本:  1.0  
 * 创建时间:   2010-12-16
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2010-12-16        author       1.0          1.0 Version  
 */  

package com.mogoo.launcher2.search;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;

public class AppsSource implements Source {
    
    private String mTitle;
    private Drawable mIcon;
    private Intent mIntent;
    
    public AppsSource(){}
    
    public AppsSource(String title, Drawable icon, Intent intent){
        mTitle = title;
        mIcon = icon;
        mIntent = intent;
    }
    
    public ComponentName getComponentName() {
        if(mIntent == null){
            return null;
        }
        
        return mIntent.getComponent();
    }

    public CharSequence getLabel() {
        return mTitle;
    }
    
    public Uri getSourceIconUri() {
        return null;
    }
    
    public Drawable getIcon() {
        return mIcon;
    }

    public CharSequence getHint() {
        return null;
    }

    public Intent getIntent() {
        return mIntent;
    }

    public CharSequence getOther() {
        return null;
    }

}
