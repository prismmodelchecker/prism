//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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
#include <cmath>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include "PrismMTBDDGlob.h"
#include "jnipointer.h"
#include "prism.h"
#include "ExportIterations.h"
#include "IntervalIteration.h"
#include <memory>

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_mtbdd_PrismMTBDD_PM_1NondetUntilInterval
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer t,	// trans matrix
jlong __jlongpointer od,	// odd
jlong __jlongpointer ndm,	// nondeterminism mask
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer ndv,	// nondet vars
jint num_ndvars,
jlong __jlongpointer y,	// 'yes' states
jlong __jlongpointer m,	// 'maybe' states
jboolean min,		// min or max probabilities (true = min, false = max)
jint flags
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(t);		// trans matrix
	ODDNode *odd = jlong_to_ODDNode(od);		// odd
	DdNode *mask = jlong_to_DdNode(ndm);		// nondeterminism mask
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars
	DdNode **ndvars = jlong_to_DdNode_array(ndv);	// nondet vars
	DdNode *yes = jlong_to_DdNode(y);		// 'yes' states
	DdNode *maybe = jlong_to_DdNode(m);		// 'maybe' states

	// mtbdds
	DdNode *a, *sol_above, *tmp_above, *sol_below, *tmp_below;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	bool done;
	int iters, i;

	IntervalIteration helper(flags);

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
	sol_below = yes;

	// initial solution from above: 1 for yes and maybe states
	Cudd_Ref(yes);
	Cudd_Ref(maybe);
	sol_above = DD_Or(ddman, yes, maybe);

	std::unique_ptr<ExportIterations> iterationExport;
	if (PM_GetFlagExportIterations()) {
		iterationExport.reset(new ExportIterations("PM_NondetUntilInterval"));
		iterationExport->exportVector(sol_below, rvars, num_rvars, odd, 0);
		iterationExport->exportVector(sol_above, rvars, num_rvars, odd, 1);
	}

	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;

	// start iterations
	iters = 0;
	done = false;
	PM_PrintToMainLog(env, "\nStarting iterations (interval iteration)...\n");

	bool below_unchanged = false, above_unchanged = false;

	while (!done && iters < max_iters) {
		below_unchanged = above_unchanged = false;

//		if (iters%20==0) {
//			PM_PrintToMainLog(env, "Iteration %d:\n", iters);
//			DD_PrintTerminalsAndNumbers(ddman, sol, num_rvars);
//		}
	
		iters++;
		
//		DD_PrintInfoBrief(ddman, sol, num_rvars);

		// matrix-vector multiply (below)
		Cudd_Ref(sol_below);
		tmp_below = DD_PermuteVariables(ddman, sol_below, rvars, cvars, num_rvars);
		Cudd_Ref(a);
		tmp_below = DD_MatrixMultiply(ddman, a, tmp_below, cvars, num_cvars, MM_BOULDER);

		// matrix-vector multiply (above)
		Cudd_Ref(sol_above);
		tmp_above = DD_PermuteVariables(ddman, sol_above, rvars, cvars, num_rvars);
		Cudd_Ref(a);
		tmp_above = DD_MatrixMultiply(ddman, a, tmp_above, cvars, num_cvars, MM_BOULDER);

//		DD_PrintInfoBrief(ddman, tmp, num_rvars+num_ndvars);
		
		// do min/max
		if (min) {
			// mask stuff
			Cudd_Ref(mask);
			tmp_below = DD_Apply(ddman, APPLY_MAX, tmp_below, mask);
			// abstract
			tmp_below = DD_MinAbstract(ddman, tmp_below, ndvars, num_ndvars);

			// mask stuff
			Cudd_Ref(mask);
			tmp_above = DD_Apply(ddman, APPLY_MAX, tmp_above, mask);
			// abstract
			tmp_above = DD_MinAbstract(ddman, tmp_above, ndvars, num_ndvars);
		}
		else {
			// abstract
			tmp_below = DD_MaxAbstract(ddman, tmp_below, ndvars, num_ndvars);
			// abstract
			tmp_above = DD_MaxAbstract(ddman, tmp_above, ndvars, num_ndvars);
		}

		// put 1s (for 'yes' states) back into into solution vector		
		Cudd_Ref(yes);
		tmp_below = DD_Apply(ddman, APPLY_MAX, tmp_below, yes);
		Cudd_Ref(yes);
		tmp_above = DD_Apply(ddman, APPLY_MAX, tmp_above, yes);

		if (helper.flag_ensure_monotonic_from_below()) {
			// below: do max of tmp_below with old solution
			Cudd_Ref(sol_below);
			tmp_below = DD_Apply(ddman, APPLY_MAX, tmp_below, sol_below);
		}
		if (helper.flag_ensure_monotonic_from_above()) {
			// above: do min of tmp_below with old solution
			Cudd_Ref(sol_above);
			tmp_above = DD_Apply(ddman, APPLY_MIN, tmp_above, sol_above);
		}

		if (iterationExport) {
			iterationExport->exportVector(tmp_below, rvars, num_rvars, odd, 0);
			iterationExport->exportVector(tmp_above, rvars, num_rvars, odd, 1);
		}

		// check convergence
		switch (term_crit) {
		case TERM_CRIT_ABSOLUTE:
			if (DD_EqualSupNorm(ddman, tmp_below, tmp_above, term_crit_param)) {
				done = true;
			}
			break;
		case TERM_CRIT_RELATIVE:
			if (DD_EqualSupNormRel(ddman, tmp_below, tmp_above, term_crit_param)) {
				done = true;
			}
			break;
		}

		if (sol_below == tmp_below) below_unchanged = true;
		if (sol_above == tmp_above) above_unchanged = true;

		if (!done && below_unchanged && above_unchanged) {
			break;
		}

		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PM_PrintToMainLog(env, "Iteration %d: ", iters);
			PM_PrintToMainLog(env, "sol_below=%d nodes sol_above=%d nodes", DD_GetNumNodes(ddman, sol_below), DD_GetNumNodes(ddman, sol_above));
			// NB: but tmp was probably bigger than sol (pre min/max-abstract)
			PM_PrintToMainLog(env, ", %.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
			start3 = util_cpu_time();
		}
		
		// prepare for next iteration
		Cudd_RecursiveDeref(ddman, sol_below);
		Cudd_RecursiveDeref(ddman, sol_above);
		sol_below = tmp_below;
		sol_above = tmp_above;
	}
							
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;

	// print iterations/timing info
	PM_PrintToMainLog(env, "\nIterative method (interval iteration): %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);

	DdNode *result;
	if (helper.flag_select_midpoint() && done) { // we did converge, select midpoint
		Cudd_Ref(sol_below);
		Cudd_Ref(sol_above);

		// compute midpoint for result
		// use x + ( y - x ) / 2 instead of (x+y)/2 for better numerical stability
		// TODO: ensure that below <= result <= above?
		DdNode* difference = DD_Apply(ddman, APPLY_MINUS, sol_above, sol_below);
		Cudd_Ref(sol_below);
		Cudd_Ref(difference);
		result = DD_Apply(ddman, APPLY_PLUS, sol_below, DD_Apply(ddman, APPLY_DIVIDE, difference, DD_Constant(ddman, 2.0)));
		
		// also compute/store accuracy
		// TODO: handle cases where result is zero
		if (term_crit == TERM_CRIT_RELATIVE) {
			Cudd_Ref(result);
			difference = DD_Apply(ddman, APPLY_DIVIDE, difference, result);
		}
		last_error_bound = DD_FindMax(ddman, difference);
		Cudd_RecursiveDeref(ddman, difference);

		// export midpoint as vector above and below
		if (iterationExport) {
			iterationExport->exportVector(result, rvars, num_rvars, odd, 0);
			iterationExport->exportVector(result, rvars, num_rvars, odd, 1);
		}
	} else {
		result = sol_below;
		Cudd_Ref(result);
	}

	// free memory
	Cudd_RecursiveDeref(ddman, a);
	Cudd_RecursiveDeref(ddman, sol_below);
	Cudd_RecursiveDeref(ddman, sol_above);

	// if the iterative method didn't terminate, this is an error
	if (!done) {
		Cudd_RecursiveDeref(ddman, result);
		if (below_unchanged && above_unchanged) {
			PM_SetErrorMessage("In interval iteration, after %d iterations, both lower and upper iteration did not change anymore but don't have the required precision yet.\nThis could be caused by the MTBDD's engine collapsing of similar constants, consider setting a smaller value for -cuddepsilon or -cuddepsilon 0 to disable collapsing", iters);
		} else {
			PM_SetErrorMessage("Iterative method (interval iteration) did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters);
		}
		return 0;
	}

	return ptr_to_jlong(result);
}

//------------------------------------------------------------------------------
