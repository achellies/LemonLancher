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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;
import com.mogoo.launcher2.utils.Mogoo_DragHelper;

/**
 * Class for initiating a drag within a view or across multiple views.
 */
public class DragController {
    @SuppressWarnings({"UnusedDeclaration"})
    private static final String TAG = "Launcher.DragController";

    /** Indicates the drag is a move.  */
    public static int DRAG_ACTION_MOVE = 0;

    /** Indicates the drag is a copy.  */
    public static int DRAG_ACTION_COPY = 1;

    private static final int SCROLL_DELAY = 450;
    private static final int SCROLL_ZONE = 20;
    private static final int VIBRATE_DURATION = 35;

    private static final boolean PROFILE_DRAWING_DURING_DRAG = false;

    private static final int SCROLL_OUTSIDE_ZONE = 0;
    private static final int SCROLL_WAITING_IN_ZONE = 1;

    private static final int SCROLL_LEFT = 0;
    private static final int SCROLL_RIGHT = 1;

    private Context mContext;
    private Handler mHandler;
    //private final Vibrator mVibrator = new Vibrator();
    private Vibrator mVibrator;

    // temporaries to avoid gc thrash
    private Rect mRectTemp = new Rect();
    private final int[] mCoordinatesTemp = new int[2];

    /** Whether or not we're dragging. */
    private boolean mDragging;    

    /** X coordinate of the down event. */
    private float mMotionDownX;

    /** Y coordinate of the down event. */
    private float mMotionDownY;

    /** Info about the screen for clamping. */
    private DisplayMetrics mDisplayMetrics = new DisplayMetrics();

    /** Original view that is being dragged.  */
    private View mOriginator;

    /** X offset from the upper-left corner of the cell to where we touched.  */
    private float mTouchOffsetX;

    /** Y offset from the upper-left corner of the cell to where we touched.  */
    private float mTouchOffsetY;

    /** Where the drag originated */
    private DragSource mDragSource;

    /** The data associated with the object being dragged */
    private Object mDragInfo;

    /** The view that moves around while you drag.  */
    private DragView mDragView;

    /** Who can receive drop events */
    private ArrayList<DropTarget> mDropTargets = new ArrayList<DropTarget>();

    private DragListener mListener;

    /** The window token used as the parent for the DragView. */
    private IBinder mWindowToken;

    /** The view that will be scrolled when dragging to the left and right edges of the screen. */
    private View mScrollView;

    private View mMoveTarget;

    private DragScroller mDragScroller;
    private int mScrollState = SCROLL_OUTSIDE_ZONE;
    private ScrollRunnable mScrollRunnable = new ScrollRunnable();

    private RectF mDeleteRegion;
    private DropTarget mLastDropTarget;

    private InputMethodManager mInputMethodManager;
    
    //--------------------motone field statement--------- 
    
    private DropTarget currentDropTarget;		//当前释放到目标对象
    
    public Mogoo_DragHelper dragHelper;
    
    private boolean isQuickDrag = false;

    private MotionEvent mCurrentDownEvent;
    private int mTouchSlopSquare;
    
    //add by huangyue for lock drag event
    public static boolean dragLocked = false;
    //end
    
    //是否执行了移动操作
    private boolean mDragMoving = false;    
    //---------------------end---------------------------
    /**
     * Interface to receive notifications when a drag starts or stops
     */
    interface DragListener {
        
        /**
         * A drag has begun
         * 
         * @param source An object representing where the drag originated
         * @param info The data associated with the object that is being dragged
         * @param dragAction The drag action: either {@link DragController#DRAG_ACTION_MOVE}
         *        or {@link DragController#DRAG_ACTION_COPY}
         */
        void onDragStart(DragSource source, Object info, int dragAction);
        
        /**
         * The drag has eneded
         */
        void onDragEnd();
    }
    
    /**
     * Used to create a new DragLayer from XML.
     *
     * @param context The application's context.
     */
    public DragController(Context context) 
    {
        mContext = context;
        mHandler = new Handler();
        dragHelper = new Mogoo_DragHelper(); 
        mTouchSlopSquare = 20 * 20;
        
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    /**
     * Starts a drag.
     * 
     * @param v The view that is being dragged
     * @param source An object representing where the drag originated
     * @param dragInfo The data associated with the object that is being dragged
     * @param dragAction The drag action: either {@link #DRAG_ACTION_MOVE} or
     *        {@link #DRAG_ACTION_COPY}
     */
    public void startDrag(View v, DragSource source, Object dragInfo, int dragAction) {
        mOriginator = v;

        Bitmap b = getViewBitmap(v);

        if (b == null) {
            // out of memory?
            return;
        }

        int[] loc = mCoordinatesTemp;
        v.getLocationOnScreen(loc);
        int screenX = loc[0];
        int screenY = loc[1];

        startDrag(b, screenX, screenY, 0, 0, b.getWidth(), b.getHeight(),
                source, dragInfo, dragAction);

        b.recycle();      

        if (dragAction == DRAG_ACTION_MOVE) {
            v.setVisibility(View.INVISIBLE);
        }
    }
    
    /**
     * Starts a drag.
     * 
     * @param cellInfo 单元格信息 
     * @param v The view that is being dragged
     * @param source An object representing where the drag originated
     * @param dragInfo The data associated with the object that is being dragged
     * @param dragAction The drag action: either {@link #DRAG_ACTION_MOVE} or
     *        {@link #DRAG_ACTION_COPY}
     */
    public void startDrag2(CellLayout.CellInfo cellInfo, View v, DragSource source, Object dragInfo, int dragAction) {
        mOriginator = v;

        Bitmap b = getViewBitmap(v);

        if (b == null) {
            // out of memory?
            return;
        }

        int[] loc = mCoordinatesTemp;
        v.getLocationOnScreen(loc);
        int screenX = loc[0];
        int screenY = loc[1];

        startDrag(b, screenX, screenY, 0, 0, b.getWidth(), b.getHeight(),
                source, dragInfo, dragAction);

        b.recycle();
        
        //------------add by 魏景春 2011-2-18----------
        // 设置拖动图标的属性 
        if (mDragView !=null && cellInfo !=null)
        {
        	mDragView.setStartIndex(cellInfo.cellIndex);
        }        
        //----------------end---------------------

        if (dragAction == DRAG_ACTION_MOVE) {
            v.setVisibility(View.INVISIBLE);
        }
        
        //--------add by 魏景春--2011-2-24--------
        //启动拖动后，立即执行拖动目标到onEnter事件
        final int[] coordinatesTemp = mCoordinatesTemp;
        DropTarget dropTargetTemp = findDropTarget(screenX, screenY, coordinatesTemp);       
        if (dropTargetTemp != null && mLastDropTarget == null) 
        {
        	dropTargetTemp.onDragEnter(mDragSource, coordinatesTemp[0], coordinatesTemp[1], (int) mTouchOffsetX, (int) mTouchOffsetY, mDragView, mDragInfo);
        }
        
        mLastDropTarget = dropTargetTemp;
        //-------------end----------------------------
    }

    /**
     * Starts a drag.
     * 
     * @param b The bitmap to display as the drag image.  It will be re-scaled to the
     *          enlarged size.
     * @param screenX The x position on screen of the left-top of the bitmap.
     * @param screenY The y position on screen of the left-top of the bitmap.
     * @param textureLeft The left edge of the region inside b to use.
     * @param textureTop The top edge of the region inside b to use.
     * @param textureWidth The width of the region inside b to use.
     * @param textureHeight The height of the region inside b to use.
     * @param source An object representing where the drag originated
     * @param dragInfo The data associated with the object that is being dragged
     * @param dragAction The drag action: either {@link #DRAG_ACTION_MOVE} or
     *        {@link #DRAG_ACTION_COPY}
     */
    public void startDrag(Bitmap b, int screenX, int screenY,
            int textureLeft, int textureTop, int textureWidth, int textureHeight,
            DragSource source, Object dragInfo, int dragAction) {
        if (PROFILE_DRAWING_DURING_DRAG) {
            android.os.Debug.startMethodTracing("Launcher");
        }

        // Hide soft keyboard, if visible
        if (mInputMethodManager == null) {
            mInputMethodManager = (InputMethodManager)
                    mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        }
        mInputMethodManager.hideSoftInputFromWindow(mWindowToken, 0);

        if (mListener != null) {
            mListener.onDragStart(source, dragInfo, dragAction);
        }

        int registrationX = ((int)mMotionDownX) - screenX;
        int registrationY = ((int)mMotionDownY) - screenY;

        mTouchOffsetX = mMotionDownX - screenX;
        mTouchOffsetY = mMotionDownY - screenY;
        
        mDragging = true;
        mDragSource = source;
        mDragInfo = dragInfo;

//        mVibrator.vibrate(VIBRATE_DURATION);

        DragView dragView = mDragView = new DragView(mContext, b, registrationX, registrationY,
                textureLeft, textureTop, textureWidth, textureHeight);
        dragView.show(mWindowToken, (int)mMotionDownX, (int)mMotionDownY);
    }

    /**
     * Draw the view into a bitmap.
     */
    private Bitmap getViewBitmap(View v) {
        //-- add by 黄悦 2011-1-25
        //-- 返回无倾角图标
        Bitmap bitmap = null;
        if(v instanceof Mogoo_BubbleTextView){
            Mogoo_BubbleTextView temp = (Mogoo_BubbleTextView)v;
            Bitmap tempBitmap = temp.getZeroAngleFrame();
            if(tempBitmap != null){
                return tempBitmap.copy(Config.ARGB_8888, false);
            }
        }
        //-- end
        
        v.clearFocus();
        v.setPressed(false);

        boolean willNotCache = v.willNotCacheDrawing();
        v.setWillNotCacheDrawing(false);

        // Reset the drawing cache background color to fully transparent
        // for the duration of this operation
        int color = v.getDrawingCacheBackgroundColor();
        v.setDrawingCacheBackgroundColor(0);

        if (color != 0) {
            v.destroyDrawingCache();
        }
        
        if(v instanceof Mogoo_BubbleTextView){
            ((Mogoo_BubbleTextView)v).stopVibrate();
            ((Mogoo_BubbleTextView)v).setReflection(false);
        }
        
        v.buildDrawingCache();
        Bitmap cacheBitmap = v.getDrawingCache();
        if (cacheBitmap == null) {
            Log.e(TAG, "failed getViewBitmap(" + v + ")", new RuntimeException());
            return null;
        }
        
        if(v instanceof Mogoo_BubbleTextView){
            Bitmap delIcon = ((Mogoo_BubbleTextView)v).getDelIcon();
            if(delIcon != null){
                Canvas tempCanvas = new Canvas(cacheBitmap);
                tempCanvas.drawBitmap(delIcon, 0, 0, null);
                tempCanvas = null;
            }
        }
        
        bitmap = Bitmap.createBitmap(cacheBitmap);

        // Restore the view
        v.destroyDrawingCache();
        v.setWillNotCacheDrawing(willNotCache);
        v.setDrawingCacheBackgroundColor(color);
        if(v instanceof Mogoo_BubbleTextView){
            ((Mogoo_BubbleTextView)v).setReflection(true);
        }

        return bitmap;
    }

    /**
     * Call this from a drag source view like this:
     *
     * <pre>
     *  @Override
     *  public boolean dispatchKeyEvent(KeyEvent event) {
     *      return mDragController.dispatchKeyEvent(this, event)
     *              || super.dispatchKeyEvent(event);
     * </pre>
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public boolean dispatchKeyEvent(KeyEvent event) {
        return mDragging;
    }

    /**
     * Stop dragging without dropping.
     */
    public void cancelDrag() {
        endDrag();
    }

    private void endDrag() {
    	 // add by 魏景春 2011-2-11
        isQuickDrag = false;
        mDragMoving = false;
        currentDropTarget = null;
        mLastDropTarget = null;
        
        if (mDragging) {
            mDragging = false;
            if (mOriginator != null) {
                mOriginator.setVisibility(View.VISIBLE);
            }
            if (mListener != null) {
                mListener.onDragEnd();
            }
            if (mDragView != null) {
                mDragView.remove();
                mDragView = null;
            }           
        }
    }

    /**
     * Call this from a drag source view.
     */
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        
//        if(Mogoo_GlobalConfig.LOG_DEBUG)
//        {
            Log.d(TAG, "--------------------call onInterceptTouchEvent()--------------------------------------"); 
//        }
        
        
        final int action = ev.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            recordScreenSize();
        }

        final int screenX = clamp((int)ev.getRawX(), 0, mDisplayMetrics.widthPixels);
        final int screenY = clamp((int)ev.getRawY(), 0, mDisplayMetrics.heightPixels);

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                break;

            case MotionEvent.ACTION_DOWN:
                // Remember location of down touch
                mMotionDownX = screenX;
                mMotionDownY = screenY;
                mLastDropTarget = null;
                
                //------------add by weijingchun
                final int[] coordinatesTemp = mCoordinatesTemp;
                DropTarget dropTargetTemp = findDropTarget(screenX, screenY, coordinatesTemp);
                this.setCurrentDropTarget(dropTargetTemp);
                
                if (mCurrentDownEvent != null) {
                    mCurrentDownEvent.recycle();
                }
                mCurrentDownEvent = MotionEvent.obtain(ev);
                
                //-------------end ---------------
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
            	// if (Mogoo_GlobalConfig.LOG_DEBUG) {
                     Log.d(TAG, TAG + " onInterceptTouchEvent ACTION_UP: mDragging= " + mDragging);
               //  }
            	if (mDragging) {
                    drop(screenX, screenY);
                }
                endDrag();
                break;
        }
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "--------------------end--------------------------------------"); 
        }

        return mDragging;
    }

    /**
     * Sets the view that should handle move events.
     */
    void setMoveTarget(View view) {
        mMoveTarget = view;
    }    
    
    public boolean dispatchUnhandledMove(View focused, int direction) {
        return mMoveTarget != null && mMoveTarget.dispatchUnhandledMove(focused, direction);
    }

    /**
     * Call this from a drag source view.
     */
    public boolean onTouchEvent(MotionEvent ev) {
        
//        if(Mogoo_GlobalConfig.LOG_DEBUG)
//        {
            Log.d(TAG, "--------------------call onTouchEvent()--------------------------------------"); 
//        }
        
        View scrollView = mScrollView;

        if (!mDragging) {
            return false;
        }

        final int action = ev.getAction();
        final int screenX = clamp((int)ev.getRawX(), 0, mDisplayMetrics.widthPixels);
        final int screenY = clamp((int)ev.getRawY(), 0, mDisplayMetrics.heightPixels);

        switch (action) {
        case MotionEvent.ACTION_DOWN:
            // Remember where the motion event started
            mMotionDownX = screenX;
            mMotionDownY = screenY;

            if ((screenX < SCROLL_ZONE) || (screenX > scrollView.getWidth() - SCROLL_ZONE)) {
                mScrollState = SCROLL_WAITING_IN_ZONE;
                mHandler.postDelayed(mScrollRunnable, SCROLL_DELAY);
            } else {
                mScrollState = SCROLL_OUTSIDE_ZONE;
            }
            

            
            break;
        case MotionEvent.ACTION_MOVE:
            // Update the drag view.  Don't use the clamped pos here so the dragging looks
            // like it goes off screen a little, intead of bumping up against the edge.
        	 if (mDragging) {
        		  mDragView.move((int)ev.getRawX(), (int)ev.getRawY());
        	 }
            mDragMoving = true;
            //--------add by 魏景春-----------------------
            //判断是否快速拖动         
            if(isQuickDrag(ev,mCurrentDownEvent))
            {
            	return false;
            }
            
            //----------------end------------------------                    
            // Drop on someone?
            final int[] coordinates = mCoordinatesTemp;
            DropTarget dropTarget = findDropTarget(screenX, screenY, coordinates);
            
            if(Mogoo_GlobalConfig.LOG_DEBUG)
            {
                Log.d(TAG, "--------------------find drop target="+dropTarget+"---------------------------------"); 
            }
            
            //如果是从文件夹中拖出来，则使workspace和dock可视，并重新找目标
            if (dropTarget ==null && mLastDropTarget !=null && mLastDropTarget instanceof Mogoo_FolderWorkspace)
            {
                final Mogoo_ComponentBus bus = Mogoo_ComponentBus.getInstance();
                bus.getActivityComp(R.id.workspace, mContext).setVisibility(View.VISIBLE);
                bus.getActivityComp(R.id.dockWorkSpace, mContext).setVisibility(View.VISIBLE);
                dropTarget = findDropTarget(screenX, screenY, coordinates);
                
                if(dropTarget != null){
                    mLastDropTarget.onDropTargetChange(mDragSource, dropTarget, mDragView, mDragInfo);
                }
            }
            
            //如果拖动的图标从workspace区域移动到非DropTarget目标，则将dropTarget设置为Workspace
            if (dropTarget ==null && mLastDropTarget !=null && mLastDropTarget instanceof Workspace)
            {
            	dropTarget = mLastDropTarget;
            }
            
            //如果是从文件夹中拖出来，且拖向DOCK工具栏中时，将拖放目标改为workspace
//            if(dropTarget instanceof MT_DockWorkSpace && mLastDropTarget !=null && mLastDropTarget instanceof MT_FolderWorkspace)
//            {
//                final MT_ComponentBus bus = MT_ComponentBus.getInstance();
//                Workspace workspace = (Workspace)bus.getActivityComp(R.id.workspace, mContext);
//                workspace.setVisibility(View.VISIBLE) ;
//                dropTarget = workspace  ;
//            }
            
            if (dropTarget != null) {
                if (mLastDropTarget == dropTarget) {
                    dropTarget.onDragOver(mDragSource, coordinates[0], coordinates[1],
                        (int) mTouchOffsetX, (int) mTouchOffsetY, mDragView, mDragInfo);
                } else {
                    if (mLastDropTarget != null) {
                        mLastDropTarget.onDragExit(mDragSource, coordinates[0], coordinates[1],
                            (int) mTouchOffsetX, (int) mTouchOffsetY, mDragView, mDragInfo);
                    }
                    dropTarget.onDragEnter(mDragSource, coordinates[0], coordinates[1],
                        (int) mTouchOffsetX, (int) mTouchOffsetY, mDragView, mDragInfo);
                }
            } else {
                if (mLastDropTarget != null) {
                    mLastDropTarget.onDragExit(mDragSource, coordinates[0], coordinates[1],
                        (int) mTouchOffsetX, (int) mTouchOffsetY, mDragView, mDragInfo);
                }
            }
            mLastDropTarget = dropTarget;
            
            //设置当前的dropTarget
            setCurrentDropTarget(dropTarget);

            // Scroll, maybe, but not if we're in the delete region.
            boolean inDeleteRegion = false;
            if (mDeleteRegion != null) {
                inDeleteRegion = mDeleteRegion.contains(screenX, screenY);
            }
            
            //---- add by huangyue
            
            
            //在此设定dock 部分不能进行切屏
            if (!inDeleteRegion && screenX < SCROLL_ZONE && screenY < Mogoo_GlobalConfig.getScreenHeight() - Mogoo_GlobalConfig.getDockHeight()) {
                if (mScrollState == SCROLL_OUTSIDE_ZONE) {
                    mScrollState = SCROLL_WAITING_IN_ZONE;
                    mScrollRunnable.setDirection(SCROLL_LEFT);
                    mHandler.postDelayed(mScrollRunnable, SCROLL_DELAY);
                }
            } else if (!inDeleteRegion && screenX > scrollView.getWidth() - SCROLL_ZONE && screenY < Mogoo_GlobalConfig.getScreenHeight() - Mogoo_GlobalConfig.getDockHeight()) {
                if (mScrollState == SCROLL_OUTSIDE_ZONE) {
                    mScrollState = SCROLL_WAITING_IN_ZONE;
                    mScrollRunnable.setDirection(SCROLL_RIGHT);
                    mHandler.postDelayed(mScrollRunnable, SCROLL_DELAY);
                }
            } else {
                if (mScrollState == SCROLL_WAITING_IN_ZONE) {
                    mScrollState = SCROLL_OUTSIDE_ZONE;
                    mScrollRunnable.setDirection(SCROLL_RIGHT);
                    mHandler.removeCallbacks(mScrollRunnable);
                }
            }
            
            // ---end 

            break;
        case MotionEvent.ACTION_UP:
            mHandler.removeCallbacks(mScrollRunnable);
           // if (Mogoo_GlobalConfig.LOG_DEBUG) {
                Log.d(TAG, TAG + " onTouchEvent ACTION_UP: mDragging= " + mDragging);
           // }
            if (mDragging) {
                drop(screenX, screenY);
            }
            endDrag();

            break;
        case MotionEvent.ACTION_CANCEL:
            cancelDrag();
        }
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "--------------------end--------------------------------------"); 
        }

        return true;
    }

    private boolean drop(float x, float y) 
    {
    	Log.d(TAG, "-------------drop-----------------");
    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------start drop()--------------------------------------"); 
        }
    	
        final int[] coordinates = mCoordinatesTemp;
        //----------------add by 魏景春 2011-2-23-------------
              	
        if (!isQuickDrag && mDragMoving)
        {
            if (Mogoo_GlobalConfig.LOG_DEBUG) {
                Log.d(TAG, TAG + " 1: isQuickDrag = false  mDragMoving = ture");
            }
        //--------------------end-------------------------
        	DropTarget dropTarget = findDropTarget((int) x, (int) y, coordinates);
        	
        	if (Mogoo_GlobalConfig.LOG_DEBUG) {
                Log.d(TAG, TAG + " dropTarget="+dropTarget+" mLastDropTarget="+mLastDropTarget);
            }
        	
        	//如果是从文件夹中拖出来，则使workspace和dock可视，并重新找目标
            if (dropTarget ==null && mLastDropTarget !=null && mLastDropTarget instanceof Mogoo_FolderWorkspace)
            {
                final Mogoo_ComponentBus bus = Mogoo_ComponentBus.getInstance();
                bus.getActivityComp(R.id.workspace, mContext).setVisibility(View.VISIBLE);
                dropTarget = findDropTarget((int) x, (int) y, coordinates);
            }
        	
        	//如果拖动的图标从workspace区域移动到非DropTarget目标，则将dropTarget设置为Workspace
            if (dropTarget ==null && mLastDropTarget !=null && mLastDropTarget instanceof Workspace)
            {
            	dropTarget = mLastDropTarget;
            }
            
            //如果是从文件夹中拖出来，且拖向DOCK工具栏中时，将拖放目标改为workspace
//            if(dropTarget instanceof MT_DockWorkSpace && mLastDropTarget !=null && mLastDropTarget instanceof MT_FolderWorkspace)
//            {
//                final MT_ComponentBus bus = MT_ComponentBus.getInstance();
//                Workspace workspace = (Workspace)bus.getActivityComp(R.id.workspace, mContext);
//                workspace.setVisibility(View.VISIBLE) ;
//                dropTarget = workspace  ;
//            }
        	
            if (dropTarget != null) {
                dropTarget.onDragExit(mDragSource, coordinates[0], coordinates[1],
                        (int) mTouchOffsetX, (int) mTouchOffsetY, mDragView, mDragInfo);
                if (dropTarget.acceptDrop(mDragSource, coordinates[0], coordinates[1],
                        (int) mTouchOffsetX, (int) mTouchOffsetY, mDragView, mDragInfo)) {
                    dropTarget.onDrop(mDragSource, coordinates[0], coordinates[1],
                            (int) mTouchOffsetX, (int) mTouchOffsetY, mDragView, mDragInfo);
                    mDragSource.onDropCompleted((View) dropTarget, true);
                    return true;
                } else {
                	//---add by 魏景春 2011-2-24 -----
                	//如果目标对象不接收拖入到图标,则将拖动到图标恢复到源拖动区域
                	mDragSource.onRestoreDragIcon(mDragInfo);
                	//-------end--------------
                	
                    mDragSource.onDropCompleted((View) dropTarget, false);
                    return true;
                }
            }
            //add by  张永辉
            else
            {
                //放到workspace中当前屏开始的第一个空位置
                if(mLastDropTarget==null)
                {
                    dropToBlackCellFromCurrentScreen();
                }
            }
          //end
        }
        else
        {
        	if (mLastDropTarget==null)
        	{
                if (Mogoo_GlobalConfig.LOG_DEBUG) {
                    Log.d(TAG, TAG + " 2: mLastDropTarget==null");
                }
                //放到workspace中当前屏开始的第一个空位置
                dropToBlackCellFromCurrentScreen() ;
        		return false;
        	}
        	else
        	{
                if (Mogoo_GlobalConfig.LOG_DEBUG) {
                    Log.d(TAG, TAG + " 3:  mLastDropTarget = " + mLastDropTarget.toString());
                }
                
        		mLastDropTarget.onDragExit(mDragSource, coordinates[0], coordinates[1],
                        (int) mTouchOffsetX, (int) mTouchOffsetY, mDragView, mDragInfo);
                if (mLastDropTarget.acceptDrop(mDragSource, coordinates[0], coordinates[1],
                        (int) mTouchOffsetX, (int) mTouchOffsetY, mDragView, mDragInfo)) {
                	mLastDropTarget.onDrop(mDragSource, coordinates[0], coordinates[1],
                            (int) mTouchOffsetX, (int) mTouchOffsetY, mDragView, mDragInfo);
                    mDragSource.onDropCompleted((View) mLastDropTarget, true);
                    return true;
                } else if(!mLastDropTarget.equals(mDragSource)){
                	//---add by 魏景春 2011-2-24 -----
                	//如果目标对象不接收拖入到图标,则将拖动到图标恢复到源拖动区域
                	mDragSource.onRestoreDragIcon(mDragInfo);
                	//-------end--------------
                    mDragSource.onDropCompleted((View) mLastDropTarget, false);
                    return true;
                } else {
                	mDragSource.onDropCompleted((View) mLastDropTarget, false);
                	return true;
                }
        	}
        }
        
    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------------- end --------------------------------------"); 
        }
    	
        return false;
    }
    
    /**
     * 放到workspace中当前屏开始的第一个空位置
     *@author: 张永辉
     *@Date：2011-4-12
     */
    private void dropToBlackCellFromCurrentScreen()
    {
        final Mogoo_ComponentBus bus = Mogoo_ComponentBus.getInstance();
        Workspace workspace = (Workspace)bus.getActivityComp(R.id.workspace, mContext) ;
        Mogoo_DockWorkSpace dockWorkSpace = (Mogoo_DockWorkSpace)bus.getActivityComp(R.id.dockWorkSpace, mContext);
        int [] shortcutScreen = Mogoo_GlobalConfig.getShortcutScreen() ;
        for(int i:shortcutScreen)
        {
            if(i<workspace.getCurrentScreen())
            {
                continue ;
            }
            
            int [] cellXY = CellLayout.findBlackCell(mContext, i) ;
            if(cellXY != null)
            {
                CellLayout cellLayout = (CellLayout)workspace.getChildAt(i) ;
                View view = workspace.createIcon(mDragInfo) ;
                workspace.addView(cellLayout, view, false) ;
                LauncherModel.setSortIconInfo(R.id.workspace, i) ;
                LauncherModel.saveAllIconInfo(mContext) ;
                if(view instanceof Mogoo_BubbleTextView)
                {
                    
                    ((Mogoo_BubbleTextView)view).startVibrate(((LauncherApplication)mContext.getApplicationContext()).getIconCache(), 0) ;
                }
                break ;
            }
        }
        dockWorkSpace.setLocaleDrag(false) ;
    }

    private DropTarget findDropTarget(int x, int y, int[] dropCoordinates) {
        final Rect r = mRectTemp;

        final ArrayList<DropTarget> dropTargets = mDropTargets;
        final int count = dropTargets.size();
        for (int i=count-1; i>=0; i--) {
            final DropTarget target = dropTargets.get(i);
            target.getHitRect(r);
            target.getLocationOnScreen(dropCoordinates);
            r.offset(dropCoordinates[0] - target.getLeft(), dropCoordinates[1] - target.getTop());
            if (r.contains(x, y) && ((View)target).getVisibility() == View.VISIBLE) {
                dropCoordinates[0] = x - dropCoordinates[0];
                dropCoordinates[1] = y - dropCoordinates[1];
                return target;
            }
        }
        return null;
    }

    /**
     * Get the screen size so we can clamp events to the screen size so even if
     * you drag off the edge of the screen, we find something.
     */
    private void recordScreenSize() {
        ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getMetrics(mDisplayMetrics);
    }

    /**
     * Clamp val to be &gt;= min and &lt; max.
     */
    private static int clamp(int val, int min, int max) {
        if (val < min) {
            return min;
        } else if (val >= max) {
            return max - 1;
        } else {
            return val;
        }
    }

    public void setDragScoller(DragScroller scroller) {
        mDragScroller = scroller;
    }

    public void setWindowToken(IBinder token) {
        mWindowToken = token;
    }

    /**
     * Sets the drag listner which will be notified when a drag starts or ends.
     */
    public void setDragListener(DragListener l) {
        mListener = l;
    }

    /**
     * Remove a previously installed drag listener.
     */
    public void removeDragListener(DragListener l) {
        mListener = null;
    }

    /**
     * Add a DropTarget to the list of potential places to receive drop events.
     */
    public void addDropTarget(DropTarget target) {
        mDropTargets.add(target);
    }

    /**
     * Don't send drop events to <em>target</em> any more.
     */
    public void removeDropTarget(DropTarget target) {
        mDropTargets.remove(target);
    }

    /**
     * Set which view scrolls for touch events near the edge of the screen.
     */
    public void setScrollView(View v) {
        mScrollView = v;
    }

    /**
     * Specifies the delete region.  We won't scroll on touch events over the delete region.
     *
     * @param region The rectangle in screen coordinates of the delete region.
     */
    void setDeleteRegion(RectF region) {
        mDeleteRegion = region;
    }

    private class ScrollRunnable implements Runnable {
        private int mDirection;

        ScrollRunnable() {
        }

        public void run() {
            if (mDragScroller != null && !Mogoo_VibrationController.isLoading) {
                if (mDirection == SCROLL_LEFT) {
                    mDragScroller.scrollLeft();
                } else {
                    mDragScroller.scrollRight();
                }
                mScrollState = SCROLL_OUTSIDE_ZONE;
            }
        }

        void setDirection(int direction) {
            mDirection = direction;
        }
    }    
    
    //--------------------motone method statement--------- 
   
    /**
     * 排序及动画播放
     * @ author: 黄悦
     * 
     */
    public boolean sortView(ViewGroup parent, int startIndex, int endIndex){
    	
    	if (Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, parent.toString()+ ": call sortView(): startIndex = " + startIndex + " endIndex =" + endIndex);
        }
    	
    	if(parent != null && startIndex != endIndex && Mogoo_VibrationController.isVibrate)
    	{
    		return dragHelper.sortView(parent, startIndex, endIndex);
    	}
    	
    	return false;
    }
    
    public void sortWithoutAnimation(ViewGroup parent, int startIndex, int endIndex){
    	if (Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, parent.toString()+ ": call sortWithoutAnimation(): startIndex = " + startIndex + " endIndex =" + endIndex);
        }
        
        if(parent != null && startIndex != endIndex && Mogoo_VibrationController.isVibrate)
        {
            dragHelper.sort(parent, startIndex, endIndex);
        }
    }
    
    /**
     * 
     * 排序及动画播放
     * @ author: 黄悦
     * 
     */
    public View moveView(ViewGroup parent, int startIndex, int endIndex, int moveIndex){
        if(parent != null && startIndex <= endIndex)
        {
            return dragHelper.moveView(parent, startIndex, endIndex, moveIndex);
        }
        
        return null;
    }
    
    /**
     * 恢复源区域中拖动到图标
     * @ author: 魏景春
     *@param dragInfo 拖动图标信息
     */
    public void restoreDragSourceDragIcon(Object dragInfo)
    {
    	if (mDragSource !=null)
    	{
    		mDragSource.onRestoreDragIcon(dragInfo);
    	}
    }
    
    private void setCurrentDropTarget(DropTarget dropTarget)
    {    	
    	if (currentDropTarget == null)
    	{
    		currentDropTarget = dropTarget;
    	}else if (!currentDropTarget.equals(dropTarget))
    	{
    		currentDropTarget.onDropTargetChange(mDragSource, dropTarget, mDragView, mDragInfo);
    		currentDropTarget = dropTarget;
    	}
    	
    }

    private boolean isQuickDrag(MotionEvent ev1, MotionEvent ev2)
    {
      final int deltaX = (int) (ev1.getX() - ev2.getX());
      final int deltaY = (int) (ev1.getY() - ev2.getY());
      int distance = (deltaX * deltaX) + (deltaY * deltaY);
      if (distance > mTouchSlopSquare) 
      {
      	this.isQuickDrag = true;      	
      }else
      {
      	this.isQuickDrag = false;      	
      }
      
      if (mCurrentDownEvent != null) {
          mCurrentDownEvent.recycle();
      }
      mCurrentDownEvent = MotionEvent.obtain(ev1);
      
      if(Mogoo_GlobalConfig.LOG_DEBUG)
      {
			Log.d(TAG, "isQuickDrag = " + isQuickDrag);
		}
    	return isQuickDrag;
    }
    
    public void setMove(boolean  Moving ) {
    	mDragging = Moving;
    }
    //---------------------end---------------------------
}
