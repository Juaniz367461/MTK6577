/*****************************************************************************
*  Copyright Statement:
*  --------------------
*  This software is protected by Copyright and the information contained
*  herein is confidential. The software may not be copied and the information
*  contained herein may not be used or disclosed except with the written
*  permission of MediaTek Inc. (C) 2005
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

 /******************************************************************************
 * Filename:
 * ---------
 *   rrlp_l4c_hdlrs.h
 *
 * Project:
 * -------- 
 *   MAUI
 *
 * Description:
 * ------------
 *
 * Author:
 * -------
 * SH Yang (mtk01600)
 *
 *-----------------------------------------------------------------------------
 * $Log$
 *
 * 06 19 2012 archilis.wang
 * [ALPS00303520] [Merge] Level 3 SW modules ���ɦ� Level 2 on ALPS.ICS2.MP
 * [Merge] Level 3 SW modules ���ɦ� Level 2 on ALPS.ICS2.MP
 *
 * Dec 26 2008 mtk01600
 * [MAUI_01305588] New Cell-ID arch check-in
 * 
 *
 * 
 *
 *
 ******************************************************************************/

/********************************************************************************
*  Copyright Statement:
*  --------------------
*
*  This product has been developed using a protocol stack
*  developed by MediaTek Inc.
*
********************************************************************************/


#ifndef _RRLP_RR_HDLRS_H
#define _RRLP_RR_HDLRS_H

/* interface to L4C */
#include "as2l4c_struct.h" 
#include "l3_inc_enums.h"

/*****************************************************************************
 * exported function prototypes
 *****************************************************************************/
extern void rrlp_l4c_msg_hdlr(ilm_struct *pIlm);

extern void rrlp_l4c_nbr_cell_info_reg_cnf_hdlr(local_para_struct * local_para_ptr,
	peer_buff_struct *peer_buff_ptr);

extern void rrlp_l4c_nbr_cell_info_dereg_cnf_hdlr(local_para_struct * local_para_ptr,
	peer_buff_struct *peer_buff_ptr);

extern void rrlp_l4c_nbr_cell_info_ind_hdlr(local_para_struct * local_para_ptr,
	peer_buff_struct *peer_buff_ptr);


extern gas_nbr_cell_info_struct *rrlp_get_data_buf_from_head(void);
extern void rrlp_free_data_buf_insert_to_tail(gas_nbr_cell_info_struct *cell);

#endif /* _RRLP_L4C_HDLRS_H */
