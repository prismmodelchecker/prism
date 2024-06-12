/**
  @file

  @ingroup cplusplus

  @brief Test program for multiple managers (one per thread).

  @details This program tests the ability to run different CUDD managers
  in different threads.  Each thread builds the hidden weight bit function
  for a certain number of variables and then reorders the variables.

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

#include "config.h"
#include "cuddObj.hh"
#include <cstdlib>
#include <iostream>
#include <sstream>
#if HAVE_WORKING_THREAD == 1
#include <thread>

/**
 * @brief Taks performed by each thread.
 */
class Task {
public:
    /** Constructor. */
    Task(int n, std::ostringstream & os) : n(n), os(os) {}
    /** Builds the hidden weight bit function and reorders the variables. */
    void operator()(void) {
        Cudd mgr;
        mgr.AutodynEnable();
        int nvars = n + 32;
        std::vector<BDD> vars;
        for (int i = 0; i != nvars; ++i) {
            vars.push_back(mgr.bddVar());
        }
        os << "Report from thread " << n << " with " << nvars << " variables: ";
        // The hidden weight bit function is built from a tally circuit and
        // a multiplexer.  First the tally circuit...
        std::vector<BDD> oldt;
        oldt.push_back(mgr.bddOne());
        std::vector<BDD> t;
        for (int i = 1; i != nvars + 1; ++i) {
            t.clear();
            t.push_back(oldt.at(0) & !vars.at(i-1));
            for (int j = 1; j != i; ++j) {
                t.push_back(vars.at(i-1).Ite(oldt.at(j-1), oldt.at(j)));
            }
            t.push_back(oldt.at(i-1) & vars.at(i-1));
            oldt = t;
        }
#if 0
        // Diagnostic print.
        for (int i = 0;  i != nvars + 1; ++i) {
            os << "t(" << i << ") = " << t.at(i).FactoredFormString()
               << std::endl;
        }
#endif
        // ...then the multiplexer.
        BDD hwb = mgr.bddZero();
        for (int i = 0; i != nvars; ++i) {
            hwb |= t.at(i+1) & vars.at(i);
        }
        mgr.ReduceHeap(CUDD_REORDER_SIFT_CONVERGE);
        
        int nodes = hwb.nodeCount();
        os << nodes << " nodes and ";
        int digits;
        DdApaNumber apa_minterms = hwb.ApaCountMinterm(nvars, &digits);
        os << mgr.ApaStringDecimal(digits, apa_minterms) << " minterms\n";
        free(apa_minterms);
        os << "Variable order: " << mgr.OrderString() << "\n";
        mgr.Srandom(n+11);
        os << "A random number from our generator: " << mgr.Random() << "\n";
#if 0
        // Diagnostic prints.
        hwb.summary(nvars);
        os << hwb.FactoredFormString() << std::endl;
#endif
    }
private:
    int n;
    std::ostringstream & os;
};


/**
 * @brief Class to join threads in RAII fashion.
 */
class joinThreads {
public:
    explicit joinThreads(std::vector<std::thread>& t) : threads_(t) {}
    /** It completes once all threads have been joined. */
    ~joinThreads() {
        for (std::vector<std::thread>::iterator it = threads_.begin();
             it != threads_.end(); ++it)
            if (it->joinable())
                it->join();
    }
    joinThreads(joinThreads const &) = delete;
    joinThreads& operator=(joinThreads const &) = delete;
private:
    std::vector<std::thread>& threads_; /**< vector of threads to be joined. */
};
#endif

/**
  @brief Main program for testmulti.
*/
int main(int argc, char **argv)
{
    int nthreads = 4; // default value

    // If there's an argument, it's the number of threads.
    if (argc == 2) {
        int nread;
        int retval = sscanf(argv[1], "%d%n", &nthreads, &nread);
        if (retval != 1 || argv[1][nread]) {
            std::cerr << "The argument should be an integer." << std::endl;
            return 1;
        }
    } else if (argc != 1) {
        std::cerr << "Either no arguments or one argument." << std::endl;
        return 1;
    }

#if HAVE_WORKING_THREAD == 1
    // Each thread has its own output stream, so that main can print thread
    // reports without interleaving.  We can't use an std::vector here because
    // old versions of g++ don't support move semantics (and streams can't
    // be copied).  We can't use a variable-length array either because it's
    // a g++ extension and clang++ rejects it.  Hence we use new/delete.
    std::ostringstream *oss = new std::ostringstream[nthreads];

    // Multi-threaded code in this block.
    {
        std::vector<std::thread> t;
        joinThreads joiner(t); // threads are joined by its destructor
        for (int n = 0; n != nthreads; ++n) {
            t.push_back(std::thread(Task(n, oss[n])));
        }
    }

    // Print the reports.
    for (int n = 0; n != nthreads; ++n) {
        std::cout << oss[n].str();
    }

    delete [] oss;

    return 0;
#else
    return 77; // test skipped
#endif
}
