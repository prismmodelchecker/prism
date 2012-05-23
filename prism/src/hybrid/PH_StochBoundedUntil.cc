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
#include "PrismHybrid.h"
#include <math.h>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include <dv.h>
#include <prism.h>
#include "sparse.h"
#include "hybrid.h"
#include "PrismHybridGlob.h"
#include "jnipointer.h"
#include <new>

// local prototypes
static void mult_rec(HDDNode *hdd, int level, int row_offset, int col_offset);
static void mult_cm(CMSparseMatrix *cmsm, int row_offset, int col_offset);
static void mult_cmsc(CMSCSparseMatrix *cmscsm, int row_offset, int col_offset);

// globals (used by local functions)
static HDDNode *zero;
static int num_levels;
static bool compact_sm;
static double *sm_dist;
static int sm_dist_shift;
static int sm_dist_mask;
static double *soln = NULL, *soln2 = NULL;
static double unif;

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_hybrid_PrismHybrid_PH_1StochBoundedUntil
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer tr,	// trans matrix
jlong __jlongpointer od,	// odd
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer ye,	// 'yes' states
jlong __jlongpointer ma,	// 'maybe' states
jdouble time,		// time bound
jlong __jlongpointer mu	// probs for multiplying
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(tr);		// trans matrix
	ODDNode *odd = jlong_to_ODDNode(od);		// odd
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars
	DdNode *yes = jlong_to_DdNode(ye);		// 'yes' states
	DdNode *maybe = jlong_to_DdNode(ma);		// 'maybe' states
	double *mult = jlong_to_double(mu);		// probs for multiplying

	// model stats
	int n;
	// flags
	bool compact_d;
	// matrix mtbdd
	DdNode *r = NULL;
	HDDMatrix *hddm = NULL;
	HDDNode *hdd = NULL;
	// vectors
	double *diags = NULL, *tmpsoln = NULL, *sum = NULL;
	DistVector *diags_dist = NULL;
	// fox glynn stuff
	FoxGlynnWeights fgw;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	bool done;
	long i, iters, num_iters;
	double x, sup_norm, kb, kbt, max_diag, weight, term_crit_param_unif;
	
	// exception handling around whole function
	try {
	
	// start clocks	
	start1 = start2 = util_cpu_time();
	
	// get number of states from odd
	n = odd->eoff + odd->toff;
	
	// count number of states to be made absorbing
	x = DD_GetNumMinterms(ddman, maybe, num_rvars);
	PH_PrintToMainLog(env, "\nNumber of non-absorbing states: %.0f of %d (%.1f%%)\n", x,  n, 100.0*(x/n));
	
	// filter out rows from rate matrix
	Cudd_Ref(trans);
	Cudd_Ref(maybe);
	r = DD_Apply(ddman, APPLY_TIMES, trans, maybe);
	
	// build hdd for matrix
	PH_PrintToMainLog(env, "\nBuilding hybrid MTBDD matrix... ");
	hddm = build_hdd_matrix(r, rvars, cvars, num_rvars, odd, false);
	hdd = hddm->top;
	zero = hddm->zero;
	num_levels = hddm->num_levels;
	kb = hddm->mem_nodes;
	kbt = kb;
	PH_PrintToMainLog(env, "[levels=%d, nodes=%d] ", hddm->num_levels, hddm->num_nodes);
	PH_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// add sparse matrices
	PH_PrintToMainLog(env, "Adding explicit sparse matrices... ");
	add_sparse_matrices(hddm, compact, false);
	compact_sm = hddm->compact_sm;
	if (compact_sm) {
		sm_dist = hddm->dist;
		sm_dist_shift = hddm->dist_shift;
		sm_dist_mask = hddm->dist_mask;
	}
	kb = hddm->mem_sm;
	kbt += kb;
	PH_PrintToMainLog(env, "[levels=%d, num=%d%s] ", hddm->l_sm, hddm->num_sm, compact_sm?", compact":"");
	PH_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// get vector of diagonals
	PH_PrintToMainLog(env, "Creating vector for diagonals... ");
	diags = hdd_negative_row_sums(hddm, n);
	compact_d = false;
	// try and convert to compact form if required
	if (compact) {
		if ((diags_dist = double_vector_to_dist(diags, n))) {
			compact_d = true;
			delete[] diags; diags = NULL;
		}
	}
	kb = (!compact_d) ? n*8.0/1024.0 : (diags_dist->num_dist*8.0+n*2.0)/1024.0;
	kbt += kb;
	if (compact_d) PH_PrintToMainLog(env, "[dist=%d, compact] ", diags_dist->num_dist);
	PH_PrintMemoryToMainLog(env, "[", kb, "]\n");
	//for(i = 0; i < n; i++) printf("%f ", (!compact_d)?(diags[i]):(diags_dist->dist[diags_dist->ptrs[i]])); printf("\n");
	
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
	last_unif = unif;
	
	// modify diagonals
	if (!compact_d) {
		for (i = 0; i < n; i++) diags[i] = diags[i] / unif + 1;
	} else {
		for (i = 0; i < diags_dist->num_dist; i++) diags_dist->dist[i] = diags_dist->dist[i] / unif + 1;
	}
	
	// create solution/iteration vectors
	PH_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = mtbdd_to_double_vector(ddman, yes, rvars, num_rvars, odd);
	soln2 = new double[n];
	sum = new double[n];
	kb = n*8.0/1024.0;
	kbt += 3*kb;
	PH_PrintMemoryToMainLog(env, "[3 x ", kb, "]\n");
	
	// multiply initial solution by 'mult' probs
	if (mult != NULL) {
		for (i = 0; i < n; i++) {
			soln[i] *= mult[i];
		}
	}
	
	// print total memory usage
	PH_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");
	
	// compute new termination criterion parameter (epsilon/8)
	term_crit_param_unif = term_crit_param / 8.0;
	
	// compute poisson probabilities (fox/glynn)
	PH_PrintToMainLog(env, "\nUniformisation: q.t = %f x %f = %f\n", unif, time, unif * time);
	fgw = fox_glynn(unif * time, 1.0e-300, 1.0e+300, term_crit_param_unif);
	if (fgw.right < 0) throw "Overflow in Fox-Glynn computation (time bound too big?)";
	for (i = fgw.left; i <= fgw.right; i++) {
		fgw.weights[i-fgw.left] /= fgw.total_weight;
	}
	PH_PrintToMainLog(env, "Fox-Glynn: left = %ld, right = %ld\n", fgw.left, fgw.right);
	
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
	PH_PrintToMainLog(env, "\nStarting iterations...\n");
	
	// if necessary, do 0th element of summation (doesn't require any matrix powers)
	if (fgw.left == 0) for (i = 0; i < n; i++) {
		sum[i] += fgw.weights[0] * soln[i];
	}
	
	// note that we ignore max_iters as we know how any iterations _should_ be performed
	for (iters = 1; (iters <= fgw.right) && !done; iters++) {
		
		// initialise vector
		if (!compact_d) {
			for (i = 0; i < n; i++) soln2[i] = diags[i] * soln[i];
		} else {
			for (i = 0; i < n; i++) soln2[i] = diags_dist->dist[diags_dist->ptrs[i]] * soln[i];
		}
		
		// do matrix vector multiply bit
		mult_rec(hdd, 0, 0, 0);
		
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
				weight = 1.0;
			} else {
				weight = 0.0;
				for (i = iters; i <= fgw.right; i++) {
					weight += fgw.weights[i-fgw.left];
				}
			}
			// add to sum
			for (i = 0; i < n; i++) sum[i] += weight * soln2[i];
			PH_PrintToMainLog(env, "\nSteady state detected at iteration %ld\n", iters);
			num_iters = iters;
			break;
		}
		
		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PH_PrintToMainLog(env, "Iteration %d (of %d): ", iters, fgw.right);
			if (do_ss_detect) PH_PrintToMainLog(env, "max %sdiff=%f, ", (term_crit == TERM_CRIT_RELATIVE)?"relative ":"", sup_norm);
			PH_PrintToMainLog(env, "%.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
			start3 = util_cpu_time();
		}
		
		// prepare for next iteration
		tmpsoln = soln;
		soln = soln2;
		soln2 = tmpsoln;
		
		// add to sum
		if (iters >= fgw.left) {
			for (i = 0; i < n; i++) sum[i] += fgw.weights[iters-fgw.left] * soln[i];
		}
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iters/timing info
	if (num_iters == -1) num_iters = fgw.right;
	PH_PrintToMainLog(env, "\nIterative method: %ld iterations in %.2f seconds (average %.6f, setup %.2f)\n", num_iters, time_taken, time_for_iters/num_iters, time_for_setup);
	
	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PH_SetErrorMessage("Out of memory");
		if (sum) delete[] sum;
		sum = 0;
	} catch (const char *err) {
		PH_SetErrorMessage(err);
		if (sum) delete sum;
		sum = 0;
	}
	
	// free memory
	if (r) Cudd_RecursiveDeref(ddman, r);
	if (hddm) delete hddm;
	if (diags) delete[] diags;
	if (diags_dist) delete diags_dist;
	if (soln) delete[] soln;
	if (soln2) delete[] soln2;
	if (fgw.weights) delete[] fgw.weights;
	
	return ptr_to_jlong(sum);
}

//------------------------------------------------------------------------------

static void mult_rec(HDDNode *hdd, int level, int row_offset, int col_offset)
{
	HDDNode *e, *t;
	
	// if it's the zero node
	if (hdd == zero) {
		return;
	}
	// or if we've reached a submatrix
	// (check for non-null ptr but, equivalently, we could just check if level==l_sm)
	else if (hdd->sm.ptr) {
		if (!compact_sm) {
			mult_cm((CMSparseMatrix *)hdd->sm.ptr, row_offset, col_offset);
		} else {
			mult_cmsc((CMSCSparseMatrix *)hdd->sm.ptr, row_offset, col_offset);
		}
		return;
	}
	// or if we've reached the bottom
	else if (level == num_levels) {
		//printf("(%d,%d)=%f\n", row_offset, col_offset, hdd->type.val);
		soln2[row_offset] += soln[col_offset] * (hdd->type.val / unif);
		return;
	}
	// otherwise recurse
	e = hdd->type.kids.e;
	if (e != zero) {
		mult_rec(e->type.kids.e, level+1, row_offset, col_offset);
		mult_rec(e->type.kids.t, level+1, row_offset, col_offset+e->off.val);
	}
	t = hdd->type.kids.t;
	if (t != zero) {
		mult_rec(t->type.kids.e, level+1, row_offset+hdd->off.val, col_offset);
		mult_rec(t->type.kids.t, level+1, row_offset+hdd->off.val, col_offset+t->off.val);
	}
}

//-----------------------------------------------------------------------------------

static void mult_cm(CMSparseMatrix *cmsm, int row_offset, int col_offset)
{
	int i2, j2, l2, h2;
	int sm_n = cmsm->n;
	int sm_nnz = cmsm->nnz;
	double *sm_non_zeros = cmsm->non_zeros;
	unsigned char *sm_col_counts = cmsm->col_counts;
	int *sm_col_starts = (int *)cmsm->col_counts;
	bool sm_use_counts = cmsm->use_counts;
	unsigned int *sm_rows = cmsm->rows;
	
	// loop through columns of submatrix
	l2 = sm_nnz; h2 = 0;
	for (i2 = 0; i2 < sm_n; i2++) {
		
		// loop through entries in this column
		if (!sm_use_counts) { l2 = sm_col_starts[i2]; h2 = sm_col_starts[i2+1]; }
		else { l2 = h2; h2 += sm_col_counts[i2]; }
		for (j2 = l2; j2 < h2; j2++) {
			soln2[row_offset + sm_rows[j2]] += soln[col_offset + i2] * (sm_non_zeros[j2] / unif);
			//printf("(%d,%d)=%f\n", row_offset + sm_rows[j2], col_offset + i2, sm_non_zeros[j2]);
		}
	}
}

//-----------------------------------------------------------------------------------

static void mult_cmsc(CMSCSparseMatrix *cmscsm, int row_offset, int col_offset)
{
	int i2, j2, l2, h2;
	int sm_n = cmscsm->n;
	int sm_nnz = cmscsm->nnz;
	unsigned char *sm_col_counts = cmscsm->col_counts;
	int *sm_col_starts = (int *)cmscsm->col_counts;
	bool sm_use_counts = cmscsm->use_counts;
	unsigned int *sm_rows = cmscsm->rows;
	
	// loop through columns of submatrix
	l2 = sm_nnz; h2 = 0;
	for (i2 = 0; i2 < sm_n; i2++) {
		
		// loop through entries in this column
		if (!sm_use_counts) { l2 = sm_col_starts[i2]; h2 = sm_col_starts[i2+1]; }
		else { l2 = h2; h2 += sm_col_counts[i2]; }
		for (j2 = l2; j2 < h2; j2++) {
			soln2[row_offset + (int)(sm_rows[j2] >> sm_dist_shift)] += soln[col_offset + i2] * (sm_dist[(int)(sm_rows[j2] & sm_dist_mask)] / unif);
			//printf("(%d,%d)=%f\n", row_offset + (int)(sm_rows[j2] >> sm_dist_shift), col_offset + i2, sm_dist[(int)(sm_rows[j2] & sm_dist_mask)]);
		}
	}
}

//------------------------------------------------------------------------------
