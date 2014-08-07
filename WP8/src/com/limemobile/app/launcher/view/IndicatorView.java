package com.limemobile.app.launcher.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.limemobile.app.launcher.wp8.R;

public class IndicatorView extends ImageView {
	private Context mContext;
	private int mCurrent = 0;
	private int mTotal = 0;
	private BitmapDrawable mNormalIndicator;
	private BitmapDrawable mSelectedIndicator;

	public IndicatorView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	public IndicatorView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public IndicatorView(Context context) {
		super(context);
		init(context);
	}

	private void init(Context context) {
		mContext = context;
		
		mNormalIndicator = new BitmapDrawable(mContext.getResources().openRawResource(R.drawable.indicator_normal));
		mSelectedIndicator = new BitmapDrawable(mContext.getResources().openRawResource(R.drawable.indicator_focus));
	}
	
	public void setIndicator(int indicator) {
		mCurrent = indicator;
	}
	
	public void setTotalCount(int count) {
		mTotal = count;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		float scale = mContext.getResources().getDisplayMetrics().scaledDensity;
		
		int width = getWidth();
		int height = getHeight();
		int iconWidth = (int) (mSelectedIndicator.getIntrinsicWidth() * scale);
		int iconHeight = (int) (mSelectedIndicator.getIntrinsicHeight() * scale);
		
		int left = getLeft() + (width - (mTotal * iconWidth)) / 2;
		int top = /*getTop() + */(height - iconHeight) / 2;
		
		for (int index = 0; index < mTotal; ++index) {
			BitmapDrawable bitmap = null;
			if (index == mCurrent)
				bitmap = mSelectedIndicator;
			else
				bitmap = mNormalIndicator;
			
			Rect bounds = new Rect(left + index * iconWidth, top,
					left + (index + 1)* iconWidth, top + iconHeight);
			bitmap.setBounds(bounds);
			bitmap.draw(canvas);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		
//        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);
        
//        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
//        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);
        float scale = mContext.getResources().getDisplayMetrics().scaledDensity;
		setMeasuredDimension(widthSpecSize, (int)(mSelectedIndicator.getIntrinsicHeight() * scale));
	}

	@Override
	protected void onDetachedFromWindow() {
		if (mSelectedIndicator != null && mSelectedIndicator.getBitmap() != null && !mSelectedIndicator.getBitmap().isRecycled()) {
		    mSelectedIndicator.setCallback(null);
			mSelectedIndicator.getBitmap().recycle();
		}
		if (mNormalIndicator != null && mNormalIndicator.getBitmap() != null && !mNormalIndicator.getBitmap().isRecycled()) {
		    mNormalIndicator.setCallback(null);
			mNormalIndicator.getBitmap().recycle();
		}
		super.onDetachedFromWindow();
	}
}
