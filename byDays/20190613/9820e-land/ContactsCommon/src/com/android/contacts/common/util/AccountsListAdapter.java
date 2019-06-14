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

package com.android.contacts.common.util;

import android.content.Context;
import android.graphics.Color;
import android.telephony.TelephonyManager;
import android.text.TextUtils.TruncateAt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.contacts.common.R;
import com.android.contacts.common.model.AccountTypeManager;
import com.android.contacts.common.model.account.AccountType;
import com.android.contacts.common.model.account.AccountWithDataSet;
import java.util.ArrayList;
import java.util.List;
/**
* SPRD:
* 
* @{
*/
import android.sim.Sim;
import android.sim.SimManager;
import com.sprd.contacts.common.model.account.PhoneAccountType;
import com.sprd.contacts.common.model.account.SimAccountType;
import com.sprd.contacts.common.model.account.USimAccountType;
import com.sprd.contacts.common.util.UniverseUtils;
/**
* @}
*/

/**
 * List-Adapter for Account selection
 */
public final class AccountsListAdapter extends BaseAdapter {
    private final LayoutInflater mInflater;
    private final List<AccountWithDataSet> mAccounts;
    private final AccountTypeManager mAccountTypes;
    private final Context mContext;

    /**
     * Filters that affect the list of accounts that is displayed by this adapter.
     */
    public enum AccountListFilter {
        ALL_ACCOUNTS,                   // All read-only and writable accounts
        ACCOUNTS_CONTACT_WRITABLE,      // Only where the account type is contact writable
        ACCOUNTS_GROUP_WRITABLE         // Only accounts where the account type is group writable
    }

    public AccountsListAdapter(Context context, AccountListFilter accountListFilter) {
        this(context, accountListFilter, null);
    }

    /**
     * @param currentAccount the Account currently selected by the user, which should come
     * first in the list. Can be null.
     */
    public AccountsListAdapter(Context context, AccountListFilter accountListFilter,
            AccountWithDataSet currentAccount) {
        mContext = context;
        mAccountTypes = AccountTypeManager.getInstance(context);
        mAccounts = getAccounts(accountListFilter);
        if (currentAccount != null
                && !mAccounts.isEmpty()
                && !mAccounts.get(0).equals(currentAccount)
                && mAccounts.remove(currentAccount)) {
            mAccounts.add(0, currentAccount);
        }
        
        /**
        * SPRD:
        * 
        * @{
        */
        if (currentAccount != null) {
            mAccounts.remove(currentAccount);
        }

        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            initSim(context);
        }
        /**
        * @}
        */
        
        mInflater = LayoutInflater.from(context);
    }

    private List<AccountWithDataSet> getAccounts(AccountListFilter accountListFilter) {
        if (accountListFilter == AccountListFilter.ACCOUNTS_GROUP_WRITABLE) {
            return new ArrayList<AccountWithDataSet>(mAccountTypes.getGroupWritableAccounts());
        }
        return new ArrayList<AccountWithDataSet>(mAccountTypes.getAccounts(
                accountListFilter == AccountListFilter.ACCOUNTS_CONTACT_WRITABLE));
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View resultView = convertView != null ? convertView
                : mInflater.inflate(R.layout.account_selector_list_item, parent, false);

        final TextView text1 = (TextView) resultView.findViewById(android.R.id.text1);
        final TextView text2 = (TextView) resultView.findViewById(android.R.id.text2);
        final ImageView icon = (ImageView) resultView.findViewById(android.R.id.icon);

        final AccountWithDataSet account = mAccounts.get(position);
        final AccountType accountType = mAccountTypes.getAccountType(account.type, account.dataSet);

        //text1.setText(accountType.getDisplayLabel(mContext));
        // For email addresses, we don't want to truncate at end, which might cut off the domain
        // name.
        
        /**
        * SPRD:
        * Modify these sentences for UniverseUI.
        * Original code:
        * The code be remarked.
        * @{
        */
        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            if (SimAccountType.ACCOUNT_TYPE.equals(account.type)
                    || USimAccountType.ACCOUNT_TYPE.equals(account.type)) {
                if (TelephonyManager.getPhoneCount() == 1 && mSims != null && mSims.length > 0) {
                    text1.setText(accountType.getDisplayLabel(mContext));
                    text2.setText(mSims[0].getName());
                    icon.setImageDrawable(mAccountTypes.getAccountIcon(account, false));
                } else {
                    if (mSims == null || (mSims != null && mSims.length == 0)) {
                        text2.setText(account.name);
                        icon.setImageDrawable(accountType.getDisplayIcon(mContext));
                    } else {
                        text1.setText(account.name);
                        for (Sim sim : mSims) {
                            String simSlotName = mAccountTypes.getSimSlotName(sim.getPhoneId());
                            //text2.setTextColor(mSimManager.getColor(sim.getPhoneId()));
                            if (simSlotName == null) {
                                break;
                            } else {
                                if (mAccountTypes.isExistInSimSlotName(simSlotName)
                                        && simSlotName.equals(account.name)) {
                                    text2.setText(sim.getName());
                                    break;
                                } else {
                                    text2.setText(account.name);
                                }
                            }
                        }
                        icon.setImageDrawable(mAccountTypes.getAccountIcon(account, false));
                    }
                }
            } else if (PhoneAccountType.ACCOUNT_TYPE.equals(account.type)) {
                text1.setText(accountType.getDisplayLabel(mContext));
                text2.setText(mContext.getString(R.string.label_phone));
                //icon.setImageDrawable(accountType.getDisplayIcon(mContext));
                /**
                 * SPRD bug 720740 The SIM card account avatar is consistent with the setting @{
                 *
                 * */
                icon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.profile));
            } else {
                text2.setText(account.name);
                icon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.profile));
                /**
                 * @}
                 **/
            }
        } else {
            if (PhoneAccountType.ACCOUNT_TYPE.equals(account.type)) {
                text1.setText(accountType.getDisplayLabel(mContext));
                text2.setText(mContext.getString(R.string.label_phone));
            } else {
                text2.setText(account.name);
            }
            icon.setImageDrawable(accountType.getDisplayIcon(mContext));
        }
        text2.setEllipsize(TruncateAt.MIDDLE);
        
//        text2.setText(account.name);
//        text2.setEllipsize(TruncateAt.MIDDLE);
//
//        icon.setImageDrawable(accountType.getDisplayIcon(mContext));
        /**
        * @}
        */

        return resultView;
    }

    @Override
    public int getCount() {
        return mAccounts.size();
    }

    @Override
    public AccountWithDataSet getItem(int position) {
        return mAccounts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }
    
    /**
    * SPRD:
    * 
    * @{
    */
    private Sim mSims[] = null;
    private SimManager mSimManager;
    
    private void initSim(Context context) {
        mSimManager = SimManager.get(context);
        if (mSimManager == null) {
            return;
        }
        mSims = mSimManager.getSims();
    }
    
    public AccountsListAdapter(Context context, List<AccountWithDataSet> accounts) {
        mContext = context;
        mAccountTypes = AccountTypeManager.getInstance(context);
        mAccounts = accounts;

        if (UniverseUtils.UNIVERSEUI_SUPPORT) {
            initSim(context);
        }
        mInflater = LayoutInflater.from(context);
    }
    /**
     * @}
     */
}

