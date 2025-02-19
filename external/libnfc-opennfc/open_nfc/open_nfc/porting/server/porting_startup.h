/*
 * Copyright (c) 2007-2010 Inside Secure, All Rights Reserved.
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

  Implementation of the server porting procedures

*******************************************************************************/

#include "nfc_hal.h"

void * StartNFCC(tNALBinding * pBinding, void * pPortingConfig);

int 	 WaitForNFCCBoot(void * ctx);

int 	 StartAcceptingClientConnections(void * ctx);

int 	 StopAcceptingClientConnections(void * ctx);

int     CloseAllClientConnections(void * ctx);

int	 StopNFCC(void * ctx);





