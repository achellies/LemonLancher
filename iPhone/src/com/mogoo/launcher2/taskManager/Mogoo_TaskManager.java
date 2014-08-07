
package com.mogoo.launcher2.taskManager;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.CellLayout;
import com.mogoo.launcher2.FastBitmapDrawable;
import com.mogoo.launcher2.ItemInfo;
import com.mogoo.launcher2.Launcher;
import com.mogoo.launcher2.LauncherApplication;
import com.mogoo.launcher2.LauncherSettings;
import com.mogoo.launcher2.Mogoo_BubbleTextView;
import com.mogoo.launcher2.Mogoo_ContentListener;
import com.mogoo.launcher2.Mogoo_VibrationController;
import com.mogoo.launcher2.ShortcutInfo;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.restore.Mogoo_RestoreController;
import com.mogoo.launcher2.restore.Mogoo_UncaughtExceptionHandler;
import com.mogoo.launcher2.taskManager.entity.Mogoo_Task;
import com.mogoo.launcher2.taskManager.ui.Mogoo_TaskBubbleText;
import com.mogoo.launcher2.taskManager.ui.Mogoo_TaskCellLayout;
import com.mogoo.launcher2.taskManager.ui.Mogoo_TaskMusicBar;
import com.mogoo.launcher2.taskManager.ui.Mogoo_TaskMusicPanel;
import com.mogoo.launcher2.taskManager.ui.Mogoo_TaskWorkspace;
import com.mogoo.launcher2.taskManager.util.Mogoo_TaskUtil;
import com.mogoo.launcher2.utils.Mogoo_BitmapCache;
import com.mogoo.launcher2.utils.Mogoo_BitmapUtils;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.Gallery.LayoutParams;
import android.widget.ImageView;
import android.widget.TextView; 

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.Inflater;

public class Mogoo_TaskManager extends Activity implements View.OnClickListener,
        View.OnLongClickListener {

    private static final String TAG = "Launcher.TaskManager";

    public static final String EXTRA_SCREEN_IMG = "com.motone.taskManager.screen_img";

    public static final String EXTRA_CELL_WIDTH = "com.motone.taskManager.cell_width";

    public static final String EXTRA_CELL_HEIGHT = "com.motone.taskManager.cell_height";

    public static final String EXTRA_STATUS_BAR_HEIGHT = "com.motone.taskManager.status_bar_height";

    public static final String EXTRA_ICON_WIDTH = "com.motone.taskManager.icon_width";

    public static final String EXTRA_ICON_HEIGHT = "com.motone.taskManager.icon_height";

    public static final String EXTRA_LAUNCHER_PACKAGE = "com.motone.taskManager.launcher_package";

    public static final String EXTRA_ICON_COUNT = "com.motone.taskManager.icon_count";
    
    private static final String SMS_CLASS_NAME = "com.android.mms.ui.ConversationList";
    
    private static final String TELEPHONE_CLASS_NAME = "com.android.contacts.DialtactsActivity";
    
    private static final String MARKET_CLASS_NAME = "com.android.contacts.T";
    
    //通讯录
    private static final String CONTACT_PACKAGE_NAME = "com.android.contacts" ;
    //电话
    private static final String TELEPHONE_PACKAGE_NAME = "com.android.phone" ;
    //设置
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings" ;
    

    // 屏幕截图透明度
    private static final int ALPHA = 60;

    // 每屏放置任务图标数
    private static final int SCREEN_SIZE = 4;

    // 存放背景图片
    private static Drawable taskListGalleryBg = null;
    
    //最近打开过的应用缓存
    private static Map<String,Intent> recentTaskMapCache;
    //正在运行的任务缓存
    private static List<String> runningTaskPackageCache ;

    // 抖动标志
    private boolean isVibrate = false;

    // 是否显示删除标志
    private boolean showDelIcon = false;

    // 是否已经在关闭app
    private boolean closed = false;

    private Bitmap screenImg;
    
    private int cellWidth;

    private int cellHeight;

    private int statusBarHeight;

    private int screenWidth;

    private int screenHeight;

    private String launcherPackage;

    private ImageView screenImageView;

    private Mogoo_TaskWorkspace taskWorkspace;

    private Mogoo_BitmapCache iconCache;

    private Mogoo_VibrationController mVibrationController;
    
    private boolean isFinished = false;
    
    //是否后面几屏也加载完了
    private boolean isFinishLoad = false ;

    private int[] resIds = {
        R.id.taskWorkspace
    };

    private Bitmap delIcon;

    private int[] counts = {
            0, 0, 0
    };
    //add by 袁业奔
    Mogoo_TaskMusicPanel mMusicPanel;
    Mogoo_TaskMusicBar mMusicBar;
    private AudioManager audioManager;
    
    //end

    //add by yeben 2011-9-1
    private int startScreen;
    private Mogoo_TaskBroadcastReceiver mTaskBroadcastReceiver;
    //end
//    private ArrayList<Bitmap> newCreatedCache = new ArrayList<Bitmap>();
    
    private ComponentName lastKilledComponentName = null;
    
    private Handler loadHandler = new Handler(){
        public void handleMessage(Message msg) {
            List<ShortcutInfo> allShortcuts = generateShortcutInfos(Integer.MAX_VALUE) ;
            if(allShortcuts!=null){
                int size = allShortcuts.size() ;
                int screenSize = getScreenByIndex(size-1)+1 ;
                for(int i=1;i<screenSize;i++){
                    List<Mogoo_BubbleTextView> childs = new ArrayList<Mogoo_BubbleTextView>() ;
                    for(int index=i*SCREEN_SIZE;index<size;index++){
                        Mogoo_BubbleTextView shortcut = (Mogoo_BubbleTextView)createShortcut(R.layout.mogoo_application_task,(Mogoo_TaskCellLayout)(taskWorkspace.getChildAt(allShortcuts.get(index).screen)),allShortcuts.get(index),true);
                        childs.add(shortcut) ;
                        if(childs.size()>=SCREEN_SIZE){
                            break ;
                        }
                    }
                    //update by yeben 2011-9-1
                    //addScreen(i,childs) ;
                    addScreen(i+startScreen,childs) ;
                    //end
                }
            }
            //加载完
            isFinishLoad = true ;
        };
    } ;
    
    private Handler initHandler = new Handler(){
        public void handleMessage(Message msg) {
        	//add by yeben 2011-10-28
        	addMusicBar();
        	//end
        	//start add by yeben 2011-9-1
        	addMusicPanel();
        	//起始屏索引，第0屏为播放器面板
        	startScreen=taskWorkspace.getChildCount();

        	//end
        	
        	List<ShortcutInfo> shortcuts = generateShortcutInfos(SCREEN_SIZE) ;
            
            if(shortcuts!=null){
                int size = shortcuts.size() ;
                int screenSize = getScreenByIndex(size-1)+1;
//                Log.d(TAG, "-------------size="+size+" screenSize="+screenSize) ;
                for(int i=0;i<screenSize;i++){
                	List<Mogoo_BubbleTextView> childs = new ArrayList<Mogoo_BubbleTextView>() ;
                    for(int index=i*SCREEN_SIZE;index<size;index++){
                        Mogoo_BubbleTextView shortcut = (Mogoo_BubbleTextView)createShortcut(R.layout.mogoo_application_task,(Mogoo_TaskCellLayout)(taskWorkspace.getChildAt(shortcuts.get(index).screen)),shortcuts.get(index),true);
                        childs.add(shortcut) ;
                        if(childs.size()>=SCREEN_SIZE){
                            break ;
                        }
                    }
                    //update  by yeben 第0屏为播放器面板,需从第startScreen屏开始
//                    addScreen(i,childs) ;
                    addScreen(i+startScreen,childs) ;
                    //end
                }
            }
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    //播放动画
                    playStartAnimation() ;
                }
            }
            ,  300) ;
            //add by yeben
            setcurrentScreen();
            //end
            //加载后面几屏
            if(shortcuts!=null&&shortcuts.size()>=SCREEN_SIZE){
                new Thread(){
                    public void run(){
                        loadHandler.sendEmptyMessage(0) ;
                    }
                }.start() ;
            }else{
                isFinishLoad = true ;
            }
        };
    } ;
    
    private Handler updateView = new Handler() {
    	
	    public void handleMessage(Message msg) {
	    	
	    	if(mMusicBar.getAudioManager().isMusicActive()) {
	    		mMusicBar.setVolume();
	    	} else {
	    		
	    	mMusicBar.setRingVolume();
	    	
	    	}
	    }
    	
    };
    
    @Override
	public void startActivity(Intent intent) {
		// TODO Auto-generated method stub
    	if(lastKilledComponentName == null || intent.getComponent() != lastKilledComponentName){
    		super.startActivity(intent);
    	}
	}

	@Override
    public void onCreate(Bundle savedInstanceState) {
        
        super.onCreate(savedInstanceState);
        //denglixia add 2011.9.16
//        setBackBtnStyle(false,-1,null);
//	    setStatusBarStyle(Activity.AUTO_BG,false);        

        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "------------------onCreate()-----------start="+System.currentTimeMillis()) ;
        }
        // 取得屏幕高宽
        screenWidth = Mogoo_GlobalConfig.getScreenWidth();
        screenHeight = Mogoo_GlobalConfig.getScreenHeight();

        // 单元格的长宽
        cellWidth = Mogoo_GlobalConfig.getWorkspaceCellWidth();
        cellHeight = Mogoo_GlobalConfig.getWorkspaceCellHeight();

        // 取得Launcher包名
        launcherPackage = this.getApplicationContext().getPackageName();
        if (launcherPackage == null || "".equals(launcherPackage)) {
            launcherPackage = "com.mogoo.launcher";
        }

        // 取得图片绑存
        iconCache = ((LauncherApplication) getApplication()).getIconCache();

        // 生成抖动控制器
        mVibrationController = new Mogoo_VibrationController(iconCache, resIds);

        // 取得从Launcher中传递过来的数据
        getLauncherData();
        
        //add by 袁业奔 2011-9-7
        //先注册广播
        mTaskBroadcastReceiver = new Mogoo_TaskBroadcastReceiver();
        mTaskBroadcastReceiver.setTaskManager(this);
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.mogoo.music.name");
        registerReceiver(mTaskBroadcastReceiver, filter);
        //音乐播放器面板
        mMusicPanel=new Mogoo_TaskMusicPanel(this);
        mMusicBar = new Mogoo_TaskMusicBar(this);
        //end
        //add by yeben 2011-12-23
        lastKilledComponentName = null;
        //end
        // 初始化
        init();
        
        //错误处理
        new RunRestorePolicy().start() ;
//        setStatusBarStyle(TRANSPARENT_BG, false);
        overridePendingTransition(R.anim.fade_in_fast, R.anim.fade_out_fast);
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "------------------onCreate()-----------end="+System.currentTimeMillis()) ;
        }
    }
    
    
    
    private class RunRestorePolicy extends Thread
    {
        public void run()
        {
            Mogoo_RestoreController RestoreController = new Mogoo_RestoreController(Mogoo_TaskManager.this);
            RestoreController.loadPolicy();
            Mogoo_UncaughtExceptionHandler uncaughtExceptionHandler = new Mogoo_UncaughtExceptionHandler(RestoreController,Mogoo_TaskManager.this);
            Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        }
     }

    @Override
    public void onBackPressed() {
        if (showDelIcon || isVibrate) {
            stopVibrate();
            removeDelIcon();
        } else {
            // finish() ;
            playFinishAnimation();
        }
    }

    @Override
    public void onAttachedToWindow() {
//         this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
        this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        super.onAttachedToWindow();
    }

	@Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
	
        if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_HOME) {
            if (showDelIcon || isVibrate) {
                stopVibrate();
                removeDelIcon();
                return true;
            } else if(!isFinished){
            	isFinished = true;
//            	screenImageView.requestFocus();
                playFinishAnimation();
            }
        }
        return super.onKeyUp(keyCode, event);
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.d(TAG, "keyCode+======" + keyCode );
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			updateView.sendEmptyMessage(0);
		}
		return super.onKeyDown(keyCode, event);
	}
@Override
	public boolean onKeyLongPress(int keyCode, KeyEvent event) {
		Log.d(TAG, "onKeyLongPress+======" + keyCode );
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			updateView.sendEmptyMessage(0);
		}
		
		return super.onKeyLongPress(keyCode, event);
	}
    @Override
    protected void onDestroy() {
        super.onDestroy();
        //add by yeben 2011-12-23
//        if(lastKilledComponentName != null){
//            Mogoo_TaskUtil.killTask(this, lastKilledComponentName);
//        }
        //end
        //add by 袁业奔 2011-9-9
        unregisterReceiver(mTaskBroadcastReceiver);
        //end
        Mogoo_TaskUtil.recycle(screenImg);
        
        if(recentTaskMapCache!=null){
            recentTaskMapCache.clear() ;
            recentTaskMapCache = null ;
        }
        
        if(runningTaskPackageCache!=null){
            runningTaskPackageCache.clear() ;
            runningTaskPackageCache = null ;
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (showDelIcon || isVibrate) {
            stopVibrate();
            removeDelIcon();
        } else if(!isFinished){
//            isFinished = true;
//            playFinishAnimation();
        }
        return false;
    }

    public boolean onLongClick(View v) {
        if(isFinishLoad){
            startVibrate() ;
        }
        return true;
    }

    public void onClick(View v) {
        if (v instanceof Mogoo_BubbleTextView && !isVibrate && isFinishLoad) {
            ShortcutInfo info = (ShortcutInfo) ((Mogoo_BubbleTextView) v).getTag();
            if (info.intent != null) {
//                startActivity(info.intent);
                Mogoo_TaskUtil.openTask(this, info.intent) ;
            }
        }
    }

    /**
     * 开始抖动
     * 
     * @author: 张永辉
     * @Date：2011-6-1
     */
    public void startVibrate() {
        if(mVibrationController != null){
            mVibrationController.startVibrate(this);
            isVibrate = true;
        }
    }

    /**
     * 停止抖动
     * 
     * @author: 张永辉
     * @Date：2011-6-1
     */
    public void stopVibrate() {
        if (mVibrationController != null) {
            mVibrationController.stopVibrate();
            isVibrate = false ;
        }
    }

    /**
     * 显示删除图标
     * 
     * @author: 张永辉
     * @Date：2011-6-1
     */
    public void showDelIcon() {
        showDelIcon = true;
    }

    /**
     * 移除删除图标
     * 
     * @author: 张永辉
     * @Date：2011-6-1
     */
    public void removeDelIcon() {
        showDelIcon = false;
    }

    /**
     * 取得从Launcher中传递过来的数据
     * 
     * @author: 张永辉
     * @Date：2011-5-26
     */
    private void getLauncherData() {
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "------------------getLauncherData()-----------start="+System.currentTimeMillis()) ;
        }
        
        Intent intent = getIntent();
        statusBarHeight = intent.getIntExtra(EXTRA_STATUS_BAR_HEIGHT, 30);
        counts = intent.getIntArrayExtra(EXTRA_ICON_COUNT);
        Log.d(TAG, "counts[0]=" + counts[0] +"counts[0]=" + counts[1] + "counts[0]="+counts[2]);
        // 屏幕截图，如果是从launcher中传过来的，且没处理为半透明的图片
//        byte[] bytes = intent.getByteArrayExtra(EXTRA_SCREEN_IMG);
//        if (bytes != null) {
//            screenImg = BitmapFactory.decodeByteArray(bytes, 0, bytes.length).copy(
//                    Bitmap.Config.ARGB_8888, true);
//        }
//        screenImg = intent.getParcelableExtra(EXTRA_SCREEN_IMG) ;
        screenImg = Launcher.screenImg ;
        // 如果没有取得传过来的屏幕截图，则取当前的壁纸，并设为半透明
        if (screenImg == null) {
            // screenImg =
            // ((BitmapDrawable)getWallpaper()).getBitmap().copy(Bitmap.Config.ARGB_8888,
            // true) ;
            screenImg = Bitmap.createBitmap(((BitmapDrawable) getWallpaper()).getBitmap(), 0,
                    statusBarHeight, screenWidth, screenHeight - statusBarHeight);
            Canvas canvas = new Canvas(screenImg);
            Paint paint = new Paint();
            paint.setAlpha(ALPHA);
            canvas.drawPaint(paint);
        } else {
            Bitmap wallpager = Bitmap.createBitmap(Mogoo_TaskUtil.drawable2Bitmap(getWallpaper()), 0,
                    statusBarHeight, screenWidth, screenHeight - statusBarHeight);
            Canvas canvas = new Canvas(wallpager);
            Paint paint = new Paint();
            paint.setAlpha(ALPHA);
            canvas.drawBitmap(screenImg, 0, 0, paint);
            Mogoo_TaskUtil.recycle(screenImg);
            screenImg = wallpager;
        }

//        bytes = null;
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "------------------getLauncherData()-----------end="+System.currentTimeMillis()) ;
        }
    }

    /**
     * 初始化
     * 
     * @author: 张永辉
     * @Date：2011-5-26
     */
    private void init(){
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "------------------init()-----------start="+System.currentTimeMillis()) ;
        }
        setContentView(R.layout.mogoo_task_manager) ;
        //初始化删除图标
        delIcon = iconCache.getBitmap(R.drawable.mogoo_task_del);
        
        screenImageView = (ImageView)this.findViewById(R.id.screenImage) ;
        taskWorkspace = (Mogoo_TaskWorkspace)this.findViewById(R.id.taskWorkspace) ;
        
        screenImageView.setImageBitmap(screenImg) ;
        screenImageView.setOnTouchListener(new OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getY() < screenHeight - statusBarHeight - taskWorkspace.getHeight()){
                    if(closed){
                      return false;
                  }
                  closed = true;
                  playFinishAnimation() ;
                }
                return false;
            }
        });
        
        
        //设置任务栏的背景图片
        taskWorkspace.setBackgroundDrawable(createTaskListGalleryBg()) ;
        taskWorkspace.setTaskManager(this);
        taskWorkspace.setOnLongClickListener(this);
        
//        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) taskWorkspace.getChildAt(0).getLayoutParams();
//        System.out.println(lp.cellX + " " + lp.cellY + " " + lp.cellHSpan + " " + lp.cellVSpan);
//        
//        View musicPanel = getLayoutInflater().inflate(R.layout.mogoo_task_music, null);
//        CellLayout.LayoutParams panelLp = new CellLayout.LayoutParams(0, 0, 4, 1);
//        musicPanel.setLayoutParams(panelLp);
//        taskWorkspace.addView(musicPanel, 0);
        
        Mogoo_ComponentBus bus = Mogoo_ComponentBus.getInstance();
        bus.addActivityComp(R.id.taskWorkspace, taskWorkspace, this);
        
        new Thread(){
           public void run(){
               initHandler.sendEmptyMessage(0) ;
           }
        }.start() ;
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "------------------init()-----------end="+System.currentTimeMillis()) ;
        }
    }

    /**
     * 载剪屏幕截图
     * 
     * @author: 张永辉
     * @Date：2011-5-26
     * @param height 图高度
     */
    private void cutScreenImg(int height) {
        if (screenImg != null) {
            screenImg = Bitmap.createBitmap(screenImg, 0, screenImg.getHeight() - height,
                    screenWidth, height);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVibrationController != null) {
            mVibrationController.stopVibrate();
        }
        
    }

    /**
     * 创建任务列表背景图
     * 
     * @author: 张永辉
     * @Date：2011-5-30
     * @return
     */
    private Drawable createTaskListGalleryBg() {
        if (taskListGalleryBg != null) {
            return taskListGalleryBg;
        }
        Bitmap bg = Bitmap.createBitmap(
                Mogoo_TaskUtil.decodeResource(this.getResources(), R.drawable.mogoo_task_list_bg), 0, 0,
                this.getWindowManager().getDefaultDisplay().getWidth(), cellHeight);
        return Mogoo_TaskUtil.bitmap2Drawable(bg);
    }
    

    /**
     * 插入taskCellLLayout
     * 
     * @author: 张永辉
     * @Date：2011-6-16
     */
    private void addScreen(int index,List<Mogoo_BubbleTextView> childs){
        taskWorkspace.addView(createScreen(childs),index) ;
    }

    /**
     * 创建taskCellLayout
     * 
     * @author: 张永辉
     * @Date：2011-6-16
     * @return
     */
    private Mogoo_TaskCellLayout createScreen(List<Mogoo_BubbleTextView> childs){
        LayoutInflater inflater = LayoutInflater.from(this) ;
        Mogoo_TaskCellLayout taskCellLayout =  (Mogoo_TaskCellLayout)(inflater.inflate(R.layout.mogoo_task_screen, null)) ;
        for(Mogoo_BubbleTextView child:childs){
            ShortcutInfo info = (ShortcutInfo)child.getTag() ;
            taskWorkspace.addInScreen(child, taskCellLayout, info.cellX, info.cellY, 1, 1,false);
        }
        return taskCellLayout ;
    }

    /**
     * 绑定任务图标到任务栏中
     * 
     * @author: 张永辉
     * @Date：2011-6-16
     */
//    private void bindItems(List<ShortcutInfo> shortcuts) {
//        if (shortcuts != null) {
//            for (ShortcutInfo info : shortcuts) {
//                View shortcut = createShortcut(R.layout.application,
//                        (MT_TaskCellLayout) (taskWorkspace.getChildAt(info.screen)), info, true);
//                taskWorkspace.addInScreen(shortcut, info.screen, info.cellX, info.cellY, 1, 1,
//                        false);
//            }
//        }
//    }

    /**
     * 创建任务栏上的图标视图
     * 
     * @author: 张永辉
     * @Date：2011-6-16
     * @param layoutResId
     * @param parent
     * @param info
     * @param getFromCache
     * @return
     */
    private View createShortcut(int layoutResId, ViewGroup parent, ShortcutInfo info, boolean getFromCache) {
        final ShortcutInfo infoTemp = info ;
        LayoutInflater inflater = LayoutInflater.from(this) ;
        Mogoo_BubbleTextView favorite = (Mogoo_BubbleTextView) inflater.inflate(layoutResId, parent,
                false);
        //edit by yeben 2011-12-27
      favorite.setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(
              getFromCache ? iconCache.getIcon(info.intent) : info.getIcon(iconCache)), null,
              null);
//        favorite.setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(
//              getFromCache ? iconCache.getIcon(info.intent) : fixInfoIcon(info.getIcon(iconCache),info.appType == 0)), null,
//              null);
        //end
        favorite.setText(info.title);
        favorite.setTag(info);
        favorite.setOnClickListener(this);
        favorite.setDelIcon(delIcon);
        
        //注册删除事件
        ((Mogoo_TaskBubbleText)favorite).setOnDelListener(new Mogoo_TaskBubbleText.OnDelListener() {
            
            public void onDel(ComponentName cn) {
            	//edit by yeben 2011-12-23
            	lastKilledComponentName = cn;
            	Mogoo_TaskUtil.killTask(Mogoo_TaskManager.this, lastKilledComponentName) ;
//                Mogoo_TaskUtil.killTask(Mogoo_TaskManager.this, cn) ;
                //end
            	//add by 袁业奔 2011-10-25 如果杀掉的进程是音乐播放器进程
            	if(cn.getPackageName().equals("com.android.music")){
                      setPlayState("pause");
                      displayMusicName("");
            	}
                //end
                Mogoo_TaskCellLayout taskCellLayout = (Mogoo_TaskCellLayout)taskWorkspace.getChildAt(infoTemp.screen) ;
                //如果只有一个图标且处于最后一屏，则删除后减屏
                if(taskWorkspace.indexOfChild(taskCellLayout)==taskWorkspace.getChildCount()-1
                        &&taskCellLayout.getChildCount()<=1){
                    taskCellLayout.removeAllViews() ;
                    //update by yeben 2011-9-1
//                    if(infoTemp.screen>0){
//                        taskWorkspace.removeViewAt(infoTemp.screen) ;
//                        taskWorkspace.scrollLeft() ;
//                    }
                    if(infoTemp.screen>startScreen){
                        taskWorkspace.removeViewAt(infoTemp.screen) ;
                        taskWorkspace.scrollLeft() ;
                    }
                    //end
                }else{//删除后，如果后面有图标则向前移
                    moveChildLeft(infoTemp.screen,infoTemp.cellX) ;
                  }
                
                iconCache.recycle(cn, Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_VIBRATION);
            }
        }) ;
        
        Log.d(TAG, "info =" + info.getIntent().getComponent().getClassName());
        
       if(checkType(SMS_CLASS_NAME, info) || "com.android.mms.ui.MogooSmsDisplayDialog".equals(info.getIntent().getComponent().getClassName())){
    	   Log.d(TAG, "SMS_CLASS_NAME==");
                favorite.setCountIcon(iconCache, counts[Mogoo_ContentListener.SMS_INDEX], info.appType);
       }else if(checkType(TELEPHONE_CLASS_NAME, info)){
    	   Log.d(TAG, "TELEPHONE_CLASS_NAME==");
                favorite.setCountIcon(iconCache, counts[Mogoo_ContentListener.TELEPHONE_INDEX], info.appType);
       }else if(checkType(MARKET_CLASS_NAME, info)){
    	   Log.d(TAG, "MARKET_CLASS_NAME==");
                favorite.setCountIcon(iconCache, counts[Mogoo_ContentListener.MARKET_INDEX], info.appType);
        }

        
        return favorite;
    }

    private boolean checkType(String type, ShortcutInfo info) {
        if(info.getIntent()!= null && info.getIntent().getComponent() != null){
            return type.equals(info.getIntent().getComponent().getClassName());
        }
        
        return false;
    }
    
    /**
     * index之后的所有图标向前移一个单元格
     *@author: 张永辉
     *@Date：2011-6-16
     *@param index
     */
    private void moveChildLeft(int screen,int index){
        Mogoo_TaskCellLayout currentCellLayout = (Mogoo_TaskCellLayout)(taskWorkspace.getChildAt(screen)) ;
        currentCellLayout.removeViewAt(index) ;
        
        for(int i=screen+1;i<taskWorkspace.getChildCount();i++){
            Mogoo_TaskCellLayout taskCellLayout = (Mogoo_TaskCellLayout)(taskWorkspace.getChildAt(i)) ;
            Mogoo_TaskCellLayout lastTaskCellLayout = (Mogoo_TaskCellLayout)(taskWorkspace.getChildAt(i-1)) ;
            View child =  taskCellLayout.getChildAt(0) ;
//            ((MT_BubbleTextView)child).stopVibrate();
            if(child!=null){
                taskCellLayout.removeViewAt(0) ;
                lastTaskCellLayout.addView(child, SCREEN_SIZE-1) ;
                ((Mogoo_BubbleTextView)child).startVibrate(iconCache, 0, false) ;
                if(taskCellLayout.getChildCount()<=0){
                    taskWorkspace.removeView(taskCellLayout) ;
                }
            }
        }
        
//        taskWorkspace.invalidate() ;
//        taskWorkspace.requestLayout() ;
        taskWorkspace.reLayoutAllCellLayout() ;
        resetAllChildTag() ;
    }
    
    /**
     * 重置所有图标的TAG
     *@author: 张永辉
     *@Date：2011-6-17
     */
    private void resetAllChildTag(){
        int screenCount = taskWorkspace.getChildCount() ;
        //update by yeben 2011-9-1 
//        for(int i=0;i<screenCount;i++)
        for(int i=startScreen;i<screenCount;i++){
        	//end
            Mogoo_TaskCellLayout taskCellLayout = (Mogoo_TaskCellLayout)(taskWorkspace.getChildAt(i)) ;
            int childCount = taskCellLayout.getChildCount() ;
            for(int j=0;j<childCount;j++){
                View child = taskCellLayout.getChildAt(j) ;
                ShortcutInfo info = (ShortcutInfo)(child.getTag()) ;
                info.screen=i ;
                info.cellX = j ;
            }
        }
    }

    /**
     * 根据所在序号取得所在单元格坐标
     * 
     * @author: 张永辉
     * @Date：2011-6-16
     * @param index
     * @return
     */
    private int[] getCellCoordinateByIndex(int index) {
        int cellX = index - getScreenByIndex(index) * SCREEN_SIZE;
        return new int[] {
                cellX, 0
        };
    }

    /**
     * 根据所在序号取得所在屏号
     * 
     * @author: 张永辉
     * @Date：2011-6-16
     * @param index
     * @return
     */
    private int getScreenByIndex(int index) {
        int screen = index / SCREEN_SIZE ;
        // Log.d(TAG, "index="+index+" screen="+screen) ;
        return screen;
    }

    /**
     * 根据当前正在运行的任务生成ShortcutInfo
     * 
     * @author: 张永辉
     * @Date：2011-6-16
     * @return
     */
    private List<ShortcutInfo> generateShortcutInfos(int maxNum) {
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "------------------generateShortcutInfos()-----------start="+System.currentTimeMillis()) ;
        }
        List<ShortcutInfo> infos = new ArrayList<ShortcutInfo>();
        List<Mogoo_Task> tasks = getRunningTask(maxNum);
        if (tasks != null) {
            int index = 0;
            for (Mogoo_Task task : tasks) {
                ShortcutInfo info = new ShortcutInfo();
                info.appType = LauncherSettings.Favorites.APP_TYPE_OTHER;
                info.cellX = getCellCoordinateByIndex(index)[0];
                info.cellY = 0;
                info.container = 0;
//                Intent intent = new Intent();
//                intent.setComponent(task.getComponentName());
//                info.intent = intent;
                info.intent = task.getIntent() ;
                info.isSystem = LauncherSettings.Favorites.NOT_SYSTEM_APP;
                info.itemType = LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT;
                //update by yeben 2011-9-1 
                //info.screen = getScreenByIndex(index);
                info.screen = getScreenByIndex(index)+startScreen;
                //end
                info.spanX = 1;
                info.spanY = 1;
                info.title = task.getTitle();
                info.setIcon(task.getIcon());
                infos.add(info);
                index++;
            }
        }
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "------------------generateShortcutInfos()-----------end="+System.currentTimeMillis()) ;
        }
        return infos;
    }
    
    /**
     * 取得正在运行的任务包列表
     * @author: 张永辉
     * @Date：2011-5-26
     * @param context
     * @param maxNum
     * @return
     */
    private List<String> getRunningTaskPackage(int maxNum){
        if(runningTaskPackageCache!=null){
            return runningTaskPackageCache ;
        }else{
            List<String> runningTaskPackage = new ArrayList<String>() ;
            List<ActivityManager.RunningTaskInfo> runningTasks = Mogoo_TaskUtil.getRunningTask(this,maxNum);
            if(runningTasks!=null){
                for(ActivityManager.RunningTaskInfo runningTask:runningTasks){
                    String packageName = runningTask.baseActivity.getPackageName() ;
                    if(!runningTaskPackage.contains(packageName))
                    runningTaskPackage.add(packageName) ;
                }
            }
            runningTaskPackageCache = runningTaskPackage ;
            return runningTaskPackage ;
        }
        
    }

    /**
     * 取得正在运行的任务
     * 
     * @author: 张永辉
     * @Date：2011-5-26
     * @param context
     * @param maxNum
     * @return
     */
    private List<Mogoo_Task> getRunningTask(int maxNum) {
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "------------------getRunningTask()-----------start="+System.currentTimeMillis()) ;
        }
        
        List<Mogoo_Task> tasks = new ArrayList<Mogoo_Task>();
        
        List<String> runningTaskPackages = getRunningTaskPackage(Integer.MAX_VALUE);
        
        if (runningTaskPackages != null) {
            Map<String,Intent> recentTaskMap = getRecentTask(Integer.MAX_VALUE) ;
            int count = 0 ;
            for (String runningTaskPackage : runningTaskPackages) {
                
                if(count>=maxNum) break ;
                
                if (runningTaskPackage.equals(launcherPackage)) {
                    continue;
                }
                if(recentTaskMap.containsKey(runningTaskPackage)){
                    //电话和通讯录
//                    if(CONTACT_PACKAGE_NAME.equals(packageName)||TELEPHONE_PACKAGE_NAME.equals(packageName)){
//                        MT_Task taskContact = new MT_Task();
//                        taskContact.setComponentName(new ComponentName(CONTACT_PACKAGE_NAME, "com.android.contacts.DialtactsContactsEntryActivity")) ;
//                        taskContact.setIcon(MT_TaskUtil.getIcon(this, taskContact.getComponentName())) ;
//                        taskContact.setTitle(MT_TaskUtil.getTitle(this, taskContact.getComponentName()));
//                        tasks.add(taskContact);
//                        
//                        MT_Task taskPhone = new MT_Task();
//                        taskPhone.setComponentName(new ComponentName(CONTACT_PACKAGE_NAME, "com.android.contacts.DialtactsActivity")) ;
//                        taskPhone.setIcon(MT_TaskUtil.getIcon(this, taskPhone.getComponentName())) ;
//                        taskPhone.setTitle(MT_TaskUtil.getTitle(this, taskPhone.getComponentName()));
//                        tasks.add(taskPhone);
//                    }else{
                        count ++ ;
                        Mogoo_Task task = new Mogoo_Task();
                        task.setIntent(recentTaskMap.get(runningTaskPackage)) ;
                        task.setComponentName(task.getIntent().getComponent());
                        
                        if(Mogoo_GlobalConfig.LOG_DEBUG){
                            Log.d(TAG,task.getComponentName().toString());
                        }
                        
                        task.setIcon(Mogoo_TaskUtil.getIcon(this, task.getComponentName())) ;
                        task.setTitle(Mogoo_TaskUtil.getTitle(this, task.getComponentName()));
//                        task.setComponentName(runningTask.baseActivity);
//                        Log.d(TAG,
//                                task.getComponentName().toString() + " top:"
//                                        + runningTask.topActivity.toString());
//                        task.setIcon(MT_TaskUtil.getIcon(this, task.getComponentName()));
//                        task.setTitle(MT_TaskUtil.getTitle(this, task.getComponentName()));
                        tasks.add(task);
//                    }
                }
            }
        }
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "------------------getRunningTask()-----------end="+System.currentTimeMillis()) ;
        }

        return tasks;
    }
    
    /**
     * 取得最近启动过的应用
     *@author: 张永辉
     *@Date：2011-6-20
     *@param maxNum
     *@return
     */
    private Map<String,Intent> getRecentTask(int maxNum){
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "------------------getRecentTask()-----------start="+System.currentTimeMillis()) ;
        }
        
        if(recentTaskMapCache!=null){
            return recentTaskMapCache ;
        }
        
        Map<String,Intent> recentTaskMap = new HashMap<String, Intent>() ;
        List<ActivityManager.RecentTaskInfo> infos = Mogoo_TaskUtil.getRecentTask(this, maxNum) ;
        if(infos!=null){
            for(ActivityManager.RecentTaskInfo info : infos){
                Intent intent = new Intent(info.baseIntent) ;
                //其它桌面应用不能出现
                if(intent.hasCategory(Intent.CATEGORY_HOME)&&!launcherPackage.equals(intent.getComponent().getPackageName())){
                    Mogoo_TaskUtil.killTask(this,intent.getComponent()) ;
                    continue ;
                }
                if(info.origActivity!=null){
                    intent.setComponent(info.origActivity) ;
//                    recentTaskMap.put(info.origActivity.getPackageName(), info.origActivity) ;
                }
                recentTaskMap.put(intent.getComponent().getPackageName(), intent) ;
            }
        }
        
        //过滤设置应用
        if(recentTaskMap.containsKey(SETTINGS_PACKAGE_NAME)){
        	 Intent intent = getPackageManager().getLaunchIntentForPackage(SETTINGS_PACKAGE_NAME);
        	 if(intent != null){
            recentTaskMap.put(SETTINGS_PACKAGE_NAME, intent) ;
        	 }
        }
        
        recentTaskMapCache = recentTaskMap ;
        
        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "------------------getRecentTask()-----------end="+System.currentTimeMillis()) ;
        }
        
        return recentTaskMapCache ;
    }

    /**
     * 播放打开动画
     * 
     * @author: 张永辉
     * @Date：2011-5-26
     */
    private void playStartAnimation() {
//        Log.d(TAG, "taskWorkspace.height="+taskWorkspace.getHeight()) ;
        TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, 0, -taskWorkspace.getHeight());
        translateAnimation.setDuration(500);
        translateAnimation.setFillAfter(true);
        translateAnimation.setFillBefore(true);
        translateAnimation.setAnimationListener(new Animation.AnimationListener() {

            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                // screenImageView.clearAnimation();
            	closed = false;
            }
        });

        screenImageView.startAnimation(translateAnimation);
    }

    /**
     * 播放关闭动画
     * 
     * @author: 张永辉
     * @Date：2011-6-2
     */
    private void playFinishAnimation() {
//        Log.d(TAG, "-----------playFinishAnimation()-------------");
        TranslateAnimation translateAnimation = new TranslateAnimation(0, 0, -taskWorkspace.getHeight(), 0);
        translateAnimation.setDuration(500);
        translateAnimation.setFillAfter(true);
        // translateAnimation.setFillBefore(true) ;
        translateAnimation.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
            	finish();
                screenImageView.clearAnimation();
//                Intent intent = new Intent();
//                intent.setClass(Mogoo_TaskManager.this, Launcher.class);
//                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                startActivity(intent);
            }
        });

        screenImageView.startAnimation(translateAnimation);
    }

    public boolean isFinishLoad() {
        return isFinishLoad;
    }

    public void setFinishLoad(boolean isFinishLoad) {
        this.isFinishLoad = isFinishLoad;
    }
    //add by yeben 2011-9-1
    //加入音乐播放器控制面板
    public void addMusicPanel(){
    	List<Mogoo_BubbleTextView> childs0 = new ArrayList<Mogoo_BubbleTextView>() ;
    	addScreen(1,childs0) ;
    	taskWorkspace.addInScreen(mMusicPanel, 1, 0, 0, 4, 1, true);
    }
    public void setcurrentScreen(){
        taskWorkspace.setCurrentScreen(startScreen);
    }

    
	public void displayMusicName(String musicName) {
		// TODO Auto-generated method stub
		mMusicPanel.displayMusicName(musicName);
	}
	public void setPlayState(String state){
		mMusicPanel.setPlayState(state);
	}
    //end
	
	private void addMusicBar(){
    	List<Mogoo_BubbleTextView> childs0 = new ArrayList<Mogoo_BubbleTextView>() ;
    	addScreen(0,childs0) ;
    	taskWorkspace.addInScreen(mMusicBar, 0, 0, 0, 4, 1, true);
	}
	
	public Mogoo_VibrationController getVibrationController(){
		return mVibrationController;
	}
}
