package com.nb.dq.util;

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.EditText;
import android.os.Build;

import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageInfo;
import android.app.AppGlobals;
import android.provider.CallLog.Calls;
import android.content.res.AssetManager;
import android.content.res.Resources;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import android.os.SystemProperties;
import android.app.AlertDialog;
import android.provider.Settings;
import android.database.Cursor;
import android.net.Uri;
import com.android.internal.telephony.PhoneConstants;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.widget.Toast;
import android.content.DialogInterface;


import android.content.ContentResolver;
import com.sprd.simple.launcher.R;

//bird, *#0000# for CTRK, sunqi add start 
import android.telephony.SubscriptionManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.telephony.ITelephony;
//bird, *#0000# for CTRK, sunqi add end 

public class BirdSpecialCharMgr {
    private static final String TAG="BirdSpecialCharMgr";
	private static ArrayList<String> CommandMMITest=new ArrayList<String>();
	private static ArrayList<String> CommandAutoMMITest=new ArrayList<String>();
	private static ArrayList<String> CommandFactoryTest=new ArrayList<String>();
	private static ArrayList<String> CommandEngineerMode=new ArrayList<String>();
	private static ArrayList<String> CommandInnerSwVersion=new ArrayList<String>();


	static{
		
		CommandMMITest.add("*#36951#*");	//default
		CommandMMITest.add("*#37*#");
		CommandMMITest.add("*#87*#");
		CommandMMITest.add("*#6688#");//bug48065 add by wucheng 20190527
//		CommandAutoMMITest.add("*#87*#");
		
		CommandFactoryTest.add("*#36955#*");	//default
        CommandFactoryTest.add("*#1*");
		
		
		
		CommandEngineerMode.add("*#15963#*");	//default
		
		CommandInnerSwVersion.add("*#8377466#");
		CommandInnerSwVersion.add("*#0000#");

	
	}
   private static final String MMI_IMEI_DISPLAY = "*#06#";
	
  private static final String BIRD_HARDWARE_INFO_CUSTOM = "*#1688#*";/**#6812#*/;
  private static final String BIRD_HARDWARE_INFO_INTERNAL = "*#732873#*";
  private static final String BIRD_SIM_INFO = "*#1699#";//bug48038 add by wucheng 20190524
  private static final String BIRD_BATTERY_INFO = "*#8222#";//bug48036 add by wucheng 20190524
  private static final String BIRD_SECRET_CODE_INFO = "*#1655#";//bug48037 add by wucheng 20190524
  private static final String BIRD_AGING_TEST = "*#11#";//bug48034 add by wucheng 20190525
  private static final String BIRD_RELIABILITY_TEST = "*#9898#";//bug48032 add by wucheng 20190525
  private static final String BIRD_WRITE_IMEI = "*#1122#";//bug48031 add by wucheng 20190525
  private static final String BIRD_VERSION_INFO_CUSTOM = "*#8111#";//add by wutingying for 48014 20190525
    private static final String BIRD_BOOT_SWITCH="*#5555#";//add by hujingcheng 20190829


    public static boolean handlerSpecialCharMgr(Context context, String input) {
        return handleMMITest(context, input)
		        || handleAutoMMIest(context, input)
                || handleFactoryTest(context, input)
                || handleEngineermode(context, input)
                || handleDeviceIdDisplay(context,input)
                || handleShowHardwareInfo(context,input)
                || handleShowSimInfo(context,input)//bug48038 add by wucheng 20190524
                || handleShowBatteryInfo(context,input)//bug48036 add by wucheng 20190524
                || handleShowSecretCodeInfo(context,input)//bug48037 add by wucheng 20190524
                || handleShowAgingTest(context,input)//bug48034 add by wucheng 20190525
                || handleShowReliabilityTest(context,input)//bug48032 add by wucheng 20190525
                || handleShowWriteImei(context,input)//bug48031 add by wucheng 20190525
                || handleInnerSoftwareVersion(context,input)
                || handleShowVersionInfo(context,input)//add by wutingying for 48014 20190525
                || handleBootSwitch(context,input);//add by hujingcheng 20190829
    }
	private static boolean startActivity(Context context,String pkgName,String clzName){
		ComponentName cpn = new ComponentName(pkgName, clzName);
        Intent intent = new Intent();
        intent.setComponent(cpn);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "startActivity() failed: " + e);
			return false;
        }
        return true;
    }
//add by wutingying for 48014 20190525 begin
      public static boolean handleShowVersionInfo(Context context, String input){
            if (BIRD_VERSION_INFO_CUSTOM.equals(input)) {
                Intent intent = new Intent();
                intent.setClassName("com.mediatek.engineermode","com.mediatek.engineermode.VersionInfoActivity");
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "startActivity() failed: " + e);
                    return false;
                }
                return true;
            }
       
        return false;
    }
//add by wutingying for 48014 20190525 end
	public static boolean handleMMITest(Context context, String input){
		if(CommandMMITest.contains(input)){
			return startActivity(context,"com.nbbsw.mmi_test", "com.nbbsw.mmi_test.MMITest");
		}else{
			return false;
		}
	}

    /**
     * add by hujingcheng 20190828 for custom boot logo and animation by secret code
     */
	public static boolean handleBootSwitch(Context context,String input){
        if(BIRD_BOOT_SWITCH.equals(input)){
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName("com.android.settings","com.bird.settings.BirdBootSwitchActivity");
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                return false;
            }
            return true;
        }
        return false;
    }
	
	public static boolean handleAutoMMIest(Context context, String input){
		if(CommandAutoMMITest.contains(input)){
		    Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setClassName("com.nbbsw.mmi_test","com.nbbsw.mmi_test.MMITest");
            intent.putExtra("auto_test", true);
            try {
                 context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
			     return false;
            }
		    return true;
		}
		return false;
        
	}
	public static boolean handleFactoryTest(Context context, String input){
		if( CommandFactoryTest.contains(input) ){
			return startActivity(context,"com.nbbsw.factory_test", "com.nbbsw.factory_test.FactoryTest");
		}else{
			return false;
		}
	}

	
	public static boolean handleEngineermode(Context context, String input){
		if(CommandEngineerMode.contains(input)){
			return startActivity(context,"com.mediatek.engineermode", "com.mediatek.engineermode.EngineerMode");
		}else{
			return false;
		}
	}
	

    public static boolean handleShowHardwareInfo(Context context, String input){
            if (BIRD_HARDWARE_INFO_CUSTOM.equals(input) || BIRD_HARDWARE_INFO_INTERNAL.equals(input)) {
                Intent intent = new Intent();
                intent.setClassName("com.nbbird.hardwareinfo","com.nbbird.hardwareinfo.MainActivity");
                if (BIRD_HARDWARE_INFO_CUSTOM.equals(input)) {
                    intent.putExtra("is_internal", false);
                } else if (BIRD_HARDWARE_INFO_INTERNAL.equals(input)) {
                    intent.putExtra("is_internal", true);
                }
                try {
                    context.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "startActivity() failed: " + e);
                    return false;
                }
                return true;
            }
       
        return false;
    }

// TODO: Use TelephonyCapabilities.getDeviceIdLabel() to get the device id label instead of a
  // hard-coded string.
  static boolean handleDeviceIdDisplay(Context context, String input) {
    TelephonyManager telephonyManager =
        (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

    if (telephonyManager != null && input.equals(MMI_IMEI_DISPLAY)) {
          int labelResId = R.string.imei;
          try {
           
              for (int slot = 0; slot < telephonyManager.getPhoneCount(); slot++) {
                int phoneType = telephonyManager.getPhoneType(slot);
                if (phoneType == TelephonyManager.PHONE_TYPE_CDMA &&
                    telephonyManager.getLteOnCdmaMode() == PhoneConstants.LTE_ON_CDMA_TRUE) {
                    labelResId = R.string.imei_meid;
                }
            }
        } catch (SecurityException e) {
            /// M: Catch the security exception to avoid dialer crash, such as user denied
            /// READ_PHONE_STATE permission in settings at N version. And display empty list.
            //Toast.makeText(context, R.string.missing_required_permission, Toast.LENGTH_SHORT).show();
        }

      List<String> deviceIds = new ArrayList<String>();
      try {
      
          for (int slot = 0; slot < telephonyManager.getPhoneCount(); slot++) {
            String deviceId = telephonyManager.getDeviceId(slot);
            if (!TextUtils.isEmpty(deviceId)) {
              deviceIds.add(deviceId);
            }
            }
      } catch (SecurityException e) {
        /// M: Catch the security exception to avoid dialer crash, such as user denied
        /// READ_PHONE_STATE permission in settings at N version. And display empty list.
       // Toast.makeText(context, R.string.missing_required_permission, Toast.LENGTH_SHORT).show();
      }

      //bird, *#06# for CTRK, sunqi add start
      deviceIds.clear();
      String meid = "MEID:" + getMeid();
      String imei1 = "IMEI1: " + TelephonyManager.getDefault().getImei(0);
      String imei2 = "IMEI2: " + TelephonyManager.getDefault().getImei(1);

      if(!meid.equals("")) deviceIds.add(meid);
      if(!imei1.equals("")) deviceIds.add(imei1);
      if(!imei2.equals("")) deviceIds.add(imei2);
      //bird, *#06# for CTRK, sunqi add end

      new AlertDialog.Builder(context)
          .setTitle(labelResId)
          .setItems(deviceIds.toArray(new String[deviceIds.size()]), null)
          .setPositiveButton(android.R.string.ok, null)
          .setCancelable(false)
          .show();
      return true;
    }
    return false;
  }


    public static boolean handleInnerSoftwareVersion(Context context, String input) {
        if(CommandInnerSwVersion.contains(input)) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.bird_inner_sw_version)
                    .setMessage(SystemProperties.get("ro.bdfun.inner_sw_version", "null"))
                    .setPositiveButton(android.R.string.ok, null)
                    .setCancelable(false)
                    .show();
            return true;
        }
        return false;
   }

	//bug48038 add by wucheng 20190524 begin
    public static boolean handleShowSimInfo(Context context, String input) {
        if (input.equals(BIRD_SIM_INFO)) {
			Intent intent = new Intent("bird.action.phone_status_check");
	        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        context.startActivity(intent);	
			return true;		
        }
        return false;
   }
	//bug48038 add by wucheng 20190524 end
	
	//bug48036 add by wucheng 20190524 begin
    public static boolean handleShowBatteryInfo(Context context, String input) {
        if (input.equals(BIRD_BATTERY_INFO)) {
			startActivity(context,"com.mediatek.engineermode", "com.mediatek.engineermode.BatteryLog");
        }
        return false;
   }
	//bug48036 add by wucheng 20190524 end
   
	//bug48037 add by wucheng 20190524 begin
    public static boolean handleShowSecretCodeInfo(Context context, String input) {
        if (input.equals(BIRD_SECRET_CODE_INFO)) {
			startActivity(context,"com.nbbsw.mmi_test", "com.nbbsw.mmi_test.secretcode.SecretCodeParserActivity");
        }
        return false;
   }
	//bug48037 add by wucheng 20190524 end
	
	//bug48037 add by wucheng 20190524 begin
    public static boolean handleShowAgingTest(Context context, String input) {
        if (input.equals(BIRD_AGING_TEST)) {
			startActivity(context,"com.nbbsw.mmi_test", "com.nbbsw.mmi_test.StressTest.AgingTest");
        }
        return false;
   }
	//bug48037 add by wucheng 20190524 end
	
	//bug48032 add by wucheng 20190524 begin
    public static boolean handleShowReliabilityTest(Context context, String input) {
        if (input.equals(BIRD_RELIABILITY_TEST)) {
			startActivity(context,"com.nbbsw.mmi_test", "com.nbbsw.mmi_test.reliability_test.ReliabilityTestMain");
        }
        return false;
   }
	//bug48032 add by wucheng 20190524 end
	
	//bug48031 add by wucheng 20190524 begin
    public static boolean handleShowWriteImei(Context context, String input) {
        if (input.equals(BIRD_WRITE_IMEI)) {
			startActivity(context,"com.bird.writeimei", "com.bird.writeimei.MainActivity");
        }
        return false;
   }
	//bug48031 add by wucheng 20190524 end
	
    //bird, *#0000# for CTRK, sunqi add start 
    private static final String PRL_VERSION_DISPLAY = "*#0000#";
    private static final String CDMAINFO = "android.intent.action.CdmaInfoSpecification";
    public static boolean handleCT0000(Context context, String input){
        if(PRL_VERSION_DISPLAY.contains(input)){
            handleChars(context,input);
            return true;
        }else{
            return false;
        }
    }


    private static boolean handleChars(Context context, String input) {
        if (input.equals(PRL_VERSION_DISPLAY)) {
            int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            int length = SubscriptionManager.from(
                    context).getActiveSubscriptionIdList().length;
            try {
                ITelephony iTel = ITelephony.Stub.asInterface(
                        ServiceManager.getService(Context.TELEPHONY_SERVICE));
                //log("handleChars getActiveSubscriptionIdList length:" + length);
                for (int i = 0; i < length; i++) {
                    int activeSubId = SubscriptionManager.from(
                    context).getActiveSubscriptionIdList()[i];
                    int slotId = SubscriptionManager.getSlotIndex(activeSubId);
                    int phoneType = iTel.getActivePhoneTypeForSlot(slotId);
                    if (PhoneConstants.PHONE_TYPE_CDMA == phoneType) {
                        subId = activeSubId;
                        //log("handleChars subId:" + subId);
                        break;
                    }
                }
            } catch (RemoteException ex) {
                ex.printStackTrace();
            }

            if (0 != length && SubscriptionManager.isValidSubscriptionId(subId)) {
                showPRLVersionSetting(context, subId);
                return true;
            } else {
                showPRLVersionSetting(context, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
                return true;
            }
        }
        return false;
    }
    
    private static void showPRLVersionSetting(Context context, int subId) {
        /*final UserManager userManager = UserManager.get(context);
        if (!userManager.isPrimaryUser()) {
            Toast.makeText(
                    mPluginContext,
                    mPluginContext.getResources().getString(
                        R.string.error_account_access), Toast.LENGTH_LONG)
                    .show();

            log("not primary user, just return.");
            return;
        }*/

        Intent intentCdma = new Intent(CDMAINFO);
        intentCdma.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intentCdma.putExtra("subid", subId);
        context.startActivity(intentCdma);
    }
    //bird, *#0000# for CTRK, sunqi add end 

    //bird, *#06# for CTRK, sunqi add start
    private static String getMeid() {
        String meid = "";
        int count = TelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < count; i++) {
            if (TextUtils.isEmpty(meid)) {
                meid = TelephonyManager.getDefault().getMeid(i);
                Log.d(TAG,"getMeid(), meid = " + meid);
            }
        }
        return meid;
    }
    //bird, *#06# for CTRK, sunqi add end
}
