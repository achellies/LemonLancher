/**  
 * 文 件 名:  MT_BootRestorePolicyImpl.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：  邓丽霞                    
 * 版    本:  1.0  
 * 创建时间:   2011-4-14
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-4-14       邓丽霞       1.0          1.0 Version  
 */        
package com.mogoo.launcher2.restore;

import com.mogoo.launcher2.CellLayout;
import com.mogoo.launcher2.LauncherApplication;
import com.mogoo.launcher2.LauncherModel;
import com.mogoo.launcher2.LauncherSettings;
import com.mogoo.launcher2.Mogoo_FolderInfo;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_Utilities;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.util.HashMap;
import java.util.Iterator;

public class Mogoo_BootRestorePolicyImpl implements Mogoo_RestorePlolicy{
	
	private static final String TAG = "Mogoo_BootRestorePolicyImpl";
	
	public void runPlolicy(Context context) {
		// TODO Auto-generated method stub
		if(Mogoo_GlobalConfig.LOG_DEBUG)
		{
		    Log.d(TAG, "Mogoo_BootRestorePolicyImpl : runPlolicy");
		}
		LauncherApplication app = (LauncherApplication)context.getApplicationContext();
		LauncherModel laucherModel = app.getModel();
		//添加launcher数据库里没有的应用程序到数据库中
		laucherModel.addInDbForNotAddToDesk();
		
		folderErrorExecute(context);
	}

    private void folderErrorExecute(Context context) {
        final ContentResolver contentResolver = context.getContentResolver();
        //比对文件夹错误部分
        Cursor c = null;
        try {
            c = contentResolver.query(LauncherSettings.Favorites.CONTENT_URI, null,
                    " container > 0 or itemType = 5", null, null);

            HashMap<Long, Integer> folderMap = new HashMap<Long, Integer>();
            HashMap<Long, Integer> itemMap = new HashMap<Long, Integer>();

            final int idIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
            final int containerIndex = c
                    .getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
            final int itemTypeIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);

            long id = -1;
            int container = -1;
            int itemType = -1;

            while (c.moveToNext()) {
                id = c.getLong(idIndex);
                itemType = c.getInt(itemTypeIndex);
                container = c.getInt(containerIndex);
                // 文件夹处理部分
                if (itemType == LauncherSettings.Favorites.ITEM_TYPE_MOGOO_FOLDER) {
                    folderMap.put(id, 0);
                }
                // 图标处理部分
                else if (container > 0) {
                    itemMap.put(id, container);
                }
            }
            
            redundancyFolderExecute(contentResolver, folderMap, itemMap);
            lostFolderExecute(context, folderMap, itemMap);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }
    
    /**
     * 
     * 缺失文件夹处理
     * @ author: 黄悦
     *@param contentResolver
     *@param folderMap
     *@param itemMap
     */
    private void lostFolderExecute(Context context, HashMap<Long, Integer> folderMap, HashMap<Long, Integer> itemMap){
        HashMap<Integer, Mogoo_FolderInfo> idMapping = new HashMap<Integer, Mogoo_FolderInfo>();
        Iterator<Long> itr = itemMap.keySet().iterator();
        Mogoo_FolderInfo folderIndo = null;
        
        while(itr.hasNext()){
            Long itemId = itr.next();
            Integer container = itemMap.get(itemId);
            if(!folderMap.containsKey(container.longValue())){
                if(idMapping.containsKey(container)){
                    folderIndo = idMapping.get(container);
                }else{
                    folderIndo = createFolderInfo(context);
                    idMapping.put(container, folderIndo);
                }
                
                updateContainer(context, itemId, container);
            }
        }
    }

    /**
     * 
     * 刷新容器id
     * @ author: 黄悦
     *@param context
     *@param id
     *@param container
     */
    private void updateContainer(Context context, long id, int container) {
        final ContentValues values = new ContentValues();
        final ContentResolver cr = context.getContentResolver();

        values.put(LauncherSettings.Favorites.CONTAINER, container);
        cr.update(LauncherSettings.Favorites.getContentUri(id, false), values, null, null);
    }
    
    
    
    /**
     * 
     * 创建新的的文件夹容器
     * @ author: 黄悦
     *@return
     */
    private Mogoo_FolderInfo createFolderInfo(Context context){
        Mogoo_FolderInfo folderInfo = new Mogoo_FolderInfo();
        
        int[] shortcutScreen = Mogoo_GlobalConfig.getShortcutScreen();
        for (int i : shortcutScreen) {

            final int[] coordinates; // 存放找出的空位坐标


            if ((coordinates = CellLayout.findBlackCell(context.getApplicationContext(), i)) != null) {
                folderInfo.cellX = coordinates[0];
                folderInfo.cellY = coordinates[1];
                folderInfo.screen = i;
                
                folderInfo.appType = LauncherSettings.Favorites.APP_TYPE_OTHER ;
                folderInfo.container = LauncherSettings.Favorites.CONTAINER_DESKTOP ;
                folderInfo.isSystem = LauncherSettings.Favorites.NOT_SYSTEM_APP ;
                
                folderInfo.title = "New folder" ;
                
                LauncherModel.addItemToDatabase(context, folderInfo, folderInfo.container, folderInfo.screen, folderInfo.cellX, folderInfo.cellY, false) ;
                
                folderInfo.intent = Mogoo_Utilities.generateMtFolderIntent(folderInfo.id) ;
                
                LauncherModel.updateItemInDatabase(context, folderInfo);

                break;
            }
        }
        
        return folderInfo;
    }
    
    /**
     * 
     * 处理冗余的文件夹
     * @ author: 黄悦
     *@param folderMap
     *@param itemMap
     */
    private void redundancyFolderExecute(ContentResolver contentResolver, HashMap<Long, Integer> folderMap, HashMap<Long, Integer> itemMap){
        Iterator<Long> itr = folderMap.keySet().iterator();
        
        while(itr.hasNext()){
            Long id = itr.next();
            if(id == null){
                continue;
            }
            
            if(!itemMap.containsValue(id.intValue())){
                contentResolver.delete(LauncherSettings.Favorites.getContentUri(id, false), null, null);
            }
        }
    }
}
