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

package com.limemobile.app.launcher.adapter;

import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.ImageView;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.database.Cursor;
import android.provider.LiveFolders;
import android.graphics.drawable.Drawable;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;

import com.limemobile.app.launcher.activity.Launcher;
import com.limemobile.app.launcher.entity.LiveFolderInfo;
import com.limemobile.app.launcher.util.Utilities;
import com.limemobile.app.launcher.view.FastBitmapDrawable;
import com.limemobile.app.launcher.wp8.R;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.lang.ref.SoftReference;

//import com.android.launcher.R; modify by author

public class LiveFolderAdapter extends CursorAdapter {
    private boolean mIsList;
    private LayoutInflater mInflater;

    private final HashMap<String, Drawable> mIcons = new HashMap<String, Drawable>();
    private final HashMap<Long, SoftReference<Drawable>> mCustomIcons =
            new HashMap<Long, SoftReference<Drawable>>();
    private final Launcher mLauncher;

    public LiveFolderAdapter(Launcher launcher, LiveFolderInfo info, Cursor cursor) {
        super(launcher, cursor, true);
        mIsList = info.displayMode == LiveFolders.DISPLAY_MODE_LIST;
        mInflater = LayoutInflater.from(launcher);
        mLauncher = launcher;

        mLauncher.startManagingCursor(getCursor());
    }

    public static Cursor query(Context context, LiveFolderInfo info) {
        return context.getContentResolver().query(info.uri, null, null,
                null, LiveFolders.NAME + " ASC");
    }

    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view;
        final ViewHolder holder = new ViewHolder();

        if (!mIsList) {
            view = mInflater.inflate(R.layout.application_boxed, parent, false);
        } else {
            view = mInflater.inflate(R.layout.application_list, parent, false);
            holder.description = (TextView) view.findViewById(R.id.description);
            holder.icon = (ImageView) view.findViewById(R.id.icon);
        }

        holder.name = (TextView) view.findViewById(R.id.name);

        holder.idIndex = cursor.getColumnIndexOrThrow(LiveFolders._ID);
        holder.nameIndex = cursor.getColumnIndexOrThrow(LiveFolders.NAME);
        holder.descriptionIndex = cursor.getColumnIndex(LiveFolders.DESCRIPTION);
        holder.intentIndex = cursor.getColumnIndex(LiveFolders.INTENT);
        holder.iconBitmapIndex = cursor.getColumnIndex(LiveFolders.ICON_BITMAP);
        holder.iconResourceIndex = cursor.getColumnIndex(LiveFolders.ICON_RESOURCE);
        holder.iconPackageIndex = cursor.getColumnIndex(LiveFolders.ICON_PACKAGE);

        view.setTag(holder);

        return view;
    }

    public void bindView(View view, Context context, Cursor cursor) {
        final ViewHolder holder = (ViewHolder) view.getTag();

        holder.id = cursor.getLong(holder.idIndex);
        final Drawable icon = loadIcon(context, cursor, holder);

        holder.name.setText(cursor.getString(holder.nameIndex));

        if (!mIsList) {
            holder.name.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
        } else {
            final boolean hasIcon = icon != null;
            holder.icon.setVisibility(hasIcon ? View.VISIBLE : View.GONE);
            if (hasIcon) holder.icon.setImageDrawable(icon);

            if (holder.descriptionIndex != -1) {
                final String description = cursor.getString(holder.descriptionIndex);
                if (description != null) {
                    holder.description.setText(description);
                    holder.description.setVisibility(View.VISIBLE);
                } else {
                    holder.description.setVisibility(View.GONE);                    
                }
            } else {
                holder.description.setVisibility(View.GONE);                
            }
        }

        if (holder.intentIndex != -1) {
            try {
                holder.intent = Intent.parseUri(cursor.getString(holder.intentIndex), 0);
            } catch (URISyntaxException e) {
                // Ignore
            }
        } else {
            holder.useBaseIntent = true;
        }
    }

    private Drawable loadIcon(Context context, Cursor cursor, ViewHolder holder) {
        Drawable icon = null;
        byte[] data = null;

        if (holder.iconBitmapIndex != -1) {
            data = cursor.getBlob(holder.iconBitmapIndex);
        }

        if (data != null) {
            final SoftReference<Drawable> reference = mCustomIcons.get(holder.id);
            if (reference != null) {
                icon = reference.get();
            }

            if (icon == null) {
                final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                /**
                 * mContext������ı������ڹ���ʱͨ��super(launcher, cursor, true)��
                 * launcher��ֵ��mContext�����mContext��mLauncher����ͬһ������
                 * modify by author
                 */
                //final Bitmap resampled = Utilities.resampleIconBitmap(bitmap, mContext);
                final Bitmap resampled = Utilities.resampleIconBitmap(bitmap, (Context)mLauncher);
                
                if (bitmap != resampled) {
                    // If we got back a different object, we don't need the old one any more.
                    bitmap.recycle();
                }
                icon = new FastBitmapDrawable(resampled);
                mCustomIcons.put(holder.id, new SoftReference<Drawable>(icon));
            }
        } else if (holder.iconResourceIndex != -1 && holder.iconPackageIndex != -1) {
            final String resource = cursor.getString(holder.iconResourceIndex);
            icon = mIcons.get(resource);
            if (icon == null) {
                try {
                    final PackageManager packageManager = context.getPackageManager();
                    Resources resources = packageManager.getResourcesForApplication(
                            cursor.getString(holder.iconPackageIndex));
                    final int id = resources.getIdentifier(resource,
                            null, null);
                    /**
                     * mContext������ı������ڹ���ʱͨ��super(launcher, cursor, true)��
                     * launcher��ֵ��mContext�����mContext��mLauncher����ͬһ������
                     * modify by author
                     */
                    //icon = new FastBitmapDrawable(
                    //        Utilities.createIconBitmap(resources.getDrawable(id), mContext));
                    icon = new FastBitmapDrawable(
                            Utilities.createIconBitmap(resources.getDrawable(id), mLauncher));
                    mIcons.put(resource, icon);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }

        return icon;
    }

    public void cleanup() {
        for (Drawable icon : mIcons.values()) {
            icon.setCallback(null);
        }
        mIcons.clear();

        for (SoftReference<Drawable> icon : mCustomIcons.values()) {
            final Drawable drawable = icon.get();
            if (drawable != null) {
                drawable.setCallback(null);
            }
        }
        mCustomIcons.clear();

        final Cursor cursor = getCursor();
        if (cursor != null) {
            try {
                cursor.close();
            } finally {
                mLauncher.stopManagingCursor(cursor);
            }
        }
    }

    public static class ViewHolder {
        public TextView name;
        public TextView description;
        public ImageView icon;

        public Intent intent;
        public long id;
        public boolean useBaseIntent;

        public int idIndex;
        public int nameIndex;
        public int descriptionIndex = -1;
        public int intentIndex = -1;
        public int iconBitmapIndex = -1;
        public int iconResourceIndex = -1;
        public int iconPackageIndex = -1;
    }
}
