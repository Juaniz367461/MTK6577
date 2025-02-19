/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein
 * is confidential and proprietary to MediaTek Inc. and/or its licensors.
 * Without the prior written permission of MediaTek inc. and/or its licensors,
 * any reproduction, modification, use or disclosure of MediaTek Software,
 * and information contained herein, in whole or in part, shall be strictly prohibited.
 */
/* MediaTek Inc. (C) 2010. All rights reserved.
 *
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER ON
 * AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
 * NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
 * SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
 * SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES TO LOOK ONLY TO SUCH
 * THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. RECEIVER EXPRESSLY ACKNOWLEDGES
 * THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES
 * CONTAINED IN MEDIATEK SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK
 * SOFTWARE RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND
 * CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
 * AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
 * OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY RECEIVER TO
 * MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek Software")
 * have been modified by MediaTek Inc. All revisions are subject to any receiver's
 * applicable license agreements with MediaTek Inc.
 */

package com.mediatek.engineermode.power;

import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import com.mediatek.engineermode.ChipSupport;
import com.mediatek.engineermode.Elog;
import com.mediatek.engineermode.R;
import com.mediatek.engineermode.ShellExe;

public class ChargeBattery extends Activity {

    private TextView mInfo = null;
    private String TAG = "EM_BATTERY_CHARGE";
    private static final int EVENT_UPDATE = 1;
    private String cmdString = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.battery_charge);

        mInfo = (TextView) findViewById(R.id.battery_charge_info_text);

        if (ChipSupport.GetChip() == ChipSupport.MTK_6573_SUPPORT) {
            cmdString = "cat /sys/devices/platform/mt6573-battery/";
        } else if (ChipSupport.GetChip() == ChipSupport.MTK_6575_SUPPORT) {
            cmdString = "cat /sys/devices/platform/mt6575-battery/";
        } else if (ChipSupport.GetChip() == ChipSupport.MTK_6577_SUPPORT) {
            // 6577 branch
            cmdString = "cat /sys/devices/platform/mt6577-battery/";
        } else {
            cmdString = "";
        }
    }

    private String getInfo(String cmd) {
        String result = null;
        try {
            String[] cmdx = { "/system/bin/sh", "-c", cmd }; // file must
            // exist// or
            // wait()
            // return2
            int ret = ShellExe.execCommand(cmdx);
            if (0 == ret) {
                result = ShellExe.getOutput();
            } else {
                // result = "ERROR";
                result = ShellExe.getOutput();
            }

        } catch (IOException e) {
            Elog.i(TAG, e.toString());
            result = "ERR.JE";
        }
        return result;
    }

    private int mUpdateInterval = 1500; // 1.5 sec
    public Handler mUpdateHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case EVENT_UPDATE:
                Bundle b = msg.getData();
                mInfo.setText(b.getString("INFO"));
                break;
            }
        }
    };

    private String[][] files = {

    { "ADC_Charger_Voltage", "mV" }, { "Power_On_Voltage", "mV" },
            { "Power_Off_Voltage", "mV" }, { "Charger_TopOff_Value", "mV" },
            { "FG_Battery_CurrentConsumption", "mA" }, { "SEP", "" },
            { "ADC_Channel_0_Slope", "" }, { "ADC_Channel_1_Slope", "" },
            { "ADC_Channel_2_Slope", "" }, { "ADC_Channel_3_Slope", "" },
            { "ADC_Channel_4_Slope", "" }, { "ADC_Channel_5_Slope", "" },
            { "ADC_Channel_6_Slope", "" }, { "ADC_Channel_7_Slope", "" },
            { "ADC_Channel_8_Slope", "" }, { "ADC_Channel_9_Slope", "" },
            { "ADC_Channel_10_Slope", "" }, { "ADC_Channel_11_Slope", "" },
            { "ADC_Channel_12_Slope", "" }, { "ADC_Channel_13_Slope", "" },
            { "SEP", "" }, { "ADC_Channel_0_Offset", "" },
            { "ADC_Channel_1_Offset", "" }, { "ADC_Channel_2_Offset", "" },
            { "ADC_Channel_3_Offset", "" }, { "ADC_Channel_4_Offset", "" },
            { "ADC_Channel_5_Offset", "" }, { "ADC_Channel_6_Offset", "" },
            { "ADC_Channel_7_Offset", "" }, { "ADC_Channel_8_Offset", "" },
            { "ADC_Channel_9_Offset", "" }, { "ADC_Channel_10_Offset", "" },
            { "ADC_Channel_11_Offset", "" }, { "ADC_Channel_12_Offset", "" },
            { "ADC_Channel_13_Offset", "" } };

    private boolean mRun = false;

    class runThread extends Thread {

        public void run() {
            while (mRun) {
                StringBuilder text = new StringBuilder("");
                String cmd = "";
                for (int i = 0; i < files.length; i++) {
                    if (files[i][0].equalsIgnoreCase("SEP")) {
                        text.append("- - - - - - - - -\n");
                        continue;
                    }
                    cmd = cmdString + files[i][0];
                    if (files[i][1].equalsIgnoreCase("mA")) {
                        double f = 0.0f;
                        try {
                            f = Float.valueOf(getInfo(cmd)) / 10.0f;
                        } catch (NumberFormatException e) {
                            Elog.e("EM-PMU", "read file error " + files[i][0]);
                        }
                        text.append(String.format("%1$-28s:[ %2$-6s ]%3$s\n",
                                files[i][0], f, files[i][1]));
                    } else {
                        text.append(String.format("%1$-28s: [ %2$-6s ]%3$s\n",
                                files[i][0], getInfo(cmd), files[i][1]));
                    }
                }

                Bundle b = new Bundle();
                b.putString("INFO", text.toString());

                Message msg = new Message();
                msg.what = EVENT_UPDATE;
                msg.setData(b);

                mUpdateHandler.sendMessage(msg);
                try {
                    sleep(mUpdateInterval);
                } catch (InterruptedException e) {

                }
            }
        }
    }

    @Override
    public void onPause() {

        super.onPause();
        mRun = false;
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        mRun = true;
        new runThread().start();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

}