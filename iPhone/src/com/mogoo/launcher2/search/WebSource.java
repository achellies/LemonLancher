/**  
 * 文 件 名:  WebSource.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：                       
 * 版    本:  1.0  
 * 创建时间:   2010-12-15
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2010-12-15        author       1.0          1.0 Version  
 */  

package com.mogoo.launcher2.search;

import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;

public class WebSource implements Source {
    
    private Uri mSourceIconUri;
    private String mTitle;
    private String mSnippet;
    private String mOther;
    private Drawable mIcon;
    private Intent mIntent;
    
    private WebSource(){}
    
    public static WebSource create(Uri sourceIconUri, String title, String snippet, String other, Drawable icon){
        WebSource temp = new WebSource();
        
        temp.mSourceIconUri = sourceIconUri;
        temp.mTitle = title;
        temp.mSnippet = snippet;
        temp.mOther = other;
        temp.mIcon = icon;
        temp.mIntent = new Intent(Intent.ACTION_VIEW, sourceIconUri);
        
        return temp;
    }
    
    public ComponentName getComponentName() {
        return null;
    }

    public CharSequence getLabel() {
        return mTitle;
    }

    public Uri getSourceIconUri() {
        return mSourceIconUri;
    }
    
    public Drawable getIcon() {
        return mIcon;
    }
    
    public CharSequence getHint() {
        return mSnippet;
    }

    public Intent getIntent() {
        return mIntent;
    }

    public CharSequence getOther() {
        return mOther;
    }
   
}
