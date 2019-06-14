package com.sprd.contacts.common;

import android.view.View;
import android.content.Context;
import android.text.Selection;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View.OnFocusChangeListener;

public class CustomSearchView extends EditText {

    public CustomSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
    }

    private OnChangeListener mListener;

    public interface OnChangeListener {
        void onChange(boolean focus, int position);
    }

    public void setOnChangeListener(OnChangeListener listener) {
        mListener = listener;
        //CustomSearchView not null
        addTextChangedListener(mTextWatcher);
        setOnFocusChangeListener(mOnFocusChangeListener);
    }

    public void unSetOnChangeListener() {
        mListener = null;
        removeTextChangedListener(mTextWatcher);
        setOnFocusChangeListener(null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = super.onKeyDown(keyCode, event);
        if (mListener != null) {
            mListener.onChange(true,getSelectionStart());
        }
        return result;
    }

    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (mListener != null) {
                mListener.onChange(true,getSelectionStart());
            }
        }
    };

    private View.OnFocusChangeListener mOnFocusChangeListener = new View.OnFocusChangeListener() {
        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            if (mListener != null) {
                mListener.onChange(hasFocus,getSelectionStart());
            }
        }
    };

}