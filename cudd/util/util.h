/**
  @file

  @ingroup util

  @brief Low-level utilities.

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
#ifndef UTIL_H_
#define UTIL_H_

#include "config.h"

#if HAVE_ASSERT_H == 1
#include <assert.h>
#else
#error assert.h is needed to build this package
#endif

#if HAVE_UNISTD_H == 1
#include <unistd.h>
#endif

#include <stdio.h>
#include <ctype.h>

#if HAVE_STDLIB_H
#include <stdlib.h>
#else
#error stdlib.h is needed to build this package
#endif

#if HAVE_STRING_H == 1
#include <string.h>
#else
#error string.h is needed to build this package
#endif

#if HAVE_INTTYPES_H == 1
#include <inttypes.h>
#else
#error inttypes.h is needed to build this package
#endif

/**
 * @def PRIszt
 * @brief Format string for a size_t value.
 */
#if defined(_WIN32) && !defined(__USE_MINGW_ANSI_STDIO)
#ifndef PRIuPTR
#define PRIuPTR "Iu"
#endif
#ifndef PRIxPTR
#define PRIxPTR "Ix"
#endif
#ifndef PRIiPTR
#define PRIiPTR "Id"
#endif
#define PRIszt "Iu"
#else
#define PRIszt "zu"
#endif

/**
 * @def UTIL_UNUSED
 * @brief Macro to tell gcc that a variable is intentionally unused.
 */
#if defined(__GNUC__)
#if __GNUC__ > 2 || __GNUC_MINOR__ >= 7
#define UTIL_UNUSED __attribute__ ((unused))
#else
#define UTIL_UNUSED
#endif
#else
#define UTIL_UNUSED
#endif

/**
 * @brief Type-decorated NULL (for documentation).
 */
#define NIL(type)		((type *) 0)

/* #define USE_MM */		/* choose default memory allocator */

/**
 * @def ALLOC
 * @brief Wrapper for either malloc or MMalloc.
 * @details Which function is wrapped depends on whether USE_MM is defined.
 */

/**
 * @def REALLOC
 * @brief Wrapper for either realloc or MMrealloc.
 * @details Which function is wrapped depends on whether USE_MM is defined.
 */

/**
 * @def FREE
 * @brief Wrapper for free.
 * @details Sets its argument to 0 after freeing.
 */

#if defined(USE_MM)
/* Assumes the memory manager is default one. */
#define ALLOC(type, num)	\
    ((type *) malloc(sizeof(type) * (num)))
#define REALLOC(type, obj, num)	\
    ((type *) realloc(obj, sizeof(type) * (num)))
#else
/* Use replacements that call MMoutOfMemory if allocation fails. */
#define ALLOC(type, num)	\
    ((type *) MMalloc(sizeof(type) * (size_t) (num)))
#define REALLOC(type, obj, num)	\
    ((type *) MMrealloc((obj), sizeof(type) * (size_t) (num)))
#endif
/* In any case, set to zero the pointer to freed memory. */
#define FREE(obj) (free(obj), (obj) = 0)

/**
 * @brief Prints message and terminates execution.
 */
/*#define fail(why) {\
    (void) fprintf(stderr, "Fatal error: file %s, line %d\n%s\n",\
	__FILE__, __LINE__, why);\
    (void) fflush(stdout);\
    abort();\
}*/

/* These arguably do NOT belong in util.h */
/**
 * @brief Computes the absolute value of its argument.
 */
#define ABS(a)			((a) < 0 ? -(a) : (a))
/**
 * @brief Computes the maximum of its two arguments.
 */
#define MAX(a,b)		((a) > (b) ? (a) : (b))
/**
 * @brief Computes the minimum of its two arguments.
 */
#define MIN(a,b)		((a) < (b) ? (a) : (b))

/**
 * @brief Type of comparison functions for util_qsort.
 */
typedef int (*QSFP)(void const *, void const *);

#ifdef __cplusplus
extern "C" {
#endif

#ifndef USE_MM
extern void *MMalloc(size_t);
extern void *MMrealloc(void *, size_t);
#endif
extern void MMout_of_memory(size_t);
extern void (*MMoutOfMemory) (size_t);

extern long util_cpu_time(void);
extern long util_cpu_ctime(void);
extern char *util_path_search(char const *);
extern char *util_file_search(char const *, char *, char const *);
extern void util_print_cpu_stats(FILE *);
extern char *util_print_time(unsigned long);
extern char *util_strsav(char const *);
extern char *util_tilde_expand(char const *);
extern size_t getSoftDataLimit(void);
extern void util_qsort (void *vbase, int n, int size, QSFP compar);
extern int util_pipefork(char * const * argv, FILE ** toCommand,
                         FILE ** fromCommand, int * pid);
#ifdef __cplusplus
}
#endif

#endif /* UTIL_H_ */
