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

JNIEXPORT jlong __jlongpointer JNICALL Java_mtbdd_PrismMTBDD_PM_1NondetReachRewardInterval
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer t,	// trans matrix
jlong __jlongpointer sr,	// state rewards
jlong __jlongpointer trr,	// state rewards
jlong __jlongpointer od,	// odd
jlong __jlongpointer ndm,	// nondeterminism mask
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer ndv,	// nondet vars
jint num_ndvars,
jlong __jlongpointer g,	// 'goal' states
jlong __jlongpointer in,	// 'inf' states
jlong __jlongpointer m,	// 'maybe' states
jlong __jlongpointer l,  // lower bound
jlong __jlongpointer u,  // uper bound
jboolean min,		// min or max probabilities (true = min, false = max)
jint flags
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(t);		// trans matrix
	DdNode *state_rewards = jlong_to_DdNode(sr);	// state rewards
	DdNode *trans_rewards = jlong_to_DdNode(trr);	// transition rewards
	ODDNode *odd = jlong_to_ODDNode(od);		// odd
	DdNode *mask = jlong_to_DdNode(ndm);		// nondeterminism mask
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars
	DdNode **ndvars = jlong_to_DdNode_array(ndv);	// nondet vars
	DdNode *goal = jlong_to_DdNode(g);		// 'goal' states
	DdNode *inf = jlong_to_DdNode(in); 		// 'inf' states
	DdNode *maybe = jlong_to_DdNode(m); 		// 'maybe' states
	DdNode *lower = jlong_to_DdNode(l); 		// lower bound
	DdNode *upper = jlong_to_DdNode(u); 		// upper bound

	// mtbdds
	DdNode *reach, *a, *all_rewards, *new_mask, *sol_below, *sol_above, *tmp_below, *tmp_above;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	bool done;
	int iters, i;

	IntervalIteration helper(flags);
	if (!helper.flag_ensure_monotonic_from_above()) {
		PM_PrintToMainLog(env, "Note: Interval iteration is configured to not enforce monotonicity from above.\n");
	}
	if (!helper.flag_ensure_monotonic_from_below()) {
		PM_PrintToMainLog(env, "Note: Interval iteration is configured to not enforce monotonicity from below.\n");
	}

	// start clocks
	start1 = start2 = util_cpu_time();
	
	// get reachable states
	reach = odd->dd;
	
	PM_PrintToMainLog(env, "\nBuilding iteration matrix MTBDD... ");
	
	// filter out rows (goal states and infinity states) from matrix
	Cudd_Ref(trans);
	Cudd_Ref(maybe);
	a = DD_Apply(ddman, APPLY_TIMES, trans, maybe);
	
	// also remove goal and infinity states from state rewards vector
	Cudd_Ref(state_rewards);
	Cudd_Ref(maybe);
	state_rewards = DD_Apply(ddman, APPLY_TIMES, state_rewards, maybe);
	
	// multiply transition rewards by transition probs and sum rows
	// (note also filters out unwanted states at the same time)
	Cudd_Ref(trans_rewards);
	Cudd_Ref(a);
	trans_rewards = DD_Apply(ddman, APPLY_TIMES, trans_rewards, a);
	trans_rewards = DD_SumAbstract(ddman, trans_rewards, cvars, num_cvars);
	
	// combine state and transition rewards
	all_rewards = DD_Apply(ddman, APPLY_PLUS, state_rewards, trans_rewards);
	
	// need to change mask because rewards are not necessarily in the range 0..1
	Cudd_Ref(mask);
	new_mask = DD_ITE(ddman, mask, DD_PlusInfinity(ddman), DD_Constant(ddman, 0));
	
	// initial solution (below) is infinity in 'inf' states, lower elsewhere
	// note: ok to do this because cudd matrix-multiply (and other ops)
	// treat 0 * inf as 0, unlike in IEEE 754 rules
	Cudd_Ref(inf);
	Cudd_Ref(lower);
	sol_below = DD_ITE(ddman, inf, DD_PlusInfinity(ddman), lower);

	// initial solution (above) is infinity in 'inf' states, upper elsewhere
	// note: ok to do this because cudd matrix-multiply (and other ops)
	// treat 0 * inf as 0, unlike in IEEE 754 rules
	Cudd_Ref(inf);
	Cudd_Ref(upper);
	sol_above = DD_ITE(ddman, inf, DD_PlusInfinity(ddman), upper);

	// print memory usage
	i = DD_GetNumNodes(ddman, a);
	PM_PrintToMainLog(env, "[nodes=%d] [%.1f Kb]\n", i, i*20.0/1024.0);

	std::unique_ptr<ExportIterations> iterationExport;
	if (PM_GetFlagExportIterations()) {
		iterationExport.reset(new ExportIterations("PM_NondetReachReward (interval)"));
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
	PM_PrintToMainLog(env, "\nStarting iterations...\n");

	bool below_unchanged = false, above_unchanged = false;

	while (!done && iters < max_iters) {
		below_unchanged = above_unchanged = false;

		iters++;
		
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

		// add rewards
		Cudd_Ref(all_rewards);
		tmp_below = DD_Apply(ddman, APPLY_PLUS, tmp_below, all_rewards);
		Cudd_Ref(all_rewards);
		tmp_above = DD_Apply(ddman, APPLY_PLUS, tmp_above, all_rewards);

		// do min/max
		if (min) {
			// mask stuff
			Cudd_Ref(new_mask);
			tmp_below = DD_Apply(ddman, APPLY_MAX, tmp_below, new_mask);
			// abstract
			tmp_below = DD_MinAbstract(ddman, tmp_below, ndvars, num_ndvars);

			// mask stuff
			Cudd_Ref(new_mask);
			tmp_above = DD_Apply(ddman, APPLY_MAX, tmp_above, new_mask);
			// abstract
			tmp_above = DD_MinAbstract(ddman, tmp_above, ndvars, num_ndvars);
		}
		else {
			// abstract
			tmp_below = DD_MaxAbstract(ddman, tmp_below, ndvars, num_ndvars);

			// abstract
			tmp_above = DD_MaxAbstract(ddman, tmp_above, ndvars, num_ndvars);
		}
		
		// put infinities (for 'inf' states) back into into solution vectors
		Cudd_Ref(inf);
		tmp_below = DD_ITE(ddman, inf, DD_PlusInfinity(ddman), tmp_below);

		Cudd_Ref(inf);
		tmp_above = DD_ITE(ddman, inf, DD_PlusInfinity(ddman), tmp_above);

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
	PM_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);

	DdNode *result;
	if (helper.flag_select_midpoint() && done) { // we did converge, select midpoint
		Cudd_Ref(sol_below);
		Cudd_Ref(sol_above);

		// compute midpoint for result
		DdNode* difference = DD_Apply(ddman, APPLY_MINUS, sol_above, sol_below);
		difference = DD_Apply(ddman, APPLY_DIVIDE, difference, DD_Constant(ddman, 2.0));

		Cudd_Ref(sol_below);
		result = DD_Apply(ddman, APPLY_PLUS, sol_below, difference);
		// TODO: ensure that below <= result <= above?

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
	Cudd_RecursiveDeref(ddman, all_rewards);
	Cudd_RecursiveDeref(ddman, new_mask);
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
