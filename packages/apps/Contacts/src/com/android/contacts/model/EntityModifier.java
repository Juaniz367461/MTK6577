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

package com.android.contacts.model;

import com.mediatek.contacts.model.UsimAccountType;
import com.mediatek.contacts.util.ContactsGroupUtils;
import com.mediatek.contacts.util.ContactsGroupUtils.USIMAas;
import com.mediatek.contacts.util.OperatorUtils;
import com.android.contacts.ContactsUtils;
import com.android.contacts.editor.ContactEditorFragment;
import com.android.contacts.editor.EventFieldEditorView;
import com.android.contacts.util.NameConverter;
import com.android.contacts.editor.PhoneticNameEditorView;
import com.android.contacts.model.AccountType.EditField;
import com.android.contacts.model.AccountType.EditType;
import com.android.contacts.model.AccountType.EventEditType;
import com.android.contacts.model.EntityDelta.ValuesDelta;
import com.android.contacts.util.DateUtils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.net.sip.SipManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.BaseTypes;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Event;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Im;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.Relation;
import android.provider.ContactsContract.CommonDataKinds.SipAddress;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Intents;
import android.provider.ContactsContract.Intents.Insert;
import android.provider.ContactsContract.RawContacts;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Helper methods for modifying an {@link EntityDelta}, such as inserting
 * new rows, or enforcing {@link AccountType}.
 */
public class EntityModifier {
    private static final String TAG = "EntityModifier";

    /** Set to true in order to view logs on entity operations */
    private static final boolean DEBUG = false;

    /**
     * For the given {@link EntityDelta}, determine if the given
     * {@link DataKind} could be inserted under specific
     * {@link AccountType}.
     */
    public static boolean canInsert(EntityDelta state, DataKind kind) {
        // Insert possible when have valid types and under overall maximum
        final int visibleCount = state.getMimeEntriesCount(kind.mimeType, true);
        final boolean validTypes = hasValidTypes(state, kind);
        final boolean validOverall = (kind.typeOverallMax == -1)
                || (visibleCount < kind.typeOverallMax);
        return (validTypes && validOverall);
    }

    public static boolean hasValidTypes(EntityDelta state, DataKind kind) {
        if (EntityModifier.hasEditTypes(kind)) {
            return (getValidTypes(state, kind).size() > 0);
        } else {
            return true;
        }
    }

    /**
     * Ensure that at least one of the given {@link DataKind} exists in the
     * given {@link EntityDelta} state, and try creating one if none exist.
     */
    public static void ensureKindExists(
            EntityDelta state, AccountType accountType, String mimeType) {
        final DataKind kind = accountType.getKindForMimetype(mimeType);
        /*
         * New Feature by Mediatek Begin. 
         * Original Android's code: 
         * M:AAS
         */
        if (OperatorUtils.isAasEnabled(accountType.accountType) && kind != null
                && Phone.CONTENT_ITEM_TYPE.equals(kind.mimeType)) {

            ArrayList<ValuesDelta> values = state.getMimeEntries(mimeType);
            final int slotId = ContactEditorFragment.getSlotId();
            final int slotNumber = USIMAas.getAnrCount(slotId);
            Log.e(TAG, "ensureKindExists slotId=" + slotId + ", slotNumber=" + slotNumber);
            if (values != null && values.size() == slotNumber + 1) {
                // primary number + slotNumber size
                Log.e(TAG, "ensureKindExists slotId=" + slotId + ", slotNumber=" + slotNumber);
                return;
            }
            if (values == null || (values.isEmpty() && kind != null)) {
                // Create child when none exists and valid kind
                final ValuesDelta child = insertChild(state, kind);
                if (kind.mimeType.equals(Photo.CONTENT_ITEM_TYPE)) {
                    child.setFromTemplate(true);
                }

                for (int i = 0; i < slotNumber; i++) {
                    final ValuesDelta slotChild = insertChild(state, kind);
                    slotChild.put(Data.IS_ADDITIONAL_NUMBER, 1);
                }
            } else {
                int pnrSize = 0;
                int anrSize = 0;
                if (values != null) {
                    for (ValuesDelta value : values) {
                        Integer isAnr = value.getAsInteger(Data.IS_ADDITIONAL_NUMBER);
                        if (isAnr != null && (isAnr.intValue() == 1)) {
                            anrSize++;
                        } else {
                            pnrSize++;
                        }
                    }
                }
                Log.d(TAG, "pnrSize=" + pnrSize + ", anrSize=" + anrSize);
                if (pnrSize < 1) {
                    // insert a empty primary number if not exists.
                    insertChild(state, kind);
                }
                for (; anrSize < slotNumber; anrSize++) {
                    // insert additional numbers if not full.
                    final ValuesDelta slotChild = insertChild(state, kind);
                    slotChild.put(Data.IS_ADDITIONAL_NUMBER, 1);
                }
            }
        } else {
            /*
             * New Feature by Mediatek End.
             */
            final boolean hasChild = state.getMimeEntriesCount(mimeType, true) > 0;
            if (!hasChild && kind != null) {
                // Create child when none exists and valid kind
                final ValuesDelta child = insertChild(state, kind);
                if (kind.mimeType.equals(Photo.CONTENT_ITEM_TYPE)) {
                    child.setFromTemplate(true);
                }
            }
        }
    }

    /**
     * For the given {@link EntityDelta} and {@link DataKind}, return the
     * list possible {@link EditType} options available based on
     * {@link AccountType}.
     */
    public static ArrayList<EditType> getValidTypes(EntityDelta state, DataKind kind) {
        return getValidTypes(state, kind, null, true, null);
    }

    /**
     * For the given {@link EntityDelta} and {@link DataKind}, return the
     * list possible {@link EditType} options available based on
     * {@link AccountType}.
     *
     * @param forceInclude Always include this {@link EditType} in the returned
     *            list, even when an otherwise-invalid choice. This is useful
     *            when showing a dialog that includes the current type.
     */
    public static ArrayList<EditType> getValidTypes(EntityDelta state, DataKind kind,
            EditType forceInclude) {
        return getValidTypes(state, kind, forceInclude, true, null);
    }

    /**
     * For the given {@link EntityDelta} and {@link DataKind}, return the
     * list possible {@link EditType} options available based on
     * {@link AccountType}.
     *
     * @param forceInclude Always include this {@link EditType} in the returned
     *            list, even when an otherwise-invalid choice. This is useful
     *            when showing a dialog that includes the current type.
     * @param includeSecondary If true, include any valid types marked as
     *            {@link EditType#secondary}.
     * @param typeCount When provided, will be used for the frequency count of
     *            each {@link EditType}, otherwise built using
     *            {@link #getTypeFrequencies(EntityDelta, DataKind)}.
     */
    private static ArrayList<EditType> getValidTypes(EntityDelta state, DataKind kind,
            EditType forceInclude, boolean includeSecondary, SparseIntArray typeCount) {
        final ArrayList<EditType> validTypes = new ArrayList<EditType>();

        // Bail early if no types provided
        if (!hasEditTypes(kind)) return validTypes;

        if (typeCount == null) {
            // Build frequency counts if not provided
            typeCount = getTypeFrequencies(state, kind);
        }

        // Build list of valid types
        final int overallCount = typeCount.get(FREQUENCY_TOTAL);
        for (EditType type : kind.typeList) {
            final boolean validOverall = (kind.typeOverallMax == -1 ? true
                    : overallCount < kind.typeOverallMax);
            final boolean validSpecific = (type.specificMax == -1 ? true : typeCount
                    .get(type.rawValue) < type.specificMax);
            final boolean validSecondary = (includeSecondary ? true : !type.secondary);
            final boolean forcedInclude = type.equals(forceInclude);
            if (forcedInclude || (validOverall && validSpecific && validSecondary)) {
                // Type is valid when no limit, under limit, or forced include
                validTypes.add(type);
            }
        }

        return validTypes;
    }

    private static final int FREQUENCY_TOTAL = Integer.MIN_VALUE;

    /**
     * Count up the frequency that each {@link EditType} appears in the given
     * {@link EntityDelta}. The returned {@link SparseIntArray} maps from
     * {@link EditType#rawValue} to counts, with the total overall count stored
     * as {@link #FREQUENCY_TOTAL}.
     */
    private static SparseIntArray getTypeFrequencies(EntityDelta state, DataKind kind) {
        final SparseIntArray typeCount = new SparseIntArray();

        // Find all entries for this kind, bailing early if none found
        final List<ValuesDelta> mimeEntries = state.getMimeEntries(kind.mimeType);
        if (mimeEntries == null) return typeCount;

        int totalCount = 0;
        for (ValuesDelta entry : mimeEntries) {
            // Only count visible entries
            if (!entry.isVisible()) continue;
            totalCount++;

            final EditType type = getCurrentType(entry, kind);
            if (type != null) {
                final int count = typeCount.get(type.rawValue);
                typeCount.put(type.rawValue, count + 1);
            }
        }
        typeCount.put(FREQUENCY_TOTAL, totalCount);
        return typeCount;
    }

    /**
     * Check if the given {@link DataKind} has multiple types that should be
     * displayed for users to pick.
     */
    public static boolean hasEditTypes(DataKind kind) {
        return kind.typeList != null && kind.typeList.size() > 0;
    }

    /**
     * Find the {@link EditType} that describes the given
     * {@link ValuesDelta} row, assuming the given {@link DataKind} dictates
     * the possible types.
     */
    public static EditType getCurrentType(ValuesDelta entry, DataKind kind) {
        final Long rawValue = entry.getAsLong(kind.typeColumn);
        if (rawValue == null) return null;
        return getType(kind, rawValue.intValue());
    }

    /**
     * Find the {@link EditType} that describes the given {@link ContentValues} row,
     * assuming the given {@link DataKind} dictates the possible types.
     */
    public static EditType getCurrentType(ContentValues entry, DataKind kind) {
        if (kind.typeColumn == null) return null;
        final Integer rawValue = entry.getAsInteger(kind.typeColumn);
        if (rawValue == null) return null;
        return getType(kind, rawValue);
    }

    /**
     * Find the {@link EditType} that describes the given {@link Cursor} row,
     * assuming the given {@link DataKind} dictates the possible types.
     */
    public static EditType getCurrentType(Cursor cursor, DataKind kind) {
        if (kind.typeColumn == null) return null;
        final int index = cursor.getColumnIndex(kind.typeColumn);
        if (index == -1) return null;
        final int rawValue = cursor.getInt(index);
        return getType(kind, rawValue);
    }

    /**
     * Find the {@link EditType} with the given {@link EditType#rawValue}.
     */
    public static EditType getType(DataKind kind, int rawValue) {
        for (EditType type : kind.typeList) {
            if (type.rawValue == rawValue) {
                return type;
            }
        }
        return null;
    }

    /**
     * Return the precedence for the the given {@link EditType#rawValue}, where
     * lower numbers are higher precedence.
     */
    public static int getTypePrecedence(DataKind kind, int rawValue) {
        for (int i = 0; i < kind.typeList.size(); i++) {
            final EditType type = kind.typeList.get(i);
            if (type.rawValue == rawValue) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }

    /**
     * Find the best {@link EditType} for a potential insert. The "best" is the
     * first primary type that doesn't already exist. When all valid types
     * exist, we pick the last valid option.
     */
    public static EditType getBestValidType(EntityDelta state, DataKind kind,
            boolean includeSecondary, int exactValue) {
        // Shortcut when no types
        if (kind.typeColumn == null) return null;

        // Find type counts and valid primary types, bail if none
        final SparseIntArray typeCount = getTypeFrequencies(state, kind);
        final ArrayList<EditType> validTypes = getValidTypes(state, kind, null, includeSecondary,
                typeCount);
        if (validTypes.size() == 0) return null;

        // Keep track of the last valid type
        final EditType lastType = validTypes.get(validTypes.size() - 1);

        // Remove any types that already exist
        Iterator<EditType> iterator = validTypes.iterator();
        while (iterator.hasNext()) {
            final EditType type = iterator.next();
            final int count = typeCount.get(type.rawValue);

            if (exactValue == type.rawValue) {
                // Found exact value match
                return type;
            }

            if (count > 0) {
                // Type already appears, so don't consider
                iterator.remove();
            }
        }

        // Use the best remaining, otherwise the last valid
        if (validTypes.size() > 0) {
            return validTypes.get(0);
        } else {
            return lastType;
        }
    }

    /**
     * Insert a new child of kind {@link DataKind} into the given
     * {@link EntityDelta}. Tries using the best {@link EditType} found using
     * {@link #getBestValidType(EntityDelta, DataKind, boolean, int)}.
     */
    public static ValuesDelta insertChild(EntityDelta state, DataKind kind) {
        // First try finding a valid primary
        /*
         * New Feature by Mediatek Begin. 
         * Original Android's code: 
         *  EditType bestType = getBestValidType(state, kind, false, Integer.MIN_VALUE); 
         * M:AAS
         */
        EditType bestType = null;
        if (OperatorUtils.isAasEnabled(kind.mAccountType)
                && Phone.CONTENT_ITEM_TYPE.equals(kind.mimeType)) {
            bestType = kind.typeList.get(0);
        } else {
            bestType = getBestValidType(state, kind, false, Integer.MIN_VALUE);
        }
        /*
         * New Feature by Mediatek End.
         */

        if (bestType == null) {
            // No valid primary found, so expand search to secondary
            bestType = getBestValidType(state, kind, true, Integer.MIN_VALUE);
        }
        return insertChild(state, kind, bestType);
    }

    /**
     * Insert a new child of kind {@link DataKind} into the given
     * {@link EntityDelta}, marked with the given {@link EditType}.
     */
    public static ValuesDelta insertChild(EntityDelta state, DataKind kind, EditType type) {
        // Bail early if invalid kind
        if (kind == null) return null;
        final ContentValues after = new ContentValues();

        // Our parent CONTACT_ID is provided later
        after.put(Data.MIMETYPE, kind.mimeType);

        // Fill-in with any requested default values
        if (kind.defaultValues != null) {
            after.putAll(kind.defaultValues);
        }

        if (kind.typeColumn != null && type != null) {
            // Set type, if provided
            after.put(kind.typeColumn, type.rawValue);
        }

        final ValuesDelta child = ValuesDelta.fromAfter(after);
        state.addEntry(child);
        return child;
    }

    /**
     * Processing to trim any empty {@link ValuesDelta} and {@link EntityDelta}
     * from the given {@link EntityDeltaList}, assuming the given {@link AccountTypeManager}
     * dictates the structure for various fields. This method ignores rows not
     * described by the {@link AccountType}.
     */
    public static void trimEmpty(EntityDeltaList set, AccountTypeManager accountTypes) {
        for (EntityDelta state : set) {
            ValuesDelta values = state.getValues();
            final String accountType = values.getAsString(RawContacts.ACCOUNT_TYPE);
            final String dataSet = values.getAsString(RawContacts.DATA_SET);
            final AccountType type = accountTypes.getAccountType(accountType, dataSet);
            trimEmpty(state, type);
        }
    }

    public static boolean hasChanges(EntityDeltaList set, AccountTypeManager accountTypes) {
        if (set.isMarkedForSplitting() || set.isMarkedForJoining()) {
            return true;
        }

        for (EntityDelta state : set) {
            ValuesDelta values = state.getValues();
            final String accountType = values.getAsString(RawContacts.ACCOUNT_TYPE);
            final String dataSet = values.getAsString(RawContacts.DATA_SET);
            final AccountType type = accountTypes.getAccountType(accountType, dataSet);
            if (hasChanges(state, type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Processing to trim any empty {@link ValuesDelta} rows from the given
     * {@link EntityDelta}, assuming the given {@link AccountType} dictates
     * the structure for various fields. This method ignores rows not described
     * by the {@link AccountType}.
     */
    public static void trimEmpty(EntityDelta state, AccountType accountType) {
        boolean hasValues = false;

        // Walk through entries for each well-known kind
        for (DataKind kind : accountType.getSortedDataKinds()) {
            final String mimeType = kind.mimeType;
            final ArrayList<ValuesDelta> entries = state.getMimeEntries(mimeType);
            if (entries == null) continue;

            for (ValuesDelta entry : entries) {
                // Skip any values that haven't been touched
                final boolean touched = entry.isInsert() || entry.isUpdate();
                if (!touched) {
                    hasValues = true;
                    continue;
                }

                // Test and remove this row if empty and it isn't a photo from google
                final boolean isGoogleAccount = TextUtils.equals(GoogleAccountType.ACCOUNT_TYPE,
                        state.getValues().getAsString(RawContacts.ACCOUNT_TYPE));
                final boolean isPhoto = TextUtils.equals(Photo.CONTENT_ITEM_TYPE, kind.mimeType);
                final boolean isGooglePhoto = isPhoto && isGoogleAccount;

                if (EntityModifier.isEmpty(entry, kind) && !isGooglePhoto) {
                    if (DEBUG) {
                        Log.v(TAG, "Trimming: " + entry.toString());
                    }
                    entry.markDeleted();
                } else if (!entry.isFromTemplate()) {
                    hasValues = true;
                }
            }
        }
        if (!hasValues) {
            // Trim overall entity if no children exist
            state.markDeleted();
        }
    }

    private static boolean hasChanges(EntityDelta state, AccountType accountType) {
        for (DataKind kind : accountType.getSortedDataKinds()) {
            final String mimeType = kind.mimeType;
            final ArrayList<ValuesDelta> entries = state.getMimeEntries(mimeType);
            if (entries == null) continue;

            for (ValuesDelta entry : entries) {
                // An empty Insert must be ignored, because it won't save anything (an example
                // is an empty name that stays empty)
                final boolean isRealInsert = entry.isInsert() && !isEmpty(entry, kind);
                if (isRealInsert || entry.isUpdate() || entry.isDelete()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Test if the given {@link ValuesDelta} would be considered "empty" in
     * terms of {@link DataKind#fieldList}.
     */
    public static boolean isEmpty(ValuesDelta values, DataKind kind) {
        if (Photo.CONTENT_ITEM_TYPE.equals(kind.mimeType)) {
            return values.isInsert() && values.getAsByteArray(Photo.PHOTO) == null;
        }

        // No defined fields mean this row is always empty
        if (kind.fieldList == null) return true;

        for (EditField field : kind.fieldList) {
            // If any field has values, we're not empty
            final String value = values.getAsString(field.column);
            if (ContactsUtils.isGraphic(value)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Compares corresponding fields in values1 and values2. Only the fields
     * declared by the DataKind are taken into consideration.
     */
    protected static boolean areEqual(ValuesDelta values1, ContentValues values2, DataKind kind) {
        if (kind.fieldList == null) {
            Log.i(TAG,"[areEqual] false");
            return false;
            }
        
        for (EditField field : kind.fieldList) {
            final String value1 = values1.getAsString(field.column);
            final String value2 = values2.getAsString(field.column);
            Log.i(TAG,"[areEqual] value1 : "+value1+" | value2 : "+value2);
            if (!TextUtils.equals(value1, value2)) {
                return false;
            }
        }
        Log.i(TAG,"[areEqual] true");
        return true;
    }

    /**
     * Parse the given {@link Bundle} into the given {@link EntityDelta} state,
     * assuming the extras defined through {@link Intents}.
     */
    public static void parseExtras(Context context, AccountType accountType, EntityDelta state,
            Bundle extras) {
        if (extras == null || extras.size() == 0) {
            // Bail early if no useful data
            return;
        }

        parseStructuredNameExtra(context, accountType, state, extras);
        parseStructuredPostalExtra(accountType, state, extras);

        {
            // Phone
            final DataKind kind = accountType.getKindForMimetype(Phone.CONTENT_ITEM_TYPE);
            parseExtras(state, kind, extras, Insert.PHONE_TYPE, Insert.PHONE, Phone.NUMBER);
            parseExtras(state, kind, extras, Insert.SECONDARY_PHONE_TYPE, Insert.SECONDARY_PHONE,
                    Phone.NUMBER);
            parseExtras(state, kind, extras, Insert.TERTIARY_PHONE_TYPE, Insert.TERTIARY_PHONE,
                    Phone.NUMBER);
        }

        {
            // Email
            final DataKind kind = accountType.getKindForMimetype(Email.CONTENT_ITEM_TYPE);
            parseExtras(state, kind, extras, Insert.EMAIL_TYPE, Insert.EMAIL, Email.DATA);
            parseExtras(state, kind, extras, Insert.SECONDARY_EMAIL_TYPE, Insert.SECONDARY_EMAIL,
                    Email.DATA);
            parseExtras(state, kind, extras, Insert.TERTIARY_EMAIL_TYPE, Insert.TERTIARY_EMAIL,
                    Email.DATA);
        }
        
        //The following lines are provided and maintained by Mediatek Inc.
        if(SipManager.isVoipSupported(context)){
        	//Internet call
        	final boolean hasInternetCall = extras.containsKey(Insert.SIP_ADDRESS);
        	String accountTypeName = accountType.getAccountTypeAndDataSet().accountType;
            Log.i(TAG, "[parseExtras] accountTypeName : " + accountTypeName);
            final DataKind kindSipAddress = accountType.getKindForMimetype(SipAddress.CONTENT_ITEM_TYPE);
            //UIM
//            if (hasInternetCall && (accountTypeName.equals(mSimAccountType) || accountTypeName.equals(mUsimAccountType))) {
            if (hasInternetCall && (accountTypeName.equals(mSimAccountType) 
                    || accountTypeName.equals(mUsimAccountType)
                    || accountTypeName.equals(mUimAccountType))) {
            //UIM
                Log.i(TAG, "[parseExtras] add SIP to sim/usim");
                mIsSimType = true;
            } else if (hasInternetCall && EntityModifier.canInsert(state, kindSipAddress)) {
                Log.i(TAG, "[parseExtras] add SIP to phone");
                final ValuesDelta child = EntityModifier.insertChild(state, kindSipAddress);
                Log.i(TAG,"[parseExtras] child.hashCode : "+child.hashCode());
                final String internetCall = extras.getString(Insert.SIP_ADDRESS);
                if (ContactsUtils.isGraphic(internetCall)) {
                    child.put(Data.DATA1, internetCall);
                } 
            } else if (hasInternetCall) {
                Log.i(TAG,"[parseExtras] has SIP No. so add fail.");
                mHasSip = true;
            }
        }
        //The previous lines are provided and maintained by Mediatek Inc.

        {
            // Im
            final DataKind kind = accountType.getKindForMimetype(Im.CONTENT_ITEM_TYPE);
            fixupLegacyImType(extras);
            parseExtras(state, kind, extras, Insert.IM_PROTOCOL, Insert.IM_HANDLE, Im.DATA);
        }

        // Organization
        final boolean hasOrg = extras.containsKey(Insert.COMPANY)
                || extras.containsKey(Insert.JOB_TITLE);
        final DataKind kindOrg = accountType.getKindForMimetype(Organization.CONTENT_ITEM_TYPE);
        if (hasOrg && EntityModifier.canInsert(state, kindOrg)) {
            final ValuesDelta child = EntityModifier.insertChild(state, kindOrg);

            final String company = extras.getString(Insert.COMPANY);
            if (ContactsUtils.isGraphic(company)) {
                child.put(Organization.COMPANY, company);
            }

            final String title = extras.getString(Insert.JOB_TITLE);
            if (ContactsUtils.isGraphic(title)) {
                child.put(Organization.TITLE, title);
            }
        }

        // Notes
        final boolean hasNotes = extras.containsKey(Insert.NOTES);
        final DataKind kindNotes = accountType.getKindForMimetype(Note.CONTENT_ITEM_TYPE);
        if (hasNotes && EntityModifier.canInsert(state, kindNotes)) {
            final ValuesDelta child = EntityModifier.insertChild(state, kindNotes);

            final String notes = extras.getString(Insert.NOTES);
            if (ContactsUtils.isGraphic(notes)) {
                child.put(Note.NOTE, notes);
            }
        }

        // Arbitrary additional data
        ArrayList<ContentValues> values = extras.getParcelableArrayList(Insert.DATA);
        if (values != null) {
            Log.i(TAG,"[parseValues]");
            parseValues(state, accountType, values);
        }
    }

    private static void parseStructuredNameExtra(
            Context context, AccountType accountType, EntityDelta state, Bundle extras) {
        // StructuredName
        EntityModifier.ensureKindExists(state, accountType, StructuredName.CONTENT_ITEM_TYPE);
        final ValuesDelta child = state.getPrimaryEntry(StructuredName.CONTENT_ITEM_TYPE);

        final String name = extras.getString(Insert.NAME);
        if (ContactsUtils.isGraphic(name)) {
            final DataKind kind = accountType.getKindForMimetype(StructuredName.CONTENT_ITEM_TYPE);
            boolean supportsDisplayName = false;
            if (kind.fieldList != null) {
                for (EditField field : kind.fieldList) {
                    if (StructuredName.DISPLAY_NAME.equals(field.column)) {
                        supportsDisplayName = true;
                        break;
                    }
                }
            }

            if (supportsDisplayName) {
                child.put(StructuredName.DISPLAY_NAME, name);
            } else {
                Uri uri = ContactsContract.AUTHORITY_URI.buildUpon()
                        .appendPath("complete_name")
                        .appendQueryParameter(StructuredName.DISPLAY_NAME, name)
                        .build();
                Cursor cursor = context.getContentResolver().query(uri,
                        new String[]{
                                StructuredName.PREFIX,
                                StructuredName.GIVEN_NAME,
                                StructuredName.MIDDLE_NAME,
                                StructuredName.FAMILY_NAME,
                                StructuredName.SUFFIX,
                        }, null, null, null);

                try {
                    if (cursor.moveToFirst()) {
                        child.put(StructuredName.PREFIX, cursor.getString(0));
                        child.put(StructuredName.GIVEN_NAME, cursor.getString(1));
                        child.put(StructuredName.MIDDLE_NAME, cursor.getString(2));
                        child.put(StructuredName.FAMILY_NAME, cursor.getString(3));
                        child.put(StructuredName.SUFFIX, cursor.getString(4));
                    }
                } finally {
                    cursor.close();
                }
            }
        }

        final String phoneticName = extras.getString(Insert.PHONETIC_NAME);
        if (ContactsUtils.isGraphic(phoneticName)) {
            child.put(StructuredName.PHONETIC_GIVEN_NAME, phoneticName);
        }
    }

    private static void parseStructuredPostalExtra(
            AccountType accountType, EntityDelta state, Bundle extras) {
        // StructuredPostal
        final DataKind kind = accountType.getKindForMimetype(StructuredPostal.CONTENT_ITEM_TYPE);
        final ValuesDelta child = parseExtras(state, kind, extras, Insert.POSTAL_TYPE,
                Insert.POSTAL, StructuredPostal.FORMATTED_ADDRESS);
        String address = child == null ? null
                : child.getAsString(StructuredPostal.FORMATTED_ADDRESS);
        if (!TextUtils.isEmpty(address)) {
            boolean supportsFormatted = false;
            if (kind.fieldList != null) {
                for (EditField field : kind.fieldList) {
                    if (StructuredPostal.FORMATTED_ADDRESS.equals(field.column)) {
                        supportsFormatted = true;
                        break;
                    }
                }
            }

            if (!supportsFormatted) {
                child.put(StructuredPostal.STREET, address);
                child.putNull(StructuredPostal.FORMATTED_ADDRESS);
            }
        }
    }

    private static void parseValues(
            EntityDelta state, AccountType accountType, ArrayList<ContentValues> dataValueList) {
        Log.i(TAG,"[parseValues] state : "+state);
        Log.i(TAG,"[parseValues] dataValueList : "+dataValueList);
        for (ContentValues values : dataValueList) {
            String mimeType = values.getAsString(Data.MIMETYPE);
            if (TextUtils.isEmpty(mimeType)) {
                Log.e(TAG, "Mimetype is required. Ignoring: " + values);
                continue;
            }

            // Won't override the contact name
            if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                continue;
            }

            DataKind kind = accountType.getKindForMimetype(mimeType);
            if (kind == null) {
                Log.e(TAG, "Mimetype not supported for account type "
                        + accountType.getAccountTypeAndDataSet() + ". Ignoring: " + values);
                continue;
            }

            ValuesDelta entry = ValuesDelta.fromAfter(values);
            if (isEmpty(entry, kind)) {
                continue;
            }

            ArrayList<ValuesDelta> entries = state.getMimeEntries(mimeType);
            Log.i(TAG,"[parseValues] entries : "+entries);
            if ((kind.typeOverallMax != 1) || GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)) {
                Log.i(TAG,"[parseValues] kind.typeOverallMax != 1");
                // Check for duplicates
                boolean addEntry = true;
                int count = 0;
                if (entries != null && entries.size() > 0) {
                    Log.i(TAG,"[parseValues] entries.size() : "+entries.size());
                    for (ValuesDelta delta : entries) {
                        if (!delta.isDelete()) {
                            Log.i(TAG,"[parseValues]!delta.isDelete() ");
                            if (areEqual(delta, values, kind)) {
                                addEntry = false;
                                break;
                            }
                            /*
                             * Bug Fix by Mediatek Begin.
                             *   Original Android's code:
                             *     count++;
                             *   CR ID: ALPS00119439
                             *   Descriptions: add filter when delta is {[]} than don't add count
                             */
                            Log.i(TAG,"[parseValues] entries.size() : "+entries.size());
                            if (GroupMembership.CONTENT_ITEM_TYPE.equals(mimeType)) {
                                //do nothing
                            } else {
                                count++;
                            }
                            /*
                             * Bug Fix by Mediatek End.
                             */
                            //
                        }
                    }
                }
                Log.i(TAG,"[parseValues] count : "+count+" | kind.typeOverallMax: "+kind.typeOverallMax);
                if (kind.typeOverallMax != -1 && count >= kind.typeOverallMax) {
                    Log.e(TAG, "Mimetype allows at most " + kind.typeOverallMax
                            + " entries. Ignoring: " + values);
                    addEntry = false;
                }

                if (addEntry) {
                    Log.i(TAG,"[parseValues] entry : "+entry+" | entries : "+entries+" | kind : "+kind);
                    addEntry = adjustType(entry, entries, kind);
                    Log.i(TAG,"[parseValues] addEntry : "+addEntry);
                }

                if (addEntry) {
                    state.addEntry(entry);
                    Log.i(TAG,"[parseValues] state : "+state);
                }
                Log.i(TAG,"[parseValues] count : " +count);
            } else {
                Log.i(TAG,"[parseValues] kind.typeOverallMax == 1");
                // Non-list entries should not be overridden
                boolean addEntry = true;
                if (entries != null && entries.size() > 0) {
                    Log.i(TAG,"[parseValues] entries != null entries: "+entries);
                    for (ValuesDelta delta : entries) {
                        if (!delta.isDelete() && !isEmpty(delta, kind)) {
                            addEntry = false;
                            break;
                        }
                    }
                    if (addEntry) {
                        for (ValuesDelta delta : entries) {
                            delta.markDeleted();
                        }
                    }
                }

                if (addEntry) {
                    addEntry = adjustType(entry, entries, kind);
                    Log.i(TAG,"[parseValues] addEntry1 : "+addEntry);
                }

                if (addEntry) {
                    state.addEntry(entry);
                    Log.i(TAG,"[parseValues] state1 : "+state);
                } else if (Note.CONTENT_ITEM_TYPE.equals(mimeType)){
                    // Note is most likely to contain large amounts of text
                    // that we don't want to drop on the ground.
                    for (ValuesDelta delta : entries) {
                        if (!isEmpty(delta, kind)) {
                            delta.put(Note.NOTE, delta.getAsString(Note.NOTE) + "\n"
                                    + values.getAsString(Note.NOTE));
                            break;
                        }
                    }
                } else {
                    Log.e(TAG, "Will not override mimetype " + mimeType + ". Ignoring: "
                            + values);
                }
            }
        }
    }

    /**
     * Checks if the data kind allows addition of another entry (e.g. Exchange only
     * supports two "work" phone numbers).  If not, tries to switch to one of the
     * unused types.  If successful, returns true.
     */
    private static boolean adjustType(
            ValuesDelta entry, ArrayList<ValuesDelta> entries, DataKind kind) {
        if (kind.typeColumn == null || kind.typeList == null || kind.typeList.size() == 0) {
            Log.i(TAG,"[adjustType] true");
            return true;
        }

        Integer typeInteger = entry.getAsInteger(kind.typeColumn);
        int type = typeInteger != null ? typeInteger : kind.typeList.get(0).rawValue;
        Log.i(TAG,"[adjustType] typeInteger : " +typeInteger+ " | type : "+type);
        if (isTypeAllowed(type, entries, kind)) {
            entry.put(kind.typeColumn, type);
            Log.i(TAG,"[adjustType] entry1 : "+entry);
            return true;
        }

        // Specified type is not allowed - choose the first available type that is allowed
        int size = kind.typeList.size();
        for (int i = 0; i < size; i++) {
            EditType editType = kind.typeList.get(i);
            if (isTypeAllowed(editType.rawValue, entries, kind)) {
                entry.put(kind.typeColumn, editType.rawValue);
                Log.i(TAG,"[adjustType] entry1 : "+entry+ " | size: "+size);
                return true;
            }
        }
        Log.i(TAG,"[adjustType] false");
        return false;
    }

    /**
     * Checks if a new entry of the specified type can be added to the raw
     * contact. For example, Exchange only supports two "work" phone numbers, so
     * addition of a third would not be allowed.
     */
    private static boolean isTypeAllowed(int type, ArrayList<ValuesDelta> entries, DataKind kind) {
        int max = 0;
        int size = kind.typeList.size();
        for (int i = 0; i < size; i++) {
            EditType editType = kind.typeList.get(i);
            if (editType.rawValue == type) {
                max = editType.specificMax;
                break;
            }
        }

        if (max == 0) {
            // This type is not allowed at all
            return false;
        }

        if (max == -1) {
            // Unlimited instances of this type are allowed
            return true;
        }

        return getEntryCountByType(entries, kind.typeColumn, type) < max;
    }

    /**
     * Counts occurrences of the specified type in the supplied entry list.
     */
    private static int getEntryCountByType(
            ArrayList<ValuesDelta> entries, String typeColumn, int type) {
        ///M: fix merge contact JE
        if (entries == null) {
            Log.i(TAG,"[getEntryCountByType] entries is null,return.");
            return -1;
        }
        int count = 0;
        int size = entries.size();
        for (int i = 0; i < size; i++) {
            Integer typeInteger = entries.get(i).getAsInteger(typeColumn);
            if (typeInteger != null && typeInteger == type) {
                count++;
            }
        }
        return count;
    }

    /**
     * Attempt to parse legacy {@link Insert#IM_PROTOCOL} values, replacing them
     * with updated values.
     */
    @SuppressWarnings("deprecation")
    private static void fixupLegacyImType(Bundle bundle) {
        final String encodedString = bundle.getString(Insert.IM_PROTOCOL);
        if (encodedString == null) return;

        try {
            final Object protocol = android.provider.Contacts.ContactMethods
                    .decodeImProtocol(encodedString);
            if (protocol instanceof Integer) {
                bundle.putInt(Insert.IM_PROTOCOL, (Integer)protocol);
            } else {
                bundle.putString(Insert.IM_PROTOCOL, (String)protocol);
            }
        } catch (IllegalArgumentException e) {
            // Ignore exception when legacy parser fails
        }
    }

    /**
     * Parse a specific entry from the given {@link Bundle} and insert into the
     * given {@link EntityDelta}. Silently skips the insert when missing value
     * or no valid {@link EditType} found.
     *
     * @param typeExtra {@link Bundle} key that holds the incoming
     *            {@link EditType#rawValue} value.
     * @param valueExtra {@link Bundle} key that holds the incoming value.
     * @param valueColumn Column to write value into {@link ValuesDelta}.
     */
    public static ValuesDelta parseExtras(EntityDelta state, DataKind kind, Bundle extras,
            String typeExtra, String valueExtra, String valueColumn) {
        final CharSequence value = extras.getCharSequence(valueExtra);

        // Bail early if account type doesn't handle this MIME type
        if (kind == null) return null;

        // Bail when can't insert type, or value missing
        final boolean canInsert = EntityModifier.canInsert(state, kind);
        final boolean validValue = (value != null && TextUtils.isGraphic(value));
        if (!validValue || !canInsert) return null;

        // Find exact type when requested, otherwise best available type
        final boolean hasType = extras.containsKey(typeExtra);
        final int typeValue = extras.getInt(typeExtra, hasType ? BaseTypes.TYPE_CUSTOM
                : Integer.MIN_VALUE);
        final EditType editType = EntityModifier.getBestValidType(state, kind, true, typeValue);

        // Create data row and fill with value
        final ValuesDelta child = EntityModifier.insertChild(state, kind, editType);
        child.put(valueColumn, value.toString());

        if (editType != null && editType.customColumn != null) {
            // Write down label when custom type picked
            final String customType = extras.getString(typeExtra);
            child.put(editType.customColumn, customType);
        }

        return child;
    }

    /**
     * Generic mime types with type support (e.g. TYPE_HOME).
     * Here, "type support" means if the data kind has CommonColumns#TYPE or not. Data kinds which
     * have their own migrate methods aren't listed here.
     */
    private static final Set<String> sGenericMimeTypesWithTypeSupport = new HashSet<String>(
            Arrays.asList(Phone.CONTENT_ITEM_TYPE,
                    Email.CONTENT_ITEM_TYPE,
                    Im.CONTENT_ITEM_TYPE,
                    Nickname.CONTENT_ITEM_TYPE,
                    Website.CONTENT_ITEM_TYPE,
                    Relation.CONTENT_ITEM_TYPE,
                    SipAddress.CONTENT_ITEM_TYPE));
    private static final Set<String> sGenericMimeTypesWithoutTypeSupport = new HashSet<String>(
            Arrays.asList(Organization.CONTENT_ITEM_TYPE,
                    Note.CONTENT_ITEM_TYPE,
                    Photo.CONTENT_ITEM_TYPE,
                    GroupMembership.CONTENT_ITEM_TYPE));
    // CommonColumns.TYPE cannot be accessed as it is protected interface, so use
    // Phone.TYPE instead.
    private static final String COLUMN_FOR_TYPE  = Phone.TYPE;
    private static final String COLUMN_FOR_LABEL  = Phone.LABEL;
    private static final int TYPE_CUSTOM = Phone.TYPE_CUSTOM;

    /**
     * Migrates old EntityDelta to newly created one with a new restriction supplied from
     * newAccountType.
     *
     * This is only for account switch during account creation (which must be insert operation).
     */
    public static void migrateStateForNewContact(Context context,
            EntityDelta oldState, EntityDelta newState,
            AccountType oldAccountType, AccountType newAccountType) {
        if (newAccountType == oldAccountType) {
            // Just copying all data in oldState isn't enough, but we can still rely on a lot of
            // shortcuts.
            for (DataKind kind : newAccountType.getSortedDataKinds()) {
                final String mimeType = kind.mimeType;
                // The fields with short/long form capability must be treated properly.
                if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    migrateStructuredName(context, oldState, newState, kind);
                } else {
                    List<ValuesDelta> entryList = oldState.getMimeEntries(mimeType);
                    if (entryList != null && !entryList.isEmpty()) {
                        for (ValuesDelta entry : entryList) {
                            ContentValues values = entry.getAfter();
                            if (values != null) {
                                newState.addEntry(ValuesDelta.fromAfter(values));
                            }
                        }
                    }
                }
            }
        } else {
            // Migrate data supported by the new account type.
            // All the other data inside oldState are silently dropped.
            for (DataKind kind : newAccountType.getSortedDataKinds()) {
                if (!kind.editable) continue;
                final String mimeType = kind.mimeType;
                if (DataKind.PSEUDO_MIME_TYPE_DISPLAY_NAME.equals(mimeType)
                        || DataKind.PSEUDO_MIME_TYPE_PHONETIC_NAME.equals(mimeType)) {
                    // Ignore pseudo data.
                    continue;
                } else if (StructuredName.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    migrateStructuredName(context, oldState, newState, kind);
                } else if (StructuredPostal.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    migratePostal(oldState, newState, kind);
                } else if (Event.CONTENT_ITEM_TYPE.equals(mimeType)) {
                    migrateEvent(oldState, newState, kind, null /* default Year */);
                } else if (sGenericMimeTypesWithoutTypeSupport.contains(mimeType)) {
                    migrateGenericWithoutTypeColumn(oldState, newState, kind);
                } else if (sGenericMimeTypesWithTypeSupport.contains(mimeType)) {
                    migrateGenericWithTypeColumn(oldState, newState, kind);
                } else {
                    throw new IllegalStateException("Unexpected editable mime-type: " + mimeType);
                }
            }
        }
    }

    /**
     * Checks {@link DataKind#isList} and {@link DataKind#typeOverallMax}, and restricts
     * the number of entries (ValuesDelta) inside newState.
     */
    private static ArrayList<ValuesDelta> ensureEntryMaxSize(EntityDelta newState, DataKind kind,
            ArrayList<ValuesDelta> mimeEntries) {
        if (mimeEntries == null) {
            return null;
        }

        final int typeOverallMax = kind.typeOverallMax;
        if (typeOverallMax >= 0 && (mimeEntries.size() > typeOverallMax)) {
            ArrayList<ValuesDelta> newMimeEntries = new ArrayList<ValuesDelta>(typeOverallMax);
            for (int i = 0; i < typeOverallMax; i++) {
                newMimeEntries.add(mimeEntries.get(i));
            }
            mimeEntries = newMimeEntries;
        }
        return mimeEntries;
    }

    /** @hide Public only for testing. */
    public static void migrateStructuredName(
            Context context, EntityDelta oldState, EntityDelta newState, DataKind newDataKind) {
        final ContentValues values =
                oldState.getPrimaryEntry(StructuredName.CONTENT_ITEM_TYPE).getAfter();
        if (values == null) {
            return;
        }

        boolean supportDisplayName = false;
        boolean supportPhoneticFullName = false;
        boolean supportPhoneticFamilyName = false;
        boolean supportPhoneticMiddleName = false;
        boolean supportPhoneticGivenName = false;
        for (EditField editField : newDataKind.fieldList) {
            if (StructuredName.DISPLAY_NAME.equals(editField.column)) {
                supportDisplayName = true;
            }
            if (DataKind.PSEUDO_COLUMN_PHONETIC_NAME.equals(editField.column)) {
                supportPhoneticFullName = true;
            }
            if (StructuredName.PHONETIC_FAMILY_NAME.equals(editField.column)) {
                supportPhoneticFamilyName = true;
            }
            if (StructuredName.PHONETIC_MIDDLE_NAME.equals(editField.column)) {
                supportPhoneticMiddleName = true;
            }
            if (StructuredName.PHONETIC_GIVEN_NAME.equals(editField.column)) {
                supportPhoneticGivenName = true;
            }
        }

        // DISPLAY_NAME <-> PREFIX, GIVEN_NAME, MIDDLE_NAME, FAMILY_NAME, SUFFIX
        final String displayName = values.getAsString(StructuredName.DISPLAY_NAME);
        if (!TextUtils.isEmpty(displayName)) {
            if (!supportDisplayName) {
                // Old data has a display name, while the new account doesn't allow it.
                NameConverter.displayNameToStructuredName(context, displayName, values);

                // We don't want to migrate unseen data which may confuse users after the creation.
                values.remove(StructuredName.DISPLAY_NAME);
            }
        } else {
            if (supportDisplayName) {
                // Old data does not have display name, while the new account requires it.
                values.put(StructuredName.DISPLAY_NAME,
                        NameConverter.structuredNameToDisplayName(context, values));
                for (String field : NameConverter.STRUCTURED_NAME_FIELDS) {
                    values.remove(field);
                }
            }
        }

        // Phonetic (full) name <-> PHONETIC_FAMILY_NAME, PHONETIC_MIDDLE_NAME, PHONETIC_GIVEN_NAME
        final String phoneticFullName = values.getAsString(DataKind.PSEUDO_COLUMN_PHONETIC_NAME);
        if (!TextUtils.isEmpty(phoneticFullName)) {
            if (!supportPhoneticFullName) {
                // Old data has a phonetic (full) name, while the new account doesn't allow it.
                final ContentValues tmpValues =
                        PhoneticNameEditorView.parsePhoneticName(phoneticFullName, null);
                values.remove(DataKind.PSEUDO_COLUMN_PHONETIC_NAME);
                if (supportPhoneticFamilyName) {
                    values.put(StructuredName.PHONETIC_FAMILY_NAME,
                            tmpValues.getAsString(StructuredName.PHONETIC_FAMILY_NAME));
                } else {
                    values.remove(StructuredName.PHONETIC_FAMILY_NAME);
                }
                if (supportPhoneticMiddleName) {
                    values.put(StructuredName.PHONETIC_MIDDLE_NAME,
                            tmpValues.getAsString(StructuredName.PHONETIC_MIDDLE_NAME));
                } else {
                    values.remove(StructuredName.PHONETIC_MIDDLE_NAME);
                }
                if (supportPhoneticGivenName) {
                    values.put(StructuredName.PHONETIC_GIVEN_NAME,
                            tmpValues.getAsString(StructuredName.PHONETIC_GIVEN_NAME));
                } else {
                    values.remove(StructuredName.PHONETIC_GIVEN_NAME);
                }
            }
        } else {
            if (supportPhoneticFullName) {
                // Old data does not have a phonetic (full) name, while the new account requires it.
                values.put(DataKind.PSEUDO_COLUMN_PHONETIC_NAME,
                        PhoneticNameEditorView.buildPhoneticName(
                                values.getAsString(StructuredName.PHONETIC_FAMILY_NAME),
                                values.getAsString(StructuredName.PHONETIC_MIDDLE_NAME),
                                values.getAsString(StructuredName.PHONETIC_GIVEN_NAME)));
            }
            if (!supportPhoneticFamilyName) {
                values.remove(StructuredName.PHONETIC_FAMILY_NAME);
            }
            if (!supportPhoneticMiddleName) {
                values.remove(StructuredName.PHONETIC_MIDDLE_NAME);
            }
            if (!supportPhoneticGivenName) {
                values.remove(StructuredName.PHONETIC_GIVEN_NAME);
            }
        }

        newState.addEntry(ValuesDelta.fromAfter(values));
    }

    /** @hide Public only for testing. */
    public static void migratePostal(EntityDelta oldState, EntityDelta newState,
            DataKind newDataKind) {
        final ArrayList<ValuesDelta> mimeEntries = ensureEntryMaxSize(newState, newDataKind,
                oldState.getMimeEntries(StructuredPostal.CONTENT_ITEM_TYPE));
        if (mimeEntries == null || mimeEntries.isEmpty()) {
            return;
        }

        boolean supportFormattedAddress = false;
        boolean supportStreet = false;
        final String firstColumn = newDataKind.fieldList.get(0).column;
        for (EditField editField : newDataKind.fieldList) {
            if (StructuredPostal.FORMATTED_ADDRESS.equals(editField.column)) {
                supportFormattedAddress = true;
            }
            if (StructuredPostal.STREET.equals(editField.column)) {
                supportStreet = true;
            }
        }

        final Set<Integer> supportedTypes = new HashSet<Integer>();
        if (newDataKind.typeList != null && !newDataKind.typeList.isEmpty()) {
            for (EditType editType : newDataKind.typeList) {
                supportedTypes.add(editType.rawValue);
            }
        }

        for (ValuesDelta entry : mimeEntries) {
            final ContentValues values = entry.getAfter();
            if (values == null) {
                continue;
            }
            final Integer oldType = values.getAsInteger(StructuredPostal.TYPE);
            if (!supportedTypes.contains(oldType)) {
                int defaultType;
                if (newDataKind.defaultValues != null) {
                    defaultType = newDataKind.defaultValues.getAsInteger(StructuredPostal.TYPE);
                } else {
                    defaultType = newDataKind.typeList.get(0).rawValue;
                }
                values.put(StructuredPostal.TYPE, defaultType);
                if (oldType != null && oldType == StructuredPostal.TYPE_CUSTOM) {
                    values.remove(StructuredPostal.LABEL);
                }
            }

            final String formattedAddress = values.getAsString(StructuredPostal.FORMATTED_ADDRESS);
            if (!TextUtils.isEmpty(formattedAddress)) {
                if (!supportFormattedAddress) {
                    // Old data has a formatted address, while the new account doesn't allow it.
                    values.remove(StructuredPostal.FORMATTED_ADDRESS);

                    // Unlike StructuredName we don't have logic to split it, so first
                    // try to use street field and. If the new account doesn't have one,
                    // then select first one anyway.
                    if (supportStreet) {
                        values.put(StructuredPostal.STREET, formattedAddress);
                    } else {
                        values.put(firstColumn, formattedAddress);
                    }
                }
            } else {
                if (supportFormattedAddress) {
                    // Old data does not have formatted address, while the new account requires it.
                    // Unlike StructuredName we don't have logic to join multiple address values.
                    // Use poor join heuristics for now.
                    String[] structuredData;
                    final boolean useJapaneseOrder =
                            Locale.JAPANESE.getLanguage().equals(Locale.getDefault().getLanguage());
                    if (useJapaneseOrder) {
                        structuredData = new String[] {
                                values.getAsString(StructuredPostal.COUNTRY),
                                values.getAsString(StructuredPostal.POSTCODE),
                                values.getAsString(StructuredPostal.REGION),
                                values.getAsString(StructuredPostal.CITY),
                                values.getAsString(StructuredPostal.NEIGHBORHOOD),
                                values.getAsString(StructuredPostal.STREET),
                                values.getAsString(StructuredPostal.POBOX) };
                    } else {
                        structuredData = new String[] {
                                values.getAsString(StructuredPostal.POBOX),
                                values.getAsString(StructuredPostal.STREET),
                                values.getAsString(StructuredPostal.NEIGHBORHOOD),
                                values.getAsString(StructuredPostal.CITY),
                                values.getAsString(StructuredPostal.REGION),
                                values.getAsString(StructuredPostal.POSTCODE),
                                values.getAsString(StructuredPostal.COUNTRY) };
                    }
                    final StringBuilder builder = new StringBuilder();
                    for (String elem : structuredData) {
                        if (!TextUtils.isEmpty(elem)) {
                            builder.append(elem + "\n");
                        }
                    }
                    values.put(StructuredPostal.FORMATTED_ADDRESS, builder.toString());

                    values.remove(StructuredPostal.POBOX);
                    values.remove(StructuredPostal.STREET);
                    values.remove(StructuredPostal.NEIGHBORHOOD);
                    values.remove(StructuredPostal.CITY);
                    values.remove(StructuredPostal.REGION);
                    values.remove(StructuredPostal.POSTCODE);
                    values.remove(StructuredPostal.COUNTRY);
                }
            }

            newState.addEntry(ValuesDelta.fromAfter(values));
        }
    }

    /** @hide Public only for testing. */
    public static void migrateEvent(EntityDelta oldState, EntityDelta newState,
            DataKind newDataKind, Integer defaultYear) {
        final ArrayList<ValuesDelta> mimeEntries = ensureEntryMaxSize(newState, newDataKind,
                oldState.getMimeEntries(Event.CONTENT_ITEM_TYPE));
        if (mimeEntries == null || mimeEntries.isEmpty()) {
            return;
        }

        final Map<Integer, EventEditType> allowedTypes = new HashMap<Integer, EventEditType>();
        for (EditType editType : newDataKind.typeList) {
            allowedTypes.put(editType.rawValue, (EventEditType) editType);
        }
        for (ValuesDelta entry : mimeEntries) {
            final ContentValues values = entry.getAfter();
            if (values == null) {
                continue;
            }
            final String dateString = values.getAsString(Event.START_DATE);
            final Integer type = values.getAsInteger(Event.TYPE);
            if (type != null && allowedTypes.containsKey(type) && !TextUtils.isEmpty(dateString)) {
                EventEditType suitableType = allowedTypes.get(type);

                final ParsePosition position = new ParsePosition(0);
                boolean yearOptional = false;
                Date date = DateUtils.DATE_AND_TIME_FORMAT.parse(dateString, position);
                if (date == null) {
                    yearOptional = true;
                    date = DateUtils.NO_YEAR_DATE_FORMAT.parse(dateString, position);
                }
                if (date != null) {
                    if (yearOptional && !suitableType.isYearOptional()) {
                        // The new EditType doesn't allow optional year. Supply default.
                        final Calendar calendar = Calendar.getInstance(DateUtils.UTC_TIMEZONE,
                                Locale.US);
                        if (defaultYear == null) {
                            defaultYear = calendar.get(Calendar.YEAR);
                        }
                        calendar.setTime(date);
                        final int month = calendar.get(Calendar.MONTH);
                        final int day = calendar.get(Calendar.DAY_OF_MONTH);
                        // Exchange requires 8:00 for birthdays
                        calendar.set(defaultYear, month, day,
                                EventFieldEditorView.getDefaultHourForBirthday(), 0, 0);
                        values.put(Event.START_DATE,
                                DateUtils.FULL_DATE_FORMAT.format(calendar.getTime()));
                    }
                }
                newState.addEntry(ValuesDelta.fromAfter(values));
            } else {
                // Just drop it.
            }
        }
    }

    /** @hide Public only for testing. */
    public static void migrateGenericWithoutTypeColumn(
            EntityDelta oldState, EntityDelta newState, DataKind newDataKind) {
        final ArrayList<ValuesDelta> mimeEntries = ensureEntryMaxSize(newState, newDataKind,
                oldState.getMimeEntries(newDataKind.mimeType));
        if (mimeEntries == null || mimeEntries.isEmpty()) {
            return;
        }

        for (ValuesDelta entry : mimeEntries) {
            ContentValues values = entry.getAfter();
            if (values != null) {
                newState.addEntry(ValuesDelta.fromAfter(values));
            }
        }
    }

    /** @hide Public only for testing. */
    public static void migrateGenericWithTypeColumn(
            EntityDelta oldState, EntityDelta newState, DataKind newDataKind) {
        final ArrayList<ValuesDelta> mimeEntries = oldState.getMimeEntries(newDataKind.mimeType);
        if (mimeEntries == null || mimeEntries.isEmpty()) {
            return;
        }

        // Note that type specified with the old account may be invalid with the new account, while
        // we want to preserve its data as much as possible. e.g. if a user typed a phone number
        // with a type which is valid with an old account but not with a new account, the user
        // probably wants to have the number with default type, rather than seeing complete data
        // loss.
        //
        // Specifically, this method works as follows:
        // 1. detect defaultType
        // 2. prepare constants & variables for iteration
        // 3. iterate over mimeEntries:
        // 3.1 stop iteration if total number of mimeEntries reached typeOverallMax specified in
        //     DataKind
        // 3.2 replace unallowed types with defaultType
        // 3.3 check if the number of entries is below specificMax specified in AccountType

        // Here, defaultType can be supplied in two ways
        // - via kind.defaultValues
        // - via kind.typeList.get(0).rawValue
        Integer defaultType = null;
        if (newDataKind.defaultValues != null) {
            defaultType = newDataKind.defaultValues.getAsInteger(COLUMN_FOR_TYPE);
        }
        final Set<Integer> allowedTypes = new HashSet<Integer>();
        // key: type, value: the number of entries allowed for the type (specificMax)
        final Map<Integer, Integer> typeSpecificMaxMap = new HashMap<Integer, Integer>();
        if (defaultType != null) {
            allowedTypes.add(defaultType);
            typeSpecificMaxMap.put(defaultType, -1);
        }
        // Note: typeList may be used in different purposes when defaultValues are specified.
        // Especially in IM, typeList contains available protocols (e.g. PROTOCOL_GOOGLE_TALK)
        // instead of "types" which we want to treate here (e.g. TYPE_HOME). So we don't add
        // anything other than defaultType into allowedTypes and typeSpecificMapMax.
        if (!Im.CONTENT_ITEM_TYPE.equals(newDataKind.mimeType) &&
                newDataKind.typeList != null && !newDataKind.typeList.isEmpty()) {
            for (EditType editType : newDataKind.typeList) {
                allowedTypes.add(editType.rawValue);
                typeSpecificMaxMap.put(editType.rawValue, editType.specificMax);
            }
            if (defaultType == null) {
                defaultType = newDataKind.typeList.get(0).rawValue;
            }
        }

        if (defaultType == null) {
            Log.w(TAG, "Default type isn't available for mimetype " + newDataKind.mimeType);
        }

        final int typeOverallMax = newDataKind.typeOverallMax;

        // key: type, value: the number of current entries.
        final Map<Integer, Integer> currentEntryCount = new HashMap<Integer, Integer>();
        int totalCount = 0;

        for (ValuesDelta entry : mimeEntries) {
            if (typeOverallMax != -1 && totalCount >= typeOverallMax) {
                break;
            }

            final ContentValues values = entry.getAfter();
            if (values == null) {
                continue;
            }

            final Integer oldType = entry.getAsInteger(COLUMN_FOR_TYPE);
            final Integer typeForNewAccount;
            if (!allowedTypes.contains(oldType)) {
                // The new account doesn't support the type.
                if (defaultType != null) {
                    typeForNewAccount = defaultType.intValue();
                    values.put(COLUMN_FOR_TYPE, defaultType.intValue());
                    if (oldType != null && oldType == TYPE_CUSTOM) {
                        values.remove(COLUMN_FOR_LABEL);
                    }
                } else {
                    typeForNewAccount = null;
                    values.remove(COLUMN_FOR_TYPE);
                }
            } else {
                typeForNewAccount = oldType;
            }
            if (typeForNewAccount != null) {
                final int specificMax = (typeSpecificMaxMap.containsKey(typeForNewAccount) ?
                        typeSpecificMaxMap.get(typeForNewAccount) : 0);
                if (specificMax >= 0) {
                    final int currentCount = (currentEntryCount.get(typeForNewAccount) != null ?
                            currentEntryCount.get(typeForNewAccount) : 0);
                    if (currentCount >= specificMax) {
                        continue;
                    }
                    currentEntryCount.put(typeForNewAccount, currentCount + 1);
                }
            }
            newState.addEntry(ValuesDelta.fromAfter(values));
            totalCount++;
        }
    }

    // The following lines are provided and maintained by Mediatek Inc.
    private static String mSimAccountType = AccountType.ACCOUNT_TYPE_SIM;
    private static String mUsimAccountType = AccountType.ACCOUNT_TYPE_USIM;
    //UIM
    private static String mUimAccountType = AccountType.ACCOUNT_TYPE_UIM;
    //UIM
    public static boolean mIsSimType = false;
    public static boolean mHasSip = false;
    // The previous lines are provided and maintained by Mediatek Inc.
}
