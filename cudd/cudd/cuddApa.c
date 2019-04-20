/**
  @file

  @ingroup cudd

  @brief Arbitrary precision arithmetic functions.

  @details This file provides just enough functionality as needed
  by CUDD to compute the number of minterms of functions with many
  variables.

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

#include "util.h"
#include "cuddInt.h"

/*---------------------------------------------------------------------------*/
/* Constant declarations                                                     */
/*---------------------------------------------------------------------------*/

/* These constants define the digits used in the representation of
** arbitrary precision integers.
*/
#define DD_APA_BITS	((int) sizeof(DdApaDigit) * 8)
#define DD_APA_BASE	((DdApaDoubleDigit) 1 << DD_APA_BITS)
#define DD_APA_MASK	(DD_APA_BASE - 1)

/*---------------------------------------------------------------------------*/
/* Stucture declarations                                                     */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/

/**
   @brief Type used for intermediate results.
*/
typedef uint64_t DdApaDoubleDigit;

/*---------------------------------------------------------------------------*/
/* Variable declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Macro declarations                                                        */
/*---------------------------------------------------------------------------*/

/**
  @brief Extract the least significant digit of a double digit.

  @sideeffect None

  @see DD_MSDIGIT

*/
#define DD_LSDIGIT(x)	((x) & DD_APA_MASK)


/**
  @brief Extract the most significant digit of a double digit.

  @sideeffect None

  @see DD_LSDIGIT

*/
#define DD_MSDIGIT(x)	((x) >> DD_APA_BITS)

/** \cond */

/*---------------------------------------------------------------------------*/
/* Static function prototypes                                                */
/*---------------------------------------------------------------------------*/

static DdApaNumber cuddApaCountMintermAux (DdManager const * manager, DdNode * node, int digits, DdApaNumber mmax, DdApaNumber mmin, st_table * table);
static enum st_retval cuddApaStCountfree (void * key, void * value, void * arg);

/** \endcond */


/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Returns the number of digits for an arbitrary precision
  integer.

  @details Finds the number of digits for an arbitrary precision
  integer given the maximum number of binary digits.  The number of
  binary digits should be positive.

  @sideeffect None

*/
int
Cudd_ApaNumberOfDigits(
  int binaryDigits)
{
    int digits;

    digits = binaryDigits / DD_APA_BITS;
    if ((digits * DD_APA_BITS) != binaryDigits)
	digits++;
    return(digits);

} /* end of Cudd_ApaNumberOfDigits */


/**
  @brief Allocates memory for an arbitrary precision integer.

  @return a pointer to the allocated memory if successful;
  NULL otherwise.

  @sideeffect None

  @see Cudd_FreeApaNumber
*/
DdApaNumber
Cudd_NewApaNumber(
  int digits)
{
    return(ALLOC(DdApaDigit, digits));

} /* end of Cudd_NewApaNumber */


/**
  @brief Frees an arbitrary precision integer.

  @sideeffect None

  @see Cudd_NewApaNumber
*/
void
Cudd_FreeApaNumber(
  DdApaNumber number)
{
    FREE(number);

} /* end of Cudd_FreeApaNumber */


/**
  @brief Makes a copy of an arbitrary precision integer.

  @sideeffect Changes parameter <code>dest</code>.

*/
void
Cudd_ApaCopy(
  int digits,
  DdConstApaNumber source,
  DdApaNumber dest)
{
    int i;

    for (i = 0; i < digits; i++) {
	dest[i] = source[i];
    }

} /* end of Cudd_ApaCopy */


/**
  @brief Adds two arbitrary precision integers.

  @return the carry out of the most significant digit.

  @sideeffect The result of the sum is stored in parameter <code>sum</code>.

*/
DdApaDigit
Cudd_ApaAdd(
  int  digits,
  DdConstApaNumber a,
  DdConstApaNumber b,
  DdApaNumber sum)
{
    int i;
    DdApaDoubleDigit partial = 0;

    for (i = digits - 1; i >= 0; i--) {
	partial = DD_MSDIGIT(partial) + a[i] + b[i];
	sum[i] = (DdApaDigit) DD_LSDIGIT(partial);
    }
    return((DdApaDigit) DD_MSDIGIT(partial));

} /* end of Cudd_ApaAdd */


/**
  @brief Subtracts two arbitrary precision integers.

  @return the borrow out of the most significant digit.

  @sideeffect The result of the subtraction is stored in parameter
  <code>diff</code>.

*/
DdApaDigit
Cudd_ApaSubtract(
  int  digits,
  DdConstApaNumber a,
  DdConstApaNumber b,
  DdApaNumber diff)
{
    int i;
    DdApaDoubleDigit partial = DD_APA_BASE;

    for (i = digits - 1; i >= 0; i--) {
        partial = DD_MSDIGIT(partial) + DD_APA_MASK + a[i] - b[i];
	diff[i] = (DdApaDigit) DD_LSDIGIT(partial);
    }
    return((DdApaDigit) DD_MSDIGIT(partial) - 1);

} /* end of Cudd_ApaSubtract */


/**
  @brief Divides an arbitrary precision integer by a digit.

  @return the remainder digit.

  @sideeffect The quotient is returned in parameter <code>quotient</code>.

*/
DdApaDigit
Cudd_ApaShortDivision(
  int  digits,
  DdConstApaNumber dividend,
  DdApaDigit divisor,
  DdApaNumber quotient)
{
    int i;
    DdApaDigit remainder;
    DdApaDoubleDigit partial;

    remainder = 0;
    for (i = 0; i < digits; i++) {
	partial = remainder * DD_APA_BASE + dividend[i];
	quotient[i] = (DdApaDigit) (partial/(DdApaDoubleDigit)divisor);
	remainder = (DdApaDigit) (partial % divisor);
    }

    return(remainder);

} /* end of Cudd_ApaShortDivision */


/**
  @brief Divides an arbitrary precision integer by an integer.

  @details Divides an arbitrary precision integer by a 32-bit unsigned
  integer. This procedure relies on the assumption that the number of
  bits of a DdApaDigit plus the number of bits of an unsigned int is
  less the number of bits of the mantissa of a double. This guarantees
  that the product of a DdApaDigit and an unsigned int can be
  represented without loss of precision by a double. On machines where
  this assumption is not satisfied, this procedure will malfunction.

  @return the remainder.

  @sideeffect The quotient is returned in parameter <code>quotient</code>.

  @deprecated The assumption on which the correctness of this function rests
  is not satisfied by modern-day 64-bit CPUs.

  @see Cudd_ApaShortDivision

*/
unsigned int
Cudd_ApaIntDivision(
  int  digits,
  DdConstApaNumber dividend,
  unsigned int divisor,
  DdApaNumber quotient)
{
    int i;
    double partial;
    unsigned int remainder = 0;
    double ddiv = (double) divisor;

    for (i = 0; i < digits; i++) {
	partial = (double) remainder * DD_APA_BASE + dividend[i];
	quotient[i] = (DdApaDigit) (partial / ddiv);
	remainder = (unsigned int) (partial - ((double)quotient[i] * ddiv));
    }

    return(remainder);

} /* end of Cudd_ApaIntDivision */


/**
  @brief Shifts right an arbitrary precision integer by one binary
  place.

  @details The most significant binary digit of the result is taken
  from parameter <code>in</code>.

  @sideeffect The result is returned in parameter <code>b</code>.

*/
void
Cudd_ApaShiftRight(
  int digits,
  DdApaDigit in,
  DdConstApaNumber a,
  DdApaNumber b)
{
    int i;

    for (i = digits - 1; i > 0; i--) {
	b[i] = (a[i] >> 1) | ((a[i-1] & 1) << (DD_APA_BITS - 1));
    }
    b[0] = (a[0] >> 1) | (in << (DD_APA_BITS - 1));

} /* end of Cudd_ApaShiftRight */


/**
  @brief Sets an arbitrary precision integer to a one-digit literal.

  @sideeffect The result is returned in parameter <code>number</code>.

*/
void
Cudd_ApaSetToLiteral(
  int digits,
  DdApaNumber number,
  DdApaDigit literal)
{
    int i;

    for (i = 0; i < digits - 1; i++)
	number[i] = 0;
    number[digits - 1] = literal;

} /* end of Cudd_ApaSetToLiteral */


/**
  @brief Sets an arbitrary precision integer to a power of two.

  @details If the power of two is too large to be represented, the number
  is set to 0.

  @sideeffect The result is returned in parameter <code>number</code>.

*/
void
Cudd_ApaPowerOfTwo(
  int digits,
  DdApaNumber number,
  int power)
{
    int i;
    int index;

    for (i = 0; i < digits; i++)
	number[i] = 0;
    i = digits - 1 - power / DD_APA_BITS;
    if (i < 0) return;
    index = power & (DD_APA_BITS - 1);
    number[i] = (DdApaDigit) 1 << index;

} /* end of Cudd_ApaPowerOfTwo */


/**
  @brief Compares two arbitrary precision integers.

  @return 1 if the first number is larger; 0 if they are equal; -1 if
  the second number is larger.

  @sideeffect None

*/
int
Cudd_ApaCompare(
  int digitsFirst,
  DdConstApaNumber first,
  int digitsSecond,
  DdConstApaNumber second)
{
    int i;
    int firstNZ, secondNZ;

    /* Find first non-zero in both numbers. */
    for (firstNZ = 0; firstNZ < digitsFirst; firstNZ++)
	if (first[firstNZ] != 0) break;
    for (secondNZ = 0; secondNZ < digitsSecond; secondNZ++)
	if (second[secondNZ] != 0) break;
    if (digitsFirst - firstNZ > digitsSecond - secondNZ) return(1);
    else if (digitsFirst - firstNZ < digitsSecond - secondNZ) return(-1);
    for (i = 0; i < digitsFirst - firstNZ; i++) {
	if (first[firstNZ + i] > second[secondNZ + i]) return(1);
	else if (first[firstNZ + i] < second[secondNZ + i]) return(-1);
    }
    return(0);

} /* end of Cudd_ApaCompare */


/**
  @brief Compares the ratios of two arbitrary precision integers to two
  unsigned ints.

  @return 1 if the first number is larger; 0 if they are equal; -1 if
  the second number is larger.

  @sideeffect None

*/
int
Cudd_ApaCompareRatios(
  int digitsFirst,
  DdConstApaNumber firstNum,
  unsigned int firstDen,
  int digitsSecond,
  DdConstApaNumber secondNum,
  unsigned int secondDen)
{
    int result;
    DdApaNumber first, second;
    unsigned int firstRem, secondRem;

    first = Cudd_NewApaNumber(digitsFirst);
    firstRem = Cudd_ApaIntDivision(digitsFirst,firstNum,firstDen,first);
    second = Cudd_NewApaNumber(digitsSecond);
    secondRem = Cudd_ApaIntDivision(digitsSecond,secondNum,secondDen,second);
    result = Cudd_ApaCompare(digitsFirst,first,digitsSecond,second);
    FREE(first);
    FREE(second);
    if (result == 0) {
	if ((double)firstRem/firstDen > (double)secondRem/secondDen)
	    return(1);
	else if ((double)firstRem/firstDen < (double)secondRem/secondDen)
	    return(-1);
    }
    return(result);

} /* end of Cudd_ApaCompareRatios */


/**
  @brief Prints an arbitrary precision integer in hexadecimal format.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see Cudd_ApaPrintDecimal Cudd_ApaPrintExponential

*/
int
Cudd_ApaPrintHex(
  FILE * fp,
  int digits,
  DdConstApaNumber number)
{
    int i, result;

    for (i = 0; i < digits; i++) {
        result = fprintf(fp, "%0*x", (int) sizeof(DdApaDigit) * 2, number[i]);
	if (result == EOF)
	    return(0);
    }
    return(1);

} /* end of Cudd_ApaPrintHex */


/**
  @brief Prints an arbitrary precision integer in decimal format.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see Cudd_ApaPrintHex Cudd_ApaPrintExponential

*/
int
Cudd_ApaPrintDecimal(
  FILE * fp,
  int digits,
  DdConstApaNumber number)
{
    int i, result;
    DdApaDigit remainder;
    DdApaNumber work;
    unsigned char *decimal;
    int leadingzero;
    int decimalDigits = (int) (digits * log10((double) DD_APA_BASE)) + 1;

    work = Cudd_NewApaNumber(digits);
    if (work == NULL)
	return(0);
    decimal = ALLOC(unsigned char, decimalDigits);
    if (decimal == NULL) {
	FREE(work);
	return(0);
    }
    Cudd_ApaCopy(digits,number,work);
    for (i = decimalDigits - 1; i >= 0; i--) {
	remainder = Cudd_ApaShortDivision(digits,work,(DdApaDigit) 10,work);
	decimal[i] = (unsigned char) remainder;
    }
    FREE(work);

    leadingzero = 1;
    for (i = 0; i < decimalDigits; i++) {
	leadingzero = leadingzero && (decimal[i] == 0);
	if ((!leadingzero) || (i == (decimalDigits - 1))) {
	    result = fprintf(fp,"%1d",decimal[i]);
	    if (result == EOF) {
		FREE(decimal);
		return(0);
	    }
	}
    }
    FREE(decimal);
    return(1);

} /* end of Cudd_ApaPrintDecimal */


/**
  @brief converts an arbitrary precision integer to a string in decimal format.

  @return the string if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_ApaPrintDecimal

*/
char *
Cudd_ApaStringDecimal(
  int digits,
  DdConstApaNumber number)
{
    int i, fsd;
    DdApaDigit remainder;
    DdApaNumber work;
    char *decimal, *ret;
    int decimalDigits = (int) (digits * log10((double) DD_APA_BASE)) + 1;

    work = Cudd_NewApaNumber(digits);
    if (work == NULL) {
	return(0);
    }
    decimal = ALLOC(char, decimalDigits);
    if (decimal == NULL) {
	FREE(work);
	return(0);
    }
    Cudd_ApaCopy(digits,number,work);
    for (i = decimalDigits - 1; i >= 0; i--) {
	remainder = Cudd_ApaShortDivision(digits,work,(DdApaDigit) 10,work);
	decimal[i] = (char) remainder;
    }
    FREE(work);

    /* Find first significant digit. */
    for (fsd = 0; fsd < decimalDigits-1; fsd++) {
        if (decimal[fsd] != 0)
            break;
    }
    ret = ALLOC(char, decimalDigits - fsd + 1);
    if (ret == NULL) {
        FREE(decimal);
        return(NULL);
    }
    for (i = fsd; i < decimalDigits; i++) {
        ret[i-fsd] = decimal[i] + '0';
    }
    ret[decimalDigits-fsd] = '\0';
    FREE(decimal);
    return(ret);

} /* end of Cudd_ApaStringDecimal */


/**
  @brief Prints an arbitrary precision integer in exponential format.

  @details Prints as an integer if precision is at least the number of
  digits to be printed.  If precision does not allow printing of all
  digits, rounds to nearest breaking ties so that the last printed
  digit is even.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see Cudd_ApaPrintHex Cudd_ApaPrintDecimal

*/
int
Cudd_ApaPrintExponential(
  FILE * fp,
  int digits,
  DdConstApaNumber number,
  int precision)
{
    int i, first, last, result;
    DdApaDigit remainder;
    DdApaNumber work;
    unsigned char *decimal, carry;
    /* We add an extra digit to have room for rounding up. */
    int decimalDigits = (int) (digits * log10((double) DD_APA_BASE)) + 2;

    /* Convert to decimal. */
    work = Cudd_NewApaNumber(digits);
    if (work == NULL)
	return(0);
    decimal = ALLOC(unsigned char, decimalDigits);
    if (decimal == NULL) {
	FREE(work);
	return(0);
    }
    Cudd_ApaCopy(digits,number,work);
    first = decimalDigits - 1;
    for (i = decimalDigits - 1; i >= 0; i--) {
	remainder = Cudd_ApaShortDivision(digits,work,(DdApaDigit) 10,work);
	decimal[i] = (unsigned char) remainder;
	if (remainder != 0) first = i; /* keep track of MS non-zero */
    }
    FREE(work);
    last = ddMin(first + precision, decimalDigits);

    /* See if we can print as integer. */
    if (decimalDigits - first <= precision) {
        for (i = first; i < last; i++) {
            result = fprintf(fp,"%1d", decimal[i]);
            if (result == EOF) {
                FREE(decimal);
                return(0);
            }
        }
        FREE(decimal);
        return(1);
    }

    /* If we get here we need to print an exponent.  Take care of rounding. */
    if (last == decimalDigits) {
        carry = 0;
    } else if (decimal[last] < 5) {
        carry = 0;
    } else if (decimal[last] == 5) {
        int nonZero = CUDD_FALSE;
        for (i = last + 1; i < decimalDigits; i++) {
            if (decimal[i] > 0) {
                nonZero = CUDD_TRUE;
                break;
            }
        }
        if (nonZero) {
            carry = 1;
        } else if (decimal[last - 1] & 1) { /* odd */
            carry = 1;
        } else {
            carry = 0;
        }
    } else {
        carry = 1;
    }

    /* Add carry. */
    for (i = last - 1; i >= 0; i--) {
        unsigned char tmp = decimal[i] + carry;
        if (tmp < 10) {
            decimal[i] = tmp;
            break;
        } else {
            decimal[i] = tmp - 10;
        }
    }

    /* Don't print trailing zeros. */
    while (last > first && decimal[last - 1] == 0)
        last--;

    /* Print. */
    for (i = first; i < last; i++) {
	result = fprintf(fp,"%s%1d",i == first+1 ? "." : "", decimal[i]);
	if (result == EOF) {
	    FREE(decimal);
	    return(0);
	}
    }
    FREE(decimal);
    result = fprintf(fp,"e+%02d",decimalDigits - first - 1);
    if (result == EOF) {
	return(0);
    }
    return(1);

} /* end of Cudd_ApaPrintExponential */


/**
  @brief Counts the number of minterms of a %DD.

  @details The function is assumed to depend on nvars variables. The
  minterm count is represented as an arbitrary precision unsigned
  integer, to allow for any number of variables CUDD supports.

  @return a pointer to the array representing the number of minterms
  of the function rooted at node if successful; NULL otherwise.

  @sideeffect The number of digits of the result is returned in
  parameter <code>digits</code>.

  @see Cudd_CountMinterm

*/
DdApaNumber
Cudd_ApaCountMinterm(
  DdManager const * manager,
  DdNode * node,
  int  nvars,
  int * digits)
{
    DdApaNumber	mmax, mmin;
    st_table	*table;
    DdApaNumber	i,count;

    *digits = Cudd_ApaNumberOfDigits(nvars+1);
    mmax = Cudd_NewApaNumber(*digits);
    if (mmax == NULL) {
	return(NULL);
    }
    Cudd_ApaPowerOfTwo(*digits,mmax,nvars);
    mmin = Cudd_NewApaNumber(*digits);
    if (mmin == NULL) {
	FREE(mmax);
	return(NULL);
    }
    Cudd_ApaSetToLiteral(*digits,mmin,0);
    table = st_init_table(st_ptrcmp,st_ptrhash);
    if (table == NULL) {
	FREE(mmax);
	FREE(mmin);
	return(NULL);
    }
    i = cuddApaCountMintermAux(manager, Cudd_Regular(node),*digits,mmax,mmin,table);
    if (i == NULL) {
	FREE(mmax);
	FREE(mmin);
	st_foreach(table, cuddApaStCountfree, NULL);
	st_free_table(table);
	return(NULL);
    }
    count = Cudd_NewApaNumber(*digits);
    if (count == NULL) {
	FREE(mmax);
	FREE(mmin);
	st_foreach(table, cuddApaStCountfree, NULL);
	st_free_table(table);
	if (Cudd_Regular(node)->ref == 1) FREE(i);
	return(NULL);
    }
    if (Cudd_IsComplement(node)) {
	(void) Cudd_ApaSubtract(*digits,mmax,i,count);
    } else {
	Cudd_ApaCopy(*digits,i,count);
    }
    FREE(mmax);
    FREE(mmin);
    st_foreach(table, cuddApaStCountfree, NULL);
    st_free_table(table);
    if (Cudd_Regular(node)->ref == 1) FREE(i);
    return(count);

} /* end of Cudd_ApaCountMinterm */


/**
  @brief Prints the number of minterms of a %BDD or %ADD using arbitrary
  precision arithmetic.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see Cudd_ApaPrintMintermExp

*/
int
Cudd_ApaPrintMinterm(
  FILE * fp,
  DdManager const * dd,
  DdNode * node,
  int  nvars)
{
    int digits;
    int result;
    DdApaNumber count;

    count = Cudd_ApaCountMinterm(dd,node,nvars,&digits);
    if (count == NULL)
	return(0);
    result = Cudd_ApaPrintDecimal(fp,digits,count);
    FREE(count);
    if (fprintf(fp,"\n") == EOF) {
	return(0);
    }
    return(result);

} /* end of Cudd_ApaPrintMinterm */


/**
  @brief Prints the number of minterms of a %BDD or %ADD in
  exponential format using arbitrary precision arithmetic.

  @details Parameter precision controls the number of signficant
  digits printed.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see Cudd_ApaPrintMinterm

*/
int
Cudd_ApaPrintMintermExp(
  FILE * fp,
  DdManager const * dd,
  DdNode * node,
  int nvars,
  int precision)
{
    int digits;
    int result;
    DdApaNumber count;

    count = Cudd_ApaCountMinterm(dd,node,nvars,&digits);
    if (count == NULL)
	return(0);
    result = Cudd_ApaPrintExponential(fp,digits,count,precision);
    FREE(count);
    if (fprintf(fp,"\n") == EOF) {
	return(0);
    }
    return(result);

} /* end of Cudd_ApaPrintMintermExp */


/**
  @brief Prints the density of a %BDD or %ADD using arbitrary
  precision arithmetic.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

*/
int
Cudd_ApaPrintDensity(
  FILE * fp,
  DdManager * dd,
  DdNode * node,
  int nvars)
{
    int digits;
    int result;
    DdApaNumber count,density;
    unsigned int size, remainder, fractional;

    count = Cudd_ApaCountMinterm(dd,node,nvars,&digits);
    if (count == NULL)
	return(0);
    size = (unsigned int) Cudd_DagSize(node);
    density = Cudd_NewApaNumber(digits);
    remainder = Cudd_ApaIntDivision(digits,count,size,density);
    result = Cudd_ApaPrintDecimal(fp,digits,density);
    FREE(count);
    FREE(density);
    fractional = (unsigned int)((double)remainder / size * 1000000);
    if (fprintf(fp,".%u\n", fractional) == EOF) {
	return(0);
    }
    return(result);

} /* end of Cudd_ApaPrintDensity */


/*---------------------------------------------------------------------------*/
/* Definition of internal functions                                          */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/


/**
  @brief Performs the recursive step of Cudd_ApaCountMinterm.

  @details It is based on the following identity. Let <code>|f|</code> be the
  number of minterms of <code>f</code>. Then:

      |f| = (|f0|+|f1|)/2

  where f0 and f1 are the two cofactors of f.
  Uses the identity <code>|f'| = mmax - |f|</code>.
  The procedure expects the argument "node" to be a regular pointer, and
  guarantees this condition is met in the recursive calls.
  For efficiency, the result of a call is cached only if the node has
  a reference count greater than 1.

  @return the number of minterms of the function rooted at node.

  @sideeffect None

*/
static DdApaNumber
cuddApaCountMintermAux(
  DdManager const * manager,
  DdNode * node,
  int digits,
  DdApaNumber mmax,
  DdApaNumber mmin,
  st_table * table)
{
    DdNode      *Nt, *Ne;
    DdApaNumber	mint, mint1, mint2;
    DdApaDigit	carryout;

    if (cuddIsConstant(node)) {
        int singleRef = Cudd_Regular(node)->ref == 1;
        if (node == manager->background || node == Cudd_Not(manager->one)) {
            if (singleRef) {
                mint = Cudd_NewApaNumber(digits);
                if (mint == NULL) {
                    return(NULL);
                }
                Cudd_ApaCopy(digits, mmin, mint);
                return(mint);
            } else {
                return(mmin);
            }
	} else {
            if (singleRef) {
                mint = Cudd_NewApaNumber(digits);
                if (mint == NULL) {
                    return(NULL);
                }
                Cudd_ApaCopy(digits, mmax, mint);
                return(mint);
            } else {
                return(mmax);
            }
	}
    }
    if (node->ref > 1 && st_lookup(table, node, (void **) &mint)) {
	return(mint);
    }

    Nt = cuddT(node); Ne = cuddE(node);

    mint1 = cuddApaCountMintermAux(manager, Nt,  digits, mmax, mmin, table);
    if (mint1 == NULL) return(NULL);
    mint2 = cuddApaCountMintermAux(manager, Cudd_Regular(Ne), digits, mmax, mmin, table);
    if (mint2 == NULL) {
	if (Nt->ref == 1) FREE(mint1);
	return(NULL);
    }
    mint = Cudd_NewApaNumber(digits);
    if (mint == NULL) {
	if (Nt->ref == 1) FREE(mint1);
	if (Cudd_Regular(Ne)->ref == 1) FREE(mint2);
	return(NULL);
    }
    if (Cudd_IsComplement(Ne)) {
	(void) Cudd_ApaSubtract(digits,mmax,mint2,mint);
	carryout = Cudd_ApaAdd(digits,mint1,mint,mint);
    } else {
	carryout = Cudd_ApaAdd(digits,mint1,mint2,mint);
    }
    Cudd_ApaShiftRight(digits,carryout,mint,mint);
    /* If the refernce count of a child is 1, its minterm count
    ** hasn't been stored in table.  Therefore, it must be explicitly
    ** freed here. */
    if (Nt->ref == 1) FREE(mint1);
    if (Cudd_Regular(Ne)->ref == 1) FREE(mint2);

    if (node->ref > 1) {
	if (st_insert(table, node, mint) == ST_OUT_OF_MEM) {
	    FREE(mint);
	    return(NULL);
	}
    }
    return(mint);

} /* end of cuddApaCountMintermAux */


/**
  @brief Frees the memory used to store the minterm counts recorded
  in the visited table.

  @return ST_CONTINUE.

  @sideeffect None

*/
static enum st_retval
cuddApaStCountfree(
  void * key,
  void * value,
  void * arg)
{
    DdApaNumber	d;

    (void) key; /* avoid warning */
    (void) arg; /* avoid warning */
    d = (DdApaNumber) value;
    FREE(d);
    return(ST_CONTINUE);

} /* end of cuddApaStCountfree */
