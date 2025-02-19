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

package libcore.java.net;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Collections;
import tests.util.SerializationTester;

public class InetAddressTest extends junit.framework.TestCase {
    private static final byte[] LOOPBACK6_BYTES = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 };

    private static final String[] INVALID_IPv4_NUMERIC_ADDRESSES = new String[] {
        // IPv4 addresses may not be surrounded by square brackets.
        "[127.0.0.1]",

        // Trailing dots are not allowed.
        "1.2.3.4.",
        // Nor is any kind of trailing junk.
        "1.2.3.4hello",

        // Out of range.
        "256.2.3.4",
        "1.256.3.4",
        "1.2.256.4",
        "1.2.3.256",

        // Deprecated.
        "1.2.3",
        "1.2",
        "1",
        "1234",
        "0", // Single out the deprecated form of the ANY address.

        // Hex.
        "0x1.0x2.0x3.0x4",
        "0x7f.0x00.0x00.0x01",
        "7f.0.0.1",

        // Octal.
        "0177.00.00.01", // Historically, this would have been interpreted as 127.0.0.1.

        // Negative numbers.
        "-1.0.0.1",
        "1.-1.0.1",
        "1.0.-1.1",
        "1.0.0.-1",
    };

    private static Inet6Address loopback6() throws Exception {
        return (Inet6Address) InetAddress.getByAddress(LOOPBACK6_BYTES);
    }

    private static Inet6Address localhost6() throws Exception {
        return (Inet6Address) InetAddress.getByAddress("localhost", LOOPBACK6_BYTES);
    }

    public void test_parseNumericAddress() throws Exception {
        // Regular IPv4.
        assertEquals("/1.2.3.4", InetAddress.parseNumericAddress("1.2.3.4").toString());
        // Regular IPv6.
        assertEquals("/2001:4860:800d::68", InetAddress.parseNumericAddress("2001:4860:800d::68").toString());
        // Optional square brackets around IPv6 addresses, including mapped IPv4.
        assertEquals("/2001:4860:800d::68", InetAddress.parseNumericAddress("[2001:4860:800d::68]").toString());
        assertEquals("/127.0.0.1", InetAddress.parseNumericAddress("[::ffff:127.0.0.1]").toString());

        try {
            InetAddress.parseNumericAddress("example.com"); // Not numeric.
            fail();
        } catch (IllegalArgumentException expected) {
        }

        for (String invalid : INVALID_IPv4_NUMERIC_ADDRESSES) {
            try {
                InetAddress.parseNumericAddress(invalid);
                fail(invalid);
            } catch (IllegalArgumentException expected) {
            }
        }

        // Strange special cases, for compatibility with InetAddress.getByName.
        assertTrue(InetAddress.parseNumericAddress(null).isLoopbackAddress());
        assertTrue(InetAddress.parseNumericAddress("").isLoopbackAddress());
    }

    public void test_isNumeric() throws Exception {
        assertTrue(InetAddress.isNumeric("1.2.3.4"));
        assertTrue(InetAddress.isNumeric("127.0.0.1"));

        assertFalse(InetAddress.isNumeric("example.com"));

        for (String invalid : INVALID_IPv4_NUMERIC_ADDRESSES) {
            assertFalse(invalid, InetAddress.isNumeric(invalid));
        }
    }

    public void test_isLinkLocalAddress() throws Exception {
        assertFalse(InetAddress.getByName("127.0.0.1").isLinkLocalAddress());
        assertTrue(InetAddress.getByName("169.254.1.2").isLinkLocalAddress());

        assertFalse(InetAddress.getByName("fec0::").isLinkLocalAddress());
        assertTrue(InetAddress.getByName("fe80::").isLinkLocalAddress());
    }

    public void test_isMCSiteLocalAddress() throws Exception {
        assertFalse(InetAddress.getByName("239.254.255.255").isMCSiteLocal());
        assertTrue(InetAddress.getByName("239.255.0.0").isMCSiteLocal());
        assertTrue(InetAddress.getByName("239.255.255.255").isMCSiteLocal());
        assertFalse(InetAddress.getByName("240.0.0.0").isMCSiteLocal());

        assertFalse(InetAddress.getByName("ff06::").isMCSiteLocal());
        assertTrue(InetAddress.getByName("ff05::").isMCSiteLocal());
        assertTrue(InetAddress.getByName("ff15::").isMCSiteLocal());
    }

    public void test_isReachable() throws Exception {
        // http://code.google.com/p/android/issues/detail?id=20203
        InetAddress addr = SerializationTester.getDeserializedObject(InetAddress.getByName("www.google.com"));
        addr.isReachable(500);
        for (NetworkInterface nif : Collections.list(NetworkInterface.getNetworkInterfaces())) {
            addr.isReachable(nif, 20, 500);
        }
    }

    public void test_isSiteLocalAddress() throws Exception {
        assertFalse(InetAddress.getByName("144.32.32.1").isSiteLocalAddress());
        assertTrue(InetAddress.getByName("10.0.0.1").isSiteLocalAddress());
        assertTrue(InetAddress.getByName("172.16.0.1").isSiteLocalAddress());
        assertFalse(InetAddress.getByName("172.32.0.1").isSiteLocalAddress());
        assertTrue(InetAddress.getByName("192.168.0.1").isSiteLocalAddress());

        assertFalse(InetAddress.getByName("fc00::").isSiteLocalAddress());
        assertTrue(InetAddress.getByName("fec0::").isSiteLocalAddress());
    }

    public void test_getByName() throws Exception {
        /* Mark this due to China network issue */
        /*for (String invalid : INVALID_IPv4_NUMERIC_ADDRESSES) {
            try {
                InetAddress.getByName(invalid);
                fail(invalid);
            } catch (UnknownHostException expected) {
            }
        }*/
    }

    public void test_getLoopbackAddress() throws Exception {
        assertTrue(InetAddress.getLoopbackAddress().isLoopbackAddress());
    }

    public void test_equals() throws Exception {
        InetAddress addr = InetAddress.getByName("239.191.255.255");
        assertTrue(addr.equals(addr));
        assertTrue(loopback6().equals(localhost6()));
        assertFalse(addr.equals(loopback6()));

        assertTrue(Inet4Address.LOOPBACK.equals(Inet4Address.LOOPBACK));

        // http://b/4328294 - the scope id isn't included when comparing Inet6Address instances.
        byte[] bs = new byte[16];
        assertEquals(Inet6Address.getByAddress("1", bs, 1), Inet6Address.getByAddress("2", bs, 2));
    }

    public void test_getHostAddress() throws Exception {
        assertEquals("::1", localhost6().getHostAddress());
        assertEquals("::1", InetAddress.getByName("::1").getHostAddress());

        assertEquals("127.0.0.1", Inet4Address.LOOPBACK.getHostAddress());

        InetAddress aAddr = InetAddress.getByName("224.0.0.0");
        assertEquals("224.0.0.0", aAddr.getHostAddress());


        try {
            InetAddress.getByName("1");
            fail();
        } catch (UnknownHostException expected) {
        }

        byte[] bAddr = {
            (byte) 0xFE, (byte) 0x80, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x02, (byte) 0x11, (byte) 0x25, (byte) 0xFF,
            (byte) 0xFE, (byte) 0xF8, (byte) 0x7C, (byte) 0xB2
        };
        aAddr = Inet6Address.getByAddress(bAddr);
        String aString = aAddr.getHostAddress();
        assertTrue(aString.equals("fe80:0:0:0:211:25ff:fef8:7cb2") || aString.equals("fe80::211:25ff:fef8:7cb2"));

        byte[] cAddr = {
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        };
        aAddr = Inet6Address.getByAddress(cAddr);
        assertEquals("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", aAddr.getHostAddress());

        byte[] dAddr = {
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
        };
        aAddr = Inet6Address.getByAddress(dAddr);
        aString = aAddr.getHostAddress();
        assertTrue(aString.equals("0:0:0:0:0:0:0:0") || aString.equals("::"));

        byte[] eAddr = {
            (byte) 0x00, (byte) 0x01, (byte) 0x02, (byte) 0x03,
            (byte) 0x04, (byte) 0x05, (byte) 0x06, (byte) 0x07,
            (byte) 0x08, (byte) 0x09, (byte) 0x0a, (byte) 0x0b,
            (byte) 0x0c, (byte) 0x0d, (byte) 0x0e, (byte) 0x0f
        };
        aAddr = Inet6Address.getByAddress(eAddr);
        assertEquals("1:203:405:607:809:a0b:c0d:e0f", aAddr.getHostAddress());

        byte[] fAddr = {
            (byte) 0x00, (byte) 0x10, (byte) 0x20, (byte) 0x30,
            (byte) 0x40, (byte) 0x50, (byte) 0x60, (byte) 0x70,
            (byte) 0x80, (byte) 0x90, (byte) 0xa0, (byte) 0xb0,
            (byte) 0xc0, (byte) 0xd0, (byte) 0xe0, (byte) 0xf0
        };
        aAddr = Inet6Address.getByAddress(fAddr);
        assertEquals("10:2030:4050:6070:8090:a0b0:c0d0:e0f0", aAddr.getHostAddress());
    }

    public void test_hashCode() throws Exception {
        InetAddress addr1 = InetAddress.getByName("1.0.0.1");
        InetAddress addr2 = InetAddress.getByName("1.0.0.1");
        assertTrue(addr1.hashCode() == addr2.hashCode());

        assertTrue(loopback6().hashCode() == localhost6().hashCode());
    }

    public void test_toString() throws Exception {
        String validIPAddresses[] = {
            "::1.2.3.4", "::", "::", "1::0", "1::", "::1",
            "FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF",
            "FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:255.255.255.255",
            "0:0:0:0:0:0:0:0", "0:0:0:0:0:0:0.0.0.0"
        };

        String [] resultStrings = {
            "/::1.2.3.4", "/::", "/::", "/1::", "/1::", "/::1",
            "/ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff",
            "/ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", "/::",
            "/::"
        };

        for(int i = 0; i < validIPAddresses.length; i++) {
            InetAddress ia = InetAddress.getByName(validIPAddresses[i]);
            String result = ia.toString();
            assertNotNull(result);
            assertEquals(resultStrings[i], result);
        }
    }
}
