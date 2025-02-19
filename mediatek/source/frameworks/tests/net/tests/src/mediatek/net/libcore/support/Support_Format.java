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
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tests.support;

import java.text.AttributedCharacterIterator;
import java.text.FieldPosition;
import java.text.Format;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.Iterator;
import java.util.Vector;
import junit.framework.TestCase;

public class Support_Format extends TestCase {

    protected String text;

    public Support_Format(String p1) {
        super(p1);
    }

    protected void t_FormatWithField(int count, Format format, Object object,
            String text, Format.Field field, int begin, int end) {
        StringBuffer buffer = new StringBuffer();
        FieldPosition pos = new FieldPosition(field);
        format.format(object, buffer, pos);

        // System.out.println(buffer);
        // System.out.println(pos);

        if (text == null) {
            assertEquals("Test " + count + ": incorrect formatted text",
                    this.text, buffer.toString());
        } else {
            assertEquals("Test " + count + ": incorrect formatted text", text,
                    buffer.toString());
        }

        assertEquals("Test " + count + ": incorrect begin index for field "
                + field, begin, pos.getBeginIndex());
        assertEquals("Test " + count + ": incorrect end index for field"
                + field, end, pos.getEndIndex());
    }

    protected void t_Format(int count, Object object, Format format,
            Vector<FieldContainer> expectedResults) {
        // System.out.println(format.format(object));
        Vector<FieldContainer> results = findFields(format.formatToCharacterIterator(object));
        assertTrue("Test " + count
                + ": Format returned incorrect CharacterIterator for "
                + format.format(object), compare(results, expectedResults));
    }

    /**
     * compares two vectors regardless of the order of their elements
     */
    protected static boolean compare(Vector<FieldContainer> vector1, Vector<FieldContainer> vector2) {
        return vector1.size() == vector2.size() && vector1.containsAll(vector2);
    }

    /**
     * finds attributes with regards to char index in this
     * AttributedCharacterIterator, and puts them in a vector
     *
     * @param iterator
     * @return a vector, each entry in this vector are of type FieldContainer ,
     *         which stores start and end indexes and an attribute this range
     *         has
     */
    protected static Vector<FieldContainer> findFields(AttributedCharacterIterator iterator) {
        Vector<FieldContainer> result = new Vector<FieldContainer>();
        while (iterator.getIndex() != iterator.getEndIndex()) {
            int start = iterator.getRunStart();
            int end = iterator.getRunLimit();

            Iterator<Attribute> it = iterator.getAttributes().keySet().iterator();
            while (it.hasNext()) {
                AttributedCharacterIterator.Attribute attribute = it.next();
                Object value = iterator.getAttribute(attribute);
                result.add(new FieldContainer(start, end, attribute, value));
                // System.out.println(start + " " + end + ": " + attribute + ",
                // " + value );
                // System.out.println("v.add(new FieldContainer(" + start +"," +
                // end +"," + attribute+ "," + value+ "));");
            }
            iterator.setIndex(end);
        }
        return result;
    }

    protected static class FieldContainer {
        int start, end;

        AttributedCharacterIterator.Attribute attribute;

        Object value;

        // called from support_decimalformat and support_simpledateformat tests
        public FieldContainer(int start, int end,
                AttributedCharacterIterator.Attribute attribute) {
            this(start, end, attribute, attribute);
        }

        // called from support_messageformat tests
        public FieldContainer(int start, int end, Attribute attribute, int value) {
            this(start, end, attribute, new Integer(value));
        }

        // called from support_messageformat tests
        public FieldContainer(int start, int end, Attribute attribute,
                Object value) {
            this.start = start;
            this.end = end;
            this.attribute = attribute;
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FieldContainer)) {
                return false;
            }

            FieldContainer fc = (FieldContainer) obj;
            return (start == fc.start && end == fc.end
                    && attribute == fc.attribute && value.equals(fc.value));
        }
    }
}
