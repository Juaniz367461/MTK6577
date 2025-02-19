/*
 * Copyright (C) 2005 The Android Open Source Project
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

/*
 * Android config -- "CYGWIN_NT-5.1".  
 *
 * Cygwin has pthreads, but GDB seems to get confused if you use it to
 * create threads.  By "confused", I mean it freezes up the first time the
 * debugged process creates a thread, even if you use CreateThread.  The
 * mere presence of pthreads linkage seems to cause problems.
 */
#ifndef _ANDROID_CONFIG_H
#define _ANDROID_CONFIG_H

#include "../mtkNonNDKConfig.h"
/*
 * ===========================================================================
 *                              !!! IMPORTANT !!!
 * ===========================================================================
 *
 * This file is included by ALL C/C++ source files.  Don't put anything in
 * here unless you are absolutely certain it can't go anywhere else.
 *
 * Any C++ stuff must be wrapped with "#ifdef __cplusplus".  Do not use "//"
 * comments.
 */

/*
 * Threading model.  Choose one:
 *
 * HAVE_PTHREADS - use the pthreads library.
 * HAVE_WIN32_THREADS - use Win32 thread primitives.
 */
#define HAVE_WIN32_THREADS

/*
 * Do we have the futex syscall?
 */

/* #define HAVE_FUTEX */


/*
 * Process creation model.  Choose one:
 *
 * HAVE_FORKEXEC - use fork() and exec()
 * HAVE_WIN32_PROC - use CreateProcess()
 */
#ifdef __CYGWIN__
#  define HAVE_FORKEXEC
#else
#  define HAVE_WIN32_PROC
#endif

/*
 * Process out-of-memory adjustment.  Set if running on Linux,
 * where we can write to /proc/<pid>/oom_adj to modify the out-of-memory
 * badness adjustment.
 */
/* #define HAVE_OOM_ADJ */

/*
 * IPC model.  Choose one:
 *
 * HAVE_SYSV_IPC - use the classic SysV IPC mechanisms (semget, shmget).
 * HAVE_MACOSX_IPC - use Macintosh IPC mechanisms (sem_open, mmap).
 * HAVE_WIN32_IPC - use Win32 IPC (CreateSemaphore, CreateFileMapping).
 * HAVE_ANDROID_IPC - use Android versions (?, mmap).
 */
#define HAVE_WIN32_IPC

/*
 * Memory-mapping model. Choose one:
 *
 * HAVE_POSIX_FILEMAP - use the Posix sys/mmap.h
 * HAVE_WIN32_FILEMAP - use Win32 filemaps
 */
#ifdef __CYGWIN__
#define  HAVE_POSIX_FILEMAP
#else
#define  HAVE_WIN32_FILEMAP
#endif

/*
 * Define this if you have <termio.h>
 */
#ifdef __CYGWIN__
#  define  HAVE_TERMIO_H
#endif

/*
 * Define this if you have <sys/sendfile.h>
 */
#ifdef __CYGWIN__
#  define  HAVE_SYS_SENDFILE_H 1
#endif

/*
 * Define this if you build against MSVCRT.DLL
 */
#ifndef __CYGWIN__
#  define HAVE_MS_C_RUNTIME
#endif

/*
 * Define this if you have sys/uio.h
 */
#ifdef __CYGWIN__
#define  HAVE_SYS_UIO_H
#endif


/*
 * Define this if we have localtime_r().
 */
/* #define HAVE_LOCALTIME_R */

/*
 * Define this if we have gethostbyname_r().
 */
/* #define HAVE_GETHOSTBYNAME_R */

/*
 * Define this if we have ioctl().
 */
/* #define HAVE_IOCTL */

/*
 * Define this if we want to use WinSock.
 */
#ifndef __CYGWIN__
#define HAVE_WINSOCK
#endif

/*
 * Define this if your platforms implements symbolic links
 * in its filesystems
 */
/* #define HAVE_SYMLINKS */

/*
 * Define this if have clock_gettime() and friends
 */
/* #define HAVE_POSIX_CLOCKS */

/*
 * Endianness of the target machine.  Choose one:
 *
 * HAVE_ENDIAN_H -- have endian.h header we can include.
 * HAVE_LITTLE_ENDIAN -- we are little endian.
 * HAVE_BIG_ENDIAN -- we are big endian.
 */
#ifdef __CYGWIN__
#define HAVE_ENDIAN_H
#endif

#define HAVE_LITTLE_ENDIAN

/*
 * We need to choose between 32-bit and 64-bit off_t.  All of our code should
 * agree on the same size.  For desktop systems, use 64-bit values,
 * because some of our libraries (e.g. wxWidgets) expect to be built that way.
 */
#define _FILE_OFFSET_BITS 64
#define _LARGEFILE_SOURCE 1

/*
 * Define if platform has off64_t (and lseek64 and other xxx64 functions)
 */
#define HAVE_OFF64_T

/*
 * Defined if we have the backtrace() call for retrieving a stack trace.
 * Needed for CallStack to operate; if not defined, CallStack is
 * non-functional.
 */
#define HAVE_BACKTRACE 0

/*
 * Defined if we have the dladdr() call for retrieving the symbol associated
 * with a memory address.  If not defined, stack crawls will not have symbolic
 * information.
 */
#define HAVE_DLADDR 0

/*
 * Defined if we have the cxxabi.h header for demangling C++ symbols.  If
 * not defined, stack crawls will be displayed with raw mangled symbols
 */
#define HAVE_CXXABI 0

/*
 * Define if tm struct has tm_gmtoff field
 */
/* #define HAVE_TM_GMTOFF 1 */

/*
 * Define if dirent struct has d_type field
 */
/* #define HAVE_DIRENT_D_TYPE 1 */

/*
 * Define if libc includes Android system properties implementation.
 */
/* #define HAVE_LIBC_SYSTEM_PROPERTIES */

/*
 * Define if system provides a system property server (should be
 * mutually exclusive with HAVE_LIBC_SYSTEM_PROPERTIES).
 */
/* #define HAVE_SYSTEM_PROPERTY_SERVER */

/*
 * Define if we have madvise() in <sys/mman.h>
 */
/*#define HAVE_MADVISE 1*/

/*
 * Add any extra platform-specific defines here.
 */
#define WIN32 1                 /* stock Cygwin doesn't define these */
#define _WIN32 1
#define _WIN32_WINNT 0x0500     /* admit to using >= Win2K */

#define HAVE_WINDOWS_PATHS      /* needed by simulator */

/*
 * What CPU architecture does this platform use?
 */
#define ARCH_X86

/*
 * sprintf() format string for shared library naming.
 */
#define OS_SHARED_LIB_FORMAT_STR    "lib%s.dll"

/*
 * type for the third argument to mincore().
 */
#define MINCORE_POINTER_TYPE unsigned char *

/*
 * The default path separator for the platform
 */
#define OS_PATH_SEPARATOR '\\'

/*
 * Is the filesystem case sensitive?
 */
/* #define OS_CASE_SENSITIVE */

/*
 * Define if <sys/socket.h> exists.
 * Cygwin has it, but not MinGW.
 */
#ifdef USE_MINGW
/* #define HAVE_SYS_SOCKET_H */
#else
#define HAVE_SYS_SOCKET_H 1
#endif

/*
 * Define if the strlcpy() function exists on the system.
 */
/* #define HAVE_STRLCPY 1 */

/*
 * Define if the open_memstream() function exists on the system.
 */
/* #define HAVE_OPEN_MEMSTREAM 1 */

/*
 * Define if the BSD funopen() function exists on the system.
 */
/* #define HAVE_FUNOPEN 1 */

/*
 * Define if <winsock2.h> exists.
 * Only MinGW has it.
 */
#ifdef USE_MINGW
#define HAVE_WINSOCK2_H 1
#else
/* #define HAVE_WINSOCK2_H */
#endif

/*
 * Various definitions missing in MinGW
 */
#ifdef USE_MINGW
#define S_IRGRP 0
#endif

/*
 * Define if writev() exists.
 */
/* #define HAVE_WRITEV */

/*
 * Define if <stdint.h> exists.
 */
/* #define HAVE_STDINT_H */

/*
 * Define if <stdbool.h> exists.
 */
#define HAVE_STDBOOL_H

/*
 * Define if <sched.h> exists.
 */
/* #define HAVE_SCHED_H */

/*
 * Define if pread() exists
 */
/* #define HAVE_PREAD 1 */

/*
 * Define if we have st_mtim in struct stat
 */
/* #define HAVE_STAT_ST_MTIM 1 */

/*
 * Define if printf() supports %zd for size_t arguments
 */
/* #define HAVE_PRINTF_ZD 1 */

#endif /*_ANDROID_CONFIG_H*/
