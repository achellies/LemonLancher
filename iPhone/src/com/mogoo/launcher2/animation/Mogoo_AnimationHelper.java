/**  
 * 文 件 名:  AnimationHelper.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：  黄悦                     
 * 版    本:  1.0  
 * 创建时间:   2011-2-16
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-2-16        黄悦       1.0          1.0 Version  
 */

package com.mogoo.launcher2.animation;

import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;

import com.mogoo.launcher2.CellEntry;
import com.mogoo.launcher2.CellEntry.CellEntryInface;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;

public class Mogoo_AnimationHelper {
    private static final int PLAY_TIMEOUT = 1000;

    private static final long ANIMATION_TIME = 300;

    private DataSetObservable playObservable;

    private int count = 0;

    private long recordTime;
    
    private final static int BUFF_DELAY = 30;

    public Mogoo_AnimationHelper() {
        playObservable = new DataSetObservable();
    }

    /**
     * 注册观察接收端 @ author: 黄悦
     * 
     * @param observer
     */
    public void registerObserver(DataSetObserver observer) {
        playObservable.registerObserver(observer);
    }

    /**
     * 注销观察接收端 @ author: 黄悦
     * 
     * @param observer
     */
    public void unregisterObserver(DataSetObserver observer) {
        playObservable.unregisterObserver(observer);
    }

    /**
     * 播放标志返回 @ author: 黄悦
     * 
     * @return 返回true为正在播放，false为空闲
     */
    public boolean isPlaying() {
        if ((System.currentTimeMillis() - recordTime > PLAY_TIMEOUT)) {
            count = 0;
        }

        return count != 0;
    }

    /**
     * 播放组件内指定范围的图标移动动画 @ author: 黄悦
     * 
     * @param parent
     * @param startIndex
     * @param endIndex
     * @return 返回AnimationSet为可以开始播放,null为有动画在播放
     */
    public AnimationSet setAnimations(ViewGroup parent, int startIndex, int endIndex, int moveOffset) {
        count = Math.abs(endIndex - startIndex);
        AnimationSet animationSet = new AnimationSet(false);

        if (Mogoo_GlobalConfig.PLAY_ANIMATION) {
            Animation animationTemp = null;

            if (Mogoo_GlobalConfig.PLAY_ANIMATION) {
                for (int i = startIndex; i <= endIndex; i++) {
                    animationTemp = animationSetting(parent, i, i + moveOffset);
                    
                    if (animationTemp != null) {
                        animationSet.addAnimation(animationTemp);
                    } else {
                        return null;
                    }
                }
            }

            animationTemp = null;
        }

        return animationSet;
    }

    /**
     * 播放组件内指定范围的图标移动动画 @ author: 黄悦
     * 
     * @param parent
     * @param startIndex
     * @param endIndex
     * @return 返回AnimationSet为可以开始播放,null为有动画在播放
     */
    public AnimationSet setAnimations(ViewGroup parent, int startIndex, int endIndex) {
        if (startIndex == endIndex || isPlaying()) {
            return null;
        }

        count = Math.abs(endIndex - startIndex);
        AnimationSet animationSet = null;

        if (isGoFront(startIndex, endIndex)) {
            animationSet = setAnimations(parent, startIndex, endIndex, true);
        } else {
            animationSet = setAnimations(parent, endIndex, startIndex, false);
        }

        return animationSet;
    }

    // 遍历设定动画
    private AnimationSet setAnimations(ViewGroup parent, int startIndex, int endIndex,
            boolean isGoFront) {
        AnimationSet animationSet = new AnimationSet(false);

        if (Mogoo_GlobalConfig.PLAY_ANIMATION) {
            Animation animationTemp = null;
            for (int i = startIndex; i < endIndex; i++) {
                if (isGoFront) {
                    animationTemp = animationSetting(parent, i + 1, i);
                } else {
                    animationTemp = animationSetting(parent, i, i + 1);
                }
                if (animationTemp != null) {
                    animationSet.addAnimation(animationTemp);
                } else {
                    return null;
                }
            }

            animationTemp = null;
        }

        return animationSet;
    }
    

    // 设定图标动画
    private Animation animationSetting(ViewGroup parent, int srcIndex, int targetIndex) {
        if (srcIndex < 0 || srcIndex >= parent.getChildCount() || targetIndex < 0 || targetIndex >= parent.getChildCount()) {
            return null;
        }

        final View srcView = parent.getChildAt(srcIndex);
        CellEntry targetCellEntry = null;
        CellEntry srcCellEntry = null;
        
        if(parent instanceof CellEntryInface){
            targetCellEntry = ((CellEntryInface)parent).getCellEntry(targetIndex);
            srcCellEntry = ((CellEntryInface)parent).getCellEntry(srcIndex);
        }
        
        if(targetCellEntry == null || srcCellEntry == null){
            return null;
        }
        
        float x = targetCellEntry.left - srcCellEntry.left;
        float y = targetCellEntry.top - srcCellEntry.top;

        TranslateAnimation animation = new TranslateAnimation(-x, 0, -y, 0);

        animation.setDuration(ANIMATION_TIME + srcIndex * BUFF_DELAY);
        animation.setFillAfter(true);
        animation.setFillBefore(true);
        animation.setAnimationListener(new AnimationListener() {
            public void onAnimationStart(Animation animation) {
            }

            public void onAnimationEnd(Animation animation) {
                count--;
                srcView.clearAnimation();
                if (!isPlaying()) {
                    playObservable.notifyChanged();
                }
            }

            public void onAnimationRepeat(Animation animation) {
            }
        });

        srcView.setAnimation(animation);

        return animation;
    }

    // 判断图标动画走向
    private boolean isGoFront(int startIndex, int endIndex) {
        return endIndex > startIndex;
    }

}
