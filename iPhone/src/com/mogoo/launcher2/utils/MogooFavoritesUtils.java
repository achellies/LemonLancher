package com.mogoo.launcher2.utils;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import com.mogoo.launcher2.LauncherModel;
import com.mogoo.launcher2.LauncherProvider;
import com.mogoo.launcher2.LauncherProvider.DatabaseHelper;
import com.mogoo.launcher2.LauncherSettings.Favorites;
import com.mogoo.launcher2.LauncherSettings;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.database.sqlite.SQLiteDatabase;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;

import com.mogoo.launcher.R;

/**
 * 处理增加Favorites功能
 * 
 * @author hy
 * 
 */
public class MogooFavoritesUtils {

	public static int addFolder(XmlResourceParser parser, SQLiteDatabase db,
			ContentValues values, Context context, DatabaseHelper helper, TypedArray folder)
			throws XmlPullParserException, IOException {
		values.put(Favorites.SPANX, 1);
		values.put(Favorites.SPANY, 1);
		values.put(Favorites.ITEM_TYPE, Favorites.ITEM_TYPE_MOGOO_FOLDER);
		
		final int titleResId = folder.getResourceId(R.styleable.Favorite_title, 0);
		String title = null;
		Resources r = context.getResources();
		
		 if(titleResId>0)
         {
            //test
         	try {
           	   title = r.getString(titleResId);
				} catch (Exception e) {
					// TODO: handle exception
					title = "title not found";
				}
         } else {
        	 title = "folder";
         }
		 values.put(Favorites.TITLE, title);

		long id = db.insert(LauncherProvider.TABLE_FAVORITES, null, values);
		int depth = parser.getDepth();
		int type;
		int i = 0;

		AttributeSet attrs = Xml.asAttributeSet(parser);
		PackageManager packageManager = context.getPackageManager();
		Intent intent = new Intent(Intent.ACTION_MAIN, null);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);

		while (((type = parser.next()) != XmlPullParser.END_TAG || parser
				.getDepth() > depth) && type != XmlPullParser.END_DOCUMENT) {
			if (type != XmlPullParser.START_TAG) {
				continue;
			}

			boolean added = false;
			final String name = parser.getName();

			TypedArray a = context.obtainStyledAttributes(attrs,
					R.styleable.Favorite);

			values.clear();

			// update by 张永辉 2010-12-31
			// values.put(LauncherSettings.Favorites.CONTAINER,
			// LauncherSettings.Favorites.CONTAINER_DESKTOP);
			int container = (int) id;
			// try{
			// container =
			// Integer.parseInt(a.getString(R.styleable.Favorite_container));
			// }catch(Exception e){
			// container = LauncherSettings.Favorites.CONTAINER_DESKTOP ;
			// }

			values.put(LauncherSettings.Favorites.CONTAINER, container);
			// end

			values.put(LauncherSettings.Favorites.SCREEN,
					a.getString(R.styleable.Favorite_screen));
			values.put(LauncherSettings.Favorites.CELLX,
					a.getString(R.styleable.Favorite_x));
			values.put(LauncherSettings.Favorites.CELLY,
					a.getString(R.styleable.Favorite_y));
			// *********add by 张永辉 2010-12-9
			values.put(LauncherSettings.Favorites.IS_SYSTEM,
					a.getString(R.styleable.Favorite_isSystem));
			values.put(LauncherSettings.Favorites.APP_TYPE,
					a.getString(R.styleable.Favorite_appType));

			if (DatabaseHelper.TAG_FAVORITE.equals(name)) {
				added = helper.addAppShortcut(db, values, a, packageManager,
						intent);
			}

			if (added)
				i++;
		}

		return i;
	}
}
