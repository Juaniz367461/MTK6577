package com.mediatek.contacts.custom;

import java.util.List;

import com.android.contacts.R;
import com.android.contacts.editor.ContactEditorFragment;
import com.mediatek.contacts.custom.AlertDialogFragment.EditTextDialogFragment;
import com.mediatek.contacts.custom.AlertDialogFragment.EditTextDialogFragment.EditTextDoneListener;
import com.mediatek.contacts.custom.MessageAlertDialogFragment.DoneListener;
import com.mediatek.contacts.util.ContactsGroupUtils.USIMAas;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.AlphaTag;

import android.R.anim;
import android.app.ActionBar;
import android.app.Activity;
import android.content.ClipData.Item;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;


public class CustomAasActivity extends Activity {
    private static final String TAG = "CustomAasActivity";
    private static final String CREATE_AAS_TAG_DIALOG = "create_aas_tag_dialog";
    private static final String EDIT_AAS_NAME = "edit_aas_name";
    private static final String DELETE_TAG_DIALOG = "delet_tag_dialog";
    private static final String EDIT_TAG_DIALOG = "edit_tag_dialog";

    private boolean isModifying = false;
    private CustomAasAdapter mAasAdapter = null;
    private int mSlotId = 0;
    private AlphaTag mAlphaTag = null;
    private View mActionBarEdit = null;
    private TextView mSelectedView = null;
    private ToastHelper mToastHelper = null;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.custom_aas);

        mSlotId = ContactEditorFragment.getSlotId();
        ListView listView = (ListView) findViewById(R.id.custom_aas);
        mAasAdapter = new CustomAasAdapter(this, mSlotId);
        mAasAdapter.updateAlphaTags();
        listView.setAdapter(mAasAdapter);
        mToastHelper = new ToastHelper(this);
        listView.setOnItemClickListener(new ListItemClickListener());
        mSlotId = ContactEditorFragment.getSlotId();

        initActionBar();
    }

    public void initActionBar() {
        ActionBar actionBar = getActionBar();
        LayoutInflater inflate = getLayoutInflater();
        View customView = inflate.inflate(R.layout.custom_aas_action_bar, null);
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM
                | ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);

        mActionBarEdit = customView.findViewById(R.id.action_bar_edit);
        mSelectedView = (TextView) customView.findViewById(R.id.selected);
        ImageView selectedIcon = (ImageView) customView.findViewById(R.id.selected_icon);
        selectedIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(CustomAasAdapter.MODE_NORMAL);
                updateActionBar();
            }
        });
        actionBar.setCustomView(customView);

        updateActionBar();
    }

    public void updateActionBar() {
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            if (mAasAdapter.isMode(CustomAasAdapter.MODE_NORMAL)) {
                actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP,
                        ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_HOME_AS_UP);

                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setTitle(R.string.aas_custom_title);
                mActionBarEdit.setVisibility(View.GONE);
            } else {
                actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM);

                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setDisplayShowTitleEnabled(false);
                mActionBarEdit.setVisibility(View.VISIBLE);
                String select = getResources().getString(R.string.selected_item_count,
                        mAasAdapter.getCheckedItemCount());
                mSelectedView.setText(select);
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        Log.d(TAG, "onPrepareOptionsMenu");
        MenuInflater inflater = getMenuInflater();
        menu.clear();
        if (mAasAdapter.isMode(CustomAasAdapter.MODE_NORMAL)) {
            inflater.inflate(R.menu.custom_normal_menu, menu);
        } else {
            inflater.inflate(R.menu.custom_edit_menu, menu);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        Log.d(TAG, "onOptionsItemSelected");
        if (mAasAdapter.isMode(CustomAasAdapter.MODE_NORMAL)) {
            switch (item.getItemId()) {
            case R.id.aas_add:
                if (!mAasAdapter.isFull()) {
                    showNewAasDialog();
                } else {
                    mToastHelper.showToast(R.string.aas_usim_full);
                }
                break;
            case R.id.aas_edit:
                setMode(CustomAasAdapter.MODE_EDIT);
                break;
            case android.R.id.home:
                finish();
                break;
            default:
            }
        } else {
            switch (item.getItemId()) {
            case R.id.select_all:
                mAasAdapter.setAllChecked(true);
                updateActionBar();
                break;
            case R.id.disselect_all:
                mAasAdapter.setAllChecked(false);
                updateActionBar();
                break;
            case R.id.delete:
                // mAasAdapter.deleteCheckedAasTag();
                if (mAasAdapter.getCheckedItemCount() == 0) {
                    mToastHelper.showToast(R.string.multichoice_no_select_alert);
                } else {
                    showDeleteAlertDialog();
                }
                break;
            default:
                break;
            }
        }
        return true;
    }

    public void setMode(int mode) {
        mAasAdapter.setMode(mode);
        updateActionBar();
        invalidateOptionsMenu();
    }

    @Override
    public void onBackPressed() {
        if (mAasAdapter.isMode(CustomAasAdapter.MODE_EDIT)) {
            setMode(CustomAasAdapter.MODE_NORMAL);
        } else {
            super.onBackPressed();
        }
    }

    protected void showNewAasDialog() {
        EditTextDialogFragment createItemDialogFragment = EditTextDialogFragment.newInstance(
                R.string.aas_new_dialog_title, android.R.string.cancel, android.R.string.ok, "");
        createItemDialogFragment.setOnEditTextDoneListener(new NewAlpahTagListener());
        createItemDialogFragment.show(getFragmentManager(), CREATE_AAS_TAG_DIALOG);
    }

    final private class NewAlpahTagListener implements EditTextDoneListener {

        @Override
        public void onClick(String text) {
            if (mAasAdapter.isExist(text)) {
                mToastHelper.showToast(R.string.aas_name_exist);
            } else if (!isNameValid(text, mSlotId)) {
                mToastHelper.showToast(R.string.aas_name_invalid);
            } else {
                int aasIndex = USIMAas.insertUSIMAAS(mSlotId, text);
                Log.d(TAG, "insertAasTag() aasIndex = " + aasIndex);
                if (aasIndex > 0) {
                    mAasAdapter.updateAlphaTags();
                } else {
                    mToastHelper.showToast(R.string.aas_new_fail);
                }
            }
        }
    }

    protected void showEditAasDialog(AlphaTag alphaTag) {
        if (alphaTag == null) {
            Log.e(TAG, "showEditAasDialog(): alphaTag is null,");
            return;
        }
        final String text = alphaTag.getAlphaTag();
        EditTextDialogFragment editDialogFragment = EditTextDialogFragment.newInstance(
                R.string.ass_rename_dialog_title, android.R.string.cancel, android.R.string.ok, text);
        editDialogFragment.setOnEditTextDoneListener(new EditAlpahTagListener(alphaTag));
        editDialogFragment.show(getFragmentManager(), EDIT_AAS_NAME);
    }

    final private class EditAlpahTagListener implements EditTextDoneListener {
        private AlphaTag mAlphaTag;

        public EditAlpahTagListener(AlphaTag alphaTag) {
            mAlphaTag = alphaTag;
        }

        @Override
        public void onClick(String text) {
            if (mAlphaTag.getAlphaTag().equals(text)) {
                Log.d(TAG, "mAlphaTag.getAlphaTag()==text : " + text);
                return;
            }
            if (mAasAdapter.isExist(text)) {
                mToastHelper.showToast(R.string.aas_name_exist);
            } else if (!isNameValid(text, mSlotId)) {
                mToastHelper.showToast(R.string.aas_name_invalid);
            } else {
                showEditAssertDialog(mAlphaTag, text);
            }
        }
    }

    private void showEditAssertDialog(AlphaTag alphaTag, String targetName) {
        MessageAlertDialogFragment editAssertDialogFragment = MessageAlertDialogFragment
                .newInstance(android.R.string.dialog_alert_title, R.string.ass_edit_assert_message,
                        true, targetName);
        editAssertDialogFragment.setDeleteDoneListener(new EditAssertListener(alphaTag));
        editAssertDialogFragment.show(getFragmentManager(), EDIT_TAG_DIALOG);
    }

    final private class EditAssertListener implements DoneListener {
        private AlphaTag mAlphaTag = null;

        public EditAssertListener(AlphaTag alphaTag) {
            mAlphaTag = alphaTag;
        }

        @Override
        public void onClick(String text) {
            boolean flag = USIMAas.updateUSIMAAS(mSlotId, mAlphaTag.getRecordIndex(), mAlphaTag
                    .getPbrIndex(), text);
            if (flag) {
                mAasAdapter.updateAlphaTags();
            } else {
                String msg = getResources().getString(R.string.aas_edit_fail,
                        mAlphaTag.getAlphaTag());
                mToastHelper.showToast(msg);
            }
        }
    }

    protected void showDeleteAlertDialog() {
        MessageAlertDialogFragment deleteDialogFragment = MessageAlertDialogFragment.newInstance(
                android.R.string.dialog_alert_title, R.string.aas_delele_dialog_message, true, "");
        deleteDialogFragment.setDeleteDoneListener(new DeletionListener());
        deleteDialogFragment.show(getFragmentManager(), DELETE_TAG_DIALOG);
    }

    final private class DeletionListener implements DoneListener {
        @Override
        public void onClick(String text) {
            Log.d(TAG, "DeletionListener");
            mAasAdapter.deleteCheckedAasTag();
            setMode(CustomAasAdapter.MODE_NORMAL);
        }
    }

    public class ListItemClickListener implements OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> view, View v, int pos, long arg3) {
            if (mAasAdapter.isMode(CustomAasAdapter.MODE_NORMAL)) {
                showEditAasDialog(mAasAdapter.getItem(pos).mAlphaTag);
            } else {
                mAasAdapter.updateChecked(pos);
                invalidateOptionsMenu();
                updateActionBar();
            }
        }
    }

    private boolean isNameValid(String text, int slot) {
        if(TextUtils.isEmpty(text)){
            return false;
        }
        final int MAX = USIMAas.getUSIMAASMaxNameLen(slot);
        try {
            GsmAlphabet.stringToGsm7BitPacked(text);
            if (text.length() > MAX) {
                return false;
            }
        } catch (EncodeException e) {
            if (text.length() > ((MAX - 2) >> 1)) {
                return false;
            }
        }
        return true;
    }
}
