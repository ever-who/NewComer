/*
 * Copyright (C) 2013 Spreadtrum Communications Inc. 
 *
 */

package com.sprd.incallui;

import com.android.dialer.DialtactsActivity;
import com.android.services.telephony.common.Call;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemProperties;
import android.os.Vibrator;
import android.util.Log;

public class SprdUtils {
    public static final boolean UNIVERSE_UI_SUPPORT = SystemProperties.getBoolean("universe_ui_support",false);
//    public static final boolean UNIVERSE_UI_SUPPORT = true;
    public static final boolean PIKEL_UI_SUPPORT = SystemProperties.getBoolean("pikel_ui_support",true);

    /* SPRD: vibration feedback for call connection and disconnection. See bug624152 @{ */
    public static String TAG = "InCallUtils";
    public static final String VIBRATION_FEEDBACK_FOR_DISCONNECT_PREFERENCES_NAME =
            "call_disconnection_prompt_key";
    public static final String VIBRATION_FEEDBACK_FOR_CONNECT_PREFERENCES_NAME =
            "call_connection_prompt_key";
    public static final int VIBRATE_DURATION = 100; // Will vibrate for 100ms.
    /* end @} */

    public  static boolean getKeyGuardStatus(Context context){
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if(keyguardManager !=null && keyguardManager.inKeyguardRestrictedInputMode()){
            return true;
        }else{
            return false;
        }
    }

    /* SPRD: vibration feedback for call connection and disconnection. See bug624152 @{ */
    static public void vibrateForCallStateChange(Context context, Call call, String preferenceName) {
        SharedPreferences sp = context.getApplicationContext().getSharedPreferences(
                DialtactsActivity.SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        boolean vibrate= sp.getBoolean(preferenceName, false);

        if (vibrate && call != null) {
            Log.d(TAG, "vibrate for call state changed to active or disconnected: (" + call.toString() + ")");
            if ((call.getState() == Call.State.ACTIVE || call.getState() == Call.State.DISCONNECTED)
                    && !call.isConferenceCall()) {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(SprdUtils.VIBRATE_DURATION);
            }
        }
    }
    /* end @} */
}
