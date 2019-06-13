/*
 * SPRD: create
 */

package com.sprd.incallui.geocode;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.android.incallui.R;
import com.sprd.incallui.SprdUtils;
import com.sprd.phone.common.utils.OperatorUtils;
import com.sprd.phone.common.utils.SprdPhoneCommonUtils;


public class GeocodeHelper {
    private static final String TAG = "GecodeHelper";
    private static final boolean DBG = true;
    //private static String SSEARCHNUMBER = "";

    private static final String KEY = "ro.device.support.geocode";
    private static final boolean SUPPORT_SPRD_GROCODE =
            SystemProperties.getBoolean(KEY, true) && (OperatorUtils.IS_CMCC || OperatorUtils.IS_CUCC);

    public static boolean isSupportSprdGeocode() {
        return true;
    }

    public static boolean isSupportGoogleGeocode() {
        /* SPRD: always to use our own geocode provider */
        return !isSupportSprdGeocode();
    }
    /*public static void setSearchNumber(String number) {
        SSEARCHNUMBER = number;
    }*/

    public static void getGeocodeMessage(TextView view, String number) {
        if (isSupportSprdGeocode()) {
            if(!TextUtils.isEmpty(number)){
                Log.d(TAG, "getGeocodeMessage: number = " + number);
                GeocodeAsyncTask task = new GeocodeAsyncTask(view, number);
                task.execute();
            }
        } else {
            Log.w(TAG, " Can not support sprd geocode !");
        }
    }

    private static class GeocodeAsyncTask extends AsyncTask<String, Void, String> {
        private Context context;
        private TextView view;
        private String number;

        public GeocodeAsyncTask(TextView view, String number) {
            this.context = view.getContext().getApplicationContext();
            this.view = view;
            this.number = number;
        }

        @Override
        protected String doInBackground(String... params) {
            Uri uri = Uri.parse("content://gecode_location/gecode");
            String description = "";
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, new String[] {
                        "province", "city"
                }, number, null, null);
                Log.d(TAG, "getGeocodeMessage: doInBackground cursor  = " + cursor + " number = " +  number);

                if (null == cursor) {
                    return "";
                } else {
                    cursor.moveToPosition(-1);
                    if (cursor.moveToNext()) {
                        String procince = cursor.getString(0);
                        String city = cursor.getString(1);

                        if (procince.equals(city)) { // Municipality
                            description = procince;
                        } else {
                            description = procince + city;
                        }
                    }
                    String operator = GeocodeUtils.getOperatorName(context, number);
                    description = description + operator;
                    Log.d(TAG, "description: " + description + " operator = " + operator);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (null != cursor) {
                    cursor.close();
                    cursor = null;
                }
            }
            return description;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result!=null) {
                String old = view.getText().toString();
                    if (DBG) {
                        Log.d(TAG, "result 1: " + result);
                        Log.d(TAG, "old 1: " + old);
                    }		
                if (!old.equals(result)) {
                    if (DBG) {
                        Log.d(TAG, "result: " + result);
                        Log.d(TAG, "old: " + old);
                    }
                    view.setText(result);
                }
            } else if(SprdUtils.UNIVERSE_UI_SUPPORT && !TextUtils.isEmpty(number)
                    && TextUtils.isDigitsOnly(number)
                    && !PhoneNumberUtils.isEmergencyNumber(number)){
                view.setText(R.string.unknown_area);
            }
        }
    }
}
