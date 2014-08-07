/**  
 * 文 件 名:  MT_IconFolderAnimation.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：  黄悦                     
 * 版    本:  1.0  
 * 创建时间:   2011-3-10
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-3-10        黄悦       1.0          1.0 Version  
 */

package com.mogoo.launcher2.animation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.CellLayout;
import com.mogoo.launcher2.FastBitmapDrawable;
import com.mogoo.launcher2.LauncherApplication;
import com.mogoo.launcher2.Mogoo_BubbleTextView;
import com.mogoo.launcher2.Mogoo_DockWorkSpace;
import com.mogoo.launcher2.Mogoo_FolderBubbleText;
import com.mogoo.launcher2.Mogoo_FolderController;
import com.mogoo.launcher2.Mogoo_FolderLayout;
import com.mogoo.launcher2.Mogoo_FolderWorkspace;
import com.mogoo.launcher2.ShortcutInfo;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.utils.Mogoo_BitmapCache;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;

public class Mogoo_IconFolderAnimation {
	
	private static String TAG = "Mogoo_IconFolderAnimation" ;
	
    private Context context;

    private Mogoo_BitmapCache iconCache;

    private Paint paint;

    private Handler handler = new Handler();
    
    private long ANIMATION_TIME = 500;

    public Mogoo_IconFolderAnimation(Context context, Mogoo_BitmapCache iconCache) {
        this.context = context;
        this.iconCache = iconCache;
        paint = new Paint();
        paint.setAntiAlias(true);
    }

    /**
     * 激活图标文件夹开启动画 @ author: 黄悦
     * 
     * @param bubbleText
     */  
    public void activeIconFolder(final Mogoo_BubbleTextView bubbleText) {
    	//update by 袁业奔 2011-9-20
//        Bitmap bitmap = Bitmap.createBitmap(66, 66, Config.ARGB_8888);
    	Bitmap bitmap = Bitmap.createBitmap(Mogoo_GlobalConfig.getIconFolderBgWidth(), Mogoo_GlobalConfig.getIconFolderBgHeight(), Config.ARGB_8888);
    	//end
        Canvas canvas = new Canvas(bitmap);
        ShortcutInfo info = (ShortcutInfo) bubbleText.getTag();
        Bitmap iconFolderBg = null;

        if (bubbleText instanceof Mogoo_FolderBubbleText) {
            bubbleText.stopVibrate();
            Bitmap icon = info.getIcon(iconCache);
            iconFolderBg = Bitmap.createScaledBitmap(icon, bitmap.getWidth(), bitmap.getHeight(),
                    true);
            canvas.drawBitmap(iconFolderBg, 0, 0, paint); 
            canvas.save();
        } else {
            iconFolderBg = iconCache.getBitmap(R.drawable.mogoo_icon_folder);
            Bitmap icon = info.getIcon(iconCache);
//            Bitmap iconTemp = Bitmap.createScaledBitmap(icon, 50, 50, true);
            if(Mogoo_GlobalConfig.LOG_DEBUG){
            	Log.d(TAG, "---------------iconFolderBg Width"+iconFolderBg.getWidth()+"-------");
            	Log.d(TAG, "---------------iconFolderBg Height"+iconFolderBg.getHeight()+"-------");
            	Log.d(TAG, "---------------icon Width"+icon.getWidth()+"-------");
            	Log.d(TAG, "---------------icon Height"+icon.getHeight()+"-------");
            }
            canvas.drawBitmap(iconFolderBg, 0, 0, paint);
            paint.setAlpha(180);
//            canvas.drawBitmap(iconTemp, 8, 8, paint);
            paint.setAlpha(255);

            canvas.save();

//            iconTemp.recycle();
//            iconTemp = null;
            icon = null;
        }

        bubbleText.setCompoundDrawablesWithIntrinsicBounds(null, new BitmapDrawable(bitmap), null,
                null);
        bubbleText.setText(null);
        bubbleText.stopVibrate();

        iconFolderBg.recycle();
        iconFolderBg = null;
        info = null;
        bitmap = null;
        canvas = null;
    }

    /**
     * 离开图标文件夹激活区域释放 @ author: 黄悦
     * 
     * @param bubbleText
     */
    public void cancelIconFolder(final Mogoo_BubbleTextView bubbleText) {
        if (bubbleText == null) {
            return;
        }

        ShortcutInfo info = (ShortcutInfo) bubbleText.getTag();
        iconCache.recycle(info.getIntent().getComponent(), Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
//        iconCache.remove(info.getIntent().getComponent());
        bubbleText.setCompoundDrawablesWithIntrinsicBounds(null,
                new FastBitmapDrawable(info.getIcon(iconCache)), null, null);
        bubbleText.setText(info.getTitle());

        bubbleText.startVibrate(iconCache, 0);
        info = null;
    }

    /**
     * 打开文件夹动画 @ author: 黄悦
     * 
     * @param bottomImage
     * @param moveDis
     */
    public void openFolderAnimation(final ImageView topImage, int moveTop, final ImageView bottomImage,
            int moveBottom, final Mogoo_BubbleTextView iconView) {
        TranslateAnimation goUpAnimation = new TranslateAnimation(0, 0, moveTop, 0);
        goUpAnimation.setFillAfter(false);
        goUpAnimation.setDuration(ANIMATION_TIME);
        goUpAnimation.setAnimationListener(new AnimationListener() {
            public void onAnimationStart(Animation animation) {
                handler.postDelayed(new Runnable() {
                    public void run() {
                        if(iconView.getParent() instanceof CellLayout){
                            drawTrigona(topImage, iconView, false);
                        }
                    }
                }, 100);
            } 

            public void onAnimationEnd(Animation animation) {
                topImage.clearAnimation();
                Mogoo_FolderBubbleText.folderOpening = false;
            }

            public void onAnimationRepeat(Animation animation) {
            }
        });
        topImage.startAnimation(goUpAnimation);

        TranslateAnimation goDownAnimation = new TranslateAnimation(0, 0, -moveBottom, 0);
        goDownAnimation.setFillAfter(false);
        goDownAnimation.setDuration(ANIMATION_TIME);
        goDownAnimation.setAnimationListener(new AnimationListener() {
            public void onAnimationStart(Animation animation) {
                handler.postDelayed(new Runnable() {
                    public void run() {
                        if(iconView.getParent() instanceof Mogoo_DockWorkSpace){
                            drawTrigona(bottomImage, iconView, true);
                        }
                    }
                }, 100);
            }

            public void onAnimationEnd(Animation animation) {
                bottomImage.clearAnimation();
            }

            public void onAnimationRepeat(Animation animation) {
            }
        });
        bottomImage.startAnimation(goDownAnimation);
    }
        
    private void drawTrigona(final ImageView image,
                final Mogoo_BubbleTextView iconView, boolean isReverse) {
            Drawable drawable = image.getDrawable();
            Mogoo_ComponentBus bus = Mogoo_ComponentBus.getInstance();
            
            bus.getActivityComp(R.id.workspace, context).setVisibility(View.INVISIBLE);
//                        bus.getActivityComp(R.id.dockWorkSpace, context).setVisibility(View.INVISIBLE);
            
            if (drawable instanceof BitmapDrawable) {
                Mogoo_BitmapCache bitmapCache = ((LauncherApplication) (context
                        .getApplicationContext())).getIconCache();
                Canvas canvas = new Canvas(((BitmapDrawable) drawable).getBitmap());
                Bitmap trigona = null;
                if(isReverse){
                    trigona = bitmapCache.getBitmap(R.drawable.mogoo_folder_trigona_r);
                    canvas.drawBitmap(trigona, iconView.getLeft() + (iconView.getWidth() - trigona.getWidth()) / 2,
                            0, paint);
                }else{
                    trigona = bitmapCache.getBitmap(R.drawable.mogoo_folder_trigona);
                    canvas.drawBitmap(trigona, iconView.getLeft() + (iconView.getWidth() - trigona.getWidth()) / 2,
                            (drawable.getIntrinsicHeight() - trigona.getHeight()), paint);
                }
                
                canvas.save();
                canvas = null;
                bitmapCache = null;
            }
    }
    

    /**
     * 关闭文件夹动画 @ author: 黄悦
     * 
     * @param topImage
     * @param moveTop
     * @param bottomImage
     * @param moveBottom
     */
    public void closeFolderAnimation(final ImageView topImage, int moveTop,
            final ImageView bottomImage, int moveBottom) {
        final Mogoo_ComponentBus bus = Mogoo_ComponentBus.getInstance();
        TranslateAnimation goDownAnimation = new TranslateAnimation(0, 0, 0, moveTop);
        goDownAnimation.setAnimationListener(new AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }
            public void onAnimationEnd(Animation animation) {
                bus.getActivityComp(R.id.folderLayer, topImage.getContext()).setVisibility(
                        View.GONE);
                final Mogoo_FolderWorkspace folderWorkspace = (Mogoo_FolderWorkspace) bus.getActivityComp(R.id.folderWorkspace, topImage.getContext());
                Mogoo_FolderController folderController = folderWorkspace.getLauncher().getFolderController();
                folderController.iconFolderInactive();
                folderController.setTempActiveIcon(null);
                folderWorkspace.setLoadingFolder(null);
                
                View v = null;
                int count = folderWorkspace.getChildCount();
                
                for(int i = 0; i < count; i ++){
                    v = folderWorkspace.getChildAt(i);
                    if(v instanceof Mogoo_BubbleTextView){
                        ((Mogoo_BubbleTextView)v).stopVibrate(iconCache);
                    }
                }
                
                folderWorkspace.clearViews();
                folderWorkspace.setVisibility(View.INVISIBLE);
                handler.postDelayed(new Runnable() {
                    public void run() {
                        clearImage(topImage);
                        clearImage(bottomImage);
                        RelativeLayout folderLayerCenter = (RelativeLayout) Mogoo_ComponentBus.getInstance().getActivityComp(R.id.folderLayerCenter, context);
                        Mogoo_FolderLayout folderLayer = (Mogoo_FolderLayout)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.folderLayer, context);
                        LayoutParams lpCenter = (LayoutParams) folderLayerCenter.getLayoutParams();
                        lpCenter.topMargin = 0;
                        folderLayer.invalidate();
                        Mogoo_FolderBubbleText.isOpen = false;
                    }
                }, 500);
            }

            public void onAnimationRepeat(Animation animation) {
            }
        });
        goDownAnimation.setDuration(ANIMATION_TIME);
        goDownAnimation.setFillAfter(true);
        topImage.startAnimation(goDownAnimation);

        TranslateAnimation goUpAnimation = new TranslateAnimation(0, 0, 0, -moveBottom);
        goUpAnimation.setDuration(ANIMATION_TIME);
        goUpAnimation.setFillAfter(true);
        bottomImage.startAnimation(goUpAnimation);
    }

    private void clearImage(final ImageView view) {
        Drawable drawable = view.getDrawable();
        if (drawable instanceof BitmapDrawable) {
            ((BitmapDrawable) drawable).getBitmap().recycle();
            view.setImageDrawable(null);
        }
        view.clearAnimation();
    }

}
