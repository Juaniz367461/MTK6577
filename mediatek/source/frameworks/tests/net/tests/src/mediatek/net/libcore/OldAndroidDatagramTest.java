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
 * Copyright (C) 2008 The Android Open Source Project
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

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import junit.framework.TestCase;

/**
 * Implements some simple tests for datagrams. Not as excessive as the core
 * tests, but good enough for the harness.
 */
public class OldAndroidDatagramTest extends TestCase {

    /**
     * Helper class that listens to incoming datagrams and reflects them to the
     * sender. Incoming datagram is interpreted as a String. It is uppercased
     * before being sent back.
     */

    class Reflector extends Thread {
        // Helper class for reflecting incoming datagrams. 
        DatagramSocket socket;

        boolean alive = true;

        byte[] buffer = new byte[256];

        DatagramPacket packet;

        /**
         * Main loop. Receives datagrams and reflects them.
         */
        @Override
        public void run() {
            try {
                while (alive) {
                    try {
                        packet.setLength(buffer.length);
                        socket.receive(packet);
                        String s = stringFromPacket(packet);
                        // System.out.println(s + " (from " + packet.getAddress() + ":" + packet.getPort() + ")");

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ex) {
                            // Ignore.
                        }

                        stringToPacket(s.toUpperCase(), packet);

                        packet.setAddress(InetAddress.getLocalHost());
                        packet.setPort(2345);

                        socket.send(packet);
                    } catch (java.io.InterruptedIOException e) {
                    }
                }
            } catch (java.io.IOException ex) {
                ex.printStackTrace();
            } finally {
                socket.close();
            }
        }

        /**
         * Creates a new Relfector object for the given local address and port.
         */
        public Reflector(int port, InetAddress address) {
            try {
                packet = new DatagramPacket(buffer, buffer.length);
                socket = new DatagramSocket(port, address);
            } catch (IOException ex) {
                throw new RuntimeException(
                        "Creating datagram reflector failed", ex);
            }
        }
    }

    /**
     * Converts a given datagram packet's contents to a String.
     */
    static String stringFromPacket(DatagramPacket packet) {
        return new String(packet.getData(), 0, packet.getLength());
    }

    /**
     * Converts a given String into a datagram packet.
     */
    static void stringToPacket(String s, DatagramPacket packet) {
        byte[] bytes = s.getBytes();
        System.arraycopy(bytes, 0, packet.getData(), 0, bytes.length);
        packet.setLength(bytes.length);
    }

    /**
     * Implements the main part of the Datagram test.
     */
    public void testDatagram() throws Exception {

        Reflector reflector = null;
        DatagramSocket socket = null;

        try {
            // Setup the reflector, so we have a partner to send to
            reflector = new Reflector(1234, InetAddress.getLocalHost());
            reflector.start();

            byte[] buffer = new byte[256];

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket = new DatagramSocket(2345, InetAddress.getLocalHost());

            // Send ten simple packets and check for the expected responses.
            for (int i = 1; i <= 10; i++) {
                String s = "Hello, Android world #" + i + "!";
                stringToPacket(s, packet);

                packet.setAddress(InetAddress.getLocalHost());
                packet.setPort(1234);

                socket.send(packet);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    // Ignore.
                }

                packet.setLength(buffer.length);
                socket.receive(packet);
                String t = stringFromPacket(packet);
                // System.out.println(t + " (from " + packet.getAddress() + ":" + packet.getPort() + ")");

                assertEquals(s.toUpperCase(), t);
            }
        } finally {
            if (reflector != null) {
                reflector.alive = false;
            }

            if (socket != null) {
                socket.close();
            }
        }
    }

    // Regression test for issue 1018003: DatagramSocket ignored a set timeout.
    public void testDatagramSocketSetSOTimeout() throws Exception {
        DatagramSocket sock = null;
        int timeout = 5000;
        long start = System.currentTimeMillis();
        try {
            sock = new DatagramSocket();
            DatagramPacket pack = new DatagramPacket(new byte[100], 100);
            sock.setSoTimeout(timeout);
            sock.receive(pack);
        } catch (SocketTimeoutException e) {
            // expected
            long delay = System.currentTimeMillis() - start;
            if (Math.abs(delay - timeout) > 1000) {
                fail("timeout was not accurate. expected: " + timeout
                        + " actual: " + delay + " miliseconds.");
            }
        } finally {
            if (sock != null) {
                sock.close();
            }
        }
    }
}
