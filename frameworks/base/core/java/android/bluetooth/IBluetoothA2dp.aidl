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

package android.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.IBinder;
/**
 * APIs for Bluetooth A2DP service
 *
 * @hide
 */
interface IBluetoothA2dp {
    // Public API
    boolean connect(in BluetoothDevice device);
    boolean disconnect(in BluetoothDevice device);
    List<BluetoothDevice> getConnectedDevices();
    List<BluetoothDevice> getDevicesMatchingConnectionStates(in int[] states);
    int getConnectionState(in BluetoothDevice device);
    boolean setPriority(in BluetoothDevice device, int priority);
    int getPriority(in BluetoothDevice device);
    boolean isA2dpPlaying(in BluetoothDevice device);

    // Internal APIs
    boolean suspendSink(in BluetoothDevice device);
    boolean resumeSink(in BluetoothDevice device);
    boolean connectSinkInternal(in BluetoothDevice device);
    boolean disconnectSinkInternal(in BluetoothDevice device);
    boolean allowIncomingConnect(in BluetoothDevice device, boolean value);

    //XH,For FM Over Bt Feature
    int getState();
    boolean setAudioPathToAudioTrack(IBinder cb);
}
