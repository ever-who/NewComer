package com.sprd.incallui;

import com.android.incallui.Log;
import com.android.incallui.R;

import android.content.Context;
import android.content.res.Resources.NotFoundException;

/**
 * Support telcel requirements in InCallUI.
 */
public class InCallUITelcelHelper {
    private static final String TAG = "InCallUITelcelHelper";
    private final String SPECIAL_VOICE_CLEAR_CODE = "*00015";
    static InCallUITelcelHelper mInstance;

    public static InCallUITelcelHelper getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new InCallUITelcelHelper();
        }
        return mInstance;
    }

    public InCallUITelcelHelper() {
    }

    public boolean isVoiceClearCodeLabel(Context context, String callStateLabel) {
        try {
            String unobtainableNumber = context.getString(R.string.callFailed_unobtainable_number);
            String congestion = context.getString(R.string.callFailed_congestion);
            String userBusy = context.getString(R.string.callFailed_userBusy);
            String limitExceeded = context.getString(R.string.callFailed_limitExceeded);

            if (unobtainableNumber.equals(callStateLabel)
                    || congestion.equals(callStateLabel)
                    || userBusy.equals(callStateLabel)
                    || limitExceeded.equals(callStateLabel)) {
                return true;
            }
            return false;
        } catch (NotFoundException e) {
            Log.e(TAG, "NotFoundException when getString.");
            e.printStackTrace();
            return false;
        }
    }

    public boolean isSpecialVoiceClearCode(String number) {
        if (SPECIAL_VOICE_CLEAR_CODE.equals(number)) {
            return true;
        }
        return false;
    }
}
