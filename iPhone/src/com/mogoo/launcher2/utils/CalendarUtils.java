package com.mogoo.launcher2.utils;

import java.util.Date;

import com.mogoo.launcher2.FastBitmapDrawable;
import com.mogoo.launcher2.ItemInfo;
import com.mogoo.launcher2.Mogoo_BubbleTextView;
import com.mogoo.launcher2.Mogoo_DockWorkSpace;
import com.mogoo.launcher2.ShortcutInfo;
import com.mogoo.launcher2.Workspace;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;
import com.mogoo.launcher2.exception.Mogoo_BootRestoreException;
import com.mogoo.launcher2.restore.Mogoo_UncaughtExceptionHandler;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import com.mogoo.launcher.R;
/**
 * 日期图标工具类
 * @author hy
 *
 */
public class CalendarUtils {
	private static ResolveInfo mCalendarInfo;
	private static final String CALENDAR_PACKAGE_NAME = "com.android.calendar";
	
	private static String[] weakArray;
	
	public static Bitmap getCalendarIcon(ResolveInfo info, Mogoo_BitmapCache cache){
		if(info != null && info.activityInfo != null && CALENDAR_PACKAGE_NAME.equals(info.activityInfo.packageName)){
//			try {
//				Thread.sleep(10000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
			if(weakArray == null){
				weakArray = cache.getContext().getResources().getStringArray(R.array.weak_entries);
			}
			
			int width = Mogoo_GlobalConfig.getIconWidth();
			int height = Mogoo_GlobalConfig.getIconHeight();
			
			mCalendarInfo = info;
			Drawable d = cache.loadIcon(cache.getContext().getPackageManager(), mCalendarInfo);
			Bitmap icon = Mogoo_BitmapUtils.createIconBitmap(d, cache.getContext(), true);
			Rect rect = new Rect();
			
			Date currentDate = new Date(System.currentTimeMillis());
			String day = weakArray[currentDate.getDay()];
//			String date = (currentDate.getDate() > 9 ? "" : "0") + currentDate.getDate() ;
			String date = ""+currentDate.getDate() ;
			
			Paint paint = new Paint();
			paint.setAntiAlias(true);
			paint.setColor(Color.WHITE);
			paint.setTextSize(Mogoo_GlobalConfig.getIntByKey(Mogoo_GlobalConfig.DAY_TEXT_SIZE));
			paint.getTextBounds(day, 0, day.length(), rect);
			
			Canvas canvas = new Canvas(icon);
			canvas.drawText(day, (width - rect.width()) / 2 + 2, rect.height() + 3, paint);
			paint.setColor(0xFF333333);
			paint.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
			paint.setTextSize(Mogoo_GlobalConfig.getIntByKey(Mogoo_GlobalConfig.DATE_TEXT_SIZE));
			paint.getTextBounds(date, 0, date.length(), rect);
			canvas.drawText(date, (width - rect.width())/2 , height - (height - rect.height())/2 + Mogoo_GlobalConfig.getDateHeightFixValue(), paint);
			canvas.save();
			
			return icon;
		}
		
		return null;
	}
	
	public static void referenceCalendarIcon(Mogoo_BitmapCache cache, Context context){
		if(mCalendarInfo != null && cache != null){
			ComponentName comp = new ComponentName(mCalendarInfo.activityInfo.packageName, mCalendarInfo.activityInfo.name);
			cache.remove(comp);
			cache.recycle(comp, Mogoo_BitmapCache.RECYCLE_COMPONENT_NAME_ALL);
			ItemInfo info = null;
			Workspace workspace = (Workspace)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.workspace, context);
			if(workspace == null){
				throw new Mogoo_BootRestoreException();
			}
			
			ViewGroup vg = null;
			View v = null;
			int size = workspace.getChildCount();
			for(int i = 0; i < size; i++){
				vg = (ViewGroup) workspace.getChildAt(i);
				if(vg == null){
					continue;
				}
				
				if(executeVg(cache, vg)){
					return;
				}
			}
			
			vg = (ViewGroup)Mogoo_ComponentBus.getInstance().getActivityComp(R.id.dockWorkSpace, context);
			
			if(executeVg(cache, vg)){
				return;
			}
			
			vg = (ViewGroup) Mogoo_ComponentBus.getInstance().getActivityComp(
					R.id.folderWorkspace, context);

			if (executeVg(cache, vg)) {
				return;
			}
			
		}
		
	}

	private static boolean executeVg(Mogoo_BitmapCache cache, ViewGroup vg) {
		ItemInfo info;
		View v;
		int count = vg.getChildCount();
		for(int j = 0; j < count; j++){
			v = vg.getChildAt(j);
			info = (ItemInfo) v.getTag();
			if(v instanceof Mogoo_BubbleTextView && info != null && info instanceof ShortcutInfo){
				if(executeDate(cache, info, vg, v)){
					return true; 
				}
			}
		}
		
		return false;
	}

	private static boolean executeDate(Mogoo_BitmapCache cache, ItemInfo info,
			ViewGroup vg, View v) {
		if(CALENDAR_PACKAGE_NAME.equals(((ShortcutInfo) info).intent.getComponent().getPackageName())){
			((ShortcutInfo) info).setIcon(null);
			((Mogoo_BubbleTextView) v).setCompoundDrawablesWithIntrinsicBounds(
					null, new FastBitmapDrawable(cache.getIcon(((ShortcutInfo) info).intent)),
					null, null);
			
			v.invalidate();
			
			return true;
		}
		
		return false;
	}
}
