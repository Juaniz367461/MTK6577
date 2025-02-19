/*
 * Copyright (C) 2006 The Android Open Source Project
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

package com.android.server.am;

import static android.view.WindowManager.LayoutParams.FLAG_SYSTEM_ERROR;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.util.Slog;
import android.os.ServiceManager;
import com.mediatek.lowstorage.LowStorageHandle;
import com.android.server.DeviceStorageMonitorService;
class AppErrorDialog extends BaseErrorDialog {
    private final static String TAG = "AppErrorDialog";

    private final AppErrorResult mResult;
    private final ProcessRecord mProc;

    // Event 'what' codes
    static final int FORCE_QUIT = 0;
    static final int FORCE_QUIT_AND_REPORT = 1;

    // 5-minute timeout, then we automatically dismiss the crash dialog
    static final long DISMISS_TIMEOUT = 1000 * 60 * 5;
    private boolean mTargetProcess = false; 
    /*
    public AppErrorDialog(Context context, AppErrorResult result, ProcessRecord app) {
        super(context);
        
        Resources res = context.getResources();
        
        mProc = app;
        mResult = result;
        CharSequence name;
        if ((app.pkgList.size() == 1) &&
                (name=context.getPackageManager().getApplicationLabel(app.info)) != null) {
            setMessage(res.getString(
                    com.android.internal.R.string.aerr_application,
                    name.toString(), app.info.processName));
        } else {
            name = app.processName;
            setMessage(res.getString(
                    com.android.internal.R.string.aerr_process,
                    name.toString()));
        }

        setCancelable(false);

        setButton(DialogInterface.BUTTON_POSITIVE,
                res.getText(com.android.internal.R.string.force_close),
                mHandler.obtainMessage(FORCE_QUIT));

        if (app.errorReportReceiver != null) {
            setButton(DialogInterface.BUTTON_NEGATIVE,
                    res.getText(com.android.internal.R.string.report),
                    mHandler.obtainMessage(FORCE_QUIT_AND_REPORT));
        }

        setTitle(res.getText(com.android.internal.R.string.aerr_title));
        getWindow().addFlags(FLAG_SYSTEM_ERROR);
        getWindow().setTitle("Application Error: " + app.info.processName);

        // After the timeout, pretend the user clicked the quit button
        mHandler.sendMessageDelayed(
                mHandler.obtainMessage(FORCE_QUIT),
                DISMISS_TIMEOUT);
        }
    */

    // [Note] rewrite Google - AppErrorDialog method (for low memory case)
    public AppErrorDialog(Context context, AppErrorResult result, ProcessRecord app) {
        super(context);
        
        Resources res = context.getResources();
        
        mProc = app;
        mResult = result;
        CharSequence name;
        ///M:For low storage, show the difference dialog,when APP&Peocess crash @{
        CharSequence message;
        DeviceStorageMonitorService dsm = (DeviceStorageMonitorService) ServiceManager
                   .getService(DeviceStorageMonitorService.SERVICE);
        boolean criticalLow = dsm.isMemoryCriticalLow();           
        if ((app.pkgList.size() == 1) &&
                (name=context.getPackageManager().getApplicationLabel(app.info)) != null) {
                mTargetProcess = false;     
                if (criticalLow == true ) {
                    message = res.getString(
                            com.mediatek.R.string.aerr_application_lowmem,
                            name.toString(), app.info.processName);

                }else {
                    message = res.getString(
                        com.android.internal.R.string.aerr_application,
                        name.toString(), app.info.processName);
                }
                setMessage(message);
        } else {
            name = app.processName;
            //these process will restart when killed
            if (((name.toString().indexOf("com.mediatek.bluetooth")) != -1) ||
                ((name.toString().indexOf("android.process.acore")) != -1 )) {
                Slog.v(TAG, "got target error process");     
                mTargetProcess = true;                
            } else {
                mTargetProcess = false;
            }
            
            if (criticalLow == true ) {            
                message = res.getString(
                        com.mediatek.R.string.aerr_process_lowmem,
                        name.toString());
            } else {
                message = res.getString(
                        com.android.internal.R.string.aerr_process,
                        name.toString());
            }
            setMessage(message);
        }
        ///@}
        setCancelable(false);

        setButton(DialogInterface.BUTTON_POSITIVE,
                res.getText(com.android.internal.R.string.force_close),
                mHandler.obtainMessage(FORCE_QUIT));

        if (app.errorReportReceiver != null) {
            setButton(DialogInterface.BUTTON_NEGATIVE,
                    res.getText(com.android.internal.R.string.report),
                    mHandler.obtainMessage(FORCE_QUIT_AND_REPORT));
        } 
        
        if (LowStorageHandle.GetCurrentFlag() == true) {
            setButton(DialogInterface.BUTTON_NEUTRAL,
                    res.getText(com.mediatek.R.string.free_memory_btn),
                    mHandler.obtainMessage(LowStorageHandle.getLowStorageFlag()));			
		}

        setTitle(res.getText(com.android.internal.R.string.aerr_title));
        getWindow().addFlags(FLAG_SYSTEM_ERROR);
        getWindow().setTitle("Application Error: " + app.info.processName);


        if ((criticalLow == true) && (mTargetProcess)) {
           Slog.v(TAG, "do not show the error dialog!");  
           mHandler.sendMessageDelayed(
                mHandler.obtainMessage(FORCE_QUIT),
                0);             
        } else {
        // After the timeout, pretend the user clicked the quit button
            mHandler.sendMessageDelayed(
                mHandler.obtainMessage(FORCE_QUIT),
                DISMISS_TIMEOUT);
        }        
       
             
    }

    private final Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            synchronized (mProc) {
                if (mProc != null && mProc.crashDialog == AppErrorDialog.this) {
                    mProc.crashDialog = null;
                }
            }
            mResult.set(msg.what);

            // If this is a timeout we won't be automatically closed, so go
            // ahead and explicitly dismiss ourselves just in case.
            dismiss();
        }
    };
}
