/**
  @file 

  @ingroup cudd

  @brief The University of Colorado decision diagram package.

  @details External functions and data strucures of the CUDD package.
  <ul>
  <li> To turn on the gathering of statistics, define DD_STATS.
  <li> To turn on additional debugging code, define DD_DEBUG.
  </ul>

  @author Fabio Somenzi
  @author Modified by Abelardo Pardo to interface it to VIS

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

#ifndef CUDD_H_
#define CUDD_H_

/*---------------------------------------------------------------------------*/
/* Nested includes                                                           */
/*---------------------------------------------------------------------------*/

#include <inttypes.h>

/*---------------------------------------------------------------------------*/
/* Constant declarations                                                     */
/*---------------------------------------------------------------------------*/

#define CUDD_TRUE 1 /**< readable true */
#define CUDD_FALSE 0 /**< readable false */

/**
 * @brief Value returned my many functions when memory is exhausted.
 */
#define CUDD_OUT_OF_MEM		-1
/* The sizes of the subtables and the cache must be powers of two. */
#define CUDD_UNIQUE_SLOTS	256	/**< Initial size of subtables */
#define CUDD_CACHE_SLOTS	262144	/**< Default size of the cache */

/* Constants for residue functions. */
#define CUDD_RESIDUE_DEFAULT	0
#define CUDD_RESIDUE_MSB	1
#define CUDD_RESIDUE_TC		2

/*---------------------------------------------------------------------------*/
/* Stucture declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Type declarations                                                         */
/*---------------------------------------------------------------------------*/

/**
  @brief Type of reordering algorithm.
*/
typedef enum {
    CUDD_REORDER_SAME,
    CUDD_REORDER_NONE,
    CUDD_REORDER_RANDOM,
    CUDD_REORDER_RANDOM_PIVOT,
    CUDD_REORDER_SIFT,
    CUDD_REORDER_SIFT_CONVERGE,
    CUDD_REORDER_SYMM_SIFT,
    CUDD_REORDER_SYMM_SIFT_CONV,
    CUDD_REORDER_WINDOW2,
    CUDD_REORDER_WINDOW3,
    CUDD_REORDER_WINDOW4,
    CUDD_REORDER_WINDOW2_CONV,
    CUDD_REORDER_WINDOW3_CONV,
    CUDD_REORDER_WINDOW4_CONV,
    CUDD_REORDER_GROUP_SIFT,
    CUDD_REORDER_GROUP_SIFT_CONV,
    CUDD_REORDER_ANNEALING,
    CUDD_REORDER_GENETIC,
    CUDD_REORDER_LINEAR,
    CUDD_REORDER_LINEAR_CONVERGE,
    CUDD_REORDER_LAZY_SIFT,
    CUDD_REORDER_EXACT
} Cudd_ReorderingType;


/**
  @brief Type of aggregation methods.
*/
typedef enum {
    CUDD_NO_CHECK,
    CUDD_GROUP_CHECK,
    CUDD_GROUP_CHECK2,
    CUDD_GROUP_CHECK3,
    CUDD_GROUP_CHECK4,
    CUDD_GROUP_CHECK5,
    CUDD_GROUP_CHECK6,
    CUDD_GROUP_CHECK7,
    CUDD_GROUP_CHECK8,
    CUDD_GROUP_CHECK9
} Cudd_AggregationType;


/**
  @brief Type of hooks.
*/
typedef enum {
    CUDD_PRE_GC_HOOK,
    CUDD_POST_GC_HOOK,
    CUDD_PRE_REORDERING_HOOK,
    CUDD_POST_REORDERING_HOOK
} Cudd_HookType;


/**
  @brief Type of error codes.
*/
typedef enum {
    CUDD_NO_ERROR,
    CUDD_MEMORY_OUT,
    CUDD_TOO_MANY_NODES,
    CUDD_MAX_MEM_EXCEEDED,
    CUDD_TIMEOUT_EXPIRED,
    CUDD_TERMINATION,
    CUDD_INVALID_ARG,
    CUDD_INTERNAL_ERROR
} Cudd_ErrorType;


/**
  @brief Group type for lazy sifting.
*/
typedef enum {
    CUDD_LAZY_NONE,
    CUDD_LAZY_SOFT_GROUP,
    CUDD_LAZY_HARD_GROUP,
    CUDD_LAZY_UNGROUP
} Cudd_LazyGroupType;


/**
  @brief Variable type.

  @details Used only in lazy sifting.

*/
typedef enum {
    CUDD_VAR_PRIMARY_INPUT,
    CUDD_VAR_PRESENT_STATE,
    CUDD_VAR_NEXT_STATE
} Cudd_VariableType;

/**
   @brief Type of the value of a terminal node.
*/
typedef double CUDD_VALUE_TYPE;

/**
   @brief Type of the decision diagram node.
*/
typedef struct DdNode DdNode;

/**
   @brief Type of a pointer to a decision diagram node.
*/
typedef DdNode *DdNodePtr;

/**
   @brief CUDD manager.
*/
typedef struct DdManager DdManager;

/**
   @brief CUDD generator.
*/
typedef struct DdGen DdGen;

/**
   @brief Type of an arbitrary precision integer "digit."
*/
typedef uint32_t DdApaDigit;

/**
   @brief Type of an arbitrary precision intger, which is an array of digits.
*/
typedef DdApaDigit * DdApaNumber;

/**
   @brief Type of a const-qualified arbitrary precision integer.
*/
typedef DdApaDigit const * DdConstApaNumber;

/**
   @brief Return type for function computing two-literal clauses.
*/
typedef struct DdTlcInfo DdTlcInfo;

/**
   @brief Type of hook function.
*/
typedef int (*DD_HFP)(DdManager *, const char *, void *);
/**
   @brief Type of priority function
*/
typedef DdNode * (*DD_PRFP)(DdManager * , int, DdNode **, DdNode **, DdNode **);
/**
   @brief Type of apply operator.
*/
typedef DdNode * (*DD_AOP)(DdManager *, DdNode **, DdNode **);
/**
   @brief Type of monadic apply operator.
*/
typedef DdNode * (*DD_MAOP)(DdManager *, DdNode *);
/**
   @brief Type of two-operand cache tag functions.
*/
typedef DdNode * (*DD_CTFP)(DdManager *, DdNode *, DdNode *);
/**
   @brief Type of one-operand cache tag functions.
*/
typedef DdNode * (*DD_CTFP1)(DdManager *, DdNode *);
/**
   @brief Type of memory-out function.
*/
typedef void (*DD_OOMFP)(size_t);
/**
   @brief Type of comparison function for qsort.
*/
typedef int (*DD_QSFP)(const void *, const void *);
/**
   @brief Type of termination handler.
*/
typedef int (*DD_THFP)(const void *);
/**
   @brief Type of timeout handler.
*/
typedef void (*DD_TOHFP)(DdManager *, void *);

/*---------------------------------------------------------------------------*/
/* Variable declarations                                                     */
/*---------------------------------------------------------------------------*/


/*---------------------------------------------------------------------------*/
/* Macro declarations                                                        */
/*---------------------------------------------------------------------------*/


/**
  @brief Complements a %DD.

  @details Complements a %DD by flipping the complement attribute of
  the pointer (the least significant bit).

  @sideeffect none

  @see Cudd_NotCond

*/
#define Cudd_Not(node) ((DdNode *)((uintptr_t)(node) ^ (uintptr_t) 01))


/**
  @brief Complements a %DD if a condition is true.

  @details Complements a %DD if condition c is true; c should be
  either 0 or 1, because it is used directly (for efficiency). If in
  doubt on the values c may take, use "(c) ? Cudd_Not(node) : node".

  @sideeffect none

  @see Cudd_Not

*/
#define Cudd_NotCond(node,c) ((DdNode *)((uintptr_t)(node) ^ (uintptr_t) (c)))


/**
  @brief Returns the regular version of a pointer.

  @details 

  @sideeffect none

  @see Cudd_Complement Cudd_IsComplement

*/
#define Cudd_Regular(node) ((DdNode *)((uintptr_t)(node) & ~(uintptr_t) 01))


/**
  @brief Returns the complemented version of a pointer.

  @details 

  @sideeffect none

  @see Cudd_Regular Cudd_IsComplement

*/
#define Cudd_Complement(node) ((DdNode *)((uintptr_t)(node) | (uintptr_t) 01))


/**
  @brief Returns 1 if a pointer is complemented.

  @details 

  @sideeffect none

  @see Cudd_Regular Cudd_Complement

*/
#define Cudd_IsComplement(node) ((int) ((uintptr_t) (node) & (uintptr_t) 01))


/**
  @brief Returns the current position in the order of variable
  index.

  @details Returns the current position in the order of variable
  index. This macro is obsolete and is kept for compatibility. New
  applications should use Cudd_ReadPerm instead.

  @sideeffect none

  @see Cudd_ReadPerm

*/
#define Cudd_ReadIndex(dd,index) (Cudd_ReadPerm(dd,index))


/**
  @brief Iterates over the cubes of a decision diagram.

  @details Iterates over the cubes of a decision diagram f.
  <ul>
  <li> DdManager *manager;
  <li> DdNode *f;
  <li> DdGen *gen;
  <li> int *cube;
  <li> CUDD_VALUE_TYPE value;
  </ul>
  Cudd_ForeachCube allocates and frees the generator. Therefore the
  application should not try to do that. Also, the cube is freed at the
  end of Cudd_ForeachCube and hence is not available outside of the loop.<p>
  CAUTION: It is assumed that dynamic reordering will not occur while
  there are open generators. It is the user's responsibility to make sure
  that dynamic reordering does not occur. As long as new nodes are not created
  during generation, and dynamic reordering is not called explicitly,
  dynamic reordering will not occur. Alternatively, it is sufficient to
  disable dynamic reordering. It is a mistake to dispose of a diagram
  on which generation is ongoing.

  @sideeffect none

  @see Cudd_ForeachNode Cudd_FirstCube Cudd_NextCube Cudd_GenFree
  Cudd_IsGenEmpty Cudd_AutodynDisable

*/
#define Cudd_ForeachCube(manager, f, gen, cube, value)\
    for((gen) = Cudd_FirstCube(manager, f, &cube, &value);\
	Cudd_IsGenEmpty(gen) ? Cudd_GenFree(gen) : CUDD_TRUE;\
	(void) Cudd_NextCube(gen, &cube, &value))


/**
  @brief Iterates over the primes of a Boolean function.

  @details Iterates over the primes of a Boolean function producing
  a prime, but not necessarily irredundant, cover.
  <ul>
  <li> DdManager *manager;
  <li> DdNode *l;
  <li> DdNode *u;
  <li> DdGen *gen;
  <li> int *cube;
  </ul>
  The Boolean function is described by an upper bound and a lower bound.  If
  the function is completely specified, the two bounds coincide.
  Cudd_ForeachPrime allocates and frees the generator.  Therefore the
  application should not try to do that.  Also, the cube is freed at the
  end of Cudd_ForeachPrime and hence is not available outside of the loop.<p>
  CAUTION: It is a mistake to change a diagram on which generation is ongoing.

  @sideeffect none

  @see Cudd_ForeachCube Cudd_FirstPrime Cudd_NextPrime Cudd_GenFree
  Cudd_IsGenEmpty

*/
#define Cudd_ForeachPrime(manager, l, u, gen, cube)\
    for((gen) = Cudd_FirstPrime(manager, l, u, &cube);\
	Cudd_IsGenEmpty(gen) ? Cudd_GenFree(gen) : CUDD_TRUE;\
	(void) Cudd_NextPrime(gen, &cube))


/**
  @brief Iterates over the nodes of a decision diagram.

  @details Iterates over the nodes of a decision diagram f.
  <ul>
  <li> DdManager *manager;
  <li> DdNode *f;
  <li> DdGen *gen;
  <li> DdNode *node;
  </ul>
  The nodes are returned in a seemingly random order.
  Cudd_ForeachNode allocates and frees the generator. Therefore the
  application should not try to do that.<p>
  CAUTION: It is assumed that dynamic reordering will not occur while
  there are open generators. It is the user's responsibility to make sure
  that dynamic reordering does not occur. As long as new nodes are not created
  during generation, and dynamic reordering is not called explicitly,
  dynamic reordering will not occur. Alternatively, it is sufficient to
  disable dynamic reordering. It is a mistake to dispose of a diagram
  on which generation is ongoing.

  @sideeffect none

  @see Cudd_ForeachCube Cudd_FirstNode Cudd_NextNode Cudd_GenFree
  Cudd_IsGenEmpty Cudd_AutodynDisable

*/
#define Cudd_ForeachNode(manager, f, gen, node)\
    for((gen) = Cudd_FirstNode(manager, f, &node);\
	Cudd_IsGenEmpty(gen) ? Cudd_GenFree(gen) : CUDD_TRUE;\
	(void) Cudd_NextNode(gen, &node))


/**
  @brief Iterates over the paths of a %ZDD.

  @details Iterates over the paths of a %ZDD f.
  <ul>
  <li> DdManager *manager;
  <li> DdNode *f;
  <li> DdGen *gen;
  <li> int *path;
  </ul>
  Cudd_zddForeachPath allocates and frees the generator. Therefore the
  application should not try to do that. Also, the path is freed at the
  end of Cudd_zddForeachPath and hence is not available outside of the loop.<p>
  CAUTION: It is assumed that dynamic reordering will not occur while
  there are open generators.  It is the user's responsibility to make sure
  that dynamic reordering does not occur.  As long as new nodes are not created
  during generation, and dynamic reordering is not called explicitly,
  dynamic reordering will not occur.  Alternatively, it is sufficient to
  disable dynamic reordering.  It is a mistake to dispose of a diagram
  on which generation is ongoing.

  @sideeffect none

  @see Cudd_zddFirstPath Cudd_zddNextPath Cudd_GenFree
  Cudd_IsGenEmpty Cudd_AutodynDisable

*/
#define Cudd_zddForeachPath(manager, f, gen, path)\
    for((gen) = Cudd_zddFirstPath(manager, f, &path);\
	Cudd_IsGenEmpty(gen) ? Cudd_GenFree(gen) : CUDD_TRUE;\
	(void) Cudd_zddNextPath(gen, &path))



/*---------------------------------------------------------------------------*/
/* Function prototypes                                                       */
/*---------------------------------------------------------------------------*/

#ifdef __cplusplus
extern "C" {
#endif

extern DdNode * Cudd_addNewVar(DdManager *dd);
extern DdNode * Cudd_addNewVarAtLevel(DdManager *dd, int level);
extern DdNode * Cudd_bddNewVar(DdManager *dd);
extern DdNode * Cudd_bddNewVarAtLevel(DdManager *dd, int level);
extern int Cudd_bddIsVar(DdManager * dd, DdNode * f);
extern DdNode * Cudd_addIthVar(DdManager *dd, int i);
extern DdNode * Cudd_bddIthVar(DdManager *dd, int i);
extern DdNode * Cudd_zddIthVar(DdManager *dd, int i);
extern int Cudd_zddVarsFromBddVars(DdManager *dd, int multiplicity);
extern unsigned int Cudd_ReadMaxIndex(void);
extern DdNode * Cudd_addConst(DdManager *dd, CUDD_VALUE_TYPE c);
extern int Cudd_IsConstant(DdNode *node);
extern int Cudd_IsNonConstant(DdNode *f);
extern DdNode * Cudd_T(DdNode *node);
extern DdNode * Cudd_E(DdNode *node);
extern CUDD_VALUE_TYPE Cudd_V(DdNode *node);
extern unsigned long Cudd_ReadStartTime(DdManager *unique);
extern unsigned long Cudd_ReadElapsedTime(DdManager *unique);
extern void Cudd_SetStartTime(DdManager *unique, unsigned long st);
extern void Cudd_ResetStartTime(DdManager *unique);
extern unsigned long Cudd_ReadTimeLimit(DdManager *unique);
extern unsigned long Cudd_SetTimeLimit(DdManager *unique, unsigned long tl);
extern void Cudd_UpdateTimeLimit(DdManager * unique);
extern void Cudd_IncreaseTimeLimit(DdManager * unique, unsigned long increase);
extern void Cudd_UnsetTimeLimit(DdManager *unique);
extern int Cudd_TimeLimited(DdManager *unique);
extern void Cudd_RegisterTerminationCallback(DdManager *unique, DD_THFP callback, void * callback_arg);
extern void Cudd_UnregisterTerminationCallback(DdManager *unique);
extern DD_OOMFP Cudd_RegisterOutOfMemoryCallback(DdManager *unique, DD_OOMFP callback);
extern void Cudd_UnregisterOutOfMemoryCallback(DdManager *unique);
extern void Cudd_RegisterTimeoutHandler(DdManager *unique, DD_TOHFP handler, void *arg);
extern DD_TOHFP Cudd_ReadTimeoutHandler(DdManager *unique, void **argp);
extern void Cudd_AutodynEnable(DdManager *unique, Cudd_ReorderingType method);
extern void Cudd_AutodynDisable(DdManager *unique);
extern int Cudd_ReorderingStatus(DdManager *unique, Cudd_ReorderingType *method);
extern void Cudd_AutodynEnableZdd(DdManager *unique, Cudd_ReorderingType method);
extern void Cudd_AutodynDisableZdd(DdManager *unique);
extern int Cudd_ReorderingStatusZdd(DdManager *unique, Cudd_ReorderingType *method);
extern int Cudd_zddRealignmentEnabled(DdManager *unique);
extern void Cudd_zddRealignEnable(DdManager *unique);
extern void Cudd_zddRealignDisable(DdManager *unique);
extern int Cudd_bddRealignmentEnabled(DdManager *unique);
extern void Cudd_bddRealignEnable(DdManager *unique);
extern void Cudd_bddRealignDisable(DdManager *unique);
extern DdNode * Cudd_ReadOne(DdManager *dd);
extern DdNode * Cudd_ReadZddOne(DdManager *dd, int i);
extern DdNode * Cudd_ReadZero(DdManager *dd);
extern DdNode * Cudd_ReadLogicZero(DdManager *dd);
extern DdNode * Cudd_ReadPlusInfinity(DdManager *dd);
extern DdNode * Cudd_ReadMinusInfinity(DdManager *dd);
extern DdNode * Cudd_ReadBackground(DdManager *dd);
extern void Cudd_SetBackground(DdManager *dd, DdNode *bck);
extern unsigned int Cudd_ReadCacheSlots(DdManager *dd);
extern double Cudd_ReadCacheUsedSlots(DdManager * dd);
extern double Cudd_ReadCacheLookUps(DdManager *dd);
extern double Cudd_ReadCacheHits(DdManager *dd);
extern double Cudd_ReadRecursiveCalls(DdManager * dd);
extern unsigned int Cudd_ReadMinHit(DdManager *dd);
extern void Cudd_SetMinHit(DdManager *dd, unsigned int hr);
extern unsigned int Cudd_ReadLooseUpTo(DdManager *dd);
extern void Cudd_SetLooseUpTo(DdManager *dd, unsigned int lut);
extern unsigned int Cudd_ReadMaxCache(DdManager *dd);
extern unsigned int Cudd_ReadMaxCacheHard(DdManager *dd);
extern void Cudd_SetMaxCacheHard(DdManager *dd, unsigned int mc);
extern int Cudd_ReadSize(DdManager *dd);
extern int Cudd_ReadZddSize(DdManager *dd);
extern unsigned int Cudd_ReadSlots(DdManager *dd);
extern double Cudd_ReadUsedSlots(DdManager * dd);
extern double Cudd_ExpectedUsedSlots(DdManager * dd);
extern unsigned int Cudd_ReadKeys(DdManager *dd);
extern unsigned int Cudd_ReadDead(DdManager *dd);
extern unsigned int Cudd_ReadMinDead(DdManager *dd);
extern unsigned int Cudd_ReadReorderings(DdManager *dd);
extern unsigned int Cudd_ReadMaxReorderings(DdManager *dd);
extern void Cudd_SetMaxReorderings(DdManager *dd, unsigned int mr);
extern long Cudd_ReadReorderingTime(DdManager * dd);
extern int Cudd_ReadGarbageCollections(DdManager * dd);
extern long Cudd_ReadGarbageCollectionTime(DdManager * dd);
extern double Cudd_ReadNodesFreed(DdManager * dd);
extern double Cudd_ReadNodesDropped(DdManager * dd);
extern double Cudd_ReadUniqueLookUps(DdManager * dd);
extern double Cudd_ReadUniqueLinks(DdManager * dd);
extern int Cudd_ReadSiftMaxVar(DdManager *dd);
extern void Cudd_SetSiftMaxVar(DdManager *dd, int smv);
extern int Cudd_ReadSiftMaxSwap(DdManager *dd);
extern void Cudd_SetSiftMaxSwap(DdManager *dd, int sms);
extern double Cudd_ReadMaxGrowth(DdManager *dd);
extern void Cudd_SetMaxGrowth(DdManager *dd, double mg);
extern double Cudd_ReadMaxGrowthAlternate(DdManager * dd);
extern void Cudd_SetMaxGrowthAlternate(DdManager * dd, double mg);
extern int Cudd_ReadReorderingCycle(DdManager * dd);
extern void Cudd_SetReorderingCycle(DdManager * dd, int cycle);
extern unsigned int Cudd_NodeReadIndex(DdNode *node);
extern int Cudd_ReadPerm(DdManager *dd, int i);
extern int Cudd_ReadPermZdd(DdManager *dd, int i);
extern int Cudd_ReadInvPerm(DdManager *dd, int i);
extern int Cudd_ReadInvPermZdd(DdManager *dd, int i);
extern DdNode * Cudd_ReadVars(DdManager *dd, int i);
extern CUDD_VALUE_TYPE Cudd_ReadEpsilon(DdManager *dd);
extern void Cudd_SetEpsilon(DdManager *dd, CUDD_VALUE_TYPE ep);
extern Cudd_AggregationType Cudd_ReadGroupcheck(DdManager *dd);
extern void Cudd_SetGroupcheck(DdManager *dd, Cudd_AggregationType gc);
extern int Cudd_GarbageCollectionEnabled(DdManager *dd);
extern void Cudd_EnableGarbageCollection(DdManager *dd);
extern void Cudd_DisableGarbageCollection(DdManager *dd);
extern int Cudd_DeadAreCounted(DdManager *dd);
extern void Cudd_TurnOnCountDead(DdManager *dd);
extern void Cudd_TurnOffCountDead(DdManager *dd);
extern int Cudd_ReadRecomb(DdManager *dd);
extern void Cudd_SetRecomb(DdManager *dd, int recomb);
extern int Cudd_ReadSymmviolation(DdManager *dd);
extern void Cudd_SetSymmviolation(DdManager *dd, int symmviolation);
extern int Cudd_ReadArcviolation(DdManager *dd);
extern void Cudd_SetArcviolation(DdManager *dd, int arcviolation);
extern int Cudd_ReadPopulationSize(DdManager *dd);
extern void Cudd_SetPopulationSize(DdManager *dd, int populationSize);
extern int Cudd_ReadNumberXovers(DdManager *dd);
extern void Cudd_SetNumberXovers(DdManager *dd, int numberXovers);
extern unsigned int Cudd_ReadOrderRandomization(DdManager * dd);
extern void Cudd_SetOrderRandomization(DdManager * dd, unsigned int factor);
extern size_t Cudd_ReadMemoryInUse(DdManager *dd);
extern int Cudd_PrintInfo(DdManager *dd, FILE *fp);
extern long Cudd_ReadPeakNodeCount(DdManager *dd);
extern int Cudd_ReadPeakLiveNodeCount(DdManager * dd);
extern long Cudd_ReadNodeCount(DdManager *dd);
extern long Cudd_zddReadNodeCount(DdManager *dd);
extern int Cudd_AddHook(DdManager *dd, DD_HFP f, Cudd_HookType where);
extern int Cudd_RemoveHook(DdManager *dd, DD_HFP f, Cudd_HookType where);
extern int Cudd_IsInHook(DdManager * dd, DD_HFP f, Cudd_HookType where);
extern int Cudd_StdPreReordHook(DdManager *dd, const char *str, void *data);
extern int Cudd_StdPostReordHook(DdManager *dd, const char *str, void *data);
extern int Cudd_EnableReorderingReporting(DdManager *dd);
extern int Cudd_DisableReorderingReporting(DdManager *dd);
extern int Cudd_ReorderingReporting(DdManager *dd);
extern int Cudd_PrintGroupedOrder(DdManager * dd, const char *str, void *data);
extern int Cudd_EnableOrderingMonitoring(DdManager *dd);
extern int Cudd_DisableOrderingMonitoring(DdManager *dd);
extern int Cudd_OrderingMonitoring(DdManager *dd);
extern void Cudd_SetApplicationHook(DdManager *dd, void * value);
extern void * Cudd_ReadApplicationHook(DdManager *dd);
extern Cudd_ErrorType Cudd_ReadErrorCode(DdManager *dd);
extern void Cudd_ClearErrorCode(DdManager *dd);
extern DD_OOMFP Cudd_InstallOutOfMemoryHandler(DD_OOMFP newHandler);
extern FILE * Cudd_ReadStdout(DdManager *dd);
extern void Cudd_SetStdout(DdManager *dd, FILE *fp);
extern FILE * Cudd_ReadStderr(DdManager *dd);
extern void Cudd_SetStderr(DdManager *dd, FILE *fp);
extern unsigned int Cudd_ReadNextReordering(DdManager *dd);
extern void Cudd_SetNextReordering(DdManager *dd, unsigned int next);
extern double Cudd_ReadSwapSteps(DdManager *dd);
extern unsigned int Cudd_ReadMaxLive(DdManager *dd);
extern void Cudd_SetMaxLive(DdManager *dd, unsigned int maxLive);
extern size_t Cudd_ReadMaxMemory(DdManager *dd);
extern size_t Cudd_SetMaxMemory(DdManager *dd, size_t maxMemory);
extern int Cudd_bddBindVar(DdManager *dd, int index);
extern int Cudd_bddUnbindVar(DdManager *dd, int index);
extern int Cudd_bddVarIsBound(DdManager *dd, int index);
extern DdNode * Cudd_addExistAbstract(DdManager *manager, DdNode *f, DdNode *cube);
extern DdNode * Cudd_addUnivAbstract(DdManager *manager, DdNode *f, DdNode *cube);
extern DdNode * Cudd_addOrAbstract(DdManager *manager, DdNode *f, DdNode *cube);
extern DdNode * Cudd_addMinAbstract(DdManager *manager, DdNode *f, DdNode *cube);
extern DdNode * Cudd_addMaxAbstract(DdManager *manager, DdNode *f, DdNode *cube);
extern DdNode * Cudd_addFirstFilter(DdManager *manager, DdNode *f, DdNode *cube);
extern DdNode * Cudd_addApply(DdManager *dd, DD_AOP op, DdNode *f, DdNode *g);
extern DdNode * Cudd_addPlus(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addTimes(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addThreshold(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addSetNZ(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addDivide(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addMinus(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addMinimum(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addMaximum(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addOneZeroMaximum(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addDiff(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addAgreement(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addOr(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addNand(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addNor(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addXor(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addXnor(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addEquals(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addNotEquals(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addGreaterThan(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addGreaterThanEquals(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addLessThan(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addLessThanEquals(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addPow(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addMod(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addLogXY(DdManager *dd, DdNode **f, DdNode **g);
extern DdNode * Cudd_addMonadicApply(DdManager * dd, DD_MAOP op, DdNode * f);
extern DdNode * Cudd_addLog(DdManager * dd, DdNode * f);
extern DdNode * Cudd_addFloor(DdManager * dd, DdNode * f);
extern DdNode * Cudd_addCeil(DdManager * dd, DdNode * f);
extern DdNode * Cudd_addFindMax(DdManager *dd, DdNode *f);
extern DdNode * Cudd_addFindMin(DdManager *dd, DdNode *f);
extern DdNode * Cudd_addIthBit(DdManager *dd, DdNode *f, int bit);
extern DdNode * Cudd_addScalarInverse(DdManager *dd, DdNode *f, DdNode *epsilon);
extern DdNode * Cudd_addIte(DdManager *dd, DdNode *f, DdNode *g, DdNode *h);
extern DdNode * Cudd_addIteConstant(DdManager *dd, DdNode *f, DdNode *g, DdNode *h);
extern DdNode * Cudd_addEvalConst(DdManager *dd, DdNode *f, DdNode *g);
extern int Cudd_addLeq(DdManager * dd, DdNode * f, DdNode * g);
extern DdNode * Cudd_addCmpl(DdManager *dd, DdNode *f);
extern DdNode * Cudd_addNegate(DdManager *dd, DdNode *f);
extern DdNode * Cudd_addRoundOff(DdManager *dd, DdNode *f, int N);
extern DdNode * Cudd_addWalsh(DdManager *dd, DdNode **x, DdNode **y, int n);
extern DdNode * Cudd_addResidue(DdManager *dd, int n, int m, int options, int top);
extern DdNode * Cudd_bddAndAbstract(DdManager *manager, DdNode *f, DdNode *g, DdNode *cube);
extern DdNode * Cudd_bddAndAbstractLimit(DdManager *manager, DdNode *f, DdNode *g, DdNode *cube, unsigned int limit);
extern int Cudd_ApaNumberOfDigits(int binaryDigits);
extern DdApaNumber Cudd_NewApaNumber(int digits);
extern void Cudd_FreeApaNumber(DdApaNumber number);
extern void Cudd_ApaCopy(int digits, DdConstApaNumber source, DdApaNumber dest);
extern DdApaDigit Cudd_ApaAdd(int digits, DdConstApaNumber a, DdConstApaNumber b, DdApaNumber sum);
extern DdApaDigit Cudd_ApaSubtract(int digits, DdConstApaNumber a, DdConstApaNumber b, DdApaNumber diff);
extern DdApaDigit Cudd_ApaShortDivision(int digits, DdConstApaNumber dividend, DdApaDigit divisor, DdApaNumber quotient);
extern unsigned int Cudd_ApaIntDivision(int  digits, DdConstApaNumber dividend, unsigned int  divisor, DdApaNumber  quotient);
extern void Cudd_ApaShiftRight(int digits, DdApaDigit in, DdConstApaNumber a, DdApaNumber b);
extern void Cudd_ApaSetToLiteral(int digits, DdApaNumber number, DdApaDigit literal);
extern void Cudd_ApaPowerOfTwo(int digits, DdApaNumber number, int power);
extern int Cudd_ApaCompare(int digitsFirst, DdConstApaNumber first, int digitsSecond, DdConstApaNumber second);
extern int Cudd_ApaCompareRatios(int digitsFirst, DdConstApaNumber firstNum, unsigned int firstDen, int digitsSecond, DdConstApaNumber secondNum, unsigned int secondDen);
extern int Cudd_ApaPrintHex(FILE *fp, int digits, DdConstApaNumber number);
extern int Cudd_ApaPrintDecimal(FILE *fp, int digits, DdConstApaNumber number);
extern char * Cudd_ApaStringDecimal(int digits, DdConstApaNumber number);
extern int Cudd_ApaPrintExponential(FILE * fp, int  digits, DdConstApaNumber number, int precision);
extern DdApaNumber Cudd_ApaCountMinterm(DdManager const *manager, DdNode *node, int nvars, int *digits);
extern int Cudd_ApaPrintMinterm(FILE *fp, DdManager const *dd, DdNode *node, int nvars);
extern int Cudd_ApaPrintMintermExp(FILE * fp, DdManager const * dd, DdNode *node, int  nvars, int precision);
extern int Cudd_ApaPrintDensity(FILE * fp, DdManager * dd, DdNode * node, int  nvars);
extern DdNode * Cudd_UnderApprox(DdManager *dd, DdNode *f, int numVars, int threshold, int safe, double quality);
extern DdNode * Cudd_OverApprox(DdManager *dd, DdNode *f, int numVars, int threshold, int safe, double quality);
extern DdNode * Cudd_RemapUnderApprox(DdManager *dd, DdNode *f, int numVars, int threshold, double quality);
extern DdNode * Cudd_RemapOverApprox(DdManager *dd, DdNode *f, int numVars, int threshold, double quality);
extern DdNode * Cudd_BiasedUnderApprox(DdManager *dd, DdNode *f, DdNode *b, int numVars, int threshold, double quality1, double quality0);
extern DdNode * Cudd_BiasedOverApprox(DdManager *dd, DdNode *f, DdNode *b, int numVars, int threshold, double quality1, double quality0);
extern DdNode * Cudd_bddExistAbstract(DdManager *manager, DdNode *f, DdNode *cube);
extern DdNode * Cudd_bddExistAbstractLimit(DdManager * manager, DdNode * f, DdNode * cube, unsigned int limit);
extern DdNode * Cudd_bddXorExistAbstract(DdManager *manager, DdNode *f, DdNode *g, DdNode *cube);
extern DdNode * Cudd_bddUnivAbstract(DdManager *manager, DdNode *f, DdNode *cube);
extern DdNode * Cudd_bddBooleanDiff(DdManager *manager, DdNode *f, int x);
extern int Cudd_bddVarIsDependent(DdManager *dd, DdNode *f, DdNode *var);
extern double Cudd_bddCorrelation(DdManager *manager, DdNode *f, DdNode *g);
extern double Cudd_bddCorrelationWeights(DdManager *manager, DdNode *f, DdNode *g, double *prob);
extern DdNode * Cudd_bddIte(DdManager *dd, DdNode *f, DdNode *g, DdNode *h);
extern DdNode * Cudd_bddIteLimit(DdManager *dd, DdNode *f, DdNode *g, DdNode *h, unsigned int limit);
extern DdNode * Cudd_bddIteConstant(DdManager *dd, DdNode *f, DdNode *g, DdNode *h);
extern DdNode * Cudd_bddIntersect(DdManager *dd, DdNode *f, DdNode *g);
extern DdNode * Cudd_bddAnd(DdManager *dd, DdNode *f, DdNode *g);
extern DdNode * Cudd_bddAndLimit(DdManager *dd, DdNode *f, DdNode *g, unsigned int limit);
extern DdNode * Cudd_bddOr(DdManager *dd, DdNode *f, DdNode *g);
extern DdNode * Cudd_bddOrLimit(DdManager *dd, DdNode *f, DdNode *g, unsigned int limit);
extern DdNode * Cudd_bddNand(DdManager *dd, DdNode *f, DdNode *g);
extern DdNode * Cudd_bddNor(DdManager *dd, DdNode *f, DdNode *g);
extern DdNode * Cudd_bddXor(DdManager *dd, DdNode *f, DdNode *g);
extern DdNode * Cudd_bddXnor(DdManager *dd, DdNode *f, DdNode *g);
extern DdNode * Cudd_bddXnorLimit(DdManager *dd, DdNode *f, DdNode *g, unsigned int limit);
extern int Cudd_bddLeq(DdManager *dd, DdNode *f, DdNode *g);
extern DdNode * Cudd_addBddThreshold(DdManager *dd, DdNode *f, CUDD_VALUE_TYPE value);
extern DdNode * Cudd_addBddStrictThreshold(DdManager *dd, DdNode *f, CUDD_VALUE_TYPE value);
extern DdNode * Cudd_addBddInterval(DdManager *dd, DdNode *f, CUDD_VALUE_TYPE lower, CUDD_VALUE_TYPE upper);
extern DdNode * Cudd_addBddIthBit(DdManager *dd, DdNode *f, int bit);
extern DdNode * Cudd_BddToAdd(DdManager *dd, DdNode *B);
extern DdNode * Cudd_addBddPattern(DdManager *dd, DdNode *f);
extern DdNode * Cudd_bddTransfer(DdManager *ddSource, DdManager *ddDestination, DdNode *f);
extern int Cudd_DebugCheck(DdManager *table);
extern int Cudd_CheckKeys(DdManager *table);
extern DdNode * Cudd_bddClippingAnd(DdManager *dd, DdNode *f, DdNode *g, int maxDepth, int direction);
extern DdNode * Cudd_bddClippingAndAbstract(DdManager *dd, DdNode *f, DdNode *g, DdNode *cube, int maxDepth, int direction);
extern DdNode * Cudd_Cofactor(DdManager *dd, DdNode *f, DdNode *g);
extern int Cudd_CheckCube(DdManager *dd, DdNode *g);
extern int Cudd_VarsAreSymmetric(DdManager * dd, DdNode * f, int index1, int index2);
extern DdNode * Cudd_bddCompose(DdManager *dd, DdNode *f, DdNode *g, int v);
extern DdNode * Cudd_addCompose(DdManager *dd, DdNode *f, DdNode *g, int v);
extern DdNode * Cudd_addPermute(DdManager *manager, DdNode *node, int *permut);
extern DdNode * Cudd_addSwapVariables(DdManager *dd, DdNode *f, DdNode **x, DdNode **y, int n);
extern DdNode * Cudd_bddPermute(DdManager *manager, DdNode *node, int *permut);
extern DdNode * Cudd_bddVarMap(DdManager *manager, DdNode *f);
extern int Cudd_SetVarMap(DdManager *manager, DdNode **x, DdNode **y, int n);
extern DdNode * Cudd_bddSwapVariables(DdManager *dd, DdNode *f, DdNode **x, DdNode **y, int n);
extern DdNode * Cudd_bddAdjPermuteX(DdManager *dd, DdNode *B, DdNode **x, int n);
extern DdNode * Cudd_addVectorCompose(DdManager *dd, DdNode *f, DdNode **vector);
extern DdNode * Cudd_addGeneralVectorCompose(DdManager *dd, DdNode *f, DdNode **vectorOn, DdNode **vectorOff);
extern DdNode * Cudd_addNonSimCompose(DdManager *dd, DdNode *f, DdNode **vector);
extern DdNode * Cudd_bddVectorCompose(DdManager *dd, DdNode *f, DdNode **vector);
extern int Cudd_bddApproxConjDecomp(DdManager *dd, DdNode *f, DdNode ***conjuncts);
extern int Cudd_bddApproxDisjDecomp(DdManager *dd, DdNode *f, DdNode ***disjuncts);
extern int Cudd_bddIterConjDecomp(DdManager *dd, DdNode *f, DdNode ***conjuncts);
extern int Cudd_bddIterDisjDecomp(DdManager *dd, DdNode *f, DdNode ***disjuncts);
extern int Cudd_bddGenConjDecomp(DdManager *dd, DdNode *f, DdNode ***conjuncts);
extern int Cudd_bddGenDisjDecomp(DdManager *dd, DdNode *f, DdNode ***disjuncts);
extern int Cudd_bddVarConjDecomp(DdManager *dd, DdNode * f, DdNode ***conjuncts);
extern int Cudd_bddVarDisjDecomp(DdManager *dd, DdNode * f, DdNode ***disjuncts);
extern DdNode * Cudd_FindEssential(DdManager *dd, DdNode *f);
extern int Cudd_bddIsVarEssential(DdManager *manager, DdNode *f, int id, int phase);
extern DdTlcInfo * Cudd_FindTwoLiteralClauses(DdManager * dd, DdNode * f);
extern int Cudd_PrintTwoLiteralClauses(DdManager * dd, DdNode * f, char **names, FILE *fp);
extern int Cudd_ReadIthClause(DdTlcInfo * tlc, int i, unsigned *var1, unsigned *var2, int *phase1, int *phase2);
extern void Cudd_tlcInfoFree(DdTlcInfo * t);
extern int Cudd_DumpBlif(DdManager *dd, int n, DdNode **f, char const * const *inames, char const * const *onames, char *mname, FILE *fp, int mv);
extern int Cudd_DumpBlifBody(DdManager *dd, int n, DdNode **f, char const * const *inames, char const * const *onames, FILE *fp, int mv);
extern int Cudd_DumpDot(DdManager *dd, int n, DdNode **f, char const * const *inames, char const * const *onames, FILE *fp);
extern int Cudd_DumpDaVinci(DdManager *dd, int n, DdNode **f, char const * const *inames, char const * const *onames, FILE *fp);
extern int Cudd_DumpDDcal(DdManager *dd, int n, DdNode **f, char const * const *inames, char const * const *onames, FILE *fp);
extern int Cudd_DumpFactoredForm(DdManager *dd, int n, DdNode **f, char const * const *inames, char const * const *onames, FILE *fp);
extern char * Cudd_FactoredFormString(DdManager *dd, DdNode *f, char const * const * inames);
extern DdNode * Cudd_bddConstrain(DdManager *dd, DdNode *f, DdNode *c);
extern DdNode * Cudd_bddRestrict(DdManager *dd, DdNode *f, DdNode *c);
extern DdNode * Cudd_bddNPAnd(DdManager *dd, DdNode *f, DdNode *c);
extern DdNode * Cudd_addConstrain(DdManager *dd, DdNode *f, DdNode *c);
extern DdNode ** Cudd_bddConstrainDecomp(DdManager *dd, DdNode *f);
extern DdNode * Cudd_addRestrict(DdManager *dd, DdNode *f, DdNode *c);
extern DdNode ** Cudd_bddCharToVect(DdManager *dd, DdNode *f);
extern DdNode * Cudd_bddLICompaction(DdManager *dd, DdNode *f, DdNode *c);
extern DdNode * Cudd_bddSqueeze(DdManager *dd, DdNode *l, DdNode *u);
extern DdNode * Cudd_bddInterpolate(DdManager * dd, DdNode * l, DdNode * u);
extern DdNode * Cudd_bddMinimize(DdManager *dd, DdNode *f, DdNode *c);
extern DdNode * Cudd_SubsetCompress(DdManager *dd, DdNode *f, int nvars, int threshold);
extern DdNode * Cudd_SupersetCompress(DdManager *dd, DdNode *f, int nvars, int threshold);
extern int Cudd_addHarwell(FILE *fp, DdManager *dd, DdNode **E, DdNode ***x, DdNode ***y, DdNode ***xn, DdNode ***yn_, int *nx, int *ny, int *m, int *n, int bx, int sx, int by, int sy, int pr);
extern DdManager * Cudd_Init(unsigned int numVars, unsigned int numVarsZ, unsigned int numSlots, unsigned int cacheSize, size_t maxMemory);
extern void Cudd_Quit(DdManager *unique);
extern int Cudd_PrintLinear(DdManager *table);
extern int Cudd_ReadLinear(DdManager *table, int x, int y);
extern DdNode * Cudd_bddLiteralSetIntersection(DdManager *dd, DdNode *f, DdNode *g);
extern DdNode * Cudd_addMatrixMultiply(DdManager *dd, DdNode *A, DdNode *B, DdNode **z, int nz);
extern DdNode * Cudd_addTimesPlus(DdManager *dd, DdNode *A, DdNode *B, DdNode **z, int nz);
extern DdNode * Cudd_addTriangle(DdManager *dd, DdNode *f, DdNode *g, DdNode **z, int nz);
extern DdNode * Cudd_addOuterSum(DdManager *dd, DdNode *M, DdNode *r, DdNode *c);
extern DdNode * Cudd_PrioritySelect(DdManager *dd, DdNode *R, DdNode **x, DdNode **y, DdNode **z, DdNode *Pi, int n, DD_PRFP PiFunc);
extern DdNode * Cudd_Xgty(DdManager *dd, int N, DdNode **z, DdNode **x, DdNode **y);
extern DdNode * Cudd_Xeqy(DdManager *dd, int N, DdNode **x, DdNode **y);
extern DdNode * Cudd_addXeqy(DdManager *dd, int N, DdNode **x, DdNode **y);
extern DdNode * Cudd_Dxygtdxz(DdManager *dd, int N, DdNode **x, DdNode **y, DdNode **z);
extern DdNode * Cudd_Dxygtdyz(DdManager *dd, int N, DdNode **x, DdNode **y, DdNode **z);
extern DdNode * Cudd_Inequality(DdManager * dd, int  N, int c, DdNode ** x, DdNode ** y);
extern DdNode * Cudd_Disequality(DdManager * dd, int  N, int c, DdNode ** x, DdNode ** y);
extern DdNode * Cudd_bddInterval(DdManager * dd, int  N, DdNode ** x, unsigned int lowerB, unsigned int upperB);
extern DdNode * Cudd_CProjection(DdManager *dd, DdNode *R, DdNode *Y);
extern DdNode * Cudd_addHamming(DdManager *dd, DdNode **xVars, DdNode **yVars, int nVars);
extern int Cudd_MinHammingDist(DdManager *dd, DdNode *f, int *minterm, int upperBound);
extern DdNode * Cudd_bddClosestCube(DdManager *dd, DdNode * f, DdNode *g, int *distance);
extern int Cudd_addRead(FILE *fp, DdManager *dd, DdNode **E, DdNode ***x, DdNode ***y, DdNode ***xn, DdNode ***yn_, int *nx, int *ny, int *m, int *n, int bx, int sx, int by, int sy);
extern int Cudd_bddRead(FILE *fp, DdManager *dd, DdNode **E, DdNode ***x, DdNode ***y, int *nx, int *ny, int *m, int *n, int bx, int sx, int by, int sy);
extern void Cudd_Ref(DdNode *n);
extern void Cudd_RecursiveDeref(DdManager *table, DdNode *n);
extern void Cudd_IterDerefBdd(DdManager *table, DdNode *n);
extern void Cudd_DelayedDerefBdd(DdManager * table, DdNode * n);
extern void Cudd_RecursiveDerefZdd(DdManager *table, DdNode *n);
extern void Cudd_Deref(DdNode *node);
extern int Cudd_CheckZeroRef(DdManager *manager);
extern int Cudd_ReduceHeap(DdManager *table, Cudd_ReorderingType heuristic, int minsize);
extern int Cudd_ShuffleHeap(DdManager *table, int *permutation);
extern DdNode * Cudd_Eval(DdManager *dd, DdNode *f, int *inputs);
extern DdNode * Cudd_ShortestPath(DdManager *manager, DdNode *f, int *weight, int *support, int *length);
extern DdNode * Cudd_LargestCube(DdManager *manager, DdNode *f, int *length);
extern int Cudd_ShortestLength(DdManager *manager, DdNode *f, int *weight);
extern DdNode * Cudd_Decreasing(DdManager *dd, DdNode *f, int i);
extern DdNode * Cudd_Increasing(DdManager *dd, DdNode *f, int i);
extern int Cudd_EquivDC(DdManager *dd, DdNode *F, DdNode *G, DdNode *D);
extern int Cudd_bddLeqUnless(DdManager *dd, DdNode *f, DdNode *g, DdNode *D);
extern int Cudd_EqualSupNorm(DdManager *dd, DdNode *f, DdNode *g, CUDD_VALUE_TYPE tolerance, int pr);
extern int Cudd_EqualSupNormRel(DdManager *dd, DdNode *f, DdNode *g, CUDD_VALUE_TYPE tolerance, int pr);
extern DdNode * Cudd_bddMakePrime(DdManager *dd, DdNode *cube, DdNode *f);
extern DdNode * Cudd_bddMaximallyExpand(DdManager *dd, DdNode *lb, DdNode *ub, DdNode *f);
extern DdNode * Cudd_bddLargestPrimeUnate(DdManager *dd , DdNode *f, DdNode *phaseBdd);
extern double * Cudd_CofMinterm(DdManager *dd, DdNode *node);
extern DdNode * Cudd_SolveEqn(DdManager * bdd, DdNode *F, DdNode *Y, DdNode **G, int **yIndex, int n);
extern DdNode * Cudd_VerifySol(DdManager * bdd, DdNode *F, DdNode **G, int *yIndex, int n);
extern DdNode * Cudd_SplitSet(DdManager *manager, DdNode *S, DdNode **xVars, int n, double m);
extern DdNode * Cudd_SubsetHeavyBranch(DdManager *dd, DdNode *f, int numVars, int threshold);
extern DdNode * Cudd_SupersetHeavyBranch(DdManager *dd, DdNode *f, int numVars, int threshold);
extern DdNode * Cudd_SubsetShortPaths(DdManager *dd, DdNode *f, int numVars, int threshold, int hardlimit);
extern DdNode * Cudd_SupersetShortPaths(DdManager *dd, DdNode *f, int numVars, int threshold, int hardlimit);
extern void Cudd_SymmProfile(DdManager *table, int lower, int upper);
extern unsigned int Cudd_Prime(unsigned int p);
extern int Cudd_Reserve(DdManager *manager, int amount);
extern int Cudd_PrintMinterm(DdManager *manager, DdNode *node);
extern int Cudd_bddPrintCover(DdManager *dd, DdNode *l, DdNode *u);
extern int Cudd_PrintDebug(DdManager *dd, DdNode *f, int n, int pr);
extern int Cudd_PrintSummary(DdManager * dd, DdNode * f, int n, int mode);
extern int Cudd_DagSize(DdNode *node);
extern int Cudd_EstimateCofactor(DdManager *dd, DdNode * node, int i, int phase);
extern int Cudd_EstimateCofactorSimple(DdNode * node, int i);
extern int Cudd_SharingSize(DdNode **nodeArray, int n);
extern double Cudd_CountMinterm(DdManager *manager, DdNode *node, int nvars);
#ifdef EPD_H_
extern int Cudd_EpdCountMinterm(DdManager const *manager, DdNode *node, int nvars, EpDouble *epd);
#endif
extern long double Cudd_LdblCountMinterm(DdManager const *manager, DdNode *node, int nvars);
extern int Cudd_EpdPrintMinterm(DdManager const * dd, DdNode * node, int nvars);
extern double Cudd_CountPath(DdNode *node);
extern double Cudd_CountPathsToNonZero(DdNode *node);
extern int Cudd_SupportIndices(DdManager * dd, DdNode * f, int **indices);
extern DdNode * Cudd_Support(DdManager *dd, DdNode *f);
extern int * Cudd_SupportIndex(DdManager *dd, DdNode *f);
extern int Cudd_SupportSize(DdManager *dd, DdNode *f);
extern int Cudd_VectorSupportIndices(DdManager * dd, DdNode ** F, int n, int **indices);
extern DdNode * Cudd_VectorSupport(DdManager *dd, DdNode **F, int n);
extern int * Cudd_VectorSupportIndex(DdManager *dd, DdNode **F, int n);
extern int Cudd_VectorSupportSize(DdManager *dd, DdNode **F, int n);
extern int Cudd_ClassifySupport(DdManager *dd, DdNode *f, DdNode *g, DdNode **common, DdNode **onlyF, DdNode **onlyG);
extern int Cudd_CountLeaves(DdNode *node);
extern int Cudd_bddPickOneCube(DdManager *ddm, DdNode *node, char *string);
extern DdNode * Cudd_bddPickOneMinterm(DdManager *dd, DdNode *f, DdNode **vars, int n);
extern DdNode ** Cudd_bddPickArbitraryMinterms(DdManager *dd, DdNode *f, DdNode **vars, int n, int k);
extern DdNode * Cudd_SubsetWithMaskVars(DdManager *dd, DdNode *f, DdNode **vars, int nvars, DdNode **maskVars, int mvars);
extern DdGen * Cudd_FirstCube(DdManager *dd, DdNode *f, int **cube, CUDD_VALUE_TYPE *value);
extern int Cudd_NextCube(DdGen *gen, int **cube, CUDD_VALUE_TYPE *value);
extern DdGen * Cudd_FirstPrime(DdManager *dd, DdNode *l, DdNode *u, int **cube);
extern int Cudd_NextPrime(DdGen *gen, int **cube);
extern DdNode * Cudd_bddComputeCube(DdManager *dd, DdNode **vars, int *phase, int n);
extern DdNode * Cudd_addComputeCube(DdManager *dd, DdNode **vars, int *phase, int n);
extern DdNode * Cudd_CubeArrayToBdd(DdManager *dd, int *array);
extern int Cudd_BddToCubeArray(DdManager *dd, DdNode *cube, int *array);
extern DdGen * Cudd_FirstNode(DdManager *dd, DdNode *f, DdNode **node);
extern int Cudd_NextNode(DdGen *gen, DdNode **node);
extern int Cudd_GenFree(DdGen *gen);
extern int Cudd_IsGenEmpty(DdGen *gen);
extern DdNode * Cudd_IndicesToCube(DdManager *dd, int *array, int n);
extern void Cudd_PrintVersion(FILE *fp);
extern double Cudd_AverageDistance(DdManager *dd);
extern int32_t Cudd_Random(DdManager * dd);
extern void Cudd_Srandom(DdManager * dd, int32_t seed);
extern double Cudd_Density(DdManager *dd, DdNode *f, int nvars);
extern void Cudd_OutOfMem(size_t size);
extern void Cudd_OutOfMemSilent(size_t size);
extern int Cudd_zddCount(DdManager *zdd, DdNode *P);
extern double Cudd_zddCountDouble(DdManager *zdd, DdNode *P);
extern DdNode * Cudd_zddProduct(DdManager *dd, DdNode *f, DdNode *g);
extern DdNode * Cudd_zddUnateProduct(DdManager *dd, DdNode *f, DdNode *g);
extern DdNode * Cudd_zddWeakDiv(DdManager *dd, DdNode *f, DdNode *g);
extern DdNode * Cudd_zddDivide(DdManager *dd, DdNode *f, DdNode *g);
extern DdNode * Cudd_zddWeakDivF(DdManager *dd, DdNode *f, DdNode *g);
extern DdNode * Cudd_zddDivideF(DdManager *dd, DdNode *f, DdNode *g);
extern DdNode * Cudd_zddComplement(DdManager *dd, DdNode *node);
extern DdNode * Cudd_zddIsop(DdManager *dd, DdNode *L, DdNode *U, DdNode **zdd_I);
extern DdNode * Cudd_bddIsop(DdManager *dd, DdNode *L, DdNode *U);
extern DdNode * Cudd_MakeBddFromZddCover(DdManager *dd, DdNode *node);
extern int Cudd_zddDagSize(DdNode *p_node);
extern double Cudd_zddCountMinterm(DdManager *zdd, DdNode *node, int path);
extern void Cudd_zddPrintSubtable(DdManager *table);
extern DdNode * Cudd_zddPortFromBdd(DdManager *dd, DdNode *B);
extern DdNode * Cudd_zddPortToBdd(DdManager *dd, DdNode *f);
extern int Cudd_zddReduceHeap(DdManager *table, Cudd_ReorderingType heuristic, int minsize);
extern int Cudd_zddShuffleHeap(DdManager *table, int *permutation);
extern DdNode * Cudd_zddIte(DdManager *dd, DdNode *f, DdNode *g, DdNode *h);
extern DdNode * Cudd_zddUnion(DdManager *dd, DdNode *P, DdNode *Q);
extern DdNode * Cudd_zddIntersect(DdManager *dd, DdNode *P, DdNode *Q);
extern DdNode * Cudd_zddDiff(DdManager *dd, DdNode *P, DdNode *Q);
extern DdNode * Cudd_zddDiffConst(DdManager *zdd, DdNode *P, DdNode *Q);
extern DdNode * Cudd_zddSubset1(DdManager *dd, DdNode *P, int var);
extern DdNode * Cudd_zddSubset0(DdManager *dd, DdNode *P, int var);
extern DdNode * Cudd_zddChange(DdManager *dd, DdNode *P, int var);
extern void Cudd_zddSymmProfile(DdManager *table, int lower, int upper);
extern int Cudd_zddPrintMinterm(DdManager *zdd, DdNode *node);
extern int Cudd_zddPrintCover(DdManager *zdd, DdNode *node);
extern int Cudd_zddPrintDebug(DdManager *zdd, DdNode *f, int n, int pr);
extern DdGen * Cudd_zddFirstPath(DdManager *zdd, DdNode *f, int **path);
extern int Cudd_zddNextPath(DdGen *gen, int **path);
extern char * Cudd_zddCoverPathToString(DdManager *zdd, int *path, char *str);
extern DdNode * Cudd_zddSupport(DdManager * dd, DdNode * f);
extern int Cudd_zddDumpDot(DdManager *dd, int n, DdNode **f, char const * const *inames, char const * const *onames, FILE *fp);
extern int Cudd_bddSetPiVar(DdManager *dd, int index);
extern int Cudd_bddSetPsVar(DdManager *dd, int index);
extern int Cudd_bddSetNsVar(DdManager *dd, int index);
extern int Cudd_bddIsPiVar(DdManager *dd, int index);
extern int Cudd_bddIsPsVar(DdManager *dd, int index);
extern int Cudd_bddIsNsVar(DdManager *dd, int index);
extern int Cudd_bddSetPairIndex(DdManager *dd, int index, int pairIndex);
extern int Cudd_bddReadPairIndex(DdManager *dd, int index);
extern int Cudd_bddSetVarToBeGrouped(DdManager *dd, int index);
extern int Cudd_bddSetVarHardGroup(DdManager *dd, int index);
extern int Cudd_bddResetVarToBeGrouped(DdManager *dd, int index);
extern int Cudd_bddIsVarToBeGrouped(DdManager *dd, int index);
extern int Cudd_bddSetVarToBeUngrouped(DdManager *dd, int index);
extern int Cudd_bddIsVarToBeUngrouped(DdManager *dd, int index);
extern int Cudd_bddIsVarHardGroup(DdManager *dd, int index);
#ifdef MTR_H_
extern MtrNode * Cudd_ReadTree(DdManager *dd);
extern void Cudd_SetTree(DdManager *dd, MtrNode *tree);
extern void Cudd_FreeTree(DdManager *dd);
extern MtrNode * Cudd_ReadZddTree(DdManager *dd);
extern void Cudd_SetZddTree(DdManager *dd, MtrNode *tree);
extern void Cudd_FreeZddTree(DdManager *dd);
extern MtrNode * Cudd_MakeTreeNode(DdManager *dd, unsigned int low, unsigned int size, unsigned int type);
extern MtrNode * Cudd_MakeZddTreeNode(DdManager *dd, unsigned int low, unsigned int size, unsigned int type);
#endif

#ifdef __cplusplus
} /* end of extern "C" */
#endif

#endif /* CUDD_H_ */
