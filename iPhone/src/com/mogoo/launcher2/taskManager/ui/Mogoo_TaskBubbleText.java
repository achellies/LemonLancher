/**  
 * 文 件 名:  MT_MissionBubbleText.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：  黄悦                     
 * 版    本:  1.0  
 * 创建时间:   2011-6-16
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-6-16        黄悦       1.0          1.0 Version  
 */        

package com.mogoo.launcher2.taskManager.ui;

import com.mogoo.launcher2.Mogoo_BubbleTextView;
import com.mogoo.launcher2.ShortcutInfo;

import android.content.ComponentName;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class Mogoo_TaskBubbleText extends Mogoo_BubbleTextView {
    
    private OnDelListener onDelListener ;

    public Mogoo_TaskBubbleText(Context context) {
        this(context, null);
    }
    
    public Mogoo_TaskBubbleText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Mogoo_TaskBubbleText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
    
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                break;

            case MotionEvent.ACTION_DOWN:
                int downX = (int) event.getX();
                int downY = (int) event.getY();
                
                if (mVibration && delIcon != null && downX >= 0 && downX <= delIcon.getWidth() && downY >= 0
                        && downY <= delIcon.getHeight()) {
                    ShortcutInfo info = (ShortcutInfo) getTag();
                    
                    if(info != null && info.intent != null){
//                        MT_TaskUtil.killTask(getContext(), info.intent.getComponent());
                        if(onDelListener!=null){
                            onDelListener.onDel(info.intent.getComponent()) ;
                        }
                    }
                } 
               
                break;

        }
        
        passTouchEvent = true;
        
        return super.onTouchEvent(event);

    }
    
    public OnDelListener getOnDelListener() {
        return onDelListener;
    }

    public void setOnDelListener(OnDelListener onDelListener) {
        this.onDelListener = onDelListener;
    }

    public static interface OnDelListener{
        public void onDel(ComponentName cn);
    }

}
