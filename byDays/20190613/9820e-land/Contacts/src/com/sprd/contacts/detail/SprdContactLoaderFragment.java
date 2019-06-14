
package com.sprd.contacts.detail;

import com.android.contacts.detail.ContactLoaderFragment;

import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.content.ContentValues;

import java.util.ArrayList;

import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.RawContact;
import com.sprd.contacts.common.model.account.SimAccountType;
import com.sprd.contacts.common.model.account.USimAccountType;
import com.android.contacts.detail.ContactDetailDisplayUtils;

import android.content.ContentResolver;
import android.content.pm.PackageManager;

import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.model.dataitem.DataItem;
import com.android.contacts.common.model.dataitem.PhoneDataItem;

import android.database.Cursor;

import com.android.contacts.common.model.ContactLoader;
import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsApplication;
import com.android.contacts.R;
import com.android.contacts.activities.ContactDetailActivity.FragmentKeyListener;
import com.android.contacts.activities.ContactEditorActivity;
import com.android.contacts.activities.JoinContactActivity;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.common.list.ShortcutIntentBuilder;
import com.android.contacts.common.list.ShortcutIntentBuilder.OnShortcutIntentCreatedListener;
import com.android.contacts.common.util.Constants;
import com.android.contacts.common.util.ContactLoaderUtils;
import com.android.contacts.util.PhoneCapabilityTester;
import com.sprd.contacts.common.util.UniverseUtils;
import com.android.internal.util.Objects;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.Toast;
import android.content.Entity;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

public class SprdContactLoaderFragment extends ContactLoaderFragment {

    private static boolean VOICE_SUPPORT = true;

    private boolean mOptionsMenuOptions;
    private boolean mOptionsMenuEditable;
    private boolean mOptionsMenuFilterable;
    private boolean mCanCopy;
    private String displayName;
    private ArrayList<String> mPhones;
    private AccountTypeManager mAccountTypeManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPhones = new ArrayList<String>();
        VOICE_SUPPORT = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_voice_capable);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mAccountTypeManager = AccountTypeManager.getInstance(activity);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            inflater.inflate(R.menu.view_contact_new_ui, menu);
        } else {
            inflater.inflate(R.menu.view_contact, menu);
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        boolean mMenuEnabled = (!ContactsApplication.sApplication.isBatchOperation())
                && (!ContactSaveService.mIsGroupSaving);
        mOptionsMenuOptions = isContactOptionsChangeEnabled();
        mOptionsMenuEditable = isContactEditable()&& !mContactData.isSdnContact();;
        mOptionsMenuShareable = isContactShareable();
        mOptionsMenuFilterable = isContactFilterable();
        mOptionsMenuCanCreateShortcut = isContactCanCreateShortcut();

        AccountWithDataSet accountType = null;
        if (mContactData != null) {
            mSendToVoicemailState = mContactData.isSendToVoicemail();
            mCustomRingtone = mContactData.getCustomRingtone();
            accountType = mContactData.getAccount();
        }

        AccountTypeManager am = AccountTypeManager.getInstance(mContext);
        ArrayList<AccountWithDataSet> allAccounts = (ArrayList) am.getAccounts(true);
        // Hide telephony-related settings (ringtone, send to voicemail)
        // if we don't have a telephone
        final MenuItem optionsSendToVoicemail = menu.findItem(R.id.menu_send_to_voicemail);
        if (optionsSendToVoicemail != null && accountType != null) {
            optionsSendToVoicemail.setChecked(mSendToVoicemailState);
            if (SimAccountType.ACCOUNT_TYPE.equals(accountType.type)
                    || USimAccountType.ACCOUNT_TYPE.equals(accountType.type)) {
                optionsSendToVoicemail.setVisible(false);
            } else {
                optionsSendToVoicemail.setVisible(mOptionsMenuOptions);
            }
            optionsSendToVoicemail.setEnabled(mMenuEnabled);
        }

		final MenuItem optionsMMS = menu.findItem(R.id.menu_mms);
		if (optionsMMS != null) {
			boolean isMMSVisible = false;
			String phoneNumber = new String();
			if (mContactData == null) {
				return;
			}
			for (RawContact rawContact : mContactData.getRawContacts()) {
				for (DataItem dataItem : rawContact.getDataItems()) {
					if (dataItem instanceof PhoneDataItem) {
						PhoneDataItem phone = (PhoneDataItem) dataItem;
						phoneNumber = phone.getFormattedPhoneNumber();
					}
				}
			}
			if (!TextUtils.isEmpty(phoneNumber)) {
				isMMSVisible = true;
			}
			optionsMMS.setVisible(isMMSVisible);
		}

        final MenuItem optionsRingtone = menu.findItem(R.id.menu_set_ringtone);
        if (optionsRingtone != null) {
            boolean isRingtoneVisible = true;
            if (mContactData != null && accountType != null) {
                if (SimAccountType.ACCOUNT_TYPE.equals(accountType.type)
                        || USimAccountType.ACCOUNT_TYPE.equals(accountType.type)) {
                    isRingtoneVisible = false;
                }
            }
            optionsRingtone.setVisible(mOptionsMenuOptions && isRingtoneVisible);
            optionsRingtone.setEnabled(mMenuEnabled);
        }

        // filter-able? (has any phone number)
        final MenuItem filterMenu = menu.findItem(R.id.menu_add_to_black_list);
        final TelephonyManager telephonyManager = (TelephonyManager) mContext
                .getSystemService(Context.TELEPHONY_SERVICE);
        /**
         * SPRD Bug635371 The menu of "Add to blacklist" should not show when not install "CallFireWall"
         * {@
         * */
        if (telephonyManager.isSupportApplication(TelephonyManager.TYPE_CALL_FIRE_WALL) && isCallFireWallInstalled()) {
            /**
            * @}
            * */
            filterMenu.setVisible(mOptionsMenuFilterable);
            if (isContactBlocked()) {
                filterMenu.setTitle(R.string.menu_deleteFromBlacklist);
            } else {
                filterMenu.setTitle(R.string.menu_addToBlacklist);
            }
            filterMenu.setEnabled(mMenuEnabled);
        } else {
            if(filterMenu!=null){
                filterMenu.setVisible(false);
            }
        }

        // edit-able?
        final MenuItem editMenu = menu.findItem(R.id.menu_edit);
        editMenu.setVisible(mOptionsMenuEditable);
        editMenu.setEnabled(mMenuEnabled);

        // delete-able?
        final MenuItem deleteMenu = menu.findItem(R.id.menu_delete);
        deleteMenu.setVisible(mOptionsMenuEditable);
        deleteMenu.setEnabled(mMenuEnabled);
        final MenuItem shareMenu = menu.findItem(R.id.menu_share);
        shareMenu.setVisible(false);
        shareMenu.setEnabled(false);
        final MenuItem createContactShortcutMenu = menu.findItem(R.id.menu_create_contact_shortcut);
        createContactShortcutMenu.setVisible(false);
        createContactShortcutMenu.setEnabled(false);

        // copy-able?
        final MenuItem copyMenu = menu.findItem(R.id.menu_copy);
        if (mContactData != null && !mContactData.isUserProfile()
                && allAccounts != null && allAccounts.size() > 1) {
            mCanCopy = true;
        }
        if (PhoneCapabilityTester.isUsingTwoPanes(mContext)) {
            mCanCopy = false;
        }
        //SPRD: add for bug637682, NullPointerException when save empty contact
        if (mContactData != null && mContactData.isSdnContact()) {
            mCanCopy = false;
        }
        copyMenu.setTitle(R.string.copy);
        copyMenu.setVisible(mCanCopy);
        copyMenu.setEnabled(mMenuEnabled);
        if(mContactData != null && mContactData.isSdnContact()) {
            copyMenu.setVisible(false);
            editMenu.setVisible(false);
            deleteMenu.setVisible(false);
        }
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            final MenuItem starredMenuItem = menu.findItem(R.id.menu_star);
            starredMenuItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    // Toggle "starred" state
                    // Make sure there is a contact
                    if (mLookupUri != null) {
                        final boolean isStarred = starredMenuItem.isChecked();
                      if (mContactData != null) {
                                // To improve responsiveness, swap out the
                                // picture (and tag) in the UI already
                                ContactDetailDisplayUtils.configureStarredMenuItem(starredMenuItem,
                                        mContactData.isDirectoryEntry(),
                                        mContactData.isUserProfile(),
                                        !isStarred);
                            }
                            // Now perform the real save
                            Intent intent = ContactSaveService.createSetStarredIntent(
                                    mContext, mLookupUri, !isStarred);
                            mContext.startService(intent);
                        }
                    return true;
                }
            });
            // If there is contact data, update the starred state
            if (mContactData != null) {
                ContactDetailDisplayUtils.configureStarredMenuItem(starredMenuItem,
                        mContactData.isDirectoryEntry(), mContactData.isUserProfile(),
                        mContactData.getStarred());
            }
            if (accountType != null
                    && (SimAccountType.ACCOUNT_TYPE.equals(accountType.type)
                    || USimAccountType.ACCOUNT_TYPE.equals(accountType.type))) {
                starredMenuItem.setVisible(false);
            }
            starredMenuItem.setEnabled(mMenuEnabled);
            starredMenuItem.setVisible(false);
        }
        if(mContactData.isFdnContact()){
            editMenu.setVisible(false);
            deleteMenu.setVisible(false);
            //filterMenu.setVisible(false);
            optionsRingtone.setVisible(false);
            optionsSendToVoicemail.setVisible(false);
        }
    }

    /**
     * SPRD Bug635371 The menu of "Add to blacklist" should not show when not install "CallFireWall"
     * {@
     * */
    private boolean isCallFireWallInstalled(){
        boolean installed = false;
        try {
            mContext.getPackageManager().getPackageUid("com.sprd.firewall", 0);
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
        }
        return installed;
    }
    /**
    * @}
    * */
    public boolean isContactOptionsChangeEnabled() {
        if (VOICE_SUPPORT)
            return mContactData != null && !mContactData.isDirectoryEntry()
                    && !mContactData.isUserProfile()
                    && PhoneCapabilityTester.isPhone(mContext);
        else
            return false;
    }

    public boolean isContactEditable() {
        if (mContactData == null || mContactData.isDirectoryEntry()) {
            return false;
        }
        AccountWithDataSet account = mContactData.getAccount();
        if (account != null && !mAccountTypeManager.contains(account, true)) { 
            return false;
        }
        return true;
    }

    public boolean isContactFilterable() {
        mPhones.clear();
        if (mContactData == null) {
            return false;
        }
        ArrayList<ContentValues> cvs = mContactData.getAllContentValues();
        for (ContentValues cv : cvs) {
            String mimeType = cv.getAsString(Data.MIMETYPE);
            if (mimeType != null && mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                String phone = cv.getAsString(Phone.NUMBER);
                mPhones.add(phone);
            }
            if (mimeType != null && mimeType.equals(StructuredName.CONTENT_ITEM_TYPE)) {
                displayName = cv.getAsString(StructuredName.DISPLAY_NAME);
            }
        }
        return mPhones.size() >= 1 ? true : false;
    }

    public boolean isContactBlocked() {
        if (mContactData == null) {
            return false;
        }
        ArrayList<ContentValues> cvs = mContactData.getAllContentValues();
        for (ContentValues cv : cvs) {
            String mimeType = cv.getAsString(Data.MIMETYPE);
            if (mimeType != null && mimeType.equals(Phone.CONTENT_ITEM_TYPE)) {
                String phone = cv.getAsString(Phone.NUMBER);
                if (!ContactLoaderUtils.CheckIsBlackNumber(mContext, phone)) {
                    return false;
                }
            }

        }
        return true;
    }

    public boolean isContactCanCreateShortcut() {
        boolean isSimAccountContact = false;
        if (mContactData != null && mContactData.getAccount() != null) {
            if (SimAccountType.ACCOUNT_TYPE.equals(mContactData.getAccount().type)
                    || USimAccountType.ACCOUNT_TYPE.equals(mContactData.getAccount().type)) {
                isSimAccountContact = true;
            }
        }
        return mContactData != null && !mContactData.isUserProfile()
                && !mContactData.isDirectoryEntry()
                && !isSimAccountContact;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_copy: {
                if (mContactData == null)
                    return false;
                if (mListener != null && mContactData != null)
                    mListener.onCopyRequested(mContactData.getLookupKey());
                break;
            }
            case R.id.menu_add_to_black_list: {
                // if (mListener != null) mListener.onFilterRequested(mPhones);
                if (isContactBlocked()) {
                    if (mListener != null)
                        mListener.onNotFilterRequested(mPhones, displayName);
                } else {
                    if (mListener != null)
                        mListener.onFilterRequested(mPhones, displayName);

                }
                return true;
            }
            case R.id.menu_mms: {
                if (mContactData == null)
                    return false;
                if (mListener != null && mContactData != null)
                    mListener.onSendMMS();
                break;
            }
            default:
                super.onOptionsItemSelected(item);
                break;
        }
        return false;
    }

}
