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

#include <linux/input.h>

#include "recovery_ui.h"
#include "common.h"
#include "cust_keys.h"

char* MENU_HEADERS[] = { "Android system recovery utility",
                         "",
                         NULL };

char* MENU_ITEMS[] = { "reboot system now",
                       "apply update from sdcard",
                       "apply update from cache",
                       "wipe data/factory reset",
                       "wipe cache partition",
#ifdef SUPPORT_DATA_BACKUP_RESTORE //wschen 2011-03-09 
                       "backup user data",
                       "restore user data",
#endif                       
                       NULL };

char* MENU_FORCE_ITEMS[] = { "reboot system now",
                             "apply sdcard:update.zip",
                             NULL };

void device_ui_init(UIParameters* ui_parameters) {
}

int device_recovery_start() {
    return 0;
}

int device_toggle_display(volatile char* key_pressed, int key_code) {
    return key_code == RECOVERY_KEY_MENU;
}

int device_reboot_now(volatile char* key_pressed, int key_code) {
    return 0;
}

int device_handle_key(int key_code, int visible) {
    if (visible) {
        switch (key_code) {
            case RECOVERY_KEY_DOWN:
                return HIGHLIGHT_DOWN;

#if (RECOVERY_KEY_UP != RECOVERY_KEY_DOWN)
            case RECOVERY_KEY_UP:
                return HIGHLIGHT_UP;
#endif //(RECOVERY_KEY_UP != RECOVERY_KEY_DOWN)

            case RECOVERY_KEY_ENTER:
                return SELECT_ITEM;
        }
    }

    return NO_ACTION;
}

int device_perform_action(int which) {
    return which;
}

int device_wipe_data() {
    return 0;
}
