/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.mogoo.launcher2.search.ui;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.Launcher;
import com.mogoo.launcher2.Mogoo_SuggestionService;
import com.mogoo.launcher2.search.SuggestionsAdapter;
import com.mogoo.launcher2.utils.Mogoo_ClearBase;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;

/**
 * 搜索屏布局类，包括广告展示、app搜索、web搜索
 */
public class SearchLayout extends RelativeLayout implements Mogoo_ClearBase {
    private static final String TAG = "LANCH.SearchActivity";

    private static final boolean DBG = true;

    private static final long TYPING_SUGGESTIONS_UPDATE_DELAY_MILLIS = 100;
    public static final int SEARCH_SCREEN_ALPHA = 150;

    private InputMethodManager inputMethodManager;
    private SuggestionsAdapter suggestionsAdapter;
    
    private SuggestionsView suggestionsView;
    private ImageButton searchClearButton;
    private ImageButton searchButton;
//    private MT_MotoneWebView webView;
    private Mogoo_ComponentBus bus;
    
    private Handler handler = new Handler();
    
    private boolean queryWasEmpty = true;
    private boolean updateSuggestions = true;
    private boolean searchWeb = false;
    private String query;
    
    private Runnable updateSuggestionsTask = new Runnable() {
        public void run() {
            updateSuggestions(query, searchWeb ? Mogoo_SuggestionService.WEB_CURSOR_TYPE : Mogoo_SuggestionService.DEFAULT_CURSOR_TYPE);
        }
    };
    
    private DataSetObserver suggestionsAdapterDataSet = new DataSetObserver(){
        @Override
        public void onChanged() {
            updateSuggestions = true;
            searchButton.setEnabled(true);
        }
    };

    // private Corpus mCorpus;

    // The string used for privateImeOptions to identify to the IME that it
    // should not show
    // a microphone button since one already exists in the search dialog.
    // TODO: This should move to android-common or something.
    private static final String IME_OPTION_NO_MICROPHONE = "nm";

    public SearchLayout(Context context) {
        this(context, null);
    }

    public SearchLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SearchLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initSearchLayout(context);
    }

    /**
     * 装载所有search layout组件及相应事件 @ author: 黄悦
     */
    public void setupView() {
        Mogoo_ComponentBus bus = Mogoo_ComponentBus.getInstance();
        EditText queryTextView = setupEditText();

        setupSearchButton(queryTextView);
        setupClearButton(queryTextView);
        setupSuggestionView(bus);

//        webView = (MT_MotoneWebView) findViewById(R.id.motone_web);
//        webView.loadPage();
    }
    
    /**
     * 清除SearchLayout的相关组件 @ author: 黄悦
     */
    public void onClear() {
        suggestionsView.onClear();
        inputMethodManager = null;
        suggestionsView = null;
        searchClearButton = null;
        suggestionsAdapter = null;
        bus = null;
//        webView.clearCache(true);
//        webView.clearView();
//        webView = null;
    }
    
    /**
     * Handles non-text keys in the query text view.
     */
    private class QueryTextViewKeyListener implements View.OnKeyListener {
        public boolean onKey(View view, int keyCode, KeyEvent event) {
            // Handle IME search action key
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                // onSearchClicked(Logger.SEARCH_METHOD_KEYBOARD);
            }
            return false;
        }
    }

    /**
     * 输入框获得焦点或失去焦点键盘的活动事件
     * @ author: 黄悦
     * 
     */
    private class QueryTextViewFocusListener implements OnFocusChangeListener {
        public void onFocusChange(View v, boolean focused) {
            if (DBG)
                Log.d(TAG, "Query focus change, now: " + focused);
            if (focused) {
                showSoftInput(v);
            } else {
                hideSoftInput(v);
            }
        }
    }
    
    /*
     * 切换广告/搜索结果显示
     */
    private void updateMotoneWeb(boolean queryEmpty) {
        if (queryEmpty) {
            suggestionsView.setVisibility(View.INVISIBLE);
            searchClearButton.setVisibility(View.INVISIBLE);
            findViewById(R.id.suggestions_sohw).setVisibility(View.INVISIBLE);
//            webView.setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.suggestions_sohw).setVisibility(View.VISIBLE);
            suggestionsView.setVisibility(View.VISIBLE);
            searchClearButton.setVisibility(View.VISIBLE);
//            webView.setVisibility(View.INVISIBLE);
        }
    }
    
    /*
     * 装载关键字输入框
     */
    private EditText setupEditText() {
        final EditText queryTextView = (EditText) findViewById(R.id.search_src_text);
        //denglixia add 2011.6.9
        queryTextView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        queryTextView.setOnKeyListener(new QueryTextViewKeyListener());
        queryTextView.setOnFocusChangeListener(new QueryTextViewFocusListener());

        queryTextView.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(Editable s) {
                boolean empty = s.length() == 0;
                if (empty != queryWasEmpty) {
                    queryWasEmpty = empty;
                    updateMotoneWeb(empty);
                }
                if (updateSuggestions) {
                    updateSuggestions = false;
                    query = s == null ? "" : s.toString();
                    searchWeb = false;
                    updateSuggestionsBuffered();
                }
            }

        });
        
        bus.addActivityComp(R.id.search_src_text, queryTextView, getContext());

        return queryTextView;
    }
    
    /*
     * 搜索频率缓冲
     */
    private void updateSuggestionsBuffered() {
        handler.removeCallbacks(updateSuggestionsTask);
        handler.postDelayed(updateSuggestionsTask, TYPING_SUGGESTIONS_UPDATE_DELAY_MILLIS);
    }
    
    /*
     * 对关键字进行搜索
     */
    private void updateSuggestions(String q, int type) {
        q = ltrim(q);
        if(q == null || q.length() == 0){
            suggestionsAdapter.clear();
            updateSuggestions = true;
        }else{
            searchButton.setEnabled(false);
            suggestionsAdapter.updateSearchResult(q, type);
        }
    }
    
    /*
     * 装载搜索结果列表
     */
    private void setupSuggestionView(Mogoo_ComponentBus bus) {
        suggestionsView = (SuggestionsView) findViewById(R.id.suggestions);
        suggestionsAdapter = new SuggestionsAdapter(getContext());
        
        suggestionsView.setAdapter(suggestionsAdapter);
        suggestionsAdapter.registerObserverLayout(suggestionsAdapterDataSet);
        
        bus.addActivityComp(R.id.suggestions, suggestionsView, (Activity) getContext());
    }

    /*
     * 装载搜索结果清空
     */
    private void setupClearButton(final EditText queryTextView) {
        searchClearButton = (ImageButton) findViewById(R.id.search_clear_btn);
        searchClearButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                query = "";
                queryTextView.setText(null);
                showSoftInput(queryTextView);
                suggestionsView.clearList();
            }
        });
    }

    /*
     * 装载搜索案件点击事件
     */
    private void setupSearchButton(final EditText queryTextView) {
        searchButton = (ImageButton) findViewById(R.id.search_go_btn);

//        searchButton.setOnClickListener(new OnClickListener() {
//            public void onClick(View view) {
//                CharSequence q = queryTextView.getText();
//                query = ltrim(q == null ? "" : q.toString());
//                searchWeb = true;
//                updateSuggestionsBuffered();
//            }
//        });
    }

    /*
     * 搜索关键字处理
     */
    private static String ltrim(String text) {
        int start = 0;
        int length = text.length();
        while (start < length && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        return start > 0 ? text.substring(start, length) : text;
    }

    /*
     * 弹出软键盘
     */
    private void showSoftInput(View view) {
        getInputMethodManager();
        inputMethodManager.showSoftInput(view, 0);
        view.requestFocus();
    }

    /*
     * 隐藏软键盘
     */
    private void hideSoftInput(View view) {
        getInputMethodManager();
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(),
                InputMethodManager.HIDE_NOT_ALWAYS);
    }

    /*
     * 初始化searchLayout
     */
    private void initSearchLayout(Context context) {
        getInputMethodManager();
        bus = Mogoo_ComponentBus.getInstance();
    }
    
    /*
     * 获得软键盘开启入口
     */
    private void getInputMethodManager() {
        if(inputMethodManager == null){
            inputMethodManager = ((InputMethodManager) getContext()
                    .getSystemService(Context.INPUT_METHOD_SERVICE));
        }
    }

}
