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

JNIEXPORT jint JNICALL Java_mtbdd_PrismMTBDD_PM_1NondetBoundedUntil
(
JNIEnv *env,
jclass cls,
jint t,			// trans matrix
jint od,		// odd
jint ndm,		// nondeterminism mask
jint rv,		// row vars
jint num_rvars,
jint cv,		// col vars
jint num_cvars,
jint ndv,		// nondet vars
jint num_ndvars,
jint y,			// 'yes' states
jint m,			// 'maybe' states
jint bound,		// time bound
jboolean min		// min or max probabilities (true = min, false = max)
)
{
	// cast function parameters
	DdNode *trans = (DdNode *)t;		// trans matrix
	ODDNode *odd = (ODDNode *)od;		// odd
	DdNode *mask = (DdNode *)ndm;		// nondeterminism mask
	DdNode **rvars = (DdNode **)rv;		// row vars
	DdNode **cvars = (DdNode **)cv;		// col vars
	DdNode **ndvars = (DdNode **)ndv;	// nondet vars
	DdNode *yes = (DdNode *)y;		// 'yes' states
	DdNode *maybe = (DdNode *)m;		// 'maybe' states
	// mtbdds
	DdNode *a, *sol, *tmp;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int iters, i;

	// start clocks	
	start1 = start2 = util_cpu_time();

	// get a - filter out rows
	PM_PrintToMainLog(env, "\nBuilding iteration matrix MTBDD... ");
	Cudd_Ref(trans);
	Cudd_Ref(maybe);
	a = DD_Apply(ddman, APPLY_TIMES, trans, maybe);
	i = DD_GetNumNodes(ddman, a);
	PM_PrintToMainLog(env, "[nodes=%d] [%.1f Kb]\n", i, i*20.0/1024.0);
			
	// initial solution
	// (prob in 0 steps given by yes)
	Cudd_Ref(yes);
	sol = yes;
	
	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;

	// start iterations
	PM_PrintToMainLog(env, "\nStarting iterations...\n");

	// note that we ignore max_iters as we know how any iterations _should_ be performed
	for (iters = 0; iters < bound; iters++) {
			
//		PM_PrintToMainLog(env, "Iteration %d: ", iters);
//		start3 = util_cpu_time();

//		DD_PrintInfoBrief(ddman, sol, num_rvars);

		// matrix-vector multiply
		Cudd_Ref(sol);
		tmp = DD_PermuteVariables(ddman, sol, rvars, cvars, num_rvars);
		Cudd_Ref(a);
		tmp = DD_MatrixMultiply(ddman, a, tmp, cvars, num_cvars, MM_BOULDER);

//		DD_PrintInfoBrief(ddman, tmp, num_rvars+num_ndvars);
		
		// do min/max
		if (min) {
			// mask stuff
			Cudd_Ref(mask);
			tmp = DD_Apply(ddman, APPLY_MAX, tmp, mask);
			// abstract
			tmp = DD_MinAbstract(ddman, tmp, ndvars, num_ndvars);
		}
		else {
			// abstract
			tmp = DD_MaxAbstract(ddman, tmp, ndvars, num_ndvars);
		}
		
		// put 1s (for 'yes' states) back into into solution vector		
		Cudd_Ref(yes);
		tmp = DD_Apply(ddman, APPLY_MAX, tmp, yes);
		
		// prepare for next iteration
		Cudd_RecursiveDeref(ddman, sol);
		sol = tmp;
		
//		PM_PrintToMainLog(env, "%.2f %.2f sec\n", ((double)(util_cpu_time() - start3)/1000), ((double)(util_cpu_time() - start2)/1000)/iters);
	}

	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;

	// print iterations/timing info
	PM_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);

	// free memory
	Cudd_RecursiveDeref(ddman, a);
	
	return (int)sol;
}

//------------------------------------------------------------------------------
