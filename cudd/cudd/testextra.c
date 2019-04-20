/**
  @file

  @ingroup cudd

  @brief This program tests selected features of CUDD.

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
#include "epd.h"
#include "cudd.h"
#include <setjmp.h>

/** \cond */
static int testBdd(int verbosity);
static int testAdd(int verbosity);
static int testZdd(int verbosity);
static int testApa(int verbosity);
static int testCount(int verbosity);
static int testLdbl(int verbosity);
static int testTimeout(int verbosity);
static void timeoutHandler(DdManager * dd, void * arg);
/** \endcond */

/**
 * @brief Main program for testextra.
 */
int main(int argc, char const * const * argv)
{
  int verbosity = 0;
  if (argc == 2) {
    int nread;
    int ret = sscanf(argv[1], "%d%n", &verbosity, &nread);
    if (ret != 1 || argv[1][nread]) {
      fprintf(stderr, "Usage: %s [verbosity]\n", argv[0]);
      return -1;
    }
  }
  if (testBdd(verbosity) != 0)
    return -1;
  if (testAdd(verbosity) != 0)
    return -1;
  if (testZdd(verbosity) != 0)
    return -1;
  if (testApa(verbosity) != 0)
    return -1;
  if (testCount(verbosity) != 0)
    return -1;
  if (testLdbl(verbosity) != 0)
    return -1;
  if (testTimeout(verbosity) != 0)
    return -1;
  return 0;
}

/**
 * @brief Basic BDD test.
 * @return 0 if successful; -1 otherwise.
 */
static int
testBdd(int verbosity)
{
  DdManager *dd;
  DdNode *f, *var, *tmp;
  int i, ret;

  dd = Cudd_Init(0, 0, CUDD_UNIQUE_SLOTS, CUDD_CACHE_SLOTS, 0);
  if (!dd) {
    if (verbosity) {
      printf("initialization failed\n");
    }
    return -1;
  }
  if (verbosity) {
    printf("Started CUDD version ");
    Cudd_PrintVersion(stdout);
  }
  f = Cudd_ReadOne(dd);
  Cudd_Ref(f);
  for (i = 3; i >= 0; i--) {
    var = Cudd_bddIthVar(dd, i);
    tmp = Cudd_bddAnd(dd, Cudd_Not(var), f);
    if (!tmp) {
      if (verbosity) {
        printf("computation failed\n");
      }
      return -1;
    }
    Cudd_Ref(tmp);
    Cudd_RecursiveDeref(dd, f);
    f = tmp;
  }
  if (verbosity) {
    Cudd_bddPrintCover(dd, f, f);
  }
  Cudd_RecursiveDeref(dd, f);
  ret = Cudd_CheckZeroRef(dd);
  if (ret != 0 && verbosity) {
    printf("%d unexpected non-zero references\n", ret);
  }
  Cudd_Quit(dd);
  return 0;
}

/**
 * @brief Basic ADD test.
 * @return 0 if successful; -1 otherwise.
 */
static int
testAdd(int verbosity)
{
  DdManager *manager;
  DdNode *f, *var, *tmp, *bg;
  int i, ret;
  CUDD_VALUE_TYPE pinf;

  manager = Cudd_Init(0, 0, CUDD_UNIQUE_SLOTS, CUDD_CACHE_SLOTS, 0);
  if (!manager) {
    if (verbosity) {
      printf("initialization failed\n");
    }
    return -1;
  }
  pinf = Cudd_V(Cudd_ReadPlusInfinity(manager));
  if (verbosity) {
    printf("Plus infinity is %g\n", pinf);
  }
  f = Cudd_addConst(manager,5);
  Cudd_Ref(f);
  for (i = 3; i >= 0; i--) {
    var = Cudd_addIthVar(manager,i);
    Cudd_Ref(var);
    tmp = Cudd_addApply(manager,Cudd_addTimes,var,f);
    Cudd_Ref(tmp);
    Cudd_RecursiveDeref(manager,f);
    Cudd_RecursiveDeref(manager,var);
    f = tmp;
  }
  if (verbosity) {
    Cudd_PrintMinterm(manager, f);
    printf("\n");
  }
  Cudd_RecursiveDeref(manager, f);
  bg = Cudd_ReadBackground(manager);
  if (verbosity) {
    printf("background (%g) minterms : ", Cudd_V(bg));
    Cudd_ApaPrintMinterm(Cudd_ReadStdout(manager), manager, bg, 0);
  }
  ret = Cudd_CheckZeroRef(manager);
  if (ret != 0 && verbosity) {
    printf("%d non-zero ADD reference counts after dereferencing\n", ret);
  }
  Cudd_Quit(manager);
  return ret != 0;
}

/**
 * @brief Basic test of ZDDs.
 * @return 0 if successful; -1 otherwise.
 */
static int
testZdd(int verbosity)
{
  DdManager *manager;
  DdNode *f, *var, *tmp;
  int i, ret;

  manager = Cudd_Init(0,4,CUDD_UNIQUE_SLOTS, CUDD_CACHE_SLOTS,0);
  if (!manager) {
    if (verbosity) {
      printf("initialization failed\n");
    }
    return -1;
  }
  tmp = Cudd_ReadZddOne(manager,0);
  Cudd_Ref(tmp);
  for (i = 3; i >= 0; i--) {
    var = Cudd_zddIthVar(manager,i);
    Cudd_Ref(var);
    f = Cudd_zddIntersect(manager,var,tmp);
    Cudd_Ref(f);
    Cudd_RecursiveDerefZdd(manager,tmp);
    Cudd_RecursiveDerefZdd(manager,var);
    tmp = f;
  }
  f = Cudd_zddDiff(manager,Cudd_ReadZddOne(manager,0),tmp);
  Cudd_Ref(f);
  Cudd_RecursiveDerefZdd(manager,tmp);
  if (verbosity) {
    Cudd_zddPrintMinterm(manager,f);
    printf("\n");
  }
  Cudd_RecursiveDerefZdd(manager,f);
  ret = Cudd_CheckZeroRef(manager);
  if (ret != 0 && verbosity) {
    printf("%d unexpected non-zero references\n", ret);
  }
  Cudd_Quit(manager);
  return 0;
}

/**
 * @brief Basic test of arbitrary-precision arithmetic.
 * @return 0 if successful; -1 otherwise.
 */
static int
testApa(int verbosity)
{
  if (verbosity) {
    printf("DD_APA_BITS = %" PRIszt "\n", sizeof(DdApaDigit) * 8);
  }
  DdApaNumber an = Cudd_NewApaNumber(3);
  Cudd_ApaSetToLiteral(3, an, (DdApaDigit) 0x0fa5);
  Cudd_ApaAdd(3, an, an, an);
  if (verbosity) {
    Cudd_ApaPrintHex(stdout, 3, an);
    printf("\n");
  }
  DdApaDigit numbers[] = {1283805, 1283815, 15983557, 1598354, 15999999};
  size_t i;
  for (i = 0; i < sizeof(numbers)/sizeof(DdApaDigit); i++) {
    Cudd_ApaSetToLiteral(3, an, numbers[i]);
    if (verbosity) {
      Cudd_ApaPrintDecimal(stdout, 3, an);
      printf(" -> ");
      Cudd_ApaPrintExponential(stdout, 3, an, 6);
      printf("\n");
    }
  }
  Cudd_FreeApaNumber(an);
  return 0;
}

/**
 * @brief Basic test of Cudd_CountMinterm().
 * @return 0 if successful; -1 otherwise.
 */
static int
testCount(int verbosity)
{
  DdManager *dd;
  DdNode *h;
  int i, ret;
  int const N = 2044; /* number of variables */

  dd = Cudd_Init(N, 0, CUDD_UNIQUE_SLOTS, CUDD_CACHE_SLOTS, 0);
  if (!dd) {
    if (verbosity) {
      printf("initialization failed\n");
    }
    return -1;
  }
  /* Build a cube with N/2 variables. */
  h = Cudd_ReadOne(dd);
  Cudd_Ref(h);
  for (i = 0; i < N; i += 2) {
    DdNode *var, *tmp;
    var = Cudd_bddIthVar(dd, N-i-1);
    tmp = Cudd_bddAnd(dd, h, var);
    if (!tmp) {
      Cudd_Quit(dd);
      return -1;
    }
    Cudd_Ref(tmp);
    Cudd_RecursiveDeref(dd, h);
    h = tmp;
  }
  if (verbosity) {
    printf("h (dbl) ");
    Cudd_PrintDebug(dd, h, N, 1);
    printf("h (apa) ");
    Cudd_PrintSummary(dd, h, N, 1);
  }
  Cudd_RecursiveDeref(dd, h);
  if (verbosity) {
    printf("one[%d] (dbl) ", N);
    Cudd_PrintDebug(dd, Cudd_ReadOne(dd), N, 1);
    printf("one[%d] (apa) ", N);
    Cudd_PrintSummary(dd, Cudd_ReadOne(dd), N, 1);
    ret = Cudd_CheckZeroRef(dd);
    printf("one[%d] (dbl) ", N+1);
    Cudd_PrintDebug(dd, Cudd_ReadOne(dd), N+1, 1);
    printf("one[%d] (apa) ", N+1);
    Cudd_PrintSummary(dd, Cudd_ReadOne(dd), N+1, 1);
    ret = Cudd_CheckZeroRef(dd);
  }
  if (verbosity && ret != 0) {
    printf("%d non-zero references\n", ret);
  }
  Cudd_Quit(dd);
  return 0;
}

/**
 * @brief Basic test of long double and EPD minterm computation.
 * @return 0 if successful; -1 otherwise.
 */
static int
testLdbl(int verbosity)
{
  DdManager *dd;
  DdNode *f, *g;
  int i, ret;
  int const N = 12; /* half the number of variables */
  long double cnt;

  dd = Cudd_Init(2*N, 0, CUDD_UNIQUE_SLOTS, CUDD_CACHE_SLOTS, 0);
  if (!dd) {
    if (verbosity) {
      printf("initialization failed\n");
    }
    return -1;
  }
  f = g = Cudd_ReadOne(dd);
  Cudd_Ref(f);
  Cudd_Ref(g);
  for (i = 0; i < N; i++) {
    DdNode *var1, *var2, *clause, *tmp;
    var1 = Cudd_bddIthVar(dd, i);
    var2 = Cudd_bddIthVar(dd, i+N);
    clause = Cudd_bddOr(dd, var1, var2);
    if (!clause) {
      Cudd_Quit(dd);
      return -1;
    }
    Cudd_Ref(clause);
    tmp = Cudd_bddAnd(dd, f, clause);
    if (!tmp) {
      Cudd_Quit(dd);
      return -1;
    }
    Cudd_Ref(tmp);
    Cudd_RecursiveDeref(dd, clause);
    Cudd_RecursiveDeref(dd, f);
    f = tmp;
    clause = Cudd_bddOr(dd, Cudd_Not(var1), Cudd_Not(var2));
    if (!clause) {
      Cudd_Quit(dd);
      return -1;
    }
    Cudd_Ref(clause);
    tmp = Cudd_bddAnd(dd, g, clause);
    if (!tmp) {
      Cudd_Quit(dd);
      return -1;
    }
    Cudd_Ref(tmp);
    Cudd_RecursiveDeref(dd, clause);
    Cudd_RecursiveDeref(dd, g);
    g = tmp;
  }
  if (verbosity) {
    printf("f");
    Cudd_PrintSummary(dd, f, 2*N, 0);
  }
  cnt = Cudd_LdblCountMinterm(dd, f, 2*N);
  if (verbosity) {
    printf("f has %Lg minterms\n", cnt);
  }
  if (verbosity) {
    printf("EPD count for f = ");
    ret = Cudd_EpdPrintMinterm(dd, f, 2*N);
    printf("\n");
    if (!ret) {
      printf("problem with EPD\n");
    }
  }
  Cudd_RecursiveDeref(dd, f);
  if (verbosity) {
    printf("g");
    Cudd_PrintSummary(dd, g, 2*N, 0);
  }
  cnt = Cudd_LdblCountMinterm(dd, g, 2*N);
  if (verbosity) {
    printf("g has %Lg minterms\n", cnt);
  }
  if (verbosity) {
    printf("EPD count for g = ");
    ret = Cudd_EpdPrintMinterm(dd, g, 2*N);
    printf("\n");
    if (!ret) {
      printf("problem with EPD\n");
    }
  }
  Cudd_RecursiveDeref(dd, g);
  ret = Cudd_CheckZeroRef(dd);
  if (verbosity && ret != 0) {
    printf("%d non-zero references\n", ret);
  }
  Cudd_Quit(dd);
  return 0;
}


/**
 * @brief Basic test of timeout handler.
 *
 * @details Sets a short timeout and then tries to build a function
 * with a large BDD.  Strives to avoid leaking nodes.
 *
 * @return 0 if successful; -1 otherwise.
 */
static int
testTimeout(int verbosity)
{
  DdManager *dd;
  /* Declare these "volatile" to prevent clobbering by longjmp. */
  DdNode * volatile f;
  DdNode * volatile clause = NULL;
  DdNode * var1, * var2;
  int i, ret, count;
  int const N = 20; /* half the number of variables in f */
  unsigned long timeout = 100UL; /* in milliseconds */
  jmp_buf timeoutEnv;

  dd = Cudd_Init(0, 0, CUDD_UNIQUE_SLOTS, CUDD_CACHE_SLOTS, 0);
  if (!dd) {
    if (verbosity) {
      printf("initialization failed\n");
    }
    return -1;
  }

  /* Set up timeout handling. */
  if (setjmp(timeoutEnv) > 0) {
    if (verbosity) {
      printf("caught timeout\n");
    }
    /* The nodes of clause may be leaked if the timeout was
     * detected while conjoining the clause to f.  We set
     * clause to NULL when it's not in use to be able to
     * detect this case.
     */
    if (clause)
      Cudd_RecursiveDeref(dd, clause);
    goto finally;
  }
  (void) Cudd_RegisterTimeoutHandler(dd, timeoutHandler, (void *) &timeoutEnv);
  (void) Cudd_SetTimeLimit(dd, timeout);

  /* Try to build function.  This is expected to run out of time. */
  f = Cudd_ReadOne(dd);
  Cudd_Ref(f);
  for (i = 0; i < N; i++) {
    DdNode * tmp;
    var1 = Cudd_bddIthVar(dd, i);
    if (!var1) {
      if (verbosity) {
        printf("computation failed\n");
        return -1;
      }
    }
    var2 = Cudd_bddIthVar(dd, i+N);
    if (!var2) {
      if (verbosity) {
        printf("computation failed\n");
        return -1;
      }
    }
    clause = Cudd_bddOr(dd, var1, var2);
    if (!clause) {
      if (verbosity) {
        printf("computation failed\n");
      }
      return -1;
    }
    Cudd_Ref(clause);
    tmp = Cudd_bddAnd(dd, f, clause);
    if (!tmp) {
      if (verbosity) {
        printf("computation failed\n");
      }
      return -1;
    }
    Cudd_Ref(tmp);
    Cudd_RecursiveDeref(dd, clause);
    clause = NULL;
    Cudd_RecursiveDeref(dd, f);
    f = tmp;
  }
  if (verbosity > 1) {
    Cudd_bddPrintCover(dd, f, f);
  }

 finally:
  if (verbosity) {
    printf("so far");
    Cudd_PrintSummary(dd, f, 2*N, 0);
  }
  count = 0;
  for (i = 0; i < N-1; i += 2) {
    var1 = Cudd_bddIthVar(dd, i);
    if (!var1) {
      printf("computation failed\n");
      return -1;
    }
    var2 = Cudd_bddIthVar(dd, i+1);
    if (!var2) {
      printf("computation failed\n");
      return -1;
    }
    clause = Cudd_bddOr(dd, var1, var2);
    if (!clause) {
      printf("computation failed\n");
      return -1;
    }
    Cudd_Ref(clause);
    if (Cudd_bddLeq(dd, f, clause)) {
      count++;
    }
    Cudd_RecursiveDeref(dd, clause);
  }
  if (verbosity) {
    printf("f implies %d clauses\n", count);
  }
  Cudd_RecursiveDeref(dd, f);
  ret = Cudd_CheckZeroRef(dd);
  if (verbosity) {
    Cudd_PrintInfo(dd, stdout);
    if (ret != 0) {
      printf("%d non-zero references\n", ret);
    }
  }
  Cudd_Quit(dd);
  return 0;
}

/**
 * @brief Timeout handler.
 */
static void
timeoutHandler(DdManager * dd, void * arg)
{
  jmp_buf * timeoutEnv = (jmp_buf *) arg;
  /* Reset manager. */
  Cudd_ClearErrorCode(dd);
  Cudd_UnsetTimeLimit(dd);
  Cudd_RegisterTimeoutHandler(dd, NULL, NULL);

  longjmp(*timeoutEnv, 1);
}
