/**
  @file

  @ingroup cudd

  @brief Genetic algorithm for variable reordering.

  @details The genetic algorithm implemented here is as follows.  We
  start with the current %DD order.  We sift this order and use this as
  the reference %DD.  We only keep 1 %DD around for the entire process
  and simply rearrange the order of this %DD, storing the various
  orders and their corresponding %DD sizes.  We generate more random
  orders to build an initial population. This initial population is 3
  times the number of variables, with a maximum of 120. Each random
  order is built (from the reference %DD) and its size stored.  Each
  random order is also sifted to keep the %DD sizes fairly small.  Then
  a crossover is performed between two orders (picked randomly) and
  the two resulting DDs are built and sifted.  For each new order, if
  its size is smaller than any %DD in the population, it is inserted
  into the population and the %DD with the largest number of nodes is
  thrown out. The crossover process happens up to 50 times, and at
  this point the %DD in the population with the smallest size is chosen
  as the result.  This %DD must then be built from the reference %DD.

  @author Curt Musfeldt, Alan Shuler, Fabio Somenzi

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
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/

typedef struct GeneticInfo GeneticInfo_t;

/*---------------------------------------------------------------------------*/
/* Stucture declarations                                                     */
/*---------------------------------------------------------------------------*/

/**
 * @brief Miscellaneous information.
 */
struct GeneticInfo {
    int popsize;	/**< the size of the population */
    int numvars;	/**< the number of variables to be ordered. */
/**
 ** @brief storedd stores the population orders and sizes.
 **
 ** @details This table has two extra rows and one extras column. The
 ** two extra rows are used for the offspring produced by a
 ** crossover. Each row stores one order and its size. The order is
 ** stored by storing the indices of variables in the order in which
 ** they appear in the order. The table is in reality a
 ** one-dimensional array which is accessed via a macro to give the
 ** illusion it is a two-dimensional structure.
 */
    int *storedd;
    st_table *computed;	/**< hash table to identify existing orders */
    int *repeat;	/**< how many times an order is present */
    int large;		/**< stores the index of the population with
                         ** the largest number of nodes in the %DD */
    int result;		/**< result */
    int cross;		/**< the number of crossovers to perform */
};

/*---------------------------------------------------------------------------*/
/* Variable declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Macro declarations                                                        */
/*---------------------------------------------------------------------------*/

/**
 ** @brief Used to access the population table as if it were a
 ** two-dimensional structure.
 */
#define STOREDD(info,i,j)	(info)->storedd[(i)*((info)->numvars+1)+(j)]

/** \cond */

/*---------------------------------------------------------------------------*/
/* Static function prototypes                                                */
/*---------------------------------------------------------------------------*/

static int make_random (DdManager *table, int lower, GeneticInfo_t * info);
static int sift_up (DdManager *table, int x, int x_low);
static int build_dd (DdManager *table, int num, int lower, int upper, GeneticInfo_t * info);
static int largest (GeneticInfo_t * info);
static int rand_int (DdManager * dd, int a);
static int array_hash (void const *array, int modulus, void const * arg);
static int array_compare (const void *array1, const void *array2, void const * arg);
static int find_best (GeneticInfo_t * info);
#ifdef DD_STATS
static double find_average_fitness (GeneticInfo_t * info);
#endif
static int PMX (DdManager * dd, int maxvar, GeneticInfo_t * info);
static int roulette (DdManager *dd, int *p1, int *p2, GeneticInfo_t * info);

/** \endcond */

/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Definition of internal functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Genetic algorithm for %DD reordering.

  @details The two children of a crossover will be stored in
  storedd[popsize] and storedd[popsize+1] --- the last two slots in the
  storedd array.  (This will make comparisons and replacement easy.)

  @return 1 in case of success; 0 otherwise.

  @sideeffect None

*/
int
cuddGa(
  DdManager * table /**< manager */,
  int  lower /**< lowest level to be reordered */,
  int  upper /**< highest level to be reorderded */)
{
    int 	i,n,m;		/* dummy/loop vars */
    int		index;
#ifdef DD_STATS
    double	average_fitness;
#endif
    int		small;		/* index of smallest DD in population */
    GeneticInfo_t info;

    /* Do an initial sifting to produce at least one reasonable individual. */
    if (!cuddSifting(table,lower,upper)) return(0);

    /* Get the initial values. */
    info.numvars = upper - lower + 1; /* number of variables to be reordered */
    if (table->populationSize == 0) {
	info.popsize = 3 * info.numvars;  /* population size is 3 times # of vars */
	if (info.popsize > 120) {
	    info.popsize = 120;	/* Maximum population size is 120 */
	}
    } else {
	info.popsize = table->populationSize;  /* user specified value */
    }
    if (info.popsize < 4) info.popsize = 4;	/* enforce minimum population size */

    /* Allocate population table. */
    info.storedd = ALLOC(int,(info.popsize+2)*(info.numvars+1));
    if (info.storedd == NULL) {
	table->errorCode = CUDD_MEMORY_OUT;
	return(0);
    }

    /* Initialize the computed table. This table is made up of two data
    ** structures: A hash table with the key given by the order, which says
    ** if a given order is present in the population; and the repeat
    ** vector, which says how many copies of a given order are stored in
    ** the population table. If there are multiple copies of an order, only
    ** one has a repeat count greater than 1. This copy is the one pointed
    ** by the computed table.
    */
    info.repeat = ALLOC(int,info.popsize);
    if (info.repeat == NULL) {
	table->errorCode = CUDD_MEMORY_OUT;
	FREE(info.storedd);
	return(0);
    }
    for (i = 0; i < info.popsize; i++) {
	info.repeat[i] = 0;
    }
    info.computed = st_init_table_with_arg(array_compare,array_hash,
                                           (void *)(ptrint) info.numvars);
    if (info.computed == NULL) {
	table->errorCode = CUDD_MEMORY_OUT;
	FREE(info.storedd);
	FREE(info.repeat);
	return(0);
    }

    /* Copy the current DD and its size to the population table. */
    for (i = 0; i < info.numvars; i++) {
	STOREDD(&info,0,i) = table->invperm[i+lower]; /* order of initial DD */
    }
    STOREDD(&info,0,info.numvars) =
        (int) (table->keys - table->isolated); /* size of initial DD */

    /* Store the initial order in the computed table. */
    if (st_insert(info.computed,info.storedd,(void *) 0) == ST_OUT_OF_MEM) {
	FREE(info.storedd);
	FREE(info.repeat);
	st_free_table(info.computed);
	return(0);
    }
    info.repeat[0]++;

    /* Insert the reverse order as second element of the population. */
    for (i = 0; i < info.numvars; i++) {
	STOREDD(&info,1,info.numvars-1-i) = table->invperm[i+lower]; /* reverse order */
    }

    /* Now create the random orders. make_random fills the population
    ** table with random permutations. The successive loop builds and sifts
    ** the DDs for the reverse order and each random permutation, and stores
    ** the results in the computed table.
    */
    if (!make_random(table,lower,&info)) {
	table->errorCode = CUDD_MEMORY_OUT;
	FREE(info.storedd);
	FREE(info.repeat);
	st_free_table(info.computed);
	return(0);
    }
    for (i = 1; i < info.popsize; i++) {
	info.result = build_dd(table,i,lower,upper,&info);	/* build and sift order */
	if (!info.result) {
	    FREE(info.storedd);
	    FREE(info.repeat);
	    st_free_table(info.computed);
	    return(0);
	}
	if (st_lookup_int(info.computed,&STOREDD(&info,i,0),&index)) {
	    info.repeat[index]++;
	} else {
	    if (st_insert(info.computed,&STOREDD(&info,i,0),(void *)(ptruint)i) ==
	    ST_OUT_OF_MEM) {
		FREE(info.storedd);
		FREE(info.repeat);
		st_free_table(info.computed);
		return(0);
	    }
	    info.repeat[i]++;
	}
    }

#if 0
#ifdef DD_STATS
    /* Print the initial population. */
    (void) fprintf(table->out,"Initial population after sifting\n");
    for (m = 0; m < info.popsize; m++) {
	for (i = 0; i < info.numvars; i++) {
	    (void) fprintf(table->out," %2d",STOREDD(&info,m,i));
	}
	(void) fprintf(table->out," : %3d (%d)\n",
		       STOREDD(&info,m,numvars),info.repeat[m]);
    }
#endif
#endif

#ifdef DD_STATS
    small = find_best(&info);
    average_fitness = find_average_fitness(&info);
    (void) fprintf(table->out,"\nInitial population: best fitness = %d, average fitness %8.3f",STOREDD(&info,small,info.numvars),average_fitness);
#endif

    /* Decide how many crossovers should be tried. */
    if (table->numberXovers == 0) {
	info.cross = 3*info.numvars;
	if (info.cross > 60) {	/* do a maximum of 50 crossovers */
	    info.cross = 60;
	}
    } else {
	info.cross = table->numberXovers;      /* use user specified value */
    }
    if (info.cross >= info.popsize) {
	info.cross = info.popsize;
    }

    /* Perform the crossovers to get the best order. */
    for (m = 0; m < info.cross; m++) {
	if (!PMX(table, table->size, &info)) {	/* perform one crossover */
	    table->errorCode = CUDD_MEMORY_OUT;
	    FREE(info.storedd);
	    FREE(info.repeat);
	    st_free_table(info.computed);
	    return(0);
	}
	/* The offsprings are left in the last two entries of the
	** population table. These are now considered in turn.
	*/
	for (i = info.popsize; i <= info.popsize+1; i++) {
	    info.result = build_dd(table,i,lower,upper,&info); /* build and sift child */
	    if (!info.result) {
		FREE(info.storedd);
		FREE(info.repeat);
		st_free_table(info.computed);
		return(0);
	    }
	    info.large = largest(&info); /* find the largest DD in population */

	    /* If the new child is smaller than the largest DD in the current
	    ** population, enter it into the population in place of the
	    ** largest DD.
	    */
	    if (STOREDD(&info,i,info.numvars) <
                STOREDD(&info,info.large,info.numvars)) {
		/* Look up the largest DD in the computed table.
		** Decrease its repetition count. If the repetition count
		** goes to 0, remove the largest DD from the computed table.
		*/
		info.result = st_lookup_int(info.computed,&STOREDD(&info,info.large,0),&index);
		if (!info.result) {
		    FREE(info.storedd);
		    FREE(info.repeat);
		    st_free_table(info.computed);
		    return(0);
		}
		info.repeat[index]--;
		if (info.repeat[index] == 0) {
		    int *pointer = &STOREDD(&info,index,0);
		    info.result = st_delete(info.computed, (void **) &pointer, NULL);
		    if (!info.result) {
			FREE(info.storedd);
			FREE(info.repeat);
			st_free_table(info.computed);
			return(0);
		    }
		}
		/* Copy the new individual to the entry of the
		** population table just made available and update the
		** computed table.
		*/
		for (n = 0; n <= info.numvars; n++) {
		    STOREDD(&info,info.large,n) = STOREDD(&info,i,n);
		}
		if (st_lookup_int(info.computed,&STOREDD(&info,info.large,0),&index)) {
		    info.repeat[index]++;
		} else {
		    if (st_insert(info.computed,&STOREDD(&info,info.large,0),
		    (void *)(ptruint)info.large) == ST_OUT_OF_MEM) {
			FREE(info.storedd);
			FREE(info.repeat);
			st_free_table(info.computed);
			return(0);
		    }
		    info.repeat[info.large]++;
		}
	    }
	}
    }

    /* Find the smallest DD in the population and build it;
    ** that will be the result.
    */
    small = find_best(&info);

    /* Print stats on the final population. */
#ifdef DD_STATS
    average_fitness = find_average_fitness(&info);
    (void) fprintf(table->out,"\nFinal population: best fitness = %d, average fitness %8.3f",STOREDD(&info,small,info.numvars),average_fitness);
#endif

    /* Clean up, build the result DD, and return. */
    st_free_table(info.computed);
    info.computed = NULL;
    info.result = build_dd(table,small,lower,upper,&info);
    FREE(info.storedd);
    FREE(info.repeat);
    return(info.result);

} /* end of cuddGa */


/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/

/**
  @brief Generates the random sequences for the initial population.

  @details The sequences are permutations of the indices between lower
  and upper in the current order.

  @sideeffect None

*/
static int
make_random(
  DdManager * table,
  int  lower,
  GeneticInfo_t * info)
{
    int i,j;		/* loop variables */
    int	*used;		/* is a number already in a permutation */
    int	next;		/* next random number without repetitions */

    used = ALLOC(int,info->numvars);
    if (used == NULL) {
	table->errorCode = CUDD_MEMORY_OUT;
	return(0);
    }
#if 0
#ifdef DD_STATS
    (void) fprintf(table->out,"Initial population before sifting\n");
    for (i = 0; i < 2; i++) {
	for (j = 0; j < numvars; j++) {
	    (void) fprintf(table->out," %2d",STOREDD(i,j));
	}
	(void) fprintf(table->out,"\n");
    }
#endif
#endif
    for (i = 2; i < info->popsize; i++) {
       	for (j = 0; j < info->numvars; j++) {
	    used[j] = 0;
	}
	/* Generate a permutation of {0...numvars-1} and use it to
	** permute the variables in the layesr from lower to upper.
	*/
       	for (j = 0; j < info->numvars; j++) {
	    do {
		next = rand_int(table,info->numvars-1);
	    } while (used[next] != 0);
	    used[next] = 1;
	    STOREDD(info,i,j) = table->invperm[next+lower];
       	}
#if 0
#ifdef DD_STATS
	/* Print the order just generated. */
	for (j = 0; j < numvars; j++) {
	    (void) fprintf(table->out," %2d",STOREDD(i,j));
	}
	(void) fprintf(table->out,"\n");
#endif
#endif
    }
    FREE(used);
    return(1);

} /* end of make_random */


/**
  @brief Moves one variable up.

  @details Takes a variable from position x and sifts it up to
  position x_low;  x_low should be less than x.

  @return 1 if successful; 0 otherwise

  @sideeffect None

*/
static int
sift_up(
  DdManager * table,
  int  x,
  int  x_low)
{
    int        y;
    int        size;

    y = cuddNextLow(table,x);
    while (y >= x_low) {
	size = cuddSwapInPlace(table,y,x);
	if (size == 0) {
	    return(0);
	}
	x = y;
	y = cuddNextLow(table,x);
    }
    return(1);

} /* end of sift_up */


/**
  @brief Builds a %DD from a given order.

  @details This procedure also sifts the final order and inserts into
  the array the size in nodes of the result.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

*/
static int
build_dd(
  DdManager * table,
  int  num /* the index of the individual to be built */,
  int  lower,
  int  upper,
  GeneticInfo_t * info)
{
    int 	i,j;		/* loop vars */
    int 	position;
    int		index;
    int		limit;		/* how large the DD for this order can grow */
    int		size;

    /* Check the computed table. If the order already exists, it
    ** suffices to copy the size from the existing entry.
    */
    if (info->computed &&
        st_lookup_int(info->computed,&STOREDD(info,num,0),&index)) {
	STOREDD(info,num,info->numvars) = STOREDD(info,index,info->numvars);
#ifdef DD_STATS
	(void) fprintf(table->out,"\nCache hit for index %d", index);
#endif
	return(1);
    }

    /* Stop if the DD grows 20 times larges than the reference size. */
    limit = 20 * STOREDD(info,0,info->numvars);

    /* Sift up the variables so as to build the desired permutation.
    ** First the variable that has to be on top is sifted to the top.
    ** Then the variable that has to occupy the secon position is sifted
    ** up to the second position, and so on.
    */
    for (j = 0; j < info->numvars; j++) {
	i = STOREDD(info,num,j);
	position = table->perm[i];
	info->result = sift_up(table,position,j+lower);
	if (!info->result) return(0);
	size = (int) (table->keys - table->isolated);
	if (size > limit) break;
    }

    /* Sift the DD just built. */
#ifdef DD_STATS
    (void) fprintf(table->out,"\n");
#endif
    info->result = cuddSifting(table,lower,upper);
    if (!info->result) return(0);

    /* Copy order and size to table. */
    for (j = 0; j < info->numvars; j++) {
	STOREDD(info,num,j) = table->invperm[lower+j];
    }
    STOREDD(info,num,info->numvars) = (int) (table->keys - table->isolated); /* size of new DD */
    return(1);

} /* end of build_dd */


/**
  @brief Finds the largest %DD in the population.

  @details If an order is repeated, it avoids choosing the copy that
  is in the computed table (it has repeat[i > 1).]

  @sideeffect None

*/
static int
largest(GeneticInfo_t * info)
{
    int i;	/* loop var */
    int big;	/* temporary holder to return result */

    big = 0;
    while (info->repeat[big] > 1) big++;
    for (i = big + 1; i < info->popsize; i++) {
	if (STOREDD(info,i,info->numvars) >=
            STOREDD(info,big,info->numvars) && info->repeat[i] <= 1) {
	    big = i;
	}
    }
    return(big);

} /* end of largest */


/**
  @brief Generates a random number between 0 and the integer a.

  @sideeffect None

*/
static int
rand_int(
  DdManager *dd,
  int  a)
{
    return(Cudd_Random(dd) % (a+1));

} /* end of rand_int */


/**
  @brief Hash function for the computed table.

  @return the bucket number.

  @sideeffect None

*/
static int
array_hash(
  void const * array,
  int modulus,
  void const * arg)
{
    int val = 0;
    int i;
    int const *intarray = (int const *) array;
    int const numvars = (int const)(ptrint const) arg;

    for (i = 0; i < numvars; i++) {
	val = val * 997 + intarray[i];
    }

    return(((val < 0) ? -val : val) % modulus);

} /* end of array_hash */


/**
  @brief Comparison function for the computed table.

  @return 0 if the two arrays are equal; 1 otherwise.

  @sideeffect None

*/
static int
array_compare(
  void const * array1,
  void const * array2,
  void const * arg)
{
    int i;
    int const *intarray1 = (int const *) array1;
    int const *intarray2 = (int const *) array2;
    int const numvars = (int const)(ptrint const) arg;

    for (i = 0; i < numvars; i++) {
	if (intarray1[i] != intarray2[i]) return(1);
    }
    return(0);

} /* end of array_compare */


/**
  @brief Returns the index of the fittest individual.

  @sideeffect None

*/
static int
find_best(GeneticInfo_t * info)
{
    int i,small;

    small = 0;
    for (i = 1; i < info->popsize; i++) {
	if (STOREDD(info,i,info->numvars) < STOREDD(info,small,info->numvars)) {
	    small = i;
	}
    }
    return(small);

} /* end of find_best */


/**
  @brief Returns the average fitness of the population.

  @sideeffect None

*/
#ifdef DD_STATS
static double
find_average_fitness(GeneticInfo_t * info)
{
    int i;
    int total_fitness = 0;
    double average_fitness;

    for (i = 0; i < info->popsize; i++) {
	total_fitness += STOREDD(info,i,info->numvars);
    }
    average_fitness = (double) total_fitness / (double) info->popsize;
    return(average_fitness);

} /* end of find_average_fitness */
#endif


/**
  @brief Performs the crossover between two parents.

  @details Performs the crossover between two randomly chosen
  parents, and creates two children, x1 and x2. Uses the Partially
  Matched Crossover operator.

  @sideeffect None

*/
static int
PMX(
  DdManager * dd,
  int  maxvar,
  GeneticInfo_t * info)
{
    int 	cut1,cut2;	/* the two cut positions (random) */
    int 	mom,dad;	/* the two randomly chosen parents */
    int		*inv1;		/* inverse permutations for repair algo */
    int		*inv2;
    int 	i;		/* loop vars */
    int		u,v;		/* aux vars */

    inv1 = ALLOC(int,maxvar);
    if (inv1 == NULL) {
	return(0);
    }
    inv2 = ALLOC(int,maxvar);
    if (inv2 == NULL) {
	FREE(inv1);
	return(0);
    }

    /* Choose two orders from the population using roulette wheel. */
    if (!roulette(dd,&mom,&dad,info)) {
	FREE(inv1);
	FREE(inv2);
	return(0);
    }

    /* Choose two random cut positions. A cut in position i means that
    ** the cut immediately precedes position i.  If cut1 < cut2, we
    ** exchange the middle of the two orderings; otherwise, we
    ** exchange the beginnings and the ends.
    */
    cut1 = rand_int(dd,info->numvars-1);
    do {
	cut2 = rand_int(dd,info->numvars-1);
    } while (cut1 == cut2);

#if 0
    /* Print out the parents. */
    (void) fprintf(dd->out,
		   "Crossover of %d (mom) and %d (dad) between %d and %d\n",
		   mom,dad,cut1,cut2);
    for (i = 0; i < info->numvars; i++) {
	if (i == cut1 || i == cut2) (void) fprintf(dd->out,"|");
	(void) fprintf(dd->out,"%2d ",STOREDD(info,mom,i));
    }
    (void) fprintf(dd->out,"\n");
    for (i = 0; i < info->numvars; i++) {
	if (i == cut1 || i == cut2) (void) fprintf(dd->out,"|");
	(void) fprintf(dd->out,"%2d ",STOREDD(info,dad,i));
    }
    (void) fprintf(dd->out,"\n");
#endif

    /* Initialize the inverse permutations: -1 means yet undetermined. */
    for (i = 0; i < maxvar; i++) {
	inv1[i] = -1;
	inv2[i] = -1;
    }

    /* Copy the portions whithin the cuts. */
    for (i = cut1; i != cut2; i = (i == info->numvars-1) ? 0 : i+1) {
	STOREDD(info,info->popsize,i) = STOREDD(info,dad,i);
	inv1[STOREDD(info,info->popsize,i)] = i;
	STOREDD(info,info->popsize+1,i) = STOREDD(info,mom,i);
	inv2[STOREDD(info,info->popsize+1,i)] = i;
    }

    /* Now apply the repair algorithm outside the cuts. */
    for (i = cut2; i != cut1; i = (i == info->numvars-1 ) ? 0 : i+1) {
	v = i;
	do {
	    u = STOREDD(info,mom,v);
	    v = inv1[u];
	} while (v != -1);
	STOREDD(info,info->popsize,i) = u;
	inv1[u] = i;
	v = i;
	do {
	    u = STOREDD(info,dad,v);
	    v = inv2[u];
	} while (v != -1);
	STOREDD(info,info->popsize+1,i) = u;
	inv2[u] = i;
    }

#if 0
    /* Print the results of crossover. */
    for (i = 0; i < info->numvars; i++) {
	if (i == cut1 || i == cut2) (void) fprintf(table->out,"|");
	(void) fprintf(table->out,"%2d ",STOREDD(info,info->popsize,i));
    }
    (void) fprintf(table->out,"\n");
    for (i = 0; i < info->numvars; i++) {
	if (i == cut1 || i == cut2) (void) fprintf(table->out,"|");
	(void) fprintf(table->out,"%2d ",STOREDD(info,info->popsize+1,i));
    }
    (void) fprintf(table->out,"\n");
#endif

    FREE(inv1);
    FREE(inv2);
    return(1);

} /* end of PMX */


/**
  @brief Selects two distinct parents with the roulette wheel method.

  @sideeffect The indices of the selected parents are returned as side
  effects.

*/
static int
roulette(
  DdManager * dd,
  int * p1,
  int * p2,
  GeneticInfo_t * info)
{
    double *wheel;
    double spin;
    int i;

    wheel = ALLOC(double,info->popsize);
    if (wheel == NULL) {
	return(0);
    }

    /* The fitness of an individual is the reciprocal of its size. */
    wheel[0] = 1.0 / (double) STOREDD(info,0,info->numvars);

    for (i = 1; i < info->popsize; i++) {
	wheel[i] = wheel[i-1] + 1.0 / (double) STOREDD(info,i,info->numvars);
    }

    /* Get a random number between 0 and wheel[popsize-1] (that is,
    ** the sum of all fitness values. 2147483561 is the largest number
    ** returned by Cudd_Random.
    */
    spin = wheel[info->numvars-1] * (double) Cudd_Random(dd) / 2147483561.0;

    /* Find the lucky element by scanning the wheel. */
    for (i = 0; i < info->popsize; i++) {
	if (spin <= wheel[i]) break;
    }
    *p1 = i;

    /* Repeat the process for the second parent, making sure it is
    ** distinct from the first.
    */
    do {
	spin = wheel[info->popsize-1] * (double) Cudd_Random(dd) / 2147483561.0;
	for (i = 0; i < info->popsize; i++) {
	    if (spin <= wheel[i]) break;
	}
    } while (i == *p1);
    *p2 = i;

    FREE(wheel);
    return(1);

} /* end of roulette */
