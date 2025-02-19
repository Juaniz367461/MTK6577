/*
** Copyright 2006, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

#define LOG_TAG "bluetooth_common.cpp"

#include "android_bluetooth_common.h"
#include "JNIHelp.h"
#include "jni.h"
#include "utils/Log.h"
#include "utils/misc.h"

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <errno.h>
#include <unistd.h>
#include <cutils/properties.h>

#ifdef HAVE_BLUETOOTH
#include <dbus/dbus.h>
#endif

namespace android {

#ifdef HAVE_BLUETOOTH

static Properties remote_device_properties[] = {
    {"Address",  DBUS_TYPE_STRING},
    {"Name", DBUS_TYPE_STRING},
    {"Icon", DBUS_TYPE_STRING},
    {"Class", DBUS_TYPE_UINT32},
    {"UUIDs", DBUS_TYPE_ARRAY},
    {"Services", DBUS_TYPE_ARRAY},
    {"Paired", DBUS_TYPE_BOOLEAN},
    {"Connected", DBUS_TYPE_BOOLEAN},
    {"Trusted", DBUS_TYPE_BOOLEAN},
    {"Blocked", DBUS_TYPE_BOOLEAN},
    {"Alias", DBUS_TYPE_STRING},
    {"Nodes", DBUS_TYPE_ARRAY},
    {"Adapter", DBUS_TYPE_OBJECT_PATH},
    {"LegacyPairing", DBUS_TYPE_BOOLEAN},
    {"RSSI", DBUS_TYPE_INT16},
    {"TX", DBUS_TYPE_UINT32},
    {"Broadcaster", DBUS_TYPE_BOOLEAN}
};

static Properties adapter_properties[] = {
    {"Address", DBUS_TYPE_STRING},
    {"Name", DBUS_TYPE_STRING},
    {"Class", DBUS_TYPE_UINT32},
    {"Powered", DBUS_TYPE_BOOLEAN},
    {"Discoverable", DBUS_TYPE_BOOLEAN},
    {"DiscoverableTimeout", DBUS_TYPE_UINT32},
    {"Pairable", DBUS_TYPE_BOOLEAN},
    {"PairableTimeout", DBUS_TYPE_UINT32},
    {"Discovering", DBUS_TYPE_BOOLEAN},
    {"Devices", DBUS_TYPE_ARRAY},
    {"UUIDs", DBUS_TYPE_ARRAY},
};

static Properties input_properties[] = {
    {"Connected", DBUS_TYPE_BOOLEAN},
};

static Properties pan_properties[] = {
    {"Connected", DBUS_TYPE_BOOLEAN},
    {"Interface", DBUS_TYPE_STRING},
    {"UUID", DBUS_TYPE_STRING},
};

static Properties health_device_properties[] = {
    {"MainChannel", DBUS_TYPE_OBJECT_PATH},
};

static Properties health_channel_properties[] = {
    {"Type", DBUS_TYPE_STRING},
    {"Device", DBUS_TYPE_OBJECT_PATH},
    {"Application", DBUS_TYPE_OBJECT_PATH},
};

typedef union {
    char *str_val;
    int int_val;
    char **array_val;
} property_value;
#endif
jfieldID get_field(JNIEnv *env, jclass clazz, const char *member,
                   const char *mtype) {
    jfieldID field = env->GetFieldID(clazz, member, mtype);
    if (field == NULL) {
        LOGE("Can't find member %s", member);
    }
    return field;
}

static void set_object_array_element(JNIEnv *env, jobjectArray strArray,
                                     const char *value, int index) {
    jstring obj;
    obj = env->NewStringUTF(value);
    env->SetObjectArrayElement(strArray, index, obj);
    env->DeleteLocalRef(obj);
}

#if defined(__BTMTK__)
void create_prop_array(JNIEnv *env, jobjectArray strArray, Properties *property,
                       property_value *value, int len, int *array_index ) {
    char **prop_val = NULL;
    char buf[32] = {'\0'}, buf1[32] = {'\0'};
    int i;

#ifdef __BTMTK__
    LOGI("[JNI][API] create_prop_array : name=%s, type=%c, len=%d", property->name, property->type, len);
#endif

    char *name = property->name;
    int prop_type = property->type;

    set_object_array_element(env, strArray, name, *array_index);
    *array_index += 1;

    if (prop_type == DBUS_TYPE_UINT32 || prop_type == DBUS_TYPE_INT16) {
        sprintf(buf, "%d", value->int_val);
        set_object_array_element(env, strArray, buf, *array_index);
        *array_index += 1;
    } else if (prop_type == DBUS_TYPE_BOOLEAN) {
        sprintf(buf, "%s", value->int_val ? "true" : "false");

        set_object_array_element(env, strArray, buf, *array_index);
        *array_index += 1;
    } else if (prop_type == DBUS_TYPE_ARRAY) {
        // Write the length first
        sprintf(buf1, "%d", len);
        set_object_array_element(env, strArray, buf1, *array_index);
        *array_index += 1;

        prop_val = value->array_val;
        for (i = 0; i < len; i++) {
            set_object_array_element(env, strArray, prop_val[i], *array_index);
            *array_index += 1;
        }
    } else {
        set_object_array_element(env, strArray, (const char *) value->str_val, *array_index);
        *array_index += 1;
    }
}
#endif

#ifdef HAVE_BLUETOOTH
typedef struct {
    void (*user_cb)(DBusMessage *, void *, void *);
    void *user;
    void *nat;
    JNIEnv *env;
} dbus_async_call_t;

void dbus_func_args_async_callback(DBusPendingCall *call, void *data) {

    dbus_async_call_t *req = (dbus_async_call_t *)data;
    DBusMessage *msg;

    /* This is guaranteed to be non-NULL, because this function is called only
       when once the remote method invokation returns. */
    msg = dbus_pending_call_steal_reply(call);

    if (msg) {
        if (req->user_cb) {
            // The user may not deref the message object.
            req->user_cb(msg, req->user, req->nat);
        }
        dbus_message_unref(msg);
    }

    //dbus_message_unref(req->method);
    dbus_pending_call_cancel(call);
    dbus_pending_call_unref(call);
    free(req);
}

static dbus_bool_t dbus_func_args_async_valist(JNIEnv *env,
                                        DBusConnection *conn,
                                        int timeout_ms,
                                        void (*user_cb)(DBusMessage *,
                                                        void *,
                                                        void*),
                                        void *user,
                                        void *nat,
                                        const char *path,
                                        const char *ifc,
                                        const char *func,
                                        int first_arg_type,
                                        va_list args) {
    DBusMessage *msg = NULL;
    const char *name;
    dbus_async_call_t *pending;
    dbus_bool_t reply = FALSE;

    /* Compose the command */
    msg = dbus_message_new_method_call(BLUEZ_DBUS_BASE_IFC, path, ifc, func);

    if (msg == NULL) {
        LOGE("Could not allocate D-Bus message object!");
        goto done;
    }

    /* append arguments */
    if (!dbus_message_append_args_valist(msg, first_arg_type, args)) {
        LOGE("Could not append argument to method call!");
        goto done;
    }

    /* Make the call. */
    pending = (dbus_async_call_t *)malloc(sizeof(dbus_async_call_t));
    if (pending) {
        DBusPendingCall *call;

        pending->env = env;
        pending->user_cb = user_cb;
        pending->user = user;
        pending->nat = nat;
        //pending->method = msg;

        reply = dbus_connection_send_with_reply(conn, msg,
                                                &call,
                                                timeout_ms);
        if (reply == TRUE) {
            dbus_pending_call_set_notify(call,
                                         dbus_func_args_async_callback,
                                         pending,
                                         NULL);
        }
    }

done:
    if (msg) dbus_message_unref(msg);
    return reply;
}

dbus_bool_t dbus_func_args_async(JNIEnv *env,
                                 DBusConnection *conn,
                                 int timeout_ms,
                                 void (*reply)(DBusMessage *, void *, void*),
                                 void *user,
                                 void *nat,
                                 const char *path,
                                 const char *ifc,
                                 const char *func,
                                 int first_arg_type,
                                 ...) {
    dbus_bool_t ret;
    va_list lst;
    va_start(lst, first_arg_type);

    ret = dbus_func_args_async_valist(env, conn,
                                      timeout_ms,
                                      reply, user, nat,
                                      path, ifc, func,
                                      first_arg_type, lst);
    va_end(lst);
    return ret;
}

// If err is NULL, then any errors will be LOGE'd, and free'd and the reply
// will be NULL.
// If err is not NULL, then it is assumed that dbus_error_init was already
// called, and error's will be returned to the caller without logging. The
// return value is NULL iff an error was set. The client must free the error if
// set.
DBusMessage * dbus_func_args_timeout_valist(JNIEnv *env,
                                            DBusConnection *conn,
                                            int timeout_ms,
                                            DBusError *err,
                                            const char *path,
                                            const char *ifc,
                                            const char *func,
                                            int first_arg_type,
                                            va_list args) {

    DBusMessage *msg = NULL, *reply = NULL;
    const char *name;
    bool return_error = (err != NULL);

    if (!return_error) {
        err = (DBusError*)malloc(sizeof(DBusError));
        dbus_error_init(err);
    }

    /* Compose the command */
    msg = dbus_message_new_method_call(BLUEZ_DBUS_BASE_IFC, path, ifc, func);

    if (msg == NULL) {
        LOGE("Could not allocate D-Bus message object!");
        goto done;
    }

    /* append arguments */
    if (!dbus_message_append_args_valist(msg, first_arg_type, args)) {
        LOGE("Could not append argument to method call!");
        goto done;
    }

    /* Make the call. */
    reply = dbus_connection_send_with_reply_and_block(conn, msg, timeout_ms, err);
    if (!return_error && dbus_error_is_set(err)) {
        LOG_AND_FREE_DBUS_ERROR_WITH_MSG(err, msg);
    }

done:
    if (!return_error) {
        free(err);
    }
    if (msg) dbus_message_unref(msg);
    return reply;
}

DBusMessage * dbus_func_args_timeout(JNIEnv *env,
                                     DBusConnection *conn,
                                     int timeout_ms,
                                     const char *path,
                                     const char *ifc,
                                     const char *func,
                                     int first_arg_type,
                                     ...) {
    DBusMessage *ret;
    va_list lst;
    va_start(lst, first_arg_type);
    ret = dbus_func_args_timeout_valist(env, conn, timeout_ms, NULL,
                                        path, ifc, func,
                                        first_arg_type, lst);
    va_end(lst);
    return ret;
}

DBusMessage * dbus_func_args(JNIEnv *env,
                             DBusConnection *conn,
                             const char *path,
                             const char *ifc,
                             const char *func,
                             int first_arg_type,
                             ...) {
    DBusMessage *ret;
    va_list lst;
    va_start(lst, first_arg_type);
    ret = dbus_func_args_timeout_valist(env, conn, -1, NULL,
                                        path, ifc, func,
                                        first_arg_type, lst);
    va_end(lst);
    return ret;
}

DBusMessage * dbus_func_args_error(JNIEnv *env,
                                   DBusConnection *conn,
                                   DBusError *err,
                                   const char *path,
                                   const char *ifc,
                                   const char *func,
                                   int first_arg_type,
                                   ...) {
    DBusMessage *ret;
    va_list lst;
    va_start(lst, first_arg_type);
    ret = dbus_func_args_timeout_valist(env, conn, -1, err,
                                        path, ifc, func,
                                        first_arg_type, lst);
    va_end(lst);
    return ret;
}

jint dbus_returns_unixfd(JNIEnv *env, DBusMessage *reply) {

    DBusError err;
    jint ret = -1;

    dbus_error_init(&err);
    if (!dbus_message_get_args(reply, &err,
                               DBUS_TYPE_UNIX_FD, &ret,
                               DBUS_TYPE_INVALID)) {
        LOG_AND_FREE_DBUS_ERROR_WITH_MSG(&err, reply);
    }
    dbus_message_unref(reply);
    return ret;
}


jint dbus_returns_int32(JNIEnv *env, DBusMessage *reply) {

    DBusError err;
    jint ret = -1;

    dbus_error_init(&err);
    if (!dbus_message_get_args(reply, &err,
                               DBUS_TYPE_INT32, &ret,
                               DBUS_TYPE_INVALID)) {
        LOG_AND_FREE_DBUS_ERROR_WITH_MSG(&err, reply);
    }
    dbus_message_unref(reply);
    return ret;
}

jint dbus_returns_uint32(JNIEnv *env, DBusMessage *reply) {

    DBusError err;
    jint ret = -1;

    dbus_error_init(&err);
    if (!dbus_message_get_args(reply, &err,
                               DBUS_TYPE_UINT32, &ret,
                               DBUS_TYPE_INVALID)) {
        LOG_AND_FREE_DBUS_ERROR_WITH_MSG(&err, reply);
    }
    dbus_message_unref(reply);
    return ret;
}

jstring dbus_returns_string(JNIEnv *env, DBusMessage *reply) {

    DBusError err;
    jstring ret = NULL;
    const char *name;

    dbus_error_init(&err);
    if (dbus_message_get_args(reply, &err,
                               DBUS_TYPE_STRING, &name,
                               DBUS_TYPE_INVALID)) {
        ret = env->NewStringUTF(name);
    } else {
        LOG_AND_FREE_DBUS_ERROR_WITH_MSG(&err, reply);
    }
    dbus_message_unref(reply);

    return ret;
}

jboolean dbus_returns_boolean(JNIEnv *env, DBusMessage *reply) {
    DBusError err;
    jboolean ret = JNI_FALSE;
    dbus_bool_t val = FALSE;

    dbus_error_init(&err);

    /* Check the return value. */
    if (dbus_message_get_args(reply, &err,
                               DBUS_TYPE_BOOLEAN, &val,
                               DBUS_TYPE_INVALID)) {
        ret = val == TRUE ? JNI_TRUE : JNI_FALSE;
    } else {
        LOG_AND_FREE_DBUS_ERROR_WITH_MSG(&err, reply);
    }

    dbus_message_unref(reply);
    return ret;
}

#ifndef RDA_BT_SUPPORT
static void set_object_array_element(JNIEnv *env, jobjectArray strArray,
                                     const char *value, int index) {
    jstring obj;
    obj = env->NewStringUTF(value);
    env->SetObjectArrayElement(strArray, index, obj);
    env->DeleteLocalRef(obj);
}
#endif

jobjectArray dbus_returns_array_of_object_path(JNIEnv *env,
                                               DBusMessage *reply) {

    DBusError err;
    char **list;
    int i, len;
    jobjectArray strArray = NULL;

    dbus_error_init(&err);
    if (dbus_message_get_args (reply,
                               &err,
                               DBUS_TYPE_ARRAY, DBUS_TYPE_OBJECT_PATH,
                               &list, &len,
                               DBUS_TYPE_INVALID)) {
        jclass stringClass;
        jstring classNameStr;

        stringClass = env->FindClass("java/lang/String");
        strArray = env->NewObjectArray(len, stringClass, NULL);

        for (i = 0; i < len; i++)
            set_object_array_element(env, strArray, list[i], i);
    } else {
        LOG_AND_FREE_DBUS_ERROR_WITH_MSG(&err, reply);
    }

    dbus_message_unref(reply);
    return strArray;
}

jobjectArray dbus_returns_array_of_strings(JNIEnv *env, DBusMessage *reply) {

    DBusError err;
    char **list;
    int i, len;
    jobjectArray strArray = NULL;

    dbus_error_init(&err);
    if (dbus_message_get_args (reply,
                               &err,
                               DBUS_TYPE_ARRAY, DBUS_TYPE_STRING,
                               &list, &len,
                               DBUS_TYPE_INVALID)) {
        jclass stringClass;
        jstring classNameStr;

        //LOGV("%s: there are %d elements in string array!", __FUNCTION__, len);

        stringClass = env->FindClass("java/lang/String");
        strArray = env->NewObjectArray(len, stringClass, NULL);

        for (i = 0; i < len; i++)
            set_object_array_element(env, strArray, list[i], i);
    } else {
        LOG_AND_FREE_DBUS_ERROR_WITH_MSG(&err, reply);
    }

    dbus_message_unref(reply);
    return strArray;
}

jbyteArray dbus_returns_array_of_bytes(JNIEnv *env, DBusMessage *reply) {

    DBusError err;
    int i, len;
    jbyte *list;
    jbyteArray byteArray = NULL;

    dbus_error_init(&err);
    if (dbus_message_get_args(reply, &err,
                              DBUS_TYPE_ARRAY, DBUS_TYPE_BYTE, &list, &len,
                              DBUS_TYPE_INVALID)) {
        //LOGV("%s: there are %d elements in byte array!", __FUNCTION__, len);
        byteArray = env->NewByteArray(len);
        if (byteArray)
            env->SetByteArrayRegion(byteArray, 0, len, list);

    } else {
        LOG_AND_FREE_DBUS_ERROR_WITH_MSG(&err, reply);
    }

    dbus_message_unref(reply);
    return byteArray;
}

void append_variant(DBusMessageIter *iter, int type, void *val)
{
    DBusMessageIter value_iter;
    char var_type[2] = { type, '\0'};
    dbus_message_iter_open_container(iter, DBUS_TYPE_VARIANT, var_type, &value_iter);
    dbus_message_iter_append_basic(&value_iter, type, val);
    dbus_message_iter_close_container(iter, &value_iter);
}

static void dict_append_entry(DBusMessageIter *dict,
                        const char *key, int type, void *val)
{
        DBusMessageIter dict_entry;
        dbus_message_iter_open_container(dict, DBUS_TYPE_DICT_ENTRY,
                                                        NULL, &dict_entry);

        dbus_message_iter_append_basic(&dict_entry, DBUS_TYPE_STRING, &key);
        append_variant(&dict_entry, type, val);
        dbus_message_iter_close_container(dict, &dict_entry);
}

static void append_dict_valist(DBusMessageIter *iterator, const char *first_key,
                                va_list var_args)
{
        DBusMessageIter dict;
        int val_type;
        const char *val_key;
        void *val;

        dbus_message_iter_open_container(iterator, DBUS_TYPE_ARRAY,
                        DBUS_DICT_ENTRY_BEGIN_CHAR_AS_STRING
                        DBUS_TYPE_STRING_AS_STRING DBUS_TYPE_VARIANT_AS_STRING
                        DBUS_DICT_ENTRY_END_CHAR_AS_STRING, &dict);

        val_key = first_key;
        while (val_key) {
                val_type = va_arg(var_args, int);
                val = va_arg(var_args, void *);
                dict_append_entry(&dict, val_key, val_type, val);
                val_key = va_arg(var_args, char *);
        }

        dbus_message_iter_close_container(iterator, &dict);
}

void append_dict_args(DBusMessage *reply, const char *first_key, ...)
{
        DBusMessageIter iter;
        va_list var_args;

        dbus_message_iter_init_append(reply, &iter);

        va_start(var_args, first_key);
        append_dict_valist(&iter, first_key, var_args);
        va_end(var_args);
}


int get_property(DBusMessageIter iter, Properties *properties,
                  int max_num_properties, int *prop_index, property_value *value, int *len) {
    DBusMessageIter prop_val, array_val_iter;
    char *property = NULL;
    uint32_t array_type;
    char *str_val;
    int i, j, type, int_val;

    if (dbus_message_iter_get_arg_type(&iter) != DBUS_TYPE_STRING)
        return -1;
    dbus_message_iter_get_basic(&iter, &property);
    if (!dbus_message_iter_next(&iter))
        return -1;
    if (dbus_message_iter_get_arg_type(&iter) != DBUS_TYPE_VARIANT)
        return -1;
    for (i = 0; i <  max_num_properties; i++) {
        if (!strncmp(property, properties[i].name, strlen(property)))
            break;
    }
    *prop_index = i;
    if (i == max_num_properties)
        return -1;

    dbus_message_iter_recurse(&iter, &prop_val);
    type = properties[*prop_index].type;
    if (dbus_message_iter_get_arg_type(&prop_val) != type) {
        LOGE("Property type mismatch in get_property: %d, expected:%d, index:%d",
             dbus_message_iter_get_arg_type(&prop_val), type, *prop_index);
        return -1;
    }

    switch(type) {
    case DBUS_TYPE_STRING:
    case DBUS_TYPE_OBJECT_PATH:
        dbus_message_iter_get_basic(&prop_val, &value->str_val);
        *len = 1;
        break;
    case DBUS_TYPE_UINT32:
    case DBUS_TYPE_INT16:
    case DBUS_TYPE_BOOLEAN:
        dbus_message_iter_get_basic(&prop_val, &int_val);
        value->int_val = int_val;
        *len = 1;
        break;
    case DBUS_TYPE_ARRAY:
        dbus_message_iter_recurse(&prop_val, &array_val_iter);
        array_type = dbus_message_iter_get_arg_type(&array_val_iter);
        *len = 0;
        value->array_val = NULL;
        if (array_type == DBUS_TYPE_OBJECT_PATH ||
            array_type == DBUS_TYPE_STRING){
            j = 0;
            do {
               j ++;
            } while(dbus_message_iter_next(&array_val_iter));
            dbus_message_iter_recurse(&prop_val, &array_val_iter);
            // Allocate  an array of char *
            *len = j;
            char **tmp = (char **)malloc(sizeof(char *) * *len);
            if (!tmp)
                return -1;
            j = 0;
            do {
               dbus_message_iter_get_basic(&array_val_iter, &tmp[j]);
               j ++;
            } while(dbus_message_iter_next(&array_val_iter));
            value->array_val = tmp;
        }
        break;
    default:
        return -1;
    }
    return 0;
}

void create_prop_array(JNIEnv *env, jobjectArray strArray, Properties *property,
                       property_value *value, int len, int *array_index ) {
    char **prop_val = NULL;
    char buf[32] = {'\0'}, buf1[32] = {'\0'};
    int i;

    char *name = property->name;
    int prop_type = property->type;

    set_object_array_element(env, strArray, name, *array_index);
    *array_index += 1;

    if (prop_type == DBUS_TYPE_UINT32 || prop_type == DBUS_TYPE_INT16) {
        sprintf(buf, "%d", value->int_val);
        set_object_array_element(env, strArray, buf, *array_index);
        *array_index += 1;
    } else if (prop_type == DBUS_TYPE_BOOLEAN) {
        sprintf(buf, "%s", value->int_val ? "true" : "false");

        set_object_array_element(env, strArray, buf, *array_index);
        *array_index += 1;
    } else if (prop_type == DBUS_TYPE_ARRAY) {
        // Write the length first
        sprintf(buf1, "%d", len);
        set_object_array_element(env, strArray, buf1, *array_index);
        *array_index += 1;

        prop_val = value->array_val;
        for (i = 0; i < len; i++) {
            set_object_array_element(env, strArray, prop_val[i], *array_index);
            *array_index += 1;
        }
    } else {
        set_object_array_element(env, strArray, (const char *) value->str_val, *array_index);
        *array_index += 1;
    }
}

jobjectArray parse_properties(JNIEnv *env, DBusMessageIter *iter, Properties *properties,
                              const int max_num_properties) {
    DBusMessageIter dict_entry, dict;
    jobjectArray strArray = NULL;
    property_value value;
    int i, size = 0,array_index = 0;
    int len = 0, prop_type = DBUS_TYPE_INVALID, prop_index = -1, type;
    struct {
        property_value value;
        int len;
        bool used;
    } values[max_num_properties];
    int t, j;

    jclass stringClass = env->FindClass("java/lang/String");
    DBusError err;
    dbus_error_init(&err);

    for (i = 0; i < max_num_properties; i++) {
        values[i].used = false;
    }

    if(dbus_message_iter_get_arg_type(iter) != DBUS_TYPE_ARRAY)
        goto failure;
    dbus_message_iter_recurse(iter, &dict);
    do {
        len = 0;
        if (dbus_message_iter_get_arg_type(&dict) != DBUS_TYPE_DICT_ENTRY)
            goto failure;
        dbus_message_iter_recurse(&dict, &dict_entry);

        if (!get_property(dict_entry, properties, max_num_properties, &prop_index,
                          &value, &len)) {
            size += 2;
            if (properties[prop_index].type == DBUS_TYPE_ARRAY)
                size += len;
            values[prop_index].value = value;
            values[prop_index].len = len;
            values[prop_index].used = true;
        } else {
            goto failure;
        }
    } while(dbus_message_iter_next(&dict));

    strArray = env->NewObjectArray(size, stringClass, NULL);

    for (i = 0; i < max_num_properties; i++) {
        if (values[i].used) {
            create_prop_array(env, strArray, &properties[i], &values[i].value, values[i].len,
                              &array_index);

            if (properties[i].type == DBUS_TYPE_ARRAY && values[i].used
                   && values[i].value.array_val != NULL)
                free(values[i].value.array_val);
        }

    }
    return strArray;

failure:
    if (dbus_error_is_set(&err))
        LOG_AND_FREE_DBUS_ERROR(&err);
    for (i = 0; i < max_num_properties; i++)
        if (properties[i].type == DBUS_TYPE_ARRAY && values[i].used == true
                                        && values[i].value.array_val != NULL)
            free(values[i].value.array_val);
    return NULL;
}

jobjectArray parse_property_change(JNIEnv *env, DBusMessage *msg,
                           Properties *properties, int max_num_properties) {
    DBusMessageIter iter;
    DBusError err;
    jobjectArray strArray = NULL;
    jclass stringClass= env->FindClass("java/lang/String");
    int len = 0, prop_index = -1;
    int array_index = 0, size = 0;
    property_value value;

    dbus_error_init(&err);
    if (!dbus_message_iter_init(msg, &iter))
        goto failure;

    if (!get_property(iter, properties, max_num_properties,
                      &prop_index, &value, &len)) {
        size += 2;
        if (properties[prop_index].type == DBUS_TYPE_ARRAY)
            size += len;
        strArray = env->NewObjectArray(size, stringClass, NULL);

        create_prop_array(env, strArray, &properties[prop_index],
                          &value, len, &array_index);

        if (properties[prop_index].type == DBUS_TYPE_ARRAY && value.array_val != NULL)
             free(value.array_val);

        return strArray;
    }
failure:
    LOG_AND_FREE_DBUS_ERROR_WITH_MSG(&err, msg);
    return NULL;
}

jobjectArray parse_adapter_property_change(JNIEnv *env, DBusMessage *msg) {
    return parse_property_change(env, msg, (Properties *) &adapter_properties,
                    sizeof(adapter_properties) / sizeof(Properties));
}

jobjectArray parse_remote_device_property_change(JNIEnv *env, DBusMessage *msg) {
    return parse_property_change(env, msg, (Properties *) &remote_device_properties,
                    sizeof(remote_device_properties) / sizeof(Properties));
}

jobjectArray parse_input_property_change(JNIEnv *env, DBusMessage *msg) {
    return parse_property_change(env, msg, (Properties *) &input_properties,
                    sizeof(input_properties) / sizeof(Properties));
}

jobjectArray parse_pan_property_change(JNIEnv *env, DBusMessage *msg) {
    return parse_property_change(env, msg, (Properties *) &pan_properties,
                    sizeof(pan_properties) / sizeof(Properties));
}

jobjectArray parse_adapter_properties(JNIEnv *env, DBusMessageIter *iter) {
    return parse_properties(env, iter, (Properties *) &adapter_properties,
                            sizeof(adapter_properties) / sizeof(Properties));
}

jobjectArray parse_remote_device_properties(JNIEnv *env, DBusMessageIter *iter) {
    return parse_properties(env, iter, (Properties *) &remote_device_properties,
                          sizeof(remote_device_properties) / sizeof(Properties));
}

jobjectArray parse_input_properties(JNIEnv *env, DBusMessageIter *iter) {
    return parse_properties(env, iter, (Properties *) &input_properties,
                          sizeof(input_properties) / sizeof(Properties));
}

jobjectArray parse_health_device_properties(JNIEnv *env, DBusMessageIter *iter) {
    return parse_properties(env, iter, (Properties *) &health_device_properties,
                          sizeof(health_device_properties) / sizeof(Properties));
}

jobjectArray parse_health_device_property_change(JNIEnv *env, DBusMessage *msg) {
    return parse_property_change(env, msg, (Properties *) &health_device_properties,
                    sizeof(health_device_properties) / sizeof(Properties));
}

jobjectArray parse_health_channel_properties(JNIEnv *env, DBusMessageIter *iter) {
    return parse_properties(env, iter, (Properties *) &health_channel_properties,
                          sizeof(health_channel_properties) / sizeof(Properties));
}

int get_bdaddr(const char *str, bdaddr_t *ba) {
    char *d = ((char *)ba) + 5, *endp;
    int i;
    for(i = 0; i < 6; i++) {
        *d-- = strtol(str, &endp, 16);
        if (*endp != ':' && i != 5) {
            memset(ba, 0, sizeof(bdaddr_t));
            return -1;
        }
        str = endp + 1;
    }
    return 0;
}

void get_bdaddr_as_string(const bdaddr_t *ba, char *str) {
    const uint8_t *b = (const uint8_t *)ba;
    sprintf(str, "%2.2X:%2.2X:%2.2X:%2.2X:%2.2X:%2.2X",
            b[5], b[4], b[3], b[2], b[1], b[0]);
}

bool debug_no_encrypt() {
    return false;
#if 0
    char value[PROPERTY_VALUE_MAX] = "";

    property_get("debug.bt.no_encrypt", value, "");
    if (!strncmp("true", value, PROPERTY_VALUE_MAX) ||
        !strncmp("1", value, PROPERTY_VALUE_MAX)) {
        LOGD("mandatory bluetooth encryption disabled");
        return true;
    } else {
        return false;
    }
#endif
}
#endif

#ifdef __BTMTK__
bool bt_sendmsg(int sock, void* ptr, int size)
{
    int ret = -1;
    ilm_struct *ilm = (ilm_struct*)ptr;
    U16 *field;
    struct sockaddr_un servname;
    socklen_t servnamelen;    

    /* LOCAL_PARA_HDR */
    field = (U16 *)ilm->ilm_data;
    *field = 1;
    *(++field) = size;
	
    size = sizeof(ilm_struct) - MAX_ILM_BUFFER_SIZE + size;
    ilm->src_mod_id = MOD_MMI;
    ilm->dest_mod_id = MOD_BT;
    LOGI("[JNI] bt_sendmsg(cmd=%lu, ptr=%p, len=%d)", ilm->msg_id, ptr, size);
    servname.sun_family = AF_UNIX;
    strcpy (servname.sun_path, BT_SOCK_NAME_INT_ADP);
    servnamelen = (offsetof (struct sockaddr_un, sun_path) + strlen (servname.sun_path) + 1);

    if(sock >= 0)
    {
    	#if 1
        ret = sendto(sock, ptr, size, 0, (const sockaddr*)&servname, servnamelen);
        #else
        ret = send(nat->servsock, ptr, size, 0);
        #endif
        if(ret < 0)
        {
            LOGE("[JNI] send msg fail : %s, %d", strerror(errno), errno);
        }
        else
        {
            LOGI("[JNI] send msg success : %d", ret);
        }
    }
    else
    {
        LOGE("[JNI] server socket uninitialized");
    }
    return (ret >= 0) ? true : false;
}

/* -1 : fail, 0 : timeout, 1: success */
int wait_response(int sock, unsigned long msg_id, ilm_struct* ilm, int timeout /* millisecond */)
{
    int res = 0;
    fd_set readfs;
    struct timeval Timeout;
	struct timeval orignalTime;
	struct timeval currentTime;
	struct timezone zone;
	int ret;
	int currentTimeout = timeout;
	
	FD_ZERO(&readfs);
    if(sock >= 0)
    {
        FD_SET(sock, &readfs);
    }
    else
    {
        LOGE("[JNI][ERR] sock == 0. exit");
    }

	ret = gettimeofday(&orignalTime, &zone);
	if (0 != ret)
	{
		LOGE("[JNI] gettimeofday failed : %s, %d", strerror(errno), errno);
		memset(&currentTime, 0x0, sizeof(struct timeval));
	}    
	while (TRUE)
	{
		Timeout.tv_usec = (currentTimeout%1000)*1000;
    	Timeout.tv_sec  = currentTimeout/1000;
		res = select(sock+1, &readfs, NULL, NULL, &Timeout);
		if(res > 0)
		{
			res = recvfrom(sock, ilm, sizeof(ilm_struct), 0, NULL, NULL);
			if(res < 0)
			{
				LOGE("[JNI] recvfrom failed : %s, %d", strerror(errno), errno);
				break;
			}
			else
			{
				if( ilm->msg_id != msg_id)
				{
					LOGE("[JNI] recv res bytes=%d, resp=%lu is mismatch, expected : %lu", res, ilm->msg_id, msg_id);
				}
				else
				{
					break;
				}
			}
		}
		else if(res == 0)
		{
			LOGW("[JNI] timeout waiting response, in %d milliseconds", currentTimeout);
			break;
		}
		else
		{
			LOGE("[JNI] select failed: %s, %d", strerror(errno), errno);
			break;
		}

		ret = gettimeofday(&currentTime, &zone);
		if (0 != ret)
		{
			LOGE("[JNI] gettimeofday failed : %s, %d", strerror(errno), errno);
			break;;
		}
		currentTimeout = (currentTime.tv_sec - orignalTime.tv_sec) * 1000 + 
							(currentTime.tv_usec/1000 - orignalTime.tv_usec/1000);
		if (currentTimeout < timeout)
		{
			currentTimeout = timeout - currentTimeout;
			LOGW("[JNI] continue to wait response for %d(us)", currentTimeout);
		}
		else
		{
			LOGW("[JNI] timeout waiting response, in %d milliseconds", timeout);
			res = -1;
			break;
		}

	}	
    return res;
}

void btmtk_util_print_uuid128(char *uuid128)
{
    LOGI("[GAP] btmtk_util_print_uuid128 : %02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x", 
        uuid128[0], uuid128[1], uuid128[2], uuid128[3], uuid128[4], uuid128[5], uuid128[6], uuid128[7], 
        uuid128[8], uuid128[9], uuid128[10], uuid128[11], uuid128[12], uuid128[13], uuid128[14], uuid128[15]);
}

void btmtk_util_convert_bdaddr2string(char *dest, bt_addr_struct *source)
{
    sprintf(dest, "%02X:%02X:%02X:%02X:%02X:%02X",
            (U8) ((source->nap & 0xFF00) >> 8),
            (U8) (source->nap & 0x00FF),
            source->uap,
            (U8) ((source->lap & 0x00FF0000) >> 16),
            (U8) ((source->lap & 0x0000FF00) >> 8),
            (U8) (source->lap & 0x000000FF));
}

void btmtk_util_convert_string2bdaddr(const char *source, bt_addr_struct *dest)
{
    unsigned char addr[6];
    int i;
    char *ptr = (char*)source;
    
    //assert(dest != NULL && source != NULL);
    LOGI("convert from %s", ptr);
    for (i = 5; i >= 0; i--)
    {
        addr[i] = strtoul(ptr, &ptr, 16);
        ptr++;
        LOGI("convert to addr[%d]=0x%X", i, addr[i]);
    }
    dest->lap = addr[0] + (addr[1] << 8) + (addr[2] << 16);
    dest->uap = addr[3];
    dest->nap = addr[4] + (addr[5] << 8);
    LOGI("[API] btmtk_util_convert_string2bdaddr: %s => (0x%06lX, 0x%02X, 0x%04X)", source, dest->lap, dest->uap, dest->nap);
}

void btmtk_util_convert_bdaddr2objpath(char *dest, bt_addr_struct *source)
{
    sprintf(dest, BTMTK_ANDROID_ADAPTER_PREFIX"/dev_%02X_%02X_%02X_%02X_%02X_%02X",
            (U8) ((source->nap & 0xFF00) >> 8),
            (U8) (source->nap & 0x00FF),
            source->uap,
            (U8) ((source->lap & 0x00FF0000) >> 16),
            (U8) ((source->lap & 0x0000FF00) >> 8),
            (U8) (source->lap & 0x000000FF));	
}

void btmtk_util_convert_objpath2bdaddr(const char *source, bt_addr_struct *dest)
{
    unsigned char addr[6];
    char *ptr;
    int i;
    int adapterlen;

    adapterlen = strlen(BTMTK_ANDROID_ADAPTER_PREFIX);
    //assert(adapter && dest != NULL && source != NULL && adapterlen < strlen(source));

    /* pattern ($adapter)/dev_XX_XX_XX_XX_XX_XX */
    for(i = 0, ptr= (char*)source + adapterlen + 5; i < 6; i++, ptr++)
    {
        addr[i] = strtoul(ptr, &ptr, 16);
    }
    dest->lap = addr[5] + (addr[4] << 8) + (addr[3] << 16);
    dest->uap = addr[2];
    dest->nap = addr[1] + (addr[0] << 8);	
}

//SH : add for unaligned ntos
unsigned short pntohs(char *arr)
{
    LOGI("pntohs(0x%02X%02X) : addr=%p", arr[0], arr[1], arr);
    if( (((U32)arr) & 0x1) )
    {
        unsigned short tmp;
        LOGI("pntohs : use tmp");
        memcpy(&tmp, arr, sizeof(unsigned short));
        return ntohs(tmp);
    }
    else
    {
        return ntohs(*(unsigned short*)arr);
    }
}

unsigned long pntohl(char *arr)
{
    LOGI("pntohl(0x%02X%02X%02X%02X) : addr=%p", arr[0], arr[1], arr[2], arr[3], arr);
    if( (((U32)arr) & 0x3) )
    {
        unsigned long tmp;
        LOGI("pntohl : use tmp");
        memcpy(&tmp, arr, sizeof(unsigned long));
        return ntohl(tmp);
    }
    else
    {
        return ntohl(*(unsigned long*)arr);
    }
}

unsigned short phtons(char *arr)
{
    LOGI("phtons(0x%02X%02X) : addr=%p", arr[0], arr[1], arr);
    if( (((U32)arr) & 0x1) )
    {
        unsigned short tmp;
        LOGI("phtons : use tmp");
        memcpy(&tmp, arr, sizeof(unsigned short));
        return htons(tmp);
    }
    else
    {
        return htons(*(unsigned short*)arr);
    }
}

unsigned long phtonl(char *arr)
{
    LOGI("phtonl(0x%02X%02X%02X%02X) : addr=%p", arr[0], arr[1], arr[2], arr[3], arr);
    if( (((U32)arr) & 0x3) )
    {
        unsigned long tmp;
        LOGI("phtonl : use tmp");
        memcpy(&tmp, arr, sizeof(unsigned long));
        return htonl(tmp);
    }
    else
    {
        return htonl(*(unsigned long*)arr);
    }
}

static char bluetooth_base_uuid[BTMTK_SDP_UUID_128_BIT_SIZE] = {
    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x00, 0x80, 0x00, 0x00, 0x80, 0x5F, 0x9B, 0x34, 0xFB
};

bool btmtk_util_is_assigned_uuid(char *uuid128)
{
    return (memcmp(&bluetooth_base_uuid[4], &uuid128[4], 12) == 0);
}

void btmtk_util_convert_uuid16_2_uuid128(char *uuid128, unsigned short uuid16)
{
    //unsigned short *data;
    LOGI("[GAP] +btmtk_util_convert_uuid16_2_uuid128(0x%x)", uuid16);
    memcpy(uuid128, bluetooth_base_uuid, BTMTK_SDP_UUID_128_BIT_SIZE);
    uuid16 += pntohs(&uuid128[2]);
    uuid16 = htons(uuid16);
    memcpy(&uuid128[2], &uuid16, 2);
    btmtk_util_print_uuid128(uuid128);
    //data = (unsigned short *)&uuid128[2];
    //*data = *data + htons(uuid16);
}

unsigned short btmtk_util_convert_uuid128_2_uuid16(char *uuid128)
{
    unsigned short *data0 = (unsigned short *)&uuid128[2], *data1 = (unsigned short *)&bluetooth_base_uuid[2];
    LOGI("btmtk_util_convert_uuid128_2_uuid16: 0x%X, 0x%X", *data0, *data1);
    return (pntohs(&uuid128[2]) - pntohs(&bluetooth_base_uuid[2]));
}

void btmtk_util_convert_uuid128_2_string(char *buf, char *uuid128)
{
    LOGI("[GAP] btmtk_util_convert_uuid128_2_string");
    btmtk_util_print_uuid128(uuid128);
    snprintf(buf, BTMTK_MAX_UUID_STR_SIZE, "%.8lx-%.4x-%.4x-%.4x-%.8lx%.4x",
             pntohl(&uuid128[0]), pntohs(&uuid128[4]),
             pntohs(&uuid128[6]), pntohs(&uuid128[8]),
             pntohl(&uuid128[10]), pntohs(&uuid128[14]));
    LOGI("[GAP] btmtk_util_convert_uuid128_2_string : %s", buf);
}

void btmtk_util_convert_juuid_2_uuid128(char *buf, long long msb, long long lsb)
{
    char tmp[16];
    int i;
    LOGI("[GAP] +btmtk_util_convert_juuid_2_uuid128(%llx, %llx)", msb, lsb);
    memcpy(tmp, &lsb, 8);
    memcpy(tmp + 8, &msb, 8);
    for (i = 0; i < 16; i++)
    {
        buf[i] = tmp[15 - i];
    }
    LOGI("[GAP] -btmtk_util_convert_juuid_2_uuid128 : %p", buf);
}

void btmtk_util_convert_string_2_uuid128(char *uuid128, const char *buf)
{
#if 0
    unsigned int   data0;
    unsigned short data1;
    unsigned short data2;
    unsigned short data3;
    unsigned int   data4;
    unsigned short data5;
    unsigned int   *data0 = (unsigned int *)&uuid128[0];
    unsigned short *data1 = (unsigned short *)&uuid128[4];
    unsigned short *data2 = (unsigned short *)&uuid128[6];
    unsigned short *data3 = (unsigned short *)&uuid128[8];
    unsigned int   *data4 = (unsigned int *)&uuid128[10];
    unsigned short *data5 = (unsigned short *)&uuid128[14];
#endif
    //sscanf(buf, "%8x-%4x-%4x-%4x-%8x%4x", &data0, &data1, &data2, &data3, &data4, &data5);
    char *pch = (char *)buf, *endch = NULL, ch;
    unsigned long value_n, value_h, i = 0;
    char tmp[BTMTK_SDP_UUID_128_BIT_SIZE];

    LOGI("[GAP] btmtk_util_convert_string_2_uuid128 : %s", buf);
    memset(tmp, 0x0, BTMTK_SDP_UUID_128_BIT_SIZE);
    value_n = strtoul(pch, &endch, 16);
    value_h = pntohl((char *)&value_n);
    memcpy(tmp, &value_h, 4);
    LOGI("[GAP] strtoul: 0x%08lx", value_n);

    pch = endch + 1;
    value_n = strtoul(pch, &endch, 16);
    value_h = pntohs((char *)&value_n);
    memcpy(tmp + 4, &value_h, 2);
    LOGI("[GAP] strtoul: 0x%04lx", value_n);

    pch = endch + 1;
    value_n = strtoul(pch, &endch, 16);
    value_h = pntohs((char *)&value_n);
    memcpy(tmp + 6, &value_h, 2);
    LOGI("[GAP] strtoul: 0x%04lx", value_n);

    pch = endch + 1;
    value_n = strtoul(pch, &endch, 16);
    value_h = pntohs((char *)&value_n);
    memcpy(tmp + 8, &value_h, 2);
    LOGI("[GAP] strtoul: 0x%04lx", value_n);

    pch = endch + 1;
    ch = *(pch + 8);
    *(pch + 8) = '\0';
    value_n = strtoul(pch, &endch, 16);
    value_h = pntohl((char *)&value_n);
    memcpy(tmp + 10, &value_h, 4);
    LOGI("[GAP] strtoul: 0x%08lx", value_n);
    *(pch + 8) = ch;

    pch = endch;
    value_n = strtoul(pch, &endch, 16);
    value_h = pntohs((char *)&value_n);
    memcpy(tmp + 14, &value_h, 2);
    LOGI("[GAP] strtoul: 0x%04lx", value_n);

    while (i < BTMTK_SDP_UUID_128_BIT_SIZE)
    {
        uuid128[i] = tmp[i];
        i++;
    }
    btmtk_util_print_uuid128(uuid128);
}

int btmtk_util_convert_uuidlist_2_uuid16( unsigned long service_list1,  /* 0x1100 ~ 0x111F */
    												unsigned long  service_list2,  /* 0x1120 ~ 0x113F */
    												unsigned long  service_list3,  /* 0x1200 ~ 0x121F */
    												unsigned long  service_list4,  /* 0x1300~ */
    												unsigned long  service_list5,
    												unsigned long  service_list6,
    												unsigned short uuid[])
{
	U8 i, idx = 0;
	if (uuid == NULL) {
		return 0;
	}
	
	for (i = 0; i < BTBM_ADP_MAX_SDAP_UUID_NO && idx < 32 * 6; idx++)
	{
		if (idx < 32)
		{
			if ((service_list1 >> idx) & 0x1)
			{
				uuid[i] = 0x1100 | idx;
				LOGI("[GAP] uuid[%d] = 0x%x", i, uuid[i]);
				i++;
			}
		}
		else if (idx < 64)
		{
			int bit = idx - 32;
			if ((service_list2 >> bit) & 0x1)
			{
				uuid[i] = 0x1120 | bit;
				LOGI("[GAP] uuid[%d] = 0x%x", i, uuid[i]);
				i++;
			}
		}
		else if (idx < 96)
		{
			int bit = idx - 64;
			if ((service_list3 >> bit) & 0x1)
			{
				uuid[i] = 0x1200 | bit;
				LOGI("[GAP] uuid[%d] = 0x%x", i, uuid[i]);
				i++;
			}
		}
		else if (idx < 128)
		{
			int bit = idx - 96;
			if ((service_list4 >> bit) & 0x1)
			{
				uuid[i] = 0x1300 | bit;
				LOGI("[GAP] uuid[%d] = 0x%x", i, uuid[i]);
				i++;
			}
		}
		else if (idx < 160)
		{
			int bit = idx - 128;
			if ((service_list5 >> bit) & 0x1)
			{
				uuid[i] = 0x1400 | bit;
				LOGI("[GAP] entry.uuid[%d] = 0x%x", i, uuid[i]);
				i++;
			}
		}
                else if (idx < 192)
                {
                         int bit = idx - 160;
                         if ((service_list6 >> bit) & 0x1)
                         {
                              uuid[i] = 0x1800 | bit;
                              LOGI("[GAP] uuid[%d] = 0x%x", i, uuid[i]);
                              i++;
                           }
                 }
	}
	return i;
}



bool btmtk_util_equal_bdaddr(bt_addr_struct *addr1, bt_addr_struct *addr2)
{
    return ((addr1->lap == addr2->lap) && (addr1->uap == addr2->uap) && (addr1->nap == addr2->nap));
}

void btmtk_util_list_append(btmtk_list_header_struct **list, btmtk_list_header_struct *node)
{
    btmtk_list_header_struct *ptr = *list;

    if (*list == NULL)
    {
        *list = node;
    }
    else
    {
        while (ptr->next)
        {
            ptr = ptr->next;
        }
        ptr->next = (struct _btmtk_list_header_struct *)node;
    }
}

void btmtk_util_list_remove(btmtk_list_header_struct **list, btmtk_list_header_struct *node)
{
    btmtk_list_header_struct *ptr = *list;

    if (node == NULL)
    {
        return;
    }
    
    if (list == NULL || *list == NULL)
    {
    }
    else if (*list == node)
    {
        *list = (btmtk_list_header_struct *)ptr->next;
    }
    else
    {
        while (ptr->next)
        {
            if (ptr->next == (struct _btmtk_list_header_struct *)node)
            {
                ptr->next = ptr->next->next;
                break;
            }
            ptr = ptr->next;
        }
    }
    free(node);
}

btmtk_sdp_req_struct *btmtk_util_find_sdp_channel_request(event_loop_native_data_t *nat, bt_addr_struct *addr, unsigned short uuid, unsigned short attrid)
{
    btmtk_sdp_req_struct *ptr = nat->requests;
    LOGI("[GAP] btmtk_util_find_sdp_channel_request search 0x%lX:0x%X:0x%X: uudi=0x%X", addr->lap, addr->uap, addr->nap, uuid);
    while (ptr)
    {
        LOGI("[GAP] btmtk_util_find_sdp_channel_request found 0x%lX:0x%X:0x%X", ptr->addr.lap, ptr->addr.uap, ptr->addr.nap);
        if (btmtk_util_equal_bdaddr(&ptr->addr, addr) && 
            ptr->type == BTMTK_SDP_DISCOVERY_CHANNEL &&
            ptr->uuid[0] == uuid && ptr->uuid[1] == attrid)
        {
            return ptr;
        }
        ptr = ptr->next;
    }
    return NULL;
}


btmtk_device_entry_struct *btmtk_paired_dev_cache_find(event_loop_native_data_t *nat, bt_addr_struct *addr)
{
    int i;

    LOGI("[GAP] btmtk_paired_dev_cache_find: addr=0x%lX:0x%X:0x%X", addr->lap, addr->uap, addr->nap);
    for (i = 0; i < nat->paired_cache_no; i++)
    {
        if (btmtk_util_equal_bdaddr(&nat->paired_dev_cache[i].addr, addr))
        {
            LOGI("[GAP] btmtk_paired_dev_cache_find found");
            return &nat->paired_dev_cache[i];
        }
    }
    LOGW("[GAP] btmtk_paired_dev_cache_find not found");
    return NULL;
}

btmtk_device_entry_struct *btmtk_inquired_dev_cache_find(event_loop_native_data_t *nat, bt_addr_struct *addr)
{
    int i;

    LOGI("[GAP] btmtk_inquired_dev_cache_find: addr=0x%lX:0x%X:0x%X", addr->lap, addr->uap, addr->nap);
    for (i = 0; i < nat->inquired_cache_no; i++)
    {
        if (btmtk_util_equal_bdaddr(&nat->inquired_dev_cache[i].addr, addr))
        {
            LOGI("[GAP] btmtk_inquired_dev_cache_find found");
            return &nat->inquired_dev_cache[i];
        }
    }
    LOGW("[GAP] btmtk_inquired_dev_cache_find not found");
    return NULL;
}

int btmtk_hdp_util_convert_result(U8 result)
{
    switch(result)
    {
	case BTBM_ADP_SUCCESS:
	    return HEALTH_OPERATION_SUCCESS;
	case BTBM_ADP_FAILED:
	    return HEALTH_OPERATION_GENERIC_FAILURE;
    	case BTBM_ADP_FAIL_NOT_ALLOW:
	    return HEALTH_OPERATION_NOT_ALLOWED;
	case BTBM_ADP_FAIL_BUSY:
    	case BTBM_ADP_FAIL_TIMEOUT:
   		case BTBM_ADP_FAIL_KEYERR:
    	case BTBM_ADP_FAIL_REMOTE_REJECT:
		default:
			return HEALTH_OPERATION_GENERIC_FAILURE;
	}
}



#endif


} /* namespace android */
