/**
  @file

  @ingroup cudd

  @brief Functions for the solution of satisfiability related problems.

  @author Seh-Woong Jeong, Fabio Somenzi

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

#define	DD_BIGGY	100000000

/*---------------------------------------------------------------------------*/
/* Stucture declarations                                                     */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/

/**
   @brief Type of item stored in memoization table.
*/
typedef struct cuddPathPair {
    int	pos;
    int	neg;
} cuddPathPair;

/*---------------------------------------------------------------------------*/
/* Variable declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Macro declarations                                                        */
/*---------------------------------------------------------------------------*/

#define WEIGHT(weight, col)	((weight) == NULL ? 1 : weight[col])

/** \cond */

/*---------------------------------------------------------------------------*/
/* Static function prototypes                                                */
/*---------------------------------------------------------------------------*/

static enum st_retval freePathPair (void *key, void *value, void *arg);
static cuddPathPair getShortest (DdManager *dd, DdNode *root, int *cost, int *support, st_table *visited);
static DdNode * getPath (DdManager *manager, st_table *visited, DdNode *f, int *weight, int cost);
static cuddPathPair getLargest (DdManager *dd, DdNode *root, st_table *visited);
static DdNode * getCube (DdManager *manager, st_table *visited, DdNode *f, int cost);
static DdNode * ddBddMaximallyExpand(DdManager *dd, DdNode *lb, DdNode *ub, DdNode *f);
static int ddBddShortestPathUnate(DdManager *dd, DdNode *f, int *phases, st_table *table);
static DdNode * ddGetLargestCubeUnate(DdManager *dd, DdNode *f, int *phases, st_table *table);

/** \endcond */

/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Returns the value of a %DD for a given variable assignment.

  @details The variable assignment is passed in an array of int's,
  that should specify a zero or a one for each variable in the support
  of the function.

  @return a pointer to a constant node. No new nodes are produced.

  @sideeffect None

  @see Cudd_bddLeq Cudd_addEvalConst

*/
DdNode *
Cudd_Eval(
  DdManager * dd,
  DdNode * f,
  int * inputs)
{
    int comple;
    DdNode *ptr;

    (void) dd; /* avoid warning */
    comple = Cudd_IsComplement(f);
    ptr = Cudd_Regular(f);

    while (!cuddIsConstant(ptr)) {
	if (inputs[ptr->index] == 1) {
	    ptr = cuddT(ptr);
	} else {
	    comple ^= Cudd_IsComplement(cuddE(ptr));
	    ptr = Cudd_Regular(cuddE(ptr));
	}
    }
    return(Cudd_NotCond(ptr,comple));

} /* end of Cudd_Eval */


/**
  @brief Finds a shortest path in a %DD.

  @details f is the %DD we want to get the shortest path for;
  weight\[i\] is the weight of the THEN arc coming from the node whose
  index is i. If weight is NULL, then unit weights are assumed for all
  THEN arcs. All ELSE arcs have 0 weight.  If non-NULL, both weight
  and support should point to arrays with at least as many entries as
  there are variables in the manager.

  @return the shortest path as the %BDD of a cube.

  @sideeffect support contains on return the true support of f.
  If support is NULL on entry, then Cudd_ShortestPath does not compute
  the true support info. length contains the length of the path.

  @see Cudd_ShortestLength Cudd_LargestCube

*/
DdNode *
Cudd_ShortestPath(
  DdManager * manager,
  DdNode * f,
  int * weight,
  int * support,
  int * length)
{
    DdNode	*F;
    st_table	*visited;
    DdNode	*sol;
    cuddPathPair *rootPair;
    int		complement, cost;
    int		i;

    DdNode *one = DD_ONE(manager);
    DdNode *zero = DD_ZERO(manager);

    /* Initialize support. Support does not depend on variable order.
    ** Hence, it does not need to be reinitialized if reordering occurs.
    */
    if (support) {
      for (i = 0; i < manager->size; i++) {
	support[i] = 0;
      }
    }

    if (f == Cudd_Not(one) || f == zero) {
      *length = DD_BIGGY;
      return(Cudd_Not(one));
    }
    /* From this point on, a path exists. */

    do {
	manager->reordered = 0;

	/* Initialize visited table. */
	visited = st_init_table(st_ptrcmp, st_ptrhash);

	/* Now get the length of the shortest path(s) from f to 1. */
	(void) getShortest(manager, f, weight, support, visited);

	complement = Cudd_IsComplement(f);

	F = Cudd_Regular(f);

	if (!st_lookup(visited, F, (void **) &rootPair)) return(NULL);

	if (complement) {
	  cost = rootPair->neg;
	} else {
	  cost = rootPair->pos;
	}

	/* Recover an actual shortest path. */
	sol = getPath(manager,visited,f,weight,cost);

	st_foreach(visited, freePathPair, NULL);
	st_free_table(visited);

    } while (manager->reordered == 1);
    if (manager->errorCode == CUDD_TIMEOUT_EXPIRED && manager->timeoutHandler) {
        manager->timeoutHandler(manager, manager->tohArg);
    }

    *length = cost;
    return(sol);

} /* end of Cudd_ShortestPath */


/**
  @brief Finds a largest cube in a %DD.

  @details f is the %DD we want to get the largest cube for. The
  problem is translated into the one of finding a shortest path in f,
  when both THEN and ELSE arcs are assumed to have unit length. This
  yields a largest cube in the disjoint cover corresponding to the
  %DD. Therefore, it is not necessarily the largest implicant of f.

  @return the largest cube as a %BDD.

  @sideeffect The number of literals of the cube is returned in the location
  pointed by length if it is non-null.

  @see Cudd_ShortestPath

*/
DdNode *
Cudd_LargestCube(
  DdManager * manager,
  DdNode * f,
  int * length)
{
    DdNode	*F;
    st_table	*visited;
    DdNode	*sol;
    cuddPathPair *rootPair;
    int		complement, cost;

    DdNode *one = DD_ONE(manager);
    DdNode *zero = DD_ZERO(manager);

    if (f == Cudd_Not(one) || f == zero) {
	if (length != NULL) {
            *length = DD_BIGGY;
        }
	return(Cudd_Not(one));
    }
    /* From this point on, a path exists. */

    do {
	manager->reordered = 0;

	/* Initialize visited table. */
	visited = st_init_table(st_ptrcmp, st_ptrhash);

	/* Now get the length of the shortest path(s) from f to 1. */
	(void) getLargest(manager, f, visited);

	complement = Cudd_IsComplement(f);

	F = Cudd_Regular(f);

	if (!st_lookup(visited, F, (void **) &rootPair)) return(NULL);

	if (complement) {
	  cost = rootPair->neg;
	} else {
	  cost = rootPair->pos;
	}

	/* Recover an actual shortest path. */
	sol = getCube(manager,visited,f,cost);

	st_foreach(visited, freePathPair, NULL);
	st_free_table(visited);

    } while (manager->reordered == 1);

    if (length != NULL) {
        *length = cost;
    }
    if (manager->errorCode == CUDD_TIMEOUT_EXPIRED && manager->timeoutHandler) {
        manager->timeoutHandler(manager, manager->tohArg);
    }
    return(sol);

} /* end of Cudd_LargestCube */


/**
  @brief Find the length of the shortest path(s) in a %DD.

  @details f is the %DD we want to get the shortest path for;
  weight\[i\] is the weight of the THEN edge coming from the node
  whose index is i. All ELSE edges have 0 weight.

  @return the length of the shortest path(s) if such a path is found;
  a large number if the function is identically 0, and CUDD_OUT_OF_MEM
  in case of failure.

  @sideeffect None

  @see Cudd_ShortestPath

*/
int
Cudd_ShortestLength(
  DdManager * manager,
  DdNode * f,
  int * weight)
{
    DdNode	*F;
    st_table	*visited;
    cuddPathPair *my_pair;
    int		complement, cost;

    DdNode *one = DD_ONE(manager);
    DdNode *zero = DD_ZERO(manager);

    if (f == Cudd_Not(one) || f == zero) {
	return(DD_BIGGY);
    }

    /* From this point on, a path exists. */
    /* Initialize visited table and support. */
    visited = st_init_table(st_ptrcmp, st_ptrhash);

    /* Now get the length of the shortest path(s) from f to 1. */
    (void) getShortest(manager, f, weight, NULL, visited);

    complement = Cudd_IsComplement(f);

    F = Cudd_Regular(f);

    if (!st_lookup(visited, F, (void **) &my_pair)) return(CUDD_OUT_OF_MEM);
    
    if (complement) {
	cost = my_pair->neg;
    } else {
	cost = my_pair->pos;
    }

    st_foreach(visited, freePathPair, NULL);
    st_free_table(visited);

    return(cost);

} /* end of Cudd_ShortestLength */


/**
  @brief Checks whether a %BDD is negative unate in a
  variable.

  @details Determines whether the function represented by %BDD f is
  negative unate (monotonic decreasing) in variable i.  This function
  does not generate any new nodes.

  @return the constant one is f is unate and the (logical) constant
  zero if it is not.

  @sideeffect None

  @see Cudd_Increasing

*/
DdNode *
Cudd_Decreasing(
  DdManager * dd,
  DdNode * f,
  int  i)
{
    int topf, level;
    DdNode *F, *fv, *fvn, *res;
    DD_CTFP cacheOp;

    statLine(dd);
#ifdef DD_DEBUG
    assert(0 <= i && i < dd->size);
#endif

    F = Cudd_Regular(f);
    topf = cuddI(dd,F->index);

    /* Check terminal case. If topf > i, f does not depend on var.
    ** Therefore, f is unate in i.
    */
    level = (unsigned) dd->perm[i];
    if (topf > level) {
	return(DD_ONE(dd));
    }

    /* From now on, f is not constant. */

    /* Check cache. */
    cacheOp = (DD_CTFP) Cudd_Decreasing;
    res = cuddCacheLookup2(dd,cacheOp,f,dd->vars[i]);
    if (res != NULL) {
	return(res);
    }

    checkWhetherToGiveUp(dd);

    /* Compute cofactors. */
    fv = cuddT(F); fvn = cuddE(F);
    if (F != f) {
	fv = Cudd_Not(fv);
	fvn = Cudd_Not(fvn);
    }

    if (topf == level) {
	/* Special case: if fv is regular, fv(1,...,1) = 1;
	** If in addition fvn is complemented, fvn(1,...,1) = 0.
	** But then f(1,1,...,1) > f(0,1,...,1). Hence f is not
	** monotonic decreasing in i.
	*/
	if (!Cudd_IsComplement(fv) && Cudd_IsComplement(fvn)) {
	    return(Cudd_Not(DD_ONE(dd)));
	}
	res = Cudd_bddLeq(dd,fv,fvn) ? DD_ONE(dd) : Cudd_Not(DD_ONE(dd));
    } else {
	res = Cudd_Decreasing(dd,fv,i);
	if (res == DD_ONE(dd)) {
	    res = Cudd_Decreasing(dd,fvn,i);
	}
    }

    cuddCacheInsert2(dd,cacheOp,f,dd->vars[i],res);
    return(res);

} /* end of Cudd_Decreasing */


/**
  @brief Checks whether a %BDD is positive unate in a variable.

  @details Determines whether the function represented by %BDD f is
  positive unate (monotonic increasing) in variable i. It is based on
  Cudd_Decreasing and the fact that f is monotonic increasing in i if
  and only if its complement is monotonic decreasing in i.

  @sideeffect None

  @see Cudd_Decreasing

*/
DdNode *
Cudd_Increasing(
  DdManager * dd,
  DdNode * f,
  int  i)
{
    return(Cudd_Decreasing(dd,Cudd_Not(f),i));

} /* end of Cudd_Increasing */


/**
  @brief Tells whether F and G are identical wherever D is 0.

  @details F and G are either two ADDs or two BDDs.  D is either a 0-1
  %ADD or a %BDD.  No new nodes are created.

  @return 1 if F and G are equivalent, and 0 otherwise.

  @sideeffect None

  @see Cudd_bddLeqUnless

*/
int
Cudd_EquivDC(
  DdManager * dd,
  DdNode * F,
  DdNode * G,
  DdNode * D)
{
    DdNode *tmp, *One, *Gr, *Dr;
    DdNode *Fv, *Fvn, *Gv, *Gvn, *Dv, *Dvn;
    int res;
    int flevel, glevel, dlevel, top;

    One = DD_ONE(dd);

    statLine(dd);
    /* Check terminal cases. */
    if (D == One || F == G) return(1);
    if (D == Cudd_Not(One) || D == DD_ZERO(dd) || F == Cudd_Not(G)) return(0);

    /* From now on, D is non-constant. */

    /* Normalize call to increase cache efficiency. */
    if (F > G) {
	tmp = F;
	F = G;
	G = tmp;
    }
    if (Cudd_IsComplement(F)) {
	F = Cudd_Not(F);
	G = Cudd_Not(G);
    }

    /* From now on, F is regular. */

    /* Check cache. */
    tmp = cuddCacheLookup(dd,DD_EQUIV_DC_TAG,F,G,D);
    if (tmp != NULL) return(tmp == One);

    /* Find splitting variable. */
    flevel = cuddI(dd,F->index);
    Gr = Cudd_Regular(G);
    glevel = cuddI(dd,Gr->index);
    top = ddMin(flevel,glevel);
    Dr = Cudd_Regular(D);
    dlevel = dd->perm[Dr->index];
    top = ddMin(top,dlevel);

    /* Compute cofactors. */
    if (top == flevel) {
	Fv = cuddT(F);
	Fvn = cuddE(F);
    } else {
	Fv = Fvn = F;
    }
    if (top == glevel) {
	Gv = cuddT(Gr);
	Gvn = cuddE(Gr);
	if (G != Gr) {
	    Gv = Cudd_Not(Gv);
	    Gvn = Cudd_Not(Gvn);
	}
    } else {
	Gv = Gvn = G;
    }
    if (top == dlevel) {
	Dv = cuddT(Dr);
	Dvn = cuddE(Dr);
	if (D != Dr) {
	    Dv = Cudd_Not(Dv);
	    Dvn = Cudd_Not(Dvn);
	}
    } else {
	Dv = Dvn = D;
    }

    /* Solve recursively. */
    res = Cudd_EquivDC(dd,Fv,Gv,Dv);
    if (res != 0) {
	res = Cudd_EquivDC(dd,Fvn,Gvn,Dvn);
    }
    cuddCacheInsert(dd,DD_EQUIV_DC_TAG,F,G,D,(res) ? One : Cudd_Not(One));

    return(res);

} /* end of Cudd_EquivDC */


/**
  @brief Tells whether f is less than of equal to G unless D is 1.

  @details f, g, and D are BDDs.  No new nodes are created.

  @return 1 if f is less than of equal to G, and 0 otherwise.

  @sideeffect None

  @see Cudd_EquivDC Cudd_bddLeq Cudd_bddIteConstant

*/
int
Cudd_bddLeqUnless(
  DdManager *dd,
  DdNode *f,
  DdNode *g,
  DdNode *D)
{
    DdNode *tmp, *One, *F, *G;
    DdNode *Ft, *Fe, *Gt, *Ge, *Dt, *De;
    int res;
    int flevel, glevel, dlevel, top;

    statLine(dd);

    One = DD_ONE(dd);

    /* Check terminal cases. */
    if (f == g || g == One || f == Cudd_Not(One) || D == One ||
	D == f || D == Cudd_Not(g)) return(1);
    /* Check for two-operand cases. */
    if (D == Cudd_Not(One) || D == g || D == Cudd_Not(f))
	return(Cudd_bddLeq(dd,f,g));
    if (g == Cudd_Not(One) || g == Cudd_Not(f)) return(Cudd_bddLeq(dd,f,D));
    if (f == One) return(Cudd_bddLeq(dd,Cudd_Not(g),D));

    /* From now on, f, g, and D are non-constant, distinct, and
    ** non-complementary. */

    /* Normalize call to increase cache efficiency.  We rely on the
    ** fact that f <= g unless D is equivalent to not(g) <= not(f)
    ** unless D and to f <= D unless g.  We make sure that D is
    ** regular, and that at most one of f and g is complemented.  We also
    ** ensure that when two operands can be swapped, the one with the
    ** lowest address comes first. */

    if (Cudd_IsComplement(D)) {
	if (Cudd_IsComplement(g)) {
	    /* Special case: if f is regular and g is complemented,
	    ** f(1,...,1) = 1 > 0 = g(1,...,1).  If D(1,...,1) = 0, return 0.
	    */
	    if (!Cudd_IsComplement(f)) return(0);
	    /* !g <= D unless !f  or  !D <= g unless !f */
	    tmp = D;
	    D = Cudd_Not(f);
	    if (g < tmp) {
		f = Cudd_Not(g);
		g = tmp;
	    } else {
		f = Cudd_Not(tmp);
	    }
	} else {
	    if (Cudd_IsComplement(f)) {
		/* !D <= !f unless g  or  !D <= g unless !f */
		tmp = f;
		f = Cudd_Not(D);
		if (tmp < g) {
		    D = g;
		    g = Cudd_Not(tmp);
		} else {
		    D = Cudd_Not(tmp);
		}
	    } else {
		/* f <= D unless g  or  !D <= !f unless g */
		tmp = D;
		D = g;
		if (tmp < f) {
		    g = Cudd_Not(f);
		    f = Cudd_Not(tmp);
		} else {
		    g = tmp;
		}
	    }
	}
    } else {
	if (Cudd_IsComplement(g)) {
	    if (Cudd_IsComplement(f)) {
		/* !g <= !f unless D  or  !g <= D unless !f */
		tmp = f;
		f = Cudd_Not(g);
		if (D < tmp) {
		    g = D;
		    D = Cudd_Not(tmp);
		} else {
		    g = Cudd_Not(tmp);
		}
	    } else {
		/* f <= g unless D  or  !g <= !f unless D */
		if (g < f) {
		    tmp = g;
		    g = Cudd_Not(f);
		    f = Cudd_Not(tmp);
		}
	    }
	} else {
	    /* f <= g unless D  or  f <= D unless g */
	    if (D < g) {
		tmp = D;
		D = g;
		g = tmp;
	    }
	}
    }

    /* From now on, D is regular. */

    /* Check cache. */
    tmp = cuddCacheLookup(dd,DD_BDD_LEQ_UNLESS_TAG,f,g,D);
    if (tmp != NULL) return(tmp == One);

    /* Find splitting variable. */
    F = Cudd_Regular(f);
    flevel = dd->perm[F->index];
    G = Cudd_Regular(g);
    glevel = dd->perm[G->index];
    top = ddMin(flevel,glevel);
    dlevel = dd->perm[D->index];
    top = ddMin(top,dlevel);

    /* Compute cofactors. */
    if (top == flevel) {
	Ft = cuddT(F);
	Fe = cuddE(F);
	if (F != f) {
	    Ft = Cudd_Not(Ft);
	    Fe = Cudd_Not(Fe);
	}
    } else {
	Ft = Fe = f;
    }
    if (top == glevel) {
	Gt = cuddT(G);
	Ge = cuddE(G);
	if (G != g) {
	    Gt = Cudd_Not(Gt);
	    Ge = Cudd_Not(Ge);
	}
    } else {
	Gt = Ge = g;
    }
    if (top == dlevel) {
	Dt = cuddT(D);
	De = cuddE(D);
    } else {
	Dt = De = D;
    }

    /* Solve recursively. */
    res = Cudd_bddLeqUnless(dd,Ft,Gt,Dt);
    if (res != 0) {
	res = Cudd_bddLeqUnless(dd,Fe,Ge,De);
    }
    cuddCacheInsert(dd,DD_BDD_LEQ_UNLESS_TAG,f,g,D,Cudd_NotCond(One,!res));

    return(res);

} /* end of Cudd_bddLeqUnless */


/**
  @brief Compares two ADDs for equality within tolerance.

  @details Two ADDs are reported to be equal if the maximum difference
  between them (the sup norm of their difference) is less than or
  equal to the tolerance parameter.  If parameter <code>pr</code> is
  positive the first failure is reported to the standard output.

  @return 1 if the two ADDs are equal (within tolerance); 0 otherwise.

  @sideeffect None

*/
int
Cudd_EqualSupNorm(
  DdManager * dd /**< manager */,
  DdNode * f /**< first %ADD */,
  DdNode * g /**< second %ADD */,
  CUDD_VALUE_TYPE  tolerance /**< maximum allowed difference */,
  int  pr /**< verbosity level */)
{
    DdNode *fv, *fvn, *gv, *gvn, *r;
    int topf, topg;

    statLine(dd);
    /* Check terminal cases. */
    if (f == g) return(1);
    if (cuddIsConstant(f) && cuddIsConstant(g)) {
	if (ddEqualVal(cuddV(f),cuddV(g),tolerance)) {
	    return(1);
	} else {
	    if (pr>0) {
		(void) fprintf(dd->out,"Offending nodes:\n");
		(void) fprintf(dd->out,
			       "f: address = %p\t value = %40.30f\n",
			       (void *) f, cuddV(f));
		(void) fprintf(dd->out,
			       "g: address = %p\t value = %40.30f\n",
			       (void *) g, cuddV(g));
	    }
	    return(0);
	}
    }

    /* We only insert the result in the cache if the comparison is
    ** successful. Therefore, if we hit we return 1. */
    r = cuddCacheLookup2(dd,(DD_CTFP)Cudd_EqualSupNorm,f,g);
    if (r != NULL) {
	return(1);
    }

    /* Compute the cofactors and solve the recursive subproblems. */
    topf = cuddI(dd,f->index);
    topg = cuddI(dd,g->index);

    if (topf <= topg) {fv = cuddT(f); fvn = cuddE(f);} else {fv = fvn = f;}
    if (topg <= topf) {gv = cuddT(g); gvn = cuddE(g);} else {gv = gvn = g;}

    if (!Cudd_EqualSupNorm(dd,fv,gv,tolerance,pr)) return(0);
    if (!Cudd_EqualSupNorm(dd,fvn,gvn,tolerance,pr)) return(0);

    cuddCacheInsert2(dd,(DD_CTFP)Cudd_EqualSupNorm,f,g,DD_ONE(dd));

    return(1);

} /* end of Cudd_EqualSupNorm */


/**
  @brief Compares two ADDs for equality within relative tolerance.

  @details Same as Cudd_EqualSupNorm but tests for max _relative_
  difference, i.e., (f-g/f)<e instead of (f-g)<e.
  
  @return 1 if the two ADDs are equal (within tolerance); 0 otherwise.

  @sideeffect None

*/
int
Cudd_EqualSupNormRel(
  DdManager * dd /**< manager */,
  DdNode * f /**< first %ADD */,
  DdNode * g /**< second %ADD */,
  CUDD_VALUE_TYPE  tolerance /**< maximum allowed difference */,
  int  pr /**< verbosity level */)
{
    DdNode *fv, *fvn, *gv, *gvn, *r;
    int topf, topg;

    statLine(dd);
    /* Check terminal cases. */
    if (f == g) return(1);
    if (cuddIsConstant(f) && cuddIsConstant(g)) {
	if (ddAbs((cuddV(f) - cuddV(g))/cuddV(f)) < tolerance) {
	    return(1);
	} else {
	    if (pr>0) {
		(void) fprintf(dd->out,"Offending nodes:\n");
		(void) fprintf(dd->out,
			       "f: address = %p\t value = %40.30f\n",
			       (void *) f, cuddV(f));
		(void) fprintf(dd->out,
			       "g: address = %p\t value = %40.30f\n",
			       (void *) g, cuddV(g));
	    }
	    return(0);
	}
    }

    /* We only insert the result in the cache if the comparison is
    ** successful. Therefore, if we hit we return 1. */
    r = cuddCacheLookup2(dd,(DD_CTFP)Cudd_EqualSupNormRel,f,g);
    if (r != NULL) {
	return(1);
    }

    /* Compute the cofactors and solve the recursive subproblems. */
    topf = cuddI(dd,f->index);
    topg = cuddI(dd,g->index);

    if (topf <= topg) {fv = cuddT(f); fvn = cuddE(f);} else {fv = fvn = f;}
    if (topg <= topf) {gv = cuddT(g); gvn = cuddE(g);} else {gv = gvn = g;}

    if (!Cudd_EqualSupNormRel(dd,fv,gv,tolerance,pr)) return(0);
    if (!Cudd_EqualSupNormRel(dd,fvn,gvn,tolerance,pr)) return(0);

    cuddCacheInsert2(dd,(DD_CTFP)Cudd_EqualSupNormRel,f,g,DD_ONE(dd));

    return(1);

} /* end of Cudd_EqualSupNormRel */


/**
  @brief Expands cube to a prime implicant of f.

  @return the prime if successful; NULL otherwise.  In particular,
  NULL is returned if cube is not a real cube or is not an implicant
  of f.

  @sideeffect None

  @see Cudd_bddMaximallyExpand

*/
DdNode *
Cudd_bddMakePrime(
  DdManager *dd /**< manager */,
  DdNode *cube /**< cube to be expanded */,
  DdNode *f /**< function of which the cube is to be made a prime */)
{
    DdNode *res;

    if (!Cudd_bddLeq(dd,cube,f)) return(NULL);

    do {
	dd->reordered = 0;
	res = cuddBddMakePrime(dd,cube,f);
    } while (dd->reordered == 1);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }
    return(res);

} /* end of Cudd_bddMakePrime */


/**
  @brief Expands lb to prime implicants of (f and ub).

  @details Expands lb to all prime implicants of (f and ub) that
  contain lb.  Assumes that lb is contained in ub.

  @return the disjunction of the primes if lb is contained in f;
  returns the zero %BDD if lb is not contained in f; returns NULL in
  case of failure.  In particular, NULL is returned if cube is not a
  real cube or is not an implicant of f.  Returning the disjunction of
  all prime implicants works because the resulting function is unate.

  @sideeffect None

  @see Cudd_bddMakePrime

*/
DdNode *
Cudd_bddMaximallyExpand(
  DdManager *dd /**< manager */,
  DdNode *lb /**< cube to be expanded */,
  DdNode *ub /**< upper bound cube */,
  DdNode *f /**< function against which to expand */)
{
    DdNode *res;

    if (!Cudd_bddLeq(dd,lb,ub)) return(NULL);

    do {
	dd->reordered = 0;
	res = ddBddMaximallyExpand(dd,lb,ub,f);
    } while (dd->reordered == 1);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }
    return(res);

} /* end of Cudd_bddMaximallyExpand */


/**
  @brief Find a largest prime implicant of a unate function.

  @details The behavior is undefined if f is not unate.  The third
  argument is used to determine whether f is unate positive
  (increasing) or negative (decreasing) in each of the variables in
  its support.

  @return the %BDD for the prime if succesful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddMaximallyExpand

*/
DdNode *
Cudd_bddLargestPrimeUnate(
  DdManager *dd /**< manager */,
  DdNode *f /**< unate function */,
  DdNode *phaseBdd /**< cube of the phases */)
{
    DdNode *res;
    int *phases;
    int retval;
    st_table *table;

    /* Extract phase vector for quick access. */
    phases = ALLOC(int, dd->size);
    if (phases == NULL) return(NULL);
    retval = Cudd_BddToCubeArray(dd, phaseBdd, phases);
    if (retval == 0) {
        FREE(phases);
        return(NULL);
    }
    do {
        dd->reordered = 0;
        table = st_init_table(st_ptrcmp,st_ptrhash);
        if (table == NULL) {
            FREE(phases);
            return(NULL);
        }
	(void) ddBddShortestPathUnate(dd, f, phases, table);
        res = ddGetLargestCubeUnate(dd, f, phases, table);
        st_free_table(table);
    } while (dd->reordered == 1);

    FREE(phases);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }
    return(res);

} /* end of Cudd_bddLargestPrimeUnate */


/*---------------------------------------------------------------------------*/
/* Definition of internal functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Performs the recursive step of Cudd_bddMakePrime.

  @return the prime if successful; NULL otherwise.

  @sideeffect None

*/
DdNode *
cuddBddMakePrime(
  DdManager *dd /**< manager */,
  DdNode *cube /**< cube to be expanded */,
  DdNode *f /**< function of which the cube is to be made a prime */)
{
    DdNode *scan;
    DdNode *t, *e;
    DdNode *res = cube;
    DdNode *lzero = Cudd_Not(DD_ONE(dd));

    Cudd_Ref(res);
    scan = cube;
    while (!Cudd_IsConstantInt(scan)) {
	DdNode *reg = Cudd_Regular(scan);
	DdNode *var = dd->vars[reg->index];
	DdNode *expanded = Cudd_bddExistAbstract(dd,res,var);
	if (expanded == NULL) {
            Cudd_RecursiveDeref(dd,res);
	    return(NULL);
	}
	Cudd_Ref(expanded);
	if (Cudd_bddLeq(dd,expanded,f)) {
	    Cudd_RecursiveDeref(dd,res);
	    res = expanded;
	} else {
	    Cudd_RecursiveDeref(dd,expanded);
	}
	cuddGetBranches(scan,&t,&e);
	if (t == lzero) {
	    scan = e;
	} else if (e == lzero) {
	    scan = t;
	} else {
	    Cudd_RecursiveDeref(dd,res);
	    return(NULL);	/* cube is not a cube */
	}
    }

    if (scan == DD_ONE(dd)) {
	Cudd_Deref(res);
	return(res);
    } else {
	Cudd_RecursiveDeref(dd,res);
	return(NULL);
    }

} /* end of cuddBddMakePrime */


/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/


/**
  @brief Frees the entries of the visited symbol table.

  @return ST_CONTINUE.

  @sideeffect None

*/
static enum st_retval
freePathPair(
  void * key,
  void * value,
  void * arg)
{
    cuddPathPair *pair = (cuddPathPair *) value;

    (void) key; /* avoid warning */
    (void) arg; /* avoid warning */
    FREE(pair);
    return(ST_CONTINUE);

} /* end of freePathPair */


/**
  @brief Finds the length of the shortest path(s) in a %DD.

  @details Uses a local symbol table to store the lengths for each
  node. Only the lengths for the regular nodes are entered in the
  table, because those for the complement nodes are simply obtained by
  swapping the two lenghts.

  @return a pair of lengths: the length of the shortest path to 1; and
  the length of the shortest path to 0. This is done so as to take
  complement arcs into account.

  @sideeffect Accumulates the support of the %DD in support.

*/
static cuddPathPair
getShortest(
  DdManager * dd,
  DdNode * root,
  int * cost,
  int * support,
  st_table * visited)
{
    cuddPathPair *my_pair, res_pair, pair_T, pair_E;
    DdNode	*my_root, *T, *E;
    int		weight;
    DdNode	*zero = DD_ZERO(dd);

    my_root = Cudd_Regular(root);

    if (st_lookup(visited, my_root, (void **) &my_pair)) {
	if (Cudd_IsComplement(root)) {
	    res_pair.pos = my_pair->neg;
	    res_pair.neg = my_pair->pos;
	} else {
	    res_pair.pos = my_pair->pos;
	    res_pair.neg = my_pair->neg;
	}
	return(res_pair);
    }

    /* In the case of a BDD the following test is equivalent to
    ** testing whether the BDD is the constant 1. This formulation,
    ** however, works for ADDs as well, by assuming the usual
    ** dichotomy of 0 and != 0.
    */
    if (cuddIsConstant(my_root)) {
	if (my_root != zero) {
	    res_pair.pos = 0;
	    res_pair.neg = DD_BIGGY;
	} else {
	    res_pair.pos = DD_BIGGY;
	    res_pair.neg = 0;
	}
    } else {
	T = cuddT(my_root);
	E = cuddE(my_root);

	pair_T = getShortest(dd, T, cost, support, visited);
	pair_E = getShortest(dd, E, cost, support, visited);
	weight = WEIGHT(cost, my_root->index);
	res_pair.pos = ddMin(pair_T.pos+weight, pair_E.pos);
	res_pair.neg = ddMin(pair_T.neg+weight, pair_E.neg);

	/* Update support. */
	if (support != NULL) {
	    support[my_root->index] = 1;
	}
    }

    my_pair = ALLOC(cuddPathPair, 1);
    if (my_pair == NULL) {
	if (Cudd_IsComplement(root)) {
	    int tmp = res_pair.pos;
	    res_pair.pos = res_pair.neg;
	    res_pair.neg = tmp;
	}
	return(res_pair);
    }
    my_pair->pos = res_pair.pos;
    my_pair->neg = res_pair.neg;

    st_insert(visited, my_root, my_pair);
    if (Cudd_IsComplement(root)) {
	res_pair.pos = my_pair->neg;
	res_pair.neg = my_pair->pos;
    } else {
	res_pair.pos = my_pair->pos;
	res_pair.neg = my_pair->neg;
    }
    return(res_pair);

} /* end of getShortest */


/**
  @brief Build a %BDD for a shortest path of f.

  @details Given the minimum length from the root, and the minimum
  lengths for each node (in visited), apply triangulation at each
  node.  Of the two children of each node on a shortest path, at least
  one is on a shortest path. In case of ties the procedure chooses the
  THEN children.

  @return a pointer to the cube %BDD representing the path if
  successful; NULL otherwise.

  @sideeffect None

*/
static DdNode *
getPath(
  DdManager * manager,
  st_table * visited,
  DdNode * f,
  int * weight,
  int  cost)
{
    DdNode	*sol, *tmp;
    DdNode	*my_dd, *T, *E;
    cuddPathPair *T_pair, *E_pair;
    int		Tcost, Ecost;
    int		complement;

    my_dd = Cudd_Regular(f);
    complement = Cudd_IsComplement(f);

    sol = DD_ONE(manager);
    cuddRef(sol);

    while (!cuddIsConstant(my_dd)) {
	Tcost = cost - WEIGHT(weight, my_dd->index);
	Ecost = cost;

	T = cuddT(my_dd);
	E = cuddE(my_dd);

	if (complement) {T = Cudd_Not(T); E = Cudd_Not(E);}

	st_lookup(visited, Cudd_Regular(T), (void **) &T_pair);
	if ((Cudd_IsComplement(T) && T_pair->neg == Tcost) ||
	(!Cudd_IsComplement(T) && T_pair->pos == Tcost)) {
	    tmp = cuddBddAndRecur(manager,manager->vars[my_dd->index],sol);
	    if (tmp == NULL) {
		Cudd_RecursiveDeref(manager,sol);
		return(NULL);
	    }
	    cuddRef(tmp);
	    Cudd_RecursiveDeref(manager,sol);
	    sol = tmp;

	    complement =  Cudd_IsComplement(T);
	    my_dd = Cudd_Regular(T);
	    cost = Tcost;
	    continue;
	}
	st_lookup(visited, Cudd_Regular(E), (void **) &E_pair);
	if ((Cudd_IsComplement(E) && E_pair->neg == Ecost) ||
	(!Cudd_IsComplement(E) && E_pair->pos == Ecost)) {
	    tmp = cuddBddAndRecur(manager,Cudd_Not(manager->vars[my_dd->index]),sol);
	    if (tmp == NULL) {
		Cudd_RecursiveDeref(manager,sol);
		return(NULL);
	    }
	    cuddRef(tmp);
	    Cudd_RecursiveDeref(manager,sol);
	    sol = tmp;
	    complement = Cudd_IsComplement(E);
	    my_dd = Cudd_Regular(E);
	    cost = Ecost;
	    continue;
	}
	(void) fprintf(manager->err,"We shouldn't be here!!\n");
	manager->errorCode = CUDD_INTERNAL_ERROR;
	return(NULL);
    }

    cuddDeref(sol);
    return(sol);

} /* end of getPath */


/**
  @brief Finds the size of the largest cube(s) in a %DD.

  @details This problem is translated into finding the shortest paths
  from a node when both THEN and ELSE arcs have unit lengths.  Uses a
  local symbol table to store the lengths for each node. Only the
  lengths for the regular nodes are entered in the table, because
  those for the complement nodes are simply obtained by swapping the
  two lenghts.

  @return a pair of lengths: the length of the shortest path to 1; and
  the length of the shortest path to 0. This is done so as to take
  complement arcs into account.

  @sideeffect none

*/
static cuddPathPair
getLargest(
  DdManager * dd,
  DdNode * root,
  st_table * visited)
{
    cuddPathPair *my_pair, res_pair, pair_T, pair_E;
    DdNode	*my_root, *T, *E;
    DdNode	*zero = DD_ZERO(dd);

    my_root = Cudd_Regular(root);

    if (st_lookup(visited, my_root, (void **) &my_pair)) {
	if (Cudd_IsComplement(root)) {
	    res_pair.pos = my_pair->neg;
	    res_pair.neg = my_pair->pos;
	} else {
	    res_pair.pos = my_pair->pos;
	    res_pair.neg = my_pair->neg;
	}
	return(res_pair);
    }

    /* In the case of a BDD the following test is equivalent to
    ** testing whether the BDD is the constant 1. This formulation,
    ** however, works for ADDs as well, by assuming the usual
    ** dichotomy of 0 and != 0.
    */
    if (cuddIsConstant(my_root)) {
	if (my_root != zero) {
	    res_pair.pos = 0;
	    res_pair.neg = DD_BIGGY;
	} else {
	    res_pair.pos = DD_BIGGY;
	    res_pair.neg = 0;
	}
    } else {
	T = cuddT(my_root);
	E = cuddE(my_root);

	pair_T = getLargest(dd, T, visited);
	pair_E = getLargest(dd, E, visited);
	res_pair.pos = ddMin(pair_T.pos, pair_E.pos) + 1;
	res_pair.neg = ddMin(pair_T.neg, pair_E.neg) + 1;
    }

    my_pair = ALLOC(cuddPathPair, 1);
    if (my_pair == NULL) {	/* simply do not cache this result */
	if (Cudd_IsComplement(root)) {
	    int tmp = res_pair.pos;
	    res_pair.pos = res_pair.neg;
	    res_pair.neg = tmp;
	}
	return(res_pair);
    }
    my_pair->pos = res_pair.pos;
    my_pair->neg = res_pair.neg;

    /* Caching may fail without affecting correctness. */
    st_insert(visited, my_root, my_pair);
    if (Cudd_IsComplement(root)) {
	res_pair.pos = my_pair->neg;
	res_pair.neg = my_pair->pos;
    } else {
	res_pair.pos = my_pair->pos;
	res_pair.neg = my_pair->neg;
    }
    return(res_pair);

} /* end of getLargest */


/**
  @brief Build a %BDD for a largest cube of f.

  @details Given the minimum length from the root, and the minimum
  lengths for each node (in visited), apply triangulation at each
  node.  Of the two children of each node on a shortest path, at least
  one is on a shortest path. In case of ties the procedure chooses the
  THEN children.

  @return a pointer to the cube %BDD representing the path if
  successful; NULL otherwise.

  @sideeffect None

*/
static DdNode *
getCube(
  DdManager * manager,
  st_table * visited,
  DdNode * f,
  int  cost)
{
    DdNode	*sol, *tmp;
    DdNode	*my_dd, *T, *E;
    cuddPathPair *T_pair, *E_pair;
    int		Tcost, Ecost;
    int		complement;

    my_dd = Cudd_Regular(f);
    complement = Cudd_IsComplement(f);

    sol = DD_ONE(manager);
    cuddRef(sol);

    while (!cuddIsConstant(my_dd)) {
	Tcost = cost - 1;
	Ecost = cost - 1;

	T = cuddT(my_dd);
	E = cuddE(my_dd);

	if (complement) {T = Cudd_Not(T); E = Cudd_Not(E);}

	if (!st_lookup(visited, Cudd_Regular(T), (void **)&T_pair)) return(NULL);
	if ((Cudd_IsComplement(T) && T_pair->neg == Tcost) ||
	(!Cudd_IsComplement(T) && T_pair->pos == Tcost)) {
	    tmp = cuddBddAndRecur(manager,manager->vars[my_dd->index],sol);
	    if (tmp == NULL) {
		Cudd_RecursiveDeref(manager,sol);
		return(NULL);
	    }
	    cuddRef(tmp);
	    Cudd_RecursiveDeref(manager,sol);
	    sol = tmp;

	    complement =  Cudd_IsComplement(T);
	    my_dd = Cudd_Regular(T);
	    cost = Tcost;
	    continue;
	}
	if (!st_lookup(visited, Cudd_Regular(E), (void **)&E_pair)) return(NULL);
	if ((Cudd_IsComplement(E) && E_pair->neg == Ecost) ||
	(!Cudd_IsComplement(E) && E_pair->pos == Ecost)) {
	    tmp = cuddBddAndRecur(manager,Cudd_Not(manager->vars[my_dd->index]),sol);
	    if (tmp == NULL) {
		Cudd_RecursiveDeref(manager,sol);
		return(NULL);
	    }
	    cuddRef(tmp);
	    Cudd_RecursiveDeref(manager,sol);
	    sol = tmp;
	    complement = Cudd_IsComplement(E);
	    my_dd = Cudd_Regular(E);
	    cost = Ecost;
	    continue;
	}
	(void) fprintf(manager->err,"We shouldn't be here!\n");
	manager->errorCode = CUDD_INTERNAL_ERROR;
	return(NULL);
    }

    cuddDeref(sol);
    return(sol);

} /* end of getCube */


/**
  @brief Performs the recursive step of Cudd_bddMaximallyExpand.

  @details On entry to this function, ub and lb should be different
  from the zero %BDD.  The function then maintains this invariant.

  @return set of primes or zero %BDD if successful; NULL otherwise.

  @sideeffect None

*/
static DdNode *
ddBddMaximallyExpand(
  DdManager *dd /**< manager */,
  DdNode *lb /**< cube to be expanded */,
  DdNode *ub /**< upper bound cube */,
  DdNode *f /**< function against which to expand */)
{
    DdNode *lone, *lzero, *lbv, *lbvn, *lbnx, *ubv, *ubvn, *fv, *fvn, *res;
    DdNode *F, *UB, *LB, *t, *e;
    int top, toplb, topub, topf;
    unsigned int index;

    statLine(dd);
    /* Terminal cases. */
    lone = DD_ONE(dd);
    lzero = Cudd_Not(lone);
    assert(ub != lzero && lb != lzero);
    /** There are three major terminal cases in theory:
     **   ub -> f     : return ub
     **   lb == f     : return lb
     **   not(lb -> f): return zero
     ** Only the second case can be checked exactly in constant time.
     ** For the others, we check for sufficient conditions.
     */
    if (ub == f || f == lone) return(ub);
    if (lb == f) return(lb);
    if (f == lzero || ub == Cudd_Not(f) || lb == lone || lb == Cudd_Not(f))
        return(lzero);
    if (!Cudd_IsComplement(lb) && Cudd_IsComplement(f)) return(lzero);

    /* Here lb and f are not constant. */

    /* Check cache.  Since lb and ub are cubes, their local reference counts
    ** are always 1.  Hence, we only check the reference count of f.
    */
    F = Cudd_Regular(f);
    if (F->ref != 1) {
        DdNode *tmp = cuddCacheLookup(dd, DD_BDD_MAX_EXP_TAG, lb, ub, f);
        if (tmp != NULL) {
            return(tmp);
        }
    }

    checkWhetherToGiveUp(dd);

    /* Compute cofactors.  For lb we use the non-zero one in
    ** both branches of the recursion.
    */
    LB = Cudd_Regular(lb);
    UB = Cudd_Regular(ub);
    topf = dd->perm[F->index];
    toplb = dd->perm[LB->index];
    topub = (ub == lone) ? CUDD_CONST_INDEX : (unsigned int) dd->perm[UB->index];
    assert(toplb <= topub);
    top = ddMin(topf,toplb);
    if (toplb == top) {
	index = LB->index;
        lbv = cuddT(LB);
        lbvn = cuddE(LB);
        if (lb != LB) {
            lbv = Cudd_Not(lbv);
            lbvn = Cudd_Not(lbvn);
        }
        if (lbv == lzero) {
            lbnx = lbvn;
        } else {
            lbnx = lbv;
        }
    } else {
	index = F->index;
        lbnx = lbv = lbvn = lb;
    }
    if (topub == top) {
        ubv = cuddT(UB);
        ubvn = cuddE(UB);
        if (ub != UB) {
            ubv = Cudd_Not(ubv);
            ubvn = Cudd_Not(ubvn);
        }
    } else {
        ubv = ubvn = ub;
    }
    if (topf == top) {
        fv = cuddT(F);
        fvn = cuddE(F);
        if (f != F) {
            fv = Cudd_Not(fv);
            fvn = Cudd_Not(fvn);
        }
    } else {
        fv = fvn = f;
    }

    /* Recursive calls. */
    if (ubv != lzero) {
        t = ddBddMaximallyExpand(dd, lbnx, ubv, fv);
        if (t == NULL) return(NULL);
    } else {
        assert(topub == toplb && topub == top && lbv == lzero);
        t = lzero;
    }
    cuddRef(t);

    /* If the top variable appears only in lb, the positive and negative
    ** cofactors of each operand are the same.  We want to avoid a
    ** needless recursive call, which would force us to give up the
    ** cache optimization trick based on reference counts.
    */
    if (ubv == ubvn && fv == fvn) {
        res = t;
    } else {
        if (ubvn != lzero) {
            e = ddBddMaximallyExpand(dd, lbnx, ubvn, fvn);
            if (e == NULL) {
                Cudd_IterDerefBdd(dd,t);
                return(NULL);
            }
        } else {
            assert(topub == toplb && topub == top && lbvn == lzero);
            e = lzero;
        }

        if (t == e) {
            res = t;
        } else {
            cuddRef(e);

            if (toplb == top) {
                if (lbv == lzero) {
                    /* Top variable appears in negative phase. */
                    if (t != lone) {
                        DdNode *newT;
                        if (Cudd_IsComplement(t)) {
                            newT = cuddUniqueInter(dd, index, Cudd_Not(t), lzero);
                            if (newT == NULL) {
                                Cudd_IterDerefBdd(dd,t);
                                Cudd_IterDerefBdd(dd,e);
                                return(NULL);
                            }
                            newT = Cudd_Not(newT);
                        } else {
                            newT = cuddUniqueInter(dd, index, t, lone);
                            if (newT == NULL) {
                                Cudd_IterDerefBdd(dd,t);
                                Cudd_IterDerefBdd(dd,e);
                                return(NULL);
                            }
                        }
                        cuddRef(newT);
                        cuddDeref(t);
                        t = newT;
                    }
                } else if (lbvn == lzero) {
                    /* Top variable appears in positive phase. */
                    if (e != lone) {
                        DdNode *newE;
                        newE = cuddUniqueInter(dd, index, lone, e);
                        if (newE == NULL) {
                            Cudd_IterDerefBdd(dd,t);
                            Cudd_IterDerefBdd(dd,e);
                            return(NULL);
                        }
                        cuddRef(newE);
                        cuddDeref(e);
                        e = newE;
                    }
                } else {
                    /* Not a cube. */
                    Cudd_IterDerefBdd(dd,t);
                    Cudd_IterDerefBdd(dd,e);
                    return(NULL);
                }
            }

            /* Combine results. */
            res = cuddBddAndRecur(dd, t, e);
            if (res == NULL) {
                Cudd_IterDerefBdd(dd,t);
                Cudd_IterDerefBdd(dd,e);
                return(NULL);
            }
            cuddRef(res);
            Cudd_IterDerefBdd(dd,t);
            Cudd_IterDerefBdd(dd,e);
        }
    }

    /* Cache result and return. */
    if (F->ref != 1) {
        cuddCacheInsert(dd, DD_BDD_MAX_EXP_TAG, lb, ub, f, res);
    }
    cuddDeref(res);
    return(res);

} /* end of ddBddMaximallyExpand */


/**
  @brief Performs shortest path computation on a unate function.

  @details This function is based on the observation that in the %BDD
  of a unate function no node except the constant is reachable from
  the root via paths of different parity.

  @return the length of the shortest path to one if successful;
  CUDD_OUT_OF_MEM otherwise.

  @sideeffect None

  @see getShortest

*/
static int
ddBddShortestPathUnate(
  DdManager *dd,
  DdNode *f,
  int *phases,
  st_table *table)
{
    int positive, l, lT, lE;
    DdNode *lone = DD_ONE(dd);
    DdNode *lzero = Cudd_Not(lone);
    DdNode *F, *fv, *fvn;

    if (st_lookup_int(table, f, &l)) {
        return(l);
    }
    if (f == lone) {
        l = 0;
    } else if (f == lzero) {
        l = DD_BIGGY;
    } else {
        F = Cudd_Regular(f);
        fv = cuddT(F);
        fvn = cuddE(F);
        if (f != F) {
            fv = Cudd_Not(fv);
            fvn = Cudd_Not(fvn);
        }
        lT = ddBddShortestPathUnate(dd, fv, phases, table);
        lE = ddBddShortestPathUnate(dd, fvn, phases, table);
        positive = phases[F->index];
        l = positive ? ddMin(lT+1, lE) : ddMin(lT, lE+1);
    }
    if (st_insert(table, f, (void *)(ptrint) l) == ST_OUT_OF_MEM) {
        return(CUDD_OUT_OF_MEM);
    }
    return(l);

} /* end of ddShortestPathUnate */


/**
  @brief Extracts largest prime of a unate function.

  @return the %BDD of the prime if successful; NULL otherwise.

  @sideeffect None

  @see getPath

*/
static DdNode *
ddGetLargestCubeUnate(
  DdManager *dd,
  DdNode *f,
  int *phases,
  st_table *table)
{
    DdNode *res, *scan;
    int cost;

    res = DD_ONE(dd);
    cuddRef(res);
    scan = f;
    st_lookup_int(table, scan, &cost);

    while (!Cudd_IsConstantInt(scan)) {
        int Pcost, Ncost, Tcost;
        DdNode *tmp, *T, *E;
        DdNode *rscan = Cudd_Regular(scan);
        unsigned int index = rscan->index;
        assert(phases[index] == 0 || phases[index] == 1);
        int positive = phases[index] == 1;
        Pcost = positive ? cost - 1 : cost;
        Ncost = positive ? cost : cost - 1;
        T = cuddT(rscan);
        E = cuddE(rscan);
        if (rscan != scan) {
            T = Cudd_Not(T);
            E = Cudd_Not(E);
        }
        tmp = res;
        st_lookup_int(table, T, &Tcost);
        if (Tcost == Pcost) {
            cost = Pcost;
            scan = T;
            if (positive) {
                tmp = cuddBddAndRecur(dd, dd->vars[index], res);
            }
        } else {
            cost = Ncost;
            scan = E;
            if (!positive) {
                tmp = cuddBddAndRecur(dd, Cudd_Not(dd->vars[index]), res);
            }
        }
        if (tmp == NULL) {
            Cudd_IterDerefBdd(dd, res);
            return(NULL);
        }
        cuddRef(tmp);
        Cudd_IterDerefBdd(dd, res);
        res = tmp;
    }

    cuddDeref(res);
    return(res);

} /* end of ddGetLargestCubeUnate */
