package com.limemobile.app.launcher.activity;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;

import com.limemobile.app.launcher.common.LauncherSettings;
import com.limemobile.app.launcher.wp8.R;

public class Settings extends PreferenceActivity implements OnClickListener {
    private View back;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        boolean autoSense = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(LauncherSettings.SCREEN_ORIENTATION, false);
        boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;        
        setRequestedOrientation(autoSense ? ActivityInfo.SCREEN_ORIENTATION_SENSOR : (portrait ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.setting);
        setContentView(R.layout.activity_setting);
        
        back = findViewById(R.id.back);
        back.setOnClickListener(this);
        
        getPreferenceScreen().findPreference("screenorientation").setOnPreferenceChangeListener(preferenceChangeListener);
        getPreferenceScreen().findPreference("lockscreen").setOnPreferenceChangeListener(preferenceChangeListener);
    }
    
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    
    private OnPreferenceChangeListener preferenceChangeListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference,
                Object newValue) {
            if (preference.getKey().equals("screenorientation")) {
                Boolean autoOrientation = (Boolean) newValue;
                boolean portrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;        
                setRequestedOrientation(autoOrientation.booleanValue() ? ActivityInfo.SCREEN_ORIENTATION_SENSOR : (portrait ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
            } else if (preference.getKey().equals("lockscreen")) {
            }
            return true;
        }
        
    };
}
