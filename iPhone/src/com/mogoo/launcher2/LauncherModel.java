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

import java.lang.ref.WeakReference;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Parcelable;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.CellLayout.LayoutParams;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_BitmapUtils;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;
import com.mogoo.launcher2.utils.Mogoo_Utilities;

/**
 * Maintains in-memory state of the Launcher. It is expected that there should
 * be only one LauncherModel object held in a static. Also provide APIs for
 * updating the database state for the Launcher.
 */
public class LauncherModel extends BroadcastReceiver {
	static final boolean DEBUG_LOADERS = false;

	static final boolean PROFILE_LOADERS = false;

	static final String TAG = "Launcher.Model";

	// private int mBatchSize; // 0 is all apps at once
	// private int mAllAppsLoadDelay; // milliseconds between batches

	private final LauncherApplication mApp;

	private final Object mLock = new Object();

	private DeferredHandler mHandler = new DeferredHandler();

	private Loader mLoader = new Loader();

	// We start off with everything not loaded. After that, we assume that
	// our monitoring of the package manager provides all updates and we never
	// need to do a requery. These are only ever touched from the loader thread.
	private boolean mWorkspaceLoaded;

	private boolean mAllAppsLoaded;

	private boolean mBeforeFirstLoad = true; // only access this from main
												// thread

	private WeakReference<Callbacks> mCallbacks;

	// private final Object mAllAppsListLock = new Object();
	// private AllAppsList mAllAppsList;
	private IconCache mIconCache;

	private Bitmap mDefaultIcon;

	// --------------------motone field statement---------

	// 用于保存成功加载到桌面中的快捷方式对象、文件夹对象、widget对象。注：该变量为从launcher.java中移过来的
	private ArrayList<ItemInfo> desktopItems = new ArrayList<ItemInfo>();

	// --------------add by 魏景春----------
	// 工具栏图标数组
	private ArrayList<ItemInfo> mToolbarItems = new ArrayList<ItemInfo>();

	// --------------end---------------

	// ---- add by huangyue ---
	private static final SortedSet<String> saveLayout = new TreeSet<String>();

	private static final SortedSet<String> buffsaveLayout = new TreeSet<String>();
	
	private static final ArrayList<String> packageNameList = new ArrayList<String>();

	private static boolean saving = false;

	// ---- end ----

	// ---------------------end---------------------------

	public interface Callbacks {
		public int getCurrentWorkspaceScreen();

		public void startBinding();

		public void bindItems(List<ItemInfo> shortcuts, int start, int end);

		public void finishBindingItems();

		public void bindAppWidget(LauncherAppWidgetInfo info);

		public boolean isAllAppsVisible();

		// add by 张永辉 2011-1-22
		
		public void addPackage(Context context, String packageName);

		public void updatePackage(String packageName);

		public void removePackage(String packageName);

		public void bindMtFolders(HashMap<Long, Mogoo_FolderInfo> folders);

		// end
		// 加载dock工具栏图标
		public void bindToolbarItems(ArrayList<ItemInfo> shortcuts); // add by
																		// 魏景春
	}

	LauncherModel(LauncherApplication app, IconCache iconCache) {
		mApp = app;
		// mAllAppsList = new AllAppsList(iconCache);
		mIconCache = iconCache;

		mDefaultIcon = Utilities.createIconBitmap(app.getPackageManager()
				.getDefaultActivityIcon(), app);

		// mAllAppsLoadDelay =
		// app.getResources().getInteger(R.integer.config_allAppsBatchLoadDelay);

		// mBatchSize =
		// app.getResources().getInteger(R.integer.config_allAppsBatchSize);
	}

	public Bitmap getFallbackIcon() {
		return Bitmap.createBitmap(mDefaultIcon);
	}

	/**
	 * Adds an item to the DB if it was not created previously, or move it to a
	 * new <container, screen, cellX, cellY>
	 */
	static void addOrMoveItemInDatabase(Context context, ItemInfo item,
			long container, int screen, int cellX, int cellY) {
		if (item.container == ItemInfo.NO_ID) {
			// From all apps
			addItemToDatabase(context, item, container, screen, cellX, cellY,
					false);
		} else {
			// From somewhere else
			moveItemInDatabase(context, item, container, screen, cellX, cellY);
		}
	}

	/**
	 * Move an item in the DB to a new <container, screen, cellX, cellY>
	 */
	public static void moveItemInDatabase(Context context, ItemInfo item,
			long container, int screen, int cellX, int cellY) {
		if (item == null) {
			return;
		}

		item.container = container;
		item.screen = screen;
		item.cellX = cellX;
		item.cellY = cellY;

		final ContentValues values = new ContentValues();
		final ContentResolver cr = context.getContentResolver();

		values.put(LauncherSettings.Favorites.CONTAINER, item.container);
		values.put(LauncherSettings.Favorites.CELLX, item.cellX);
		values.put(LauncherSettings.Favorites.CELLY, item.cellY);
		values.put(LauncherSettings.Favorites.SCREEN, item.screen);

		cr.update(LauncherSettings.Favorites.getContentUri(item.id, false),
				values, null, null);
	}

	/**
	 * 方法的功能描述 @ author: 黄悦
	 * 
	 * @param redId
	 * @param childId
	 */
	public static void setSortIconInfo(int redId, int childId) {
		if (!saving) {
			synchronized (saveLayout) {
				if (redId == R.id.workspace) {
					saveLayout.add(redId + "_" + childId);
				} else if (redId == R.id.dockWorkSpace) {
					saveLayout.add(redId + "");
				} else if (redId == R.id.folderWorkspace) {
					saveLayout.add(redId + "_" + childId);
				}
			}
		} else {
			synchronized (buffsaveLayout) {
				if (redId == R.id.workspace) {
					buffsaveLayout.add(redId + "_" + childId);
				} else if (redId == R.id.dockWorkSpace) {
					buffsaveLayout.add(redId + "");
				} else if (redId == R.id.folderWorkspace) {
					saveLayout.add(redId + "_" + childId);
				}
			}
		}
	}

	/**
	 * 保存所有经过sort的组件 @ author: 黄悦
	 * 
	 * @param cxt
	 */
	public static void saveAllIconInfo(final Context cxt) {
		if (!saving) {
			new Thread() {
				@Override
				public void run() {
					synchronized (saveLayout) {
						saving = true;
						Mogoo_ComponentBus bus = Mogoo_ComponentBus
								.getInstance();
						Workspace workspace = (Workspace) bus.getActivityComp(
								R.id.workspace, cxt);
						Mogoo_DockWorkSpace dock = (Mogoo_DockWorkSpace) bus
								.getActivityComp(R.id.dockWorkSpace, cxt);
						Mogoo_FolderWorkspace folderWorkspace = (Mogoo_FolderWorkspace) bus
								.getActivityComp(R.id.folderWorkspace, cxt);
						StringBuffer sb = new StringBuffer();
						StringBuffer sub = null;
						Iterator<String> itr = saveLayout.iterator();
						String resId = null;

						while (itr.hasNext()) {
							int index = 0;
							sb.append(itr.next());
							index = sb.indexOf("_");
							if (index == -1) {
								resId = sb.toString();
							} else {
								resId = sb.substring(0, index);
							}
							sub = sb.delete(0, index + 1);

							switch (new Integer(resId)) {
							case R.id.workspace:
								saveWorksapce(cxt, workspace, sub);
								break;
							case R.id.dockWorkSpace:
								saveDockspcae(cxt, dock);
								break;
							case R.id.folderWorkspace:
								saveFolderWorkspace(cxt, folderWorkspace, sub);
								break;
							default:
								break;
							}

							sb.delete(0, sb.length());
							sub.delete(0, sub.length());
						}

						dock = null;
						workspace = null;
						saveLayout.clear();
						saving = false;

						if (buffsaveLayout.size() != 0) {
							saveLayout.addAll(buffsaveLayout);
							buffsaveLayout.clear();
							saveAllIconInfo(cxt);
						}
					}
				}

				private void saveWorksapce(Context cxt, Workspace workspace,
						StringBuffer sb) {
					View view;
					CellLayout layout = null;
					LayoutParams lp = null;
					int cellXY[] = null;
					if (sb.length() < 1) {
						return;
					}

					int i = new Integer(sb.toString());
					layout = (CellLayout) workspace.getChildAt(i);

					for (int j = 0; j < layout.getChildCount(); j++) {
						view = layout.getChildAt(j);
						cellXY = layout.convertToCell(j);
						LauncherModel.moveItemInDatabase(cxt,
								(ItemInfo) view.getTag(),
								LauncherSettings.Favorites.CONTAINER_DESKTOP,
								i, cellXY[0], cellXY[1]);
					}

					layout = null;
				}

				private void saveDockspcae(Context cxt, Mogoo_DockWorkSpace dock) {
					View view;
					for (int i = 0; i < dock.getChildCount(); i++) {
						view = dock.getChildAt(i);
						LauncherModel.moveItemInDatabase(cxt,
								(ItemInfo) view.getTag(),
								LauncherSettings.Favorites.CONTAINER_TOOLBAR,
								-1, i, 0);
					}
				}

				private void saveFolderWorkspace(Context cxt,
						Mogoo_FolderWorkspace folderWorkspace, StringBuffer sb) {
					View view;
					ItemInfo info;
					int cellXY[];

					int folderId = new Integer(sb.toString());

					if (folderId == -1) {
						return;
					}

					for (int i = 0; i < folderWorkspace.getChildCount(); i++) {
						view = folderWorkspace.getChildAt(i);
						info = (ItemInfo) view.getTag();
						cellXY = Mogoo_Utilities.convertToCell(i);

						if (Mogoo_GlobalConfig.LOG_DEBUG) {
							Log.d(TAG,
									"-------------saveFolderWorkspace-----folderId="
											+ folderId + " infoId=" + info.id
											+ " cellX=" + cellXY[0] + " cellY="
											+ cellXY[1]);
						}

						LauncherModel.moveItemInDatabase(cxt, info, folderId,
								Launcher.getScreen(), cellXY[0], cellXY[1]);
					}
				}
			}.start();

		}
	}

	/**
	 * Returns true if the shortcuts already exists in the database. we identify
	 * a shortcut by its title and intent.
	 */
	static boolean shortcutExists(Context context, String title, Intent intent) {
		final ContentResolver cr = context.getContentResolver();
		Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI,
				new String[] { "title", "intent" }, "title=? and intent=?",
				new String[] { title, intent.toUri(0) }, null);
		boolean result = false;
		try {
			result = c.moveToFirst();
		} finally {
			c.close();
		}
		return result;
	}

	/**
	 * Find a folder in the db, creating the FolderInfo if necessary, and adding
	 * it to folderList.
	 */
	FolderInfo getFolderById(Context context,
			HashMap<Long, FolderInfo> folderList, long id) {
		final ContentResolver cr = context.getContentResolver();
		Cursor c = cr
				.query(LauncherSettings.Favorites.CONTENT_URI,
						null,
						"_id=? and (itemType=? or itemType=?)",
						new String[] {
								String.valueOf(id),
								String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER),
								String.valueOf(LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER) },
						null);

		try {
			if (c.moveToFirst()) {
				final int itemTypeIndex = c
						.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
				final int titleIndex = c
						.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
				final int containerIndex = c
						.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
				final int screenIndex = c
						.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
				final int cellXIndex = c
						.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
				final int cellYIndex = c
						.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);

				FolderInfo folderInfo = null;
				switch (c.getInt(itemTypeIndex)) {
				case LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER:
					folderInfo = findOrMakeUserFolder(folderList, id);
					break;
				case LauncherSettings.Favorites.ITEM_TYPE_LIVE_FOLDER:
					folderInfo = findOrMakeLiveFolder(folderList, id);
					break;
				}

				folderInfo.title = c.getString(titleIndex);
				folderInfo.id = id;
				folderInfo.container = c.getInt(containerIndex);
				folderInfo.screen = c.getInt(screenIndex);
				folderInfo.cellX = c.getInt(cellXIndex);
				folderInfo.cellY = c.getInt(cellYIndex);
				
				c.close();

				return folderInfo;
			}
		} finally {
			c.close();
		}

		return null;
	}

	// end
	/**
	 * 获取所需要的总屏数 @ author: 袁业奔 2011-9-7
	 * 
	 */
	public int getTotalScreen(Context context) {
    	final ContentResolver cr = context.getContentResolver();
    	int result=0;
    	Cursor c = null;
        try {
			c = cr.query(LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, 
					new String[] { "Max(screen)" }, "container = " + LauncherSettings.Favorites.CONTAINER_DESKTOP, null, null);
			c.moveToFirst();
			result = c.getInt(0);
        }
        catch (Exception e) {
        	e.printStackTrace();
        }finally {
        	if(c != null){
                c.close();
        	}
        }
        return result;
	}
	/**
	 * 获取数据库中桌面图标记录数
	 */
    public int getItemCountFromDB(Context context){
    	final ContentResolver cr = context.getContentResolver();
    	int result=0;
    	Cursor c = null;
        try {
			c = cr.query(LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION, 
					null, "container = " + LauncherSettings.Favorites.CONTAINER_DESKTOP + " and " + LauncherSettings.Favorites.ITEM_TYPE + " <> " + LauncherSettings.Favorites.ITEM_TYPE_USER_FOLDER, null, null);
			result = c.getCount();
        }
        catch (Exception e) {
        	e.printStackTrace();
        }finally {
        	if(c != null){
                c.close();
        	}
        }    
        return result;
    }
    /**
     * 获取 PackageManager 应用数量
     * @param context
     * @return
     */
	public int getItemCountFromPackageManager(Context context){
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final PackageManager packageManager = context.getPackageManager();

        List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        return apps.size();
	}
	/**
	 * 根据PackageManager中应用程序的数量返回所需加载的屏数
	 * yyb
	 * @return
	 */
	public int getScreenCountByPackageManager(Context context){
		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final PackageManager packageManager = context.getPackageManager();

        List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);
        
        if(apps.size()%Mogoo_GlobalConfig.getWorkspaceCellCounts()==0){
        	return apps.size()/Mogoo_GlobalConfig.getWorkspaceCellCounts();
        }else{
        	return apps.size()/Mogoo_GlobalConfig.getWorkspaceCellCounts() +1;
        }
	}
	// end
	/**
	 * 判断是否存在容器为workspace的item @ author: 袁业奔 2011-9-15
	 * 
	 */
	private boolean isExistWorkspaceContainer(Context context, int screen) {
		final ContentResolver cr = context.getContentResolver();
		Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI, null,
				"screen = " + screen + " and container = -100", null, null);
		boolean result = false;
		try {
			if (c.getCount() != 0) {
				result = true;
			}
		} finally {
			c.close();
		}
		return result;
	}

	/**
	 * Add an item to the database in a specified container. Sets the container,
	 * screen, cellX and cellY fields of the item. Also assigns an ID to the
	 * item.
	 */
	public static void addItemToDatabase(Context context, ItemInfo item,
			long container, int screen, int cellX, int cellY, boolean notify) {
		item.container = container;
		item.screen = screen;
		item.cellX = cellX;
		item.cellY = cellY;

		final ContentValues values = new ContentValues();
		final ContentResolver cr = context.getContentResolver();

		item.onAddToDatabase(values);

		Uri result = cr.insert(notify ? LauncherSettings.Favorites.CONTENT_URI
				: LauncherSettings.Favorites.CONTENT_URI_NO_NOTIFICATION,
				values);

		if (result != null) {
			item.id = Integer.parseInt(result.getPathSegments().get(1));
		}
	}

	/**
	 * add by huangyue for add record of click data
	 */
	public static void addClickItemToDatabase(Context context,
			String packageName) {
		if (packageName == null) {
			return;
		}

		final ContentValues values = new ContentValues();
		final ContentResolver cr = context.getContentResolver();
		Cursor cursor = null;

		try {
			cursor = cr.query(
					LauncherSettings.Favorites.ACTIVE_URI_NO_NOTIFICATION,
					null, LauncherSettings.Favorites.PACKAGE + "=?",
					new String[] { packageName }, null);

			if (cursor.getCount() == 0) {
				values.put(LauncherSettings.Favorites.PACKAGE, packageName);
				values.put(LauncherSettings.Favorites.ACTIVE_DATE,
						System.currentTimeMillis());

				Uri result = cr.insert(
						LauncherSettings.Favorites.ACTIVE_URI_NO_NOTIFICATION,
						values);
			}
		} catch (Exception e) {
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * Update an item to the database in a specified container.
	 */
	public static void updateItemInDatabase(Context context, ItemInfo item) {
		final ContentValues values = new ContentValues();
		final ContentResolver cr = context.getContentResolver();

		item.onAddToDatabase(values);

		cr.update(LauncherSettings.Favorites.getContentUri(item.id, false),
				values, null, null);
	}

	/**
	 * Removes the specified item from the database
	 * 
	 * @param context
	 * @param item
	 */
	static void deleteItemFromDatabase(Context context, ItemInfo item) {
		final ContentResolver cr = context.getContentResolver();

		cr.delete(LauncherSettings.Favorites.getContentUri(item.id, false),
				null, null);
	}

	/**
	 * Remove the contents of the specified folder from the database
	 */
	static void deleteUserFolderContentsFromDatabase(Context context,
			UserFolderInfo info) {
		final ContentResolver cr = context.getContentResolver();

		cr.delete(LauncherSettings.Favorites.getContentUri(info.id, false),
				null, null);
		cr.delete(LauncherSettings.Favorites.CONTENT_URI,
				LauncherSettings.Favorites.CONTAINER + "=" + info.id, null);
	}

	/**
	 * Set this as the current Launcher activity object for the loader.
	 */
	public void initialize(Callbacks callbacks) {
		synchronized (mLock) {
			mCallbacks = new WeakReference<Callbacks>(callbacks);
		}
	}

	public void startLoader(Context context, boolean isLaunching) {
		mLoader.startLoader(context, isLaunching);
	}

	public void stopLoader() {
		mLoader.stopLoader();
	}

	/**
	 * Call from the handler for ACTION_PACKAGE_ADDED, ACTION_PACKAGE_REMOVED
	 * and ACTION_PACKAGE_CHANGED.
	 */
	public void onReceive(Context context, Intent intent) {

		final Context cxt = context;

		// add by 张永辉 2011-1-22 重写软件包安装、删除、更新后快捷方式加载到桌面的功能
		final String action = intent.getAction();

		if (Mogoo_GlobalConfig.LOG_DEBUG) {
			Log.d(TAG, "action:" + action);
		}

		if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
				|| Intent.ACTION_PACKAGE_REMOVED.equals(action)
				|| Intent.ACTION_PACKAGE_ADDED.equals(action)) {

			final String packageName = intent.getData().getSchemeSpecificPart();
			final boolean replacing = intent.getBooleanExtra(
					Intent.EXTRA_REPLACING, false);

			if (Mogoo_GlobalConfig.LOG_DEBUG) {
				Log.d(TAG, "packageName:" + packageName);
				Log.d(TAG, "replacing:" + replacing);
			}

			if (packageName == null || packageName.length() == 0) {
				// they sent us a bad intent
				return;
			}

			final Callbacks callbacks = mCallbacks != null ? mCallbacks.get()
					: null;
			if (callbacks == null) {
				Log.w(TAG,
						"Nobody to tell about the new app.  Launcher is probably loading.");
				return;
			}

			if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {// 更新软件包
				mHandler.post(new Runnable() {
					public void run() {
						callbacks.updatePackage(packageName);
					}
				});
			} else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
				if (!replacing) { // 删除软件包
					mHandler.post(new Runnable() {
						public void run() {
							callbacks.removePackage(packageName);
						}
					});
				}
				// else, we are replacing the package, so a PACKAGE_ADDED will
				// be sent
				// later, we will update the package at this time
			} else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
				if (!replacing) {// 增加软件包
					mHandler.post(new Runnable() {
						public void run() {
							callbacks.addPackage(cxt, packageName);
						}
					});
				} else {// 更新软件包
					mHandler.post(new Runnable() {
						public void run() {
							callbacks.updatePackage(packageName);
						}
					});
				}
			}
		} else {
			if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)) {
				String packages[] = intent
						.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
				if (packages == null || packages.length == 0) {
					return;
				}
				synchronized (this) {
					mAllAppsLoaded = mWorkspaceLoaded = false;
				}
				startLoader(context, false);
			} else if (Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE
					.equals(action)) {
				String packages[] = intent
						.getStringArrayExtra(Intent.EXTRA_CHANGED_PACKAGE_LIST);
				if (packages == null || packages.length == 0) {
					return;
				}
				synchronized (this) {
					mAllAppsLoaded = mWorkspaceLoaded = false;
				}
				startLoader(context, false);
			}
		}
		// end

		// delete by 张永辉 2011-1-22 重写软件包安装、删除、更新后快捷方式加载到桌面的功能
		// Use the app as the context.
		// context = mApp;
		//
		// ArrayList<ApplicationInfo> added = null;
		// ArrayList<ApplicationInfo> removed = null;
		// ArrayList<ApplicationInfo> modified = null;
		//
		// if (mBeforeFirstLoad) {
		// // If we haven't even loaded yet, don't bother, since we'll just pick
		// // up the changes.
		// return;
		// }
		//
		// synchronized (mAllAppsListLock) {
		// final String action = intent.getAction();
		//
		// if (Intent.ACTION_PACKAGE_CHANGED.equals(action)
		// || Intent.ACTION_PACKAGE_REMOVED.equals(action)
		// || Intent.ACTION_PACKAGE_ADDED.equals(action)) {
		// final String packageName = intent.getData().getSchemeSpecificPart();
		// final boolean replacing =
		// intent.getBooleanExtra(Intent.EXTRA_REPLACING, false);
		//
		// if (packageName == null || packageName.length() == 0) {
		// // they sent us a bad intent
		// return;
		// }
		//
		// if (Intent.ACTION_PACKAGE_CHANGED.equals(action)) {
		// mAllAppsList.updatePackage(context, packageName);
		// } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
		// if (!replacing) {
		// mAllAppsList.removePackage(packageName);
		// }
		// // else, we are replacing the package, so a PACKAGE_ADDED will be
		// sent
		// // later, we will update the package at this time
		// } else if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
		// if (!replacing) {
		// mAllAppsList.addPackage(context, packageName);
		// } else {
		// mAllAppsList.updatePackage(context, packageName);
		// }
		// }
		//
		// if (mAllAppsList.added.size() > 0) {
		// added = mAllAppsList.added;
		// mAllAppsList.added = new ArrayList<ApplicationInfo>();
		// }
		// if (mAllAppsList.removed.size() > 0) {
		// removed = mAllAppsList.removed;
		// mAllAppsList.removed = new ArrayList<ApplicationInfo>();
		// for (ApplicationInfo info: removed) {
		// mIconCache.remove(info.intent.getComponent());
		// }
		// }
		// if (mAllAppsList.modified.size() > 0) {
		// modified = mAllAppsList.modified;
		// mAllAppsList.modified = new ArrayList<ApplicationInfo>();
		// }
		//
		// final Callbacks callbacks = mCallbacks != null ? mCallbacks.get() :
		// null;
		// if (callbacks == null) {
		// Log.w(TAG,
		// "Nobody to tell about the new app.  Launcher is probably loading.");
		// return;
		// }
		//
		// if (added != null) {
		// final ArrayList<ApplicationInfo> addedFinal = added;
		// mHandler.post(new Runnable() {
		// public void run() {
		// callbacks.bindAppsAdded(addedFinal);
		// }
		// });
		// }
		// if (modified != null) {
		// final ArrayList<ApplicationInfo> modifiedFinal = modified;
		// mHandler.post(new Runnable() {
		// public void run() {
		// callbacks.bindAppsUpdated(modifiedFinal);
		// }
		// });
		// }
		// if (removed != null) {
		// final ArrayList<ApplicationInfo> removedFinal = removed;
		// mHandler.post(new Runnable() {
		// public void run() {
		// callbacks.bindAppsRemoved(removedFinal);
		// }
		// });
		// }
		// } else {
		// if (Intent.ACTION_EXTERNAL_APPLICATIONS_AVAILABLE.equals(action)) {
		// String packages[] = intent.getStringArrayExtra(
		// Intent.EXTRA_CHANGED_PACKAGE_LIST);
		// if (packages == null || packages.length == 0) {
		// return;
		// }
		// synchronized (this) {
		// mAllAppsLoaded = mWorkspaceLoaded = false;
		// }
		// startLoader(context, false);
		// } else if
		// (Intent.ACTION_EXTERNAL_APPLICATIONS_UNAVAILABLE.equals(action)) {
		// String packages[] = intent.getStringArrayExtra(
		// Intent.EXTRA_CHANGED_PACKAGE_LIST);
		// if (packages == null || packages.length == 0) {
		// return;
		// }
		// synchronized (this) {
		// mAllAppsLoaded = mWorkspaceLoaded = false;
		// }
		// startLoader(context, false);
		// }
		// }
		// }
		// end
	}

	public class Loader {
		public static final int ITEMS_CHUNK = 6;

		private LoaderThread mLoaderThread;

		final Vector<ItemInfo> mItems = new Vector<ItemInfo>();

		final ArrayList<LauncherAppWidgetInfo> mAppWidgets = new ArrayList<LauncherAppWidgetInfo>();

		// final HashMap<Long, FolderInfo> mFolders = new HashMap<Long,
		// FolderInfo>();

		// 用于保存图标文件信息
		final HashMap<Long, Mogoo_FolderInfo> mtFolders = new HashMap<Long, Mogoo_FolderInfo>();

		/**
		 * Call this from the ui thread so the handler is initialized on the
		 * correct thread.
		 */
		public Loader() {
		}

		public void startLoader(Context context, boolean isLaunching) {
			synchronized (mLock) {
				if (DEBUG_LOADERS) {
					Log.d(TAG, "startLoader isLaunching=" + isLaunching);
				}

				// Don't bother to start the thread if we know it's not going to
				// do anything
				if (mCallbacks != null && mCallbacks.get() != null) {
					LoaderThread oldThread = mLoaderThread;
					if (oldThread != null) {
						if (oldThread.isLaunching()) {
							// don't downgrade isLaunching if we're already
							// running
							isLaunching = true;
						}
						oldThread.stopLocked();
					}
					mLoaderThread = new LoaderThread(context, oldThread,
							isLaunching);
					mLoaderThread.start();
				}
			}
		}

		public void stopLoader() {
			synchronized (mLock) {
				if (mLoaderThread != null) {
					mLoaderThread.stopLocked();
				}
			}
		}

		/**
		 * Runnable for the thread that loads the contents of the launcher: -
		 * workspace icons - widgets - all apps icons
		 */
		private class LoaderThread extends Thread {
			private Context mContext;

			private Thread mWaitThread;

			private boolean mIsLaunching;

			private boolean mStopped;

			private boolean mLoadAndBindStepFinished;

			LoaderThread(Context context, Thread waitThread, boolean isLaunching) {
				mContext = context;
				mWaitThread = waitThread;
				mIsLaunching = isLaunching;
			}

			boolean isLaunching() {
				return mIsLaunching;
			}

			/**
			 * If another LoaderThread was supplied, we need to wait for that to
			 * finish before we start our processing. This keeps the ordering of
			 * the setting and clearing of the dirty flags correct by making
			 * sure we don't start processing stuff until they've had a chance
			 * to re-set them. We do this waiting the worker thread, not the ui
			 * thread to avoid ANRs.
			 */
			private void waitForOtherThread() {
				if (mWaitThread != null) {
					boolean done = false;
					while (!done) {
						try {
							mWaitThread.join();
							done = true;
						} catch (InterruptedException ex) {
							// Ignore
						}
					}
					mWaitThread = null;
				}
			}

			private void loadAndBindWorkspace() {
				// Load the workspace

				// Other other threads can unset mWorkspaceLoaded, so atomically
				// set it,
				// and then if they unset it, or we unset it because of
				// mStopped, it will
				// be unset.
				boolean loaded;
				synchronized (this) {
					loaded = mWorkspaceLoaded;
					mWorkspaceLoaded = true;
				}

				// For now, just always reload the workspace. It's ~100 ms vs.
				// the
				// binding which takes many hundreds of ms.
				// We can reconsider.
				if (DEBUG_LOADERS)
					Log.d(TAG, "loadAndBindWorkspace loaded=" + loaded);
				if (true || !loaded) {
					loadWorkspace();
					if (mStopped) {
						mWorkspaceLoaded = false;
						return;
					}
				}

				// Bind the workspace
                if(Launcher.bindLocked){
                	return;
                }
				bindWorkspace();
			}

			private void waitForIdle() {
				// Wait until the either we're stopped or the other threads are
				// done.
				// This way we don't start loading all apps until the workspace
				// has settled
				// down.
				synchronized (LoaderThread.this) {
					final long workspaceWaitTime = DEBUG_LOADERS ? SystemClock
							.uptimeMillis() : 0;

					mHandler.postIdle(new Runnable() {
						public void run() {
							synchronized (LoaderThread.this) {
								mLoadAndBindStepFinished = true;
								if (DEBUG_LOADERS) {
									Log.d(TAG,
											"done with previous binding step");
								}
								LoaderThread.this.notify();
							}
						}
					});

					while (!mStopped && !mLoadAndBindStepFinished) {
						try {
							this.wait();
						} catch (InterruptedException ex) {
							// Ignore
						}
					}
					if (DEBUG_LOADERS) {
						Log.d(TAG,
								"waited "
										+ (SystemClock.uptimeMillis() - workspaceWaitTime)
										+ "ms for previous step to finish binding");
					}
				}
			}

			public void run() {
				waitForOtherThread();

				// Optimize for end-user experience: if the Launcher is up and
				// // running with the
				// All Apps interface in the foreground, load All Apps first.
				// Otherwise, load the
				// workspace first (default).
				final Callbacks cbk = mCallbacks.get();
				final boolean loadWorkspaceFirst = cbk != null ? (!cbk
						.isAllAppsVisible()) : true;

				// Elevate priority when Home launches for the first time to
				// avoid
				// starving at boot time. Staring at a blank home is not cool.
				synchronized (mLock) {
					android.os.Process
							.setThreadPriority(mIsLaunching ? Process.THREAD_PRIORITY_DEFAULT
									: Process.THREAD_PRIORITY_BACKGROUND);
				}

				if (PROFILE_LOADERS) {
					android.os.Debug
							.startMethodTracing("/sdcard/launcher-loaders");
				}

				if (loadWorkspaceFirst) {
					if (DEBUG_LOADERS)
						Log.d(TAG,
								"step 1: loading workspace + "
										+ System.currentTimeMillis());
					loadAndBindWorkspace();
				} else {
					if (DEBUG_LOADERS)
						Log.d(TAG, "step 1: special: loading all apps");
					// delete by 张永辉 2011-1-23 不加载抽屉
					// loadAndBindAllApps();
					// end
				}

				// Whew! Hard work done.
				synchronized (mLock) {
					if (mIsLaunching) {
						android.os.Process
								.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
					}
				}

				// second step
				if (loadWorkspaceFirst) {
					if (DEBUG_LOADERS)
						Log.d(TAG, "step 2: loading all apps");
					// delete by 张永辉 2011-1-23 不加载抽屉
					// loadAndBindAllApps();
					// end
				} else {
					if (DEBUG_LOADERS)
						Log.d(TAG, "step 2: special: loading workspace");
					loadAndBindWorkspace();
				}

				// Clear out this reference, otherwise we end up holding it
				// until all of the
				// callback runnables are done.
				mContext = null;

				synchronized (mLock) {
					// Setting the reference is atomic, but we can't do it
					// inside the other critical
					// sections.
					mLoaderThread = null;
				}

				if (PROFILE_LOADERS) {
					android.os.Debug.stopMethodTracing();
				}

				// Trigger a gc to try to clean up after the stuff is done,
				// since the
				// renderscript allocations aren't charged to the java heap.
				mHandler.post(new Runnable() {
					public void run() {
						System.gc();
					}
				});
			}

			public void stopLocked() {
				synchronized (LoaderThread.this) {
					mStopped = true;
					this.notify();
				}
			}

			/**
			 * Gets the callbacks object. If we've been stopped, or if the
			 * launcher object has somehow been garbage collected, return null
			 * instead. Pass in the Callbacks object that was around when the
			 * deferred message was scheduled, and if there's a new Callbacks
			 * object around then also return null. This will save us from
			 * calling onto it with data that will be ignored.
			 */
			Callbacks tryGetCallbacks(Callbacks oldCallbacks) {
				synchronized (mLock) {
					if (mStopped) {
						return null;
					}

					if (mCallbacks == null) {
						return null;
					}

					final Callbacks callbacks = mCallbacks.get();
					if (callbacks != oldCallbacks) {
						return null;
					}
					if (callbacks == null) {
						Log.w(TAG, "no mCallbacks");
						return null;
					}

					return callbacks;
				}
			}

			/*
			 * // check & update map of what's occupied; used to discard //
			 * overlapping/invalid items private boolean
			 * checkItemPlacement(ItemInfo occupied[][][], ItemInfo item) { if
			 * (item.container != LauncherSettings.Favorites.CONTAINER_DESKTOP)
			 * { return true; }
			 * 
			 * for (int x = item.cellX; x < (item.cellX + item.spanX); x++) {
			 * for (int y = item.cellY; y < (item.cellY + item.spanY); y++) { if
			 * (occupied[item.screen][x][y] != null) { Log.e(TAG,
			 * "Error loading shortcut " + item + " into cell (" + item.screen +
			 * ":" + x + "," + y + ") occupied by " +
			 * occupied[item.screen][x][y]); return false; } } } for (int x =
			 * item.cellX; x < (item.cellX + item.spanX); x++) { for (int y =
			 * item.cellY; y < (item.cellY + item.spanY); y++) {
			 * occupied[item.screen][x][y] = item; } } return true; }
			 */

			/**
			 * 保存图标
			 */
			private void placeWorkspaceItem(Mogoo_ScreenHolder[] screens,
					ItemInfo item) {
				int screen = item.screen + 1;
				if (screen >= screens.length)
					return;

				Mogoo_ScreenHolder holder = screens[screen];

				holder.items.add(item);
			}

			/**
			 * @author 曾少彬 2011-4-21 去除重复的项
			 * @param screens
			 */
			private void removeDuplicatedItems(Mogoo_ScreenHolder[] screens) {
				Set set = new HashSet();

				// 循环所有屏幕, 第一屏为工具栏
				for (Mogoo_ScreenHolder screen : screens) {
					// 循环所有项
					for (ItemInfo item : screen.items) {
						if (!(item instanceof ShortcutInfo)) {
							continue;
						}

						ShortcutInfo info = (ShortcutInfo) item;
						if (info == null || info.intent == null) {
							continue;
						}

						String intentString = info.intent.toString();
						boolean success = set.add(intentString);

						// 如果没有添加成功， 说明已经有同一个Intent的图标在队列中
						if (!success) {
							// 从队列中移除
							screen.items.remove(item);

							// 从数据库中移除
							deleteItemFromDatabase(mApp, item);
						}
					}
				}
			}

			/**
			 * 重新排列图标
			 * 
			 * @param screens
			 */
			private void reOrderItems(Mogoo_ScreenHolder[] screens) {
				Mogoo_DataWashingController.getInstance().washData(screens,
						mApp);

				for (int i = 0; i < screens.length; i++) {
					// toolbar
					if (i == 0) {
						mToolbarItems.addAll(screens[i].items);
					} else {
						mItems.addAll(screens[i].items);
					}
				}
			}

			/**
			 * 从DB中加载桌面图标信息到内存中（存在mItems,mAppWidgets,mtFolders,mToolbarItems列表中）
			 */
			private void loadWorkspace() {

				long begin = new Date().getTime();

				if (Mogoo_GlobalConfig.LOG_DEBUG) {
					Log.d(TAG,
							"---------------------------loadWorkspace: start load data-------------------------"
									+ new Date().toString());
				}
				
				mItems.clear();
				mAppWidgets.clear();
				// 加载前清空文件夹列表
				mtFolders.clear();
				mToolbarItems.clear();
				packageNameList.clear();

				// 在第一次启动时将应用列表中没有加入到桌面数据库的加入到桌面数据库
				if (Mogoo_GlobalConfig.LOG_DEBUG) {
					Log.d(TAG, "--------isInit:" + Launcher.isInit);
				}

				if (Launcher.isInit) {

					addInDbForNotAddToDesk();
				}
				// end

				final long t = DEBUG_LOADERS ? SystemClock.uptimeMillis() : 0;

				final Context context = mContext;
				final ContentResolver contentResolver = context
						.getContentResolver();
				final PackageManager manager = context.getPackageManager();
				final AppWidgetManager widgets = AppWidgetManager
						.getInstance(context);
				final boolean isSafeMode = manager.isSafeMode();

				final ArrayList<Long> itemsToRemove = new ArrayList<Long>();

				// 按容器升序、屏幕序号升序、行升序、列升序的方式将查找出来的桌面元素进行排序
				String orderBy = "container asc , screen asc , cellY asc , cellX asc ";

				final Cursor c = contentResolver.query(
						LauncherSettings.Favorites.CONTENT_URI, null, null,
						null, orderBy);

				// 为了适应横屏行列数不相同的情况
				int numberScreens = Mogoo_GlobalConfig
						.getWorkspaceScreenCount();
				int numberCellsX = Mogoo_GlobalConfig
						.getWorkspaceLongAxisCells(false);
				int numberCellsY = Mogoo_GlobalConfig
						.getWorkspaceShortAxisCells(false);

				if (Mogoo_GlobalConfig.isLandscape()) {
					numberCellsX = Mogoo_GlobalConfig
							.getWorkspaceLongAxisCells(true);
					numberCellsY = Mogoo_GlobalConfig
							.getWorkspaceShortAxisCells(true);
				}

				// final ItemInfo occupied[][][] = new
				// ItemInfo[Launcher.SCREEN_COUNT][Launcher.NUMBER_CELLS_X][Launcher.NUMBER_CELLS_Y];
				// final ItemInfo occupied[][][] = new
				// ItemInfo[numberScreens][numberCellsX][numberCellsY];
				// end

				Mogoo_ScreenHolder[] screens = new Mogoo_ScreenHolder[numberScreens + 1];
				for (int index = 0; index < screens.length; index++) {
					screens[index] = new Mogoo_ScreenHolder();
				}

				try {
					final int idIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
					final int intentIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
					final int titleIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE);
					final int iconTypeIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_TYPE);
					final int iconIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON);
					final int iconPackageIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_PACKAGE);
					final int iconResourceIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_RESOURCE);
					final int containerIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.CONTAINER);
					final int itemTypeIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.ITEM_TYPE);
					final int appWidgetIdIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.APPWIDGET_ID);
					final int screenIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.SCREEN);
					final int cellXIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
					final int cellYIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
					final int spanXIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
					final int spanYIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);
					final int uriIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.URI);
					final int displayModeIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.DISPLAY_MODE);
					// add by 张永辉 2011-1-21
					final int isSystemIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.IS_SYSTEM);
					final int appTypeIndex = c
							.getColumnIndexOrThrow(LauncherSettings.Favorites.APP_TYPE);
					// end

					ShortcutInfo info;
					String intentDescription;
					LauncherAppWidgetInfo appWidgetInfo;
					int container;
					long id;
					Intent intent;

					while (!mStopped && c.moveToNext()) {
						try {
							int itemType = c.getInt(itemTypeIndex);

							switch (itemType) {
							// 应用类型或快捷方式类型
							case LauncherSettings.Favorites.ITEM_TYPE_APPLICATION:
							case LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT:
								intentDescription = c.getString(intentIndex);
								try {
									intent = Intent.parseUri(intentDescription,
											0);
								} catch (URISyntaxException e) {
									continue;
								}
								
								/**
								 * Updated by 曾少彬 Reason:
								 * 统一APPLICATION和SHORTCUT取图标的方式
								 */

								info = getShortcutInfo(manager, intent,
										context, c, iconIndex, titleIndex);

								/*
								 * if (itemType ==
								 * LauncherSettings.Favorites.ITEM_TYPE_APPLICATION
								 * ) { info = getShortcutInfo(manager, intent,
								 * context, c, iconIndex, titleIndex); } else {
								 * info = getShortcutInfo(manager, intent,c,
								 * context, iconTypeIndex, iconPackageIndex,
								 * iconResourceIndex, iconIndex, titleIndex); }
								 */

								// End update

								if (info != null) {
									updateSavedIcon(context, info, c, iconIndex);

									info.intent = intent;
									info.id = c.getLong(idIndex);
									container = c.getInt(containerIndex);
									info.container = container;
									info.screen = c.getInt(screenIndex);
									info.cellX = c.getInt(cellXIndex);
									info.cellY = c.getInt(cellYIndex);
									
//									if(intent.getComponent() != null && !packageNameList.contains(intent.getComponent().getClassName())){
//										contentResolver.delete(
//												LauncherSettings.Favorites
//														.getContentUri(info.id, false), null, null);
//										continue;
//									}

									/**
									 * add by 张永辉 2010-12-10
									 */
									if (Mogoo_GlobalConfig.LOG_DEBUG) {
										Log.d(TAG, "Screen:" + info.screen
												+ " CELLX:" + info.cellX
												+ " CELLY:" + info.cellY);
										Log.d(TAG, "INTENT:"
												+ intentDescription + " Title:"
												+ info.title);
									}

									info.isSystem = c.getInt(isSystemIndex);
									info.appType = c.getInt(appTypeIndex);

									// 横屏时调整程序图标的位置
									if (Mogoo_GlobalConfig.isLandscape()
											&& info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP) {
										int cellXTemp = info.cellX;
										int cellYTemp = info.cellY;
										int cellYNew = cellYTemp
												/ Mogoo_GlobalConfig
														.getWorkspaceShortAxisCellsLand();
										int cellXNew = ((cellYTemp)
												* Mogoo_GlobalConfig
														.getWorkspaceLongAxisCellsPort() + cellXTemp)
												% numberCellsX;
										info.cellX = cellXNew;
										info.cellY = cellYNew;
										if (Mogoo_GlobalConfig.LOG_DEBUG) {
											Log.d(TAG, "Screen:" + info.screen
													+ " NewCELLX:" + info.cellX
													+ " NewCELLY:" + info.cellY);
										}
									}

									/**
									 * end add by 张永辉 2010-12-10
									 */

									/**
									 * Added by 曾少彬 2011-4-20
									 */
									if (info.container == LauncherSettings.Favorites.CONTAINER_DESKTOP
											|| info.container == LauncherSettings.Favorites.CONTAINER_TOOLBAR) {
										placeWorkspaceItem(screens, info);
									}
									/**
									 * Ended
									 */

									switch (container) {
									case LauncherSettings.Favorites.CONTAINER_DESKTOP:
										// Added by 曾少彬 2011-4-20
										// 桌面图标需处理过后再加入到mItems中
										// mItems.add(info);
										// Ended
										break;
									// -------------add by
									// 魏景春---------------
									case LauncherSettings.Favorites.CONTAINER_TOOLBAR:
										// Added by 曾少彬 2011-4-20
										// 工具栏图标需处理过后再加入到mToolbarItems中
										// mToolbarItems.add(info);
										// Ended
										break;
									// ---------------end-----------------
									default:
										Mogoo_FolderInfo mtFoldInfo = findOrMakeMtFolder(
												mtFolders, container);
										mtFoldInfo.add(info);
										break;
									}
								} else {
									// Failed to load the shortcut, probably
									// because the
									// activity manager couldn't resolve it
									// (maybe the app
									// was uninstalled), or the db row was
									// somehow screwed up.
									// Delete it.
									id = c.getLong(idIndex);
									Log.e(TAG, "Error loading shortcut " + id
											+ ", removing it");
									contentResolver.delete(
											LauncherSettings.Favorites
													.getContentUri(id, false),
											null, null);
								}
								break;
							// 图标文件夹类型
							case LauncherSettings.Favorites.ITEM_TYPE_MOGOO_FOLDER:
								id = c.getLong(idIndex);

								Mogoo_FolderInfo mtFolderInfo = findOrMakeMtFolder(
										mtFolders, id);

								mtFolderInfo.setTitle(c.getString(titleIndex));

								if (Mogoo_GlobalConfig.LOG_DEBUG) {
									Log.d(TAG,
											"Folder Title:"
													+ mtFolderInfo.getTitle());
								}

								mtFolderInfo.id = id;
								mtFolderInfo.appType = LauncherSettings.Favorites.APP_TYPE_OTHER;
								mtFolderInfo.cellX = c.getInt(cellXIndex);
								mtFolderInfo.cellY = c.getInt(cellYIndex);
								container = c.getInt(containerIndex);
								mtFolderInfo.container = container;

								intentDescription = c.getString(intentIndex);
								intent = null;
								if (intentDescription != null) {
									try {
										intent = Intent.parseUri(
												intentDescription, 0);
									} catch (URISyntaxException e) {

									}
								}

								// 如果DB中没有存储文件夹的intent，则手动生成
								if (intent == null) {
									intent = Mogoo_Utilities
											.generateMtFolderIntent(id);
								}

								mtFolderInfo.intent = intent;

								mtFolderInfo.isSystem = c.getInt(isSystemIndex);
								mtFolderInfo.itemType = LauncherSettings.Favorites.ITEM_TYPE_MOGOO_FOLDER;
								mtFolderInfo.screen = c.getInt(screenIndex);
								mtFolderInfo.spanX = c.getInt(spanXIndex);
								mtFolderInfo.spanY = c.getInt(spanYIndex);
								mtFolderInfo.setOpened(false);

								placeWorkspaceItem(screens, mtFolderInfo);

								// Added by 曾少彬 2011-4-20
								// 暂时不保存到mItems和mToolbarItems中
								/*
								 * switch (container) { case
								 * LauncherSettings.Favorites.CONTAINER_DESKTOP:
								 * mItems.add(mtFolderInfo); break; case
								 * LauncherSettings.Favorites.CONTAINER_TOOLBAR:
								 * mToolbarItems.add(mtFolderInfo) ; }
								 */
								// Ended

								mtFolders.put(mtFolderInfo.id, mtFolderInfo);

								break;

							// widget类型
							case LauncherSettings.Favorites.ITEM_TYPE_APPWIDGET:
								// Read all Launcher-specific widget details
								int appWidgetId = c.getInt(appWidgetIdIndex);
								id = c.getLong(idIndex);

								final AppWidgetProviderInfo provider = widgets
										.getAppWidgetInfo(appWidgetId);

								if (!isSafeMode
										&& (provider == null
												|| provider.provider == null || provider.provider
												.getPackageName() == null)) {
									Log.e(TAG,
											"Deleting widget that isn't installed anymore: id="
													+ id + " appWidgetId="
													+ appWidgetId);
									itemsToRemove.add(id);
								} else {
									appWidgetInfo = new LauncherAppWidgetInfo(
											appWidgetId);
									appWidgetInfo.id = id;
									appWidgetInfo.screen = c
											.getInt(screenIndex);
									appWidgetInfo.cellX = c.getInt(cellXIndex);
									appWidgetInfo.cellY = c.getInt(cellYIndex);
									appWidgetInfo.spanX = c.getInt(spanXIndex);
									appWidgetInfo.spanY = c.getInt(spanYIndex);

									// add by 张永辉 2010-12-14 横竖屏widget屏转换
									if (Mogoo_GlobalConfig.isLandscape()) {
										int tempCellY = appWidgetInfo.cellY;
										int tempSpanY = appWidgetInfo.spanY;
										switch (tempCellY) {
										case 0:
											switch (tempSpanY) {
											case 3:
											case 4:
												appWidgetInfo.spanY = 2;
												break;
											}
										case 1:
											switch (tempSpanY) {
											case 2:
											case 3:
												appWidgetInfo.spanY = 1;
												break;
											}
											break;
										case 2:
										case 3:
											appWidgetInfo.cellX = 4 + appWidgetInfo.cellX;
											appWidgetInfo.cellY = appWidgetInfo.cellY - 2;
											break;

										}
									}
									if (Mogoo_GlobalConfig.LOG_DEBUG) {
										Log.d(TAG, "Widget:CellX:"
												+ appWidgetInfo.cellX
												+ " CellY:"
												+ appWidgetInfo.cellY
												+ " SpanX:"
												+ appWidgetInfo.spanX
												+ " SpanY:"
												+ appWidgetInfo.spanY);
									}
									// end add

									container = c.getInt(containerIndex);
									if (container != LauncherSettings.Favorites.CONTAINER_DESKTOP) {
										Log.e(TAG,
												"Widget found where container "
														+ "!= CONTAINER_DESKTOP -- ignoring!");
										continue;
									}
									appWidgetInfo.container = c
											.getInt(containerIndex);

									placeWorkspaceItem(screens, appWidgetInfo);

									mAppWidgets.add(appWidgetInfo);
								}
								break;
							}
						} catch (Exception e) {
							Log.w(TAG, "Desktop items loading interrupted:", e);
						}
					}

					// Updated by 曾少彬

					// Commented out
					// Reason: reOrderItems 会完成这些工作
					// 整理桌面图标
					/*
					 * if (Launcher.isInit) { Launcher.isInit = false;
					 * LauncherModel.this.trimShortCut(occupied); }
					 */

					// Added by 曾少彬

					// 去除重复的图标
					removeDuplicatedItems(screens);

					// 重新安置图标
					reOrderItems(screens);
					// End Add

					// End Update
				} finally {
					c.close();
				}

				if (itemsToRemove.size() > 0) {
					ContentProviderClient client = contentResolver
							.acquireContentProviderClient(LauncherSettings.Favorites.CONTENT_URI);
					// Remove dead items
					for (long id : itemsToRemove) {
						if (DEBUG_LOADERS) {
							Log.d(TAG, "Removed id = " + id);
						}
						// Don't notify content observers
						try {
							client.delete(LauncherSettings.Favorites
									.getContentUri(id, false), null, null);
						} catch (RemoteException e) {
							Log.w(TAG, "Could not remove id = " + id);
						}
					}
				}

				// Updated by 曾少彬 2011-4-20

				// Commented out
				// 查看加载时间和桌面图标分布情况
				/*
				 * if (DEBUG_LOADERS) { Log.d(TAG, "loaded workspace in " +
				 * (SystemClock.uptimeMillis() - t) + "ms"); Log.d(TAG,
				 * "workspace layout: "); for (int y = 0; y <
				 * MT_GlobalConfig.getWorkspaceLongAxisCells
				 * (MT_GlobalConfig.isLandscape()); y++) { String line = ""; int
				 * [] shortcutSreen = MT_GlobalConfig.getShortcutScreen() ; //
				 * for (int s = 0; s < Launcher.SCREEN_COUNT; s++) { for(int
				 * s:shortcutSreen){ if (s > 0) { line += " | "; } for (int x =
				 * 0; x <
				 * MT_GlobalConfig.getWorkspaceShortAxisCells(MT_GlobalConfig
				 * .isLandscape()); x++) { line += ((occupied[s][x][y] != null)
				 * ? "#" : "."); } } Log.d(TAG, "[ " + line + " ]"); } }
				 * 
				 * if (MT_GlobalConfig.LOG_DEBUG) { Log.d(TAG,
				 * "loadWorkspace():mToolbarItems.size = " +
				 * mToolbarItems.size()); Log.d(TAG,
				 * "-----------------------------loadWorkspace: load data end--------------------------------------------"
				 * + new Date().toString()); }
				 */

				if (Mogoo_GlobalConfig.LOG_DEBUG) {
					Log.d(TAG, "loadWorkspace():mToolbarItems.size = "
							+ mToolbarItems.size());
					Log.d(TAG,
							"-----------------------------loadWorkspace: load data end-----time cost:"
									+ (new Date().getTime() - begin));
				}

				// Ended
			}

			/**
			 * Read everything out of our database.
			 */
			private void bindWorkspace() {

				final long t = SystemClock.uptimeMillis();

				// Don't use these two variables in any of the callback
				// runnables.
				// Otherwise we hold a reference to them.
				final Callbacks oldCallbacks = mCallbacks.get();
				if (oldCallbacks == null) {
					// This launcher has exited and nobody bothered to tell us.
					// Just bail.
					Log.w(TAG, "LoaderThread running with no launcher");
					return;
				}

				int N;
				// Tell the workspace that we're about to start firing items at
				// it
				mHandler.post(new Runnable() {
					public void run() {
						Callbacks callbacks = tryGetCallbacks(oldCallbacks);
						if (callbacks != null) {
							callbacks.startBinding();
						}
					}
				});
				// Add the items to the workspace.
				N = mItems.size();
				if (Mogoo_GlobalConfig.LOG_DEBUG) {
					Log.d(TAG, "-------------------mItems=" + mItems.size()
							+ "------------------");
				}
				for (int i = 0; i < N; i += ITEMS_CHUNK) {
					final int start = i;
					final int chunkSize = (i + ITEMS_CHUNK <= N) ? ITEMS_CHUNK
							: (N - i);
					mHandler.post(new Runnable() {
						public void run() {
							Callbacks callbacks = tryGetCallbacks(oldCallbacks);
							if (callbacks != null) {
								callbacks.bindItems(mItems, start, start
										+ chunkSize);
							}
						}
					});
				}

				// ---------------add by 魏景春------------------
				// Add the items to the toolbar
				if (mToolbarItems.size() > 0) {
					mHandler.post(new Runnable() {
						public void run() {
							Callbacks callbacks = tryGetCallbacks(oldCallbacks);
							if (callbacks != null) {
								callbacks.bindToolbarItems(mToolbarItems);
							}
						}
					});
				}
				// ---------------end----------------

				// add by 张永辉
				// 绑定图标文件夹
				if (mtFolders.size() > 0) {
					mHandler.post(new Runnable() {
						public void run() {
							Callbacks callbacks = tryGetCallbacks(oldCallbacks);
							if (callbacks != null) {
								callbacks.bindMtFolders(mtFolders);
							}
						}
					});
				}
				// end

				// Wait until the queue goes empty.
				mHandler.post(new Runnable() {
					public void run() {
						if (DEBUG_LOADERS) {
							Log.d(TAG, "Going to start binding widgets soon.");
						}
					}
				});
				// Bind the widgets, one at a time.
				// WARNING: this is calling into the workspace from the
				// background thread,
				// but since getCurrentScreen() just returns the int, we should
				// be okay. This
				// is just a hint for the order, and if it's wrong, we'll be
				// okay.
				// TODO: instead, we should have that push the current screen
				// into here.
				final int currentScreen = oldCallbacks
						.getCurrentWorkspaceScreen();
				N = mAppWidgets.size();
				// once for the current screen
				// 绑定当前屏
				for (int i = 0; i < N; i++) {
					final LauncherAppWidgetInfo widget = mAppWidgets.get(i);
					if (widget.screen == currentScreen) {
						mHandler.post(new Runnable() {
							public void run() {
								Callbacks callbacks = tryGetCallbacks(oldCallbacks);
								if (callbacks != null) {
									callbacks.bindAppWidget(widget);
								}
							}
						});
					}
				}
				// once for the other screens
				// 绑定其它屏
				for (int i = 0; i < N; i++) {
					final LauncherAppWidgetInfo widget = mAppWidgets.get(i);
					if (widget.screen != currentScreen) {
						mHandler.post(new Runnable() {
							public void run() {
								Callbacks callbacks = tryGetCallbacks(oldCallbacks);
								if (callbacks != null) {
									callbacks.bindAppWidget(widget);
								}
							}
						});
					}
				}
				// Tell the workspace that we're done.
				mHandler.post(new Runnable() {
					public void run() {
						Callbacks callbacks = tryGetCallbacks(oldCallbacks);
						if (callbacks != null) {
							callbacks.finishBindingItems();
						}
					}
				});
				// If we're profiling, this is the last thing in the queue.
				mHandler.post(new Runnable() {
					public void run() {
						if (DEBUG_LOADERS) {
							Log.d(TAG,
									"bound workspace in "
											+ (SystemClock.uptimeMillis() - t)
											+ "ms");
						}
					}
				});
			}

			public void dumpState() {
				Log.d(TAG, "mLoader.mLoaderThread.mContext=" + mContext);
				Log.d(TAG, "mLoader.mLoaderThread.mWaitThread=" + mWaitThread);
				Log.d(TAG, "mLoader.mLoaderThread.mIsLaunching=" + mIsLaunching);
				Log.d(TAG, "mLoader.mLoaderThread.mStopped=" + mStopped);
				Log.d(TAG, "mLoader.mLoaderThread.mLoadAndBindStepFinished="
						+ mLoadAndBindStepFinished);
			}
		}

		public void dumpState() {
			Log.d(TAG, "mLoader.mItems size=" + mLoader.mItems.size());
			if (mLoaderThread != null) {
				mLoaderThread.dumpState();
			} else {
				Log.d(TAG, "mLoader.mLoaderThread=null");
			}
		}
	}

	/**
	 * This is called from the code that adds shortcuts from the intent
	 * receiver. This doesn't have a Cursor, but
	 */
	public ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent,
			Context context) {
		return getShortcutInfo(manager, intent, context, null, -1, -1);
	}

	/**
	 * Make an ShortcutInfo object for a shortcut that is an application. If c
	 * is not null, then it will be used to fill in missing data like the title
	 * and icon.
	 */
	public ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent,
			Context context, Cursor c, int iconIndex, int titleIndex) {
		Bitmap icon = null;
		final ShortcutInfo info = new ShortcutInfo();

		ComponentName componentName = intent.getComponent();
		if (componentName == null) {
			return null;
		}

		// TODO: See if the PackageManager knows about this case. If it doesn't
		// then return null & delete this.

		// the resource -- This may implicitly give us back the fallback icon,
		// but don't worry about that. All we're doing with usingFallbackIcon is
		// to avoid saving lots of copies of that in the database, and most apps
		// have icons anyway.
		final ResolveInfo resolveInfo = manager.resolveActivity(intent, 0);
		// -------------------- modify by weijingchun 2011-12-8----------------
		// if (resolveInfo != null) {
		// icon = mIconCache.getIcon(componentName, resolveInfo);
		// }

		int iconTypeIndex = c
				.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_TYPE);
		int iconType = c.getInt(iconTypeIndex);
		if (iconType == LauncherSettings.Favorites.ICON_TYPE_RESOURCE) {
			int iconPackageIndex = c
					.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_PACKAGE);
			int iconResourceIndex = c
					.getColumnIndexOrThrow(LauncherSettings.Favorites.ICON_RESOURCE);
			int titleResourceIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE_RESOURCE);
			String packageName = c.getString(iconPackageIndex);
			String resourceName = c.getString(iconResourceIndex);
	          //add by yeben 2012-3-30
	          String titleResourceName = c.getString(titleResourceIndex);
	          //end
			// the resource
			try {
				Resources resources = manager
						.getResourcesForApplication(packageName);
				if (resources != null) {
					final int id = resources.getIdentifier(resourceName, null,
							null);
					icon = Utilities.createIconBitmap(
							resources.getDrawable(id), context);
					if (icon != null) {
						Mogoo_BitmapUtils.setApplicationIcon(componentName,
								icon);
						mIconCache.remove(componentName);
//						if (c != null) {
//							info.title = c.getString(titleIndex);
//							Mogoo_BitmapUtils.setApplicationTitle(
//									componentName, info.title.toString());
//							Log.i("LauncherModel", "componentName==" + componentName.getPackageName());
//						}

					}
	                  //add by yeben 2012-3-31
	                  final int titleId = resources.getIdentifier(titleResourceName, null, null);
	                  String title = resources.getString(titleId);
	                  if(title!=null){
	                      info.title =  title;  
	                      LauncherModel.updateItemInDatabase(context, info);
	                      Mogoo_BitmapUtils.setApplicationTitle(componentName, info.title.toString());
	                      Log.i("LauncherModel", "componentName==" + componentName.getPackageName());
	                  }
	                  //end 
				}
			} catch (Exception e) {
				// drop this. we have other places to look for icons
			}
		}

		if (icon == null && resolveInfo != null) {
			icon = mIconCache.getIcon(componentName, resolveInfo);
		}

		// -------------------------end ----------------------------
		// the db
		if (icon == null) {
			if (c != null) {
				icon = getIconFromCursor(c, iconIndex);
			}
		}
		// the fallback icon
		if (icon == null) {
			icon = getFallbackIcon();
			info.usingFallbackIcon = true;
		}
		info.setIcon(icon);

		// -------------------- modify by weijingchun 2011-12-8----------------
		// // from the resource
		// if (resolveInfo != null) {
		// info.title = resolveInfo.activityInfo.loadLabel(manager);
		// }
		// // from the db
		// if (info.title == null) {
		// if (c != null) {
		// info.title = c.getString(titleIndex);
		// }
		// }
		//

		// from the resource
		if (info.title == null && resolveInfo != null) {
			info.title = resolveInfo.activityInfo.loadLabel(manager);
		}
		// from the db
		if (info.title == null) {
			if (c != null) {
				info.title = c.getString(titleIndex);
			}
		}

		// -------------------------end ----------------------------
		// fall back to the class name of the activity
		if (info.title == null) {
			info.title = componentName.getClassName();
		}
		info.itemType = LauncherSettings.Favorites.ITEM_TYPE_APPLICATION;
		return info;
	}

	/**
	 * Make an ShortcutInfo object for a shortcut that isn't an application.
	 */
	private ShortcutInfo getShortcutInfo(PackageManager manager, Intent intent,
			Cursor c, Context context, int iconTypeIndex, int iconPackageIndex,
			int iconResourceIndex, int iconIndex, int titleIndex) {

		Bitmap icon = null;
		final ShortcutInfo info = new ShortcutInfo();
		info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;

		// TODO: If there's an explicit component and we can't install that,
		// delete it.
		// add by 张永辉 快捷方式的图标标题也以系统列表中的保持一致
		info.title = getTitleByIntent(manager, intent);
		// end

		if (info.title == null) {
			info.title = c.getString(titleIndex);
		}

		int iconType = c.getInt(iconTypeIndex);
		switch (iconType) {
		case LauncherSettings.Favorites.ICON_TYPE_RESOURCE:
			String packageName = c.getString(iconPackageIndex);
			String resourceName = c.getString(iconResourceIndex);
			PackageManager packageManager = context.getPackageManager();
			info.customIcon = false;
			// the resource
			try {
				Resources resources = packageManager
						.getResourcesForApplication(packageName);
				if (resources != null) {
					final int id = resources.getIdentifier(resourceName, null,
							null);
					icon = Utilities.createIconBitmap(
							resources.getDrawable(id), context);
				}
			} catch (Exception e) {
				// drop this. we have other places to look for icons
			}
			// the db
			if (icon == null) {
				icon = getIconFromCursor(c, iconIndex);
			}
			// the fallback icon
			if (icon == null) {
				icon = getFallbackIcon();
				info.usingFallbackIcon = true;
			}
			break;
		case LauncherSettings.Favorites.ICON_TYPE_BITMAP:
			icon = getIconFromCursor(c, iconIndex);
			if (icon == null) {
				icon = getFallbackIcon();
				info.customIcon = false;
				info.usingFallbackIcon = true;
			} else {
				info.customIcon = true;
			}
			break;
		default:
			icon = getFallbackIcon();
			info.usingFallbackIcon = true;
			info.customIcon = false;
			break;
		}
		info.setIcon(icon);
		return info;
	}

	Bitmap getIconFromCursor(Cursor c, int iconIndex) {
		if (false) {
			Log.d(TAG,
					"getIconFromCursor app="
							+ c.getString(c
									.getColumnIndexOrThrow(LauncherSettings.Favorites.TITLE)));
		}
		byte[] data = c.getBlob(iconIndex);
		try {
			return BitmapFactory.decodeByteArray(data, 0, data.length);
		} catch (Exception e) {
			return null;
		}
	}

	ShortcutInfo addShortcut(Context context, Intent data,
			CellLayout.CellInfo cellInfo, boolean notify) {

		final ShortcutInfo info = infoFromShortcutIntent(context, data);
		addItemToDatabase(context, info,
				LauncherSettings.Favorites.CONTAINER_DESKTOP, cellInfo.screen,
				cellInfo.cellX, cellInfo.cellY, notify);

		return info;
	}

	private ShortcutInfo infoFromShortcutIntent(Context context, Intent data) {
		Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
		String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
		Parcelable bitmap = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);

		Bitmap icon = null;
		boolean filtered = false;
		boolean customIcon = false;
		ShortcutIconResource iconResource = null;

		if (bitmap != null && bitmap instanceof Bitmap) {
			icon = Utilities.createIconBitmap(new FastBitmapDrawable(
					(Bitmap) bitmap), context);
			filtered = true;
			customIcon = true;
		} else {
			Parcelable extra = data
					.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
			if (extra != null && extra instanceof ShortcutIconResource) {
				try {
					iconResource = (ShortcutIconResource) extra;
					final PackageManager packageManager = context
							.getPackageManager();
					Resources resources = packageManager
							.getResourcesForApplication(iconResource.packageName);
					final int id = resources.getIdentifier(
							iconResource.resourceName, null, null);
					icon = Utilities.createIconBitmap(
							resources.getDrawable(id), context);
				} catch (Exception e) {
					Log.w(TAG, "Could not load shortcut icon: " + extra);
				}
			}
		}

		final ShortcutInfo info = new ShortcutInfo();

		if (icon == null) {
			icon = getFallbackIcon();
			info.usingFallbackIcon = true;
		}
		info.setIcon(icon);

		info.title = name;
		info.intent = intent;
		info.customIcon = customIcon;
		info.iconResource = iconResource;

		return info;
	}

	private static void loadLiveFolderIcon(Context context, Cursor c,
			int iconTypeIndex, int iconPackageIndex, int iconResourceIndex,
			LiveFolderInfo liveFolderInfo) {

		int iconType = c.getInt(iconTypeIndex);
		switch (iconType) {
		case LauncherSettings.Favorites.ICON_TYPE_RESOURCE:
			String packageName = c.getString(iconPackageIndex);
			String resourceName = c.getString(iconResourceIndex);
			PackageManager packageManager = context.getPackageManager();
			try {
				Resources resources = packageManager
						.getResourcesForApplication(packageName);
				final int id = resources
						.getIdentifier(resourceName, null, null);
				liveFolderInfo.icon = Utilities.createIconBitmap(
						resources.getDrawable(id), context);
			} catch (Exception e) {
				liveFolderInfo.icon = Utilities.createIconBitmap(
						context.getResources().getDrawable(
								R.drawable.ic_launcher_folder), context);
			}
			liveFolderInfo.iconResource = new Intent.ShortcutIconResource();
			liveFolderInfo.iconResource.packageName = packageName;
			liveFolderInfo.iconResource.resourceName = resourceName;
			break;
		default:
			liveFolderInfo.icon = Utilities.createIconBitmap(context
					.getResources().getDrawable(R.drawable.ic_launcher_folder),
					context);
		}
	}

	void updateSavedIcon(Context context, ShortcutInfo info, Cursor c,
			int iconIndex) {
		// If this icon doesn't have a custom icon, check to see
		// what's stored in the DB, and if it doesn't match what
		// we're going to show, store what we are going to show back
		// into the DB. We do this so when we're loading, if the
		// package manager can't find an icon (for example because
		// the app is on SD) then we can use that instead.
		if (info.onExternalStorage && !info.customIcon
				&& !info.usingFallbackIcon) {
			boolean needSave;
			byte[] data = c.getBlob(iconIndex);
			try {
				if (data != null) {
					Bitmap saved = BitmapFactory.decodeByteArray(data, 0,
							data.length);
					Bitmap loaded = info.getIcon(mIconCache);
					needSave = !saved.sameAs(loaded);
				} else {
					needSave = true;
				}
			} catch (Exception e) {
				needSave = true;
			}
			if (needSave) {
				Log.d(TAG, "going to save icon bitmap for info=" + info);
				// This is slower than is ideal, but this only happens either
				// after the froyo OTA or when the app is updated with a new
				// icon.
				updateItemInDatabase(context, info);
			}
		}
	}

	/**
	 * Return an existing UserFolderInfo object if we have encountered this ID
	 * previously, or make a new one.
	 */
	private static UserFolderInfo findOrMakeUserFolder(
			HashMap<Long, FolderInfo> folders, long id) {
		// See if a placeholder was created for us already
		FolderInfo folderInfo = folders.get(id);
		if (folderInfo == null || !(folderInfo instanceof UserFolderInfo)) {
			// No placeholder -- create a new instance
			folderInfo = new UserFolderInfo();
			folders.put(id, folderInfo);
		}
		return (UserFolderInfo) folderInfo;
	}

	/**
	 * 如果文件夹列表中存在这个文件夹，则直接返回，否创建一个新的文件夹对象并加入列表中
	 * 
	 * @param folders
	 *            文件夹列表
	 * @param id
	 *            文件夹在DB中的ID
	 * @return
	 */
	private static Mogoo_FolderInfo findOrMakeMtFolder(
			HashMap<Long, Mogoo_FolderInfo> folders, long id) {
		Mogoo_FolderInfo info = folders.get(id);
		if (info == null || !(info instanceof Mogoo_FolderInfo)) {
			info = new Mogoo_FolderInfo();
			folders.put(id, info);
		}
		return info;
	}

	/**
	 * Return an existing UserFolderInfo object if we have encountered this ID
	 * previously, or make a new one.
	 */
	private static LiveFolderInfo findOrMakeLiveFolder(
			HashMap<Long, FolderInfo> folders, long id) {
		// See if a placeholder was created for us already
		FolderInfo folderInfo = folders.get(id);
		if (folderInfo == null || !(folderInfo instanceof LiveFolderInfo)) {
			// No placeholder -- create a new instance
			folderInfo = new LiveFolderInfo();
			folders.put(id, folderInfo);
		}
		return (LiveFolderInfo) folderInfo;
	}

	private static String getLabel(PackageManager manager,
			ActivityInfo activityInfo) {
		String label = activityInfo.loadLabel(manager).toString();
		if (label == null) {
			label = manager.getApplicationLabel(activityInfo.applicationInfo)
					.toString();
			if (label == null) {
				label = activityInfo.name;
			}
		}
		return label;
	}

	private static final Collator sCollator = Collator.getInstance();

	public static final Comparator<ApplicationInfo> APP_NAME_COMPARATOR = new Comparator<ApplicationInfo>() {
		public final int compare(ApplicationInfo a, ApplicationInfo b) {
			return sCollator.compare(a.title.toString(), b.title.toString());
		}
	};

	public void dumpState() {
		Log.d(TAG, "mBeforeFirstLoad=" + mBeforeFirstLoad);
		Log.d(TAG, "mCallbacks=" + mCallbacks);
		// ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.data",
		// mAllAppsList.data);
		// ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.added",
		// mAllAppsList.added);
		// ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.removed",
		// mAllAppsList.removed);
		// ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList.modified",
		// mAllAppsList.modified);
		mLoader.dumpState();
	}

	// --------------------motone method statement---------

	/**
	 * 将新安装的桌面快捷方式加入数据库中 @ author: 张永辉
	 * 
	 * @param context
	 *            上下文
	 * @param appInfo
	 *            要安装的应用程序信息
	 * @param cellInfo
	 *            单元格信息
	 * @param notify
	 *            是否通知内容查看器数据库有更新
	 * @return 返回新增加的快捷方式信息
	 */
	public static ShortcutInfo addShortcut(Context context,
			ApplicationInfo appInfo, CellLayout.CellInfo cellInfo,
			boolean notify) {

		Intent intent = appInfo.intent;
		String name = "";
		if (appInfo.title != null) {
			name = appInfo.title.toString();
		}
		Parcelable bitmap = appInfo.iconBitmap;

		Bitmap icon = null;
		boolean filtered = false;
		boolean customIcon = false;
		ShortcutIconResource iconResource = null;

		if (bitmap != null && bitmap instanceof Bitmap) {
			icon = Utilities.createIconBitmap(new FastBitmapDrawable(
					(Bitmap) bitmap), context);
			filtered = true;
			customIcon = true;
		} else {
			// Parcelable extra =
			// data.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
			// if (extra != null && extra instanceof ShortcutIconResource) {
			// try {
			// iconResource = (ShortcutIconResource) extra;
			// final PackageManager packageManager =
			// context.getPackageManager();
			// Resources resources = packageManager.getResourcesForApplication(
			// iconResource.packageName);
			// final int id = resources.getIdentifier(iconResource.resourceName,
			// null, null);
			// icon = Utilities.createIconBitmap(resources.getDrawable(id),
			// context);
			// } catch (Exception e) {
			// Log.w(TAG, "Could not load shortcut icon: " + extra);
			// }
			// }
		}
		final ShortcutInfo info = new ShortcutInfo();

		if (icon == null) {
			// icon =
			// Bitmap.createBitmap(Utilities.createIconBitmap(context.getPackageManager()
			// .getDefaultActivityIcon(), context));
			// add by
			ComponentName componentName = intent.getComponent();
			if (componentName == null) {
				return null;
			}
			final ResolveInfo resolveInfo = context.getPackageManager()
					.resolveActivity(intent, 0);
			if (resolveInfo != null) {
				icon = ((LauncherApplication) (context.getApplicationContext()))
						.getIconCache().getIcon(componentName, resolveInfo);
			}
			// end
			info.usingFallbackIcon = true;
		}

		info.setIcon(icon);

		info.title = name;
		info.intent = intent;
		info.customIcon = customIcon;
		info.iconResource = iconResource;

		if (Mogoo_GlobalConfig.LOG_DEBUG) {
			Log.d(TAG, "screen:" + cellInfo.screen + " cellX:" + cellInfo.cellX
					+ " cellY:" + cellInfo.cellY);
		}

		LauncherModel.addItemToDatabase(context, info,
				LauncherSettings.Favorites.CONTAINER_DESKTOP, cellInfo.screen,
				cellInfo.cellX, cellInfo.cellY, notify);

		return info;
	}

	/**
	 * 返回桌面图标信息,其中包括快捷方式对象、文件夹对象和widget对象 @ author: 张永辉
	 * 
	 * @return
	 */
	public ArrayList<ItemInfo> getDesktopItems() {
		return this.desktopItems;
	}

	/**
	 * 安装应用到桌面快捷方式 @ author: 张永辉
	 * 
	 * @param context
	 * @param info
	 *            应用信息
	 * @param screen
	 *            安装到哪屏
	 * @return 是否安装成功
	 */
	public boolean installShortcut(Context context, ApplicationInfo info,
			int screen) {

		final int[] mCoordinates;

		String name = "";
		if (info.title != null) {
			name = info.title.toString();
		}

		mCoordinates = CellLayout.findBlackCell(context, screen);

		if (mCoordinates != null) {
			CellLayout.CellInfo cell = new CellLayout.CellInfo();
			cell.cellX = mCoordinates[0];
			cell.cellY = mCoordinates[1];
			cell.screen = screen;

			if (!shortcutExists(context, name, info.intent)) {
				addShortcut(context, info, cell, true);
				Toast.makeText(context,
						context.getString(R.string.shortcut_installed, name),
						Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(context,
						context.getString(R.string.shortcut_duplicate, name),
						Toast.LENGTH_SHORT).show();
			}

			return true;
		}
		// delete by 张永辉 2011-7-29
		// 如果未删除会导致多次出现没有空间的提示
		// else {
		// Toast.makeText(context, context.getString(R.string.out_of_space),
		// Toast.LENGTH_SHORT)
		// .show();
		// }
		// end

		return false;
	}

	/**
	 * 找出action为main,category为launcher的activity
	 * 
	 * @author: 张永辉
	 * @param context
	 * @param packageName
	 *            应用包名
	 * @return
	 */
	public static List<ResolveInfo> findMainAndLauncherActivitiesForPackage(
			Context context, String packageName) {
		final PackageManager packageManager = context.getPackageManager();

		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		final List<ResolveInfo> apps = packageManager.queryIntentActivities(
				mainIntent, 0);
		final List<ResolveInfo> matches = new ArrayList<ResolveInfo>();

		if (apps != null) {
			// Find all activities that match the packageName
			int count = apps.size();
			for (int i = 0; i < count; i++) {
				final ResolveInfo info = apps.get(i);
				final ActivityInfo activityInfo = info.activityInfo;
				if (packageName.equals(activityInfo.packageName)) {
					matches.add(info);
				}
			}
		}

		return matches;
	}

	/**
	 * 是否为系统应用
	 * 
	 * @author: 张永辉
	 * @param cn
	 * @return
	 */
	public boolean isSystemApp(ComponentName cn) {
		boolean isSystem = false;
		Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setComponent(cn);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		String intentStr = intent.toUri(0);
		final ContentResolver contentResolver = mApp.getContentResolver();
		final Cursor c = contentResolver.query(
				LauncherSettings.Favorites.CONTENT_URI, null,
				"intent=? and isSystem=?", new String[] { intentStr,
						LauncherSettings.Favorites.IS_SYSTEM_APP + "" }, null);
		if (c != null) {
			if (c.getCount() > 0) {
				isSystem = true;
			}

			c.close();
		}
		return isSystem;
	}

	/**
	 * 是否该应用的快捷方式信息在数据库中有存储 @ author: 张永辉
	 * 
	 * @param cn
	 *            应用组件名
	 * @return
	 */
	private boolean isInDb(ComponentName cn) {
		boolean isInDb = false;
		Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		intent.setComponent(cn);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		String intentStr = intent.toUri(0);
		final ContentResolver contentResolver = mApp.getContentResolver();
		final Cursor c = contentResolver.query(
				LauncherSettings.Favorites.CONTENT_URI, null, "intent=? ",
				new String[] { intentStr }, null);
		if (c != null) {
			if (c.getCount() > 0) {
				isInDb = true;
			}

			c.close();
		} else {
			isInDb = false;
		}

		if (Mogoo_GlobalConfig.LOG_DEBUG) {
			Log.d(TAG, "companyName:" + cn.getPackageName() + " isInDb:"
					+ isInDb);
		}

		return isInDb;
	}

	/**
	 * 取得所有己经在桌面数据库的应用包名 @ author: 张永辉
	 * 
	 * @return
	 */
	private Set<ComponentName> getAllShortcutComponentName() {
		// modify by huangyue
		HashMap<String, ComponentName> cns = new HashMap<String, ComponentName>();
		ArrayList<Long> repeatComponentList = new ArrayList<Long>();
		// end

		final Cursor c = mApp.getContentResolver().query(
				LauncherSettings.Favorites.CONTENT_URI, null, null, null, null);
		// denglixia modify 2011.4.20 add try catch finally structure
		try {
			if (c != null) {
				final int intentIndex = c
						.getColumnIndexOrThrow(LauncherSettings.Favorites.INTENT);
				final int idIndex = c
						.getColumnIndexOrThrow(LauncherSettings.Favorites._ID);
				while (c.moveToNext()) {
					String intentDescription = c.getString(intentIndex);
					if (intentDescription != null
							&& !"".equals(intentDescription)) {
						Intent intent;
						try {
							intent = Intent.parseUri(intentDescription, 0);

							// add by huangyue 去除重复的数据
							if (cns.containsKey(intent.getComponent()
									.toString())) {
								Long id = c.getLong(intentIndex);
								if (id != null) {
									repeatComponentList.add(id);
								}
								continue;
							}

							if (intent != null) {
								cns.put(intent.getComponent().toString(),
										intent.getComponent());
							}

							// end
						} catch (URISyntaxException e) {
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (c != null) {
				c.close();
			}
		}

		deleteBatchId(repeatComponentList);

		// modify by huangyue
		Set<ComponentName> cnSet = new HashSet<ComponentName>(cns.values());
		cns.clear();
		repeatComponentList.clear();
		repeatComponentList = null;
		cns = null;
		return cnSet;
		// end
	}

	/**
	 * 删除列表中id的数据库数据 方法的功能描述 @ author: 黄悦
	 * 
	 * @param ids
	 */
	private void deleteBatchId(ArrayList<Long> ids) {
		final ContentResolver cr = mApp.getContentResolver();
		for (Long id : ids) {
			if (id == null) {
				continue;
			}
			cr.delete(LauncherSettings.Favorites.getContentUri(id, false),
					null, null);
		}
	}

	/**
	 * 取得软件列表中没有加到桌面上的应用列表
	 * 
	 * @author: 张永辉
	 * @return
	 */
	private List<ApplicationInfo> getItemsForNotAddToDesk() {
		List<ApplicationInfo> notAddToDeskList = new ArrayList<ApplicationInfo>();

		final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
		mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

		final PackageManager packageManager = mApp.getPackageManager();

		List<ResolveInfo> apps = packageManager.queryIntentActivities(
				mainIntent, 0);

		if (Mogoo_GlobalConfig.LOG_DEBUG) {
			Log.d(TAG, "apps:" + apps.size());
		}
		// denglixia add 2011.4.20
		Set<ComponentName> cns = this.getAllShortcutComponentName();
		// denglixia add end 2011.4.20
		if (apps != null) {
			for (ResolveInfo ri : apps) {
				ApplicationInfo appInfo = new ApplicationInfo(ri, mIconCache);
				// Set<ComponentName> cns = this.getAllShortcutComponentName() ;
				// //denglixia modify 2011.4.20
				if (!cns.contains(appInfo.componentName)) {
					notAddToDeskList.add(appInfo);
				}
				
				packageNameList.add(ri.activityInfo.name);
			}
		}

		return notAddToDeskList;
	}

	/**
	 * 将应用列表中没有加入到桌面数据库的应用加入到桌面数据库 @ author:张永辉
	 */
	// dendlixia modify 2011.4.20
	public/* private */void addInDbForNotAddToDesk() {
		List<ApplicationInfo> list = this.getItemsForNotAddToDesk();
		for (ApplicationInfo info : list) {
			this.addToDeskDb(info);
		}
	}

	/**
	 * 增加应用信息到桌面数据库中
	 * 
	 * @author: 张永辉 2010-12-28
	 * @param info
	 *            要加入桌面的应用信息
	 * @param context
	 * @param occupied
	 */
	private void addToDeskDb(ApplicationInfo info) {
		// 取得当前哪些为快捷方式屏幕
//		int[] shortcutScreen = Mogoo_GlobalConfig.getShortcutScreen();
		boolean sussed = false;
		int screen = 1;
		while (!sussed) {

			final int[] coordinates; // 存放找出的空位坐标

			String name = "";
			if (info.title != null) {
				name = info.title.toString();
			}

			if ((coordinates = CellLayout.findBlackCell(mApp, screen)) != null) {
				CellLayout.CellInfo cell = new CellLayout.CellInfo();
				cell.cellX = coordinates[0];
				cell.cellY = coordinates[1];
				cell.screen = screen;

				if (!LauncherModel.shortcutExists(mApp, name, info.intent)) {// 如果桌面不存在这个应用
					int cellXNew = cell.cellX;
					int cellYNew = cell.cellY;
					// Log.d(TAG, "old:"+i+","+cellXNew+","+cellYNew);
					if (Mogoo_GlobalConfig.isLandscape()) {// 如果是横屏，则加入图标时要转换一下坐标值
						int cellXTemp = cell.cellX;
						int cellYTemp = cell.cellY;
						cellYNew = cellYTemp
								/ Mogoo_GlobalConfig
										.getWorkspaceShortAxisCellsLand();
						cellXNew = ((cellYTemp)
								* Mogoo_GlobalConfig
										.getWorkspaceLongAxisCellsPort() + cellXTemp)
								% Mogoo_GlobalConfig
										.getWorkspaceLongAxisCellsLand();
						// Log.d(TAG,
						// "New:"+i+","+cellXNew+","+cellYNew);
					}
					addShortcut(mApp, info, cell, false);
					sussed = true;
				}
				break;
			}else{
				screen ++ ;
			}
		}
	}

	// Commented out by 曾少彬 2011-4-20
	/**
	 * 整理桌面快捷方式图标，防止桌面图标中间存在空位
	 * 
	 * @author: 张永辉
	 */
	/*
	 * private void trimShortCut(ItemInfo[][][] items) { // 整理workspace //
	 * 取得当前哪些为快捷方式屏幕 int[] shortcutScreen = MT_GlobalConfig.getShortcutScreen();
	 * for (int i : shortcutScreen) { // 计算第i屏有多少个图标 int totalItems = 0; for
	 * (int x = 0; x < items[i].length; x++) { for (int y = 0; y <
	 * items[i][x].length; y++) { if (items[i][x][y] != null) { totalItems++; }
	 * } }
	 * 
	 * // Log.d(TAG , //
	 * "di "+i+" SCREEN "+totalItems+" ICON .x:"+items[i].length
	 * +" y:"+items[i][0].length);
	 * 
	 * int count = 0;
	 * 
	 * // 整理第i屏 for (int x = 0; x < items[i].length; x++) { for (int y = 0; y <
	 * items[i][x].length; y++) { if (items[i][x][y] == null) { if (totalItems >
	 * count) { // 之后的图标往前移动一个位置 // Log.d(TAG,
	 * "NULL ICON :("+i+","+x+","+y+")");
	 * 
	 * for (int yTemp = y, xTemp = x + 1; xTemp < items[i].length; xTemp++) {
	 * ItemInfo itemTemp = items[i][xTemp][yTemp]; if (itemTemp != null) { if
	 * (itemTemp.cellX == 0) { itemTemp.cellX = items[i].length - 1;
	 * itemTemp.cellY = itemTemp.cellY - 1; } else { itemTemp.cellX =
	 * itemTemp.cellX - 1; } int[] point = MT_Utilities.switchPoint(new int[] {
	 * itemTemp.cellX, itemTemp.cellY }); // Log.d(TAG, //
	 * "Before TRim:"+itemTemp.cellX+" "+itemTemp.cellY); // Log.d(TAG, //
	 * "After TRim:"+point[0]+" "+point[1]); ItemInfo itemTemp2 = new
	 * ItemInfo(itemTemp); moveItemInDatabase(mApp, itemTemp2,
	 * LauncherSettings.Favorites.CONTAINER_DESKTOP, i, point[0], point[1]); } }
	 * for (int yTemp = y + 1; yTemp < items[i][x].length; yTemp++) { for (int
	 * xTemp = 0; xTemp < items[i].length; xTemp++) { ItemInfo itemTemp =
	 * items[i][xTemp][yTemp]; if (itemTemp != null) { if (itemTemp.cellX == 0)
	 * { itemTemp.cellX = items[i].length - 1; itemTemp.cellY = itemTemp.cellY -
	 * 1; } else { itemTemp.cellX = itemTemp.cellX - 1; } int[] point =
	 * MT_Utilities.switchPoint(new int[] { itemTemp.cellX, itemTemp.cellY });
	 * // Log.d(TAG, // "Before TRim2:"+xTemp+" "+yTemp); // Log.d(TAG, //
	 * "After TRim2:"+point[0]+" "+point[1]); ItemInfo itemTemp2 = new
	 * ItemInfo(itemTemp); moveItemInDatabase(mApp, itemTemp2,
	 * LauncherSettings.Favorites.CONTAINER_DESKTOP, i, point[0], point[1]); } }
	 * } } } else { count++; } } } } }
	 */

	/**
	 * 通过intent取得应用的标题
	 * 
	 * @author: 张永辉
	 * @Date：2011-4-1
	 * @param manager
	 * @param intent
	 * @return
	 */
	private String getTitleByIntent(PackageManager manager, Intent intent) {
		CharSequence title = null;
		final ResolveInfo resolveInfo = manager.resolveActivity(intent, 0);
		// from the resource
		if (resolveInfo != null) {
			title = resolveInfo.activityInfo.loadLabel(manager);
		}

		if (title != null) {
			return title.toString();
		} else {
			return null;
		}
	}

	// --------------------end----------------------
}