package com.bird.contacts;

import android.os.SystemProperties;

public final class BirdFeatureOption {
/*** BUG #46860 wanglei 20190419 add begin ***/
	public static final boolean BIRD_CONTACTS_IMPORT_EXPORT = SystemProperties
			.getBoolean("ro.bird.contacts_import_export", false);
/*** BUG #46860 wanglei 20190419 add end ***/
/*** BUG #46878 wanglei 20190419 add begin ***/
	public static final boolean BIRD_CONTACTS_BLACKLIST = SystemProperties
			.getBoolean("ro.bird.contacts_blacklist", false);
/*** BUG #46878 wanglei 20190419 add end ***/
}