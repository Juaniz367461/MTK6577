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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <ctype.h>
#include <signal.h>
#include <sys/wait.h>
#include <sys/mount.h>
#include <sys/stat.h>
#include <sys/poll.h>
#include <errno.h>
#include <stdarg.h>
#include <mtd/mtd-user.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/un.h>
#include <libgen.h>

#include <cutils/list.h>
#include <cutils/sockets.h>
#include <cutils/iosched_policy.h>
#include <cutils/xlog.h>
#include <private/android_filesystem_config.h>
#include <termios.h>

#include <sys/system_properties.h>

#include "devices.h"
#include "init.h"
#include "log.h"
#include "property_service.h"
#include "bootchart.h"
#include "signal_handler.h"
#include "keychords.h"
#include "init_parser.h"
#include "util.h"
#include "ueventd.h"

#define LOG_TAG "init"

static int property_triggers_enabled = 0;

#if BOOTCHART
static int   bootchart_count;
#endif

static char console[32];
static char serialno[32];
static char bootmode[32];
static char baseband[32];
static char carrier[32];
static char bootloader[32];
static char hardware[32];
static char modelno[32];
static unsigned revision = 0;
static char qemu[32];

static struct action *cur_action = NULL;
static struct command *cur_command = NULL;
static struct listnode *command_queue = NULL;

void notify_service_state(const char *name, const char *state)
{
    char pname[PROP_NAME_MAX];
    int len = strlen(name);
    if ((len + 10) > PROP_NAME_MAX)
        return;
    snprintf(pname, sizeof(pname), "init.svc.%s", name);
    property_set(pname, state);
}

static int have_console;
static char *console_name = "/dev/console";
static time_t process_needs_restart;

static const char *ENV[32];

/* add_environment - add "key=value" to the current environment */
int add_environment(const char *key, const char *val)
{
    int n;

    for (n = 0; n < 31; n++) {
        if (!ENV[n]) {
            size_t len = strlen(key) + strlen(val) + 2;
            char *entry = malloc(len);
            snprintf(entry, len, "%s=%s", key, val);
            ENV[n] = entry;
            return 0;
        }
    }

    return 1;
}

static void zap_stdio(void)
{
    int fd;
    fd = open("/dev/null", O_RDWR);
    dup2(fd, 0);
    dup2(fd, 1);
    dup2(fd, 2);
    close(fd);
}

static void open_console()
{
    int fd;
    if ((fd = open(console_name, O_RDWR)) < 0) {
        fd = open("/dev/null", O_RDWR);
    }
    dup2(fd, 0);
    dup2(fd, 1);
    dup2(fd, 2);
    close(fd);
}

static void publish_socket(const char *name, int fd)
{
    char key[64] = ANDROID_SOCKET_ENV_PREFIX;
    char val[64];

    strlcpy(key + sizeof(ANDROID_SOCKET_ENV_PREFIX) - 1,
            name,
            sizeof(key) - sizeof(ANDROID_SOCKET_ENV_PREFIX));
    snprintf(val, sizeof(val), "%d", fd);
    add_environment(key, val);

    /* make sure we don't close-on-exec */
    fcntl(fd, F_SETFD, 0);
}

void service_start(struct service *svc, const char *dynamic_args)
{
    struct stat s;
    pid_t pid;
    int needs_console;
    int n;

        /* starting a service removes it from the disabled or reset
         * state and immediately takes it out of the restarting
         * state if it was in there
         */
    svc->flags &= (~(SVC_DISABLED|SVC_RESTARTING|SVC_RESET));
    svc->time_started = 0;

        /* running processes require no additional work -- if
         * they're in the process of exiting, we've ensured
         * that they will immediately restart on exit, unless
         * they are ONESHOT
         */
    if (svc->flags & SVC_RUNNING) {
        return;
    }

    needs_console = (svc->flags & SVC_CONSOLE) ? 1 : 0;
    if (needs_console && (!have_console)) {
        ERROR("service '%s' requires console\n", svc->name);
        svc->flags |= SVC_DISABLED;
        return;
    }

    if (stat(svc->args[0], &s) != 0) {
        ERROR("cannot find '%s', disabling '%s'\n", svc->args[0], svc->name);
        svc->flags |= SVC_DISABLED;
        return;
    }

    if ((!(svc->flags & SVC_ONESHOT)) && dynamic_args) {
        ERROR("service '%s' must be one-shot to use dynamic args, disabling\n",
               svc->args[0]);
        svc->flags |= SVC_DISABLED;
        return;
    }

    NOTICE("starting '%s'\n", svc->name);

    pid = fork();

    if (pid == 0) {
        struct socketinfo *si;
        struct svcenvinfo *ei;
        char tmp[32];
        int fd, sz;

        if (properties_inited()) {
            get_property_workspace(&fd, &sz);
            sprintf(tmp, "%d,%d", dup(fd), sz);
            add_environment("ANDROID_PROPERTY_WORKSPACE", tmp);
        }

        for (ei = svc->envvars; ei; ei = ei->next)
            add_environment(ei->name, ei->value);

        for (si = svc->sockets; si; si = si->next) {
            int socket_type = (
                    !strcmp(si->type, "stream") ? SOCK_STREAM :
                        (!strcmp(si->type, "dgram") ? SOCK_DGRAM : SOCK_SEQPACKET));
            int s = create_socket(si->name, socket_type,
                                  si->perm, si->uid, si->gid);
            if (s >= 0) {
                publish_socket(si->name, s);
            }
        }

        if (svc->ioprio_class != IoSchedClass_NONE) {
            if (android_set_ioprio(getpid(), svc->ioprio_class, svc->ioprio_pri)) {
                ERROR("Failed to set pid %d ioprio = %d,%d: %s\n",
                      getpid(), svc->ioprio_class, svc->ioprio_pri, strerror(errno));
            }
        }

        if (needs_console) {
            setsid();
            open_console();
        } else {
            zap_stdio();
        }

#if 0
        for (n = 0; svc->args[n]; n++) {
            INFO("args[%d] = '%s'\n", n, svc->args[n]);
        }
        for (n = 0; ENV[n]; n++) {
            INFO("env[%d] = '%s'\n", n, ENV[n]);
        }
#endif

        setpgid(0, getpid());

    /* as requested, set our gid, supplemental gids, and uid */
        if (svc->gid) {
            if (setgid(svc->gid) != 0) {
                ERROR("setgid failed: %s\n", strerror(errno));
                _exit(127);
            }
        }
        if (svc->nr_supp_gids) {
            if (setgroups(svc->nr_supp_gids, svc->supp_gids) != 0) {
                ERROR("setgroups failed: %s\n", strerror(errno));
                _exit(127);
            }
        }
        if (svc->uid) {
            if (setuid(svc->uid) != 0) {
                ERROR("setuid failed: %s\n", strerror(errno));
                _exit(127);
            }
        }

        if (!dynamic_args) {
            if (execve(svc->args[0], (char**) svc->args, (char**) ENV) < 0) {
                ERROR("cannot execve('%s'): %s\n", svc->args[0], strerror(errno));
            }
        } else {
            char *arg_ptrs[INIT_PARSER_MAXARGS+1];
            int arg_idx = svc->nargs;
            char *tmp = strdup(dynamic_args);
            char *next = tmp;
            char *bword;

            /* Copy the static arguments */
            memcpy(arg_ptrs, svc->args, (svc->nargs * sizeof(char *)));

            while((bword = strsep(&next, " "))) {
                arg_ptrs[arg_idx++] = bword;
                if (arg_idx == INIT_PARSER_MAXARGS)
                    break;
            }
            arg_ptrs[arg_idx] = '\0';
            execve(svc->args[0], (char**) arg_ptrs, (char**) ENV);
        }
        _exit(127);
    }

    if (pid < 0) {
        ERROR("failed to start '%s'\n", svc->name);
        svc->pid = 0;
        return;
    }

    svc->time_started = gettime();
    svc->pid = pid;
    svc->flags |= SVC_RUNNING;

    if (properties_inited())
        notify_service_state(svc->name, "running");
}

/* The how field should be either SVC_DISABLED or SVC_RESET */
static void service_stop_or_reset(struct service *svc, int how)
{
        /* we are no longer running, nor should we
         * attempt to restart
         */
    svc->flags &= (~(SVC_RUNNING|SVC_RESTARTING));

    if ((how != SVC_DISABLED) && (how != SVC_RESET)) {
        /* Hrm, an illegal flag.  Default to SVC_DISABLED */
        how = SVC_DISABLED;
    }
        /* if the service has not yet started, prevent
         * it from auto-starting with its class
         */
    if (how == SVC_RESET) {
        svc->flags |= (svc->flags & SVC_RC_DISABLED) ? SVC_DISABLED : SVC_RESET;
    } else {
        svc->flags |= how;
    }

    if (svc->pid) {
        NOTICE("service '%s' is being killed\n", svc->name);
        kill(-svc->pid, SIGKILL);
        notify_service_state(svc->name, "stopping");
    } else {
        notify_service_state(svc->name, "stopped");
    }
}

void service_reset(struct service *svc)
{
    service_stop_or_reset(svc, SVC_RESET);
}

void service_stop(struct service *svc)
{
    service_stop_or_reset(svc, SVC_DISABLED);
}

void property_changed(const char *name, const char *value)
{
    if (property_triggers_enabled)
        queue_property_triggers(name, value);
}

static void restart_service_if_needed(struct service *svc)
{
    time_t next_start_time = svc->time_started + 5;

    if (next_start_time <= gettime()) {
        svc->flags &= (~SVC_RESTARTING);
        service_start(svc, NULL);
        return;
    }

    if ((next_start_time < process_needs_restart) ||
        (process_needs_restart == 0)) {
        process_needs_restart = next_start_time;
    }
}

static void restart_processes()
{
    process_needs_restart = 0;
    service_for_each_flags(SVC_RESTARTING,
                           restart_service_if_needed);
}

static void msg_start(const char *name)
{
    struct service *svc;
    char *tmp = NULL;
    char *args = NULL;

    if (!strchr(name, ':'))
        svc = service_find_by_name(name);
    else {
        tmp = strdup(name);
        args = strchr(tmp, ':');
        *args = '\0';
        args++;

        svc = service_find_by_name(tmp);
    }

    if (svc) {
        service_start(svc, args);
    } else {
        ERROR("no such service '%s'\n", name);
    }
    if (tmp)
        free(tmp);
}

static void msg_stop(const char *name)
{
    struct service *svc = service_find_by_name(name);

    if (svc) {
        service_stop(svc);
    } else {
        ERROR("no such service '%s'\n", name);
    }
}

void handle_control_message(const char *msg, const char *arg)
{
    if (!strcmp(msg,"start")) {
        msg_start(arg);
    } else if (!strcmp(msg,"stop")) {
        msg_stop(arg);
    } else if (!strcmp(msg,"restart")) {
        msg_stop(arg);
        msg_start(arg);
    } else if (!strcmp(msg,"class_reset")) {
		service_for_each_class(arg, service_reset);
    } else {
        ERROR("unknown control msg '%s'\n", msg);
    }
}

static void import_kernel_nv(char *name, int in_qemu)
{
    char *value = strchr(name, '=');

    if (value == 0) return;
    *value++ = 0;
    if (*name == 0) return;

    if (!in_qemu)
    {
        /* on a real device, white-list the kernel options */
        if (!strcmp(name,"qemu")) {
            strlcpy(qemu, value, sizeof(qemu));
        } else if (!strcmp(name,"androidboot.console")) {
            strlcpy(console, value, sizeof(console));
        } else if (!strcmp(name,"androidboot.mode")) {
            strlcpy(bootmode, value, sizeof(bootmode));
        } else if (!strcmp(name,"androidboot.serialno")) {
            strlcpy(serialno, value, sizeof(serialno));
        } else if (!strcmp(name,"androidboot.baseband")) {
            strlcpy(baseband, value, sizeof(baseband));
        } else if (!strcmp(name,"androidboot.carrier")) {
            strlcpy(carrier, value, sizeof(carrier));
        } else if (!strcmp(name,"androidboot.bootloader")) {
            strlcpy(bootloader, value, sizeof(bootloader));
        } else if (!strcmp(name,"androidboot.hardware")) {
            strlcpy(hardware, value, sizeof(hardware));
        } else if (!strcmp(name,"androidboot.modelno")) {
            strlcpy(modelno, value, sizeof(modelno));
        }
    } else {
        /* in the emulator, export any kernel option with the
         * ro.kernel. prefix */
        char  buff[32];
        int   len = snprintf( buff, sizeof(buff), "ro.kernel.%s", name );
        if (len < (int)sizeof(buff)) {
            property_set( buff, value );
        }
    }
}

static struct command *get_first_command(struct action *act)
{
    struct listnode *node;
    node = list_head(&act->commands);
    if (!node || list_empty(&act->commands))
        return NULL;

    return node_to_item(node, struct command, clist);
}

static struct command *get_next_command(struct action *act, struct command *cmd)
{
    struct listnode *node;
    node = cmd->clist.next;
    if (!node)
        return NULL;
    if (node == &act->commands)
        return NULL;

    return node_to_item(node, struct command, clist);
}

static int is_last_command(struct action *act, struct command *cmd)
{
    return (list_tail(&act->commands) == &cmd->clist);
}

void execute_one_command(void)
{
    int ret;

    if (!cur_action || !cur_command || is_last_command(cur_action, cur_command)) {
        cur_action = action_remove_queue_head();
        cur_command = NULL;
        if (!cur_action)
            return;
        INFO("processing action %p (%s)\n", cur_action, cur_action->name);
        cur_command = get_first_command(cur_action);
    } else {
        cur_command = get_next_command(cur_action, cur_command);
    }

    if (!cur_command)
        return;

    ret = cur_command->func(cur_command->nargs, cur_command->args);
    INFO("command '%s' r=%d\n", cur_command->args[0], ret);
}

static int wait_for_coldboot_done_action(int nargs, char **args)
{
    int ret;
    INFO("wait for %s\n", coldboot_done);
    ret = wait_for_file(coldboot_done, COMMAND_RETRY_TIMEOUT);
    if (ret)
        ERROR("Timed out waiting for %s\n", coldboot_done);
    return ret;
}

static int property_init_action(int nargs, char **args)
{
    bool load_defaults = true;

    INFO("property init\n");
    if (!strcmp(bootmode, "charger"))
        load_defaults = false;
    property_init(load_defaults);
    return 0;
}

static int keychord_init_action(int nargs, char **args)
{
    keychord_init();
    return 0;
}

static int console_init_action(int nargs, char **args)
{
    int fd;
    char tmp[PROP_VALUE_MAX];

    if (console[0]) {
        snprintf(tmp, sizeof(tmp), "/dev/%s", console);
        console_name = strdup(tmp);
    }

    fd = open(console_name, O_RDWR);
    if (fd >= 0)
        have_console = 1;
    close(fd);

    if( load_565rle_image(INIT_IMAGE_FILE) ) {
        fd = open("/dev/tty0", O_WRONLY);
        if (fd >= 0) {
            const char *msg;
                msg = "\n"
            "\n"
            "\n"
            "\n"
            "\n"
            "\n"
            "\n"  // console is 40 cols x 30 lines
            "\n"
            "\n"
            "\n"
            "\n"
            "\n"
            "\n"
            "\n"
            "             A N D R O I D ";
            write(fd, msg, strlen(msg));
            close(fd);
        }
    }
    return 0;
}

static int read_serialno(char* serial_no)
{
    int fd;
    size_t s;

    fd = open("/sys/sys_info/serial_number", O_RDWR);
    if (fd < 0) {
        xlog_printf(ANDROID_LOG_WARN, LOG_TAG, "fail to open: %s\n", "/sys/sys_info/serial_number");
        return 0;
    }
    s = read(fd, serial_no, sizeof(char)*32);

    serial_no[s-1] = '\0';

    close(fd);

    if (s <= 0) {
	    xlog_printf(ANDROID_LOG_WARN, LOG_TAG, "could not read serial number sys file\n");
	    return 0;
	}

    xlog_printf(ANDROID_LOG_WARN, LOG_TAG, "serial number=%s\n",serial_no);

    return 1;
}

static int set_init_properties_action(int nargs, char **args)
{
    char tmp[PROP_VALUE_MAX];

    if (qemu[0])
        import_kernel_cmdline(1, import_kernel_nv);

    if (!strcmp(bootmode,"factory"))
        property_set("ro.factorytest", "1");
    else if (!strcmp(bootmode,"factory2"))
        property_set("ro.factorytest", "2");
    else
        property_set("ro.factorytest", "0");

    if(!serialno[0])
        read_serialno(serialno);

    property_set("ro.serialno", serialno[0] ? serialno : "0123456789ABCDEF");
    property_set("ro.bootmode", bootmode[0] ? bootmode : "unknown");
    property_set("ro.baseband", baseband[0] ? baseband : "unknown");
    property_set("ro.carrier", carrier[0] ? carrier : "unknown");
    property_set("ro.bootloader", bootloader[0] ? bootloader : "unknown");

    if (modelno[0])
        property_set("ro.boot.modelno", modelno);

    property_set("ro.hardware", hardware);
    snprintf(tmp, PROP_VALUE_MAX, "%d", revision);
    property_set("ro.revision", tmp);
/*    
#if defined(MTK_EMU_SUPPORT) || !defined(INIT_ENG_BUILD)
    property_set("ro.mtprof.disable", "1");
#endif
*/
    return 0;
}

static int property_service_init_action(int nargs, char **args)
{
    /* read any property files on system or data and
     * fire up the property service.  This must happen
     * after the ro.foo properties are set above so
     * that /data/local.prop cannot interfere with them.
     */
    start_property_service();
    return 0;
}

static int signal_init_action(int nargs, char **args)
{
    signal_init();
    return 0;
}

static int check_startup_action(int nargs, char **args)
{
    /* make sure we actually have all the pieces we need */
    if ((get_property_set_fd() < 0) ||
        (get_signal_fd() < 0)) {
        ERROR("init startup failure\n");
        exit(1);
    }

        /* signal that we hit this point */
    unlink("/dev/.booting");

    return 0;
}
static int queue_early_property_triggers_action()
{
    queue_early_property_triggers();
    return 0;
}

static int queue_property_triggers_action(int nargs, char **args)
{
    queue_all_property_triggers();
    /* enable property triggers */
    property_triggers_enabled = 1;
    return 0;
}

#if BOOTCHART
static int bootchart_init_action(int nargs, char **args)
{
    bootchart_count = bootchart_init();
    if (bootchart_count < 0) {
        ERROR("bootcharting init failure\n");
    } else if (bootchart_count > 0) {
        NOTICE("bootcharting started (period=%d ms)\n", bootchart_count*BOOTCHART_POLLING_MS);
    } else {
        NOTICE("bootcharting ignored\n");
    }
}
#endif

#ifdef USE_BUILT_IN_FACTORY
static int is_factory_boot(void)
{
    int fd;
    size_t s;
    char boot_mode;

    fd = open("/sys/class/BOOT/BOOT/boot/boot_mode", O_RDWR);
    if (fd < 0) {
        printf("fail to open: %s\n", "/sys/class/BOOT/BOOT/boot/boot_mode");
        return 0;
    }
    s = read(fd, (void *)&boot_mode, sizeof(boot_mode));

    close(fd);

    if(s <= 0){
	ERROR("could not read boot mode sys file\n");
    return 0;
}
    // Factory Mode, '4'
    // ATE Factory Mode, '6'	
    if ((boot_mode != '4') && (boot_mode != '6')){
	ERROR("Unsupported factory mode\n");
        return 0;
    }

    printf("Factory Mode Booting.....\n");

    return 1;
}
#endif

static int is_meta_boot(void)
{
  int fd;
  size_t s;
  char boot_mode;
  
  fd = open("/sys/class/BOOT/BOOT/boot/boot_mode", O_RDWR);
  if (fd < 0) 
  {
    printf("fail to open: %s\n", "/sys/class/BOOT/BOOT/boot/boot_mode");
    return 0;
  }
  
  s = read(fd, (void *)&boot_mode, sizeof(boot_mode));
  close(fd);
  
  if(s <= 0)
  {
	  ERROR("could not read boot mode sys file\n");
    return 0;
  }
  
  if ((boot_mode != '1'))
  {
	  printf("Current boot_mode is Not meta mode\n");
    return 0;
  }

  printf("META Mode Booting.....\n");
  return 1;  
}

static int is_advanced_meta_boot(void)
{
  int fd;
  size_t s;
  char boot_mode;
  
  fd = open("/sys/class/BOOT/BOOT/boot/boot_mode", O_RDWR);
  if (fd < 0) 
  {
    printf("fail to open: %s\n", "/sys/class/BOOT/BOOT/boot/boot_mode");
    return 0;
  }
  
  s = read(fd, (void *)&boot_mode, sizeof(boot_mode));
  close(fd);
  
  if(s <= 0)
  {
	  ERROR("could not read boot mode sys file\n");
    return 0;
  }
  
  if ((boot_mode != '5'))
  {
	  printf("Current boot_mode is Not advanced meta mode\n");
    return 0;
  }

  printf("ADVANCED META Mode Booting.....\n");
  return 1;
}

#ifdef MTK_SHARED_SDCARD
static int is_support_sdcard_share_boot(void)
{
  int fd;
  size_t s;
  char boot_mode;
/***************************

    NORMAL_BOOT = 0,
    META_BOOT = 1,
    RECOVERY_BOOT = 2,    
    SW_REBOOT = 3,
    FACTORY_BOOT = 4,
    ADVMETA_BOOT = 5,
    ATE_FACTORY_BOOT = 6,
    ALARM_BOOT = 7,
    UNKNOWN_BOOT
****************************/
  
  fd = open("/sys/class/BOOT/BOOT/boot/boot_mode", O_RDWR);
  if (fd < 0) 
  {
    printf("fail to open: %s\n", "/sys/class/BOOT/BOOT/boot/boot_mode");
    return 0;
  }
  
  s = read(fd, (void *)&boot_mode, sizeof(boot_mode));
  close(fd);
  
  if(s <= 0)
  {
	  ERROR("could not read boot mode sys file\n");
    return 0;
  }
  
  if ((boot_mode == '2') || (boot_mode == '7') || (boot_mode == '8'))
  {
	  printf("Current boot_mode is Not Support sdcard share\n");
    return 0;
  }

  printf("The Booting is support sdcard shared, sdcard daemon will start.....\n");
  return 1;  
}
#endif
static int get_boot_mode(void)
{
  int fd;
  size_t s;
  char boot_mode;

  fd = open("/sys/class/BOOT/BOOT/boot/boot_mode", O_RDWR);
  if (fd < 0)
  {
    printf("fail to open: %s\n", "/sys/class/BOOT/BOOT/boot/boot_mode");
    return 0;
  }

  s = read(fd, (void *)&boot_mode, sizeof(boot_mode));
  close(fd);

  if(s <= 0)
  {
	ERROR("could not read boot mode sys file\n");
    return 0;
  }

  return atoi(&boot_mode);
}

static int get_meta_com_id(void)
{
  int fd;
  size_t s;
  char com_id;

  fd = open("/sys/bus/platform/drivers/meta_com_id_info/meta_com_id_info", O_RDWR);
  if (fd < 0) {
      printf("fail to open: %s\n", "/sys/bus/platform/drivers/meta_com_id_info/meta_com_id_info");
      return 0;
  }

  s = read(fd, (void *)&com_id, sizeof(com_id));
  close(fd);

  if (s <= 0) {
      printf("could not read com id sys file\n");
      return 0;
  }

  return atoi(&com_id);
}

static int queue_com_triggers_action()
{
    int boot_mode;
    int meta_com_id;

    boot_mode = get_boot_mode();
    meta_com_id = get_meta_com_id();

    /* If enter META mode and met_com_id == 1, enable single com mode
     * If enter ATE FACTORY mode, enable single com mode
     * Else andble compostie USB mode
     * Please refernce meta_inti.rc and init.factory.rc
     */

    if ((boot_mode == 1 && meta_com_id == 1)
        || (boot_mode == 6)) {
        property_set("sys.usb.com_config", "1");
    } else {
        property_set("sys.usb.com_config", "0");
    }
    return 0;
}

int main(int argc, char **argv)
{
    int fd_count = 0;
    struct pollfd ufds[4];
    char *tmpdev;
    char* debuggable;
    char tmp[32];
    int property_set_fd_init = 0;
    int signal_fd_init = 0;
    int keychord_fd_init = 0;

    if (!strcmp(basename(argv[0]), "ueventd"))
        return ueventd_main(argc, argv);

    /* clear the umask */
    umask(0);

        /* Get the basic filesystem setup we need put
         * together in the initramdisk on / and then we'll
         * let the rc file figure out the rest.
         */
    mkdir("/dev", 0755);
    mkdir("/proc", 0755);
    mkdir("/sys", 0755);

    mount("tmpfs", "/dev", "tmpfs", MS_NOSUID, "mode=0755");
    mkdir("/dev/pts", 0755);
    mkdir("/dev/socket", 0755);
    mount("devpts", "/dev/pts", "devpts", 0, NULL);
    mount("proc", "/proc", "proc", 0, NULL);
    mount("sysfs", "/sys", "sysfs", 0, NULL);

#ifdef INIT_ENG_BUILD
    mount("debugfs", "/sys/kernel/debug", "debugfs", 0, NULL);
#endif

        /* indicate that booting is in progress to background fw loaders, etc */
    close(open("/dev/.booting", O_WRONLY | O_CREAT, 0000));

        /* We must have some place other than / to create the
         * device nodes for kmsg and null, otherwise we won't
         * be able to remount / read-only later on.
         * Now that tmpfs is mounted on /dev, we can actually
         * talk to the outside world.
         */
    //open_devnull_stdio();
    klog_init();

#ifdef HAVE_AEE_FEATURE    
    INFO("reading AEE config file\n");
#ifndef PARTIAL_BUILD
	init_parse_config_file("/init.aee.mtk.rc");
#else
	init_parse_config_file("/init.aee.customer.rc");
#endif // MTK_INTERNAL

#endif // HAVE_AEE_FEATURE
    INFO("reading config file\n");

#ifdef USE_BUILT_IN_FACTORY
    ERROR("USE_BUILT_IN_FACTORY");
    if (is_factory_boot()) 
    {
        ERROR("This is factory boot");
        if (init_parse_config_file("/init.factory.rc") < 0){
            init_parse_config_file("/init.rc");
            INFO("reading project config file\n");
            init_parse_config_file("/init.project.rc");
        }
    }
    else 
    {
        if(is_meta_boot())
        {
            ERROR("Parsing meta_init.rc ...\n");
            init_parse_config_file("/meta_init.rc");
            INFO("reading project config file\n");
            init_parse_config_file("/meta_init.project.rc");
        }  
        else if(is_advanced_meta_boot())
        {
            ERROR("Parsing advanced_meta_init.rc ...\n");
            init_parse_config_file("/advanced_meta_init.rc");
            INFO("reading project config file\n");
            init_parse_config_file("/advanced_meta_init.project.rc");
        }
        else
        {
            printf("Parsing init.rc ...\n");  
            init_parse_config_file("/init.rc");
            INFO("reading project config file\n");
            init_parse_config_file("/init.project.rc");
        }
    }
#else
    if(is_meta_boot())
    {
        ERROR("Parsing meta_init.rc ...\n");
        init_parse_config_file("/meta_init.rc");
        INFO("reading project config file\n");
        init_parse_config_file("/meta_init.project.rc");
    }  
    else if(is_advanced_meta_boot())
    {
        ERROR("Parsing advanced_meta_init.rc ...\n");
        init_parse_config_file("/advanced_meta_init.rc");
        INFO("reading project config file\n");
        init_parse_config_file("/advanced_meta_init.project.rc");
    }  
    else
    {
        printf("Parsing init.rc ...\n");  
        init_parse_config_file("/init.rc");
        INFO("reading project config file\n");
        init_parse_config_file("/init.project.rc");
    }
#endif

#ifdef MTK_SHARED_SDCARD
    if(is_support_sdcard_share_boot())
    {       	    
	printf("Parsing init.sdcard.rc ...\n");  
        init_parse_config_file("/init.sdcard.rc");
    }
#endif

    /* pull the kernel commandline and ramdisk properties file in */
    import_kernel_cmdline(0, import_kernel_nv);
    /* don't expose the raw commandline to nonpriv processes */
    chmod("/proc/cmdline", 0440);
    get_hardware_name(hardware, &revision);
    snprintf(tmp, sizeof(tmp), "/init.%s.rc", hardware);
    init_parse_config_file(tmp);

    action_for_each_trigger("early-init", action_add_queue_tail);

    queue_builtin_action(wait_for_coldboot_done_action, "wait_for_coldboot_done");
    queue_builtin_action(property_init_action, "property_init");
    queue_builtin_action(keychord_init_action, "keychord_init");
    queue_builtin_action(console_init_action, "console_init");
    queue_builtin_action(set_init_properties_action, "set_init_properties");

    /* execute all the boot actions to get us started */
    action_for_each_trigger("init", action_add_queue_tail);

    /* skip mounting filesystems in charger mode */
    if (strcmp(bootmode, "charger") != 0) {
        action_for_each_trigger("early-fs", action_add_queue_tail);
        action_for_each_trigger("fs", action_add_queue_tail);
        action_for_each_trigger("post-fs", action_add_queue_tail);
        action_for_each_trigger("post-fs-data", action_add_queue_tail);
    }

    queue_builtin_action(property_service_init_action, "property_service_init");
    queue_builtin_action(signal_init_action, "signal_init");
    queue_builtin_action(check_startup_action, "check_startup");

    queue_builtin_action(queue_early_property_triggers_action, "queue_early_propety_triggers");

    if (!strcmp(bootmode, "charger")) {
        action_for_each_trigger("charger", action_add_queue_tail);
    } else {
        action_for_each_trigger("early-boot", action_add_queue_tail);
        action_for_each_trigger("boot", action_add_queue_tail);
    }

        /* run all property triggers based on current state of the properties */
    queue_builtin_action(queue_property_triggers_action, "queue_propety_triggers");

    /* change USB function by meta_com_id */
#ifdef USE_BUILT_IN_FACTORY
    if (is_meta_boot() || is_factory_boot()) {
        queue_builtin_action(queue_com_triggers_action, "queue_com_triggers");
    }
#else
    if (is_meta_boot()) {
        queue_builtin_action(queue_com_triggers_action, "queue_com_triggers");
    }
#endif

#if BOOTCHART
    queue_builtin_action(bootchart_init_action, "bootchart_init");
#endif

    for(;;) {
        int nr, i, timeout = -1;

        execute_one_command();
        restart_processes();

        if (!property_set_fd_init && get_property_set_fd() > 0) {
            ufds[fd_count].fd = get_property_set_fd();
            ufds[fd_count].events = POLLIN;
            ufds[fd_count].revents = 0;
            fd_count++;
            property_set_fd_init = 1;
        }
        if (!signal_fd_init && get_signal_fd() > 0) {
            ufds[fd_count].fd = get_signal_fd();
            ufds[fd_count].events = POLLIN;
            ufds[fd_count].revents = 0;
            fd_count++;
            signal_fd_init = 1;
        }
        if (!keychord_fd_init && get_keychord_fd() > 0) {
            ufds[fd_count].fd = get_keychord_fd();
            ufds[fd_count].events = POLLIN;
            ufds[fd_count].revents = 0;
            fd_count++;
            keychord_fd_init = 1;
        }

        if (process_needs_restart) {
            timeout = (process_needs_restart - gettime()) * 1000;
            if (timeout < 0)
                timeout = 0;
        }

        if (!action_queue_empty() || cur_action)
            timeout = 0;

#if BOOTCHART
        if (bootchart_count > 0) {
            if (timeout < 0 || timeout > BOOTCHART_POLLING_MS)
                timeout = BOOTCHART_POLLING_MS;
            if (bootchart_step() < 0 || --bootchart_count == 0) {
                bootchart_finish();
                bootchart_count = 0;
            }
        }
#endif

        nr = poll(ufds, fd_count, timeout);
        if (nr <= 0)
            continue;

        for (i = 0; i < fd_count; i++) {
            if (ufds[i].revents == POLLIN) {
                if (ufds[i].fd == get_property_set_fd())
                    handle_property_set_fd();
                else if (ufds[i].fd == get_keychord_fd())
                    handle_keychord();
                else if (ufds[i].fd == get_signal_fd())
                    handle_signal();
            }
        }
    }

    return 0;
}
