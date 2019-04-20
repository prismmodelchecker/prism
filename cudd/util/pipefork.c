/**
  @file

  @ingroup util

  @brief Forks a command and sets up pipes to and from.

  @author Rick L Spickelmier
  @author Richard Rudell

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

#include "config.h"
#if HAVE_UNISTD_H == 1
#define _POSIX_SOURCE
#include <unistd.h>
#endif
#include <stdio.h>
#if HAVE_SYS_WAIT_H == 1
#include <sys/wait.h>
#endif

/**
 * @def EXTERN
 * @brief Allows C linkage when compiling as C++.
 */
#ifndef EXTERN
#   ifdef __cplusplus
#	define EXTERN extern "C"
#   else
#	define EXTERN extern
#   endif
#endif


/**
 * @brief Forks a command and sets up pipes to and from.
 *
 * @return 1 for success, with toCommand and fromCommand pointing to
 *   the streams; 0 for failure
 */
int
util_pipefork(
  char * const *argv,	/**< normal argv argument list */
  FILE **toCommand,	/**< pointer to the sending stream */
  FILE **fromCommand,	/**< pointer to the reading stream */
  int *pid /**< process ID */)
{
#if HAVE_SYS_WAIT_H == 1
    int forkpid, waitPid;
    int topipe[2], frompipe[2];
    char buffer[1024];
    int status;

    /* create the PIPES...
     * fildes[0] for reading from command
     * fildes[1] for writing to command
     */
    if (pipe(topipe)) return(0);
    if (pipe(frompipe)) return(0);

    if ((forkpid = fork()) == 0) {
	/* child here, connect the pipes */
	(void) dup2(topipe[0], fileno(stdin));
	(void) dup2(frompipe[1], fileno(stdout));

	(void) close(topipe[0]);
	(void) close(topipe[1]);
	(void) close(frompipe[0]);
	(void) close(frompipe[1]);

	(void) execvp(argv[0], argv);
	(void) sprintf(buffer, "util_pipefork: can not exec %s", argv[0]);
	perror(buffer);
	(void) _exit(1);
    }

    if (pid) {
        *pid = forkpid;
    }

    waitPid = waitpid(-1, &status, WNOHANG);

    /* parent here, use fork() semantics to get return status */
    if (waitPid == forkpid && WIFEXITED(status)) {
	return 0;
    }
    if ((*toCommand = fdopen(topipe[1], "w")) == NULL) {
	return 0;
    }
    if ((*fromCommand = fdopen(frompipe[0], "r")) == NULL) {
	return 0;
    }
    (void) close(topipe[0]);
    (void) close(frompipe[1]);
    return 1;
#else
    (void) argv;        /* to avoid warning */
    (void) toCommand;   /* to avoid warning */
    (void) fromCommand; /* to avoid warning */
    (void) pid;         /* to avoid warning */
    (void) fprintf(stderr, 
	"util_pipefork: not implemented on your operating system\n");
    return 0;
#endif
}
