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
#include "PrismSparse.h"
#include <math.h>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include <dv.h>
#include "sparse.h"
#include "PrismSparseGlob.h"
#include "jnipointer.h"

//------------------------------------------------------------------------------

JNIEXPORT jlong __pointer JNICALL Java_sparse_PrismSparse_PS_1ProbReachReward
(
JNIEnv *env,
jclass cls,
jlong __pointer t,	// trans matrix
jlong __pointer sr,	// state rewards
jlong __pointer trr,	// transition rewards
jlong __pointer od,	// odd
jlong __pointer rv,	// row vars
jint num_rvars,
jlong __pointer cv,	// col vars
jint num_cvars,
jlong __pointer g,	// 'goal' states
jlong __pointer in,	// 'inf' states
jlong __pointer m	// 'maybe' states
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(t);		// trans matrix
	DdNode *state_rewards = jlong_to_DdNode(sr);	// state rewards
	DdNode *trans_rewards = jlong_to_DdNode(trr);	// transition rewards
	ODDNode *odd = jlong_to_ODDNode(od); 		// reachable states
	DdNode **rvars = jlong_to_DdNode_array(rv); 	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv); 	// col vars
	DdNode *goal = jlong_to_DdNode(g);		// 'goal' states
	DdNode *inf = jlong_to_DdNode(in); 		// 'inf' states
	DdNode *maybe = jlong_to_DdNode(m); 		// 'maybe' states

	// mtbdds
	DdNode *reach, *a, *tmp;
	// model stats
	int n;
	// vectors
	double *soln, *inf_vec;
	// misc
	int i;
	
	// get number of states
	n = odd->eoff + odd->toff;
	
	// get reachable states
	reach = odd->dd;
	
	// filter out rows (goal states and infinity states) from matrix
	Cudd_Ref(trans);
	Cudd_Ref(maybe);
	a = DD_Apply(ddman, APPLY_TIMES, trans, maybe);
	
	// take copy of state/trans rewards
	Cudd_Ref(state_rewards);
	Cudd_Ref(trans_rewards);
	
	// remove goal and infinity states from state rewards vector
	Cudd_Ref(maybe);
	state_rewards = DD_Apply(ddman, APPLY_TIMES, state_rewards, maybe);
	
	// multiply transition rewards by transition probs and sum rows
	// (note also filters out unwanted states at the same time)
	Cudd_Ref(a);
	trans_rewards = DD_Apply(ddman, APPLY_TIMES, trans_rewards, a);
	trans_rewards = DD_SumAbstract(ddman, trans_rewards, cvars, num_cvars);
	
	// combine state and transition rewards and put in a vector
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
	soln = NULL;
	switch (lin_eq_method) {
		case LIN_EQ_METHOD_POWER:
			soln = jlong_to_double(Java_sparse_PrismSparse_PS_1Power(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(state_rewards), ptr_to_jlong(state_rewards), false)); break;
		case LIN_EQ_METHOD_JACOBI:
			soln = jlong_to_double(Java_sparse_PrismSparse_PS_1JOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(state_rewards), ptr_to_jlong(state_rewards), false, false, 1.0)); break;
		case LIN_EQ_METHOD_GAUSSSEIDEL:
			soln = jlong_to_double(Java_sparse_PrismSparse_PS_1SOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(state_rewards), ptr_to_jlong(state_rewards), false, false, 1.0, true)); break;
		case LIN_EQ_METHOD_BGAUSSSEIDEL:
			soln = jlong_to_double(Java_sparse_PrismSparse_PS_1SOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(state_rewards), ptr_to_jlong(state_rewards), false, false, 1.0, false)); break;
		case LIN_EQ_METHOD_JOR:
			soln = jlong_to_double(Java_sparse_PrismSparse_PS_1JOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(state_rewards), ptr_to_jlong(state_rewards), false, false, lin_eq_method_param)); break;
		case LIN_EQ_METHOD_SOR:
			soln = jlong_to_double(Java_sparse_PrismSparse_PS_1SOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(state_rewards), ptr_to_jlong(state_rewards), false, false, lin_eq_method_param, true)); break;
		case LIN_EQ_METHOD_BSOR:
			soln = jlong_to_double(Java_sparse_PrismSparse_PS_1SOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(state_rewards), ptr_to_jlong(state_rewards), false, false, lin_eq_method_param, false)); break;
	}
	
	// set reward for infinity states to infinity
	if (soln != NULL) {
		// first, generate vector for inf
		inf_vec = mtbdd_to_double_vector(ddman, inf, rvars, num_rvars, odd);
		// go thru setting elements of soln to infinity
		for (i = 0; i < n; i++) if (inf_vec[i] > 0) soln[i] = HUGE_VAL;
	}
	
	// free remaining memory
	Cudd_RecursiveDeref(ddman, a);
	Cudd_RecursiveDeref(ddman, state_rewards);
	Cudd_RecursiveDeref(ddman, trans_rewards);
	free(inf_vec);
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------
