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

import android.animation.LayoutTransition;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.incallui.InCallPresenter.InCallState;
import com.android.internal.telephony.TelephonyIntents;
import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.Call.Capabilities;

import com.sprd.incallui.geocode.GeocodeHelper;
import com.sprd.incallui.InCallUITelcelHelper;
import com.sprd.incallui.SprdUtils;
import com.sprd.internal.telephony.CpSupportUtils;

import android.provider.Settings;
import android.sim.Sim;
import android.sim.SimManager;

import java.util.List;

/**
 * Fragment for call card.
 */
public class CallCardFragment extends BaseFragment<CallCardPresenter, CallCardPresenter.CallCardUi>
        implements CallCardPresenter.CallCardUi {

    // Primary caller info
    private TextView mPhoneNumber;
    private TextView mNumberLabel;
    private TextView mPrimaryName;
    /* SPRD bug 301615 add sim name color start */
    private TextView mCallSimLabel;
    /* SPRD bug 301615 add sim name color end */
    private TextView mCallStateLabel;
    private TextView mCallTypeLabel;
    private TextView mRecordingLabel; // SPRD: Add for recorder
    private ImageView mPhoto;
    private ImageView HdVoiceView; // SPRD: add for HIGH_DEF_AUDIO
    private ImageView mWifiCallView;
    private TextView mElapsedTime;
    private View mProviderInfo;
    private TextView mProviderLabel;
    private TextView mProviderNumber;
    private ViewGroup mSupplementaryInfoContainer;

    //add for vt
    private View mPrimaryCallCardInfoContainer;

    // Secondary caller info
    /* SPRD: modify for Universe UI @{ */
    private View mSecondaryCallInfo;
    private View mProviderInfoView;
    private ViewStub mProviderInfoStub;
    /* @} */
    private TextView mSecondaryCallName;
    private ImageView mSecondaryPhoto;
    private View mSecondaryPhotoOverlay;

    /* SPRD: Phone calls attribution @{ */
    private TextView mGeocodeView;
    SimManager mSimManager;
    /* @} */

    // Cached DisplayMetrics density.
    private float mDensity;
    private ImageView mRecordingIcon;
    private TextView mRecordtext;
    private ImageView mEndCallButton;
    private TextView mSecondaryPhoneNumber;

    @Override
    CallCardPresenter.CallCardUi getUi() {
        return this;
    }

    @Override
    CallCardPresenter createPresenter() {
        return new CallCardPresenter();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final CallList calls = CallList.getInstance();
        final Call call = calls.getFirstCall();
        getPresenter().init(getActivity(), call);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mDensity = getResources().getDisplayMetrics().density;
        View view;
        // SPRD: add for Universe UI
        if(SprdUtils.PIKEL_UI_SUPPORT) {
            view = inflater.inflate(R.layout.call_card_sprd_pikel, container, false);
            mSimManager = SimManager.get(getActivity());
        } else if (SprdUtils.UNIVERSE_UI_SUPPORT){
            view = inflater.inflate(R.layout.call_card_sprd, container, false);
            mSimManager = SimManager.get(getActivity());
        } else {
            view = inflater.inflate(R.layout.call_card, container, false);
        }
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mPhoneNumber = (TextView) view.findViewById(R.id.phoneNumber);
        mPrimaryName = (TextView) view.findViewById(R.id.name);
        mNumberLabel = (TextView) view.findViewById(R.id.label);

        mSecondaryCallInfo = (View) view.findViewById(R.id.secondary_call_info);//SPRD: modify for Universe UI

        mPhoto = (ImageView) view.findViewById(R.id.photo);
        /* SPRD bug 301615 add sim name color start */
        if(SprdUtils.PIKEL_UI_SUPPORT || SprdUtils.UNIVERSE_UI_SUPPORT){
            mCallSimLabel = (TextView) view.findViewById(R.id.callSimLabel);
        }
        /* SPRD bug 301615 add sim name color end */
        HdVoiceView = (ImageView) view.findViewById(R.id.hd_voice); //SPRD: add for HIGH_DEF_AUDIO
        mWifiCallView = (ImageView) view.findViewById(R.id.wifi_call);
        mCallStateLabel = (TextView) view.findViewById(R.id.callStateLabel);
        mCallTypeLabel = (TextView) view.findViewById(R.id.callTypeLabel);
        mRecordingLabel = (TextView) view.findViewById(R.id.recordinglabel); //SPRD: Add for recorder

        if (SprdUtils.PIKEL_UI_SUPPORT) {
            mRecordingIcon = (ImageView) view.findViewById(R.id.recordingIcon);
            mRecordtext =  (TextView) view.findViewById(R.id.recordtext);
            if (mRecordingLabel != null && getActivity() != null
                    && ((InCallActivity)getActivity()).getRecorderState()!= null
                    && ((InCallActivity)getActivity()).getRecorderState().isActive()) {
                mRecordingLabel.setVisibility(View.VISIBLE);
                mRecordingIcon.setVisibility(View.VISIBLE);
                mRecordtext.setVisibility(View.VISIBLE);
            }
            mEndCallButton = (ImageView)view.findViewById(R.id.end_call_button);
            mEndCallButton.setOnClickListener(new View.OnClickListener() {
                @Override
                 public void onClick(View v) {
                     getPresenter().endCallClicked();
               }
            });

            //add for vt
            mPrimaryCallCardInfoContainer = view.findViewById(R.id.primay_call_info_container_pikel);
        }
        mElapsedTime = (TextView) view.findViewById(R.id.elapsedTime);
        /* SPRD: modify for layout optimization @ {
         * @Orig:
        mProviderInfo = view.findViewById(R.id.providerInfo);
        mProviderLabel = (TextView) view.findViewById(R.id.providerLabel);
        mProviderNumber = (TextView) view.findViewById(R.id.providerAddress);
         */
        mProviderInfoStub = (ViewStub)view.findViewById(R.id.provider_info_stub);
        /*@} */
        mSupplementaryInfoContainer =
            (ViewGroup) view.findViewById(R.id.supplementary_info_container);

        /* SPRD: for call geocode @ { */
        mGeocodeView = (TextView)view.findViewById(R.id.geocode);
        /* SPRD: Modify for bug789304 @{
         * if(mGeocodeView != null){
            mGeocodeView.setVisibility(GeocodeHelper.isSupportSprdGeocode() ? View.VISIBLE : View.GONE);
        }*/
        /* @} */
    }

    @Override
    public void setVisible(boolean on) {
        if (on) {
            getView().setVisibility(View.VISIBLE);
        } else {
            getView().setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void setPrimaryName(String name, boolean nameIsNumber, boolean isRealNoName) {
	//android.util.Log.d("callcard","name="+name+",nameIsNumber="+nameIsNumber+",isRealNoName="+isRealNoName); //mark       
	if (TextUtils.isEmpty(name)) {
            mPrimaryName.setText("");
            mPrimaryName.setVisibility(View.GONE);
        } else {
            /* SPRD: add for bug325479 @ { */
            mPrimaryName.setVisibility(View.VISIBLE);
            if(isRealNoName){
                mPrimaryName.setText(getActivity().getString(R.string.unknown_contacts));
            }else{
                mPrimaryName.setText(name);
            }
            /* @} */
            // Set direction of the name field
            int nameDirection = View.TEXT_DIRECTION_INHERIT;
            if (nameIsNumber) {
                nameDirection = View.TEXT_DIRECTION_LTR;
            }
            mPrimaryName.setTextDirection(nameDirection);
        }
    }

    @Override
    public void setPrimaryImage(Drawable image) {
        if (image != null) {
            setDrawableToImageView(mPhoto, image);
        }
    }

    @Override
    public void setPrimaryPhoneNumber(String number) {
        // Set the number
        if (TextUtils.isEmpty(number)) {
            mPhoneNumber.setText("");
            mPhoneNumber.setVisibility(View.GONE);
        } else {
            mPhoneNumber.setText(number);
            mPhoneNumber.setVisibility(View.VISIBLE);
            mPhoneNumber.setTextDirection(View.TEXT_DIRECTION_LTR);
        }
    }

    private void setSecondaryPhoneNumber(String phoneNumber) {
        // TODO Auto-generated method stub
        Log.d(this, "setSecondaryPhoneNumber phoneNumber is " + phoneNumber);
        if (TextUtils.isEmpty(phoneNumber)) {
            mSecondaryPhoneNumber.setText("");
            mSecondaryPhoneNumber.setVisibility(View.GONE);
        } else {
            mSecondaryPhoneNumber.setText(phoneNumber);
            mSecondaryPhoneNumber.setVisibility(View.VISIBLE);
            mSecondaryPhoneNumber.setTextDirection(View.TEXT_DIRECTION_LTR);
        }
    }

    @Override
    public void setPrimaryLabel(String label) {
        if (!TextUtils.isEmpty(label)) {
            mNumberLabel.setText(label);
            if (SprdUtils.PIKEL_UI_SUPPORT) {
                mNumberLabel.setVisibility(View.GONE);
            } else {
                mNumberLabel.setVisibility(View.VISIBLE);
            }
        } else {
            mNumberLabel.setVisibility(View.GONE);
        }

    }

    @Override
    public void setPrimary(String number, String name, boolean nameIsNumber, String label,
            Drawable photo, boolean isConference, boolean isGeneric, boolean isSipCall,
            boolean isRealNoName, String googleGeocode) {
        Log.d(this, "Setting primary call");

        if (isConference) {
            name = getConferenceString(isGeneric);
            photo = getConferencePhoto(isGeneric);
            nameIsNumber = false;
        }

        /* SPRD: Modify for bug789304 @{ */
        if (mGeocodeView != null) {
            /*if (!TextUtils.isEmpty(googleGeocode)){
                mGeocodeView.setVisibility(View.VISIBLE);
                mGeocodeView.setText(googleGeocode);
            } else */ if (GeocodeHelper.isSupportSprdGeocode()) {
                mGeocodeView.setVisibility(View.VISIBLE);
                GeocodeHelper.getGeocodeMessage(mGeocodeView, nameIsNumber ? name : number);
            }else {
                mGeocodeView.setVisibility(View.GONE);
            }
        }
        /* @} */
        /* SPRD: add for bug671746 @{ */
        if (nameIsNumber) {
             mPhoneNumber.setVisibility(View.GONE);
        }else{
            setPrimaryPhoneNumber(number);
        }
        /* @} */
        // set the name field.
        setPrimaryName(name, nameIsNumber, isRealNoName);

        // Set the label (Mobile, Work, etc)
        setPrimaryLabel(label);

        showInternetCallLabel(isSipCall);

        if (!SprdUtils.PIKEL_UI_SUPPORT) {
            setDrawableToImageView(mPhoto, photo);
        }

        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            /* SPRD: add for UniverseUI @{ */
            if((number != null && !number.equals(mCallNumber))
                    || (name != null && !name.equals(mCallName))){
                mCallNumber = number;
                mCallName = name;

                if (number != null && PhoneNumberUtils.isEmergencyNumber(number)) {
                    if(mPrimaryName != null) mPrimaryName.setText("");
                    Log.d(this, "Primary call is Emergency Call!");
                    return;
                }

                if(getActivity() != null){
                    InCallActivity activity = (InCallActivity)getActivity();
                    activity.checkNumberIsShouldBeSave(CallList.getInstance().getOutgoingCall());
                }
            }
            /* @} */
        }
    }

    @Override
    public void setSecondary(boolean show, String name, boolean nameIsNumber, String label,
            Drawable photo, boolean isConference, boolean isGeneric) {

        if (show) {
            if (isConference) {
                name = getConferenceString(isGeneric);
                photo = getConferencePhoto(isGeneric);
                nameIsNumber = false;
            }

            showAndInitializeSecondaryCallInfo();
            mSecondaryCallName.setText(name);

            int nameDirection = View.TEXT_DIRECTION_INHERIT;
            if (nameIsNumber) {
                nameDirection = View.TEXT_DIRECTION_LTR;
            }
            mSecondaryCallName.setTextDirection(nameDirection);
            if(!SprdUtils.UNIVERSE_UI_SUPPORT){
                setDrawableToImageView(mSecondaryPhoto, photo);// SPRD: add for Universe UI
            }
        } else {
            mSecondaryCallInfo.setVisibility(View.GONE);
        }
    }

    @Override
    public void setSecondary(boolean show, String number, String name, boolean nameIsNumber, String label,
            Drawable photo, boolean isConference, boolean isGeneric) {

        if (show) {
            if (isConference) {
                name = getConferenceString(isGeneric);
                nameIsNumber = false;
            }

            showAndInitializeSecondaryCallInfo();
            mSecondaryCallName.setText(name);

            int nameDirection = View.TEXT_DIRECTION_INHERIT;
            if (nameIsNumber) {
                nameDirection = View.TEXT_DIRECTION_LTR;
            }
            mSecondaryCallName.setTextDirection(nameDirection);
            setSecondaryPhoneNumber(number);
        } else {
            mSecondaryCallInfo.setVisibility(View.GONE);
        }
    }

    @Override
    public void setSecondaryImage(Drawable image) {
        if (image != null) {
            if(!SprdUtils.PIKEL_UI_SUPPORT && !SprdUtils.UNIVERSE_UI_SUPPORT){
                setDrawableToImageView(mSecondaryPhoto, image);// SPRD: add for Universe UI
            }
        }
    }

    //UNISOC:add for bug899333
    public boolean isShowAnritsuWifiFlag(Context context, int phoneId){
        PersistableBundle carrierConfig = new PersistableBundle();
        CarrierConfigManager configManager = (CarrierConfigManager) context.getSystemService(Context.CARRIER_CONFIG_SERVICE);
        if (configManager != null) {
            Log.v(this, "isShowAnritsuWifiFlag carrierConfigmanager is not null");
            carrierConfig = configManager.getConfigForPhoneId(phoneId);
            if (carrierConfig != null) {
               Log.v(this, "isShowAnritsuWifiFlag= " + carrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_ANRITSU_WIFI_FLAG));
               return carrierConfig.getBoolean(CarrierConfigManager.KEY_SHOW_ANRITSU_WIFI_FLAG);
            }
        }
        return false;
    }

    /**
     * SPRD:
     * @orig setCallState(int, Call.DisconnectCause, boolean,String, String)
     *
     */
    @Override
    public void setCallState(int phoneId, int state, Call.DisconnectCause cause, boolean bluetoothOn,
            String gatewayLabel, String gatewayNumber) {
        String callStateLabel = null;
        /* SPRD bug 301615 add sim name color start */
        final Context context = getView().getContext();
        String callSimNameLabel = null;
        int SimColor = R.color.incall_call_button_text_color_sprd;
        /* SPRD bug 301615 add sim name color end */

        // States other than disconnected not yet supported
        callStateLabel = SprdUtils.PIKEL_UI_SUPPORT ? getCallStateLabelFromStatePikel(phoneId, state, cause)
                : getCallStateLabelFromState(phoneId, state, cause);
        /* SPRD bug 301615 add sim name color start */
        // SPRD bug 723087 wificalling icon
        if (getPresenter().isWifiCall()){
            mWifiCallView.setVisibility(View.VISIBLE);
            if(isShowAnritsuWifiFlag(context, phoneId)){ //UNISOC:add for bug899333
               callSimNameLabel = context.getResources().getString(R.string.anritsu_wifi_call);
            } else {
               callSimNameLabel = context.getResources().getString(R.string.wifi_call);
            }
        }
        else{
            callSimNameLabel = getCallSimNameLabel(phoneId);
            mWifiCallView.setVisibility(View.GONE);
        }
        if(mSimManager != null){
            SimColor = mSimManager.getColor(phoneId);
        }
        /* SPRD bug 301615 add sim name color end */

        Log.v(this, "phoneId " + phoneId);
        Log.v(this, "setCallState " + callStateLabel);
        Log.v(this, "DisconnectCause " + cause);
        Log.v(this, "bluetooth on " + bluetoothOn);
        Log.v(this, "gateway " + gatewayLabel + gatewayNumber);
        Log.v(this, "callSimNameLabel " + callSimNameLabel);
        // There are cases where we totally skip the animation, in which case remove the transition
        // animation here and restore it afterwards.
        final boolean skipAnimation = (Call.State.isDialing(state)
                || state == Call.State.DISCONNECTED || state == Call.State.DISCONNECTING);
        LayoutTransition transition = null;
        if (skipAnimation) {
            transition = mSupplementaryInfoContainer.getLayoutTransition();
            mSupplementaryInfoContainer.setLayoutTransition(null);
        }

        if (SprdUtils.PIKEL_UI_SUPPORT){
               if (!TextUtils.isEmpty(callSimNameLabel) && (Call.State.isDialing(state)
                        || (Call.State.INCOMING == state)
                        || (Call.State.CALL_WAITING == state)
                        || (Call.State.ACTIVE == state)
                        || (Call.State.ONHOLD == state))){
                   mCallSimLabel.setVisibility(View.VISIBLE);
                   mCallSimLabel.setText(callSimNameLabel);
            } else {
                mCallSimLabel.setVisibility(View.GONE);
            }
        } else

        /* SPRD bug 301615 add sim name color start */
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            if(!TextUtils.isEmpty(callSimNameLabel)
                    && (Call.State.isDialing(state)
                            || (Call.State.INCOMING == state)
                            || (Call.State.CALL_WAITING == state)
                            || (Call.State.ACTIVE == state)
                            || (Call.State.ONHOLD == state))){
                mCallSimLabel.setVisibility(View.VISIBLE);
                mCallSimLabel.setText(callSimNameLabel);
                mCallSimLabel.setTextColor(SimColor);

            } else{
                mCallSimLabel.setVisibility(View.GONE);
            }

        }
        /* SPRD bug 301615 add sim name color end */
        /* SPRD: add for HIGH_DEF_AUDIO @{ */
        if ("cucc".equals(SystemProperties.get("ro.operator"))
                && Call.State.ACTIVE == state && getPresenter().getPrimary().can(Capabilities.HIGH_DEF_AUDIO)) {
            HdVoiceView.setVisibility(View.VISIBLE);
            Log.d(this, "sendStickyBroadcastAsUser ACTION_HIGH_DEF_AUDIO_SUPPORT true");
            Intent intent = new Intent(TelephonyIntents.ACTION_HIGH_DEF_AUDIO_SUPPORT);
            intent.putExtra("isHdVoiceSupport", true);
            getView().getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
        } else {
            Log.d(this, "sendStickyBroadcastAsUser ACTION_HIGH_DEF_AUDIO_SUPPORT false");
            Intent intent = new Intent(TelephonyIntents.ACTION_HIGH_DEF_AUDIO_SUPPORT);
            intent.putExtra("isHdVoiceSupport", false);
            getView().getContext().sendStickyBroadcastAsUser(intent, UserHandle.ALL);
            HdVoiceView.setVisibility(View.GONE);
        }
        /* @} */
        // Update the call state label.
        if (!TextUtils.isEmpty(callStateLabel)) {
            if (SprdUtils.UNIVERSE_UI_SUPPORT && mElapsedTime.getVisibility() == View.VISIBLE) {
                mElapsedTime.setVisibility(View.GONE);
            }
            mCallStateLabel.setVisibility(View.VISIBLE);
            mCallStateLabel.setText(callStateLabel);

            if (Call.State.INCOMING == state) {
                setBluetoothOn(bluetoothOn);
            }
        } else {
            /* SPRD bug 356234 normalize layout START*/
            mCallStateLabel.setVisibility(/*View.GONE*/View.INVISIBLE);
            /* SPRD bug 356234 normalize layout END*/

            // Gravity is aligned left when receiving an incoming call in landscape.
            // In that rare case, the gravity needs to be reset to the right.
            // Also, setText("") is used since there is a delay in making the view GONE,
            // so the user will otherwise see the text jump to the right side before disappearing.
            if(!SprdUtils.PIKEL_UI_SUPPORT && mCallStateLabel.getGravity() != Gravity.END && !SprdUtils.UNIVERSE_UI_SUPPORT) {// SPRD: add for Universe UI
                mCallStateLabel.setText("");
                mCallStateLabel.setGravity(Gravity.END);
            }
        }

        // Provider info: (e.g. "Calling via <gatewayLabel>")
        if (!TextUtils.isEmpty(gatewayLabel) && !TextUtils.isEmpty(gatewayNumber)) {
            /* SPRD: modify for layout optimization @ {
             * @Orig:
            mProviderInfo.setVisibility(View.VISIBLE);
            mProviderLabel.setText(gatewayLabel);
            mProviderNumber.setText(gatewayNumber);
             */
            if(mProviderInfoStub != null && mProviderInfoView == null){
                mProviderInfoView = mProviderInfoStub.inflate();
                mProviderInfo = mProviderInfoView.findViewById(R.id.providerInfo);
                mProviderLabel = (TextView) mProviderInfoView.findViewById(R.id.providerLabel);
                mProviderNumber = (TextView) mProviderInfoView.findViewById(R.id.providerAddress);
            }
            if(mProviderInfo != null){
                mProviderInfo.setVisibility(View.VISIBLE);
            }
            if(mProviderLabel != null){
                mProviderLabel.setText(gatewayLabel);
            }
            if(mProviderNumber != null){
                mProviderNumber.setText(gatewayNumber);
            }
            /* @} */

        } else {
            /* SPRD: modify for layout optimization @ {
             * @Orig:
            mProviderInfo.setVisibility(View.GONE);
             */
            if(mProviderInfo != null){
                mProviderInfo.setVisibility(View.GONE);
            }
            /* @} */
        }

        // Restore the animation.
        if (skipAnimation) {
            mSupplementaryInfoContainer.setLayoutTransition(transition);
        }
    }

    private void showInternetCallLabel(boolean show) {
        if (show) {
            final String label = getView().getContext().getString(
                    R.string.incall_call_type_label_sip);
            mCallTypeLabel.setVisibility(View.VISIBLE);
            mCallTypeLabel.setText(label);
        } else {
            mCallTypeLabel.setVisibility(View.GONE);
        }
    }

    @Override
    public void setPrimaryCallElapsedTime(boolean show, String callTimeElapsed) {
        if (show) {
            if (mElapsedTime.getVisibility() != View.VISIBLE) {
                AnimationUtils.Fade.show(mElapsedTime);
            }
            /* SPRD: add for bug267376 @ { */
            Log.d(this, "callTimeElapsed: " + callTimeElapsed);
            /* @} */
            mElapsedTime.setText(callTimeElapsed);
            if(SprdUtils.UNIVERSE_UI_SUPPORT && callTimeElapsed != null){
                mCallTime = callTimeElapsed;//SPRD:add for Universe UI
            }
        } else {
            // hide() animation has no effect if it is already hidden.
            // SPRD: add for Universe UI & PIKEL UI
            if (SprdUtils.UNIVERSE_UI_SUPPORT || SprdUtils.PIKEL_UI_SUPPORT) {
                AnimationUtils.Fade.hide(mElapsedTime, View.GONE);
            } else {
                AnimationUtils.Fade.hide(mElapsedTime, View.INVISIBLE);
            }
        }
    }

    private void setDrawableToImageView(ImageView view, Drawable photo) {
        if (photo == null) {
            int photoResId;
            if(SprdUtils.UNIVERSE_UI_SUPPORT){
                photoResId = R.drawable.picture_unknown_sprd;// SPRD: add for Universe UI
            } else {
                photoResId = R.drawable.picture_unknown;
            }
            photo = view.getResources().getDrawable(photoResId);
        }

        final Drawable current = view.getDrawable();
        if (current == null) {
            view.setImageDrawable(photo);
            AnimationUtils.Fade.show(view);
        } else {
            AnimationUtils.startCrossFade(view, current, photo);
            view.setVisibility(View.VISIBLE);
        }
    }

    private String getConferenceString(boolean isGeneric) {
        Log.v(this, "isGenericString: " + isGeneric);
        final int resId = isGeneric ? R.string.card_title_in_call : R.string.card_title_conf_call;
        return getView().getResources().getString(resId);
    }

    private Drawable getConferencePhoto(boolean isGeneric) {
        Log.v(this, "isGenericPhoto: " + isGeneric);
        final int resId;
        // SPRD: add for Universe UI
        if(SprdUtils.UNIVERSE_UI_SUPPORT){
            resId = isGeneric ? R.drawable.picture_dialing : R.drawable.picture_conference_sprd;
        } else {
            resId = isGeneric ? R.drawable.picture_dialing : R.drawable.picture_conference;
        }
        return getView().getResources().getDrawable(resId);
    }

    private void setBluetoothOn(boolean onOff) {
        // Also, display a special icon (alongside the "Incoming call"
        // label) if there's an incoming call and audio will be routed
        // to bluetooth when you answer it.
        final int bluetoothIconId = R.drawable.ic_in_call_bt_dk;

        if (onOff) {
            mCallStateLabel.setCompoundDrawablesWithIntrinsicBounds(bluetoothIconId, 0, 0, 0);
            mCallStateLabel.setCompoundDrawablePadding((int) (mDensity * 5));
        } else {
            // Clear out any icons
            mCallStateLabel.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        }
    }

    /**
     * Gets the call state label based on the state of the call and
     * cause of disconnect
     *
     * SPRD: modify
     * @orig getCallStateLabelFromState(int, Call.DisconnectCause)
     */
    private String getCallStateLabelFromStatePikel(int phoneId, int state, Call.DisconnectCause cause) {
        final Context context = getView().getContext();
        String callStateLabel = null;  // Label to display as part of the call banne

        if (Call.State.IDLE == state) {
            // "Call state" is meaningless in this state.

        } else if (Call.State.ACTIVE == state) {
            // We normally don't show a "call state label" at all in
            // this state (but see below for some special cases).
            callStateLabel = context.getString(R.string.card_title_in_call);
        } else if (Call.State.ONHOLD == state) {
            callStateLabel = context.getString(R.string.card_title_on_hold);
        } else if (Call.State.DIALING == state) {
            callStateLabel = context.getString(R.string.card_title_dialing);
        } else if (Call.State.REDIALING == state) {
            callStateLabel = context.getString(R.string.card_title_redialing);
        } else if (Call.State.INCOMING == state || Call.State.CALL_WAITING == state) {
            callStateLabel = context.getString(R.string.card_title_incoming_call);
        } else if (Call.State.DISCONNECTING == state) {
            // While in the DISCONNECTING state we display a "Hanging up"
            // message in order to make the UI feel more responsive.  (In
            // GSM it's normal to see a delay of a couple of seconds while
            // negotiating the disconnect with the network, so the "Hanging
            // up" state at least lets the user know that we're doing
            // something.  This state is currently not used with CDMA.)
            callStateLabel = context.getString(R.string.card_title_hanging_up);

        } else if (Call.State.DISCONNECTED == state) {
            callStateLabel = getCallFailedString(cause);
            /* SPRD: Support telcel requirements in InCallUI. @{ */
            if (getActivity() != null && InCallUITelcelHelper.getInstance(getActivity())
                    .isVoiceClearCodeLabel(getActivity(), callStateLabel.toString())) {
                callStateLabel = context.getString(R.string.card_title_call_ended);
            }
            /* @} */
        } else {
            Log.wtf(this, "updateCallStateWidgets: unexpected call: " + state);
        }

        return callStateLabel;
    }

    /**
     * Gets the call state label based on the state of the call and
     * cause of disconnect
     *
     * SPRD: modify
     * @orig getCallStateLabelFromState(int, Call.DisconnectCause)
     */
    private String getCallStateLabelFromState(int phoneId, int state, Call.DisconnectCause cause) {
        final Context context = getView().getContext();
        String callStateLabel = null;  // Label to display as part of the call banner
        /* SPRD: add message to show which sim is active @{ */
        String sim_msg = "";
        if(!SprdUtils.UNIVERSE_UI_SUPPORT){
            /*    callStateLabel = "";
            if(mSimManager != null){
                Sim sim = mSimManager.getSimById(phoneId);
                if(sim != null && phoneId != CallCardPresenter.INVALID_PHONE_ID){
                    sim_msg = "sim:" +(sim.getName()+" ");
                } else {
                    sim_msg = "";
                }
            } else {
                sim_msg = "";
            }
        } else {*/
            sim_msg = phoneId == CallCardPresenter.INVALID_PHONE_ID ? "" : context.getString(
                    R.string.sim_reference, phoneId + 1);
        }
        /* @} */

        if (Call.State.IDLE == state) {
            // "Call state" is meaningless in this state.

        } else if (Call.State.ACTIVE == state) {
            // We normally don't show a "call state label" at all in
            // this state (but see below for some special cases).
            /** SPRD: add sim reference */
            callStateLabel = sim_msg;
        } else if (Call.State.ONHOLD == state) {
            callStateLabel = context.getString(R.string.card_title_on_hold);
        } else if (Call.State.DIALING == state) {
            /** SPRD: add sim reference */
            callStateLabel = sim_msg + context.getString(R.string.card_title_dialing);
        } else if (Call.State.REDIALING == state) {
            /** SPRD: add sim reference */
            callStateLabel = sim_msg + context.getString(R.string.card_title_redialing);
        } else if (Call.State.INCOMING == state || Call.State.CALL_WAITING == state) {
            /** SPRD: add sim reference */
            callStateLabel = sim_msg + context.getString(R.string.card_title_incoming_call);
        } else if (Call.State.DISCONNECTING == state) {
            // While in the DISCONNECTING state we display a "Hanging up"
            // message in order to make the UI feel more responsive.  (In
            // GSM it's normal to see a delay of a couple of seconds while
            // negotiating the disconnect with the network, so the "Hanging
            // up" state at least lets the user know that we're doing
            // something.  This state is currently not used with CDMA.)
            callStateLabel = context.getString(R.string.card_title_hanging_up);

        } else if (Call.State.DISCONNECTED == state) {
            callStateLabel = getCallFailedString(cause);

        } else {
            Log.wtf(this, "updateCallStateWidgets: unexpected call: " + state);
        }

        return callStateLabel;
    }
    /* SPRD: bug 301615 add sim name color end */
    private String getCallSimNameLabel(int phoneId) {
        String sim_msg = "";
        if(SprdUtils.PIKEL_UI_SUPPORT || SprdUtils.UNIVERSE_UI_SUPPORT){
            if(mSimManager != null){
                Sim sim = mSimManager.getSimById(phoneId);
                if(sim != null && phoneId != CallCardPresenter.INVALID_PHONE_ID){
                    if(TelephonyManager.getPhoneCount() > 1){
                        int supportMainSolt = Settings.Secure.getInt(this.getView().getContext().getContentResolver(), Settings.Secure.SERVICE_PRIMARY_CARD,-1);
                        if (supportMainSolt == phoneId) {
                            sim_msg = this.getString(R.string.main_card_slot)
                                    + sim.getName();
                        } else {
                            sim_msg = this.getString(R.string.gsm_card_slot)
                                    + sim.getName();
                        }
                    } else {
                        sim_msg = sim.getName();
                    }
                } else {
                    sim_msg = "";
                }
            } else {
                sim_msg = "";
            }
        }
        return sim_msg;
    }
    /* SPRD: bug 301615 add sim name color end */
    /**
     * Maps the disconnect cause to a resource string.
     */
    private String getCallFailedString(Call.DisconnectCause cause) {
        int resID = R.string.card_title_call_ended;

        // TODO: The card *title* should probably be "Call ended" in all
        // cases, but if the DisconnectCause was an error condition we should
        // probably also display the specific failure reason somewhere...

        switch (cause) {
            case BUSY:
                resID = R.string.callFailed_userBusy;
                break;

            case CONGESTION:
                resID = R.string.callFailed_congestion;
                break;

            case TIMED_OUT:
                resID = R.string.callFailed_timedOut;
                break;

            case SERVER_UNREACHABLE:
                resID = R.string.callFailed_server_unreachable;
                break;

            case NUMBER_UNREACHABLE:
                resID = R.string.callFailed_number_unreachable;
                break;

            case INVALID_CREDENTIALS:
                resID = R.string.callFailed_invalid_credentials;
                break;

            case SERVER_ERROR:
                resID = R.string.callFailed_server_error;
                break;

            case OUT_OF_NETWORK:
                resID = R.string.callFailed_out_of_network;
                break;

            case LOST_SIGNAL:
            case CDMA_DROP:
                resID = R.string.callFailed_noSignal;
                break;

            case LIMIT_EXCEEDED:
                resID = R.string.callFailed_limitExceeded;
                break;

            case POWER_OFF:
                resID = R.string.callFailed_powerOff;
                break;

            case ICC_ERROR:
                resID = R.string.callFailed_simError;
                break;

            case OUT_OF_SERVICE:
                resID = R.string.callFailed_outOfService;
                break;

            case INVALID_NUMBER:
            case UNOBTAINABLE_NUMBER:
                resID = R.string.callFailed_unobtainable_number;
                break;

            default:
                resID = R.string.card_title_call_ended;
                break;
        }
        return this.getView().getContext().getString(resID);
    }

    private void showAndInitializeSecondaryCallInfo() {
        mSecondaryCallInfo.setVisibility(View.VISIBLE);

        // mSecondaryCallName is initialized here (vs. onViewCreated) because it is inaccesible
        // until mSecondaryCallInfo is inflated in the call above.
        if (mSecondaryCallName == null) {
            mSecondaryCallName = (TextView) getView().findViewById(R.id.secondaryCallName);
            if (SprdUtils.PIKEL_UI_SUPPORT) {
                mSecondaryPhoneNumber = (TextView) getView().findViewById(R.id.secondaryPhoneNumber);
            }
        }
        if (!SprdUtils.PIKEL_UI_SUPPORT && mSecondaryPhoto == null && !SprdUtils.UNIVERSE_UI_SUPPORT) { //SPRD: Modify for Universe UI
            mSecondaryPhoto = (ImageView) getView().findViewById(R.id.secondaryCallPhoto);
        }

        if (!SprdUtils.PIKEL_UI_SUPPORT && mSecondaryPhotoOverlay == null && !SprdUtils.UNIVERSE_UI_SUPPORT) { // SPRD: Modify for Universe UI
            mSecondaryPhotoOverlay = getView().findViewById(R.id.dim_effect_for_secondary_photo);
            mSecondaryPhotoOverlay.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    getPresenter().secondaryPhotoClicked();
                }
            });
            mSecondaryPhotoOverlay.setOnTouchListener(new SmallerHitTargetTouchListener());
        }
    }

    public void dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            dispatchPopulateAccessibilityEvent(event, mPrimaryName);
            dispatchPopulateAccessibilityEvent(event, mPhoneNumber);
            return;
        }
        dispatchPopulateAccessibilityEvent(event, mCallStateLabel);
        dispatchPopulateAccessibilityEvent(event, mPrimaryName);
        dispatchPopulateAccessibilityEvent(event, mPhoneNumber);
        dispatchPopulateAccessibilityEvent(event, mCallTypeLabel);
        dispatchPopulateAccessibilityEvent(event, mSecondaryCallName);

        return;
    }

    private void dispatchPopulateAccessibilityEvent(AccessibilityEvent event, View view) {
        if (view == null) return;
        final List<CharSequence> eventText = event.getText();
        int size = eventText.size();
        view.dispatchPopulateAccessibilityEvent(event);
        // if no text added write null to keep relative position
        if (size == eventText.size()) {
            eventText.add(null);
        }
    }

    /* SPRD: Add for record */
    public void setRecordText(String str){
        mRecordingLabel.setText(str);
    }
    /* SPRD: Add for record */
    public TextView getRecordingLabel(){
        return mRecordingLabel;
    }
    /* SPRD: Add for record */
    public void setRecordingVisibility(int visibility){
        mRecordingLabel.setVisibility(visibility);
        if (SprdUtils.PIKEL_UI_SUPPORT) {
            mRecordingIcon.setVisibility(visibility);
            mRecordtext.setVisibility(visibility);
        }
    }

    /**
     * SPRD:
     * add for Universe UI
     * @{
     */
    String mCallNumber = null;
    String mCallName = null;
    String mCallTime = null;
    public String getCallNumber(){
        return mCallNumber;
    }
    public String getCallName(){
        return mCallName;
    }
    public String getCallTime(){
        return mCallTime;
    }
    public void clearCallParameters(){
        mCallNumber = null;
        mCallName = null;
        mCallTime = null;
    }
    /* @} */

    public float getSpaceBesideCallCard() {
        if (SprdUtils.PIKEL_UI_SUPPORT) {
            return getView().getHeight() - mPrimaryCallCardInfoContainer.getHeight();
        } else {
            return mPhoto.getHeight();
        }
    }

    @Override
    public void displayPhoneView(boolean show) {
        mPhoto.setVisibility(show ? View.VISIBLE : View.GONE);
    }

}
