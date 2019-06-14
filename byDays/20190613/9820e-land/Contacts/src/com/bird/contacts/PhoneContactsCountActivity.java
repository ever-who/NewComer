package com.bird.contacts;

import com.android.contacts.R;
import com.bird.widget.Utils;
import com.sprd.android.support.featurebar.FeatureBarHelper;

import android.app.ActionBar;
import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.widget.TextView;

/**
 * @author wanglei BUG #45703
 */
public class PhoneContactsCountActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setDisplayShowTitleEnabled(true);
		}
		setContentView(R.layout.phone_contacts_count);
		FeatureBarHelper featureBarHelper = new FeatureBarHelper(this);
		TextView leftSkView = (TextView) featureBarHelper.getOptionsKeyView();
		TextView centerSkView = (TextView) featureBarHelper.getCenterKeyView();
		TextView rightSkView = (TextView) featureBarHelper.getBackKeyView();
		leftSkView.setText("");
		centerSkView.setText("");
		rightSkView.setText(R.string.softkey_back);
		Utils.getSubThreadHandler().post(new Runnable() {
			@Override
			public void run() {
				int count = 0;
				Cursor cursor = null;
				try {
					cursor = getContentResolver().query(Contacts.CONTENT_URI, null,
							"account_type != 'sprd.com.android.account.sim' AND account_type != 'sprd.com.android.account.usim'",
							null, null);
					count = cursor.getCount();
				} finally {
					if (cursor != null) {
						cursor.close();
					}
				}
				final int countFinal = count;
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						TextView tv = (TextView) findViewById(R.id.phone_contacts_count);
						tv.setText(String.valueOf(countFinal));
					}
				});
			}
		});
	}
}