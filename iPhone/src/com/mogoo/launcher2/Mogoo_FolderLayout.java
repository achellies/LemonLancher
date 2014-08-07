/**  
 * 文 件 名:  MT_FolderLayout.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：  黄悦                     
 * 版    本:  1.0  
 * 创建时间:   2011-3-22
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-3-22        黄悦       1.0          1.0 Version  
 */        

package com.mogoo.launcher2;

import android.content.Context;
import android.text.Editable;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;

public class Mogoo_FolderLayout extends RelativeLayout {
    
    public Mogoo_FolderLayout(Context context) {
        this(context, null);
    }
    
    public Mogoo_FolderLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    
    public Mogoo_FolderLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    /**
     * 
     * 安装组件及其事件
     * @ author: 黄悦
     */
    public void setupViews(){
        Mogoo_ComponentBus componentBus = Mogoo_ComponentBus.getInstance();
        
        //add by 张永辉 2011-3-15
        //文件夹展开后的上部区域
        ImageView folderLayerTopImage ;
        //文件夹展开后的下部区域
        ImageView folderLayerBottomImage ;
        //文件夹展开后的小三角图片
        ImageView folderIndication ;
        //文件夹展开后的中部区域
        RelativeLayout folderLayerCenter ;
        //文件夹展开后的编辑框
        EditText titleEdit ;
        
        
        folderLayerTopImage = (ImageView)findViewById(R.id.folderLayerTopImage);
        componentBus.addActivityComp(R.id.folderLayerTopImage, folderLayerTopImage, getContext());
        
        folderLayerBottomImage = (ImageView)findViewById(R.id.folderLayerBottomImage);
        componentBus.addActivityComp(R.id.folderLayerBottomImage, folderLayerBottomImage, getContext());
        
        folderIndication = (ImageView)findViewById(R.id.folderIndication);
        componentBus.addActivityComp(R.id.folderIndication, folderIndication, getContext());
        
        folderLayerCenter = (RelativeLayout) findViewById(R.id.folderLayerCenter);
        componentBus.addActivityComp(R.id.folderLayerCenter, folderLayerCenter, getContext());
        
        titleEdit = (EditText) findViewById(R.id.titleEdit);
        titleEdit.setOnKeyListener((Mogoo_FolderWorkspace)findViewById(R.id.folderWorkspace));
        componentBus.addActivityComp(R.id.titleEdit, titleEdit, getContext());
        
        TextView title = (TextView) findViewById(R.id.title);
        componentBus.addActivityComp(R.id.title, title, getContext());
        
        componentBus.addActivityComp(R.id.titleText, findViewById(R.id.titleText), getContext());
        componentBus.addActivityComp(R.id.folderTrigona, findViewById(R.id.folderTrigona), getContext());
        //end 
    }
}
