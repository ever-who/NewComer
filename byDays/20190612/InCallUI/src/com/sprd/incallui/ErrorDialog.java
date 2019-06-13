/*
 * SPRD: create
 */

package com.sprd.incallui;

import java.util.Locale;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

public class ErrorDialog extends DialogFragment {
    private static final String TAG = "ErrorDialog";
    private static final String SAVED_DIALOG_MSG_KEY = "message_key";
    private static final String SAVED_DIALOG_LANGUAGE = "language_key";
    private String mMessage = null;

    /** Preferred way to show this dialog */
    public static void showDialog(FragmentManager fragmentManager, String msg) {
        ErrorDialog dialog = new ErrorDialog(msg);
        dialog.show(fragmentManager, TAG);
    }

    /**
     * had better not called this function directly in other class, if you want
     * to show dialog, you can call {@link #showDialog(FragmentManager, String)}
     * instead.
     */
    public ErrorDialog(String msg) {
        mMessage = msg;
    }

    /** had better not called this function directly */
    public ErrorDialog() {
        this(null);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && mMessage == null) {
            mMessage = savedInstanceState.getString(SAVED_DIALOG_MSG_KEY);
            if(!Locale.getDefault().getLanguage().equals(savedInstanceState.getString(SAVED_DIALOG_LANGUAGE)))
                dismiss();
            Log.d(TAG, "get msg from saved state.");
        }
        if (mMessage == null) {
            throw new NullPointerException("Can not be null.");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(mMessage)
                .setPositiveButton(android.R.string.ok, null)
                .setCancelable(true)
                .create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (!TextUtils.isEmpty(mMessage)) {
            outState.putString(SAVED_DIALOG_MSG_KEY, mMessage);
            outState.putString(SAVED_DIALOG_LANGUAGE, Locale.getDefault().getLanguage());
        }
        super.onSaveInstanceState(outState);
    }

}
