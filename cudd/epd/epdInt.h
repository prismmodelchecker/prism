/**
  @file 

  @ingroup epd

  @brief Internal header for the University of Colorado extended
  double precision package.

  @author In-Ho Moon

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

#ifndef EPD_INT_H_
#define EPD_INT_H_

#include "config.h"
#include "epd.h"

#if WORDS_BIGENDIAN == 1
#define EPD_BIG_ENDIAN
#endif

/*---------------------------------------------------------------------------*/
/* Constant declarations                                                     */
/*---------------------------------------------------------------------------*/

#define	EPD_MAX_BIN	1023
#define	EPD_MAX_DEC	308
#define	EPD_EXP_INF	0x7ff

/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/

typedef struct IeeeDoubleStruct IeeeDouble;
typedef struct IeeeNanStruct IeeeNan;
typedef union EpTypeUnion EpType;

/*---------------------------------------------------------------------------*/
/* Structure declarations                                                    */
/*---------------------------------------------------------------------------*/

/**
  @brief IEEE double struct.
*/
#ifdef	EPD_BIG_ENDIAN
struct IeeeDoubleStruct {	/* BIG_ENDIAN */
  unsigned int sign: 1;
  unsigned int exponent: 11;
  unsigned int mantissa0: 20;
  unsigned int mantissa1: 32;
};
#else
struct IeeeDoubleStruct {	/* LITTLE_ENDIAN */
  unsigned int mantissa1: 32;
  unsigned int mantissa0: 20;
  unsigned int exponent: 11;
  unsigned int sign: 1;
};
#endif

/**
  @brief IEEE double NaN struct.
*/
#ifdef	EPD_BIG_ENDIAN
struct IeeeNanStruct {	/* BIG_ENDIAN */
  unsigned int sign: 1;
  unsigned int exponent: 11;
  unsigned int quiet_bit: 1;
  unsigned int mantissa0: 19;
  unsigned int mantissa1: 32;
};
#else
struct IeeeNanStruct {	/* LITTLE_ENDIAN */
  unsigned int mantissa1: 32;
  unsigned int mantissa0: 19;
  unsigned int quiet_bit: 1;
  unsigned int exponent: 11;
  unsigned int sign: 1;
};
#endif

/**
  @brief Different views of a double.
*/
union EpTypeUnion {
  double			value;
  struct IeeeDoubleStruct	bits;
  struct IeeeNanStruct		nan;
};

/**
  @brief Extended precision double to keep very large value.
*/
struct EpDoubleStruct {
  union EpTypeUnion		type;
  int				exponent;
};

/*---------------------------------------------------------------------------*/
/* Function prototypes                                                       */
/*---------------------------------------------------------------------------*/

#ifdef __cplusplus
extern "C" {
#endif

#ifdef __cplusplus
}
#endif

#endif /* EPD_H_ */
