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
#include <prism.h>
#include "sparse.h"
#include "PrismSparseGlob.h"
#include "jnipointer.h"
#include <new>

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_PrismSparse_PS_1StochCumulReward
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
	DdNode *tmp = NULL;
	// model stats
	int n;
	long nnz;
	// flags
	bool compact_tr, compact_d;
	// sparse matrix
	RMSparseMatrix *rmsm = NULL;
	CMSRSparseMatrix *cmsrsm = NULL;
	// vectors
	double *diags = NULL, *soln = NULL, *soln2 = NULL, *tmpsoln = NULL, *sum = NULL;
	DistVector *diags_dist = NULL;
	// fox glynn stuff
	FoxGlynnWeights fgw;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	bool done;
	int j, l, h;
	long i, iters, num_iters;
	double d, x, sup_norm, max_diag, weight, kb, kbt, unif, term_crit_param_unif;
	
	// exception handling around whole function
	try {
	
	// start clocks
	start1 = start2 = util_cpu_time();
	
	// get number of states
	n = odd->eoff + odd->toff;
	
	// build sparse matrix
	PS_PrintToMainLog(env, "\nBuilding sparse matrix... ");
	// if requested, try and build a "compact" version
	compact_tr = true;
	cmsrsm = NULL;
	if (compact) cmsrsm = build_cmsr_sparse_matrix(ddman, trans, rvars, cvars, num_rvars, odd);
	if (cmsrsm != NULL) {
		nnz = cmsrsm->nnz;
		kb = cmsrsm->mem;
	}
	// if not or if it wasn't possible, built a normal one
	else {
		compact_tr = false;
		rmsm = build_rm_sparse_matrix(ddman, trans, rvars, cvars, num_rvars, odd);
		nnz = rmsm->nnz;
		kb = rmsm->mem;
	}
	kbt = kb;
	// print some info
	PS_PrintToMainLog(env, "[n=%d, nnz=%d%s] ", n, nnz, compact_tr?", compact":"");
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// get vector of diagonals
	PS_PrintToMainLog(env, "Creating vector for diagonals... ");
	diags = compact_tr ? cmsr_negative_row_sums(cmsrsm) : rm_negative_row_sums(rmsm);
	// try and convert to compact form if required
	compact_d = false;
	if (compact) {
		if ((diags_dist = double_vector_to_dist(diags, n))) {
			compact_d = true;
			delete diags; diags = NULL;
		}
	}
	kb = (!compact_d) ? n*8.0/1024.0 : (diags_dist->num_dist*8.0+n*2.0)/1024.0;
	kbt += kb;
	if (compact_d) PS_PrintToMainLog(env, "[dist=%d, compact] ", diags_dist->num_dist);
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// find max diagonal element
	if (!compact_d) {
		max_diag = diags[0];
		for (i = 1; i < n; i++) if (diags[i] < max_diag) max_diag = diags[i];
	} else {
		max_diag = diags_dist->dist[0];
		for (i = 1; i < diags_dist->num_dist; i++) if (diags_dist->dist[i] < max_diag) max_diag = diags_dist->dist[i];
	}
	max_diag = -max_diag;
	
	// constant for uniformization
	unif = 1.02*max_diag;
	
	// modify diagonals
	if (!compact_d) {
		for (i = 0; i < n; i++) diags[i] = diags[i] / unif + 1;
	} else {
		for (i = 0; i < diags_dist->num_dist; i++) diags_dist->dist[i] = diags_dist->dist[i] / unif + 1;
	}
	
	// uniformization
	if (!compact_tr) {
		for (i = 0; i < nnz; i++) rmsm->non_zeros[i] /= unif;
	} else {
		for (i = 0; i < cmsrsm->dist_num; i++) cmsrsm->dist[i] /= unif;
	}
	
	// combine state/transition rewards into a single vector
	// new state rewards = c + (R.C)1
	// first, multiply transition rates by transition rewards and sum rows
	// = (R.C)1
	Cudd_Ref(trans);
	Cudd_Ref(trans_rewards);
	tmp = DD_Apply(ddman, APPLY_TIMES, trans, trans_rewards);
	tmp = DD_SumAbstract(ddman, tmp, cvars, num_cvars);
	// then add state rewards
	// = c + (R.C)1
	Cudd_Ref(state_rewards);
	tmp = DD_Apply(ddman, APPLY_PLUS, tmp, state_rewards);
	soln = mtbdd_to_double_vector(ddman, tmp, rvars, num_rvars, odd);
	Cudd_RecursiveDeref(ddman, tmp);
	
	// create solution/iteration vectors
	PS_PrintToMainLog(env, "Allocating iteration vectors... ");
	// soln has already been created and initialised to rewards vector as required
	// need to create soln2 and sum
	soln2 = new double[n];
	sum = new double[n];
	kb = n*8.0/1024.0;
	kbt += 3*kb;
	PS_PrintMemoryToMainLog(env, "[3 x ", kb, "]\n");
	
	// print total memory usage
	PS_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");
	
	// compute new termination criterion parameter (epsilon/8)
	term_crit_param_unif = term_crit_param / 8.0;
	
	// compute poisson probabilities (fox/glynn)
	PS_PrintToMainLog(env, "\nUniformisation: q.t = %f x %f = %f\n", unif, time, unif * time);
	fgw = fox_glynn(unif * time, 1.0e-300, 1.0e+300, term_crit_param_unif);
	if (fgw.right < 0) throw "Overflow in Fox-Glynn computation (time bound too big?)";
	for (i = fgw.left; i <= fgw.right; i++) {
		fgw.weights[i-fgw.left] /= fgw.total_weight;
	}
	PS_PrintToMainLog(env, "Fox-Glynn: left = %ld, right = %ld\n", fgw.left, fgw.right);
	
	// modify the poisson probabilities to what we need for this computation
	// first make the kth value equal to the sum of the values for 0...k
	for (i = fgw.left+1; i <= fgw.right; i++) {
		fgw.weights[i-fgw.left] += fgw.weights[i-1-fgw.left];
	}
	// then subtract from 1 and divide by uniformisation constant (q) to give mixed poisson probabilities
	for (i = fgw.left; i <= fgw.right; i++) {
		fgw.weights[i-fgw.left] = (1 - fgw.weights[i-fgw.left]) / unif;
	}
	
	// set up vectors
	for (i = 0; i < n; i++) {
		sum[i] = 0.0;
	}
	
	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;
	
	// start transient analysis
	done = false;
	num_iters = -1;
	PS_PrintToMainLog(env, "\nStarting iterations...\n");
	
	// do 0th element of summation (doesn't require any matrix powers)
	if (fgw.left == 0) {
		for (i = 0; i < n; i++) sum[i] += fgw.weights[0] * soln[i];
	} else {
		for (i = 0; i < n; i++) sum[i] += soln[i] / unif;
	}
	
	// note that we ignore max_iters as we know how any iterations _should_ be performed
	for (iters = 1; (iters <= fgw.right) && !done; iters++) {
		
		// store local copies of stuff
		double *non_zeros;
		unsigned char *row_counts;
		int *row_starts;
		bool use_counts;
		unsigned int *cols;
		double *dist;
		int dist_shift;
		int dist_mask;
		if (!compact_tr) {
			non_zeros = rmsm->non_zeros;
			row_counts = rmsm->row_counts;
			row_starts = (int *)rmsm->row_counts;
			use_counts = rmsm->use_counts;
			cols = rmsm->cols;
		} else {
			row_counts = cmsrsm->row_counts;
			row_starts = (int *)cmsrsm->row_counts;
			use_counts = cmsrsm->use_counts;
			cols = cmsrsm->cols;
			dist = cmsrsm->dist;
			dist_shift = cmsrsm->dist_shift;
			dist_mask = cmsrsm->dist_mask;
		}
		
		// do matrix vector multiply bit
		h = 0;
		for (i = 0; i < n; i++) {
			d = (!compact_d) ? (diags[i] * soln[i]) : (diags_dist->dist[diags_dist->ptrs[i]] * soln[i]);
			if (!use_counts) { l = row_starts[i]; h = row_starts[i+1]; }
			else { l = h; h += row_counts[i]; }
			// "row major" version
			if (!compact_tr) {
				for (j = l; j < h; j++) {
					d += non_zeros[j] * soln[cols[j]];
				}
			// "compact msr" version
			} else {
				for (j = l; j < h; j++) {
					d += dist[(int)(cols[j] & dist_mask)] * soln[(int)(cols[j] >> dist_shift)];
				}
			}
			// set vector element
			soln2[i] = d;
		}
		
		// check for steady state convergence
		if (do_ss_detect) {
			sup_norm = 0.0;
			for (i = 0; i < n; i++) {
				x = fabs(soln2[i] - soln[i]);
				if (term_crit == TERM_CRIT_RELATIVE) {
					x /= soln2[i];
				}
				if (x > sup_norm) sup_norm = x;
			}
			if (sup_norm < term_crit_param_unif) {
				done = true;
			}
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
			for (i = 0; i < n; i++) sum[i] += weight * soln2[i];
			
			PS_PrintToMainLog(env, "\nSteady state detected at iteration %ld\n", iters);
			num_iters = iters;
			break;
		}
		
		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PS_PrintToMainLog(env, "Iteration %d (of %d): ", iters, fgw.right);
			if (do_ss_detect) PS_PrintToMainLog(env, "max %sdiff=%f, ", (term_crit == TERM_CRIT_RELATIVE)?"relative ":"", sup_norm);
			PS_PrintToMainLog(env, "%.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
			start3 = util_cpu_time();
		}
		
		// prepare for next iteration
		tmpsoln = soln;
		soln = soln2;
		soln2 = tmpsoln;
		
		// add to sum
		if (iters < fgw.left) {
			for (i = 0; i < n; i++) sum[i] += soln[i] / unif;
		} else {
			for (i = 0; i < n; i++) sum[i] += fgw.weights[iters-fgw.left] * soln[i];
		}
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iters/timing info
	if (num_iters == -1) num_iters = fgw.right;
	PS_PrintToMainLog(env, "\nIterative method: %ld iterations in %.2f seconds (average %.6f, setup %.2f)\n", num_iters, time_taken, time_for_iters/num_iters, time_for_setup);
	
	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PS_SetErrorMessage("Out of memory");
		if (sum) delete[] sum;
		sum = 0;
	} catch (const char *err) {
		PS_SetErrorMessage(err);
		if (sum) delete sum;
		sum = 0;
	}
	
	// free memory
	if (rmsm) delete rmsm;
	if (cmsrsm) delete cmsrsm;
	if (diags) delete[] diags;
	if (diags_dist) delete diags_dist;
	if (soln) delete[] soln;
	if (soln2) delete[] soln2;
	
	return ptr_to_jlong(sum);
}

//------------------------------------------------------------------------------
