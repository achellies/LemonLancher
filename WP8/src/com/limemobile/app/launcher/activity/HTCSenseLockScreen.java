package com.limemobile.app.launcher.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.limemobile.app.launcher.common.LauncherSettings;
import com.limemobile.app.launcher.util.ManageWakeLock;
import com.limemobile.app.launcher.view.HTCSenseLockScreenView;
import com.limemobile.app.launcher.view.HTCSenseLockScreenView.OnTriggeredListener;
import com.limemobile.app.launcher.wp8.R;

public class HTCSenseLockScreen extends Activity implements OnTriggeredListener {
    public static final String TAG = "Launcher";
    private HTCSenseLockScreenView mSenseUnLockView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean autoSense = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(LauncherSettings.SCREEN_ORIENTATION, false);
        boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;        
        setRequestedOrientation(autoSense ? ActivityInfo.SCREEN_ORIENTATION_SENSOR : (portrait ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
        
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        
        setContentView(R.layout.htc_sense_lock_screen);
        
        mSenseUnLockView = (HTCSenseLockScreenView) findViewById(R.id.sense_unlock_view);
        
        mSenseUnLockView.setOnTriggeredListener(this);
    }

    @Override
	protected void onResume() {
    	ManageWakeLock.acquirePartial(this);
		super.onResume();
	}

	@Override
	protected void onPause() {
		ManageWakeLock.releasePartial();
		super.onPause();
		
		finish();
	}

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
    public void onBackPressed() {
    	
    }
    
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	return true;
	}

    @Override
	public boolean dispatchKeyEvent(KeyEvent event) {
    	if (KeyEvent.KEYCODE_HOME == event.getAction())
    		return true;
		return super.dispatchKeyEvent(event);
	}

	@Override
	public void onAttachedToWindow() {
    	this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD);
		super.onAttachedToWindow();
	}
    
	@Override
    public void OnUnLockTriggered() {
		finish();
    }

	@Override
    public void OnShortcutTriggered(Intent intent) {
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Unable to launch. " + " intent=" + intent, e);
            } catch (SecurityException e) {
                Toast.makeText(this, R.string.activity_not_found, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Launcher does not have the permission to launch " + intent +
                        ". Make sure to create a MAIN intent-filter for the corresponding activity " +
                        "or use the exported attribute for this activity. "
                        +  " intent=" + intent, e);
            }
        }
    }

}
