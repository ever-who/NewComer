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

import android.app.AlertDialog;
import android.content.Context;

import android.text.TextUtils;
import android.widget.Toast;
import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.AudioMode;
import com.sprd.incallui.SprdUtils;

import java.util.ArrayList;
import android.telephony.TelephonyManager;

/**
 * Presenter for the Incoming call widget.
 */
public class AnswerPresenter extends Presenter<AnswerPresenter.AnswerUi>
        implements CallList.CallUpdateListener, CallList.Listener {

    private static final String TAG = AnswerPresenter.class.getSimpleName();

    private int mCallId = Call.INVALID_CALL_ID;
    private Call mCall = null;
    private AlertDialog mHangupcallDialog = null;

    @Override
    public void onUiReady(AnswerUi ui) {
        super.onUiReady(ui);

        final CallList calls = CallList.getInstance();
        final Call call = calls.getIncomingCall();
        // TODO: change so that answer presenter never starts up if it's not incoming.
        if (call != null) {
            processIncomingCall(call);
        }

        // Listen for incoming calls.
        calls.addListener(this);
        //Add for Universe UI
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            getUi().updateIncomingMuteButton();
        }
    }

    @Override
    public void onUiUnready(AnswerUi ui) {
        super.onUiUnready(ui);

        CallList.getInstance().removeListener(this);

        // This is necessary because the activity can be destroyed while an incoming call exists.
        // This happens when back button is pressed while incoming call is still being shown.
        if (mCallId != Call.INVALID_CALL_ID) {
            CallList.getInstance().removeCallUpdateListener(mCallId, this);
        }
    }

    @Override
    public void onCallListChange(CallList callList) {
        // no-op
    }

    @Override
    public void onDisconnect(Call call) {
        // no-op
    }

    @Override
    public void onIncomingCall(Call call) {
        // TODO: Ui is being destroyed when the fragment detaches.  Need clean up step to stop
        // getting updates here.
        Log.d(this, "onIncomingCall: " + this);
        if (getUi() != null) {
            if (call.getCallId() != mCallId) {
                // A new call is coming in.
                processIncomingCall(call);
            }
        }
    }

    private void processIncomingCall(Call call) {
        mCallId = call.getCallId();
        mCall = call;

        // Listen for call updates for the current call.
        CallList.getInstance().addCallUpdateListener(mCallId, this);

        Log.d(TAG, "Showing incoming for call id: " + mCallId + " " + this);
        final ArrayList<String> textMsgs = CallList.getInstance().getTextResponses(
                call.getCallId());
        getUi().showAnswerUi(true);

        if (call.can(Call.Capabilities.RESPOND_VIA_TEXT) && textMsgs != null) {
            getUi().showTextButton(true);
            getUi().configureMessageDialog(textMsgs);
        } else {
            getUi().showTextButton(false);
        }
        //Add for Universe UI
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            getUi().updateIncomingMuteButton();
        }
    }


    @Override
    public void onCallStateChanged(Call call) {
        Log.d(this, "onCallStateChange() " + call + " " + this);
        if (call.getState() != Call.State.INCOMING && call.getState() != Call.State.CALL_WAITING) {
            /* SPRD: add for CRTU-G case 31.3.1.2.2.1@{ */
            if(CallList.getInstance().getIncomingCall() != null &&
                    CallList.getInstance().getIncomingCall().getCallId() != call.getCallId()){
                Log.i(this, "onCallStateChange()==> Another all still ringing!");
                return;
            }
            /* @} */
            // Stop listening for updates.
            CallList.getInstance().removeCallUpdateListener(mCallId, this);

            getUi().showAnswerUi(false);

            // mCallId will hold the state of the call. We don't clear the mCall variable here as
            // it may be useful for sending text messages after phone disconnects.
            mCallId = Call.INVALID_CALL_ID;
        }
        //Add for Universe UI
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            getUi().updateIncomingMuteButton();
        }
    }

    public void onAnswer() {
        if (mCallId == Call.INVALID_CALL_ID) {
            return;
        }

        Log.d(this, "onAnswer " + mCallId);

        if (SprdUtils.PIKEL_UI_SUPPORT) {
            TelephonyManager.getDefault().answerRingingCall();
        } else {
            CallCommandClient.getInstance().answerCall(mCallId);
        }
    }
    /**
     * SPRD: Add for Multi-Part-Call(MPC)
     */
    public void onAnswerMpc(int mpcMode) {
        if (mCallId == Call.INVALID_CALL_ID) {
            return;
        }

        Log.d(this, "onAnswerMpc " + mCallId);

        if (SprdUtils.PIKEL_UI_SUPPORT) {
            TelephonyManager.getDefault().answerRingingCallForMpc(mpcMode);
        } else {
            CallCommandClient.getInstance().answerCall(mCallId);
        }
    }
    public void onAnswer(int callType) { //add for bug 713095 688629
        if (mCallId == Call.INVALID_CALL_ID) {
            return;
        }

        Log.d(this, "onAnswer " + mCallId);
        CallCommandClient.getInstance().answerCall(callType, mCallId);

    }


    public void onDecline() {
        Log.d(this, "onDecline " + mCallId);

        CallCommandClient.getInstance().rejectCall(mCall, false, null);
    }

    // SPRD:bug651194 fail reject to send sms
    public void onText(Context context) {
        if (getUi() != null) {
             /* SPRD:bug651194 fail reject to send sms @{ */
            if (context != null && mCall != null && TextUtils.isEmpty(mCall.getNumber())) {
                Toast.makeText(context, context.getString(R.string.fail_reject_to_send_sms), Toast.LENGTH_SHORT).show();
                return;
            }
            /* @} */
            getUi().showMessageDialog();
        }
    }

    public void rejectCallWithMessage(String message) {
        Log.d(this, "sendTextToDefaultActivity()...");

        CallCommandClient.getInstance().rejectCall(mCall, true, message);

        onDismissDialog();
    }

    public void onDismissDialog() {
        InCallPresenter.getInstance().onDismissDialog();
    }

    public void dealIncomingThirdCall(Context context, boolean show) {
        if (show) {
            if ((CallList.getInstance().getActiveCall() != null)
                    && (CallList.getInstance().getBackgroundCall() != null)
                    && mHangupcallDialog == null) {
                showHangupCallDialog(context);
            }
        } else {
            dismissHangupCallDialog();
        }
    }

    private void showHangupCallDialog(Context context) {
        String note_title = context.getString(R.string.hangupcall_note_title);
        String note_message = context.getString(R.string.hangupcall_note_message);
        mHangupcallDialog = new AlertDialog.Builder(context)
                .setTitle(note_title).setMessage(note_message)
                .setPositiveButton(com.android.internal.R.string.ok, null)
                .setCancelable(false).create();
        mHangupcallDialog.show();
    }

    public void dismissHangupCallDialog() {
        if (mHangupcallDialog != null) {
            mHangupcallDialog.dismiss();
            mHangupcallDialog = null;
        }
    }

    interface AnswerUi extends Ui {
        public void showAnswerUi(boolean show);
        public void showTextButton(boolean show);
        public void showMessageDialog();
        public void configureMessageDialog(ArrayList<String> textResponses);
        public void updateIncomingMuteButton();//Add for Universe UI
    }

    /**
     * SPRD: 
     * add for Universe UI 
     * @{
     */
    public void toggleSpeakerphone() {
        int supportMode = AudioModeProvider.getInstance().getSupportedModes();
        int audioMode = AudioModeProvider.getInstance().getAudioMode();
        Log.d(TAG, "support mode = " + supportMode + ", audio mode = " + audioMode);
        if (AudioMode.SPEAKER != (AudioMode.SPEAKER & supportMode)) {
            Log.e(this, "toggling speakerphone not allowed when SPEAKER not supported.");
            return;
        }

        if (audioMode == AudioMode.SPEAKER) {
            Log.d(TAG, "Audio mode has been set to SPEAKER already.");
            return;
        }
        Log.d(TAG, "speakerOnAfterAnswer.");
        CallCommandClient.getInstance().speakerOnAfterAnswer();
    }
    
    public void silenceRinger(){
        CallCommandClient.getInstance().silenceRinger();
    }

    public boolean isRingtonePlaying(){
        return CallCommandClient.getInstance().isRingtonePlaying();
    }
    /* @}
     */

    /**
     * SPRD:
     * modify for performance optimization
     */
    @Override
    public void onPrepareUi() {
    }

    /** SPRD: Modify for bug 391128@{ */
    public Call getCall() {
        return mCall;
    }
    /** @} */
}
