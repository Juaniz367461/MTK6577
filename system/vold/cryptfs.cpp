/*
 * Copyright (C) 2010 The Android Open Source Project
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

/* TO DO:
 *   1.  Perhaps keep several copies of the encrypted key, in case something
 *       goes horribly wrong?
 *
 */

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <linux/dm-ioctl.h>
#include <libgen.h>
#include <stdlib.h>
#include <sys/param.h>
#include <string.h>
#include <sys/mount.h>
#include <openssl/evp.h>
#include <openssl/sha.h>
#include <errno.h>
#include <cutils/android_reboot.h>
#include <ext4.h>
#include <linux/kdev_t.h>
#include "cryptfs.h"
#define LOG_TAG "Cryptfs"
#include "cutils/log.h"
#include "cutils/properties.h"
#include "hardware_legacy/power.h"
#include "VolumeManager.h"
#include "cutils/partition_utils.h"
#include "Process.h"


#define DM_CRYPT_BUF_SIZE 4096
#define DATA_MNT_POINT "/data"

#define HASH_COUNT 2000
#define KEY_LEN_BYTES 16
#define IV_LEN_BYTES 16

#define KEY_LOC_PROP   "ro.crypto.keyfile.userdata"
#define KEY_IN_FOOTER  "footer"

#define EXT4_FS 1
#define FAT_FS 2

char *me = "cryptfs";

static unsigned char saved_master_key[KEY_LEN_BYTES];
static char *saved_data_blkdev;
static char *saved_mount_point;
static int  master_key_saved = 0;

static void ioctl_init(struct dm_ioctl *io, size_t dataSize, const char *name, unsigned flags)
{
    memset(io, 0, dataSize);
    io->data_size = dataSize;
    io->data_start = sizeof(struct dm_ioctl);
    io->version[0] = 4;
    io->version[1] = 0;
    io->version[2] = 0;
    io->flags = flags;
    if (name) {
        strncpy(io->name, name, sizeof(io->name));
    }
}

static unsigned int get_fs_size(char *dev)
{
    int fd, block_size;
    struct ext4_super_block sb;
    off64_t len;

    if ((fd = open(dev, O_RDONLY)) < 0) {
        SLOGE("Cannot open device to get filesystem size ");
        return 0;
    }

    if (lseek64(fd, 1024, SEEK_SET) < 0) {
        SLOGE("Cannot seek to superblock");
        return 0;
    }

    if (read(fd, &sb, sizeof(sb)) != sizeof(sb)) {
        SLOGE("Cannot read superblock");
        return 0;
    }

    close(fd);

    block_size = 1024 << sb.s_log_block_size;
    /* compute length in bytes */
    len = ( ((off64_t)sb.s_blocks_count_hi << 32) + sb.s_blocks_count_lo) * block_size;

    /* return length in sectors */
    return (unsigned int) (len / 512);
}

static unsigned int get_blkdev_size(int fd)
{
  unsigned int nr_sec;

  if ( (ioctl(fd, BLKGETSIZE, &nr_sec)) == -1) {
    nr_sec = 0;
  }

  return nr_sec;
}

/* key or salt can be NULL, in which case just skip writing that value.  Useful to
 * update the failed mount count but not change the key.
 */
static int put_crypt_ftr_and_key(char *real_blk_name, struct crypt_mnt_ftr *crypt_ftr,
                                  unsigned char *key, unsigned char *salt)
{
  int fd;
  unsigned int nr_sec, cnt;
  off64_t off;
  int rc = -1;
  char *fname;
  char key_loc[PROPERTY_VALUE_MAX];
  struct stat statbuf;

  property_get(KEY_LOC_PROP, key_loc, KEY_IN_FOOTER);

  if (!strcmp(key_loc, KEY_IN_FOOTER)) {
    fname = real_blk_name;
    if ( (fd = open(fname, O_RDWR)) < 0) {
      SLOGE("Cannot open real block device %s\n", fname);
      return -1;
    }

    if ( (nr_sec = get_blkdev_size(fd)) == 0) {
      SLOGE("Cannot get size of block device %s\n", fname);
      goto errout;
    }

    /* If it's an encrypted Android partition, the last 16 Kbytes contain the
     * encryption info footer and key, and plenty of bytes to spare for future
     * growth.
     */
    off = ((off64_t)nr_sec * 512) - CRYPT_FOOTER_OFFSET;

    if (lseek64(fd, off, SEEK_SET) == -1) {
      SLOGE("Cannot seek to real block device footer\n");
      goto errout;
    }
  } else if (key_loc[0] == '/') {
    fname = key_loc;
    if ( (fd = open(fname, O_RDWR | O_CREAT, 0600)) < 0) {
      SLOGE("Cannot open footer file %s\n", fname);
      return -1;
    }
  } else {
    SLOGE("Unexpected value for" KEY_LOC_PROP "\n");
    return -1;;
  }

  if ((cnt = write(fd, crypt_ftr, sizeof(struct crypt_mnt_ftr))) != sizeof(struct crypt_mnt_ftr)) {
    SLOGE("Cannot write real block device footer\n");
    goto errout;
  }

  if (key) {
    if (crypt_ftr->keysize != KEY_LEN_BYTES) {
      SLOGE("Keysize of %d bits not supported for real block device %s\n",
            crypt_ftr->keysize*8, fname);
      goto errout; 
    }

    if ( (cnt = write(fd, key, crypt_ftr->keysize)) != crypt_ftr->keysize) {
      SLOGE("Cannot write key for real block device %s\n", fname);
      goto errout;
    }
  }

  if (salt) {
    /* Compute the offset from the last write to the salt */
    off = KEY_TO_SALT_PADDING;
    if (! key)
      off += crypt_ftr->keysize;

    if (lseek64(fd, off, SEEK_CUR) == -1) {
      SLOGE("Cannot seek to real block device salt \n");
      goto errout;
    }

    if ( (cnt = write(fd, salt, SALT_LEN)) != SALT_LEN) {
      SLOGE("Cannot write salt for real block device %s\n", fname);
      goto errout;
    }
  }

  fstat(fd, &statbuf);
  /* If the keys are kept on a raw block device, do not try to truncate it. */
  if (S_ISREG(statbuf.st_mode) && (key_loc[0] == '/')) {
    if (ftruncate(fd, 0x4000)) {
      SLOGE("Cannot set footer file size\n", fname);
      goto errout;
    }
  }

  /* Success! */
  rc = 0;

errout:
  close(fd);
  return rc;

}

static int get_crypt_ftr_and_key(char *real_blk_name, struct crypt_mnt_ftr *crypt_ftr,
                                  unsigned char *key, unsigned char *salt)
{
  int fd;
  unsigned int nr_sec, cnt;
  off64_t off;
  int rc = -1;
  char key_loc[PROPERTY_VALUE_MAX];
  char *fname;
  struct stat statbuf;

  property_get(KEY_LOC_PROP, key_loc, KEY_IN_FOOTER);

  if (!strcmp(key_loc, KEY_IN_FOOTER)) {
    fname = real_blk_name;
    if ( (fd = open(fname, O_RDONLY)) < 0) {
      SLOGE("Cannot open real block device %s\n", fname);
      return -1;
    }

    if ( (nr_sec = get_blkdev_size(fd)) == 0) {
      SLOGE("Cannot get size of block device %s\n", fname);
      goto errout;
    }

    /* If it's an encrypted Android partition, the last 16 Kbytes contain the
     * encryption info footer and key, and plenty of bytes to spare for future
     * growth.
     */
    off = ((off64_t)nr_sec * 512) - CRYPT_FOOTER_OFFSET;

    if (lseek64(fd, off, SEEK_SET) == -1) {
      SLOGE("Cannot seek to real block device footer\n");
      goto errout;
    }
  } else if (key_loc[0] == '/') {
    fname = key_loc;
    if ( (fd = open(fname, O_RDONLY)) < 0) {
      SLOGE("Cannot open footer file %s\n", fname);
      return -1;
    }

    /* Make sure it's 16 Kbytes in length */
    fstat(fd, &statbuf);
    if (S_ISREG(statbuf.st_mode) && (statbuf.st_size != 0x4000)) {
      SLOGE("footer file %s is not the expected size!\n", fname);
      goto errout;
    }
  } else {
    SLOGE("Unexpected value for" KEY_LOC_PROP "\n");
    return -1;;
  }

  if ( (cnt = read(fd, crypt_ftr, sizeof(struct crypt_mnt_ftr))) != sizeof(struct crypt_mnt_ftr)) {
    SLOGE("Cannot read real block device footer\n");
    goto errout;
  }

  if (crypt_ftr->magic != CRYPT_MNT_MAGIC) {
    SLOGE("Bad magic for real block device %s\n", fname);
    goto errout;
  }

  if (crypt_ftr->major_version != 1) {
    SLOGE("Cannot understand major version %d real block device footer\n",
          crypt_ftr->major_version);
    goto errout;
  }

  if (crypt_ftr->minor_version != 0) {
    SLOGW("Warning: crypto footer minor version %d, expected 0, continuing...\n",
          crypt_ftr->minor_version);
  }

  if (crypt_ftr->ftr_size > sizeof(struct crypt_mnt_ftr)) {
    /* the footer size is bigger than we expected.
     * Skip to it's stated end so we can read the key.
     */
    if (lseek(fd, crypt_ftr->ftr_size - sizeof(struct crypt_mnt_ftr),  SEEK_CUR) == -1) {
      SLOGE("Cannot seek to start of key\n");
      goto errout;
    }
  }

  if (crypt_ftr->keysize != KEY_LEN_BYTES) {
    SLOGE("Keysize of %d bits not supported for real block device %s\n",
          crypt_ftr->keysize * 8, fname);
    goto errout;
  }

  if ( (cnt = read(fd, key, crypt_ftr->keysize)) != crypt_ftr->keysize) {
    SLOGE("Cannot read key for real block device %s\n", fname);
    goto errout;
  }

  if (lseek64(fd, KEY_TO_SALT_PADDING, SEEK_CUR) == -1) {
    SLOGE("Cannot seek to real block device salt\n");
    goto errout;
  }

  if ( (cnt = read(fd, salt, SALT_LEN)) != SALT_LEN) {
    SLOGE("Cannot read salt for real block device %s\n", fname);
    goto errout;
  }

  /* Success! */
  rc = 0;

errout:
  close(fd);
  return rc;
}

/* Convert a binary key of specified length into an ascii hex string equivalent,
 * without the leading 0x and with null termination
 */
void convert_key_to_hex_ascii(unsigned char *master_key, unsigned int keysize,
                              char *master_key_ascii)
{
  unsigned int i, a;
  unsigned char nibble;

  for (i=0, a=0; i<keysize; i++, a+=2) {
    /* For each byte, write out two ascii hex digits */
    nibble = (master_key[i] >> 4) & 0xf;
    master_key_ascii[a] = nibble + (nibble > 9 ? 0x37 : 0x30);

    nibble = master_key[i] & 0xf;
    master_key_ascii[a+1] = nibble + (nibble > 9 ? 0x37 : 0x30);
  }

  /* Add the null termination */
  master_key_ascii[a] = '\0';

}

static int create_crypto_blk_dev(struct crypt_mnt_ftr *crypt_ftr, unsigned char *master_key,
                                    char *real_blk_name, char *crypto_blk_name, const char *name)
{
  char buffer[DM_CRYPT_BUF_SIZE];
  char master_key_ascii[129]; /* Large enough to hold 512 bit key and null */
  char *crypt_params;
  struct dm_ioctl *io;
  struct dm_target_spec *tgt;
  unsigned int minor;
  int fd;
  int retval = -1;

  if ((fd = open("/dev/device-mapper", O_RDWR)) < 0 ) {
    SLOGE("Cannot open device-mapper\n");
    goto errout;
  }

  io = (struct dm_ioctl *) buffer;

  ioctl_init(io, DM_CRYPT_BUF_SIZE, name, 0);
  if (ioctl(fd, DM_DEV_CREATE, io)) {
    SLOGE("Cannot create dm-crypt device\n");
    goto errout;
  }

  /* Get the device status, in particular, the name of it's device file */
  ioctl_init(io, DM_CRYPT_BUF_SIZE, name, 0);
  if (ioctl(fd, DM_DEV_STATUS, io)) {
    SLOGE("Cannot retrieve dm-crypt device status\n");
    goto errout;
  }
  minor = (io->dev & 0xff) | ((io->dev >> 12) & 0xfff00);
  snprintf(crypto_blk_name, MAXPATHLEN, "/dev/block/dm-%u", minor);

  /* Load the mapping table for this device */
  tgt = (struct dm_target_spec *) &buffer[sizeof(struct dm_ioctl)];

  ioctl_init(io, 4096, name, 0);
  io->target_count = 1;
  tgt->status = 0;
  tgt->sector_start = 0;
  tgt->length = crypt_ftr->fs_size;
  strcpy(tgt->target_type, "crypt");

  crypt_params = buffer + sizeof(struct dm_ioctl) + sizeof(struct dm_target_spec);
  convert_key_to_hex_ascii(master_key, crypt_ftr->keysize, master_key_ascii);
  sprintf(crypt_params, "%s %s 0 %s 0", crypt_ftr->crypto_type_name,
          master_key_ascii, real_blk_name);
  crypt_params += strlen(crypt_params) + 1;
  crypt_params = (char *) (((unsigned long)crypt_params + 7) & ~8); /* Align to an 8 byte boundary */
  tgt->next = crypt_params - buffer;

  if (ioctl(fd, DM_TABLE_LOAD, io)) {
      SLOGE("Cannot load dm-crypt mapping table.\n");
      goto errout;
  }

  /* Resume this device to activate it */
  ioctl_init(io, 4096, name, 0);

  if (ioctl(fd, DM_DEV_SUSPEND, io)) {
    SLOGE("Cannot resume the dm-crypt device\n");
    goto errout;
  }

  /* We made it here with no errors.  Woot! */
  retval = 0;

errout:
  close(fd);   /* If fd is <0 from a failed open call, it's safe to just ignore the close error */

  return retval;
}

static int delete_crypto_blk_dev(char *name)
{
  int fd;
  char buffer[DM_CRYPT_BUF_SIZE];
  struct dm_ioctl *io;
  int retval = -1;

  if ((fd = open("/dev/device-mapper", O_RDWR)) < 0 ) {
    SLOGE("Cannot open device-mapper\n");
    goto errout;
  }

  io = (struct dm_ioctl *) buffer;

  ioctl_init(io, DM_CRYPT_BUF_SIZE, name, 0);
  if (ioctl(fd, DM_DEV_REMOVE, io)) {
    SLOGE("Cannot remove dm-crypt device\n");
    goto errout;
  }

  /* We made it here with no errors.  Woot! */
  retval = 0;

errout:
  close(fd);    /* If fd is <0 from a failed open call, it's safe to just ignore the close error */

  return retval;

}

static void pbkdf2(char *passwd, unsigned char *salt, unsigned char *ikey)
{
    /* Turn the password into a key and IV that can decrypt the master key */
    PKCS5_PBKDF2_HMAC_SHA1(passwd, strlen(passwd), salt, SALT_LEN,
                           HASH_COUNT, KEY_LEN_BYTES+IV_LEN_BYTES, ikey);
}

static int encrypt_master_key(char *passwd, unsigned char *salt,
                              unsigned char *decrypted_master_key,
                              unsigned char *encrypted_master_key)
{
    unsigned char ikey[32+32] = { 0 }; /* Big enough to hold a 256 bit key and 256 bit IV */
    EVP_CIPHER_CTX e_ctx;
    int encrypted_len, final_len;

    /* Turn the password into a key and IV that can decrypt the master key */
    pbkdf2(passwd, salt, ikey);
  
    /* Initialize the decryption engine */
    if (! EVP_EncryptInit(&e_ctx, EVP_aes_128_cbc(), ikey, ikey+KEY_LEN_BYTES)) {
        SLOGE("EVP_EncryptInit failed\n");
        return -1;
    }
    EVP_CIPHER_CTX_set_padding(&e_ctx, 0); /* Turn off padding as our data is block aligned */

    /* Encrypt the master key */
    if (! EVP_EncryptUpdate(&e_ctx, encrypted_master_key, &encrypted_len,
                              decrypted_master_key, KEY_LEN_BYTES)) {
        SLOGE("EVP_EncryptUpdate failed\n");
        return -1;
    }
    if (! EVP_EncryptFinal(&e_ctx, encrypted_master_key + encrypted_len, &final_len)) {
        SLOGE("EVP_EncryptFinal failed\n");
        return -1;
    }

    if (encrypted_len + final_len != KEY_LEN_BYTES) {
        SLOGE("EVP_Encryption length check failed with %d, %d bytes\n", encrypted_len, final_len);
        return -1;
    } else {
        return 0;
    }
}

static int decrypt_master_key(char *passwd, unsigned char *salt,
                              unsigned char *encrypted_master_key,
                              unsigned char *decrypted_master_key)
{
  unsigned char ikey[32+32] = { 0 }; /* Big enough to hold a 256 bit key and 256 bit IV */
  EVP_CIPHER_CTX d_ctx;
  int decrypted_len, final_len;

  /* Turn the password into a key and IV that can decrypt the master key */
  pbkdf2(passwd, salt, ikey);

  /* Initialize the decryption engine */
  if (! EVP_DecryptInit(&d_ctx, EVP_aes_128_cbc(), ikey, ikey+KEY_LEN_BYTES)) {
    return -1;
  }
  EVP_CIPHER_CTX_set_padding(&d_ctx, 0); /* Turn off padding as our data is block aligned */
  /* Decrypt the master key */
  if (! EVP_DecryptUpdate(&d_ctx, decrypted_master_key, &decrypted_len,
                            encrypted_master_key, KEY_LEN_BYTES)) {
    return -1;
  }
  if (! EVP_DecryptFinal(&d_ctx, decrypted_master_key + decrypted_len, &final_len)) {
    return -1;
  }

  if (decrypted_len + final_len != KEY_LEN_BYTES) {
    return -1;
  } else {
    return 0;
  }
}

static int create_encrypted_random_key(char *passwd, unsigned char *master_key, unsigned char *salt)
{
    int fd;
    unsigned char key_buf[KEY_LEN_BYTES];
    EVP_CIPHER_CTX e_ctx;
    int encrypted_len, final_len;

    /* Get some random bits for a key */
    fd = open("/dev/urandom", O_RDONLY);
    read(fd, key_buf, sizeof(key_buf));
    read(fd, salt, SALT_LEN);
    close(fd);

    /* Now encrypt it with the password */
    return encrypt_master_key(passwd, salt, key_buf, master_key);
}

static int get_orig_mount_parms(char *mount_point, char *fs_type, char *real_blkdev,
                                unsigned long *mnt_flags, char *fs_options)
{
  char mount_point2[PROPERTY_VALUE_MAX];
  char fs_flags[PROPERTY_VALUE_MAX];

  property_get("ro.crypto.fs_type", fs_type, "");
  property_get("ro.crypto.fs_real_blkdev", real_blkdev, "");
  property_get("ro.crypto.fs_mnt_point", mount_point2, "");
  property_get("ro.crypto.fs_options", fs_options, "");
  property_get("ro.crypto.fs_flags", fs_flags, "");
  *mnt_flags = strtol(fs_flags, 0, 0);

  if (strcmp(mount_point, mount_point2)) {
    /* Consistency check.  These should match. If not, something odd happened. */
    return -1;
  }

  return 0;
}

static int wait_and_unmount(char *mountpoint)
{
    int i, rc;
#define WAIT_UNMOUNT_COUNT 20

    /*  Now umount the tmpfs filesystem */
    for (i=0; i<WAIT_UNMOUNT_COUNT; i++) {
        if (umount(mountpoint)) {
            if (errno == EINVAL) {
                /* EINVAL is returned if the directory is not a mountpoint,
                 * i.e. there is no filesystem mounted there.  So just get out.
                 */
                break;
            }
            sleep(1);
            i++;
        } else {
          break;
        }
    }

    if (i < WAIT_UNMOUNT_COUNT) {
      SLOGD("unmounting %s succeeded\n", mountpoint);
      rc = 0;
    } else {
      SLOGE("unmounting %s failed\n", mountpoint);
      SLOGE("try to kill related process\n");

      Process::killProcessesWithOpenFiles(mountpoint, 2);
      if (!umount(mountpoint)) {
          rc = 0;
      }
      else {
        rc = -1;
      }
    }
    return rc;
}

#define DATA_PREP_TIMEOUT 100
static int prep_data_fs(void)
{
    int i;

    /* Do the prep of the /data filesystem */
    property_set("vold.post_fs_data_done", "0");
    property_set("vold.decrypt", "trigger_post_fs_data");
    SLOGD("Just triggered post_fs_data\n");

    /* Wait a max of 25 seconds, hopefully it takes much less */
    for (i=0; i<DATA_PREP_TIMEOUT; i++) {
        char p[PROPERTY_VALUE_MAX];

        property_get("vold.post_fs_data_done", p, "0");
        if (*p == '1') {
            break;
        } else {
            usleep(250000);
        }
    }
    if (i == DATA_PREP_TIMEOUT) {
        /* Ugh, we failed to prep /data in time.  Bail. */
        return -1;
    } else {
        SLOGD("post_fs_data done\n");
        return 0;
    }
}

int cryptfs_restart(bool is_ipo_reboot, int boot_reason)
{
    char fs_type[32];
    char real_blkdev[MAXPATHLEN];
    char crypto_blkdev[MAXPATHLEN];
    char tmpfs_options[MAXPATHLEN];
    char fs_options[256];
    unsigned long mnt_flags;
    struct stat statbuf;
    int rc = -1, i;
    static int restart_successful = 0;
    static int isBackupBootReason = false;
    static int BackupBootReason = 0;    /* 1: alarm boot,  0: normal boot */


    /* Validate that it's OK to call this routine */


	if (is_ipo_reboot) {
	   isBackupBootReason = true;
	   BackupBootReason = boot_reason;	  /* 1: alarm boot,  0: normal boot */ 
    }

    SLOGE("cryptfs_restart: is_ipo_reboot=%d, isBackupBootReason=%d, BackupBootReason=%d", is_ipo_reboot, isBackupBootReason, BackupBootReason);
    if (! master_key_saved && !is_ipo_reboot) {
        SLOGE("Encrypted filesystem not validated, aborting");
        return -1;
    }

    /* Here is where we shut down the framework.  The init scripts
     * start all services in one of three classes: core, main or late_start.
     * On boot, we start core and main.  Now, we stop main, but not core,
     * as core includes vold and a few other really important things that
     * we need to keep running.  Once main has stopped, we should be able
     * to umount the tmpfs /data, then mount the encrypted /data.
     * We then restart the class main, and also the class late_start.
     * At the moment, I've only put a few things in late_start that I know
     * are not needed to bring up the framework, and that also cause problems
     * with unmounting the tmpfs /data, but I hope to add add more services
     * to the late_start class as we optimize this to decrease the delay
     * till the user is asked for the password to the filesystem.
     */


    property_set("mux.report.case", "3");
    property_set("ctl.start", "muxreport-daemon"); 
    SLOGD("Just asked init to stop modem\n");



    /* The init files are setup to stop the class main when vold.decrypt is
     * set to trigger_reset_main.
     */
    property_set("vold.decrypt", "trigger_reset_main");
    SLOGD("Just asked init to shut down class main\n");

    /* Now that the framework is shutdown, we should be able to umount()
     * the tmpfs filesystem, and mount the real one.
     */
    if (is_ipo_reboot) {
        master_key_saved = 0;
        property_get("ro.crypto.tmpfs_options", tmpfs_options, "");
        if (strlen(tmpfs_options) == 0) {
          SLOGE("ro.crypto.tmpfs_options not set\n");
          goto cryptfs_restart_error;
        }
    }
    else {
        property_get("ro.crypto.fs_crypto_blkdev", crypto_blkdev, "");
        if (strlen(crypto_blkdev) == 0) {
            SLOGE("fs_crypto_blkdev not set\n");
            goto cryptfs_restart_error;
        }
    }

    if (! get_orig_mount_parms(DATA_MNT_POINT, fs_type, real_blkdev, &mnt_flags, fs_options)) {
        SLOGD("Just got orig mount parms\n");

        if (! (rc = wait_and_unmount(DATA_MNT_POINT)) ) {
           if (is_ipo_reboot) {
                delete_crypto_blk_dev("userdata");  
                if (mount("tmpfs", DATA_MNT_POINT, "tmpfs", MS_NOATIME | MS_NOSUID | MS_NODEV, tmpfs_options)) {
                    SLOGE("Fail: mount tmpfs, errno=%d\n", errno);
                    goto cryptfs_restart_error;
                 }
                 else {
                    SLOGD("Successful: mount tmpfs\n");
                }   
            }
            else {
                /* If that succeeded, then mount the decrypted filesystem */
                if (mount(crypto_blkdev, DATA_MNT_POINT, fs_type, mnt_flags, fs_options)) {
                     SLOGE("Fail: mount crypto_blkdev, errno=%d\n", errno);
                     goto cryptfs_restart_error;
                }
                else {
                    SLOGD("Successful: mount crypto_blkdev\n");
                }
            }

            property_set("vold.decrypt", "trigger_load_persist_props");
            /* Create necessary paths on /data */
            if (prep_data_fs()) {
                goto cryptfs_restart_error;
            }

            if (is_ipo_reboot) {
              int usb_ret = 0;
              int fd = open("/sys/devices/platform/mt_usb/cmode", O_WRONLY);
              if (fd >= 0) {
                  usb_ret = write(fd, "1", 1);
                  close(fd);
                  if (usb_ret < 0) {
                    SLOGE("Unable to enable usb");          
                  }
                  else {
                    SLOGE("Enable usb successfully");       
                  }
              } else {
                  SLOGE("Unable to enable usb cos creating fd error");          
              }             
            }
            else {
                if (isBackupBootReason) {
                   char buf[8];
                   sprintf(buf, "%d", BackupBootReason);
                   property_set("sys.boot.reason", buf);
                }
                else {
                   property_set("ctl.start", "bootlogoupdater"); 
                }
            }

            property_set("mux.report.case", "4");
            property_set("ctl.start", "muxreport-daemon"); 
            SLOGD("Just asked init to start modem\n");

            /* startup service classes main and late_start */
            property_set("vold.decrypt", "trigger_restart_framework");
            SLOGD("Just triggered restart_framework\n");

            /* Give it a few moments to get started */
            sleep(1);

            if (is_ipo_reboot) {
                property_set("vold.decrypt", "1");
                SLOGD("Set the status to encrypted, vold.decrypt to 1\n");               
            }
        }
    }

    return rc;

cryptfs_restart_error:
    property_set("mux.report.case", "4");
    property_set("ctl.start", "muxreport-daemon"); 
    SLOGD("cryptfs_restart_error: asked init to start modem\n");
    return -1;

}

static int do_crypto_complete(char *mount_point)
{
  struct crypt_mnt_ftr crypt_ftr;
  unsigned char encrypted_master_key[32];
  unsigned char salt[SALT_LEN];
  char real_blkdev[MAXPATHLEN];
  char fs_type[PROPERTY_VALUE_MAX];
  char fs_options[PROPERTY_VALUE_MAX];
  unsigned long mnt_flags;
  char encrypted_state[PROPERTY_VALUE_MAX];

  property_get("ro.crypto.state", encrypted_state, "");
  if (strcmp(encrypted_state, "encrypted") ) {
    SLOGE("not running with encryption, aborting");
    return 1;
  }

  if (get_orig_mount_parms(mount_point, fs_type, real_blkdev, &mnt_flags, fs_options)) {
    SLOGE("Error reading original mount parms for mount point %s\n", mount_point);
    return -1;
  }

  if (get_crypt_ftr_and_key(real_blkdev, &crypt_ftr, encrypted_master_key, salt)) {
    SLOGE("Error getting crypt footer and key\n");
    return -1;
  }

  if (crypt_ftr.flags & CRYPT_ENCRYPTION_IN_PROGRESS) {
    SLOGE("Encryption process didn't finish successfully\n");
    return -2;  /* -2 is the clue to the UI that there is no usable data on the disk,
                 * and give the user an option to wipe the disk */
  }

  /* We passed the test! We shall diminish, and return to the west */
  return 0;
}

static int test_mount_encrypted_fs(char *passwd, char *mount_point, char *label)
{
  struct crypt_mnt_ftr crypt_ftr;
  /* Allocate enough space for a 256 bit key, but we may use less */
  unsigned char encrypted_master_key[32], decrypted_master_key[32];
  unsigned char salt[SALT_LEN];
  char crypto_blkdev[MAXPATHLEN];
  char real_blkdev[MAXPATHLEN];
  char fs_type[PROPERTY_VALUE_MAX];
  char fs_options[PROPERTY_VALUE_MAX];
  char tmp_mount_point[64];
  unsigned long mnt_flags;
  unsigned int orig_failed_decrypt_count;
  char encrypted_state[PROPERTY_VALUE_MAX];
  int rc;

  property_get("ro.crypto.state", encrypted_state, "");
  if ( master_key_saved || strcmp(encrypted_state, "encrypted") ) {
    SLOGE("encrypted fs already validated or not running with encryption, aborting");
    return -1;
  }

  if (get_orig_mount_parms(mount_point, fs_type, real_blkdev, &mnt_flags, fs_options)) {
    SLOGE("Error reading original mount parms for mount point %s\n", mount_point);
    return -1;
  }

  if (get_crypt_ftr_and_key(real_blkdev, &crypt_ftr, encrypted_master_key, salt)) {
    SLOGE("Error getting crypt footer and key\n");
    return -1;
  }

  SLOGD("crypt_ftr->fs_size = %lld\n", crypt_ftr.fs_size);
  orig_failed_decrypt_count = crypt_ftr.failed_decrypt_count;

  if (! (crypt_ftr.flags & CRYPT_MNT_KEY_UNENCRYPTED) ) {
    decrypt_master_key(passwd, salt, encrypted_master_key, decrypted_master_key);
  }

  if (create_crypto_blk_dev(&crypt_ftr, decrypted_master_key,
                               real_blkdev, crypto_blkdev, label)) {
    SLOGE("Error creating decrypted block device\n");
    return -1;
  }

  /* If init detects an encrypted filesystme, it writes a file for each such
   * encrypted fs into the tmpfs /data filesystem, and then the framework finds those
   * files and passes that data to me */
  /* Create a tmp mount point to try mounting the decryptd fs
   * Since we're here, the mount_point should be a tmpfs filesystem, so make
   * a directory in it to test mount the decrypted filesystem.
   */
  sprintf(tmp_mount_point, "%s/tmp_mnt", mount_point);
  mkdir(tmp_mount_point, 0755);
  if ( mount(crypto_blkdev, tmp_mount_point, "ext4", MS_RDONLY, "") ) {
    SLOGE("Error temp mounting decrypted block device\n");
    delete_crypto_blk_dev(label);
    crypt_ftr.failed_decrypt_count++;
  } else {
    /* Success, so just umount and we'll mount it properly when we restart
     * the framework.
     */
    umount(tmp_mount_point);
    crypt_ftr.failed_decrypt_count  = 0;
  }

  if (orig_failed_decrypt_count != crypt_ftr.failed_decrypt_count) {
    put_crypt_ftr_and_key(real_blkdev, &crypt_ftr, 0, 0);
  }

  if (crypt_ftr.failed_decrypt_count) {
    /* We failed to mount the device, so return an error */
    rc = crypt_ftr.failed_decrypt_count;

  } else {
    /* Woot!  Success!  Save the name of the crypto block device
     * so we can mount it when restarting the framework.
     */
    property_set("ro.crypto.fs_crypto_blkdev", crypto_blkdev);

    /* Also save a the master key so we can reencrypted the key
     * the key when we want to change the password on it.
     */
    memcpy(saved_master_key, decrypted_master_key, KEY_LEN_BYTES);
    saved_data_blkdev = strdup(real_blkdev);
    saved_mount_point = strdup(mount_point);
    master_key_saved = 1;
    rc = 0;
  }

  return rc;
}

/* Called by vold when it wants to undo the crypto mapping of a volume it
 * manages.  This is usually in response to a factory reset, when we want
 * to undo the crypto mapping so the volume is formatted in the clear.
 */
int cryptfs_revert_volume(const char *label)
{
    return delete_crypto_blk_dev((char *)label);
}

/*
 * Called by vold when it's asked to mount an encrypted, nonremovable volume.
 * Setup a dm-crypt mapping, use the saved master key from
 * setting up the /data mapping, and return the new device path.
 */
int cryptfs_setup_volume(const char *label, int major, int minor,
                         char *crypto_sys_path, unsigned int max_path,
                         int *new_major, int *new_minor)
{
    char real_blkdev[MAXPATHLEN], crypto_blkdev[MAXPATHLEN];
    struct crypt_mnt_ftr sd_crypt_ftr;
    unsigned char key[32], salt[32];
    struct stat statbuf;
    int nr_sec, fd;

    sprintf(real_blkdev, "/dev/block/vold/%d:%d", major, minor);

    /* Just want the footer, but gotta get it all */
    get_crypt_ftr_and_key(saved_data_blkdev, &sd_crypt_ftr, key, salt);

    /* Update the fs_size field to be the size of the volume */
    fd = open(real_blkdev, O_RDONLY);
    nr_sec = get_blkdev_size(fd);
    close(fd);
    if (nr_sec == 0) {
        SLOGE("Cannot get size of volume %s\n", real_blkdev);
        return -1;
    }

    sd_crypt_ftr.fs_size = nr_sec;
    create_crypto_blk_dev(&sd_crypt_ftr, saved_master_key, real_blkdev, 
                          crypto_blkdev, label);

    stat(crypto_blkdev, &statbuf);
    *new_major = MAJOR(statbuf.st_rdev);
    *new_minor = MINOR(statbuf.st_rdev);

    /* Create path to sys entry for this block device */
    snprintf(crypto_sys_path, max_path, "/devices/virtual/block/%s", strrchr(crypto_blkdev, '/')+1);

    return 0;
}

int cryptfs_crypto_complete(void)
{
  return do_crypto_complete("/data");
}

int cryptfs_check_passwd(char *passwd)
{
    int rc = -1;

    rc = test_mount_encrypted_fs(passwd, DATA_MNT_POINT, "userdata");

    return rc;
}

int cryptfs_verify_passwd(char *passwd)
{
    struct crypt_mnt_ftr crypt_ftr;
    /* Allocate enough space for a 256 bit key, but we may use less */
    unsigned char encrypted_master_key[32], decrypted_master_key[32];
    unsigned char salt[SALT_LEN];
    char real_blkdev[MAXPATHLEN];
    char fs_type[PROPERTY_VALUE_MAX];
    char fs_options[PROPERTY_VALUE_MAX];
    unsigned long mnt_flags;
    char encrypted_state[PROPERTY_VALUE_MAX];
    int rc;

    property_get("ro.crypto.state", encrypted_state, "");
    if (strcmp(encrypted_state, "encrypted") ) {
        SLOGE("device not encrypted, aborting");
        return -2;
    }

    if (!master_key_saved) {
        SLOGE("encrypted fs not yet mounted, aborting");
        return -1;
    }

    if (!saved_mount_point) {
        SLOGE("encrypted fs failed to save mount point, aborting");
        return -1;
    }

    if (get_orig_mount_parms(saved_mount_point, fs_type, real_blkdev, &mnt_flags, fs_options)) {
        SLOGE("Error reading original mount parms for mount point %s\n", saved_mount_point);
        return -1;
    }

    if (get_crypt_ftr_and_key(real_blkdev, &crypt_ftr, encrypted_master_key, salt)) {
        SLOGE("Error getting crypt footer and key\n");
        return -1;
    }

    if (crypt_ftr.flags & CRYPT_MNT_KEY_UNENCRYPTED) {
        /* If the device has no password, then just say the password is valid */
        rc = 0;
    } else {
        decrypt_master_key(passwd, salt, encrypted_master_key, decrypted_master_key);
        if (!memcmp(decrypted_master_key, saved_master_key, crypt_ftr.keysize)) {
            /* They match, the password is correct */
            rc = 0;
        } else {
            /* If incorrect, sleep for a bit to prevent dictionary attacks */
            sleep(1);
            rc = 1;
        }
    }

    return rc;
}

/* Initialize a crypt_mnt_ftr structure.  The keysize is
 * defaulted to 16 bytes, and the filesystem size to 0.
 * Presumably, at a minimum, the caller will update the
 * filesystem size and crypto_type_name after calling this function.
 */
static void cryptfs_init_crypt_mnt_ftr(struct crypt_mnt_ftr *ftr)
{
    ftr->magic = CRYPT_MNT_MAGIC;
    ftr->major_version = 1;
    ftr->minor_version = 0;
    ftr->ftr_size = sizeof(struct crypt_mnt_ftr);
    ftr->flags = 0;
    ftr->keysize = KEY_LEN_BYTES;
    ftr->spare1 = 0;
    ftr->fs_size = 0;
    ftr->failed_decrypt_count = 0;
    ftr->crypto_type_name[0] = '\0';
}

static int cryptfs_enable_wipe(char *crypto_blkdev, off64_t size, int type)
{
    char cmdline[256];
    int rc = -1;

    if (type == EXT4_FS) {
        snprintf(cmdline, sizeof(cmdline), "/system/bin/make_ext4fs -a /data -l %lld %s",
                 size * 512, crypto_blkdev);
        SLOGI("Making empty filesystem with command %s\n", cmdline);
    } else if (type== FAT_FS) {
        snprintf(cmdline, sizeof(cmdline), "/system/bin/newfs_msdos -F 32 -O android -c 8 -s %lld %s",
                 size, crypto_blkdev);
        SLOGI("Making empty filesystem with command %s\n", cmdline);
    } else {
        SLOGE("cryptfs_enable_wipe(): unknown filesystem type %d\n", type);
        return -1;
    }

    if (system(cmdline)) {
      SLOGE("Error creating empty filesystem on %s\n", crypto_blkdev);
    } else {
      SLOGD("Successfully created empty filesystem on %s\n", crypto_blkdev);
      rc = 0;
    }

    return rc;
}

static inline int unix_read(int  fd, void*  buff, int  len)
{
    int  ret;
    do { ret = read(fd, buff, len); } while (ret < 0 && errno == EINTR);
    return ret;
}

static inline int unix_write(int  fd, const void*  buff, int  len)
{
    int  ret;
    do { ret = write(fd, buff, len); } while (ret < 0 && errno == EINTR);
    return ret;
}

#define CRYPT_INPLACE_BUFSIZE 4096
#define CRYPT_SECTORS_PER_BUFSIZE (CRYPT_INPLACE_BUFSIZE / 512)
static int cryptfs_enable_inplace(char *crypto_blkdev, char *real_blkdev, off64_t size,
                                  off64_t *size_already_done, off64_t tot_size)
{
    int realfd, cryptofd;
    char *buf[CRYPT_INPLACE_BUFSIZE];
    int rc = -1;
    off64_t numblocks, i, remainder;
    off64_t one_pct, cur_pct, new_pct;
    off64_t blocks_already_done, tot_numblocks;

    if ( (realfd = open(real_blkdev, O_RDONLY)) < 0) { 
        SLOGE("Error opening real_blkdev %s for inplace encrypt\n", real_blkdev);
        return -1;
    }

    if ( (cryptofd = open(crypto_blkdev, O_WRONLY)) < 0) { 
        SLOGE("Error opening crypto_blkdev %s for inplace encrypt\n", crypto_blkdev);
        close(realfd);
        return -1;
    }

    /* This is pretty much a simple loop of reading 4K, and writing 4K.
     * The size passed in is the number of 512 byte sectors in the filesystem.
     * So compute the number of whole 4K blocks we should read/write,
     * and the remainder.
     */
    numblocks = size / CRYPT_SECTORS_PER_BUFSIZE;
    remainder = size % CRYPT_SECTORS_PER_BUFSIZE;
    tot_numblocks = tot_size / CRYPT_SECTORS_PER_BUFSIZE;
    blocks_already_done = *size_already_done / CRYPT_SECTORS_PER_BUFSIZE;

    SLOGE("Encrypting filesystem in place...");

    one_pct = tot_numblocks / 100;
    cur_pct = 0;
    /* process the majority of the filesystem in blocks */
    for (i=0; i<numblocks; i++) {
        new_pct = (i + blocks_already_done) / one_pct;
        if (new_pct > cur_pct) {
            char buf[8];

            cur_pct = new_pct;
            snprintf(buf, sizeof(buf), "%lld", cur_pct);
            property_set("vold.encrypt_progress", buf);
        }
        if (unix_read(realfd, buf, CRYPT_INPLACE_BUFSIZE) <= 0) {
            SLOGE("Error reading real_blkdev %s for inplace encrypt\n", crypto_blkdev);
            goto errout;
        }
        if (unix_write(cryptofd, buf, CRYPT_INPLACE_BUFSIZE) <= 0) {
            SLOGE("Error writing crypto_blkdev %s for inplace encrypt\n", crypto_blkdev);
            goto errout;
        }
    }

    /* Do any remaining sectors */
    for (i=0; i<remainder; i++) {
        if (unix_read(realfd, buf, 512) <= 0) {
            SLOGE("Error reading rival sectors from real_blkdev %s for inplace encrypt\n", crypto_blkdev);
            goto errout;
        }
        if (unix_write(cryptofd, buf, 512) <= 0) {
            SLOGE("Error writing final sectors to crypto_blkdev %s for inplace encrypt\n", crypto_blkdev);
            goto errout;
        }
    }

    *size_already_done += size;
    rc = 0;

errout:
    close(realfd);
    close(cryptofd);

    return rc;
}

#define CRYPTO_ENABLE_WIPE 1
#define CRYPTO_ENABLE_INPLACE 2

#define FRAMEWORK_BOOT_WAIT 60

static inline int should_encrypt(struct volume_info *volume)
{
    return (volume->flags & (VOL_ENCRYPTABLE | VOL_NONREMOVABLE)) == 
            (VOL_ENCRYPTABLE | VOL_NONREMOVABLE);
}

int cryptfs_enable(char *howarg, char *passwd)
{
    int how = 0;
    char crypto_blkdev[MAXPATHLEN], real_blkdev[MAXPATHLEN], sd_crypto_blkdev[MAXPATHLEN];
    char fs_type[PROPERTY_VALUE_MAX], fs_options[PROPERTY_VALUE_MAX],
         mount_point[PROPERTY_VALUE_MAX];
    unsigned long mnt_flags, nr_sec;
    unsigned char master_key[KEY_LEN_BYTES], decrypted_master_key[KEY_LEN_BYTES];
    unsigned char salt[SALT_LEN];
    int rc=-1, fd, i, ret;
    struct crypt_mnt_ftr crypt_ftr, sd_crypt_ftr;;
    char tmpfs_options[PROPERTY_VALUE_MAX];
    char encrypted_state[PROPERTY_VALUE_MAX];
    char lockid[32] = { 0 };
    char key_loc[PROPERTY_VALUE_MAX];
    char fuse_sdcard[PROPERTY_VALUE_MAX];
    char *sd_mnt_point;
    char sd_blk_dev[256] = { 0 };
    int num_vols;
    struct volume_info *vol_list = 0;
    off64_t cur_encryption_done=0, tot_encryption_size=0;

    property_get("ro.crypto.state", encrypted_state, "");
    if (strcmp(encrypted_state, "unencrypted")) {
        SLOGE("Device is already running encrypted, aborting");
        goto error_unencrypted;
    }

    property_get(KEY_LOC_PROP, key_loc, KEY_IN_FOOTER);

    if (!strcmp(howarg, "wipe")) {
      how = CRYPTO_ENABLE_WIPE;
    } else if (! strcmp(howarg, "inplace")) {
      how = CRYPTO_ENABLE_INPLACE;
    } else {
      /* Shouldn't happen, as CommandListener vets the args */
      goto error_unencrypted;
    }

    get_orig_mount_parms(mount_point, fs_type, real_blkdev, &mnt_flags, fs_options);

    /* Get the size of the real block device */
    fd = open(real_blkdev, O_RDONLY);
    if ( (nr_sec = get_blkdev_size(fd)) == 0) {
        SLOGE("Cannot get size of block device %s\n", real_blkdev);
        goto error_unencrypted;
    }
    close(fd);

    /* If doing inplace encryption, make sure the orig fs doesn't include the crypto footer */
    if ((how == CRYPTO_ENABLE_INPLACE) && (!strcmp(key_loc, KEY_IN_FOOTER))) {
        unsigned int fs_size_sec, max_fs_size_sec;

        fs_size_sec = get_fs_size(real_blkdev);
        max_fs_size_sec = nr_sec - (CRYPT_FOOTER_OFFSET / 512);

        if (fs_size_sec > max_fs_size_sec) {
            SLOGE("Orig filesystem overlaps crypto footer region.  Cannot encrypt in place.");
            goto error_unencrypted;
        }
    }

    /* Get a wakelock as this may take a while, and we don't want the
     * device to sleep on us.  We'll grab a partial wakelock, and if the UI
     * wants to keep the screen on, it can grab a full wakelock.
     */
    snprintf(lockid, sizeof(lockid), "enablecrypto%d", (int) getpid());
    acquire_wake_lock(PARTIAL_WAKE_LOCK, lockid);

     /* Get the sdcard mount point */
     sd_mnt_point = getenv("EXTERNAL_STORAGE");
     if (! sd_mnt_point) {
         sd_mnt_point = "/mnt/sdcard";
     }

    num_vols=vold_getNumDirectVolumes();
    vol_list = (struct volume_info *)malloc(sizeof(struct volume_info) * num_vols);
    vold_getDirectVolumeList(vol_list);

    for (i=0; i<num_vols; i++) {
        if (should_encrypt(&vol_list[i])) {
            fd = open(vol_list[i].blk_dev, O_RDONLY);
            if ( (vol_list[i].size = get_blkdev_size(fd)) == 0) {
                SLOGE("Cannot get size of block device %s\n", vol_list[i].blk_dev);
                goto error_unencrypted;
            }
            close(fd);

            ret=vold_disableVol(vol_list[i].label);
            if ((ret < 0) && (ret != UNMOUNT_NOT_MOUNTED_ERR)) {
                /* -2 is returned when the device exists but is not currently mounted.
                 * ignore the error and continue. */
                SLOGE("Failed to unmount volume %s\n", vol_list[i].label);
                goto error_unencrypted;
            }
        }
    }

    property_set("mux.report.case", "3");
    property_set("ctl.start", "muxreport-daemon"); 
    SLOGD("Just asked init to stop modem\n");

	property_set("ctl.class_reset", "default"); 
    /* The init files are setup to stop the class main and late start when
     * vold sets trigger_shutdown_framework.
     */
    property_set("vold.decrypt", "trigger_shutdown_framework");
    SLOGD("Just asked init to shut down class main\n");

    property_get("ro.crypto.fuse_sdcard", fuse_sdcard, "");
    if (!strcmp(fuse_sdcard, "true")) {
        /* This is a device using the fuse layer to emulate the sdcard semantics
         * on top of the userdata partition.  vold does not manage it, it is managed
         * by the sdcard service.  The sdcard service was killed by the property trigger
         * above, so just unmount it now.  We must do this _AFTER_ killing the framework,
         * unlike the case for vold managed devices above.
         */
        if (wait_and_unmount(sd_mnt_point)) {
            goto error_shutting_down;
        }
    }

    //property_set("vold.decrypt", "trigger_stop_modem_and_default"); 
    //SLOGD("Just triggered trigger_stop_modem_and_default to avoid modem EE and umount /data fail\n");

    /* Now unmount the /data partition. */
    if (wait_and_unmount(DATA_MNT_POINT)) {
        goto error_shutting_down;
    }

    /* Do extra work for a better UX when doing the long inplace encryption */
    if (how == CRYPTO_ENABLE_INPLACE) {
        /* Now that /data is unmounted, we need to mount a tmpfs
         * /data, set a property saying we're doing inplace encryption,
         * and restart the framework.
         */
        property_get("ro.crypto.tmpfs_options", tmpfs_options, "");
        if (mount("tmpfs", DATA_MNT_POINT, "tmpfs", MS_NOATIME | MS_NOSUID | MS_NODEV,
            tmpfs_options) < 0) {
            goto error_shutting_down;
        }
        /* Tells the framework that inplace encryption is starting */
        property_set("vold.encrypt_progress", "0");

        /* restart the framework. */
        /* Create necessary paths on /data */
        if (prep_data_fs()) {
            goto error_shutting_down;
        }

        //start modem in order to make emergency call during screen lock
        property_set("mux.report.case", "4");
        property_set("ctl.start", "muxreport-daemon"); 
        SLOGD("Just asked init to start modem\n");

        /* startup service classes main and late_start */
        property_set("vold.decrypt", "trigger_restart_min_framework");
        SLOGD("Just triggered restart_min_framework\n");

        /* OK, the framework is restarted and will soon be showing a
         * progress bar.  Time to setup an encrypted mapping, and
         * either write a new filesystem, or encrypt in place updating
         * the progress bar as we work.
         */
    }

    /* Start the actual work of making an encrypted filesystem */
    /* Initialize a crypt_mnt_ftr for the partition */
    cryptfs_init_crypt_mnt_ftr(&crypt_ftr);
    if (!strcmp(key_loc, KEY_IN_FOOTER)) {
        crypt_ftr.fs_size = nr_sec - (CRYPT_FOOTER_OFFSET / 512);
    } else {
        crypt_ftr.fs_size = nr_sec;
    }
    crypt_ftr.flags |= CRYPT_ENCRYPTION_IN_PROGRESS;
    strcpy((char *)crypt_ftr.crypto_type_name, "aes-cbc-essiv:sha256");

    /* Make an encrypted master key */
    if (create_encrypted_random_key(passwd, master_key, salt)) {
        SLOGE("Cannot create encrypted master key\n");
        goto error_unencrypted;
    }

    /* Write the key to the end of the partition */
    put_crypt_ftr_and_key(real_blkdev, &crypt_ftr, master_key, salt);

    decrypt_master_key(passwd, salt, master_key, decrypted_master_key);
    create_crypto_blk_dev(&crypt_ftr, decrypted_master_key, real_blkdev, crypto_blkdev,
                          "userdata");

    /* The size of the userdata partition, and add in the vold volumes below */
    tot_encryption_size = crypt_ftr.fs_size;

    /* setup crypto mapping for all encryptable volumes handled by vold */
    for (i=0; i<num_vols; i++) {
        if (should_encrypt(&vol_list[i])) {
            vol_list[i].crypt_ftr = crypt_ftr; /* gotta love struct assign */
            vol_list[i].crypt_ftr.fs_size = vol_list[i].size;
            create_crypto_blk_dev(&vol_list[i].crypt_ftr, decrypted_master_key,
                                  vol_list[i].blk_dev, vol_list[i].crypto_blkdev,
                                  vol_list[i].label);
            tot_encryption_size += vol_list[i].size;
        }
    }

    if (how == CRYPTO_ENABLE_WIPE) {
        rc = cryptfs_enable_wipe(crypto_blkdev, crypt_ftr.fs_size, EXT4_FS);
        /* Encrypt all encryptable volumes handled by vold */
        if (!rc) {
            for (i=0; i<num_vols; i++) {
                if (should_encrypt(&vol_list[i])) {
                    rc = cryptfs_enable_wipe(vol_list[i].crypto_blkdev,
                                             vol_list[i].crypt_ftr.fs_size, FAT_FS);
                }
            }
        }
    } else if (how == CRYPTO_ENABLE_INPLACE) {
        rc = cryptfs_enable_inplace(crypto_blkdev, real_blkdev, crypt_ftr.fs_size,
                                    &cur_encryption_done, tot_encryption_size);
        /* Encrypt all encryptable volumes handled by vold */
        if (!rc) {
            for (i=0; i<num_vols; i++) {
                if (should_encrypt(&vol_list[i])) {
                    rc = cryptfs_enable_inplace(vol_list[i].crypto_blkdev,
                                                vol_list[i].blk_dev,
                                                vol_list[i].crypt_ftr.fs_size,
                                                &cur_encryption_done, tot_encryption_size);
                }
            }
        }
        if (!rc) {
            /* The inplace routine never actually sets the progress to 100%
             * due to the round down nature of integer division, so set it here */
            property_set("vold.encrypt_progress", "100");
        }
    } else {
        /* Shouldn't happen */
        SLOGE("cryptfs_enable: internal error, unknown option\n");
        goto error_unencrypted;
    }

    /* Undo the dm-crypt mapping whether we succeed or not */
    delete_crypto_blk_dev("userdata");
    for (i=0; i<num_vols; i++) {
        if (should_encrypt(&vol_list[i])) {
            delete_crypto_blk_dev(vol_list[i].label);
        }
    }

    free(vol_list);

    if (! rc) {
        /* Success */

        /* Clear the encryption in progres flag in the footer */
        crypt_ftr.flags &= ~CRYPT_ENCRYPTION_IN_PROGRESS;
        put_crypt_ftr_and_key(real_blkdev, &crypt_ftr, 0, 0);

#ifdef MTK_EMMC_SUPPORT
        {
            struct phone_encrypt_state ps;
            ps.state = PHONE_ENCRYPTED;
            if (misc_set_phone_encrypt_state(&ps) < 0) {
                SLOGE("Failed to set encrypted status to 0x%x in MISC\n", ps.state);
            }
            else {
                SLOGD("Success: Set encrypted status to 0x%x in MISC\n", ps.state);
            }
        }
#endif
        
        sleep(2); /* Give the UI a chance to show 100% progress */
        android_reboot(ANDROID_RB_RESTART, 0, 0);
    } else {
        property_set("vold.encrypt_progress", "error_partially_encrypted");
        release_wake_lock(lockid);
        return -1;
    }

    /* hrm, the encrypt step claims success, but the reboot failed.
     * This should not happen.
     * Set the property and return.  Hope the framework can deal with it.
     */
    property_set("vold.encrypt_progress", "error_reboot_failed");
    release_wake_lock(lockid);
    return rc;

error_unencrypted:
    free(vol_list);
    property_set("vold.encrypt_progress", "error_not_encrypted");
    if (lockid[0]) {
        release_wake_lock(lockid);
    }
    return -1;

error_shutting_down:
    /* we failed, and have not encrypted anthing, so the users's data is still intact,
     * but the framework is stopped and not restarted to show the error, so it's up to
     * vold to restart the system.
     */
    SLOGE("Error enabling encryption after framework is shutdown, no data changed, restarting system");
    android_reboot(ANDROID_RB_RESTART, 0, 0);

    /* shouldn't get here */
    property_set("vold.encrypt_progress", "error_shutting_down");
    free(vol_list);
    if (lockid[0]) {
        release_wake_lock(lockid);
    }
    return -1;
}

int cryptfs_changepw(char *newpw)
{
    struct crypt_mnt_ftr crypt_ftr;
    unsigned char encrypted_master_key[KEY_LEN_BYTES], decrypted_master_key[KEY_LEN_BYTES];
    unsigned char salt[SALT_LEN];
    char real_blkdev[MAXPATHLEN];

    /* This is only allowed after we've successfully decrypted the master key */
    if (! master_key_saved) {
        SLOGE("Key not saved, aborting");
        return -1;
    }

    property_get("ro.crypto.fs_real_blkdev", real_blkdev, "");
    if (strlen(real_blkdev) == 0) {
        SLOGE("Can't find real blkdev");
        return -1;
    }

    /* get key */
    if (get_crypt_ftr_and_key(real_blkdev, &crypt_ftr, encrypted_master_key, salt)) {
      SLOGE("Error getting crypt footer and key");
      return -1;
    }

    encrypt_master_key(newpw, salt, saved_master_key, encrypted_master_key);

    /* save the key */
    put_crypt_ftr_and_key(real_blkdev, &crypt_ftr, encrypted_master_key, salt);

    return 0;
}
