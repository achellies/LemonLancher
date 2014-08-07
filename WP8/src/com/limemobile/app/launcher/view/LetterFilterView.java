
package com.limemobile.app.launcher.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class LetterFilterView extends View {
    public static final String[] mLetters = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
            "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y",
            "Z", "#"
    };
    private OnTouchingLetterChangedListener onTouchingLetterChangedListener;
    private int choose = -1;
    private Paint mPaint = new Paint();
    private boolean mFocused = false;

    public LetterFilterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public LetterFilterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LetterFilterView(Context context) {
        super(context);
        init(context);
    }
    
    private void init(Context context) {
        setFocusable(true);
        setClickable(true);
        setFocusableInTouchMode(true);
    }

    
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float height = getHeight();
        float width = getWidth();

        mPaint.setAntiAlias(true);

        RectF rect = new RectF(0, 0, width, height);
        if (mFocused)
            mPaint.setColor(Color.DKGRAY);
        else
            mPaint.setColor(Color.TRANSPARENT);
        canvas.drawRoundRect(rect, width / 2.0f,
                width / 2.0f, mPaint);
        
        float topMargin = 10.0f;
        float bottomMargin = 10.0f;

        float singleHeight = (height - topMargin - bottomMargin) / mLetters.length;
        for (int i = 0; i < mLetters.length; i++) {
            float xPos = width / 2 - mPaint.measureText(mLetters[i]);
            // float yPos = singleHeight * i + singleHeight;
            Rect bounds = new Rect();
            mPaint.getTextBounds(mLetters[i], 0, 1, bounds);

            int textHeight = Math.abs(bounds.height());
            float yPos = getTop() + topMargin + singleHeight * i + singleHeight
                    - ((singleHeight - textHeight) / 2);
            mPaint.setTextSize(36.0f);
            mPaint.setAntiAlias(true);
            if (i == choose) {
                mPaint.setColor(Color.parseColor("#40FAFAFA"));
                float centerX = width / 2;
                float centerY = yPos - bounds.height() / 2.0f;
                canvas.drawCircle(centerX, centerY, width / 2.0f, mPaint);
            }

            mPaint.setColor(Color.GRAY);
            canvas.drawText(mLetters[i], xPos, yPos, mPaint);
            mPaint.reset();
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final int action = event.getAction();
        final float y = event.getY();
        final int oldChoose = choose;
        final OnTouchingLetterChangedListener listener = onTouchingLetterChangedListener;
        final int c = (int) (y / getHeight() * mLetters.length);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mFocused = true;
                if (oldChoose != c) {
                    if (c >= 0 && c < mLetters.length) {
                        if (listener != null)
                            listener.onTouchingLetterChanged(mLetters[c]);
                        choose = c;
                        invalidate();
                    }
                }

                break;
            case MotionEvent.ACTION_MOVE:
                if (oldChoose != c) {
                    if (c >= 0 && c < mLetters.length) {
                        if (listener != null)
                            listener.onTouchingLetterChanged(mLetters[c]);
                        choose = c;
                        invalidate();
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                mFocused = false;
                choose = -1;
                invalidate();
                break;
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    public void setOnTouchingLetterChangedListener(
            OnTouchingLetterChangedListener onTouchingLetterChangedListener) {
        this.onTouchingLetterChangedListener = onTouchingLetterChangedListener;
    }

    public interface OnTouchingLetterChangedListener {
        public void onTouchingLetterChanged(String s);
    }

}
