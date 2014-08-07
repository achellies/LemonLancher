/**  
 * 文 件 名:  MT_GlobalConfig.java  
 * 描    述： 对系统中使用到全集变量进行统一管理  
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者： 魏景春                      
 * 版    本:  1.0  
 * 创建时间:   2011-1-19
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-1-19       魏景春       1.0          1.0 Version  
 */

package com.mogoo.launcher2.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.mogoo.launcher.R;

public class Mogoo_GlobalConfig {

	public static int SCREEN_HEIGHT = 1000;

	public static int SCREEN_WIDTH = 1001;

	public static int SCREEN_SUPPORT_LANDSCAPE = 1002;

	public static int SCREEN_SUPPORT_WIDGET = 1003;

	public static int VIBRATION_VIEW = 1004;

	public static int REFLECTION_WIDTH_PORT = 1100;

	public static int REFLECTION_WIDTH_LAND = 1101;

	public static int REFLECTION_HEIGHT_PORT = 1102;

	public static int REFLECTION_HEIGHT_LAND = 1103;

	public static int REFLECTION_GAP = 1104;

	public static int ICON_WIDTH = 1200;

	public static int ICON_HEIGHT = 1201;

	public static int ICON_RADIAN = 1203;

	public static int FRAME_ANGLE = 1204;

	public static int FRAME_COUNT = 1205;

	// 桌面单元格高度
	public static int WORKSPACE_CELL_HEIGHT = 1300;

	// 桌面单元格宽度
	public static int WORKSPACE_CELL_WIDTH = 1301;

	// 桌面单元格左padding
	public static int WORKSPACE_CELL_PADDING_LEFT = 1302;

	// 桌面单元格右padding
	public static int WORKSPACE_CELL_PADDING_RIGHT = 1303;

	// 桌面单元格上padding
	public static int WORKSPACE_CELL_PADDING_TOP = 1304;

	// 桌面单元格下padding
	public static int WORKSPACE_CELL_PADDING_BOTTOM = 1305;

	public static int WORKSPACE_SHORT_AXIS_CELLS_PORT = 1306;

	public static int WORKSPACE_SHORT_AXIS_CELLS_LAND = 1307;

	public static int WORKSPACE_LONG_AXIS_CELLS_PORT = 1308;

	public static int WORKSPACE_LONG_AXIS_CELLS_LAND = 1309;

	public static int WORKSPACE_DEFAULT_SCREEN = 1310;

	public static int WORKSPACE_SCREEN_COUNT = 1311;

	public static int WORKSPACE_SCREEN_TYPE = 1312;

	// 距离上边距的距离
	public static int WORKSPACE_LONG_AXIS_START_PADDING = 1313;

	// 距离下边距的距离
	public static int WORKSPACE_LONG_AXIS_END_PADDING = 1314;

	// 距离左边距的距离
	public static int WORKSPACE_SHORT_AXIS_START_PADDING = 1315;

	// 距离右边距的距离
	public static int WORKSPACE_SHORT_AXIS_END_PADDING = 1316;

	public static int FRAME_ZERO_ANGLE = 1317;

	public static int WORKSPACE_CELL_MARGIN_TOP = 1318;

	public static int WORKSPACE_CELL_MARGIN_LEFT = 1319;

	public static int WORKSPACE_CELL_MARGIN_RIGHT = 1320;

	public static int WORKSPACE_CELL_MARGIN_BOTTOM = 1321;

	public static int DOCK_MAX_ICON_COUNT_LAND = 1400;

	public static int DOCK_MAX_ICON_COUNT_PORT = 1401;

	public static int DOCK_CELL_HEIGHT = 1402;

	public static int DOCK_HEIGHT = 1601;

	public static int DOCK_CELL_WIDTH = 1403;

	public static int DOCK_CELL_PADDING_LEFT = 1404;

	public static int DOCK_CELL_PADDING_RIGHT = 1405;

	public static int DOCK_CELL_PADDING_TOP = 1406;

	public static int IPHONE_FOLDER_START_Y = 1500;

	// 操作系统版本
	public static final int OS_VERSION = 1600;

	// 文件夹生成的面积百分比
	public static int FOLDER_GENERATE_AREA_RATE = 1700;

	// 文件夹接受桌面图标的面积百分比
	public static int FOLDER_ACCEPT_AREA_RATE = 1701;

	// 文件夹是否打开的停留时间是值
	public static int FOLDER_STAY_TIME = 1702;

	public static int FOLDER_OPEN_BOTTOM_HEIGHT = 1703;
    //add by 袁业奔 2011-9-7
	public static int WORKSPACE_SCREEN_MAXCOUNT = 1704;
	public static int WORKSPACE_SCREEN_MINCOUNT = 1705;
	//end
    //add by 袁业奔 2011-9-21
	//指示器点之间的距离
	public static int INDICATOR_MARGIN=1706;
	//出现文件夹图标时绘制的宽
	public static int ICON_FODLER_BG_WIDTH=1077;
	//出现文件夹图标时绘制的高
	public static int ICON_FODLER_BG_HEIGHT=1078;
	//文件夹图标中的图标缩略图
	public static int SCALE_ICON_SIZE=1079;
    public static final int SCALE_CELL_SIZE = 1080;
    public static final int FOLDER_PADDING_SIZE = 1081;
    public static final int SCALE_ICON_PADDING_SIZE = 1082;
	//end
    public static final int LOCK_MUSIC_PANEL = 1083;
    //图标圆角
    public static final int RADII = 1084;
    
    //update by 袁业奔 2011-10-25 
//	public static int ICON_SCALE_SIZE = 45;//45
	public static final int ICON_SCALE_SIZE = 1085;
	//end
	//图标上方短信计数器数字字体大小
	public static final int ICON_COUNT_INFO_TEXT_SIZE = 1086;
    //add by huangyue 2011-11-4 
//	public static int ICON_SCALE_SIZE = 45;//45
	public static final int DATE_TEXT_SIZE = 1087;
	public static final int DAY_TEXT_SIZE = 1088;
	public static final int DATE_HEIGHT_FIX_VALUE = 1089;
	//end
    
	public static final String OS_VERSION_2_1 = "2.1";

	public static final String OS_VERSION_2_2 = "2.2";

	public static boolean LOG_DEBUG = false;

	public static boolean LOG_INFO = true;

	public static boolean LOG_WARN = true;

	public static boolean LOG_ERROR = true;

	public static final int SCREEN_TYPE_SEARCH = 1;

	public static final int SCREEN_TYPE_SHORTCUT = 2;

	public static final int SCREEN_TYPE_WIDGET = 3;

	// 目标类型
	public final static int TARGET_NULL = 0; // 空白单元格

	public final static int TARGET_SHORTCUT = 1;// 快捷方式

	public final static int TARGET_FOLDER = 2;// 文件夹

	private static HashMap<Integer, Object> configCache = new HashMap<Integer, Object>();

	// TODO: disabled by achellies
	//private static WindowManager windowManager = WindowManagerImpl.getDefault();
	private static WindowManager windowManager;

	public static boolean PLAY_ANIMATION = true;

	public static final int FOLDER_BASE_INDEX = 10000;

	public static final boolean DEAL_WITH_SYSTEM_ICON = true;

	public static final boolean ICON_FOLDER = true;

	// land模式下单元格数
	private static int workspaceCellCountsForLandscape = 0;
	// port模式下单元格数
	private static int workspaceCellCountsForPortscape = 0;

	/**
	 * 初始化全局配置参数 @ author: 魏景春
	 */
	public static void init() {

		configCache.clear();
        //update by 袁业奔 2011-9-20
//		configCache.put(ICON_WIDTH, 86);
//		configCache.put(ICON_HEIGHT, 86);
		//end
		configCache.put(SCREEN_SUPPORT_LANDSCAPE, false);
		// configCache.put(WORKSPACE_SCREEN_TYPE, new int
		// []{SCREEN_TYPE_SEARCH,SCREEN_TYPE_SHORTCUT,
		// SCREEN_TYPE_SHORTCUT,SCREEN_TYPE_SHORTCUT,SCREEN_TYPE_SHORTCUT,SCREEN_TYPE_WIDGET});
		configCache.put(WORKSPACE_SCREEN_TYPE, new int[] { SCREEN_TYPE_SEARCH,
				SCREEN_TYPE_SHORTCUT, SCREEN_TYPE_SHORTCUT,
				SCREEN_TYPE_SHORTCUT, SCREEN_TYPE_SHORTCUT,
				SCREEN_TYPE_SHORTCUT });
		configCache.put(WORKSPACE_LONG_AXIS_CELLS_PORT, 4);
		configCache.put(WORKSPACE_SHORT_AXIS_CELLS_PORT, 4);
		configCache.put(WORKSPACE_LONG_AXIS_CELLS_LAND, 4);
		configCache.put(WORKSPACE_SHORT_AXIS_CELLS_LAND, 4);

		// float[] angles = {-2f,-1.5f, -0.5f, 0.5f,1.5f,2f};
//		float[] angles = { -1.8f, -1.3f, -0.6f, 0.6f, 1.3f, 1.8f };
		float[] angles = { -1.3f,  -0.3f,0.3f,1.3f };
		configCache.put(FRAME_ZERO_ANGLE, 3);
		configCache.put(FRAME_ANGLE, angles);
		configCache.put(FRAME_COUNT, angles.length);
		int[] vibrationView = { R.id.workspace, R.id.dockWorkSpace,
				R.id.folderWorkspace };
		configCache.put(VIBRATION_VIEW, vibrationView);
        //update by 袁业奔 2011-9-20
//		configCache.put(REFLECTION_WIDTH_PORT, 86);
//		configCache.put(REFLECTION_HEIGHT_PORT, 30);
		//end
		configCache.put(REFLECTION_WIDTH_LAND, 57);
		configCache.put(REFLECTION_HEIGHT_LAND, 21);
		configCache.put(REFLECTION_GAP, 1);

		configCache.put(FOLDER_GENERATE_AREA_RATE, 0.7f);
		configCache.put(FOLDER_ACCEPT_AREA_RATE, 0.65f);
		configCache.put(FOLDER_STAY_TIME, 1000);
		configCache.put(FOLDER_OPEN_BOTTOM_HEIGHT, 50);

		configCache.put(OS_VERSION, OS_VERSION_2_2);
        //add by 袁业奔 2011-9-7
		configCache.put(WORKSPACE_SCREEN_MAXCOUNT,13);
		configCache.put(WORKSPACE_SCREEN_MINCOUNT,2);
		//end
		//add by yeben 2011-10-12 是否隐藏音乐控制面板
		configCache.put(LOCK_MUSIC_PANEL,false);
		//end
	}

	static {
		init();
	}

	/**
	 * 初始化日志级别 @ author: 魏景春
	 * 
	 * @param logLevel
	 *            日志级别(1: debug;2:info;3:warn;4:error)
	 */
	public static void initLogLevel(int logLevel) {

	}

	/**
	 * 添加配置参数 @ author: 魏景春
	 * 
	 * @param key
	 *            参数项到key值
	 * @param value
	 *            参数值
	 */
	public static void setConfigParm(int key, Object value) {
		configCache.put(key, value);

	}
	
	public static int getIntByKey(int key) {
		return (Integer) configCache.get(key);
	}

	public static int getRefectionWidth() {
		if (isPortrait()) {
			return (Integer) configCache.get(REFLECTION_WIDTH_PORT);
		} else {
			return (Integer) configCache.get(REFLECTION_WIDTH_LAND);
		}

	}

	public static int getReflectionHeight() {
		if (isPortrait()) {
			return (Integer) configCache.get(REFLECTION_HEIGHT_PORT);
		} else {
			return (Integer) configCache.get(REFLECTION_HEIGHT_LAND);
		}
	}

	public static int getReflectionGap() {
		return (Integer) configCache.get(REFLECTION_GAP);
	}

	public static int getIconWidth() {
		return (Integer) configCache.get(ICON_WIDTH);
	}
  
	public static int getIconHeight() {
		return (Integer) configCache.get(ICON_HEIGHT);
	}

	public static int getIconRadian() {
		return 0;
	}

	public static float[] getFrameAngle() {
		return (float[]) configCache.get(FRAME_ANGLE);
	}

	public static int getFrameCount() {
		return (Integer) configCache.get(FRAME_COUNT);
	}

	public static int getZeroAngleFrame() {
		return (Integer) configCache.get(FRAME_ZERO_ANGLE);
	}

	/**
	 * 取得单元格的高度 @ author: 张永辉
	 * 
	 * @return 返回单元格的高度
	 */
	public static int getWorkspaceCellHeight() {
		if (configCache.get(WORKSPACE_CELL_HEIGHT) != null) {
			return (Integer) configCache.get(WORKSPACE_CELL_HEIGHT);
		} else {
			return 0;
		}
	}

	/**
	 * 取得单元格的宽度 @ author: 张永辉
	 * 
	 * @return
	 */
	public static int getWorkspaceCellWidth() {
		if (configCache.get(WORKSPACE_CELL_WIDTH) != null) {
			return (Integer) configCache.get(WORKSPACE_CELL_WIDTH);
		} else {
			return 0;
		}
	}

	/**
	 * 取得单元格的左pading @ author: 张永辉
	 * 
	 * @return
	 */
	public static int getWorkspaceCellPaddingLeft() {
		if (configCache.get(WORKSPACE_CELL_PADDING_LEFT) != null) {
			return (Integer) configCache.get(WORKSPACE_CELL_PADDING_LEFT);
		} else {
			return 5;
		}
	}

	/**
	 * 取得单元格的右pading @ author: 张永辉
	 * 
	 * @return
	 */
	public static int getWorkspaceCellPaddingRight() {
		if (configCache.get(WORKSPACE_CELL_PADDING_RIGHT) != null) {
			return (Integer) configCache.get(WORKSPACE_CELL_PADDING_RIGHT);
		} else {
			return 5;
		}
	}

	/**
	 * 取得单元格的上pading @ author: 张永辉
	 * 
	 * @return
	 */
	public static int getWorkspaceCellPaddingTop() {
		if (configCache.get(WORKSPACE_CELL_PADDING_TOP) != null) {
			return (Integer) configCache.get(WORKSPACE_CELL_PADDING_TOP);
		} else {
			return 5;
		}
	}

	/**
	 * 取得单元格的下pading @ author: 张永辉
	 * 
	 * @return
	 */
	public static int getWorkspaceCellPaddingBottom() {
		if (configCache.get(WORKSPACE_CELL_PADDING_BOTTOM) != null) {
			return (Integer) configCache.get(WORKSPACE_CELL_PADDING_BOTTOM);
		} else {
			return 5;
		}
	}

	/**
	 * marginTop
	 * 
	 * @author: 张永辉
	 * @Date：2011-3-25
	 * @return
	 */
	public static int getWorkspaceCellMarginTop() {
		// if(configCache.get(WORKSPACE_CELL_MARGIN_TOP)!=null){
		// return (Integer)configCache.get(WORKSPACE_CELL_MARGIN_TOP) ;
		// }else{
		// return 6 ;
		// }
		return 6;
	}

	/**
	 * marginLeft
	 * 
	 * @author: 张永辉
	 * @Date：2011-3-25
	 * @return
	 */
	public static int getWorkspaceCellMarginLeft() {
		if (configCache.get(WORKSPACE_CELL_MARGIN_LEFT) != null) {
			return (Integer) configCache.get(WORKSPACE_CELL_MARGIN_LEFT);
		} else {
			return 4;
		}
	}

	/**
	 * marginRight
	 * 
	 * @author: 张永辉
	 * @Date：2011-3-25
	 * @return
	 */
	public static int getWorkspaceCellMarginRight() {
		if (configCache.get(WORKSPACE_CELL_MARGIN_RIGHT) != null) {
			return (Integer) configCache.get(WORKSPACE_CELL_MARGIN_RIGHT);
		} else {
			return 4;
		}
	}

	/**
	 * marginBottom
	 * 
	 * @author: 张永辉
	 * @Date：2011-3-25
	 * @return
	 */
	public static int getWorkspaceCellMarginBottom() {
		if (configCache.get(WORKSPACE_CELL_MARGIN_BOTTOM) != null) {
			return (Integer) configCache.get(WORKSPACE_CELL_MARGIN_BOTTOM);
		} else {
			return 4;
		}
	}

	/**
	 * 取得竖屏行数 @ author: 张永辉
	 * 
	 * @return 返回竖屏行数
	 */
	public static int getWorkspaceShortAxisCellsPort() {
		if (configCache.get(WORKSPACE_SHORT_AXIS_CELLS_PORT) != null) {
			return (Integer) configCache.get(WORKSPACE_SHORT_AXIS_CELLS_PORT);
		} else {
			return 4;
		}
	}

	/**
	 * 取得竖屏列数 @ author: 张永辉
	 * 
	 * @return 返回竖屏列数
	 */
	public static int getWorkspaceLongAxisCellsPort() {
		if (configCache.get(WORKSPACE_LONG_AXIS_CELLS_PORT) != null) {
			return (Integer) configCache.get(WORKSPACE_LONG_AXIS_CELLS_PORT);
		} else {
			return 4;
		}
	}

	/**
	 * 取得横屏行数 @ author: 张永辉
	 * 
	 * @return 返回横屏行数
	 */
	public static int getWorkspaceShortAxisCellsLand() {
		if (configCache.get(WORKSPACE_SHORT_AXIS_CELLS_LAND) != null) {
			return (Integer) configCache.get(WORKSPACE_SHORT_AXIS_CELLS_LAND);
		} else {
			return 2;
		}
	}

	/**
	 * 取得横屏列数 @ author: 张永辉
	 * 
	 * @return 返回横屏列数
	 */
	public static int getWorkspaceLongAxisCellsLand() {
		if (configCache.get(WORKSPACE_LONG_AXIS_CELLS_LAND) != null) {
			return (Integer) configCache.get(WORKSPACE_LONG_AXIS_CELLS_LAND);
		} else {
			return 8;
		}
	}

	/**
	 * 取得当前屏幕类型的列数 @ author: 张永辉
	 * 
	 * @param isLandscape
	 *            是否为横屏
	 * @return 返回单元格列数
	 */
	public static int getWorkspaceLongAxisCells(boolean isLandscape) {
		if (isLandscape) {
			return getWorkspaceLongAxisCellsLand();
		} else {
			return getWorkspaceLongAxisCellsPort();
		}
	}

	/**
	 * 取得肖前屏幕类型的行数 @ author: 张永辉
	 * 
	 * @param isLandscape
	 *            是否为横屏
	 * @return 返回单元格行数
	 */
	public static int getWorkspaceShortAxisCells(boolean isLandscape) {
		if (isLandscape) {
			return getWorkspaceShortAxisCellsLand();
		} else {
			return getWorkspaceShortAxisCellsPort();
		}
	}

	/**
	 * 取得当前屏幕总到单元个数 @ author: 魏景春
	 * 
	 * @return
	 */
	public static int getWorkspaceCellCounts() {
		if (isLandscape()) {
			if (workspaceCellCountsForLandscape == 0) {
				workspaceCellCountsForLandscape = getWorkspaceShortAxisCellsLand()
						* getWorkspaceLongAxisCellsLand();
			}

			return workspaceCellCountsForLandscape;

		} else {
			if (workspaceCellCountsForPortscape == 0) {
				workspaceCellCountsForPortscape = getWorkspaceShortAxisCellsPort()
						* getWorkspaceLongAxisCellsPort();
			}

			return workspaceCellCountsForPortscape;
		}
	}

	/**
	 * 取得默认屏序号 @ author: 张永辉
	 * 
	 * @return 返回默认屏序号
	 */
	public static int getWorkspaceDefaultScreen() {
		if (configCache.get(WORKSPACE_DEFAULT_SCREEN) != null) {
			return (Integer) configCache.get(WORKSPACE_DEFAULT_SCREEN);
		} else {
			return 1;
		}
	}

	/**
	 * 返回屏幕总数 @ author: 张永辉
	 * 
	 * @return 返回屏幕总数
	 */
	public static int getWorkspaceScreenCount() {
		if (configCache.get(WORKSPACE_SCREEN_COUNT) != null) {
			return (Integer) configCache.get(WORKSPACE_SCREEN_COUNT);
		} else {
			return 5;
		}
	}

	/**
	 * 取得各屏幕所属分类 @ author: 张永辉
	 * 
	 * @return
	 */
	public static int[] getWorkspaceScreenType() {
		if (configCache.get(WORKSPACE_SCREEN_TYPE) != null) {
			return (int[]) configCache.get(WORKSPACE_SCREEN_TYPE);
		} else {
			return new int[] { SCREEN_TYPE_SEARCH, SCREEN_TYPE_SHORTCUT,
					SCREEN_TYPE_SHORTCUT, SCREEN_TYPE_SHORTCUT,
					SCREEN_TYPE_WIDGET };
		}

	}

	/**
	 * 取得单元格与屏幕左边的距离 @ author: 张永辉
	 * 
	 * @return
	 */
	public static int getWorkspaceLongAxisStartPadding() {
		if (configCache.get(WORKSPACE_LONG_AXIS_START_PADDING) != null) {
			return (Integer) configCache.get(WORKSPACE_LONG_AXIS_START_PADDING);
		} else {
			return 0;
		}
	}

	/**
	 * 取得单元格与屏幕上边的距离 @ author: 张永辉
	 * 
	 * @return
	 */
	public static int getWorkspaceShortAxisStartPadding() {
		if (configCache.get(WORKSPACE_SHORT_AXIS_START_PADDING) != null) {
			return (Integer) configCache
					.get(WORKSPACE_SHORT_AXIS_START_PADDING);
		} else {
			return 0;
		}
	}

	/**
	 * 取得单元格与屏幕右边的距离 @ author: 张永辉
	 * 
	 * @return
	 */
	public static int getWorkspaceLongAxisEndPadding() {
		if (configCache.get(WORKSPACE_LONG_AXIS_END_PADDING) != null) {
			return (Integer) configCache.get(WORKSPACE_LONG_AXIS_END_PADDING);
		} else {
			return 0;
		}
	}

	/**
	 * 取得单元格与屏幕下边的距离 @ author: 张永辉
	 * 
	 * @return
	 */
	public static int getWorkspaceShortAxisEndPadding() {
		if (configCache.get(WORKSPACE_SHORT_AXIS_END_PADDING) != null) {
			return (Integer) configCache.get(WORKSPACE_SHORT_AXIS_END_PADDING);
		} else {
			return 0;
		}
	}

	public static int getDockHeight() {
		return (Integer) configCache.get(Mogoo_GlobalConfig.DOCK_HEIGHT);
	}

	public static int getDockMaxIconCount() {

		return 4;
	}

	public static int getDockCellHeight() {
		return 0;
	}

	public static int getDockCellWidth() {
		return 0;
	}

	public static int getDockCellPaddingLeft() {
		return 0;
	}

	public static int getDockCellPaddingTop() {
		return 0;
	}

	public static int getIphoneStartY() {
		return 0;
	}
	
	public static void initWindowManager(Context context) {
		windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
	}

	/**
	 * 获取屏幕的宽度 @ author: 魏景春
	 * 
	 * @return 屏幕到宽度
	 */
	public static int getScreenWidth() {
		if (configCache.get(SCREEN_WIDTH) != null) {
			return (Integer) configCache.get(SCREEN_WIDTH);
		} else {
			DisplayMetrics dm = new DisplayMetrics();
			windowManager.getDefaultDisplay().getMetrics(dm);
			configCache.put(SCREEN_WIDTH, dm.widthPixels);
			configCache.put(SCREEN_HEIGHT, dm.heightPixels);
			return dm.widthPixels;

		}
	}

	/**
	 * 获取屏幕的高度 @ author: 魏景春
	 * 
	 * @return 屏幕到高度
	 */
	public static int getScreenHeight() {
		if (configCache.get(SCREEN_HEIGHT) != null) {
			return (Integer) configCache.get(SCREEN_HEIGHT);
		} else {
			DisplayMetrics dm = new DisplayMetrics();
			windowManager.getDefaultDisplay().getMetrics(dm);
			configCache.put(SCREEN_WIDTH, dm.widthPixels);
			configCache.put(SCREEN_HEIGHT, dm.heightPixels);
			return dm.heightPixels;
		}
	}

	public static int[] getVibrationViewID() {
		return (int[]) configCache.get(VIBRATION_VIEW);
	}

	public static boolean isLandscape() {

		if (windowManager.getDefaultDisplay().getHeight() < windowManager
				.getDefaultDisplay().getWidth()) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isPortrait() {
		if (windowManager.getDefaultDisplay().getHeight() > windowManager
				.getDefaultDisplay().getWidth()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 是否支持横屏 @ author: 张永辉
	 * 
	 * @return
	 */
	public static boolean isSupportLandscape() {
		if (configCache.get(SCREEN_SUPPORT_LANDSCAPE) != null) {
			return (Boolean) configCache.get(SCREEN_SUPPORT_LANDSCAPE);
		} else {
			return false;
		}
	}

	public static boolean isSupportWidget() {
		return false;
	}

	/**
	 * 操作系统版本
	 * 
	 * @author: 张永辉
	 * @return
	 */
	public static String getOsVersion() {
		if (configCache.get(OS_VERSION) != null) {
			return (String) configCache.get(OS_VERSION);
		} else {
			return OS_VERSION_2_2;
		}
	}

	/**
	 * 取得生成图标文件夹的面积比例
	 * 
	 * @author: 张永辉
	 * @Date：2011-3-18
	 * @return
	 */
	public static float getFolderGenerateAreaRate() {
		if (configCache.get(FOLDER_GENERATE_AREA_RATE) != null) {
			return (Float) configCache.get(FOLDER_GENERATE_AREA_RATE);
		} else {
			return 0.8f;
		}
	}

	/**
	 * 取得图标文件夹接受的面积比例
	 * 
	 * @author: 张永辉
	 * @Date：2011-3-18
	 * @return
	 */
	public static float getFolderAcceptAreaRate() {
		if (configCache.get(FOLDER_ACCEPT_AREA_RATE) != null) {
			return (Float) configCache.get(FOLDER_ACCEPT_AREA_RATE);
		} else {
			return 0.6f;
		}
	}

	/**
	 * 图标文件夹打开的停留时间
	 * 
	 * @author: 张永辉
	 * @Date：2011-3-21
	 * @return
	 */
	public static int getFolderStayTime() {
		if (configCache.get(FOLDER_STAY_TIME) != null) {
			return (Integer) configCache.get(FOLDER_STAY_TIME);
		} else {
			return 1000;
		}
	}

	public static int getFolderOpenBottomHeight() {
		if (configCache.get(FOLDER_OPEN_BOTTOM_HEIGHT) != null) {
			return (Integer) configCache.get(FOLDER_OPEN_BOTTOM_HEIGHT);
		} else {
			return 50;
		}
	}

	/**
	 * 取得widget屏的屏幕序号 @ author: 张永辉
	 * 
	 * @return 返回widget屏序号数组
	 */
	public static int[] getWidgetScreen() {
		List<Integer> widgetList = new ArrayList<Integer>();

		int[] array = getWorkspaceScreenType();
		int len = array.length;

		for (int i = 0; i < len; i++) {
			if (array[i] == SCREEN_TYPE_WIDGET) {
				widgetList.add(i);
			}
		}

		Object[] objs = widgetList.toArray();

		len = objs.length;

		int[] widget = new int[len]; // 用于保存widget屏序号

		for (int i = 0; i < len; i++) {
			widget[i] = (Integer) (objs[i]);
		}

		return widget;

	}

	/**
	 * 取得shortcut屏的屏幕序号 @ author: 张永辉
	 * 
	 * @return 返回shortcut屏序号数组
	 */
	public static int[] getShortcutScreen() {
		List<Integer> shortcutList = new ArrayList<Integer>();

		int[] array = getWorkspaceScreenType();
		int len = array.length;

		for (int i = 0; i < len; i++) {
			if (array[i] == SCREEN_TYPE_SHORTCUT) {
				shortcutList.add(i);
			}
		}

		Object[] objs = shortcutList.toArray();

		len = objs.length;

		int[] shortcut = new int[len]; // 用于保存shortcut屏序号

		for (int i = 0; i < len; i++) {
			shortcut[i] = (Integer) (objs[i]);
		}

		return shortcut;
	}

	/**
	 * 取得search屏的屏幕序号 @ author: 张永辉
	 * 
	 * @return 返回search屏序号
	 */
	public static int getSearchScreen() {
		List<Integer> searchList = new ArrayList<Integer>();

		int[] array = getWorkspaceScreenType();
		int len = array.length;

		for (int i = 0; i < len; i++) {
			if (array[i] == SCREEN_TYPE_SEARCH) {
				searchList.add(i);
			}
		}

		Object[] objs = searchList.toArray();

		len = objs.length;

		int[] search = new int[len]; // 用于保存shortcut屏序号

		for (int i = 0; i < len; i++) {
			search[i] = (Integer) (objs[i]);
		}

		if (search != null && search.length > 0) {
			return search[0];
		} else {
			return 0;
		}
	}

	/**
	 * 判断序号为screen屏是否为快捷方式屏 @ author: 张永辉
	 * 
	 * @param screen
	 *            屏幕序号
	 * @return
	 */
	public static boolean isShortcutScreen(int screen) {

		if (screen < 0 || screen >= getWorkspaceScreenType().length) {
			return false;
		}

		if (getWorkspaceScreenType()[screen] == SCREEN_TYPE_SHORTCUT) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 判断序号为screen的屏是否为搜索屏 @ author: 张永辉
	 * 
	 * @param screen
	 *            屏幕序号
	 * @return
	 */
	public static boolean isSearchScreen(int screen) {

		if (screen < 0 || screen >= getWorkspaceScreenType().length) {
			return false;
		}

		if (getWorkspaceScreenType()[screen] == SCREEN_TYPE_SEARCH) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 判断序号为screen的屏是否为widget屏 @ author: 张永辉
	 * 
	 * @param screen
	 *            屏幕序号
	 * @return
	 */
	public static boolean isWidgetScreen(int screen) {

		if (screen < 0 || screen >= getWorkspaceScreenType().length) {
			return false;
		}

		if (getWorkspaceScreenType()[screen] == SCREEN_TYPE_WIDGET) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 单元格面积
	 * 
	 * @author: 张永辉
	 * @Date：2011-3-18
	 * @return
	 */
	public static float getCellArea() {
		return getWorkspaceCellWidth() * getWorkspaceCellHeight();
	}
	//add by 袁业奔 2011-9-7
	/**
	 * 允许最大屏数
	 * @author: 袁业奔
	 * @Date：2011-9-7
	 * @return
	 */
	public static int getWorkspaceScreenMaxCount(){
		return (Integer)configCache.get(WORKSPACE_SCREEN_MAXCOUNT);
	}
	/**
	 * 允许最小屏数
	 * @author: 袁业奔
	 * @Date：2011-9-7
	 * @return
	 */
	public static int getWorkspaceScreenMinCount(){
		return (Integer)configCache.get(WORKSPACE_SCREEN_MINCOUNT);
	}
	//end
	/**
	 * 指示器点间距
	 * @author: 袁业奔
	 * @Date：2011-9-21
	 * @return
	 */
	public static int getIndicatorMargin(){
		return (Integer)configCache.get(INDICATOR_MARGIN);
	}
	/**
	 * 文件夹背景绘制宽度
	 * @author: 袁业奔
	 * @Date：2011-9-21
	 * @return
	 */
	public static int getIconFolderBgWidth(){
		return (Integer)configCache.get(ICON_FODLER_BG_WIDTH);
	}
	/**
	 * 文件夹背景绘制高度
	 * @author: 袁业奔
	 * @Date：2011-9-21
	 * @return
	 */
	public static int getIconFolderBgHeight(){
		return (Integer)configCache.get(ICON_FODLER_BG_HEIGHT);
	}
	/**
	 * 文件夹图标中的图标缩略图缩放比例
	 * @author: 袁业奔
	 * @Date：2011-9-21
	 * @return
	 */
	public static int getScaleIconSize(){
		return (Integer)configCache.get(SCALE_ICON_SIZE);
	}
	public static float getScaleCellSize(){
		return (Float)configCache.get(SCALE_CELL_SIZE);
	}
	public static float getFolderPaddingSize(){
		return (Float)configCache.get(FOLDER_PADDING_SIZE);
	}
	public static float getScallIconPaddingSize(){
		return (Float)configCache.get(SCALE_ICON_PADDING_SIZE);
	}
	
	/**
	 * 动态生成数组
	 * @author: 袁业奔
	 * @Date：2011-9-28
	 * @return
	 */
	public static int[] createWorkspaceScreenType(int screenCount){
		if(screenCount<=0){
			screenCount=1;
		}
		int[] screenType=new int[screenCount];
		for (int i = 0; i < screenType.length; i++) {
			if(i==0){
				screenType[i] = SCREEN_TYPE_SEARCH;
			}else{
				screenType[i] = SCREEN_TYPE_SHORTCUT;
			}
		}
		return screenType;
	}
	/**
	 * 是否锁定隐藏工具栏中的音乐播放器
	 * @return
	 */
	public static boolean isLockMusicPanel() {
		if (configCache.get(LOCK_MUSIC_PANEL) != null) {
			return (Boolean) configCache.get(LOCK_MUSIC_PANEL);
		} else {
			return false;
		}
	}
	
	public static int getRadii(){
		return (Integer)configCache.get(RADII);
	}
	
	public static int geticonScaleSize(){
		return (Integer)configCache.get(ICON_SCALE_SIZE);
	}
	public static float getIconCountInfoTextSize(){
		return (Float)configCache.get(ICON_COUNT_INFO_TEXT_SIZE);
	}
	public static int getDateHeightFixValue(){
		return (Integer)configCache.get(DATE_HEIGHT_FIX_VALUE);
	}
}  
