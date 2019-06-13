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

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnDismissListener;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.android.services.telephony.common.AudioMode;
import com.sprd.incallui.SprdUtils;
import android.util.SparseIntArray;
import static com.android.incallui.CallButtonFragment.Buttons.*;
import android.os.SystemProperties;

/**
 * Fragment for call control buttons
 */
public class CallButtonFragment
        extends BaseFragment<CallButtonPresenter, CallButtonPresenter.CallButtonUi>
        implements CallButtonPresenter.CallButtonUi, OnMenuItemClickListener, OnDismissListener,
        View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    /**
     * SPRD:
     * modify for Universe UI
     * Original Android code:
     *     ImageButton View mMuteButton;
     *     ImageButton View mAudioButton;
     *     ImageButton View mHoldButton;
     * @{
     */
    private View mMuteButton;
    private View mAudioButton;
    private View mHoldButton;
    private View mInCallControls;
    private TextView mAudioButtonLabel;
    /* @}
     */

    private ToggleButton mShowDialpadButton;
    private View mMergeButton;
    private View mAddCallButton;
    private View mSwapButton;
    private View mRecordButton;
    private View mSendsmsButton; // SPRD: add for send sms. See Bug624151
    private View  mVideoCallRxButton; //SPRD:Add for received video call only. Bug760076
    private View  mVideoCallTxButton; //SPRD:Add for broadcast video call only. Bug760076

    private PopupMenu mAudioModePopup;
    private boolean mAudioModePopupVisible;
    private View mEndCallButton;
    private View mExtraRowButton;
    private View mManageConferenceButton;
    private View mGenericMergeButton;

    private ImageButton mOverflowButton;
    private PopupMenu mOverflowPopup;
    private int mButtonMaxVisible;
    private View mChangeToVoiceButton;
    private View mChangeToVideoButton;
    private ToggleButton mSwitchCameraButton;
    private ToggleButton mPauseVideoButton;
    private TextView mLeftSkView;
    private TextView mCenterSkView;
    private TextView mRightSkView;
    //Sprd: Add for bug820120
    private boolean mOnClickManageConfButton = false;
    // The button is currently visible in the UI
    private static final int BUTTON_VISIBLE = 1;
    // The button is hidden in the UI
    private static final int BUTTON_HIDDEN = 2;
    // The button has been collapsed into the overflow menu
    private static final int BUTTON_MENU = 3;
    public interface Buttons {
        public static final int BUTTON_AUDIO = 0;
        public static final int BUTTON_MUTE = 1;
        public static final int BUTTON_DIALPAD = 2;
        public static final int BUTTON_HOLD = 3;
        public static final int BUTTON_SWAP = 4;
        public static final int BUTTON_UPGRADE_TO_VIDEO = 5;
        public static final int BUTTON_SWITCH_CAMERA = 6;
        public static final int BUTTON_ADD_CALL = 7;
        public static final int BUTTON_MERGE = 8;
        public static final int BUTTON_PAUSE_VIDEO = 9;
//        public static final int BUTTON_MANAGE_VIDEO_CONFERENCE = 10;
        // SPRD: Add for Recorder
        public static final int BUTTON_RECORD = 11;
//        public static final int BUTTON_INVITE = 12;
        public static final int BUTTON_CHANGED_TO_AUDIO = 12;
//        public static final int BUTTON_ECT = 14;            // SPRD: Porting Explicit Transfer Call.
        public static final int BUTTON_MANAGE_VOICE_CONFERENCE = 13;
        public static final int BUTTON_SEND_SMS = 14; // SPRD: Add for send sms. See Bug624151
        public static final int BUTTON_VIDEO_CALL_RX = 15;//SPRD:Add for received video call only. Bug760076
        public static final int BUTTON_VIDEO_CALL_TX = 16;//SPRD:Add for broadcast video call only. Bug760076
        public static final int BUTTON_COUNT = 17;
    }
    private SparseIntArray mButtonVisibilityMap = new SparseIntArray(BUTTON_COUNT);

    @Override
    CallButtonPresenter createPresenter() {
        // TODO: find a cleaner way to include audio mode provider than
        // having a singleton instance.
        return new CallButtonPresenter();
    }

    @Override
    CallButtonPresenter.CallButtonUi getUi() {
        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (SprdUtils.PIKEL_UI_SUPPORT) {
            mButtonMaxVisible = getResources().getInteger(R.integer.call_card_max_buttons);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View parent;
        // SPRD: add for Universe UI
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            parent = inflater.inflate(R.layout.call_button_fragment_sprd, container, false);
            mInCallControlsStub = (ViewStub)parent.findViewById(R.id.inCallControlsStub);
        } else {
            if (SprdUtils.PIKEL_UI_SUPPORT) {
                parent = inflater.inflate(R.layout.call_button_fragment_sprd_pikel, container, false);
                mOverflowButton = (ImageButton) parent.findViewById(R.id.overflowButton);
                mOverflowButton.setOnClickListener(this);
                mChangeToVoiceButton = (ImageButton) parent.findViewById(R.id.changeToVoiceButton);
                mChangeToVoiceButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    	getPresenter().changeToVoiceClicked();
                    }
                });
                mChangeToVideoButton = (ImageButton) parent.findViewById(R.id.changeToVideoButton);
                mChangeToVideoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    	getPresenter().changeToVideoClicked();
                    }
                });
                mSwitchCameraButton = (ToggleButton) parent.findViewById(R.id.switchCameraButton);
                mSwitchCameraButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    	getPresenter().switchCameraClicked(
                                mSwitchCameraButton.isSelected() /* useFrontFacingCamera */);
                        /* SPRD: modify for bug634571 @{ */
                        if(mPauseVideoButton!= null && mPauseVideoButton.isSelected()){
                           setPauseVideoButton(!mPauseVideoButton.isSelected() /* pause */);
                        }/* @ } */
                    }
                });
                mPauseVideoButton = (ToggleButton) parent.findViewById(R.id.pauseVideoButton);
                mPauseVideoButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    	getPresenter().pauseVideoClicked(
                                !mPauseVideoButton.isSelected() /* pause */);
                    }
                });
            } else {
                parent = inflater.inflate(R.layout.call_button_fragment, container, false);
                mExtraRowButton = parent.findViewById(R.id.extraButtonRow);

                mGenericMergeButton = parent.findViewById(R.id.cdmaMergeButton);
                mGenericMergeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getPresenter().mergeClicked();
                    }
                });
                mEndCallButton = parent.findViewById(R.id.endButton);
                mEndCallButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getPresenter().endCallClicked();
                    }
                });

                // make the hit target smaller for the end button so that is creates a deadzone
                // along the inside perimeter of the button.
                mEndCallButton.setOnTouchListener(new SmallerHitTargetTouchListener());
            }
            mMuteButton = (ImageButton) parent.findViewById(R.id.muteButton);
            mAudioButton = (ImageButton) parent.findViewById(R.id.audioButton);
            mHoldButton = (ImageButton) parent.findViewById(R.id.holdButton);
            mShowDialpadButton = (ToggleButton) parent.findViewById(R.id.dialpadButton);
            mAddCallButton = (ImageButton) parent.findViewById(R.id.addButton);
            mMergeButton = (ImageButton) parent.findViewById(R.id.mergeButton);
            mSwapButton = (ImageButton) parent.findViewById(R.id.swapButton);

            mRecordButton = (View) parent.findViewById(R.id.recordButton);/* SPRD: Add for record*/
            // SPRD: add for send sms. See Bug624151
            mSendsmsButton = (View) parent.findViewById(R.id.sendsmsButton);
            /* SPRD: Add for received/broadcast video call only. Bug760076@{*/
            mVideoCallRxButton = (ImageButton) parent.findViewById(R.id.changeToVideoRxButton);
            mVideoCallRxButton.setOnClickListener(this);
            mVideoCallTxButton = (ImageButton) parent.findViewById(R.id.changeToVideoTxButton);
            mVideoCallTxButton.setOnClickListener(this);
            /* @} */

            mMuteButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ImageButton button = (ImageButton) v;
                    getPresenter().muteClicked(!button.isSelected());
                }
            });

            mAudioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onAudioButtonClicked();
                }
            });

            mHoldButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    final ImageButton button = (ImageButton) v;
                    getPresenter().holdClicked(!button.isSelected());
                }
            });

            /* SPRD: add for send sms. See Bug624151 @{ */
            mSendsmsButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    getPresenter().sendSMSClicked();
                }
            });
            /* end @} */

            /* SPRD: Add for record @{ */
            mRecordButton.setOnClickListener(new OnClickListener(){
                @Override
                public void onClick(View v) {
                    final ImageButton button = (ImageButton) v;
                    getPresenter().recordClicked(!button.isSelected());
                }
            });/* @} */
            if (mRecordButton != null && getActivity() != null
                    && ((InCallActivity)getActivity()).getRecorderState()!= null
                    && ((InCallActivity)getActivity()).getRecorderState().isActive()) {
                setRecord(true);
            }
            mShowDialpadButton.setOnClickListener(this);
            mAddCallButton.setOnClickListener(this);
            mMergeButton.setOnClickListener(this);
            mSwapButton.setOnClickListener(this);

            mManageConferenceButton = parent.findViewById(R.id.manageConferenceButton);
            mManageConferenceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getPresenter().manageConferenceButtonClicked();
                    mOnClickManageConfButton = true;//Sprd: Add for bug820120
                }
            });
        }
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // set the buttons
        //SPRD:add for UUI
        if(!SprdUtils.UNIVERSE_UI_SUPPORT){
            updateAudioButtons(getPresenter().getSupportedAudio());
        }
    }

    @Override
    public void onResume() {
        if (getPresenter() != null) {
            getPresenter().refreshMuteState();
        }
        if (getPresenter() != null) {
            getPresenter().createPhoneStateListener();
            getPresenter().startMonitor();
        }
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (getPresenter() != null) {
            getPresenter().stopMonitor();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        Log.i(this, "onClick(View " + view + ", id " + id + ")...");

        switch(id) {
        case R.id.addButton:
            getPresenter().addCallClicked();
            break;
        case R.id.mergeButton:
            getPresenter().mergeClicked();
            break;
        case R.id.swapButton:
            getPresenter().swapClicked();
            break;
        case R.id.dialpadButton:
            getPresenter().showDialpadClicked(mShowDialpadButton.isChecked());
            break;
        case R.id.overflowButton:
            if (mOverflowPopup != null) {
                // mOverflowPopup.show(); //SPRD Bug#636347: Comment out for PikeL
            }
            break;
        /*SPRD: add for received/broadcast video call only{@*/
        case R.id.changeToVideoRxButton:
        case R.id.changeToVideoTxButton:
            getPresenter().changeToCallTypeClicked(id);
            break;
        /* @} */
        default:
            Log.wtf(this, "onClick: unexpected");
            break;
        }
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        /* SPRD: add for UUI @ { */
        if (SprdUtils.UNIVERSE_UI_SUPPORT && mInCallControls == null) {
            Log.i(this, "setEnabled,mInCallControls is null,should init view before update.");
            return;
        }
        /* @} */
        View view = getView();
        if (view.getVisibility() != View.VISIBLE) {
            view.setVisibility(View.VISIBLE);
        }

        mSendsmsButton.setEnabled(isEnabled); // SPRD: Add for send sms. See Bug624151
        mRecordButton.setEnabled(isEnabled); // SPRD: Add for record
        // The smaller buttons laid out horizontally just below the end-call button.
        mMuteButton.setEnabled(isEnabled);
        mAudioButton.setEnabled(isEnabled);
        mHoldButton.setEnabled(isEnabled);
        mShowDialpadButton.setEnabled(isEnabled);
        mMergeButton.setEnabled(isEnabled);
        mAddCallButton.setEnabled(isEnabled);
        mSwapButton.setEnabled(isEnabled);
        if (SprdUtils.PIKEL_UI_SUPPORT) {
            mOverflowButton.setEnabled(isEnabled);
            mManageConferenceButton.setEnabled(isEnabled);
            mChangeToVoiceButton.setEnabled(isEnabled);
            mChangeToVideoButton.setEnabled(isEnabled);
            mSwitchCameraButton.setEnabled(isEnabled);
            mPauseVideoButton.setEnabled(isEnabled);
            mVideoCallRxButton.setEnabled(isEnabled); //SPRD: Add for received video call only. Bug760076
            mVideoCallTxButton.setEnabled(isEnabled); //SPRD: Add for broadcast video call only. Bug760076
        } else {
            // The main end-call button spanning across the screen.
            mEndCallButton.setEnabled(isEnabled);
        }
    }

    @Override
    public void setMute(boolean value) {
        /* SPRD: add for UUI @ { */
        if(mMuteButton == null){
            Log.i(this, "setMute,Button is null,should init view before update.");
            return;
        }
        /* @} */
        mMuteButton.setSelected(value);
        /* SPRD: add for Bug637670 @{ */
        if (mOverflowPopup != null) {
            Menu menu = mOverflowPopup.getMenu();
            if (menu != null && menu.findItem(BUTTON_MUTE) != null && getContext() != null) {
                menu.findItem(BUTTON_MUTE).setTitle(getContext().getString(
                        value ? R.string.onscreenStopMuteText : R.string.onscreenMuteText));
            }
        }
        if (getContext() != null) {
            mMuteButton.setContentDescription(getContext().getString(
                    value ? R.string.onscreenStopMuteText : R.string.onscreenMuteText));
        }
        /* @} */
    }

    @Override
    public void enableMute(boolean enabled) {
        /* SPRD: add for UUI @ { */
        if(mMuteButton == null){
            Log.i(this, "setMute,Button is null,should init view before update.");
            return;
        }
        /* @} */
        mMuteButton.setEnabled(enabled);
    }

    @Override
    public void setHold(boolean value) {
        /* SPRD: add for UUI @ { */
        if(mHoldButton == null){
            Log.i(this, "setHold,Button is null,should init view before update.");
            return;
        }
        /* @} */
        mHoldButton.setSelected(value);
        mHoldButton.setContentDescription(getContext().getString(
                value ? R.string.onscreenHoldText_selected : R.string.onscreenHoldText_unselected));
    }

    @Override
    public void showHold(boolean show) {
        /* SPRD: add for UUI @ { */
        if(mHoldButton == null){
            Log.i(this, "showHold,Button is null,should init view before update.");
            return;
        }
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            View holdButtonLabel = getView().findViewById(R.id.holdButtonLabel);
            if(holdButtonLabel != null)
                holdButtonLabel.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
        /* @} */
        mHoldButton.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void enableHold(boolean enabled) {
        /* SPRD: add for UUI @ { */
        if(mHoldButton == null){
            Log.i(this, "enableHold,Button is null,should init view before update.");
            return;
        }
        /* @} */
        mHoldButton.setEnabled(enabled);
    }

    @Override
    public void showMerge(boolean show) {
        /* SPRD: add for UUI @ { */
        if(mMergeButton == null){
            Log.i(this, "showMerge,Button is null,should init view before update.");
            return;
        }
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            View mergeButtonLabel = getView().findViewById(R.id.mergeButtonLabel);
            if(mergeButtonLabel != null)
                mergeButtonLabel.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        /* @} */
        mMergeButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showSwap(boolean show) {
        /* SPRD: add for UUI @ { */
        if(mSwapButton == null){
            Log.i(this, "showSwap,Button is null,should init view before update.");
            return;
        }
        /* @} */
        mSwapButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showAddCall(boolean show) {
        /* SPRD: add for UUI @ { */
        if(mAddCallButton == null){
            Log.i(this, "showAddCall,Button is null,should init view before update.");
            return;
        }
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            View addButtonLabel = getView().findViewById(R.id.addButtonLabel);
            if(addButtonLabel != null)
                addButtonLabel.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        }
        /* @} */
        mAddCallButton.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void enableAddCall(boolean enabled) {
        /* SPRD: add for UUI @ { */
        if(mAddCallButton == null){
            Log.i(this, "enableAddCall,Button is null,should init view before update.");
            return;
        }
        /* @} */
        mAddCallButton.setEnabled(enabled);
    }

    @Override
    public void setAudio(int mode) {
        updateAudioButtons(getPresenter().getSupportedAudio());
        refreshAudioModePopup();
    }

    @Override
    public void setSupportedAudio(int modeMask) {
        updateAudioButtons(modeMask);
        refreshAudioModePopup();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Log.i(this, "- onMenuItemClick: " + item);
        Log.i(this, "  id: " + item.getItemId());
        Log.i(this, "  title: '" + item.getTitle() + "'");

        int mode = AudioMode.WIRED_OR_EARPIECE;

        switch (item.getItemId()) {
            case R.id.audio_mode_speaker:
                mode = AudioMode.SPEAKER;
                /* SPRD: add for Universe UI@ { */
                if(SprdUtils.UNIVERSE_UI_SUPPORT){
                    mAudioButtonLabel.setText(R.string.audio_mode_speaker);
                }
                /* @} */
                break;
            /*
             * SPRD: modify  bug264390
             * @orig case R.id.audio_mode_earpiece:
                     case R.id.audio_mode_wired_headset:
                    // InCallAudioMode.EARPIECE means either the handset earpiece,
                    // or the wired headset (if connected.)
                     mode = AudioMode.WIRED_OR_EARPIECE;
                     */
            case R.id.audio_mode_earpiece:
                mode = AudioMode.WIRED_OR_EARPIECE;
                if(SprdUtils.UNIVERSE_UI_SUPPORT){
                    mAudioButtonLabel.setText(R.string.audio_mode_earpiece);
                }
                break;
            case R.id.audio_mode_wired_headset:
                // InCallAudioMode.EARPIECE means either the handset earpiece,
                // or the wired headset (if connected.)
                mode = AudioMode.WIRED_OR_EARPIECE;
                if(SprdUtils.UNIVERSE_UI_SUPPORT){
                    mAudioButtonLabel.setText(R.string.audio_mode_wired_headset);
                }
                break;
                /* @} */
            case R.id.audio_mode_bluetooth:
                mode = AudioMode.BLUETOOTH;
                if(SprdUtils.UNIVERSE_UI_SUPPORT){
                    mAudioButtonLabel.setText(R.string.audio_mode_bluetooth);
                }
                break;
            default:
                Log.e(this, "onMenuItemClick:  unexpected View ID " + item.getItemId()
                        + " (MenuItem = '" + item + "')");
                break;
        }

        getPresenter().setAudioMode(mode);
        return true;
    }

    // PopupMenu.OnDismissListener implementation; see showAudioModePopup().
    // This gets called when the PopupMenu gets dismissed for *any* reason, like
    // the user tapping outside its bounds, or pressing Back, or selecting one
    // of the menu items.
    @Override
    public void onDismiss(PopupMenu menu) {
        Log.i(this, "- onDismiss: " + menu);
        mAudioModePopupVisible = false;
        //Sprd: Add for bug820120
        if (mOnClickManageConfButton) {
            updateRightKey();
            mOnClickManageConfButton = false;
        } else if (mCenterSkView != null && !TextUtils.isEmpty(mCenterSkView.getText()) &&
                mCenterSkView.getText().equals(getResources().getString(R.string.onscreenRejectMessageText))) {
            // do nothing
            Log.i(this, "onDismiss: mCenterSkView.getText() =" + mCenterSkView.getText());
        } else {
            updateKey(true);
        }
    }

    /**
     * Checks for supporting modes.  If bluetooth is supported, it uses the audio
     * pop up menu.  Otherwise, it toggles the speakerphone.
     */
    private void onAudioButtonClicked() {
        Log.i(this, "onAudioButtonClicked: " +
                AudioMode.toString(getPresenter().getSupportedAudio()));

        if (isSupported(AudioMode.BLUETOOTH)) {
            showAudioModePopup();
        } else {
            getPresenter().toggleSpeakerphone();
        }
    }

    /**
     * Refreshes the "Audio mode" popup if it's visible.  This is useful
     * (for example) when a wired headset is plugged or unplugged,
     * since we need to switch back and forth between the "earpiece"
     * and "wired headset" items.
     *
     * This is safe to call even if the popup is already dismissed, or even if
     * you never called showAudioModePopup() in the first place.
     */
    public void refreshAudioModePopup() {
        if (mAudioModePopup != null && mAudioModePopupVisible) {
            // Dismiss the previous one
            mAudioModePopup.dismiss();  // safe even if already dismissed
            // And bring up a fresh PopupMenu
            showAudioModePopup();
        }
    }

    /**
     * Updates the audio button so that the appriopriate visual layers
     * are visible based on the supported audio formats.
     */
    private void updateAudioButtons(int supportedModes) {
        /* SPRD: add for UUI @ { */
        if(mAudioButton == null){
            Log.i(this, "updateAudioButtons,mAudioButton is null,should init view before update.");
            return;
        }
        /* @} */
        final boolean bluetoothSupported = isSupported(AudioMode.BLUETOOTH);
        final boolean speakerSupported = isSupported(AudioMode.SPEAKER);
        /* SPRD: add for bug264390 @ { */
        final boolean wireHeadsetSupported = isSupported(AudioMode.WIRED_HEADSET);
        /* @} */
        boolean audioButtonEnabled = false;
        boolean audioButtonChecked = false;
        boolean showMoreIndicator = false;

        boolean showBluetoothIcon = false;
        boolean showSpeakerphoneOnIcon = false;
        boolean showSpeakerphoneOffIcon = false;
        boolean showHandsetIcon = false;

        boolean showToggleIndicator = false;

        if (bluetoothSupported) {
            Log.i(this, "updateAudioButtons - popup menu mode");

            audioButtonEnabled = true;
            showMoreIndicator = true;
            // The audio button is NOT a toggle in this state.  (And its
            // setChecked() state is irrelevant since we completely hide the
            // btn_compound_background layer anyway.)

            // Update desired layers:
            if (isAudio(AudioMode.BLUETOOTH)) {
                showBluetoothIcon = true;
            } else if (isAudio(AudioMode.SPEAKER)) {
                showSpeakerphoneOnIcon = true;
            } else {
                showHandsetIcon = true;
                // TODO: if a wired headset is plugged in, that takes precedence
                // over the handset earpiece.  If so, maybe we should show some
                // sort of "wired headset" icon here instead of the "handset
                // earpiece" icon.  (Still need an asset for that, though.)
            }
        } else if (speakerSupported) {
            Log.i(this, "updateAudioButtons - speaker toggle mode");

            /* SPRD: bug 320575
            @orig
            audioButtonEnabled = true;
            @{ */
            audioButtonEnabled = CallList.getInstance().existsLiveCall();
            /* @} */

            // The audio button *is* a toggle in this state, and indicated the
            // current state of the speakerphone.
            audioButtonChecked = isAudio(AudioMode.SPEAKER);

            // update desired layers:
            showToggleIndicator = true;

            showSpeakerphoneOnIcon = isAudio(AudioMode.SPEAKER);
            showSpeakerphoneOffIcon = !showSpeakerphoneOnIcon;
        } else {
            Log.i(this, "updateAudioButtons - disabled...");

            // The audio button is a toggle in this state, but that's mostly
            // irrelevant since it's always disabled and unchecked.
            audioButtonEnabled = false;
            audioButtonChecked = false;

            // update desired layers:
            showToggleIndicator = true;
            showSpeakerphoneOffIcon = true;
        }

        // Finally, update it all!

        Log.i(this, "audioButtonEnabled: " + audioButtonEnabled);
        Log.i(this, "audioButtonChecked: " + audioButtonChecked);
        Log.i(this, "showMoreIndicator: " + showMoreIndicator);
        Log.i(this, "showBluetoothIcon: " + showBluetoothIcon);
        Log.i(this, "showSpeakerphoneOnIcon: " + showSpeakerphoneOnIcon);
        Log.i(this, "showSpeakerphoneOffIcon: " + showSpeakerphoneOffIcon);
        Log.i(this, "showHandsetIcon: " + showHandsetIcon);

        // Constants for Drawable.setAlpha()
        final int HIDDEN = 0;
        final int VISIBLE = 255;

        mAudioButton.setEnabled(audioButtonEnabled);
        mAudioButton.setSelected(audioButtonChecked);

        /* SPRD:Bug 634171 Update Audio description when set audio */
        int audioMode = getPresenter().getAudioMode();
        if (mOverflowPopup != null) {
            Menu menu = mOverflowPopup.getMenu();
            if (menu != null && menu.findItem(BUTTON_AUDIO) != null && getContext() != null) {
                menu.findItem(BUTTON_AUDIO).setTitle(getContext().getString(
                        audioMode == AudioMode.SPEAKER ? R.string.audio_mode_earpiece
                                : R.string.audio_mode_speaker));
            }
        }
        if (audioMode == AudioMode.SPEAKER) {
            mAudioButton.setContentDescription(getContext().getString(R.string.audio_mode_earpiece));
        }else{
            mAudioButton.setContentDescription(getContext().getString(R.string.audio_mode_speaker));
        }
        /* SPRD:Bug 634171 end */
        final LayerDrawable layers = (LayerDrawable) mAudioButton.getBackground();
        Log.i(this, "'layers' drawable: " + layers);

        layers.findDrawableByLayerId(R.id.compoundBackgroundItem)
                .setAlpha(showToggleIndicator ? VISIBLE : HIDDEN);

        layers.findDrawableByLayerId(R.id.moreIndicatorItem)
                .setAlpha(showMoreIndicator ? VISIBLE : HIDDEN);

        layers.findDrawableByLayerId(R.id.bluetoothItem)
                .setAlpha(showBluetoothIcon ? VISIBLE : HIDDEN);

        layers.findDrawableByLayerId(R.id.handsetItem)
                .setAlpha(showHandsetIcon ? VISIBLE : HIDDEN);

        layers.findDrawableByLayerId(R.id.speakerphoneOnItem)
                .setAlpha(showSpeakerphoneOnIcon ? VISIBLE : HIDDEN);

        layers.findDrawableByLayerId(R.id.speakerphoneOffItem)
                .setAlpha(showSpeakerphoneOffIcon ? VISIBLE : HIDDEN);
        /* SPRD: add for Universe UI @ { */
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            if(showBluetoothIcon){
                mAudioButtonLabel.setText(R.string.audio_mode_bluetooth);
            }else if(showSpeakerphoneOffIcon || showSpeakerphoneOnIcon){
                mAudioButtonLabel.setText(R.string.audio_mode_speaker);
            }else if(showHandsetIcon && !wireHeadsetSupported){
                mAudioButtonLabel.setText(R.string.audio_mode_earpiece);
            }else if(showHandsetIcon && wireHeadsetSupported){
                mAudioButtonLabel.setText(R.string.audio_mode_wired_headset);
            }
        }
        /* @} */
    }

    private void showAudioModePopup() {
        Log.i(this, "showAudioPopup()...");
        /* SPRD: Modified for Bug626106 @{ */
        if (mLeftSkView != null && mLeftSkView.isShown()) {
            mAudioModePopup = new PopupMenu(getView().getContext(), mLeftSkView /* anchorView */);
        } else if (mOverflowButton != null && mOverflowButton.isShown()) {
            mAudioModePopup = new PopupMenu(getView().getContext(), mOverflowButton /* anchorView */);
        } else {
            mAudioModePopup = new PopupMenu(getView().getContext(), mAudioButton /* anchorView */);
        }
        /* @} */
        mAudioModePopup.getMenuInflater().inflate(R.menu.incall_audio_mode_menu,
                mAudioModePopup.getMenu());
        mAudioModePopup.setOnMenuItemClickListener(this);
        mAudioModePopup.setOnDismissListener(this);

        final Menu menu = mAudioModePopup.getMenu();

        // TODO: Still need to have the "currently active" audio mode come
        // up pre-selected (or focused?) with a blue highlight.  Still
        // need exact visual design, and possibly framework support for this.
        // See comments below for the exact logic.

        final MenuItem speakerItem = menu.findItem(R.id.audio_mode_speaker);
        speakerItem.setEnabled(isSupported(AudioMode.SPEAKER));
        // TODO: Show speakerItem as initially "selected" if
        // speaker is on.

        // We display *either* "earpiece" or "wired headset", never both,
        // depending on whether a wired headset is physically plugged in.
        final MenuItem earpieceItem = menu.findItem(R.id.audio_mode_earpiece);
        final MenuItem wiredHeadsetItem = menu.findItem(R.id.audio_mode_wired_headset);

        final boolean usingHeadset = isSupported(AudioMode.WIRED_HEADSET);
        earpieceItem.setVisible(!usingHeadset);
        earpieceItem.setEnabled(!usingHeadset);
        wiredHeadsetItem.setVisible(usingHeadset);
        wiredHeadsetItem.setEnabled(usingHeadset);
        // TODO: Show the above item (either earpieceItem or wiredHeadsetItem)
        // as initially "selected" if speakerOn and
        // bluetoothIndicatorOn are both false.

        final MenuItem bluetoothItem = menu.findItem(R.id.audio_mode_bluetooth);
        bluetoothItem.setEnabled(isSupported(AudioMode.BLUETOOTH));
        // TODO: Show bluetoothItem as initially "selected" if
        // bluetoothIndicatorOn is true.

        mAudioModePopup.show();
        // Unfortunately we need to manually keep track of the popup menu's
        // visiblity, since PopupMenu doesn't have an isShowing() method like
        // Dialogs do.
        mAudioModePopupVisible = true;
    }

    private boolean isSupported(int mode) {
        return (mode == (getPresenter().getSupportedAudio() & mode));
    }

    private boolean isAudio(int mode) {
        return (mode == getPresenter().getAudioMode());
    }

    @Override
    public void displayDialpad(boolean value) {
        /* SPRD: add for UUI @ { */
        if(mShowDialpadButton == null){
            Log.i(this, "displayDialpad,mShowDialpadButton is null,should init view before update.");
            return;
        }
        /* @} */
        mShowDialpadButton.setChecked(value);
        if (getActivity() != null && getActivity() instanceof InCallActivity) {
            ((InCallActivity) getActivity()).displayDialpad(value);
        }
    }

    @Override
    public boolean isDialpadVisible() {
        if (getActivity() != null && getActivity() instanceof InCallActivity) {
            return ((InCallActivity) getActivity()).isDialpadVisible();
        }
        return false;
    }

    @Override
    public void displayManageConferencePanel(boolean value) {
        if (getActivity() != null && getActivity() instanceof InCallActivity) {
            ((InCallActivity) getActivity()).displayManageConferencePanel(value);
        }
    }


    @Override
    public void showManageConferenceCallButton() {
        mExtraRowButton.setVisibility(View.VISIBLE);
        mManageConferenceButton.setVisibility(View.VISIBLE);
        mGenericMergeButton.setVisibility(View.GONE);
    }

    @Override
    public void showGenericMergeButton() {
        mExtraRowButton.setVisibility(View.VISIBLE);
        mManageConferenceButton.setVisibility(View.GONE);
        mGenericMergeButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideExtraRow() {
       mExtraRowButton.setVisibility(View.GONE);
    }

    /* SPRD: Add for record */
    @Override
    public void toggleRecord() {
        // TODO Auto-generated method stub
        if (getActivity() != null && getActivity() instanceof InCallActivity) {
            ((InCallActivity) getActivity()).toggleRecord();
        }
    }
    /* SPRD: Add for record */
    @Override
    public void setRecord(boolean on) {
        // TODO Auto-generated method stub
        if(mRecordButton == null){
            Log.i(this, "setRecord,Button is null,should init view before update.");
            return;
        }
        mRecordButton.setSelected(on);
        /* SPRD: Update record description when set record @{ */
        if (mOverflowPopup != null) {
            Menu menu = mOverflowPopup.getMenu();
            if (menu != null && menu.findItem(BUTTON_RECORD) != null && getContext() != null) {
                menu.findItem(BUTTON_RECORD).setTitle(getContext().getString(
                        on ? R.string.onscreenShowStopRecodText
                                : R.string.onscreenShowRecodText));
            }
        }
        if (getContext() != null) {
            mRecordButton.setContentDescription(getContext().getString(
                    on ? R.string.onscreenShowStopRecodText
                            : R.string.onscreenShowRecodText));
        }
    }
    /* SPRD: Add for record */
    @Override
    public void enableRecord(boolean on) {
        // TODO Auto-generated method stub
        mRecordButton.setEnabled(on);
    }


    /**
     * SPRD:
     * add for Universe &Pikel UI
     * @{
     */
    View mCallButtonView;
    ViewStub mInCallControlsStub;
    public void showButtonUi(boolean show){
        if(show){
            if(mInCallControlsStub != null && mInCallControls == null){
                mCallButtonView = mInCallControlsStub.inflate();
                initViews();
            } else if(mInCallControls != null){
                mInCallControls.setVisibility(View.VISIBLE);
            }
            if(mAudioButton != null){
                updateAudioButtons(getPresenter().getSupportedAudio());
            }
            getPresenter().refreshMuteState();
            setMute(AudioModeProvider.getInstance().getMute());
        } else if(mInCallControls != null) {
            mInCallControls.setVisibility(View.GONE);
        }
    }

    private void initViews(){
        mInCallControls = mCallButtonView.findViewById(R.id.inCallControls);
        mMuteButton = (ToggleButton) mCallButtonView.findViewById(R.id.muteButton);
        mAudioButton = (ToggleButton) mCallButtonView.findViewById(R.id.audioButton);
        mHoldButton = (ToggleButton) mCallButtonView.findViewById(R.id.holdButton);
        mShowDialpadButton = (ToggleButton) mCallButtonView.findViewById(R.id.dialpadButton);
        mAddCallButton = (ToggleButton) mCallButtonView.findViewById(R.id.addButton);
        mMergeButton = (ToggleButton) mCallButtonView.findViewById(R.id.mergeButton);
        mSwapButton = (ToggleButton) mCallButtonView.findViewById(R.id.swapButton);
        mAudioButtonLabel = (TextView) mCallButtonView.findViewById(R.id.audioButtonLabel);

        mRecordButton = (ToggleButton) mCallButtonView.findViewById(R.id.recordButton);

        mMuteButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final Button button = (Button) v;//SPRD:change ImageButton to button for UUI.
                getPresenter().muteClicked(!button.isSelected());
            }
        });

        mAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onAudioButtonClicked();
            }
        });

        mHoldButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final Button button = (Button) v;//SPRD:change ImageButton to button for UUI.
                getPresenter().holdClicked(!button.isSelected());
            }
        });

        /* SPRD: Add for record @{ */
        mRecordButton.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                final Button button = (Button) v;//SPRD:change ImageButton to button for UUI.
                getPresenter().recordClicked(!button.isSelected());
            }
        });/* @} */
        mShowDialpadButton.setOnClickListener(this);
        mAddCallButton.setOnClickListener(this);
        mMergeButton.setOnClickListener(this);
        mSwapButton.setOnClickListener(this);

        mExtraRowButton = mCallButtonView.findViewById(R.id.extraButtonRow);

        mManageConferenceButton = mCallButtonView.findViewById(R.id.manageConferenceButton);
        mManageConferenceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPresenter().manageConferenceButtonClicked();
                mOnClickManageConfButton = true;//Sprd: Add for bug820120
            }
        });
        mGenericMergeButton = mCallButtonView.findViewById(R.id.cdmaMergeButton);
        mGenericMergeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPresenter().mergeClicked();
            }
        });

        mEndCallButton = mCallButtonView.findViewById(R.id.endButton);
        mEndCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPresenter().endCallClicked();
            }
        });

         // make the hit target smaller for the end button so that is creates a deadzone
         // along the inside perimeter of the button.
         mEndCallButton.setOnTouchListener(new SmallerHitTargetTouchListener());
    }
    /* @}
     */

    /**
     * Iterates through the list of buttons and toggles their visibility depending on the
     * setting configured by the CallButtonPresenter. If there are more visible buttons than
     * the allowed maximum, the excess buttons are collapsed into a single overflow menu.
     */
    @Override
    public void updateButtonStates() {
        View prevVisibleButton = null;
        int prevVisibleId = -1;
        PopupMenu menu = null;
        int visibleCount = 0;
        for (int i = 0; i < BUTTON_COUNT; i++) {
            final int visibility = mButtonVisibilityMap.get(i);
            final View button = getButtonById(i);
            if (visibility == BUTTON_VISIBLE) {
                visibleCount++;
                if (visibleCount <= mButtonMaxVisible) {
                    button.setVisibility(View.VISIBLE);
//                      if(button == mPauseVideoButton){
//                          if(InCallPresenter.getInstance().getInCallCameraManager().isCameraPaused()){
//                             mPauseVideoButton.setSelected(true);
//                         }
//                       }
                    prevVisibleButton = button;
                    prevVisibleId = i;
                } else {
                    if (menu == null) {
                        menu = getPopupMenu();
                    }
                    // Collapse the current button into the overflow menu. If is the first visible
                    // button that exceeds the threshold, also collapse the previous visible button
                    // so that the total number of visible buttons will never exceed the threshold.
                    if (prevVisibleButton != null) {
                        addToOverflowMenu(prevVisibleId, prevVisibleButton, menu);
                        prevVisibleButton = null;
                        prevVisibleId = -1;
                    }
                    addToOverflowMenu(i, button, menu);
                }
            } else if (visibility == BUTTON_HIDDEN){
                button.setVisibility(View.GONE);
            }
        }

        //mOverflowButton.setVisibility(menu != null ? View.VISIBLE : View.GONE);
        if (menu != null) {
            if (mOverflowPopup != null && mOverflowPopup.getMenu()!=null && mOverflowPopup.getMenu().hasVisibleItems()) {
                mOverflowPopup.dismiss();
            }
            mOverflowPopup = menu;
            mOverflowPopup.setOnDismissListener(this);
            mOverflowPopup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    final int id = item.getItemId();
//                    if(id == BUTTON_PAUSE_VIDEO){
//                        if(InCallPresenter.getInstance().getInCallCameraManager().isCameraPaused()){
//                           mPauseVideoButton.setSelected(true);
//                       }else{
//                           mPauseVideoButton.setSelected(false);
//                       }
//                     }
                    getButtonById(id).performClick();
                    updateKey(true);
                    return true;
                }
            });
        }
    }

    private View getButtonById(int id) {
        switch (id) {
            case BUTTON_AUDIO:
                return mAudioButton;
            case BUTTON_MUTE:
                return mMuteButton;
            case BUTTON_DIALPAD:
                return mShowDialpadButton;
            case BUTTON_HOLD:
                return mHoldButton;
            case BUTTON_SWAP:
                return mSwapButton;
            case BUTTON_UPGRADE_TO_VIDEO:
                return mChangeToVideoButton;
            case BUTTON_SWITCH_CAMERA:
                return mSwitchCameraButton;
            case BUTTON_ADD_CALL:
                return mAddCallButton;
            case BUTTON_MERGE:
                return mMergeButton;
            case BUTTON_PAUSE_VIDEO:
                return mPauseVideoButton;
//            case BUTTON_MANAGE_VIDEO_CONFERENCE:
//                return mManageVideoCallConferenceButton;
            case BUTTON_RECORD:
                return mRecordButton;
//            case BUTTON_INVITE:
//                return mInviteButton;
            case BUTTON_CHANGED_TO_AUDIO:
                return mChangeToVoiceButton;
//            case BUTTON_ECT:
//                return mTransferButton;
            case BUTTON_SEND_SMS:
                return mSendsmsButton;
            case BUTTON_MANAGE_VOICE_CONFERENCE:
                return mManageConferenceButton;
            /*SPRD: add for received/broadcast video call only{@*/
            case BUTTON_VIDEO_CALL_RX:
                return mVideoCallRxButton;
            case BUTTON_VIDEO_CALL_TX:
                return mVideoCallTxButton;
            /*@}*/
            default:
                Log.w(this, "Invalid button id");
                return null;
        }
    }

    @Override
    public void showButton(int buttonId, boolean show) {
        Log.d(this,"buttonId:"+buttonId+",show:"+show);
        mButtonVisibilityMap.put(buttonId, show ? BUTTON_VISIBLE : BUTTON_HIDDEN);
    }

    private void addToOverflowMenu(int id, View button, PopupMenu menu) {
        button.setVisibility(View.GONE);
        menu.getMenu().add(Menu.NONE, id, Menu.NONE, button.getContentDescription());
        mButtonVisibilityMap.put(id, BUTTON_MENU);
    }

    private PopupMenu getPopupMenu() {
        if (getActivity() != null && getActivity() instanceof InCallActivity) {
            mLeftSkView = ((InCallActivity)getActivity()).mLeftSkView;
            mCenterSkView = ((InCallActivity)getActivity()).mCenterSkView;
            mRightSkView = ((InCallActivity)getActivity()).mRightSkView;

        }
        return new PopupMenu(new ContextThemeWrapper(getActivity(), R.style.InCallPopupMenuStyle),
                mLeftSkView/*mOverflowButton*/);
    }

    @Override
    public void showOverflowButton(boolean show) {
        //mOverflowButton.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void leftSkDown() {
        if (getActivity() != null && getActivity() instanceof InCallActivity) {
            if (mOverflowPopup!=null) {
                mOverflowPopup.show();
                updateKey(false);
            }
        }
    }

    @Override
    public Context getContext() {
        return getActivity();
    }

    @Override
    public void setPauseVideoButton(boolean isPaused) {
        Log.d(this,"setPauseVideoButton..isPaused: "+isPaused+"  mPauseVideoButton.isSelected: "+mPauseVideoButton.isSelected());
        if (mPauseVideoButton.isSelected() != isPaused) {
            mPauseVideoButton.setSelected(isPaused);
            //add for SPRD:Bug 635166 update title of pause video
            if (mOverflowPopup != null) {
                Menu menu = mOverflowPopup.getMenu();
                if (menu != null && menu.findItem(BUTTON_PAUSE_VIDEO) != null && getContext() != null) {
                    menu.findItem(BUTTON_PAUSE_VIDEO).setTitle(getContext().getString(
                            isPaused ? R.string.onscreenResumeVideoText
                                    : R.string.onscreenPauseVideoText));
                }
            }
            if (getContext() != null) {
                mPauseVideoButton.setContentDescription(getContext().getString(
                        isPaused ? R.string.onscreenResumeVideoText
                                : R.string.onscreenPauseVideoText));
            }
        }
    }
    /* SPRD: modify for bug634571 @{ */
    @Override
    public void setSwitchCameraButtonSelect(boolean isBackFacingCamera) {
        Log.d(this,"setSwitchCameraButtonSelect..isBackFacingCamera: "+isBackFacingCamera);
        mSwitchCameraButton.setSelected(isBackFacingCamera);
    }
    /* @ } */

    public void updateKey(boolean b) {
        if (mLeftSkView != null && mCenterSkView != null && mRightSkView != null) {
            Log.d(this, "updateKey----b = " + b);
            if(b) {
                mLeftSkView.setText(getResources().getString(R.string.onscreenOptionText));
                mCenterSkView.setText("");
                mRightSkView.setText(getResources().getString(R.string.onscreenHangupText));//change by hujingcheng 20180929
            }else {
                mLeftSkView.setText("");
                mCenterSkView.setText(getResources().getString(R.string.onscreenChooseText));
                mRightSkView.setText(getResources().getString(R.string.onscreenHangupText));
            }
        }
    }

    //Sprd: Add for bug820120
    public void updateRightKey() {
        if (mRightSkView != null) {
            mRightSkView.setText(getResources().getString(R.string.onscreenHangupText));
        }
    }

}
