/**
  @file

  @ingroup cudd

  @brief Correlation between BDDs.

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

/** Type of hash-table key. */
typedef struct hashEntry {
    DdNode *f;
    DdNode *g;
} HashEntry;


/*---------------------------------------------------------------------------*/
/* Variable declarations                                                     */
/*---------------------------------------------------------------------------*/

#ifdef CORREL_STATS
static	int	num_calls;
#endif

/*---------------------------------------------------------------------------*/
/* Macro declarations                                                        */
/*---------------------------------------------------------------------------*/

/** \cond */

/*---------------------------------------------------------------------------*/
/* Static function prototypes                                                */
/*---------------------------------------------------------------------------*/

static double bddCorrelationAux (DdManager *dd, DdNode *f, DdNode *g, st_table *table);
static double bddCorrelationWeightsAux (DdManager *dd, DdNode *f, DdNode *g, double *prob, st_table *table);
static int CorrelCompare (const void *key1, const void *key2);
static int CorrelHash (void const *key, int modulus);
static enum st_retval CorrelCleanUp (void *key, void *value, void *arg);

/** \endcond */

/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Computes the correlation of f and g.

  @details If f == g, their correlation is 1. If f == g', their
  correlation is 0.

  @return the fraction of minterms in the ON-set of the EXNOR of f and
  g.  If it runs out of memory, returns (double)CUDD_OUT_OF_MEM.

  @sideeffect None

  @see Cudd_bddCorrelationWeights

*/
double
Cudd_bddCorrelation(
  DdManager * manager,
  DdNode * f,
  DdNode * g)
{
    st_table	*table;
    double	correlation;

#ifdef CORREL_STATS
    num_calls = 0;
#endif

    table = st_init_table(CorrelCompare,CorrelHash);
    if (table == NULL) return((double)CUDD_OUT_OF_MEM);
    correlation = bddCorrelationAux(manager,f,g,table);
    st_foreach(table, CorrelCleanUp, NIL(void));
    st_free_table(table);
    return(correlation);

} /* end of Cudd_bddCorrelation */


/**
  @brief Computes the correlation of f and g for given input
  probabilities.

  @details On input, prob\[i\] is supposed to contain the probability
  of the i-th input variable to be 1.  If f == g, their correlation is
  1. If f == g', their correlation is 0.  The correlation of f and the
  constant one gives the probability of f.

  @return the probability that f and g have the same value. If it runs
  out of memory, returns (double)CUDD_OUT_OF_MEM.

  @sideeffect None

  @see Cudd_bddCorrelation

*/
double
Cudd_bddCorrelationWeights(
  DdManager * manager,
  DdNode * f,
  DdNode * g,
  double * prob)
{
    st_table	*table;
    double	correlation;

#ifdef CORREL_STATS
    num_calls = 0;
#endif

    table = st_init_table(CorrelCompare,CorrelHash);
    if (table == NULL) return((double)CUDD_OUT_OF_MEM);
    correlation = bddCorrelationWeightsAux(manager,f,g,prob,table);
    st_foreach(table, CorrelCleanUp, NIL(void));
    st_free_table(table);
    return(correlation);

} /* end of Cudd_bddCorrelationWeights */


/*---------------------------------------------------------------------------*/
/* Definition of internal functions                                          */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/


/**
  @brief Performs the recursive step of Cudd_bddCorrelation.

  @return the fraction of minterms in the ON-set of the EXNOR of f and
  g.

  @sideeffect None

  @see bddCorrelationWeightsAux

*/
static double
bddCorrelationAux(
  DdManager * dd,
  DdNode * f,
  DdNode * g,
  st_table * table)
{
    DdNode	*Fv, *Fnv, *G, *Gv, *Gnv;
    double	min, *pmin, min1, min2;
    void        *dummy;
    HashEntry	*entry;
    int topF, topG;

    statLine(dd);
#ifdef CORREL_STATS
    num_calls++;
#endif

    /* Terminal cases: only work for BDDs. */
    if (f == g) return(1.0);
    if (f == Cudd_Not(g)) return(0.0);

    /* Standardize call using the following properties:
    **     (f EXNOR g)   = (g EXNOR f)
    **     (f' EXNOR g') = (f EXNOR g).
    */
    if (f > g) {
	DdNode *tmp = f;
	f = g; g = tmp;
    }
    if (Cudd_IsComplement(f)) {
	f = Cudd_Not(f);
	g = Cudd_Not(g);
    }
    /* From now on, f is regular. */
    
    entry = ALLOC(HashEntry,1);
    if (entry == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return(CUDD_OUT_OF_MEM);
    }
    entry->f = f; entry->g = g;

    /* We do not use the fact that
    ** correlation(f,g') = 1 - correlation(f,g)
    ** to minimize the risk of cancellation.
    */
    if (st_lookup(table, entry, &dummy)) {
        min = *(double *) dummy;
	FREE(entry);
	return(min);
    }

    G = Cudd_Regular(g);
    topF = cuddI(dd,f->index); topG = cuddI(dd,G->index);
    if (topF <= topG) { Fv = cuddT(f); Fnv = cuddE(f); } else { Fv = Fnv = f; }
    if (topG <= topF) { Gv = cuddT(G); Gnv = cuddE(G); } else { Gv = Gnv = G; }

    if (g != G) {
	Gv = Cudd_Not(Gv);
	Gnv = Cudd_Not(Gnv);
    }

    min1 = bddCorrelationAux(dd, Fv, Gv, table) / 2.0;
    if (min1 == (double)CUDD_OUT_OF_MEM) {
	FREE(entry);
	return(CUDD_OUT_OF_MEM);
    }
    min2 = bddCorrelationAux(dd, Fnv, Gnv, table) / 2.0; 
    if (min2 == (double)CUDD_OUT_OF_MEM) {
	FREE(entry);
	return(CUDD_OUT_OF_MEM);
    }
    min = (min1+min2);
    
    pmin = ALLOC(double,1);
    if (pmin == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return((double)CUDD_OUT_OF_MEM);
    }
    *pmin = min;

    if (st_insert(table, entry, pmin) == ST_OUT_OF_MEM) {
	FREE(entry);
	FREE(pmin);
	return((double)CUDD_OUT_OF_MEM);
    }
    return(min);

} /* end of bddCorrelationAux */


/**
  @brief Performs the recursive step of Cudd_bddCorrelationWeigths.

  @sideeffect None

  @see bddCorrelationAux

*/
static double
bddCorrelationWeightsAux(
  DdManager * dd,
  DdNode * f,
  DdNode * g,
  double * prob,
  st_table * table)
{
    DdNode	*Fv, *Fnv, *G, *Gv, *Gnv;
    double	min, *pmin, min1, min2;
    void        *dummy;
    HashEntry	*entry;
    int		topF, topG;
    unsigned	index;

    statLine(dd);
#ifdef CORREL_STATS
    num_calls++;
#endif

    /* Terminal cases: only work for BDDs. */
    if (f == g) return(1.0);
    if (f == Cudd_Not(g)) return(0.0);

    /* Standardize call using the following properties:
    **     (f EXNOR g)   = (g EXNOR f)
    **     (f' EXNOR g') = (f EXNOR g).
    */
    if (f > g) {
	DdNode *tmp = f;
	f = g; g = tmp;
    }
    if (Cudd_IsComplement(f)) {
	f = Cudd_Not(f);
	g = Cudd_Not(g);
    }
    /* From now on, f is regular. */
    
    entry = ALLOC(HashEntry,1);
    if (entry == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return((double)CUDD_OUT_OF_MEM);
    }
    entry->f = f; entry->g = g;

    /* We do not use the fact that
    ** correlation(f,g') = 1 - correlation(f,g)
    ** to minimize the risk of cancellation.
    */
    if (st_lookup(table, entry, &dummy)) {
	min = *(double *) dummy;
	FREE(entry);
	return(min);
    }

    G = Cudd_Regular(g);
    topF = cuddI(dd,f->index); topG = cuddI(dd,G->index);
    if (topF <= topG) {
	Fv = cuddT(f); Fnv = cuddE(f);
	index = f->index;
    } else {
	Fv = Fnv = f;
	index = G->index;
    }
    if (topG <= topF) { Gv = cuddT(G); Gnv = cuddE(G); } else { Gv = Gnv = G; }

    if (g != G) {
	Gv = Cudd_Not(Gv);
	Gnv = Cudd_Not(Gnv);
    }

    min1 = bddCorrelationWeightsAux(dd, Fv, Gv, prob, table) * prob[index];
    if (min1 == (double)CUDD_OUT_OF_MEM) {
	FREE(entry);
	return((double)CUDD_OUT_OF_MEM);
    }
    min2 = bddCorrelationWeightsAux(dd, Fnv, Gnv, prob, table) * (1.0 - prob[index]); 
    if (min2 == (double)CUDD_OUT_OF_MEM) {
	FREE(entry);
	return((double)CUDD_OUT_OF_MEM);
    }
    min = (min1+min2);
    
    pmin = ALLOC(double,1);
    if (pmin == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return((double)CUDD_OUT_OF_MEM);
    }
    *pmin = min;

    if (st_insert(table, entry, pmin) == ST_OUT_OF_MEM) {
	FREE(entry);
	FREE(pmin);
	return((double)CUDD_OUT_OF_MEM);
    }
    return(min);

} /* end of bddCorrelationWeightsAux */


/**
  @brief Compares two hash table entries.

  @return 0 if they are identical; 1 otherwise.

  @sideeffect None

*/
static int
CorrelCompare(
  void const * key1,
  void const * key2)
{
    HashEntry const *entry1 = (HashEntry const *) key1;
    HashEntry const *entry2 = (HashEntry const *) key2;
    if (entry1->f != entry2->f || entry1->g != entry2->g) return(1);

    return(0);

} /* end of CorrelCompare */


/**
  @brief Hashes a hash table entry.

  @details It is patterned after st_strhash.

  @return a value between 0 and modulus.

  @sideeffect None

*/
static int
CorrelHash(
  void const * key,
  int  modulus)
{
    HashEntry const *entry = (HashEntry const *) key;
    int val = 0;

    val = (int) (((ptrint)entry->f)*997 + ((ptrint)entry->g));

    return ((val < 0) ? -val : val) % modulus;

} /* end of CorrelHash */


/**
  @brief Frees memory associated with hash table.

  @return ST_CONTINUE.

  @sideeffect None

*/
static enum st_retval
CorrelCleanUp(
  void * key,
  void * value,
  void * arg)
{
    double	  *d = (double *) value;
    HashEntry *entry = (HashEntry *) key;

    (void) arg; /* avoid warning */
    FREE(entry);
    FREE(d);
    return ST_CONTINUE;

} /* end of CorrelCleanUp */
