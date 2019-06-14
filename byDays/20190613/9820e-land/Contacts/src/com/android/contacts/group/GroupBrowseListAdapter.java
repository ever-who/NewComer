/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.contacts.group;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.Groups;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import com.android.contacts.GroupListLoader;
import com.android.contacts.group.GroupListItem;
import android.database.MergeCursor;
import com.android.contacts.common.R;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.AccountTypeManager;
import com.sprd.contacts.common.group.GroupPhotoAdapter;
import com.sprd.contacts.common.util.UniverseUtils;
import com.sprd.contacts.common.model.account.PhoneAccountType;
import com.google.common.base.Objects;
import com.sprd.contacts.group.GroupBrowseListFragmentSprd;
import java.util.ArrayList;
import java.util.HashMap;
/**
 * Adapter to populate the list of groups.
 */
public class GroupBrowseListAdapter extends BaseAdapter {

    private final Context mContext;
    private final LayoutInflater mLayoutInflater;
    private final AccountTypeManager mAccountTypeManager;

    private Cursor mCursor;

    private boolean mSelectionVisible;
    private Uri mSelectedGroupUri;

    public GroupBrowseListAdapter(Context context) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mAccountTypeManager = AccountTypeManager.getInstance(mContext);
    }

    public void setCursor(Cursor cursor) {
        mCursor = cursor;

        // If there's no selected group already and the cursor is valid, then by default, select the
        // first group
        if (mSelectedGroupUri == null && cursor != null && cursor.getCount() > 0) {
            GroupListItem firstItem = getItem(0);
            long groupId = (firstItem == null) ? 0 : firstItem.getGroupId();
            mSelectedGroupUri = getGroupUriFromId(groupId);
        }

        notifyDataSetChanged();
    }

    public int getSelectedGroupPosition() {
        if (mSelectedGroupUri == null || mCursor == null || mCursor.getCount() == 0) {
            return -1;
        }

        int index = 0;
        mCursor.moveToPosition(-1);
        while (mCursor.moveToNext()) {
            long groupId = mCursor.getLong(GroupListLoader.GROUP_ID);
            Uri uri = getGroupUriFromId(groupId);
            if (mSelectedGroupUri.equals(uri)) {
                  return index;
            }
            index++;
        }
        return -1;
    }

    public void setSelectionVisible(boolean flag) {
        mSelectionVisible = flag;
    }

    public void setSelectedGroup(Uri groupUri) {
        mSelectedGroupUri = groupUri;
    }

    private boolean isSelectedGroup(Uri groupUri) {
        return mSelectedGroupUri != null && mSelectedGroupUri.equals(groupUri);
    }

    public Uri getSelectedGroup() {
        return mSelectedGroupUri;
    }

    @Override
    public int getCount() {
        /**
         * SPRD:
         * Original Android code:
         * return (mCursor == null || mCursor.isClosed()) ? 0 : mCursor.getCount();
         *
         * @{
         */
        if (mCursor instanceof MergeCursor) {
            MergeCursor mergeCursor = (MergeCursor) mCursor;
            return (mergeCursor == null || mergeCursor.isClosed()) ? 0 : mergeCursor.getExtras()
                    .getInt(GroupBrowseListFragmentSprd.CURSOR_KEY_COUNT);
        } else {
            return (mCursor == null || mCursor.isClosed()) ? 0 : mCursor.getCount();
        }
        /**
         * @}
         */
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public GroupListItem getItem(int position) {
        if (mCursor == null || mCursor.isClosed() || !mCursor.moveToPosition(position)) {
            return null;
        }
        String accountName = mCursor.getString(GroupListLoader.ACCOUNT_NAME);
        String accountType = mCursor.getString(GroupListLoader.ACCOUNT_TYPE);
        String dataSet = mCursor.getString(GroupListLoader.DATA_SET);
        long groupId = mCursor.getLong(GroupListLoader.GROUP_ID);
        String title = mCursor.getString(GroupListLoader.TITLE);
        int memberCount = mCursor.getInt(GroupListLoader.MEMBER_COUNT);

        // Figure out if this is the first group for this account name / account type pair by
        // checking the previous entry. This is to determine whether or not we need to display an
        // account header in this item.
        int previousIndex = position - 1;
        boolean isFirstGroupInAccount = true;
        if (previousIndex >= 0 && mCursor.moveToPosition(previousIndex)) {
            String previousGroupAccountName = mCursor.getString(GroupListLoader.ACCOUNT_NAME);
            String previousGroupAccountType = mCursor.getString(GroupListLoader.ACCOUNT_TYPE);
            String previousGroupDataSet = mCursor.getString(GroupListLoader.DATA_SET);

            if (accountName.equals(previousGroupAccountName) &&
                    accountType.equals(previousGroupAccountType) &&
                    Objects.equal(dataSet, previousGroupDataSet)) {
                isFirstGroupInAccount = false;
            }
        }

        return new GroupListItem(accountName, accountType, dataSet, groupId, title,
                isFirstGroupInAccount, memberCount);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        GroupListItem entry = getItem(position);
        View result;
        GroupListItemViewCache viewCache;
        if (convertView != null) {
            result = convertView;
            viewCache = (GroupListItemViewCache) result.getTag();
        } else {
            /**
             * SPRD:
             *      The following code is added by sprdUUI
             * Original Android code:
             *  result = mLayoutInflater.inflate(R.layout.group_browse_list_item, parent, false);
             *
             * @{
             */
            if (UniverseUtils.UNIVERSEUI_SUPPORT){
                result = mLayoutInflater.inflate(R.layout.group_browse_list_item_overlay, parent, false);
            }else{
                result = mLayoutInflater.inflate(R.layout.group_browse_list_item, parent, false);
            }
            /**
             * @}
             */
            viewCache = new GroupListItemViewCache(result);
            result.setTag(viewCache);
        }

        // Add a header if this is the first group in an account and hide the divider
        if (entry.isFirstGroupInAccount()) {
            bindHeaderView(entry, viewCache);
            viewCache.accountHeader.setVisibility(View.VISIBLE);
            /**
             * SPRD:
             *      The following code is added by sprdUUI
             * Original Android code:
             *  viewCache.divider.setVisibility(View.GONE);
             *
             * @{
             */
            if (!UniverseUtils.UNIVERSEUI_SUPPORT){
                viewCache.divider.setVisibility(View.GONE);
            }
            /**
             * @}
             */
            if (position == 0) {
                // Have the list's top padding in the first header.
                //
                // This allows the ListView to show correct fading effect on top.
                // If we have topPadding in the ListView itself, an inappropriate padding is
                // inserted between fading items and the top edge.
                /**
                 * SPRD:
                 *      The following code is added by sprdUUI
                 * Original Android code:
                 *  viewCache.accountHeaderExtraTopPadding.setVisibility(View.VISIBLE);
                 *
                 * @{
                 */
                if (UniverseUtils.UNIVERSEUI_SUPPORT){
                    viewCache.accountHeaderExtraTopPadding.setVisibility(View.GONE);
                }else{
                    viewCache.accountHeaderExtraTopPadding.setVisibility(View.VISIBLE);
                }
                /**
                 * @}
                 */
            } else {
                viewCache.accountHeaderExtraTopPadding.setVisibility(View.GONE);
            }
        } else {
            viewCache.accountHeader.setVisibility(View.GONE);
            /**
             * SPRD:
             *      The following code is added by sprdUUI
             * Original Android code:
             *   viewCache.divider.setVisibility(View.VISIBLE);
             *
             * @{
             */
            if (!UniverseUtils.UNIVERSEUI_SUPPORT){
                viewCache.divider.setVisibility(View.VISIBLE);
            }
            /**
             * @}
             */
            viewCache.accountHeaderExtraTopPadding.setVisibility(View.GONE);
        }

        // Bind the group data
        Uri groupUri = getGroupUriFromId(entry.getGroupId());
        String memberCountString = mContext.getResources().getQuantityString(
                R.plurals.group_list_num_contacts_in_group, entry.getMemberCount(),
                entry.getMemberCount());
        viewCache.setUri(groupUri);
        viewCache.groupTitle.setText(entry.getTitle());
        viewCache.groupMemberCount.setText(memberCountString);

        if (mSelectionVisible) {
            result.setActivated(isSelectedGroup(groupUri));
        }
        /**
        * SPRD:
        *   The following code is added by sprdUUI
        * @{
        */
        setGroupPhotoAdapter(entry, viewCache);
        /**
        * @}
        */
        return result;
    }

    private void bindHeaderView(GroupListItem entry, GroupListItemViewCache viewCache) {
        AccountType accountType = mAccountTypeManager.getAccountType(
                entry.getAccountType(), entry.getDataSet());
        viewCache.accountType.setText(accountType.getDisplayLabel(mContext).toString());

        /**
        * SPRD:
        *   Due to account name is Chinese from the database, so need to use the multi-language
        *   to fit different countries.
        *
        * Original Android code:
        *   viewCache.accountName.setText(entry.getAccountName());
        *
        * @{
        */
        if (PhoneAccountType.ACCOUNT_TYPE.equals(entry.getAccountType())) {
            viewCache.accountName.setText(mContext.getString(R.string.label_phone));
        }else {
            viewCache.accountName.setText(entry.getAccountName());
        }
        /**
        * @}
        */
    }

    private static Uri getGroupUriFromId(long groupId) {
        return ContentUris.withAppendedId(Groups.CONTENT_URI, groupId);
    }

    /**
     * Cache of the children views of a contact detail entry represented by a
     * {@link GroupListItem}
     */
    public static class GroupListItemViewCache {
        public final TextView accountType;
        public final TextView accountName;
        public final TextView groupTitle;
        public final TextView groupMemberCount;
        public final View accountHeader;
        public final View accountHeaderExtraTopPadding;
        public final View divider;
        private Uri mUri;
        /**
         * SPRD: This function is added by sprd, and can view the picture in the
         *  groupListItem.
         *
         * @{
         */
        public final GridView groupMember;
        /**
         * @}
         */

        public GroupListItemViewCache(View view) {
            /**
             * SPRD:
             *      The following code is added by sprdUUI
             * Original Android code:
             *  accountType = (TextView)view.findViewById(R.id.account_type);
             *  accountName = (TextView)view.findViewById(R.id.account_name);
             *  accountHeader = view.findViewById(R.id.group_list_header);
             *
             * @{
             */
            if (UniverseUtils.UNIVERSEUI_SUPPORT){
                accountType = (TextView) view.findViewById(R.id.account_type_overlay);
                accountName = (TextView) view.findViewById(R.id.account_name_overlay);
                accountHeader = view.findViewById(R.id.group_list_header_overlay);
            }else{
                accountType = (TextView) view.findViewById(R.id.account_type);
                accountName = (TextView) view.findViewById(R.id.account_name);
                accountHeader = view.findViewById(R.id.group_list_header);
            }
            if (accountName != null && TelephonyManager.getPhoneCount() == 1) {
                accountName.setVisibility(View.GONE);
            }
            groupMember = (GridView)view.findViewById(R.id.group_member);
            /**
             * @}
             */
            groupTitle = (TextView) view.findViewById(R.id.label);
            groupMemberCount = (TextView) view.findViewById(R.id.count);
            accountHeaderExtraTopPadding = view.findViewById(R.id.header_extra_top_padding);
            divider = view.findViewById(R.id.divider);
        }

        public void setUri(Uri uri) {
            mUri = uri;
        }

        public Uri getUri() {
            return mUri;
        }
    }

    /**
     * SPRD:The following code is added by sprd
     *
     * @{
     */
    private static final String TAG = GroupBrowseListAdapter.class.getSimpleName();

    public HashMap<Long, ArrayList<Uri>> mHashMap;

    public void setUriMap(HashMap<Long, ArrayList<Uri>> hashMap) {
        mHashMap = hashMap;
    }

    private void setGroupPhotoAdapter(GroupListItem entry, GroupListItemViewCache viewCache) {
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            ArrayList<Uri> photoUri;
            ArrayList<HashMap<String, Object>> memList = new ArrayList<HashMap<String, Object>>();
            photoUri = mHashMap == null ? null : mHashMap.get(entry.getGroupId());

            Uri uri;
            if (photoUri != null) {
                for (int i = 0; i < photoUri.size(); i++) {
                    HashMap<String, Object> map = new HashMap<String, Object>();
                    uri = photoUri.get(i);
                    map.put("group_member_photo", Uri.parse(uri.toString()));
                    memList.add(map);
                }
            }
            while (memList.size() < 4) {
                HashMap<String, Object> map = new HashMap<String, Object>();
                map.put("group_member_photo",
                        R.drawable.ic_contact_picture_holo_light);
                memList.add(map);
            }
            GroupPhotoAdapter adapter = new GroupPhotoAdapter(mContext, memList,
                    R.layout.group_photo_grid_layout,
                    new String[] {
                            "group_member_photo"
                    },
                    new int[] {
                            R.id.group_member_photo
                    });
            viewCache.groupMember.setAdapter(adapter);
        }
    }

    /**
     * @}
     */
}
