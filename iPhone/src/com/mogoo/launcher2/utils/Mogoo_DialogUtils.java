/**  
 * 文 件 名:  DialogUtils.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：  黄悦                     
 * 版    本:  1.0  
 * 创建时间:   2011-1-24
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2011-1-24        黄悦       1.0          1.0 Version  
 */        

package com.mogoo.launcher2.utils;

import java.util.List;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.Launcher;
import com.mogoo.launcher2.LauncherModel;
import com.mogoo.launcher2.ShortcutInfo;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.View;

public class Mogoo_DialogUtils {
    
    /**
     * 
     * 展开删除对话框
     * @ author: 黄悦
     */
    public static void showDelDialog(final Context context, final ShortcutInfo info){
        Dialog dialog22 = new AlertDialog.Builder(context)
        .setMessage(context.getString(R.string.mogoo_del_ask) + "   " + info.getTitle() + " ?")
        .setPositiveButton(context.getString(R.string.mogoo_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog1, int whichButton) {
                        if(info!=null&&info.getIntent()!=null){
                        	//edit by yeben 2012-4-20
                        	if(!checkedPackage(context,info)){
                        		((Launcher)context).removePackage(info.getIntent().getComponent().getPackageName());
                        	}else{
                                Uri uri = Uri.fromParts("package", info.getIntent().getComponent().getPackageName(), null);
                                Intent intent = new Intent(Intent.ACTION_DELETE, uri);
                                context.startActivity(intent);
                        	}
//                            Uri uri = Uri.fromParts("package", info.getIntent().getComponent().getPackageName(), null);
//                            Intent intent = new Intent(Intent.ACTION_DELETE, uri);
//                            context.startActivity(intent);
                        	//end
                        }
                    }
                })
        .setNeutralButton(context.getString(R.string.mogoo_cancal),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog1, int whichButton) {
                        dialog1.dismiss() ;
                    }
                }).create();
dialog22.show();//
    }
    /*
     * 判断package是否已经安装
     */
    private static boolean checkedPackage(final Context context, final ShortcutInfo info){
    	boolean flag = false;
    	if(info!=null&&info.getIntent()!=null){
    		String packageName = info.getIntent().getComponent().getPackageName();
            final PackageManager packageManager = context.getPackageManager();

            List<PackageInfo> packageInfos = packageManager.getInstalledPackages(0);
            if(packageInfos != null){
            	
                for (int i = 0; i < packageInfos.size(); i++) {
                     if(packageName.equals(packageInfos.get(i).packageName)){
                    	 flag = true;
                    	 break;
                     }
    			}
            }
            
    	}
        return flag;
    }
}
