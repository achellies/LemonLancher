package com.mogoo.launcher2;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.exception.Mogoo_BootRestoreException;
import com.mogoo.launcher2.restore.Mogoo_RestoreController;
import com.mogoo.launcher2.restore.Mogoo_UncaughtExceptionHandler;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;

//import com.android.common.Search;
import android.app.ListActivity;
import android.app.SearchManager;
import android.app.StatusBarManager;
import android.os.Bundle;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.TextKeyListener;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
//import android.app.StatusBarManager;
import android.content.Context;
import android.content.Intent;

public class Mogoo_LauncherSettings extends ListActivity {
	   private static final String TAG ="Mogoo_LauncherSettings";
	   private static final int ADD_WALLPAPER = 0;
//	   private static final int SEARCH = 1;
//	   private static final int NOTIFICATIONS = 2;
	   private static final int REFRESH = 1;
	   private SpannableStringBuilder mDefaultKeySsb = null;
	   private Mogoo_RestoreController mRestoreController;
	   private static Mogoo_UncaughtExceptionHandler mUncaughtExceptionHandler;
	   
	   @Override
	   protected void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        //addPreferencesFromResource(R.xml.mt_launcher_settings);
	        // For handling default keys
	        Log.d(TAG, "onCreate");
	        String title = getResources().getString(R.string.settings_label);
	        
	        // TODO: disabled by achellies
	        //setBackBtnStyle(true,-1,title);
	        setContentView(R.layout.mogoo_launcher_settings);
	        String[] launcher_settings = getResources().getStringArray(R.array.launcher_entries);
	        setListAdapter(new SettingAdapter(this,
	                android.R.layout.simple_list_item_1, launcher_settings));
	        RunRestorePolicy runPolicy = new RunRestorePolicy();
	        runPolicy.start();
	        mDefaultKeySsb = new SpannableStringBuilder();
	        Selection.setSelection(mDefaultKeySsb, 0);
	        
	        TextView text = (TextView) findViewById(android.R.id.title);
	        text.setText(R.string.launcher_settings);
	   }
	   
	   
	   private class RunRestorePolicy extends Thread
	   {
		    public void run()
			{
		    	mRestoreController = new Mogoo_RestoreController(Mogoo_LauncherSettings.this);
		    	mRestoreController.loadPolicy();
		    	mUncaughtExceptionHandler = new Mogoo_UncaughtExceptionHandler(mRestoreController,Mogoo_LauncherSettings.this);
		    	Log.d(TAG, "setDefaultUncaughtExceptionHandler");
		    	Thread.setDefaultUncaughtExceptionHandler(mUncaughtExceptionHandler);
			}
	   }
	   
	   @Override
	   protected void onListItemClick(ListView l, View v, int position, long id)
	   {
		   if(ADD_WALLPAPER == position)
		   {
			   startWallpaper();
		   }
		   /*else if(SEARCH == position)
		   {
			   onSearchRequested();			   
		   }
		   else if(NOTIFICATIONS == position)
		   {
			   showNotifications();
		   }*/
		   else if(REFRESH == position)
		   {
			   returnToLauncher();
			   refreshThrowException();
		   }
	   }
	   
	   private void returnToLauncher()
	   {
		   try
		   {
			   Intent intent = new Intent();
			   intent.addCategory(Intent.CATEGORY_HOME);
			   intent.setAction(Intent.ACTION_MAIN);
			   startActivity(intent);
		   }
		   catch(Exception e)
		   {
			   e.printStackTrace();
		   }
	   }
	    //denglixia add 2011.4.20
	    //按刷新菜单，抛出异常，重启Launcher
	    private void refreshThrowException() throws Mogoo_BootRestoreException
	    {
	    	throw new Mogoo_BootRestoreException();
	    }
	    private void showNotifications() {
	    	// TODO: disabled by achellies
	        final StatusBarManager statusBar = (StatusBarManager) getSystemService("statusbar"/*STATUS_BAR_SERVICE*/);
	        if (statusBar != null) {
	            statusBar.expand();
	        }
	    }
	    private void startWallpaper() {
	        final Intent pickWallpaper = new Intent(Intent.ACTION_SET_WALLPAPER);
	        Intent chooser = Intent.createChooser(pickWallpaper, getText(R.string.chooser_wallpaper));
	        startActivityForResult(chooser, 0);
	    }
	    @Override
	    public boolean onSearchRequested() {
	        startSearch(null, false, null, true);
	        return true;
	    }
	    @Override
	    public void startSearch(String initialQuery, boolean selectInitialQuery, Bundle appSearchData,
	            boolean globalSearch) {

	        // closeAllApps(true);

	        if (initialQuery == null) {
	            // Use any text typed in the launcher as the initial query
	            initialQuery = getTypedText();
	            clearTypedText();
	        }
	        if (appSearchData == null) {
	            appSearchData = new Bundle();
	            //appSearchData.putString(Search.SOURCE, "launcher-search");
	            appSearchData.putString("source", "launcher-search");
	        }

	        final SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	        searchManager.startSearch(initialQuery, selectInitialQuery, getComponentName(),
	                appSearchData, globalSearch);
	    }
	    
	    private String getTypedText() {
	        return mDefaultKeySsb.toString();
	    }

	    private void clearTypedText() {
	        mDefaultKeySsb.clear();
	        mDefaultKeySsb.clearSpans();
	        Selection.setSelection(mDefaultKeySsb, 0);
	    }
	    
	    private class SettingAdapter extends ArrayAdapter<String>{

			public SettingAdapter(Context context, int textViewResourceId,
					String[] objects) {
				super(context, textViewResourceId, objects);
			}
			 
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				View v = super.getView(position, convertView, parent);
				
				if(v instanceof TextView){
					int count = getCount();
				    CharSequence text = ((TextView)v).getText();
				    v = getLayoutInflater().inflate(R.layout.mogoo_setting_row, null);
				    TextView tv = (TextView)v.findViewById(R.id.row_title);
				    
				    tv.setText(text);
				
					if(position == 0){
						v.setBackgroundResource(R.drawable.mogoo_pre_bg_top);
					} else if(position > 0 && position < count - 1){
						v.setBackgroundResource(R.drawable.mogoo_pre_bg_middle);
					} else if(position == count - 1){
						v.setBackgroundResource(R.drawable.mogoo_pre_bg_buttom);
					}
					
					tv = null;
					text = null;
				}
				
				return v;
			}
	    	
	    }
}
