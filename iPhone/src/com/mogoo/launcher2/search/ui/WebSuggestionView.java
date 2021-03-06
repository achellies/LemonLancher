/**  
 * 文 件 名:  WebSuggestionView.java  
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

package com.mogoo.launcher2.search.ui;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.search.Source;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.Html;
import android.text.Spanned;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class WebSuggestionView extends RelativeLayout implements SuggestionView {
    private TextView mText1;
    private TextView mText2;
    private TextView mText3;
    private ImageView mIcon1;
    private Source mSource;

    public WebSuggestionView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public WebSuggestionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WebSuggestionView(Context context) {
        super(context);
    }

    /**
     * 
     * 填充 suggestion item 内容
     * @ author: 黄悦
     * @return 
     */
    public void bindAsSuggestion(Source source) {
        if(source == null){
            return;
        }
        
        if(source.getLabel() != null){
            mText1.setText(Html.fromHtml("<b>" + source.getLabel() + "<\\/b>"));
        }
        if(source.getHint() != null){
            mText2.setText(Html.fromHtml(source.getHint().toString()));
            mText2.setTextColor(Color.BLACK);
        }
        
        mIcon1.setImageResource(R.drawable.mogoo_globe);
        LayoutParams lp = (LayoutParams) mIcon1.getLayoutParams();
        lp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
    }
    
    /**
     * 
     * 填充View组件
     * @ author: 黄悦
     * @return 
     */
    public void onFinishInflate() {
        super.onFinishInflate();
        mText1 = (TextView) findViewById(R.id.ItemTitle);
        mText1.setTextColor(Color.BLACK);
        mText2 = (TextView) findViewById(R.id.ItemText);
        mIcon1 = (ImageView) findViewById(R.id.ItemImage);
    }

    public void setSource(Source source) {
        mSource = source;
    }

    /**
     * 
     * 获得Intent
     * @ author: 黄悦
     * @return Intent
     */
    public Intent getIntent() {
        if(mSource != null){
           return  mSource.getIntent();
        }
        
        return null;
    }

}
