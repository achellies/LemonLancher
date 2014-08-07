package com.limemobile.app.launcher.util;

import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Adds intent options with icons.
 * 
 * This code is retrieved from this message:
 * http://groups.google.com/group/android
 * -developers/browse_frm/thread/3fed25cdda765b02
 * 
 */
public class MenuIntentOptionsWithIcons {

	Context mContext;
	Menu mMenu;

	public MenuIntentOptionsWithIcons(Context context, Menu menu) {
		mContext = context;
		mMenu = menu;
	}

	public int addIntentOptions(int group, int id, int categoryOrder,
			ComponentName caller, Intent[] specifics, Intent intent, int flags,
			MenuItem[] outSpecificItems) {
		PackageManager pm = mContext.getPackageManager();
		final List<ResolveInfo> lri = pm.queryIntentActivityOptions(caller,
				specifics, intent, 0);
		final int N = lri != null ? lri.size() : 0;
		if ((flags & Menu.FLAG_APPEND_TO_GROUP) == 0) {
			mMenu.removeGroup(group);
		}
		for (int i = 0; i < N; i++) {
			final ResolveInfo ri = lri.get(i);
			Intent rintent = new Intent(ri.specificIndex < 0 ? intent
					: specifics[ri.specificIndex]);
			rintent.setComponent(new ComponentName(
					ri.activityInfo.applicationInfo.packageName,
					ri.activityInfo.name));
			final MenuItem item = mMenu
					.add(group, id, categoryOrder, ri.loadLabel(pm))
					.setIcon(ri.loadIcon(pm)).setIntent(rintent);
			if (outSpecificItems != null && ri.specificIndex >= 0) {
				outSpecificItems[ri.specificIndex] = item;
			}
		}
		return N;
	}
}
