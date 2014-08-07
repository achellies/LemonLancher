package com.limemobile.app.launcher.view;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.limemobile.app.launcher.activity.Launcher;
import com.limemobile.app.launcher.entity.ApplicationInfo;
import com.limemobile.app.launcher.receiver.LauncherModel;
import com.limemobile.app.launcher.wp8.R;

import java.util.ArrayList;
import java.util.Collections;

public class SearchAppsView extends LinearLayout implements OnItemClickListener, OnItemLongClickListener, DragSource, TextWatcher, OnClickListener {
    private static final String TAG = "Launcher.AllAppsLetterFilter";
    private static final boolean DEBUG = false;

    private Launcher mLauncher;
    private DragController mDragController;

    private ListView mList;
    private EditText mSearchInput;
    private TextView mPlayStoreSearch;
    
    private ArrayList<ApplicationInfo> mAllAppsList = new ArrayList<ApplicationInfo>();
    private ArrayList<ApplicationInfo> mSearchedAppsList = new ArrayList<ApplicationInfo>();

    private AppsAdapter mAppsAdapter;
    
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
                convertView = mInflater.inflate(R.layout.application_list, parent, false);
                holder = new ViewHolder();
                
                holder.description = (TextView) convertView.findViewById(R.id.description);
                holder.icon = (ImageView) convertView.findViewById(R.id.icon);
                holder.name = (TextView) convertView.findViewById(R.id.name);
                convertView.setTag(holder);
            } else
                holder = (ViewHolder) convertView.getTag();

            holder.description.setVisibility(View.GONE);            
            info.iconBitmap.setDensity(Bitmap.DENSITY_NONE);
            holder.icon.setImageBitmap(info.iconBitmap);
            holder.name.setText(info.title);

            return convertView;
        }
        
        class ViewHolder {
            ImageView icon;
            TextView name;
            TextView description;
        }
    }
    
    public SearchAppsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setVisibility(View.GONE);
        setSoundEffectsEnabled(false);
        setHapticFeedbackEnabled(false);

        mAppsAdapter = new AppsAdapter(getContext(), mSearchedAppsList);
        mAppsAdapter.setNotifyOnChange(false);
    }

    @Override
    protected void onFinishInflate() {
        setBackgroundColor(Color.BLACK);

        try {
            mList = (ListView)findViewWithTag("search_app_list");
            if (mList == null) throw new Resources.NotFoundException();
            
            mSearchInput = (EditText) findViewWithTag("searchString");
            if (mSearchInput == null) throw new Resources.NotFoundException();
            
            mSearchInput.addTextChangedListener(this);
            
            mPlayStoreSearch = (TextView) findViewById(R.id.playStoreSearch);
            mPlayStoreSearch.setVisibility(View.GONE);
            mPlayStoreSearch.setOnClickListener(this);
            
            mList.setOnItemClickListener(this);
            mList.setOnItemLongClickListener(this);
            mList.setBackgroundColor(Color.BLACK);
            mList.setCacheColorHint(Color.BLACK);
            
            mList.setAdapter(mAppsAdapter);
        } catch (Resources.NotFoundException e) {
            if (DEBUG)
                Log.e(TAG, "Can't find necessary layout elements for AllAppsLetterFilter");
        }
    }

    public void setLauncher(Launcher launcher) {
        mLauncher = launcher;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
        if (mLauncher != null)
            mLauncher.startActivitySafely(app.intent, app);
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (!view.isInTouchMode()) {
            return false;
        }

        ApplicationInfo app = (ApplicationInfo) parent.getItemAtPosition(position);
        app = new ApplicationInfo(app);

        if (mDragController != null)
            mDragController.startDrag(view, this, app, DragController.DRAG_ACTION_COPY);
        
        if (mLauncher != null)
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
    }

    public void onDropCompleted(View target, boolean success) {
    }

    public void setApps(ArrayList<ApplicationInfo> list) {
        mAllAppsList.clear();
        addApps(list);
    }

    public void addApps(ArrayList<ApplicationInfo> list) {
//        Log.d(TAG, "addApps: " + list.size() + " apps: " + list.toString());

        final int N = list.size();

        for (int i=0; i<N; i++) {
            final ApplicationInfo item = list.get(i);
            int index = Collections.binarySearch(mAllAppsList, item,
                    LauncherModel.APP_NAME_COMPARATOR);
            if (index < 0) {
                index = -(index+1);
            }
            mAllAppsList.add(index, item);
        }
        updateSearchResult(mSearchInput.getText().toString());
        mAppsAdapter.notifyDataSetChanged();
    }
    
    private void updateSearchResult(String seachText) {
        mSearchedAppsList.clear();
        
        for (ApplicationInfo info : mAllAppsList) {
            if (TextUtils.isEmpty(seachText) || info.title.toString().toLowerCase().contains(seachText.toLowerCase()))
                mSearchedAppsList.add(info);
        }
        
        if (mSearchedAppsList.isEmpty() && !TextUtils.isEmpty(seachText)) {
            mPlayStoreSearch.setVisibility(View.VISIBLE);
            mList.setVisibility(View.GONE);
        } else {
            mPlayStoreSearch.setVisibility(View.GONE);
            mList.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        updateSearchResult(s.toString());
        mAppsAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.playStoreSearch) {
            String searchText = mSearchInput.getText().toString();
            if (!TextUtils.isEmpty(searchText)) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://search?q=" + searchText));
                if (mLauncher != null)
                    mLauncher.startActivitySafely(intent, null);
            }
        }
    }
}
