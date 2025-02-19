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

package tests.support;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

/**
 * Represents test data used by the Request API tests
 */
public class Support_TestWebData {
  public final static byte[] test1 = utfBytes();
  public final static byte[] test2 = newBinaryFile(8192);

  private static byte[] utfBytes() {
    try {
      return "<html>\n<body>\n<h1>Hello World!</h1>\n</body>\n</html>\n".getBytes("UTF-8");
    } catch (UnsupportedEncodingException ex) {
      throw new AssertionError();
    }
  }

  private static byte[] newBinaryFile(int byteCount) {
    byte[] result = new byte[byteCount];
    for (int i = 0; i < result.length; ++i) {
      result[i] = (byte) i;
    }
    return result;
  }

  // string for test request post body
  public final static String postContent = "user=111";

  // Array of all test data
  public final static byte[][] tests = {
    test1,
    test2
  };

  /**
   * List of static test cases for use with test server
   */
  public static Support_TestWebData[] testParams = {
    new Support_TestWebData(test1.length, 14000000, "test1", "text/html", false, 0),
    new Support_TestWebData(test2.length, 14000002, "test2", "unknown/unknown", false,
            new Date().getTime() + 100000)
  };

  /**
   * List of response strings for use by the test server
   */
  public static String[] testServerResponse = {
    "Redirecting 301",
    "Redirecting 302",
    "Redirecting 303",
    "Redirecting 307"
  };

  // Redirection indices into testServerResponse
  public final static int REDIRECT_301 = 0;
  public final static int REDIRECT_302 = 1;
  public final static int REDIRECT_303 = 2;
  public final static int REDIRECT_307 = 3;

  /**
   * Creates a data package with information used by the server when responding
   * to requests
   */
  Support_TestWebData(int length, int lastModified, String name, String type, boolean isDir, long expDate) {
    testLength = length;
    testLastModified = lastModified;
    testName = name;
    testType = type;
    testDir = isDir;
    testExp = expDate;
  }

  /**
   * Creates a data package with information used by the server when responding
   * to requests
   */
  private Support_TestWebData(String path, String type) {
    File file = new File(path);
    testLength = file.length();
    testLastModified = file.lastModified();
    testName = file.getName();
    testType = type;
    testDir = file.isDirectory();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    FileInputStream in = null;
    try {
        in = new FileInputStream(file);
        while (in.available() > 0) {
            out.write(in.read());
        }
        in.close();
        out.flush();
        test0Data = out.toByteArray();
        out.close();
        test0DataAvailable = true;
        return;
    } catch (Exception e) {
        // ignore
        e.printStackTrace();
    } finally {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }
  }

  public static void initDynamicTestWebData(String path, String type) {
      test0Params = new Support_TestWebData(path, type);
  }

  // Length of test entity body
  public long testLength;

  // Last modified date value (milliseconds)
  public long testLastModified;

  // Test identification name
  public String testName;

  // The MIME type to assume for this test
  public String testType;

  // The expiration date
  public long testExp;

  // Indicates if this is a directory or not
  public boolean testDir;

  // Indicate if test0 data has bin initialized
  public static boolean test0DataAvailable = false;

  // test0 data
  public static byte[] test0Data;

  // test0 parameters
  public static Support_TestWebData test0Params;
}
