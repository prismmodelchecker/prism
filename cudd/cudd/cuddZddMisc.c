/**
  @file

  @ingroup cudd

  @brief Miscellaneous utility functions for ZDDs.

  @author Hyong-Kyoon Shin, In-Ho Moon

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

static int cuddZddDagInt (DdNode *n, st_table *tab);

/** \endcond */


/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Counts the number of nodes in a %ZDD.

  @deprecated This function duplicates Cudd_DagSize and is only
  retained for compatibility.

  @sideeffect None

  @see Cudd_DagSize

*/
int
Cudd_zddDagSize(
  DdNode * p_node)
{

    int		i;
    st_table	*table;

    table = st_init_table(st_ptrcmp, st_ptrhash);
    i = cuddZddDagInt(p_node, table);
    st_free_table(table);
    return(i);

} /* end of Cudd_zddDagSize */


/**
  @brief Counts the number of minterms of a %ZDD.

  @details Counts the number of minterms of the %ZDD rooted at
  <code>node</code>.  This procedure takes a parameter
  <code>path</code> that specifies how many variables are in the
  support of the function.

  @return the count.  If the procedure runs out of memory, it returns
  (double) CUDD_OUT_OF_MEM.

  @sideeffect None

  @see Cudd_zddCountDouble

*/
double
Cudd_zddCountMinterm(
  DdManager * zdd,
  DdNode * node,
  int  path)
{
    double	dc_var, minterms;

    dc_var = (double)((double)(zdd->sizeZ) - (double)path);
    minterms = Cudd_zddCountDouble(zdd, node) / pow(2.0, dc_var);
    return(minterms);

} /* end of Cudd_zddCountMinterm */


/**
  @brief Prints the %ZDD table for debugging purposes.

  @sideeffect None

*/
void
Cudd_zddPrintSubtable(
  DdManager * table)
{
    int		i, j;
    DdNode	*z1, *z1_next, *base;
    DdSubtable	*ZSubTable;

    base = table->one;
    for (i = table->sizeZ - 1; i >= 0; i--) {
	ZSubTable = &(table->subtableZ[i]);
	printf("subtable[%d]:\n", i);
	for (j = ZSubTable->slots - 1; j >= 0; j--) {
	    z1 = ZSubTable->nodelist[j];
	    while (z1 != NIL(DdNode)) {
		(void) fprintf(table->out,
		    "ID = 0x%" PRIxPTR "\tindex = %u\tr = %u\t",
		    (ptruint) z1 / (ptruint) sizeof(DdNode),
		    z1->index, z1->ref);
		z1_next = cuddT(z1);
		if (Cudd_IsConstantInt(z1_next)) {
		    (void) fprintf(table->out, "T = %d\t\t",
			(z1_next == base));
		}
		else {
		    (void) fprintf(table->out, "T = 0x%" PRIxPTR "\t",
			(ptruint) z1_next / (ptruint) sizeof(DdNode));
		}
		z1_next = cuddE(z1);
		if (Cudd_IsConstantInt(z1_next)) {
		    (void) fprintf(table->out, "E = %d\n",
			(z1_next == base));
		}
		else {
		    (void) fprintf(table->out, "E = 0x%" PRIxPTR "\n",
			(ptruint) z1_next / (ptruint) sizeof(DdNode));
		}

		z1_next = z1->next;
		z1 = z1_next;
	    }
	}
    }
    putchar('\n');

} /* Cudd_zddPrintSubtable */


/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/


/**
  @brief Performs the recursive step of Cudd_zddDagSize.

  @details Does not check for out-of-memory conditions.

  @sideeffect None

*/
static int
cuddZddDagInt(
  DdNode * n,
  st_table * tab)
{
    if (n == NIL(DdNode))
	return(0);

    if (st_is_member(tab, n) == 1)
	return(0);

    if (Cudd_IsConstantInt(n))
	return(0);

    (void)st_insert(tab, n, NIL(void));
    return(1 + cuddZddDagInt(cuddT(n), tab) +
	cuddZddDagInt(cuddE(n), tab));

} /* cuddZddDagInt */
