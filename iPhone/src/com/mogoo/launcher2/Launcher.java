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

//import com.android.common.Search;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.app.StatusBarManager;
import android.app.WallpaperManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.Process;
import android.os.SystemClock;
import android.provider.CallLog;
import android.provider.LiveFolders;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.exception.Mogoo_BootRestoreException;
import com.mogoo.launcher2.restore.Mogoo_RestoreController;
import com.mogoo.launcher2.restore.Mogoo_UncaughtExceptionHandler;
import com.mogoo.launcher2.search.ui.SearchLayout;
import com.mogoo.launcher2.taskManager.Mogoo_TaskManager;
import com.mogoo.launcher2.utils.CalendarUtils;
import com.mogoo.launcher2.utils.ClickUtils;
import com.mogoo.launcher2.utils.ClickUtils.IBEBroadcastReceiver;
import com.mogoo.launcher2.utils.Mogoo_BitmapCache;
import com.mogoo.launcher2.utils.Mogoo_BitmapUtils;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;

/**
 * Default launcher application.
 */
public final class Launcher extends Activity implements View.OnClickListener,
		OnLongClickListener, LauncherModel.Callbacks {
	static final String TAG = "Launcher";

	static final boolean LOGD = false;

	static final boolean PROFILE_STARTUP = false;

	static final boolean DEBUG_WIDGETS = false;

	static final boolean DEBUG_USER_INTERFACE = false;

	private static final int WALLPAPER_SCREENS_SPAN = 2;

	private static final int MENU_GROUP_ADD = 1;

	private static final int MENU_GROUP_WALLPAPER = MENU_GROUP_ADD + 1;

	private static final int MENU_ADD = Menu.FIRST + 1;

	private static final int MENU_WALLPAPER_SETTINGS = MENU_ADD + 1;

	private static final int MENU_SEARCH = MENU_WALLPAPER_SETTINGS + 1;

	private static final int MENU_NOTIFICATIONS = MENU_SEARCH + 1;

	private static final int MENU_SETTINGS = MENU_NOTIFICATIONS + 1;

	// add by 张永辉 增加widget
	private static final int MENU_ADD_WIDGET = MENU_SETTINGS + 1;

	// end

	// denglixia add 2011.4.20
	private static final int MENU_REFRESH = MENU_ADD_WIDGET + 1;
	// denglixia add end 2011.4.20
	private static final int REQUEST_CREATE_SHORTCUT = 1;

	private static final int REQUEST_CREATE_LIVE_FOLDER = 4;

	private static final int REQUEST_CREATE_APPWIDGET = 5;

	private static final int REQUEST_PICK_APPLICATION = 6;

	private static final int REQUEST_PICK_SHORTCUT = 7;

	private static final int REQUEST_PICK_LIVE_FOLDER = 8;

	private static final int REQUEST_PICK_APPWIDGET = 9;

	private static final int REQUEST_PICK_WALLPAPER = 10;

	static final String EXTRA_SHORTCUT_DUPLICATE = "duplicate";

	static final int SCREEN_COUNT = 5;

	static final int DEFAULT_SCREEN = 2;

	static final int NUMBER_CELLS_X = 4;

	static final int NUMBER_CELLS_Y = 4;

	static final int DIALOG_CREATE_SHORTCUT = 1;

	static final int DIALOG_RENAME_FOLDER = 2;

	private static final String PREFERENCES = "launcher.preferences";

	// Type: int
	private static final String RUNTIME_STATE_CURRENT_SCREEN = "launcher.current_screen";

	// Type: boolean
	private static final String RUNTIME_STATE_ALL_APPS_FOLDER = "launcher.all_apps_folder";

	// Type: long
	private static final String RUNTIME_STATE_USER_FOLDERS = "launcher.user_folder";

	// Type: int
	private static final String RUNTIME_STATE_PENDING_ADD_SCREEN = "launcher.add_screen";

	// Type: int
	private static final String RUNTIME_STATE_PENDING_ADD_CELL_X = "launcher.add_cellX";

	// Type: int
	private static final String RUNTIME_STATE_PENDING_ADD_CELL_Y = "launcher.add_cellY";

	// Type: int
	private static final String RUNTIME_STATE_PENDING_ADD_SPAN_X = "launcher.add_spanX";

	// Type: int
	private static final String RUNTIME_STATE_PENDING_ADD_SPAN_Y = "launcher.add_spanY";

	// Type: int
	private static final String RUNTIME_STATE_PENDING_ADD_COUNT_X = "launcher.add_countX";

	// Type: int
	private static final String RUNTIME_STATE_PENDING_ADD_COUNT_Y = "launcher.add_countY";

	// Type: int[]
	private static final String RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS = "launcher.add_occupied_cells";

	// Type: boolean
	private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME = "launcher.rename_folder";

	// Type: long
	private static final String RUNTIME_STATE_PENDING_FOLDER_RENAME_ID = "launcher.rename_folder_id";

	static final int APPWIDGET_HOST_ID = 1024;

	private static final Object sLock = new Object();

	private static int sScreen = DEFAULT_SCREEN;

	// denglixia modify 2011.6.17
	// private final BroadcastReceiver mCloseSystemDialogsReceiver = new
	// CloseSystemDialogsIntentReceiver();
	private final BroadcastReceiver mLauncherBroadcastReceiver = new LauncherBroadcastReceiver();
	// denglixia add 2011.6.17
	private IntentFilter mIntentFilter;
	private static final String POWER_ACTION = "motone_power_long_press";
	// denglixia add end 2011.6.17

	private final ContentObserver mWidgetObserver = new AppWidgetResetObserver();

	private LayoutInflater mInflater;

	private DragController mDragController;

	private Workspace mWorkspace;
	private Mogoo_DockWorkSpace dockWorkSpace;

	private AppWidgetManager mAppWidgetManager;

	private LauncherAppWidgetHost mAppWidgetHost;

	private CellLayout.CellInfo mAddItemCellInfo;

	private CellLayout.CellInfo mMenuAddInfo;

	private final int[] mCellCoordinates = new int[2];

	private FolderInfo mFolderInfo;

	private Bundle mSavedState;

	private SpannableStringBuilder mDefaultKeySsb = null;

	private boolean mWorkspaceLoading = true;

	private boolean mPaused = true;

	private boolean mRestoring;

	private boolean mWaitingForResult;

	private Bundle mSavedInstanceState;

	private LauncherModel mModel;

	private Mogoo_BitmapCache mIconCache;

	// delete by 张永辉 2011-1-21 将该变量移到LauncherModel.java中方便其它模块调用
	// private ArrayList<ItemInfo> mDesktopItems = new ArrayList<ItemInfo>();
	// end

	private static HashMap<Long, Mogoo_FolderInfo> mtFolders = new HashMap<Long, Mogoo_FolderInfo>();

	// 为了程序不出错，能调试，暂时不删除这个
	private static HashMap<Long, FolderInfo> mFolders = new HashMap<Long, FolderInfo>();

	// add by 张永辉 屏幕指示器
	private ImageView screenIndicator;

	// --------------------motone field statement---------

	public static boolean isWidgetLongPress = false;

	// add by 张永辉 2011-1-21
	// 保存屏幕切换前为哪一屏
	private static int lastScreen = Mogoo_GlobalConfig.getWorkspaceDefaultScreen();

	// 是否己经始化
	public static boolean isInit = false;

	// end

	// add by 黄悦 2011-1-21
	private Mogoo_VibrationController mVibrationController;

	private Mogoo_ContentListener contentListener;

	private Mogoo_FolderController folderController;

	private static final String RUNTIME_STATE_VIBRATE_FLAG = "launcher.vibrate_flag";

	private long runningAppTime = 0;

	private Handler handler = new Handler();

	private boolean clickLocked = false;

	// end
	// denglixia add 2011.4.19
	private Mogoo_RestoreController mRestoreController;
	private Mogoo_UncaughtExceptionHandler mUncaughtExceptionHandler;
	// denglixia add end 2011.4.19

	// add by 张永辉 2011-6-23 存放传给任务管理器截图
	public static Bitmap screenImg = null;
	//add by yenben IBE广播接收器 2012-2-17
	private IBEBroadcastReceiver mIBEBroadcastReceiver;
	// end
	public static boolean bindLocked = false;
	public int bindcount;
	public int binded;
	// end

	// denglixia modify 2011.6.17
	// add by huangyue for long press power to stop vibrate
	/*
	 * private BroadcastReceiver mPowerLongReceiver = new BroadcastReceiver() {
	 * 
	 * @Override public void onReceive(Context context, Intent intent) { // TODO
	 * Auto-generated method stub String action = intent.getAction();
	 * if(action.equals("motone_power_long_press")) { if(mVibrationController !=
	 * null){ mVibrationController.stopVibrate(); } } }
	 * 
	 * }; // end
	 */

	// ---------------------end---------------------------

	@Override
	protected void onCreate(Bundle savedInstanceState)
			throws IllegalAccessError {
		super.onCreate(savedInstanceState);
		// add by 张永辉
		isInit = true;
		// end

		LauncherApplication app = ((LauncherApplication) getApplication());
		mModel = app.setLauncher(this);
		mIconCache = app.getIconCache();
		
		// add by 张永辉 2011-1-24 每次重新启动Launcher都清空一下桌面item列表
		if (mModel.getDesktopItems() != null) {
			mModel.getDesktopItems().clear();
		}
		// end

		mDragController = new DragController(this);
		// add by 黄悦 2011-1-23 抖动控制器
		mVibrationController = new Mogoo_VibrationController(mIconCache,
				Mogoo_GlobalConfig.getVibrationViewID());
		// end

		mInflater = getLayoutInflater();

		mAppWidgetManager = AppWidgetManager.getInstance(this);
		mAppWidgetHost = new LauncherAppWidgetHost(this, APPWIDGET_HOST_ID);
		mAppWidgetHost.startListening();

		if (PROFILE_STARTUP) {
			android.os.Debug.startMethodTracing("/sdcard/launcher");
		}
		
		// denglixia add 2011.4.18
		RunRestorePolicy runPolicy = new RunRestorePolicy();
		runPolicy.start();
		// denglixia add end 2011.4.18

		checkForLocaleChange();
		// try {
		// setWallpaper(getResources().openRawResource(R.drawable.wallpaper_zanzibar));
		// } catch (Exception e) {
		// // TODO: handle exception
		// }
		overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast);
		setWallpaperDimension();

		
		//add by yeben 2012-2-24
//		setStatusBarStyle(MogooActivity.TRANSPARENT_BG,false);
        //end
		setContentView(R.layout.launcher);
		setupViews();
		registerContentObservers();

		mSavedState = savedInstanceState;
		restoreState(mSavedState);

		if (PROFILE_STARTUP) {
			android.os.Debug.stopMethodTracing();
		}

		if (!mRestoring) {
			mModel.startLoader(this, true);
		}

		// For handling default keys
		mDefaultKeySsb = new SpannableStringBuilder();
		Selection.setSelection(mDefaultKeySsb, 0);

		// denglixia modify 2011.6.17
		// IntentFilter filter = new
		// IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		// registerReceiver(mCloseSystemDialogsReceiver, filter);
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		mIntentFilter.addAction(Intent.ACTION_DATE_CHANGED);
		mIntentFilter.addAction(Intent.ACTION_TIME_CHANGED);
		mIntentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
		mIntentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		mIntentFilter.addAction(Intent.ACTION_LOCALE_CHANGED);
		// ---- add by huangyue 2011-1-25
		// -- 注册计数监听
		contentListener = new Mogoo_ContentListener(new Handler(), this);
		contentListener.setCache(mIconCache);
		getContentResolver().registerContentObserver(CallLog.Calls.CONTENT_URI,
				true, contentListener);
		// denglixia modify 2011.4.15
		// getContentResolver().registerContentObserver(Uri.parse("content://sms/"),
		// true,
		getContentResolver().registerContentObserver(
				Uri.parse("content://mms-sms"), false, contentListener);
		// getContentResolver().registerContentObserver(
		// Uri.parse("content://com.android.email.provider/message"), true,
		// contentListener);

		folderController = new Mogoo_FolderController(this);
		folderController.setLauncher(this);
		folderController.startOpenFolderListener();
		
		Mogoo_FolderBubbleText.isOpen = false;
		Mogoo_FolderBubbleText.folderOpening = false;
		// -- end
		// Thread.setDefaultUncaughtExceptionHandler(new ExceptionHandler());
		// //denglixia modify 2011.4.18

		// languageSetting(app);
		registerReceiver(mLauncherBroadcastReceiver, mIntentFilter);
		//add by yeben 2012-2-17
		mIBEBroadcastReceiver = new IBEBroadcastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(ClickUtils.ACTION_APP_CLICK_RESULT);
		registerReceiver(mIBEBroadcastReceiver,filter);
		//end
		//add by huangyue
		if(app.isFilter()){
			Dialog dialog22 = new AlertDialog.Builder(this)
					.setMessage("Lost package error!")
					.setNeutralButton("OK", new OnClickListener() {
						public void onClick(DialogInterface arg0, int arg1) {
							Process.killProcess(Process.myPid());
						}

					}).create();
			dialog22.show();
		}
		//end
	}

	// denglixia add 2011.4.18
	private class RunRestorePolicy extends Thread {
		public void run() {
			// MT_RestoreController restoreController =
			// MT_RestoreController.getInstance();
			// restoreController.loadPolicy();
			// Thread.setDefaultUncaughtExceptionHandler(new
			// MT_UncaughtExceptionHandler());
			mRestoreController = new Mogoo_RestoreController(Launcher.this);
			mRestoreController.loadPolicy();
			mUncaughtExceptionHandler = new Mogoo_UncaughtExceptionHandler(
					mRestoreController, Launcher.this);
			Thread.setDefaultUncaughtExceptionHandler(mUncaughtExceptionHandler);
		}
	}

	// denglixia add end 2011.4.18

	/*
	 * private void languageSetting(LauncherApplication app) { Configuration
	 * config = getResources().getConfiguration(); String c =
	 * config.locale.getDisplayLanguage() + config.locale.getCountry(); Locale
	 * location = app.getLocation(); if(!c.equals(location.getDisplayLanguage()
	 * + location.getCountry())){ Intent mainIntent = new
	 * Intent(Intent.ACTION_MAIN, null);
	 * mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
	 * 
	 * PackageManager packageManager = getPackageManager(); List<ResolveInfo>
	 * apps = packageManager .queryIntentActivities(mainIntent, 0);
	 * 
	 * ResolveInfo info = null; ItemInfo item = null; StringBuilder infoStr =
	 * new StringBuilder(); StringBuilder itemStr = new StringBuilder(); //
	 * ArrayList<ItemInfo> items = mModel.getDesktopItems();
	 * 
	 * for(int i = 0; i < apps.size(); i++){ info = apps.get(i);
	 * infoStr.append(info.activityInfo.applicationInfo.packageName);
	 * infoStr.append(info.activityInfo.name); // for(int j = 0; j <
	 * items.size(); i++){ // item = items.get(j); // itemStr.append(item.) //
	 * if(item) // } }
	 * 
	 * //刷新语音时运行此行，更新数据库 throw new IllegalAccessError("Location change"); } }
	 */

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		// denglixia modify 2011.6.17
		// unregisterReceiver(mPowerLongReceiver);
	}

	public Mogoo_FolderController getFolderController() {
		return folderController;
	}

	private void checkForLocaleChange() {
		final LocaleConfiguration localeConfiguration = new LocaleConfiguration();
		readConfiguration(this, localeConfiguration);

		final Configuration configuration = getResources().getConfiguration();

		final String previousLocale = localeConfiguration.locale;
		final String locale = configuration.locale.toString();

		final int previousMcc = localeConfiguration.mcc;
		final int mcc = configuration.mcc;

		final int previousMnc = localeConfiguration.mnc;
		final int mnc = configuration.mnc;

		boolean localeChanged = !locale.equals(previousLocale)
				|| mcc != previousMcc || mnc != previousMnc;

		if (localeChanged) {
			localeConfiguration.locale = locale;
			localeConfiguration.mcc = mcc;
			localeConfiguration.mnc = mnc;

			writeConfiguration(this, localeConfiguration);
			mIconCache.flush();
		}
	}

	private static class LocaleConfiguration {
		public String locale;

		public int mcc = -1;

		public int mnc = -1;
	}

	private static void readConfiguration(Context context,
			LocaleConfiguration configuration) {
		DataInputStream in = null;
		try {
			in = new DataInputStream(context.openFileInput(PREFERENCES));
			configuration.locale = in.readUTF();
			configuration.mcc = in.readInt();
			configuration.mnc = in.readInt();
		} catch (FileNotFoundException e) {
			// Ignore
		} catch (IOException e) {
			// Ignore
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
	}

	private static void writeConfiguration(Context context,
			LocaleConfiguration configuration) {
		DataOutputStream out = null;
		try {
			out = new DataOutputStream(context.openFileOutput(PREFERENCES,
					MODE_PRIVATE));
			out.writeUTF(configuration.locale);
			out.writeInt(configuration.mcc);
			out.writeInt(configuration.mnc);
			out.flush();
		} catch (FileNotFoundException e) {
			// Ignore
		} catch (IOException e) {
			// noinspection ResultOfMethodCallIgnored
			context.getFileStreamPath(PREFERENCES).delete();
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}
	}

	static int getScreen() {
		synchronized (sLock) {
			return sScreen;
		}
	}

	static void setScreen(int screen) {
		synchronized (sLock) {
			sScreen = screen;
		}
	}

	private void setWallpaperDimension() {
		WallpaperManager wpm = (WallpaperManager) getSystemService(WALLPAPER_SERVICE);

		Display display = getWindowManager().getDefaultDisplay();
		boolean isPortrait = display.getWidth() < display.getHeight();

		final int width = isPortrait ? display.getWidth() : display.getHeight();
		final int height = isPortrait ? display.getHeight() : display
				.getWidth();
		//dendlixia modify 2011.7.28
//		if (wpm.getWallpaperInfo() == null
//				|| wpm.getWallpaperInfo().getPackageName() == null) {
//			try {
//				setWallpaper(getResources().openRawResource(
//						R.drawable.wallpaper_zanzibar));
//			} catch (Exception e) {
//				// TODO: handle exception
//			}
//
//			Log.i(TAG,
//					"setWallpaper(getResources().openRawResource(R.drawable.wallpaper_zanzibar));");
//		}

		wpm.suggestDesiredDimensions(width, height);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		mWaitingForResult = false;

		// The pattern used here is that a user PICKs a specific application,
		// which, depending on the target, might need to CREATE the actual
		// target.

		// For example, the user would PICK_SHORTCUT for "Music playlist", and
		// we
		// launch over to the Music app to actually CREATE_SHORTCUT.

		if (resultCode == RESULT_OK && mAddItemCellInfo != null) {
			switch (requestCode) {
			case REQUEST_PICK_APPLICATION:
				completeAddApplication(this, data, mAddItemCellInfo);
				break;
			case REQUEST_PICK_SHORTCUT:
				processShortcut(data);
				break;
			case REQUEST_CREATE_SHORTCUT:
				completeAddShortcut(data, mAddItemCellInfo);
				break;
			case REQUEST_PICK_LIVE_FOLDER:
				addLiveFolder(data);
				break;
			case REQUEST_CREATE_LIVE_FOLDER:
				completeAddLiveFolder(data, mAddItemCellInfo);
				break;
			case REQUEST_PICK_APPWIDGET:
				addAppWidget(data);
				break;
			case REQUEST_CREATE_APPWIDGET:
				completeAddAppWidget(data, mAddItemCellInfo);
				break;
			case REQUEST_PICK_WALLPAPER:
				// We just wanted the activity result here so we can clear
				// mWaitingForResult
				break;
			}
		} else if ((requestCode == REQUEST_PICK_APPWIDGET || requestCode == REQUEST_CREATE_APPWIDGET)
				&& resultCode == RESULT_CANCELED && data != null) {
			// Clean up the appWidgetId if we canceled
			int appWidgetId = data.getIntExtra(
					AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
			if (appWidgetId != -1) {
				mAppWidgetHost.deleteAppWidgetId(appWidgetId);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (Mogoo_GlobalConfig.LOG_DEBUG) {
			Log.d(TAG,
					"-----------------------onResume()----------------------");
		}
		//add by yeben 2012-3-22
		if (Mogoo_VibrationController.isVibrate) {
		    mVibrationController.stopVibrate();
	    }
		//end
		
		//add  by yeben 2012-7-2
//		if(mWorkspace.getCurrentScreen() == 0){
//			mWorkspace.setCurrentScreen(1);
//			mWorkspace.switchAboutSearch(1);
//		}
		//delete by yeben 2012-8-31 在第一屏时按home键桌面消失
		//end add by yeben 2012-7-2
//		if(mTaskManagerDialog != null && mTaskManagerDialog.isShowing()){
//			mTaskManagerDialog.dismiss();
//		}
		//end
		//add by huangyue
//		mWorkspace.getBackground().setAlpha(0);
//		mWorkspace.setCurrentScreen(mWorkspace.getCurrentScreen());
		mWorkspace.requestLayout();
		mWorkspace.invalidate();
		dockWorkSpace.requestLayout();
		dockWorkSpace.invalidate();
		//end
		// denglixia modify 2011.6.17
		// add by huangyue
		/*
		 * IntentFilter powerLongFilter = new IntentFilter();
		 * powerLongFilter.addAction("motone_power_long_press");
		 * registerReceiver(mPowerLongReceiver, powerLongFilter);
		 * powerLongFilter = null; // end
		 */
		hidFolder();

		mPaused = false;
		if (mRestoring) {

			if (Mogoo_GlobalConfig.LOG_DEBUG) {
				Log.d(TAG, "call startLoader....");
			}

			mWorkspaceLoading = true;
			mModel.startLoader(this, true);
			mRestoring = false;
		}
	}
	
	
	@Override
	protected void onPause() {
		super.onPause();

		if (Mogoo_GlobalConfig.LOG_DEBUG) {
			Log.d(TAG, "-----------------------onPause()----------------------");
		}

		dismissPreview(screenIndicator);
		mDragController.cancelDrag();
		mWorkspace.setLocaleDrag(false);
		//edit by yeben 2012-3-22
		// add by 张永辉 2011-3-1 锁屏后停止抖动
//		if (Mogoo_VibrationController.isVibrate) {
//			mVibrationController.stopVibrate();
//		}
		// end
		//end
		//add by huangyue
//		if(mTaskManagerDialog != null){
//			mTaskManagerDialog.dismiss();
//		}
		//end

		// denglixia modify 2011.6.17
		// add by huangyue
		// unregisterReceiver(mPowerLongReceiver);
		// end
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		// Flag the loader to stop early before switching
		mModel.stopLoader();
		return Boolean.TRUE;
	}

	// We can't hide the IME if it was forced open. So don't bother
	/*
	 * @Override public void onWindowFocusChanged(boolean hasFocus) {
	 * super.onWindowFocusChanged(hasFocus); if (hasFocus) { final
	 * InputMethodManager o = (InputMethodManager)
	 * getSystemService(Context.INPUT_METHOD_SERVICE);
	 * WindowManager.LayoutParams lp = getWindow().getAttributes();
	 * inputManager.hideSoftInputFromWindow(lp.token, 0, new
	 * android.os.ResultReceiver(new android.os.Handler()) { protected void
	 * onReceiveResult(int resultCode, Bundle resultData) { Log.d(TAG,
	 * "ResultReceiver got resultCode=" + resultCode); } }); Log.d(TAG,
	 * "called hideSoftInputFromWindow from onWindowFocusChanged"); } }
	 */

	private boolean acceptFilter() {
		final InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		return !inputManager.isFullscreenMode();
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if(keyCode ==  KeyEvent.KEYCODE_HOME + 200){
			if(mVibrationController.isVibrate){
				mVibrationController.stopVibrate();
			}else{
				startTaskManager();
			}
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			mVibrationController.stopVibrate();
			hidFolder();
		}

		/*
		 * if (!handled && acceptFilter() && keyCode != KeyEvent.KEYCODE_ENTER)
		 * { boolean gotKey =
		 * TextKeyListener.getInstance().onKeyDown(mWorkspace, mDefaultKeySsb,
		 * keyCode, event); if (gotKey && mDefaultKeySsb != null &&
		 * mDefaultKeySsb.length() > 0) { // something usable has been typed -
		 * start a search // the typed text will be retrieved and cleared by //
		 * showSearchDialog() // If there are multiple keystrokes before the
		 * search dialog // takes focus, // onSearchRequested() will be called
		 * for every keystroke, // but it is idempotent, so it's fine. return
		 * onSearchRequested(); } }
		 */

		// Eat the long press event so the keyboard doesn't come up.
		if (keyCode == KeyEvent.KEYCODE_MENU && event.isLongPress()) {
			return true;
		}

		return super.onKeyDown(keyCode, event);
	}

	private void hidFolder() {
		Mogoo_FolderWorkspace folderWorkspace = (Mogoo_FolderWorkspace) Mogoo_ComponentBus
				.getInstance().getActivityComp(R.id.folderWorkspace, this);
		if (folderWorkspace.getVisibility() == View.VISIBLE
				&& Mogoo_VibrationController.isVibrate) {
			mVibrationController.stopVibrate();
			return;
		}

		if (folderWorkspace != null
				&& folderWorkspace.getVisibility() == View.VISIBLE) {
			Mogoo_FolderBubbleText folder = folderWorkspace.getLoadingFolder();
			if (folder != null) {
				folder.closeFolder();
			} else if (folderWorkspace.getCloseFolder() != null) {
				folder = folderWorkspace.getCloseFolder();
				folder.closeFolder();
			}
			folder = null;
		}
	}

	private String getTypedText() {
		return mDefaultKeySsb.toString();
	}

	private void clearTypedText() {
		mDefaultKeySsb.clear();
		mDefaultKeySsb.clearSpans();
		Selection.setSelection(mDefaultKeySsb, 0);
	}

	/**
	 * Restores the previous state, if it exists.
	 * 
	 * @param savedState
	 *            The previous state.
	 */
	private void restoreState(Bundle savedState) {
		if (savedState == null) {
			return;
		}

		// final boolean allApps =
		// savedState.getBoolean(RUNTIME_STATE_ALL_APPS_FOLDER, false);
		// if (allApps) {
		// showAllApps(false);
		// }

		final int currentScreen = savedState.getInt(
				RUNTIME_STATE_CURRENT_SCREEN, -1);
		if (currentScreen > -1) {
			mWorkspace.setCurrentScreen(currentScreen);
		}

		final int addScreen = savedState.getInt(
				RUNTIME_STATE_PENDING_ADD_SCREEN, -1);
		if (addScreen > -1) {
			mAddItemCellInfo = new CellLayout.CellInfo();
			final CellLayout.CellInfo addItemCellInfo = mAddItemCellInfo;
			addItemCellInfo.valid = true;
			addItemCellInfo.screen = addScreen;
			addItemCellInfo.cellX = savedState
					.getInt(RUNTIME_STATE_PENDING_ADD_CELL_X);
			addItemCellInfo.cellY = savedState
					.getInt(RUNTIME_STATE_PENDING_ADD_CELL_Y);
			addItemCellInfo.spanX = savedState
					.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_X);
			addItemCellInfo.spanY = savedState
					.getInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y);
			addItemCellInfo.findVacantCellsFromOccupied(savedState
					.getBooleanArray(RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS),
					savedState.getInt(RUNTIME_STATE_PENDING_ADD_COUNT_X),
					savedState.getInt(RUNTIME_STATE_PENDING_ADD_COUNT_Y));
			mRestoring = true;
		}

		boolean renameFolder = savedState.getBoolean(
				RUNTIME_STATE_PENDING_FOLDER_RENAME, false);
		if (renameFolder) {
			long id = savedState
					.getLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID);
			mFolderInfo = mModel.getFolderById(this, mFolders, id);
			mRestoring = true;
		}
	}

	/**
	 * Finds all the views we need and configure them properly.
	 */
	private void setupViews() {
		// -------add by huangyue 2011-1-20-----
		Mogoo_ComponentBus componentBus = Mogoo_ComponentBus.getInstance();
		// ------- end -------------

		DragController dragController = mDragController;

		DragLayer dragLayer = (DragLayer) findViewById(R.id.drag_layer);
		dragLayer.setDragController(dragController);
		componentBus.addActivityComp(R.id.drag_layer, dragLayer, this);

		mWorkspace = (Workspace) dragLayer.findViewById(R.id.workspace);
		final Workspace workspace = mWorkspace;
		workspace.setHapticFeedbackEnabled(false);
		componentBus.addActivityComp(R.id.workspace, mWorkspace, this);
		loadCellLayout();
		screenIndicator = (ImageView) dragLayer
				.findViewById(R.id.screenIndicator);

		// 设置默认屏的屏幕指示器
		this.setIndicator(lastScreen);

		screenIndicator.setHapticFeedbackEnabled(false);
//		screenIndicator.setOnLongClickListener(this);

		workspace.setOnLongClickListener(this);
		workspace.setDragController(dragController);
		workspace.setLauncher(this);

		
		
		
		dragController.setDragScoller(workspace);

		dragController.setScrollView(dragLayer);
		dragController.setMoveTarget(workspace);

		// The order here is bottom to top.
		dragController.addDropTarget(workspace);

		// -----------add by weijingchun 2011-1-19 ------------------
		// 设置工具栏基本属性
		dockWorkSpace = (Mogoo_DockWorkSpace) findViewById(R.id.dockWorkSpace);
		dockWorkSpace.setLauncher(this);
		dockWorkSpace.setDragController(dragController);
		dockWorkSpace.setOnLongClickListener(this);
		dockWorkSpace.setOnClickListener(this);
		dragController.addDropTarget(dockWorkSpace);
		componentBus.addActivityComp(R.id.dockWorkSpace, dockWorkSpace, this);

		// ----------------------end-------------------

		// ------------ add by huangyue 2011-1-20----
		SearchLayout searchLayout = (SearchLayout) findViewById(R.id.search_view);
		searchLayout.setupView();
		componentBus.addActivityComp(R.id.search_view, searchLayout, this);
		Mogoo_FolderLayout folderLayout = (Mogoo_FolderLayout) findViewById(R.id.folderLayer);
		componentBus.addActivityComp(R.id.folderLayer, folderLayout, this);
		if(folderLayout == null){
			return;
		}
		folderLayout.setupViews();
		// -----------------end-------------------

		// add by 张永辉 2011-3-15
		Mogoo_FolderWorkspace folderWorkspace;
		folderWorkspace = (Mogoo_FolderWorkspace) findViewById(R.id.folderWorkspace);
		folderWorkspace.setLauncher(this);
		folderWorkspace.setDragController(dragController);
		folderWorkspace.setOnLongClickListener(this);
		folderWorkspace.setOnClickListener(this);
		dragController.addDropTarget(folderWorkspace);
		componentBus.addActivityComp(R.id.folderWorkspace, folderWorkspace,
				this);
		// end

	}

	/**
	 * Creates a view representing a shortcut.
	 * 
	 * @param info
	 *            The data structure describing the shortcut.
	 * @return A View inflated from R.layout.application.
	 */
	View createShortcut(ShortcutInfo info) {
		View view = null;
		if (info instanceof Mogoo_FolderInfo) {
			mIconCache.recycle(info.intent.getComponent(),
					Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
//			mIconCache.remove(info.intent.getComponent());
			view = createFolder(R.layout.application_folder,
					(ViewGroup) mWorkspace.getChildAt(mWorkspace
							.getCurrentScreen()), info);
		} else {
			view = createShortcut(R.layout.application,
					(ViewGroup) mWorkspace.getChildAt(mWorkspace
							.getCurrentScreen()), info);
		}

		return view;
	}

	View createFolder(int layoutResId, ViewGroup parent, ShortcutInfo info) {
		Mogoo_FolderBubbleText favorite = (Mogoo_FolderBubbleText) mInflater
				.inflate(layoutResId, parent, false);
		// -------------------end
		// -------------------------------------------------
		favorite.setCompoundDrawablesWithIntrinsicBounds(null,
				new FastBitmapDrawable(info.getIcon(mIconCache)), null, null);
		favorite.setText(info.title);
		favorite.setTag(info);
		favorite.setOnClickListener(folderController);
		favorite.setOnLongClickListener(this);
		// ------ add by huangyue 2011-1-24
		// 初始化快捷方式删除类型
		setIconIntoBubbleText(favorite, info);

		if (info instanceof Mogoo_FolderInfo) {
			ArrayList<ShortcutInfo> infos = ((Mogoo_FolderInfo) info)
					.getContents();
			for (ShortcutInfo temp : infos) {
				// 添加监控对象
				if (contentListener.isListenType(temp.appType)) {
					contentListener.addItem(temp.appType, favorite);
					contentListener.onChange(true);
				}
			}
		}
		// -------end ----
//		favorite.setBackgroundResource(R.drawable.mogoo_icon_s);

		return favorite;
	}

	// add by huangyue
	View createShortcut(int layoutResId, ViewGroup parent, ShortcutInfo info,
			boolean getFromCache) {
		// -------------------modify by weijingchun
		// 2011-1-19--------------------
		// 将系统中所有生成到快捷键类型“TextView“修改为它继承类“MT_BubbleTextView“
		// TextView favorite = (TextView) mInflater.inflate(layoutResId, parent,
		// false);
		Mogoo_BubbleTextView favorite = (Mogoo_BubbleTextView) mInflater
				.inflate(layoutResId, parent, false);
		// -------------------end
		// -------------------------------------------------
		favorite.setCompoundDrawablesWithIntrinsicBounds(
				null,
				new FastBitmapDrawable(mIconCache.getIcon(info.intent)),
				null, null);
		favorite.setText(info.title);
		favorite.setTag(info);
		favorite.setOnClickListener(this);
		// ------ add by huangyue 2011-1-24
		// 初始化快捷方式删除类型
		setIconIntoBubbleText(favorite, info); 

		// 添加监控对象
		if (contentListener.isListenType(info.appType)) {
			contentListener.addItem(info.appType, favorite);
			contentListener.onChange(true);
		}
		// -------end ----
//		favorite.setBackgroundResource(R.drawable.mogoo_icon_s);
		
		return favorite;
	}

	// -------end ----

	/**
	 * Creates a view representing a shortcut inflated from the specified
	 * resource.
	 * 
	 * @param layoutResId
	 *            The id of the XML layout used to create the shortcut.
	 * @param parent
	 *            The group the shortcut belongs to.
	 * @param info
	 *            The data structure describing the shortcut.
	 * @return A View inflated from layoutResId.
	 */
	View createShortcut(int layoutResId, ViewGroup parent, ShortcutInfo info) {
		return createShortcut(layoutResId, parent, info, false);
	}

	/**
	 * Add an application shortcut to the workspace.
	 * 
	 * @param data
	 *            The intent describing the application.
	 * @param cellInfo
	 *            The position on screen where to create the shortcut.
	 */
	void completeAddApplication(Context context, Intent data,
			CellLayout.CellInfo cellInfo) {
		cellInfo.screen = mWorkspace.getCurrentScreen();
		if (!findSingleSlot(cellInfo))
			return;

		final ShortcutInfo info = mModel.getShortcutInfo(
				context.getPackageManager(), data, context);

		if (info != null) {
			info.setActivity(data.getComponent(), Intent.FLAG_ACTIVITY_NEW_TASK
					| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
			info.container = ItemInfo.NO_ID;
			mWorkspace.addApplicationShortcut(info, cellInfo,
					isWorkspaceLocked());
		} else {
			Log.e(TAG, "Couldn't find ActivityInfo for selected application: "
					+ data);
		}
	}

	/**
	 * Add a shortcut to the workspace.
	 * 
	 * @param data
	 *            The intent describing the shortcut.
	 * @param cellInfo
	 *            The position on screen where to create the shortcut.
	 */
	private void completeAddShortcut(Intent data, CellLayout.CellInfo cellInfo) {
		cellInfo.screen = mWorkspace.getCurrentScreen();
		if (!findSingleSlot(cellInfo))
			return;

		final ShortcutInfo info = mModel.addShortcut(this, data, cellInfo,
				false);

		if (!mRestoring) {
			final View view = createShortcut(info);
			mWorkspace.addInCurrentScreen(view, cellInfo.cellX, cellInfo.cellY,
					1, 1, isWorkspaceLocked());
		}
	}

	/**
	 * Add a widget to the workspace.
	 * 
	 * @param data
	 *            The intent describing the appWidgetId.
	 * @param cellInfo
	 *            The position on screen where to create the widget.
	 */
	private void completeAddAppWidget(Intent data, CellLayout.CellInfo cellInfo) {
		Bundle extras = data.getExtras();
		int appWidgetId = extras
				.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);

		if (LOGD)
			Log.d(TAG, "dumping extras content=" + extras.toString());

		AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager
				.getAppWidgetInfo(appWidgetId);

		// Calculate the grid spans needed to fit this widget
		CellLayout layout = (CellLayout) mWorkspace.getChildAt(cellInfo.screen);
		int[] spans = layout.rectToCell(appWidgetInfo.minWidth,
				appWidgetInfo.minHeight);

		// Try finding open space on Launcher screen
		final int[] xy = mCellCoordinates;
		if (!findSlot(cellInfo, xy, spans[0], spans[1])) {
			if (appWidgetId != -1)
				mAppWidgetHost.deleteAppWidgetId(appWidgetId);
			return;
		}

		// Build Launcher-specific widget info and save to database
		LauncherAppWidgetInfo launcherInfo = new LauncherAppWidgetInfo(
				appWidgetId);
		launcherInfo.spanX = spans[0];
		launcherInfo.spanY = spans[1];

		LauncherModel.addItemToDatabase(this, launcherInfo,
				LauncherSettings.Favorites.CONTAINER_DESKTOP,
				mWorkspace.getCurrentScreen(), xy[0], xy[1], false);

		if (!mRestoring) {
			mModel.getDesktopItems().add(launcherInfo);

			// Perform actual inflation because we're live
			launcherInfo.hostView = mAppWidgetHost.createView(this,
					appWidgetId, appWidgetInfo);
			// add by 张永辉
			((LauncherAppWidgetHostView) (launcherInfo.hostView))
					.setLauncher(this);
			// end
			launcherInfo.hostView.setAppWidget(appWidgetId, appWidgetInfo);
			launcherInfo.hostView.setTag(launcherInfo);

			mWorkspace
					.addInCurrentScreen(launcherInfo.hostView, xy[0], xy[1],
							launcherInfo.spanX, launcherInfo.spanY,
							isWorkspaceLocked());
		}
	}

	public void removeAppWidget(LauncherAppWidgetInfo launcherInfo) {
		mModel.getDesktopItems().remove(launcherInfo);
		launcherInfo.hostView = null;
	}

	public LauncherAppWidgetHost getAppWidgetHost() {
		return mAppWidgetHost;
	}

	void closeSystemDialogs() {
		getWindow().closeAllPanels();

		try {
			dismissDialog(DIALOG_CREATE_SHORTCUT);
			// Unlock the workspace if the dialog was showing
		} catch (Exception e) {
			// An exception is thrown if the dialog is not visible, which is
			// fine
		}

		try {
			dismissDialog(DIALOG_RENAME_FOLDER);
			// Unlock the workspace if the dialog was showing
		} catch (Exception e) {
			// An exception is thrown if the dialog is not visible, which is
			// fine
		}

		// Whatever we were doing is hereby canceled.
		mWaitingForResult = false;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		// Close the menu
		if (Intent.ACTION_MAIN.equals(intent.getAction())) {
			// also will cancel mWaitingForResult.
			closeSystemDialogs();

			boolean alreadyOnHome = ((intent.getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
			/*boolean allAppsVisible = isAllAppsVisible();
			if (!mWorkspace.isDefaultScreenShowing() && !Mogoo_VibrationController.isVibrate) {
				mWorkspace
						.moveToDefaultScreen(alreadyOnHome && !allAppsVisible);
			}*/
			if (!mVibrationController.isVibrate) {
				mWorkspace
						.moveToDefaultScreen(alreadyOnHome);
			}
			
			// add by 张永辉 2011-3-1 锁屏后停止抖动
			if (mVibrationController.isVibrate) {
				mVibrationController.stopVibrate();
			}
			// closeAllApps(alreadyOnHome && allAppsVisible);

			final View v = getWindow().peekDecorView();
			if (v != null && v.getWindowToken() != null) {
				InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
			}
		}
	}

	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// Do not call super here
		mSavedInstanceState = savedInstanceState;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(RUNTIME_STATE_CURRENT_SCREEN,
				mWorkspace.getCurrentScreen());

		final ArrayList<Folder> folders = mWorkspace.getOpenFolders();
		if (folders.size() > 0) {
			final int count = folders.size();
			long[] ids = new long[count];
			for (int i = 0; i < count; i++) {
				final FolderInfo info = folders.get(i).getInfo();
				ids[i] = info.id;
			}
			outState.putLongArray(RUNTIME_STATE_USER_FOLDERS, ids);
		} else {
			super.onSaveInstanceState(outState);
		}

		// TODO should not do this if the drawer is currently closing.
		// if (isAllAppsVisible()) {
		// outState.putBoolean(RUNTIME_STATE_ALL_APPS_FOLDER, true);
		// }

		if (mAddItemCellInfo != null && mAddItemCellInfo.valid
				&& mWaitingForResult) {
			final CellLayout.CellInfo addItemCellInfo = mAddItemCellInfo;
			final CellLayout layout = (CellLayout) mWorkspace
					.getChildAt(addItemCellInfo.screen);

			outState.putInt(RUNTIME_STATE_PENDING_ADD_SCREEN,
					addItemCellInfo.screen);
			outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_X,
					addItemCellInfo.cellX);
			outState.putInt(RUNTIME_STATE_PENDING_ADD_CELL_Y,
					addItemCellInfo.cellY);
			outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_X,
					addItemCellInfo.spanX);
			outState.putInt(RUNTIME_STATE_PENDING_ADD_SPAN_Y,
					addItemCellInfo.spanY);
			outState.putInt(RUNTIME_STATE_PENDING_ADD_COUNT_X,
					layout.getCountX());
			outState.putInt(RUNTIME_STATE_PENDING_ADD_COUNT_Y,
					layout.getCountY());
			outState.putBooleanArray(RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS,
					layout.getOccupiedCells());
		}

		if (mFolderInfo != null && mWaitingForResult) {
			outState.putBoolean(RUNTIME_STATE_PENDING_FOLDER_RENAME, true);
			outState.putLong(RUNTIME_STATE_PENDING_FOLDER_RENAME_ID,
					mFolderInfo.id);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		try {
			mAppWidgetHost.stopListening();
		} catch (NullPointerException ex) {
			Log.w(TAG,
					"problem while stopping AppWidgetHost during Launcher destruction",
					ex);
		}

		TextKeyListener.getInstance().release();

		mModel.stopLoader();

		unbindDesktopItems();

		getContentResolver().unregisterContentObserver(mWidgetObserver);

		// dismissPreview(mPreviousView);
		dismissPreview(screenIndicator);

		// denglixia modify 2011.6.17
		// unregisterReceiver(mCloseSystemDialogsReceiver);
		// unregisterReceiver(mLauncherBroadcastReceiver);

		// denglixia add 2011.4.28
		contentListener.unRegisterMarketReceiver();

		// ------ add by huangyue 2011-1-20-----
		Mogoo_ComponentBus.getInstance().clear(this);
		getContentResolver().unregisterContentObserver(contentListener);
		mVibrationController.stopVibrate();
		folderController.stopOpenFolderListener();
		mIconCache.recycleAll();
		// ------------ end --------------------

		// add by 张永辉 2010-12-16 保存当前处于哪一屏
		lastScreen = this.sScreen;
		// end
		// denglixia add 2011.4.20
		mRestoreController.clear();
		unregisterReceiver(mLauncherBroadcastReceiver);
		//add by yeben 2012-2-17
		unregisterReceiver(mIBEBroadcastReceiver);
		//end
	}

	// ------ add by huangyue 2011-1-20-----
	@Override
	public void finish() {
		super.finish();
		Mogoo_ComponentBus.getInstance().clear(this);
	}

	// ------------ end --------------------

	@Override
	public void startActivityForResult(Intent intent, int requestCode) {
		if (requestCode >= 0)
			mWaitingForResult = true;
		super.startActivityForResult(intent, requestCode);
	}

	@Override
	public void startSearch(String initialQuery, boolean selectInitialQuery,
			Bundle appSearchData, boolean globalSearch) {

		// closeAllApps(true);

		if (initialQuery == null) {
			// Use any text typed in the launcher as the initial query
			initialQuery = getTypedText();
			clearTypedText();
		}
		if (appSearchData == null) {
			appSearchData = new Bundle();
			//appSearchData.putString(Search.SOURCE, "launcher-search");
			appSearchData.putString("source", "launcher-search");
		}

		final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
		searchManager.startSearch(initialQuery, selectInitialQuery,
				getComponentName(), appSearchData, globalSearch);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		if (isWorkspaceLocked()) {
			return false;
		}

		super.onCreateOptionsMenu(menu);

		// delete by 张永辉 2011-1-23 删除增加选项
		// menu.add(MENU_GROUP_ADD, MENU_ADD, 0, R.string.menu_add)
		// .setIcon(android.R.drawable.ic_menu_add)
		// .setAlphabeticShortcut('A');
		// end

		// add by 张永辉 2011-1-27 当处于widget屏时，增加一个添加widget的入口
		// if(MT_GlobalConfig.isWidgetScreen(this.getCurrentWorkspaceScreen())){
		menu.add(MENU_GROUP_ADD, MENU_ADD_WIDGET, 0, R.string.menu_add_widget)
				.setIcon(android.R.drawable.ic_menu_add)
				.setAlphabeticShortcut('A');
		// }
		// end

		menu.add(MENU_GROUP_WALLPAPER, MENU_WALLPAPER_SETTINGS, 0,
				R.string.menu_wallpaper)
				.setIcon(android.R.drawable.ic_menu_gallery)
				.setAlphabeticShortcut('W');
		menu.add(0, MENU_SEARCH, 0, R.string.menu_search)
				.setIcon(android.R.drawable.ic_search_category_default)
				.setAlphabeticShortcut(SearchManager.MENU_KEY);
		menu.add(0, MENU_NOTIFICATIONS, 0, R.string.menu_notifications)
				.setIcon(R.drawable.ic_menu_notifications)
				.setAlphabeticShortcut('N');

		final Intent settings = new Intent(
				android.provider.Settings.ACTION_SETTINGS);
		settings.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);

		menu.add(0, MENU_SETTINGS, 0, R.string.menu_settings)
				.setIcon(android.R.drawable.ic_menu_preferences)
				.setAlphabeticShortcut('P').setIntent(settings);

		// denglixia add 2011.4.20
		menu.add(0, MENU_REFRESH, 0, R.string.menu_refresh).setIcon(
				R.drawable.ic_menu_refresh);
		// denglixia add end 2011.4.20
		// just for test
		if (Mogoo_GlobalConfig.LOG_DEBUG) {
			// menu.add(0, 10, 0, "Animation");
		}

		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		// add by 张永辉 2011-1-27 抖动时禁止弹出菜单
		if (Mogoo_VibrationController.isVibrate) {
			return false;
		}
		// end

		// If all apps is animating, don't show the menu, because we don't know
		// which one to show.

		// delete by 张永辉 2011-1-23 删除增加选项
		// menu.setGroupVisible(MENU_GROUP_ADD, visible);
		// end

		// menu.setGroupVisible(MENU_GROUP_WALLPAPER, visible);

		// Disable add if the workspace is full.
		// delete by 张永辉 2011-1-23 删除增加选项
		// if (visible) {
		// mMenuAddInfo = mWorkspace.findAllVacantCells(null);
		// menu.setGroupEnabled(MENU_GROUP_ADD, mMenuAddInfo != null &&
		// mMenuAddInfo.valid);
		// }
		// end

		// add by 张永辉 2011-1-27 当处于widget屏时，增加一个添加widget的入口
		if (Mogoo_GlobalConfig.isWidgetScreen(this.getCurrentWorkspaceScreen())) {
			mMenuAddInfo = mWorkspace.findAllVacantCells(null);
			// if (mMenuAddInfo != null) {
			// mAddItemCellInfo = mMenuAddInfo;
			// }
			menu.setGroupVisible(MENU_GROUP_ADD, true);
			menu.setGroupEnabled(MENU_GROUP_ADD, mMenuAddInfo != null
					&& mMenuAddInfo.valid);
		} else {
			menu.setGroupVisible(MENU_GROUP_ADD, false);
			menu.setGroupEnabled(MENU_GROUP_ADD, false);
		}
		// end

		// add by 张永辉 启动任务管理器
		//startTaskManager();

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_ADD:
			addItems();
			return true;
		case MENU_WALLPAPER_SETTINGS:
			startWallpaper();
			return true;
		case MENU_SEARCH:
			onSearchRequested();
			return true;
		case MENU_NOTIFICATIONS:
			showNotifications();
			return true;

			// add by 张永辉 2011-1-27 增加widget菜单
		case MENU_ADD_WIDGET:
			addWidget();
			return true;
			// end
			// denglixia add 2011.4.20
		case MENU_REFRESH:
			refreshThrowException(true);
			return true;
			// denglixia add end 2011.4.20
			// 用于开关动画 just for test
		case 10:
			Mogoo_GlobalConfig.PLAY_ANIMATION = !Mogoo_GlobalConfig.PLAY_ANIMATION;
			return true;
			// end
		}

		return super.onOptionsItemSelected(item);
	}

	// denglixia add 2011.4.20
	// 按刷新菜单，抛出异常，重启Launcher
	private void refreshThrowException(boolean IsThrow)
			throws Mogoo_BootRestoreException {
		if (IsThrow) {
			throw new Mogoo_BootRestoreException();
		}
	}

	// denglixia add end 2011.4.20
	/**
	 * Indicates that we want global search for this activity by setting the
	 * globalSearch argument for {@link #startSearch} to true.
	 */

	@Override
	public boolean onSearchRequested() {
		startSearch(null, false, null, true);
		return true;
	}

	public boolean isWorkspaceLocked() {
		return mWorkspaceLoading || mWaitingForResult;
	}

	private void addItems() {
		// closeAllApps(true);
		showAddDialog(mMenuAddInfo);
	}

	void addAppWidget(Intent data) {
		// TODO: catch bad widget exception when sent
		int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
				-1);
		AppWidgetProviderInfo appWidget = mAppWidgetManager
				.getAppWidgetInfo(appWidgetId);

		if (appWidget.configure != null) {
			// Launch over to configure widget, if needed
			Intent intent = new Intent(
					AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
			intent.setComponent(appWidget.configure);
			intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);

			startActivityForResultSafely(intent, REQUEST_CREATE_APPWIDGET);
		} else {
			// Otherwise just add it
			onActivityResult(REQUEST_CREATE_APPWIDGET, Activity.RESULT_OK, data);
		}
	}

	void processShortcut(Intent intent) {
		// Handle case where user selected "Applications"
		String applicationName = getResources().getString(
				R.string.group_applications);
		String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

		if (applicationName != null && applicationName.equals(shortcutName)) {
			Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
			mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

			Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
			pickIntent.putExtra(Intent.EXTRA_INTENT, mainIntent);
			startActivityForResult(pickIntent, REQUEST_PICK_APPLICATION);
		} else {
			startActivityForResult(intent, REQUEST_CREATE_SHORTCUT);
		}
	}

	void addLiveFolder(Intent intent) {
		// Handle case where user selected "Folder"
		String folderName = getResources().getString(R.string.group_folder);
		String shortcutName = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);

		if (folderName != null && folderName.equals(shortcutName)) {
			addFolder();
		} else {
			startActivityForResult(intent, REQUEST_CREATE_LIVE_FOLDER);
		}
	}

	void addFolder() {
		UserFolderInfo folderInfo = new UserFolderInfo();
		folderInfo.title = getText(R.string.folder_name);

		CellLayout.CellInfo cellInfo = mAddItemCellInfo;
		cellInfo.screen = mWorkspace.getCurrentScreen();
		if (!findSingleSlot(cellInfo))
			return;

		// Update the model
		LauncherModel.addItemToDatabase(this, folderInfo,
				LauncherSettings.Favorites.CONTAINER_DESKTOP,
				mWorkspace.getCurrentScreen(), cellInfo.cellX, cellInfo.cellY,
				false);
		mFolders.put(folderInfo.id, folderInfo);

		// Create the view
		FolderIcon newFolder = FolderIcon
				.fromXml(R.layout.folder_icon, this, (ViewGroup) mWorkspace
						.getChildAt(mWorkspace.getCurrentScreen()), folderInfo);
		mWorkspace.addInCurrentScreen(newFolder, cellInfo.cellX,
				cellInfo.cellY, 1, 1, isWorkspaceLocked());
	}

	void removeFolder(FolderInfo folder) {
		mFolders.remove(folder.id);
	}

	private void completeAddLiveFolder(Intent data, CellLayout.CellInfo cellInfo) {
		cellInfo.screen = mWorkspace.getCurrentScreen();
		if (!findSingleSlot(cellInfo))
			return;

		final LiveFolderInfo info = addLiveFolder(this, data, cellInfo, false);

		if (!mRestoring) {
			final View view = LiveFolderIcon.fromXml(R.layout.live_folder_icon,
					this, (ViewGroup) mWorkspace.getChildAt(mWorkspace
							.getCurrentScreen()), info);
			mWorkspace.addInCurrentScreen(view, cellInfo.cellX, cellInfo.cellY,
					1, 1, isWorkspaceLocked());
		}
	}

	static LiveFolderInfo addLiveFolder(Context context, Intent data,
			CellLayout.CellInfo cellInfo, boolean notify) {

		Intent baseIntent = data
				.getParcelableExtra(LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT);
		String name = data.getStringExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME);

		Drawable icon = null;
		Intent.ShortcutIconResource iconResource = null;

		Parcelable extra = data
				.getParcelableExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON);
		if (extra != null && extra instanceof Intent.ShortcutIconResource) {
			try {
				iconResource = (Intent.ShortcutIconResource) extra;
				final PackageManager packageManager = context
						.getPackageManager();
				Resources resources = packageManager
						.getResourcesForApplication(iconResource.packageName);
				final int id = resources.getIdentifier(
						iconResource.resourceName, null, null);
				icon = resources.getDrawable(id);
			} catch (Exception e) {
				Log.w(TAG, "Could not load live folder icon: " + extra);
			}
		}

		if (icon == null) {
			icon = context.getResources().getDrawable(
					R.drawable.ic_launcher_folder);
		}

		final LiveFolderInfo info = new LiveFolderInfo();
		info.icon = Utilities.createIconBitmap(icon, context);
		info.title = name;
		info.iconResource = iconResource;
		info.uri = data.getData();
		info.baseIntent = baseIntent;
		info.displayMode = data.getIntExtra(
				LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE,
				LiveFolders.DISPLAY_MODE_GRID);

		LauncherModel.addItemToDatabase(context, info,
				LauncherSettings.Favorites.CONTAINER_DESKTOP, cellInfo.screen,
				cellInfo.cellX, cellInfo.cellY, notify);
		mFolders.put(info.id, info);

		return info;
	}

	private boolean findSingleSlot(CellLayout.CellInfo cellInfo) {
		final int[] xy = new int[2];
		if (findSlot(cellInfo, xy, 1, 1)) {
			cellInfo.cellX = xy[0];
			cellInfo.cellY = xy[1];
			return true;
		}
		return false;
	}

	private boolean findSlot(CellLayout.CellInfo cellInfo, int[] xy, int spanX,
			int spanY) {
		if (!cellInfo.findCellForSpan(xy, spanX, spanY)) {
			boolean[] occupied = mSavedState != null ? mSavedState
					.getBooleanArray(RUNTIME_STATE_PENDING_ADD_OCCUPIED_CELLS)
					: null;
			cellInfo = mWorkspace.findAllVacantCells(occupied);
			if (!cellInfo.findCellForSpan(xy, spanX, spanY)) {
				Toast.makeText(this, getString(R.string.out_of_space),
						Toast.LENGTH_SHORT).show();
				return false;
			}
		}
		return true;
	}

	private void showNotifications() {
		// TODO: disabled by achellies
		final StatusBarManager statusBar = (StatusBarManager) getSystemService("statusbar"/*STATUS_BAR_SERVICE*/);
		if (statusBar != null) {
			statusBar.expand();
		}
	}

	private void startWallpaper() {
		// closeAllApps(true);
		final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
		Intent chooser = Intent.createChooser(pickWallpaper,
				getText(R.string.chooser_wallpaper));
		// NOTE: Adds a configure option to the chooser if the wallpaper
		// supports it
		// Removed in Eclair MR1
		// WallpaperManager wm = (WallpaperManager)
		// getSystemService(Context.WALLPAPER_SERVICE);
		// WallpaperInfo wi = wm.getWallpaperInfo();
		// if (wi != null && wi.getSettingsActivity() != null) {
		// LabeledIntent li = new LabeledIntent(getPackageName(),
		// R.string.configure_wallpaper, 0);
		// li.setClassName(wi.getPackageName(), wi.getSettingsActivity());
		// chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[] { li });
		// }
		startActivityForResult(chooser, REQUEST_PICK_WALLPAPER);
	}

	/**
	 * Registers various content observers. The current implementation registers
	 * only a favorites observer to keep track of the favorites applications.
	 */
	private void registerContentObservers() {
		ContentResolver resolver = getContentResolver();
		resolver.registerContentObserver(
				LauncherProvider.CONTENT_APPWIDGET_RESET_URI, true,
				mWidgetObserver);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_HOME:
				return true;
			case KeyEvent.KEYCODE_VOLUME_DOWN:
				// TODO : disabled by achellies
//				if (SystemProperties.getInt("debug.launcher2.dumpstate", 0) != 0) {
//					dumpState();
//					return true;
//				}
				break;
			}
		} else if (event.getAction() == KeyEvent.ACTION_UP) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_HOME:
				return true;
			}
		}

		return super.dispatchKeyEvent(event);
	}

	@Override
	public void onBackPressed() {
		closeFolder();
		dismissPreview(screenIndicator);

		// add by 张永辉 2011-3-3 取消widget上的删除按钮
		clearWidgetsDelIcon();
		// end
	}

	private void closeFolder() {
		Folder folder = mWorkspace.getOpenFolder();
		if (folder != null) {
			closeFolder(folder);
		}
	}

	void closeFolder(Folder folder) {
		folder.getInfo().opened = false;
		ViewGroup parent = (ViewGroup) folder.getParent();
		if (parent != null) {
			parent.removeView(folder);
			if (folder instanceof DropTarget) {
				// Live folders aren't DropTargets.
				mDragController.removeDropTarget((DropTarget) folder);
			}
		}
		folder.onClose();
	}

	/**
	 * Re-listen when widgets are reset.
	 */
	private void onAppWidgetReset() {
		mAppWidgetHost.startListening();
	}

	/**
	 * Go through the and disconnect any of the callbacks in the drawables and
	 * the views or we leak the previous Home screen on orientation change.
	 */
	private void unbindDesktopItems() {
		for (ItemInfo item : mModel.getDesktopItems()) {
			item.unbind();
		}
	}

	/**
	 * Launches the intent referred by the clicked shortcut.
	 * 
	 * @param v
	 *            The view representing the clicked shortcut.
	 */
	public void onClick(View v) {
		if (clickLocked) {
			return;
		}

		lockClick();

		if (Mogoo_GlobalConfig.LOG_DEBUG) {
			Log.d(TAG,
					"--------------------start launcher onClick()--------------------------------------");
		}
		// add by 张永辉 2011-3-1 抖动的时候不能打开应用
		if (Mogoo_VibrationController.isVibrate
				|| mDragController.dispatchKeyEvent(null)) {
			return;
		}
		// end
		Object tag = v.getTag();

		if (tag instanceof ShortcutInfo) {
			ShortcutInfo info = (ShortcutInfo) tag;
			changeIconColor(v, info);
			// Open shortcut
			final Intent intent = info.intent;
			//add by huangyue
			if(intent.getComponent().getPackageName().equals("com.mogoo.deskclear")){
				throw new Mogoo_BootRestoreException();
			}
			//end
			int[] pos = new int[2];
			v.getLocationOnScreen(pos);
			intent.setSourceBounds(new Rect(pos[0], pos[1], pos[0]
					+ v.getWidth(), pos[1] + v.getHeight()));
			ClickUtils.firstClick(this, intent);
			startActivitySafely(intent, tag);
		} else if (tag instanceof FolderInfo) {
			handleFolderClick((FolderInfo) tag);
		}

		runningAppTime = System.currentTimeMillis();

		if (Mogoo_GlobalConfig.LOG_DEBUG) {
			Log.d(TAG,
					"--------------------end launcher onClick()--------------------------------------");
		}
	}

	private void changeIconColor(View v, final ShortcutInfo info) {
		if(v instanceof BubbleTextView){
			final BubbleTextView bubbleTextView = (BubbleTextView) v;
			final Bitmap iconTemp = Mogoo_BitmapUtils.getIconClickColor(info, mIconCache);
			final FastBitmapDrawable icon = new FastBitmapDrawable(iconTemp);
			bubbleTextView.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
			bubbleTextView.invalidate();
			
			handler.postDelayed(new Runnable() {
				public void run() {
					bubbleTextView.setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(info.getIcon(mIconCache)), null, null);
					bubbleTextView.invalidate();
					iconTemp.recycle();
				}
			}, 1000);			
		}
	}

	void startActivitySafely(Intent intent, Object tag) {
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.activity_not_found,
					Toast.LENGTH_SHORT).show();
			Log.e(TAG, "Unable to launch. tag=" + tag + " intent=" + intent, e);
		} catch (SecurityException e) {
			Toast.makeText(this, R.string.activity_not_found,
					Toast.LENGTH_SHORT).show();
			Log.e(TAG,
					"Launcher does not have the permission to launch "
							+ intent
							+ ". Make sure to create a MAIN intent-filter for the corresponding activity "
							+ "or use the exported attribute for this activity. "
							+ "tag=" + tag + " intent=" + intent, e);
		}
	}

	void startActivityForResultSafely(Intent intent, int requestCode) {
		try {
			startActivityForResult(intent, requestCode);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(this, R.string.activity_not_found,
					Toast.LENGTH_SHORT).show();
		} catch (SecurityException e) {
			Toast.makeText(this, R.string.activity_not_found,
					Toast.LENGTH_SHORT).show();
			Log.e(TAG,
					"Launcher does not have the permission to launch "
							+ intent
							+ ". Make sure to create a MAIN intent-filter for the corresponding activity "
							+ "or use the exported attribute for this activity.",
					e);
		}
	}

	private void handleFolderClick(FolderInfo folderInfo) {
		if (!folderInfo.opened) {
			// Close any open folder
			closeFolder();
			// Open the requested folder
			openFolder(folderInfo);
		} else {
			// Find the open folder...
			Folder openFolder = mWorkspace.getFolderForTag(folderInfo);
			int folderScreen;
			if (openFolder != null) {
				folderScreen = mWorkspace.getScreenForView(openFolder);
				// .. and close it
				closeFolder(openFolder);
				if (folderScreen != mWorkspace.getCurrentScreen()) {
					// Close any folder open on the current screen
					closeFolder();
					// Pull the folder onto this screen
					openFolder(folderInfo);
				}
			}
		}
	}

	/**
	 * Opens the user fodler described by the specified tag. The opening of the
	 * folder is animated relative to the specified View. If the View is null,
	 * no animation is played.
	 * 
	 * @param folderInfo
	 *            The FolderInfo describing the folder to open.
	 */
	private void openFolder(FolderInfo folderInfo) {
		Folder openFolder;

		if (folderInfo instanceof UserFolderInfo) {
			openFolder = UserFolder.fromXml(this);
		} else if (folderInfo instanceof LiveFolderInfo) {
			openFolder = com.mogoo.launcher2.LiveFolder.fromXml(this,
					folderInfo);
		} else {
			return;
		}

		openFolder.setDragController(mDragController);
		openFolder.setLauncher(this);

		openFolder.bind(folderInfo);
		folderInfo.opened = true;

		mWorkspace.addInScreen(openFolder, folderInfo.screen, 0, 0, 4, 4);
		openFolder.onOpen();
	}

	public boolean onLongClick(View v) {
		try {

			if (clickLocked) {
				return true;
			}

			lockClick();

			if (Mogoo_GlobalConfig.LOG_DEBUG) {
				Log.d(TAG,
						"--------------------start launcher onLongClick()--------------------------------------");
			}
			// add by huangyue
			View destView = v;
			// end
			switch (v.getId()) {
			case R.id.screenIndicator:
				if (!isAllAppsVisible()) {
					mWorkspace.performHapticFeedback(
							HapticFeedbackConstants.LONG_PRESS,
							HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
					showPreviews(v);
				}
				return true;
			}

			if (isWorkspaceLocked()
					|| Math.abs(System.currentTimeMillis() - runningAppTime) < 1000) {
				return true;
			}

			if (!(v instanceof CellLayout)) {
				v = (View) v.getParent();
			}

			CellLayout.CellInfo cellInfo = (CellLayout.CellInfo) v.getTag();

			// This happens when long clicking an item with the dpad/trackball
			if (cellInfo == null) {
				return true;
			}

			// -----------add by weijingchun 2011-1-22---------
			// dock工具栏
			if (cellInfo.containter == R.id.dockWorkSpace) {
				if (cellInfo.cell == null) {
					if (cellInfo.valid) {
						// User long pressed on empty space
						mWorkspace.setAllowLongPress(false);
						mWorkspace
								.performHapticFeedback(
										HapticFeedbackConstants.LONG_PRESS,
										HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
						// showAddDialog(cellInfo);
					}
				} else {
					if (!(cellInfo.cell instanceof Folder)) {
						// add by huangyue 2011-1-22
						// mWorkspace.getCurrentScreen() !=
						// MT_GlobalConfig.getSearchScreen()

						if (!Mogoo_GlobalConfig.isSearchScreen(mWorkspace
								.getCurrentScreen())
								&& !Mogoo_GlobalConfig
										.isWidgetScreen(mWorkspace
												.getCurrentScreen())) {
							Mogoo_DockWorkSpace dockWorkSpace = (Mogoo_DockWorkSpace) Mogoo_ComponentBus
									.getInstance().getActivityComp(
											R.id.dockWorkSpace, this);
							dockWorkSpace
									.performHapticFeedback(
											HapticFeedbackConstants.LONG_PRESS,
											HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
							destView.setVisibility(View.INVISIBLE);
							mVibrationController.startVibrate(this);
							dockWorkSpace.startDrag(cellInfo);

						} else {
							return true;
						}
						// end
					}
				}

				return true;
			}
			// 文件夹
			else if (cellInfo.containter == R.id.folderWorkspace) {
				if (cellInfo.cell == null) {
					if (cellInfo.valid) {
						// User long pressed on empty space
						mWorkspace.setAllowLongPress(false);
						mWorkspace
								.performHapticFeedback(
										HapticFeedbackConstants.LONG_PRESS,
										HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
					}
				} else {
					if (!(cellInfo.cell instanceof Folder)) {
						if (!Mogoo_GlobalConfig.isSearchScreen(mWorkspace
								.getCurrentScreen())
								&& !Mogoo_GlobalConfig
										.isWidgetScreen(mWorkspace
												.getCurrentScreen())) {
							Mogoo_FolderWorkspace folderWorkspace = (Mogoo_FolderWorkspace) Mogoo_ComponentBus
									.getInstance().getActivityComp(
											R.id.folderWorkspace, this);
							folderWorkspace
									.performHapticFeedback(
											HapticFeedbackConstants.LONG_PRESS,
											HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
							destView.setVisibility(View.INVISIBLE);
							mVibrationController.startVibrate(this);
							folderWorkspace.startDrag(cellInfo);
						} else {
							return true;
						}
					}
				}
				return true;
			}
			// workspace
			else {
				// -----------------end ------------------------
				if (mWorkspace.allowLongPress()) {
					if (cellInfo.cell == null) {
						if (cellInfo.valid) {
							// User long pressed on empty space
							mWorkspace.setAllowLongPress(false);
							mWorkspace
									.performHapticFeedback(
											HapticFeedbackConstants.LONG_PRESS,
											HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);
							// delete by 张永辉 2011-1-23 除去桌面长按出现的快捷菜单
							// showAddDialog(cellInfo);
							// end
						}
					} else {

						if (!(cellInfo.cell instanceof Folder)&& !(cellInfo.cell instanceof SearchLayout)) {
							// User long pressed on an item
							mWorkspace
									.performHapticFeedback(
											HapticFeedbackConstants.LONG_PRESS,
											HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING);

							// add by huangyue 2011-1-22
							destView.setVisibility(View.INVISIBLE);
							if (mWorkspace.getCurrentScreen() != Mogoo_GlobalConfig
									.getSearchScreen()
									&& !Mogoo_GlobalConfig
											.isWidgetScreen(mWorkspace
													.getCurrentScreen())) {
								mVibrationController.startVibrate(this);
							}

							mWorkspace.startDrag(cellInfo);
							// end

							// add by 张永辉 2011-3-4
							// 如果长按对象为WIDGET时，WIDGET左上角出现删除按钮
							clearWidgetsDelIcon();
							if (cellInfo.cell instanceof LauncherAppWidgetHostView) {
								LauncherAppWidgetHostView hostView = (LauncherAppWidgetHostView) (cellInfo.cell);
								hostView.showDelIcon();
								isWidgetLongPress = true;
							}
							// end
						}
					}
				}
				return true;
			}

		} finally {
			if (Mogoo_GlobalConfig.LOG_DEBUG) {
				Log.d(TAG,
						"--------------------end launcher onLongClick()--------------------------------------");
			}
		}

	}

	@SuppressWarnings({ "unchecked" })
	private void dismissPreview(final View v) {
		final PopupWindow window = (PopupWindow) v.getTag();
		if (window != null) {
			window.setOnDismissListener(new PopupWindow.OnDismissListener() {
				public void onDismiss() {
					ViewGroup group = (ViewGroup) v.getTag(R.id.workspace);
					int count = group.getChildCount();
					for (int i = 0; i < count; i++) {
						((ImageView) group.getChildAt(i))
								.setImageDrawable(null);
					}
					ArrayList<Bitmap> bitmaps = (ArrayList<Bitmap>) v
							.getTag(R.id.icon);
					for (Bitmap bitmap : bitmaps)
						bitmap.recycle();

					v.setTag(R.id.workspace, null);
					v.setTag(R.id.icon, null);
					window.setOnDismissListener(null);
				}
			});
			window.dismiss();
		}
		v.setTag(null);
	}

	private void showPreviews(View anchor) {
		//update by 袁业奔 2011-9-8
		showPreviews(anchor, 1, mWorkspace.getChildCount());

		//update by hy 2011-7-26
		//showPreviews(anchor, 0, mWorkspace.getChildCount());
//		int currentScreen = mWorkspace.getCurrentScreen();
//		int start = currentScreen - 2;
//		int end = currentScreen + 2;
//		int max = mWorkspace.getChildCount();
//		
//		if(currentScreen < 2){
//			start = 0;
//			end = 4;
//		} else if((max - currentScreen) < 2){
//			start = max - 5;
//			end = max - 1;
//		}
//		
//		showPreviews(anchor, start, end + 1);
		//end update 
		
		//------------end------------
	}

	private void showPreviews(final View anchor, int start, int end) {
		final Resources resources = getResources();
		final Workspace workspace = mWorkspace;

		CellLayout cell = ((CellLayout) workspace.getChildAt(start));

		//update by hy 2011-7-26
		float max = end - start + 1;
		//float max = workspace.getChildCount();
		//end update 

		final Rect r = new Rect();
		resources.getDrawable(R.drawable.preview_background).getPadding(r);
		int extraW = (int) ((r.left + r.right) * max);
		int extraH = r.top + r.bottom;

		int aW = cell.getWidth() - extraW;
		float w = aW / max;

		int width = cell.getWidth();
		int height = cell.getHeight();
		int x = cell.getLeftPadding();
		int y = cell.getTopPadding();
		width -= (x + cell.getRightPadding());
		height -= (y + cell.getBottomPadding());

		float scale = w / width;

		int count = end - start;

		final float sWidth = width * scale;
		float sHeight = height * scale;

		LinearLayout preview = new LinearLayout(this);

		PreviewTouchHandler handler = new PreviewTouchHandler(anchor);
		ArrayList<Bitmap> bitmaps = new ArrayList<Bitmap>(count);

		for (int i = start; i < end; i++) {
			ImageView image = new ImageView(this);
			cell = (CellLayout) workspace.getChildAt(i);

			final Bitmap bitmap = Bitmap.createBitmap((int) sWidth,
					(int) sHeight, Bitmap.Config.ARGB_8888);

			final Canvas c = new Canvas(bitmap);
			c.scale(scale, scale);
			c.translate(-cell.getLeftPadding(), -cell.getTopPadding());
			cell.dispatchDraw(c);

			image.setBackgroundDrawable(resources
					.getDrawable(R.drawable.preview_background));
			image.setImageBitmap(bitmap);
			image.setTag(i);
			image.setOnClickListener(handler);
			image.setOnFocusChangeListener(handler);
			image.setFocusable(true);
			if (i == mWorkspace.getCurrentScreen())
				image.requestFocus();

			preview.addView(image, LinearLayout.LayoutParams.WRAP_CONTENT,
					LinearLayout.LayoutParams.WRAP_CONTENT);

			bitmaps.add(bitmap);
		}

		final PopupWindow p = new PopupWindow(this);
		p.setContentView(preview);
		//update by 张永辉 2011-7-26 
//		p.setWidth((int) (sWidth * count + extraW));
		p.setWidth(Mogoo_GlobalConfig.getScreenWidth());
		//end update 
		p.setHeight((int) (sHeight + extraH));
		p.setAnimationStyle(R.style.AnimationPreview);
		p.setOutsideTouchable(true);
		p.setFocusable(true);
		p.setBackgroundDrawable(new ColorDrawable(0));
		p.showAsDropDown(anchor, 0, 0);
		
		//add by 张永辉 2011-7-26
		//让预览窗口居中
		preview.setHorizontalGravity(Gravity.CENTER) ;
		//end 

		p.setOnDismissListener(new PopupWindow.OnDismissListener() {
			public void onDismiss() {
				dismissPreview(anchor);
			}
		});

		anchor.setTag(p);
		anchor.setTag(R.id.workspace, preview);
		anchor.setTag(R.id.icon, bitmaps);
	}

	class PreviewTouchHandler implements View.OnClickListener, Runnable,
			View.OnFocusChangeListener {
		private final View mAnchor;

		public PreviewTouchHandler(View anchor) {
			mAnchor = anchor;
		}

		public void onClick(View v) {
			// update by 张永辉
			int whichScreen = (Integer) v.getTag();
			// 当振动状态下，不能跳到搜索屏和Widget屏
			if (Mogoo_VibrationController.isVibrate) {
				if (Mogoo_GlobalConfig.isShortcutScreen(whichScreen)) {
					mWorkspace.snapToScreen(whichScreen);
				}
				// 非振动状态下，任何屏都可以跳
			} else {
				mWorkspace.snapToScreen(whichScreen, 0, false);
			}
			// end
			v.post(this);
		}

		public void run() {
			dismissPreview(mAnchor);
		}

		public void onFocusChange(View v, boolean hasFocus) {
			if (hasFocus) {
				mWorkspace.snapToScreen((Integer) v.getTag());
			}
		}
	}

	Workspace getWorkspace() {
		return mWorkspace;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_CREATE_SHORTCUT:
			return new CreateShortcut().createDialog();
		case DIALOG_RENAME_FOLDER:
			return new RenameFolder().createDialog();
		}

		return super.onCreateDialog(id);
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_CREATE_SHORTCUT:
			break;
		case DIALOG_RENAME_FOLDER:
			if (mFolderInfo != null) {
				EditText input = (EditText) dialog
						.findViewById(R.id.folder_name);
				final CharSequence text = mFolderInfo.title;
				input.setText(text);
				input.setSelection(0, text.length());
			}
			break;
		}
	}

	void showRenameDialog(FolderInfo info) {
		mFolderInfo = info;
		mWaitingForResult = true;
		showDialog(DIALOG_RENAME_FOLDER);
	}

	private void showAddDialog(CellLayout.CellInfo cellInfo) {
		mAddItemCellInfo = cellInfo;
		mWaitingForResult = true;
		showDialog(DIALOG_CREATE_SHORTCUT);
	}

	private void pickShortcut() {
		Bundle bundle = new Bundle();

		ArrayList<String> shortcutNames = new ArrayList<String>();
		shortcutNames.add(getString(R.string.group_applications));
		bundle.putStringArrayList(Intent.EXTRA_SHORTCUT_NAME, shortcutNames);

		ArrayList<ShortcutIconResource> shortcutIcons = new ArrayList<ShortcutIconResource>();
		shortcutIcons.add(ShortcutIconResource.fromContext(Launcher.this,
				R.drawable.ic_launcher_application));
		bundle.putParcelableArrayList(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
				shortcutIcons);

		Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
		pickIntent.putExtra(Intent.EXTRA_INTENT, new Intent(
				Intent.ACTION_CREATE_SHORTCUT));
		pickIntent.putExtra(Intent.EXTRA_TITLE,
				getText(R.string.title_select_shortcut));
		pickIntent.putExtras(bundle);

		startActivityForResult(pickIntent, REQUEST_PICK_SHORTCUT);
	}

	private class RenameFolder {
		private EditText mInput;

		Dialog createDialog() {
			final View layout = View.inflate(Launcher.this,
					R.layout.rename_folder, null);
			mInput = (EditText) layout.findViewById(R.id.folder_name);

			AlertDialog.Builder builder = new AlertDialog.Builder(Launcher.this);
			builder.setIcon(0);
			builder.setTitle(getString(R.string.rename_folder_title));
			builder.setCancelable(true);
			builder.setOnCancelListener(new Dialog.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					cleanup();
				}
			});
			builder.setNegativeButton(getString(R.string.cancel_action),
					new Dialog.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							cleanup();
						}
					});
			builder.setPositiveButton(getString(R.string.rename_action),
					new Dialog.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							changeFolderName();
						}
					});
			builder.setView(layout);

			final AlertDialog dialog = builder.create();
			dialog.setOnShowListener(new DialogInterface.OnShowListener() {
				public void onShow(DialogInterface dialog) {
					mWaitingForResult = true;
					mInput.requestFocus();
					InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
					inputManager.showSoftInput(mInput, 0);
				}
			});

			return dialog;
		}

		private void changeFolderName() {
			final String name = mInput.getText().toString();
			if (!TextUtils.isEmpty(name)) {
				// Make sure we have the right folder info
				mFolderInfo = mFolders.get(mFolderInfo.id);
				mFolderInfo.title = name;
				LauncherModel.updateItemInDatabase(Launcher.this, mFolderInfo);

				if (mWorkspaceLoading) {
					// lockAllApps();
					mModel.startLoader(Launcher.this, false);
				} else {
					final FolderIcon folderIcon = (FolderIcon) mWorkspace
							.getViewForTag(mFolderInfo);
					if (folderIcon != null) {
						folderIcon.setText(name);
						getWorkspace().requestLayout();
					} else {
						// lockAllApps();
						mWorkspaceLoading = true;
						mModel.startLoader(Launcher.this, false);
					}
				}
			}
			cleanup();
		}

		private void cleanup() {
			dismissDialog(DIALOG_RENAME_FOLDER);
			mWaitingForResult = false;
			mFolderInfo = null;
		}
	}

	// Now a part of LauncherModel.Callbacks. Used to reorder loading steps.
	public boolean isAllAppsVisible() {
		return false;
	}

	/**
	 * Displays the shortcut creation dialog and launches, if necessary, the
	 * appropriate activity.
	 */
	private class CreateShortcut implements DialogInterface.OnClickListener,
			DialogInterface.OnCancelListener,
			DialogInterface.OnDismissListener, DialogInterface.OnShowListener {

		private AddAdapter mAdapter;

		Dialog createDialog() {
			mAdapter = new AddAdapter(Launcher.this);

			final AlertDialog.Builder builder = new AlertDialog.Builder(
					Launcher.this);
			builder.setTitle(getString(R.string.menu_item_add_item));
			builder.setAdapter(mAdapter, this);

			builder.setInverseBackgroundForced(true);

			AlertDialog dialog = builder.create();
			dialog.setOnCancelListener(this);
			dialog.setOnDismissListener(this);
			dialog.setOnShowListener(this);

			return dialog;
		}

		public void onCancel(DialogInterface dialog) {
			mWaitingForResult = false;
			cleanup();
		}

		public void onDismiss(DialogInterface dialog) {
		}

		private void cleanup() {
			try {
				dismissDialog(DIALOG_CREATE_SHORTCUT);
			} catch (Exception e) {
				// An exception is thrown if the dialog is not visible, which is
				// fine
			}
		}

		/**
		 * Handle the action clicked in the "Add to home" dialog.
		 */
		public void onClick(DialogInterface dialog, int which) {
			Resources res = getResources();
			cleanup();

			switch (which) {
			case AddAdapter.ITEM_SHORTCUT: {
				// Insert extra item to handle picking application
				pickShortcut();
				break;
			}

			case AddAdapter.ITEM_APPWIDGET: {
				int appWidgetId = Launcher.this.mAppWidgetHost
						.allocateAppWidgetId();

				Intent pickIntent = new Intent(
						AppWidgetManager.ACTION_APPWIDGET_PICK);
				pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
						appWidgetId);
				// start the pick activity
				startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
				break;
			}

			case AddAdapter.ITEM_LIVE_FOLDER: {
				// Insert extra item to handle inserting folder
				Bundle bundle = new Bundle();

				ArrayList<String> shortcutNames = new ArrayList<String>();
				shortcutNames.add(res.getString(R.string.group_folder));
				bundle.putStringArrayList(Intent.EXTRA_SHORTCUT_NAME,
						shortcutNames);

				ArrayList<ShortcutIconResource> shortcutIcons = new ArrayList<ShortcutIconResource>();
				shortcutIcons.add(ShortcutIconResource.fromContext(
						Launcher.this, R.drawable.ic_launcher_folder));
				bundle.putParcelableArrayList(
						Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcons);

				Intent pickIntent = new Intent(Intent.ACTION_PICK_ACTIVITY);
				pickIntent.putExtra(Intent.EXTRA_INTENT, new Intent(
						LiveFolders.ACTION_CREATE_LIVE_FOLDER));
				pickIntent.putExtra(Intent.EXTRA_TITLE,
						getText(R.string.title_select_live_folder));
				pickIntent.putExtras(bundle);

				startActivityForResult(pickIntent, REQUEST_PICK_LIVE_FOLDER);
				break;
			}

			case AddAdapter.ITEM_WALLPAPER: {
				startWallpaper();
				break;
			}
			}
		}

		public void onShow(DialogInterface dialog) {
			mWaitingForResult = true;
		}
	}

	/**
	 * Receives notifications when applications are added/removed.
	 */
	// denglixia modify 2011.6.17
	// private class CloseSystemDialogsIntentReceiver extends BroadcastReceiver
	// {
	private class LauncherBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// denglixia modify 2011.6.17
			// closeSystemDialogs();
			String action = intent.getAction();
			Workspace workspace = (Workspace)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.workspace, context);
			if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
				closeSystemDialogs();
				if (mVibrationController != null) {
//					mVibrationController.stopVibrate();
				}
			}
			
			if(action.equals(Intent.ACTION_DATE_CHANGED) || action.equals(Intent.ACTION_TIME_CHANGED) || action.equals(Intent.ACTION_CONFIGURATION_CHANGED) || action.equals(Intent.ACTION_TIMEZONE_CHANGED)){
				if(workspace != null) {
				CalendarUtils.referenceCalendarIcon(mIconCache, Launcher.this);
				}
			} else if(action.equals(Intent.ACTION_LOCALE_CHANGED)){
				if(workspace != null) {
				CalendarUtils.referenceCalendarIcon(mIconCache, Launcher.this);
				throw new Mogoo_BootRestoreException();
				}
			}
			// else if(action.equals(POWER_ACTION))
			// {
			// if(mVibrationController != null){
			// mVibrationController.stopVibrate();
			// }
			// }

			// String reason = intent.getStringExtra("reason");
			// if (!"homekey".equals(reason)) {
			// boolean animate = true;
			// if (mPaused || "lock".equals(reason)) {
			// animate = false;
			// }
			// closeAllApps(animate);
			// }
		}
	}

	/**
	 * Receives notifications whenever the appwidgets are reset.
	 */
	private class AppWidgetResetObserver extends ContentObserver {
		public AppWidgetResetObserver() {
			super(new Handler());
		}

		@Override
		public void onChange(boolean selfChange) {
			onAppWidgetReset();
		}
	}

	/**
	 * Implementation of the method from LauncherModel.Callbacks.
	 */
	public int getCurrentWorkspaceScreen() {
		if (mWorkspace != null) {
			return mWorkspace.getCurrentScreen();
		} else {
			return SCREEN_COUNT / 2;
		}
	}

	/**
	 * Refreshes the shortcuts shown on the workspace. Implementation of the
	 * method from LauncherModel.Callbacks. 开始绑定,刷新桌面，移除桌面上所有子视图
	 */
	public void startBinding() {
		bindLocked = true;
		bindcount = 0;
		binded = 0;
		final Workspace workspace = mWorkspace;
		int count = workspace.getChildCount();
		for (int i = 1; i < count; i++) {
			// Use removeAllViewsInLayout() to avoid an extra requestLayout()
			// and invalidate().
			((ViewGroup) workspace.getChildAt(i)).removeAllViewsInLayout();
		}

		if (DEBUG_USER_INTERFACE) {
			android.widget.Button finishButton = new android.widget.Button(this);
			finishButton.setText("Finish");
			workspace.addInScreen(finishButton, 1, 0, 0, 1, 1);

			finishButton
					.setOnClickListener(new android.widget.Button.OnClickListener() {
						public void onClick(View v) {
							finish();
						}
					});
		}
	}

	/**
	 * Bind the items start-end from the list. Implementation of the method from
	 * LauncherModel.Callbacks.
	 */
	public void bindItems(List<ItemInfo> shortcuts, int start, int end) {
		
		if(Mogoo_GlobalConfig.LOG_DEBUG){
			Log.d(TAG, "--------------------bindItems()-------------start="+start+" end="+end) ;
		}
		// TODO: disabled by achellies, 检查是否在高仿机上运行
		//CheckUtils.check(this);
		if(start == 0){
			int z = shortcuts.size();
			if(z % LauncherModel.Loader.ITEMS_CHUNK == 0){
				bindcount = z/LauncherModel.Loader.ITEMS_CHUNK;
			}else{
				bindcount = z/LauncherModel.Loader.ITEMS_CHUNK + 1;
			}
		}
		final Workspace workspace = mWorkspace;
		try{
			for (int i = start; i < end; i++) {
				final ItemInfo item = shortcuts.get(i);
				mModel.getDesktopItems().add(item);
				switch (item.itemType) {
				// 生成快捷方式/应用图标，并加入到桌面中
				case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
				case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
					final View shortcut = createShortcut((ShortcutInfo) item);
					workspace.addInScreen(shortcut, item.screen, item.cellX,
							item.cellY, 1, 1, false);
					break;
				case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
					final FolderIcon newFolder = FolderIcon.fromXml(
							R.layout.folder_icon, this, (ViewGroup) workspace
									.getChildAt(workspace.getCurrentScreen()),
							(UserFolderInfo) item);
					workspace.addInScreen(newFolder, item.screen, item.cellX,
							item.cellY, 1, 1, false);
					break;
				case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:
					final FolderIcon newLiveFolder = LiveFolderIcon.fromXml(
							R.layout.live_folder_icon, this, (ViewGroup) workspace
									.getChildAt(workspace.getCurrentScreen()),
							(LiveFolderInfo) item);
					workspace.addInScreen(newLiveFolder, item.screen, item.cellX,
							item.cellY, 1, 1, false);
					break;
				// 生成文件夹图标，并加入到桌面中
				case LauncherSettings.Favorites.ITEM_TYPE_MOGOO_FOLDER:
					final View folder = createShortcut((ShortcutInfo) item);
					workspace.addInScreen(folder, item.screen, item.cellX,
							item.cellY, 1, 1, false);
					break;
				}
			}
	
			workspace.requestLayout();
			// ps by 张永辉：因为bindItems会被分段调用，所以计数监听器会被调用多次
			// ---- add by huangyue 2011-1-25
			// -- 注册计数监听,
			contentListener.onChange(true);
			DragController.dragLocked = false;
			// ---- end -----
			//因为分段调用 当 end == shortcuts.size 时为最后加载完成
			//bindcount为调用次数 binded记录已经调用的次数
			binded++;
			
			if(binded == bindcount){
				mWorkspace.refreshWorkspace();
			}
		}catch (Exception e) {
			Log.w(TAG, e.getCause());
		}
	}

	/**
	 * Implementation of the method from LauncherModel.Callbacks.
	 * 
	 * @author:魏景春
	 */
	public void bindToolbarItems(ArrayList<ItemInfo> shortcuts) {
		Mogoo_DockWorkSpace dockWorkSpace = (Mogoo_DockWorkSpace) Mogoo_ComponentBus
				.getInstance().getActivityComp(R.id.dockWorkSpace, this);
		try{
			if (dockWorkSpace != null) {
				dockWorkSpace.removeAllViewsInLayout();
				dockWorkSpace.loadToolbarItems(shortcuts);
			}
		}catch (Exception e) {
			Log.w(TAG, e.getCause());
		}

	}

	/**
	 * Implementation of the method from LauncherModel.Callbacks. 绑定图标文件夹信息
	 */
	public void bindMtFolders(HashMap<Long, Mogoo_FolderInfo> folders) {
		mtFolders.clear();
		mtFolders.putAll(folders);
	}

	/**
	 * Add the views for a widget to the workspace. Implementation of the method
	 * from LauncherModel.Callbacks.
	 */
	public void bindAppWidget(LauncherAppWidgetInfo item) {

		final long start = DEBUG_WIDGETS ? SystemClock.uptimeMillis() : 0;

		if (DEBUG_WIDGETS) {
			Log.d(TAG, "bindAppWidget: " + item);
		}

		final Workspace workspace = mWorkspace;

		final int appWidgetId = item.appWidgetId;
		final AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager
				.getAppWidgetInfo(appWidgetId);

		if (DEBUG_WIDGETS) {
			Log.d(TAG, "bindAppWidget: id=" + item.appWidgetId
					+ " belongs to component " + appWidgetInfo.provider);
		}

		item.hostView = mAppWidgetHost.createView(this, appWidgetId,
				appWidgetInfo);

		// add by 张永辉
		((LauncherAppWidgetHostView) (item.hostView)).setLauncher(this);
		// end

		item.hostView.setAppWidget(appWidgetId, appWidgetInfo);
		item.hostView.setTag(item);

		workspace.addInScreen(item.hostView, item.screen, item.cellX,
				item.cellY, item.spanX, item.spanY, false);

		workspace.requestLayout();

		mModel.getDesktopItems().add(item);

		if (DEBUG_WIDGETS) {
			Log.d(TAG, "bound widget id=" + item.appWidgetId + " in "
					+ (SystemClock.uptimeMillis() - start) + "ms");
		}
	}

	/**
	 * Callback saying that there aren't any more items to bind. Implementation
	 * of the method from LauncherModel.Callbacks.
	 */
	public void finishBindingItems() {
		if (mSavedState != null) {
			if (!mWorkspace.hasFocus()) {
				mWorkspace.getChildAt(mWorkspace.getCurrentScreen())
						.requestFocus();
			}

			// 此处可能要加上恢复图标文件夹打开的代码
			// todo
			final long[] userFolders = mSavedState
					.getLongArray(RUNTIME_STATE_USER_FOLDERS);
			if (userFolders != null) {
				for (long folderId : userFolders) {
					final FolderInfo info = mFolders.get(folderId);
					if (info != null) {
						openFolder(info);
					}
				}
				final Folder openFolder = mWorkspace.getOpenFolder();
				if (openFolder != null) {
					openFolder.requestFocus();
				}
			}

			mSavedState = null;
		}

		if (mSavedInstanceState != null) {
//			super.onRestoreInstanceState(mSavedInstanceState);
			mSavedInstanceState = null;
		}

		mWorkspaceLoading = false;
		//add by yeben 2011-11-18 除去空屏
		//mWorkspace.refreshWorkspace();
		//end
		bindLocked = false;
	}

	/**
	 * 
	 * Prints out out state for debugging.
	 */
	public void dumpState() {
		Log.d(TAG, "BEGIN launcher2 dump state for launcher " + this);
		Log.d(TAG, "mSavedState=" + mSavedState);
		Log.d(TAG, "mWorkspaceLoading=" + mWorkspaceLoading);
		Log.d(TAG, "mRestoring=" + mRestoring);
		Log.d(TAG, "mWaitingForResult=" + mWaitingForResult);
		Log.d(TAG, "mSavedInstanceState=" + mSavedInstanceState);
		Log.d(TAG, "mDesktopItems.size=" + mModel.getDesktopItems().size());
		Log.d(TAG, "mtFolders.size=" + mtFolders.size());
		mModel.dumpState();
		Log.d(TAG, "END launcher2 dump state");
	}

	// --------------------motone method statement---------

	/**
	 * 取消widget上的删除按钮 @ author:
	 */
	public void clearWidgetsDelIcon() {
		int[] widgetScreen = Mogoo_GlobalConfig.getWidgetScreen();
		for (int i : widgetScreen) {
			CellLayout cellLayout = (CellLayout) mWorkspace.getChildAt(i);
			int size = cellLayout.getChildCount();
			for (int j = 0; j < size; j++) {
				View child = cellLayout.getChildAt(j);
				if (child instanceof LauncherAppWidgetHostView) {
					((LauncherAppWidgetHostView) child).removeDelIcon();
				}
			}
		}

		isWidgetLongPress = false;
	}

	/**
	 * 设置屏幕指示器图标 @ author: 张永辉
	 * 
	 * @param whichScreen
	 *            哪一屏
	 */
	public void setIndicator(int whichScreen) {		
        //add by 袁业奔 2011-9-15 动态加屏，需清空原先指示器图标
		Mogoo_BitmapUtils.clearIndicatorImages();
		
		screenIndicator.setImageBitmap(Mogoo_BitmapUtils
				.generateIndicatorImage(mIconCache, whichScreen,
						Mogoo_GlobalConfig.getWorkspaceScreenCount()));
		Log.d(TAG, "------------setIndicator----whichScreen:"+whichScreen+"---Mogoo_GlobalConfig.getWorkspaceScreenCount()"+Mogoo_GlobalConfig.getWorkspaceScreenCount()+"-------");
	}

	/**
	 * 安装软件包 @ author: 张永辉
	 * 
	 * @param context
	 * @param packageName
	 *            包名
	 */
	public void addPackage(Context context, String packageName) {

		final List<ResolveInfo> matches = LauncherModel
				.findMainAndLauncherActivitiesForPackage(context, packageName);

		mVibrationController.stopVibrate();

		if (matches.size() > 0) {
			for (ResolveInfo info : matches) {

				ApplicationInfo appInfo = new ApplicationInfo(info, mIconCache);

				// 取得当前处于哪一屏
				int screen = mWorkspace.getCurrentScreen();

				// 标识是否己经安装
				boolean isSetup = false;

				// alter by huangyue for find contain screen before install
				// shortcut
				// if(mDragController.)
				// end

				mVibrationController.stopVibrate();

				// 如果当前屏为快捷方式屏,并且快捷方式安装在当前屏成功
				if (Mogoo_GlobalConfig.isShortcutScreen(screen)
						&& mModel.installShortcut(context, appInfo, screen)) {
					isSetup = true;
				}

				// 如果没有安装成功，则依次轮流在各个快捷方式屏中安装，直接到安装成功为止
				if (!isSetup) {
					//update by 袁业奔 2011-9-7
					//动态加屏
					for (int i = 1; i < mWorkspace.getChildCount(); i++) {
						if (i != screen
								&& mModel.installShortcut(context, appInfo,
										i)) {
							isSetup = true ;
							break;
						}
					}
					//update end
//					int[] shortcutScreen = Mogoo_GlobalConfig
//							.getShortcutScreen();
//					for (int index : shortcutScreen) {
//						Log.d(TAG, "--------------shortcutScreen---------"+index+"-----------");
//						if (index != screen
//								&& mModel.installShortcut(context, appInfo,
//										index)) {
//							isSetup = true ;
//							break;
//						}
//
//					}
					
				}
				//add by 袁业奔 2011-9-6
                //如果没有安装成功说明桌面没有空间,且未到达最大屏数
                //需增加新的一屏
				if(!isSetup && 
						(mWorkspace.getChildCount()<Mogoo_GlobalConfig.getWorkspaceScreenMaxCount())){
					//加屏，返回新曾屏的索引号
					int newScreen=addCellLayout();
					//
					int screenCount=newScreen+1;
					Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SCREEN_COUNT, screenCount);
					int[] screenType = Mogoo_GlobalConfig.createWorkspaceScreenType(screenCount);
					Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SCREEN_TYPE,screenType);
					 if(mModel.installShortcut(context, appInfo,
							 newScreen)){
						 isSetup = true ;
					 } ;
//					 Mogoo_BitmapUtils.clearIndicatorImages();
					 mWorkspace.setCurrentScreen(newScreen);
				}
				//end
				//add by 张永辉 2011-7-29
				//如果桌面没有空间提示桌面没有空间
				if(!isSetup){
					Toast.makeText(context, context.getString(R.string.screen_out_of_space),
	                        Toast.LENGTH_SHORT).show();
				}
				//end 
				//add by yeben 2012-4-11 安装完成后重启桌面
//				if(isSetup){
//					throw new Mogoo_BootRestoreException();
//				}
				//end
			}
		}
	}

	private void cellLayoutFull(int scrrenIndex) {

	}

	/**
	 * 删除包 @ author: 张永辉
	 * 
	 * @param packageName
	 *            要删除的包名
	 */
	public void removePackage(String packageName) {
		// mVibrationController.stopVibrate();
		List<String> packageNameList = new ArrayList<String>();
		packageNameList.add(packageName);
		mWorkspace.removeItems(packageNameList);
		Mogoo_DockWorkSpace dockWorkSpace = (Mogoo_DockWorkSpace) Mogoo_ComponentBus
				.getInstance().getActivityComp(R.id.dockWorkSpace, this);
		dockWorkSpace.removeItems(packageNameList);
		Mogoo_FolderWorkspace folderWorkspace = (Mogoo_FolderWorkspace) Mogoo_ComponentBus
				.getInstance().getActivityComp(R.id.folderWorkspace, this);
		folderWorkspace.removeItem(packageName);
		// 暂时使用此方法，以后采用局部删除，到时需要相关人员提供单个图标刷新接口
		// mIconCache.flush();
	}

	/**
	 * 更新包 @ author: 张永辉
	 * 
	 * @param packageName
	 *            要更新的包名
	 */
	public void updatePackage(String packageName) {
		mVibrationController.stopVibrate();
		List<String> packageNameList = new ArrayList<String>();
		packageNameList.add(packageName);
		mWorkspace.updateShortcuts(packageNameList);
		Mogoo_DockWorkSpace dockWorkSpace = (Mogoo_DockWorkSpace) Mogoo_ComponentBus
				.getInstance().getActivityComp(R.id.dockWorkSpace, this);
		dockWorkSpace.updateShortcuts(packageNameList);
		Mogoo_FolderWorkspace folderWorkspace = (Mogoo_FolderWorkspace) Mogoo_ComponentBus
				.getInstance().getActivityComp(R.id.folderWorkspace, this);
		folderWorkspace.updateItem(packageName);
		// 暂时使用全局刷新，以后采用单个单元格刷新
		// mModel.startLoader(this, true);
	}

	public Mogoo_ContentListener getContentListener() {
		return contentListener;
	}

	/**
	 * 初始化快捷方式的删除计数图标
	 * 
	 * @author: 黄悦
	 * @param favorite
	 * @param info
	 */
	private void setIconIntoBubbleText(Mogoo_BubbleTextView favorite,
			ShortcutInfo info) {
		if (!(info instanceof Mogoo_FolderInfo)
				&& info.isSystem == LauncherSettings.Favorites.NOT_SYSTEM_APP) {
			favorite.setDelIcon(mIconCache.getBitmap(R.drawable.mogoo_del));
		}
	}

	/**
	 * 增加widget
	 * 
	 * @author:张永辉
	 */
	private void addWidget() {
		mAddItemCellInfo = mMenuAddInfo;
		if (mMenuAddInfo != null && mMenuAddInfo.screen == -1) {
			mMenuAddInfo.screen = this.getCurrentWorkspaceScreen();
		}
		mWaitingForResult = true;
		int appWidgetId = Launcher.this.mAppWidgetHost.allocateAppWidgetId();

		Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
		pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		// start the pick activity
		startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
	}

	/**
	 * 取得屏幕截图
	 * 
	 * @author: 张永辉
	 * @Date：2011-5-30
	 * @return
	 */
	private Bitmap getScreenShots() {
		if (Mogoo_GlobalConfig.LOG_DEBUG) {
			Log.d(TAG, "------------------getScreenShots()-----------start="
					+ System.currentTimeMillis());
		}
		Bitmap screenImage = null;
		// 取得当前壁纸图片
		// Bitmap wallpagerImage = MT_Utilities.getWallpagerImage(this);
		// 获取屏幕的高宽
		int screenWidth = Mogoo_GlobalConfig.getScreenWidth(); // 屏幕的宽
		int screenHeight = Mogoo_GlobalConfig.getScreenHeight(); // 屏幕的高
		// 取得当前状态栏的高度
		int statusBarHeight = mWorkspace.getStatusBarHeight();
		// 生成底板
		// screenImage = Bitmap.createBitmap(wallpagerImage, 0, statusBarHeight,
		// screenWidth, screenHeight-statusBarHeight) ;
		screenImage = Bitmap.createBitmap(screenWidth, screenHeight
				- statusBarHeight, Bitmap.Config.ARGB_8888);

		CellLayout cellLayout = (CellLayout) (mWorkspace.getChildAt(mWorkspace
				.getCurrentScreen()));
		Mogoo_DockWorkSpace dockWorkSpace = (Mogoo_DockWorkSpace) Mogoo_ComponentBus
				.getInstance().getActivityComp(R.id.dockWorkSpace, this);

		cellLayout.setDrawingCacheEnabled(true);
		dockWorkSpace.setDrawingCacheEnabled(true);

		Canvas c = new Canvas(screenImage);

		Bitmap cellLayoutBitmap = cellLayout.getDrawingCache();
		Bitmap dockWorkSpaceBitmap = dockWorkSpace.getDrawingCache();

		Bitmap dockBg = mIconCache
				.getBitmap(R.drawable.mogoo_dockview_background);
		c.drawBitmap(cellLayoutBitmap, 0, 0, null);
		c.drawBitmap(dockBg, 0,
				screenHeight - statusBarHeight - dockBg.getHeight(), null);
		c.drawBitmap(dockWorkSpaceBitmap, 0, screenHeight - statusBarHeight
				- dockWorkSpace.getHeight(), null);
		cellLayoutBitmap.recycle();
		dockWorkSpaceBitmap.recycle();
		c.save();

		cellLayout.setDrawingCacheEnabled(false);
		dockWorkSpace.setDrawingCacheEnabled(false);

		if (Mogoo_GlobalConfig.LOG_DEBUG) {
			Log.d(TAG, "------------------getScreenShots()-----------end="
					+ System.currentTimeMillis());
		}
		return screenImage;
	}

	
	private Mogoo_TaskManager mTaskManagerDialog;
	
	/**
	 * 开启任务管理器
	 * 
	 * @author: 张永辉
	 * @Date：2011-5-30
	 */
	private void startTaskManager() {
//		if(mTaskManagerDialog == null){
//			mTaskManagerDialog = new TaskManagerDialog(this);
//		}
//		
//		mTaskManagerDialog.setRootView((DragLayer) findViewById(R.id.drag_layer));
//		
//		mTaskManagerDialog.show();
		if (Mogoo_GlobalConfig.LOG_DEBUG) {
			Log.d(TAG, "------------------startTaskManager()-----------start="
					+ System.currentTimeMillis());
		}

		Intent intent = new Intent();
		intent.setClass(this, Mogoo_TaskManager.class);
		screenImg = this.getScreenShots();
		intent.putExtra("com.motone.taskManager.status_bar_height",
				mWorkspace.getStatusBarHeight());
		intent.putExtra(Mogoo_TaskManager.EXTRA_ICON_COUNT,
				contentListener.getCountsArray());
		startActivity(intent);
	}

	public void unLockClick() {
		clickLocked = false;
	}

	private void lockClick() {
		if (clickLocked || Mogoo_VibrationController.isVibrate) {
			return;
		}

		clickLocked = true;

		handler.postDelayed(new Runnable() {
			public void run() {
				clickLocked = false;
			}
		}, 3000);
	}

	// ---------------------end---------------------------
	//增加一个celllayout到workspace中
	private int addCellLayout(){
		CellLayout cellLayout=(CellLayout)mInflater.inflate(R.layout.workspace_screen, null);
		cellLayout.setOnLongClickListener(this);
		mWorkspace.addView(cellLayout);
		mWorkspace.requestLayout();
		if(Mogoo_GlobalConfig.LOG_DEBUG){
			Log.d(TAG, "-----------addCellLayout()--index:"+mWorkspace.indexOfChild(cellLayout)+"----");
		}
		return mWorkspace.indexOfChild(cellLayout);
	}
	//-------------------add end----------------------
	public Mogoo_VibrationController getVibrationController(){
		return mVibrationController;
	}

	public void loadCellLayout() {
		// TODO Auto-generated method stub
		//除了search屏外还需要的屏数
		int needScreen=mModel.getTotalScreen(Launcher.this);
		if(needScreen < Mogoo_GlobalConfig.getWorkspaceScreenMinCount() - 1){
			needScreen = Mogoo_GlobalConfig.getWorkspaceScreenMaxCount();
//			Log.e(TAG, "needScreen = " + needScreen, null);
//			throw new Mogoo_BootRestoreException("needScreen = " + needScreen);
		}
		Log.i(TAG, "needScreen = " + needScreen);
		if(Mogoo_GlobalConfig.LOG_DEBUG){
			Log.d(TAG, "needScreen = " + needScreen);
		}
		
		//search占了第一屏
		int screenCount=needScreen+1;
		//加载最少屏数
		if(screenCount<Mogoo_GlobalConfig.getWorkspaceScreenMinCount()){
			screenCount=Mogoo_GlobalConfig.getWorkspaceScreenMinCount();
			needScreen=screenCount-1;
		}
		int[] screenType = Mogoo_GlobalConfig.createWorkspaceScreenType(screenCount);
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SCREEN_TYPE,screenType);
		Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SCREEN_COUNT, screenCount);
        
		for (int i = 0; i < needScreen; i++) {
			addCellLayout();
		}
		//初始化
		if(mModel.getItemCountFromDB(this)<mModel.getItemCountFromPackageManager(this)){
			int z = mModel.getScreenCountByPackageManager(this);
			if(needScreen < z){
				screenCount = z + 1;
				screenType = Mogoo_GlobalConfig.createWorkspaceScreenType(screenCount);
				Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SCREEN_TYPE,screenType);
				Mogoo_GlobalConfig.setConfigParm(Mogoo_GlobalConfig.WORKSPACE_SCREEN_COUNT, screenCount);
                for (int i = 0; i < z - needScreen; i++) {
                	addCellLayout();
				}
			}
		}
	}
}