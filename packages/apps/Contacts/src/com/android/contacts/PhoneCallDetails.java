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
 * limitations under the License.
 */

package com.android.contacts;

import android.net.Uri;
import android.provider.CallLog.Calls;
import android.provider.ContactsContract.CommonDataKinds.Phone;

/**
 * The details of a phone call to be shown in the UI.
 */
public class PhoneCallDetails {
    /** The number of the other party involved in the call. */
    public final CharSequence number;
    /** The formatted version of {@link #number}. */
    public final CharSequence formattedNumber;
    /** The country corresponding with the phone number. */
    public final String countryIso;
    /** The geocoded location for the phone number. */
    public final String geocode;
    /**
     * The type of calls, as defined in the call log table, e.g.,
     * {@link Calls#INCOMING_TYPE}.
     * <p>
     * There might be multiple types if this represents a set of entries grouped
     * together.
     */
    
    /**
    * Change Feature by Mediatek Begin.
    * Original Android's Code:
    * public final int[] callTypes;
    * Descriptions:
    */

    public final int callType;
    
    public final int callCount;
    /**
    * Change Feature by Mediatek End.
    */

    /** The date of the call, in milliseconds since the epoch. */
    public final long date;
    /** The duration of the call in milliseconds, or 0 for missed calls. */
    public final long duration;
    /** The name of the contact, or the empty string. */
    public final CharSequence name;
    /** The type of phone, e.g., {@link Phone#TYPE_HOME}, 0 if not available. */
    public final int numberType;
    /** The custom label associated with the phone number in the contact, or the empty string. */
    public final CharSequence numberLabel;
    /** The URI of the contact associated with this phone call. */
    public final Uri contactUri;
    /**
     * The photo URI of the picture of the contact that is associated with this phone call or
     * null if there is none.
     * <p>
     * This is meant to store the high-res photo only.
     */
    public final Uri photoUri;
    
    /**
    * Change Feature by Mediatek Begin.
        * Original Android's Code:
       /** Create the details for a call with a number not associated with a contact. 
        public PhoneCallDetails(CharSequence number, CharSequence formattedNumber,
                String countryIso, String geocode, int[] callTypes, long date, long duration) {
            this(number, formattedNumber, countryIso, geocode, callTypes, date, duration, "", 0, "",
                    null, null);
        }
        */

        /** Create the details for a call with a number associated with a contact. 
        public PhoneCallDetails(CharSequence number, CharSequence formattedNumber,
                String countryIso, String geocode, int[] callTypes, long date, long duration,
                CharSequence name, int numberType, CharSequence numberLabel, Uri contactUri,
                Uri photoUri) {
            this.number = number;
            this.formattedNumber = formattedNumber;
            this.countryIso = countryIso;
            this.geocode = geocode;
            this.callTypes = callTypes;
            this.date = date;
            this.duration = duration;
            this.name = name;
            this.numberType = numberType;
            this.numberLabel = numberLabel;
            this.contactUri = contactUri;
            this.photoUri = photoUri;
        }
    }
    * Descriptions:Add simId,vtCall,call count in PhoneCallDetails.
    */
    public final int simId;
    public final int vtCall;
    public final String ipPrefix;
    public final int contactSimId;///M:AAS

    /**
     * Create the details for a call with a number not associated with a
     * contact.
     */
    public PhoneCallDetails(CharSequence number, CharSequence formattedNumber, String countryIso,
            String geocode, int callType, long date, long duration, int simId, int vtCall, int callCount, String ipPrefix) {
        this(number, formattedNumber, countryIso, geocode, callType, date, duration, "", 0, "",
                null, null, simId, vtCall, callCount, ipPrefix);
    }

    /** Create the details for a call with a number associated with a contact. */
    public PhoneCallDetails(CharSequence number, CharSequence formattedNumber, String countryIso,
            String geocode, int callType, long date, long duration, CharSequence name,
            int numberType, CharSequence numberLabel, Uri contactUri, Uri photoUri, int simId,
            int vtCall, int callCount, String ipPrefix) {
        this(number, formattedNumber, countryIso, geocode, callType, date, duration,
                name, numberType, numberLabel, contactUri, photoUri, simId, vtCall, callCount,
                ipPrefix, -1);
    }
    /**
    * Change Feature by Mediatek End.
    */

    // The following lines are provided and maintained by Mediatek Inc.
    ///M:AAS
    public PhoneCallDetails(CharSequence number, CharSequence formattedNumber, String countryIso,
            String geocode, int callType, long date, long duration, int simId, int vtCall, 
            int callCount, String ipPrefix, int contactSimId) {
        this(number, formattedNumber, countryIso, geocode, callType, date, duration, "", 0, "",
                null, null, simId, vtCall, callCount, ipPrefix, contactSimId);
    }
    
    public PhoneCallDetails(CharSequence number, CharSequence formattedNumber, String countryIso,
            String geocode, int callType, long date, long duration, CharSequence name,
            int numberType, CharSequence numberLabel, Uri contactUri, Uri photoUri, int simId,
            int vtCall, int callCount, String ipPrefix, int contactSimId) {
        this.number = number;
        this.formattedNumber = formattedNumber;
        this.countryIso = countryIso;
        this.geocode = geocode;
        this.callType = callType;
        this.date = date;
        this.duration = duration;
        this.name = name;
        this.numberType = numberType;
        this.numberLabel = numberLabel;
        this.contactUri = contactUri;
        this.photoUri = photoUri;
        this.simId = simId;
        this.vtCall = vtCall;
        this.callCount = callCount;
        this.ipPrefix = ipPrefix;
        this.contactSimId = contactSimId; //different with simId.
    }
    // The previous lines are provided and maintained by Mediatek Inc.

}