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

package mediatek.net.cts;

import java.util.List;
import java.util.Set;
import android.net.UrlQuerySanitizer;
import android.net.UrlQuerySanitizer.IllegalCharacterValueSanitizer;
import android.net.UrlQuerySanitizer.ParameterValuePair;
import android.net.UrlQuerySanitizer.ValueSanitizer;
import android.test.AndroidTestCase;
import dalvik.annotation.TestLevel;
import dalvik.annotation.TestTargetClass;
import dalvik.annotation.TestTargetNew;
import dalvik.annotation.TestTargets;

@TestTargetClass(UrlQuerySanitizer.class)
public class UrlQuerySanitizerTest extends AndroidTestCase {
    private static final int ALL_OK = IllegalCharacterValueSanitizer.ALL_OK;

    // URL for test.
    private static final String TEST_URL = "http://example.com/?name=Joe+User&age=20&height=175";

    // Default sanitizer's change when "+".
    private static final String EXPECTED_UNDERLINE_NAME = "Joe_User";

    // IllegalCharacterValueSanitizer sanitizer's change when "+".
    private static final String EXPECTED_SPACE_NAME = "Joe User";
    private static final String EXPECTED_AGE = "20";
    private static final String EXPECTED_HEIGHT = "175";
    private static final String NAME = "name";
    private static final String AGE = "age";
    private static final String HEIGHT = "height";

    @TestTargets({
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "UrlQuerySanitizer",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "UrlQuerySanitizer",
            args = {String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "parseUrl",
            args = {String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "parseQuery",
            args = {String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "parseEntry",
            args = {String.class, String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getValue",
            args = {String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "addSanitizedEntry",
            args = {String.class, String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "hasParameter",
            args = {String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getParameterSet",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getParameterList",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setUnregisteredParameterValueSanitizer",
            args = {ValueSanitizer.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getUnregisteredParameterValueSanitizer",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getAllButNulAndAngleBracketsLegal",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getAllButNulLegal",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getAllButWhitespaceLegal",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getAllIllegal",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getAmpAndSpaceLegal",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getAmpLegal",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getSpaceLegal",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getUrlAndSpaceLegal",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getUrlLegal",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "unescape",
            args = {String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "isHexDigit",
            args = {char.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "decodeHexDigit",
            args = {char.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setAllowUnregisteredParamaters",
            args = {boolean.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getAllowUnregisteredParamaters",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "registerParameter",
            args = {String.class, ValueSanitizer.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "registerParameters",
            args = {String[].class, ValueSanitizer.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getEffectiveValueSanitizer",
            args = {String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getValueSanitizer",
            args = {String.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "clear",
            args = {}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "setPreferFirstRepeatedParameter",
            args = {boolean.class}
        ),
        @TestTargetNew(
            level = TestLevel.COMPLETE,
            method = "getPreferFirstRepeatedParameter",
            args = {}
        )
    })
    public void testUrlQuerySanitizer() {
        MockUrlQuerySanitizer uqs = new MockUrlQuerySanitizer();
        assertFalse(uqs.getAllowUnregisteredParamaters());

        final String query = "book=thinking in java&price=108";
        final String book = "book";
        final String bookName = "thinking in java";
        final String price = "price";
        final String bookPrice = "108";
        final String notExistPar = "notExistParameter";
        uqs.registerParameters(new String[]{book, price}, UrlQuerySanitizer.getSpaceLegal());
        uqs.parseQuery(query);
        assertTrue(uqs.hasParameter(book));
        assertTrue(uqs.hasParameter(price));
        assertFalse(uqs.hasParameter(notExistPar));
        assertEquals(bookName, uqs.getValue(book));
        assertEquals(bookPrice, uqs.getValue(price));
        assertNull(uqs.getValue(notExistPar));
        uqs.clear();
        assertFalse(uqs.hasParameter(book));
        assertFalse(uqs.hasParameter(price));

        uqs.parseEntry(book, bookName);
        assertTrue(uqs.hasParameter(book));
        assertEquals(bookName, uqs.getValue(book));
        uqs.parseEntry(price, bookPrice);
        assertTrue(uqs.hasParameter(price));
        assertEquals(bookPrice, uqs.getValue(price));
        assertFalse(uqs.hasParameter(notExistPar));
        assertNull(uqs.getValue(notExistPar));

        uqs = new MockUrlQuerySanitizer(TEST_URL);
        assertTrue(uqs.getAllowUnregisteredParamaters());

        assertTrue(uqs.hasParameter(NAME));
        assertTrue(uqs.hasParameter(AGE));
        assertTrue(uqs.hasParameter(HEIGHT));
        assertFalse(uqs.hasParameter(notExistPar));

        assertEquals(EXPECTED_UNDERLINE_NAME, uqs.getValue(NAME));
        assertEquals(EXPECTED_AGE, uqs.getValue(AGE));
        assertEquals(EXPECTED_HEIGHT, uqs.getValue(HEIGHT));
        assertNull(uqs.getValue(notExistPar));

        final int ContainerLen = 3;
        Set<String> urlSet = uqs.getParameterSet();
        assertEquals(ContainerLen, urlSet.size());
        assertTrue(urlSet.contains(NAME));
        assertTrue(urlSet.contains(AGE));
        assertTrue(urlSet.contains(HEIGHT));
        assertFalse(urlSet.contains(notExistPar));

        List<ParameterValuePair> urlList = uqs.getParameterList();
        assertEquals(ContainerLen, urlList.size());
        ParameterValuePair pvp = urlList.get(0);
        assertEquals(NAME, pvp.mParameter);
        assertEquals(EXPECTED_UNDERLINE_NAME, pvp.mValue);
        pvp = urlList.get(1);
        assertEquals(AGE, pvp.mParameter);
        assertEquals(EXPECTED_AGE, pvp.mValue);
        pvp = urlList.get(2);
        assertEquals(HEIGHT, pvp.mParameter);
        assertEquals(EXPECTED_HEIGHT, pvp.mValue);

        assertFalse(uqs.getPreferFirstRepeatedParameter());
        uqs.addSanitizedEntry(HEIGHT, EXPECTED_HEIGHT + 1);
        assertEquals(ContainerLen, urlSet.size());
        assertEquals(ContainerLen + 1, urlList.size());
        assertEquals(EXPECTED_HEIGHT + 1, uqs.getValue(HEIGHT));

        uqs.setPreferFirstRepeatedParameter(true);
        assertTrue(uqs.getPreferFirstRepeatedParameter());
        uqs.addSanitizedEntry(HEIGHT, EXPECTED_HEIGHT);
        assertEquals(ContainerLen, urlSet.size());
        assertEquals(ContainerLen + 2, urlList.size());
        assertEquals(EXPECTED_HEIGHT + 1, uqs.getValue(HEIGHT));

        uqs.registerParameter(NAME, null);
        assertNull(uqs.getValueSanitizer(NAME));
        assertNotNull(uqs.getEffectiveValueSanitizer(NAME));

        uqs.setAllowUnregisteredParamaters(false);
        assertFalse(uqs.getAllowUnregisteredParamaters());
        uqs.registerParameter(NAME, null);
        assertNull(uqs.getEffectiveValueSanitizer(NAME));

        ValueSanitizer vs = new IllegalCharacterValueSanitizer(ALL_OK);
        uqs.registerParameter(NAME, vs);
        uqs.parseUrl(TEST_URL);
        assertEquals(EXPECTED_SPACE_NAME, uqs.getValue(NAME));
        assertNotSame(EXPECTED_AGE, uqs.getValue(AGE));

        String[] register = {NAME, AGE};
        uqs.registerParameters(register, vs);
        uqs.parseUrl(TEST_URL);
        assertEquals(EXPECTED_SPACE_NAME, uqs.getValue(NAME));
        assertEquals(EXPECTED_AGE, uqs.getValue(AGE));
        assertNotSame(EXPECTED_HEIGHT, uqs.getValue(HEIGHT));

        uqs.setUnregisteredParameterValueSanitizer(vs);
        assertEquals(vs, uqs.getUnregisteredParameterValueSanitizer());

        vs = UrlQuerySanitizer.getAllIllegal();
        assertEquals("Joe_User", vs.sanitize("Joe<User"));
        vs = UrlQuerySanitizer.getAllButNulAndAngleBracketsLegal();
        assertEquals("Joe   User", vs.sanitize("Joe<>\0User"));
        vs = UrlQuerySanitizer.getAllButNulLegal();
        assertEquals("Joe User", vs.sanitize("Joe\0User"));
        vs = UrlQuerySanitizer.getAllButWhitespaceLegal();
        assertEquals("Joe_User", vs.sanitize("Joe User"));
        vs = UrlQuerySanitizer.getAmpAndSpaceLegal();
        assertEquals("Joe User&", vs.sanitize("Joe User&"));
        vs = UrlQuerySanitizer.getAmpLegal();
        assertEquals("Joe_User&", vs.sanitize("Joe User&"));
        vs = UrlQuerySanitizer.getSpaceLegal();
        assertEquals("Joe User ", vs.sanitize("Joe User&"));
        vs = UrlQuerySanitizer.getUrlAndSpaceLegal();
        assertEquals("Joe User&Smith%B5'\'", vs.sanitize("Joe User&Smith%B5'\'"));
        vs = UrlQuerySanitizer.getUrlLegal();
        assertEquals("Joe_User&Smith%B5'\'", vs.sanitize("Joe User&Smith%B5'\'"));

        String escape = "Joe";
        assertEquals(escape, uqs.unescape(escape));
        String expectedPlus = "Joe User";
        String expectedPercentSignHex = "title=" + Character.toString((char)181);
        String initialPlus = "Joe+User";
        String initialPercentSign = "title=%B5";
        assertEquals(expectedPlus, uqs.unescape(initialPlus));
        assertEquals(expectedPercentSignHex, uqs.unescape(initialPercentSign));

        assertTrue(uqs.decodeHexDigit('0') >= 0);
        assertTrue(uqs.decodeHexDigit('b') >= 0);
        assertTrue(uqs.decodeHexDigit('F') >= 0);
        assertTrue(uqs.decodeHexDigit('$') < 0);

        assertTrue(uqs.isHexDigit('0'));
        assertTrue(uqs.isHexDigit('b'));
        assertTrue(uqs.isHexDigit('F'));
        assertFalse(uqs.isHexDigit('$'));

        uqs.clear();
        assertEquals(0, urlSet.size());
        assertEquals(0, urlList.size());

        uqs.setPreferFirstRepeatedParameter(true);
        assertTrue(uqs.getPreferFirstRepeatedParameter());
        uqs.setPreferFirstRepeatedParameter(false);
        assertFalse(uqs.getPreferFirstRepeatedParameter());

        UrlQuerySanitizer uq = new UrlQuerySanitizer();
        uq.setPreferFirstRepeatedParameter(true);
        final String PARA_ANSWER = "answer";
        uq.registerParameter(PARA_ANSWER, new MockValueSanitizer());
        uq.parseUrl("http://www.google.com/question?answer=13&answer=42");
        assertEquals("13", uq.getValue(PARA_ANSWER));

        uq.setPreferFirstRepeatedParameter(false);
        uq.parseQuery("http://www.google.com/question?answer=13&answer=42");
        assertEquals("42", uq.getValue(PARA_ANSWER));

    }

    private static class MockValueSanitizer implements ValueSanitizer{

        public String sanitize(String value) {
            return value;
        }
    }

    class MockUrlQuerySanitizer extends UrlQuerySanitizer {
        public MockUrlQuerySanitizer() {
            super();
        }

        public MockUrlQuerySanitizer(String url) {
            super(url);
        }

        @Override
        protected void addSanitizedEntry(String parameter, String value) {
            super.addSanitizedEntry(parameter, value);
        }

        @Override
        protected void clear() {
            super.clear();
        }

        @Override
        protected int decodeHexDigit(char c) {
            return super.decodeHexDigit(c);
        }

        @Override
        protected boolean isHexDigit(char c) {
            return super.isHexDigit(c);
        }

        @Override
        protected void parseEntry(String parameter, String value) {
            super.parseEntry(parameter, value);
        }
    }
}
