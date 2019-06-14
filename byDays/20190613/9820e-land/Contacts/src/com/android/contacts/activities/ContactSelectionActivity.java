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

package com.android.contacts.activities;

import android.app.ActionBar;
import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Intents.Insert;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;
import android.widget.SearchView.OnCloseListener;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.Toast;

import com.android.contacts.ContactsActivity;
import com.android.contacts.R;
import com.android.contacts.common.list.ContactEntryListFragment;
import com.android.contacts.list.ContactPickerFragment;
import com.android.contacts.list.ContactsIntentResolver;
import com.android.contacts.list.ContactsRequest;
import com.android.contacts.common.list.DirectoryListLoader;
import com.android.contacts.list.EmailAddressPickerFragment;
import com.android.contacts.list.LegacyPhoneNumberPickerFragment;
import com.android.contacts.list.OnContactPickerActionListener;
import com.android.contacts.list.OnEmailAddressPickerActionListener;
import com.android.contacts.common.list.OnPhoneNumberPickerActionListener;
import com.android.contacts.list.OnPostalAddressPickerActionListener;
import com.android.contacts.common.list.PhoneNumberPickerFragment;
import com.android.contacts.list.PostalAddressPickerFragment;
import com.google.common.collect.Sets;
import com.sprd.contacts.activities.ContactSelectionActivitySprd;
import com.sprd.contacts.activities.ContactSelectionActivitySprd.ConfirmBatchDeleteDialogFragment;
import com.sprd.contacts.common.CustomSearchView;
import com.sprd.contacts.common.util.UniverseUtils;
import com.sprd.contacts.list.AllInOneDataPickerFragment;
import com.sprd.android.support.featurebar.FeatureBarHelper;

import android.app.ActionBar.OnMenuVisibilityListener;
import android.widget.TextView;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.CheckBox;

import com.android.contacts.common.list.ContactListItemView;

import android.widget.ListView;
import android.text.TextWatcher;
import android.text.Editable;
import android.content.ComponentName;
import android.database.Cursor;

import java.util.Set;

/**
 * Displays a list of contacts (or phone numbers or postal addresses) for the
 * purposes of selecting one.
 */
/*
* SPRD:
*
* @orig
* public class ContactSelectionActivity extends ContactsActivity
*
* @{
*/
public class ContactSelectionActivity extends ContactSelectionActivitySprd
/*
* @}
*/
        implements View.OnCreateContextMenuListener, OnQueryTextListener, OnClickListener,
        OnCloseListener, OnFocusChangeListener, ContactEntryListFragment.OnChangeListener {
    private static final String TAG = "ContactSelectionActivity";

    private static final int SUBACTIVITY_ADD_TO_EXISTING_CONTACT = 0;

    private static final String KEY_ACTION_CODE = "actionCode";
    private static final int DEFAULT_DIRECTORY_RESULT_LIMIT = 20;
    private static final int SUBACTIVITY_BATCH_DELETE = 7;
    private static final int SUBACTIVITY_BATCH_IMPORT = 8;
    private static final int SUBACTIVITY_SHARE_BY_SMS = 12;
    private static final int SUBACTIVITY_BATCH_EXPORT = 16;
    private static final int SUBACTIVITY_SHARE_VISIBLED = 14;
    private static final int SUBACTIVITY_MULTI_PICK = 15;

    // Delay to allow the UI to settle before making search view visible
    private static final int FOCUS_DELAY = 200;

    private ContactsIntentResolver mIntentResolver;
    protected ContactEntryListFragment<?> mListFragment;

    private int mActionCode = -1;

    private ContactsRequest mRequest;
    private FeatureBarHelper mFeatureBarHelper;
    private TextView  mCenterSkView;
    private TextView mLeftSkView;
    private TextView mRightSkView;
    private EditText searchView;

    /**
     * Can be null. If null, the "Create New Contact" button should be on the menu.
     */
    private View mCreateNewContactButton;

    public ContactSelectionActivity() {
        mIntentResolver = new ContactsIntentResolver(this);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        if (fragment instanceof ContactEntryListFragment<?>) {
            mListFragment = (ContactEntryListFragment<?>) fragment;
            setupActionListener();
        }
    }

    private CustomSearchView mSearchView;
    private Cursor mData;

    @Override
    public void onDataChange(Cursor data) {
        mData = data;
        if (mData != null && mData.getCount() == 0) {
            mCenterSkView.setText("");
        }
    }

    @Override
    protected void onCreate(Bundle savedState) {
        super.onCreate(savedState);

        if (savedState != null) {
            mActionCode = savedState.getInt(KEY_ACTION_CODE);
        }
        // Extract relevant information from the intent
        mRequest = mIntentResolver.resolveIntent(getIntent());
        if (!mRequest.isValid()) {
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        Intent redirect = mRequest.getRedirectIntent();
        if (redirect != null) {
            // Need to start a different activity
            startActivity(redirect);
            finish();
            return;
        }

        configureActivityTitle();

        setContentView(R.layout.contact_picker);
        mFeatureBarHelper = new FeatureBarHelper(this);
        mLeftSkView = (TextView)mFeatureBarHelper.getOptionsKeyView() ;
        mCenterSkView =(TextView)mFeatureBarHelper.getCenterKeyView();
        mRightSkView = (TextView)mFeatureBarHelper.getBackKeyView();
        /**
         * SPRD:Bug673006 it will be disappeared when enter contacts and click softkey firstly
         * @{
         */
        //mCenterSkView.setText("");
        /**
         * @}
         */
        if (mActionCode != mRequest.getActionCode()) {
            mActionCode = mRequest.getActionCode();
            configureListFragment();
        }

        prepareSearchViewAndActionBar();
        mCreateNewContactButton = findViewById(R.id.new_contact);
        if (mCreateNewContactButton != null) {
            if (shouldShowCreateNewContactButton()) {
                mCreateNewContactButton.setVisibility(View.VISIBLE);
                mCreateNewContactButton.setOnClickListener(this);
            } else {
                mCreateNewContactButton.setVisibility(View.GONE);
            }
        }

        mListFragment.setOnChangeListener(this);
        getActionBar().addOnMenuVisibilityListener(new OnMenuVisibilityListener() {
            @Override
            public void onMenuVisibilityChanged(boolean isVisible) {
                if (isVisible) {
                    mLeftSkView.setText("");
                    mCenterSkView.setText(R.string.default_feature_bar_center);
                    mRightSkView.setText(R.string.softkey_cancel);
                } else {
                    /**SPRD Bug643037 monky test, contacts occur the crash
                     * {@
                     * */
                    try {
                        View view = getCurrentFocus();
                        if (view instanceof ListView) {
                            ContactListItemView mListed = (ContactListItemView)((ListView)view).getSelectedView();
                            if (mListed != null && mListed.getNameTextView()!= null){
                                mCenterSkView.setText(R.string.default_feature_bar_center);
                            } else {
                                mCenterSkView.setText("");
                            }
                        } else if (view instanceof EditText) {
                            if (searchView == null) {
                                searchView = (EditText) view;
                                searchView.addTextChangedListener(mDigitsTextListener);
                            }
                            mCenterSkView.setText("");
                        } else if (view instanceof CheckBox) {
                            mCenterSkView.setText(R.string.default_feature_bar_center);
                        } else {
                            mCenterSkView.setText("");
                        }
                    } catch (Exception e) {
                        Log.d(TAG, e.toString());
                        //mCenterSkView.setText("");
                    }
                    /**
                     * @}
                     */
                    mLeftSkView.setText(R.string.softkey_option);
                    /**
                     * SPRD:Bug 638587,647463 it occured transient jump under the focus from searchView to listView
                     * SPRD Bug647285 Sofkey display incorrect when search contacts {@
                     */
                    if (searchView != null && !TextUtils.isEmpty(searchView.getText()) && searchView.isFocused()){
                        int postion = searchView.getSelectionStart();
                        if (postion > 0) {
                            mRightSkView.setText(R.string.softkey_clear);
                        } else {
                            mRightSkView.setText(R.string.softkey_back);
                        }
                    }else{
                        mRightSkView.setText(R.string.softkey_back);
                    }
                    /**
                     * @}
                     */
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSearchView = (CustomSearchView) mListFragment.getView().findViewById(com.android.contacts.common.R.id.search_view);
        mSearchView.setOnChangeListener(new CustomSearchView.OnChangeListener() {
            @Override
            public void onChange(boolean focus, int position) {
                if (focus) {
                    if (position > 0) {
                        mRightSkView.setText(R.string.softkey_clear);
                    } else {
                        mRightSkView.setText(R.string.softkey_back);
                    }
                    mCenterSkView.setText("");
                } else {
                    if (mData != null && mData.getCount() == 0) {
                        mCenterSkView.setText("");
                    } else {
                        mCenterSkView.setText(R.string.default_feature_bar_center);
                    }
                    mRightSkView.setText(R.string.softkey_back);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCenterSkView.setText(R.string.default_feature_bar_center);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSearchView.unSetOnChangeListener();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mListFragment.unSetOnChangeListener();
    }


    private final TextWatcher mDigitsTextListener = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (searchView != null && !TextUtils.isEmpty(searchView.getText())){
                mRightSkView.setText(R.string.softkey_clear);
            } else {
                mRightSkView.setText(R.string.softkey_back);
            }
        }
        @Override
        public void afterTextChanged(Editable s) {
        }

    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        return super.dispatchKeyEvent(event);
    }

    private boolean shouldShowCreateNewContactButton() {
        return (mActionCode == ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT
                || (mActionCode == ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT
                        && !mRequest.isSearchMode()));
    }

    private void prepareSearchViewAndActionBar() {
        // Postal address pickers (and legacy pickers) don't support search, so just show
        // "HomeAsUp" button and title.
        if (mRequest.getActionCode() == ContactsRequest.ACTION_PICK_POSTAL ||
                mRequest.isLegacyCompatibilityMode()) {
            //findViewById(R.id.search_view).setVisibility(View.GONE);
            final ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                /*
                * SPRD:
                *
                * @{
                */
                if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                    configureActionBar(actionBar);
                    return;
                }
                /*
                * @}
                */
                actionBar.setDisplayShowHomeEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setDisplayShowTitleEnabled(true);
            }
            return;
        }

        // If ActionBar is available, show SearchView on it. If not, show SearchView inside the
        // Activity's layout.
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            /*final View searchViewOnLayout = findViewById(R.id.search_view);
            if (searchViewOnLayout != null) {
                searchViewOnLayout.setVisibility(View.GONE);
            }*/
            /*
            * SPRD:
            * 
            * @{
            */
            if (UniverseUtils.UNIVERSEUI_SUPPORT){
                if (!isFinishing()) {
                    configureSearchActionBar(actionBar);
                }
                return;
            }
            /*
            * @}
            */

            /*final View searchViewContainer = LayoutInflater.from(actionBar.getThemedContext())
                    .inflate(R.layout.custom_action_bar, null);
            mSearchView = (SearchView) searchViewContainer.findViewById(R.id.search_view);*/

            // In order to make the SearchView look like "shown via search menu", we need to
            // manually setup its state. See also DialtactsActivity.java and ActionBarAdapter.java.
            /*mSearchView.setIconifiedByDefault(true);
            mSearchView.setQueryHint(getString(R.string.hint_findContacts));
            mSearchView.setIconified(false);

            mSearchView.setOnQueryTextListener(this);
            mSearchView.setOnCloseListener(this);
            mSearchView.setOnQueryTextFocusChangeListener(this);

            actionBar.setCustomView(searchViewContainer,
                    new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));*/
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        } else {
            /*mSearchView = (SearchView) findViewById(R.id.search_view);
            mSearchView.setQueryHint(getString(R.string.hint_findContacts));
            mSearchView.setOnQueryTextListener(this);

            // This is a hack to prevent the search view from grabbing focus
            // at this point.  If search view were visible, it would always grabs focus
            // because it is the first focusable widget in the window.
            mSearchView.setVisibility(View.INVISIBLE);
            mSearchView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mSearchView.setVisibility(View.VISIBLE);
                }
            }, FOCUS_DELAY);*/
        }

        // Clear focus and suppress keyboard show-up.
        //mSearchView.clearFocus();

        //mSearchView.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // If we want "Create New Contact" button but there's no such a button in the layout,
        // try showing a menu for it.
//        if (shouldShowCreateNewContactButton() && mCreateNewContactButton == null) {
//            MenuInflater inflater = getMenuInflater();
//            inflater.inflate(R.menu.contact_picker_options, menu);
//        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.contact_picker_options, menu);
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final MenuItem doneMenu = menu.findItem(R.id.menu_done);
        boolean mMessageCalling = false;
        ComponentName callingActivity = getCallingActivity();
        if (callingActivity != null) {
            String className = callingActivity.getShortClassName();
            if (className.endsWith("ConversationActivity")) {
                mMessageCalling = true;
            }
        }
        /**SPRD Bug635563 NullPointerException occured when enter Family number
         * {@
         * */
        if (getIntent() != null  && getIntent().getExtras() != null ) {
            if (getIntent().getExtras().getInt("mode") == SUBACTIVITY_BATCH_DELETE
                    || getIntent().getExtras().getInt("mode") == SUBACTIVITY_SHARE_BY_SMS
                    || getIntent().getExtras().getInt("mode") == SUBACTIVITY_BATCH_IMPORT
                    || getIntent().getExtras().getInt("mode") == SUBACTIVITY_BATCH_EXPORT
                    || getIntent().getExtras().getInt("mode") == SUBACTIVITY_SHARE_VISIBLED
                    || getIntent().getExtras().getInt("mode") == SUBACTIVITY_MULTI_PICK
                /**
                 * SPRD Bug639595 The doneMenu should be gone if the contact is selected
                 * {@
                 * */
                    || getIntent().getExtras().getInt("mode") == SUBACTIVITY_ADD_TO_EXISTING_CONTACT ) {
                /**
                 * @}
                 * */
                /**SPRD Bug637119 The doneMenu should be gray if no contacts were selected
                 * {@
                 * */
                if(getListFragment().getAdapter() != null && getListFragment().getAdapter().getCurrentCheckedItems() != null
                   && getListFragment().getAdapter().getCurrentCheckedItems().size() <= 0){
                    doneMenu.setEnabled(false);
                    /**
                     * SPRD Bug639595 The doneMenu should be gone if the contact is selected
                     * SPRD Bug640860 The doneMenu should be visible except in the mode of "SUBACTIVITY_ADD_TO_EXISTING_CONTACT"
                     * {@
                     * */
                    if(getIntent().getExtras().getInt("mode") == SUBACTIVITY_ADD_TO_EXISTING_CONTACT ){
                        doneMenu.setVisible(false);
                    }
                    /**
                     * @}
                     * */
                }else{
                    doneMenu.setEnabled(true);
                    doneMenu.setVisible(true);
                }
            }
            /**
             * @}
             * */
        } else if (mActionCode == ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT) {
            doneMenu.setVisible(false);
        } else if (mMessageCalling) {
            doneMenu.setVisible(true);
        } else {
            doneMenu.setVisible(false);
            if (mLeftSkView != null) {
                mLeftSkView.setText("");
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Go back to previous screen, intending "cancel"
                setResult(RESULT_CANCELED);
                finish();
                return true;
            case R.id.create_new_contact: {
                startCreateNewContactActivity();
                return true;
            }
            case R.id.menu_done: {
                if (getIntent().getExtras() != null &&
                        getIntent().getExtras().getInt("mode") == SUBACTIVITY_BATCH_DELETE) {
                    ConfirmBatchDeleteDialogFragment cDialog = new ConfirmBatchDeleteDialogFragment();
                    cDialog.setTargetFragment(getListFragment(), 0);
                    cDialog.show(getFragmentManager(), null);
                } else {
                    getListFragment().onMultiPickerSelected();
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_ACTION_CODE, mActionCode);
    }

    private void configureActivityTitle() {
        if (!TextUtils.isEmpty(mRequest.getActivityTitle())) {
            setTitle(mRequest.getActivityTitle());
            return;
        }

        int actionCode = mRequest.getActionCode();
        switch (actionCode) {
            case ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_PICK_CONTACT: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT: {
                setTitle(R.string.shortcutActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_PICK_PHONE: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_PICK_EMAIL: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CALL: {
                setTitle(R.string.callShortcutActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_SMS: {
                setTitle(R.string.messageShortcutActivityTitle);
                break;
            }

            case ContactsRequest.ACTION_PICK_POSTAL: {
                setTitle(R.string.contactPickerActivityTitle);
                break;
            }
            /*
            * SPRD:
            *
            * @{
            */
            default :
                setActivityTitle(actionCode);
                break;
            /*
            * @}
            */
        }
    }

    /**
     * Creates the fragment based on the current request.
     */
    public void configureListFragment() {
        /**
        * SPRD:
        * 
        * @{
        */
        mListFragment = configListFragment(mActionCode);
        if (mListFragment != null) {
            mListFragment.setLegacyCompatibilityMode(mRequest.isLegacyCompatibilityMode());
            mListFragment.setDirectoryResultLimit(DEFAULT_DIRECTORY_RESULT_LIMIT);

            getFragmentManager().beginTransaction()
                    .replace(R.id.list_container, mListFragment)
                    .commitAllowingStateLoss();
            return;
        }
        /**
        * @}
        */
        switch (mActionCode) {
            case ContactsRequest.ACTION_INSERT_OR_EDIT_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setEditMode(true);
                fragment.setDirectorySearchMode(DirectoryListLoader.SEARCH_MODE_NONE);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_DEFAULT:
            case ContactsRequest.ACTION_PICK_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setIncludeProfile(mRequest.shouldIncludeProfile());
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_OR_CREATE_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CONTACT: {
                ContactPickerFragment fragment = new ContactPickerFragment();
                fragment.setShortcutRequested(true);
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_PHONE: {
                PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
                fragment.setListRequestModeSelection("mode_pick");
                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_EMAIL: {
                mListFragment = new EmailAddressPickerFragment();
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_CALL: {
                PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
                fragment.setShortcutAction(Intent.ACTION_CALL);

                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_CREATE_SHORTCUT_SMS: {
                PhoneNumberPickerFragment fragment = getPhoneNumberPickerFragment(mRequest);
                fragment.setShortcutAction(Intent.ACTION_SENDTO);

                mListFragment = fragment;
                break;
            }

            case ContactsRequest.ACTION_PICK_POSTAL: {
                PostalAddressPickerFragment fragment = new PostalAddressPickerFragment();
                mListFragment = fragment;
                break;
            }

            default:
                /*
                * SPRD:
                *   Bug 296790
                *   CRASH: com.android.contacts (java.lang.IllegalStateException: Invalid action code: 10).
                *
                * @orig
                * throw new IllegalStateException("Invalid action code: " + mActionCode);
                *
                * @{
                */
                if (!isFinishing()) {
                    Log.e(TAG, "Invalid action code: " + mActionCode);
                    finish();
                    return;
                }
                /*
                * @}
                */
        }

        // Setting compatibility is no longer needed for PhoneNumberPickerFragment since that logic
        // has been separated into LegacyPhoneNumberPickerFragment.  But we still need to set
        // compatibility for other fragments.
        mListFragment.setLegacyCompatibilityMode(mRequest.isLegacyCompatibilityMode());
        mListFragment.setDirectoryResultLimit(DEFAULT_DIRECTORY_RESULT_LIMIT);

        getFragmentManager().beginTransaction()
                .replace(R.id.list_container, mListFragment)
                .commitAllowingStateLoss();
    }

    private PhoneNumberPickerFragment getPhoneNumberPickerFragment(ContactsRequest request) {
        if (mRequest.isLegacyCompatibilityMode()) {
            return new LegacyPhoneNumberPickerFragment();
        } else {
            return new PhoneNumberPickerFragment();
        }
    }

    public void setupActionListener() {
        /*
        * SPRD:
        *
        * @{
        */
        if (mListFragment.isMultiPickerSupported()) {
            setupMultiActionListener(mListFragment);
            return;
        } else if (mListFragment instanceof AllInOneDataPickerFragment) {
            setupAllInOneActionListener(mListFragment);
            return;
        }
        /*
        * @}
        */
        if (mListFragment instanceof ContactPickerFragment) {
            ((ContactPickerFragment) mListFragment).setOnContactPickerActionListener(
                    new ContactPickerActionListener());
        } else if (mListFragment instanceof PhoneNumberPickerFragment) {
            ((PhoneNumberPickerFragment) mListFragment).setOnPhoneNumberPickerActionListener(
                    new PhoneNumberPickerActionListener());
        } else if (mListFragment instanceof PostalAddressPickerFragment) {
            ((PostalAddressPickerFragment) mListFragment).setOnPostalAddressPickerActionListener(
                    new PostalAddressPickerActionListener());
        } else if (mListFragment instanceof EmailAddressPickerFragment) {
            ((EmailAddressPickerFragment) mListFragment).setOnEmailAddressPickerActionListener(
                    new EmailAddressPickerActionListener());
        } else {
            throw new IllegalStateException("Unsupported list fragment type: " + mListFragment);
        }
    }

    private final class ContactPickerActionListener implements OnContactPickerActionListener {
        @Override
        public void onCreateNewContactAction() {
            startCreateNewContactActivity();
        }

        @Override
        public void onEditContactAction(Uri contactLookupUri) {
            /*
            * SPRD:
            *
            * @orig
            *  Bundle extras = getIntent().getExtras();
            *   if (launchAddToContactDialog(extras)) {
            *    // Show a confirmation dialog to add the value(s) to the existing contact.
            *    Intent intent = new Intent(ContactSelectionActivity.this,
            *            ConfirmAddDetailActivity.class);
            *    intent.setData(contactLookupUri);
            *    if (extras != null) {
            *        // First remove name key if present because the dialog does not support name
            *        // editing. This is fine because the user wants to add information to an
            *        // existing contact, who should already have a name and we wouldn't want to
            *        // override the name.
            *        extras.remove(Insert.NAME);
            *        intent.putExtras(extras);
            *    }

            *    // Wait for the activity result because we want to keep the picker open (in case the
            *    // user cancels adding the info to a contact and wants to pick someone else).
            *    startActivityForResult(intent, SUBACTIVITY_ADD_TO_EXISTING_CONTACT);
            *    } else {
            *    // Otherwise launch the full contact editor.
            *    startActivityAndForwardResult(new Intent(Intent.ACTION_EDIT, contactLookupUri));
            *    }
            *
            * @{
            */
            startActivityAndForwardResult(new Intent(Intent.ACTION_EDIT, contactLookupUri));
            /*
            * @}
            */
        }

        @Override
        public void onPickContactAction(Uri contactUri) {
            returnPickerResult(contactUri);
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            returnPickerResult(intent);
        }

        /**
         * Returns true if is a single email or single phone number provided in the {@link Intent}
         * extras bundle so that a pop-up confirmation dialog can be used to add the data to
         * a contact. Otherwise return false if there are other intent extras that require launching
         * the full contact editor. Ignore extras with the key {@link Insert.NAME} because names
         * are a special case and we typically don't want to replace the name of an existing
         * contact.
         */
        private boolean launchAddToContactDialog(Bundle extras) {
            if (extras == null) {
                return false;
            }

            // Copy extras because the set may be modified in the next step
            Set<String> intentExtraKeys = Sets.newHashSet();
            intentExtraKeys.addAll(extras.keySet());

            // Ignore name key because this is an existing contact.
            if (intentExtraKeys.contains(Insert.NAME)) {
                intentExtraKeys.remove(Insert.NAME);
            }

            int numIntentExtraKeys = intentExtraKeys.size();
            if (numIntentExtraKeys == 2) {
                boolean hasPhone = intentExtraKeys.contains(Insert.PHONE) &&
                        intentExtraKeys.contains(Insert.PHONE_TYPE);
                boolean hasEmail = intentExtraKeys.contains(Insert.EMAIL) &&
                        intentExtraKeys.contains(Insert.EMAIL_TYPE);
                return hasPhone || hasEmail;
            } else if (numIntentExtraKeys == 1) {
                return intentExtraKeys.contains(Insert.PHONE) ||
                        intentExtraKeys.contains(Insert.EMAIL);
            }
            // Having 0 or more than 2 intent extra keys means that we should launch
            // the full contact editor to properly handle the intent extras.
            return false;
        }
    }

    private final class PhoneNumberPickerActionListener implements
            OnPhoneNumberPickerActionListener {
        @Override
        public void onPickPhoneNumberAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }

        @Override
        public void onCallNumberDirectly(String phoneNumber) {
            Log.w(TAG, "Unsupported call.");
        }

        @Override
        public void onShortcutIntentCreated(Intent intent) {
            returnPickerResult(intent);
        }

        public void onHomeInActionBarSelected() {
            ContactSelectionActivity.this.onBackPressed();
        }
    }

    private final class PostalAddressPickerActionListener implements
            OnPostalAddressPickerActionListener {
        @Override
        public void onPickPostalAddressAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }
    }

    private final class EmailAddressPickerActionListener implements
            OnEmailAddressPickerActionListener {
        @Override
        public void onPickEmailAddressAction(Uri dataUri) {
            returnPickerResult(dataUri);
        }
    }

    public void startActivityAndForwardResult(final Intent intent) {
        intent.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

        // Forward extras to the new activity
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        try {
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "startActivity() failed: " + e);
            Toast.makeText(ContactSelectionActivity.this, R.string.missing_app,
                    Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mListFragment.setQueryString(newText, true);
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        /**
        * SPRD:
        * 
        *
        * Original Android code:
        * return false;
        * 
        * @{
        */
        /*if (mListFragment.isSearchMode() && mSearchView != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(
                    Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
            }
            mSearchView.clearFocus();
            return true;
        } else {*/
            return false;
        //}
        /**
        * @}
        */
    }

    @Override
    public boolean onClose() {
        /*if (!TextUtils.isEmpty(mSearchView.getQuery())) {
            mSearchView.setQuery(null, true);
        }*/
        return true;
    }

    @Override
    public void onFocusChange(View view, boolean hasFocus) {

    }

    public void returnPickerResult(Uri data) {
        Intent intent = new Intent();
        intent.setData(data);
        returnPickerResult(intent);
    }

    public void returnPickerResult(Intent intent) {
        /**
        * SPRD:
        * 
        * @{
        */
        intent = setPickerResultIntent(intent);
        /**
        * @}
        */
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.new_contact: {
                startCreateNewContactActivity();
                break;
            }
        }
    }

    private void startCreateNewContactActivity() {
        Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
        intent.putExtra(ContactEditorActivity.INTENT_KEY_FINISH_ACTIVITY_ON_SAVE_COMPLETED, true);
        startActivityAndForwardResult(intent);
    }

    private void showInputMethod(View view) {
        final InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            if (!imm.showSoftInput(view, 0)) {
                Log.w(TAG, "Failed to show soft input method.");
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SUBACTIVITY_ADD_TO_EXISTING_CONTACT) {
            if (resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    startActivity(data);
                }
                finish();
            }
        }
    }
    /*
    * SPRD:
    *
    * @{
    */
    public ContactEntryListFragment<?> getListFragment() {
        return mListFragment;
    }

    public ContactsRequest getRequest() {
        return mRequest;
    }

    public boolean showCreateNewContactButton() {
        return shouldShowCreateNewContactButton();
    }

    public View getCreateNewContactButton() {
        return mCreateNewContactButton;
    }

    @Override
    public void hideCenterSkView() {
        mCenterSkView.setText("");
    }
}
