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
#include <cmath>
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
#include "Measures.h"
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

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_hybrid_PrismHybrid_PH_1ProbTransient
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer tr,	// trans matrix
jlong __jlongpointer od,	// odd
jlong __jlongpointer in,	// initial distribution (note: this will be deleted afterwards)
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jint time		// time
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(tr);		// trans matrix
	ODDNode *odd = jlong_to_ODDNode(od);		// odd
	double *init = jlong_to_double(in);			// initial distribution
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars

	// model stats
	int n;
	// matrix mtbdd
	HDDMatrix *hddm = NULL;
	HDDNode *hdd = NULL;
	// vectors
	double *tmpsoln = NULL, *sum = NULL;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	bool done;
	int i, iters;
	double kb, kbt;
	// measure for convergence termination check
	MeasureSupNorm measure(term_crit == TERM_CRIT_RELATIVE);
	
	// exception handling around whole function
	try {
	
	// start clocks	
	start1 = start2 = util_cpu_time();
	
	// get number of states from odd
	n = odd->eoff + odd->toff;
	
	// build hdd for matrix
	PH_PrintToMainLog(env, "\nBuilding hybrid MTBDD matrix... ");
	hddm = build_hdd_matrix(trans, rvars, cvars, num_rvars, odd, false);
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
	
	// create solution/iteration vectors
	PH_PrintToMainLog(env, "Allocating iteration vectors... ");
	// for soln, we just use init (since we are free to modify/delete this vector)
	// we also report the memory usage of this vector here, even though it has already been created
	soln = init;
	soln2 = new double[n];
	sum = new double[n];
	kb = n*8.0/1024.0;
	kbt += 3*kb;
	PH_PrintMemoryToMainLog(env, "[3 x ", kb, "]\n");
	
	// print total memory usage
	PH_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");
	
	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;
	
	// start transient analysis
	iters = 0;
	done = false;
	PH_PrintToMainLog(env, "\nStarting iterations...\n");
	
	// note that we ignore max_iters as we know how any iterations _should_ be performed
	for (iters = 0; iters < time && !done; iters++) {
		
		// initialise vector
		for (i = 0; i < n; i++) soln2[i] = 0.0;
		
		// do matrix vector multiply bit
		mult_rec(hdd, 0, 0, 0);
		
		// check for steady state convergence
		if (do_ss_detect) {
			measure.reset();
			measure.measure(soln, soln2, n);
			if (measure.value() < term_crit_param) {
				done = true;
			}
		}
		
		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PH_PrintToMainLog(env, "Iteration %d (of %d): ", iters, (int)time);
			if (do_ss_detect) PH_PrintToMainLog(env, "max %sdiff=%f, ", measure.isRelative()?"relative ":"", measure.value());
			PH_PrintToMainLog(env, "%.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
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
	if (done) PH_PrintToMainLog(env, "\nSteady state detected at iteration %d\n", iters);
	PH_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PH_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}
	
	// free memory
	if (hddm) delete hddm;
	if (soln2) delete[] soln2;
	
	return ptr_to_jlong(soln);
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
		//printf("(%d,%d)=%f\n", col_offset, row_offset, hdd->type.val);
		soln2[col_offset] += soln[row_offset] * (hdd->type.val);
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
			soln2[col_offset + i2] += soln[row_offset + sm_rows[j2]] * (sm_non_zeros[j2]);
			//printf("(%d,%d)=%f\n", col_offset + sm_rows[j2], row_offset + i2, sm_non_zeros[j2]);
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
			soln2[col_offset + i2] += soln[row_offset + (int)(sm_rows[j2] >> sm_dist_shift)] * (sm_dist[(int)(sm_rows[j2] & sm_dist_mask)]);
			//printf("(%d,%d)=%f\n", col_offset + (int)(sm_rows[j2] >> sm_dist_shift), row_offset + i2, sm_dist[(int)(sm_rows[j2] & sm_dist_mask)]);
		}
	}
}

//------------------------------------------------------------------------------
