package com.sprd.incallui;

import java.util.AbstractMap;
import java.util.List;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.android.incallui.CallCommandClient;
import com.android.incallui.CallList;
import com.android.incallui.InCallPresenter;
import com.android.incallui.Log;
import com.android.incallui.VideoCallPresenter;
import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.ICallCommandService;
import com.android.services.telephony.common.IVideoCallHandlerService;
import com.android.services.telephony.common.ISprdCallCommandService;

public class VideoCallHandlerService extends Service {

    private final static String TAG = VideoCallHandlerService.class.getSimpleName();

    private static final int ON_START = 1;
    private static final int ON_DESTROY = 2;
    private static final int LARGEST_MSG_ID = ON_DESTROY;

    private Handler mMainHandler;
    private Object mHandlerInitLock = new Object();
    private boolean mServiceStarted = false;
    private VideoCallPresenter mVideoCallPresenter;
    private static final int EVENT_VOLTE_CALL_PREVIEW_SURFACE = 10000;

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate");
        super.onCreate();

        synchronized(mHandlerInitLock) {
            if (mMainHandler == null) {
                mMainHandler = new MainHandler();
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");

        // onDestroy will get called when:
        // 1) there are no more calls
        // 2) the client (TeleService) crashes.
        //
        // Because onDestroy is not sequenced with calls to CallHandlerService binder,
        // we cannot know which is happening.
        // Thats okay since in both cases we want to end all calls and let the UI know it can tear
        // itself down when it's ready. Start the destruction sequence.
        mMainHandler.sendMessage(mMainHandler.obtainMessage(ON_DESTROY));
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "onUnbind");

        // Returning true here means we get called on rebind, which is a feature we do not need.
        // Return false so that all reconnections happen with a call to onBind().
        return false;
    }

    private final IVideoCallHandlerService.Stub mBinder = new IVideoCallHandlerService.Stub() {
        @Override
        public void startSprdCallService(ISprdCallCommandService service) {
            try {
                Log.d(TAG, "startSprdCallService: " + service.toString());
                mMainHandler.sendMessage(mMainHandler.obtainMessage(ON_START, service));
            } catch (Exception e) {
                Log.e(TAG, "Error processing setSprdCallCommandservice() call", e);
            }
        }

        @Override
        public void VTManagerEvent(Message message){
            try {
                onVTManagerEvent(message);
            } catch (Exception e) {
                Log.e(TAG, "Error processing setSprdCallCommandservice() call", e);
            }
        }
    };

    private class MainHandler extends Handler {
        MainHandler() {
            super(getApplicationContext().getMainLooper(), null, true);
        }

        @Override
        public void handleMessage(Message msg) {
            executeMessage(msg);
        }
    }

    private void executeMessage(Message msg) {
        if (msg.what > LARGEST_MSG_ID) {
            // If you got here, you may have added a new message and forgotten to
            // update LARGEST_MSG_ID
            Log.wtf(TAG, "Cannot handle message larger than LARGEST_MSG_ID.");
        }

        // If we are not initialized, ignore all messages except start up
        if (!mServiceStarted && msg.what != ON_START) {
            Log.i(TAG, "System not initialized.  Ignoring message: " + msg.what);
            return;
        }

        Log.d(TAG, "executeMessage " + msg.what);

        switch (msg.what) {
            case ON_START:
                doStart(msg);
                break;
            case ON_DESTROY:
                doStop();
                break;
            default:
                break;
        }
    }

    private void doStart(Message msg){
        if (mServiceStarted) {
            Log.i(TAG, "Starting a service before another one is completed");
            doStop();
        }
        Log.i(TAG, "Starting");
        ISprdCallCommandService service = (ISprdCallCommandService) msg.obj;
        CallCommandClient.getInstance().setSprdService(service);
        SprdCallCommandClient.getInstance().setService(service);

        mServiceStarted = true;
        mVideoCallPresenter = VideoCallPresenter.getInstance();
    }

    private void doStop() {
        Log.i(TAG, "doStop");

        if (!mServiceStarted) {
            return;
        }

        mServiceStarted = false;
    }

    private void onVTManagerEvent(Message message){
        if(message == null){
            Log.i(TAG, "onVTManagerEvent, message is null !");
            return;
        }
        Log.i(TAG, "onVTManagerEvent:"+message.what);
        switch (message.what) {
            case EVENT_VOLTE_CALL_PREVIEW_SURFACE:
            mVideoCallPresenter.onVTManagerEvent(message);
            break;
            /*case VtParameters.MEDIA_CALLEVENT_CAMERAOPEN:

                break;
            case VtParameters.MEDIA_CALLEVENT_CAMERACLOSE:

                break;
            case VtParameters.MEDIA_CALLEVENT_STRING:

                break;
            case VtParameters.MEDIA_CALLEVENT_CODEC_START:

                break;
            case VtParameters.MEDIA_CALLEVENT_CODEC_CLOSE:

                break;
            case VtParameters.MEDIA_CALLEVENT_CODEC_OPEN:

                break;
            case VtParameters.MEDIA_CALLEVENT_CODEC_SET_PARAM_ENCODER:

                break;
            case VtParameters.MEDIA_CALLEVENT_CODEC_SET_PARAM_DECODER:

                break;
            case VtParameters.MEDIA_CALLEVENT_MEDIA_START:

                break;
            case VtParameters.MEDIA_CALLEVENT_UPDATE_OPTION_MENU:
                break;
            case VtParameters.MEDIA_CALLEVENT_UPDATE_VIEW_TYPE:
                break;
            case VtParameters.MEDIA_CALLEVENT_UPDATE_VIEW_CONFIG:
                break;
            case VtParameters.MEDIA_CALLEVENT_CAMERA_OPEN_FAIL:
                break;
            case VtParameters.MEDIA_CALLEVENT_CAMERA_CLOSE_FAIL:
                break;
            case VtParameters.MEDIA_CALLEVENT_DELAYED_CREATE_CAMERA:
                break;
            case VtParameters.MEDIA_CALLEVENT_VTMANAGER_INIT:
                break;
            case VtParameters.MEDIA_CALLEVENT_VOLTE_MEDIA_CHANGE:
                VideoCallPresenter.getInstance().changeVolteCallMediaType();
                break;*/
            default:
                break;
        }
    }
}

