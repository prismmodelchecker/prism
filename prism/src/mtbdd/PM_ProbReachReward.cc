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

JNIEXPORT jint JNICALL Java_mtbdd_PrismMTBDD_PM_1ProbReachReward
(
JNIEnv *env,
jclass cls,
jint t,			// trans matrix
jint sr,		// state rewards
jint trr,		// transition rewards
jint od,		// odd
jint rv,		// row vars
jint num_rvars,
jint cv,		// col vars
jint num_cvars,
jint g,			// 'goal' states
jint in,		// 'inf' states
jint m			// 'maybe' states
)
{
	// cast function parameters
	DdNode *trans = (DdNode *)t;	// trans matrix
	DdNode *state_rewards = (DdNode *)sr;	// state rewards
	DdNode *trans_rewards = (DdNode *)trr;	// transition rewards
	ODDNode *odd = (ODDNode *)od;	// odd
	DdNode **rvars = (DdNode **)rv; // row vars
	DdNode **cvars = (DdNode **)cv; // col vars
	DdNode *goal = (DdNode *)g;	// 'goal' states
	DdNode *inf = (DdNode *)in; 	// 'inf' states
	DdNode *maybe = (DdNode *)m; 	// 'maybe' states
	// mtbdds
	DdNode *reach, *a, *sol, *tmp;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, iters;
	bool done;
	
	// start clocks
	start1 = start2 = util_cpu_time();
	
	// get reachable states
	reach = odd->dd;
	
	// filter out rows (goal states and infinity states) from matrix
	Cudd_Ref(trans);
	Cudd_Ref(maybe);
	a = DD_Apply(ddman, APPLY_TIMES, trans, maybe);
	
	// take copy of state/trans rewards
	Cudd_Ref(state_rewards);
	Cudd_Ref(trans_rewards);
	
	// also remove goal and infinity states from state rewards vector
	Cudd_Ref(maybe);
	state_rewards = DD_Apply(ddman, APPLY_TIMES, state_rewards, maybe);
	
	// multiply transition rewards by transition probs and sum rows
	// (note also filters out unwanted states at the same time)
	Cudd_Ref(a);
	trans_rewards = DD_Apply(ddman, APPLY_TIMES, trans_rewards, a);
	trans_rewards = DD_SumAbstract(ddman, trans_rewards, cvars, num_cvars);
	
	// combine state and transition rewards
	Cudd_Ref(trans_rewards);
	state_rewards = DD_Apply(ddman, APPLY_PLUS, state_rewards, trans_rewards);
	
	// subtract a from identity (unless we are going to solve with the power method)
	if (lin_eq_method != LIN_EQ_METHOD_POWER) {
		tmp =  DD_Identity(ddman, rvars, cvars, num_rvars);
		Cudd_Ref(reach);
		tmp = DD_And(ddman, tmp, reach);
		a = DD_Apply(ddman, APPLY_MINUS, tmp, a);
	}
	
	// call iterative method
	sol = NULL;
	switch (lin_eq_method) {
		case LIN_EQ_METHOD_POWER:
			sol = (DdNode *)Java_mtbdd_PrismMTBDD_PM_1Power(env, cls, (jint)odd, (jint)rvars, num_rvars, (jint)cvars, num_cvars, (jint)a, (jint)state_rewards, (jint)state_rewards, false); break;
		case LIN_EQ_METHOD_JACOBI:
			sol = (DdNode *)Java_mtbdd_PrismMTBDD_PM_1JOR(env, cls, (jint)odd, (jint)rvars, num_rvars, (jint)cvars, num_cvars, (jint)a, (jint)state_rewards, (jint)state_rewards, false, 1.0); break;
		case LIN_EQ_METHOD_JOR:
			sol = (DdNode *)Java_mtbdd_PrismMTBDD_PM_1JOR(env, cls, (jint)odd, (jint)rvars, num_rvars, (jint)cvars, num_cvars, (jint)a, (jint)state_rewards, (jint)state_rewards, false, lin_eq_method_param); break;
	}
	
	// set reward for infinity states to infinity
	if (sol != NULL) {
		Cudd_Ref(inf);
		sol = DD_ITE(ddman, inf, DD_PlusInfinity(ddman), sol);
	}
	
	// free memory
	Cudd_RecursiveDeref(ddman, a);
	Cudd_RecursiveDeref(ddman, state_rewards);
	Cudd_RecursiveDeref(ddman, trans_rewards);
	
	return (int)sol;
}

//------------------------------------------------------------------------------
