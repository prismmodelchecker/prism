/**
  @file

  @ingroup cudd

  @brief Cofactoring functions.

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

static int ddVarsAreSymmetricBefore(DdManager * dd, DdNode * f, DdNode * var1, DdNode * var2);
static int ddVarsAreSymmetricBetween(DdManager * dd, DdNode * f1, DdNode * f0, DdNode * var2);

/** \endcond */


/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Computes the cofactor of f with respect to g.

  @details g must be the %BDD or the %ADD of a cube.

  @return a pointer to the cofactor if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddConstrain Cudd_bddRestrict

*/
DdNode *
Cudd_Cofactor(
  DdManager * dd,
  DdNode * f,
  DdNode * g)
{
    DdNode *res,*zero;

    zero = Cudd_Not(DD_ONE(dd));
    if (g == zero || g == DD_ZERO(dd)) {
	(void) fprintf(dd->err,"Cudd_Cofactor: Invalid restriction 1\n");
	dd->errorCode = CUDD_INVALID_ARG;
	return(NULL);
    }
    do {
	dd->reordered = 0;
	res = cuddCofactorRecur(dd,f,g);
    } while (dd->reordered == 1);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }
    return(res);

} /* end of Cudd_Cofactor */


/**
  @brief Checks whether g is the %BDD of a cube.

  @details The constant 1 is a valid cube, but all other constant
  functions cause cuddCheckCube to return 0.

  @return 1 in case of success; 0 otherwise.

  @sideeffect None

*/
int
Cudd_CheckCube(
  DdManager * dd,
  DdNode * g)
{
    DdNode *g1,*g0,*one,*zero;
    
    one = DD_ONE(dd);
    if (g == one) return(1);
    if (Cudd_Not(g) == one) return(0);

    zero = Cudd_Not(one);
    cuddGetBranches(g,&g1,&g0);

    if (g0 == zero) {
        return(Cudd_CheckCube(dd, g1));
    }
    if (g1 == zero) {
        return(Cudd_CheckCube(dd, g0));
    }
    return(0);

} /* end of Cudd_CheckCube */


/**
   @brief Checks whether two variables are symmetric in a BDD.

   @return 1 if the variables are symmetric; 0 if they are not.

   @details No nodes are built during the check.

   @sideeffect None
*/
int
Cudd_VarsAreSymmetric(
  DdManager * dd /**< manager */,
  DdNode * f /**< BDD whose variables are tested */,
  int index1 /**< index of first variable */,
  int index2 /**< index of second variable */)
{
    DdNode *var1, *var2;

    if (index1 == index2) /* trivial case: symmetry is reflexive */
        return(1);

    if (index1 >= dd->size) {
        if (index2 >= dd->size) {
            return(1); /* f depends on neither variable */
        } else {
            /* f does not depend on var1; check whether it depends on var2 */
            var2 = dd->vars[index2];
            return ddVarsAreSymmetricBetween(dd, f, f, var2);
        }
    } else if (index2 >= dd->size) {
        /* f does not depend on var2; check whether it depends on var1 */
        var1 = dd->vars[index1];
        return  ddVarsAreSymmetricBetween(dd, f, f, var1);
    }

    /* Make sure index1 denotes the variable currently closer to the root. */
    if (dd->perm[index1] < dd->perm[index2]) {
        var1 = dd->vars[index1];
        var2 = dd->vars[index2];
    } else {
        var1 = dd->vars[index2];
        var2 = dd->vars[index1];
    }

    return ddVarsAreSymmetricBefore(dd, f, var1, var2);

} /* end of Cudd_VarsAreSymmetric */


/*---------------------------------------------------------------------------*/
/* Definition of internal functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Computes the children of g.

  @sideeffect None

*/
void
cuddGetBranches(
  DdNode * g,
  DdNode ** g1,
  DdNode ** g0)
{
    DdNode	*G = Cudd_Regular(g);

    *g1 = cuddT(G);
    *g0 = cuddE(G);
    if (Cudd_IsComplement(g)) {
	*g1 = Cudd_Not(*g1);
	*g0 = Cudd_Not(*g0);
    }

} /* end of cuddGetBranches */


/**
  @brief Performs the recursive step of Cudd_Cofactor.

  @return a pointer to the cofactor if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_Cofactor

*/
DdNode *
cuddCofactorRecur(
  DdManager * dd,
  DdNode * f,
  DdNode * g)
{
    DdNode *one,*zero,*F,*G,*g1,*g0,*f1,*f0,*t,*e,*r;
    int topf,topg;
    int comple;

    statLine(dd);
    F = Cudd_Regular(f);
    if (cuddIsConstant(F)) return(f);

    one = DD_ONE(dd);

    /* The invariant g != 0 is true on entry to this procedure and is
    ** recursively maintained by it. Therefore it suffices to test g
    ** against one to make sure it is not constant.
    */
    if (g == one) return(f);
    /* From now on, f and g are known not to be constants. */

    comple = f != F;
    r = cuddCacheLookup2(dd,Cudd_Cofactor,F,g);
    if (r != NULL) {
	return(Cudd_NotCond(r,comple));
    }

    checkWhetherToGiveUp(dd);

    topf = dd->perm[F->index];
    G = Cudd_Regular(g);
    topg = dd->perm[G->index];

    /* We take the cofactors of F because we are going to rely on
    ** the fact that the cofactors of the complement are the complements
    ** of the cofactors to better utilize the cache. Variable comple
    ** remembers whether we have to complement the result or not.
    */
    if (topf <= topg) {
	f1 = cuddT(F); f0 = cuddE(F);
    } else {
	f1 = f0 = F;
    }
    if (topg <= topf) {
	g1 = cuddT(G); g0 = cuddE(G);
	if (g != G) { g1 = Cudd_Not(g1); g0 = Cudd_Not(g0); }
    } else {
	g1 = g0 = g;
    }

    zero = Cudd_Not(one);
    if (topf >= topg) {
	if (g0 == zero || g0 == DD_ZERO(dd)) {
	    r = cuddCofactorRecur(dd, f1, g1);
	} else if (g1 == zero || g1 == DD_ZERO(dd)) {
	    r = cuddCofactorRecur(dd, f0, g0);
	} else {
	    (void) fprintf(dd->err,
			   "Cudd_Cofactor: Invalid restriction 2\n");
	    dd->errorCode = CUDD_INVALID_ARG;
	    return(NULL);
	}
	if (r == NULL) return(NULL);
    } else /* if (topf < topg) */ {
	t = cuddCofactorRecur(dd, f1, g);
	if (t == NULL) return(NULL);
    	cuddRef(t);
    	e = cuddCofactorRecur(dd, f0, g);
	if (e == NULL) {
	    Cudd_RecursiveDeref(dd, t);
	    return(NULL);
	}
	cuddRef(e);

	if (t == e) {
	    r = t;
	} else if (Cudd_IsComplement(t)) {
	    r = cuddUniqueInter(dd,(int)F->index,Cudd_Not(t),Cudd_Not(e));
	    if (r != NULL)
		r = Cudd_Not(r);
	} else {
	    r = cuddUniqueInter(dd,(int)F->index,t,e);
	}
	if (r == NULL) {
	    Cudd_RecursiveDeref(dd ,e);
	    Cudd_RecursiveDeref(dd ,t);
	    return(NULL);
	}
	cuddDeref(t);
	cuddDeref(e);
    }

    cuddCacheInsert2(dd,Cudd_Cofactor,F,g,r);

    return(Cudd_NotCond(r,comple));

} /* end of cuddCofactorRecur */


/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/

/**
   @brief Implements the upper recursive step of Cudd_VarsAreSymmetric().

   @details The assumption is made that the level of index1 is less
   than the level of index2.

   @return 1 if the variables are symmetric for the given function;
   0 if they are not.

   @see Cudd_VarsAreSymmetric ddVarsAreSymmetricBetween

*/
static int
ddVarsAreSymmetricBefore(
  DdManager * dd,
  DdNode * f,
  DdNode * var1,
  DdNode * var2)
{
    DdNode *F, *ft, *fe, *r;
    int top, res, level1;

    statLine(dd);
    F = Cudd_Regular(f);
    if (cuddIsConstant(F)) /* f depends on neither variable */
        return(1);
    top = dd->perm[F->index];
    if (top > dd->perm[var2->index])
        return(1); /* f depends on neither variable */
    /* Cache lookup.  We take advantage of the observation that
     * var1 and var2 are symmetric in f iff they are symmetric in
     * the complement of f. */
    r = cuddCacheLookup(dd, DD_VARS_SYMM_BEFORE_TAG, F, var1, var2);
    if (r != NULL) {
        return(r == DD_ONE(dd) ? 1 : 0);
    }
    level1 = dd->perm[var1->index];
    if (top > level1)
        /* Check whether f1 depends on the variable currently at level2. */
        return ddVarsAreSymmetricBetween(dd, f, f, var2);
    ft = cuddT(F);
    fe = cuddE(F);
    if (F != f) {
        ft = Cudd_Not(ft);
        fe = Cudd_Not(fe);
    }
    if (top < level1) {
        res = ddVarsAreSymmetricBefore(dd, ft, var1, var2);
        if (res)
            res = ddVarsAreSymmetricBefore(dd, fe, var1, var2);
    } else {
        res = ddVarsAreSymmetricBetween(dd, ft, fe, var2);
    }
    /* Cache insertion. */
    cuddCacheInsert(dd, DD_VARS_SYMM_BEFORE_TAG, F, var1, var2,
                    res ? DD_ONE(dd) : Cudd_Not(DD_ONE(dd)));
    return(res);

} /* end of ddVarsAreSymmetricBefore */


/**
   @brief Implements the lower recursive step of Cudd_VarsAreSymmetric().

   @return 1 if the negative cofactor of the first argument w.r.t. the variable
   currently at level2 is the same as the positive cofactor of the second
   argument; 0 if the two cofactors are not the same.

   @see Cudd_VarsAreSymmetric ddVarsAreSymmetricBefore

*/
static int
ddVarsAreSymmetricBetween(
  DdManager * dd,
  DdNode * f1,
  DdNode * f0,
  DdNode * var2)
{
    DdNode *F1, *F0, *f1t, *f1e, *f0t, *f0e, *r;
    int topf1, topf0, top, res;
    int level2 = dd->perm[var2->index];

    statLine(dd);
    F1 = Cudd_Regular(f1);
    F0 = Cudd_Regular(f0);
    if (cuddIsConstant(F1) && cuddIsConstant(F0))
        return f1 == f0;
    /* Here we know that one of f1 and f0 is not constant.  Hence the
     * least index is that of a variable. */
    if (cuddIsConstant(F1))
        topf1 = CUDD_CONST_INDEX;
    else
        topf1 = dd->perm[F1->index];
    if (cuddIsConstant(F0))
        topf0 = CUDD_CONST_INDEX;
    else
        topf0 = dd->perm[F0->index];
    if (topf0 > level2 && topf1 > level2)
        return(f1 == f0);
    /* Cache lookup. */
    r = cuddCacheLookup(dd, DD_VARS_SYMM_BETWEEN_TAG, f1, f0, var2);
    if (r != NULL) {
        return(r == DD_ONE(dd) ? 1 : 0);
    }
    /* Compute cofactors and find top level. */
    if (topf1 <= topf0) {
        top = topf1;
        f1t = cuddT(F1);
        f1e = cuddE(F1);
        if (F1 != f1) {
            f1t = Cudd_Not(f1t);
            f1e = Cudd_Not(f1e);
        }
    } else {
        top = topf0;
        f1t = f1e = f1;
    }
    if (topf0 <= topf1) {
        f0t = cuddT(F0);
        f0e = cuddE(F0);
        if (F0 != f0) {
            f0t = Cudd_Not(f0t);
            f0e = Cudd_Not(f0e);
        }
    } else {
        f0t = f0e = f0;
    }
    if (top < level2) {
        res = ddVarsAreSymmetricBetween(dd, f1t, f0t, var2);
        if (res)
            res = ddVarsAreSymmetricBetween(dd, f1e, f0e, var2);
    } else {
        assert(top == level2);
        res = f1e == f0t;
    }
    /* Cache insertion. */
    cuddCacheInsert(dd, DD_VARS_SYMM_BETWEEN_TAG, f1, f0, var2,
                    res ? DD_ONE(dd) : Cudd_Not(DD_ONE(dd)));
    return(res);

} /* end of ddVarsAreSymmetricBetween */
