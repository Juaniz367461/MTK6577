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
 *   mtk_gps_agent.h
 *
 * Project:
 * --------
 *   Yusu
 *
 * Description:
 * ------------
 *    Prototype of MTK navigation library
 *
 * Author:
 * -------
 *  Mike, Chunhui, Hiki
 *
 *============================================================================
 *             HISTORY
 * Below this line, this part is controlled by CC/CQ. DO NOT MODIFY!!
 *------------------------------------------------------------------------------
 * $Revision:$
 * $Modtime:$
 * $Log:$
 *
 * 08 20 2010 qiuhuan.zhao
 * [ALPS00123522] [GPS] Android 2.2 porting
 * Android 2.2  GPS driver porting.
 * 
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by CC/CQ. DO NOT MODIFY!!
 *============================================================================
 ****************************************************************************/


#ifndef MTK_GPS_AGENT_H
#define MTK_GPS_AGENT_H

#ifdef __cplusplus
  extern "C" {
#endif

/* Copy required parameters from MNL internal header files.
   Remember to sync with MNL when any of these are changed. */

#define PMTK_MAX_PKT_LENGTH     256
#define MGPSID 32

// message between mnl and agps agent
#define MTK_GPS_EVENT_BASE          (0)
#define MTK_GPS_MSG_BASE            (MTK_GPS_EVENT_BASE+500)
#define MTK_GPS_MNL_MSG             (MTK_GPS_MSG_BASE+1) //msg to mnl
#define MTK_GPS_MNL_EPO_MSG         (MTK_GPS_MSG_BASE+2) //msg to agent epo module
#define MTK_GPS_MNL_BEE_MSG         (MTK_GPS_MSG_BASE+3) //msg to agent bee module
#define MTK_GPS_MNL_SUPL_MSG        (MTK_GPS_MSG_BASE+4) //msg to agent supl module
//#define MTK_GPS_MNL_EPO_MSG         (MTK_GPS_MSG_BASE+5) //msg to agent epo module
#define MTK_GPS_MNL_BEE_IND_MSG         (MTK_GPS_MSG_BASE+6) //msg to agent bee module
//#define MTK_GPS_MNL_SUPL_MSG        (MTK_GPS_MSG_BASE+7) //msg to agent supl module

#define AGPS_AGENT_DEBUG

//agps state machine
#define AGENT_ST_IDLE     (0)
#define AGENT_ST_WORKING  (1)

#define AGENT_WORK_MODE_IDLE          (0)
#define AGENT_WORK_MODE_EPO           (1)
#define AGENT_WORK_MODE_BEE           (2)
#define AGENT_WORK_MODE_SUPL_SI       (3)
#define AGENT_WORK_MODE_SUPL_NI       (4)
#define AGENT_WORK_MODE_WIFI_AIDING   (5)
#define AGENT_WORK_MODE_CELLID_AIDING (6)

/*****************************************************************************
 * FUNCTION
 *  mtk_gps_agent_init
 * DESCRIPTION
 *  Initialize AGPS Agent module
 * PARAMETERS
 *  void
 * RETURNS
 *  success(MTK_GPS_SUCCESS); failure (MTK_GPS_ERROR)
 *****************************************************************************/
mtk_int32
mtk_agps_agent_init();

/*****************************************************************************
 * FUNCTION
 *  mtk_agps_agent_proc
 * DESCRIPTION
 *  process the message recv. from agps agent thread
 * PARAMETERS
 *  prmsg       [IN]   the message recv. from agps agent thread
 * RETURNS
 *  success(MTK_GPS_SUCCESS)
 *****************************************************************************/
mtk_int32 
mtk_agps_agent_proc (mtk_gps_msg *prmsg);



#ifdef __cplusplus
   }
#endif

#endif /* MTK_GPS_H */

