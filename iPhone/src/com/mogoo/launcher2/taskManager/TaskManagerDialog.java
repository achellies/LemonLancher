//package com.mogoo.launcher2.taskManager;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import com.mogoo.launcher.R;
//import com.mogoo.launcher2.FastBitmapDrawable;
//import com.mogoo.launcher2.Launcher;
//import com.mogoo.launcher2.LauncherApplication;
//import com.mogoo.launcher2.LauncherSettings;
//import com.mogoo.launcher2.Mogoo_BubbleTextView;
//import com.mogoo.launcher2.Mogoo_ContentListener;
//import com.mogoo.launcher2.Mogoo_VibrationController;
//import com.mogoo.launcher2.ShortcutInfo;
//import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
//import com.mogoo.launcher2.taskManager.entity.Mogoo_Task;
//import com.mogoo.launcher2.taskManager.ui.Mogoo_TaskBubbleText;
//import com.mogoo.launcher2.taskManager.ui.Mogoo_TaskCellLayout;
//import com.mogoo.launcher2.taskManager.ui.Mogoo_TaskMusicBar;
//import com.mogoo.launcher2.taskManager.ui.Mogoo_TaskMusicPanel;
//import com.mogoo.launcher2.taskManager.ui.Mogoo_TaskWorkspace;
//import com.mogoo.launcher2.taskManager.util.Mogoo_TaskUtil;
//import com.mogoo.launcher2.utils.Mogoo_BitmapCache;
//import com.mogoo.launcher2.utils.Mogoo_ComponentBus;
//
//import android.app.Activity;
//import android.app.ActivityManager;
//import android.app.Dialog;
//import android.app.WallpaperManager;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.graphics.Bitmap;
//import android.graphics.Canvas;
//import android.graphics.Bitmap.Config;
//import android.graphics.Paint;
//import android.graphics.drawable.Drawable;
//import android.os.Handler;
//import android.os.Message;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.WindowManager;
//import android.view.View.OnTouchListener;
//import android.view.ViewGroup;
//import android.view.ViewParent;
//import android.view.Window;
//import android.view.animation.Animation;
//import android.view.animation.TranslateAnimation;
//import android.widget.FrameLayout;
//import android.widget.ImageView;
//
//public class TaskManagerDialog extends Dialog implements View.OnClickListener,
//		View.OnLongClickListener {
//	private static final String TAG = "Launcher.TaskManagerDialog";
//
//	private Activity mActivity;
//
//	private int statusBarHeight;
//	private int screenWidth;
//	private int screenHeight;
//	// 是否已经在关闭app
//	private boolean closed = false;
//	private boolean opened = false;
//	private int[] resIds = { R.id.taskWorkspace };
//	private int[] counts = { 0, 0, 0 };
//
//	private ImageView screenImageView;
//	private Mogoo_TaskWorkspace taskWorkspace;
//
//	private ViewGroup mRootView;
//	// add by yeben 2011-9-1
//	private int startScreen;
//	private Mogoo_TaskBroadcastReceiver mTaskBroadcastReceiver;
//	//广播是否已经注测
//	private boolean isRegistered = false;
//	// end
//
//	// add by 袁业奔
//	private Mogoo_TaskMusicPanel mMusicPanel;
//    private Mogoo_TaskMusicBar mMusicBar;
//	// end
//
//	// 是否后面几屏也加载完了
//	private boolean isFinishLoad = false;
//
//	// 每屏放置任务图标数
//	private static final int SCREEN_SIZE = 4;
//	// 屏幕截图透明度
//	private static final int ALPHA = 60;
//
//	private static final int START_SCREEN = 2;
//
//	public static final String EXTRA_SCREEN_IMG = "com.motone.taskManager.screen_img";
//
//	public static final String EXTRA_CELL_WIDTH = "com.motone.taskManager.cell_width";
//
//	public static final String EXTRA_CELL_HEIGHT = "com.motone.taskManager.cell_height";
//
//	public static final String EXTRA_STATUS_BAR_HEIGHT = "com.motone.taskManager.status_bar_height";
//
//	public static final String EXTRA_ICON_WIDTH = "com.motone.taskManager.icon_width";
//
//	public static final String EXTRA_ICON_HEIGHT = "com.motone.taskManager.icon_height";
//
//	public static final String EXTRA_LAUNCHER_PACKAGE = "com.motone.taskManager.launcher_package";
//
//	public static final String EXTRA_ICON_COUNT = "com.motone.taskManager.icon_count";
//
//	private static final String SMS_CLASS_NAME = "com.android.mms.ui.ConversationList";
//
//	private static final String TELEPHONE_CLASS_NAME = "com.android.contacts.DialtactsActivity";
//
//	private static final String MARKET_CLASS_NAME = "com.android.contacts.T";
//
//	// 通讯录
//	private static final String CONTACT_PACKAGE_NAME = "com.android.contacts";
//	// 电话
//	private static final String TELEPHONE_PACKAGE_NAME = "com.android.phone";
//	// 设置
//	private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
//	// 最近打开过的应用缓存
//	private static Map<String, Intent> recentTaskMapCache;
//	// 正在运行的任务缓存
//	private static List<String> runningTaskPackageCache;
//	private String launcherPackage;
//	private Mogoo_BitmapCache iconCache;
//	private Mogoo_VibrationController mVibrationController;
//	private Bitmap delIcon;
//	private IntentFilter musicFilter;
//	private WallpaperManager mwm;
//	private int cellHeight;
//	private int cellWidth;
//	// 存放背景图片
//	private static Drawable taskListGalleryBg = null;
//	// 抖动标志
//	private boolean isVibrate = false;
//	
//	private Handler loadHandler = new Handler() {
//		public void handleMessage(Message msg) {
//			List<ShortcutInfo> allShortcuts = generateShortcutInfos(Integer.MAX_VALUE);
//			if (allShortcuts != null) {
//				int size = allShortcuts.size();
//				int screenSize = getScreenByIndex(size - 1) + 1;
//				for (int i = 1; i < screenSize; i++) {
//					List<Mogoo_BubbleTextView> childs = new ArrayList<Mogoo_BubbleTextView>();
//					for (int index = i * SCREEN_SIZE; index < size; index++) {
//						Mogoo_BubbleTextView shortcut = (Mogoo_BubbleTextView) createShortcut(
//								R.layout.mogoo_application_task,
//								(Mogoo_TaskCellLayout) (taskWorkspace
//										.getChildAt(allShortcuts.get(index).screen)),
//								allShortcuts.get(index), true);
//						childs.add(shortcut);
//						if (childs.size() >= SCREEN_SIZE) {
//							break;
//						}
//					}
//					// update by yeben 2011-9-1
//					// addScreen(i,childs) ;
//					addScreen(i + startScreen, childs);
//					// end
//				}
//			}
//			// 加载完
//			isFinishLoad = true;
//		};
//	};
//	
//	public Mogoo_VibrationController getVibrationController() {
//		return mVibrationController;
//
//	}
//
//	private Handler initHandler = new Handler() {
//		public void handleMessage(Message msg) {
//			Bitmap bitmap = drawBackground();
//
//			screenImageView.setImageBitmap(bitmap);
//			// 起始屏索引，第0屏为播放器面板
//			startScreen = START_SCREEN;
//
//			// end
//
//			List<ShortcutInfo> shortcuts = generateShortcutInfos(SCREEN_SIZE);
//
//			if (shortcuts != null) {
//				int size = shortcuts.size();
//				int screenSize = getScreenByIndex(size - 1) + 1;
//				// Log.d(TAG,
//				// "-------------size="+size+" screenSize="+screenSize) ;
//				for (int i = 0; i < screenSize; i++) {
//					List<Mogoo_BubbleTextView> childs = new ArrayList<Mogoo_BubbleTextView>();
//					for (int index = i * SCREEN_SIZE; index < size; index++) {
//						Mogoo_BubbleTextView shortcut = (Mogoo_BubbleTextView) createShortcut(
//								R.layout.mogoo_application_task,
//								(Mogoo_TaskCellLayout) (taskWorkspace
//										.getChildAt(shortcuts.get(index).screen)),
//								shortcuts.get(index), true);
//						childs.add(shortcut);
//						if (childs.size() >= SCREEN_SIZE) {
//							break;
//						}
//					}
//					// update by yeben 第0屏为播放器面板,需从第startScreen屏开始
//					// addScreen(i,childs) ;
//					addScreen(i + startScreen, childs);
//					// end
//				}
//			}
//			new Handler().post(new Runnable() {
//				public void run() {
//					// 播放动画
//					playStartAnimation();
//				}
//			});
//			// add by yeben
//			setcurrentScreen();
//			// end
//			// 加载后面几屏
//			if (shortcuts != null && shortcuts.size() >= SCREEN_SIZE) {
//				new Thread() {
//					public void run() {
//						loadHandler.sendEmptyMessage(0);
//					}
//				}.start();
//			} else {
//				isFinishLoad = true;
//			}
//		}
//
//		private Bitmap drawBackground() {
//			Drawable tempDrawable = mwm.getDrawable();
//			tempDrawable.setBounds(0, 0, tempDrawable.getIntrinsicWidth(), tempDrawable.getIntrinsicHeight());
//			
//			Bitmap bitmap = Bitmap.createBitmap(mRootView.getWidth(),
//					mRootView.getHeight(), Config.ARGB_8888);
//			Bitmap temp = bitmap.copy(Config.ARGB_8888, true);
//			Paint paint = new Paint();
//			paint.setAlpha(ALPHA);
//			paint.setAntiAlias(true);
//			
//			Canvas canvasTemp = new Canvas(temp);
//			
//			mRootView.draw(canvasTemp);
//			canvasTemp.save();
//			
//			Canvas canvas = new Canvas(bitmap);
//			tempDrawable.draw(canvas);
//			canvas.drawBitmap(temp, 0, 0, paint);
//			canvas.save();
//
//			temp.recycle();
//			return bitmap;
//		};
//	};
//
//	public TaskManagerDialog(Activity context) {
//		super(context);
//		mActivity = context;
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
//
//		initDialog();
//	}
//
//	/**
//	 * 根据所在序号取得所在屏号
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-6-16
//	 * @param index
//	 * @return
//	 */
//	private int getScreenByIndex(int index) {
//		int screen = index / SCREEN_SIZE;
//		// Log.d(TAG, "index="+index+" screen="+screen) ;
//		return screen;
//	}
//	
//	/**
//	 * 创建任务栏上的图标视图
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-6-16
//	 * @param layoutResId
//	 * @param parent
//	 * @param info
//	 * @param getFromCache
//	 * @return
//	 */
//	private View createShortcut(int layoutResId, ViewGroup parent,
//			ShortcutInfo info, boolean getFromCache) {
//		final ShortcutInfo infoTemp = info;
//		LayoutInflater inflater = LayoutInflater.from(mActivity);
//		Mogoo_BubbleTextView favorite = (Mogoo_BubbleTextView) inflater
//				.inflate(layoutResId, parent, false);
//		favorite.setCompoundDrawablesWithIntrinsicBounds(
//				null,
//				new FastBitmapDrawable(getFromCache ? iconCache
//						.getIcon(info.intent) : info.getIcon(iconCache)), null,
//				null);
//		favorite.setText(info.title);
//		favorite.setTag(info);
//		favorite.setOnClickListener(this);
//		favorite.setOnLongClickListener(this);
//		favorite.setDelIcon(delIcon);
//
//		// 注册删除事件
//		((Mogoo_TaskBubbleText) favorite)
//				.setOnDelListener(new Mogoo_TaskBubbleText.OnDelListener() {
//
//					public void onDel(ComponentName cn) {
////						Mogoo_TaskUtil.killTask(mActivity, cn);
//						// add by 袁业奔 2011-10-25 如果杀掉的进程是音乐播放器进程
//						if (cn.getPackageName().equals("com.android.music")) {
//							setPlayState("pause");
//							displayMusicName("");
//						}
//						// end
//						Mogoo_TaskCellLayout taskCellLayout = (Mogoo_TaskCellLayout) taskWorkspace
//								.getChildAt(infoTemp.screen);
//						// 如果只有一个图标且处于最后一屏，则删除后减屏
//						if (taskWorkspace.indexOfChild(taskCellLayout) == taskWorkspace
//								.getChildCount() - 1
//								&& taskCellLayout.getChildCount() <= 1) {
//							taskCellLayout.removeAllViews();
//							// update by yeben 2011-9-1
//							// if(infoTemp.screen>0){
//							// taskWorkspace.removeViewAt(infoTemp.screen) ;
//							// taskWorkspace.scrollLeft() ;
//							// }
//							if (infoTemp.screen > startScreen) {
//								taskWorkspace.removeViewAt(infoTemp.screen);
//								taskWorkspace.scrollLeft();
//							}
//							// end
//						} else {// 删除后，如果后面有图标则向前移
//							moveChildLeft(infoTemp.screen, infoTemp.cellX);
//						}
//
//						iconCache
//								.recycle(
//										cn,
//										Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_VIBRATION);
//						try{
//				            ActivityManager am = (ActivityManager)mActivity.getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE) ;
////				            am.killBackgroundProcesses(componentName.getPackageName()) ;
//				    //        am.restartPackage(componentName.gtetPackageName()) ;
//				            //android.os.Process.killProcess(android.os.Process.myPid()); 
//				            am.forceStopPackage(cn.getPackageName()) ;            
//				            
//				        }catch (Exception e) {
//				            e.printStackTrace();
//				        }
//					}
//				});
//
//		if (checkType(SMS_CLASS_NAME, info)) {
//			favorite.setCountIcon(iconCache,
//					counts[Mogoo_ContentListener.SMS_INDEX], info.appType);
//		} else if (checkType(TELEPHONE_CLASS_NAME, info)) {
//			favorite.setCountIcon(iconCache,
//					counts[Mogoo_ContentListener.TELEPHONE_INDEX], info.appType);
//		} else if (checkType(MARKET_CLASS_NAME, info)) {
//			favorite.setCountIcon(iconCache,
//					counts[Mogoo_ContentListener.MARKET_INDEX], info.appType);
//		}
//
//		return favorite;
//	}
//
//	private boolean checkType(String type, ShortcutInfo info) {
//		if (info.getIntent() != null && info.getIntent().getComponent() != null) {
//			return type.equals(info.getIntent().getComponent().getClassName());
//		}
//
//		return false;
//	}
//
//	/**
//	 * index之后的所有图标向前移一个单元格
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-6-16
//	 * @param index
//	 */
//	private void moveChildLeft(int screen, int index) {
//		Mogoo_TaskCellLayout currentCellLayout = (Mogoo_TaskCellLayout) (taskWorkspace
//				.getChildAt(screen));
//		currentCellLayout.removeViewAt(index);
//
//		for (int i = screen + 1; i < taskWorkspace.getChildCount(); i++) {
//			Mogoo_TaskCellLayout taskCellLayout = (Mogoo_TaskCellLayout) (taskWorkspace
//					.getChildAt(i));
//			Mogoo_TaskCellLayout lastTaskCellLayout = (Mogoo_TaskCellLayout) (taskWorkspace
//					.getChildAt(i - 1));
//			View child = taskCellLayout.getChildAt(0);
//			// ((MT_BubbleTextView)child).stopVibrate();
//			if (child != null) {
//				taskCellLayout.removeViewAt(0);
//				lastTaskCellLayout.addView(child, SCREEN_SIZE - 1);
//				((Mogoo_BubbleTextView) child)
//						.startVibrate(iconCache, 0, false);
//				if (taskCellLayout.getChildCount() <= 0) {
//					taskWorkspace.removeView(taskCellLayout);
//				}
//			}
//		}
//
//		// taskWorkspace.invalidate() ;
//		// taskWorkspace.requestLayout() ;
//		taskWorkspace.reLayoutAllCellLayout();
//		resetAllChildTag();
//	}
//
//	/**
//	 * 重置所有图标的TAG
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-6-17
//	 */
//	private void resetAllChildTag() {
//		int screenCount = taskWorkspace.getChildCount();
//		// update by yeben 2011-9-1
//		// for(int i=0;i<screenCount;i++)
//		for (int i = startScreen; i < screenCount; i++) {
//			// end
//			Mogoo_TaskCellLayout taskCellLayout = (Mogoo_TaskCellLayout) (taskWorkspace
//					.getChildAt(i));
//			int childCount = taskCellLayout.getChildCount();
//			for (int j = 0; j < childCount; j++) {
//				View child = taskCellLayout.getChildAt(j);
//				ShortcutInfo info = (ShortcutInfo) (child.getTag());
//				info.screen = i;
//				info.cellX = j;
//			}
//		}
//	}
//
//	private void initDialog() {
//		initParmas();
//		this.getWindow().setBackgroundDrawable(mwm.getDrawable());
//		getWindow().setContentView(R.layout.mogoo_task_manager);
//
//		initView();
//		
//		// start add by yeben 2011-9-1
//		addMusicBar();
//		addMusicPanel();
//	}
//
//	private void initView() {
//		screenImageView = (ImageView) this.findViewById(R.id.screenImage);
//		taskWorkspace = (Mogoo_TaskWorkspace) this
//				.findViewById(R.id.taskWorkspace);
//		taskWorkspace.setTaskManager(this);
//		// 设置任务栏的背景图片
//		taskWorkspace.setBackgroundDrawable(createTaskListGalleryBg());
//		taskWorkspace.setOnLongClickListener(this);
//
//		Mogoo_ComponentBus.getInstance().addActivityComp(R.id.taskWorkspace,
//				taskWorkspace, mActivity);
//
//		screenImageView.setOnTouchListener(new OnTouchListener() {
//			public boolean onTouch(View v, MotionEvent event) {
//				if (event.getY() < screenHeight - taskWorkspace.getHeight()
//						- statusBarHeight) {
//					if (closed || !opened) {
//						return true;
//					}
//					closed = true;
//					dismiss();
//					return true;
//				}
//				return false;
//			}
//		});
//	}
//
//	/**
//	 * 创建任务列表背景图
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-5-30
//	 * @return
//	 */
//	private Drawable createTaskListGalleryBg() {
//		if (taskListGalleryBg != null) {
//			return taskListGalleryBg;
//		}
//		Bitmap bg = Bitmap.createBitmap(Mogoo_TaskUtil.decodeResource(
//				mActivity.getResources(), R.drawable.mogoo_task_list_bg), 0, 0,
//				mActivity.getWindowManager().getDefaultDisplay().getWidth(),
//				cellHeight);
//		return Mogoo_TaskUtil.bitmap2Drawable(bg);
//	}
//
//	public void setRootView(ViewGroup vg) {
//		mRootView = vg;
//		statusBarHeight = screenHeight - mRootView.getHeight();
//	}
//
//	private void initParmas() {
//		// 取得图片绑存
//		iconCache = ((LauncherApplication) mActivity.getApplication())
//				.getIconCache();
//		// 生成抖动控制器
//		mVibrationController = new Mogoo_VibrationController(iconCache, resIds);
//
//		mwm = (WallpaperManager) getContext().getSystemService(
//				Context.WALLPAPER_SERVICE);
//
//		musicFilter = new IntentFilter();
//		musicFilter.addAction("com.mogoo.music.name");
//
//		// 取得Launcher包名
//		launcherPackage = mActivity.getApplicationContext().getPackageName();
//		if (launcherPackage == null || "".equals(launcherPackage)) {
//			launcherPackage = "com.mogoo.launcher";
//		}
//		// 初始化删除图标
//		delIcon = iconCache.getBitmap(R.drawable.mogoo_task_del);
//
//		// add by 袁业奔 2011-9-7
//		// 先注册广播
//		mTaskBroadcastReceiver = new Mogoo_TaskBroadcastReceiver();
//		mTaskBroadcastReceiver.setTaskManager(this);
//
//		// 音乐播放器面板
//		mMusicPanel = new Mogoo_TaskMusicPanel(mActivity);
//		mMusicBar = new Mogoo_TaskMusicBar(mActivity);
//		// end
//
//		// 取得屏幕高宽
//		screenWidth = Mogoo_GlobalConfig.getScreenWidth();
//		screenHeight = Mogoo_GlobalConfig.getScreenHeight();
//		// 单元格的长宽
//		cellWidth = Mogoo_GlobalConfig.getWorkspaceCellWidth();
//		cellHeight = Mogoo_GlobalConfig.getWorkspaceCellHeight();
//	}
//
//	/**
//	 * 播放打开动画
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-5-26
//	 */
//	private void playStartAnimation() {
//		// Log.d(TAG, "taskWorkspace.height="+taskWorkspace.getHeight()) ;
//		TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, 0,
//				-taskWorkspace.getHeight());
//		translateAnimation.setDuration(500);
//		translateAnimation.setFillAfter(true);
//		translateAnimation.setFillBefore(true);
//		translateAnimation
//				.setAnimationListener(new Animation.AnimationListener() {
//
//					public void onAnimationStart(Animation animation) {
//					}
//
//					public void onAnimationRepeat(Animation animation) {
//					}
//
//					public void onAnimationEnd(Animation animation) {
//						// screenImageView.clearAnimation();
//						opened = true;
//					}
//				});
//
//		screenImageView.startAnimation(translateAnimation);
//	}
//
//	/**
//	 * 播放关闭动画
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-6-2
//	 */
//	private void playFinishAnimation() {
//		// Log.d(TAG, "-----------playFinishAnimation()-------------");
//		TranslateAnimation translateAnimation = new TranslateAnimation(0, 0,
//				-taskWorkspace.getHeight(), 0);
//		translateAnimation.setDuration(500);
//		translateAnimation.setFillAfter(false);
//		// translateAnimation.setFillBefore(true) ;
//		translateAnimation
//				.setAnimationListener(new Animation.AnimationListener() {
//					public void onAnimationStart(Animation animation) {
//					}
//
//					public void onAnimationRepeat(Animation animation) {
//					}
//
//					public void onAnimationEnd(Animation animation) {
//						screenImageView.clearAnimation();
//						TaskManagerDialog.super.dismiss();
//						taskWorkspace.removeViews(START_SCREEN, taskWorkspace.getChildCount() - START_SCREEN);
//					}
//				});
//
//		screenImageView.startAnimation(translateAnimation);
//	}
//
//	@Override
//	public void onBackPressed() {
//		if (isVibrate) {
//			stopVibrate();
//		} else {
//			super.onBackPressed();
//		}
//	}
//
//	@Override
//	public void show() {
//		super.show();
//		closed = false;
//		isRegistered = true;
//		mActivity.registerReceiver(mTaskBroadcastReceiver, musicFilter);
//		
//		initHandler.sendEmptyMessage(0);
//	}
//
//	@Override
//	public void dismiss() {
//		playFinishAnimation();
//
//		if (recentTaskMapCache != null) {
//			recentTaskMapCache.clear();
//			recentTaskMapCache = null;
//		}
//
//		if (runningTaskPackageCache != null) {
//			runningTaskPackageCache.clear();
//			runningTaskPackageCache = null;
//		}
//		if(isRegistered){
//			mActivity.unregisterReceiver(mTaskBroadcastReceiver);
//			isRegistered = false;
//		}
//		stopVibrate();
//		iconCache.recycleAllByType(Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_VIBRATION);
//	}
//	
//
//	/**
//	 * 插入taskCellLLayout
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-6-16
//	 */
//	private void addScreen(int index, List<Mogoo_BubbleTextView> childs) {
//		taskWorkspace.addView(createScreen(childs), index);
//	}
//
//	/**
//	 * 创建taskCellLayout
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-6-16
//	 * @return
//	 */
//	private Mogoo_TaskCellLayout createScreen(List<Mogoo_BubbleTextView> childs) {
//		LayoutInflater inflater = LayoutInflater.from(mActivity);
//		Mogoo_TaskCellLayout taskCellLayout = (Mogoo_TaskCellLayout) (inflater
//				.inflate(R.layout.mogoo_task_screen, null));
//		for (Mogoo_BubbleTextView child : childs) {
//			ShortcutInfo info = (ShortcutInfo) child.getTag();
//			taskWorkspace.addInScreen(child, taskCellLayout, info.cellX,
//					info.cellY, 1, 1, false);
//		}
//		return taskCellLayout;
//	}
//
//	// add by yeben 2011-9-1
//	// 加入音乐播放器控制面板
//	public void addMusicPanel() {
//		List<Mogoo_BubbleTextView> childs0 = new ArrayList<Mogoo_BubbleTextView>();
//		addScreen(1, childs0);
//		taskWorkspace.addInScreen(mMusicPanel, 1, 0, 0, 4, 1, true);
//	}
//
//	public void setcurrentScreen() {
//		taskWorkspace.setCurrentScreen(startScreen);
//	}
//
//	public void displayMusicName(String musicName) {
//		// TODO Auto-generated method stub
//		mMusicPanel.displayMusicName(musicName);
//	}
//
//	public void setPlayState(String state) {
//		mMusicPanel.setPlayState(state);
//	}
//    
//	private void addMusicBar(){
//    	List<Mogoo_BubbleTextView> childs0 = new ArrayList<Mogoo_BubbleTextView>() ;
//    	addScreen(0,childs0) ;
//    	taskWorkspace.addInScreen(mMusicBar, 0, 0, 0, 4, 1, true);
//	}
//	// end
//
//	/**
//	 * 取得正在运行的任务包列表
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-5-26
//	 * @param context
//	 * @param maxNum
//	 * @return
//	 */
//	private List<String> getRunningTaskPackage(int maxNum) {
//		if (runningTaskPackageCache != null) {
//			return runningTaskPackageCache;
//		} else {
//			List<String> runningTaskPackage = new ArrayList<String>();
//			List<ActivityManager.RunningTaskInfo> runningTasks = Mogoo_TaskUtil
//					.getRunningTask(mActivity, maxNum);
//			if (runningTasks != null) {
//				for (ActivityManager.RunningTaskInfo runningTask : runningTasks) {
//					String packageName = runningTask.baseActivity
//							.getPackageName();
//					if (!runningTaskPackage.contains(packageName))
//						runningTaskPackage.add(packageName);
//				}
//			}
//			runningTaskPackageCache = runningTaskPackage;
//			return runningTaskPackage;
//		}
//
//	}
//
//	public void onClick(View v) {
//		if (v instanceof Mogoo_BubbleTextView && !isVibrate && isFinishLoad) {
//			ShortcutInfo info = (ShortcutInfo) ((Mogoo_BubbleTextView) v)
//					.getTag();
//			if (info.intent != null) {
//				// mActivity.startActivity(info.intent);
//				Mogoo_TaskUtil.openTask(mActivity, info.intent);
//				dismiss();
//			}
//		}
//	}
//
//	public boolean onLongClick(View v) {
//		if (isFinishLoad) {
//			startVibrate();
//		}
//		return true;
//	}
//
//	/**
//	 * 停止抖动
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-6-1
//	 */
//	public void stopVibrate() {
//		if (mVibrationController != null) {
//			mVibrationController.stopVibrate();
//			isVibrate = false;
//		}
//	}
//
//	/**
//	 * 开始抖动
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-6-1
//	 */
//	public void startVibrate() {
//		if (mVibrationController != null) {
//			mVibrationController.startVibrate(mActivity);
//			isVibrate = true;
//		}
//	}
//
//	/**
//	 * 取得最近启动过的应用
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-6-20
//	 * @param maxNum
//	 * @return
//	 */
//	private Map<String, Intent> getRecentTask(int maxNum) {
//		if (Mogoo_GlobalConfig.LOG_DEBUG) {
//			Log.d(TAG, "------------------getRecentTask()-----------start="
//					+ System.currentTimeMillis());
//		}
//
//		if (recentTaskMapCache != null) {
//			return recentTaskMapCache;
//		}
//
//		Map<String, Intent> recentTaskMap = new HashMap<String, Intent>();
//		List<ActivityManager.RecentTaskInfo> infos = Mogoo_TaskUtil
//				.getRecentTask(mActivity, maxNum);
//		if (infos != null) {
//			for (ActivityManager.RecentTaskInfo info : infos) {
//				Intent intent = new Intent(info.baseIntent);
//				// 其它桌面应用不能出现
//				if (intent.hasCategory(Intent.CATEGORY_HOME)
//						&& !launcherPackage.equals(intent.getComponent()
//								.getPackageName())) {
//					Mogoo_TaskUtil.killTask(mActivity, intent.getComponent());
//					continue;
//				}
//				if (info.origActivity != null) {
//					intent.setComponent(info.origActivity);
//					// recentTaskMap.put(info.origActivity.getPackageName(),
//					// info.origActivity) ;
//				}
//				recentTaskMap.put(intent.getComponent().getPackageName(),
//						intent);
//			}
//		}
//
//		// 过滤设置应用
//		if (recentTaskMap.containsKey(SETTINGS_PACKAGE_NAME)) {
//			recentTaskMap.put(
//					SETTINGS_PACKAGE_NAME,
//					mActivity.getPackageManager().getLaunchIntentForPackage(
//							SETTINGS_PACKAGE_NAME));
//		}
//
//		recentTaskMapCache = recentTaskMap;
//
//		if (Mogoo_GlobalConfig.LOG_DEBUG) {
//			Log.d(TAG, "------------------getRecentTask()-----------end="
//					+ System.currentTimeMillis());
//		}
//
//		return recentTaskMapCache;
//	}
//
//	/**
//	 * 取得正在运行的任务
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-5-26
//	 * @param context
//	 * @param maxNum
//	 * @return
//	 */
//	private List<Mogoo_Task> getRunningTask(int maxNum) {
//		if (Mogoo_GlobalConfig.LOG_DEBUG) {
//			Log.d(TAG, "------------------getRunningTask()-----------start="
//					+ System.currentTimeMillis());
//		}
//
//		List<Mogoo_Task> tasks = new ArrayList<Mogoo_Task>();
//
//		List<String> runningTaskPackages = getRunningTaskPackage(Integer.MAX_VALUE);
//
//		if (runningTaskPackages != null) {
//			Map<String, Intent> recentTaskMap = getRecentTask(Integer.MAX_VALUE);
//			int count = 0;
//			for (String runningTaskPackage : runningTaskPackages) {
//
//				if (count >= maxNum)
//					break;
//
//				if (runningTaskPackage.equals(launcherPackage)) {
//					continue;
//				}
//				if (recentTaskMap.containsKey(runningTaskPackage)) {
//					// 电话和通讯录
//					// if(CONTACT_PACKAGE_NAME.equals(packageName)||TELEPHONE_PACKAGE_NAME.equals(packageName)){
//					// MT_Task taskContact = new MT_Task();
//					// taskContact.setComponentName(new
//					// ComponentName(CONTACT_PACKAGE_NAME,
//					// "com.android.contacts.DialtactsContactsEntryActivity")) ;
//					// taskContact.setIcon(MT_TaskUtil.getIcon(this,
//					// taskContact.getComponentName())) ;
//					// taskContact.setTitle(MT_TaskUtil.getTitle(this,
//					// taskContact.getComponentName()));
//					// tasks.add(taskContact);
//					//
//					// MT_Task taskPhone = new MT_Task();
//					// taskPhone.setComponentName(new
//					// ComponentName(CONTACT_PACKAGE_NAME,
//					// "com.android.contacts.DialtactsActivity")) ;
//					// taskPhone.setIcon(MT_TaskUtil.getIcon(this,
//					// taskPhone.getComponentName())) ;
//					// taskPhone.setTitle(MT_TaskUtil.getTitle(this,
//					// taskPhone.getComponentName()));
//					// tasks.add(taskPhone);
//					// }else{
//					count++;
//					Mogoo_Task task = new Mogoo_Task();
//					task.setIntent(recentTaskMap.get(runningTaskPackage));
//					task.setComponentName(task.getIntent().getComponent());
//
//					if (Mogoo_GlobalConfig.LOG_DEBUG) {
//						Log.d(TAG, task.getComponentName().toString());
//					}
//
//					task.setIcon(Mogoo_TaskUtil.getIcon(mActivity,
//							task.getComponentName()));
//					task.setTitle(Mogoo_TaskUtil.getTitle(mActivity,
//							task.getComponentName()));
//					// task.setComponentName(runningTask.baseActivity);
//					// Log.d(TAG,
//					// task.getComponentName().toString() + " top:"
//					// + runningTask.topActivity.toString());
//					// task.setIcon(MT_TaskUtil.getIcon(this,
//					// task.getComponentName()));
//					// task.setTitle(MT_TaskUtil.getTitle(this,
//					// task.getComponentName()));
//					tasks.add(task);
//					// }
//				}
//			}
//		}
//
//		if (Mogoo_GlobalConfig.LOG_DEBUG) {
//			Log.d(TAG, "------------------getRunningTask()-----------end="
//					+ System.currentTimeMillis());
//		}
//
//		return tasks;
//	}
//
//	/**
//	 * 根据当前正在运行的任务生成ShortcutInfo
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-6-16
//	 * @return
//	 */
//	private List<ShortcutInfo> generateShortcutInfos(int maxNum) {
//		if (Mogoo_GlobalConfig.LOG_DEBUG) {
//			Log.d(TAG,
//					"------------------generateShortcutInfos()-----------start="
//							+ System.currentTimeMillis());
//		}
//		List<ShortcutInfo> infos = new ArrayList<ShortcutInfo>();
//		List<Mogoo_Task> tasks = getRunningTask(maxNum);
//		if (tasks != null) {
//			int index = 0;
//			for (Mogoo_Task task : tasks) {
//				ShortcutInfo info = new ShortcutInfo();
//				info.appType = LauncherSettings.Favorites.APP_TYPE_OTHER;
//				info.cellX = getCellCoordinateByIndex(index)[0];
//				info.cellY = 0;
//				info.container = 0;
//				// Intent intent = new Intent();
//				// intent.setComponent(task.getComponentName());
//				// info.intent = intent;
//				info.intent = task.getIntent();
//				info.isSystem = LauncherSettings.Favorites.NOT_SYSTEM_APP;
//				info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
//				// update by yeben 2011-9-1
//				// info.screen = getScreenByIndex(index);
//				info.screen = getScreenByIndex(index) + startScreen;
//				// end
//				info.spanX = 1;
//				info.spanY = 1;
//				info.title = task.getTitle();
//				info.setIcon(task.getIcon());
//				infos.add(info);
//				index++;
//			}
//		}
//		if (Mogoo_GlobalConfig.LOG_DEBUG) {
//			Log.d(TAG,
//					"------------------generateShortcutInfos()-----------end="
//							+ System.currentTimeMillis());
//		}
//		return infos;
//	}
//
//	/**
//	 * 根据所在序号取得所在单元格坐标
//	 * 
//	 * @author: 张永辉
//	 * @Date：2011-6-16
//	 * @param index
//	 * @return
//	 */
//	private int[] getCellCoordinateByIndex(int index) {
//		int cellX = index - getScreenByIndex(index) * SCREEN_SIZE;
//		return new int[] { cellX, 0 };
//	}
//
//	public boolean isFinishLoad() {
//		return isFinishLoad;
//	}
//
//	public void setFinishLoad(boolean isFinishLoad) {
//		this.isFinishLoad = isFinishLoad;
//	}
//}
