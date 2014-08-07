/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.mogoo.launcher2.search;

import com.mogoo.launcher2.Mogoo_SuggestionService;
import com.mogoo.launcher2.Mogoo_SuggestionService.SuggestionBinder;
import com.mogoo.launcher2.search.ui.SuggestionView;
import com.mogoo.launcher2.search.ui.SuggestionViewInflater;
import com.mogoo.launcher2.search.ui.SuggestionsView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Uses a {@link Suggestions} object to back a {@link SuggestionsView}.
 */
public class SuggestionsAdapter extends ArrayAdapter<Source>{

    private static final boolean DBG = false;

    private static final String TAG = "QSB.SuggestionsAdapter";
    
    private int[] colors = new int[] { 0xFFDFDFDF, 0xFFCACACA };  
    
    private SuggestionViewInflater suggestionViewInflater;
    private Mogoo_SuggestionService suggestionService;
    private final DataSetObservable dataSetObservable = new DataSetObservable();
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            SuggestionBinder binder = (SuggestionBinder) service;
            SuggestionsAdapter.super.clear();
            updateData(binder.getSources());
            try{
                getContext().unbindService(serviceConnection);  
            }catch (Exception e) {
                Log.e(TAG, e.toString(), e);
            }
            
            suggestionService = binder.getService();
            dataSetObservable.notifyChanged();
        }

        public void onServiceDisconnected(ComponentName name) {
            suggestionService.stopSelf();
            suggestionService = null;
        }
    };
    
    public SuggestionsAdapter(Context context) {
        super(context, 0);
        suggestionViewInflater = new SuggestionViewInflater(context);
    }
    
    /**
     * 
     * 重写获得行view子项的方法
     * @ author: 黄悦
     *@return View
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Source source = getItem(position);
        int viewType = suggestionViewInflater.getSuggestionViewType(source);
        SuggestionView view = suggestionViewInflater.getSuggestionView(viewType, convertView, parent);
        view.bindAsSuggestion(source);
        ((View)view).setBackgroundColor(colors[position % 2]);
        
        return (View) view;
    }
    
    /**
     * 清空 SuggestionsAdapter
     * @ author: 黄悦
     */
    public void clearAll() {
        super.clear();
        suggestionViewInflater = null;
        serviceConnection = null;
        dataSetObservable.unregisterAll();
    }
    
    /**
     * 
     * 传入新的关键字更新搜索列表
     * @ author: 黄悦
     *@param query
     */
    public void updateSearchResult(String query, int type){
        Intent serviceIntent = new Intent();
        serviceIntent.setClass(getContext(), Mogoo_SuggestionService.class);
        serviceIntent.putExtra(Mogoo_SuggestionService.KEYWORD_KEY, query);
        serviceIntent.putExtra(Mogoo_SuggestionService.CURSOR_TYPE_KEY, type);
        getContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }
    
    public void unregisterObserverLayout(DataSetObserver observer){
        dataSetObservable.unregisterObserver(observer);
    }
    
    public void registerObserverLayout(DataSetObserver observer){
        dataSetObservable.registerObserver(observer);
    }
    
    /*
     * 添加数据到适配器
     */
    private void updateData(ArrayList<Source> sources){
        Iterator<Source> sourceItr = sources.iterator();
        super.clear();
        while(sourceItr.hasNext()){
            add(sourceItr.next());
            sourceItr.remove();
        }
        
        notifyDataSetInvalidated();
        sourceItr = null;
    }

}
