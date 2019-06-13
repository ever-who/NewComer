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

import com.android.dialer.DialerApplication;
import com.android.dialer.DialtactsActivity;
import com.android.incallui.AudioModeProvider.AudioModeListener;
import com.android.incallui.InCallPresenter.InCallState;
import com.android.incallui.InCallPresenter.InCallStateListener;
import com.android.incallui.InCallPresenter.IncomingCallListener;
import com.android.incallui.InCallPresenter.MTCallStateListener;
import com.android.services.telephony.common.AudioMode;
import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.Call.Capabilities;
import com.sprd.incallui.SprdCallCommandClient;
import com.sprd.incallui.SprdUtils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import static com.android.incallui.CallButtonFragment.Buttons.*;

import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.telephony.VoLteServiceState;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.widget.Toast;
import android.telecom.VideoProfile;
import android.os.SystemProperties;

/**
 * Logic for call buttons.
 */
public class CallButtonPresenter extends Presenter<CallButtonPresenter.CallButtonUi>
        implements InCallStateListener, AudioModeListener, IncomingCallListener ,MTCallStateListener {

    private Call mCall;
    private boolean mAutomaticallyMuted = false;
    private boolean mPreviousMuteState = false;

    private boolean mShowGenericMerge = false;
    private boolean mShowManageConference = false;
    private boolean mShowDialpad = false;//SPRD:add for Universe UI

    private InCallState mPreviousState = null;

    /* SPRD: AUTOMATIC RECORD FEATURE. @{ */
    private static final String AUTOMATIC_RECORDING_PREFERENCES_NAME = "automatic_recording_key";
    private boolean mAutomaticRecording;
    private boolean mIsAutomaticRecordingStart;
    /* @} */
    private boolean isVolteEnable = SystemProperties.getBoolean("persist.sys.volte.enable", false);
    public static final boolean VT_SUPPORT = SystemProperties.getBoolean("persist.sys.support.vt", true);
    /* SPRD: add for bug636130 @ { */
    private int mPrimayCardId;
    private int mCallId = 99; //SPRD: bug 845977
    private boolean mMTCallOnHold = false; //SPRD: bug 845977

    /* @} */
    //SPRD: add for bug 853657
    boolean mIsSupportTxRxVideo = SystemProperties.getBoolean("persist.sys.txrx_vt", false);

    public CallButtonPresenter() {
    }

    @Override
    public void onUiReady(CallButtonUi ui) {
        super.onUiReady(ui);

        AudioModeProvider.getInstance().addListener(this);

        // register for call state changes last
        InCallPresenter.getInstance().addListener(this);
        InCallPresenter.getInstance().addIncomingCallListener(this);
        InCallPresenter.getInstance().addMTCallStateListener(this);   //SPRD: bug 845977
        /* SPRD: add for bug636130 @ { */
        mPrimayCardId = TelephonyManager.from(ui.getContext()).getPrimaryCard();
        /* @} */
    }

    @Override
    public void onUiUnready(CallButtonUi ui) {
        super.onUiUnready(ui);

        InCallPresenter.getInstance().removeListener(this);
        AudioModeProvider.getInstance().removeListener(this);
        InCallPresenter.getInstance().removeIncomingCallListener(this);
        InCallPresenter.getInstance().removeMTCallStateListener(this);   //SPRD: bug 845977
    }

    @Override
    public void onStateChange(InCallState state, CallList callList) {
        /* SPRD: AUTOMATIC RECORD FEATURE. @{ */
        Context context = getUi().getContext().getApplicationContext();
        DialerApplication dialerApplication = (DialerApplication) context;
        mIsAutomaticRecordingStart = dialerApplication.getIsAutomaticRecordingStart();
        final SharedPreferences sp = context.getSharedPreferences(
                DialtactsActivity.SHARED_PREFS_NAME, Context.MODE_WORLD_READABLE | Context.MODE_MULTI_PROCESS);
        mAutomaticRecording = sp.getBoolean(AUTOMATIC_RECORDING_PREFERENCES_NAME, false);
        /* @} */
        if (state == InCallState.OUTGOING || state == InCallState.PREPARE_UI) {//SPRD:ADD PREPARE_UI
            mCall = callList.getOutgoingCall();
        } else if (state == InCallState.INCALL) {
            mCall = callList.getActiveOrBackgroundCall();

            // When connected to voice mail, automatically shows the dialpad.
            // (On previous releases we showed it when in-call shows up, before waiting for
            // OUTGOING.  We may want to do that once we start showing "Voice mail" label on
            // the dialpad too.)
            if (mPreviousState == InCallState.OUTGOING
                    && mCall != null && PhoneNumberUtils.isVoiceMailNumber(mCall.getNumber())) {
                /* SPRD: add for Universe UI
                 * orig: getUi().displayDialpad(true);
                 *  @ { */
                mShowDialpad = true;
                /* @}*/
            }
        } else if (state == InCallState.INCOMING) {
            /* SPRD: add for Universe UI
             * orig: getUi().displayDialpad(false);
             *  @ { */
            mShowDialpad = false;
            /* @}*/
            mCall = callList.getIncomingCall();
        } else {
            mCall = null;
            mShowDialpad = false;// SPRD: add for Universe UI
            InCallPresenter.getInstance().stopRecorderForDisconnect();
        }
        if (SprdUtils.PIKEL_UI_SUPPORT) {
            updatePikeLUi(state, mCall);
        } else {
            updateUi(state, mCall);
        }
        /* SPRD: AUTOMATIC RECORD FEATURE. @{
         * Using function toggleRecorder for triggering the automatic recording only once on the following conditions:
         * 1) mAutomaticRecording is true :Automatic recording switch to open In the general setting before dialing.
         * 2) Call State is ACTIVE.
         * mIsAutomaticRecordingStart is used for identifying automatic recording started or not
         * TODO: When we cancel recording in first call and add a new call ,the new call will not trigger automatic recording currently.
         * */
         if (mAutomaticRecording && !mIsAutomaticRecordingStart
                 && mCall != null && mCall.getState() == Call.State.ACTIVE) {
             getUi().toggleRecord();
             dialerApplication.setIsAutomaticRecordingStart(true);
         }
        // SPRD: record issue for 652294
        if (mCall == null || state == InCallState.NO_CALLS) {
             dialerApplication.setIsAutomaticRecordingStart(false);
         }
         /* @} */

         /* SPRD: vibration feedback for call connection and disConnection. See bug624152 @{ */
         if(mCall != null && mCall.getState() == Call.State.ACTIVE && state == InCallState.INCALL
                 && mPreviousState == InCallState.OUTGOING) {
             SprdUtils.vibrateForCallStateChange(context.getApplicationContext(), mCall,
                     SprdUtils.VIBRATION_FEEDBACK_FOR_CONNECT_PREFERENCES_NAME);
         }
         /* @} */
        mPreviousState = state;
    }

    @Override
    public void onIncomingCall(InCallState state, Call call) {
        onStateChange(state, CallList.getInstance());
    }

    @Override
    public void onAudioMode(int mode) {
        if (getUi() != null) {
            getUi().setAudio(mode);
        }
    }

    @Override
    public void onSupportedAudioMode(int mask) {
        if (getUi() != null) {
            getUi().setSupportedAudio(mask);
        }
    }

    @Override
    public void onMute(boolean muted) {
        if (getUi() != null) {
            getUi().setMute(muted);
        }
    }

    //SPRD: bug 845977
    @Override
    public void onMTHold(int onHold) {
        final CallButtonUi ui = getUi();

        if(mCall != null) {
            mCallId = mCall.getCallId();
            mMTCallOnHold = mCall.getMTCallHold();
            Log.d(this, "onMTHold get value of call, mMTCallOnHold: "+mMTCallOnHold  );

            if (ui == null) {
                return;
            }
            updatePikeLButtonUi(ui,mCall);
        }
    }

    public int getAudioMode() {
        return AudioModeProvider.getInstance().getAudioMode();
    }

    public int getSupportedAudio() {
        return AudioModeProvider.getInstance().getSupportedModes();
    }

    public void setAudioMode(int mode) {

        // TODO: Set a intermediate state in this presenter until we get
        // an update for onAudioMode().  This will make UI response immediate
        // if it turns out to be slow

        Log.d(this, "Sending new Audio Mode: " + AudioMode.toString(mode));
        CallCommandClient.getInstance().setAudioMode(mode);
    }

    /**
     * Function assumes that bluetooth is not supported.
     */
    public void toggleSpeakerphone() {
        // this function should not be called if bluetooth is available
        if (0 != (AudioMode.BLUETOOTH & getSupportedAudio())) {

            // It's clear the UI is wrong, so update the supported mode once again.
            Log.e(this, "toggling speakerphone not allowed when bluetooth supported.");
            getUi().setSupportedAudio(getSupportedAudio());
            return;
        }

        int newMode = AudioMode.SPEAKER;

        // if speakerphone is already on, change to wired/earpiece
        if (getAudioMode() == AudioMode.SPEAKER) {
            newMode = AudioMode.WIRED_OR_EARPIECE;
        }

        setAudioMode(newMode);
    }

    public void endCallClicked() {
        if (mCall == null) {
            return;
        }

        CallCommandClient.getInstance().disconnectCall(mCall.getCallId());
        /* SPRD: add for I Log  @ { */
        if (android.util.Log.isIloggable()) {
            android.util.Log.startPerfTracking("PhonePerf : HangUpCall start");
        }
        /* @}*/
    }

    public void manageConferenceButtonClicked() {
        getUi().displayManageConferencePanel(true);
    }

    public void muteClicked(boolean checked) {
        Log.d(this, "turning on mute: " + checked);

        CallCommandClient.getInstance().mute(checked);
    }

    public void holdClicked(boolean checked) {
        if (mCall == null) {
            return;
        }

        Log.d(this, "holding: " + mCall.getCallId());

        CallCommandClient.getInstance().hold(mCall.getCallId(), checked);
    }

    public void mergeClicked() {
        Call confCall = CallList.getInstance().getAllConferenceCall();
        if(confCall != null && confCall.getChildCallIds().size() >= 5) {
            Log.d(this, "mergeClicked: conference call has 5 member");
            if (getUi() != null && getUi().getContext() != null) {
                Context context = getUi().getContext();
                String failMessage = context.getString(R.string.conf_can_not_invite_more);
                Toast.makeText(context, failMessage, Toast.LENGTH_LONG).show();
            }
            return;
        }
        CallCommandClient.getInstance().merge();
    }

    public void addCallClicked() {
        // Automatically mute the current call
        mAutomaticallyMuted = true;
        mPreviousMuteState = AudioModeProvider.getInstance().getMute();
        // Simulate a click on the mute button
        muteClicked(true);

        CallCommandClient.getInstance().addCall();
    }

    public void swapClicked() {
        CallCommandClient.getInstance().swap();
    }
    /* SPRD: Add for record */
    public void recordClicked(boolean checked) {
        getUi().toggleRecord();
    }
    public void showDialpadClicked(boolean checked) {
        Log.v(this, "Show dialpad " + String.valueOf(checked));
        getUi().displayDialpad(checked);
        updateExtraButtonRow();
    }

    private void updatePikeLUi(InCallState state, Call call) {
        final CallButtonUi ui = getUi();
        if (ui == null) {
            return;
        }

        final boolean isEnabled = state.isConnectingOrConnected() &&
                !state.isIncoming() && call != null;

        /* SPRD: add for PIKEL UI @ { */
        Log.i(this, "isEnabled: " + isEnabled);
        ui.showButtonUi(isEnabled);
        /* @} */

        ui.setEnabled(isEnabled);
        if (call == null) {
            ui.showOverflowButton(false);
            return;
        }

        Log.i(this, "updatePikeLUi call UI for call: "+ call);

        if (isEnabled) {
            updatePikeLButtonUi(ui,call);
        }
    }

    private void updatePikeLButtonUi(CallButtonUi ui,Call call){
        /* SPRD: add for bug636130 @ { */
        int currentCallingCardId = call.getPhoneId();
        boolean isUsingPrimaryCard = currentCallingCardId == mPrimayCardId;
        /* @} */

        final boolean canMerge = call.can(Capabilities.MERGE_CALLS);
        final boolean canAdd = call.can(Capabilities.ADD_CALL);
        final boolean isGenericConference = call.can(Capabilities.GENERIC_CONFERENCE);

        final boolean showMerge = !isGenericConference && canMerge;
        final boolean showOverflowButton = call.getState() != Call.State.INCOMING;
        final boolean showMute = call.can(Capabilities.MUTE);
        final boolean isVideoCall = call.isVideo();
        mShowManageConference = (call.isConferenceCall() // SPRD: modify for bug885496
                || call.can(Capabilities.CAPABILITY_IMS_CONFERENCE)) && !isGenericConference
                && call.getChildCallIds() != null && call.getChildCallIds().size() >1;
        final boolean supportHold = call.can(Capabilities.SUPPORT_HOLD);
        final boolean canSwap = call.can(Capabilities.SWAP_CALLS);
        final boolean canHold = call.can(Capabilities.HOLD);
        final boolean showHold = canHold && supportHold &&!canSwap /*&& !isVideoCall*/;
        InCallCameraManager cameraManager = InCallPresenter.getInstance().
                getInCallCameraManager();
        final boolean showSwitchCamera = (cameraManager.getFrontFacingCameraId() != null) &&
                (cameraManager.getRearFacingCameraId() != null) ;
        Log.i(this, "Support hold: "+ supportHold);
        Log.i(this, "Can hold: "+ canHold);
        Log.i(this, "Show hold: "+ showHold);
        Log.i(this, "Show merge: "+ canMerge);
        Log.i(this, "Show swap: "+ canSwap);
        Log.i(this, "Show add call: "+ canAdd);
        Log.i(this, "Show mute: "+ showMute);
        Log.i(this, "isVideoCall: "+ isVideoCall);
        Log.i(this, "showSwitchCamera:"+showSwitchCamera);
        Log.i(this,"showManageConference:"+mShowManageConference);

        ui.showOverflowButton(showOverflowButton);
        ui.showButton(BUTTON_RECORD, enableRecorderOrAddCall(call));
        // SPRD: Add for send sms button. See Bug62415
        ui.showButton(BUTTON_SEND_SMS, enableRecorderOrAddCall(call));
        ui.showButton(BUTTON_AUDIO, true);
        ui.showButton(BUTTON_DIALPAD, false);
        ui.showButton(BUTTON_MUTE, showMute);
        ui.showButton(BUTTON_MANAGE_VOICE_CONFERENCE,mShowManageConference);

        if (showMerge) {
            ui.showButton(BUTTON_MERGE,true);
            ui.showButton(BUTTON_ADD_CALL,false);
        } else {
            ui.showButton(BUTTON_MERGE,false);
            ui.showButton(BUTTON_ADD_CALL,canAdd);
            ui.enableAddCall(canAdd);
        }

        if (showHold) {
            ui.showButton(BUTTON_HOLD,true);
            ui.setHold(call.getState() == Call.State.ONHOLD);
            ui.enableHold(true);
            ui.showButton(BUTTON_SWAP,false);
        } else if (canSwap) {
            ui.showButton(BUTTON_HOLD,false);
            ui.showButton(BUTTON_SWAP,true);
        }

        if(call != null && mCallId != call.getCallId()) {//&& mMTCallHoldState != SuppServiceNotification.MT_CODE_FORWARDED_CALL) {
            mMTCallOnHold = call.getMTCallHold();
        }
        // SPRD: Don't show upgrade video menu if call is conference call.
        //SPRD: add for bug 853657, 815119
        if(!mIsSupportTxRxVideo){
            ui.showButton(BUTTON_UPGRADE_TO_VIDEO,(!mMTCallOnHold) && mImsEnableButton && !call.isVideo() && (call.getState() == Call.State.ACTIVE) && isUsingPrimaryCard
                    && !call.isConferenceCall() && VT_SUPPORT && !call.can(Capabilities.CAPABILITY_IMS_CONFERENCE)); //SPRD: add for Bug 843810
        }else{
            ui.showButton(BUTTON_UPGRADE_TO_VIDEO,(!mMTCallOnHold) && mImsEnableButton && !call.isBidirectionalVideoCall() && (call.getState() == Call.State.ACTIVE) && isUsingPrimaryCard
                    && !call.isConferenceCall() && VT_SUPPORT && !call.can(Capabilities.CAPABILITY_IMS_CONFERENCE)); //SPRD: add for Bug 843810
        }

        ui.showButton(BUTTON_CHANGED_TO_AUDIO,mImsEnableButton && call.isVideo() && (call.getState() == Call.State.ACTIVE) && isUsingPrimaryCard
                && !call.isConferenceCall() && !call.can(Capabilities.CAPABILITY_IMS_CONFERENCE));

        ui.showButton(BUTTON_VIDEO_CALL_RX,(!mMTCallOnHold) && mIsSupportTxRxVideo && mImsEnableButton && (call.isBidirectionalVideoCall() || !call.isVideo()) && (call.getState() == Call.State.ACTIVE) && isUsingPrimaryCard
                && !call.isConferenceCall() && VT_SUPPORT && !call.can(Capabilities.CAPABILITY_IMS_CONFERENCE)); //SPRD: add for Bug 843810, 853657, 815119

        ui.showButton(BUTTON_VIDEO_CALL_TX,(!mMTCallOnHold) && mIsSupportTxRxVideo && mImsEnableButton && (call.isBidirectionalVideoCall() || !call.isVideo()) && (call.getState() == Call.State.ACTIVE) && isUsingPrimaryCard
                && !call.isConferenceCall() && VT_SUPPORT && !call.can(Capabilities.CAPABILITY_IMS_CONFERENCE)); //SPRD: add for Bug 843810, 853657, 815119

        ui.showButton(BUTTON_PAUSE_VIDEO,!mIsSupportTxRxVideo && mImsEnableButton && call.isVideo() && (call.getState() == Call.State.ACTIVE) && isUsingPrimaryCard);
        //add for SPRD:Bug 673157
        ui.showButton(BUTTON_SWITCH_CAMERA,mImsEnableButton && call.isVideo() && (call.getState() == Call.State.ACTIVE) && isUsingPrimaryCard && showSwitchCamera);
        ui.enableMute(call.can(Capabilities.MUTE));
        ui.enableRecord(call.enableRecord());
        ui.enableAddCall(call.enableAddCall());
        ui.enableHold(call.enableHold());

        ui.updateButtonStates();
    }

    private void updateUi(InCallState state, Call call) {
        final CallButtonUi ui = getUi();
        if (ui == null) {
            return;
        }

        final boolean isEnabled = state.isConnectingOrConnected() &&
                !state.isIncoming() && call != null;

        /* SPRD: add for Universe UI @ { */
        getUi().displayDialpad(mShowDialpad);
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            boolean showButtonUI = isEnabled;
            Log.i(this, "showButtonUi: "+ showButtonUI);
            ui.showButtonUi(showButtonUI);
            if(!showButtonUI) return;
        }
        /* @} */

        ui.setEnabled(isEnabled);

        Log.i(this, "Updating call UI for call: "+ call);

        if (isEnabled) {
            Log.i(this, "Show hold: "+call.can(Capabilities.SUPPORT_HOLD));
            Log.i(this, "Enable hold: "+call.can(Capabilities.HOLD));
            Log.i(this, "Show merge: "+call.can(Capabilities.MERGE_CALLS));
            Log.i(this, "Show swap: "+call.can(Capabilities.SWAP_CALLS));
            Log.i(this, "Show add call: "+call.can(Capabilities.ADD_CALL));
            Log.i(this, "Show mute: "+call.can(Capabilities.MUTE));

            final boolean canMerge = call.can(Capabilities.MERGE_CALLS);
            final boolean canAdd = call.can(Capabilities.ADD_CALL);
            final boolean isGenericConference = call.can(Capabilities.GENERIC_CONFERENCE);


            final boolean showMerge = !isGenericConference && canMerge;

            if (showMerge) {
                ui.showMerge(true);
                ui.showAddCall(false);
            } else {
                ui.showMerge(false);
                ui.showAddCall(true);
                ui.enableAddCall(canAdd);
            }

            final boolean canHold = call.can(Capabilities.HOLD);
            final boolean canSwap = call.can(Capabilities.SWAP_CALLS);
            final boolean supportHold = call.can(Capabilities.SUPPORT_HOLD);

            if (canHold) {
                ui.showHold(true);
                ui.setHold(call.getState() == Call.State.ONHOLD);
                ui.enableHold(true);
                ui.showSwap(false);
            } else if (canSwap) {
                ui.showHold(false);
                ui.showSwap(true);
            } else {
                // Neither "Hold" nor "Swap" is available.  This can happen for two
                // reasons:
                //   (1) this is a transient state on a device that *can*
                //       normally hold or swap, or
                //   (2) this device just doesn't have the concept of hold/swap.
                //
                // In case (1), show the "Hold" button in a disabled state.  In case
                // (2), remove the button entirely.  (This means that the button row
                // will only have 4 buttons on some devices.)

                if (supportHold) {
                    ui.showHold(true);
                    ui.enableHold(false);
                    ui.setHold(call.getState() == Call.State.ONHOLD);
                    ui.showSwap(false);
                } else {
                    ui.showHold(false);
                    ui.showSwap(false);
                }
            }
            ui.enableMute(call.can(Capabilities.MUTE));
            /* SPRD: add for bug264550 @ { */
            ui.enableRecord(call.enableRecord());
            ui.enableAddCall(call.enableAddCall());
            ui.enableHold(call.enableHold());
            /* @} */

            // Finally, update the "extra button row": It's displayed above the
            // "End" button, but only if necessary.  Also, it's never displayed
            // while the dialpad is visible (since it would overlap.)
            //
            // The row contains two buttons:
            //
            // - "Manage conference" (used only on GSM devices)
            // - "Merge" button (used only on CDMA devices)

            mShowGenericMerge = isGenericConference && canMerge;
            mShowManageConference = (call.isConferenceCall() && !isGenericConference);

            updateExtraButtonRow();
        }
    }
    private boolean mStay4G;
    private boolean mImsEnableButton;
    private PhoneStateListener mLtePhoneStateListener;
    public void createPhoneStateListener() {
        mLtePhoneStateListener = new PhoneStateListener() {
            @Override
            public void onVoLteServiceStateChanged(VoLteServiceState serviceState) {
                    mImsEnableButton = (serviceState.getSrvccState() != VoLteServiceState.HANDOVER_STARTED
                            && (serviceState.getImsState() == 1));
                    if (mCall != null && getUi() != null) {
                        final CallButtonUi ui = getUi();
                        if (ui == null) {
                            return;
                        }
                        if (SprdUtils.PIKEL_UI_SUPPORT) {
                            updatePikeLButtonUi(ui,mCall);
                        }
                    }
            }
        };
    }
    public void startMonitor() {
        if (isVolteEnable) {
            TelephonyManager.getDefault().listen(mLtePhoneStateListener,
                    PhoneStateListener.LISTEN_SERVICE_STATE
                            | PhoneStateListener.LISTEN_VOLTE_STATE);
        }
    }

    public void stopMonitor() {
        if (isVolteEnable) {
            TelephonyManager.getDefault().listen(mLtePhoneStateListener,
                    PhoneStateListener.LISTEN_NONE);
        }
        mLtePhoneStateListener = null;
    }
    private void updateExtraButtonRow() {
        final boolean showExtraButtonRow = (mShowGenericMerge || mShowManageConference) &&
                !getUi().isDialpadVisible();

        Log.d(this, "isGeneric: " + mShowGenericMerge);
        Log.d(this, "mShowManageConference : " + mShowManageConference);
        Log.d(this, "mShowGenericMerge: " + mShowGenericMerge);
        if (showExtraButtonRow) {
            if (mShowGenericMerge) {
                getUi().showGenericMergeButton();
            } else if (mShowManageConference) {
                getUi().showManageConferenceCallButton();
            }
        } else {
            getUi().hideExtraRow();
        }
    }

    /* SPRD: Add for send sms button. See Bug62415 */
    public void sendSMSClicked() {
        Log.i(this, "sendSMSClicked");
        final CallButtonUi ui = getUi();
        String muticall = "";
        Uri uri = null;
        String[] numberArray = InCallPresenter.getInstance().getCallList().getConferenceCallNumberArray();

        if (mCall != null && ui != null) {
            if (mCall.isConferenceCall()) {
                for (int i = 0; i < numberArray.length; i++) {
                    muticall += numberArray[i] + ",";
                }

                Log.d(this, "Muticall --- =" + muticall + ", mCall.getNumber() = " + mCall.getNumber());
                if (TextUtils.isEmpty(muticall)) {
                    muticall = mCall.getNumber();
                }
                uri = Uri.parse("smsto:" + muticall);
            } else {
                uri = Uri.parse("smsto:" + mCall.getNumber());
                Log.d(this, "callNumber --- =" + mCall.getNumber());
            }
        } else {
            Log.i(this, "The call is null,can't send message.");
        }
        Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
        ui.getContext().startActivity(intent);

        //add for SPRD:Bug 639332
        getUi().setPauseVideoButton(false);
    }
    /* @} */

    public void refreshMuteState() {
        /* SPRD: add for Bug316054 @ { */
        if (getUi() == null || InCallPresenter.getInstance().isPrepareUi()) {
            return;
        }
        /* @} */

        // Restore the previous mute state
        if (mAutomaticallyMuted &&
                AudioModeProvider.getInstance().getMute() != mPreviousMuteState) {
            if (getUi() == null) {
                return;
            }
            muteClicked(mPreviousMuteState);
        }
        mAutomaticallyMuted = false;
    }

    /* SPRD: add for bug261759 @ { */
    public void refreshUi(InCallState state, Call call){
        if (SprdUtils.PIKEL_UI_SUPPORT) {
            updatePikeLUi(state, call);
        } else {
            updateUi(state,call);
        }
    }
    /* @} */

    public boolean enableRecorderOrAddCall(Call call) {
        int state = call.getState();
        return (state == Call.State.ACTIVE || state == Call.State.ONHOLD
                || state == Call.State.CONFERENCED);
    }

    public void changeToVoiceClicked() {
        if (mCall == null) {
            return;
        }
        SprdCallCommandClient.getInstance().requestVolteCallMediaChange(VideoProfile.STATE_AUDIO_ONLY, mCall.getPhoneId(), mCall);
    }

    public void changeToVideoClicked() {
        if (mCall == null) {
            return;
        }
        //add for SPRD:Bug 647290
        if (getUi() != null) {
            getUi().setSwitchCameraButtonSelect(false);
        }
        SprdCallCommandClient.getInstance().requestVolteCallMediaChange(VideoProfile.STATE_BIDIRECTIONAL, mCall.getPhoneId(),mCall);
    }

    public void switchCameraClicked(boolean useFrontFacingCamera) {
        if (mCall == null) {
            return;
        }
        InCallCameraManager cameraManager = InCallPresenter.getInstance().getInCallCameraManager();
        cameraManager.setUseFrontFacingCamera(useFrontFacingCamera);
        String cameraId = cameraManager.getActiveCameraId();
        if (cameraId != null) {
            SprdCallCommandClient.getInstance().handleSetCamera(cameraId, mCall);
            //videoCall.requestCameraCapabilities();
        }
        getUi().setSwitchCameraButtonSelect(!useFrontFacingCamera);
    }

    public void pauseVideoClicked(boolean pause) {
        if (mCall == null) {
            return;
        }
        //InCallPresenter.getInstance().getInCallCameraManager().setCameraPaused(pause);
        if (pause) {
            SprdCallCommandClient.getInstance().handleSetCamera(null, mCall);
        } else {
            InCallCameraManager cameraManager = InCallPresenter.getInstance().
                    getInCallCameraManager();
            String cameraId = cameraManager.getActiveCameraId();
            if (cameraId != null) {
                 SprdCallCommandClient.getInstance().handleSetCamera(cameraId, mCall);
            }
        }
        InCallPresenter.getInstance().getInCallCameraManager().setCameraPaused(pause);
        getUi().setPauseVideoButton(pause);
    }

    public void changeToCallTypeClicked(int id) {
        int selectedCallProfile = -1;
        if(mCall == null){
            Log.d(this, "mCall == null");
            return;
        }

        Log.d(this, "changeToCallTypeClicked id = "+id);
        switch (id){
            case R.id.changeToVideoRxButton:
                selectedCallProfile = VideoProfile.STATE_RX_ENABLED;
                break;
            case R.id.changeToVideoTxButton:
                selectedCallProfile = VideoProfile.STATE_TX_ENABLED;
                break;
        }

        SprdCallCommandClient.getInstance().requestVolteCallMediaChange(selectedCallProfile, mCall.getPhoneId(), mCall);
    }

    public interface CallButtonUi extends Ui {
        Context getContext();
        void setEnabled(boolean on);
        void setRecord(boolean on); // SPRD: Add for record
        void enableRecord(boolean on); // SPRD: Add for record
        void toggleRecord(); // SPRD: Add for record
        void setMute(boolean on);
        void enableMute(boolean enabled);
        void setHold(boolean on);
        void showHold(boolean show);
        void enableHold(boolean enabled);
        void showMerge(boolean show);
        void showSwap(boolean show);
        void showAddCall(boolean show);
        void enableAddCall(boolean enabled);
        void displayDialpad(boolean on);
        boolean isDialpadVisible();
        void setAudio(int mode);
        void setSupportedAudio(int mask);
        void showManageConferenceCallButton();
        void showGenericMergeButton();
        void hideExtraRow();
        void displayManageConferencePanel(boolean on);
        void showButtonUi(boolean show);// SPRD: add for Universe UI
        void updateButtonStates();
        void showButton(int buttonId, boolean show);
        void showOverflowButton(boolean show);
        void setPauseVideoButton(boolean isPaused);
        void setSwitchCameraButtonSelect(boolean isSwitched);
    }
}
