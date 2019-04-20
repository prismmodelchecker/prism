/**
  @file

  @ingroup cudd

  @brief Utility functions.

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

#include <stddef.h>
#include <float.h>
#include "util.h"
#include "epdInt.h"
#include "cuddInt.h"

/*---------------------------------------------------------------------------*/
/* Constant declarations                                                     */
/*---------------------------------------------------------------------------*/

/* Random generator constants. */
#define MODULUS1 2147483563
#define LEQA1 40014
#define LEQQ1 53668
#define LEQR1 12211
#define MODULUS2 2147483399
#define LEQA2 40692
#define LEQQ2 52774
#define LEQR2 3791
#define STAB_DIV (1 + (MODULUS1 - 1) / STAB_SIZE)

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

#define bang(f)	((Cudd_IsComplement(f)) ? '!' : ' ')

/** \cond */

/*---------------------------------------------------------------------------*/
/* Static function prototypes                                                */
/*---------------------------------------------------------------------------*/

static int dp2 (DdManager *dd, DdNode *f, st_table *t);
static void ddPrintMintermAux (DdManager *dd, DdNode *node, int *list);
static int ddDagInt (DdNode *n);
static int cuddNodeArrayRecur (DdNode *f, DdNodePtr *table, int index);
static int cuddEstimateCofactor (DdManager *dd, st_table *table, DdNode * node, int i, int phase, DdNode ** ptr);
static DdNode * cuddUniqueLookup (DdManager * unique, int  index, DdNode * T, DdNode * E);
static int cuddEstimateCofactorSimple (DdNode * node, int i);
static double ddCountMintermAux (DdManager *dd, DdNode *node, double max, DdHashTable *table);
static int ddEpdCountMintermAux (DdManager const *dd, DdNode *node, EpDouble *max, EpDouble *epd, st_table *table);
static long double ddLdblCountMintermAux(DdManager const *manager, DdNode *node, long double max, st_table *table);
static double ddCountPathAux (DdNode *node, st_table *table);
static double ddCountPathsToNonZero (DdNode * N, st_table * table);
static void ddSupportStep (DdNode *f, int *support);
static void ddClearFlag (DdNode *f);
static int ddLeavesInt (DdNode *n);
static int ddPickArbitraryMinterms (DdManager *dd, DdNode *node, int nvars, int nminterms, char **string);
static int ddPickRepresentativeCube (DdManager *dd, DdNode *node, double *weight, char *string);
static enum st_retval ddEpdFree (void * key, void * value, void * arg);
static void ddFindSupport(DdManager *dd, DdNode *f, int *SP);
static void ddClearVars(DdManager *dd, int SP);
static int indexCompare(const void *a, const void *b);
static enum st_retval ddLdblFree(void * key, void * value, void * arg);
#if HAVE_POWL != 1
static long double powl(long double base, long double exponent);
#endif
/** \endcond */

/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Prints a disjoint sum of products.

  @details Prints a disjoint sum of product cover for the function
  rooted at node. Each product corresponds to a path from node to a
  leaf node different from the logical zero, and different from the
  background value. Uses the package default output file.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see Cudd_PrintDebug Cudd_bddPrintCover

*/
int
Cudd_PrintMinterm(
  DdManager * manager,
  DdNode * node)
{
    int		i, *list;

    list = ALLOC(int,manager->size);
    if (list == NULL) {
	manager->errorCode = CUDD_MEMORY_OUT;
	return(0);
    }
    for (i = 0; i < manager->size; i++) list[i] = 2;
    ddPrintMintermAux(manager,node,list);
    FREE(list);
    return(1);

} /* end of Cudd_PrintMinterm */


/**
  @brief Prints a sum of prime implicants of a %BDD.

  @details Prints a sum of product cover for an incompletely
  specified function given by a lower bound and an upper bound.  Each
  product is a prime implicant obtained by expanding the product
  corresponding to a path from node to the constant one.  Uses the
  package default output file.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see Cudd_PrintMinterm

*/
int
Cudd_bddPrintCover(
  DdManager *dd,
  DdNode *l,
  DdNode *u)
{
    int *array;
    int q, result;
    DdNode *lb;
#ifdef DD_DEBUG
    DdNode *cover;
#endif

    array = ALLOC(int, Cudd_ReadSize(dd));
    if (array == NULL) return(0);
    lb = l;
    cuddRef(lb);
#ifdef DD_DEBUG
    cover = Cudd_ReadLogicZero(dd);
    cuddRef(cover);
#endif
    while (lb != Cudd_ReadLogicZero(dd)) {
	DdNode *implicant, *prime, *tmp;
	int length;
	implicant = Cudd_LargestCube(dd,lb,&length);
	if (implicant == NULL) {
	    Cudd_RecursiveDeref(dd,lb);
	    FREE(array);
	    return(0);
	}
	cuddRef(implicant);
	prime = Cudd_bddMakePrime(dd,implicant,u);
	if (prime == NULL) {
	    Cudd_RecursiveDeref(dd,lb);
	    Cudd_RecursiveDeref(dd,implicant);
	    FREE(array);
	    return(0);
	}
	cuddRef(prime);
	Cudd_RecursiveDeref(dd,implicant);
	tmp = Cudd_bddAnd(dd,lb,Cudd_Not(prime));
	if (tmp == NULL) {
	    Cudd_RecursiveDeref(dd,lb);
	    Cudd_RecursiveDeref(dd,prime);
	    FREE(array);
	    return(0);
	}
	cuddRef(tmp);
	Cudd_RecursiveDeref(dd,lb);
	lb = tmp;
	result = Cudd_BddToCubeArray(dd,prime,array);
	if (result == 0) {
	    Cudd_RecursiveDeref(dd,lb);
	    Cudd_RecursiveDeref(dd,prime);
	    FREE(array);
	    return(0);
	}
	for (q = 0; q < dd->size; q++) {
	    switch (array[q]) {
	    case 0:
		(void) fprintf(dd->out, "0");
		break;
	    case 1:
		(void) fprintf(dd->out, "1");
		break;
	    case 2:
		(void) fprintf(dd->out, "-");
		break;
	    default:
		(void) fprintf(dd->out, "?");
	    }
	}
	(void) fprintf(dd->out, " 1\n");
#ifdef DD_DEBUG
	tmp = Cudd_bddOr(dd,prime,cover);
	if (tmp == NULL) {
	    Cudd_RecursiveDeref(dd,cover);
	    Cudd_RecursiveDeref(dd,lb);
	    Cudd_RecursiveDeref(dd,prime);
	    FREE(array);
	    return(0);
	}
	cuddRef(tmp);
	Cudd_RecursiveDeref(dd,cover);
	cover = tmp;
#endif
	Cudd_RecursiveDeref(dd,prime);
    }
    (void) fprintf(dd->out, "\n");
    Cudd_RecursiveDeref(dd,lb);
    FREE(array);
#ifdef DD_DEBUG
    if (!Cudd_bddLeq(dd,cover,u) || !Cudd_bddLeq(dd,l,cover)) {
	Cudd_RecursiveDeref(dd,cover);
	return(0);
    }
    Cudd_RecursiveDeref(dd,cover);
#endif
    return(1);

} /* end of Cudd_bddPrintCover */


/**
  @brief Prints to the manager standard output a %DD and its statistics.

  @details The statistics include the number of nodes, the number of leaves,
  and the number of minterms. (The number of minterms is the number of
  assignments to the variables that cause the function to be different
  from the logical zero (for BDDs) and from the background value (for
  ADDs.) The statistics are printed if pr &gt; 0. Specifically:
  <ul>
  <li> pr = 0 : prints nothing
  <li> pr = 1 : prints counts of nodes and minterms
  <li> pr = 2 : prints counts + disjoint sum of product
  <li> pr = 3 : prints counts + list of nodes
  <li> pr &gt; 3 : prints counts + disjoint sum of product + list of nodes
  </ul>
  For the purpose of counting the number of minterms, the function is
  supposed to depend on n variables.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see Cudd_DagSize Cudd_CountLeaves Cudd_CountMinterm
  Cudd_PrintMinterm

*/
int
Cudd_PrintDebug(
  DdManager * dd,
  DdNode * f,
  int  n,
  int  pr)
{
    DdNode *azero, *bzero;
    int	   nodes;
    int	   leaves;
    double minterms;
    int    retval = 1;

    if (dd == NULL) {
	return(0);
    }
    if (f == NULL) {
	(void) fprintf(dd->out,": is the NULL DD\n");
	(void) fflush(dd->out);
        dd->errorCode = CUDD_INVALID_ARG;
	return(0);
    }
    azero = DD_ZERO(dd);
    bzero = Cudd_Not(DD_ONE(dd));
    if ((f == azero || f == bzero) && pr > 0){
       (void) fprintf(dd->out,": is the zero DD\n");
       (void) fflush(dd->out);
       return(1);
    }
    if (pr > 0) {
	nodes = Cudd_DagSize(f);
	if (nodes == CUDD_OUT_OF_MEM) retval = 0;
	leaves = Cudd_CountLeaves(f);
	if (leaves == CUDD_OUT_OF_MEM) retval = 0;
	minterms = Cudd_CountMinterm(dd, f, n);
	if (minterms == (double)CUDD_OUT_OF_MEM) {
            retval = 0;
            (void) fprintf(dd->out,": %d nodes %d leaves unknown minterms\n",
                           nodes, leaves);
        } else {
            (void) fprintf(dd->out,": %d nodes %d leaves %g minterms\n",
                           nodes, leaves, minterms);
        }
	if (pr > 2) {
	    if (!cuddP(dd, f)) retval = 0;
	}
	if (pr == 2 || pr > 3) {
	    if (!Cudd_PrintMinterm(dd,f)) retval = 0;
	    (void) fprintf(dd->out,"\n");
	}
	(void) fflush(dd->out);
    }
    return(retval);

} /* end of Cudd_PrintDebug */


/**
  @brief Prints a one-line summary of an %ADD or %BDD to the manager stdout.

  @details The summary includes the number of nodes, the number of leaves,
  and the number of minterms.  The number of minterms is computed with
  arbitrary precision unlike Cudd_PrintDebug().  For the purpose of counting
  minterms, the function `f` is supposed to depend on `n` variables.

  @return 1 if successful; 0 otherwise.

  @see Cudd_PrintDebug Cudd_ApaPrintMinterm Cudd_ApaPrintMintermExp
*/
int
Cudd_PrintSummary(
  DdManager * dd /**< manager */,
  DdNode * f /**< %DD to be summarized */,
  int n /**< number of variables for minterm computation */,
  int mode /**< integer (0) or exponential (1) format */)
{
    DdNode *azero, *bzero;
    int	nodes, leaves, digits;
    int retval = 1;
    DdApaNumber count;

    if (dd == NULL) {
        return(0);
    }
    if (f == NULL) {
	(void) fprintf(dd->out,": is the NULL DD\n");
	(void) fflush(dd->out);
        dd->errorCode = CUDD_INVALID_ARG;
	return(0);
    }
    azero = DD_ZERO(dd);
    bzero = Cudd_Not(DD_ONE(dd));
    if (f == azero || f == bzero){
        (void) fprintf(dd->out,": is the zero DD\n");
        (void) fflush(dd->out);
        return(1);
    }
    nodes = Cudd_DagSize(f);
    if (nodes == CUDD_OUT_OF_MEM) retval = 0;
    leaves = Cudd_CountLeaves(f);
    if (leaves == CUDD_OUT_OF_MEM) retval = 0;
    (void) fprintf(dd->out,": %d nodes %d leaves ", nodes, leaves);
    count = Cudd_ApaCountMinterm(dd, f, n, &digits);
    if (count == NULL) {
	retval = 0;
    } else if (mode) {
        if (!Cudd_ApaPrintExponential(dd->out, digits, count, 6))
            retval = 0;
    } else {
        if (!Cudd_ApaPrintDecimal(dd->out, digits, count))
            retval = 0;
    }
    FREE(count);
    (void) fprintf(dd->out, " minterms\n");
    (void) fflush(dd->out);
    return(retval);

} /* end of Cudd_PrintSummary */
  

/**
  @brief Counts the number of nodes in a %DD.

  @return the number of nodes in the graph rooted at node.

  @sideeffect None

  @see Cudd_SharingSize Cudd_PrintDebug

*/
int
Cudd_DagSize(
  DdNode * node)
{
    int	i;

    i = ddDagInt(Cudd_Regular(node));
    ddClearFlag(Cudd_Regular(node));

    return(i);

} /* end of Cudd_DagSize */


/**
  @brief Estimates the number of nodes in a cofactor of a %DD.

  @details This function uses a refinement of the algorithm of Cabodi
  et al.  (ICCAD96). The refinement allows the procedure to account
  for part of the recombination that may occur in the part of the
  cofactor above the cofactoring variable. This procedure does not
  create any new node.  It does keep a small table of results;
  therefore it may run out of memory.  If this is a concern, one
  should use Cudd_EstimateCofactorSimple, which is faster, does not
  allocate any memory, but is less accurate.

  @return an estimate of the number of nodes in a cofactor of the
  graph rooted at node with respect to the variable whose index is i.
  In case of failure, returns CUDD_OUT_OF_MEM.

  @sideeffect None

  @see Cudd_DagSize Cudd_EstimateCofactorSimple

*/
int
Cudd_EstimateCofactor(
  DdManager *dd /**< manager */,
  DdNode * f	/**< function */,
  int i		/**< index of variable */,
  int phase	/**< 1: positive; 0: negative */
  )
{
    int	val;
    DdNode *ptr;
    st_table *table;

    table = st_init_table(st_ptrcmp,st_ptrhash);
    if (table == NULL) {
        dd->errorCode = CUDD_MEMORY_OUT;
        return(CUDD_OUT_OF_MEM);
    }
    val = cuddEstimateCofactor(dd,table,Cudd_Regular(f),i,phase,&ptr);
    ddClearFlag(Cudd_Regular(f));
    st_free_table(table);
    if (val == CUDD_OUT_OF_MEM)
        dd->errorCode = CUDD_MEMORY_OUT;
    
    return(val);

} /* end of Cudd_EstimateCofactor */


/**
  @brief Estimates the number of nodes in a cofactor of a %DD.

  @details Returns an estimate of the number of nodes in the positive
  cofactor of the graph rooted at node with respect to the variable
  whose index is i.  This procedure implements with minor changes the
  algorithm of Cabodi et al.  (ICCAD96). It does not allocate any
  memory, it does not change the state of the manager, and it is
  fast. However, it has been observed to overestimate the size of the
  cofactor by as much as a factor of 2.

  @sideeffect None

  @see Cudd_DagSize

*/
int
Cudd_EstimateCofactorSimple(
  DdNode * node,
  int i)
{
    int	val;

    val = cuddEstimateCofactorSimple(Cudd_Regular(node),i);
    ddClearFlag(Cudd_Regular(node));

    return(val);

} /* end of Cudd_EstimateCofactorSimple */


/**
  @brief Counts the number of nodes in an array of DDs.

  @details Shared nodes are counted only once.

  @return the total number of nodes.

  @sideeffect None

  @see Cudd_DagSize

*/
int
Cudd_SharingSize(
  DdNode ** nodeArray,
  int  n)
{
    int	i,j;

    i = 0;
    for (j = 0; j < n; j++) {
	i += ddDagInt(Cudd_Regular(nodeArray[j]));
    }
    for (j = 0; j < n; j++) {
	ddClearFlag(Cudd_Regular(nodeArray[j]));
    }
    return(i);

} /* end of Cudd_SharingSize */


/**
  @brief Counts the minterms of an %ADD or %BDD.

  @details The function is assumed to depend on `nvars` variables. The
  minterm count is represented as a double; hence overflow is
  possible.  For functions with many variables (more than 1023 if
  floating point conforms to IEEE 754), one should consider
  Cudd_ApaCountMinterm() or Cudd_EpdCountMinterm().

  @return the number of minterms of the function rooted at node if
  successful; +infinity if the number of minterms is known to be larger
  than the maximum value representable as a double; `(double) CUDD_OUT_OF_MEM`
  otherwise.

  @sideeffect None

  @see Cudd_ApaCountMinterm Cudd_EpdCountMinterm Cudd_LdblCountMinterm
  Cudd_PrintDebug Cudd_CountPath

*/
double
Cudd_CountMinterm(
  DdManager * manager,
  DdNode * node,
  int  nvars)
{
    double	max;
    DdHashTable	*table;
    double	res;
    CUDD_VALUE_TYPE epsilon;

    /* PRISM modification:
     * Revert to CUDD 2.5.1 behaviour (unscaled max).
     * To enable CUDD 3.0.0 behaviour, define CUDD_COUNT_MINTERM_3_0_0
     */

#ifdef CUDD_COUNT_MINTERM_3_0_0
    /* Scale the maximum number of minterm.  This is done in an attempt
     * to deal with functions that depend on more than 1023, but less
     * than 2044 variables and don't have too many minterms.
     */
    max = pow(2.0,(double)(nvars + DBL_MIN_EXP));
#else  // !CUDD_COUNT_MINTERM_3_0_0
    max = pow(2.0,(double)nvars);
#endif
    if (max >= DD_PLUS_INF_VAL) {
        return((double)CUDD_OUT_OF_MEM);
    }
    table = cuddHashTableInit(manager,1,2);
    if (table == NULL) {
	return((double)CUDD_OUT_OF_MEM);
    }
    /* Temporarily set epsilon to 0 to avoid rounding errors. */
    epsilon = Cudd_ReadEpsilon(manager);
    Cudd_SetEpsilon(manager,(CUDD_VALUE_TYPE)0.0);
    res = ddCountMintermAux(manager,node,max,table);
    cuddHashTableQuit(table);
    Cudd_SetEpsilon(manager,epsilon);
#ifdef CUDD_COUNT_MINTERM_3_0_0
    if (res == (double)CUDD_OUT_OF_MEM) {
        return((double)CUDD_OUT_OF_MEM);
    } else if (res >= pow(2.0,(double)(DBL_MAX_EXP + DBL_MIN_EXP))) {
        /* Minterm count is too large to be scaled back. */
        return(DD_PLUS_INF_VAL);
    } else {
        /* Undo the scaling. */
        res *= pow(2.0,(double)-DBL_MIN_EXP);
        return(res);
    }
#else  // !CUDD_COUNT_MINTERM_3_0_0
    if (res == (double)CUDD_OUT_OF_MEM) {
        return((double)CUDD_OUT_OF_MEM);
    } else {
        return(res);
    }
#endif

} /* end of Cudd_CountMinterm */


/**
  @brief Counts the paths of a %DD.

  @details Paths to all terminal nodes are counted. The path count is
  represented as a double; hence overflow is possible.

  @return the number of paths of the function rooted at node if
  successful; `(double) CUDD_OUT_OF_MEM` otherwise.

  @sideeffect None

  @see Cudd_CountMinterm

*/
double
Cudd_CountPath(
  DdNode * node)
{

    st_table	*table;
    double	i;

    table = st_init_table(st_ptrcmp,st_ptrhash);
    if (table == NULL) {
	return((double)CUDD_OUT_OF_MEM);
    }
    i = ddCountPathAux(Cudd_Regular(node),table);
    st_foreach(table, cuddStCountfree, NULL);
    st_free_table(table);
    return(i);

} /* end of Cudd_CountPath */


/**
  @brief Counts the minterms of an %ADD or %BDD with extended range.

  @details The function is assumed to depend on `nvars` variables. The
  minterm count is represented as an `EpDouble`, to allow for any
  number of variables.

  @return 0 if successful; `CUDD_OUT_OF_MEM` otherwise.

  @sideeffect None

  @see Cudd_CountMinterm Cudd_LdblCountMinterm Cudd_ApaCountMinterm
  Cudd_PrintDebug Cudd_CountPath

*/
int
Cudd_EpdCountMinterm(
  DdManager const * manager,
  DdNode * node,
  int  nvars,
  EpDouble * epd)
{
    EpDouble	max, tmp;
    st_table	*table;
    int		status;

    EpdPow2(nvars, &max);
    table = st_init_table(st_ptrcmp, st_ptrhash);
    if (table == NULL) {
	EpdMakeZero(epd, 0);
	return(CUDD_OUT_OF_MEM);
    }
    status = ddEpdCountMintermAux(manager,Cudd_Regular(node),&max,epd,table);
    st_foreach(table, ddEpdFree, NULL);
    st_free_table(table);
    if (status == CUDD_OUT_OF_MEM) {
	EpdMakeZero(epd, 0);
	return(CUDD_OUT_OF_MEM);
    }
    if (Cudd_IsComplement(node)) {
	EpdSubtract3(&max, epd, &tmp);
	EpdCopy(&tmp, epd);
    }
    return(0);

} /* end of Cudd_EpdCountMinterm */


/**
  @brief Returns the number of minterms of aa %ADD or %BDD as a long double.

  @details On systems where double and long double are the same type,
  Cudd_CountMinterm() is preferable.  On systems where long double values
  have 15-bit exponents, this function avoids overflow for up to 16383
  variables.  It applies scaling to try to avoid overflow when the number of
  variables is larger than 16383, but smaller than 32764.

  @return The nimterm count if successful; +infinity if the number is known to
  be too large for representation as a long double;
  `(long double)CUDD_OUT_OF_MEM` otherwise. 

  @see Cudd_CountMinterm Cudd_EpdCountMinterm Cudd_ApaCountMinterm
*/
long double
Cudd_LdblCountMinterm(
  DdManager const *manager,
  DdNode *node,
  int nvars)
{
    long double max, count;
    st_table *table;

    max = powl(2.0L, (long double) (nvars+LDBL_MIN_EXP));
    if (max == HUGE_VALL) {
        return((long double)CUDD_OUT_OF_MEM);
    }
    table = st_init_table(st_ptrcmp, st_ptrhash);
    if (table == NULL) {
        return((long double)CUDD_OUT_OF_MEM);
    }
    count = ddLdblCountMintermAux(manager, Cudd_Regular(node), max, table);
    st_foreach(table, ddLdblFree, NULL);
    st_free_table(table);
    if (count == (long double)CUDD_OUT_OF_MEM) {
        return((long double)CUDD_OUT_OF_MEM);
    }
    if (Cudd_IsComplement(node)) {
        count = max - count;
    }
    if (count >= powl(2.0L, (long double)(LDBL_MAX_EXP + LDBL_MIN_EXP))) {
        /* Minterm count is too large to be scaled back. */
        return(HUGE_VALL);
    } else {
        /* Undo the scaling. */
        count *= powl(2.0L,(long double)-LDBL_MIN_EXP);
        return(count);
    }

} /* end of Cudd_LdlbCountMinterm */


/**
  @brief Prints the number of minterms of an %ADD or %BDD with extended range.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see Cudd_EpdCountMinterm Cudd_ApaPrintMintermExp

*/
int
Cudd_EpdPrintMinterm(
  DdManager const * dd,
  DdNode * node,
  int nvars)
{
    EpDouble epd;
    int ret;
    char pstring[128];

    ret = Cudd_EpdCountMinterm(dd, node, nvars, &epd);
    if (ret !=0) return(0);
    EpdGetString(&epd, pstring);
    fprintf(dd->out, "%s", pstring);
    return(1);

} /* end of Cudd_EpdPrintMinterm */


/**
  @brief Counts the paths to a non-zero terminal of a %DD.

  @details The path count is represented as a double; hence overflow is
  possible.

  @return the number of paths of the function rooted at node.

  @sideeffect None

  @see Cudd_CountMinterm Cudd_CountPath

*/
double
Cudd_CountPathsToNonZero(
  DdNode * node)
{

    st_table	*table;
    double	i;

    table = st_init_table(st_ptrcmp,st_ptrhash);
    if (table == NULL) {
	return((double)CUDD_OUT_OF_MEM);
    }
    i = ddCountPathsToNonZero(node,table);
    st_foreach(table, cuddStCountfree, NULL);
    st_free_table(table);
    return(i);

} /* end of Cudd_CountPathsToNonZero */


/**
  @brief Finds the variables on which a %DD depends.

  @return the number of variables if successful; CUDD_OUT_OF_MEM
  otherwise.

  @sideeffect The indices of the support variables are returned as
  side effects.  If the function is constant, no array is allocated.

  @see Cudd_Support Cudd_SupportIndex Cudd_VectorSupportIndices

*/
int
Cudd_SupportIndices(
  DdManager * dd /**< manager */,
  DdNode * f /**< %DD whose support is sought */,
  int **indices /**< array containing (on return) the indices */)
{
    int SP = 0;

    ddFindSupport(dd, Cudd_Regular(f), &SP);
    ddClearFlag(Cudd_Regular(f));
    ddClearVars(dd, SP);
    if (SP > 0) {
        int i;
        *indices = ALLOC(int, SP);
        if (*indices == NULL) {
            dd->errorCode = CUDD_MEMORY_OUT;
            return(CUDD_OUT_OF_MEM);
        }

        for (i = 0; i < SP; i++)
            (*indices)[i] = (int) (ptrint) dd->stack[i];

        util_qsort(*indices, SP, sizeof(int), indexCompare);
    } else {
        *indices = NULL;
    }

    return(SP);

} /* end of Cudd_SupportIndices */


/**
  @brief Finds the variables on which a %DD depends.

  @return a %BDD consisting of the product of the variables if
  successful; NULL otherwise.

  @sideeffect None

  @see Cudd_VectorSupport Cudd_ClassifySupport

*/
DdNode *
Cudd_Support(
  DdManager * dd /**< manager */,
  DdNode * f /**< %DD whose support is sought */)
{
    int	*support;
    DdNode *res;
    int j;

    int size = Cudd_SupportIndices(dd, f, &support);
    if (size == CUDD_OUT_OF_MEM)
        return(NULL);

    /* Transform support from array of indices to cube. */
    res = DD_ONE(dd);
    cuddRef(res);
    
    for (j = size - 1; j >= 0; j--) { /* for each index bottom-up (almost) */
        int index = support[j];
        DdNode *var = dd->vars[index];
        DdNode *tmp = Cudd_bddAnd(dd,res,var);
        if (tmp == NULL) {
            Cudd_RecursiveDeref(dd,res);
            FREE(support);
            return(NULL);
        }
        cuddRef(tmp);
        Cudd_RecursiveDeref(dd,res);
        res = tmp;
    }

    FREE(support);
    cuddDeref(res);
    return(res);

} /* end of Cudd_Support */


/**
  @brief Finds the variables on which a %DD depends.

  @return an index array of the variables if successful; NULL
  otherwise.  The size of the array equals the number of variables in
  the manager.  Each entry of the array is 1 if the corresponding
  variable is in the support of the %DD and 0 otherwise.

  @sideeffect None

  @see Cudd_Support Cudd_SupportIndices Cudd_ClassifySupport

*/
int *
Cudd_SupportIndex(
  DdManager * dd /**< manager */,
  DdNode * f /**< %DD whose support is sought */)
{
    int	*support;
    int	i;
    int size;

    /* Allocate and initialize support array for ddSupportStep. */
    size = ddMax(dd->size, dd->sizeZ);
    support = ALLOC(int,size);
    if (support == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return(NULL);
    }
    for (i = 0; i < size; i++) {
	support[i] = 0;
    }

    /* Compute support and clean up markers. */
    ddSupportStep(Cudd_Regular(f),support);
    ddClearFlag(Cudd_Regular(f));

    return(support);

} /* end of Cudd_SupportIndex */


/**
  @brief Counts the variables on which a %DD depends.

  @return the variables on which a %DD depends.

  @sideeffect None

  @see Cudd_Support Cudd_SupportIndices

*/
int
Cudd_SupportSize(
  DdManager * dd /**< manager */,
  DdNode * f /**< %DD whose support size is sought */)
{
    int SP = 0;

    ddFindSupport(dd, Cudd_Regular(f), &SP);
    ddClearFlag(Cudd_Regular(f));
    ddClearVars(dd, SP);

    return(SP);

} /* end of Cudd_SupportSize */


/**
  @brief Finds the variables on which a set of DDs depends.

  @details The set must contain either BDDs and ADDs, or ZDDs.

  @return the number of variables if successful; CUDD_OUT_OF_MEM
  otherwise.

  @sideeffect The indices of the support variables are returned as
  side effects.  If the function is constant, no array is allocated.

  @see Cudd_Support Cudd_SupportIndex Cudd_VectorSupportIndices

*/
int
Cudd_VectorSupportIndices(
  DdManager * dd /**< manager */,
  DdNode ** F /**< %DD whose support is sought */,
  int  n /**< size of the array */,
  int **indices /**< array containing (on return) the indices */)
{
    int i;
    int SP = 0;

    /* Compute support and clean up markers. */
    for (i = 0; i < n; i++) {
	ddFindSupport(dd, Cudd_Regular(F[i]), &SP);
    }
    for (i = 0; i < n; i++) {
	ddClearFlag(Cudd_Regular(F[i]));
    }
    ddClearVars(dd, SP);

    if (SP > 0) {
        *indices = ALLOC(int, SP);
        if (*indices == NULL) {
            dd->errorCode = CUDD_MEMORY_OUT;
            return(CUDD_OUT_OF_MEM);
        }

        for (i = 0; i < SP; i++)
            (*indices)[i] = (int) (ptrint) dd->stack[i];

        util_qsort(*indices, SP, sizeof(int), indexCompare);
    } else {
        *indices = NULL;
    }

    return(SP);

} /* end of Cudd_VectorSupportIndices */


/**
  @brief Finds the variables on which a set of DDs depends.

  @details The set must contain either BDDs and ADDs, or ZDDs.

  @return a %BDD consisting of the product of the variables if
  successful; NULL otherwise.

  @sideeffect None

  @see Cudd_Support Cudd_ClassifySupport

*/
DdNode *
Cudd_VectorSupport(
  DdManager * dd /**< manager */,
  DdNode ** F /**< array of DDs whose support is sought */,
  int  n /**< size of the array */)
{
    int	*support;
    DdNode *res;
    int	j;
    int size = Cudd_VectorSupportIndices(dd, F, n, &support);
    if (size == CUDD_OUT_OF_MEM)
        return(NULL);

    /* Transform support from array of indices to cube. */
    res = DD_ONE(dd);
    cuddRef(res);
    
    for (j = size - 1; j >= 0; j--) { /* for each index bottom-up (almost) */
        int index = support[j];
        DdNode *var = dd->vars[index];
        DdNode *tmp = Cudd_bddAnd(dd,res,var);
        if (tmp == NULL) {
            Cudd_RecursiveDeref(dd,res);
            FREE(support);
            return(NULL);
        }
        cuddRef(tmp);
        Cudd_RecursiveDeref(dd,res);
        res = tmp;
    }

    FREE(support);
    cuddDeref(res);
    return(res);

} /* end of Cudd_VectorSupport */


/**
  @brief Finds the variables on which a set of DDs depends.

  @details The set must contain either BDDs and ADDs, or ZDDs.

  @return an index array of the variables if successful; NULL
  otherwise.

  @sideeffect None

  @see Cudd_SupportIndex Cudd_VectorSupport Cudd_VectorSupportIndices

*/
int *
Cudd_VectorSupportIndex(
  DdManager * dd /**< manager */,
  DdNode ** F /**< array of DDs whose support is sought */,
  int  n /**< size of the array */)
{
    int	*support;
    int	i;
    int size;

    /* Allocate and initialize support array for ddSupportStep. */
    size = ddMax(dd->size, dd->sizeZ);
    support = ALLOC(int,size);
    if (support == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return(NULL);
    }
    for (i = 0; i < size; i++) {
	support[i] = 0;
    }

    /* Compute support and clean up markers. */
    for (i = 0; i < n; i++) {
	ddSupportStep(Cudd_Regular(F[i]),support);
    }
    for (i = 0; i < n; i++) {
	ddClearFlag(Cudd_Regular(F[i]));
    }

    return(support);

} /* end of Cudd_VectorSupportIndex */


/**
  @brief Counts the variables on which a set of DDs depends.

  @details The set must contain either BDDs and ADDs, or ZDDs.

  @return the number of variables on which a set of DDs depends.

  @sideeffect None

  @see Cudd_VectorSupport Cudd_SupportSize

*/
int
Cudd_VectorSupportSize(
  DdManager * dd /**< manager */,
  DdNode ** F /**< array of DDs whose support is sought */,
  int  n /**< size of the array */)
{
    int i;
    int SP = 0;

    /* Compute support and clean up markers. */
    for (i = 0; i < n; i++) {
	ddFindSupport(dd, Cudd_Regular(F[i]), &SP);
    }
    for (i = 0; i < n; i++) {
	ddClearFlag(Cudd_Regular(F[i]));
    }
    ddClearVars(dd, SP);

    return(SP);

} /* end of Cudd_VectorSupportSize */


/**
  @brief Classifies the variables in the support of two DDs.

  @details Classifies the variables in the support of two DDs
  <code>f</code> and <code>g</code>, depending on whether they appear
  in both DDs, only in <code>f</code>, or only in <code>g</code>.

  @return 1 if successful; 0 otherwise.

  @sideeffect The cubes of the three classes of variables are
  returned as side effects.

  @see Cudd_Support Cudd_VectorSupport

*/
int
Cudd_ClassifySupport(
  DdManager * dd /**< manager */,
  DdNode * f /**< first %DD */,
  DdNode * g /**< second %DD */,
  DdNode ** common /**< cube of shared variables */,
  DdNode ** onlyF /**< cube of variables only in f */,
  DdNode ** onlyG /**< cube of variables only in g */)
{
    int	*supportF, *supportG;
    int	fi, gi;
    int sizeF, sizeG;

    sizeF = Cudd_SupportIndices(dd, f, &supportF);
    if (sizeF == CUDD_OUT_OF_MEM)
        return(0);

    sizeG = Cudd_SupportIndices(dd, g, &supportG);
    if (sizeG == CUDD_OUT_OF_MEM) {
        FREE(supportF);
        return(0);
    }

    /* Classify variables and create cubes. This part of the procedure
    ** relies on the sorting of the indices in the two support arrays.
    */
    *common = *onlyF = *onlyG = DD_ONE(dd);
    cuddRef(*common); cuddRef(*onlyF); cuddRef(*onlyG);
    fi = sizeF - 1;
    gi = sizeG - 1;
    while (fi >= 0 || gi >= 0) {
        int indexF = fi >= 0 ? supportF[fi] : -1;
        int indexG = gi >= 0 ? supportG[gi] : -1;
        int index = ddMax(indexF, indexG);
        DdNode *var = dd->vars[index];
#ifdef DD_DEBUG
        assert(index >= 0);
#endif
        if (indexF == indexG) {
            DdNode *tmp = Cudd_bddAnd(dd,*common,var);
	    if (tmp == NULL) {
		Cudd_RecursiveDeref(dd,*common);
		Cudd_RecursiveDeref(dd,*onlyF);
		Cudd_RecursiveDeref(dd,*onlyG);
		FREE(supportF); FREE(supportG);
		return(0);
	    }
	    cuddRef(tmp);
	    Cudd_RecursiveDeref(dd,*common);
	    *common = tmp;
            fi--;
            gi--;
        } else if (index == indexF) {
	    DdNode *tmp = Cudd_bddAnd(dd,*onlyF,var);
	    if (tmp == NULL) {
		Cudd_RecursiveDeref(dd,*common);
		Cudd_RecursiveDeref(dd,*onlyF);
		Cudd_RecursiveDeref(dd,*onlyG);
		FREE(supportF); FREE(supportG);
		return(0);
	    }
	    cuddRef(tmp);
	    Cudd_RecursiveDeref(dd,*onlyF);
	    *onlyF = tmp;
            fi--;
        } else { /* index == indexG */
	    DdNode *tmp = Cudd_bddAnd(dd,*onlyG,var);
	    if (tmp == NULL) {
		Cudd_RecursiveDeref(dd,*common);
		Cudd_RecursiveDeref(dd,*onlyF);
		Cudd_RecursiveDeref(dd,*onlyG);
		FREE(supportF); FREE(supportG);
		return(0);
	    }
	    cuddRef(tmp);
	    Cudd_RecursiveDeref(dd,*onlyG);
	    *onlyG = tmp;
            gi--;
        }
    }

    FREE(supportF); FREE(supportG);
    cuddDeref(*common); cuddDeref(*onlyF); cuddDeref(*onlyG);
    return(1);

} /* end of Cudd_ClassifySupport */


/**
  @brief Counts the number of leaves in a %DD.

  @return the number of leaves in the %DD rooted at node if successful;
  CUDD_OUT_OF_MEM otherwise.

  @sideeffect None

  @see Cudd_PrintDebug

*/
int
Cudd_CountLeaves(
  DdNode * node)
{
    int	i;

    i = ddLeavesInt(Cudd_Regular(node));
    ddClearFlag(Cudd_Regular(node));
    return(i);

} /* end of Cudd_CountLeaves */


/**
  @brief Picks one on-set cube randomly from the given %DD.

  @details The cube is written into an array of characters.  The array
  must have at least as many entries as there are variables.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see Cudd_bddPickOneMinterm

*/
int
Cudd_bddPickOneCube(
  DdManager * ddm,
  DdNode * node,
  char * string)
{
    DdNode *N, *T, *E;
    DdNode *one, *bzero;
    char   dir;
    int    i;

    if (string == NULL || node == NULL) return(0);

    /* The constant 0 function has no on-set cubes. */
    one = DD_ONE(ddm);
    bzero = Cudd_Not(one);
    if (node == bzero) {
        ddm->errorCode = CUDD_INVALID_ARG;
        return(0);
    }

    for (i = 0; i < ddm->size; i++) string[i] = 2;

    for (;;) {

	if (node == one) break;

	N = Cudd_Regular(node);

	T = cuddT(N); E = cuddE(N);
	if (Cudd_IsComplement(node)) {
	    T = Cudd_Not(T); E = Cudd_Not(E);
	}
	if (T == bzero) {
	    string[N->index] = 0;
	    node = E;
	} else if (E == bzero) {
	    string[N->index] = 1;
	    node = T;
	} else {
	    dir = (char) ((Cudd_Random(ddm) & 0x2000) >> 13);
	    string[N->index] = dir;
	    node = dir ? T : E;
	}
    }
    return(1);

} /* end of Cudd_bddPickOneCube */


/**
  @brief Picks one on-set minterm randomly from the given %DD.

  @details The minterm is in terms of <code>vars</code>. The array
  <code>vars</code> should contain at least all variables in the
  support of <code>f</code>; if this condition is not met the minterm
  built by this procedure may not be contained in <code>f</code>.

  @return a pointer to the %BDD for the minterm if successful; NULL otherwise.
  There are three reasons why the procedure may fail:
  <ul>
  <li> It may run out of memory;
  <li> the function <code>f</code> may be the constant 0;
  <li> the minterm may not be contained in <code>f</code>.
  </ul>

  @sideeffect None

  @see Cudd_bddPickOneCube

*/
DdNode *
Cudd_bddPickOneMinterm(
  DdManager * dd /**< manager */,
  DdNode * f /**< function from which to pick one minterm */,
  DdNode ** vars /**< array of variables */,
  int  n /**< size of <code>vars</code> */)
{
    char *string;
    int i, size;
    int *indices;
    int result;
    DdNode *old, *neW;

    size = dd->size;
    string = ALLOC(char, size);
    if (string == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return(NULL);
    }
    indices = ALLOC(int,n);
    if (indices == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	FREE(string);
	return(NULL);
    }

    for (i = 0; i < n; i++) {
	indices[i] = vars[i]->index;
    }

    result = Cudd_bddPickOneCube(dd,f,string);
    if (result == 0) {
	FREE(string);
	FREE(indices);
	return(NULL);
    }

    /* Randomize choice for don't cares. */
    for (i = 0; i < n; i++) {
	if (string[indices[i]] == 2)
	    string[indices[i]] = (char) ((Cudd_Random(dd) & 0x20) >> 5);
    }

    /* Build result BDD. */
    old = Cudd_ReadOne(dd);
    cuddRef(old);

    for (i = n-1; i >= 0; i--) {
	neW = Cudd_bddAnd(dd,old,Cudd_NotCond(vars[i],string[indices[i]]==0));
	if (neW == NULL) {
	    FREE(string);
	    FREE(indices);
	    Cudd_RecursiveDeref(dd,old);
	    return(NULL);
	}
	cuddRef(neW);
	Cudd_RecursiveDeref(dd,old);
	old = neW;
    }

#ifdef DD_DEBUG
    /* Test. */
    if (Cudd_bddLeq(dd,old,f)) {
	cuddDeref(old);
    } else {
	Cudd_RecursiveDeref(dd,old);
	old = NULL;
    }
#else
    cuddDeref(old);
#endif

    FREE(string);
    FREE(indices);
    return(old);

}  /* end of Cudd_bddPickOneMinterm */


/**
  @brief Picks k on-set minterms evenly distributed from given %DD.

  @details The minterms are in terms of <code>vars</code>. The array
  <code>vars</code> should contain at least all variables in the
  support of <code>f</code>; if this condition is not met the minterms
  built by this procedure may not be contained in <code>f</code>.

  @return an array of BDDs for the minterms if successful; NULL otherwise.
  There are three reasons why the procedure may fail:
  <ul>
  <li> It may run out of memory;
  <li> the function <code>f</code> may be the constant 0;
  <li> the minterms may not be contained in <code>f</code>.
  </ul>

  @sideeffect None

  @see Cudd_bddPickOneMinterm Cudd_bddPickOneCube

*/
DdNode **
Cudd_bddPickArbitraryMinterms(
  DdManager * dd /**< manager */,
  DdNode * f /**< function from which to pick k minterms */,
  DdNode ** vars /**< array of variables */,
  int  n /**< size of <code>vars</code> */,
  int  k /**< number of minterms to find */)
{
    char **string;
    int i, j, l, size;
    int *indices;
    int result;
    DdNode **old, *neW;
    double minterms;
    char *saveString;
    int saveFlag, savePoint = 0, isSame;

    minterms = Cudd_CountMinterm(dd,f,n);
    if ((double)k > minterms) {
	return(NULL);
    }

    size = dd->size;
    string = ALLOC(char *, k);
    if (string == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return(NULL);
    }
    for (i = 0; i < k; i++) {
	string[i] = ALLOC(char, size + 1);
	if (string[i] == NULL) {
	    for (j = 0; j < i; j++)
		FREE(string[i]);
	    FREE(string);
	    dd->errorCode = CUDD_MEMORY_OUT;
	    return(NULL);
	}
	for (j = 0; j < size; j++) string[i][j] = '2';
	string[i][size] = '\0';
    }
    indices = ALLOC(int,n);
    if (indices == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	for (i = 0; i < k; i++)
	    FREE(string[i]);
	FREE(string);
	return(NULL);
    }

    for (i = 0; i < n; i++) {
	indices[i] = vars[i]->index;
    }

    result = ddPickArbitraryMinterms(dd,f,n,k,string);
    if (result == 0) {
	for (i = 0; i < k; i++)
	    FREE(string[i]);
	FREE(string);
	FREE(indices);
	return(NULL);
    }

    old = ALLOC(DdNode *, k);
    if (old == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	for (i = 0; i < k; i++)
	    FREE(string[i]);
	FREE(string);
	FREE(indices);
	return(NULL);
    }
    saveString = ALLOC(char, size + 1);
    if (saveString == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	for (i = 0; i < k; i++)
	    FREE(string[i]);
	FREE(string);
	FREE(indices);
	FREE(old);
	return(NULL);
    }
    saveFlag = 0;

    /* Build result BDD array. */
    for (i = 0; i < k; i++) {
	isSame = 0;
	if (!saveFlag) {
	    for (j = i + 1; j < k; j++) {
		if (strcmp(string[i], string[j]) == 0) {
		    savePoint = i;
		    strcpy(saveString, string[i]);
		    saveFlag = 1;
		    break;
		}
	    }
	} else {
	    if (strcmp(string[i], saveString) == 0) {
		isSame = 1;
	    } else {
		saveFlag = 0;
		for (j = i + 1; j < k; j++) {
		    if (strcmp(string[i], string[j]) == 0) {
			savePoint = i;
			strcpy(saveString, string[i]);
			saveFlag = 1;
			break;
		    }
		}
	    }
	}
	/* Randomize choice for don't cares. */
	for (j = 0; j < n; j++) {
	    if (string[i][indices[j]] == '2')
		string[i][indices[j]] =
		  (char) ((Cudd_Random(dd) & 0x20) ? '1' : '0');
	}

	while (isSame) {
	    isSame = 0;
	    for (j = savePoint; j < i; j++) {
		if (strcmp(string[i], string[j]) == 0) {
		    isSame = 1;
		    break;
		}
	    }
	    if (isSame) {
		strcpy(string[i], saveString);
		/* Randomize choice for don't cares. */
		for (j = 0; j < n; j++) {
		    if (string[i][indices[j]] == '2')
			string[i][indices[j]] =
			  (char) ((Cudd_Random(dd) & 0x20) ? '1' : '0');
		}
	    }
	}

	old[i] = Cudd_ReadOne(dd);
	cuddRef(old[i]);

	for (j = 0; j < n; j++) {
	    if (string[i][indices[j]] == '0') {
		neW = Cudd_bddAnd(dd,old[i],Cudd_Not(vars[j]));
	    } else {
		neW = Cudd_bddAnd(dd,old[i],vars[j]);
	    }
	    if (neW == NULL) {
		FREE(saveString);
		for (l = 0; l < k; l++)
		    FREE(string[l]);
		FREE(string);
		FREE(indices);
		for (l = 0; l <= i; l++)
		    Cudd_RecursiveDeref(dd,old[l]);
		FREE(old);
		return(NULL);
	    }
	    cuddRef(neW);
	    Cudd_RecursiveDeref(dd,old[i]);
	    old[i] = neW;
	}

	/* Test. */
	if (!Cudd_bddLeq(dd,old[i],f)) {
	    FREE(saveString);
	    for (l = 0; l < k; l++)
		FREE(string[l]);
	    FREE(string);
	    FREE(indices);
	    for (l = 0; l <= i; l++)
		Cudd_RecursiveDeref(dd,old[l]);
	    FREE(old);
	    return(NULL);
	}
    }

    FREE(saveString);
    for (i = 0; i < k; i++) {
	cuddDeref(old[i]);
	FREE(string[i]);
    }
    FREE(string);
    FREE(indices);
    return(old);

}  /* end of Cudd_bddPickArbitraryMinterms */


/**
  @brief Extracts a subset from a %BDD.

  @details Extracts a subset from a %BDD in the following procedure.
  1. Compute the weight for each mask variable by counting the number of
     minterms for both positive and negative cofactors of the %BDD with
     respect to each mask variable. (weight = # positive - # negative)
  2. Find a representative cube of the %BDD by using the weight. From the
     top variable of the %BDD, for each variable, if the weight is greater
     than 0.0, choose THEN branch, othereise ELSE branch, until meeting
     the constant 1.
  3. Quantify out the variables not in maskVars from the representative
     cube and if a variable in maskVars is don't care, replace the
     variable with a constant(1 or 0) depending on the weight.
  4. Make a subset of the %BDD by multiplying with the modified cube.

  @sideeffect None

*/
DdNode *
Cudd_SubsetWithMaskVars(
  DdManager * dd /**< manager */,
  DdNode * f /**< function from which to pick a cube */,
  DdNode ** vars /**< array of variables */,
  int  nvars /**< size of <code>vars</code> */,
  DdNode ** maskVars /**< array of variables */,
  int  mvars /**< size of <code>maskVars</code> */)
{
    double	*weight;
    char	*string;
    int		i, size;
    int		*indices, *mask;
    int		result;
    DdNode	*cube, *newCube, *subset;
    DdNode	*cof;
    DdNode	*support;
    DdNode	*zero;

    support = Cudd_Support(dd,f);
    cuddRef(support);
    Cudd_RecursiveDeref(dd,support);

    size = dd->size;

    weight = ALLOC(double,size);
    if (weight == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return(NULL);
    }
    for (i = 0; i < size; i++) {
	weight[i] = 0.0;
    }
    for (i = 0; i < mvars; i++) {
	cof = Cudd_Cofactor(dd, f, maskVars[i]);
	cuddRef(cof);
	weight[i] = Cudd_CountMinterm(dd, cof, nvars);
	Cudd_RecursiveDeref(dd,cof);

	cof = Cudd_Cofactor(dd, f, Cudd_Not(maskVars[i]));
	cuddRef(cof);
	weight[i] -= Cudd_CountMinterm(dd, cof, nvars);
	Cudd_RecursiveDeref(dd,cof);
    }

    string = ALLOC(char, size + 1);
    if (string == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	FREE(weight);
	return(NULL);
    }
    mask = ALLOC(int, size);
    if (mask == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	FREE(weight);
	FREE(string);
	return(NULL);
    }
    for (i = 0; i < size; i++) {
	string[i] = '2';
	mask[i] = 0;
    }
    string[size] = '\0';
    indices = ALLOC(int,nvars);
    if (indices == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	FREE(weight);
	FREE(string);
	FREE(mask);
	return(NULL);
    }
    for (i = 0; i < nvars; i++) {
	indices[i] = vars[i]->index;
    }

    result = ddPickRepresentativeCube(dd,f,weight,string);
    if (result == 0) {
	FREE(weight);
	FREE(string);
	FREE(mask);
	FREE(indices);
	return(NULL);
    }

    cube = Cudd_ReadOne(dd);
    cuddRef(cube);
    zero = Cudd_Not(Cudd_ReadOne(dd));
    for (i = 0; i < nvars; i++) {
	if (string[indices[i]] == '0') {
	    newCube = Cudd_bddIte(dd,cube,Cudd_Not(vars[i]),zero);
	} else if (string[indices[i]] == '1') {
	    newCube = Cudd_bddIte(dd,cube,vars[i],zero);
	} else
	    continue;
	if (newCube == NULL) {
	    FREE(weight);
	    FREE(string);
	    FREE(mask);
	    FREE(indices);
	    Cudd_RecursiveDeref(dd,cube);
	    return(NULL);
	}
	cuddRef(newCube);
	Cudd_RecursiveDeref(dd,cube);
	cube = newCube;
    }
    Cudd_RecursiveDeref(dd,cube);

    for (i = 0; i < mvars; i++) {
	mask[maskVars[i]->index] = 1;
    }
    for (i = 0; i < nvars; i++) {
	if (mask[indices[i]]) {
	    if (string[indices[i]] == '2') {
		if (weight[indices[i]] >= 0.0)
		    string[indices[i]] = '1';
		else
		    string[indices[i]] = '0';
	    }
	} else {
	    string[indices[i]] = '2';
	}
    }

    cube = Cudd_ReadOne(dd);
    cuddRef(cube);
    zero = Cudd_Not(Cudd_ReadOne(dd));

    /* Build result BDD. */
    for (i = 0; i < nvars; i++) {
	if (string[indices[i]] == '0') {
	    newCube = Cudd_bddIte(dd,cube,Cudd_Not(vars[i]),zero);
	} else if (string[indices[i]] == '1') {
	    newCube = Cudd_bddIte(dd,cube,vars[i],zero);
	} else
	    continue;
	if (newCube == NULL) {
	    FREE(weight);
	    FREE(string);
	    FREE(mask);
	    FREE(indices);
	    Cudd_RecursiveDeref(dd,cube);
	    return(NULL);
	}
	cuddRef(newCube);
	Cudd_RecursiveDeref(dd,cube);
	cube = newCube;
    }

    subset = Cudd_bddAnd(dd,f,cube);
    cuddRef(subset);
    Cudd_RecursiveDeref(dd,cube);

    /* Test. */
    if (Cudd_bddLeq(dd,subset,f)) {
	cuddDeref(subset);
    } else {
	Cudd_RecursiveDeref(dd,subset);
	subset = NULL;
    }

    FREE(weight);
    FREE(string);
    FREE(mask);
    FREE(indices);
    return(subset);

} /* end of Cudd_SubsetWithMaskVars */


/**
  @brief Finds the first cube of a decision diagram.

  @details Defines an iterator on the onset of a decision diagram
  and finds its first cube.<p>
  A cube is represented as an array of literals, which are integers in
  {0, 1, 2}; 0 represents a complemented literal, 1 represents an
  uncomplemented literal, and 2 stands for don't care. The enumeration
  produces a disjoint cover of the function associated with the diagram.
  The size of the array equals the number of variables in the manager at
  the time Cudd_FirstCube is called.<p>
  For each cube, a value is also returned. This value is always 1 for a
  %BDD, while it may be different from 1 for an %ADD.
  For BDDs, the offset is the set of cubes whose value is the logical zero.
  For ADDs, the offset is the set of cubes whose value is the
  background value. The cubes of the offset are not enumerated.

  @return a generator that contains the information necessary to
  continue the enumeration if successful; NULL otherwise.

  @sideeffect The first cube and its value are returned as side effects.

  @see Cudd_ForeachCube Cudd_NextCube Cudd_GenFree Cudd_IsGenEmpty
  Cudd_FirstNode

*/
DdGen *
Cudd_FirstCube(
  DdManager * dd,
  DdNode * f,
  int ** cube,
  CUDD_VALUE_TYPE * value)
{
    DdGen *gen;
    DdNode *top, *treg, *next, *nreg, *prev, *preg;
    int i;
    int nvars;

    /* Sanity Check. */
    if (dd == NULL || f == NULL) return(NULL);

    /* Allocate generator an initialize it. */
    gen = ALLOC(DdGen,1);
    if (gen == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return(NULL);
    }

    gen->manager = dd;
    gen->type = CUDD_GEN_CUBES;
    gen->status = CUDD_GEN_EMPTY;
    gen->gen.cubes.cube = NULL;
    gen->gen.cubes.value = DD_ZERO_VAL;
    gen->stack.sp = 0;
    gen->stack.stack = NULL;
    gen->node = NULL;

    nvars = dd->size;
    gen->gen.cubes.cube = ALLOC(int,nvars);
    if (gen->gen.cubes.cube == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	FREE(gen);
	return(NULL);
    }
    for (i = 0; i < nvars; i++) gen->gen.cubes.cube[i] = 2;

    /* The maximum stack depth is one plus the number of variables.
    ** because a path may have nodes at all levels, including the
    ** constant level.
    */
    gen->stack.stack = ALLOC(DdNodePtr, nvars+1);
    if (gen->stack.stack == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	FREE(gen->gen.cubes.cube);
	FREE(gen);
	return(NULL);
    }
    for (i = 0; i <= nvars; i++) gen->stack.stack[i] = NULL;

    /* Find the first cube of the onset. */
    gen->stack.stack[gen->stack.sp] = f; gen->stack.sp++;

    while (1) {
	top = gen->stack.stack[gen->stack.sp-1];
	treg = Cudd_Regular(top);
	if (!cuddIsConstant(treg)) {
	    /* Take the else branch first. */
	    gen->gen.cubes.cube[treg->index] = 0;
	    next = cuddE(treg);
	    if (top != treg) next = Cudd_Not(next);
	    gen->stack.stack[gen->stack.sp] = next; gen->stack.sp++;
	} else if (top == Cudd_Not(DD_ONE(dd)) || top == dd->background) {
	    /* Backtrack */
	    while (1) {
		if (gen->stack.sp == 1) {
		    /* The current node has no predecessor. */
		    gen->status = CUDD_GEN_EMPTY;
		    gen->stack.sp--;
		    goto done;
		}
		prev = gen->stack.stack[gen->stack.sp-2];
		preg = Cudd_Regular(prev);
		nreg = cuddT(preg);
		if (prev != preg) {next = Cudd_Not(nreg);} else {next = nreg;}
		if (next != top) { /* follow the then branch next */
		    gen->gen.cubes.cube[preg->index] = 1;
		    gen->stack.stack[gen->stack.sp-1] = next;
		    break;
		}
		/* Pop the stack and try again. */
		gen->gen.cubes.cube[preg->index] = 2;
		gen->stack.sp--;
		top = gen->stack.stack[gen->stack.sp-1];
	    }
	} else {
	    gen->status = CUDD_GEN_NONEMPTY;
	    gen->gen.cubes.value = cuddV(top);
	    goto done;
	}
    }

done:
    *cube = gen->gen.cubes.cube;
    *value = gen->gen.cubes.value;
    return(gen);

} /* end of Cudd_FirstCube */


/**
  @brief Generates the next cube of a decision diagram onset.

  @return 0 if the enumeration is completed; 1 otherwise.

  @sideeffect The cube and its value are returned as side effects. The
  generator is modified.

  @see Cudd_ForeachCube Cudd_FirstCube Cudd_GenFree Cudd_IsGenEmpty
  Cudd_NextNode

*/
int
Cudd_NextCube(
  DdGen * gen,
  int ** cube,
  CUDD_VALUE_TYPE * value)
{
    DdNode *top, *treg, *next, *nreg, *prev, *preg;
    DdManager *dd = gen->manager;

    /* Backtrack from previously reached terminal node. */
    while (1) {
	if (gen->stack.sp == 1) {
	    /* The current node has no predecessor. */
	    gen->status = CUDD_GEN_EMPTY;
	    gen->stack.sp--;
	    goto done;
	}
	top = gen->stack.stack[gen->stack.sp-1];
	prev = gen->stack.stack[gen->stack.sp-2];
	preg = Cudd_Regular(prev);
	nreg = cuddT(preg);
	if (prev != preg) {next = Cudd_Not(nreg);} else {next = nreg;}
	if (next != top) { /* follow the then branch next */
	    gen->gen.cubes.cube[preg->index] = 1;
	    gen->stack.stack[gen->stack.sp-1] = next;
	    break;
	}
	/* Pop the stack and try again. */
	gen->gen.cubes.cube[preg->index] = 2;
	gen->stack.sp--;
    }

    while (1) {
	top = gen->stack.stack[gen->stack.sp-1];
	treg = Cudd_Regular(top);
	if (!cuddIsConstant(treg)) {
	    /* Take the else branch first. */
	    gen->gen.cubes.cube[treg->index] = 0;
	    next = cuddE(treg);
	    if (top != treg) next = Cudd_Not(next);
	    gen->stack.stack[gen->stack.sp] = next; gen->stack.sp++;
	} else if (top == Cudd_Not(DD_ONE(dd)) || top == dd->background) {
	    /* Backtrack */
	    while (1) {
		if (gen->stack.sp == 1) {
		    /* The current node has no predecessor. */
		    gen->status = CUDD_GEN_EMPTY;
		    gen->stack.sp--;
		    goto done;
		}
		prev = gen->stack.stack[gen->stack.sp-2];
		preg = Cudd_Regular(prev);
		nreg = cuddT(preg);
		if (prev != preg) {next = Cudd_Not(nreg);} else {next = nreg;}
		if (next != top) { /* follow the then branch next */
		    gen->gen.cubes.cube[preg->index] = 1;
		    gen->stack.stack[gen->stack.sp-1] = next;
		    break;
		}
		/* Pop the stack and try again. */
		gen->gen.cubes.cube[preg->index] = 2;
		gen->stack.sp--;
		top = gen->stack.stack[gen->stack.sp-1];
	    }
	} else {
	    gen->status = CUDD_GEN_NONEMPTY;
	    gen->gen.cubes.value = cuddV(top);
	    goto done;
	}
    }

done:
    if (gen->status == CUDD_GEN_EMPTY) return(0);
    *cube = gen->gen.cubes.cube;
    *value = gen->gen.cubes.value;
    return(1);

} /* end of Cudd_NextCube */


/**
  @brief Finds the first prime of a Boolean function.

  @details@parblock
  Defines an iterator on a pair of BDDs describing a
  (possibly incompletely specified) Boolean functions and finds the
  first cube of a cover of the function.

  The two argument BDDs are the lower and upper bounds of an interval.
  It is a mistake to call this function with a lower bound that is not
  less than or equal to the upper bound.

  A cube is represented as an array of literals, which are integers in
  {0, 1, 2}; 0 represents a complemented literal, 1 represents an
  uncomplemented literal, and 2 stands for don't care. The enumeration
  produces a prime and irredundant cover of the function associated
  with the two BDDs.  The size of the array equals the number of
  variables in the manager at the time Cudd_FirstCube is called.

  This iterator can only be used on BDDs.
  @endparblock

  @return a generator that contains the information necessary to
  continue the enumeration if successful; NULL otherwise.

  @sideeffect The first cube is returned as side effect.

  @see Cudd_ForeachPrime Cudd_NextPrime Cudd_GenFree Cudd_IsGenEmpty
  Cudd_FirstCube Cudd_FirstNode

*/
DdGen *
Cudd_FirstPrime(
  DdManager *dd,
  DdNode *l,
  DdNode *u,
  int **cube)
{
    DdGen *gen;
    DdNode *implicant, *prime, *tmp;
    int length, result;

    /* Sanity Check. */
    if (dd == NULL || l == NULL || u == NULL) return(NULL);

    /* Allocate generator an initialize it. */
    gen = ALLOC(DdGen,1);
    if (gen == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return(NULL);
    }

    gen->manager = dd;
    gen->type = CUDD_GEN_PRIMES;
    gen->status = CUDD_GEN_EMPTY;
    gen->gen.primes.cube = NULL;
    gen->gen.primes.ub = u;
    gen->stack.sp = 0;
    gen->stack.stack = NULL;
    gen->node = l;
    cuddRef(l);

    gen->gen.primes.cube = ALLOC(int,dd->size);
    if (gen->gen.primes.cube == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	FREE(gen);
	return(NULL);
    }

    if (gen->node == Cudd_ReadLogicZero(dd)) {
	gen->status = CUDD_GEN_EMPTY;
    } else {
	implicant = Cudd_LargestCube(dd,gen->node,&length);
	if (implicant == NULL) {
	    Cudd_RecursiveDeref(dd,gen->node);
	    FREE(gen->gen.primes.cube);
	    FREE(gen);
	    return(NULL);
	}
	cuddRef(implicant);
	prime = Cudd_bddMakePrime(dd,implicant,gen->gen.primes.ub);
	if (prime == NULL) {
	    Cudd_RecursiveDeref(dd,gen->node);
	    Cudd_RecursiveDeref(dd,implicant);
	    FREE(gen->gen.primes.cube);
	    FREE(gen);
	    return(NULL);
	}
	cuddRef(prime);
	Cudd_RecursiveDeref(dd,implicant);
	tmp = Cudd_bddAnd(dd,gen->node,Cudd_Not(prime));
	if (tmp == NULL) {
	    Cudd_RecursiveDeref(dd,gen->node);
	    Cudd_RecursiveDeref(dd,prime);
	    FREE(gen->gen.primes.cube);
	    FREE(gen);
	    return(NULL);
	}
	cuddRef(tmp);
	Cudd_RecursiveDeref(dd,gen->node);
	gen->node = tmp;
	result = Cudd_BddToCubeArray(dd,prime,gen->gen.primes.cube);
	if (result == 0) {
	    Cudd_RecursiveDeref(dd,gen->node);
	    Cudd_RecursiveDeref(dd,prime);
	    FREE(gen->gen.primes.cube);
	    FREE(gen);
	    return(NULL);
	}
	Cudd_RecursiveDeref(dd,prime);
	gen->status = CUDD_GEN_NONEMPTY;
    }
    *cube = gen->gen.primes.cube;
    return(gen);

} /* end of Cudd_FirstPrime */


/**
  @brief Generates the next prime of a Boolean function.

  @return 0 if the enumeration is completed; 1 otherwise.

  @sideeffect The cube and is returned as side effects. The
  generator is modified.

  @see Cudd_ForeachPrime Cudd_FirstPrime Cudd_GenFree Cudd_IsGenEmpty
  Cudd_NextCube Cudd_NextNode

*/
int
Cudd_NextPrime(
  DdGen *gen,
  int **cube)
{
    DdNode *implicant, *prime, *tmp;
    DdManager *dd = gen->manager;
    int length, result;

    if (gen->node == Cudd_ReadLogicZero(dd)) {
	gen->status = CUDD_GEN_EMPTY;
    } else {
	implicant = Cudd_LargestCube(dd,gen->node,&length);
	if (implicant == NULL) {
	    gen->status = CUDD_GEN_EMPTY;
	    return(0);
	}
	cuddRef(implicant);
	prime = Cudd_bddMakePrime(dd,implicant,gen->gen.primes.ub);
	if (prime == NULL) {
	    Cudd_RecursiveDeref(dd,implicant);
	    gen->status = CUDD_GEN_EMPTY;
	    return(0);
	}
	cuddRef(prime);
	Cudd_RecursiveDeref(dd,implicant);
	tmp = Cudd_bddAnd(dd,gen->node,Cudd_Not(prime));
	if (tmp == NULL) {
	    Cudd_RecursiveDeref(dd,prime);
	    gen->status = CUDD_GEN_EMPTY;
	    return(0);
	}
	cuddRef(tmp);
	Cudd_RecursiveDeref(dd,gen->node);
	gen->node = tmp;
	result = Cudd_BddToCubeArray(dd,prime,gen->gen.primes.cube);
	if (result == 0) {
	    Cudd_RecursiveDeref(dd,prime);
	    gen->status = CUDD_GEN_EMPTY;
	    return(0);
	}
	Cudd_RecursiveDeref(dd,prime);
	gen->status = CUDD_GEN_NONEMPTY;
    }
    if (gen->status == CUDD_GEN_EMPTY) return(0);
    *cube = gen->gen.primes.cube;
    return(1);

} /* end of Cudd_NextPrime */


/**
  @brief Computes the cube of an array of %BDD variables.

  @details If non-null, the phase argument indicates which literal of
  each variable should appear in the cube. If phase\[i\] is nonzero,
  then the positive literal is used. If phase is NULL, the cube is
  positive unate.

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_addComputeCube Cudd_IndicesToCube Cudd_CubeArrayToBdd

*/
DdNode *
Cudd_bddComputeCube(
  DdManager * dd,
  DdNode ** vars,
  int * phase,
  int  n)
{
    DdNode	*cube;
    DdNode	*fn;
    int         i;

    cube = DD_ONE(dd);
    cuddRef(cube);

    for (i = n - 1; i >= 0; i--) {
	if (phase == NULL || phase[i] != 0) {
	    fn = Cudd_bddAnd(dd,vars[i],cube);
	} else {
	    fn = Cudd_bddAnd(dd,Cudd_Not(vars[i]),cube);
	}
	if (fn == NULL) {
	    Cudd_RecursiveDeref(dd,cube);
	    return(NULL);
	}
	cuddRef(fn);
	Cudd_RecursiveDeref(dd,cube);
	cube = fn;
    }
    cuddDeref(cube);

    return(cube);

}  /* end of Cudd_bddComputeCube */


/**
  @brief Computes the cube of an array of %ADD variables.

  @details If non-null, the phase argument indicates which literal of
  each variable should appear in the cube. If phase\[i\] is nonzero,
  then the positive literal is used. If phase is NULL, the cube is
  positive unate.

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect none

  @see Cudd_bddComputeCube

*/
DdNode *
Cudd_addComputeCube(
  DdManager * dd,
  DdNode ** vars,
  int * phase,
  int  n)
{
    DdNode	*cube, *azero;
    DdNode	*fn;
    int         i;

    cube = DD_ONE(dd);
    cuddRef(cube);
    azero = DD_ZERO(dd);

    for (i = n - 1; i >= 0; i--) {
	if (phase == NULL || phase[i] != 0) {
	    fn = Cudd_addIte(dd,vars[i],cube,azero);
	} else {
	    fn = Cudd_addIte(dd,vars[i],azero,cube);
	}
	if (fn == NULL) {
	    Cudd_RecursiveDeref(dd,cube);
	    return(NULL);
	}
	cuddRef(fn);
	Cudd_RecursiveDeref(dd,cube);
	cube = fn;
    }
    cuddDeref(cube);

    return(cube);

} /* end of Cudd_addComputeCube */


/**
  @brief Builds the %BDD of a cube from a positional array.

  @details The array must have one integer entry for each %BDD
  variable.  If the i-th entry is 1, the variable of index i appears
  in true form in the cube; If the i-th entry is 0, the variable of
  index i appears complemented in the cube; otherwise the variable
  does not appear in the cube.

  @return a pointer to the %BDD for the cube if successful; NULL
  otherwise.

  @sideeffect None

  @see Cudd_bddComputeCube Cudd_IndicesToCube Cudd_BddToCubeArray

*/
DdNode *
Cudd_CubeArrayToBdd(
  DdManager *dd,
  int *array)
{
    DdNode *cube, *var, *tmp;
    int i;
    int size = Cudd_ReadSize(dd);

    cube = DD_ONE(dd);
    cuddRef(cube);
    for (i = size - 1; i >= 0; i--) {
	if ((array[i] & ~1) == 0) {
	    var = Cudd_bddIthVar(dd,i);
	    tmp = Cudd_bddAnd(dd,cube,Cudd_NotCond(var,array[i]==0));
	    if (tmp == NULL) {
		Cudd_RecursiveDeref(dd,cube);
		return(NULL);
	    }
	    cuddRef(tmp);
	    Cudd_RecursiveDeref(dd,cube);
	    cube = tmp;
	}
    }
    cuddDeref(cube);
    return(cube);

} /* end of Cudd_CubeArrayToBdd */


/**
  @brief Builds a positional array from the %BDD of a cube.

  @details Array must have one entry for each %BDD variable.  The
  positional array has 1 in i-th position if the variable of index i
  appears in true form in the cube; it has 0 in i-th position if the
  variable of index i appears in complemented form in the cube;
  finally, it has 2 in i-th position if the variable of index i does
  not appear in the cube.

  @return 1 if successful (the %BDD is indeed a cube); 0 otherwise.

  @sideeffect The result is in the array passed by reference.

  @see Cudd_CubeArrayToBdd

*/
int
Cudd_BddToCubeArray(
  DdManager *dd,
  DdNode *cube,
  int *array)
{
    DdNode *scan, *t, *e;
    int i;
    int size = Cudd_ReadSize(dd);
    DdNode *lzero = Cudd_Not(DD_ONE(dd));

    for (i = size-1; i >= 0; i--) {
	array[i] = 2;
    }
    scan = cube;
    while (!Cudd_IsConstantInt(scan)) {
	unsigned int index = Cudd_Regular(scan)->index;
	cuddGetBranches(scan,&t,&e);
	if (t == lzero) {
	    array[index] = 0;
	    scan = e;
	} else if (e == lzero) {
	    array[index] = 1;
	    scan = t;
	} else {
	    return(0);	/* cube is not a cube */
	}
    }
    if (scan == lzero) {
	return(0);
    } else {
	return(1);
    }

} /* end of Cudd_BddToCubeArray */


/**
  @brief Finds the first node of a decision diagram.

  @details Defines an iterator on the nodes of a decision diagram and
  finds its first node.  The nodes are enumerated in a reverse
  topological order, so that a node is always preceded in the
  enumeration by its descendants.

  @return a generator that contains the information necessary to
  continue the enumeration if successful; NULL otherwise.

  @sideeffect The first node is returned as a side effect.

  @see Cudd_ForeachNode Cudd_NextNode Cudd_GenFree Cudd_IsGenEmpty
  Cudd_FirstCube

*/
DdGen *
Cudd_FirstNode(
  DdManager * dd,
  DdNode * f,
  DdNode ** node)
{
    DdGen *gen;
    int size;

    /* Sanity Check. */
    if (dd == NULL || f == NULL) return(NULL);

    /* Allocate generator an initialize it. */
    gen = ALLOC(DdGen,1);
    if (gen == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return(NULL);
    }

    gen->manager = dd;
    gen->type = CUDD_GEN_NODES;
    gen->status = CUDD_GEN_EMPTY;
    gen->stack.sp = 0;
    gen->node = NULL;

    /* Collect all the nodes on the generator stack for later perusal. */
    gen->stack.stack = cuddNodeArray(Cudd_Regular(f), &size);
    if (gen->stack.stack == NULL) {
	FREE(gen);
	dd->errorCode = CUDD_MEMORY_OUT;
	return(NULL);
    }
    gen->gen.nodes.size = size;

    /* Find the first node. */
    if (gen->stack.sp < gen->gen.nodes.size) {
	gen->status = CUDD_GEN_NONEMPTY;
	gen->node = gen->stack.stack[gen->stack.sp];
	*node = gen->node;
    }

    return(gen);

} /* end of Cudd_FirstNode */


/**
  @brief Finds the next node of a decision diagram.

  @return 0 if the enumeration is completed; 1 otherwise.

  @sideeffect The next node is returned as a side effect.

  @see Cudd_ForeachNode Cudd_FirstNode Cudd_GenFree Cudd_IsGenEmpty
  Cudd_NextCube

*/
int
Cudd_NextNode(
  DdGen * gen,
  DdNode ** node)
{
    /* Find the next node. */
    gen->stack.sp++;
    if (gen->stack.sp < gen->gen.nodes.size) {
	gen->node = gen->stack.stack[gen->stack.sp];
	*node = gen->node;
	return(1);
    } else {
	gen->status = CUDD_GEN_EMPTY;
	return(0);
    }

} /* end of Cudd_NextNode */


/**
  @brief Frees a CUDD generator.

  @return always 0.

  @sideeffect None

  @see Cudd_ForeachCube Cudd_ForeachNode Cudd_FirstCube Cudd_NextCube
  Cudd_FirstNode Cudd_NextNode Cudd_IsGenEmpty

*/
int
Cudd_GenFree(
  DdGen * gen)
{
    if (gen == NULL) return(0);
    switch (gen->type) {
    case CUDD_GEN_CUBES:
    case CUDD_GEN_ZDD_PATHS:
	FREE(gen->gen.cubes.cube);
	FREE(gen->stack.stack);
	break;
    case CUDD_GEN_PRIMES:
	FREE(gen->gen.primes.cube);
	Cudd_RecursiveDeref(gen->manager,gen->node);
	break;
    case CUDD_GEN_NODES:
	FREE(gen->stack.stack);
	break;
    default:
	return(0);
    }
    FREE(gen);
    return(0);

} /* end of Cudd_GenFree */


/**
  @brief Queries the status of a generator.

  @return 1 if the generator is empty or NULL; 0 otherswise.

  @sideeffect None

  @see Cudd_ForeachCube Cudd_ForeachNode Cudd_FirstCube Cudd_NextCube
  Cudd_FirstNode Cudd_NextNode Cudd_GenFree

*/
int
Cudd_IsGenEmpty(
  DdGen * gen)
{
    if (gen == NULL) return(1);
    return(gen->status == CUDD_GEN_EMPTY);

} /* end of Cudd_IsGenEmpty */


/**
  @brief Builds a cube of %BDD variables from an array of indices.

  @return a pointer to the result if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddComputeCube Cudd_CubeArrayToBdd

*/
DdNode *
Cudd_IndicesToCube(
  DdManager * dd,
  int * array,
  int  n)
{
    DdNode *cube, *tmp;
    int i;

    cube = DD_ONE(dd);
    cuddRef(cube);
    for (i = n - 1; i >= 0; i--) {
	tmp = Cudd_bddAnd(dd,Cudd_bddIthVar(dd,array[i]),cube);
	if (tmp == NULL) {
	    Cudd_RecursiveDeref(dd,cube);
	    return(NULL);
	}
	cuddRef(tmp);
	Cudd_RecursiveDeref(dd,cube);
	cube = tmp;
    }

    cuddDeref(cube);
    return(cube);

} /* end of Cudd_IndicesToCube */


/**
  @brief Prints the package version number.

  @sideeffect None

*/
void
Cudd_PrintVersion(
  FILE * fp)
{
    (void) fprintf(fp, "%s\n", CUDD_VERSION);

} /* end of Cudd_PrintVersion */


/**
  @brief Computes the average distance between adjacent nodes in the manager.

  @details Adjacent nodes are node pairs such that the second node
  is the then child, else child, or next node in the collision list.

  @sideeffect None

*/
double
Cudd_AverageDistance(
  DdManager * dd)
{
    double tetotal, nexttotal;
    double tesubtotal, nextsubtotal;
    double temeasured, nextmeasured;
    int i, j;
    int slots, nvars;
    ptrdiff_t diff;
    DdNode *scan;
    DdNodePtr *nodelist;
    DdNode *sentinel = &(dd->sentinel);

    nvars = dd->size;
    if (nvars == 0) return(0.0);

    /* Initialize totals. */
    tetotal = 0.0;
    nexttotal = 0.0;
    temeasured = 0.0;
    nextmeasured = 0.0;

    /* Scan the variable subtables. */
    for (i = 0; i < nvars; i++) {
	nodelist = dd->subtables[i].nodelist;
	tesubtotal = 0.0;
	nextsubtotal = 0.0;
	slots = dd->subtables[i].slots;
	for (j = 0; j < slots; j++) {
	    scan = nodelist[j];
	    while (scan != sentinel) {
		diff = (ptrint) scan - (ptrint) cuddT(scan);
		tesubtotal += (double) ddAbs(diff);
		diff = (ptrint) scan - (ptrint) Cudd_Regular(cuddE(scan));
		tesubtotal += (double) ddAbs(diff);
		temeasured += 2.0;
		if (scan->next != sentinel) {
		    diff = (ptrint) scan - (ptrint) scan->next;
		    nextsubtotal += (double) ddAbs(diff);
		    nextmeasured += 1.0;
		}
		scan = scan->next;
	    }
	}
	tetotal += tesubtotal;
	nexttotal += nextsubtotal;
    }

    /* Scan the constant table. */
    nodelist = dd->constants.nodelist;
    nextsubtotal = 0.0;
    slots = dd->constants.slots;
    for (j = 0; j < slots; j++) {
	scan = nodelist[j];
	while (scan != NULL) {
	    if (scan->next != NULL) {
		diff = (ptrint) scan - (ptrint) scan->next;
		nextsubtotal += (double) ddAbs(diff);
		nextmeasured += 1.0;
	    }
	    scan = scan->next;
	}
    }
    nexttotal += nextsubtotal;

    return((tetotal + nexttotal) / (temeasured + nextmeasured));

} /* end of Cudd_AverageDistance */


/**
  @brief Portable random number generator.

  @details Based on ran2 from "Numerical Recipes in C." It is a long
  period (> 2 * 10^18) random number generator of L'Ecuyer with
  Bays-Durham shuffle.  The random generator can be explicitly
  initialized by calling Cudd_Srandom. If no explicit initialization
  is performed, then the seed 1 is assumed.

  @return a long integer uniformly distributed between 0 and
  2147483561 (inclusive of the endpoint values).

  @sideeffect None

  @see Cudd_Srandom

*/
int32_t
Cudd_Random(DdManager *dd)
{
    int i;	/* index in the shuffle table */
    int32_t w;	/* work variable */

    /* dd->cuddRand == 0 if the geneartor has not been initialized yet. */
    if (dd->cuddRand == 0) Cudd_Srandom(dd,1);

    /* Compute cuddRand = (cuddRand * LEQA1) % MODULUS1 avoiding
    ** overflows by Schrage's method.
    */
    w          = dd->cuddRand / LEQQ1;
    dd->cuddRand   = LEQA1 * (dd->cuddRand - w * LEQQ1) - w * LEQR1;
    dd->cuddRand  += (dd->cuddRand < 0) * MODULUS1;

    /* Compute dd->cuddRand2 = (dd->cuddRand2 * LEQA2) % MODULUS2 avoiding
    ** overflows by Schrage's method.
    */
    w          = dd->cuddRand2 / LEQQ2;
    dd->cuddRand2  = LEQA2 * (dd->cuddRand2 - w * LEQQ2) - w * LEQR2;
    dd->cuddRand2 += (dd->cuddRand2 < 0) * MODULUS2;

    /* dd->cuddRand is shuffled with the Bays-Durham algorithm.
    ** dd->shuffleSelect and cuddRand2 are combined to generate the output.
    */

    /* Pick one element from the shuffle table; "i" will be in the range
    ** from 0 to STAB_SIZE-1.
    */
    i = (int) (dd->shuffleSelect / STAB_DIV);
    /* Mix the element of the shuffle table with the current iterate of
    ** the second sub-generator, and replace the chosen element of the
    ** shuffle table with the current iterate of the first sub-generator.
    */
    dd->shuffleSelect   = dd->shuffleTable[i] - dd->cuddRand2;
    dd->shuffleTable[i] = dd->cuddRand;
    dd->shuffleSelect  += (dd->shuffleSelect < 1) * (MODULUS1 - 1);
    /* Since dd->shuffleSelect != 0, and we want to be able to return 0,
    ** here we subtract 1 before returning.
    */
    return(dd->shuffleSelect - 1);

} /* end of Cudd_Random */


/**
  @brief Initializer for the portable random number generator.

  @details Based on ran2 in "Numerical Recipes in C." The input is the
  seed for the generator. If it is negative, its absolute value is
  taken as seed.  If it is 0, then 1 is taken as seed. The initialized
  sets up the two recurrences used to generate a long-period stream,
  and sets up the shuffle table.

  @sideeffect None

  @see Cudd_Random

*/
void
Cudd_Srandom(
  DdManager *dd,
  int32_t  seed)
{
    int32_t i;

    if (seed < 0)       dd->cuddRand = -seed;
    else if (seed == 0) dd->cuddRand = 1;
    else                dd->cuddRand = seed;
    dd->cuddRand2 = dd->cuddRand;
    /* Load the shuffle table (after 11 warm-ups). */
    for (i = 0; i < STAB_SIZE + 11; i++) {
	int32_t w;
	w = dd->cuddRand / LEQQ1;
	dd->cuddRand = LEQA1 * (dd->cuddRand - w * LEQQ1) - w * LEQR1;
	dd->cuddRand += (dd->cuddRand < 0) * MODULUS1;
	dd->shuffleTable[i % STAB_SIZE] = dd->cuddRand;
    }
    dd->shuffleSelect = dd->shuffleTable[1 % STAB_SIZE];

} /* end of Cudd_Srandom */


/**
  @brief Computes the density of a %BDD or %ADD.

  @details The density is the ratio of the number of minterms to the
  number of nodes. If 0 is passed as number of variables, the number
  of variables existing in the manager is used.

  @return the density if successful; (double) CUDD_OUT_OF_MEM
  otherwise.

  @sideeffect None

  @see Cudd_CountMinterm Cudd_DagSize

*/
double
Cudd_Density(
  DdManager * dd /**< manager */,
  DdNode * f /**< function whose density is sought */,
  int  nvars /**< size of the support of f */)
{
    double minterms;
    int nodes;
    double density;

    if (nvars == 0) nvars = dd->size;
    minterms = Cudd_CountMinterm(dd,f,nvars);
    if (minterms == (double) CUDD_OUT_OF_MEM) return(minterms);
    nodes = Cudd_DagSize(f);
    density = minterms / (double) nodes;
    return(density);

} /* end of Cudd_Density */


/**
  @brief Warns that a memory allocation failed.

  @details This function can be used as replacement of MMout_of_memory
  to prevent the safe_mem functions of the util package from exiting
  when malloc returns NULL.  One possible use is in case of
  discretionary allocations; for instance, an allocation of memory to
  enlarge the computed table.

  @sideeffect None

  @see Cudd_OutOfMemSilent Cudd_RegisterOutOfMemoryCallback

*/
void
Cudd_OutOfMem(
  size_t size /**< size of the allocation that failed */)
{
    (void) fflush(stdout);
    (void) fprintf(stderr, "\nCUDD: unable to allocate %" PRIszt " bytes\n",
                   size);

} /* end of Cudd_OutOfMem */


/**
  @brief Doesn not warn that a memory allocation failed.

  @details This function can be used as replacement of MMout_of_memory
  to prevent the safe_mem functions of the util package from exiting
  when malloc returns NULL.  One possible use is in case of
  discretionary allocations; for instance, an allocation of memory to
  enlarge the computed table.

  @sideeffect None

  @see Cudd_OutOfMem Cudd_RegisterOutOfMemoryCallback

*/
void
Cudd_OutOfMemSilent(
  size_t size /**< size of the allocation that failed */)
{
    (void) size; /* suppress warning */

} /* end of Cudd_OutOfMem */


/*---------------------------------------------------------------------------*/
/* Definition of internal functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Prints a %DD to the standard output. One line per node is
  printed.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see Cudd_PrintDebug

*/
int
cuddP(
  DdManager * dd,
  DdNode * f)
{
    int retval;
    st_table *table = st_init_table(st_ptrcmp,st_ptrhash);

    if (table == NULL) return(0);

    retval = dp2(dd,f,table);
    st_free_table(table);
    (void) fputc('\n',dd->out);
    return(retval);

} /* end of cuddP */


/**
  @brief Frees the memory used to store the minterm counts recorded
  in the visited table.

  @return ST_CONTINUE.

  @sideeffect None

*/
enum st_retval
cuddStCountfree(
  void * key,
  void * value,
  void * arg)
{
    double *d = (double *)value;

    (void) key; /* avoid warning */
    (void) arg; /* avoid warning */
    FREE(d);
    return(ST_CONTINUE);

} /* end of cuddStCountfree */


/**
  @brief Recursively collects all the nodes of a %DD in a symbol
  table.

  @details Traverses the %DD f and collects all its nodes in a
  symbol table.  f is assumed to be a regular pointer and
  cuddCollectNodes guarantees this assumption in the recursive calls.

  @return 1 in case of success; 0 otherwise.

  @sideeffect None

*/
int
cuddCollectNodes(
  DdNode * f,
  st_table * visited)
{
    DdNode	*T, *E;
    int		retval;

#ifdef DD_DEBUG
    assert(!Cudd_IsComplement(f));
#endif

    /* If already visited, nothing to do. */
    if (st_is_member(visited, f) == 1)
	return(1);

    /* Check for abnormal condition that should never happen. */
    if (f == NULL)
	return(0);

    /* Mark node as visited. */
    if (st_add_direct(visited, f, NULL) == ST_OUT_OF_MEM)
	return(0);

    /* Check terminal case. */
    if (cuddIsConstant(f))
	return(1);

    /* Recursive calls. */
    T = cuddT(f);
    retval = cuddCollectNodes(T,visited);
    if (retval != 1) return(retval);
    E = Cudd_Regular(cuddE(f));
    retval = cuddCollectNodes(E,visited);
    return(retval);

} /* end of cuddCollectNodes */


/**
  @brief Recursively collects all the nodes of a %DD in an array.

  @details Traverses the %DD f and collects all its nodes in an array.
  The caller should free the array returned by cuddNodeArray.  The
  nodes are collected in reverse topological order, so that a node is
  always preceded in the array by all its descendants.

  @return a pointer to the array of nodes in case of success; NULL
  otherwise.

  @sideeffect The number of nodes is returned as a side effect.

  @see Cudd_FirstNode

*/
DdNodePtr *
cuddNodeArray(
  DdNode *f,
  int *n)
{
    DdNodePtr *table;
    int size, retval;

    size = ddDagInt(Cudd_Regular(f));
    table = ALLOC(DdNodePtr, size);
    if (table == NULL) {
	ddClearFlag(Cudd_Regular(f));
	return(NULL);
    }

    retval = cuddNodeArrayRecur(f, table, 0);
    assert(retval == size);

    *n = size;
    return(table);

} /* cuddNodeArray */


/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/


/**
  @brief Performs the recursive step of cuddP.

  @return 1 in case of success; 0 otherwise.

  @sideeffect None

*/
static int
dp2(
  DdManager *dd,
  DdNode * f,
  st_table * t)
{
    DdNode *g, *n, *N;
    int T,E;

    if (f == NULL) {
	return(0);
    }
    g = Cudd_Regular(f);
    if (cuddIsConstant(g)) {
	(void) fprintf(dd->out,"ID = %c0x%" PRIxPTR "\tvalue = %-9g\n", bang(f),
		(ptruint) g / (ptruint) sizeof(DdNode),cuddV(g));
	return(1);
    }
    if (st_is_member(t,g) == 1) {
	return(1);
    }
    if (st_add_direct(t,g,NULL) == ST_OUT_OF_MEM)
	return(0);
#ifdef DD_STATS
    (void) fprintf(dd->out,"ID = %c0x%"PRIxPTR"\tindex = %d\tr = %d\t", bang(f),
		(ptruint) g / (ptruint) sizeof(DdNode), g->index, g->ref);
#else
    (void) fprintf(dd->out,"ID = %c0x%" PRIxPTR "\tindex = %u\t", bang(f),
		(ptruint) g / (ptruint) sizeof(DdNode),g->index);
#endif
    n = cuddT(g);
    if (cuddIsConstant(n)) {
	(void) fprintf(dd->out,"T = %-9g\t",cuddV(n));
	T = 1;
    } else {
	(void) fprintf(dd->out,"T = 0x%" PRIxPTR "\t",
                       (ptruint) n / (ptruint) sizeof(DdNode));
	T = 0;
    }

    n = cuddE(g);
    N = Cudd_Regular(n);
    if (cuddIsConstant(N)) {
	(void) fprintf(dd->out,"E = %c%-9g\n",bang(n),cuddV(N));
	E = 1;
    } else {
	(void) fprintf(dd->out,"E = %c0x%" PRIxPTR "\n",
                       bang(n), (ptruint) N/(ptruint) sizeof(DdNode));
	E = 0;
    }
    if (E == 0) {
	if (dp2(dd,N,t) == 0)
	    return(0);
    }
    if (T == 0) {
	if (dp2(dd,cuddT(g),t) == 0)
	    return(0);
    }
    return(1);

} /* end of dp2 */


/**
  @brief Performs the recursive step of Cudd_PrintMinterm.

  @sideeffect None

*/
static void
ddPrintMintermAux(
  DdManager * dd /**< manager */,
  DdNode * node /**< current node */,
  int * list /**< current recursion path */)
{
    DdNode	 *N,*Nv,*Nnv;
    int		 i,v;
    unsigned int index;

    N = Cudd_Regular(node);

    if (cuddIsConstant(N)) {
	/* Terminal case: Print one cube based on the current recursion
	** path, unless we have reached the background value (ADDs) or
	** the logical zero (BDDs).
	*/
	if (node != dd->background && node != Cudd_Not(dd->one)) {
	    for (i = 0; i < dd->size; i++) {
		v = list[i];
		if (v == 0) (void) fprintf(dd->out,"0");
		else if (v == 1) (void) fprintf(dd->out,"1");
		else (void) fprintf(dd->out,"-");
	    }
	    (void) fprintf(dd->out," % g\n", cuddV(node));
	}
    } else {
	Nv  = cuddT(N);
	Nnv = cuddE(N);
	if (Cudd_IsComplement(node)) {
	    Nv  = Cudd_Not(Nv);
	    Nnv = Cudd_Not(Nnv);
	}
	index = N->index;
	list[index] = 0;
	ddPrintMintermAux(dd,Nnv,list);
	list[index] = 1;
	ddPrintMintermAux(dd,Nv,list);
	list[index] = 2;
    }
    return;

} /* end of ddPrintMintermAux */


/**
  @brief Performs the recursive step of Cudd_DagSize.

  @return the number of nodes in the graph rooted at n.

  @sideeffect None

*/
static int
ddDagInt(
  DdNode * n)
{
    int tval, eval;

    if (Cudd_IsComplement(n->next)) {
	return(0);
    }
    n->next = Cudd_Not(n->next);
    if (cuddIsConstant(n)) {
	return(1);
    }
    tval = ddDagInt(cuddT(n));
    eval = ddDagInt(Cudd_Regular(cuddE(n)));
    return(1 + tval + eval);

} /* end of ddDagInt */


/**
  @brief Performs the recursive step of cuddNodeArray.

  @details node is supposed to be regular; the invariant is maintained
  by this procedure.

  @return an the number of nodes in the %DD.

  @sideeffect Clears the least significant bit of the next field that
  was used as visited flag by cuddNodeArrayRecur when counting the
  nodes.

*/
static int
cuddNodeArrayRecur(
  DdNode *f,
  DdNodePtr *table,
  int index)
{
    int tindex, eindex;

    if (!Cudd_IsComplement(f->next)) {
	return(index);
    }
    /* Clear visited flag. */
    f->next = Cudd_Regular(f->next);
    if (cuddIsConstant(f)) {
	table[index] = f;
	return(index + 1);
    }
    tindex = cuddNodeArrayRecur(cuddT(f), table, index);
    eindex = cuddNodeArrayRecur(Cudd_Regular(cuddE(f)), table, tindex);
    table[eindex] = f;
    return(eindex + 1);

} /* end of cuddNodeArrayRecur */


/**
  @brief Performs the recursive step of Cudd_CofactorEstimate.

  @details Uses the least significant bit of the next field as visited
  flag. node is supposed to be regular; the invariant is maintained by
  this procedure.

  @return an estimate of the number of nodes in the %DD of a cofactor
  of node.

  @sideeffect None

*/
static int
cuddEstimateCofactor(
  DdManager *dd,
  st_table *table,
  DdNode * node,
  int i,
  int phase,
  DdNode ** ptr)
{
    int tval, eval, val;
    DdNode *ptrT, *ptrE;

#ifdef DD_DEBUG
    assert(!Cudd_IsComplement(node));
#endif
    if (Cudd_IsComplement(node->next)) {
	if (!st_lookup(table, node, (void **)ptr)) {
	    if (st_add_direct(table, node, node) == ST_OUT_OF_MEM)
		return(CUDD_OUT_OF_MEM);
	    *ptr = node;
	}
	return(0);
    }
    node->next = Cudd_Not(node->next);
    if (cuddIsConstant(node)) {
	*ptr = node;
	if (st_add_direct(table, node, node) == ST_OUT_OF_MEM)
	    return(CUDD_OUT_OF_MEM);
	return(1);
    }
    if ((int) node->index == i) {
	if (phase == 1) {
	    *ptr = cuddT(node);
	    val = ddDagInt(cuddT(node));
	} else {
	    *ptr = cuddE(node);
	    val = ddDagInt(Cudd_Regular(cuddE(node)));
	}
	if (node->ref > 1) {
	    if (st_add_direct(table,node,*ptr) == ST_OUT_OF_MEM)
		return(CUDD_OUT_OF_MEM);
	}
	return(val);
    }
    if (dd->perm[node->index] > dd->perm[i]) {
	*ptr = node;
	if (node->ref > 1) {
	    if (st_add_direct(table,node,node) == ST_OUT_OF_MEM)
		return(CUDD_OUT_OF_MEM);
	}
	val = 1 + ddDagInt(cuddT(node)) + ddDagInt(Cudd_Regular(cuddE(node)));
	return(val);
    }
    tval = cuddEstimateCofactor(dd,table,cuddT(node),i,phase,&ptrT);
    if (tval == CUDD_OUT_OF_MEM) return(CUDD_OUT_OF_MEM);
    eval = cuddEstimateCofactor(dd,table,Cudd_Regular(cuddE(node)),i,
				phase,&ptrE);
    if (eval == CUDD_OUT_OF_MEM) return(CUDD_OUT_OF_MEM);
    ptrE = Cudd_NotCond(ptrE,Cudd_IsComplement(cuddE(node)));
    if (ptrT == ptrE) {		/* recombination */
	*ptr = ptrT;
	val = tval;
	if (node->ref > 1) {
	    if (st_add_direct(table,node,*ptr) == ST_OUT_OF_MEM)
		return(CUDD_OUT_OF_MEM);
	}
    } else {
        int complement = Cudd_IsComplement(ptrT);
        if (complement) {
            ptrT = Cudd_Regular(ptrT);
            ptrE = Cudd_Complement(ptrE);
        }
        if ((ptrT != cuddT(node) || ptrE != cuddE(node)) &&
            (*ptr = cuddUniqueLookup(dd,node->index,ptrT,ptrE)) != NULL) {
            if (Cudd_IsComplement((*ptr)->next)) {
                val = 0;
            } else {
                val = 1 + tval + eval;
            }
            if (node->ref > 1) {
                if (st_add_direct(table,node,*ptr) == ST_OUT_OF_MEM)
                    return(CUDD_OUT_OF_MEM);
            }
            if (complement) {
                *ptr = Cudd_Complement(*ptr);
            }
        } else {
            *ptr = node;
            val = 1 + tval + eval;
        }
    }
    return(val);

} /* end of cuddEstimateCofactor */


/**
  @brief Checks the unique table for the existence of an internal node.

  @return a pointer to the node if it is in the table; NULL otherwise.

  @sideeffect None

  @see cuddUniqueInter

*/
static DdNode *
cuddUniqueLookup(
  DdManager * unique,
  int  index,
  DdNode * T,
  DdNode * E)
{
    unsigned int posn;
    int level;
    DdNodePtr *nodelist;
    DdNode *looking;
    DdSubtable *subtable;

    if (index >= unique->size) {
	return(NULL);
    }

    level = unique->perm[index];
    subtable = &(unique->subtables[level]);

#ifdef DD_DEBUG
    assert(level < cuddI(unique,T->index));
    assert(level < cuddI(unique,Cudd_Regular(E)->index));
#endif

    posn = ddHash(T, E, subtable->shift);
    nodelist = subtable->nodelist;
    looking = nodelist[posn];

    while (T < cuddT(looking)) {
	looking = Cudd_Regular(looking->next);
    }
    while (T == cuddT(looking) && E < cuddE(looking)) {
	looking = Cudd_Regular(looking->next);
    }
    if (cuddT(looking) == T && cuddE(looking) == E) {
	return(looking);
    }

    return(NULL);

} /* end of cuddUniqueLookup */


/**
  @brief Performs the recursive step of Cudd_CofactorEstimateSimple.

  @details Uses the least significant bit of the next field as visited
  flag. node is supposed to be regular; the invariant is maintained by
  this procedure.

  @return an estimate of the number of nodes in the %DD of the positive
  cofactor of node.

  @sideeffect None

*/
static int
cuddEstimateCofactorSimple(
  DdNode * node,
  int i)
{
    int tval, eval;

    if (Cudd_IsComplement(node->next)) {
	return(0);
    }
    node->next = Cudd_Not(node->next);
    if (cuddIsConstant(node)) {
	return(1);
    }
    tval = cuddEstimateCofactorSimple(cuddT(node),i);
    if ((int) node->index == i) return(tval);
    eval = cuddEstimateCofactorSimple(Cudd_Regular(cuddE(node)),i);
    return(1 + tval + eval);

} /* end of cuddEstimateCofactorSimple */


/**
  @brief Performs the recursive step of Cudd_CountMinterm.

  @details It is based on the following identity. Let |f| be the
  number of minterms of f. Then:

      |f| = (|f0|+|f1|)/2

  where f0 and f1 are the two cofactors of f.  Does not use the
  identity |f'| = max - |f|, to minimize loss of accuracy due to
  roundoff.

  @return the number of minterms of the function rooted at node.

  @sideeffect None

*/
static double
ddCountMintermAux(
  DdManager * dd,
  DdNode * node,
  double  max,
  DdHashTable * table)
{
    DdNode	*N, *Nt, *Ne;
    double	min, minT, minE;
    DdNode	*res;

    N = Cudd_Regular(node);

    if (cuddIsConstant(N)) {
	if (node == dd->background || node == Cudd_Not(dd->one)) {
	    return(0.0);
	} else {
	    return(max);
	}
    }
    if (N->ref != 1 && (res = cuddHashTableLookup1(table,node)) != NULL) {
	min = cuddV(res);
	if (res->ref == 0) {
	    table->manager->dead++;
	    table->manager->constants.dead++;
	}
	return(min);
    }

    Nt = cuddT(N); Ne = cuddE(N);
    if (Cudd_IsComplement(node)) {
	Nt = Cudd_Not(Nt); Ne = Cudd_Not(Ne);
    }

    minT = ddCountMintermAux(dd,Nt,max,table);
    if (minT == (double)CUDD_OUT_OF_MEM) return((double)CUDD_OUT_OF_MEM);
    minT *= 0.5;
    minE = ddCountMintermAux(dd,Ne,max,table);
    if (minE == (double)CUDD_OUT_OF_MEM) return((double)CUDD_OUT_OF_MEM);
    minE *= 0.5;
    min = minT + minE;

    if (N->ref != 1) {
	ptrint fanout = (ptrint) N->ref;
	cuddSatDec(fanout);
	res = cuddUniqueConst(table->manager,min);
	if (!res) {
	    return((double)CUDD_OUT_OF_MEM);
	}
	if (!cuddHashTableInsert1(table,node,res,fanout)) {
	    cuddRef(res); Cudd_RecursiveDeref(table->manager, res);
	    return((double)CUDD_OUT_OF_MEM);
	}
    }

    return(min);

} /* end of ddCountMintermAux */


/**
  @brief Performs the recursive step of Cudd_CountPath.

  @details It is based on the following identity. Let |f| be the
  number of paths of f. Then:

      |f| = |f0|+|f1|

  where f0 and f1 are the two cofactors of f.  Uses the
  identity |f'| = |f|, to improve the utilization of the (local) cache.

  @return the number of paths of the function rooted at node.

  @sideeffect None

*/
static double
ddCountPathAux(
  DdNode * node,
  st_table * table)
{

    DdNode	*Nv, *Nnv;
    double	paths, *ppaths, paths1, paths2;
    void	*dummy;


    if (cuddIsConstant(node)) {
	return(1.0);
    }
    if (st_lookup(table, node, &dummy)) {
	paths = *(double *) dummy;
	return(paths);
    }

    Nv = cuddT(node); Nnv = cuddE(node);

    paths1 = ddCountPathAux(Nv,table);
    if (paths1 == (double)CUDD_OUT_OF_MEM) return((double)CUDD_OUT_OF_MEM);
    paths2 = ddCountPathAux(Cudd_Regular(Nnv),table);
    if (paths2 == (double)CUDD_OUT_OF_MEM) return((double)CUDD_OUT_OF_MEM);
    paths = paths1 + paths2;

    ppaths = ALLOC(double,1);
    if (ppaths == NULL) {
	return((double)CUDD_OUT_OF_MEM);
    }

    *ppaths = paths;

    if (st_add_direct(table, node, ppaths) == ST_OUT_OF_MEM) {
	FREE(ppaths);
	return((double)CUDD_OUT_OF_MEM);
    }
    return(paths);

} /* end of ddCountPathAux */


/**
  @brief Performs the recursive step of Cudd_EpdCountMinterm.

  @details It is based on the following identity. Let |f| be the
  number of minterms of f. Then:

      |f| = (|f0|+|f1|)/2

  where f0 and f1 are the two cofactors of f.  Does not use the
  identity |f'| = max - |f|, to minimize loss of accuracy due to
  roundoff.

  @return the number of minterms of the function rooted at node.

  @sideeffect None

*/
static int
ddEpdCountMintermAux(
  DdManager const * dd,
  DdNode * node,
  EpDouble * max,
  EpDouble * epd,
  st_table * table)
{
    DdNode	*Nt, *Ne;
    EpDouble	*min, minT, minE;
    EpDouble	*res;
    int		status;

    /* node is assumed to be regular */
    if (cuddIsConstant(node)) {
	if (node == dd->background) {
	    EpdMakeZero(epd, 0);
	} else {
	    EpdCopy(max, epd);
	}
	return(0);
    }
    if (node->ref != 1 && st_lookup(table, node, (void **) &res)) {
	EpdCopy(res, epd);
	return(0);
    }

    Nt = cuddT(node); Ne = cuddE(node);

    status = ddEpdCountMintermAux(dd,Nt,max,&minT,table);
    if (status == CUDD_OUT_OF_MEM) return(CUDD_OUT_OF_MEM);
    EpdMultiply(&minT, (double)0.5);
    status = ddEpdCountMintermAux(dd,Cudd_Regular(Ne),max,&minE,table);
    if (status == CUDD_OUT_OF_MEM) return(CUDD_OUT_OF_MEM);
    if (Cudd_IsComplement(Ne)) {
	EpdSubtract3(max, &minE, epd);
	EpdCopy(epd, &minE);
    }
    EpdMultiply(&minE, (double)0.5);
    EpdAdd3(&minT, &minE, epd);

    if (node->ref > 1) {
	min = EpdAlloc();
	if (!min)
	    return(CUDD_OUT_OF_MEM);
	EpdCopy(epd, min);
	if (st_insert(table, node, min) == ST_OUT_OF_MEM) {
	    EpdFree(min);
	    return(CUDD_OUT_OF_MEM);
	}
    }

    return(0);

} /* end of ddEpdCountMintermAux */


/**
  @brief Performs the recursive step of Cudd_LdblCountMinterm.

  @details It is based on the following identity. Let |f| be the
  number of minterms of f. Then:

      |f| = (|f0|+|f1|)/2

  where f0 and f1 are the two cofactors of f.  Does not use the
  identity |f'| = max - |f|, to minimize loss of accuracy due to
  roundoff.

  @return the number of minterms of the function rooted at node.

  @sideeffect None

*/
static long double
ddLdblCountMintermAux(
  DdManager const *manager,
  DdNode *node,
  long double max,
  st_table *table)
{
    DdNode *t, *e;
    long double min, minT, minE;
    long double *res;
    if (cuddIsConstant(node)) {
        if (node == manager->background) {
            return 0.0L;
        } else {
            return max;
        }
    }
    if (node->ref != 1 && st_lookup(table, node, (void **) &res)) {
        return *res;
    }

    t = cuddT(node); e = cuddE(node);

    minT = ddLdblCountMintermAux(manager, t, max, table);
    if (minT == (long double) CUDD_OUT_OF_MEM)
        return((long double) CUDD_OUT_OF_MEM);
    minT *= 0.5L;
    minE = ddLdblCountMintermAux(manager, Cudd_Regular(e), max, table);
    if (minE == (long double) CUDD_OUT_OF_MEM)
        return((long double) CUDD_OUT_OF_MEM);
    if (Cudd_IsComplement(e)) {
        minE = max - minE;
    }
    minE *= 0.5L;
    min = minT + minE;
    if (node->ref != 1) {
        res = ALLOC(long double, 1);
        if (res == NULL)
            return((long double) CUDD_OUT_OF_MEM);
        *res = min;
        if (st_insert(table, node, res) == ST_OUT_OF_MEM) {
            FREE(res);
            return((long double) CUDD_OUT_OF_MEM);
        }
    }
    return(min);

} /* end of ddLdblCountMintermAux */


/**
  @brief Performs the recursive step of Cudd_CountPathsToNonZero.

  @details It is based on the following identity. Let |f| be the
  number of paths of f. Then:

      |f| = |f0|+|f1|

  where f0 and f1 are the two cofactors of f.

  @return the number of paths of the function rooted at node.

  @sideeffect None

*/
static double
ddCountPathsToNonZero(
  DdNode * N,
  st_table * table)
{

    DdNode	*node, *Nt, *Ne;
    double	paths, *ppaths, paths1, paths2;
    void	*dummy;

    node = Cudd_Regular(N);
    if (cuddIsConstant(node)) {
	return((double) !(Cudd_IsComplement(N) || cuddV(node)==DD_ZERO_VAL));
    }
    if (st_lookup(table, N, &dummy)) {
	paths = *(double *) dummy;
	return(paths);
    }

    Nt = cuddT(node); Ne = cuddE(node);
    if (node != N) {
	Nt = Cudd_Not(Nt); Ne = Cudd_Not(Ne);
    }

    paths1 = ddCountPathsToNonZero(Nt,table);
    if (paths1 == (double)CUDD_OUT_OF_MEM) return((double)CUDD_OUT_OF_MEM);
    paths2 = ddCountPathsToNonZero(Ne,table);
    if (paths2 == (double)CUDD_OUT_OF_MEM) return((double)CUDD_OUT_OF_MEM);
    paths = paths1 + paths2;

    ppaths = ALLOC(double,1);
    if (ppaths == NULL) {
	return((double)CUDD_OUT_OF_MEM);
    }

    *ppaths = paths;

    if (st_add_direct(table, N, ppaths) == ST_OUT_OF_MEM) {
	FREE(ppaths);
	return((double)CUDD_OUT_OF_MEM);
    }
    return(paths);

} /* end of ddCountPathsToNonZero */


/**
  @brief Performs the recursive step of Cudd_Support.

  @details Performs a DFS from f. The support is accumulated in supp
  as a side effect. Uses the LSB of the then pointer as visited flag.

  @sideeffect None

  @see ddClearFlag

*/
static void
ddSupportStep(
  DdNode * f,
  int * support)
{
    if (cuddIsConstant(f) || Cudd_IsComplement(f->next))
	return;

    support[f->index] = 1;
    ddSupportStep(cuddT(f),support);
    ddSupportStep(Cudd_Regular(cuddE(f)),support);
    /* Mark as visited. */
    f->next = Cudd_Complement(f->next);

} /* end of ddSupportStep */


/**
  @brief Performs a DFS from f, clearing the LSB of the next pointers.

  @sideeffect None

  @see ddSupportStep ddFindSupport ddLeavesInt ddDagInt

*/
static void
ddClearFlag(
  DdNode * f)
{
    if (!Cudd_IsComplement(f->next)) {
	return;
    }
    /* Clear visited flag. */
    f->next = Cudd_Regular(f->next);
    if (cuddIsConstant(f)) {
	return;
    }
    ddClearFlag(cuddT(f));
    ddClearFlag(Cudd_Regular(cuddE(f)));
    return;

} /* end of ddClearFlag */


/**
  @brief Performs the recursive step of Cudd_CountLeaves.

  @return the number of leaves in the %DD rooted at n.

  @sideeffect None

  @see Cudd_CountLeaves

*/
static int
ddLeavesInt(
  DdNode * n)
{
    int tval, eval;

    if (Cudd_IsComplement(n->next)) {
	return(0);
    }
    n->next = Cudd_Not(n->next);
    if (cuddIsConstant(n)) {
	return(1);
    }
    tval = ddLeavesInt(cuddT(n));
    eval = ddLeavesInt(Cudd_Regular(cuddE(n)));
    return(tval + eval);

} /* end of ddLeavesInt */


/**
  @brief Performs the recursive step of Cudd_bddPickArbitraryMinterms.

  @return 1 if successful; 0 otherwise.

  @sideeffect none

  @see Cudd_bddPickArbitraryMinterms

*/
static int
ddPickArbitraryMinterms(
  DdManager *dd,
  DdNode *node,
  int nvars,
  int nminterms,
  char **string)
{
    DdNode *N, *T, *E;
    DdNode *one, *bzero;
    int    i, t, result;
    double min1, min2;

    if (string == NULL || node == NULL) return(0);

    /* The constant 0 function has no on-set cubes. */
    one = DD_ONE(dd);
    bzero = Cudd_Not(one);
    if (nminterms == 0 || node == bzero) return(1);
    if (node == one) {
	return(1);
    }

    N = Cudd_Regular(node);
    T = cuddT(N); E = cuddE(N);
    if (Cudd_IsComplement(node)) {
	T = Cudd_Not(T); E = Cudd_Not(E);
    }

    min1 = Cudd_CountMinterm(dd, T, nvars) / 2.0;
    if (min1 == (double)CUDD_OUT_OF_MEM) return(0);
    min2 = Cudd_CountMinterm(dd, E, nvars) / 2.0;
    if (min2 == (double)CUDD_OUT_OF_MEM) return(0);

    t = (int)((double)nminterms * min1 / (min1 + min2) + 0.5);
    for (i = 0; i < t; i++)
	string[i][N->index] = '1';
    for (i = t; i < nminterms; i++)
	string[i][N->index] = '0';

    result = ddPickArbitraryMinterms(dd,T,nvars,t,&string[0]);
    if (result == 0)
	return(0);
    result = ddPickArbitraryMinterms(dd,E,nvars,nminterms-t,&string[t]);
    return(result);

} /* end of ddPickArbitraryMinterms */


/**
  @brief Finds a representative cube of a %BDD.

  @details Finds a representative cube of a %BDD with the weight of
  each variable. From the top variable, if the weight is greater than or
  equal to 0.0, choose THEN branch unless the child is the constant 0.
  Otherwise, choose ELSE branch unless the child is the constant 0.

  @sideeffect Cudd_SubsetWithMaskVars Cudd_bddPickOneCube

*/
static int
ddPickRepresentativeCube(
  DdManager *dd,
  DdNode *node,
  double *weight,
  char *string)
{
    DdNode *N, *T, *E;
    DdNode *one, *bzero;

    if (string == NULL || node == NULL) return(0);

    /* The constant 0 function has no on-set cubes. */
    one = DD_ONE(dd);
    bzero = Cudd_Not(one);
    if (node == bzero) return(0);

    if (node == DD_ONE(dd)) return(1);

    for (;;) {
	N = Cudd_Regular(node);
	if (N == one)
	    break;
	T = cuddT(N);
	E = cuddE(N);
	if (Cudd_IsComplement(node)) {
	    T = Cudd_Not(T);
	    E = Cudd_Not(E);
	}
	if (weight[N->index] >= 0.0) {
	    if (T == bzero) {
		node = E;
		string[N->index] = '0';
	    } else {
		node = T;
		string[N->index] = '1';
	    }
	} else {
	    if (E == bzero) {
		node = T;
		string[N->index] = '1';
	    } else {
		node = E;
		string[N->index] = '0';
	    }
	}
    }
    return(1);

} /* end of ddPickRepresentativeCube */


/**
  @brief Frees the memory used to store the minterm counts recorded
  in the visited table.

  @return ST_CONTINUE.

  @sideeffect None

*/
static enum st_retval
ddEpdFree(
  void * key,
  void * value,
  void * arg)
{
    EpDouble *epd = (EpDouble *) value;

    (void) key; /* avoid warning */
    (void) arg; /* avoid warning */
    EpdFree(epd);
    return(ST_CONTINUE);

} /* end of ddEpdFree */


/**
  @brief Recursively find the support of f.

  @details This function uses the LSB of the next field of the nodes
  of f as visited flag.  It also uses the LSB of the next field of the
  variables as flag to remember whether a certain index has already
  been seen.  Finally, it uses the manager stack to record all seen
  indices.

  @sideeffect The stack pointer SP is modified by side-effect.  The next
  fields are changed and need to be reset.

*/
static void
ddFindSupport(
  DdManager *dd,
  DdNode *f,
  int *SP)
{
    unsigned int index;
    DdNode *var;

    if (cuddIsConstant(f) || Cudd_IsComplement(f->next)) {
	return;
    }

    index = f->index;
    var = dd->vars[index];
    /* It is possible that var is embedded in f.  That causes no problem,
    ** though, because if we see it after encountering another node with
    ** the same index, nothing is supposed to happen.
    */
    if (!Cudd_IsComplement(var->next)) {
        var->next = Cudd_Complement(var->next);
        dd->stack[*SP] = (DdNode *)(ptruint) index;
        (*SP)++;
    }
    ddFindSupport(dd, cuddT(f), SP);
    ddFindSupport(dd, Cudd_Regular(cuddE(f)), SP);
    /* Mark as visited. */
    f->next = Cudd_Complement(f->next);

} /* end of ddFindSupport */


/**
  @brief Clears visited flags for variables.

  @sideeffect None

*/
static void
ddClearVars(
  DdManager *dd,
  int SP)
{
    int i;

    for (i = 0; i < SP; i++) {
        int index = (int) (ptrint) dd->stack[i];
        DdNode *var = dd->vars[index];
        var->next = Cudd_Regular(var->next);
    }
    
} /* end of ddClearVars */


/**
  @brief Compares indices for qsort.

  @details Subtracting these integers cannot produce overflow, because
  they are non-negative.

  @sideeffect None

*/
static int
indexCompare(
  const void *a,
  const void *b)
{
    int ia = *(int const *) a;
    int ib = *(int const *) b;
    return(ia - ib);

} /* end of indexCompare */


/**
  @brief Frees the memory used to store the minterm counts recorded in the
  visited table by Cudd_LdblCountMinterm.

  @returns ST_CONTINUE.

  @sideeffect None
*/
static enum st_retval
ddLdblFree(
  void * key,
  void * value,
  void * arg)
{
    long double * ld = (long double *) value;

    (void) key; /* avoid warning */
    (void) arg; /* avoid warning */
    FREE(ld);
    return(ST_CONTINUE);

} /* end of ddLdblFree */


#if HAVE_POWL != 1
/**
  @brief Replacement for standard library powl.

  @details Some systems' C libraries, notably Cygwin as of 2015,
  lack an implementation of powl.  This simple-minded replacement
  works for integral powers.  It is based on iterative squaring.

  @return base raised to the exponent.
*/
static long double
powl(
  long double base,
  long double exponent)
{
    long exp;
    long double power = 1.0L, square = base;
    if (exponent < 0.0L) {
        exp = (long) -exponent;
    } else {
        exp = (long) exponent;
    }
    /* Compute base^exponent by iterative squaring.
     * The loop invariant is power * square^exp = base^exponent.
     */
    while (exp > 0) {
        if (exp & 1L)
            power *= square;
        square *= square;
        exp >>= 1L;
    }
    if (exponent < 0.0L) {
        power = 1.0L / power;
    }
    return(power);

} /* end of powl */
#endif
