/**
  @file

  @ingroup cplusplus

  @brief Class definitions for C++ object-oriented encapsulation of
  CUDD.

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

#ifndef CUDD_OBJ_H_
#define CUDD_OBJ_H_

/*---------------------------------------------------------------------------*/
/* Nested includes                                                           */
/*---------------------------------------------------------------------------*/

#include <cstdio>
#include <string>
#include <vector>
#include "cudd.h"

/*---------------------------------------------------------------------------*/
/* Type definitions                                                          */
/*---------------------------------------------------------------------------*/
class BDD;
class ADD;
class ZDD;
class Cudd;

typedef void (*PFC)(std::string);	// handler function type

/*---------------------------------------------------------------------------*/
/* Class definitions                                                         */
/*---------------------------------------------------------------------------*/

class Capsule;


/**
  @brief Base class for all decision diagrams in CUDD.

  @see Cudd ABDD ADD BDD ZDD

*/
class DD {
protected:
    Capsule *p;
    DdNode *node;
    inline DdManager * checkSameManager(const DD &other) const;
    inline void checkReturnValue(const void *result) const;
    inline void checkReturnValue(int result, int expected = 1)
	const;
    DD();
    DD(Capsule *cap, DdNode *ddNode);
    DD(Cudd const & manager, DdNode *ddNode);
    DD(const DD &from);
    ~DD();
public:
    // This operator should be declared explicit, but there are still too many
    // compilers out there that do not support this C++11 feature.
    operator bool() const { return node; }
    DdManager * manager() const;
    DdNode * getNode() const;
    DdNode * getRegularNode() const;
    int nodeCount() const;
    unsigned int NodeReadIndex() const;

}; // DD


/**
  @brief Class for ADDs and BDDs.

  @see Cudd ADD BDD

*/
class ABDD : public DD {
    friend class Cudd;
protected:
    ABDD();
    ABDD(Capsule *cap, DdNode *bddNode);
    ABDD(Cudd const & manager, DdNode *ddNode);
    ABDD(const ABDD &from);
    ~ABDD();
public:
    bool operator==(const ABDD &other) const;
    bool operator!=(const ABDD &other) const;
    void print(int nvars, int verbosity = 1) const;
    void summary(int nvars, int mode = 0) const;
    DdApaNumber ApaCountMinterm(int nvars, int * digits) const;
    void ApaPrintMinterm(int nvars, FILE * fp = stdout) const;
    void ApaPrintMintermExp(int nvars, int precision = 6, FILE * fp = stdout) const;
    void EpdPrintMinterm(int nvars, FILE * fp = stdout) const;
    long double LdblCountMinterm(int nvars) const;
    bool IsOne() const;
    bool IsCube() const;
    BDD FindEssential() const;
    void PrintTwoLiteralClauses(char ** names = 0, FILE * fp = stdout) const;
    BDD ShortestPath(int * weight, int * support, int * length) const;
    BDD LargestCube(int * length = 0) const;
    int ShortestLength(int * weight = 0) const;
    bool EquivDC(const ABDD& G, const ABDD& D) const;
    double * CofMinterm() const;
    void PrintMinterm() const;
    double CountMinterm(int nvars) const;
    double CountPath() const;
    BDD Support() const;
    int SupportSize() const;
    std::vector<unsigned int> SupportIndices() const;
    void ClassifySupport(const ABDD& g, BDD* common, BDD* onlyF, BDD* onlyG)
	const;
    int CountLeaves() const;
    double Density(int nvars) const;

}; // ABDD


/**
  @brief Class for BDDs.

  @see Cudd

*/
class BDD : public ABDD {
    friend class Cudd;
public:
    BDD();
    BDD(Capsule *cap, DdNode *bddNode);
    BDD(Cudd const & manager, DdNode *ddNode);
    BDD(const BDD &from);
    BDD operator=(const BDD& right);
    bool operator<=(const BDD& other) const;
    bool operator>=(const BDD& other) const;
    bool operator<(const BDD& other) const;
    bool operator>(const BDD& other) const;
    BDD operator!() const;
    BDD operator~() const;
    BDD operator*(const BDD& other) const;
    BDD operator*=(const BDD& other);
    BDD operator&(const BDD& other) const;
    BDD operator&=(const BDD& other);
    BDD operator+(const BDD& other) const;
    BDD operator+=(const BDD& other);
    BDD operator|(const BDD& other) const;
    BDD operator|=(const BDD& other);
    BDD operator^(const BDD& other) const;
    BDD operator^=(const BDD& other);
    BDD operator-(const BDD& other) const;
    BDD operator-=(const BDD& other);
    friend std::ostream & operator<<(std::ostream & os, BDD const & f);
    bool IsZero() const;
    bool IsVar() const;
    BDD AndAbstract(const BDD& g, const BDD& cube, unsigned int limit = 0)
	const;
    BDD UnderApprox(
      int numVars,
      int threshold = 0,
      bool safe = false,
      double quality = 1.0) const;
    BDD OverApprox(
      int numVars,
      int threshold = 0,
      bool safe = false,
      double quality = 1.0) const;
    BDD RemapUnderApprox(int numVars, int threshold = 0, double quality = 1.0)
	const;
    BDD RemapOverApprox(int numVars, int threshold = 0, double quality = 1.0)
	const;
    BDD BiasedUnderApprox(const BDD& bias, int numVars, int threshold = 0, 
                          double quality1 = 1.0, double quality0 = 1.0) const;
    BDD BiasedOverApprox(const BDD& bias, int numVars, int threshold = 0, 
                         double quality1 = 1.0, double quality0 = 1.0) const;
    BDD ExistAbstract(const BDD& cube, unsigned int limit = 0) const;
    BDD XorExistAbstract(const BDD& g, const BDD& cube) const;
    BDD UnivAbstract(const BDD& cube) const;
    BDD BooleanDiff(int x) const;
    bool VarIsDependent(const BDD& var) const;
    double Correlation(const BDD& g) const;
    double CorrelationWeights(const BDD& g, double * prob) const;
    BDD Ite(const BDD& g, const BDD& h, unsigned int limit = 0) const;
    BDD IteConstant(const BDD& g, const BDD& h) const;
    BDD Intersect(const BDD& g) const;
    BDD And(const BDD& g, unsigned int limit = 0) const;
    BDD Or(const BDD& g, unsigned int limit = 0) const;
    BDD Nand(const BDD& g) const;
    BDD Nor(const BDD& g) const;
    BDD Xor(const BDD& g) const;
    BDD Xnor(const BDD& g, unsigned int limit = 0) const;
    bool Leq(const BDD& g) const;
    ADD Add() const;
    BDD Transfer(Cudd& destination) const;
    BDD ClippingAnd(const BDD& g, int maxDepth, int direction = 0) const;
    BDD ClippingAndAbstract(const BDD& g, const BDD& cube, int maxDepth,
			    int direction = 0) const;
    BDD Cofactor(const BDD& g) const;
    bool VarAreSymmetric(int index1, int index2) const;
    BDD Compose(const BDD& g, int v) const;
    BDD Permute(int * permut) const;
    BDD SwapVariables(std::vector<BDD> x, std::vector<BDD> y) const;
    BDD AdjPermuteX(std::vector<BDD> x) const;
    BDD VectorCompose(std::vector<BDD> vector) const;
    void ApproxConjDecomp(BDD* g, BDD* h) const;
    void ApproxDisjDecomp(BDD* g, BDD* h) const;
    void IterConjDecomp(BDD* g, BDD* h) const;
    void IterDisjDecomp(BDD* g, BDD* h) const;
    void GenConjDecomp(BDD* g, BDD* h) const;
    void GenDisjDecomp(BDD* g, BDD* h) const;
    void VarConjDecomp(BDD* g, BDD* h) const;
    void VarDisjDecomp(BDD* g, BDD* h) const;
    bool IsVarEssential(int id, int phase) const;
    BDD Constrain(const BDD& c) const;
    BDD Restrict(const BDD& c) const;
    BDD NPAnd(const BDD& g) const;
    std::vector<BDD> ConstrainDecomp() const;
    std::vector<BDD> CharToVect() const;
    BDD LICompaction(const BDD& c) const;
    BDD Squeeze(const BDD& u) const;
    BDD Interpolate(const BDD& u) const;
    BDD Minimize(const BDD& c) const;
    BDD SubsetCompress(int nvars, int threshold) const;
    BDD SupersetCompress(int nvars, int threshold) const;
    BDD LiteralSetIntersection(const BDD& g) const;
    BDD PrioritySelect(std::vector<BDD> x, std::vector<BDD> y,
		       std::vector<BDD> z, const BDD& Pi, DD_PRFP Pifunc) const;
    BDD CProjection(const BDD& Y) const;
    int MinHammingDist(int *minterm, int upperBound) const;
    BDD Eval(int * inputs) const;
    BDD Decreasing(int i) const;
    BDD Increasing(int i) const;
    bool LeqUnless(const BDD& G, const BDD& D) const;
    BDD MakePrime(const BDD& F) const;
    BDD MaximallyExpand(const BDD& ub, const BDD& f);
    BDD LargestPrimeUnate(const BDD& phases);
    BDD SolveEqn(const BDD& Y, std::vector<BDD> & G, int ** yIndex, int n) const;
    BDD VerifySol(std::vector<BDD> const & G, int * yIndex) const;
    BDD SplitSet(std::vector<BDD> xVars, double m) const;
    BDD SubsetHeavyBranch(int numVars, int threshold) const;
    BDD SupersetHeavyBranch(int numVars, int threshold) const;
    BDD SubsetShortPaths(int numVars, int threshold, bool hardlimit = false) const;
    BDD SupersetShortPaths(int numVars, int threshold, bool hardlimit = false) const;
    void PrintCover() const;
    void PrintCover(const BDD& u) const;
    int EstimateCofactor(int i, int phase) const;
    int EstimateCofactorSimple(int i) const;
    void PickOneCube(char * string) const;
    BDD PickOneMinterm(std::vector<BDD> vars) const;
    BDD zddIsop(const BDD& U, ZDD* zdd_I) const;
    BDD Isop(const BDD& U) const;
    ZDD PortToZdd() const;
    void PrintFactoredForm(char const * const * inames = 0, FILE * fp = stdout) const;
    std::string FactoredFormString(char const * const * inames = 0) const;

}; // BDD


/**
  @brief Class for ADDs.

  @see Cudd

*/
class ADD : public ABDD {
    friend class Cudd;
public:
    ADD();
    ADD(Capsule *cap, DdNode *bddNode);
    ADD(Cudd const & manager, DdNode *ddNode);
    ADD(const ADD &from);
    ADD operator=(const ADD& right);
    // Relational operators
    bool operator<=(const ADD& other) const;
    bool operator>=(const ADD& other) const;
    bool operator<(const ADD& other) const;
    bool operator>(const ADD& other) const;
    // Arithmetic operators
    ADD operator-() const;
    ADD operator*(const ADD& other) const;
    ADD operator*=(const ADD& other);
    ADD operator+(const ADD& other) const;
    ADD operator+=(const ADD& other);
    ADD operator-(const ADD& other) const;
    ADD operator-=(const ADD& other);
    // Logical operators
    ADD operator~() const;
    ADD operator&(const ADD& other) const;
    ADD operator&=(const ADD& other);
    ADD operator|(const ADD& other) const;
    ADD operator|=(const ADD& other);
    bool IsZero() const;
    ADD ExistAbstract(const ADD& cube) const;
    ADD UnivAbstract(const ADD& cube) const;
    ADD OrAbstract(const ADD& cube) const;
    ADD Plus(const ADD& g) const;
    ADD Times(const ADD& g) const;
    ADD Threshold(const ADD& g) const;
    ADD SetNZ(const ADD& g) const;
    ADD Divide(const ADD& g) const;
    ADD Minus(const ADD& g) const;
    ADD Minimum(const ADD& g) const;
    ADD Maximum(const ADD& g) const;
    ADD OneZeroMaximum(const ADD& g) const;
    ADD Diff(const ADD& g) const;
    ADD Agreement(const ADD& g) const;
    ADD Or(const ADD& g) const;
    ADD Nand(const ADD& g) const;
    ADD Nor(const ADD& g) const;
    ADD Xor(const ADD& g) const;
    ADD Xnor(const ADD& g) const;
    ADD Log() const;
    ADD FindMax() const;
    ADD FindMin() const;
    ADD IthBit(int bit) const;
    ADD ScalarInverse(const ADD& epsilon) const;
    ADD Ite(const ADD& g, const ADD& h) const;
    ADD IteConstant(const ADD& g, const ADD& h) const;
    ADD EvalConst(const ADD& g) const;
    bool Leq(const ADD& g) const;
    ADD Cmpl() const;
    ADD Negate() const;
    ADD RoundOff(int N) const;
    BDD BddThreshold(CUDD_VALUE_TYPE value) const;
    BDD BddStrictThreshold(CUDD_VALUE_TYPE value) const;
    BDD BddInterval(CUDD_VALUE_TYPE lower, CUDD_VALUE_TYPE upper) const;
    BDD BddIthBit(int bit) const;
    BDD BddPattern() const;
    ADD Cofactor(const ADD& g) const;
    ADD Compose(const ADD& g, int v) const;
    ADD Permute(int * permut) const;
    ADD SwapVariables(std::vector<ADD> x, std::vector<ADD> y) const;
    ADD VectorCompose(std::vector<ADD> vector) const;
    ADD NonSimCompose(std::vector<ADD> vector) const;
    ADD Constrain(const ADD& c) const;
    ADD Restrict(const ADD& c) const;
    ADD MatrixMultiply(const ADD& B, std::vector<ADD> z) const;
    ADD TimesPlus(const ADD& B, std::vector<ADD> z) const;
    ADD Triangle(const ADD& g, std::vector<ADD> z) const;
    ADD Eval(int * inputs) const;
    bool EqualSupNorm(const ADD& g, CUDD_VALUE_TYPE tolerance, int pr) const;

}; // ADD


/**
  @brief Class for ZDDs.

  @see Cudd

*/
class ZDD : public DD {
    friend class Cudd;
public:
    ZDD(Capsule *cap, DdNode *bddNode);
    ZDD();
    ZDD(const ZDD &from);
    ~ZDD();
    ZDD operator=(const ZDD& right);
    bool operator==(const ZDD& other) const;
    bool operator!=(const ZDD& other) const;
    bool operator<=(const ZDD& other) const;
    bool operator>=(const ZDD& other) const;
    bool operator<(const ZDD& other) const;
    bool operator>(const ZDD& other) const;
    void print(int nvars, int verbosity = 1) const;
    ZDD operator*(const ZDD& other) const;
    ZDD operator*=(const ZDD& other);
    ZDD operator&(const ZDD& other) const;
    ZDD operator&=(const ZDD& other);
    ZDD operator+(const ZDD& other) const;
    ZDD operator+=(const ZDD& other);
    ZDD operator|(const ZDD& other) const;
    ZDD operator|=(const ZDD& other);
    ZDD operator-(const ZDD& other) const;
    ZDD operator-=(const ZDD& other);
    int Count() const;
    double CountDouble() const;
    ZDD Product(const ZDD& g) const;
    ZDD UnateProduct(const ZDD& g) const;
    ZDD WeakDiv(const ZDD& g) const;
    ZDD Divide(const ZDD& g) const;
    ZDD WeakDivF(const ZDD& g) const;
    ZDD DivideF(const ZDD& g) const;
    double CountMinterm(int path) const;
    BDD PortToBdd() const;
    ZDD Ite(const ZDD& g, const ZDD& h) const;
    ZDD Union(const ZDD& Q) const;
    ZDD Intersect(const ZDD& Q) const;
    ZDD Diff(const ZDD& Q) const;
    ZDD DiffConst(const ZDD& Q) const;
    ZDD Subset1(int var) const;
    ZDD Subset0(int var) const;
    ZDD Change(int var) const;
    void PrintMinterm() const;
    void PrintCover() const;
    BDD Support() const;

}; // ZDD


/**
   @brief Default error handler.
*/
extern void defaultError(std::string message);


/**
  @brief Class for CUDD managers.

  @see DD

*/
class Cudd {
    friend class DD;
    friend class ABDD;
    friend class BDD;
    friend class ADD;
    friend class ZDD;
    friend std::ostream & operator<<(std::ostream & os, BDD const & f);
private:
    Capsule *p;
public:
    Cudd(
      unsigned int numVars = 0,
      unsigned int numVarsZ = 0,
      unsigned int numSlots = CUDD_UNIQUE_SLOTS,
      unsigned int cacheSize = CUDD_CACHE_SLOTS,
      unsigned long maxMemory = 0,
      PFC defaultHandler = defaultError);
    Cudd(const Cudd& x);
    ~Cudd(void);
    PFC setHandler(PFC newHandler) const;
    PFC getHandler(void) const;
    PFC setTimeoutHandler(PFC newHandler) const;
    PFC getTimeoutHandler(void) const;
    PFC setTerminationHandler(PFC newHandler) const;
    PFC getTerminationHandler(void) const;
    void pushVariableName(std::string s) const;
    void clearVariableNames(void) const;
    std::string getVariableName(size_t i) const;
    DdManager *getManager(void) const;
    void makeVerbose(void) const;
    void makeTerse(void) const;
    bool isVerbose(void) const;
    void checkReturnValue(const void *result) const;
    void checkReturnValue(const int result) const;
    Cudd& operator=(const Cudd& right);
    void info(void) const;
    BDD bddVar(void) const;
    BDD bddVar(int index) const;
    BDD bddOne(void) const;
    BDD bddZero(void) const;
    ADD addVar(void) const;
    ADD addVar(int index) const;
    ADD addOne(void) const;
    ADD addZero(void) const;
    ADD constant(CUDD_VALUE_TYPE c) const;
    ADD plusInfinity(void) const;
    ADD minusInfinity(void) const;
    ZDD zddVar(int index) const;
    ZDD zddOne(int i) const;
    ZDD zddZero(void) const;
    ADD addNewVarAtLevel(int level) const;
    BDD bddNewVarAtLevel(int level) const;
    void zddVarsFromBddVars(int multiplicity) const;
    unsigned long ReadStartTime(void) const;
    unsigned long ReadElapsedTime(void) const;
    void SetStartTime(unsigned long st) const;
    void ResetStartTime(void) const;
    unsigned long ReadTimeLimit(void) const;
    unsigned long SetTimeLimit(unsigned long tl) const;
    void UpdateTimeLimit(void) const;
    void IncreaseTimeLimit(unsigned long increase) const;
    void UnsetTimeLimit(void) const;
    bool TimeLimited(void) const;
    void RegisterTerminationCallback(DD_THFP callback,
                                     void * callback_arg) const;
    void UnregisterTerminationCallback(void) const;
    DD_OOMFP RegisterOutOfMemoryCallback(DD_OOMFP callback) const;
    void UnregisterOutOfMemoryCallback(void) const;
    void AutodynEnable(Cudd_ReorderingType method = CUDD_REORDER_SIFT) const;
    void AutodynDisable(void) const;
    bool ReorderingStatus(Cudd_ReorderingType * method) const;
    void AutodynEnableZdd(Cudd_ReorderingType method = CUDD_REORDER_SIFT) const;
    void AutodynDisableZdd(void) const;
    bool ReorderingStatusZdd(Cudd_ReorderingType * method) const;
    bool zddRealignmentEnabled(void) const;
    void zddRealignEnable(void) const;
    void zddRealignDisable(void) const;
    bool bddRealignmentEnabled(void) const;
    void bddRealignEnable(void) const;
    void bddRealignDisable(void) const;
    ADD background(void) const;
    void SetBackground(ADD bg) const;
    unsigned int ReadCacheSlots(void) const;
    double ReadCacheUsedSlots(void) const;
    double ReadCacheLookUps(void) const;
    double ReadCacheHits(void) const;
    unsigned int ReadMinHit(void) const;
    void SetMinHit(unsigned int hr) const;
    unsigned int ReadLooseUpTo(void) const;
    void SetLooseUpTo(unsigned int lut) const;
    unsigned int ReadMaxCache(void) const;
    unsigned int ReadMaxCacheHard(void) const;
    void SetMaxCacheHard(unsigned int mc) const;
    int ReadSize(void) const;
    int ReadZddSize(void) const;
    unsigned int ReadSlots(void) const;
    unsigned int ReadKeys(void) const;
    unsigned int ReadDead(void) const;
    unsigned int ReadMinDead(void) const;
    unsigned int ReadReorderings(void) const;
    unsigned int ReadMaxReorderings(void) const;
    void SetMaxReorderings(unsigned int mr) const;
    long ReadReorderingTime(void) const;
    int ReadGarbageCollections(void) const;
    long ReadGarbageCollectionTime(void) const;
    int ReadSiftMaxVar(void) const;
    void SetSiftMaxVar(int smv) const;
    int ReadSiftMaxSwap(void) const;
    void SetSiftMaxSwap(int sms) const;
    double ReadMaxGrowth(void) const;
    void SetMaxGrowth(double mg) const;
#ifdef MTR_H_
    MtrNode * ReadTree(void) const;
    void SetTree(MtrNode * tree) const;
    void FreeTree(void) const;
    MtrNode * ReadZddTree(void) const;
    void SetZddTree(MtrNode * tree) const;
    void FreeZddTree(void) const;
    MtrNode * MakeTreeNode(unsigned int low, unsigned int size,
                           unsigned int type) const;
    MtrNode * MakeZddTreeNode(unsigned int low, unsigned int size,
                              unsigned int type) const;
#endif
    int ReadPerm(int i) const;
    int ReadPermZdd(int i) const;
    int ReadInvPerm(int i) const;
    int ReadInvPermZdd(int i) const;
    BDD ReadVars(int i) const;
    CUDD_VALUE_TYPE ReadEpsilon(void) const;
    void SetEpsilon(CUDD_VALUE_TYPE ep) const;
    Cudd_AggregationType ReadGroupcheck(void) const;
    void SetGroupcheck(Cudd_AggregationType gc) const;
    bool GarbageCollectionEnabled(void) const;
    void EnableGarbageCollection(void) const;
    void DisableGarbageCollection(void) const;
    bool DeadAreCounted(void) const;
    void TurnOnCountDead(void) const;
    void TurnOffCountDead(void) const;
    int ReadRecomb(void) const;
    void SetRecomb(int recomb) const;
    int ReadSymmviolation(void) const;
    void SetSymmviolation(int symmviolation) const;
    int ReadArcviolation(void) const;
    void SetArcviolation(int arcviolation) const;
    int ReadPopulationSize(void) const;
    void SetPopulationSize(int populationSize) const;
    int ReadNumberXovers(void) const;
    void SetNumberXovers(int numberXovers) const;
    unsigned int ReadOrderRandomization(void) const;
    void SetOrderRandomization(unsigned int factor) const;
    unsigned long ReadMemoryInUse(void) const;
    long ReadPeakNodeCount(void) const;
    long ReadNodeCount(void) const;
    long zddReadNodeCount(void) const;
    void AddHook(DD_HFP f, Cudd_HookType where) const;
    void RemoveHook(DD_HFP f, Cudd_HookType where) const;
    bool IsInHook(DD_HFP f, Cudd_HookType where) const;
    void EnableReorderingReporting(void) const;
    void DisableReorderingReporting(void) const;
    bool ReorderingReporting(void) const;
    int ReadErrorCode(void) const;
    DD_OOMFP InstallOutOfMemoryHandler(DD_OOMFP newHandler) const;
    void ClearErrorCode(void) const;
    FILE *ReadStdout(void) const;
    void SetStdout(FILE * fp) const;
    FILE *ReadStderr(void) const;
    void SetStderr(FILE * fp) const;
    unsigned int ReadNextReordering(void) const;
    void SetNextReordering(unsigned int) const;
    double ReadSwapSteps(void) const;
    unsigned int ReadMaxLive(void) const;
    void SetMaxLive(unsigned int) const;
    size_t ReadMaxMemory(void) const;
    size_t SetMaxMemory(size_t) const;
    int bddBindVar(int) const;
    int bddUnbindVar(int) const;
    bool bddVarIsBound(int) const;
    ADD Walsh(std::vector<ADD> x, std::vector<ADD> y) const;
    ADD addResidue(int n, int m, int options, int top) const;
    int ApaNumberOfDigits(int binaryDigits) const;
    DdApaNumber NewApaNumber(int digits) const;
    void ApaCopy(int digits, DdApaNumber source, DdApaNumber dest) const;
    DdApaDigit ApaAdd(int digits, DdApaNumber a, DdApaNumber b, DdApaNumber
		      sum) const;
    DdApaDigit ApaSubtract(int digits, DdApaNumber a, DdApaNumber b,
			   DdApaNumber diff) const;
    DdApaDigit ApaShortDivision(int digits, DdApaNumber dividend, DdApaDigit
				divisor, DdApaNumber quotient) const;
    void ApaShiftRight(int digits, DdApaDigit in, DdApaNumber a, DdApaNumber
		       b) const;
    void ApaSetToLiteral(int digits, DdApaNumber number, DdApaDigit literal)
      const;
    void ApaPowerOfTwo(int digits, DdApaNumber number, int power) const;
    void ApaPrintHex(int digits, DdApaNumber number, FILE * fp = stdout) const;
    void ApaPrintDecimal(int digits, DdApaNumber number, FILE * fp = stdout) const;
    std::string ApaStringDecimal(int digits, DdApaNumber number) const;
    void ApaPrintExponential(int digits, DdApaNumber number,
                             int precision = 6, FILE * fp = stdout) const;
    void DebugCheck(void) const;
    void CheckKeys(void) const;
    ADD Harwell(FILE * fp, std::vector<ADD>& x, std::vector<ADD>& y,
                std::vector<ADD>& xn, std::vector<ADD>& yn_,
                int * m, int * n, int bx = 0, int sx = 2, int by = 1,
                int sy = 2, int pr = 0) const;
    void PrintLinear(void) const;
    int ReadLinear(int x, int y) const;
    BDD Xgty(std::vector<BDD> z, std::vector<BDD> x, std::vector<BDD> y) const;
    BDD Xeqy(std::vector<BDD> x, std::vector<BDD> y) const;
    ADD Xeqy(std::vector<ADD> x, std::vector<ADD> y) const;
    BDD Dxygtdxz(std::vector<BDD> x, std::vector<BDD> y,
                 std::vector<BDD> z) const;
    BDD Dxygtdyz(std::vector<BDD> x, std::vector<BDD> y,
                 std::vector<BDD> z) const;
    BDD Inequality(int c, std::vector<BDD> x, std::vector<BDD> y) const;
    BDD Disequality(int c, std::vector<BDD> x, std::vector<BDD> y) const;
    BDD Interval(std::vector<BDD> x, unsigned int lowerB,
                 unsigned int upperB) const;
    ADD Hamming(std::vector<ADD> xVars, std::vector<ADD> yVars) const;
    ADD Read(FILE * fp, std::vector<ADD>& x, std::vector<ADD>& y, std::vector<ADD>& xn,
             std::vector<ADD>& yn_, int * m, int * n, int bx = 0, int sx = 2,
             int by = 1, int sy = 2) const;
    BDD Read(FILE * fp, std::vector<BDD>& x, std::vector<BDD>& y,
             int * m, int * n, int bx = 0, int sx = 2, int by = 1,
             int sy = 2) const;
    void ReduceHeap(Cudd_ReorderingType heuristic = CUDD_REORDER_SIFT,
                    int minsize = 0) const;
    void ShuffleHeap(int * permutation) const;
    void SymmProfile(int lower, int upper) const;
    unsigned int Prime(unsigned int pr) const;
    void Reserve(int amount) const;
    int SharingSize(DD* nodes, int n) const;
    int SharingSize(const std::vector<BDD>& v) const;
    BDD bddComputeCube(BDD * vars, int * phase, int n) const;
    BDD computeCube(std::vector<BDD> const & vars) const;
    ADD addComputeCube(ADD * vars, int * phase, int n) const;
    ADD computeCube(std::vector<ADD> const & vars) const;
    BDD IndicesToCube(int * array, int n) const;
    void PrintVersion(FILE * fp) const;
    double AverageDistance(void) const;
    int32_t Random(void) const;
    void Srandom(int32_t seed) const;
    void zddPrintSubtable() const;
    void zddReduceHeap(Cudd_ReorderingType heuristic = CUDD_REORDER_SIFT,
                       int minsize = 0) const;
    void zddShuffleHeap(int * permutation) const;
    void zddSymmProfile(int lower, int upper) const;
    void DumpDot(
      const std::vector<BDD>& nodes, 
      char const * const * inames = 0, 
      char const * const * onames = 0, 
      FILE * fp = stdout) const;
    void DumpDaVinci(
      const std::vector<BDD>& nodes, 
      char const * const * inames = 0,
      char const * const * onames = 0,
      FILE * fp = stdout) const;
    void DumpBlif(
      const std::vector<BDD>& nodes, 
      char const * const * inames = 0,
      char const * const * onames = 0,
      char * mname = 0,
      FILE * fp = stdout,
      int mv = 0) const;
    void DumpDDcal(
      const std::vector<BDD>& nodes, 
      char const * const * inames = 0, 
      char const * const * onames = 0, 
      FILE * fp = stdout) const;
    void DumpFactoredForm(
      const std::vector<BDD>& nodes, 
      char const * const * inames = 0,
      char const * const * onames = 0,
      FILE * fp = stdout) const;
    BDD VectorSupport(const std::vector<BDD>& roots) const;
    std::vector<unsigned int> 
    SupportIndices(const std::vector<BDD>& roots) const;
    std::vector<unsigned int> 
    SupportIndices(const std::vector<ADD>& roots) const;
    int nodeCount(const std::vector<BDD>& roots) const;
    int VectorSupportSize(const std::vector<BDD>& roots) const;
    void DumpDot(
      const std::vector<ADD>& nodes,
      char const * const * inames = 0, 
      char const * const * onames = 0, 
      FILE * fp = stdout) const;
    void DumpDaVinci(
      const std::vector<ADD>& nodes,
      char const * const * inames = 0,
      char const * const * onames = 0,
      FILE * fp = stdout) const;
    BDD VectorSupport(const std::vector<ADD>& roots) const;
    int VectorSupportSize(const std::vector<ADD>& roots) const;
    void DumpDot(
      const std::vector<ZDD>& nodes,
      char const * const * inames = 0,
      char const * const * onames = 0,
      FILE * fp = stdout) const;
    std::string OrderString(void) const;

}; // Cudd


#endif
