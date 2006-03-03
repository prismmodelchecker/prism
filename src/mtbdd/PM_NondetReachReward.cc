//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker
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

JNIEXPORT jint JNICALL Java_mtbdd_PrismMTBDD_PM_1NondetReachReward
(
JNIEnv *env,
jclass cls,
jint t,			// trans matrix
jint sr,		// state rewards
jint trr,		// state rewards
jint od,		// odd
jint ndm,		// nondeterminism mask
jint rv,		// row vars
jint num_rvars,
jint cv,		// col vars
jint num_cvars,
jint ndv,		// nondet vars
jint num_ndvars,
jint g,			// 'goal' states
jint in,		// 'inf' states
jint m,			// 'maybe' states
jboolean min	// min or max probabilities (true = min, false = max)
)
{
	// cast function parameters
	DdNode *trans = (DdNode *)t;		// trans matrix
	DdNode *state_rewards = (DdNode *)sr;	// state rewards
	DdNode *trans_rewards = (DdNode *)trr;	// transition rewards
	ODDNode *odd = (ODDNode *)od;		// odd
	DdNode *mask = (DdNode *)ndm;		// nondeterminism mask
	DdNode **rvars = (DdNode **)rv;		// row vars
	DdNode **cvars = (DdNode **)cv;		// col vars
	DdNode **ndvars = (DdNode **)ndv;	// nondet vars
	DdNode *goal = (DdNode *)g;			// 'goal' states
	DdNode *inf = (DdNode *)in; 		// 'inf' states
	DdNode *maybe = (DdNode *)m; 	// 'maybe' states
	// mtbdds
	DdNode *reach, *a, *all_rewards, *new_mask, *sol, *tmp;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	bool done;
	int iters, i;
	
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
	
	// initial solution is zero
	sol = DD_Constant(ddman, 0);
	
	// print memory usage
	i = DD_GetNumNodes(ddman, a);
	PM_PrintToMainLog(env, "[nodes=%d] [%.1f Kb]\n", i, i*20.0/1024.0);
	
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
		
		// matrix-vector multiply
		Cudd_Ref(sol);
		tmp = DD_PermuteVariables(ddman, sol, rvars, cvars, num_rvars);
		Cudd_Ref(a);
		tmp = DD_MatrixMultiply(ddman, a, tmp, cvars, num_cvars, MM_BOULDER);
		
		// add rewards
		Cudd_Ref(all_rewards);
		tmp = DD_Apply(ddman, APPLY_PLUS, tmp, all_rewards);
		
		// do min/max
		if (min) {
			// mask stuff
			Cudd_Ref(new_mask);
			tmp = DD_Apply(ddman, APPLY_MAX, tmp, new_mask);
			// abstract
			tmp = DD_MinAbstract(ddman, tmp, ndvars, num_ndvars);
		}
		else {
			// abstract
			tmp = DD_MaxAbstract(ddman, tmp, ndvars, num_ndvars);
		}
		
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
	
	// set reward for infinity states to infinity
	Cudd_Ref(inf);
	sol = DD_ITE(ddman, inf, DD_PlusInfinity(ddman), sol);
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iterations/timing info
	if (!done) PM_PrintToMainLog(env, "\nWarning: Iterative method stopped early at %d iterations.\n", iters);
	PM_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	// free memory
	Cudd_RecursiveDeref(ddman, a);
	Cudd_RecursiveDeref(ddman, all_rewards);
	Cudd_RecursiveDeref(ddman, new_mask);
	
	return (int)sol;
}

//------------------------------------------------------------------------------
