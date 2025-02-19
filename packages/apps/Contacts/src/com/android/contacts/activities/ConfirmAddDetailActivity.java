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

package com.android.contacts.activities;

import com.android.contacts.R;
import com.android.contacts.editor.Editor;
import com.android.contacts.editor.ViewIdGenerator;
import com.android.contacts.model.AccountType;
import com.android.contacts.model.AccountTypeManager;
import com.android.contacts.model.DataKind;
import com.android.contacts.model.EntityDelta;
import com.android.contacts.model.EntityDelta.ValuesDelta;
import com.android.contacts.model.EntityDeltaList;
import com.android.contacts.model.EntityModifier;
import com.android.contacts.util.DialogManager;
import com.android.contacts.util.EmptyService;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.AsyncQueryHandler;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.telephony.PhoneNumberUtils;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * This is a dialog-themed activity for confirming the addition of a detail to an existing contact
 * (once the user has selected this contact from a list of all contacts). The incoming intent
 * must have an extra with max 1 phone or email specified, using
 * {@link ContactsContract.Intents.Insert.PHONE} with type
 * {@link ContactsContract.Intents.Insert.PHONE_TYPE} or
 * {@link ContactsContract.Intents.Insert.EMAIL} with type
 * {@link ContactsContract.Intents.Insert.EMAIL_TYPE} intent keys.
 */
public class ConfirmAddDetailActivity extends Activity implements
        DialogManager.DialogShowingViewActivity {

    private static final String TAG = ConfirmAddDetailActivity.class.getSimpleName();

    private static final String LEGACY_CONTACTS_AUTHORITY = "contacts";

    private LayoutInflater mInflater;
    private View mRootView;
    private TextView mDisplayNameView;
    private TextView mReadOnlyWarningView;
    private ImageView mPhotoView;
    private ViewGroup mEditorContainerView;
    private static WeakReference<ProgressDialog> sProgressDialog;

    private AccountTypeManager mAccountTypeManager;
    private ContentResolver mContentResolver;

    private AccountType mEditableAccountType;
    private EntityDelta mState;
    private Uri mContactUri;
    private long mContactId;
    private String mDisplayName;
    private boolean mIsReadyOnly;

    private QueryHandler mQueryHandler;
    private EntityDeltaList mEntityDeltaList;

    private String mMimetype = Phone.CONTENT_ITEM_TYPE;

    /**
     * DialogManager may be needed if the user wants to apply a "custom" label to the contact detail
     */
    private final DialogManager mDialogManager = new DialogManager(this);

    /**
     * PhotoQuery contains the projection used for retrieving the name and photo
     * ID of a contact.
     */
    private interface ContactQuery {
        final String[] COLUMNS = new String[] {
            Contacts._ID,
            Contacts.LOOKUP_KEY,
            Contacts.PHOTO_ID,
            Contacts.DISPLAY_NAME,
        };
        final int _ID = 0;
        final int LOOKUP_KEY = 1;
        final int PHOTO_ID = 2;
        final int DISPLAY_NAME = 3;
    }

    /**
     * PhotoQuery contains the projection used for retrieving the raw bytes of
     * the contact photo.
     */
    private interface PhotoQuery {
        final String[] COLUMNS = new String[] {
            Photo.PHOTO
        };

        final int PHOTO = 0;
    }

    /**
     * ExtraInfoQuery contains the projection used for retrieving the extra info
     * on a contact (only needed if someone else exists with the same name as
     * this contact).
     */
    private interface ExtraInfoQuery {
        final String[] COLUMNS = new String[] {
            RawContacts.CONTACT_ID,
            Data.MIMETYPE,
            Data.DATA1,
        };
        final int CONTACT_ID = 0;
        final int MIMETYPE = 1;
        final int DATA1 = 2;
    }

    /**
     * List of mimetypes to use in order of priority to display for a contact in
     * a disambiguation case. For example, if the contact does not have a
     * nickname, use the email field, and etc.
     */
    private static final String[] sMimeTypePriorityList = new String[] { Nickname.CONTENT_ITEM_TYPE,
            Email.CONTENT_ITEM_TYPE, Im.CONTENT_ITEM_TYPE, StructuredPostal.CONTENT_ITEM_TYPE,
            Phone.CONTENT_ITEM_TYPE };

    private static final int TOKEN_CONTACT_INFO = 0;
    private static final int TOKEN_PHOTO_QUERY = 1;
    private static final int TOKEN_DISAMBIGUATION_QUERY = 2;
    private static final int TOKEN_EXTRA_INFO_QUERY = 3;

    private final OnClickListener mDetailsButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mIsReadyOnly) {
                onSaveCompleted(true);
            } else {
                doSaveAction();
            }
        }
    };

    private final OnClickListener mDoneButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            doSaveAction();
        }
    };

    private final OnClickListener mCancelButtonClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            setResult(RESULT_CANCELED);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mContentResolver = getContentResolver();

        final Intent intent = getIntent();
        mContactUri = intent.getData();

        if (mContactUri == null) {
            setResult(RESULT_CANCELED);
            finish();
        }

        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey(ContactsContract.Intents.Insert.PHONE)) {
                mMimetype = Phone.CONTENT_ITEM_TYPE;
            } else if (extras.containsKey(ContactsContract.Intents.Insert.EMAIL)) {
                mMimetype = Email.CONTENT_ITEM_TYPE;
              //The following lines are provided and maintained by Mediatek Inc.
            } else if (extras.containsKey(ContactsContract.Intents.Insert.SIP_ADDRESS)) {
                mMimetype = SipAddress.CONTENT_ITEM_TYPE;
                //The previous lines are provided and maintained by Mediatek Inc.
            } else {
                throw new IllegalStateException("Error: No valid mimetype found in intent extras");
            }
        }

        mAccountTypeManager = AccountTypeManager.getInstance(this);

        setContentView(R.layout.confirm_add_detail_activity);

        mRootView = findViewById(R.id.root_view);
        mReadOnlyWarningView = (TextView) findViewById(R.id.read_only_warning);

        // Setup "header" (containing contact info) to save the detail and then go to the editor
        findViewById(R.id.open_details_push_layer).setOnClickListener(mDetailsButtonClickListener);

        // Setup "done" button to save the detail to the contact and exit.
        findViewById(R.id.btn_done).setOnClickListener(mDoneButtonClickListener);

        // Setup "cancel" button to return to previous activity.
        findViewById(R.id.btn_cancel).setOnClickListener(mCancelButtonClickListener);

        // Retrieve references to all the Views in the dialog activity.
        mDisplayNameView = (TextView) findViewById(R.id.name);
        mPhotoView = (ImageView) findViewById(R.id.photo);
        mEditorContainerView = (ViewGroup) findViewById(R.id.editor_container);

        startContactQuery(mContactUri, true);

        new QueryEntitiesTask(this).execute(intent);
    }

    @Override
    public DialogManager getDialogManager() {
        return mDialogManager;
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        if (DialogManager.isManagedId(id)) return mDialogManager.onCreateDialog(id, args);

        // Nobody knows about the Dialog
        Log.w(TAG, "Unknown dialog requested, id: " + id + ", args: " + args);
        return null;
    }

    /**
     * Reset the query handler by creating a new QueryHandler instance.
     */
    private void resetAsyncQueryHandler() {
        // the api AsyncQueryHandler.cancelOperation() doesn't really work. Since we really
        // need the old async queries to be cancelled, let's do it the hard way.
        mQueryHandler = new QueryHandler(mContentResolver);
    }

    /**
     * Internal method to query contact by Uri.
     *
     * @param contactUri the contact uri
     * @param resetQueryHandler whether to use a new AsyncQueryHandler or not
     */
    private void startContactQuery(Uri contactUri, boolean resetQueryHandler) {
        if (resetQueryHandler) {
            resetAsyncQueryHandler();
        }

        mQueryHandler.startQuery(TOKEN_CONTACT_INFO, contactUri, contactUri, ContactQuery.COLUMNS,
                null, null, null);
    }

    /**
     * Internal method to query contact photo by photo id and uri.
     *
     * @param photoId the photo id.
     * @param lookupKey the lookup uri.
     * @param resetQueryHandler whether to use a new AsyncQueryHandler or not.
     */
    private void startPhotoQuery(long photoId, Uri lookupKey, boolean resetQueryHandler) {
        if (resetQueryHandler) {
            resetAsyncQueryHandler();
        }

        mQueryHandler.startQuery(TOKEN_PHOTO_QUERY, lookupKey,
                ContentUris.withAppendedId(Data.CONTENT_URI, photoId),
                PhotoQuery.COLUMNS, null, null, null);
    }

    /**
     * Internal method to query for contacts with a given display name.
     *
     * @param contactDisplayName the display name to look for.
     */
    private void startDisambiguationQuery(String contactDisplayName) {
        // Apply a limit of 1 result to the query because we only need to
        // determine whether or not at least one other contact has the same
        // name. We don't need to find ALL other contacts with the same name.
        Builder builder = Contacts.CONTENT_URI.buildUpon();
        builder.appendQueryParameter("limit", String.valueOf(1));
        Uri uri = builder.build();

        mQueryHandler.startQuery(TOKEN_DISAMBIGUATION_QUERY, null, uri,
                new String[] { Contacts._ID } /* unused projection but a valid one was needed */,
                Contacts.DISPLAY_NAME_PRIMARY + " = ? and " + Contacts.PHOTO_ID + " is null and "
                + Contacts._ID + " <> ?",
                new String[] { contactDisplayName, String.valueOf(mContactId) }, null);
    }

    /**
     * Internal method to query for extra data fields for this contact.
     */
    private void startExtraInfoQuery() {
        mQueryHandler.startQuery(TOKEN_EXTRA_INFO_QUERY, null, Data.CONTENT_URI,
                ExtraInfoQuery.COLUMNS, RawContacts.CONTACT_ID + " = ?",
                new String[] { String.valueOf(mContactId) }, null);
    }

    private static class QueryEntitiesTask extends AsyncTask<Intent, Void, EntityDeltaList> {

        private ConfirmAddDetailActivity activityTarget;
        private String mSelection;

        public QueryEntitiesTask(ConfirmAddDetailActivity target) {
            activityTarget = target;
        }

        @Override
        protected EntityDeltaList doInBackground(Intent... params) {

            final Intent intent = params[0];

            final ContentResolver resolver = activityTarget.getContentResolver();

            // Handle both legacy and new authorities
            final Uri data = intent.getData();
            final String authority = data.getAuthority();
            final String mimeType = intent.resolveType(resolver);

            mSelection = "0";
            String selectionArg = null;
            if (ContactsContract.AUTHORITY.equals(authority)) {
                if (Contacts.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    // Handle selected aggregate
                    final long contactId = ContentUris.parseId(data);
                    selectionArg = String.valueOf(contactId);
                    mSelection = RawContacts.CONTACT_ID + "=?";
                } else if (RawContacts.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    final long rawContactId = ContentUris.parseId(data);
                    final long contactId = queryForContactId(resolver, rawContactId);
                    selectionArg = String.valueOf(contactId);
                    mSelection = RawContacts.CONTACT_ID + "=?";
                }
            } else if (android.provider.Contacts.AUTHORITY.equals(authority)) {
                final long rawContactId = ContentUris.parseId(data);
                selectionArg = String.valueOf(rawContactId);
                mSelection = Data.RAW_CONTACT_ID + "=?";
            }

            // Note that this query does not need to concern itself with whether the contact is
            // the user's profile, since the profile does not show up in the picker.
            return EntityDeltaList.fromQuery(RawContactsEntity.CONTENT_URI,
                    activityTarget.getContentResolver(), mSelection,
                    new String[] { selectionArg }, null);
        }

        private static long queryForContactId(ContentResolver resolver, long rawContactId) {
            Cursor contactIdCursor = null;
            long contactId = -1;
            try {
                contactIdCursor = resolver.query(RawContacts.CONTENT_URI,
                        new String[] { RawContacts.CONTACT_ID },
                        RawContacts._ID + "=?", new String[] { String.valueOf(rawContactId) },
                        null);
                if (contactIdCursor != null && contactIdCursor.moveToFirst()) {
                    contactId = contactIdCursor.getLong(0);
                }
            } finally {
                if (contactIdCursor != null) {
                    contactIdCursor.close();
                }
            }
            return contactId;
        }

        @Override
        protected void onPostExecute(EntityDeltaList entityList) {
            if (activityTarget.isFinishing()) {
                return;
            }
            activityTarget.setEntityDeltaList(entityList);
            activityTarget.findEditableRawContact();
            activityTarget.parseExtras();
            activityTarget.bindEditor();
        }
    }

    private class QueryHandler extends AsyncQueryHandler {

        public QueryHandler(ContentResolver cr) {
            super(cr);
        }

        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            try {
                if (this != mQueryHandler) {
                    Log.d(TAG, "onQueryComplete: discard result, the query handler is reset!");
                    return;
                }
                if (ConfirmAddDetailActivity.this.isFinishing()) {
                    return;
                }

                switch (token) {
                    case TOKEN_PHOTO_QUERY: {
                        // Set the photo
                        Bitmap photoBitmap = null;
                        if (cursor != null && cursor.moveToFirst()
                                && !cursor.isNull(PhotoQuery.PHOTO)) {
                            byte[] photoData = cursor.getBlob(PhotoQuery.PHOTO);
                            photoBitmap = BitmapFactory.decodeByteArray(photoData, 0,
                                    photoData.length, null);
                        }

                        if (photoBitmap != null) {
                            mPhotoView.setImageBitmap(photoBitmap);
                        }

                        break;
                    }
                    case TOKEN_CONTACT_INFO: {
                        // Set the contact's name
                        if (cursor != null && cursor.moveToFirst()) {
                            // Get the cursor values
                            mDisplayName = cursor.getString(ContactQuery.DISPLAY_NAME);
                            final long photoId = cursor.getLong(ContactQuery.PHOTO_ID);

                            // If there is no photo ID, then do a disambiguation
                            // query because other contacts could have the same
                            // name as this contact.
                            if (photoId == 0) {
                                mContactId = cursor.getLong(ContactQuery._ID);
                                startDisambiguationQuery(mDisplayName);
                            } else {
                                // Otherwise do the photo query.
                                Uri lookupUri = Contacts.getLookupUri(mContactId,
                                        cursor.getString(ContactQuery.LOOKUP_KEY));
                                startPhotoQuery(photoId, lookupUri,
                                        false /* don't reset query handler */);
                                // Display the name because there is no
                                // disambiguation query.
                                setDisplayName();
                                onLoadDataFinished();
                            }
                        }
                        break;
                    }
                    case TOKEN_DISAMBIGUATION_QUERY: {
                        // If a cursor was returned with more than 0 results,
                        // then at least one other contact exists with the same
                        // name as this contact. Extra info on this contact must
                        // be displayed to disambiguate the contact, so retrieve
                        // those additional fields. Otherwise, no other contacts
                        // with this name exists, so do nothing further.
                        if (cursor != null && cursor.getCount() > 0) {
                            startExtraInfoQuery();
                        } else {
                            // If there are no other contacts with this name,
                            // then display the name.
                            setDisplayName();
                            onLoadDataFinished();
                        }
                        break;
                    }
                    case TOKEN_EXTRA_INFO_QUERY: {
                        // This case should only occur if there are one or more
                        // other contacts with the same contact name.
                        if (cursor != null && cursor.moveToFirst()) {
                            HashMap<String, String> hashMapCursorData = new
                                    HashMap<String, String>();

                            // Convert the cursor data into a hashmap of
                            // (mimetype, data value) pairs. If a contact has
                            // multiple values with the same mimetype, it's fine
                            // to override that hashmap entry because we only
                            // need one value of that type.
                            while (!cursor.isAfterLast()) {
                                final String mimeType = cursor.getString(ExtraInfoQuery.MIMETYPE);
                                if (!TextUtils.isEmpty(mimeType)) {
                                    String value = cursor.getString(ExtraInfoQuery.DATA1);
                                    if (!TextUtils.isEmpty(value)) {
                                        // As a special case, phone numbers
                                        // should be formatted in a specific way.
                                        if (Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                                            value = PhoneNumberUtils.formatNumber(value);
                                        }
                                        hashMapCursorData.put(mimeType, value);
                                    }
                                }
                                cursor.moveToNext();
                            }

                            // Find the first non-empty field according to the
                            // mimetype priority list and display this under the
                            // contact's display name to disambiguate the contact.
                            for (String mimeType : sMimeTypePriorityList) {
                                if (hashMapCursorData.containsKey(mimeType)) {
                                    setDisplayName();
                                    setExtraInfoField(hashMapCursorData.get(mimeType));
                                    break;
                                }
                            }
                            onLoadDataFinished();
                        }
                        break;
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
    }

    public void setEntityDeltaList(EntityDeltaList entityList) {
        mEntityDeltaList = entityList;
    }

    public void findEditableRawContact() {
        if (mEntityDeltaList == null) {
            return;
        }
        for (EntityDelta state : mEntityDeltaList) {
            final String accountType = state.getValues().getAsString(RawContacts.ACCOUNT_TYPE);
            final String dataSet = state.getValues().getAsString(RawContacts.DATA_SET);
            final AccountType type = mAccountTypeManager.getAccountType(accountType, dataSet);

            if (type.areContactsWritable()) {
                mEditableAccountType = type;
                mState = state;
                return;
            }
        }
    }

    public void parseExtras() {
        if (mEditableAccountType == null || mState == null) {
            return;
        }
        // Handle any incoming values that should be inserted
        final Bundle extras = getIntent().getExtras();
        if (extras != null && extras.size() > 0) {
            // If there are any intent extras, add them as additional fields in the EntityDelta.
            EntityModifier.parseExtras(this, mEditableAccountType, mState, extras);
        }
    }

    /**
     * Rebuild the editor to match our underlying {@link #mEntityDeltaList} object.
     */
    private void bindEditor() {
        if (mEntityDeltaList == null) {
            return;
        }

        // If no valid raw contact (to insert the data) was found, we won't have an editable
        // account type to use. In this case, display an error message and hide the "OK" button.
        if (mEditableAccountType == null) {
            mIsReadyOnly = true;
            mReadOnlyWarningView.setText(getString(R.string.contact_read_only));
            mReadOnlyWarningView.setVisibility(View.VISIBLE);
            mEditorContainerView.setVisibility(View.GONE);
            findViewById(R.id.btn_done).setVisibility(View.GONE);
            // Nothing more to be done, just show the UI
            onLoadDataFinished();
            return;
        }

        // Otherwise display an editor that allows the user to add the data to this raw contact.
        for (DataKind kind : mEditableAccountType.getSortedDataKinds()) {
            // Skip kind that are not editable
            if (!kind.editable) continue;
            if (mMimetype.equals(kind.mimeType)) {
                for (ValuesDelta valuesDelta : mState.getMimeEntries(mMimetype)) {
                    // Skip entries that aren't visible
                    if (!valuesDelta.isVisible()) continue;
                    if (valuesDelta.isInsert()) {
                        inflateEditorView(kind, valuesDelta, mState);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Creates an EditorView for the given entry. This function must be used while constructing
     * the views corresponding to the the object-model. The resulting EditorView is also added
     * to the end of mEditors
     */
    private void inflateEditorView(DataKind dataKind, ValuesDelta valuesDelta, EntityDelta state) {
        final View view = mInflater.inflate(dataKind.editorLayoutResourceId, mEditorContainerView,
                false);

        if (view instanceof Editor) {
            Editor editor = (Editor) view;
            // Don't allow deletion of the field because there is only 1 detail in this editor.
            editor.setDeletable(false);
            editor.setValues(dataKind, valuesDelta, state, false, new ViewIdGenerator());
        }

        mEditorContainerView.addView(view);
    }

    /**
     * Set the display name to the correct TextView. Don't do this until it is
     * certain there is no need for a disambiguation field (otherwise the screen
     * will flicker because the name will be centered and then moved upwards).
     */
    private void setDisplayName() {
        mDisplayNameView.setText(mDisplayName);
    }

    /**
     * Set the TextView (for extra contact info) with the given value and make the
     * TextView visible.
     */
    private void setExtraInfoField(String value) {
        TextView extraTextView = (TextView) findViewById(R.id.extra_info);
        extraTextView.setVisibility(View.VISIBLE);
        extraTextView.setText(value);
    }

    /**
     * Shows all the contents of the dialog to the user at one time. This should only be called
     * once all the queries have completed, otherwise the screen will flash as additional data
     * comes in.
     */
    private void onLoadDataFinished() {
        mRootView.setVisibility(View.VISIBLE);
    }

    /**
     * Saves or creates the contact based on the mode, and if successful
     * finishes the activity.
     */
    private void doSaveAction() {
        final PersistTask task = new PersistTask(this, mAccountTypeManager);
        task.execute(mEntityDeltaList);
    }


    /**
     * Background task for persisting edited contact data, using the changes
     * defined by a set of {@link EntityDelta}. This task starts
     * {@link EmptyService} to make sure the background thread can finish
     * persisting in cases where the system wants to reclaim our process.
     */
    public static class PersistTask extends AsyncTask<EntityDeltaList, Void, Integer> {
        // In the future, use ContactSaver instead of WeakAsyncTask because of
        // the danger of the activity being null during a save action
        private static final int PERSIST_TRIES = 3;

        private static final int RESULT_UNCHANGED = 0;
        private static final int RESULT_SUCCESS = 1;
        private static final int RESULT_FAILURE = 2;

        private ConfirmAddDetailActivity activityTarget;

        private AccountTypeManager mAccountTypeManager;

        public PersistTask(ConfirmAddDetailActivity target, AccountTypeManager accountTypeManager) {
            activityTarget = target;
            mAccountTypeManager = accountTypeManager;
        }

        @Override
        protected void onPreExecute() {
            sProgressDialog = new WeakReference<ProgressDialog>(ProgressDialog.show(activityTarget,
                    null, activityTarget.getText(R.string.savingContact)));

            // Before starting this task, start an empty service to protect our
            // process from being reclaimed by the system.
            final Context context = activityTarget;
            context.startService(new Intent(context, EmptyService.class));
        }

        @Override
        protected Integer doInBackground(EntityDeltaList... params) {
            final Context context = activityTarget;
            final ContentResolver resolver = context.getContentResolver();

            EntityDeltaList state = params[0];

            if (state == null) {
                return RESULT_FAILURE;
            }

            // Trim any empty fields, and RawContacts, before persisting
            EntityModifier.trimEmpty(state, mAccountTypeManager);

            // Attempt to persist changes
            int tries = 0;
            Integer result = RESULT_FAILURE;
            while (tries++ < PERSIST_TRIES) {
                try {
                    // Build operations and try applying
                    final ArrayList<ContentProviderOperation> diff = state.buildDiff();
                    ContentProviderResult[] results = null;
                    if (!diff.isEmpty()) {
                         results = resolver.applyBatch(ContactsContract.AUTHORITY, diff);
                    }

                    result = (diff.size() > 0) ? RESULT_SUCCESS : RESULT_UNCHANGED;
                    break;

                } catch (RemoteException e) {
                    // Something went wrong, bail without success
                    Log.e(TAG, "Problem persisting user edits", e);
                    break;

                } catch (OperationApplicationException e) {
                    // Version consistency failed, bail without success
                    Log.e(TAG, "Version consistency failed", e);
                    break;
                }
            }

            return result;
        }

        /** {@inheritDoc} */
        @Override
        protected void onPostExecute(Integer result) {
            final Context context = activityTarget;

            dismissProgressDialog();

            // Show a toast message based on the success or failure of the save action.
            if (result == RESULT_SUCCESS) {
                Toast.makeText(context, R.string.contactSavedToast, Toast.LENGTH_SHORT).show();
            } else if (result == RESULT_FAILURE) {
                Toast.makeText(context, R.string.contactSavedErrorToast, Toast.LENGTH_LONG).show();
            }

            // Stop the service that was protecting us
            context.stopService(new Intent(context, EmptyService.class));
            activityTarget.onSaveCompleted(result != RESULT_FAILURE);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Dismiss the progress dialog here to prevent leaking the window on orientation change.
        dismissProgressDialog();
    }

    /**
     * Dismiss the progress dialog (check if it is null because it is a {@link WeakReference}).
     */
    private static void dismissProgressDialog() {
        ProgressDialog dialog = (sProgressDialog == null) ? null : sProgressDialog.get();
        if (dialog != null) {
            dialog.dismiss();
        }
        sProgressDialog = null;
    }

    /**
     * This method is intended to be executed after the background task for saving edited info has
     * finished. The method sets the activity result (and intent if applicable) and finishes the
     * activity.
     * @param success is true if the save task completed successfully, or false otherwise.
     */
    private void onSaveCompleted(boolean success) {
        if (success) {
            Intent intent = new Intent(Intent.ACTION_VIEW, mContactUri);
            setResult(RESULT_OK, intent);
        } else {
            setResult(RESULT_CANCELED);
        }
        finish();
    }
}