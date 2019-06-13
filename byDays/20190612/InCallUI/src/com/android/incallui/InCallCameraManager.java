package com.android.incallui;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Debug;

import java.lang.String;

import com.android.incallui.Log;

/**
 * Used to track which camera is used for outgoing video.
 */
public class InCallCameraManager {

    /**
     * The camera ID for the front facing camera.
     */
    private String mFrontFacingCameraId;

    /**
     * The camera ID for the rear facing camera.
     */
    private String mRearFacingCameraId;

    /**
     * The currently active camera.
     */
    private boolean mUseFrontFacingCamera;

    /**
     * Aspect ratio of the front facing camera.
     */
    private float mFrontFacingCameraAspectRatio;

    /**
     * Aspect ratio of the rear facing camera.
     */
    private float mRearFacingCameraAspectRatio;
    // SPRD: add for bug427890
    private static final boolean DBG = Debug.isDebug();

    /**
     * Initializes the InCall CameraManager.
     *
     * @param context The current context.
     */
    public InCallCameraManager(Context context) {
        mUseFrontFacingCamera = true;
        initializeCameraList(context);
    }

    /**
     * Sets whether the front facing camera should be used or not.
     *
     * @param useFrontFacingCamera {@code True} if the front facing camera is to be used.
     */
    public void setUseFrontFacingCamera(boolean useFrontFacingCamera) {
        mUseFrontFacingCamera = useFrontFacingCamera;
    }

    /**
     * Determines whether the front facing camera is currently in use.
     *
     * @return {@code True} if the front facing camera is in use.
     */
    public boolean isUsingFrontFacingCamera() {
        return mUseFrontFacingCamera;
    }

    /**
     * Determines the active camera ID.
     *
     * @return The active camera ID.
     */
    public String getActiveCameraId() {
        //SPRD: add for bug427890
        if(DBG) Log.i(this, "getActiveCameraId mUseFrontFacingCamera = " + mUseFrontFacingCamera
                + " mFrontFacingCameraId = " + mFrontFacingCameraId + " mRearFacingCameraId = " + mRearFacingCameraId);
        if (mUseFrontFacingCamera) {
            return mFrontFacingCameraId;
        } else {
            return mRearFacingCameraId;
        }
    }

    /**
     * Get the camera ID and aspect ratio for the front and rear cameras.
     *
     * @param context The context.
     */
    private void initializeCameraList(Context context) {
        if (context == null) {
            //SPRD: add for bug427890
            if(DBG) Log.i(this, "initializeCameraList context is null");
            return;
        }

        CameraManager cameraManager = null;
        try {
            cameraManager = (CameraManager) context.getSystemService(
                    Context.CAMERA_SERVICE);
        } catch (Exception e) {
            Log.e(this, "Could not get camera service.");
            return;
        }

        if (cameraManager == null) {
            //SPRD: add for bug427890
            if(DBG) Log.i(this, "initializeCameraList cameraManager is null");
            return;
        }

        String[] cameraIds = {};
        try {
            cameraIds = cameraManager.getCameraIdList();
        } catch (CameraAccessException e) {
            //SPRD: add for bug427890
            if(DBG) Log.i(this, "Could not access camera: "+e);
            // Camera disabled by device policy.
            return;
        }
        //SPRD: add for bug427890
        if(DBG) Log.i(this, "initializeCameraList cameraIds.length = " + cameraIds.length);

        for (int i = 0; i < cameraIds.length; i++) {
            CameraCharacteristics c = null;
            try {
                c = cameraManager.getCameraCharacteristics(cameraIds[i]);
            } catch (IllegalArgumentException e) {
                //SPRD: add for bug427890
                if(DBG) Log.i(this, "initializeCameraList IllegalArgumentException" + e);
                // Device Id is unknown.
            } catch (CameraAccessException e) {
                //SPRD: add for bug427890
                if(DBG) Log.i(this, "initializeCameraList CameraAccessException" + e);
                // Camera disabled by device policy.
            }
            if (c != null) {
                int facingCharacteristic = c.get(CameraCharacteristics.LENS_FACING);
                if (facingCharacteristic == CameraCharacteristics.LENS_FACING_FRONT) {
                    mFrontFacingCameraId = cameraIds[i];
                } else if (facingCharacteristic == CameraCharacteristics.LENS_FACING_BACK) {
                    mRearFacingCameraId = cameraIds[i];
                }
                //SPRD: add for bug427890
                if(DBG) Log.i(this, "initializeCameraList facingCharacteristic = " + facingCharacteristic
                        + " mFrontFacingCameraId = " + mFrontFacingCameraId + " mRearFacingCameraId = " + mRearFacingCameraId);
            }
        }
    }

    private boolean mCameraPaused;

    public boolean isCameraPaused() {
        return mCameraPaused;
    }
    //add for SPRD:Bug 673217
    public String getFrontFacingCameraId () {
        return mFrontFacingCameraId;
    }
    public String getRearFacingCameraId () {
        return mRearFacingCameraId;
    }

    public void setCameraPaused(boolean pause) {
        mCameraPaused = pause;
    }
}
