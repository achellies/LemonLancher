package com.limemobile.app.launcher.service;

import com.limemobile.app.launcher.activity.HTCSenseLockScreen;
import com.limemobile.app.launcher.common.LauncherSettings;

import android.app.KeyguardManager;
import android.app.KeyguardManager.KeyguardLock;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class KeyguardService extends Service {

	private TelephonyManager mTelephonyManager;
	private KeyguardManager mKeyguardManager;
	private KeyguardLock mKeyguardLock;
	
	private int mPhoneState = TelephonyManager.CALL_STATE_IDLE;
	private boolean isCalling = false;
	private boolean isCharging = false;
	private boolean isDisableKeyguard = false;
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);		
		mKeyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		
		mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
		
		mKeyguardLock = mKeyguardManager.newKeyguardLock(KeyguardService.class.getName());
		
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mInternalReceiver, filter);
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		
		mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
		unregisterReceiver(mInternalReceiver);
		
		if (isDisableKeyguard)
			mKeyguardLock.reenableKeyguard();
		isDisableKeyguard = false;
		super.onDestroy();
	}
	
	private PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			/*
			CALL_STATE_IDLE is 0 - this state comes back when calls end
			CALL_STATE_RINGING is 1 - a call is incoming, waiting for user to answer.
			CALL_STATE_OFFHOOK is 2 - call is actually in progress
			 */
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				isCalling = false;
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
				isCalling = true;
				break;
			case TelephonyManager.CALL_STATE_RINGING:
				isCalling = true;
				break;
			}
			mPhoneState = state;
			super.onCallStateChanged(state, incomingNumber);
		}
		
	};
	
	private BroadcastReceiver mInternalReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (Intent.ACTION_SCREEN_OFF.equals(action)) {
				
			} else if (Intent.ACTION_SCREEN_ON.equals(action)) {
				if (!isCalling /*&& !isCharging*/) {
//					mKeyguardLock.disableKeyguard();
//					isDisableKeyguard = true;
				    boolean lockScreen = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).getBoolean(LauncherSettings.LOCK_SCREEN, true);
				    if (lockScreen) {
    					Intent lockScreenIntent = new Intent(getBaseContext(), HTCSenseLockScreen.class);
    					lockScreenIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    					startActivity(lockScreenIntent);
				    }
				}
			} else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
				final int plugstate = intent.getIntExtra(
						BatteryManager.EXTRA_PLUGGED,
						BatteryManager.BATTERY_PLUGGED_AC);
				if (plugstate == BatteryManager.BATTERY_PLUGGED_AC
						|| plugstate == BatteryManager.BATTERY_PLUGGED_USB) {
					isCharging = true;
				} else
					isCharging = false;
			}
		}
	};
}
