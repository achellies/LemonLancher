/**  
 * 文 件 名:  TaskWorkspace.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：                       
 * 版    本:  1.0  
 * 创建时间:   2011-6-15
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-6-15        张永辉       1.0          1.0 Version  
 */  

package com.mogoo.launcher2.taskManager.ui;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.widget.Scroller;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.CellLayout;
import com.mogoo.launcher2.DragSource;
import com.mogoo.launcher2.DragView;
import com.mogoo.launcher2.DropTarget;
import com.mogoo.launcher2.Folder;
import com.mogoo.launcher2.ItemInfo;
import com.mogoo.launcher2.Launcher;
import com.mogoo.launcher2.LauncherApplication;
import com.mogoo.launcher2.LauncherModel;
import com.mogoo.launcher2.LauncherSettings;
import com.mogoo.launcher2.Mogoo_BubbleTextView;
import com.mogoo.launcher2.ShortcutInfo;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.taskManager.Mogoo_TaskManager;
import com.mogoo.launcher2.utils.Mogoo_BitmapCache;
import com.mogoo.launcher2.utils.Mogoo_Utilities;
import com.mogoo.launcher2.utils.Mogoo_WorkspaceInface;

import java.util.ArrayList;

public class Mogoo_TaskWorkspace extends ViewGroup implements Mogoo_WorkspaceInface{
    private static final String TAG = "Launcher.Mogoo_TaskWorkspace";

    private static final int INVALID_SCREEN = -1;

    /**
     * The velocity at which a fling gesture will cause us to snap to the next
     * screen
     */
    private static final int SNAP_VELOCITY = 600;

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
    private Mogoo_TaskCellLayout.CellInfo mDragInfo = new Mogoo_TaskCellLayout.CellInfo();;

    /**
     * Target drop ara calculated during last acceptDrop call.
     */
    private int[] mTargetCell = null;

    private float mLastMotionX;

    private float mLastMotionY;

    private final static int TOUCH_STATE_REST = 0;

    private final static int TOUCH_STATE_SCROLLING = 1;

    private int mTouchState = TOUCH_STATE_REST;

    private OnLongClickListener mLongClickListener;

    private Mogoo_TaskManager mTaskManager;

    private Mogoo_BitmapCache mIconCache;

//    private DragController mDragController;

    /**
     * Cache of vacant cells, used during drag events and invalidated as needed.
     */
    private Mogoo_TaskCellLayout.CellInfo mVacantCache = null;

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

    private static final float SMOOTHING_SPEED = 0.75f;

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

    // ---------------------end---------------------------

    private static class WorkspaceOvershootInterpolator implements Interpolator {
        private static final float DEFAULT_TENSION = 1.3f;

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
    public Mogoo_TaskWorkspace(Context context, AttributeSet attrs) {
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
    public Mogoo_TaskWorkspace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        mWallpaperManager = WallpaperManager.getInstance(context);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Workspace, defStyle, 0);
        mDefaultScreen = a.getInt(R.styleable.Workspace_defaultScreen, 0);
        a.recycle();

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
//        Launcher.setScreen(mCurrentScreen);
        LauncherApplication app = (LauncherApplication) context.getApplicationContext();
        mIconCache = app.getIconCache();

        final ViewConfiguration configuration = ViewConfiguration.get(getContext());
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

        // 初始化全局参数－屏幕总数和默认显示屏
//        MT_GlobalConfig.setConfigParm(MT_GlobalConfig.WORKSPACE_DEFAULT_SCREEN, mDefaultScreen);

        // 设定workspace背景色
        setBackgroundColor(Color.BLACK);
        getBackground().setAlpha(0);
    }
    

    @Override
    public void addView(View child, int index, LayoutParams params) {
        if (!(child instanceof Mogoo_TaskCellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, index, params);
    }

    @Override
    public void addView(View child) {
        if (!(child instanceof Mogoo_TaskCellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (!(child instanceof Mogoo_TaskCellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, index);
    }

    @Override
    public void addView(View child, int width, int height) {
        if (!(child instanceof Mogoo_TaskCellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, width, height);
    }

    @Override
    public void addView(View child, LayoutParams params) {
        if (!(child instanceof Mogoo_TaskCellLayout)) {
            throw new IllegalArgumentException("A Workspace can only have CellLayout children.");
        }
        super.addView(child, params);
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
    public void setCurrentScreen(int currentScreen) {
        if (!mScroller.isFinished())
            mScroller.abortAnimation();
        clearVacantCache();
        mCurrentScreen = Math.max(0, Math.min(currentScreen, getChildCount() - 1));
        scrollTo(mCurrentScreen * getWidth(), 0);
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
    public void addInScreen(View child, int screen, int x, int y, int spanX, int spanY, boolean insert) {
        if (screen < 0 || screen >= getChildCount()) {
            Log.e(TAG, "The screen must be >= 0 and < " + getChildCount() + " (was " + screen
                    + "); skipping child");
            return;
        }

        clearVacantCache();

        final Mogoo_TaskCellLayout group = (Mogoo_TaskCellLayout) getChildAt(screen);
        Mogoo_TaskCellLayout.LayoutParams lp = (Mogoo_TaskCellLayout.LayoutParams) child.getLayoutParams();
        if (lp == null) {
            lp = new Mogoo_TaskCellLayout.LayoutParams(x, y, spanX, spanY);
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
    }
    
    public void addInScreen(View child, Mogoo_TaskCellLayout group, int x, int y, int spanX, int spanY, boolean insert) {
        if (group==null) {
            return;
        }

        clearVacantCache();

        Mogoo_TaskCellLayout.LayoutParams lp = (Mogoo_TaskCellLayout.LayoutParams) child.getLayoutParams();
        if (lp == null) {
            lp = new Mogoo_TaskCellLayout.LayoutParams(x, y, spanX, spanY);
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
    }

    Mogoo_TaskCellLayout.CellInfo findAllVacantCells(boolean[] occupied) {
        Mogoo_TaskCellLayout group = (Mogoo_TaskCellLayout) getChildAt(mCurrentScreen);
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
            super.scrollTo(mScroller.getCurrX(), getScrollY());
            mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
            super.scrollTo(getScrollX(), mScroller.getCurrY());
            postInvalidate();
        } else if (mNextScreen != INVALID_SCREEN) {
            mCurrentScreen = Math.max(0, Math.min(mNextScreen, getChildCount() - 1));
            // mPreviousIndicator.setLevel(mCurrentScreen);
            // mNextIndicator.setLevel(mCurrentScreen);
//            Launcher.setScreen(mCurrentScreen);
            mNextScreen = INVALID_SCREEN;
            clearChildrenCache();
        } else if (mTouchState == TOUCH_STATE_SCROLLING) {
            final float now = System.nanoTime() / NANOTIME_DIV;
            final float e = (float) Math.exp((now - mSmoothingTime) / SMOOTHING_CONSTANT);
            final float dx = mTouchX - getScrollX();
            super.scrollTo((int)(getScrollX() + dx * e), getScrollY());
            mSmoothingTime = now;
            

            // Keep generating points as long as we're more than 1px away from
            // the target
            if (dx > 1.f || dx < -1.f) {
                postInvalidate();
            }
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
        	//解决出现null指针问题 add by liangss 2012-3-23
        	if(getChildAt(mCurrentScreen) != null) {
        		drawChild(canvas, getChildAt(mCurrentScreen), getDrawingTime());
        	}
        	//end
        } else {
            final long drawingTime = getDrawingTime();
            final float scrollPos = (float) getScrollX() / getWidth();
            final int leftScreen = (int) scrollPos;
            final int rightScreen = leftScreen + 1;
            if (leftScreen >= 0) {
                drawChild(canvas, getChildAt(leftScreen), drawingTime);
            }
            if (scrollPos != leftScreen && rightScreen < getChildCount()) {
                drawChild(canvas, getChildAt(rightScreen), drawingTime);
            }
        }

        if (restore) {
            canvas.restoreToCount(restoreCount);
        }
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        computeScroll();
//        mDragController.setWindowToken(getWindowToken());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "*******************onMeasure()*****************") ;
        }

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
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "*******************onLayout()*****************") ;
        }
        
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
//            if (!mTaskManager.isWorkspaceLocked()) {
                snapToScreen(screen);
//            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
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
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
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
            final Mogoo_TaskCellLayout layout = (Mogoo_TaskCellLayout) getChildAt(i);
            layout.setChildrenDrawnWithCacheEnabled(true);
            layout.setChildrenDrawingCacheEnabled(true);
        }
    }

    void clearChildrenCache() {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final Mogoo_TaskCellLayout layout = (Mogoo_TaskCellLayout) getChildAt(i);
            layout.setChildrenDrawnWithCacheEnabled(false);
        }
    }
    
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "*******************onInterceptTouchEvent() *****************") ;
        }
//        final boolean workspaceLocked = mLauncher.isWorkspaceLocked();
//        final boolean allAppsVisible = mLauncher.isAllAppsVisible();
//        if (workspaceLocked || allAppsVisible) {
//            return false; // We don't want the events. Let them fall through to
//                          // the all apps view.
//        }

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
                handTouchState(ev) ;
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
                    final Mogoo_TaskCellLayout currentScreen = (Mogoo_TaskCellLayout) getChildAt(mCurrentScreen);
                    if(currentScreen!=null){
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

        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "*******************onInterceptTouchEvent() end*****************") ;
        }
        /*
         * The only time we want to intercept motion events is if we are in the
         * drag mode.
         */
        return mTouchState != TOUCH_STATE_REST;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "*******************onTouchEvent()*****************") ;
        }
        
        /**
         * 如果任务管理器中的图标没有加载完，不响应触摸事件
         */
        if(!mTaskManager.isFinishLoad()) return true ;
        

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
//                Log.d(TAG, "mTouchState="+mTouchState) ;
                if (mTouchState == TOUCH_STATE_SCROLLING) {
                    // Scroll to follow the motion event
                    final int pointerIndex = ev.findPointerIndex(mActivePointerId);
                    final float x = ev.getX(pointerIndex);
                    final float deltaX = mLastMotionX - x;
                    mLastMotionX = x;               	
//                	//add by 袁业奔 锁定第一屏音乐 2011-10-9
                	if(switchFilter(deltaX)){
                		break;
                	}
                	//end
                    if (deltaX < 0) {
                        if (mTouchX > 0) {
                            mTouchX += Math.max(-mTouchX, deltaX);
                            mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
                            invalidate();
                        }
                    } else if (deltaX > 0) {
                        final float availableToScroll = getChildAt(getChildCount() - 1).getRight()
                                - mTouchX - getWidth();
                        if (availableToScroll > 0) {
                            mTouchX += Math.min(availableToScroll, deltaX);
                            mSmoothingTime = System.nanoTime() / NANOTIME_DIV;
                            invalidate();
                        }
                    } else {
                        awakenScrollBars();
                    }
                }else{
                    handTouchState(ev);
                }
                break;
            case MotionEvent.ACTION_UP:
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
                        snapToScreen(Math.max(whichScreen, bound), velocityX, true);
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
                mTouchState = TOUCH_STATE_REST;
                mActivePointerId = INVALID_POINTER;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                onSecondaryPointerUp(ev);
                break;
        }
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "*******************onTouchEvent() end*****************") ;
        }
        return true;
    }
    
    /**
     * 
     *@author: 张永辉
     *@Date：2011-6-17
     *@param ev
     */
    private void handTouchState(MotionEvent ev){
        final int pointerIndex = ev.findPointerIndex(mActivePointerId);
        final float x = ev.getX(pointerIndex);
        final float y = ev.getY(pointerIndex);
        final int xDiff = (int) Math.abs(x - mLastMotionX);
        final int yDiff = (int) Math.abs(y - mLastMotionY);

        final int touchSlop = mTouchSlop;
//        Log.d(TAG, "xDiff="+xDiff+" touchSlop="+touchSlop+" mLastMotionX="+mLastMotionX+" x="+x) ;
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
    
    }

    void snapToScreen(int whichScreen) {
        snapToScreen(whichScreen, 0, false);
    }

    void snapToScreen(int whichScreen, int velocity, boolean settle) {
        // if (!mScroller.isFinished()) return;
        
        whichScreen = Math.max(0, Math.min(whichScreen, getChildCount() - 1));
        //add by 袁业奔 2011-10-9 锁定0屏 
        if(Mogoo_GlobalConfig.isLockMusicPanel()){
            if(whichScreen == 0){
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
        if (velocity > 0) {
            duration += (duration / (velocity / BASELINE_FLING_VELOCITY))
                    * FLING_VELOCITY_INFLUENCE;
        } else {
            duration += 100;
        }

        awakenScrollBars(duration);
        mScroller.startScroll(getScrollX(), 0, delta, 0, duration);
        //add by yeben 2011-10-29
		handler.postDelayed(new Runnable() {
			public void run() {
				mTaskManager.getVibrationController().excuteWorkspaceOtherFlag(
						Mogoo_TaskWorkspace.this);
			}
		}, 100);
		//end
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
//            Launcher.setScreen(mCurrentScreen);
        }
    }

    void addApplicationShortcut(ShortcutInfo info, Mogoo_TaskCellLayout.CellInfo cellInfo) {
        addApplicationShortcut(info, cellInfo, false);
    }

    void addApplicationShortcut(ShortcutInfo info, Mogoo_TaskCellLayout.CellInfo cellInfo,
            boolean insertAtFirst) {
        final Mogoo_TaskCellLayout layout = (Mogoo_TaskCellLayout) getChildAt(cellInfo.screen);
        final int[] result = new int[2];

        layout.cellToPoint(cellInfo.cellX, cellInfo.cellY, result);
//        View view = createIcon(info); 
        //addView(layout, view, result[0], result[1], insertAtFirst);
    }

    
    private void removeLastScreenView() {
        CellLayout lastCellLayout = (CellLayout) getChildAt(mDragInfo.screen);
//        mDragController.sortWithoutAnimation(lastCellLayout, mDragInfo.cellIndex,
//                lastCellLayout.getChildCount() - 1);
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
        
        for(int i = currentScreen + 1; i < childSize - 1; i++){
            if(((ViewGroup)getChildAt(i)).getChildCount() < cellSize){
                return i;
            }
        }
        
        return -1;
    }


    void addView(Mogoo_TaskCellLayout parent, View view,boolean insertAtFirst)
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


    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) {
        clearVacantCache();
    }

    /**
     * Return the current {@link CellLayout}, correctly picking the destination
     * screen while a scroll is in progress.
     */
    private Mogoo_TaskCellLayout getCurrentDropLayout() {
        int index = mScroller.isFinished() ? mCurrentScreen : mNextScreen;
        return (Mogoo_TaskCellLayout) getChildAt(index);
    }
    
    
    /**
     * {@inheritDoc}
     */
    public Rect estimateDropLocation(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo, Rect recycle) {
        final Mogoo_TaskCellLayout layout = getCurrentDropLayout();

        final Mogoo_TaskCellLayout.CellInfo cellInfo = mDragInfo;
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
            Mogoo_TaskCellLayout layout, int[] recycle) {
        // Create vacant cell cache if none exists
        if (mVacantCache == null) {
            mVacantCache = layout.findAllVacantCells(null, ignoreView);
        }

        // Find the best target drop location
        return layout.findNearestVacantArea(pixelX, pixelY, spanX, spanY, mVacantCache, recycle);
    }

    public void setTaskManager(Mogoo_TaskManager taskManager) {
        mTaskManager = taskManager;
    }

//    public void setDragController(DragController dragController) {
//        mDragController = dragController;
//    }


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
            if (mCurrentScreen < getChildCount() - 1)
                snapToScreen(mCurrentScreen + 1);
        } else {
            if (mNextScreen < getChildCount() - 1)
                snapToScreen(mNextScreen + 1);
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
//                    if (f.getInfo() == tag) {
//                        return f;
//                    }
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


    void moveToDefaultScreen(boolean animate) {
        if (animate) {
            snapToScreen(mDefaultScreen);
        } else {
            setCurrentScreen(mDefaultScreen);
        }
        getChildAt(mDefaultScreen).requestFocus();
    }


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
            LauncherModel.moveItemInDatabase(mTaskManager, info,
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
        mTaskManager = null;
        mIconCache = null;
//        mDragController = null;
        mLongClickListener = null;
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
     * 从父视图中移除某子视图 
     *@author:张永辉 
     *@param layout 父视图
     *@param child 子视图
     */
    private void removeChild(CellLayout layout,View child){
        layout.removeViewInLayout(child);
        if (child instanceof DropTarget) {
//            mDragController.removeDropTarget((DropTarget) child);
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

    

    /**
     * 取得状态栏的高度 
     * @author: 张永辉
     * @return 返回状态栏的高度值
     */
    int getStatusBarHeight() {
        return Mogoo_GlobalConfig.getScreenHeight() - this.getHeight();
    }

    

    private void resetDragInfo() {
        mDragInfo.cell = null;
        mDragInfo.cellIndex = -1;
        mDragInfo.cellX = -1;
        mDragInfo.cellY = -1;
        mDragInfo.screen = -1;
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
    public void reLayoutCellLayout(ViewGroup parent)
    {
        for(int i=0; i < parent.getChildCount();i++)
        {
          View child = parent.getChildAt(i);
          if(child!=null)
          {
              Mogoo_TaskCellLayout.LayoutParams lp = (Mogoo_TaskCellLayout.LayoutParams) child.getLayoutParams();
              int[] cellXY = Mogoo_Utilities.convertToCell(i);
              lp.cellX = cellXY[0];
              lp.cellY = cellXY[1]; 
          }
        }       
        
        parent.requestLayout() ;
    }
    
    /**
     * 重整所有的cellLayout
     *@author: 张永辉
     *@Date：2011-6-17
     */
    public void reLayoutAllCellLayout(){
        int childCount = this.getChildCount() ;
        for(int i=0;i<childCount;i++){
            Mogoo_TaskCellLayout cellLayout = (Mogoo_TaskCellLayout)getChildAt(i) ;
            reLayoutCellLayout(cellLayout) ;
        }
    }
    
    
    // --------------------end----------------------
//    /**
//     * 屏幕切换管制 返回true时表示不能切换到目标屏 
//     */
    private boolean switchFilter(float deltaX) {
        boolean switchToSearch = false;
        // 能否切换到另一屏控制
        if (deltaX < 0 && mCurrentScreen ==1 && Mogoo_GlobalConfig.isLockMusicPanel()) {
                switchToSearch = true;
        }
        return switchToSearch;
    }
}
