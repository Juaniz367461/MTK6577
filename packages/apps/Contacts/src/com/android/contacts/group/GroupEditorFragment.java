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
 * limitations under the License
 */

package com.android.contacts.group;

import com.android.contacts.ContactPhotoManager;
import com.android.contacts.ContactSaveService;
import com.android.contacts.GroupMemberLoader;
import com.android.contacts.GroupMemberLoader.GroupEditorQuery;
import com.android.contacts.GroupMetaDataLoader;
import com.android.contacts.R;
import com.android.contacts.activities.GroupEditorActivity;
import com.android.contacts.editor.ContactEditorFragment.SaveMode;
import com.android.contacts.editor.SelectAccountDialogFragment;
import com.android.contacts.group.SuggestedMemberListAdapter.SuggestedMember;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.AccountWithDataSet;
import com.android.contacts.util.AccountsListAdapter.AccountListFilter;
import com.android.internal.telephony.gemini.GeminiPhone;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.util.Objects;

import android.accounts.Account;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.DialogInterface.OnCancelListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.Intents;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;
import com.android.contacts.activities.PeopleActivity.AccountCategoryInfo;
import com.mediatek.contacts.ContactsFeatureConstants.FeatureOption;
import com.mediatek.contacts.model.AccountWithDataSetEx;
import com.mediatek.contacts.simcontact.AbstractStartSIMService;
import com.mediatek.contacts.simcontact.SIMInfoWrapper;
import com.mediatek.contacts.util.ContactsGroupUtils;
import com.mediatek.contacts.util.OperatorUtils;

import java.util.ArrayList;
import java.util.List;
import android.provider.Telephony.SIMInfo;

public class GroupEditorFragment extends Fragment implements SelectAccountDialogFragment.Listener {
    private static final String TAG = "GroupEditorFragment";

    private static final String LEGACY_CONTACTS_AUTHORITY = "contacts";

    private static final String KEY_ACTION = "action";
    private static final String KEY_GROUP_URI = "groupUri";
    private static final String KEY_GROUP_ID = "groupId";
    private static final String KEY_STATUS = "status";
    private static final String KEY_ACCOUNT_NAME = "accountName";
    private static final String KEY_ACCOUNT_TYPE = "accountType";
    private static final String KEY_DATA_SET = "dataSet";
    private static final String KEY_GROUP_NAME_IS_READ_ONLY = "groupNameIsReadOnly";
    private static final String KEY_ORIGINAL_GROUP_NAME = "originalGroupName";
    private static final String KEY_MEMBERS_TO_ADD = "membersToAdd";
    private static final String KEY_MEMBERS_TO_REMOVE = "membersToRemove";
    private static final String KEY_MEMBERS_TO_DISPLAY = "membersToDisplay";
    
    private static final String KEY_GROUP_SLOT_ID = "groupSlotId"; 
    private static final String KEY_CATEGORY =  "mCategory";
    
    private static final String CURRENT_EDITOR_TAG = "currentEditorForAccount";

    public static interface Listener {
        /**
         * Group metadata was not found, close the fragment now.
         */
        public void onGroupNotFound();

        /**
         * User has tapped Revert, close the fragment now.
         */
        void onReverted();

        /**
         * Contact was saved and the Fragment can now be closed safely.
         */
        void onSaveFinished(int resultCode, Intent resultIntent);

        /**
         * Fragment is created but there's no accounts set up.
         */
        void onAccountsNotFound();
    }

    private static final int LOADER_GROUP_METADATA = 1;
    private static final int LOADER_EXISTING_MEMBERS = 2;
    private static final int LOADER_NEW_GROUP_MEMBER = 3;

    public static final String SAVE_MODE_EXTRA_KEY = "saveMode";
    public static final String SHOW_TOAST_EXTRA_KEY = "showToast";

    private static final String MEMBER_RAW_CONTACT_ID_KEY = "rawContactId";
    private static final String MEMBER_LOOKUP_URI_KEY = "memberLookupUri";

    protected static final String[] PROJECTION_CONTACT = new String[] {
        Contacts._ID,                           // 0
        Contacts.DISPLAY_NAME_PRIMARY,          // 1
        Contacts.DISPLAY_NAME_ALTERNATIVE,      // 2
        Contacts.SORT_KEY_PRIMARY,              // 3
        Contacts.STARRED,                       // 4
        Contacts.CONTACT_PRESENCE,              // 5
        Contacts.CONTACT_CHAT_CAPABILITY,       // 6
        Contacts.PHOTO_ID,                      // 7
        Contacts.PHOTO_THUMBNAIL_URI,           // 8
        Contacts.LOOKUP_KEY,                    // 9
        Contacts.PHONETIC_NAME,                 // 10
        Contacts.HAS_PHONE_NUMBER,              // 11
        Contacts.IS_USER_PROFILE,               // 12
        
        // the following lines are provided and maintained by Mediatek Inc.
        Contacts.INDICATE_PHONE_SIM,            // 13 
        Contacts.INDEX_IN_SIM,		            // 14
        Contacts.IS_SDN_CONTACT,                // 15
   	    // the previous lines are provided and maintained by Mediatek Inc.
    };

    protected static final int CONTACT_ID_COLUMN_INDEX = 0;
    protected static final int CONTACT_DISPLAY_NAME_PRIMARY_COLUMN_INDEX = 1;
    protected static final int CONTACT_DISPLAY_NAME_ALTERNATIVE_COLUMN_INDEX = 2;
    protected static final int CONTACT_SORT_KEY_PRIMARY_COLUMN_INDEX = 3;
    protected static final int CONTACT_STARRED_COLUMN_INDEX = 4;
    protected static final int CONTACT_PRESENCE_STATUS_COLUMN_INDEX = 5;
    protected static final int CONTACT_CHAT_CAPABILITY_COLUMN_INDEX = 6;
    protected static final int CONTACT_PHOTO_ID_COLUMN_INDEX = 7;
    protected static final int CONTACT_PHOTO_URI_COLUMN_INDEX = 8;
    protected static final int CONTACT_LOOKUP_KEY_COLUMN_INDEX = 9;
    protected static final int CONTACT_PHONETIC_NAME_COLUMN_INDEX = 10;
    protected static final int CONTACT_HAS_PHONE_COLUMN_INDEX = 11;
    protected static final int CONTACT_IS_USER_PROFILE = 12;

    // the following lines are provided and maintained by Mediatek Inc.
    protected static final int CONTACT_INDICATE_PHONE_SIM_COLUMN_INDEX = 13;
    protected static final int CONTACT_INDEX_IN_SIM_COLUMN_INDEX = 14;
    protected static final int CONTACT_IS_SDN_CONTACT_COLUMN_INDEX = 15;
	// the previous lines are provided and maintained by Mediatek Inc.

    /**
     * Modes that specify the status of the editor
     */
    public enum Status {
        SELECTING_ACCOUNT, // Account select dialog is showing
        LOADING,    // Loader is fetching the group metadata
        EDITING,    // Not currently busy. We are waiting forthe user to enter data.
        SAVING,     // Data is currently being saved
        CLOSING     // Prevents any more saves
    }

    private Context mContext;
    private String mAction;
    private Bundle mIntentExtras;
    private Uri mGroupUri;
    private long mGroupId;
    private Listener mListener;

    private Status mStatus;

    private ViewGroup mRootView;
    private ListView mListView;
    private LayoutInflater mLayoutInflater;

    private TextView mGroupNameView;
    private AutoCompleteTextView mAutoCompleteTextView;

    private String mAccountName;
    private String mAccountType;
    private String mDataSet;

    private boolean mGroupNameIsReadOnly;
    private String mOriginalGroupName = "";
    private int mLastGroupEditorId;

    private MemberListAdapter mMemberListAdapter;
    private ContactPhotoManager mPhotoManager;

    private ContentResolver mContentResolver;
    private SuggestedMemberListAdapter mAutoCompleteAdapter;

    private ArrayList<Member> mListMembersToAdd = new ArrayList<Member>();
    private ArrayList<Member> mListMembersToRemove = new ArrayList<Member>();
    private ArrayList<Member> mListToDisplay = new ArrayList<Member>();

    public GroupEditorFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        setHasOptionsMenu(true);
        mLayoutInflater = inflater;
        mRootView = (ViewGroup) inflater.inflate(R.layout.group_editor_fragment, container, false);
        return mRootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mContext = activity;
        mPhotoManager = ContactPhotoManager.getInstance(mContext);
        mMemberListAdapter = new MemberListAdapter();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            // Just restore from the saved state.  No loading.
            onRestoreInstanceState(savedInstanceState);
            if (mStatus == Status.SELECTING_ACCOUNT) {
                // Account select dialog is showing.  Don't setup the editor yet.
            } else if (mStatus == Status.LOADING) {
                startGroupMetaDataLoader();
            } else {
                setupEditorForAccount();
            }
        } else if (Intent.ACTION_EDIT.equals(mAction)) {
            startGroupMetaDataLoader();
        } else if (Intent.ACTION_INSERT.equals(mAction)) {
            final Account account = mIntentExtras == null ? null :
                    (Account) mIntentExtras.getParcelable(Intents.Insert.ACCOUNT);
            final String dataSet = mIntentExtras == null ? null :
                    mIntentExtras.getString(Intents.Insert.DATA_SET);

            if (account != null) {
                // Account specified in Intent - no data set can be specified in this manner.
                mAccountName = account.name;
                mAccountType = account.type;
                mDataSet = dataSet;
                setupEditorForAccount();
            } else {
                // No Account specified. Let the user choose from a disambiguation dialog.
                selectAccountAndCreateGroup();
            }
        } else {
            throw new IllegalArgumentException("Unknown Action String " + mAction +
                    ". Only support " + Intent.ACTION_EDIT + " or " + Intent.ACTION_INSERT);
        }
        
        if (mReceiver == null) {
            mReceiver = new SimReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            filter.addAction(TelephonyIntents.ACTION_PHB_STATE_CHANGED);
            filter.addAction(GeminiPhone.EVENT_PRE_3G_SWITCH);
            mContext.registerReceiver(mReceiver,filter);
            Log.i(TAG, "registerReceiver mReceiver");
        }
    }

    private void startGroupMetaDataLoader() {
        mStatus = Status.LOADING;
        getLoaderManager().initLoader(LOADER_GROUP_METADATA, null,
                mGroupMetaDataLoaderListener);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_ACTION, mAction);
        outState.putParcelable(KEY_GROUP_URI, mGroupUri);
        outState.putLong(KEY_GROUP_ID, mGroupId);

        outState.putSerializable(KEY_STATUS, mStatus);
        outState.putString(KEY_ACCOUNT_NAME, mAccountName);
        outState.putString(KEY_ACCOUNT_TYPE, mAccountType);
        outState.putString(KEY_DATA_SET, mDataSet);

        outState.putBoolean(KEY_GROUP_NAME_IS_READ_ONLY, mGroupNameIsReadOnly);
        outState.putString(KEY_ORIGINAL_GROUP_NAME, mOriginalGroupName);

        outState.putParcelableArrayList(KEY_MEMBERS_TO_ADD, mListMembersToAdd);
        outState.putParcelableArrayList(KEY_MEMBERS_TO_REMOVE, mListMembersToRemove);
        outState.putParcelableArrayList(KEY_MEMBERS_TO_DISPLAY, mListToDisplay);
        
        outState.putInt(KEY_GROUP_SLOT_ID, mSlotId);
        outState.putString(KEY_CATEGORY, mCategory);
    }

    private void onRestoreInstanceState(Bundle state) {
        mAction = state.getString(KEY_ACTION);
        mGroupUri = state.getParcelable(KEY_GROUP_URI);
        mGroupId = state.getLong(KEY_GROUP_ID);

        mStatus = (Status) state.getSerializable(KEY_STATUS);
        mAccountName = state.getString(KEY_ACCOUNT_NAME);
        mAccountType = state.getString(KEY_ACCOUNT_TYPE);
        mDataSet = state.getString(KEY_DATA_SET);

        mGroupNameIsReadOnly = state.getBoolean(KEY_GROUP_NAME_IS_READ_ONLY);
        mOriginalGroupName = state.getString(KEY_ORIGINAL_GROUP_NAME);

        mListMembersToAdd = state.getParcelableArrayList(KEY_MEMBERS_TO_ADD);
        mListMembersToRemove = state.getParcelableArrayList(KEY_MEMBERS_TO_REMOVE);
        mListToDisplay = state.getParcelableArrayList(KEY_MEMBERS_TO_DISPLAY);
        
        mSlotId = state.getInt(KEY_GROUP_SLOT_ID);
        mCategory = state.getString(KEY_CATEGORY);
    }

    public void setContentResolver(ContentResolver resolver) {
        mContentResolver = resolver;
        if (mAutoCompleteAdapter != null) {
            mAutoCompleteAdapter.setContentResolver(mContentResolver);
        }
    }

    private void selectAccountAndCreateGroup() {
        final List<AccountWithDataSet> accounts =
                AccountTypeManager.getInstance(mContext).getAccounts(true /* writeable */);
        // No Accounts available
        if (accounts.isEmpty()) {
            Log.e(TAG, "No accounts were found.");
            if (mListener != null) {
                mListener.onAccountsNotFound();
            }
            return;
        }

        // In the common case of a single account being writable, auto-select
        // it without showing a dialog.
        if (accounts.size() == 1) {
            mAccountName = accounts.get(0).name;
            mAccountType = accounts.get(0).type;
            mDataSet = accounts.get(0).dataSet;
            setupEditorForAccount();
            return;  // Don't show a dialog.
        }

        mStatus = Status.SELECTING_ACCOUNT;
        SelectAccountDialogFragment.show(getFragmentManager(), this,
                R.string.dialog_new_group_account, AccountListFilter.ACCOUNTS_GROUP_WRITABLE,
                null);
    }

    @Override
    public void onAccountChosen(AccountWithDataSet account, Bundle extraArgs) {
        mAccountName = account.name;
        mAccountType = account.type;
        mDataSet = account.dataSet;
        if(account instanceof AccountWithDataSetEx){
            mSlotId =  ((AccountWithDataSetEx) account).getSlotId();
            //mSimId = SIMInfoWrapper.getDefault().getSimIdBySlotId(mSlotId);
            SIMInfo simInfro = SIMInfo.getSIMInfoBySlot(mContext, mSlotId);
            mSimId = (int)simInfro.mSimId;
            mCategory = account.type;
        }     
        Log.i(TAG,mAccountName+"-------------mAccountName[onAccountChosen]");
        Log.i(TAG,mAccountType+"-------------mAccountType[onAccountChosen]");
        Log.i(TAG,mSlotId+"-------------mSlotId[onAccountChosen]");
        Log.i(TAG,mSimId+"-------------mSimId[onAccountChosen]");
        Log.i(TAG,mCategory+"-------------mCategory[onAccountChosen]");
        setupEditorForAccount();
    }

    @Override
    public void onAccountSelectorCancelled() {
        if (mListener != null) {
            // Exit the fragment because we cannot continue without selecting an account
            mListener.onGroupNotFound();
        }
    }

    private AccountType getAccountType() {
        return AccountTypeManager.getInstance(mContext).getAccountType(mAccountType, mDataSet);
    }

    /**
     * @return true if the group membership is editable on this account type.  false otherwise,
     *         or account is not set yet.
     */
    private boolean isGroupMembershipEditable() {
        if (mAccountType == null) {
            return false;
        }
        return getAccountType().isGroupMembershipEditable();
    }

    /**
     * Sets up the editor based on the group's account name and type.
     */
    private void setupEditorForAccount() {
        final AccountType accountType = getAccountType();
        final boolean editable = isGroupMembershipEditable();
        boolean isNewEditor = false;
        mMemberListAdapter.setIsGroupMembershipEditable(editable);

        // Since this method can be called multiple time, remove old editor if the editor type
        // is different from the new one and mark the editor with a tag so it can be found for
        // removal if needed
        View editorView;
        int newGroupEditorId =
                editable ? R.layout.group_editor_view : R.layout.external_group_editor_view;
        if (newGroupEditorId != mLastGroupEditorId) {
            View oldEditorView = mRootView.findViewWithTag(CURRENT_EDITOR_TAG);
            if (oldEditorView != null) {
                mRootView.removeView(oldEditorView);
            }
            editorView = mLayoutInflater.inflate(newGroupEditorId, mRootView, false);
            editorView.setTag(CURRENT_EDITOR_TAG);
            mAutoCompleteAdapter = null;
            mLastGroupEditorId = newGroupEditorId;
            isNewEditor = true;
        } else {
            editorView = mRootView.findViewWithTag(CURRENT_EDITOR_TAG);
            if (editorView == null) {
                throw new IllegalStateException("Group editor view not found");
            }
        }

        mGroupNameView = (TextView) editorView.findViewById(R.id.group_name);
        mAutoCompleteTextView = (AutoCompleteTextView) editorView.findViewById(
                R.id.add_member_field);
        mAutoCompleteTextView.setThreshold(1);
        mListView = (ListView) editorView.findViewById(android.R.id.list);
        mListView.setAdapter(mMemberListAdapter);

        // Setup the account header, only when exists.
        if (editorView.findViewById(R.id.account_header) != null) {
            CharSequence accountTypeDisplayLabel = accountType.getDisplayLabel(mContext);
            ImageView accountIcon = (ImageView) editorView.findViewById(R.id.account_icon);
            TextView accountTypeTextView = (TextView) editorView.findViewById(R.id.account_type);
            TextView accountNameTextView = (TextView) editorView.findViewById(R.id.account_name);
            if (!TextUtils.isEmpty(mAccountName)) {
                String accountName = "";
                //UIM
//                if (AccountType.ACCOUNT_NAME_SIM.equals(mAccountName)
//                        || AccountType.ACCOUNT_NAME_USIM.equals(mAccountName)) {
                if (AccountType.ACCOUNT_NAME_SIM.equals(mAccountName)
                        || AccountType.ACCOUNT_NAME_USIM.equals(mAccountName)
                        || AccountType.ACCOUNT_NAME_UIM.equals(mAccountName)) {
                //UIM
                    accountName = SIMInfoWrapper.getDefault().getSimDisplayNameBySlotId(0);
                }
                //UIM
//                else if (AccountType.ACCOUNT_NAME_SIM2.equals(mAccountName)
//                        || AccountType.ACCOUNT_NAME_USIM2.equals(mAccountName)) {
                else if (AccountType.ACCOUNT_NAME_SIM2.equals(mAccountName)
                        || AccountType.ACCOUNT_NAME_USIM2.equals(mAccountName)
                        || AccountType.ACCOUNT_NAME_UIM2.equals(mAccountName)) {
                //UIM
                    accountName = SIMInfoWrapper.getDefault().getSimDisplayNameBySlotId(1);
                } else {
                    accountName = mAccountName;
                }
                accountNameTextView.setText(
                        mContext.getString(R.string.from_account_format, accountName));
            }
            accountTypeTextView.setText(accountTypeDisplayLabel);
            
        	/*
    		 * New feature by Mediatek Begin
    		 * Original Android code:
    		 * accountIcon.setImageDrawable(accountType.getDisplayIcon(mContext));
    		 * Descriptions:
    		 */

            Log.i(TAG,"[groupeditorfragment] setupEditorForAccount-----");           	
			if (mSlotId >= 0) {
				Log.i(TAG, "GroupEditorFragment mSlotId : " + mSlotId);
				accountIcon.setImageDrawable(accountType.getDisplayIconBySlotId(mContext, mSlotId));
			} else {
				accountIcon.setImageDrawable(accountType.getDisplayIcon(mContext));// show the USim icon
			}

        	/*
    		 * New feature by Mediatek End
    		 */

        }

        // Setup the autocomplete adapter (for contacts to suggest to add to the group) based on the
        // account name and type. For groups that cannot have membership edited, there will be no
        // autocomplete text view.
        if (mAutoCompleteTextView != null) {
            mAutoCompleteAdapter = new SuggestedMemberListAdapter(mContext,
                    android.R.layout.simple_dropdown_item_1line);
            mAutoCompleteAdapter.setContentResolver(mContentResolver);
            mAutoCompleteAdapter.setAccountType(mAccountType);
            mAutoCompleteAdapter.setAccountName(mAccountName);
            mAutoCompleteAdapter.setDataSet(mDataSet);
            mAutoCompleteTextView.setAdapter(mAutoCompleteAdapter);
            mAutoCompleteTextView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    SuggestedMember member = (SuggestedMember) view.getTag();
                    if (member == null) {
                        return; // just in case
                    }
                    loadMemberToAddToGroup(member.getRawContactId(),
                            String.valueOf(member.getContactId()));

                    // Update the autocomplete adapter so the contact doesn't get suggested again
                    mAutoCompleteAdapter.addNewMember(member.getContactId());

                    // Clear out the text field
                    mAutoCompleteTextView.setText("");
                }
            });
            // Update the exempt list.  (mListToDisplay might have been restored from the saved
            // state.)
            mAutoCompleteAdapter.updateExistingMembersList(mListToDisplay);
        }

        // If the group name is ready only, don't let the user focus on the field.
        mGroupNameView.setFocusable(!mGroupNameIsReadOnly);
        if(isNewEditor) {
            mRootView.addView(editorView);
        }
        mStatus = Status.EDITING;
    }

    public void load(String action, Uri groupUri, Bundle intentExtras) {
        mAction = action;
        mGroupUri = groupUri;
        mGroupId = (groupUri != null) ? ContentUris.parseId(mGroupUri) : 0;
        mIntentExtras = intentExtras;
    }

    private void bindGroupMetaData(Cursor cursor) {
        if (!cursor.moveToFirst()) {
            Log.i(TAG, "Group not found with URI: " + mGroupUri + " Closing activity now.");
            if (mListener != null) {
                mListener.onGroupNotFound();
            }
            return;
        }
        mOriginalGroupName = cursor.getString(GroupMetaDataLoader.TITLE);
        mAccountName = cursor.getString(GroupMetaDataLoader.ACCOUNT_NAME);
        mAccountType = cursor.getString(GroupMetaDataLoader.ACCOUNT_TYPE);
        mDataSet = cursor.getString(GroupMetaDataLoader.DATA_SET);
        mGroupNameIsReadOnly = (cursor.getInt(GroupMetaDataLoader.IS_READ_ONLY) == 1);
        setupEditorForAccount();

        // Setup the group metadata display
        mGroupNameView.setText(mOriginalGroupName);
    }

    public void loadMemberToAddToGroup(long rawContactId, String contactId) {
        Bundle args = new Bundle();
        args.putLong(MEMBER_RAW_CONTACT_ID_KEY, rawContactId);
        args.putString(MEMBER_LOOKUP_URI_KEY, contactId);
        getLoaderManager().restartLoader(LOADER_NEW_GROUP_MEMBER, args, mContactLoaderListener);
    }

    public void setListener(Listener value) {
        mListener = value;
    }

    public void onDoneClicked() {
        if (isGroupMembershipEditable()) {
            /*
             * Change feature by Mediatek Begin
             * Original Android code:
             * save(SaveMode.CLOSE);
             * CR ID :ALPS00229457
             * Descriptions: 
             */
            save(SaveMode.CLOSE);
            /*
             * Change feature by Mediatek End
             */
        } else {
            // Just revert it.
            doRevertAction();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, final MenuInflater inflater) {
        inflater.inflate(R.menu.edit_group, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_discard:
                return revert();
        }
        return false;
    }

    private boolean revert() {
        if (!hasNameChange() && !hasMembershipChange()) {
            doRevertAction();
        } else {
            CancelEditDialogFragment.show(this);
        }
        return true;
    }

    private void doRevertAction() {
        // When this Fragment is closed we don't want it to auto-save
        mStatus = Status.CLOSING;
        if (mListener != null) mListener.onReverted();
    }

    public static class CancelEditDialogFragment extends DialogFragment {

        public static void show(GroupEditorFragment fragment) {
            CancelEditDialogFragment dialog = new CancelEditDialogFragment();
            dialog.setTargetFragment(fragment, 0);
            dialog.show(fragment.getFragmentManager(), "cancelEditor");
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog dialog = new AlertDialog.Builder(getActivity())
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(R.string.cancel_confirmation_dialog_title)
                    .setMessage(R.string.cancel_confirmation_dialog_message)
                    .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((GroupEditorFragment) getTargetFragment()).doRevertAction();
                            }
                        }
                    )
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();
            return dialog;
        }
    }

    public boolean save(int saveMode) {
        return save(saveMode, true);
    }
    
    /**
     * Saves or creates the group based on the mode, and if successful
     * finishes the activity. This actually only handles saving the group name.
     * @return true when successful
     */
    public boolean save(int saveMode, boolean showErrorTips) { 
        //Check background sim service state if it is sim group
        if (!checkSimServiceState(true)) {
            if (saveMode == SaveMode.CLOSE) {
                getLoaderManager().destroyLoader(LOADER_EXISTING_MEMBERS);
            }
            onSaveCompleted(false, SaveMode.CLOSE, null);
            return true;
        }
        
        if (!hasValidGroupName(showErrorTips) || mStatus != Status.EDITING) {
            return false;
        }
        
        // If we are about to close the editor - there is no need to refresh the data
        if (saveMode == SaveMode.CLOSE) {
            getLoaderManager().destroyLoader(LOADER_EXISTING_MEMBERS);
        }

        // If there are no changes, then go straight to onSaveCompleted()
        if (!hasNameChange() && !hasMembershipChange()) {
            onSaveCompleted(false, SaveMode.CLOSE, mGroupUri);
            return true;
        }

        mStatus = Status.SAVING;

        Activity activity = getActivity();
        // If the activity is not there anymore, then we can't continue with the save process.
        if (activity == null) {
            return false;
        }
        Intent saveIntent = null;
        if (Intent.ACTION_INSERT.equals(mAction)) {
        	/*
    		 * New feature by Mediatek Begin
    		 * Original Android code:
    		 * long[] membersToAddArray = convertToArray(mListMembersToAdd);
             * // Create the save intent to create the group and add members at the same time
             * saveIntent = ContactSaveService.createNewGroupIntent(activity,
             *      new AccountWithDataSet(mAccountName, mAccountType, mDataSet),
             *      mGroupNameView.getText().toString(),
             *      membersToAddArray, activity.getClass(),
             *      GroupEditorActivity.ACTION_SAVE_COMPLETED);
    		 */
            long[] membersToAddArray = convertToArray(mListMembersToAdd);
            int [] simIndexArray = convertSimIndexToArray(mListMembersToAdd);
            // Create the save intent to create the group and add members at the same time
            saveIntent = ContactSaveService.createNewGroupIntent(activity,
                    new AccountWithDataSet(mAccountName, mAccountType, mDataSet),
                    mGroupNameView.getText().toString(),
                    membersToAddArray, activity.getClass(),
                   GroupEditorActivity.ACTION_SAVE_COMPLETED, simIndexArray, mSlotId);
            int m =0;
            for(long i :membersToAddArray){
                Log.i(TAG, "[insert] membersToAddArray"+membersToAddArray[m]+"------");
                m++;
            }
            m =0;
            for(int i :simIndexArray){
                Log.i(TAG, "[insert] simIndexArray"+simIndexArray[m]+"------");
                m++;
            }
        	/*
    		 * New feature by Mediatek End
    		 */

        } else if (Intent.ACTION_EDIT.equals(mAction)) {
            // Create array of raw contact IDs for contacts to add to the group
            long[] membersToAddArray = convertToArray(mListMembersToAdd);
            int [] simIndexToAddArray = convertSimIndexToArray(mListMembersToAdd);
            // Create array of raw contact IDs for contacts to add to the group
            long[] membersToRemoveArray = convertToArray(mListMembersToRemove);
            int [] simIndexToRemoveArray = convertSimIndexToArray(mListMembersToRemove);
            // Create the update intent (which includes the updated group name if necessary)
            
        	/*
    		 * New feature by Mediatek Begin
    		 * Original Android code:
    		 * saveIntent = ContactSaveService.createGroupUpdateIntent(activity, mGroupId,
             *		getUpdatedName(), membersToAddArray, membersToRemoveArray,
             *		activity.getClass(), GroupEditorActivity.ACTION_SAVE_COMPLETED);
    		 */
            
            Log.i(TAG, "[edit] slotId"+mSlotId);
            Log.i(TAG, "[edit] getUpdatedName"+getUpdatedName());
            int m =0;
            for(long i :membersToAddArray){
                Log.i(TAG, "[edit] membersToAddArray"+membersToAddArray[m]+"------");
                m++;
            }
            m =0;
            for(int i :simIndexToAddArray){
                Log.i(TAG, "[edit] simIndexToAddArray"+simIndexToAddArray[m]+"------");
                m++;
            }
            m =0;
            for(long i :membersToRemoveArray){
                Log.i(TAG, "[edit] membersToRemoveArray"+membersToRemoveArray[m]+"------");
                m++;
            }
            m =0;
            for(int i :simIndexToRemoveArray){
                Log.i(TAG, "[edit] simIndexToRemoveArray"+simIndexToRemoveArray[m]+"------");
                m++;
            }
            saveIntent = ContactSaveService.createGroupUpdateIntent(activity,
                    mGroupId, getUpdatedName(), membersToAddArray,
                    membersToRemoveArray, activity.getClass(),
                    GroupEditorActivity.ACTION_SAVE_COMPLETED,
                    mOriginalGroupName, mSlotId, simIndexToAddArray,
                    simIndexToRemoveArray,
                    new AccountWithDataSet(mAccountName, mAccountType, mDataSet));
  
            /*
             * New feature by Mediatek End
             */        
        } else {
            throw new IllegalStateException("Invalid intent action type " + mAction);
        }
        showDialog();
        activity.startService(saveIntent);
        return true;
    }

    public void onSaveCompleted(boolean hadChanges, int saveMode, Uri groupUri) {
        dismissDialog();
        boolean success = groupUri != null;
        Log.d(TAG, "onSaveCompleted(" + saveMode + ", " + groupUri + ")");
        if (hadChanges && saveMode != SaveMode.RELOAD) {
//            Toast.makeText(mContext, success ? R.string.groupSavedToast :
//                    R.string.groupSavedErrorToast, Toast.LENGTH_SHORT).show();
        }
        switch (saveMode) {
            case SaveMode.CLOSE:
            case SaveMode.HOME:
                final Intent resultIntent;
                final int resultCode;
                if (success && groupUri != null) {
                    final String requestAuthority =
                            groupUri == null ? null : groupUri.getAuthority();

                    resultIntent = new Intent();
                    if (LEGACY_CONTACTS_AUTHORITY.equals(requestAuthority)) {
                        // Build legacy Uri when requested by caller
                        final long groupId = ContentUris.parseId(groupUri);
                        final Uri legacyContentUri = Uri.parse("content://contacts/groups");
                        final Uri legacyUri = ContentUris.withAppendedId(
                                legacyContentUri, groupId);
                        resultIntent.setData(legacyUri);
                    } else {
                        // Otherwise pass back the given Uri
                        resultIntent.setData(groupUri);
                    }

                    resultCode = Activity.RESULT_OK;
                } else {
                    resultCode = Activity.RESULT_CANCELED;
                    resultIntent = null;
                }
                // It is already saved, so prevent that it is saved again
                mStatus = Status.CLOSING;
                if (mListener != null) {
                    mListener.onSaveFinished(resultCode, resultIntent);
                }
                break;
            case SaveMode.RELOAD: {
                // TODO: Handle reloading the group list
                mStatus = Status.EDITING;
                break;
            }
            default:
                throw new IllegalStateException("Unsupported save mode " + saveMode);
        }
    }

    private boolean hasValidGroupName(boolean showTips) {
        if (mGroupNameView != null) {
            if (!checkEmptyGroupName(showTips)) {
                return checkGroupName(mGroupNameView.getText().toString(), showTips);
            }
        }
        return false;
    }

    private boolean hasNameChange() {
        return mGroupNameView != null &&
                !mGroupNameView.getText().toString().equals(mOriginalGroupName);
    }

    private boolean hasMembershipChange() {
        return mListMembersToAdd.size() > 0 || mListMembersToRemove.size() > 0;
    }

    /**
     * Returns the group's new name or null if there is no change from the
     * original name that was loaded for the group.
     */
    private String getUpdatedName() {
        String groupNameFromTextView = mGroupNameView.getText().toString();
        if (groupNameFromTextView.equals(mOriginalGroupName)) {
            // No name change, so return null
            return null;
        }
        return groupNameFromTextView;
    }

    private static long[] convertToArray(List<Member> listMembers) {
        int size = listMembers.size();
        long[] membersArray = new long[size];
        for (int i = 0; i < size; i++) {
            membersArray[i] = listMembers.get(i).getRawContactId();
        }
        return membersArray;
    }

    private void addExistingMembers(List<Member> members) {

        // Re-create the list to display
        mListToDisplay.clear();
        mListToDisplay.addAll(members);
        mListToDisplay.addAll(mListMembersToAdd);
        mListToDisplay.removeAll(mListMembersToRemove);
        mMemberListAdapter.notifyDataSetChanged();


        // Update the autocomplete adapter (if there is one) so these contacts don't get suggested
        if (mAutoCompleteAdapter != null) {
            mAutoCompleteAdapter.updateExistingMembersList(mListToDisplay);
        }
    }

    private void addMember(Member member) {
        // Update the display list
        if (!mListMembersToAdd.contains(member)) {
            mListMembersToAdd.add(member);
        }
        if (!mListToDisplay.contains(member)) {
            mListToDisplay.add(member);
        }
        mMemberListAdapter.notifyDataSetChanged();

        // Update the autocomplete adapter so the contact doesn't get suggested again
        mAutoCompleteAdapter.addNewMember(member.getContactId());
    }

    private void removeMember(Member member) {
        // If the contact was just added during this session, remove it from the list of
        // members to add
        if (mListMembersToAdd.contains(member)) {
            mListMembersToAdd.remove(member);
        } else {
            // Otherwise this contact was already part of the existing list of contacts,
            // so we need to do a content provider deletion operation
            if (!mListMembersToRemove.contains(member)) {
                mListMembersToRemove.add(member);
            }
        }
        // In either case, update the UI so the contact is no longer in the list of
        // members
        if (mListToDisplay.contains(member)) {
            mListToDisplay.remove(member);
        }
        mMemberListAdapter.notifyDataSetChanged();

        // Update the autocomplete adapter so the contact can get suggested again
        mAutoCompleteAdapter.removeMember(member.getContactId());
    }

    /**
     * The listener for the group metadata (i.e. group name, account type, and account name) loader.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMetaDataLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return new GroupMetaDataLoader(mContext, mGroupUri, false);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            bindGroupMetaData(data);

            // Load existing members
            getLoaderManager().initLoader(LOADER_EXISTING_MEMBERS, null,
                    mGroupMemberListLoaderListener);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    /**
     * The loader listener for the list of existing group members.
     */
    private final LoaderManager.LoaderCallbacks<Cursor> mGroupMemberListLoaderListener =
            new LoaderCallbacks<Cursor>() {

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            return GroupMemberLoader.constructLoaderForGroupEditorQuery(mContext, mGroupId);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            List<Member> listExistingMembers = new ArrayList<Member>();
            data.moveToPosition(-1);
            while (data.moveToNext()) {
                long contactId = data.getLong(GroupEditorQuery.CONTACT_ID);
                long rawContactId = data.getLong(GroupEditorQuery.RAW_CONTACT_ID);
                String lookupKey = data.getString(GroupEditorQuery.CONTACT_LOOKUP_KEY);
                String displayName = data.getString(GroupEditorQuery.CONTACT_DISPLAY_NAME_PRIMARY);
                String photoUri = data.getString(GroupEditorQuery.CONTACT_PHOTO_URI);
                int simIndex = data.getInt(GroupEditorQuery.CONTACT_INDEX_IN_SIM_COLUMN_INDEX);
                
                int i = -1;
                if (mSimInfoWrapper == null) {
                    mSimInfoWrapper = SIMInfoWrapper.getDefault();
                }
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    
                    SIMInfo simInfo = mSimInfoWrapper.getSimInfoBySlot(mSlotId);
                    if (simInfo != null) {
                        i = simInfo.mColor;
                    }
                    Log.i(TAG, "[onLoadFinished] i = " + i);
                    
                }
                Log.i(TAG, "[onLoadFinished] simIndex : " + simIndex);
                if (simIndex > 0) {
                	int isSdnContact = data.getInt(GroupEditorQuery.CONTACT_IS_SDN_CONTACT_COLUMN_INDEX);
                    if (FeatureOption.MTK_GEMINI_SUPPORT) {
                        /*
                         * Change Feature by Mediatek Begin.
                         *   Original Android's code:
                         *     xxx
                         *   CR ID: ALPS00269801
                         *   Descriptions:
                         */
//                        if (OperatorUtils.getOptrProperties().equals("OP02")) {
//                            if (mSlotId == 0) {
//                                photoUri = "content://slot0";
//                                Log.i(TAG, photoUri + "---------photoUri");
//                            } else if (mSlotId == 1) {
//                                photoUri = "content://slot1";
//                                Log.i(TAG, photoUri + "---------photoUri");
//                            }
//                        } else if (i == 0) {
                        /*
                         * Change Feature by Mediatek End.
                         */
                    	
                    	if (isSdnContact > 0) {
                    		if (i == 0) {
								photoUri = "content://sdn-5";
							} else if (i == 1) {
								photoUri = "content://sdn-6";
							} else if (i == 2) {
								photoUri = "content://sdn-7";
							} else if (i == 3) {
								photoUri = "content://sdn-8";
							} else {
								photoUri = "content://sdn";
							}
    					} else {
							if (i == 0) {
								photoUri = "content://sim-10";
							} else if (i == 1) {
								photoUri = "content://sim-11";
							} else if (i == 2) {
								photoUri = "content://sim-12";
							} else if (i == 3) {
								photoUri = "content://sim-13";
							} else {
								photoUri = "content://sim";
							}
    					}
                    } else {
                    	if (isSdnContact > 0) {
                    		photoUri = "content://sdn";
                    	} else {
                            photoUri = "content://sim";
                    	}
                    }

                }
                
                listExistingMembers.add(new Member(rawContactId, lookupKey, contactId,
                        displayName, photoUri, simIndex));
            }

            // Update the display list
            addExistingMembers(listExistingMembers);

            // No more updates
            // TODO: move to a runnable
            getLoaderManager().destroyLoader(LOADER_EXISTING_MEMBERS);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    /**
     * The listener to load a summary of details for a contact.
     */
    // TODO: Remove this step because showing the aggregate contact can be confusing when the user
    // just selected a raw contact
    private final LoaderManager.LoaderCallbacks<Cursor> mContactLoaderListener =
            new LoaderCallbacks<Cursor>() {

        private long mRawContactId;

        @Override
        public CursorLoader onCreateLoader(int id, Bundle args) {
            String memberId = args.getString(MEMBER_LOOKUP_URI_KEY);
            mRawContactId = args.getLong(MEMBER_RAW_CONTACT_ID_KEY);
            return new CursorLoader(mContext, Uri.withAppendedPath(Contacts.CONTENT_URI, memberId),
                    PROJECTION_CONTACT, null, null, null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
            if (!cursor.moveToFirst()) {
                return;
            }
            // Retrieve the contact data fields that will be sufficient to update the adapter with
            // a new entry for this contact
            long contactId = cursor.getLong(CONTACT_ID_COLUMN_INDEX);
            String displayName = cursor.getString(CONTACT_DISPLAY_NAME_PRIMARY_COLUMN_INDEX);
            String lookupKey = cursor.getString(CONTACT_LOOKUP_KEY_COLUMN_INDEX);
            String photoUri = cursor.getString(CONTACT_PHOTO_URI_COLUMN_INDEX);
           
            // the following lines are provided and maintained by Mediatek Inc.
            int simIndex = cursor.getInt(CONTACT_INDEX_IN_SIM_COLUMN_INDEX);
            int indexSimOrPhone = cursor.getInt(CONTACT_INDICATE_PHONE_SIM_COLUMN_INDEX);
            
            int i = -1;
            if (mSimInfoWrapper == null) {
                mSimInfoWrapper = SIMInfoWrapper.getDefault();
            }
            if (FeatureOption.MTK_GEMINI_SUPPORT) {
                SIMInfo simInfo = mSimInfoWrapper.getSimInfoBySlot(mSlotId);
                if (simInfo != null) {
                    i = simInfo.mColor;
                }
                Log.i(TAG, "[onLoadFinished] i = " + i);
            }
            Log.i(TAG, "[onLoadFinished] simIndex : " + simIndex);
            if (simIndex > 0) {
            	int isSdnContact = cursor.getInt(CONTACT_IS_SDN_CONTACT_COLUMN_INDEX);
                if (FeatureOption.MTK_GEMINI_SUPPORT) {
                    /*
                     * Change Feature by Mediatek Begin.
                     *   Original Android's code:
                     *     xxx
                     *   CR ID: ALPS00269801
                     *   Descriptions:
                     */
//                    if (OperatorUtils.getOptrProperties().equals("OP02")) {
//                        if (mSlotId == 0) {
//                            photoUri = "content://slot0";
//                            Log.i(TAG, photoUri + "---------photoUri");
//                        } else if (mSlotId == 1) {
//                            photoUri = "content://slot1";
//                            Log.i(TAG, photoUri + "---------photoUri");
//                        }
//                    } else if (i == 0) {
                    /*
                     * Change Feature by Mediatek End.
                     */
                	
                	if (isSdnContact > 0) {
                		if (i == 0) {
							photoUri = "content://sdn-5";
						} else if (i == 1) {
							photoUri = "content://sdn-6";
						} else if (i == 2) {
							photoUri = "content://sdn-7";
						} else if (i == 3) {
							photoUri = "content://sdn-8";
						} else {
							photoUri = "content://sdn";
						}
					} else {
						if (i == 0) {
							photoUri = "content://sim-10";
						} else if (i == 1) {
							photoUri = "content://sim-11";
						} else if (i == 2) {
							photoUri = "content://sim-12";
						} else if (i == 3) {
							photoUri = "content://sim-13";
						} else {
							photoUri = "content://sim";
						}
					}
                } else {
                	if (isSdnContact > 0) {                    
                		photoUri = "content://sdn";                  
        	        } else {
                        photoUri = "content://sim";
        	        }
                }
            }
            
       	    // the previous lines are provided and maintained by Mediatek Inc.
            
            getLoaderManager().destroyLoader(LOADER_NEW_GROUP_MEMBER);
            Member member = new Member(mRawContactId, lookupKey, contactId, displayName, photoUri, simIndex);
            addMember(member);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {}
    };

    /**
     * This represents a single member of the current group.
     */
    public static class Member implements Parcelable {
        private static final Member[] EMPTY_ARRAY = new Member[0];

        // TODO: Switch to just dealing with raw contact IDs everywhere if possible
        private final long mRawContactId;
        private final long mContactId;
        private final Uri mLookupUri;
        private final String mDisplayName;
        private final Uri mPhotoUri;
        
        // the following lines are provided and maintained by Mediatek Inc.
        private final int mSimIndex;
//        private final int mSimSlotId;
        // the previous lines are provided and maintained by Mediatek Inc.
        
        /**
         * @param simIndex  the sim index add by mediatek
         */
        public Member(long rawContactId, String lookupKey, long contactId, String displayName,
                String photoUri, int simIndex) { 
            mRawContactId = rawContactId;
            mContactId = contactId;
            mLookupUri = Contacts.getLookupUri(contactId, lookupKey);
            mDisplayName = displayName;
            mPhotoUri = (photoUri != null) ? Uri.parse(photoUri) : null;
            
            // the following lines are provided and maintained by Mediatek Inc.
            mSimIndex = simIndex;
       	    // the previous lines are provided and maintained by Mediatek Inc. 
            
        }

        public long getRawContactId() {
            return mRawContactId;
        }

        public long getContactId() {
            return mContactId;
        }

        public Uri getLookupUri() {
            return mLookupUri;
        }

        public String getDisplayName() {
            return mDisplayName;
        }

        public Uri getPhotoUri() {
            return mPhotoUri;
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof Member) {
                Member otherMember = (Member) object;
                return otherMember != null && Objects.equal(mLookupUri, otherMember.getLookupUri());
            }
            return false;
        }

        // Parcelable
        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeLong(mRawContactId);
            dest.writeLong(mContactId);
            dest.writeParcelable(mLookupUri, flags);
            dest.writeString(mDisplayName);
            dest.writeParcelable(mPhotoUri, flags);
            
            // the following lines are provided and maintained by Mediatek Inc.
            dest.writeInt(mSimIndex);
            // the previous lines are provided and maintained by Mediatek Inc.
            
        }

        private Member(Parcel in) {
            mRawContactId = in.readLong();
            mContactId = in.readLong();
            mLookupUri = in.readParcelable(getClass().getClassLoader());
            mDisplayName = in.readString();
            mPhotoUri = in.readParcelable(getClass().getClassLoader());
            
            // the following lines are provided and maintained by Mediatek Inc.
            mSimIndex = in.readInt();
            // the previous lines are provided and maintained by Mediatek Inc.
            
        }

        public static final Parcelable.Creator<Member> CREATOR = new Parcelable.Creator<Member>() {
            public Member createFromParcel(Parcel in) {
                return new Member(in);
            }

            public Member[] newArray(int size) {
                return new Member[size];
            }
        };
        
        // the following lines are provided and maintained by Mediatek Inc.
		public int getmSimIndex() {
			return mSimIndex;
		}
   	    // the previous lines are provided and maintained by Mediatek Inc.

    }

    /**
     * This adapter displays a list of members for the current group being edited.
     */
    private final class MemberListAdapter extends BaseAdapter {

        private boolean mIsGroupMembershipEditable = true;

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View result;
            if (convertView == null) {
                result = mLayoutInflater.inflate(mIsGroupMembershipEditable ?
                        R.layout.group_member_item : R.layout.external_group_member_item,
                        parent, false);
            } else {
                result = convertView;
            }
            final Member member = getItem(position);

            QuickContactBadge badge = (QuickContactBadge) result.findViewById(R.id.badge);
            badge.assignContactUri(member.getLookupUri());

            TextView name = (TextView) result.findViewById(R.id.name);
            name.setText(member.getDisplayName());

            View deleteButton = result.findViewById(R.id.delete_button_container);
            if (deleteButton != null) {
                deleteButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        removeMember(member);
                    }
                });
            }

            mPhotoManager.loadPhoto(badge, member.getPhotoUri(), false, false);
            return result;
        }

        @Override
        public int getCount() {
            return mListToDisplay.size();
        }

        @Override
        public Member getItem(int position) {
            return mListToDisplay.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public void setIsGroupMembershipEditable(boolean editable) {
            mIsGroupMembershipEditable = editable;
        }
    }
    
    // the following lines are provided and maintained by Mediatek Inc.
    private String mCategory = null;
   	private int mSlotId = -1;
   	private int mSimId;
   	private String mSimName ;
   	private ProgressDialog mProgressDialog = null;
   	
    public static int[] convertSimIndexToArray(List<Member> listMembers){
    	int size = listMembers.size();
    	int [] simIndexArray = new int[size];
    	  for (int i = 0; i < size; i++) {
    		  simIndexArray[i] = listMembers.get(i).getmSimIndex();
          }
    	return simIndexArray;
    }
    
    public void load(String action, Uri groupUri, Bundle intentExtras, int slotId, int simId) {
        mAction = action;
        mGroupUri = groupUri;
        mGroupId = (groupUri != null) ? ContentUris.parseId(mGroupUri) : 0;
        mIntentExtras = intentExtras;
        mSlotId = slotId;
        mSimId = simId;
    }
    
    private boolean checkSimServiceState(boolean showTips) {
        if (mSlotId >= 0 && AbstractStartSIMService.isServiceRunning(mSlotId)) {
            if (showTips) {
                Toast.makeText(this.getActivity(),
                        R.string.msg_loading_sim_contacts_toast,
                        Toast.LENGTH_SHORT).show();
            }
            return false;
        }
        return true;
    }
    
    private boolean checkEmptyGroupName(boolean showTips) {
        if(mGroupNameView == null || TextUtils.isEmpty(mGroupNameView.getText())) {
            if (showTips) {
                Toast.makeText(mContext, R.string.name_needed, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }
    
    /*
     * CR ID :ALP00S110925
     * Descriptions: 
     *    It should not save % or / as group name
     */
    private boolean checkGroupName(String groupName, boolean showTips) {
        if(groupName.contains("/") || groupName.contains("%")) {
            if (showTips) {
                Toast.makeText(mContext, R.string.save_group_fail, Toast.LENGTH_SHORT).show();
            }
            return false;
       }
        return true;
    }
    
    public boolean checkOnBackPressedState() {
        String groupName = mGroupNameView == null? null:mGroupNameView.getText().toString();
        if (mGroupNameView != null && TextUtils.isEmpty(groupName)) {
            if (hasMembershipChange()) {
                new AlertDialog.Builder(getActivity())
                        .setIconAttribute(android.R.attr.alertDialogIcon)
                        .setTitle(R.string.group_discard_member)
                        .setMessage(R.string.group_discard_member_reason)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                        onSaveCompleted(false, SaveMode.CLOSE, null);
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create()
                        .show();
                return true;
            }
        }
        return false;
    }
    
    private BroadcastReceiver mReceiver;
    
    class SimReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "In onReceive ");
            final String action = intent.getAction();
            Log.i(TAG, "action is " + action);
            if(Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(action)) {
                processAirplaneModeChanged(intent);
            } else if (TelephonyIntents.ACTION_PHB_STATE_CHANGED.equals(action)) {
                processPhbStateChange(intent);
            } else if (GeminiPhone.EVENT_PRE_3G_SWITCH.equals(action)) {
                Log.i(TAG, "Modem switch .....");
                if (mSlotId >= 0) {
                    getActivity().finish();
                }
            }
        }
        
    };
    
    void processAirplaneModeChanged(Intent intent) {
        boolean isAirplaneModeOn = intent.getBooleanExtra("state", false);
        Log.i(TAG, "[processAirplaneModeChanged]isAirplaneModeOn:" + isAirplaneModeOn);
        if (isAirplaneModeOn && mSlotId >= 0) {
            getActivity().finish();
        }
    }

    void processPhbStateChange(Intent intent) {
        boolean phbReady = intent.getBooleanExtra("ready", false);
        int slotId = intent.getIntExtra("simId", -10);
        Log.i(TAG, "[processPhbStateChange]phbReady:" + phbReady + "|slotId:" + slotId);
        if (mSlotId >= 0) {
            getActivity().finish();
        }
    }

    @Override
    public void onDestroy() {
        if (mReceiver != null) {
            mContext.unregisterReceiver(mReceiver);
            mReceiver = null;
            Log.i(TAG,"unregisterReceiver mReceiver");
        }
        if (mProgressDialog != null) {
        	mProgressDialog.dismiss(); 
        	mProgressDialog = null;
        }
        super.onDestroy();
    }
    
    private void showDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = ProgressDialog.show(mContext, null, mContext
                    .getText(R.string.please_wait), false, false);
        }
    }

    private void dismissDialog() {
        if (mProgressDialog != null) {
            if (!this.getActivity().isFinishing()) {
                mProgressDialog.dismiss();
            }
            mProgressDialog = null;
        }
    }
    
    private SIMInfoWrapper mSimInfoWrapper;
    // the previous lines are provided and maintained by Mediatek Inc.
}
