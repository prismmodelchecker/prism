/**
  @file

  @ingroup cudd

  @brief Generalized cofactors for BDDs and ADDs.

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

/* Codes for edge markings in Cudd_bddLICompaction.  The codes are defined
** so that they can be bitwise ORed to implement the code priority scheme.
*/
#define DD_LIC_DC 0
#define DD_LIC_1  1
#define DD_LIC_0  2
#define DD_LIC_NL 3

/*---------------------------------------------------------------------------*/
/* Stucture declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/

/** Key for the cache used in the edge marking phase. */
typedef struct MarkCacheKey {
    DdNode *f;
    DdNode *c;
} MarkCacheKey;

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

static int cuddBddConstrainDecomp (DdManager *dd, DdNode *f, DdNode **decomp);
static DdNode * cuddBddCharToVect (DdManager *dd, DdNode *f, DdNode *x);
static int cuddBddLICMarkEdges (DdManager *dd, DdNode *f, DdNode *c, st_table *table, st_table *cache);
static DdNode * cuddBddLICBuildResult (DdManager *dd, DdNode *f, st_table *cache, st_table *table);
static int MarkCacheHash (void const *ptr, int modulus);
static int MarkCacheCompare (const void *ptr1, const void *ptr2);
static enum st_retval MarkCacheCleanUp (void *key, void *value, void *arg);
static DdNode * cuddBddSqueeze (DdManager *dd, DdNode *l, DdNode *u);
static DdNode * cuddBddInterpolate (DdManager * dd, DdNode * l, DdNode * u);

/** \endcond */

/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Computes f constrain c.

  @details Computes f constrain c (f @ c).
  Uses a canonical form: (f' @ c) = (f @ c)'.  (Note: this is not true
  for c.)  List of special cases:
    <ul>
    <li> f @ 0 = 0
    <li> f @ 1 = f
    <li> 0 @ c = 0
    <li> 1 @ c = 1
    <li> f @ f = 1
    <li> f @ f'= 0
    </ul>
  Note that if F=(f1,...,fn) and reordering takes place while computing F @ c,
  then the image restriction property (Img(F,c) = Img(F @ c)) is lost.

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddRestrict Cudd_addConstrain

*/
DdNode *
Cudd_bddConstrain(
  DdManager * dd,
  DdNode * f,
  DdNode * c)
{
    DdNode *res;

    do {
	dd->reordered = 0;
	res = cuddBddConstrainRecur(dd,f,c);
    } while (dd->reordered == 1);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }
    return(res);

} /* end of Cudd_bddConstrain */


/**
  @brief %BDD restrict according to Coudert and Madre's algorithm
  (ICCAD90).

  @details If application of restrict results in a %BDD larger than the
  input %BDD, the input %BDD is returned.

  @return the restricted %BDD if successful; otherwise NULL.

  @sideeffect None

  @see Cudd_bddConstrain Cudd_addRestrict

*/
DdNode *
Cudd_bddRestrict(
  DdManager * dd,
  DdNode * f,
  DdNode * c)
{
    DdNode *suppF, *suppC, *commonSupport;
    DdNode *cplus, *res;
    int retval;
    int sizeF, sizeRes;

    /* Check terminal cases here to avoid computing supports in trivial cases.
    ** This also allows us notto check later for the case c == 0, in which
    ** there is no common support. */
    if (c == Cudd_Not(DD_ONE(dd))) return(Cudd_Not(DD_ONE(dd)));
    if (Cudd_IsConstantInt(f)) return(f);
    if (f == c) return(DD_ONE(dd));
    if (f == Cudd_Not(c)) return(Cudd_Not(DD_ONE(dd)));

    /* Check if supports intersect. */
    retval = Cudd_ClassifySupport(dd,f,c,&commonSupport,&suppF,&suppC);
    if (retval == 0) {
	return(NULL);
    }
    cuddRef(commonSupport); cuddRef(suppF); cuddRef(suppC);
    Cudd_IterDerefBdd(dd,suppF);

    if (commonSupport == DD_ONE(dd)) {
	Cudd_IterDerefBdd(dd,commonSupport);
	Cudd_IterDerefBdd(dd,suppC);
	return(f);
    }
    Cudd_IterDerefBdd(dd,commonSupport);

    /* Abstract from c the variables that do not appear in f. */
    cplus = Cudd_bddExistAbstract(dd, c, suppC);
    if (cplus == NULL) {
	Cudd_IterDerefBdd(dd,suppC);
	return(NULL);
    }
    cuddRef(cplus);
    Cudd_IterDerefBdd(dd,suppC);

    do {
	dd->reordered = 0;
	res = cuddBddRestrictRecur(dd, f, cplus);
    } while (dd->reordered == 1);
    if (res == NULL) {
	Cudd_IterDerefBdd(dd,cplus);
        if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
          dd->timeoutHandler(dd, dd->tohArg);
        }
	return(NULL);
    }
    cuddRef(res);
    Cudd_IterDerefBdd(dd,cplus);
    /* Make restric safe by returning the smaller of the input and the
    ** result. */
    sizeF = Cudd_DagSize(f);
    sizeRes = Cudd_DagSize(res);
    if (sizeF <= sizeRes) {
	Cudd_IterDerefBdd(dd, res);
	return(f);
    } else {
	cuddDeref(res);
	return(res);
    }

} /* end of Cudd_bddRestrict */


/**
  @brief Computes f non-polluting-and g.

  @details The non-polluting AND of f and g is a hybrid of AND and
  Restrict.  From Restrict, this operation takes the idea of
  existentially quantifying the top variable of the second operand if
  it does not appear in the first.  Therefore, the variables that
  appear in the result also appear in f.  For the rest, the function
  behaves like AND.  Since the two operands play different roles,
  non-polluting AND is not commutative.

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddConstrain Cudd_bddRestrict

*/
DdNode *
Cudd_bddNPAnd(
  DdManager * dd,
  DdNode * f,
  DdNode * g)
{
    DdNode *res;

    do {
	dd->reordered = 0;
	res = cuddBddNPAndRecur(dd,f,g);
    } while (dd->reordered == 1);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }
    return(res);

} /* end of Cudd_bddNPAnd */


/**
  @brief Computes f constrain c for ADDs.

  @details Computes f constrain c (f @ c), for f an %ADD and c a 0-1
  %ADD.  List of special cases:
  <ul>
  <li> F @ 0 = 0
  <li> F @ 1 = F
  <li> 0 @ c = 0
  <li> 1 @ c = 1
  <li> F @ F = 1
  </ul>

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddConstrain

*/
DdNode *
Cudd_addConstrain(
  DdManager * dd,
  DdNode * f,
  DdNode * c)
{
    DdNode *res;

    do {
	dd->reordered = 0;
	res = cuddAddConstrainRecur(dd,f,c);
    } while (dd->reordered == 1);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }
    return(res);

} /* end of Cudd_addConstrain */


/**
  @brief %BDD conjunctive decomposition as in McMillan's CAV96 paper.

  @details The decomposition is canonical only for a given variable
  order. If canonicity is required, variable ordering must be disabled
  after the decomposition has been computed.  The components of the
  solution have their reference counts already incremented (unlike the
  results of most other functions in the package).

  @return an array with one entry for each %BDD variable in the manager
  if successful; otherwise NULL.

  @sideeffect None

  @see Cudd_bddConstrain Cudd_bddExistAbstract

*/
DdNode **
Cudd_bddConstrainDecomp(
  DdManager * dd,
  DdNode * f)
{
    DdNode **decomp;
    int res;
    int i;

    /* Create an initialize decomposition array. */
    decomp = ALLOC(DdNode *,dd->size);
    if (decomp == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return(NULL);
    }
    for (i = 0; i < dd->size; i++) {
	decomp[i] = NULL;
    }
    do {
	dd->reordered = 0;
	/* Clean up the decomposition array in case reordering took place. */
	for (i = 0; i < dd->size; i++) {
	    if (decomp[i] != NULL) {
		Cudd_IterDerefBdd(dd, decomp[i]);
		decomp[i] = NULL;
	    }
	}
	res = cuddBddConstrainDecomp(dd,f,decomp);
    } while (dd->reordered == 1);
    if (res == 0) {
	FREE(decomp);
        if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
          dd->timeoutHandler(dd, dd->tohArg);
        }
	return(NULL);
    }
    /* Missing components are constant ones. */
    for (i = 0; i < dd->size; i++) {
	if (decomp[i] == NULL) {
	    decomp[i] = DD_ONE(dd);
	    cuddRef(decomp[i]);
	}
    }
    return(decomp);

} /* end of Cudd_bddConstrainDecomp */


/**
  @brief %ADD restrict according to Coudert and Madre's algorithm
  (ICCAD90).

  @details If application of restrict results in an %ADD larger than
  the input %ADD, the input %ADD is returned.

  @return the restricted %ADD if successful; otherwise NULL.

  @sideeffect None

  @see Cudd_addConstrain Cudd_bddRestrict

*/
DdNode *
Cudd_addRestrict(
  DdManager * dd,
  DdNode * f,
  DdNode * c)
{
    DdNode *supp_f, *supp_c;
    DdNode *res, *commonSupport;
    int intersection;
    int sizeF, sizeRes;

    /* Check if supports intersect. */
    supp_f = Cudd_Support(dd, f);
    if (supp_f == NULL) {
	return(NULL);
    }
    cuddRef(supp_f);
    supp_c = Cudd_Support(dd, c);
    if (supp_c == NULL) {
	Cudd_RecursiveDeref(dd,supp_f);
	return(NULL);
    }
    cuddRef(supp_c);
    commonSupport = Cudd_bddLiteralSetIntersection(dd, supp_f, supp_c);
    if (commonSupport == NULL) {
	Cudd_RecursiveDeref(dd,supp_f);
	Cudd_RecursiveDeref(dd,supp_c);
	return(NULL);
    }
    cuddRef(commonSupport);
    Cudd_RecursiveDeref(dd,supp_f);
    Cudd_RecursiveDeref(dd,supp_c);
    intersection = commonSupport != DD_ONE(dd);
    Cudd_RecursiveDeref(dd,commonSupport);

    if (intersection) {
	do {
	    dd->reordered = 0;
	    res = cuddAddRestrictRecur(dd, f, c);
	} while (dd->reordered == 1);
        if (res == 0) {
            if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
                dd->timeoutHandler(dd, dd->tohArg);
            }
            return(f);
        }
	sizeF = Cudd_DagSize(f);
	sizeRes = Cudd_DagSize(res);
	if (sizeF <= sizeRes) {
	    cuddRef(res);
	    Cudd_RecursiveDeref(dd, res);
	    return(f);
	} else {
	    return(res);
	}
    } else {
	return(f);
    }

} /* end of Cudd_addRestrict */


/**
  @brief Computes a vector of BDDs whose image equals a non-zero function.

  @details 
  The result depends on the variable order. The i-th component of the vector
  depends only on the first i variables in the order.  Each %BDD in the vector
  is not larger than the %BDD of the given characteristic function.  This
  function is based on the description of char-to-vect in "Verification of
  Sequential Machines Using Boolean Functional Vectors" by O. Coudert, C.
  Berthet and J. C. Madre.

  @return a pointer to an array containing the result if successful;
  NULL otherwise.  The size of the array equals the number of
  variables in the manager. The components of the solution have their
  reference counts already incremented (unlike the results of most
  other functions in the package).

  @sideeffect None

  @see Cudd_bddConstrain

*/
DdNode **
Cudd_bddCharToVect(
  DdManager * dd,
  DdNode * f)
{
    int i, j;
    DdNode **vect;
    DdNode *res = NULL;

    if (f == Cudd_Not(DD_ONE(dd))) return(NULL);

    vect = ALLOC(DdNode *, dd->size);
    if (vect == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return(NULL);
    }

    do {
	dd->reordered = 0;
	for (i = 0; i < dd->size; i++) {
	    res = cuddBddCharToVect(dd,f,dd->vars[dd->invperm[i]]);
	    if (res == NULL) {
		/* Clean up the vector array in case reordering took place. */
		for (j = 0; j < i; j++) {
		    Cudd_IterDerefBdd(dd, vect[dd->invperm[j]]);
		}
		break;
	    }
	    cuddRef(res);
	    vect[dd->invperm[i]] = res;
	}
    } while (dd->reordered == 1);
    if (res == NULL) {
	FREE(vect);
        if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
            dd->timeoutHandler(dd, dd->tohArg);
        }
	return(NULL);
    }
    return(vect);

} /* end of Cudd_bddCharToVect */


/**
  @brief Performs safe minimization of a %BDD.

  @details Given the %BDD `f` of a function to be minimized and a %BDD
  `c` representing the care set, Cudd_bddLICompaction produces the
  %BDD of a function that agrees with `f` wherever `c` is 1.  Safe
  minimization means that the size of the result is guaranteed not to
  exceed the size of `f`. This function is based on the DAC97 paper by
  Hong et al..

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddRestrict

*/
DdNode *
Cudd_bddLICompaction(
  DdManager * dd /**< manager */,
  DdNode * f /**< function to be minimized */,
  DdNode * c /**< constraint (care set) */)
{
    DdNode *res;

    do {
	dd->reordered = 0;
	res = cuddBddLICompaction(dd,f,c);
    } while (dd->reordered == 1);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }
    return(res);

} /* end of Cudd_bddLICompaction */


/**
  @brief Finds a small %BDD in a function interval.

  @details Given BDDs `l` and `u`, representing the lower bound and
  upper bound of a function interval, Cudd_bddSqueeze produces the
  %BDD of a function within the interval with a small %BDD.

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddRestrict Cudd_bddLICompaction

*/
DdNode *
Cudd_bddSqueeze(
  DdManager * dd /**< manager */,
  DdNode * l /**< lower bound */,
  DdNode * u /**< upper bound */)
{
    DdNode *res;
    int sizeRes, sizeL, sizeU;

    do {
	dd->reordered = 0;
	res = cuddBddSqueeze(dd,l,u);
    } while (dd->reordered == 1);
    if (res == NULL) {
        if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
            dd->timeoutHandler(dd, dd->tohArg);
        }
        return(NULL);
    }
    /* We now compare the result with the bounds and return the smallest.
    ** We first compare to u, so that in case l == 0 and u == 1, we return
    ** 0 as in other minimization algorithms. */
    sizeRes = Cudd_DagSize(res);
    sizeU = Cudd_DagSize(u);
    if (sizeU <= sizeRes) {
	cuddRef(res);
	Cudd_IterDerefBdd(dd,res);
	res = u;
	sizeRes = sizeU;
    }
    sizeL = Cudd_DagSize(l);
    if (sizeL <= sizeRes) {
	cuddRef(res);
	Cudd_IterDerefBdd(dd,res);
	res = l;
    }
    return(res);

} /* end of Cudd_bddSqueeze */


/**
  @brief Finds an interpolant of two functions.

  @details Given BDDs `l` and `u`, representing the lower bound and
  upper bound of a function interval, Cudd_bddInterpolate produces the
  %BDD of a function within the interval that only depends on the
  variables common to `l` and `u`.

  The approach is based on quantification as in Cudd_bddRestrict().
  The function assumes that `l` implies `u`, but does not check whether
  that's true.

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddRestrict Cudd_bddSqueeze

*/
DdNode *
Cudd_bddInterpolate(
  DdManager * dd /**< manager */,
  DdNode * l /**< lower bound */,
  DdNode * u /**< upper bound */)
{
    DdNode *res;

    do {
	dd->reordered = 0;
	res = cuddBddInterpolate(dd,l,u);
    } while (dd->reordered == 1);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }
    return(res);

} /* end of Cudd_bddInterpolate */


/**
  @brief Finds a small %BDD that agrees with `f` over `c`.

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddRestrict Cudd_bddLICompaction Cudd_bddSqueeze

*/
DdNode *
Cudd_bddMinimize(
  DdManager * dd,
  DdNode * f,
  DdNode * c)
{
    DdNode *cplus, *res;

    if (c == Cudd_Not(DD_ONE(dd))) return(c);
    if (Cudd_IsConstantInt(f)) return(f);
    if (f == c) return(DD_ONE(dd));
    if (f == Cudd_Not(c)) return(Cudd_Not(DD_ONE(dd)));

    cplus = Cudd_RemapOverApprox(dd,c,0,0,1.0);
    if (cplus == NULL) return(NULL);
    cuddRef(cplus);
    res = Cudd_bddLICompaction(dd,f,cplus);
    if (res == NULL) {
	Cudd_IterDerefBdd(dd,cplus);
	return(NULL);
    }
    cuddRef(res);
    Cudd_IterDerefBdd(dd,cplus);
    cuddDeref(res);
    return(res);

} /* end of Cudd_bddMinimize */


/**
  @brief Find a dense subset of %BDD `f`.

  @details Density is the ratio of number of minterms to number of
  nodes.  Uses several techniques in series. It is more expensive than
  other subsetting procedures, but often produces better results. See
  Cudd_SubsetShortPaths for a description of the threshold and nvars
  parameters.

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_RemapUnderApprox Cudd_SubsetShortPaths
  Cudd_SubsetHeavyBranch Cudd_bddSqueeze

*/
DdNode *
Cudd_SubsetCompress(
  DdManager * dd /**< manager */,
  DdNode * f /**< %BDD whose subset is sought */,
  int  nvars /**< number of variables in the support of f */,
  int  threshold /**< maximum number of nodes in the subset */)
{
    DdNode *res, *tmp1, *tmp2;

    tmp1 = Cudd_SubsetShortPaths(dd, f, nvars, threshold, 0);
    if (tmp1 == NULL) return(NULL);
    cuddRef(tmp1);
    tmp2 = Cudd_RemapUnderApprox(dd,tmp1,nvars,0,0.95);
    if (tmp2 == NULL) {
	Cudd_IterDerefBdd(dd,tmp1);
	return(NULL);
    }
    cuddRef(tmp2);
    Cudd_IterDerefBdd(dd,tmp1);
    res = Cudd_bddSqueeze(dd,tmp2,f);
    if (res == NULL) {
	Cudd_IterDerefBdd(dd,tmp2);
	return(NULL);
    }
    cuddRef(res);
    Cudd_IterDerefBdd(dd,tmp2);
    cuddDeref(res);
    return(res);

} /* end of Cudd_SubsetCompress */


/**
  @brief Find a dense superset of %BDD `f`.

  @details Density is the ratio of number of minterms to number of
  nodes.  Uses several techniques in series. It is more expensive than
  other supersetting procedures, but often produces better
  results. See Cudd_SupersetShortPaths for a description of the
  threshold and nvars parameters.

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_SubsetCompress Cudd_SupersetRemap Cudd_SupersetShortPaths
  Cudd_SupersetHeavyBranch Cudd_bddSqueeze

*/
DdNode *
Cudd_SupersetCompress(
  DdManager * dd /**< manager */,
  DdNode * f /**< %BDD whose superset is sought */,
  int  nvars /**< number of variables in the support of f */,
  int  threshold /**< maximum number of nodes in the superset */)
{
    DdNode *subset;

    subset = Cudd_SubsetCompress(dd, Cudd_Not(f),nvars,threshold);

    return(Cudd_NotCond(subset, (subset != NULL)));

} /* end of Cudd_SupersetCompress */


/*---------------------------------------------------------------------------*/
/* Definition of internal functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Performs the recursive step of Cudd_bddConstrain.

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddConstrain

*/
DdNode *
cuddBddConstrainRecur(
  DdManager * dd,
  DdNode * f,
  DdNode * c)
{
    DdNode       *Fv, *Fnv, *Cv, *Cnv, *t, *e, *r;
    DdNode	 *one, *zero;
    int		 topf, topc;
    unsigned int index;
    int          comple = 0;

    statLine(dd);
    one = DD_ONE(dd);
    zero = Cudd_Not(one);

    /* Trivial cases. */
    if (c == one)		return(f);
    if (c == zero)		return(zero);
    if (Cudd_IsConstantInt(f))	return(f);
    if (f == c)			return(one);
    if (f == Cudd_Not(c))	return(zero);

    /* Make canonical to increase the utilization of the cache. */
    if (Cudd_IsComplement(f)) {
	f = Cudd_Not(f);
	comple = 1;
    }
    /* Now f is a regular pointer to a non-constant node; c is also
    ** non-constant, but may be complemented.
    */

    /* Check the cache. */
    r = cuddCacheLookup2(dd, Cudd_bddConstrain, f, c);
    if (r != NULL) {
	return(Cudd_NotCond(r,comple));
    }

    checkWhetherToGiveUp(dd);
    
    /* Recursive step. */
    topf = dd->perm[f->index];
    topc = dd->perm[Cudd_Regular(c)->index];
    if (topf <= topc) {
	index = f->index;
	Fv = cuddT(f); Fnv = cuddE(f);
    } else {
	index = Cudd_Regular(c)->index;
	Fv = Fnv = f;
    }
    if (topc <= topf) {
	Cv = cuddT(Cudd_Regular(c)); Cnv = cuddE(Cudd_Regular(c));
	if (Cudd_IsComplement(c)) {
	    Cv = Cudd_Not(Cv);
	    Cnv = Cudd_Not(Cnv);
	}
    } else {
	Cv = Cnv = c;
    }

    if (!Cudd_IsConstantInt(Cv)) {
	t = cuddBddConstrainRecur(dd, Fv, Cv);
	if (t == NULL)
	    return(NULL);
    } else if (Cv == one) {
	t = Fv;
    } else {		/* Cv == zero: return Fnv @ Cnv */
	if (Cnv == one) {
	    r = Fnv;
	} else {
	    r = cuddBddConstrainRecur(dd, Fnv, Cnv);
	    if (r == NULL)
		return(NULL);
	}
	return(Cudd_NotCond(r,comple));
    }
    cuddRef(t);

    if (!Cudd_IsConstantInt(Cnv)) {
	e = cuddBddConstrainRecur(dd, Fnv, Cnv);
	if (e == NULL) {
	    Cudd_IterDerefBdd(dd, t);
	    return(NULL);
	}
    } else if (Cnv == one) {
	e = Fnv;
    } else {		/* Cnv == zero: return Fv @ Cv previously computed */
	cuddDeref(t);
	return(Cudd_NotCond(t,comple));
    }
    cuddRef(e);

    if (Cudd_IsComplement(t)) {
	t = Cudd_Not(t);
	e = Cudd_Not(e);
	r = (t == e) ? t : cuddUniqueInter(dd, index, t, e);
	if (r == NULL) {
	    Cudd_IterDerefBdd(dd, e);
	    Cudd_IterDerefBdd(dd, t);
	    return(NULL);
	}
	r = Cudd_Not(r);
    } else {
	r = (t == e) ? t : cuddUniqueInter(dd, index, t, e);
	if (r == NULL) {
	    Cudd_IterDerefBdd(dd, e);
	    Cudd_IterDerefBdd(dd, t);
	    return(NULL);
	}
    }
    cuddDeref(t);
    cuddDeref(e);

    cuddCacheInsert2(dd, Cudd_bddConstrain, f, c, r);
    return(Cudd_NotCond(r,comple));

} /* end of cuddBddConstrainRecur */


/**
  @brief Performs the recursive step of Cudd_bddRestrict.

  @return the restricted %BDD if successful; otherwise NULL.

  @sideeffect None

  @see Cudd_bddRestrict

*/
DdNode *
cuddBddRestrictRecur(
  DdManager * dd,
  DdNode * f,
  DdNode * c)
{
    DdNode	 *Fv, *Fnv, *Cv, *Cnv, *t, *e, *r, *one, *zero;
    int		 topf, topc;
    unsigned int index;
    int		 comple = 0;

    statLine(dd);
    one = DD_ONE(dd);
    zero = Cudd_Not(one);

    /* Trivial cases */
    if (c == one)		return(f);
    if (c == zero)		return(zero);
    if (Cudd_IsConstantInt(f))	return(f);
    if (f == c)			return(one);
    if (f == Cudd_Not(c))	return(zero);

    /* Make canonical to increase the utilization of the cache. */
    if (Cudd_IsComplement(f)) {
	f = Cudd_Not(f);
	comple = 1;
    }
    /* Now f is a regular pointer to a non-constant node; c is also
    ** non-constant, but may be complemented.
    */

    /* Check the cache. */
    r = cuddCacheLookup2(dd, Cudd_bddRestrict, f, c);
    if (r != NULL) {
	return(Cudd_NotCond(r,comple));
    }

    checkWhetherToGiveUp(dd);

    topf = dd->perm[f->index];
    topc = dd->perm[Cudd_Regular(c)->index];

    if (topc < topf) {	/* abstract top variable from c */
	DdNode *d, *s1, *s2;

	/* Find complements of cofactors of c. */
	if (Cudd_IsComplement(c)) {
	    s1 = cuddT(Cudd_Regular(c));
	    s2 = cuddE(Cudd_Regular(c));
	} else {
	    s1 = Cudd_Not(cuddT(c));
	    s2 = Cudd_Not(cuddE(c));
	}
	/* Take the OR by applying DeMorgan. */
	d = cuddBddAndRecur(dd, s1, s2);
	if (d == NULL) return(NULL);
	d = Cudd_Not(d);
	cuddRef(d);
	r = cuddBddRestrictRecur(dd, f, d);
	if (r == NULL) {
	    Cudd_IterDerefBdd(dd, d);
	    return(NULL);
	}
	cuddRef(r);
	Cudd_IterDerefBdd(dd, d);
	cuddCacheInsert2(dd, Cudd_bddRestrict, f, c, r);
	cuddDeref(r);
	return(Cudd_NotCond(r,comple));
    }

    /* Recursive step. Here topf <= topc. */
    index = f->index;
    Fv = cuddT(f); Fnv = cuddE(f);
    if (topc == topf) {
	Cv = cuddT(Cudd_Regular(c)); Cnv = cuddE(Cudd_Regular(c));
	if (Cudd_IsComplement(c)) {
	    Cv = Cudd_Not(Cv);
	    Cnv = Cudd_Not(Cnv);
	}
    } else {
	Cv = Cnv = c;
    }

    if (!Cudd_IsConstantInt(Cv)) {
	t = cuddBddRestrictRecur(dd, Fv, Cv);
	if (t == NULL) return(NULL);
    } else if (Cv == one) {
	t = Fv;
    } else {		/* Cv == zero: return(Fnv @ Cnv) */
	if (Cnv == one) {
	    r = Fnv;
	} else {
	    r = cuddBddRestrictRecur(dd, Fnv, Cnv);
	    if (r == NULL) return(NULL);
	}
	return(Cudd_NotCond(r,comple));
    }
    cuddRef(t);

    if (!Cudd_IsConstantInt(Cnv)) {
	e = cuddBddRestrictRecur(dd, Fnv, Cnv);
	if (e == NULL) {
	    Cudd_IterDerefBdd(dd, t);
	    return(NULL);
	}
    } else if (Cnv == one) {
	e = Fnv;
    } else {		/* Cnv == zero: return (Fv @ Cv) previously computed */
	cuddDeref(t);
	return(Cudd_NotCond(t,comple));
    }
    cuddRef(e);

    if (Cudd_IsComplement(t)) {
	t = Cudd_Not(t);
	e = Cudd_Not(e);
	r = (t == e) ? t : cuddUniqueInter(dd, index, t, e);
	if (r == NULL) {
	    Cudd_IterDerefBdd(dd, e);
	    Cudd_IterDerefBdd(dd, t);
	    return(NULL);
	}
	r = Cudd_Not(r);
    } else {
	r = (t == e) ? t : cuddUniqueInter(dd, index, t, e);
	if (r == NULL) {
	    Cudd_IterDerefBdd(dd, e);
	    Cudd_IterDerefBdd(dd, t);
	    return(NULL);
	}
    }
    cuddDeref(t);
    cuddDeref(e);

    cuddCacheInsert2(dd, Cudd_bddRestrict, f, c, r);
    return(Cudd_NotCond(r,comple));

} /* end of cuddBddRestrictRecur */


/**
  @brief Implements the recursive step of Cudd_bddAnd.

  @return a pointer to the result is successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddNPAnd

*/
DdNode *
cuddBddNPAndRecur(
  DdManager * manager,
  DdNode * f,
  DdNode * g)
{
    DdNode *F, *ft, *fe, *G, *gt, *ge;
    DdNode *one, *r, *t, *e;
    int topf, topg;
    unsigned int index;

    statLine(manager);
    one = DD_ONE(manager);

    /* Terminal cases. */
    F = Cudd_Regular(f);
    G = Cudd_Regular(g);
    if (F == G) {
	if (f == g) return(one);
	else return(Cudd_Not(one));
    }
    if (G == one) {
	if (g == one) return(f);
	else return(g);
    }
    if (F == one) {
	return(f);
    }

    /* At this point f and g are not constant. */
    /* Check cache. */
    if (F->ref != 1 || G->ref != 1) {
	r = cuddCacheLookup2(manager, Cudd_bddNPAnd, f, g);
	if (r != NULL) return(r);
    }

    checkWhetherToGiveUp(manager);

    /* Here we can skip the use of cuddI, because the operands are known
    ** to be non-constant.
    */
    topf = manager->perm[F->index];
    topg = manager->perm[G->index];

    if (topg < topf) {	/* abstract top variable from g */
	DdNode *d;

	/* Find complements of cofactors of g. */
	if (Cudd_IsComplement(g)) {
	    gt = cuddT(G);
	    ge = cuddE(G);
	} else {
	    gt = Cudd_Not(cuddT(g));
	    ge = Cudd_Not(cuddE(g));
	}
	/* Take the OR by applying DeMorgan. */
	d = cuddBddAndRecur(manager, gt, ge);
	if (d == NULL) return(NULL);
	d = Cudd_Not(d);
	cuddRef(d);
	r = cuddBddNPAndRecur(manager, f, d);
	if (r == NULL) {
	    Cudd_IterDerefBdd(manager, d);
	    return(NULL);
	}
	cuddRef(r);
	Cudd_IterDerefBdd(manager, d);
	cuddCacheInsert2(manager, Cudd_bddNPAnd, f, g, r);
	cuddDeref(r);
	return(r);
    }

    /* Compute cofactors. */
    index = F->index;
    ft = cuddT(F);
    fe = cuddE(F);
    if (Cudd_IsComplement(f)) {
      ft = Cudd_Not(ft);
      fe = Cudd_Not(fe);
    }

    if (topg == topf) {
	gt = cuddT(G);
	ge = cuddE(G);
	if (Cudd_IsComplement(g)) {
	    gt = Cudd_Not(gt);
	    ge = Cudd_Not(ge);
	}
    } else {
	gt = ge = g;
    }

    t = cuddBddAndRecur(manager, ft, gt);
    if (t == NULL) return(NULL);
    cuddRef(t);

    e = cuddBddAndRecur(manager, fe, ge);
    if (e == NULL) {
	Cudd_IterDerefBdd(manager, t);
	return(NULL);
    }
    cuddRef(e);

    if (t == e) {
	r = t;
    } else {
	if (Cudd_IsComplement(t)) {
	    r = cuddUniqueInter(manager,(int)index,Cudd_Not(t),Cudd_Not(e));
	    if (r == NULL) {
		Cudd_IterDerefBdd(manager, t);
		Cudd_IterDerefBdd(manager, e);
		return(NULL);
	    }
	    r = Cudd_Not(r);
	} else {
	    r = cuddUniqueInter(manager,(int)index,t,e);
	    if (r == NULL) {
		Cudd_IterDerefBdd(manager, t);
		Cudd_IterDerefBdd(manager, e);
		return(NULL);
	    }
	}
    }
    cuddDeref(e);
    cuddDeref(t);
    if (F->ref != 1 || G->ref != 1)
	cuddCacheInsert2(manager, Cudd_bddNPAnd, f, g, r);
    return(r);

} /* end of cuddBddNPAndRecur */


/**
  @brief Performs the recursive step of Cudd_addConstrain.

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_addConstrain

*/
DdNode *
cuddAddConstrainRecur(
  DdManager * dd,
  DdNode * f,
  DdNode * c)
{
    DdNode       *Fv, *Fnv, *Cv, *Cnv, *t, *e, *r;
    DdNode	 *one, *zero;
    int		 topf, topc;
    unsigned int index;

    statLine(dd);
    one = DD_ONE(dd);
    zero = DD_ZERO(dd);

    /* Trivial cases. */
    if (c == one)		return(f);
    if (c == zero)		return(zero);
    if (cuddIsConstant(f))	return(f);
    if (f == c)			return(one);

    /* Now f and c are non-constant. */

    /* Check the cache. */
    r = cuddCacheLookup2(dd, Cudd_addConstrain, f, c);
    if (r != NULL) {
	return(r);
    }

    checkWhetherToGiveUp(dd);

    /* Recursive step. */
    topf = dd->perm[f->index];
    topc = dd->perm[c->index];
    if (topf <= topc) {
	index = f->index;
	Fv = cuddT(f); Fnv = cuddE(f);
    } else {
	index = c->index;
	Fv = Fnv = f;
    }
    if (topc <= topf) {
	Cv = cuddT(c); Cnv = cuddE(c);
    } else {
	Cv = Cnv = c;
    }

    if (!cuddIsConstant(Cv)) {
	t = cuddAddConstrainRecur(dd, Fv, Cv);
	if (t == NULL)
	    return(NULL);
    } else if (Cv == one) {
	t = Fv;
    } else {		/* Cv == zero: return Fnv @ Cnv */
	if (Cnv == one) {
	    r = Fnv;
	} else {
	    r = cuddAddConstrainRecur(dd, Fnv, Cnv);
	    if (r == NULL)
		return(NULL);
	}
	return(r);
    }
    cuddRef(t);

    if (!cuddIsConstant(Cnv)) {
	e = cuddAddConstrainRecur(dd, Fnv, Cnv);
	if (e == NULL) {
	    Cudd_RecursiveDeref(dd, t);
	    return(NULL);
	}
    } else if (Cnv == one) {
	e = Fnv;
    } else {		/* Cnv == zero: return Fv @ Cv previously computed */
	cuddDeref(t);
	return(t);
    }
    cuddRef(e);

    r = (t == e) ? t : cuddUniqueInter(dd, index, t, e);
    if (r == NULL) {
	Cudd_RecursiveDeref(dd, e);
	Cudd_RecursiveDeref(dd, t);
	return(NULL);
    }
    cuddDeref(t);
    cuddDeref(e);

    cuddCacheInsert2(dd, Cudd_addConstrain, f, c, r);
    return(r);

} /* end of cuddAddConstrainRecur */


/**
  @brief Performs the recursive step of Cudd_addRestrict.

  @return the restricted %ADD if successful; otherwise NULL.

  @sideeffect None

  @see Cudd_addRestrict

*/
DdNode *
cuddAddRestrictRecur(
  DdManager * dd,
  DdNode * f,
  DdNode * c)
{
    DdNode	 *Fv, *Fnv, *Cv, *Cnv, *t, *e, *r, *one, *zero;
    int		 topf, topc;
    unsigned int index;

    statLine(dd);
    one = DD_ONE(dd);
    zero = DD_ZERO(dd);

    /* Trivial cases */
    if (c == one)		return(f);
    if (c == zero)		return(zero);
    if (cuddIsConstant(f))	return(f);
    if (f == c)			return(one);

    /* Now f and c are non-constant. */

    /* Check the cache. */
    r = cuddCacheLookup2(dd, Cudd_addRestrict, f, c);
    if (r != NULL) {
	return(r);
    }

    checkWhetherToGiveUp(dd);

    topf = dd->perm[f->index];
    topc = dd->perm[c->index];

    if (topc < topf) {	/* abstract top variable from c */
	DdNode *d, *s1, *s2;

	/* Find cofactors of c. */
	s1 = cuddT(c);
	s2 = cuddE(c);
	/* Take the OR by applying DeMorgan. */
	d = cuddAddApplyRecur(dd, Cudd_addOr, s1, s2);
	if (d == NULL) return(NULL);
	cuddRef(d);
	r = cuddAddRestrictRecur(dd, f, d);
	if (r == NULL) {
	    Cudd_RecursiveDeref(dd, d);
	    return(NULL);
	}
	cuddRef(r);
	Cudd_RecursiveDeref(dd, d);
	cuddCacheInsert2(dd, Cudd_addRestrict, f, c, r);
	cuddDeref(r);
	return(r);
    }

    /* Recursive step. Here topf <= topc. */
    index = f->index;
    Fv = cuddT(f); Fnv = cuddE(f);
    if (topc == topf) {
	Cv = cuddT(c); Cnv = cuddE(c);
    } else {
	Cv = Cnv = c;
    }

    if (!Cudd_IsConstantInt(Cv)) {
	t = cuddAddRestrictRecur(dd, Fv, Cv);
	if (t == NULL) return(NULL);
    } else if (Cv == one) {
	t = Fv;
    } else {		/* Cv == zero: return(Fnv @ Cnv) */
	if (Cnv == one) {
	    r = Fnv;
	} else {
	    r = cuddAddRestrictRecur(dd, Fnv, Cnv);
	    if (r == NULL) return(NULL);
	}
	return(r);
    }
    cuddRef(t);

    if (!cuddIsConstant(Cnv)) {
	e = cuddAddRestrictRecur(dd, Fnv, Cnv);
	if (e == NULL) {
	    Cudd_RecursiveDeref(dd, t);
	    return(NULL);
	}
    } else if (Cnv == one) {
	e = Fnv;
    } else {		/* Cnv == zero: return (Fv @ Cv) previously computed */
	cuddDeref(t);
	return(t);
    }
    cuddRef(e);

    r = (t == e) ? t : cuddUniqueInter(dd, index, t, e);
    if (r == NULL) {
	Cudd_RecursiveDeref(dd, e);
	Cudd_RecursiveDeref(dd, t);
	return(NULL);
    }
    cuddDeref(t);
    cuddDeref(e);

    cuddCacheInsert2(dd, Cudd_addRestrict, f, c, r);
    return(r);

} /* end of cuddAddRestrictRecur */



/**
  @brief Performs safe minimization of a %BDD.

  @details Given the %BDD `f` of a function to be minimized and a %BDD
  `c` representing the care set, Cudd_bddLICompaction produces the
  %BDD of a function that agrees with `f` wherever `c` is 1.  Safe
  minimization means that the size of the result is guaranteed not to
  exceed the size of `f`. This function is based on the DAC97 paper by
  Hong et al..

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddLICompaction

*/
DdNode *
cuddBddLICompaction(
  DdManager * dd /**< manager */,
  DdNode * f /**< function to be minimized */,
  DdNode * c /**< constraint (care set) */)
{
    st_table *marktable, *markcache, *buildcache;
    DdNode *res, *zero;

    zero = Cudd_Not(DD_ONE(dd));
    if (c == zero) return(zero);

    /* We need to use local caches for both steps of this operation.
    ** The results of the edge marking step are only valid as long as the
    ** edge markings themselves are available. However, the edge markings
    ** are lost at the end of one invocation of Cudd_bddLICompaction.
    ** Hence, the cache entries for the edge marking step must be
    ** invalidated at the end of this function.
    ** For the result of the building step we argue as follows. The result
    ** for a node and a given constrain depends on the BDD in which the node
    ** appears. Hence, the same node and constrain may give different results
    ** in successive invocations.
    */
    marktable = st_init_table(st_ptrcmp,st_ptrhash);
    if (marktable == NULL) {
	return(NULL);
    }
    markcache = st_init_table(MarkCacheCompare,MarkCacheHash);
    if (markcache == NULL) {
	st_free_table(marktable);
	return(NULL);
    }
    if (cuddBddLICMarkEdges(dd,f,c,marktable,markcache) == CUDD_OUT_OF_MEM) {
	st_foreach(markcache, MarkCacheCleanUp, NULL);
	st_free_table(marktable);
	st_free_table(markcache);
	return(NULL);
    }
    st_foreach(markcache, MarkCacheCleanUp, NULL);
    st_free_table(markcache);
    buildcache = st_init_table(st_ptrcmp,st_ptrhash);
    if (buildcache == NULL) {
	st_free_table(marktable);
	return(NULL);
    }
    res = cuddBddLICBuildResult(dd,f,buildcache,marktable);
    st_free_table(buildcache);
    st_free_table(marktable);
    return(res);

} /* end of cuddBddLICompaction */


/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/


/**
  @brief Performs the recursive step of Cudd_bddConstrainDecomp.

  @return f super (i) if successful; otherwise NULL.

  @sideeffect None

  @see Cudd_bddConstrainDecomp

*/
static int
cuddBddConstrainDecomp(
  DdManager * dd,
  DdNode * f,
  DdNode ** decomp)
{
    DdNode *F, *fv, *fvn;
    DdNode *fAbs;
    DdNode *result;
    int ok;

    if (Cudd_IsConstantInt(f)) return(1);
    /* Compute complements of cofactors. */
    F = Cudd_Regular(f);
    fv = cuddT(F);
    fvn = cuddE(F);
    if (F == f) {
	fv = Cudd_Not(fv);
	fvn = Cudd_Not(fvn);
    }
    /* Compute abstraction of top variable. */
    fAbs = cuddBddAndRecur(dd, fv, fvn);
    if (fAbs == NULL) {
	return(0);
    }
    cuddRef(fAbs);
    fAbs = Cudd_Not(fAbs);
    /* Recursively find the next abstraction and the components of the
    ** decomposition. */
    ok = cuddBddConstrainDecomp(dd, fAbs, decomp);
    if (ok == 0) {
	Cudd_IterDerefBdd(dd,fAbs);
	return(0);
    }
    /* Compute the component of the decomposition corresponding to the
    ** top variable and store it in the decomposition array. */
    result = cuddBddConstrainRecur(dd, f, fAbs);
    if (result == NULL) {
	Cudd_IterDerefBdd(dd,fAbs);
	return(0);
    }
    cuddRef(result);
    decomp[F->index] = result;
    Cudd_IterDerefBdd(dd, fAbs);
    return(1);

} /* end of cuddBddConstrainDecomp */


/**
  @brief Performs the recursive step of Cudd_bddCharToVect.

  @details This function maintains the invariant that f is non-zero.

  @return the i-th component of the vector if successful; otherwise NULL.

  @sideeffect None

  @see Cudd_bddCharToVect

*/
static DdNode *
cuddBddCharToVect(
  DdManager * dd,
  DdNode * f,
  DdNode * x)
{
    int topf;
    int level;
    int comple;

    DdNode *one, *zero, *res, *F, *fT, *fE, *T, *E;

    statLine(dd);
    /* Check the cache. */
    res = cuddCacheLookup2(dd, cuddBddCharToVect, f, x);
    if (res != NULL) {
	return(res);
    }

    checkWhetherToGiveUp(dd);

    F = Cudd_Regular(f);

    topf = cuddI(dd,F->index);
    level = dd->perm[x->index];

    if (topf > level) return(x);

    one = DD_ONE(dd);
    zero = Cudd_Not(one);

    comple = F != f;
    fT = Cudd_NotCond(cuddT(F),comple);
    fE = Cudd_NotCond(cuddE(F),comple);

    if (topf == level) {
	if (fT == zero) return(zero);
	if (fE == zero) return(one);
	return(x);
    }

    /* Here topf < level. */
    if (fT == zero) return(cuddBddCharToVect(dd, fE, x));
    if (fE == zero) return(cuddBddCharToVect(dd, fT, x));

    T = cuddBddCharToVect(dd, fT, x);
    if (T == NULL) {
	return(NULL);
    }
    cuddRef(T);
    E = cuddBddCharToVect(dd, fE, x);
    if (E == NULL) {
	Cudd_IterDerefBdd(dd,T);
	return(NULL);
    }
    cuddRef(E);
    res = cuddBddIteRecur(dd, dd->vars[F->index], T, E);
    if (res == NULL) {
	Cudd_IterDerefBdd(dd,T);
	Cudd_IterDerefBdd(dd,E);
	return(NULL);
    }
    cuddDeref(T);
    cuddDeref(E);
    cuddCacheInsert2(dd, cuddBddCharToVect, f, x, res);
    return(res);

} /* end of cuddBddCharToVect */


/**
  @brief Performs the edge marking step of Cudd_bddLICompaction.

  @return the LUB of the markings of the two outgoing edges of
  <code>f</code> if successful; otherwise CUDD_OUT_OF_MEM.

  @sideeffect None

  @see Cudd_bddLICompaction cuddBddLICBuildResult

*/
static int
cuddBddLICMarkEdges(
  DdManager * dd,
  DdNode * f,
  DdNode * c,
  st_table * table,
  st_table * cache)
{
    DdNode *Fv, *Fnv, *Cv, *Cnv;
    DdNode *one, *zero;
    int topf, topc;
    int comple;
    int resT, resE, res, retval;
    void **slot;
    MarkCacheKey *key;

    one = DD_ONE(dd);
    zero = Cudd_Not(one);

    /* Terminal cases. */
    if (c == zero) return(DD_LIC_DC);
    if (f == one)  return(DD_LIC_1);
    if (f == zero) return(DD_LIC_0);

    /* Make canonical to increase the utilization of the cache. */
    comple = Cudd_IsComplement(f);
    f = Cudd_Regular(f);
    /* Now f is a regular pointer to a non-constant node; c may be
    ** constant, or it may be complemented.
    */

    /* Check the cache. */
    key = ALLOC(MarkCacheKey, 1);
    if (key == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return(CUDD_OUT_OF_MEM);
    }
    key->f = f; key->c = c;
    if (st_lookup_int(cache, key, &res)) {
	FREE(key);
	if (comple) {
	    if (res == DD_LIC_0) res = DD_LIC_1;
	    else if (res == DD_LIC_1) res = DD_LIC_0;
	}
	return(res);
    }

    /* Recursive step. */
    topf = dd->perm[f->index];
    topc = cuddI(dd,Cudd_Regular(c)->index);
    if (topf <= topc) {
	Fv = cuddT(f); Fnv = cuddE(f);
    } else {
	Fv = Fnv = f;
    }
    if (topc <= topf) {
	/* We know that c is not constant because f is not. */
	Cv = cuddT(Cudd_Regular(c)); Cnv = cuddE(Cudd_Regular(c));
	if (Cudd_IsComplement(c)) {
	    Cv = Cudd_Not(Cv);
	    Cnv = Cudd_Not(Cnv);
	}
    } else {
	Cv = Cnv = c;
    }

    resT = cuddBddLICMarkEdges(dd, Fv, Cv, table, cache);
    if (resT == CUDD_OUT_OF_MEM) {
	FREE(key);
	return(CUDD_OUT_OF_MEM);
    }
    resE = cuddBddLICMarkEdges(dd, Fnv, Cnv, table, cache);
    if (resE == CUDD_OUT_OF_MEM) {
	FREE(key);
	return(CUDD_OUT_OF_MEM);
    }

    /* Update edge markings. */
    if (topf <= topc) {
	retval = st_find_or_add(table, f, &slot);
	if (retval == 0) {
	    *slot = (void **) (ptrint)((resT << 2) | resE);
	} else if (retval == 1) {
	    *slot = (void **) (ptrint)((int)((ptrint) *slot) | (resT << 2) | resE);
	} else {
	    FREE(key);
	    return(CUDD_OUT_OF_MEM);
	}
    }

    /* Cache result. */
    res = resT | resE;
    if (st_insert(cache, key, (void *)(ptrint)res) == ST_OUT_OF_MEM) {
	FREE(key);
	return(CUDD_OUT_OF_MEM);
    }

    /* Take into account possible complementation. */
    if (comple) {
	if (res == DD_LIC_0) res = DD_LIC_1;
	else if (res == DD_LIC_1) res = DD_LIC_0;
    }
    return(res);

} /* end of cuddBddLICMarkEdges */


/**
  @brief Builds the result of Cudd_bddLICompaction.

  @return a pointer to the minimized %BDD if successful; otherwise NULL.

  @sideeffect None

  @see Cudd_bddLICompaction cuddBddLICMarkEdges

*/
static DdNode *
cuddBddLICBuildResult(
  DdManager * dd,
  DdNode * f,
  st_table * cache,
  st_table * table)
{
    DdNode *Fv, *Fnv, *r, *t, *e;
    DdNode *one, *zero;
    unsigned int index;
    int comple;
    int markT, markE, markings;

    one = DD_ONE(dd);
    zero = Cudd_Not(one);

    if (Cudd_IsConstantInt(f)) return(f);
    /* Make canonical to increase the utilization of the cache. */
    comple = Cudd_IsComplement(f);
    f = Cudd_Regular(f);

    /* Check the cache. */
    if (st_lookup(cache, f, (void **) &r)) {
	return(Cudd_NotCond(r,comple));
    }

    /* Retrieve the edge markings. */
    if (st_lookup_int(table, f, &markings) == 0)
	return(NULL);
    markT = markings >> 2;
    markE = markings & 3;

    index = f->index;
    Fv = cuddT(f); Fnv = cuddE(f);

    if (markT == DD_LIC_NL) {
	t = cuddBddLICBuildResult(dd,Fv,cache,table);
	if (t == NULL) {
	    return(NULL);
	}
    } else if (markT == DD_LIC_1) {
	t = one;
    } else {
	t = zero;
    }
    cuddRef(t);
    if (markE == DD_LIC_NL) {
	e = cuddBddLICBuildResult(dd,Fnv,cache,table);
	if (e == NULL) {
	    Cudd_IterDerefBdd(dd,t);
	    return(NULL);
	}
    } else if (markE == DD_LIC_1) {
	e = one;
    } else {
	e = zero;
    }
    cuddRef(e);

    if (markT == DD_LIC_DC && markE != DD_LIC_DC) {
	r = e;
    } else if (markT != DD_LIC_DC && markE == DD_LIC_DC) {
	r = t;
    } else {
	if (Cudd_IsComplement(t)) {
	    t = Cudd_Not(t);
	    e = Cudd_Not(e);
	    r = (t == e) ? t : cuddUniqueInter(dd, index, t, e);
	    if (r == NULL) {
		Cudd_IterDerefBdd(dd, e);
		Cudd_IterDerefBdd(dd, t);
		return(NULL);
	    }
	    r = Cudd_Not(r);
	} else {
	    r = (t == e) ? t : cuddUniqueInter(dd, index, t, e);
	    if (r == NULL) {
		Cudd_IterDerefBdd(dd, e);
		Cudd_IterDerefBdd(dd, t);
		return(NULL);
	    }
	}
    }
    cuddDeref(t);
    cuddDeref(e);

    if (st_insert(cache, f, r) == ST_OUT_OF_MEM) {
	cuddRef(r);
	Cudd_IterDerefBdd(dd,r);
	return(NULL);
    }

    return(Cudd_NotCond(r,comple));

} /* end of cuddBddLICBuildResult */


/**
  @brief Hash function for the computed table of cuddBddLICMarkEdges.

  @return the bucket number.

  @sideeffect None

  @see Cudd_bddLICompaction

*/
static int
MarkCacheHash(
  void const * ptr,
  int  modulus)
{
    int val = 0;
    MarkCacheKey const *entry = (MarkCacheKey const *) ptr;

    val = (int) (ptrint) entry->f;
    val = val * 997 + (int) (ptrint) entry->c;

    return ((val < 0) ? -val : val) % modulus;

} /* end of MarkCacheHash */


/**
  @brief Comparison function for the computed table of
  cuddBddLICMarkEdges.

  @return 0 if the two nodes of the key are equal; 1 otherwise.

  @sideeffect None

  @see Cudd_bddLICompaction

*/
static int
MarkCacheCompare(
  const void * ptr1,
  const void * ptr2)
{
    MarkCacheKey const *entry1 = (MarkCacheKey const *) ptr1;
    MarkCacheKey const *entry2 = (MarkCacheKey const *) ptr2;
    
    return((entry1->f != entry2->f) || (entry1->c != entry2->c));

} /* end of MarkCacheCompare */


/**
  @brief Frees memory associated with computed table of
  cuddBddLICMarkEdges.

  @return ST_CONTINUE.

  @sideeffect None

  @see Cudd_bddLICompaction

*/
static enum st_retval
MarkCacheCleanUp(
  void * key,
  void * value,
  void * arg)
{
    MarkCacheKey *entry = (MarkCacheKey *) key;

    (void) value; /* avoid warning */
    (void) arg;   /* avoid warning */
    FREE(entry);
    return ST_CONTINUE;

} /* end of MarkCacheCleanUp */


/**
  @brief Performs the recursive step of Cudd_bddSqueeze.

  @details This procedure exploits the fact that if we complement and
  swap the bounds of the interval we obtain a valid solution by taking
  the complement of the solution to the original problem. Therefore,
  we can enforce the condition that the upper bound is always regular.

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddSqueeze

*/
static DdNode *
cuddBddSqueeze(
  DdManager * dd,
  DdNode * l,
  DdNode * u)
{
    DdNode *one, *zero, *r, *lt, *le, *ut, *ue, *t, *e;
#if 0
    DdNode *ar;
#endif
    int comple = 0;
    int topu, topl;
    unsigned int index;

    statLine(dd);
    if (l == u) {
	return(l);
    }
    one = DD_ONE(dd);
    zero = Cudd_Not(one);
    /* The only case when l == zero && u == one is at the top level,
    ** where returning either one or zero is OK. In all other cases
    ** the procedure will detect such a case and will perform
    ** remapping. Therefore the order in which we test l and u at this
    ** point is immaterial. */
    if (l == zero) return(l);
    if (u == one)  return(u);

    /* Make canonical to increase the utilization of the cache. */
    if (Cudd_IsComplement(u)) {
	DdNode *temp;
	temp = Cudd_Not(l);
	l = Cudd_Not(u);
	u = temp;
	comple = 1;
    }
    /* At this point u is regular and non-constant; l is non-constant, but
    ** may be complemented. */

    /* Here we could check the relative sizes. */

    /* Check the cache. */
    r = cuddCacheLookup2(dd, Cudd_bddSqueeze, l, u);
    if (r != NULL) {
	return(Cudd_NotCond(r,comple));
    }

    checkWhetherToGiveUp(dd);

    /* Recursive step. */
    topu = dd->perm[u->index];
    topl = dd->perm[Cudd_Regular(l)->index];
    if (topu <= topl) {
	index = u->index;
	ut = cuddT(u); ue = cuddE(u);
    } else {
	index = Cudd_Regular(l)->index;
	ut = ue = u;
    }
    if (topl <= topu) {
	lt = cuddT(Cudd_Regular(l)); le = cuddE(Cudd_Regular(l));
	if (Cudd_IsComplement(l)) {
	    lt = Cudd_Not(lt);
	    le = Cudd_Not(le);
	}
    } else {
	lt = le = l;
    }

    /* If one interval is contained in the other, use the smaller
    ** interval. This corresponds to one-sided matching. */
    if ((lt == zero || Cudd_bddLeq(dd,lt,le)) &&
	(ut == one  || Cudd_bddLeq(dd,ue,ut))) { /* remap */
	r = cuddBddSqueeze(dd, le, ue);
	if (r == NULL)
	    return(NULL);
	return(Cudd_NotCond(r,comple));
    } else if ((le == zero || Cudd_bddLeq(dd,le,lt)) &&
	       (ue == one  || Cudd_bddLeq(dd,ut,ue))) { /* remap */
	r = cuddBddSqueeze(dd, lt, ut);
	if (r == NULL)
	    return(NULL);
	return(Cudd_NotCond(r,comple));
    } else if ((le == zero || Cudd_bddLeq(dd,le,Cudd_Not(ut))) &&
	       (ue == one  || Cudd_bddLeq(dd,Cudd_Not(lt),ue))) { /* c-remap */
	t = cuddBddSqueeze(dd, lt, ut);
	cuddRef(t);
	if (Cudd_IsComplement(t)) {
	    r = cuddUniqueInter(dd, index, Cudd_Not(t), t);
	    if (r == NULL) {
		Cudd_IterDerefBdd(dd, t);
		return(NULL);
	    }
	    r = Cudd_Not(r);
	} else {
	    r = cuddUniqueInter(dd, index, t, Cudd_Not(t));
	    if (r == NULL) {
		Cudd_IterDerefBdd(dd, t);
		return(NULL);
	    }
	}
	cuddDeref(t);
	if (r == NULL)
	    return(NULL);
	cuddCacheInsert2(dd, Cudd_bddSqueeze, l, u, r);
	return(Cudd_NotCond(r,comple));
    } else if ((lt == zero || Cudd_bddLeq(dd,lt,Cudd_Not(ue))) &&
	       (ut == one  || Cudd_bddLeq(dd,Cudd_Not(le),ut))) { /* c-remap */
	e = cuddBddSqueeze(dd, le, ue);
	cuddRef(e);
	if (Cudd_IsComplement(e)) {
	    r = cuddUniqueInter(dd, index, Cudd_Not(e), e);
	    if (r == NULL) {
		Cudd_IterDerefBdd(dd, e);
		return(NULL);
	    }
	} else {
	    r = cuddUniqueInter(dd, index, e, Cudd_Not(e));
	    if (r == NULL) {
		Cudd_IterDerefBdd(dd, e);
		return(NULL);
	    }
	    r = Cudd_Not(r);
	}
	cuddDeref(e);
	if (r == NULL)
	    return(NULL);
	cuddCacheInsert2(dd, Cudd_bddSqueeze, l, u, r);
	return(Cudd_NotCond(r,comple));
    }

#if 0
    /* If the two intervals intersect, take a solution from
    ** the intersection of the intervals. This guarantees that the
    ** splitting variable will not appear in the result.
    ** This approach corresponds to two-sided matching, and is very
    ** expensive. */
    if (Cudd_bddLeq(dd,lt,ue) && Cudd_bddLeq(dd,le,ut)) {
	DdNode *au, *al;
	au = cuddBddAndRecur(dd,ut,ue);
	if (au == NULL)
	    return(NULL);
	cuddRef(au);
	al = cuddBddAndRecur(dd,Cudd_Not(lt),Cudd_Not(le));
	if (al == NULL) {
	    Cudd_IterDerefBdd(dd,au);
	    return(NULL);
	}
	cuddRef(al);
	al = Cudd_Not(al);
	ar = cuddBddSqueeze(dd, al, au);
	if (ar == NULL) {
	    Cudd_IterDerefBdd(dd,au);
	    Cudd_IterDerefBdd(dd,al);
	    return(NULL);
	}
	cuddRef(ar);
	Cudd_IterDerefBdd(dd,au);
	Cudd_IterDerefBdd(dd,al);
    } else {
	ar = NULL;
    }
#endif

    t = cuddBddSqueeze(dd, lt, ut);
    if (t == NULL) {
	return(NULL);
    }
    cuddRef(t);
    e = cuddBddSqueeze(dd, le, ue);
    if (e == NULL) {
	Cudd_IterDerefBdd(dd,t);
	return(NULL);
    }
    cuddRef(e);

    if (Cudd_IsComplement(t)) {
	t = Cudd_Not(t);
	e = Cudd_Not(e);
	r = (t == e) ? t : cuddUniqueInter(dd, index, t, e);
	if (r == NULL) {
	    Cudd_IterDerefBdd(dd, e);
	    Cudd_IterDerefBdd(dd, t);
	    return(NULL);
	}
	r = Cudd_Not(r);
    } else {
	r = (t == e) ? t : cuddUniqueInter(dd, index, t, e);
	if (r == NULL) {
	    Cudd_IterDerefBdd(dd, e);
	    Cudd_IterDerefBdd(dd, t);
	    return(NULL);
	}
    }
    cuddDeref(t);
    cuddDeref(e);

#if 0
    /* Check whether there is a result obtained by abstraction and whether
    ** it is better than the one obtained by recursion. */
    cuddRef(r);
    if (ar != NULL) {
	if (Cudd_DagSize(ar) <= Cudd_DagSize(r)) {
	    Cudd_IterDerefBdd(dd, r);
	    r = ar;
	} else {
	    Cudd_IterDerefBdd(dd, ar);
	}
    }
    cuddDeref(r);
#endif

    cuddCacheInsert2(dd, Cudd_bddSqueeze, l, u, r);
    return(Cudd_NotCond(r,comple));

} /* end of cuddBddSqueeze */


/**
  @brief Performs the recursive step of Cudd_bddInterpolate.

  @details This procedure exploits the fact that if we complement and
  swap the bounds of the interval we obtain a valid solution by taking
  the complement of the solution to the original problem. Therefore,
  we can enforce the condition that the upper bound is always regular.

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddInterpolate

*/
static DdNode *
cuddBddInterpolate(
  DdManager * dd,
  DdNode * l,
  DdNode * u)
{
    DdNode *one, *zero, *r, *lt, *le, *ut, *ue, *t, *e;
#if 0
    DdNode *ar;
#endif
    int comple = 0;
    int topu, topl;
    unsigned int index;

    statLine(dd);
    if (l == u) {
	return(l);
    }
    one = DD_ONE(dd);
    zero = Cudd_Not(one);
    if (l == zero) return(l);
    if (u == one)  return(u);

    /* Make canonical to increase the utilization of the cache. */
    if (Cudd_IsComplement(u)) {
	DdNode *temp;
	temp = Cudd_Not(l);
	l = Cudd_Not(u);
	u = temp;
	comple = 1;
    }
    /* At this point u is regular and non-constant; l is non-constant, but
    ** may be complemented. */

    /* Check the cache. */
    r = cuddCacheLookup2(dd, Cudd_bddInterpolate, l, u);
    if (r != NULL) {
	return(Cudd_NotCond(r,comple));
    }

    checkWhetherToGiveUp(dd);

    /* Recursive step. */
    topu = dd->perm[u->index];
    topl = dd->perm[Cudd_Regular(l)->index];
    if (topu < topl) {
        /* Universally quantify top variable from upper bound. */
        DdNode *qu;
	ut = cuddT(u); ue = cuddE(u);
        qu = cuddBddAndRecur(dd, ut, ue);
        if (qu == NULL) return(NULL);
        cuddRef(qu);
        r = cuddBddInterpolate(dd, l, qu);
        if (r == NULL) {
            Cudd_IterDerefBdd(dd, qu);
            return(NULL);
        }
        cuddRef(r);
        Cudd_IterDerefBdd(dd, qu);
        cuddCacheInsert2(dd, Cudd_bddInterpolate, l, u, r);
        cuddDeref(r);
        return(Cudd_NotCond(r, comple));
    } else if (topl < topu) {
        /* Existentially quantify top variable from lower bound. */
        DdNode *ql;
        /* Find complements of cofactors of c. */
        if (Cudd_IsComplement(l)) {
            lt = cuddT(Cudd_Regular(l));
            le = cuddE(Cudd_Regular(l));
        } else {
            lt = Cudd_Not(cuddT(l));
            le = Cudd_Not(cuddE(l));
        }
        /* Disjoin cofactors by applying DeMorgan. */
        ql = cuddBddAndRecur(dd, lt, le);
        if (ql == NULL) return (NULL);
        cuddRef(ql);
        ql = Cudd_Not(ql);
        r = cuddBddInterpolate(dd, ql, u);
        if (r == NULL) {
            Cudd_IterDerefBdd(dd, ql);
            return(NULL);
        }
        cuddRef(r);
        Cudd_IterDerefBdd(dd, ql);
        cuddCacheInsert2(dd, Cudd_bddInterpolate, l, u, r);
        cuddDeref(r);
        return(Cudd_NotCond(r, comple));
    }

    /* Both bounds depend on the top variable: split and recur. */
    index = u->index;
    ut = cuddT(u); ue = cuddE(u);
    lt = cuddT(Cudd_Regular(l)); le = cuddE(Cudd_Regular(l));
    if (Cudd_IsComplement(l)) {
        lt = Cudd_Not(lt);
        le = Cudd_Not(le);
    }

    t = cuddBddInterpolate(dd, lt, ut);
    if (t == NULL) {
	return(NULL);
    }
    cuddRef(t);
    e = cuddBddInterpolate(dd, le, ue);
    if (e == NULL) {
	Cudd_IterDerefBdd(dd,t);
	return(NULL);
    }
    cuddRef(e);

    if (Cudd_IsComplement(t)) {
	t = Cudd_Not(t);
	e = Cudd_Not(e);
	r = (t == e) ? t : cuddUniqueInter(dd, index, t, e);
	if (r == NULL) {
	    Cudd_IterDerefBdd(dd, e);
	    Cudd_IterDerefBdd(dd, t);
	    return(NULL);
	}
	r = Cudd_Not(r);
    } else {
	r = (t == e) ? t : cuddUniqueInter(dd, index, t, e);
	if (r == NULL) {
	    Cudd_IterDerefBdd(dd, e);
	    Cudd_IterDerefBdd(dd, t);
	    return(NULL);
	}
    }
    cuddDeref(t);
    cuddDeref(e);

    cuddCacheInsert2(dd, Cudd_bddInterpolate, l, u, r);
    return(Cudd_NotCond(r,comple));

} /* end of cuddBddInterpolate */
