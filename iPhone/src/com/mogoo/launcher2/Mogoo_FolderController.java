/**  
 * 文 件 名:  MT_FolderController.java  
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
import com.mogoo.launcher2.CellLayout.LayoutParams;
import com.mogoo.launcher2.animation.Mogoo_IconFolderAnimation;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_BitmapCache;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;
import com.mogoo.launcher2.utils.Mogoo_Utilities;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;


public class Mogoo_FolderController implements OnClickListener {
    
    private static String TAG = "Mogoo_FolderController" ;
    
    private Launcher launcher;

    private Context context;

    private Mogoo_IconFolderAnimation iconFolderAnimation;

    private Mogoo_BubbleTextView lastActiveIcon;

    private Mogoo_BubbleTextView tempActiveIcon;

    private static final int ICON_FOLDER_OPEN = 1;

    private static final int FOLDER_OPEN = 2;

    private static final int INACTIVE = 3;

    private boolean isListen = false;

    private long LISTEN_DELAY = 500;
    
    private boolean canActive = false;
    
    private boolean active = false;
    
    private Mogoo_FolderWorkspace folderWorkspace;

    public Handler activeHandle = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.obj instanceof Mogoo_BubbleTextView) {
                iconFolderInactive();
                iconFolderActive((Mogoo_BubbleTextView) msg.obj);
            }
        }
    };

    public Handler folderOpenHandle = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ICON_FOLDER_OPEN:
                    if (msg.obj instanceof Mogoo_FolderBubbleText) {
                        canActive = false;
                        ((Mogoo_FolderBubbleText) msg.obj).openFolder();
                        iconFolderInactive();
                    }
                    break;
                case FOLDER_OPEN:
                    if (msg.obj instanceof Mogoo_FolderBubbleText) {
                        openFolder((Mogoo_FolderBubbleText) msg.obj);
                    }
                    break;
                case INACTIVE:
                    if (msg.obj instanceof Mogoo_BubbleTextView){
                        iconFolderAnimation.cancelIconFolder((Mogoo_BubbleTextView)msg.obj);
                    }
                    iconFolderInactive();
                    break;
            }
        }
    };

    private Thread openFolderListener = new Thread() {
        private Mogoo_BubbleTextView preActiveIcon;

        public void run() {
            while (isListen) {
                if(!canActive){
                    tempActiveIcon = null;
                }
                
                if (tempActiveIcon != null && tempActiveIcon.equals(preActiveIcon) && !tempActiveIcon.equals(lastActiveIcon)) {
                    Message msg = new Message();
                    msg.obj = tempActiveIcon;
                    
                    activeHandle.sendMessage(msg);
                }
                else if(lastActiveIcon != null && tempActiveIcon == null)
                {
                    Message msg = new Message();
                    msg.what = INACTIVE;
                    folderOpenHandle.sendMessage(msg);
                }

                preActiveIcon = tempActiveIcon;

                try {
                    Thread.sleep(LISTEN_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            preActiveIcon = null;
        };
    };

    public void setCanActive(boolean canActive) {
        this.canActive = canActive;
    }

    public Mogoo_FolderController(Context cxt) {
        context = cxt;
        iconFolderAnimation = new Mogoo_IconFolderAnimation(cxt,
                ((LauncherApplication) (cxt.getApplicationContext())).getIconCache());
        folderWorkspace = (Mogoo_FolderWorkspace) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.folderWorkspace, cxt);
    }

    public void setLauncher(Launcher launcher) {
        this.launcher = launcher;
    }

    /**
     * 启动文件夹监听 @ author: 黄悦
     * 
     * @param launcher
     */
    public void startOpenFolderListener() {
        if (!isListen && !openFolderListener.isAlive()) {
            isListen = true;
            openFolderListener.start();
        }
    }

    /**
     * 终止文件夹监听 @ author: 黄悦
     * 
     * @param launcher
     */
    public void stopOpenFolderListener() {
        isListen = false;
        // openFolderListener.stop();
    }

    /**
     * 激活图标文件夹判断 @ author: 黄悦
     * 
     * @return 返回 true为激活，否则为不激活
     */
    private boolean iconFolderActive(Mogoo_BubbleTextView bubbleTextView) {
        // 当图标文件夹功能未激活时
        if (!Mogoo_GlobalConfig.ICON_FOLDER) {
            return false;
        }

        if ((lastActiveIcon != null && lastActiveIcon.equals(bubbleTextView)) || Mogoo_FolderBubbleText.folderOpening) {
            return true;
        }

        iconFolderAnimation.activeIconFolder(bubbleTextView);
        lastActiveIcon = bubbleTextView;

        //防止图标合并打开和计时器打开两个进程造成交叉影响，关闭图标合并时的计时器打开
        if(!Mogoo_FolderBubbleText.folderOpening && bubbleTextView instanceof Mogoo_FolderBubbleText){
            FolderOpenTimer fot = new FolderOpenTimer(lastActiveIcon);
            fot.start();
    
            fot = null;
        }
        
        active = true;

        return true;
    }

    /**
     * 清除已激活图标文件夹的图标 @ author: 黄悦
     */
    public void iconFolderInactive() {
        if (lastActiveIcon != null && active) {
            iconFolderAnimation.cancelIconFolder(lastActiveIcon);
            active = false;
        }
//        Log.d(TAG, "Inactive");
        lastActiveIcon = null;
    }
    
    public boolean isActive() {
		return active;
	}

	/**
     * 新生成图标文件夹替换图标 @ author: 黄悦
     * 
     * @return
     */
    public View replaceIcon2Folder(ViewGroup parent, View dragView, int screen, int targetIndex) {
        Mogoo_BubbleTextView targetView = (Mogoo_BubbleTextView) parent.getChildAt(targetIndex);
        
        if(targetView != null && targetView.equals(dragView)){
            return null;
        }
        
        View bubbleText = null;
        Mogoo_FolderInfo info = null;
        if (targetView instanceof Mogoo_FolderBubbleText) {
            info = (Mogoo_FolderInfo) targetView.getTag();
            Mogoo_BitmapCache iconCache = ((LauncherApplication)context.getApplicationContext()).getIconCache();
            
            iconCache.recycle(info.intent.getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
            iconCache.remove(info.intent.getComponent());
            
            Mogoo_ContentListener contentListener = launcher.getContentListener();
            ShortcutInfo dragInfo = (ShortcutInfo) ((Mogoo_BubbleTextView) dragView).getTag();
            info.addItem(context, dragInfo);
            targetView.setCompoundDrawablesWithIntrinsicBounds(null, new FastBitmapDrawable(info.getIcon(iconCache)), null, null);
            
            if(dragView instanceof Mogoo_BubbleTextView){
                contentListener.addItem(dragInfo.appType, targetView);
                targetView.stopVibrate();
                targetView.setCountIcon(iconCache, contentListener.getCountByType(dragInfo.appType), dragInfo.appType);
                targetView.startVibrate(iconCache, 0);
            }
            
            bubbleText = targetView;
        } else {
            info = Mogoo_FolderInfo.createFolder((Workspace) Mogoo_ComponentBus
                    .getInstance().getActivityComp(R.id.workspace, context), targetIndex,
                    (Mogoo_BubbleTextView) dragView, screen);
            
            bubbleText = launcher.createShortcut(info);
            parent.removeView(targetView);
            parent.addView(bubbleText, targetIndex);
        }
        
        
        
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) bubbleText.getLayoutParams();
        int[] cellXY = Mogoo_Utilities.convertToCell(targetIndex);
        lp.cellX = cellXY[0];
        lp.cellY = cellXY[1];
        
        targetView = null;
        
        return bubbleText;
    }

    public Mogoo_BubbleTextView getLastActiveIcon() {
        return lastActiveIcon;
    }

    public void onClick(View v) {
        if (!(v instanceof Mogoo_FolderBubbleText) || Mogoo_FolderBubbleText.folderOpening || folderWorkspace.getVisibility() == View.VISIBLE) {
            return;
        }

        canActive = false;
        Mogoo_FolderBubbleText folder = (Mogoo_FolderBubbleText) v;
        folder.openFolder();
    }

    public void setTempActiveIcon(Mogoo_BubbleTextView tempActiveIcon) {
        this.tempActiveIcon = tempActiveIcon;
    }

    private void openFolder(Mogoo_FolderBubbleText folder) {
        if (lastActiveIcon == null) {
            iconFolderInactive();
            return;
        }
        CellLayout cellLayout = (CellLayout) (lastActiveIcon.getParent());

        if (cellLayout == null) {
            return;
        }

        cellLayout.removeView(lastActiveIcon);
        ShortcutInfo info = (ShortcutInfo) lastActiveIcon.getTag();
        int targetIndex = cellLayout.getIndexByCellXY(info.cellX, info.cellY);
        
        cellLayout.addView(folder, targetIndex);
        LayoutParams lp = (LayoutParams) folder.getLayoutParams();
        lp.cellX = info.cellX;
        lp.cellY = info.cellY;

        folder.openFolder();
    }

    /**
     * 文件打开定时器
     */
    class FolderOpenTimer extends Thread {
        private Mogoo_BubbleTextView bubbleText;

        public FolderOpenTimer(Mogoo_BubbleTextView folder) {
            this.bubbleText = folder;
        }

        public void run() {
            try {
                Thread.sleep(Mogoo_GlobalConfig.getFolderStayTime());
                
                if(lastActiveIcon == null || !canActive){
                    return;
                } 
                // 检查是否具备文件夹打开的条件
                Message msg = new Message();
                
                if(Mogoo_GlobalConfig.LOG_DEBUG)
                {
                    Log.d(TAG, "open by FolderOpenTimer"); 
                }

                if (bubbleText instanceof Mogoo_FolderBubbleText && lastActiveIcon != null && !Mogoo_FolderBubbleText.folderOpening) {
                    Mogoo_FolderBubbleText folder = (Mogoo_FolderBubbleText) bubbleText;
                    if (folder != null && folder.equals(lastActiveIcon)) {
                        msg.what = ICON_FOLDER_OPEN;
                        msg.obj = folder;
                        folderOpenHandle.sendMessage(msg);
                        
                        //add by 张永辉 2011-3-29 当不够行时，加一行
                        Mogoo_FolderInfo info = (Mogoo_FolderInfo)folder.getTag() ;
                        int size = info.getContents().size() ;
                        int col = Mogoo_GlobalConfig.getWorkspaceLongAxisCells(Mogoo_GlobalConfig.isLandscape()) ;
                        if(size< col*(Mogoo_GlobalConfig.getWorkspaceShortAxisCells(Mogoo_GlobalConfig.isLandscape())-1)
                                && size%col==0)
                        {
                            info.setAddRow(true) ;
                        }
                        //end
                        // folder.openFolder() ;
                    } else {
                        msg.what = INACTIVE;
                        folderOpenHandle.sendMessage(msg);
                        // iconFolderInactive() ;
                    }
                } else if (bubbleText instanceof Mogoo_BubbleTextView && lastActiveIcon != null && !Mogoo_FolderBubbleText.folderOpening) {
                    if (bubbleText != null && bubbleText.equals(lastActiveIcon)) {
                        ShortcutInfo info = (ShortcutInfo) lastActiveIcon.getTag();

                        int targetIndex = Mogoo_Utilities.getIndexByCellXY(info.cellX, info.cellY);

                        Mogoo_FolderInfo folderInfo = Mogoo_FolderInfo.createFolder(
                                (Workspace) Mogoo_ComponentBus.getInstance().getActivityComp(
                                        R.id.workspace, context), targetIndex, null, info.screen);
                        
                        //add by 张永辉 2011-3-29 当不够行时，加一行
                        int size = folderInfo.getContents().size() ;
                        int col = Mogoo_GlobalConfig.getWorkspaceLongAxisCells(Mogoo_GlobalConfig.isLandscape()) ;
                        if(size< col*(Mogoo_GlobalConfig.getWorkspaceShortAxisCells(Mogoo_GlobalConfig.isLandscape())-1)
                                && size%col==0)
                        {
                            folderInfo.setAddRow(true) ;
                        }
                        //end

                        Mogoo_FolderBubbleText folder = (Mogoo_FolderBubbleText) launcher
                                .createShortcut(folderInfo);

                        msg.what = FOLDER_OPEN;
                        msg.arg1 = targetIndex;
                        msg.obj = folder;
                        folderOpenHandle.sendMessage(msg);
                        // CellLayout cellLayout =
                        // (CellLayout)(lastActiveIcon.getParent()) ;
                        // cellLayout.removeView(lastActiveIcon) ;
                        //
                        // cellLayout.addView(folder, targetIndex) ;
                        //
                        // folder.openFolder() ;
                    } else {
                        msg.what = INACTIVE;
                        msg.obj = bubbleText;
                        folderOpenHandle.sendMessage(msg);
//                         iconFolderInactive() ;
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
