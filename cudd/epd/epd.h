/**CHeaderFile*****************************************************************

  FileName    [epd.h]

  PackageName [epd]

  Synopsis    [The University of Colorado extended double precision package.]

  Description [arithmetic functions with extended double precision.]

  SeeAlso     []

  Author      [In-Ho Moon]

  Copyright [This file was created at the University of Colorado at
  Boulder.  The University of Colorado at Boulder makes no warranty
  about the suitability of this software for any purpose.  It is
  presented on an AS IS basis.]

  Revision    [$Id: epd.h,v 1.8 2004/01/01 06:54:27 fabio Exp $]

******************************************************************************/

#ifndef _EPD
#define _EPD

#ifdef __cplusplus
extern "C" {
#endif

/*---------------------------------------------------------------------------*/
/* Constant declarations                                                     */
/*---------------------------------------------------------------------------*/

#define	EPD_MAX_BIN	1023
#define	EPD_MAX_DEC	308
#define	EPD_EXP_INF	0x7ff

/*---------------------------------------------------------------------------*/
/* Structure declarations                                                    */
/*---------------------------------------------------------------------------*/

/**Struct**********************************************************************

  Synopsis    [IEEE double struct.]

  Description [IEEE double struct.]

  SeeAlso     []

******************************************************************************/
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

/**Struct**********************************************************************

  Synopsis    [IEEE double NaN struct.]

  Description [IEEE double NaN struct.]

  SeeAlso     []

******************************************************************************/
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

/**Struct**********************************************************************

  Synopsis    [Extended precision double to keep very large value.]

  Description [Extended precision double to keep very large value.]

  SeeAlso     []

******************************************************************************/
union EpTypeUnion {
  double			value;
  struct IeeeDoubleStruct	bits;
  struct IeeeNanStruct		nan;
};

struct EpDoubleStruct {
  union EpTypeUnion		type;
  int				exponent;
};

/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/
typedef struct EpDoubleStruct EpDouble;
typedef struct IeeeDoubleStruct IeeeDouble;
typedef struct IeeeNanStruct IeeeNan;
typedef union EpTypeUnion EpType;

/**AutomaticStart*************************************************************/

/*---------------------------------------------------------------------------*/
/* Function prototypes                                                       */
/*---------------------------------------------------------------------------*/

extern EpDouble *EpdAlloc(void);
extern int EpdCmp(const char *key1, const char *key2);
extern void EpdFree(EpDouble *epd);
extern void EpdGetString(EpDouble *epd, char *str);
extern void EpdConvert(double value, EpDouble *epd);
extern void EpdMultiply(EpDouble *epd1, double value);
extern void EpdMultiply2(EpDouble *epd1, EpDouble *epd2);
extern void EpdMultiply2Decimal(EpDouble *epd1, EpDouble *epd2);
extern void EpdMultiply3(EpDouble *epd1, EpDouble *epd2, EpDouble *epd3);
extern void EpdMultiply3Decimal(EpDouble *epd1, EpDouble *epd2, EpDouble *epd3);
extern void EpdDivide(EpDouble *epd1, double value);
extern void EpdDivide2(EpDouble *epd1, EpDouble *epd2);
extern void EpdDivide3(EpDouble *epd1, EpDouble *epd2, EpDouble *epd3);
extern void EpdAdd(EpDouble *epd1, double value);
extern void EpdAdd2(EpDouble *epd1, EpDouble *epd2);
extern void EpdAdd3(EpDouble *epd1, EpDouble *epd2, EpDouble *epd3);
extern void EpdSubtract(EpDouble *epd1, double value);
extern void EpdSubtract2(EpDouble *epd1, EpDouble *epd2);
extern void EpdSubtract3(EpDouble *epd1, EpDouble *epd2, EpDouble *epd3);
extern void EpdPow2(int n, EpDouble *epd);
extern void EpdPow2Decimal(int n, EpDouble *epd);
extern void EpdNormalize(EpDouble *epd);
extern void EpdNormalizeDecimal(EpDouble *epd);
extern void EpdGetValueAndDecimalExponent(EpDouble *epd, double *value, int *exponent);
extern int EpdGetExponent(double value);
extern int EpdGetExponentDecimal(double value);
extern void EpdMakeInf(EpDouble *epd, int sign);
extern void EpdMakeZero(EpDouble *epd, int sign);
extern void EpdMakeNan(EpDouble *epd);
extern void EpdCopy(EpDouble *from, EpDouble *to);
extern int EpdIsInf(EpDouble *epd);
extern int EpdIsZero(EpDouble *epd);
extern int EpdIsNan(EpDouble *epd);
extern int EpdIsNanOrInf(EpDouble *epd);
extern int IsInfDouble(double value);
extern int IsNanDouble(double value);
extern int IsNanOrInfDouble(double value);

/**AutomaticEnd***************************************************************/

#ifdef __cplusplus
}
#endif

#endif /* _EPD */
