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
#include "prism.h"
#include "PrismNativeGlob.h"
#include "PrismSparseGlob.h"
#include "jnipointer.h"
#include <new>

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_PrismSparse_PS_1NondetReachReward
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer t,		// trans matrix
jlong __jlongpointer ta,	// trans action labels
jobject synchs,
jlong __jlongpointer sr,	// state rewards
jlong __jlongpointer trr,	// transition rewards
jlong __jlongpointer od,	// odd
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer ndv,	// nondet vars
jint num_ndvars,
jlong __jlongpointer g,		// 'goal' states
jlong __jlongpointer in,	// 'inf' states
jlong __jlongpointer m,		// 'maybe' states
jboolean min				// min or max probabilities (true = min, false = max)
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(t);				// trans matrix
	DdNode *trans_actions = jlong_to_DdNode(ta);	// trans action labels
	DdNode *state_rewards = jlong_to_DdNode(sr);	// state rewards
	DdNode *trans_rewards = jlong_to_DdNode(trr);	// transition rewards
	ODDNode *odd = jlong_to_ODDNode(od); 			// reachable states
	DdNode **rvars = jlong_to_DdNode_array(rv); 	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv); 	// col vars
	DdNode **ndvars = jlong_to_DdNode_array(ndv);	// nondet vars
	DdNode *goal = jlong_to_DdNode(g);				// 'goal' states
	DdNode *inf = jlong_to_DdNode(in); 				// 'inf' states
	DdNode *maybe = jlong_to_DdNode(m); 			// 'maybe' states

	// mtbdds
	DdNode *a, *tmp = NULL;
	// model stats
	int n, nc, nc_r;
	long nnz, nnz_r;
	// sparse matrix
	NDSparseMatrix *ndsm = NULL, *ndsm_r = NULL;
	// vectors
	double *sr_vec = NULL, *soln = NULL, *soln2 = NULL, *tmpsoln = NULL, *inf_vec = NULL;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// adversary stuff
	int export_adv_enabled = export_adv;
	bool adv_loop = false;
	FILE *fp_adv = NULL;
	int adv_j;
	bool adv_new;
	int *adv = NULL;
	// action info
	jstring *action_names_jstrings;
	const char** action_names = NULL;
	int num_actions;
	// misc
	int i, j, k, k_r, l1, h1, l2, h2, l2_r, h2_r, iters;
	double d1, d2, x, sup_norm, kb, kbt;
	bool done, first;
	
	// exception handling around whole function
	try {
	
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
	kb = (nnz*12.0+nc*4.0+n*4.0)/1024.0;
	kbt = kb;
	// print out info
	PS_PrintToMainLog(env, "[n=%d, nc=%d, nnz=%d, k=%d] ", n, nc, nnz, ndsm->k);
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// if needed, and if info is available, build a vector of action indices for the MDP
	if (export_adv_enabled != EXPORT_ADV_NONE) {
		if (trans_actions != NULL) {
			PS_PrintToMainLog(env, "Building action information... ");
			// first need to filter out unwanted rows
			Cudd_Ref(trans_actions);
			Cudd_Ref(maybe);
			tmp = DD_Apply(ddman, APPLY_TIMES, trans_actions, maybe);
			// then convert to a vector of integer indices
			build_nd_action_vector(ddman, a, tmp, ndsm, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
			Cudd_RecursiveDeref(ddman, tmp);
			kb = n*4.0/1024.0;
			kbt += kb;
			PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
			// also extract list of action names from 'synchs'
			get_string_array_from_java(env, synchs, action_names_jstrings, action_names, num_actions);
		} else {
			PS_PrintWarningToMainLog(env, "Action labels are not available for adversary generation.", export_adv_filename);
		}
	}
	
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
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// get vector for state rewards
	PS_PrintToMainLog(env, "Creating vector for state rewards... ");
	sr_vec = mtbdd_to_double_vector(ddman, state_rewards, rvars, num_rvars, odd);
	kb = n*8.0/1024.0;
	kbt += kb;
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// get vector for yes
	PS_PrintToMainLog(env, "Creating vector for inf... ");
	inf_vec = mtbdd_to_double_vector(ddman, inf, rvars, num_rvars, odd);
	kb = n*8.0/1024.0;
	kbt += kb;
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// create solution/iteration vectors
	PS_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = new double[n];
	soln2 = new double[n];
	kb = n*8.0/1024.0;
	kbt += 2*kb;
	PS_PrintMemoryToMainLog(env, "[2 x ", kb, "]\n");

	// if required, create storage for adversary and initialise
	if (export_adv_enabled != EXPORT_ADV_NONE) {
		PS_PrintToMainLog(env, "Allocating adversary vector... ");
		adv = new int[n];
		kb = n*sizeof(int)/1024.0;
		kbt += kb;
		PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
		// Initialise all entries to -1 ("don't know")
		for (i = 0; i < n; i++) {
			adv[i] = -1;
		}
	}
	
	// print total memory usage
	PS_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");

	// initial solution is infinity in 'inf' states, zero elsewhere
	for (i = 0; i < n; i++) {
		soln[i] = (inf_vec[i] > 0) ? HUGE_VAL : 0.0;
	}

	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;

	// start iterations
	iters = 0;
	done = false;
	PS_PrintToMainLog(env, "\nStarting iterations...\n");

	// open file to store adversary (if required)
	if (export_adv_enabled != EXPORT_ADV_NONE) {
		fp_adv = fopen(export_adv_filename, "w");
		if (fp_adv) {
			fprintf(fp_adv, "%d ?\n", n);
		} else {
			PS_PrintWarningToMainLog(env, "Adversary generation cancelled (could not open file \"%s\").", export_adv_filename);
			export_adv_enabled = EXPORT_ADV_NONE;
		}
	}
	
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
		
	while (!done && iters < max_iters) {
	
		iters++;

		// do matrix multiplication and min/max
		h1 = h2 = h2_r = 0;
		// loop through states
		for (i = 0; i < n; i++) {
			d1 = 0.0; // initial value doesn't matter
			first = true; // (because we also remember 'first')
			adv_new = false;
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
				if (first || (min&&(d2<d1)) || (!min&&(d2>d1))) {
					d1 = d2;
					// if adversary generation is enabled, remember optimal choice
					if (export_adv_enabled != EXPORT_ADV_NONE) {
						// for max, only remember strictly better choices
						// (this resolves problems with end components)
						if (!min) {
							if (adv[i] == -1 || (d1>soln[i])) {
								adv[i] = j;
							}
						}
						// for min, this is straightforward
						// (in fact, could do it at the end of value iteration, but we don't)
						else {
							adv[i] = j;
						}
					}
				}
				first = false;
			}
			// set vector element
			// (if there were no choices from this state, reward is zero/infinity)
			soln2[i] = (h1 > l1) ? d1 : inf_vec[i] > 0 ? HUGE_VAL : 0;
		}
		
		// check convergence
		sup_norm = 0.0;
		for (i = 0; i < n; i++) {
			x = fabs(soln2[i] - soln[i]);
			if (term_crit == TERM_CRIT_RELATIVE) {
				x /= soln2[i];
			}
			if (x > sup_norm) sup_norm = x;
		}
		if (sup_norm < term_crit_param) {
			done = true;
		}
		
		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PS_PrintToMainLog(env, "Iteration %d: max %sdiff=%f", iters, (term_crit == TERM_CRIT_RELATIVE)?"relative ":"", sup_norm);
			PS_PrintToMainLog(env, ", %.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
			start3 = util_cpu_time();
		}
		
		// prepare for next iteration
		tmpsoln = soln;
		soln = soln2;
		soln2 = tmpsoln;
		
		// if we're done, but adversary generation is required, go round once more
		if (done && adv) adv_loop = !adv_loop;
	}
	
	// Traverse matrix to extract adversary
	if (export_adv_enabled != EXPORT_ADV_NONE) {
		h1 = h2 = 0;
		for (i = 0; i < n; i++) {
			if (!use_counts) { l1 = row_starts[i]; h1 = row_starts[i+1]; }
			else { l1 = h1; h1 += row_counts[i]; }
			// Have to loop through all choices (to compute offsets)
			for (j = l1; j < h1; j++) {
				if (!use_counts) { l2 = choice_starts[j]; h2 = choice_starts[j+1]; }
				else { l2 = h2; h2 += choice_counts[j]; }
				// But only output a choice if it is in the adversary
				if (j == adv[i]) {
					for (k = l2; k < h2; k++) {
						switch (export_adv_enabled) {
						case EXPORT_ADV_DTMC:
							fprintf(fp_adv, "%d %d %g", i, cols[k], non_zeros[k]); break;
						case EXPORT_ADV_MDP:
							fprintf(fp_adv, "%d 0 %d %g", i, cols[k], non_zeros[k]); break;
						}
						if (ndsm->actions != NULL) fprintf(fp_adv, " %s", ndsm->actions[j]>0?action_names[ndsm->actions[j]-1]:"");
						fprintf(fp_adv, "\n");
					}
				}
			}
		}
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iterations/timing info
	PS_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	// if the iterative method didn't terminate, this is an error
	if (!done) { delete soln; soln = NULL; PS_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters); }
	
	// close file to store adversary (if required)
	if (export_adv_enabled != EXPORT_ADV_NONE) {
		fclose(fp_adv);
		PS_PrintToMainLog(env, "\nAdversary written to file \"%s\".\n", export_adv_filename);
	}
	
	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PS_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}
	
	// free memory
	if (a) Cudd_RecursiveDeref(ddman, a);
	if (state_rewards) Cudd_RecursiveDeref(ddman, state_rewards);
	if (trans_rewards) Cudd_RecursiveDeref(ddman, trans_rewards);
	if (ndsm) delete ndsm;
	if (ndsm_r) delete ndsm_r;
	if (inf_vec) delete[] inf_vec;
	if (sr_vec) delete[] sr_vec;
	if (soln2) delete[] soln2;
	if (adv) delete[] adv;
	if (action_names != NULL) {
		release_string_array_from_java(env, action_names_jstrings, action_names, num_actions);
	}
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------
