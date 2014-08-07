/**  
 * 文 件 名:  MT_DockView.java  
 * 描    述： Dock工具栏类 
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者： 魏景春                     
 * 版    本:  1.0  
 * 创建时间:   2010-12-3
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2010-12-3       魏景春       1.0            工具栏类  
 **/

package com.mogoo.launcher2;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.CellEntry.CellEntryInface;
import com.mogoo.launcher2.CellLayout.CellInfo;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_BitmapCache;
import com.mogoo.launcher2.utils.Mogoo_ClearBase;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;

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
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class Mogoo_DockWorkSpace extends ViewGroup implements DropTarget, DragSource, Mogoo_ClearBase, CellEntryInface {
    private static final String TAG = "Launcher.Mogoo_DockWorkSpace";

    private DragController mDragController;
    
    private boolean mPortrait;

    private Launcher mLauncher;

    private Workspace mWorkspace;

    private DragLayer mDragLayer;
    
    private Mogoo_DockWorkSpace dockview;

    private Mogoo_BitmapCache mIconCache;

    private OnLongClickListener mLongClickListener;

    // 默认dock工具栏显示的最大图标数
    private static int maxIconCount = 4;

    private final Rect mRect = new Rect();

    private boolean mDirtyTag;
    
    private Context mContext;
    
    private Handler handler = new Handler();

    //拖动类型
    public final static int DRAG_INSERT = 1;
    public final static int DRAG_DELETE = 2;
    public final static int DRAG_MOVE = 3;

    //单元格宽度
    private int mCellWidth;
    //单元格高度
    private int mCellHeight;

    //Dock工具栏到宽度
    private int mWidthSpecSize;
    //Dock工具栏到宽度
    private int mHeightSpecSize;
    //Dockg工具栏X方向，单元格之间的间距
    private int mWidthGap;
    //Dockg工具栏Y方向，单元格之间的间距
    private int mHeightGap;

    private RectF mDragRect = new RectF();

    int[] mCellXY = new int[2];

    private final CellInfo mCellInfo = new CellInfo();
    
    private CellInfo mDragCellInfo = new CellInfo();

    // Dock工具栏单元格坐标列表
    private ArrayList<CellEntry> dockbarCellCoordinateList = new ArrayList<CellEntry>();

    //判断是否在本区域内拖动
    private boolean isLocaleDrag = false;
    
    private static final long ANIMATION_TIME = 500;
    
    

    public Mogoo_DockWorkSpace(Context context) {
        this(context, null);
    }

    public Mogoo_DockWorkSpace(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Mogoo_DockWorkSpace(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        dockview = this;
        LauncherApplication app = (LauncherApplication) this.getContext().getApplicationContext();
        mIconCache = app.getIconCache();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CellLayout, defStyle, 0);

        mCellWidth = a.getDimensionPixelSize(R.styleable.CellLayout_cellWidth, 75);
        mCellHeight = a.getDimensionPixelSize(R.styleable.CellLayout_cellHeight, 90);
        Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.DOCK_HEIGHT, context.getResources().getDimensionPixelSize(R.dimen.dock_height));
        

        a.recycle();

        setAlwaysDrawnWithCacheEnabled(false);
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

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        // Generate an id for each view, this assumes we have at most 256x256
        // cells
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

    /**
     * 设置拖动控制器
     */
    public void setDragController(DragController dragger) {
        mDragController = dragger;

    }

    // 判断Dock工具栏是否接受拖入到图标
    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) {

        // Log.i(TAG, "acceptDrop----" + System.currentTimeMillis());

        final ItemInfo item = (ItemInfo) dragInfo;

        if (disallowType(item)) {
            return false;
        } else {
            if (getChildCount() < maxIconCount) {
                return true;
            } else if (getChildCount() == maxIconCount && isLocaleDrag) {
                return true;
            } 
            return false; 
        }
    }
    
    private boolean acceptFolderAdd(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo){
        ItemInfo item = (ItemInfo) dragInfo;
        if (disallowType(item)){
            return false;
        }else if (getChildCount() == maxIconCount && haveFolder()){
            return true;
        }
        
        return false;
    }

    private boolean disallowType(ItemInfo item) {
        return item.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET
                || item.itemType == LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER
                || item.itemType == LauncherSettings.Favorites.ITEM_TYPE_WIDGET_PHOTO_FRAME
                || item.itemType == LauncherSettings.Favorites.ITEM_TYPE_WIDGET_SEARCH
                || item.itemType == LauncherSettings.Favorites.ITEM_TYPE_WIDGET_CLOCK;
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) 
    {
    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------call onDragEnter()--------------------------------------"); 
        }    
    	
    	//是否排满且有图标文件夹时
    	boolean result = getChildCount()==maxIconCount && !haveInVisibleChild() && haveFolder() ;
    	
    	Mogoo_FolderController folderController = mLauncher.getFolderController();
        folderController.setCanActive(true) ;
    	
        if (source != this) 
        {
            // 拖入进入区域
            if (!acceptDrop(source, x, y, xOffset, yOffset, dragView, dragInfo)) 
            {
                mDragCellInfo.cell = null;
                return;
            }
            

            if (Mogoo_GlobalConfig.LOG_DEBUG)
            {
                Log.d(TAG, "isLocaleDrag = " + isLocaleDrag + " source = " + source.toString()+" result="+result);
            }
            
            int index = -1 ;
            
            //如果排满且有图标文件夹时
            if(result)
            {
                index = findTargetIndex(dragView, this, DRAG_MOVE) ;
            }
            else
            {
                index = findTargetIndex(dragView, this, DRAG_INSERT);
            }
            
//            //触发文件夹
//            if(index>=MT_GlobalConfig.FOLDER_BASE_INDEX && !(dragInfo instanceof MT_FolderInfo))
//            {
//                folderController.setTempActiveIcon((MT_BubbleTextView) getChildAt(index - MT_GlobalConfig.FOLDER_BASE_INDEX));
//                // 根据dragInfo创建一个临时插入的图标对象
//                View view = createReflectionIcon(dragInfo);
//                mDragCellInfo.cell = view ;
//                mDragCellInfo.cellIndex = index ;
//                isLocaleDrag = true;
//            }
//            else
//            {
                folderController.iconFolderInactive();
                folderController.setTempActiveIcon(null);
                
                //如果排满且有图标文件夹时
                if(result)
                {
                    isLocaleDrag = true;
                    return ;
                }
                else
                {
                    // 根据dragInfo创建一个临时插入的图标对象
                    View view = createReflectionIcon(dragInfo);
                    // 设置接受图标标志
                    if (index == -1) {
                        index = this.getChildCount();
                    }
                    
                    if(index>=Mogoo_GlobalConfig.FOLDER_BASE_INDEX)
                    {
                        index = index - Mogoo_GlobalConfig.FOLDER_BASE_INDEX ;
                    }
                    addView(view, index);
                    
                    setDragInfo(index);
                    //执行排序动画
                    startSortForInsertOrDelete(index, DRAG_INSERT);
                    view.setVisibility(View.INVISIBLE);
                }
                isLocaleDrag = true;
//            }
        } 
        else 
        { 
        	if (Mogoo_GlobalConfig.LOG_DEBUG)
            {
                Log.d(TAG, "isLocaleDrag = " + isLocaleDrag + " source = " + source.toString()+" result="+result);
            }
        	
        	if (isLocaleDrag)
        	{ 	
        		isLocaleDrag = false;
        		int index = -1;
        		
                //如果排满且有图标文件夹时
                if(result)
                {
                    index = findTargetIndex(dragView, this, DRAG_MOVE) ;
                }
                else
                {
                    index = findTargetIndex(dragView, this, DRAG_INSERT);
                }
        		
                
//                //触发文件夹
//                if(index>=MT_GlobalConfig.FOLDER_BASE_INDEX && !(dragInfo instanceof MT_FolderInfo))
//                {
//                    folderController.setTempActiveIcon((MT_BubbleTextView) getChildAt(index - MT_GlobalConfig.FOLDER_BASE_INDEX));
//                    // 根据dragInfo创建一个临时插入的图标对象
//                    View view = createReflectionIcon(dragInfo);
//                    mDragCellInfo.cell = view ;
//                    mDragCellInfo.cellIndex = index ;
//                }
//                else
//                {
                    folderController.iconFolderInactive();
                    folderController.setTempActiveIcon(null);
                    
                    //如果排满且有图标文件夹时
                    if(result)
                    {
                        isLocaleDrag = true;
                        return ;
                    }
                    else
                    {
                        // 根据dragInfo创建一个临时插入的图标对象
                        View view = createReflectionIcon(dragInfo);
                        // 设置接受图标标志
                        if (index == -1 || index > this.getChildCount()) {
                            index = this.getChildCount();
                        }
                        
//                        if(index>=MT_GlobalConfig.FOLDER_BASE_INDEX)
//                        {
//                            index = index - MT_GlobalConfig.FOLDER_BASE_INDEX ;
//                        }
                        
                        addView(view, index);
                        setDragInfo(index);
                        //执行排序动画
                        startSortForInsertOrDelete(index, DRAG_INSERT);
                        view.setVisibility(View.INVISIBLE);
                    }
//                }
        	}
            isLocaleDrag = true;
        }
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "-------------------------end--------------------------------------"); 
        }

    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) 
    {
    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------call onDragOver()--------------------------------------"); 
        }     
    	
    	//当有文件夹打开时，随便在这上面移动都不会响应
    	Mogoo_FolderWorkspace folderWorkspace = (Mogoo_FolderWorkspace) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.folderWorkspace, mLauncher) ;
    	
    	if(folderWorkspace.getLoadingFolder()!=null)
    	{
    	    return ;
    	}
    	
        if (!isLocaleDrag) {
            return;
        }

        if (mDragCellInfo == null || mDragCellInfo.cell == null) {
            //如果排满且有图标文件夹时
            if(getChildCount()==maxIconCount && haveFolder())
            {
            }
            else
            {
                return;
            }
        }

        int endIndex = findTargetIndex(dragView, this);
        
        Mogoo_FolderController folderController = mLauncher.getFolderController();
        folderController.setCanActive(true) ;
        
        //触发文件夹
        if(endIndex>=Mogoo_GlobalConfig.FOLDER_BASE_INDEX && !(dragInfo instanceof Mogoo_FolderInfo)){
            folderController.setTempActiveIcon((Mogoo_BubbleTextView) getChildAt(endIndex - Mogoo_GlobalConfig.FOLDER_BASE_INDEX));
            mDragCellInfo.cellIndex = endIndex ;
            
            //排满且有图标文件夹，但enter时没有生成副本
            if(mDragCellInfo.cell == null)
            {
                View view = createReflectionIcon(dragInfo) ;
                mDragCellInfo.cell = view ;
            }
        }
        else
        {
            folderController.iconFolderInactive();
            folderController.setTempActiveIcon(null);
            
            //如果排满且有图标文件夹时
            if(getChildCount()==maxIconCount && !haveInVisibleChild() && haveFolder())
            {
                return ;
            }
            else
            {
                if (mDragController.sortView(this, mDragCellInfo.cellIndex, endIndex)) 
                {
                    setDragInfo(endIndex);
                }
            }
            
        }
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "-------------------------end--------------------------------------"); 
        }
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) {

    }

    /**
     * 拖动释放事件
     */
    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset, DragView dragView, Object dragInfo) 
    {
    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------call onDrop()--------------------------------------"); 
        }  
    	
    	Mogoo_FolderController folderController = mLauncher.getFolderController();
        folderController.setCanActive(false);
//        folderController.iconFolderInactive();
        
        //如果文件打开了的话，则放手后图标也要加入文件夹中
        Mogoo_FolderWorkspace folderWorkspace = (Mogoo_FolderWorkspace) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.folderWorkspace, mLauncher) ;
        
        if(folderWorkspace.getLoadingFolder()!=null)
        {
            //如果文件夹中接收，则放入文件夹中
            if(folderWorkspace.acceptDrop(source, x, y, xOffset, yOffset, dragView, dragInfo))
            {
                Mogoo_FolderInfo info = (Mogoo_FolderInfo)folderWorkspace.getLoadingFolder().getTag();
                info.addItem(mLauncher, (ShortcutInfo)dragInfo) ;
                
                if(mDragCellInfo.cell != null && mDragCellInfo.cell.getParent() instanceof Mogoo_DockWorkSpace){
                    ShortcutInfo shortcutInfo = (ShortcutInfo) mDragCellInfo.cell.getTag();
                    mIconCache.recycle(shortcutInfo.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
                    ((Mogoo_BubbleTextView) mDragCellInfo.cell).setReflection(false);
                    ((Mogoo_BubbleTextView) mDragCellInfo.cell).setIconReflection(null);
                }
                
                View view = folderWorkspace.createFolderItem(dragInfo) ;
                
                folderWorkspace.addView(view, false) ; 
                if(view instanceof Mogoo_BubbleTextView){
                    ((Mogoo_BubbleTextView) view).startVibrate(mIconCache, 0);
                }
                
                //有副本
                if(isChild(mDragCellInfo.cell))
                {
                    //移除
                    removeView(mDragCellInfo.cell) ;
                    resetCellLayout(new int []{0}) ;
                }
            }
            //不接收，则还原
            else
            {
                dropNormal(source, dragInfo) ;
            }
        }
        else
        {
            isLocaleDrag = false; 
            if (mDragCellInfo != null && mDragCellInfo.cell != null) 
            {
                 if (mDragCellInfo.cellIndex != -1) 
                 {
                     if(Mogoo_GlobalConfig.LOG_DEBUG)
                     {
                         Log.d(TAG, "-------------------------mDragCellInfo.cellIndex!=-1---------------------------"); 
                     }
                     
                     if(mDragCellInfo.cellIndex >= Mogoo_GlobalConfig.FOLDER_BASE_INDEX && folderController.getLastActiveIcon() != null){
                            //图标文件夹流程
                            folderController.iconFolderInactive();
                            
                            //如果目标文件夹排満了
                            if(targetFolderIsFull(mDragCellInfo.cellIndex))
                            {
                                dropNormal(source, dragInfo) ;
                            }
                            else
                            {
                                if(source instanceof Mogoo_DockWorkSpace){
                                    ShortcutInfo shortcutInfo = (ShortcutInfo) mDragCellInfo.cell.getTag();
                                    mIconCache.recycle(shortcutInfo.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
                                    ((Mogoo_BubbleTextView) mDragCellInfo.cell).setReflection(false);
                                    ((Mogoo_BubbleTextView) mDragCellInfo.cell).setIconReflection(null);
                                }
                                
                                
                                View mergeFolder = folderController.replaceIcon2Folder(this, mDragCellInfo.cell, -1, mDragCellInfo.cellIndex - Mogoo_GlobalConfig.FOLDER_BASE_INDEX);
                                
                                if(mergeFolder == null){
                                    folderController.iconFolderInactive() ;
                                    return;
                                }
                                
                                folderController.iconFolderInactive() ;
                                
//                                mDragController.sortView(this, mDragCellInfo.cellIndex, this.getChildCount() - 1);
                                removeView(mDragCellInfo.cell);
                                
                                if(!isDockFull()){
                                    this.startSortForInsertOrDelete(mDragCellInfo.cellIndex - Mogoo_GlobalConfig.FOLDER_BASE_INDEX,  DRAG_DELETE);
                                }
                                
                                if (mDragCellInfo.cell instanceof DropTarget) {
                                    mDragController.removeDropTarget((DropTarget) mDragCellInfo.cell);
                                }
                                ((Mogoo_BubbleTextView)mergeFolder).startVibrate(mIconCache, 0);
                            }
                     }
                     else
                     {
                         folderController.iconFolderInactive();
                         dropNormal(source, dragInfo) ;
                     }
                     
                 }    
                 else
                 {
                     if(Mogoo_GlobalConfig.LOG_DEBUG)
                     {
                         Log.d(TAG, "-------------------------mDragCellInfo.cellIndex=-1---------------------------"); 
                     }
                     folderController.iconFolderInactive();
                     dropNormal(source, dragInfo) ;
                 }
            }
            else
            {
                
                if(Mogoo_GlobalConfig.LOG_DEBUG)
                {
                    Log.d(TAG, "-------------------------mDragCellInfo.cell=null---------------------------"); 
                }
                //恢复文件夹
                folderWorkspace.restoreFolder((ShortcutInfo)dragInfo) ;
            }
            
        }
        
        LauncherModel.setSortIconInfo(R.id.dockWorkSpace, -1);
       	
        resetDragInfo();
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "-------------------------end--------------------------------------"); 
        }
    }
    
    private boolean isDockFull(){
        if(getChildCount() < maxIconCount){
            return false;
        }
        
        for(int i = 0; i < maxIconCount; i++){
            if(getChildAt(i).getVisibility() != View.VISIBLE){
                return false;
            }
        }
        
        return true;
    }
    
    /**
     *
     *@author: 张永辉
     *@Date：2011-4-6
     *@param dragInfo
     */
    private void dropNormal(DragSource source, Object dragInfo)
    {
        ShortcutInfo info = (ShortcutInfo)dragInfo ;
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "------------------------call dropNormal()---------mDragCellInfo.cell="+mDragCellInfo.cell+"-info.container="+info.container); 
        }
        
        //有副本
        if(isChild(mDragCellInfo.cell))
        {
            if(Mogoo_GlobalConfig.LOG_DEBUG)
            {
                Log.d(TAG, "------------------------isChild---------------------------"); 
            }
            
            mDragCellInfo.cell.setVisibility(VISIBLE);
            if (mDragCellInfo.cell instanceof Mogoo_BubbleTextView) 
            {
                ((Mogoo_BubbleTextView) mDragCellInfo.cell).startVibrate(mIconCache, 0);
            } 
        }
        //无副本
        else
        {
            if(source instanceof Workspace)
            {
                if(Mogoo_GlobalConfig.LOG_DEBUG)
                {
                    Log.d(TAG, "------------------------CONTAINER_DESKTOP---------------------------"); 
                }
                
                Workspace workspace = (Workspace) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.workspace, mLauncher) ;
                
                View view = workspace.createIcon(dragInfo) ;
                if(view!=null)
                {
                    workspace.addView((CellLayout)workspace.getChildAt(info.screen), view, false) ;
                    ((Mogoo_BubbleTextView) view).startVibrate(mIconCache, 0);
                    LauncherModel.setSortIconInfo(R.id.workspace, info.screen);
                }
            }
            else if(source instanceof Mogoo_DockWorkSpace)
            {
                if(Mogoo_GlobalConfig.LOG_DEBUG)
                {
                    Log.d(TAG, "------------------------CONTAINER_TOOLBAR---------------------------"); 
                }
                
                View view = createReflectionIcon(dragInfo) ;
                if(view!=null)
                {
                    addView(view, getChildCount()) ;
                    startSortForInsertOrDelete(getChildCount()-1,  DRAG_INSERT);
                    ((Mogoo_BubbleTextView) view).startVibrate(mIconCache, 0);
                }
            }
            //文件夹内
            else
            {
                //恢复文件夹
                Mogoo_FolderWorkspace folderWorkspace = (Mogoo_FolderWorkspace) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.folderWorkspace, mLauncher) ;
                folderWorkspace.restoreFolder(info) ;
            }
        }
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "------------------------end---------------------------"); 
        }
        
    }
    
    /**
     * 拖动释放结束事件
     */
    public void onDropCompleted(View target, boolean success)
    {
        // Log.i(TAG, "onDropCompleted----" + System.currentTimeMillis());
    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------call onDropCompleted()--------------------------------------"); 
            Log.d(TAG, "success = " + success);
        } 
    	
        if (success) {
            if (target != this && mDragCellInfo != null && mDragCellInfo.cell !=null) {
                removeView(mDragCellInfo.cell);
                
                if (mDragCellInfo.cell instanceof DropTarget) {
                    Log.i(TAG,
                            "mDragInfo.cell instanceof DropTarget------------------------------------"
                                    + "true");
                    mDragController.removeDropTarget((DropTarget) mDragCellInfo.cell);
                }
                
                //重整Dock工具栏布局
                resetCellLayout(new int[]{0,0});
                
                LauncherModel.setSortIconInfo(R.id.dockWorkSpace, -1);
            }
        } else {
            if (mDragCellInfo != null) {
                if (mDragCellInfo.cell != null) {
                    invalidate();
                }
            }
        }        
        
        isLocaleDrag = false;
        resetDragInfo();
        LauncherModel.saveAllIconInfo(getContext());
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "-------------------------end--------------------------------------"); 
        }
        // mDragCellInfo = null;
    }

    public Rect estimateDropLocation(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo, Rect recycle) {
        // TODO Auto-generated method stub
        return null;
    }

    public void setOnLongClickListener(OnLongClickListener l) {
        // Log.i(TAG, "setOnLongClickListener--" + System.currentTimeMillis());
        mLongClickListener = l;
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).setOnLongClickListener(l);
        }
    }

    public void setLauncher(Launcher launcher) {
        this.mLauncher = launcher;
    }

    public void setDragger(DragLayer dragLayer) {
        this.mDragLayer = dragLayer;
    }

    public void setWorkspace(Workspace workspace) {
        this.mWorkspace = workspace;
    }

    private void setDragInfo(int index) {
        mDragCellInfo.cell = getChildAt(index);
        mDragCellInfo.cellIndex = index;
        mDragCellInfo.cellX = index;
        mDragCellInfo.cellY = 0;
    }

    private void resetDragInfo() {
        mDragCellInfo.cell = null;
        mDragCellInfo.cellIndex = -1;
        mDragCellInfo.cellX = -1;
        mDragCellInfo.cellY = -1;
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "--------------------call onInterceptTouchEvent()--------------------------------------"); 
        }
        // Log.i(TAG, "onInterceptTouchEvent----" + System.currentTimeMillis());

        final int action = ev.getAction();
        final CellInfo cellInfo = mCellInfo;

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
                        cellInfo.cell = child;
                        cellInfo.cellX = child.getScrollX();
                        cellInfo.cellY = child.getScrollY();
                        cellInfo.spanX = child.getWidth() + 1;
                        cellInfo.spanY = child.getHeight() + 1;
                        cellInfo.valid = true;
                        cellInfo.containter = R.id.dockWorkSpace;
                        cellInfo.cellIndex = i;
                        found = true;
                        mDirtyTag = false;

                        ItemInfo info = (ItemInfo) cellInfo.cell.getTag();
                        if (info != null) {
                            info.cellX = i;
                            // info.setAdditionInfo(dragIconType);
                        }

                        break;
                    }
                }
            }

            if (!found) {
                cellInfo.cell = null;
                cellInfo.cellX = -1;
                cellInfo.cellY = -1;
                cellInfo.spanX = 0;
                cellInfo.spanY = 0;
                cellInfo.valid = false;
                cellInfo.containter = -1;
                cellInfo.cellIndex = -1;
                mDirtyTag = false;
            }

            setTag(cellInfo);
        } else if (action == MotionEvent.ACTION_UP) {
            cellInfo.cell = null;
            cellInfo.cellX = -1;
            cellInfo.cellY = -1;
            cellInfo.spanX = 0;
            cellInfo.spanY = 0;
            cellInfo.valid = false;
            cellInfo.containter = -1;
            cellInfo.cellIndex = -1;
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
        return info;
    }

    /**
     * 设置dock工具栏显示的最大图标数 @ author: 魏景春 *
     * 
     * @param count 显示的最大图标数
     */
    public void setMaxIconCount(int count) {
        maxIconCount = count;
    }

    /**
     * 拖动工具栏上的图标 
     * @ author: 魏景春
     * @param cellInfo 拖动图标信息
     */
    public void startDrag(CellLayout.CellInfo cellInfo) 
    {
        View child = cellInfo.cell;

        // Make sure the drag was started by a long press as opposed to a long click.
        if (!child.isInTouchMode()) {
            return;
        }

        //mDragCellInfo = cellInfo;        
        setDragInfo(cellInfo.cellIndex);  
        
        mDragController.startDrag2(cellInfo, child, this, child.getTag(),DragController.DRAG_ACTION_MOVE);
        mDragCellInfo.cell.setVisibility(INVISIBLE);
        invalidate();
    }


    public void onClear() {
        mDragController = null;
        mLauncher = null;
        mWorkspace = null;
        mDragLayer = null;
        mContext = null;
        mIconCache = null;
        dockview = null;
    }

    int getCellWidth() {
        return mCellWidth;
    }

    int getCellHeight() {
        return mCellHeight;
    }

    int getLeftPadding() {
        return Mogoo_GlobalConfig.isPortrait() ? Mogoo_GlobalConfig.getWorkspaceShortAxisStartPadding()
                : Mogoo_GlobalConfig.getWorkspaceLongAxisStartPadding();

    }

    int getTopPadding() {
        return Mogoo_GlobalConfig.isPortrait() ? Mogoo_GlobalConfig.getWorkspaceLongAxisStartPadding()
                : Mogoo_GlobalConfig.getWorkspaceShortAxisStartPadding();
    }

    int getRightPadding() {
        return Mogoo_GlobalConfig.isPortrait() ? Mogoo_GlobalConfig.getWorkspaceShortAxisEndPadding()
                : Mogoo_GlobalConfig.getWorkspaceLongAxisEndPadding();
    }

    int getBottomPadding() {
        return Mogoo_GlobalConfig.isPortrait() ? Mogoo_GlobalConfig.getWorkspaceLongAxisEndPadding()
                : Mogoo_GlobalConfig.getWorkspaceShortAxisEndPadding();
    }

    int getLeftStartX(int cellCount) {
        int leftX = 0;
        if (cellCount > 0) {
            int distance = (Mogoo_GlobalConfig.getScreenWidth() - getLeftPadding() - getRightPadding() - (mCellWidth
                    * cellCount + mWidthGap * (cellCount - 1))) / 2;
            leftX = distance;
        }

        return leftX;

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // TODO: currently ignoring padding
        
        int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
        mWidthSpecSize = MeasureSpec.getSize(widthMeasureSpec);

        int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
        mHeightSpecSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthSpecMode == MeasureSpec.UNSPECIFIED || heightSpecMode == MeasureSpec.UNSPECIFIED) {
            throw new RuntimeException("CellLayout cannot have UNSPECIFIED dimensions");
        }

        final int cellWidth = mCellWidth;
        final int cellHeight = mCellHeight;

//         mPortrait = mHeightSpecSize < mWidthSpecSize;
//         
//         if(!mPortrait){
//        	 requestLayout();
//        	 invalidate();
//        	 return;
//         }

        int numHGaps = Mogoo_GlobalConfig.getDockMaxIconCount() - 1;
        int numVGaps = 0;

        int vSpaceLeft = mHeightSpecSize - getLeftPadding() - getRightPadding() - (cellHeight * 0);
        if (numVGaps > 0) {
            mHeightGap = vSpaceLeft / numVGaps;
        } else {
            mHeightGap = 0;
        }

        int hSpaceLeft = mWidthSpecSize - getLeftPadding() - getRightPadding()
                - (cellWidth * Mogoo_GlobalConfig.getDockMaxIconCount());

        if (numHGaps > 0) {
            mWidthGap = hSpaceLeft / numHGaps;
        } else {
            mWidthGap = 0;
        }

        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
            lp.setup(cellWidth, cellHeight, mWidthGap, mHeightGap, getLeftPadding(),
                    getTopPadding());

            if (lp.regenerateId) {
                child.setId(((getId() & 0xFF) << 16) | (lp.cellX & 0xFF) << 8 | (lp.cellY & 0xFF));
                lp.regenerateId = false;
            }

            int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
            int childheightMeasureSpec = MeasureSpec
                    .makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
            child.measure(childWidthMeasureSpec, childheightMeasureSpec);
        }

        setMeasuredDimension(mWidthSpecSize, mHeightSpecSize);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int count = getChildCount();
        int leftStartX = getLeftStartX(count);
        
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();

                int childLeft = leftStartX + lp.x;
                int childTop = lp.y;
                child.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height);

                if (lp.dropped) {
                    lp.dropped = false;
                }
            }
        }

        // 初始化单元个坐标列表
        initCellCoordinateList(count);

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
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        // cellToRect(cellXY[0], cellXY[1], lp.cellHSpan, lp.cellVSpan,
        // mDragRect);
        invalidate();
    }

    /**
     * Computes a bounding rectangle for a range of cells
     * 
     * @param cellX X coordinate of upper left corner expressed as a cell
     *            position
     * @param cellY Y coordinate of upper left corner expressed as a cell
     *            position
     * @param cellHSpan Width in cells
     * @param cellVSpan Height in cells
     * @param dragRect Rectnagle into which to put the results
     */
    public void cellToRect(int cellX, int cellY, int cellHSpan, int cellVSpan, RectF dragRect) {
        // final boolean portrait = mPortrait;
        // final int cellWidth = mCellWidth;
        // final int cellHeight = mCellHeight;
        // final int widthGap = mWidthGap;
        // final int heightGap = mHeightGap;
        //
        // final int hStartPadding = portrait ? mShortAxisStartPadding :
        // mLongAxisStartPadding;
        // final int vStartPadding = portrait ? mLongAxisStartPadding :
        // mShortAxisStartPadding;
        //
        // int width = cellHSpan * cellWidth + ((cellHSpan - 1) * widthGap);
        // int height = cellVSpan * cellHeight + ((cellVSpan - 1) * heightGap);
        //
        // int x = hStartPadding + cellX * (cellWidth + widthGap);
        // int y = vStartPadding + cellY * (cellHeight + heightGap);
        //
        // dragRect.set(x, y, x + width, y + height);
    }

    /**
     * Computes the required horizontal and vertical cell spans to always fit
     * the given rectangle.
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

        return new int[] {
                spanX, spanY
        };
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



    /**
     * 启动时加载工具栏图标 @ author: 魏景春
     * 
     * @param shortcuts 图标快捷键数组
     */
    public void loadToolbarItems(ArrayList<ItemInfo> shortcuts) {
        
    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------start loadToolbarItems()--------------------------------------"); 
        }
    	
    	ArrayList<ItemInfo> items = shortcuts;
        View view = null;
        int itemSize = items.size();
        
        if (Mogoo_GlobalConfig.LOG_DEBUG) 
        {
            Log.d(TAG, " shortcuts items size = " + itemSize);
        }
        
        if (items != null && itemSize > 0) {
            for (int i = 0; i < itemSize; i++) {
                ItemInfo info = (ItemInfo) items.get(i);
                view = createReflectionIcon(info);

                if (view != null && info.cellX < maxIconCount) {
                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) view.getLayoutParams();
                    if (lp == null) {
                        lp = new CellLayout.LayoutParams(info.cellX, 0, 1, 1);
                    } else {
                        lp.cellX = info.cellX;
                        lp.cellY = 0;
                        lp.cellHSpan = 1;
                        lp.cellVSpan = 1;
                    }

                    if (info.cellX < getChildCount()) {
                        addView(view, info.cellX, lp);
                    } else {
                        addView(view, -1, lp);
                    }
                }
            }
        }
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------------- end --------------------------------------"); 
        }
    }


    public void onDropTargetChange(DragSource source, DropTarget dropTarget, DragView dragView,
            Object dragInfo) {

    	if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------call onDropTargetChange()--------------------------------------"); 
        	Log.d(TAG, "mDragInfo.cellIndex = " + mDragCellInfo.cellIndex);
        }    	

        //如果拖动源不是本区域，则将isLocaleDrag置为false
        if (source !=this)
        {
        	isLocaleDrag = false;
        }
        
        if (dropTarget != this && mDragCellInfo != null && mDragCellInfo.cell != null) 
        {
            ShortcutInfo info = (ShortcutInfo) mDragCellInfo.cell.getTag();
            mIconCache.recycle(info.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
//            mIconCache.remove(info.intent.getComponent());
            removeView(mDragCellInfo.cell);

            this.startSortForInsertOrDelete(mDragCellInfo.cellIndex,  DRAG_DELETE);
            
            if (mDragCellInfo.cell instanceof DropTarget) {
                Log.i(TAG,"mDragInfo.cell instanceof DropTarget------------------------------------" + "true");
                mDragController.removeDropTarget((DropTarget) mDragCellInfo.cell);
            }
            LauncherModel.setSortIconInfo(R.id.dockWorkSpace, -1);
        }
        
        resetDragInfo();

        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "-------------------------end--------------------------------------"); 
        }
    }

	public void onRestoreDragIcon(final Object dragInfo) 
	{
		if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------call onRestoreDragIcon()--------------------------------------"); 
        } 
		
		Mogoo_FolderController folderController = mLauncher.getFolderController();
        folderController.setCanActive(false);
		
		if (dragInfo !=null)
		{
            handler.postDelayed(new Runnable() {
                public void run() {
                    // 根据dragInfo创建一个临时插入的图标对象
                    View view = createReflectionIcon(dragInfo);
                    // 将回复到图标插入到最后一个位置点
                    int index = getChildCount();
                    addView(view, index);

                    // mDragCellInfo.cell.setVisibility(VISIBLE);

                    if (view instanceof Mogoo_BubbleTextView) {
                        ((Mogoo_BubbleTextView) view).startVibrate(mIconCache, 0);
                    }

                    // 执行排序动画
                    startSortForInsertOrDelete(index, DRAG_INSERT);
                    isLocaleDrag = false;
                    LauncherModel.setSortIconInfo(R.id.dockWorkSpace, -1);
                    LauncherModel.saveAllIconInfo(getContext());
                }
            }, 100);
		}
		
		if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "-------------------------end--------------------------------------"); 
        }
	}
	
    /**
     * 识别排序插入点index
     */
    public int findTargetIndex(DragView dragView, ViewGroup parent) {    	
        return findTargetIndex(dragView, parent, DRAG_MOVE);       
        
    }

    /**
     * 识别排序插入点index
     */
    public int findTargetIndex(DragView dragView, ViewGroup parent, int dragType) {
        int targetIndex = -1;

        switch (dragType) {
            case DRAG_INSERT:
                targetIndex = findInsertIndex(dragView, parent);
                break;
            case DRAG_MOVE:
                targetIndex = findMoveIndex(dragView, parent, true);
                break;

            case DRAG_DELETE:
                break;
        }

        return targetIndex;
    }

    public int getDockWorkSpaceCellCount() {
        return dockbarCellCoordinateList != null ? dockbarCellCoordinateList.size() : 0;
    }
    
    /**
     * 创建倒影图标 
     * @ author: 魏景春 
     * @param dragInfo 拖动图标信息
     * @return 图标视图
     */
    View createReflectionIcon(Object dragInfo) {
        View view;
        ItemInfo info = (ItemInfo) dragInfo;

        switch (info.itemType) {
            case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
            case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
                if (info.container == NO_ID && info instanceof ApplicationInfo) {
                    // Came from all apps -- make a copy
                    info = new ShortcutInfo((ApplicationInfo) info);
                }
                info.container = LauncherSettings.Favorites.CONTAINER_TOOLBAR;
                view = mLauncher.createShortcut(R.layout.mogoo_dock_application, dockview,
                        (ShortcutInfo) info);
                
                break;
            case LauncherSettings.Favorites.ITEM_TYPE_MOGOO_FOLDER:
                view = mLauncher.createFolder(R.layout.mogoo_dock_application_folder, dockview, (ShortcutInfo) info) ;
                break ;
            case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
                view = null;
                break;

            default:
                throw new IllegalStateException("Unknown item type: " + info.itemType);
        }

        if (view == null)
            return null;

        // 生成快捷键图标之前，清空图标缓冲区中该图标过时的图标位图
        mIconCache.recycle(((ShortcutInfo) info).intent.getComponent(),
                Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
        ((ShortcutInfo) info).setIconReflection(null);

        Bitmap bitmap = ((ShortcutInfo) info).getIconReflection(mIconCache);

        ((Mogoo_BubbleTextView) view).setReflection(true);
        ((Mogoo_BubbleTextView) view).setIconReflection(bitmap);

        view.setHapticFeedbackEnabled(false);
        view.setOnLongClickListener(mLongClickListener);

        if (view instanceof DropTarget) {
            mDragController.addDropTarget((DropTarget) view);
        }
        
        if(view instanceof Mogoo_BubbleTextView){
            ((ShortcutInfo)(view.getTag())).container = LauncherSettings.Favorites.CONTAINER_TOOLBAR;
        }
        
        return view;
    }
    
    /**
     * 移除工具栏上过时的图标
     */
    private void removeOldChild(View view) {
        if (view != null) {
            int index = dockview.indexOfChild(view);
            if (index >= 0)
                dockview.removeViewAt(index);
        }
    }

    /**
     * 获取Invisible对象的索引号
     */
    private int getInvisibleChildIndex() {
        int index = -1;
        for (int i = 0; i < this.getChildCount(); i++) {
            if (getChildAt(i).getVisibility() == INVISIBLE) {
                index = i;
                break;
            }
        }
        return index;
    }

    /**
     * 判断是否有Invisible对象
     */
    private boolean isInvisibleChild() {
        boolean flag = false;
        for (int i = 0; i < this.getChildCount(); i++) {
            if (getChildAt(i).getVisibility() == INVISIBLE) {
                flag = true;
                break;
            }
        }
        return flag;
    }
    
 

    /**
     * 插入(或者删除)排序动画
     * @ author: 魏景春
     *@param index 插入(或者删除)图标的索引号
     *@param dragType 拖动类型(dragType= DRAG_INSERT 插入排序；dragType= DRAG_DELETE删除排序）
     */
    public void startSortForInsertOrDelete(int index, int dragType)
    {
    	 View child = null;
         int cellCount = this.getChildCount();
         
         AnimationSet animationSet = new AnimationSet(false);
         Animation animation =  null;
         CellLayout.LayoutParams lp = null;
         int moveX = 0;
         
         switch (dragType)
         {        
         	case DRAG_INSERT:
         		for (int i = 0; i < cellCount; i++)
         		{
         			animation = null;
         			child = this.getChildAt(i);
         			lp = (CellLayout.LayoutParams) child.getLayoutParams();
        	            
         			if (Mogoo_GlobalConfig.PLAY_ANIMATION) 
         			{
         				moveX = lp.leftMargin + child.getWidth()/2;
            	            
         				if(i < index){
         					animation = animationSetting(this,child,moveX);
         				}else if(i > index)
         				{
         					animation = animationSetting(this,child,-moveX);
         				}
            	            
         				if(animation != null)
         				{         					
             				animationSet.addAnimation(animation);
         				} 

         			}
         			
         			if (child != null) 
         			{
         				lp.cellX = i;
         				lp.cellY = 0;
         			}
         		}
         		
        	    this.requestLayout();
         		//invalidate();
        	    if (Mogoo_GlobalConfig.PLAY_ANIMATION) 
     			{
        	    	animationSet.start();
     			}
         		
         		break;
           case DRAG_MOVE:
               	break;
           case DRAG_DELETE:
         	  	for (int i = 0; i < cellCount; i++) 
         	  	{
         	  		animation = null;
         	  		child = this.getChildAt(i);
         	  		lp = (CellLayout.LayoutParams) child.getLayoutParams();
     	            
         	  		if (Mogoo_GlobalConfig.PLAY_ANIMATION) 
         	  		{
         	  			moveX = lp.leftMargin + child.getWidth()/2;
         	            
         	  			if(i < index){
         	  				animation = animationSetting(this,child,-moveX);
         	  			}else if(i >= index)
         	  			{
         	  				animation = animationSetting(this,child,moveX);
         	  			}
         	            
         	  			if(animation != null)
         	  			{         					
         	  				animationSet.addAnimation(animation);
         	  			} 

         	  		}
      			
         	  		if (child != null) 
         	  		{
         	  			lp.cellX = i;
         	  			lp.cellY = 0;
         	  		}
         	  	}
         	  	this.requestLayout();
         	  	//invalidate();
         	  	if (Mogoo_GlobalConfig.PLAY_ANIMATION) 
         	  	{
         	  		animationSet.start();
         	  	}
       	       break;
         }
    }
    
    /**
     * 重置工具栏单元格布局
     * 
     * @param startCellXY 起始的单元
     */
    void resetCellLayout(final int[] startCellXY) 
    { 
//        handler.postDelayed(new Runnable() {
//            public void run() {
                View child = null;
                int cellCount = getChildCount();
                CellLayout.LayoutParams lp = null;
                
                for (int i = startCellXY[0]; i < cellCount; i++) 
                {
                    child = getChildAt(i);
                    lp = (CellLayout.LayoutParams) child.getLayoutParams();
                    if (child != null) 
                    {
                           lp.cellX = i;
                           lp.cellY = 0;
                    }  
                }
                requestLayout();
                invalidate();
//            }
//        }, 100);
    }    
 
    
    // 设定图标动画
    private Animation animationSetting(ViewGroup parent, final View view, int moveX) {
        if (view == null ) {
            return null;
        }

        TranslateAnimation	animation = new TranslateAnimation(moveX, 0, 0, 0);

        animation.setDuration(ANIMATION_TIME);
        animation.setFillAfter(true);
        animation.setFillBefore(true);
        animation.setAnimationListener(new AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                view.clearAnimation();               
            }

            public void onAnimationRepeat(Animation animation) {
            }
        });

        view.setAnimation(animation);
        return animation;
    }
    
    
    /**
     * 识别排序插入点index
     *@author: 张永辉
     *@param dragView
     *@param parent
     *@param run
     *@return
     */
    private int findMoveIndex(final DragView dragView, final ViewGroup parent, boolean run) {
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
        	Log.d(TAG, "--------------------call findMoveIndex()--------------------------------------");        	 
        }
        
        int targetIndex = -1;
        // 拖动图标左上角的坐标
        int x = dragView.getScreenLeft();
        int y = dragView.getScreenTop() - getScreenTop();
        int width = dragView.getWidth();
        int height = dragView.getHeight();

        int count = this.getDockWorkSpaceCellCount();

        CellEntry first = null;
        CellEntry last = null;

        try {
            // 取得第一个单元格坐标
            first = this.getCellEntry(0);
            // 取得第最后一个单元格坐标
            last = this.getCellEntry(count - 1);
        } catch (Exception e) {
            return -1;
        }

        // 如果移入的图标在第一个单元格的左边，则返回0
        if ((x + width) <= first.left) {
            targetIndex = 0;
        }
        // 如果移入的图标在最后一个单元格的右边，则返回count-1
        else if (x >= last.right) {
            targetIndex = count - 1;
        } else {
            int leftTopIndex = this.getCellIndexByCoordinate(x, y); // 左上角所在单元格
            int leftBottomIndex = this.getCellIndexByCoordinate(x, y + height);// 左下角所在单元格
            int rightTopIndex = this.getCellIndexByCoordinate(x + width, y); // 右上角所在单元格
            int rightBottomIndex = this.getCellIndexByCoordinate(x + width, y + height);// 右下角所在的单元格
            
            if(Mogoo_GlobalConfig.LOG_DEBUG)
            {        
                Log.d(TAG, "leftTopIndex = "+leftTopIndex+", leftBottomIndex = "+leftBottomIndex+", rightTopIndex = "+rightTopIndex+", rightBottomIndex="+rightBottomIndex) ;
            }
            
            // 如果拖动图标四个角都不在单元格内
            if (leftTopIndex == -1 && leftBottomIndex == -1 && rightTopIndex == -1
                    && rightBottomIndex == -1) {
                int leftMiddleIndex = this.getCellIndexByCoordinate(x, y + height / 2);
                int rightMiddleIndex = this.getCellIndexByCoordinate(x + width, y + height / 2);
                
                if(Mogoo_GlobalConfig.LOG_DEBUG)
                {        
                    Log.d(TAG, "leftMiddleIndex = "+leftMiddleIndex+", rightMiddleIndex = "+rightMiddleIndex) ;
                }
                
                // 不与任单元格相交或拖动图标包含了整个单元格
                if (leftMiddleIndex == -1 && rightMiddleIndex == -1) {
                    int middleIndex = this.getCellIndexByCoordinate(x+width/2, y + height / 2);
                    //拖动图标包含了整个单元格
                    if(middleIndex!=-1){
                        //文件夹功能
//                        if(MT_GlobalConfig.ICON_FOLDER && getTargetType(middleIndex)==MT_GlobalConfig.TARGET_FOLDER){
//                            targetIndex = middleIndex + MT_GlobalConfig.FOLDER_BASE_INDEX ;
//                        }else{
                            targetIndex = middleIndex ;
//                        }
                    //不与任单元格相交
                    }else{
                        targetIndex = count - 1;
                    }
                }
                // 如果只与一个单元格相交,看左右边哪边离中线远
                else if (leftMiddleIndex == rightMiddleIndex) {
                    //文件夹功能
//                    if(MT_GlobalConfig.ICON_FOLDER  && getTargetType(leftMiddleIndex)==MT_GlobalConfig.TARGET_FOLDER){
//                        targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + leftMiddleIndex ;
//                    }
//                    else
//                    {
                        CellEntry entry = this.getCellEntry(leftMiddleIndex);
                        int leftPart = entry.left + (entry.right - entry.left) / 2 - x;
                        int rightPart = x + width - entry.left - (entry.right - entry.left) / 2;
                        // 左部分大于等于右部分
                        if (leftPart >= rightPart) {
                            targetIndex = leftMiddleIndex;
                        }
                        // 如果右部分大于左部分
                        else {
                            //update by 张永辉 2011-7-21
//                            targetIndex = leftMiddleIndex + 1;
                            targetIndex = leftMiddleIndex ;
                            //end update 
                        }
//                    }
                }
                // 与第一个单元格相交或
                else if (leftMiddleIndex == -1 && rightMiddleIndex != -1) {
                    //文件夹功能
//                    if(MT_GlobalConfig.ICON_FOLDER && triggerFolderOuterH(x, width, rightMiddleIndex, false)){
//                        targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX;
//                    }
//                    else
//                    {
                        targetIndex = 0;
//                    }
                }
                // 最后一个单元格相交
                else if (leftMiddleIndex != -1 && rightMiddleIndex == -1) {
                    //文件夹功能
//                    if(MT_GlobalConfig.ICON_FOLDER && triggerFolderOuterH(x, width, leftMiddleIndex, true)){
//                        targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + count-1;
//                    }
//                    else
//                    {
                        targetIndex = count - 1;
//                    }
                }
                // 如果与二个单元格相交,再看拖动方向
                else if(rightMiddleIndex - leftMiddleIndex == 1) {
                    //文件夹功能
//                    if(MT_GlobalConfig.ICON_FOLDER && leftMiddleIndex!=-1 && triggerFolderOuterH(x, width, leftMiddleIndex, true)){
//                        targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + leftMiddleIndex;
//                    }
//                    else if(MT_GlobalConfig.ICON_FOLDER && rightMiddleIndex!=-1 && triggerFolderOuterH(x, width, rightMiddleIndex, false)){
//                        targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX +  rightMiddleIndex;
//                    }
//                    else{
                        // 从前往后拖
                        if (leftMiddleIndex - dragView.getStartIndex() >= 0) {
                            targetIndex = leftMiddleIndex;
                        }
                        // 从后往前拖
                        else {
                            targetIndex = rightMiddleIndex;
                        }
//                    }
                }
                
                //add by 张永辉 2011-7-21 
                //跨三列
                else if(rightMiddleIndex - leftMiddleIndex == 2){
                    targetIndex = rightMiddleIndex-1;
                }
                //end 
            }
            // 相交
            else {
                // 拖动图标与二个单元格都相交
                if (leftTopIndex != rightTopIndex || leftBottomIndex != rightBottomIndex) {
                    //拖动图标左边或右边在单元格里面
                    if(leftTopIndex == leftBottomIndex || rightBottomIndex == rightTopIndex){
                        //文件夹功能
//                        if(MT_GlobalConfig.ICON_FOLDER && leftTopIndex!=-1 && triggerFolderInnerH(x, width, height, leftTopIndex, true)){
//                            targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + leftTopIndex ;
//                        }
//                        else if(MT_GlobalConfig.ICON_FOLDER && rightTopIndex!=-1 && triggerFolderInnerH(x, width, height, rightTopIndex, false)){
//                            targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + rightTopIndex ;
//                        }
//                        else{
                            // 从前往后拖，返回第一个
                            if ((leftTopIndex == -1 ? leftBottomIndex : leftTopIndex)
                                    - dragView.getStartIndex() >= 0) {
                                if (leftTopIndex == -1 && leftBottomIndex == -1) {
                                    targetIndex = 0;
                                } else {
                                    targetIndex = leftTopIndex == -1 ? leftBottomIndex : leftTopIndex;
                                }
                            }
                            // 从后往前，返回第二个
                            else {
                                if (rightTopIndex == -1 && rightBottomIndex == -1) {
                                    targetIndex = count - 1;
                                } else {
                                    targetIndex = rightTopIndex == -1 ? rightBottomIndex : rightTopIndex;
                                }
                            }
//                        }
                        
                    }
                    //拖动图标左边或右边不全在单元格里面
                    else
                    {
                        //文件夹功能
//                        if(MT_GlobalConfig.ICON_FOLDER && leftTopIndex!=-1 && triggerFolder(x, y, width, height, leftTopIndex, true, true)){
//                            targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + leftTopIndex ;
//                        }
//                        else if(MT_GlobalConfig.ICON_FOLDER && rightTopIndex!=-1 && triggerFolder(x, y, width, height, rightTopIndex, false, true)){
//                            targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + rightTopIndex ;
//                        }
//                        else if(MT_GlobalConfig.ICON_FOLDER && leftBottomIndex!=-1 && triggerFolder(x, y, width, height, leftBottomIndex, true, false)){
//                            targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + leftBottomIndex ;
//                        }
//                        else if(MT_GlobalConfig.ICON_FOLDER && rightBottomIndex!=-1 && triggerFolder(x, y, width, height, rightBottomIndex, false, false)){
//                            targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + rightBottomIndex ;
//                        }
//                        else
//                        {
                            // 从前往后拖，返回第一个
                            if ((leftTopIndex == -1 ? leftBottomIndex : leftTopIndex)
                                    - dragView.getStartIndex() >= 0) {
                                if (leftTopIndex == -1 && leftBottomIndex == -1) {
                                    targetIndex = 0;
                                } else {
                                    targetIndex = leftTopIndex == -1 ? leftBottomIndex : leftTopIndex;
                                }
                            }
                            // 从后往前，返回第二个
                            else {
                                if (rightTopIndex == -1 && rightBottomIndex == -1) {
                                    targetIndex = count - 1;
                                } else {
                                    targetIndex = rightTopIndex == -1 ? rightBottomIndex : rightTopIndex;
                                }
                            }
//                        }
                    }
                }
                // 拖动图标只与一个单元格相交
                else {
                    //如果拖动图标被包含在单元格内
                    if(leftTopIndex == leftBottomIndex){
                        //文件夹功能
//                        if(MT_GlobalConfig.ICON_FOLDER && getTargetType(leftBottomIndex) == MT_GlobalConfig.TARGET_FOLDER){
//                            targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + leftBottomIndex ;
//                        }
//                        else{
                            targetIndex = leftTopIndex == -1 ? leftBottomIndex : leftTopIndex;
//                        }
                    }
                    //上边或下边在单元格内
                    else{
                        if(Mogoo_GlobalConfig.ICON_FOLDER && leftTopIndex!=-1 && triggerFolderInnerV(y, width, height, leftTopIndex, true)){
                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + leftTopIndex ;
                        }
                        else if(Mogoo_GlobalConfig.ICON_FOLDER && leftBottomIndex!=-1 && triggerFolderInnerV(y, width, height, leftBottomIndex, false)){
                            targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + leftBottomIndex ;
                        }
                        else{
                            targetIndex = leftTopIndex == -1 ? leftBottomIndex : leftTopIndex;
                        }
                    }
                }
            }
        }
        
        // 重置拖动开始索引值
        if (targetIndex != -1) {
            dragView.setStartIndex(targetIndex);
        }
        
        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "Find Target Index:"+targetIndex) ;
        	Log.d(TAG, "-------------------------- end --------------------------------------"); 
        }

        return targetIndex;
    }
    
    /**
     * 识别排序插入点index
     *@author: 张永辉
     *@param dragView
     *@param parent
     *@return
     */
    private int findInsertIndex(DragView dragView, ViewGroup parent) {
            	
    	 if(Mogoo_GlobalConfig.LOG_DEBUG)
         {
         	Log.d(TAG, "--------------------call findInsertIndex()--------------------------------------");
         }
    	 
        int targetIndex = -1;
        // 拖动图标左上角的坐标
        int x = dragView.getScreenLeft();
        int y = dragView.getScreenTop() - getScreenTop();
        int width = dragView.getWidth();
        int height = dragView.getHeight();

        int count = this.getDockWorkSpaceCellCount();

        CellEntry first = null;
        CellEntry last = null;

        // isFromOther = false ; //重置标识
        // 当前单元格总数为0时，返回目标索引为0
        if (count == 0) 
        {
            targetIndex = 0;
        } 
        else if (count > 0) 
        {
            try 
            {
                // 取得第一个单元格坐标
                first = this.getCellEntry(0);
                // 取得第最后一个单元格坐标
                last = this.getCellEntry(count - 1);
            } 
            catch (Exception e) 
            {
                return -1;
            }

            // 如果移入的图标在第一个单元格的左边，则返回0
            if ((x + width) <= first.left) 
            {
                targetIndex = 0;
            }
            // 如果移入的图标在最后一个单元格的右边，则返回count
            else if (x >= last.right) 
            {
                targetIndex = count;
            } 
            else 
            {
                int leftTopIndex = this.getCellIndexByCoordinate(x, y); // 左上角所在单元格
                int leftBottomIndex = this.getCellIndexByCoordinate(x, y + height);// 左下角所在单元格
                int rightTopIndex = this.getCellIndexByCoordinate(x + width, y); // 右上角所在单元格
                int rightBottomIndex = this.getCellIndexByCoordinate(x + width, y + height);// 右下角所在的单元格
                
                if(Mogoo_GlobalConfig.LOG_DEBUG)
                {            
                    Log.d(TAG, "leftTopIndex = "+leftTopIndex+", leftBottomIndex = "+leftBottomIndex+", rightTopIndex = "+rightTopIndex+", rightBottomIndex = "+rightBottomIndex) ;
                }
                
                // 如果拖动图标四个角不在单元格内
                if (leftTopIndex == -1 && leftBottomIndex == -1 && rightTopIndex == -1
                        && rightBottomIndex == -1) 
                {
                    int leftMiddleIndex = this.getCellIndexByCoordinate(x, y + height / 2);
                    int rightMiddleIndex = this.getCellIndexByCoordinate(x + width, y + height / 2);
                    //拖动图标包含了整个单元格或不与任单元格相交
                    if(leftMiddleIndex == -1 && rightMiddleIndex == -1) {
                        int middleIndex = this.getCellIndexByCoordinate(x+width/2, y + height / 2);
                        //拖动图标包含了整个单元格
                        if(middleIndex!=-1){
//                            //文件夹功能
//                            if(MT_GlobalConfig.ICON_FOLDER && getTargetType(middleIndex)==MT_GlobalConfig.TARGET_FOLDER){
//                                targetIndex = middleIndex + MT_GlobalConfig.FOLDER_BASE_INDEX ;
//                            }else{
                                targetIndex = middleIndex+1 ;
//                            }
                        //不与任单元格相交
                        }else{
                            targetIndex = count ;
                        }
                    }
                    // 如果只与一个单元格相交,看左右边哪边离中线远
                    else if (leftMiddleIndex == rightMiddleIndex) {
                        CellEntry entry = this.getCellEntry(leftMiddleIndex);
                        int leftPart = entry.left + (entry.right - entry.left) / 2 - x;
                        int rightPart = x + width - entry.left - (entry.right - entry.left) / 2;
                        
                        //文件夹功能
//                        if(MT_GlobalConfig.ICON_FOLDER  && getTargetType(leftMiddleIndex)==MT_GlobalConfig.TARGET_FOLDER){
//                            targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + leftMiddleIndex ;
//                        }
//                        else
//                        {
                            // 左部分大于等于右部分
                            if (leftPart >= rightPart) {
                                targetIndex = leftMiddleIndex;
                            }
                            // 如果右部分大于左部分
                            else {
                                targetIndex = leftMiddleIndex + 1;
                            }
//                        }
                    }
                    // 与第一个单元格相交或
                    else if (leftMiddleIndex == -1 && rightMiddleIndex != -1) {
                        //文件夹功能
//                        if(MT_GlobalConfig.ICON_FOLDER && triggerFolderOuterH(x, width, rightMiddleIndex, false)){
//                            targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX;
//                        }
//                        else
//                        {
                            targetIndex = 0;
//                        }
                    }
                    // 最后一个单元格相交
                    else if (leftMiddleIndex != -1 && rightMiddleIndex == -1) {
                        //文件夹功能
//                        if(MT_GlobalConfig.ICON_FOLDER && triggerFolderOuterH(x, width, leftMiddleIndex, true)){
//                            targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + count;
//                        }
//                        else
//                        {
                            targetIndex = count;
//                        }
                    }
                    // 如果与二个单元格相交
                    else {
                        //文件夹功能
//                        if(MT_GlobalConfig.ICON_FOLDER && leftMiddleIndex!=-1 && triggerFolderOuterH(x, width, leftMiddleIndex, true)){
//                            targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + leftMiddleIndex;
//                        }
//                        else if(MT_GlobalConfig.ICON_FOLDER && rightMiddleIndex!=-1 && triggerFolderOuterH(x, width, rightMiddleIndex, false)){
//                            targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX +  rightMiddleIndex;
//                        }
//                        else{
                            targetIndex = rightMiddleIndex;
//                        }
                    }
                }
                // 相交
                else {
                    // 拖动图标与二个单元格都相交,则返回第二个相交的单元格号
                    if (leftTopIndex != rightTopIndex || leftBottomIndex != rightBottomIndex) {
                        //拖动图标左边或右边在单元格里面
                        if(leftTopIndex == leftBottomIndex || rightBottomIndex == rightTopIndex){
                            //文件夹功能
//                            if(MT_GlobalConfig.ICON_FOLDER && leftTopIndex!=-1 && triggerFolderInnerH(x, width, height, leftTopIndex, true)){
//                                targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + leftTopIndex ;
//                            }
//                            else if(MT_GlobalConfig.ICON_FOLDER && rightTopIndex!=-1 && triggerFolderInnerH(x, width, height, rightTopIndex, false)){
//                                targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + rightTopIndex ;
//                            }
//                            else{
                                // 处于最右边
                                if (rightTopIndex == -1 && rightBottomIndex == -1) {
                                    targetIndex = count;
                                } else {
                                    targetIndex = rightTopIndex == -1 ? rightBottomIndex : rightTopIndex;
                                }
//                            }
                            
                        }
                        //拖动图标左边或右边不全在单元格里面
                        else
                        {
                            //文件夹功能
//                            if(MT_GlobalConfig.ICON_FOLDER && leftTopIndex!=-1 && triggerFolder(x, y, width, height, leftTopIndex, true, true)){
//                                targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + leftTopIndex ;
//                            }
//                            else if(MT_GlobalConfig.ICON_FOLDER && rightTopIndex!=-1 && triggerFolder(x, y, width, height, rightTopIndex, false, true)){
//                                targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + rightTopIndex ;
//                            }
//                            else if(MT_GlobalConfig.ICON_FOLDER && leftBottomIndex!=-1 && triggerFolder(x, y, width, height, leftBottomIndex, true, false)){
//                                targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + leftBottomIndex ;
//                            }
//                            else if(MT_GlobalConfig.ICON_FOLDER && rightBottomIndex!=-1 && triggerFolder(x, y, width, height, rightBottomIndex, false, false)){
//                                targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + rightBottomIndex ;
//                            }
//                            else
//                            {
                                // 处于最右边
                                if (rightTopIndex == -1 && rightBottomIndex == -1) {
                                    targetIndex = count;
                                } else {
                                    targetIndex = rightTopIndex == -1 ? rightBottomIndex : rightTopIndex;
                                }
//                            }
                        }
                    }
                    // 拖动图标只与一个单元格相交，则要比较拖动图标与相交单元格中线的相对位置
                    else {
                        //如果拖动图标被包含在单元格内
                        if(leftTopIndex == leftBottomIndex){
                            //文件夹功能
//                            if(MT_GlobalConfig.ICON_FOLDER && getTargetType(leftBottomIndex) == MT_GlobalConfig.TARGET_FOLDER){
//                                targetIndex = MT_GlobalConfig.FOLDER_BASE_INDEX + leftBottomIndex ;
//                            }
//                            else{
                                // 相交单元格号
                                int cellIndex = leftTopIndex == -1 ? leftBottomIndex : leftTopIndex;

                                CellEntry entry = this.getCellEntry(cellIndex);
                                // 中线横坐标
                                int xMiddle = entry.left + (entry.right - entry.left) / 2;

                                // 左半部分
                                int leftPart = xMiddle - x;
                                int rightPart = x + width - xMiddle;

                                // 左半部分大于右半部分，则返回相交单元格号
                                if (leftPart > rightPart) {
                                    targetIndex = cellIndex;
                                }
                                // 如果左半部分小于等于右半部分，则返回相交单元格号＋1
                                else {
                                    targetIndex = cellIndex + 1;
                                }
//                            }
                        }
                        //上边或下边在单元格内
                        else{
                            if(Mogoo_GlobalConfig.ICON_FOLDER && leftTopIndex!=-1 && triggerFolderInnerV(y, width, height, leftTopIndex, true)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + leftTopIndex ;
                            }
                            else if(Mogoo_GlobalConfig.ICON_FOLDER && leftBottomIndex!=-1 && triggerFolderInnerV(y, width, height, leftBottomIndex, false)){
                                targetIndex = Mogoo_GlobalConfig.FOLDER_BASE_INDEX + leftBottomIndex ;
                            }
                            else{
                                // 相交单元格号
                                int cellIndex = leftTopIndex == -1 ? leftBottomIndex : leftTopIndex;

                                CellEntry entry = this.getCellEntry(cellIndex);
                                // 中线横坐标
                                int xMiddle = entry.left + (entry.right - entry.left) / 2;

                                // 左半部分
                                int leftPart = xMiddle - x;
                                int rightPart = x + width - xMiddle;

                                // 左半部分大于右半部分，则返回相交单元格号
                                if (leftPart > rightPart) {
                                    targetIndex = cellIndex;
                                }
                                // 如果左半部分小于等于右半部分，则返回相交单元格号＋1
                                else {
                                    targetIndex = cellIndex + 1;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 重置拖动开始索引值
        if (targetIndex != -1) {
            dragView.setStartIndex(targetIndex);
        }

        if(Mogoo_GlobalConfig.LOG_DEBUG)
        {
            Log.d(TAG, "Find Target Index:" + targetIndex) ;
        	Log.d(TAG, "-------------------------- end --------------------------------------"); 
        }
        
        return targetIndex;
    }



    public CellEntry getCellEntry(int cellIndex) {
    	if(cellIndex >= dockbarCellCoordinateList.size()){
    		return null;
    	}
    	
        return dockbarCellCoordinateList.get(cellIndex);
    }

    public List<CellEntry> getCellCoordinateList() {
        return dockbarCellCoordinateList;
    }

    /**
     * 取得指定坐标所在的单元格的素引号 
     * @ author: 魏景春 
     * @param x 拖动图标相对拖动区域的x坐标
     * @param y 拖动图标相对拖动区域的y坐标
     * @return 单元格索引号
     */
    int getCellIndexByCoordinate(int x, int y) {
        // 找不到在哪一个单元格中时，返回-1
        int index = -1;

        // 单元格总数
        int count = getDockWorkSpaceCellCount();

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
     * 初始化单元格坐标值到列表中 
     * @ author: 魏景春
     * @param viewCount 图标数
     */
    private void initCellCoordinateList(int viewCount) {

        dockbarCellCoordinateList.clear();
        
        if (Mogoo_GlobalConfig.LOG_DEBUG) {
            Log.d(TAG, "CellCoordinateList = " + viewCount);
        }
        int leftStartX = getLeftStartX(viewCount);
        for (int i = 0; i < viewCount; i++) {
            int left = this.getLeftPadding() + leftStartX + i * (mCellWidth + mWidthGap);
            int right = left + mCellWidth;
            int top = this.getTopPadding() + 0 * (mCellHeight + mHeightGap);
            int bottom = top + mCellHeight;

            CellEntry entry = new CellEntry();
            entry.left = left;
            entry.bottom = bottom;
            entry.right = right;
            entry.top = top;

            if (Mogoo_GlobalConfig.LOG_DEBUG) {
                Log.d(TAG, "left=" + left + " top=" + top + " right=" + right + " bottom=" + bottom);
            }

            dockbarCellCoordinateList.add(entry);
        }

    }

    private int getScreenTop() {
        return Mogoo_GlobalConfig.getScreenHeight() - mHeightSpecSize;
    }
    
    /**
     * 从workspace中移除桌面图标，并从数据库中也清除相信的桌面图标信息
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

        // Avoid ANRs by treating each screen separately
        post(new Runnable() {
            public void run() {
                final ArrayList<View> childrenToRemove = new ArrayList<View>();
                childrenToRemove.clear();

                for (int j = 0; j < count; j++) {
                    final View view = getChildAt(j);
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
                                    if(view instanceof Mogoo_BubbleTextView){
                                        ((Mogoo_BubbleTextView)view).stopVibrate();
                                    }
                                    
                                    mIconCache.recycle(info.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
                                    mIconCache.remove(info.intent.getComponent()); 
                                    
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
                                        LauncherModel.deleteItemFromDatabase(mLauncher, appInfo);
                                        removedFromFolder = true;
                                    }
                                }
                            }
                        }

                        contents.removeAll(toRemove);
                        if (removedFromFolder) {
                            // final Folder folder = getOpenFolder();
                            // if (folder != null)
                            // folder.notifyDataSetChanged();
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

                int childCount = childrenToRemove.size();
                for (int j = 0; j < childCount; j++) {
                    View child = childrenToRemove.get(j);
                    removeView(child);
                    if (child instanceof DropTarget) {
                        mDragController.removeDropTarget((DropTarget) child);
                    }
                }

                for (int i = 0; i < getChildCount(); i++) {
                    View temp = getChildAt(i);
                    LauncherModel.moveItemInDatabase(mLauncher, (ItemInfo) temp.getTag(),
                            LauncherSettings.Favorites.CONTAINER_TOOLBAR, -1, i, -1);
                }
                
                Mogoo_DockWorkSpace.this.resetCellLayout(new int []{0,-1}) ;

            }
        });
    }

    /**
     * 更新桌面图标 @ author: 张永辉
     * 
     * @param packageNameList 要更新的应用的包名
     */
    void updateShortcuts(List<String> packageNameList) {
        final PackageManager pm = mLauncher.getPackageManager();

        final int count = getChildCount();
        for (int j = 0; j < count; j++) {
            final View view = this.getChildAt(j);
            Object tag = view.getTag();
            if (tag instanceof ShortcutInfo) {
                ShortcutInfo info = (ShortcutInfo) tag;
                final Intent intent = info.intent;
                final ComponentName name = intent.getComponent();
                if ((info.itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION || info.itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT)
                        && Intent.ACTION_MAIN.equals(intent.getAction()) && name != null) {
                    final int appCount = packageNameList.size();
                    for (int k = 0; k < appCount; k++) {
                        if (packageNameList.get(k).equals(name.getPackageName())) {
                            mIconCache.remove(name);
                            mIconCache.recycle(name, Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
                            info.setIcon(mIconCache.getIcon(info.intent));
                            ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(null,
                                    new FastBitmapDrawable(info.getIcon(mIconCache)), null, null);

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
    
    /**
     * 根据索引号得到该索引对应的图标类型 
     *@author:张永辉 
     *@param index
     *@return
     */
    private int getTargetType(int index){
        int count = getChildCount() ;
        
        if(index>=count){
            return Mogoo_GlobalConfig.TARGET_NULL ;
        }else{
            View child = getChildAt(index) ;
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
     * 是否触发文件夹(拖动图标左边或右边在单元格里面)
     *@author: 张永辉
     *@Date：2011-3-18
     *@param dragViewX
     *@param dragHeight
     *@param index
     *@param isLeft
     *@return
     */
    private boolean triggerFolderInnerH(int dragViewX,int dragWidth,int dragHeight,int index,boolean isLeft){
        
        int targetType = this.getTargetType(index) ;
        
        if(targetType != Mogoo_GlobalConfig.TARGET_FOLDER){
            return false ;
        }
        
        CellEntry entry = getCellEntry(index) ;
        float cellArea = Mogoo_GlobalConfig.getCellArea()  ;
        float area = 0f ;
        
        if(isLeft){
           area = (entry.right - dragViewX)*(dragHeight) ;
        }else{
            area = (dragViewX + dragWidth - entry.left)*dragHeight ;
        }
            
        float rate = area / cellArea ;
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "triggerFolderInnerH area="+area+" rate="+rate) ;
        }
        
        if(rate >= Mogoo_GlobalConfig.getFolderAcceptAreaRate()){
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
    private boolean triggerFolderInnerV(int dragViewY,int dragWidth,int dragHeight,int index,boolean isTop){
        int targetType = this.getTargetType(index) ;
        
        if(targetType != Mogoo_GlobalConfig.TARGET_FOLDER){
            return false ;
        }
        
        CellEntry entry = getCellEntry(index) ;
        float cellArea = Mogoo_GlobalConfig.getCellArea()  ;
        float area = 0f ;
        
        if(isTop){
           area = (entry.bottom-dragViewY)*dragWidth ;
        }else{
            area = (dragViewY + dragHeight - entry.top)*dragWidth ;
        }
            
        float rate = area / cellArea ;
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "triggerFolderInnerV area="+area+" rate="+rate) ;
        }
        
        if(rate >= Mogoo_GlobalConfig.getFolderAcceptAreaRate()){
            return true ;
        }
        
        return false ;
    }
    
    /**
     * 是否触发文件夹(拖动图标只有一个顶点在单元格里面)
     *@author: 张永辉
     *@Date：2011-3-18
     *@param dragViewX
     *@param dragViewY
     *@param dragWidth
     *@param dragHeight
     *@param index
     *@param isLeft
     *@param isTop
     *@return
     */
    private boolean triggerFolder(int dragViewX,int dragViewY,int dragWidth,int dragHeight,int index,boolean isLeft,boolean isTop){
        
        int targetType = this.getTargetType(index) ;
        
        if(targetType != Mogoo_GlobalConfig.TARGET_FOLDER){
            return false ;
        }
        
        CellEntry entry = getCellEntry(index) ;
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
        
       if(rate >= Mogoo_GlobalConfig.getFolderAcceptAreaRate()){
            return true ;
        }
        
        return false ;
    }
    
    /**
     * 是否触发文件夹(拖动图标四个角在指定单元格外面且拖动图标与近定单元格相交)
     *@author: 张永辉
     *@Date：2011-3-18
     *@param dragViewX
     *@param dragWidth
     *@param index
     *@param isLeft
     *@return
     */
    private boolean triggerFolderOuterH(int dragViewX,int dragWidth,int index,boolean isLeft){
        
        int targetType = this.getTargetType(index) ;
        
        if(targetType == Mogoo_GlobalConfig.TARGET_NULL || targetType == Mogoo_GlobalConfig.TARGET_SHORTCUT){
            return false ;
        }
        
        CellEntry entry = getCellEntry(index) ;
        
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
        
        //folder
        if(rate >= Mogoo_GlobalConfig.getFolderAcceptAreaRate()){
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
    private boolean targetFolderIsFull(int index)
    {
        int count = Mogoo_GlobalConfig.getWorkspaceLongAxisCells(Mogoo_GlobalConfig.isLandscape())
                    *(Mogoo_GlobalConfig.getWorkspaceShortAxisCells(Mogoo_GlobalConfig.isLandscape())-1) ;
        
        View targetView = getChildAt(index-Mogoo_GlobalConfig.FOLDER_BASE_INDEX);
        
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
    
    /**
     * 是否为工具栏中的图标
     *@author: 张永辉
     *@Date：2011-4-6
     *@param view
     *@return
     */
    private boolean isChild(View view)
    {
        int count = getChildCount() ;
        for(int i=0;i<count;i++)
        {
            if(view.equals(getChildAt(i)))
            {
                return true ;
            }
        }
        return false ;
    }
    
    /**
     * 是否含有图标文件夹
     *@author: 张永辉
     *@Date：2011-4-7
     *@return
     */
    private boolean haveFolder()
    {
        int count = getChildCount() ;
        for(int i=0;i<count;i++)
        {
            if(getChildAt(i) instanceof Mogoo_FolderBubbleText)
            {
                return true ;
            }
        }
        return false ;
    }
    
    /**
     * 是否有不可见图标
     *@author: 张永辉
     *@Date：2011-4-7
     *@return
     */
    private boolean haveInVisibleChild()
    {
        int count = getChildCount() ;
        for(int i=0;i<count;i++)
        {
            if(getChildAt(i).getVisibility() == View.INVISIBLE)
            {
                return true ;
            }
        }
        return false ;
    }
    
    public void setLocaleDrag(boolean isLocaleDrag) {
        this.isLocaleDrag = isLocaleDrag;
    }

}