/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2008
*
*  BY OPENING THIS FILE, BUYER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
*  THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
*  RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO BUYER ON
*  AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL WARRANTIES,
*  EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED WARRANTIES OF
*  MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NONINFRINGEMENT.
*  NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH RESPECT TO THE
*  SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY, INCORPORATED IN, OR
*  SUPPLIED WITH THE MEDIATEK SOFTWARE, AND BUYER AGREES TO LOOK ONLY TO SUCH
*  THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO. MEDIATEK SHALL ALSO
*  NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE RELEASES MADE TO BUYER'S
*  SPECIFICATION OR TO CONFORM TO A PARTICULAR STANDARD OR OPEN FORUM.
*
*  BUYER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S ENTIRE AND CUMULATIVE
*  LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE RELEASED HEREUNDER WILL BE,
*  AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE MEDIATEK SOFTWARE AT ISSUE,
*  OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE CHARGE PAID BY BUYER TO
*  MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
*
*  THE TRANSACTION CONTEMPLATED HEREUNDER SHALL BE CONSTRUED IN ACCORDANCE
*  WITH THE LAWS OF THE STATE OF CALIFORNIA, USA, EXCLUDING ITS CONFLICT OF
*  LAWS PRINCIPLES.  ANY DISPUTES, CONTROVERSIES OR CLAIMS ARISING THEREOF AND
*  RELATED THERETO SHALL BE SETTLED BY ARBITRATION IN SAN FRANCISCO, CA, UNDER
*  THE RULES OF THE INTERNATIONAL CHAMBER OF COMMERCE (ICC).
*
*****************************************************************************/
/*****************************************************************************
 *
 * Filename:
 * ---------
 *   meta_fm.h
 *
 * Project:
 * --------
 *   YUSU
 *
 * Description:
 * ------------
 *   FM meta implement.
 *
 * Author:
 * -------
 *  LiChunhui (MTK80143)
 *
 *============================================================================
 *             HISTORY
 * Below this line, this part is controlled by CC/CQ. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision:$
 * $Modtime:$
 * $Log:$
 *
 * 07 03 2012 vend_am00076
 * [ALPS00269605] [MP Feature Patch Back]Shared sdcard feature support
 * shared sdcard --meta mode
 *
 * 03 12 2012 vend_am00076
 * [ALPS00251394] [Patch Request]
 * .
 *
 * 03 02 2012 vend_am00076
 * NULL
 * .
 *
 * 01 26 2011 hongcheng.xia
 * [ALPS00030208] [Need Patch] [Volunteer Patch][MT6620 FM]enable FM Meta mode
 * .
 *
 * 11 18 2010 hongcheng.xia
 * [ALPS00135614] [Need Patch] [Volunteer Patch]MT6620 FM Radio code check in
 * .
 *
 * 11 16 2010 hongcheng.xia
 * [ALPS00135614] [Need Patch] [Volunteer Patch]MT6620 FM Radio code check in
 * .
 *
 * 11 15 2010 hongcheng.xia
 * [ALPS00135614] [Need Patch] [Volunteer Patch]MT6620 FM Radio code check in
 * .
 *
 * 11 15 2010 hongcheng.xia
 * [ALPS00135614] [Need Patch] [Volunteer Patch]MT6620 FM Radio code check in
 * .
 *
 * 08 28 2010 chunhui.li
 * [ALPS00123709] [Bluetooth] meta mode check in
 * for FM meta enable

 *
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by CC/CQ. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/
#include <stdio.h>   /* Standard input/output definitions */
#include <string.h>  /* String function definitions */
#include <unistd.h>  /* UNIX standard function definitions */
#include <fcntl.h>   /* File control definitions */
#include <errno.h>   /* Error number definitions */
#include <termios.h> /* POSIX terminal control definitions */
#include <time.h>
#include <pthread.h>
#include <stdlib.h>
#include <signal.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <sys/socket.h>
#include <sys/epoll.h>
#include <sys/reboot.h>
#include <sys/mount.h>
#include <utils/Log.h>
#include <dirent.h>
#include "FT_Public.h"
#include "meta_common.h"
#include "mounts.h"
#include "meta_clr_emmc_para.h"
#include "make_ext4fs.h"

#undef  LOG_TAG
#define LOG_TAG  "CLR_EMMC_META"
#define DATA_PARTITION "/data"
#define CACHE_PARTITION "/cache"

extern BOOL WriteDataToPC(void *Local_buf,unsigned short Local_len,void *Peer_buf,unsigned short Peer_len);



/********************************************************************************
//FUNCTION:
//		META_CLR_EMMC_init
//DESCRIPTION:
//		EMMC Init for META test.
//
//PARAMETERS:
//		void
//RETURN VALUE:
//		true : success
//      false: failed
//
********************************************************************************/
bool META_CLR_EMMC_init()
{
	LOGD("META_CLR_EMMC_INIT ...\n");
	return 1;
}

/********************************************************************************
//FUNCTION:
//		META_CLR_EMMC_deinit
//DESCRIPTION:
//		EMMC deinit for META test.
//
//PARAMETERS:
//		void
//RETURN VALUE:
//		void
//     
********************************************************************************/
void META_CLR_EMMC_deinit()
{
	LOGD("META_CLR_EMMC_DEINIT ...\n");
	return;   
}
#if 0
int ensure_path_mounted(const char* path) {
    int result;
    result = scan_mounted_volumes();
    if (result < 0) {
        LOGE("failed to scan mounted volumes\n");
        return -1;
    }

    const MountedVolume* mv = find_mounted_volume_by_mount_point("/data");
    const MountedVolume* m_v = find_mounted_volume_by_mount_point("/cache");
    if (mv && m_v) {
        // volume is already mounted
        return 0;
    }

    if (!mv) {
	    result = mount("/emmc@userdata", "/data", "ext4",
                       MS_NOATIME | MS_NODEV | MS_NODIRATIME, "");
	    if (result < 0) {
  		   LOGE("failed to mount /data (%s)\n", strerror(errno));
		   return -1;
	    }
    }
    if (!m_v) {
	    result = mount("/emmc@cache", "/cache", "ext4",
                       MS_NOATIME | MS_NODEV | MS_NODIRATIME, "");
	    if (result < 0) {
  		   LOGE("failed to mount /cache (%s)\n", strerror(errno));
		   return -1;
	    }
    }
    if (result == 0) 
	 	return 0;
}
#endif

int ensure_root_path_unmounted(const char *root_path)
{
    /* See if this root is already mounted. */
    int ret = scan_mounted_volumes();
    
    if (ret < 0) 
    {
        return ret;
    }

    const MountedVolume *volume;
    volume = find_mounted_volume_by_mount_point(root_path);

    if (volume == NULL) 
    {
        /* It's not mounted. */
        LOGD(LOG_TAG "The path %s is unmounted\n", root_path);
        return 0;
    }

    return unmount_mounted_volume(volume);
}

int format_root_device(const char *root)
{
    /* Don't try to format a mounted device. */
    int ret = ensure_root_path_unmounted(root);  
    if (ret < 0) {
        LOGD(LOG_TAG "format_root_device: can't unmount \"%s\"\n", root);
        return -1;
    }

    if (!strcmp(root, "/data")) {
	  int result = make_ext4fs("/emmc@usrdata", 0);
	  if (result != 0) {	
		LOGE("format_volume: make_extf4fs failed on /emmc@usrdata\n");
	    	return -1;
    	  }
    }	else if (!strcmp(root, "/cache")) {
    	
	   int  result = make_ext4fs("/emmc@cache", 0);
	   if (result != 0) {	
			LOGE("format_volume: make_extf4fs failed on /emmc@cache\n");
	    		return -1;
    	   }

    }

    return 0;
}


int clear_emmc_entry()
{
    int result = 0;
    LOGD("before clear emmc ...\n");
    result = format_root_device(DATA_PARTITION);
    
    sync();
    LOGD("after clear DATA %s, %d...\n", DATA_PARTITION, result);
    result = format_root_device(CACHE_PARTITION);

    LOGD("after clear CACHE %s, %d...\n", DATA_PARTITION, result);
	
    sync();
    return result;
}


int clear_emmc_nomedia_entry(char *path)
{
	  int result = 0;
	  DIR* d;
	  struct dirent* de;
	  d = opendir(path);
	  if (d == NULL) {
		   LOGE("error opening %s: %s\n", path, strerror(errno));
	   }
	  int alloc_len = 10;
          char *files = malloc(alloc_len + 30);
		   
	  while ((de = readdir(d)) != NULL) {
		   int name_len = strlen(de->d_name);
	   
		   if (de->d_type == DT_DIR) {
			   // skip "." and ".." entries
			   if (name_len == 1 && de->d_name[0] == '.') continue;
			   if (name_len == 2 && de->d_name[0] == '.' &&
				   de->d_name[1] == '.') continue;
	   		   if (name_len == 5 && strcmp(de->d_name, "media") == 0) continue;
		   }
		   if (name_len >= alloc_len) {
		  	files = realloc(files, (name_len + 30) * sizeof(char));
		   }
		   strcpy(files, "/system/bin/rm -r /data/");
		   strcat(files, de->d_name);		   
		   LOGD("the file is %s\n", de->d_name);
		   if (system(files)) {
		   	LOGE("cant rm file %s,error %s\n", de->d_name, strerror(errno));
			return -1;
		   }  	
	   }

	   closedir(d);
		
	   result = format_root_device(CACHE_PARTITION);
		
	   LOGD("after clear CACHE %s, %d...\n", CACHE_PARTITION, result);
		
	   sync();
	   return 0;

}
#ifdef MTK_SHARED_SDCARD
int format_internal_sd_partition()
{
    int result = 0;
    result = system("/system/bin/rm -r /data/media");    
    if (result) {
   	  LOGE("can NOT rm /data/media\n");
	   return result;
    }
    sync();
	return result;
}

#else

int format_internal_sd_partition()
{
    pid_t pid;
    if ((pid = fork()) < 0) 
    {
        LOGE("eMMC: fork fails: %d (%s)\n", errno, strerror(errno));
        return (-2);
    } 
    else if (pid == 0)  /*child process*/
    {
        int err;
        err = execl("/system/bin/newfs_msdos", "newfs_msdos", "/emmc@fat", NULL);
        exit(-3) ;
    } 
    else  /*parent process*/
    {
        int child_stat ;
        waitpid(pid, &child_stat, 0) ;
        sync();
        if (WIFEXITED(child_stat) && (WEXITSTATUS(child_stat) == 0)) {
            LOGD("eMMC: pid = %d\n", pid);
        }
        else {
            LOGE("eMMC: execl error: %s\n", strerror(errno));
            return -1;
        }
    }
    return 0;

}
#endif

int clear_emmc_internal_sd()
{
    int result = 0;
    LOGD("before clear internal sd ...\n");
    result = format_internal_sd_partition();    
    LOGD("after clear internal sd, %d...\n", result);
    
    sync();
    return result;
}


/********************************************************************************
//FUNCTION:
//		META_FM_OP
//DESCRIPTION:
//		META FM test main process function.
//
//PARAMETERS:
//		req: FM Req struct
//      peer_buff: peer buffer pointer
//      peer_len: peer buffer length
//RETURN VALUE:
//		void
//      
********************************************************************************/
void META_CLR_EMMC_OP(FT_EMMC_REQ *req) 
{
	LOGD("req->op:%d\n", req->op); 
	int ret = 0;
	FT_EMMC_CNF emmc_cnf;
	memcpy(&emmc_cnf, req, sizeof(FT_H) + sizeof(FT_EMMC_OP));
	emmc_cnf.header.id ++; 
	switch (req->op) {
	        case FT_EMMC_OP_CLEAR:
			ret = clear_emmc_entry();
			LOGD("clr emmc clear ret is %d\n", ret);
			emmc_cnf.m_status = META_SUCCESS;
			if (!ret) { 				
				emmc_cnf.result.clear_cnf.status = 1;
			} else {
				emmc_cnf.result.clear_cnf.status = 0;
			}
			WriteDataToPC(&emmc_cnf, sizeof(FT_EMMC_CNF), NULL, 0);
			reboot(RB_AUTOBOOT); 
			break;

          case FT_EMMC_OP_FORMAT_TCARD:
              ret = clear_emmc_internal_sd();
              LOGD("clr emmc clear internal sd ret is %d\n", ret);
              emmc_cnf.m_status = META_SUCCESS;
              if (!ret) {                 
                  emmc_cnf.result.form_tcard_cnf.status = 1;
              } else {
                  emmc_cnf.result.form_tcard_cnf.status = 0;
              }
              WriteDataToPC(&emmc_cnf, sizeof(FT_EMMC_CNF), NULL, 0);
              break;

	     case FT_EMMC_OP_CLEAR_WITHOUT_TCARD:
		  	ret = clear_emmc_nomedia_entry("/data");

			LOGD("clear emmc no mediaret is %d\n", ret);
			emmc_cnf.m_status = META_SUCCESS;
			if (!ret) { 				
				emmc_cnf.result.clear_without_tcard_cnf.status = 1;
			} else {
				emmc_cnf.result.clear_without_tcard_cnf.status = 0;
			}
			WriteDataToPC(&emmc_cnf, sizeof(FT_EMMC_CNF), NULL, 0);
			reboot(RB_AUTOBOOT); 
			
			LOGD("success to call 143 op 3\n");
			break;
	      default:
		  	emmc_cnf.m_status = META_SUCCESS;
			emmc_cnf.result.clear_cnf.status = META_STATUS_FAILED;
			WriteDataToPC(&emmc_cnf, sizeof(FT_EMMC_CNF), NULL, 0);
			break;
	}		
}

