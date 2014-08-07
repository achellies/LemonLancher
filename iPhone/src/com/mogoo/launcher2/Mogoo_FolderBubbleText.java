/**  
 * 文 件 名:  MT_FolderBubbleText.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：  黄悦                     
 * 版    本:  1.0  
 * 创建时间:   2011-3-14
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-3-14        黄悦       1.0          1.0 Version  
 */        

package com.mogoo.launcher2;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_BitmapCache;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;
import com.mogoo.launcher2.utils.Mogoo_Utilities;

import android.R.integer;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.text.method.KeyListener;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.RelativeLayout.LayoutParams;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class Mogoo_FolderBubbleText extends Mogoo_BubbleTextView implements View.OnClickListener{
    
    private String TAG = "Launcher.Mogoo_FolderBubbleText" ;
    
    private Bitmap[] openImages ;
    
    public static boolean folderOpening = false;
    
    public static boolean isOpen = false;
    
    private HashMap<Integer, Integer> countMap; 
    
    //记录文件夹打开后上部图片的高度
    private int topHeight  ;
    
//    private Launcher launcher ;

    public Mogoo_FolderBubbleText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Mogoo_FolderBubbleText(Context context) {
        this(context, null);
    }

    public int findTargetIndex(DragView dragView, ViewGroup parent) {
        return 0;
    }
    
//    public void setLauncher(Launcher launcher) {
//        this.launcher = launcher;
//    }
    
    public int getTopHeight() {
        return topHeight;
    }
    
    public HashMap<Integer, Integer> getCountMap() {
        if(countMap == null){
            countMap = new HashMap<Integer, Integer>();
        }
        
        return countMap;
    }

    /**
     * 添加计数图标 @ author: 黄悦
     * 
     * @param countIcon
     */
    public void setCountIcon(Mogoo_BitmapCache cache, int num, int type) {
        if(countMap == null){
            countMap = new HashMap<Integer, Integer>();
        }
        
        if(num != 0){
            countMap.put(type, num);
        }else{
            countMap.remove(type);
        }
        
        Collection<Integer> nums = countMap.values();
        int sum = 0;
        for(int n : nums){
            sum += n;
        }
        
        Bitmap countIcon = cache.getDigitalIcon(sum);
        
        if (this.countIcon != null) {
            Bitmap temp = this.countIcon;
            temp.recycle();
            temp = null;
        }
        
        this.countIcon = countIcon;
    }
    
    /**
     * 打开图标文件夹 
     *@author:张永辉 
     *@param folder
     */
    void openFolder(){
        if(folderOpening || isOpen){
            return;
        }
        
        if (Mogoo_GlobalConfig.LOG_DEBUG) {
            Log.d(TAG, "folder id = " + this + " db_id = " + ((ShortcutInfo)this.getTag()).id);
        }
        
        //如果当前处于搜索屏或WIDGT屏，则切到第一个快捷方式屏后再打开文件夹
        filterOpenFolder() ;
        
        folderOpening = true;
        isOpen = true;
        Mogoo_FolderWorkspace.acceptFlag = true;
        
        Mogoo_ComponentBus bus = Mogoo_ComponentBus.getInstance();
        
        Workspace workspace = (Workspace)bus.getActivityComp(R.id.workspace, getContext());
        
        workspace.vibateOperate(false);
        Mogoo_FolderLayout folderLayer = (Mogoo_FolderLayout)bus.getActivityComp(R.id.folderLayer, getContext());
        folderLayer.setVisibility(VISIBLE);
        
        
        recyleOpenImages() ;
        
        openImages = getFolderLayerTopAndBottomImage();
        if(openImages == null){
//            new Handler().postDelayed(new Runnable() {
//                public void run() {
//                    openFolder();
//                }
//            }, 150);
            
            return;
        }
        
        ImageView folderTop = (ImageView)bus.getActivityComp(R.id.folderLayerTopImage, getContext());
        folderTop.setImageBitmap(openImages[0]);
        folderTop.setVisibility(VISIBLE);
        folderTop.setOnClickListener(this);
        
        ImageView folderBottom = (ImageView) bus.getActivityComp(R.id.folderLayerBottomImage, getContext());
        folderBottom.setImageBitmap(openImages[1]);
        folderBottom.setVisibility(VISIBLE) ;
        folderBottom.setOnClickListener(this);
        
        EditText titleEdit = (EditText)bus.getActivityComp(R.id.titleEdit, getContext());
        titleEdit.setText(((ShortcutInfo)getTag()).title);
//        titleEdit.addTextChangedListener(new TextWatcher() {
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                if(count > 0){
//                    ShortcutInfo info = (ShortcutInfo) getTag();
//                    info.title = s;
//                    setText(s);
//                    
//                    LauncherModel.updateItemInDatabase(getContext(), info);
//                }
//            }
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//            public void afterTextChanged(Editable text) {}
//        });
        
        int position = titleEdit.length(); 
        Selection.setSelection(titleEdit.getText(), position);
        
        TextView title = (TextView)bus.getActivityComp(R.id.title, getContext());
        title.setText(((ShortcutInfo)getTag()).title);
        
        Mogoo_FolderWorkspace folderWorkspace = (Mogoo_FolderWorkspace) bus.getActivityComp(R.id.folderWorkspace, getContext());
        folderWorkspace.setVisibility(VISIBLE);
        folderWorkspace.loadFolderWorkspace(this);
        
        int topHeight = this.getFolderLayerTopHeight() ;
        this.topHeight = topHeight ;
        int bottomHeight = this.getFolderLayerBottomHeight() ;
        int topMoveHeight = openImages[0].getHeight() - topHeight ;
        int bottomMoveHeight = openImages[1].getHeight() - bottomHeight ;
        
        LayoutParams lpTop = (LayoutParams)folderTop.getLayoutParams();
        lpTop.topMargin = -topMoveHeight;
        RelativeLayout folderLayerCenter = (RelativeLayout) bus.getActivityComp(R.id.folderLayerCenter, getContext());
        LayoutParams lpCenter = (LayoutParams) folderLayerCenter.getLayoutParams();
        lpCenter.topMargin = topHeight;
        LayoutParams lpBottom = (LayoutParams)folderBottom.getLayoutParams();
        lpBottom.topMargin = topMoveHeight + bottomMoveHeight;
        lpBottom.bottomMargin = -bottomMoveHeight;
        
        
        folderLayer.invalidate();
        
        workspace.getIconFolderAnimation().openFolderAnimation(folderTop, topMoveHeight, folderBottom, bottomMoveHeight, this);
        folderWorkspace.getLauncher().getFolderController().iconFolderInactive();
    }
    
    public void onClick(View v) {
        Mogoo_FolderWorkspace folderWorkspace = (Mogoo_FolderWorkspace) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.folderWorkspace, getContext());
        folderWorkspace.saveFolderName();
        closeFolder() ;
    }
    
    /**
     * 关闭图标文件夹 
     * @author:张永辉
     */
    void closeFolder(){
        if(folderOpening || !isOpen){
            return;
        }
        
        if(openImages!=null){
            final Mogoo_ComponentBus bus = Mogoo_ComponentBus.getInstance();
            ((InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(bus.getActivityComp(R.id.titleEdit, getContext()).getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            Mogoo_FolderWorkspace.acceptFlag = false;
            Workspace workspace = (Workspace)bus.getActivityComp(R.id.workspace, getContext());
            workspace.vibateOperate(true);
            workspace.setVisibility(View.VISIBLE);
            bus.getActivityComp(R.id.dockWorkSpace, getContext()).setVisibility(View.VISIBLE);
            
            int topHeight = this.getFolderLayerTopHeight() ;
            int bottomHeight = this.getFolderLayerBottomHeight() ;
            int topMoveHeight = openImages[0].getHeight() - topHeight ;
            int bottomMoveHeight = openImages[1].getHeight() - bottomHeight ;
            
            ImageView folderTop = (ImageView)bus.getActivityComp(R.id.folderLayerTopImage, getContext());
            ImageView folderBottom = (ImageView)bus.getActivityComp(R.id.folderLayerBottomImage, getContext());

            workspace.getIconFolderAnimation().closeFolderAnimation(folderTop, topMoveHeight, folderBottom, bottomMoveHeight);
            openImages = null;
            
            Mogoo_FolderWorkspace folderWorkspace = (Mogoo_FolderWorkspace)bus.getActivityComp(R.id.folderWorkspace, getContext());
            folderWorkspace.setLoadingFolder(null);
            
            Object tag = this.getTag() ;
            
            if(tag instanceof Mogoo_FolderInfo){
                Mogoo_FolderInfo info = (Mogoo_FolderInfo)tag ;
                info.setAddRow(false) ;
            }
            
            if(!folderWorkspace.isNewFolderClosed())
            {
                folderWorkspace.setNewFolderClosed(true) ;
            }
        }
    }
    
    /**
     *取得文件夹展开后中间区域的高度 
     *@author: 张永辉
     *@param itemSize 文件夹中的应用图标数
     *@return
     */
    private int getFolderLayerCenterHeight(){
        Object tag = this.getTag() ;
        
        if(tag instanceof Mogoo_FolderInfo){
            Mogoo_FolderInfo info = (Mogoo_FolderInfo)tag ;
            int itemSize = info.getContents().size() ;
            
            if(info.isAddRow()){
                itemSize ++ ;
            }
            
            int row = (itemSize - 1)/Mogoo_GlobalConfig.getWorkspaceLongAxisCells(Mogoo_GlobalConfig.isLandscape()) + 1;
            //MT_FolderWorkspace的高度
            int height = row*Mogoo_GlobalConfig.getWorkspaceCellHeight() ;
            //文件夹标题编辑框的高度
            int titleEditHeight = Mogoo_ComponentBus.getInstance().getActivityComp(R.id.titleText, getContext()).getHeight() ;
            return height + titleEditHeight ;
        }
        else
        {
            return 0 ;
        }
    }
    
    /**
     *取得文件夹展开后上部区域的高度  
     *@author: 张永辉
     *@param folder
     *@return
     */
    int getFolderLayerTopHeight(){
        
        int topPartHeight = 0 ;
        
        Workspace workspace = (Workspace)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.workspace, getContext());
        //add by yeben
        DragLayer dragLayer = (DragLayer)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.drag_layer, getContext());
        //end
        Object tag = this.getTag() ;
        
        if(tag instanceof Mogoo_FolderInfo){
            Mogoo_FolderInfo info = (Mogoo_FolderInfo)tag ;
            int centerPartHeight = this.getFolderLayerCenterHeight() ;
            CellLayout cellLayout =(CellLayout)(workspace.getChildAt(workspace.getCurrentScreen())) ;
            if(info.container == LauncherSettings.Favorites.CONTAINER_TOOLBAR){
                int bottomPartHeight = dragLayer.getHeight() - cellLayout.getWorkspaceCellBottom() ;
                topPartHeight =dragLayer.getHeight() - centerPartHeight - bottomPartHeight ;
            }else if(info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP){
                int bottomPartHeight = dragLayer.getHeight() - cellLayout.getCellEntry(cellLayout.getIndexByCellXY(info.cellX, info.cellY)).bottom;
                
                //如果其下的能放下文件夹内容区域
                if(bottomPartHeight>=centerPartHeight){
                    topPartHeight = dragLayer.getHeight() - centerPartHeight - (bottomPartHeight - centerPartHeight) ;
                }
                //如果其下不能放下文件内容区域
                else{
                    topPartHeight = dragLayer.getHeight() - centerPartHeight - Mogoo_GlobalConfig.getFolderOpenBottomHeight();
                }
            }
        }
        
        return topPartHeight ;
    }
    
    private int getFolderLayerBottomHeight(){
        
        int bottomPartHeight = 0 ;
        
        Workspace workspace = (Workspace)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.workspace, getContext());
        //add by yeben
        DragLayer dragLayer = (DragLayer)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.drag_layer, getContext());
        //end
        Object tag = this.getTag() ;
        
        if(tag instanceof Mogoo_FolderInfo){
            Mogoo_FolderInfo info = (Mogoo_FolderInfo)tag ;
            int centerPartHeight = this.getFolderLayerCenterHeight() ;
            CellLayout cellLayout =(CellLayout)(workspace.getChildAt(workspace.getCurrentScreen())) ;
            if(info.container == LauncherSettings.Favorites.CONTAINER_TOOLBAR){
                bottomPartHeight = dragLayer.getHeight() - cellLayout.getCellEntry(Mogoo_GlobalConfig.getWorkspaceCellCounts()-1).bottom ;
            }else if(info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP){
                bottomPartHeight = dragLayer.getHeight() - cellLayout.getCellEntry(cellLayout.getIndexByCellXY(info.cellX, info.cellY)).bottom;
                
                //如果其下的能放下文件夹内容区域
                if(bottomPartHeight>=centerPartHeight){
                    bottomPartHeight = bottomPartHeight - centerPartHeight ;
                }
                //如果其下不能放下文件内容区域
                else{
                    bottomPartHeight = Mogoo_GlobalConfig.getFolderOpenBottomHeight();
                }
            }
        }
        
        return bottomPartHeight ;
    }
    
    /**
     * 根据文件夹图标信息生成展开文件夹时的上，下部分图片 
     *@author: 张永辉 
     *@param folder 图片文件夹
     *@param screenImage
     *@return
     */
    private Bitmap[] getFolderLayerTopAndBottomImage(){
        
        Bitmap [] topAndBottomImage = new Bitmap[2] ;
        
        Bitmap topImage = null ;
        
        Bitmap bottomImage = null ;
        
        Object tag = this.getTag() ;
        
        Workspace workspace = (Workspace)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.workspace, getContext());
        
        if(tag instanceof Mogoo_FolderInfo){
            
            Mogoo_FolderInfo info = (Mogoo_FolderInfo)tag ;
            
            Bitmap screenImage = this.getScreenImage() ;
            
            if(screenImage == null){
                return null;
            }
            
            CellLayout cellLayout = (CellLayout)(workspace.getChildAt(workspace.getCurrentScreen())) ;
            
            CellEntry entry = null ;
            
//            //图标间合成只能发生在桌面，强制将不属于两者的文件夹转换为属于workspace
//            if(info.container != LauncherSettings.Favorites.CONTAINER_TOOLBAR && info.container != LauncherSettings.Favorites.CONTAINER_DESKTOP){
//                info.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
//            }
            
            //如果要展开的文件夹在DOCK工具栏上或在桌面的最后一行
            if(info.container == LauncherSettings.Favorites.CONTAINER_TOOLBAR
                    ||info.cellY==Mogoo_GlobalConfig.getWorkspaceShortAxisCellsPort()-1){
                //取得最后一行的纵坐标
                entry = cellLayout.getCellEntry(Mogoo_GlobalConfig.getWorkspaceCellCounts()-1) ;
            }
            //如果要展开的文件夹在桌面上
            else if(info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP){
                //取得打开文件夹的纵坐标
                entry = cellLayout.getCellEntry(cellLayout.getIndexByCellXY(info.cellX, info.cellY)) ;
            }
            
            if(Mogoo_GlobalConfig.LOG_DEBUG)
            {
                Log.d(TAG, "info.container="+info.container+" info.cellX="+info.cellX+" info.cellY="+info.cellY+ " entry="+entry) ;
            }
//            //cell xy 获取错误则返回null，放弃此次打开
//            if(entry == null){
//                screenImage.recycle();
//                screenImage = null;
//                topAndBottomImage = null;
//                Log.e(TAG, "entry error") ;
//                return topAndBottomImage;
//            }
            
            topImage = Bitmap.createBitmap(screenImage, 0, 0, screenImage.getWidth(), entry.bottom) ;
            bottomImage = Bitmap.createBitmap(screenImage, 0, entry.bottom, screenImage.getWidth(), screenImage.getHeight()- entry.bottom) ;
           
//            int topHeight = this.getFolderLayerTopHeight() ;
//            int centerHeight = this.getFolderLayerCenterHeight() ;
//            int bottomHeight = workspace.getHeight() - topHeight - centerHeight ;
//            
//            if(MT_GlobalConfig.LOG_DEBUG){
//                Log.d(TAG, "topHeight="+topHeight+" centerHeight="+centerHeight+" bottomHeight="+bottomHeight) ;
//            }
            
//            Bitmap topImageTemp = Bitmap.createBitmap(topImage, 0,topImage.getHeight()-topHeight , topImage.getWidth(), topHeight) ;
//            Bitmap bottomImageTemp = Bitmap.createBitmap(bottomImage, 0,0 , bottomImage.getWidth(), bottomHeight) ;
            
            topAndBottomImage[0] = topImage ;
            topAndBottomImage[1] = bottomImage ;
            
            if(screenImage!=null&&!screenImage.isRecycled()){
                screenImage.recycle() ;
            }
            
//            if(topImage!=null&&!topImage.isRecycled()){
//                topImage.recycle() ;
//            }
            
//            if(bottomImage!=null&&!bottomImage.isRecycled()){
//                bottomImage.recycle() ;
//            }
            
        }
        
        return topAndBottomImage ;
    }
    
    
    /**
     * 取得当前屏幕截图 
     * @author: 张永辉 
     * @param folder 图片文件夹
     * @return
     */
    private Bitmap getScreenImage()
    {
        
        Bitmap screenImage = null ;
        
        Object tag = getTag() ;
        
        Workspace workspace = (Workspace)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.workspace, getContext());
        
        
        if(tag instanceof Mogoo_FolderInfo){
            
            Mogoo_FolderInfo info = (Mogoo_FolderInfo)tag;
            
            //取得当前壁纸图片
            Bitmap wallpagerImage = Mogoo_Utilities.getWallpagerImage(this.getContext());
            
            // 获取屏幕的高宽
            int screenWidth = Mogoo_GlobalConfig.getScreenWidth(); // 屏幕的宽
            int screenHeight = Mogoo_GlobalConfig.getScreenHeight(); // 屏幕的高
            //取得当前状态栏的高度
            int statusBarHeight = workspace.getStatusBarHeight() ;
            Paint paint = new Paint() ;
            //设置图标透明度
            paint.setAlpha(60) ;
            
            //生成底层
            screenImage = Bitmap.createBitmap(wallpagerImage, 0, statusBarHeight, screenWidth, screenHeight-statusBarHeight) ;
            
            LauncherApplication app = (LauncherApplication) this.getContext().getApplicationContext();
            CellLayout cellLayout = (CellLayout)(workspace.getChildAt(workspace.getCurrentScreen())) ;
            Mogoo_DockWorkSpace dockWorkSpace = (Mogoo_DockWorkSpace) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.dockWorkSpace, getContext());
            
            cellLayout.setDrawingCacheEnabled(true);
            dockWorkSpace.setDrawingCacheEnabled(true);
            
            Canvas c = new Canvas(screenImage);
            
            Bitmap cellLayoutBitmap = cellLayout.getDrawingCache();
            Bitmap dockWorkSpaceBitmap = dockWorkSpace.getDrawingCache();
            
            //展开文件夹图标的索引号
            int index = -1 ;
            if(info.container==LauncherSettings.Favorites.CONTAINER_DESKTOP){//如果展开文件夹在桌面上
                cellLayoutBitmap = drawCellLayout(info, cellLayout, cellLayoutBitmap, app.getIconCache());
            }else if(info.container==LauncherSettings.Favorites.CONTAINER_TOOLBAR){
                dockWorkSpaceBitmap = drawDock(info, dockWorkSpace, dockWorkSpaceBitmap, app.getIconCache());
            }
            
            Bitmap dockBg = app.getIconCache().getBitmap(R.drawable.mogoo_dockview_background);
            
            //如果展开文件夹在桌面上
            if(info.container==LauncherSettings.Favorites.CONTAINER_DESKTOP){
                c.drawBitmap(cellLayoutBitmap, 0, 0, null);
                c.drawBitmap(dockBg, 0, screenHeight-statusBarHeight-dockBg.getHeight(), paint) ;
                c.drawBitmap(dockWorkSpaceBitmap, 0, screenHeight-statusBarHeight - dockWorkSpace.getHeight(), paint);
                cellLayoutBitmap.recycle();
            }else if(info.container==LauncherSettings.Favorites.CONTAINER_TOOLBAR){
                c.drawBitmap(cellLayoutBitmap, 0, 0, paint);
                c.drawBitmap(dockBg, 0, screenHeight-statusBarHeight-dockBg.getHeight(), paint) ;
                c.drawBitmap(dockWorkSpaceBitmap, 0, screenHeight-statusBarHeight - dockWorkSpace.getHeight(), null);
                dockWorkSpaceBitmap.recycle();
            }
            
            c.save();
            
            cellLayout.setDrawingCacheEnabled(false);
            dockWorkSpace.setDrawingCacheEnabled(false);
        }
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "screenImage width:"+screenImage.getWidth()) ;
            Log.d(TAG, "screenImage height:"+screenImage.getHeight());
            Log.d(TAG, "dockWorkSpace height:"+((Mogoo_DockWorkSpace) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.dockWorkSpace, getContext())).getHeight());
        }
        
        return screenImage ;
    }

    private Bitmap drawDock(Mogoo_FolderInfo info, Mogoo_DockWorkSpace dockWorkSpace, Bitmap bg, Mogoo_BitmapCache iconCache) {
        int index = findIndexByEquals(dockWorkSpace, info);
        if(index == -1){
            index = info.cellX ;
        }
        
        CellEntry entry = dockWorkSpace.getCellEntry(info.cellX);
        
        Bitmap bitmap = Bitmap.createBitmap(bg.getWidth(), bg.getHeight(), Config.ARGB_8888);
        Paint paint = new Paint() ;
        //设置图标透明度
        paint.setAlpha(60) ;
        
        Canvas canvas = new Canvas(bitmap);
        drawDockworkspaceImage(iconCache, canvas, entry, 0);
        canvas.clipRect(entry.left, entry.top, entry.right, entry.bottom, Op.XOR);
        canvas.drawBitmap(bg, 0, 0, paint);
        canvas.save(); 
        
        canvas = null;
        paint = null;
        return bitmap;
    }

    private Bitmap drawCellLayout(Mogoo_FolderInfo info, CellLayout cellLayout, Bitmap bg, Mogoo_BitmapCache iconCache) {
        int index = findIndexByEquals(cellLayout, info);
        if(index == -1){
            index = cellLayout.getIndexByCellXY(info.cellX, info.cellY) ;
        }
        CellEntry entry = cellLayout.getCellEntry(index);
        
        Bitmap bitmap = Bitmap.createBitmap(bg.getWidth(), bg.getHeight(), Config.ARGB_8888);
        Paint paint = new Paint() ;
        //设置图标透明度
        paint.setAlpha(60) ;
        
        Canvas canvas = new Canvas(bitmap);
        drawWorkspaceImage(iconCache, canvas, entry);
        canvas.clipRect(entry.left, entry.top, entry.right, entry.bottom, Op.XOR);
        canvas.drawBitmap(bg, 0, 0, paint);
        canvas.save(); 
        
        canvas = null;
        paint = null;
        return bitmap;
    }
    
    /**
     * 
     * 查找folder的真实位置
     * @ author: 黄悦
     *@param viewGroup
     *@param info
     *@return
     */
    public int findIndexByEquals(ViewGroup viewGroup, Mogoo_FolderInfo info){
        int size = viewGroup.getChildCount();
        for(int i = 0; i < size; i++){
            if(this.equals(viewGroup.getChildAt(i))){
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * 
     * 重画workspace image的图标
     * @ author: 黄悦
     *@param app
     *@param entry
     */
    public void drawWorkspaceImage(Mogoo_BitmapCache iconCache, Canvas c, CellEntry entry) {
//        Canvas c = new Canvas(bitmap);
        Mogoo_FolderInfo info = (Mogoo_FolderInfo)getTag();
        Paint paint = new Paint();
        paint.setAlpha(255);
        
        c.drawBitmap(info.getIcon(iconCache), entry.left + Mogoo_GlobalConfig.getWorkspaceCellMarginLeft() + 3, entry.top + Mogoo_GlobalConfig.getWorkspaceCellMarginTop() + 1, paint) ;
        c.save();
    }
    
    /**
     * 
     * 重画dockworkspace image的图标
     * @ author: 黄悦
     *@param app
     *@param entry
     */
    public void drawDockworkspaceImage(Mogoo_BitmapCache iconCache, Canvas c, CellEntry entry, int dockStartY) {
//        Canvas c = new Canvas(bitmap);
        Mogoo_FolderInfo info = (Mogoo_FolderInfo)getTag();
        Paint paint = new Paint();
        paint.setAlpha(255);
        
        c.drawBitmap(info.getIcon(iconCache), entry.left + Mogoo_GlobalConfig.getWorkspaceCellMarginLeft() + 3, dockStartY + entry.top + Mogoo_GlobalConfig.getWorkspaceCellMarginTop() - 4, paint) ;
        c.save();
    }
    
    
   

    /**
     * 取得当前壁纸图片 
     *@author: 张永辉
     *@return
     */
//    private Bitmap getWallpagerImage(){
//        // 获取当前壁纸
//        WallpaperManager wm = WallpaperManager.getInstance(this.getContext());
//        
//        Drawable wallpaper = wm.getDrawable();
//        Bitmap wallpagerImage = ((BitmapDrawable) wallpaper).getBitmap();
//        
//        if(MT_GlobalConfig.LOG_DEBUG){
//            Log.d(TAG, "wallpagerImage width:"+wallpagerImage.getWidth()) ;
//            Log.d(TAG, "wallpagerImage height:"+wallpagerImage.getHeight());
//        }
//        
//        return wallpagerImage ;
//    }
    
    private void recyleOpenImages(){
        if(openImages!=null){
          if(openImages[0]!=null&&!openImages[0].isRecycled()){
              openImages[0].recycle() ;
          }
          if(openImages[1]!=null&&!openImages[1].isRecycled()){
              openImages[1].recycle() ;
          }
          openImages = null ;
      }
    }
    

    /**
     * 如果当前处于搜索屏或WIDGT屏，则切到第一个快捷方式屏后再打开文件夹
     *@author: 张永辉
     *@Date：2011-4-9
     */
    private void filterOpenFolder()
    {
        Workspace workspace = (Workspace)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.workspace, getContext());
        
        if (Mogoo_GlobalConfig.isSearchScreen(workspace.getCurrentScreen())  
                 || Mogoo_GlobalConfig.isWidgetScreen(workspace.getCurrentScreen())) {
            int [] shortcutScreen = Mogoo_GlobalConfig.getShortcutScreen() ;
            if(shortcutScreen!=null && shortcutScreen.length>0)
            {
                workspace.snapToScreen(shortcutScreen[0], 0, false);
            }
            
        }
    }
    
}
