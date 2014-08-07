package com.limemobile.app.launcher.util;

import android.content.Context;
import android.os.PowerManager;

public class ManageWakeLock {
	private static PowerManager.WakeLock myWakeLock = null;
	private static PowerManager.WakeLock myPartialWakeLock = null;

	public static synchronized void acquireFull(Context context) {
		PowerManager myPM = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

		if (myWakeLock != null) {
			return;
		}
		
		int flags = PowerManager.SCREEN_DIM_WAKE_LOCK;
		flags |= PowerManager.ON_AFTER_RELEASE;
		// PowerManager.ACQUIRE_CAUSES_WAKEUP

		myWakeLock = myPM.newWakeLock(flags, "acquire");
		myWakeLock.setReferenceCounted(false);
		myWakeLock.acquire();
	}

	public static synchronized void DoCancel(Context context) {
		releaseFull();
	}

	public static synchronized void acquirePartial(Context context) {
		PowerManager myPM = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

		if (myPartialWakeLock != null) {
			return;
		}

		myPartialWakeLock = myPM.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
				"myLock");
		myPartialWakeLock.acquire();
	}

	public static synchronized void releaseFull() {
		if (myWakeLock != null) {
			myWakeLock.release();
			myWakeLock = null;
		}
	}

	public static synchronized void releasePartial() {
		if (myPartialWakeLock != null) {
			myPartialWakeLock.release();
			myPartialWakeLock = null;
		}
	}

	public static synchronized void releaseAll() {
		releaseFull();
		releasePartial();
	}
}
