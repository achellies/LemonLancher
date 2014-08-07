/**  
 * 文 件 名:  SuggestionService.java  
 * 描    述：   
 * 版    权： Copyright (c)20010-2011 motone All Rights Reserved.
 * 公    司:  摩通科技 
 * 作    者：  黄悦                      
 * 版    本:  1.0  
 * 创建时间:   2010-12-15
 *  
 * 修改历史：  
 * 时间            作者         版本           描述  
 * ------------------------------------------------------------------  
 * 2010-12-15       黄悦       1.0          1.0 Version  
 */

package com.mogoo.launcher2;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.search.AppsSource;
import com.mogoo.launcher2.search.Source;
import com.mogoo.launcher2.search.WebSource;
import com.mogoo.launcher2.utils.Mogoo_BitmapCache;

public class Mogoo_SuggestionService extends Service {
    private SuggestionBinder mBinder = new SuggestionBinder();

    private static final boolean DBG = false;

    private static final String LOG_TAG = "GoogleSearch";

    private static final String USER_AGENT = "Android/1.0";

    private static final int HTTP_TIMEOUT_MS = 1000;

    // TODO: this should be defined somewhere
    private static final String HTTP_TIMEOUT = "http.connection-manager.timeout";

    private DefaultHttpClient mHttpClient;

    private String mSuggestUri;

    public static final int WEB_CURSOR_TYPE = 1;
    
    public static final int DEFAULT_CURSOR_TYPE = 0;

    public static final String KEYWORD_KEY = "query";

    public static final String CURSOR_TYPE_KEY = "cursor_type";

    private LauncherModel mLauncherModel;

    private Resources mRes;

    @Override
    public IBinder onBind(Intent intent) {
        int cursorType = intent.getIntExtra(CURSOR_TYPE_KEY, DEFAULT_CURSOR_TYPE);
        String keyword = intent.getStringExtra(KEYWORD_KEY);

        ArrayList<Source> sources = null;

        sources = queryApps(keyword);

        if (cursorType == WEB_CURSOR_TYPE) {
            ArrayList<Source> temp = queryWeb(keyword);
            if(temp != null){
                sources.addAll(temp);
                temp.clear();
                temp = null;
            }
        }
        
        if(mBinder.sources != null){
            mBinder.sources.clear();
        }
        mBinder.sources = null;
        mBinder.sources = sources;
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mHttpClient = new DefaultHttpClient();
        HttpParams params = mHttpClient.getParams();
        params.setLongParameter(HTTP_TIMEOUT, HTTP_TIMEOUT_MS);

        // NOTE: Do not look up the resource here; Localization changes may not
        // have completed
        // yet (e.g. we may still be reading the SIM card).
        mSuggestUri = null;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mHttpClient.clearResponseInterceptors();
        mHttpClient.clearRequestInterceptors();
        mHttpClient = null;
        mBinder = null;

        return super.onUnbind(intent);
    }

    public class SuggestionBinder extends Binder {
        private ArrayList<Source> sources;

        public Mogoo_SuggestionService getService() {
            return Mogoo_SuggestionService.this;
        }

        public ArrayList<Source> getSources() {
            return sources;
        }
    }

    private ArrayList<Source> queryApps(String query) {
        //add by 张永辉 改为从系统中读取
        ArrayList<Source> sources = new ArrayList<Source>();
        
        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final PackageManager packageManager = getPackageManager();
        List<ResolveInfo> apps = packageManager
                .queryIntentActivities(mainIntent, 0);
        
        LauncherApplication application = (LauncherApplication) getApplication();
        Mogoo_BitmapCache cache = application.getIconCache();
        
        List<ApplicationInfo> appInfos = new ArrayList<ApplicationInfo>();
        
        if (apps != null) {
            for (ResolveInfo ri : apps) {
                appInfos.add(new ApplicationInfo(ri, cache));
            }
        }
        
        for(ApplicationInfo info:appInfos){
            
            if (info.title != null && info.title.toString().toLowerCase().startsWith(query.toLowerCase())) {
              Drawable drawable = new BitmapDrawable(info.iconBitmap);
              AppsSource source = new AppsSource(info.title.toString(), drawable, info.intent);

              sources.add(source);
          }
        }
        
        return sources;
        //end 
    }

    /**
     * Queries for a given search term and returns a cursor containing
     * suggestions ordered by best match.
     */
    private ArrayList<Source> queryWeb(String query) {
        if (TextUtils.isEmpty(query)) {
            return null;
        }
        if (!isNetworkConnected()) {
            Log.i(LOG_TAG, "Not connected to network.");
            return null;
        }
        try {
            query = URLEncoder.encode(query, "UTF-8");
            // NOTE: This code uses resources to optionally select the search
            // Uri, based on the
            // MCC value from the SIM. iThe default string will most likely be
            // fine. It is
            // paramerterized to accept info from the Locale, the language code
            // is the first
            // parameter (%1$s) and the country code is the second (%2$s). This
            // code *must*
            // function in the same way as a similar lookup in
            // com.android.browser.BrowserActivity#onCreate(). If you change
            // either of these functions, change them both. (The same is true
            // for the underlying
            // resource strings, which are stored in mcc-specific xml files.)
            if (mSuggestUri == null) {
                Locale l = Locale.US;
                String language = "en";
                String country = "us";
                // Chinese and Portuguese have two langauge variants.
                mSuggestUri = getBaseContext().getResources().getString(
                        R.string.google_suggest_base, language, country)
                        + "&q=";
            }

            String suggestUri = mSuggestUri + query;
            if (DBG)
                Log.d(LOG_TAG, "Sending request: " + suggestUri);
            HttpGet method = new HttpGet(suggestUri);
            HttpResponse response = mHttpClient.execute(method);
            if (response.getStatusLine().getStatusCode() == 200) {

                /*
                 * Goto http://www.google.com/complete/search?json=true&q=foo to
                 * see what the data format looks like. It's basically a json
                 * array containing 4 other arrays. We only care about the
                 * middle 2 which contain the suggestions and their popularity.
                 */
                JSONObject resultJson = new JSONObject(EntityUtils.toString(response.getEntity()));
                ArrayList<Source> sources = getWebContent(resultJson);
                
                resultJson = null;
                // JSONArray results = new
                // JSONArray(resultJson.getJSONArray("responseData").);
                // JSONArray suggestions = results.getJSONArray(1);
                // JSONArray popularity = results.getJSONArray(2);
                if (DBG)
                    Log.d(LOG_TAG, "Got " + sources.size() + " results");
                return sources;// new WebCursor(suggestions, popularity);
            } else {
                if (DBG)
                    Log.d(LOG_TAG, "Request failed " + response.getStatusLine());
            }
            response.getEntity().consumeContent();
            response = null;
            method = null;
        } catch (UnsupportedEncodingException e) {
            Log.w(LOG_TAG, "Error", e);
        } catch (IOException e) {
            Log.w(LOG_TAG, "Error", e);
        } catch (JSONException e) {
            Log.w(LOG_TAG, "Error", e);
        }
        return null;
    }

    private ArrayList<Source> getWebContent(JSONObject resultJson) throws JSONException {
        int resultCode = resultJson.getInt("responseStatus");
        /*
         * {"content":"<b>Q<\/b> Magazine the UK&#39;s biggest music magazines,
         * music news, reviews and world exclusives with the most important
         * bands on the planet.","GsearchResultClass":"GwebSearch
         * ","titleNoFormatting":"Q Magazine | Music news &amp; reviews, music
         * videos, band pictures ...","title":"<b>Q<\/b> Magazine | Music news
         * &amp; reviews, music videos, band pictures <b>...
         * <\/b>","cacheUrl":"http:\/\/www.google.com\/search?q=cache
         * :3aIeKl4cbKAJ :www.qthemusic.com","unescapedUrl":"http:\/\/www.
         * qthemusic.com \/","url":"http:\/\/www.qthemusic.com\/","visibleUrl
         * ":"www.qthemusic.com"}
         */

        if (resultCode == 200) {
            ArrayList<Source> sources = new ArrayList<Source>();
            JSONObject responseData = resultJson.getJSONObject("responseData");
            JSONArray list = responseData.getJSONArray("results");
            JSONObject item = null;
            Uri sourceIconUri = null;
            for (int i = 0; i < list.length(); i++) {
                item = list.getJSONObject(i);
                sourceIconUri = Uri.parse(item.getString("url"));
                WebSource source = WebSource.create(sourceIconUri, item.getString("title"), item.getString("content"), null, null);
                sources.add(source);
            }
            
            item = null;
            sourceIconUri = null;
            list = null;
            responseData = null;

            return sources;
        }

        return null;
    }

    private boolean isNetworkConnected() {
        NetworkInfo networkInfo = getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private NetworkInfo getActiveNetworkInfo() {
        try {
            ConnectivityManager connectivity = (ConnectivityManager) getApplicationContext()
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

}
