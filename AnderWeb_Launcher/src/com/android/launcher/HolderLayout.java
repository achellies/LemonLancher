package com.android.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class HolderLayout extends ViewGroup {
    //ADW: Animation vars
	private final static int CLOSED=1;
	private final static int OPEN=2;
	private final static int CLOSING=3;
	private final static int OPENING=4;
	private int mStatus=OPEN;
	private boolean isAnimating;
	private long startTime;
	private float mScaleFactor;
	private int mIconSize=0;
	private Paint mPaint;
	private Paint mLabelPaint;
	private int mBgAlpha=255;
	private boolean shouldDrawLabels=false;
	private int mAnimationDuration=800;
	private boolean mDrawLabels=true;
	private boolean mFadeDrawLabels=false;
	private float mLabelFactor;
	private long mCurrentTime;
	private float mPorcentajeScale;
	//ADW: listener to dispatch open/close animation events
	private OnFadingListener mOnFadingListener;
    private int distH;
    private int distV;
    private float x;
    private float y;
    private float width;
    private float height;
    private Rect rl1=new Rect();
    private Rect rl2=new Rect();
    private float scale;
    private Rect r3=new Rect();
    private int xx;
	public HolderLayout(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		mPaint=new Paint();
		mPaint.setDither(false);
        mLabelPaint=new Paint();
        mLabelPaint.setDither(false);
        setWillNotDraw(false);
        updateLabelVars(context);
	}

	public HolderLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		mPaint=new Paint();
		mPaint.setDither(false);
        mLabelPaint=new Paint();
        mLabelPaint.setDither(false);
        setWillNotDraw(false);
        updateLabelVars(context);
	}

	public HolderLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		mPaint=new Paint();
		mPaint.setDither(false);
        mLabelPaint=new Paint();
        mLabelPaint.setDither(false);
        setWillNotDraw(false);
        updateLabelVars(context);
	}
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub

	}

	@Override
	protected boolean addViewInLayout(View child, int index,
			LayoutParams params, boolean preventRequestLayout) {
		// TODO Auto-generated method stub
		return super.addViewInLayout(child, index, params, preventRequestLayout);
	}

	@Override
	protected void attachViewToParent(View child, int index, LayoutParams params) {
		// TODO Auto-generated method stub
		super.attachViewToParent(child, index, params);
	}

	@Override
	protected void dispatchSetPressed(boolean pressed) {
		// TODO Auto-generated method stub
		//super.dispatchSetPressed(pressed);
	}

	@Override
	public void dispatchSetSelected(boolean selected) {
		// TODO Auto-generated method stub
		super.dispatchSetSelected(selected);
	}
    /*@Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (child != null) {
            Rect r = new Rect();
            child.getDrawingRect(r);
            requestRectangleOnScreen(r);
        }
    }*/
    @Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            view.setDrawingCacheEnabled(enabled);
            // Update the drawing caches
            view.buildDrawingCache(true);
        }
    }

    @Override
    protected void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        super.setChildrenDrawnWithCacheEnabled(enabled);
    }

	@Override
	protected void onFocusChanged(boolean gainFocus, int direction,
			Rect previouslyFocusedRect) {
		// TODO Auto-generated method stub
		//super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
	}
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
    	//Log.d("HolderLayout","INTERCEPT");
		return true;
    }
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
    	//Log.d("HolderLayout","TOUCH");
		return true;
    }
    /**
     * ADW: easing functions for animation
     */
	static float easeOut (float time, float begin, float end, float duration) {
		float change=end- begin;
		return change*((time=time/duration-1)*time*time + 1) + begin;
	}
	static float easeIn (float time, float begin, float end, float duration) {
		float change=end- begin;
		return change*(time/=duration)*time*time + begin;
	}
	static float easeInOut (float time, float begin, float end, float duration) {
		float change=end- begin;
		if ((time/=duration/2.0f) < 1) return change/2.0f*time*time*time + begin;
		return change/2.0f*((time-=2.0f)*time*time + 2.0f) + begin;
	}
	/**
	 * ADW: Override drawing methods to do animation
	 */
	@Override
	public void draw(Canvas canvas) {
        if (isAnimating) {
            if (startTime == 0) {
                startTime = SystemClock.uptimeMillis();
                mCurrentTime = 0;
            } else {
                mCurrentTime = SystemClock.uptimeMillis() - startTime;
            }
            if (mStatus == OPENING) {
                mScaleFactor = easeOut(mCurrentTime, 3.0f, 1.0f, mAnimationDuration);
                mLabelFactor = easeOut(mCurrentTime, -1.0f, 1.0f, mAnimationDuration);
            } else if (mStatus == CLOSING) {
                mScaleFactor = easeIn(mCurrentTime, 1.0f, 3.0f, mAnimationDuration);
                mLabelFactor = easeIn(mCurrentTime, 1.0f, -1.0f, mAnimationDuration);
            }
            if (mLabelFactor < 0)
                mLabelFactor = 0;
            if (mCurrentTime >= mAnimationDuration) {
                isAnimating = false;
                if (mStatus == OPENING) {
                    mStatus = OPEN;
                    dispatchFadingEvent(OnFadingListener.OPEN);
                } else if (mStatus == CLOSING) {
                    mStatus = CLOSED;
                    dispatchFadingEvent(OnFadingListener.CLOSE);
                }
            }
        }
        if(mStatus!=CLOSED){
            shouldDrawLabels = mFadeDrawLabels && mDrawLabels
                    && (mStatus == OPENING || mStatus == CLOSING);
            mPorcentajeScale = 1.0f;
            if (isAnimating) {
                mPorcentajeScale = 1.0f - ((mScaleFactor - 1) / 3.0f);
                if (mPorcentajeScale > 0.9f)
                    mPorcentajeScale = 1f;
                if (mPorcentajeScale < 0)
                    mPorcentajeScale = 0;
                dispatchFadingAlphaEvent(mPorcentajeScale);
                mBgAlpha = (int) (mPorcentajeScale * 255);
            }
            mPaint.setAlpha(mBgAlpha);
            super.draw(canvas);
        }
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		int saveCount = canvas.save();
		Drawable[] tmp=((TextView)child).getCompoundDrawables();
		if(mIconSize==0){
			mIconSize=tmp[1].getIntrinsicHeight()+child.getPaddingTop();
		}
		if(isAnimating){
			postInvalidate();
			//float x;
			//float y;
			distH=(child.getLeft()+(child.getWidth()/2))-(getWidth()/2);
			distV=(child.getTop()+(child.getHeight()/2))-(getHeight()/2);
			x=child.getLeft()+(distH*(mScaleFactor-1))*(mScaleFactor);
			y=child.getTop()+(distV*(mScaleFactor-1))*(mScaleFactor);
			width=child.getWidth()*mScaleFactor;
			height=(child.getHeight()-(child.getHeight()-mIconSize))*mScaleFactor;
			if(shouldDrawLabels)child.setDrawingCacheEnabled(true);
			if(shouldDrawLabels && child.getDrawingCache()!=null){
				//ADW: try to manually draw labels
				rl1.set(0,mIconSize,child.getDrawingCache().getWidth(),child.getDrawingCache().getHeight());
				rl2.set(child.getLeft(),child.getTop()+mIconSize,child.getLeft()+child.getDrawingCache().getWidth(),child.getTop()+child.getDrawingCache().getHeight());
				mLabelPaint.setAlpha((int) (mLabelFactor*255));
				canvas.drawBitmap(child.getDrawingCache(), rl1, rl2, mLabelPaint);
			}
			scale=((width)/child.getWidth());
			r3 = tmp[1].getBounds();
			xx=(child.getWidth()/2)-(r3.width()/2);
			canvas.save();
			canvas.translate(x+xx, y+child.getPaddingTop());
			canvas.scale(scale, scale);
			tmp[1].draw(canvas);
			canvas.restore();
		}else{
			if(mDrawLabels){
				child.setDrawingCacheEnabled(true);
				if(child.getDrawingCache()!=null){
					mPaint.setAlpha(255);
					canvas.drawBitmap(child.getDrawingCache(), child.getLeft(), child.getTop(), mPaint);
				}else{
					canvas.save();
					canvas.translate(child.getLeft(), child.getTop());
					child.draw(canvas);
					canvas.restore();
				}
			}else{
				r3 = tmp[1].getBounds();
				int xx=(child.getWidth()/2)-(r3.width()/2);
				canvas.save();
				canvas.translate(child.getLeft()+xx, child.getTop()+child.getPaddingTop());
				tmp[1].draw(canvas);
				canvas.restore();
			}
		}
		canvas.restoreToCount(saveCount);
		return true;
	}
	/**
	 * Open/close public methods
	 */
	public void open(boolean animate, int speed){
		if(mStatus!=OPENING){
			mAnimationDuration=speed;
			if(animate){
				isAnimating=true;
				mStatus=OPENING;
			}else{
				isAnimating=false;
				mStatus=OPEN;
				dispatchFadingEvent(OnFadingListener.OPEN);
			}
			startTime=0;
			invalidate();
		}
	}
	public void close(boolean animate, int speed){
		if(mStatus!=CLOSING){
			mAnimationDuration=speed;
			if(animate){
				mStatus=CLOSING;
				isAnimating=true;
			}else{
				mStatus=CLOSED;
				isAnimating=false;
				dispatchFadingEvent(OnFadingListener.CLOSE);
			}
			startTime=0;
			invalidate();
		}
	}
    /**
     * Interface definition for a callback to be invoked when an open/close animation
     * starts/ends
     */
    public interface OnFadingListener {
        public static final int OPEN=1;
        public static final int CLOSE=2;
        void onUpdate(int Status);
        void onAlphaChange(float alphaPercent);
    }
    public void setOnFadingListener(OnFadingListener listener) {
        mOnFadingListener = listener;
    }
    /**
     * Dispatches a trigger event to listener. Ignored if a listener is not set.
     * @param whichHandle the handle that triggered the event.
     */
    private void dispatchFadingEvent(int status) {
        if (mOnFadingListener != null) {
            mOnFadingListener.onUpdate(status);
        }
    }
    /**
     * Dispatches a trigger event to listener. Ignored if a listener is not set.
     * @param whichHandle the handle that triggered the event.
     */
    private void dispatchFadingAlphaEvent(float alphaPercent) {
        if (mOnFadingListener != null) {
            mOnFadingListener.onAlphaChange(alphaPercent);
        }
    }
    public void updateLabelVars(Context context){
    	mDrawLabels=AlmostNexusSettingsHelper.getDrawerLabels(context);
    	mFadeDrawLabels=AlmostNexusSettingsHelper.getFadeDrawerLabels(context);
    }
}
