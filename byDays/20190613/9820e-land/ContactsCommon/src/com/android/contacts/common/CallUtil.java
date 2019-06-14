/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.contacts.common;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.TelephonyCapabilities;
import com.android.phone.common.PhoneConstants;

import android.content.Intent;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnKeyListener;
import android.view.LayoutInflater;
import android.view.ContextThemeWrapper;
import android.view.View;
import com.android.contacts.common.R;

import android.util.Log;
import android.content.ContentResolver;
import android.provider.ContactsContract;
import android.database.Cursor;

/**
 * Utilities related to calls.
 */
public class CallUtil {

    private static final String TAG = "CallUtil";
    public static final String SCHEME_TEL = "tel";
    public static final String SCHEME_SMSTO = "smsto";
    public static final String SCHEME_MAILTO = "mailto";
    public static final String SCHEME_IMTO = "imto";
    public static final String SCHEME_SIP = "sip";
    public static final String EXTRA_IS_VIDEOCALL = "android.phone.extra.IS_VIDEOCALL";

    public static final ComponentName CALL_INTENT_DESTINATION = new ComponentName(
            "com.android.phone", "com.android.phone.PrivilegedOutgoingCallBroadcaster");

    /**
     * Return an Intent for making a phone call. Scheme (e.g. tel, sip) will be determined
     * automatically.
     */
    public static Intent getCallIntent(String number) {
        return getCallIntent(number, null);
    }

    /**
     * Return an Intent for making a phone call. A given Uri will be used as is (without any
     * sanity check).
     */
    public static Intent getCallIntent(Uri uri) {
        return getCallIntent(uri, null);
    }

    /**
     * A variant of {@link #getCallIntent(String)} but also accept a call origin. For more
     * information about call origin, see comments in Phone package (PhoneApp).
     */
    public static Intent getCallIntent(String number, String callOrigin) {
        return getCallIntent(getCallUri(number), callOrigin);
    }

    /**
     * A variant of {@link #getCallIntent(android.net.Uri)} but also accept a call origin. For more
     * information about call origin, see comments in Phone package (PhoneApp).
     */
    public static Intent getCallIntent(Uri uri, String callOrigin) {
        final Intent intent = new Intent(Intent.ACTION_CALL_PRIVILEGED, uri);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (callOrigin != null) {
            intent.putExtra(PhoneConstants.EXTRA_CALL_ORIGIN, callOrigin);
        }

        // Set phone as an explicit component of CALL_PRIVILEGED intent.
        // Setting destination explicitly prevents other apps from capturing this Intent since,
        // unlike SendBroadcast, there is no API for specifying a permission on startActivity.
        intent.setComponent(CALL_INTENT_DESTINATION);

        return intent;
    }

    //videocall
    public static Intent getVideoCallIntent(String number, String callOrigin) {
        final Intent intent = new Intent(Intent.ACTION_CALL, getCallUri(number));
        intent.putExtra(EXTRA_IS_VIDEOCALL,true);
        if (callOrigin != null) {
            intent.putExtra(PhoneConstants.EXTRA_CALL_ORIGIN, callOrigin);
        }
        intent.setComponent(CALL_INTENT_DESTINATION);
        return intent;
    }

    public static void showCallDialogAlert(final Context context,final String number,final String callOrigin ) {
        LayoutInflater inflater = (LayoutInflater)context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        final View customView = inflater
                .inflate(R.layout.dial_call_type, null);
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(context, R.style.DialerCallTypeStyle);
        AlertDialog.Builder builder = new AlertDialog.Builder(contextThemeWrapper);
        builder.setCustomTitle(customView);
        builder.setItems(R.array.dial_call_type_options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        Intent audioCallIntent = getCallIntent(number, callOrigin);
                        context.startActivity(audioCallIntent);
                        if(dialog != null){
                            dialog.dismiss();
                        }
                        break;
                    case 1:
                        Intent videoCallIntent = getVideoCallIntent(number,callOrigin);
                        context.startActivity(videoCallIntent);
                        if(dialog != null){
                            dialog.dismiss();
                        }
                        break;
                    default:
                        break;
                }
            }
        });
        builder.setNegativeButton(R.string.select_cancle, null);
        builder.show();
    }

    /**
     * Return Uri with an appropriate scheme, accepting Voicemail, SIP, and usual phone call
     * numbers.
     */
    public static Uri getCallUri(String number) {
        if (PhoneNumberUtils.isUriNumber(number)) {
             return Uri.fromParts(SCHEME_SIP, number, null);
        }
        return Uri.fromParts(SCHEME_TEL, number, null);
     }

    public static boolean isVideoEnabled(Context context) {
        TelephonyManager telephonyMgr = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyMgr == null) {
            return false;
        }

        return SystemProperties.getBoolean("persist.sys.volte.enable", false);
    }

    /* SPRD: add for bug632743, click dial key, directly call on contact list. @} */
    public static String getPhoneNumber(Context context, Uri uri) {
        String[] CONTACTS_PROJECTION = new String[] {
                "name_raw_contact_id",// 0
                "has_phone_number",// 1
        };
        String[] DATA_PROJECTION = new String[] {
                ContactsContract.Data.DATA1,// 0
        };

        ContentResolver resolver = context.getContentResolver();
        Cursor contactsCursor = null;
        try {
            contactsCursor = resolver.query(uri, CONTACTS_PROJECTION, null, null, null);
            if (contactsCursor != null && contactsCursor.moveToFirst()) {
                int rawId = contactsCursor.getInt(0);
                int hasPhoneNumber = contactsCursor.getInt(1);

                if (hasPhoneNumber == 1) {
                    Cursor dataCursor = null;
                    try {
                        String query = ContactsContract.Data.RAW_CONTACT_ID + "=?" + " AND "
                                + ContactsContract.Data.MIMETYPE + "=?";
                        dataCursor = resolver.query(ContactsContract.Data.CONTENT_URI,
                                DATA_PROJECTION, query, new String[] {
                                        String.valueOf(rawId),
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                                }, null);
                        if (dataCursor != null && dataCursor.moveToFirst()) {
                            return dataCursor.getString(0);
                        }
                    } finally {
                        if (dataCursor != null) {
                            dataCursor.close();
                            dataCursor = null;
                        }
                    }
                }
            }
        } finally {
            if (contactsCursor != null) {
                contactsCursor.close();
                contactsCursor = null;
            }
        }

        Log.d(TAG, "No valid number");
        return null;
    }
    /* @} */
}
