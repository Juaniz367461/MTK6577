/*
 * Copyright (C) 2008 Esmertec AG.
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.mms.ui;

import com.android.mms.MmsConfig;
import com.android.mms.data.Contact;
import com.android.mms.data.ContactList;

import android.content.Context;
import android.provider.Telephony.Mms;
import android.telephony.PhoneNumberUtils;
import android.text.Annotation;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.MotionEvent;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.MultiAutoCompleteTextView;

import java.util.ArrayList;
import java.util.List;
// a0
import android.view.inputmethod.InputMethodManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AutoCompleteTextView;
//import android.widget.RecipientsView;
import android.util.Log;
import com.mediatek.xlog.Xlog;
// a1

/**
 * Provide UI for editing the recipients of multi-media messages.
 */
public class RecipientsEditor extends MultiAutoCompleteTextView {
// a0
    private static final String TAG = "Mms/RecipientsEditor";
// a1
    private int mLongPressedPosition = -1;
    private final RecipientsEditorTokenizer mTokenizer;
    private char mLastSeparator = ',';

    public RecipientsEditor(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.autoCompleteTextViewStyle);
        setDropDownWidth(LayoutParams.MATCH_PARENT);
        mTokenizer = new RecipientsEditorTokenizer(context, this);
        setTokenizer(mTokenizer);
        // For the focus to move to the message body when soft Next is pressed
        setImeOptions(EditorInfo.IME_ACTION_NEXT);

        /*
         * The point of this TextWatcher is that when the user chooses
         * an address completion from the AutoCompleteTextView menu, it
         * is marked up with Annotation objects to tie it back to the
         * address book entry that it came from.  If the user then goes
         * back and edits that part of the text, it no longer corresponds
         * to that address book entry and needs to have the Annotations
         * claiming that it does removed.
         */
        addTextChangedListener(new TextWatcher() {
            private Annotation[] mAffected;

            public void beforeTextChanged(CharSequence s, int start,
                    int count, int after) {
                mAffected = ((Spanned) s).getSpans(start, start + count,
                        Annotation.class);
            }

            public void onTextChanged(CharSequence s, int start,
                    int before, int after) {
                if (before == 0 && after == 1) {    // inserting a character
                    char c = s.charAt(start);
                    if (c == ',' || c == ';') {
                        // Remember the delimiter the user typed to end this recipient. We'll
                        // need it shortly in terminateToken().
                        mLastSeparator = c;
                    }
                }
            }

            public void afterTextChanged(Editable s) {
                if (mAffected != null) {
                    for (Annotation a : mAffected) {
                        s.removeSpan(a);
                    }
                }
                mAffected = null;
            }
        });
    }

    @Override
    public boolean enoughToFilter() {
        if (!super.enoughToFilter()) {
            return false;
        }
        // If the user is in the middle of editing an existing recipient, don't offer the
        // auto-complete menu. Without this, when the user selects an auto-complete menu item,
        // it will get added to the list of recipients so we end up with the old before-editing
        // recipient and the new post-editing recipient. As a precedent, gmail does not show
        // the auto-complete menu when editing an existing recipient.
        int end = getSelectionEnd();
        int len = getText().length();

        return end == len;

    }

    public int getRecipientCount() {
// m0
//        return mTokenizer.getNumbers().size();
        List<String> numberList = mTokenizer.getNumbers();
        return numberList.isEmpty()? 0: numberList.size();
// m1
    }

    public List<String> getNumbers() {
        return mTokenizer.getNumbers();
    }

    public ContactList constructContactsFromInput(boolean blocking) {
        List<String> numbers = mTokenizer.getNumbers();
        ContactList list = new ContactList();
        for (String number : numbers) {
            Contact contact = Contact.get(number, blocking);
            contact.setNumber(number);
            list.add(contact);
        }
        return list;
    }

    private boolean isValidAddress(String number, boolean isMms) {
        if (isMms) {
            return MessageUtils.isValidMmsAddress(number);
        } else {
            // TODO: PhoneNumberUtils.isWellFormedSmsAddress() only check if the number is a valid
            // GSM SMS address. If the address contains a dialable char, it considers it a well
            // formed SMS addr. CDMA doesn't work that way and has a different parser for SMS
            // address (see CdmaSmsAddress.parse(String address)). We should definitely fix this!!!
            return MessageUtils.isWellFormedSmsAddress(number.replaceAll(" |-", ""))
                    || Mms.isEmailAddress(number);
        }
    }

    public boolean hasValidRecipient(boolean isMms) {
        for (String number : mTokenizer.getNumbers()) {
            if (isValidAddress(number, isMms))
                return true;
        }
        return false;
    }

    public boolean hasInvalidRecipient(boolean isMms) {
        for (String number : mTokenizer.getNumbers()) {
            if (!isValidAddress(number, isMms)) {
                if (MmsConfig.getEmailGateway() == null) {
                    return true;
                } else if (!MessageUtils.isAlias(number)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String formatInvalidNumbers(boolean isMms) {
        StringBuilder sb = new StringBuilder();
        for (String number : mTokenizer.getNumbers()) {
            if (!isValidAddress(number, isMms)) {
                if (sb.length() != 0) {
                    sb.append(", ");
                }
                sb.append(number);
            }
        }
        return sb.toString();
    }

    public boolean containsEmail() {
        if (TextUtils.indexOf(getText(), '@') == -1)
            return false;

        List<String> numbers = mTokenizer.getNumbers();
        for (String number : numbers) {
            if (Mms.isEmailAddress(number))
                return true;
        }
        return false;
    }

    public static CharSequence contactToToken(Contact c) {
// a0
        Xlog.v(TAG, "RecipientsEditor:contactToToken, contact.getNameAndNumber=" + c.getNameAndNumber());
// a1
        SpannableString s = new SpannableString(c.getNameAndNumber());
        int len = s.length();

        if (len == 0) {
            return s;
        }

        s.setSpan(new Annotation("number", c.getNumber()), 0, len,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return s;
    }

    public void populate(ContactList list) {
        SpannableStringBuilder sb = new SpannableStringBuilder();

        // Very tricky bug. In the recipient editor, we always leave a trailing
        // comma to make it easy for users to add additional recipients. When a
        // user types (or chooses from the dropdown) a new contact Mms has never
        // seen before, the contact gets the correct trailing comma. But when the
        // contact gets added to the mms's contacts table, contacts sends out an
        // onUpdate to CMA. CMA would recompute the recipients and since the
        // recipient editor was still visible, call mRecipientsEditor.populate(recipients).
        // This would replace the recipient that had a comma with a recipient
        // without a comma. When a user manually added a new comma to add another
        // recipient, this would eliminate the span inside the text. The span contains the
        // number part of "Fred Flinstone <123-1231>". Hence, the whole
        // "Fred Flinstone <123-1231>" would be considered the number of
        // the first recipient and get entered into the canonical_addresses table.
        // The fix for this particular problem is very easy. All recipients have commas.
        // TODO: However, the root problem remains. If a user enters the recipients editor
        // and deletes chars into an address chosen from the suggestions, it'll cause
        // the number annotation to get deleted and the whole address (name + number) will
        // be used as the number.
        for (Contact c : list) {
            sb.append(contactToToken(c)).append(", ");
        }

        setText(sb);
    }

    private int pointToPosition(int x, int y) {
        x -= getCompoundPaddingLeft();
        y -= getExtendedPaddingTop();

        x += getScrollX();
        y += getScrollY();

        Layout layout = getLayout();
        if (layout == null) {
            return -1;
        }

        int line = layout.getLineForVertical(y);
        int off = layout.getOffsetForHorizontal(line, x);

        return off;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction();
        final int x = (int) ev.getX();
        final int y = (int) ev.getY();

        if (action == MotionEvent.ACTION_DOWN) {
            mLongPressedPosition = pointToPosition(x, y);
        }

        return super.onTouchEvent(ev);
    }

    @Override
    protected ContextMenuInfo getContextMenuInfo() {
        if ((mLongPressedPosition >= 0)) {
            Spanned text = getText();
            if (mLongPressedPosition <= text.length()) {
                int start = mTokenizer.findTokenStart(text, mLongPressedPosition);
                int end = mTokenizer.findTokenEnd(text, start);

                if (end != start) {
                    String number = getNumberAt(getText(), start, end, getContext());
                    Contact c = Contact.get(number, false);
                    return new RecipientContextMenuInfo(c);
                }
            }
        }
        return null;
    }

    private static String getNumberAt(Spanned sp, int start, int end, Context context) {
        return getFieldAt("number", sp, start, end, context);
    }

    private static int getSpanLength(Spanned sp, int start, int end, Context context) {
        // TODO: there's a situation where the span can lose its annotations:
        //   - add an auto-complete contact
        //   - add another auto-complete contact
        //   - delete that second contact and keep deleting into the first
        //   - we lose the annotation and can no longer get the span.
        // Need to fix this case because it breaks auto-complete contacts with commas in the name.
        Annotation[] a = sp.getSpans(start, end, Annotation.class);
        if (a.length > 0) {
            return sp.getSpanEnd(a[0]);
        }
        return 0;
    }

    private static String getFieldAt(String field, Spanned sp, int start, int end,
            Context context) {
        Annotation[] a = sp.getSpans(start, end, Annotation.class);
        String fieldValue = getAnnotation(a, field);
        if (TextUtils.isEmpty(fieldValue)) {
            fieldValue = TextUtils.substring(sp, start, end);
            // just return the substring is not so good,
            // its format is probably like this: lily <1234567>
            // as the comment in populate, there is some cases that user can deliminate the annotation.
            // when this happened, we come here.
            // and the old strategy is return the whole string as the phone number
            // but the while string is as lily <1234567>
            // it is wrong. currently we can not find out all cases that deliminate the annotation.
            // but we can make a little more, try to filter out the right number and return it.
            // it's better than just return.
            int lIndex = TextUtils.indexOf(fieldValue, "<");
            if (lIndex >= 0) {
                int rIndex = TextUtils.indexOf(fieldValue, ">");
                if (lIndex < rIndex) {
                    fieldValue = TextUtils.substring(fieldValue, lIndex+1, rIndex);
                    Xlog.d(TAG, "annotation missing! filter right number:"+fieldValue);
                }
            }
        }
        return fieldValue;

    }

    private static String getAnnotation(Annotation[] a, String key) {
        for (int i = 0; i < a.length; i++) {
            if (a[i].getKey().equals(key)) {
                return a[i].getValue();
            }
        }

        return "";
    }

    private class RecipientsEditorTokenizer
            implements MultiAutoCompleteTextView.Tokenizer {
        private final MultiAutoCompleteTextView mList;
        private final Context mContext;

        RecipientsEditorTokenizer(Context context, MultiAutoCompleteTextView list) {
            mList = list;
            mContext = context;
        }

        /**
         * Returns the start of the token that ends at offset
         * <code>cursor</code> within <code>text</code>.
         * It is a method from the MultiAutoCompleteTextView.Tokenizer interface.
         */
        public int findTokenStart(CharSequence text, int cursor) {
            int i = cursor;
            char c;
            while (i > 0 && (c = text.charAt(i - 1)) != ',' && c != ';') {
                i--;
            }
            while (i < cursor && text.charAt(i) == ' ') {
                i++;
            }
            return i;
        }

        /**
         * Returns the end of the token (minus trailing punctuation)
         * that begins at offset <code>cursor</code> within <code>text</code>.
         * It is a method from the MultiAutoCompleteTextView.Tokenizer interface.
         */
        public int findTokenEnd(CharSequence text, int cursor) {
            int i = cursor;
            int len = text.length();
            char c;

            while (i < len) {
                if ((c = text.charAt(i)) == ',' || c == ';') {
                    return i;
                } else {
                    i++;
                }
            }

            return len;
        }

        /**
         * Returns <code>text</code>, modified, if necessary, to ensure that
         * it ends with a token terminator (for example a space or comma).
         * It is a method from the MultiAutoCompleteTextView.Tokenizer interface.
         */
        public CharSequence terminateToken(CharSequence text) {
            int i = text.length();

            while (i > 0 && text.charAt(i - 1) == ' ') {
                i--;
            }

            char c;
            if (i > 0 && ((c = text.charAt(i - 1)) == ',' || c == ';')) {
                return text;
            } else {
                // Use the same delimiter the user just typed.
                // This lets them have a mixture of commas and semicolons in their list.
                String separator = mLastSeparator + " ";
                if (text instanceof Spanned) {
                    SpannableString sp = new SpannableString(text + separator);
                    TextUtils.copySpansFrom((Spanned) text, 0, text.length(),
                                            Object.class, sp, 0);
                    return sp;
                } else {
                    return text + separator;
                }
            }
        }

        public List<String> getNumbers() {
            Spanned sp = mList.getText();
            int len = sp.length();
            List<String> list = new ArrayList<String>();

            int start = 0;
            int i = 0;
            while (i < len + 1) {
                char c;
                //65292 for Chinese ',' and 65307 for Chinese ';'
                if ((i == len) || ((c = sp.charAt(i)) == ',') || (c == ';') || ((int)c == 65307) || ((int)c == 65292)) {
                    if (i > start) {
                        list.add(getNumberAt(sp, start, i, mContext));

                        // calculate the recipients total length. This is so if the name contains
                        // commas or semis, we'll skip over the whole name to the next
                        // recipient, rather than parsing this single name into multiple
                        // recipients.
                        int spanLen = getSpanLength(sp, start, i, mContext);
                        if (spanLen > i) {
                            i = spanLen;
                        }
                    }

                    i++;

                    while ((i < len) && (sp.charAt(i) == ' ')) {
                        i++;
                    }

                    start = i;
                } else {
                    i++;
                }
            }

            return list;
        }
    }

    static class RecipientContextMenuInfo implements ContextMenuInfo {
        final Contact recipient;

        RecipientContextMenuInfo(Contact r) {
            recipient = r;
        }
    }

// a0
//    public void setNumbers(List<String> numberList) {
//        mTokenizer.setNumbers(numberList);
//    }
//
//    public String allNumberToString() {
//        return mTokenizer.allNumberToString();
//    }
//
//    public boolean isDuplicateNumber(String number) {
//        return mTokenizer.getNumbers().contains(number);
//    }
//
//    @Override
//    public boolean onKeyUp(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
//            InputMethodManager inputMethodManager = (InputMethodManager) getContext()
//                    .getSystemService(Context.INPUT_METHOD_SERVICE);
//
//            inputMethodManager.showSoftInput(this, 0);
//        }
//        return super.onKeyUp(keyCode, event);
//    }
//
//    @Override
//    public void showDropDown() {
//        // TODO Auto-generated method stub
//        super.showDropDown();
//        ((View) (this.getParent().getParent())).invalidate();
//    }

// a1
}
