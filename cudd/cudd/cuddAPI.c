/**
  @file

  @ingroup cudd

  @brief Application interface functions.

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
#include "mtrInt.h"
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

static void fixVarTree (MtrNode *treenode, int *perm, int size);
static int addMultiplicityGroups (DdManager *dd, MtrNode *treenode, int multiplicity, char *vmask, char *lmask);

/** \endcond */



/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/


/**
  @brief Returns a new %ADD variable.

  @details The new variable has an index equal to the largest previous
  index plus 1.  An %ADD variable differs from a %BDD variable because
  it points to the arithmetic zero, instead of having a complement
  pointer to 1.

  @return a pointer to the new variable if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddNewVar Cudd_addIthVar Cudd_addConst
  Cudd_addNewVarAtLevel

*/
DdNode *
Cudd_addNewVar(
  DdManager * dd)
{
    DdNode *res;

    if ((unsigned int) dd->size >= CUDD_MAXINDEX - 1) {
        dd->errorCode = CUDD_INVALID_ARG;
        return(NULL);
    }
    do {
	dd->reordered = 0;
	res = cuddUniqueInter(dd,dd->size,DD_ONE(dd),DD_ZERO(dd));
    } while (dd->reordered == 1);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }

    return(res);

} /* end of Cudd_addNewVar */


/**
  @brief Returns a new %ADD variable at a specified level.

  @details The new variable has an index equal to the largest previous
  index plus 1 and is positioned at the specified level in the order.

  @return a pointer to the new variable if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_addNewVar Cudd_addIthVar Cudd_bddNewVarAtLevel

*/
DdNode *
Cudd_addNewVarAtLevel(
  DdManager * dd,
  int  level)
{
    DdNode *res;

    if ((unsigned int) dd->size >= CUDD_MAXINDEX - 1) {
        dd->errorCode = CUDD_INVALID_ARG;
        return(NULL);
    }
    if (level >= dd->size) return(Cudd_addIthVar(dd,level));
    if (!cuddInsertSubtables(dd,1,level)) return(NULL);
    do {
	dd->reordered = 0;
	res = cuddUniqueInter(dd,dd->size - 1,DD_ONE(dd),DD_ZERO(dd));
    } while (dd->reordered == 1);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }

    return(res);

} /* end of Cudd_addNewVarAtLevel */


/**
  @brief Returns a new %BDD variable.

  @details The new variable has an index equal to the largest previous
  index plus 1.

  @return a pointer to the new variable if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_addNewVar Cudd_bddIthVar Cudd_bddNewVarAtLevel

*/
DdNode *
Cudd_bddNewVar(
  DdManager * dd)
{
    DdNode *res;

    if ((unsigned int) dd->size >= CUDD_MAXINDEX - 1) {
        dd->errorCode = CUDD_INVALID_ARG;
        return(NULL);
    }
    res = cuddUniqueInter(dd,dd->size,dd->one,Cudd_Not(dd->one));

    return(res);

} /* end of Cudd_bddNewVar */


/**
  @brief Returns a new %BDD variable at a specified level.

  @details The new variable has an index equal to the largest previous
  index plus 1 and is positioned at the specified level in the order.

  @return a pointer to the new variable if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddNewVar Cudd_bddIthVar Cudd_addNewVarAtLevel

*/
DdNode *
Cudd_bddNewVarAtLevel(
  DdManager * dd,
  int  level)
{
    DdNode *res;

    if ((unsigned int) dd->size >= CUDD_MAXINDEX - 1) {
        dd->errorCode = CUDD_INVALID_ARG;
        return(NULL);
    }
    if (level >= dd->size) return(Cudd_bddIthVar(dd,level));
    if (!cuddInsertSubtables(dd,1,level)) return(NULL);
    res = dd->vars[dd->size - 1];

    return(res);

} /* end of Cudd_bddNewVarAtLevel */


/**
  @brief Returns 1 if the given node is a %BDD variable; 0 otherwise.

  @sideeffect None

*/
int
Cudd_bddIsVar(
  DdManager * dd,
  DdNode * f)
{
    DdNode *one = DD_ONE(dd);
    return(f != 0 && cuddT(f) == one && cuddE(f) == Cudd_Not(one));

} /* end of Cudd_bddIsVar */


/**
  @brief Returns the %ADD variable with index i.

  @details Retrieves the %ADD variable with index i if it already
  exists, or creates a new %ADD variable.  An %ADD variable differs from
  a %BDD variable because it points to the arithmetic zero, instead of
  having a complement pointer to 1.

  @return a pointer to the variable if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_addNewVar Cudd_bddIthVar Cudd_addConst
  Cudd_addNewVarAtLevel

*/
DdNode *
Cudd_addIthVar(
  DdManager * dd,
  int  i)
{
    DdNode *res;

    if ((unsigned int) i >= CUDD_MAXINDEX - 1) {
        dd->errorCode = CUDD_INVALID_ARG;
        return(NULL);
    }
    do {
	dd->reordered = 0;
	res = cuddUniqueInter(dd,i,DD_ONE(dd),DD_ZERO(dd));
    } while (dd->reordered == 1);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }

    return(res);

} /* end of Cudd_addIthVar */


/**
  @brief Returns the %BDD variable with index i.

  @details Retrieves the %BDD variable with index i if it already
  exists, or creates a new %BDD variable.

  @return a pointer to the variable if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddNewVar Cudd_addIthVar Cudd_bddNewVarAtLevel
  Cudd_ReadVars

*/
DdNode *
Cudd_bddIthVar(
  DdManager * dd,
  int  i)
{
    DdNode *res;

    if ((unsigned int) i >= CUDD_MAXINDEX - 1) {
        dd->errorCode = CUDD_INVALID_ARG;
        return(NULL);
    }
    if (i < dd->size) {
	res = dd->vars[i];
    } else {
	res = cuddUniqueInter(dd,i,dd->one,Cudd_Not(dd->one));
    }

    return(res);

} /* end of Cudd_bddIthVar */


/**
  @brief Returns the %ZDD variable with index i.

  @details Retrieves the %ZDD variable with index i if it already
  exists, or creates a new %ZDD variable.

  @return a pointer to the variable if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_bddIthVar Cudd_addIthVar

*/
DdNode *
Cudd_zddIthVar(
  DdManager * dd,
  int  i)
{
    DdNode *res;
    DdNode *zvar;
    DdNode *lower;
    int j;

    if ((unsigned int) i >= CUDD_MAXINDEX - 1) {
        dd->errorCode = CUDD_INVALID_ARG;
        return(NULL);
    }
    /* The i-th variable function has the following structure:
    ** at the level corresponding to index i there is a node whose "then"
    ** child points to the universe, and whose "else" child points to zero.
    ** Above that level there are nodes with identical children.
    */

    /* First we build the node at the level of index i. */
    lower = (i < dd->sizeZ - 1) ? dd->univ[dd->permZ[i]+1] : DD_ONE(dd);
    do {
	dd->reordered = 0;
	zvar = cuddUniqueInterZdd(dd, i, lower, DD_ZERO(dd));
    } while (dd->reordered == 1);
    if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
        dd->timeoutHandler(dd, dd->tohArg);
    }

    if (zvar == NULL)
	return(NULL);
    cuddRef(zvar);

    /* Now we add the "filler" nodes above the level of index i. */
    for (j = dd->permZ[i] - 1; j >= 0; j--) {
	do {
	    dd->reordered = 0;
	    res = cuddUniqueInterZdd(dd, dd->invpermZ[j], zvar, zvar);
	} while (dd->reordered == 1);
	if (res == NULL) {
	    Cudd_RecursiveDerefZdd(dd,zvar);
            if (dd->errorCode == CUDD_TIMEOUT_EXPIRED && dd->timeoutHandler) {
                dd->timeoutHandler(dd, dd->tohArg);
            }
	    return(NULL);
	}
	cuddRef(res);
	Cudd_RecursiveDerefZdd(dd,zvar);
	zvar = res;
    }
    cuddDeref(zvar);
    return(zvar);

} /* end of Cudd_zddIthVar */


/**
  @brief Creates one or more %ZDD variables for each %BDD variable.

  @details If some %ZDD variables already exist, only the missing
  variables are created.  Parameter multiplicity allows the caller to
  control how many variables are created for each %BDD variable in
  existence. For instance, if ZDDs are used to represent covers, two
  %ZDD variables are required for each %BDD variable.  The order of the
  %BDD variables is transferred to the %ZDD variables. If a variable
  group tree exists for the %BDD variables, a corresponding %ZDD
  variable group tree is created by expanding the %BDD variable
  tree. In any case, the %ZDD variables derived from the same %BDD
  variable are merged in a %ZDD variable group. If a %ZDD variable group
  tree exists, it is freed.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see Cudd_bddNewVar Cudd_bddIthVar Cudd_bddNewVarAtLevel

*/
int
Cudd_zddVarsFromBddVars(
  DdManager * dd /**< %DD manager */,
  int multiplicity /**< how many %ZDD variables are created for each %BDD variable */)
{
    int res;
    int i, j;
    int allnew;
    int *permutation;

    if (multiplicity < 1) {
        dd->errorCode = CUDD_INVALID_ARG;
        return(0);
    }
    allnew = dd->sizeZ == 0;
    if (dd->size * multiplicity > dd->sizeZ) {
	res = cuddResizeTableZdd(dd,dd->size * multiplicity - 1);
	if (res == 0) return(0);
    }
    /* Impose the order of the BDD variables to the ZDD variables. */
    if (allnew) {
	for (i = 0; i < dd->size; i++) {
	    for (j = 0; j < multiplicity; j++) {
		dd->permZ[i * multiplicity + j] =
		    dd->perm[i] * multiplicity + j;
		dd->invpermZ[dd->permZ[i * multiplicity + j]] =
		    i * multiplicity + j;
	    }
	}
	for (i = 0; i < dd->sizeZ; i++) {
	    dd->univ[i]->index = dd->invpermZ[i];
	}
    } else {
	permutation = ALLOC(int,dd->sizeZ);
	if (permutation == NULL) {
	    dd->errorCode = CUDD_MEMORY_OUT;
	    return(0);
	}
	for (i = 0; i < dd->size; i++) {
	    for (j = 0; j < multiplicity; j++) {
		permutation[i * multiplicity + j] =
		    dd->invperm[i] * multiplicity + j;
	    }
	}
	for (i = dd->size * multiplicity; i < dd->sizeZ; i++) {
	    permutation[i] = i;
	}
	res = Cudd_zddShuffleHeap(dd, permutation);
	FREE(permutation);
	if (res == 0) return(0);
    }
    /* Copy and expand the variable group tree if it exists. */
    if (dd->treeZ != NULL) {
	Cudd_FreeZddTree(dd);
    }
    if (dd->tree != NULL) {
	dd->treeZ = Mtr_CopyTree(dd->tree, multiplicity);
	if (dd->treeZ == NULL) return(0);
    } else if (multiplicity > 1) {
	dd->treeZ = Mtr_InitGroupTree(0, dd->sizeZ);
	if (dd->treeZ == NULL) return(0);
	dd->treeZ->index = dd->invpermZ[0];
    }
    /* Create groups for the ZDD variables derived from the same BDD variable.
    */
    if (multiplicity > 1) {
	char *vmask, *lmask;

	vmask = ALLOC(char, dd->size);
	if (vmask == NULL) {
	    dd->errorCode = CUDD_MEMORY_OUT;
	    return(0);
	}
	lmask =  ALLOC(char, dd->size);
	if (lmask == NULL) {
	    dd->errorCode = CUDD_MEMORY_OUT;
	    return(0);
	}
	for (i = 0; i < dd->size; i++) {
	    vmask[i] = lmask[i] = 0;
	}
	res = addMultiplicityGroups(dd,dd->treeZ,multiplicity,vmask,lmask);
	FREE(vmask);
	FREE(lmask);
	if (res == 0) return(0);
    }
    return(1);

} /* end of Cudd_zddVarsFromBddVars */


/**
  @brief Returns the maximum possible index for a variable.

  @sideeffect None
*/
unsigned int
Cudd_ReadMaxIndex(void)
{
    return(CUDD_MAXINDEX);

} /* end of Cudd_ReadMaxIndex */


/**
  @brief Returns the %ADD for constant c.

  @details Retrieves the %ADD for constant c if it already
  exists, or creates a new %ADD.

  @return a pointer to the %ADD if successful; NULL otherwise.

  @sideeffect None

  @see Cudd_addNewVar Cudd_addIthVar

*/
DdNode *
Cudd_addConst(
  DdManager * dd,
  CUDD_VALUE_TYPE  c)
{
    return(cuddUniqueConst(dd,c));

} /* end of Cudd_addConst */


/**
  @brief Returns 1 if the node is a constant node.

  @details A constant node is not an internal node.  The pointer
  passed to Cudd_IsConstant may be either regular or complemented.

  @sideeffect none

*/
int Cudd_IsConstant(DdNode *node)
{
    return Cudd_Regular(node)->index == CUDD_CONST_INDEX;

} /* end of Cudd_IsConstant */


/**
  @brief Returns 1 if a %DD node is not constant.

  @details This function is useful to test the results of
  Cudd_bddIteConstant, Cudd_addIteConstant, Cudd_addEvalConst. These
  results may be a special value signifying non-constant. In the other
  cases Cudd_IsConstant can be used.

  @sideeffect None

  @see Cudd_IsConstant Cudd_bddIteConstant Cudd_addIteConstant
  Cudd_addEvalConst

*/
int
Cudd_IsNonConstant(
  DdNode *f)
{
    return(f == DD_NON_CONSTANT || !Cudd_IsConstantInt(f));

} /* end of Cudd_IsNonConstant */


/**
  @brief Returns the then child of an internal node.

  @details If <code>node</code> is a constant node, the result is
  unpredictable.

  @sideeffect none

  @see Cudd_E Cudd_V

*/
DdNode *
Cudd_T(DdNode *node)
{
    return Cudd_Regular(node)->type.kids.T;

} /* end of Cudd_T */


/**
  @brief Returns the else child of an internal node.

  @details If <code>node</code> is a constant node, the result is
  unpredictable.

  @sideeffect none

  @see Cudd_T Cudd_V

*/
DdNode *
Cudd_E(DdNode *node)
{
    return Cudd_Regular(node)->type.kids.E;

} /* end of Cudd_E */


/**
  @brief Returns the value of a constant node.

  @details If <code>node</code> is an internal node, the result is
  unpredictable.

  @sideeffect none

  @see Cudd_T Cudd_E

*/
CUDD_VALUE_TYPE
Cudd_V(DdNode *node)
{
    return Cudd_Regular(node)->type.value;

} /* end of Cudd_V */


/**
  @brief Returns the start time of the manager.

  @details This is initially set to the number of milliseconds since
  the program started, but may be reset by the application.

  @sideeffect None

  @see Cudd_SetStartTime Cudd_ResetStartTime Cudd_ReadTimeLimit

*/
unsigned long
Cudd_ReadStartTime(
  DdManager * unique)
{
    return unique->startTime;

} /* end of Cudd_ReadStartTime */


/**
  @brief Returns the time elapsed since the start time of the manager.

  @details The time is expressed in milliseconds.

  @sideeffect None

  @see Cudd_ReadStartTime Cudd_SetStartTime

*/
unsigned long
Cudd_ReadElapsedTime(
  DdManager * unique)
{
  return util_cpu_time() - unique->startTime;

} /* end of Cudd_ReadElapsedTime */


/**
  @brief Sets the start time of the manager.

  @details The time must be expressed in milliseconds.

  @sideeffect None

  @see Cudd_ReadStartTime Cudd_ResetStartTime Cudd_ReadElapsedTime
  Cudd_SetTimeLimit

*/
void
Cudd_SetStartTime(
  DdManager * unique,
  unsigned long st)
{
    unique->startTime = st;

} /* end of Cudd_SetStartTime */


/**
  @brief Resets the start time of the manager.

  @sideeffect None

  @see Cudd_ReadStartTime Cudd_SetStartTime Cudd_SetTimeLimit

*/
void
Cudd_ResetStartTime(
  DdManager * unique)
{
    unique->startTime = util_cpu_time();

} /* end of Cudd_ResetStartTime */


/**
  @brief Returns the time limit for the manager.

  @details This is initially set to a very large number, but may be
  reset by the application.  The time is expressed in milliseconds.

  @sideeffect None

  @see Cudd_SetTimeLimit Cudd_UpdateTimeLimit Cudd_UnsetTimeLimit
  Cudd_IncreaseTimeLimit Cudd_TimeLimited Cudd_ReadStartTime

*/
unsigned long
Cudd_ReadTimeLimit(
  DdManager * unique)
{
    return unique->timeLimit;

} /* end of Cudd_ReadTimeLimit */


/**
  @brief Sets the time limit for the manager.

  @details The time must be expressed in milliseconds.

  @return the old time limit.

  @sideeffect None

  @see Cudd_ReadTimeLimit Cudd_UnsetTimeLimit Cudd_UpdateTimeLimit
  Cudd_IncreaseTimeLimit Cudd_TimeLimited Cudd_SetStartTime

*/
unsigned long
Cudd_SetTimeLimit(
  DdManager * unique,
  unsigned long tl)
{
    unsigned long ret = unique->timeLimit;
    unique->timeLimit = tl;
    return(ret);

} /* end of Cudd_SetTimeLimit */


/**
  @brief Updates the time limit for the manager.

  @details Updates the time limit for the manager by subtracting the
  elapsed time from it.

  @sideeffect None

  @see Cudd_ReadTimeLimit Cudd_SetTimeLimit Cudd_UnsetTimeLimit
  Cudd_IncreaseTimeLimit Cudd_TimeLimited Cudd_SetStartTime

*/
void
Cudd_UpdateTimeLimit(
  DdManager * unique)
{
    unsigned long elapsed;
    if (unique->timeLimit == ~0UL)
        return;
    elapsed = util_cpu_time() - unique->startTime;
    if (unique->timeLimit >= elapsed) {
        unique->timeLimit -= elapsed;
    } else {
        unique->timeLimit = 0;
    }

} /* end of Cudd_UpdateTimeLimit */


/**
  @brief Increases the time limit for the manager.

  @details The time increase must be expressed in milliseconds.

  @sideeffect None

  @see Cudd_ReadTimeLimit Cudd_SetTimeLimit Cudd_UnsetTimeLimit
  Cudd_UpdateTimeLimit Cudd_TimeLimited Cudd_SetStartTime

*/
void
Cudd_IncreaseTimeLimit(
  DdManager * unique,
  unsigned long increase)
{
    if (unique->timeLimit == ~0UL)
        unique->timeLimit = increase;
    else
        unique->timeLimit += increase;

} /* end of Cudd_IncreaseTimeLimit */


/**
  @brief Unsets the time limit for the manager.

  @details Actually, sets it to a very large value.

  @sideeffect None

  @see Cudd_ReadTimeLimit Cudd_SetTimeLimit Cudd_UpdateTimeLimit
  Cudd_IncreaseTimeLimit Cudd_TimeLimited Cudd_SetStartTime

*/
void
Cudd_UnsetTimeLimit(
  DdManager * unique)
{
    unique->timeLimit = ~0UL;

} /* end of Cudd_UnsetTimeLimit */


/**
  @brief Returns true if the time limit for the manager is set.

  @sideeffect None

  @see Cudd_ReadTimeLimit Cudd_SetTimeLimit Cudd_UpdateTimeLimit
  Cudd_UnsetTimeLimit Cudd_IncreaseTimeLimit

*/
int
Cudd_TimeLimited(
  DdManager * unique)
{
    return unique->timeLimit != ~0UL;

} /* end of Cudd_TimeLimited */


/**
  @brief Installs a termination callback.

  @details Registers a callback function that is called from time
  to time to decide whether computation should be abandoned.

  @sideeffect None

  @see Cudd_UnregisterTerminationCallback

*/
void
Cudd_RegisterTerminationCallback(
  DdManager *unique,
  DD_THFP callback,
  void * callback_arg)
{
    unique->terminationCallback = callback;
    unique->tcbArg = callback_arg;

} /* end of Cudd_RegisterTerminationCallback */


/**
  @brief Unregisters a termination callback.

  @sideeffect None

  @see Cudd_RegisterTerminationCallback

*/
void
Cudd_UnregisterTerminationCallback(
  DdManager *unique)
{
    unique->terminationCallback = NULL;
    unique->tcbArg = NULL;

}  /* end of Cudd_UnregisterTerminationCallback */


/**
  @brief Installs an out-of-memory callback.

  @details Registers a callback function that is called when
  a discretionary memory allocation fails.

  @return the old callback function.

  @sideeffect None

  @see Cudd_UnregisterOutOfMemoryCallback Cudd_OutOfMem Cudd_OutOfMemSilent

*/
DD_OOMFP
Cudd_RegisterOutOfMemoryCallback(
  DdManager *unique,
  DD_OOMFP callback)
{
  DD_OOMFP ret = unique->outOfMemCallback;
  unique->outOfMemCallback = callback;
  return(ret);

} /* end of Cudd_RegisterOutOfMemoryCallback */


/**
  @brief Unregister an out-of-memory callback.

  @sideeffect None

  @see Cudd_RegisterOutOfMemoryCallback Cudd_OutOfMem Cudd_OutOfMemSilent

*/
void
Cudd_UnregisterOutOfMemoryCallback(
  DdManager *unique)
{
  unique->outOfMemCallback = Cudd_OutOfMemSilent;

} /* end of Cudd_UnregisterOutOfMemoryCallback */


/**
  @brief Register a timeout handler function.

  @details To unregister a handler, register a NULL pointer.

  @sideeffect None

  @see Cudd_ReadTimeoutHandler
*/
void
Cudd_RegisterTimeoutHandler(
  DdManager *unique,
  DD_TOHFP handler,
  void *arg)
{
    unique->timeoutHandler = handler;
    unique->tohArg = arg;

} /* end of Cudd_RegisterTimeoutHandler */


/**
  @brief Read the current timeout handler function.

  @sideeffect If argp is non-null, the second argument to
  the handler is written to the location it points to.

  @see Cudd_RegisterTimeoutHandler
*/
DD_TOHFP
Cudd_ReadTimeoutHandler(
  DdManager *unique,
  void **argp)
{
    if (argp != NULL)
        *argp = unique->tohArg;
    return unique->timeoutHandler;

} /* end of Cudd_ReadTimeoutHandler */


/**
  @brief Enables automatic dynamic reordering of BDDs and ADDs.

  @details Parameter method is used to determine the method used for
  reordering. If CUDD_REORDER_SAME is passed, the method is unchanged.

  @sideeffect None

  @see Cudd_AutodynDisable Cudd_ReorderingStatus
  Cudd_AutodynEnableZdd

*/
void
Cudd_AutodynEnable(
  DdManager * unique,
  Cudd_ReorderingType  method)
{
    unique->autoDyn = 1;
    if (method != CUDD_REORDER_SAME) {
	unique->autoMethod = method;
    }
#ifndef DD_NO_DEATH_ROW
    /* If reordering is enabled, using the death row causes too many
    ** invocations. Hence, we shrink the death row to just one entry.
    */
    cuddClearDeathRow(unique);
    unique->deathRowDepth = 1;
    unique->deadMask = unique->deathRowDepth - 1;
    if ((unsigned) unique->nextDead > unique->deadMask) {
	unique->nextDead = 0;
    }
    unique->deathRow = REALLOC(DdNodePtr, unique->deathRow,
	unique->deathRowDepth);
#endif
    return;

} /* end of Cudd_AutodynEnable */


/**
  @brief Disables automatic dynamic reordering.

  @sideeffect None

  @see Cudd_AutodynEnable Cudd_ReorderingStatus
  Cudd_AutodynDisableZdd

*/
void
Cudd_AutodynDisable(
  DdManager * unique)
{
    unique->autoDyn = 0;
    return;

} /* end of Cudd_AutodynDisable */


/**
  @brief Reports the status of automatic dynamic reordering of BDDs
  and ADDs.

  @details The location pointed by parameter method is set to
  the reordering method currently selected if method is non-null.

  @return 1 if automatic reordering is enabled; 0 otherwise.

  @sideeffect The location pointed by parameter method is set to the
  reordering method currently selected if method is non-null.

  @see Cudd_AutodynEnable Cudd_AutodynDisable
  Cudd_ReorderingStatusZdd

*/
int
Cudd_ReorderingStatus(
  DdManager * unique,
  Cudd_ReorderingType * method)
{
    if (method)
	*method = unique->autoMethod;
    return(unique->autoDyn);

} /* end of Cudd_ReorderingStatus */


/**
  @brief Enables automatic dynamic reordering of ZDDs.

  @details Parameter method is used to determine the method used for
  reordering ZDDs.  If CUDD_REORDER_SAME is passed, the method is
  unchanged.

  @sideeffect None

  @see Cudd_AutodynDisableZdd Cudd_ReorderingStatusZdd
  Cudd_AutodynEnable

*/
void
Cudd_AutodynEnableZdd(
  DdManager * unique,
  Cudd_ReorderingType method)
{
    unique->autoDynZ = 1;
    if (method != CUDD_REORDER_SAME) {
	unique->autoMethodZ = method;
    }
    return;

} /* end of Cudd_AutodynEnableZdd */


/**
  @brief Disables automatic dynamic reordering of ZDDs.

  @sideeffect None

  @see Cudd_AutodynEnableZdd Cudd_ReorderingStatusZdd
  Cudd_AutodynDisable

*/
void
Cudd_AutodynDisableZdd(
  DdManager * unique)
{
    unique->autoDynZ = 0;
    return;

} /* end of Cudd_AutodynDisableZdd */


/**
  @brief Reports the status of automatic dynamic reordering of ZDDs.

  @details Parameter method is set to the %ZDD reordering method currently
  selected.

  @return 1 if automatic reordering is enabled; 0 otherwise.

  @sideeffect Parameter method is set to the %ZDD reordering method currently
  selected.

  @see Cudd_AutodynEnableZdd Cudd_AutodynDisableZdd
  Cudd_ReorderingStatus

*/
int
Cudd_ReorderingStatusZdd(
  DdManager * unique,
  Cudd_ReorderingType * method)
{
    *method = unique->autoMethodZ;
    return(unique->autoDynZ);

} /* end of Cudd_ReorderingStatusZdd */


/**
  @brief Tells whether the realignment of %ZDD order to %BDD order is
  enabled.

  @return 1 if the realignment of %ZDD order to %BDD order is enabled; 0
  otherwise.

  @sideeffect None

  @see Cudd_zddRealignEnable Cudd_zddRealignDisable
  Cudd_bddRealignEnable Cudd_bddRealignDisable

*/
int
Cudd_zddRealignmentEnabled(
  DdManager * unique)
{
    return(unique->realign);

} /* end of Cudd_zddRealignmentEnabled */


/**
  @brief Enables realignment of %ZDD order to %BDD order.

  @details Enables realignment of the %ZDD variable order to the
  %BDD variable order after the BDDs and ADDs have been reordered.  The
  number of %ZDD variables must be a multiple of the number of %BDD
  variables for realignment to make sense. If this condition is not met,
  Cudd_ReduceHeap will return 0. Let <code>M</code> be the
  ratio of the two numbers. For the purpose of realignment, the %ZDD
  variables from <code>M*i</code> to <code>(M+1)*i-1</code> are
  reagarded as corresponding to %BDD variable <code>i</code>. Realignment
  is initially disabled.

  @sideeffect None

  @see Cudd_ReduceHeap Cudd_zddRealignDisable
  Cudd_zddRealignmentEnabled Cudd_bddRealignDisable
  Cudd_bddRealignmentEnabled

*/
void
Cudd_zddRealignEnable(
  DdManager * unique)
{
    unique->realign = 1;
    return;

} /* end of Cudd_zddRealignEnable */


/**
  @brief Disables realignment of %ZDD order to %BDD order.

  @sideeffect None

  @see Cudd_zddRealignEnable Cudd_zddRealignmentEnabled
  Cudd_bddRealignEnable Cudd_bddRealignmentEnabled

*/
void
Cudd_zddRealignDisable(
  DdManager * unique)
{
    unique->realign = 0;
    return;

} /* end of Cudd_zddRealignDisable */


/**
  @brief Tells whether the realignment of %BDD order to %ZDD order is
  enabled.

  @return 1 if the realignment of %BDD order to %ZDD order is enabled; 0
  otherwise.

  @sideeffect None

  @see Cudd_bddRealignEnable Cudd_bddRealignDisable
  Cudd_zddRealignEnable Cudd_zddRealignDisable

*/
int
Cudd_bddRealignmentEnabled(
  DdManager * unique)
{
    return(unique->realignZ);

} /* end of Cudd_bddRealignmentEnabled */


/**
  @brief Enables realignment of %BDD order to %ZDD order.

  @details Enables realignment of the %BDD variable order to the
  %ZDD variable order after the ZDDs have been reordered.  The
  number of %ZDD variables must be a multiple of the number of %BDD
  variables for realignment to make sense. If this condition is not met,
  Cudd_zddReduceHeap will return 0. Let <code>M</code> be the
  ratio of the two numbers. For the purpose of realignment, the %ZDD
  variables from <code>M*i</code> to <code>(M+1)*i-1</code> are
  reagarded as corresponding to %BDD variable <code>i</code>. Realignment
  is initially disabled.

  @sideeffect None

  @see Cudd_zddReduceHeap Cudd_bddRealignDisable
  Cudd_bddRealignmentEnabled Cudd_zddRealignDisable
  Cudd_zddRealignmentEnabled

*/
void
Cudd_bddRealignEnable(
  DdManager * unique)
{
    unique->realignZ = 1;
    return;

} /* end of Cudd_bddRealignEnable */


/**
  @brief Disables realignment of %ZDD order to %BDD order.

  @sideeffect None

  @see Cudd_bddRealignEnable Cudd_bddRealignmentEnabled
  Cudd_zddRealignEnable Cudd_zddRealignmentEnabled

*/
void
Cudd_bddRealignDisable(
  DdManager * unique)
{
    unique->realignZ = 0;
    return;

} /* end of Cudd_bddRealignDisable */


/**
  @brief Returns the one constant of the manager.

  @details The one constant is common to ADDs and BDDs.

  @sideeffect None

  @see Cudd_ReadZero Cudd_ReadLogicZero Cudd_ReadZddOne

*/
DdNode *
Cudd_ReadOne(
  DdManager * dd)
{
    return(dd->one);

} /* end of Cudd_ReadOne */


/**
  @brief Returns the %ZDD for the constant 1 function.

  @details The representation of the constant 1 function as a %ZDD
  depends on how many variables it (nominally) depends on. The index
  of the topmost variable in the support is given as argument
  <code>i</code>.

  @sideeffect None

  @see Cudd_ReadOne

*/
DdNode *
Cudd_ReadZddOne(
  DdManager * dd,
  int  i)
{
    if (i < 0)
	return(NULL);
    return(i < dd->sizeZ ? dd->univ[i] : DD_ONE(dd));

} /* end of Cudd_ReadZddOne */



/**
  @brief Returns the zero constant of the manager.

  @details The zero constant is the arithmetic zero, rather than the
  logic zero. The latter is the complement of the one constant.

  @sideeffect None

  @see Cudd_ReadOne Cudd_ReadLogicZero

*/
DdNode *
Cudd_ReadZero(
  DdManager * dd)
{
    return(DD_ZERO(dd));

} /* end of Cudd_ReadZero */


/**
  @brief Returns the logic zero constant of the manager.

  @details The logic zero constant is the complement of the one
  constant, and is distinct from the arithmetic zero.

  @sideeffect None

  @see Cudd_ReadOne Cudd_ReadZero

*/
DdNode *
Cudd_ReadLogicZero(
  DdManager * dd)
{
    return(Cudd_Not(DD_ONE(dd)));

} /* end of Cudd_ReadLogicZero */


/**
  @brief Reads the plus-infinity constant from the manager.

  @sideeffect None

*/
DdNode *
Cudd_ReadPlusInfinity(
  DdManager * dd)
{
    return(dd->plusinfinity);

} /* end of Cudd_ReadPlusInfinity */


/**
  @brief Reads the minus-infinity constant from the manager.

  @sideeffect None

*/
DdNode *
Cudd_ReadMinusInfinity(
  DdManager * dd)
{
    return(dd->minusinfinity);

} /* end of Cudd_ReadMinusInfinity */


/**
  @brief Reads the background constant of the manager.

  @sideeffect None

*/
DdNode *
Cudd_ReadBackground(
  DdManager * dd)
{
    return(dd->background);

} /* end of Cudd_ReadBackground */


/**
  @brief Sets the background constant of the manager.

  @details It assumes that the DdNode pointer bck is already
  referenced.

  @sideeffect None

*/
void
Cudd_SetBackground(
  DdManager * dd,
  DdNode * bck)
{
    dd->background = bck;

} /* end of Cudd_SetBackground */


/**
  @brief Reads the number of slots in the cache.

  @sideeffect None

  @see Cudd_ReadCacheUsedSlots

*/
unsigned int
Cudd_ReadCacheSlots(
  DdManager * dd)
{
    return(dd->cacheSlots);

} /* end of Cudd_ReadCacheSlots */


/**
  @brief Reads the fraction of used slots in the cache.

  @details The unused slots are those in which no valid data is
  stored. Garbage collection, variable reordering, and cache resizing
  may cause used slots to become unused.

  @sideeffect None

  @see Cudd_ReadCacheSlots

*/
double
Cudd_ReadCacheUsedSlots(
  DdManager * dd)
{
    size_t used = 0;
    int slots = dd->cacheSlots;
    DdCache *cache = dd->cache;
    int i;

    for (i = 0; i < slots; i++) {
	used += cache[i].h != 0;
    }

    return((double)used / (double) dd->cacheSlots);

} /* end of Cudd_ReadCacheUsedSlots */


/**
  @brief Returns the number of cache look-ups.

  @sideeffect None

  @see Cudd_ReadCacheHits

*/
double
Cudd_ReadCacheLookUps(
  DdManager * dd)
{
    return(dd->cacheHits + dd->cacheMisses +
	   dd->totCachehits + dd->totCacheMisses);

} /* end of Cudd_ReadCacheLookUps */


/**
  @brief Returns the number of cache hits.

  @sideeffect None

  @see Cudd_ReadCacheLookUps

*/
double
Cudd_ReadCacheHits(
  DdManager * dd)
{
    return(dd->cacheHits + dd->totCachehits);

} /* end of Cudd_ReadCacheHits */


/**
  @brief Returns the number of recursive calls.

  @details Returns the number of recursive calls if the package is
  compiled with DD_COUNT defined.

  @sideeffect None

*/
double
Cudd_ReadRecursiveCalls(
  DdManager * dd)
{
#ifdef DD_COUNT
    return(dd->recursiveCalls);
#else
    (void) dd; /* avoid warning */
    return(-1.0);
#endif

} /* end of Cudd_ReadRecursiveCalls */



/**
  @brief Reads the hit rate that causes resizinig of the computed
  table.

  @sideeffect None

  @see Cudd_SetMinHit

*/
unsigned int
Cudd_ReadMinHit(
  DdManager * dd)
{
    /* Internally, the package manipulates the ratio of hits to
    ** misses instead of the ratio of hits to accesses. */
    return((unsigned int) (0.5 + 100 * dd->minHit / (1 + dd->minHit)));

} /* end of Cudd_ReadMinHit */


/**
  @brief Sets the hit rate that causes resizinig of the computed
  table.

  @details Sets the minHit parameter of the manager. This
  parameter controls the resizing of the computed table. If the hit
  rate is larger than the specified value, and the cache is not
  already too large, then its size is doubled.

  @sideeffect None

  @see Cudd_ReadMinHit

*/
void
Cudd_SetMinHit(
  DdManager * dd,
  unsigned int hr)
{
    /* Internally, the package manipulates the ratio of hits to
    ** misses instead of the ratio of hits to accesses. */
    dd->minHit = (double) hr / (100.0 - (double) hr);

} /* end of Cudd_SetMinHit */


/**
  @brief Reads the looseUpTo parameter of the manager.

  @sideeffect None

  @see Cudd_SetLooseUpTo Cudd_ReadMinHit Cudd_ReadMinDead

*/
unsigned int
Cudd_ReadLooseUpTo(
  DdManager * dd)
{
    return(dd->looseUpTo);

} /* end of Cudd_ReadLooseUpTo */


/**
  @brief Sets the looseUpTo parameter of the manager.

  @details This parameter of the manager controls the threshold beyond
  which no fast growth of the unique table is allowed. The threshold
  is given as a number of slots. If the value passed to this function
  is 0, the function determines a suitable value based on the
  available memory.

  @sideeffect None

  @see Cudd_ReadLooseUpTo Cudd_SetMinHit

*/
void
Cudd_SetLooseUpTo(
  DdManager * dd,
  unsigned int lut)
{
    if (lut == 0) {
	unsigned long datalimit = getSoftDataLimit();
	lut = (unsigned int) (datalimit / (sizeof(DdNode) *
					   DD_MAX_LOOSE_FRACTION));
    }
    dd->looseUpTo = lut;

} /* end of Cudd_SetLooseUpTo */


/**
  @brief Returns the soft limit for the cache size.

  @sideeffect None

  @see Cudd_ReadMaxCacheHard

*/
unsigned int
Cudd_ReadMaxCache(
  DdManager * dd)
{
    return(2 * dd->cacheSlots + dd->cacheSlack);

} /* end of Cudd_ReadMaxCache */


/**
  @brief Reads the maxCacheHard parameter of the manager.

  @sideeffect None

  @see Cudd_SetMaxCacheHard Cudd_ReadMaxCache

*/
unsigned int
Cudd_ReadMaxCacheHard(
  DdManager * dd)
{
    return(dd->maxCacheHard);

} /* end of Cudd_ReadMaxCache */


/**
  @brief Sets the maxCacheHard parameter of the manager.

  @details The cache cannot grow larger than maxCacheHard
  entries. This parameter allows an application to control the
  trade-off of memory versus speed. If the value passed to this
  function is 0, the function determines a suitable maximum cache size
  based on the available memory.

  @sideeffect None

  @see Cudd_ReadMaxCacheHard Cudd_SetMaxCache

*/
void
Cudd_SetMaxCacheHard(
  DdManager * dd,
  unsigned int mc)
{
    if (mc == 0) {
	unsigned long datalimit = getSoftDataLimit();
	mc = (unsigned int) (datalimit / (sizeof(DdCache) *
					  DD_MAX_CACHE_FRACTION));
    }
    dd->maxCacheHard = mc;

} /* end of Cudd_SetMaxCacheHard */


/**
  @brief Returns the number of %BDD variables in existance.

  @sideeffect None

  @see Cudd_ReadZddSize

*/
int
Cudd_ReadSize(
  DdManager * dd)
{
    return(dd->size);

} /* end of Cudd_ReadSize */


/**
  @brief Returns the number of %ZDD variables in existance.

  @sideeffect None

  @see Cudd_ReadSize

*/
int
Cudd_ReadZddSize(
  DdManager * dd)
{
    return(dd->sizeZ);

} /* end of Cudd_ReadZddSize */


/**
  @brief Returns the total number of slots of the unique table.

  @details This number is mainly for diagnostic purposes.

  @sideeffect None

*/
unsigned int
Cudd_ReadSlots(
  DdManager * dd)
{
    return(dd->slots);

} /* end of Cudd_ReadSlots */


/**
  @brief Reads the fraction of used slots in the unique table.

  @details The unused slots are those in which no valid data is
  stored. Garbage collection, variable reordering, and subtable
  resizing may cause used slots to become unused.

  @sideeffect None

  @see Cudd_ReadSlots

*/
double
Cudd_ReadUsedSlots(
  DdManager * dd)
{
    size_t used = 0;
    int i, j;
    int size = dd->size;
    DdNodePtr *nodelist;
    DdSubtable *subtable;
    DdNode *node;
    DdNode *sentinel = &(dd->sentinel);

    /* Scan each BDD/ADD subtable. */
    for (i = 0; i < size; i++) {
	subtable = &(dd->subtables[i]);
	nodelist = subtable->nodelist;
	for (j = 0; (unsigned) j < subtable->slots; j++) {
	    node = nodelist[j];
	    if (node != sentinel) {
		used++;
	    }
	}
    }

    /* Scan the ZDD subtables. */
    size = dd->sizeZ;

    for (i = 0; i < size; i++) {
	subtable = &(dd->subtableZ[i]);
	nodelist = subtable->nodelist;
	for (j = 0; (unsigned) j < subtable->slots; j++) {
	    node = nodelist[j];
	    if (node != NULL) {
		used++;
	    }
	}
    }

    /* Constant table. */
    subtable = &(dd->constants);
    nodelist = subtable->nodelist;
    for (j = 0; (unsigned) j < subtable->slots; j++) {
	node = nodelist[j];
	if (node != NULL) {
	    used++;
	}
    }

    return((double)used / (double) dd->slots);

} /* end of Cudd_ReadUsedSlots */


/**
  @brief Computes the expected fraction of used slots in the unique
  table.

  @details This expected value is based on the assumption that the
  hash function distributes the keys randomly; it can be compared with
  the result of Cudd_ReadUsedSlots to monitor the performance of the
  unique table hash function.

  @sideeffect None

  @see Cudd_ReadSlots Cudd_ReadUsedSlots

*/
double
Cudd_ExpectedUsedSlots(
  DdManager * dd)
{
    int i;
    int size = dd->size;
    DdSubtable *subtable;
    double empty = 0.0;

    /* To each subtable we apply the corollary to Theorem 8.5 (occupancy
    ** distribution) from Sedgewick and Flajolet's Analysis of Algorithms.
    ** The corollary says that for a table with M buckets and a load ratio
    ** of r, the expected number of empty buckets is asymptotically given
    ** by M * exp(-r).
    */

    /* Scan each BDD/ADD subtable. */
    for (i = 0; i < size; i++) {
	subtable = &(dd->subtables[i]);
	empty += (double) subtable->slots *
	    exp(-(double) subtable->keys / (double) subtable->slots);
    }

    /* Scan the ZDD subtables. */
    size = dd->sizeZ;

    for (i = 0; i < size; i++) {
	subtable = &(dd->subtableZ[i]);
	empty += (double) subtable->slots *
	    exp(-(double) subtable->keys / (double) subtable->slots);
    }

    /* Constant table. */
    subtable = &(dd->constants);
    empty += (double) subtable->slots *
	exp(-(double) subtable->keys / (double) subtable->slots);

    return(1.0 - empty / (double) dd->slots);

} /* end of Cudd_ExpectedUsedSlots */


/**
  @brief Returns the number of nodes in the unique table.

  @details Returns the total number of nodes currently in the unique
  table, including the dead nodes.

  @sideeffect None

  @see Cudd_ReadDead

*/
unsigned int
Cudd_ReadKeys(
  DdManager * dd)
{
    return(dd->keys);

} /* end of Cudd_ReadKeys */


/**
  @brief Returns the number of dead nodes in the unique table.

  @sideeffect None

  @see Cudd_ReadKeys

*/
unsigned int
Cudd_ReadDead(
  DdManager * dd)
{
    return(dd->dead);

} /* end of Cudd_ReadDead */


/**
  @brief Reads the minDead parameter of the manager.

  @details The minDead parameter is used by the package to decide
  whether to collect garbage or resize a subtable of the unique table
  when the subtable becomes too full. The application can indirectly
  control the value of minDead by setting the looseUpTo parameter.

  @sideeffect None

  @see Cudd_ReadDead Cudd_ReadLooseUpTo Cudd_SetLooseUpTo

*/
unsigned int
Cudd_ReadMinDead(
  DdManager * dd)
{
    return(dd->minDead);

} /* end of Cudd_ReadMinDead */


/**
  @brief Returns the number of times reordering has occurred.

  @details The number includes both the calls to Cudd_ReduceHeap from
  the application program and those automatically performed by the
  package. However, calls that do not even initiate reordering are not
  counted. A call may not initiate reordering if there are fewer than
  minsize live nodes in the manager, or if CUDD_REORDER_NONE is specified
  as reordering method. The calls to Cudd_ShuffleHeap are not counted.

  @sideeffect None

  @see Cudd_ReduceHeap Cudd_ReadReorderingTime

*/
unsigned int
Cudd_ReadReorderings(
  DdManager * dd)
{
    return(dd->reorderings);

} /* end of Cudd_ReadReorderings */


/**
  @brief Returns the maximum number of times reordering may be invoked.

  @sideeffect None

  @see Cudd_ReadReorderings Cudd_SetMaxReorderings Cudd_ReduceHeap

*/
unsigned int
Cudd_ReadMaxReorderings(
  DdManager * dd)
{
    return(dd->maxReorderings);

} /* end of Cudd_ReadMaxReorderings */


/**
  @brief Sets the maximum number of times reordering may be invoked.

  @details The default value is (practically) infinite.

  @sideeffect None

  @see Cudd_ReadReorderings Cudd_ReadMaxReorderings Cudd_ReduceHeap

*/
void
Cudd_SetMaxReorderings(
  DdManager * dd, unsigned int mr)
{
    dd->maxReorderings = mr;

} /* end of Cudd_SetMaxReorderings */


/**
  @brief Returns the time spent in reordering.

  @details Returns the number of milliseconds spent reordering
  variables since the manager was initialized. The time spent in collecting
  garbage before reordering is included.

  @sideeffect None

  @see Cudd_ReadReorderings

*/
long
Cudd_ReadReorderingTime(
  DdManager * dd)
{
    return(dd->reordTime);

} /* end of Cudd_ReadReorderingTime */


/**
  @brief Returns the number of times garbage collection has occurred.

  @details The number includes both the calls from reordering
  procedures and those caused by requests to create new nodes.

  @sideeffect None

  @see Cudd_ReadGarbageCollectionTime

*/
int
Cudd_ReadGarbageCollections(
  DdManager * dd)
{
    return(dd->garbageCollections);

} /* end of Cudd_ReadGarbageCollections */


/**
  @brief Returns the time spent in garbage collection.

  @details Returns the number of milliseconds spent doing garbage
  collection since the manager was initialized.

  @sideeffect None

  @see Cudd_ReadGarbageCollections

*/
long
Cudd_ReadGarbageCollectionTime(
  DdManager * dd)
{
    return(dd->GCTime);

} /* end of Cudd_ReadGarbageCollectionTime */


/**
  @brief Returns the number of nodes freed.

  @details Returns the number of nodes returned to the free list if the
  keeping of this statistic is enabled; -1 otherwise. This statistic is
  enabled only if the package is compiled with DD_STATS defined.

  @sideeffect None

  @see Cudd_ReadNodesDropped

*/
double
Cudd_ReadNodesFreed(
  DdManager * dd)
{
#ifdef DD_STATS
    return(dd->nodesFreed);
#else
    (void) dd; /* avoid warning */
    return(-1.0);
#endif

} /* end of Cudd_ReadNodesFreed */


/**
  @brief Returns the number of nodes dropped.

  @details Returns the number of nodes killed by dereferencing if the
  keeping of this statistic is enabled; -1 otherwise. This statistic is
  enabled only if the package is compiled with DD_STATS defined.

  @sideeffect None

  @see Cudd_ReadNodesFreed

*/
double
Cudd_ReadNodesDropped(
  DdManager * dd)
{
#ifdef DD_STATS
    return(dd->nodesDropped);
#else
    (void) dd; /* avoid warning */
    return(-1.0);
#endif

} /* end of Cudd_ReadNodesDropped */


/**
  @brief Returns the number of look-ups in the unique table.

  @details Returns the number of look-ups in the unique table if the
  keeping of this statistic is enabled; -1 otherwise. This statistic is
  enabled only if the package is compiled with DD_UNIQUE_PROFILE defined.

  @sideeffect None

  @see Cudd_ReadUniqueLinks

*/
double
Cudd_ReadUniqueLookUps(
  DdManager * dd)
{
#ifdef DD_UNIQUE_PROFILE
    return(dd->uniqueLookUps);
#else
    (void) dd; /* avoid warning */
    return(-1.0);
#endif

} /* end of Cudd_ReadUniqueLookUps */


/**
  @brief Returns the number of links followed in the unique table.

  @details Returns the number of links followed during look-ups in the
  unique table if the keeping of this statistic is enabled; -1 otherwise.
  If an item is found in the first position of its collision list, the
  number of links followed is taken to be 0. If it is in second position,
  the number of links is 1, and so on. This statistic is enabled only if
  the package is compiled with DD_UNIQUE_PROFILE defined.

  @sideeffect None

  @see Cudd_ReadUniqueLookUps

*/
double
Cudd_ReadUniqueLinks(
  DdManager * dd)
{
#ifdef DD_UNIQUE_PROFILE
    return(dd->uniqueLinks);
#else
    (void) dd; /* avoid warning */
    return(-1.0);
#endif

} /* end of Cudd_ReadUniqueLinks */


/**
  @brief Reads the siftMaxVar parameter of the manager.

  @details This parameter gives the maximum number of variables that
  will be sifted for each invocation of sifting.

  @sideeffect None

  @see Cudd_ReadSiftMaxSwap Cudd_SetSiftMaxVar

*/
int
Cudd_ReadSiftMaxVar(
  DdManager * dd)
{
    return(dd->siftMaxVar);

} /* end of Cudd_ReadSiftMaxVar */


/**
  @brief Sets the siftMaxVar parameter of the manager.

  @details This parameter gives the maximum number of variables that
  will be sifted for each invocation of sifting.

  @sideeffect None

  @see Cudd_SetSiftMaxSwap Cudd_ReadSiftMaxVar

*/
void
Cudd_SetSiftMaxVar(
  DdManager * dd,
  int  smv)
{
    dd->siftMaxVar = smv;

} /* end of Cudd_SetSiftMaxVar */


/**
  @brief Reads the siftMaxSwap parameter of the manager.

  @details This parameter gives the maximum number of swaps that will
  be attempted for each invocation of sifting. The real number of
  swaps may exceed the set limit because the package will always
  complete the sifting of the variable that causes the limit to be
  reached.

  @sideeffect None

  @see Cudd_ReadSiftMaxVar Cudd_SetSiftMaxSwap

*/
int
Cudd_ReadSiftMaxSwap(
  DdManager * dd)
{
    return(dd->siftMaxSwap);

} /* end of Cudd_ReadSiftMaxSwap */


/**
  @brief Sets the siftMaxSwap parameter of the manager.

  @details This parameter gives the maximum number of swaps that will
  be attempted for each invocation of sifting. The real number of
  swaps may exceed the set limit because the package will always
  complete the sifting of the variable that causes the limit to be
  reached.

  @sideeffect None

  @see Cudd_SetSiftMaxVar Cudd_ReadSiftMaxSwap

*/
void
Cudd_SetSiftMaxSwap(
  DdManager * dd,
  int  sms)
{
    dd->siftMaxSwap = sms;

} /* end of Cudd_SetSiftMaxSwap */


/**
  @brief Reads the maxGrowth parameter of the manager.

  @details This parameter determines how much the number of nodes can
  grow during sifting of a variable.  Overall, sifting never increases
  the size of the decision diagrams.  This parameter only refers to
  intermediate results.  A lower value will speed up sifting, possibly
  at the expense of quality.

  @sideeffect None

  @see Cudd_SetMaxGrowth Cudd_ReadMaxGrowthAlternate

*/
double
Cudd_ReadMaxGrowth(
  DdManager * dd)
{
    return(dd->maxGrowth);

} /* end of Cudd_ReadMaxGrowth */


/**
  @brief Sets the maxGrowth parameter of the manager.

  @details This parameter determines how much the number of nodes can
  grow during sifting of a variable.  Overall, sifting never increases
  the size of the decision diagrams.  This parameter only refers to
  intermediate results.  A lower value will speed up sifting, possibly
  at the expense of quality.

  @sideeffect None

  @see Cudd_ReadMaxGrowth Cudd_SetMaxGrowthAlternate

*/
void
Cudd_SetMaxGrowth(
  DdManager * dd,
  double mg)
{
    dd->maxGrowth = mg;

} /* end of Cudd_SetMaxGrowth */


/**
  @brief Reads the maxGrowthAlt parameter of the manager.

  @details This parameter is analogous to the maxGrowth paramter, and
  is used every given number of reorderings instead of maxGrowth.  The
  number of reorderings is set with Cudd_SetReorderingCycle.  If the
  number of reorderings is 0 (default) maxGrowthAlt is never used.

  @sideeffect None

  @see Cudd_ReadMaxGrowth Cudd_SetMaxGrowthAlternate
  Cudd_SetReorderingCycle Cudd_ReadReorderingCycle

*/
double
Cudd_ReadMaxGrowthAlternate(
  DdManager * dd)
{
    return(dd->maxGrowthAlt);

} /* end of Cudd_ReadMaxGrowthAlternate */


/**
  @brief Sets the maxGrowthAlt parameter of the manager.

  @details This parameter is analogous to the maxGrowth paramter, and
  is used every given number of reorderings instead of maxGrowth.  The
  number of reorderings is set with Cudd_SetReorderingCycle.  If the
  number of reorderings is 0 (default) maxGrowthAlt is never used.

  @sideeffect None

  @see Cudd_ReadMaxGrowthAlternate Cudd_SetMaxGrowth
  Cudd_SetReorderingCycle Cudd_ReadReorderingCycle

*/
void
Cudd_SetMaxGrowthAlternate(
  DdManager * dd,
  double mg)
{
    dd->maxGrowthAlt = mg;

} /* end of Cudd_SetMaxGrowthAlternate */


/**
  @brief Reads the reordCycle parameter of the manager.

  @details This parameter determines how often the alternate threshold
  on maximum growth is used in reordering.

  @sideeffect None

  @see Cudd_ReadMaxGrowthAlternate Cudd_SetMaxGrowthAlternate
  Cudd_SetReorderingCycle

*/
int
Cudd_ReadReorderingCycle(
  DdManager * dd)
{
    return(dd->reordCycle);

} /* end of Cudd_ReadReorderingCycle */


/**
  @brief Sets the reordCycle parameter of the manager.

  @details This parameter determines how often the alternate threshold
  on maximum growth is used in reordering.

  @sideeffect None

  @see Cudd_ReadMaxGrowthAlternate Cudd_SetMaxGrowthAlternate
  Cudd_ReadReorderingCycle

*/
void
Cudd_SetReorderingCycle(
  DdManager * dd,
  int cycle)
{
    dd->reordCycle = cycle;

} /* end of Cudd_SetReorderingCycle */


/**
  @brief Returns the variable group tree of the manager.

  @sideeffect None

  @see Cudd_SetTree Cudd_FreeTree Cudd_ReadZddTree

*/
MtrNode *
Cudd_ReadTree(
  DdManager * dd)
{
    return(dd->tree);

} /* end of Cudd_ReadTree */


/**
  @brief Sets the variable group tree of the manager.

  @sideeffect None

  @see Cudd_FreeTree Cudd_ReadTree Cudd_SetZddTree

*/
void
Cudd_SetTree(
  DdManager * dd,
  MtrNode * tree)
{
    if (dd->tree != NULL) {
	Mtr_FreeTree(dd->tree);
    }
    dd->tree = tree;
    if (tree == NULL) return;

    fixVarTree(tree, dd->perm, dd->size);
    return;

} /* end of Cudd_SetTree */


/**
  @brief Frees the variable group tree of the manager.

  @sideeffect None

  @see Cudd_SetTree Cudd_ReadTree Cudd_FreeZddTree

*/
void
Cudd_FreeTree(
  DdManager * dd)
{
    if (dd->tree != NULL) {
	Mtr_FreeTree(dd->tree);
	dd->tree = NULL;
    }
    return;

} /* end of Cudd_FreeTree */


/**
  @brief Returns the variable group tree of the manager.

  @sideeffect None

  @see Cudd_SetZddTree Cudd_FreeZddTree Cudd_ReadTree

*/
MtrNode *
Cudd_ReadZddTree(
  DdManager * dd)
{
    return(dd->treeZ);

} /* end of Cudd_ReadZddTree */


/**
  @brief Sets the %ZDD variable group tree of the manager.

  @sideeffect None

  @see Cudd_FreeZddTree Cudd_ReadZddTree Cudd_SetTree

*/
void
Cudd_SetZddTree(
  DdManager * dd,
  MtrNode * tree)
{
    if (dd->treeZ != NULL) {
	Mtr_FreeTree(dd->treeZ);
    }
    dd->treeZ = tree;
    if (tree == NULL) return;

    fixVarTree(tree, dd->permZ, dd->sizeZ);
    return;

} /* end of Cudd_SetZddTree */


/**
  @brief Frees the variable group tree of the manager.

  @sideeffect None

  @see Cudd_SetZddTree Cudd_ReadZddTree Cudd_FreeTree

*/
void
Cudd_FreeZddTree(
  DdManager * dd)
{
    if (dd->treeZ != NULL) {
	Mtr_FreeTree(dd->treeZ);
	dd->treeZ = NULL;
    }
    return;

} /* end of Cudd_FreeZddTree */


/**
  @brief Returns the index of the node.

  @details The node pointer can be either regular or complemented.

  @sideeffect None

  @see Cudd_ReadIndex

*/
unsigned int
Cudd_NodeReadIndex(
  DdNode * node)
{
    return((unsigned int) Cudd_Regular(node)->index);

} /* end of Cudd_NodeReadIndex */


/**
  @brief Returns the current position of the i-th variable in the
  order.

  @details If the index is CUDD_CONST_INDEX, returns CUDD_CONST_INDEX;
  otherwise, if the index is out of bounds returns -1.

  @sideeffect None

  @see Cudd_ReadInvPerm Cudd_ReadPermZdd

*/
int
Cudd_ReadPerm(
  DdManager * dd,
  int  i)
{
    if (i == CUDD_CONST_INDEX) return(CUDD_CONST_INDEX);
    if (i < 0 || i >= dd->size) return(-1);
    return(dd->perm[i]);

} /* end of Cudd_ReadPerm */


/**
  @brief Returns the current position of the i-th %ZDD variable in the
  order.

  @details If the index is CUDD_CONST_INDEX, returns CUDD_CONST_INDEX;
  otherwise, if the index is out of bounds returns -1.

  @sideeffect None

  @see Cudd_ReadInvPermZdd Cudd_ReadPerm

*/
int
Cudd_ReadPermZdd(
  DdManager * dd,
  int  i)
{
    if (i == CUDD_CONST_INDEX) return(CUDD_CONST_INDEX);
    if (i < 0 || i >= dd->sizeZ) return(-1);
    return(dd->permZ[i]);

} /* end of Cudd_ReadPermZdd */


/**
  @brief Returns the index of the variable currently in the i-th
  position of the order.

  @details If the index is CUDD_CONST_INDEX, returns CUDD_CONST_INDEX;
  otherwise, if the index is out of bounds returns -1.

  @sideeffect None

  @see Cudd_ReadPerm Cudd_ReadInvPermZdd

*/
int
Cudd_ReadInvPerm(
  DdManager * dd,
  int  i)
{
    if (i == CUDD_CONST_INDEX) return(CUDD_CONST_INDEX);
    if (i < 0 || i >= dd->size) return(-1);
    return(dd->invperm[i]);

} /* end of Cudd_ReadInvPerm */


/**
  @brief Returns the index of the %ZDD variable currently in the i-th
  position of the order.

  @details If the index is CUDD_CONST_INDEX, returns CUDD_CONST_INDEX;
  otherwise, if the index is out of bounds returns -1.

  @sideeffect None

  @see Cudd_ReadPerm Cudd_ReadInvPermZdd

*/
int
Cudd_ReadInvPermZdd(
  DdManager * dd,
  int  i)
{
    if (i == CUDD_CONST_INDEX) return(CUDD_CONST_INDEX);
    if (i < 0 || i >= dd->sizeZ) return(-1);
    return(dd->invpermZ[i]);

} /* end of Cudd_ReadInvPermZdd */


/**
  @brief Returns the i-th element of the vars array.

  @details Returns the i-th element of the vars array if it falls
  within the array bounds; NULL otherwise. If i is the index of an
  existing variable, this function produces the same result as
  Cudd_bddIthVar. However, if the i-th var does not exist yet,
  Cudd_bddIthVar will create it, whereas Cudd_ReadVars will not.

  @sideeffect None

  @see Cudd_bddIthVar

*/
DdNode *
Cudd_ReadVars(
  DdManager * dd,
  int  i)
{
    if (i < 0 || i > dd->size) return(NULL);
    return(dd->vars[i]);

} /* end of Cudd_ReadVars */


/**
  @brief Reads the epsilon parameter of the manager.

  @details The epsilon parameter control the comparison between
  floating point numbers.

  @sideeffect None

  @see Cudd_SetEpsilon

*/
CUDD_VALUE_TYPE
Cudd_ReadEpsilon(
  DdManager * dd)
{
    return(dd->epsilon);

} /* end of Cudd_ReadEpsilon */


/**
  @brief Sets the epsilon parameter of the manager to ep.

  @details The epsilon parameter control the comparison between
  floating point numbers.

  @sideeffect None

  @see Cudd_ReadEpsilon

*/
void
Cudd_SetEpsilon(
  DdManager * dd,
  CUDD_VALUE_TYPE  ep)
{
    dd->epsilon = ep;

} /* end of Cudd_SetEpsilon */


/**
  @brief Reads the groupcheck parameter of the manager.

  @details The groupcheck parameter determines the aggregation
  criterion in group sifting.

  @sideeffect None

  @see Cudd_SetGroupcheck

*/
Cudd_AggregationType
Cudd_ReadGroupcheck(
  DdManager * dd)
{
    return(dd->groupcheck);

} /* end of Cudd_ReadGroupCheck */


/**
  @brief Sets the parameter groupcheck of the manager to gc.

  @details The groupcheck parameter determines the aggregation
  criterion in group sifting.

  @sideeffect None

  @see Cudd_ReadGroupCheck

*/
void
Cudd_SetGroupcheck(
  DdManager * dd,
  Cudd_AggregationType gc)
{
    dd->groupcheck = gc;

} /* end of Cudd_SetGroupcheck */


/**
  @brief Tells whether garbage collection is enabled.

  @return 1 if garbage collection is enabled; 0 otherwise.

  @sideeffect None

  @see Cudd_EnableGarbageCollection Cudd_DisableGarbageCollection

*/
int
Cudd_GarbageCollectionEnabled(
  DdManager * dd)
{
    return(dd->gcEnabled);

} /* end of Cudd_GarbageCollectionEnabled */


/**
  @brief Enables garbage collection.

  @details Garbage collection is initially enabled. Therefore it is
  necessary to call this function only if garbage collection has been
  explicitly disabled.

  @sideeffect None

  @see Cudd_DisableGarbageCollection Cudd_GarbageCollectionEnabled

*/
void
Cudd_EnableGarbageCollection(
  DdManager * dd)
{
    dd->gcEnabled = 1;

} /* end of Cudd_EnableGarbageCollection */


/**
  @brief Disables garbage collection.

  @details Garbage collection is initially enabled. This function may
  be called to disable it.  However, garbage collection will still
  occur when a new node must be created and no memory is left, or when
  garbage collection is required for correctness. (E.g., before
  reordering.)

  @sideeffect None

  @see Cudd_EnableGarbageCollection Cudd_GarbageCollectionEnabled

*/
void
Cudd_DisableGarbageCollection(
  DdManager * dd)
{
    dd->gcEnabled = 0;

} /* end of Cudd_DisableGarbageCollection */


/**
  @brief Tells whether dead nodes are counted towards triggering
  reordering.

  @return 1 if dead nodes are counted; 0 otherwise.

  @sideeffect None

  @see Cudd_TurnOnCountDead Cudd_TurnOffCountDead

*/
int
Cudd_DeadAreCounted(
  DdManager * dd)
{
    return(dd->countDead == 0 ? 1 : 0);

} /* end of Cudd_DeadAreCounted */


/**
  @brief Causes the dead nodes to be counted towards triggering
  reordering.

  @details This causes more frequent reorderings. By default dead
  nodes are not counted.

  @sideeffect Changes the manager.

  @see Cudd_TurnOffCountDead Cudd_DeadAreCounted

*/
void
Cudd_TurnOnCountDead(
  DdManager * dd)
{
    dd->countDead = 0;

} /* end of Cudd_TurnOnCountDead */


/**
  @brief Causes the dead nodes not to be counted towards triggering
  reordering.

  @details This causes less frequent reorderings. By default dead
  nodes are not counted. Therefore there is no need to call this
  function unless Cudd_TurnOnCountDead has been previously called.

  @sideeffect Changes the manager.

  @see Cudd_TurnOnCountDead Cudd_DeadAreCounted

*/
void
Cudd_TurnOffCountDead(
  DdManager * dd)
{
    dd->countDead = ~0U;

} /* end of Cudd_TurnOffCountDead */


/**
  @brief Returns the current value of the recombination parameter used
  in group sifting.

  @details A larger (positive) value makes the aggregation of
  variables due to the second difference criterion more likely. A
  smaller (negative) value makes aggregation less likely.

  @sideeffect None

  @see Cudd_SetRecomb

*/
int
Cudd_ReadRecomb(
  DdManager * dd)
{
    return(dd->recomb);

} /* end of Cudd_ReadRecomb */


/**
  @brief Sets the value of the recombination parameter used in group
  sifting.

  @details A larger (positive) value makes the aggregation of
  variables due to the second difference criterion more likely. A
  smaller (negative) value makes aggregation less likely. The default
  value is 0.

  @sideeffect Changes the manager.

  @see Cudd_ReadRecomb

*/
void
Cudd_SetRecomb(
  DdManager * dd,
  int  recomb)
{
    dd->recomb = recomb;

} /* end of Cudd_SetRecomb */


/**
  @brief Returns the current value of the symmviolation parameter used
  in group sifting.

  @details This parameter is used in group sifting to decide how many
  violations to the symmetry conditions <code>f10 = f01</code> or
  <code>f11 = f00</code> are tolerable when checking for aggregation
  due to extended symmetry. The value should be between 0 and 100. A
  small value causes fewer variables to be aggregated. The default
  value is 0.

  @sideeffect None

  @see Cudd_SetSymmviolation

*/
int
Cudd_ReadSymmviolation(
  DdManager * dd)
{
    return(dd->symmviolation);

} /* end of Cudd_ReadSymmviolation */


/**
  @brief Sets the value of the symmviolation parameter used
  in group sifting.

  @details This parameter is used in group sifting to decide how many
  violations to the symmetry conditions <code>f10 = f01</code> or
  <code>f11 = f00</code> are tolerable when checking for aggregation
  due to extended symmetry. The value should be between 0 and 100. A
  small value causes fewer variables to be aggregated. The default
  value is 0.

  @sideeffect Changes the manager.

  @see Cudd_ReadSymmviolation

*/
void
Cudd_SetSymmviolation(
  DdManager * dd,
  int  symmviolation)
{
    dd->symmviolation = symmviolation;

} /* end of Cudd_SetSymmviolation */


/**
  @brief Returns the current value of the arcviolation parameter used
  in group sifting.

  @details This parameter is used to decide how many arcs into
  <code>y</code> not coming from <code>x</code> are tolerable when
  checking for aggregation due to extended symmetry. The value should
  be between 0 and 100. A small value causes fewer variables to be
  aggregated. The default value is 0.

  @sideeffect None

  @see Cudd_SetArcviolation

*/
int
Cudd_ReadArcviolation(
  DdManager * dd)
{
    return(dd->arcviolation);

} /* end of Cudd_ReadArcviolation */


/**
  @brief Sets the value of the arcviolation parameter used
  in group sifting.

  @details This parameter is used to decide how many arcs into
  <code>y</code> not coming from <code>x</code> are tolerable when
  checking for aggregation due to extended symmetry. The value should
  be between 0 and 100. A small value causes fewer variables to be
  aggregated. The default value is 0.

  @sideeffect None

  @see Cudd_ReadArcviolation

*/
void
Cudd_SetArcviolation(
  DdManager * dd,
  int  arcviolation)
{
    dd->arcviolation = arcviolation;

} /* end of Cudd_SetArcviolation */


/**
  @brief Reads the current size of the population used by the
  genetic algorithm for variable reordering.

  @details A larger population size will cause the genetic algorithm
  to take more time, but will generally produce better results. The
  default value is 0, in which case the package uses three times the
  number of variables as population size, with a maximum of 120.

  @sideeffect None

  @see Cudd_SetPopulationSize

*/
int
Cudd_ReadPopulationSize(
  DdManager * dd)
{
    return(dd->populationSize);

} /* end of Cudd_ReadPopulationSize */


/**
  @brief Sets the size of the population used by the
  genetic algorithm for variable reordering.

  @details A larger population size will cause the genetic algorithm
  to take more time, but will generally produce better results. The
  default value is 0, in which case the package uses three times the
  number of variables as population size, with a maximum of 120.

  @sideeffect Changes the manager.

  @see Cudd_ReadPopulationSize

*/
void
Cudd_SetPopulationSize(
  DdManager * dd,
  int  populationSize)
{
    dd->populationSize = populationSize;

} /* end of Cudd_SetPopulationSize */


/**
  @brief Reads the current number of crossovers used by the
  genetic algorithm for variable reordering.

  @details A larger number of crossovers will cause the genetic
  algorithm to take more time, but will generally produce better
  results. The default value is 0, in which case the package uses
  three times the number of variables as number of crossovers, with a
  maximum of 60.

  @sideeffect None

  @see Cudd_SetNumberXovers

*/
int
Cudd_ReadNumberXovers(
  DdManager * dd)
{
    return(dd->numberXovers);

} /* end of Cudd_ReadNumberXovers */


/**
  @brief Sets the number of crossovers used by the
  genetic algorithm for variable reordering.

  @details A larger number of crossovers will cause the genetic
  algorithm to take more time, but will generally produce better
  results. The default value is 0, in which case the package uses
  three times the number of variables as number of crossovers, with a
  maximum of 60.

  @sideeffect None

  @see Cudd_ReadNumberXovers

*/
void
Cudd_SetNumberXovers(
  DdManager * dd,
  int  numberXovers)
{
    dd->numberXovers = numberXovers;

} /* end of Cudd_SetNumberXovers */


/**
  @brief Returns the order randomization factor.

  @details If non-zero this factor is used to determine a perturbation
  of the next reordering threshold.  Larger factors cause larger
  perturbations.

  @sideeffect None

  @see Cudd_SetOrderRandomization

*/
unsigned int
Cudd_ReadOrderRandomization(
  DdManager * dd)
{
    return(dd->randomizeOrder);

} /* end of Cudd_ReadOrderRandomization */


/**
  @brief Sets the order randomization factor.

  @sideeffect None

  @see Cudd_ReadOrderRandomization

*/
void
Cudd_SetOrderRandomization(
  DdManager * dd,
  unsigned int factor)
{
    dd->randomizeOrder = factor;

} /* end of Cudd_SetOrderRandomization */


/**
  @brief Returns the memory in use by the manager measured in bytes.

  @sideeffect None

*/
size_t
Cudd_ReadMemoryInUse(
  DdManager * dd)
{
    return(dd->memused);

} /* end of Cudd_ReadMemoryInUse */


/**
  @brief Prints out statistics and settings for a CUDD manager.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

*/
int
Cudd_PrintInfo(
  DdManager * dd,
  FILE * fp)
{
    int retval;
    Cudd_ReorderingType autoMethod, autoMethodZ;

    /* Modifiable parameters. */
    retval = fprintf(fp,"**** CUDD modifiable parameters ****\n");
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Hard limit for cache size: %u\n",
		     Cudd_ReadMaxCacheHard(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Cache hit threshold for resizing: %u%%\n",
		     Cudd_ReadMinHit(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Garbage collection enabled: %s\n",
		     Cudd_GarbageCollectionEnabled(dd) ? "yes" : "no");
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Limit for fast unique table growth: %u\n",
		     Cudd_ReadLooseUpTo(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,
		     "Maximum number of variables sifted per reordering: %d\n",
		     Cudd_ReadSiftMaxVar(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,
		     "Maximum number of variable swaps per reordering: %d\n",
		     Cudd_ReadSiftMaxSwap(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Maximum growth while sifting a variable: %g\n",
		     Cudd_ReadMaxGrowth(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Dynamic reordering of BDDs enabled: %s\n",
		     Cudd_ReorderingStatus(dd,&autoMethod) ? "yes" : "no");
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Default BDD reordering method: %d\n",
		     (int) autoMethod);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Dynamic reordering of ZDDs enabled: %s\n",
		     Cudd_ReorderingStatusZdd(dd,&autoMethodZ) ? "yes" : "no");
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Default ZDD reordering method: %d\n",
		     (int) autoMethodZ);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Realignment of ZDDs to BDDs enabled: %s\n",
		     Cudd_zddRealignmentEnabled(dd) ? "yes" : "no");
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Realignment of BDDs to ZDDs enabled: %s\n",
		     Cudd_bddRealignmentEnabled(dd) ? "yes" : "no");
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Dead nodes counted in triggering reordering: %s\n",
		     Cudd_DeadAreCounted(dd) ? "yes" : "no");
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Group checking criterion: %u\n",
		     (unsigned int) Cudd_ReadGroupcheck(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Recombination threshold: %d\n", Cudd_ReadRecomb(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Symmetry violation threshold: %d\n",
		     Cudd_ReadSymmviolation(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Arc violation threshold: %d\n",
		     Cudd_ReadArcviolation(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"GA population size: %d\n",
		     Cudd_ReadPopulationSize(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Number of crossovers for GA: %d\n",
		     Cudd_ReadNumberXovers(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Next reordering threshold: %u\n",
		     Cudd_ReadNextReordering(dd));
    if (retval == EOF) return(0);

    /* Non-modifiable parameters. */
    retval = fprintf(fp,"**** CUDD non-modifiable parameters ****\n");
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Memory in use: %" PRIszt "\n",
                     Cudd_ReadMemoryInUse(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Peak number of nodes: %ld\n",
		     Cudd_ReadPeakNodeCount(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Peak number of live nodes: %d\n",
		     Cudd_ReadPeakLiveNodeCount(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Number of BDD variables: %d\n", dd->size);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Number of ZDD variables: %d\n", dd->sizeZ);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Number of cache entries: %u\n", dd->cacheSlots);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Number of cache look-ups: %.0f\n",
		     Cudd_ReadCacheLookUps(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Number of cache hits: %.0f\n",
		     Cudd_ReadCacheHits(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Number of cache insertions: %.0f\n",
		     dd->cacheinserts);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Number of cache collisions: %.0f\n",
		     dd->cachecollisions);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Number of cache deletions: %.0f\n",
		     dd->cachedeletions);
    if (retval == EOF) return(0);
    retval = cuddCacheProfile(dd,fp);
    if (retval == 0) return(0);
    retval = fprintf(fp,"Soft limit for cache size: %u\n",
		     Cudd_ReadMaxCache(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Number of buckets in unique table: %u\n", dd->slots);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Used buckets in unique table: %.2f%% (expected %.2f%%)\n",
		     100.0 * Cudd_ReadUsedSlots(dd),
		     100.0 * Cudd_ExpectedUsedSlots(dd));
    if (retval == EOF) return(0);
#ifdef DD_UNIQUE_PROFILE
    retval = fprintf(fp,"Unique lookups: %.0f\n", dd->uniqueLookUps);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Unique links: %.0f (%g per lookup)\n",
	    dd->uniqueLinks, dd->uniqueLinks / dd->uniqueLookUps);
    if (retval == EOF) return(0);
#endif
    retval = fprintf(fp,"Number of BDD and ADD nodes: %u\n", dd->keys);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Number of ZDD nodes: %u\n", dd->keysZ);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Number of dead BDD and ADD nodes: %u\n", dd->dead);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Number of dead ZDD nodes: %u\n", dd->deadZ);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Total number of nodes allocated: %.0f\n",
		     dd->allocated);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Total number of nodes reclaimed: %.0f\n",
		     dd->reclaimed);
    if (retval == EOF) return(0);
#ifdef DD_STATS
    retval = fprintf(fp,"Nodes freed: %.0f\n", dd->nodesFreed);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Nodes dropped: %.0f\n", dd->nodesDropped);
    if (retval == EOF) return(0);
#endif
#ifdef DD_COUNT
    retval = fprintf(fp,"Number of recursive calls: %.0f\n",
		     Cudd_ReadRecursiveCalls(dd));
    if (retval == EOF) return(0);
#endif
    retval = fprintf(fp,"Garbage collections so far: %d\n",
		     Cudd_ReadGarbageCollections(dd));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Time for garbage collection: %.2f sec\n",
		     ((double)Cudd_ReadGarbageCollectionTime(dd)/1000.0));
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Reorderings so far: %d\n", dd->reorderings);
    if (retval == EOF) return(0);
    retval = fprintf(fp,"Time for reordering: %.2f sec\n",
		     ((double)Cudd_ReadReorderingTime(dd)/1000.0));
    if (retval == EOF) return(0);
#ifdef DD_COUNT
    retval = fprintf(fp,"Node swaps in reordering: %.0f\n",
	Cudd_ReadSwapSteps(dd));
    if (retval == EOF) return(0);
#endif

    return(1);

} /* end of Cudd_PrintInfo */


/**
  @brief Reports the peak number of nodes.

  @details This number includes node on the free list. At the peak,
  the number of nodes on the free list is guaranteed to be less than
  DD_MEM_CHUNK.

  @sideeffect None

  @see Cudd_ReadNodeCount Cudd_PrintInfo

*/
long
Cudd_ReadPeakNodeCount(
  DdManager * dd)
{
    long count = 0;
    DdNodePtr *scan = dd->memoryList;

    while (scan != NULL) {
	count += DD_MEM_CHUNK;
	scan = (DdNodePtr *) *scan;
    }
    return(count);

} /* end of Cudd_ReadPeakNodeCount */


/**
  @brief Reports the peak number of live nodes.

  @sideeffect None

  @see Cudd_ReadNodeCount Cudd_PrintInfo Cudd_ReadPeakNodeCount

*/
int
Cudd_ReadPeakLiveNodeCount(
  DdManager * dd)
{
    unsigned int live = dd->keys - dd->dead;

    if (live > dd->peakLiveNodes) {
	dd->peakLiveNodes = live;
    }
    return((int)dd->peakLiveNodes);

} /* end of Cudd_ReadPeakLiveNodeCount */


/**
  @brief Reports the number of nodes in BDDs and ADDs.

  @details This number does not include the isolated projection
  functions and the unused constants. These nodes that are not counted
  are not part of the DDs manipulated by the application.

  @sideeffect None

  @see Cudd_ReadPeakNodeCount Cudd_zddReadNodeCount

*/
long
Cudd_ReadNodeCount(
  DdManager * dd)
{
    long count;
    int i;

#ifndef DD_NO_DEATH_ROW
    cuddClearDeathRow(dd);
#endif

    count = (long) (dd->keys - dd->dead);

    /* Count isolated projection functions. Their number is subtracted
    ** from the node count because they are not part of the BDDs.
    */
    for (i=0; i < dd->size; i++) {
	if (dd->vars[i]->ref == 1) count--;
    }
    /* Subtract from the count the unused constants. */
    if (DD_ZERO(dd)->ref == 1) count--;
    if (DD_PLUS_INFINITY(dd)->ref == 1) count--;
    if (DD_MINUS_INFINITY(dd)->ref == 1) count--;

    return(count);

} /* end of Cudd_ReadNodeCount */



/**
  @brief Reports the number of nodes in ZDDs.

  @details This number always includes the two constants 1 and 0.

  @sideeffect None

  @see Cudd_ReadPeakNodeCount Cudd_ReadNodeCount

*/
long
Cudd_zddReadNodeCount(
  DdManager * dd)
{
    return((long)(dd->keysZ - dd->deadZ + 2));

} /* end of Cudd_zddReadNodeCount */


/**
  @brief Adds a function to a hook.

  @details A hook is a list of
  application-provided functions called on certain occasions by the
  package.

  @return 1 if the function is successfully added; 2 if the function
  was already in the list; 0 otherwise.

  @sideeffect None

  @see Cudd_RemoveHook

*/
int
Cudd_AddHook(
  DdManager * dd,
  DD_HFP f,
  Cudd_HookType where)
{
    DdHook **hook, *nextHook, *newHook;

    switch (where) {
    case CUDD_PRE_GC_HOOK:
	hook = &(dd->preGCHook);
	break;
    case CUDD_POST_GC_HOOK:
	hook = &(dd->postGCHook);
	break;
    case CUDD_PRE_REORDERING_HOOK:
	hook = &(dd->preReorderingHook);
	break;
    case CUDD_POST_REORDERING_HOOK:
	hook = &(dd->postReorderingHook);
	break;
    default:
        return(0);
    }
    /* Scan the list and find whether the function is already there.
    ** If so, just return. */
    nextHook = *hook;
    while (nextHook != NULL) {
	if (nextHook->f == f) {
	    return(2);
	}
	hook = &(nextHook->next);
	nextHook = nextHook->next;
    }
    /* The function was not in the list. Create a new item and append it
    ** to the end of the list. */
    newHook = ALLOC(DdHook,1);
    if (newHook == NULL) {
	dd->errorCode = CUDD_MEMORY_OUT;
	return(0);
    }
    newHook->next = NULL;
    newHook->f = f;
    *hook = newHook;
    return(1);

} /* end of Cudd_AddHook */


/**
  @brief Removes a function from a hook.

  @details A hook is a list of application-provided functions called
  on certain occasions by the package.

  @return 1 if successful; 0 the function was not in the list.

  @sideeffect None

  @see Cudd_AddHook

*/
int
Cudd_RemoveHook(
  DdManager * dd,
  DD_HFP f,
  Cudd_HookType where)
{
    DdHook **hook, *nextHook;

    switch (where) {
    case CUDD_PRE_GC_HOOK:
	hook = &(dd->preGCHook);
	break;
    case CUDD_POST_GC_HOOK:
	hook = &(dd->postGCHook);
	break;
    case CUDD_PRE_REORDERING_HOOK:
	hook = &(dd->preReorderingHook);
	break;
    case CUDD_POST_REORDERING_HOOK:
	hook = &(dd->postReorderingHook);
	break;
    default:
        return(0);
    }
    nextHook = *hook;
    while (nextHook != NULL) {
	if (nextHook->f == f) {
	    *hook = nextHook->next;
	    FREE(nextHook);
	    return(1);
	}
	hook = &(nextHook->next);
	nextHook = nextHook->next;
    }

    return(0);

} /* end of Cudd_RemoveHook */


/**
  @brief Checks whether a function is in a hook.

  @details A hook is a list of application-provided functions called
  on certain occasions by the package.

  @return 1 if the function is found; 0 otherwise.

  @sideeffect None

  @see Cudd_AddHook Cudd_RemoveHook

*/
int
Cudd_IsInHook(
  DdManager * dd,
  DD_HFP f,
  Cudd_HookType where)
{
    DdHook *hook;

    switch (where) {
    case CUDD_PRE_GC_HOOK:
	hook = dd->preGCHook;
	break;
    case CUDD_POST_GC_HOOK:
	hook = dd->postGCHook;
	break;
    case CUDD_PRE_REORDERING_HOOK:
	hook = dd->preReorderingHook;
	break;
    case CUDD_POST_REORDERING_HOOK:
	hook = dd->postReorderingHook;
	break;
    default:
        return(0);
    }
    /* Scan the list and find whether the function is already there. */
    while (hook != NULL) {
	if (hook->f == f) {
	    return(1);
	}
	hook = hook->next;
    }
    return(0);

} /* end of Cudd_IsInHook */


/**
  @brief Sample hook function to call before reordering.

  @details Prints on the manager's stdout reordering method and initial size.
  
  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see Cudd_StdPostReordHook

*/
int
Cudd_StdPreReordHook(
  DdManager *dd,
  const char *str,
  void *data)
{
    Cudd_ReorderingType method = (Cudd_ReorderingType) (ptruint) data;
    int retval;

    retval = fprintf(dd->out,"%s reordering with ", str);
    if (retval == EOF) return(0);
    switch (method) {
    case CUDD_REORDER_SIFT_CONVERGE:
    case CUDD_REORDER_SYMM_SIFT_CONV:
    case CUDD_REORDER_GROUP_SIFT_CONV:
    case CUDD_REORDER_WINDOW2_CONV:
    case CUDD_REORDER_WINDOW3_CONV:
    case CUDD_REORDER_WINDOW4_CONV:
    case CUDD_REORDER_LINEAR_CONVERGE:
	retval = fprintf(dd->out,"converging ");
	if (retval == EOF) return(0);
	break;
    default:
	break;
    }
    switch (method) {
    case CUDD_REORDER_RANDOM:
    case CUDD_REORDER_RANDOM_PIVOT:
	retval = fprintf(dd->out,"random");
	break;
    case CUDD_REORDER_SIFT:
    case CUDD_REORDER_SIFT_CONVERGE:
	retval = fprintf(dd->out,"sifting");
	break;
    case CUDD_REORDER_SYMM_SIFT:
    case CUDD_REORDER_SYMM_SIFT_CONV:
	retval = fprintf(dd->out,"symmetric sifting");
	break;
    case CUDD_REORDER_LAZY_SIFT:
	retval = fprintf(dd->out,"lazy sifting");
	break;
    case CUDD_REORDER_GROUP_SIFT:
    case CUDD_REORDER_GROUP_SIFT_CONV:
	retval = fprintf(dd->out,"group sifting");
	break;
    case CUDD_REORDER_WINDOW2:
    case CUDD_REORDER_WINDOW3:
    case CUDD_REORDER_WINDOW4:
    case CUDD_REORDER_WINDOW2_CONV:
    case CUDD_REORDER_WINDOW3_CONV:
    case CUDD_REORDER_WINDOW4_CONV:
	retval = fprintf(dd->out,"window");
	break;
    case CUDD_REORDER_ANNEALING:
	retval = fprintf(dd->out,"annealing");
	break;
    case CUDD_REORDER_GENETIC:
	retval = fprintf(dd->out,"genetic");
	break;
    case CUDD_REORDER_LINEAR:
    case CUDD_REORDER_LINEAR_CONVERGE:
	retval = fprintf(dd->out,"linear sifting");
	break;
    case CUDD_REORDER_EXACT:
	retval = fprintf(dd->out,"exact");
	break;
    default:
	return(0);
    }
    if (retval == EOF) return(0);

    retval = fprintf(dd->out,": from %ld to ... ", strcmp(str, "BDD") == 0 ?
		     Cudd_ReadNodeCount(dd) : Cudd_zddReadNodeCount(dd));
    if (retval == EOF) return(0);
    fflush(dd->out);
    return(1);

} /* end of Cudd_StdPreReordHook */


/**
  @brief Sample hook function to call after reordering.

  @details Prints on the manager's stdout final size and reordering time.
  
  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see Cudd_StdPreReordHook

*/
int
Cudd_StdPostReordHook(
  DdManager *dd,
  const char *str,
  void *data)
{
    unsigned long initialTime = (unsigned long) (ptruint) data;
    int retval;
    unsigned long finalTime = util_cpu_time();
    double totalTimeSec = (double)(finalTime - initialTime) / 1000.0;

    retval = fprintf(dd->out,"%ld nodes in %g sec\n", strcmp(str, "BDD") == 0 ?
		     Cudd_ReadNodeCount(dd) : Cudd_zddReadNodeCount(dd),
		     totalTimeSec);
    if (retval == EOF) return(0);
    retval = fflush(dd->out);
    if (retval == EOF) return(0);
    return(1);

} /* end of Cudd_StdPostReordHook */


/**
  @brief Enables reporting of reordering stats.

  @return 1 if successful; 0 otherwise.

  @sideeffect Installs functions in the pre-reordering and post-reordering
  hooks.

  @see Cudd_DisableReorderingReporting Cudd_ReorderingReporting

*/
int
Cudd_EnableReorderingReporting(
  DdManager *dd)
{
    if (!Cudd_AddHook(dd, Cudd_StdPreReordHook, CUDD_PRE_REORDERING_HOOK)) {
	return(0);
    }
    if (!Cudd_AddHook(dd, Cudd_StdPostReordHook, CUDD_POST_REORDERING_HOOK)) {
	return(0);
    }
    return(1);

} /* end of Cudd_EnableReorderingReporting */


/**
  @brief Disables reporting of reordering stats.

  @return 1 if successful; 0 otherwise.

  @sideeffect Removes functions from the pre-reordering and post-reordering
  hooks.

  @see Cudd_EnableReorderingReporting Cudd_ReorderingReporting

*/
int
Cudd_DisableReorderingReporting(
  DdManager *dd)
{
    if (!Cudd_RemoveHook(dd, Cudd_StdPreReordHook, CUDD_PRE_REORDERING_HOOK)) {
	return(0);
    }
    if (!Cudd_RemoveHook(dd, Cudd_StdPostReordHook, CUDD_POST_REORDERING_HOOK)) {
	return(0);
    }
    return(1);

} /* end of Cudd_DisableReorderingReporting */


/**
  @brief Returns 1 if reporting of reordering stats is enabled; 0
  otherwise.

  @sideeffect none

  @see Cudd_EnableReorderingReporting Cudd_DisableReorderingReporting

*/
int
Cudd_ReorderingReporting(
  DdManager *dd)
{
    return(Cudd_IsInHook(dd, Cudd_StdPreReordHook, CUDD_PRE_REORDERING_HOOK));

} /* end of Cudd_ReorderingReporting */


/**
  @brief Hook function to print the current variable order.

  @details It may be called before or after reordering. Prints on the
  manager's stdout a parenthesized list that describes the variable
  groups.

  @return 1 if successful; 0 otherwise.

  @sideeffect None

  @see Cudd_StdPreReordHook

*/
int
Cudd_PrintGroupedOrder(
  DdManager * dd,
  const char *str,
  void *data)
{
    (void) data; /* avoid warning */
    int isBdd = strcmp(str, "ZDD");
    MtrNode *tree = isBdd ? dd->tree : dd->treeZ;
    int *invperm = isBdd ? dd->invperm : dd->invpermZ;
    int size = isBdd ? dd->size : dd->sizeZ;
    if (tree == NULL) {
        int i, retval;
        for (i=0; i < size; i++) {
            retval = fprintf(dd->out, "%c%d", i==0 ? '(' : ',', invperm[i]);
            if (retval == EOF) return(0);
        }
        retval = fprintf(dd->out,")\n");
        return (retval != EOF);
    } else {
        return Mtr_PrintGroupedOrder(tree,invperm,dd->out);
    }
        
} /* end of Cudd_PrintGroupedOrder */


/**
  @brief Enables monitoring of ordering.

  @return 1 if successful; 0 otherwise.

  @sideeffect Installs functions in the pre-reordering and post-reordering
  hooks.

  @see Cudd_EnableReorderingReporting

*/
int
Cudd_EnableOrderingMonitoring(
  DdManager *dd)
{
    if (!Cudd_AddHook(dd, Cudd_PrintGroupedOrder, CUDD_PRE_REORDERING_HOOK)) {
	return(0);
    }
    if (!Cudd_AddHook(dd, Cudd_StdPreReordHook, CUDD_PRE_REORDERING_HOOK)) {
	return(0);
    }
    if (!Cudd_AddHook(dd, Cudd_StdPostReordHook, CUDD_POST_REORDERING_HOOK)) {
	return(0);
    }
    if (!Cudd_AddHook(dd, Cudd_PrintGroupedOrder, CUDD_POST_REORDERING_HOOK)) {
	return(0);
    }
    return(1);

} /* end of Cudd_EnableOrderingMonitoring */


/**
  @brief Disables monitoring of ordering.

  @return 1 if successful; 0 otherwise.

  @sideeffect Removes functions from the pre-reordering and post-reordering
  hooks.

  @see Cudd_EnableOrderingMonitoring

*/
int
Cudd_DisableOrderingMonitoring(
  DdManager *dd)
{
    if (!Cudd_RemoveHook(dd, Cudd_StdPreReordHook, CUDD_PRE_REORDERING_HOOK)) {
	return(0);
    }
    if (!Cudd_RemoveHook(dd, Cudd_PrintGroupedOrder, CUDD_PRE_REORDERING_HOOK)) {
	return(0);
    }
    if (!Cudd_RemoveHook(dd, Cudd_PrintGroupedOrder, CUDD_POST_REORDERING_HOOK)) {
	return(0);
    }
    if (!Cudd_RemoveHook(dd, Cudd_StdPostReordHook, CUDD_POST_REORDERING_HOOK)) {
	return(0);
    }
    return(1);

} /* end of Cudd_DisableOrderingMonitoring */


/**
  @brief Returns 1 if monitoring of ordering is enabled; 0 otherwise.

  @sideeffect none

  @see Cudd_EnableOrderingMonitoring Cudd_DisableOrderingMonitoring

*/
int
Cudd_OrderingMonitoring(
  DdManager *dd)
{
    return(Cudd_IsInHook(dd, Cudd_PrintGroupedOrder, CUDD_PRE_REORDERING_HOOK));

} /* end of Cudd_OrderingMonitoring */


/**
  @brief Sets the application hook.

  @sideeffect None

  @see Cudd_ReadApplicationHook

*/
void
Cudd_SetApplicationHook(
  DdManager *dd,
  void * value)
{
    dd->hooks = value;  

} /* end of Cudd_SetApplicationHook */


/**
  @brief Reads the application hook.

  @sideeffect None

  @see Cudd_SetApplicationHook

*/
void *
Cudd_ReadApplicationHook(
  DdManager *dd)
{
    return(dd->hooks);  

} /* end of Cudd_ReadApplicationHook */


/**
  @brief Returns the code of the last error.

  @details The error codes are defined in cudd.h.

  @sideeffect None

  @see Cudd_ClearErrorCode

*/
Cudd_ErrorType
Cudd_ReadErrorCode(
  DdManager *dd)
{
    return(dd->errorCode);

} /* end of Cudd_ReadErrorCode */


/**
  @brief Clear the error code of a manager.

  @sideeffect None

  @see Cudd_ReadErrorCode

*/
void
Cudd_ClearErrorCode(
  DdManager *dd)
{
    dd->errorCode = CUDD_NO_ERROR;

} /* end of Cudd_ClearErrorCode */


/**
  @brief Installs a handler for failed memory allocations.

  @details Changing the handler only has an effect if the wrappers
  in safe_mem.c are in use.

  @return the current handler.
*/
DD_OOMFP
Cudd_InstallOutOfMemoryHandler(
  DD_OOMFP newHandler)
{
    DD_OOMFP oldHandler = MMoutOfMemory;
    MMoutOfMemory = newHandler;
    return oldHandler;

} /* end of Cudd_InstallOutOfMemoryHandler */


/**
  @brief Reads the stdout of a manager.

  @details This is the file pointer to which messages normally going
  to stdout are written. It is initialized to stdout. Cudd_SetStdout
  allows the application to redirect it.

  @sideeffect None

  @see Cudd_SetStdout Cudd_ReadStderr

*/
FILE *
Cudd_ReadStdout(
  DdManager *dd)
{
    return(dd->out);

} /* end of Cudd_ReadStdout */


/**
  @brief Sets the stdout of a manager.

  @sideeffect None

  @see Cudd_ReadStdout Cudd_SetStderr

*/
void
Cudd_SetStdout(
  DdManager *dd,
  FILE *fp)
{
    dd->out = fp;

} /* end of Cudd_SetStdout */


/**
  @brief Reads the stderr of a manager.

  @details This is the file pointer to which messages normally going
  to stderr are written. It is initialized to stderr. Cudd_SetStderr
  allows the application to redirect it.

  @sideeffect None

  @see Cudd_SetStderr Cudd_ReadStdout

*/
FILE *
Cudd_ReadStderr(
  DdManager *dd)
{
    return(dd->err);

} /* end of Cudd_ReadStderr */


/**
  @brief Sets the stderr of a manager.

  @sideeffect None

  @see Cudd_ReadStderr Cudd_SetStdout

*/
void
Cudd_SetStderr(
  DdManager *dd,
  FILE *fp)
{
    dd->err = fp;

} /* end of Cudd_SetStderr */


/**
  @brief Returns the threshold for the next dynamic reordering.

  @details The threshold is in terms of number of nodes and is in
  effect only if reordering is enabled. The count does not include the
  dead nodes, unless the countDead parameter of the manager has been
  changed from its default setting.

  @sideeffect None

  @see Cudd_SetNextReordering

*/
unsigned int
Cudd_ReadNextReordering(
  DdManager *dd)
{
    return(dd->nextDyn);

} /* end of Cudd_ReadNextReordering */


/**
  @brief Sets the threshold for the next dynamic reordering.

  @details The threshold is in terms of number of nodes and is in
  effect only if reordering is enabled. The count does not include the
  dead nodes, unless the countDead parameter of the manager has been
  changed from its default setting.

  @sideeffect None

  @see Cudd_ReadNextReordering

*/
void
Cudd_SetNextReordering(
  DdManager *dd,
  unsigned int next)
{
    dd->nextDyn = next;

} /* end of Cudd_SetNextReordering */


/**
  @brief Reads the number of elementary reordering steps.

  @sideeffect none

*/
double
Cudd_ReadSwapSteps(
  DdManager *dd)
{
#ifdef DD_COUNT
    return(dd->swapSteps);
#else
    (void) dd; /* avoid warning */
    return(-1);
#endif

} /* end of Cudd_ReadSwapSteps */


/**
  @brief Reads the maximum allowed number of live nodes.

  @details When this number is exceeded, the package returns NULL.

  @sideeffect none

  @see Cudd_SetMaxLive

*/
unsigned int
Cudd_ReadMaxLive(
  DdManager *dd)
{
    return(dd->maxLive);

} /* end of Cudd_ReadMaxLive */


/**
  @brief Sets the maximum allowed number of live nodes.

  @details When this number is exceeded, the package returns NULL.

  @sideeffect none

  @see Cudd_ReadMaxLive

*/
void
Cudd_SetMaxLive(
  DdManager *dd,
  unsigned int maxLive)
{
    dd->maxLive = maxLive;

} /* end of Cudd_SetMaxLive */


/**
  @brief Reads the maximum allowed memory.

  @details When this number is exceeded, the package returns NULL.

  @sideeffect none

  @see Cudd_SetMaxMemory

*/
size_t
Cudd_ReadMaxMemory(
  DdManager *dd)
{
    return(dd->maxmemhard);

} /* end of Cudd_ReadMaxMemory */


/**
  @brief Sets the maximum allowed memory.

  @details When this number is exceeded, the package returns NULL.

  @return the previous limit.

  @sideeffect none

  @see Cudd_ReadMaxMemory

*/
size_t
Cudd_SetMaxMemory(
  DdManager *dd,
  size_t maxMemory)
{
    size_t oldLimit = dd->maxmemhard;
    dd->maxmemhard = maxMemory;
    return oldLimit;

} /* end of Cudd_SetMaxMemory */


/**
  @brief Prevents sifting of a variable.

  @details This function sets a flag to prevent sifting of a
  variable.

  @return 1 if successful; 0 otherwise (i.e., invalid variable index).

  @sideeffect Changes the "bindVar" flag in DdSubtable.

  @see Cudd_bddUnbindVar

*/
int
Cudd_bddBindVar(
  DdManager *dd /**< manager */,
  int index /**< variable index */)
{
    if (index >= dd->size || index < 0) return(0);
    dd->subtables[dd->perm[index]].bindVar = 1;
    return(1);

} /* end of Cudd_bddBindVar */


/**
  @brief Allows the sifting of a variable.

  @details This function resets the flag that prevents the sifting
  of a variable. In successive variable reorderings, the variable will
  NOT be skipped, that is, sifted.  Initially all variables can be
  sifted. It is necessary to call this function only to re-enable
  sifting after a call to Cudd_bddBindVar.

  @return 1 if successful; 0 otherwise (i.e., invalid variable index).

  @sideeffect Changes the "bindVar" flag in DdSubtable.

  @see Cudd_bddBindVar

*/
int
Cudd_bddUnbindVar(
  DdManager *dd /**< manager */,
  int index /**< variable index */)
{
    if (index >= dd->size || index < 0) return(0);
    dd->subtables[dd->perm[index]].bindVar = 0;
    return(1);

} /* end of Cudd_bddUnbindVar */


/**
  @brief Tells whether a variable can be sifted.

  @details This function returns 1 if a variable is enabled for
  sifting.  Initially all variables can be sifted. This function
  returns 0 if there has been a previous call to Cudd_bddBindVar for
  that variable not followed by a call to Cudd_bddUnbindVar. The
  function returns 0 also in the case in which the index of the
  variable is out of bounds.

  @sideeffect none

  @see Cudd_bddBindVar Cudd_bddUnbindVar

*/
int
Cudd_bddVarIsBound(
  DdManager *dd /**< manager */,
  int index /**< variable index */)
{
    if (index >= dd->size || index < 0) return(0);
    return(dd->subtables[dd->perm[index]].bindVar);

} /* end of Cudd_bddVarIsBound */


/**
  @brief Sets a variable type to primary input.

  @details The variable type is used by lazy sifting.

  @return 1 if successful; 0 otherwise.

  @sideeffect modifies the manager

  @see Cudd_bddSetPsVar Cudd_bddSetNsVar Cudd_bddIsPiVar

*/
int
Cudd_bddSetPiVar(
  DdManager *dd /**< manager */,
  int index /**< variable index */)
{
    if (index >= dd->size || index < 0) return (0);
    dd->subtables[dd->perm[index]].varType = CUDD_VAR_PRIMARY_INPUT;
    return(1);

} /* end of Cudd_bddSetPiVar */


/**
  @brief Sets a variable type to present state.

  @details The variable type is used by lazy sifting.

  @return 1 if successful; 0 otherwise.

  @sideeffect modifies the manager

  @see Cudd_bddSetPiVar Cudd_bddSetNsVar Cudd_bddIsPsVar

*/
int
Cudd_bddSetPsVar(
  DdManager *dd /**< manager */,
  int index /**< variable index */)
{
    if (index >= dd->size || index < 0) return (0);
    dd->subtables[dd->perm[index]].varType = CUDD_VAR_PRESENT_STATE;
    return(1);

} /* end of Cudd_bddSetPsVar */


/**
  @brief Sets a variable type to next state.

  @details The variable type is used by lazy sifting.

  @return 1 if successful; 0 otherwise.

  @sideeffect modifies the manager

  @see Cudd_bddSetPiVar Cudd_bddSetPsVar Cudd_bddIsNsVar

*/
int
Cudd_bddSetNsVar(
  DdManager *dd /**< manager */,
  int index /**< variable index */)
{
    if (index >= dd->size || index < 0) return (0);
    dd->subtables[dd->perm[index]].varType = CUDD_VAR_NEXT_STATE;
    return(1);

} /* end of Cudd_bddSetNsVar */


/**
  @brief Checks whether a variable is primary input.

  @return 1 if the variable's type is primary input; 0 if the variable
  exists but is not a primary input; -1 if the variable does not
  exist.

  @sideeffect none

  @see Cudd_bddSetPiVar Cudd_bddIsPsVar Cudd_bddIsNsVar

*/
int
Cudd_bddIsPiVar(
  DdManager *dd /**< manager */,
  int index /**< variable index */)
{
    if (index >= dd->size || index < 0) return -1;
    return (dd->subtables[dd->perm[index]].varType == CUDD_VAR_PRIMARY_INPUT);

} /* end of Cudd_bddIsPiVar */


/**
  @brief Checks whether a variable is present state.

  @return 1 if the variable's type is present state; 0 if the variable
  exists but is not a present state; -1 if the variable does not
  exist.

  @sideeffect none

  @see Cudd_bddSetPsVar Cudd_bddIsPiVar Cudd_bddIsNsVar

*/
int
Cudd_bddIsPsVar(
  DdManager *dd,
  int index)
{
    if (index >= dd->size || index < 0) return -1;
    return (dd->subtables[dd->perm[index]].varType == CUDD_VAR_PRESENT_STATE);

} /* end of Cudd_bddIsPsVar */


/**
  @brief Checks whether a variable is next state.

  @return 1 if the variable's type is present state; 0 if the variable
  exists but is not a present state; -1 if the variable does not
  exist.

  @sideeffect none

  @see Cudd_bddSetNsVar Cudd_bddIsPiVar Cudd_bddIsPsVar

*/
int
Cudd_bddIsNsVar(
  DdManager *dd,
  int index)
{
    if (index >= dd->size || index < 0) return -1;
    return (dd->subtables[dd->perm[index]].varType == CUDD_VAR_NEXT_STATE);

} /* end of Cudd_bddIsNsVar */


/**
  @brief Sets a corresponding pair index for a given index.

  @details These pair indices are present and next state variable.

  @return 1 if successful; 0 otherwise.

  @sideeffect modifies the manager

  @see Cudd_bddReadPairIndex

*/
int
Cudd_bddSetPairIndex(
  DdManager *dd /**< manager */,
  int index /**< variable index */,
  int pairIndex /**< corresponding variable index */)
{
    if (index >= dd->size || index < 0) return(0);
    dd->subtables[dd->perm[index]].pairIndex = pairIndex;
    return(1);

} /* end of Cudd_bddSetPairIndex */


/**
  @brief Reads a corresponding pair index for a given index.

  @details These pair indices are present and next state variable.

  @return the corresponding variable index if the variable exists; -1
  otherwise.

  @sideeffect modifies the manager

  @see Cudd_bddSetPairIndex

*/
int
Cudd_bddReadPairIndex(
  DdManager *dd,
  int index)
{
    if (index >= dd->size || index < 0) return -1;
    return dd->subtables[dd->perm[index]].pairIndex;

} /* end of Cudd_bddReadPairIndex */


/**
  @brief Sets a variable to be grouped.

  @details This function is used for lazy sifting.

  @return 1 if successful; 0 otherwise.

  @sideeffect modifies the manager

  @see Cudd_bddSetVarHardGroup Cudd_bddResetVarToBeGrouped

*/
int
Cudd_bddSetVarToBeGrouped(
  DdManager *dd,
  int index)
{
    if (index >= dd->size || index < 0) return(0);
    if (dd->subtables[dd->perm[index]].varToBeGrouped <= CUDD_LAZY_SOFT_GROUP) {
	dd->subtables[dd->perm[index]].varToBeGrouped = CUDD_LAZY_SOFT_GROUP;
    }
    return(1);

} /* end of Cudd_bddSetVarToBeGrouped */


/**
  @brief Sets a variable to be a hard group.

  @details This function is used for lazy sifting.

  @return 1 if successful; 0 otherwise.

  @sideeffect modifies the manager

  @see Cudd_bddSetVarToBeGrouped Cudd_bddResetVarToBeGrouped
  Cudd_bddIsVarHardGroup

*/
int
Cudd_bddSetVarHardGroup(
  DdManager *dd,
  int index)
{
    if (index >= dd->size || index < 0) return(0);
    dd->subtables[dd->perm[index]].varToBeGrouped = CUDD_LAZY_HARD_GROUP;
    return(1);

} /* end of Cudd_bddSetVarHardGrouped */


/**
  @brief Resets a variable not to be grouped.

  @details This function is used for lazy sifting.

  @return 1 if successful; 0 otherwise.

  @sideeffect modifies the manager

  @see Cudd_bddSetVarToBeGrouped Cudd_bddSetVarHardGroup

*/
int
Cudd_bddResetVarToBeGrouped(
  DdManager *dd,
  int index)
{
    if (index >= dd->size || index < 0) return(0);
    if (dd->subtables[dd->perm[index]].varToBeGrouped <=
	CUDD_LAZY_SOFT_GROUP) {
	dd->subtables[dd->perm[index]].varToBeGrouped = CUDD_LAZY_NONE;
    }
    return(1);

} /* end of Cudd_bddResetVarToBeGrouped */


/**
  @brief Checks whether a variable is set to be grouped.

  @details This function is used for lazy sifting.

  @sideeffect none

*/
int
Cudd_bddIsVarToBeGrouped(
  DdManager *dd,
  int index)
{
    if (index >= dd->size || index < 0) return(-1);
    if (dd->subtables[dd->perm[index]].varToBeGrouped == CUDD_LAZY_UNGROUP)
	return(0);
    else
	return(dd->subtables[dd->perm[index]].varToBeGrouped);

} /* end of Cudd_bddIsVarToBeGrouped */


/**
  @brief Sets a variable to be ungrouped.

  @details This function is used for lazy sifting.

  @return 1 if successful; 0 otherwise.

  @sideeffect modifies the manager

  @see Cudd_bddIsVarToBeUngrouped

*/
int
Cudd_bddSetVarToBeUngrouped(
  DdManager *dd,
  int index)
{
    if (index >= dd->size || index < 0) return(0);
    dd->subtables[dd->perm[index]].varToBeGrouped = CUDD_LAZY_UNGROUP;
    return(1);

} /* end of Cudd_bddSetVarToBeGrouped */


/**
  @brief Checks whether a variable is set to be ungrouped.

  @details This function is used for lazy sifting.

  @return 1 if the variable is marked to be ungrouped; 0 if the
  variable exists, but it is not marked to be ungrouped; -1 if the
  variable does not exist.

  @sideeffect none

  @see Cudd_bddSetVarToBeUngrouped

*/
int
Cudd_bddIsVarToBeUngrouped(
  DdManager *dd,
  int index)
{
    if (index >= dd->size || index < 0) return(-1);
    return dd->subtables[dd->perm[index]].varToBeGrouped == CUDD_LAZY_UNGROUP;

} /* end of Cudd_bddIsVarToBeGrouped */


/**
  @brief Checks whether a variable is set to be in a hard group.

  @details This function is used for lazy sifting.

  @return 1 if the variable is marked to be in a hard group; 0 if the
  variable exists, but it is not marked to be in a hard group; -1 if
  the variable does not exist.

  @sideeffect none

  @see Cudd_bddSetVarHardGroup

*/
int
Cudd_bddIsVarHardGroup(
  DdManager *dd,
  int index)
{
    if (index >= dd->size || index < 0) return(-1);
    if (dd->subtables[dd->perm[index]].varToBeGrouped == CUDD_LAZY_HARD_GROUP)
	return(1);
    return(0);

} /* end of Cudd_bddIsVarToBeGrouped */


/*---------------------------------------------------------------------------*/
/* Definition of internal functions                                          */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Definition of static functions                                            */
/*---------------------------------------------------------------------------*/


/**
  @brief Fixes a variable group tree.

  @sideeffect Changes the variable group tree.

*/
static void
fixVarTree(
  MtrNode * treenode,
  int * perm,
  int  size)
{
    treenode->index = treenode->low;
    treenode->low = ((int) treenode->index < size) ?
	(MtrHalfWord) perm[treenode->index] : treenode->index;
    if (treenode->child != NULL)
	fixVarTree(treenode->child, perm, size);
    if (treenode->younger != NULL)
	fixVarTree(treenode->younger, perm, size);
    return;

} /* end of fixVarTree */


/**
  @brief Adds multiplicity groups to a %ZDD variable group tree.

  @details This function creates the groups for set of %ZDD variables
  (whose cardinality is given by parameter multiplicity) that are
  created for each %BDD variable in Cudd_zddVarsFromBddVars. The crux
  of the matter is to determine the index each new group. (The index
  of the first variable in the group.)  We first build all the groups
  for the children of a node, and then deal with the %ZDD variables
  that are directly attached to the node. The problem for these is
  that the tree itself does not provide information on their position
  inside the group. While we deal with the children of the node,
  therefore, we keep track of all the positions they occupy. The
  remaining positions in the tree can be freely used. Also, we keep
  track of all the variables placed in the children. All the remaining
  variables are directly attached to the group. We can then place any
  pair of variables not yet grouped in any pair of available positions
  in the node.

  @return 1 if successful; 0 otherwise.

  @sideeffect Changes the variable group tree.

  @see Cudd_zddVarsFromBddVars

*/
static int
addMultiplicityGroups(
  DdManager *dd /**< manager */,
  MtrNode *treenode /**< current tree node */,
  int multiplicity /**< how many %ZDD vars per %BDD var */,
  char *vmask /**< variable pairs for which a group has been already built */,
  char *lmask /**< levels for which a group has already been built*/)
{
    int startV, stopV, startL;
    int i, j;
    MtrNode *auxnode = treenode;

    while (auxnode != NULL) {
	if (auxnode->child != NULL) {
	    addMultiplicityGroups(dd,auxnode->child,multiplicity,vmask,lmask);
	}
	/* Build remaining groups. */
	startV = dd->permZ[auxnode->index] / multiplicity;
	startL = auxnode->low / multiplicity;
	stopV = startV + auxnode->size / multiplicity;
	/* Walk down vmask starting at startV and build missing groups. */
	for (i = startV, j = startL; i < stopV; i++) {
	    if (vmask[i] == 0) {
		MtrNode *node;
		while (lmask[j] == 1) j++;
		node = Mtr_MakeGroup(auxnode, j * multiplicity, multiplicity,
				     MTR_FIXED);
		if (node == NULL) {
		    return(0);
		}
		node->index = dd->invpermZ[i * multiplicity];
		vmask[i] = 1;
		lmask[j] = 1;
	    }
	}
	auxnode = auxnode->younger;
    }
    return(1);

} /* end of addMultiplicityGroups */
