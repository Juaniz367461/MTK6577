/*
 * Copyright 2008, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <errno.h>
#include <string.h>
#include <sys/types.h>
#include <signal.h>

#include "hardware_legacy/wifi_hotspot.h"
#include "hardware_legacy/wifi_common.h"

#define LOG_TAG "WifiHotSpot"
#include "cutils/log.h"
#include "cutils/memory.h"
#include "cutils/misc.h"
#include "cutils/properties.h"
#include "private/android_filesystem_config.h"
#ifdef HAVE_LIBC_SYSTEM_PROPERTIES
#define _REALLY_INCLUDE_SYS__SYSTEM_PROPERTIES_H_
#include <sys/_system_properties.h>
#endif

extern struct wpa_ctrl* p2p_ctrl_open(const char *ctrl_path);
extern void p2p_ctrl_close(struct wpa_ctrl *ctrl);
extern int p2p_ctrl_request(struct wpa_ctrl *ctrl, const char *cmd, size_t cmd_len, char *reply, size_t *reply_len, void (*msg_cb)(char *msg, size_t len));
extern int p2p_ctrl_recv(struct wpa_ctrl *ctrl, char *reply, size_t *reply_len);
extern int p2p_ctrl_attach(struct wpa_ctrl *ctrl);

static const SUPPLICANT_PARA_T rHotspotSuppPara = {
    "ap0",
    "wifi.tethering.interface",
    "/data/misc/p2p_supplicant",
    "p2p_supplicant",
    "init.svc.p2p_supplicant",
    "/system/etc/wifi/p2p_supplicant.conf",
    "/data/misc/wifi/p2p_supplicant.conf",
    "%s:-Dnl80211 -i%s -c%s -dd",
#ifdef CFG_SUPPORT_CONCURRENT_NETWORK
    "p2p_ctrl"
#else
    "wpa_ctrl"
#endif
};

static SUPPLICANT_INFO_T rHotspotSuppInfo = {
    "ap0",
    NULL,
    NULL,
#ifdef CFG_SUPPORT_CONCURRENT_NETWORK
    {
        p2p_ctrl_open,
        p2p_ctrl_close,
        p2p_ctrl_request,
        p2p_ctrl_recv,
        p2p_ctrl_attach
    },
#else
    {   //Supplicant ctrl API
        wpa_ctrl_open,
        wpa_ctrl_close,
        wpa_ctrl_request,
        wpa_ctrl_recv,
        wpa_ctrl_attach
    },
#endif

    {                           //exit socket pair
        -1,
        -1
    }
};

int wifi_hotspot_load_driver()
{
    property_get(
        rHotspotSuppPara.acIfPropName,
        rHotspotSuppInfo.acIfName,
        rHotspotSuppPara.acIfDefName);
    return halDoCommand("load hotspot");
}

int wifi_hotspot_unload_driver()
{
    return halDoCommand("unload hotspot");
}

int wifi_hotspot_start_supplicant()
{
   return start_supplicant(&rHotspotSuppPara, rHotspotSuppInfo.acIfName);
}

int wifi_hotspot_stop_supplicant()
{
    return stop_supplicant(&rHotspotSuppPara, &rHotspotSuppInfo);
}

int wifi_hotspot_connect_to_supplicant()
{
    return connect_to_supplicant(&rHotspotSuppPara, &rHotspotSuppInfo);
}

void wifi_hotspot_close_supplicant_connection_no_wait()
{
    close_supplicant_connection_no_wait(&rHotspotSuppPara, &rHotspotSuppInfo);
}

void wifi_hotspot_close_supplicant_connection()
{
    close_supplicant_connection(&rHotspotSuppPara, &rHotspotSuppInfo);
}

int wifi_hotspot_command(const char *command, char *reply, size_t *reply_len)
{
    return send_command(&rHotspotSuppInfo, command, reply, reply_len);
}

int wifi_hotspot_set_iface(int on)
{
    return set_iface(rHotspotSuppInfo.acIfName, on);
}

int wifi_hotspot_wait_for_event(char *buf, size_t buflen)
{
    return wait_for_event(&rHotspotSuppInfo, buf, buflen);
}

