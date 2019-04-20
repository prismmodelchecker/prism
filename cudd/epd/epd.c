/**
  @file

  @ingroup epd

  @brief Arithmetic functions with extended double precision.

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

#include <math.h>
#include "util.h"
#include "epdInt.h"


/**
  @brief Allocates an EpDouble struct.
*/
EpDouble *
EpdAlloc(void)
{
  EpDouble	*epd;

  epd = ALLOC(EpDouble, 1);
  return(epd);
}


/**
  @brief Compares two EpDouble struct.

  @return 0 if the two structures hold the same value; 1 otherwise.
*/
int
EpdCmp(const void *key1, const void *key2)
{
  EpDouble const *epd1 = (EpDouble const *) key1;
  EpDouble const *epd2 = (EpDouble const *) key2;
  if (epd1->type.value != epd2->type.value ||
      epd1->exponent != epd2->exponent) {
    return(1);
  }
  return(0);
}


/**
  @brief Frees an EpDouble struct.
*/
void
EpdFree(EpDouble *epd)
{
  FREE(epd);
}


/**
  @brief Converts an extended precision double value to a string.

  @sideeffect The string is written at the address passed in `str`.
*/
void
EpdGetString(EpDouble const *epd, char *str)
{
  double	value;
  int		exponent;
  char		*pos;

  if (!str) return;

  if (IsNanDouble(epd->type.value)) {
    sprintf(str, "NaN");
    return;
  } else if (IsInfDouble(epd->type.value)) {
    if (epd->type.bits.sign == 1)
      sprintf(str, "-inf");
    else
      sprintf(str, "inf");
    return;
  }

  assert(epd->type.bits.exponent == EPD_MAX_BIN ||
	 epd->type.bits.exponent == 0);

  EpdGetValueAndDecimalExponent(epd, &value, &exponent);
  sprintf(str, "%e", value);
  pos = strstr(str, "e");
  if (exponent >= 0) {
    if (exponent < 10)
      sprintf(pos + 1, "+0%d", exponent);
    else
      sprintf(pos + 1, "+%d", exponent);
  } else {
    exponent *= -1;
    if (exponent < 10)
      sprintf(pos + 1, "-0%d", exponent);
    else
      sprintf(pos + 1, "-%d", exponent);
  }
}


/**
  @brief Converts double to EpDouble struct.
*/
void
EpdConvert(double value, EpDouble *epd)
{
  epd->type.value = value;
  epd->exponent = 0;
  EpdNormalize(epd);
}


/**
  @brief Multiplies an extended precision double by a double.
*/
void
EpdMultiply(EpDouble *epd1, double value)
{
  EpDouble	epd2;
  double	tmp;
  int		exponent;

  if (EpdIsNan(epd1) || IsNanDouble(value)) {
    EpdMakeNan(epd1);
    return;
  } else if (EpdIsInf(epd1) || IsInfDouble(value)) {
    int	sign;

    EpdConvert(value, &epd2);
    sign = epd1->type.bits.sign ^ epd2.type.bits.sign;
    EpdMakeInf(epd1, sign);
    return;
  }

  assert(epd1->type.bits.exponent == EPD_MAX_BIN);

  EpdConvert(value, &epd2);
  tmp = epd1->type.value * epd2.type.value;
  exponent = epd1->exponent + epd2.exponent;
  epd1->type.value = tmp;
  epd1->exponent = exponent;
  EpdNormalize(epd1);
}


/**
  @brief Multiplies an extended precision double by another.
*/
void
EpdMultiply2(EpDouble *epd1, EpDouble const *epd2)
{
  double	value;
  int		exponent;

  if (EpdIsNan(epd1) || EpdIsNan(epd2)) {
    EpdMakeNan(epd1);
    return;
  } else if (EpdIsInf(epd1) || EpdIsInf(epd2)) {
    int	sign;

    sign = epd1->type.bits.sign ^ epd2->type.bits.sign;
    EpdMakeInf(epd1, sign);
    return;
  }

  assert(epd1->type.bits.exponent == EPD_MAX_BIN);
  assert(epd2->type.bits.exponent == EPD_MAX_BIN);

  value = epd1->type.value * epd2->type.value;
  exponent = epd1->exponent + epd2->exponent;
  epd1->type.value = value;
  epd1->exponent = exponent;
  EpdNormalize(epd1);
}


/**
  @brief Multiplies two extended precision double values.
*/
void
EpdMultiply2Decimal(EpDouble *epd1, EpDouble const *epd2)
{
  double	value;
  int		exponent;

  if (EpdIsNan(epd1) || EpdIsNan(epd2)) {
    EpdMakeNan(epd1);
    return;
  } else if (EpdIsInf(epd1) || EpdIsInf(epd2)) {
    int	sign;

    sign = epd1->type.bits.sign ^ epd2->type.bits.sign;
    EpdMakeInf(epd1, sign);
    return;
  }

  value = epd1->type.value * epd2->type.value;
  exponent = epd1->exponent + epd2->exponent;
  epd1->type.value = value;
  epd1->exponent = exponent;
  EpdNormalizeDecimal(epd1);
}


/**
  @brief Multiplies two extended precision double values.

  @details The result goes in the third operand.
*/
void
EpdMultiply3(EpDouble const *epd1, EpDouble const *epd2, EpDouble *epd3)
{
  if (EpdIsNan(epd1) || EpdIsNan(epd2)) {
    EpdMakeNan(epd3);
    return;
  } else if (EpdIsInf(epd1) || EpdIsInf(epd2)) {
    int	sign;

    sign = epd1->type.bits.sign ^ epd2->type.bits.sign;
    EpdMakeInf(epd3, sign);
    return;
  }

  assert(epd1->type.bits.exponent == EPD_MAX_BIN);
  assert(epd2->type.bits.exponent == EPD_MAX_BIN);

  epd3->type.value = epd1->type.value * epd2->type.value;
  epd3->exponent = epd1->exponent + epd2->exponent;
  EpdNormalize(epd3);
}


/**
  @brief Multiplies two extended precision double values.
*/
void
EpdMultiply3Decimal(EpDouble const *epd1, EpDouble const *epd2, EpDouble *epd3)
{
  if (EpdIsNan(epd1) || EpdIsNan(epd2)) {
    EpdMakeNan(epd3);
    return;
  } else if (EpdIsInf(epd1) || EpdIsInf(epd2)) {
    int	sign;

    sign = epd1->type.bits.sign ^ epd2->type.bits.sign;
    EpdMakeInf(epd3, sign);
    return;
  }

  epd3->type.value = epd1->type.value * epd2->type.value;
  epd3->exponent = epd1->exponent + epd2->exponent;
  EpdNormalizeDecimal(epd3);
}


/**
  @brief Divides an extended precision double by a double.
*/
void
EpdDivide(EpDouble *epd1, double value)
{
  EpDouble	epd2;
  double	tmp;
  int		exponent;

  if (EpdIsNan(epd1) || IsNanDouble(value)) {
    EpdMakeNan(epd1);
    return;
  } else if (EpdIsInf(epd1) || IsInfDouble(value)) {
    int	sign;

    EpdConvert(value, &epd2);
    if (EpdIsInf(epd1) && IsInfDouble(value)) {
      EpdMakeNan(epd1);
    } else if (EpdIsInf(epd1)) {
      sign = epd1->type.bits.sign ^ epd2.type.bits.sign;
      EpdMakeInf(epd1, sign);
    } else {
      sign = epd1->type.bits.sign ^ epd2.type.bits.sign;
      EpdMakeZero(epd1, sign);
    }
    return;
  }

  if (value == 0.0) {
    EpdMakeNan(epd1);
    return;
  }

  assert(epd1->type.bits.exponent == EPD_MAX_BIN);

  EpdConvert(value, &epd2);
  tmp = epd1->type.value / epd2.type.value;
  exponent = epd1->exponent - epd2.exponent;
  epd1->type.value = tmp;
  epd1->exponent = exponent;
  EpdNormalize(epd1);
}


/**
  @brief Divides an extended precision double by another.
*/
void
EpdDivide2(EpDouble *epd1, EpDouble const *epd2)
{
  double	value;
  int		exponent;

  if (EpdIsNan(epd1) || EpdIsNan(epd2)) {
    EpdMakeNan(epd1);
    return;
  } else if (EpdIsInf(epd1) || EpdIsInf(epd2)) {
    int	sign;

    if (EpdIsInf(epd1) && EpdIsInf(epd2)) {
      EpdMakeNan(epd1);
    } else if (EpdIsInf(epd1)) {
      sign = epd1->type.bits.sign ^ epd2->type.bits.sign;
      EpdMakeInf(epd1, sign);
    } else {
      sign = epd1->type.bits.sign ^ epd2->type.bits.sign;
      EpdMakeZero(epd1, sign);
    }
    return;
  }

  if (epd2->type.value == 0.0) {
    EpdMakeNan(epd1);
    return;
  }

  assert(epd1->type.bits.exponent == EPD_MAX_BIN);
  assert(epd2->type.bits.exponent == EPD_MAX_BIN);

  value = epd1->type.value / epd2->type.value;
  exponent = epd1->exponent - epd2->exponent;
  epd1->type.value = value;
  epd1->exponent = exponent;
  EpdNormalize(epd1);
}


/**
  @brief Divides two extended precision double values.
*/
void
EpdDivide3(EpDouble const *epd1, EpDouble const *epd2, EpDouble *epd3)
{
  if (EpdIsNan(epd1) || EpdIsNan(epd2)) {
    EpdMakeNan(epd3);
    return;
  } else if (EpdIsInf(epd1) || EpdIsInf(epd2)) {
    int	sign;

    if (EpdIsInf(epd1) && EpdIsInf(epd2)) {
      EpdMakeNan(epd3);
    } else if (EpdIsInf(epd1)) {
      sign = epd1->type.bits.sign ^ epd2->type.bits.sign;
      EpdMakeInf(epd3, sign);
    } else {
      sign = epd1->type.bits.sign ^ epd2->type.bits.sign;
      EpdMakeZero(epd3, sign);
    }
    return;
  }

  if (epd2->type.value == 0.0) {
    EpdMakeNan(epd3);
    return;
  }

  assert(epd1->type.bits.exponent == EPD_MAX_BIN);
  assert(epd2->type.bits.exponent == EPD_MAX_BIN);

  epd3->type.value = epd1->type.value / epd2->type.value;
  epd3->exponent = epd1->exponent - epd2->exponent;
  EpdNormalize(epd3);
}


/**
  @brief Adds a double to an extended precision double.
*/
void
EpdAdd(EpDouble *epd1, double value)
{
  EpDouble	epd2;
  double	tmp;
  int		exponent, diff;

  if (EpdIsNan(epd1) || IsNanDouble(value)) {
    EpdMakeNan(epd1);
    return;
  } else if (EpdIsInf(epd1) || IsInfDouble(value)) {
    int	sign;

    EpdConvert(value, &epd2);
    if (EpdIsInf(epd1) && IsInfDouble(value)) {
      sign = epd1->type.bits.sign ^ epd2.type.bits.sign;
      if (sign == 1)
	EpdMakeNan(epd1);
    } else if (EpdIsInf(&epd2)) {
      EpdCopy(&epd2, epd1);
    }
    return;
  }

  assert(epd1->type.bits.exponent == EPD_MAX_BIN);

  EpdConvert(value, &epd2);
  if (epd1->exponent > epd2.exponent) {
    diff = epd1->exponent - epd2.exponent;
    if (diff <= EPD_MAX_BIN)
      tmp = epd1->type.value + epd2.type.value / pow((double)2.0, (double)diff);
    else
      tmp = epd1->type.value;
    exponent = epd1->exponent;
  } else if (epd1->exponent < epd2.exponent) {
    diff = epd2.exponent - epd1->exponent;
    if (diff <= EPD_MAX_BIN)
      tmp = epd1->type.value / pow((double)2.0, (double)diff) + epd2.type.value;
    else
      tmp = epd2.type.value;
    exponent = epd2.exponent;
  } else {
    tmp = epd1->type.value + epd2.type.value;
    exponent = epd1->exponent;
  }
  epd1->type.value = tmp;
  epd1->exponent = exponent;
  EpdNormalize(epd1);
}


/**
  @brief Adds an extended precision double to another.

  @details The sum goes in the first argument.
*/
void
EpdAdd2(EpDouble *epd1, EpDouble const *epd2)
{
  double	value;
  int		exponent, diff;

  if (EpdIsNan(epd1) || EpdIsNan(epd2)) {
    EpdMakeNan(epd1);
    return;
  } else if (EpdIsInf(epd1) || EpdIsInf(epd2)) {
    int	sign;

    if (EpdIsInf(epd1) && EpdIsInf(epd2)) {
      sign = epd1->type.bits.sign ^ epd2->type.bits.sign;
      if (sign == 1)
	EpdMakeNan(epd1);
    } else if (EpdIsInf(epd2)) {
      EpdCopy(epd2, epd1);
    }
    return;
  }

  assert(epd1->type.bits.exponent == EPD_MAX_BIN);
  assert(epd2->type.bits.exponent == EPD_MAX_BIN);

  if (epd1->exponent > epd2->exponent) {
    diff = epd1->exponent - epd2->exponent;
    if (diff <= EPD_MAX_BIN) {
      value = epd1->type.value +
		epd2->type.value / pow((double)2.0, (double)diff);
    } else
      value = epd1->type.value;
    exponent = epd1->exponent;
  } else if (epd1->exponent < epd2->exponent) {
    diff = epd2->exponent - epd1->exponent;
    if (diff <= EPD_MAX_BIN) {
      value = epd1->type.value / pow((double)2.0, (double)diff) +
		epd2->type.value;
    } else
      value = epd2->type.value;
    exponent = epd2->exponent;
  } else {
    value = epd1->type.value + epd2->type.value;
    exponent = epd1->exponent;
  }
  epd1->type.value = value;
  epd1->exponent = exponent;
  EpdNormalize(epd1);
}


/**
  @brief Adds two extended precision double values.
*/
void
EpdAdd3(EpDouble const *epd1, EpDouble const *epd2, EpDouble *epd3)
{
  double	value;
  int		exponent, diff;

  if (EpdIsNan(epd1) || EpdIsNan(epd2)) {
    EpdMakeNan(epd3);
    return;
  } else if (EpdIsInf(epd1) || EpdIsInf(epd2)) {
    int	sign;

    if (EpdIsInf(epd1) && EpdIsInf(epd2)) {
      sign = epd1->type.bits.sign ^ epd2->type.bits.sign;
      if (sign == 1)
	EpdMakeNan(epd3);
      else
	EpdCopy(epd1, epd3);
    } else if (EpdIsInf(epd1)) {
      EpdCopy(epd1, epd3);
    } else {
      EpdCopy(epd2, epd3);
    }
    return;
  }

  assert(epd1->type.bits.exponent == EPD_MAX_BIN);
  assert(epd2->type.bits.exponent == EPD_MAX_BIN);

  if (epd1->exponent > epd2->exponent) {
    diff = epd1->exponent - epd2->exponent;
    if (diff <= EPD_MAX_BIN) {
      value = epd1->type.value +
		epd2->type.value / pow((double)2.0, (double)diff);
    } else
      value = epd1->type.value;
    exponent = epd1->exponent;
  } else if (epd1->exponent < epd2->exponent) {
    diff = epd2->exponent - epd1->exponent;
    if (diff <= EPD_MAX_BIN) {
      value = epd1->type.value / pow((double)2.0, (double)diff) +
		epd2->type.value;
    } else
      value = epd2->type.value;
    exponent = epd2->exponent;
  } else {
    value = epd1->type.value + epd2->type.value;
    exponent = epd1->exponent;
  }
  epd3->type.value = value;
  epd3->exponent = exponent;
  EpdNormalize(epd3);
}


/**
  @brief Subtracts a double from an extended precision double.
*/
void
EpdSubtract(EpDouble *epd1, double value)
{
  EpDouble	epd2;
  double	tmp;
  int		exponent, diff;

  if (EpdIsNan(epd1) || IsNanDouble(value)) {
    EpdMakeNan(epd1);
    return;
  } else if (EpdIsInf(epd1) || IsInfDouble(value)) {
    int	sign;

    EpdConvert(value, &epd2);
    if (EpdIsInf(epd1) && IsInfDouble(value)) {
      sign = epd1->type.bits.sign ^ epd2.type.bits.sign;
      if (sign == 0)
	EpdMakeNan(epd1);
    } else if (EpdIsInf(&epd2)) {
      EpdCopy(&epd2, epd1);
    }
    return;
  }

  assert(epd1->type.bits.exponent == EPD_MAX_BIN);

  EpdConvert(value, &epd2);
  if (epd1->exponent > epd2.exponent) {
    diff = epd1->exponent - epd2.exponent;
    if (diff <= EPD_MAX_BIN)
      tmp = epd1->type.value - epd2.type.value / pow((double)2.0, (double)diff);
    else
      tmp = epd1->type.value;
    exponent = epd1->exponent;
  } else if (epd1->exponent < epd2.exponent) {
    diff = epd2.exponent - epd1->exponent;
    if (diff <= EPD_MAX_BIN)
      tmp = epd1->type.value / pow((double)2.0, (double)diff) - epd2.type.value;
    else
      tmp = epd2.type.value * (double)(-1.0);
    exponent = epd2.exponent;
  } else {
    tmp = epd1->type.value - epd2.type.value;
    exponent = epd1->exponent;
  }
  epd1->type.value = tmp;
  epd1->exponent = exponent;
  EpdNormalize(epd1);
}


/**
  @brief Subtracts an extended precision double from another.
*/
void
EpdSubtract2(EpDouble *epd1, EpDouble const *epd2)
{
  double	value;
  int		exponent, diff;

  if (EpdIsNan(epd1) || EpdIsNan(epd2)) {
    EpdMakeNan(epd1);
    return;
  } else if (EpdIsInf(epd1) || EpdIsInf(epd2)) {
    int	sign;

    if (EpdIsInf(epd1) && EpdIsInf(epd2)) {
      sign = epd1->type.bits.sign ^ epd2->type.bits.sign;
      if (sign == 0)
	EpdMakeNan(epd1);
    } else if (EpdIsInf(epd2)) {
      EpdCopy(epd2, epd1);
    }
    return;
  }

  assert(epd1->type.bits.exponent == EPD_MAX_BIN);
  assert(epd2->type.bits.exponent == EPD_MAX_BIN);

  if (epd1->exponent > epd2->exponent) {
    diff = epd1->exponent - epd2->exponent;
    if (diff <= EPD_MAX_BIN) {
      value = epd1->type.value -
		epd2->type.value / pow((double)2.0, (double)diff);
    } else
      value = epd1->type.value;
    exponent = epd1->exponent;
  } else if (epd1->exponent < epd2->exponent) {
    diff = epd2->exponent - epd1->exponent;
    if (diff <= EPD_MAX_BIN) {
      value = epd1->type.value / pow((double)2.0, (double)diff) -
		epd2->type.value;
    } else
      value = epd2->type.value * (double)(-1.0);
    exponent = epd2->exponent;
  } else {
    value = epd1->type.value - epd2->type.value;
    exponent = epd1->exponent;
  }
  epd1->type.value = value;
  epd1->exponent = exponent;
  EpdNormalize(epd1);
}


/**
  @brief Subtracts two extended precision double values.
*/
void
EpdSubtract3(EpDouble const *epd1, EpDouble const *epd2, EpDouble *epd3)
{
  double	value;
  int		exponent, diff;

  if (EpdIsNan(epd1) || EpdIsNan(epd2)) {
    EpdMakeNan(epd3);
    return;
  } else if (EpdIsInf(epd1) || EpdIsInf(epd2)) {
    int	sign;

    if (EpdIsInf(epd1) && EpdIsInf(epd2)) {
      sign = epd1->type.bits.sign ^ epd2->type.bits.sign;
      if (sign == 0)
	EpdCopy(epd1, epd3);
      else
	EpdMakeNan(epd3);
    } else if (EpdIsInf(epd1)) {
      EpdCopy(epd1, epd3);
    } else {
      sign = epd2->type.bits.sign ^ 0x1;
      EpdMakeInf(epd3, sign);
    }
    return;
  }

  assert(epd1->type.bits.exponent == EPD_MAX_BIN);
  assert(epd2->type.bits.exponent == EPD_MAX_BIN);

  if (epd1->exponent > epd2->exponent) {
    diff = epd1->exponent - epd2->exponent;
    if (diff <= EPD_MAX_BIN) {
      value = epd1->type.value -
		epd2->type.value / pow((double)2.0, (double)diff);
    } else
      value = epd1->type.value;
    exponent = epd1->exponent;
  } else if (epd1->exponent < epd2->exponent) {
    diff = epd2->exponent - epd1->exponent;
    if (diff <= EPD_MAX_BIN) {
      value = epd1->type.value / pow((double)2.0, (double)diff) -
		epd2->type.value;
    } else
      value = epd2->type.value * (double)(-1.0);
    exponent = epd2->exponent;
  } else {
    value = epd1->type.value - epd2->type.value;
    exponent = epd1->exponent;
  }
  epd3->type.value = value;
  epd3->exponent = exponent;
  EpdNormalize(epd3);
}


/**
  @brief Computes extended precision pow of base 2.
*/
void
EpdPow2(int n, EpDouble *epd)
{
  if (n <= EPD_MAX_BIN) {
    EpdConvert(pow((double)2.0, (double)n), epd);
  } else {
    EpDouble	epd1, epd2;
    int		n1, n2;

    n1 = n / 2;
    n2 = n - n1;
    EpdPow2(n1, &epd1);
    EpdPow2(n2, &epd2);
    EpdMultiply3(&epd1, &epd2, epd);
  }
}


/**
  @brief Computes extended precision pow of base 2.
*/
void
EpdPow2Decimal(int n, EpDouble *epd)
{
  if (n <= EPD_MAX_BIN) {
    epd->type.value = pow((double)2.0, (double)n);
    epd->exponent = 0;
    EpdNormalizeDecimal(epd);
  } else {
    EpDouble	epd1, epd2;
    int		n1, n2;

    n1 = n / 2;
    n2 = n - n1;
    EpdPow2Decimal(n1, &epd1);
    EpdPow2Decimal(n2, &epd2);
    EpdMultiply3Decimal(&epd1, &epd2, epd);
  }
}


/**
  @brief Normalize an extended precision double value.
*/
void
EpdNormalize(EpDouble *epd)
{
  int		exponent;

  if (IsNanOrInfDouble(epd->type.value)) {
    epd->exponent = 0;
    return;
  }

  exponent = EpdGetExponent(epd->type.value);
  if (exponent == EPD_MAX_BIN)
    return;
  exponent -= EPD_MAX_BIN;
  epd->type.bits.exponent = EPD_MAX_BIN;
  epd->exponent += exponent;
}


/**
  @brief Normalize an extended precision double value.
*/
void
EpdNormalizeDecimal(EpDouble *epd)
{
  int		exponent;

  if (IsNanOrInfDouble(epd->type.value)) {
    epd->exponent = 0;
    return;
  }

  exponent = EpdGetExponentDecimal(epd->type.value);
  epd->type.value /= pow((double)10.0, (double)exponent);
  epd->exponent += exponent;
}


/**
  @brief Returns value and decimal exponent of EpDouble.
*/
void
EpdGetValueAndDecimalExponent(EpDouble const *epd, double *value, int *exponent)
{
  EpDouble	epd1, epd2;

  if (EpdIsNanOrInf(epd)) {
    *exponent = EPD_EXP_INF;
    *value = 0.0;
    return;
  }

  if (EpdIsZero(epd)) {
    *value = 0.0;
    *exponent = 0;
    return;
  }

  epd1.type.value = epd->type.value;
  epd1.exponent = 0;
  EpdPow2Decimal(epd->exponent, &epd2);
  EpdMultiply2Decimal(&epd1, &epd2);

  *value = epd1.type.value;
  *exponent = epd1.exponent;
}

/**
  @brief Returns the exponent value of a double.
*/
int
EpdGetExponent(double value)
{
  int		exponent;
  EpDouble	epd;

  epd.type.value = value;
  exponent = epd.type.bits.exponent;
  return(exponent);
}


/**
  @brief Returns the decimal exponent value of a double.
*/
int
EpdGetExponentDecimal(double value)
{
  char	*pos, str[24];
  int	exponent;

  sprintf(str, "%E", value);
  pos = strstr(str, "E");
  sscanf(pos, "E%d", &exponent);
  return(exponent);
}


/**
  @brief Makes EpDouble Inf.
*/
void
EpdMakeInf(EpDouble *epd, int sign)
{
  epd->type.bits.mantissa1 = 0;
  epd->type.bits.mantissa0 = 0;
  epd->type.bits.exponent = EPD_EXP_INF;
  epd->type.bits.sign = sign;
  epd->exponent = 0;
}


/**
  @brief Makes EpDouble Zero.
*/
void
EpdMakeZero(EpDouble *epd, int sign)
{
  epd->type.bits.mantissa1 = 0;
  epd->type.bits.mantissa0 = 0;
  epd->type.bits.exponent = 0;
  epd->type.bits.sign = sign;
  epd->exponent = 0;
}


/**
  @brief Makes EpDouble NaN.
*/
void
EpdMakeNan(EpDouble *epd)
{
  epd->type.nan.mantissa1 = 0;
  epd->type.nan.mantissa0 = 0;
  epd->type.nan.quiet_bit = 1;
  epd->type.nan.exponent = EPD_EXP_INF;
  epd->type.nan.sign = 1;
  epd->exponent = 0;
}


/**
  @brief Copies an EpDouble struct.
*/
void
EpdCopy(EpDouble const *from, EpDouble *to)
{
  to->type.value = from->type.value;
  to->exponent = from->exponent;
}


/**
  @brief Checks whether the value is Inf.
*/
int
EpdIsInf(EpDouble const *epd)
{
  return(IsInfDouble(epd->type.value));
}


/**
  @brief Checks whether the value is Zero.
*/
int
EpdIsZero(EpDouble const *epd)
{
  if (epd->type.value == 0.0)
    return(1);
  else
    return(0);
}


/**
  @brief Checks whether the value is NaN.
*/
int
EpdIsNan(EpDouble const *epd)
{
  return(IsNanDouble(epd->type.value));
}


/**
  @brief Checks whether the value is NaN or Inf.
*/
int
EpdIsNanOrInf(EpDouble const *epd)
{
  return(IsNanOrInfDouble(epd->type.value));
}


/**
  @brief Checks whether the value is Inf.
*/
int
IsInfDouble(double value)
{
  EpType val;

  val.value = value;
  if (val.bits.exponent == EPD_EXP_INF &&
      val.bits.mantissa0 == 0 &&
      val.bits.mantissa1 == 0) {
    if (val.bits.sign == 0)
      return(1);
    else
      return(-1);
  }
  return(0);
}


/**
  @brief Checks whether the value is NaN.
*/
int
IsNanDouble(double value)
{
  EpType	val;
  
  val.value = value;
  if (val.nan.exponent == EPD_EXP_INF &&
      val.nan.sign == 1 &&
      val.nan.quiet_bit == 1 &&
      val.nan.mantissa0 == 0 &&
      val.nan.mantissa1 == 0) {
    return(1);
  }
  return(0);
}


/**
  @brief Checks whether the value is NaN or Inf.
*/
int
IsNanOrInfDouble(double value)
{
  EpType	val;

  val.value = value;
  if (val.nan.exponent == EPD_EXP_INF &&
      val.nan.mantissa0 == 0 &&
      val.nan.mantissa1 == 0 &&
      (val.nan.sign == 1 || val.nan.quiet_bit == 0)) {
    return(1);
  }
  return(0);
}
