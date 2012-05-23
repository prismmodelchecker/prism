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
#include "sparse.h"
#include "hybrid.h"
#include "PrismHybridGlob.h"
#include "jnipointer.h"
#include "prism.h"
#include <new>

// local prototypes
static void power_rec(HDDNode *hdd, int level, int row_offset, int col_offset, bool transpose);
static void power_rm(RMSparseMatrix *rmsm, int row_offset, int col_offset);
static void power_cmsr(CMSRSparseMatrix *cmsrsm, int row_offset, int col_offset);

// globals (used by local functions)
static HDDNode *zero;
static int num_levels;
static bool compact_sm;
static double *sm_dist;
static int sm_dist_shift;
static int sm_dist_mask;
static double *soln = NULL, *soln2 = NULL;

//------------------------------------------------------------------------------

// solve the linear equation system Ax=x with the Power method
// in addition, solutions may be provided for additional states in the vector b
// these states are assumed not to have non-zero rows in the matrix A

JNIEXPORT jlong __jlongpointer JNICALL Java_hybrid_PrismHybrid_PH_1Power
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer _odd,	// odd
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer _a,	// matrix A
jlong __jlongpointer _b,	// vector b (if null, assume all zero)
jlong __jlongpointer _init,	// init soln
jboolean transpose	// transpose A? (i.e. solve xA=x not Ax=x?)
)
{
	// cast function parameters
	ODDNode *odd = jlong_to_ODDNode(_odd);		// odd
	DdNode **rvars = jlong_to_DdNode_array(rv); 	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv); 	// col vars
	DdNode *a = jlong_to_DdNode(_a);		// matrix A
	DdNode *b = jlong_to_DdNode(_b);		// vector b
	DdNode *init = jlong_to_DdNode(_init);		// init soln
	// model stats
	int n;
	// flags
	bool compact_b;
	// matrix mtbdd
	HDDMatrix *hddm = NULL;
	HDDNode *hdd = NULL;
	// vectors
	double *b_vec = NULL, *tmpsoln = NULL;
	DistVector *b_dist = NULL;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, iters;
	double x, sup_norm, kb, kbt;
	bool done;
	
	// exception handling around whole function
	try {
	
	// start clocks
	start1 = start2 = util_cpu_time();
	
	// get number of states
	n = odd->eoff + odd->toff;
	
	// make local copy of a
	Cudd_Ref(a);
	
	// build hdd for matrix
	PH_PrintToMainLog(env, "\nBuilding hybrid MTBDD matrix... ");
	hddm = build_hdd_matrix(a, rvars, cvars, num_rvars, odd, true, transpose);
	hdd = hddm->top;
	zero = hddm->zero;
	num_levels = hddm->num_levels;
	kb = hddm->mem_nodes;
	kbt = kb;
	PH_PrintToMainLog(env, "[levels=%d, nodes=%d] ", hddm->num_levels, hddm->num_nodes);
	PH_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// add sparse matrices
	PH_PrintToMainLog(env, "Adding explicit sparse matrices... ");
	add_sparse_matrices(hddm, compact, false, transpose);
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
	
	// build b vector (if present)
	if (b != NULL) {
		PH_PrintToMainLog(env, "Creating vector for RHS... ");
		b_vec = mtbdd_to_double_vector(ddman, b, rvars, num_rvars, odd);
		// try and convert to compact form if required
		compact_b = false;
		if (compact) {
			if ((b_dist = double_vector_to_dist(b_vec, n))) {
				compact_b = true;
				delete[] b_vec; b_vec = NULL;
			}
		}
		kb = (!compact_b) ? n*8.0/1024.0 : (b_dist->num_dist*8.0+n*2.0)/1024.0;
		kbt += kb;
		if (compact_b) PH_PrintToMainLog(env, "[dist=%d, compact] ", b_dist->num_dist);
		PH_PrintMemoryToMainLog(env, "[", kb, "]\n");
	}
	
	// create solution/iteration vectors
	PH_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = mtbdd_to_double_vector(ddman, init, rvars, num_rvars, odd);
	soln2 = new double[n];
	kb = n*8.0/1024.0;
	kbt += 2*kb;
	PH_PrintMemoryToMainLog(env, "[2 x ", kb, "]\n");
	
	// print total memory usage
	PH_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");
	
	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;
	
	// start iterations
	iters = 0;
	done = false;
	PH_PrintToMainLog(env, "\nStarting iterations...\n");
	
	while (!done && iters < max_iters) {
		
		iters++;
		
		// matrix multiply
		
		// initialise vector
		if (b == NULL) {
			for (i = 0; i < n; i++) { soln2[i] = 0.0; }
		} else if (!compact_b) {
			for (i = 0; i < n; i++) { soln2[i] = b_vec[i]; }
		} else {
			for (i = 0; i < n; i++) { soln2[i] = b_dist->dist[b_dist->ptrs[i]]; }
		}
		
		// do matrix vector multiply bit
		power_rec(hdd, 0, 0, 0, transpose);
		
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
			PH_PrintToMainLog(env, "Iteration %d: max %sdiff=%f", iters, (term_crit == TERM_CRIT_RELATIVE)?"relative ":"", sup_norm);
			PH_PrintToMainLog(env, ", %.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
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
	
	// print iters/timing info
	PH_PrintToMainLog(env, "\nPower method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	// if the iterative method didn't terminate, this is an error
	if (!done) { delete soln; soln = NULL; PH_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters); }
	
	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PH_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}
	
	// free memory
	if (a) Cudd_RecursiveDeref(ddman, a);
	if (hddm) delete hddm;
	if (b_vec) delete[] b_vec;
	if (b_dist) delete b_dist;
	if (soln2) delete soln2;
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------

static void power_rec(HDDNode *hdd, int level, int row_offset, int col_offset, bool transpose)
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
			power_rm((RMSparseMatrix *)hdd->sm.ptr, row_offset, col_offset);
		} else {
			power_cmsr((CMSRSparseMatrix *)hdd->sm.ptr, row_offset, col_offset);
		}
		return;
	}
	// or if we've reached the bottom
	else if (level == num_levels) {
		//printf("(%d,%d)=%f\n", row_offset, col_offset, hdd->type.val);
		soln2[row_offset] += soln[col_offset] * hdd->type.val;
		return;
	}
	// otherwise recurse
	e = hdd->type.kids.e;
	if (e != zero) {
		if (!transpose) {
			power_rec(e->type.kids.e, level+1, row_offset, col_offset, transpose);
			power_rec(e->type.kids.t, level+1, row_offset, col_offset+e->off.val, transpose);
		} else {
			power_rec(e->type.kids.e, level+1, row_offset, col_offset, transpose);
			power_rec(e->type.kids.t, level+1, row_offset+e->off.val, col_offset, transpose);
		}
	}
	t = hdd->type.kids.t;
	if (t != zero) {
		if (!transpose) {
			power_rec(t->type.kids.e, level+1, row_offset+hdd->off.val, col_offset, transpose);
			power_rec(t->type.kids.t, level+1, row_offset+hdd->off.val, col_offset+t->off.val, transpose);
		} else {
			power_rec(t->type.kids.e, level+1, row_offset, col_offset+hdd->off.val, transpose);
			power_rec(t->type.kids.t, level+1, row_offset+t->off.val, col_offset+hdd->off.val, transpose);
		}
	}
}

//-----------------------------------------------------------------------------------

static void power_rm(RMSparseMatrix *rmsm, int row_offset, int col_offset)
{
	int i2, j2, l2, h2;
	int sm_n = rmsm->n;
	int sm_nnz = rmsm->nnz;
	double *sm_non_zeros = rmsm->non_zeros;
	unsigned char *sm_row_counts = rmsm->row_counts;
	int *sm_row_starts = (int *)rmsm->row_counts;
	bool sm_use_counts = rmsm->use_counts;
	unsigned int *sm_cols = rmsm->cols;
	
	// loop through rows of submatrix
	l2 = sm_nnz; h2 = 0;
	for (i2 = 0; i2 < sm_n; i2++) {
		
		// loop through entries in this row
		if (!sm_use_counts) { l2 = sm_row_starts[i2]; h2 = sm_row_starts[i2+1]; }
		else { l2 = h2; h2 += sm_row_counts[i2]; }
		for (j2 = l2; j2 < h2; j2++) {
			soln2[row_offset + i2] += soln[col_offset + sm_cols[j2]] * sm_non_zeros[j2];
			//printf("(%d,%d)=%f\n", row_offset + i2, col_offset + sm_cols[j2], sm_non_zeros[j2]);
		}
	}
}

//-----------------------------------------------------------------------------------

static void power_cmsr(CMSRSparseMatrix *cmsrsm, int row_offset, int col_offset)
{
	int i2, j2, l2, h2;
	int sm_n = cmsrsm->n;
	int sm_nnz = cmsrsm->nnz;
	unsigned char *sm_row_counts = cmsrsm->row_counts;
	int *sm_row_starts = (int *)cmsrsm->row_counts;
	bool sm_use_counts = cmsrsm->use_counts;
	unsigned int *sm_cols = cmsrsm->cols;
	
	// loop through rows of submatrix
	l2 = sm_nnz; h2 = 0;
	for (i2 = 0; i2 < sm_n; i2++) {
		
		// loop through entries in this row
		if (!sm_use_counts) { l2 = sm_row_starts[i2]; h2 = sm_row_starts[i2+1]; }
		else { l2 = h2; h2 += sm_row_counts[i2]; }
		for (j2 = l2; j2 < h2; j2++) {
			soln2[row_offset + i2] += soln[col_offset + (int)(sm_cols[j2] >> sm_dist_shift)] * sm_dist[(int)(sm_cols[j2] & sm_dist_mask)];
			//printf("(%d,%d)=%f\n", row_offset + i2, col_offset + (int)(sm_cols[j2] >> sm_dist_shift), sm_dist[(int)(sm_cols[j2] & sm_dist_mask)]);
		}
	}
}

//------------------------------------------------------------------------------
