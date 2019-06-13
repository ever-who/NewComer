package com.android.incallui;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.android.incallui.DialpadFragment;
import com.android.incallui.Log;

public class DTMFEntryFragment extends DialpadFragment{

    private EditText mDtmfDialerField;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        Log.d(this,"onCreateView");
        final View parent = inflater.inflate(R.layout.dtmf_entry_info_sprd_pikel, container, false);
        mDtmfDialerField = (EditText) parent.findViewById(R.id.digits);
        if (mDtmfDialerField != null) {
            mDialerKeyListener = new DTMFKeyListener();
            mDtmfDialerField.setKeyListener(mDialerKeyListener);
            // remove the long-press context menus that
            // supportconfigureKeypadListeners
            // the edit (copy / paste / select) functions.
            mDtmfDialerField.setLongClickable(false);
        }
        return parent;
    }

    @Override
    public boolean onDialerKeyDown(KeyEvent event) {
            return super.onDialerKeyDown(event);
    }

    @Override
    public boolean onDialerKeyUp(KeyEvent event) {
            return super.onDialerKeyUp(event);
    }

    public void initDigits(KeyEvent event) {
        Log.d(this,"initDigits");
        if (mDtmfDialerField == null || !mDtmfDialerField.isShown()) {
            return ;
        }
        char digit = mDialerKeyListener.lookup(event);
        mDtmfDialerField.getText().append(digit);
    }

}
