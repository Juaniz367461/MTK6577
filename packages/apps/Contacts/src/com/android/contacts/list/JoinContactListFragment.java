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

import com.android.contacts.R;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.ContentUris;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Fragment for the Join Contact list.
 */
public class JoinContactListFragment extends ContactEntryListFragment<JoinContactListAdapter> {

    private static final int DISPLAY_NAME_LOADER = -2;

    private static final String KEY_TARGET_CONTACT_ID = "targetContactId";

    private OnContactPickerActionListener mListener;
    private long mTargetContactId;

    private final LoaderCallbacks<Cursor> mLoaderCallbacks = new LoaderCallbacks<Cursor>() {

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case DISPLAY_NAME_LOADER: {
                    // Loader for the display name of the target contact
                    return new CursorLoader(getActivity(),
                            ContentUris.withAppendedId(Contacts.CONTENT_URI, mTargetContactId),
                            new String[] { Contacts.DISPLAY_NAME }, null, null, null);
                }
                case JoinContactListAdapter.PARTITION_ALL_CONTACTS: {
                    JoinContactLoader loader = new JoinContactLoader(getActivity());
                    JoinContactListAdapter adapter = getAdapter();
                    if (adapter != null) {
                        adapter.configureLoader(loader, 0);
                    }
                    return loader;
                }
            }
            throw new IllegalArgumentException("No loader for ID=" + id);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            switch (loader.getId()) {
                case DISPLAY_NAME_LOADER: {
                    if (data != null && data.moveToFirst()) {
                        showTargetContactName(data.getString(0));
                    }
                    break;
                }
                case JoinContactListAdapter.PARTITION_ALL_CONTACTS: {
                    Cursor suggestionsCursor = ((JoinContactLoader) loader).getSuggestionsCursor();
                    onContactListLoaded(suggestionsCursor, data);
                    break;
                }
            }
        }

        public void onLoaderReset(Loader<Cursor> loader) {
        }
    };

    public JoinContactListFragment() {
        setPhotoLoaderEnabled(true);
        setSectionHeaderDisplayEnabled(true);
        setVisibleScrollbarEnabled(false);
        setQuickContactEnabled(false);
    }

    public void setOnContactPickerActionListener(OnContactPickerActionListener listener) {
        mListener = listener;
    }

    @Override
    protected void startLoading() {
        configureAdapter();

        getLoaderManager().initLoader(DISPLAY_NAME_LOADER, null, mLoaderCallbacks);
        getLoaderManager().initLoader(JoinContactListAdapter.PARTITION_ALL_CONTACTS,
                null, mLoaderCallbacks);
    }

    private void onContactListLoaded(Cursor suggestionsCursor, Cursor allContactsCursor) {
        JoinContactListAdapter adapter = getAdapter();
        adapter.setSuggestionsCursor(suggestionsCursor);
        setVisibleScrollbarEnabled(true);
        onPartitionLoaded(JoinContactListAdapter.PARTITION_ALL_CONTACTS, allContactsCursor);
    }

    private static final int DISPLAYNAME_LENGTH = 18;    
    private void showTargetContactName(String displayName) {
        Activity activity = getActivity();
        TextView blurbView = (TextView) activity.findViewById(R.id.join_contact_blurb);

        /*
         * Bug Fix by Mediatek Begin. Original Android's code: String blurb =
         * activity.getString(R.string.blurbJoinContactDataWith, displayName);
         * CR ID: ALPS00117275 Descriptions:
         */
        String blurb;
        if (displayName != null && displayName.length() > DISPLAYNAME_LENGTH) {
            String strTemp = displayName.subSequence(0, DISPLAYNAME_LENGTH).toString();
            strTemp = strTemp + "...";
            blurb = getString(R.string.blurbJoinContactDataWith, strTemp);
        } else {
            blurb = getString(R.string.blurbJoinContactDataWith, displayName);
        }
        /*
         * Bug Fix by Mediatek End.
         */
        blurbView.setText(blurb);
    }

    public void setTargetContactId(long targetContactId) {
        mTargetContactId = targetContactId;
    }

    @Override
    public JoinContactListAdapter createListAdapter() {
        return new JoinContactListAdapter(getActivity());
    }

    @Override
    protected void configureAdapter() {
        super.configureAdapter();
        JoinContactListAdapter adapter = getAdapter();
        adapter.setTargetContactId(mTargetContactId);
    }

    @Override
    protected View inflateView(LayoutInflater inflater, ViewGroup container) {
        return inflater.inflate(R.layout.join_contact_picker_list_content, null);
    }

    @Override
    protected void onItemClick(int position, long id) {
        JoinContactListAdapter adapter = getAdapter();
        int partition = adapter.getPartitionForPosition(position);
        mListener.onPickContactAction(adapter.getContactUri(position));
    }

    @Override
    public void onPickerResult(Intent data) {
        mListener.onPickContactAction(data.getData());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_TARGET_CONTACT_ID, mTargetContactId);
    }

    @Override
    public void restoreSavedState(Bundle savedState) {
        super.restoreSavedState(savedState);
        if (savedState != null) {
            mTargetContactId = savedState.getLong(KEY_TARGET_CONTACT_ID);
        }
    }
}
