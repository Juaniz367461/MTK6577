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

package com.mediatek.bluetooth.map;

import com.mediatek.bluetooth.map.Instance;
import com.mediatek.bluetooth.map.MAP;

import java.util.ArrayList;
import java.util.HashSet;
import android.content.Context;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothMap;
import com.mediatek.bluetooth.map.util.*;

import android.util.Log;
import com.mediatek.xlog.Xlog;

class InstanceManager {
	private final String TAG = "MAP InstanceManager";
	private Context mContext;
	private static InstanceManager mManager ;
	private ArrayList<Instance> mInstances = new ArrayList<Instance>();

	public static InstanceManager getDefaultManager (Context context) {
		synchronized(InstanceManager.class) {
			if (mManager == null) {
				mManager = new InstanceManager();
			}	
			if (!mManager.init(context)) {
				return null;
			}
			return mManager;
		}
	}

	private boolean init(Context context) {
		mContext = context;
		return true;
	}

	//generate instance when initializing, but the instance has no message controller until user set in BluetoothAdvanceSettings
	public ArrayList<Instance> generateInstances(Context context) {
		mInstances.add(createInstance(true));
		if (NetworkUtil.isGeminiSupport()) {
			mInstances.add(createInstance(false));
		}
		return mInstances;
	}

	private Instance createInstance(boolean isDefault) {
		int type = 0;
		int simId = -1;

		//EMAIL message will be exposed in default instance
		type = type | MAP.MSG_TYPE_MMS;
		if (isDefault) {
			type = type | MAP.MSG_TYPE_EMAIL;
		}

		//SMS type
		if (NetworkUtil.isGeminiSupport()) {			
			simId = isDefault ? NetworkUtil.SIM1 : NetworkUtil.SIM2;
			type = type | NetworkUtil.getGeminiSmsType(simId);
		} else {
			simId = NetworkUtil.SIM1; 
			type = type | NetworkUtil.getSmsType();
		}

		return new Instance(mContext,type, simId);
	}

	//when the API is called, add a controller to an instance
	/*@ type: the message type, used to construct a controller*/
	/*@ value: for MMS/SMS , the value indicate sim card ID,*/
	/*		   for EMAIL, the value indicate account ID*/
/*	public synchronized boolean registerMsgRepository(int type, int value) {
		Instance instance;
		boolean result = false;
		for (int i = 0; i < mInstances.size(); i++) {
			instance = mInstances.get(i);
			
			if (instance.isMsgTypeSupported(type) && 
				!instance.containsMessageController(type) &&
				containsValue(instance, type, value)) {
				
				return instance.addMessageController(type);
			}
		}
		return false;
	}
	public synchronized void unregisterMsgRepository(int type, int value) {
		Instance instance;
		for (int i = 0; i < mInstances.size(); i++) {
			instance = mInstances.get(i);
			if (instance.isMsgTypeSupported(type) && 
				instance.containsMessageController(type) &&
				containsValue(instance, type, value)) {
				
				return instance.removeMessageController(type);
			}
			break;
		}
	}
	public synchronized boolean replaceMsgRepository(int type, int value) {
		Instance instance;
		for (int i = 0; i < mInstances.size(); i++) {
			instance = mInstances.get(i);
			
			if (instance.isMsgTypeSupported(type)) {
					instance.updateMessageController(type, value);
			}
			break;
		}
	}
	*/
	public boolean registerSim(int simid) {
		Instance instance;
		log("registerSim");
		for (int i = 0; i < mInstances.size(); i++) {
			instance = mInstances.get(i);
			log("instance.getSimId()"+instance.getSimId());
			if (instance.getSimId() == simid) {
				instance.enableSim(true);
			break;
		}
		}
		return true;
	}

	public boolean unregisterSim(int simid) {
		Instance instance;
		for (int i = 0; i < mInstances.size(); i++) {
			instance = mInstances.get(i);
			
			if (instance.getSimId() == simid) {
				instance.disableSim();
			break;
		}		
		}		
		return true;
	}
	public boolean replaceAccount(long account) {
		Instance instance;
		for (int i = 0; i < mInstances.size(); i++) {
			instance = mInstances.get(i);
			
			if (instance.isMsgTypeSupported(MAP.MSG_TYPE_EMAIL)) {
				instance.updateMessageController(MAP.MSG_TYPE_EMAIL, account);
			break;
		}
		}
		return true;
	}
	public long getEmailAccount(){
		Instance instance;
		for (int i = 0; i < mInstances.size(); i++) {
			instance = mInstances.get(i);					
			if (instance.isMsgTypeSupported(MAP.MSG_TYPE_EMAIL)) {
				return instance.getAccountId();
			}
		}
		return -1;
	}
	//AIDL does not support Integer
	public int[] getSims(){
		Instance instance;
		int[] sims = new int[mInstances.size()];
		log("getSims():"+mInstances.size());
		
	//	HashSet<Integer> simSet = new HashSet<Integer>();
		for (int i = 0; i < mInstances.size(); i++) {
			instance = mInstances.get(i);					
			if (instance.containsMessageController(MAP.MSG_TYPE_MMS) ||
				instance.containsMessageController(MAP.MSG_TYPE_SMS_CDMA) ||
				instance.containsMessageController(MAP.MSG_TYPE_SMS_GSM)) {
				sims[i] = instance.getSimId();
			} else {
				sims[i] = -1;
			}
		}
		return sims;
	}
/*	private boolean containsValue(Instance instance, int type, int value) {
		if (type = MAP.MSG_TYPE_EMAIL) {
			return instance.getAccountId() == value;
		} else {
			return instance.getSimId() == value;
		}
		return true;
	}
	*/

	public synchronized void removeAllInstances(){
		log("removeAllInstances()");
		mInstances.clear();
	}

	public synchronized void removeInstance(int id) {
		Instance instance;
		log("removeInstance():"+id);
		for (int i = 0; i < mInstances.size(); i++) {
			instance = mInstances.get(i);
			if (instance.getInstanceId() == id) {
				 mInstances.remove(i);
				 break;
			}
		}
	}

	
	public Instance getInstanceById(int id){
		Instance instance;
		for (int i = 0; i < mInstances.size(); i++) {
			instance = mInstances.get(i);
			if (instance.getInstanceId() == id) {
				return instance;
			}
		}
		return null;
	}
	public ArrayList<Instance> getInstanceByDevice(BluetoothDevice device) {
		ArrayList<Instance> instanceList = new ArrayList<Instance>();
		Instance instance;	
		if (device == null) {
			return instanceList;
		}
		for (int i = 0; i < mInstances.size(); i++) {
			instance = mInstances.get(i);
			//notes: maybe device is null when instance is not connected
	//		if (instance.getDevice().equals(device)) {
			if (device.equals(instance.getDevice())) {
				instanceList.add(instance);
			}
		}
		return instanceList;
		
	}

	public ArrayList<Instance> getAllInstances(){
		return mInstances;
	}
	

	public boolean isConnected(BluetoothDevice device) {
		if (device == null) {
			return false;
		}
		Instance instance;
		for (int i = 0; i < mInstances.size(); i++) {
			instance = mInstances.get(i);
	//		if(instance.getDevice().equals(device)) {
			if (device.equals(instance.getDevice())) {
				return true;
			}
		}
		return false;
	}
	
	public BluetoothDevice[] getConnectedDevices() {
		HashSet<BluetoothDevice> devices = new HashSet<BluetoothDevice>();
		Instance instance;
		for (int i = 0; i < mInstances.size(); i++) {
			instance = mInstances.get(i);
			log("instance device is "+ instance.getDevice() == null ? instance.getDevice().getAddress():"null");
			devices.add(instance.getDevice());
		}
		return devices.toArray(new BluetoothDevice[devices.size()]);		
	}

	public int getState(BluetoothDevice device) {
		if (isConnected(device)) {
			return BluetoothMap.STATE_CONNECTED;
		} 
		return BluetoothMap.STATE_DISCONNECTED;
	}


	private void log(String info) {
		if (null != info){
			Xlog.v(TAG, info);
		}
	}

	/*interface MapServerCallBack{
		public boolean registerInstance(Instance instance);
		public boolean 
	}*/
	

}
