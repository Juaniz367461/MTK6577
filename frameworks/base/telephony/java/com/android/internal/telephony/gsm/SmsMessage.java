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

package com.android.internal.telephony.gsm;

import android.os.Parcel;
import android.telephony.PhoneNumberUtils;
import android.text.format.Time;
import android.util.Log;
import com.android.internal.telephony.IccUtils;
import com.android.internal.telephony.EncodeException;
import com.android.internal.telephony.GsmAlphabet;
import com.android.internal.telephony.SmsHeader;
import com.android.internal.telephony.SmsMessageBase;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import static android.telephony.SmsMessage.ENCODING_7BIT;
import static android.telephony.SmsMessage.ENCODING_8BIT;
import static android.telephony.SmsMessage.ENCODING_16BIT;
import static android.telephony.SmsMessage.ENCODING_KSC5601;
import static android.telephony.SmsMessage.ENCODING_UNKNOWN;
import static android.telephony.SmsMessage.MAX_USER_DATA_BYTES;
import static android.telephony.SmsMessage.MAX_USER_DATA_BYTES_WITH_HEADER;
import static android.telephony.SmsMessage.MAX_USER_DATA_SEPTETS;
import static android.telephony.SmsMessage.MAX_USER_DATA_SEPTETS_WITH_HEADER;
import static android.telephony.SmsMessage.MessageClass;

// MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
import android.os.SystemProperties;
import android.util.Config;
import com.android.internal.telephony.SmsMessageBase.TextEncodingDetails;
import static android.telephony.SmsMessage.MWI_VOICEMAIL;
import static android.telephony.SmsMessage.MWI_FAX;
import static android.telephony.SmsMessage.MWI_EMAIL;
import static android.telephony.SmsMessage.MWI_OTHER;
import static android.telephony.SmsMessage.MWI_VIDEO;
// MTK-END   [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16

import com.android.internal.telephony.IMessageWaitingExt;

/**
 * A Short Message Service message.
 *
 */
public class SmsMessage extends SmsMessageBase {
    static final String LOG_TAG = "GSM";

    private MessageClass messageClass;

    /**
     * TP-Message-Type-Indicator
     * 9.2.3
     */
    private int mti;

    /** TP-Protocol-Identifier (TP-PID) */
    private int protocolIdentifier;

    // TP-Data-Coding-Scheme
    // see TS 23.038
    private int dataCodingScheme;

    // TP-Reply-Path
    // e.g. 23.040 9.2.2.1
    private boolean replyPathPresent = false;

    // "Message Marked for Automatic Deletion Group"
    // 23.038 Section 4
    private boolean automaticDeletion;

    /** True if Status Report is for SMS-SUBMIT; false for SMS-COMMAND. */
    private boolean forSubmit;

    /** The address of the receiver. */
    private GsmSmsAddress recipientAddress;

    /** Time when SMS-SUBMIT was delivered from SC to MSE. */
    private long dischargeTimeMillis;

    /**
     *  TP-Status - status of a previously submitted SMS.
     *  This field applies to SMS-STATUS-REPORT messages.  0 indicates success;
     *  see TS 23.040, 9.2.3.15 for description of other possible values.
     */
    private int status;

    /**
     *  TP-Status - status of a previously submitted SMS.
     *  This field is true iff the message is a SMS-STATUS-REPORT message.
     */
    private boolean isStatusReportMessage = false;
    
    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    public static final int ENCODING_7BIT_SINGLE = 11;
    public static final int ENCODING_7BIT_LOCKING = 12;
    public static final int ENCODING_7BIT_LOCKING_SINGLE = 13;

    public static final int MASK_MESSAGE_TYPE_INDICATOR     = 0x03;
    public static final int MASK_VALIDITY_PERIOD_FORMAT     = 0x18;
    public static final int MASK_USER_DATA_HEADER_INDICATOR = 0x40;

    public static final int MASK_VALIDITY_PERIOD_FORMAT_NONE = 0x00;
    public static final int MASK_VALIDITY_PERIOD_FORMAT_RELATIVE = 0x10;
    public static final int MASK_VALIDITY_PERIOD_FORMAT_ENHANCED = 0x08;
    public static final int MASK_VALIDITY_PERIOD_FORMAT_ABSOLUTE = 0x18;
    // MTK-END [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16

    public static class SubmitPdu extends SubmitPduBase {}
    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    public static class DeliverPdu extends DeliverPduBase {}
    // MTK-END   [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16

    /**
     * Create an SmsMessage from a raw PDU.
     */
    public static SmsMessage createFromPdu(byte[] pdu) {
        try {
            SmsMessage msg = new SmsMessage();
            msg.parsePdu(pdu);
            return msg;
        } catch (RuntimeException ex) {
            Log.e(LOG_TAG, "SMS PDU parsing failed: ", ex);
            return null;
        }
    }

    /**
     * 3GPP TS 23.040 9.2.3.9 specifies that Type Zero messages are indicated
     * by TP_PID field set to value 0x40
     */
    public boolean isTypeZero() {
        return (protocolIdentifier == 0x40);
    }

    /**
     * TS 27.005 3.4.1 lines[0] and lines[1] are the two lines read from the
     * +CMT unsolicited response (PDU mode, of course)
     *  +CMT: [&lt;alpha>],<length><CR><LF><pdu>
     *
     * Only public for debugging
     *
     * {@hide}
     */
    public static SmsMessage newFromCMT(String[] lines) {
        try {
            SmsMessage msg = new SmsMessage();
            msg.parsePdu(IccUtils.hexStringToBytes(lines[1]));
            return msg;
        } catch (RuntimeException ex) {
            Log.e(LOG_TAG, "SMS PDU parsing failed: ", ex);
            return null;
        }
    }

    /** @hide */
    public static SmsMessage newFromCDS(String line) {
        try {
            SmsMessage msg = new SmsMessage();
            msg.parsePdu(IccUtils.hexStringToBytes(line));
            return msg;
        } catch (RuntimeException ex) {
            Log.e(LOG_TAG, "CDS SMS PDU parsing failed: ", ex);
            return null;
        }
    }

    /**
     * Create an SmsMessage from an SMS EF record.
     *
     * @param index Index of SMS record. This should be index in ArrayList
     *              returned by SmsManager.getAllMessagesFromSim + 1.
     * @param data Record data.
     * @return An SmsMessage representing the record.
     *
     * @hide
     */
    public static SmsMessage createFromEfRecord(int index, byte[] data) {
        try {
            SmsMessage msg = new SmsMessage();

            msg.indexOnIcc = index;

            // First byte is status: RECEIVED_READ, RECEIVED_UNREAD, STORED_SENT,
            // or STORED_UNSENT
            // See TS 51.011 10.5.3
            if ((data[0] & 1) == 0) {
                Log.w(LOG_TAG,
                        "SMS parsing failed: Trying to parse a free record");
                return null;
            } else {
                msg.statusOnIcc = data[0] & 0x07;
            }

            int size = data.length - 1;

            // Note: Data may include trailing FF's.  That's OK; message
            // should still parse correctly.
            byte[] pdu = new byte[size];
            System.arraycopy(data, 1, pdu, 0, size);
            msg.parsePdu(pdu);
            return msg;
        } catch (RuntimeException ex) {
            Log.e(LOG_TAG, "SMS PDU parsing failed: ", ex);
            return null;
        }
    }

    /**
     * Get the TP-Layer-Length for the given SMS-SUBMIT PDU Basically, the
     * length in bytes (not hex chars) less the SMSC header
     */
    public static int getTPLayerLengthForPDU(String pdu) {
        int len = pdu.length() / 2;
        int smscLen = Integer.parseInt(pdu.substring(0, 2), 16);

        return len - smscLen - 1;
    }

    /**
     * Get an SMS-SUBMIT PDU for a destination address and a message
     *
     * @param scAddress Service Centre address.  Null means use default.
     * @return a <code>SubmitPdu</code> containing the encoded SC
     *         address, if applicable, and the encoded message.
     *         Returns null on encode error.
     * @hide
     */
    public static SubmitPdu getSubmitPdu(String scAddress,
            String destinationAddress, String message,
            boolean statusReportRequested, byte[] header) {
        return getSubmitPdu(scAddress, destinationAddress, message, statusReportRequested, header,
                ENCODING_UNKNOWN, 0, 0);
    }


    /**
     * Get an SMS-SUBMIT PDU for a destination address and a message using the
     * specified encoding.
     *
     * @param scAddress Service Centre address.  Null means use default.
     * @param encoding Encoding defined by constants in android.telephony.SmsMessage.ENCODING_*
     * @param languageTable
     * @param languageShiftTable
     * @return a <code>SubmitPdu</code> containing the encoded SC
     *         address, if applicable, and the encoded message.
     *         Returns null on encode error.
     * @hide
     */
    public static SubmitPdu getSubmitPdu(String scAddress,
            String destinationAddress, String message,
            boolean statusReportRequested, byte[] header, int encoding,
            int languageTable, int languageShiftTable) {

        // Perform null parameter checks.
        if (message == null || destinationAddress == null) {
            return null;
        }

        if (encoding == ENCODING_UNKNOWN) {
            // Find the best encoding to use
            TextEncodingDetails ted = calculateLength(message, false);
            encoding = ted.codeUnitSize;
            languageTable = ted.languageTable;
            languageShiftTable = ted.languageShiftTable;

            if (encoding == ENCODING_7BIT && (languageTable != 0 || languageShiftTable != 0)) {
                if (header != null) {
                    SmsHeader smsHeader = SmsHeader.fromByteArray(header);
                    if (smsHeader.languageTable != languageTable
                            || smsHeader.languageShiftTable != languageShiftTable) {
                        Log.w(LOG_TAG, "Updating language table in SMS header: "
                                + smsHeader.languageTable + " -> " + languageTable + ", "
                                + smsHeader.languageShiftTable + " -> " + languageShiftTable);
                        smsHeader.languageTable = languageTable;
                        smsHeader.languageShiftTable = languageShiftTable;
                        header = SmsHeader.toByteArray(smsHeader);
                    }
                } else {
                    SmsHeader smsHeader = new SmsHeader();
                    smsHeader.languageTable = languageTable;
                    smsHeader.languageShiftTable = languageShiftTable;
                    header = SmsHeader.toByteArray(smsHeader);
                }
            }
        }

        SubmitPdu ret = new SubmitPdu();
        // MTI = SMS-SUBMIT, UDHI = header != null
        byte mtiByte = (byte)(0x01 | (header != null ? 0x40 : 0x00));
        ByteArrayOutputStream bo = getSubmitPduHead(
                scAddress, destinationAddress, mtiByte,
                statusReportRequested, ret);

        // User Data (and length)
        byte[] userData;
        try {
            if (encoding == ENCODING_7BIT) {
                userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header,
                        languageTable, languageShiftTable);
            } else { //assume UCS-2
                try {
                    userData = encodeUCS2(message, header);
                } catch(UnsupportedEncodingException uex) {
                    Log.e(LOG_TAG,
                            "Implausible UnsupportedEncodingException ",
                            uex);
                    return null;
                }
            }
        } catch (EncodeException ex) {
            // Encoding to the 7-bit alphabet failed. Let's see if we can
            // send it as a UCS-2 encoded message
            try {
                userData = encodeUCS2(message, header);
                encoding = ENCODING_16BIT;
            } catch(UnsupportedEncodingException uex) {
                Log.e(LOG_TAG,
                        "Implausible UnsupportedEncodingException ",
                        uex);
                return null;
            }
        }

        if (encoding == ENCODING_7BIT) {
            if ((0xff & userData[0]) > MAX_USER_DATA_SEPTETS) {
                // Message too long
                //MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
                Log.e(LOG_TAG, "Message too long (" + (0xff & userData[0]) + " septets)");
                //MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
                return null;
            }
            // TP-Data-Coding-Scheme
            // Default encoding, uncompressed
            // To test writing messages to the SIM card, change this value 0x00
            // to 0x12, which means "bits 1 and 0 contain message class, and the
            // class is 2". Note that this takes effect for the sender. In other
            // words, messages sent by the phone with this change will end up on
            // the receiver's SIM card. You can then send messages to yourself
            // (on a phone with this change) and they'll end up on the SIM card.
            bo.write(0x00);
        } else { // assume UCS-2
            if ((0xff & userData[0]) > MAX_USER_DATA_BYTES) {
                // Message too long
                //MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
                Log.e(LOG_TAG, "Message too long (" + (0xff & userData[0]) + " bytes)");
                //MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
                return null;
            }
            // TP-Data-Coding-Scheme
            // UCS-2 encoding, uncompressed
            bo.write(0x08);
        }

        // (no TP-Validity-Period)
        bo.write(userData, 0, userData.length);
        ret.encodedMessage = bo.toByteArray();
        return ret;
    }

    /**
     * Packs header and UCS-2 encoded message. Includes TP-UDL & TP-UDHL if necessary
     *
     * @return
     * @throws UnsupportedEncodingException
     */
    private static byte[] encodeUCS2(String message, byte[] header)
        throws UnsupportedEncodingException {
        byte[] userData, textPart;
        textPart = message.getBytes("utf-16be");

        if (header != null) {
            // Need 1 byte for UDHL
            userData = new byte[header.length + textPart.length + 1];

            userData[0] = (byte)header.length;
            System.arraycopy(header, 0, userData, 1, header.length);
            System.arraycopy(textPart, 0, userData, header.length + 1, textPart.length);
        }
        else {
            userData = textPart;
        }
        byte[] ret = new byte[userData.length+1];
        ret[0] = (byte) (userData.length & 0xff );
        System.arraycopy(userData, 0, ret, 1, userData.length);
        return ret;
    }

    /**
     * Get an SMS-SUBMIT PDU for a destination address and a message
     *
     * @param scAddress Service Centre address.  Null means use default.
     * @return a <code>SubmitPdu</code> containing the encoded SC
     *         address, if applicable, and the encoded message.
     *         Returns null on encode error.
     */
    public static SubmitPdu getSubmitPdu(String scAddress,
            String destinationAddress, String message,
            boolean statusReportRequested) {

        return getSubmitPdu(scAddress, destinationAddress, message, statusReportRequested, null);
    }

    /**
     * Get an SMS-SUBMIT PDU for a data message to a destination address &amp; port
     *
     * @param scAddress Service Centre address. null == use default
     * @param destinationAddress the address of the destination for the message
     * @param destinationPort the port to deliver the message to at the
     *        destination
     * @param data the data for the message
     * @return a <code>SubmitPdu</code> containing the encoded SC
     *         address, if applicable, and the encoded message.
     *         Returns null on encode error.
     */
    public static SubmitPdu getSubmitPdu(String scAddress,
            String destinationAddress, int destinationPort, byte[] data,
            boolean statusReportRequested) {

        SmsHeader.PortAddrs portAddrs = new SmsHeader.PortAddrs();
        portAddrs.destPort = destinationPort;
        portAddrs.origPort = 0;
        portAddrs.areEightBits = false;

        SmsHeader smsHeader = new SmsHeader();
        smsHeader.portAddrs = portAddrs;

        byte[] smsHeaderData = SmsHeader.toByteArray(smsHeader);

        if ((data.length + smsHeaderData.length + 1) > MAX_USER_DATA_BYTES) {
            Log.e(LOG_TAG, "SMS data message may only contain "
                    + (MAX_USER_DATA_BYTES - smsHeaderData.length - 1) + " bytes");
            return null;
        }

        SubmitPdu ret = new SubmitPdu();
        ByteArrayOutputStream bo = getSubmitPduHead(
                scAddress, destinationAddress, (byte) 0x41, // MTI = SMS-SUBMIT,
                                                            // TP-UDHI = true
                statusReportRequested, ret);

        // TP-Data-Coding-Scheme
        // No class, 8 bit data
        bo.write(0x04);

        // (no TP-Validity-Period)

        // Total size
        bo.write(data.length + smsHeaderData.length + 1);

        // User data header
        bo.write(smsHeaderData.length);
        bo.write(smsHeaderData, 0, smsHeaderData.length);

        // User data
        bo.write(data, 0, data.length);

        ret.encodedMessage = bo.toByteArray();
        return ret;
    }

    /**
     * Create the beginning of a SUBMIT PDU.  This is the part of the
     * SUBMIT PDU that is common to the two versions of {@link #getSubmitPdu},
     * one of which takes a byte array and the other of which takes a
     * <code>String</code>.
     *
     * @param scAddress Service Centre address. null == use default
     * @param destinationAddress the address of the destination for the message
     * @param mtiByte
     * @param ret <code>SubmitPdu</code> containing the encoded SC
     *        address, if applicable, and the encoded message
     */
    private static ByteArrayOutputStream getSubmitPduHead(
            String scAddress, String destinationAddress, byte mtiByte,
            boolean statusReportRequested, SubmitPdu ret) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream(
                MAX_USER_DATA_BYTES + 40);

        // SMSC address with length octet, or 0
        if (scAddress == null) {
            ret.encodedScAddress = null;
        } else {
            ret.encodedScAddress = PhoneNumberUtils.networkPortionToCalledPartyBCDWithLength(
                    scAddress);
        }

        // TP-Message-Type-Indicator (and friends)
        if (statusReportRequested) {
            // Set TP-Status-Report-Request bit.
            mtiByte |= 0x20;
            if (false) Log.d(LOG_TAG, "SMS status report requested");
        }
        bo.write(mtiByte);

        // space for TP-Message-Reference
        bo.write(0);

        byte[] daBytes;

        daBytes = PhoneNumberUtils.networkPortionToCalledPartyBCD(destinationAddress);

        // destination address length in BCD digits, ignoring TON byte and pad
        // TODO Should be better.
        if (daBytes != null) {
            bo.write((daBytes.length - 1) * 2
                     - ((daBytes[daBytes.length - 1] & 0xf0) == 0xf0 ? 1 : 0));

            // destination address
            bo.write(daBytes, 0, daBytes.length);
        } else {
            // TP-Protocol-Identifier
            Log.d(LOG_TAG, "write an empty address for submit pdu");
            bo.write(0);
            bo.write(PhoneNumberUtils.TOA_Unknown);
        }
        
        // TP-Protocol-Identifier
        bo.write(0);
        
        return bo;
    }

    private static class PduParser {
        byte pdu[];
        int cur;
        SmsHeader userDataHeader;
        byte[] userData;
        int mUserDataSeptetPadding;
        int mUserDataSize;

        PduParser(byte[] pdu) {
            this.pdu = pdu;
            cur = 0;
            mUserDataSeptetPadding = 0;
        }

        /**
         * Parse and return the SC address prepended to SMS messages coming via
         * the TS 27.005 / AT interface.  Returns null on invalid address
         */
        String getSCAddress() {
            int len;
            String ret;

            // length of SC Address
            len = getByte();

            if (len == 0) {
                // no SC address
                ret = null;
            } else {
                // SC address
                try {
                    ret = PhoneNumberUtils
                            .calledPartyBCDToString(pdu, cur, len);
                } catch (RuntimeException tr) {
                    Log.d(LOG_TAG, "invalid SC address: ", tr);
                    ret = null;
                }
            }

            cur += len;

            return ret;
        }

        /**
         * returns non-sign-extended byte value
         */
        int getByte() {
            return pdu[cur++] & 0xff;
        }

        /**
         * Any address except the SC address (eg, originating address) See TS
         * 23.040 9.1.2.5
         */
        GsmSmsAddress getAddress() {
            GsmSmsAddress ret;

            // "The Address-Length field is an integer representation of
            // the number field, i.e. excludes any semi-octet containing only
            // fill bits."
            // The TOA field is not included as part of this
            int addressLength = pdu[cur] & 0xff;
            int lengthBytes = 2 + (addressLength + 1) / 2;

            ret = new GsmSmsAddress(pdu, cur, lengthBytes);

            cur += lengthBytes;

            return ret;
        }

        /**
         * Parses an SC timestamp and returns a currentTimeMillis()-style
         * timestamp
         */

        long getSCTimestampMillis() {
            // TP-Service-Centre-Time-Stamp
            int year = IccUtils.gsmBcdByteToInt(pdu[cur++]);
            int month = IccUtils.gsmBcdByteToInt(pdu[cur++]);
            int day = IccUtils.gsmBcdByteToInt(pdu[cur++]);
            int hour = IccUtils.gsmBcdByteToInt(pdu[cur++]);
            int minute = IccUtils.gsmBcdByteToInt(pdu[cur++]);
            int second = IccUtils.gsmBcdByteToInt(pdu[cur++]);

            // For the timezone, the most significant bit of the
            // least significant nibble is the sign byte
            // (meaning the max range of this field is 79 quarter-hours,
            // which is more than enough)

            byte tzByte = pdu[cur++];

            // Mask out sign bit.
            int timezoneOffset = IccUtils.gsmBcdByteToInt((byte) (tzByte & (~0x08)));

            timezoneOffset = ((tzByte & 0x08) == 0) ? timezoneOffset : -timezoneOffset;

            Time time = new Time(Time.TIMEZONE_UTC);

            // It's 2006.  Should I really support years < 2000?
            time.year = year >= 90 ? year + 1900 : year + 2000;
            time.month = month - 1;
            time.monthDay = day;
            time.hour = hour;
            time.minute = minute;
            time.second = second;

            // Timezone offset is in quarter hours.
            return time.toMillis(true) - (timezoneOffset * 15 * 60 * 1000);
        }

        /**
         * Pulls the user data out of the PDU, and separates the payload from
         * the header if there is one.
         *
         * @param hasUserDataHeader true if there is a user data header
         * @param dataInSeptets true if the data payload is in septets instead
         *  of octets
         * @return the number of septets or octets in the user data payload
         */
        int constructUserData(boolean hasUserDataHeader, boolean dataInSeptets) {
            int offset = cur;
            int userDataLength = pdu[offset++] & 0xff;
            int headerSeptets = 0;
            int userDataHeaderLength = 0;

            if (hasUserDataHeader) {
                userDataHeaderLength = pdu[offset++] & 0xff;

                byte[] udh = new byte[userDataHeaderLength];
                System.arraycopy(pdu, offset, udh, 0, userDataHeaderLength);
                userDataHeader = SmsHeader.fromByteArray(udh);
                offset += userDataHeaderLength;

                int headerBits = (userDataHeaderLength + 1) * 8;
                headerSeptets = headerBits / 7;
                headerSeptets += (headerBits % 7) > 0 ? 1 : 0;
                mUserDataSeptetPadding = (headerSeptets * 7) - headerBits;
            }

            int bufferLen;
            if (dataInSeptets) {
                /*
                 * Here we just create the user data length to be the remainder of
                 * the pdu minus the user data header, since userDataLength means
                 * the number of uncompressed septets.
                 */
                bufferLen = pdu.length - offset;
            } else {
                /*
                 * userDataLength is the count of octets, so just subtract the
                 * user data header.
                 */
                bufferLen = userDataLength - (hasUserDataHeader ? (userDataHeaderLength + 1) : 0);
                if (bufferLen < 0) {
                    bufferLen = 0;
                }
            }

            userData = new byte[bufferLen];
            System.arraycopy(pdu, offset, userData, 0, userData.length);
            cur = offset;

            if (dataInSeptets) {
                // Return the number of septets
                int count = userDataLength - headerSeptets;
                // If count < 0, return 0 (means UDL was probably incorrect)
                return count < 0 ? 0 : count;
            } else {
                // Return the number of octets
                return userData.length;
            }
        }

        /**
         * Returns the user data payload, not including the headers
         *
         * @return the user data payload, not including the headers
         */
        byte[] getUserData() {
            return userData;
        }

        /**
         * Returns the number of padding bits at the beginning of the user data
         * array before the start of the septets.
         *
         * @return the number of padding bits at the beginning of the user data
         * array before the start of the septets
         */
        int getUserDataSeptetPadding() {
            return mUserDataSeptetPadding;
        }

        /**
         * Returns an object representing the user data headers
         *
         * {@hide}
         */
        SmsHeader getUserDataHeader() {
            return userDataHeader;
        }

        /**
         * Interprets the user data payload as packed GSM 7bit characters, and
         * decodes them into a String.
         *
         * @param septetCount the number of septets in the user data payload
         * @return a String with the decoded characters
         */
        String getUserDataGSM7Bit(int septetCount, int languageTable,
                int languageShiftTable) {
            String ret;

            ret = GsmAlphabet.gsm7BitPackedToString(pdu, cur, septetCount,
                    mUserDataSeptetPadding, languageTable, languageShiftTable);

            cur += (septetCount * 7) / 8;

            return ret;
        }

        /**
         * Interprets the user data payload as UCS2 characters, and
         * decodes them into a String.
         *
         * @param byteCount the number of bytes in the user data payload
         * @return a String with the decoded characters
         */
        String getUserDataUCS2(int byteCount) {
            String ret;

            try {
                ret = new String(pdu, cur, byteCount, "utf-16");
            } catch (UnsupportedEncodingException ex) {
                ret = "";
                Log.e(LOG_TAG, "implausible UnsupportedEncodingException", ex);
            }

            cur += byteCount;
            return ret;
        }

        /**
         * Interprets the user data payload as KSC-5601 characters, and
         * decodes them into a String.
         *
         * @param byteCount the number of bytes in the user data payload
         * @return a String with the decoded characters
         */
        String getUserDataKSC5601(int byteCount) {
            String ret;

            try {
                ret = new String(pdu, cur, byteCount, "KSC5601");
            } catch (UnsupportedEncodingException ex) {
                ret = "";
                Log.e(LOG_TAG, "implausible UnsupportedEncodingException", ex);
            }

            cur += byteCount;
            return ret;
        }

        boolean moreDataPresent() {
            return (pdu.length > cur);
        }
    }

    /**
     * Calculate the number of septets needed to encode the message.
     *
     * @param msgBody the message to encode
     * @param use7bitOnly ignore (but still count) illegal characters if true
     * @return TextEncodingDetails
     */
    public static TextEncodingDetails calculateLength(CharSequence msgBody,
            boolean use7bitOnly) {
        TextEncodingDetails ted = GsmAlphabet.countGsmSeptets(msgBody, use7bitOnly);
        if (ted == null) {
            ted = new TextEncodingDetails();
            int octets = msgBody.length() * 2;
            ted.codeUnitCount = msgBody.length();
            if (octets > MAX_USER_DATA_BYTES) {
                ted.msgCount = (octets + (MAX_USER_DATA_BYTES_WITH_HEADER - 1)) /
                        MAX_USER_DATA_BYTES_WITH_HEADER;
                ted.codeUnitsRemaining = ((ted.msgCount *
                        MAX_USER_DATA_BYTES_WITH_HEADER) - octets) / 2;
            } else {
                ted.msgCount = 1;
                ted.codeUnitsRemaining = (MAX_USER_DATA_BYTES - octets)/2;
            }
            ted.codeUnitSize = ENCODING_16BIT;
        }
        return ted;
    }

    /** {@inheritDoc} */
    @Override
    public int getProtocolIdentifier() {
        return protocolIdentifier;
    }

    //MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
    /**
     * Returns the TP-Data-Coding-Scheme byte, for acknowledgement of SMS-PP download messages.
     * @return the TP-DCS field of the SMS header
     */
    int getDataCodingScheme() {
        return dataCodingScheme;
    }
    //MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3

    /** {@inheritDoc} */
    @Override
    public boolean isReplace() {
        return (protocolIdentifier & 0xc0) == 0x40
                && (protocolIdentifier & 0x3f) > 0
                && (protocolIdentifier & 0x3f) < 8;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCphsMwiMessage() {
        return ((GsmSmsAddress) originatingAddress).isCphsVoiceMessageClear()
                || ((GsmSmsAddress) originatingAddress).isCphsVoiceMessageSet();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMWIClearMessage() {
        try {
            if(userDataHeader != null) {
                IMessageWaitingExt iMwi = (IMessageWaitingExt)userDataHeader;
                if(iMwi.getVoiceMailCount() >= 0 && iMwi.getVoiceMailCount() == 0) {
                    return true;
                }
            }
        } catch(ClassCastException ex) {
            Log.w(LOG_TAG, "SmsHeader unsupports IMessageWaitingExt");
        }
        
        if (isMwi && !mwiSense) {
            return true;
        }

        return originatingAddress != null
                && ((GsmSmsAddress) originatingAddress).isCphsVoiceMessageClear();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMWISetMessage() {
        try {
            if(userDataHeader != null) {
                IMessageWaitingExt iMwi = (IMessageWaitingExt)userDataHeader;
                if(iMwi.getVoiceMailCount() >= 0 && iMwi.getVoiceMailCount() > 0) {
                    return true;
                }
            }
        } catch(ClassCastException ex) {
            Log.w(LOG_TAG, "SmsHeader unsupports IMessageWaitingExt");
        }
        
        if (isMwi && mwiSense) {
            return true;
        }

        return originatingAddress != null
                && ((GsmSmsAddress) originatingAddress).isCphsVoiceMessageSet();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isMwiDontStore() {
        if (isMwi && mwiDontStore) {
            return true;
        }

        if (isCphsMwiMessage()) {
            // See CPHS 4.2 Section B.4.2.1
            // If the user data is a single space char, do not store
            // the message. Otherwise, store and display as usual
            if (" ".equals(getMessageBody())) {
                ;
            }
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int getStatus() {
        return status;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isStatusReportMessage() {
        return isStatusReportMessage;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isReplyPathPresent() {
        return replyPathPresent;
    }

    /**
     * TS 27.005 3.1, &lt;pdu&gt; definition "In the case of SMS: 3GPP TS 24.011 [6]
     * SC address followed by 3GPP TS 23.040 [3] TPDU in hexadecimal format:
     * ME/TA converts each octet of TP data unit into two IRA character long
     * hex number (e.g. octet with integer value 42 is presented to TE as two
     * characters 2A (IRA 50 and 65))" ...in the case of cell broadcast,
     * something else...
     */
    private void parsePdu(byte[] pdu) {
        mPdu = pdu;
        // Log.d(LOG_TAG, "raw sms message:");
        // Log.d(LOG_TAG, s);

        PduParser p = new PduParser(pdu);

        scAddress = p.getSCAddress();

        if (scAddress != null) {
            if (false) Log.d(LOG_TAG, "SMS SC address: " + scAddress);
        }

        // TODO(mkf) support reply path, user data header indicator

        // TP-Message-Type-Indicator
        // 9.2.3
        int firstByte = p.getByte();

        mti = firstByte & 0x3;
        switch (mti) {
        // TP-Message-Type-Indicator
        // 9.2.3
        case 0:
        case 3: //GSM 03.40 9.2.3.1: MTI == 3 is Reserved.
                //This should be processed in the same way as MTI == 0 (Deliver)
            parseSmsDeliver(p, firstByte);
            break;
        case 1:
            parseSmsSubmit(p, firstByte);
            break;
        case 2:
            parseSmsStatusReport(p, firstByte);
            break;
        default:
            // TODO(mkf) the rest of these
            throw new RuntimeException("Unsupported message type");
        }
    }

    /**
     * Parses a SMS-STATUS-REPORT message.
     *
     * @param p A PduParser, cued past the first byte.
     * @param firstByte The first byte of the PDU, which contains MTI, etc.
     */
    private void parseSmsStatusReport(PduParser p, int firstByte) {
        isStatusReportMessage = true;

        // TP-Status-Report-Qualifier bit == 0 for SUBMIT
        forSubmit = (firstByte & 0x20) == 0x00;
        // TP-Message-Reference
        messageRef = p.getByte();
        // TP-Recipient-Address
        recipientAddress = p.getAddress();
        // TP-Service-Centre-Time-Stamp
        scTimeMillis = p.getSCTimestampMillis();
        // TP-Discharge-Time
        dischargeTimeMillis = p.getSCTimestampMillis();
        // TP-Status
        status = p.getByte();
        
        messageBody = "";

        // The following are optional fields that may or may not be present.
        if (p.moreDataPresent()) {
            // TP-Parameter-Indicator
            int extraParams = p.getByte();
            int moreExtraParams = extraParams;
            while (((moreExtraParams & 0x80) != 0) && (p.moreDataPresent() == true)) {
                // We only know how to parse a few extra parameters, all
                // indicated in the first TP-PI octet, so skip over any
                // additional TP-PI octets.
                moreExtraParams = p.getByte();
            }
            // TP-Protocol-Identifier
            if ((extraParams & 0x01) != 0) {
                protocolIdentifier = p.getByte();
            }
            // TP-Data-Coding-Scheme
            if ((extraParams & 0x02) != 0) {
                dataCodingScheme = p.getByte();
            }
            // TP-User-Data-Length (implies existence of TP-User-Data)
            if ((extraParams & 0x04) != 0) {
                boolean hasUserDataHeader = (firstByte & 0x40) == 0x40;
                parseUserData(p, hasUserDataHeader);
            }
        }
    }

    private void parseSmsDeliver(PduParser p, int firstByte) {
        replyPathPresent = (firstByte & 0x80) == 0x80;

        originatingAddress = p.getAddress();

        if (originatingAddress != null) {
            if (false) Log.v(LOG_TAG, "SMS originating address: "
                    + originatingAddress.address);
        }

        // TP-Protocol-Identifier (TP-PID)
        // TS 23.040 9.2.3.9
        protocolIdentifier = p.getByte();

        // TP-Data-Coding-Scheme
        // see TS 23.038
        dataCodingScheme = p.getByte();

        if (false) {
            Log.v(LOG_TAG, "SMS TP-PID:" + protocolIdentifier
                    + " data coding scheme: " + dataCodingScheme);
        }

        scTimeMillis = p.getSCTimestampMillis();

        if (false) Log.d(LOG_TAG, "SMS SC timestamp: " + scTimeMillis);

        boolean hasUserDataHeader = (firstByte & 0x40) == 0x40;

        parseUserData(p, hasUserDataHeader);
    }

    /**
     * Parses the User Data of an SMS.
     *
     * @param p The current PduParser.
     * @param hasUserDataHeader Indicates whether a header is present in the
     *                          User Data.
     */
    private void parseUserData(PduParser p, boolean hasUserDataHeader) {
        boolean hasMessageClass = false;
        boolean userDataCompressed = false;

        int encodingType = ENCODING_UNKNOWN;

        // Look up the data encoding scheme
        if ((dataCodingScheme & 0x80) == 0) {
            // Bits 7..4 == 0xxx
            automaticDeletion = (0 != (dataCodingScheme & 0x40));
            userDataCompressed = (0 != (dataCodingScheme & 0x20));
            hasMessageClass = (0 != (dataCodingScheme & 0x10));

            if (userDataCompressed) {
                Log.w(LOG_TAG, "4 - Unsupported SMS data coding scheme "
                        + "(compression) " + (dataCodingScheme & 0xff));
            } else {
                switch ((dataCodingScheme >> 2) & 0x3) {
                case 0: // GSM 7 bit default alphabet
                    encodingType = ENCODING_7BIT;
                    break;

                case 2: // UCS 2 (16bit)
                    encodingType = ENCODING_16BIT;
                    break;

                case 1: // 8 bit data
                case 3: // reserved
                    Log.w(LOG_TAG, "1 - Unsupported SMS data coding scheme "
                            + (dataCodingScheme & 0xff));
                    encodingType = ENCODING_8BIT;
                    break;
                }
            }
        } else if ((dataCodingScheme & 0xf0) == 0xf0) {
            automaticDeletion = false;
            hasMessageClass = true;
            userDataCompressed = false;

            if (0 == (dataCodingScheme & 0x04)) {
                // GSM 7 bit default alphabet
                encodingType = ENCODING_7BIT;
            } else {
                // 8 bit data
                encodingType = ENCODING_8BIT;
            }
        } else if ((dataCodingScheme & 0xF0) == 0xC0
                || (dataCodingScheme & 0xF0) == 0xD0
                || (dataCodingScheme & 0xF0) == 0xE0) {
            // 3GPP TS 23.038 V7.0.0 (2006-03) section 4

            // 0xC0 == 7 bit, don't store
            // 0xD0 == 7 bit, store
            // 0xE0 == UCS-2, store

            if ((dataCodingScheme & 0xF0) == 0xE0) {
                encodingType = ENCODING_16BIT;
            } else {
                encodingType = ENCODING_7BIT;
            }

            userDataCompressed = false;
            boolean active = ((dataCodingScheme & 0x08) == 0x08);

            // bit 0x04 reserved

            if ((dataCodingScheme & 0x03) == 0x00) {
                isMwi = true;
                mwiSense = active;
                mwiDontStore = ((dataCodingScheme & 0xF0) == 0xC0);
            } else {
                isMwi = false;

                Log.w(LOG_TAG, "MWI for fax, email, or other "
                        + (dataCodingScheme & 0xff));
            }
        } else if ((dataCodingScheme & 0xC0) == 0x80) {
            // 3GPP TS 23.038 V7.0.0 (2006-03) section 4
            // 0x80..0xBF == Reserved coding groups
            if (dataCodingScheme == 0x84) {
                // This value used for KSC5601 by carriers in Korea.
                encodingType = ENCODING_KSC5601;
            } else {
                Log.w(LOG_TAG, "5 - Unsupported SMS data coding scheme "
                        + (dataCodingScheme & 0xff));
            }
        } else {
            Log.w(LOG_TAG, "3 - Unsupported SMS data coding scheme "
                    + (dataCodingScheme & 0xff));
        }

        // set both the user data and the user data header.
        int count = p.constructUserData(hasUserDataHeader,
                encodingType == ENCODING_7BIT);
        this.userData = p.getUserData();
        this.userDataHeader = p.getUserDataHeader();

        switch (encodingType) {
        case ENCODING_UNKNOWN:
        case ENCODING_8BIT:
            messageBody = null;
            break;

        case ENCODING_7BIT:
            messageBody = p.getUserDataGSM7Bit(count,
                    hasUserDataHeader ? userDataHeader.languageTable : 0,
                    hasUserDataHeader ? userDataHeader.languageShiftTable : 0);
            break;

        case ENCODING_16BIT:
            messageBody = p.getUserDataUCS2(count);
            break;

        case ENCODING_KSC5601:
            messageBody = p.getUserDataKSC5601(count);
            break;
        }

        if (false) Log.v(LOG_TAG, "SMS message body (raw): '" + messageBody + "'");

        if (messageBody != null) {
            parseMessageBody();
        }

        if (!hasMessageClass) {
            messageClass = MessageClass.UNKNOWN;
        } else {
            switch (dataCodingScheme & 0x3) {
            case 0:
                messageClass = MessageClass.CLASS_0;
                break;
            case 1:
                messageClass = MessageClass.CLASS_1;
                break;
            case 2:
                messageClass = MessageClass.CLASS_2;
                break;
            case 3:
                messageClass = MessageClass.CLASS_3;
                break;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageClass getMessageClass() {
        return messageClass;
    }

    //MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
    /**
     * Returns true if this is a (U)SIM data download type SM.
     * See 3GPP TS 31.111 section 9.1 and TS 23.040 section 9.2.3.9.
     *
     * @return true if this is a USIM data download message; false otherwise
     */
    boolean isUsimDataDownload() {
        return messageClass == MessageClass.CLASS_2 &&
                (protocolIdentifier == 0x7f || protocolIdentifier == 0x7c);
    }
    //MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3


    // MTK-START [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    /** @hide */
    public static SmsMessage newFromCMTI(String line) {
        // the thinking here is not to read the message immediately
        // FTA test case
        Log.e(LOG_TAG, "newFromCMTI: not yet supported");
        return null;
    }
    
    /**
     * Note: This functionality is currently not supported in GSM mode.
     * @hide
     */
    public static SmsMessageBase newFromParcel(Parcel p){
        Log.w(LOG_TAG, "newFromParcel: is not supported in GSM mode.");
        return null;
    }

    /**
     * Get an SMS-SUBMIT PDU for a data message to a destination address &amp; port
     *
     * @param scAddress Service Centre address. null == use default
     * @param destinationAddress the address of the destination for the message
     * @param destinationPort the port to deliver the message to at the
     *        destination
     * @param originalPort the port to deliver the message from
     * @param data the dat for the message
     * @return a <code>SubmitPdu</code> containing the encoded SC
     *         address, if applicable, and the encoded message.
     *         Returns null on encode error.
     */
    public static SubmitPdu getSubmitPdu(String scAddress,
            String destinationAddress, int destinationPort, int originalPort, byte[] data,
            boolean statusReportRequested) {

        byte[] smsHeaderData = SmsHeader.getSubmitPduHeader(destinationPort, originalPort);
        if (smsHeaderData == null)
        {
            return null;
        }
        return getSubmitPdu(scAddress, destinationAddress,
                data, smsHeaderData, statusReportRequested);
    }
    
    /**
     * Get an SMS-SUBMIT PDU for a destination address and a message
     * which are sent to a specified application port
     *
     * @param scAddress Service Centre address.  Null means use default.
     * @return a <code>SubmitPdu</code> containing the encoded SC
     *         address, if applicable, and the encoded message.
     *         Returns null on encode error.
     */
    public static SubmitPdu getSubmitPdu(String scAddress,
            String destinationAddress, String message,
            int destPort, boolean statusReportRequested) {

        int language = getCurrentSysLanguage();
        int singleId = -1;
        int lockingId = -1;
        int encoding = ENCODING_UNKNOWN;
        TextEncodingDetails ted = new TextEncodingDetails();

        if (encodeStringWithSpecialLang(message, language, ted)) {
            if (ted.useLockingShift && ted.useSingleShift) {
                encoding = ENCODING_7BIT_LOCKING_SINGLE;
                singleId = lockingId = language;
            }
            else if (ted.useLockingShift) {
                encoding = ENCODING_7BIT_LOCKING;
                lockingId = language;
            }
            else if (ted.useSingleShift) {
                encoding = ENCODING_7BIT_SINGLE;
                singleId = language;
            }
            else {
                encoding = ENCODING_7BIT;
                language = -1;
            }
        }
        else {
            encoding = ENCODING_16BIT;
        }

        byte[] smsHeaderData = SmsHeader.getSubmitPduHeaderWithLang(destPort, singleId, lockingId);
         
        return getSubmitPduWithLang(scAddress, destinationAddress, 
                message, statusReportRequested, smsHeaderData, encoding, language);
    }
    
    /**
     * Get an SMS-SUBMIT PDU for a data message with data header
     * to a destination address
     *
     * @param scAddress Service Centre address. null == use default
     * @param destinationAddress the address of the destination for the message
     * @param data the data for the message
     * @param header the pdu header for the message
     * @return a <code>SubmitPdu</code> containing the encoded SC
     *         address, if applicable, and the encoded message.
     *         Returns null on encode error.
     */
    public static SubmitPdu getSubmitPdu(String scAddress,
            String destinationAddress, byte[] data, byte[] smsHeaderData,
            boolean statusReportRequested) {
            
        if ((data.length + smsHeaderData.length + 1) > MAX_USER_DATA_BYTES) {
            Log.e(LOG_TAG, "SMS data message may only contain "
                    + (MAX_USER_DATA_BYTES - smsHeaderData.length - 1) + " bytes");
            return null;
        }

        SubmitPdu ret = new SubmitPdu();
        ByteArrayOutputStream bo = getSubmitPduHead(
                scAddress, destinationAddress, (byte) 0x41, // MTI = SMS-SUBMIT,
                                                            // TP-UDHI = true
                statusReportRequested, ret);

        // TP-Data-Coding-Scheme
        // No class, 8 bit data
        bo.write(0x04);

        // (no TP-Validity-Period)

        // Total size
        bo.write(data.length + smsHeaderData.length + 1);

        // User data header
        bo.write(smsHeaderData.length);
        bo.write(smsHeaderData, 0, smsHeaderData.length);

        // User data
        bo.write(data, 0, data.length);

        ret.encodedMessage = bo.toByteArray();
        return ret;
    }
    
    /**
     * Get an SMS-SUBMIT PDU for a destination address and a message using the
     * specified encoding.
     *
     * @param scAddress Service Centre address.  Null means use default.
     * @param encoding Encoding defined by constants in android.telephony.SmsMessage.ENCODING_*
     * @return a <code>SubmitPdu</code> containing the encoded SC
     *         address, if applicable, and the encoded message.
     *         Returns null on encode error.
     * @hide
     */
    public static SubmitPdu getSubmitPduWithLang(String scAddress,
            String destinationAddress, String message,
            boolean statusReportRequested, byte[] header, int encoding, int language) {
        
        Log.d(LOG_TAG, "SmsMessage: get submit pdu");
        // Perform null parameter checks.
        if (message == null || destinationAddress == null) {
            return null;
        }

        SubmitPdu ret = new SubmitPdu();
        // MTI = SMS-SUBMIT, UDHI = header != null
        Log.d(LOG_TAG, "SmsMessage: UDHI = " + (header != null));
        byte mtiByte = (byte)(0x01 | (header != null ? 0x40 : 0x00));
        ByteArrayOutputStream bo = getSubmitPduHead(
                scAddress, destinationAddress, mtiByte,
                statusReportRequested, ret);
        // User Data (and length)
        byte[] userData;
        if (encoding == ENCODING_UNKNOWN) {
            // First, try encoding it with the GSM alphabet
            encoding = ENCODING_7BIT;
        }
        try {
            Log.d(LOG_TAG, "Get SubmitPdu with Lang " + encoding + " " + language);
            if (encoding == ENCODING_7BIT) {
                //userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header);
                userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, 0, 0);
            } else if (language > 0 && encoding != ENCODING_16BIT) {
                if (encoding == ENCODING_7BIT_LOCKING) {
                    //userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, -1, language);
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, 0, language);
                } else if (encoding == ENCODING_7BIT_SINGLE) {
                    //userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, language, -1);
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, language, 0);
                } else if (encoding == ENCODING_7BIT_LOCKING_SINGLE) {
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, language, language);
                } else {
                    //userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header);
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, 0, 0);
                }
                encoding = ENCODING_7BIT;
            } else { //assume UCS-2
                try {
                    userData = encodeUCS2(message, header);
                } catch(UnsupportedEncodingException uex) {
                    Log.e(LOG_TAG,
                            "Implausible UnsupportedEncodingException ",
                            uex);
                    return null;
                }
            }
        } catch (EncodeException ex) {
            // Encoding to the 7-bit alphabet failed. Let's see if we can
            // send it as a UCS-2 encoded message
            try {
                userData = encodeUCS2(message, header);
                encoding = ENCODING_16BIT;
            } catch(UnsupportedEncodingException uex) {
                Log.e(LOG_TAG,
                        "Implausible UnsupportedEncodingException ",
                        uex);
                return null;
            }
        }

        if (encoding == ENCODING_7BIT) {
            if ((0xff & userData[0]) > MAX_USER_DATA_SEPTETS) {
                // Message too long
                return null;
            }
            // TP-Data-Coding-Scheme
            // Default encoding, uncompressed
            // To test writing messages to the SIM card, change this value 0x00
            // to 0x12, which means "bits 1 and 0 contain message class, and the
            // class is 2". Note that this takes effect for the sender. In other
            // words, messages sent by the phone with this change will end up on
            // the receiver's SIM card. You can then send messages to yourself
            // (on a phone with this change) and they'll end up on the SIM card.
            bo.write(0x00);
        } else { //assume UCS-2
            if ((0xff & userData[0]) > MAX_USER_DATA_BYTES) {
                // Message too long
                return null;
            }
            // TP-Data-Coding-Scheme
            // Class 3, UCS-2 encoding, uncompressed

            //modified by mtk80611
            //bo.write(0x0b);
            bo.write(0x08);
            //modified by mtk80611
        }

        // (no TP-Validity-Period)
        bo.write(userData, 0, userData.length);
        ret.encodedMessage = bo.toByteArray();
        return ret;
    }
    
    public static DeliverPdu getDeliverPduWithLang(String scAddress, String originalAddress, String message,
        byte[] header, long timestamp, int encoding, int language) {
        Log.d(LOG_TAG, "SmsMessage: get deliver pdu");

        if (message == null || originalAddress == null) {
            return null;
        }

        DeliverPdu ret = new DeliverPdu();

        Log.d(LOG_TAG, "SmsMessage: UDHI = " + (header != null));
            byte mtiByte = (byte)(0x00 | (header != null ? 0x40 : 0x00));

        ByteArrayOutputStream bo = getDeliverPduHead(scAddress, originalAddress, mtiByte, ret);

        // encode User Data (and length)
        byte[] userData;
        if (encoding == ENCODING_UNKNOWN) {
            // First, try encoding it with the GSM alphabet
            encoding = ENCODING_7BIT;
        }
        try {
            Log.d(LOG_TAG, "Get SubmitPdu with Lang " + encoding + " " + language);
            if (encoding == ENCODING_7BIT) {
                //userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header);
                userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, 0, 0);
            } else if (language > 0 && encoding != ENCODING_16BIT) {
                if (encoding == ENCODING_7BIT_LOCKING) {
                    //userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, -1, language);
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, 0, language);
                } else if (encoding == ENCODING_7BIT_SINGLE) {
                    //userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, language, -1);
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, language, 0);
                } else if (encoding == ENCODING_7BIT_LOCKING_SINGLE) {
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, language, language);
                } else {
                    //userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header);
                    userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header, 0, 0);
                }
                encoding = ENCODING_7BIT;
            } else { //assume UCS-2
                try {
                    userData = encodeUCS2(message, header);
                } catch(UnsupportedEncodingException uex) {
                    Log.e(LOG_TAG,
                            "Implausible UnsupportedEncodingException ",
                            uex);
                    return null;
                }
            }
        } catch (EncodeException ex) {
            // Encoding to the 7-bit alphabet failed. Let's see if we can
            // send it as a UCS-2 encoded message
            try {
                userData = encodeUCS2(message, header);
                encoding = ENCODING_16BIT;
            } catch(UnsupportedEncodingException uex) {
                Log.e(LOG_TAG,
                        "Implausible UnsupportedEncodingException ",
                        uex);
                return null;
            }
        }

        if (userData != null && (0xff & userData[0]) > MAX_USER_DATA_SEPTETS) {
            Log.d(LOG_TAG, "SmsMessage: message is too long");
            return null;
        }

        // write dcs type
        if (encoding == ENCODING_7BIT) {
            bo.write(0x00);
        } else { //assume UCS-2
            bo.write(0x08);
        }

        // write timestamp
        // Because we can't get invalid timestamp which indicate the message arrival service center,
        // we just write 7 0x00 into the pdu
        
        byte[] scts = parseSCTimestamp(timestamp);
        if(scts != null) {
            bo.write(scts, 0, scts.length);
        } else {
            for(int i = 0; i < 7; ++i) {
              bo.write(0x00);
            }
        }

        bo.write(userData, 0, userData.length);
        ret.encodedMessage = bo.toByteArray();

        return ret;
    }
    
    static private byte[] parseSCTimestamp(long millis) {
        Time t = new Time(Time.TIMEZONE_UTC);
        t.set(millis);
        
        byte[] scts = new byte[7];
        scts[0] = intToGsmBCDByte(t.year);
        scts[1] = intToGsmBCDByte(t.month + 1);
        scts[2] = intToGsmBCDByte(t.monthDay);
        scts[3] = intToGsmBCDByte(t.hour);
        scts[4] = intToGsmBCDByte(t.minute);
        scts[5] = intToGsmBCDByte(t.second);
        scts[6] = intToGsmBCDByte(0);
        
        return scts;
    }
    
    static private byte intToGsmBCDByte(int value) {
        if(value < 0) {
            Log.d(LOG_TAG, "[time invalid value: " + value);
            return (byte)0;
        }
        value %= 100;
        Log.d(LOG_TAG, "[time value: " + value);
        
        //byte b = (byte)(((value / 10) << 4) + (value % 10));
        byte b = (byte)(((value / 10) & 0x0f) | (((value % 10) << 4) & 0xf0));
        Log.d(LOG_TAG, "[time bcd value: " + b);
        return b;
    }
    
    private static ByteArrayOutputStream getDeliverPduHead(
        String scAddress, String originalAddress, byte mtiByte, DeliverPdu ret) {

        ByteArrayOutputStream bo = new ByteArrayOutputStream(
                MAX_USER_DATA_BYTES + 40);

        if (scAddress == null) {
            ret.encodedScAddress = null;
        } else {
            ret.encodedScAddress = PhoneNumberUtils.networkPortionToCalledPartyBCDWithLength(
                    scAddress);
        }

        // write mti byte
        bo.write(mtiByte);

        // write original bytes
        byte[] oaBytes;
        oaBytes = PhoneNumberUtils.networkPortionToCalledPartyBCD(originalAddress);
        if(oaBytes != null) {
            bo.write((oaBytes.length - 1) * 2
                - ((oaBytes[oaBytes.length - 1] & 0xf0) == 0xf0 ? 1 : 0));
            bo.write(oaBytes, 0, oaBytes.length);
        } else {
            Log.d(LOG_TAG, "write a empty address for deliver pdu");
            bo.write(0);
            bo.write(PhoneNumberUtils.TOA_International);
        }

        // write PID
        bo.write(0);

        return bo;
    }
    
    private static boolean encodeStringWithSpecialLang(
            CharSequence msgBody,
            int language, 
            TextEncodingDetails ted) {

        int septets;

        //1st, try default GSM
        //septets = GsmAlphabet.countGsmSeptetsWithTable(
        //        msgBody, -1, -1);
        septets = GsmAlphabet.countGsmSeptetsUsingTables(
                msgBody, true, 0, 0);
        if (septets != -1) {
            
            ted.codeUnitCount = septets;
            if (septets > MAX_USER_DATA_SEPTETS) {
                ted.msgCount = (septets / MAX_USER_DATA_SEPTETS_WITH_HEADER) + 1;
                ted.codeUnitsRemaining = MAX_USER_DATA_SEPTETS_WITH_HEADER
                    - (septets % MAX_USER_DATA_SEPTETS_WITH_HEADER);
            } else {
                ted.msgCount = 1;
                ted.codeUnitsRemaining = MAX_USER_DATA_SEPTETS - septets;
            }
            ted.codeUnitSize = ENCODING_7BIT;
            ted.shiftLangId = -1;
            Log.d(LOG_TAG, "Try Default: "+ language + " " + ted);
            return true;
        }
            
        //2nd, try locking shift
        //septets = GsmAlphabet.countGsmSeptetsWithTable(
        //        msgBody, -1, language);
        septets = GsmAlphabet.countGsmSeptetsUsingTables(
                msgBody, true, 0, language);
        if (septets != -1) {

            int headerElt[] = {SmsHeader.ELT_ID_NATIONAL_LANGUAGE_LOCKING_SHIFT, 0xffff};
            int maxLength = computeRemainUserDataLength(true, headerElt);
            
            ted.codeUnitCount = septets;
            if (septets > maxLength) {
                headerElt[1] = SmsHeader.ELT_ID_CONCATENATED_8_BIT_REFERENCE;
                maxLength = computeRemainUserDataLength(true, headerElt);
                
                ted.msgCount = (septets / maxLength) + 1;
                ted.codeUnitsRemaining = maxLength - (septets % maxLength);
            } else {
                ted.msgCount = 1;
                ted.codeUnitsRemaining = maxLength - septets;
            }
            ted.codeUnitSize = ENCODING_7BIT;
            ted.useLockingShift = true;
            ted.shiftLangId = language;
            Log.d(LOG_TAG, "Try Locking Shift: "+ language + " " + ted);
            return true;
        }
            
        //3rd, try single shift
        //septets = GsmAlphabet.countGsmSeptetsWithTable(
        //        msgBody, language, -1);
        septets = GsmAlphabet.countGsmSeptetsUsingTables(
                msgBody, true, language, 0);
        if (septets != -1) {
            
            int headerElt[] = {SmsHeader.ELT_ID_NATIONAL_LANGUAGE_SINGLE_SHIFT, 0xffff};
            int maxLength = computeRemainUserDataLength(true, headerElt);
            
            ted.codeUnitCount = septets;
            if (septets > maxLength) {
                headerElt[1] = SmsHeader.ELT_ID_CONCATENATED_8_BIT_REFERENCE;
                maxLength = computeRemainUserDataLength(true, headerElt);
                
                ted.msgCount = (septets / maxLength) + 1;
                ted.codeUnitsRemaining = maxLength - (septets % maxLength);
            } else {
                ted.msgCount = 1;
                ted.codeUnitsRemaining = maxLength - septets;
            }
            ted.codeUnitSize = ENCODING_7BIT;
            ted.useSingleShift = true;
            ted.shiftLangId = language;
            Log.d(LOG_TAG, "Try Single Shift: "+ language + " " + ted);
            return true;
        }
            
        //4th, try locking and single shift
        //septets = GsmAlphabet.countGsmSeptetsWithTable(
        //        msgBody, language, language);
        septets = GsmAlphabet.countGsmSeptetsUsingTables(
                msgBody, true, language, language);
        if (septets != -1) {
            int headerElt[] = {
                    SmsHeader.ELT_ID_NATIONAL_LANGUAGE_LOCKING_SHIFT, 
                    SmsHeader.ELT_ID_NATIONAL_LANGUAGE_SINGLE_SHIFT, 
                    0xffff};
            int maxLength = computeRemainUserDataLength(true, headerElt);
            
            ted.codeUnitCount = septets;
            if (septets > maxLength) {
                headerElt[2] = SmsHeader.ELT_ID_CONCATENATED_8_BIT_REFERENCE;
                maxLength = computeRemainUserDataLength(true, headerElt);
                
                ted.msgCount = (septets / maxLength) + 1;
                ted.codeUnitsRemaining = maxLength - (septets % maxLength);
            } else {
                ted.msgCount = 1;
                ted.codeUnitsRemaining = maxLength - septets;
            }
            ted.codeUnitSize = ENCODING_7BIT;
            ted.useLockingShift = true;
            ted.useSingleShift = true;
            ted.shiftLangId = language;
            Log.d(LOG_TAG, "Try Locking & Single Shift: "+ language + " " + ted);
            return true;
        }

        Log.d(LOG_TAG, "Use UCS2"+ language + " " + ted);
        return false;
    }
    
    private static int getCurrentSysLanguage() {
        int ret;
        String language;
        
        language = SystemProperties.get("persist.sys.language", null);
        if (language == null) {
            language = SystemProperties.get("ro.product.locale.language", null);
        }

        if (language.equals("tr")) {
            // ret = GsmAlphabet.SHIFT_ID_TURKISH;
            ret = -1;
        }
        else {
            ret = -1;
        }

        return ret;
    }

    public static int computeRemainUserDataLength(boolean inSeptets, int headerElt[]) {
        int headerBytes = 0;
        int count;
        for (int i=0; i<headerElt.length; i++) {
            switch (headerElt[i]) {
                case SmsHeader.ELT_ID_CONCATENATED_8_BIT_REFERENCE:
                    headerBytes += SmsHeader.CONCATENATED_8_BIT_REFERENCE_LENGTH;
                    break;
                case SmsHeader.ELT_ID_NATIONAL_LANGUAGE_SINGLE_SHIFT:
                    headerBytes += SmsHeader.NATIONAL_LANGUAGE_SINGLE_SHIFT_LENGTH;
                    break;
                case SmsHeader.ELT_ID_NATIONAL_LANGUAGE_LOCKING_SHIFT:
                    headerBytes += SmsHeader.NATIONAL_LANGUAGE_LOCKING_SHIFT_LENGTH;
                    break;
                default:
                    break;
            }
        }

        if (headerBytes != 0) {
            headerBytes++;   // header length
        }

        count = MAX_USER_DATA_BYTES - headerBytes;
        if (inSeptets) {
            count = count * 8 / 7;
        }

        //Log.d(LOG_TAG, "computeRemainUserDataLength: inSeptets: "+ inSeptets +
        //        " , max: "+ count + " header:" + headerBytes);
        
        return count;
    }
    
    private void parseSmsSubmit(PduParser p, int firstByte) {
        replyPathPresent = (firstByte & 0x80) == 0x80;

        messageRef = p.getByte();

        destinationAddress = p.getAddress();
        if (destinationAddress != null) {
            if (Config.LOGV) Log.v(LOG_TAG, "SMS destination address: "
                    + destinationAddress.address);
        }

        // TP-Protocol-Identifier (TP-PID)
        // TS 23.040 9.2.3.9
        protocolIdentifier = p.getByte();

        // TP-Data-Coding-Scheme
        // see TS 23.038
        dataCodingScheme = p.getByte();

        if (Config.LOGV) {
            Log.v(LOG_TAG, "SMS TP-PID:" + protocolIdentifier
                    + " data coding scheme: " + dataCodingScheme);
        }

        int validPeriodFormat = firstByte & MASK_VALIDITY_PERIOD_FORMAT;
        switch(validPeriodFormat) {
            case MASK_VALIDITY_PERIOD_FORMAT_NONE:
                Log.d(LOG_TAG, "TP-VP field is not present");
                break;
            case MASK_VALIDITY_PERIOD_FORMAT_RELATIVE:
                Log.d(LOG_TAG, "TP-VP field is relative format");
                relativeValidityPeriod = p.getByte();
                break;
            case MASK_VALIDITY_PERIOD_FORMAT_ENHANCED:
                Log.d(LOG_TAG, "TP-VP field is enhanced format");
                for(int i = 0; i < 7; ++i) {
                    p.getByte();
                }
                break;
            case MASK_VALIDITY_PERIOD_FORMAT_ABSOLUTE:
                Log.d(LOG_TAG, "TP-VP field is absolute format");
                for(int i = 0; i < 7; ++i) {
                    p.getByte();
                }
                break;
            default:
                Log.d(LOG_TAG, "TP-VP invalid validity format");
                break;
        }

        boolean hasUserDataHeader = ((firstByte & MASK_USER_DATA_HEADER_INDICATOR) == MASK_USER_DATA_HEADER_INDICATOR);
        parseUserData(p, hasUserDataHeader);
    }
    // MTK-END   [ALPS000xxxxx] MTK code port to ICS added by mtk80589 in 2011.11.16
    
    // MTK-START [ALPS00094531] Orange feature SMS Encoding Type Setting by mtk80589 in 2011.11.22
    /**
     * Calculate the number of septets needed to encode the message.
     *
     * @param msgBody the message to encode
     * @param use7bitOnly ignore (but still count) illegal characters if true
     * @param encodingType text encoding type(7-bit, 16-bit or automatic)
     * @return TextEncodingDetails
     */
    public static TextEncodingDetails calculateLength(CharSequence msgBody,
            boolean use7bitOnly, int encodingType) {
        TextEncodingDetails ted = GsmAlphabet.countGsmSeptets(msgBody, use7bitOnly);
        
        if(encodingType == ENCODING_16BIT) {
            Log.d(LOG_TAG, "input mode is unicode");
            ted = null;
        }
        if (ted == null) {
            Log.d(LOG_TAG, "7-bit encoding fail");
            ted = new TextEncodingDetails();
            int octets = msgBody.length() * 2;
            ted.codeUnitCount = msgBody.length();
            if (octets > MAX_USER_DATA_BYTES) {
                ted.msgCount = (octets + (MAX_USER_DATA_BYTES_WITH_HEADER - 1)) /
                        MAX_USER_DATA_BYTES_WITH_HEADER;
                ted.codeUnitsRemaining = ((ted.msgCount *
                        MAX_USER_DATA_BYTES_WITH_HEADER) - octets) / 2;
            } else {
                ted.msgCount = 1;
                ted.codeUnitsRemaining = (MAX_USER_DATA_BYTES - octets)/2;
            }
            ted.codeUnitSize = ENCODING_16BIT;
        }
        return ted;
    }
    // MTK-END [ALPS00094531] Orange feature SMS Encoding Type Setting by mtk80589 in 2011.11.22
    
    /**
     * Get an SMS-SUBMIT PDU for a destination address and a message using the
     * specified encoding.
     *
     * @param scAddress Service Centre address.  Null means use default.
     * @param encoding Encoding defined by constants in android.telephony.SmsMessage.ENCODING_*
     * @param languageTable
     * @param languageShiftTable
     * @return a <code>SubmitPdu</code> containing the encoded SC
     *         address, if applicable, and the encoded message.
     *         Returns null on encode error.
     * @hide
     */
    public static SubmitPdu getSubmitPdu(String scAddress,
            String destinationAddress, String message,
            boolean statusReportRequested, byte[] header, int encoding,
            int languageTable, int languageShiftTable, int validityPeriod) {

        // Perform null parameter checks.
        if (message == null || destinationAddress == null) {
            return null;
        }

        if (encoding == ENCODING_UNKNOWN) {
            // Find the best encoding to use
            TextEncodingDetails ted = calculateLength(message, false);
            encoding = ted.codeUnitSize;
            languageTable = ted.languageTable;
            languageShiftTable = ted.languageShiftTable;

            if (encoding == ENCODING_7BIT && (languageTable != 0 || languageShiftTable != 0)) {
                if (header != null) {
                    SmsHeader smsHeader = SmsHeader.fromByteArray(header);
                    if (smsHeader.languageTable != languageTable
                            || smsHeader.languageShiftTable != languageShiftTable) {
                        Log.w(LOG_TAG, "Updating language table in SMS header: "
                                + smsHeader.languageTable + " -> " + languageTable + ", "
                                + smsHeader.languageShiftTable + " -> " + languageShiftTable);
                        smsHeader.languageTable = languageTable;
                        smsHeader.languageShiftTable = languageShiftTable;
                        header = SmsHeader.toByteArray(smsHeader);
                    }
                } else {
                    SmsHeader smsHeader = new SmsHeader();
                    smsHeader.languageTable = languageTable;
                    smsHeader.languageShiftTable = languageShiftTable;
                    header = SmsHeader.toByteArray(smsHeader);
                }
            }
        }

        SubmitPdu ret = new SubmitPdu();
        // MTI = SMS-SUBMIT, UDHI = header != null
        byte mtiByte = (byte)(0x01 | (header != null ? 0x40 : 0x00));
        ByteArrayOutputStream bo = getSubmitPduHead(
                scAddress, destinationAddress, mtiByte,
                statusReportRequested, ret);

        // User Data (and length)
        byte[] userData;
        try {
            if (encoding == ENCODING_7BIT) {
                userData = GsmAlphabet.stringToGsm7BitPackedWithHeader(message, header,
                        languageTable, languageShiftTable);
            } else { //assume UCS-2
                try {
                    userData = encodeUCS2(message, header);
                } catch(UnsupportedEncodingException uex) {
                    Log.e(LOG_TAG,
                            "Implausible UnsupportedEncodingException ",
                            uex);
                    return null;
                }
            }
        } catch (EncodeException ex) {
            // Encoding to the 7-bit alphabet failed. Let's see if we can
            // send it as a UCS-2 encoded message
            try {
                userData = encodeUCS2(message, header);
                encoding = ENCODING_16BIT;
            } catch(UnsupportedEncodingException uex) {
                Log.e(LOG_TAG,
                        "Implausible UnsupportedEncodingException ",
                        uex);
                return null;
            }
        }

        if (encoding == ENCODING_7BIT) {
            if ((0xff & userData[0]) > MAX_USER_DATA_SEPTETS) {
                // Message too long
                //MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
                Log.e(LOG_TAG, "Message too long (" + (0xff & userData[0]) + " septets)");
                //MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
                return null;
            }
            // TP-Data-Coding-Scheme
            // Default encoding, uncompressed
            // To test writing messages to the SIM card, change this value 0x00
            // to 0x12, which means "bits 1 and 0 contain message class, and the
            // class is 2". Note that this takes effect for the sender. In other
            // words, messages sent by the phone with this change will end up on
            // the receiver's SIM card. You can then send messages to yourself
            // (on a phone with this change) and they'll end up on the SIM card.
            bo.write(0x00);
        } else { // assume UCS-2
            if ((0xff & userData[0]) > MAX_USER_DATA_BYTES) {
                // Message too long
                //MTK-START [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
                Log.e(LOG_TAG, "Message too long (" + (0xff & userData[0]) + " bytes)");
                //MTK-END [mtk04070][111223][ALPS00106134]Merge to ICS 4.0.3
                return null;
            }
            // TP-Data-Coding-Scheme
            // UCS-2 encoding, uncompressed
            bo.write(0x08);
        }
        
        if(validityPeriod >= 0) {
        	Log.d(LOG_TAG, "write validity period into pdu: " + validityPeriod);
        	bo.write(validityPeriod);
        }

        // (no TP-Validity-Period)
        bo.write(userData, 0, userData.length);
        ret.encodedMessage = bo.toByteArray();
        return ret;
    }
}
