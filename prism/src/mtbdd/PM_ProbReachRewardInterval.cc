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
#include "IntervalIteration.h"
#include "jnipointer.h"

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_mtbdd_PrismMTBDD_PM_1ProbReachRewardInterval
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer t,	// trans matrix
jlong __jlongpointer sr,	// state rewards
jlong __jlongpointer trr,	// transition rewards
jlong __jlongpointer od,	// odd
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer g,	// 'goal' states
jlong __jlongpointer in,	// 'inf' states
jlong __jlongpointer m,	// 'maybe' states
jlong __jlongpointer l,  // lower bound
jlong __jlongpointer u,   // upper bound
jint flags
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(t);		// trans matrix
	DdNode *state_rewards = jlong_to_DdNode(sr);	// state rewards
	DdNode *trans_rewards = jlong_to_DdNode(trr);	// transition rewards
	ODDNode *odd = jlong_to_ODDNode(od);		// odd
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars
	DdNode *goal = jlong_to_DdNode(g);		// 'goal' states
	DdNode *inf = jlong_to_DdNode(in);		// 'inf' states
	DdNode *maybe = jlong_to_DdNode(m);		// 'maybe' states
	DdNode *lower = jlong_to_DdNode(l);		// lower bound
	DdNode *upper = jlong_to_DdNode(u);		// upper bound

	// mtbdds
	DdNode *reach, *a, *sol, *tmp;
	
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

	IntervalIteration helper(flags);
	if (!helper.flag_ensure_monotonic_from_above()) {
		PM_PrintToMainLog(env, "Note: Interval iteration is configured to not enforce monotonicity from above.\n");
	}
	if (!helper.flag_ensure_monotonic_from_below()) {
		PM_PrintToMainLog(env, "Note: Interval iteration is configured to not enforce monotonicity from below.\n");
	}

	// call iterative method
	sol = NULL;
	switch (lin_eq_method) {
		case LIN_EQ_METHOD_POWER:
			sol = jlong_to_DdNode(Java_mtbdd_PrismMTBDD_PM_1PowerInterval(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(state_rewards), ptr_to_jlong(lower), ptr_to_jlong(upper), false, flags)); break;
		case LIN_EQ_METHOD_JACOBI:
			sol = jlong_to_DdNode(Java_mtbdd_PrismMTBDD_PM_1JORInterval(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(state_rewards), ptr_to_jlong(lower), ptr_to_jlong(upper), false, 1.0, flags)); break;
		case LIN_EQ_METHOD_JOR:
			sol = jlong_to_DdNode(Java_mtbdd_PrismMTBDD_PM_1JORInterval(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(state_rewards), ptr_to_jlong(lower), ptr_to_jlong(upper), false, lin_eq_method_param, flags)); break;
		default:
			PM_SetErrorMessage("Gauss-Seidel and its variants are currently not supported by the MTBDD engine"); return 0;
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
	
	return ptr_to_jlong(sol);
}

//------------------------------------------------------------------------------
