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

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnCancelListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.text.format.DateFormat;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.text.format.Time;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateTimeSettings extends SettingsPreferenceFragment
        implements OnSharedPreferenceChangeListener,
                TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener ,
                DialogInterface.OnClickListener, OnCancelListener{
	private static final String TAG = "DateTimeSettings";
    private static final String HOURS_12 = "12";
    private static final String HOURS_24 = "24";

    // Used for showing the current date format, which looks like "12/31/2010", "2010/12/13", etc.
    // The date value is dummy (independent of actual date).
    private Calendar mDummyDate;

    private static final String KEY_DATE_FORMAT = "date_format";
    private static final String KEY_AUTO_TIME = "auto_time_list";
    private static final String KEY_AUTO_TIME_ZONE = "auto_zone";

    private static final int DIALOG_DATEPICKER = 0;
    private static final int DIALOG_TIMEPICKER = 1;
    private static final int DIALOG_GPS_CONFIRM = 2;

    private static final int AUTO_TIME_NETWORK_INDEX = 0;
    private static final int AUTO_TIME_GPS_INDEX = 1;
    private static final int AUTO_TIME_OFF_INDEX = 2;

    // have we been launched from the setup wizard?
    protected static final String EXTRA_IS_FIRST_RUN = "firstRun";

    private ListPreference mAutoTimePref;
    private Preference mTimePref;
    private Preference mTime24Pref;
    private CheckBoxPreference mAutoTimeZonePref;
    private Preference mTimeZone;
    private Preference mDatePref;
    private ListPreference mDateFormat;
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.date_time_prefs);

        initUI();
    }

    private void initUI() {
        boolean autoTimeEnabled = getAutoState(Settings.System.AUTO_TIME);
        boolean autoTimeZoneEnabled = getAutoState(Settings.System.AUTO_TIME_ZONE);
        boolean autoTimeGpsEnabled = getAutoState(Settings.System.AUTO_TIME_GPS);
        Intent intent = getActivity().getIntent();
        boolean isFirstRun = intent.getBooleanExtra(EXTRA_IS_FIRST_RUN, false);

        mDummyDate = Calendar.getInstance();
        mDummyDate.set(mDummyDate.get(Calendar.YEAR), 11, 31, 13, 0, 0);

        mAutoTimePref = (ListPreference) findPreference(KEY_AUTO_TIME);
        if(autoTimeEnabled){
            mAutoTimePref.setValueIndex(AUTO_TIME_NETWORK_INDEX);
        }else if(autoTimeGpsEnabled){
            mAutoTimePref.setValueIndex(AUTO_TIME_GPS_INDEX);
        }else{
            mAutoTimePref.setValueIndex(AUTO_TIME_OFF_INDEX);
        }
        mAutoTimePref.setSummary(mAutoTimePref.getValue());
        mAutoTimeZonePref = (CheckBoxPreference) findPreference(KEY_AUTO_TIME_ZONE);
        // Override auto-timezone if it's a wifi-only device or if we're still in setup wizard.
        // TODO: Remove the wifiOnly test when auto-timezone is implemented based on wifi-location.
        if (Utils.isWifiOnly(getActivity()) || isFirstRun) {
            getPreferenceScreen().removePreference(mAutoTimeZonePref);
            autoTimeZoneEnabled = false;
        }
        mAutoTimeZonePref.setChecked(autoTimeZoneEnabled);

        mTimePref = findPreference("time");
        mTime24Pref = findPreference("24 hour");
        mTimeZone = findPreference("timezone");
        mDatePref = findPreference("date");
        mDateFormat = (ListPreference) findPreference(KEY_DATE_FORMAT);
        if (isFirstRun) {
            getPreferenceScreen().removePreference(mTime24Pref);
            getPreferenceScreen().removePreference(mDateFormat);
        }

        String [] dateFormats = getResources().getStringArray(R.array.date_format_values);
        String [] formattedDates = new String[dateFormats.length];
        String currentFormat = getDateFormat();
        // Initialize if DATE_FORMAT is not set in the system settings
        // This can happen after a factory reset (or data wipe)
        if (currentFormat == null) {
            currentFormat = "";
        }
        for (int i = 0; i < formattedDates.length; i++) {
            String formatted =
                    DateFormat.getDateFormatForSetting(getActivity(), dateFormats[i])
                    .format(mDummyDate.getTime());

            if (dateFormats[i].length() == 0) {
                formattedDates[i] = getResources().
                    getString(R.string.normal_date_format, formatted);
            } else {
                formattedDates[i] = formatted;
            }
        }

        mDateFormat.setEntries(formattedDates);
        mDateFormat.setEntryValues(R.array.date_format_values);
        mDateFormat.setValue(currentFormat);

        boolean autoEnabled = autoTimeEnabled || autoTimeGpsEnabled;

        mTimePref.setEnabled(!autoEnabled);
        mDatePref.setEnabled(!autoEnabled);
        mTimeZone.setEnabled(!autoTimeZoneEnabled);
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);

        ((CheckBoxPreference)mTime24Pref).setChecked(is24Hour());

        // Register for time ticks and other reasons for time change
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getActivity().registerReceiver(mIntentReceiver, filter, null, null);

        updateTimeAndDateDisplay(getActivity());
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mIntentReceiver);
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void updateTimeAndDateDisplay(Context context) {
        java.text.DateFormat shortDateFormat = DateFormat.getDateFormat(context);
        final Calendar now = Calendar.getInstance();
        mDummyDate.setTimeZone(now.getTimeZone());
        mDummyDate.set(now.get(Calendar.YEAR), 11, 31, 13, 0, 0);
        Date dummyDate = mDummyDate.getTime();
        mTimePref.setSummary(DateFormat.getTimeFormat(getActivity()).format(now.getTime()));
        mTimeZone.setSummary(getTimeZoneText(now.getTimeZone()));
        mDatePref.setSummary(shortDateFormat.format(now.getTime()));
        mDateFormat.setSummary(shortDateFormat.format(dummyDate));
    }
    private void updateDateFormatEntries(){
    	String [] dateFormats = getResources().getStringArray(R.array.date_format_values);
        String [] formattedDates = new String[dateFormats.length];
        for (int i = 0; i < formattedDates.length; i++) {
            String formatted =
                    DateFormat.getDateFormatForSetting(getActivity(), dateFormats[i])
                    .format(mDummyDate.getTime());
            if (dateFormats[i].length() == 0) {
                formattedDates[i] = getResources().
                    getString(R.string.normal_date_format, formatted);
            } else {
                formattedDates[i] = formatted;
            }
        }
        mDateFormat.setEntries(formattedDates);
    }	
    @Override
    public void onDateSet(DatePicker view, int year, int month, int day) {
        setDate(year, month, day);
        final Activity activity = getActivity();
        if (activity != null) {
            updateTimeAndDateDisplay(activity);
            updateDateFormatEntries();
        }
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        setTime(hourOfDay, minute);
        final Activity activity = getActivity();
        if (activity != null) {
            updateTimeAndDateDisplay(activity);
        }

        // We don't need to call timeUpdated() here because the TIME_CHANGED
        // broadcast is sent by the AlarmManager as a side effect of setting the
        // SystemClock time.
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (key.equals(KEY_DATE_FORMAT)) {
            String format = preferences.getString(key,
                    getResources().getString(R.string.default_date_format));
            Settings.System.putString(getContentResolver(),
                    Settings.System.DATE_FORMAT, format);
            updateTimeAndDateDisplay(getActivity());
        } else if (key.equals(KEY_AUTO_TIME)) {
            String value = mAutoTimePref.getValue();
            int index = mAutoTimePref.findIndexOfValue(value);
            mAutoTimePref.setSummary(value);
            boolean autoEnabled = true;

            if(index == AUTO_TIME_NETWORK_INDEX){
                Settings.System.putInt(getContentResolver(), Settings.System.AUTO_TIME,1);
                Settings.System.putInt(getContentResolver(), Settings.System.AUTO_TIME_GPS,0);
            }else if (index == AUTO_TIME_GPS_INDEX){
            	showDialog(DIALOG_GPS_CONFIRM);
            	setOnCancelListener(this);
            }else {
                Settings.System.putInt(getContentResolver(), Settings.System.AUTO_TIME,0);
                Settings.System.putInt(getContentResolver(), Settings.System.AUTO_TIME_GPS,0);
                autoEnabled = false;
            }
            mTimePref.setEnabled(!autoEnabled);
            mDatePref.setEnabled(!autoEnabled);
        } else if (key.equals(KEY_AUTO_TIME_ZONE)) {
            boolean autoZoneEnabled = preferences.getBoolean(key, true);
            Settings.System.putInt(
                    getContentResolver(), Settings.System.AUTO_TIME_ZONE, autoZoneEnabled ? 1 : 0);
            mTimeZone.setEnabled(!autoZoneEnabled);
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        Dialog d;

        switch (id) {
        case DIALOG_DATEPICKER: {
            final Calendar calendar = Calendar.getInstance();
            d = new DatePickerDialog(
                getActivity(),
                this,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
	    setDateRange((DatePickerDialog)d);
            break;
        }
        case DIALOG_TIMEPICKER: {
            final Calendar calendar = Calendar.getInstance();
            d = new TimePickerDialog(
                    getActivity(),
                    this,
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(getActivity()));
            break;
        }
        case DIALOG_GPS_CONFIRM:{
        	int msg;
        	if (Settings.Secure.isLocationProviderEnabled(
	        		getContentResolver(), LocationManager.GPS_PROVIDER)) {
        		msg=R.string.gps_time_sync_attention_gps_on;
        	} else {
        		msg=R.string.gps_time_sync_attention_gps_off;
        	}
        	d = new AlertDialog.Builder(getActivity()).setMessage(
                    getActivity().getResources().getString(msg))
                    .setTitle(R.string.proxy_error)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, this)
                    .setNegativeButton(android.R.string.no, this)
                    .create();
        	break;
        }
        default:
            d = null;
            break;
        }

        return d;
    }
    private void setDateRange(DatePickerDialog dialog) {
	if (dialog != null) {
	    Time minTime=new Time();
	    Time maxTime=new Time();
	    minTime.set(0, 0, 0, 1, 0, 1970);//1970/1/1
            maxTime.set(59,59,23,31,11,2037);//2037/12/31
            long maxDate=maxTime.toMillis(false);
            maxDate=maxDate+999;//in millsec
            long minDate=minTime.toMillis(false);
            dialog.getDatePicker().setMinDate(minDate);
            dialog.getDatePicker().setMaxDate(maxDate);
	}
    }
    /*
    @Override
    public void onPrepareDialog(int id, Dialog d) {
        switch (id) {
        case DIALOG_DATEPICKER: {
            DatePickerDialog datePicker = (DatePickerDialog)d;
            final Calendar calendar = Calendar.getInstance();
            datePicker.updateDate(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            break;
        }
        case DIALOG_TIMEPICKER: {
            TimePickerDialog timePicker = (TimePickerDialog)d;
            final Calendar calendar = Calendar.getInstance();
            timePicker.updateTime(
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE));
            break;
        }
        default:
            break;
        }
    }
    */
    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mDatePref) {
            showDialog(DIALOG_DATEPICKER);
        } else if (preference == mTimePref) {
            // The 24-hour mode may have changed, so recreate the dialog
            removeDialog(DIALOG_TIMEPICKER);
            showDialog(DIALOG_TIMEPICKER);
        } else if (preference == mTime24Pref) {
            set24Hour(((CheckBoxPreference)mTime24Pref).isChecked());
            updateTimeAndDateDisplay(getActivity());
            timeUpdated();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
            Intent data) {
        updateTimeAndDateDisplay(getActivity());
    }

    private void timeUpdated() {
        Intent timeChanged = new Intent(Intent.ACTION_TIME_CHANGED);
        getActivity().sendBroadcast(timeChanged);
    }

    /*  Get & Set values from the system settings  */

    private boolean is24Hour() {
        return DateFormat.is24HourFormat(getActivity());
    }

    private void set24Hour(boolean is24Hour) {
        Settings.System.putString(getContentResolver(),
                Settings.System.TIME_12_24,
                is24Hour? HOURS_24 : HOURS_12);
    }

    private String getDateFormat() {
        return Settings.System.getString(getContentResolver(),
                Settings.System.DATE_FORMAT);
    }

    private boolean getAutoState(String name) {
        try {
            return Settings.System.getInt(getContentResolver(), name) > 0;
        } catch (SettingNotFoundException snfe) {
            return false;
        }
    }

    /*  Helper routines to format timezone */

    /* package */ static void setDate(int year, int month, int day) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        long when = c.getTimeInMillis();

        if (when / 1000 < Integer.MAX_VALUE) {
            SystemClock.setCurrentTimeMillis(when);
        }
    }

    /* package */ static void setTime(int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long when = c.getTimeInMillis();

        if (when / 1000 < Integer.MAX_VALUE) {
            SystemClock.setCurrentTimeMillis(when);
        }
    }

    /* package */ static String getTimeZoneText(TimeZone tz) {
        boolean daylight = tz.inDaylightTime(new Date());
        StringBuilder sb = new StringBuilder();

        sb.append(formatOffset(tz.getRawOffset() +
                               (daylight ? tz.getDSTSavings() : 0))).
            append(", ").
            append(tz.getDisplayName(daylight, TimeZone.LONG));

        return sb.toString();
    }

    private static char[] formatOffset(int off) {
        off = off / 1000 / 60;

        char[] buf = new char[9];
        buf[0] = 'G';
        buf[1] = 'M';
        buf[2] = 'T';

        if (off < 0) {
            buf[3] = '-';
            off = -off;
        } else {
            buf[3] = '+';
        }

        int hours = off / 60;
        int minutes = off % 60;

        buf[4] = (char) ('0' + hours / 10);
        buf[5] = (char) ('0' + hours % 10);

        buf[6] = ':';

        buf[7] = (char) ('0' + minutes / 10);
        buf[8] = (char) ('0' + minutes % 10);

        return buf;
    }

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Activity activity = getActivity();
            if (activity != null) {
                updateTimeAndDateDisplay(activity);
            }
        }
    };

	@Override
	public void onClick(DialogInterface dialog, int which) {
		 if (which == DialogInterface.BUTTON_POSITIVE) {
			 Log.d(TAG, "Enable GPS time sync");
			 boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
		        		getContentResolver(), LocationManager.GPS_PROVIDER);
			 if (false == gpsEnabled) {
				 Settings.Secure.setLocationProviderEnabled(getContentResolver(),
		                    LocationManager.GPS_PROVIDER, true);
			 }
			 Settings.System.putInt(getContentResolver(), Settings.System.AUTO_TIME,0);
			 Settings.System.putInt(getContentResolver(), Settings.System.AUTO_TIME_GPS,1);
	        }
		 else if (which == DialogInterface.BUTTON_NEGATIVE) {
			 	Log.d(TAG, "DialogInterface.BUTTON_NEGATIVE");
				reSetAutoTimePref();
		 }
	}
	private void reSetAutoTimePref(){
		Log.d(TAG, "reset AutoTimePref as cancel the selection");
		boolean autoTimeEnabled = getAutoState(Settings.System.AUTO_TIME);
    	boolean autoTimeGpsEnabled = getAutoState(Settings.System.AUTO_TIME_GPS);
    	if(autoTimeEnabled){
            mAutoTimePref.setValueIndex(AUTO_TIME_NETWORK_INDEX);
        }else if(autoTimeGpsEnabled){
            mAutoTimePref.setValueIndex(AUTO_TIME_GPS_INDEX);
        }else{
            mAutoTimePref.setValueIndex(AUTO_TIME_OFF_INDEX);
        }
        mAutoTimePref.setSummary(mAutoTimePref.getValue()); 
	}

	@Override
	public void onCancel(DialogInterface arg0) {
		Log.d(TAG, "onCancel Dialog");
		reSetAutoTimePref();	
	}
}
