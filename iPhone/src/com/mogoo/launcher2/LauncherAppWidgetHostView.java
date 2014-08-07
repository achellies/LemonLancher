/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;

import com.mogoo.launcher.R;

/**
 * {@inheritDoc}
 */
public class LauncherAppWidgetHostView extends AppWidgetHostView implements OnClickListener {
    
    private final static String TAG = "Launcher.LauncherAppWidgetHostView" ;
    
    private boolean mHasPerformedLongPress;
    
    private CheckForLongPress mPendingCheckForLongPress;
    
    private LayoutInflater mInflater;
    
    //add by 张永辉
    private View button;
    private Bitmap delBitmap;
    private boolean showDelIcon = false ;
    private Launcher launcher;
    //end 
    
    public LauncherAppWidgetHostView(Context context) {
        super(context);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        
        //add by 张永辉
        button = new View(context);
        delBitmap = ((LauncherApplication)(context.getApplicationContext())).getIconCache().getBitmap(R.drawable.mogoo_del) ;
        button.setBackgroundResource(R.drawable.mogoo_del);
        button.setOnClickListener(this);
        //end
    }
    
    @Override
    protected View getErrorView() {
        return mInflater.inflate(R.layout.appwidget_error, this, false);
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // Consume any touch events for ourselves after longpress is triggered
        if (mHasPerformedLongPress) {
            mHasPerformedLongPress = false;
            return true;
        }
            
        // Watch for longpress events at this level to make sure
        // users can always pick up this widget
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                postCheckForLongClick();
                break;
            }
            
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mHasPerformedLongPress = false;
                if (mPendingCheckForLongPress != null) {
                    removeCallbacks(mPendingCheckForLongPress);
                }
                break;
        }
        
        // Otherwise continue letting touch events fall through to children
        return false;
    }
    
    class CheckForLongPress implements Runnable {
        private int mOriginalWindowAttachCount;

        public void run() {
            if ((LauncherAppWidgetHostView.this.getParent() != null) && hasWindowFocus()
                    && mOriginalWindowAttachCount == getWindowAttachCount()
                    && !mHasPerformedLongPress) {
                if (performLongClick()) {
                    mHasPerformedLongPress = true;
                }
            }
        }

        public void rememberWindowAttachCount() {
            mOriginalWindowAttachCount = getWindowAttachCount();
        }
    }

    private void postCheckForLongClick() {
        mHasPerformedLongPress = false;

        if (mPendingCheckForLongPress == null) {
            mPendingCheckForLongPress = new CheckForLongPress();
        }
        mPendingCheckForLongPress.rememberWindowAttachCount();
        postDelayed(mPendingCheckForLongPress, ViewConfiguration.getLongPressTimeout());
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        mHasPerformedLongPress = false;
        if (mPendingCheckForLongPress != null) {
            removeCallbacks(mPendingCheckForLongPress);
        }
    }
    
    /**
     * 张永辉 2011-2-24
     */
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        
        if(this.showDelIcon){
            CellLayout.LayoutParams lay = (CellLayout.LayoutParams) getLayoutParams();
            lay.width = delBitmap.getWidth();
            lay.height = delBitmap.getHeight();
            lay.x = this.getLeft();
            lay.y = this.getTop();
            
            if (button != null) {
                removeView(button) ;
                  addView(button, lay);
            }
        }else{
            removeView(button) ;
        }
        
    }

    /**
     * 张永辉 2011-2-24
     */
    public void onClick(View v) {
        Dialog dialog22 = new AlertDialog.Builder(this.getContext())
        .setMessage(this.getContext().getString(R.string.mogoo_del_ask) + " "  + " ?")
        .setPositiveButton(this.getContext().getString(R.string.mogoo_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog1, int whichButton) {
                        try {
                            if (launcher != null) {
                                launcher.removeAppWidget((LauncherAppWidgetInfo)getTag());

                                final LauncherAppWidgetHost appWidgetHost = launcher.getAppWidgetHost();
                                if (appWidgetHost != null) {
                                    appWidgetHost.deleteAppWidgetId(((LauncherAppWidgetInfo)getTag()).appWidgetId);
                                }

                                LauncherModel.deleteItemFromDatabase(launcher, (LauncherAppWidgetInfo)getTag());
                                CellLayout cellLayout = (CellLayout)(launcher.getWorkspace().getChildAt(launcher.getWorkspace().getCurrentScreen())) ;
                                cellLayout.removeView(LauncherAppWidgetHostView.this) ;
                            }
                        } catch (Exception e) {
                            // TODO: handle exception
                            e.printStackTrace();
                        }
                    }
                })
        .setNeutralButton(this.getContext().getString(R.string.mogoo_cancal),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog1, int whichButton) {
                        dialog1.dismiss() ;
                    }
                }).create();
        dialog22.show();
    }

    /**
     * 
     * @ author: 张永辉 2011-2-24
     *@param launcher
     */
    public void setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }

    /**
     * 显示删除按钮
     * @ author:张永辉 
     */
    public void showDelIcon(){
        this.showDelIcon = true ;
        this.requestLayout() ;
    }
    
    public boolean isDelIconState()
    {
    	return showDelIcon;
    }
    
    /**
     * 去掉删除按钮
     * @ author:张永辉 
     */
    public void removeDelIcon(){
        this.showDelIcon = false ;
        this.requestLayout() ;
    }
    
}
