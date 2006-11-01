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
#include "foxglynn.h"
#include "PrismMTBDDGlob.h"

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_mtbdd_PrismMTBDD_PM_1StochBoundedUntil
(
JNIEnv *env,
jclass cls,
jint tr,		// rate matrix
jint od,		// odd
jint rv,		// row vars
jint num_rvars,
jint cv,		// col vars
jint num_cvars,
jint ye,		// 'yes' states
jint ma,		// 'maybe' states
jdouble time,		// time bound
jint mu			// probs for multiplying
)
{
	// cast function parameters
	DdNode *trans = (DdNode *)tr;	// trans matrix
	ODDNode *odd = (ODDNode *)od;	// odd
	DdNode **rvars = (DdNode **)rv;	// row vars
	DdNode **cvars = (DdNode **)cv;	// col vars
	DdNode *yes = (DdNode *)ye;	// 'yes' states
	DdNode *maybe = (DdNode *)ma;	// 'maybe' states
	DdNode *mult = (DdNode *)mu;	// probs for multiplying
	// model stats
	int n;
	// mtbdds
	DdNode *reach, *diags, *q, *r, *d, *sol, *tmp, *tmp2, *sum;
	// fox glynn stuff
	FoxGlynnWeights fgw;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, iters, num_iters;
	double x, max_diag, weight, unif, term_crit_param_unif;
	bool done, combine;
	
	// METHOD 1 or METHOD 2? (combine rate matrix and diagonals or keep separate?)
	combine = true; // 1
//	combine = false; // 2
	
	// start clocks	
	start1 = start2 = util_cpu_time();
	
	// get number of states
	n = odd->eoff + odd->toff;
	
	// get reachable states
	reach = odd->dd;
	
	// count number of states to be made absorbing
	x = DD_GetNumMinterms(ddman, maybe, num_cvars);
	PM_PrintToMainLog(env, "\nNumber of non-absorbing states: %.0f of %d (%.1f%%)\n", x,  n, 100.0*x/n);
	
	// compute diagonals
	PM_PrintToMainLog(env, "\nComputing diagonals MTBDD... ");
	Cudd_Ref(trans);
	diags = DD_SumAbstract(ddman, trans, cvars, num_rvars);
	diags = DD_Apply(ddman, APPLY_TIMES, diags, DD_Constant(ddman, -1));
	i = DD_GetNumNodes(ddman, diags);
	PM_PrintToMainLog(env, "[nodes=%d] [%.1f Kb]\n", i, i*20.0/1024.0);
	
	if (combine) {
		
		PM_PrintToMainLog(env, "Building iteration matrix MTBDD... ");
		
		// METHOD 1 (combine rate matrix and diagonals)
		
		// build generator matrix q from trans and diags
		// note that any self loops are effectively removed because we include their rates
		// in the 'diags' row sums and then subtract these from the original rate matrix
		// same applies in the "!combine" case below
		Cudd_Ref(trans);
		Cudd_Ref(diags);
		q = DD_Apply(ddman, APPLY_PLUS, trans, DD_Apply(ddman, APPLY_TIMES, DD_Identity(ddman, rvars, cvars, num_rvars), diags));
//		PM_PrintToMainLog(env, "Q = %d %d %.0f\n", DD_GetNumNodes(ddman, q), DD_GetNumTerminals(ddman, q), DD_GetNumMinterms(ddman, q, num_rvars+num_cvars));
		
		// filter out rows
		Cudd_Ref(maybe);
		q = DD_Apply(ddman, APPLY_TIMES, q, maybe);
		
		// find max diagonal element
		Cudd_Ref(diags);
		Cudd_Ref(maybe);
		d = DD_Apply(ddman, APPLY_TIMES, diags, maybe);
		max_diag = -DD_FindMin(ddman, d);
		Cudd_RecursiveDeref(ddman, d);
		
		// constant for uniformization
		unif = 1.02*max_diag;
		
		// uniformization	
		q = DD_Apply(ddman, APPLY_DIVIDE, q, DD_Constant(ddman, unif));
		Cudd_Ref(reach);
		q = DD_Apply(ddman, APPLY_PLUS, q, DD_Apply(ddman, APPLY_TIMES, DD_Identity(ddman, rvars, cvars, num_rvars), reach));
//		PM_PrintToMainLog(env, "Q (final) = %d %d %.0f\n", DD_GetNumNodes(ddman, q), DD_GetNumTerminals(ddman, q), DD_GetNumMinterms(ddman, q, num_rvars+num_cvars));
		
		i = DD_GetNumNodes(ddman, q);
		PM_PrintToMainLog(env, "[nodes=%d] [%.1f Kb]\n", i, i*20.0/1024.0);
	}
	else {
		
		// METHOD 2 (keep rate matrix and diagonals separate)
		
		PM_PrintToMainLog(env, "Building iteration matrix MTBDD... ");
		
		// copy trans/diags
		Cudd_Ref(trans);
		r = trans;
		Cudd_Ref(diags);
		d = diags;
//		PM_PrintToMainLog(env, "r = %d %d %.0f\n", DD_GetNumNodes(ddman, r), DD_GetNumTerminals(ddman, r), DD_GetNumMinterms(ddman, r, num_rvars+num_cvars));
//		PM_PrintToMainLog(env, "diags = %d %d %.0f\n", DD_GetNumNodes(ddman, d), DD_GetNumTerminals(ddman, d), DD_GetNumMinterms(ddman, d, num_rvars));
		
		// filter out rows
		Cudd_Ref(maybe);
		r = DD_Apply(ddman, APPLY_TIMES, r, maybe);
		Cudd_Ref(maybe);
		d = DD_Apply(ddman, APPLY_TIMES, d, maybe);
		
		// find max diagonal element
		max_diag = -DD_FindMin(ddman, d);
		
		// constant for uniformization
		unif = 1.02*max_diag;
		
		// uniformization	
		r = DD_Apply(ddman, APPLY_DIVIDE, r, DD_Constant(ddman, unif));
		d = DD_Apply(ddman, APPLY_DIVIDE, d, DD_Constant(ddman, unif));
		Cudd_Ref(reach);
		d = DD_Apply(ddman, APPLY_PLUS, d, reach);
//		PM_PrintToMainLog(env, "r (final) = %d %d %.0f\n", DD_GetNumNodes(ddman, r), DD_GetNumTerminals(ddman, r), DD_GetNumMinterms(ddman, r, num_rvars+num_cvars));
//		PM_PrintToMainLog(env, "diags (final) = %d %d %.0f\n", DD_GetNumNodes(ddman, d), DD_GetNumTerminals(ddman, d), DD_GetNumMinterms(ddman, d, num_rvars));
		
		i = DD_GetNumNodes(ddman, r);
		PM_PrintToMainLog(env, "[nodes=%d] [%.1f Kb]\n", i, i*20.0/1024.0);
	}
	
	// compute new termination criterion parameter (epsilon/8)
	term_crit_param_unif = term_crit_param / 8.0;
	
	// compute poisson probabilities (fox/glynn)
	PM_PrintToMainLog(env, "\nUniformisation: q.t = %f x %f = %f\n", unif, time, unif * time);
	fgw = fox_glynn(unif * time, 1.0e-300, 1.0e+300, term_crit_param_unif);
	for (i = fgw.left; i <= fgw.right; i++) {
		fgw.weights[i-fgw.left] /= fgw.total_weight;
	}
	PM_PrintToMainLog(env, "Fox-Glynn: left = %d, right = %d\n", fgw.left, fgw.right);
	
//	PM_PrintToMainLog(env, "right-left = %d\n", fgw.right-fgw.left);
//	PM_PrintToMainLog(env, "total_weight = %f\n", fgw.total_weight);
//	for (int i = 0; i < (fgw.right-fgw.left+1); i++) {
//		PM_PrintToMainLog(env, "%.20f\n", fgw.weights[i]/fgw.total_weight);
//	}
	
	// set up vectors
	Cudd_Ref(yes);
	sol = yes;
	sum = DD_Constant(ddman, 0);
	
	// multiply initial solution by 'mult' probs
	if (mult != NULL) {
		Cudd_Ref(mult);
		sol = DD_Apply(ddman, APPLY_TIMES, sol, mult);
	}
	
	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	
	// start iterations
	done = false;
	num_iters = -1;
	PM_PrintToMainLog(env, "\nStarting iterations...\n");
//	PM_PrintToMainLog(env, "Iteration 0: (%d %d %.0f)", DD_GetNumNodes(ddman, sol), DD_GetNumTerminals(ddman, sol), DD_GetNumMinterms(ddman, sol, num_rvars));
//	PM_PrintToMainLog(env, " (%d %d %.0f)\n", DD_GetNumNodes(ddman, sum), DD_GetNumTerminals(ddman, sum), DD_GetNumMinterms(ddman, sum, num_rvars));
	
	// if necessary, do 0th element of summation (doesn't require any matrix powers)
	if (fgw.left == 0) {
		Cudd_Ref(sol);
		sum = DD_Apply(ddman, APPLY_PLUS, sum, DD_Apply(ddman, APPLY_TIMES, sol, DD_Constant(ddman, fgw.weights[0])));
	}
	
	// note that we ignore max_iters as we know how any iterations _should_ be performed
	for (iters = 1; (iters <= fgw.right) && !done; iters++) {
	
//		PM_PrintToMainLog(env, "Iteration %d: ", iters);
//		start3 = util_cpu_time();
		
		if (combine) {
			
			// METHOD 1 (combine rate matrix and diagonals)
			
			//matrix-vector multiply
			Cudd_Ref(sol);
			tmp = DD_PermuteVariables(ddman, sol, rvars, cvars, num_rvars);
			Cudd_Ref(q);
			tmp = DD_MatrixMultiply(ddman, q, tmp, cvars, num_cvars, MM_BOULDER);
		
		}
		else {
		
			// METHOD 2 (combine rate matrix and diagonals)
			
			//matrix-vector multiply
			Cudd_Ref(sol);
			Cudd_Ref(d);
			tmp2 = DD_Apply(ddman, APPLY_TIMES, sol, d);
			Cudd_Ref(sol);
			tmp = DD_PermuteVariables(ddman, sol, rvars, cvars, num_rvars);
			Cudd_Ref(r);
			tmp = DD_MatrixMultiply(ddman, r, tmp, cvars, num_cvars, MM_BOULDER);
			tmp = DD_Apply(ddman, APPLY_PLUS, tmp, tmp2);
		
		}
		
//		PM_PrintToMainLog(env, "(%d %d %.0f) ", DD_GetNumNodes(ddman, sol), DD_GetNumTerminals(ddman, sol), DD_GetNumMinterms(ddman, sol, num_rvars));
//		PM_PrintToMainLog(env, "(%d %d %.0f)\n", DD_GetNumNodes(ddman, sum), DD_GetNumTerminals(ddman, sum), DD_GetNumMinterms(ddman, sum, num_rvars));
		
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
				weight = 1.0;
			} else {
				weight = 0.0;
				for (i = iters; i <= fgw.right; i++) {
					weight += fgw.weights[i-fgw.left];
				}
			}
			// add to sum
			Cudd_Ref(tmp);
			sum = DD_Apply(ddman, APPLY_PLUS, sum, DD_Apply(ddman, APPLY_TIMES, tmp, DD_Constant(ddman, weight)));
			PM_PrintToMainLog(env, "\nSteady state detected at iteration %d\n", iters);
			num_iters = iters;
			Cudd_RecursiveDeref(ddman, tmp);
			break;
		}
		
		// prepare for next iteration
		Cudd_RecursiveDeref(ddman, sol);
		sol = tmp;
		
		// add to sum
		if (iters >= fgw.left) {
			Cudd_Ref(sol);
			sum = DD_Apply(ddman, APPLY_PLUS, sum, DD_Apply(ddman, APPLY_TIMES, sol, DD_Constant(ddman, fgw.weights[iters-fgw.left])));
		}
		
//		PM_PrintToMainLog(env, "%.2f %.2f sec\n", ((double)(util_cpu_time() - start3)/1000), ((double)(util_cpu_time() - start2)/1000)/iters);
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iterations/timing info
	if (num_iters == -1) num_iters = fgw.right;
	PM_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", num_iters, time_taken, time_for_iters/num_iters, time_for_setup);
	
	// free memory
	if (combine) {
		// METHOD 1
		Cudd_RecursiveDeref(ddman, q);
	}
	else {
		// METHOD 2
		Cudd_RecursiveDeref(ddman, r);
		Cudd_RecursiveDeref(ddman, d);
	}
	Cudd_RecursiveDeref(ddman, diags);
	Cudd_RecursiveDeref(ddman, sol);
	
	return (int)sum;
}

//------------------------------------------------------------------------------
