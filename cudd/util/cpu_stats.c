/**
  @file

  @ingroup util

  @brief CPU statistics.

  @copyright@parblock
  Copyright (c) 1994-1998 The Regents of the Univ. of California.
  All rights reserved.

  Permission is hereby granted, without written agreement and without license
  or royalty fees, to use, copy, modify, and distribute this software and its
  documentation for any purpose, provided that the above copyright notice and
  the following two paragraphs appear in all copies of this software.

  IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
  DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
  OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
  CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

  THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
  FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS ON AN
  "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO PROVIDE
  MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
  @endparblock

  @copyright@parblock
  Copyright (c) 1999-2015, Regents of the University of Colorado

  All rights reserved.

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions
  are met:

  Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.

  Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.

  Neither the name of the University of Colorado nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
  COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
  CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
  LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
  ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.
  @endparblock

*/

#define _BSD_SOURCE
#include "util.h"

#if HAVE_SYS_TIME_H == 1
#include <sys/time.h>
#endif
#if HAVE_SYS_RESOURCE_H == 1
#include <sys/resource.h>
#endif

#ifdef BSD
#if defined(_IBMR2)
#define etext _etext
#define edata _edata
#define end _end
#endif

extern int end, etext, edata;

#endif

#ifdef _WIN32
#include <winsock2.h>
#include <psapi.h>
#endif

/**
   @brief Prints CPU statistics.

   The amount of detail printed depends on the host operating system.

*/
void
util_print_cpu_stats(FILE *fp)
{
#if HAVE_GETRUSAGE == 1 && HAVE_GETRLIMIT == 1
    struct rusage rusage;
    double user, system, scale;
    long text, data;
    struct rlimit rlp;
    long vm_limit, vm_soft_limit;
    char hostname[257];
#ifdef BSD
    long vm_text, vm_init_data, vm_uninit_data, vm_sbrk_data;
#endif

    /* Get the hostname */
    (void) gethostname(hostname, sizeof(hostname));
    hostname[sizeof(hostname)-1] = '\0';	/* just in case */

#ifdef BSD
    /* Get the virtual memory sizes */
    vm_text = (long) (((long) (&etext)) / 1024.0 + 0.5);
    vm_init_data = (long) (((long) (&edata) - (long) (&etext)) / 1024.0 + 0.5);
    vm_uninit_data = (long) (((long) (&end) - (long) (&edata)) / 1024.0 + 0.5);
    vm_sbrk_data = (long) (((long) sbrk(0) - (long) (&end)) / 1024.0 + 0.5);
#endif

    /* Get virtual memory limits */
    (void) getrlimit(RLIMIT_DATA, &rlp);
    vm_limit = (long) (rlp.rlim_max / 1024.0 + 0.5);
    vm_soft_limit = (long) (rlp.rlim_cur / 1024.0 + 0.5);

    /* Get usage stats */
    (void) getrusage(RUSAGE_SELF, &rusage);
    user = rusage.ru_utime.tv_sec + rusage.ru_utime.tv_usec * 1e-6;
    system = rusage.ru_stime.tv_sec + rusage.ru_stime.tv_usec * 1e-6;
    scale = (user + system)*100.0;
    if (scale == 0.0) scale = 0.001;
    text = (int) (rusage.ru_ixrss / scale + 0.5);
    data = (int) ((rusage.ru_idrss + rusage.ru_isrss) / scale + 0.5);

#elif defined(_WIN32)
    char hostname[257];
    WSADATA wsaData;
    FILETIME creationTime, exitTime, kernelTime, userTime;
    double user, system;
    MEMORYSTATUSEX statex;
    size_t vm_limit;
    PROCESS_MEMORY_COUNTERS pmc;
    size_t peak_working_set;
    long page_faults;

    /* Get the hostname */
    WSAStartup(MAKEWORD(2, 2), &wsaData);
    (void) gethostname(hostname, sizeof(hostname));
    hostname[sizeof(hostname)-1] = '\0';	/* just in case */
    WSACleanup();

    /* Get usage stats */
    if (GetProcessTimes(GetCurrentProcess(), &creationTime, &exitTime,
			&kernelTime, &userTime)) {
	ULARGE_INTEGER integerSystemTime, integerUserTime;
	integerUserTime.u.LowPart = userTime.dwLowDateTime;
	integerUserTime.u.HighPart = userTime.dwHighDateTime;
	user = (double) integerUserTime.QuadPart * 1e-7;
	integerSystemTime.u.LowPart = kernelTime.dwLowDateTime;
	integerSystemTime.u.HighPart = kernelTime.dwHighDateTime;
	system = (double) integerSystemTime.QuadPart * 1e-7;
    } else {
	user = system = 0.0;
    }
    statex.dwLength = sizeof(statex);
    if (GlobalMemoryStatusEx(&statex)) {
	vm_limit = (size_t) (statex.ullTotalVirtual / 1024.0 + 0.5);
    } else {
	vm_limit = 0;
    }
    if (GetProcessMemoryInfo(GetCurrentProcess(), &pmc, sizeof(pmc))) {
	peak_working_set = (size_t) (pmc.PeakWorkingSetSize / 1024.0 + 0.5);
	page_faults = (long) pmc.PageFaultCount;
    } else {
	peak_working_set = 0;
	page_faults = 0;
    }
#endif

#if (HAVE_GETRUSAGE == 1 && HAVE_GETRLIMIT == 1) || defined(_WIN32)
    (void) fprintf(fp, "Runtime Statistics\n");
    (void) fprintf(fp, "------------------\n");
    (void) fprintf(fp, "Machine name: %s\n", hostname);
    (void) fprintf(fp, "User time   %6.1f seconds\n", user);
    (void) fprintf(fp, "System time %6.1f seconds\n\n", system);

#if HAVE_GETRUSAGE == 1 && HAVE_GETRLIMIT == 1
    (void) fprintf(fp, "Average resident text size       = %5ldK\n", text);
    (void) fprintf(fp, "Average resident data+stack size = %5ldK\n", data);
    (void) fprintf(fp, "Maximum resident size            = %5ldK\n\n",
	rusage.ru_maxrss);
#if defined(BSD)
    (void) fprintf(fp, "Virtual text size                = %5ldK\n",
	vm_text);
    (void) fprintf(fp, "Virtual data size                = %5ldK\n",
	vm_init_data + vm_uninit_data + vm_sbrk_data);
    (void) fprintf(fp, "    data size initialized        = %5ldK\n",
	vm_init_data);
    (void) fprintf(fp, "    data size uninitialized      = %5ldK\n",
	vm_uninit_data);
    (void) fprintf(fp, "    data size sbrk               = %5ldK\n",
	vm_sbrk_data);
#endif
    (void) fprintf(fp, "Virtual memory limit             = ");
    if (rlp.rlim_cur == RLIM_INFINITY)
        (void) fprintf(fp, "unlimited");
    else
        (void) fprintf(fp, "%5ldK", vm_soft_limit);
    if (rlp.rlim_max == RLIM_INFINITY)
        (void) fprintf(fp, " (unlimited)\n");
    else
        (void) fprintf(fp, " (%ldK)\n\n", vm_limit);

    (void) fprintf(fp, "Major page faults = %ld\n", rusage.ru_majflt);
    (void) fprintf(fp, "Minor page faults = %ld\n", rusage.ru_minflt);
    (void) fprintf(fp, "Swaps = %ld\n", rusage.ru_nswap);
    (void) fprintf(fp, "Input blocks = %ld\n", rusage.ru_inblock);
    (void) fprintf(fp, "Output blocks = %ld\n", rusage.ru_oublock);
    (void) fprintf(fp, "Context switch (voluntary) = %ld\n", rusage.ru_nvcsw);
    (void) fprintf(fp, "Context switch (involuntary) = %ld\n", rusage.ru_nivcsw);
#else
    (void) fprintf(fp, "Maximum resident size            = ");
    if (peak_working_set == 0)
	(void) fprintf(fp, "unavailable\n");
    else
	(void) fprintf(fp, "%" PRIszt "K\n", peak_working_set);
    (void) fprintf(fp, "Virtual memory limit             = ");
    if (vm_limit == 0)
	(void) fprintf(fp, "unavailable\n");
    else
	(void) fprintf(fp, "%5" PRIszt "K\n", vm_limit);
    (void) fprintf(fp, "Page faults       = %ld\n", page_faults);
#endif
#else
    (void) fprintf(fp, "Usage statistics not available\n");
#endif
}
