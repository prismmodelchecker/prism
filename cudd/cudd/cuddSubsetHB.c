/**
  @file

  @ingroup cudd

  @brief Procedure to subset the given %BDD by choosing the heavier
  branches.

  @see cuddSubsetSP.c

  @author Kavita Ravi

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

#ifdef __STDC__
#include <float.h>
#else
#define DBL_MAX_EXP 1024
#endif
#include "util.h"
#include "cuddInt.h"

/*---------------------------------------------------------------------------*/
/* Constant declarations                                                     */
/*---------------------------------------------------------------------------*/

#define	DEFAULT_PAGE_SIZE 2048
#define	DEFAULT_NODE_DATA_PAGE_SIZE 1024
#define INITIAL_PAGES 128

#undef max

/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/

typedef struct NodeData NodeData_t;

typedef struct SubsetInfo SubsetInfo_t;

/*---------------------------------------------------------------------------*/
/* Stucture declarations                                                     */
/*---------------------------------------------------------------------------*/

/**
 * @brief Data structure to store the information on each node.

 * @details It keeps the number of minterms represented by the DAG
 * rooted at this node in terms of the number of variables specified
 * by the user, number of nodes in this DAG and the number of nodes of
 * its child with lesser number of minterms that are not shared by the
 * child with more minterms.
 */
struct NodeData {
    double *mintermPointer;
    int *nodesPointer;
    int *lightChildNodesPointer;
};

/**
 * @brief Miscellaneous info.
 */
struct SubsetInfo {
    DdNode	*zero, *one; /**< constant functions */
    double	**mintermPages;	/**< pointers to the pages */
    int		**nodePages; /**< pointers to the pages */
    int		**lightNodePages; /**< pointers to the pages */
    double	*currentMintermPage; /**< pointer to the current page */
    double	max; /**< to store the 2^n value of the number of variables */
    int		*currentNodePage; /**< pointer to the current page */
    int		*currentLightNodePage; /**< pointer to the current page */
    int		pageIndex; /**< index to next element */
    int		page; /**< index to current page */
    int		pageSize; /**< page size */
    int         maxPages; /**< number of page pointers */
    NodeData_t	*currentNodeDataPage; /**< pointer to the current page */
    int		nodeDataPage; /**< index to next element */
    int		nodeDataPageIndex; /**< index to next element */
    NodeData_t	**nodeDataPages; /**< index to current page */
    int		nodeDataPageSize; /**< page size */
    int         maxNodeDataPages; /**< number of page pointers */
    int memOut;
#ifdef DEBUG
    int		num_calls;
#endif
};

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

static void ResizeNodeDataPages (SubsetInfo_t * info);
static void ResizeCountMintermPages (SubsetInfo_t * info);
static void ResizeCountNodePages (SubsetInfo_t * info);
static double SubsetCountMintermAux (DdNode *node, double max, st_table *table, SubsetInfo_t * info);
static st_table * SubsetCountMinterm (DdNode *node, int nvars, SubsetInfo_t * info);
static int SubsetCountNodesAux (DdNode *node, st_table *table, double max, SubsetInfo_t * info);
static int SubsetCountNodes (DdNode *node, st_table *table, int nvars, SubsetInfo_t * info);
static void StoreNodes (st_table *storeTable, DdManager *dd, DdNode *node);
static DdNode * BuildSubsetBdd (DdManager *dd, DdNode *node, int *size, st_table *visitedTable, int threshold, st_table *storeTable, st_table *approxTable, SubsetInfo_t * info);

/** \endcond */


/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/

/**
  @brief Extracts a dense subset from a %BDD with the heavy branch
  heuristic.

  @details This procedure builds a subset by throwing away one of the
  children of each node, starting from the root, until the result is
  small enough. The child that is eliminated from the result is the
  one that contributes the fewer minterms.  The parameter numVars is
  the maximum number of variables to be used in minterm calculation
  and node count calculation.  The optimal number should be as close
  as possible to the size of the support of f.  However, it is safe to
  pass the value returned by Cudd_ReadSize for numVars when the number
  of variables is under 1023.  If numVars is larger than 1023, it will
  overflow. If a 0 parameter is passed then the procedure will compute
  a value which will avoid overflow but will cause underflow with 2046
  variables or more.

  @return a pointer to the %BDD of the subset if successful. NULL if
  the procedure runs out of memory.

  @sideeffect None

  @see Cudd_SubsetShortPaths Cudd_SupersetHeavyBranch Cudd_ReadSize

*/
DdNode *
Cudd_SubsetHeavyBranch(
  DdManager * dd /**< manager */,
  DdNode * f /**< function to be subset */,
  int  numVars /**< number of variables in the support of f */,
  int  threshold /**< maximum number of nodes in the subset */)
{
    DdNode *subset;

    do {
	dd->reordered = 0;
	subset = cuddSubsetHeavyBranch(dd, f, numVars, threshold);
    } while (dd->reordered == 1);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }

    return(subset);

} /* end of Cudd_SubsetHeavyBranch */


/**
  @brief Extracts a dense superset from a %BDD with the heavy branch
  heuristic.

  @details The procedure is identical to the subset procedure except
  for the fact that it receives the complement of the given
  function. Extracting the subset of the complement function is
  equivalent to extracting the superset of the function. This
  procedure builds a superset by throwing away one of the children of
  each node starting from the root of the complement function, until
  the result is small enough. The child that is eliminated from the
  result is the one that contributes the fewer minterms.  The
  parameter numVars is the maximum number of variables to be used in
  minterm calculation and node count calculation.  The optimal number
  should be as close as possible to the size of the support of f.
  However, it is safe to pass the value returned by Cudd_ReadSize for
  numVars when the number of variables is under 1023.  If numVars is
  larger than 1023, it will overflow. If a 0 parameter is passed then
  the procedure will compute a value which will avoid overflow but
  will cause underflow with 2046 variables or more.

  @return a pointer to the %BDD of the superset if successful. NULL if
  intermediate result causes the procedure to run out of memory.

  @sideeffect None

  @see Cudd_SubsetHeavyBranch Cudd_SupersetShortPaths Cudd_ReadSize

*/
DdNode *
Cudd_SupersetHeavyBranch(
  DdManager * dd /**< manager */,
  DdNode * f /**< function to be superset */,
  int  numVars /**< number of variables in the support of f */,
  int  threshold /**< maximum number of nodes in the superset */)
{
    DdNode *subset, *g;

    g = Cudd_Not(f);
    do {
	dd->reordered = 0;
	subset = cuddSubsetHeavyBranch(dd, g, numVars, threshold);
    } while (dd->reordered == 1);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }

    return(Cudd_NotCond(subset, (subset != NULL)));

} /* end of Cudd_SupersetHeavyBranch */


/*---------------------------------------------------------------------------*/
/* Definition of internal functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief The main procedure that returns a subset by choosing the heavier
  branch in the %BDD.

  @details Here a subset %BDD is built by throwing away one of the
  children. Starting at root, annotate each node with the number of
  minterms (in terms of the total number of variables specified -
  numVars), number of nodes taken by the DAG rooted at this node and
  number of additional nodes taken by the child that has the lesser
  minterms. The child with the lower number of minterms is thrown away
  and a dyanmic count of the nodes of the subset is kept. Once the
  threshold is reached the subset is returned to the calling
  procedure.

  @sideeffect None

  @see Cudd_SubsetHeavyBranch

*/
DdNode *
cuddSubsetHeavyBranch(
  DdManager * dd /**< %DD manager */,
  DdNode * f /**< current %DD */,
  int  numVars /**< maximum number of variables */,
  int  threshold /**< threshold size for the subset */)
{

    int i, *size;
    st_table *visitedTable;
    int numNodes;
    NodeData_t *currNodeQual;
    DdNode *subset;
    st_table *storeTable, *approxTable;
    DdNode *key, *value;
    st_generator *stGen;
    SubsetInfo_t info;

    if (f == NULL) {
	fprintf(dd->err, "Cannot subset, nil object\n");
	dd->errorCode = CUDD_INVALID_ARG;
	return(NULL);
    }

    /* If user does not know numVars value, set it to the maximum
     * exponent that the pow function can take. The -1 is due to the
     * discrepancy in the value that pow takes and the value that
     * log gives.
     */
    if (numVars == 0) {
	/* set default value */
	numVars = DBL_MAX_EXP - 1;
    }

    if (Cudd_IsConstantInt(f)) {
	return(f);
    }

    info.one  = Cudd_ReadOne(dd);
    info.zero = Cudd_Not(info.one);
    info.mintermPages = NULL;
    info.nodePages = info.lightNodePages = NULL;
    info.currentMintermPage = NULL;
    info.max = pow(2.0, (double)numVars);
    info.currentNodePage = info.currentLightNodePage = NULL;
    info.pageIndex = info.page = 0;
    info.pageSize = DEFAULT_PAGE_SIZE;
    info.maxPages = 0;
    info.currentNodeDataPage = NULL;
    info.nodeDataPage = info.nodeDataPageIndex = 0;
    info.nodeDataPages = NULL;
    info.nodeDataPageSize = DEFAULT_NODE_DATA_PAGE_SIZE;
    info.maxNodeDataPages = 0;
    info.memOut = 0;
#ifdef DEBUG
    info.num_calls = 0;
#endif

    /* Create visited table where structures for node data are allocated and
       stored in a st_table */
    visitedTable = SubsetCountMinterm(f, numVars, &info);
    if ((visitedTable == NULL) || info.memOut) {
	(void) fprintf(dd->err, "Out-of-memory; Cannot subset\n");
	dd->errorCode = CUDD_MEMORY_OUT;
	return(0);
    }
    numNodes = SubsetCountNodes(f, visitedTable, numVars, &info);
    if (info.memOut) {
	(void) fprintf(dd->err, "Out-of-memory; Cannot subset\n");
	dd->errorCode = CUDD_MEMORY_OUT;
	return(0);
    }

    if (st_lookup(visitedTable, f, (void **) &currNodeQual) == 0) {
	fprintf(dd->err,
		"Something is wrong, ought to be node quality table\n");
	dd->errorCode = CUDD_INTERNAL_ERROR;
    }

    size = ALLOC(int, 1);
    if (size == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return(NULL);
    }
    *size = numNodes;

    /* table to store nodes being created. */
    storeTable = st_init_table(st_ptrcmp, st_ptrhash);
    /* insert the constant */
    cuddRef(info.one);
    if (st_insert(storeTable, Cudd_ReadOne(dd), NULL) ==
	ST_OUT_OF_MEM) {
	fprintf(dd->out, "Something wrong, st_table insert failed\n");
    }
    /* table to store approximations of nodes */
    approxTable = st_init_table(st_ptrcmp, st_ptrhash);
    subset = (DdNode *)BuildSubsetBdd(dd, f, size, visitedTable, threshold,
				      storeTable, approxTable, &info);
    if (subset != NULL) {
	cuddRef(subset);
    }

    if (info.memOut) {
        dd->errorCode = CUDD_MEMORY_OUT;
        dd->reordered = 0;
    }

    stGen = st_init_gen(approxTable);
    if (stGen == NULL) {
	st_free_table(approxTable);
	return(NULL);
    }
    while(st_gen(stGen, (void **) &key, (void **) &value)) {
	Cudd_RecursiveDeref(dd, value);
    }
    st_free_gen(stGen); stGen = NULL;
    st_free_table(approxTable);

    stGen = st_init_gen(storeTable);
    if (stGen == NULL) {
	st_free_table(storeTable);
	return(NULL);
    }
    while(st_gen(stGen, (void **) &key, (void **) &value)) {
	Cudd_RecursiveDeref(dd, key);
    }
    st_free_gen(stGen); stGen = NULL;
    st_free_table(storeTable);

    for (i = 0; i <= info.page; i++) {
	FREE(info.mintermPages[i]);
    }
    FREE(info.mintermPages);
    for (i = 0; i <= info.page; i++) {
	FREE(info.nodePages[i]);
    }
    FREE(info.nodePages);
    for (i = 0; i <= info.page; i++) {
	FREE(info.lightNodePages[i]);
    }
    FREE(info.lightNodePages);
    for (i = 0; i <= info.nodeDataPage; i++) {
	FREE(info.nodeDataPages[i]);
    }
    FREE(info.nodeDataPages);
    st_free_table(visitedTable);
    FREE(size);
#if 0
    (void) Cudd_DebugCheck(dd);
    (void) Cudd_CheckKeys(dd);
#endif

    if (subset != NULL) {
#ifdef DD_DEBUG
      if (!Cudd_bddLeq(dd, subset, f)) {
	    fprintf(dd->err, "Wrong subset\n");
	    dd->errorCode = CUDD_INTERNAL_ERROR;
	    return(NULL);
      }
#endif
	cuddDeref(subset);
    }
    return(subset);

} /* end of cuddSubsetHeavyBranch */


/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/


/**
  @brief Resize the number of pages allocated to store the node data.

  @details The procedure moves the counter to the next page when the
  end of the page is reached and allocates new pages when necessary.

  @sideeffect Changes the size of pages, page, page index, maximum
  number of pages freeing stuff in case of memory out. 

*/
static void
ResizeNodeDataPages(SubsetInfo_t * info)
{
    int i;
    NodeData_t **newNodeDataPages;

    info->nodeDataPage++;
    /* If the current page index is larger than the number of pages
     * allocated, allocate a new page array. Page numbers are incremented by
     * INITIAL_PAGES
     */
    if (info->nodeDataPage == info->maxNodeDataPages) {
	newNodeDataPages = ALLOC(NodeData_t *, info->maxNodeDataPages + INITIAL_PAGES);
	if (newNodeDataPages == NULL) {
	    for (i = 0; i < info->nodeDataPage; i++) FREE(info->nodeDataPages[i]);
	    FREE(info->nodeDataPages);
	    info->memOut = 1;
	    return;
	} else {
	    for (i = 0; i < info->maxNodeDataPages; i++) {
		newNodeDataPages[i] = info->nodeDataPages[i];
	    }
	    /* Increase total page count */
	    info->maxNodeDataPages += INITIAL_PAGES;
	    FREE(info->nodeDataPages);
	    info->nodeDataPages = newNodeDataPages;
	}
    }
    /* Allocate a new page */
    info->currentNodeDataPage = info->nodeDataPages[info->nodeDataPage] =
	ALLOC(NodeData_t ,info->nodeDataPageSize);
    if (info->currentNodeDataPage == NULL) {
	for (i = 0; i < info->nodeDataPage; i++) FREE(info->nodeDataPages[i]);
	FREE(info->nodeDataPages);
	info->memOut = 1;
	return;
    }
    /* reset page index */
    info->nodeDataPageIndex = 0;
    return;

} /* end of ResizeNodeDataPages */


/**
  @brief Resize the number of pages allocated to store the minterm
  counts. 

  @details The procedure  moves the counter to the next page when the
  end of the page is reached and allocates new pages when necessary.

  @sideeffect Changes the size of minterm pages, page, page index, maximum
  number of pages freeing stuff in case of memory out. 

*/
static void
ResizeCountMintermPages(SubsetInfo_t * info)
{
    int i;
    double **newMintermPages;

    info->page++;
    /* If the current page index is larger than the number of pages
     * allocated, allocate a new page array. Page numbers are incremented by
     * INITIAL_PAGES
     */
    if (info->page == info->maxPages) {
	newMintermPages = ALLOC(double *, info->maxPages + INITIAL_PAGES);
	if (newMintermPages == NULL) {
	    for (i = 0; i < info->page; i++) FREE(info->mintermPages[i]);
	    FREE(info->mintermPages);
	    info->memOut = 1;
	    return;
	} else {
	    for (i = 0; i < info->maxPages; i++) {
		newMintermPages[i] = info->mintermPages[i];
	    }
	    /* Increase total page count */
	    info->maxPages += INITIAL_PAGES;
	    FREE(info->mintermPages);
	    info->mintermPages = newMintermPages;
	}
    }
    /* Allocate a new page */
    info->currentMintermPage = info->mintermPages[info->page] = ALLOC(double,info->pageSize);
    if (info->currentMintermPage == NULL) {
	for (i = 0; i < info->page; i++) FREE(info->mintermPages[i]);
	FREE(info->mintermPages);
	info->memOut = 1;
	return;
    }
    /* reset page index */
    info->pageIndex = 0;
    return;

} /* end of ResizeCountMintermPages */


/**
  @brief Resize the number of pages allocated to store the node counts.

  @details The procedure moves the counter to the next page when the
  end of the page is reached and allocates new pages when necessary.

  @sideeffect Changes the size of pages, page, page index, maximum
  number of pages freeing stuff in case of memory out.

*/
static void
ResizeCountNodePages(SubsetInfo_t * info)
{
    int i;
    int **newNodePages;

    info->page++;

    /* If the current page index is larger than the number of pages
     * allocated, allocate a new page array. The number of pages is incremented
     * by INITIAL_PAGES.
     */
    if (info->page == info->maxPages) {
	newNodePages = ALLOC(int *, info->maxPages + INITIAL_PAGES);
	if (newNodePages == NULL) {
	    for (i = 0; i < info->page; i++) FREE(info->nodePages[i]);
	    FREE(info->nodePages);
	    for (i = 0; i < info->page; i++) FREE(info->lightNodePages[i]);
	    FREE(info->lightNodePages);
	    info->memOut = 1;
	    return;
	} else {
	    for (i = 0; i < info->maxPages; i++) {
		newNodePages[i] = info->nodePages[i];
	    }
	    FREE(info->nodePages);
	    info->nodePages = newNodePages;
	}

	newNodePages = ALLOC(int *, info->maxPages + INITIAL_PAGES);
	if (newNodePages == NULL) {
	    for (i = 0; i < info->page; i++) FREE(info->nodePages[i]);
	    FREE(info->nodePages);
	    for (i = 0; i < info->page; i++) FREE(info->lightNodePages[i]);
	    FREE(info->lightNodePages);
	    info->memOut = 1;
	    return;
	} else {
	    for (i = 0; i < info->maxPages; i++) {
		newNodePages[i] = info->lightNodePages[i];
	    }
	    FREE(info->lightNodePages);
	    info->lightNodePages = newNodePages;
	}
	/* Increase total page count */
	info->maxPages += INITIAL_PAGES;
    }
    /* Allocate a new page */
    info->currentNodePage = info->nodePages[info->page] = ALLOC(int,info->pageSize);
    if (info->currentNodePage == NULL) {
	for (i = 0; i < info->page; i++) FREE(info->nodePages[i]);
	FREE(info->nodePages);
	for (i = 0; i < info->page; i++) FREE(info->lightNodePages[i]);
	FREE(info->lightNodePages);
	info->memOut = 1;
	return;
    }
    /* Allocate a new page */
    info->currentLightNodePage = info->lightNodePages[info->page]
        = ALLOC(int,info->pageSize);
    if (info->currentLightNodePage == NULL) {
	for (i = 0; i <= info->page; i++) FREE(info->nodePages[i]);
	FREE(info->nodePages);
	for (i = 0; i < info->page; i++) FREE(info->lightNodePages[i]);
	FREE(info->lightNodePages);
	info->memOut = 1;
	return;
    }
    /* reset page index */
    info->pageIndex = 0;
    return;

} /* end of ResizeCountNodePages */


/**
  @brief Recursively counts minterms of each node in the DAG.

  @details Similar to the cuddCountMintermAux which recursively counts
  the number of minterms for the dag rooted at each node in terms of
  the total number of variables (max). This procedure creates the node
  data structure and stores the minterm count as part of the node data
  structure.

  @sideeffect Creates structures of type node quality and fills the st_table

  @see SubsetCountMinterm

*/
static double
SubsetCountMintermAux(
  DdNode * node /**< function to analyze */,
  double  max /**< number of minterms of constant 1 */,
  st_table * table /**< visitedTable table */,
  SubsetInfo_t * info /**< miscellaneous info */)
{

    DdNode	*N,*Nv,*Nnv; /* nodes to store cofactors  */
    double	min,*pmin; /* minterm count */
    double	min1, min2; /* minterm count */
    NodeData_t *dummy;
    NodeData_t *newEntry;
    int i;

#ifdef DEBUG
    info->num_calls++;
#endif

    /* Constant case */
    if (Cudd_IsConstantInt(node)) {
	if (node == info->zero) {
	    return(0.0);
	} else {
	    return(max);
	}
    } else {

	/* check if entry for this node exists */
        if (st_lookup(table, node, (void **) &dummy)) {
	    min = *(dummy->mintermPointer);
	    return(min);
	}

	/* Make the node regular to extract cofactors */
	N = Cudd_Regular(node);

	/* store the cofactors */
	Nv = Cudd_T(N);
	Nnv = Cudd_E(N);

	Nv = Cudd_NotCond(Nv, Cudd_IsComplement(node));
	Nnv = Cudd_NotCond(Nnv, Cudd_IsComplement(node));

	min1 =  SubsetCountMintermAux(Nv, max,table,info)/2.0;
	if (info->memOut) return(0.0);
	min2 =  SubsetCountMintermAux(Nnv,max,table,info)/2.0;
	if (info->memOut) return(0.0);
	min = (min1+min2);

	/* if page index is at the bottom, then create a new page */
	if (info->pageIndex == info->pageSize) ResizeCountMintermPages(info);
	if (info->memOut) {
	    for (i = 0; i <= info->nodeDataPage; i++) FREE(info->nodeDataPages[i]);
	    FREE(info->nodeDataPages);
	    st_free_table(table);
	    return(0.0);
	}

	/* point to the correct location in the page */
	pmin = info->currentMintermPage + info->pageIndex;
	info->pageIndex++;

	/* store the minterm count of this node in the page */
	*pmin = min;

	/* Note I allocate the struct here. Freeing taken care of later */
	if (info->nodeDataPageIndex == info->nodeDataPageSize)
            ResizeNodeDataPages(info);
	if (info->memOut) {
	    for (i = 0; i <= info->page; i++) FREE(info->mintermPages[i]);
	    FREE(info->mintermPages);
	    st_free_table(table);
	    return(0.0);
	}

	newEntry = info->currentNodeDataPage + info->nodeDataPageIndex;
	info->nodeDataPageIndex++;

	/* points to the correct location in the page */
	newEntry->mintermPointer = pmin;
	/* initialize this field of the Node Quality structure */
	newEntry->nodesPointer = NULL;

	/* insert entry for the node in the table */
	if (st_insert(table,node, newEntry) == ST_OUT_OF_MEM) {
	    info->memOut = 1;
	    for (i = 0; i <= info->page; i++) FREE(info->mintermPages[i]);
	    FREE(info->mintermPages);
	    for (i = 0; i <= info->nodeDataPage; i++) FREE(info->nodeDataPages[i]);
	    FREE(info->nodeDataPages);
	    st_free_table(table);
	    return(0.0);
	}
	return(min);
    }

} /* end of SubsetCountMintermAux */


/**
  @brief Counts minterms of each node in the DAG

  @details Similar to the Cudd_CountMinterm procedure except this
  returns the minterm count for all the nodes in the bdd in an
  st_table.

  @sideeffect none

  @see SubsetCountMintermAux

*/
static st_table *
SubsetCountMinterm(
  DdNode * node /**< function to be analyzed */,
  int nvars /**< number of variables node depends on */,
  SubsetInfo_t * info /**< miscellaneous info */)
{
    st_table	*table;
    int i;


#ifdef DEBUG
    info->num_calls = 0;
#endif

    info->max = pow(2.0,(double) nvars);
    table = st_init_table(st_ptrcmp,st_ptrhash);
    if (table == NULL) goto OUT_OF_MEM;
    info->maxPages = INITIAL_PAGES;
    info->mintermPages = ALLOC(double *,info->maxPages);
    if (info->mintermPages == NULL) {
	st_free_table(table);
	goto OUT_OF_MEM;
    }
    info->page = 0;
    info->currentMintermPage = ALLOC(double,info->pageSize);
    info->mintermPages[info->page] = info->currentMintermPage;
    if (info->currentMintermPage == NULL) {
	FREE(info->mintermPages);
	st_free_table(table);
	goto OUT_OF_MEM;
    }
    info->pageIndex = 0;
    info->maxNodeDataPages = INITIAL_PAGES;
    info->nodeDataPages = ALLOC(NodeData_t *, info->maxNodeDataPages);
    if (info->nodeDataPages == NULL) {
	for (i = 0; i <= info->page ; i++) FREE(info->mintermPages[i]);
	FREE(info->mintermPages);
	st_free_table(table);
	goto OUT_OF_MEM;
    }
    info->nodeDataPage = 0;
    info->currentNodeDataPage = ALLOC(NodeData_t ,info->nodeDataPageSize);
    info->nodeDataPages[info->nodeDataPage] = info->currentNodeDataPage;
    if (info->currentNodeDataPage == NULL) {
	for (i = 0; i <= info->page ; i++) FREE(info->mintermPages[i]);
	FREE(info->mintermPages);
	FREE(info->nodeDataPages);
	st_free_table(table);
	goto OUT_OF_MEM;
    }
    info->nodeDataPageIndex = 0;

    (void) SubsetCountMintermAux(node,info->max,table,info);
    if (info->memOut) goto OUT_OF_MEM;
    return(table);

OUT_OF_MEM:
    info->memOut = 1;
    return(NULL);

} /* end of SubsetCountMinterm */


/**
  @brief Recursively counts the number of nodes under the dag.
  Also counts the number of nodes under the lighter child of
  this node.

  @details Note that the same dag may be the lighter child of two
  different nodes and have different counts. As with the minterm
  counts, the node counts are stored in pages to be space efficient
  and the address for these node counts are stored in an st_table
  associated to each node.

  @sideeffect Updates the node data table with node counts

  @see SubsetCountNodes

*/
static int
SubsetCountNodesAux(
  DdNode * node /**< current node */,
  st_table * table /**< table to update node count, also serves as visited table. */,
  double  max /**< maximum number of variables */,
  SubsetInfo_t * info)
{
    int tval, eval, i;
    DdNode *N, *Nv, *Nnv;
    double minNv, minNnv;
    NodeData_t *dummyN, *dummyNv, *dummyNnv, *dummyNBar;
    int *pmin, *pminBar, *val;

    if ((node == NULL) || Cudd_IsConstantInt(node))
	return(0);

    /* if this node has been processed do nothing */
    if (st_lookup(table, node, (void **) &dummyN) == 1) {
	val = dummyN->nodesPointer;
	if (val != NULL)
	    return(0);
    } else {
	return(0);
    }

    N  = Cudd_Regular(node);
    Nv = Cudd_T(N);
    Nnv = Cudd_E(N);

    Nv = Cudd_NotCond(Nv, Cudd_IsComplement(node));
    Nnv = Cudd_NotCond(Nnv, Cudd_IsComplement(node));

    /* find the minterm counts for the THEN and ELSE branches */
    if (Cudd_IsConstantInt(Nv)) {
	if (Nv == info->zero) {
	    minNv = 0.0;
	} else {
	    minNv = max;
	}
    } else {
	if (st_lookup(table, Nv, (void **) &dummyNv) == 1)
	    minNv = *(dummyNv->mintermPointer);
	else {
	    return(0);
	}
    }
    if (Cudd_IsConstantInt(Nnv)) {
	if (Nnv == info->zero) {
	    minNnv = 0.0;
	} else {
	    minNnv = max;
	}
    } else {
	if (st_lookup(table, Nnv, (void **) &dummyNnv) == 1) {
	    minNnv = *(dummyNnv->mintermPointer);
	}
	else {
	    return(0);
	}
    }


    /* recur based on which has larger minterm, */
    if (minNv >= minNnv) {
	tval = SubsetCountNodesAux(Nv, table, max, info);
	if (info->memOut) return(0);
	eval = SubsetCountNodesAux(Nnv, table, max, info);
	if (info->memOut) return(0);

	/* store the node count of the lighter child. */
	if (info->pageIndex == info->pageSize) ResizeCountNodePages(info);
	if (info->memOut) {
	    for (i = 0; i <= info->page; i++) FREE(info->mintermPages[i]);
	    FREE(info->mintermPages);
	    for (i = 0; i <= info->nodeDataPage; i++) FREE(info->nodeDataPages[i]);
	    FREE(info->nodeDataPages);
	    st_free_table(table);
	    return(0);
	}
	pmin = info->currentLightNodePage + info->pageIndex;
	*pmin = eval; /* Here the ELSE child is lighter */
	dummyN->lightChildNodesPointer = pmin;

    } else {
	eval = SubsetCountNodesAux(Nnv, table, max, info);
	if (info->memOut) return(0);
	tval = SubsetCountNodesAux(Nv, table, max, info);
	if (info->memOut) return(0);

	/* store the node count of the lighter child. */
	if (info->pageIndex == info->pageSize) ResizeCountNodePages(info);
	if (info->memOut) {
	    for (i = 0; i <= info->page; i++) FREE(info->mintermPages[i]);
	    FREE(info->mintermPages);
	    for (i = 0; i <= info->nodeDataPage; i++) FREE(info->nodeDataPages[i]);
	    FREE(info->nodeDataPages);
	    st_free_table(table);
	    return(0);
	}
	pmin = info->currentLightNodePage + info->pageIndex;
	*pmin = tval; /* Here the THEN child is lighter */
	dummyN->lightChildNodesPointer = pmin;

    }
    /* updating the page index for node count storage. */
    pmin = info->currentNodePage + info->pageIndex;
    *pmin = tval + eval + 1;
    dummyN->nodesPointer = pmin;

    /* pageIndex is parallel page index for count_nodes and count_lightNodes */
    info->pageIndex++;

    /* if this node has been reached first, it belongs to a heavier
       branch. Its complement will be reached later on a lighter branch.
       Hence the complement has zero node count. */

    if (st_lookup(table, Cudd_Not(node), (void **) &dummyNBar) == 1)  {
	if (info->pageIndex == info->pageSize) ResizeCountNodePages(info);
	if (info->memOut) {
	    for (i = 0; i < info->page; i++) FREE(info->mintermPages[i]);
	    FREE(info->mintermPages);
	    for (i = 0; i < info->nodeDataPage; i++) FREE(info->nodeDataPages[i]);
	    FREE(info->nodeDataPages);
	    st_free_table(table);
	    return(0);
	}
	pminBar = info->currentLightNodePage + info->pageIndex;
	*pminBar = 0;
	dummyNBar->lightChildNodesPointer = pminBar;
	/* The lighter child has less nodes than the parent.
	 * So if parent 0 then lighter child zero
	 */
	if (info->pageIndex == info->pageSize) ResizeCountNodePages(info);
	if (info->memOut) {
	    for (i = 0; i < info->page; i++) FREE(info->mintermPages[i]);
	    FREE(info->mintermPages);
	    for (i = 0; i < info->nodeDataPage; i++) FREE(info->nodeDataPages[i]);
	    FREE(info->nodeDataPages);
	    st_free_table(table);
	    return(0);
	}
	pminBar = info->currentNodePage + info->pageIndex;
	*pminBar = 0;
	dummyNBar->nodesPointer = pminBar ; /* maybe should point to zero */

	info->pageIndex++;
    }
    return(*pmin);
} /*end of SubsetCountNodesAux */


/**
  @brief Counts the nodes under the current node and its lighter child.

  @details Calls a recursive procedure to count the number of nodes of
  a DAG rooted at a particular node and the number of nodes taken by
  its lighter child.

  @sideeffect None

  @see SubsetCountNodesAux

*/
static int
SubsetCountNodes(
  DdNode * node /**< function to be analyzed */,
  st_table * table /**< node quality table */,
  int  nvars /**< number of variables node depends on */,
  SubsetInfo_t * info /**< miscellaneous info */)
{
    int	num;
    int i;

#ifdef DEBUG
    info->num_calls = 0;
#endif

    info->max = pow(2.0,(double) nvars);
    info->maxPages = INITIAL_PAGES;
    info->nodePages = ALLOC(int *, info->maxPages);
    if (info->nodePages == NULL)  {
	goto OUT_OF_MEM;
    }

    info->lightNodePages = ALLOC(int *, info->maxPages);
    if (info->lightNodePages == NULL) {
	for (i = 0; i <= info->page; i++) FREE(info->mintermPages[i]);
	FREE(info->mintermPages);
	for (i = 0; i <= info->nodeDataPage; i++) FREE(info->nodeDataPages[i]);
	FREE(info->nodeDataPages);
	FREE(info->nodePages);
	goto OUT_OF_MEM;
    }

    info->page = 0;
    info->currentNodePage = info->nodePages[info->page] =
        ALLOC(int,info->pageSize);
    if (info->currentNodePage == NULL) {
	for (i = 0; i <= info->page; i++) FREE(info->mintermPages[i]);
	FREE(info->mintermPages);
	for (i = 0; i <= info->nodeDataPage; i++) FREE(info->nodeDataPages[i]);
	FREE(info->nodeDataPages);
	FREE(info->lightNodePages);
	FREE(info->nodePages);
	goto OUT_OF_MEM;
    }

    info->currentLightNodePage = info->lightNodePages[info->page] =
        ALLOC(int,info->pageSize);
    if (info->currentLightNodePage == NULL) {
	for (i = 0; i <= info->page; i++) FREE(info->mintermPages[i]);
	FREE(info->mintermPages);
	for (i = 0; i <= info->nodeDataPage; i++) FREE(info->nodeDataPages[i]);
	FREE(info->nodeDataPages);
	FREE(info->currentNodePage);
	FREE(info->lightNodePages);
	FREE(info->nodePages);
	goto OUT_OF_MEM;
    }

    info->pageIndex = 0;
    num = SubsetCountNodesAux(node,table,info->max,info);
    if (info->memOut) goto OUT_OF_MEM;
    return(num);

OUT_OF_MEM:
    info->memOut = 1;
    return(0);

} /* end of SubsetCountNodes */


/**
  @brief Procedure to recursively store nodes that are retained in the subset.

  @sideeffect None

  @see StoreNodes

*/
static void
StoreNodes(
  st_table * storeTable,
  DdManager * dd,
  DdNode * node)
{
    DdNode *N, *Nt, *Ne;
    if (Cudd_IsConstantInt(dd)) {
	return;
    }
    N = Cudd_Regular(node);
    if (st_is_member(storeTable, N)) {
	return;
    }
    cuddRef(N);
    if (st_insert(storeTable, N, NULL) == ST_OUT_OF_MEM) {
	fprintf(dd->err,"Something wrong, st_table insert failed\n");
    }

    Nt = Cudd_T(N);
    Ne = Cudd_E(N);

    StoreNodes(storeTable, dd, Nt);
    StoreNodes(storeTable, dd, Ne);
    return;

}


/**
  @brief Builds the subset %BDD using the heavy branch method.

  @details The procedure carries out the building of the subset %BDD
  starting at the root. Using the three different counts labelling each node,
  the procedure chooses the heavier branch starting from the root and keeps
  track of the number of nodes it discards at each step, thus keeping count
  of the size of the subset %BDD dynamically. Once the threshold is satisfied,
  the procedure then calls ITE to build the %BDD.

  @sideeffect None

*/
static DdNode *
BuildSubsetBdd(
  DdManager * dd /**< %DD manager */,
  DdNode * node /**< current node */,
  int * size /**< current size of the subset */,
  st_table * visitedTable /**< visited table storing all node data */,
  int threshold /**< subsetting threshold */,
  st_table * storeTable /**< store table */,
  st_table * approxTable /**< approximation table */,
  SubsetInfo_t * info /**< miscellaneous info */)
{

    DdNode *Nv, *Nnv, *N, *topv, *neW;
    double minNv, minNnv;
    NodeData_t *currNodeQual;
    NodeData_t *currNodeQualT;
    NodeData_t *currNodeQualE;
    DdNode *ThenBranch, *ElseBranch;
    int topid;
    void *dummy;

#ifdef DEBUG
    info->num_calls++;
#endif
    /*If the size of the subset is below the threshold, dont do
      anything. */
    if ((*size) <= threshold) {
      /* store nodes below this, so we can recombine if possible */
      StoreNodes(storeTable, dd, node);
      return(node);
    }

    if (Cudd_IsConstantInt(node))
	return(node);

    /* Look up minterm count for this node. */
    if (!st_lookup(visitedTable, node, (void **) &currNodeQual)) {
	fprintf(dd->err,
		"Something is wrong, ought to be in node quality table\n");
    }

    /* Get children. */
    N = Cudd_Regular(node);
    Nv = Cudd_T(N);
    Nnv = Cudd_E(N);

    /* complement if necessary */
    Nv = Cudd_NotCond(Nv, Cudd_IsComplement(node));
    Nnv = Cudd_NotCond(Nnv, Cudd_IsComplement(node));

    if (!Cudd_IsConstantInt(Nv)) {
	/* find out minterms and nodes contributed by then child */
	if (!st_lookup(visitedTable, Nv, (void **) &currNodeQualT)) {
		fprintf(dd->out,"Something wrong, couldnt find nodes in node quality table\n");
		dd->errorCode = CUDD_INTERNAL_ERROR;
		return(NULL);
	    }
	else {
	    minNv = *(((NodeData_t *)currNodeQualT)->mintermPointer);
	}
    } else {
	if (Nv == info->zero) {
	    minNv = 0;
	} else  {
	    minNv = info->max;
	}
    }
    if (!Cudd_IsConstantInt(Nnv)) {
	/* find out minterms and nodes contributed by else child */
	if (!st_lookup(visitedTable, Nnv, (void **) &currNodeQualE)) {
	    fprintf(dd->out,"Something wrong, couldnt find nodes in node quality table\n");
	    dd->errorCode = CUDD_INTERNAL_ERROR;
	    return(NULL);
	} else {
	    minNnv = *(((NodeData_t *)currNodeQualE)->mintermPointer);
	}
    } else {
	if (Nnv == info->zero) {
	    minNnv = 0;
	} else {
	    minNnv = info->max;
	}
    }

    /* keep track of size of subset by subtracting the number of
     * differential nodes contributed by lighter child
     */
    *size = (*(size)) - (int)*(currNodeQual->lightChildNodesPointer);
    if (minNv >= minNnv) { /*SubsetCountNodesAux procedure takes
			     the Then branch in case of a tie */

	/* recur with the Then branch */
	ThenBranch = (DdNode *)BuildSubsetBdd(dd, Nv, size, visitedTable,
                                              threshold, storeTable,
                                              approxTable, info);
	if (ThenBranch == NULL) {
	    return(NULL);
	}
	cuddRef(ThenBranch);
	/* The Else branch is either a node that already exists in the
	 * subset, or one whose approximation has been computed, or
	 * Zero.
	 */
	if (st_lookup(storeTable, Cudd_Regular(Nnv), &dummy)) {
            ElseBranch = Nnv;
            cuddRef(ElseBranch);
	} else {
            if (st_lookup(approxTable, Nnv, &dummy)) {
                ElseBranch = (DdNode *)dummy;
                cuddRef(ElseBranch);
            } else {
                ElseBranch = info->zero;
                cuddRef(ElseBranch);
            }
	}

    }
    else {
	/* recur with the Else branch */
	ElseBranch = (DdNode *)BuildSubsetBdd(dd, Nnv, size, visitedTable,
                                              threshold, storeTable,
                                              approxTable, info);
	if (ElseBranch == NULL) {
	    return(NULL);
	}
	cuddRef(ElseBranch);
	/* The Then branch is either a node that already exists in the
	 * subset, or one whose approximation has been computed, or
	 * Zero.
	 */
	if (st_lookup(storeTable, Cudd_Regular(Nv), &dummy)) {
            ThenBranch = Nv;
            cuddRef(ThenBranch);
	} else {
            if (st_lookup(approxTable, Nv, &dummy)) {
                ThenBranch = (DdNode *)dummy;
                cuddRef(ThenBranch);
            } else {
                ThenBranch = info->zero;
                cuddRef(ThenBranch);
            }
	}
    }

    /* construct the Bdd with the top variable and the two children */
    topid = Cudd_NodeReadIndex(N);
    topv = Cudd_ReadVars(dd, topid);
    cuddRef(topv);
    neW =  cuddBddIteRecur(dd, topv, ThenBranch, ElseBranch);
    if (neW != NULL) {
      cuddRef(neW);
    }
    Cudd_RecursiveDeref(dd, topv);
    Cudd_RecursiveDeref(dd, ThenBranch);
    Cudd_RecursiveDeref(dd, ElseBranch);


    if (neW == NULL)
	return(NULL);
    else {
	/* store this node in the store table */
	if (!st_lookup(storeTable, Cudd_Regular(neW), &dummy)) {
            cuddRef(neW);
            if (st_insert(storeTable, Cudd_Regular(neW), NULL) ==
                ST_OUT_OF_MEM)
                return (NULL);
	}
	/* store the approximation for this node */
	if (N !=  Cudd_Regular(neW)) {
	    if (st_lookup(approxTable, node, &dummy)) {
		fprintf(dd->err, "This node should not be in the approximated table\n");
	    } else {
		cuddRef(neW);
		if (st_insert(approxTable, node, neW) ==
                    ST_OUT_OF_MEM)
		    return(NULL);
	    }
	}
	cuddDeref(neW);
	return(neW);
    }
} /* end of BuildSubsetBdd */
