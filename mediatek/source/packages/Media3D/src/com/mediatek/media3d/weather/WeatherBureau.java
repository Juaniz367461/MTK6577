package com.mediatek.media3d.weather;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.RemoteException;
import com.mediatek.location.LocationEx;
import com.mediatek.media3d.LogUtil;
import com.mediatek.media3d.Media3D;
import com.mediatek.media3d.Setting;
import com.mediatek.weather.IWeatherService;
import com.mediatek.weather.IWeatherServiceCallback;
import com.mediatek.weather.Weather;
import com.mediatek.location.LocationUpdater;
import java.util.LinkedList;

public class WeatherBureau {
    private static final String TAG = "Media3D.WeatherBureau";
    IWeatherService mService;
    boolean mIsInited;
    Context  mContext;

    public final LinkedList<LocationWeather> mLocations = new LinkedList<LocationWeather>();
    private static final LocationWeather INVALID_LOCATION = new LocationWeather(-1);

    public void init(final Context context) {
        LogUtil.v(TAG);
        if (!mIsInited) {
            mContext = context;
            context.bindService(new Intent(IWeatherService.class.getName()), mConnection, Context.BIND_AUTO_CREATE);
            LocationWeather.setContentResolver(context.getContentResolver());
            mIsInited = true;
        }
    }

    public void startRequestLocationUpdate(final Context context) {
        if (mIsInited) {
            registerLocationListener(context);
        }
    }

    public void stopRequestLocationUpdate() {
        unregisterLocationListener();
    }

    private final static int INTERVAL_MIN = 30;
    private void registerLocationListener(final Context context) {
        if (mLocationUpdater == null) {
            mLocationUpdater = new LocationUpdater(context);
        }
        mLocationUpdater.registerLocationListener(mLocationListener);
        mLocationUpdater.requestLocationUpdates(INTERVAL_MIN);
    }

    private LocationUpdater mLocationUpdater;
    private final LocationWeather mCurrentCity = new LocationWeather(0);
    private final LocationUpdater.LocationListener mLocationListener = new LocationUpdater.LocationListener() {
        public void onLocationChanged(LocationEx location) {
            if (location != null) {
                LogUtil.v(TAG, "Woeid :" + location.getWoeid());
                mCurrentCity.fetch(location);
                queryCurrentCityAsync();
            }
        }

        public void onStatusChanged(int msg) {
            LogUtil.v(TAG, "message :" + msg);
        }
    };

    private void queryCurrentCityAsync() {
        for (int i = 0; i < mLocations.size(); ++i) {
            if (mLocations.get(i).getCityId() == mCurrentCity.getCityId()) {
                return;
            }
        }
        mLocations.addFirst(mCurrentCity);
        new QueryCurrentCityTask().execute();
    }

    private class QueryCurrentCityTask extends AsyncTask<Void, Void, Boolean> {
        private boolean isContained(Cursor c) {
            if (c.getCount() == 0) {
                return false;
            }

            c.moveToFirst();
            do{
                LogUtil.v(TAG, "City id : " + c.getInt(c.getColumnIndex(Weather.City.ID)));
                if (c.getInt(c.getColumnIndex(Weather.City.ID)) ==
                        mCurrentCity.getCityId()) {
                    return true;
                }
            } while (c.moveToNext());
            return false;
        }

        protected Boolean doInBackground(Void... none) {
            if (mContext == null) {
                return Boolean.FALSE;
            }

            int nextPosition = 0;
            Cursor cursor = null;
            try {
                cursor = mContext.getContentResolver().query(Weather.City.CONTENT_URI,
                    new String[] {Weather.City.ID}, null, null, null);
                if (cursor != null) {
                    nextPosition = cursor.getCount();
                    if (isContained(cursor)) {
                        cursor.close();
                        return Boolean.FALSE;
                    }
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            ContentValues values = new ContentValues();
            values.put(Weather.City.ID, mCurrentCity.getCityId());
            values.put(Weather.City.NAME, mCurrentCity.getLocationName());
            values.put(Weather.City.LATITUDE, mCurrentCity.getLatitude());
            values.put(Weather.City.LONGITUDE, mCurrentCity.getLongitude());
            values.put(Weather.City.POSITION, nextPosition);
            // insert to database
            mContext.getContentResolver().insert(Weather.City.CONTENT_URI, values);

            if (mService != null) {
                try {
                    mService.updateWeather(mCurrentCity.getCityId(), -1);
                } catch (RemoteException e) {
                    LogUtil.v(TAG, "exception :" + e.getMessage());
                }
            }

            mCurrentCity.queryCurrentWeatherStatus(true);
            return Boolean.TRUE;
        }

        protected void onPostExecute(Boolean result) {
            // Only update once to reduce power consumption.
            stopRequestLocationUpdate();
        }
    }

    public void updateLocations(Context context) {
        if (Media3D.isDemoMode()) {
            queryDemoLocations();
        } else {
            queryLocations(context);
        }
    }

    private void queryLocations(Context context) {
        final Cursor cursor = context.getContentResolver().query(Weather.City.CONTENT_URI,
                null, null, null, null);
        if (cursor == null) {
            return;
        }

        mLocations.clear();
        if (cursor.getCount() != 0 && cursor.moveToFirst()) {
            int index = 0;
            do {
                int cityId = cursor.getInt(cursor.getColumnIndex(Weather.City.ID));
                final LocationWeather lw = new LocationWeather(index++, cityId);
                boolean isSynchronousTry = mLocations.isEmpty();
                lw.query(cursor, isSynchronousTry);
                mLocations.add(lw);
            } while (cursor.moveToNext());
        }
        cursor.close();
    }

    private void queryDemoLocations() {
        mLocations.add(
                new LocationWeather(0, "New York", "GMT-5", Weather.WeatherCondition.Snow, 40.43f, -74f, -10, -15, 8, 1));
        mLocations.add(
                new LocationWeather(1, "Tokyo", "GMT+9", Weather.WeatherCondition.Sunny, 35.42f, 139.f, 20, 10, 30, 1));
        mLocations.add(
                new LocationWeather(2, "Beijing", "GMT+8", Weather.WeatherCondition.Cloudy, 39.54f, 116.23f, -2, -10, 9, 1));
        mLocations.add(
                new LocationWeather(3, "London", "GMT", Weather.WeatherCondition.Rain, 51.30f, 0.7f, 3, 0, 15, 1));
        mLocations.add(
                new LocationWeather(4, "Taipei", "GMT+8", Weather.WeatherCondition.Sand, 25.2f, 121.38f, 20, 10, 30, 1));
    }

    private void unregisterLocationListener() {
        if (mLocationUpdater != null) {
            mLocationUpdater.unregisterLocationListener(mLocationListener);
            mLocationUpdater = null;
        }
    }

    public void deInit() {
        LogUtil.v(TAG);
        if (!mIsInited) {
            return;
        }

        stopRequestLocationUpdate();

        if (mService != null) {
            try {
                mService.unregisterCallback(mCallback);
            } catch (RemoteException e) {
                // do nothing
            }
        }
        mContext.unbindService(mConnection);
        mService = null;
        mLocations.clear();
        LocationWeather.setWeatherService(null);
        LocationWeather.setContentResolver(null);
        mContext = null;
        mIsInited = false;
    }

    public int getLocationCount() {
        LogUtil.v(TAG, "Location size : " + mLocations.size());
        return mLocations.size();
    }

    public LocationWeather getLocationByIndex(int index) {
        if (index < 0 || index >= mLocations.size()) {
            return INVALID_LOCATION;
        }
        return mLocations.get(index);
    }

    public LocationWeather getLocationByCityId(int cityId) {
        for (int i = 0; i < mLocations.size(); i++) {
            if (mLocations.get(i).getCityId() == cityId) {
                return mLocations.get(i);
            }
        }
        return INVALID_LOCATION;
    }

    public int getNext(final int index) {
        if (index == (getLocationCount() - 1) ) {
            return 0;
        }
        return (index + 1);
    }

    public int getPrev(final int index) {
        if (index == 0 && !mLocations.isEmpty()) {
            return (getLocationCount() - 1);
        }
        return (index - 1);
    }


    public void refreshWeather(final int cityId) {
        try {
            if (mService != null) {
                final int result = mService.updateWeather(cityId, System.currentTimeMillis());
                LogUtil.v(TAG, "result :" + result);
            }
        } catch (RemoteException e) {
            LogUtil.v(TAG, "exception :" + e.getMessage());
        }

        getLocationByCityId(cityId).queryWeather();
    }

    /**
     * Class for interacting with the main interface of the service.
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IWeatherService.Stub.asInterface(service);
            LocationWeather.setWeatherService(mService);
            try {
                mService.registerCallback(mCallback);
            } catch (RemoteException e) {
            }
        }

        @SuppressWarnings("PMD.UncommentedEmptyMethod")
        public void onServiceDisconnected(ComponentName className) {
        }
    };

    private final IWeatherServiceCallback mCallback = new IWeatherServiceCallback.Stub() {
        public void onWeatherUpdate(int cityId, int result) throws RemoteException {
            LogUtil.v(TAG);
            getLocationByCityId(cityId).queryWeather();
        }
    };
}
