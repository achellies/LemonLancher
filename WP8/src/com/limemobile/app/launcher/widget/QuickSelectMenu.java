package com.limemobile.app.launcher.widget;


import android.content.Context;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

/* This class exposes a subset of PopupMenu functionality, and chooses whether 
 * to use the platform PopupMenu (on Honeycomb or above) or a backported version.
 */
public class QuickSelectMenu {

//	android.widget.PopupMenu mImplPlatform = null;
	com.limemobile.app.launcher.widget.PopupMenu mImplBackport = null;

	private enum ImplMode {
		PLATFORM, BACKPORT, NONE
	};

	ImplMode mMode = ImplMode.NONE;
	private OnItemSelectedListener mItemSelectedListener;

	public QuickSelectMenu(Context context, View anchor) {
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
//			mMode = ImplMode.PLATFORM;
//			mImplPlatform = new android.widget.PopupMenu(context, anchor);
//			mImplPlatform
//					.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() {
//						@Override
//						public boolean onMenuItemClick(MenuItem item) {
//							return onMenuItemClickImpl(item);
//						}
//					});
//		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			mMode = ImplMode.BACKPORT;
			mImplBackport = new com.limemobile.app.launcher.widget.PopupMenu(context, anchor);
			mImplBackport
					.setOnMenuItemClickListener(new com.limemobile.app.launcher.widget.PopupMenu.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							return onMenuItemClickImpl(item);
						}
					});
//		}
	}

	// not sure if we want to expose this or just an add() method.
	public Menu getMenu() {
//		if (mMode == ImplMode.PLATFORM) {
//			Menu menu = mImplPlatform.getMenu();
//			return menu;
//		}
		if (mMode == ImplMode.BACKPORT) {
			Menu menu = mImplBackport.getMenu();
			return menu;
		}
		return null;
	}

	/**
	 * Interface responsible for receiving menu item click events if the items
	 * themselves do not have individual item click listeners.
	 */
	public interface OnItemSelectedListener {
		/**
		 * This method will be invoked when an item is selected.
		 * 
		 * @param item
		 *            {@link CharSequence} that was selected
		 * @param id
		 */
		public void onItemSelected(CharSequence item, int id);
	}

	public void setOnItemSelectedListener(OnItemSelectedListener listener) {
		mItemSelectedListener = listener;
	}

	public void show() {
//		if (mMode == ImplMode.PLATFORM) {
//			mImplPlatform.show();
//		}
		if (mMode == ImplMode.BACKPORT) {
			mImplBackport.show();
		}
	}
	
	public void dismiss() {
//      if (mMode == ImplMode.PLATFORM) {
//      mImplPlatform.dismiss();
//  }
        if (mMode == ImplMode.BACKPORT) {
            mImplBackport.dismiss();
        }
	}

	// popup.setOnMenuItemClickListener(new
	// android.widget.PopupMenu.OnMenuItemClickListener() {

	public boolean onMenuItemClickImpl(MenuItem item) {
		CharSequence name = item.getTitle();
		int id = item.getItemId();
		if (this.mItemSelectedListener != null)
		    this.mItemSelectedListener.onItemSelected(name, id);
		return true;
	}
}
