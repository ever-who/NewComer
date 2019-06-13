package com.android.incallui;

import java.util.Objects;
import java.util.Set;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.telecom.VideoProfile;
import android.telephony.TelephonyManager;
import android.view.Surface;
import android.widget.Toast;

import com.android.contacts.common.CallUtil;
import com.android.incallui.AudioModeProvider;
import com.android.incallui.CallCommandClient;
import com.android.incallui.CallList;
import com.android.incallui.InCallPresenter;
import com.android.incallui.Log;
import com.android.incallui.R;
import com.android.incallui.Presenter;
import com.android.incallui.Ui;
import com.android.incallui.CallButtonPresenter.CallButtonUi;
import com.android.incallui.CallList.Listener;
import com.android.incallui.InCallPresenter.InCallState;
import com.android.incallui.InCallPresenter.InCallStateListener;
import com.android.incallui.InCallPresenter.IncomingCallListener;
import com.android.internal.telephony.SprdRIL;
import com.android.services.telephony.common.AudioMode;
import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.Call.Capabilities;
import com.google.android.collect.Sets;
import com.google.common.base.Preconditions;
import com.sprd.incallui.SprdCallCommandClient;
import android.os.SystemProperties;

public class VideoCallPresenter extends Presenter<VideoCallPresenter.VideoCallUi>
        implements InCallStateListener, IncomingCallListener {

    
    /**
     * The minimum width or height of the preview surface.  Used when re-sizing the preview surface
     * to match the aspect ratio of the currently selected camera.
     */
    private float mMinimumVideoDimension;

    /**
     * The current context.
     */
    private Context mContext;

    /**
     * The call the video surfaces are currently related to
     */
    private Call mPrimaryCall;

    /**
     * Determines if the current UI state represents a video call.
     */
    private boolean mIsVideoCall;

    /**
     * Determines the device orientation (portrait/lanscape).
     */
    private int mDeviceOrientation;

    /**
     * Determines whether the video surface is in full-screen mode.
     */
    private boolean mIsFullScreen = false;

    /**
     * Saves the audio mode which was selected prior to going into a video call.
     */
    private int mPreVideoAudioMode = AudioModeProvider.AUDIO_MODE_INVALID;
    private static boolean mIsVideoMode = false;

    private static final Object mLock = new Object();
    private static VideoCallPresenter mVideoCallPresenter;
    // SPRD: add for bug427890
    private static final boolean DBG = Debug.isDebug();

    private static float DEFAULT_ASPECTRATIO = (float)720/(float)1280;
    private static final int EVENT_VOLTE_CALL_PREVIEW_SURFACE = 10000;
    private boolean isVolteEnable = SystemProperties.getBoolean("persist.sys.volte.enable", false);
    private boolean mIsVideoCallHold;
    /* SPRD: fix bug 782497 @{ */
    private int mCurrentVideoState = VideoProfile.STATE_AUDIO_ONLY;
    private int mCurrentCallState = Call.State.INVALID;
    /* @} */
    //SPRD: add for 871916
    private boolean mIsAudioCallDialing = false;

    public static VideoCallPresenter getInstance() {
        synchronized (mLock) {
            if (mVideoCallPresenter == null) {
                mVideoCallPresenter = new VideoCallPresenter();
            }
        }
        return mVideoCallPresenter;
    }

    /**
     * Initializes the presenter.
     *
     * @param context The current context.
     */
    public void init(Context context) {
        mContext = Preconditions.checkNotNull(context);
        mMinimumVideoDimension = mContext.getResources().getDimension(
                R.dimen.video_preview_small_dimension);
    }

    /**
     * Called when the user interface is ready to be used.
     *
     * @param ui The Ui implementation that is now ready to be used.
     */
    @Override
    public void onUiReady(VideoCallUi ui) {
        super.onUiReady(ui);

        // Register for call state changes last
        InCallPresenter.getInstance().addListener(this);
        InCallPresenter.getInstance().addIncomingCallListener(this);

        mIsVideoCall = false;
        /* SPRD: fix bug 782497 @{ */
        mCurrentVideoState = VideoProfile.STATE_AUDIO_ONLY;
        mCurrentCallState = Call.State.INVALID;
        /* @} */
    }

    /**
     * Called when the user interface is no longer ready to be used.
     *
     * @param ui The Ui implementation that is no longer ready to be used.
     */
    @Override
    public void onUiUnready(VideoCallUi ui) {
        super.onUiUnready(ui);

        InCallPresenter.getInstance().removeListener(this);
        InCallPresenter.getInstance().removeIncomingCallListener(this);
    }

    /**
     * Handles incoming calls.
     *
     * @param state The in call state.
     * @param call The call.
     */
    @Override
    public void onIncomingCall(InCallPresenter.InCallState newState, Call call) {
        // same logic should happen as with onStateChange()
        onStateChange(newState, CallList.getInstance());
    }

    /**
     * Handles state changes (including incoming calls)
     *
     * @param newState The in call state.
     * @param callList The call list.
     */
    @Override
    public void onStateChange(InCallPresenter.InCallState newState, CallList callList) {
        // Bail if video calling is disabled for the device.
        if (!CallUtil.isVideoEnabled(mContext)) {
            return;
        }

        if (newState == InCallPresenter.InCallState.NO_CALLS) {
            exitVideoMode();
        }

        // Determine the primary active call).
        Call primary = null;
        if (newState == InCallPresenter.InCallState.INCOMING) {
            primary = callList.getActiveCall();
            if (!(primary != null && primary.isVideo())) {
                primary = callList.getIncomingCall();
            }
        } else if (newState == InCallPresenter.InCallState.OUTGOING) {
            primary = callList.getOutgoingCall();
        } else if (newState == InCallPresenter.InCallState.INCALL) {
            primary = callList.getActiveCall();
        }

        final boolean primaryChanged = ((mPrimaryCall == null && primary != null)
                || (mPrimaryCall != null && primary == null)
                || (mPrimaryCall != null && primary != null && (mPrimaryCall.getCallId() != primary.getCallId()
                || mPrimaryCall.isVideo() != primary.isVideo())));
        /** SPRD: modify primaryChanged to vt surface disappear for bug383038
         * org:if (primaryChanged) {
         *  @ { */
        boolean shouldUpdateUi = false;
        if(primary != null && getUi() != null){
            // SPRD:modify getUi().isActivityRestart() to getUi().isVideoSurfacesInUse for bug390673
            shouldUpdateUi = getUi().isVideoSurfacesInUse() && !primary.isVideo();
            if(TelephonyManager.getVolteEnabled()
                    && (primary.isVideo() != getUi().isVideoSurfacesInUse())){
                shouldUpdateUi = true;
                Log.i(this, "onStateChange->isVideoSurfacesInUse:" + getUi().isVideoSurfacesInUse());
            }
        }
        Log.i(this, "onStateChange->primaryChanged:" + primaryChanged + " shouldUpdateUi="+shouldUpdateUi
                +" primary="+primary+" mPrimaryCall="+mPrimaryCall);

        if (primaryChanged || shouldUpdateUi) {
        /** @} */
            //mPrimaryCall = primary;

            if (primary != null) {
                //checkForVideoCallChange();
                mIsVideoCall = primary.isVideo();
                if (mIsVideoCall) {
                    enterVideoMode(primary);
                } else if(!mIsVideoCall){
                    exitVideoMode();
                }
            } else if (primary == null) {
                // If no primary call, ensure we exit video state and clean up the video surfaces.
                /*SPRD: add for bug640813 @{*/
                /*if(callList.getBackgroundCall() != null && callList.getBackgroundCall().isVideo()){
                    mIsVideoCallHold = true;
                }else{
                    mIsVideoCallHold = false;
                }*/
                /* @} */
                exitVideoMode();
            }
        }
        mPrimaryCall = primary;
        /* SPRD: fix bug 782497 @{ */
        if(primary != null){
            updateVideoCall(primary);
        }
        updateCallCache(primary);
        /* @} */
    }
    public boolean isVideoMode() {
        return mIsVideoMode;
    }
    /**
     * Enters video mode by showing the video surfaces and making other adjustments (eg. audio).
     * TODO(vt): Need to adjust size and orientation of preview surface here.
     */
    private void enterVideoMode(Call call) {
        int newVideoDetailsState = call.getVideoDetailsState();
        final boolean bluetoothSupported = (AudioMode.BLUETOOTH == (
                AudioModeProvider.getInstance().getSupportedModes() & AudioMode.BLUETOOTH));
        VideoCallUi ui = getUi();
        // SPRD: add for bug427890
        if (DBG) Log.i(this, "enterVideoMode ui is null?:" + (ui == null) + " mPrimaryCall is null?:" + (mPrimaryCall == null)
                + " newVideoDetailsState = "+newVideoDetailsState);
        if (ui == null) {
            return;
        }
        //SPRD: add for 871916
        mIsAudioCallDialing = isAudioCallDialing(call);

        ui.showVideoUi(true);

        // Communicate the current camera to telephony and make a request for the camera
        // capabilities.
        if (call != null) {
            // Do not reset the surfaces if we just restarted the activity due to an orientation
            // change.
            if (ui.isActivityRestart()) {
                // SPRD: add for bug427890
                if (DBG) Log.i(this, "enterVideoMode ui is ActivityRestart.");
                return;
            }

            InCallCameraManager cameraManager = InCallPresenter.getInstance().
                    getInCallCameraManager();
            /*SPRD: add for bug421900 and for bug651725@{*/
            //add for SPRD:Bug 673217
            Log.d(this, "enterVideoMode getFrontFacingCameraId:"+cameraManager.getFrontFacingCameraId());
            if (cameraManager.getFrontFacingCameraId() == null) {
                cameraManager.setUseFrontFacingCamera(false);
            } else {
                cameraManager.setUseFrontFacingCamera(true);
            }
            if (InCallPresenter.getInstance().getActivity() != null) {
                CallButtonFragment mCallButtonUi =  InCallPresenter.getInstance().getActivity().getCallButtonFragment();
                if (mCallButtonUi != null) {
                    mCallButtonUi.setSwitchCameraButtonSelect(false);
                }
            }
            /* @} */

            //SPRD: add for 871916
            Log.d(this, "mIsAudioCallDialing = "+mIsAudioCallDialing);
            if(mIsAudioCallDialing){
                SprdCallCommandClient.getInstance().handleSetCamera(null, call);
            }else{
                SprdCallCommandClient.getInstance().handleSetCamera(cameraManager.getActiveCameraId(), call);
            }

            if (ui.isDisplayVideoSurfaceCreated()) {
                SprdCallCommandClient.getInstance().setRemoteSurface(ui.getDisplayVideoSurface(), call);
            }
        }

        mPreVideoAudioMode = AudioModeProvider.getInstance().getAudioMode();
        //add for SPRD:Bug 716618, 871916
        if(!bluetoothSupported && !mIsAudioCallDialing){
            /** SPRD: VIDEO CALL FEATURE. FIXBUG383592 @{ */
            if(mPreVideoAudioMode == AudioMode.EARPIECE
                    || mPreVideoAudioMode == AudioModeProvider.AUDIO_MODE_INVALID){
            /** @} */
                CallCommandClient.getInstance().setAudioMode(AudioMode.SPEAKER);
            }
        }
        mIsVideoMode = true;
    }

    /**
     * Exits video mode by hiding the video surfaces  and making other adjustments (eg. audio).
     */
    private void exitVideoMode() {
        VideoCallUi ui = getUi();
        // SPRD: add for bug427890
        if (DBG) Log.i(this, "exitVideoMode ui is null?:" + (ui == null) + " mPrimaryCall is null?:" + (mPrimaryCall == null)
                 +"  mIsVideoCallHold="+mIsVideoCallHold);
        if (ui == null) {
            return;
        }
        // SPRD: modify for bug640813
        /*if(mIsVideoCallHold){
            SprdCallCommandClient.getInstance().setRemoteSurface(null, mPrimaryCall);
            SprdCallCommandClient.getInstance().setLocalSurface(null, mPrimaryCall);
        }*/

        // SPRD: Add for bug392827
        InCallPresenter.getInstance().getInCallCameraManager().setCameraPaused(false);

        ui.showVideoUi(false);

        //Call activeCall = CallList.getInstance().getActiveCall();
        //Call backgroundCall = CallList.getInstance().getBackgroundCall();
        if((mPrimaryCall != null && mPrimaryCall.can(Capabilities.PROPERTY_WIFI) && mPrimaryCall.isVideo())
                /*||(backgroundCall != null && backgroundCall.can(Capabilities.PROPERTY_WIFI) && backgroundCall.isVideo())
                ||(mPrimaryCall != null && mPrimaryCall.getState() == Call.State.CALL_WAITING
                && activeCall != null && activeCall.can(Capabilities.PROPERTY_WIFI) && activeCall.isVideo())*/){
            SprdCallCommandClient.getInstance().handleSetCamera(null,mPrimaryCall);
        }

        if (mPreVideoAudioMode != AudioModeProvider.AUDIO_MODE_INVALID) {
            CallCommandClient.getInstance().setAudioMode(mPreVideoAudioMode);
            mPreVideoAudioMode = AudioModeProvider.AUDIO_MODE_INVALID;
        }
        mIsVideoMode = false;
    }

    /**
     * Checks for a change to the video call and changes it if required.
     */
    private void checkForVideoCallChange() {
        Call firstCall = CallList.getInstance().getFirstCall();
        if (!Objects.equals(firstCall, mPrimaryCall)) {
            //changeVideoCall(firstCall);
        }
    }

    /**
     * Handles a change to the video call.  Sets the surfaces on the previous call to null and sets
     * the surfaces on the new video call accordingly.
     *
     * @param videoCall The new video call.
     */
    private void changeVideoCall(Call firstCall) {
        if (firstCall != null) {
            SprdCallCommandClient.getInstance().setRemoteSurface(null, mPrimaryCall);
            SprdCallCommandClient.getInstance().setLocalSurface(null, mPrimaryCall);
        }
        mPrimaryCall = firstCall;
    }

    /**
     * Sets the preview surface size based on the current device orientation.
     * See: {@link Configuration.ORIENTATION_LANDSCAPE}, {@link Configuration.ORIENTATION_PORTRAIT}
     *
     * @param orientation The device orientation.
     * @param aspectRatio The aspect ratio of the camera (width / height).
     */
    private void setPreviewSize(int orientation, float aspectRatio) {
        VideoCallUi ui = getUi();
        if (ui == null) {
            return;
        }

        int height;
        int width;
        if (isVolteEnable || (orientation == Configuration.ORIENTATION_LANDSCAPE)) {
            width = (int) (mMinimumVideoDimension * aspectRatio);
            height = (int) mMinimumVideoDimension;
        } else {
            width = (int) mMinimumVideoDimension;
            height = (int) (mMinimumVideoDimension * aspectRatio);
        }
        ui.setPreviewSize(width, height);
    }
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Log.i(this, "handleMessage->msg.what:" + msg.what);
            switch (msg.what) {
                case EVENT_VOLTE_CALL_PREVIEW_SURFACE:
                    if(mPrimaryCall == null){//SPRD:modify for bug819226
                      return;
                    }
                    float aspectRatio = (float) 180 / (float) 240;
                    mDeviceOrientation = 1;
                    /* SPRD: Add for vowifi video call @{ */
                    if(mPrimaryCall != null && mPrimaryCall.can(Capabilities.PROPERTY_WIFI)){
                        aspectRatio = (float) msg.arg1 / (float) msg.arg2;
                    }
                    /* @} */
                    setPreviewSize(mDeviceOrientation, aspectRatio);
                    break;
            }
        }
    };
    public void onVTManagerEvent(Message message) {
        // TODO Auto-generated method stub
        switch (message.what) {
        case EVENT_VOLTE_CALL_PREVIEW_SURFACE:
            mHandler.removeMessages(EVENT_VOLTE_CALL_PREVIEW_SURFACE);
            mHandler.sendMessageDelayed(message,300);
            break;
       }
    }
    public void changeVolteCallMediaType() {
        VideoCallUi ui = getUi();
        if (ui == null) {
            return;
        }
        //ui.showVideoUi(CallList.getInstance().getFirstCall().isVideo());
    }

    /**
     * Handles the creation of a surface in the {@link VideoCallFragment}.
     *
     * @param surface The surface which was created.
     */
    public void onSurfaceCreated(int surface) {
        final VideoCallUi ui = getUi();

        // SPRD: modify for surface don't clear in some case
        Log.i(this, "onSurfaceCreated->surface:" + surface + " ui is null?:"
                + (ui == null) + "  mPrimaryCall is null?:" + (mPrimaryCall == null));
        if (ui == null || mPrimaryCall == null) {
            return;
        }

        // If the preview surface has just been created and we have already received camera
        // capabilities, but not yet set the surface, we will set the surface now.
        if (surface == VideoCallFragment.SURFACE_PREVIEW) {
            //setPreviewSize(Configuration.ORIENTATION_PORTRAIT, DEFAULT_ASPECTRATIO);
            SprdCallCommandClient.getInstance().setLocalSurface(ui.getPreviewVideoSurface(), mPrimaryCall);
            /*SPRD:modify for bug635158 @{*/
            InCallCameraManager cameraManager = InCallPresenter.getInstance().
                    getInCallCameraManager();
            //SPRD: add for 871916
            if(isAudioCallDialing(mPrimaryCall)){
                SprdCallCommandClient.getInstance().handleSetCamera(null, mPrimaryCall);
            }else{
                SprdCallCommandClient.getInstance().handleSetCamera(cameraManager.getActiveCameraId(),mPrimaryCall);
                cameraManager.setCameraPaused(false);
                CallButtonFragment mCallButtonUi =  InCallPresenter.getInstance().getActivity().getCallButtonFragment();
                mCallButtonUi.setPauseVideoButton(false);
            }
            /* @}*/
        } else if (surface == VideoCallFragment.SURFACE_DISPLAY) {
            SprdCallCommandClient.getInstance().setRemoteSurface(ui.getDisplayVideoSurface(), mPrimaryCall);
        }
    }

    /**
     * Handles structural changes (format or size) to a surface.
     *
     * @param surface The surface which changed.
     * @param format The new PixelFormat of the surface.
     * @param width The new width of the surface.
     * @param height The new height of the surface.
     */
    public void onSurfaceChanged(int surface, int format, int width, int height) {
        //Do stuff
    }

    /**
     * Handles the destruction of a surface in the {@link VideoCallFragment}.
     *
     * @param surface The surface which was destroyed.
     */
    public void onSurfaceDestroyed(int surface) {
        final VideoCallUi ui = getUi();
        /** SPRD: modify for surface don't clear in some case
         * @Orig:if (ui == null || mVideoCall == null) {
         * { */
        Log.i(this, "onSurfaceDestroyed->surface:" + surface + " ui is null?:"
                + (ui == null) + "  mPrimaryCall is null?:" + (mPrimaryCall == null));
        if (mPrimaryCall == null) {
        /** @} */
            return;
        }

        if (surface == VideoCallFragment.SURFACE_DISPLAY) {
            SprdCallCommandClient.getInstance().setRemoteSurface(null, mPrimaryCall);
        } else if (surface == VideoCallFragment.SURFACE_PREVIEW) {
            //Call activeCall = CallList.getInstance().getActiveCall();
            //Call backgroundCall = CallList.getInstance().getBackgroundCall();
            if((mPrimaryCall != null && mPrimaryCall.can(Capabilities.PROPERTY_WIFI) && mPrimaryCall.isVideo())
                    /*||(backgroundCall != null && backgroundCall.can(Capabilities.PROPERTY_WIFI) && backgroundCall.isVideo())
                    ||(mPrimaryCall != null && mPrimaryCall.getState() == Call.State.CALL_WAITING
                    && activeCall != null && activeCall.can(Capabilities.PROPERTY_WIFI) && activeCall.isVideo())*/){
                SprdCallCommandClient.getInstance().handleSetCamera(null,mPrimaryCall);
            }
            SprdCallCommandClient.getInstance().setLocalSurface(null, mPrimaryCall);
        }
    }

    /**
     * Handles clicks on the video surfaces by toggling full screen state.
     * Informs the {@link InCallPresenter} of the change so that it can inform the
     * {@link CallCardPresenter} of the change.
     *
     * @param surfaceId The video surface receiving the click.
     */
    public void onSurfaceClick(int surfaceId) {
        //TODO
    }

    /**
     * Based on the current video state and call state, show or hide the incoming and
     * outgoing video surfaces.  The outgoing video surface is shown any time video is transmitting.
     * The incoming video surface is shown whenever the video is un-paused and active.
     *
     * @param videoState The video state.
     */
    private void showVideoUi(int videoState) {
        VideoCallUi ui = getUi();
        if (ui == null || videoState == SprdRIL.VIDEO_DETAILS_STATE_INVALID || videoState == VideoProfile.STATE_AUDIO_ONLY) {
            Log.e(this, "showVideoUi, VideoCallUi is null returning, ui is null? "+(ui == null) +", videoState = "+videoState);
            return;
        }

        boolean showOutgoingVideo = showOutgoingVideo(videoState);
        boolean showIncomingVideo = showIncomingVideo(videoState);
        Log.v(this, "showVideoUi : showOutgoing = " + showOutgoingVideo + " showIncoming = "
                + showIncomingVideo);
        if (showOutgoingVideo || showIncomingVideo) {
            ui.showVideoViews(showOutgoingVideo, showIncomingVideo);
        } else {
            ui.hideVideoViews();
        }
    }

    /**
     * Determines if the incoming video surface should be shown based on the current videoState and
     * callState.  The video surface is shown when incoming video is not paused, the call is active,
     * and video reception is enabled.
     *
     * @param videoState The current video state.
     * @param callState The current call state.
     * @return {@code true} if the incoming video surface should be shown, {@code false} otherwise.
     */
    public static boolean showIncomingVideo(int videoState) {
        boolean isPaused = VideoProfile.isPaused(videoState);
        return !isPaused && VideoProfile.isReceptionEnabled(videoState);
    }

    /**
     * Determines if the outgoing video surface should be shown based on the current videoState.
     * The video surface is shown if video transmission is enabled.
     *
     * @param videoState The current video state.
     * @return {@code true} if the the outgoing video surface should be shown, {@code false}
     *      otherwise.
     */
    public static boolean showOutgoingVideo(int videoState) {
        return VideoProfile.isTransmissionEnabled(videoState);
    }

    /* SPRD: fix bug 782497 @{ */
    private void updateVideoCall(Call call) {
        checkForVideoStateChange(call);
        checkForCallStateChange(call);
    }

    private void checkForVideoStateChange(Call call) {
        final int newVideoState = call.getVideoDetailsState();
        Log.d(this,"checkForVideoStateChange: mIsVideoCall = " + mIsVideoCall
                + ", mCurrentVideoState = "+ mCurrentVideoState
                + ", newVideoState = "+ newVideoState);

        if(call.getState() != Call.State.ACTIVE || mCurrentVideoState == newVideoState){
            return;
        }

        if(mIsVideoCall){
            showVideoUi(newVideoState);

            if(!call.can(Capabilities.PROPERTY_WIFI)){
                return;
            }

            if(VideoProfile.isBidirectional(newVideoState)){
                if(VideoProfile.isReceptionEnabled(mCurrentVideoState)){
                    InCallCameraManager cameraManager = InCallPresenter.getInstance().
                            getInCallCameraManager();
                    SprdCallCommandClient.getInstance().handleSetCamera(cameraManager.getActiveCameraId(), call);
                }else if(VideoProfile.isTransmissionEnabled(mCurrentVideoState)){
                    VideoCallUi ui = getUi();
                    Log.d(this,"checkForVideoStateChange: ui.isDisplayVideoSurfaceCreated() = " + ui.isDisplayVideoSurfaceCreated());
                    if(ui != null && ui.isDisplayVideoSurfaceCreated()) {
                        SprdCallCommandClient.getInstance().setRemoteSurface(ui.getDisplayVideoSurface(), call);
                    }
                }
            }else if(VideoProfile.isReceptionEnabled(newVideoState)){
                SprdCallCommandClient.getInstance().handleSetCamera(null, call);
                SprdCallCommandClient.getInstance().setLocalSurface(null, call);
            }
        }
    }

    private void checkForCallStateChange(Call call) {
        final int newCallState = call.getState();
        Log.d(this,"checkForCallStateChange: mIsVideoCall = " + mIsVideoCall
                + ", mCurrentCallState = "+ mCurrentCallState
                + ", newCallState = "+ newCallState);

        if(mCurrentCallState == newCallState){
            return;
        }

        if(mIsVideoCall){
            showVideoUi(call.getVideoDetailsState());
        }
    }

    private void updateCallCache(Call call) {
        if (call == null) {
            mCurrentVideoState = VideoProfile.STATE_AUDIO_ONLY;
            mCurrentCallState = Call.State.INVALID;
        } else {
            mCurrentVideoState = call.getVideoDetailsState();
            mCurrentCallState = call.getState();
        }
    }
    /* @} */

    /*SPRD: add for 871916@{*/
    public boolean isAudioCallDialing(Call call) {
        if (call == null) {
            Log.d(this, "isAudioCallDialing() call = null!!");
            return false;
        }
        return call.isRingToneOnAudioCall();
    }

    public boolean getAudioCallDialing() {
        return mIsAudioCallDialing;
    }
    /* @} */

    /**
     * Defines the VideoCallUI interactions.
     */
    public interface VideoCallUi extends Ui {
        void showVideoUi(boolean show);
        boolean isDisplayVideoSurfaceCreated();
        boolean isPreviewVideoSurfaceCreated();
        Surface getDisplayVideoSurface();
        Surface getPreviewVideoSurface();
        void setPreviewSize(int width, int height);
        void cleanupSurfaces();
        boolean isActivityRestart();
        // SPRD: VIDEO CALL FEATURE. FIXBUG390673
        boolean isVideoSurfacesInUse();
        void showVideoViews(boolean previewPaused, boolean showIncoming);
        void hideVideoViews();
    }

}
