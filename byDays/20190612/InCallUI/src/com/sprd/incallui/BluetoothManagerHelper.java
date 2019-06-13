/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.sprd.incallui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import com.android.incallui.R;
import java.util.List;

/**
 * Listens to and caches bluetooth headset state.  Used By the CallAudioManager for maintaining
 * overall audio state. Also provides method for connecting the bluetooth headset to the phone call.
 */
public class BluetoothManagerHelper {

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothHeadset mBluetoothHeadset;
    private static final String TAG = "BluetoothManagerHelper";
    static BluetoothManagerHelper mInstance;

    public static BluetoothManagerHelper getInstance(Context context) {
        if (mInstance != null) {
            return mInstance;
        }
        mInstance = new BluetoothManagerHelper(context);

        return mInstance;
    }

    public BluetoothManagerHelper(Context context) {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.getProfileProxy(context, mBluetoothProfileServiceListener,
                                    BluetoothProfile.HEADSET);
        }
    }

    private BluetoothProfile.ServiceListener mBluetoothProfileServiceListener =
            new BluetoothProfile.ServiceListener() {
                @Override
                public void onServiceConnected(int profile, BluetoothProfile proxy) {
                    mBluetoothHeadset = (BluetoothHeadset) proxy;
                    Log.d(TAG, "- Got BluetoothHeadset: " + mBluetoothHeadset);
                }

                @Override
                public void onServiceDisconnected(int profile) {
                    mBluetoothHeadset = null;
                   Log.d(TAG, "Lost BluetoothHeadset: " + mBluetoothHeadset);
                }
           };

    /**
     * @return true if a BT Headset is available, and its audio is currently connected.
     */
    public boolean isBluetoothAudioConnected() {
        if (mBluetoothHeadset == null) {
            Log.d(TAG, "isBluetoothAudioConnected: ==> FALSE (null mBluetoothHeadset)");
            return false;
        }
        List<BluetoothDevice> deviceList = mBluetoothHeadset.getConnectedDevices();

        if (deviceList.isEmpty()) {
            return false;
        }
        BluetoothDevice device = deviceList.get(0);
        boolean isAudioOn = mBluetoothHeadset.isAudioConnected(device);
        Log.d(TAG, "isBluetoothAudioConnected: ==> isAudioOn = " + isAudioOn
                + "for headset: " + device);
        return isAudioOn;
    }
}
