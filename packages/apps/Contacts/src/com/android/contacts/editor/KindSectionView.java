/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.android.contacts.editor;

import com.android.contacts.R;
import com.android.contacts.editor.Editor.EditorListener;
import com.android.contacts.model.DataKind;
import com.android.contacts.model.EntityDelta;
import com.android.contacts.model.EntityDelta.ValuesDelta;
import com.android.contacts.model.EntityModifier;

import android.content.ContentValues;
import android.content.Context;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


//The following lines are provided and maintained by Mediatek Inc.

import com.mediatek.contacts.util.OperatorUtils;
import com.mediatek.contacts.util.ContactsGroupUtils.USIMAas;
import com.mediatek.featureoption.FeatureOption;
import android.content.res.Resources;
//The previous lines are provided and maintained by Mediatek Inc.
/**
 * Custom view for an entire section of data as segmented by
 * {@link DataKind} around a {@link Data#MIMETYPE}. This view shows a
 * section header and a trigger for adding new {@link Data} rows.
 */
public class KindSectionView extends LinearLayout implements EditorListener {
    private static final String TAG = "KindSectionView";

    private TextView mTitle;
    private ViewGroup mEditors;
    private View mAddFieldFooter;
    private String mTitleString;

    private DataKind mKind;
    private EntityDelta mState;
    private boolean mReadOnly;

    private ViewIdGenerator mViewIdGenerator;

    private LayoutInflater mInflater;

    public KindSectionView(Context context) {
        this(context, null);
    }

    public KindSectionView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (mEditors != null) {
            int childCount = mEditors.getChildCount();
            for (int i = 0; i < childCount; i++) {
                mEditors.getChildAt(i).setEnabled(enabled);
            }
        }

        if (enabled && !mReadOnly) {
            mAddFieldFooter.setVisibility(View.VISIBLE);
        } else {
            mAddFieldFooter.setVisibility(View.GONE);
        }
    }

    public boolean isReadOnly() {
        return mReadOnly;
    }

    /** {@inheritDoc} */
    @Override
    protected void onFinishInflate() {
        setDrawingCacheEnabled(true);
        setAlwaysDrawnWithCacheEnabled(true);

        mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        mTitle = (TextView) findViewById(R.id.kind_title);
        
        //MTK_THEMEMANAGER_APP
        if (FeatureOption.MTK_THEMEMANAGER_APP) {
            Resources res = mContext.getResources();
            int textColor = res.getThemeMainColor();
            if (textColor != 0) {
                mTitle.setTextColor(textColor);
            }
        }
        //MTK_THEMEMANAGER_APP
        
        mEditors = (ViewGroup) findViewById(R.id.kind_editors);
        mAddFieldFooter = findViewById(R.id.add_field_footer);
        mAddFieldFooter.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // Setup click listener to add an empty field when the footer is clicked.
                mAddFieldFooter.setVisibility(View.GONE);
                addItem();
            }
        });
    }

    @Override
    public void onDeleteRequested(Editor editor) {
        // If there is only 1 editor in the section, then don't allow the user to delete it.
        // Just clear the fields in the editor.
        if (OperatorUtils.isAasEnabled(mKind.mAccountType)) {
            editor.clearAllFields();
        } else {
            if (getEditorCount() == 1) {
                editor.clearAllFields();
            } else {
                // Otherwise it's okay to delete this {@link Editor}
                editor.deleteEditor();
            }
        }
        updateAddFooterVisible();
    }

    @Override
    public void onRequest(int request) {
        // If a field has become empty or non-empty, then check if another row
        // can be added dynamically.
        if (request == FIELD_TURNED_EMPTY || request == FIELD_TURNED_NON_EMPTY) {
            updateAddFooterVisible();
        }
    }

    public void setState(DataKind kind, EntityDelta state, boolean readOnly, ViewIdGenerator vig) {
        mKind = kind;
        mState = state;
        mReadOnly = readOnly;
        mViewIdGenerator = vig;

        setId(mViewIdGenerator.getId(state, kind, null, ViewIdGenerator.NO_VIEW_INDEX));

        // TODO: handle resources from remote packages
        mTitleString = (kind.titleRes == -1 || kind.titleRes == 0)
                ? ""
                : getResources().getString(kind.titleRes);
        mTitle.setText(mTitleString);

        if (OperatorUtils.isAasEnabled(mKind.mAccountType)
                && Phone.CONTENT_ITEM_TYPE.equals(kind.mimeType)) {
            mAddFieldFooter.setVisibility(View.GONE);
        }
       
        rebuildFromState();
        updateAddFooterVisible();
        updateSectionVisible();
    }

    public String getTitle() {
        return mTitleString;
    }

    public void setTitleVisible(boolean visible) {
        findViewById(R.id.kind_title_layout).setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    /**
     * Build editors for all current {@link #mState} rows.
     */
    public void rebuildFromState() {
        // Remove any existing editors
        mEditors.removeAllViews();

        // Check if we are displaying anything here
        /*
         * New Feature by Mediatek Begin. 
         * Original Android's code: 
         * M:AAS
         */
        if (OperatorUtils.isAasEnabled(mKind.mAccountType)
                && Phone.CONTENT_ITEM_TYPE.equals(mKind.mimeType)) {
            // for loop twice: primary number be shown before additional numbers.
            ArrayList<ValuesDelta> values = mState.getMimeEntries(mKind.mimeType);
            if (values == null) {
                Log.e(TAG, "rebuildFromState() PHONE: AAS-values == null");
                return;
            }
            Log.i(TAG, "rebuildFromState() PHONE: AAS-values.size()=" + values.size());
            for (ValuesDelta entry : values) {
                boolean isPrimaryNumber = true;
                Integer isAnr = entry.getAsInteger(Data.IS_ADDITIONAL_NUMBER);
                if (isAnr != null) {
                    isPrimaryNumber = (1 != isAnr.intValue());
                }
                if (isPrimaryNumber) {
                    createEditorView(entry);
                }
            }
            for (ValuesDelta entry : values) {
                ContentValues cv = entry.getBefore();
                boolean isAdditionalNumber = false;
                Integer isAnr = entry.getAsInteger(Data.IS_ADDITIONAL_NUMBER);
                if (isAnr != null) {
                    isAdditionalNumber = (1 == isAnr.intValue());
                }
                if (isAdditionalNumber) {
                    createEditorView(entry);
                }
            }
        } else {
            /*
             * New Feature by Mediatek End.
             */
            boolean hasEntries = mState.hasMimeEntries(mKind.mimeType);
            if (hasEntries) {
                for (ValuesDelta entry : mState.getMimeEntries(mKind.mimeType)) {
                    // Skip entries that aren't visible
                    if (!entry.isVisible()) continue;
                    if (isEmptyNoop(entry)) continue;

                    createEditorView(entry);
                }
            }
        }
    }


    /**
     * Creates an EditorView for the given entry. This function must be used while constructing
     * the views corresponding to the the object-model. The resulting EditorView is also added
     * to the end of mEditors
     */
    private View createEditorView(ValuesDelta entry) {
        final View view;
        try {
            view = mInflater.inflate(mKind.editorLayoutResourceId, mEditors, false);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Cannot allocate editor with layout resource ID " +
                    mKind.editorLayoutResourceId + " for MIME type " + mKind.mimeType +
                    " with error " + e.toString());
        }

        view.setEnabled(isEnabled());
        /*
         * New Feature by Mediatek Begin. 
         * Original Android's code: 
         * M:AAS
         */
        if (OperatorUtils.isAasEnabled(mKind.mAccountType)
                && Phone.CONTENT_ITEM_TYPE.equals(mKind.mimeType)) {
            if (view instanceof TextFieldsEditorView) {
                TextFieldsEditorView editor = (TextFieldsEditorView) view;
                editor.setDeletable(true);
                Integer isAnr = entry.getAsInteger(Data.IS_ADDITIONAL_NUMBER);
                if (isAnr != null && (1 == isAnr.intValue())) {
                    editor.setFodrceHideLable(false);
                } else {
                    editor.setFodrceHideLable(true);
                }
                editor.setValues(mKind, entry, mState, mReadOnly, mViewIdGenerator);
                editor.setEditorListener(this);
            } else {
                if (view instanceof Editor) {
                    Editor editor = (Editor) view;
                    editor.setDeletable(true);
                    editor.setValues(mKind, entry, mState, mReadOnly, mViewIdGenerator);
                    editor.setEditorListener(this);
                }
            }
        } else {
            /*
             * New Feature by Mediatek End.
             */
            if (view instanceof Editor) {
                Editor editor = (Editor) view;
                editor.setDeletable(true);
                editor.setValues(mKind, entry, mState, mReadOnly, mViewIdGenerator);
                editor.setEditorListener(this);
            }
        }
        mEditors.addView(view);
        return view;
    }

    /**
     * Tests whether the given item has no changes (so it exists in the database) but is empty
     */
    private boolean isEmptyNoop(ValuesDelta item) {
        if (!item.isNoop()) return false;
        final int fieldCount = mKind.fieldList.size();
        for (int i = 0; i < fieldCount; i++) {
            final String column = mKind.fieldList.get(i).column;
            final String value = item.getAsString(column);
            if (!TextUtils.isEmpty(value)) return false;
        }
        return true;
    }

    private void updateSectionVisible() {
        setVisibility(getEditorCount() != 0 ? VISIBLE : GONE);
    }

    protected void updateAddFooterVisible() {
        if (!mReadOnly && (mKind.typeOverallMax != 1)) {
            // First determine whether there are any existing empty editors.
            updateEmptyEditors();
            // If there are no existing empty editors and it's possible to add
            // another field, then make the "add footer" field visible.

            if (!hasEmptyEditor() && EntityModifier.canInsert(mState, mKind)) {
                if (OperatorUtils.isAasEnabled(mKind.mAccountType)
                        && Phone.CONTENT_ITEM_TYPE.equals(mKind.mimeType)) {
                    mAddFieldFooter.setVisibility(View.GONE);
                } else {
                    mAddFieldFooter.setVisibility(View.VISIBLE);
                }
                return;
            }
        }
        mAddFieldFooter.setVisibility(View.GONE);
    }

    /**
     * Updates the editors being displayed to the user removing extra empty
     * {@link Editor}s, so there is only max 1 empty {@link Editor} view at a time.
     */
    private void updateEmptyEditors() {
        List<View> emptyEditors = getEmptyEditors();

        // If there is more than 1 empty editor, then remove it from the list of editors.

        int max = 1;
        /*
         * New Feature by Mediatek Begin. 
         * Original Android's code: 
         * M:AAS
         */
        if (mKind!=null && OperatorUtils.isAasEnabled(mKind.mAccountType)
                && Phone.CONTENT_ITEM_TYPE.equals(mKind.mimeType)) {
            final int slotId = ContactEditorFragment.getSlotId();
            max = USIMAas.getAnrCount(slotId) + 1;
        }
        /*
         * New Feature by Mediatek End.
         */
        if (emptyEditors.size() > max) {
            for (View emptyEditorView : emptyEditors) {
                // If no child {@link View}s are being focused on within
                // this {@link View}, then remove this empty editor.
                if (emptyEditorView.findFocus() == null) {
                    mEditors.removeView(emptyEditorView);
                }
            }
        }
    }

    /**
     * Returns a list of empty editor views in this section.
     */
    private List<View> getEmptyEditors() {
        List<View> emptyEditorViews = new ArrayList<View>();
        for (int i = 0; i < mEditors.getChildCount(); i++) {
            View view = mEditors.getChildAt(i);
            if (((Editor) view).isEmpty()) {
                emptyEditorViews.add(view);
            }
        }
        return emptyEditorViews;
    }

    /**
     * Returns true if one of the editors has all of its fields empty, or false
     * otherwise.
     */
    private boolean hasEmptyEditor() {
        return getEmptyEditors().size() > 0;
    }

    /**
     * Returns true if all editors are empty.
     */
    public boolean isEmpty() {
        for (int i = 0; i < mEditors.getChildCount(); i++) {
            View view = mEditors.getChildAt(i);
            if (!((Editor) view).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public void addItem() {
        ValuesDelta values = null;
        // If this is a list, we can freely add. If not, only allow adding the first.
        if (mKind.typeOverallMax == 1) {
            if (getEditorCount() == 1) {
                return;
            }

            // If we already have an item, just make it visible
            ArrayList<ValuesDelta> entries = mState.getMimeEntries(mKind.mimeType);
            if (entries != null && entries.size() > 0) {
                values = entries.get(0);
            }
        }

        // Insert a new child, create its view and set its focus
        if (values == null) {
            values = EntityModifier.insertChild(mState, mKind);
        }

        final View newField = createEditorView(values);
        post(new Runnable() {

            @Override
            public void run() {
                newField.requestFocus();
            }
        });

        // Hide the "add field" footer because there is now a blank field.
        mAddFieldFooter.setVisibility(View.GONE);

        // Ensure we are visible
        updateSectionVisible();
    }

    public int getEditorCount() {
        return mEditors.getChildCount();
    }

    public DataKind getKind() {
        return mKind;
    }
}
