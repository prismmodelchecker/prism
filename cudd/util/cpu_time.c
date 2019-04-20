/**
  @file

  @ingroup util

  @brief System time calls

  @details Provide a uniform interface across different operating systems.

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

#include "util.h"

#if HAVE_SYS_TIMES_H == 1
#include <sys/times.h>
#endif

#ifdef _WIN32
#include <windows.h>
#endif

/**
 * @brief returns a long which represents the elapsed processor
 * time in milliseconds since some constant reference.
 */
long
util_cpu_time(void)
{
#if HAVE_SYSCONF == 1

    /* Code for POSIX systems */

    struct tms buffer;
    long nticks;                /* number of clock ticks per second */

    nticks = sysconf(_SC_CLK_TCK);
    times(&buffer);
    return (long) ((buffer.tms_utime + buffer.tms_stime) * (1000.0/nticks));

#elif defined(_WIN32)
    FILETIME creationTime, exitTime, kernelTime, userTime;
    if (GetProcessTimes(GetCurrentProcess(), &creationTime, &exitTime,
			&kernelTime, &userTime)) {
	ULARGE_INTEGER integerTime;
	integerTime.u.LowPart = userTime.dwLowDateTime;
	integerTime.u.HighPart = userTime.dwHighDateTime;
	return (long) (integerTime.QuadPart / 10000);
    } else {
	return 0;
    }
#else
    return 0L;
#endif

}

/**
 * @brief returns a long which represents the elapsed processor
 * time in milliseconds since some constant reference.  It includes
 * waited-for terminated children.
 */
long
util_cpu_ctime(void)
{
#if HAVE_SYSCONF == 1

    /* Code for POSIX systems */

    struct tms buffer;
    long nticks;                /* number of clock ticks per second */

    nticks = sysconf(_SC_CLK_TCK);
    times(&buffer);
    return (long) ((buffer.tms_utime + buffer.tms_cutime) * (1000.0/nticks));

#else
    return 0L;
#endif

}

