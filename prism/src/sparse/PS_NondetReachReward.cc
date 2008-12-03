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

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_PrismSparse_PS_1NondetReachReward
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
jlong __jlongpointer ndv,	// nondet vars
jint num_ndvars,
jlong __jlongpointer g,	// 'goal' states
jlong __jlongpointer in,	// 'inf' states
jlong __jlongpointer m,	// 'maybe' states
jboolean min		// min or max probabilities (true = min, false = max)
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(t);		// trans matrix
	DdNode *state_rewards = jlong_to_DdNode(sr);	// state rewards
	DdNode *trans_rewards = jlong_to_DdNode(trr);	// transition rewards
	ODDNode *odd = jlong_to_ODDNode(od); 		// reachable states
	DdNode **rvars = jlong_to_DdNode_array(rv); 	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv); 	// col vars
	DdNode **ndvars = jlong_to_DdNode_array(ndv);	// nondet vars
	DdNode *goal = jlong_to_DdNode(g);		// 'goal' states
	DdNode *inf = jlong_to_DdNode(in); 		// 'inf' states
	DdNode *maybe = jlong_to_DdNode(m); 		// 'maybe' states

	// mtbdds
	DdNode *a;
	// model stats
	int n, nc, nc_r;
	long nnz, nnz_r;
	// sparse matrix
	NDSparseMatrix *ndsm, *ndsm_r;
	// vectors
	double *sr_vec, *soln, *soln2, *tmpsoln, *inf_vec;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// adversary stuff
	bool adv = false, adv_loop = false;
	FILE *fp_adv = NULL;
	int adv_l, adv_h;
	// misc
	int i, j, k, k_r, l1, h1, l2, h2, l2_r, h2_r, iters;
	double d1, d2, kb, kbt;
	bool done, first;
	
	// start clocks	
	start1 = start2 = util_cpu_time();
	
	// get number of states
	n = odd->eoff + odd->toff;
	
	// filter out rows (goal states and infinity states) from matrix
	Cudd_Ref(trans);
	Cudd_Ref(maybe);
	a = DD_Apply(ddman, APPLY_TIMES, trans, maybe);
	
	// also remove goal and infinity states from state rewards vector
	Cudd_Ref(state_rewards);
	Cudd_Ref(maybe);
	state_rewards = DD_Apply(ddman, APPLY_TIMES, state_rewards, maybe);
	
	// and from transition rewards matrix
	Cudd_Ref(trans_rewards);
	Cudd_Ref(maybe);
	trans_rewards = DD_Apply(ddman, APPLY_TIMES, trans_rewards, maybe);
	
	// build sparse matrix (probs)
	PS_PrintToMainLog(env, "\nBuilding sparse matrix (transitions)... ");
	ndsm = build_nd_sparse_matrix(ddman, a, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	// get number of transitions/choices
	nnz = ndsm->nnz;
	nc = ndsm->nc;
	// print out info
	PS_PrintToMainLog(env, "[n=%d, nc=%d, nnz=%d, k=%d] ", n, nc, nnz, ndsm->k);
	kb = (nnz*12.0+nc*4.0+n*4.0)/1024.0;
	kbt = kb;
	PS_PrintToMainLog(env, "[%.1f KB]\n", kb);
	
	// build sparse matrix (rewards)
	PS_PrintToMainLog(env, "Building sparse matrix (transition rewards)... ");
	ndsm_r = build_sub_nd_sparse_matrix(ddman, a, trans_rewards, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	// get number of transitions/choices
	nnz_r = ndsm_r->nnz;
	nc_r = ndsm_r->nc;
	// print out info
	PS_PrintToMainLog(env, "[n=%d, nc=%d, nnz=%d, k=%d] ", n, nc_r, nnz_r, ndsm_r->k);
	kb = (nnz_r*12.0+nc_r*4.0+n*4.0)/1024.0;
	kbt += kb;
	PS_PrintToMainLog(env, "[%.1f KB]\n", kb);
	
	// get vector for state rewards
	PS_PrintToMainLog(env, "Creating vector for state rewards... ");
	sr_vec = mtbdd_to_double_vector(ddman, state_rewards, rvars, num_rvars, odd);
	kb = n*8.0/1024.0;
	kbt += kb;
	PS_PrintToMainLog(env, "[%.1f KB]\n", kb);
	
	// create solution/iteration vectors
	PS_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = new double[n];
	soln2 = new double[n];
	kb = n*8.0/1024.0;
	kbt += 2*kb;
	PS_PrintToMainLog(env, "[2 x %.1f KB]\n", kb);

	// print total memory usage
	PS_PrintToMainLog(env, "TOTAL: [%.1f KB]\n", kbt);

	// initial solution is zero
	for (i = 0; i < n; i++) {
		soln[i] = 0;
	}

	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;

	// start iterations
	iters = 0;
	done = false;
	PS_PrintToMainLog(env, "\nStarting iterations...\n");

	// open file to store adversary (if required)
	if (adv) {
		fp_adv = fopen("adv.tra", "w");
		fprintf(fp_adv, "%d ?\n", n);
	}
	
	while ((!done && iters < max_iters) || adv_loop) {
	
		iters++;

//		PS_PrintToMainLog(env, "iter %d\n", iters);
//		start3 = util_cpu_time();

		// store local copies of stuff
		// firstly for transition matrix
		double *non_zeros = ndsm->non_zeros;
		unsigned char *row_counts = ndsm->row_counts;
		int *row_starts = (int *)ndsm->row_counts;
		unsigned char *choice_counts = ndsm->choice_counts;
		int *choice_starts = (int *)ndsm->choice_counts;
		bool use_counts = ndsm->use_counts;
		unsigned int *cols = ndsm->cols;
		// and then for transition rewards matrix
		// (note: we don't need row_counts/row_starts for
		// this since choice structure mirrors transition matrix)
		double *non_zeros_r = ndsm_r->non_zeros;
		unsigned char *choice_counts_r = ndsm_r->choice_counts;
		int *choice_starts_r = (int *)ndsm_r->choice_counts;
		bool use_counts_r = ndsm_r->use_counts;
		unsigned int *cols_r = ndsm_r->cols;
		
		// do matrix multiplication and min/max
		h1 = h2 = h2_r = 0;
		// loop through states
		for (i = 0; i < n; i++) {
			d1 = 0.0;
			first = true;
			// get pointers to nondeterministic choices for state i
			if (!use_counts) { l1 = row_starts[i]; h1 = row_starts[i+1]; }
			else { l1 = h1; h1 += row_counts[i]; }
			// loop through those choices
			for (j = l1; j < h1; j++) {
				// compute the reward value for state i for this iteration
				// start with state reward for this state
				d2 = sr_vec[i];
				// get pointers to transitions
				if (!use_counts) { l2 = choice_starts[j]; h2 = choice_starts[j+1]; }
				else { l2 = h2; h2 += choice_counts[j]; }
				// and get pointers to transition rewards
				if (!use_counts_r) { l2_r = choice_starts_r[j]; h2_r = choice_starts_r[j+1]; }
				else { l2_r = h2_r; h2_r += choice_counts_r[j]; }
				// loop through transitions
				for (k = l2; k < h2; k++) {
					// find corresponding transition reward if any
					k_r = l2_r; while (k_r < h2_r && cols_r[k_r] != cols[k]) k_r++;
					// if there is one, add reward * prob to reward value
					if (k_r < h2_r) { d2 += non_zeros_r[k_r] * non_zeros[k]; k_r++; }
					// add prob * corresponding reward from previous iteration
					d2 += non_zeros[k] * soln[cols[k]];
				}
				// see if this value is the min/max so far
				if (first || min&&(d2<d1) || !min&&(d2>d1)) {
					d1 = d2;
					if (adv_loop) { adv_l = l2; adv_h = h2; }
				}
				first = false;
			}
			// set vector element
			// (if there were no choices from this state, reward is zero)
			soln2[i] = (h1 > l1) ? d1 : 0;
			// store adversary info (if required)
			if (adv_loop) if (h1 > l1)
				for (k = adv_l; k < adv_h; k++) fprintf(fp_adv, "%d %d %g\n", i, cols[k], non_zeros[k]);
		}
		
		// check convergence
		// (note: doing outside loop means may not need to check all elements)
		switch (term_crit) {
		case TERM_CRIT_ABSOLUTE:
			done = true;
			for (i = 0; i < n; i++) {
				if (fabs(soln2[i] - soln[i]) > term_crit_param) {
					done = false;
					break;
				}
				
			}
			break;
		case TERM_CRIT_RELATIVE:
			done = true;
			for (i = 0; i < n; i++) {
				if (fabs(soln2[i] - soln[i])/soln2[i] > term_crit_param) {
					done = false;
					break;
				}
				
			}
			break;
		}
		
		// prepare for next iteration
		tmpsoln = soln;
		soln = soln2;
		soln2 = tmpsoln;
		
		// if we're done, but adversary generation is required, go round once more
		if (done && adv) adv_loop = !adv_loop;
		
//		PS_PrintToMainLog(env, "%.2f %.2f sec\n", ((double)(util_cpu_time() - start3)/1000), ((double)(util_cpu_time() - start2)/1000)/iters);
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iterations/timing info
	PS_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	// set reward for infinity states to infinity
	if (soln != NULL) {
		// first, generate vector for inf
		inf_vec = mtbdd_to_double_vector(ddman, inf, rvars, num_rvars, odd);
		// go thru setting elements of soln to infinity
		for (i = 0; i < n; i++) if (inf_vec[i] > 0) soln[i] = HUGE_VAL;
	}
	
	// close file to store adversary (if required)
	if (adv) {
		fclose(fp_adv);
	}
	
	// free memory
	Cudd_RecursiveDeref(ddman, a);
	Cudd_RecursiveDeref(ddman, state_rewards);
	Cudd_RecursiveDeref(ddman, trans_rewards);
	free_nd_sparse_matrix(ndsm);
	free_nd_sparse_matrix(ndsm_r);
	delete sr_vec;
	delete soln2;
	
	// if the iterative method didn't terminate, this is an error
	if (!done) { delete soln; PS_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters); return 0; }
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------
