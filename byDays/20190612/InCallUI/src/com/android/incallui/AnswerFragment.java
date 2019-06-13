/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.incallui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.ViewStub.OnInflateListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.sprd.incallui.MultiPartCallHelper;
import com.sprd.incallui.OnViewChangeListener;
import com.sprd.incallui.SmsRejectAdapter;
import com.sprd.incallui.SprdUtils;
import com.sprd.incallui.VerticalScrollLayout;

import java.util.ArrayList;

import com.android.services.telephony.common.AudioMode;
import com.android.services.telephony.common.Call;
/**
 *
 */
public class AnswerFragment extends BaseFragment<AnswerPresenter, AnswerPresenter.AnswerUi>
        implements GlowPadWrapper.AnswerListener, AnswerPresenter.AnswerUi 
        ,View.OnClickListener { // SPRD: add for Universe UI 

    /**
     * The popup showing the list of canned responses.
     *
     * This is an AlertDialog containing a ListView showing the possible choices.  This may be null
     * if the InCallScreen hasn't ever called showRespondViaSmsPopup() yet, or if the popup was
     * visible once but then got dismissed.
     */
    private Dialog mCannedResponsePopup = null;

    /**
     * The popup showing a text field for users to type in their custom message.
     */
    private AlertDialog mCustomMessagePopup = null;

    private AlertDialog mAnswerMpcPopup = null;

    private ArrayAdapter<String> mTextResponsesAdapter = null;

    private GlowPadWrapper mGlowpad;
    
    /**
     * SPRD: 
     * add for Universe UI 
     * @{
     */
    public static final int LOCKSCREEN_REJECT = 0;
    public static final int LOCKSCREEN_ANSWER = 1;
    public static final int LOCKSCREEN_HEIGHT = 300;
    
    private View mIncomingCallControls;
    private ViewStub mIncomingCallControlsStub;
    private View mFreeHandsButton;
    private View mSendMessageButton;
    private Button mRejectButton;
    private Button mAnswerButton;
    private Button mEndCallButton;
    private View mIncomingMuteButton;
    private View mSmsRejectListLayout;
    private ViewStub mSmsRejectListStubView;
    private View mIncomingCallControlsLockScreen;
    private ViewStub mIncomingCallControlsLockScreenStub;
    private VerticalScrollLayout mIncomingCallScrollLayout;
    private ListView mIncomingRejectList;
    private TextView mDisplayName;
    private Context mContext;
    private ImageView mSlideLeft;
    private ImageView mSlideRight;
    @Override
    public void onAttach(Activity activity){
        mContext = activity;
        super.onAttach(activity);
    }
    /*@}
     */ 

    public AnswerFragment() {
    }
    

    @Override
    public AnswerPresenter createPresenter() {
        return new AnswerPresenter();
    }

    @Override
    AnswerPresenter.AnswerUi getUi() {
        return this;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        // SPRD: add for Universe UI
        if(SprdUtils.PIKEL_UI_SUPPORT){
            View view = inflater.inflate(R.layout.answer_fragment_sprd_pikel,
                    container, false);

            Log.d(this, "Creating view for answer fragment ", this);
            Log.d(this, "Created from activity", getActivity());
            return view;
        } else if (SprdUtils.UNIVERSE_UI_SUPPORT) {
            View answerView = inflater.inflate(R.layout.answer_fragment_sprd,
                    container, false);
            mSmsRejectListStubView = (ViewStub)answerView.findViewById(R.id.smsRejectListStubView);
            mIncomingCallControlsStub = (ViewStub)answerView.findViewById(R.id.inComingCallControlsStub);
            mIncomingCallControlsLockScreenStub = (ViewStub)answerView.findViewById(R.id.inComingCallControlsLockedStub);
            mInflater = inflater;
            return answerView;
        } else {
                mGlowpad = (GlowPadWrapper) inflater.inflate(R.layout.answer_fragment,
                        container, false);

                Log.d(this, "Creating view for answer fragment ", this);
                Log.d(this, "Created from activity", getActivity());
                mGlowpad.setAnswerListener(this);

                return mGlowpad;
            }
        }

    @Override
    public void onDestroyView() {
        Log.d(this, "onDestroyView");
        if (mGlowpad != null && !SprdUtils.PIKEL_UI_SUPPORT) {
            mGlowpad.stopPing();
            mGlowpad = null;
        }
        getPresenter().dismissHangupCallDialog();
        super.onDestroyView();
    }

    @Override
    public void showAnswerUi(boolean show) {
        getView().setVisibility(show ? View.VISIBLE : View.GONE);

        Log.d(this, "Show answer UI: " + show);
        // SPRD: add for Universe UI 
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            if(show){
                if(SprdUtils.getKeyGuardStatus(getActivity())){
                    if(mIncomingCallControlsLockScreenStub != null && mIncomingCallControlsLockScreen == null){
                        initIncomingLockScreenView();
                        mIncomingCallControlsLockScreen.setVisibility(View.VISIBLE);
                    } else if(mIncomingCallControlsLockScreen != null){
                        loadSmsRejectList(mContext,mIncomingRejectList);
                        mIncomingCallControlsLockScreen.setVisibility(View.VISIBLE);
                    }
                    if(mIncomingCallControls != null){
                        mIncomingCallControls.setVisibility(View.GONE);
                    }
                    if(mIncomingCallScrollLayout != null){
                        mIncomingCallScrollLayout.resetScrollLayout();
                    }
                } else{
                    if(mIncomingCallControlsStub != null && mIncomingCallControls == null){
                        initIncomingControlsView();
                        mIncomingCallControls.setVisibility(View.VISIBLE);
                    } else if(mIncomingCallControls != null) {
                        mIncomingCallControls.setVisibility(View.VISIBLE);
                    }
                    if(mIncomingCallControlsLockScreen != null){
                        mIncomingCallControlsLockScreen.setVisibility(View.GONE);
                    }
                    updateFreeHandsButton();
                }
            }
        } else {
            if (!SprdUtils.PIKEL_UI_SUPPORT) {
                if (show) {
                    mGlowpad.startPing();
                } else {
                    mGlowpad.stopPing();
                }
            }
        }
        /* SPRD: add for Multi-Part-Call,commented this code to show MPC dialog @ { */
        //if (mContext != null) {
        //getPresenter().dealIncomingThirdCall(mContext, show);
        //}
        /* @} */
    }

    @Override
    public void showTextButton(boolean show) {
        // SPRD: add for Universe UI
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            Log.w(this,"UNIVERSE_UI_SUPPORT,showTextButton just return");
            return;
        }
        final int targetResourceId = show
                ? R.array.incoming_call_widget_3way_targets
                : R.array.incoming_call_widget_2way_targets;

        if (!SprdUtils.PIKEL_UI_SUPPORT && targetResourceId != mGlowpad.getTargetResourceId()) {
            if (show) {
                // Answer, Decline, and Respond via SMS.
                mGlowpad.setTargetResources(targetResourceId);
                mGlowpad.setTargetDescriptionsResourceId(
                        R.array.incoming_call_widget_3way_target_descriptions);
                mGlowpad.setDirectionDescriptionsResourceId(
                        R.array.incoming_call_widget_3way_direction_descriptions);
            } else {
                // Answer or Decline.
                mGlowpad.setTargetResources(targetResourceId);
                mGlowpad.setTargetDescriptionsResourceId(
                        R.array.incoming_call_widget_2way_target_descriptions);
                mGlowpad.setDirectionDescriptionsResourceId(
                        R.array.incoming_call_widget_2way_direction_descriptions);
            }

            mGlowpad.reset(false);
        }
    }

    @Override
    public void showMessageDialog() {
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            displayMessageList(true); // SPRD: add for Universe UI 
        } else {
            final ListView lv = new ListView(getActivity());

            Preconditions.checkNotNull(mTextResponsesAdapter);
            lv.setAdapter(mTextResponsesAdapter);
            lv.setOnItemClickListener(new RespondViaSmsItemClickListener());

            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setCancelable(
                    true).setView(lv);
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    if (mGlowpad != null && !SprdUtils.PIKEL_UI_SUPPORT) {
                        mGlowpad.startPing();
                    }
                    dismissCannedResponsePopup();
                    getPresenter().onDismissDialog();
                }
            });
            mCannedResponsePopup = builder.create();
            mCannedResponsePopup.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
            mCannedResponsePopup.show();
        }
    }

    private boolean isCannedResponsePopupShowing() {
        /* SPRD: add for Universe UI @ { */
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            return isMessageListDisplayed();
        }
        /* @} */
        if (mCannedResponsePopup != null) {
            return mCannedResponsePopup.isShowing();
        }
        return false;
    }

    private boolean isCustomMessagePopupShowing() {
        if (mCustomMessagePopup != null) {
            return mCustomMessagePopup.isShowing();
        }
        return false;
    }
    private boolean isAnswerPopupShowing() {
        if (mAnswerMpcPopup != null) {
            return mAnswerMpcPopup.isShowing();
        }
        return false;
    }
    /**
     * Dismiss the canned response list popup.
     *
     * This is safe to call even if the popup is already dismissed, and even if you never called
     * showRespondViaSmsPopup() in the first place.
     */
    private void dismissCannedResponsePopup() {
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            displayMessageList(false);// SPRD: add for Universe UI 
        } else{
            if (mCannedResponsePopup != null) {
                mCannedResponsePopup.dismiss();  // safe even if already dismissed
                mCannedResponsePopup = null;
            }
        }
    }

    /**
     * Dismiss the custom compose message popup.
     */
    private void dismissCustomMessagePopup() {
       if (mCustomMessagePopup != null) {
           mCustomMessagePopup.dismiss();
           mCustomMessagePopup = null;
       }
    }

    /**
     * Dismiss the answer choice popup.
     */
    private void dismissAnswerPopup() {
       if (mAnswerMpcPopup != null) {
           mAnswerMpcPopup.dismiss();
           mAnswerMpcPopup = null;
       }
    }

    public void dismissPendingDialogues() {
        if (isCannedResponsePopupShowing()) {
            dismissCannedResponsePopup();
        }

        if (isCustomMessagePopupShowing()) {
            dismissCustomMessagePopup();
        }

        if(isAnswerPopupShowing()) {
            dismissAnswerPopup();
        }
    }

    public boolean hasPendingDialogs() {
        return !(mCannedResponsePopup == null && mCustomMessagePopup == null);
    }

    /**
     * Shows the custom message entry dialog.
     */
    public void showCustomMessageDialog() {
        // Create an alert dialog containing an EditText
        final EditText et = new EditText(getActivity());
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setCancelable(
                /* SPRD: add for resolve bug 258064
                 * @orig
                 * true).setView(et)
                 * @{ */
                false).setView(et)
                .setOnDismissListener( new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface arg0) {
                        mCustomMessagePopup = null;
                    }
                })
                /* @} */
                .setPositiveButton(R.string.custom_message_send,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // The order is arranged in a way that the popup will be destroyed when the
                        // InCallActivity is about to finish.
                        final String textMessage = et.getText().toString().trim();
                        dismissCustomMessagePopup();
                        getPresenter().rejectCallWithMessage(textMessage);
                    }
                })
                .setNegativeButton(R.string.custom_message_cancel,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dismissCustomMessagePopup();
                        getPresenter().onDismissDialog();
                    }
                })
                .setTitle(R.string.respond_via_sms_custom_message);
        mCustomMessagePopup = builder.create();

        // Enable/disable the send button based on whether there is a message in the EditText
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                final Button sendButton = mCustomMessagePopup.getButton(
                        DialogInterface.BUTTON_POSITIVE);
                sendButton.setEnabled(s != null && s.toString().trim().length() != 0);
            }
        });

        // Keyboard up, show the dialog
        mCustomMessagePopup.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        mCustomMessagePopup.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        mCustomMessagePopup.show();

        // Send button starts out disabled
        final Button sendButton = mCustomMessagePopup.getButton(DialogInterface.BUTTON_POSITIVE);
        sendButton.setEnabled(false);
    }

    @Override
    public void configureMessageDialog(ArrayList<String> textResponses) {
        final ArrayList<String> textResponsesForDisplay = new ArrayList<String>(textResponses);

        textResponsesForDisplay.add(getResources().getString(
                R.string.respond_via_sms_custom_message));
        mTextResponsesAdapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1, textResponsesForDisplay);
    }

    @Override
    public void onAnswer() {
        getPresenter().onAnswer();
    }
    /**
     * SPRD: Add for Multi-Part-Call(MPC)
     */
    public void onAnswerMpc(int mpcMode) {
        getPresenter().onAnswerMpc(mpcMode);
    }
    //add for bug 713095
    public void onAnswer(int callType) {
        Log.d(this, "answerfragment" + callType);
        getPresenter().onAnswer(callType);
   }

    @Override
    public void onDecline() {
        getPresenter().onDecline();
    }

    @Override
    public void onText() {
        // SPRD:bug651194 fail reject to send sms
        getPresenter().onText(getActivity());
    }

    /**
     * OnItemClickListener for the "Respond via SMS" popup.
     */
    public class RespondViaSmsItemClickListener implements AdapterView.OnItemClickListener {

        /**
         * Handles the user selecting an item from the popup.
         */
        @Override
        public void onItemClick(AdapterView<?> parent,  // The ListView
                View view,  // The TextView that was clicked
                int position, long id) {
            Log.d(this, "RespondViaSmsItemClickListener.onItemClick(" + position + ")...");
            final String message = (String) parent.getItemAtPosition(position);
            Log.v(this, "- message: '" + message + "'");
            dismissCannedResponsePopup();
            // SPRD: add for Universe UI 
            if(SprdUtils.UNIVERSE_UI_SUPPORT){
                String resIndex = (String)view.getTag();
                String validMessage;
                if(resIndex != null&& message != null && message.startsWith(SMS_PREFIX)){
                    updateUseFrequencyForDefaultString(resIndex,mContext);
                    validMessage = message.substring(SMS_PREFIX_LENGTH+1);
                    if(resIndex.equals("0")){
                        showSMSRejectionActivity();
                    } else {
                        getPresenter().rejectCallWithMessage(validMessage);
                    }
                } else {
                    updateUseFrequency(message,mContext);
                    getPresenter().rejectCallWithMessage(message);
                }
            } else {
                // The "Custom" choice is a special case.
                // (For now, it's guaranteed to be the last item.)
                if (position == (parent.getCount() - 1)) {
                    // Show the custom message dialog
                    showSMSRejectionActivity();
                } else {
                    getPresenter().rejectCallWithMessage(message);
                }

            }
        }
    }
    
    /**
     * SPRD: 
     * add for Universe UI 
     * @{
     */
    @Override
    public void onClick(View view) {
        int id = view.getId();
        Log.d(this,"onClick(View " + view + ", id " + id + ")...");

        switch (id) {
        case R.id.handfreeButton:
            getPresenter().toggleSpeakerphone();
            getPresenter().onAnswer();
            break;
        case R.id.IncomingCallAnswerButton:
            getPresenter().onAnswer();
            break;
        case R.id.MessageButton:
            if(CallCommandClient.getInstance().isRingtonePlaying()){
                CallCommandClient.getInstance().silenceRinger();
            }
            // SPRD:bug651194 fail reject to send sms
            getPresenter().onText(getActivity());
            break;
        case R.id.IncomingCallRejectButton:
            getPresenter().onDecline();
            break;
        case R.id.IncomingMuteButton:
            getPresenter().silenceRinger();
            updateIncomingMuteButton();
            break;
        default:
            Log.w(this, "onClick: unexpected click: View " + view + ", id " + id);
            break;
        }
    }


    public void loadSmsRejectList(Context context,ListView listView){
        LoadSmsRejectStringsTask task = new LoadSmsRejectStringsTask(context,listView);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public static final Uri COMMON_PHRASE_URI = Uri.parse("content://remprovider/common_phrases");
    public static final int STRING_TYPE_FIXED = 3;
    public static final int STRING_TYPE_NORMAL = 2;
    public static final int SMS_PREFIX_LENGTH = 9;
    public static final String SMS_PREFIX = "sprd_msg:";
    public static final String[] PROJECTION = new String[] {
        "_id", 
        "content",
        "ctype", 
        "usefrequency",
    };
    private ArrayList<String> mSmsRes;

    public static void getSmsRejectStringArray(Context context,String[] smsRes){
        smsRes[0] = context.getString(R.string.respond_via_sms_custom_message);
        smsRes[1] = context.getString(R.string.respond_via_sms_canned_response_1);
        smsRes[2] = context.getString(R.string.respond_via_sms_canned_response_2);
        smsRes[3] = context.getString(R.string.respond_via_sms_canned_response_3);
        smsRes[4] = context.getString(R.string.respond_via_sms_canned_response_4);
    }

    public class LoadSmsRejectStringsTask extends AsyncTask<Void, Void, ArrayList<String>>{   
        Context mContext;
        ListView mListView;
        LoadSmsRejectStringsTask(Context context,ListView listView){
            mContext = context;
            mListView = listView;
        }
        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            Cursor cursor = null;
            ArrayList<String> SmsRes = new ArrayList<String>();
            try{
                cursor = mContext.getContentResolver().query(COMMON_PHRASE_URI, 
                        PROJECTION, 
                        String.format("(ctype = "+STRING_TYPE_FIXED+") OR (ctype = "+STRING_TYPE_NORMAL+" )"), 
                        null, 
                        "usefrequency DESC");
                if(cursor != null){
                    int dataType;
                    int stringIndex;
                    String smsString;
                    while(cursor.moveToNext()){
                        dataType = cursor.getInt(cursor.getColumnIndex("ctype"));
                        smsString = cursor.getString(cursor.getColumnIndex("content"));
                        if(dataType == STRING_TYPE_FIXED){
                            stringIndex = Integer.valueOf(smsString);
                            if (stringIndex == 0) {
                                SmsRes.add(0,SMS_PREFIX+stringIndex+getDefaultString(stringIndex,mContext));
                            } else {
                                SmsRes.add(SMS_PREFIX+stringIndex+getDefaultString(stringIndex,mContext));
                            }
                        } else {
                            SmsRes.add(smsString);
                        }
                    };
                } 
            } finally {
                if(cursor != null){
                    cursor.close();
                }
            }

            return SmsRes;
        }

        @Override
        protected void onPostExecute(ArrayList<String> SmsRes){
            mSmsRes = SmsRes;
            initAdapter();
        }

        private void initAdapter(){
            ArrayAdapter<String> adapter = (ArrayAdapter)mListView.getAdapter();
            if(adapter == null){
                adapter = new SmsRejectAdapter(mContext,
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        mSmsRes);
                mListView.setAdapter(adapter);
            } else {
                adapter.clear();
                adapter.addAll(mSmsRes);
                adapter.notifyDataSetChanged();
            }
            mListView.setOnItemClickListener(new RespondViaSmsItemClickListener());
            if(mListView.equals(mIncomingRejectList)){
                KeyEvent eventDown = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.ACTION_DOWN);
                KeyEvent eventUP = new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.ACTION_UP);
                mIncomingRejectList.onKeyDown(KeyEvent.ACTION_DOWN, eventDown);
                mIncomingRejectList.onKeyDown(KeyEvent.ACTION_UP, eventUP);
            }
        }
    }

    private void updateUseFrequency(final String smsRes,final Context context){
        String tempString = smsRes;
        if(tempString.contains("\""))
            tempString = tempString.replace("\"","“");
        final String newSmsRes = tempString;
        new Thread(){
            public void run(){
                Cursor cursor = null;
                int resId = -1;
                int useFrequency = 0;
                try{
                    cursor = context.getContentResolver().query(COMMON_PHRASE_URI, 
                            PROJECTION, 
                            "content=?",
                            new String[]{smsRes},
                            "usefrequency DESC");
                    if(cursor.moveToFirst()){
                        resId = cursor.getInt(cursor.getColumnIndex("_id"));
                        useFrequency = cursor.getInt(cursor.getColumnIndex("usefrequency")) + 1;
                        Log.d(this,"updateUseFrequency,resId="+resId+",useFrequency="+useFrequency);
                    } 

                    if(resId != -1){
                        ContentValues values = new ContentValues();
                        values.put("usefrequency", useFrequency);
                        context.getContentResolver().update(COMMON_PHRASE_URI,
                                values,
                                "_id = " + resId,
                                null);
                    }

                } finally {
                    if(cursor != null){
                        cursor.close();
                    }
                }
            }
        }.start();
    }

    private void updateUseFrequencyForDefaultString(final String smsRes,final Context context){
        String tempString = smsRes;
        if(tempString.contains("\""))
            tempString = tempString.replace("\"","“");
        final String newSmsRes = tempString;
        new Thread(){
            public void run(){
                Cursor cursor = null;
                int resId = -1;
                int useFrequency = 0;
                try{
                    cursor = context.getContentResolver().query(COMMON_PHRASE_URI, 
                            PROJECTION, 
                            "content=? AND ctype=?",
                            new String[]{smsRes,STRING_TYPE_FIXED+""},
                            "usefrequency DESC");
                    if(cursor != null && cursor.moveToFirst()){
                        resId = cursor.getInt(cursor.getColumnIndex("_id"));
                        useFrequency = cursor.getInt(cursor.getColumnIndex("usefrequency")) + 1;
                        Log.d(this,"updateUseFrequency,resId="+resId+",useFrequency="+useFrequency);
                    } 

                    if(resId != -1){
                        ContentValues values = new ContentValues();
                        values.put("usefrequency", useFrequency);
                        context.getContentResolver().update(COMMON_PHRASE_URI,
                                values,
                                "_id = " + resId,
                                null);
                    }

                } finally {
                    if(cursor != null){
                        cursor.close();
                    }
                }
            }
        }.start();
    }

    private String getDefaultString(int resIndex,Context context){
        String message = null;
        final Resources res = context.getResources();
        switch(resIndex){
        case 0:
            message = res.getString(R.string.respond_via_sms_custom_message);
            break;
        case 1:
            message = res.getString(R.string.respond_via_sms_canned_response_1);
            break;
        case 2:
            message = res.getString(R.string.respond_via_sms_canned_response_2);
            break;
        case 3:
            message = res.getString(R.string.respond_via_sms_canned_response_3);
            break;
        case 4:
            message = res.getString(R.string.respond_via_sms_canned_response_4);
            break;
        default:
            message = res.getString(R.string.respond_via_sms_custom_message);
            break;
        }
        return message;
    }
    
    public boolean isMessageListDisplayed(){
        if(mSmsRejectListLayout == null){
            return false;
        } else {
            return  (mSmsRejectListLayout.getVisibility() == View.VISIBLE);
        }
    }
    
    public void displayMessageList(boolean show){
        if(mSmsRejectListStubView != null){
            if(show){
                if(mSmsRejectListLayout == null){
                    View view = mSmsRejectListStubView.inflate();
                    mSmsRejectListLayout = view.findViewById(R.id.smsRejectListLayout);
                } else {
                    mSmsRejectListLayout.setVisibility(View.VISIBLE);
                }
                ListView smsList = (ListView)mSmsRejectListLayout.findViewById(R.id.sms_reject_list);
                loadSmsRejectList(mContext,smsList);
            } else if(mSmsRejectListLayout != null) {
                mSmsRejectListLayout.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void updateIncomingMuteButton(){
        if(mIncomingMuteButton != null){
            if(getPresenter().isRingtonePlaying()){
                mIncomingMuteButton.setEnabled(true);
            } else {
                mIncomingMuteButton.setEnabled(false);
            }
        }
    }

    public void preLoadSmsRejectStrings(Context context, ListView listView){
        ArrayList<String> SmsRes = new ArrayList<String>();
        SmsRes.add(mContext.getString(R.string.respond_via_sms_custom_message));
        SmsRes.add(mContext.getString(R.string.respond_via_sms_canned_response_1));
        SmsRes.add(mContext.getString(R.string.respond_via_sms_canned_response_2));
        SmsRes.add(mContext.getString(R.string.respond_via_sms_canned_response_3));
        SmsRes.add(mContext.getString(R.string.respond_via_sms_canned_response_4));
        ArrayAdapter<String> adapter =
                new SmsRejectAdapter(context,
                        android.R.layout.simple_list_item_1,
                        android.R.id.text1,
                        SmsRes);
        listView.setAdapter(adapter);
    }

    private LayoutInflater mInflater;
    private void initIncomingLockScreenView(){
        View answerView = mIncomingCallControlsLockScreenStub.inflate();

        mIncomingCallControlsLockScreen = answerView.findViewById(R.id.inComingCallControlsLocked);
        mIncomingCallScrollLayout = (VerticalScrollLayout) mIncomingCallControlsLockScreen.findViewById(R.id.ScrollLayout);
        if(mIncomingCallScrollLayout != null){
            mIncomingCallScrollLayout.init(answerView.getContext(),1);
            View childView = mInflater.inflate(R.layout.lockscreen_incomingcall_sprd, null);
            View childViewSms = mInflater.inflate(R.layout.lockscreen_incomingcall_sms_sprd, null);
            mIncomingCallScrollLayout.addView(childView);
            mIncomingCallScrollLayout.addView(childViewSms);
            mIncomingCallScrollLayout.SetOnViewChangeListener(new OnViewChangeListener(){
                @Override
                public void OnViewChange(int view, int actionValue) {
                    if(actionValue == LOCKSCREEN_ANSWER){
                        getPresenter().onAnswer();
                    }else if(actionValue == LOCKSCREEN_REJECT){
                        getPresenter().onDecline();
                    }
                }
            });
            mSlideLeft = (ImageView)answerView.findViewById(R.id.slide_left);
            mSlideRight = (ImageView)answerView.findViewById(R.id.slide_right);
            mSlideLeft.setBackgroundResource(R.anim.lockscreen_sprd);
            mSlideRight.setBackgroundResource(R.anim.lockscreen_sprd);
            AnimationDrawable slideAniLeft = (AnimationDrawable) mSlideLeft.getBackground();
            AnimationDrawable slideAniRight = (AnimationDrawable) mSlideRight.getBackground();
            slideAniLeft.setOneShot(false);
            slideAniLeft.start();
            slideAniRight.setOneShot(false);
            slideAniRight.start();

            mIncomingRejectList = (ListView)childViewSms.findViewById(R.id.sms_reject_list);
            preLoadSmsRejectStrings(mContext,mIncomingRejectList);
            loadSmsRejectList(mContext,mIncomingRejectList);
            mIncomingCallScrollLayout.setScreen(VerticalScrollLayout.LOCKSCREEN_SMS);
        }
    }

    private void initIncomingControlsView(){
        View answerView = mIncomingCallControlsStub.inflate();
        mIncomingCallControls = answerView.findViewById(R.id.inComingCallControls);
        mFreeHandsButton = (View) mIncomingCallControls.findViewById(R.id.handfreeButton);
        mFreeHandsButton.setOnClickListener(this);
        mSendMessageButton = (View) mIncomingCallControls.findViewById(R.id.MessageButton);
        mSendMessageButton.setOnClickListener(this);
        mRejectButton = (Button)mIncomingCallControls.findViewById(R.id.IncomingCallRejectButton);
        mRejectButton.setOnClickListener(this);
        mAnswerButton = (Button)mIncomingCallControls.findViewById(R.id.IncomingCallAnswerButton);
        mAnswerButton.setOnClickListener(this);
        mIncomingMuteButton = mIncomingCallControls.findViewById(R.id.IncomingMuteButton);
        mIncomingMuteButton.setOnClickListener(this);
    }

    public void updateFreeHandsButton() {
        if(mFreeHandsButton != null){
            final boolean bluetoothSupported = (AudioMode.BLUETOOTH == (
                    AudioModeProvider.getInstance().getSupportedModes() & AudioMode.BLUETOOTH));
            mFreeHandsButton.setEnabled(!bluetoothSupported);
        }
    }
    /* @}
     */ 

    /** SPRD: Modify for bug 391128@{ */
    public void showSMSRejectionActivity() {
        getPresenter().onDecline();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        Intent mmsIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", getPresenter()
                .getCall().getNumber(), null));
        startActivity(mmsIntent);
    }
    /** @} */

    /** SPRD: Add for bug 631607 for featurebar@{ */
    public void centerSkDown() {
        if (getActivity() != null && getActivity() instanceof InCallActivity) {
            onText();
        }
    }
    public void leftSkDown() {
        if (getActivity() != null && getActivity() instanceof InCallActivity) {
//            onAnswer();
            showAnswerOptions();
        }
    }
    public void leftSkDown(int callType) {
        if (getActivity() != null && getActivity() instanceof InCallActivity) {
            onAnswer(callType);
        }
    }
    /** @} */

    /**
     * SPRD: Add for Multi-Part-Call(MPC)
     */
    private void showAnswerOptions() {
        if (CallList.getInstance().getBackgroundCall() != null
                && CallList.getInstance().getActiveCall() != null
                && CallList.getInstance().getIncomingCall() != null
                && CallList.getInstance().getBackgroundCall().getState() == Call.State.ONHOLD
                && CallList.getInstance().getActiveCall().getState() == Call.State.ACTIVE) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setCancelable(false).
                setOnDismissListener( new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface arg0) {
                            mAnswerMpcPopup = null;
                        }
                    });
            String[] item_list = new String[] {"",""};
            item_list[0] = mContext.getString(R.string.hangup_hold_and_answer);
            item_list[1] = mContext.getString(R.string.hangup_active_and_answer);
            builder.setItems(item_list, new DialogInterface.OnClickListener() {
                 @Override
                 public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                    case 0:
                        onAnswerMpc(MultiPartCallHelper.MPC_MODE_HB);
                        break;
                    case 1:
                        onAnswerMpc(MultiPartCallHelper.MPC_MODE_HF);
                        break;
                    }
                 }
            });
            mAnswerMpcPopup = builder.create();
            mAnswerMpcPopup.show();
        } else if (CallList.getInstance().getActiveCall() != null
                && CallList.getInstance().getIncomingCall() != null
                && CallList.getInstance().getActiveCall().getState() == Call.State.ACTIVE){
            //onAnswer();
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setCancelable(false).
                    setOnDismissListener( new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface arg0) {
                                mAnswerMpcPopup = null;
                            }
                        });
                String[] item_list = new String[] {"",""};
                item_list[0] = mContext.getString(R.string.hold_current_call_and_answer);
                item_list[1] = mContext.getString(R.string.hangup_current_call_and_answer);
                builder.setItems(item_list, new DialogInterface.OnClickListener() {
                     @Override
                     public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                        case 0:
                            onAnswer();
                            break;
                        case 1:
                            onAnswerMpc(MultiPartCallHelper.MPC_MODE_HF_TWO);
                            break;
                        }
                     }
                });
                mAnswerMpcPopup = builder.create();
                mAnswerMpcPopup.show();
        } else {
            onAnswer();
        }
    }

}
