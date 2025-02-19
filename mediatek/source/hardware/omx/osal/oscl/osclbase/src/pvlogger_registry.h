/* Copyright Statement:
 *
 * This software/firmware and related documentation ("MediaTek Software") are
 * protected under relevant copyright laws. The information contained herein is
 * confidential and proprietary to MediaTek Inc. and/or its licensors. Without
 * the prior written permission of MediaTek inc. and/or its licensors, any
 * reproduction, modification, use or disclosure of MediaTek Software, and
 * information contained herein, in whole or in part, shall be strictly
 * prohibited.
 * 
 * MediaTek Inc. (C) 2010. All rights reserved.
 * 
 * BY OPENING THIS FILE, RECEIVER HEREBY UNEQUIVOCALLY ACKNOWLEDGES AND AGREES
 * THAT THE SOFTWARE/FIRMWARE AND ITS DOCUMENTATIONS ("MEDIATEK SOFTWARE")
 * RECEIVED FROM MEDIATEK AND/OR ITS REPRESENTATIVES ARE PROVIDED TO RECEIVER
 * ON AN "AS-IS" BASIS ONLY. MEDIATEK EXPRESSLY DISCLAIMS ANY AND ALL
 * WARRANTIES, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
 * NONINFRINGEMENT. NEITHER DOES MEDIATEK PROVIDE ANY WARRANTY WHATSOEVER WITH
 * RESPECT TO THE SOFTWARE OF ANY THIRD PARTY WHICH MAY BE USED BY,
 * INCORPORATED IN, OR SUPPLIED WITH THE MEDIATEK SOFTWARE, AND RECEIVER AGREES
 * TO LOOK ONLY TO SUCH THIRD PARTY FOR ANY WARRANTY CLAIM RELATING THERETO.
 * RECEIVER EXPRESSLY ACKNOWLEDGES THAT IT IS RECEIVER'S SOLE RESPONSIBILITY TO
 * OBTAIN FROM ANY THIRD PARTY ALL PROPER LICENSES CONTAINED IN MEDIATEK
 * SOFTWARE. MEDIATEK SHALL ALSO NOT BE RESPONSIBLE FOR ANY MEDIATEK SOFTWARE
 * RELEASES MADE TO RECEIVER'S SPECIFICATION OR TO CONFORM TO A PARTICULAR
 * STANDARD OR OPEN FORUM. RECEIVER'S SOLE AND EXCLUSIVE REMEDY AND MEDIATEK'S
 * ENTIRE AND CUMULATIVE LIABILITY WITH RESPECT TO THE MEDIATEK SOFTWARE
 * RELEASED HEREUNDER WILL BE, AT MEDIATEK'S OPTION, TO REVISE OR REPLACE THE
 * MEDIATEK SOFTWARE AT ISSUE, OR REFUND ANY SOFTWARE LICENSE FEES OR SERVICE
 * CHARGE PAID BY RECEIVER TO MEDIATEK FOR SUCH MEDIATEK SOFTWARE AT ISSUE.
 *
 * The following software/firmware and/or related documentation ("MediaTek
 * Software") have been modified by MediaTek Inc. All revisions are subject to
 * any receiver's applicable license agreements with MediaTek Inc.
 */

/* ------------------------------------------------------------------
 * Copyright (C) 1998-2009 PacketVideo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 * -------------------------------------------------------------------
 */
#ifndef PVLOGGER_REGISTRY_H_INCLUDED
#define PVLOGGER_REGISTRY_H_INCLUDED

#ifndef PVLOGGER_H_INCLUDED
#include "pvlogger.h"
#endif

#ifndef OSCL_TAGTREE_H_INCLUDED
#include "oscl_tagtree.h"
#endif

/**
 * Class: PVLoggerRegistry
 *
 * PVLoggerRegistry class, maintains a repository of all the loggers, along with
 * their associated tags, in a tag tree. Any request for a log control point is
 * serviced by this class.
 *
 * Memory Ownership: Creates log control points for each tag, and holds these
 * pointers in the tag tree. PVLogger registry is responsible for calling the
 * destructor on each of these loggers.
 *
 */

class PVLoggerRegistry
{
    public:

        typedef PVLogger::log_level_type log_level_type;
        typedef PVLogger::alloc_type alloc_type;

        /**
         * Get the logger registry.  There is only one logger
         * registry instance per thread.
         */
        OSCL_IMPORT_REF static PVLoggerRegistry* GetPVLoggerRegistry();

        /**
         * PVLoggerRegistry Constructor
         *
         */
        OSCL_IMPORT_REF PVLoggerRegistry();

        /**
         * PVLoggerRegistry Destructor
         *
         */
        OSCL_IMPORT_REF virtual ~PVLoggerRegistry();

        /**
         * PVLoggerRegistry method to get access to a logging control point, associated with
         * a tag. In case the logger for this tag does not exist, it is created afresh, else
         * pointer to the existing one is returned.
         *
         * @param inputTag   logger tag, viz. "x.y.z"
         * @param level      log level associated with the logging control point
         *
         * @param oAppenderInheritance
         *
         * @return PVLogger<Alloc, TheLock>* Pointer to the logging control point
         */
        OSCL_IMPORT_REF PVLogger *GetPVLoggerObject(const char* tagIn);

        /**
         * This method creates a log control point, with specified tag, and level
         *
         * @param inputTag   logger tag, viz. "x.y.z"
         * @param level      log level associated with the logging control point
         *
         * @param oAppenderInheritance
         *
         * @return PVLogger<alloc_type, TheLock>* Pointer to the logging control point
         */
        OSCL_IMPORT_REF PVLogger *CreatePVLogger(const char* tagIn, log_level_type level, bool oAppenderInheritance);

        /**
         * This method propagates the log level to all the descendents of the node,
         * with a specified tag.
         *
         * @param tagIn      logger tag, viz. "x.y.z"
         * @param level      log level associated with the logging control point
         *
         * @return           true on success, else false.
         */
        OSCL_IMPORT_REF bool SetNodeLogLevelExplicit(char* tagIn, log_level_type level);

        /**
         * This method recursively propagates the log level to all the descendents,
         * of a node.
         *
         * @param node       Node ptr, associated with a logger, from the tag tree.
         * @param level      log level associated with the logging control point
         *
         * @return           NONE
         */
        OSCL_IMPORT_REF void SetNodeLogLevelExplicit(Oscl_TagTree<PVLogger*, alloc_type>::node_type* node, log_level_type level);

    private:
#if (PVLOGGER_ENABLE)
        Oscl_TAlloc<PVLogger, alloc_type> _pvloggerAlloc;
        Oscl_TagTree<PVLogger*, alloc_type> _loggerTree;
#endif
};

#endif // PVLOGGER_REGISTRY_H_INCLUDED

