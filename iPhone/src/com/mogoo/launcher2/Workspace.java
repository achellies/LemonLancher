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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.Interpolator;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;
import android.widget.EditText;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.CellLayout.LayoutParams;
import com.mogoo.launcher2.animation.Mogoo_IconFolderAnimation;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.search.ui.SearchLayout;
import com.mogoo.launcher2.utils.Mogoo_BitmapCache;
import com.mogoo.launcher2.utils.Mogoo_BitmapUtils;
import com.mogoo.launcher2.utils.Mogoo_ClearBase;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;
import com.mogoo.launcher2.utils.Mogoo_Utilities;
import com.mogoo.launcher2.utils.Mogoo_WorkspaceInface;

/**
 * The workspace is a wide area with a wallpaper and a finite number of screens.
 * Each screen contains a number of icons, folders or widgets the user can
 * interact with. A workspace is meant to be used with a fixed width only.
 */
public class Workspace extends ViewGroup implements DropTarget, DragSource, DragScroller,
        Mogoo_ClearBase, Mogoo_WorkspaceInface {
    private static final String TAG = "Launcher.Workspace";

    private static final int INVALID_SCREEN = -1;

    /**
     * The velocity at which a fling gesture will cause us to snap to the next
     * screen
     */
    private static final int SNAP_VELOCITY = 50;

    private final WallpaperManager mWallpaperManager;

    private int mDefaultScreen;

    private boolean mFirstLayout = true;

    private int mCurrentScreen;

    private int mNextScreen = INVALID_SCREEN;

    // add by huangyue 2011-1-24
    private int mSearchScreen = Mogoo_GlobalConfig.getSearchScreen();

    private Scroller mScroller;

    private VelocityTracker mVelocityTracker;

    /**
     * CellInfo for the cell that is currently being dragged
     */
    private CellLayout.CellInfo mDragInfo = new CellLayout.CellInfo();;

    /**
     * Target drop area calculated during last acceptDrop call.
     */
    private int[] mTargetCell = null;

    private float mLastMotionX;

    private float mLastMotionY;

    private final static int TOUCH_STATE_REST = 0;

    private final static int TOUCH_STATE_SCROLLING = 1;

    private int mTouchState = TOUCH_STATE_REST;

    private OnLongClickListener mLongClickListener;

    private Launcher mLauncher;
    
    private Mogoo_BitmapCache mIconCache;

    private DragController mDragController;

    /**
     * Cache of vacant cells, used during drag events and invalidated as needed.
     */
    private CellLayout.CellInfo mVacantCache = null;

    private int[] mTempCell = new int[2];

    private int[] mTempEstimate = new int[2];

    private boolean mAllowLongPress = true;

    private int mTouchSlop;

    private int mMaximumVelocity;

    private static final int INVALID_POINTER = -1;

    private int mActivePointerId = INVALID_POINTER;

    // private Drawable mPreviousIndicator;
    // private Drawable mNextIndicator;

    private static final float NANOTIME_DIV = 1000000000.0f;

    private static final float SMOOTHING_SPEED = 0.5f;

    private static final float SMOOTHING_CONSTANT = (float) (0.016 / Math.log(SMOOTHING_SPEED));

    private float mSmoothingTime;

    private float mTouchX;

    private WorkspaceOvershootInterpolator mScrollInterpolator;

    private static final float BASELINE_FLING_VELOCITY = 2500.f;

    private static final float FLING_VELOCITY_INFLUENCE = 0.4f;

    
    // --------------------motone field statement---------
 
    private Handler handler = new Handler();
    //判断是否在本区域内拖动
    private boolean isLocaleDrag = false;

	private boolean moveWidget = false;
	private Mogoo_IconFolderAnimation iconFolderAnimation; 
	
	//add by 张永辉 2011-7-23 保存上一次找到的位置
	private int lastFindIndex = 0 ;
	//end add
	
	//add by huangyue
	private long dragOverTime = 0;
	private long DRAG_GEP = 450;
	private int scrollAdd = 0;
	private final int mOverscrollDistance = 200;
	//end

    // ---------------------end---------------------------
	
	 
		public boolean isLocaleDrag() {
			return isLocaleDrag;
		}

		public void setLocaleDrag(boolean isLocaleDrag) {
			this.isLocaleDrag = isLocaleDrag;
		}

    private static class WorkspaceOvershootInterpolator implements Interpolator {
        private static final float DEFAULT_TENSION = 0.9f;

        private float mTension;

        public WorkspaceOvershootInterpolator() {
            mTension = DEFAULT_TENSION;
        }

        public void setDistance(int distance) {
            mTension = distance > 0 ? DEFAULT_TENSION / distance : DEFAULT_TENSION;
        }

        public void disableSettle() {
            mTension = 0.f;
        }

        public float getInterpolation(float t) 
        {
            // _o(t) = t * t * ((tension + 1) * t + tension)
            // o(t) = _o(t - 1) + 1
            t -= 1.0f;
            return t * t * ((mTension + 1) * t + mTension) + 1.0f;
        }
    }

    /**
     * Used to inflate the Workspace from XML.
     * 
     * @param context The application's context.
     * @param attrs The attribtues set containing the Workspace's customization
     *            values.
     */
    public Workspace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Used to inflate the Workspace from XML.
     * 
     * @param context The application's context.
     * @param attrs The attribtues set containing the Workspace's customization
     *            values.
     * @param defStyle Unused.
     */
    public Workspace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mWallpaperManager = WallpaperManager.getInstance(context);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Workspace, defStyle, 0);
        mDefaultScreen = a.getInt(R.styleable.Workspace_defaultScreen, 1);
        a.recycle();
        
        scrollAdd = (int)context.getResources().getDimension(R.dimen.scroll_fix_value);

        setHapticFeedbackEnabled(false);
        initWorkspace();
    }

    /**
     * Initializes various states for this workspace.
     */
    private void initWorkspace() {
        Context context = getContext();
        mScrollInterpolator = new WorkspaceOvershootInterpolator();
        mScroller = new Scroller(context, mScrollInterpolator);
        mCurrentScreen = mDefaultScreen;
        Launcher.setScreen(mCurrentScreen);
        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        mIconCache = app.getIconCache();

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        // 初始化全局参数－屏幕总数和默认显示屏
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_DEFAULT_SCREEN, mDefaultScreen);
        iconFolderAnimation = new Mogoo_IconFolderAnimation(getContext(), mIconCache);
    }
    
    public Mogoo_IconFolderAnimation getIconFolderAnimation() {
        return iconFolderAnimation;
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, index, params);
    }

    @Override
    public void addView(View child) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, index);
    }

    @Override
    public void addView(View child, int width, int height) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, width, height);
    }

    @Override
    public void addView(View child, LayoutParams params) {
        if (!(child instanceof CellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, params);
    }

    /**
     * @return The open folder on the current screen, or null if there is none
     */
    Folder getOpenFolder() {
        CellLayout currentScreen = (CellLayout) getChildAt(mCurrentScreen);
        int count = currentScreen.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = currentScreen.getChildAt(i);
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
            if (lp.cellHSpan == 4 && lp.cellVSpan == 4 && child instanceof Folder) {
                return (Folder) child;
            }
        }
        return null;
    }

    ArrayList<Folder> getOpenFolders() {
        final int screens = getChildCount();
        ArrayList<Folder> folders = new ArrayList<Folder>(screens);

        for (int screen = 0; screen < screens; screen++) {
            CellLayout currentScreen = (CellLayout) getChildAt(screen);
            int count = currentScreen.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = currentScreen.getChildAt(i);
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
                if (lp.cellHSpan == 4 && lp.cellVSpan == 4 && child instanceof Folder) {
                    folders.add((Folder) child);
                    break;
                }
            }
        }

        return folders;
    }

    boolean isDefaultScreenShowing() {
        return mCurrentScreen == mDefaultScreen;
    }

    /**
     * Returns the index of the currently displayed screen.
     * 
     * @return The index of the currently displayed screen.
     */
    public int getCurrentScreen() {
        //return mCurrentScreen;
    	return mScroller.isFinished() ? mCurrentScreen : mNextScreen;
    }

    /**
     * Sets the current screen.
     * 
     * @param currentScreen
     */
    void setCurrentScreen(int currentScreen) {
        if (!mScroller.isFinished())
            mScroller.abortAnimation();
        clearVacantCache();
        mCurrentScreen = Math.max(0, Math.min(currentScreen, getChildCount() - 1));
        // mPreviousIndicator.setLevel(mCurrentScreen);
        // mNextIndicator.setLevel(mCurrentScreen);
        scrollTo(mCurrentScreen * getWidth(), 0);
        updateWallpaperOffset();
        mLauncher.setIndicator(currentScreen);
        invalidate();
    }

    /**
     * Adds the specified child in the current screen. The position and
     * dimension of the child are defined by x, y, spanX and spanY.
     * 
     * @param child The child to add in one of the workspace's screens.
     * @param x The X position of the child in the screen's grid.
     * @param y The Y position of the child in the screen's grid.
     * @param spanX The number of cells spanned horizontally by the child.
     * @param spanY The number of cells spanned vertically by the child.
     */
    void addInCurrentScreen(View child, int x, int y, int spanX, int spanY) {
        addInScreen(child, mCurrentScreen, x, y, spanX, spanY, false);
    }

    /**
     * Adds the specified child in the current screen. The position and
     * dimension of the child are defined by x, y, spanX and spanY.
     * 
     * @param child The child to add in one of the workspace's screens.
     * @param x The X position of the child in the screen's grid.
     * @param y The Y position of the child in the screen's grid.
     * @param spanX The number of cells spanned horizontally by the child.
     * @param spanY The number of cells spanned vertically by the child.
     * @param insert When true, the child is inserted at the beginning of the
     *            children list.
     */
    void addInCurrentScreen(View child, int x, int y, int spanX, int spanY, boolean insert) {
        addInScreen(child, mCurrentScreen, x, y, spanX, spanY, insert);
    }

    /**
     * Adds the specified child in the specified screen. The position and
     * dimension of the child are defined by x, y, spanX and spanY.
     * 
     * @param child The child to add in one of the workspace's screens.
     * @param screen The screen in which to add the child.
     * @param x The X position of the child in the screen's grid.
     * @param y The Y position of the child in the screen's grid.
     * @param spanX The number of cells spanned horizontally by the child.
     * @param spanY The number of cells spanned vertically by the child.
     */
    void addInScreen(View child, int screen, int x, int y, int spanX, int spanY) {
        addInScreen(child, screen, x, y, spanX, spanY, false);
    }

    /**
     * Adds the specified child in the specified screen. The position and
     * dimension of the child are defined by x, y, spanX and spanY.
     * 
     * @param child The child to add in one of the workspace's screens.
     * @param screen The screen in which to add the child.
     * @param x The X position of the child in the screen's grid.
     * @param y The Y position of the child in the screen's grid.
     * @param spanX The number of cells spanned horizontally by the child.
     * @param spanY The number of cells spanned vertically by the child.
     * @param insert When true, the child is inserted at the beginning of the
     *            children list.
     */
    void addInScreen(View child, int screen, int x, int y, int spanX, int spanY, boolean insert) {
        if (screen < 0 || screen >= getChildCount()) {
            Log.e(TAG, "The screen must be >= 0 and < " + getChildCount() + " (was " + screen
                    + "); skipping child");
            return;
        }

        clearVacantCache();

        final CellLayout group = (CellLayout) getChildAt(screen);
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
        if (lp == null) {
            lp = new CellLayout.LayoutParams(x, y, spanX, spanY);
        } else {
            lp.cellX = x;
            lp.cellY = y;
            lp.cellHSpan = spanX;
            lp.cellVSpan = spanY;
        }
        group.addView(child, insert ? 0 : -1, lp);
        if (!(child instanceof Folder)) {
            child.setHapticFeedbackEnabled(false);
            child.setOnLongClickListener(mLongClickListener);
        }
//        if (child instanceof DropTarget) {
//            mDragController.addDropTarget((DropTarget) child);
//        }
    }

    CellLayout.CellInfo findAllVacantCells(boolean[] occupied) {
        CellLayout group = (CellLayout) getChildAt(mCurrentScreen);
        if (group != null) {
            return group.findAllVacantCells(occupied, null);
        }
        return null;
    }

    private void clearVacantCache() {
        if (mVacantCache != null) {
            mVacantCache.clearVacantCells();
            mVacantCache = null;
        }
    }

    /**
     * Registers the specified listener on each screen contained in this
     * workspace.
     * 
     * @param l The listener used to respond to long clicks.
     */
    @Override
    public void setOnLongClickListener(OnLongClickListener l) {
        mLongClickListener = l;
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).setOnLongClickListener(l);
        }
    }

    private void updateWallpaperOffset() {
        // delete by 张永辉 2011-1-23 滑屏时让壁纸停止移动
        // updateWallpaperOffset(getChildAt(getChildCount() - 1).getRight() -
        // (mRight - mLeft));
        // end
    }

    private void updateWallpaperOffset(int scrollRange) {
        IBinder token = getWindowToken();
        if (token != null) {
            mWallpaperManager.setWallpaperOffsetSteps(1.0f / (getChildCount() - 1), 0);
            mWallpaperManager.setWallpaperOffsets(getWindowToken(),
                    Math.max(0.f, Math.min(getScrollX() / (float) scrollRange, 1.f)), 0);
        }
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
        mTouchX = x;
        mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mTouchX = mScroller.getCurrX();
            super.scrollTo((int)mTouchX, getScrollY());
            mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
            super.scrollTo(mScroller.getCurrX(), mScroller.getCurrY());
            updateWallpaperOffset();
            postInvalidate();
        } else if (mNextScreen != INVALID_SCREEN) {
            mCurrentScreen = Math.max(0, Math.min(mNextScreen, getChildCount() - 1));
            // mPreviousIndicator.setLevel(mCurrentScreen);
            // mNextIndicator.setLevel(mCurrentScreen);
            Launcher.setScreen(mCurrentScreen);
            mNextScreen = INVALID_SCREEN;
            clearChildrenCache();
        } else if (mTouchState == TOUCH_STATE_SCROLLING) {
//            final float now = System.nanoTime() / NANOTIME_DIV;
//            final float e = (float) Math.exp((now - mSmoothingTime) / SMOOTHING_CONSTANT);
//            final float dx = mTouchX - mScrollX;
//            mScrollX += dx * e;
//            mSmoothingTime = now;
//
//            // Keep generating points as long as we're more than 1px away from
//            // the target
//            if (dx > 1.f || dx < -1.f) {
//                updateWallpaperOffset();
//                postInvalidate();
//            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        boolean restore = false;
        int restoreCount = 0;

        // ViewGroup.dispatchDraw() supports many features we don't need:
        // clip to padding, layout animation, animation listener, disappearing
        // children, etc. The following implementation attempts to fast-track
        // the drawing dispatch by drawing only what we know needs to be drawn.

        boolean fastDraw = mTouchState != TOUCH_STATE_SCROLLING && mNextScreen == INVALID_SCREEN;
        // If we are not scrolling or flinging, draw only the current screen
        if (fastDraw) {
            drawChild(canvas, getChildAt(mCurrentScreen), getDrawingTime());
        } else {
            final long drawingTime = getDrawingTime();
            final float scrollPos = (float) getScrollX() / getWidth();
            final int leftScreen = (int) scrollPos;
            final int rightScreen = leftScreen + 1;
            if (leftScreen >= 0) {
            	if(leftScreen>=getChildCount()){
                    drawChild(canvas, getChildAt(getChildCount()-1), drawingTime);
            	}else{
                    drawChild(canvas, getChildAt(leftScreen), drawingTime);
            	}
            }
            if (scrollPos != leftScreen && rightScreen < getChildCount()) {
            	 if(canvas == null){
                 	postInvalidate();
                  } else {
                	  drawChild(canvas, getChildAt(rightScreen), drawingTime);
                  }
            }
        }

        if (restore) {
            canvas.restoreToCount(restoreCount);
        }
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
//        computeScroll();
        mDragController.setWindowToken(getWindowToken());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (heightMode != MeasureSpec.EXACTLY) {
            throw new IllegalStateException("Workspace can only be used in EXACTLY mode.");
        }

        // The children are given the same width and height as the workspace
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(widthMeasureSpec, heightMeasureSpec);
        }

        if (mFirstLayout) {
            setHorizontalScrollBarEnabled(false);
            scrollTo(mCurrentScreen * width, 0);
            setHorizontalScrollBarEnabled(true);
            updateWallpaperOffset(width * (getChildCount() - 1));
            mFirstLayout = false;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int childLeft = 0;

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != View.GONE) {
                final int childWidth = child.getMeasuredWidth();
                child.layout(childLeft, 0, childLeft + childWidth, child.getMeasuredHeight());
                childLeft += childWidth;
            }
        }
    }

    @Override
    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        int screen = indexOfChild(child);
        if (screen != mCurrentScreen || !mScroller.isFinished()) {
            if (!mLauncher.isWorkspaceLocked()) {
                snapToScreen(screen);
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
        if (!mLauncher.isAllAppsVisible()) {
            final Folder openFolder = getOpenFolder();
            if (openFolder != null) {
                return openFolder.requestFocus(direction, previouslyFocusedRect);
            } else {
                int focusableScreen;
                if (mNextScreen != INVALID_SCREEN) {
                    focusableScreen = mNextScreen;
                } else {
                    focusableScreen = mCurrentScreen;
                }
                getChildAt(focusableScreen).requestFocus(direction, previouslyFocusedRect);
            }
        }
        return false;
    }

    @Override
    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (direction == View.FOCUS_LEFT) {
            if (getCurrentScreen() > 0) {
                snapToScreen(getCurrentScreen() - 1);
                return true;
            }
        } else if (direction == View.FOCUS_RIGHT) {
            if (getCurrentScreen() < getChildCount() - 1) {
                snapToScreen(getCurrentScreen() + 1);
                return true;
            }
        }
        return super.dispatchUnhandledMove(focused, direction);
    }

    @Override
    public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
        if (!mLauncher.isAllAppsVisible()) {
            final Folder openFolder = getOpenFolder();
            if (openFolder == null) {
                getChildAt(mCurrentScreen).addFocusables(views, direction);
                if (direction == View.FOCUS_LEFT) {
                    if (mCurrentScreen > 0) {
                        getChildAt(mCurrentScreen - 1).addFocusables(views, direction);
                    }
                } else if (direction == View.FOCUS_RIGHT) {
                    if (mCurrentScreen < getChildCount() - 1) {
                        getChildAt(mCurrentScreen + 1).addFocusables(views, direction);
                    }
                }
            } else {
                openFolder.addFocusables(views, direction);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            if (mLauncher.isWorkspaceLocked() || mLauncher.isAllAppsVisible()) {
                return false;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final boolean workspaceLocked = mLauncher.isWorkspaceLocked();
        final boolean allAppsVisible = mLauncher.isAllAppsVisible();
        if (workspaceLocked || allAppsVisible) {
            return false; // We don't want the events. Let them fall through to
                          // the all apps view.
        }

        /*
         * This method JUST determines whether we want to intercept the motion.
         * If we return true, onTouchEvent will be called and we do the actual
         * scrolling there.
         */

        /*
         * Shortcut the most recurring case: the user is in the dragging state
         * and he is moving his finger. We want to intercept this motion.
         */
        final int action = ev.getAction();
        if ((action == MotionEvent.ACTION_MOVE) && (mTouchState != TOUCH_STATE_REST)) {
            return true;
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_MOVE: {
                /*
                 * mIsBeingDragged == false, otherwise the shortcut would have
                 * caught it. Check whether the user has moved far enough from
                 * his original down touch.
                 */

                /*
                 * Locally do absolute value. mLastMotionX is set to the y value
                 * of the down event.
                 */
                final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                final float x = ev.getX(pointerIndex);
                final float y = ev.getY(pointerIndex);
                final int xDiff = (int) Math.abs(x - mLastMotionX);
                final int yDiff = (int) Math.abs(y - mLastMotionY);

                final int touchSlop = mTouchSlop;
                boolean xMoved = xDiff > touchSlop;
                boolean yMoved = yDiff > touchSlop;

                if (xMoved || yMoved) {

                    if (xMoved) {
                        // Scroll if the user moved far enough along the X axis
                        mTouchState = TOUCH_STATE_SCROLLING;
                        mLastMotionX = x;
                        mTouchX = getScrollX();
                        mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
                        enableChildrenCache(mCurrentScreen - 1, mCurrentScreen + 1);
                    }
                    // Either way, cancel any pending longpress
                    if (mAllowLongPress) {
                        mAllowLongPress = false;
                        // Try canceling the long press. It could also have been
                        // scheduled
                        // by a distant descendant, so use the mAllowLongPress
                        // flag to block
                        // everything
                        final View currentScreen = getChildAt(mCurrentScreen);
                        currentScreen.cancelLongPress();
                    }
                }
                break;
            }

            case MotionEvent.ACTION_DOWN: {
                final float x = ev.getX();
                final float y = ev.getY();
                // Remember location of down touch
                mLastMotionX = x;
                mLastMotionY = y;
                mActivePointerId = ev.getPointerId(0);
                mAllowLongPress = true;

                /*
                 * If being flinged and user touches the screen, initiate drag;
                 * otherwise don't. mScroller.isFinished should be false when
                 * being flinged.
                 */
                mTouchState = mScroller.isFinished() ? TOUCH_STATE_REST : TOUCH_STATE_SCROLLING;
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:

                if (mTouchState != TOUCH_STATE_SCROLLING) {
                    final CellLayout currentScreen = (CellLayout) getChildAt(mCurrentScreen);
                    if (!currentScreen.lastDownOnOccupiedCell()) {
                        getLocationOnScreen(mTempCell);
                        // Send a tap to the wallpaper if the last down was on
                        // empty space
                        final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                        mWallpaperManager.sendWallpaperCommand(getWindowToken(),
                                "android.wallpaper.tap",
                                mTempCell[0] + (int) ev.getX(pointerIndex),
                                mTempCell[1] + (int) ev.getY(pointerIndex), 0, null);
                    }
                }

                // Release the drag
                clearChildrenCache();
                mTouchState = TOUCH_STATE_REST;
                mActivePointerId = INVALID_POINTER;
                mAllowLongPress = false;

                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }

                break;

            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }

        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mTouchState != TOUCH_STATE_REST;
    }

    private void onSecondaryPointerUp(MotionEvent ev) {
        final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            // This was our active pointer going up. Choose a new
            // active pointer and adjust accordingly.
            // TODO: Make this decision more intelligent.
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionX = ev.getX(newPointerIndex);
            mLastMotionY = ev.getY(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
  
            /****motone zhanmin 12.01.05 add***************/
            if(Mogoo_GlobalConfig.LOG_DEBUG){
            	Log.d(TAG, "-----------------onSecondaryPointerUp.upAnimotion()--------mTouchState:"+mTouchState+"------");
        	}
        	upAnimotion();
//        	mTouchState = TOUCH_STATE_REST;
//            mActivePointerId = INVALID_POINTER;
        	/****motone zhanmin 12.01.05 add***************/
            
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    /**
     * If one of our descendant views decides that it could be focused now, only
     * pass that along if it's on the current screen. This happens when live
     * folders requery, and if they're off screen, they end up calling
     * requestFocus, which pulls it on screen.
     */
    @Override
    public void focusableViewAvailable(View focused) {
        View current = getChildAt(mCurrentScreen);
        View v = focused;
        while (true) {
            if (v == current) {
                super.focusableViewAvailable(focused);
                return;
            }
            if (v == this) {
                return;
            }
            ViewParent parent = v.getParent();
            if (parent instanceof View) {
                v = (View) v.getParent();
            } else {
                return;
            }
        }
    }

    void enableChildrenCache(int fromScreen, int toScreen) {
        if (fromScreen > toScreen) {
            final int temp = fromScreen;
            fromScreen = toScreen;
            toScreen = temp;
        }

        final int count = getChildCount();

        fromScreen = Math.max(fromScreen, 0);
        toScreen = Math.min(toScreen, count - 1);

        for (int i = fromScreen; i <= toScreen; i++) {
            final CellLayout layout = (CellLayout) getChildAt(i);
            layout.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_LOW);
            layout.setChildrenDrawnWithCacheEnabled(true);
            layout.setChildrenDrawingCacheEnabled(true);
        }
    }

    void clearChildrenCache() {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final CellLayout layout = (CellLayout) getChildAt(i);
            layout.setChildrenDrawnWithCacheEnabled(false);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mLauncher.isWorkspaceLocked()) {
            return false; // We don't want the events. Let them fall through to
                          // the all apps view.
        }
        if (mLauncher.isAllAppsVisible()) {
            // Cancel any scrolling that is in progress.
            if (!mScroller.isFinished()) {
                mScroller.abortAnimation();
            }
            snapToScreen(mCurrentScreen);
            return false; // We don't want the events. Let them fall through to
                          // the all apps view.
        }

        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        final int action = ev.getAction();

        switch (action & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                /*
                 * If being flinged and user touches, stop the fling. isFinished
                 * will be false if being flinged.
                 */
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }

                // Remember where the motion event started
                mLastMotionX = ev.getX();
                mActivePointerId = ev.getPointerId(0);
                if (mTouchState == TOUCH_STATE_SCROLLING) {
                    enableChildrenCache(mCurrentScreen - 1, mCurrentScreen + 1);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mTouchState == TOUCH_STATE_SCROLLING) {
                    // Scroll to follow the motion event
                	try {
                    final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                    final float x = ev.getX(pointerIndex);
                    final float deltaX = mLastMotionX - x;
                    mLastMotionX = x;
                    //当安装并更新图标时禁止划屏
                    if(mScroller.isFinished() && DragController.dragLocked){
                    	break;
                	}

                    // --- add by huangyue 2011-1-25
                    if (switchFilter(deltaX)) {
                    	break;
                    }
                    // ---- end

                    // --- add by weijingchun 2011-1-27
//                    if (switchWidgetFilter(deltaX)) {
//                        break;
//                    }
                    // ---- end

                    if (deltaX < 0) {
//                        if (mTouchX > 0) {
//                            mTouchX += Math.max(-mTouchX, deltaX);
//                            mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
//                            invalidate();
//                        }
                    	  scrollBy((int) Math.max(-getScrollX(), deltaX), 0);
                    } else if (deltaX > 0) {
                	   final int screenWidth = getWidth();
                	   final int whichScreen = (getScrollX() + (screenWidth / 2)) / screenWidth;
                	   int sreachNum = Mogoo_GlobalConfig.getSearchScreen();
                   	if(whichScreen == sreachNum){
                   		float alpha = 1f - ((getScrollX() * 3.5f)/getWidth());
                   		if(alpha < 0){
                   			alpha = 0;
                   		    }
                        final View dragLayer = Mogoo_ComponentBus.getInstance().getActivityComp(R.id.drag_layer,
                                getContext());
                   		dragLayer.getBackground().setAlpha((int)(SearchLayout.SEARCH_SCREEN_ALPHA * alpha));
                   	   }
                	   if(whichScreen == 0){
                		   scrollBy((int) Math.min(getScrollX() != 0 ? getScrollX() : mOverscrollDistance, 36 + deltaX), 0);
                	   }else{
                		   scrollBy((int) Math.min(getScrollX() != 0 ? getScrollX() : mOverscrollDistance, deltaX), 0);
                	   }

                  } else {
                        awakenScrollBars();
                    }
                	} catch (ArrayIndexOutOfBoundsException e) {
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            	if(Mogoo_GlobalConfig.LOG_DEBUG){
                	Log.d(TAG, "-----------------MotionEvent.ACTION_UP--------mTouchState:"+mTouchState+"------");
            	}
            	if (mTouchState == TOUCH_STATE_SCROLLING) {
                    final VelocityTracker velocityTracker = mVelocityTracker;
                    velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
                    final int velocityX = (int) velocityTracker.getXVelocity(mActivePointerId);

                    final int screenWidth = getWidth();
                    final int whichScreen = (getScrollX() + (screenWidth / 2)) / screenWidth;
                    final float scrolledPos = (float) getScrollX() / screenWidth;

                    if (velocityX > SNAP_VELOCITY && mCurrentScreen > 0) {
                        // Fling hard enough to move left.
                        // Don't fling across more than one screen at a time.
                        final int bound = scrolledPos < whichScreen ? mCurrentScreen - 1
                                : mCurrentScreen;
                        snapToScreen(Math.min(whichScreen, bound), velocityX, true);
                    } else if (velocityX < -SNAP_VELOCITY && mCurrentScreen < getChildCount() - 1) {
                        // Fling hard enough to move right
                        // Don't fling across more than one screen at a time.
                        final int bound = scrolledPos > whichScreen ? mCurrentScreen + 1
                                : mCurrentScreen;
                         int screen = Math.max(whichScreen, bound);
                         int sreachNum = Mogoo_GlobalConfig.getSearchScreen();
                        if(mCurrentScreen == sreachNum && screen == 1){
                    	    final View dragLayer = Mogoo_ComponentBus.getInstance().getActivityComp(R.id.drag_layer,
                                    getContext());
                        	    dragLayer.getBackground().setAlpha(0);
                        		dragLayer.invalidate();
                           }
                        snapToScreen(screen, velocityX, true);
                    } else {
                        snapToScreen(whichScreen, 0, true);
                    }

                    if (mVelocityTracker != null) {
                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    }
                }
                mTouchState = TOUCH_STATE_REST;
                mActivePointerId = INVALID_POINTER;
                break;
            case MotionEvent.ACTION_CANCEL:
            	if(Mogoo_GlobalConfig.LOG_DEBUG){
                	Log.d(TAG, "-----------------MotionEvent.ACTION_CANCEL--------mTouchState:"+mTouchState+"------");
            	}
                mTouchState = TOUCH_STATE_REST;
                mActivePointerId = INVALID_POINTER;
                break;
            case MotionEvent.ACTION_POINTER_UP:
            	if(Mogoo_GlobalConfig.LOG_DEBUG){
                	Log.d(TAG, "-----------------MotionEvent.ACTION_POINTER_UP--------mTouchState:"+mTouchState+"------");
            	}
                onSecondaryPointerUp(ev);
                break;
        }

        return true;
    }

    void snapToScreen(int whichScreen) {
    	//modify liangss 2012-3-13
        /*// 防止图标拖入widget屏和搜索屏、防止widget拖入其他屏
        if(Mogoo_GlobalConfig.isSearchScreen(whichScreen)||Mogoo_GlobalConfig.isWidgetScreen(whichScreen)
                || Mogoo_GlobalConfig.isWidgetScreen(this.getCurrentScreen())){
            return ;
        }
        */
        mLauncher.getFolderController().iconFolderInactive();

        //this.toScreenBeforeSort(whichScreen);
        snapToScreen(whichScreen, 0, false);
    }

    void snapToScreen(int screen, int velocity, boolean settle) {
        // if (!mScroller.isFinished()) return;
        if(Mogoo_GlobalConfig.LOG_DEBUG){
        	Log.d(TAG, "------------------snapToScreen()--------------------------");
        }
        final int whichScreen = Math.max(0, Math.min(screen, getChildCount() - 1));

        //add by 袁业奔 2011-11-1 防止抖动的时候进入搜索屏 
        if(mLauncher.getVibrationController().isVibrate){
            if(whichScreen == 0){
            	snapToScreen(1);
            	return;
            }	
        }
        //end
        
        clearVacantCache();
        enableChildrenCache(mCurrentScreen, whichScreen);

        mNextScreen = whichScreen;

        View focusedChild = getFocusedChild();
        if (focusedChild != null && whichScreen != mCurrentScreen
                && focusedChild == getChildAt(mCurrentScreen)) {
            focusedChild.clearFocus();
        }

        final int screenDelta = Math.max(1, Math.abs(whichScreen - mCurrentScreen));
        final int newX = whichScreen * getWidth();
        final int delta = newX - getScrollX();
        int duration = (screenDelta + 1) * 100;

        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        } 

        if (settle) {
            mScrollInterpolator.setDistance(screenDelta);
        } else {
            mScrollInterpolator.disableSettle();
        }

        velocity = Math.abs(velocity);
		if (getScrollX() < 0 || getScrollX() + getWidth() - getChildAt(getChildCount() - 1).getRight() > 0) {
			duration = 450;
		} else if (velocity > 0) {
            duration += (duration / (velocity / BASELINE_FLING_VELOCITY)) * FLING_VELOCITY_INFLUENCE;
        } else {
            duration += 32;
        }

        awakenScrollBars(270);
        mScroller.startScroll(getScrollX(), 0, delta, 0, 270);
//        computeScroll();

        // add by huangyue 2011-1-21 搜索屏转换完成处理
         handler.postDelayed(new Runnable() {
        	 public void run() {
				 	CellLayout whichScreenLayout = (CellLayout) getChildAt(whichScreen);
			        for(int i = 0; i < whichScreenLayout.getChildCount(); i++){
			            View v = whichScreenLayout.getChildAt(i);
			            if(v instanceof Mogoo_BubbleTextView){
			                ((Mogoo_BubbleTextView)v).startVibrate(mIconCache, i % 6);
			            }
			         }
				}
		 }, 300);
// 		// add by huangyue 2011-10-19
 		invalidate();
		handler.postDelayed(new Runnable() {
			public void run() {
				mLauncher.getVibrationController().excuteWorkspaceOtherFlag(
						Workspace.this);
				switchAboutSearch(whichScreen);
				invalidate();
		        // add by 张永辉 2010-1-20 设置屏幕指示器
		        mLauncher.setIndicator(whichScreen);
		        // end
			}
		}, 300);
		// end
    }

    void startDrag(CellLayout.CellInfo cellInfo) {
        View child = cellInfo.cell;
        
        // Make sure the drag was started by a long press as opposed to a long
        // click.
        if (!child.isInTouchMode()) {
            return;
        }

        //mDragInfo = cellInfo;        
        //mDragInfo.screen = mCurrentScreen;

        CellLayout current = ((CellLayout) getChildAt(getCurrentScreen()));
        setDragInfo(current, cellInfo.cellIndex); 
        mDragInfo.spanX = cellInfo.spanX;
        mDragInfo.spanY = cellInfo.spanY;
        mDragInfo.cell = cellInfo.cell;
        
        current.onDragChild(child);
        mDragController.startDrag2(cellInfo, child, this, child.getTag(), DragController.DRAG_ACTION_MOVE);

        invalidate();
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final SavedState state = new SavedState(super.onSaveInstanceState());
        state.currentScreen = mCurrentScreen;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        if (savedState.currentScreen != -1) {
            mCurrentScreen = savedState.currentScreen;
            Launcher.setScreen(mCurrentScreen);
        }
    }

    void addApplicationShortcut(ShortcutInfo info, CellLayout.CellInfo cellInfo) {
        addApplicationShortcut(info, cellInfo, false);
    }

    void addApplicationShortcut(ShortcutInfo info, CellLayout.CellInfo cellInfo,
            boolean insertAtFirst) {
        final CellLayout layout = (CellLayout) getChildAt(cellInfo.screen);
        final int[] result = new int[2];

        layout.cellToPoint(cellInfo.cellX, cellInfo.cellY, result);
        View view = createIcon(info); 
        //addView(layout, view, result[0], result[1], insertAtFirst);
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) {
        //add by 张永辉 
        //如果是widget
    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------start onDrop()--------------------------------------"); 
        } 
    	
    	isLocaleDrag = false; 
        if(dragInfo instanceof LauncherAppWidgetInfo)
        {
            moveWidget(x,y,xOffset,yOffset) ;
            return;
        }
    	
    	Mogoo_FolderController folderController = mLauncher.getFolderController();
    	folderController.setCanActive(false);
    	
    	int curScreen = getCurrentScreen();   
        final CellLayout cellLayout = (CellLayout) this.getChildAt(curScreen);
        Mogoo_FolderWorkspace folderWorkspace = (Mogoo_FolderWorkspace) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.folderWorkspace, mLauncher) ;
    	
    	//如果文件打开了的话，则放手后图标也要加入文件夹中
    	if(folderWorkspace.getVisibility() == View.VISIBLE && folderWorkspace.getLoadingFolder()!=null)
    	{
    	    if(source instanceof Mogoo_DockWorkSpace){
                ShortcutInfo shortcutInfo = (ShortcutInfo) mDragInfo.cell.getTag();
                mIconCache.recycle(shortcutInfo.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
                ((Mogoo_BubbleTextView) mDragInfo.cell).setReflection(false);
                ((Mogoo_BubbleTextView) mDragInfo.cell).setIconReflection(null);
            }
    	    //当folder接收的时候添加到folder里面
    	    if(folderWorkspace.acceptDrop(source, x, y, xOffset, yOffset, dragView, dragInfo)){
                Mogoo_FolderBubbleText loadingFolder = folderWorkspace.getLoadingFolder();
                Mogoo_FolderInfo info = (Mogoo_FolderInfo)loadingFolder.getTag();
                info.addItem(getContext(), (ShortcutInfo) mDragInfo.cell.getTag());
                mIconCache.recycle(info.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
                mIconCache.remove(info.intent.getComponent());
                loadingFolder.setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(info.getIcon(mIconCache)), null, null);
                loadingFolder.setText(info.title);
        	    
        	    View view = folderWorkspace.createFolderItem(dragInfo) ;
        	    
        	    folderWorkspace.addView(view,false) ;
        	    
        	    //排序
        	    mDragController.sortView(cellLayout, mDragInfo.cellIndex, cellLayout.getChildCount() - 1);
        	    //移除
                cellLayout.removeView(mDragInfo.cell) ;
                
                if(view instanceof Mogoo_BubbleTextView){
                    ((Mogoo_BubbleTextView)view).startVibrate(mIconCache, 0);
                }
    	    }
    	    //无法接受的时候打回原处
    	    else{
    	        source.onRestoreDragIcon(dragInfo);
    	    }
//            LauncherModel.setSortIconInfo(R.id.folderWorkspace, -1);
    	}
    	else
    	{
            int index = findTargetIndex(dragView, cellLayout);
            
            if(index >= Mogoo_GlobalConfig.FOLDER_BASE_INDEX && (folderController.getLastActiveIcon() != null || (folderWorkspace.getVisibility() == View.VISIBLE && folderWorkspace.getLoadingFolder()!=null))){
                Mogoo_BubbleTextView targetView = folderController.getLastActiveIcon();
                //图标文件夹流程
                folderController.iconFolderInactive();
                
                index = index - Mogoo_GlobalConfig.FOLDER_BASE_INDEX;
                //如果目标文件夹排満了
                if(targetFolderIsFull(cellLayout,index))
                {
                    folderRestoreDragIcon(source, curScreen, cellLayout, index);
                }
                else
                {
                    boolean open = false ;
                    
                    //add by huangyue------ 当激活图标和最后放手时的图标index不一致时，走正常排序分支
                    int indexTemp = cellLayout.indexOfChild(targetView);
                    if(indexTemp != index){
                        dropNormal(source, curScreen, cellLayout, index);
                        return;
                    }
                    //end                    
                    
                    //第一次形成图标文件夹时打开
                    if(!(cellLayout.getChildAt(index) instanceof Mogoo_FolderBubbleText)){
                        open = true ;
                    }
                    
                    if(mDragInfo.screen != getCurrentScreen()){
                    	removeLastScreenView();
                    }
                    
                    View mergeFolder = folderController.replaceIcon2Folder(cellLayout, mDragInfo.cell, curScreen, index);
                    
                    if(mergeFolder == null){
                        return;
                    }
                    
                    if(Mogoo_GlobalConfig.LOG_DEBUG)
                    {
                        Log.d(TAG, "icon folder index = " + (index)); 
                    }
                    
                    mDragController.sortView(cellLayout, mDragInfo.cellIndex, cellLayout.getChildCount() - 1);
                    
                    if(Mogoo_GlobalConfig.LOG_DEBUG)
                    {
                        Log.d(TAG, "icon folder mDragInfo.cellIndex = " + mDragInfo.cellIndex); 
                    }
                    
                    cellLayout.removeView(mDragInfo.cell);
                    ShortcutInfo shortInfo = (ShortcutInfo) ((Mogoo_FolderBubbleText)mergeFolder).getTag();
                    shortInfo.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
                    if(open)
                    {
                        ((Mogoo_FolderBubbleText)mergeFolder).openFolder() ;
                        folderWorkspace.setNewFolderClosed(false) ;
                    }
                    
                    ((Mogoo_BubbleTextView)mergeFolder).startVibrate(mIconCache, 0);
                }
                
            }
            else
            {
                if(index >= Mogoo_GlobalConfig.FOLDER_BASE_INDEX){
                    index = index - Mogoo_GlobalConfig.FOLDER_BASE_INDEX;
                }
                
                folderController.iconFolderInactive();
                
                //正常拖动流程
                //add by huangyue
                	if(mDragInfo.cell == null){
                		onDragEnter(source, index, y, xOffset, yOffset, dragView, dragInfo);
                	}
                //end
                	if(mDragInfo.cell != null){
                		dropNormal(source, curScreen, cellLayout, index);
                	} else {
                		source.onRestoreDragIcon(dragInfo);
                	}
            }
                   
            resetDragInfo();
    	}
    	
    	LauncherModel.setSortIconInfo(R.id.workspace, curScreen);
    	
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "-------------------------end--------------------------------------"); 
        }
    }
    
    //回弹来自图标文件满的情况
    private void folderRestoreDragIcon(DragSource source, int curScreen, CellLayout cellLayout, int index){
        if(mDragInfo.cell == null){
            return;
        }
        //用于清除前一个target的副本 add by huangyue 
        ViewParent vp = mDragInfo.cell.getParent();
        if((!(vp instanceof CellLayout) && !source.equals(vp)) || (vp instanceof CellLayout && !(source instanceof Workspace))){
            if(vp != null && vp instanceof ViewGroup){
                ViewGroup vg = (ViewGroup) vp;
                mDragController.sortView(vg, mDragInfo.cellIndex, vg.getChildCount() - 1);
                vg.removeView(mDragInfo.cell);
            }
        }
        
        ShortcutInfo info = (ShortcutInfo) mDragInfo.cell.getTag();
        if(!(source instanceof Workspace)){
            if(mDragInfo.cellIndex != -1){
                mDragController.sortWithoutAnimation(cellLayout, mDragInfo.cellIndex, cellLayout.getChildCount() - 1);
            }
            cellLayout.removeView(mDragInfo.cell);
        }
        source.onRestoreDragIcon(info);
    }

    private void dropNormal(DragSource source, int curScreen, CellLayout cellLayout, int index) {
        //从外部区域拖入到WorkSpace
        if (source != this) {
            if (mDragInfo != null) {
                // CellLayout cellLayout = (CellLayout)
                // this.getChildAt(curScreen);
                int unfullScreen = getUnfullScreen(curScreen);

                if (mDragInfo.cellIndex == -1) {
                    // 如WorkSpace没有空闲的单元格，则拖动到图标放回到拖动源到最后的位置
                    // if(unfullScreen == -1)
                    // {
                    // //mDragController.restoreDragSourceDragIcon(dragInfo);
                    // resetDragInfo();
                    // return;
                    // }
                    if (Mogoo_GlobalConfig.LOG_DEBUG) {
                        Log.d(TAG, "mDragInfo.cellIndex == -1 source != this");
                    }

                    // int index = findTargetIndex(dragView, cellLayout);
                    View endView = null;

                    if (index == -1) {
                        Log.e(TAG, "The insert index = -1");
                        return;
                    }

                    if (index < cellLayout.getChildCount() - 1) {
                        endView = mDragController.moveView(cellLayout, index,
                                cellLayout.getChildCount() - 2, 1);
                    } else {
                        endView = cellLayout.getChildAt(cellLayout.getChildCount() - 1);
                        cellLayout.removeView(endView);
                    }

                    addToIndexCell(cellLayout, mDragInfo.cell, index);

                    resortCelllayout(endView, curScreen, unfullScreen);

                }
            } 
        } else {
            // 从该区域拖动图标到其他区域后，再拖回到该区域后释放拖动到图标
            if (mDragInfo.cellIndex == -1) {
                if (isFullOfCurrentScreen(curScreen) && !isFullFromCurrentScreen(curScreen)) {
                    if (Mogoo_GlobalConfig.LOG_DEBUG) {
                        Log.d(TAG, "mDragInfo.cellIndex == -1 source == this");
                    }

                    // CellLayout cellLayout = (CellLayout)
                    // this.getChildAt(curScreen);
                    int unfullScreen = getUnfullScreen(curScreen);

                    // int index = findTargetIndex(dragView, cellLayout);
                    View endView = null;
                    if (index < cellLayout.getChildCount() - 1) {
                        endView = mDragController.moveView(cellLayout, index,
                                cellLayout.getChildCount() - 2, 1);
                    } else {
                        endView = cellLayout.getChildAt(cellLayout.getChildCount() - 1);
                        cellLayout.removeView(endView);
                    }

                    addToIndexCell(cellLayout, mDragInfo.cell, index);
                    resortCelllayout(endView, curScreen, unfullScreen);
                }
            }
            // 从该区域拖动图标,然后释放拖动
            else {
                // 跨屏拖动释放
                if (mDragInfo.screen != curScreen) {
                    if (Mogoo_GlobalConfig.LOG_DEBUG) {
                        Log.d(TAG, "mDragInfo.cellIndex =" + mDragInfo.cellIndex
                                + " mDragInfo.screen =" + mDragInfo.screen + " currentScreen="
                                + curScreen);
                    }

                    if (isFullOfCurrentScreen(curScreen) && !isFullFromCurrentScreen(curScreen)) {
                        if (Mogoo_GlobalConfig.LOG_DEBUG) {
                            Log.d(TAG,
                                    "isFullOfCurrentScreen  = true  isFullFromCurrentScreen = false");
                        }
                        removeLastScreenView();

                        // CellLayout cellLayout = (CellLayout)
                        // this.getChildAt(curScreen);
                        int unfullScreen = getUnfullScreen(curScreen);

                        // int index = findTargetIndex(dragView, cellLayout);
                        View endView = null;
                        if (index < cellLayout.getChildCount() - 1) {
                            endView = mDragController.moveView(cellLayout, index,
                                    cellLayout.getChildCount() - 2, 1);
                        } else {
                            endView = cellLayout.getChildAt(cellLayout.getChildCount() - 1);
                            cellLayout.removeView(endView);
                        }

                        addToIndexCell(cellLayout, mDragInfo.cell, index);
                        resortCelllayout(endView, curScreen, unfullScreen);
                    } else if (!isFullOfCurrentScreen(curScreen)) {
                        // 清除当前屏之前屏的拖动图标信息
                        removeLastScreenView();

                        // CellLayout cellLayout = (CellLayout)
                        // this.getChildAt(curScreen);

                        // 为了保持与其它拖动方式一致,将拖动图标插入到该屏到最后一个位置
                        addView(cellLayout, mDragInfo.cell, false);
                        // int index = findTargetIndex(dragView, cellLayout);
                        mDragController.sortView(cellLayout, cellLayout.getChildCount() - 1, index);
                        // //找完位置后，再移除
                        // cellLayout.removeView(mDragInfo.cell) ;

                        // addToIndexCell(cellLayout, mDragInfo.cell, index);

                    }
                }
            }
        }

        if (mDragInfo.cell != null) {
            mDragInfo.cell.setVisibility(View.VISIBLE);
            if (mDragInfo.cell instanceof Mogoo_BubbleTextView) {
                if (source instanceof Mogoo_DockWorkSpace
                        && mDragInfo.cell instanceof Mogoo_FolderBubbleText) {
                    mIconCache.recycle(
                            ((ShortcutInfo) mDragInfo.cell.getTag()).intent.getComponent(),
                            Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_VIBRATION);
                }

                ((Mogoo_BubbleTextView) mDragInfo.cell).startVibrate(mIconCache, 0);
            }
        }

        resetDragInfo();

        if (Mogoo_GlobalConfig.LOG_DEBUG) {
            Log.d(TAG, "-------------------------end--------------------------------------");
        }
    }

	private void removeLastScreenView() {
		CellLayout lastCellLayout = (CellLayout) getChildAt(mDragInfo.screen);
		mDragController.sortWithoutAnimation(lastCellLayout, mDragInfo.cellIndex,
		        lastCellLayout.getChildCount() - 1);
		lastCellLayout.removeView(mDragInfo.cell);
	}

    private void addToIndexCell(CellLayout cellLayout, View child, int index) {
//        if(mDragInfo.cell.getParent() != null){
//            ((ViewGroup)mDragInfo.cell.getParent()).removeView(mDragInfo.cell);
//        }
//        
    	if (child ==null)
    		return;
    		
        cellLayout.addView(child, index);                        
//        int[] cellXY = cellLayout.convertToCell(index);
//        CellLayout.LayoutParams lp = (CellLayout.LayoutParams)child.getLayoutParams();
//        lp.cellX = cellXY[0];
//        lp.cellY = cellXY[1];        
//        child.requestLayout();
//        invalidate();
    	
    	reLayoutCellLayout(cellLayout);
        
        setDragInfo(cellLayout, index);
        child.setOnLongClickListener(mLongClickListener);
        LauncherModel.setSortIconInfo(R.id.workspace, getCurrentScreen());
    }

    //重排不可见celllayout布局
    private void resortCelllayout(View view, int startScreen,int unfullScreen) {
        if(view == null){
            return;
        }
        
        //int screen = mCurrentScreen + 1;
        int screen = startScreen + 1;
        CellLayout layout = null;
        CellLayout.LayoutParams lp = null;
        CellLayout.LayoutParams lp1 = null;
        View lastView = null;
        
        while(screen != unfullScreen)
        {
            layout = (CellLayout) getChildAt(screen);
            lp = null;
            lp1 = null;
            lastView = layout.getChildAt(layout.getChildCount() - 1);
                
            for(int i = 0; i < layout.getChildCount() - 1; i++){
                lp = (CellLayout.LayoutParams)layout.getChildAt(i).getLayoutParams();
                lp1 = (CellLayout.LayoutParams)layout.getChildAt(i + 1).getLayoutParams();
                lp.cellX = lp1.cellX;
                lp.cellY = lp1.cellY;
            }
            
            layout.removeView(lastView);
            layout.addView(view, 0);
            lp = (CellLayout.LayoutParams) view.getLayoutParams();
            lp.cellX = 0;
            lp.cellY = 0;
            
            layout.requestLayout();
            invalidate();
            
            view = lastView;
            LauncherModel.setSortIconInfo(R.id.workspace, screen);
            screen++;
        }
        
        layout = (CellLayout) getChildAt(unfullScreen);
        
        if(layout.getChildCount() != 0){
            lastView = layout.getChildAt(layout.getChildCount() - 1);
            for(int i = 0; i < layout.getChildCount() - 1; i++){
                lp = (CellLayout.LayoutParams)layout.getChildAt(i).getLayoutParams();
                lp1 = (CellLayout.LayoutParams)layout.getChildAt(i + 1).getLayoutParams();
                lp.cellX = lp1.cellX;
                lp.cellY = lp1.cellY;
            }
            
            lp = (CellLayout.LayoutParams) lastView.getLayoutParams();
            int cellXY[] = layout.convertToCell(layout.getChildCount());
            lp.cellX = cellXY[0];
            lp.cellY = cellXY[1];
        }
        
        layout.addView(view, 0);
        lp = (CellLayout.LayoutParams) view.getLayoutParams();
        lp.cellX = 0;
        lp.cellY = 0;
        
        layout.requestLayout();
        invalidate();
        
        LauncherModel.setSortIconInfo(R.id.workspace, unfullScreen);
    }

    private int getUnfullScreen(int currentScreen) {
        boolean isLandscape = Mogoo_GlobalConfig.isLandscape();
        int cellSize = Mogoo_GlobalConfig.getWorkspaceLongAxisCells(isLandscape)
                * Mogoo_GlobalConfig.getWorkspaceShortAxisCells(isLandscape);
        int childSize = getChildCount();
        //ipdate by yeben 2011-9-28
//      for(int i = currentScreen + 1; i < childSize - 1; i++){
//          if(((ViewGroup)getChildAt(i)).getChildCount() < cellSize){
//              return i;
//          }
//      }
      
      for(int i = currentScreen; i < childSize ; i++){
          if(((CellLayout)getChildAt(i)).getChildCount() < cellSize){
              return i;
          }
      }
      //end
        return -1;
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) 
    {
    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------start onDragEnter()--------------------------------------"); 
        } 
    	
        clearVacantCache();
        mLauncher.getFolderController().setCanActive(true);
        
        //add by 张永辉 2011-34 不接收widget
        if(dragInfo instanceof LauncherAppWidgetInfo){
            return ;
        }
        //end
        
        CellLayout currentCellLayout = getCurrentDropLayout();
        boolean isLandscape = Mogoo_GlobalConfig.isLandscape();
        //int cellSize = MT_GlobalConfig.getWorkspaceLongAxisCells(isLandscape) * MT_GlobalConfig.getWorkspaceShortAxisCells(isLandscape);
          
        int workspaceCellCounts = Mogoo_GlobalConfig.getWorkspaceCellCounts();
        
        if (Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "isLocaleDrag = " + isLocaleDrag + " WorkspaceCellCounts =" + workspaceCellCounts +" source = " + source.toString());
        }
        
        //从外部区域拖入
        if (source != this)
        {
        	if (Mogoo_GlobalConfig.LOG_DEBUG)
            {
                Log.d(TAG, "Outside area  dragEnter...... ");
            }
        	
            //add by 张永辉 2011-3-2 设置开始序号
            dragView.setStartIndex(100) ;
            //end 
            
        	// 判断该屏不接受拖动图标
            if (!acceptDrop(source, x, y, xOffset, yOffset, dragView, dragInfo)) {
                return;
            }            
            
            if(source instanceof Mogoo_DockWorkSpace && dragInfo instanceof ShortcutInfo){
                mIconCache.recycle(((ShortcutInfo)dragInfo).intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
//                mIconCache.remove(((ShortcutInfo)dragInfo).intent.getComponent());
            }
            
            View view = createIcon(dragInfo);
            mDragInfo.cell = view;
            mDragInfo.cellIndex = -1;
            mDragInfo.cell.setVisibility(View.INVISIBLE);
            mDragInfo.screen = getCurrentScreen();
           
            // 当前屏已满            
            if (Mogoo_GlobalConfig.LOG_DEBUG)
            {
                Log.d(TAG, " WorkspaceCellCounts =" + workspaceCellCounts +" currentCellLayout.getChildCount() = " + currentCellLayout.getChildCount());
            }
            
            if (currentCellLayout.getChildCount() == workspaceCellCounts) 
            {        
                isLocaleDrag = true;
                return;
            }
            int index = currentCellLayout.getChildCount();
            addView(currentCellLayout, view, false);
            
            if (Mogoo_GlobalConfig.LOG_DEBUG)
            {
                Log.d(TAG, "(outside) start index = " + index);
            }
            
            setDragInfo(currentCellLayout, index);

            isLocaleDrag = true;
            onDragOver(source, index, y, xOffset, yOffset, dragView, dragInfo);
        }else
        {
            if (isLocaleDrag)
            {   
            	if (Mogoo_GlobalConfig.LOG_DEBUG)
                {
                    Log.d(TAG, "Inside area  dragEnter...... ");
                }
            	
                isLocaleDrag = false;
                
//                if(dragInfo instanceof ShortcutInfo){
//                    mIconCache.recycle(((ShortcutInfo)dragInfo).intent.getComponent(), MT_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
//                }
                
                View view = createIcon(dragInfo);
                mDragInfo.cell = view;
                mDragInfo.cellIndex = -1;
                mDragInfo.cell.setVisibility(View.INVISIBLE);
                mDragInfo.screen = getCurrentScreen();
                // 当前屏已满
                if (currentCellLayout.getChildCount() == workspaceCellCounts) 
                {
                	isLocaleDrag = true;
                    return;
                }
                
                addView(currentCellLayout, view, false);
                setDragInfo(currentCellLayout, currentCellLayout.getChildCount() - 1);
                
                if (Mogoo_GlobalConfig.LOG_DEBUG)
                {
                    Log.d(TAG, "(inside) start index = " + (currentCellLayout.getChildCount() - 1));
                }
                
                LauncherModel.setSortIconInfo(R.id.workspace, getCurrentScreen());
                isLocaleDrag = true;
                onDragOver(source, x, y, xOffset, yOffset, dragView, dragInfo);

            }
            isLocaleDrag = true;
        }
        
        LauncherModel.setSortIconInfo(R.id.workspace, getCurrentScreen());
        currentCellLayout = null;
//        setLocaleDrag(false);
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "-------------------------end--------------------------------------"); 
        }
    }

    View createIcon(Object dragInfo) {
        ItemInfo info = (ItemInfo) dragInfo;

        CellLayout currCellLayout = this.getCurrentDropLayout();

        View view = null;

        switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                if (info.container == NO_ID && info instanceof ApplicationInfo) { // 从抽屉过来的
                    // Came from all apps -- make a copy
                    info = new ShortcutInfo((ApplicationInfo) info);
                }
                view = mLauncher.createShortcut(R.layout.application, currCellLayout,(ShortcutInfo) info, false);
                
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_MOGOO_FOLDER:
                view = mLauncher.createShortcut((ShortcutInfo)info) ;
                break ;
            case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
            case LauncherSettings.Favorites.ITEM_TYPE_WIDGET_PHOTO_FRAME:
            case LauncherSettings.Favorites.ITEM_TYPE_WIDGET_SEARCH:
            case LauncherSettings.Favorites.ITEM_TYPE_WIDGET_CLOCK:                 
            	break;
            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
        }
        
        if (view == null)
            return null;
        
        view.setHapticFeedbackEnabled(false);
        view.setOnLongClickListener(mLongClickListener);
        if (view instanceof DropTarget) {
            mDragController.addDropTarget((DropTarget) view);
        }
        
        if(view instanceof Mogoo_BubbleTextView){
            ((ShortcutInfo)(view.getTag())).container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
        }
        
        return view;
    }

    void addView(CellLayout parent, View view,boolean insertAtFirst)
    {
        if (parent != null) 
        {
            int endindex = parent.getChildCount() ;
            parent.addView(view, insertAtFirst ? 0 : -1);
            
            //mTargetCell = estimateDropCell(x, y, 1, 1, view, parent, mTargetCell);
            int[] endCell =  parent.convertToCell(endindex);
            parent.onDropChild(view, endCell);
            //CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();
        }

    }

    private void setDragInfo(CellLayout currentCellLayout, int index) {
        mDragInfo.cell = currentCellLayout.getChildAt(index);
        mDragInfo.cellIndex = index;
        int[] cellXY = currentCellLayout.convertToCell(index);
        mDragInfo.cellX = cellXY[0];
        mDragInfo.cellY = cellXY[1];
        mDragInfo.screen = getCurrentScreen();
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) {
        
    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------start onDragOver()--------------------------------------"); 
        } 
    	
    	//如果文件夹打开时
    	Mogoo_FolderWorkspace folderWorkspace = (Mogoo_FolderWorkspace) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.folderWorkspace, mLauncher) ;
    	if(folderWorkspace.getLoadingFolder()!=null){
    	    return ;
    	}
        
        //add by 张永辉
        if(dragInfo instanceof LauncherAppWidgetInfo || !isLocaleDrag){
        	moveWidget = true ;
            return ;
        }
        //end 
        
        // add by huangyue
        Mogoo_FolderController folderController = mLauncher.getFolderController();
        // end 
        
        int screen = getCurrentScreen();
        CellLayout cellLayout = (CellLayout) getChildAt(screen); 
        
        if (Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "isLocaleDrag = " + isLocaleDrag + " mCurrentScreen =" + screen + " mDragInfo.screen = " + mDragInfo.screen + " mDragInfo.cellIndex = " + mDragInfo.cellIndex);
        }
        
        //---------------------------add by 魏景春-------------------------------------
        
        boolean isFull = isFullOfCurrentScreen(screen) ;
        
      	// 1. 如果当前屏满并且具有文件夹功能则不进行排序
        if (isFull && Mogoo_GlobalConfig.ICON_FOLDER) 
        {
            int endIndex = findTargetIndex(dragView, cellLayout);
            
            //add by 张永辉 2011-7-23
            if(!(dragInfo instanceof Mogoo_FolderInfo) && (endIndex==lastFindIndex-Mogoo_GlobalConfig.FOLDER_BASE_INDEX && (System.currentTimeMillis() - dragOverTime) < DRAG_GEP )){
            	  folderController.setTempActiveIcon(null);
                folderController.iconFolderInactive();
                return ;
              }
            lastFindIndex = endIndex ;
            dragOverTime = System.currentTimeMillis();
            //end add 
            
            if(endIndex != mDragInfo.cellIndex && !(dragInfo instanceof Mogoo_FolderInfo) && endIndex >= Mogoo_GlobalConfig.FOLDER_BASE_INDEX && endIndex != (mDragInfo.cellIndex+Mogoo_GlobalConfig.FOLDER_BASE_INDEX)){
                folderController.setTempActiveIcon((Mogoo_BubbleTextView) cellLayout.getChildAt(endIndex - Mogoo_GlobalConfig.FOLDER_BASE_INDEX));
            }else{
                folderController.setTempActiveIcon(null);
                folderController.iconFolderInactive();
            }
        }
        else
        {
            //1. 如果当前屏满则不进行排序
            if(isFull)
            {
                return ;
            }
            
            //2. 在当前屏拖动
            if (mDragInfo.screen == screen)
            {
                if (Mogoo_GlobalConfig.LOG_DEBUG)
                {
                    Log.d(TAG, " CurrentScreen dragOver......");
                }
                
                if(Mogoo_VibrationController.isLoading){
                    return;
                }
                
                int endIndex = findTargetIndex(dragView, cellLayout);
                
                //add by 张永辉 2011-7-23
                if(!(dragInfo instanceof Mogoo_FolderInfo) && (endIndex==lastFindIndex-Mogoo_GlobalConfig.FOLDER_BASE_INDEX && (System.currentTimeMillis() - dragOverTime) < DRAG_GEP)){
                	   folderController.setTempActiveIcon(null);
                    folderController.iconFolderInactive();
                    return ;
                  }
                lastFindIndex = endIndex ;
                dragOverTime = System.currentTimeMillis();
                //end
                
//                if(endIndex == mDragInfo.cellIndex){
//                	return;
//                }
                
                if(endIndex != mDragInfo.cellIndex && !(dragInfo instanceof Mogoo_FolderInfo) && endIndex >= Mogoo_GlobalConfig.FOLDER_BASE_INDEX && endIndex != (mDragInfo.cellIndex+Mogoo_GlobalConfig.FOLDER_BASE_INDEX)){
                    folderController.setTempActiveIcon((Mogoo_BubbleTextView) cellLayout.getChildAt(endIndex - Mogoo_GlobalConfig.FOLDER_BASE_INDEX));
                }else{
                    folderController.iconFolderInactive();
                    folderController.setTempActiveIcon(null);
                    if (mDragInfo != null  && mDragController.sortView(cellLayout, mDragInfo.cellIndex, endIndex)) {
                        setDragInfo(cellLayout, endIndex);
                    }
                }
                
                LauncherModel.setSortIconInfo(R.id.workspace, screen);
            }
            //3.跨屏拖动    
            else
            {            
                //3.1 如果拖动图标的“索引号== -1”则将该图标插入到该屏到最后一个位置,并更新拖动图标信息
                // mDragInfo.cellIndex = -1 表示从该区域拖动图标到其他区域后，再拖回到该worksapce区域后且当前屏图标已满)
                if ( mDragInfo != null && mDragInfo.cell !=null && mDragInfo.cellIndex ==-1)
                {
                    if (Mogoo_GlobalConfig.LOG_DEBUG)
                    {
                        Log.d(TAG, " Switch Screen dragOver: mDragInfo.cellIndex == -1");
                    }
                    
                     addView(cellLayout, mDragInfo.cell, false);
                     int index =  cellLayout.getChildCount() - 1;
                     setDragInfo(cellLayout, index);
                     
                     int endIndex = findTargetIndex(dragView, cellLayout);
                     
                     if(!(dragInfo instanceof Mogoo_FolderInfo) && endIndex >= Mogoo_GlobalConfig.FOLDER_BASE_INDEX && endIndex != (mDragInfo.cellIndex+Mogoo_GlobalConfig.FOLDER_BASE_INDEX)){
                         folderController.setTempActiveIcon((Mogoo_BubbleTextView) cellLayout.getChildAt(endIndex - Mogoo_GlobalConfig.FOLDER_BASE_INDEX));
                     }else{
                         folderController.iconFolderInactive();
                         folderController.setTempActiveIcon(null);
                     
                         if (mDragInfo != null  && mDragController.sortView(cellLayout, mDragInfo.cellIndex, endIndex)) 
                         {
                             setDragInfo(cellLayout, endIndex);
                         }
                     }


                }else
                {
                    if (Mogoo_GlobalConfig.LOG_DEBUG)
                    {
                        Log.d(TAG, " Switch Screen dragOver: mDragInfo.cellIndex != -1");
                    }
                    
                    removeLastScreenView();
                    
                    //3.3 将拖动图标插入到该屏到最后一个位置,并更新拖动图标信息
                    addView(cellLayout, mDragInfo.cell, false);
                    int index = cellLayout.getChildCount() - 1;
                    setDragInfo(cellLayout, index);

                    int endIndex = findTargetIndex(dragView, cellLayout);
                    
                    if(!(dragInfo instanceof Mogoo_FolderInfo) && endIndex >= Mogoo_GlobalConfig.FOLDER_BASE_INDEX && endIndex != (mDragInfo.cellIndex+Mogoo_GlobalConfig.FOLDER_BASE_INDEX)){
                        folderController.setTempActiveIcon((Mogoo_BubbleTextView) cellLayout.getChildAt(endIndex - Mogoo_GlobalConfig.FOLDER_BASE_INDEX));
                    }else{
                        folderController.iconFolderInactive();
                        folderController.setTempActiveIcon(null);
                        
                        if (mDragInfo != null  && mDragController.sortView(cellLayout, mDragInfo.cellIndex, endIndex)) 
                        {
                            setDragInfo(cellLayout, endIndex);
                        }
                    }

                }
                
                LauncherModel.setSortIconInfo(R.id.workspace, screen);
//                lastScreen  = screen;
                
                if(Mogoo_GlobalConfig.LOG_DEBUG)
                {
                    Log.d(TAG, "-------------------------end--------------------------------------"); 
                }
            }
        }
        //---------------------------------------end--------------------------------------------
    
        
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) {
        clearVacantCache();
    }

    /**
     * Return the current {@link CellLayout}, correctly picking the destination
     * screen while a scroll is in progress.
     */
    private CellLayout getCurrentDropLayout() {
        int index = mScroller.isFinished() ? mCurrentScreen : mNextScreen;
        return (CellLayout) getChildAt(index);
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) {
        // add by 张永辉 2011-01-25 改写acceptDrop()方法，用于适应现有需求
        /**
         * 搜索屏中不能拖入任何东西 快捷方式屏中只能放入文件夹和应用程序快捷方式 widget屏中只能放入widget
         */
        final CellLayout layout = getCurrentDropLayout();

        // 当前要拖放的屏的序号
        int currentDropScreenIndex = this.indexOfChild(layout);

        // 搜索屏
        int searchScreen = Mogoo_GlobalConfig.getSearchScreen();

        // 如果当前要拖放的屏为搜索屏，则不接收
        if (currentDropScreenIndex == searchScreen) {
            return false;
            // 如果当前要拖放的屏为快捷方式屏，则widget不能放入,其它均可，如果拖入的快捷方式屏及其之后的所有快捷方式屏全满时
        } else if (Mogoo_GlobalConfig.isShortcutScreen(currentDropScreenIndex)) 
        {
            if(dragInfo instanceof LauncherAppWidgetInfo)
            {
                 return false;
            }else
            {
            	if (isFullFromCurrentScreen(currentDropScreenIndex))
            	{
            		if (isLocaleDrag && mDragInfo.cellIndex !=-1 && mDragInfo.screen >= getCurrentScreen())
            		{
            			return true;
            		}else
            		{
            			return false;
            		}
            	}
            }
            // 如果当前要拖放的屏为widget屏，则widget能放入,其它均不可
        } else if (Mogoo_GlobalConfig.isWidgetScreen(currentDropScreenIndex)
                && !(dragInfo instanceof LauncherAppWidgetInfo)) {
            return false;
        }
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "call acceptDrop");
        }

        return true;
        // end

        // delete by 张永辉 2011-1-25
        // final CellLayout layout = getCurrentDropLayout();
        // final CellLayout.CellInfo cellInfo = mDragInfo;
        // final int spanX = cellInfo == null ? 1 : cellInfo.spanX;
        // final int spanY = cellInfo == null ? 1 : cellInfo.spanY;
        //
        // if (mVacantCache == null) {
        // final View ignoreView = cellInfo == null ? null : cellInfo.cell;
        // mVacantCache = layout.findAllVacantCells(null, ignoreView);
        // }
        //
        // return mVacantCache.findCellForSpan(mTempEstimate, spanX, spanY,
        // false);
        // end
    }

    /**
     * {@inheritDoc}
     */
    public Rect estimateDropLocation(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo, Rect recycle) {
        final CellLayout layout = getCurrentDropLayout();

        final CellLayout.CellInfo cellInfo = mDragInfo;
        final int spanX = cellInfo == null ? 1 : cellInfo.spanX;
        final int spanY = cellInfo == null ? 1 : cellInfo.spanY;
        final View ignoreView = cellInfo == null ? null : cellInfo.cell;

        final Rect location = recycle != null ? recycle : new Rect();

        // Find drop cell and convert into rectangle
        int[] dropCell = estimateDropCell(x - xOffset, y - yOffset, spanX, spanY, ignoreView,
                layout, mTempCell);

        if (dropCell == null) {
            return null;
        }

        layout.cellToPoint(dropCell[0], dropCell[1], mTempEstimate);
        location.left = mTempEstimate[0];
        location.top = mTempEstimate[1];

        layout.cellToPoint(dropCell[0] + spanX, dropCell[1] + spanY, mTempEstimate);
        location.right = mTempEstimate[0];
        location.bottom = mTempEstimate[1];

        return location;
    }

    /**
     * Calculate the nearest cell where the given object would be dropped.
     */
    private int[] estimateDropCell(int pixelX, int pixelY, int spanX, int spanY, View ignoreView,
            CellLayout layout, int[] recycle) {
        // Create vacant cell cache if none exists
        if (mVacantCache == null) {
            mVacantCache = layout.findAllVacantCells(null, ignoreView);
        }

        // Find the best target drop location
        return layout.findNearestVacantArea(pixelX, pixelY, spanX, spanY, mVacantCache, recycle);
    }

    void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }

    public void setDragController(DragController dragController) {
        mDragController = dragController;
    }

    public void onDropCompleted(View target, boolean success) 
    {
    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------start onDropCompleted()--------------------------------------"); 
        } 
    	
        clearVacantCache();
        Mogoo_FolderController folderController = mLauncher.getFolderController();
        
        if(folderController.getLastActiveIcon() != null){
            folderController.getLastActiveIcon();
            folderController.setTempActiveIcon(null);
        }else{
            dropNormal(target, success);
        }
        
        if (Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "call onDropCompleted: success =" + success);
        }
        
    	isLocaleDrag = false; 
        resetDragInfo();
        LauncherModel.saveAllIconInfo(getContext());
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "-------------------------end--------------------------------------"); 
        }
    }

    private void dropNormal(View target, boolean success) {
        if (success)
        {
        	//跨区域拖动结束操作            
            if (target != this && mDragInfo != null && mDragInfo.cell != null) {
                final CellLayout cellLayout = (CellLayout) getChildAt(mDragInfo.screen);
                cellLayout.removeView(mDragInfo.cell);
                
                //重整变化屏的单元格布局，然后保存到数据库
                
                if (mDragInfo.cell instanceof DropTarget) {
                    mDragController.removeDropTarget((DropTarget)mDragInfo.cell);
                }
                LauncherModel.setSortIconInfo(R.id.workspace, getCurrentScreen());
            }
        } else {
        	//本区域拖动结束操作
            if (mDragInfo != null && mDragInfo.cell != null) {
                final CellLayout cellLayout = (CellLayout) getChildAt(mDragInfo.screen);
//                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) mDragInfo.cell.getLayoutParams();
//                lp.cellX = mDragInfo.cellX;
//                lp.cellX = mDragInfo.cellY;
                cellLayout.onDropAborted(mDragInfo.cell);
                if(mDragInfo.cell instanceof Mogoo_BubbleTextView)
                {
                	System.out.println("===================================3");
                    ((Mogoo_BubbleTextView)mDragInfo.cell).startVibrate(mIconCache, 0);
                }
            }
        }
    }

    public void scrollLeft() {
        clearVacantCache();
        if (mScroller.isFinished()) {
            if (mCurrentScreen > 0)
                snapToScreen(mCurrentScreen - 1);
        } else {
            if (mNextScreen > 0)
                snapToScreen(mNextScreen - 1);
        }
    }

    public void scrollRight() {
        clearVacantCache();
        if (mScroller.isFinished()) {
        	//update by 袁业奔 2011-9-14 拖动加屏
//            if (mCurrentScreen < getChildCount() - 1)
//                snapToScreen(mCurrentScreen + 1);
        	//不是最后一屏
        	if(mCurrentScreen < getChildCount() - 1){
        		snapToScreen(mCurrentScreen + 1);
        	}else{//最后一屏,且总屏数小于规定屏数，则需加一屏
        		if(getChildCount() < Mogoo_GlobalConfig.getWorkspaceScreenMaxCount()){
//            		Mogoo_BitmapUtils.clearIndicatorImages();
            		addCellLayout();       		
            		//加屏后需更新配置文件中的屏幕总数        		
            		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SCREEN_COUNT, getChildCount());
            		int[] screenType = Mogoo_GlobalConfig.createWorkspaceScreenType(getChildCount());
            		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SCREEN_TYPE,screenType);
        		}

        	}
        	//-------------end---
        } else {
        	//update by 袁业奔 2011-9-14 拖动加屏
//            if (mNextScreen < getChildCount() - 1)
//                snapToScreen(mNextScreen + 1);
            if (mNextScreen < getChildCount() - 1){
                snapToScreen(mNextScreen + 1);
            }else{
            	if(getChildCount() < Mogoo_GlobalConfig.getWorkspaceScreenMaxCount()){
            		addCellLayout();       		
            		//加屏后需更新配置文件中的屏幕总数        		
            		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SCREEN_COUNT, getChildCount());
            		int[] screenType = Mogoo_GlobalConfig.createWorkspaceScreenType(getChildCount());
    				Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SCREEN_TYPE,screenType);
            	}       	
            }
            //------------end------------
        }
    }

    public int getScreenForView(View v) {
        int result = -1;
        if (v != null) {
            ViewParent vp = v.getParent();
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                if (vp == getChildAt(i)) {
                    return i;
                }
            }
        }
        return result;
    }
    
    public int getScreenIndex(View v){
		int result = -1;
		if (v instanceof CellLayout) {
			int count = getChildCount();
			for (int i = 0; i < count; i++) {
				if (v.equals(getChildAt(i))) {
					return i;
				}
			}
		}
		return result;
    }

    public Folder getFolderForTag(Object tag) {
        int screenCount = getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            CellLayout currentScreen = ((CellLayout) getChildAt(screen));
            int count = currentScreen.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = currentScreen.getChildAt(i);
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
                if (lp.cellHSpan == 4 && lp.cellVSpan == 4 && child instanceof Folder) {
                    Folder f = (Folder) child;
                    if (f.getInfo() == tag) {
                        return f;
                    }
                }
            }
        }
        return null;
    }

    public View getViewForTag(Object tag) {
        int screenCount = getChildCount();
        for (int screen = 0; screen < screenCount; screen++) {
            CellLayout currentScreen = ((CellLayout) getChildAt(screen));
            int count = currentScreen.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = currentScreen.getChildAt(i);
                if (child.getTag() == tag) {
                    return child;
                }
            }
        }
        return null;
    }

    /**
     * @return True is long presses are still allowed for the current touch
     */
    public boolean allowLongPress() {
        return mAllowLongPress;
    }

    /**
     * Set true to allow long-press events to be triggered, usually checked by
     * {@link Launcher} to accept or block dpad-initiated long-presses.
     */
    public void setAllowLongPress(boolean allowLongPress) {
        mAllowLongPress = allowLongPress;
    }

    void removeItems(final ArrayList<ApplicationInfo> apps) {
        final int count = getChildCount();
        final PackageManager manager = getContext().getPackageManager();
        final AppWidgetManager widgets = AppWidgetManager.getInstance(getContext());

        final HashSet<String> packageNames = new HashSet<String>();
        final int appCount = apps.size();
        for (int i = 0; i < appCount; i++) {
            packageNames.add(apps.get(i).componentName.getPackageName());
        }

        for (int i = 0; i < count; i++) {
            final CellLayout layout = (CellLayout) getChildAt(i);

            // Avoid ANRs by treating each screen separately
            post(new Runnable() {
                public void run() {
                    final ArrayList<View> childrenToRemove = new ArrayList<View>();
                    childrenToRemove.clear();

                    int childCount = layout.getChildCount();
                    for (int j = 0; j < childCount; j++) {
                        final View view = layout.getChildAt(j);
                        Object tag = view.getTag();

                        if (tag instanceof ShortcutInfo) {
                            final ShortcutInfo info = (ShortcutInfo) tag;
                            final Intent intent = info.intent;
                            final ComponentName name = intent.getComponent();

                            if (Intent.ACTION_MAIN.equals(intent.getAction()) && name != null) {
                                for (String packageName : packageNames) {
                                    if (packageName.equals(name.getPackageName())) {
                                        // TODO: This should probably be done on
                                        // a worker thread
                                        LauncherModel.deleteItemFromDatabase(mLauncher, info);
                                        childrenToRemove.add(view);
                                    }
                                }
                            }
                        } else if (tag instanceof UserFolderInfo) {
                            final UserFolderInfo info = (UserFolderInfo) tag;
                            final ArrayList<ShortcutInfo> contents = info.contents;
                            final ArrayList<ShortcutInfo> toRemove = new ArrayList<ShortcutInfo>(1);
                            final int contentsCount = contents.size();
                            boolean removedFromFolder = false;

                            for (int k = 0; k < contentsCount; k++) {
                                final ShortcutInfo appInfo = contents.get(k);
                                final Intent intent = appInfo.intent;
                                final ComponentName name = intent.getComponent();

                                if (Intent.ACTION_MAIN.equals(intent.getAction()) && name != null) {
                                    for (String packageName : packageNames) {
                                        if (packageName.equals(name.getPackageName())) {
                                            toRemove.add(appInfo);
                                            // TODO: This should probably be
                                            // done on a worker thread
                                            LauncherModel
                                                    .deleteItemFromDatabase(mLauncher, appInfo);
                                            removedFromFolder = true;
                                        }
                                    }
                                }
                            }

                            contents.removeAll(toRemove);
                            if (removedFromFolder) {
                                final Folder folder = getOpenFolder();
                                if (folder != null)
                                    folder.notifyDataSetChanged();
                            }
                        } else if (tag instanceof LiveFolderInfo) {
                            final LiveFolderInfo info = (LiveFolderInfo) tag;
                            final Uri uri = info.uri;
                            final ProviderInfo providerInfo = manager.resolveContentProvider(
                                    uri.getAuthority(), 0);

                            if (providerInfo != null) {
                                for (String packageName : packageNames) {
                                    if (packageName.equals(providerInfo.packageName)) {
                                        // TODO: This should probably be done on
                                        // a worker thread
                                        LauncherModel.deleteItemFromDatabase(mLauncher, info);
                                        childrenToRemove.add(view);
                                    }
                                }
                            }
                        } else if (tag instanceof LauncherAppWidgetInfo) {
                            final LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) tag;
                            final AppWidgetProviderInfo provider = widgets
                                    .getAppWidgetInfo(info.appWidgetId);
                            if (provider != null) {
                                for (String packageName : packageNames) {
                                    if (packageName.equals(provider.provider.getPackageName())) {
                                        // TODO: This should probably be done on
                                        // a worker thread
                                        LauncherModel.deleteItemFromDatabase(mLauncher, info);
                                        childrenToRemove.add(view);
                                    }
                                }
                            }
                        }
                    }

                    childCount = childrenToRemove.size();
                    for (int j = 0; j < childCount; j++) {
                        View child = childrenToRemove.get(j);
                        layout.removeViewInLayout(child);
                        if (child instanceof DropTarget) {
                            mDragController.removeDropTarget((DropTarget) child);
                        }
                    }

                    if (childCount > 0) {
                        layout.requestLayout();
                        layout.invalidate();
                    }
                }
            });
        }
    }

    void updateShortcuts(ArrayList<ApplicationInfo> apps) {
        final PackageManager pm = mLauncher.getPackageManager();

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final CellLayout layout = (CellLayout) getChildAt(i);
            int childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                final View view = layout.getChildAt(j);
                Object tag = view.getTag();
                if (tag instanceof ShortcutInfo) {
                    ShortcutInfo info = (ShortcutInfo) tag;
                    // We need to check for ACTION_MAIN otherwise getComponent()
                    // might
                    // return null for some shortcuts (for instance, for
                    // shortcuts to
                    // web pages.)
                    final Intent intent = info.intent;
                    final ComponentName name = intent.getComponent();
                    if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                            && Intent.ACTION_MAIN.equals(intent.getAction()) && name != null) {
                        final int appCount = apps.size();
                        for (int k = 0; k < appCount; k++) {
                            ApplicationInfo app = apps.get(k);
                            if (app.componentName.equals(name)) {
                                info.setIcon(mIconCache.getIcon(info.intent));
                                ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null,
                                        new FastBitmapDrawable(info.getIcon(mIconCache)), null,
                                        null);
                            }
                        }
                    }
                }
            }
        }
    }

    void moveToDefaultScreen(boolean animate) {
    	 //modify by liangss 2012-2-15
    	int screen = 0 ;
    	if(mCurrentScreen != 1) {
    		screen = mDefaultScreen;
    	} else {
    		screen = 0;
    	}
    	//end
    	if (animate) {
            snapToScreen(screen); 
           // getChildAt(screen).requestFocus();
        } else {
        	setCurrentScreen(mCurrentScreen);
           // getChildAt(mCurrentScreen).requestFocus();
        }
        //add by huangyue
       // switchAboutSearch(screen);
        //end
     //   getChildAt(screen).requestFocus();
    }

    // delete by 张永辉 2011-1-20 改变原有屏幕指示器实现方式
    // void setIndicators(Drawable previous, Drawable next) {
    // mPreviousIndicator = previous;
    // mNextIndicator = next;
    // previous.setLevel(mCurrentScreen);
    // next.setLevel(mCurrentScreen);
    // }

    public static class SavedState extends BaseSavedState {
        int currentScreen = -1;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            currentScreen = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(currentScreen);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    // --------------------motone method statement---------

    /**
     * 刷新界面－－－－－从空白位置开始后面所有的图标都往前移动一个单元格 
     * @author: 邓炜
     * @param screen:第几屏
     * @param point：空白位置的单元格
     */
    public void reload(int screen, int[] point) {
        CellLayout cellLayout = (CellLayout) getChildAt(screen);
        int startIndex = cellLayout.getIndexByCellXY(point);
        int end = cellLayout.getChildCount();
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "uninstall reload start index ="+startIndex+"----- end index="+startIndex) ;
        }
        
        for (int i = startIndex; i < end; i++) 
        {
            
            if(Mogoo_GlobalConfig.LOG_DEBUG){
                Log.d(TAG, "-----------uninstall reload middle----------------"+i) ;
            }
            
            View currView = cellLayout.getChildAt(i);
            //cellLayout.onDropChild(currView, point);

            // update by 张永辉 2011-1-22
            // int mPoint[] = switchPoint(point, cellLayout);
            point = cellLayout.convertToCell(i);
            int mPoint[] = Mogoo_Utilities.switchPoint(point);
            // end

            ItemInfo info = (ItemInfo) currView.getTag();
            LauncherModel.moveItemInDatabase(mLauncher, info,
                    LauncherSettings.Favorites.CONTAINER_DESKTOP, screen, mPoint[0], mPoint[1]);

//            point[0]++;
//            if (point[0] >= cellLayout.getCountX()) {
//                point[1]++;
//                point[0] = 0;
//            }

        }
        
        this.reLayoutCellLayout(cellLayout);
    }

    public void onClear() 
    {
        mLauncher = null;
        mIconCache = null;
        mDragController = null;
        mLongClickListener = null;
    }

    public void onDropTargetChange(DragSource source, DropTarget dropTarget, DragView dragView,Object dragInfo) 
    {
    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------start onDropTargetChange()--------------------------------------"); 
        	Log.d(TAG, "mDragInfo.cellIndex =" + mDragInfo.cellIndex);
        }
        
        //add by 张永辉 2011-34 不接收widget
        if(dragInfo instanceof LauncherAppWidgetInfo){
        	return ;
        }
        //end
        
        Mogoo_FolderController folderController = mLauncher.getFolderController();
        folderController.iconFolderInactive();
        folderController.setTempActiveIcon(null);
        
//        MT_FolderController folderController = mLauncher.getFolderController();
//        folderController.setCanActive(false);
        
        //如果拖动源不是本区域，则将isLocaleDrag置为false
        if (source !=this)
        {
        	isLocaleDrag = false;
        }
        
        if (dropTarget != this && mDragInfo.cell != null && mDragInfo.cellIndex !=-1)
        {
        	CellLayout cellLayout = (CellLayout) getChildAt(mDragInfo.screen);
        	mDragController.sortView(cellLayout, mDragInfo.cellIndex, cellLayout.getChildCount() - 1);
        	cellLayout.removeView(mDragInfo.cell);
        	LauncherModel.setSortIconInfo(R.id.workspace, mDragInfo.screen);
        } 
        
        resetDragInfo();
        
    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "-------------------------- end --------------------------------------"); 
        }
    }

	public void onRestoreDragIcon(final Object dragInfo) 
	{
    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------start onRestoreDragIcon()--------------------------------------"); 
        }
    	
    	Mogoo_FolderController folderController = mLauncher.getFolderController();
        folderController.setCanActive(false);
    	
        isLocaleDrag = false;
        
        if (Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "call onRestoreDragIcon:");
        }
        
        //add by 张永辉 2011-34 不接收widget
        if(dragInfo instanceof LauncherAppWidgetInfo)
        {
//            if (mDragInfo != null && mDragInfo.cell !=null) 
//            {
//                mDragInfo.cell.setVisibility(View.VISIBLE);
//            }
    	    resetDragInfo();
            return;
        }
        //end
        
 
     // 根据dragInfo创建一个临时插入的图标对象
        if (dragInfo ==null)
        	return;
        
       
        int dragScreen =-1;
        // 从该区域拖动图标到其他区域后，释放拖动到图标
    	if (mDragInfo.cellIndex == -1)
    	{
    		if (this.isFullOfCurrentScreen(getCurrentScreen()))
    		{
    			int screen = this.getUnfullScreen(getCurrentScreen());
    			dragScreen = screen;
    		}else
    		{
    			dragScreen = getCurrentScreen();
    		}
    		
    		if (dragScreen >-1)
            {
    		    final int dragScreenTemp = dragScreen;
    		    //会发生回弹时正在排序使得刷新的图标冲突，将回弹图标延时一下错开排序刷新时间
    		    final int screen = dragScreen;
    		    handler.postDelayed(new Runnable() {
                    public void run() {
                       View view = createIcon(dragInfo);
//                      mDragInfo.cell = view;
                       CellLayout currentCellLayout = (CellLayout) getChildAt(dragScreenTemp);
                       addView(currentCellLayout, view, false); 
                       
                       if(view instanceof Mogoo_BubbleTextView)
                       {
                        ((Mogoo_BubbleTextView)view).startVibrate(mIconCache, 0);
                       }
                       
                       LauncherModel.setSortIconInfo(R.id.workspace, screen);
                       LauncherModel.saveAllIconInfo(getContext());
                    }
                }, 200);
            }
    		
    	}else
    	{//在该区域拖动图标到满屏后，释放拖动到图标
    		dragScreen = mDragInfo.screen;
    		if (mDragInfo.cell != null)
    		{
    			mDragInfo.cell.setVisibility(VISIBLE);
           		((Mogoo_BubbleTextView)mDragInfo.cell).startVibrate(mIconCache, 0);
    		}
    		
    	}
    	
    	
        
         
         
         
         
//        if (this.isFullOfCurrentScreen(getCurrentScreen()))
//        {
//        	int screen = this.getUnfullScreen(getCurrentScreen());
//        	System.out.println("onRestoreDragIcon: screen =" + screen);
//        	if (screen >-1)
//        	{
//        		System.out.println("onRestoreDragIcon: screen >-1" );
//        		CellLayout currentCellLayout = (CellLayout) getChildAt(screen);
//           	 	addView(currentCellLayout, view, false);
//           	 	
//           	 	if(view instanceof MT_BubbleTextView){
//           	 		((MT_BubbleTextView)view).startVibrate(mIconCache, 0);
//           	 	}
//        	}
//        }else
//        {
//        	CellLayout currentCellLayout = (CellLayout) getChildAt(getCurrentScreen());
//        	addView(currentCellLayout, view, false);
//        	
//    		System.out.println("onRestoreDragIcon: isNotFullOfCurrentScreen" );
//        	
//        	if(view instanceof MT_BubbleTextView){
//       	 		((MT_BubbleTextView)view).startVibrate(mIconCache, 0);
//       	 	}
//        }
        
	    resetDragInfo();
	    findUnVibrate();

	    if (dragScreen >-1)
	    {
	    	 LauncherModel.setSortIconInfo(R.id.workspace, dragScreen);
	    }
       
    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "------------------------- end --------------------------------------"); 
        }
	}
	
	/*
	 * 检查是否当前屏有未抖动的图标
	 */
	private void findUnVibrate(){
	   CellLayout currentCelllayout = (CellLayout) getChildAt(getCurrentScreen());
	   
	   if(currentCelllayout == null){
	       return;
	   }
	   
	   int size = currentCelllayout.getChildCount();
	   View cell = null;
	   
	   for(int i = 0; i < size; i++){
	       cell = currentCelllayout.getChildAt(i);
	       if(cell != null && cell instanceof Mogoo_BubbleTextView && !((Mogoo_BubbleTextView)cell).isVibration()){
	           mIconCache.recycle(((ShortcutInfo)cell.getTag()).intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_VIBRATION);
	           ((Mogoo_BubbleTextView)cell).startVibrate(mIconCache, 0);
	       }
	   }
	}
	

    /**
     * 初始workspace后调用该方法
     */
    protected void onFinishInflate() {
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SCREEN_COUNT, this.getChildCount());

        if (Mogoo_GlobalConfig.LOG_DEBUG) {
            Log.d(TAG, "init workspace over ..---onFinishInflate");
        }
    }

    /**
     * 从workspace中移除桌面图标，并从数据库中也清除相关的桌面图标信息,如果为最后一屏且为空且大于最少屏，则删屏
     * @ author: 张永辉
     * @param packageNameList 要移除的桌面图标的对应包名
     */
    void removeItems(List<String> packageNameList) {
        final int count = getChildCount();
        final PackageManager manager = getContext().getPackageManager();
        final AppWidgetManager widgets = AppWidgetManager.getInstance(getContext());

        final HashSet<String> packageNames = new HashSet<String>();
        final int appCount = packageNameList.size();
        for (int i = 0; i < appCount; i++) {
            packageNames.add(packageNameList.get(i));
         }

        for (int i = 0; i < count; i++) {
            
            if(Mogoo_GlobalConfig.isSearchScreen(i)||Mogoo_GlobalConfig.isWidgetScreen(i)){
                continue ;
            }
            
            final CellLayout layout = (CellLayout) getChildAt(i);
            
            // Avoid ANRs by treating each screen separately
            post(new Runnable() {
                public void run() {
                    final ArrayList<View> childrenToRemove = new ArrayList<View>();
                    childrenToRemove.clear();

                    int childCount = layout.getChildCount();
                    for (int j = 0; j < childCount; j++) {
                        final View view = layout.getChildAt(j);                                      

                        Object tag = view.getTag();

                        if (tag instanceof ShortcutInfo) {
                            final ShortcutInfo info = (ShortcutInfo) tag;
                            final Intent intent = info.intent;
                            final ComponentName name = intent.getComponent();

                            if (Intent.ACTION_MAIN.equals(intent.getAction()) && name != null) {
                                for (String packageName : packageNames) {
                                    if (packageName.equals(name.getPackageName())) {
                                        // TODO: This should probably be done on
                                        // a worker thread
                                        if(Mogoo_GlobalConfig.LOG_DEBUG){
                                            Log.d(TAG, "-----------uninstall deleteItemFromDatabase----------------") ;
                                        }
                                        
                                        if(view instanceof Mogoo_BubbleTextView){
                                            ((Mogoo_BubbleTextView)view).stopVibrate();
                                        }
                                        
                                        mIconCache.recycle(info.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
                                        mIconCache.remove(info.intent.getComponent());
                                        LauncherModel.deleteItemFromDatabase(mLauncher, info);
                                        
                                        //removeChild(layout,view);                                       
                                        
                                        // end
                                        childrenToRemove.add(view);
                                    }
                                }
                            }
                        } else if (tag instanceof UserFolderInfo) {
                            final UserFolderInfo info = (UserFolderInfo) tag;
                            final ArrayList<ShortcutInfo> contents = info.contents;
                            final ArrayList<ShortcutInfo> toRemove = new ArrayList<ShortcutInfo>(1);
                            final int contentsCount = contents.size();
                            boolean removedFromFolder = false;

                            for (int k = 0; k < contentsCount; k++) {
                                final ShortcutInfo appInfo = contents.get(k);
                                final Intent intent = appInfo.intent;
                                final ComponentName name = intent.getComponent();

                                if (Intent.ACTION_MAIN.equals(intent.getAction()) && name != null) {
                                    for (String packageName : packageNames) {
                                        if (packageName.equals(name.getPackageName())) {
                                            toRemove.add(appInfo);
                                            // TODO: This should probably be
                                            // done on a worker thread
                                            LauncherModel
                                                    .deleteItemFromDatabase(mLauncher, appInfo);
                                            removedFromFolder = true;
                                        }
                                    }
                                }
                            }

                            contents.removeAll(toRemove);
                            if (removedFromFolder) {
                                final Folder folder = getOpenFolder();
                                if (folder != null)
                                    folder.notifyDataSetChanged();
                            }
                        } else if (tag instanceof LiveFolderInfo) {
                            final LiveFolderInfo info = (LiveFolderInfo) tag;
                            final Uri uri = info.uri;
                            final ProviderInfo providerInfo = manager.resolveContentProvider(
                                    uri.getAuthority(), 0);

                            if (providerInfo != null) {
                                for (String packageName : packageNames) {
                                    if (packageName.equals(providerInfo.packageName)) {
                                        // TODO: This should probably be done on
                                        // a worker thread
                                        LauncherModel.deleteItemFromDatabase(mLauncher, info);
                                        childrenToRemove.add(view);
                                    }
                                }
                            }
                        } else if (tag instanceof LauncherAppWidgetInfo) {
                            final LauncherAppWidgetInfo info = (LauncherAppWidgetInfo) tag;
                            final AppWidgetProviderInfo provider = widgets
                                    .getAppWidgetInfo(info.appWidgetId);
                            if (provider != null) {
                                for (String packageName : packageNames) {
                                    if (packageName.equals(provider.provider.getPackageName())) {
                                        // TODO: This should probably be done on
                                        // a worker thread
                                        LauncherModel.deleteItemFromDatabase(mLauncher, info);
                                        childrenToRemove.add(view);
                                    }
                                }
                            }
                        }
                    }

                    childCount = childrenToRemove.size();
                    for (int j = 0; j < childCount; j++) {
                        View child = childrenToRemove.get(j);
                        
                        if(Mogoo_GlobalConfig.LOG_DEBUG){
                            Log.d(TAG, "-----------uninstall removeViewInLayout----------------") ;
                        }
                        
                        layout.removeViewInLayout(child);
                        
                        // add by 张永辉 2011-01-06 整理桌面,补全空位
                        Object tag = child.getTag();
                        if (tag instanceof ShortcutInfo) 
                        {
                            final ShortcutInfo info = (ShortcutInfo) tag;
                            
//                            if(MT_GlobalConfig.LOG_DEBUG){
//                                Log.d(TAG, "uninstall reload: cellX = "+info.cellX+" cellY ="+info.cellY) ;
//                            }
                            
                            reload(info.screen, new int[] { info.cellX, info.cellY});
                        }
                        //------------------end---------------
                        
                        
                        if (child instanceof DropTarget) {
                            mDragController.removeDropTarget((DropTarget) child);
                        }
                        
                        //add by 袁业奔 2011-9-7 如果为最后一屏且为空且大于最少屏，则删屏
                        if (tag instanceof ShortcutInfo) {
                        	final ShortcutInfo info = (ShortcutInfo) tag;
                        	final CellLayout celllayout = (CellLayout)getChildAt(info.screen);
                        	if(celllayout != null && isLastCellLayout(info.screen) && celllayout.getChildCount() == 0
                        			&& getChildCount()>Mogoo_GlobalConfig.getWorkspaceScreenMinCount()){
                        		removeView(celllayout);
//                        		Mogoo_BitmapUtils.clearIndicatorImages();
                        		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SCREEN_COUNT, getChildCount());
                        		int[] screenType = Mogoo_GlobalConfig.createWorkspaceScreenType(getChildCount());
            					Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SCREEN_TYPE,screenType);
                        		setCurrentScreen(info.screen-1);
                        	}
                        }
                        //----------------end---------------
                    }                   

//                    if (childCount > 0) {
//                        layout.requestLayout();
//                        layout.invalidate();
//                    }
                }
            });
        }
    }
    /**
     * 识别排序插入点index
     */
    public int findTargetIndex(DragView dragView, ViewGroup parent) 
    {
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "--------------------start findTargetIndex()--------------------------------------startIndex="+dragView.getStartIndex()); 
        }
        
        int targetIndex = -1;
        
        //CellLayout cellLayout = (CellLayout) this.getChildAt(mCurrentScreen);

        CellLayout cellLayout = (CellLayout) parent;
        // 拖动图标左上角的坐标
        int x = dragView.getScreenLeft();
        int y = dragView.getScreenTop() - this.getStatusBarHeight();
        int width = dragView.getWidth();
        int height = dragView.getHeight();

        // 取得拖动图标相交的第一个单元格的素引号
        int index = getCellIndexByDragView(dragView);
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "current screen child count:"+cellLayout.getChildCount()); 
        }
        
        if (index == -1) {
            targetIndex = cellLayout.getChildCount() - 1; // 放于最后一个位置
        } else if (index == cellLayout.getChildCount() - 1) // 当第一个相交单元格为最后一个图标时
        {
            //是否触发文件夹
            if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolder(cellLayout, x, y, width, height, index)){
                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index ;
            }
            else{
                // 从前往后拖
                if (index >= dragView.getStartIndex()) {
                    targetIndex = index;
                }
                // 从后往前拖
                else {
                    targetIndex = index + 1;
                }
            }
        } else if (index >= cellLayout.getChildCount()) {
            targetIndex = cellLayout.getChildCount() - 1; // 放于最后一个位置
        } else {
            // index单元格
            CellEntry entry = cellLayout.getCellEntry(index);

            // 拖动图标左上角是否在index所在的单元格内
            if (x >= entry.left && x <= entry.right && y >= entry.top && y <= entry.bottom) 
            {
                //在里面
                targetIndex = findTargetIndexInner(cellLayout, dragView, x, y, width, height, index, entry);
            } else {
                //在外面
                targetIndex = findTargetIndexOuter(cellLayout, dragView, x, y, width, height, index, entry) ;
            }
        }

        // 如果目标位置大于等于图标总数目时，则放于最后一个位置
        if(targetIndex<Mogoo_GlobalConfig.FOLDER_BASE_INDEX){
            if (targetIndex >= cellLayout.getChildCount()) {
                targetIndex = cellLayout.getChildCount() - 1;
            }
        }

        // 重置拖动开始索引值
        if (targetIndex != -1) {
            //add by 张永辉 2011-7-22
            if(targetIndex>=Mogoo_GlobalConfig.FOLDER_BASE_INDEX){
                dragView.setStartIndex(targetIndex-Mogoo_GlobalConfig.FOLDER_BASE_INDEX);
            }
            //end add 
            else{
                dragView.setStartIndex(targetIndex);
            }
            
        }

        if (Mogoo_GlobalConfig.LOG_DEBUG) {
            Log.d(TAG, "Find Target Index:" + targetIndex);
            Log.d(TAG, "------------------------- end --------------------------------------"); 
        }

        return targetIndex;
    }
    
    /**
     * 拖动图标左上角在单元格里面
     *@author: 张永辉
     *@Date：2011-3-22
     *@param cellLayout
     *@param dragView
     *@param x
     *@param y
     *@param width
     *@param height
     *@param index
     *@param entry
     *@return
     */
    private int findTargetIndexInner(CellLayout cellLayout,DragView dragView,int x,int y,int width,int height,int index,CellEntry entry){// 在里面
        int targetIndex = -1 ;
        
        // 看右上角在哪个单元格内
        int rightTopIndex = this.getCellIndexByCoordinate(x + width, y);
        // 看右下角在哪个单元格内
        int rightBottomIndex = this.getCellIndexByCoordinate(x + width, y + height);
        // 看左下角在哪个单元格内
        int leftBottomIndex = this.getCellIndexByCoordinate(x, y + height);

        if (Mogoo_GlobalConfig.LOG_DEBUG) {
            Log.d(TAG, "leftBottomIndex=" + leftBottomIndex + " rightTopIndex=" + rightTopIndex
                    + " rightBottomIndex=" + rightBottomIndex);
        }
        
        //行宽
        int rowWidth = Mogoo_GlobalConfig.getWorkspaceLongAxisCells(Mogoo_GlobalConfig.isLandscape()) ;

        // 左上角与左下角在同一个单元格内
        if (leftBottomIndex == index) {
            // 如果四个角在同一个单元格内
            if (rightTopIndex == index) {
                //是否打开了文件夹的功能
                if(Mogoo_GlobalConfig.ICON_FOLDER){
                    targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index ;
                }
                else{
                    targetIndex = index;
                }
            }
            // 不在同一个单元格内，则通过拖动方向来返回单元格号
            else {
                //是否触发文件夹功能
                if(Mogoo_GlobalConfig.ICON_FOLDER&&triggerFolderInnerH(cellLayout, x,width, height, leftBottomIndex, true)){
                    targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + leftBottomIndex ;
                }
                else if(Mogoo_GlobalConfig.ICON_FOLDER&&rightBottomIndex!=-1&&triggerFolderInnerH(cellLayout, x,width, height, rightBottomIndex, false)){
                    targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightBottomIndex ;
                }
                else{
                    // 从前往后拖
                    if (index - dragView.getStartIndex() >= 0) {
                        targetIndex = index;
                    }
                    // 从后往前拖
                    else {
                        if(rightTopIndex!=-1)
                        {
                            targetIndex = rightTopIndex;
                        }
                        else
                        {
                            targetIndex = index ;;
                        }
                    }
                }
            }
        }
        // 左上角与左下角不在同一个单元格内
        else {
            // 如果左上角与右上角在同一个单元格内
            if (index == rightTopIndex) {
                // 左下角在外面，则取左上角所在单元格号
                if (leftBottomIndex == -1) {
                    //是否触发文件夹
                    if(Mogoo_GlobalConfig.ICON_FOLDER&&triggerFolderInnerV(cellLayout, y, width,height, index, true)){
                        targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index ;
                    }else{
                        targetIndex = index;
                    }
                }
                // 左下角在单元格内，则看是否跨了三个单元格
                else {
                    //跨了三个单元格高度，则取中间的单元格号
                    if(leftBottomIndex-index == rowWidth*2){
                        //是否打开了文件夹功能
                        if(Mogoo_GlobalConfig.ICON_FOLDER){
                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index + rowWidth ;
                        }else{
                            targetIndex = index +  rowWidth  ;
                        }
                    }
                    //跨了二个单元格高度,则看上半部分与下半部分的面积大小
                    else{
                        //是否打开图标文件夹
                        if(Mogoo_GlobalConfig.ICON_FOLDER&&triggerFolderInnerV(cellLayout, y, width,height,  index, true)){
                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index ; 
                        }
                        else if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolderInnerV(cellLayout, y, width,height,  leftBottomIndex, false)){
                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + leftBottomIndex ;
                        }
                        else{
                            int topPart = entry.bottom - y;
                            int bottomPart = (y + height)
                                    - cellLayout.getCellEntry(leftBottomIndex).top;

                            // 上半部分大
                            if (topPart >= bottomPart) {
                                targetIndex = index;
                            }
                            // 下半部分大
                            else {
                                targetIndex = leftBottomIndex;
                            }
                        }
                    }
                }
            }
            // 如果左上角与右上角不在同一个单元格内
            else {
                // 左下角不在单元格内，看是否跨了二个单元格高度。
                if (leftBottomIndex == -1) {
                    //左边中点所在单元格号
                    int leftMiddleIndex = this.getCellIndexByCoordinate(x, y+height/2) ;
                    //占一个单元格高度,则取上半部分，再看拖动方向
                    if(leftMiddleIndex==index||leftMiddleIndex==-1){
                        //是否触发文件夹功能
                        
                        //add by 张永辉 2011-7-21
                        //是否跨了三列或跨了二列但右上角在单元格外面 , 看中间单元格
                        if(rightTopIndex-index==2||(rightTopIndex==-1&&(rightTopIndex-index-1)%rowWidth==0)){
                            if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolderRect(cellLayout, y, height, index+1, true)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index +1 ;
                            }else{
                                targetIndex = index +1 ;
                            }
                        }
                        //end add
                        
                        //跨了二列
                        else{
                            if(Mogoo_GlobalConfig.ICON_FOLDER&&triggerFolder(cellLayout, x, y, width, height, index, true,true)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index ;
                            }else if(Mogoo_GlobalConfig.ICON_FOLDER && rightTopIndex!=-1
                                    && triggerFolder(cellLayout, x, y, width, height, rightTopIndex, false, true)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightTopIndex ;
                            }
                            else{
                                // 从前往后拖
                                if (index - dragView.getStartIndex() >= 0) {
                                    targetIndex = index;
                                }
                                // 从后往前拖
                                else {
                                    //如果右上角在外面，则返回左上角所在单元格号，否则返回右上角所在单元格号
                                    if(rightTopIndex==-1){
                                        targetIndex = index ;
                                    }else{
                                        targetIndex = rightTopIndex;
                                    }
                                }
                            }
                        }
                        
                    }
                    //占二个单元格高度
                    else {
                        //是否触发文件夹打开功能
                        
                        //add by 张永辉 2011-7-21
                        //是否跨了三列或跨了二列但右上角在单元格外面 , 看中间单元格
                        if(rightTopIndex-index==2||(rightTopIndex==-1&&(rightTopIndex-index-1)%rowWidth==0)){
                            if(Mogoo_GlobalConfig.ICON_FOLDER && (index+rowWidth+1)<Mogoo_GlobalConfig.getWorkspaceCellCounts()){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index+rowWidth+1 ;
                            }else{
                                targetIndex = index+rowWidth+1 ;
                            }
                        }
                        //end add
                        
                        //跨二列
                        else{
                            if(Mogoo_GlobalConfig.ICON_FOLDER&&triggerFolderOuterH(cellLayout, x, width, index+rowWidth, true)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index + rowWidth ;
                            }
                            else if(Mogoo_GlobalConfig.ICON_FOLDER && rightTopIndex!=-1 &&
                                    triggerFolderOuterH(cellLayout, x, width, rightTopIndex+rowWidth, false)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightTopIndex+rowWidth ;
                            }
                            else{
                                // 从前往后拖
                                if (index+rowWidth - dragView.getStartIndex() >= 0) {
                                    targetIndex = index+rowWidth;
                                }
                                // 从后往前拖
                                else {
                                    //如果右上角在外面，则返回左上角下面的单元格号，否则返回右上角下面所在单元格号
                                    if(rightTopIndex==-1){
                                        targetIndex = index+rowWidth ;
                                    }else{
                                        targetIndex = rightTopIndex+rowWidth;
                                    }
                                }
                            }
                        }
                        
                    }
                    
                }
                // 左下角在单元格内，看是否跨越了三个单元格高度，
                else {
                    //跨了三行
                    if(leftBottomIndex-index==rowWidth*2){
                        //是否触发文件夹打开功能
                        
                        //add by 张永辉 2011-7-21
                        //是否跨了三列或跨了二列但右上角在单元格外面 , 看中间单元格
                        if(rightTopIndex-index==2||(rightTopIndex==-1&&(rightTopIndex-index-1)%rowWidth==0)){
                            if(Mogoo_GlobalConfig.ICON_FOLDER && (index+rowWidth+1)<Mogoo_GlobalConfig.getWorkspaceCellCounts()){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index+rowWidth+1 ;
                            }else{
                                targetIndex = index+rowWidth+1 ;
                            }
                        }
                        //end add
                        
                        //跨二列
                        else{
                            if(Mogoo_GlobalConfig.ICON_FOLDER&&triggerFolderOuterH(cellLayout, x, width, index+rowWidth, true)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index + rowWidth ;
                            }
                            else if(Mogoo_GlobalConfig.ICON_FOLDER && rightTopIndex!=-1 &&
                                    triggerFolderOuterH(cellLayout, x, width, rightTopIndex+rowWidth, false)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightTopIndex+rowWidth ;
                            }
                            else{
                                // 从前往后拖
                                if (index+rowWidth - dragView.getStartIndex() >= 0) {
                                    targetIndex = index+rowWidth;
                                }
                                // 从后往前拖
                                else {
                                    //如果右上角在外面，则返回左上角下面的单元格号，否则返回右上角下面所在单元格号
                                    if(rightTopIndex==-1){
                                        targetIndex = index+rowWidth ;
                                    }else{
                                        targetIndex = rightTopIndex+rowWidth;
                                    }
                                }
                            }
                        }
                        
                    }
                    //跨了二行，则看上下部分哪个部分大
                    else{
                        //是否触发文件打开功能
                        
                        //add by 张永辉 2011-7-21
                        //是否跨了三列或跨了二列但右上角在单元格外面 , 看中间单元格
                        if(rightTopIndex-index==2||(rightTopIndex==-1&&(rightTopIndex-index-1)%rowWidth==0)){//(rightTopIndex-index-1)%rowWidth==0
                            if(Mogoo_GlobalConfig.ICON_FOLDER && (index+rowWidth+1)<Mogoo_GlobalConfig.getWorkspaceCellCounts()){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index+rowWidth+1 ;
                            }else{
                                targetIndex = index+rowWidth+1 ;
                            }
                        }
                        //end add
                        
                        else{
                            if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolder(cellLayout, x, y, width, height, index, true, true)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index ;
                            }
                            else if(Mogoo_GlobalConfig.ICON_FOLDER && rightTopIndex!=-1 &&
                                    triggerFolder(cellLayout, x, y, width, height, rightTopIndex, false, true)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightTopIndex ;
                            }
                            else if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolder(cellLayout, x, y, width, height, leftBottomIndex, true, false)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + leftBottomIndex ;
                            }
                            else if(Mogoo_GlobalConfig.ICON_FOLDER && rightBottomIndex!=-1 &&
                                    triggerFolder(cellLayout, x, y, width, height, rightBottomIndex, false, false)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightBottomIndex ;
                            }
                            else{
                                int topPart = entry.bottom - y;
                                int bottomPart = (y + height)
                                        - cellLayout.getCellEntry(leftBottomIndex).top;

                                // 上半部分大,则再看拖动方向
                                if (topPart >= bottomPart) {
                                    
                                    //add by 张永辉 2011-7-19
                                    //是否跨了三列
                                    if(rightTopIndex!=-1 && rightTopIndex-index==2 ){
                                        if(Mogoo_GlobalConfig.ICON_FOLDER){
                                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index+1;
                                        }else{
                                            targetIndex = index+1;
                                        }
                                    }
                                    //跨了二列且右上角在单元格外面
                                    else if(rightTopIndex ==-1 && (index+1)%rowWidth!=0){
                                        if(Mogoo_GlobalConfig.ICON_FOLDER){
                                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index+1;
                                        }else{
                                            targetIndex = index+1;
                                        }
                                    }
                                    //end
                                    
                                    // 从前往后拖
                                    else if (index - dragView.getStartIndex() >= 0) {
                                        targetIndex = index;
                                    }
                                    // 从后往前拖
                                    else {
                                        //如果右上角在边外，则返回index,否则返回右上角所在的单元格号
                                        if(rightTopIndex==-1){
                                            targetIndex = index;
                                        }else{
                                            targetIndex = rightTopIndex;
                                        }
                                    }
                                }
                                // 下半部分大,则再看拖动方向
                                else {
                                    //add by 张永辉 2011-7-19
                                    //是否跨了三列
                                    if(rightBottomIndex!=-1 && rightBottomIndex-index==2 ){
                                        if(Mogoo_GlobalConfig.ICON_FOLDER){
                                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + leftBottomIndex+1;
                                        }else{
                                            targetIndex = leftBottomIndex+1;
                                        }
                                    }
                                    //跨了二列且右下角在单元格外面
                                    else if(rightBottomIndex ==-1 && (leftBottomIndex+1)%rowWidth!=0){
                                        if(Mogoo_GlobalConfig.ICON_FOLDER){
                                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + leftBottomIndex+1;
                                        }else{
                                            targetIndex = leftBottomIndex+1;
                                        }
                                    }
                                    //end
                                    
                                    // 从前往后拖
                                    else if (leftBottomIndex - dragView.getStartIndex() >= 0) {
                                        targetIndex = leftBottomIndex;
                                    } else {
                                        //如果右下角在边外，则返回左下角所在单元格号,否则返回右下角所在的单元格号
                                        if(rightBottomIndex==-1){
                                            targetIndex = leftBottomIndex;
                                        }else{
                                            targetIndex = rightBottomIndex;
                                        }
                                    }
                                }
                            }
                        }
                        
                    }
                }
            }
        }
        
        return targetIndex ;
    }
    
    /**
     * 拖动图标左上角不在单元格里
     *@author: 张永辉
     *@Date：2011-3-22
     *@param cellLayout
     *@param dragView
     *@param x
     *@param y
     *@param width
     *@param height
     *@param index
     *@param entry
     *@return
     */
    private int findTargetIndexOuter(CellLayout cellLayout,DragView dragView,int x,int y,int width,int height,int index,CellEntry entry){// 不在里面
        int targetIndex = -1 ;
        
        // 看右上角在哪个单元格内
        int rightTopIndex = this.getCellIndexByCoordinate(x + width, y);
        // 看右下角在哪个单元格内
        int rightBottomIndex = this.getCellIndexByCoordinate(x + width, y + height);
        // 看左下角在哪个单元格内
        int leftBottomIndex = this.getCellIndexByCoordinate(x, y + height);

        if (Mogoo_GlobalConfig.LOG_DEBUG) {
            Log.d(TAG, "leftBottomIndex=" + leftBottomIndex + " rightTopIndex=" + rightTopIndex
                    + " rightBottomIndex=" + rightBottomIndex);
        }
        
        //行宽
        int rowWidth = Mogoo_GlobalConfig.getWorkspaceLongAxisCells(Mogoo_GlobalConfig.isLandscape()) ;
        
        // 如果左下角不在单元格内
        if (leftBottomIndex == -1) {
            // 如果右上角不在单元格内，则返回第一个相交的单元格号
            if (rightTopIndex == -1) {
                if (rightBottomIndex != -1) {
                    //触发图标文件夹打开功能
                    if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolder(cellLayout, x, y, width, height, index, false, false)){
                        targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index ;
                    }else{
                        targetIndex = index;
                    }
                } else {
                    targetIndex = -1;
                }
            }
            // 如果右上角在单元格内，且右上角和右下角都在同一个单元格内，则返回右上角所在的单元格号
            else if (rightTopIndex == rightBottomIndex) {
                //触发图标文件夹打开
                if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolderInnerH(cellLayout, x,width, height, rightTopIndex, false)){
                    targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightTopIndex ;
                }else{
                    targetIndex = rightTopIndex;
                }
            }
            // 如果右上角和右下角所在的单元格不相同，则比较拖动图标在这二个单元格中哪个占用面积大，则返回哪个单元格号
            else if (rightTopIndex != rightBottomIndex) {
                // 右下角在单元格外面
                if (rightBottomIndex == -1){
                    //右边中点所在单元格
                    int rightMiddleIndex = this.getCellIndexByCoordinate(x+width, y+height/2) ;
                    
                    //add by 张永辉 2011-7-21 
                    //跨了二列
                    if(rightTopIndex%rowWidth!=0&&(rightTopIndex-1)%rowWidth==0){
                        //一行
                        if(rightTopIndex==rightMiddleIndex||rightMiddleIndex==-1){
                            if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolderRect(cellLayout, y, height, rightTopIndex-1, true)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightTopIndex-1 ;
                            }else{
                                targetIndex = rightTopIndex-1;
                            }
                        }
                        //二行
                        else{
                            if(Mogoo_GlobalConfig.ICON_FOLDER ){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightMiddleIndex-1 ;
                            }else{
                                targetIndex = rightMiddleIndex-1;
                            }
                        }
                    }
                    //end add
                    
                    //跨了一列
                    //如果右边中点与右上角在同一个单元格内，则返回右上角所在单元格号
                    else if(rightTopIndex==rightMiddleIndex) {
                        //触发图标文件夹打开
                        if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolder(cellLayout, x, y, width, height, rightTopIndex, false, true)){
                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightTopIndex ;
                        }else{
                            targetIndex = rightTopIndex;
                        }
                    }
                    //右边中点在外面
                    else if(rightMiddleIndex==-1){
                        //触发图标文件夹打开
                        if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolder(cellLayout, x, y, width, height, rightTopIndex, false, true)){
                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightTopIndex ;
                        }else{
                            targetIndex = rightTopIndex;
                        }
                    }
                    //右边中间在里面，则取右中点所在单元格号
                    else {
                        //触发图标文件夹打开
                        if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolderOuterH(cellLayout, x, width, rightMiddleIndex, false)){
                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightMiddleIndex;
                        }else{
                            targetIndex = rightMiddleIndex;
                        }
                    }
                    
                }
                // 右下角在单元格里面
                else {
                    //如果右边跨越三个单元格，则选中间的单元格
                    if(rightBottomIndex-rightTopIndex==rowWidth*2){
                        //触发图标文件夹打开
                        
                        //add by 张永辉 2011-7-21 
                        //跨了二列
                        if(rightTopIndex%rowWidth!=0&&(rightTopIndex-1)%rowWidth==0&&(index+rowWidth)<Mogoo_GlobalConfig.getWorkspaceCellCounts()){
                            if(Mogoo_GlobalConfig.ICON_FOLDER ){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index+rowWidth ;
                            }else{
                                targetIndex = index+rowWidth ;
                            }
                        }
                        //end add
                        
                        //跨一列
                        else if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolderOuterH(cellLayout, x, width, rightTopIndex + rowWidth, false)){
                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightTopIndex + rowWidth;
                        }else{
                            targetIndex = rightTopIndex + rowWidth ;
                        }
                    }
                    //跨越二个单元格
                    else {
                        //触发图标文件夹打开
                        
                        //add by 张永辉 2011-7-21 
                        //跨了二列
                        if(rightTopIndex%rowWidth!=0&&(rightTopIndex-1)%rowWidth==0){
                            if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolderRect(cellLayout, y, height, rightTopIndex-1, true)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightTopIndex-1 ;
                            }
                            else if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolderRect(cellLayout, y, height, rightBottomIndex-1, false)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightBottomIndex-1 ;
                            }
                            else{
                                int topPart = cellLayout.getCellEntry(rightTopIndex).bottom - y;
                                int bottomPart = (y + height)
                                        - cellLayout.getCellEntry(rightBottomIndex).top;

                                if (topPart >= bottomPart) {
                                    targetIndex = rightTopIndex-1 ;
                                } else {
                                    targetIndex = rightBottomIndex-1 ;
                                } 
                            }
                        }
                        //end add
                        
                        //跨一列
                        else if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolder(cellLayout, x, y, width, height, rightTopIndex, false, true)){
                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightTopIndex ;
                        }
                        else if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolder(cellLayout, x, y, width, height, rightBottomIndex, false, false)){
                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightBottomIndex ;
                        }
                        else{
                            int topPart = cellLayout.getCellEntry(rightTopIndex).bottom - y;
                            int bottomPart = (y + height)
                                    - cellLayout.getCellEntry(rightBottomIndex).top;

                            if (topPart >= bottomPart) {
                                targetIndex = rightTopIndex;
                            } else {
                                targetIndex = rightBottomIndex;
                            } 
                        }
                    }
                }
            }
        }
        // 如果左下角在单元格内
        else if (leftBottomIndex != -1) {
            // 左下角与右下角都在同一个单元格内，看是否跨二行单元
            if (leftBottomIndex == rightBottomIndex) {
                //跨二行
                if(leftBottomIndex-index==rowWidth){
                    //触发图标文件夹打开
                    if(Mogoo_GlobalConfig.ICON_FOLDER){
                        targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index ;
                    }else{
                        targetIndex = index ;
                    }
                }
                //跨一行,则返回左下角所在的单元格号
                else{
                    //触发图标文件夹打开
                    if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolderInnerV(cellLayout, y, width,height,  leftBottomIndex, false)){
                        targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + leftBottomIndex;
                    }else{
                        targetIndex = leftBottomIndex;
                    }
                }
            }
            // 如果不在同一个单元格内，看是否跨二行单元 
            else {
                //跨二行
                if(leftBottomIndex-index==rowWidth){
                    //触发图标文件夹打开
                    
                    //add by 张永辉 2011-7-21
                    //是否跨了三列或跨了二列但右下角在单元格外面 , 看中间单元格
                    if(rightBottomIndex-index==2||(rightBottomIndex==-1&&(rightBottomIndex-index-1)%rowWidth==0)){
                        if(Mogoo_GlobalConfig.ICON_FOLDER ){
                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index+1 ;
                        }else{
                            targetIndex = index+1 ;
                        }
                    }
                    //end add
                    
                    //跨二列或一列
                    else if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolderOuterH(cellLayout, x, width, index, true)){
                        targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index ;
                    }
                    else if(rightBottomIndex!=-1&&Mogoo_GlobalConfig.ICON_FOLDER && triggerFolderOuterH(cellLayout, x, width, index+1, false)){
                        targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index +1 ;
                    }
                    else{
                        // 从前往后拖
                        if (index - dragView.getStartIndex() >= 0) {
                            targetIndex = index;
                        }
                        // 从后往前拖
                        else {
                            targetIndex = index+1;
                        }
                    }
                }
                //跨一行,如果是从前往后拖，则返回左下角所在单元格号，如果从后往前拖则返回右下角所在单元格号
                else{
                    //触发图标文件夹打开
                    
                    //add by 张永辉 2011-7-21
                    //是否跨了三列或跨了二列但右下角在单元格外面 , 看中间单元格
                    if(rightBottomIndex-index==2||(rightBottomIndex==-1&&(rightBottomIndex-index-1)%rowWidth==0)){
                        if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolderRect(cellLayout, y, height, index+1, false)){
                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + index+1 ;
                        }else{
                            targetIndex = index+1 ;
                        }
                    }
                    //end add
                    
                    //跨二列或一列
                    else if(Mogoo_GlobalConfig.ICON_FOLDER && triggerFolder(cellLayout, x, y, width, height, leftBottomIndex, true, false)){
                        targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + leftBottomIndex ;
                    }
                    else if(Mogoo_GlobalConfig.ICON_FOLDER && rightBottomIndex!=-1 && triggerFolder(cellLayout, x, y, width, height, rightBottomIndex, false, false)){
                        targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + rightBottomIndex ;
                    }
                    else{
                        // 从前往后拖
                        if (leftBottomIndex - dragView.getStartIndex() >= 0) {
                            targetIndex = leftBottomIndex;
                        }
                        // 从后往前拖
                        else {
                            if(rightBottomIndex==-1)
                            {
                                targetIndex = leftBottomIndex;
                            }
                            else
                            {
                                targetIndex = rightBottomIndex;
                            }
                        }
                    }
                }
                
            }
        }
        // 如果左下角也不在单元格内，则返回右下角所在的单元格
        else {
            targetIndex = rightBottomIndex != -1 ? rightBottomIndex : -1;
        }
        
        return targetIndex ;
    }
    
    /**
     * 从父视图中移除某子视图 
     *@author:张永辉 
     *@param layout 父视图
     *@param child 子视图
     */
    private void removeChild(CellLayout layout,View child){
        layout.removeViewInLayout(child);
        if (child instanceof DropTarget) {
            mDragController.removeDropTarget((DropTarget) child);
        }
    }

    /**
     * 更新桌面图标 
     * @ author: 张永辉
     * @param packageNameList 要更新的应用的包名
     */
    void updateShortcuts(List<String> packageNameList) {
        final PackageManager pm = mLauncher.getPackageManager();

        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final CellLayout layout = (CellLayout) getChildAt(i);
            int childCount = layout.getChildCount();
            for (int j = 0; j < childCount; j++) {
                final View view = layout.getChildAt(j);
                Object tag = view.getTag();
                if (tag instanceof ShortcutInfo) {
                    ShortcutInfo info = (ShortcutInfo) tag;
                    // We need to check for ACTION_MAIN otherwise getComponent()
                    // might
                    // return null for some shortcuts (for instance, for
                    // shortcuts to
                    // web pages.)
                    final Intent intent = info.intent;
                    final ComponentName name = intent.getComponent();
                    // update by 张永辉 2010-12-14
                    // if (info.itemType ==
                    // LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
                    if ((info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT)
                            // end
                            && Intent.ACTION_MAIN.equals(intent.getAction()) && name != null) {
                        final int appCount = packageNameList.size();
                        for (int k = 0; k < appCount; k++) {
                            // ApplicationInfo app = apps.get(k);
                            if (packageNameList.get(k).equals(name.getPackageName())) {
                                mIconCache.remove(name);
                                mIconCache.recycle(name, Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
                                info.setIcon(mIconCache.getIcon(info.intent));
                                ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null,
                                        new FastBitmapDrawable(info.getIcon(mIconCache)), null,
                                        null);

                                String title = pm.resolveActivity(info.intent, 0).activityInfo
                                        .loadLabel(pm).toString();

                                if (Mogoo_GlobalConfig.LOG_DEBUG) {
                                    Log.d(TAG, "update info.title:" + info.title);
                                    Log.d(TAG, "update title:" + title);
                                }

                                info.title = title;
                                LauncherModel.updateItemInDatabase(mLauncher, info);
                                // end
                            }
                        }
                    }
                }
            }
        }
    }


    /**
     * 得到排序的Handler 
     * @ author: 邓炜 
     * @return 当前的Handler
     */
    protected Handler getSortHandler() {
        return this.handler;
    }

    /*
     * 切入切出search事件 黄悦
     */
    public void switchAboutSearch(int whichScreen) {
        int sreachNum = Mogoo_GlobalConfig.getSearchScreen();
        View editText = Mogoo_ComponentBus.getInstance().getActivityComp(R.id.search_src_text,
                getContext());
        final View dragLayer = Mogoo_ComponentBus.getInstance().getActivityComp(R.id.drag_layer,
                getContext());
        if (whichScreen == sreachNum) {
        	// editText.setFocusable(true);
            editText.requestFocus();
            dragLayer.getBackground().setAlpha(SearchLayout.SEARCH_SCREEN_ALPHA);
//        } else if (whichScreen == sreachNum - 1 || whichScreen == sreachNum + 1) {
        }else 
        {
        	// editText.setFocusable(false);
            editText.clearFocus();
            dragLayer.getBackground().setAlpha(0);
        }
        dragLayer.invalidate();
    }

    /**
     * 屏幕切换管制 返回true时表示不能切换到目标屏
     * @ author: 魏景春
     * @param deltaX：X轴移动的距离
     */
    private boolean switchWidgetFilter(float deltaX) {
        boolean switchToWidget = false;
        // ------------add by 魏景春 2011-1-27---------
        // 是否切换到widget屏
        if (Mogoo_GlobalConfig.isWidgetScreen(getCurrentScreen() + 1) && deltaX > 0) {
            switchToWidget = true;
        } else if (Mogoo_GlobalConfig.isWidgetScreen(getCurrentScreen() - 1) && deltaX < 0) {
            switchToWidget = true;
        }

        // ---------------end---------------------
        
//        if(MT_GlobalConfig.LOG_DEBUG){
//            Log.d(TAG, "switchWidgetFilter deltaX:"+deltaX+" switchToWidget:"+switchToWidget+
//                    " mCurrentScreen + 1="+MT_GlobalConfig.isWidgetScreen(mCurrentScreen + 1)+
//                    " mCurrentScreen - 1="+MT_GlobalConfig.isWidgetScreen(mCurrentScreen - 1));
//        }

        return Mogoo_VibrationController.isVibrate && switchToWidget;
    }
    
    

    /*
     * 屏幕切换管制 返回true时表示不能切换到目标屏 黄悦
     */    
    private boolean switchFilter(float deltaX) {
        boolean switchToSearch = false;
        // 能否切换到另一屏控制
        if (getCurrentScreen() == mSearchScreen - 1 && deltaX > 0) {
            switchToSearch = true;
        } else if (getCurrentScreen() == mSearchScreen + 1 && deltaX < 0) {
            switchToSearch = true;
        }

        if (switchToSearch
                && (getScrollX() + deltaX > (mSearchScreen + 1) * Mogoo_GlobalConfig.getScreenWidth() || getScrollX() < (mSearchScreen) * Mogoo_GlobalConfig.getScreenWidth())) {
            switchToSearch = false;
        }

        return Mogoo_VibrationController.isVibrate && switchToSearch;
    }

    /**
     * 拖动图标换屏前的处理-----当前屏先排序，然后把拖动图标从当前屏移除，并添加到下一屏
     * @ author: 黄悦
     * @param whichScreen：哪一屏
     */
    private void toScreenBeforeSort(final int whichScreen) 
    {
        CellLayout cellLayout = (CellLayout) getChildAt(mCurrentScreen);
        
        mDragController.sortView(cellLayout, mDragInfo.cellIndex, cellLayout.getChildCount() - 1);
        cellLayout.removeView(mDragInfo.cell);

        handler.postDelayed(new Runnable() {
            public void run() {
                CellLayout cellLayout = (CellLayout) getChildAt(whichScreen);
                boolean isLandscape = Mogoo_GlobalConfig.isLandscape();
                int cellSize = Mogoo_GlobalConfig.getWorkspaceLongAxisCells(isLandscape) * Mogoo_GlobalConfig.getWorkspaceShortAxisCells(isLandscape);
                
             // 当前屏已满
                if (cellLayout.getChildCount() == cellSize) {
                    return;
                }
                if (mDragInfo == null || mDragInfo.cell == null) {
                    return;
                }
                
                if(mDragInfo.cell.getParent() != null){
                    ((ViewGroup)mDragInfo.cell.getParent()).removeView(mDragInfo.cell);
                }
                
                addView(cellLayout, mDragInfo.cell, false);
                int index = cellLayout.getChildCount() - 1;
                setDragInfo(cellLayout, index);

                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) mDragInfo.cell
                        .getLayoutParams();
                lp.cellX = mDragInfo.cellX;
                lp.cellY = mDragInfo.cellY;
                
                LauncherModel.setSortIconInfo(R.id.workspace, whichScreen);
            }
        }, 100);

        cellLayout = null;
    }

    /**
     * 取得拖动图标与某屏所有图标的交集的面积 
     * @author: 张永辉
     * @param dragView 拖动图标
     * @param cellLayout 所在屏幕
     * @return
     */
    /*
     * private int [] getAllCellIntersection(DragView dragView,CellLayout
     * cellLayout){ if(cellLayout==null){ return null ; } int size =
     * cellLayout.getChildCount() ; int [] intersectionAreas = new int [size] ;
     * int statusBarHeight = this.getStatusBarHeight() ; //图标左边与屏幕左边沿的距离 int
     * left = dragView.getScreenLeft() ; //图标上边与workspace上边沿的距离 int top =
     * dragView.getScreenTop()-statusBarHeight ; //图标右边与屏幕在边沿的距离 int right =
     * left+dragView.getWidth() ; //图标下边与workspace上边沿的距离 int bottom =
     * top+dragView.getHeight() ; if(MT_GlobalConfig.LOG_DEBUG){ Log.d(TAG,
     * "left="+left+" top="+top+" right="+right+" bootom="+bottom+" statusBar="+
     * statusBarHeight); } return intersectionAreas ; }
     */

    /**
     * 取得拖动图标与桌面图标的相交面积 
     * @author: 张永辉
     * @param left 被拖动图标左边与屏幕左边沿的距离
     * @param top 被拖动图标上边与workspace上边沿的距离
     * @param right 被拖动图标右边与屏幕在边沿的距离
     * @param bottom 被拖动图标下边与workspace上边沿的距离
     * @param btv 桌面图标
     * @return
     */
    /*
     * private int getIntersectionArea(int left,int top,int right,int
     * bottom,MT_BubbleTextView btv) { //桌面图标可视区域左上角横坐标 int leftTopX =
     * btv.getLeft() - MT_GlobalConfig.getWorkspaceCellPaddingLeft() ;
     * //桌面图标可视区域左上角纵坐标 int leftTopY = btv.getTop() -
     * MT_GlobalConfig.getWorkspaceCellPaddingTop() ; //桌面图标可视区域右下角横坐标 int
     * rightBottomX = leftTopX + MT_GlobalConfig.getIconWidth() ;
     * //桌面图标可视区域右下角纵坐标 int rightBottomY = leftTopY + btv.getHeight() -
     * MT_GlobalConfig
     * .getWorkspaceCellPaddingBottom()-MT_GlobalConfig.getWorkspaceCellPaddingTop
     * (); int workspaceLeft = this.getLeft() ; int workspaceTop = this.getTop()
     * ; if(MT_GlobalConfig.LOG_DEBUG) { Log.d(TAG,
     * "leftTopX="+leftTopX+" leftTopY="
     * +leftTopY+" rightBottomX="+rightBottomX+" rightBottomY"+rightBottomY); }
     * return 0 ; }
     */

    /**
     * 取得状态栏的高度 
     * @author: 张永辉
     * @return 返回状态栏的高度值
     */
    public int getStatusBarHeight() {
    	DragLayer layout = (DragLayer)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.drag_layer, getContext());
        return Mogoo_GlobalConfig.getScreenHeight() - layout.getHeight();
    }

    /**
     * 取得拖动图标相交的第一个单元格的素引号 
     * @author: 张永辉
     * @return
     */
    private int getCellIndexByDragView(DragView dragView) {
        int x = dragView.getScreenLeft();
        int y = dragView.getScreenTop() - this.getStatusBarHeight();
        int width = dragView.getWidth();
        int height = dragView.getHeight();

        // 找不到在哪一个单元格中时，返回-1
        int index = -1;

        if (Mogoo_GlobalConfig.LOG_DEBUG) {
            Log.d(TAG, "getCellIndexByDragView getCurrentScreen = " + getCurrentScreen() + " mCurrentScreen=" + mCurrentScreen);
        }
        
        CellLayout cellLayout = (CellLayout) this.getChildAt(getCurrentScreen());

        // 单元格总数
        int count = cellLayout.getCellCoordinateList() != null ? cellLayout.getCellCoordinateList()
                .size() : 0;

        int longAxisStartPadding = Mogoo_GlobalConfig.getWorkspaceLongAxisStartPadding();
        int shortAxisStartPadding = Mogoo_GlobalConfig.getWorkspaceShortAxisStartPadding();

        if ((x < shortAxisStartPadding && (x + width) <= shortAxisStartPadding)
                || (y < longAxisStartPadding && (y + height) <= longAxisStartPadding)) {
            index = -1;
        } else if (x < shortAxisStartPadding && y < longAxisStartPadding) {
            index = 0;
        } else if (x < shortAxisStartPadding) {
            for (int i = 0; i < count; i++) {
                CellEntry entry = cellLayout.getCellEntry(i);
                if (entry.left == shortAxisStartPadding && y >= entry.top && y <= entry.bottom) {
                    index = i;
                }
            }
        } else if (y < longAxisStartPadding) {
            for (int i = 0; i < count; i++) {
                CellEntry entry = cellLayout.getCellEntry(i);
                if (entry.top == longAxisStartPadding && x >= entry.left && x <= entry.right) {
                    index = i;
                }
            }
        } else {
            for (int i = 0; i < count; i++) {
                CellEntry entry = cellLayout.getCellEntry(i);
                if (x >= entry.left && x <= entry.right && y >= entry.top && y <= entry.bottom) {
                    index = i;
                }
            }
        }

        if (Mogoo_GlobalConfig.LOG_DEBUG) {
            Log.d(TAG, "find DragView(" + x + "," + y + ")  cellIndex :" + index + " count=" + count);
        }

        return index;
    }

    /**
     * 取得指定坐标所在的单元格的素引号 
     * @author: 张永辉
     * @param x
     * @param y
     * @param width
     * @param height
     * @return
     */
    private int getCellIndexByCoordinate(int x, int y) {
        // 找不到在哪一个单元格中时，返回-1
        int index = -1;

        CellLayout cellLayout = (CellLayout) this.getChildAt(getCurrentScreen());

        // 单元格总数
        int count = cellLayout.getCellCoordinateList() != null ? cellLayout.getCellCoordinateList()
                .size() : 0;

        for (int i = 0; i < count; i++) {
            CellEntry entry = cellLayout.getCellEntry(i);
            if (x >= entry.left && x <= entry.right && y >= entry.top && y <= entry.bottom) {
                index = i;
            }
        }

        if (Mogoo_GlobalConfig.LOG_DEBUG) {
            // Log.d(TAG,
            // "find coordinate("+x+","+y+") index :"+index+" count="+count);

        }

        return index;
    }

    public void resetDragInfo() {
    	mDragInfo.cell = null;
    	mDragInfo.cellIndex = -1;
    	mDragInfo.cellX = -1;
    	mDragInfo.cellY = -1;
        mDragInfo.screen = -1;
    }
    
    /**
     * 移动widget 
     * @ author:张永辉 
     *@param x
     *@param y
     *@param xOffset
     *@param yOffset
     */
    private void moveWidget(int x,int y,int xOffset,int yOffset){
        final CellLayout cellLayout = getCurrentDropLayout();
        
//        if(MT_GlobalConfig.LOG_DEBUG){
//        	Log.d(TAG, "moveWidget x="+x+" y="+y+" xOffset="+xOffset+" yOffset="+yOffset) ;
//        }
        
        if (mDragInfo != null) {
            final View cell = mDragInfo.cell;
            int index = mScroller.isFinished() ? mCurrentScreen : mNextScreen;                
            if (index != mDragInfo.screen) {
                final CellLayout originalCellLayout = (CellLayout) getChildAt(mDragInfo.screen);
                originalCellLayout.removeView(cell);
                cellLayout.addView(cell);
            }
            
            if(!moveWidget){
            	mTargetCell = new int[]{mDragInfo.cellX,mDragInfo.cellY} ;
            }else{
            	moveWidget = false ;
            	mTargetCell = estimateDropCell(x - xOffset, y - yOffset,
                        mDragInfo.spanX, mDragInfo.spanY, cell, cellLayout, mTargetCell);
            }
            
            cellLayout.onDropChild(cell, mTargetCell);

            final ItemInfo info = (ItemInfo) cell.getTag();
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) cell.getLayoutParams();
            //坐标转换
            int point[] = Mogoo_Utilities.switchPoint(new int[] {
                    lp.cellX, lp.cellY
            });
            
//            if(MT_GlobalConfig.LOG_DEBUG){
//            	Log.d(TAG, "moveWidget cellX="+point[0]+" cellY="+point[1]) ;
//            }
            
            LauncherModel.moveItemInDatabase(mLauncher, info,
                    LauncherSettings.Favorites.CONTAINER_DESKTOP, index, point[0], point[1]);
        }
    }
    
    /**
     * screen屏及其之后的所有快捷方式屏是否全满屏
     * @ author:张永辉 
     *@param screen
     *@return
     */
    private boolean isFullFromCurrentScreen(int screen){
        int [] shortcutScreen = Mogoo_GlobalConfig.getShortcutScreen() ;
        CellLayout layout = (CellLayout)getChildAt(getCurrentScreen());
        
        if(layout.getChildCount() < Mogoo_GlobalConfig.getWorkspaceCellCounts()){
            return false;
        }else if (isInvisibleChild(layout))
        {
        	return false;
        }
       
             
        for(int i:shortcutScreen)
        {
            if( i>= screen)
            {
            	layout = (CellLayout)getChildAt(i);
                if(layout.getChildCount() < Mogoo_GlobalConfig.getWorkspaceCellCounts())
                {
                    return false ;
                }
            }
        }
        return true ;
    }
    
    
    /**
     * 判断是否有Invisible对象
     */
    private boolean isInvisibleChild(CellLayout layout ) {
        boolean flag = false;
        int count = layout.getChildCount();
        for (int i = 0; i < count; i++) {
            if (layout.getChildAt(i).getVisibility() == INVISIBLE) {
                flag = true;
                break;
            }
        }
        return flag;
    }
    
    /**
     * 
     * 控制当前屏是否抖动
     * @ author: 黄悦
     *@param currentLayout
     *@param start
     */
    public void vibateOperate(final boolean start){
        final CellLayout currentLayout = (CellLayout) getChildAt(getCurrentScreen());
        oprateVibate(start, (ViewGroup)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.dockWorkSpace, getContext()));
        //因为抖动开启有延时，当两个抖动同时申请内存时会造成交错申请时的内存错误导致有的帧不完整
        //所以先申请数量比较少的dock，并对workspace的申请进行延时错开内存开辟时间
        if(!start){
        	oprateVibate(start, currentLayout);
        }else{
	        handler.postDelayed(new Runnable() {
	            public void run() {
	                oprateVibate(start, currentLayout);
	            }
	        }, 500);
        }
    }

    private void oprateVibate(boolean start, ViewGroup viewGroup) {
        int count = viewGroup.getChildCount();
        View child = null;
        for(int i = 0; i < count; i++){
            child = viewGroup.getChildAt(i);
            if(child instanceof Mogoo_BubbleTextView){
                if(start){
                    ((Mogoo_BubbleTextView)child).startVibrate(mIconCache, 0);
                }else{
                    ((Mogoo_BubbleTextView)child).stopVibrate();
                }
            }
        }
    }
    
    /**
     * 当前屏是否满屏
     * @ author: 魏景春
     *@param curScreen
     *@return
     */
    private boolean isFullOfCurrentScreen(int curScreen)
    {
        CellLayout layout = (CellLayout)getChildAt(curScreen);   
        int count = layout.getChildCount();
        if (Mogoo_GlobalConfig.getWorkspaceCellCounts() == count && !isInvisibleChild(layout))
        {
        	return true;
        }else
        {
        	return false;
        }
    }
    /**
     * 重整celllayout 
     * @ author: 张永辉
     *@param cellLayout
     */
    void reLayoutCellLayout(ViewGroup parent)
    {
    	for(int i=0; i < parent.getChildCount();i++)
    	{
          View child = parent.getChildAt(i);
          if(child!=null)
          {
              CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
              int[] cellXY = Mogoo_Utilities.convertToCell(i);
              lp.cellX = cellXY[0];
              lp.cellY = cellXY[1];   
          }
    	}    	
    	
    	parent.requestLayout() ;
    }
    
    /**
     * 根据索引号得到该索引对应的图标类型 
     *@author:张永辉 
     *@param cellLayout
     *@param index
     *@return
     */
    private int getTargetType(CellLayout cellLayout,int index){
        int count = cellLayout.getChildCount() ;
        
        if(index>=count){
            return Mogoo_GlobalConfig.TARGET_NULL ;
        }else{
            View child = cellLayout.getChildAt(index) ;
            if(child instanceof Mogoo_FolderBubbleText && child.getVisibility() == View.VISIBLE){
                return Mogoo_GlobalConfig.TARGET_FOLDER ;
            }
            else if(child instanceof Mogoo_BubbleTextView && child.getVisibility() == View.VISIBLE){
                return Mogoo_GlobalConfig.TARGET_SHORTCUT ;
            }
            else{
                return Mogoo_GlobalConfig.TARGET_NULL ;
            }
        }
    }
    
    /**
     * 是否触发文件夹
     *@author: 张永辉
     *@Date：2011-3-18
     *@param cellLayout
     *@param dragViewX
     *@param dragViewY
     *@param dragWidth
     *@param dragHeight
     *@param index
     *@return
     */
    private boolean triggerFolder(CellLayout cellLayout,int dragViewX,int dragViewY,int dragWidth,int dragHeight,int index){
        CellEntry entry = cellLayout.getCellEntry(index) ;
        int targetType = this.getTargetType(cellLayout, index) ;
//        float cellArea = MT_GlobalConfig.getCellArea()  ;
        float cellArea = Mogoo_GlobalConfig.getIconHeight() * Mogoo_GlobalConfig.getIconWidth();
        int left = entry.left + Mogoo_GlobalConfig.getWorkspaceCellMarginLeft();
        int right = entry.right - Mogoo_GlobalConfig.getWorkspaceCellMarginRight();
        int top = entry.top + Mogoo_GlobalConfig.getWorkspaceCellMarginTop();
        int bottom = entry.top + Mogoo_GlobalConfig.getWorkspaceCellMarginTop() + Mogoo_GlobalConfig.getIconHeight();
        float rate = 0f ;
        float area = 0f ;
        
        //左上角是否在最后一个单元格内
        if(this.getCellIndexByCoordinate(dragViewX, dragViewY)==index){
            area = (right - dragViewX)*(bottom - dragViewY) ;
            rate = area/cellArea;
        }
        //右上角是否在最后一个单元格内
        else if(this.getCellIndexByCoordinate(dragViewX+dragWidth, dragViewY)==index){
            area = (dragViewX - left)*(bottom - dragViewY) ;
            rate = area/cellArea ;
        }
        //左下角是否在最后一个单元格内
        else if(this.getCellIndexByCoordinate(dragViewX, dragViewY+dragHeight)==index){
            area = (right-dragViewX)*(dragViewY-top) ;
            rate = area / cellArea ;
        }
        //右下角是否在最后一个单元格内
        else if(this.getCellIndexByCoordinate(dragViewX+dragWidth, dragViewY+dragHeight)==index){
            area = (dragViewX - left)*(dragViewY - top) ;
            rate = area / cellArea ;
        }
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "triggerFolder area="+area+" rate="+rate) ;
        }
        
        //shortcut
        if(targetType == Mogoo_GlobalConfig.TARGET_SHORTCUT 
                && rate >= Mogoo_GlobalConfig.getFolderGenerateAreaRate()){
            return true ;
        }
        //folder
        else if(targetType == Mogoo_GlobalConfig.TARGET_FOLDER 
                && rate >= Mogoo_GlobalConfig.getFolderAcceptAreaRate()){
            return true ;
        }
        
        return false ;
    }
    
    /**
     * 是否触发文件夹(拖动图标左边或右边在单元格里面)
     *@author: 张永辉
     *@Date：2011-3-18
     *@param cellLayout
     *@param dragViewX
     *@param dragHeight
     *@param index
     *@param isLeft
     *@return
     */
    private boolean triggerFolderInnerH(CellLayout cellLayout,int dragViewX,int dragWidth,int dragHeight,int index,boolean isLeft){
        CellEntry entry = cellLayout.getCellEntry(index) ;
        int targetType = this.getTargetType(cellLayout, index) ;
        float cellArea = Mogoo_GlobalConfig.getCellArea()  ;
        float area = 0f ;
        
        if(isLeft){
           area = (entry.right - dragViewX)*(dragHeight) ;
        }else{
            area = (dragViewX+dragWidth - entry.left)*dragHeight ;
        }
            
        float rate = area / cellArea ;
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "triggerFolderInnerH area="+area+" rate="+rate) ;
        }
        
        //shortcut
        if(targetType == Mogoo_GlobalConfig.TARGET_SHORTCUT 
                && rate >= Mogoo_GlobalConfig.getFolderGenerateAreaRate()){
            return true ;
        }
        //folder
        else if(targetType == Mogoo_GlobalConfig.TARGET_FOLDER 
                && rate >= Mogoo_GlobalConfig.getFolderAcceptAreaRate()){
            return true ;
        }
        
        return false ;
    }
    
    /**
     * 是否触发文件夹(拖动图标左边或下边在单元格里面)
     *@author: 张永辉
     *@Date：2011-3-18
     *@param cellLayout
     *@param dragViewY
     *@param dragWidth
     *@param index
     *@param isTop
     *@return
     */
    private boolean triggerFolderInnerV(CellLayout cellLayout,int dragViewY,int dragWidth,int dragHeight,int index,boolean isTop){
        CellEntry entry = cellLayout.getCellEntry(index) ;
        int targetType = this.getTargetType(cellLayout, index) ;
        float cellArea = Mogoo_GlobalConfig.getCellArea()  ;
        float area = 0f ;
        
        if(isTop){
           area = (entry.bottom-dragViewY)*dragWidth ;
        }else{
            area = (dragViewY+dragHeight-entry.top)*dragWidth ;
        }
            
        float rate = area / cellArea ;
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "triggerFolderInnerV area="+area+" rate="+rate) ;
        }
        
        //shortcut
        if(targetType == Mogoo_GlobalConfig.TARGET_SHORTCUT 
                && rate >= Mogoo_GlobalConfig.getFolderGenerateAreaRate()){
            return true ;
        }
        //folder
        else if(targetType == Mogoo_GlobalConfig.TARGET_FOLDER 
                && rate >= Mogoo_GlobalConfig.getFolderAcceptAreaRate()){
            return true ;
        }
        
        return false ;
    }
    
    /**
     * 是否触发文件夹(拖动图标只有一个顶点在单元格里面)
     *@author: 张永辉
     *@Date：2011-3-18
     *@param cellLayout
     *@param dragViewX
     *@param dragViewY
     *@param dragWidth
     *@param dragHeight
     *@param index
     *@param isLeft
     *@param isTop
     *@return
     */
    private boolean triggerFolder(CellLayout cellLayout,int dragViewX,int dragViewY,int dragWidth,int dragHeight,int index,boolean isLeft,boolean isTop){
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "triggerFolder cell count="+cellLayout.getCellCoordinateList().size()+" index="+index) ;
        }
        
        CellEntry entry = cellLayout.getCellEntry(index) ;
        int targetType = this.getTargetType(cellLayout, index) ;
        float cellArea = Mogoo_GlobalConfig.getCellArea()  ;
        float area = 0f ;
        
        if(isLeft&&isTop){
            area = (entry.right - dragViewX)*(entry.bottom - dragViewY) ;
        }else if(!isLeft&&isTop){
            area = (dragViewX+dragWidth-entry.left)*(entry.bottom - dragViewY) ;
        }else if(isLeft&&!isTop){
            area = (entry.right - dragViewX)*(dragViewY+dragHeight-entry.top) ;
        }else if(!isLeft&&!isTop){
            area = (dragViewX+dragWidth-entry.left)*(dragViewY+dragHeight-entry.top) ;
        }
        
        float rate = area / cellArea ;
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "triggerFolder2 area="+area+" rate="+rate) ;
        }
        
        //shortcut
        if(targetType == Mogoo_GlobalConfig.TARGET_SHORTCUT 
                && rate >= Mogoo_GlobalConfig.getFolderGenerateAreaRate()){
            return true ;
        }
        //folder
        else if(targetType == Mogoo_GlobalConfig.TARGET_FOLDER 
                && rate >= Mogoo_GlobalConfig.getFolderAcceptAreaRate()){
            return true ;
        }
        
        return false ;
    }
    
    /**
     * 是否触发文件夹(拖动图标四个角在指定单元格外面且拖动图标与近定单元格相交)
     *@author: 张永辉
     *@Date：2011-3-18
     *@param cellLayout
     *@param dragViewX
     *@param dragWidth
     *@param index
     *@param isLeft
     *@return
     */
    private boolean triggerFolderOuterH(CellLayout cellLayout,int dragViewX,int dragWidth,int index,boolean isLeft){
        CellEntry entry = cellLayout.getCellEntry(index) ;
        int targetType = this.getTargetType(cellLayout, index) ;
        float cellArea = Mogoo_GlobalConfig.getCellArea()  ;
        float area = 0f ;
        
        if(isLeft){
            area = (entry.right-dragViewX)*Mogoo_GlobalConfig.getWorkspaceCellHeight() ;
        }else{
            area = (dragViewX+dragWidth - entry.left)*Mogoo_GlobalConfig.getWorkspaceCellHeight() ;
        }
        
        float rate = area / cellArea ;
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "triggerFolderOuterH area="+area+" rate="+rate) ;
        }
        
        //shortcut
        if(targetType == Mogoo_GlobalConfig.TARGET_SHORTCUT 
                && rate >= Mogoo_GlobalConfig.getFolderGenerateAreaRate()){
            return true ;
        }
        //folder
        else if(targetType == Mogoo_GlobalConfig.TARGET_FOLDER 
                && rate >= Mogoo_GlobalConfig.getFolderAcceptAreaRate()){
            return true ;
        }
        
        return false ;
    }
    
    /**
     * 是否触发文件夹(拖动图标跨三列时，与中间的单元格相交是否触发)
     *@author: 张永辉
     *@Date：2011-7-21
     *@param cellLayout
     *@param dragViewY
     *@param index
     *@param isTop
     *@return
     */
    private boolean triggerFolderRect(CellLayout cellLayout,int dragViewY,int dragHeight,int index,boolean isTop){
        CellEntry entry = cellLayout.getCellEntry(index) ;
        int targetType = this.getTargetType(cellLayout, index) ;
        float cellArea = Mogoo_GlobalConfig.getCellArea()  ;
        float area = 0f ;
        
        if(isTop){
            area = (entry.bottom-dragViewY)*Mogoo_GlobalConfig.getWorkspaceCellWidth() ;
        }else{
            area = (dragViewY+dragHeight - entry.top)*Mogoo_GlobalConfig.getWorkspaceCellWidth() ;
        }
        
        float rate = area / cellArea ;
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "triggerFolderRect area="+area+" rate="+rate) ;
        }
        
        //shortcut
        if(targetType == Mogoo_GlobalConfig.TARGET_SHORTCUT 
                && rate >= Mogoo_GlobalConfig.getFolderGenerateAreaRate()){
            return true ;
        }
        //folder
        else if(targetType == Mogoo_GlobalConfig.TARGET_FOLDER 
                && rate >= Mogoo_GlobalConfig.getFolderAcceptAreaRate()){
            return true ;
        }
        
        return false ;
    }
    
    /**
     * 目标文件夹是否装満了图标
     *@author: 张永辉
     *@Date：2011-4-2
     *@param index
     *@return
     */
    private boolean targetFolderIsFull(ViewGroup parent,int index)
    {
        int count = Mogoo_GlobalConfig.getWorkspaceLongAxisCells(Mogoo_GlobalConfig.isLandscape())
                    *(Mogoo_GlobalConfig.getWorkspaceShortAxisCells(Mogoo_GlobalConfig.isLandscape())-1) ;
        
        View targetView = parent.getChildAt(index);
        
        if(targetView instanceof Mogoo_FolderBubbleText)
        {
            Mogoo_FolderInfo info = (Mogoo_FolderInfo)targetView.getTag() ;
            
            if(Mogoo_GlobalConfig.LOG_DEBUG)
            {
                System.out.println("-------------size="+info.getContents().size()+"------/////////////////");
            }
            
            if(info.getContents().size()>=count)
            {
                return true ;
            }
            else
            {
                return false ;
            }
        }
         else
        {
             if(Mogoo_GlobalConfig.LOG_DEBUG)
             {
                 System.out.println("-------------no------/////////////////");
             }
             
            return false ;
        }
    }
    // --------------------end----------------------
    //add by 袁业奔 2011-9-7
	//删除
	private boolean isLastCellLayout(int index){
		return (getChildCount()-1)==index;
	}
	private boolean isEmptyCellLayout(int index){
		CellLayout cell=(CellLayout)getChildAt(index);
		return cell.getChildCount()==0;
	}
	//add end
	//add by  袁业奔 2011-9-14
	/**
	 * 加屏
	 */
	private void addCellLayout(){
		LayoutInflater inflater = LayoutInflater.from(mLauncher);
		CellLayout child=(CellLayout)inflater.inflate(R.layout.workspace_screen, null);
		child.setOnClickListener(mLauncher);
		addView(child);
		requestLayout();
	}
    /**
     * 刷新workspace删除空屏，刷新数据库
     *@author: 袁业奔
     *@Date：2011-9-14
     *@return
     */
	public void refreshWorkspace(){
		int count = getChildCount();
		int mCurrent=mCurrentScreen;
		ArrayList<CellLayout> emptyCellLayout = new  ArrayList();
		CellLayout cellLayout = null;
		for (int i = 0; i < count; i++) {
			cellLayout = (CellLayout)getChildAt(i);
			if(cellLayout.getChildCount() == 0){
				if(mCurrent>i){
					mCurrentScreen--;
				}
				emptyCellLayout.add(cellLayout);
			}
		}
		
		cellLayout = null;
//		if(Mogoo_GlobalConfig.LOG_DEBUG){
//			System.out.println("-----mCurrentScreen----"+mCurrentScreen+"-------");
//		}
		mLauncher.setScreen(mCurrentScreen);
		int size = emptyCellLayout.size();
		//add by yeben 2011-10-12 不存在空屏
		if(size==0){
			return;
		}
		
		
		
		for (int i = 0; i < size; i++) {
			if(emptyCellLayout.get(i)!=null){
				if(getChildCount()<=Mogoo_GlobalConfig.getWorkspaceScreenMinCount()){
					break;
				}
				removeView(emptyCellLayout.get(i));
			}
		}
		emptyCellLayout.clear();
//		Mogoo_BitmapUtils.clearIndicatorImages();
		//db
		int newCount = getChildCount();
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SCREEN_COUNT, newCount);
		int[] screenType = Mogoo_GlobalConfig.createWorkspaceScreenType(newCount);
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SCREEN_TYPE,screenType);
		ItemInfo item;
		CellLayout child;
		for (int i = 1; i < newCount; i++) {
			//
			child=(CellLayout) getChildAt(i);
			for (int j = 0; j < child.getChildCount(); j++) {
				item=(ItemInfo)child.getChildAt(j).getTag();
				//screen有变化时才更新数据库
				if(item.screen!=i){
					item.screen=i;
					LauncherModel.updateItemInDatabase(mLauncher, item);
					if(Mogoo_GlobalConfig.LOG_DEBUG){
						System.out.println("------------updateItemInDatabase-------"+j+"-------");
					}
				}
			}			
		}
		setCurrentScreen(mCurrentScreen); 
	}
	//----------end-----------
	
	/**
	 * 手指抬起时的动画
	 * motone zhanmin 12.01.05 add 
	 */
	public void upAnimotion(){
		if (mTouchState == TOUCH_STATE_SCROLLING) {
            final VelocityTracker velocityTracker = mVelocityTracker;
            velocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);
            final int velocityX = (int) velocityTracker.getXVelocity(mActivePointerId);

            final int screenWidth = getWidth();
            final int whichScreen = (getScrollX() + (screenWidth / 2)) / screenWidth;
            final float scrolledPos = (float) getScrollX() / screenWidth;

            if (velocityX > SNAP_VELOCITY && mCurrentScreen > 0) {
                // Fling hard enough to move left.
                // Don't fling across more than one screen at a time.
                final int bound = scrolledPos < whichScreen ? mCurrentScreen - 1
                        : mCurrentScreen;
                snapToScreen(Math.min(whichScreen, bound), velocityX, true);
            } else if (velocityX < -SNAP_VELOCITY && mCurrentScreen < getChildCount() - 1) {
                // Fling hard enough to move right
                // Don't fling across more than one screen at a time.
                final int bound = scrolledPos > whichScreen ? mCurrentScreen + 1
                        : mCurrentScreen;
                 int screen = Math.max(whichScreen, bound);
                 int sreachNum = Mogoo_GlobalConfig.getSearchScreen();
                if(mCurrentScreen == sreachNum && screen == 1){
                		getBackground().setAlpha(0);
                		invalidate();
                   }
                snapToScreen(screen, velocityX, true);
            } else {
                snapToScreen(whichScreen, 0, true);
            }
            
        }
	}
}