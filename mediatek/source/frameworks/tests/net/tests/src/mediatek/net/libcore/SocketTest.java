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

package libcore.java.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketImpl;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SocketTest extends junit.framework.TestCase {
    // See http://b/2980559.
    public void test_close() throws Exception {
        Socket s = new Socket();
        s.close();
        // Closing a closed socket does nothing.
        s.close();
    }

    /**
     * Our getLocalAddress and getLocalPort currently use getsockname(3).
     * This means they give incorrect results on closed sockets (as well
     * as requiring an unnecessary call into native code).
     */
    public void test_getLocalAddress_after_close() throws Exception {
        Socket s = new Socket();
        try {
            // Bind to an ephemeral local port.
            s.bind(new InetSocketAddress("localhost", 0));
            assertTrue(s.getLocalAddress().toString(), s.getLocalAddress().isLoopbackAddress());
            // What local port did we get?
            int localPort = s.getLocalPort();
            assertTrue(localPort > 0);
            // Now close the socket...
            s.close();
            // The RI returns the ANY address but the original local port after close.
            assertTrue(s.getLocalAddress().isAnyLocalAddress());
            assertEquals(localPort, s.getLocalPort());
        } finally {
            s.close();
        }
    }

    // http://code.google.com/p/android/issues/detail?id=7935
    public void test_newSocket_connection_refused() throws Exception {
        try {
            new Socket("localhost", 80);
            fail("connection should have been refused");
        } catch (ConnectException expected) {
        }
    }

    // http://code.google.com/p/android/issues/detail?id=3123
    // http://code.google.com/p/android/issues/detail?id=1933
    public void test_socketLocalAndRemoteAddresses() throws Exception {
        checkSocketLocalAndRemoteAddresses(false);
        checkSocketLocalAndRemoteAddresses(true);
    }

    public void checkSocketLocalAndRemoteAddresses(boolean setOptions) throws Exception {
        InetAddress host = InetAddress.getLocalHost();

        // Open a local server port.
        ServerSocketChannel ssc = ServerSocketChannel.open();
        InetSocketAddress listenAddr = new InetSocketAddress(host, 0);
        ssc.socket().bind(listenAddr, 0);
        ServerSocket ss = ssc.socket();

        // Open a socket to the local port.
        SocketChannel out = SocketChannel.open();
        out.configureBlocking(false);
        if (setOptions) {
            out.socket().setTcpNoDelay(false);
        }
        InetSocketAddress addr = new InetSocketAddress(host, ssc.socket().getLocalPort());
        out.connect(addr);
        while (!out.finishConnect()) {
            Thread.sleep(1);
        }

        SocketChannel in = ssc.accept();
        if (setOptions) {
            in.socket().setTcpNoDelay(false);
        }

        InetSocketAddress outRemoteAddress = (InetSocketAddress) out.socket().getRemoteSocketAddress();
        InetSocketAddress outLocalAddress = (InetSocketAddress) out.socket().getLocalSocketAddress();
        InetSocketAddress inLocalAddress = (InetSocketAddress) in.socket().getLocalSocketAddress();
        InetSocketAddress inRemoteAddress = (InetSocketAddress) in.socket().getRemoteSocketAddress();
        System.err.println("inLocalAddress: " + inLocalAddress);
        System.err.println("inRemoteAddress: " + inRemoteAddress);
        System.err.println("outLocalAddress: " + outLocalAddress);
        System.err.println("outRemoteAddress: " + outRemoteAddress);

        assertEquals(outRemoteAddress.getPort(), ss.getLocalPort());
        assertEquals(inLocalAddress.getPort(), ss.getLocalPort());
        assertEquals(inRemoteAddress.getPort(), outLocalAddress.getPort());

        assertEquals(inLocalAddress.getAddress(), ss.getInetAddress());
        assertEquals(inRemoteAddress.getAddress(), ss.getInetAddress());
        assertEquals(outLocalAddress.getAddress(), ss.getInetAddress());
        assertEquals(outRemoteAddress.getAddress(), ss.getInetAddress());

        in.close();
        out.close();
        ssc.close();

        assertNull(in.socket().getRemoteSocketAddress());
        assertNull(out.socket().getRemoteSocketAddress());

        assertEquals(in.socket().getLocalSocketAddress(), ss.getLocalSocketAddress());
    }

    // SocketOptions.setOption has weird behavior for setSoLinger/SO_LINGER.
    // This test ensures we do what the RI does.
    public void test_SocketOptions_setOption() throws Exception {
        class MySocketImpl extends SocketImpl {
            public int option;
            public Object value;

            public boolean createCalled;
            public boolean createStream;

            public MySocketImpl() { super(); }
            @Override protected void accept(SocketImpl arg0) throws IOException { }
            @Override protected int available() throws IOException { return 0; }
            @Override protected void bind(InetAddress arg0, int arg1) throws IOException { }
            @Override protected void close() throws IOException { }
            @Override protected void connect(String arg0, int arg1) throws IOException { }
            @Override protected void connect(InetAddress arg0, int arg1) throws IOException { }
            @Override protected void connect(SocketAddress arg0, int arg1) throws IOException { }
            @Override protected InputStream getInputStream() throws IOException { return null; }
            @Override protected OutputStream getOutputStream() throws IOException { return null; }
            @Override protected void listen(int arg0) throws IOException { }
            @Override protected void sendUrgentData(int arg0) throws IOException { }
            public Object getOption(int arg0) throws SocketException { return null; }

            @Override protected void create(boolean isStream) throws IOException {
                this.createCalled = true;
                this.createStream = isStream;
            }

            public void setOption(int option, Object value) throws SocketException {
                this.option = option;
                this.value = value;
            }
        }

        class MySocket extends Socket {
            public MySocket(SocketImpl impl) throws SocketException {
                super(impl);
            }
        }

        MySocketImpl impl = new MySocketImpl();
        Socket s = new MySocket(impl);

        // Check that, as per the SocketOptions.setOption documentation, we pass false rather
        // than -1 to the SocketImpl when setSoLinger is called with the first argument false.
        s.setSoLinger(false, -1);
        assertEquals(Boolean.FALSE, (Boolean) impl.value);
        // We also check that SocketImpl.create was called. SocketChannelImpl.SocketAdapter
        // subclasses Socket, and whether or not to call SocketImpl.create is the main behavioral
        // difference.
        assertEquals(true, impl.createCalled);
        s.setSoLinger(false, 0);
        assertEquals(Boolean.FALSE, (Boolean) impl.value);
        s.setSoLinger(false, 1);
        assertEquals(Boolean.FALSE, (Boolean) impl.value);

        // Check that otherwise, we pass down an Integer.
        s.setSoLinger(true, 0);
        assertEquals(Integer.valueOf(0), (Integer) impl.value);
        s.setSoLinger(true, 1);
        assertEquals(Integer.valueOf(1), (Integer) impl.value);
    }

    public void test_setTrafficClass() throws Exception {
        Socket s = new Socket();
        s.setTrafficClass(123);
        assertEquals(123, s.getTrafficClass());
    }

    public void testReadAfterClose() throws Exception {
        MockServer server = new MockServer();
        server.enqueue(new byte[]{5, 3}, 0);
        Socket socket = new Socket("localhost", server.port);
        InputStream in = socket.getInputStream();
        assertEquals(5, in.read());
        assertEquals(3, in.read());
        assertEquals(-1, in.read());
        assertEquals(-1, in.read());
        socket.close();
        in.close();

        /*
         * Rather astonishingly, read() doesn't throw even though the stream is
         * closed. This is consistent with the RI's behavior.
         */
        assertEquals(-1, in.read());
        assertEquals(-1, in.read());

        server.shutdown();
    }

    public void testWriteAfterClose() throws Exception {
        MockServer server = new MockServer();
        server.enqueue(new byte[0], 3);
        Socket socket = new Socket("localhost", server.port);
        OutputStream out = socket.getOutputStream();
        out.write(5);
        out.write(3);
        socket.close();
        out.close();

        try {
            out.write(9);
            fail();
        } catch (IOException expected) {
        }

        server.shutdown();
    }

    static class MockServer {
        private ExecutorService executor;
        private ServerSocket serverSocket;
        private int port = -1;

        MockServer() throws IOException {
            executor = Executors.newCachedThreadPool();
            serverSocket = new ServerSocket(0);
            serverSocket.setReuseAddress(true);
            port = serverSocket.getLocalPort();
        }

        public Future<byte[]> enqueue(final byte[] sendBytes, final int receiveByteCount)
                throws IOException {
            return executor.submit(new Callable<byte[]>() {
                @Override public byte[] call() throws Exception {
                    Socket socket = serverSocket.accept();
                    OutputStream out = socket.getOutputStream();
                    out.write(sendBytes);

                    InputStream in = socket.getInputStream();
                    byte[] result = new byte[receiveByteCount];
                    int total = 0;
                    while (total < receiveByteCount) {
                        total += in.read(result, total, result.length - total);
                    }
                    socket.close();
                    return result;
                }
            });
        }

        public void shutdown() throws IOException {
            serverSocket.close();
            executor.shutdown();
        }
    }
}
