package com.sprd.incallui;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.RemoteException;
import android.view.Surface;

import com.android.incallui.Log;
import com.android.incallui.VideoCallPresenter;
import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.ISprdCallCommandService;

public class SprdCallCommandClient {
    private static SprdCallCommandClient sInstance;

    public static synchronized SprdCallCommandClient getInstance() {
        if (sInstance == null) {
            sInstance = new SprdCallCommandClient();
        }
        return sInstance;
    }

    private ISprdCallCommandService mSprdCommandService;

    private SprdCallCommandClient() {
    }

    public void setService(ISprdCallCommandService service) {
        mSprdCommandService = service;
    }

    public void setRemoteSurface(Surface sf, Call call){
        if (mSprdCommandService == null || call == null) {
            return;
        }
        try {
            mSprdCommandService.setRemoteSurface(sf, call);
        } catch (Exception e) {
            Log.e(this, "Error on enableRecord().", e);
        }
    }

    public void setLocalSurface(Surface sf, Call call){
        if (mSprdCommandService == null || call == null) {
            return;
        }
        try {
            mSprdCommandService.setLocalSurface(sf, call);
        } catch (Exception e) {
            Log.e(this, "Error on enableRecord().", e);
        }
    }

    /*public void requestVolteCallMediaChange(boolean isVideo, int phoneId){
        if (mSprdCommandService == null) {
            return;
        }
        try {
            mSprdCommandService.requestVolteCallMediaChange(isVideo, phoneId, call);
            VideoCallPresenter.getInstance().changeVolteCallMediaType();
        } catch (Exception e) {
            Log.e(this, "Error on requestVolteCallMediaChange().", e);
            return;
        }
    }*/

    public void requestVolteCallMediaChange(int toVideoDetailsState, int phoneId, Call call){
        if (mSprdCommandService == null || call == null) {
            return;
        }
        try {
            mSprdCommandService.requestVolteCallMediaChange(toVideoDetailsState, phoneId, call);
        } catch (Exception e) {
            Log.e(this, "Error on requestVolteCallMediaChange(int int).", e);
            return;
        }
    }

    public void handleSetCamera(String cameraId, Call call) {
        if (mSprdCommandService == null || call == null) {
            return;
        }
        try {
            mSprdCommandService.handleSetCamera(cameraId, call);
            VideoCallPresenter.getInstance().changeVolteCallMediaType();
        } catch (Exception e) {
            Log.e(this, "Error on handleSetCamera().", e);
            return;
        }
    }
}

