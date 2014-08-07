/**  
 * 文 件 名:  MT_FolderInfo.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：                       
 * 版    本:  1.0  
 * 创建时间:   2011-3-14
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-3-14        author       1.0          1.0 Version  
 */  

package com.mogoo.launcher2;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_BitmapCache;
import com.mogoo.launcher2.utils.Mogoo_BitmapUtils;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;
import com.mogoo.launcher2.utils.Mogoo_Utilities;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

public class Mogoo_FolderInfo extends ShortcutInfo {
    /**
     * Whether this folder has been opened
     */
    private boolean opened;

    /**
     * The folder name.
     */
    CharSequence folderName;
    
    private Bitmap mIcon;
    
    private Bitmap mIconReflection;
    

    //是否加行
    private boolean addRow = false ;
    
    /**
     * The shortcuts 
     */
    private ArrayList <ShortcutInfo> contents = new ArrayList<ShortcutInfo>();
    
    public Mogoo_FolderInfo() {
        itemType = LauncherSettings.Favorites.ITEM_TYPE_MOGOO_FOLDER;
    }
    
    Mogoo_FolderInfo(Mogoo_FolderInfo info){
        this.addRow = info.addRow  ;
        this.appType = info.appType  ;
        this.cellX = info.cellX ;
        this.cellY = info.cellY ;
        this.container = info.container ;
        for(ShortcutInfo item:this.contents){
            add(item);
        }
        this.customIcon = info.customIcon ;
        this.folderName = info.folderName ;
        this.iconResource = info.iconResource ;
        this.id = info.id ;
        this.intent = info.intent ;
        this.isGesture = info.isGesture ;
        this.isSystem = info.isSystem ;
        this.itemType = info.itemType ;
        this.mIcon = info.mIcon ;
        this.mIconReflection = info.mIconReflection ;
        this.onExternalStorage = info.onExternalStorage ;
        this.opened = info.opened ;
        this.screen = info.screen ;
        this.spanX = info.spanX ;
        this.spanY = info.spanY ;
        this.title = info.title ;
        this.usingFallbackIcon = info.usingFallbackIcon ;
    }
    
    /**
     * Add an shortcut
     * 
     * @param item
     */
    public void add(ShortcutInfo item) {
        contents.add(item);
    }
    
    /**
     * Remove an  shortcut. Does not change the DB.
     * 
     * @param item
     */
    public void remove(ShortcutInfo item) {
        contents.remove(item);
    }
    
    @Override
    void onAddToDatabase(ContentValues values) { 
        super.onAddToDatabase(values);
        values.put(LauncherSettings.Favorites.TITLE, title.toString());
    }

    public boolean isOpened() {
        return opened;
    }

    public void setOpened(boolean opened) {
        this.opened = opened;
    }

    public CharSequence getTitle() {
        return title;
    }

    public void setTitle(CharSequence title) {
        this.title = title;
    }

    public void setIcon(Bitmap mIcon) {
        this.mIcon = mIcon;
    }

    public Bitmap getmIconReflection() {
        return mIconReflection;
    }

    public void setmIconReflection(Bitmap mIconReflection) {
        this.mIconReflection = mIconReflection;
    }

    public ArrayList<ShortcutInfo> getContents() {
        return contents;
    }

    public void setContents(ArrayList<ShortcutInfo> contents) {
        this.contents = contents ;
    }
    
    /**
     * 
     * 获得图标文件夹图片
     * @ author: 黄悦
     *@param iconCache
     *@return
     */
    public Bitmap getIcon(IconCache iconCache){
        Bitmap bitmap = iconCache.getIcon(intent);
        if(bitmap == null && iconCache instanceof Mogoo_BitmapCache){
            bitmap = Mogoo_BitmapUtils.createFolderBitmap(iconCache.getContext(), Mogoo_FolderInfo.this);
            ((Mogoo_BitmapCache)iconCache).putFodlerIcon(bitmap, intent);
        }
        
        return bitmap;
    }
    
    
    /**
     * 创建图标文件夹
     *@author: 张永辉
     *@Date：2011-3-18
     *@param target
     *@param source
     */
    public static Mogoo_FolderInfo createFolder(Workspace workspace,int index,Mogoo_BubbleTextView source,int screen){
        Mogoo_FolderInfo info = new Mogoo_FolderInfo() ;
        
//        Workspace workspace = (Workspace)MT_ComponentBus.getInstance().getActivityComp(R.id.workspace, source.getContext()) ;
        
        CellLayout cellLayout = (CellLayout)(workspace.getChildAt(screen)) ;
        
        Mogoo_BubbleTextView target = (Mogoo_BubbleTextView)(cellLayout.getChildAt(index)) ;
        
        ShortcutInfo targetInfo = (ShortcutInfo)(target.getTag()) ;
        
        info.appType = LauncherSettings.Favorites.APP_TYPE_OTHER ;
        info.cellX = targetInfo.cellX ;
        info.cellY = targetInfo.cellY ;
        info.container = LauncherSettings.Favorites.CONTAINER_DESKTOP ;
        info.isSystem = LauncherSettings.Favorites.NOT_SYSTEM_APP ;
        info.itemType = LauncherSettings.Favorites.ITEM_TYPE_MOGOO_FOLDER ;
        info.opened = false ;
        info.screen = screen ;
        info.spanX = targetInfo.spanX ;
        info.spanY = targetInfo.spanY ;
        
        info.title = "New folder" ;
        
        LauncherModel.addItemToDatabase(workspace.getContext(), info, info.container, screen, info.cellX, info.cellY, false) ;
        
        if(info.id != -1){
            info.intent = Mogoo_Utilities.generateMtFolderIntent(info.id) ;
            LauncherModel.updateItemInDatabase(workspace.getContext(), info) ;
            
            targetInfo.cellX = 0 ;
            targetInfo.cellY = 0 ;
            targetInfo.container = info.id ;
                
            info.add(targetInfo) ;
            
            LauncherModel.updateItemInDatabase(workspace.getContext(), targetInfo) ;
            
            if(source!=null){
                ShortcutInfo sourceInfo = (ShortcutInfo)(source.getTag()) ;
                sourceInfo.cellX = 1 ;
                sourceInfo.cellY = 0 ;
                sourceInfo.container = info.id ;
                
                info.add(sourceInfo) ;
                
                LauncherModel.updateItemInDatabase(workspace.getContext(), sourceInfo) ;
            }
        }
        
        return info ;
    }
    
    /**
     * 添加快捷方式图标到文件夹中
     *@author: 张永辉
     *@Date：2011-3-24
     *@param item
     *@return
     */
    public Mogoo_FolderInfo addItem(Context context,ShortcutInfo item){
        if(item instanceof Mogoo_FolderInfo){
            return this;
        }
        
        if(this.id!=-1){
            int size = contents.size() ;
            //列数
            int col = Mogoo_GlobalConfig.getWorkspaceLongAxisCells(Mogoo_GlobalConfig.isLandscape())  ;
            
            item.container = this.id ;
            item.screen = this.screen ;
            item.cellY = size / col ;
            item.cellX = size % col ;
            
            LauncherModel.updateItemInDatabase(context, item) ;
        }
        
        if(!contents.contains(item)){
            add(item) ;
        }
        
        if (Mogoo_GlobalConfig.LOG_DEBUG) {
            Log.d("Mogoo_FolderInfo", " db_id = " + id);
        }
        
        return this ;
    }
    
    /**
     * 从文件夹中删除图标
     *@author: 张永辉
     *@Date：2011-4-11
     *@param context
     *@param item
     *@return
     */
    public Mogoo_FolderInfo removeItem(Context context,ShortcutInfo item)
    {
        LauncherModel.deleteItemFromDatabase(context, item) ;
        
        contents.remove(item) ;
        
        //内容排序
        Collections.sort(contents, new SortByIndex()) ;
        
        //重新排序
        int count = contents.size() ;
        for(int i=0;i<count;i++)
        {
            ShortcutInfo info = contents.get(i) ;
            int [] cellXY = Mogoo_Utilities.convertToCell(i) ;
            info.cellX = cellXY[0] ;
            info.cellY = cellXY[1] ;
            LauncherModel.moveItemInDatabase(context, info, id , screen, info.cellX, info.cellY) ;
        }
        
        return this ;
    }
    
    /**
     * 更新文件夹中的图标
     *@author: 张永辉
     *@Date：2011-4-11
     *@param context
     *@param item
     *@return
     */
    public Mogoo_FolderInfo updateItem(Context context,ShortcutInfo item)
    {
        LauncherModel.updateItemInDatabase(context, item) ;
        
        contents.remove(item) ;
        contents.add(item) ;
        
        return this ;
    }
    
    public boolean isAddRow() {
        return addRow;
    }

    public void setAddRow(boolean addRow) {
        this.addRow = addRow;
    }
    
    /**
     * 用于文件夹内部图标的排序
     */
    static class SortByIndex  implements Comparator{

        public int compare(Object object1, Object object2) {
            ShortcutInfo info1 = (ShortcutInfo) object1 ;
            ShortcutInfo info2 = (ShortcutInfo) object2 ;
            int index1 = Mogoo_Utilities.getIndexByCellXY(info1.cellX, info1.cellY) ;
            int index2 = Mogoo_Utilities.getIndexByCellXY(info2.cellX, info2.cellY) ;
            
            if(index1>index2){
                return 1 ;
            }else if(index1<index2){
                return -1 ;
            }else{
                return 0;  
            }
        }
        
    }

}
