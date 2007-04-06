//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

// includes
#include "PrismMTBDD.h"
#include <math.h>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include "PrismMTBDDGlob.h"

//------------------------------------------------------------------------------

// solve the linear equation system Ax=x with the Power method
// in addition, solutions may be provided for additional states in the vector b
// these states are assumed not to have non-zero rows in the matrix A

jint JNICALL Java_mtbdd_PrismMTBDD_PM_1Power
(
JNIEnv *env,
jclass cls,
jint _odd,			// odd
jint rv,			// row vars
jint num_rvars,
jint cv,			// col vars
jint num_cvars,
jint _a,			// matrix A
jint _b,			// vector b (if null, assume all zero)
jint _init,			// init soln
jboolean transpose	// transpose A? (i.e. solve xA=b not Ax=b?)
)
{
	// cast function parameters
	ODDNode *odd = (ODDNode *)_odd;		// odd
	DdNode **rvars = (DdNode **)rv; 	// row vars
	DdNode **cvars = (DdNode **)cv; 	// col vars
	DdNode *a = (DdNode *)_a;			// matrix A
	DdNode *b = (DdNode *)_b;			// vector b
	DdNode *init = (DdNode *)_init;		// init soln
	// mtbdds
	DdNode *reach, *sol, *tmp;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, iters;
	bool done;
	
	// start clocks
	start1 = start2 = util_cpu_time();
	
	// make local copy of b
	Cudd_Ref(b);
	
	// print out some memory usage
	i = DD_GetNumNodes(ddman, a);
	PM_PrintToMainLog(env, "\nIteration matrix MTBDD... [nodes=%d] [%.1f Kb]\n", i, i*20.0/1024.0);
	
	// transpose b if necessary
	if (transpose) {
		b = DD_PermuteVariables(ddman, b, rvars, cvars, num_rvars);
	}
	
	// store initial solution, transposing if necessary
	Cudd_Ref(init);
	sol = init;
	if (transpose) {
		sol = DD_PermuteVariables(ddman, sol, rvars, cvars, num_rvars);
	}
	
	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	
	// start iterations
	iters = 0;
	done = false;
	PM_PrintToMainLog(env, "\nStarting iterations...\n");
	
	while (!done && iters < max_iters) {
		
		iters++;
		
//		PM_PrintToMainLog(env, "Iteration %d: ", iters);
//		start3 = util_cpu_time();
		
		// matrix multiply
		Cudd_Ref(sol);
		tmp = DD_PermuteVariables(ddman, sol, (transpose?cvars:rvars), (transpose?rvars:cvars), num_rvars);
		Cudd_Ref(a);
		tmp = DD_MatrixMultiply(ddman, a, tmp, (transpose?rvars:cvars), num_cvars, MM_BOULDER);
		Cudd_Ref(b);
		tmp = DD_Apply(ddman, APPLY_PLUS, tmp, b);
		
		// check convergence
		switch (term_crit) {
		case TERM_CRIT_ABSOLUTE:
			if (DD_EqualSupNorm(ddman, tmp, sol, term_crit_param)) {
				done = true;
			}
			break;
		case TERM_CRIT_RELATIVE:
			if (DD_EqualSupNormRel(ddman, tmp, sol, term_crit_param)) {
				done = true;
			}
			break;
		}
		
		// prepare for next iteration
		Cudd_RecursiveDeref(ddman, sol);
		sol = tmp;
		
//		PM_PrintToMainLog(env, "%.2f %.2f sec\n", ((double)(util_cpu_time() - start3)/1000), ((double)(util_cpu_time() - start2)/1000)/iters);
	}
	
	// transpose solution if necessary
	if (transpose) {
		sol = DD_PermuteVariables(ddman, sol, cvars, rvars, num_rvars);
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iters/timing info
	PM_PrintToMainLog(env, "\nPower method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	// free memory
	Cudd_RecursiveDeref(ddman, b);
	
	// if the iterative method didn't terminate, this is an error
	if (!done) { Cudd_RecursiveDeref(ddman, sol); PM_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters); return 0; }
	
	return (int)sol;
}

//------------------------------------------------------------------------------
