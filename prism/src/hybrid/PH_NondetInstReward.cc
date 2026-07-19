//==============================================================================
//
//	Copyright (c) 2026-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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
#include "sparse.h"
#include "hybrid.h"
#include "PrismNativeGlob.h"
#include "jnipointer.h"
#include "prism.h"
#include <new>

// local prototypes
static void mult_rec(HDDNode *hdd, int level, int row_offset, int col_offset);
static void mult_rm(RMSparseMatrix *rmsm, int row_offset, int col_offset);
static void mult_cmsr(CMSRSparseMatrix *cmsrsm, int row_offset, int col_offset);

// globals (used by local functions)
static HDDNode *zero;
static int num_levels;
static bool compact_sm;
static double *sm_dist;
static int sm_dist_shift;
static int sm_dist_mask;
static double *soln = NULL, *soln2 = NULL, *soln3 = NULL;
// Which entries of soln2/soln3 have been written to yet.
// Note: the other hybrid MDP routines track this with a negative value in the vector
// itself, but that is not available here: instantaneous rewards may legitimately be
// negative (unlike probabilities, or the cumulated rewards of the other reward
// routines, which are checked to be non-negative), so a negative value cannot be
// distinguished from "not visited".
static bool *soln2_seen = NULL, *soln3_seen = NULL;

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_hybrid_PrismHybrid_PH_1NondetInstReward
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer t,	// trans matrix
jlong __jlongpointer sr,	// state rewards
jlong __jlongpointer od,	// odd
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer ndv,	// nondet vars
jint num_ndvars,
jint bound,			// time bound
jboolean min		// min or max probabilities (true = min, false = max)
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(t);		// trans matrix
	DdNode *state_rewards = jlong_to_DdNode(sr);	// state rewards
	ODDNode *odd = jlong_to_ODDNode(od); 		// reachable states
	DdNode **rvars = jlong_to_DdNode_array(rv); 	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv); 	// col vars
	DdNode **ndvars = jlong_to_DdNode_array(ndv);	// nondet vars

	// model stats
	int n, nm;
	// hybrid stuff
	HDDMatrices *hddms = NULL;
	HDDMatrix *hddm = NULL;
	HDDNode *hdd = NULL;
	// vectors
	double *tmpsoln = NULL;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, j, iters;
	double kb, kbt;

	// exception handling around whole function
	try {

	// start clocks
	start1 = start2 = util_cpu_time();

	// get number of states
	n = odd->eoff + odd->toff;

	// build hdds for matrix
	PN_PrintToMainLog(env, "\nBuilding hybrid MTBDD matrices... ");
	hddms = build_hdd_matrices_mdp(trans, NULL, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	nm = hddms->nm;
	kb = hddms->mem_nodes;
	kbt = kb;
	PN_PrintToMainLog(env, "[nm=%d, levels=%d, nodes=%d] ", hddms->nm, hddms->num_levels, hddms->num_nodes);
	PN_PrintMemoryToMainLog(env, "[", kb, "]\n");

	// add sparse bits
	PN_PrintToMainLog(env, "Adding sparse bits... ");
	add_sparse_matrices_mdp(hddms, compact);
	kb = hddms->mem_sm;
	kbt += kb;
	PN_PrintToMainLog(env, "[levels=%d-%d, num=%d, compact=%d/%d] ", hddms->l_sm_min, hddms->l_sm_max, hddms->num_sm, hddms->compact_sm, hddms->nm);
	PN_PrintMemoryToMainLog(env, "[", kb, "]\n");

	// create solution/iteration vectors
	// (solution is initialised to the state rewards)
	PN_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = mtbdd_to_double_vector(ddman, state_rewards, rvars, num_rvars, odd);
	soln2 = new double[n];
	soln3 = new double[n];
	soln2_seen = new bool[n];
	soln3_seen = new bool[n];
	kb = n*8.0/1024.0;
	kbt += 3*kb;
	PN_PrintMemoryToMainLog(env, "[3 x ", kb, "]\n");

	// print total memory usage
	PN_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");

	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;

	// start iterations
	iters = 0;
	PN_PrintToMainLog(env, "\nStarting iterations...\n");

	// note that we ignore max_iters as we know how many iterations _should_ be performed
	for (iters = 0; iters < bound; iters++) {

		// mark all rows as not yet having a min/max stored
		for (i = 0; i < n; i++) {
			soln2_seen[i] = false;
		}

		// do matrix multiplication and min/max
		for (i = 0; i < nm; i++) {

			// mark all rows as not visited for this choice
			for (j = 0; j < n; j++) {
				soln3_seen[j] = false;
			}

			// matrix multiply
			// store stuff to be used globally
			hddm = hddms->choices[i];
			hdd = hddm->top;
			zero = hddm->zero;
			num_levels = hddm->num_levels;
			compact_sm = hddm->compact_sm;
			if (compact_sm) {
				sm_dist = hddm->dist;
				sm_dist_shift = hddm->dist_shift;
				sm_dist_mask = hddm->dist_mask;
			}
			// do traversal
			mult_rec(hdd, 0, 0, 0);

			// min/max
			for (j = 0; j < n; j++) {
				if (soln3_seen[j]) {
					if (!soln2_seen[j]) {
						soln2[j] = soln3[j];
						soln2_seen[j] = true;
					} else if (min) {
						if (soln3[j] < soln2[j]) soln2[j] = soln3[j];
					} else {
						if (soln3[j] > soln2[j]) soln2[j] = soln3[j];
					}
				}
			}
		}

		// states with no choices (deadlocks) get reward zero
		for (i = 0; i < n; i++) {
			if (!soln2_seen[i]) soln2[i] = 0;
		}

		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PN_PrintToMainLog(env, "Iteration %d (of %d): ", iters, (int)bound);
			PN_PrintToMainLog(env, "%.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
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
	PN_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);

	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PN_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}

	// free memory
	if (hddms) delete hddms;
	if (soln2) delete[] soln2;
	if (soln3) delete[] soln3;
	if (soln2_seen) delete[] soln2_seen;
	if (soln3_seen) delete[] soln3_seen;

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
			mult_rm((RMSparseMatrix *)hdd->sm.ptr, row_offset, col_offset);
		} else {
			mult_cmsr((CMSRSparseMatrix *)hdd->sm.ptr, row_offset, col_offset);
		}
		return;
	}
	// or if we've reached the bottom
	else if (level == num_levels) {
		if (!soln3_seen[row_offset]) { soln3_seen[row_offset] = true; soln3[row_offset] = 0; }
		soln3[row_offset] += soln[col_offset] * hdd->type.val;
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

static void mult_rm(RMSparseMatrix *rmsm, int row_offset, int col_offset)
{
	int i2, j2, l2, h2, r;
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
			r = row_offset + i2;
			if (!soln3_seen[r]) { soln3_seen[r] = true; soln3[r] = 0; }
			soln3[r] += soln[col_offset + sm_cols[j2]] * sm_non_zeros[j2];
		}
	}
}

//-----------------------------------------------------------------------------------

static void mult_cmsr(CMSRSparseMatrix *cmsrsm, int row_offset, int col_offset)
{
	int i2, j2, l2, h2, r;
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
			r = row_offset + i2;
			if (!soln3_seen[r]) { soln3_seen[r] = true; soln3[r] = 0; }
			soln3[r] += soln[col_offset + (int)(sm_cols[j2] >> sm_dist_shift)] * sm_dist[(int)(sm_cols[j2] & sm_dist_mask)];
		}
	}
}

//------------------------------------------------------------------------------
