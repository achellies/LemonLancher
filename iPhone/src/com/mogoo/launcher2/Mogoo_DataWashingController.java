/**  
 * 文 件 名:  MT_DataWashingController.java 
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者： 曾少彬                    
 * 版    本:  1.0  
 * 创建时间:   2011-4-20
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-4-20        曾少彬       1.0          1.0 Version  
 */
package com.mogoo.launcher2;

import java.util.ArrayList;
import java.util.List;

import com.mogoo.launcher2.ItemInfo;
import com.mogoo.launcher2.LauncherSettings;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_Utilities;

public class Mogoo_DataWashingController
{
	private static Mogoo_DataWashingController mController;
	private int numberScreens = 0;
	private int numberCellsX = 0;
    private int numberCellsY = 0;
    private int numberMaxDockCount = 0;
    
    private LauncherApplication app = null;
    
	
	private Mogoo_DataWashingController()
	{
		
	}
	
	public static Mogoo_DataWashingController getInstance()
	{
		if(mController == null)
		{
			mController = new Mogoo_DataWashingController();
		}
		
		return mController;
	}
	
	/***
	 * 对传入的数据进行清洗，去除重复、空白的图标，同时给多余的图标找到合适的位置
	 * @param screens 对于当前Launcher， 数组第一项是工具栏，第二项是搜索屏，第三、四、五、六项是图标屏
	 * @param app
	 */
	public void washData(Mogoo_ScreenHolder[] screens, LauncherApplication app)
	{
		this.app = app;
		
		numberScreens = Mogoo_GlobalConfig.getWorkspaceScreenCount();
		numberCellsX = Mogoo_GlobalConfig.getWorkspaceLongAxisCells(false);
	    numberCellsY = Mogoo_GlobalConfig.getWorkspaceShortAxisCells(false);
	    numberMaxDockCount = Mogoo_GlobalConfig.getDockMaxIconCount();
	    
	    if (Mogoo_GlobalConfig.isLandscape()) 
	    {
            numberCellsX = Mogoo_GlobalConfig.getWorkspaceLongAxisCells(true);
            numberCellsY = Mogoo_GlobalConfig.getWorkspaceShortAxisCells(true);
        }
	    
		// 排序
		sortItems(screens);
		
		// 重新放置图标
		placeItems(screens);
	}
	
	/**
	 * 重新编排cellX cellY
	 * @param screens
	 */
	private void placeItems(Mogoo_ScreenHolder[] screens)
	{
		int[] shortcutScreen = Mogoo_GlobalConfig.getShortcutScreen();
		
		// 第一个图标屏的索引号，当工具栏多出图标时，图标会被放到该屏
		int firstShortcutScreen = 1;
		if(shortcutScreen!=null && shortcutScreen.length>0)
			firstShortcutScreen = shortcutScreen[0];
		
		for(int screenIndex = 0; screenIndex < screens.length; screenIndex++)
		{
			List<ItemInfo> items = screens[screenIndex].items;
			for(int i=0;i<items.size();i++)
			{
				ItemInfo item = items.get(i);
				
				int cellX = i % numberCellsX;
				int cellY = i / numberCellsY;
				
				// 处理工具栏的图标，将多余的放到主屏上面
				if(item.container == LauncherSettings.Favorites.CONTAINER_TOOLBAR)
				{
					// 工具栏只有一排，所以cellY都为0
					item.cellY = 0;
					if(i < numberMaxDockCount)
					{
						item.cellX = i;
						
						moveItemInDatabase(item, screenIndex-1);
					}
					else // 如果工具栏装不下，则移动到workspace第一屏的最后一个
					{
						insertToNextScreen(screens, item, firstShortcutScreen);
						moveItemInDatabase(item, firstShortcutScreen);
					}
				}
				else
				{
					// 重排序号
					if(i< numberCellsX*numberCellsY)
					{
						item.cellX = cellX;
						item.cellY = cellY;
					}
					else // 追加到下一屏
					{
						insertToNextScreen(screens, item, screenIndex+1);
						moveItemInDatabase(item, screenIndex+1);
					}
				}
			}
		}
	}
	
	// 移动数据库中的图标
	private void moveItemInDatabase(ItemInfo item, int screen)
	{
		int[] point = Mogoo_Utilities.switchPoint(new int[] {
				item.cellX, item.cellY
        });
		
		ItemInfo itemTemp2 = new ItemInfo(item);
        LauncherModel.moveItemInDatabase(app, itemTemp2,
                LauncherSettings.Favorites.CONTAINER_DESKTOP, screen,
                point[0], point[1]);
	}
	
	/**
	 * 追加某一图标到指定屏的最后一位
	 * @param screens
	 * @param item
	 * @param screenIndex
	 */
	private void insertToNextScreen(Mogoo_ScreenHolder[] screens, ItemInfo item, int screenIndex)
	{
		if(screenIndex >= screens.length)
		{
			return;
		}
		
		Mogoo_ScreenHolder screen = screens[screenIndex];
		
		// 肯定为DESKTOP屏
		item.container = LauncherSettings.Favorites.CONTAINER_DESKTOP;
		item.screen = screenIndex;
		screen.items.add(item);
	}
	
	/**
	 * 对桌面和工具栏图标按cellX cellY 排序
	 * 
	 * @param screens
	 */
	private void sortItems(Mogoo_ScreenHolder[] screens)
	{
		for(int screenIndex = 0; screenIndex < screens.length; screenIndex++)
		{
			List<ItemInfo> items = screens[screenIndex].items;
			int totalCells = 0;
			
			for(int i = 0; i< items.size(); i++)
			{
				totalCells += items.get(i).spanX*items.get(i).spanY;
				
				// 如果前一屏的图标放不下了，则追加到后一屏的末尾
				if(totalCells > numberCellsX * numberCellsY)
				{
					ItemInfo item = items.remove(i);
					if(screenIndex+1 < screens.length)
					{
						screens[screenIndex+1].items.add(item);
					}
					continue;
				}
				
				for(int j=items.size()-1; j>i; j--)
				{
					ItemInfo itemI = items.get(i);
					ItemInfo itemJ = items.get(j);
					
					int indexI = (itemI.cellY-1)*numberCellsY + itemI.cellX;
					int indexJ = (itemJ.cellY-1)*numberCellsY + itemJ.cellX;
					
					// 把更靠前的图标放前面
					if(indexI > indexJ) 
					{
						items.remove(j);
						items.add(i, itemJ);
					}
				}
			}
		}
	}
}

class Mogoo_ScreenHolder
{
	public List<ItemInfo> items;
	
	public Mogoo_ScreenHolder()
	{
		items = new ArrayList<ItemInfo>();
	}
}