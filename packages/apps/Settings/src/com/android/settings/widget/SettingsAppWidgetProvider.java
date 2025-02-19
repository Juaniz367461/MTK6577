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

package com.android.settings.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncStorageEngine;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IPowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.android.settings.R;
import com.android.settings.bluetooth.LocalBluetoothAdapter;
import com.android.settings.bluetooth.LocalBluetoothManager;
import com.android.settings.Utils;
import com.mediatek.featureoption.FeatureOption;

/**
 * Provides control of power-related settings from a widget.
 */
public class SettingsAppWidgetProvider extends AppWidgetProvider {
    static final String TAG = "SettingsAppWidgetProvider";

    static final ComponentName THIS_APPWIDGET =
            new ComponentName("com.android.settings",
                    "com.android.settings.widget.SettingsAppWidgetProvider");

    private static LocalBluetoothAdapter sLocalBluetoothAdapter = null;

    private static final int BUTTON_WIFI = 0;
    private static final int BUTTON_BRIGHTNESS = 1;
    private static final int BUTTON_SYNC = 2;
    private static final int BUTTON_GPS = 3;
    private static final int BUTTON_BLUETOOTH = 4;

    // This widget keeps track of two sets of states:
    // "3-state": STATE_DISABLED, STATE_ENABLED, STATE_INTERMEDIATE
    // "5-state": STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON, STATE_TURNING_OFF, STATE_UNKNOWN
    private static final int STATE_DISABLED = 0;
    private static final int STATE_ENABLED = 1;
    private static final int STATE_TURNING_ON = 2;
    private static final int STATE_TURNING_OFF = 3;
    private static final int STATE_UNKNOWN = 4;
    private static final int STATE_INTERMEDIATE = 5;

    // Position in the widget bar, to enable different graphics for left, center and right buttons
    private static final int POS_LEFT = 0;
    private static final int POS_CENTER = 1;
    private static final int POS_RIGHT = 2;

    private static final int[] IND_DRAWABLE_OFF = {
        R.drawable.appwidget_settings_ind_off_l_holo,
        R.drawable.appwidget_settings_ind_off_c_holo,
        R.drawable.appwidget_settings_ind_off_r_holo
    };

    private static final int[] IND_DRAWABLE_MID = {
        R.drawable.appwidget_settings_ind_mid_l_holo,
        R.drawable.appwidget_settings_ind_mid_c_holo,
        R.drawable.appwidget_settings_ind_mid_r_holo
    };

    private static final int[] IND_DRAWABLE_ON = {
        R.drawable.appwidget_settings_ind_on_l_holo,
        R.drawable.appwidget_settings_ind_on_c_holo,
        R.drawable.appwidget_settings_ind_on_r_holo
    };

    /**
     * Minimum and maximum brightnesses.  Don't go to 0 since that makes the display unusable
     */
    private static final int MINIMUM_BACKLIGHT = android.os.Power.BRIGHTNESS_DIM + 10;
    private static final int MAXIMUM_BACKLIGHT = android.os.Power.BRIGHTNESS_ON;
    private static final int DEFAULT_BACKLIGHT = (int) (android.os.Power.BRIGHTNESS_ON * 0.4f);
    /** Minimum brightness at which the indicator is shown at half-full and ON */
    private static final int HALF_BRIGHTNESS_THRESHOLD = (int) (0.3 * MAXIMUM_BACKLIGHT);
    /** Minimum brightness at which the indicator is shown at full */
    private static final int FULL_BRIGHTNESS_THRESHOLD = (int) (0.8 * MAXIMUM_BACKLIGHT);

    private static final StateTracker sWifiState = new WifiStateTracker();
    private static final StateTracker sBluetoothState = new BluetoothStateTracker();
    private static final StateTracker sGpsState = new GpsStateTracker();
    private static final StateTracker sSyncState = new SyncStateTracker();
    private static SettingsObserver sSettingsObserver;

    /**
     * The state machine for a setting's toggling, tracking reality
     * versus the user's intent.
     *
     * This is necessary because reality moves relatively slowly
     * (turning on &amp; off radio drivers), compared to user's
     * expectations.
     */
    private abstract static class StateTracker {
        // Is the state in the process of changing?
        private boolean mInTransition = false;
        private Boolean mActualState = null;  // initially not set
        private Boolean mIntendedState = null;  // initially not set

        // Did a toggle request arrive while a state update was
        // already in-flight?  If so, the mIntendedState needs to be
        // requested when the other one is done, unless we happened to
        // arrive at that state already.
        private boolean mDeferredStateChangeRequestNeeded = false;

        /**
         * User pressed a button to change the state.  Something
         * should immediately appear to the user afterwards, even if
         * we effectively do nothing.  Their press must be heard.
         */
        public final void toggleState(Context context) {
            int currentState = getTriState(context);
            boolean newState = false;
            switch (currentState) {
                case STATE_ENABLED:
                    newState = false;
                    break;
                case STATE_DISABLED:
                    newState = true;
                    break;
                case STATE_INTERMEDIATE:
                    if (mIntendedState != null) {
                        newState = !mIntendedState;
                    }
                    break;
            }
            mIntendedState = newState;
            if (mInTransition) {
                // We don't send off a transition request if we're
                // already transitioning.  Makes our state tracking
                // easier, and is probably nicer on lower levels.
                // (even though they should be able to take it...)
                mDeferredStateChangeRequestNeeded = true;
            } else {
                mInTransition = true;
                requestStateChange(context, newState);
            }
        }

        /**
         * Return the ID of the main large image button for the setting.
         */
        public abstract int getButtonId();

        /**
         * Returns the small indicator image ID underneath the setting.
         */
        public abstract int getIndicatorId();

        /**
         * Returns the resource ID of the image to show as a function of
         * the on-vs-off state.
         */
        public abstract int getButtonImageId(boolean on);

        /**
         * Returns the position in the button bar - either POS_LEFT, POS_RIGHT or POS_CENTER.
         */
        public int getPosition() { return POS_CENTER; }

        /**
         * Updates the remote views depending on the state (off, on,
         * turning off, turning on) of the setting.
         */
        public final void setImageViewResources(Context context, RemoteViews views) {
            int buttonId = getButtonId();
            int indicatorId = getIndicatorId();
            int pos = getPosition();
            switch (getTriState(context)) {
                case STATE_DISABLED:
                    views.setImageViewResource(buttonId, getButtonImageId(false));
                    views.setImageViewResource(
                        indicatorId, IND_DRAWABLE_OFF[pos]);
                    break;
                case STATE_ENABLED:
                    views.setImageViewResource(buttonId, getButtonImageId(true));
                    views.setImageViewResource(
                        indicatorId, IND_DRAWABLE_ON[pos]);
                    break;
                case STATE_INTERMEDIATE:
                    // In the transitional state, the bottom green bar
                    // shows the tri-state (on, off, transitioning), but
                    // the top dark-gray-or-bright-white logo shows the
                    // user's intent.  This is much easier to see in
                    // sunlight.
                    if (isTurningOn()) {
                        views.setImageViewResource(buttonId, getButtonImageId(true));
                        views.setImageViewResource(
                            indicatorId, IND_DRAWABLE_MID[pos]);
                    } else {
                        views.setImageViewResource(buttonId, getButtonImageId(false));
                        views.setImageViewResource(
                            indicatorId, IND_DRAWABLE_OFF[pos]);
                    }
                    break;
            }
        }

        /**
         * Update internal state from a broadcast state change.
         */
        public abstract void onActualStateChange(Context context, Intent intent);

        /**
         * Sets the value that we're now in.  To be called from onActualStateChange.
         *
         * @param newState one of STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON,
         *                 STATE_TURNING_OFF, STATE_UNKNOWN
         */
        protected final void setCurrentState(Context context, int newState) {
            final boolean wasInTransition = mInTransition;
            switch (newState) {
                case STATE_DISABLED:
                    mInTransition = false;
                    mActualState = false;
                    break;
                case STATE_ENABLED:
                    mInTransition = false;
                    mActualState = true;
                    break;
                case STATE_TURNING_ON:
                    mInTransition = true;
                    mActualState = false;
                    break;
                case STATE_TURNING_OFF:
                    mInTransition = true;
                    mActualState = true;
                    break;
            }

            if (wasInTransition && !mInTransition) {
                if (mDeferredStateChangeRequestNeeded) {
                    Log.v(TAG, "processing deferred state change");
                    if (mActualState != null && mIntendedState != null &&
                        mIntendedState.equals(mActualState)) {
                        Log.v(TAG, "... but intended state matches, so no changes.");
                    } else if (mIntendedState != null) {
                        mInTransition = true;
                        requestStateChange(context, mIntendedState);
                    }
                    mDeferredStateChangeRequestNeeded = false;
                }
            }
        }


        /**
         * If we're in a transition mode, this returns true if we're
         * transitioning towards being enabled.
         */
        public final boolean isTurningOn() {
            return mIntendedState != null && mIntendedState;
        }

        /**
         * Returns simplified 3-state value from underlying 5-state.
         *
         * @param context
         * @return STATE_ENABLED, STATE_DISABLED, or STATE_INTERMEDIATE
         */
        public final int getTriState(Context context) {
            if (mInTransition) {
                // If we know we just got a toggle request recently
                // (which set mInTransition), don't even ask the
                // underlying interface for its state.  We know we're
                // changing.  This avoids blocking the UI thread
                // during UI refresh post-toggle if the underlying
                // service state accessor has coarse locking on its
                // state (to be fixed separately).
                return STATE_INTERMEDIATE;
            }
            switch (getActualState(context)) {
                case STATE_DISABLED:
                    return STATE_DISABLED;
                case STATE_ENABLED:
                    return STATE_ENABLED;
                default:
                    return STATE_INTERMEDIATE;
            }
        }

        /**
         * Gets underlying actual state.
         *
         * @param context
         * @return STATE_ENABLED, STATE_DISABLED, STATE_ENABLING, STATE_DISABLING,
         *         or or STATE_UNKNOWN.
         */
        public abstract int getActualState(Context context);

        /**
         * Actually make the desired change to the underlying radio
         * API.
         */
        protected abstract void requestStateChange(Context context, boolean desiredState);
    }

    /**
     * Subclass of StateTracker to get/set Wifi state.
     */
    private static final class WifiStateTracker extends StateTracker {
        public int getButtonId() { return R.id.img_wifi; }
        public int getIndicatorId() { return R.id.ind_wifi; }
        public int getButtonImageId(boolean on) {
            return on ? R.drawable.ic_appwidget_settings_wifi_on_holo
                    : R.drawable.ic_appwidget_settings_wifi_off_holo;
        }

        @Override
        public int getPosition() { return POS_LEFT; }

        @Override
        public int getActualState(Context context) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                return wifiStateToFiveState(wifiManager.getWifiState());
            }
            return STATE_UNKNOWN;
        }

        @Override
        protected void requestStateChange(Context context, final boolean desiredState) {
            final WifiManager wifiManager =
                    (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                Log.d(TAG, "No wifiManager.");
                return;
            }

            // Actually request the wifi change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    /**
                     * Disable tethering if enabling Wifi
                     */
                    int wifiApState = wifiManager.getWifiApState();
                    if (desiredState && ((wifiApState == WifiManager.WIFI_AP_STATE_ENABLING) ||
                                         (wifiApState == WifiManager.WIFI_AP_STATE_ENABLED))) {
                        wifiManager.setWifiApEnabled(null, false);
                    }

                    wifiManager.setWifiEnabled(desiredState);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                return;
            }
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            setCurrentState(context, wifiStateToFiveState(wifiState));
        }

        /**
         * Converts WifiManager's state values into our
         * Wifi/Bluetooth-common state values.
         */
        private static int wifiStateToFiveState(int wifiState) {
            switch (wifiState) {
                case WifiManager.WIFI_STATE_DISABLED:
                    return STATE_DISABLED;
                case WifiManager.WIFI_STATE_ENABLED:
                    return STATE_ENABLED;
                case WifiManager.WIFI_STATE_DISABLING:
                    return STATE_TURNING_OFF;
                case WifiManager.WIFI_STATE_ENABLING:
                    return STATE_TURNING_ON;
                default:
                    return STATE_UNKNOWN;
            }
        }
    }

    /**
     * Subclass of StateTracker to get/set Bluetooth state.
     */
    private static final class BluetoothStateTracker extends StateTracker {
        public int getButtonId() { return R.id.img_bluetooth; }
        public int getIndicatorId() { return R.id.ind_bluetooth; }
        public int getButtonImageId(boolean on) {
            return on ? R.drawable.ic_appwidget_settings_bluetooth_on_holo
                    : R.drawable.ic_appwidget_settings_bluetooth_off_holo;
        }

        @Override
        public int getActualState(Context context) {
            if (sLocalBluetoothAdapter == null) {
                LocalBluetoothManager manager = LocalBluetoothManager.getInstance(context);
                if (manager == null) {
                    return STATE_UNKNOWN;  // On emulator?
                }
                sLocalBluetoothAdapter = manager.getBluetoothAdapter();
            }
            return bluetoothStateToFiveState(sLocalBluetoothAdapter.getBluetoothState());
        }

        @Override
        protected void requestStateChange(Context context, final boolean desiredState) {
            if (sLocalBluetoothAdapter == null) {
                Log.d(TAG, "No LocalBluetoothManager");
                return;
            }
            // Actually request the Bluetooth change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    sLocalBluetoothAdapter.setBluetoothEnabled(desiredState);
                    return null;
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                return;
            }
            int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
            setCurrentState(context, bluetoothStateToFiveState(bluetoothState));
        }

        /**
         * Converts BluetoothAdapter's state values into our
         * Wifi/Bluetooth-common state values.
         */
        private static int bluetoothStateToFiveState(int bluetoothState) {
            switch (bluetoothState) {
                case BluetoothAdapter.STATE_OFF:
                    return STATE_DISABLED;
                case BluetoothAdapter.STATE_ON:
                    return STATE_ENABLED;
                case BluetoothAdapter.STATE_TURNING_ON:
                    return STATE_TURNING_ON;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    return STATE_TURNING_OFF;
                default:
                    return STATE_UNKNOWN;
            }
        }
    }

    /**
     * Subclass of StateTracker for GPS state.
     */
    private static final class GpsStateTracker extends StateTracker {
        public int getButtonId() { return R.id.img_gps; }
        public int getIndicatorId() { return R.id.ind_gps; }
        public int getButtonImageId(boolean on) {
            return on ? R.drawable.ic_appwidget_settings_gps_on_holo
                    : R.drawable.ic_appwidget_settings_gps_off_holo;
        }

        @Override
        public int getActualState(Context context) {
            ContentResolver resolver = context.getContentResolver();
            boolean on = Settings.Secure.isLocationProviderEnabled(
                resolver, LocationManager.GPS_PROVIDER);
            return on ? STATE_ENABLED : STATE_DISABLED;
        }

        @Override
        public void onActualStateChange(Context context, Intent unused) {
            // Note: the broadcast location providers changed intent
            // doesn't include an extras bundles saying what the new value is.
            setCurrentState(context, getActualState(context));
        }

        @Override
        public void requestStateChange(final Context context, final boolean desiredState) {
            final ContentResolver resolver = context.getContentResolver();
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... args) {
                    Settings.Secure.setLocationProviderEnabled(
                        resolver,
                        LocationManager.GPS_PROVIDER,
                        desiredState);
                    return desiredState;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    setCurrentState(
                        context,
                        result ? STATE_ENABLED : STATE_DISABLED);
                    updateWidget(context);
                }
            }.execute();
        }
    }

    /**
     * Subclass of StateTracker for sync state.
     */
    private static final class SyncStateTracker extends StateTracker {
        public int getButtonId() { return R.id.img_sync; }
        public int getIndicatorId() { return R.id.ind_sync; }
        public int getButtonImageId(boolean on) {
            return on ? R.drawable.ic_appwidget_settings_sync_on_holo
                    : R.drawable.ic_appwidget_settings_sync_off_holo;
        }

        @Override
        public int getActualState(Context context) {
            boolean on = ContentResolver.getMasterSyncAutomatically();
            return on ? STATE_ENABLED : STATE_DISABLED;
        }

        @Override
        public void onActualStateChange(Context context, Intent unused) {
            setCurrentState(context, getActualState(context));
        }

        @Override
        public void requestStateChange(final Context context, final boolean desiredState) {
            final ConnectivityManager connManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final boolean sync = ContentResolver.getMasterSyncAutomatically();

            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... args) {
                    // Turning sync on.
                    if (desiredState) {
                        if (!sync) {
                            ContentResolver.setMasterSyncAutomatically(true);
                        }
                        return true;
                    }

                    // Turning sync off
                    if (sync) {
                        ContentResolver.setMasterSyncAutomatically(false);
                    }
                    return false;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    setCurrentState(
                        context,
                        result ? STATE_ENABLED : STATE_DISABLED);
                    updateWidget(context);
                }
            }.execute();
        }
    }

    private static void checkObserver(Context context) {
        if (sSettingsObserver == null) {
            sSettingsObserver = new SettingsObserver(new Handler(),
                    context.getApplicationContext());
            sSettingsObserver.startObserving();
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
    	Log.d(TAG, "onUpdate()");
        // Update each requested appWidgetId
        RemoteViews view = buildUpdate(context);

        for (int i = 0; i < appWidgetIds.length; i++) {
            appWidgetManager.updateAppWidget(appWidgetIds[i], view);
        }
    }

    @Override
    public void onEnabled(Context context) {
    	Log.d(TAG, "onEnabled()");
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(
                new ComponentName("com.android.settings", ".widget.SettingsAppWidgetProvider"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        checkObserver(context);
    }

    @Override
    public void onDisabled(Context context) {
    	Log.d(TAG, "onDisabled()");
        Class clazz = com.android.settings.widget.SettingsAppWidgetProvider.class;
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(
                new ComponentName("com.android.settings", ".widget.SettingsAppWidgetProvider"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        if (sSettingsObserver != null) {
            sSettingsObserver.stopObserving();
            sSettingsObserver = null;
        }
    }

    /**
     * Load image for given widget and build {@link RemoteViews} for it.
     */
    static RemoteViews buildUpdate(Context context) {
    	Log.d(TAG, "buildUpdate()");
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget);
        if (FeatureOption.MTK_WLAN_SUPPORT) {
        views.setOnClickPendingIntent(R.id.btn_wifi, getLaunchPendingIntent(context,
                BUTTON_WIFI));
        }else {
            views.setViewVisibility(R.id.btn_wifi, View.GONE);
        }
        views.setOnClickPendingIntent(R.id.btn_brightness,
                getLaunchPendingIntent(context,
                        BUTTON_BRIGHTNESS));
        views.setOnClickPendingIntent(R.id.btn_sync,
                getLaunchPendingIntent(context,
                        BUTTON_SYNC));
        if (FeatureOption.MTK_GPS_SUPPORT) {
        views.setOnClickPendingIntent(R.id.btn_gps,
                getLaunchPendingIntent(context, BUTTON_GPS));
        }else {
            views.setViewVisibility(R.id.btn_gps, View.GONE);
        }
        if (FeatureOption.MTK_BT_SUPPORT || FeatureOption.RDA_BT_SUPPORT) {       
        views.setOnClickPendingIntent(R.id.btn_bluetooth,
                getLaunchPendingIntent(context,
                        BUTTON_BLUETOOTH));
        } else {
            views.setViewVisibility(R.id.btn_bluetooth, View.GONE);
        }
        updateButtons(views, context);
        return views;
    }

    /**
     * Updates the widget when something changes, or when a button is pushed.
     *
     * @param context
     */
    public static void updateWidget(Context context) {
        Log.d(TAG, "updateWidget()");
        RemoteViews views = buildUpdate(context);
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        gm.updateAppWidget(THIS_APPWIDGET, views);
        checkObserver(context);
    }

    /**
     * Updates the buttons based on the underlying states of wifi, etc.
     *
     * @param views   The RemoteViews to update.
     * @param context
     */
    private static void updateButtons(RemoteViews views, Context context) {
    	Log.d(TAG, "updateButtons()");
        if (FeatureOption.MTK_WLAN_SUPPORT) {
        sWifiState.setImageViewResources(context, views);
        }

        if (FeatureOption.MTK_BT_SUPPORT || FeatureOption.RDA_BT_SUPPORT) {
        sBluetoothState.setImageViewResources(context, views);
        }

        if (FeatureOption.MTK_GPS_SUPPORT) {
        sGpsState.setImageViewResources(context, views);
        }

        sSyncState.setImageViewResources(context, views);

        if (getBrightnessMode(context)) {
        	Log.d(TAG, "updateButtons , getBrightnessMode(context) == true, auto");
            views.setImageViewResource(R.id.img_brightness,
                    R.drawable.ic_appwidget_settings_brightness_auto_holo);
            views.setImageViewResource(R.id.ind_brightness,
                    R.drawable.appwidget_settings_ind_on_r_holo);
        } else {
            final int brightness = getBrightness(context);
        	Log.d(TAG, "updateButtons, getBrightnessMode(context) == false ,manual , brightness = "+brightness);
            // Set the icon
            if (brightness > FULL_BRIGHTNESS_THRESHOLD) {
                views.setImageViewResource(R.id.img_brightness,
                        R.drawable.ic_appwidget_settings_brightness_full_holo);
            } else if (brightness > HALF_BRIGHTNESS_THRESHOLD) {
                views.setImageViewResource(R.id.img_brightness,
                        R.drawable.ic_appwidget_settings_brightness_half_holo);
            } else {
                views.setImageViewResource(R.id.img_brightness,
                        R.drawable.ic_appwidget_settings_brightness_off_holo);
            }
            // Set the ON state
            if (brightness > HALF_BRIGHTNESS_THRESHOLD) {
                views.setImageViewResource(R.id.ind_brightness,
                        R.drawable.appwidget_settings_ind_on_r_holo);
            } else {
                views.setImageViewResource(R.id.ind_brightness,
                        R.drawable.appwidget_settings_ind_off_r_holo);
            }
        }
    }

    /**
     * Creates PendingIntent to notify the widget of a button click.
     *
     * @param context
     * @return
     */
    private static PendingIntent getLaunchPendingIntent(Context context,
            int buttonId) {
        Intent launchIntent = new Intent();
        launchIntent.setClass(context, SettingsAppWidgetProvider.class);
        launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        launchIntent.setData(Uri.parse("custom:" + buttonId));
        PendingIntent pi = PendingIntent.getBroadcast(context, 0 /* no requestCode */,
                launchIntent, 0 /* no flags */);
    	Log.d(TAG, "PendingIntent , buttonId = "+buttonId+" pi = "+pi);
        return pi;
    }

    /**
     * Receives and processes a button pressed intent or state change.
     *
     * @param context
     * @param intent  Indicates the pressed button.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        Log.d(TAG, "onReceive , action = "+action);
        Log.d(TAG, "onReceive , intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)= "+ intent.hasCategory(Intent.CATEGORY_ALTERNATIVE));
        if ((FeatureOption.MTK_WLAN_SUPPORT)
              && WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            sWifiState.onActualStateChange(context, intent);
        } else if ((FeatureOption.MTK_BT_SUPPORT || FeatureOption.RDA_BT_SUPPORT)
              && BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
            sBluetoothState.onActualStateChange(context, intent);
        } else if (FeatureOption.MTK_GPS_SUPPORT && LocationManager.PROVIDERS_CHANGED_ACTION.equals(action)) {
            sGpsState.onActualStateChange(context, intent);
        } else if (SyncStorageEngine.SYNC_CONNECTION_SETTING_CHANGED_INTENT.getAction()
                .equals(action)) {
            sSyncState.onActualStateChange(context, intent);
        } else if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
            Uri data = intent.getData();
            int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
            if ((FeatureOption.MTK_WLAN_SUPPORT) && (buttonId == BUTTON_WIFI)) {
                // MTK_OP01_PROTECT_START
                // disable wifi button in the CMCC's airplane mode
                if(!((isAirplaneModeOn(context)) && (Utils.isCmccLoad())))
                // MTK_OP01_PROTECT_END
                {
                   sWifiState.toggleState(context);
                }
            }           
            else if (buttonId == BUTTON_BRIGHTNESS) {
                toggleBrightness(context);
            } else if (buttonId == BUTTON_SYNC) {
                sSyncState.toggleState(context);
            } else if (FeatureOption.MTK_GPS_SUPPORT && (buttonId == BUTTON_GPS)) {
                sGpsState.toggleState(context);
            } else if ((FeatureOption.MTK_BT_SUPPORT || FeatureOption.RDA_BT_SUPPORT) && (buttonId == BUTTON_BLUETOOTH)) {
                sBluetoothState.toggleState(context);
            }
        } else {
            // Don't fall-through to updating the widget.  The Intent
            // was something unrelated or that our super class took
            // care of.
            return;
        }

        // State changes fall through
        updateWidget(context);
    }

    /**
     * Gets brightness level.
     *
     * @param context
     * @return brightness level between 0 and 255.
     */
    private static int getBrightness(Context context) {
        try {
            IPowerManager power = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));
            if (power != null) {
                int brightness = Settings.System.getInt(context.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS);
                Log.d(TAG, "getBrightness() , brightness =" + brightness);
                return brightness;
            }
        } catch (Exception e) {
        }
        return 0;
    }

    /**
     * Gets state of brightness mode.
     *
     * @param context
     * @return true if auto brightness is on.
     */
    private static boolean getBrightnessMode(Context context) {
        try {
            IPowerManager power = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));
            if (power != null) {
                int brightnessMode = Settings.System.getInt(context.getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE);
                Log.d(TAG, "getBrightnessMode() , brightnessMode =" + brightnessMode);
                return brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
            }
        } catch (Exception e) {
            Log.d(TAG, "getBrightnessMode: " + e);
        }
        return false;
    }

    /**
     * Increases or decreases the brightness.
     *
     * @param context
     */
    private void toggleBrightness(Context context) {
        try {
            IPowerManager power = IPowerManager.Stub.asInterface(
                    ServiceManager.getService("power"));
            if (power != null) {
                ContentResolver cr = context.getContentResolver();
                int brightness = Settings.System.getInt(cr,
                        Settings.System.SCREEN_BRIGHTNESS);
                int brightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
                //Only get brightness setting if available
//                if (context.getResources().getBoolean(
//                        com.android.internal.R.bool.config_automatic_brightness_available)) {
                SensorManager mgr = (SensorManager)context.getSystemService(
                        Context.SENSOR_SERVICE);
                if (mgr != null && mgr.getDefaultSensor(Sensor.TYPE_LIGHT) != null){
                    brightnessMode = Settings.System.getInt(cr,
                            Settings.System.SCREEN_BRIGHTNESS_MODE);
                }
                Log.d(TAG, "begin ------toggleBrightness() ,brightnessMode ="+brightnessMode+" brightness = "+brightness);
                // Rotate AUTO -> MINIMUM -> DEFAULT -> MAXIMUM
                // Technically, not a toggle...
                if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                	Log.d(TAG, "toggleBrightness(),AUTO -> MINIMUM");
                    brightness = MINIMUM_BACKLIGHT;
                    brightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
                } else if (brightness < DEFAULT_BACKLIGHT) {
                	Log.d(TAG, "toggleBrightness(),MINIMUM -> DEFAULT");
                    brightness = DEFAULT_BACKLIGHT;
                } else if (brightness < MAXIMUM_BACKLIGHT) {
                	Log.d(TAG, "toggleBrightness(),DEFAULT -> MAXIMUM");
                    brightness = MAXIMUM_BACKLIGHT;
                } else {
                	Log.d(TAG, "toggleBrightness(),MAXIMUM -> AUTO");
                    brightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
                    brightness = MINIMUM_BACKLIGHT;
                }

                if (mgr != null && mgr.getDefaultSensor(Sensor.TYPE_LIGHT) != null){
                    // Set screen brightness mode (automatic or manual)
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            brightnessMode);
                } else {
                    // Make sure we set the brightness if automatic mode isn't available
                    brightnessMode = Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;
                }
                if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) {
                    Log.i(TAG, "-----------toggleBrightness()----setBacklightBrightness()----brightness: "+brightness);
                    power.setBacklightBrightness(brightness);
                    Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, brightness);
                }
                Log.d(TAG, "end ------toggleBrightness(),brightnessMode ="+Settings.System.getInt(cr,
                        Settings.System.SCREEN_BRIGHTNESS_MODE)+" brightness = "+Settings.System.getInt(cr,
                                Settings.System.SCREEN_BRIGHTNESS));
            }
        } catch (RemoteException e) {
            Log.d(TAG, "toggleBrightness1: " + e);
        } catch (Settings.SettingNotFoundException e) {
            Log.d(TAG, "toggleBrightness2: " + e);
        }
    }

    /** Observer to watch for changes to the BRIGHTNESS setting */
    private static class SettingsObserver extends ContentObserver {

        private Context mContext;

        SettingsObserver(Handler handler, Context context) {
            super(handler);
            mContext = context;
        }

        void startObserving() {
            ContentResolver resolver = mContext.getContentResolver();
            // Listen to brightness and brightness mode
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.SCREEN_BRIGHTNESS), false, this);
            resolver.registerContentObserver(Settings.System
                    .getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), false, this);
        }

        void stopObserving() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }

        @Override
        public void onChange(boolean selfChange) {
        	Log.d(TAG, "SettingsObserver , onChange()");
            updateWidget(mContext);
        }
    }
    public static boolean isAirplaneModeOn(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.AIRPLANE_MODE_ON, 0) != 0;
    }
}
