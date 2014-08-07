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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.ContentResolver;
import android.database.Cursor;
import android.util.Log;
import android.widget.Toast;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.config.Mogoo_GlobalConfig;

public class InstallShortcutReceiver extends BroadcastReceiver {
    
    private static final String TAG = "Launcher.InstallShortcutReceiver" ;
    
    private static final String ACTION_INSTALL_SHORTCUT =
            "com.mogoo.launcher.action.INSTALL_SHORTCUT";
    
    //update by 张永辉 2011-1-22
    private  int[] mCoordinates ;
    
    public void onReceive(Context context, Intent data) {
        if (!ACTION_INSTALL_SHORTCUT.equals(data.getAction())) {
            return;
        }

        if(Mogoo_GlobalConfig.LOG_DEBUG){
            Log.d(TAG, "start install shortcut...");
        }
        
        int screen = Launcher.getScreen();
        
        //add by 张永辉 2011-1-22
        //标识是否己经安装
        boolean isSetup = false ;
        
        //如果当前屏为快捷方式屏,并且快捷方式安装在当前屏成功
        if(Mogoo_GlobalConfig.isShortcutScreen(screen)&&installShortcut(context, data, screen)){
            isSetup = true ;
        }
        
        //如果没有安装成功，则依次轮流在各个快捷方式屏中安装，直接到安装成功为止
        if(!isSetup){
            int [] shortcutScreen = Mogoo_GlobalConfig.getShortcutScreen() ;
            
            for(int index : shortcutScreen){
                if(index != screen && installShortcut(context, data, index)){
                    break ;
                }
            }
        }
        //end 
        
        //delete by 张永辉 2011-1-22 更改安装方案
//        if (!installShortcut(context, data, screen)) {
//            // The target screen is full, let's try the other screens
//            for (int i = 0; i < Launcher.SCREEN_COUNT; i++) {
//                if (i != screen && installShortcut(context, data, i)) break;
//            }
//        }
        //end 
    }

    private boolean installShortcut(Context context, Intent data, int screen) {
        String name = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        
        //update by 张永辉 2011-1-22
        mCoordinates = CellLayout.findBlackCell(context, screen);

//        if (findEmptyCell(context, mCoordinates, screen)) {
        if (mCoordinates!=null) {
        //end 
            CellLayout.CellInfo cell = new CellLayout.CellInfo();
            cell.cellX = mCoordinates[0];
            cell.cellY = mCoordinates[1];
            cell.screen = screen;

            Intent intent = data.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);

            if (intent.getAction() == null) {
                intent.setAction(Intent.ACTION_VIEW);
            }

            // By default, we allow for duplicate entries (located in
            // different places)
            boolean duplicate = data.getBooleanExtra(Launcher.EXTRA_SHORTCUT_DUPLICATE, true);
            if (duplicate || !LauncherModel.shortcutExists(context, name, intent)) {
                ((LauncherApplication)context.getApplicationContext()).getModel()
                        .addShortcut(context, data, cell, true);
                Toast.makeText(context, context.getString(R.string.shortcut_installed, name),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, context.getString(R.string.shortcut_duplicate, name),
                        Toast.LENGTH_SHORT).show();
            }

            return true;
        } else {
            Toast.makeText(context, context.getString(R.string.out_of_space),
                    Toast.LENGTH_SHORT).show();
        }

        return false;
    }

    private static boolean findEmptyCell(Context context, int[] xy, int screen) {
        final int xCount = Launcher.NUMBER_CELLS_X;
        final int yCount = Launcher.NUMBER_CELLS_Y;

        boolean[][] occupied = new boolean[xCount][yCount];

        final ContentResolver cr = context.getContentResolver();
        Cursor c = cr.query(LauncherSettings.Favorites.CONTENT_URI,
            new String[] { LauncherSettings.Favorites.CELLX, LauncherSettings.Favorites.CELLY,
                    LauncherSettings.Favorites.SPANX, LauncherSettings.Favorites.SPANY },
            LauncherSettings.Favorites.SCREEN + "=?",
            new String[] { String.valueOf(screen) }, null);

        final int cellXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLX);
        final int cellYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.CELLY);
        final int spanXIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANX);
        final int spanYIndex = c.getColumnIndexOrThrow(LauncherSettings.Favorites.SPANY);

        try {
            while (c.moveToNext()) {
                int cellX = c.getInt(cellXIndex);
                int cellY = c.getInt(cellYIndex);
                int spanX = c.getInt(spanXIndex);
                int spanY = c.getInt(spanYIndex);

                for (int x = cellX; x < cellX + spanX && x < xCount; x++) {
                    for (int y = cellY; y < cellY + spanY && y < yCount; y++) {
                        occupied[x][y] = true;
                    }
                }
            }
        } catch (Exception e) {
        	if (c != null)
        		c.close();
            return false;
        } finally {
        	if (c != null)
        		c.close();
        }

        return CellLayout.findVacantCell(xy, 1, 1, xCount, yCount, occupied);
    }
}
