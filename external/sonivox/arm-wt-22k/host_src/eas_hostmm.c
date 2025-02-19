/*----------------------------------------------------------------------------
 *
 * File:
 * eas_hostmm.c
 *
 * Contents and purpose:
 * This file contains the host wrapper functions for stdio, stdlib, etc.
 * This is a sample version that reads from a filedescriptor.
 * The file locator (EAS_FILE_LOCATOR) handle passed to
 * HWOpenFile is the same one that is passed to EAS_OpenFile.
 *
 * Modify this file to suit the needs of your particular system.
 *
 * EAS_MAX_FILE_HANDLES sets the maximum number of MIDI streams within
 * a MIDI type 1 file that can be played.
 *
 * EAS_HW_FILE is a structure to support the file I/O functions. It
 * comprises the file descriptor, the file read pointer, and
 * the dup flag, which when set, indicates that the file handle has
 * been duplicated, and offset and length within the file.
 *
 * Copyright 2005 Sonic Network Inc.

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
 *
 *----------------------------------------------------------------------------
 * Revision Control:
 *   $Revision: 795 $
 *   $Date: 2007-08-01 00:14:45 -0700 (Wed, 01 Aug 2007) $
 *----------------------------------------------------------------------------
*/

#ifdef _lint
#include "lint_stdlib.h"
#else
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <limits.h>
#include <sys/mman.h>
#include <errno.h>
#include <signal.h>
#define LOG_TAG "Sonivox"
#include <utils/Log.h>
#include <media/MediaPlayerInterface.h>
#endif

#ifdef MTK_DRM_APP
#include <DrmManagerClient_wrapper.h>
#endif

#include "eas_host.h"

/* Only for debugging LED, vibrate, and backlight functions */
#include "eas_report.h"

/* this module requires dynamic memory support */
#ifdef _STATIC_MEMORY
#error "eas_hostmm.c requires the dynamic memory model!\n"
#endif

#ifndef EAS_MAX_FILE_HANDLES
// 100 max file handles == 3 * (nb tracks(32) + 1 for the segment) + 1 for jet file
//                         3 == 1(playing segment) + 1(prepared segment)
//                              + 1(after end of playing segment, before files closed)
#define EAS_MAX_FILE_HANDLES    100
#endif

/*
 * this structure and the related function are here
 * to support the ability to create duplicate handles
 * and buffering it in memory. If your system uses
 * in-memory resources, you can eliminate the calls
 * to malloc and free, the dup flag, and simply track
 * the file size and read position.
 */
typedef struct eas_hw_file_tag
{
    EAS_I32 fileSize;
    EAS_I32 filePos;
    EAS_BOOL dup;
    int fd;
    EAS_I32 offset;

#ifdef MTK_DRM_APP
    DecryptHandle *mDecryptHandle;
    DrmManagerClient *mDrmManagerClient;
    int64_t mDrmBufOffset;
    int64_t mDrmBufSize;
    unsigned char *mDrmBuf;
#endif

} EAS_HW_FILE;

typedef struct eas_hw_inst_data_tag
{
    EAS_HW_FILE files[EAS_MAX_FILE_HANDLES];
} EAS_HW_INST_DATA;

pthread_key_t EAS_sigbuskey;

#ifdef MTK_DRM_APP
ssize_t readAtDRM(EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, off_t offset, void *data, size_t size) {
    size_t DRM_CACHE_SIZE = 1024*4;
    if (file->mDrmBuf == NULL) {
        file->mDrmBuf = malloc(DRM_CACHE_SIZE);// must free file->mDrmBuf
    }

    if (file->mDrmBuf != NULL && file->mDrmBufSize > 0 && offset >= file->mDrmBufOffset && (offset + size) <= (file->mDrmBufOffset + file->mDrmBufSize)) {
        // Use buffered data
        memcpy(data, (void*)(file->mDrmBuf + (offset - file->mDrmBufOffset)), size);
        return size;
    } else if (size <= DRM_CACHE_SIZE) { // not in cache & size is ok for new cache
        // Buffer new data
        file->mDrmBufOffset = offset;
        file->mDrmBufSize = pread_drm(file->mDrmManagerClient, file->mDecryptHandle, file->mDrmBuf, DRM_CACHE_SIZE, offset);

        if (file->mDrmBufSize > 0) {
            int64_t dataRead = 0;
            dataRead = size > file->mDrmBufSize ? file->mDrmBufSize : size;
            memcpy(data, (void*)file->mDrmBuf, dataRead);
            return dataRead;
        } else { // error
            LOGE("readAtDRM: file [%d], failed to read any valid data from DRM file. mDrmBufSize [%lld]",
                    (int)file, file->mDrmBufSize);
            return file->mDrmBufSize;
        }
    } else {
        // Too big chunk to cache. Call DRM directly
        ssize_t result = pread_drm(file->mDrmManagerClient, file->mDecryptHandle, data, size, offset);
        if (result <= 0) {
            LOGE("readAtDRM: file [%d], failed to read any valid data from DRM file. result [%ld]",
                    (int)file, result);
        }
        return result;
    }
}

EAS_RESULT EAS_HWConsumeRights(EAS_HW_DATA_HANDLE hwInstData)
{
    EAS_FILE_HANDLE fileHandle = hwInstData->files;
    int i = 0;
    for (i = 0; i < EAS_MAX_FILE_HANDLES; i++)
    {
        if (fileHandle->fd != -1 && fileHandle->mDrmManagerClient != NULL && fileHandle->mDecryptHandle != NULL)
        {
            LOGD("EAS_HWConsumeRights ---->.");
            consumeRights(fileHandle->mDrmManagerClient, fileHandle->mDecryptHandle, 0x01);// action must be play
            break;
        }
        fileHandle++;
    }
    return EAS_SUCCESS;
}
#endif


/*----------------------------------------------------------------------------
 * EAS_HWInit
 *
 * Initialize host wrapper interface
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWInit (EAS_HW_DATA_HANDLE *pHWInstData)
{
    EAS_HW_FILE *file;
    int i;

    /* need to track file opens for duplicate handles */
    *pHWInstData = malloc(sizeof(EAS_HW_INST_DATA));
    if (!(*pHWInstData))
        return EAS_ERROR_MALLOC_FAILED;

    EAS_HWMemSet(*pHWInstData, 0, sizeof(EAS_HW_INST_DATA));

    file = (*pHWInstData)->files;
    for (i = 0; i < EAS_MAX_FILE_HANDLES; i++)
    {
        file->fd = -1;
        file++;
    }


    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 * EAS_HWShutdown
 *
 * Shut down host wrapper interface
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWShutdown (EAS_HW_DATA_HANDLE hwInstData)
{

    free(hwInstData);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWMalloc
 *
 * Allocates dynamic memory
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
void *EAS_HWMalloc (EAS_HW_DATA_HANDLE hwInstData, EAS_I32 size)
{
    /* Since this whole library loves signed sizes, let's not let
     * negative or 0 values through */
    if (size <= 0)
      return NULL;
    return malloc((size_t) size);
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFree
 *
 * Frees dynamic memory
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
void EAS_HWFree (EAS_HW_DATA_HANDLE hwInstData, void *p)
{
    free(p);
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWMemCpy
 *
 * Copy memory wrapper
 *
 *----------------------------------------------------------------------------
*/
void *EAS_HWMemCpy (void *dest, const void *src, EAS_I32 amount)
{
    if (amount < 0)  {
      EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x1a54b6e8, 0x00000004 , amount);
      exit(255);
    }
    return memcpy(dest, src, (size_t) amount);
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWMemSet
 *
 * Set memory wrapper
 *
 *----------------------------------------------------------------------------
*/
void *EAS_HWMemSet (void *dest, int val, EAS_I32 amount)
{
    if (amount < 0)  {
      EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x1a54b6e8, 0x00000005 , amount);
      exit(255);
    }
    return memset(dest, val, (size_t) amount);
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWMemCmp
 *
 * Compare memory wrapper
 *
 *----------------------------------------------------------------------------
*/
EAS_I32 EAS_HWMemCmp (const void *s1, const void *s2, EAS_I32 amount)
{
    if (amount < 0) {
      EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x1a54b6e8, 0x00000006 , amount);
      exit(255);
    }
    return (EAS_I32) memcmp(s1, s2, (size_t) amount);
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWOpenFile
 *
 * Open a file for read or write
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWOpenFile (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_LOCATOR locator, EAS_FILE_HANDLE *pFile, EAS_FILE_MODE mode)
{
    EAS_HW_FILE *file;
    int fd;
    int i, temp;

    /* set return value to NULL */
    *pFile = NULL;

    /* only support read mode at this time */
    if (mode != EAS_FILE_READ)
        return EAS_ERROR_INVALID_FILE_MODE;

    /* find an empty entry in the file table */
    file = hwInstData->files;
    for (i = 0; i < EAS_MAX_FILE_HANDLES; i++)
    {
        /* is this slot being used? */
        if (file->fd < 0)
        {
            if (locator->path) {
                /* open the file */
                if ((fd = open(locator->path, O_RDONLY)) < 0) {
                    return EAS_ERROR_FILE_OPEN_FAILED;
                }
            } else {
                /* else file is already open */
                fd = dup(locator->fd);
            }

            /* determine the file size */
            if (locator->length == 0) {
                if (lseek(fd, 0, SEEK_END) < 0) {
                    close(fd);
                    return EAS_ERROR_FILE_LENGTH;
                }
                if ((file->fileSize = lseek(fd, 0, SEEK_CUR)) == -1L) {
                    close(fd);
                    return EAS_ERROR_FILE_LENGTH;
                }
            }

            // file size was passed in
            else {
                file->fileSize = (EAS_I32) locator->length;
            }

            file->fd = fd;
            file->offset = locator->offset;

            /* initialize some values */
            file->filePos = 0;
            file->dup = EAS_FALSE;

#ifdef MTK_DRM_APP
            LOGV("EAS_HWOpenFile ----> file [%d] mDrmManagerClient [%d] mDecryptHandle [%d] mDrmBufOffset [%lld] mDrmBufSize [%lld] mDrmBuf [%d]",
                    (int)file, file->mDrmManagerClient, file->mDecryptHandle, file->mDrmBufOffset, file->mDrmBufSize, file->mDrmBuf);

            int isDcf = 0;
            if (locator->path) {
                LOGD("EAS_HWOpenFile ----> use path [%s]", locator->path);
                isDcf = isDcf_path(locator->path);
            } else {
                LOGD("EAS_HWOpenFile ----> use file descriptor [%d]", file->fd);
                isDcf = isDcf_fd(file->fd);
            }

            if (1 == isDcf) { // check dcf first so we can avoid create DrmManagerClient unnecessarily.
                LOGD("EAS_HWOpenFile ----> identified a DRM file.");
                DrmManagerClient* client = file->mDrmManagerClient;
                if (client == NULL) {
                    client = create_DrmManagerClient();
                    file->mDrmManagerClient = client; // update
                }

                DecryptHandle* handle = file->mDecryptHandle;
                if (handle == NULL) {
                    handle = openDecryptSession_fd(client, file->fd, 0, 0);
                    file->mDecryptHandle = handle; // update
                }

                if (file->mDecryptHandle != NULL) {
                    long realFileSize = getContentSize(client, handle);
                    LOGD("EAS_HWOpenFile ----> file [%d], valid key, realFileSize [%ld]", (int)file, realFileSize);
                    if (realFileSize >= 0) {
                        file->fileSize = realFileSize;
                    }
                } else {
                    LOGD("EAS_HWOpenFile ----> file [%d], invalid key.", (int)file);
                    destroy_DrmManagerClient(client);
                    file->mDrmManagerClient = NULL;
                }
            } else {
                LOGD("EAS_HWOpenFile ----> checked, not a DRM file.");
                file->mDrmManagerClient = NULL;
                file->mDecryptHandle = NULL;
            }

            file->mDrmBufOffset = 0;
            file->mDrmBufSize = 0;
            file->mDrmBuf = NULL;
            LOGV("EAS_HWOpenFile [result] ----> file [%d] mDrmManagerClient [%d] mDecryptHandle [%d] mDrmBufOffset [%lld] mDrmBufSize [%lld] mDrmBuf [%d]",
                    (int)file, file->mDrmManagerClient, file->mDecryptHandle, file->mDrmBufOffset, file->mDrmBufSize, file->mDrmBuf);
#endif

            *pFile = file;
            return EAS_SUCCESS;
        }
        file++;
    }

    /* too many open files */
    return EAS_ERROR_MAX_FILES_OPEN;
}


/*----------------------------------------------------------------------------
 *
 * EAS_HWReadFile
 *
 * Read data from a file
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWReadFile (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *pBuffer, EAS_I32 n, EAS_I32 *pBytesRead)
{
    EAS_I32 count;

    /* make sure we have a valid handle */
    if (file->fd < 0)
        return EAS_ERROR_INVALID_HANDLE;

    if (n < 0)
      return EAS_EOF;

    /* calculate the bytes to read */
    count = file->fileSize - file->filePos;
    if (n < count)
        count = n;
    if (count < 0)
      return EAS_EOF;

    /* copy the data to the requested location, and advance the pointer */
    if (count) {
#ifdef MTK_DRM_APP
        if (file->mDrmManagerClient != NULL && file->mDecryptHandle != NULL) {
            count = readAtDRM(hwInstData, file, file->filePos + file->offset, pBuffer, count);
        } else { // normal file
#endif
        lseek(file->fd, file->filePos + file->offset, SEEK_SET);
        count = read(file->fd, pBuffer, count);
#ifdef MTK_DRM_APP
    }
#endif
    }
    file->filePos += count;
    *pBytesRead = count;

    /* were n bytes read? */
    if (count!= n)
        return EAS_EOF;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWGetByte
 *
 * Read a byte from a file
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWGetByte (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *p)
{
    EAS_I32 numread;
    return EAS_HWReadFile(hwInstData, file, p, 1, &numread);
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWGetWord
 *
 * Read a 16 bit word from a file
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWGetWord (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *p, EAS_BOOL msbFirst)
{
    EAS_RESULT result;
    EAS_U8 c1, c2;

    /* read 2 bytes from the file */
    if ((result = EAS_HWGetByte(hwInstData, file, &c1)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWGetByte(hwInstData, file, &c2)) != EAS_SUCCESS)
        return result;

    /* order them as requested */
    if (msbFirst)
        *((EAS_U16*) p) = ((EAS_U16) c1 << 8) | c2;
    else
        *((EAS_U16*) p) = ((EAS_U16) c2 << 8) | c1;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWGetDWord
 *
 * Returns the current location in the file
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWGetDWord (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, void *p, EAS_BOOL msbFirst)
{
    EAS_RESULT result;
    EAS_U8 c1, c2,c3,c4;

    /* read 4 bytes from the file */
    if ((result = EAS_HWGetByte(hwInstData, file, &c1)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWGetByte(hwInstData, file, &c2)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWGetByte(hwInstData, file, &c3)) != EAS_SUCCESS)
        return result;
    if ((result = EAS_HWGetByte(hwInstData, file, &c4)) != EAS_SUCCESS)
        return result;

    /* order them as requested */
    if (msbFirst)
        *((EAS_U32*) p) = ((EAS_U32) c1 << 24) | ((EAS_U32) c2 << 16) | ((EAS_U32) c3 << 8) | c4;
    else
        *((EAS_U32*) p)= ((EAS_U32) c4 << 24) | ((EAS_U32) c3 << 16) | ((EAS_U32) c2 << 8) | c1;

    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFilePos
 *
 * Returns the current location in the file
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWFilePos (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_I32 *pPosition)
{

    /* make sure we have a valid handle */
    if (file->fd < 0)
        return EAS_ERROR_INVALID_HANDLE;

    *pPosition = file->filePos;
    return EAS_SUCCESS;
} /* end EAS_HWFilePos */

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileSeek
 *
 * Seek to a specific location in the file
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWFileSeek (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_I32 position)
{

    /* make sure we have a valid handle */
    if (file->fd < 0)
        return EAS_ERROR_INVALID_HANDLE;

    /* validate new position */
    if ((position < 0) || (position > file->fileSize))
        return EAS_ERROR_FILE_SEEK;

    /* save new position */
    file->filePos = position;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileSeekOfs
 *
 * Seek forward or back relative to the current position
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWFileSeekOfs (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_I32 position)
{

    /* make sure we have a valid handle */
    if (file->fd < 0)
        return EAS_ERROR_INVALID_HANDLE;

    /* determine the file position */
    position += file->filePos;
    if ((position < 0) || (position > file->fileSize))
        return EAS_ERROR_FILE_SEEK;

    /* save new position */
    file->filePos = position;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWFileLength
 *
 * Return the file length
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWFileLength (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_I32 *pLength)
{

    /* make sure we have a valid handle */
    if (file->fd < 0)
        return EAS_ERROR_INVALID_HANDLE;

    *pLength = file->fileSize;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWDupHandle
 *
 * Duplicate a file handle
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWDupHandle (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file, EAS_FILE_HANDLE *pDupFile)
{
    EAS_HW_FILE *dupFile;
    int i;

    /* make sure we have a valid handle */
    if (file->fd < 0)
        return EAS_ERROR_INVALID_HANDLE;

    /* find an empty entry in the file table */
    dupFile = hwInstData->files;
    for (i = 0; i < EAS_MAX_FILE_HANDLES; i++)
    {
        /* is this slot being used? */
        if (dupFile->fd < 0)
        {
            /* copy info from the handle to be duplicated */
            dupFile->filePos = file->filePos;
            dupFile->fileSize = file->fileSize;
            dupFile->fd = file->fd;
            dupFile->offset = file->offset;

            /* set the duplicate handle flag */
            dupFile->dup = file->dup = EAS_TRUE;

#ifdef MTK_DRM_APP
            dupFile->mDrmManagerClient = file->mDrmManagerClient;
            dupFile->mDecryptHandle = file->mDecryptHandle;
            dupFile->mDrmBufOffset = file->mDrmBufOffset;
            dupFile->mDrmBufSize = file->mDrmBufSize;
            size_t DRM_CACHE_SIZE = 1024*4;
            if (file->mDrmBuf != NULL) {
                dupFile->mDrmBuf = malloc(DRM_CACHE_SIZE);
                memcpy(dupFile->mDrmBuf, (void*)file->mDrmBuf, file->mDrmBufSize);
            }
            LOGV("EAS_HWDupHandle: dupFile->mDrmBuf [%d] , file->mDrmBuf [%d]",
                    dupFile->mDrmBuf, file->mDrmBuf);
            if (dupFile->mDrmManagerClient != NULL && dupFile->mDecryptHandle != NULL && dupFile->mDrmBuf == NULL) {
                LOGW("EAS_HWDupHandle: ***** Potential memory leak *****");
                LOGW("Duplicate a DRM file with NULL drm buf, so 2 duplicated DRM file will not share drm buf.");
                LOGW("And we only free one drm buf.");
            }
            LOGV("EAS_HWDupHandle: file [%d] dupFile [%d] mDrmManagerClient [%d] mDecryptHandle [%d] mDrmBufOffset [%lld] mDrmBufSize [%lld] mDrmBuf [%d]",
                    (int)file, (int)dupFile, dupFile->mDrmManagerClient, dupFile->mDecryptHandle, dupFile->mDrmBufOffset, dupFile->mDrmBufSize, dupFile->mDrmBuf);
#endif

            *pDupFile = dupFile;
            return EAS_SUCCESS;
        }
        dupFile++;
    }

    /* too many open files */
    return EAS_ERROR_MAX_FILES_OPEN;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWClose
 *
 * Wrapper for fclose function
 *
 *----------------------------------------------------------------------------
*/
EAS_RESULT EAS_HWCloseFile (EAS_HW_DATA_HANDLE hwInstData, EAS_FILE_HANDLE file1)
{
    EAS_HW_FILE *file2,*dupFile;
    int i;


    /* make sure we have a valid handle */
    if (file1->fd < 0)
        return EAS_ERROR_INVALID_HANDLE;

    /* check for duplicate handle */
    if (file1->dup)
    {
#ifdef MTK_DRM_APP
        file1->mDrmManagerClient = NULL;
        file1->mDecryptHandle = NULL;
        if (file1->mDrmBuf != NULL) {
            free(file1->mDrmBuf);
        }
        file1->mDrmBufOffset = 0;
        file1->mDrmBufSize = 0;
#endif
        dupFile = NULL;
        file2 = hwInstData->files;
        for (i = 0; i < EAS_MAX_FILE_HANDLES; i++)
        {
            /* check for duplicate */
            if ((file1 != file2) && (file2->fd == file1->fd))
            {
                /* is there more than one duplicate? */
                if (dupFile != NULL)
                {
                    /* clear this entry and return */
                    file1->fd = -1;
                    return EAS_SUCCESS;
                }

                /* this is the first duplicate found */
                else
                    dupFile = file2;
            }
            file2++;
        }

        /* there is only one duplicate, clear the dup flag */
        if (dupFile)
            dupFile->dup = EAS_FALSE;
        else
            /* if we get here, there's a serious problem */
            return EAS_ERROR_HANDLE_INTEGRITY;

        /* clear this entry and return */
        file1->fd = -1;
        return EAS_SUCCESS;
    }

    /* no duplicates - close the file */
    close(file1->fd);

#ifdef MTK_DRM_APP
    LOGV("EAS_HWCloseFile ----> file [%d] mDrmManagerClient [%d] mDecryptHandle [%d] mDrmBufOffset [%lld] mDrmBufSize [%lld] mDrmBuf [%d]",
            (int)file1, file1->mDrmManagerClient, file1->mDecryptHandle, file1->mDrmBufOffset, file1->mDrmBufSize, file1->mDrmBuf);

    if (file1->mDrmManagerClient != NULL && file1->mDecryptHandle != NULL) {
        closeDecryptSession(file1->mDrmManagerClient, file1->mDecryptHandle);
        destroy_DrmManagerClient(file1->mDrmManagerClient);
        if (file1->mDrmBuf != NULL) {
            free(file1->mDrmBuf);
        }
    }

    file1->mDrmManagerClient = NULL;
    file1->mDecryptHandle = NULL;
    file1->mDrmBuf = NULL;
    file1->mDrmBufOffset = 0;
    file1->mDrmBufSize = 0;
    LOGV("EAS_HWCloseFile [result] ----> file [%d] mDrmManagerClient [%d] mDecryptHandle [%d] mDrmBufOffset [%lld] mDrmBufSize [%lld] mDrmBuf [%d]",
            (int)file1, file1->mDrmManagerClient, file1->mDecryptHandle, file1->mDrmBufOffset, file1->mDrmBufSize, file1->mDrmBuf);
#endif

    /* clear this entry and return */
    file1->fd = -1;
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWVibrate
 *
 * Turn on/off vibrate function
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWVibrate (EAS_HW_DATA_HANDLE hwInstData, EAS_BOOL state)
{
    EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x1a54b6e8, 0x00000001 , state);
    return EAS_SUCCESS;
} /* end EAS_HWVibrate */

/*----------------------------------------------------------------------------
 *
 * EAS_HWLED
 *
 * Turn on/off LED
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWLED (EAS_HW_DATA_HANDLE hwInstData, EAS_BOOL state)
{
    EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x1a54b6e8, 0x00000002 , state);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWBackLight
 *
 * Turn on/off backlight
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_RESULT EAS_HWBackLight (EAS_HW_DATA_HANDLE hwInstData, EAS_BOOL state)
{
    EAS_ReportEx(_EAS_SEVERITY_NOFILTER, 0x1a54b6e8, 0x00000003 , state);
    return EAS_SUCCESS;
}

/*----------------------------------------------------------------------------
 *
 * EAS_HWYield
 *
 * This function is called periodically by the EAS library to give the
 * host an opportunity to allow other tasks to run. There are two ways to
 * use this call:
 *
 * If you have a multi-tasking OS, you can call the yield function in the
 * OS to allow other tasks to run. In this case, return EAS_FALSE to tell
 * the EAS library to continue processing when control returns from this
 * function.
 *
 * If tasks run in a single thread by sequential function calls (sometimes
 * call a "commutator loop"), return EAS_TRUE to cause the EAS Library to
 * return to the caller. Be sure to check the number of bytes rendered
 * before passing the audio buffer to the codec - it may not be filled.
 * The next call to EAS_Render will continue processing until the buffer
 * has been filled.
 *
 *----------------------------------------------------------------------------
*/
/*lint -esym(715, hwInstData) hwInstData available for customer use */
EAS_BOOL EAS_HWYield (EAS_HW_DATA_HANDLE hwInstData)
{
    /* put your code here */
    return EAS_FALSE;
}

