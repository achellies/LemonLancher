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

package com.mogoo.launcher2.search.ui;

import com.mogoo.launcher.R;
import com.mogoo.launcher2.search.AppsSource;
import com.mogoo.launcher2.search.Source;
import com.mogoo.launcher2.search.SuggestionsAdapter;
import com.mogoo.launcher2.search.WebSource;
import com.mogoo.launcher2.utils.Mogoo_ClearBase;
import com.mogoo.launcher2.utils.Mogoo_ComponentBus;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Holds a list of suggestions.
 */
public class SuggestionsView extends ListView implements Mogoo_ClearBase{

    private static final boolean DBG = false;
    private static final String TAG = "QSB.SuggestionsView";

    private SuggestionClickListener mSuggestionClickListener;

    private SuggestionSelectionListener mSuggestionSelectionListener;

    private InteractionListener mInteractionListener;
    
    private SuggestionsAdapter suggestionsAdapter;
    
    public SuggestionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        setOnItemSelectedListener(new ItemSelectedListener());
        setOnItemClickListener(new ItemClickListener());
        setOnItemLongClickListener(new ItemLongClickListener());
    }

    public void setSuggestionClickListener(SuggestionClickListener listener) {
        mSuggestionClickListener = listener;
    }

    public void setSuggestionSelectionListener(SuggestionSelectionListener listener) {
        mSuggestionSelectionListener = listener;
    }

    public void setInteractionListener(InteractionListener listener) {
        mInteractionListener = listener;
    }

    /**
     * Gets the position of the selected suggestion.
     *
     * @return A 0-based index, or {@code -1} if no suggestion is selected.
     */
    public int getSelectedPosition() {
        return getSelectedItemPosition();
    }

    /**
     * Gets the selected suggestion.
     *
     * @return {@code null} if no suggestion is selected.
     */
//    public SuggestionPosition getSelectedSuggestion() {
//        return (SuggestionPosition) getSelectedItem();
//    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && mInteractionListener != null) {
            mInteractionListener.onInteraction();
        }
        return super.onInterceptTouchEvent(event);
    }
    
    public interface InteractionListener {
        /**
         * Called when the user interacts with this view.
         */
        void onInteraction();
    }
    
    @Override
    public void setAdapter(ListAdapter adapter) {
        if (adapter instanceof SuggestionsAdapter){
            super.setAdapter(adapter);
            suggestionsAdapter = (SuggestionsAdapter) adapter;
        } else {
            throw new IllegalArgumentException("SuggestionsView only accept SuggestionsAdapter");
        }
    }
    
    /**
     * 清除适配器
     * @ author: 黄悦
     */
    @SuppressWarnings("rawtypes")
    public void onClear() {
        suggestionsAdapter.clearAll();
    }
    
    /**
     * 
     * 清空搜索结果
     * @ author: 黄悦
     */
    public void clearList(){
        suggestionsAdapter.clear();
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (DBG) {
            Log.d(TAG, "Suggestions focus change, gainFocus: " + gainFocus
                    + ", selected=" + getSelectedItemPosition());
        }
        // In non-touch mode, ListView does not clear the list selection when
        // the ListView loses focus. And when it regains focus, onItemSelected() never gets
        // called if the new selected position is the same as the old. We work around that
        // by firing extra selection events on focus changes in non-touch mode.
        // This implementation can result in duplicate selection events when the old selected
        // item is not the same as the new.
        if (!isInTouchMode()) {
            if (gainFocus) {
                int position = getSelectedPosition();
                if (position != INVALID_POSITION) {
                    fireSuggestionSelected(position);
                }
            } else {
                fireNothingSelected();
            }
        }
    }

    private void fireSuggestionSelected(int position) {
        if (DBG) Log.d(TAG, "fireSuggestionSelected(" + position + ")");
        if (mSuggestionSelectionListener != null) {
            mSuggestionSelectionListener.onSuggestionSelected(position);
        }
    }

    private void fireNothingSelected() {
        if (DBG) Log.d(TAG, "fireNothingSelected()");
        if (mSuggestionSelectionListener != null) {
            mSuggestionSelectionListener.onNothingSelected();
        }
    }
    
    private class ItemClickListener implements AdapterView.OnItemClickListener {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (DBG) Log.d(TAG, "onItemClick(" + position + ")");
            
            Source source = suggestionsAdapter.getItem(position);
            if(source == null){
              return;
            }
            
            getContext().startActivity(source.getIntent());
            View editText = Mogoo_ComponentBus.getInstance().getActivityComp(R.id.search_src_text, getContext());
            if(editText != null){
                editText.clearFocus();
            }
        }
    }

    private class ItemLongClickListener implements AdapterView.OnItemLongClickListener {
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            if (DBG) Log.d(TAG, "onItemLongClick(" + position + ")");
            SuggestionView suggestionView = (SuggestionView) view;
            if (mSuggestionClickListener != null) {
                return mSuggestionClickListener.onSuggestionLongClicked(position);
            }
            return false;
        }
    }

    private class ItemSelectedListener implements AdapterView.OnItemSelectedListener {
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // Only fire suggestion selection events when the list has focus.
            // This suppresses selection events caused by data set changes (as opposed
            // to user action).
            if (hasFocus()) {
                fireSuggestionSelected(position);
            } else {
                if (DBG) Log.d(TAG, "Suppressed selection event for position " + position);
            }
        }

        public void onNothingSelected(AdapterView<?> parent) {
            fireNothingSelected();
        }
    }

}
