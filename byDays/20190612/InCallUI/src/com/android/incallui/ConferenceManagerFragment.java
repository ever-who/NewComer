/*
 * Copyright (C) 2013 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.incallui;

import java.util.List;

import com.sprd.incallui.SprdUtils;

import android.app.ActionBar;
import android.app.ActionBar.OnMenuVisibilityListener;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.contacts.common.ContactPhotoManager;
import com.android.contacts.common.ContactPhotoManager.DefaultImageRequest;
import com.android.services.telephony.common.Call;
import com.android.services.telephony.common.Call.State;

/**
 * Fragment for call control buttons
 */
public class ConferenceManagerFragment
        extends BaseFragment<ConferenceManagerPresenter,
                ConferenceManagerPresenter.ConferenceManagerUi>
        implements ConferenceManagerPresenter.ConferenceManagerUi {

    private View mButtonManageConferenceDone;
    private ViewGroup[] mConferenceCallList;
    private Chronometer mConferenceTime;
    private ContactPhotoManager mContactPhotoManager;
    public  static final int  SEPARATE_CALL = 0;
    public  static final int  HANGUP_CALL = 1;
    private LayoutInflater mInflater;
    private ConferenceParticipantListAdapter mConferenceParticipantListAdapter;
    private ListView mConferenceParticipantList;
    public int mCallId;
    private TextView mLeftSkView;
    private TextView mCenterSkView;
    private TextView mRightSkView;

    @Override
    ConferenceManagerPresenter createPresenter() {
        // having a singleton instance.
        return new ConferenceManagerPresenter();
    }

    @Override
    ConferenceManagerPresenter.ConferenceManagerUi getUi() {
        return this;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View parent;
        if (SprdUtils.PIKEL_UI_SUPPORT) {
             parent = inflater.inflate(R.layout.conference_manager_fragment_sprd_pikel, container, false);

            mConferenceParticipantList = (ListView) parent.findViewById(R.id.participantList);
            mContactPhotoManager =
                    ContactPhotoManager.getInstance(getActivity().getApplicationContext());
            mInflater = LayoutInflater.from(getActivity().getApplicationContext());
        } else if (SprdUtils.UNIVERSE_UI_SUPPORT) {
            parent = inflater.inflate(R.layout.conference_manager_stub_sprd, container, false);
            mConferenceStub = (ViewStub) parent.findViewById(R.id.conferenceStub);
        } else {
            parent = inflater.inflate(R.layout.conference_manager_fragment, container,
                    false);

            // set up the Conference Call chronometer
            mConferenceTime = (Chronometer) parent.findViewById(R.id.manageConferencePanelHeader);
            mConferenceTime.setFormat(getActivity().getString(R.string.caller_manage_header));

            // Create list of conference call widgets
            mConferenceCallList = new ViewGroup[getPresenter().getMaxCallersInConference()];

            final int[] viewGroupIdList = { R.id.caller0, R.id.caller1, R.id.caller2,
                    R.id.caller3, R.id.caller4 };
            for (int i = 0; i < getPresenter().getMaxCallersInConference(); i++) {
                mConferenceCallList[i] =
                        (ViewGroup) parent.findViewById(viewGroupIdList[i]);
            }

            mButtonManageConferenceDone = parent.findViewById(R.id.manage_done);
            mButtonManageConferenceDone.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getPresenter().manageConferenceDoneClicked();
                }
            });

        }
        return parent;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void setVisible(boolean on) {
        if (SprdUtils.PIKEL_UI_SUPPORT) {
            ActionBar actionBar = getActivity().getActionBar();
            if (on) {
                actionBar.setTitle(R.string.manageConferenceLabel);
                actionBar.setDisplayUseLogoEnabled(false);
                setHasOptionsMenu(true);
                actionBar.show();
                actionBar.setHomeButtonEnabled(false);
                actionBar.addOnMenuVisibilityListener(new OnMenuListener());
                final CallList calls = CallList.getInstance();
                getPresenter().init(getActivity(), calls);
                mConferenceParticipantList.setOnItemSelectedListener(new onConferenceItemSelectedListener());
                mConferenceParticipantList.requestFocus();
                getView().setVisibility(View.VISIBLE);
            } else {
                actionBar.hide();
                getView().setVisibility(View.GONE);
            }
        } else if (SprdUtils.UNIVERSE_UI_SUPPORT) {
            if (on) {
                final CallList calls = CallList.getInstance();
                if(mConferenceStub != null && mConferenceView == null){
                    initConferenceViews();
                } else if(mConferenceView != null){
                    mConferenceView.setVisibility(View.VISIBLE);
                }
                getPresenter().init(getActivity(), calls);
                getView().setVisibility(View.VISIBLE);

            } else {
                if(mConferenceView != null){
                    mConferenceView.setVisibility(View.GONE);
                }
                getView().setVisibility(View.GONE);
            }
        } else {
            if (on) {
                final CallList calls = CallList.getInstance();
                getPresenter().init(getActivity(), calls);
                getView().setVisibility(View.VISIBLE);

            } else {
                getView().setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean isFragmentVisible() {
        return isVisible();
    }

    @Override
    public void setRowVisible(int rowId, boolean on) {
        if (on) {
            mConferenceCallList[rowId].setVisibility(View.VISIBLE);
        } else {
            mConferenceCallList[rowId].setVisibility(View.GONE);
        }
    }

    /**
     * Helper function to fill out the Conference Call(er) information
     * for each item in the "Manage Conference Call" list.
     */
    @Override
    public final void displayCallerInfoForConferenceRow(int rowId, String callerName,
            String callerNumber, String callerNumberType) {

        final TextView nameTextView = (TextView) mConferenceCallList[rowId].findViewById(
                R.id.conferenceCallerName);
        final TextView numberTextView = (TextView) mConferenceCallList[rowId].findViewById(
                R.id.conferenceCallerNumber);
        final TextView numberTypeTextView = (TextView) mConferenceCallList[rowId].findViewById(
                R.id.conferenceCallerNumberType);

        // set the caller name
        nameTextView.setText(callerName);

        // set the caller number in subscript, or make the field disappear.
        if (TextUtils.isEmpty(callerNumber)) {
            numberTextView.setVisibility(View.GONE);
            numberTypeTextView.setVisibility(View.GONE);
        } else {
            numberTextView.setVisibility(View.VISIBLE);
            numberTextView.setText(callerNumber);
            numberTypeTextView.setVisibility(View.VISIBLE);
            numberTypeTextView.setText(callerNumberType);
        }
    }

    @Override
    public void update(Context context, List<Call> participants, boolean parentCanSeparate) {
        if (mConferenceParticipantListAdapter == null) {
            mConferenceParticipantListAdapter = new ConferenceParticipantListAdapter(
                    mConferenceParticipantList, context, mInflater, mContactPhotoManager);

            mConferenceParticipantList.setAdapter(mConferenceParticipantListAdapter);
        }
        mConferenceParticipantListAdapter.updateParticipants(participants, parentCanSeparate);
        /* SPRD: add for Bug 643710 */
        if (!participants.isEmpty() && mConferenceParticipantList != null
                && mConferenceParticipantListAdapter.getCount() > 1
                && mConferenceParticipantList.getSelectedItemPosition() != -1
                && mConferenceParticipantList.getSelectedItemPosition() < participants.size()) {
            boolean allCallExist = true;
            for (Call call : participants) {
                if (null == call) {
                    allCallExist = false;
                }
            }
            if (allCallExist) {
                mCallId = participants.get(mConferenceParticipantList.getSelectedItemPosition()).getCallId();
            }
        }
        /* end */
    }

    @Override
    public final void setupEndButtonForRow(final int rowId) {
            View endButton = mConferenceCallList[rowId].findViewById(R.id.conferenceCallerDisconnect);
            endButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getPresenter().endConferenceConnection(rowId);
                }
            });
    }

    @Override
    public final void setCanSeparateButtonForRow(final int rowId, boolean canSeparate) {
        final View separateButton = mConferenceCallList[rowId].findViewById(
                R.id.conferenceCallerSeparate);

        if (canSeparate) {
            final View.OnClickListener separateThisConnection = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getPresenter().separateConferenceConnection(rowId);
                    }
                };
            separateButton.setOnClickListener(separateThisConnection);
            separateButton.setVisibility(View.VISIBLE);
        } else {
            separateButton.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Starts the "conference time" chronometer.
     */
    @Override
    public void startConferenceTime(long base) {
        if (mConferenceTime != null) {
            mConferenceTime.setBase(base);
            mConferenceTime.start();
        }
    }

    /**
     * Stops the "conference time" chronometer.
     */
    @Override
    public void stopConferenceTime() {
        if (mConferenceTime != null) {
            mConferenceTime.stop();
        }
    }

    /* SPRD: Add for Universe UI @{ */
    private ViewStub mConferenceStub;
    private View mConferenceView;

    private void initConferenceViews(){
        View parent = mConferenceStub.inflate();
        mConferenceView = parent.findViewById(R.id.manageConferencePanel);
        // set up the Conference Call chronometer
        mConferenceTime = (Chronometer) parent.findViewById(R.id.manageConferencePanelHeader);
        mConferenceTime.setFormat(getActivity().getString(R.string.caller_manage_header));

        // Create list of conference call widgets
        mConferenceCallList = new ViewGroup[getPresenter().getMaxCallersInConference()];

        final int[] viewGroupIdList = { R.id.caller0, R.id.caller1, R.id.caller2,
                R.id.caller3, R.id.caller4 };
        for (int i = 0; i < getPresenter().getMaxCallersInConference(); i++) {
            mConferenceCallList[i] =
                    (ViewGroup) parent.findViewById(viewGroupIdList[i]);
        }

        mButtonManageConferenceDone = parent.findViewById(R.id.manage_done);
        mButtonManageConferenceDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPresenter().manageConferenceDoneClicked();
            }
        });
        mConferenceView.setVisibility(View.VISIBLE);
    }
    /* @}
     */

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.conference_manager_menu,menu);

        if (getActivity() != null && getActivity() instanceof InCallActivity) {
            mLeftSkView = ((InCallActivity) getActivity()).mLeftSkView;
            mCenterSkView = ((InCallActivity) getActivity()).mCenterSkView;
            mRightSkView = ((InCallActivity) getActivity()).mRightSkView;
        }

    }

    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (mConferenceParticipantListAdapter != null) {
            menu.findItem(R.id.conference_caller_separate).setVisible(mConferenceParticipantListAdapter.isThisRowCanSeparate()? true : false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.conference_caller_separate:
            updateCenterSk(SEPARATE_CALL);
            return true;
        case R.id.conference_caller_disconnect:
            updateCenterSk(HANGUP_CALL);
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private class OnMenuListener implements  OnMenuVisibilityListener {

        @Override
        public void onMenuVisibilityChanged(boolean isVisible) {
            Log.d(this,"onMenuVisibilityChanged isVisible: " +isVisible);
            if (isVisible && mLeftSkView != null && mCenterSkView != null && mRightSkView != null) {
                mLeftSkView.setText("");
                mCenterSkView.setText(getResources().getString(R.string.onscreenChooseText));
            } else {
                mLeftSkView.setText(getResources().getString(R.string.onscreenOptionText));
                mCenterSkView.setText("");
            }
        }
    }

    public void updateCenterSk(int commandID) {
        if (getActivity() != null && getActivity() instanceof InCallActivity) {
            //String centerSkText = mCenterSkView.getText().toString();
            //Log.d(this, "updateCenterSk centerSkText : " + centerSkText +",mCallId="+mCallId + ",commandID:" + commandID);
            if (commandID == SEPARATE_CALL) {
                toSeparateCall();
            } else if (commandID == HANGUP_CALL) {
                toHandUpCall();
            }/* else if(commandID == HANDLE_CALL_FROM_CENTER_PAD){
                if (centerSkText.equals(getBottonButtonText(R.string.bottom_button_hangup))) {
                    toHandUpCall();
                }else if(centerSkText.equals(getBottonButtonText(R.string.bottom_button_conference_separate))){
                    toSeparateCall();
                }
            } */else {
                Log.d(this, "commandID is not expected: " + commandID);
            }
        }
    }

    private void toHandUpCall(){
        getPresenter().endConferenceConnectionPikel(mCallId);
    }

    private void toSeparateCall(){
        if (mConferenceParticipantListAdapter != null && mConferenceParticipantListAdapter.isThisRowCanSeparate()) {
            getPresenter().separateConferenceConnectionPikel(mCallId);
        }
    }

    private class onConferenceItemSelectedListener implements OnItemSelectedListener{

        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                int position, long id) {
            mCallId= Integer.parseInt ((String)view.getTag());
            Log.d(this,"onConferenceItemSelectedListener :" + mCallId);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            Log.d(this, "onNothingSelected");
        }
    }
}