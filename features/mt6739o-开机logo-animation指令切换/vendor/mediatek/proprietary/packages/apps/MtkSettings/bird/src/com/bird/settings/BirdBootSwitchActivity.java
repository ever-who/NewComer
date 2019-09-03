package com.bird.settings;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.DialogPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;

import com.android.settings.R;
import com.bird.settings.nvram.NvRAMHelper;
import com.bird.settings.nvram.NvRamEntry;

import java.util.Arrays;

/**
 * Created by hujingcheng on 19-8-26 custom boot logo and animation by secret code .
 */

public class BirdBootSwitchActivity extends PreferenceActivity
        implements Preference.OnPreferenceClickListener,Preference.OnPreferenceChangeListener{

    private static final String TAG = "BirdBootSwitchActivity";
    private static final String BOOT_LOGO_PREF_KEY="boot_logo";
    private static final String BOOT_ANIMATION_PREF_KEY="boot_animation";
    private static final String SAVER_PREF_KEY="saver";
    private static final int BOOT_LOGO_INDEX=553;//537+16
    private static final int BOOT_ANIMATION_INDEX=554;//553+1
    private static final int SHUT_ANIMATION_INDEX=555;//554+1

    private byte[] productInfoBytes=null;

    private ListPreference mBootLogoPref;
    private ListPreference mBootAnimationPref;
    private Preference mSavePref;

    private static final int MSG_SAVE=1;
    private static final int MSG_ENABLE=2;
    private static final int MSG_ERROR=3;

    private Handler handler=new BootSwitchHandler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.boot_switch_activity);
        productInfoBytes = NvRAMHelper.readData(NvRamEntry.PRODUCT_INFO_FILENAME,0,1024);
        initViews();
    }

    private void initViews(){
        //product_info_bytes = NvRAMHelper.readData(NvRamEntry.PRODUCT_INFO_FILENAME,0,1024);
        mBootLogoPref=(ListPreference)findPreference(BOOT_LOGO_PREF_KEY);
        mBootAnimationPref=(ListPreference)findPreference(BOOT_ANIMATION_PREF_KEY);
        mSavePref=findPreference(SAVER_PREF_KEY);
        mBootAnimationPref.setOnPreferenceChangeListener(this);
        mBootLogoPref.setOnPreferenceChangeListener(this);
        mSavePref.setOnPreferenceClickListener(this);
        initValues();
    }

    private void initValues(){
        if(productInfoBytes!=null && productInfoBytes.length==1024){
            int bootLogoValue=productInfoBytes[BOOT_LOGO_INDEX];
            mBootLogoPref.setValue(String.valueOf(bootLogoValue));
            mBootLogoPref.setSummary(getString(R.string.boot_logo_summary)+(bootLogoValue+1));
            int bootAnimationValue=productInfoBytes[BOOT_ANIMATION_INDEX];
            mBootAnimationPref.setValue(String.valueOf(bootAnimationValue));
            mBootAnimationPref.setSummary(getString(R.string.boot_animation_summary)+(bootAnimationValue+1));
        }else{
            disableAll();
        }
    }

    private void disableAll(){
        if(mBootLogoPref!=null){
            mBootLogoPref.setEnabled(false);
        }
        if(mSavePref!=null){
            mSavePref.setEnabled(false);
        }
        if(mBootAnimationPref!=null){
            mBootAnimationPref.setEnabled(false);
        }
    }

    private void enableAll(){
        if(mBootLogoPref!=null){
            mBootLogoPref.setEnabled(true);
        }
        if(mSavePref!=null){
            mSavePref.setEnabled(true);
        }
        if(mBootAnimationPref!=null){
            mBootAnimationPref.setEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: product_info_bytes = "+ Arrays.toString(productInfoBytes));
    }

    private void dismissDialogs(DialogPreference preference){
        if(preference!=null){
            Dialog dialog=preference.getDialog();
            if(dialog!=null && dialog.isShowing()){
                dialog.dismiss();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        dismissDialogs(mBootLogoPref);
        dismissDialogs(mBootAnimationPref);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        Log.d(TAG, "onPreferenceTreeClick: enter");
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        Log.d(TAG, "onPreferenceClick: ");
        if(SAVER_PREF_KEY.equals(preference.getKey())){
            preference.setTitle(getString(R.string.saving));
            disableAll();
            handler.sendEmptyMessage(MSG_SAVE);
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        //handler=null;
        //mBootLogoPref.setOnPreferenceChangeListener(null);
        //Log.d(TAG, "onDestroy: NvRAMHelper.readData(NvRamEntry.PRODUCT_INFO_FILENAME,0,1024)"+Arrays.toString(NvRAMHelper.readData(NvRamEntry.PRODUCT_INFO_FILENAME,BOOT_LOGO_INDEX,1)));
        super.onDestroy();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        String key= preference.getKey();
        Log.d(TAG, "onPreferenceChange: key="+key+",o="+o);
        switch (key){
            case BOOT_LOGO_PREF_KEY:
                mBootLogoPref.setSummary(getString(R.string.boot_logo_summary)+String.valueOf(1+Integer.valueOf(o.toString())));
                return true;
            case BOOT_ANIMATION_PREF_KEY:
                mBootAnimationPref.setSummary(getString(R.string.boot_animation_summary)+String.valueOf(1+Integer.valueOf(o.toString())));
                return true;
            default:break;
        }
        return false;
    }


    private class BootSwitchHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "handleMessage: msg.what = "+msg.what);
            switch (msg.what){
                case MSG_SAVE:
                    byte bootLogoFlag= -1;
                    byte bootAnimationFlag = -1;
                    if(mBootLogoPref!=null){
                        bootLogoFlag=Byte.valueOf(mBootLogoPref.getValue());
                    }
                    if(mBootAnimationPref!=null){
                        bootAnimationFlag=Byte.valueOf(mBootAnimationPref.getValue());
                    }
                    if(bootLogoFlag<0 && bootAnimationFlag<0){
                        sendEmptyMessageDelayed(MSG_ERROR,1000);
                    }else{
                        if(productInfoBytes!=null && productInfoBytes.length==1024){
                            //NvRAMHelper.int2Bytes(bootLogoFlag,productInfoBytes,BOOT_LOGO_INDEX);
                            productInfoBytes[BOOT_LOGO_INDEX]=bootLogoFlag;
                            productInfoBytes[BOOT_ANIMATION_INDEX]=bootAnimationFlag;
                            Log.d(TAG, "MSG_SAVE:  productInfoBytes="+Arrays.toString(productInfoBytes));
                            NvRAMHelper.writeData(NvRamEntry.PRODUCT_INFO_FILENAME,0,1024,productInfoBytes);
                            sendEmptyMessageDelayed(MSG_ENABLE,1000);
                        }else{
                            sendEmptyMessageDelayed(MSG_ERROR,1000);
                        }
                    }
                    break;
                case MSG_ENABLE:
                    mSavePref.setTitle(R.string.menu_save);
                    enableAll();
                    Toast.makeText(BirdBootSwitchActivity.this,getString(R.string.save_success),Toast.LENGTH_SHORT).show();
                    break;
                case MSG_ERROR:
                    mSavePref.setTitle(R.string.menu_save);
                    enableAll();
                    Toast.makeText(BirdBootSwitchActivity.this,getString(R.string.save_fail),Toast.LENGTH_SHORT).show();
                default:break;
            }
        }
    }

}
