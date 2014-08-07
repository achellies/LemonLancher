/**  
 * 文 件 名:  MT_BubbleTextView.java  
 * 描    述： BubbleTextView的扩展类，实现图标倒影到绘制，抖动  
 * 版    权： Copyright (c)2010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者： 魏景春 黄悦     
 * 版    本:  1.0  
 * 创建时间:   2011-1-19
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-1-19        魏景春       1.0          1.0 Version  
 */

package com.mogoo.launcher2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_BitmapCache;
import com.mogoo.launcher2.utils.Mogoo_BitmapUtils;
import com.mogoo.launcher2.utils.Mogoo_DialogUtils;

public class Mogoo_BubbleTextView extends BubbleTextView implements Cloneable {
    
    private static final String TAG = "Mogoo_BubbleTextView" ;

    // 显示图标倒影标志
    private boolean reflection = false;

    private Bitmap iconReflection;

    private Bitmap iconWithoutReflection;

    // 抖动标志
    protected boolean mVibration = false;

    // 图标顺时针旋转标志
    private boolean clockWiseRotate = true;

    // 抖动帧数组
    private Bitmap[] vibrationBitmaps;

    // 删除图标
    protected Bitmap delIcon;

    // 计数图标
    protected Bitmap countIcon;

    private int frameIndex;

    private int frameCount;

    private int leftLimit;

    private int rightLimit;

    private Paint paint;

    private Context mContext;

    private boolean isClicked = false;

    private static final int POST_START_TIME_LIMIT = 3;

    private long lastInvalTime = 0;

    private long keyDownTime;

    private int downX;

    private int downY;

    private final static int DRAG_RANGE = 10;

    private final static long DELAY_TIME_DRAG = 200;
    
    protected boolean passTouchEvent = false;

    public Mogoo_BubbleTextView(Context context) {
        this(context, null);
    }

    public Mogoo_BubbleTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public Mogoo_BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    @Override
    public void draw(Canvas canvas) {
        if (!mVibration) {
            drawReflection(canvas);
            super.draw(canvas);
        }
        // 抖动描绘
        drawVibrateIcon(canvas);
        // 计数器描绘
        if (!mVibration) {
            drawCountIcon(canvas);
        }
        canvas.save();
        
    }

    public void setIconWithoutReflection() {

        if (iconWithoutReflection == null) {
            if (reflection) {
                reflection = false;

                if (!isDrawingCacheEnabled() && vibrationBitmaps == null) {
                    setDrawingCacheEnabled(true);
                }
                Bitmap bitmap = getDrawingCache();

                if (bitmap != null) {
                    iconWithoutReflection = bitmap.copy(Config.ARGB_8888, false);
                }
                reflection = true;
                invalidate();
            }
        }

    }

    public void setVibrationBitmaps(Bitmap[] vibrationBitmaps) {
        this.vibrationBitmaps = vibrationBitmaps;
    }

    /**
     * 设置图标是否显示倒影 @ author: 魏景春
     * 
     * @param reflection 显示倒影标志
     */
    public void setReflection(boolean reflection) {
        this.reflection = reflection;
    }

    public void setIconReflection(Bitmap bitmap) {
        if (bitmap == null && iconReflection != null) {
            iconReflection.isRecycled();
        }
        iconReflection = bitmap;
    }

    /**
     * 抖动标志获取 @ author: 黄悦
     * 
     * @return
     */
    public boolean isVibration() {
        return mVibration;
    }

    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        int eventX = (int) event.getX();
        int eventY = (int) event.getY();
        if(!passTouchEvent){
            switch (action) {
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(System.currentTimeMillis() - keyDownTime) < DELAY_TIME_DRAG || isOverRange(eventX, eventY)) {
                        break;
                    }
    
                    if (mVibration) {
                        float x = event.getX();
                        float y = event.getY();
    
                        super.performLongClick();
    
                        return true;
                    }
    
                    break;
    
                case MotionEvent.ACTION_DOWN:
                    keyDownTime = System.currentTimeMillis();
                    downX = (int) event.getX();
                    downY = (int) event.getY();
                    
                    if (mVibration && delIcon != null && downX >= 0 && downX <= delIcon.getWidth() && downY >= 0
                            && downY <= delIcon.getHeight()) {
                        Mogoo_DialogUtils.showDelDialog(getContext(), (ShortcutInfo) getTag());
                    } 
                   
                    break;
    
            }
        }

        return super.onTouchEvent(event);

    }
    
    
    private boolean isOverRange(int x, int y){
        return Math.sqrt(Math.pow(x - downX, 2) + Math.pow(y - downY, 2)) > DRAG_RANGE;
    }

    /**
     * 删除按键点击 @ author: 黄悦
     */
//    public boolean onTouch(View v, MotionEvent event) {
//
//        // if (mVibration && MotionEvent.ACTION_DOWN == action) {
//        // float x = event.getX();
//        // float y = event.getY();
//        //
//        // if (delIcon != null && x >= 0 && x <= delIcon.getWidth() && y >= 0
//        // && y <= delIcon.getHeight()) {
//        // MT_DialogUtils.showDelDialog(getContext(), (ShortcutInfo) getTag());
//        // } else {
//        // super.performLongClick();
//        // }
//        //
//        // return true;
//        // }
//        //
//        // return false;
//    }

    /**
     * 返回无倾角图标 @ author: 黄悦
     * 
     * @return
     */
    public Bitmap getZeroAngleFrame() {
        if (this.reflection && iconWithoutReflection != null) {
            return iconWithoutReflection;
        }

        return null;
    }

    public void postInvalidate() {
        super.postInvalidate();
    }

    /**
     * 启动图标抖动 @ author: 黄悦
     * 
     * @param cache
     */
    public void startVibrate(Mogoo_BitmapCache cache, int startFrameIndex) {
        startVibrate(cache, startFrameIndex, 0, false);
    }
    
    /**
     * 启动图标抖动 @ author: 曾少彬
     * 
     * @param cache
     * @param refresh 如果为 true, 需重新生成缓存图片，比如：有短信发来的时候
     */
    public void startVibrate(Mogoo_BitmapCache cache, int startFrameIndex, boolean refresh) {
        startVibrate(cache, startFrameIndex, 0, refresh);
    }

    /**
     * 停止图标抖动 @ author: 黄悦
     */
    public void stopVibrate() {
        if (!mVibration) {
            return;
        }

        setDrawingCacheEnabled(false);
        leftLimit = 0;
        rightLimit = 0;
        mVibration = false;
        vibrationBitmaps = null;
        postInvalidate();
    }
    
    public void stopVibrate(Mogoo_BitmapCache cache){
        stopVibrate();
        ShortcutInfo info = (ShortcutInfo) getTag();
        cache.recycle(info.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_VIBRATION);
        
        info = null;
    }

    /**
     * 添加删除图标 @ author: 黄悦
     * 
     * @param delIcon
     */
    public void setDelIcon(Bitmap delIcon) {
        this.delIcon = delIcon;
    }

    public Bitmap getDelIcon() {
        return delIcon;
    }

    /**
     * 添加计数图标 @ author: 黄悦
     * 
     * @param countIcon
     */
    public void setCountIcon(Mogoo_BitmapCache cache, int num, int type) {
    	
        Bitmap countIcon = cache.getDigitalIcon(num);
        
        if (this.countIcon != null) {
            Bitmap temp = this.countIcon;
            temp = null;
         }
        this.countIcon = countIcon;
    }

    /**
     * 绘制图标倒影 @ author:魏景春
     * 
     * @param canvas 画布
     */
    private void drawReflection(Canvas canvas) {
        if (reflection && iconReflection != null) {
            if (iconReflection.isRecycled()) {
                iconReflection = ((ShortcutInfo) getTag())
                        .getIconReflection(((LauncherApplication) getContext()
                                .getApplicationContext()).getIconCache());
            }
            
            try{
                canvas.drawBitmap(iconReflection, (float)(getScrollX() + getPaddingLeft() - 2), (float)(getScrollY() + getPaddingTop()
                        + Mogoo_GlobalConfig.getIconHeight() + 2), null);

            }catch (Exception e) {
                System.out.println(e);
            }
            
            
        }
    }

    private void startVibrate(final Mogoo_BitmapCache cache, int startFrameIndex, final int times, final boolean refresh) {
        if (!Mogoo_VibrationController.isVibrate || times > POST_START_TIME_LIMIT || mVibration) {
            return;
        }
        
        
        int size = Mogoo_GlobalConfig.getFrameAngle().length;
        if(startFrameIndex >= size){
            startFrameIndex = size - 1;
        }

        if (!isDrawingCacheEnabled() && vibrationBitmaps == null) {
            setDrawingCacheEnabled(true);
            buildDrawingCache(true);
        }
        Bitmap bitmap = getDrawingCache();

        if (bitmap == null || bitmap.isRecycled()) {
            final int startIndex = startFrameIndex;
            postDelayed(new Runnable() {
                public void run() {
                    startVibrate(cache, startIndex, times, refresh);
                }
            }, 100);
            return;
        }
        ShortcutInfo info = (ShortcutInfo) getTag();
        Canvas canvas = new Canvas(bitmap);
        // 删除图标描绘
        drawDelIcon(canvas);
        drawCountIcon(canvas);
        canvas.save();

        leftLimit = getScrollX() + getLeft();
        rightLimit = getScrollY() + getRight();
        mVibration = true;
        
        if(refresh)
        {
        	cache.recycle(info.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_VIBRATION);
        }
        
        vibrationBitmaps = info.getIconVibrate(cache, bitmap);
//        frameIndex = startFrameIndex;
        int len = vibrationBitmaps.length;
        frameIndex = (int)(Math.random() * len);
        setDrawingCacheEnabled(false);
        buildDrawingCache(false);
        bitmap = null;
        info= null;
    }

    /*
     * 绘制非系统软件删除标志 黄悦
     */
    private void drawDelIcon(Canvas canvas) {
        if (delIcon != null) {
            canvas.drawBitmap(delIcon, 0, 0, paint);
        }
    }

    /*
     * 绘制抖动帧
     */
    private void drawVibrateIcon(Canvas canvas) {
        if (vibrationBitmaps == null || vibrationBitmaps[frameIndex].isRecycled()) {
            super.draw(canvas);

            if (vibrationBitmaps != null && vibrationBitmaps[frameIndex].isRecycled()) {
                restartVibrate(
                        ((LauncherApplication) getContext().getApplicationContext()).getIconCache(),
                        0);
            }

            return;
        }

        if (mVibration) {
            drawRote(canvas);
        }
    }

    /*
     * 绘制旋转图
     */
    private void drawRote(Canvas canvas) {
        if (!mVibration) {
            return;
        }

        // canvas.setDrawFilter(new PaintFlagsDrawFilter(0,
        // Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
        canvas.drawBitmap(vibrationBitmaps[frameIndex], getScrollX(), getScrollY(), paint);

        if (Math.abs(System.currentTimeMillis() - lastInvalTime) > 60) {
            if (clockWiseRotate) {
                if (++frameIndex >= frameCount) {
                    frameIndex = frameIndex - 2;
                    clockWiseRotate = false;
                }
            } else {
                if (--frameIndex < 0) {
                    frameIndex = 1;
                    clockWiseRotate = true;
                }
            }

            lastInvalTime = System.currentTimeMillis();
        }
    }

    // /*
    // * 绘制旋转图
    // */
    // private void drawRote(Canvas canvas) {
    // if (!mVibration) {
    // return;
    // }
    //
    // if(this.nextFrame ==null)
    // {
    // nextFrame = Bitmap.createBitmap(vibrationBitmaps[frameIndex].getWidth(),
    // vibrationBitmaps[frameIndex].getHeight(), Config.ARGB_8888);
    // tempCanvas.setBitmap(nextFrame);
    // tempCanvas.drawBitmap(vibrationBitmaps[frameIndex], 0,0, tempPaint);
    // }
    //
    // canvas.drawBitmap(nextFrame, mScrollX, mScrollY, paint);
    //
    // if(clockwise)
    // {
    // if (++frameIndex >= frameCount) {
    // --frameIndex;
    // clockwise = false;
    // }
    // }else
    // {
    // if (--frameIndex < 0) {
    // frameIndex = 0;
    // clockwise = true;
    // }
    // }
    //
    //
    // nextFrame = Bitmap.createBitmap(vibrationBitmaps[frameIndex].getWidth(),
    // vibrationBitmaps[frameIndex].getHeight(), Config.ARGB_8888);
    // tempCanvas.setBitmap(nextFrame);
    // tempCanvas.drawBitmap(vibrationBitmaps[frameIndex], 0,0, tempPaint);
    // }

    /*
     * 图标计数器初始化
     */
    private void drawCountIcon(Canvas canvas) {
        if (countIcon != null) {
            Matrix matrx = new Matrix();
            matrx.setTranslate(getScrollX() + getWidth() - countIcon.getWidth() - 2, getScrollY());
            canvas.drawBitmap(countIcon, matrx, paint);
            canvas.save();
        }
    }

    /**
     * 更新抖动帧 @ author: 黄悦
     * 
     * @param cache
     * @param startFrameIndex
     */
    private void restartVibrate(Mogoo_BitmapCache cache, int startFrameIndex) {
        stopVibrate();
        cache.recycle(((ShortcutInfo) getTag()).intent.getComponent(),
                Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_VIBRATION);
        startVibrate(cache, startFrameIndex, 0, false);
    }

    /*
     * 初始化
     */
    private void init() {
        frameCount = Mogoo_GlobalConfig.getFrameCount();
        paint = new Paint();
        paint.setTextSize(12);
        paint.setAntiAlias(true);
//        setOnTouchListener(this);
        // add by 张永辉 2011-1-23
        // 初始化padingTop,padingLeft,padingRight,paddingBottom
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_CELL_PADDING_LEFT,
                this.getPaddingLeft());
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_CELL_PADDING_TOP,
                this.getPaddingTop());
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_CELL_PADDING_RIGHT,
                this.getPaddingRight());
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_CELL_PADDING_BOTTOM,
                this.getPaddingBottom());
        // end
    }
    /**
     * 去掉桌面图标的右部渐入渐出效果
     * 
     * @author 张永辉 2010-12-28
     */
    @Override
    protected float getRightFadingEdgeStrength() {
        return 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)getLayoutParams();
        
//        if(MT_GlobalConfig.LOG_DEBUG){
//            Log.d(TAG, "topMargin="+lp.topMargin+" leftMargin="+lp.leftMargin+" rightMargin="+lp.rightMargin+" bottomMargin="+lp.bottomMargin) ;
//        }
        
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_CELL_MARGIN_TOP, lp.topMargin) ;
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_CELL_MARGIN_LEFT, lp.leftMargin) ;
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_CELL_MARGIN_RIGHT, lp.rightMargin) ;
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_CELL_MARGIN_BOTTOM, lp.bottomMargin) ;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
