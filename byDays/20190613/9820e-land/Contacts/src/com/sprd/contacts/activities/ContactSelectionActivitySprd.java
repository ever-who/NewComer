
package com.sprd.contacts.activities;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.Intents;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import com.android.contacts.common.util.AccountFilterUtil;
import android.provider.ContactsContract.Data;
import android.content.ContentValues;
import com.android.contacts.ContactSaveService;
import com.android.contacts.common.widget.ContextMenuAdapter;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.ValuesDelta;
import com.android.contacts.common.model.RawContactModifier;
import com.android.contacts.common.model.RawContactDelta;
import com.android.contacts.common.model.account.AccountWithDataSet;
import com.android.contacts.common.list.ContactListFilter;
import android.graphics.Color;
import android.app.Dialog;
import android.content.DialogInterface;
import android.app.DialogFragment;
import android.app.AlertDialog;
import com.android.contacts.ContactsActivity;
import com.android.contacts.ContactsApplication;
import com.android.contacts.R;
import com.android.contacts.group.GroupEditorFragment;
import com.android.contacts.common.util.Constants;
import com.android.contacts.common.list.ContactEntryListFragment;
import com.android.contacts.list.ContactPickerFragment;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.common.list.DirectoryListLoader;
import com.android.contacts.common.list.ContactListFilterController;
import com.android.contacts.common.list.ContactListFilterController.ContactListFilterListener;
import com.android.contacts.list.EmailAddressPickerFragment;
import com.android.contacts.list.LegacyPhoneNumberPickerFragment;
import com.android.contacts.list.OnContactPickerActionListener;
import com.android.contacts.list.OnEmailAddressPickerActionListener;
import com.android.contacts.common.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.list.OnPostalAddressPickerActionListener;
import com.android.contacts.common.list.PhoneNumberPickerFragment;
import com.android.contacts.list.PostalAddressPickerFragment;
import com.google.common.collect.Sets;
import com.sprd.contacts.common.util.UniverseUtils;
import com.sprd.contacts.list.OnAllInOneDataPickerActionListener;
import com.sprd.contacts.list.AllInOneDataPickerFragment;
import com.sprd.contacts.list.OnAllInOneDataMultiPickerActionListener;
import com.sprd.contacts.list.OnEmailAddressMultiPickerActionListener;
import com.sprd.contacts.common.list.OnPhoneNumberMultiPickerActionListener;
import com.sprd.contacts.list.OnContactMultiPickerActionListener;
import com.sprd.contacts.group.GroupDetailFragmentSprd;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;

import com.sprd.contacts.util.AccountsForMimeTypeUtils;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Parcel;
import android.widget.Toast;

public abstract class ContactSelectionActivitySprd extends ContactsActivity implements
        ContactListFilterController.ContactListFilterListener {
    private static final String TAG = "ContactSelectionActivitySprd";

    private static final int MAX_DATA_SIZE = 500000;

    private BroadcastReceiver mSelecStatusReceiver;

    public static final String MOVE_GROUP_COMPLETE = "move_group_member_completed";
    public static final String FILTER_CHANG = "filter_changed";

    private static final String KEY_FILTER = "mFilter";
    private static final String KEY_FILTER_CHANG = "mIsFilterChanged";

    private static final int SUBACTIVITY_ACCOUNT_FILTER = 6;
    public static final int SUBACTIVITY_BATCH_DELETE = 7;
    private static final int SUBACTIVITY_BATCH_IMPORT = 8;
    public static final int SUBACTIVITY_BATCH_EXPORT = 16;

    private ContactListFilterController mContactListFilterController;
    private ContactListFilter mPermanentAccountFilter = null;
    private ContactListFilter mFilter = null;
    private ContactListFilter mGroupFilter = null;
    private Button mDoneMenuItem;
    private int mDoneMenuDisableColor = Color.WHITE;
    private Long mSelectDataId;
    private int mAccountNum = 0;
    private AccountTypeManager mAccountManager;
    private boolean mIsFilterChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            mPermanentAccountFilter = (ContactListFilter) savedInstanceState
                    .getParcelable(KEY_FILTER);
            setupActionListener();
            mIsFilterChanged = savedInstanceState.getBoolean(KEY_FILTER_CHANG);
        }

        mContactListFilterController = ContactListFilterController.getInstance(this);
        mContactListFilterController.checkFilterValidity(false);
        mContactListFilterController.addListener(this);
        mFilter = mContactListFilterController.getFilter();

        mAccountManager = AccountTypeManager
                .getInstance(ContactSelectionActivitySprd.this);
        ArrayList<AccountWithDataSet> allAccounts = (ArrayList) mAccountManager
                .getAccounts(false);
        mAccountNum = allAccounts.size();
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter("com.android.contacts.common.action.SSU");
        mSelecStatusReceiver = new SSUReceiver();
        registerReceiver(mSelecStatusReceiver, filter);
        setDoneMenu(getListFragment().getSelecStatus());

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mSelecStatusReceiver);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_FILTER, mPermanentAccountFilter);
        if (mIsFilterChanged) {
            outState.putBoolean(KEY_FILTER_CHANG, mIsFilterChanged);
        }
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        // If we want "Create New Contact" button but there's no such a button
        // in the layout,
        // try showing a menu for it.
        final MenuItem newContactMenu = menu.findItem(R.id.create_new_contact);
        if (newContactMenu == null) {
            return true;
        }
        if (!(showCreateNewContactButton() && getCreateNewContactButton() == null)) {
            newContactMenu.setVisible(false);
        } else if (ContactsApplication.sApplication.isBatchOperation()
                || ContactSaveService.mIsGroupSaving) {
            newContactMenu.setEnabled(false);
        }
        return true;
    }

    private final class EmailAddressMultiPickerActionListener implements
            OnEmailAddressMultiPickerActionListener {
        public void onPickEmailAddressAction(HashMap<String, String> pairs) {
            returnPickerResult(pairs);
        }

        public void onCancel() {
            ContactSelectionActivitySprd.this.onBackPressed();
        }
    }

    private final class PhoneNumberMultiPickerActionListener implements
            OnPhoneNumberMultiPickerActionListener {
        public void onPickPhoneNumberAction(HashMap<String, String> pairs) {
            returnPickerResult(pairs);
        }

        public void onCancel() {
            ContactSelectionActivitySprd.this.onBackPressed();
        }
    }

    private final class AllInOneDataMultiPickerActionListener implements
            OnAllInOneDataMultiPickerActionListener {
        public void onPickAllInOneDataAction(HashMap<String, String> pairs) {
            returnPickerResult(pairs);
        }

        public void onCancel() {
            ContactSelectionActivitySprd.this.onBackPressed();
        }
    }

    private final class ContactMultiPickerActionListener implements
            OnContactMultiPickerActionListener {
        public void onPickContactAction(ArrayList<String> lookupKeys, ArrayList<String> ids) {
            returnPickerResult(lookupKeys, ids);
        }

        public void onCancel() {
            ContactSelectionActivitySprd.this.onBackPressed();
        }
    }

    private final class AllInOneDataPickerActionListener implements
            OnAllInOneDataPickerActionListener {
        @Override
        public void onPickAllInOneDataAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            returnPickerResult(intent);
        }

        public void onHomeInActionBarSelected() {
            ContactSelectionActivitySprd.this.onBackPressed();
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ContextMenuAdapter menuAdapter = getListFragment().getContextMenuAdapter();
        if (menuAdapter != null) {
            return menuAdapter.onContextItemSelected(item);
        }

        return super.onContextItemSelected(item);
    }

    public void returnPickerResult() {
        Intent intent = new Intent();
        if (mIsFilterChanged) {
            intent.putExtra(FILTER_CHANG, mIsFilterChanged);
        }
        setResult(RESULT_CANCELED, intent);
        finish();
    }

    public void returnPickerResult(ArrayList<String> data, ArrayList<String> data2) {
        Intent intent = getIntent();
        intent.putStringArrayListExtra("result", data);
        intent.putStringArrayListExtra("result_alternative", data2);
        intent.putExtra("filter", mContactListFilterController.getFilter().accountName);
        Parcel parcel = Parcel.obtain();
        intent.writeToParcel(parcel, 0);
        if (Constants.DEBUG) {
            Log.d(TAG, "returnPickerResult parcel size is" + parcel.dataSize());
        }
        if (parcel.dataSize() > MAX_DATA_SIZE) {
            Toast.makeText(ContactSelectionActivitySprd.this, R.string.transaction_too_large,
                    Toast.LENGTH_LONG).show();
            parcel.recycle();
            return;
        }
        if (parcel != null) {
            parcel.recycle();
        }

        returnPickerResult(intent);
    }

    public void returnPickerResult(HashMap<String, String> data) {
        Intent intent = new Intent();
        if (data.isEmpty()) {
            returnPickerResult();
        } else {
            intent.putExtra("result", data);
            returnPickerResult(intent);
        }
    }

    @Override
    public void onContactListFilterChanged() {
        ContactListFilter filter = mContactListFilterController.getFilter();
        // if (mFilter.equals(filter)) {
        // return;
        // }
        ArrayList<AccountWithDataSet> allAccounts = (ArrayList) mAccountManager
                .getAccounts(false);
        if (mPermanentAccountFilter != null && mAccountNum != allAccounts.size()) {
            // if the account information is changed,reconfigure list fragment
            mAccountNum = allAccounts.size();
            configureListFragment();
        } else {
            mFilter = filter;
            getListFragment().setFilter(mFilter);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        if (mIsFilterChanged) {
            intent.putExtra(FILTER_CHANG, mIsFilterChanged);
        }
        setResult(RESULT_CANCELED, intent);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (mContactListFilterController != null) {
            mContactListFilterController.removeListener(this);
        }
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SUBACTIVITY_ACCOUNT_FILTER) {
            AccountFilterUtil.handleAccountFilterResult(
                    mContactListFilterController, resultCode, data);
            mIsFilterChanged = true;
        }
    }

    protected void configureActionBar(ActionBar actionBar) {
        View customActionBarView = null;
        LayoutInflater inflater = (LayoutInflater) getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        customActionBarView = inflater.inflate(
                R.layout.editor_custom_action_bar_overlay, null);
        mDoneMenuItem = (Button) customActionBarView
                .findViewById(R.id.save_menu_item_button);
//        mDoneMenuItem
//                .setVisibility(getListFragment().isMultiPickerSupported() ? View.VISIBLE
//                        : View.GONE);
        mDoneMenuItem.setVisibility(View.GONE);
        mDoneMenuDisableColor = mDoneMenuItem.getCurrentTextColor();
        setDoneMenu(getListFragment().getSelecStatus());
        mDoneMenuItem.setText(R.string.menu_done);
        mDoneMenuItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getListFragment().onMultiPickerSelected();
            }
        });
        mDoneMenuItem
                .setVisibility(getListFragment().isMultiPickerSupported() ? View.VISIBLE
                        : View.GONE);
        Button cancelMenuItem = (Button) customActionBarView
                .findViewById(R.id.cancel_menu_item_button);
        cancelMenuItem.setVisibility(View.GONE);
        cancelMenuItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        actionBar.setTitle(R.string.contactPickerActivityTitle);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM
                | ActionBar.DISPLAY_SHOW_TITLE );
        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL
                        | Gravity.END));
    }

    protected void configureSearchActionBar(ActionBar actionBar) {
        View customActionBarView = null;
        LayoutInflater inflater = (LayoutInflater) getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        customActionBarView = inflater.inflate(R.layout.editor_custom_action_bar_overlay, null);
        mDoneMenuItem = (Button) customActionBarView.findViewById(R.id.save_menu_item_button);
        mDoneMenuItem.setVisibility(getListFragment().isMultiPickerSupported() ? View.VISIBLE
                : View.GONE);
        mDoneMenuDisableColor = mDoneMenuItem.getCurrentTextColor();
        setDoneMenu(false);
        mDoneMenuItem.setText(R.string.menu_done);
        mDoneMenuItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getIntent().getExtras() != null &&
                        getIntent().getExtras().getInt("mode") == SUBACTIVITY_BATCH_DELETE) {
                    ConfirmBatchDeleteDialogFragment cDialog = new ConfirmBatchDeleteDialogFragment();
                    cDialog.setTargetFragment(getListFragment(), 0);
                    cDialog.show(getFragmentManager(), null);
                } else {
                    getListFragment().onMultiPickerSelected();
                }
            }
        });
        mDoneMenuItem.setVisibility(getListFragment().isMultiPickerSupported() ? View.VISIBLE
                : View.GONE);
        View cancelMenuItem = customActionBarView.findViewById(R.id.cancel_menu_item_button);
        cancelMenuItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        actionBar.setTitle(R.string.contactPickerActivityTitle);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView, new ActionBar.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL | Gravity.END));
        mDoneMenuItem.setVisibility(View.GONE);
        cancelMenuItem.setVisibility(View.GONE);

    }

    protected Intent setPickerResultIntent(Intent intent) {
        if (mIsFilterChanged) {
            intent.putExtra(FILTER_CHANG, mIsFilterChanged);
        }
        return intent;
    }

    protected ContactEntryListFragment<?> configListFragment(int actionCode) {
        ContactEntryListFragment<?> listFragment = null;
        AccountWithDataSet account = getIntent().getParcelableExtra(Intents.Insert.ACCOUNT);
        if (account != null) {
            mPermanentAccountFilter = ContactListFilter.createAccountFilter(account.type,
                    account.name, null, null);
        }
        switch (actionCode) {
            case ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT:
            case ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                if (actionCode == ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT) {
                    fragment.setEditMode(false);
                } else {
                    fragment.setEditMode(true);
                }
                if (mPermanentAccountFilter != null) {
                    fragment.setPermanentFilter(mPermanentAccountFilter);
                }
                // check for mime-type capability
                ContactListFilter filter = getAccountFilterForMimeType(getIntent().getExtras());
                if (filter != null) {
                    if (AccountsForMimeTypeUtils.isAllAccountsForMimeType(
                            ContactSelectionActivitySprd.this, getIntent().getExtras())) {
                        mPermanentAccountFilter = null;
                        fragment.setFilter(mFilter);
                    } else {
                        mPermanentAccountFilter = filter;
                        fragment.setPermanentFilter(mPermanentAccountFilter);
                    }
                }
                fragment.setListRequestModeSelection("mode_dilar");
                fragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_NONE);
                listFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setIncludeProfile(getRequest().shouldIncludeProfile());
                if (getIntent().getBooleanExtra("no_sim", false)) {
                    AccountTypeManager am = AccountTypeManager
                            .getInstance(ContactSelectionActivitySprd.this);
                    ArrayList<AccountWithDataSet> allAccounts = (ArrayList) am
                            .getAccounts(false);
                    ArrayList<AccountWithDataSet> accounts = (ArrayList) allAccounts
                            .clone();
                    Iterator<AccountWithDataSet> iter = accounts.iterator();
                    while (iter.hasNext()) {
                        AccountWithDataSet accountWithDataSet = iter.next();
                        if (accountWithDataSet.type.equals("sprd.com.android.account.sim")
                                || accountWithDataSet.type.equals("sprd.com.android.account.usim")) {
                            iter.remove();
                        }
                    }
                    mPermanentAccountFilter = ContactListFilter
                            .createAccountsFilter(accounts);
                    fragment.setPermanentFilter(mPermanentAccountFilter);
                } else {
                    // check for mime-type capability
                    ContactListFilter filter = getAccountFilterForMimeType(getIntent().getExtras());
                    if (filter != null) {
                        if (AccountsForMimeTypeUtils.isAllAccountsForMimeType(
                                ContactSelectionActivitySprd.this, getIntent().getExtras())) {
                            mPermanentAccountFilter = null;
                            fragment.setFilter(mFilter);
                        } else {
                            mPermanentAccountFilter = filter;
                            fragment.setPermanentFilter(mPermanentAccountFilter);
                        }
                    }
                }
                listFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment(this);
                fragment.setShortcutRequested(true);
                AccountTypeManager am = AccountTypeManager
                        .getInstance(ContactSelectionActivitySprd.this);
                ArrayList<AccountWithDataSet> allAccounts = (ArrayList) am
                        .getAccounts(false);
                ArrayList<AccountWithDataSet> accounts = (ArrayList) allAccounts
                        .clone();
                Iterator<AccountWithDataSet> iter = accounts.iterator();
                while (iter.hasNext()) {
                    AccountWithDataSet accountWithDataSet = iter.next();
                    if (accountWithDataSet.type.equals("sprd.com.android.account.sim")
                            || accountWithDataSet.type.equals("sprd.com.android.account.usim")) {
                        iter.remove();
                    }
                }
                mPermanentAccountFilter = ContactListFilter
                        .createAccountsFilter(accounts);
                fragment.setPermanentFilter(mPermanentAccountFilter);
                listFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_MULTI_PICK_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment(this);
                fragment.setMultiPickerSupported(true);
                fragment.setCreateContactEnabled(false);
                fragment.setEditMode(false);
                if(getIntent().hasExtra("mode")) {
                    switch (getIntent().getExtras().getInt("mode")) {
                        case SUBACTIVITY_BATCH_DELETE:
                            fragment.setListRequestModeSelection("mode_delete");
                            break;
                        case GroupEditorFragment.GROUP_MEMBER_SELECT_MODE:
                            fragment.setListRequestModeSelection("mode_group_select");
                            break;
                        case SUBACTIVITY_BATCH_IMPORT:
                            fragment.setListRequestModeSelection("mode_copyto");
                            break;
                        case SUBACTIVITY_BATCH_EXPORT:
                            fragment.setListRequestModeSelection("mode_export_to");
                            break;
                        default:
                            break;
                    }
                }
                if (getIntent().hasExtra("src_account")) {
                    account = (AccountWithDataSet) (getIntent().getParcelableExtra("src_account"));
                    mPermanentAccountFilter = ContactListFilter.createAccountFilter(account.type,
                            account.name, null, null);
                    fragment.setAddGroupMemSelection(getIntent().getStringExtra(
                            GroupEditorFragment.CONTACTID_IN_GROUP));
                    fragment.setPermanentFilter(mPermanentAccountFilter);
                } else if (getIntent().hasExtra("dst_account")) {
                    AccountWithDataSet dstAcount = (AccountWithDataSet) getIntent()
                            .getExtra("dst_account");
                    AccountTypeManager am = AccountTypeManager
                            .getInstance(ContactSelectionActivitySprd.this);
                    ArrayList<AccountWithDataSet> allAccounts = (ArrayList) am
                            .getAccounts(false);
                    ArrayList<AccountWithDataSet> accounts = (ArrayList) allAccounts
                            .clone();
                    Iterator<AccountWithDataSet> iter = accounts.iterator();
                    while (iter.hasNext()) {
                        AccountWithDataSet accountWithDataSet = iter.next();
                        if (accountWithDataSet.name.equals(dstAcount.name)) {
                            iter.remove();
                        }
                    }
                    mPermanentAccountFilter = ContactListFilter
                            .createAccountsFilter(accounts);
                    fragment.setPermanentFilter(mPermanentAccountFilter);
                } else if (getIntent().hasExtra("setMulitStarred")) {
                    AccountTypeManager am = AccountTypeManager
                            .getInstance(ContactSelectionActivitySprd.this);
                    ArrayList<AccountWithDataSet> allAccounts = (ArrayList) am
                            .getAccounts(false);
                    ArrayList<AccountWithDataSet> accounts = (ArrayList) allAccounts
                            .clone();
                    Iterator<AccountWithDataSet> iter = accounts.iterator();
                    while (iter.hasNext()) {
                        AccountWithDataSet accountWithDataSet = iter.next();
                        if (accountWithDataSet.type
                                .equals("sprd.com.android.account.usim")
                                || accountWithDataSet.type
                                        .equals("sprd.com.android.account.sim")) {
                            iter.remove();
                        }
                    }
                    mPermanentAccountFilter = ContactListFilter
                            .createAccountsFilter(accounts);
                    fragment.setStarMemFlag();
                    fragment.setPermanentFilter(mPermanentAccountFilter);
                } else if (getIntent().hasExtra("move_group_member")) {
                    Long groupId = (Long) getIntent()
                            .getExtra(GroupDetailFragmentSprd.SRC_GROUP_ID);
                    mGroupFilter = ContactListFilter.createGroupFilter(groupId);
                    fragment.setFilter(mGroupFilter);
                } else if (getIntent().hasExtra("delete_group_member")) {
                    Long groupId = (Long) getIntent().getExtra("delete_group_member");
                    mGroupFilter = ContactListFilter.createGroupFilter(groupId);
                    fragment.setFilter(mGroupFilter);
                } else if (getIntent().hasExtra(ContactPickerFragment.MMS_MULTI_VCARD)) {
                    fragment.setFilter(mFilter);
                } else {
                    fragment.setFilter(mFilter);
                }
                listFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_MULTI_PICK_ALL_IN_ONE_DATA: {
                AllInOneDataPickerFragment fragment = new AllInOneDataPickerFragment();
                if (getIntent().hasExtra("select_group_member")) {
                    long groupId = (long) getIntent().getLongExtra("select_group_member", -1);
                    fragment.setFilter(ContactListFilter.createGroupFilter(groupId));
                } else {
                    fragment.setFilter(mFilter);
                }
                fragment.setMultiPickerSupported(true);
                fragment.setCascadingData(getRequest().getCascadingData());
                listFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_MULTI_PICK_PHONE: {
                PhoneNumberPickerFragment fragment = new PhoneNumberPickerFragment();
                fragment.setMultiPickerSupported(true);
                fragment.setFilter(mFilter);
                listFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_MULTI_PICK_EMAIL: {
                EmailAddressPickerFragment fragment = new EmailAddressPickerFragment();
                fragment.setMultiPickerSupported(true);
                fragment.setFilter(mFilter);
                listFragment = fragment;
                break;
            }

            default:
                break;
        }
        return listFragment;
    }

    protected void setActivityTitle(int actionCode) {
        switch (actionCode) {
            case ContactsRequest.ACTION_MULTI_PICK_CONTACT: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }
            case ContactsRequest.ACTION_MULTI_PICK_PHONE: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }
            case ContactsRequest.ACTION_MULTI_PICK_EMAIL: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }
            default:
                break;
        }
    }

    public void setupMultiActionListener(ContactEntryListFragment<?> mListFragment) {
        if (mListFragment instanceof ContactPickerFragment) {
            ((ContactPickerFragment) mListFragment).setOnContactMultiPickerActionListener(
                    new ContactMultiPickerActionListener());
        } else if (mListFragment instanceof PhoneNumberPickerFragment) {
            ((PhoneNumberPickerFragment) mListFragment).setOnPhoneNumberMultiPickerActionListener(
                    new PhoneNumberMultiPickerActionListener());
        } else if (mListFragment instanceof EmailAddressPickerFragment) {
            ((EmailAddressPickerFragment) mListFragment)
                    .setOnEmailAddressMultiPickerActionListener(
                    new EmailAddressMultiPickerActionListener());
        } else if (mListFragment instanceof AllInOneDataPickerFragment) {
            ((AllInOneDataPickerFragment) mListFragment)
                    .setOnAllInOneDataMultiPickerActionListener(
                    new AllInOneDataMultiPickerActionListener());
        } else {
            throw new IllegalStateException("Unsupported list fragment type: " + mListFragment);
        }
    }

    public void setupAllInOneActionListener(ContactEntryListFragment<?> mListFragment) {
        if (mListFragment instanceof AllInOneDataPickerFragment) {
            ((AllInOneDataPickerFragment) mListFragment)
                    .setOnAllInOneDataPickerActionListener(new AllInOneDataPickerActionListener());
        }
    }

    private ContactListFilter getAccountFilterForMimeType(Bundle extras) {
        ArrayList<AccountWithDataSet> tmp = AccountsForMimeTypeUtils.getAccountsForMimeType(
                ContactSelectionActivitySprd.this, extras);
        if (tmp.isEmpty()) {
            return null;
        }
        return ContactListFilter.createAccountsFilter(tmp);
    }

    public ContactListFilter getPermanentFilter() {
        return mPermanentAccountFilter;
    }

    public void setDoneMenu(boolean enabled) {
        if (mDoneMenuItem == null || !UniverseUtils.UNIVERSEUI_SUPPORT) {
            return;
        }
        if (enabled) {
            mDoneMenuItem.setEnabled(true);
            mDoneMenuItem.setTextColor(mDoneMenuDisableColor);
        } else {
            mDoneMenuItem.setEnabled(false);
            mDoneMenuItem.setTextColor(getResources().getColor(
                    R.color.action_bar_button_disable_text_color));
        }
    }

    public class SSUReceiver extends BroadcastReceiver {

        public void onReceive(final Context context, final Intent intent) {
            if (getListFragment() instanceof ContactEntryListFragment<?>) {
                boolean enabled = getListFragment().getSelecStatus();
                setDoneMenu(enabled);
            }

        }
    }

    public static class ConfirmBatchDeleteDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity())
                    .setTitle(R.string.batch_delete_confim_title)
                    .setMessage(R.string.batch_delete_confim_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,
                                        int which) {
                                    ((ContactEntryListFragment) getTargetFragment())
                                            .onMultiPickerSelected();
                                }
                            });
            return builder.create();
        }
    }

    public abstract ContactEntryListFragment<?> getListFragment();

    public abstract ContactsRequest getRequest();

    public abstract void configureListFragment();

    public abstract boolean showCreateNewContactButton();

    public abstract void setupActionListener();

    public abstract View getCreateNewContactButton();

    public abstract void returnPickerResult(Uri data);

    public abstract void returnPickerResult(Intent intent);
/*** BUG #46829 wanglei 20190418 add begin ***/
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean result = false;

		ContactEntryListFragment fragment = getListFragment();
		if (fragment != null) {
			result = fragment.onKeyDown(keyCode, event);
		}

		if (!result) {
			result = super.onKeyDown(keyCode, event);
		}
		return result;
	}
/*** BUG #46829 wanglei 20190418 add end ***/
}
