/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2007
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

/*******************************************************************************
 * Filename:
 * ---------
 *  gps_sm.h
 *
 * Project:
 * --------
 *  MAUI
 *
 * Description:
 * ------------
 *  GPS task
 *
 * Author:
 * -------
 *  James Liu (mtk01206)
 *
 *==============================================================================
 *             HISTORY
 * Below this line, this part is controlled by PVCS VM. DO NOT MODIFY!! 
 *------------------------------------------------------------------------------
 * $Log$
 *
 * 06 19 2012 archilis.wang
 * [ALPS00303520] [Merge] Level 3 SW modules ���ɦ� Level 2 on ALPS.ICS2.MP
 * [Merge] Level 3 SW modules ���ɦ� Level 2 on ALPS.ICS2.MP
 *
 * Feb 13 2008 mtk01206
 * [MAUI_00618179] [GPS][Modify] Modify MT3316/MT3318 authentication process and NVRAM access scenario
 * Move wakeup command from EINT HISR to GPS Task
 *
 * Nov 19 2007 mtk01206
 * [MAUI_00574860] [GPS][Modify] Update some GPS task files
 * 
 *
 * Nov 12 2007 mtk01206
 * [MAUI_00574860] [GPS][Modify] Update some GPS task files
 * 
 *
 * Nov 5 2007 mtk01206
 * [MAUI_00571439] [GPS][New] Add new GPS task for QC
 * 
 *
 *------------------------------------------------------------------------------
 * Upper this line, this part is controlled by PVCS VM. DO NOT MODIFY!! 
 *==============================================================================
 *******************************************************************************/
/***************************************************************************** 
 * Include
 *****************************************************************************/
#ifdef __GPS_SUPPORT__

#ifndef _GPS_SM_H_
#define _GPS_SM_H_

extern void gps_uart_rx_sleep_handler(void);
extern void gps_uart_tx_sleep_handler(void);
extern void gps_host_wake_up_ind_handler(void);

#endif /* _GPS_SM_H_ */

#endif /* __GPS_SUPPORT__ */
