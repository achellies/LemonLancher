/**  
 * 文   件  名:   MotoneWebView.java  
 * 描          述:   
 * 版          权:   Copyright (c)20010-2011 motone All Rights Reserved.
 * 公          司:   摩通科技 
 * 作           者：  黄悦                    
 * 版          本:   1.0  
 * 创建时间:   2010-12-1
 *  
 * 修改历史:  
 * 时间                          作者                                版本                        描述  
 * ------------------------------------------------------------------  
 * 2010-12-1     黄悦           1.0         1.0 Version  
 */

package com.mogoo.launcher2.search.ui;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.mogoo.launcher.R;

public class Mogoo_MotoneWebView extends WebView {
    private static final boolean DBG = false;

    private static final String LOG_TAG = "MotoneSearchView";

    public Mogoo_MotoneWebView(Context context) {
        this(context, null);
    }

    public Mogoo_MotoneWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setInitialScale(25);
        WebSettings webSettings = getSettings();  
        webSettings.setJavaScriptEnabled(true);  
        webSettings.setBuiltInZoomControls(true);
    }

    /**
     * 加载页面内容 
     * @ author: 黄悦
     */
    public void loadPage() {
        final CharSequence motoneURL = getContext().getText(R.string.motone_index_page);

        new Handler().post(new Runnable() {
            public void run() {
                try {
                    if (motoneURL != null && isNetworkConnected()) {
                        loadUrl(motoneURL.toString());
                    } else {
//                        if (mMotoneWebHistory != null && mMotoneWebHistory.hasHistory()) {
//                            loadLastPage();
//                        } else {
                            String encoding = getContext().getText(R.string.web_init_encoding).toString();
                            loadDataWithBaseURL("fake://not/needed", readStringFromAsset(encoding),"text/html", encoding, "");  
//                        }
                    }
                } catch (Exception e) {
                    Log.w(LOG_TAG, e);
                }
            }

        });
    }
    
//    private void loadLastPage() {
//        Cursor cursor = mMotoneWebHistory.getLastPage();
//        cursor.moveToLast();
//        int htmlIndex = cursor.getColumnIndex(MotoneWebHistory.html_content.fullName);
//        int typeIndex = cursor.getColumnIndex(MotoneWebHistory.mime_type.fullName);
//        int encodingIndex = cursor.getColumnIndex(MotoneWebHistory.encoding.fullName);
//        loadData(cursor.getString(htmlIndex), cursor.getString(typeIndex),
//                cursor.getString(encodingIndex));
//    }

    /*
     * 读取asset文件夹文件
     */
    private String readStringFromAsset(String encoding) {
        InputStream is = null;

        try {
            AssetManager am = getContext().getAssets();
            CharSequence fileName = getContext().getText(R.string.web_init_buff);
            is = am.open(fileName.toString());

            fileName = null;
            am = null;
            
            byte[] buff = new byte[is.available()];
            is.read(buff);
            
            return new String(buff);
        } catch (Exception e) {
            Log.e("readStringFromAsset", e.getMessage(), e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                    is = null;
                }
            } catch (IOException e) {
                Log.e("readStringFromAsset", e.getMessage(), e);
            }
        }

        return null;
    }

//    /**
//     * 保存当前页面，当下次进入搜索无连接时，调用保存html @ author: 黄悦
//     * 
//     * @throws IOException
//     * @throws ClientProtocolException
//     */
//    public void saveCurrentPage() {
//        mMotoneWebHistory.updateLastPage(getUrl());
//    }

    private boolean isNetworkConnected() {
        NetworkInfo networkInfo = getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private NetworkInfo getActiveNetworkInfo() {
        try {
            ConnectivityManager connectivity = (ConnectivityManager) getContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity == null) {
                return null;
            }
            return connectivity.getActiveNetworkInfo();
        } catch (Exception e) {
            Log.w(LOG_TAG, e);
        }

        return null;
    }

//    public MT_MotoneWebHistory getMotoneWebHistory() {
//        return mMotoneWebHistory;
//    }
//
//    public void setMotoneWebHistory(MT_MotoneWebHistory MotoneWebHistory) {
//        this.mMotoneWebHistory = MotoneWebHistory;
//    }

}
