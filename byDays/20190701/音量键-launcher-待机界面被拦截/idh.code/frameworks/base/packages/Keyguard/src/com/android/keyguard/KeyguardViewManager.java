 /*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.keyguard;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import com.android.internal.policy.IKeyguardShowCallback;
import com.android.internal.widget.LockPatternUtils;
import com.android.keyguard.KeyguardSecurityModel.SecurityMode;
import com.sprd.keyguard.DialogFeatureBarHelper;
import com.sprd.keyguard.Utilities;

import android.app.StatusBarManager;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.DialogInterface.OnKeyListener;
import android.content.DialogInterface.OnShowListener;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.Parcelable;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Contacts;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Slog;
import android.util.SparseArray;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.widget.AbsoluteLayout.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
/* SPRD: bug268719 2014-01-19 can not set lockscreen wallpaper @{ */
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.app.admin.DevicePolicyManager;

import java.lang.reflect.InvocationTargetException;
/* SPRD: bug268719 2014-01-19 can not set lockscreen wallpaper @} */
/* Bug 622797 pikel UI @{ */
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

/* @} */
import android.content.Intent;

/**
 * Manages creating, showing, hiding and resetting the keyguard.  Calls back
 * via {@link KeyguardViewMediator.ViewMediatorCallback} to poke
 * the wake lock and report that the keyguard is done, which is in turn,
 * reported to this class by the current {@link KeyguardViewBase}.
 */
public class KeyguardViewManager {
    private final static boolean DEBUG = KeyguardViewMediator.DEBUG;
    private static String TAG = "KeyguardViewManager";
    public final static String IS_SWITCHING_USER = "is_switching_user";

    // Delay dismissing keyguard to allow animations to complete.
    private static final int HIDE_KEYGUARD_DELAY = 500;

    // Timeout used for keypresses
    static final int DIGIT_PRESS_WAKE_MILLIS = 5000;

    private final Context mContext;
    private final ViewManager mViewManager;
    private final KeyguardViewMediator.ViewMediatorCallback mViewMediatorCallback;

    private WindowManager.LayoutParams mWindowLayoutParams;
    private boolean mNeedsInput = false;

    private ViewManagerHost mKeyguardHost;
    private KeyguardHostView mKeyguardView;

    private boolean mScreenOn = false;
    private LockPatternUtils mLockPatternUtils;
    private int preKeyCode;
    private AlertDialog myAlertDialog;
    //private CustomDialog myCustomDialog;
    private AlertDialog mSecureLockDialog;

    // SPRD: Modify 20131230 Spreadst of Bug 262132 modify UUI lockscreen policy
    static boolean mIsShowUUILock = true;
    /* SPRD: 642217 @{ */
   // private static final boolean ENABLE_FLASHLIGHT_BY_CENTER_KEY = SystemProperties.getBoolean("ro.home.flashlight.centerkey", true);
    private static final String SOS_BROADCAST = "android.intent.action.SOS";
    private PowerManager mPowerManager ;
    /* @} */
    protected TextView mPasswordEntry;
    private BroadcastReceiver mAlarmReceiver = null;
    private SecurityAlertDialog dialog;
    private int mSubId = -1;
    private boolean is_dialog_hidden=false;
    // UNISOC: add for bug879650
    private EmergencyCallDialog mEmergencyDialog;

    private KeyguardUpdateMonitorCallback mBackgroundChanger = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onSetBackground(Bitmap bmp) {
            /* SPRD: 633910 @{ */
            if(bmp == null)
            {
                Log.d(TAG,"KeyguardViewManager setCustomBackground albumart_unknown ");
                bmp = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.albumart_unknown);
            }
            /* @} */
            mKeyguardHost.setCustomBackground(bmp != null ?new BitmapDrawable(mContext.getResources(), bmp): mCustomWallPaper != null ? mCustomWallPaper : null);
            updateShowWallpaper(bmp == null ? mCustomWallPaper == null : false);
        }
    };

    public interface ShowListener {
        void onShown(IBinder windowToken);
    };

    /**
     * @param context Used to create views.
     * @param viewManager Keyguard will be attached to this.
     * @param callback Used to notify of changes.
     * @param lockPatternUtils
     */
    public KeyguardViewManager(Context context, ViewManager viewManager,
            KeyguardViewMediator.ViewMediatorCallback callback,
            LockPatternUtils lockPatternUtils) {
        mContext = context;
        mViewManager = viewManager;
        mViewMediatorCallback = callback;
        mLockPatternUtils = lockPatternUtils;
    }

    /**
     * Show the keyguard.  Will handle creating and attaching to the view manager
     * lazily.
     */ 
    public synchronized void show(Bundle options) {
        if (DEBUG) Log.d(TAG, "show(); mKeyguardView==" + mKeyguardView);

        boolean enableScreenRotation = shouldEnableScreenRotation();

        maybeCreateKeyguardLocked(enableScreenRotation, false, options);
        maybeEnableScreenRotation(enableScreenRotation);

        // Disable common aspects of the system/status/navigation bars that are not appropriate or
        // useful on any keyguard screen but can be re-shown by dialogs or SHOW_WHEN_LOCKED
        // activities. Other disabled bits are handled by the KeyguardViewMediator talking
        // directly to the status bar service.
        int visFlags = View.STATUS_BAR_DISABLE_HOME;
        if (shouldEnableTranslucentDecor()) {
            mWindowLayoutParams.flags |= WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                                       | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION;
        }
        if (DEBUG) Log.v(TAG, "show:setSystemUiVisibility(" + Integer.toHexString(visFlags)+")");
        mKeyguardHost.setSystemUiVisibility(visFlags);

        mViewManager.updateViewLayout(mKeyguardHost, mWindowLayoutParams);
        mKeyguardHost.setVisibility(View.VISIBLE);
        mKeyguardView.show();
        mKeyguardView.requestFocus();
    }

    private boolean shouldEnableScreenRotation() {
        Resources res = mContext.getResources();
        return SystemProperties.getBoolean("lockscreen.rot_override",false)
                || res.getBoolean(R.bool.config_enableLockScreenRotation);
    }

    private boolean shouldEnableTranslucentDecor() {
        Resources res = mContext.getResources();
        return res.getBoolean(R.bool.config_enableLockScreenTranslucentDecor);
    }

    class ViewManagerHost extends FrameLayout {
        private static final int BACKGROUND_COLOR = 0x70000000;

        private Drawable mCustomBackground;

        // This is a faster way to draw the background on devices without hardware acceleration
        private final Drawable mBackgroundDrawable = new Drawable() {
            @Override
            public void draw(Canvas canvas) {
                if (mCustomBackground != null) {
                    final Rect bounds = mCustomBackground.getBounds();
                    final int vWidth = getWidth();
                    final int vHeight = getHeight();

                    final int restore = canvas.save();
                    canvas.translate(-(bounds.width() - vWidth) / 2,
                            -(bounds.height() - vHeight) / 2);
                    mCustomBackground.draw(canvas);
                    canvas.restoreToCount(restore);
                } else {
                    canvas.drawColor(BACKGROUND_COLOR, PorterDuff.Mode.SRC);
                }
            }

            @Override
            public void setAlpha(int alpha) {
            }

            @Override
            public void setColorFilter(ColorFilter cf) {
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        };

        public ViewManagerHost(Context context) {
            super(context);
            setBackground(mBackgroundDrawable);
        }

        public void setCustomBackground(Drawable d) {
            mCustomBackground = d;
            if (d != null) {
                d.setColorFilter(BACKGROUND_COLOR, PorterDuff.Mode.SRC_OVER);
            }
            computeCustomBackgroundBounds();
            invalidate();
        }

        private void computeCustomBackgroundBounds() {
            if (mCustomBackground == null) return; // Nothing to do

            /* SPRD: bug268719 2014-01-19 can not set lockscreen wallpaper @{ */
            // the lockscreen will show transparent and user only see the wallpaper of system
            // but not the lockscreen wallpaper when device power on and showed the lockscreen.
            // because the isLaidOut allways return false untill you unlock once.
            // so we add condition getHeight, if onSizeChanged called, widht and height is ok,
            // then we can calc the bounds of the custom drawable.
            if ((!isLaidOut()) && (getHeight() <= 0)) {
                return; // We'll do this later
            }
            /* SPRD: bug268719 2014-01-19 can not set lockscreen wallpaper @} */

            final int bgWidth = mCustomBackground.getIntrinsicWidth();
            final int bgHeight = mCustomBackground.getIntrinsicHeight();
            final int vWidth = getWidth();
            final int vHeight = getHeight();

            final float bgAspect = (float) bgWidth / bgHeight;
            final float vAspect = (float) vWidth / vHeight;

            if (bgAspect > vAspect) {
                mCustomBackground.setBounds(0, 0, (int) (vHeight * bgAspect), vHeight);
            } else {
                mCustomBackground.setBounds(0, 0, vWidth, (int) (vWidth / bgAspect));
            }
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            computeCustomBackgroundBounds();
        }

        @Override
        protected void onConfigurationChanged(Configuration newConfig) {
            super.onConfigurationChanged(newConfig);
            /* SPRD: Modify for Bug 352623 add log and delete extra code @{ */
            if (DEBUG) Log.d(TAG, "onConfigurationChanged");
            if (mKeyguardHost.getVisibility() == View.VISIBLE) {
                // only propagate configuration messages if we're currently showing
                maybeCreateKeyguardLocked(shouldEnableScreenRotation(), true, null);
            } else {
                if (DEBUG) Log.d(TAG, "onConfigurationChanged: view not visible");
            }
            /* @} */
        }
		//BUG #47213  add by maoyufeng 20190429 begin
        private void showtipDialog(int tip) {
            final CustomDialog  myCustomDialog = new CustomDialog.Builder(mContext).create();
            myCustomDialog.setTip(tip,mContext);
            Window window = myCustomDialog.getWindow();
            if (!(mContext instanceof Activity)) {
                window.setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
            }
            WindowManager.LayoutParams wlp = window.getAttributes();
            wlp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(wlp);
            window.setGravity(Gravity.CENTER);//modify by maoyufeng 20190408
            //agenew:BUG #47490 add by lizhenye @20190506{
            myCustomDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if(myCustomDialog.isShowing() && event.getAction() == KeyEvent.ACTION_UP){
                        myCustomDialog.dismiss();
                        if(keyCode==KeyEvent.KEYCODE_MENU){
                            showUnlockDialog();
                        }
                    }
                    return true;
            }
            });
            //@}
            myCustomDialog.show();
            final int SLEEP_TIME = 3000;
            new Thread((new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(SLEEP_TIME);
                        myCustomDialog.dismiss();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            })).start();

        }
        private boolean isShowLockTip =true;
        private boolean mIsDown;//BUG #47752 add by maoyufeng 20190515
        private long mPresstime;//BUG #47833 add by maoyufeng 20190515
    //     @Override
    //     public boolean dispatchKeyEvent(KeyEvent event) {
    //         if (mKeyguardView != null) {
				// //add by maoyufeng 20190419 begin
    //             if(event.isLongPress() && event.getKeyCode() == KeyEvent.KEYCODE_STAR){
    //                 if (mKeyguardView != null) 
    //                     mKeyguardView.handleStarKey();
    //                 return true;
    //             }
				// //add by maoyufeng 20190419 end
    //             /* SPRD: modify for Bug 779403 & 639505 & 628416 & 820833 @{ */
    //             KeyguardSimPinView pin = (KeyguardSimPinView) mKeyguardView.findViewById(
    //                     R.id.keyguard_sim_pin_view);
    //             KeyguardSimPukView puk = (KeyguardSimPukView) mKeyguardView.findViewById(
    //                     R.id.keyguard_sim_puk_view);
    //             // Always process back and menu keys, regardless of focus
    //             if (event.getAction() == KeyEvent.ACTION_DOWN) {
    //                 int keyCode = event.getKeyCode();
    //                 preKeyCode = keyCode;
    //                 Log.d(TAG, " pinView : " + pin + "; pukView : " + puk + ", keyCode = " + keyCode);
    //                 if (pin == null && puk == null) {
    //                     if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
    //                         boolean consume = authLongClick(event);
				// 			 //modify by maoyufeng 20190418 begin 
    //                         // if (!consume) {
    //                         //     autushortClick(event);
    //                         // }
    //                         return consume;
				// 			//modify by maoyufeng 20190418 end 
    //                     } else if (keyCode == KeyEvent.KEYCODE_MENU) {
    //                             //|| keyCode == KeyEvent.KEYCODE_BACK) {modify by maoyufeng 2019041
    //                         autushortClick(event);
    //                         return true;
    //                     } else if ((keyCode == KeyEvent.KEYCODE_0)
    //                             || (keyCode == KeyEvent.KEYCODE_POUND)) {
    //                         autuLongClickForQuickentry(event);
    //                         return true;
    //                     }
    //                      UNISOC: add for bug879650 @{ 
    //                     if (keyCode == KeyEvent.KEYCODE_1 || keyCode == KeyEvent.KEYCODE_9) {
    //                         showEmergencyCallDialog(keyCode);
    //                     }
    //                     /* @} */
    //                 }
    //                 if (keyCode == KeyEvent.KEYCODE_STAR){
    //                     autuLongClickForQuickentry(event);
    //                     return true;
    //                 }
    //                 /* @} */
    //                 /* SPRD: Modify 20131126 Spreadst of Bug 244476 unlock lockscreen when clcik MENU @{ 
    //                 else if (keyCode == KeyEvent.KEYCODE_MENU && mKeyguardView.handleMenuKey()) {
    //                     return true;
    //                 } */
    //                 /* @} */
    //             }
    //             // Always process media keys, regardless of focus
    //             /* SPRD: Bug 244476 requestFocus if keyguard View has no focus @{ */
    //             if (!this.hasFocus()) {
    //                 if (DEBUG) {
    //                   Log.d(TAG, "keyguardViewManager has no focus");
    //                 }
    //                 requestFocus();
    //             }
    //             /* @} */
    //             if (mKeyguardView.dispatchKeyEvent(event)) {
    //                 return true;
    //             }
    //             /* SPRD: Modify 20140214 Spreadst of Bug 279392 delete 2 chars for password lockscreen @{ */
    //             if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
    //                 return true;
    //             }
    //             /* @} */

    //             EmergencyButton button = (EmergencyButton) findViewById(R.id.emergency_call_button);
    //             if ((pin != null || puk != null)
    //                     && event.getKeyCode() == KeyEvent.KEYCODE_MENU
    //                     && event.getAction() == KeyEvent.ACTION_UP
    //                     && button.canEmergencyCall()) {
    //                 button.takeEmergencyCallAction();
    //                 return true;
    //             }
    //         }
    //         /* SPRD: 632292  @{ */
    //         //return super.dispatchKeyEvent(event);
    //         return true;
    //         /* @} */
    //     }
    // }
	    @Override
        public boolean dispatchKeyEvent(KeyEvent event) {
            if (mKeyguardView != null) {
				//add by maoyufeng 20190419 begin
                if(event.isLongPress() && event.getKeyCode() == KeyEvent.KEYCODE_STAR){
                    if (mKeyguardView != null) 
                        mKeyguardView.handleStarKey();
                    return true;
                }
				//add by maoyufeng 20190419 end
                /* SPRD: modify for Bug 779403 & 639505 & 628416 & 820833 @{ */
                KeyguardSimPinView pin = (KeyguardSimPinView) mKeyguardView.findViewById(
                        R.id.keyguard_sim_pin_view);
                KeyguardSimPukView puk = (KeyguardSimPukView) mKeyguardView.findViewById(
                        R.id.keyguard_sim_puk_view);
                // Always process back and menu keys, regardless of focus
                boolean isHandle = false ;
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if(event.getKeyCode() != 0)
                        mIsDown = true;//BUG #47752 add by maoyufeng 20190515
                    int keyCode = event.getKeyCode();
                    preKeyCode = keyCode;
                    Log.d(TAG, " pinView : " + pin + "; pukView : " + puk + ", keyCode = " + keyCode);
                    if (pin == null && puk == null) {
						//BUG #47833 add by maoyufeng 20190515 begin
                        if(keyCode == KeyEvent.KEYCODE_POWER && mPresstime == 0){
                            mPresstime = SystemClock.uptimeMillis();
                        }
						//BUG #47833 add by maoyufeng 20190515 end
                        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
                            boolean consume = authLongClick(event);
							 //modify by maoyufeng 20190418 begin 
                            // if (!consume) {
                            //     autushortClick(event);
                            // }
                            isHandle = consume;
                            if(isHandle)
                             isShowLockTip = !isHandle;
                            if(isHandle)
                                return isHandle;
							//modify by maoyufeng 20190418 end 
                        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
                                //|| keyCode == KeyEvent.KEYCODE_BACK) {modify by maoyufeng 2019041
                            autushortClick(event);
                            isShowLockTip = false;
                            return true;
                        } else if ((keyCode == KeyEvent.KEYCODE_0)
                                || (keyCode == KeyEvent.KEYCODE_POUND)) {
                            isHandle = autuLongClickForQuickentry(event);
                            if (isHandle)
                             isShowLockTip = !isHandle;
                            if(isHandle)
                                return isHandle;
                        }
                        /* UNISOC: add for bug879650 @{ */
                        if (keyCode == KeyEvent.KEYCODE_1 || keyCode == KeyEvent.KEYCODE_9) {
                            isHandle = showEmergencyCallDialog(keyCode);
                            if( isHandle)
                             isShowLockTip = !isHandle;
                            if(isHandle)
                                return isHandle;
                        }
                        /* @} */
                    }
                    if (keyCode == KeyEvent.KEYCODE_STAR){
                        isHandle = autuLongClickForQuickentry(event);
                        if(isHandle)
                             isShowLockTip = !isHandle;
                            if(isHandle)
                                return isHandle;
                    }
                    if(keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                        isHandle =true;
                    if(isHandle)
                        isShowLockTip = !isHandle;
                    /* @} */
                    /* SPRD: Modify 20131126 Spreadst of Bug 244476 unlock lockscreen when clcik MENU @{ 
                    else if (keyCode == KeyEvent.KEYCODE_MENU && mKeyguardView.handleMenuKey()) {
                        return true;
                    } */
                    /* @} */
                }else if(event.getAction() == KeyEvent.ACTION_UP){
                    if(mIsDown && isShowLockTip && pin == null && puk == null && event.getKeyCode() != 0 && isShowTipDialog()){//BUG #47752 add by maoyufeng 20190515
                        /* change by BIRD@hujingcheng 20190605 function key e516 no tips in keyguard view */
                        int keyCode=event.getKeyCode();
                        if(keyCode == KeyEvent.KEYCODE_POWER){
                            if(mPresstime != 0 && SystemClock.uptimeMillis() - mPresstime > 500)
                                showtipDialog(R.string.agenew_keyguard_lock_power_tip);
                            mPresstime = 0;
                        }else
                            if(!(keyCode==KeyEvent.KEYCODE_VOLUME_DOWN || keyCode==KeyEvent.KEYCODE_VOLUME_UP)){
                                showtipDialog(R.string.agenew_keyguard_lock_tip);
                            }
                        /* change by BIRD@hujingcheng 20190605 end */
                    }
                    mIsDown =false;//BUG #47752 add by maoyufeng 20190515
                    isShowLockTip =true;
                }
                // Always process media keys, regardless of focus
                /* SPRD: Bug 244476 requestFocus if keyguard View has no focus @{ */
                if (!this.hasFocus()) {
                    if (DEBUG) {
                      Log.d(TAG, "keyguardViewManager has no focus");
                    }
                    requestFocus();
                }
                /* @} */
                if (mKeyguardView.dispatchKeyEvent(event)) {
                    return true;
                }
                /* SPRD: Modify 20140214 Spreadst of Bug 279392 delete 2 chars for password lockscreen @{ */
                if (event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                    return true;
                }
                /* @} */

                EmergencyButton button = (EmergencyButton) findViewById(R.id.emergency_call_button);
                if ((pin != null || puk != null)
                        && event.getKeyCode() == KeyEvent.KEYCODE_MENU
                        && event.getAction() == KeyEvent.ACTION_UP
                        && button.canEmergencyCall()) {
                    button.takeEmergencyCallAction();
                    return true;
                }
            }
            /* SPRD: 632292  @{ */
            //return super.dispatchKeyEvent(event);
            return true;
            /* @} */
        }
    }

    /* Bug 622797 pikel UI @{ */
    private long preClickTime = 0;
    private static final int DURATION_TIME = 1000;
    private boolean authLongClick(KeyEvent event) {
		//modify by maoyufeng 20190418 begin 
        if(event.isLongPress()){
            startFlashlight();
            return true;
        }
        // int count = event.getRepeatCount();
        // if (count == 0) {
        //     preClickTime = new Date().getTime();
        // }
        // long curTimes = new Date().getTime();
        // long dur = curTimes - preClickTime;

        // if(preClickTime > 0 && (curTimes - preClickTime) > DURATION_TIME ){
        //     if (event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER){
        //         Log.d(TAG, "authLongClick KEYCODE_DPAD_CENTER");
        //         if (!ENABLE_FLASHLIGHT_BY_CENTER_KEY) {
        //              startFlashlight();
        //             return true;
        //         }else{
        //             startFlashlight();
        //             return true;
        //         }
        //     }
        //     preClickTime = new Date().getTime();
        // }
        return false;
    }
    private static void startFlashlight() {
        FlashlightController.switchFlashlight();
	}
	//add by maoyufeng 20190430 begin 
    public boolean isShowTipDialog(){
          return !(mLockPatternUtils.isSecure() && (mLockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC
                          || mLockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_NUMERIC
                          || mLockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC
                          || mLockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_BIOMETRIC_WEAK
                          || mLockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_COMPLEX));
    }
	//add by maoyufeng 20190430 end
    //}
	//modify by maoyufeng 20190418 end 
   	//BUG #47213  add by maoyufeng 20190429 void to boolean
    private boolean autuLongClickForQuickentry(KeyEvent event) {
        int count = event.getRepeatCount();
        if (count == 0) {
            preClickTime = new Date().getTime();
        }
        long curTimes = new Date().getTime();
        long dur = curTimes - preClickTime;

        if(preClickTime > 0 && (curTimes - preClickTime) > DURATION_TIME ){
            if (event.getKeyCode() == KeyEvent.KEYCODE_POUND){
                Log.d(TAG, "authLongClick KEYCODE_POUND");
                //showUnlockDialog();
                if (mLockPatternUtils.isSecure() && (mLockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC
                          || mLockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_NUMERIC
                          || mLockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC
                          || mLockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_BIOMETRIC_WEAK
                          || mLockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_COMPLEX)){
                          showSecureUnlockDialog();
                    }else {
                          showUnlockDialog();
                    }
                    return true;		//BUG #47213  add by maoyufeng 20190429
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_0){
                Log.d(TAG, "authLongClick KEYCODE_0");
                Utilities.switchFlashlight();
                return true;		//BUG #47213  add by maoyufeng 20190429 
            }
            if (event.getKeyCode() == KeyEvent.KEYCODE_STAR){
                Log.d(TAG, "authLongClick KEYCODE_STAR");
                mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                mPowerManager.goToSleep(SystemClock.uptimeMillis());
                 return true;		//BUG #47213  add by maoyufeng 20190429 
            }
            preClickTime = new Date().getTime();
        }
         return false;		//BUG #47213  add by maoyufeng 20190429 
    }
   	//BUG #47213  add by maoyufeng 20190429 void/boolean
    private boolean autushortClick(KeyEvent event) {
        int count = event.getRepeatCount();
        if (count == 0) {
            preClickTime = new Date().getTime();
        }
        long curTimes = new Date().getTime();
        long dur = curTimes - preClickTime;
        if(preClickTime > 0 && (curTimes - preClickTime) <= DURATION_TIME ){
            if (event.getKeyCode() == KeyEvent.KEYCODE_MENU || event.getKeyCode() == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER){
                   if (mLockPatternUtils.isSecure() && (mLockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC
                        || mLockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_NUMERIC
                        || mLockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC
                        || mLockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_BIOMETRIC_WEAK
                        || mLockPatternUtils.getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_COMPLEX)){
                        showSecureUnlockDialog();
                        return true;		//BUG #47213  add by maoyufeng 20190429 
                  }else {
                        showUnlockDialog();
                        return true;		//BUG #47213  add by maoyufeng 20190429 
                  }
            }
            preClickTime = new Date().getTime();
        }
        return false;  		//BUG #47213  add by maoyufeng 20190429 
    }

    /* UNISOC: add for bug879650 @{ */
	//BUG #47213  add by maoyufeng 20190429 void/boolean
    private boolean showEmergencyCallDialog(int keyCode) {
        final String emergencyNum = mContext.getResources().getString(R.string.emergency_dial);
        final EmergencyCallDialog.Builder builder = new EmergencyCallDialog.Builder(mContext);
        mEmergencyDialog = builder.create();
        DialogFeatureBarHelper dialogFeatureBar = new DialogFeatureBarHelper(mEmergencyDialog,
                mContext);
        final TextView optionView = (TextView) dialogFeatureBar.getOptionsKeyView();
        final TextView centerView = (TextView) dialogFeatureBar.getCenterKeyView();
        final TextView backView = (TextView) dialogFeatureBar.getBackKeyView();
        optionView.setVisibility(View.GONE);
        builder.setEditText(keyCode == KeyEvent.KEYCODE_1 ? "1" : "9");
        builder.setSelection(1);
        TextWatcher textWatcher = new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {
            }
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
            }
            public void afterTextChanged(Editable input) {
                Log.d(TAG, "input : " + input.toString());
                if (!emergencyNum.contains(input.toString())) {
                    mEmergencyDialog.dismiss();
                }
                if (TextUtils.isEmpty(input.toString())) {
                    backView.setText(R.string.default_feature_bar_back);
                    centerView.setVisibility(View.INVISIBLE);
                } else {
                    backView.setText(R.string.kg_sim_pin_delete);
                    centerView.setVisibility(View.VISIBLE);
                }
            }
        };
        builder.setTextWatcher(textWatcher);
        Window window = mEmergencyDialog.getWindow();
        if (!(mContext instanceof Activity)) {
           window.setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        }
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.width = WindowManager.LayoutParams.MATCH_PARENT;
        wlp.height = WindowManager.LayoutParams.MATCH_PARENT;
        mEmergencyDialog.getWindow().setAttributes(wlp);
        mEmergencyDialog.getWindow().setGravity(Gravity.TOP);
        mEmergencyDialog.show();
        if (TextUtils.isEmpty(builder.getEditText())) {
            backView.setText(R.string.default_feature_bar_back);
            centerView.setVisibility(View.INVISIBLE);
        } else {
            backView.setText(R.string.kg_sim_pin_delete);
            centerView.setVisibility(View.VISIBLE);
        }
        mEmergencyDialog.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keycode, KeyEvent event) {
                Log.d(TAG, "emergency dialog key change " + event);
                String num = builder.getEditText();
                if (mEmergencyDialog.isShowing()) {
                    if(event.getAction() == KeyEvent.ACTION_DOWN
                            && (keycode == KeyEvent.KEYCODE_DEL
                            || keycode == KeyEvent.KEYCODE_BACK)) {
                        if(num != null && num.length() > 0) {
                            builder.setEditText(num.substring(0, num.length() - 1));
                            builder.setSelection(num.length() - 1);
                        } else {
                            mEmergencyDialog.dismiss();
                        }
                        return true;
                    } else if (!TextUtils.isEmpty(num)
                            && (event.getKeyCode() == KeyEvent.KEYCODE_CALL
                                || event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER)
                            && event.getAction() == KeyEvent.ACTION_UP) {
                        Intent intent = new Intent();
                        intent.setClassName("com.android.phone", "com.android.phone.EmergencyCall");
                        intent.putExtra("emergency_dial_number", num);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                        mContext.startActivity(intent);
                        mEmergencyDialog.dismiss();
                        return true;
                    }
                }
                return false;
            }
        });
		//BUG #47213  add by maoyufeng 20190429 begin
        String input =String.valueOf((char)((keyCode - KeyEvent.KEYCODE_0)+'0'));
        if(emergencyNum.contains(input)){
            return true;
        }
        return false;
		//BUG #47213  add by maoyufeng 20190429 end
    }
    /* @} */

    private void showSecureUnlockDialog() {
        final SecurityAlertDialog.Builder builder = new SecurityAlertDialog.Builder(mContext);
       // final SecurityAlertDialog dialog = builder.create();
        dialog = builder.create();
        if (mLockPatternUtils.isSecure() && mLockPatternUtils
             .getKeyguardStoredPasswordQuality() == DevicePolicyManager.PASSWORD_QUALITY_NUMERIC) {
             builder.setInputType();
             builder.setPasswordTitle();
        }

        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        if (!(mContext instanceof Activity)) {
             dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        }
        WindowManager.LayoutParams wlp = dialog.getWindow().getAttributes();
        wlp.width = WindowManager.LayoutParams.MATCH_PARENT;
        wlp.height = WindowManager.LayoutParams.MATCH_PARENT;
        dialog.getWindow().setAttributes(wlp);
        dialog.getWindow().setGravity(Gravity.CENTER);
        final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                Log.d(TAG,"onCallStateChanged state="+state+" isShowing()="+isShowing());
                if(!isShowing()){
                   Log.d(TAG,"onCallStateChanged keyhostview is not showing and return");
                   return;
                }
                switch(state){
                case TelephonyManager.CALL_STATE_IDLE:
                        dialog.show();
                        break;
                case TelephonyManager.CALL_STATE_RINGING:
                        if (dialog != null) {
                                if (state != TelephonyManager.CALL_STATE_IDLE) {
                                       final int SLEEP_TIME = 2000;
                                       new Thread((new Runnable() {
                                               public void run() {
                                                   try {
                                                       Thread.sleep(SLEEP_TIME);
                                                       dialog.hide();
                                                   } catch (Exception e) {
                                                       e.printStackTrace();
                                                   }
                                               }

                                        })).start();
                                }
                        }
                        break;
                default:
                        break;
                }
            }
        };
        final TelephonyManager mTelephonyManager = (TelephonyManager) mContext
            .getSystemService(Context.TELEPHONY_SERVICE);
        dialog.setOnShowListener(new OnShowListener(){
            @Override
            public void onShow(DialogInterface arg0) {
                if (mPhoneStateListener != null && dialog.isShowing()) {
                    Log.d(TAG, "PhoneStateListener.LISTEN_CALL_STATE");
                    mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                }
                Log.d(TAG, "SecurityDialog is showing");
                registerAlarmDialogReceiver();
            }
        });
        dialog.show();
        //dialog.setCancelable(false);
        dialog.setOnDismissListener(new OnDismissListener(){
            @Override
            public void onDismiss(DialogInterface arg0) {
                Log.d(TAG, "onDismiss");
                if ((mPhoneStateListener != null) && !(dialog.isShowing())) {
                    mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
                }
            }
        });
        dialog.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keycode, KeyEvent event) {
                String pwd = builder.getEditText();
                if (dialog.isShowing() && event.getAction() == KeyEvent.ACTION_DOWN) {
                    builder.setTips("");
                    if(keycode == KeyEvent.KEYCODE_DEL || keycode == KeyEvent.KEYCODE_BACK) {
                        if(pwd != null && pwd.length() > 0) {
                            builder.setEditText(pwd.substring(0, pwd.length() - 1));
                            builder.setSelection(pwd.length() - 1);
                        } else {
                            //dialog.hide();
                            dialog.setCancelable(false);
                        }
                        return true;
                    }else if(keycode == KeyEvent.KEYCODE_POUND){
                        //for bug874358 fix password Editbox will display "#" when long press # first time after reboot
                        builder.setEditText("");
                        return true;
                    }
                }
                return false;
            }
        });

        builder.setOnOKClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String lockEntry = builder.getEditText();
                int securityPassword = 0;
                if (mKeyguardView != null) {
                     securityPassword = mKeyguardView.getCurrentSecurityView(lockEntry);
                }
                Log.d(TAG, "securityPassword =  "+securityPassword);
                if (securityPassword == 1) {
                     String securityPasswordTips1 = mContext.getResources().getString(R.string.security_password_pass_tips);
                     builder.setTips(securityPasswordTips1);
                     final int SLEEP_TIME = 150;
                     new Thread((new Runnable() {
                        public void run() {
                            try {
                                Thread.sleep(SLEEP_TIME);
                                dialog.dismiss();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                     })).start();
                     Log.d(TAG, "securityPassword = 1,preKeyCode = "+preKeyCode);
			//BUG #47206  remove by maoyufeng 20190430 begin		 
          //            if (preKeyCode == KeyEvent.KEYCODE_MENU) {
          //                Intent intent = new Intent();
          //                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          //                /*ComponentName cpn = new ComponentName("com.sprd.simple.launcher",
          //                   "com.sprd.classichome.family.FamilyActivity");*/
					     // ComponentName cpn = new ComponentName("com.android.dialer","com.android.dialer.DialtactsActivity"); //add by guquanding for bug leftkey unlock loop
          //                intent.setComponent(cpn);
          //                mContext.startActivity(intent);
          //             }else if (preKeyCode == KeyEvent.KEYCODE_BACK) {
          //                Intent intent = new Intent();
          //                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          //                intent.setAction("android.media.action.STILL_IMAGE_CAMERA");
          //                mContext.startActivity(intent);
          //             }else if (preKeyCode == KeyEvent.KEYCODE_POUND) {
          //                try {
          //                     Class.forName("android.app.StatusBarManager")
          //                     .getMethod("expandNotificationsPanel")
          //                     .invoke(mContext.getSystemService("statusbar"));
          //                 } catch (Exception e) {
          //                    e.printStackTrace();
          //                 }
          //              }
		  //BUG #47206  remove by maoyufeng 20190430 
                } else if (securityPassword == 2) {
                      String securityPasswordTips2 = mContext.getResources().getString(R.string.security_password_wrong_tips);
                      builder.setTips(securityPasswordTips2);
                      builder.clearEditText();
                } else if (securityPassword == 3) {
                      builder.clearEditText();
                      builder.setEditable(false);
                      builder.setButtonOkEditable(false);
                      builder.setButtonCancelEditable(false);
                      String securityPasswordTips = mContext.getResources().getString(R.string.security_password_tips);
                      builder.setTips(securityPasswordTips);
                      new Timer().schedule(new TimerTask() {
                        int seconds = 30;
                        @Override
                        public void run() {
                          seconds--;
                          if(mKeyguardView != null){
                            mKeyguardView.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (seconds == 0) {
                                        builder.setTips("");
                                        builder.setEditable(true);
                                        builder.setButtonOkEditable(true);
                                        builder.setButtonCancelEditable(true);
                                        cancel();
                                        return;
                                    }
                                    String securityPasswordTips3;
                                    if(seconds<=1){
                                        securityPasswordTips3 = mContext.getResources().getString(R.string.security_password_pre_tips) 
                                                                   +" "
                                                                   + seconds
                                                                   +" "
                                                                   + mContext.getResources().getString(R.string.security_password_aft_tips,"second");
                                     }else{
                                        securityPasswordTips3 = mContext.getResources().getString(R.string.security_password_pre_tips)
                                                                  +" "
                                                                  + seconds
                                                                  +" "
                                                                  + mContext.getResources().getString(R.string.security_password_aft_tips,"seconds");


                                   }
                                    builder.setTips(securityPasswordTips3);
                                }
                            });}
                        }
                     }, 0, 1000);
                }
            }
        });
        builder.setOnCancelClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                      dialog.dismiss();
            }
        });
        }


    /**
     * register receiver for Alarm or Timer Tick
     */
    private void registerAlarmDialogReceiver() {
        Log.d(TAG, "registerAlarmDialogReceiver  mReceiver="+mAlarmReceiver);
        if (mAlarmReceiver == null) {
            mAlarmReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if ((intent.getAction().equals("com.android.deskclock.ALARM_ALERT"))
                        ||(intent.getAction().equals("com.android.deskclock.TIMER_ALERT"))){
                        Log.d(TAG, "onReceive ALARM_ALERT or  TIMER_ALERT");
                        if ((dialog != null) &&(dialog.isShowing())){
                             final int SLEEP_TIME = 700;
                             new Thread((new Runnable() {
                                 public void run() {
                                     try {
                                         Thread.sleep(SLEEP_TIME);
                                         dialog.hide();
                                         is_dialog_hidden=true;
                                         unregisterAlarmDialogReceiver();
                                     } catch (Exception e) {
                                         e.printStackTrace();
                                     }
                                 }
                          })).start();
                        }
                    }
                    if ((intent.getAction()
                        .equals("com.android.deskclock.ALARM_DONE"))
                        || (intent.getAction()
                        .equals("notification_for_keyguard_to_cancel"))) {
                        Log.d(TAG,"onReceive ALARM_DONE or notification_for_keyguard_to_cancel");
                        if (isShowing()&&is_dialog_hidden) {
                            Log.d(TAG,"to show dialog");
                            dialog.show();
                        }
                    }
              }
            };

            final IntentFilter filter = new IntentFilter();
            filter.addAction("com.android.deskclock.ALARM_ALERT");
            filter.addAction("com.android.deskclock.TIMER_ALERT");
            filter.addAction("com.android.deskclock.ALARM_DONE");
            filter.addAction("notification_for_keyguard_to_cancel");
            mContext.registerReceiver(mAlarmReceiver, filter);
            Log.d(TAG, "registerAlarmDialogReceiver  registered");
        }
    }

    private void unregisterAlarmDialogReceiver() {
        if(mAlarmReceiver != null){
            mContext.unregisterReceiver(mAlarmReceiver);
            mAlarmReceiver = null;
        }
    }
    private void showUnlockDialog() {
        final CustomDialog  myCustomDialog = new CustomDialog.Builder(mContext)
        .create();

        myCustomDialog.setOnKeyListener(new OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keycode, KeyEvent event) {
                if (myCustomDialog.isShowing() && event.getKeyCode() == KeyEvent.KEYCODE_STAR){
                    myCustomDialog.dismiss();
                    if (mKeyguardView != null) mKeyguardView.handleStarKey();
					//modify by maoyufeng 2019041 begin
          //           if (preKeyCode == KeyEvent.KEYCODE_MENU) {
          //               Intent intent = new Intent();
          //               intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          //              // ComponentName cpn = new ComponentName("com.sprd.simple.launcher", "com.sprd.classichome.family.FamilyActivity");
					     // ComponentName cpn = new ComponentName("com.android.dialer","com.android.dialer.DialtactsActivity"); //add by guquanding for bug leftkey unlock loop
          //               intent.setComponent(cpn);
          //               mContext.startActivity(intent);
          //           } else if (preKeyCode == KeyEvent.KEYCODE_BACK) {
          //               Intent intent = new Intent();
          //               intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          //               intent.setAction("android.media.action.STILL_IMAGE_CAMERA");
          //               mContext.startActivity(intent);
          //           } else if (preKeyCode == KeyEvent.KEYCODE_POUND) {
          //               try {
          //                   Class.forName("android.app.StatusBarManager")
          //                   .getMethod("expandNotificationsPanel")
          //                   .invoke(mContext.getSystemService("statusbar"));
          //               } catch (Exception e) {
          //                  e.printStackTrace();
          //               }
          //           }
		 // modify by maoyufeng 2019041 end
                }
                return true;
            }
        });
         Window window = myCustomDialog.getWindow();
         if (!(mContext instanceof Activity)) {
            window.setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
         }
         WindowManager.LayoutParams wlp = window.getAttributes();
		 // wlp.width = WindowManager.LayoutParams.MATCH_PARENT;
         wlp.width = WindowManager.LayoutParams.WRAP_CONTENT;//modify by maoyufeng 20190408
         wlp.height = WindowManager.LayoutParams.WRAP_CONTENT;
         window.setAttributes(wlp);
		 //window.setGravity(Gravity.BOTTOM);
         window.setGravity(Gravity.CENTER);//modify by maoyufeng 20190408
         myCustomDialog.show();
         final int SLEEP_TIME = 3000;
         new Thread((new Runnable() {
            public void run() {
                try {
                    Thread.sleep(SLEEP_TIME);
                    myCustomDialog.dismiss();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

         })).start();

    }
    /* @} */

    SparseArray<Parcelable> mStateContainer = new SparseArray<Parcelable>();
    Drawable mCustomWallPaper;

    private void maybeCreateKeyguardLocked(boolean enableScreenRotation, boolean force,
            Bundle options) {
        if (mKeyguardHost != null) {
            mKeyguardHost.saveHierarchyState(mStateContainer);
            /* SPRD: add for bug848924@{ */
            if (mKeyguardView != null) {
                mSubId = mKeyguardView.getCurrentSubId();
                if (DEBUG) {
                    Log.d(TAG, "mKeyguardView ..." + mSubId);
                }
            }
            /* @} */
        }

        if (mKeyguardHost == null) {
            if (DEBUG) Log.d(TAG, "keyguard host is null, creating it...");

            mKeyguardHost = new ViewManagerHost(mContext);

            int flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                    | WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN
                    | WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;

            if (!mNeedsInput) {
                flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
            }

            final int stretch = ViewGroup.LayoutParams.MATCH_PARENT;
            final int type = WindowManager.LayoutParams.TYPE_KEYGUARD;
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                    stretch, stretch, type, flags, PixelFormat.TRANSLUCENT);
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
            lp.windowAnimations = R.style.Animation_LockScreen;
            lp.screenOrientation = enableScreenRotation ?
                    ActivityInfo.SCREEN_ORIENTATION_USER : ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;

            if (ActivityManager.isHighEndGfx()) {
                lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
                lp.privateFlags |=
                        WindowManager.LayoutParams.PRIVATE_FLAG_FORCE_HARDWARE_ACCELERATED;
            }
            lp.privateFlags |= WindowManager.LayoutParams.PRIVATE_FLAG_SET_NEEDS_MENU_KEY;
            lp.inputFeatures |= WindowManager.LayoutParams.INPUT_FEATURE_DISABLE_USER_ACTIVITY;
            lp.setTitle("Keyguard");
            mWindowLayoutParams = lp;
            // SPRD: fixbug361954 catch the BadTokenException
            try {
                mViewManager.addView(mKeyguardHost, lp);
            } catch (BadTokenException e) {
                Log.w(TAG, "Unable to add window :" + e);
            }

            KeyguardUpdateMonitor.getInstance(mContext).registerCallback(mBackgroundChanger);
        }
        if (force || mKeyguardView == null) {
            /* SPRD: bug268719 2014-01-19 can not set lockscreen wallpaper @{ */
            // default show lockscreen wallpaper
            WallpaperManager wm = (WallpaperManager) mContext
                    .getSystemService(Context.WALLPAPER_SERVICE);
            Drawable drawable = wm.getDrawable(WallpaperInfo.WALLPAPER_LOCKSCREEN_TYPE);
            mCustomWallPaper = drawable;
            Log.d(TAG, "wallpaper drawable=" + drawable);
            mKeyguardHost.setCustomBackground(drawable);
            updateShowWallpaper(drawable == null);
            /* SPRD: bug268719 2014-01-19 can not set lockscreen wallpaper @} */
            View v = mKeyguardHost.findViewById(R.id.keyguard_host_view);
            Log.d(TAG, "maybeCreateKeyguardLocked keyguard_host_view =" + v + "  mKeyguardHost="
                    + mKeyguardHost);
            if (v != null) {
                ((KeyguardHostView) v).onPauseForUUI();
            }
            mKeyguardHost.removeAllViews();
            inflateKeyguardView(options);
            mKeyguardView.requestFocus();
        }
        updateUserActivityTimeoutInWindowLayoutParams();
        mViewManager.updateViewLayout(mKeyguardHost, mWindowLayoutParams);

        /* SPRD: add for bug848924@{ */
        if (DEBUG) {
            Log.d(TAG, "current slot id ..." + mSubId);
        }
        if ((mKeyguardView != null) && (mKeyguardView.getCurrentSubId() == mSubId)) {
            if (DEBUG) {
                Log.d(TAG, "restoreHierarchyState ..." + mSubId);
            }
            mKeyguardHost.restoreHierarchyState(mStateContainer);
        }
        /* @} */
    }

    private void inflateKeyguardView(Bundle options) {
        View v = mKeyguardHost.findViewById(R.id.keyguard_host_view);
        if (v != null) {
            mKeyguardHost.removeView(v);
        }
        final LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.keyguard_host_view, mKeyguardHost, true);
        mKeyguardView = (KeyguardHostView) view.findViewById(R.id.keyguard_host_view);
        mKeyguardView.setLockPatternUtils(mLockPatternUtils);
        mKeyguardView.setViewMediatorCallback(mViewMediatorCallback);
        mKeyguardView.initializeSwitchingUserState(options != null &&
                options.getBoolean(IS_SWITCHING_USER));

        // HACK
        // The keyguard view will have set up window flags in onFinishInflate before we set
        // the view mediator callback. Make sure it knows the correct IME state.
        if (mViewMediatorCallback != null) {
            KeyguardPasswordView kpv = (KeyguardPasswordView) mKeyguardView.findViewById(
                    R.id.keyguard_password_view);

            if (kpv != null) {
               // mViewMediatorCallback.setNeedsInput(kpv.needsInput());
            }
        } 

        if (options != null) {
            int widgetToShow = options.getInt(LockPatternUtils.KEYGUARD_SHOW_APPWIDGET,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            if (widgetToShow != AppWidgetManager.INVALID_APPWIDGET_ID) {
                mKeyguardView.goToWidget(widgetToShow);
            }
        }
    }

    public void updateUserActivityTimeout() {
        updateUserActivityTimeoutInWindowLayoutParams();
        mViewManager.updateViewLayout(mKeyguardHost, mWindowLayoutParams);
    }

    private void updateUserActivityTimeoutInWindowLayoutParams() {
        // Use the user activity timeout requested by the keyguard view, if any.
        if (mKeyguardView != null) {
            long timeout = mKeyguardView.getUserActivityTimeout();
            if (timeout >= 0) {
                mWindowLayoutParams.userActivityTimeout = timeout;
                return;
            }
        }

        // Otherwise, use the default timeout.
        mWindowLayoutParams.userActivityTimeout = KeyguardViewMediator.AWAKE_INTERVAL_DEFAULT_MS;
    }

    private void maybeEnableScreenRotation(boolean enableScreenRotation) {
        // TODO: move this outside
        if (enableScreenRotation) {
            if (DEBUG) Log.d(TAG, "Rotation sensor for lock screen On!");
            mWindowLayoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_USER;
        } else {
            if (DEBUG) Log.d(TAG, "Rotation sensor for lock screen Off!");
            mWindowLayoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_NOSENSOR;
        }
        mViewManager.updateViewLayout(mKeyguardHost, mWindowLayoutParams);
    }

    void updateShowWallpaper(boolean show) {
        if (show) {
            mWindowLayoutParams.flags |= WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
        } else {
            mWindowLayoutParams.flags &= ~WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER;
        }

        mViewManager.updateViewLayout(mKeyguardHost, mWindowLayoutParams);
    }

    public void setNeedsInput(boolean needsInput) {
        mNeedsInput = needsInput;
        if (mWindowLayoutParams != null) {
            if (needsInput) {
                mWindowLayoutParams.flags &=
                    ~WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
            } else {
                mWindowLayoutParams.flags |=
                    WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
            }

            try {
                mViewManager.updateViewLayout(mKeyguardHost, mWindowLayoutParams);
            } catch (java.lang.IllegalArgumentException e) {
                // TODO: Ensure this method isn't called on views that are changing...
                Log.w(TAG,"Can't update input method on " + mKeyguardHost + " window not attached");
            }
        }
    }

    /**
     * Reset the state of the view.
     */
    public synchronized void reset(Bundle options) {
        if (DEBUG) Log.d(TAG, "reset()");
        // User might have switched, check if we need to go back to keyguard
        // TODO: It's preferable to stay and show the correct lockscreen or unlock if none
        maybeCreateKeyguardLocked(shouldEnableScreenRotation(), true, options);
    }

    public synchronized void onScreenTurnedOff() {
        if (DEBUG) Log.d(TAG, "onScreenTurnedOff()");
        mScreenOn = false;
        if (mKeyguardView != null) {
            mKeyguardView.onScreenTurnedOff();
        }
        // SPRD: Modify 20131230 Spreadst of Bug 262132 modify UUI lockscreen policy
        KeyguardViewManager.setShowUUILock(true);
    }

    public synchronized void onScreenTurnedOn(final IKeyguardShowCallback callback) {
        if (DEBUG) Log.d(TAG, "onScreenTurnedOn()");
        mScreenOn = true;

        // If keyguard is not showing, we need to inform PhoneWindowManager with a null
        // token so it doesn't wait for us to draw...
        final IBinder token = isShowing() ? mKeyguardHost.getWindowToken() : null;

        if (DEBUG && token == null) Slog.v(TAG, "send wm null token: "
                + (mKeyguardHost == null ? "host was null" : "not showing"));

        if (mKeyguardView != null) {
            mKeyguardView.onScreenTurnedOn();

            // Caller should wait for this window to be shown before turning
            // on the screen.
            if (callback != null) {
                if (mKeyguardHost.getVisibility() == View.VISIBLE) {
                    // Keyguard may be in the process of being shown, but not yet
                    // updated with the window manager...  give it a chance to do so.
                    mKeyguardHost.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                callback.onShown(token);
                            } catch (RemoteException e) {
                                Slog.w(TAG, "Exception calling onShown():", e);
                            }
                        }
                    });
                } else {
                    try {
                        callback.onShown(token);
                    } catch (RemoteException e) {
                        Slog.w(TAG, "Exception calling onShown():", e);
                    }
                }
            }
        } else if (callback != null) {
            try {
                callback.onShown(token);
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception calling onShown():", e);
            }
        }
    }

    public synchronized void verifyUnlock() {
        if (DEBUG) Log.d(TAG, "verifyUnlock()");
        show(null);
        mKeyguardView.verifyUnlock();
    }

    /**
     * Hides the keyguard view
     */
    public synchronized void hide() {
        if (DEBUG) Log.d(TAG, "hide()");

        if (mKeyguardHost != null) {
            mKeyguardHost.setVisibility(View.GONE);

            // We really only want to preserve keyguard state for configuration changes. Hence
            // we should clear state of widgets (e.g. Music) when we hide keyguard so it can
            // start with a fresh state when we return.
            mStateContainer.clear();

            // Don't do this right away, so we can let the view continue to animate
            // as it goes away.
            if (mKeyguardView != null) {
                final KeyguardViewBase lastView = mKeyguardView;
                mKeyguardView = null;
                // SPRD: Modify 20131230 Spreadst of Bug 262132 modify UUI lockscreen policy
                KeyguardViewManager.setShowUUILock(true);
                mKeyguardHost.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (KeyguardViewManager.this) {
                            lastView.cleanUp();
                            Log.d(TAG, " the lock showing is "+ isShowing());
                            if (!isShowing()) {
                                // Let go of any large bitmaps.
                                mKeyguardHost.setCustomBackground(null);
                                mCustomWallPaper = null;
                                updateShowWallpaper(true);
                            }
                            mKeyguardHost.removeView(lastView);
                            mViewMediatorCallback.keyguardGone();
                        }
                    }
                }, HIDE_KEYGUARD_DELAY);
            }
        }
    }

    /**
     * Dismisses the keyguard by going to the next screen or making it gone.
     */
    public synchronized void dismiss() {
        if (mScreenOn) {
            mKeyguardView.dismiss();
        }
    }

    /**
     * @return Whether the keyguard is showing
     */
    public synchronized boolean isShowing() {
        return (mKeyguardHost != null && mKeyguardHost.getVisibility() == View.VISIBLE);
    }

    public void showAssistant() {
        if (mKeyguardView != null) {
            mKeyguardView.showAssistant();
        }
    }

    public void dispatch(MotionEvent event) {
        if (mKeyguardView != null) {
            mKeyguardView.dispatch(event);
        }
    }

    public void launchCamera() {
        if (mKeyguardView != null) {
            mKeyguardView.launchCamera();
        }
    }

    /* SPRD: Modify 20131230 Spreadst of Bug 262132 modify UUI lockscreen policy @{ */
    static void setShowUUILock(boolean show) {
        mIsShowUUILock = show;
    }

    static boolean IsShowUUILock() {
        return mIsShowUUILock;
    }
    /* @} */

}
