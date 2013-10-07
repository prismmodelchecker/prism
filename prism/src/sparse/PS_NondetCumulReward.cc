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

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_PrismSparse_PS_1NondetCumulReward
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer t,		// trans matrix
jlong __jlongpointer sr,	// state rewards
jlong __jlongpointer trr,	// transition rewards
jlong __jlongpointer od,	// odd
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer ndv,	// nondet vars
jint num_ndvars,
jint bound,					// time bound
jboolean min				// min or max probabilities (true = min, false = max)
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(t);				// trans matrix
	DdNode *state_rewards = jlong_to_DdNode(sr);	// state rewards
	DdNode *trans_rewards = jlong_to_DdNode(trr);	// transition rewards
	ODDNode *odd = jlong_to_ODDNode(od); 			// reachable states
	DdNode **rvars = jlong_to_DdNode_array(rv); 	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv); 	// col vars
	DdNode **ndvars = jlong_to_DdNode_array(ndv);	// nondet vars

	// model stats
	int n, nc, nc_r;
	long nnz, nnz_r;
	// sparse matrix
	NDSparseMatrix *ndsm = NULL, *ndsm_r = NULL;
	// vectors
	double *sr_vec = NULL, *soln = NULL, *soln2 = NULL, *tmpsoln = NULL;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, j, k, k_r, l1, h1, l2, h2, l2_r, h2_r, iters;
	double d1, d2, x, kb, kbt;
	bool first;
	
	// exception handling around whole function
	try {
	
	// start clocks	
	start1 = start2 = util_cpu_time();
	
	// get number of states
	n = odd->eoff + odd->toff;
	
	// build sparse matrix (probs)
	PS_PrintToMainLog(env, "\nBuilding sparse matrix (transitions)... ");
	ndsm = build_nd_sparse_matrix(ddman, trans, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	// get number of transitions/choices
	nnz = ndsm->nnz;
	nc = ndsm->nc;
	kb = (nnz*12.0+nc*4.0+n*4.0)/1024.0;
	kbt = kb;
	// print out info
	PS_PrintToMainLog(env, "[n=%d, nc=%d, nnz=%d, k=%d] ", n, nc, nnz, ndsm->k);
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// build sparse matrix (rewards)
	PS_PrintToMainLog(env, "Building sparse matrix (transition rewards)... ");
	ndsm_r = build_sub_nd_sparse_matrix(ddman, trans, trans_rewards, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
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
	
	// create solution/iteration vectors
	PS_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = new double[n];
	soln2 = new double[n];
	kb = n*8.0/1024.0;
	kbt += 2*kb;
	PS_PrintMemoryToMainLog(env, "[2 x ", kb, "]\n");

	// print total memory usage
	PS_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");

	// initial solution is zero
	for (i = 0; i < n; i++) {
		soln[i] = 0;
	}
	
	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;

	// start iterations
	PS_PrintToMainLog(env, "\nStarting iterations...\n");

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
		
	// note that we ignore max_iters as we know how any iterations _should_ be performed
	for (iters = 0; iters < bound; iters++) {
	
		// do matrix multiplication and min/max
		h1 = h2 = h2_r = 0;
		// loop through states
		for (i = 0; i < n; i++) {
			d1 = 0.0; // initial value doesn't matter
			first = true; // (because we also remember 'first')
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
				}
				first = false;
			}
			// set vector element
			soln2[i] = d1;
		}
		
		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PS_PrintToMainLog(env, "Iteration %d (of %d): ", iters, bound);
			PS_PrintToMainLog(env, "%.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
			start3 = util_cpu_time();
		}
		
		// prepare for next iteration
		tmpsoln = soln;
		soln = soln2;
		soln2 = tmpsoln;
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iterations/timing info
	PS_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PS_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}
	
	// free memory
	if (ndsm) delete ndsm;
	if (ndsm_r) delete ndsm_r;
	if (sr_vec) delete[] sr_vec;
	if (soln2) delete[] soln2;
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------
