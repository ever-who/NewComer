/*
 * Copyright (C) 2010 The Android Open Source Project
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
package com.android.contacts.list;

import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.graphics.Color;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.android.contacts.ContactSaveService;
import com.android.contacts.ContactsApplication;
import com.android.contacts.activities.ContactEditorActivity;
import com.android.contacts.common.R;
import com.android.contacts.common.CallUtil;
import com.android.contacts.common.list.ContactListAdapter;
import com.android.contacts.common.list.ContactListFilter;
import com.android.contacts.common.list.ContactListFilterController;
import com.android.contacts.common.list.ContactListItemView;
import com.android.contacts.common.list.DefaultContactListAdapter;
import com.android.contacts.common.list.ProfileAndContactsLoader;
import com.android.contacts.common.util.AccountFilterUtil;
import com.android.contacts.common.util.Constants;
import com.android.contacts.util.PhoneCapabilityTester;
import com.sprd.contacts.common.util.UniverseUtils;
import com.sprd.contacts.common.model.account.PhoneAccountType;
import com.android.contacts.activities.PeopleActivity;

import android.telephony.PhoneStateListener;
import android.telephony.VoLteServiceState;
import android.telephony.TelephonyManager;
import android.os.SystemProperties;
/**
 * Fragment containing a contact list used for browsing (as compared to
 * picking a contact with one of the PICK intents).
 */
public class DefaultContactBrowseListFragment extends ContactBrowseListFragment {
    private static final String TAG = DefaultContactBrowseListFragment.class.getSimpleName();

    private static final int REQUEST_CODE_ACCOUNT_FILTER = 1;

    private TextView mCounterHeaderView;
    private View mSearchHeaderView;
    private View mAccountFilterHeader;
    private FrameLayout mProfileHeaderContainer;
    private View mProfileHeader;
    private Button mProfileMessage;
    private FrameLayout mMessageContainer;
    private TextView mProfileTitle;
    private View mSearchProgress;
    private TextView mSearchProgressText;
    private boolean mImsRegisted = false;
    private PhoneStateListener mLtePhoneStateListener;
    private static boolean mIsVolteSupport = SystemProperties.getBoolean("persist.sys.support.vt", true);
    private static boolean mIsVolteEnable = SystemProperties.getBoolean("persist.sys.volte.enable", false);

    private class FilterHeaderClickListener implements OnClickListener {
        @Override
        public void onClick(View view) {
            AccountFilterUtil.startAccountFilterActivityForResult(
                        DefaultContactBrowseListFragment.this,
                        REQUEST_CODE_ACCOUNT_FILTER,
                        getFilter());
        }
    }
    private OnClickListener mFilterHeaderClickListener = new FilterHeaderClickListener();

    public DefaultContactBrowseListFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(true);
    }

    @Override
    public CursorLoader createCursorLoader(Context context) {
        return new ProfileAndContactsLoader(context);
    }

    @Override
    protected void onItemClick(int position, long id) {
        viewContact(getAdapter().getContactUri(position));
    }

    @Override
    protected ContactListAdapter createListAdapter() {
        DefaultContactListAdapter adapter = new DefaultContactListAdapter(getContext());
        adapter.setSectionHeaderDisplayEnabled(isSectionHeaderDisplayEnabled());
        Log.d(TAG, "createListAdapter: config_browse_list_show_images="+getResources().getBoolean(R.bool.config_browse_list_show_images));
        //adapter.setDisplayPhotos(getResources().getBoolean(R.bool.config_browse_list_show_images));
        adapter.setDisplayPhotos(false);//mark
        return adapter;
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        /**
        * SPRD:
        *
        *
        * Original Android code:
        * return inflater.inflate(R.layout.contact_list_content, null);
        * 
        * @{
        */
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            return inflater.inflate(R.layout.contact_list_content_overlay, null);
        } else {
            return inflater.inflate(R.layout.contact_list_content, null);
        }
        /**
        * @}
        */
    }

    @Override
    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        super.onCreateView(inflater, container);

        mAccountFilterHeader = getView().findViewById(R.id.account_filter_header_container);
        /**
        * SPRD:
        *
        *
        * Original Android code:
        * mAccountFilterHeader.setOnClickListener(mFilterHeaderClickListener);
        * 
        * @{
        */
        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
            mAccountFilterHeader.setOnClickListener(mFilterHeaderClickListener);
        }
        /**
        * @}
        */
        // Create an empty user profile header and hide it for now (it will be visible if the
        // contacts list will have no user profile).
        //addEmptyUserProfileHeader(inflater);
       // showEmptyUserProfile(false);

        // Putting the header view inside a container will allow us to make
        // it invisible later. See checkHeaderViewVisibility()
        FrameLayout headerContainer = new FrameLayout(inflater.getContext());
        mSearchHeaderView = inflater.inflate(R.layout.search_header, null, false);
        headerContainer.addView(mSearchHeaderView);
        getListView().addHeaderView(headerContainer, null, false);
        checkHeaderViewVisibility();

        mSearchProgress = getView().findViewById(R.id.search_progress);
        mSearchProgressText = (TextView) mSearchHeaderView.findViewById(R.id.totalContactsText);

        /* SPRD: add for bug632743, click dial key, directly call on contact list. @} */
        final ListView listView = getListView();
        Log.d(TAG,"listView.getAdapter().getCount() = "+getAdapter().getCount());
        if(getAdapter().getCount()!=0){
            prepareListView();
        }

        /* add by BIRD@hujingcheng 20190610 */
//        mAddContactsView=(ContactListItemView)getView().findViewById(R.id.addContacts);
//        mAddContactsView.setDisplayName("add contact");
//        mAddContactsView.setActivatedStateSupported(true);
//        mAddContactsView.setActivated(true);
        /*mAddContactsView.setEnabled(true);
        mAddContactsView.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View view) {
                        if (ContactsApplication.sApplication.isBatchOperation()
                                || ContactSaveService.mIsGroupSaving) {
                            Toast.makeText(getActivity(), R.string.toast_batchoperation_is_running,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            final Intent intent = new Intent(Intent.ACTION_INSERT, Contacts.CONTENT_URI);
                            startActivity(intent);
                        }
                    }
                }
        );*/
        /* add by BIRD@hujingcheng 20190610 end */

        listView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                int position = listView.getSelectedItemPosition() - listView.getHeaderViewsCount();
                Uri uri = getAdapter().getContactUri(position);
                if (uri == null) {
                    return false;
                }
                if (keyCode == KeyEvent.KEYCODE_CALL && event.getAction() == KeyEvent.ACTION_UP) {
                    String phoneNumber = CallUtil.getPhoneNumber(mContext, uri);
                    if (phoneNumber != null && !TextUtils.isEmpty(phoneNumber) && isPhoneNumber(phoneNumber)) {
                        if (mIsVolteSupport && mImsRegisted) {
                            CallUtil.showCallDialogAlert(getActivity(), phoneNumber, null);
                            return true;
                        } else {
                            Intent intent = CallUtil.getCallIntent(phoneNumber);
                            startActivity(intent);
                        }
                    } else {
                        Toast.makeText(getActivity(), R.string.contact_has_no_phonenumber,
                                Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        createPhoneStateListener();
        startMonitor();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopMonitor();
    }

    public void createPhoneStateListener() {
        mLtePhoneStateListener = new PhoneStateListener() {
            @Override
            public void onVoLteServiceStateChanged(VoLteServiceState serviceState) {
                Log.i(TAG, "=======PhoneStateListener: ImsState = " + serviceState.getImsState() + "  srvccState = "+ serviceState.getSrvccState());
                mImsRegisted = (serviceState.getSrvccState() != VoLteServiceState.HANDOVER_STARTED
                            && (serviceState.getImsState() == 1));
            }
        };
    }

    public void startMonitor() {
        if (mIsVolteEnable) {
            TelephonyManager.from(getActivity()).listen(mLtePhoneStateListener,
                    PhoneStateListener.LISTEN_SERVICE_STATE
                            | PhoneStateListener.LISTEN_VOLTE_STATE);
        }
    }

    public void stopMonitor() {
        if (mIsVolteEnable) {
            TelephonyManager.from(getActivity()).listen(mLtePhoneStateListener,
                    PhoneStateListener.LISTEN_NONE);
        }
        mLtePhoneStateListener = null;
    }

    public static boolean isPhoneNumber(String text) {
        char[] sChar = text.toCharArray();
        for (char c : sChar) {
            if (checkCharacter(c)) {
                continue;
            } else {
                return false;
            }
        }
        return true;
    }

    private static boolean checkCharacter(char c) {
        return ((c >= '0' && c <= '9') || c == ',' || c == ';' || c == '*'
                || c == '#' || c == '+' || c == '-' || c == '(' || c == ')'
                || c == ',' || c == '/' || c == 'N' || c == '.' || c == ' ' || c == ';');
    }
    /* @} */

    @Override
    protected void setSearchMode(boolean flag) {
        super.setSearchMode(flag);
        checkHeaderViewVisibility();
        if (!flag) showSearchProgress(false);
    }

    /** Show or hide the directory-search progress spinner. */
    private void showSearchProgress(boolean show) {
        /**
        * SPRD:
        *
        *
        * Original Android code:
        * mSearchProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        * 
        * @{
        */
        if (mSearchProgress != null) {
            mSearchProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        /**
        * @}
        */
    }

    private void checkHeaderViewVisibility() {
        /**
        * SPRD:
        *   for UUI
        *
        * Original Android code:
        * updateFilterHeaderView();
        * 
        * @{
        */
        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
            updateFilterHeaderView();
        }
        /**
        * @}
        */

        // Hide the search header by default. See showCount().
        if (mSearchHeaderView != null) {
            mSearchHeaderView.setVisibility(View.GONE);
        }
    }

    @Override
    public void setFilter(ContactListFilter filter) {
        super.setFilter(filter);
        /**
        * SPRD:
        *   for UUI
        *
        * Original Android code:
        * updateFilterHeaderView();
        * 
        * @{
        */
        if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
            updateFilterHeaderView();
        }
        /**
        * @}
        */
    }

    private void updateFilterHeaderView() {
        if (mAccountFilterHeader == null) {
            return; // Before onCreateView -- just ignore it.
        }
        final ContactListFilter filter = getFilter();
        /**
        * SPRD:
        *   Defer the action to make the window properly repaint.
        *
        * Original Android code:
        *if (filter != null && !isSearchMode()) {
            final boolean shouldShowHeader = AccountFilterUtil.updateAccountFilterTitleForPeople(
                    mAccountFilterHeader, filter, false);
            mAccountFilterHeader.setVisibility(shouldShowHeader ? View.VISIBLE : View.GONE);
        } else {
            mAccountFilterHeader.setVisibility(View.GONE);
        }
        * 
        * @{
        */
        if (filter != null && !isSearchMode()) {
      
            final boolean shouldShowHeader = AccountFilterUtil.updateAccountFilterTitle(
                    mAccountFilterHeader, filter, true, R.string.list_filter_all_accounts);
            // always show the filter header
            mAccountFilterHeader.setVisibility(View.VISIBLE);
        } else {
            mAccountFilterHeader.setVisibility(View.GONE);
        }
        /**
        * @}
        */

    }

    @Override
    protected void prepareEmptyView() {
           super.prepareEmptyView();
           setEmptyText(R.string.noContacts);
           /**
            * SPRD Bug642503 The prompt of "Select" donot disappear when betch delete all the contacts
            * {@*/
           ((PeopleActivity)getActivity()).getCenterSkView().setText("");
           /**
            * @}
            * */
    }

    @Override
    protected void showCount(int partitionIndex, Cursor data) {
        setSearchViewPermanentVisible(true);
        if (data != null && data.getCount() == 0) {
            /**
             * SPRD Bug636961 The prompt is diffrent when the number of character is diffent
             * {@
             * */
            if(!isSearchMode() || TextUtils.isEmpty(getQueryString())){
                prepareEmptyView();
            }
            /**
             * @}
             * */

        }
        if (!isSearchMode() && data != null) {
            int count = data.getCount();
            if (count != 0) {
                count -= (mUserProfileExists ? 1: 0);
                String format = getResources().getQuantityText(
                        R.plurals.listTotalAllContacts, count).toString();
                // Do not count the user profile in the contacts count
                getAdapter().setContactsCount(String.format(format, count));
            }
        } else {
            ContactListAdapter adapter = getAdapter();
            if (adapter == null) {
                return;
            }

            // In search mode we only display the header if there is nothing found
            if (TextUtils.isEmpty(getQueryString()) || !adapter.areAllPartitionsEmpty()) {
                mSearchHeaderView.setVisibility(View.GONE);
                showSearchProgress(false);
            } else {
                Log.d(TAG, "showCount: isSearchMode()="+isSearchMode()+",data="+data+",getQueryString()="+getQueryString());
                mSearchHeaderView.setVisibility(View.VISIBLE);
                if (adapter.isLoading()) {
                    mSearchProgressText.setText(R.string.search_results_searching);
                    showSearchProgress(true);
                } else {
                    mSearchProgressText.setText(R.string.listFoundAllContactsZero);
                    mSearchProgressText.sendAccessibilityEvent(
                            AccessibilityEvent.TYPE_VIEW_SELECTED);
                    showSearchProgress(false);
                }
                prepareListView();//mark
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_ACCOUNT_FILTER) {
            if (getActivity() != null) {
                AccountFilterUtil.handleAccountFilterResult(
                        ContactListFilterController.getInstance(getActivity()), resultCode, data);
            } else {
                Log.e(TAG, "getActivity() returns null during Fragment#onActivityResult()");
            }
        }
    }

    /**
     * This method creates a pseudo user profile contact. When the returned query doesn't have
     * a profile, this methods creates 2 views that are inserted as headers to the listview:
     * 1. A header view with the "ME" title and the contacts count.
     * 2. A button that prompts the user to create a local profile
     */

    /**
    * SPRD:
    * 
    * @{
    */
    private boolean isImporting=false;

    /**
    * @}
    */
}
