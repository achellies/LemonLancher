package com.limemobile.app.launcher.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import com.limemobile.app.launcher.adapter.ShortcutsAdapter;
import com.limemobile.app.launcher.common.LauncherSettings;
import com.limemobile.app.launcher.entity.ApplicationInfo;
import com.limemobile.app.launcher.entity.FolderInfo;
import com.limemobile.app.launcher.entity.ItemInfo;
import com.limemobile.app.launcher.entity.ShortcutInfo;
import com.limemobile.app.launcher.entity.UserFolderInfo;
import com.limemobile.app.launcher.receiver.LauncherModel;
import com.limemobile.app.launcher.wp8.R;

//import com.android.launcher.R; modify by author

/**
 * Folder which contains applications or shortcuts chosen by the user.
 *
 */
public class UserFolder extends Folder implements DropTarget {
    private static final String TAG = "Launcher.UserFolder";

    public UserFolder(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    /**
     * Creates a new UserFolder, inflated from R.layout.user_folder.
     *
     * @param context The application's context.
     *
     * @return A new UserFolder.
     */
    public static UserFolder fromXml(Context context) {
        return (UserFolder) LayoutInflater.from(context).inflate(R.layout.user_folder, null);
    }

    public boolean acceptDrop(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) {
        final ItemInfo item = (ItemInfo) dragInfo;
        final int itemType = item.itemType;
        return (itemType == LauncherSettings.Favorites.ITEM_TYPE_APPLICATION ||
                    itemType == LauncherSettings.Favorites.ITEM_TYPE_SHORTCUT)
                && item.container != mInfo.id;
    }
    
    public Rect estimateDropLocation(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo, Rect recycle) {
        return null;
    }

    public void onDrop(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) {
        ShortcutInfo item;
        if (dragInfo instanceof ApplicationInfo) {
            // Came from all apps -- make a copy
            item = ((ApplicationInfo)dragInfo).makeShortcut();
        } else {
            item = (ShortcutInfo)dragInfo;
        }
        ((ShortcutsAdapter)mContent.getAdapter()).add(item);
        LauncherModel.addOrMoveItemInDatabase(mLauncher, item, mInfo.id, 0, 0, 0);
    }

    public void onDragEnter(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) {
    }

    public void onDragOver(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) {
    }

    public void onDragExit(DragSource source, int x, int y, int xOffset, int yOffset,
            DragView dragView, Object dragInfo) {
    }

    @Override
    public void onDropCompleted(View target, boolean success) {
        if (success) {
            ShortcutsAdapter adapter = (ShortcutsAdapter)mContent.getAdapter();
            adapter.remove(mDragItem);
        }
    }

    public void bind(FolderInfo info) {
        super.bind(info);
        /**
         * mContext��View���еı������޷�ֱ�ӷ���
         * �޸�Ϊͨ��getContext()����
         * modify by author
         */
        setContentAdapter(new ShortcutsAdapter(getContext(), ((UserFolderInfo) info).contents));
    }

    // When the folder opens, we need to refresh the GridView's selection by
    // forcing a layout
    @Override
    public void onOpen() {
        super.onOpen();
        requestFocus();
    }
}
