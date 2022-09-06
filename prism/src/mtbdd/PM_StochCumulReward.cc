//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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
#include <prism.h>
#include "PrismMTBDDGlob.h"
#include "jnipointer.h"

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_mtbdd_PrismMTBDD_PM_1StochCumulReward
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer tr,	// trans matrix
jlong __jlongpointer sr,	// state rewards
jlong __jlongpointer trr,	// transition rewards
jlong __jlongpointer od,	// odd
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jdouble time		// time bound
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(tr);		// trans matrix
	DdNode *state_rewards = jlong_to_DdNode(sr);	// state rewards
	DdNode *trans_rewards = jlong_to_DdNode(trr);	// transition rewards
	ODDNode *odd = jlong_to_ODDNode(od);		// odd
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars

	// mtbdds
	DdNode *reach, *diags, *q, *sol, *tmp, *sum;
	// model stats
//	int n;
	// fox glynn stuff
	FoxGlynnWeights fgw;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	bool done;
	long i, iters, num_iters;
	double max_diag, weight, unif, term_crit_param_unif;
	
	// start clocks
	start1 = start2 = util_cpu_time();
	
	// get number of states
//	n = odd->eoff + odd->toff;
	
	// get reachable states
	reach = odd->dd;
	
	// compute diagonals
	PM_PrintToMainLog(env, "\nComputing diagonals MTBDD... ");
	Cudd_Ref(trans);
	diags = DD_SumAbstract(ddman, trans, cvars, num_rvars);
	diags = DD_Apply(ddman, APPLY_TIMES, diags, DD_Constant(ddman, -1));
	i = DD_GetNumNodes(ddman, diags);
	PM_PrintToMainLog(env, "[nodes=%ld] [%.1f Kb]\n", i, i*20.0/1024.0);
	
	PM_PrintToMainLog(env, "Building iteration matrix MTBDD... ");
	
	// build generator matrix q from trans and diags
	// note that any self loops are effectively removed because we include their rates
	// in the 'diags' row sums and then subtract these from the original rate matrix
	// same applies in the "!combine" case below
	Cudd_Ref(trans);
	Cudd_Ref(diags);
	q = DD_Apply(ddman, APPLY_PLUS, trans, DD_Apply(ddman, APPLY_TIMES, DD_Identity(ddman, rvars, cvars, num_rvars), diags));
	
	// find max diagonal element
	max_diag = -DD_FindMin(ddman, diags);
	
	// constant for uniformization
	unif = 1.02*max_diag;
	
	// uniformization
	q = DD_Apply(ddman, APPLY_DIVIDE, q, DD_Constant(ddman, unif));
	Cudd_Ref(reach);
	q = DD_Apply(ddman, APPLY_PLUS, q, DD_Apply(ddman, APPLY_TIMES, DD_Identity(ddman, rvars, cvars, num_rvars), reach));
	i = DD_GetNumNodes(ddman, q);
	PM_PrintToMainLog(env, "[nodes=%ld] [%.1f Kb]\n", i, i*20.0/1024.0);
	
	// combine state/transition rewards into a single vector - this is the initial solution vector
	Cudd_Ref(trans);
	Cudd_Ref(trans_rewards);
	sol = DD_Apply(ddman, APPLY_TIMES, trans, trans_rewards);
	sol = DD_SumAbstract(ddman, sol, cvars, num_cvars);
	Cudd_Ref(state_rewards);
	sol = DD_Apply(ddman, APPLY_PLUS, sol, state_rewards);
	
	// set up sum vector
	sum = DD_Constant(ddman, 0);
	
	// compute new termination criterion parameter (epsilon/8)
	term_crit_param_unif = term_crit_param / 8.0;
	
	// compute poisson probabilities (fox/glynn)
	PM_PrintToMainLog(env, "\nUniformisation: q.t = %f x %f = %f\n", unif, time, unif * time);
	fgw = fox_glynn(unif * time, 1.0e-300, 1.0e+300, term_crit_param_unif);
	if (fgw.right < 0) {
		PM_SetErrorMessage("Overflow in Fox-Glynn computation (time bound too big?)");
		Cudd_RecursiveDeref(ddman, q);
		Cudd_RecursiveDeref(ddman, diags);
		Cudd_RecursiveDeref(ddman, sol);
		Cudd_RecursiveDeref(ddman, sum);
		return 0;
	}
	for (i = fgw.left; i <= fgw.right; i++) {
		fgw.weights[i-fgw.left] /= fgw.total_weight;
	}
	PM_PrintToMainLog(env, "Fox-Glynn: left = %ld, right = %ld\n", fgw.left, fgw.right);
	
	// modify the poisson probabilities to what we need for this computation
	// first make the kth value equal to the sum of the values for 0...k
	for (i = fgw.left+1; i <= fgw.right; i++) {
		fgw.weights[i-fgw.left] += fgw.weights[i-1-fgw.left];
	}
	// then subtract from 1 and divide by uniformisation constant (q) to give mixed poisson probabilities
	for (i = fgw.left; i <= fgw.right; i++) {
		fgw.weights[i-fgw.left] = (1 - fgw.weights[i-fgw.left]) / unif;
	}
	
	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;
	
	// start transient analysis
	done = false;
	num_iters = -1;
	PM_PrintToMainLog(env, "\nStarting iterations...\n");
	
	// do 0th element of summation (doesn't require any matrix powers)
	if (fgw.left == 0) {
		Cudd_Ref(sol);
		sum = DD_Apply(ddman, APPLY_PLUS, sum, DD_Apply(ddman, APPLY_TIMES, sol, DD_Constant(ddman, fgw.weights[0])));
	} else {
		Cudd_Ref(sol);
		sum = DD_Apply(ddman, APPLY_PLUS, sum, DD_Apply(ddman, APPLY_DIVIDE, sol, DD_Constant(ddman, unif)));
	}
	
	// note that we ignore max_iters as we know how any iterations _should_ be performed
	for (iters = 1; (iters <= fgw.right) && !done; iters++) {
		
		//matrix-vector multiply
		Cudd_Ref(sol);
		tmp = DD_PermuteVariables(ddman, sol, rvars, cvars, num_rvars);
		Cudd_Ref(q);
		tmp = DD_MatrixMultiply(ddman, q, tmp, cvars, num_cvars, MM_BOULDER);
		
		// check for steady state convergence
		if (do_ss_detect) switch (term_crit) {
		case TERM_CRIT_ABSOLUTE:
			if (DD_EqualSupNorm(ddman, tmp, sol, term_crit_param_unif)) {
				done = true;
			}
			break;
		case TERM_CRIT_RELATIVE:
			if (DD_EqualSupNormRel(ddman, tmp, sol, term_crit_param_unif)) {
				done = true;
			}
			break;
		}
		
		// special case when finished early (steady-state detected)
		if (done) {
			// work out sum of remaining poisson probabilities
			if (iters <= fgw.left) {
				weight = time - iters/unif;
			} else {
				weight = 0.0;
				for (i = iters; i <= fgw.right; i++) {
					weight += fgw.weights[i-fgw.left];
				}
			}
			// add to sum
			Cudd_Ref(tmp);
			sum = DD_Apply(ddman, APPLY_PLUS, sum, DD_Apply(ddman, APPLY_TIMES, tmp, DD_Constant(ddman, weight)));
			PM_PrintToMainLog(env, "\nSteady state detected at iteration %ld\n", iters);
			num_iters = iters;
			Cudd_RecursiveDeref(ddman, tmp);
			break;
		}
		
		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PM_PrintToMainLog(env, "Iteration %ld (of %ld): ", iters, fgw.right);
			PM_PrintToMainLog(env, "%.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
			start3 = util_cpu_time();
		}
		
		// prepare for next iteration
		Cudd_RecursiveDeref(ddman, sol);
		sol = tmp;
		
		// add to sum
		if (iters < fgw.left) {
			Cudd_Ref(sol);
			sum = DD_Apply(ddman, APPLY_PLUS, sum, DD_Apply(ddman, APPLY_DIVIDE, sol, DD_Constant(ddman, unif)));
		} else {
			Cudd_Ref(sol);
			sum = DD_Apply(ddman, APPLY_PLUS, sum, DD_Apply(ddman, APPLY_TIMES, sol, DD_Constant(ddman, fgw.weights[iters-fgw.left])));
		}
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iters/timing info
	if (num_iters == -1) num_iters = fgw.right;
	PM_PrintToMainLog(env, "\nIterative method: %ld iterations in %.2f seconds (average %.6f, setup %.2f)\n", num_iters, time_taken, time_for_iters/num_iters, time_for_setup);
	
	// free memory
	Cudd_RecursiveDeref(ddman, q);
	Cudd_RecursiveDeref(ddman, diags);
	Cudd_RecursiveDeref(ddman, sol);
	if (fgw.weights) delete[] fgw.weights;

	return ptr_to_jlong(sum);
}

//------------------------------------------------------------------------------
