/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.internal.telephony;

import com.android.internal.telephony.RILConstants;

import android.util.Log;

/**
 * {@hide}
 */
public class CommandException extends RuntimeException {
    private Error e;

    //MTK-START [mtk04070][111118][ALPS00093395]Add some enum values
    // Please sync with RILConstants.java from RIL_Errno in ril.h
    public enum Error {
        INVALID_RESPONSE,
        RADIO_NOT_AVAILABLE,
        GENERIC_FAILURE,
        PASSWORD_INCORRECT,
        SIM_PIN2,
        SIM_PUK2,
        REQUEST_NOT_SUPPORTED,
        REQUEST_CANCELLED,
        OP_NOT_ALLOWED_DURING_VOICE_CALL,
        OP_NOT_ALLOWED_BEFORE_REG_NW,
        SMS_FAIL_RETRY,
        SIM_ABSENT,
        SUBSCRIPTION_NOT_AVAILABLE,
        MODE_NOT_SUPPORTED,
        FDN_CHECK_FAILURE,
        CALL_BARRED,
        SIM_MEM_FULL,
        DIAL_STRING_TOO_LONG,
        TEXT_STRING_TOO_LONG,
        NOT_READY,
        ILLEGAL_SIM_OR_ME,
        BT_SAP_UNDEFINED,
        BT_SAP_NOT_ACCESSIBLE,
        BT_SAP_CARD_REMOVED,
        ADDITIONAL_NUMBER_STRING_TOO_LONG,        
        ADN_LIST_NOT_EXIST,
        EMAIL_SIZE_LIMIT,
	EMAIL_NAME_TOOLONG,
        // NFC SEEK start
        MISSING_RESOURCE,
        NO_SUCH_ELEMENT,
        INVALID_PARAMETER,
        // NFC SEEK end
    }
    //MTK-END [mtk04070][111118][ALPS00093395]Add some enum values

    public CommandException(Error e) {
        super(e.toString());
        this.e = e;
    }

    public static CommandException
    fromRilErrno(int ril_errno) {
        switch(ril_errno) {
            case RILConstants.SUCCESS:                       return null;
            case RILConstants.RIL_ERRNO_INVALID_RESPONSE:
                return new CommandException(Error.INVALID_RESPONSE);
            case RILConstants.RADIO_NOT_AVAILABLE:
                return new CommandException(Error.RADIO_NOT_AVAILABLE);
            case RILConstants.GENERIC_FAILURE:
                return new CommandException(Error.GENERIC_FAILURE);
            case RILConstants.PASSWORD_INCORRECT:
                return new CommandException(Error.PASSWORD_INCORRECT);
            case RILConstants.SIM_PIN2:
                return new CommandException(Error.SIM_PIN2);
            case RILConstants.SIM_PUK2:
                return new CommandException(Error.SIM_PUK2);
            case RILConstants.REQUEST_NOT_SUPPORTED:
                return new CommandException(Error.REQUEST_NOT_SUPPORTED);
            case RILConstants.OP_NOT_ALLOWED_DURING_VOICE_CALL:
                return new CommandException(Error.OP_NOT_ALLOWED_DURING_VOICE_CALL);
            case RILConstants.OP_NOT_ALLOWED_BEFORE_REG_NW:
                return new CommandException(Error.OP_NOT_ALLOWED_BEFORE_REG_NW);
            case RILConstants.SMS_SEND_FAIL_RETRY:
                return new CommandException(Error.SMS_FAIL_RETRY);
            case RILConstants.SIM_ABSENT:
                return new CommandException(Error.SIM_ABSENT);
            case RILConstants.SUBSCRIPTION_NOT_AVAILABLE:
                return new CommandException(Error.SUBSCRIPTION_NOT_AVAILABLE);
            case RILConstants.MODE_NOT_SUPPORTED:
                return new CommandException(Error.MODE_NOT_SUPPORTED);
            case RILConstants.FDN_CHECK_FAILURE:
                return new CommandException(Error.FDN_CHECK_FAILURE);
            case RILConstants.ILLEGAL_SIM_OR_ME:
                return new CommandException(Error.ILLEGAL_SIM_OR_ME);
            // NFC SEEK start
            case RILConstants.MISSING_RESOURCE:
                return new CommandException(Error.MISSING_RESOURCE);
            case RILConstants.NO_SUCH_ELEMENT:
                return new CommandException(Error.NO_SUCH_ELEMENT);
            case RILConstants.INVALID_PARAMETER:
                return new CommandException(Error.INVALID_PARAMETER);
            // NFC SEEK end
            //MTK-START [mtk04070][111118][ALPS00093395]Add some cases according to enum Error 
            case RILConstants.REQUEST_CANCELLED:
                return new CommandException(Error.REQUEST_CANCELLED);
            case RILConstants.CALL_BARRED:
                return new CommandException(Error.CALL_BARRED); 
            case RILConstants.DIAL_STRING_TOO_LONG:
                return new CommandException(Error.DIAL_STRING_TOO_LONG); 
            case RILConstants.TEXT_STRING_TOO_LONG:
                return new CommandException(Error.TEXT_STRING_TOO_LONG); 
            case RILConstants.SIM_MEM_FULL:
                return new CommandException(Error.SIM_MEM_FULL); 
            case RILConstants.BT_SAP_UNDEFINED:
                return new CommandException(Error.BT_SAP_UNDEFINED);
            case RILConstants.BT_SAP_NOT_ACCESSIBLE:
                return new CommandException(Error.BT_SAP_NOT_ACCESSIBLE);   
            case RILConstants.BT_SAP_CARD_REMOVED:
                return new CommandException(Error.BT_SAP_CARD_REMOVED);
            case RILConstants.ADDITIONAL_NUMBER_STRING_TOO_LONG:
                return new CommandException(Error.ADDITIONAL_NUMBER_STRING_TOO_LONG);
            case RILConstants.ADN_LIST_NOT_EXIST:
                return new CommandException(Error.ADN_LIST_NOT_EXIST);
	    case RILConstants.EMAIL_SIZE_LIMIT:
		return new CommandException(Error.EMAIL_SIZE_LIMIT);
	    case RILConstants.EMAIL_NAME_TOOLONG:
		return new CommandException(Error.EMAIL_NAME_TOOLONG);
            //MTK-END [mtk04070][111118][ALPS00093395]Add some cases according to enum Error 
            default:
                Log.e("GSM", "Unrecognized RIL errno " + ril_errno);
                return new CommandException(Error.INVALID_RESPONSE);
        }
    }

    public Error getCommandError() {
        return e;
    }



}
