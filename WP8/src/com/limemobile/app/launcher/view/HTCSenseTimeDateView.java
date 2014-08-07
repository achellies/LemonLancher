package com.limemobile.app.launcher.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.limemobile.app.launcher.wp8.R;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class HTCSenseTimeDateView extends RelativeLayout {
    
    private static final String INTENT_ACTION_TIME_TICK = "android.intent.action.TIME_TICK";
    
    private final static String M12 = "h:mm";
    private final static String M24 = "kk:mm";
    
    private Context mContext;
    
    private Calendar mCalendar;

    private String mFormat;
    private HTCSenseTimeView mTimeView;
    private TextView mAmPmView;
    private TextView mDateView;
    private TextView mWeatherTextView;
    private ImageView mWeatherImgView;
    
    private String mAmString;
    private String mPmString;
    
    private boolean mReceiverRegistered = false;
    
    public static class HTCSenseTimeView extends TextView {
        
        private HashMap<String, BitmapDrawable> mNumberDrawables = new HashMap<String, BitmapDrawable>();
        
        private int mIconWidth;
        private int mIconHeight;

        public HTCSenseTimeView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public HTCSenseTimeView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }
        
        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }

        @Override
        protected void onFinishInflate() {
            super.onFinishInflate();
            
            Context context = getContext();
            BitmapDrawable drawable = new BitmapDrawable(context.getResources().openRawResource(R.drawable.time_0));
            mNumberDrawables.put("0", drawable);
            
            float density = context.getResources().getDisplayMetrics().density;
            
            mIconWidth = (int) (drawable.getIntrinsicWidth() * density);
            mIconHeight = (int) (drawable.getIntrinsicHeight() * density);
            
            drawable = new BitmapDrawable(context.getResources().openRawResource(R.drawable.time_1));
            mNumberDrawables.put("1", drawable);
            
            drawable = new BitmapDrawable(context.getResources().openRawResource(R.drawable.time_2));
            mNumberDrawables.put("2", drawable);
            
            drawable = new BitmapDrawable(context.getResources().openRawResource(R.drawable.time_3));
            mNumberDrawables.put("3", drawable);
            
            drawable = new BitmapDrawable(context.getResources().openRawResource(R.drawable.time_4));
            mNumberDrawables.put("4", drawable);
            
            drawable = new BitmapDrawable(context.getResources().openRawResource(R.drawable.time_5));
            mNumberDrawables.put("5", drawable);
            
            drawable = new BitmapDrawable(context.getResources().openRawResource(R.drawable.time_6));
            mNumberDrawables.put("6", drawable);
            
            drawable = new BitmapDrawable(context.getResources().openRawResource(R.drawable.time_7));
            mNumberDrawables.put("7", drawable);
            
            drawable = new BitmapDrawable(context.getResources().openRawResource(R.drawable.time_8));
            mNumberDrawables.put("8", drawable);
            
            drawable = new BitmapDrawable(context.getResources().openRawResource(R.drawable.time_9));
            mNumberDrawables.put("9", drawable);
        }

        @Override
        protected void onDetachedFromWindow() {
            if (!mNumberDrawables.isEmpty()) {
                Set<String> keys = mNumberDrawables.keySet();
                Iterator<String> iterator = keys.iterator();
                while (iterator.hasNext()) {
                    BitmapDrawable drawable = mNumberDrawables.get(iterator.next());
                    if (drawable != null)
                        drawable.setCallback(null);
                    if (drawable != null && drawable.getBitmap() != null && !drawable.getBitmap().isRecycled())
                        drawable.getBitmap().recycle();
                }
            }
            mNumberDrawables.clear();
            super.onDetachedFromWindow();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            //super.onDraw(canvas);
        	String mTimeString = getText().toString();
            if (!TextUtils.isEmpty(mTimeString)) {
                int width = getWidth();
                int height = getHeight();
                
                int length = mTimeString.length();
                
                int left = getLeft() /*+ (width - length * mIconWidth) / 2*/;
                int top = getTop() /*+ (height - mIconHeight) / 2*/;
                
                int xOffset = 0;
                
                for (int index = 0; index < length; ++index) {
                    String time = String.valueOf(mTimeString.charAt(index));
                    
                    if (mNumberDrawables.containsKey(time)) {
                        BitmapDrawable drawable = mNumberDrawables.get(time);
                        
                        Rect bounds = new Rect();
                        
                        bounds.left = left + index * mIconWidth - xOffset;
                        bounds.top = top;
                        bounds.right = bounds.left + mIconWidth;
                        bounds.bottom = bounds.top + mIconHeight;
                        
                        drawable.setBounds(bounds);
                        drawable.draw(canvas);
                    } else {
                        xOffset = mIconWidth * 3 / 4;
                    }
                }
            }
        }
    }

    public HTCSenseTimeDateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        mContext = context;
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        if (View.VISIBLE == visibility && !mReceiverRegistered) {
            mReceiverRegistered = true;
            IntentFilter intentfilter = new IntentFilter();
            intentfilter.addAction(INTENT_ACTION_TIME_TICK);
            mContext.registerReceiver(mReceiver, intentfilter);
        } else if (mReceiverRegistered) {
            mReceiverRegistered = false;
            mContext.unregisterReceiver(mReceiver);
        }
        super.onWindowVisibilityChanged(visibility);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        
        mTimeView = (HTCSenseTimeView) findViewById(R.id.time);
        mAmPmView = (TextView) findViewById(R.id.ampm);
        mDateView = (TextView) findViewById(R.id.date);
        mWeatherTextView = (TextView) findViewById(R.id.weather_text);
        mWeatherImgView = (ImageView) findViewById(R.id.weather_img);
        
        String[] ampm = new DateFormatSymbols().getAmPmStrings();
        mAmString = ampm[0];
        mPmString = ampm[1];
        
        mCalendar = Calendar.getInstance();
        
        mWeatherTextView.setVisibility(View.GONE);
        mWeatherImgView.setVisibility(View.GONE);
        
        updateTime();
    }
    
    private void setShowAmPm(boolean show) {
        mAmPmView.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void setIsMorning(boolean isMorning) {
        mAmPmView.setText(isMorning ? mAmString : mPmString);
    }
    
    
    private void updateTime() {
        mFormat = android.text.format.DateFormat.is24HourFormat(mContext)
                ? M24 : M12;
        setShowAmPm(mFormat.equals(M12));
        setIsMorning(mCalendar.get(Calendar.AM_PM) == 0);
        
        mCalendar.setTimeInMillis(System.currentTimeMillis());
        CharSequence newTime = DateFormat.format(mFormat, mCalendar);
        mTimeView.setText(newTime.toString());
        
        mDateView.setText(DateFormat.format("MMMM dd EEEE", new Date()));
        //mDateView.setText(DateFormat.format("MMMM dd EEEE yyyy", new Date()));
        
        invalidate();
    }
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(INTENT_ACTION_TIME_TICK)) {
                updateTime();
            }
        }
        
    };
}
