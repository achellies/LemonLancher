/**  
 * 文 件 名:  DragHelper.java  
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

package com.mogoo.launcher2.utils;

import com.mogoo.launcher2.CellLayout;
import com.mogoo.launcher2.CellLayout.LayoutParams;
import com.mogoo.launcher2.animation.Mogoo_AnimationHelper;

import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationSet;

public class Mogoo_DragHelper {
    private Mogoo_AnimationHelper animationHelper;
    
//    private ViewGroup lastParent;
//    private int lastStartIndex;
//    private int lastEndIndex;
    
    private AnimationDataSetObserver dataSetObserve;
    
    
    public Mogoo_DragHelper(){
        animationHelper = new Mogoo_AnimationHelper();
        dataSetObserve = new AnimationDataSetObserver();
        animationHelper.registerObserver(dataSetObserve);
    }
    
    /**
     * 
     * 对排序数据进行实现操作
     * @ author: 黄悦
     *@param parent
     *@param startIndex
     *@param endIndex
     *@return
     */
    public boolean sortView(ViewGroup parent, int startIndex, int endIndex){
        if(parent == null || sortFilter(parent, startIndex, endIndex)){
            return false;
        }

        AnimationSet playHandle = animationHelper.setAnimations(parent, startIndex, endIndex);
        
        if(playHandle == null){
//            setSortData(parent, startIndex, endIndex);
            return false;
        }
        
        sort(parent, startIndex, endIndex);
        playHandle.start();
        
        playHandle = null;
        
        return true;
    }
    
    public View moveView(ViewGroup parent, int startIndex, int endIndex, int moveOffset){
        if(parent == null || sortFilter(parent, startIndex, endIndex)){
            return null;
        }
        
        View view = parent.getChildAt(endIndex + 1);
        if(view == null){
            return null;
        }
        
        AnimationSet playHandle = animationHelper.setAnimations(parent, startIndex, endIndex, moveOffset);
        
        if(playHandle == null){
            setSortData(parent, startIndex, endIndex);
            return null;
        }
        
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams)parent.getChildAt(endIndex).getLayoutParams();
        CellLayout.LayoutParams lp1 = null;
        for(int i = startIndex; i <= endIndex; i++){
            lp = (CellLayout.LayoutParams)parent.getChildAt(i).getLayoutParams();
            lp1 = (CellLayout.LayoutParams)parent.getChildAt(i + moveOffset).getLayoutParams();
            lp.cellX = lp1.cellX;
            lp.cellY = lp1.cellY;
        }
        
        parent.removeView(view);
        playHandle.start();
        playHandle = null;
        lp = null;
        lp1 = null;
        
        return view;
    }
    
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        animationHelper.unregisterObserver(dataSetObserve);
        animationHelper = null;
        dataSetObserve = null;
    }

    public void sort(ViewGroup parent, int startIndex,  int endIndex) {
        View view = resetCellLayout(parent, startIndex, endIndex);
        
        if(view == null){
            return;
        }
        
        parent.removeView(view);
        parent.addView(view, endIndex);
        
        parent.invalidate();
    }
    
    private View resetCellLayout(ViewGroup parent, int startIndex, int endIndex) {
        View dragView = parent.getChildAt(startIndex);
        
        if(dragView == null){
            return null;
        }
        
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams)parent.getChildAt(endIndex).getLayoutParams();
        CellLayout.LayoutParams lp1 = null;
        int x = lp.cellX;
        int y = lp.cellY;
        
        if(endIndex > startIndex){
            for(int i = endIndex; i > startIndex; i--){
                lp = (CellLayout.LayoutParams)parent.getChildAt(i).getLayoutParams();
                lp1 = (CellLayout.LayoutParams)parent.getChildAt(i - 1).getLayoutParams();
                lp.cellX = lp1.cellX;
                lp.cellY = lp1.cellY;
            }
        }else{
            for(int i = endIndex; i < startIndex; i++){
                lp = (CellLayout.LayoutParams)parent.getChildAt(i).getLayoutParams();
                lp1 = (CellLayout.LayoutParams)parent.getChildAt(i + 1).getLayoutParams();
                lp.cellX = lp1.cellX;
                lp.cellY = lp1.cellY;
            }
        }
        
        lp = (CellLayout.LayoutParams)dragView.getLayoutParams();
        lp.cellX = x;
        lp.cellY = y;
        
        lp = null;
        lp1 = null;
        
        return dragView;
    }
    
    private boolean sortFilter(ViewGroup parent, int startIndex, int endIndex){
        if(parent.getChildCount() < startIndex || parent.getChildCount() < endIndex){
            return true;
        }
        
        if(startIndex < 0 || endIndex < 0){
            return true;
        }
        return false;
    }
    
    //add by huangyue
    //观察接收类
    private class AnimationDataSetObserver extends DataSetObserver{
        private ViewGroup parent;
        private int startIndex;
        private int endIndex;
        
        public void onChanged() {
            sortView(parent, startIndex, endIndex);
            clear();
        }
        
        private void clear(){
            parent = null;
            startIndex = 0;
            endIndex = 0;
        }
    }
    //end
    
    /**
     * 缓存排序信息
     * @author 黄悦
     */
    private void setSortData(ViewGroup parent, int startIndex, int endIndex){
        dataSetObserve.parent = parent;
        dataSetObserve.startIndex = startIndex;
        dataSetObserve.endIndex = endIndex;
    }
}
