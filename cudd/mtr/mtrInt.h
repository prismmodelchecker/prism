/**
  @file 

  @ingroup mtr

  @brief Internal data structures of the mtr package

  @author Fabio Somenzi

  @copyright@parblock
  Copyright (c) 1995-2015, Regents of the University of Colorado

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

#ifndef MTRINT_H_
#define MTRINT_H_

/*---------------------------------------------------------------------------*/
/* Nested includes                                                           */
/*---------------------------------------------------------------------------*/

#include "config.h"
#include "mtr.h"

/*---------------------------------------------------------------------------*/
/* Constant declarations                                                     */
/*---------------------------------------------------------------------------*/

#ifndef SIZEOF_VOID_P
#define SIZEOF_VOID_P 4
#endif
#ifndef SIZEOF_INT
#define SIZEOF_INT 4
#endif

#if defined(__GNUC__)
#define MTR_INLINE __inline__
# if (__GNUC__ >2 || __GNUC_MINOR__ >=7)
#   define MTR_UNUSED __attribute__ ((unused))
# else
#   define MTR_UNUSED
# endif
#else
#define MTR_INLINE
#define MTR_UNUSED
#endif

/* MTR_MAXHIGH is defined in such a way that on 32-bit and 64-bit
** machines one can cast a value to (int) without generating a negative
** number.
*/
#if SIZEOF_VOID_P == 8
#define MTR_MAXHIGH	(((MtrHalfWord) ~0) >> 1)
#else
#define MTR_MAXHIGH	((MtrHalfWord) ~0)
#endif

/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/

/**
 * @brief unsigned integer half the size of a pointer.
 */
#if SIZEOF_VOID_P == 8
typedef uint32_t   MtrHalfWord;
#else
typedef uint16_t MtrHalfWord;
#endif

/*---------------------------------------------------------------------------*/
/* Stucture declarations                                                     */
/*---------------------------------------------------------------------------*/

/**
 * @brief multi-way tree node.
 */
struct MtrNode_ {
    MtrHalfWord flags;
    MtrHalfWord low;
    MtrHalfWord size;
    MtrHalfWord index;
    struct MtrNode_ *parent;
    struct MtrNode_ *child;
    struct MtrNode_ *elder;
    struct MtrNode_ *younger;
};

/*---------------------------------------------------------------------------*/
/* Variable declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Macro declarations                                                        */
/*---------------------------------------------------------------------------*/

/* Flag manipulation macros */
#define MTR_SET(node, flag)	(node->flags |= (flag))
#define MTR_RESET(node, flag)	(node->flags &= ~ (flag))
#define MTR_TEST(node, flag)	(node->flags & (flag))


/** \cond */

/*---------------------------------------------------------------------------*/
/* Function prototypes                                                       */
/*---------------------------------------------------------------------------*/

/** \endcond */


#endif /* MTRINT_H_ */
