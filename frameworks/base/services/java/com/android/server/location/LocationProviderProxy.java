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

package com.android.server.location;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Criteria;
import android.location.ILocationProvider;
import android.location.Location;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.WorkSource;
import android.util.Log;
import com.mediatek.featureoption.FeatureOption;
import android.provider.Settings;
import com.mediatek.xlog.Xlog;
import com.android.internal.location.DummyLocationProvider;

/**
 * A class for proxying location providers implemented as services.
 * 
 * {@hide}
 */
public class LocationProviderProxy implements LocationProviderInterface
{

	private static final String TAG = "LocationProviderProxy";

	private final Context mContext;
	private final String mName;
	private Intent mIntent;
	private final Handler mHandler;
	private final Object mMutex = new Object(); // synchronizes access to
	// non-final members
	private Connection mServiceConnection = null; // never null

	// cached values set by the location manager
	private boolean mLocationTracking = false;
	private boolean mEnabled = false;
	private long mMinTime = -1;
	private WorkSource mMinTimeSource = new WorkSource();
	private int mNetworkState;
	private NetworkInfo mNetworkInfo;
	// added for test
	private boolean mIsBindNetworkSucess = false;
	private String mNetworkServiceName = "Default";
	private static final String NETWORKLOCATION_BINDSERVICENAME = "NetworkLocationBindServiceName";
	private static final String NETWORKLOCATION_BINDSERVICESUCESS = "NetworkLocationBindServiceSucess";

	// added for test

	public boolean getIsBindNetworkSucess()
	{
		return mIsBindNetworkSucess;
	}

	public String getNetworkServiceName()
	{
		return mNetworkServiceName;
	}

	// constructor for proxying location providers implemented in a separate
	// service
	public LocationProviderProxy(Context context, String name, String serviceName, Handler handler)
	{
		mContext = context;
		mName = name;
		mHandler = handler;
		loadNetworkLocationProvider(serviceName);
	}

	private void loadNetworkLocationProvider(String serviceName)
	{
		if (serviceName != null)
		{
			mIntent = new Intent(serviceName);
			boolean bRet = false;
			if (serviceName.equalsIgnoreCase("com.mediatek.android.location.NetworkLocationProvider"))
			{
				if (FeatureOption.MTK_GEMINI_SUPPORT)
				{
					mIntent.putExtra("GEMINI_SUPPORT", true);
				}
				else
				{
					mIntent.putExtra("GEMINI_SUPPORT", false);
				}
			}
			mServiceConnection = new Connection();
			bRet = mContext.bindService(mIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
			Xlog.d(TAG, "LocationProviderProxy:bind networkLocationProvider service " + (bRet == true ? "sucess" : "failed") + " name:" + serviceName);
			Settings.Secure.putString(mContext.getContentResolver(), "ActualNetworkServiceName", serviceName);
			Settings.Secure.putInt(mContext.getContentResolver(), "ActualNetworkServiceSucess", bRet == true ? 1 : 0);
			mIsBindNetworkSucess = bRet;
			mNetworkServiceName = serviceName;
		}
	}

	/**
	 * When unbundled NetworkLocationService package is updated, we need to
	 * unbind from the old version and re-bind to the new one.
	 */
	public void reconnect()
	{
		synchronized (mMutex)
		{
			Xlog.d(TAG, "LocationProviderProxy:reconnect() is called");
			mContext.unbindService(mServiceConnection);
			mServiceConnection = new Connection();
			mContext.bindService(mIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
		}
	}

	public void reconnectAfterIPO(String serviceName)
	{
		synchronized (mMutex)
		{
			Xlog.d(TAG, "LocationProviderProxy:reconnect() after is called");
			loadNetworkLocationProvider(serviceName);
		}
	}

	private class Connection implements ServiceConnection, Runnable
	{

		private ILocationProvider mProvider;

		// for caching requiresNetwork, requiresSatellite, etc.
		private DummyLocationProvider mCachedAttributes; // synchronized by

		// mMutex

		public void onServiceConnected(ComponentName className, IBinder service)
		{
			Xlog.d(TAG, "LocationProviderProxy.onServiceConnected " + className);
			synchronized (this)
			{
				mProvider = ILocationProvider.Stub.asInterface(service);
				mHandler.post(this);
			}
		}

		public void onServiceDisconnected(ComponentName className)
		{
			Xlog.d(TAG, "LocationProviderProxy.onServiceDisconnected " + className);
			synchronized (this)
			{
				mProvider = null;
			}
		}

		public synchronized ILocationProvider getProvider()
		{
			return mProvider;
		}

		public synchronized DummyLocationProvider getCachedAttributes()
		{
			return mCachedAttributes;
		}

		public void run()
		{
			if (LocationServiceDebug.DEBUG_LOCATPROVIDERPROXY)
			{
				Xlog.d(TAG, "LocationProviderProxy.run() is called for caching");
			}
			synchronized (mMutex)
			{
				if (mServiceConnection != this)
				{
					// This ServiceConnection no longer the one we want to bind
					// to.
					return;
				}
				ILocationProvider provider = getProvider();
				if (provider == null)
				{
					return;
				}

				// resend previous values from the location manager if the
				// service has restarted
				try
				{
					if (mEnabled)
					{
						provider.enable();
					}
					if (mLocationTracking)
					{
						provider.enableLocationTracking(true);
					}
					if (mMinTime >= 0)
					{
						provider.setMinTime(mMinTime, mMinTimeSource);
					}
					if (mNetworkInfo != null)
					{
						provider.updateNetworkState(mNetworkState, mNetworkInfo);
					}
				}
				catch (RemoteException e)
				{
				}

				// init cache of parameters
				if (mCachedAttributes == null)
				{
					try
					{
						mCachedAttributes = new DummyLocationProvider(mName, null);
						mCachedAttributes.setRequiresNetwork(provider.requiresNetwork());
						mCachedAttributes.setRequiresSatellite(provider.requiresSatellite());
						mCachedAttributes.setRequiresCell(provider.requiresCell());
						mCachedAttributes.setHasMonetaryCost(provider.hasMonetaryCost());
						mCachedAttributes.setSupportsAltitude(provider.supportsAltitude());
						mCachedAttributes.setSupportsSpeed(provider.supportsSpeed());
						mCachedAttributes.setSupportsBearing(provider.supportsBearing());
						mCachedAttributes.setPowerRequirement(provider.getPowerRequirement());
						mCachedAttributes.setAccuracy(provider.getAccuracy());
					}
					catch (RemoteException e)
					{
						mCachedAttributes = null;
					}
				}
			}
		}
	}

	public String getName()
	{
		return mName;
	}

	private DummyLocationProvider getCachedAttributes()
	{
		synchronized (mMutex)
		{
			return mServiceConnection.getCachedAttributes();
		}
	}

	public boolean requiresNetwork()
	{
		DummyLocationProvider cachedAttributes = getCachedAttributes();
		if (cachedAttributes != null)
		{
			return cachedAttributes.requiresNetwork();
		}
		else
		{
			return false;
		}
	}

	public boolean requiresSatellite()
	{
		DummyLocationProvider cachedAttributes = getCachedAttributes();
		if (cachedAttributes != null)
		{
			return cachedAttributes.requiresSatellite();
		}
		else
		{
			return false;
		}
	}

	public boolean requiresCell()
	{
		DummyLocationProvider cachedAttributes = getCachedAttributes();
		if (cachedAttributes != null)
		{
			return cachedAttributes.requiresCell();
		}
		else
		{
			return false;
		}
	}

	public boolean hasMonetaryCost()
	{
		DummyLocationProvider cachedAttributes = getCachedAttributes();
		if (cachedAttributes != null)
		{
			return cachedAttributes.hasMonetaryCost();
		}
		else
		{
			return false;
		}
	}

	public boolean supportsAltitude()
	{
		DummyLocationProvider cachedAttributes = getCachedAttributes();
		if (cachedAttributes != null)
		{
			return cachedAttributes.supportsAltitude();
		}
		else
		{
			return false;
		}
	}

	public boolean supportsSpeed()
	{
		DummyLocationProvider cachedAttributes = getCachedAttributes();
		if (cachedAttributes != null)
		{
			return cachedAttributes.supportsSpeed();
		}
		else
		{
			return false;
		}
	}

	public boolean supportsBearing()
	{
		DummyLocationProvider cachedAttributes = getCachedAttributes();
		if (cachedAttributes != null)
		{
			return cachedAttributes.supportsBearing();
		}
		else
		{
			return false;
		}
	}

	public int getPowerRequirement()
	{
		DummyLocationProvider cachedAttributes = getCachedAttributes();
		if (cachedAttributes != null)
		{
			return cachedAttributes.getPowerRequirement();
		}
		else
		{
			return -1;
		}
	}

	public int getAccuracy()
	{
		DummyLocationProvider cachedAttributes = getCachedAttributes();
		if (cachedAttributes != null)
		{
			return cachedAttributes.getAccuracy();
		}
		else
		{
			return -1;
		}
	}

	public boolean meetsCriteria(Criteria criteria)
	{
		synchronized (mMutex)
		{
			ILocationProvider provider = mServiceConnection.getProvider();
			if (provider != null)
			{
				try
				{
					return provider.meetsCriteria(criteria);
				}
				catch (RemoteException e)
				{
				}
			}
		}
		// default implementation if we lost connection to the provider
		if ((criteria.getAccuracy() != Criteria.NO_REQUIREMENT) && (criteria.getAccuracy() < getAccuracy()))
		{
			return false;
		}
		int criteriaPower = criteria.getPowerRequirement();
		if ((criteriaPower != Criteria.NO_REQUIREMENT) && (criteriaPower < getPowerRequirement()))
		{
			return false;
		}
		if (criteria.isAltitudeRequired() && !supportsAltitude())
		{
			return false;
		}
		if (criteria.isSpeedRequired() && !supportsSpeed())
		{
			return false;
		}
		if (criteria.isBearingRequired() && !supportsBearing())
		{
			return false;
		}
		return true;
	}

	public void enable()
	{
		synchronized (mMutex)
		{
			mEnabled = true;
			ILocationProvider provider = mServiceConnection.getProvider();
			if (provider != null)
			{
				try
				{
					provider.enable();
				}
				catch (RemoteException e)
				{
				}
			}
		}
	}

	public void disable()
	{
		synchronized (mMutex)
		{
			mEnabled = false;
			ILocationProvider provider = mServiceConnection.getProvider();
			if (provider != null)
			{
				try
				{
					provider.disable();
				}
				catch (RemoteException e)
				{
				}
			}
		}
	}

	public boolean isEnabled()
	{
		synchronized (mMutex)
		{
			return mEnabled;
		}
	}

	public int getStatus(Bundle extras)
	{
		ILocationProvider provider;
		synchronized (mMutex)
		{
			provider = mServiceConnection.getProvider();
		}
		if (provider != null)
		{
			try
			{
				return provider.getStatus(extras);
			}
			catch (RemoteException e)
			{
			}
		}
		return 0;
	}

	public long getStatusUpdateTime()
	{
		ILocationProvider provider;
		synchronized (mMutex)
		{
			provider = mServiceConnection.getProvider();
		}
		if (provider != null)
		{
			try
			{
				return provider.getStatusUpdateTime();
			}
			catch (RemoteException e)
			{
			}
		}
		return 0;
	}

	public String getInternalState()
	{
		ILocationProvider provider;
		synchronized (mMutex)
		{
			provider = mServiceConnection.getProvider();
		}
		if (provider != null)
		{
			try
			{
				return provider.getInternalState();
			}
			catch (RemoteException e)
			{
				Xlog.e(TAG, "getInternalState failed", e);
			}
		}
		return null;
	}

	public boolean isLocationTracking()
	{
		synchronized (mMutex)
		{
			return mLocationTracking;
		}
	}

	public void enableLocationTracking(boolean enable)
	{
		Xlog.d(TAG, "enableLocationTracking is called");
		synchronized (mMutex)
		{
			Xlog.d(TAG, "enableLocationTracking is called inter the mMutex");
			mLocationTracking = enable;
			if (!enable)
			{
				mMinTime = -1;
				mMinTimeSource.clear();
			}
			ILocationProvider provider = mServiceConnection.getProvider();
			if (provider != null)
			{
				try
				{
					provider.enableLocationTracking(enable);
					Xlog.d(TAG, "enableLocationTracking is called with no exception");
				}
				catch (RemoteException e)
				{
					Xlog.e(TAG, "enableLocationTracking is called with exception: " + e);
				}
			}
		}
	}

	public boolean requestSingleShotFix()
	{
		return false;
	}

	public long getMinTime()
	{
		synchronized (mMutex)
		{
			return mMinTime;
		}
	}

	public void setMinTime(long minTime, WorkSource ws)
	{
		synchronized (mMutex)
		{
			mMinTime = minTime;
			mMinTimeSource.set(ws);
			ILocationProvider provider = mServiceConnection.getProvider();
			if (provider != null)
			{
				try
				{
					provider.setMinTime(minTime, ws);
				}
				catch (RemoteException e)
				{
				}
			}
		}
	}

	public void updateNetworkState(int state, NetworkInfo info)
	{
		synchronized (mMutex)
		{
			mNetworkState = state;
			mNetworkInfo = info;
			ILocationProvider provider = mServiceConnection.getProvider();
			if (provider != null)
			{
				try
				{
					provider.updateNetworkState(state, info);
				}
				catch (RemoteException e)
				{
				}
			}
		}
	}

	public void updateLocation(Location location)
	{
		synchronized (mMutex)
		{
			ILocationProvider provider = mServiceConnection.getProvider();
			if (provider != null)
			{
				try
				{
					provider.updateLocation(location);
				}
				catch (RemoteException e)
				{
				}
			}
		}
	}

	public boolean sendExtraCommand(String command, Bundle extras)
	{
		synchronized (mMutex)
		{
			ILocationProvider provider = mServiceConnection.getProvider();
			if (provider != null)
			{
				try
				{
					return provider.sendExtraCommand(command, extras);
				}
				catch (RemoteException e)
				{
				}
			}
		}
		return false;
	}

	public void addListener(int uid)
	{
		synchronized (mMutex)
		{
			ILocationProvider provider = mServiceConnection.getProvider();
			if (provider != null)
			{
				try
				{
					provider.addListener(uid);
				}
				catch (RemoteException e)
				{
				}
			}
		}
	}

	public void removeListener(int uid)
	{
		synchronized (mMutex)
		{
			ILocationProvider provider = mServiceConnection.getProvider();
			if (provider != null)
			{
				try
				{
					provider.removeListener(uid);
				}
				catch (RemoteException e)
				{
				}
			}
		}
	}

	// For EPO
	public int getFileTime(long[] times)
	{
		return -1;
	}

	public int updateEPOFile()
	{
		return -1;
	}

}
