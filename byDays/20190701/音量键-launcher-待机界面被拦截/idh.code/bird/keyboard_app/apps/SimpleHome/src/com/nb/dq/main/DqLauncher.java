package com.nb.dq.main;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewRootImpl;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;
import com.nb.dq.base.DqBaseActivity;
import com.nb.dq.util.AndroidUtil;
import com.nb.dq.util.Lunar;
import com.nb.dq.model.Favorite;
import com.nb.dq.model.Folder;
import com.nb.dq.model.ShowMode;
import com.nb.dq.model.WeatherInfo;
import com.nb.dq.util.Config;
import com.nb.dq.util.FavoriteStorage;
import com.nb.dq.util.LoadImageUtil;
import com.sprd.simple.launcher.R;
import com.sprd.simple.model.TextSpeech;
import com.sprd.simple.util.MissCallContentObserver;
import com.sprd.simple.util.UnreadInfoUtil;
import com.sprd.simple.util.UnreadMessageContentObserver;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
//bird add by wucheng 20180811 begin
import android.text.TextUtils;
import com.nb.dq.smart.QuickStartFeatureOption;
import com.sprd.simple.util.KeyCodeEventUtil;
//bird add by wucheng 20180811 end
import java.util.List;
import android.view.Gravity;
import com.nb.dq.util.TextClockUtils;
import android.text.format.DateFormat;
import android.content.res.Resources;
import android.media.AudioManager;

//@ {bird:add by fanglongxiang 20180908
import com.bird.dq.BirdFeatureOption;
//@ }

import android.sim.Sim;
import android.sim.SimManager;
import android.telephony.TelephonyManager;
import android.net.Uri;//add by maoyufeng 20190418
/**
 * Created by czh on 2017/6/5.
 */

public class DqLauncher extends DqBaseActivity implements ViewPager.OnPageChangeListener {
/*** performance optimization wanglei 20180929 add begin ***/
	private static final String TAG = "DqLauncher";
/*** performance optimization wanglei 20180929 add end ***/
    public static final String HOME_CURRENT_POSITION = "home_current_postion";

    /*add by BIRD@hujingcheng 20190531 ui control for land */
    private static final boolean SHOW_LUNAR_DATE=false;
    private static final boolean LAND_UI=true;
	
	private AudioManager mAudioManager;
	
	private LinearLayout mCarrierInfoLayout;

    private LinearLayout mainMenuContainer;
    private TextView lunarText;
    private TextClock timeText;
    private TextView weekText;//mark
    private TextView leftControl;
    private TextView rightControl;
    private ViewPager mViewPager;
    public static int mMode = ShowMode.Launcher;
    private ArrayList<CellLayout> mCellLayoutList;
    private UnreadMessageContentObserver mMmsSmsObserver;
    /*YUNOS BEGIN*/
    private MissCallContentObserver mCallObserver;
    /*YUNOS END*/
	private TelephonyManager mTelephonyManager;//bug45926 add by wucheng 20190321
	
    private Sim mSims[];	
	private int SIM_CARD_1 = 0;
    private int SIM_CARD_2 = 1;
	private final static String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";	
    private static final int DATE_CHANGE_MESSAGE = 1000;
    private static final int WEATHER_REQUEST_SUCCES = 2000;
    private static final int WEATHER_REQUEST_EMPTY = 3000;
	private static final int UPDATE_CARRIER_MESSAGE = 4000;
    private ImageView weatherIV;
    private TextView cityTV;
    private TextView tmpTV;
    private String mCurrentLocale = "";
    private int mCurrrentPosition =0 ;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                switch (msg.what) {
                    case UnreadInfoUtil.MMSSMS_UNREAD_MESSAGE:
                        int messageCount = Integer.parseInt(String.valueOf(msg.obj));
                        for (CellLayout view : mCellLayoutList) {
                            if (view.getReadCount() == 2) {
                                view.setUnReadCount(messageCount);
                            }
                        }
                        break;
                /*YUNOS BEGIN*/
                    case UnreadInfoUtil.MISS_CALL_MESSAGE:
                        int callCount = Integer.parseInt(String.valueOf(msg.obj));
                        for (CellLayout view : mCellLayoutList) {
                            if (view.getReadCount() == 1) {
                                view.setUnReadCount(callCount);
                            }
                        }
                        break;
                    case DATE_CHANGE_MESSAGE:
                        //change by BIRD@hujingcheng 20190530
                        if(SHOW_LUNAR_DATE){
                            setLunarText();
                        }
                        if(LAND_UI){
                            setWeekText();
                        }
                        //change by BIRD@hujingcheng 20190530
                        break;
                    case WEATHER_REQUEST_SUCCES:
                        WeatherInfo info = (WeatherInfo) msg.obj;
                        setWeatherText(info);
                        break;
                    case WEATHER_REQUEST_EMPTY:
                        setWeatherEmpty();
                        break;
                /*YUNOS END*/
				    case UPDATE_CARRIER_MESSAGE:
					    updateCarrierLayout();
					    break;
                    default:
                        break;

                }
            } catch (Exception e) {

            }
        }
    };
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.i(TAG, "WL_DEBUG onReceive action = " + action);
			if (action.equals(Intent.ACTION_DATE_CHANGED) || action.equals(Intent.ACTION_TIME_CHANGED)) {
				mHandler.sendEmptyMessage(DATE_CHANGE_MESSAGE);
				startWeatherService();
			} else if (action.equals("weather_update_broadcast")) {
				String district = intent.getStringExtra("district");
				String img = intent.getStringExtra("img");
				String temp = intent.getStringExtra("temp");
				WeatherInfo info = new WeatherInfo(district, img, temp);
				mHandler.obtainMessage(WEATHER_REQUEST_SUCCES, info).sendToTarget();
			} else if (action.equals("weather_empty_broadcast")) {
				mHandler.sendEmptyMessage(WEATHER_REQUEST_EMPTY);
			} else if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
				String reason = intent.getStringExtra("reason");
				if ("lock".equals(reason)) {
					if (mMode == ShowMode.MainMenu) {
						try {
							setMode(ShowMode.Launcher);
						} catch (Exception e) {

						}
					}
				}
			} else if (action.equals(ACTION_SIM_STATE_CHANGED)) {
				updateCarrierLayout();
/*** BUG #47186 wanglei 20190429 add begin ***/
			} else if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
				updateCarrierLayout();
/*** BUG #47186 wanglei 20190429 add end ***/
			}
		}
	};
    private void setWeatherEmpty() {
        cityTV.setText("");
        tmpTV.setText("");
        weatherIV.setImageDrawable(null);
    }
    private void setWeatherText(WeatherInfo info) {
        try {
            cityTV.setText(info.cityName);
            tmpTV.setText(info.temp + "°C");
            weatherIV.setImageResource(LoadImageUtil.getDrawableId(this, "ic_" + info.img));
        } catch (Exception e) {

        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dq_launcher_layout;
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        Log.i("czh","initData");
        mCurrentLocale = Locale.getDefault().toString();
        FavoriteStorage.get().loadFavorites(this, R.xml.default_items);
        FavoriteStorage.get().loadFliter(this);
        initViews();
//        setMainBackgroundDrawable();
        //change by BIRD@hujingcheng 20190530
        if(SHOW_LUNAR_DATE){
            setLunarText();
        }
        if(LAND_UI){
            setWeekText();
        }

		//carrierInfo
		updateCarrierLayout();
		
        configViewByMode();
        config();
        new UnreadInfoThread().start();
        registerContentObservers();
        registerReceiver();

		mAudioManager  = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mTelephonyManager = TelephonyManager.getDefault();//bug45926 add by wucheng 20190321
    }

    private void startWeatherService() {
        Intent intent = new Intent("simplehome.weather.service");
        intent.setPackage("simplehome.weather");
        startService(intent);
    }

	private void registerReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(Intent.ACTION_DATE_CHANGED);
		intentFilter.addAction(Intent.ACTION_TIME_CHANGED);
		intentFilter.addAction("weather_update_broadcast");
		intentFilter.addAction("weather_empty_broadcast");
		intentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		intentFilter.addAction(ACTION_SIM_STATE_CHANGED);
/*** BUG #47186 wanglei 20190429 add begin ***/
		intentFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
/*** BUG #47186 wanglei 20190429 add end ***/
		registerReceiver(mReceiver, intentFilter);
/*** BUG #42909 wanglei 20181107 add begin ***/
		IntentFilter packageIntentFilter = new IntentFilter();
		packageIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		packageIntentFilter.addDataScheme("package");
		registerReceiver(mPackageReceiver, packageIntentFilter);
/*** BUG #42909 wanglei 20181107 add end ***/
	}

    private void unRegisterReceiver() {
        try {
            if (mReceiver != null) {
                unregisterReceiver(mReceiver);
            }
        } catch (Exception e) {

        }
/*** BUG #42909 wanglei 20181107 add begin ***/
		try {
			if (mPackageReceiver != null) {
				unregisterReceiver(mPackageReceiver);
			}
		} catch (Exception e) {

		}
/*** BUG #42909 wanglei 20181107 add end ***/
    }

    private void config() {
        if (!Config.KEY_SUPPORT) {
            leftControl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doMenu();
                }
            });
            rightControl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    doBack();
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!needChange()) {
            if (SHOW_LUNAR_DATE && mCurrentLocale.contains("zh")) {//change by BIRD@hujingcheng 20190530
                lunarText.setVisibility(View.VISIBLE);
            } else {
                lunarText.setVisibility(View.GONE);
            }
            for (CellLayout cellLayout : mCellLayoutList) {
                cellLayout.setLocale(mCurrentLocale);
            }
        }
        findViewById(R.id.main_container).setVisibility(View.VISIBLE);
		mHandler.sendEmptyMessageDelayed(UPDATE_CARRIER_MESSAGE, 5*1000);
        Log.i(TAG, "WL_DEBUG onResume end");
    }

    private boolean needChange() {
        String locale = Locale.getDefault().toString();
        if (locale.equals(mCurrentLocale)) {
            return false;
        } else {
            mCurrentLocale = locale;
            return true;
        }
    }

    private void initViews() {
        lunarText = (TextView) findViewById(R.id.lunar_tv);
        timeText = (TextClock) findViewById(R.id.time_tv);
        weekText =(TextView)findViewById(R.id.week_tv);//mark
        mainMenuContainer = (LinearLayout) findViewById(R.id.main_menu_container);
        leftControl = (TextView) findViewById(R.id.bottom_left);
        rightControl = (TextView) findViewById(R.id.bottom_right);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        weatherIV = (ImageView) findViewById(R.id.weather_iv);
        cityTV = (TextView) findViewById(R.id.city_tv);
        tmpTV = (TextView) findViewById(R.id.tempt_tv);
        mCellLayoutList = new ArrayList<CellLayout>();
        int i = 0;
        for (Favorite favorite : FavoriteStorage.get().favorites) {
			if (isFavoriteAvailable(favorite)) {
				CellLayout cellLayout = new CellLayout(this);
				cellLayout.setFavorite(favorite);
				cellLayout.setLocale(mCurrentLocale);
				mCellLayoutList.add(cellLayout);
				if (i == 0) {
					cellLayout.setFocusable(true);
					cellLayout.requestFocus();
				}
				i++;
			}
        }
        QuickPageAdapter<CellLayout> adapter = new QuickPageAdapter<CellLayout>(mCellLayoutList);
        mViewPager.setAdapter(adapter);
        mViewPager.setOnPageChangeListener(this);
		
		//carrierInfo
		mCarrierInfoLayout = (LinearLayout) findViewById(R.id.carrierInfo_layout);
		
/*** wanglei 20190418 add begin ***/
		findViewById(R.id.base_bottom_layout).setBackgroundColor(Color.TRANSPARENT);
/*** wanglei 20190418 add end ***/
    }

    private void configViewByMode() {
/*** BUG #42501 wanglei 20181024 add begin ***/
    	Window window = getWindow();
/*** BUG #42501 wanglei 20181024 add end ***/
		Settings.System.putInt(getContentResolver(), HOME_CURRENT_POSITION, mMode);
        if (mMode == ShowMode.Launcher) {
/*** BUG #47420 wanglei 20190506 add begin ***/
			window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
					WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
/*** BUG #47420 wanglei 20190506 add end ***/
/*** BUG #42501 wanglei 20181024 add begin ***/
			window.setFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
					WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
/*** BUG #42501 wanglei 20181024 add end ***/
            // Apply data from current theme.
            //@ {bird: For fix bug#full screen, add by shicuiliang@szba-mobile.com 19-5-31.
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            //@ }
            mainMenuContainer.setVisibility(View.GONE);
            leftControl.setText(R.string.menu);
            rightControl.setText(R.string.agenew_shuortcut);//add by maoyufeng 20190418
            leftControl.setTextColor(Color.parseColor("#ffffff"));
            leftControl.setShadowLayer(2, 1, 1, Color.parseColor("#cc000000"));
            rightControl.setTextColor(Color.parseColor("#ffffff"));
            rightControl.setShadowLayer(2, 1, 1, Color.parseColor("#cc000000"));
        } else if (mMode == ShowMode.MainMenu) {
/*** BUG #47420 wanglei 20190506 add begin ***/
			window.setFlags(0, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
/*** BUG #47420 wanglei 20190506 add end ***/
/*** BUG #42501 wanglei 20181024 add begin ***/
        	window.setFlags(0, WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
/*** BUG #42501 wanglei 20181024 add end ***/
            //@ {bird: For fix bug#full screen, add by shicuiliang@szba-mobile.com 19-5-31.
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            //@ }
            mainMenuContainer.setVisibility(View.VISIBLE);
            leftControl.setText(R.string.ok);
            rightControl.setText(R.string.back);
            leftControl.setTextColor(Color.parseColor("#ffffff"));
            leftControl.setShadowLayer(2, 1, 1, Color.parseColor("#cc000000"));
            rightControl.setTextColor(Color.parseColor("#ffffff"));
            rightControl.setShadowLayer(2, 1, 1, Color.parseColor("#cc000000"));
			mCurrrentPosition = 0;
			mViewPager.setCurrentItem(0, false);
            try {
            	mCellLayoutList.get(0).requestFocus();
            	mCellLayoutList.get(0).requestFocusFromTouch();
            } catch (Exception e) {

            }
            /// bird: BUG #26490, peibaosheng @20170627 {
            Favorite favorite = FavoriteStorage.get().favorites.get(0);
            if (mCurrentLocale.contains("zh_CN")) {
                TextSpeech.readMenu(favorite.titleCN);
            } else if (mCurrentLocale.contains("zh_TW")) {
                TextSpeech.readMenu(favorite.titleTW);
            } else {
                TextSpeech.readMenu(favorite.titleEN);
            }
            /// @}
        }
    }
	private TextView getShowTextView(int phoneId,String text){		
		TextView textView = new TextView(this);
		textView.setText(text);
	    textView.setTextSize(26);
	    textView.setTextColor(0xfff8f8f8);
	textView.setShadowLayer	(2,0,0,Color.BLACK);
	    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
		    LinearLayout.LayoutParams.WRAP_CONTENT);  
        lp.gravity = Gravity.CENTER;  
        textView.setLayoutParams(lp);  
		if(phoneId>=0){
			Drawable drawable = getResources().getDrawable(phoneId == 0 ? R.drawable.sim_card1 : R.drawable.sim_card2);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight());  
            textView.setCompoundDrawables(drawable,null,null,null);
			textView.setCompoundDrawablePadding(10);
		}
		return textView;
		
	}
	private boolean noNeedUpdate = false;

	private void updateCarrierLayout() {
		mCarrierInfoLayout.removeAllViews();
		String tag = "";
        mSims = SimManager.get(this).getSims();
        SimManager simManager = SimManager.get(this);
		Log.d(TAG,"WL_DEBUG mSims.length:"+mSims.length);	
		
        if(mSims.length > 0){
			int mNumSlots = TelephonyManager.getPhoneCount();
		    Log.d(TAG,"mNumSlots:"+mNumSlots);
	        boolean isSim1CardExist = TelephonyManager.getDefault(SIM_CARD_1).hasIccCard();
		    boolean isSim2CardExist = false;
		    if(mNumSlots > 1){
	        	isSim2CardExist = TelephonyManager.getDefault(SIM_CARD_2).hasIccCard();
		    }
/*** BUG #47186 wanglei 20190429 add begin ***/
			boolean isAirPlaneModeOn = isAirPlaneModeOn();
			String airPlaneMode = getString(com.android.internal.R.string.global_actions_toggle_airplane_mode);
/*** BUG #47186 wanglei 20190429 add end ***/
            if(mSims.length == 1){
                if(isSim1CardExist || isSim2CardExist){
                    //@ {bird: For fix bug#47743, add by shicuiliang@szba-mobile.com 19-5-14.
                    String displayName = Settings.System.getString(getContentResolver(),
                            "bird_sim_name_" + mSims[SIM_CARD_1].getIccId());
                    Log.d(TAG, "displayName: " + displayName);

                    if (displayName != null && !"".equals(displayName)) {
                        mSims[SIM_CARD_1].setName(displayName);
                        simManager.setName(SIM_CARD_1, displayName, SimManager.NAME_SOURCE_USER_INPUT);
                    }
                    //@ }

/*** BUG #47186 wanglei 20190429 modify begin ***/
					String text = isAirPlaneModeOn ? airPlaneMode : mSims[SIM_CARD_1].getName();
					mCarrierInfoLayout.addView(getShowTextView(isSim1CardExist ? 0 : 1, text));
					tag += text;
/*** BUG #47186 wanglei 20190429 modify end ***/
                }
            } else if(mSims.length == 2){
                if(isSim1CardExist){
                    //@ {bird: For fix bug#47743, add by shicuiliang@szba-mobile.com 19-5-14.
                    String displayName = Settings.System.getString(getContentResolver(),
                            "bird_sim_name_" + mSims[SIM_CARD_1].getIccId());
                    Log.d(TAG, "displayName: " + displayName);

                    if (displayName != null && !"".equals(displayName)) {
                        mSims[SIM_CARD_1].setName(displayName);
                        simManager.setName(SIM_CARD_1, displayName, SimManager.NAME_SOURCE_USER_INPUT);
                    }
                    //@ }
/*** BUG #47186 wanglei 20190429 modify begin ***/
					String text = isAirPlaneModeOn ? airPlaneMode : mSims[SIM_CARD_1].getName();
					mCarrierInfoLayout.addView(getShowTextView(0, text));
					tag += text;
/*** BUG #47186 wanglei 20190429 modify end ***/
                }
                if(isSim2CardExist){
                    //@ {bird: For fix bug#47743, add by shicuiliang@szba-mobile.com 19-5-14.
                    String displayName = Settings.System.getString(getContentResolver(),
                            "bird_sim_name_" + mSims[SIM_CARD_2].getIccId());
                    Log.d(TAG, "displayName: " + displayName);

                    if (displayName != null && !"".equals(displayName)) {
                        mSims[SIM_CARD_2].setName(displayName);
                        simManager.setName(SIM_CARD_2, displayName, SimManager.NAME_SOURCE_USER_INPUT);
                    }
                    //@ }

/*** BUG #47186 wanglei 20190429 modify begin ***/
					String text = isAirPlaneModeOn ? airPlaneMode : mSims[SIM_CARD_2].getName();
					mCarrierInfoLayout.addView(getShowTextView(1, text));
					tag += text;
/*** BUG #47186 wanglei 20190429 modify end ***/
                }
            }
        } else {
			mCarrierInfoLayout.addView(getShowTextView(-1, getResources().getString(R.string.no_sim_card_note)));
			tag = "null";
        }
		/*SubscriptionManager subscriptionManager = (SubscriptionManager) getSystemService(
				TELEPHONY_SUBSCRIPTION_SERVICE);
		List<SubscriptionInfo> list = subscriptionManager.getActiveSubscriptionInfoList();
		if (list == null) {
			mCarrierInfoLayout.addView(getShowTextView(-1, getResources().getString(R.string.no_sim_card_note)));
			tag = "null";

		} else {
			for (SubscriptionInfo info : list) {
				if (info != null) {
					mCarrierInfoLayout
							.addView(getShowTextView(info.getSimSlotIndex(), info.getDisplayName().toString()));
					tag += info.getDisplayName().toString();
				}
			}
		}*/
		Log.i(TAG, "WL_DEBUG updateCarrierLayout tag = " + tag);
		if ((mCarrierInfoLayout.getTag() != null) && (!tag.contains("CARD"))
				&& (tag.equals(mCarrierInfoLayout.getTag()))) {
			noNeedUpdate = true;
		}
		mCarrierInfoLayout.setTag(tag);
		if (!noNeedUpdate) {
			mHandler.sendEmptyMessageDelayed(UPDATE_CARRIER_MESSAGE, 1000);
		} else {
			//mHandler.removeMessages(UPDATE_CARRIER_MESSAGE);
		}
	}
    private void setLunarText() {
		Patterns.update(this);
        if (lunarText != null) {
            Lunar lunar = new Lunar(Calendar.getInstance());
            lunarText.setText(lunar.getChineseLunar());
        }
        if (timeText != null){
            if (is24Hour()){
                timeText.setFormat24Hour(Patterns.clockView24);
            }else{
				timeText.setFormat12Hour(TextClockUtils.get12ModeFormat(0.3f, Patterns.clockView12));
            }
        }
    }

    //add by BIRD@hujingcheng 20190531 set customize weekday
    private void setWeekText(){
        if(weekText!=null){
            if( mCurrentLocale!=null && mCurrentLocale.contains("zh")){
                String weekStr = DateFormat.format("E",Calendar.getInstance()).toString();
                //Log.d(TAG,"weekStr="+weekStr);
                weekStr=weekStr.replace('周','（');
                weekStr=weekStr.concat("）");
                weekText.setText(weekStr);
            }
        }
    }

    private boolean is24Hour(){
        return false;//Settings.System.getString(this.getContentResolver(),
                //Settings.System.TIME_12_24).equals("24");
    }
	//add by maoyufeng 20190419 begin
    private long mLastDoMenuTime = -1;
    private static final int TRY_TO_KEYGUARD_TIMEOUT = 2000;
	//add by maoyufeng 20190419 end
    @Override
    protected void doMenu() {
        mLastDoMenuTime = System.currentTimeMillis(); //add by maoyufeng 20190419 end
/*** performance optimization wanglei 20180929 add begin ***/
		Log.i(TAG, "WL_DEBUG doMenu start");
/*** performance optimization wanglei 20180929 add end ***/
        if (mMode == ShowMode.Launcher) {
            setMode(ShowMode.MainMenu);
        } else {
            try {
                int index = mViewPager.getCurrentItem();
                Favorite favorite = getFavorite(index);
             /*   if ("zh_CN".equals(mCurrentLocale)) {
                    TextSpeech.read(favorite.titleCN, getApplicationContext());
                } else if ("zh_TW".equals(mCurrentLocale)) {
                    TextSpeech.read(favorite.titleTW, getApplicationContext());
                } else {
                    TextSpeech.read(favorite.titleEN, getApplicationContext());
                }*/

                Intent intent = new Intent();
                intent.setComponent(new ComponentName(favorite.packageName, favorite.className));
                if (favorite instanceof Folder) {
                    Folder folder = (Folder) favorite;
//                    intent.putExtra("favorite_index", index);
                    intent.putExtra("favorites", folder);
                }
				if (!favorite.packageName.equals(getPackageName())) {
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);// bird:BUG #42356 add by lizhenye 20181022
				}
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, R.string.app_not_found, Toast.LENGTH_SHORT).show();
            }

        }
    }

    @Override
    protected void doBack() {
        if (mMode == ShowMode.Launcher) {
						//add by maoyufeng 20190418 begin
            Intent intent = new Intent(this,com.bird.activity.ShortcutAvtivity.class);
            try{
                startActivity(intent);
                return;
            }catch (Exception e){

            }
			//add by maoyufeng 20190418 end
            intent.setPackage(getString(R.string.contacts_package));
            intent.setComponent(new ComponentName(getString(R.string.contacts_package), getString(R.string.contacts_activity)));
            startActivity(intent);
        } else {
            if (mainMenuContainer != null) {
                setMode(ShowMode.Launcher);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("czh", "ondestroy");
        unregisterContentObservers();
        unRegisterReceiver();
        //mViewPager.removeOnPageChangeListener(this);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
	    mCurrrentPosition = position;
        /// bird: BUG #26490, peibaosheng @20170627 {
        Favorite favorite = getFavorite(position);
        if ("zh_CN".equals(mCurrentLocale)) {
            TextSpeech.readMenu(favorite.titleCN);
        } else if ("zh_TW".equals(mCurrentLocale)) {
            TextSpeech.readMenu(favorite.titleTW);
        } else {
            TextSpeech.readMenu(favorite.titleEN);
        }
        /// @}

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private boolean mShortClick = true;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /* add by BIRD@hujingcheng 20190605 e516 function keys */
        Log.d(TAG, "onKeyDown");
        if(needBacktoSuper(keyCode)){
            return super.onKeyDown(keyCode,event);
        }
        /* add by BIRD@hujingcheng 20190605 e516 function keys end */
		//add by maoyufeng 20190419 begin 
	   if(keyCode == KeyEvent.KEYCODE_0){
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setPackage("com.android.dialer");
            startActivity(intent);
            return false;
        }
		//add by maoyufeng 20190419 end
        if(event.getAction() == KeyEvent.ACTION_DOWN){
            event.startTracking();
            if(event.getRepeatCount() == 0){
                mShortClick = true;
            }else{
                mShortClick = false;
            }
            if (!mShortClick) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP){
                    onLongPressUpKey(getApplicationContext());
                    return true;
                }else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN){
                    onLongPressDownKey(getApplicationContext());
                    return true;
                }
            }
        }
        return true;
    }


    private static void onLongPressDownKey(Context context) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setStreamVolume(AudioManager.STREAM_RING,
                mAudioManager.getStreamVolume(AudioManager.STREAM_RING), AudioManager.FLAG_SHOW_UI);
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_LOWER, AudioManager.FX_FOCUS_NAVIGATION_DOWN);
    }

    private static void onLongPressUpKey(Context context) {
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_RAISE, AudioManager.FX_FOCUS_NAVIGATION_UP);

    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        Log.d(TAG, "onKeyLongPress");
        mShortClick = false;
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
		//add by maoyufeng 20190419 begin
        if(System.currentTimeMillis() - mLastDoMenuTime <= TRY_TO_KEYGUARD_TIMEOUT && keyCode ==KeyEvent.KEYCODE_STAR){
            Intent intent = new Intent("com.android.internal.policy.impl.PhoneWindowManager.DO_KEYGUARD_NOW");
            sendBroadcast(intent); 
            //mLastDoMenuTime =System.currentTimeMillis();agenew:BUG #47201 remove by lizhenye 20190430
            mLastDoMenuTime = -1;//agenew:BUG #47201 add by lizhenye 20190430
            return true;
        }
        //mLastDoMenuTime =System.currentTimeMillis();agenew:BUG #47201 remove by lizhenye 20190430
		//add by maoyufeng 20190419 end
		Log.d(TAG, "onKeyUp mShortClick="+mShortClick+"keycode="+keyCode);
        if(!mShortClick){
            return true;
        }
        mShortClick = true;

//bird add by wucheng 20180814 begin
		if (mMode == ShowMode.Launcher) {
			switch (keyCode) {
				case KeyEvent.KEYCODE_DPAD_UP:
				case KeyEvent.KEYCODE_DPAD_DOWN:
				case KeyEvent.KEYCODE_DPAD_LEFT:
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					quickStartActivity(this,keyCode);
					return true;
			}
		}
//bird add by wucheng 20180814 end
        if (keyCode==KeyEvent.KEYCODE_CALL&&mMode==ShowMode.Launcher){
			//bug45926 add by wucheng 20190321 begin
			if(mTelephonyManager != null && mTelephonyManager.isIdle()) {
            	AndroidUtil.startCallLog(this);
			} else {			
				Intent intent = new Intent("android.intent.action.MAIN");
		        intent.setComponent(new ComponentName("com.android.dialer","com.android.incallui.InCallActivity"));
		        startActivity(intent);
			}		
			//bug45926 add by wucheng 20190321 end
            return true;
        } else if( keyCode== KeyEvent.KEYCODE_VOLUME_UP){
			//if(mAudioManager.isMusicActive()&&("com.android.music".equals(mAudioManager.getFocusedPackageName()))){
			//mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE,AudioManager.FX_FOCUS_NAVIGATION_UP);
			//}else{
            //mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_RAISE,AudioManager.FX_FOCUS_NAVIGATION_UP);
			//}
            return super.onKeyUp(keyCode, event);//change by BIRD@hujingcheng 20190605 function key e516
        }else if( keyCode ==KeyEvent.KEYCODE_VOLUME_DOWN){
			/*if(mAudioManager.isMusicActive()&&("com.android.music".equals(mAudioManager.getFocusedPackageName()))){
			     mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER,AudioManager.FX_FOCUS_NAVIGATION_DOWN);
			}else{
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_LOWER,AudioManager.FX_FOCUS_NAVIGATION_DOWN);
			}*/
            return super.onKeyUp(keyCode, event);//change by BIRD@hujingcheng 20190605 function key e516
		}
        Log.d(TAG,"onKeyUp end");
        return super.onKeyUp(keyCode, event);
    }

    protected void registerContentObservers() {
        mMmsSmsObserver = new UnreadMessageContentObserver(this, mHandler);
        this.getContentResolver().registerContentObserver(UnreadInfoUtil.MMSSMS_CONTENT_URI,
                true, mMmsSmsObserver);
        /*YUNOS BEGIN*/
        mCallObserver = new MissCallContentObserver(this, mHandler);
        this.getContentResolver().registerContentObserver(UnreadInfoUtil.CALLS_CONTENT_URI, true, mCallObserver);
        /*YUNOS END*/
    }

    protected void unregisterContentObservers() {
        this.getContentResolver().unregisterContentObserver(mMmsSmsObserver);
        /*YUNOS BEGIN*/
        this.getContentResolver().unregisterContentObserver(mCallObserver);
        /*YUNOS END*/
    }

    class UnreadInfoThread extends Thread {
        @Override
        public void run() {
            int messageCount = UnreadInfoUtil.getUnreadMessageCount(DqLauncher.this);
            mHandler.obtainMessage(UnreadInfoUtil.MMSSMS_UNREAD_MESSAGE, messageCount).sendToTarget();
            /*YUNOS BEGIN*/
            int callCount = UnreadInfoUtil.getMissedCallCount(DqLauncher.this);

            mHandler.obtainMessage(UnreadInfoUtil.MISS_CALL_MESSAGE, callCount).sendToTarget();
            /*YUNOS END*/
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (mainMenuContainer != null) {
            setMode(ShowMode.Launcher);
        }
    }

	//bird add by wucheng 20180814 begin
	private void quickStartActivity (Context context,int keycode) {
		String intent=QuickStartFeatureOption.getIntent(context,QuickStartFeatureOption.getStartKey(keycode));
		if(TextUtils.isEmpty(intent)){
			Log.i(TAG, "component is null");
			return ;
		}
		String[] name=intent.split("/");
		if(name!=null && name.length==2){
			KeyCodeEventUtil.startActivity(context, name[0], name[1]);
		}		
	}
	//bird add by wucheng 20180814 begin
	
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (mMode == ShowMode.MainMenu&&event.getAction()==KeyEvent.ACTION_DOWN) {

		    int moveToPos;
			switch (event.getKeyCode()) {
				case KeyEvent.KEYCODE_DPAD_UP:
				case KeyEvent.KEYCODE_DPAD_LEFT:
				    if(mCurrrentPosition==0){				 	   
					   moveToPos = mCellLayoutList.size()-1;
					}else{
					  moveToPos = mCurrrentPosition-1;
					}
				    mViewPager.setCurrentItem(moveToPos, false);
				    return true;
				case KeyEvent.KEYCODE_DPAD_DOWN:
				case KeyEvent.KEYCODE_DPAD_RIGHT:
				    if(mCurrrentPosition==mCellLayoutList.size()-1){				 	   
					   moveToPos = 0;
					}else{
					   moveToPos = mCurrrentPosition+1;
					}
				    mViewPager.setCurrentItem(moveToPos, false);
					return true;
			}
		}
        return super.dispatchKeyEvent(event);
    }
	
	private static final class Patterns {
        static String clockView12;
        static String clockView24;

        static void update(Context context) {
            final Locale locale = Locale.getDefault();
            final Resources res = context.getResources();
            final String clockView12Skel = res.getString(R.string.clock_12hr_format);
            final String clockView24Skel = res.getString(R.string.clock_24hr_format);

            clockView12 = DateFormat.getBestDateTimePattern(locale, clockView12Skel);

            //@ {bird:add by fanglongxiang 20180908
            //make am or pm after time in zh
            if (BirdFeatureOption.BIRD_AM_PM_AFTER_TIME) {
                clockView12 = "h:mm";
            }
            //@ }

            if (!clockView12.contains("a")) {
                    clockView12 += "a";
            }
            
            clockView24 = DateFormat.getBestDateTimePattern(locale, clockView24Skel);

            // Use fancy colon.
            clockView24 = clockView24.replace(':', '\uee01');
            clockView12 = clockView12.replace(':', '\uee01');

        }
    }
/*** performance optimization wanglei 20180929 add begin ***/
	@Override
	protected void onPause() {
		super.onPause();
		findViewById(R.id.main_container).setVisibility(View.GONE);
	}
/*** performance optimization wanglei 20180929 add end ***/
	
	private void setMode(int mode) {
		if (mMode != mode) {
			mMode = mode;
			configViewByMode();
		}
	}
/*** BUG #42909 wanglei 20181107 add begin ***/
	private BroadcastReceiver mPackageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (action.equals(Intent.ACTION_PACKAGE_REMOVED)) {
				String packageName = intent.getData().getSchemeSpecificPart();
				rebuildCellLayoutList();
			}
		}
	};

	private void rebuildCellLayoutList() {
		ArrayList<CellLayout> tempCellLayoutList = new ArrayList<CellLayout>();
		for (CellLayout cellLayout : mCellLayoutList) {
			if (!isFavoriteAvailable(cellLayout.getFavorite())) {
				tempCellLayoutList.add(cellLayout);
			}
		}
		if (tempCellLayoutList.size() > 0) {
			mCellLayoutList.removeAll(tempCellLayoutList);
			mViewPager.getAdapter().notifyDataSetChanged();
		}
	}

	private boolean isFavoriteAvailable(Favorite favorite) {
		final PackageManager packageManager = getPackageManager();
		Intent intent = new Intent();
		intent.setComponent(new ComponentName(favorite.packageName, favorite.className));
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	private Favorite getFavorite(int position) {
		return mCellLayoutList.get(position).getFavorite();
	}
/*** BUG #42909 wanglei 20181107 add end ***/
	
/*** BUG #47186 wanglei 20190429 add begin ***/
	private boolean isAirPlaneModeOn() {
		int mode = 0;
		try {
			mode = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON);
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		return mode == 1;
	}
/*** BUG #47186 wanglei 20190429 add end ***/
}
