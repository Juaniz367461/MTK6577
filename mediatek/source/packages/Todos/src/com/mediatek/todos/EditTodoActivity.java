/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

package com.mediatek.todos;

import com.mediatek.todos.TimeChangeReceiver.TimeChangeListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.DialogInterface;
import android.content.EntityIterator;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.Time;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


public class EditTodoActivity extends Activity implements TimeChangeListener, QueryListener {
    private static final String TAG = "EditTodoActivity";

    private EditTodoListeners mListeners = new EditTodoListeners();
    private TodoAsyncQuery mAsyncQuery = null;
    private static final int DIALOG_DELETE_ITEMS = 1;
    private static final int DIALOG_BACK_MAIN_PAGE = 2;
    private static final int DIALOG_CANCEL_EDIT = 3;

    private static final int STATE_NULL = -1; 
    /** state to add a new TodoInfo */
    private static final int STATE_ADD_NEW = 0;
    /** state showing a TodoInfo's details */
    private static final int STATE_SHOW_DETAILS = STATE_ADD_NEW + 1;
    /** state to edit a TodoInfo */
    private static final int STATE_EDIT_TODO = STATE_SHOW_DETAILS + 1;
    
    /** Values {@link STATE_ADD_NEW}, {@link STATE_SHOW_DETAILS}, {@link STATE_EDIT_TODO} */
    private int mState = STATE_NULL;

    private int mOperatorCode = Utils.OPERATOR_NONE;

    // The all components in this activity.
    private ImageView mImgViewBack;
    private TextView mTextDetailsName;
    private ImageButton mImgBtnNewTodo;
    private TodoEditText mTodoTitle;
    private TodoEditText mTodoDescription;
    private LinearLayout mSetDueDate;
    private ImageView mDateIcon;
    private TextView mDateText;
    private ImageButton mImgBtnDueDateRemove;
    private RelativeLayout mLayoutDetailsBottom;
    private ImageButton mImgBtnChangeStatus;
    private LinearLayout mLayoutCancelDone;

    // use to backUp the data passed from other activity.
    private TodoInfo mDataOriginal = new TodoInfo();
    // use to preserve the passed data that after modified,
    private TodoInfo mDataAfterModify = new TodoInfo();

    // DatePicker show range.2037/12/31
    private static final int MIN_YEAR = 1970;
    private static final int MAX_YEAR = 2036;
    private static final int MAX_MONTH = 11;// index 0-11
    private static final int MAX_DAY = 31;
    private long mMaxTime = 0;

    // use to receive system date change broasdcasr
    private TimeChangeReceiver mTimeChangeReceiver = null;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.item_details);
        initViews();
        mTimeChangeReceiver = TimeChangeReceiver.registTimeChangeReceiver(this);
        mTimeChangeReceiver.addDateChangeListener(this);

        mAsyncQuery = TodoAsyncQuery.getInstatnce(getApplicationContext());
        Time maxTime = new Time();
        maxTime.set(MAX_DAY, MAX_MONTH, MAX_YEAR);
        mMaxTime = maxTime.toMillis(false);

        LogUtils.d(TAG, "onCreate() finshed.");
    }

    /**
     * init all components used in this activity
     */
    private void initViews() {
        mImgViewBack = (ImageView) findViewById(R.id.back);
        mTextDetailsName = (TextView) findViewById(R.id.details_name);
        mImgBtnNewTodo = (ImageButton) findViewById(R.id.btn_new_todo);
        mImgViewBack.setOnClickListener(mListeners);
        mTextDetailsName.setOnClickListener(mListeners);
        mImgBtnNewTodo.setOnClickListener(mListeners);

        mTodoTitle = (TodoEditText) findViewById(R.id.title);
        mTodoTitle.setOnClickListener(mListeners);
        final int titleMaxLength = getResources().getInteger(R.integer.ToDoTitleMaxLength);
        mTodoTitle.setMaxLength(titleMaxLength);
        mTodoDescription = (TodoEditText) findViewById(R.id.details);
        mTodoDescription.setOnClickListener(mListeners);
        final int descMaxLength = getResources().getInteger(R.integer.ToDoDescriptionMaxLength);
        mTodoDescription.setMaxLength(descMaxLength);
        mSetDueDate = (LinearLayout) findViewById(R.id.set_due_date);
        mDateIcon = (ImageView) findViewById(R.id.date_icon);
        mDateText = (TextView) findViewById(R.id.date_text);
        mImgBtnDueDateRemove = (ImageButton) findViewById(R.id.btn_due_date_remove);
        mSetDueDate.setOnClickListener(mListeners);
        mImgBtnDueDateRemove.setOnClickListener(mListeners);

        mLayoutDetailsBottom = (RelativeLayout) findViewById(R.id.details_bottom);
        mImgBtnChangeStatus = (ImageButton) findViewById(R.id.btn_change_done_todo_status);
        ImageButton btnDelete = (ImageButton) findViewById(R.id.btn_delete);
        mImgBtnChangeStatus.setOnClickListener(mListeners);
        btnDelete.setOnClickListener(mListeners);

        mLayoutCancelDone = (LinearLayout) findViewById(R.id.cancel_done);
        Button btnCancel = (Button) findViewById(R.id.btn_cancel);
        Button btnDone = (Button) findViewById(R.id.btn_done);
        btnCancel.setOnClickListener(mListeners);
        btnDone.setOnClickListener(mListeners);
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.d(TAG, "onResume mState=" + mState);

        if (mState == STATE_NULL) {
            Bundle intentBundle = getIntent().getExtras();
            if (intentBundle != null) {
                TodoInfo data = (TodoInfo) intentBundle.get(Utils.KEY_PASSED_DATA);
                mDataOriginal.copy(data);
                mState = STATE_SHOW_DETAILS;
            } else {
                mState = STATE_ADD_NEW;
            }
        }
        changeToState(mState);
        LogUtils.d(TAG, "onResume() finshed. mState="+mState);
    }
    
    public void onPause() {
        LogUtils.d(TAG, "onPause() save data.");
        // if home->reopen quickly, it will not call onSaveInstanceState(), some data may lost.
        // So save data when pause activity.
        switch (mState) {
        case STATE_ADD_NEW:
            final String title = mTodoTitle.getText().toString();
            final String description = mTodoDescription.getText().toString();
            mDataAfterModify.setTitle(title);
            mDataAfterModify.setDescription(description);
            break;
        case STATE_SHOW_DETAILS:
            break;
        case STATE_EDIT_TODO:
            final String modifyTitle = mTodoTitle.getText().toString();
            final String modifyDescription = mTodoDescription.getText().toString();
            mDataAfterModify.setTitle(modifyTitle);
            mDataAfterModify.setDescription(modifyDescription);
            break;
        }
        super.onPause();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        
        mState = savedInstanceState.getInt("state", -1);
        switch(mState){
        case STATE_ADD_NEW:
            TodoInfo newInfo = (TodoInfo) savedInstanceState.getSerializable("modify");
            mDataAfterModify.copy(newInfo);
            break;
        case STATE_SHOW_DETAILS:
            TodoInfo detailsInfo = (TodoInfo) savedInstanceState.getSerializable("original");
            mDataOriginal.copy(detailsInfo);
            break;
        case STATE_EDIT_TODO:
            TodoInfo originalInfo = (TodoInfo) savedInstanceState.getSerializable("original");
            TodoInfo editInfo = (TodoInfo) savedInstanceState.getSerializable("modify");
            mDataOriginal.copy(originalInfo);
            mDataAfterModify.copy(editInfo);
            break;
        }
        LogUtils.w(TAG, "onRestoreInstanceState()-mState=" + mState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        LogUtils.w(TAG, "onSaveInstanceState()-mState=" + mState);
        outState.putInt("state", mState);
        switch(mState){
        case STATE_ADD_NEW:
            final String title = mTodoTitle.getText().toString();
            final String description = mTodoDescription.getText().toString();
            mDataAfterModify.setTitle(title);
            mDataAfterModify.setDescription(description);
            outState.putSerializable("modify", mDataAfterModify);
            break;
        case STATE_SHOW_DETAILS:
            outState.putSerializable("original", mDataOriginal);
            break;
        case STATE_EDIT_TODO:
            final String modifyTitle = mTodoTitle.getText().toString();
            final String modifyDescription = mTodoDescription.getText().toString();
            mDataAfterModify.setTitle(modifyTitle);
            mDataAfterModify.setDescription(modifyDescription);
            outState.putSerializable("original", mDataOriginal);
            outState.putSerializable("modify", mDataAfterModify);
            break;
        }
        super.onSaveInstanceState(outState);
    }
    
    /**
     * Change Activity's State.
     * @param state
     */
    public void changeToState(int state) {
        mState = state;
        mOperatorCode = Utils.OPERATOR_NONE;
        switch (mState) {
        case STATE_ADD_NEW:
        case STATE_EDIT_TODO:
            int headerId = 0;
            if (mState == STATE_ADD_NEW) {
                mOperatorCode = Utils.OPERATOR_INSERT;
                headerId = R.string.add_new_todo;
            } else {
                mOperatorCode = Utils.OPERATOR_UPDATE;
                headerId = R.string.edit_todo;
            }
            mTextDetailsName.setText(headerId);
            mImgBtnNewTodo.setVisibility(View.INVISIBLE);

            mTodoTitle.setEnabled(true);
            mTodoTitle.setClickable(true);
            mTodoTitle.setLongClickable(true);
            mTodoTitle.setText(mDataAfterModify.getTitle());
            mTodoTitle.setHint(R.string.hint_title);
            mTodoTitle.setLongClickable(true);

            mTodoDescription.setEnabled(true);
            mTodoDescription.setClickable(true);
            mTodoDescription.setLongClickable(true);
            mTodoDescription.setText(mDataAfterModify.getDescription());
            mTodoDescription.setHint(R.string.hint_details);
            mTodoDescription.setLongClickable(true);

            mSetDueDate.setEnabled(true);
            mSetDueDate.setClickable(true);

            int expireInt = getExpireCode(mDataAfterModify);
            if (expireInt == Utils.DATE_NO_EXPIRE) { // not set due date
                mDateIcon.setImageResource(R.drawable.ic_unexpire);
                mImgBtnDueDateRemove.setVisibility(View.INVISIBLE);
            } else if (expireInt == Utils.DATE_NOT_EXPIRE) { // not expired
                mDateIcon.setImageResource(R.drawable.ic_unexpire);
                mImgBtnDueDateRemove.setVisibility(View.VISIBLE);
            } else { // expired
                mDateIcon.setImageResource(R.drawable.ic_expired);
                mImgBtnDueDateRemove.setVisibility(View.VISIBLE);
            }
            mDateText.setText(Utils.getDateText(this, mDataAfterModify.getDueDate(),
                    Utils.DATE_TYPE_DUE));

            mLayoutDetailsBottom.setVisibility(View.GONE);
            mLayoutCancelDone.setVisibility(View.VISIBLE);
            break;
        case STATE_SHOW_DETAILS:
            boolean isNotDone = !TodoInfo.STATUS_DONE.equals(mDataOriginal.getStatus());
            final int detailsHeaderId = (isNotDone ? R.string.todo_detail : R.string.done_detail);
            mTextDetailsName.setText(detailsHeaderId);
            mImgBtnNewTodo.setVisibility(View.VISIBLE);

            mTodoTitle.setEnabled(isNotDone);
            mTodoTitle.setClickable(isNotDone);
            mTodoTitle.setLongClickable(false);
            mTodoTitle.clearFocus();
            mTodoTitle.setText(mDataOriginal.getTitle());
            mTodoTitle.setHint(null);

            mTodoDescription.setEnabled(isNotDone);
            mTodoDescription.setClickable(isNotDone);
            mTodoDescription.setLongClickable(false);
            mTodoDescription.clearFocus();
            mTodoDescription.setText(mDataOriginal.getDescription());
            mTodoDescription.setHint(null);

            mSetDueDate.setEnabled(isNotDone);
            mSetDueDate.setClickable(isNotDone);
            mImgBtnDueDateRemove.setVisibility(View.INVISIBLE);
            if (isNotDone) {
                final int expireCode = getExpireCode(mDataOriginal);
                if (expireCode == Utils.DATE_NO_EXPIRE || expireCode == Utils.DATE_NOT_EXPIRE) {
                    mDateIcon.setImageResource(R.drawable.ic_unexpire);
                } else {
                    mDateIcon.setImageResource(R.drawable.ic_expired);
                }
                mDateText.setText(Utils.getDateText(this, mDataOriginal.getDueDate(),
                        Utils.DATE_TYPE_DUE));
                mImgBtnChangeStatus.setImageResource(R.drawable.todo_mark_todo);
            } else {
                mDateIcon.setImageResource(R.drawable.ic_done);
                mDateText.setText(Utils.getDateText(this, mDataOriginal.getCompleteTime(),
                        Utils.DATE_TYPE_COMPLETE));
                mImgBtnChangeStatus.setImageResource(R.drawable.todo_mark_done);
            }

            mLayoutDetailsBottom.setVisibility(View.VISIBLE);
            mLayoutDetailsBottom.requestFocus();
            mLayoutCancelDone.setVisibility(View.GONE);
            break;
        default:
            LogUtils.w(TAG, "changeToState : " + mState + ", no this state.");
            break;
        }
    }

    /**
     * return expire code.
     * 
     * @return -1 no expire time. 0 expired. 1 not expire.
     */
    private int getExpireCode(TodoInfo info) {
        if (info != null) {
            if (info.getDueDate() <= 0) {
                return Utils.DATE_NO_EXPIRE;
            }
            if (info.isExpire()) {
                return Utils.DATE_EXPIRED;
            }
        }
        return Utils.DATE_NOT_EXPIRE;
    }

    @Override
    public void onBackPressed() {
        checkAndSaveQuit();
    }

    /**
     * save the modify data and quite
     */
    private void checkAndSaveQuit() {
        hideIMEFromWindow();

        final String title = mTodoTitle.getText().toString();
        final String description = mTodoDescription.getText().toString();
        switch (mOperatorCode) {
        case Utils.OPERATOR_INSERT:
            if (TextUtils.isEmpty(title) && TextUtils.isEmpty(description)) {
                quiteWithOutSave();
            } else {
                mDataAfterModify.setTitle(title);
                mDataAfterModify.setDescription(description);
                startInsert(mDataAfterModify);
            }
            break;
        case Utils.OPERATOR_UPDATE:
            if (TextUtils.isEmpty(title) && TextUtils.isEmpty(description)) {
                quiteWithOutSave();
            } else {
                mDataAfterModify.setTitle(title);
                mDataAfterModify.setDescription(description);
                if (mDataAfterModify.equals(mDataOriginal)) {
                    quiteWithOutSave();
                } else {
                    startUpdate(mDataAfterModify);
                }
            }
            break;
        case Utils.OPERATOR_DELETE:
            startDelete(mDataOriginal);
            break;
        default:
            quiteWithOutSave();
            break;
        }
    }

    /**
     * quite without save the mofify data.
     */
    private void quiteWithOutSave() {
        quit(Utils.OPERATOR_NONE);
    }

    /**
     * quite function
     * 
     * @param operatorCode
     */
    private void quit(int operatorCode) {
        LogUtils.d(TAG, "quit() operatorCode=" + operatorCode);
        Intent intent = new Intent();
        if (operatorCode == Utils.OPERATOR_INSERT) {
            intent.putExtra(Utils.KEY_PASSED_DATA, mDataAfterModify);
        } else if (operatorCode == Utils.OPERATOR_DELETE) {
            intent.putExtra(Utils.KEY_PASSED_DATA, mDataOriginal);
        } else if (operatorCode == Utils.OPERATOR_UPDATE) {
            intent.putExtra(Utils.KEY_PASSED_DATA, mDataAfterModify);
        }
        setResult(operatorCode, intent);
        finish();
    }

    private class EditTodoListeners implements View.OnClickListener, OnDateSetListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
            case R.id.btn_new_todo:
                onClickNewTodo();
                break;
            case R.id.title:
            case R.id.details:
                onClickEditText();
                break;
            case R.id.set_due_date:
                onClickSetDueDate();
                break;
            case R.id.btn_due_date_remove:
                onClickRemoveDueDate();
                break;
            case R.id.btn_cancel:
                onClickBtnCancel();
                break;
            case R.id.back:
            case R.id.btn_done:
                checkAndSaveQuit();
                break;
            case R.id.btn_change_done_todo_status:
                onClickChangeTodoState();
                break;
            case R.id.btn_delete:
                showDialog(DIALOG_DELETE_ITEMS);
                break;
            default:
                LogUtils.w(TAG, "Ingore the click. View = " + v);
            }
        }

        /**
         * Due date datepicker value changed listener.
         */
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            if (year > MAX_YEAR || year < MIN_YEAR) {
                Utils.prompt(EditTodoActivity.this, R.string.valid_date_range);
                LogUtils.w(TAG, "year < 1970 or year > 2037.Cannot be set.");
                return;
            }

            Time dueDay = new Time();
            dueDay.set(Utils.DUE_DATE_SECOND, Utils.DUE_DATE_MINUTE, Utils.DUE_DATE_HOUR,
                    dayOfMonth, monthOfYear, year);
            long dueDayMillis = dueDay.normalize(true);

            Time currentTime = new Time();
            currentTime.setToNow();
            if (Utils.dateCompare(dueDay, currentTime) < 0) {
                Utils.prompt(EditTodoActivity.this, R.string.prompt_date_before_today);
                LogUtils.w(TAG, "Date can't before today");
            } else {
                String dueDate = Utils.getDateText(EditTodoActivity.this, dueDayMillis,
                        Utils.DATE_TYPE_DUE);
                mDateText.setText(dueDate);
                mDataAfterModify.setDueDay(dueDayMillis);
                mImgBtnDueDateRemove.setVisibility(View.VISIBLE);
                if (mDataAfterModify.isExpire()) {
                    mDateIcon.setImageResource(R.drawable.ic_expired);
                } else {
                    mDateIcon.setImageResource(R.drawable.ic_unexpire);
                }
            }
            currentTime = null;
        }
    }

    public void onClickNewTodo() {
        changeToState(STATE_ADD_NEW);
    }

    public void onClickEditText() {
        if (mState == STATE_SHOW_DETAILS & !TodoInfo.STATUS_DONE.equals(mDataOriginal.getStatus())) {
            mDataAfterModify.copy(mDataOriginal);
            changeToState(STATE_EDIT_TODO);
        }
    }

    public void onClickSetDueDate() {
        hideIMEFromWindow();

        if (mState == STATE_SHOW_DETAILS) {
            mDataAfterModify.copy(mDataOriginal);
            changeToState(STATE_EDIT_TODO);
        }
        showDatePicker();
    }

    /**
     * click to show date picker
     */
    private void showDatePicker() {
        Time time = new Time();
        if (0 == mDataAfterModify.getDueDate()) {
            time.setToNow();
        } else {
            time.set(mDataAfterModify.getDueDate());
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, mListeners, time.year,
                time.month, time.monthDay);
        DatePicker datePicker = datePickerDialog.getDatePicker();
        datePicker.setMinDate(0);
        datePicker.setMaxDate(mMaxTime);
        datePicker.updateDate(time.year, time.month, time.monthDay);

        datePickerDialog.show();
    }

    public void onClickRemoveDueDate() {
        mDataAfterModify.setDueDay(0);
        mDateText.setText(R.string.no_expire_date);
        mDateIcon.setImageResource(R.drawable.ic_unexpire);
        mImgBtnDueDateRemove.setVisibility(View.INVISIBLE);
    }

    public void onClickBtnCancel() {
        hideIMEFromWindow();

        final String title = mTodoTitle.getText().toString();
        final String description = mTodoDescription.getText().toString();
        switch (mState) {
        case STATE_ADD_NEW:
            if (TextUtils.isEmpty(title) && TextUtils.isEmpty(description)) {
                quiteWithOutSave();
            } else {
                showDialog(DIALOG_BACK_MAIN_PAGE);
            }
            break;
        case STATE_EDIT_TODO:
            final String originalTitle = mDataOriginal.getTitle();
            final String originalDescription = mDataOriginal.getDescription();
            if (originalTitle.equals(title) && originalDescription.equals(description)) {
                mDataAfterModify.clear();
                changeToState(STATE_SHOW_DETAILS);
            } else {
                showDialog(DIALOG_CANCEL_EDIT);
            }
            break;
        default:
            break;
        }
    }

    private void hideIMEFromWindow() {
        LogUtils.d(TAG, "call InputMethodManager.hideSoftInputFromWindow()");
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mTodoDescription.getWindowToken(), 0);
    }

    public void onClickChangeTodoState() {
        mDataAfterModify.copy(mDataOriginal);
        final String status = TodoInfo.STATUS_DONE.equals(mDataAfterModify.getStatus()) ? TodoInfo.STATUS_TODO
                : TodoInfo.STATUS_DONE;
        mDataAfterModify.updateStatus(status);
        mOperatorCode = Utils.OPERATOR_UPDATE;
        checkAndSaveQuit();
    }

    /**
     * when chagne view,should clear data content.
     */
    private void clearData() {
        mDataAfterModify = new TodoInfo();
        mDataOriginal = new TodoInfo();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog dialog = null;
        switch (id) {
        case DIALOG_DELETE_ITEMS:
            dialog = new AlertDialog.Builder(EditTodoActivity.this).setTitle(R.string.delete)
                    .setMessage(R.string.delete_item)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setNegativeButton(R.string.cancel, null).create();
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, getText(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            mOperatorCode = Utils.OPERATOR_DELETE;
                            checkAndSaveQuit();
                        }
                    });
            return dialog;

        case DIALOG_BACK_MAIN_PAGE:
            dialog = new AlertDialog.Builder(EditTodoActivity.this).setTitle(R.string.back_title)
                    .setMessage(R.string.back_message)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setNegativeButton(R.string.cancel, null).create();
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, getText(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            quiteWithOutSave();
                        }
                    });
            return dialog;

        case DIALOG_CANCEL_EDIT:
            dialog = new AlertDialog.Builder(EditTodoActivity.this).setTitle(R.string.back_title)
                    .setMessage(R.string.back_message)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setNegativeButton(R.string.cancel, null).create();
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, EditTodoActivity.this
                    .getText(R.string.ok), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mDataAfterModify.clear();
                    changeToState(STATE_SHOW_DETAILS);
                }
            });
            return dialog;
        default:
            LogUtils.w(TAG, "onCreateDialog,the id is not match!");
            break;
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onDestroy() {
        mAsyncQuery = null;
        mTimeChangeReceiver.clearChangeListener();
        unregisterReceiver(mTimeChangeReceiver);
        super.onDestroy();
    }

    /**
     * implements TimeChangeListener. Callback when set system date.
     */
    @Override
    public void onDateChange() {
        LogUtils.d(TAG, "onDateChnge isExpire:" + mDataAfterModify.isExpire());
        switch (mState) {
        case STATE_SHOW_DETAILS:
            if (!TodoInfo.STATUS_DONE.equals(mDataOriginal.getStatus())) {
                if (mDataOriginal.isExpire()) {
                    mDateIcon.setImageResource(R.drawable.ic_expired);
                } else {
                    mDateIcon.setImageResource(R.drawable.ic_unexpire);
                }
            }
            break;
        default:
            if (!TodoInfo.STATUS_DONE.equals(mDataAfterModify.getStatus())) {
                if (mDataAfterModify.isExpire()) {
                    mDateIcon.setImageResource(R.drawable.ic_expired);
                } else {
                    mDateIcon.setImageResource(R.drawable.ic_unexpire);
                }
            }
            break;
        }
    }

    /**
     * implements TimeChangeListener. Callback for system date change(called per minute).
     */
    @Override
    public void onTimePick() {
        switch (mState) {
        case STATE_SHOW_DETAILS:
            if (!TodoInfo.STATUS_DONE.equals(mDataOriginal.getStatus())) {
                if (mDataOriginal.isExpire()) {
                    mDateIcon.setImageResource(R.drawable.ic_expired);
                } else {
                    mDateIcon.setImageResource(R.drawable.ic_unexpire);
                }
            }
            break;
        default:
            if (!TodoInfo.STATUS_DONE.equals(mDataAfterModify.getStatus())) {
                if (mDataAfterModify.isExpire()) {
                    mDateIcon.setImageResource(R.drawable.ic_expired);
                } else {
                    mDateIcon.setImageResource(R.drawable.ic_unexpire);
                }
            }
            break;
        }
    }

    public void onDeleteComplete(int token, int result) {
        LogUtils.d(TAG, "onDeleteComplete(). ReQuery the insert TodoInfo. result=" + result);
        if (result <= 0) {
            Utils.prompt(EditTodoActivity.this, R.string.operator_failed);
            quiteWithOutSave();
            return;
        }
        quit(Utils.OPERATOR_DELETE);
    }

    public void onInsertComplete(int token, Uri uri) {
        LogUtils.d(TAG, "onInsertComplete(). reQuery the insert TodoInfo.");
        mAsyncQuery.startQuery(token, this, uri, null, null, null, null);
    }

    public void onQueryComplete(int token, Cursor cursor) {
        LogUtils.d(TAG, "onQueryComplete().");
        if (cursor == null || (!cursor.moveToFirst())) {
            Utils.prompt(EditTodoActivity.this, R.string.operator_failed);
            LogUtils.d(TAG, "onQueryComplete,cursor is empty!");
            quiteWithOutSave();
            return;
        }
        mDataAfterModify = TodoInfo.makeTodoInfoFromCursor(cursor);
        quit(Utils.OPERATOR_INSERT);
    }

    public void onUpdateComplete(int token, int result) {
        LogUtils.d(TAG, "onUpdateComplete(). result="+result);
        if (result <= 0) {
            LogUtils.d(TAG, "update failed");
            Utils.prompt(EditTodoActivity.this, R.string.operator_failed);
            quiteWithOutSave();
            return;
        }
        quit(Utils.OPERATOR_UPDATE);
    }

    public void startDelete(TodoInfo info) {
        LogUtils.d(TAG, "startDelete().");
        Utils.writeAdapterDataToDB(this, info, mAsyncQuery, mOperatorCode);
    }

    public void startInsert(TodoInfo info) {
        LogUtils.d(TAG, "startInsert().");
        Utils.writeAdapterDataToDB(this, info, mAsyncQuery, mOperatorCode);
    }

    public void startQuery(String selection) {

    }

    public void startUpdate(TodoInfo info) {
        LogUtils.d(TAG, "startUpdate()");
        Utils.writeAdapterDataToDB(this, info, mAsyncQuery, mOperatorCode);
    }
}
