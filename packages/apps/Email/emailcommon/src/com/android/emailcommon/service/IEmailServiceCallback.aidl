/*
 * Copyright (C) 2008-2009 Marc Blank
 * Licensed to The Android Open Source Project.
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

package com.android.emailcommon.service;

oneway interface IEmailServiceCallback {
    /*
     * Ordinary results:
     *   statuscode = 1, progress = 0:      "starting"
     *   statuscode = 0, progress = n/a:    "finished"
     *
     * If there is an error, it must be reported as follows:
     *   statuscode = err, progress = n/a:  "stopping due to error"
     *
     * *Optionally* a callback can also include intermediate values from 1..99 e.g.
     *   statuscode = 1, progress = 0:      "starting"
     *   statuscode = 1, progress = 30:     "working"
     *   statuscode = 1, progress = 60:     "working"
     *   statuscode = 0, progress = n/a:    "finished"
     */

    /**
     * Callback to indicate that an account is being synced (updating folder list)
     * accountId = the account being synced
     * statusCode = 0 for OK, 1 for progress, other codes for error
     * progress = 0 for "start", 1..100 for optional progress reports
     */
    void syncMailboxListStatus(long accountId, int statusCode, int progress);

    /**
     * Callback to indicate that a mailbox is being synced
     * mailboxId = the mailbox being synced
     * statusCode = 0 for OK, 1 for progress, other codes for error
     * progress = 0 for "start", 1..100 for optional progress reports
     */
    void syncMailboxStatus(long mailboxId, int statusCode, int progress);

    /**
     * Callback to indicate that a particular attachment is being synced
     * messageId = the message that owns the attachment
     * attachmentId = the attachment being synced
     * statusCode = 0 for OK, 1 for progress, other codes for error
     * progress = 0 for "start", 1..100 for optional progress reports
     */
    void loadAttachmentStatus(long messageId, long attachmentId, int statusCode, int progress);

    /**
     * Callback to indicate that a particular message is being sent
     * messageId = the message being sent
     * statusCode = 0 for OK, 1 for progress, other codes for error
     * progress = 0 for "start", 1..100 for optional progress reports
     */
    void sendMessageStatus(long messageId, String subject, int statusCode, int progress);

    /**
     * Callback to indicate that a particular message was fetched
     * messageId = the message being fetched
     * statusCode = 0 for OK, 1 for progress, other codes for error
     * progress = 0 for "start", 1..100 for optional progress reports
     */
    void fetchMessageCallBack(long messageId, int statusCode, int progress);
}
