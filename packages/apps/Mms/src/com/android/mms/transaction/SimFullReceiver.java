/*
 * Copyright (C) 2007-2008 Esmertec AG.
 * Copyright (C) 2007-2008 The Android Open Source Project
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

package com.android.mms.transaction;

import com.android.mms.R;
import com.android.mms.ui.ManageSimMessages;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.provider.Telephony;
import com.mediatek.featureoption.FeatureOption;
import com.android.internal.telephony.Phone;
import android.provider.Telephony.SIMInfo;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.TextAppearanceSpan;
import android.util.Log;

import com.mediatek.telephony.TelephonyManagerEx;
import com.android.mms.ui.MessageUtils;
/**
 * Receive Intent.SIM_FULL_ACTION.  Handle notification that SIM is full.
 */
public class SimFullReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Settings.Secure.getInt(context.getContentResolver(),
            Settings.Secure.DEVICE_PROVISIONED, 0) == 1 &&
            Telephony.Sms.Intents.SIM_FULL_ACTION.equals(intent.getAction())) {
            
            NotificationManager nm = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
            Intent viewSimIntent;
            int slotId ;
            viewSimIntent = new Intent(context, ManageSimMessages.class);
            if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
            	slotId = intent.getIntExtra(Phone.GEMINI_SIM_ID_KEY, Phone.GEMINI_SIM_1);
            	Log.d("SimFullReceiver", "slotId is " + slotId);
                viewSimIntent.putExtra("SlotId", slotId);
            }   
            
            //viewSimIntent.setAction(Intent.ACTION_VIEW);
            viewSimIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, 0, viewSimIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Notification notification = new Notification();
            notification.icon = R.drawable.stat_sys_no_sim;
            // add for gemini
            if (FeatureOption.MTK_GEMINI_SUPPORT == true) {
                SIMInfo simInfo = SIMInfo.getSIMInfoBySlot(context, slotId);
                if (simInfo != null) {
                    notification.simId = simInfo.mSimId;
            	    notification.simInfoType = 1;
                }
            }
            notification.tickerText = context.getString(R.string.sim_full_title);
            
            notification.defaults = Notification.DEFAULT_ALL;

            notification.setLatestEventInfo(
                    context, notification.tickerText, 
                    context.getString(R.string.sim_full_body),
                    pendingIntent);
            nm.notify(ManageSimMessages.SIM_FULL_NOTIFICATION_ID, notification);
       }
    }

}
