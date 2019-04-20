/**
  @file 

  @ingroup epd

  @brief The University of Colorado extended double precision package.

  @details Arithmetic functions with extended double precision.  The floating
  point numbers manipulated by this package use an int to hold the exponent.
  The significand has the same precision as a standard double.

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

#ifndef EPD_H_
#define EPD_H_

#ifdef __cplusplus
extern "C" {
#endif

/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/

/**
   @brief The type of extended precision floating-point numbers.
*/
typedef struct EpDoubleStruct EpDouble;

/*---------------------------------------------------------------------------*/
/* Function prototypes                                                       */
/*---------------------------------------------------------------------------*/

extern EpDouble *EpdAlloc(void);
extern int EpdCmp(const void *key1, const void *key2);
extern void EpdFree(EpDouble *epd);
extern void EpdGetString(EpDouble const *epd, char *str);
extern void EpdConvert(double value, EpDouble *epd);
extern void EpdMultiply(EpDouble *epd1, double value);
extern void EpdMultiply2(EpDouble *epd1, EpDouble const *epd2);
extern void EpdMultiply2Decimal(EpDouble *epd1, EpDouble const *epd2);
extern void EpdMultiply3(EpDouble const *epd1, EpDouble const *epd2, EpDouble *epd3);
extern void EpdMultiply3Decimal(EpDouble const *epd1, EpDouble const *epd2, EpDouble *epd3);
extern void EpdDivide(EpDouble *epd1, double value);
extern void EpdDivide2(EpDouble *epd1, EpDouble const *epd2);
extern void EpdDivide3(EpDouble const *epd1, EpDouble const *epd2, EpDouble *epd3);
extern void EpdAdd(EpDouble *epd1, double value);
extern void EpdAdd2(EpDouble *epd1, EpDouble const *epd2);
extern void EpdAdd3(EpDouble const *epd1, EpDouble const *epd2, EpDouble *epd3);
extern void EpdSubtract(EpDouble *epd1, double value);
extern void EpdSubtract2(EpDouble *epd1, EpDouble const *epd2);
extern void EpdSubtract3(EpDouble const *epd1, EpDouble const *epd2, EpDouble *epd3);
extern void EpdPow2(int n, EpDouble *epd);
extern void EpdPow2Decimal(int n, EpDouble *epd);
extern void EpdNormalize(EpDouble *epd);
extern void EpdNormalizeDecimal(EpDouble *epd);
extern void EpdGetValueAndDecimalExponent(EpDouble const *epd, double *value, int *exponent);
extern int EpdGetExponent(double value);
extern int EpdGetExponentDecimal(double value);
extern void EpdMakeInf(EpDouble *epd, int sign);
extern void EpdMakeZero(EpDouble *epd, int sign);
extern void EpdMakeNan(EpDouble *epd);
extern void EpdCopy(EpDouble const *from, EpDouble *to);
extern int EpdIsInf(EpDouble const *epd);
extern int EpdIsZero(EpDouble const *epd);
extern int EpdIsNan(EpDouble const *epd);
extern int EpdIsNanOrInf(EpDouble const *epd);
extern int IsInfDouble(double value);
extern int IsNanDouble(double value);
extern int IsNanOrInfDouble(double value);

#ifdef __cplusplus
}
#endif

#endif /* EPD_H_ */
