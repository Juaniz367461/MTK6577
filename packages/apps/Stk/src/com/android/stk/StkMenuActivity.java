/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.stk;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.internal.telephony.cat.Item;
import com.android.internal.telephony.cat.Menu;
import com.android.internal.telephony.cat.CatLog;

import android.widget.AdapterView.AdapterContextMenuInfo;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Context;
import android.provider.Settings.System;
import android.graphics.Color;
import com.mediatek.featureoption.FeatureOption;

import android.view.Gravity;
import android.widget.Toast;

import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

/**
 * ListActivity used for displaying STK menus. These can be SET UP MENU and
 * SELECT ITEM menus. This activity is started multiple times with different
 * menu content.
 *
 */
public class StkMenuActivity extends ListActivity {
	private static final String LOGTAG = "Stk-MA ";
	
    private Context mContext;
    private Menu mStkMenu = null;
    private int mState = STATE_MAIN;
    private boolean mAcceptUsersInput = true;

    private TextView mTitleTextView = null;
    private ImageView mTitleIconView = null;
    private ProgressBar mProgressView = null;

    StkAppService appService = StkAppService.getInstance();
    
    private final BroadcastReceiver mReceiver = new AirplaneBroadcastReceiver(); 
    // Internal state values
    static final int STATE_MAIN = 1;
    static final int STATE_SECONDARY = 2;
    static final int STATE_END = 3;

    // message id for time out
    private static final int MSG_ID_TIMEOUT = 1;

    Handler mTimeoutHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case MSG_ID_TIMEOUT:
                mAcceptUsersInput = false;
                sendResponse(StkAppService.RES_ID_TIMEOUT);
                break;
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        CatLog.d(this, "onCreate");
        // Remove the default title, customized one is used.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        // Set the layout for this activity.
        setContentView(R.layout.stk_menu_list);

        mTitleTextView = (TextView) findViewById(R.id.title_text);
        mTitleIconView = (ImageView) findViewById(R.id.title_icon);
        mProgressView = (ProgressBar) findViewById(R.id.progress_bar);
        mContext = getBaseContext();
        getListView().setOnCreateContextMenuListener(mOnCreateContextMenuListener);
        initFromIntent(getIntent());
        mAcceptUsersInput = true;
/*        
        // Change content background color to support theme mananger.
        if (FeatureOption.MTK_THEMEMANAGER_APP) {
            View contentView = this.findViewById(android.R.id.content);
            contentView.setThemeContentBgColor(Color.TRANSPARENT);
        }  
*/
        IntentFilter intentFilter =
            new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        CatLog.d(this, "onNewIntent");
        initFromIntent(intent);
        mAcceptUsersInput = true;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        if (!mAcceptUsersInput) {
            return;
        }

        Item item = getSelectedItem(position);
        if (item == null) {
            return;
        }
        sendResponse(StkAppService.RES_ID_MENU_SELECTION, item.id, false);
        mAcceptUsersInput = false;
        mProgressView.setVisibility(View.VISIBLE);
        mProgressView.setIndeterminate(true);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (!mAcceptUsersInput) {
            return true;
        }

        switch (keyCode) {
        case KeyEvent.KEYCODE_BACK:
            switch (mState) {
            case STATE_SECONDARY:
                cancelTimeOut();
                mAcceptUsersInput = false;
                sendResponse(StkAppService.RES_ID_BACKWARD);
                return true;
            case STATE_MAIN:
                break;
            }
            break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onResume() {
        super.onResume();
        
        if(isOnFlightMode() == true) {
            CatLog.d(LOGTAG, "don't make stk be visible");
            showTextToast(getString(R.string.lable_on_flight_mode));
            
            finish();
            return;
        }

        if(appService == null) {
            CatLog.d(LOGTAG, "can not launch stk menu 'cause null StkAppService");
            finish();
            return;
        }

        appService.indicateMenuVisibility(true);
        mStkMenu = appService.getMenu();
        if (mStkMenu == null) {
            finish();
            return;
        }
        displayMenu();
        startTimeOut();
        // whenever this activity is resumed after a sub activity was invoked
        // (Browser, In call screen) switch back to main state and enable
        // user's input;
        if (!mAcceptUsersInput) {
            mState = STATE_MAIN;
            mAcceptUsersInput = true;
        }
        // make sure the progress bar is not shown.
        mProgressView.setIndeterminate(false);
        mProgressView.setVisibility(View.GONE);
    }

    @Override
    public void onPause() {
        super.onPause();

        appService.indicateMenuVisibility(false);
        cancelTimeOut();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
        CatLog.d(this, "onDestroy");
    }
    // For long click menu
    private final OnCreateContextMenuListener mOnCreateContextMenuListener =
        new OnCreateContextMenuListener() {
        public void onCreateContextMenu(ContextMenu menu, View v,
                ContextMenuInfo menuInfo) {
            boolean helpVisible = false;
            if (mStkMenu != null) {
                helpVisible = mStkMenu.helpAvailable;
            }
            if(helpVisible == true) {
                menu.add(0, StkApp.MENU_ID_HELP, 0, R.string.help);
            }
        }
    };

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        final AdapterContextMenuInfo info =
            (AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
        case StkApp.MENU_ID_HELP:
            cancelTimeOut();
            mAcceptUsersInput = false;
            Item stkItem = getSelectedItem(info.position);
            if (stkItem == null) {
                break;
            }
            // send help needed response.
            sendResponse(StkAppService.RES_ID_MENU_SELECTION, stkItem.id, true);
            return true;
        default:
            break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, StkApp.MENU_ID_END_SESSION, 1, R.string.menu_end_session);
        menu.add(0, StkApp.MENU_ID_HELP, 2, R.string.help);
        menu.add(0, StkApp.MENU_ID_DEFAULT_ITEM, 3, R.string.help);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(android.view.Menu menu) {
        super.onPrepareOptionsMenu(menu);
        boolean helpVisible = false;
        boolean mainVisible = false;

        if (mState == STATE_SECONDARY) {
            mainVisible = true;
        }
        if (mStkMenu != null) {
            helpVisible = mStkMenu.helpAvailable;
        }

        if(mainVisible) {
            menu.findItem(StkApp.MENU_ID_END_SESSION).setTitle(R.string.menu_end_session);
        }
        menu.findItem(StkApp.MENU_ID_END_SESSION).setVisible(mainVisible);
        
        if(helpVisible) {
            menu.findItem(StkApp.MENU_ID_HELP).setTitle(R.string.help);
        }
        menu.findItem(StkApp.MENU_ID_HELP).setVisible(helpVisible);
        // for defaut item
        if (mStkMenu != null) {
            Item item = mStkMenu.items.get(mStkMenu.defaultItem);
            if(item == null || item.text == null || item.text.length() == 0 ) {
                menu.findItem(StkApp.MENU_ID_DEFAULT_ITEM).setVisible(false);
            } else {
                menu.findItem(StkApp.MENU_ID_DEFAULT_ITEM).setTitle(item.text);
                menu.findItem(StkApp.MENU_ID_DEFAULT_ITEM).setVisible(true);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (!mAcceptUsersInput) {
            return true;
        }
        switch (item.getItemId()) {
        case StkApp.MENU_ID_END_SESSION:
            cancelTimeOut();
            mAcceptUsersInput = false;
            // send session end response.
            sendResponse(StkAppService.RES_ID_END_SESSION);
            return true;
        case StkApp.MENU_ID_HELP:
            cancelTimeOut();
            mAcceptUsersInput = false;
            // Cannot get the current position, just consider as 0.
            int position = 0;
            Item stkItem = getSelectedItem(position);
            if (stkItem == null) {
                break;
            }
            // send help needed response.
            sendResponse(StkAppService.RES_ID_MENU_SELECTION, stkItem.id, true);
            return true;
        case StkApp.MENU_ID_DEFAULT_ITEM:
            if (mStkMenu != null) {
                Item defaultItem = mStkMenu.items.get(mStkMenu.defaultItem);
                if (defaultItem == null) {
                    return true;
                }
                sendResponse(StkAppService.RES_ID_MENU_SELECTION, defaultItem.id,
                        false);
                mAcceptUsersInput = false;
                mProgressView.setVisibility(View.VISIBLE);
                mProgressView.setIndeterminate(true);
            }
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt("STATE", mState);
        outState.putParcelable("MENU", mStkMenu);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mState = savedInstanceState.getInt("STATE");
        mStkMenu = savedInstanceState.getParcelable("MENU");
    }

    private void cancelTimeOut() {
        mTimeoutHandler.removeMessages(MSG_ID_TIMEOUT);
    }

    private void startTimeOut() {
        if (mState == STATE_SECONDARY) {
            // Reset timeout.
            cancelTimeOut();
            mTimeoutHandler.sendMessageDelayed(mTimeoutHandler
                    .obtainMessage(MSG_ID_TIMEOUT), StkApp.UI_TIMEOUT);
        }
    }

    // Bind list adapter to the items list.
    private void displayMenu() {

        if (mStkMenu != null) {
            // Display title & title icon
            if (mStkMenu.titleIcon != null) {
                mTitleIconView.setImageBitmap(mStkMenu.titleIcon);
                mTitleIconView.setVisibility(View.VISIBLE);
            } else {
                mTitleIconView.setVisibility(View.GONE);
            }
            if (!mStkMenu.titleIconSelfExplanatory) {
                mTitleTextView.setVisibility(View.VISIBLE);
                if (mStkMenu.title == null) {
                    mTitleTextView.setText(R.string.app_name);
                } else {
                    mTitleTextView.setText(mStkMenu.title);
                }
            } else {
                mTitleTextView.setVisibility(View.INVISIBLE);
            }
            // create an array adapter for the menu list
            StkMenuAdapter adapter = new StkMenuAdapter(this,
                    mStkMenu.items, mStkMenu.nextActionIndicator, mStkMenu.itemsIconSelfExplanatory);
            // Bind menu list to the new adapter.
            setListAdapter(adapter);
            // Set default item
            setSelection(mStkMenu.defaultItem);
        }
    }

    private void initFromIntent(Intent intent) {

        if (intent != null) {
            mState = intent.getIntExtra("STATE", STATE_MAIN);
            if(mState == STATE_END) {
                finish();
            }
        } else {
            finish();
        }
    }

    private Item getSelectedItem(int position) {
        Item item = null;
        if (mStkMenu != null) {
            try {
                item = mStkMenu.items.get(position);
            } catch (IndexOutOfBoundsException e) {
                if (StkApp.DBG) {
                    CatLog.d(this, "Invalid menu");
                }
            } catch (NullPointerException e) {
                if (StkApp.DBG) {
                    CatLog.d(this, "Invalid menu");
                }
            }
        }
        return item;
    }

    private void sendResponse(int resId) {
        sendResponse(resId, 0, false);
    }

    private void sendResponse(int resId, int itemId, boolean help) {
        if ((STATE_SECONDARY != mState) && (StkAppService.RES_ID_END_SESSION == resId)) {
            CatLog.d(this, "Ignore response of End Session in mState[" + mState + "]");
            return;
        }
        
        Bundle args = new Bundle();
        args.putInt(StkAppService.OPCODE, StkAppService.OP_RESPONSE);
        args.putInt(StkAppService.RES_ID, resId);
        args.putInt(StkAppService.MENU_SELECTION, itemId);
        args.putBoolean(StkAppService.HELP, help);
        mContext.startService(new Intent(mContext, StkAppService.class)
                .putExtras(args));
    }
    
    /**
     * Receiver for misc intent broadcasts the Stk app cares about.
     */
    private class AirplaneBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                // do not care about whether enable areplane mode or not, just finish the screen.
                StkMenuActivity.this.finish();
            }
        }
    }
    
    private void showTextToast(String msg) {
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.BOTTOM, 0, 0);
        toast.show();
    }
    
    private boolean isOnFlightMode() {
        int mode = 0;
        try {
            mode = Settings.System.getInt(mContext.getContentResolver(), Settings.System.AIRPLANE_MODE_ON);
        } catch(SettingNotFoundException e) {
            CatLog.d(LOGTAG, "fail to get airlane mode");
            mode = 0;
        }
        
        CatLog.d(LOGTAG, "airlane mode is " + mode);
        return (mode != 0);
    }
}
