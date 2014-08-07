/**  
 * 文 件 名:  MT_FolderWorkspace.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：                       
 * 版    本:  1.0  
 * 创建时间:   2011-3-10
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-3-10        author       1.0          1.0 Version  
 */  

package com.mogoo.launcher2;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.CellEntry.CellEntryInface;
import com.mogoo.launcher2.CellLayout.CellInfo;
import com.mogoo.launcher2.animation.Mogoo_IconFolderAnimation;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_BitmapCache;
import com.mogoo.launcher2.utils.Mogoo_ClearBase;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;
import com.mogoo.launcher2.utils.Mogoo_Utilities;

import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Handler;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.View.OnLongClickListener;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;


public class Mogoo_FolderWorkspace extends ViewGroup implements DropTarget, DragSource, Mogoo_ClearBase, CellEntryInface, View.OnKeyListener  {
    
    private final static String TAG = "Launcher.Mogoo_FolderWorkspace" ;
    
    private boolean mPortrait = Mogoo_GlobalConfig.isPortrait();
    
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
    
    boolean[][] mOccupied;
    
    int[] mCellXY = new int[2];
    
    private RectF mDragRect = new RectF();
    
    private final Rect mRect = new Rect();
    private final CellInfo mCellInfo = new CellInfo();
    
    private final WallpaperManager mWallpaperManager;   
    
    private boolean mLastDownOnOccupiedCell = false;
    
    private boolean mDirtyTag;
    
    private Launcher launcher ;
    
    private Mogoo_FolderWorkspace folderWorkspace ;
    
    private Mogoo_BitmapCache mIconCache;
    
    private DragController dragController ;

    private CellLayout.CellInfo mDragInfo = new CellLayout.CellInfo();
    
    private ArrayList<CellEntry> cellCoordinateList = new ArrayList<CellEntry>();
    
    private Mogoo_IconFolderAnimation iconFolderAnimation;
    
    //正在打开的文件夹
    private Mogoo_FolderBubbleText loadingFolder;
    
    //刚关闭的文件夹
    private Mogoo_FolderBubbleText closeFolder ;
    
    //保存当前打开的文件夹ID
    private long folderId  ;
    
    //新生成的文件夹是否关闭过
    private boolean newFolderClosed = true ;
    
    //判断是否在本区域内拖动过
    private boolean isLocaleDrag = false;
    
    private Mogoo_FolderBubbleText folderTemp = null ; 
    
    private CellEntry dockLastOpenEntry = null;
    
    public static boolean acceptFlag = false;
    
    public boolean onKey(View v, int keyCode, KeyEvent event) 
    {
        if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER && loadingFolder != null)
        {
            saveFolderName();
        }
        return false;
    }

    public void setDockLastOpenEntry(CellEntry dockLastOpenEntry) {
        this.dockLastOpenEntry = dockLastOpenEntry;
    }

    /**
     * 
     * 保存文件名称
     * @ author: 黄悦
     */
    public void saveFolderName() {
        EditText titleEdit = (EditText)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.titleEdit, getContext());
        Editable eidtText = titleEdit.getText();
        
        if(eidtText == null || eidtText.toString().trim().length() == 0 || loadingFolder == null){ 
            return;
        }
        
        Mogoo_FolderInfo info = (Mogoo_FolderInfo) loadingFolder.getTag();
        
        mIconCache.recycle(info.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
        
        info.title = titleEdit.getText();
        loadingFolder.setText(info.title);
        LauncherModel.updateItemInDatabase(getContext(), info);
        
        loadingFolder.requestLayout();
        loadingFolder.invalidate();
    }
    
    public Mogoo_FolderWorkspace(Context context) {
        this(context,null);
        
    }

    public Mogoo_FolderWorkspace(Context context, AttributeSet attrs) {
        this(context, attrs,0);
        
    }
    
    public Mogoo_FolderWorkspace(Context context, AttributeSet attrs, int defStyle) {
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
        
        mLongAxisCells = Mogoo_GlobalConfig.isPortrait()?1:Mogoo_GlobalConfig.getWorkspaceLongAxisCellsLand();
        mShortAxisCells = Mogoo_GlobalConfig.isPortrait()?Mogoo_GlobalConfig.getWorkspaceShortAxisCellsPort():1;
        
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
        
        folderWorkspace = this ;
        
        LauncherApplication app = (LauncherApplication) this.getContext().getApplicationContext();
        mIconCache = app.getIconCache();
        
        iconFolderAnimation = new Mogoo_IconFolderAnimation(context, mIconCache);
        
        CellInfo cellInfo = new CellInfo();
        cellInfo.containter = R.id.folderWorkspace;
        setTag(cellInfo);
    }
    
    public Mogoo_IconFolderAnimation getIconFolderAnimation() {
        return iconFolderAnimation;
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
        final CellLayout.LayoutParams cellParams = (CellLayout.LayoutParams) params;
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
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "--------------------call onInterceptTouchEvent()--------------------------------------"); 
        }
        
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
                        final CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
                        cellInfo.cell = child;
                        cellInfo.cellX = lp.cellX;
                        cellInfo.cellY = lp.cellY;
                        cellInfo.spanX = lp.cellHSpan;
                        cellInfo.spanY = lp.cellVSpan;
                        cellInfo.cellIndex = getIndexByCellXY(cellInfo.cellX, cellInfo.cellY);
                        cellInfo.containter = R.id.folderWorkspace;
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
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "--------------------end--------------------------------------"); 
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
        
//        if(MT_GlobalConfig.LOG_DEBUG){
//            Log.d(TAG, "-------------------start onMeasure()-------------------") ;
//        }
        
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        
        //根据父组件分配的尺寸来得到当前组件应有的尺寸大小
        int widthSpecSize =  MeasureSpec.getSize(widthMeasureSpec);
        
        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSpecSize =  MeasureSpec.getSize(heightMeasureSpec);
        
//        if(MT_GlobalConfig.LOG_DEBUG){
//            Log.d(TAG, "widthSpecSize="+widthSpecSize +" heightSpecSize="+heightSpecSize) ;
//        }
        
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

        mPortrait = Mogoo_GlobalConfig.isPortrait(); 

        int numShortGaps = shortAxisCells - 1;
        int numLongGaps = longAxisCells - 1;
        
        heightSpecSize = longAxisStartPadding +longAxisEndPadding + (cellHeight * longAxisCells) ;

        if (mPortrait) {
            //取得垂直方向余下的尺寸
            int vSpaceLeft = heightSpecSize - longAxisStartPadding - longAxisEndPadding
                    - (cellHeight * longAxisCells);
            
            //将垂直方向余下的尺寸平分到单元格垂直间隙中
            if(numLongGaps>0){
                mHeightGap = vSpaceLeft / numLongGaps;
            }else{
                mHeightGap = 0 ;
            }

            //取得水平方向余下的尺寸
            int hSpaceLeft = widthSpecSize - shortAxisStartPadding - shortAxisEndPadding
                    - (cellWidth * shortAxisCells);
            
            //将水平方向余下的尺寸平分到单元格水平间隙中
            if (numShortGaps > 0) {
                mWidthGap = hSpaceLeft / numShortGaps;
            } else {
                mWidthGap = 0;
            }
        } else {
            int hSpaceLeft = widthSpecSize - longAxisStartPadding - longAxisEndPadding
                    - (cellWidth * longAxisCells);
            if(numLongGaps>0){
            	mWidthGap = hSpaceLeft / numLongGaps;
            }else{
            	mWidthGap = 0;
            }
            
            
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
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();

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
        
//        if(MT_GlobalConfig.LOG_DEBUG){
//            Log.d(TAG, "-------------------start onLayout()-------------------") ;
//        }
        
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
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
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
            ((CellLayout.LayoutParams) child.getLayoutParams()).isDragging = false;
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
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
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
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
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
            out:for (int i = x; i < x + spanX - 1 && x < xCount; i++) {
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
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();

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
    
    public boolean lastDownOnOccupiedCell() {
        return mLastDownOnOccupiedCell;
    }
    
    public void setDragController(DragController dragger) {
        this.dragController = dragger ;
    }
    
    
    public void startDrag(CellLayout.CellInfo cellInfo) 
    {
        View child = cellInfo.cell;

        // Make sure the drag was started by a long press as opposed to a long click.
        if (!child.isInTouchMode()) {
            return;
        }

        //mDragCellInfo = cellInfo;        
        setDragInfo(cellInfo.cellIndex);  
        
        dragController.startDrag2(cellInfo, child, this, child.getTag(),DragController.DRAG_ACTION_MOVE);
        mDragInfo.cell.setVisibility(INVISIBLE);
        invalidate();
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) 
    {
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "--------------------call onDragEnter()--------------------------------------isLocaleDrag="+isLocaleDrag); 
        }  
        
        if(!acceptFlag){
            return;
        }
        
        //如果是从别的区域拖进来的
        if(((ItemInfo)dragInfo).container != ((ItemInfo)loadingFolder.getTag()).id || fromOtherFolder(dragInfo) || isFull())
        {
            if(Mogoo_GlobalConfig.LOG_DEBUG)
            {
                Log.d(TAG, "--------------------drag from other area--------------------------------------mDragInfo.cell="+mDragInfo.cell); 
            }
            
            if(!acceptDrop(source, x, y, xOffset, yOffset, dragView, dragInfo)){
                return ;
            }
            
            isLocaleDrag = true ;
            
            View view = createFolderItem(dragInfo);
            mDragInfo.cell = view;
            mDragInfo.cellIndex = -1;
            mDragInfo.cell.setVisibility(View.INVISIBLE);
            mDragInfo.screen = Launcher.getScreen();
            
            addView(mDragInfo.cell, false);
            
            Mogoo_FolderInfo openFolderInfo = getOpenFolderInfo() ;
            if(openFolderInfo!=null){
                openFolderInfo.addItem(getContext(), (ShortcutInfo)mDragInfo.cell.getTag()) ;
            }
            
            setDragInfo(getChildCount()-1) ;
//            LauncherModel.setSortIconInfo(R.id.folderWorkspace, (int)folderId);
            
        }
        else
        {
            //拖出去又拖进来的情况
            if(isLocaleDrag)
            {
                View view = createFolderItem(dragInfo);
                mDragInfo.cell = view;
                mDragInfo.cellIndex = -1;
                mDragInfo.cell.setVisibility(View.INVISIBLE);
                mDragInfo.screen = Launcher.getScreen();
                
                addView(mDragInfo.cell, false);
                
                Mogoo_FolderInfo openFolderInfo = getOpenFolderInfo() ;
                if(openFolderInfo!=null){
                    openFolderInfo.addItem(launcher, (ShortcutInfo)mDragInfo.cell.getTag()) ;
                }
                
                setDragInfo(getChildCount()-1) ;
//                LauncherModel.setSortIconInfo(R.id.folderWorkspace, (int)folderId);
            }
            
            isLocaleDrag = true ;
            
            if(Mogoo_GlobalConfig.LOG_DEBUG)
            {
                Log.d(TAG, "--------------------drag from local area--------------------------------------"); 
            }
        }
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "--------------------call onDragEnter() end--------------------------------------"); 
        }
    }
    
    private boolean fromOtherFolder(Object dragInfo){
        if(getOpenFolderInfo() == null){
            return false;
        }
        
        if(!(dragInfo instanceof ItemInfo)){
            return true;
        }
        
        ItemInfo info = (ItemInfo) dragInfo;
        ItemInfo targetInfo = getOpenFolderInfo();
        
        return info.container != targetInfo.id;
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) {
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "--------------------start onDragOver()--------------------------------------"); 
        } 
        
        if(isFull()){
            return ;
        }
        
        int endIndex = findTargetIndex(dragView, this);
        if (mDragInfo != null  && dragController.sortView(this, mDragInfo.cellIndex, endIndex)) {
            setDragInfo(endIndex);
            updateShortcutInfo();
//            LauncherModel.setSortIconInfo(R.id.folderWorkspace, (int)folderId);
        }
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) {
    }
    

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) {
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "--------------------call onDrop()--------------------------------------"); 
        } 
        
        Mogoo_FolderController folderController = launcher.getFolderController();
        folderController.setCanActive(false);
//        folderController.iconFolderInactive();
        
        if(!acceptFlag){
            source.onRestoreDragIcon(dragInfo);
            return;
        }
        
        
        if(mDragInfo.cell!=null)
        {
            ShortcutInfo shortcutInfo = (ShortcutInfo) mDragInfo.cell.getTag();
            mIconCache.recycle(shortcutInfo.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
            ((Mogoo_BubbleTextView) mDragInfo.cell).setReflection(false);
            ((Mogoo_BubbleTextView) mDragInfo.cell).setIconReflection(null);
            
            mDragInfo.cell.setVisibility(View.VISIBLE) ;
            Mogoo_FolderInfo info = null;
            Mogoo_FolderBubbleText folder = null;
            if(loadingFolder != null){
                info = (Mogoo_FolderInfo) loadingFolder.getTag();
                folder = loadingFolder;
            }else if(closeFolder != null){
                info = (Mogoo_FolderInfo) closeFolder.getTag();
                folder = closeFolder;
            } else {
                throw new NullPointerException();
            }
                
            info.addItem(getContext(), (ShortcutInfo) mDragInfo.cell.getTag());
            
            mIconCache.recycle(info.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
            mIconCache.remove(info.intent.getComponent());
            folder.setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(info.getIcon(mIconCache)), null, null);
            
            folder.setText(info.title);
            
//            redrawIconWhenOpenFolder(folder, info);
//            loadFolderWorkspace(loadingFolder) ;
            
            if(mDragInfo.cell instanceof Mogoo_BubbleTextView){
                ((Mogoo_BubbleTextView)mDragInfo.cell).startVibrate(mIconCache, 0);
            }
        }
        //打开文件夹时，没有经过dragEnter直接onDrop时做如下处理
        else
        {
            //文件夹中加入新图标
            View view = createFolderItem(dragInfo);
            
            addView(view, false);
            
            Mogoo_FolderInfo openFolderInfo = getOpenFolderInfo() ;
            if(openFolderInfo!=null){
                openFolderInfo.addItem(launcher, (ShortcutInfo)view.getTag()) ;
            }
            
            //删除之前所在的容器中的图标并排序,因为发此种情况的只有在workspace时
            if(source instanceof Workspace)
            { 
                Workspace workspace = (Workspace) source ;
                CellLayout cellLayout = (CellLayout)workspace.getChildAt(workspace.getCurrentScreen()) ;
                ShortcutInfo info = (ShortcutInfo) dragInfo ;
                int index = Mogoo_Utilities.getIndexByCellXY(info.cellX, info.cellY) ;
//                dragController.sortView(cellLayout, index, cellLayout.getChildCount()-1) ;
                cellLayout.removeViewAt(index) ;
                reLayoutCellLayout(cellLayout) ;
            }
            
            mIconCache.recycle(openFolderInfo.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
            mIconCache.remove(openFolderInfo.intent.getComponent());
            loadingFolder.setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(openFolderInfo.getIcon(mIconCache)), null, null);
            
            //重新load文件夹
//            loadFolderWorkspace(loadingFolder) ;
            if(view instanceof Mogoo_BubbleTextView){
                ((Mogoo_BubbleTextView)view).startVibrate(mIconCache, 0);
            }
        }
        
        resetDragInfo();
        
        isLocaleDrag = false ;
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "-------------------------end--------------------------------------"); 
        }
    }

    private void redrawIconWhenOpenFolder(Mogoo_FolderBubbleText folder, Mogoo_FolderInfo info) {
        CellEntry entry = null;
        Mogoo_ComponentBus bus = Mogoo_ComponentBus.getInstance();
        Workspace ws = (Workspace)bus.getActivityComp(R.id.workspace, getContext());
        Mogoo_DockWorkSpace dock = (Mogoo_DockWorkSpace)bus.getActivityComp(R.id.dockWorkSpace, getContext());
        
        if(info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP){
            View v = ws.getChildAt(info.screen);
            ImageView image = (ImageView) bus.getActivityComp(R.id.folderLayerTopImage, getContext());
            if(v != null && image != null && image.getDrawable() != null){
                Bitmap bitmap = ((BitmapDrawable)image.getDrawable()).getBitmap();
                Canvas c = new Canvas(bitmap);
                
                folder.drawWorkspaceImage(mIconCache, c, ((CellLayout)v).getCellEntry(Mogoo_Utilities.getIndexByCellXY(info.cellX, info.cellY)));
                image.setImageBitmap(bitmap);
                c = null;
            }
            
            image = null;
        //dock 工具栏暂时不开放图标文件夹功能
        } else if(info.container == LauncherSettings.Favorites.CONTAINER_TOOLBAR){
//            ImageView topImage = (ImageView) bus.getActivityComp(R.id.folderLayerTopImage, getContext());
//            ImageView bottomImage = (ImageView) bus.getActivityComp(R.id.folderLayerBottomImage, getContext());
//            if(dock != null && bottomImage != null){
//                Bitmap bitmap = ((BitmapDrawable)bottomImage.getDrawable()).getBitmap();
//                Canvas c = new Canvas(bitmap);
//                
//                // 获取屏幕的高宽
//                int screenWidth = MT_GlobalConfig.getScreenWidth(); // 屏幕的宽
//                int screenHeight = MT_GlobalConfig.getScreenHeight(); // 屏幕的高
//                //取得当前状态栏的高度
//                int statusBarHeight = ws.getStatusBarHeight() ;
//                int dockStartY = screenHeight-statusBarHeight-dock.getHeight(); 
//                
//                folder.drawDockworkspaceImage(mIconCache, c, dockLastOpenEntry, dockStartY - topImage.getHeight());
//                bottomImage.setImageBitmap(bitmap);
//                c = null;
//            }
//            
//            topImage = null;
//            bottomImage = null;
        }
    }
    
    public void onDropCompleted(View target, boolean success) {
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "--------------------start onDropCompleted()--------------------------------------"); 
        } 
        
        resetDragInfo();
        LauncherModel.saveAllIconInfo(getContext());
        
        isLocaleDrag = false ;
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "-------------------------end--------------------------------------"); 
        }
    }

    public void onDropTargetChange(DragSource source, DropTarget dropTarget, DragView dragView,
            Object dragInfo) {
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "--------------------start onDropTargetChange()--------------------------------------"); 
            Log.d(TAG, "mDragInfo.cellIndex =" + mDragInfo.cellIndex);
        }
        
        if(mDragInfo!=null && mDragInfo.cell!=null)
        {
            Mogoo_ComponentBus bus = Mogoo_ComponentBus.getInstance();
            Workspace workspace = (Workspace)bus.getActivityComp(R.id.workspace, getContext()) ;
            workspace.setVisibility(View.VISIBLE); 
            bus.getActivityComp(R.id.dockWorkSpace, getContext()).setVisibility(View.VISIBLE);
            
//            MT_FolderBubbleText loadingFolderTemp = loadingFolder ;
            
            if(loadingFolder != null){
                closeFolder = loadingFolder ;
                outputView();
                
//                if(!newFolderClosed)
//                {
//                    System.out.println("===========================================");
//                    //将包含一个快捷方式图标的文件夹更新为里面的快捷方式图标
//                    //界面
//                    MT_FolderInfo folderInfo = (MT_FolderInfo)loadingFolderTemp.getTag() ;
//                    ShortcutInfo info = folderInfo.getContents().get(0) ;
//                    View view = createFolderItem(info);
//                    CellLayout cellLayout = (CellLayout)workspace.getChildAt(folderInfo.screen) ;
//                    
//                    cellLayout.addView(view,MT_Utilities.getIndexByCellXY(folderInfo.cellX, folderInfo.cellY)) ;
//                    
//                    workspace.removeView(loadingFolderTemp) ;
//                    
//                    info.cellX = folderInfo.cellX ;
//                    info.cellY = folderInfo.cellY ;
//                    info.container = LauncherSettings.Favorites.CONTAINER_DESKTOP ;
//                    info.screen = folderInfo.screen ;
//                    
//                    //db
//                    LauncherModel.deleteItemFromDatabase(launcher, folderInfo) ;
//                    LauncherModel.updateItemInDatabase(launcher, info);
//                    
//                } 
                
                //关闭前记录下要关闭的文件夹
//                closeFolder = (MT_FolderBubbleText) loadingFolder.clone() ;
                loadingFolder.closeFolder();
                
            }
            
//            LauncherModel.setSortIconInfo(R.id.folderWorkspace, (int)folderId);
        }
        
        resetDragInfo();
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "-------------------------- end --------------------------------------"); 
        }
    	
    }

    /**
     * 
     * 处理从folder 内拖到 folder外的事件
     * @ author: 黄悦
     */
    private void outputView() {
        final Mogoo_FolderInfo info = (Mogoo_FolderInfo) loadingFolder.getTag();
        info.getContents().remove(mDragInfo.cell.getTag());
        mIconCache.recycle(info.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
        
        if(info.getContents().size() > 0){
        	  mIconCache.remove(info.intent.getComponent());
            loadingFolder.setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(info.getIcon(mIconCache)), null, null);
            dragController.sortWithoutAnimation(this, mDragInfo.cellIndex, getChildCount() - 1);
            removeView(mDragInfo.cell);
            LauncherModel.updateItemInDatabase(getContext(), info);
        }else{
            
//            new Handler().postDelayed(new Runnable() {
//                public void run() {
                    if(loadingFolder == null){
                        return;
                    }
                    
                    ViewGroup parent = (ViewGroup) loadingFolder.getParent();
                    
                    if(parent == null){
                        return;
                    }
                    
                    int index = 0;
                    for(int i = 0; i < parent.getChildCount(); i++){
                        if(loadingFolder.equals(parent.getChildAt(i))){
                            index = i;
                            break;
                        }
                    }
                    
                    removeViews(0, getChildCount()) ;
                    
//                    closeFolder.setVisibility(View.INVISIBLE);
                    dragController.sortView(parent, index, parent.getChildCount() - 1);
                    LauncherModel.deleteItemFromDatabase(getContext(), info);
                    mIconCache.recycle(((ShortcutInfo)(loadingFolder.getTag())).intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
                    loadingFolder.stopVibrate();
                    parent.removeView(loadingFolder);
                    mIconCache.remove(((ShortcutInfo)(loadingFolder.getTag())).intent.getComponent());
                    LauncherModel.deleteItemFromDatabase(getContext(), info);
                    
                    if(parent instanceof Workspace)
                    {
                        LauncherModel.setSortIconInfo(R.id.workspace, info.screen);
                    }
                    else if(parent instanceof Mogoo_DockWorkSpace)
                    {
                        LauncherModel.setSortIconInfo(R.id.dockWorkSpace, -1);
                    }
//                    reLayoutCellLayout(parent) ;
//                }
//            }, 1500);
        }
//        updateShortcutInfo();
    }
    
    public void onRestoreDragIcon(Object dragInfo) {
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "--------------------start onRestoreDragIcon()--------------------------------------"); 
        }
        
        //拖向别的区域后，别的区域不接收时
        if (mDragInfo.cellIndex == -1)
        {
            //恢复文件夹
            restoreFolder((ShortcutInfo)dragInfo) ;
        }
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "------------------------- end --------------------------------------"); 
        }
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) 
    {
        
        if(dragInfo instanceof ShortcutInfo && !isFull())
        {
            return true ;
        }
        else
        {
            return false ;
        }
    }

    public Rect estimateDropLocation(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo, Rect recycle) {
        return null;
    }

    public int findTargetIndex(DragView dragView, ViewGroup parent) {
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "--------------------start findTargetIndex()--------------------------------------"); 
        }
        
        int targetIndex = -1;
        
        // 拖动图标左上角的坐标
        int x = dragView.getScreenLeft();
        int y = dragView.getScreenTop() - getScreenTop();
        int width = dragView.getWidth();
        int height = dragView.getHeight();
        
        // 取得拖动图标相交的第一个单元格的素引号
        int index = getCellIndexByDragView(dragView);
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "current screen child count:"+getChildCount()); 
        }
        
        if (index == -1) 
        {
            targetIndex = getChildCount() - 1; // 放于最后一个位置
        } 
        else if (index >= getChildCount())
        {
            targetIndex = getChildCount() - 1; // 放于最后一个位置
        }
        else {
            // index单元格
            CellEntry entry = getCellEntry(index);
            
            // 拖动图标左上角是否在index所在的单元格内
            if (x >= entry.left && x <= entry.right && y >= entry.top && y <= entry.bottom)
            {// 在里面
                targetIndex = findTargetIndexInner(x, y, width, height, index) ;
            }
            else
            {//在外面
                targetIndex = findTargetIndexOuter(x, y, width, height, index) ;
            }
        }
        
        // 如果目标位置大于等于图标总数目时，则放于最后一个位置
        if (targetIndex >= getChildCount()) {
            targetIndex = getChildCount() - 1;
        }

        // 重置拖动开始索引值
        if (targetIndex != -1) {
            dragView.setStartIndex(targetIndex);
        }

        if (Mogoo_GlobalConfig.LOG_DEBUG) {
            Log.d(TAG, "Find Target Index:" + targetIndex);
            Log.d(TAG, "------------------------- end --------------------------------------"); 
        }

        
        return targetIndex;
    }
    
    
    /**
     * 重整celllayout 
     * @ author: 张永辉
     *@param cellLayout
     */
    private void reLayoutCellLayout(ViewGroup parent)
    {
        if(parent instanceof CellLayout){
            Workspace ws = (Workspace) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.workspace, getContext());
            ws.reLayoutCellLayout(parent);
        }else if(parent instanceof Mogoo_DockWorkSpace){
            Mogoo_DockWorkSpace dw = (Mogoo_DockWorkSpace) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.dockWorkSpace, getContext());
            dw.resetCellLayout(new int[]{0});
        }
    }
    
    /**
     * 左上角在里面
     *@author: 张永辉
     *@Date：2011-3-22
     *@param x
     *@param y
     *@param width
     *@param height
     *@param index
     *@return
     */
    private int findTargetIndexInner(int x,int y,int width,int height,int index){
        int targetIndex = -1 ;
        
        // 看右上角在哪个单元格内
        int rightTopIndex = getCellIndexByCoordinate(x + width, y);
        // 看右下角在哪个单元格内
        int rightBottomIndex = getCellIndexByCoordinate(x + width, y + height);
        // 看左下角在哪个单元格内
        int leftBottomIndex = getCellIndexByCoordinate(x, y + height);

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
                targetIndex = index;
            }
            // 不在同一个单元格内，则通过面积来返回单元格号
            else 
            {
                //如果右下角不在单元格内，则返回左下角所在的单元格号
                if(rightBottomIndex==-1)
                {
                    targetIndex = leftBottomIndex  ;
                }
                else
                {
                    int leftPart = getCellEntry(leftBottomIndex).right - x ;
                    int rightPart = x + width - getCellEntry(rightBottomIndex).left ;
                    if(leftPart >= rightPart){
                        targetIndex = leftBottomIndex ;
                    }else{
                        targetIndex = rightBottomIndex ;
                    }
                }
            }
        }
        // 左上角与左下角不在同一个单元格内
        else {
            // 如果左上角与右上角在同一个单元格内
            if (index == rightTopIndex) {
                // 左下角在外面，则取左上角所在单元格号
                if (leftBottomIndex == -1) 
                {
                    targetIndex = index;
                }
                // 左下角在单元格内，则看是否跨了三个单元格
                else 
                {
                    //跨了三个单元格高度，则取中间的单元格号
                    if(leftBottomIndex-index == rowWidth*2)
                    {
                        targetIndex = index +  rowWidth  ;
                    }
                    //跨了二个单元格高度,则看上半部分与下半部分的面积大小
                    else
                    {
                        int topPart = getCellEntry(index).bottom - y;
                        int bottomPart = (y + height)
                                - getCellEntry(leftBottomIndex).top;

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
            // 如果左上角与右上角不在同一个单元格内
            else {
                // 左下角不在单元格内，看是否跨了二个单元格高度。
                if (leftBottomIndex == -1) {
                    //左边中点所在单元格号
                    int leftMiddleIndex = this.getCellIndexByCoordinate(x, y+height/2) ;
                    //占一个单元格高度,则取上半部分，再看左右大小
                    if(leftMiddleIndex==index||leftMiddleIndex==-1){
                        
                        //add by 张永辉 2011-7-21 
                        //跨三列或跨二列但右上角在单元格外面
                        if(rightTopIndex-index==2||(rightTopIndex==-1&&(rightTopIndex-index-1)%rowWidth==0)){
                            targetIndex = index+1 ;
                        }
                        //end add
                        
                        //跨二列或一列
                        //如果右上角不在单元格内，则返回左上角所在单元格号
                        else if(rightTopIndex==-1)
                        {
                            targetIndex = index ;
                        }
                        else
                        {
                            int leftPart = getCellEntry(index).right - x ;
                            int rightPart = x + width - getCellEntry(rightTopIndex).left ;
                            if(leftPart>=rightPart){
                                targetIndex = index ;
                            }else{
                                targetIndex = rightTopIndex ;
                            }
                        }
                    }
                    //占二个单元格高度
                    else 
                    {
                        //add by 张永辉 2011-7-21 
                        //跨三列或跨二列但右上角在单元格外面
                        if(rightTopIndex-index==2||(rightTopIndex==-1&&(rightTopIndex-index-1)%rowWidth==0)){
                            targetIndex = index+1+rowWidth ;
                        }
                        //end add
                        
                        //跨二列或一列
                        //如果右上角不在单元格内，则返回左边的
                        else if(rightTopIndex==-1)
                        {
                            targetIndex = index + rowWidth ;
                        }
                        else
                        {
                            int leftPart = getCellEntry(index).right - x ;
                            int rightPart = x + width - getCellEntry(rightTopIndex).left ; 
                            if(leftPart>=rightPart){
                                targetIndex = index + rowWidth ;
                            }else{
                                targetIndex = rightTopIndex + rowWidth ;
                            }
                        }
                    }
                    
                }
                // 左下角在单元格内，看是否跨越了三个单元格高度，
                else 
                {
                    //跨了三行
                    if(leftBottomIndex-index==rowWidth*2)
                    {
                        //add by 张永辉 2011-7-21 
                        //跨三列或跨二列但右上角在单元格外面
                        if(rightTopIndex-index==2||(rightTopIndex==-1&&(rightTopIndex-index-1)%rowWidth==0)){
                            targetIndex = index+1+rowWidth ;
                        }
                        //end add
                        
                        //跨二列或一列
                        //如果右上角不在单元格内，则返回左边的
                        else if(rightTopIndex==-1)
                        {
                            targetIndex = index + rowWidth ;
                        }
                        else
                        {
                            int leftPart = getCellEntry(index).right - x ;
                            int rightPart = x + width - getCellEntry(rightTopIndex).left ; 
                            if(leftPart>=rightPart){
                                targetIndex = index + rowWidth ;
                            }else{
                                targetIndex = rightTopIndex + rowWidth ;
                            }
                        }
                    }
                    //跨了二行，则看上下部分哪个部分大
                    else
                    {
                        int topPart = getCellEntry(index).bottom - y;
                        int bottomPart = (y + height)
                                - getCellEntry(leftBottomIndex).top;
                        
                        //add by 张永辉 2011-7-21 
                        //跨三列或跨二列但右上角在单元格外面
                        if(rightTopIndex-index==2||(rightTopIndex==-1&&(rightTopIndex-index-1)%rowWidth==0)){
                            if (topPart >= bottomPart) {
                                targetIndex = index+1 ;
                            }else{
                                targetIndex = index+1+rowWidth ;
                            }
                        }
                        //end add

                        //跨一列或二列
                        // 上半部分大,则再看左右大小
                        else if (topPart >= bottomPart) {
                            //如果右边在单元格外面，则直接返回左上角的单元格号
                            if(rightTopIndex==-1)
                            {
                                targetIndex = index  ;
                            }
                            else
                            {
                                int leftPart = getCellEntry(index).right - x ;
                                int rightPart = x + width - getCellEntry(rightTopIndex).left ; 
                                if(leftPart>=rightPart){
                                    targetIndex = index  ;
                                }else{
                                    targetIndex = rightTopIndex  ;
                                }
                            }
                        }
                        // 下半部分大,则再左右大小
                        else {
                            //右下角是否在单元格外面，如果在，则返回左下角所在的单元格号
                            if(rightBottomIndex==-1)
                            {
                                targetIndex = leftBottomIndex ;
                            }
                            else
                            {
                                int leftPart = getCellEntry(leftBottomIndex).right - x ;
                                int rightPart = x + width - getCellEntry(rightBottomIndex).left ; 
                                if(leftPart>=rightPart){
                                    targetIndex = leftBottomIndex ;
                                }else{
                                    targetIndex = rightBottomIndex ;
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
     * 左上角在外面
     *@author: 张永辉
     *@Date：2011-3-22
     *@param x
     *@param y
     *@param width
     *@param height
     *@param index
     *@return
     */
    private int findTargetIndexOuter(int x,int y,int width,int height,int index){
        int targetIndex = -1 ;
        
        // 看右上角在哪个单元格内
        int rightTopIndex = getCellIndexByCoordinate(x + width, y);
        // 看右下角在哪个单元格内
        int rightBottomIndex = getCellIndexByCoordinate(x + width, y + height);
        // 看左下角在哪个单元格内
        int leftBottomIndex = getCellIndexByCoordinate(x, y + height);

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
                    targetIndex = index;
                } else {
                    targetIndex = -1;
                }
            }
            // 如果右上角在单元格内，且右上角和右下角都在同一个单元格内，则返回右上角所在的单元格号
            else if (rightTopIndex == rightBottomIndex) {
                targetIndex = rightTopIndex;
            }
            // 如果右上角和右下角所在的单元格不相同，则比较拖动图标在这二个单元格中哪个占用面积大，则返回哪个单元格号
            else if (rightTopIndex != rightBottomIndex) {
                // 右下角在单元格外面
                if (rightBottomIndex == -1){
                    //右边中点所在单元格
                    int rightMiddleIndex = this.getCellIndexByCoordinate(x+width, y+height/2) ;
                    
                    //add by 张永辉 2011-7-21 
                    //跨了二列
                    if(rightTopIndex%rowWidth!=0&&(rightTopIndex-1)%rowWidth==0&&(index+rowWidth)<Mogoo_GlobalConfig.getWorkspaceCellCounts()-rowWidth){
                        //高度占一行
                        if(rightTopIndex==rightMiddleIndex||rightMiddleIndex==-1){
                            targetIndex = index  ;
                        }
                        //高度占二行
                        else{
                            targetIndex = index + rowWidth ;
                        }
                    }
                    //end add
                    
                    //一列
                    //如果右边中点与右上角在同一个单元格内，则返回右上角所在单元格号
                    else if(rightTopIndex==rightMiddleIndex) {
                        targetIndex = rightTopIndex;
                    }
                    //右边中点在外面
                    else if(rightMiddleIndex==-1){
                        targetIndex = rightTopIndex;
                    }
                    //右边中间在里面，则取右中点所在单元格号
                    else {
                        targetIndex = rightMiddleIndex;
                    }
                }
                // 右下角在单元格里面
                else {
                    //如果右边跨越三个单元格，则选中间的单元格
                    if(rightBottomIndex-rightTopIndex==rowWidth*2){
                        
                        //add by 张永辉 2011-7-21 
                        //跨二列
                        if(rightTopIndex%rowWidth!=0&&(rightTopIndex-1)%rowWidth==0&&(index+rowWidth)<Mogoo_GlobalConfig.getWorkspaceCellCounts()-rowWidth){
                            targetIndex = index + rowWidth ;
                        }
                        //end 
                        
                        //跨一列
                        else{
                            targetIndex = rightTopIndex + rowWidth ;
                        }
                    }
                    //跨越二个单元格
                    else {
                        int topPart = getCellEntry(rightTopIndex).bottom - y;
                        int bottomPart = (y + height)
                                - getCellEntry(rightBottomIndex).top;
                        
                        //add by 张永辉 2011-7-21 
                        //跨二列
                        if(rightTopIndex%rowWidth!=0&&(rightTopIndex-1)%rowWidth==0&&(index+rowWidth)<Mogoo_GlobalConfig.getWorkspaceCellCounts()-rowWidth){
                            if (topPart >= bottomPart) {
                                targetIndex = index ;
                            }
                            else{
                                targetIndex = index + rowWidth ;
                            }
                        }
                        //end 

                        //跨一列
                        else if (topPart >= bottomPart) {
                            targetIndex = rightTopIndex;
                        } else {
                            targetIndex = rightBottomIndex;
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
                    targetIndex = index ;
                }
                //跨一行,则返回左下角所在的单元格号
                else{
                    targetIndex = leftBottomIndex;
                }
            }
            // 如果不在同一个单元格内，看是否跨二行单元 
            else {
                //add by 张永辉 2011-7-21
                //跨三列或二列但右下角在单元格外面
                if(rightBottomIndex-index==2||(rightBottomIndex==-1&&(rightBottomIndex-index-1)%rowWidth==0)){
                    targetIndex = index+1;
                }
                //end add 
                
                //跨二列或一列
                //跨二行,看哪个占面积大，则返回哪一个单元格号
                else if(leftBottomIndex-index==rowWidth){
                    //如果右下角在单元格外面，则返回左边的
                    if(rightBottomIndex==-1)
                    {
                        targetIndex = leftBottomIndex - rowWidth ;
                    }
                    else
                    {
                        int leftPart = this.getCellEntry(leftBottomIndex).right - x ;
                        int rightPart = x + width - this.getCellEntry(rightBottomIndex).left ;
                        if(leftPart>=rightPart){
                            targetIndex = leftBottomIndex - rowWidth ;
                        }else{
                            targetIndex = rightBottomIndex - rowWidth ;
                        }
                    }
                }
                //跨一行,看哪个占面积大，则返回哪一个单元格号
                else{
                    //如果右下角在单元格外面，则返回左下解所在的单元格号
                    if(rightBottomIndex==-1)
                    {
                        targetIndex = leftBottomIndex ;
                    }
                    else
                    {
                        int leftPart = this.getCellEntry(leftBottomIndex).right - x ;
                        int rightPart = x + width - this.getCellEntry(rightBottomIndex).left ;
                        if(leftPart>=rightPart){
                            targetIndex = leftBottomIndex ;
                        }else{
                            targetIndex = rightBottomIndex ;
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
    
    public CellEntry getCellEntry(int cellIndex)
    {
        if(cellIndex >= cellCoordinateList.size()){
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
        
//        if(MT_GlobalConfig.LOG_DEBUG){
//            Log.d(TAG, "shortAxisCells="+shortAxisCells+" longAxisCells="+longAxisCells) ;
//            Log.d(TAG, "longAxisStartPadding="+longAxisStartPadding+" shortAxisStartPadding="+shortAxisStartPadding) ;
//            Log.d(TAG, "cellWidth="+cellWidth+" cellHeight="+cellHeight) ;
//            Log.d(TAG, "mWidthGap="+mWidthGap+" mHeightGap="+mHeightGap) ;
//        }
        
        cellCoordinateList.clear();
        
        for(int i=0;i<longAxisCells;i++)
        {
            for(int j=0;j<shortAxisCells;j++)
            {
                int left = shortAxisStartPadding+j*cellWidth+j*mWidthGap ;
                int right = left + cellWidth ;
                int top = longAxisStartPadding+i*cellHeight+i*mHeightGap ;
                int bottom = top + cellHeight ;
                CellEntry entry = new CellEntry() ;
                entry.left = left ;
                entry.bottom = bottom ;
                entry.right = right ;
                entry.top = top ;
                
//                if(MT_GlobalConfig.LOG_DEBUG)
//                {
//                  Log.d(TAG, "left="+left+" top="+top+" right="+right+" bottom="+bottom);
//                }
                
                cellCoordinateList.add(entry);
            }
        }
        
    }
    
    /**
     * 加载文件夹内容区域 
     * @author: 张永辉
     * @param folder
     */
    public void loadFolderWorkspace(Mogoo_FolderBubbleText folder){
        if(folder == null){
            return;
        }
        
//        clearViews();
        
        Object tag = folder.getTag() ;
        
        if(tag instanceof Mogoo_FolderInfo){
            
            Mogoo_FolderInfo info = (Mogoo_FolderInfo)tag ;
            List<ShortcutInfo> items =  info.getContents() ;
            loadingFolder = folder;
            
            //内容排序
            Collections.sort(items, new Mogoo_FolderInfo.SortByIndex()) ;
            
            int size = items.size() ;
            
            folderId = info.id ;
            
            if (Mogoo_GlobalConfig.isPortrait()) {
                //列数
                mShortAxisCells = Mogoo_GlobalConfig.getWorkspaceShortAxisCellsPort() ;
                //行数
                mLongAxisCells = (size-1)/mShortAxisCells +1 ;
                
                if(info.isAddRow())
                {
                    mLongAxisCells+=1 ;
                }
                
                mOccupied = new boolean[mShortAxisCells][mLongAxisCells];
            } else {
                //列数
                mLongAxisCells = Mogoo_GlobalConfig.getWorkspaceLongAxisCellsLand() ;
                //行数
                mShortAxisCells = (size-1)/mLongAxisCells +1 ;
                
                if(info.isAddRow())
                {
                    mShortAxisCells+=1 ;
                }
                
                mOccupied = new boolean[mLongAxisCells][mShortAxisCells];
            }
            
            View view = null ;
            ShortcutInfo item = null;
            Mogoo_BitmapCache iconCache = ((LauncherApplication)getContext().getApplicationContext()).getIconCache();
            
            for(int i = 0; i < items.size(); i++){
                item = items.get(i);
                view = createFolderItem(item);
                if(view!=null){
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();
                    int index = getIndexByCellXY(item.cellX,item.cellY);
                    
                    if(index >= getChildCount()){
                        index = i;
                        item.cellX = index % getCountX(); 
                        item.cellY = index / getCountX();
                    }
                    
                    if (lp == null) {
                        lp = new CellLayout.LayoutParams(item.cellX, item.cellY, 1, 1);
                    } else {
                        lp.cellX = item.cellX;
                        lp.cellY = item.cellY;
                        lp.cellHSpan = 1;
                        lp.cellVSpan = 1;
                    }
                    
                    this.addView(view, index, lp) ;
                    if(view instanceof Mogoo_BubbleTextView){
                        ((Mogoo_BubbleTextView)view).startVibrate(iconCache, 0);
                    }
                }
            }
            
            requestLayout() ;
            invalidate() ;
        }
    }

    /**
     * 
     * 停止抖动，清除所有对象
     * @ author: 黄悦
     */
    public void clearViews() {
        int size = getChildCount();
        View v = null;
        for(int i = 0; i < size; i++){
            v = getChildAt(i);
            if(v instanceof Mogoo_BubbleTextView){
                ((Mogoo_BubbleTextView)v).stopVibrate();
            }
        }
        
        this.removeAllViewsInLayout();
        layout(getLeft(), getTop(), getRight(), getBottom());
    }
    
    /**
     * 根据图标所在的单元格获取图标的索引号 
     * @author:张永辉
     * @param cellXY 单元格
     * @return 索引号
     */
    public int getIndexByCellXY(int cellX, int cellY) {
        int index = getCountX() * cellY + cellX;
        return index;
    }
    
    /**
     *创建文件夹内部图标 
     *@author: 张永辉
     *@param dragInfo
     *@return
     */
    View createFolderItem(Object dragInfo) {
        View view;
        ItemInfo info = (ItemInfo) dragInfo;
        //修正图标类型错误
        if(dragInfo instanceof ShortcutInfo && !(dragInfo instanceof Mogoo_FolderInfo)){
        	info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
        }

        switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                view = launcher.createShortcut(R.layout.application, folderWorkspace,
                        (ShortcutInfo) info);
                break;
            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
        }

        if (view == null)
            return null;

        // 生成快捷键图标之前，清空图标缓冲区中该图标过时的图标位图
//        mIconCache.recycle(((ShortcutInfo) info).intent.getComponent(),
//                MT_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
        
        view.setHapticFeedbackEnabled(false);
        view.setOnLongClickListener(launcher);
        
        return view;
    }
    
    public Launcher getLauncher() {
        return launcher;
    }

    public void setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }
    
    public Mogoo_FolderBubbleText getLoadingFolder() {
        return loadingFolder;
    }

    public void setLoadingFolder(Mogoo_FolderBubbleText loadingFolder) {
        this.loadingFolder = loadingFolder;
    }
    
    public boolean isNewFolderClosed() {
        return newFolderClosed;
    }

    public void setNewFolderClosed(boolean newFolderClosed) {
        this.newFolderClosed = newFolderClosed;
    }

    public Mogoo_FolderBubbleText getCloseFolder() {
        return closeFolder;
    }
    
    public void onClear() {
        
    }
    
    private int getScreenTop() {
//        int topHeight = ((ImageView)(MT_ComponentBus.getInstance().getActivityComp(R.id.folderLayerTopImage, launcher))).getHeight() ;
        int topHeight = 0;
        if(loadingFolder != null){
            topHeight = loadingFolder.getTopHeight();
        }else{
            topHeight = closeFolder.getTopHeight();
        }
        int editTitleHeight = ((EditText)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.titleEdit , launcher)).getHeight() ;
        int statusBarHeight = ((Workspace)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.workspace , launcher)).getStatusBarHeight() ;
        return topHeight + editTitleHeight + statusBarHeight;
    }
    
    /**
     * 取得拖动图标相交的第一个单元格的素引号 
     * @author: 张永辉
     * @return
     */
    private int getCellIndexByDragView(DragView dragView) {
        int x = dragView.getScreenLeft();
        int screenTop = getScreenTop() ;
        int y = dragView.getScreenTop() - screenTop;
        int width = dragView.getWidth();
        int height = dragView.getHeight();

        // 找不到在哪一个单元格中时，返回-1
        int index = -1;
        
        // 单元格总数
        int count = getCellCoordinateList() != null ? getCellCoordinateList()
                .size() : 0;

        int longAxisStartPadding = this.mLongAxisStartPadding;
        int shortAxisStartPadding = this.mShortAxisStartPadding;

        if ((x < shortAxisStartPadding && (x + width) <= shortAxisStartPadding)
                || (y < longAxisStartPadding && (y + height) <= longAxisStartPadding)) {
            index = -1;
        } else if (x < shortAxisStartPadding && y < longAxisStartPadding) {
            index = 0;
        } else if (x < shortAxisStartPadding) {
            for (int i = 0; i < count; i++) {
                CellEntry entry = getCellEntry(i);
                if (entry.left == shortAxisStartPadding && y >= entry.top && y <= entry.bottom) {
                    index = i;
                }
            }
        } else if (y < longAxisStartPadding) {
            for (int i = 0; i < count; i++) {
                CellEntry entry = getCellEntry(i);
                if (entry.top == longAxisStartPadding && x >= entry.left && x <= entry.right) {
                    index = i;
                }
            }
        } else {
            for (int i = 0; i < count; i++) {
                CellEntry entry = getCellEntry(i);
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

        // 单元格总数
        int count = getCellCoordinateList() != null ? getCellCoordinateList()
                .size() : 0;

        for (int i = 0; i < count; i++) {
            CellEntry entry = getCellEntry(i);
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
    
    /**
     * 
     *@author: 张永辉
     *@Date：2011-3-23
     *@param dragInfo
     *@return
     */
//    private View createIcon(Object dragInfo)
//    {
//
//        ItemInfo info = (ItemInfo) dragInfo;
//
//        View view = null;
//
//        switch (info.itemType) 
//        {
//            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
//            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
//                view = launcher.createShortcut(R.layout.application, this,(ShortcutInfo) info, false);
//                break;
//            case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
//            case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
//            case LauncherSettings.Favorites.ITEM_TYPE_WIDGET_PHOTO_FRAME:
//            case LauncherSettings.Favorites.ITEM_TYPE_WIDGET_SEARCH:
//            case LauncherSettings.Favorites.ITEM_TYPE_WIDGET_CLOCK:                 
//                break;
//            default:
//                throw new IllegalStateException("Unknown item type: " + info.itemType);
//        }
//        
//        if (view == null)
//        {
//            return null;
//        }
//        
//        view.setHapticFeedbackEnabled(false);
//        view.setOnLongClickListener(launcher);
//        if (view instanceof DropTarget) 
//        {
//            dragController.addDropTarget((DropTarget) view);
//        }
//        
//        return view;
//    }
    
    private void setDragInfo(int index) {
        mDragInfo.cell = getChildAt(index);
        mDragInfo.cellIndex = index;
        int[] cellXY = Mogoo_Utilities.convertToCell(index);
        mDragInfo.cellX = cellXY[0];
        mDragInfo.cellY = cellXY[1];
        mDragInfo.screen = launcher.getCurrentWorkspaceScreen();
    }
    
    /**
     * 判断是否有Invisible对象
     */
    private boolean isInvisibleChild() {
        boolean flag = false;
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            if (getChildAt(i).getVisibility() == INVISIBLE) {
                flag = true;
                break;
            }
        }
        return flag;
    }
    
    private void resetDragInfo() {
        mDragInfo.cell = null;
        mDragInfo.cellIndex = -1;
        mDragInfo.cellX = -1;
        mDragInfo.cellY = -1;
        mDragInfo.screen = -1;
    }
    
    /**
     * 取得打开文件夹的info
     *@author: 张永辉
     *@Date：2011-3-28
     *@return
     */
    private Mogoo_FolderInfo getOpenFolderInfo(){
        if(loadingFolder!=null)
        {
            return (Mogoo_FolderInfo)loadingFolder.getTag() ;
        }
        else
        {
            return null ;
        }
    }
    
    /**
     * 是否排満
     *@author: 张永辉
     *@Date：2011-3-28
     *@return
     */
    private boolean isFull(){
        int count = this.getChildCount() ;
        for(int i=0;i<count;i++){
            if(this.getChildAt(i).getVisibility()==View.INVISIBLE){
                return false ;
            }
        }
        
        if(count<Mogoo_GlobalConfig.getWorkspaceLongAxisCells(Mogoo_GlobalConfig.isLandscape())
                *(Mogoo_GlobalConfig.getWorkspaceShortAxisCells(Mogoo_GlobalConfig.isLandscape())-1)){
            return false ;
        }
        return true ;
    }
    
    /**
     * 更新内部快捷方式的info
     *@author: 张永辉
     *@Date：2011-3-28
     */
    private void updateShortcutInfo(){
        int count = this.getChildCount() ;
        
        for(int i=0;i<count;i++){
            View child = getChildAt(i) ;
            ShortcutInfo info = (ShortcutInfo) child.getTag() ;
            int [] cellXY = Mogoo_Utilities.convertToCell(i) ;
            info.cellX = cellXY[0] ;
            info.cellY = cellXY[1] ;
        }
    }
    
    public void removeView(View view) {
        super.removeView(view);
        
        Mogoo_FolderBubbleText folder = null;
        if(loadingFolder != null){
        	folder = loadingFolder;
        }else if(closeFolder != null){
        	folder = closeFolder;
        }else{
        	return;
        }
        
        Mogoo_ContentListener contentListener = launcher.getContentListener();
        folder.stopVibrate();
        if(view instanceof Mogoo_BubbleTextView){
            Mogoo_BubbleTextView btv = (Mogoo_BubbleTextView) view;
            ShortcutInfo info = (ShortcutInfo) btv.getTag();
            if(contentListener.isListenType(info.appType)){
                contentListener.removeItem(info.appType, folder);
                folder.getCountMap().remove(info.appType);
                folder.setCountIcon(mIconCache, 0, info.appType);
            }
        }
        folder.startVibrate(mIconCache, 0);
    }
    
    public void addView(View child) {
        addView(child, false);
    }
    
    public void addView(View view,boolean insertAtFirst)
    {
        Mogoo_ContentListener contentListener = launcher.getContentListener();
        if(loadingFolder != null){
            loadingFolder.stopVibrate();
            if(view instanceof Mogoo_BubbleTextView){
                ShortcutInfo info = (ShortcutInfo) ((Mogoo_BubbleTextView) view).getTag();
                if(contentListener.isListenType(info.appType)){
                    contentListener.addItem(info.appType, loadingFolder);
                    loadingFolder.setCountIcon(mIconCache, contentListener.getCountByType(info.appType), info.appType);
                }
                
            }
            loadingFolder.startVibrate(mIconCache, 0);
        }
        
         addView(view, insertAtFirst ? 0 : -1);
         int endindex = getChildCount() -1 ;
         int[] endCell =  Mogoo_Utilities.convertToCell(endindex);
         onDropChild(view, endCell);
    }
    
    /**
     * 恢复文件夹
     *@author: 张永辉
     *@Date：2011-4-7
     *@param info
     */
    void restoreFolder(ShortcutInfo info)
    {
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "------------------call restoreFolder()----------------") ;
        }
        
        if(closeFolder!=null)
        {
            Mogoo_FolderInfo folderInfo = (Mogoo_FolderInfo)closeFolder.getTag();
            int size = folderInfo.getContents().size() ;
            //之前关闭的文件夹己删除了，则恢复图标到原来文件夹所在面的最后一个位置
            if(size==0)
            {
                
                if(Mogoo_GlobalConfig.LOG_DEBUG)
                {
                    Log.d(TAG, "------------------closeFolder size=0 ----------------") ;
                }
                
//                folderInfo = new MT_FolderInfo(folderInfo); 
                int cellXY []  ;
                View view = null;
                //workspace中
                if(folderInfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP)
                {
                    Workspace workspace = (Workspace) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.workspace, getContext());
                    CellLayout cellLayout = (CellLayout)workspace.getChildAt(folderInfo.screen) ;
                    cellXY = cellLayout.convertToCell(cellLayout.getChildCount()) ;
                    if(cellXY!=null)
                    {
                        info.cellX = cellXY[0];
                        info.cellY = cellXY[1];
                        info.container = folderInfo.container;
                        info.screen = folderInfo.screen;
                        mIconCache.recycle(info.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
                        mIconCache.remove(info.intent.getComponent());
                        
                        LauncherModel.updateItemInDatabase(getContext(), info) ;
                        
                        view = workspace.createIcon(info) ;
                        workspace.addView(cellLayout, view, false) ;
                        
                        LauncherModel.setSortIconInfo(R.id.workspace, folderInfo.screen);
                        ((Mogoo_BubbleTextView)view).startVibrate(mIconCache, 0);
                        closeFolder = null;
                    }
                   
                }
                else
                {
                    Mogoo_DockWorkSpace docWorkspace = (Mogoo_DockWorkSpace) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.dockWorkSpace, getContext()) ;
                    cellXY = Mogoo_Utilities.convertToCell(docWorkspace.getChildCount()) ;
                    
                    if(cellXY!=null)
                    {
                        info.cellX = cellXY[0];
                        info.cellY = cellXY[1];
                        info.container = folderInfo.container;
                        info.screen = folderInfo.screen;
                        mIconCache.recycle(info.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
                        mIconCache.remove(info.intent.getComponent());
                        
                        LauncherModel.updateItemInDatabase(getContext(), info) ;
                        
                        view = docWorkspace.createReflectionIcon(info) ;
                        
                        docWorkspace.addView(view, docWorkspace.getChildCount()) ;
                        docWorkspace.resetCellLayout(new int [1]) ;
                        
                        LauncherModel.setSortIconInfo(R.id.dockWorkSpace, -1);
                        ((Mogoo_BubbleTextView)view).startVibrate(mIconCache, 0);
                        closeFolder = null;
                    }
                }
                view = null;
                //DB中
                //内存中
//                if(cellXY!=null)
//                {
//                    folderInfo.cellX = cellXY[0] ;
//                    folderInfo.cellY = cellXY[1] ;
//                    LauncherModel.addItemToDatabase(getContext(), folderInfo, folderInfo.container, folderInfo.screen, folderInfo.cellX, folderInfo.cellY, false) ;
//                    
//                    System.out.println("----------id="+folderInfo.id+"-----------------");
//                    if(folderInfo.id != -1)
//                    {
                        
//                        folderInfo.intent = MT_Utilities.generateMtFolderIntent(folderInfo.id) ;
//                        LauncherModel.updateItemInDatabase(getContext(), folderInfo) ;
//                        
//                        folderInfo.addItem(getContext(), info) ;
//                        
//                        //界面上
//                        View view = launcher.createShortcut(folderInfo) ;
//                        
//                        if(folderInfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP)
//                        {
//                            Workspace workspace = (Workspace) MT_ComponentBus.getInstance().getActivityComp(R.id.workspace, getContext());
//                            CellLayout cellLayout = (CellLayout)workspace.getChildAt(folderInfo.screen) ;
//                            cellLayout.addView(view,cellLayout.getChildCount());
//                            reLayoutCellLayout(cellLayout) ;
//                        }
//                        else
//                        {
//                            MT_DockWorkSpace docWorkspace = (MT_DockWorkSpace) MT_ComponentBus.getInstance().getActivityComp(R.id.dockWorkSpace, getContext()) ;
//                            docWorkspace.addView(view, docWorkspace.getChildCount()) ;
//                            docWorkspace.resetCellLayout(new int[0]) ;
//                        }
//                        
//                        if(view instanceof MT_BubbleTextView){
//                            ((MT_BubbleTextView)view).startVibrate(mIconCache, 0);
//                        }
//                        
//                        LauncherModel.setSortIconInfo(R.id.folderWorkspace, (int)folderInfo.id);
                        
//                    }
//                }
                
            }
            //之前关闭的文件夹没有删除，还在某个面上
            else
            {
                if(Mogoo_GlobalConfig.LOG_DEBUG)
                {
                    Log.d(TAG, "------------------closeFolder size="+size+" ----------------") ;
                }
                
                //内存中
                //DB中
                folderInfo.addItem(getContext(), info) ;
                //界面上更新图标文件夹图标
                closeFolder.stopVibrate();
                mIconCache.recycle(folderInfo.getIntent().getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
                mIconCache.remove(folderInfo.getIntent().getComponent());
                closeFolder.setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(folderInfo.getIcon(mIconCache)), null, null);
                closeFolder.startVibrate(mIconCache, 0);
            }
        }
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "------------------end ----------------") ;
        }
        
    }
    
    /**
     * 移除图标
     *@author: 张永辉
     *@Date：2011-4-11
     *@param packageName
     */
    void removeItem(String packageName)
    { 
       final String packageNameTemp = packageName ; 
       List<Mogoo_FolderBubbleText> folders = getAllFolder() ;
       for(Mogoo_FolderBubbleText folder : folders)
       {
           Mogoo_FolderInfo folderInfo = (Mogoo_FolderInfo)folder.getTag() ;
           List<ShortcutInfo> items = folderInfo.getContents() ;
           boolean isBreak = false ;
           for(ShortcutInfo item:items)
           {
               if(packageName.equals(item.intent.getComponent().getPackageName()))
               {
                   folderInfo.removeItem(launcher,item) ;
                   folderTemp = folder ;
                   mIconCache.recycle(folderInfo.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
                   mIconCache.remove(folderInfo.intent.getComponent());
                   folder.setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(folderInfo.getIcon(mIconCache)), null, null);
                   folder.setText(folderInfo.title);
                   isBreak = true ;
                   break ;
               }
           }
           
           if(isBreak)
           {
               break ;
           }
       }
        
        final int count = getChildCount();

        post(new Runnable() {
            public void run() {
                final ArrayList<View> childrenToRemove = new ArrayList<View>();
                childrenToRemove.clear();
                int index = -1 ;

                for (int j = 0; j < count; j++) {
                    final View view = getChildAt(j);
                    Object tag = view.getTag();

                    if (tag instanceof ShortcutInfo) {
                        final ShortcutInfo info = (ShortcutInfo) tag;
                        final Intent intent = info.intent;
                        final ComponentName name = intent.getComponent();

                        if (Intent.ACTION_MAIN.equals(intent.getAction()) && name != null) {
                            if (packageNameTemp.equals(name.getPackageName())) {
                                childrenToRemove.add(view);
                                index = j ;
                            }
                        }
                    }
                }

                int childCount = childrenToRemove.size();
                for (int j = 0; j < childCount; j++) {
                    View child = childrenToRemove.get(j);
                    if(index!=-1)
                    {
                        removeViewAt(index);
                    }
                    if (child instanceof DropTarget) {
                        dragController.removeDropTarget((DropTarget) child);
                    }
                }
                
                reLayoutFolderWorkspace() ;
                
                //如果文件夹中没有图标了则同时也删除文件夹
                if(folderTemp!=null)
                {
                    Mogoo_FolderInfo folderInfo = (Mogoo_FolderInfo)folderTemp.getTag() ;
                    if(folderInfo.getContents().size()==0)
                    {
                        //桌面
                        if(folderInfo.container == LauncherSettings.Favorites.CONTAINER_DESKTOP)
                        {
                            Workspace workspace = (Workspace)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.workspace, launcher);
                            CellLayout cellLayout =(CellLayout)workspace.getChildAt(folderInfo.screen) ;
                            cellLayout.removeView(folderTemp) ;
                            workspace.reLayoutCellLayout(cellLayout) ;
                            LauncherModel.setSortIconInfo(R.id.workspace, folderInfo.screen) ;
                            LauncherModel.saveAllIconInfo(launcher) ;
                        }
                        //工具栏
                        else if(folderInfo.container == LauncherSettings.Favorites.CONTAINER_TOOLBAR)
                        {
                            Mogoo_DockWorkSpace dockWorkspace = (Mogoo_DockWorkSpace)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.dockWorkSpace, launcher);
                            dockWorkspace.removeView(folderTemp) ;
                            dockWorkspace.resetCellLayout(new int[]{0}) ;
                            LauncherModel.setSortIconInfo(R.id.dockWorkSpace, -1) ;
                            LauncherModel.saveAllIconInfo(launcher) ;
                        }
                        LauncherModel.deleteItemFromDatabase(launcher, folderInfo) ;
                            
                    }
                }
            }
        });
    }
    
    /**
     * 更新图标信息
     *@author: 张永辉
     *@Date：2011-4-11
     *@param packageNames
     */
    void updateItem(String packageName)
    {
        List<Mogoo_FolderBubbleText> folders = getAllFolder() ;
        for(Mogoo_FolderBubbleText folder : folders)
        {
            Mogoo_FolderInfo folderInfo = (Mogoo_FolderInfo)folder.getTag() ;
            List<ShortcutInfo> items = folderInfo.getContents() ;
            boolean isBreak = false ;
            for(ShortcutInfo item:items)
            {
                if(packageName.equals(item.intent.getComponent().getPackageName()))
                {
                    PackageManager pm = launcher.getPackageManager() ;
                    String title = pm.resolveActivity(item.intent, 0).activityInfo
                        .loadLabel(pm).toString();
                    item.title = title;
                    mIconCache.recycle(item.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL) ;
                    mIconCache.remove(item.intent.getComponent());
                    item.setIcon(mIconCache.getIcon(item.intent));
                    
                    folderInfo.updateItem(launcher,item) ;
                    
                    mIconCache.recycle(folderInfo.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
                    mIconCache.remove(folderInfo.intent.getComponent());
                    folder.setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(folderInfo.getIcon(mIconCache)), null, null);
                    folder.setText(folderInfo.title);
                    isBreak = true ;
                    break ;
                }
            }
            
            if(isBreak)
            {
                break ;
            }
        }
    }
    
    /**
     * 取得所有的文件夹
     *@author: 张永辉
     *@Date：2011-4-11
     *@return
     */
    private List<Mogoo_FolderBubbleText> getAllFolder()
    {
        List<Mogoo_FolderBubbleText> folders = new ArrayList<Mogoo_FolderBubbleText>() ;
        
        Workspace workspace = (Workspace)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.workspace, launcher) ;
        
        int cellLayoutCount = workspace.getChildCount() ; 
        
        for(int i=0;i<cellLayoutCount;i++)
        {
            CellLayout cellLayout = (CellLayout)workspace.getChildAt(i) ;
            int childCount = cellLayout.getChildCount() ;
            
            for(int j=0;j<childCount;j++)
            {
                View child = cellLayout.getChildAt(j) ;
                if(child instanceof Mogoo_FolderBubbleText)
                {
                    folders.add((Mogoo_FolderBubbleText)child) ;
                }
            }
        }
        
        Mogoo_DockWorkSpace dockWorkspace = (Mogoo_DockWorkSpace) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.dockWorkSpace, launcher) ;

        int childCount = dockWorkspace.getChildCount() ;
        
        for(int j=0;j<childCount;j++)
        {
            View child = dockWorkspace.getChildAt(j) ;
            if(child instanceof Mogoo_FolderBubbleText)
            {
                folders.add((Mogoo_FolderBubbleText)child) ;
            }
        }
        
        return folders ;
    }
    
    /**
     * 重整界面
     *@author: 张永辉
     *@Date：2011-4-11
     */
    void reLayoutFolderWorkspace()
    {
        for(int i=0; i < getChildCount();i++)
        {
          View child = getChildAt(i);
          if(child!=null)
          {
              CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
              int[] cellXY = Mogoo_Utilities.convertToCell(i);
              lp.cellX = cellXY[0];
              lp.cellY = cellXY[1];   
          }
        }       
        
        requestLayout() ;
    }
    
    
    /**
     * screen屏及其之后的所有快捷方式屏是否全满屏
     * @ author:张永辉 
     *@param screen
     *@return
     */
    /*private boolean isFullFromCurrentScreen(int screen){
        int [] shortcutScreen = MT_GlobalConfig.getShortcutScreen() ;
        CellLayout layout = (CellLayout)getChildAt(getCurrentScreen());
        
        if(layout.getChildCount() < MT_GlobalConfig.getWorkspaceCellCounts()){
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
                if(layout.getChildCount() < MT_GlobalConfig.getWorkspaceCellCounts())
                {
                    return false ;
                }
            }
        }
        return true ;
    }*/

}
