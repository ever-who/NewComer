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

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.text.format.DateUtils;

import com.android.incallui.AudioModeProvider.AudioModeListener;
import com.android.incallui.ContactInfoCache.ContactCacheEntry;
import com.android.incallui.ContactInfoCache.ContactInfoCacheCallback;
import com.android.incallui.InCallPresenter.InCallState;
import com.android.incallui.InCallPresenter.InCallStateListener;
import com.android.incallui.InCallPresenter.IncomingCallListener;
import com.android.services.telephony.common.AudioMode;
import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.Call.Capabilities;
import com.android.services.telephony.common.CallIdentification;
import com.google.common.base.Preconditions;
import com.sprd.incallui.SprdUtils;
import com.sprd.incallui.geocode.GeocodeHelper;

/**
 * Presenter for the Call Card Fragment.
 * <p>
 * This class listens for changes to InCallState and passes it along to the fragment.
 */
public class CallCardPresenter extends Presenter<CallCardPresenter.CallCardUi>
        implements InCallStateListener, AudioModeListener, IncomingCallListener {

    private static final String TAG = CallCardPresenter.class.getSimpleName();
    private static final long CALL_TIME_UPDATE_INTERVAL = 1000; // in milliseconds

    /* SPRD: add for dsds @{ */
    public static final int INVALID_PHONE_ID = -1;

    private Call mPrimary;
    private Call mSecondary;
    private ContactCacheEntry mPrimaryContactInfo;
    private ContactCacheEntry mSecondaryContactInfo;
    private CallTimer mCallTimer;
    private Context mContext;

    public CallCardPresenter() {
        // create the call timer
        mCallTimer = new CallTimer(new Runnable() {
            @Override
            public void run() {
                updateCallTime();
            }
        });
    }


    public void init(Context context, Call call) {
        mContext = Preconditions.checkNotNull(context);

        // Call may be null if disconnect happened already.
        if (call != null) {
            mPrimary = call;
            // UNISOC: add for bug917296
            boolean isConference = isConference(call);

            if (!isConference) {

                CallIdentification identification = call.getIdentification();
                if (call.getChildCallIds() != null && call.getChildCallIds().size() == 1) {
                    Call child = CallList.getInstance().getCall(call.getChildCallIds().first());
                    if (child != null) {
                        identification = child.getIdentification();
                    }
                }
                // start processing lookups right away.
                startContactInfoSearch(identification, true,
                        call.getState() == Call.State.INCOMING);
            } else {
                updateContactEntry(null, true, true);
            }
        }
    }

    @Override
    public void onUiReady(CallCardUi ui) {
        super.onUiReady(ui);

        AudioModeProvider.getInstance().addListener(this);

        // Contact search may have completed before ui is ready.
        if (mPrimaryContactInfo != null) {
            updatePrimaryDisplayInfo(mPrimaryContactInfo, isConference(mPrimary));
        }

        // Register for call state changes last
        InCallPresenter.getInstance().addListener(this);
        InCallPresenter.getInstance().addIncomingCallListener(this);
    }

    @Override
    public void onUiUnready(CallCardUi ui) {
        super.onUiUnready(ui);
        /* SPRD: add for CMCC case @{ */
        mCallTimer.cancel();
        ui.setPrimaryCallElapsedTime(false, null);
        /* @} */
        // stop getting call state changes
        InCallPresenter.getInstance().removeListener(this);
        InCallPresenter.getInstance().removeIncomingCallListener(this);

        AudioModeProvider.getInstance().removeListener(this);

        mPrimary = null;
        mPrimaryContactInfo = null;
        mSecondaryContactInfo = null;
    }

    @Override
    public void onIncomingCall(InCallState state, Call call) {
        // same logic should happen as with onStateChange()
        onStateChange(state, CallList.getInstance());
    }

    @Override
    public void onStateChange(InCallState state, CallList callList) {
        Log.d(this, "onStateChange() " + state);
        final CallCardUi ui = getUi();
        if (ui == null) {
            return;
        }

        Call primary = null;
        Call secondary = null;

        if (state == InCallState.INCOMING) {
            primary = callList.getIncomingCall();
        } else if (state == InCallState.OUTGOING || state == InCallState.PREPARE_UI) {//SPRD:ADD PREPARE_UI
            primary = callList.getOutgoingCall();

            // getCallToDisplay doesn't go through outgoing or incoming calls. It will return the
            // highest priority call to display as the secondary call.
            secondary = getCallToDisplay(callList, null, true);
        } else if (state == InCallState.INCALL) {
            primary = getCallToDisplay(callList, null, false);
            secondary = getCallToDisplay(callList, primary, true);
        }

        Log.i(TAG, "Primary call: " + primary);
        Log.i(TAG, "Secondary call: " + secondary);

        // UNISOC: add for bug917296
        int primaryState = (mPrimary == null ? 0 : mPrimary.getState());
        int secondaryState = (mSecondary == null ? 0 : mSecondary.getState());
        if (primary != null) {
            primaryState = primary.getState();
        }
        if (secondary != null) {
            secondaryState = secondary.getState();
        }
        if (primary != null && primary.getChildCallIds() != null && primary.getChildCallIds().size() == 1) {               
            primary = CallList.getInstance().getCall(primary.getChildCallIds().first());
            Log.i(TAG, "new Primary call: " + primary);
        }
        
        if (secondary != null && secondary.getChildCallIds() != null && secondary.getChildCallIds().size() == 1) {
            secondary = CallList.getInstance().getCall(secondary.getChildCallIds().first());
            Log.i(TAG, "new Secondary call: " + secondary);
        }
        final boolean primaryChanged = !areCallsSame(mPrimary, primary);
        final boolean secondaryChanged = !areCallsSame(mSecondary, secondary);
        mSecondary = secondary;
        Call previousPrimary = mPrimary;    // SPRD: fix bug649585
        mPrimary = primary;

        if (mPrimary != null && (primaryChanged
                || (isConference(previousPrimary) != isConference(mPrimary)))) {          
            // primary call has changed
            mPrimaryContactInfo = ContactInfoCache.buildCacheEntryFromCall(mContext,
                    mPrimary.getIdentification(), mPrimary.getState() == Call.State.INCOMING);
            updatePrimaryDisplayInfo(mPrimaryContactInfo, isConference(mPrimary));
            /* SPRD: add for Bug245660 Bug658723@{ */
            ContactCacheEntry primaryCache = ContactInfoCache.getInstance(mContext).getInfo(mPrimary.getCallId());
            if(primaryCache != null && !TextUtils.isEmpty(mPrimary.getNumber())
                    && !TextUtils.equals(primaryCache.number, mPrimary.getNumber())
                    && !PhoneNumberUtils.isVoiceMailNumber(mPrimary.getPhoneId(), mPrimary.getNumber())){
                primaryCache.number = mPrimary.getNumber();
            }
            /* @} */
            maybeStartSearch(mPrimary, true);
        }

        if (mSecondary == null) {
            // Secondary call may have ended.  Update the ui.
            mSecondaryContactInfo = null;
            updateSecondaryDisplayInfo(false);
        } else if (secondaryChanged) {
            // secondary call has changed
            mSecondaryContactInfo = ContactInfoCache.buildCacheEntryFromCall(mContext,
                    mSecondary.getIdentification(), mSecondary.getState() == Call.State.INCOMING);
            updateSecondaryDisplayInfo(isConference(mSecondary));
            maybeStartSearch(mSecondary, false);
        }

        // Start/Stop the call time update timer
        if (mPrimary != null && (primaryState == Call.State.ACTIVE
                || primaryState == Call.State.ONHOLD)) {
            Log.d(this, "Starting the calltime timer");
            mCallTimer.start(CALL_TIME_UPDATE_INTERVAL);
        } else {
            Log.d(this, "Canceling the calltime timer");
            mCallTimer.cancel();
            /* SPRD: modify for CMCC case,
             * Original Android code: @{
                ui.setPrimaryCallElapsedTime(false, null);*/
            if (mPrimary != null && primaryState != Call.State.DISCONNECTING
                    && primaryState != Call.State.DISCONNECTED) {
                ui.setPrimaryCallElapsedTime(false, null);
            }
            /* @} */
        }

        // Set the call state
        if (mPrimary != null) {
            final boolean bluetoothOn =
                    (AudioModeProvider.getInstance().getAudioMode() == AudioMode.BLUETOOTH);
                ui.setCallState(mPrimary.getPhoneId(), primaryState, mPrimary.getDisconnectCause(),
                        bluetoothOn, getGatewayLabel(), getGatewayNumber());

            Log.d(this, "mPrimary isVideo call : " + mPrimary.isVideo());
            /* SPRD: set photo display or hide for bug844162 ＆ bug 848106 */
            if(getUi() != null) {
                if(mPrimary.isVideo() || (callList.getActiveCall() != null && callList.getActiveCall().isVideo() && !mPrimary.isVideo())) {
                    getUi().displayPhoneView(false);
                } else {
                    getUi().displayPhoneView(true);
                }
            }
        } else {
            ui.setCallState(INVALID_PHONE_ID, Call.State.IDLE, Call.DisconnectCause.UNKNOWN, false, null, null);
        }
    }

    @Override
    public void onAudioMode(int mode) {
        if (mPrimary != null && getUi() != null) {
            final boolean bluetoothOn = (AudioMode.BLUETOOTH == mode);

/*            getUi().setCallState(mPrimary.getState(), mPrimary.getDisconnectCause(), bluetoothOn,
                    getGatewayLabel(), getGatewayNumber());*/
            getUi().setCallState(mPrimary.getPhoneId(), mPrimary.getState(),
                    mPrimary.getDisconnectCause(), bluetoothOn, getGatewayLabel(),
                    getGatewayNumber());
        }
    }

    @Override
    public void onSupportedAudioMode(int mask) {
    }

    @Override
    public void onMute(boolean muted) {
    }

    public void updateCallTime() {
        final CallCardUi ui = getUi();

        if (ui == null || mPrimary == null || (mPrimary.getState() != Call.State.ACTIVE
                && mPrimary.getState() != Call.State.ONHOLD)) {
            /* SPRD: modify for CMCC case,
             * Original Android code: @{
            if (ui != null) {
                ui.setPrimaryCallElapsedTime(false, null);
            }
             @} */
            mCallTimer.cancel();
        } else {
            /* SPRD: modify for Bug 276943
             * @Orig:
             * final long callStart = mPrimary.getConnectTime();
             * final long duration = System.currentTimeMillis() - callStart;
             * @ { */
            final long callStart = mPrimary.getConnectRealTime();
            final long duration = SystemClock.elapsedRealtime() - callStart;
            /* @} */
            long callTime = callStart == 0 ? callStart : (duration / 1000);
            /* SPRD: add for bug267376 @ { */
            Log.d(this, "callTime = "+callTime);
            /* @} */
            ui.setPrimaryCallElapsedTime(true, DateUtils.formatElapsedTime(callTime));
        }
    }

    private boolean areCallsSame(Call call1, Call call2) {
        if (call1 == null && call2 == null) {
            return true;
        } else if (call1 == null || call2 == null) {
            return false;
        }

        // otherwise compare call Ids
        /* SPRD: add for Bug245660
         * @Orig:return call1.getCallId() == call2.getCallId();
         *  @{ */
        boolean numberNotChanged = TextUtils.equals(call1.getNumber(), call2.getNumber());
        return call1.getCallId() == call2.getCallId() && numberNotChanged;
        /* @} */
    }

    private void maybeStartSearch(Call call, boolean isPrimary) {
        // no need to start search for conference calls which show generic info.
        if (call != null && !isConference(call)) {
            // UNISOC: add for bug917296
            CallIdentification identification = call.getIdentification();
            if (call.getChildCallIds() != null && call.getChildCallIds().size() == 1) {
                Call child = CallList.getInstance().getCall(call.getChildCallIds().first());
                if (child != null) {
                    identification = child.getIdentification();
                }
            }
            startContactInfoSearch(identification, isPrimary,
                    call.getState() == Call.State.INCOMING);
        }
    }

    /**
     * Starts a query for more contact data for the save primary and secondary calls.
     */
    private void startContactInfoSearch(final CallIdentification identification,
            final boolean isPrimary, boolean isIncoming) {
        final ContactInfoCache cache = ContactInfoCache.getInstance(mContext);

        cache.findInfo(identification, isIncoming, new ContactInfoCacheCallback() {
                @Override
                public void onContactInfoComplete(int callId, ContactCacheEntry entry) {
                    //SPRD:add for bug723321
                    Call call = isPrimary ? mPrimary : mSecondary;
                    if (call != null && call.getIdentification().getCallId() == callId) {
                        Log.d(TAG, "onContactInfoComplete call:" +call);
                        updateContactEntry(entry, isPrimary, false);
                    }

                    if (entry.name != null) {
                        Log.d(TAG, "Contact found: " + entry);
                    }
                    if (entry.personUri != null) {
                        CallerInfoUtils.sendViewNotification(mContext, entry.personUri);
                    }
                }

                @Override
                public void onImageLoadComplete(int callId, ContactCacheEntry entry) {
                    if (getUi() == null) {
                        return;
                    }
                    if (!SprdUtils.PIKEL_UI_SUPPORT && entry.photo != null) {
                        if (mPrimary != null && callId == mPrimary.getCallId()) {
                             getUi().setPrimaryImage(entry.photo);
                        } else if (mSecondary != null && callId == mSecondary.getCallId()) {
                            getUi().setSecondaryImage(entry.photo);
                        }
                    }
                }
            });
    }

    private static boolean isConference(Call call) {
        return call != null && (call.isConferenceCall()
                || call.can(Capabilities.CAPABILITY_IMS_CONFERENCE)) // SPRD: modify for bug649585
                && call.getChildCallIds() != null && call.getChildCallIds().size() > 1
                && !call.can(Capabilities.GENERIC_CONFERENCE); // SPRD: modify for bug885496
    }

    private static boolean isGenericConference(Call call) {
        return call != null && call.can(Capabilities.GENERIC_CONFERENCE);
    }

    private void updateContactEntry(ContactCacheEntry entry, boolean isPrimary,
            boolean isConference) {
        if (isPrimary) {
            mPrimaryContactInfo = entry;
            updatePrimaryDisplayInfo(entry, isConference);
        } else {
            mSecondaryContactInfo = entry;
            updateSecondaryDisplayInfo(isConference);
        }
    }

    /**
     * Get the highest priority call to display.
     * Goes through the calls and chooses which to return based on priority of which type of call
     * to display to the user. Callers can use the "ignore" feature to get the second best call
     * by passing a previously found primary call as ignore.
     *
     * @param ignore A call to ignore if found.
     */
    private Call getCallToDisplay(CallList callList, Call ignore, boolean skipDisconnected) {

        // Active calls come second.  An active call always gets precedent.
        Call retval = callList.getActiveCall();
        if (retval != null && retval != ignore) {
            return retval;
        }

        // Disconnected calls get primary position if there are no active calls
        // to let user know quickly what call has disconnected. Disconnected
        // calls are very short lived.
        if (!skipDisconnected) {
            retval = callList.getDisconnectingCall();
            if (retval != null && retval != ignore) {
                return retval;
            }
            retval = callList.getDisconnectedCall();
            if (retval != null && retval != ignore) {
                return retval;
            }
        }

        // Then we go to background call (calls on hold)
        retval = callList.getBackgroundCall();
        if (retval != null && retval != ignore) {
            return retval;
        }

        // Lastly, we go to a second background call.
        retval = callList.getSecondBackgroundCall();

        return retval;
    }

    private void updatePrimaryDisplayInfo(ContactCacheEntry entry, boolean isConference) {
        Log.d(TAG, "Update primary display " + entry);
        final CallCardUi ui = getUi();
        if (ui == null || mPrimary == null) {
            // TODO: May also occur if search result comes back after ui is destroyed. Look into
            // removing that case completely.
            Log.d(TAG, "updatePrimaryDisplayInfo called but ui or primary call is null!");
            return;
        }

        final boolean isGenericConf = isGenericConference(mPrimary);
        if (entry != null) {
            final String name = getNameForCall(entry);
            final String number = isConference ? mPrimary.getNumber() : getNumberForCall(entry);
            final String googleGeocode = getGeocodeForCall(entry);
            final boolean nameIsNumber = name != null && name.equals(entry.number);
            Log.d(TAG, "updatePrimaryDisplayInfo name = " + name + " number = " + number + " googleGeocode = " + googleGeocode
                    + " isConference = " + isConference);
            ui.setPrimary(number, name, nameIsNumber, entry.label, entry.photo,
                    isConference, isGenericConf, entry.isSipCall, entry.isRealNoName, googleGeocode);
        } else {
            ui.setPrimary(mPrimary.getNumber(), null, false, null, null, isConference,
                    isGenericConf, false, false, null);
        }

    }

    private void updateSecondaryDisplayInfo(boolean isConference) {

        final CallCardUi ui = getUi();
        if (ui == null) {
            return;
        }

        final boolean isGenericConf = isGenericConference(mSecondary);
        if (mSecondaryContactInfo != null) {
            Log.d(TAG, "updateSecondaryDisplayInfo() " + mSecondaryContactInfo);
            /* SPRD: add for display secondary number
             *  @orig:final String nameForCall = getNameForCall(mSecondaryContactInfo);
             * @{ */
            String nameForCall = getNameForCall(mSecondaryContactInfo);
            String numberForCall = getNumberForCall(mSecondaryContactInfo);
            /* @} */

            final boolean nameIsNumber = nameForCall != null && nameForCall.equals(
                    mSecondaryContactInfo.number);
            /* SPRD: add for display secondary number * @{ */
            if (nameForCall == null || TextUtils.isEmpty(nameForCall) || mSecondaryContactInfo.isRealNoName) {
                nameForCall = mSecondaryContactInfo.number;
            }
            /* @} */
            if (SprdUtils.PIKEL_UI_SUPPORT) {
                ui.setSecondary(true, numberForCall ,nameForCall, nameIsNumber, mSecondaryContactInfo.label,
                        mSecondaryContactInfo.photo, isConference, isGenericConf);
            } else {
                ui.setSecondary(true, nameForCall, nameIsNumber, mSecondaryContactInfo.label,
                        mSecondaryContactInfo.photo, isConference, isGenericConf);
            }
        } else {
            // reset to nothing so that it starts off blank next time we use it.
            if (SprdUtils.PIKEL_UI_SUPPORT) {
                ui.setSecondary(false, null,null, false, null, null, isConference, isGenericConf);
            } else {
                ui.setSecondary(false, null, false, null, null, isConference, isGenericConf);
            }
        }
    }

    /**
     * Returns the gateway number for any existing outgoing call.
     */
    private String getGatewayNumber() {
        if (hasOutgoingGatewayCall()) {
            return mPrimary.getGatewayNumber();
        }

        return null;
    }

    /**
     * Returns the label for the gateway app for any existing outgoing call.
     */
    private String getGatewayLabel() {
        if (hasOutgoingGatewayCall() && getUi() != null) {
            final PackageManager pm = mContext.getPackageManager();
            try {
                final ApplicationInfo info = pm.getApplicationInfo(mPrimary.getGatewayPackage(), 0);
                return mContext.getString(R.string.calling_via_template,
                        pm.getApplicationLabel(info).toString());
            } catch (PackageManager.NameNotFoundException e) {
            }
        }
        return null;
    }

    private boolean hasOutgoingGatewayCall() {
        // We only display the gateway information while DIALING so return false for any othe
        // call state.
        // TODO: mPrimary can be null because this is called from updatePrimaryDisplayInfo which
        // is also called after a contact search completes (call is not present yet).  Split the
        // UI update so it can receive independent updates.
        if (mPrimary == null) {
            return false;
        }
        return (Call.State.isDialing(mPrimary.getState()) &&
                !TextUtils.isEmpty(mPrimary.getGatewayNumber()) &&
                !TextUtils.isEmpty(mPrimary.getGatewayPackage()));
    }

    /**
     * Gets the name to display for the call.
     */
    private static String getNameForCall(ContactCacheEntry contactInfo) {
        if (TextUtils.isEmpty(contactInfo.name) && GeocodeHelper.isSupportGoogleGeocode()) {
            return contactInfo.number;
        }
        return contactInfo.name;
    }

    /**
     * Gets the number to display for a call.
     */
    private static String getNumberForCall(ContactCacheEntry contactInfo) {
        // If the name is empty, we use the number for the name...so dont show a second
        // number in the number field
        /* SPRD: add for bug789304 @{
         * if (TextUtils.isEmpty(contactInfo.name) && GeocodeHelper.isSupportGoogleGeocode()) {
            return contactInfo.location;
        }*/
        //return TextUtils.isEmpty(contactInfo.name) ? null : contactInfo.number;
        return contactInfo.number;
        /* @} */
    }

    /* SPRD: add for bug789304 @{*/
    private static String getGeocodeForCall(ContactCacheEntry contactInfo) {
        return contactInfo.location;
    }
    /* @} */

    public void secondaryPhotoClicked() {
        CallCommandClient.getInstance().swap();
    }

    /* SPRD: add for HIGH_DEF_AUDIO @{ */
    public Call getPrimary () {
        return mPrimary;
    }
    /* @} */
    public boolean isWifiCall(){
        boolean mWifiCall = mPrimary != null ? mPrimary.can(Capabilities.PROPERTY_WIFI) :false;
        return mWifiCall;
    }

    public void endCallClicked() {
        if (mPrimary == null) {
            return;
        }

        CallCommandClient.getInstance().disconnectCall(mPrimary.getCallId());
        if (android.util.Log.isIloggable()) {
            android.util.Log.startPerfTracking("PhonePerf : HangUpCall start");
        }
    }

    public interface CallCardUi extends Ui {
        void setVisible(boolean on);
        void setPrimary(String number, String name, boolean nameIsNumber, String label,
                        Drawable photo, boolean isConference, boolean isGeneric, boolean isSipCall,
                        boolean isRealNoName, String googleGeocode); //Modify for bug789304
        void setSecondary(boolean show, String name, boolean nameIsNumber, String label,
                          Drawable photo, boolean isConference, boolean isGeneric);
        void setSecondary(boolean show, String number, String name, boolean nameIsNumber, String label,
                          Drawable photo, boolean isConference, boolean isGeneric);
        void setSecondaryImage(Drawable image);
        //@orig setCallState(int, Call.DisconnectCause, boolean, String, String);
        void setCallState(int phoneId, int state, Call.DisconnectCause cause, boolean bluetoothOn,
                          String gatewayLabel, String gatewayNumber);
        void setPrimaryCallElapsedTime(boolean show, String duration);
        void setPrimaryName(String name, boolean nameIsNumber, boolean isRealNoName);
        void setPrimaryImage(Drawable image);
        void setPrimaryPhoneNumber(String phoneNumber);
        void setPrimaryLabel(String label);
        void displayPhoneView(boolean show);
    }
}
