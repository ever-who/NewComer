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

package com.android.contacts.common.list;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentResolver;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.IContentService;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Directory;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewStub;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.bird.util.TextSpeech;
import com.sprd.contacts.common.CustomSearchView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.common.widget.CompositeCursorAdapter.Partition;
import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.R;
import com.android.contacts.common.widget.ContextMenuAdapter;
import com.bird.widget.CircularListController;
import com.android.contacts.common.preference.ContactsPreferences;
import com.android.contacts.common.util.Constants;
import com.sprd.contacts.common.util.MultiContactDataCacheUtils;
import com.sprd.contacts.common.util.UniverseUtils;

import java.util.Locale;
import java.util.ArrayList;





/*SPRD @{*/
import com.sprd.contacts.common.ContactListEmptyView;






/*SPRD @}*/
/*
* SPRD:289887 Delay the data loading when open the contacts
*
* @{
*/
import android.os.Handler;
import android.os.Message;
import android.media.AudioManager;//add by maoyufeng 20190514 
/*
* @}
*/


/**
 * Common base class for various contact-related list fragments.
 */
public abstract class ContactEntryListFragment<T extends ContactEntryListAdapter>
        extends Fragment
        implements OnItemClickListener, OnScrollListener, OnFocusChangeListener, OnTouchListener, OnClickListener,
                LoaderCallbacks<Cursor>, TextWatcher,AdapterView.OnItemSelectedListener,TextToSpeech.OnInitListener {
    private static final String TAG = "ContactEntryListFragment";

    // TODO: Make this protected. This should not be used from the PeopleActivity but
    // instead use the new startActivityWithResultFromFragment API
    public static final int ACTIVITY_REQUEST_CODE_PICKER = 1;

    private static final String KEY_LIST_STATE = "liststate";
    private static final String KEY_SECTION_HEADER_DISPLAY_ENABLED = "sectionHeaderDisplayEnabled";
    private static final String KEY_PHOTO_LOADER_ENABLED = "photoLoaderEnabled";
    private static final String KEY_QUICK_CONTACT_ENABLED = "quickContactEnabled";
    private static final String KEY_INCLUDE_PROFILE = "includeProfile";
    private static final String KEY_SEARCH_MODE = "searchMode";
    private static final String KEY_VISIBLE_SCROLLBAR_ENABLED = "visibleScrollbarEnabled";
    private static final String KEY_SCROLLBAR_POSITION = "scrollbarPosition";
    private static final String KEY_QUERY_STRING = "queryString";
    private static final String KEY_DIRECTORY_SEARCH_MODE = "directorySearchMode";
    private static final String KEY_SELECTION_VISIBLE = "selectionVisible";
    private static final String KEY_REQUEST = "request";
    private static final String KEY_DARK_THEME = "darkTheme";
    private static final String KEY_LEGACY_COMPATIBILITY = "legacyCompatibility";
    private static final String KEY_DIRECTORY_RESULT_LIMIT = "directoryResultLimit";
    private static final String KEY_CHECKED_LIMIT_COUNT = "checked_limit_count";
    private static final String DIRECTORY_ID_ARG_KEY = "directoryId";

    private static final boolean LAND_UI_E516 = true;//add by BIRD@hujingcheng

    private static final int DIRECTORY_LOADER_ID = -1;

    private static final int DIRECTORY_SEARCH_DELAY_MILLIS = 300;
    private static final int DIRECTORY_SEARCH_MESSAGE = 1;

    private static final int DEFAULT_DIRECTORY_RESULT_LIMIT = 20;

    private boolean mSectionHeaderDisplayEnabled;
    private boolean mPhotoLoaderEnabled;
    private boolean mQuickContactEnabled = true;
    private boolean mIncludeProfile;
    private boolean mSearchMode;
    private boolean mVisibleScrollbarEnabled;
    private int mVerticalScrollbarPosition = getDefaultVerticalScrollbarPosition();
    private String mQueryString;
    private int mDirectorySearchMode = DirectoryListLoader.SEARCH_MODE_NONE;
    private boolean mSelectionVisible;
    private boolean mLegacyCompatibility;

    private boolean mEnabled = true;

    private T mAdapter;
    private View mView;
    private TextToSpeech mTts;



    /**
    * SPRD:
    * 
    *
    * Original Android code:
    * private ListView mListView;
    * private Context mContext;
    * 
    * @{
    */
    protected ListView mListView;
    protected Context mContext;
    protected ContactListItemView mAddContactsView;//add by BIRD@hujingcheng

    /**
    * @}
    */
    /**
     * SPRD:
     * 
     * @{
     */
     public static final int CHECKED_ITEMS_MAX = 3500;

     private static final String KEY_CHECKED_ITEM_IDS = "checkedItemIds";
     private static final String KEY_CHECKED_ITEM_OFFSET = "checkedItemoffset";
     private static final String KEY_MULTI_PICKER_SUPPORTED = "multiPickerSupported";
     private static final String KEY_FILTER = "mFilter";
     private static final String KEY_SEARCH_VISIBLE = "search_visible";
     private static final String KEY_FIRST_DIVIDER_VISIBLE = "first_divider_visible";
     private static final String KEY_SELECT_TEXT_VISIBLE = "select_text_visible";
     private static final String KEY_CONTACT_CACHE = "contact_cache";

     private static final int SUBACTIVITY_BATCH_DELETE = 7;
     private boolean mMultiPickerSupported=false;
     private View mFirstDivider;
     private long[] mCheckedItems;
     private ContactListFilter mFilter;
     private boolean mSearchVisible = true;
     private boolean mFirstDividerVisible = false;
     private boolean mSelectTextVisible = true;

     private int mSelectionOffset = 0;
     private ContextMenuAdapter mContextMenuAdapter;

     private LinearLayout mFooter;
     private Button mOkBtn;
     private Button mRevertBtn;
     private CheckBox mSelectAll;
     private TextView mSelectAllTxt;
     private TextView mSelectText;
     private TextView mSelectedContactsNum;
     private RelativeLayout mSelectContacts;
     protected RelativeLayout mSearchViewContainer;
     private CustomSearchView mSearchView;
     private ImageView mClearAll;
     private MultiContactDataCacheUtils mContactDataCache = new MultiContactDataCacheUtils();
     /**
      * @}
      */
    /**
     * Used for keeping track of the scroll state of the list.
     */
    private Parcelable mListState;

    private int mDisplayOrder;
    private int mSortOrder;
    private int mDirectoryResultLimit = DEFAULT_DIRECTORY_RESULT_LIMIT;
    private int mCheckedLimitCount = 5000;

    private ContactPhotoManager mPhotoManager;
    private ContactsPreferences mContactsPrefs;
/*SPRD @{*/
    private ContactListEmptyView mEmptyView;
/*SPRD @}*/

    private boolean mForceLoad;

    private boolean mDarkTheme;

    protected boolean mUserProfileExists;

    private static final int STATUS_NOT_LOADED = 0;
    private static final int STATUS_LOADING = 1;
    private static final int STATUS_LOADED = 2;

    private int mDirectoryListStatus = STATUS_NOT_LOADED;


    private OnChangeListener mOnChangeListener;

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int voiceForContacts = Settings.System.getInt(mContext.getContentResolver(), "voice_for_contacts", 0);
        if (voiceForContacts == 1 && !isSearchMode()) {
            ContactListItemView itemView = (ContactListItemView) parent.getSelectedView();
            if (itemView != null && itemView.getNameTextView() != null && itemView.getNameTextView().getText() != null) {
                String nameToRead = itemView.getNameTextView().getText().toString();
                if (!nameToRead.isEmpty()) {
                    textToSpeech(nameToRead);
                }
            }
        }
    }
	//add by maoyufeng 20190514 begin
    private static final long VOICE_ENABLE_OVERTIME = 2000;
    private long lastVoiceEnableTime = 0;
    private boolean isVoiceEnable() {
       AudioManager audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        boolean isRingerModeNormal = audioManager.getRingerMode() >= AudioManager.RINGER_MODE_NORMAL;
        boolean isMusicActive = audioManager.isMusicActive();
        boolean isFmActive = audioManager.isFmActive();
        boolean isRecording = audioManager.isAudioRecording();
        boolean isSpeaking = mTts.isSpeaking();
        boolean isVoiceEnable = isRingerModeNormal && ((!isMusicActive && !isFmActive && !isRecording) || isSpeaking);
        long curTime = System.currentTimeMillis();

        if (isVoiceEnable) {
            lastVoiceEnableTime = curTime;
        }

        boolean result = isVoiceEnable || (curTime - lastVoiceEnableTime < VOICE_ENABLE_OVERTIME);
        Log.d(TAG,
                "WL_DEBUG isVoiceEnable  isRingerModeNormal = " + isRingerModeNormal + ", isMusicActive = "
                        + isMusicActive + ", isSpeaking = " + isSpeaking + ", isVoiceEnable = " + isVoiceEnable
                        + ", result = " + result);
        return result;
    }
	//add by maoyufeng 20190514 end

    //@ {bird: For fix bug#47848, add by shicuiliang@agenewtech.com 2019/05/23.
    private long mSpeakTime = 0;
    private String mSpeakName = "";
    private Handler mSpeakHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            long curTime = System.currentTimeMillis();
            if(curTime - mSpeakTime >= 300){
                Log.d(TAG, "mSpeakHandler - readContact");
                mSpeakTime = System.currentTimeMillis();
                TextSpeech.readContact(mSpeakName, mContext);
            }
        }
    };

    private void textToSpeech(String name) {
        mSpeakName = name;
        boolean isRead = false;
        long curTime = System.currentTimeMillis();
        if(curTime - mSpeakTime > 300){
            isRead = true;
            Log.d(TAG, "textToSpeech - readContact");
            TextSpeech.readContact(name, mContext);
        }
        mSpeakTime = System.currentTimeMillis();

        if (!isRead) {
            mSpeakHandler.sendEmptyMessageDelayed(0, 300);
        }
        // Drop all pending entries in the playback queue.
//        mTts.speak(name, TextToSpeech.QUEUE_FLUSH,null);
    }
    //@ }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.d(TAG, "onNothingSelected");
        TextSpeech.readContact("", getContext());
//        mTts.speak("", TextToSpeech.QUEUE_FLUSH,null);
    }

    public interface OnChangeListener {
        void onDataChange(Cursor data);
        void hideCenterSkView();
    }

    public void setOnChangeListener(OnChangeListener listener) {
        mOnChangeListener = listener;
    }

    public OnChangeListener getOnChangeListener() {
        return mOnChangeListener;
    }

    public void unSetOnChangeListener() {
        mOnChangeListener = null;
    }
    /**
     * Indicates whether we are doing the initial complete load of data (false) or
     * a refresh caused by a change notification (true)
     */
    private boolean mLoadPriorityDirectoriesOnly;

    private LoaderManager mLoaderManager;

    private Handler mDelayedDirectorySearchHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == DIRECTORY_SEARCH_MESSAGE) {
                loadDirectoryPartition(msg.arg1, (DirectoryPartition) msg.obj);
            }
        }
    };
    private int defaultVerticalScrollbarPosition;

    protected abstract View inflateView(LayoutInflater inflater, ViewGroup container);
    protected abstract T createListAdapter();

    /**
     * @param position Please note that the position is already adjusted for
     *            header views, so "0" means the first list item below header
     *            views.
     */
    protected abstract void onItemClick(int position, long id);

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        setContext(activity);
        setLoaderManager(super.getLoaderManager());
    }

    /**
     * Sets a context for the fragment in the unit test environment.
     */
    public void setContext(Context context) {
        mContext = context;
        configurePhotoLoader();
    }

    public Context getContext() {
        return mContext;
    }

    public void setEnabled(boolean enabled) {
        if (mEnabled != enabled) {
            mEnabled = enabled;
            if (mAdapter != null) {
                if (mEnabled) {
                    reloadData();
                } else {
                    mAdapter.clearPartitions();
                }
            }
        }
    }

    /**
     * Overrides a loader manager for use in unit tests.
     */
    public void setLoaderManager(LoaderManager loaderManager) {
        mLoaderManager = loaderManager;
    }

    @Override
    public LoaderManager getLoaderManager() {
        return mLoaderManager;
    }

    public T getAdapter() {
        return mAdapter;
    }

    @Override
    public View getView() {
        return mView;
    }

    public ListView getListView() {
        return mListView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_SECTION_HEADER_DISPLAY_ENABLED, mSectionHeaderDisplayEnabled);
        outState.putBoolean(KEY_PHOTO_LOADER_ENABLED, mPhotoLoaderEnabled);
        outState.putBoolean(KEY_QUICK_CONTACT_ENABLED, mQuickContactEnabled);
        outState.putBoolean(KEY_INCLUDE_PROFILE, mIncludeProfile);
        outState.putBoolean(KEY_SEARCH_MODE, mSearchMode);
        outState.putBoolean(KEY_VISIBLE_SCROLLBAR_ENABLED, mVisibleScrollbarEnabled);
        outState.putInt(KEY_SCROLLBAR_POSITION, mVerticalScrollbarPosition);
        outState.putInt(KEY_DIRECTORY_SEARCH_MODE, mDirectorySearchMode);
        outState.putBoolean(KEY_SELECTION_VISIBLE, mSelectionVisible);
        outState.putBoolean(KEY_LEGACY_COMPATIBILITY, mLegacyCompatibility);
        outState.putString(KEY_QUERY_STRING, mQueryString);
        outState.putInt(KEY_DIRECTORY_RESULT_LIMIT, mDirectoryResultLimit);
        outState.putBoolean(KEY_DARK_THEME, mDarkTheme);

        /**
        * SPRD:
        * 
        * @{
        */
        outState.putBoolean(KEY_MULTI_PICKER_SUPPORTED, mMultiPickerSupported);
        outState.putBoolean(KEY_SEARCH_VISIBLE, mSearchVisible);
        outState.putBoolean(KEY_FIRST_DIVIDER_VISIBLE, mFirstDividerVisible);
        outState.putBoolean(KEY_SELECT_TEXT_VISIBLE, mSelectTextVisible);
        outState.putInt(KEY_CHECKED_LIMIT_COUNT,mCheckedLimitCount);
        if (mAdapter != null) {
            outState.putParcelable(KEY_FILTER, mAdapter.getFilter());
            if (isMultiPickerSupported()) {
                outState.putLongArray(KEY_CHECKED_ITEM_IDS, mAdapter.getAllCheckedItemIds());
                outState.putInt(KEY_CHECKED_ITEM_OFFSET, mSelectionOffset);
                outState.putParcelable(KEY_CONTACT_CACHE, mAdapter.getContactDataCache());
            }
        }
        /**
        * @}
        */
        if (mListView != null) {
            outState.putParcelable(KEY_LIST_STATE, mListView.onSaveInstanceState());
        }
    }

    @Override
    public void onCreate(Bundle savedState) {
        super.onCreate(savedState);
        mAdapter = createListAdapter();
        mContactsPrefs = new ContactsPreferences(mContext);
        restoreSavedState(savedState);
//        mTts = new TextToSpeech(mContext,this);
    }

    public void restoreSavedState(Bundle savedState) {
        if (savedState == null) {
            return;
        }

        mSectionHeaderDisplayEnabled = savedState.getBoolean(KEY_SECTION_HEADER_DISPLAY_ENABLED);
        mPhotoLoaderEnabled = savedState.getBoolean(KEY_PHOTO_LOADER_ENABLED);
        mQuickContactEnabled = savedState.getBoolean(KEY_QUICK_CONTACT_ENABLED);
        mIncludeProfile = savedState.getBoolean(KEY_INCLUDE_PROFILE);
        mSearchMode = savedState.getBoolean(KEY_SEARCH_MODE);
        mVisibleScrollbarEnabled = savedState.getBoolean(KEY_VISIBLE_SCROLLBAR_ENABLED);
        mVerticalScrollbarPosition = savedState.getInt(KEY_SCROLLBAR_POSITION);
        mDirectorySearchMode = savedState.getInt(KEY_DIRECTORY_SEARCH_MODE);
        mSelectionVisible = savedState.getBoolean(KEY_SELECTION_VISIBLE);
        mLegacyCompatibility = savedState.getBoolean(KEY_LEGACY_COMPATIBILITY);
        mQueryString = savedState.getString(KEY_QUERY_STRING);
        mDirectoryResultLimit = savedState.getInt(KEY_DIRECTORY_RESULT_LIMIT);
        mDarkTheme = savedState.getBoolean(KEY_DARK_THEME);
        /**
        * SPRD:
        * 
        * @{
        */
        mMultiPickerSupported = savedState.getBoolean(KEY_MULTI_PICKER_SUPPORTED);
        mFilter = (ContactListFilter) savedState.getParcelable(KEY_FILTER);
        mSearchVisible = savedState.getBoolean(KEY_SEARCH_VISIBLE);
        mFirstDividerVisible = savedState.getBoolean(KEY_FIRST_DIVIDER_VISIBLE);
        mSelectTextVisible = savedState.getBoolean(KEY_SELECT_TEXT_VISIBLE);
        if (isMultiPickerSupported() && savedState.getLongArray(KEY_CHECKED_ITEM_IDS) != null) {
            mCheckedItems = savedState.getLongArray(KEY_CHECKED_ITEM_IDS);
            mSelectionOffset = savedState.getInt(KEY_CHECKED_ITEM_OFFSET);
        }
        if (isMultiPickerSupported()) {
            mContactDataCache = savedState.getParcelable(KEY_CONTACT_CACHE);
        }
        /**
        * @}
        */

        // Retrieve list state. This will be applied in onLoadFinished
        mListState = savedState.getParcelable(KEY_LIST_STATE);
        mCheckedLimitCount = savedState.getInt(KEY_CHECKED_LIMIT_COUNT);
    }

    @Override
    public void onStart() {
        super.onStart();
        mContactsPrefs.registerChangeListener(mPreferencesChangeListener);

        mForceLoad = loadPreferences();

        mDirectoryListStatus = STATUS_NOT_LOADED;
        mLoadPriorityDirectoriesOnly = true;

        startLoading();
        /**
        * SPRD:
        * bug 263320
        * @{
        */
        if(mQueryString == null && !mSearchMode && mSearchView != null){
            mSearchView.setText("");
        }
        /**
        * @}
        */

    }

    protected void startLoading() {
        if (mAdapter == null) {
            // The method was called before the fragment was started
            return;
        }

        configureAdapter();
        int partitionCount = mAdapter.getPartitionCount();
        for (int i = 0; i < partitionCount; i++) {
            Partition partition = mAdapter.getPartition(i);
            if (partition instanceof DirectoryPartition) {
                DirectoryPartition directoryPartition = (DirectoryPartition)partition;
                /*
                * SPRD:
                *   Bug 308627
                *   Select "via SMS / MMS" after entering the message, return the
                *   contact interface, occasional contacts can not be displayed.
                *
                * @orig
                *  if (directoryPartition.getStatus() == DirectoryPartition.STATUS_NOT_LOADED) {
                *
                * @{
                */
                if (directoryPartition.getStatus() == DirectoryPartition.STATUS_NOT_LOADED
                        || directoryPartition.getStatus() == DirectoryPartition.STATUS_LOADING) {
                /*
                * @}
                */
                    if (directoryPartition.isPriorityDirectory() || !mLoadPriorityDirectoriesOnly) {
                        startLoadingDirectoryPartition(i);
                    }
                }
            } else {
                getLoaderManager().initLoader(i, null, this);
            }
        }

        // Next time this method is called, we should start loading non-priority directories
        mLoadPriorityDirectoriesOnly = false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == DIRECTORY_LOADER_ID) {
            DirectoryListLoader loader = new DirectoryListLoader(mContext);
            loader.setDirectorySearchMode(mAdapter.getDirectorySearchMode());
            loader.setLocalInvisibleDirectoryEnabled(
                    ContactEntryListAdapter.LOCAL_INVISIBLE_DIRECTORY_ENABLED);
            return loader;
        } else {
            CursorLoader loader = createCursorLoader(mContext);
            long directoryId = args != null && args.containsKey(DIRECTORY_ID_ARG_KEY)
                    ? args.getLong(DIRECTORY_ID_ARG_KEY)
                    : Directory.DEFAULT;
            mAdapter.configureLoader(loader, directoryId);
            return loader;
        }
    }

    public CursorLoader createCursorLoader(Context context) {
        return new CursorLoader(context, null, null, null, null, null);
    }

    private void startLoadingDirectoryPartition(int partitionIndex) {
        DirectoryPartition partition = (DirectoryPartition)mAdapter.getPartition(partitionIndex);
        partition.setStatus(DirectoryPartition.STATUS_LOADING);
        long directoryId = partition.getDirectoryId();
        if (mForceLoad) {
            if (directoryId == Directory.DEFAULT) {
                loadDirectoryPartition(partitionIndex, partition);
            } else {
                loadDirectoryPartitionDelayed(partitionIndex, partition);
            }
        } else {
            Bundle args = new Bundle();
            args.putLong(DIRECTORY_ID_ARG_KEY, directoryId);
            getLoaderManager().initLoader(partitionIndex, args, this);
        }
    }

    /**
     * Queues up a delayed request to search the specified directory. Since
     * directory search will likely introduce a lot of network traffic, we want
     * to wait for a pause in the user's typing before sending a directory request.
     */
    private void loadDirectoryPartitionDelayed(int partitionIndex, DirectoryPartition partition) {
        mDelayedDirectorySearchHandler.removeMessages(DIRECTORY_SEARCH_MESSAGE, partition);
        Message msg = mDelayedDirectorySearchHandler.obtainMessage(
                DIRECTORY_SEARCH_MESSAGE, partitionIndex, 0, partition);
        mDelayedDirectorySearchHandler.sendMessageDelayed(msg, DIRECTORY_SEARCH_DELAY_MILLIS);
    }

    /**
     * Loads the directory partition.
     */
    protected void loadDirectoryPartition(int partitionIndex, DirectoryPartition partition) {
        Bundle args = new Bundle();
        args.putLong(DIRECTORY_ID_ARG_KEY, partition.getDirectoryId());
        getLoaderManager().restartLoader(partitionIndex, args, this);
    }

    /**
     * Cancels all queued directory loading requests.
     */
    private void removePendingDirectorySearchRequests() {
        mDelayedDirectorySearchHandler.removeMessages(DIRECTORY_SEARCH_MESSAGE);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mOnChangeListener != null && data != null ) {
            mOnChangeListener.onDataChange(data);
        }
        if (!mEnabled) {
            return;
        }

        int loaderId = loader.getId();
        if (loaderId == DIRECTORY_LOADER_ID) {
            mDirectoryListStatus = STATUS_LOADED;
            mAdapter.changeDirectories(data);
            startLoading();
        } else {
            onPartitionLoaded(loaderId, data);
            if (isSearchMode()) {
                int directorySearchMode = getDirectorySearchMode();
                if (directorySearchMode != DirectoryListLoader.SEARCH_MODE_NONE) {
                    if (mDirectoryListStatus == STATUS_NOT_LOADED) {
                        mDirectoryListStatus = STATUS_LOADING;
                        getLoaderManager().initLoader(DIRECTORY_LOADER_ID, null, this);
                    } else {
                        startLoading();
                    }
                }
            } else {
                mDirectoryListStatus = STATUS_NOT_LOADED;
                getLoaderManager().destroyLoader(DIRECTORY_LOADER_ID);
            }
        }
        /**
        * SPRD:
        *       Bug 277267 After adding the contact interface, check some of the
        *       contacts, turn screen, full box is checked on the contacts and the
        *       Done button is grayed.
        * @{
        */
        if (isMultiPickerSupported()) {
            refreshRevertButton();
        }
        /**
        * @}
        */
    }

    public void onLoaderReset(Loader<Cursor> loader) {
    }

    protected void onPartitionLoaded(int partitionIndex, Cursor data) {
        if (partitionIndex >= mAdapter.getPartitionCount()) {
            // When we get unsolicited data, ignore it.  This could happen
            // when we are switching from search mode to the default mode.
            return;
        }

        mAdapter.changeCursor(partitionIndex, data);
        //setProfileHeader();
        showCount(partitionIndex, data);

        if (!isLoading()) {
            completeRestoreInstanceState();
        }
    }

    public boolean isLoading() {
        if (mAdapter != null && mAdapter.isLoading()) {
            return true;
        }

        if (isLoadingDirectoryList()) {
            return true;
        }

        return false;
    }

    public boolean isLoadingDirectoryList() {
        return isSearchMode() && getDirectorySearchMode() != DirectoryListLoader.SEARCH_MODE_NONE
                && (mDirectoryListStatus == STATUS_NOT_LOADED
                        || mDirectoryListStatus == STATUS_LOADING);
    }

    @Override
    public void onStop() {
        super.onStop();
        mContactsPrefs.unregisterChangeListener();
        mAdapter.clearPartitions();
        /**
        * SPRD:
        * 
        * @{
        */
        if (mSearchView != null && !mSearchMode) {
            mSearchView.clearFocus();
            hideSoftKeyboard();
        }
        /**
        * @}
        */
    }

    @Override
    public void onDestroy() {
//        if (mTts != null) {
//            mTts.stop();
//            mTts.shutdown();
//        }
        super.onDestroy();
    }

    protected void reloadData() {
        removePendingDirectorySearchRequests();
        mAdapter.onDataReload();
        mLoadPriorityDirectoriesOnly = true;
        mForceLoad = true;
        startLoading();
    }

    /**
     * Shows the count of entries included in the list. The default
     * implementation does nothing.
     */
    protected void showCount(int partitionIndex, Cursor data) {
        /**
        * SPRD:
        * 
        * @{
        */
        if (data == null || data.getCount() == 0) {
            prepareEmptyView();
            if (mSelectContacts != null) {
                mSelectContacts.setVisibility(View.GONE);
            }
			if (mSearchViewContainer != null && TextUtils.isEmpty(mQueryString)) {
				mSearchViewContainer.setVisibility(View.GONE);
			} else if (mSearchViewContainer != null && isSearchMode()) {
				mSearchViewContainer.setVisibility(View.VISIBLE);
			}
        } else {
            if (mSelectContacts != null) {
                mSelectContacts.setVisibility(View.VISIBLE);
            }
            if (mSearchViewContainer != null && mSearchVisible) {
                mSearchViewContainer.setVisibility(View.VISIBLE);
            }
            prepareListView();
        }
        /**
        * @}
        */
    }

    /**
     * Shows a view at the top of the list with a pseudo local profile prompting the user to add
     * a local profile. Default implementation does nothing.
     */
//    protected void setProfileHeader() {
//        mUserProfileExists = false;
//    }

    /**
     * Provides logic that dismisses this fragment. The default implementation
     * does nothing.
     */
    protected void finish() {
    }

    public void setSectionHeaderDisplayEnabled(boolean flag) {
        if (mSectionHeaderDisplayEnabled != flag) {
            mSectionHeaderDisplayEnabled = flag;
            if (mAdapter != null) {
                mAdapter.setSectionHeaderDisplayEnabled(flag);
            }
            configureVerticalScrollbar();
        }
    }

    public boolean isSectionHeaderDisplayEnabled() {
        return mSectionHeaderDisplayEnabled;
    }

    public void setVisibleScrollbarEnabled(boolean flag) {
        if (mVisibleScrollbarEnabled != flag) {
            mVisibleScrollbarEnabled = flag;
            configureVerticalScrollbar();
        }
    }

    public boolean isVisibleScrollbarEnabled() {
        return mVisibleScrollbarEnabled;
    }

    public void setVerticalScrollbarPosition(int position) {
        if (mVerticalScrollbarPosition != position) {
            mVerticalScrollbarPosition = position;
            configureVerticalScrollbar();
        }
    }

    private void configureVerticalScrollbar() {
        boolean hasScrollbar = isSectionHeaderDisplayEnabled() && isVisibleScrollbarEnabled();
        if (UniverseUtils.UNIVERSEUI_SUPPORT){
            hasScrollbar = false;
        }

        if (mListView != null) {
            mListView.setFastScrollEnabled(hasScrollbar);
            mListView.setFastScrollAlwaysVisible(hasScrollbar);
            mListView.setVerticalScrollbarPosition(mVerticalScrollbarPosition);
            mListView.setScrollBarStyle(ListView.SCROLLBARS_OUTSIDE_OVERLAY);
            int leftPadding = 0;
            int rightPadding = 0;
            if (mVerticalScrollbarPosition == View.SCROLLBAR_POSITION_LEFT) {
                leftPadding = mContext.getResources().getDimensionPixelOffset(
                        R.dimen.list_visible_scrollbar_padding);
            } else {
                rightPadding = mContext.getResources().getDimensionPixelOffset(
                        R.dimen.list_visible_scrollbar_padding);
            }
            /*
            * SPRD:
            *   Bug 329992
            *
            * @{
            */
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                leftPadding = 0;
                rightPadding = 0;
            }
            /*
            * @}
            */
            mListView.setPadding(leftPadding, mListView.getPaddingTop(),
                    rightPadding, mListView.getPaddingBottom());
        }
    }

    public void setPhotoLoaderEnabled(boolean flag) {
        mPhotoLoaderEnabled = flag;
        configurePhotoLoader();
    }

    public boolean isPhotoLoaderEnabled() {
        return mPhotoLoaderEnabled;
    }

    /**
     * Returns true if the list is supposed to visually highlight the selected item.
     */
    public boolean isSelectionVisible() {
        return mSelectionVisible;
    }

    public void setSelectionVisible(boolean flag) {
        this.mSelectionVisible = flag;
    }

    public void setQuickContactEnabled(boolean flag) {
        this.mQuickContactEnabled = flag;
    }

    public void setIncludeProfile(boolean flag) {
        mIncludeProfile = flag;
        if(mAdapter != null) {
            mAdapter.setIncludeProfile(flag);
        }
    }

    /**
     * Enter/exit search mode.  By design, a fragment enters search mode only when it has a
     * non-empty query text, so the mode must be tightly related to the current query.
     * For this reason this method must only be called by {@link #setQueryString}.
     *
     * Also note this method doesn't call {@link #reloadData()}; {@link #setQueryString} does it.
     */
    protected void setSearchMode(boolean flag) {
        if (mSearchMode != flag) {
            mSearchMode = flag;
            setSectionHeaderDisplayEnabled(!mSearchMode);

            if (!flag) {
                mDirectoryListStatus = STATUS_NOT_LOADED;
                getLoaderManager().destroyLoader(DIRECTORY_LOADER_ID);
            }

            if (mAdapter != null) {
                mAdapter.setPinnedPartitionHeadersEnabled(flag);
                mAdapter.setSearchMode(flag);

                mAdapter.clearPartitions();
                if (!flag) {
                    // If we are switching from search to regular display, remove all directory
                    // partitions after default one, assuming they are remote directories which
                    // should be cleaned up on exiting the search mode.
                    mAdapter.removeDirectoriesAfterDefault();
                }
                mAdapter.configureDefaultPartition(false, flag);
            }

			if (mListView != null) {
				if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
					mListView.setFastScrollEnabled(!flag);
				} else {
					mListView.setFastScrollEnabled(false);
				}
			}
        }
    }

    public final boolean isSearchMode() {
        return mSearchMode;
    }

    public final String getQueryString() {
        return mQueryString;
    }

    public void setQueryString(String queryString, boolean delaySelection) {
        // Normalize the empty query.
        if (TextUtils.isEmpty(queryString)) queryString = null;
        /*
        * SPRD:
        *       Bug 305452 355895
        * @{
        */
        Log.d(TAG,"contact search queryString==="+queryString);
        if (queryString != null) {
            String queryStringTemp;
            queryStringTemp = DatabaseUtils.sqlEscapeString(queryString);
            int index = queryStringTemp.indexOf(0);
            if (index != -1) {
                queryString = queryStringTemp;
                queryString = queryString.replace(queryString.charAt(index), ' ');
            }
        }
        /*
        * @}
        */
        if (!TextUtils.equals(mQueryString, queryString)) {
            mQueryString = queryString;
            setSearchMode(!TextUtils.isEmpty(mQueryString));
            if (mAdapter != null) {
                mAdapter.setQueryString(queryString);
                reloadData();
            }
        }
    }

    public int getDirectoryLoaderId() {
        return DIRECTORY_LOADER_ID;
    }

    public int getDirectorySearchMode() {
        return mDirectorySearchMode;
    }

    public void setDirectorySearchMode(int mode) {
        mDirectorySearchMode = mode;
    }

    public boolean isLegacyCompatibilityMode() {
        return mLegacyCompatibility;
    }

    public void setLegacyCompatibilityMode(boolean flag) {
        mLegacyCompatibility = flag;
    }

    protected int getContactNameDisplayOrder() {
        return mDisplayOrder;
    }

    protected void setContactNameDisplayOrder(int displayOrder) {
        mDisplayOrder = displayOrder;
        if (mAdapter != null) {
            mAdapter.setContactNameDisplayOrder(displayOrder);
        }
    }

    public int getSortOrder() {
        return mSortOrder;
    }

    public void setSortOrder(int sortOrder) {
        mSortOrder = sortOrder;
        if (mAdapter != null) {
            mAdapter.setSortOrder(sortOrder);
        }
    }

    public void setDirectoryResultLimit(int limit) {
        mDirectoryResultLimit = limit;
    }

    protected boolean loadPreferences() {
        boolean changed = false;
        if (getContactNameDisplayOrder() != mContactsPrefs.getDisplayOrder()) {
            setContactNameDisplayOrder(mContactsPrefs.getDisplayOrder());
            changed = true;
        }

        if (getSortOrder() != mContactsPrefs.getSortOrder()) {
            setSortOrder(mContactsPrefs.getSortOrder());
            changed = true;
        }

        return changed;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        onCreateView(inflater, container);

        mAdapter = createListAdapter();
        mAdapter.setCheckedLimitCount(mCheckedLimitCount);

        boolean searchMode = isSearchMode();
        mAdapter.setSearchMode(searchMode);
        mAdapter.configureDefaultPartition(false, searchMode);
        mAdapter.setPhotoLoader(mPhotoManager);
        /**
        * SPRD:
        * 
        * @{
        */
        mAdapter.setMultiPickerSupported(mMultiPickerSupported);
        /**
        * @}
        */
        mListView.setAdapter(mAdapter);

        if (!isSearchMode()) {
            mListView.setFocusableInTouchMode(true);
            mListView.requestFocus();
        }

        return mView;
    }

    protected void onCreateView(LayoutInflater inflater, ViewGroup container) {
        mView = inflateView(inflater, container);

        mListView = (ListView)mView.findViewById(android.R.id.list);
        if (mListView == null) {
            throw new RuntimeException(
                    "Your content must have a ListView whose id attribute is " +
                    "'android.R.id.list'");
        }
        View emptyView = mView.findViewById(android.R.id.empty);
        if (emptyView != null) {
            mListView.setEmptyView(emptyView);
/*SPRD @{*/
            if (emptyView instanceof ContactListEmptyView) {
                mEmptyView = (ContactListEmptyView)emptyView;
            }
/*SPRD @}*/
        }

        mListView.setOnItemClickListener(this);
        mListView.setOnItemSelectedListener(this);
        mListView.setOnFocusChangeListener(this);
        mListView.setOnTouchListener(this);
		if (!UniverseUtils.UNIVERSEUI_SUPPORT) {
			mListView.setFastScrollEnabled(!isSearchMode());
		} else {
			mListView.setFastScrollEnabled(false);
		}

        // Tell list view to not show dividers. We'll do it ourself so that we can *not* show
        // them when an A-Z headers is visible.
        mListView.setDividerHeight(0);

        // We manually save/restore the listview state
        mListView.setSaveEnabled(false);
        /**
        * SPRD:
        * 
        * @{
        */
        if (mContextMenuAdapter != null) {
            mListView.setOnCreateContextMenuListener(mContextMenuAdapter);
        }
        configureVerticalScrollbar();
        configurePhotoLoader();
        judgeCheckedLimitCount();


        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            mSearchViewContainer = (RelativeLayout) getView().findViewById(
                    R.id.search_view_container);
            if (mSearchViewContainer != null) {
                if (!mSearchVisible) {
                    mSearchViewContainer.setVisibility(View.GONE);
                } else {
                    mSearchView = (CustomSearchView) getView().findViewById(
                            R.id.search_view);
                    mSearchView.addTextChangedListener(this);
                    //mSearchView.clearFocus();
                    mClearAll = (ImageView) getView().findViewById(
                            R.id.clear_all_img);
                    mClearAll.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mSearchView.setText("");
                            mClearAll.setVisibility(View.GONE);
                        }
                    });
                }
            }
            if (mFirstDividerVisible) {
                mFirstDivider = getView().findViewById(R.id.divider);
                mFirstDivider.setVisibility(View.GONE);//MARK
            }
        }

        if (isMultiPickerSupported()) {
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                ViewStub selectStub = (ViewStub) getView().findViewById(
                        R.id.select_contact_stub);
                if (selectStub != null) {
                    mSelectContacts = (RelativeLayout) selectStub.inflate();

                    mSelectAll = (CheckBox) mSelectContacts
                            .findViewById(R.id.select_contact_cb);
                    mSelectAllTxt = (TextView) mSelectContacts
                            .findViewById(R.id.select_all_contact);
                    mSelectAll.setOnClickListener(this);
                    mSelectedContactsNum = (TextView)mSelectContacts
                            .findViewById(R.id.select_contact_num);
                    mSelectAll.setOnFocusChangeListener(new OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View arg0, boolean arg1) {
                            if (arg1) {
                                mSelectContacts.setBackground(getResources().getDrawable(R.color.contact_search_result_title_text_color));
                                mSelectedContactsNum.setTextColor(getResources().getColor(R.color.text_selected_color));
                            } else {
                                mSelectContacts.setBackground(getResources().getDrawable(android.R.color.white));
                                mSelectedContactsNum.setTextColor(getResources().getColor(R.color.text_unselected_color));
                            }
                        }
                    });

                    getActivity().getActionBar().addOnMenuVisibilityListener(new ActionBar.OnMenuVisibilityListener() {
                    @Override
                    public void onMenuVisibilityChanged(boolean isVisible) {
                        if (isAdded()) {
                            if (isVisible) {
                                mSelectContacts.setBackground(getResources().getDrawable(android.R.color.white));
                                mSelectedContactsNum.setTextColor(getResources().getColor(R.color.text_unselected_color));
                            } else {
                                if (mSelectAll.isFocused()) {
                                    mSelectContacts.setBackground(getResources()
                                            .getDrawable(R.color.contact_search_result_title_text_color));
                                    mSelectedContactsNum.setTextColor(getResources().getColor(R.color.text_selected_color));
                                } else {
                                    mSelectContacts.setBackground(getResources().getDrawable(android.R.color.white));
                                    mSelectedContactsNum.setTextColor(getResources().getColor(R.color.text_unselected_color));
                                }
                            }
                        }
                    }
                    });

                }
            } else {
                // show footer
                ViewStub stub = (ViewStub) getView().findViewById(R.id.footer_stub);
                if (stub != null) {
                    mFooter = (LinearLayout) stub.inflate();
                    mFooter.setVisibility(View.VISIBLE);
                    mOkBtn = (Button) mFooter.findViewById(R.id.done);
                    mRevertBtn = (Button) mFooter.findViewById(R.id.revert);
                    mRevertBtn.setText(R.string.menu_select_all);
                    mOkBtn.setEnabled(false);
                    mOkBtn.setOnClickListener(this);
                    mRevertBtn.setOnClickListener(this);
                }
            }
        } 
        /**
        * @}
        */
/*** BUG #46829 wanglei 20190418 add begin ***/
		mCircularListController = new CircularListController(mSearchView, mListView);
/*** BUG #46829 wanglei 20190418 add end ***/
    }

    protected void configurePhotoLoader() {
        if (isPhotoLoaderEnabled() && mContext != null) {
            if (mPhotoManager == null) {
                mPhotoManager = ContactPhotoManager.getInstance(mContext);
            }
            if (mListView != null) {
                mListView.setOnScrollListener(this);
            }
            if (mAdapter != null) {
                mAdapter.setPhotoLoader(mPhotoManager);
            }
        }
    }

    protected void configureAdapter() {
        if (mAdapter == null) {
            return;
        }

        mAdapter.setQuickContactEnabled(mQuickContactEnabled);
        mAdapter.setIncludeProfile(mIncludeProfile);
        mAdapter.setQueryString(mQueryString);
        mAdapter.setDirectorySearchMode(mDirectorySearchMode);
        mAdapter.setPinnedPartitionHeadersEnabled(mSearchMode);
        mAdapter.setContactNameDisplayOrder(mDisplayOrder);
        mAdapter.setSortOrder(mSortOrder);
        mAdapter.setSectionHeaderDisplayEnabled(mSectionHeaderDisplayEnabled);
        mAdapter.setSelectionVisible(mSelectionVisible);
        mAdapter.setDirectoryResultLimit(mDirectoryResultLimit);
        mAdapter.setDarkTheme(mDarkTheme);
        /**
        * SPRD:
        * 
        * @{
        */
        mAdapter.setMultiPickerSupported(mMultiPickerSupported);
        if (mFilter != null) {
            mAdapter.setFilter(mFilter);
            mFilter = null;
        }
        if (isMultiPickerSupported()) {
            mAdapter.setAllCheckedItemIds(mCheckedItems);
            mAdapter.setContactDataCache(mContactDataCache);
            mCheckedItems = null;
            refreshRevertButton();
        }
        /**
        * @}
        */
    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        if (scrollState == OnScrollListener.SCROLL_STATE_FLING) {
            mPhotoManager.pause();
        } else if (isPhotoLoaderEnabled()) {
            mPhotoManager.resume();
        }
    }

    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            Log.e(TAG, "Language is not available.");
        } else {
            Log.e(TAG, "Could not initialize TextToSpeech.");
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        hideSoftKeyboard();

        int adjPosition = position - mListView.getHeaderViewsCount();
        if (adjPosition >= 0) {
            /**
            * SPRD:
            * 
            * @{
            */
            if (isMultiPickerSupported()) {
                boolean isChecked = getAdapter().isChecked(adjPosition);
                if (!isChecked
                        && getAdapter().getCurrentCheckedItems().size() >= mCheckedLimitCount) {
                    if (mCheckedLimitCount == CHECKED_ITEMS_MAX) {
                        Toast.makeText(mContext, mContext.getString(
                            R.string.contacts_selection_too_more, mCheckedLimitCount),
                            Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        Toast.makeText(mContext, mContext.getString(
                                R.string.contacts_selection_for_mms_limit, mCheckedLimitCount),
                                Toast.LENGTH_SHORT)
                                .show();
                    }
                    return;
                }
                getAdapter().setChecked(adjPosition, !isChecked);
                refreshRevertButton();
                getAdapter().notifyDataSetChanged();
                return;
            }
            /**
            * @}
            */
            onItemClick(adjPosition, id);
        }
    }

    private void hideSoftKeyboard() {
        // Hide soft keyboard, if visible
        InputMethodManager inputMethodManager = (InputMethodManager)
                mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(mListView.getWindowToken(), 0);
    }

    /**
     * Dismisses the soft keyboard when the list takes focus.
     */
    @Override
    public void onFocusChange(View view, boolean hasFocus) {
        Log.d(TAG, "onFocusChange: view = "+view+";hasFocus = "+hasFocus);
        if (view == mListView && hasFocus) {
            hideSoftKeyboard();
        }
    }

    /**
     * Dismisses the soft keyboard when the list is touched.
     */
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (view == mListView) {
            hideSoftKeyboard();
        }
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        /**
        * SPRD:
        * 
        * @{
        */
        if (mSearchView != null) {
            mSearchView.clearFocus();
        }
        /**
        * @}
        */
        removePendingDirectorySearchRequests();
    }

    /**
     * Restore the list state after the adapter is populated.
     */
    protected void completeRestoreInstanceState() {
        if (mListState != null) {
            mListView.onRestoreInstanceState(mListState);
            mListState = null;
        }
    }

    public void setDarkTheme(boolean value) {
        mDarkTheme = value;
        if (mAdapter != null) mAdapter.setDarkTheme(value);
    }

    /**
     * Processes a result returned by the contact picker.
     */
    public void onPickerResult(Intent data) {
        throw new UnsupportedOperationException("Picker result handler is not implemented.");
    }

    private ContactsPreferences.ChangeListener mPreferencesChangeListener =
            new ContactsPreferences.ChangeListener() {
        @Override
        public void onChange() {
            loadPreferences();
            reloadData();
        }
    };

    private int getDefaultVerticalScrollbarPosition() {
        final Locale locale = Locale.getDefault();
        final int layoutDirection = TextUtils.getLayoutDirectionFromLocale(locale);
        switch (layoutDirection) {
            case View.LAYOUT_DIRECTION_RTL:
                return View.SCROLLBAR_POSITION_LEFT;
            case View.LAYOUT_DIRECTION_LTR:
            default:
                return View.SCROLLBAR_POSITION_RIGHT;
        }
    }



    /**
    * SPRD:
    * 
    * @{
    */
    /**
     * Configures the empty view. It is called when we are about to populate
     * the list with an empty cursor.
     */
    protected void prepareEmptyView() {
        Log.d(TAG, "prepareEmptyView: enter");
        mListView.setVisibility(View.GONE);
        //mView.findViewById(R.id.list_container).setVisibility(View.GONE);//mark
        if (mOnChangeListener != null) {
            mOnChangeListener.hideCenterSkView();
        }
    }

    protected void prepareListView() {
        Log.d(TAG, "prepareListView: enter");
        if (mView == null) {
            Log.e("monkey", "Exception may caused, because mView is null, dumpStack");
            Thread.dumpStack();
        } else if (mView.findViewById(R.id.list_container) == null) {
            Log.e("monkey", "Exception may caused, because mView.findViewById is null, dumpStack");
            Thread.dumpStack();
        } else {
            mView.findViewById(R.id.list_container).setVisibility(View.VISIBLE);
        }
        mListView.setVisibility(View.VISIBLE);
        if (mFooter != null) {
            mFooter.setVisibility(View.VISIBLE);
            refreshRevertButton();
        }
        Log.d(TAG, "prepareListView: getEmptyView()="+getEmptyView());//mark
        if (getEmptyView() != null) {
            Log.d(TAG,"prepareListView getEmptyView!=NULL emptyContainer GONE");
            mView.findViewById(R.id.emptyContainer).setVisibility(View.GONE);//MARK
            getEmptyView().setVisibility(View.GONE);
        }
    }

    public void setMultiPickerSupported(boolean suppported) {
        mMultiPickerSupported = suppported;
    }

    public boolean isMultiPickerSupported() {
        return mMultiPickerSupported;
    }

    public void setContextMenuAdapter(ContextMenuAdapter adapter) {
        mContextMenuAdapter = adapter;
        if (mListView != null) {
            mListView.setOnCreateContextMenuListener(adapter);
        }
    }

    public ContextMenuAdapter getContextMenuAdapter() {
        return mContextMenuAdapter;
    }

    @Override
    public void onResume() {
        super.onResume();
		//BUG #48360  add by maoyufeng 20190531 begin 
        if(mListView != null && mListView.getChildCount() > 0){
             mListView.setSelection(0);
        }
		//BUG #48360  add by maoyufeng 20190531 end
        /* SPRD: add for bug648879, delete the search view auto focus. @{ */
        if (mSearchView != null) {
            if (!mSearchMode) {
                mSearchView.clearFocus();
                hideSoftKeyboard();
            }
        }
        /* @{ */
    }

    /**
     * Dismisses the search UI along with the keyboard if the filter text is
     * empty.
     */
    public void onClose() {
        hideSoftKeyboard();
        finish();
    }

    protected void setEmptyText(int resourceId) {
        if (getEmptyView() == null) {
            return;
        }
        //mView.findViewById(R.id.emptyContainer).setVisibility(View.VISIBLE);//MARK
        getEmptyView().setVisibility(View.VISIBLE);

        TextView empty = (TextView) getEmptyView().findViewById(R.id.emptyText);
        if (UniverseUtils.UNIVERSEUI_SUPPORT && !TextUtils.isEmpty(mQueryString)) {
            empty.setText(mContext.getText(R.string.no_match_contact));
        } else {
            empty.setText(mContext.getText(resourceId));
        }
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            //empty.setPadding(0, 80, 0, 0);
            empty.setGravity(Gravity.CENTER);
        }

        empty.setVisibility(View.VISIBLE);
    }

    // TODO redesign into an async task or loader
    protected boolean isSyncActive() {
        Account[] accounts = AccountManager.get(mContext).getAccounts();
        if (accounts != null && accounts.length > 0) {
            IContentService contentService = ContentResolver.getContentService();
            for (Account account : accounts) {
                try {
                    if (contentService.isSyncActive(account, ContactsContract.AUTHORITY)) {
                        return true;
                    }
                } catch (RemoteException e) {
                    Log.e(TAG, "Could not get the sync status");
                }
            }
        }
        return false;
    }

    public void setFilter(ContactListFilter filter) {

    }

    protected boolean hasIccCard() {
        TelephonyManager telephonyManager =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        return telephonyManager.hasIccCard();
    }

    @Override
    public void onClick(View v) {
        ArrayList<String> ret = new ArrayList<String>();
        ContactEntryListAdapter adapter = getAdapter();
        if (v == mOkBtn) {
            Intent intent = getActivity().getIntent();
            int mode = 0;
            if (intent != null) {
                mode = intent.getIntExtra("mode", 0);
            }
            if (mode == SUBACTIVITY_BATCH_DELETE) {
                new AlertDialog.Builder(getActivity())
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        onMultiPickerSelected();
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })
                        .setTitle(android.R.string.dialog_alert_title)
                        .setMessage(R.string.alert_delete_contact)
                        .show();
            } else {
                onMultiPickerSelected();
            }
        } else if (v == mRevertBtn) {
            toggleRevertButton();
        } else if (v == mSelectAll) {
            toggleRevertButton();
        }
    }

    public void onMultiPickerSelected() {
    }

    private void toggleRevertButton() {
        ContactEntryListAdapter adapter = getAdapter();
        int currentPosition = mListView.getLastVisiblePosition();
        if (adapter.isAllChecked()) {
            adapter.checkAll(false, mSelectionOffset);
            mSelectionOffset = 0;
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                /**
                 * SPRD Bug654153 delete selection hints for pikel
                 * {@ */
//                mSelectAllTxt.setText(R.string.cancel_select_all_contacts);
                mSelectAllTxt.setVisibility(View.GONE);
                /**@}*/
                mSelectAll.setChecked(false);
                refreshDone();
            } else {
                mRevertBtn.setText(R.string.menu_select_all);
                mOkBtn.setEnabled(false);
            }
        } else {
            if (currentPosition + 1 <= mCheckedLimitCount) {
                adapter.checkAll(true, 0);
            } else {
                mSelectionOffset = currentPosition + 1 - mCheckedLimitCount;
                adapter.checkAll(true, mSelectionOffset);
            }

            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
               /**
                 * SPRD Bug654153 delete selection hints for pikel
                 * {@ */
//                mSelectAllTxt.setText(R.string.cancel_select_all_contacts);
                mSelectAllTxt.setVisibility(View.GONE);
                /**@}*/
                mSelectAll.setChecked(true);
                refreshDone();
            } else {
                mRevertBtn.setText(R.string.menu_select_none);
                mOkBtn.setEnabled(true);
            }
            if (adapter.getCount() > mCheckedLimitCount) {
                if (mCheckedLimitCount == CHECKED_ITEMS_MAX) {
                    Toast.makeText(mContext, mContext.getString(
                            R.string.contacts_selection_too_more, mCheckedLimitCount),
                            Toast.LENGTH_SHORT)
                            .show();
                } else {
                    Toast.makeText(mContext, mContext.getString(
                            R.string.contacts_selection_for_mms_limit, mCheckedLimitCount),
                            Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void refreshRevertButton() {
        ContactEntryListAdapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        if (adapter.isAllChecked()) {
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                /**
                 * SPRD Bug654153 delete selection hints for pikel
                 * {@ */
//                mSelectAllTxt.setText(R.string.cancel_select_all_contacts);
                mSelectAllTxt.setVisibility(View.GONE);
                /** @} */
                mSelectAll.setChecked(true);
                refreshDone();
            } else {
                mRevertBtn.setText(R.string.menu_select_none);
                mOkBtn.setEnabled(true);
            }

        } else {
            if (UniverseUtils.UNIVERSEUI_SUPPORT) {
                /**
                 * SPRD Bug654153 delete selection hints for pikel
                 * {@ */
//                mSelectAllTxt.setText(R.string.select_all_contacts);
                mSelectAllTxt.setVisibility(View.GONE);
                /** @} */
                mSelectAll.setChecked(false);
                refreshDone();
            } else {
                mRevertBtn.setText(R.string.menu_select_all);
                if (adapter.hasCheckedItems()) {
                    mOkBtn.setEnabled(true);
                } else {
                    mOkBtn.setEnabled(false);
                }
            }
        }
    }

    protected int getListFilterId() {
        return R.string.list_filter_all_accounts;
    }

    @Override
    public void afterTextChanged(Editable s) {
        // TODO Auto-generated method stub
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
            int after) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onTextChanged(CharSequence queryString, int start, int before, int count) {
        // TODO Auto-generated method stub
        /*if (!TextUtils.isEmpty(queryString.toString().trim())) {
            mClearAll.setVisibility(View.VISIBLE);
        } else {
            mClearAll.setVisibility(View.GONE);
        }*/
        setQueryString(queryString.toString().trim(), true);
    }

    private void refreshDone() {
        Intent intent = new Intent();
        intent.setAction("com.android.contacts.common.action.SSU");
        if(getActivity()!=null){
            getActivity().sendBroadcast(intent);
            refreshSelectedNum();
        }
    }

    private void refreshSelectedNum() {
        int count = mAdapter.getCurrentCheckedItems().size();
        String format = getResources().getQuantityText(
                R.plurals.listSelectedContacts, count).toString();
        if (mSelectedContactsNum != null) {
            mSelectedContactsNum.setText(String.format(format, count));
        }
    }

    public boolean getSelecStatus() {
        return mAdapter.hasCheckedItems();
    }
    
    public void setSearchVisible(boolean visible) {
        mSearchVisible = visible;
    }

    public void setFirstDividerVisible(boolean visible) {
        mFirstDividerVisible = visible;
    }

    public void setSelectTextVisible(boolean visible) {
        mSelectTextVisible = visible;
    }

    public void clearToggleRevert() {
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            /**
             * SPRD Bug654153 delete selection hints for pikel
             * {@ */
//            mSelectAllTxt.setText(R.string.select_all_contacts);
            mSelectAllTxt.setVisibility(View.GONE);
            /** @}*/
            mSelectAll.setChecked(false);
        }
    }

    public void clearAllCheckItems() {
        mCheckedItems = null;
        mAdapter.clearCheckedItems();
        mAdapter.notifyDataSetChanged();
        refreshSelectedNum();
    }

    public View getEmptyView() {
       // return mListView.getEmptyView();
       return mEmptyView;
    }

    public void judgeCheckedLimitCount() {
        Intent intent = getActivity().getIntent();
        if(intent != null){
            mCheckedLimitCount = intent.getIntExtra(KEY_CHECKED_LIMIT_COUNT,CHECKED_ITEMS_MAX);
        }
    }
    
    //bug  264659
    public void setSearchViewText(String queryString){
        if(!TextUtils.isEmpty(queryString) && mSearchView != null){
            mSearchView.setText(queryString);
            mSearchView.setSelection(queryString.length());
        } else if (mSearchView != null) {
            mSearchView.setText("");
        }
    }

	public void setSearchViewPermanentVisible(boolean visible) {
		if (mSearchViewContainer != null) {
			if (visible) {
				mSearchViewContainer.setVisibility(View.VISIBLE);
			} else {
				mSearchViewContainer.setVisibility(View.GONE);
			}
		}
	}

    public void setContactCacheModel(int mode, String mainIndex, String minorIndex) {
        this.mContactDataCache.setModel(mode, mainIndex, minorIndex);
    }

    public MultiContactDataCacheUtils getContactCache() {
        return mContactDataCache;
    }
    /**
    * @}
    */
/*** BUG #46829 wanglei 20190418 add begin ***/
	private CircularListController mCircularListController;

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean result = mCircularListController.onKeyDown(keyCode, event);

		if (!result) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_0:
			case KeyEvent.KEYCODE_1:
			case KeyEvent.KEYCODE_2:
			case KeyEvent.KEYCODE_3:
			case KeyEvent.KEYCODE_4:
			case KeyEvent.KEYCODE_5:
			case KeyEvent.KEYCODE_6:
			case KeyEvent.KEYCODE_7:
			case KeyEvent.KEYCODE_8:
			case KeyEvent.KEYCODE_9:
			case KeyEvent.KEYCODE_STAR:
			case KeyEvent.KEYCODE_POUND:
				if (!mSearchView.hasFocus()) {
					mSearchView.requestFocus();
					result = true;
				}
				break;
			}
		}

		return result;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		mCircularListController.onDestroy();
		mCircularListController = null;
	}
/*** BUG #46829 wanglei 20190418 add end ***/
}
