package com.android.launcher;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.TransitionDrawable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class PreviewPager extends ViewGroup {
	private int mTotalItems;
	private int mCurrentItem;
	private int mDotDrawableId;
	
	public PreviewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		initPager();
	}

	public PreviewPager(Context context) {
		super(context);
		initPager();
		// TODO Auto-generated constructor stub
	}
	private void initPager(){
		setFocusable(false);
		setWillNotDraw(false);
		mDotDrawableId=R.drawable.pager_dots;
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		if(mTotalItems<=0) return;
		createLayout();
	}
	private void updateLayout(){
		for(int i=0;i<getChildCount();i++){
			final ImageView img=(ImageView) getChildAt(i);
			TransitionDrawable tmp=(TransitionDrawable)img.getDrawable();
			if(i==mCurrentItem){
				tmp.startTransition(50);
			}else{
				tmp.resetTransition();
			}
		}
	}
	private void createLayout(){
		detachAllViewsFromParent();
    	//ADW: Load the specified theme
    	String themePackage=AlmostNexusSettingsHelper.getThemePackageName(getContext(), Launcher.THEME_DEFAULT);
    	PackageManager pm=getContext().getPackageManager();
    	Resources themeResources=null;
    	if(!themePackage.equals(Launcher.THEME_DEFAULT)){
	    	try {
				themeResources=pm.getResourcesForApplication(themePackage);
			} catch (NameNotFoundException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();
			}
    	}
    	int resource_id=0;
		if(themeResources!=null){
			resource_id=themeResources.getIdentifier ("pager_dots", "drawable", themePackage);
		}
		
		int dotWidth=getResources().getDrawable(mDotDrawableId).getIntrinsicWidth();
		int separation=dotWidth;
		int marginLeft=((getWidth())/2)-(((mTotalItems*dotWidth)/2)+(((mTotalItems-1)*separation)/2));
		int marginTop=((getHeight())/2)-(dotWidth/2);
		for(int i=0;i<mTotalItems;i++){
			ImageView dot=new ImageView(getContext());
			TransitionDrawable td;
			if(themeResources!=null && resource_id!=0){
				td=(TransitionDrawable)themeResources.getDrawable(resource_id);
			}else{
				td=(TransitionDrawable)getResources().getDrawable(mDotDrawableId);
			}
			td.setCrossFadeEnabled(true);
			dot.setImageDrawable(td);
	        ViewGroup.LayoutParams p;
	        p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
	        		ViewGroup.LayoutParams.FILL_PARENT);
            dot.setLayoutParams(p);
            int childHeightSpec = getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(dotWidth, MeasureSpec.UNSPECIFIED), 0, p.height);
            int childWidthSpec = getChildMeasureSpec(
                    MeasureSpec.makeMeasureSpec(dotWidth, MeasureSpec.EXACTLY), 0, p.width);
            dot.measure(childWidthSpec, childHeightSpec);
			
            int left=marginLeft+(i*(dotWidth+separation));
            
            
			dot.layout(left, marginTop, left+dotWidth,marginTop+dotWidth );
            addViewInLayout(dot, getChildCount(), p, true);
            if(i==mCurrentItem){
            	TransitionDrawable tmp=(TransitionDrawable)dot.getDrawable();
            	tmp.startTransition(200);
            }
		}
		postInvalidate();
	}
	protected int getTotalItems() {
		return mTotalItems;
	}

	protected void setTotalItems(int totalItems) {
		if(totalItems!=mTotalItems){
			this.mTotalItems = totalItems;
			createLayout();
		}
	}

	protected int getCurrentItem() {
		return mCurrentItem;
	}

	protected void setCurrentItem(int currentItem) {
		if(currentItem!=mCurrentItem){
			this.mCurrentItem = currentItem;
			updateLayout();
		}
	}
	
//    public void setLeft(int value) {
//        int width = this.getWidth();
//        this.mLeft = value;
//        this.mRight = this.mLeft + width;
//    }
}
