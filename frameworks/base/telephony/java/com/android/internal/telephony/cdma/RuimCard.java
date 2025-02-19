/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.internal.telephony.cdma;

import com.android.internal.telephony.IccCard;
import android.util.Log;


/**
 * Note: this class shares common code with SimCard, consider a base class to minimize code
 * duplication.
 * {@hide}
 */
public final class RuimCard extends IccCard {

    RuimCard(CDMAPhone phone, String LOG_TAG, boolean dbg) {
        super(phone, LOG_TAG, dbg);
        mPhone.mCM.registerForRUIMLockedOrAbsent(mHandler, EVENT_ICC_LOCKED_OR_ABSENT, null);
        mPhone.mCM.registerForOffOrNotAvailable(mHandler, EVENT_RADIO_OFF_OR_NOT_AVAILABLE, null);
        mPhone.mCM.registerForRUIMReady(mHandler, EVENT_ICC_READY, null);
        mPhone.mCM.registerForPhbReady(mHandler, EVENT_PHB_READY, null);
	mPhone.mCM.registerForSimInsertedStatus(mHandler, EVENT_UIM_INSERT_STATUS, null);
	Log.d(LOG_TAG, "registerForSimInsertedStatus + registerForSimMissing ");
        updateStateProperty();
    }

    @Override
    public void dispose() {
        super.dispose();
        //Unregister for all events
        mPhone.mCM.unregisterForRUIMLockedOrAbsent(mHandler);
        mPhone.mCM.unregisterForOffOrNotAvailable(mHandler);
        mPhone.mCM.unregisterForRUIMReady(mHandler);
        mPhone.mCM.unregisterForPhbReady(mHandler);
	mPhone.mCM.unregisterForSimInsertedStatus(mHandler);
    }

    @Override
    public String getServiceProviderName () {
        return mPhone.mIccRecords.getServiceProviderName();
    }
 }

