/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mogoo.launcher2;

import android.provider.BaseColumns;
import android.view.ViewGroup;
import android.net.Uri;

/**
 * Settings related utilities.
 */
public class LauncherSettings {
    static interface BaseLauncherColumns extends BaseColumns {
        /**
         * Descriptive name of the gesture that can be displayed to the user.
         * <P>Type: TEXT</P>
         */
        static final String TITLE = "title";

        /**
         * The Intent URL of the gesture, describing what it points to. This
         * value is given to {@link android.content.Intent#parseUri(String, int)} to create
         * an Intent that can be launched.
         * <P>Type: TEXT</P>
         */
        static final String INTENT = "intent";

        /**
         * The type of the gesture
         *
         * <P>Type: INTEGER</P>
         */
        static final String ITEM_TYPE = "itemType";

        /**
         * The gesture is an application
         */
        static final int ITEM_TYPE_APPLICATION = 0;

        /**
         * The gesture is an application created shortcut
         */
        static final int ITEM_TYPE_SHORTCUT = 1;

        /**
         * The icon type.
         * <P>Type: INTEGER</P>
         */
        static final String ICON_TYPE = "iconType";

        /**
         * The icon is a resource identified by a package name and an integer id.
         */
        static final int ICON_TYPE_RESOURCE = 0;

        /**
         * The icon is a bitmap.
         */
        static final int ICON_TYPE_BITMAP = 1;

        /**
         * The icon package name, if icon type is ICON_TYPE_RESOURCE.
         * <P>Type: TEXT</P>
         */
        static final String ICON_PACKAGE = "iconPackage";

        /**
         * The icon resource id, if icon type is ICON_TYPE_RESOURCE.
         * <P>Type: TEXT</P>
         */
        static final String ICON_RESOURCE = "iconResource";
        
        static final String TITLE_RESOURCE = "titleResource";

        /**
         * The custom icon bitmap, if icon type is ICON_TYPE_BITMAP.
         * <P>Type: BLOB</P>
         */
        static final String ICON = "icon";
        
      //****************add by 张永辉 2011-1-21********************
        /**
         * 标识图标是否为系统程序
         * <P>Type:INTEGER</P>
         */
        static final String IS_SYSTEM = "isSystem" ;
        /**
         * 系统程序
         */
        static final int IS_SYSTEM_APP = 1 ;
        /**
         * 非系统程序
         */
        static final int NOT_SYSTEM_APP = 0 ;
        /**
         * 应用类型 例如：短信、邮件等
         * <P>Type:INTEGER</P>
         */
        static final String APP_TYPE = "appType" ;
        /**
         * 短信
         */
        static final int APP_TYPE_SMS = 1 ;
        /**
         * 邮件
         */
        static final int APP_TYPE_EMAIL = 2 ;
        /**
         * 电话
         */
        static final int APP_TYPE_TELEPHONE = 3 ;
        /**
         * market
         */
        static final int APP_TYPE_MARKET = 4 ;
        /**
         * 其它类型
         */
        static final int APP_TYPE_OTHER = -1 ;
        
      //****************add by 张永辉*********************** end 
        
    }
    
    //add by huangyue
    public static int getViewGourpType(ViewGroup viewGroup){
        if(viewGroup instanceof Workspace || viewGroup instanceof CellLayout){
            return Favorites.CONTAINER_DESKTOP;
        } else if(viewGroup instanceof Mogoo_DockWorkSpace){
            return Favorites.CONTAINER_TOOLBAR;
        }
        
        return -1;
    }
    //end

    /**
     * Favorites.
     */
    public static final class Favorites implements BaseLauncherColumns {
        /**
         * The content:// style URL for this table
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" +
                LauncherProvider.AUTHORITY + "/" + LauncherProvider.TABLE_FAVORITES +
                "?" + LauncherProvider.PARAMETER_NOTIFY + "=true");

        /**
         * The content:// style URL for this table. When this Uri is used, no notification is
         * sent if the content changes.
         */
        static final Uri CONTENT_URI_NO_NOTIFICATION = Uri.parse("content://" +
                LauncherProvider.AUTHORITY + "/" + LauncherProvider.TABLE_FAVORITES +
                "?" + LauncherProvider.PARAMETER_NOTIFY + "=false");
        
        /**
         * add by huangyue for sent if app active change
         */
        public static final Uri ACTIVE_URI_NO_NOTIFICATION = Uri.parse("content://" +
                LauncherProvider.AUTHORITY + "/" + LauncherProvider.TABLE_ACTIVE +
                "?" + LauncherProvider.PARAMETER_NOTIFY + "=false");

        /**
         * The content:// style URL for a given row, identified by its id.
         *
         * @param id The row id.
         * @param notify True to send a notification is the content changes.
         *
         * @return The unique content URL for the specified row.
         */
        public static Uri getContentUri(long id, boolean notify) {
            return Uri.parse("content://" + LauncherProvider.AUTHORITY +
                    "/" + LauncherProvider.TABLE_FAVORITES + "/" + id + "?" +
                    LauncherProvider.PARAMETER_NOTIFY + "=" + notify);
        }
        
        /**
         * The content:// style URL for a given row, identified by its id.
         *
         * @param id The row id.
         * @param notify True to send a notification is the content changes.
         *
         * @return The unique content URL for the specified row.
         */
        public static Uri getContentActiveUri(long id) {
            return Uri.parse("content://" + LauncherProvider.AUTHORITY +
                    "/" + LauncherProvider.TABLE_ACTIVE + "/" + id + "?" +
                    LauncherProvider.PARAMETER_NOTIFY + "=" + false);
        }

        /**
         * The container holding the favorite
         * <P>Type: INTEGER</P>
         */
        public static final String CONTAINER = "container";

        /**
         * The icon is a resource identified by a package name and an integer id.
         */
        public static final int CONTAINER_DESKTOP = -100;
        
        //----------------add by 魏景春-------------
        //工具栏容器ID
        public static final int CONTAINER_TOOLBAR = -200;

        /**
         * The screen holding the favorite (if container is CONTAINER_DESKTOP)
         * <P>Type: INTEGER</P>
         */
        public static final String SCREEN = "screen";

        /**
         * The X coordinate of the cell holding the favorite
         * (if container is CONTAINER_DESKTOP or CONTAINER_DOCK)
         * <P>Type: INTEGER</P>
         */
        public static final String CELLX = "cellX";

        /**
         * The Y coordinate of the cell holding the favorite
         * (if container is CONTAINER_DESKTOP)
         * <P>Type: INTEGER</P>
         */
        public static final String CELLY = "cellY";

        /**
         * The X span of the cell holding the favorite
         * <P>Type: INTEGER</P>
         */
        public static final String SPANX = "spanX";

        /**
         * The Y span of the cell holding the favorite
         * <P>Type: INTEGER</P>
         */
        public static final String SPANY = "spanY";

        /**
         * The favorite is a user created folder
         */
        public static final int ITEM_TYPE_USER_FOLDER = 2;

        /**
         * The favorite is a live folder
         */
        public static final int ITEM_TYPE_LIVE_FOLDER = 3;

        /**
         * The favorite is a widget
         */
        public static final int ITEM_TYPE_APPWIDGET = 4;
        
        /**
         * 文件夹类型
         * add by 张永辉 2011-3-14
         */
        public static final int ITEM_TYPE_MOGOO_FOLDER = 5;

        /**
         * The favorite is a clock
         */
        static final int ITEM_TYPE_WIDGET_CLOCK = 1000;

        /**
         * The favorite is a search widget
         */
        static final int ITEM_TYPE_WIDGET_SEARCH = 1001;

        /**
         * The favorite is a photo frame
         */
        static final int ITEM_TYPE_WIDGET_PHOTO_FRAME = 1002;

        /**
         * The appWidgetId of the widget
         *
         * <P>Type: INTEGER</P>
         */
        static final String APPWIDGET_ID = "appWidgetId";
        
        /**
         * Indicates whether this favorite is an application-created shortcut or not.
         * If the value is 0, the favorite is not an application-created shortcut, if the
         * value is 1, it is an application-created shortcut.
         * <P>Type: INTEGER</P>
         */
        @Deprecated
        static final String IS_SHORTCUT = "isShortcut";

        /**
         * The URI associated with the favorite. It is used, for instance, by
         * live folders to find the content provider.
         * <P>Type: TEXT</P>
         */
        static final String URI = "uri";

        /**
         * The display mode if the item is a live folder.
         * <P>Type: INTEGER</P>
         *
         * @see android.provider.LiveFolders#DISPLAY_MODE_GRID
         * @see android.provider.LiveFolders#DISPLAY_MODE_LIST
         */
        static final String DISPLAY_MODE = "displayMode";
        
        //add by huangyue for APP_ACTIVE
        public static final String PACKAGE = "package";
        
        public static final String ACTIVE_DATE = "activeDate";
        
        public static final String UPDATE_DATE = "updateDate";
        
        public static final String CLICK_COUNT = "clickCount";
        
        public static final String IS_UPLOAD = "isUpload";
        //end
        
    }
}
