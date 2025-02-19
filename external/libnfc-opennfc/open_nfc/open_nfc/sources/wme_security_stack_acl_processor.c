/*
 * Copyright (c) 2011-2012 Inside Secure, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*******************************************************************************
   Contains the implementation of the Security Stack ACL Processor functions
*******************************************************************************/

#define P_MODULE  P_MODULE_DEC( SECSTACK )

#include "wme_context.h"

#if ( (P_BUILD_CONFIG == P_CONFIG_DRIVER) || (P_BUILD_CONFIG == P_CONFIG_MONOLITHIC) ) && (defined P_INCLUDE_SE_SECURITY)

#include "wme_security_stack_acl_processor.h"
#include "wme_security_stack_acl_processor_gp.h"
#include "wme_security_stack_acl_processor_pkcs15.h"

/** @brief The list of the ACL processors to be checked one after the other */
tPSecurityStackAclProcessorInterface* g_PSecurityStackAclProcessors[] =
{
   &PSecurityStackGpAclProcessor,
   &PSecurityStackPkcs15AclProcessor,

   /* Use a null pointer as the end-of-table marker */
   (tPSecurityStackAclProcessorInterface*)null
};

#endif /* (P_CONFIG_DRIVER || P_CONFIG_MONOLITHIC) && P_INCLUDE_SE_SECURITY */
