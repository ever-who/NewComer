/*
 * Copyright (C) 2013 Spreadtrum Communications Inc. 
 *
 */

package com.sprd.incallui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.PhoneLookup;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.incallui.Log;
import com.android.incallui.R;


/**
 * Phone app "in call" screen.
 */
public class SaveContacts extends Activity implements View.OnClickListener {
    private static final String LOG_TAG = "SaveContacts";
    private Button mNewContactsButton;
    private Button mEditContactsButton;
    private Button mBackButton;
    private TextView mName;
    private TextView mElapsedTime;
    public static String PHONE_NAME = "phonenumber";
    public static String CALL_TIME = "calltime";
    private static int DISPLAY_TIME_DELAY = 5000;

    private Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            handler.removeMessages(DISPLAY_TIME_DELAY);
            SaveContacts.this.finish();
            super.handleMessage(msg);
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.save_contacts_sprd);
        mNewContactsButton = (Button) this.findViewById(R.id.ContactsNew);
        mNewContactsButton.setOnClickListener(this);
        mEditContactsButton = (Button) this.findViewById(R.id.ContactsAdd);
        mEditContactsButton.setOnClickListener(this);
        mBackButton = (Button) this.findViewById(R.id.backButton);
        mBackButton.setOnClickListener(this);
        mName = (TextView) this.findViewById(R.id.name);
        mElapsedTime = (TextView) this.findViewById(R.id.elapsedTime);
        getActionBar().hide();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        parseIntent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        parseIntent();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.ContactsNew:
                this.finish();
                startCreateNewContactActivity(mName.getText().toString());
                break;
            case R.id.ContactsAdd:
                this.finish();
                startAddContactActivity(mName.getText().toString());
                break;
            case R.id.backButton:
                this.onBackPressed();
                break;
            default:
                Log.w(LOG_TAG, "onClick,view ID ="+view.getId()+"  is invalid!");
        }
    }

    private void parseIntent() {
        Intent intent = this.getIntent();
        String strPhoneNumber = intent.getStringExtra(PHONE_NAME);
        String calltime = intent.getStringExtra(CALL_TIME);
        if (mElapsedTime != null) {
            if (calltime == null) {
                mElapsedTime.setVisibility(View.GONE);
            } else {
                mElapsedTime.setVisibility(View.VISIBLE);
                mElapsedTime.setText(calltime);
            }
        }

        if (mName != null) {
            if (strPhoneNumber != null && !strPhoneNumber.isEmpty()) {
                mName.setText(strPhoneNumber);
            }
        }

        if(handler != null){
            Message msg =  handler.obtainMessage();
            msg.what = DISPLAY_TIME_DELAY;
            handler.sendMessageDelayed(msg, DISPLAY_TIME_DELAY);
        }
    }

    public static boolean checkPhoneNumber(String number,Context context) { 
        Cursor cursor = null;
        boolean result = false;
        if(number == null || TextUtils.isEmpty(number)){
            Log.i("SaveContacts","The number is empty!");
            return true;
        }
        try {
            Uri contactUri = Uri.withAppendedPath(PhoneLookup.CONTENT_FILTER_URI,
                    Uri.encode(number));
            cursor = context.getContentResolver().query(contactUri,
                    new String[] { PhoneLookup.NORMALIZED_NUMBER}, null, null, null);
            int count = 0;
            if (cursor != null && cursor.getCount()>0) {
                result = true;
                count = cursor.getCount();
            }
            Log.i("SaveContacts","checkPhoneNumber,number="+number+" cursor.getCount()="+count);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            return result;
        }
    }

    private void startCreateNewContactActivity(String number) {
        Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
        intent.putExtra(Insert.PHONE, number);
        Bundle extras = new Bundle();
        extras.putString(Insert.PHONE, number);
        intent.putExtras(extras);
        startActivity(intent);
    }

    private void startAddContactActivity(String number){
        Intent intent = new Intent(Intent.ACTION_INSERT_OR_EDIT);
        intent.setType(Contacts.CONTENT_ITEM_TYPE);
        intent.putExtra(Insert.PHONE, number);
        startActivity(intent);
    }
}