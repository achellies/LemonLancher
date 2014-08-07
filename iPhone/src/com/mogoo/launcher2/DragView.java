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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.mogoo.launcher2.config.Mogoo_GlobalConfig;

public class DragView extends View implements TweenCallback 
{
	
    private static final String TAG = "Launcher.DragView";
	
    // Number of pixels to add to the dragged item for scaling.  Should be even for pixel alignment.
    //private static final int DRAG_SCALE = 15;
    private static final int DRAG_SCALE = 6;
    private Bitmap mBitmap;
    private Paint mPaint;
    private int mRegistrationX;
    private int mRegistrationY;

    SymmetricalLinearTween mTween;
    private float mScale;
    private float mAnimationScale = 1.0f;

    private WindowManager.LayoutParams mLayoutParams;
    private WindowManager mWindowManager;

    
    //--------------------motone field statement--------- 
    // 拖动的图标在屏幕上坐标位置
    private int screenLeft;
    private int screenTop;
    private int screenRight;
    private int screenBottom;
    
    //拖动图标的起始索引号
    private int startIndex = -1;
    
    
//    // workspace中拖动的图标的单元格号
//    private int[][] mCellIndex = {
//        {
//                -1, -1
//        }
//    };
    //---------------------end---------------------------
    
    /**
     * Construct the drag view.
     * <p>
     * The registration point is the point inside our view that the touch events should
     * be centered upon.
     *
     * @param context A context
     * @param bitmap The view that we're dragging around.  We scale it up when we draw it.
     * @param registrationX The x coordinate of the registration point.
     * @param registrationY The y coordinate of the registration point.
     */
    public DragView(Context context, Bitmap bitmap, int registrationX, int registrationY,
            int left, int top, int width, int height) {
        super(context);

        //mWindowManager = WindowManagerImpl.getDefault();
        mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        
        mTween = new SymmetricalLinearTween(false, 110 /*ms duration*/, this);

        Matrix scale = new Matrix();
        float scaleFactor = width;
        scaleFactor = mScale = (scaleFactor + DRAG_SCALE) / scaleFactor;
        scale.setScale(scaleFactor, scaleFactor);

        mBitmap = Bitmap.createBitmap(bitmap, left, top, width, height, scale, true);

        // The point in our scaled bitmap that the touch events are located
        mRegistrationX = registrationX + (DRAG_SCALE / 2);
        mRegistrationY = registrationY + (DRAG_SCALE / 2);
        
        //add by 张永辉 2011-3-1 使放大的图片透明
        if (mPaint ==null)
        {
            mPaint = new Paint();
        }
        mPaint.setAlpha(160);
        //end 
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(mBitmap.getWidth(), mBitmap.getHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (false) {
            // for debugging
            Paint p = new Paint();
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xaaffffff);
            canvas.drawRect(0, 0, getWidth(), getHeight(), p);
        }
        float scale = mAnimationScale;
        if (scale < 0.999f) { // allow for some float error
            float width = mBitmap.getWidth();
            float offset = (width-(width*scale))/2;
            canvas.translate(offset, offset);
            canvas.scale(scale, scale);
        }
        canvas.drawBitmap(mBitmap, 0.0f, 0.0f, mPaint);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mBitmap.recycle();
    }

    public void onTweenValueChanged(float value, float oldValue) {
        mAnimationScale = (1.0f+((mScale-1.0f)*value))/mScale;
        invalidate();
    }

    public void onTweenStarted() {
    }

    public void onTweenFinished() {
    }

    public void setPaint(Paint paint) {
        mPaint = paint;
        invalidate();
    }

    /**
     * Create a window containing this view and show it.
     *
     * @param windowToken obtained from v.getWindowToken() from one of your views
     * @param touchX the x coordinate the user touched in screen coordinates
     * @param touchY the y coordinate the user touched in screen coordinates
     */
    public void show(IBinder windowToken, int touchX, int touchY) {
        WindowManager.LayoutParams lp;
        int pixelFormat;

        pixelFormat = PixelFormat.TRANSLUCENT;

        lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                touchX-mRegistrationX, touchY-mRegistrationY,
                WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                    /*| WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM*/,
                pixelFormat);
//        lp.token = mStatusBarView.getWindowToken();
        lp.gravity = Gravity.LEFT | Gravity.TOP;
        lp.token = windowToken;
        lp.setTitle("DragView");
        mLayoutParams = lp;

        mWindowManager.addView(this, lp);

        mAnimationScale = 1.0f/mScale;
        mTween.start(true);
    }
    
    /**
     * Move the window containing this view.
     *
     * @param touchX the x coordinate the user touched in screen coordinates
     * @param touchY the y coordinate the user touched in screen coordinates
     */
    void move(int touchX, int touchY) 
    {
        WindowManager.LayoutParams lp = mLayoutParams;
        lp.x = touchX - mRegistrationX;
        lp.y = touchY - mRegistrationY;
        
        if (lp.y < 0)
        {
        	lp.y = 0;
        }
        mWindowManager.updateViewLayout(this, lp);
        
        // -------------add by weijingchun 2011-1-19 ------------
        //设置拖动到图标在屏幕上到坐标位置
        screenLeft = lp.x;
        screenTop = lp.y;
        screenRight = lp.x + getMeasuredWidth();
        screenBottom = lp.y + getMeasuredHeight();
        
        if (Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "touchX =" + touchX + " touchY =" + touchY + " screenTop = " + screenTop);
        }
        
        //-----------------end----------------------------------
    }

    void remove() {
        mWindowManager.removeView(this);
    }
    
    //--------------------motone method statement---------
    /**
     * 拖动图标在屏幕的Left坐标
     * @ author: 魏景春
     * @return 图标Left坐标值
     */
    public int getScreenLeft() {
        return this.screenLeft;
    }

    /**
     * 拖动图标在屏幕的Top坐标 
     * @ author: 魏景春
     * @return 图标Top坐标值
     */
    public int getScreenTop() {
        return this.screenTop;
    }

    /**
     * 拖动图标在屏幕的Right坐标 
     * @ author: 魏景春
     * @return 图标Right坐标值
     */
    public int getScreenRight() 
    {
        return this.screenRight;
    }

    /**
     * 拖动图标在屏幕的Bottom坐标 
     * @ author: 魏景春
     * @return 图标Bottom坐标值
     */
    public int getScreenBottom() {
        return this.screenBottom;
    }
    
//    /**
//     * 设置workspace中拖动的图标的单元格号 
//     * @ author: 魏景春
//     * @param index 拖动图标的单元格号
//     */
//    public void setCellIndex(int[][] index) {
//        mCellIndex = index;
//
//    }
//
//    /**
//     * 获取workspace中拖动的图标的单元格号
//     */
//    public int[][] getCellIndex() {
//        return mCellIndex;
//    }
    
    public void setStartIndex(int index)
    {
    	this.startIndex = index;
    }
    
    public int getStartIndex()
    {
    	return this.startIndex;
    }
    //---------------------end---------------------------
}

