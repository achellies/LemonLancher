package com.limemobile.app.launcher.wp8;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.limemobile.app.launcher.activity.Launcher;
import com.limemobile.app.launcher.anim.Rotate3d;
import com.limemobile.app.launcher.entity.ApplicationInfo;
import com.limemobile.app.launcher.receiver.LauncherModel;
import com.limemobile.app.launcher.util.PinyinUtil;
import com.limemobile.app.launcher.view.AllAppsView;
import com.limemobile.app.launcher.view.DragController;
import com.limemobile.app.launcher.view.DragSource;
import com.limemobile.app.launcher.view.LetterFilterView;
import com.limemobile.app.launcher.view.LetterFilterView.OnTouchingLetterChangedListener;
import com.limemobile.app.launcher.view.SearchAppsView;
import com.limemobile.app.launcher.wp8.AllAppsLetterFilter.AppsAdapter.ViewHolder;

public class AllAppsLetterFilter extends RelativeLayout implements AllAppsView,
		AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener,
		View.OnKeyListener, DragSource, OnTouchingLetterChangedListener, OnClickListener {
    
    private static final String TAG = "Launcher.AllAppsLetterFilter";
    private static final boolean DEBUG = false;

    private Launcher mLauncher;
    private DragController mDragController;

    private SearchAppsView mSearchView;
    private GridView mLetterGrid;
    private ListView mList;
    private LetterFilterView mLetterFilter;
    private TextView mLetterIndicator;
    
    private OverlayThread mOverlayThread;
    
    private ASCIIComparator mASCIIComparator;
    
    private ArrayList<ApplicationInfo> mAllAppsList = new ArrayList<ApplicationInfo>();
    private HashMap<String, Integer> mLetterIndex = new HashMap<String, Integer>();

    // preserve compatibility with 3D all apps:
    //    0.0 -> hidden
    //    1.0 -> shown and opaque
    //    intermediate values -> partially shown & partially opaque
    private float mZoom;

    private AppsAdapter mAppsAdapter;
    private LettersAdapter mLettersAdapter;
    
    private Rotate3d mLeftRotate;
    private Rotate3d mRightRotate;
    //= new Rotate3d(0, -90, centerX, 0);

    // ------------------------------------------------------------
    private class ASCIIComparator implements Comparator<ApplicationInfo> {

        @Override
        public int compare(ApplicationInfo lhs, ApplicationInfo rhs) {
            return lhs.compareTo(rhs);
        }
        
    }
    
    public class AppsAdapter extends ArrayAdapter<ApplicationInfo> {
        private final LayoutInflater mInflater;

        public AppsAdapter(Context context, ArrayList<ApplicationInfo> apps) {
            super(context, 0, apps);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ApplicationInfo info = getItem(position);
            ViewHolder holder = null;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.application_wp8, parent, false);
                holder = new ViewHolder();
                
                holder.letter = (TextView) convertView.findViewById(R.id.letter_indicator);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                holder.name = (TextView) convertView.findViewById(R.id.name);
                holder.version = (TextView) convertView.findViewById(R.id.version);
                convertView.setTag(holder);
            } else
                holder = (ViewHolder) convertView.getTag();

//            if (!info.filtered) {
//                info.icon = Utilities.createIconThumbnail(info.icon, getContext());
//                info.filtered = true;
//            }
            holder.letter.setVisibility(View.GONE);
            holder.letter.setClickable(true);
            if (position == 0) {
                holder.letter.setVisibility(View.VISIBLE);
                holder.letter.setText(PinyinUtil.getFirstLetter(info.ascii));
            } else {
                String currentLetter = PinyinUtil.getFirstLetter(info.ascii);
                String previousLetter = PinyinUtil.getFirstLetter(getItem(position - 1).ascii);
                
                if (!currentLetter.equals(previousLetter)) {
                    holder.letter.setVisibility(View.VISIBLE);
                    holder.letter.setText(currentLetter);
                }
                
                holder.letter.setOnClickListener(AllAppsLetterFilter.this);
            }
            
            info.iconBitmap.setDensity(Bitmap.DENSITY_NONE);
            holder.icon.setImageBitmap(info.iconBitmap);
            holder.name.setText(info.title);
            
            try {
                PackageInfo pi = getContext().getPackageManager().getPackageInfo(info.componentName.getPackageName(), PackageManager.GET_ACTIVITIES);
                holder.version.setText(pi.versionName);
                holder.version.setVisibility(View.VISIBLE);
            } catch (NameNotFoundException e) {
                holder.version.setVisibility(View.GONE);
            }

            return convertView;
        }
        
        class ViewHolder {
            TextView letter;
            ImageView icon;
            TextView name;
            TextView version;
        }
    }
    
    public AllAppsLetterFilter(Context context, AttributeSet attrs) {
        super(context, attrs);
        setVisibility(View.GONE);
        setSoundEffectsEnabled(false);
        setHapticFeedbackEnabled(false);
        
        mASCIIComparator = new ASCIIComparator();

        mAppsAdapter = new AppsAdapter(getContext(), mAllAppsList);
        mAppsAdapter.setNotifyOnChange(false);
        
        mLettersAdapter = new LettersAdapter(getContext(), LetterFilterView.mLetters);
        mLettersAdapter.setNotifyOnChange(false);
        
        mOverlayThread = new OverlayThread();
        
        int centerX = context.getResources().getDisplayMetrics().widthPixels / 2;
        mLeftRotate = new Rotate3d(0, -90, centerX, 0);
        mRightRotate = new Rotate3d(90, 0, centerX, 0);
        mLeftRotate.setFillAfter(false);
        mLeftRotate.setDuration(800);
        mRightRotate.setFillAfter(false);
        mRightRotate.setDuration(800);
    }

    @Override
    protected void onFinishInflate() {
        setBackgroundColor(Color.BLACK);

        try {
            mList = (ListView)findViewWithTag("all_apps_list");
            if (mList == null) throw new Resources.NotFoundException();
            
            mLetterFilter = (LetterFilterView) findViewWithTag("letter_filter");
            if (mLetterFilter == null) throw new Resources.NotFoundException();
            
            mSearchView = (SearchAppsView) findViewWithTag("search_app");
            if (mSearchView == null) throw new Resources.NotFoundException();
            mSearchView.setLauncher(mLauncher);
            mSearchView.setDragController(mDragController);
            mSearchView.setApps(mAllAppsList);
            
            mLetterIndicator = (TextView) findViewWithTag("letter_indicator");
            if (mLetterIndicator == null) throw new Resources.NotFoundException();
            mLetterIndicator.setVisibility(View.GONE);
            
            mList.setOnItemClickListener(this);
            mList.setOnItemLongClickListener(this);
//            mList.setBackgroundColor(Color.BLACK);
//            mList.setCacheColorHint(Color.BLACK);
            
            mLetterFilter.setOnTouchingLetterChangedListener(this);
            
            mLetterGrid = (GridView) findViewWithTag("letter_grid");
            if (mLetterGrid == null) throw new Resources.NotFoundException();
            mLetterGrid.setVisibility(View.GONE);
            
            ImageButton searchBtn = (ImageButton) findViewById(R.id.wp8_search);
            ImageButton menuBtn = (ImageButton) findViewById(R.id.wp8_menu);
            ImageButton arrowBtn = (ImageButton) findViewById(R.id.wp8_arrow);
            
            searchBtn.setOnClickListener(this);
            menuBtn.setOnClickListener(this);
            arrowBtn.setOnClickListener(this);
        } catch (Resources.NotFoundException e) {
        	Log.e(TAG, "Can't find necessary layout elements for AllAppsLetterFilter");
        }

        setOnKeyListener(this);
    }

    public void setLauncher(Launcher launcher) {
        mLauncher = launcher;
        
        if (mSearchView != null)
            mSearchView.setLauncher(mLauncher);
    }

    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (!isVisible()) return false;

        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                if (mLetterGrid.getVisibility() == View.VISIBLE) {
                    mLetterGrid.setVisibility(View.GONE);
                    return true;
                }
                if (mSearchView.getVisibility() == View.VISIBLE) {
                	mSearchView.clearAnimation();
                    mSearchView.startAnimation(mLeftRotate);
                    mLeftRotate.setAnimationListener(new AnimationListener () {

                        @Override
                        public void onAnimationStart(Animation animation) {
                        }

                        @Override
                        public void onAnimationEnd(Animation animation) {
                        	mSearchView.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationRepeat(Animation animation) {
                        }
                        
                    });
                    return true;
                }
                mLauncher.closeAllApps(true);
                return false;
            default:
                return false;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
        mLauncher.startActivitySafely(app.intent, app);
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (!view.isInTouchMode()) {
            return false;
        }
        
        int visibility = View.VISIBLE;
        try {
            AppsAdapter.ViewHolder holder = (ViewHolder) view.getTag();
            visibility = holder.letter.getVisibility();
            holder.letter.setVisibility(View.GONE);
        } catch (ClassCastException e) {
        }

        ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
        app = new ApplicationInfo(app);

        mDragController.startDrag(view, this, app, DragController.DRAG_ACTION_COPY);
        
        try {
            if (visibility == View.VISIBLE) {
                AppsAdapter.ViewHolder holder = (ViewHolder) view.getTag();
                holder.letter.setVisibility(View.VISIBLE);
            }
        } catch (ClassCastException e) {
        }
        
        mLauncher.closeAllApps(true);

        return true;
    }

    protected void onFocusChanged(boolean gainFocus, int direction, android.graphics.Rect prev) {
        if (gainFocus) {
            mList.requestFocus();
        }
    }

    public void setDragController(DragController dragger) {
        mDragController = dragger;
        
        if (mSearchView != null)
            mSearchView.setDragController(mDragController);
    }

    public void onDropCompleted(View target, boolean success) {
    }

    /**
     * Zoom to the specifed level.
     *
     * @param zoom [0..1] 0 is hidden, 1 is open
     */
    public void zoom(float zoom, boolean animate) {
//        Log.d(TAG, "zooming " + ((zoom == 1.0) ? "open" : "closed"));
        cancelLongPress();

        mZoom = zoom;

        if (isVisible()) {
            getParent().bringChildToFront(this);
            setVisibility(View.VISIBLE);
            mList.setAdapter(mAppsAdapter);
            mLetterGrid.setAdapter(mLettersAdapter);
            if (animate) {
                startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.all_apps_2d_fade_in));
            } else {
                onAnimationEnd();
            }
        } else {
            if (animate) {
                startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.all_apps_2d_fade_out));
            } else {
                onAnimationEnd();
            }
        }
    }

    protected void onAnimationEnd() {
        if (!isVisible()) {
            setVisibility(View.GONE);
            mLetterGrid.setVisibility(View.GONE);
            mSearchView.setVisibility(View.GONE);
            mList.setAdapter(null);
            mLetterGrid.setAdapter(null);
            mZoom = 0.0f;
        } else {
            mZoom = 1.0f;
        }

        mLauncher.zoomed(mZoom);
    }

    public boolean isVisible() {
        return mZoom > 0.001f;
    }

    @Override
    public boolean isOpaque() {
        return mZoom > 0.999f;
    }

    public void setApps(ArrayList<ApplicationInfo> list) {
        mAllAppsList.clear();
        addApps(list);
        
        if (mSearchView != null)
            mSearchView.addApps(mAllAppsList);
    }

    public void addApps(ArrayList<ApplicationInfo> list) {
//        Log.d(TAG, "addApps: " + list.size() + " apps: " + list.toString());

//        final int N = list.size();
//
//        for (int i=0; i<N; i++) {
//            final ApplicationInfo item = list.get(i);
//            int index = Collections.binarySearch(mAllAppsList, item,
//                    LauncherModel.APP_NAME_COMPARATOR);
//            if (index < 0) {
//                index = -(index+1);
//            }
//            mAllAppsList.add(index, item);
//        }
    	for (ApplicationInfo info : list)
    		mAllAppsList.add(info);
        Collections.sort(mAllAppsList, mASCIIComparator);
        
        mLetterIndex.clear();
        String previousLetter = null;
        for (int index = 0; index < mAllAppsList.size(); ++index) {
            if (previousLetter == null || !previousLetter.equals(PinyinUtil.getFirstLetter(mAllAppsList.get(index).ascii))) {
                previousLetter = PinyinUtil.getFirstLetter(mAllAppsList.get(index).ascii);
                mLetterIndex.put(previousLetter, index);
            }
        }
        mAppsAdapter.notifyDataSetChanged();
    }

    public void removeApps(ArrayList<ApplicationInfo> list) {
        final int N = list.size();
        for (int i=0; i<N; i++) {
            final ApplicationInfo item = list.get(i);
            int index = findAppByComponent(mAllAppsList, item);
            if (index >= 0) {
                mAllAppsList.remove(index);
            } else {
            	if (DEBUG)
            		Log.w(TAG, "couldn't find a match for item \"" + item + "\"");
                // Try to recover.  This should keep us from crashing for now.
            }
        }
        mLetterIndex.clear();
        String previousLetter = null;
        for (int index = 0; index < mAllAppsList.size(); ++index) {
            if (previousLetter == null || !previousLetter.equals(PinyinUtil.getFirstLetter(mAllAppsList.get(index).ascii))) {
                previousLetter = PinyinUtil.getFirstLetter(mAllAppsList.get(index).ascii);
                mLetterIndex.put(previousLetter, index);
            }
        }
        mAppsAdapter.notifyDataSetChanged();
    }

    public void updateApps(ArrayList<ApplicationInfo> list) {
        // Just remove and add, because they may need to be re-sorted.
        removeApps(list);
        addApps(list);
        
        if (mSearchView != null)
            mSearchView.addApps(mAllAppsList);
    }

    private static int findAppByComponent(ArrayList<ApplicationInfo> list, ApplicationInfo item) {
        ComponentName component = item.intent.getComponent();
        final int N = list.size();
        for (int i=0; i<N; i++) {
            ApplicationInfo x = list.get(i);
            if (x.intent.getComponent().equals(component)) {
                return i;
            }
        }
        return -1;
    }

    public void dumpState() {
        ApplicationInfo.dumpApplicationInfoList(TAG, "mAllAppsList", mAllAppsList);
    }
    
    public void surrender() {
    }

    @Override
    public void onTouchingLetterChanged(String s) {
        if (mLetterIndicator != null) {
            mLetterIndicator.setText(s);
            mLetterIndicator.setVisibility(View.VISIBLE);
            mLetterIndicator.removeCallbacks(mOverlayThread);
            mLetterIndicator.postDelayed(mOverlayThread, 1500);
            
            if (mLetterIndex.containsKey(s.toLowerCase()))
                mList.setSelection(mLetterIndex.get(s.toLowerCase()));
        }
    }
    
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.letter_indicator) {
            mLetterGrid.startLayoutAnimation();
            mLetterGrid.setVisibility(View.VISIBLE);
        } else if (v instanceof TextView && v.getTag() != null && v.getTag() instanceof String) {
            String letter = (String) v.getTag();
            if (mLetterIndex.containsKey(letter.toLowerCase())) {
                mLetterGrid.setVisibility(View.GONE);
                mList.setSelection(mLetterIndex.get(letter.toLowerCase()));
            }
        } else if (v.getId() == R.id.wp8_search) {
        	mSearchView.clearAnimation();
            mSearchView.startAnimation(mRightRotate);
            mSearchView.setVisibility(View.VISIBLE);
        } else if (v.getId() == R.id.wp8_menu) {
            //mLauncher.prepareExpandedMenu();
            mLauncher.changeAllAppMode(true);
            mLauncher.closeAllApps(true);
            mLauncher.showAllApps(true);
        } else if (v.getId() == R.id.wp8_arrow) {
            mLauncher.closeAllApps(true);
        }
    }
    
    private class OverlayThread implements Runnable {

        @Override
        public void run() {
            if (mLetterIndicator != null)
                mLetterIndicator.setVisibility(View.GONE);
        }

    }
    
    public class LettersAdapter extends ArrayAdapter<String> {

        public LettersAdapter(Context context, String[] objects) {
            super(context, 0, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final String letter = getItem(position).toLowerCase();
            if (convertView == null) {
                float density = 1.0f;//getContext().getResources().getDisplayMetrics().density;
                convertView = new TextView(getContext());
                float width = getContext().getResources().getDimension(R.dimen.letter_tile_size);
                convertView.setLayoutParams(new GridView.LayoutParams(LayoutParams.FILL_PARENT, (int)(width * density)));
                ((TextView)convertView).setTextColor(Color.WHITE);
                ((TextView)convertView).setTextSize(36.0f);
                ((TextView)convertView).setGravity(Gravity.LEFT | Gravity.BOTTOM);
                ((TextView)convertView).setPadding(5, 5, 5, 5);
                mLetterGrid.setColumnWidth((int)(width * density));
            }
            
            convertView.setTag(letter);
            ((TextView)convertView).setText(letter);
            if (mLetterIndex.containsKey(letter)) {
                convertView.setBackgroundColor(Color.BLUE);
                convertView.setClickable(true);
                convertView.setOnClickListener(AllAppsLetterFilter.this);
            }
            else {
                convertView.setBackgroundColor(Color.BLACK);
                convertView.setClickable(false);
                convertView.setOnClickListener(null);
            }
            
            return convertView;
        }
        
    }

	@Override
	public MenuType getOptionsMenuType() {
		return MenuType.ExpendedMenu;
	}
}
