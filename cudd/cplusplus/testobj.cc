/**
  @file

  @ingroup cplusplus

  @brief Test program for the C++ object-oriented encapsulation of CUDD.

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

#include "cuddObj.hh"
#include <math.h>
#include <iostream>
#include <sstream>
#include <cassert>
#include <stdexcept>

using namespace std;

/*---------------------------------------------------------------------------*/
/* Variable declarations                                                     */
/*---------------------------------------------------------------------------*/

/*---------------------------------------------------------------------------*/
/* Static function prototypes                                                */
/*---------------------------------------------------------------------------*/

static void testBdd(Cudd& mgr, int verbosity);
static void testAdd(Cudd& mgr, int verbosity);
static void testAdd2(Cudd& mgr, int verbosity);
static void testZdd(Cudd& mgr, int verbosity);
static void testBdd2(Cudd& mgr, int verbosity);
static void testBdd3(Cudd& mgr, int verbosity);
static void testZdd2(Cudd& mgr, int verbosity);
static void testBdd4(Cudd& mgr, int verbosity);
static void testBdd5(Cudd& mgr, int verbosity);
static void testInterpolation(Cudd& mgr, int verbosity);
static void testErrorHandling(Cudd& mgr, int verbosity);

/*---------------------------------------------------------------------------*/
/* Definition of exported functions                                          */
/*---------------------------------------------------------------------------*/

/**
  @brief Main program for testobj.
*/
int
main(int argc, char **argv)
{
    int verbosity = 0;

    if (argc == 2) {
        int cnt;
        int retval = sscanf(argv[1], "%d %n", &verbosity, &cnt);
        if (retval != 1 || argv[1][cnt])
            return 1;
    } else if (argc != 1) {
        return 1;
    }

    Cudd mgr(0,2);
    if (verbosity > 2) mgr.makeVerbose(); // trace constructors and destructors
    testBdd(mgr,verbosity);
    testAdd(mgr,verbosity);
    testAdd2(mgr,verbosity);
    testZdd(mgr,verbosity);
    testBdd2(mgr,verbosity);
    testBdd3(mgr,verbosity);
    testZdd2(mgr,verbosity);
    testBdd4(mgr,verbosity);
    testBdd5(mgr,verbosity);
    testInterpolation(mgr,verbosity);
    testErrorHandling(mgr,verbosity);
    if (verbosity) mgr.info();
    return 0;

} // main


/**
  @brief Test basic operators on BDDs.

  @details The function returns void
  because it relies on the error handling done by the interface. The
  default error handler causes program termination.

  @sideeffect Creates BDD variables in the manager.

  @see testBdd2 testBdd3 testBdd4 testBdd5

*/
static void
testBdd(
  Cudd& mgr,
  int verbosity)
{
    if (verbosity) cout << "Entering testBdd\n";
    // Create two new variables in the manager. If testBdd is called before
    // any variable is created in mgr, then x gets index 0 and y gets index 1.
    BDD x = mgr.bddVar();
    BDD y = mgr.bddVar();

    BDD f = x * y;
    if (verbosity) cout << "f"; f.print(2,verbosity);

    BDD g = y + !x;
    if (verbosity) cout << "g"; g.print(2,verbosity);

    if (verbosity) 
        cout << "f and g are" << (f == !g ? "" : " not") << " complementary\n";
    if (verbosity) 
        cout << "f is" << (f <= g ? "" : " not") << " less than or equal to g\n";

    g = f | ~g;
    if (verbosity) cout << "g"; g.print(2,verbosity);

    BDD h = f = y;
    if (verbosity) cout << "h"; h.print(2,verbosity);

    if (verbosity) cout << "x + h has " << (x+h).nodeCount() << " nodes\n";

    h += x;
    if (verbosity) cout << "h"; h.print(2,verbosity);

} // testBdd


/**
  @brief Test basic operators on ADDs.

  @details The function returns void because it relies on the error
  handling done by the interface. The default error handler causes
  program termination.

  @sideeffect May create ADD variables in the manager.

  @see testAdd2

*/
static void
testAdd(
  Cudd& mgr,
  int verbosity)
{
    if (verbosity) cout << "Entering testAdd\n";
    // Create two ADD variables. If we called method addVar without an
    // argument, we would get two new indices. If testAdd is indeed called
    // after testBdd, then those indices would be 2 and 3. By specifying the
    // arguments, on the other hand, we avoid creating new unnecessary BDD
    // variables.
    ADD p = mgr.addVar(0);
    ADD q = mgr.addVar(1);

    // Test arithmetic operators.
    ADD r = p + q;
    if (verbosity) cout << "r"; r.print(2,verbosity);

    // CUDD_VALUE_TYPE is double.
    ADD s = mgr.constant(3.0);
    s *= p * q;
    if (verbosity) cout << "s"; s.print(2,verbosity);

    s += mgr.plusInfinity();
    if (verbosity) cout << "s"; s.print(2,verbosity);

    // Test relational operators.
    if (verbosity) 
        cout << "p is" << (p <= r ? "" : " not") << " less than or equal to r\n";

    // Test logical operators.
    r = p | q;
    if (verbosity) cout << "r"; r.print(2,verbosity);

} // testAdd


/**
  @brief Test some more operators on ADDs.

  @details The function returns void because it relies on the error
  handling done by the interface. The default error handler causes
  program termination.

  @sideeffect May create ADD variables in the manager.

  @see testAdd

*/
static void
testAdd2(
  Cudd& mgr,
  int verbosity)
{
    if (verbosity) cout << "Entering testAdd2\n";
    // Create two ADD variables. If we called method addVar without an
    // argument, we would get two new indices.
    vector<ADD> x(2);
    for (size_t i = 0; i < 2; ++i) {
      x[i] = mgr.addVar((int) i);
    }

    // Build a probability density function: [0.1, 0.2, 0.3, 0.4].
    ADD f0 = x[1].Ite(mgr.constant(0.2), mgr.constant(0.1));
    ADD f1 = x[1].Ite(mgr.constant(0.4), mgr.constant(0.3));
    ADD f  = x[0].Ite(f1, f0);
    if (verbosity) cout << "f"; f.print(2,verbosity);

    // Compute the entropy.
    ADD l = f.Log();
    if (verbosity) cout << "l"; l.print(2,verbosity);
    ADD r = f * l;
    if (verbosity) cout << "r"; r.print(2,verbosity);

    ADD e = r.MatrixMultiply(mgr.constant(-1.0/log(2.0)),x);
    if (verbosity) cout << "e"; e.print(2,verbosity);

} // testAdd2


/**
  @brief Test basic operators on ZDDs.

  @details The function returns void because it relies on the error
  handling done by the interface. The default error handler causes
  program termination.

  @sideeffect May create ZDD variables in the manager.

  @see testZdd2

*/
static void
testZdd(
  Cudd& mgr,
  int verbosity)
{
    if (verbosity) cout << "Entering testZdd\n";
    ZDD v = mgr.zddVar(0);
    ZDD w = mgr.zddVar(1);

    ZDD s = v + w;
    if (verbosity) cout << "s"; s.print(2,verbosity);

    if (verbosity) cout << "v is" << (v < s ? "" : " not") << " less than s\n";

    s -= v;
    if (verbosity) cout << "s"; s.print(2,verbosity);

} // testZdd


/**
  @brief Test vector operators on BDDs.

  @details The function returns void because it relies on the error
  handling done by the interface. The default error handler causes
  program termination.

  @sideeffect May create BDD variables in the manager.

  @see testBdd testBdd3 testBdd4 testBdd5

*/
static void
testBdd2(
  Cudd& mgr,
  int verbosity)
{
    if (verbosity) cout << "Entering testBdd2\n";
    vector<BDD> x(4);
    for (size_t i = 0; i < 4; ++i) {
      x[i] = mgr.bddVar((int) i);
    }

    // Create the BDD for the Achilles' Heel function.
    BDD p1 = x[0] * x[2];
    BDD p2 = x[1] * x[3];
    BDD f = p1 + p2;
    const char* inames[] = {"x0", "x1", "x2", "x3"};
    if (verbosity) {
        cout << "f"; f.print(4,verbosity);
        cout << "Irredundant cover of f:" << endl; f.PrintCover();
        cout << "Number of minterms (arbitrary precision): "; f.ApaPrintMinterm(4);
        cout << "Number of minterms (extended precision):  "; f.EpdPrintMinterm(4);
        cout << "Two-literal clauses of f:" << endl;
        f.PrintTwoLiteralClauses((char **)inames); cout << endl;
    }

    vector<BDD> vect = f.CharToVect();
    if (verbosity) {
        for (size_t i = 0; i < vect.size(); i++) {
            cout << "vect[" << i << "]" << endl; vect[i].PrintCover();
        }
    }

    // v0,...,v3 suffice if testBdd2 is called before testBdd3.
    if (verbosity) {
        const char* onames[] = {"v0", "v1", "v2", "v3", "v4", "v5"};
        mgr.DumpDot(vect, (char **)inames,(char **)onames);
    }

} // testBdd2


/**
  @brief Test additional operators on BDDs.

  @details The function returns void because it relies on the error
  handling done by the interface. The default error handler causes
  program termination.

  @sideeffect May create BDD variables in the manager.

  @see testBdd testBdd2 testBdd4 testBdd5

*/
static void
testBdd3(
  Cudd& mgr,
  int verbosity)
{
    if (verbosity) cout << "Entering testBdd3\n";
    vector<BDD> x(6);
    for (size_t i = 0; i < 6; ++i) {
      x[i] = mgr.bddVar((int) i);
    }

    BDD G = x[4] + !x[5];
    BDD H = x[4] * x[5];
    BDD E = x[3].Ite(G,!x[5]);
    BDD F = x[3] + !H;
    BDD D = x[2].Ite(F,!H);
    BDD C = x[2].Ite(E,!F);
    BDD B = x[1].Ite(C,!F);
    BDD A = x[0].Ite(B,!D);
    BDD f = !A;
    if (verbosity) cout << "f"; f.print(6,verbosity);

    BDD f1 = f.RemapUnderApprox(6);
    if (verbosity) cout << "f1"; f1.print(6,verbosity);
    if (verbosity) 
        cout << "f1 is" << (f1 <= f ? "" : " not") << " less than or equal to f\n";

    BDD g;
    BDD h;
    f.GenConjDecomp(&g,&h);
    if (verbosity) {
        cout << "g"; g.print(6,verbosity);
        cout << "h"; h.print(6,verbosity);
        cout << "g * h " << (g * h == f ? "==" : "!=") << " f\n";
    }

} // testBdd3


/**
  @brief Test cover manipulation with BDDs and ZDDs.

  @details The function returns void because it relies on the error
  handling done by the interface.  The default error handler causes
  program termination.  This function builds the BDDs for a
  transformed adder: one in which the inputs are transformations of
  the original inputs. It then creates ZDDs for the covers from the
  BDDs.

  @sideeffect May create BDD and ZDD variables in the manager.

  @see testZdd

*/
static void
testZdd2(
  Cudd& mgr,
  int verbosity)
{
    if (verbosity) cout << "Entering testZdd2\n";
    size_t N = 3;			// number of bits
    // Create variables.
    vector<BDD> a(N);
    vector<BDD> b(N);
    vector<BDD> c(N+1);
    for (size_t i = 0; i < N; ++i) {
      a[N-1-i] = mgr.bddVar(2*(int)i);
      b[N-1-i] = mgr.bddVar(2*(int)i+1);
    }
    c[0] = mgr.bddVar(2*(int)N);
    // Build functions.
    vector<BDD> s(N);
    for (size_t i = 0; i < N; ++i) {
	s[i] = a[i].Xnor(c[i]);
	c[i+1] = a[i].Ite(b[i],c[i]);
    }

    // Create array of outputs and print it.
    vector<BDD> p(N+1);
    for (size_t i = 0; i < N; ++i) {
	p[i] = s[i];
    }
    p[N] = c[N];
    if (verbosity) {
        for (size_t i = 0; i < p.size(); ++i) {
          cout << "p[" << i << "]"; p[i].print(2*(int)N+1,verbosity);
        }
    }
    const char* onames[] = {"s0", "s1", "s2", "c3"};
    if (verbosity) {
        const char* inames[] = {"a2", "b2", "a1", "b1", "a0", "b0", "c0"};
        mgr.DumpDot(p, (char **)inames,(char **)onames);
    }

    // Create ZDD variables and build ZDD covers from BDDs.
    mgr.zddVarsFromBddVars(2);
    vector<ZDD> z(N+1);
    for (size_t i = 0; i < N+1; ++i) {
	ZDD temp;
	BDD dummy = p[i].zddIsop(p[i],&temp);
	z[i] = temp;
    }

    // Print out covers.
    if (verbosity) {
        DdGen *gen;
        int *path;
        for (size_t i = 0; i < z.size(); i++) {
          cout << "z[" << i << "]"; z[i].print(4*(int)N+2,verbosity);
        }
        // Print cover in two different ways: with PrintCover and with
        // enumeration over the paths.  The only difference should be
        // a reversal in the order of the cubes.
        for (size_t i = 0; i < z.size(); i++) {
            cout << "z[" << i << "]\n"; z[i].PrintCover();
            cout << "z[" << i << "]\n";
            DdNode *f = Cudd_Not(z[i].getNode());
            Cudd_zddForeachPath(z[i].manager(), f, gen, path) {
                for (size_t q = 0; q < 4*N+2; q += 2) {
                    int v = path[q] * 4 + path[q+1];
                    switch (v) {
                    case 0:
                    case 2:
                    case 8:
                    case 10:
                        cout << "-";
                        break;
                    case 1:
                    case 9:
                        cout << "0";
                        break;
                    case 6:
                        cout << "1";
                        break;
                    default:
                        cout << "?";
                    }
                }
                cout << " 1\n";
            }
        }
        const char* znames[] = {"a2+", "a2-", "b2+", "b2-", "a1+", "a1-", "b1+",
                                "b1-", "a0+", "a0-", "b0+", "b0-", "c0+", "c0-"};
        mgr.DumpDot(z, (char **)znames,(char **)onames);
    }

} // testZdd2


/**
  @brief Test transfer between BDD managers.

  @details The function returns void because it relies on the error
  handling done by the interface.  The default error handler causes
  program termination.

  @sideeffect May create BDD variables in the manager.

  @see testBdd testBdd2 testBdd3 testBdd5

*/
static void
testBdd4(
  Cudd& mgr,
  int verbosity)
{
    if (verbosity) cout << "Entering testBdd4\n";
    BDD x = mgr.bddVar(0);
    BDD y = mgr.bddVar(1);
    BDD z = mgr.bddVar(2);

    BDD f = (~x & ~y & ~z) | (x & y);
    if (verbosity) cout << "f"; f.print(3,verbosity);

    Cudd otherMgr(0,0);
    BDD g = f.Transfer(otherMgr);
    if (verbosity) cout << "g"; g.print(3,verbosity);

    BDD h = g.Transfer(mgr);
    if (verbosity) 
        cout << "f and h are" << (f == h ? "" : " not") << " identical\n";

} // testBdd4


/**
  @brief Test maximal expansion of cubes.

  @details The function returns void because it relies on the error
  handling done by the interface.  The default error handler causes
  program termination.

  @sideeffect May create BDD variables in the manager.

  @see testBdd testBdd2 testBdd3 testBdd4

*/
static void
testBdd5(
  Cudd& mgr,
  int verbosity)
{
    if (verbosity) cout << "Entering testBdd5\n";
    vector<BDD> x;
    x.reserve(4);
    for (int i = 0; i < 4; i++) {
	x.push_back(mgr.bddVar(i));
    }
    const char* inames[] = {"a", "b", "c", "d"};
    BDD f = (x[1] & x[3]) | (x[0] & ~x[2] & x[3]) | (~x[0] & x[1] & ~x[2]);
    BDD lb = x[1] & ~x[2] & x[3];
    BDD ub = x[3];
    BDD primes = lb.MaximallyExpand(ub,f);
    assert(primes == (x[1] & x[3]));
    BDD lprime = primes.LargestPrimeUnate(lb);
    assert(lprime == primes);
    if (verbosity) {
      const char * onames[] = {"lb", "ub", "f", "primes", "lprime"};
        vector<BDD> z;
        z.reserve(5);
        z.push_back(lb);
        z.push_back(ub);
        z.push_back(f);
        z.push_back(primes);
        z.push_back(lprime);
        mgr.DumpDot(z, (char **)inames, (char **)onames);
        cout << "primes(1)"; primes.print(4,verbosity);
    }

    lb = ~x[0] & x[2] & x[3];
    primes = lb.MaximallyExpand(ub,f);
    assert(primes == mgr.bddZero());
    if (verbosity) {
        cout << "primes(2)"; primes.print(4,verbosity);
    }

    lb = x[0] & ~x[2] & x[3];
    primes = lb.MaximallyExpand(ub,f);
    assert(primes == lb);
    lprime = primes.LargestPrimeUnate(lb);
    assert(lprime == primes);
    if (verbosity) {
        cout << "primes(3)"; primes.print(4,verbosity);
    }

    lb = ~x[0] & x[1] & ~x[2] & x[3];
    ub = mgr.bddOne();
    primes = lb.MaximallyExpand(ub,f);
    assert(primes == ((x[1] & x[3]) | (~x[0] & x[1] & ~x[2])));
    lprime = primes.LargestPrimeUnate(lb);
    assert(lprime == (x[1] & x[3]));
    if (verbosity) {
        cout << "primes(4)"; primes.print(4,1); primes.PrintCover();
    }

    ub = ~x[0] & x[3];
    primes = lb.MaximallyExpand(ub,f);
    assert(primes == (~x[0] & x[1] & x[3]));
    lprime = primes.LargestPrimeUnate(lb);
    assert(lprime == primes);
    if (verbosity) {
        cout << "primes(5)"; primes.print(4,verbosity);
    }

} // testBdd5


/**
  @brief Test BDD interpolation.
*/
static void
testInterpolation(
  Cudd& mgr,
  int verbosity)
{
    BDD a = mgr.bddVar(0);
    BDD b = mgr.bddVar(1);
    BDD c = mgr.bddVar(2);
    BDD d = mgr.bddVar(3);

    BDD l1 = (a | d) & b & c;
    BDD u1 = (~a & ~b & ~c) | ((a | b) & c);
    BDD ip1 = l1.Interpolate(u1);
    if (verbosity) {
        cout << "l1"; l1.print(4,verbosity);
        cout << "u1"; u1.print(4,verbosity);
        cout << "interpolant1"; ip1.print(4,verbosity);
    }

    BDD l2 = (~a | ~b) & (a | c) & (b | c) & (a | ~b | ~d);
    BDD u2 = (~b & ~d) | (~b & c & d) | (b & c & ~d);
    BDD ip2 = l2.Interpolate(u2);
    if (verbosity) {
        cout << "l2"; l2.print(4,verbosity);
        cout << "u2"; u2.print(4,verbosity);
        cout << "interpolant2"; ip2.print(4,verbosity);
    }

    BDD l3 = ~a & ~b & d;
    BDD u3 = ~b & d;
    BDD ip3 = l3.Interpolate(u3);
    if (verbosity) {
        cout << "l3"; l3.print(4,verbosity);
        cout << "u3"; u3.print(4,verbosity);
        cout << "interpolant3"; ip3.print(4,verbosity);
    }

} // testInterpolation


/**
  @brief Basic test of error handling.

  @details This function also illustrates the use of the overloading of the
  stream insertion operator (operator<<) for BDDs.
*/
static void
testErrorHandling(
  Cudd& mgr,
  int verbosity)
{
    // Setup.

    if (verbosity) cout << "Entering testErrorHandling\n";

    FILE *savefp = 0;
    if (verbosity == 0) {
        // Suppress error messages coming from CUDD.
        savefp = mgr.ReadStderr();
#ifndef _WIN32
        FILE * devnull = fopen("/dev/null", "w");
#else
        FILE * devnull = fopen("NUL", "w");
#endif
        if (devnull)
            mgr.SetStderr(devnull);
    }

    size_t const N = 60;
    vector<BDD> vars;
    vars.reserve(N);
    for (size_t i = 0; i < N; ++i) {
        vars.push_back(mgr.bddVar((int) i));
    }

    // It is necessary to give names to all the BDD variables in the manager
    // for the names to be used by operator<<.
    for (int i = 0; i < mgr.ReadSize(); ++i) {
        ostringstream os;
        os << "var[" << i << "]";
        mgr.pushVariableName(os.str());
    }

    // Tests.

    // Trying to print the expression of an empty BDD.
    try {
        BDD empty;
        if (verbosity > 0)
            cout << "Oops! ";
        cout << empty << endl;
    } catch (logic_error const & e) {
        if (verbosity > 0)
            cerr << "Caught: " << e.what() << endl;
    }

    // Trying to extract a minterm from the zero BDD.
    try {
        BDD zero = mgr.bddZero();
        BDD minterm = zero.PickOneMinterm(vars);
    } catch (logic_error const & e) {
        if (verbosity > 0)
            cerr << "Caught: " << e.what() << endl;
        mgr.ClearErrorCode();
    }

    // Passing a non-cube second argument to Cofactor.
    try {
        BDD f = vars.at(1) | (vars.at(2) & vars.at(3));
        if (verbosity > 0)
            cout << "f = " << f << endl;
        BDD notAcube = vars.at(0) | vars.at(1);
        if (verbosity > 0)
            cout << notAcube << " is not a cube" << endl;
        BDD fc = f.Cofactor(notAcube);
        if (verbosity > 0) {
            cout << "The cofactor is: "; fc.summary(3);
        }
    } catch (logic_error const & e) {
        if (verbosity > 0)
            cerr << "Caught: " << e.what() << endl;
        mgr.ClearErrorCode();
    }

#if 0
    // This attempt to allocate over 100 GB may succeed on machines with
    // enough memory; hence we exclude it from "make check."
    // Failing malloc.
    // Don't let the memory manager kill the program if malloc fails.
    DD_OOMFP saveHandler = mgr.InstallOutOfMemoryHandler(Cudd_OutOfMemSilent);
    try {
        mgr.Reserve(2000000000);
    } catch (logic_error const & e) {
        if (verbosity > 0)
            cerr << "Caught: " << e.what() << endl;
        mgr.ClearErrorCode();
    }
    (void) mgr.InstallOutOfMemoryHandler(saveHandler);
#endif

    // Forgetting to check for empty result when setting a limit on
    // the number of new nodes.
    try {
        BDD f = mgr.bddOne();
        BDD g = f;
        for (size_t i = 0; i < N/2; i += 4) {
            f &= vars.at(i) | vars.at(i+N/2);
            g &= vars.at(i+1) | vars.at(i+N/2+1);
        }
        if (verbosity > 0) {
            cout << "f "; f.summary(N);
            cout << "g "; g.summary(N);
        }
        BDD h = f.And(g, /* max new nodes */ 1);
        if (verbosity > 0) {
            cout << "h "; h.summary(N);
        }
    } catch (logic_error const & e) {
        if (verbosity > 0)
            cerr << "Caught: " << e.what() << endl;
        mgr.ClearErrorCode();
    }

    // Using more memory than the set limit.
    size_t saveLimit = mgr.SetMaxMemory((size_t) 1);
    try {
        // The limit is ridiculously low (1 byte), but CUDD is resourceful.
        // Therefore we can still create a few BDDs.
        BDD f = mgr.Interval(vars, 122346345U, 348353453U);
        if (verbosity > 0) {
            cout << "f "; f.summary(N);
        }
        BDD g = mgr.Interval(vars, 34234U, 3143534534U);
        if (verbosity > 0) {
            cout << "g "; g.summary(N);
        }
        BDD h = f ^ g;
        if (verbosity > 0) {
            cout << "h "; h.summary(N);
        }
        // But if we really insist...
        BDD extra = mgr.bddVar(60000);
        // Here we would have to fix the variable names.
    } catch (logic_error const & e) {
        if (verbosity > 0)
            cerr << "Caught: " << e.what() << endl;
        mgr.ClearErrorCode();
    }
    (void) mgr.SetMaxMemory(saveLimit);

    // Timing out.
    unsigned long saveTl = mgr.SetTimeLimit(1UL); // 1 ms
    try {
        BDD f = mgr.bddOne();
        for (size_t i = 0; i < N/2; ++i) {
            f &= vars.at(i) | vars.at(i+N/2);
        }
    } catch (logic_error const & e) {
        if (verbosity > 0)
            cerr << "Caught: " << e.what() << endl;
        mgr.ClearErrorCode();
    }
    (void) mgr.SetTimeLimit(saveTl);

    // Let's clean up after ourselves.
    mgr.clearVariableNames();
    if (verbosity == 0) {
        mgr.SetStderr(savefp);
    }

} // testErrorHandling
