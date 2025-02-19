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

package com.android.contacts.editor;

import com.android.contacts.ContactsUtils;
import com.android.contacts.R;
import com.android.contacts.model.AccountType.EditType;
import com.android.contacts.model.DataKind;
import com.android.contacts.model.EntityDelta;
import com.android.contacts.model.EntityDelta.ValuesDelta;
import com.android.contacts.model.EntityModifier;
import com.android.contacts.util.DialogManager;
import com.android.contacts.util.DialogManager.DialogShowingView;
import com.mediatek.contacts.model.UsimAccountType;
import com.mediatek.contacts.util.OperatorUtils;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Entity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

/**
 * Base class for editors that handles labels and values. Uses
 * {@link ValuesDelta} to read any existing {@link Entity} values, and to
 * correctly write any changes values.
 */
public abstract class LabeledEditorView extends LinearLayout implements Editor, DialogShowingView {
    private static final String TAG = "LabeledEditorView";
    protected static final String DIALOG_ID_KEY = "dialog_id";
    private static final int DIALOG_ID_CUSTOM = 1;

    private static final int INPUT_TYPE_CUSTOM = EditorInfo.TYPE_CLASS_TEXT
            | EditorInfo.TYPE_TEXT_FLAG_CAP_WORDS;

    private boolean mForceHideLabel = false;

    private Spinner mLabel;
    private EditTypeAdapter mEditTypeAdapter;
    private View mDeleteContainer;
    private ImageView mDelete;

    private DataKind mKind;
    private ValuesDelta mEntry;
    private EntityDelta mState;
    private boolean mReadOnly;
    private boolean mWasEmpty = true;
    private boolean mIsDeletable = true;
    private boolean mIsAttachedToWindow;

    private EditType mType;

    private ViewIdGenerator mViewIdGenerator;
    private DialogManager mDialogManager = null;
    private EditorListener mListener;
    protected int mMinLineItemHeight;

    /**
     * A marker in the spinner adapter of the currently selected custom type.
     */
    public static final EditType CUSTOM_SELECTION = new EditType(0, 0);

    private OnItemSelectedListener mSpinnerListener = new OnItemSelectedListener() {

        @Override
        public void onItemSelected(
                AdapterView<?> parent, View view, int position, long id) {
            onTypeSelectionChange(position);
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    public LabeledEditorView(Context context) {
        super(context);
        init(context);
    }

    public LabeledEditorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LabeledEditorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        mMinLineItemHeight = context.getResources().getDimensionPixelSize(
                R.dimen.editor_min_line_item_height);
    }

    /** {@inheritDoc} */
    @Override
    protected void onFinishInflate() {

        mLabel = (Spinner) findViewWithTag("edit_spinner");
        mLabel.setOnItemSelectedListener(mSpinnerListener);

        mDelete = (ImageView) findViewById(R.id.delete_button);
        mDeleteContainer = findViewById(R.id.delete_button_container);
        mDeleteContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                // defer removal of this button so that the pressed state is visible shortly
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        // Don't do anything if the view is no longer attached to the window
                        // (This check is needed because when this {@link Runnable} is executed,
                        // we can't guarantee the view is still valid.
                        if (!mIsAttachedToWindow) {
                            return;
                        }
                        // Send the delete request to the listener (which will in turn call
                        // deleteEditor() on this view if the deletion is valid - i.e. this is not
                        // the last {@link Editor} in the section).
                        if (mListener != null) {
                            mListener.onDeleteRequested(LabeledEditorView.this);
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Keep track of when the view is attached or detached from the window, so we know it's
        // safe to remove views (in case the user requests to delete this editor).
        mIsAttachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsAttachedToWindow = false;
    }

    @Override
    public void deleteEditor() {
        // Keep around in model, but mark as deleted
        mEntry.markDeleted();

        // Remove the view
        ((ViewGroup) getParent()).removeView(LabeledEditorView.this);
    }

    public boolean isReadOnly() {
        return mReadOnly;
    }

    public int getBaseline(int row) {
        if (row == 0 && mLabel != null) {
            return mLabel.getBaseline();
        }
        return -1;
    }

    /**
     * Configures the visibility of the type label button and enables or disables it properly.
     */
    private void setupLabelButton(boolean shouldExist) {
        if (shouldExist) {
            mLabel.setEnabled(!mReadOnly && isEnabled());
            mLabel.setVisibility(View.VISIBLE);
        } else {
            mLabel.setVisibility(View.GONE);
        }
    }

    /**
     * Configures the visibility of the "delete" button and enables or disables it properly.
     */
    private void setupDeleteButton() {
        if (mIsDeletable) {
            mDeleteContainer.setVisibility(View.VISIBLE);
            mDelete.setEnabled(!mReadOnly && isEnabled());
        } else {
            mDeleteContainer.setVisibility(View.GONE);
        }
    }

    public void setDeleteButtonVisible(boolean visible) {
        if (mIsDeletable) {
            mDeleteContainer.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        }
    }

    protected void onOptionalFieldVisibilityChange() {
        if (mListener != null) {
            mListener.onRequest(EditorListener.EDITOR_FORM_CHANGED);
        }
    }

    @Override
    public void setEditorListener(EditorListener listener) {
        mListener = listener;
    }

    @Override
    public void setDeletable(boolean deletable) {
        mIsDeletable = deletable;
        setupDeleteButton();
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        mLabel.setEnabled(!mReadOnly && enabled);
        mDelete.setEnabled(!mReadOnly && enabled);
    }

    public Spinner getLabel() {
        return mLabel;
    }

    public ImageView getDelete() {
        return mDelete;
    }

    protected DataKind getKind() {
        return mKind;
    }

    protected ValuesDelta getEntry() {
        return mEntry;
    }

    protected EditType getType() {
        return mType;
    }

    /**
     * Build the current label state based on selected {@link EditType} and
     * possible custom label string.
     */
    private void rebuildLabel() {
        mEditTypeAdapter = new EditTypeAdapter(mContext);
        mLabel.setAdapter(mEditTypeAdapter);
        Log.i(TAG,"mEditTypeAdapter.hasCustomSelection() : "+mEditTypeAdapter.hasCustomSelection());
        if (mEditTypeAdapter.hasCustomSelection()) {
            mLabel.setSelection(mEditTypeAdapter.getPosition(CUSTOM_SELECTION));
        } else {
            Log.i(TAG,"mEditTypeAdapter.getPosition(mType) : "+mEditTypeAdapter.getPosition(mType)+" | mType : "+mType);
            mLabel.setSelection(mEditTypeAdapter.getPosition(mType));
        }
    }

    @Override
    public void onFieldChanged(String column, String value) {
        if (!isFieldChanged(column, value)) {
            return;
        }

        // Field changes are saved directly
        mEntry.put(column, value);
        if (mListener != null) {
            mListener.onRequest(EditorListener.FIELD_CHANGED);
        }

        boolean isEmpty = isEmpty();
        if (mWasEmpty != isEmpty) {
            if (isEmpty) {
                if (mListener != null) {
                    mListener.onRequest(EditorListener.FIELD_TURNED_EMPTY);
                }
                if (mIsDeletable) mDeleteContainer.setVisibility(View.INVISIBLE);
            } else {
                if (mListener != null) {
                    mListener.onRequest(EditorListener.FIELD_TURNED_NON_EMPTY);
                }
                if (mIsDeletable) mDeleteContainer.setVisibility(View.VISIBLE);
            }
            mWasEmpty = isEmpty;
        }
    }

    protected boolean isFieldChanged(String column, String value) {
        final String dbValue = mEntry.getAsString(column);
        // nullable fields (e.g. Middle Name) are usually represented as empty columns,
        // so lets treat null and empty space equivalently here
        final String dbValueNoNull = dbValue == null ? "" : dbValue;
        final String valueNoNull = value == null ? "" : value;
        return !TextUtils.equals(dbValueNoNull, valueNoNull);
    }

    protected void rebuildValues() {
        setValues(mKind, mEntry, mState, mReadOnly, mViewIdGenerator);
    }

    /*
     * New Feature by Mediatek Begin. 
     * Original Android's code: 
     * M:AAS
     */
    protected void updateValues() {
        Log.v(TAG, "updateValues()");
        if (mKind != null && Phone.CONTENT_ITEM_TYPE.equals(mKind.mimeType)) {
            Log.e(TAG, "--Phone.CONTENT_ITEM_TYPE--");
            rebuildLabel();
        }
    }
    /*
     * New Feature by Mediatek End.
     */

    /**
     * Prepare this editor using the given {@link DataKind} for defining
     * structure and {@link ValuesDelta} describing the content to edit.
     */
    @Override
    public void setValues(DataKind kind, ValuesDelta entry, EntityDelta state, boolean readOnly,
            ViewIdGenerator vig) {
        mKind = kind;
        mEntry = entry;
        mState = state;
        mReadOnly = readOnly;
        mViewIdGenerator = vig;
        setId(vig.getId(state, kind, entry, ViewIdGenerator.NO_VIEW_INDEX));

        if (!entry.isVisible()) {
            // Hide ourselves entirely if deleted
            setVisibility(View.GONE);
            return;
        }
        setVisibility(View.VISIBLE);

        // Display label selector if multiple types available
        final boolean hasTypes = mForceHideLabel ? false : EntityModifier
                .hasEditTypes(kind);
        setupLabelButton(hasTypes);
        mLabel.setEnabled(!readOnly && isEnabled());
        if (hasTypes) {
            mType = EntityModifier.getCurrentType(entry, kind);
            Log.i(TAG,"hasType is true mType : "+mType);
            rebuildLabel();
        }
    }

    public ValuesDelta getValues() {
        return mEntry;
    }

    /**
     * Prepare dialog for entering a custom label. The input value is trimmed: white spaces before
     * and after the input text is removed.
     * <p>
     * If the final value is empty, this change request is ignored;
     * no empty text is allowed in any custom label.
     */
    private Dialog createCustomDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle(R.string.customLabelPickerTitle);

        final EditText customType = new EditText(builder.getContext());
        customType.setId(R.id.custom_dialog_content);
        customType.setInputType(INPUT_TYPE_CUSTOM);
        customType.setSaveEnabled(true);
        customType.requestFocus();

        builder.setView(customType);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String customText = customType.getText().toString().trim();
                if (ContactsUtils.isGraphic(customText)) {
                    final List<EditType> allTypes =
                            EntityModifier.getValidTypes(mState, mKind, null);
                    mType = null;
                    for (EditType editType : allTypes) {
                        if (editType.customColumn != null) {
                            mType = editType;
                            break;
                        }
                    }
                    if (mType == null) return;

                    mEntry.put(mKind.typeColumn, mType.rawValue);
                    mEntry.put(mType.customColumn, customText);
                    rebuildLabel();
                    requestFocusForFirstEditField();
                    onLabelRebuilt();
                }
            }
        });

        builder.setNegativeButton(android.R.string.cancel, null);

        return builder.create();
    }

    /**
     * Called after the label has changed (either chosen from the list or entered in the Dialog)
     */
    protected void onLabelRebuilt() {
    }

    protected void onTypeSelectionChange(int position) {
        Log.i(TAG,"position : "+position);
        EditType selected = mEditTypeAdapter.getItem(position);
        // See if the selection has in fact changed

        /*
         * New Feature by Mediatek Begin.
         * Original Android's code:
         * M:AAS
         */
        if (OperatorUtils.isAasEnabled(mKind.mAccountType)) {
            if (Phone.CONTENT_ITEM_TYPE.equals(mKind.mimeType)
                    && mEditTypeAdapter.getCount() - 1 == position) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setAction("com.android.contacts.action.EDIT_AAS");
                Log.e(TAG, "return0");
                getContext().startActivity(intent);
                return;
            }
        }
        /*
         * New Feature by Mediatek End.
         */

        if (mEditTypeAdapter.hasCustomSelection() && selected == CUSTOM_SELECTION) {
            Log.i(TAG,"return1");
            return;
        }

        if (mType == selected && mType.customColumn == null) {
            Log.i(TAG,"return2");
            return;
        }

        if (selected.customColumn != null) {
            Log.i(TAG,"return3");
            showDialog(DIALOG_ID_CUSTOM);
        } else {
            // User picked type, and we're sure it's ok to actually write the entry.
            Log.i(TAG,"mKind.mimeType : "+mKind.mimeType);
            Log.i(TAG," selected = "+selected);
            Log.i(TAG,"mType = "+mType);
            mType = selected;
            mEntry.put(mKind.typeColumn, mType.rawValue);
            rebuildLabel();
            requestFocusForFirstEditField();
            onLabelRebuilt();
        }
    }

    /* package */
    void showDialog(int bundleDialogId) {
        Bundle bundle = new Bundle();
        bundle.putInt(DIALOG_ID_KEY, bundleDialogId);
        getDialogManager().showDialogInView(this, bundle);
    }

    private DialogManager getDialogManager() {
        if (mDialogManager == null) {
            Context context = getContext();
            if (!(context instanceof DialogManager.DialogShowingViewActivity)) {
                throw new IllegalStateException(
                        "View must be hosted in an Activity that implements " +
                        "DialogManager.DialogShowingViewActivity");
            }
            mDialogManager = ((DialogManager.DialogShowingViewActivity)context).getDialogManager();
        }
        return mDialogManager;
    }

    @Override
    public Dialog createDialog(Bundle bundle) {
        if (bundle == null) throw new IllegalArgumentException("bundle must not be null");
        int dialogId = bundle.getInt(DIALOG_ID_KEY);
        switch (dialogId) {
            case DIALOG_ID_CUSTOM:
                return createCustomDialog();
            default:
                throw new IllegalArgumentException("Invalid dialogId: " + dialogId);
        }
    }

    protected abstract void requestFocusForFirstEditField();

    private class EditTypeAdapter extends ArrayAdapter<EditType> {
        private final LayoutInflater mInflater;
        private boolean mHasCustomSelection;
        private int mTextColor;

        public EditTypeAdapter(Context context) {
            super(context, 0);
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mTextColor = context.getResources().getColor(R.color.secondary_text_color);

            if (mType != null && mType.customColumn != null) {

                // Use custom label string when present
                final String customText = mEntry.getAsString(mType.customColumn);
                if (customText != null) {
                    add(CUSTOM_SELECTION);
                    mHasCustomSelection = true;
                }
            }

            addAll(EntityModifier.getValidTypes(mState, mKind, mType));
        }

        public boolean hasCustomSelection() {
            return mHasCustomSelection;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return createViewFromResource(
                    position, convertView, parent, android.R.layout.simple_spinner_item);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return createViewFromResource(
                    position, convertView, parent, android.R.layout.simple_spinner_dropdown_item);
        }

        private View createViewFromResource(int position, View convertView, ViewGroup parent,
                int resource) {
            TextView textView;

            if (convertView == null) {
                textView = (TextView) mInflater.inflate(resource, parent, false);
                textView.setAllCaps(true);
                textView.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                textView.setTextAppearance(mContext, android.R.style.TextAppearance_Small);
                textView.setTextColor(mTextColor);
                textView.setEllipsize(TruncateAt.MIDDLE);
            } else {
                textView = (TextView) convertView;
            }

            EditType type = getItem(position);
            String text = null;
            if (type == CUSTOM_SELECTION) {
                text = mEntry.getAsString(mType.customColumn);
            } else {
                /*
                 * New Feature by Mediatek Begin.
                 * Original Android's code:
                 * M:AAS
                 */
                if (OperatorUtils.isAasEnabled(mKind.mAccountType)) {
                    text = type.labelText;
                    if (text == null) {
                        text = getContext().getString(type.labelRes);
                    }
                } else {
                    /*
                     * New Feature by Mediatek End.
                     */
                    text = getContext().getString(type.labelRes);
                }
            }
            textView.setText(text);
            return textView;
        }
    }

    /*
     * New Feature by Mediatek Begin. 
     * Original Android's code:
     * M:AAS  
     */
    public void setFodrceHideLable(boolean hide) {
        mForceHideLabel = hide;
    }
    /*
     * New Feature  by Mediatek End.
    */
}
