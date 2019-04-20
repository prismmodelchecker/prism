/**
  @file

  @ingroup cudd

  @brief Function to compute the scalar inverse of an %ADD.

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


/*---------------------------------------------------------------------------*/
/* Stucture declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Variable declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Macro declarations                                                        */
/*---------------------------------------------------------------------------*/

/** \cond */

/*---------------------------------------------------------------------------*/
/* Static function prototypes                                                */
/*---------------------------------------------------------------------------*/

/** \endcond */


/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Computes the scalar inverse of an %ADD.
  
  @details Computes an n %ADD where the discriminants are the
  multiplicative inverses of the corresponding discriminants of the
  argument %ADD.

  @return a pointer to the resulting %ADD in case of success. Returns
  NULL if any discriminants smaller than epsilon is encountered.

  @sideeffect None

*/
DdNode *
Cudd_addScalarInverse(
  DdManager * dd,
  DdNode * f,
  DdNode * epsilon)
{
    DdNode *res;

    if (!cuddIsConstant(epsilon)) {
	(void) fprintf(dd->err,"Invalid epsilon\n");
	return(NULL);
    }
    do {
	dd->reordered = 0;
	res  = cuddAddScalarInverseRecur(dd,f,epsilon);
    } while (dd->reordered == 1);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }
    return(res);

} /* end of Cudd_addScalarInverse */

/*---------------------------------------------------------------------------*/
/* Definition of internal functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Performs the recursive step of addScalarInverse.

  @return a pointer to the resulting %ADD in case of success. Returns
  NULL if any discriminants smaller than epsilon is encountered.

  @sideeffect None

*/
DdNode *
cuddAddScalarInverseRecur(
  DdManager * dd,
  DdNode * f,
  DdNode * epsilon)
{
    DdNode *t, *e, *res;
    CUDD_VALUE_TYPE value;

    statLine(dd);
    if (cuddIsConstant(f)) {
	if (ddAbs(cuddV(f)) < cuddV(epsilon)) return(NULL);
	value = 1.0 / cuddV(f);
	res = cuddUniqueConst(dd,value);
	return(res);
    }

    res = cuddCacheLookup2(dd,Cudd_addScalarInverse,f,epsilon);
    if (res != NULL) return(res);

    checkWhetherToGiveUp(dd);

    t = cuddAddScalarInverseRecur(dd,cuddT(f),epsilon);
    if (t == NULL) return(NULL);
    cuddRef(t);

    e = cuddAddScalarInverseRecur(dd,cuddE(f),epsilon);
    if (e == NULL) {
	Cudd_RecursiveDeref(dd, t);
	return(NULL);
    }
    cuddRef(e);

    res = (t == e) ? t : cuddUniqueInter(dd,(int)f->index,t,e);
    if (res == NULL) {
	Cudd_RecursiveDeref(dd, t);
	Cudd_RecursiveDeref(dd, e);
	return(NULL);
    }
    cuddDeref(t);
    cuddDeref(e);

    cuddCacheInsert2(dd,Cudd_addScalarInverse,f,epsilon,res);

    return(res);

} /* end of cuddAddScalarInverseRecur */


/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/

