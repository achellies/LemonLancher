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

import com.mogoo.launcher.R;
import com.mogoo.launcher2.CellEntry.CellEntryInface;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;

import android.app.WallpaperManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewDebug;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.util.ArrayList;
import java.util.List;

public class CellLayout extends ViewGroup implements CellEntryInface{
    
    private final static String TAG = "Launcher.CellLayout" ;
    
    private boolean mPortrait;

    private int mCellWidth;
    private int mCellHeight;
    
    private int mLongAxisStartPadding;
    private int mLongAxisEndPadding;

    private int mShortAxisStartPadding;
    private int mShortAxisEndPadding;

    private int mShortAxisCells;
    private int mLongAxisCells;

    private int mWidthGap;
    private int mHeightGap;

    private final Rect mRect = new Rect();
    private final CellInfo mCellInfo = new CellInfo();
    
    int[] mCellXY = new int[2];
    boolean[][] mOccupied;

    private RectF mDragRect = new RectF();

    private boolean mDirtyTag;
    private boolean mLastDownOnOccupiedCell = false;
    
    private final WallpaperManager mWallpaperManager;     
    
    private int workspaceCellBottom = -1;

    
    //--------------------motone field statement---------   
    //单元格坐标列表
    private ArrayList<CellEntry> cellCoordinateList = new ArrayList<CellEntry>();
    
    //---------------------end---------------------------    
    
    public CellLayout(Context context) {
        this(context, null);
    }

    public CellLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CellLayout, defStyle, 0);

        mCellWidth = a.getDimensionPixelSize(R.styleable.CellLayout_cellWidth, 10);
        mCellHeight = a.getDimensionPixelSize(R.styleable.CellLayout_cellHeight, 10);
        
        mLongAxisStartPadding = 
            a.getDimensionPixelSize(R.styleable.CellLayout_longAxisStartPadding, 10);
        mLongAxisEndPadding = 
            a.getDimensionPixelSize(R.styleable.CellLayout_longAxisEndPadding, 10);
        mShortAxisStartPadding =
            a.getDimensionPixelSize(R.styleable.CellLayout_shortAxisStartPadding, 10);
        mShortAxisEndPadding = 
            a.getDimensionPixelSize(R.styleable.CellLayout_shortAxisEndPadding, 10);
        
        mShortAxisCells = a.getInt(R.styleable.CellLayout_shortAxisCells, 4);
        mLongAxisCells = a.getInt(R.styleable.CellLayout_longAxisCells, 4);
        
        //add by 张永辉 2011-1-20 初始化桌面行列数、桌面单元格高和宽、屏幕的四边与单元格的间距
//        MT_GlobalConfig.setConfigParm(MT_GlobalConfig.WORKSPACE_SHORT_AXIS_CELLS , mShortAxisCells) ;
//        MT_GlobalConfig.setConfigParm(MT_GlobalConfig.WORKSPACE_LONG_AXIS_CELLS , mLongAxisCells) ;
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_CELL_WIDTH , mCellWidth) ;
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_CELL_HEIGHT , mCellHeight) ;
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SHORT_AXIS_START_PADDING , mShortAxisStartPadding) ;
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_LONG_AXIS_START_PADDING , mLongAxisStartPadding) ;
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SHORT_AXIS_END_PADDING , mShortAxisEndPadding) ;
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_LONG_AXIS_END_PADDING, mLongAxisEndPadding) ;
        //end 

        a.recycle();

        setAlwaysDrawnWithCacheEnabled(false);

        if (mOccupied == null) {
            if (mPortrait) {
                mOccupied = new boolean[mShortAxisCells][mLongAxisCells];
            } else {
                mOccupied = new boolean[mLongAxisCells][mShortAxisCells];
            }
        }
        
        mWallpaperManager = WallpaperManager.getInstance(getContext());
        
        
    }

    @Override
    public void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        // Cancel long press for all children
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.cancelLongPress();
        }
        
    }

    int getCountX() {
        return mPortrait ? mShortAxisCells : mLongAxisCells;
    }

    int getCountY() {
        return mPortrait ? mLongAxisCells : mShortAxisCells;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        // Generate an id for each view, this assumes we have at most 256x256 cells
        // per workspace screen
        final LayoutParams cellParams = (LayoutParams) params;
        cellParams.regenerateId = true;

        super.addView(child, index, params);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (child != null) {
            Rect r = new Rect();
            child.getDrawingRect(r);
            requestRectangleOnScreen(r);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mCellInfo.screen = ((ViewGroup) getParent()).indexOfChild(this);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        final CellInfo cellInfo = mCellInfo;
        
        //----add by 魏景春 2011-1-28
        //如果当前是searchScreen屏，禁止禁止执行该事件
        Mogoo_ComponentBus componentBus = Mogoo_ComponentBus.getInstance();
        Workspace ws = (Workspace)componentBus.getActivityComp(R.id.workspace, getContext());
        if (Mogoo_GlobalConfig.isSearchScreen(ws.getCurrentScreen()))
        {
        	  return false;
        }
        //-----end--------------

        if (action == MotionEvent.ACTION_DOWN) {
            final Rect frame = mRect;
            final int x = (int) ev.getX() + getScrollX();
            final int y = (int) ev.getY() + getScrollY();
            final int count = getChildCount();

            boolean found = false;
            for (int i = count - 1; i >= 0; i--) {
                final View child = getChildAt(i);

                if ((child.getVisibility()) == VISIBLE || child.getAnimation() != null) {
                    child.getHitRect(frame);
                    if (frame.contains(x, y)) {
                        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                        cellInfo.cell = child;
                        cellInfo.cellX = lp.cellX;
                        cellInfo.cellY = lp.cellY;
                        cellInfo.spanX = lp.cellHSpan;
                        cellInfo.spanY = lp.cellVSpan;
                        cellInfo.cellIndex = getIndexByCellXY(cellInfo.cellX, cellInfo.cellY);
                        cellInfo.valid = true;
                        found = true;
                        mDirtyTag = false;
                        break;
                    }
                }
            }
            
            mLastDownOnOccupiedCell = found;

            if (!found) {
                int cellXY[] = mCellXY;
                pointToCellExact(x, y, cellXY);

                final boolean portrait = mPortrait;
                final int xCount = portrait ? mShortAxisCells : mLongAxisCells;
                final int yCount = portrait ? mLongAxisCells : mShortAxisCells;

                final boolean[][] occupied = mOccupied;
                findOccupiedCells(xCount, yCount, occupied, null);

                cellInfo.cell = null;
                cellInfo.cellX = cellXY[0];
                cellInfo.cellY = cellXY[1];
                cellInfo.spanX = 1;
                cellInfo.spanY = 1;
                cellInfo.valid = cellXY[0] >= 0 && cellXY[1] >= 0 && cellXY[0] < xCount &&
                        cellXY[1] < yCount && !occupied[cellXY[0]][cellXY[1]];

                // Instead of finding the interesting vacant cells here, wait until a
                // caller invokes getTag() to retrieve the result. Finding the vacant
                // cells is a bit expensive and can generate many new objects, it's
                // therefore better to defer it until we know we actually need it.

                mDirtyTag = true;
            }
            setTag(cellInfo);
        } else if (action == MotionEvent.ACTION_UP) {
            cellInfo.cell = null;
            cellInfo.cellX = -1;
            cellInfo.cellY = -1;
            cellInfo.spanX = 0;
            cellInfo.spanY = 0;
            cellInfo.valid = false;
            mDirtyTag = false;
            setTag(cellInfo);
        }

        return false;
    }

    @Override
    public CellInfo getTag() {
        final CellInfo info = (CellInfo) super.getTag();
        if (mDirtyTag && info.valid) {
            final boolean portrait = mPortrait;
            final int xCount = portrait ? mShortAxisCells : mLongAxisCells;
            final int yCount = portrait ? mLongAxisCells : mShortAxisCells;

            final boolean[][] occupied = mOccupied;
            findOccupiedCells(xCount, yCount, occupied, null);

            findIntersectingVacantCells(info, info.cellX, info.cellY, xCount, yCount, occupied);

            mDirtyTag = false;
        }
        return info;
    }

    private static void findIntersectingVacantCells(CellInfo cellInfo, int x, int y,
            int xCount, int yCount, boolean[][] occupied) {

        cellInfo.maxVacantSpanX = Integer.MIN_VALUE;
        cellInfo.maxVacantSpanXSpanY = Integer.MIN_VALUE;
        cellInfo.maxVacantSpanY = Integer.MIN_VALUE;
        cellInfo.maxVacantSpanYSpanX = Integer.MIN_VALUE;
        cellInfo.clearVacantCells();

        if (occupied[x][y]) {
            return;
        }

        cellInfo.current.set(x, y, x, y);

        findVacantCell(cellInfo.current, xCount, yCount, occupied, cellInfo);
    }

    private static void findVacantCell(Rect current, int xCount, int yCount, boolean[][] occupied,
            CellInfo cellInfo) {

        addVacantCell(current, cellInfo);

        if (current.left > 0) {
            if (isColumnEmpty(current.left - 1, current.top, current.bottom, occupied)) {
                current.left--;
                findVacantCell(current, xCount, yCount, occupied, cellInfo);
                current.left++;
            }
        }

        if (current.right < xCount - 1) {
            if (isColumnEmpty(current.right + 1, current.top, current.bottom, occupied)) {
                current.right++;
                findVacantCell(current, xCount, yCount, occupied, cellInfo);
                current.right--;
            }
        }

        if (current.top > 0) {
            if (isRowEmpty(current.top - 1, current.left, current.right, occupied)) {
                current.top--;
                findVacantCell(current, xCount, yCount, occupied, cellInfo);
                current.top++;
            }
        }

        if (current.bottom < yCount - 1) {
            if (isRowEmpty(current.bottom + 1, current.left, current.right, occupied)) {
                current.bottom++;
                findVacantCell(current, xCount, yCount, occupied, cellInfo);
                current.bottom--;
            }
        }
    }

    private static void addVacantCell(Rect current, CellInfo cellInfo) {
        CellInfo.VacantCell cell = CellInfo.VacantCell.acquire();
        cell.cellX = current.left;
        cell.cellY = current.top;
        cell.spanX = current.right - current.left + 1;
        cell.spanY = current.bottom - current.top + 1;
        if (cell.spanX > cellInfo.maxVacantSpanX) {
            cellInfo.maxVacantSpanX = cell.spanX;
            cellInfo.maxVacantSpanXSpanY = cell.spanY;
        }
        if (cell.spanY > cellInfo.maxVacantSpanY) {
            cellInfo.maxVacantSpanY = cell.spanY;
            cellInfo.maxVacantSpanYSpanX = cell.spanX;
        }
        cellInfo.vacantCells.add(cell);
    }

    private static boolean isColumnEmpty(int x, int top, int bottom, boolean[][] occupied) {
        for (int y = top; y <= bottom; y++) {
            if (occupied[x][y]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isRowEmpty(int y, int left, int right, boolean[][] occupied) {
        for (int x = left; x <= right; x++) {
            if (occupied[x][y]) {
                return false;
            }
        }
        return true;
    }

    CellInfo findAllVacantCells(boolean[] occupiedCells, View ignoreView) {
        final boolean portrait = mPortrait;
        final int xCount = portrait ? mShortAxisCells : mLongAxisCells;
        final int yCount = portrait ? mLongAxisCells : mShortAxisCells;

        boolean[][] occupied = mOccupied;

        if (occupiedCells != null) {
            for (int y = 0; y < yCount; y++) {
                for (int x = 0; x < xCount; x++) {
                    occupied[x][y] = occupiedCells[y * xCount + x];
                }
            }
        } else {
            findOccupiedCells(xCount, yCount, occupied, ignoreView);
        }

        CellInfo cellInfo = new CellInfo();

        cellInfo.cellX = -1;
        cellInfo.cellY = -1;
        cellInfo.spanY = 0;
        cellInfo.spanX = 0;
        cellInfo.maxVacantSpanX = Integer.MIN_VALUE;
        cellInfo.maxVacantSpanXSpanY = Integer.MIN_VALUE;
        cellInfo.maxVacantSpanY = Integer.MIN_VALUE;
        cellInfo.maxVacantSpanYSpanX = Integer.MIN_VALUE;
        cellInfo.screen = mCellInfo.screen;

        Rect current = cellInfo.current;

        for (int x = 0; x < xCount; x++) {
            for (int y = 0; y < yCount; y++) {
                if (!occupied[x][y]) {
                    current.set(x, y, x, y);
                    findVacantCell(current, xCount, yCount, occupied, cellInfo);
                    occupied[x][y] = true;
                }
            }
        }

        cellInfo.valid = cellInfo.vacantCells.size() > 0;

        // Assume the caller will perform their own cell searching, otherwise we
        // risk causing an unnecessary rebuild after findCellForSpan()
        
        return cellInfo;
    }

    /**
     * Given a point, return the cell that strictly encloses that point 
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    void pointToCellExact(int x, int y, int[] result) {
        final boolean portrait = mPortrait;
        
        final int hStartPadding = portrait ? mShortAxisStartPadding : mLongAxisStartPadding;
        final int vStartPadding = portrait ? mLongAxisStartPadding : mShortAxisStartPadding;

        result[0] = (x - hStartPadding) / (mCellWidth + mWidthGap);
        result[1] = (y - vStartPadding) / (mCellHeight + mHeightGap);

        final int xAxis = portrait ? mShortAxisCells : mLongAxisCells;
        final int yAxis = portrait ? mLongAxisCells : mShortAxisCells;

        if (result[0] < 0) result[0] = 0;
        if (result[0] >= xAxis) result[0] = xAxis - 1;
        if (result[1] < 0) result[1] = 0;
        if (result[1] >= yAxis) result[1] = yAxis - 1;
    }
    
    /**
     * Given a point, return the cell that most closely encloses that point
     * @param x X coordinate of the point
     * @param y Y coordinate of the point
     * @param result Array of 2 ints to hold the x and y coordinate of the cell
     */
    void pointToCellRounded(int x, int y, int[] result) {
        pointToCellExact(x + (mCellWidth / 2), y + (mCellHeight / 2), result);
    }

    /**
     * Given a cell coordinate, return the point that represents the upper left corner of that cell
     * 
     * @param cellX X coordinate of the cell 
     * @param cellY Y coordinate of the cell
     * 
     * @param result Array of 2 ints to hold the x and y coordinate of the point
     */
    void cellToPoint(int cellX, int cellY, int[] result) {
        final boolean portrait = mPortrait;
        
        final int hStartPadding = portrait ? mShortAxisStartPadding : mLongAxisStartPadding;
        final int vStartPadding = portrait ? mLongAxisStartPadding : mShortAxisStartPadding;


        result[0] = hStartPadding + cellX * (mCellWidth + mWidthGap);
        result[1] = vStartPadding + cellY * (mCellHeight + mHeightGap);
    }

    int getCellWidth() {
        return mCellWidth;
    }

    int getCellHeight() {
        return mCellHeight;
    }

    int getLeftPadding() {
        return mPortrait ? mShortAxisStartPadding : mLongAxisStartPadding;
    }

    int getTopPadding() {
        return mPortrait ? mLongAxisStartPadding : mShortAxisStartPadding;        
    }

    int getRightPadding() {
        return mPortrait ? mShortAxisEndPadding : mLongAxisEndPadding;
    }

    int getBottomPadding() {
        return mPortrait ? mLongAxisEndPadding : mShortAxisEndPadding;        
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO: currently ignoring padding
        
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);
        
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);
        
        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }
        
        final int shortAxisCells = mShortAxisCells;
        final int longAxisCells = mLongAxisCells;
        final int longAxisStartPadding = mLongAxisStartPadding;
        final int longAxisEndPadding = mLongAxisEndPadding;
        final int shortAxisStartPadding = mShortAxisStartPadding;
        final int shortAxisEndPadding = mShortAxisEndPadding;
        final int cellWidth = mCellWidth;
        final int cellHeight = mCellHeight;

        mPortrait = heightSpecSize > widthSpecSize;

        int numShortGaps = shortAxisCells - 1;
        int numLongGaps = longAxisCells - 1;

        if (mPortrait) {
            int vSpaceLeft = heightSpecSize - longAxisStartPadding - longAxisEndPadding
                    - (cellHeight * longAxisCells);
            mHeightGap = vSpaceLeft / numLongGaps;

            int hSpaceLeft = widthSpecSize - shortAxisStartPadding - shortAxisEndPadding
                    - (cellWidth * shortAxisCells);
            
            if (numShortGaps > 0) {
                mWidthGap = hSpaceLeft / numShortGaps;
            } else {
                mWidthGap = 0;
            }
        } else {
            int hSpaceLeft = widthSpecSize - longAxisStartPadding - longAxisEndPadding
                    - (cellWidth * longAxisCells);
            mWidthGap = hSpaceLeft / numLongGaps;
            
            int vSpaceLeft = heightSpecSize - shortAxisStartPadding - shortAxisEndPadding
                    - (cellHeight * shortAxisCells);
            if (numShortGaps > 0) {
                mHeightGap = vSpaceLeft / numShortGaps;
            } else {
                mHeightGap = 0;
            }
        }
        
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            if (mPortrait) {
                lp.setup(cellWidth, cellHeight, mWidthGap, mHeightGap, shortAxisStartPadding,
                        longAxisStartPadding);
            } else {
                lp.setup(cellWidth, cellHeight, mWidthGap, mHeightGap, longAxisStartPadding,
                        shortAxisStartPadding);
            }
            
            if (lp.regenerateId) {
                child.setId(((getId() & 0xFF) << 16) | (lp.cellX & 0xFF) << 8 | (lp.cellY & 0xFF));
                lp.regenerateId = false;
            }

            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            int childheightMeasureSpec =
                    MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            child.measure(childWidthMeasureSpec, childheightMeasureSpec);
        }

        setMeasuredDimension(widthSpecSize, heightSpecSize);
        
        
        //初始化单元个坐标列表
        initCellCoordinateList(shortAxisCells,longAxisCells,longAxisStartPadding,shortAxisStartPadding,cellWidth,cellHeight,mWidthGap,mHeightGap);
        
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {

                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();

                int childLeft = lp.x;
                int childTop = lp.y;
                child.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height);

                if (lp.dropped) {
                    lp.dropped = false;

                    final int[] cellXY = mCellXY;
                    getLocationOnScreen(cellXY);
                    mWallpaperManager.sendWallpaperCommand(getWindowToken(), "android.home.drop",
                            cellXY[0] + childLeft + lp.width / 2,
                            cellXY[1] + childTop + lp.height / 2, 0, null);
                }
            }
        }
    }

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

    /**
     * Find a vacant area that will fit the given bounds nearest the requested
     * cell location. Uses Euclidean distance to score multiple vacant areas.
     * 
     * @param pixelX The X location at which you want to search for a vacant area.
     * @param pixelY The Y location at which you want to search for a vacant area.
     * @param spanX Horizontal span of the object.
     * @param spanY Vertical span of the object.
     * @param vacantCells Pre-computed set of vacant cells to search.
     * @param recycle Previously returned value to possibly recycle.
     * @return The X, Y cell of a vacant area that can contain this object,
     *         nearest the requested location.
     */
    int[] findNearestVacantArea(int pixelX, int pixelY, int spanX, int spanY,
            CellInfo vacantCells, int[] recycle) {
        
        // Keep track of best-scoring drop area
        final int[] bestXY = recycle != null ? recycle : new int[2];
        final int[] cellXY = mCellXY;
        double bestDistance = Double.MAX_VALUE;
        
        // Bail early if vacant cells aren't valid
        if (!vacantCells.valid) {
            return null;
        }

        // Look across all vacant cells for best fit
        final int size = vacantCells.vacantCells.size();
        for (int i = 0; i < size; i++) {
            final CellInfo.VacantCell cell = vacantCells.vacantCells.get(i);
            
            // Reject if vacant cell isn't our exact size
            if (cell.spanX != spanX || cell.spanY != spanY) {
                continue;
            }
            
            // Score is center distance from requested pixel
            cellToPoint(cell.cellX, cell.cellY, cellXY);
            
            double distance = Math.sqrt(Math.pow(cellXY[0] - pixelX, 2) +
                    Math.pow(cellXY[1] - pixelY, 2));
            if (distance <= bestDistance) {
                bestDistance = distance;
                bestXY[0] = cell.cellX;
                bestXY[1] = cell.cellY;
            }
        }

        // Return null if no suitable location found 
        if (bestDistance < Double.MAX_VALUE) {
            return bestXY;
        } else {
            return null;
        }
    }
    
    /**
     * Drop a child at the specified position
     *
     * @param child The child that is being dropped
     * @param targetXY Destination area to move to
     */
    void onDropChild(View child, int[] targetXY) {
        if (child != null) {
            LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.cellX = targetXY[0];
            lp.cellY = targetXY[1];
            lp.isDragging = false;
            lp.dropped = true;
            mDragRect.setEmpty();
            child.requestLayout();
            invalidate();
        }
    }

    void onDropAborted(View child) {
        if (child != null) {
            ((LayoutParams) child.getLayoutParams()).isDragging = false;
            invalidate();
        }
        mDragRect.setEmpty();
    }

    /**
     * Start dragging the specified child
     * 
     * @param child The child that is being dragged
     */
    void onDragChild(View child) {
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        lp.isDragging = true;
        mDragRect.setEmpty();
    }
    
    /**
     * Drag a child over the specified position
     * 
     * @param child The child that is being dropped
     * @param cellX The child's new x cell location
     * @param cellY The child's new y cell location 
     */
    void onDragOverChild(View child, int cellX, int cellY) {
        int[] cellXY = mCellXY;
        pointToCellRounded(cellX, cellY, cellXY);
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        cellToRect(cellXY[0], cellXY[1], lp.cellHSpan, lp.cellVSpan, mDragRect);
        invalidate();
    }
    
    /**
     * Computes a bounding rectangle for a range of cells
     *  
     * @param cellX X coordinate of upper left corner expressed as a cell position
     * @param cellY Y coordinate of upper left corner expressed as a cell position
     * @param cellHSpan Width in cells 
     * @param cellVSpan Height in cells
     * @param dragRect Rectnagle into which to put the results
     */
    public void cellToRect(int cellX, int cellY, int cellHSpan, int cellVSpan, RectF dragRect) {
        final boolean portrait = mPortrait;
        final int cellWidth = mCellWidth;
        final int cellHeight = mCellHeight;
        final int widthGap = mWidthGap;
        final int heightGap = mHeightGap;
        
        final int hStartPadding = portrait ? mShortAxisStartPadding : mLongAxisStartPadding;
        final int vStartPadding = portrait ? mLongAxisStartPadding : mShortAxisStartPadding;
        
        int width = cellHSpan * cellWidth + ((cellHSpan - 1) * widthGap);
        int height = cellVSpan * cellHeight + ((cellVSpan - 1) * heightGap);

        int x = hStartPadding + cellX * (cellWidth + widthGap);
        int y = vStartPadding + cellY * (cellHeight + heightGap);
        
        dragRect.set(x, y, x + width, y + height);
    }
    
    /**
     * Computes the required horizontal and vertical cell spans to always 
     * fit the given rectangle.
     *  
     * @param width Width in pixels
     * @param height Height in pixels
     */
    public int[] rectToCell(int width, int height) {
        // Always assume we're working with the smallest span to make sure we
        // reserve enough space in both orientations.
        final Resources resources = getResources();
        int actualWidth = resources.getDimensionPixelSize(R.dimen.workspace_cell_width);
        int actualHeight = resources.getDimensionPixelSize(R.dimen.workspace_cell_height);
        int smallerSize = Math.min(actualWidth, actualHeight);

        // Always round up to next largest cell
        int spanX = (width + smallerSize) / smallerSize;
        int spanY = (height + smallerSize) / smallerSize;

        return new int[] { spanX, spanY };
    }

    /**
     * Find the first vacant cell, if there is one.
     *
     * @param vacant Holds the x and y coordinate of the vacant cell
     * @param spanX Horizontal cell span.
     * @param spanY Vertical cell span.
     * 
     * @return True if a vacant cell was found
     */
    public boolean getVacantCell(int[] vacant, int spanX, int spanY) {
        final boolean portrait = mPortrait;
        final int xCount = portrait ? mShortAxisCells : mLongAxisCells;
        final int yCount = portrait ? mLongAxisCells : mShortAxisCells;
        final boolean[][] occupied = mOccupied;

        findOccupiedCells(xCount, yCount, occupied, null);

        return findVacantCell(vacant, spanX, spanY, xCount, yCount, occupied);
    }

    static boolean findVacantCell(int[] vacant, int spanX, int spanY,
            int xCount, int yCount, boolean[][] occupied) {

        for (int x = 0; x < xCount; x++) {
            for (int y = 0; y < yCount; y++) {
                boolean available = !occupied[x][y];
out:            for (int i = x; i < x + spanX - 1 && x < xCount; i++) {
                    for (int j = y; j < y + spanY - 1 && y < yCount; j++) {
                        available = available && !occupied[i][j];
                        if (!available) break out;
                    }
                }

                if (available) {
                    vacant[0] = x;
                    vacant[1] = y;
                    return true;
                }
            }
        }

        return false;
    }

    boolean[] getOccupiedCells() {
        final boolean portrait = mPortrait;
        final int xCount = portrait ? mShortAxisCells : mLongAxisCells;
        final int yCount = portrait ? mLongAxisCells : mShortAxisCells;
        final boolean[][] occupied = mOccupied;

        findOccupiedCells(xCount, yCount, occupied, null);

        final boolean[] flat = new boolean[xCount * yCount];
        for (int y = 0; y < yCount; y++) {
            for (int x = 0; x < xCount; x++) {
                flat[y * xCount + x] = occupied[x][y];
            }
        }

        return flat;
    }

    private void findOccupiedCells(int xCount, int yCount, boolean[][] occupied, View ignoreView) {
        for (int x = 0; x < xCount; x++) {
            for (int y = 0; y < yCount; y++) {
                occupied[x][y] = false;
            }
        }

        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child instanceof Folder || child.equals(ignoreView)) {
                continue;
            }
            LayoutParams lp = (LayoutParams) child.getLayoutParams();

            for (int x = lp.cellX; x < lp.cellX + lp.cellHSpan && x < xCount; x++) {
                for (int y = lp.cellY; y < lp.cellY + lp.cellVSpan && y < yCount; y++) {
                    occupied[x][y] = true;
                }
            }
        }
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new CellLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof CellLayout.LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new CellLayout.LayoutParams(p);
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {
        /**
         * Horizontal location of the item in the grid.
         */
        @ViewDebug.ExportedProperty
        public int cellX;

        /**
         * Vertical location of the item in the grid.
         */
        @ViewDebug.ExportedProperty
        public int cellY;

        /**
         * Number of cells spanned horizontally by the item.
         */
        @ViewDebug.ExportedProperty
        public int cellHSpan;

        /**
         * Number of cells spanned vertically by the item.
         */
        @ViewDebug.ExportedProperty
        public int cellVSpan;
        
        /**
         * Is this item currently being dragged
         */
        public boolean isDragging;

        // X coordinate of the view in the layout.
        @ViewDebug.ExportedProperty
        int x;
        // Y coordinate of the view in the layout.
        @ViewDebug.ExportedProperty
        int y;

        boolean regenerateId;
        
        boolean dropped;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            cellHSpan = 1;
            cellVSpan = 1;
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
            cellHSpan = 1;
            cellVSpan = 1;
        }
        
        public LayoutParams(int cellX, int cellY, int cellHSpan, int cellVSpan) {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            this.cellX = cellX;
            this.cellY = cellY;
            this.cellHSpan = cellHSpan;
            this.cellVSpan = cellVSpan;
        }

        public void setup(int cellWidth, int cellHeight, int widthGap, int heightGap,
                int hStartPadding, int vStartPadding) {
            
            final int myCellHSpan = cellHSpan;
            final int myCellVSpan = cellVSpan;
            final int myCellX = cellX;
            final int myCellY = cellY;
            
            width = myCellHSpan * cellWidth + ((myCellHSpan - 1) * widthGap) -
                    leftMargin - rightMargin;
            height = myCellVSpan * cellHeight + ((myCellVSpan - 1) * heightGap) -
                    topMargin - bottomMargin;

            x = hStartPadding + myCellX * (cellWidth + widthGap) + leftMargin;
            y = vStartPadding + myCellY * (cellHeight + heightGap) + topMargin;
        }
    }

    static final class CellInfo implements ContextMenu.ContextMenuInfo {
        /**
         * See View.AttachInfo.InvalidateInfo for futher explanations about
         * the recycling mechanism. In this case, we recycle the vacant cells
         * instances because up to several hundreds can be instanciated when
         * the user long presses an empty cell.
         */
        static final class VacantCell {
            int cellX;
            int cellY;
            int spanX;
            int spanY;

            // We can create up to 523 vacant cells on a 4x4 grid, 100 seems
            // like a reasonable compromise given the size of a VacantCell and
            // the fact that the user is not likely to touch an empty 4x4 grid
            // very often 
            private static final int POOL_LIMIT = 100;
            private static final Object sLock = new Object();

            private static int sAcquiredCount = 0;
            private static VacantCell sRoot;

            private VacantCell next;

            static VacantCell acquire() {
                synchronized (sLock) {
                    if (sRoot == null) {
                        return new VacantCell();
                    }

                    VacantCell info = sRoot;
                    sRoot = info.next;
                    sAcquiredCount--;

                    return info;
                }
            }

            void release() {
                synchronized (sLock) {
                    if (sAcquiredCount < POOL_LIMIT) {
                        sAcquiredCount++;
                        next = sRoot;
                        sRoot = this;
                    }
                }
            }

            @Override
            public String toString() {
                return "VacantCell[x=" + cellX + ", y=" + cellY + ", spanX=" + spanX +
                        ", spanY=" + spanY + "]";
            }
        }

        View cell;
        int cellX;
        int cellY;
        int spanX;
        int spanY;
        int screen;
        //----add by weijingchun 2011-1-20------
        int cellIndex;   //单元格索引号
        int containter; //单元格所在到容器 
        //-----------end -------------------
        boolean valid;

        final ArrayList<VacantCell> vacantCells = new ArrayList<VacantCell>(VacantCell.POOL_LIMIT);
        int maxVacantSpanX;
        int maxVacantSpanXSpanY;
        int maxVacantSpanY;
        int maxVacantSpanYSpanX;
        final Rect current = new Rect();

        void clearVacantCells() {
            final ArrayList<VacantCell> list = vacantCells;
            final int count = list.size();

            for (int i = 0; i < count; i++) list.get(i).release();

            list.clear();
        }

        void findVacantCellsFromOccupied(boolean[] occupied, int xCount, int yCount) {
            if (cellX < 0 || cellY < 0) {
                maxVacantSpanX = maxVacantSpanXSpanY = Integer.MIN_VALUE;
                maxVacantSpanY = maxVacantSpanYSpanX = Integer.MIN_VALUE;
                clearVacantCells();
                return;
            }

            final boolean[][] unflattened = new boolean[xCount][yCount];
            for (int y = 0; y < yCount; y++) {
                for (int x = 0; x < xCount; x++) {
                    unflattened[x][y] = occupied[y * xCount + x];
                }
            }
            CellLayout.findIntersectingVacantCells(this, cellX, cellY, xCount, yCount, unflattened);
        }

        /**
         * This method can be called only once! Calling #findVacantCellsFromOccupied will
         * restore the ability to call this method.
         *
         * Finds the upper-left coordinate of the first rectangle in the grid that can
         * hold a cell of the specified dimensions.
         *
         * @param cellXY The array that will contain the position of a vacant cell if such a cell
         *               can be found.
         * @param spanX The horizontal span of the cell we want to find.
         * @param spanY The vertical span of the cell we want to find.
         *
         * @return True if a vacant cell of the specified dimension was found, false otherwise.
         */
        boolean findCellForSpan(int[] cellXY, int spanX, int spanY) {
            return findCellForSpan(cellXY, spanX, spanY, true);
        }

        boolean findCellForSpan(int[] cellXY, int spanX, int spanY, boolean clear) {
            final ArrayList<VacantCell> list = vacantCells;
            final int count = list.size();

            boolean found = false;

            if (this.spanX >= spanX && this.spanY >= spanY) {
                cellXY[0] = cellX;
                cellXY[1] = cellY;
                found = true;
            }

            // Look for an exact match first
            for (int i = 0; i < count; i++) {
                VacantCell cell = list.get(i);
                if (cell.spanX == spanX && cell.spanY == spanY) {
                    cellXY[0] = cell.cellX;
                    cellXY[1] = cell.cellY;
                    found = true;
                    break;
                }
            }

            // Look for the first cell large enough
            for (int i = 0; i < count; i++) {
                VacantCell cell = list.get(i);
                if (cell.spanX >= spanX && cell.spanY >= spanY) {
                    cellXY[0] = cell.cellX;
                    cellXY[1] = cell.cellY;
                    found = true;
                    break;
                }
            }

            if (clear) clearVacantCells();

            return found;
        }

        @Override
        public String toString() {
            return "Cell[view=" + (cell == null ? "null" : cell.getClass()) + ", x=" + cellX +
                    ", y=" + cellY + "]";
        }
    }

    public boolean lastDownOnOccupiedCell() {
        return mLastDownOnOccupiedCell;
    }
    
   //--------------------motone method statement--------- 
   
    /**
     * 
     * 找出某屏中紧跟最后一个单元格的空位,如果中间的空位将找到中间的空位
     * @ author: 张永辉
     *@param screen
     *@return 返回空位单元格坐标
     */
    public static int [] findBlackCell(Context context,int screen){
        final int xCount = Mogoo_GlobalConfig.getWorkspaceLongAxisCellsPort();
        final int yCount = Mogoo_GlobalConfig.getWorkspaceShortAxisCellsPort();

        boolean[][] occupied = new boolean[xCount][yCount];

        final ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI,
            new String[] { LauncherSettings.Favorites.CELLX, LauncherSettings.Favorites.CELLY,
                    LauncherSettings.Favorites.SPANX, LauncherSettings.Favorites.SPANY },
            LauncherSettings.Favorites.SCREEN + "=?",
            new String[] { String.valueOf(screen) }, null);

        final int cellXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
        final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
        final int spanXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
        final int spanYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);

        try {
            while (c.moveToNext()) {
                int cellX = c.getInt(cellXIndex);
                int cellY = c.getInt(cellYIndex);
                int spanX = c.getInt(spanXIndex);
                int spanY = c.getInt(spanYIndex);

                for (int x = cellX; x < cellX + spanX && x < xCount; x++) {
                    for (int y = cellY; y < cellY + spanY && y < yCount; y++) {
                        occupied[x][y] = true;
                    }
                }
            }
        } catch (Exception e) {
            
        } finally {
            c.close();
        }
        
        for(int y=0;y<yCount;y++){
            for(int x=0;x<xCount;x++){
                if(occupied[x][y]==false){
                    return new int []{x,y};
                }
            }
        }
        return null ;
    }
    
    /**
     * 根据图标所在的单元格获取图标的索引号 
     * @ author: 邓炜
     * @param cellXY 单元格
     * @return 索引号
     */
    public int getIndexByCellXY(int cellXY[]) {
       
        int index = this.getCountX() * cellXY[1] + cellXY[0];
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "uninstall reload: cellX = "+cellXY[0]+" cellY ="+ cellXY[1] + " convert index =" + index) ;
        }
        return index;
    }
    
    /**
     * 根据图标所在的单元格获取图标的索引号 
     * @ author: 邓炜
     * @param cellXY 单元格
     * @return 索引号
     */
    public int getIndexByCellXY(int cellX, int cellY) {
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "uninstall reload: cellX = "+cellX+" cellY ="+ cellY) ;
        }
        
        int index = getCountX() * cellY + cellX;
        return index;
    }
    
    /**
     * 根据图标的索引号获取图标所在的单元格 
     * @ author: 邓炜
     * @param index 索引号
     * @return 单元格
     */
    public int[] convertToCell(int index) {
        int xCount = this.getCountX();
        return new int[] {
                index % this.getCountX(), index / this.getCountX()
        };
    }
    
    /**
     * 根据拖动图标的位置获取目标图标的索引号 
     * @ author: 邓炜
     * @param dragView 拖动图标
     * @return 索引号
     */
    /*public int findTargetIndex(DragView dragView) {
        int index = -1;

        View view = null;
        int left = dragView.getScreenLeft();
        int right = dragView.getScreenRight();
        int top = dragView.getScreenTop();
        int cellPoint[][] = dragView.getCellIndex();
        int indexOfCellLayout[] = cellPoint[0];
        int cellIndex = this.getIndexByCellXY(indexOfCellLayout);
        for (int i = 0; i < this.getChildCount(); i++) {
            view = this.getChildAt(i);
            if(view.getVisibility() != View.VISIBLE){
                continue;
            }
            if (Math.abs(top - view.getTop()) < 3 * view.getHeight() / 4) {
                if (left < view.getRight()) {
                    if(right < view.getLeft()){
                        return -2;
                    }
                    
                    if (cellIndex != i) {
                        index = i;
                    } else {
                        index = -2;
                    }
                    break;
                }
            }
        }

        return index;
    }*/

   
    
    
    /**
     * 取得所有桌面图标的左上角坐标值，此处的左上角为图标可视区域的左上角，不考虑背景区域
     * @ author: 张永辉
     *@return 返回坐标数组
     */
    int [][] getAllChildCoordinate(){
        int size = this.getChildCount() ;
        int [][] coordinates = new int [size][2];
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "size="+size);
        }
        
        for(int i=0;i<size;i++){
           View child = this.getChildAt(i);
           if(child instanceof Mogoo_BubbleTextView){
               
               Mogoo_BubbleTextView btv = (Mogoo_BubbleTextView)child ;
               //横坐标
               int left = btv.getLeft() - Mogoo_GlobalConfig.getWorkspaceCellPaddingLeft()  ;
               //纵坐标
               int top = btv.getTop() - Mogoo_GlobalConfig.getWorkspaceCellPaddingTop() ;
               coordinates[i][0] = left;
               coordinates[i][1] = top ;
               
               if(Mogoo_GlobalConfig.LOG_DEBUG){
                   Log.d(TAG, "X="+left+" Y="+top);
               }
           }
        }
        
        return coordinates ;
    }
    
    
    public CellEntry getCellEntry(int cellIndex)
    {
        if(cellIndex >= cellCoordinateList.size()){
            Log.d(TAG, "out Array index = " + cellIndex);
            return null;
        }
        
        return cellCoordinateList.get(cellIndex);
    }
    
    
    public List<CellEntry> getCellCoordinateList(){
        return cellCoordinateList ;
    }
    
    /**
     * 初始化单元格坐标值到列表中
     * @ author: 张永辉 
     *@param shortAxisCells 纵向单元格数
     *@param longAxisCells 横向单元格数
     *@param longAxisStartPadding 长轴开始大小
     *@param shortAxisStartPadding 短轴开始大小
     *@param cellWidth 单元格宽
     *@param cellHeight 单元格高
     *@param mWidthGap 单元格与单元格之间的间隙宽
     *@param mHeightGap 单元格与单元格之间的间隙高
     */
    private void initCellCoordinateList(int shortAxisCells, int longAxisCells, int longAxisStartPadding, int shortAxisStartPadding,
    							
    		int cellWidth, int cellHeight, int mWidthGap, int mHeightGap)
    {
    	cellCoordinateList.clear();
    	
    	for(int i=0;i<longAxisCells;i++)
    	{
    	    for(int j=0;j<shortAxisCells;j++)
    	    {
    	    	//endit by yeben 2012-2-10
//    	        int left = shortAxisStartPadding+j*cellWidth+j*mWidthGap ;
//    	        int right = left + cellWidth ;
    	        int left = shortAxisStartPadding+j*cellWidth +j*mWidthGap;
    	        int right = left + cellWidth +mWidthGap;
//    	        int top = longAxisStartPadding+i*cellHeight+i*mHeightGap ;
//    	        int bottom = top + cellHeight ;
    	        int top = longAxisStartPadding+i*cellHeight+i*mHeightGap ;
    	        int bottom = top + cellHeight +mHeightGap;
    	    	//end
    	        CellEntry entry = new CellEntry() ;
    	        entry.left = left ;
    	        entry.bottom = bottom ;
    	        entry.right = right ;
    	        entry.top = top ;
    	        
    	        if(Mogoo_GlobalConfig.LOG_DEBUG)
    	        {
    	            Log.d(TAG, "left="+left+" top="+top+" right="+right+" bottom="+bottom);
    	        }
    	        
    	        cellCoordinateList.add(entry);
    	    }
    	}
    	
    	
    	if(workspaceCellBottom == -1 && cellCoordinateList.size() == 16){
    	    workspaceCellBottom = getCellEntry(Mogoo_GlobalConfig.getWorkspaceCellCounts()-1).bottom;
    	}
    }

    public int getWorkspaceCellBottom() {
        return workspaceCellBottom;
    }
  //---------------------end---------------------------
}


