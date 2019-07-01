package com.nb.dq.base;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.nb.dq.main.DqLauncher;
import com.nb.dq.model.ShowMode;
import com.sprd.simple.util.KeyCodeEventUtil;

import android.os.Handler;//bird add by wucheng 20180930
import android.net.Uri;
/**
 * Created by czh on 2017/6/5.
 */

public abstract class DqBaseActivity extends Activity {
    protected static final String TAG = "czh";
    //protected Activity context;
	//bird add by wucheng 20180930 begin
	private int currentKeyCode = 0;
	private KeyEvent mEvent;
	private Handler mHandler = new Handler();
	private Runnable mLongPressRunnable = new Runnable() {    
            @Override  
            public void run() {
				if (currentKeyCode==KeyEvent.KEYCODE_POUND) {
					KeyCodeEventUtil.longPressKeyEvent(DqBaseActivity.this, currentKeyCode, mEvent);	
				}
            }  
        };
	//bird add by wucheng 20180930 end
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //context = this;

        if (getLayoutId() > 0) {
            setContentView(getLayoutId());
        }
        initData(savedInstanceState);
    }
    protected abstract int getLayoutId();

    protected abstract void initData(Bundle savedInstanceState);

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode==KeyEvent.KEYCODE_DPAD_CENTER){
            KeyCodeEventUtil.onLongPressCenterKey(this);
		//bird add by wucheng 20180930 begin
        //}else if (keyCode==KeyEvent.KEYCODE_POUND && "bihee".equals(KeyCodeEventUtil.BIRD_HOME_LONGPRESS_KEYEVENT_TYPE)){
		//bird add by wucheng 20180930 end
        }else {
            KeyCodeEventUtil.longPressKeyEvent(this, keyCode, event);
        }
        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //change by BIRD@hujingcheng 20190605 add keycodes no need to handle
        if (needBacktoSuper(keyCode) ||keyCode==KeyEvent.KEYCODE_DPAD_LEFT||keyCode==KeyEvent.KEYCODE_DPAD_RIGHT||keyCode==KeyEvent.KEYCODE_DPAD_UP||keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
            return super.onKeyDown(keyCode,event);
        }
		//if ((keyCode==KeyEvent.KEYCODE_0&&"philips".equals(KeyCodeEventUtil.BIRD_HOME_LONGPRESS_KEYEVENT_TYPE))
		//	/*|| (keyCode==KeyEvent.KEYCODE_STAR&&"bihee".equals(KeyCodeEventUtil.BIRD_HOME_LONGPRESS_KEYEVENT_TYPE))*/) { 
            //Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:"));
            //intent.setPackage(getPackageName());
            //startActivity(intent);
		    //return true;
        //}
        if (event.getRepeatCount() == 0) {
			//bird add by wucheng 20180930 begin
			if ("bihee".equals(KeyCodeEventUtil.BIRD_HOME_LONGPRESS_KEYEVENT_TYPE)) {
				mHandler.removeCallbacks(mLongPressRunnable);
				currentKeyCode = keyCode;
				mEvent = event;
	            mHandler.postDelayed(mLongPressRunnable, 3000);
			}
			//bird add by wucheng 20180930 end
            event.startTracking();
            return true;
        }else{
            Log.i(TAG,event.getRepeatCount()+"");
        }

        return false;
    }

    /**
     * keycode no need to handle in launcher , add by BIRD@hujingcheng 20190604
     * @param keycode
     * @return if need to back to super
     */
    protected boolean needBacktoSuper(int keycode){
        switch (keycode){
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        //change by BIRD@hujingcheng 20190605 add keycodes no need to handle
        if ( needBacktoSuper(keyCode)||keyCode==KeyEvent.KEYCODE_DPAD_LEFT||keyCode==KeyEvent.KEYCODE_DPAD_RIGHT||keyCode==KeyEvent.KEYCODE_DPAD_UP||keyCode==KeyEvent.KEYCODE_DPAD_DOWN){
            return super.onKeyUp(keyCode,event);
        }
        if (event.isTracking() && !event.isCanceled()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_MENU:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    doMenu();
                    break;
                case KeyEvent.KEYCODE_BACK:
                    doBack();
                    break;
                default:
                    if (this instanceof DqLauncher){
                        if (DqLauncher.mMode == ShowMode.Launcher) {
                            KeyCodeEventUtil.pressKeyEventForMainActivity(this, keyCode, event);
                        }
                    }
                    break;
            }
        } else {
        }
		//bird add by wucheng 20180930 begin
		if (keyCode==KeyEvent.KEYCODE_POUND && "bihee".equals(KeyCodeEventUtil.BIRD_HOME_LONGPRESS_KEYEVENT_TYPE)){
			mHandler.removeCallbacks(mLongPressRunnable);
		}
		//bird add by wucheng 20180930 end
        return false;
    }
    protected abstract void doBack();
    protected abstract void doMenu();

}
