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

package com.mediatek.MediatekDM.mdm;

public class MdmException extends Exception {
    private static final long serialVersionUID = 5511575635796082182L;
    private MdmError mErrorCode;

    public static enum MdmError {
        // TODO check these values
        ABORT(0),
        ALERT_MISSING_DATA(1),
        ALERT_MISSING_ITEMS(2),
        ALERT_PARSING_ERROR(3),
        ALERT_SESSION_ABORTED(4),
        ALERT_TOO_MANY_CHOICES(5),
        ALERT_USER_ABORTED(6),
        ALREADY_EXISTS(7),
        AUTHENTICATION(8),
        BAD_DD(9),
        BAD_INPUT(10),
        BAD_URL(11),
        BOOT_BAD_MAC(12),
        BOOT_BAD_MESSAGE(13),
        BOOT_BAD_PROF(14),
        BOOT_BAD_SEC(15),
        BOOT_DIGEST(16),
        BOOT_DISABLED(17),
        BOOT_NSS(18),
        BOOT_PIN(19),
        BOOT_PINLENGTH(20),
        BUFFER_OVERFLOW(21),
        CANCEL(22),
        COMMAND_FAILED(23),
        COMMS_BAD_PROTOCOL(24),
        COMMS_FATAL(25),
        COMMS_HTTP_ERROR(26),
        COMMS_MIME_MISMATCH(27),
        COMMS_NON_FATAL(28),
        COMMS_OBJECT_CHANGED(29),
        COMMS_SOCKET_ERROR(30),
        COMMS_SOCKET_TIMEOUT(31),
        DL_OBJ_TOO_LARGE(32),
        INTERNAL(33),
        INVALID_CALL(34),
        INVALID_INPUT_PARAM(35),
        INVALID_PROTO_OR_VERSION(36),
        IS_SUSPENDED(37),
        LEAF_NODE(38),
        LO_HANDLED(39),
        MAY_TREE_NOT_REPLACE(40),
        MEMORY(41),
        MISSING_START_MESSAGE_CMD(42),
        MISSING_STATUS_CMD(43),
        MO_STORAGE(44),
        NO_DATA(45),
        NO_MORE_COMMANDS(46),
        NODE_ACCESS_DENIED(47),
        NODE_MISSING(48),
        NODE_NOT_EXECUTABLE(49),
        NODE_VALUE_NODE_NOT_WRITEABLE(50),
        NODE_VALUE_NOT_READABLE(51),
        NOT_ALLOWED(52),
        NOT_IMPLEMENTED(53),
        NOT_LEAF_NODE(54),
        NOTIF_BAD_DIGEST(55),
        NOTIF_BAD_LENGTH(56),
        NOTIF_UNSUPPORTED_VERSION(57),
        OK(58),
        OUT_OF_BOUNDS(59),
        OUT_OF_SYNC(60),
        PARENT_MISSING(61),
        PERMANENT_NODE(62),
        REGISTRY(63),
        RTK_BUFFER_OVERFLOW(64),
        SHUTTING_DOWN(65),
        STORAGE_STORAGE_COMMIT(66),
        STORAGE_STORAGE_OPEN(67),
        STORAGE_STORAGE_READ(68),
        STORAGE_STORAGE_REMOVE(69),
        STORAGE_STORAGE_WRITE(70),
        TOO_BIG(71),
        TREE_ACCESS_DENIED(72),
        TREE_EXT_NOT_ALLOWED(73),
        TREE_EXT_NOT_PARTIAL(74),
        TRG_BAD_REASON(75),
        UNKNOWN_PROPERTY(76),
        UNSPECIFIC(77),
        UPDATE_INIT(78);

        public String string;
        public int val;

        private MdmError(int value) {
            val = value;
            string = this.toString();
        }

        public static MdmError fromInt(int error) {
            for (MdmError e: MdmError.values()) {
                if (e.val == error)
                    return e;
            }
            return null;
        }
    }

    public MdmException(MdmError error, String message) {
        super(message);
        mErrorCode = error;

    }

    public MdmException(String message) {
        super(message);
        mErrorCode = MdmError.UNSPECIFIC;
    }

    public MdmException(MdmError error) {
        this(error, "Error");
    }

    public MdmException(int error) {
        this(MdmError.fromInt(error), "Error");
    }

    public MdmError getError() {
        return mErrorCode;
    }

    public String getMessage() {
        return ("MdmException(" + mErrorCode + "): " + super.getMessage());
    }
}
