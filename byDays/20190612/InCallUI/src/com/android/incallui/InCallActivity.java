/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.incallui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.Call.State;
import com.sprd.android.config.OptConfig;
import com.sprd.incallui.BluetoothManagerHelper;
import com.sprd.incallui.Recorder;
import com.sprd.incallui.Recorder.OnStateChangedListener;
import com.sprd.incallui.SaveContacts;
import com.sprd.incallui.SprdUtils;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;
import android.os.SystemProperties;

import com.android.incallui.InCallPresenter.InCallState;
import com.sprd.android.support.featurebar.FeatureBarHelper;
import android.widget.TextView;
import android.widget.PopupMenu;
import android.view.MenuItem;
import com.android.ims.ImsCallProfile;
import android.telecom.VideoProfile;
/**
 * Phone app "in call" screen.
 */
public class InCallActivity extends Activity {

    public static final String SHOW_DIALPAD_EXTRA = "InCallActivity.show_dialpad";

    private static final int INVALID_RES_ID = -1;
    private Call mCall;
    private int mCallId = 99;

    private CallButtonFragment mCallButtonFragment;
    private CallCardFragment mCallCardFragment;
    private AnswerFragment mAnswerFragment;
    private DialpadFragment mDialpadFragment;
    private ConferenceManagerFragment mConferenceManagerFragment;
    private boolean mIsForegroundActivity;
    private AlertDialog mDialog;

    /** Use to pass 'showDialpad' from {@link #onNewIntent} to {@link #onResume} */
    private boolean mShowDialpadRequested;
    /* SPRD: Add for recorder @{ */
    private Recorder mRecorder;
    private String mRecordingLabelStr;
    private BroadcastReceiver mSDCardMountEventReceiver;
    /* @} */
    private Recorder.State mRecorderState;
    private DTMFEntryFragment mDTMFEntryFragment;
    private FragmentManager mChildFragmentManager;
    private AudioManager mAudioManager;
    private FeatureBarHelper mFeatureBarHelper;
    public TextView mLeftSkView;
    public TextView mCenterSkView;
    public TextView mRightSkView;
    private static final int TEXTSIZE_OF_FEATUREBAR = 19;
    private String mLsk = "";
    private String mCsk = "";
    private String mRsk = "";
    private int mVideoState = VideoProfile.STATE_AUDIO_ONLY;
    // SPRD: Fix bug#635122
    private static final String ACTION_INCALL_VISIBLE = "com.android.incallui.ACTION_INCALL_VISIBLE";
    private static final String EXTRA_VISIBLE = "is_visible";
    Intent mInCallVisibleIntent = new Intent(ACTION_INCALL_VISIBLE);

    /*SPRD: bug 360344 @{*/
    private static final int EVENT_WINDOW_TRANS_CONVERT = 0;
    // SPRD: add for bug679096.
    private TelephonyManager mTelephonyManager;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_WINDOW_TRANS_CONVERT:
                    InCallActivity.this.convertFromTranslucent();
                    break;
                default:
                    break;
            }
        }
    };

    /* @}*/
    @Override
    protected void onCreate(Bundle icicle) {
        Log.d(this, "onCreate()...  this = " + this);

        super.onCreate(icicle);

        // set this flag so this activity will stay in front of the keyguard
        // Have the WindowManager filter out touch events that are "too fat".
        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES;

        getWindow().addFlags(flags);


        // TODO(klp): Do we need to add this back when prox sensor is not available?
        // lp.inputFeatures |= WindowManager.LayoutParams.INPUT_FEATURE_DISABLE_USER_ACTIVITY;

        // Inflate everything in incall_screen.xml and add it to the screen.
        if (SprdUtils.PIKEL_UI_SUPPORT) {
             // Setup action bar for the conference call manager.
            requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.hide();
            }
            setContentView(R.layout.incall_screen_sprd_pikel);
        } else {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            if (SprdUtils.UNIVERSE_UI_SUPPORT){
                setContentView(R.layout.incall_screen_sprd);// SPRD: add for Universe UI
            } else {
                setContentView(R.layout.incall_screen);
            }
        }
        mFeatureBarHelper = new FeatureBarHelper(this);
        mLeftSkView = (TextView)mFeatureBarHelper.getOptionsKeyView();
        mCenterSkView =(TextView)mFeatureBarHelper.getCenterKeyView();
        mRightSkView = (TextView)mFeatureBarHelper.getBackKeyView();
        mLeftSkView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,TEXTSIZE_OF_FEATUREBAR);
        mCenterSkView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,TEXTSIZE_OF_FEATUREBAR);
        mRightSkView.setTextSize(TypedValue.COMPLEX_UNIT_DIP,TEXTSIZE_OF_FEATUREBAR);
        mCenterSkView.setMaxWidth(100);
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        initializeInCall();
        /* SPRD: Add for recorder @{ */
        mRecorder = Recorder.getInstance(getApplicationContext());
        mRecorder.setOnStateChangedListener(mRecorderStateChangedListener);
        registerExternalStorageStateListener();
        /* SPRD: add for customer pad@{ */
        if(SystemProperties.getInt("ro.sf.hwrotation", 0) == 270){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        /* @} */
        // SPRD: add for bug361319.
        initDataFromDialer(getIntent());
        Log.d(this, "onCreate(): exit");
        /* SPRD: add for bug 679096@{ */
         mTelephonyManager = TelephonyManager.getDefault();
        /* @} */
    }

    @Override
    protected void onStart() {
        Log.d(this, "onStart()...");
        super.onStart();

        // setting activity should be last thing in setup process
        InCallPresenter.getInstance().setActivity(this);
    }

    @Override
    protected void onResume() {
        Log.i(this, "onResume()...");
        super.onResume();

        mIsForegroundActivity = true;
        InCallPresenter.getInstance().onUiShowing(true);

        if (mShowDialpadRequested) {
            mCallButtonFragment.displayDialpad(true);
            mShowDialpadRequested = false;
        }
        /* SPRD: Add for recorder @{ */
        mRecordingLabelStr = getResources().getString(R.string.recording);
        mRecorder.notifyCurrentState();
        /* @} */
        /* SPRD: add for I Log  @ { */
        if (android.util.Log.isIloggable()) {
            android.util.Log.stopPerfTracking("PhonePerf : MO.Call.display end");
        }
        /* @}*/
        /* SPRD: add for Bug307590 @ { */
        InCallPresenter.getInstance().resumeDialCharWait();
        /* @}*/
        /*SPRD: bug 360344 @{*/
        mHandler.sendEmptyMessageDelayed(EVENT_WINDOW_TRANS_CONVERT,800);
        /* @}*/
         /* SPRD: add for Feature 5s screen off */
        mInCallVisibleIntent.putExtra(EXTRA_VISIBLE, true);
        sendBroadcast(mInCallVisibleIntent);
    }

    /** SPRD: only be used to call killStopFrontApp */
    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (OptConfig.LC_RAM_SUPPORT) {
            try {
                ActivityManagerNative.getDefault().killStopFrontApp(
                                     ActivityManager.CANCEL_KILL_STOP_TIMEOUT);
            } catch (Exception e) {
                Log.e(this, "killStopFrontApp : CANCEL_KILL_STOP_TIMEOUT");
                e.printStackTrace();
            }
        }
    }
    /** @} */

    // onPause is guaranteed to be called when the InCallActivity goes
    // in the background.
    @Override
    protected void onPause() {
        Log.d(this, "onPause()...");
        super.onPause();
        mIsForegroundActivity = false;

        mDialpadFragment.onDialerKeyUp(null);

        InCallPresenter.getInstance().onUiShowing(false);
        /* SPRD: add for I Log  @ { */
        if (android.util.Log.isIloggable()) {
            android.util.Log.stopPerfTracking("PhonePerf : HangUpCall end");
        }
        /* @}*/
         /* SPRD: add for Feature 5s screen off */
        mInCallVisibleIntent.putExtra(EXTRA_VISIBLE, false);
        sendBroadcast(mInCallVisibleIntent);
    }

    @Override
    protected void onStop() {
        Log.d(this, "onStop()...");
        if (hasPendingErrorDialog()) {
            mDialog.dismiss();
            mDialog = null;//SPRD:add for Bug283382
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.d(this, "onDestroy()...  this = " + this);
        unRegisterExternalStorageStateListener(); /* SPRD: Add for record */
        /* SPRD: modify for Bug386775 @{ */
        if(InCallPresenter.getInstance().isLatestAvitvity(this)){
        /* @} */
            InCallPresenter.getInstance().setActivity(null);
        }
        /* SPRD: When activity is destroyed, turn off speaker if no active call exists. @{*/
        if (!CallList.getInstance().existsLiveCall()) {
            CallCommandClient.getInstance().speaker(false);
        }

        // Clear Dialed char wait string
        InCallPresenter.getInstance().storeDialCharWait(0, null);
        /* @} */
        super.onDestroy();
    }

    /**
     * Returns true when theActivity is in foreground (between onResume and onPause).
     */
    /* package */ boolean isForegroundActivity() {
        return mIsForegroundActivity;
    }

    public boolean hasPendingErrorDialog() {
        return mDialog != null;
    }
    /**
     * Dismisses the in-call screen.
     *
     * We never *really* finish() the InCallActivity, since we don't want to get destroyed and then
     * have to be re-created from scratch for the next call.  Instead, we just move ourselves to the
     * back of the activity stack.
     *
     * This also means that we'll no longer be reachable via the BACK button (since moveTaskToBack()
     * puts us behind the Home app, but the home app doesn't allow the BACK key to move you any
     * farther down in the history stack.)
     *
     * (Since the Phone app itself is never killed, this basically means that we'll keep a single
     * InCallActivity instance around for the entire uptime of the device.  This noticeably improves
     * the UI responsiveness for incoming calls.)
     */
    @Override
    public void finish() {
        Log.i(this, "finish().  Dialog showing: " + (mDialog != null));

        // skip finish if we are still showing a dialog.
        if (!hasPendingErrorDialog() && !mAnswerFragment.hasPendingDialogs()) {
            super.finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(this, "onNewIntent: intent = " + intent);

        // We're being re-launched with a new Intent.  Since it's possible for a
        // single InCallActivity instance to persist indefinitely (even if we
        // finish() ourselves), this sequence can potentially happen any time
        // the InCallActivity needs to be displayed.

        // Stash away the new intent so that we can get it in the future
        // by calling getIntent().  (Otherwise getIntent() will return the
        // original Intent from when we first got created!)
        setIntent(intent);

        // Activities are always paused before receiving a new intent, so
        // we can count on our onResume() method being called next.

        // Just like in onCreate(), handle the intent.
        internalResolveIntent(intent);
    }

    @Override
    public void onBackPressed() {
        Log.d(this, "onBackPressed()...");
        // BACK is also used to exit out of any "special modes" of the
        // in-call UI:

        if (mDialpadFragment != null && mDialpadFragment.isVisible()) {
            mCallButtonFragment.displayDialpad(false);  // do the "closing" animation
            return;
        } else if (mConferenceManagerFragment.isVisible()) {
            mConferenceManagerFragment.setVisible(false);
            updateRightKeyWithText(mRsk);
            return;
        } else if (SprdUtils.UNIVERSE_UI_SUPPORT && mAnswerFragment != null && mAnswerFragment.isMessageListDisplayed()){
            mAnswerFragment.displayMessageList(false);// SPRD: add for Universe UI
            return;
        }

        // Always disable the Back key while an incoming call is ringing
        final Call call = CallList.getInstance().getIncomingCall();
        if (call != null) {
            Log.d(this, "Consume Back press for an inconing call");
            return;
        }

        // Nothing special to do.  Fall back to the default behavior.
        super.onBackPressed();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        android.util.Log.i("zhangsu","keyCode-----"+keyCode);
        // push input to the dialer.
        if ((mDialpadFragment.isVisible()) && (mDialpadFragment.onDialerKeyUp(event))){
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_CALL) {
            // Always consume CALL to be sure the PhoneWindow won't do anything with it
            return true;
        } else if (mDTMFEntryFragment != null && mDTMFEntryFragment.isVisible()&& mDTMFEntryFragment.onDialerKeyUp(event)) {
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (!isConferenceManagerFragmentVisibile()) {
                leftSkDown();
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            //centerSkDown();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!isConferenceManagerFragmentVisibile()) {
                rightSkdown();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        android.util.Log.i("zhangsu","keyCode-----"+keyCode);
        switch (keyCode) {
            case KeyEvent.KEYCODE_CALL:
                boolean handled = InCallPresenter.getInstance().handleCallKey();
                if (!handled) {
                    Log.w(this, "InCallActivity should always handle KEYCODE_CALL in onKeyDown");
                }
                // Always consume CALL to be sure the PhoneWindow won't do anything with it
                return true;

            // Note there's no KeyEvent.KEYCODE_ENDCALL case here.
            // The standard system-wide handling of the ENDCALL key
            // (see PhoneWindowManager's handling of KEYCODE_ENDCALL)
            // already implements exactly what the UI spec wants,
            // namely (1) "hang up" if there's a current active call,
            // or (2) "don't answer" if there's a current ringing call.

            case KeyEvent.KEYCODE_CAMERA:
                // Disable the CAMERA button while in-call since it's too
                // easy to press accidentally.
                return true;

            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                // Ringer silencing handled by PhoneWindowManager.
                break;

            case KeyEvent.KEYCODE_MUTE:
                // toggle mute
                CallCommandClient.getInstance().mute(!AudioModeProvider.getInstance().getMute());
                return true;

            // Various testing/debugging features, enabled ONLY when VERBOSE == true.
            case KeyEvent.KEYCODE_SLASH:
                if (Log.VERBOSE) {
                    Log.v(this, "----------- InCallActivity View dump --------------");
                    // Dump starting from the top-level view of the entire activity:
                    Window w = this.getWindow();
                    View decorView = w.getDecorView();
                    decorView.debug();
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_EQUALS:
                // TODO: Dump phone state?
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                /* SPRD: add for Bug 679096 @ { */
                if (mTelephonyManager != null && mTelephonyManager.isRinging()) {
                    mTelephonyManager.silenceRinger();
                    return true;
                }
                /* @}*/
                if (mConferenceManagerFragment == null || !mConferenceManagerFragment.isVisible()) {
                    mAudioManager.adjustStreamVolume(BluetoothManagerHelper.getInstance(getApplicationContext())
                            .isBluetoothAudioConnected() ? AudioManager.STREAM_BLUETOOTH_SCO : AudioManager.STREAM_VOICE_CALL,
                            AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                /* SPRD: add for Bug 679096 @ { */
                if (mTelephonyManager != null && mTelephonyManager.isRinging()) {
                    mTelephonyManager.silenceRinger();
                    return true;
                }
                /* @}*/
                if (mConferenceManagerFragment == null || !mConferenceManagerFragment.isVisible()) {
                    mAudioManager.adjustStreamVolume(BluetoothManagerHelper.getInstance(getApplicationContext())
                            .isBluetoothAudioConnected() ? AudioManager.STREAM_BLUETOOTH_SCO : AudioManager.STREAM_VOICE_CALL,
                            AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
                    return true;
                }
                break;
        }

        if (event.getRepeatCount() == 0 && handleDialerKeyDown(keyCode, event)) {
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private boolean handleDialerKeyDown(int keyCode, KeyEvent event) {
        Log.v(this, "handleDialerKeyDown: keyCode " + keyCode + ", event " + event + "...");

        // As soon as the user starts typing valid dialable keys on the
        // keyboard (presumably to type DTMF tones) we start passing the
        // key events to the DTMFDialer's onDialerKeyDown.
        if (mDialpadFragment.isVisible()) {
            return mDialpadFragment.onDialerKeyDown(event);

            // TODO: If the dialpad isn't currently visible, maybe
            // consider automatically bringing it up right now?
            // (Just to make sure the user sees the digits widget...)
            // But this probably isn't too critical since it's awkward to
            // use the hard keyboard while in-call in the first place,
            // especially now that the in-call UI is portrait-only...
        }

        switch (keyCode) {
        case KeyEvent.KEYCODE_0:
        case KeyEvent.KEYCODE_1:
        case KeyEvent.KEYCODE_2:
        case KeyEvent.KEYCODE_3:
        case KeyEvent.KEYCODE_4:
        case KeyEvent.KEYCODE_5:
        case KeyEvent.KEYCODE_6:
        case KeyEvent.KEYCODE_7:
        case KeyEvent.KEYCODE_8:
        case KeyEvent.KEYCODE_9:
        case KeyEvent.KEYCODE_STAR:
        case KeyEvent.KEYCODE_POUND:
            if (mDTMFEntryFragment == null || !mDTMFEntryFragment.isVisible()) {
                showDTMFEntryFragment(true);
                mDTMFEntryFragment.initDigits(event);
                Log.d(this,"handleDialerKeyDown:show DTMF entey");
            } else {
                mDTMFEntryFragment.initDigits(event);
            }
            Log.d(this,"handleDialerKeyDown:handle DTMF event = " + event);
            return mDTMFEntryFragment.onDialerKeyDown(event);

        default:
            break;
        }

        return false;
    }

    void showDTMFEntryFragment(boolean show) {
        final FragmentTransaction ft = mChildFragmentManager.beginTransaction();
        View fragmentContainer = findViewById(R.id.DTMFEntryFragmentContainer);
        mDTMFEntryFragment = new DTMFEntryFragment();
        ft.replace(fragmentContainer.getId(), mDTMFEntryFragment,
                DTMFEntryFragment.class.getName());
        ft.commitAllowingStateLoss();
        mChildFragmentManager.executePendingTransactions();
        mDTMFEntryFragment.setVisible(show);
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        // SPRD: call through to super.onConfigurationChanged()
        super.onConfigurationChanged(config);
        InCallPresenter.getInstance().getProximitySensor().onConfigurationChanged(config);
    }

    public CallButtonFragment getCallButtonFragment() {
        return mCallButtonFragment;
    }

    private void internalResolveIntent(Intent intent) {
        final String action = intent.getAction();

        if (action.equals(intent.ACTION_MAIN)) {
            // This action is the normal way to bring up the in-call UI.
            //
            // But we do check here for one extra that can come along with the
            // ACTION_MAIN intent:

            if (intent.hasExtra(SHOW_DIALPAD_EXTRA)) {
                // SHOW_DIALPAD_EXTRA can be used here to specify whether the DTMF
                // dialpad should be initially visible.  If the extra isn't
                // present at all, we just leave the dialpad in its previous state.

                final boolean showDialpad = intent.getBooleanExtra(SHOW_DIALPAD_EXTRA, false);
                Log.d(this, "- internalResolveIntent: SHOW_DIALPAD_EXTRA: " + showDialpad);

                relaunchedFromDialer(showDialpad);
            }

            return;
        }
    }

    private void relaunchedFromDialer(boolean showDialpad) {
        mShowDialpadRequested = showDialpad;

        if (mShowDialpadRequested) {
            // If there's only one line in use, AND it's on hold, then we're sure the user
            // wants to use the dialpad toward the exact line, so un-hold the holding line.
            final Call call = CallList.getInstance().getActiveOrBackgroundCall();
            if (call != null && call.getState() == State.ONHOLD) {
                CallCommandClient.getInstance().hold(call.getCallId(), false);
            }
        }
    }

    private void initializeInCall() {
        if (mCallButtonFragment == null) {
            mCallButtonFragment = (CallButtonFragment) getFragmentManager()
                    .findFragmentById(R.id.callButtonFragment);
            mCallButtonFragment.getView().setVisibility(View.INVISIBLE);
        }

        if (mCallCardFragment == null) {
            mCallCardFragment = (CallCardFragment) getFragmentManager()
                    .findFragmentById(R.id.callCardFragment);
            mChildFragmentManager = mCallCardFragment.getChildFragmentManager();
        }

        if (mAnswerFragment == null) {
            mAnswerFragment = (AnswerFragment) getFragmentManager()
                    .findFragmentById(R.id.answerFragment);
        }

        if (mDialpadFragment == null) {
            mDialpadFragment = (DialpadFragment) getFragmentManager()
                    .findFragmentById(R.id.dialpadFragment);
            getFragmentManager().beginTransaction().hide(mDialpadFragment).commit();
        }

        if (mConferenceManagerFragment == null) {
            mConferenceManagerFragment = (ConferenceManagerFragment) getFragmentManager()
                    .findFragmentById(R.id.conferenceManagerFragment);
            mConferenceManagerFragment.getView().setVisibility(View.INVISIBLE);
        }
    }

    private void toast(String text) {
        final Toast toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);

        toast.show();
    }

    /**
     * Simulates a user click to hide the dialpad. This will update the UI to show the call card,
     * update the checked state of the dialpad button, and update the proximity sensor state.
     */
    public void hideDialpadForDisconnect() {
        mCallButtonFragment.displayDialpad(false);
    }

    public void dismissKeyguard(boolean dismiss) {
        if (dismiss) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
    }

    public void displayDialpad(boolean showDialpad) {
        final FragmentTransaction ft = getFragmentManager().beginTransaction();
        // SPRD: add for Universe UI
        if (SprdUtils.UNIVERSE_UI_SUPPORT) {
            mDialpadFragment.setVisible(showDialpad);
            mCallCardFragment.setVisible(!showDialpad);
        }
        if (showDialpad) {
            ft.setCustomAnimations(R.anim.incall_dialpad_slide_in, 0);
            ft.show(mDialpadFragment);
        } else {
            ft.setCustomAnimations(0, R.anim.incall_dialpad_slide_out);
            ft.hide(mDialpadFragment);
        }
        ft.commitAllowingStateLoss();

        InCallPresenter.getInstance().getProximitySensor().onDialpadVisible(showDialpad);
    }

    public boolean isDialpadVisible() {
        return mDialpadFragment != null && mDialpadFragment.isVisible();
    }

    public void displayManageConferencePanel(boolean showPanel) {
        if (showPanel) {
            mConferenceManagerFragment.setVisible(true);
            updateRightKeyWithText(mRsk);
        }
    }

    public void showPostCharWaitDialog(int callId, String chars) {
        /* SPRD: add for Bug307590 @ { */
        if(!mIsForegroundActivity){
            Log.w(this, "Shouldn't show PostCharWaitDialog when Activity in background.");
            InCallPresenter.getInstance().storeDialCharWait(callId, chars);
            return;
        }
        /* @}*/
        final PostCharDialogFragment fragment = new PostCharDialogFragment(callId,  chars);
        fragment.show(getFragmentManager(), "postCharWait");
        /* SPRD: add for Bug307590 @ { */
        InCallPresenter.getInstance().storeDialCharWait(0, null);
        /* @}*/
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (mCallCardFragment != null) {
            mCallCardFragment.dispatchPopulateAccessibilityEvent(event);
        }
        return super.dispatchPopulateAccessibilityEvent(event);
    }

    public void maybeShowErrorDialogOnDisconnect(Call.DisconnectCause cause) {
        Log.d(this, "maybeShowErrorDialogOnDisconnect");

        if (!isFinishing()) {
            final int resId = getResIdForDisconnectCause(cause);
            if (resId != INVALID_RES_ID) {
                showErrorDialog(resId);
            }
        }
    }

    public void dismissPendingDialogs() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
        mAnswerFragment.dismissPendingDialogues();
    }

    /**
     * Utility function to bring up a generic "error" dialog.
     */
    private void showErrorDialog(int resId) {
        final CharSequence msg = getResources().getText(resId);
        Log.i(this, "Show Dialog: " + msg);

        dismissPendingDialogs();

        mDialog = new AlertDialog.Builder(this)
            .setMessage(msg)
            .setPositiveButton(R.string.ok, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    onDialogDismissed();
                }})
            .setOnCancelListener(new OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    onDialogDismissed();
                }})
            .create();

        mDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        mDialog.show();
    }

    private int getResIdForDisconnectCause(Call.DisconnectCause cause) {
        int resId = INVALID_RES_ID;

        if (cause == Call.DisconnectCause.CALL_BARRED) {
            resId = R.string.callFailed_cb_enabled;
        } else if (cause == Call.DisconnectCause.FDN_BLOCKED) {
            resId = R.string.callFailed_fdn_only;
        } else if (cause == Call.DisconnectCause.CS_RESTRICTED) {
            resId = R.string.callFailed_dsac_restricted;
        } else if (cause == Call.DisconnectCause.CS_RESTRICTED_EMERGENCY) {
            resId = R.string.callFailed_dsac_restricted_emergency;
        } else if (cause == Call.DisconnectCause.CS_RESTRICTED_NORMAL) {
            resId = R.string.callFailed_dsac_restricted_normal;
        }

        return resId;
    }

    private void onDialogDismissed() {
        mDialog = null;
        InCallPresenter.getInstance().onDismissDialog();
    }

    public void setRecorderState(Recorder.State state) {
        mRecorderState = state;
    }

    public Recorder.State getRecorderState() {
        return mRecorderState;
    }

    /* SPRD: Add for recorder @{ */
    private OnStateChangedListener mRecorderStateChangedListener = new OnStateChangedListener() {
        public void onTimeChanged(long time) {
            if (SprdUtils.PIKEL_UI_SUPPORT) {
                mCallCardFragment.setRecordText(DateUtils.formatElapsedTime(time / 1000));
            } else if (SprdUtils.UNIVERSE_UI_SUPPORT){
                mCallCardFragment.setRecordText(DateUtils.formatElapsedTime(time / 1000));
            } else {
                mCallCardFragment.setRecordText(mRecordingLabelStr + DateUtils.formatElapsedTime(time / 1000));
            }
        }

        public void onStateChanged(Recorder.State state) {
            setRecorderState(state);
            if (mCallButtonFragment != null) {
                mCallButtonFragment.setRecord(state.isActive());
            }
            if (mCallCardFragment != null && mCallCardFragment.getRecordingLabel() != null) {
              mCallCardFragment.setRecordingVisibility(state.isActive() ? View.VISIBLE : View.GONE);
            }
        }

        @Override
        public void onShowMessage(int type, String msg) {
            String res = null;
            boolean resetIcon = false;
            switch (type) {
                case Recorder.TYPE_ERROR_SD_NOT_EXIST:
                    res = getString(R.string.no_sd_card);
                    resetIcon = true;
                    break;
                case Recorder.TYPE_ERROR_SD_FULL:
                    res = getString(R.string.storage_is_full);
                    resetIcon = true;
                    break;
                case Recorder.TYPE_ERROR_SD_ACCESS:
                    res = getString(R.string.sdcard_access_error);
                    resetIcon = true;
                    break;
                case Recorder.TYPE_ERROR_IN_RECORD:
                    res = getString(R.string.used_by_other_applications);
                    resetIcon = true;
                    break;
                case Recorder.TYPE_ERROR_INTERNAL:
                    resetIcon = true;
                    break;
                case Recorder.TYPE_MSG_PATH:
                case Recorder.TYPE_SAVE_FAIL:
                    res = msg;
                    resetIcon = true;
                    break;
            }
            if (mCallButtonFragment != null && resetIcon) {
                mCallButtonFragment.setRecord(false);
            }
            Log.d(this, " toast message: " + res);
            if(!TextUtils.isEmpty(res)){
                Toast.makeText(InCallActivity.this, res, Toast.LENGTH_SHORT).show();
            }
        }
	};

    private void registerExternalStorageStateListener() {
        if (mSDCardMountEventReceiver == null) {
            mSDCardMountEventReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    boolean hasSdcard = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
                    if (mRecorder != null && !hasSdcard) {
                        mRecorder.stop();
                    }
                }
            };
            IntentFilter iFilter = new IntentFilter();
            iFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
            iFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
            iFilter.addDataScheme("file");
            registerReceiver(mSDCardMountEventReceiver, iFilter);
        }
    }

    private void unRegisterExternalStorageStateListener() {
        if (mSDCardMountEventReceiver != null) {
            unregisterReceiver(mSDCardMountEventReceiver);
        }
    }

    public void toggleRecord() {
        InCallPresenter.getInstance().toggleRecorder();
    }
    /* @} */

    /**
     * SPRD:
     * add for Universe UI
     * @{
     */
    public void showSaveContactActivity(final Call call){
        if(mCallCardFragment == null) {
            Log.w("SaveContacts","mCallCardFragment is null!");
            return;
        }
        if(mIsNumberShouldBeSave){
            String callTime = mCallCardFragment.getCallTime();
            mSaveContactIntent.putExtra(SaveContacts.CALL_TIME, callTime);
            startActivity(mSaveContactIntent);
            mIsNumberShouldBeSave = false;
        }
        mCallCardFragment.clearCallParameters();
    }

    private boolean mIsNumberShouldBeSave = false;
    private Intent mSaveContactIntent;

    public void checkNumberIsShouldBeSave(final Call call){
        if(mIsNumberShouldBeSave){
            Log.i("SaveContacts","mIsNumberShouldBeSave is true!");
            return;
        }
        if(mCallCardFragment == null) {
            Log.w("SaveContacts","mCallCardFragment is null!");
            return;
        }
        if(call == null || call.isIncoming()){
            Log.w("SaveContacts","call is null or isIncoming!");
            return;
        }
        new Thread(){
            public void run(){
                String phoneNumber = mCallCardFragment.getCallNumber();
                String phoneName = mCallCardFragment.getCallName();
                String originalNumber = phoneNumber;
                String originalName = phoneName;

                if(phoneNumber == null && phoneName != null){
                    if(!containDigit(phoneName)){
                        Log.i("SaveContacts","phoneName not contain Digit!");
                        return;
                    }
                    phoneName = PhoneNumberUtils.normalizeNumber(phoneName);
                } else if(phoneNumber != null){
                    if(!containDigit(phoneNumber)){
                        Log.i("SaveContacts","phoneNumber not contain Digit!");
                        return;
                    }
                    phoneNumber = PhoneNumberUtils.normalizeNumber(phoneNumber);
                }
                Log.i(this,"showSaveContactActivity,The number:"+phoneNumber+"phoneName="+phoneName
                        +" originalNumber="+originalNumber+" originalName="+originalName);
                if(phoneNumber != null && PhoneNumberUtils.isEmergencyNumber(phoneNumber) ||
                        phoneName != null && PhoneNumberUtils.isEmergencyNumber(phoneName)){
                    Log.i(this,"showSaveContactActivity,The number is emergencyNumber");
                    return;
                }
                mSaveContactIntent = new Intent(InCallActivity.this,SaveContacts.class);
                if((phoneNumber != null && !SaveContacts.checkPhoneNumber(phoneNumber, InCallActivity.this))){
                    mSaveContactIntent.putExtra(SaveContacts.PHONE_NAME, originalNumber);
                    mIsNumberShouldBeSave = true;
                } else if ((phoneNumber == null || (!TextUtils.isDigitsOnly(phoneNumber)) && !phoneNumber.startsWith("+"))
                        && phoneName != null && !SaveContacts.checkPhoneNumber(phoneName, InCallActivity.this)){
                    mSaveContactIntent.putExtra(SaveContacts.PHONE_NAME, originalName);
                    mIsNumberShouldBeSave = true;
                }
            }
        }.start();
    }

    private boolean containDigit(String content) {
        boolean flag = false;
        Pattern p = Pattern.compile(".*\\d+.*");
        Matcher m = p.matcher(content);
        if (m.matches())
        flag = true;
        return flag;
   }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP
                || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN
                || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_MUTE){
            updateIncomingMuteButton();
        }
        return super.dispatchKeyEvent(event);
    }

    /* SPRD: modify for Bug348353 @{ */
    public void updateIncomingMuteButton(){
        if(mAnswerFragment != null){
            mAnswerFragment.updateIncomingMuteButton();
        }
    }
    /* @} */

    public void refreshCallButtonFragmentUi(InCallState state, Call call){
        if(mCallButtonFragment != null){
            mCallButtonFragment.getPresenter().refreshUi(state,call);
        }
    }

    /**
     * SPRD: add for bug361319. @{
     * @param intent
     */
    private void initDataFromDialer(Intent intent) {
        if (intent != null && intent.getAction().equals(Intent.ACTION_MAIN)) {
            if (intent.hasExtra(SHOW_DIALPAD_EXTRA)) {
                mShowDialpadRequested = intent.getBooleanExtra(SHOW_DIALPAD_EXTRA, false);
            }
        }
    }
    /** @} */

    public CallCardFragment getCallCardFragment() {
        return mCallCardFragment;
    }

    /** SPRD: Add for bug 631607 for featurebar@{ */
    private boolean isCallButtonFragmentVisibile() {
        return mCallButtonFragment != null && mCallButtonFragment.isVisible();
    }

    private boolean isAnswerFragmentVisibile() {
        return mAnswerFragment != null && mAnswerFragment.isVisible();
    }

    private boolean isConferenceManagerFragmentVisibile() {
        return mConferenceManagerFragment != null && mConferenceManagerFragment.isVisible();
    }

    public ViewGroup getFeatureBar() {
        if (mFeatureBarHelper != null) {
            return mFeatureBarHelper.getFeatureBar();
        }
        return  null;
    }

    private void leftSkDown() {
        final Call call = CallList.getInstance().getFirstCall();
        if (call == null) {
            return;
        }
        if (mLeftSkView != null && !TextUtils.isEmpty(mLeftSkView.getText())) {
            if (mLeftSkView.getText().equals(getResources().getString(R.string.onscreenAnswerText))) {
                if (call.isIncoming() && isAnswerFragmentVisibile()) {
                    if (call.isVideo() && SystemProperties.getBoolean("persist.sys.support.add_vtmenu", true)) {
                        showPopUpMenu();//add for bug 713095
                    } else {
                	    mAnswerFragment.leftSkDown();
                    }
                }
            } else if (isCallButtonFragmentVisibile()) {
                mCallButtonFragment.leftSkDown();
            }
        }
    }

    public void showPopUpMenu() { //add for bug 713095 688629
        final  PopupMenu mPopUpOnlyMenu = new PopupMenu(this, mLeftSkView);
        mPopUpOnlyMenu.getMenuInflater().inflate(R.menu.popup_only,mPopUpOnlyMenu.getMenu());
        mPopUpOnlyMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener () {
            int videoState = VideoProfile.STATE_AUDIO_ONLY;
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                case R.id.video_call:
                    videoState = VideoProfile.STATE_BIDIRECTIONAL;
                    break;
                case R.id.voice_call:
                    videoState = VideoProfile.STATE_AUDIO_ONLY;
                    break;
                default:
                    break;
                }
                Call mCall = CallList.getInstance().getIncomingCall();
                if (mCall != null){
                    Log.d(this, "videoState = " + videoState );
                    mAnswerFragment.leftSkDown(videoState);
                }
                return false;
            }
    });
        mPopUpOnlyMenu.show();
    }

    private void centerSkDown() {
        final Call call = CallList.getInstance().getFirstCall();
        if (call == null) {
            return;
        }
        if (mCenterSkView != null && !TextUtils.isEmpty(mCenterSkView.getText())) {
            String centerSkText = mCenterSkView.getText().toString();
            Log.d(this, "centerSkDown :" + centerSkText);
            if (centerSkText.equals(getSkString(R.string.onscreenRejectMessageText))) {
                if (isAnswerFragmentVisibile()) {
                    mAnswerFragment.centerSkDown();
                }
            }
        }
    }

    private void rightSkdown() {
        final Call call = CallList.getInstance().getFirstCall();
        if (call == null) {
            return;
        }
        if (mRightSkView != null && !TextUtils.isEmpty(mRightSkView.getText())) {

             int mRingerMode =  mAudioManager.getRingerMode();
             android.util.Log.i("zhangsu","mRingerMode-----"+mRingerMode);
             //mCallCardFragment.getPresenter().endCallClicked();

            if (mRightSkView.getText().equals(getSkString(R.string.onscreenHangupText))) {
                mCallCardFragment.getPresenter().endCallClicked();
                //CallCommandClient.getInstance().disconnectCall(call.getCallId());
                //finish(); 
	//change by hujingcheng 20180929 rightButton to Back fuction end

            } 
            if (mRightSkView.getText().equals(getSkString(R.string.onscreenMuteText))) {
                mTelephonyManager.silenceRinger();
                //mAudioManager.setRingerMode(0);
                //updateRightKeyWithText(getString(R.string.onscreenHangupText));
                mRightSkView.setText(getString(R.string.onscreenHangupText));
            }
        
            return ;
        }
    }

    private void updateSoftKeyWithText(String lsk, String csk, String rsk) {
        updateLeftKeyWithText(lsk);
        updateCenterKeyWithText(csk);
        updateRightKeyWithText(rsk);
        getFeatureBar().invalidate();
    }

    private void updateLeftKeyWithText(String lsk) {
        mLeftSkView.setText(lsk);
    }

    private void updateCenterKeyWithText(String csk) {
        mCenterSkView.setText(csk);
    }

    private void updateRightKeyWithText(String rsk) {
        if(mAudioManager.getRingerMode() == 0){
            rsk = /*isConferenceManagerFragmentVisibile() ?*/ getSkString(R.string.onscreenHangupText);//change by hujingcheng 20180929
                //: getSkString(R.string.onscreenHangupText);
        } else {
            rsk = /*isConferenceManagerFragmentVisibile() ?*/ getSkString(R.string.onscreenMuteText);//change by hujingcheng 20180929
                //: getSkString(R.string.onscreenHangupText);
            
        }
        mRightSkView.setText(rsk);
    }

    public void updateSoftKeyWithText(InCallState newState) {
        Log.d(this,"newState="+newState);
        if (newState == InCallState.INCALL || newState == InCallState.OUTGOING
                || newState == InCallState.PREPARE_UI) {
            mLsk = getSkString(R.string.onscreenOptionText);
            mCsk = null;
            if(mAudioManager.getRingerMode() == 0){
                mRsk = getSkString(R.string.onscreenHangupText);//change by hujingcheng 20180929
            }else {
                mRsk = getSkString(R.string.onscreenMuteText);//change by hujingcheng 20180929
            }
        } else if (newState == InCallState.INCOMING) {
            mLsk = getSkString(R.string.onscreenAnswerText);
            mCsk = getSkString(R.string.onscreenRejectMessageText);
            if(mAudioManager.getRingerMode() == 0){
                mRsk = getSkString(R.string.onscreenHangupText);//change by hujingcheng 20180929
            } else {
                mRsk = getSkString(R.string.onscreenMuteText);//change by hujingcheng 20180929
            }
        }else if (newState == InCallState.NO_CALLS) {
            mLsk = null;
            mCsk = null;
            mRsk = null;
        }
        updateSoftKeyWithText(mLsk,mCsk,mRsk);
    }

    private String getSkString(int resId) {
        return getResources().getString(resId);
    }
    /** @} */

    /* add for bug 670373 dismiss pending dialogs when disconnected @{ */
    public void dismissPendingDialogsOfAnswerFragment() {
        if (mAnswerFragment != null) {
            mAnswerFragment.dismissPendingDialogues();
        }
    }
    /* @} */
}
