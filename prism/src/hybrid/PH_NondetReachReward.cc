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
#include "sparse.h"
#include "hybrid.h"
#include "PrismHybridGlob.h"
#include "jnipointer.h"
#include "prism.h"
#include "Measures.h"
#include "ExportIterations.h"
#include <new>
#include <memory>

// local prototypes
static void mult_rec(HDDNode *hdd, int level, int row_offset, int col_offset, int code);
static void mult_rm(RMSparseMatrix *rmsm, int row_offset, int col_offset, int code);
static void mult_cmsr(CMSRSparseMatrix *cmsrsm, int row_offset, int col_offset, int code);

// globals (used by local functions)
static HDDNode *zero;
static int num_levels;
static bool compact_sm;
static double *sm_dist;
static int sm_dist_shift;
static int sm_dist_mask;
static double *soln = NULL, *soln2 = NULL, *soln3 = NULL;

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_hybrid_PrismHybrid_PH_1NondetReachReward
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
	DdNode *reach = NULL, *a = NULL;
	// model stats
	int n, nm;
	// flags
	bool compact_r;
	// hybrid stuff	
	HDDMatrices *hddms = NULL, *hddms2 = NULL;
	HDDMatrix *hddm = NULL;
	HDDNode *hdd = NULL;
	// vectors
	double *rew_vec = NULL, *tmpsoln = NULL;
	DistVector *rew_dist = NULL;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, j, k, iters;
	double d, kb, kbt;
	bool done;
	// measure for convergence termination check
	MeasureSupNorm measure(term_crit == TERM_CRIT_RELATIVE);

	// exception handling around whole function
	try {
	
	// start clocks
	start1 = start2 = util_cpu_time();
	
	// get number of states
	n = odd->eoff + odd->toff;
	
	// get reachable states
	reach = odd->dd;
	
	// filter out rows (goal states and infinity states) from matrix
	Cudd_Ref(trans);
	Cudd_Ref(maybe);
	a = DD_Apply(ddman, APPLY_TIMES, trans, maybe);
	
	// build hdds for matrix
	PH_PrintToMainLog(env, "\nBuilding hybrid MTBDD matrices... ");
	hddms = build_hdd_matrices_mdp(a, NULL, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	nm = hddms->nm;
	kb = hddms->mem_nodes;
	kbt = kb;
	PH_PrintToMainLog(env, "[nm=%d, levels=%d, nodes=%d] ", hddms->nm, hddms->num_levels, hddms->num_nodes);
	PH_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// add sparse bits
	PH_PrintToMainLog(env, "Adding sparse bits... ");
	add_sparse_matrices_mdp(hddms, compact);
	kb = hddms->mem_sm;
	kbt += kb;
	PH_PrintToMainLog(env, "[levels=%d-%d, num=%d, compact=%d/%d] ", hddms->l_sm_min, hddms->l_sm_max, hddms->num_sm, hddms->compact_sm, hddms->nm);
	PH_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// multiply transition rewards by transition probs and sum rows
	// (note also filters out unwanted states at the same time)
	Cudd_Ref(trans_rewards);
	Cudd_Ref(a);
	trans_rewards = DD_Apply(ddman, APPLY_TIMES, trans_rewards, a);
	trans_rewards = DD_SumAbstract(ddman, trans_rewards, cvars, num_cvars);
	trans_rewards = DD_Apply(ddman, APPLY_TIMES, trans_rewards, DD_SetVectorElement(ddman, DD_Constant(ddman, 0), cvars, num_cvars, 0, 1));
	
	// build hdds for transition rewards matrix
	PH_PrintToMainLog(env, "Building hybrid MTBDD matrices for rewards... ");
	hddms2 = build_hdd_matrices_mdp(trans_rewards, hddms, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	kb = hddms2->mem_nodes;
	kbt = kb;
	PH_PrintToMainLog(env, "[nm=%d, levels=%d, nodes=%d] ", hddms2->nm, hddms2->num_levels, hddms2->num_nodes);
	PH_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// add sparse bits
	PH_PrintToMainLog(env, "Adding sparse bits... ");
	add_sparse_matrices_mdp(hddms2, compact);
	kb = hddms2->mem_sm;
	kbt += kb;
	PH_PrintToMainLog(env, "[levels=%d-%d, num=%d, compact=%d/%d] ", hddms2->l_sm_min, hddms2->l_sm_max, hddms2->num_sm, hddms2->compact_sm, hddms2->nm);
	PH_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// remove goal and infinity states from state rewards vector
	Cudd_Ref(state_rewards);
	Cudd_Ref(maybe);
	state_rewards = DD_Apply(ddman, APPLY_TIMES, state_rewards, maybe);
	
	// put state rewards in a vector
	PH_PrintToMainLog(env, "Creating rewards vector... ");
	rew_vec = mtbdd_to_double_vector(ddman, state_rewards, rvars, num_rvars, odd);
	// try and convert to compact form if required
	compact_r = false;
	if (compact) {
		if ((rew_dist = double_vector_to_dist(rew_vec, n))) {
			compact_r = true;
			delete rew_vec; rew_vec = NULL;
		}
	}
	kb = (!compact_r) ? n*8.0/1024.0 : (rew_dist->num_dist*8.0+n*2.0)/1024.0;
	kbt += kb;
	if (compact_r) PH_PrintToMainLog(env, "[dist=%d, compact] ", rew_dist->num_dist);
	PH_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// create solution/iteration vectors
	PH_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = new double[n];
	soln2 = new double[n];
	soln3 = new double[n];
	kb = n*8.0/1024.0;
	kbt += 3*kb;
	PH_PrintMemoryToMainLog(env, "[3 x ", kb, "]\n");
	
	// print total memory usage
	PH_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");
	
	// initial solution is zero
	for (i = 0; i < n; i++) {
		soln[i] = 0;
	}

	std::unique_ptr<ExportIterations> iterationExport;
	if (PH_GetFlagExportIterations()) {
		iterationExport.reset(new ExportIterations("PH_NondetReachReward"));
		PH_PrintToMainLog(env, "Exporting iterations to %s\n", iterationExport->getFileName().c_str());
		iterationExport->exportVector(soln, n, 0);
	}

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
		
		// initialise array for storing mins/maxs to -1s
		// (allows us to keep track of rows not visited)
		for (i = 0; i < n; i++) {	
			soln2[i] = -1;
		}
		
		// do matrix multiplication and min/max
		for (i = 0; i < nm; i++) {
			
			// start off all negative
			// (need to keep track of rows not visited)
			for (j = 0; j < n; j++) {
				soln3[j] = -1;
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
			mult_rec(hdd, 0, 0, 0, 1);
			
			// add transition rewards
			// store stuff to be used globally
			hddm = hddms2->choices[i];
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
			mult_rec(hdd, 0, 0, 0, 2);
			
			// min/max
			for (j = 0; j < n; j++) {
				if (soln3[j] >= 0) {
					if (soln2[j] < 0) {
						soln2[j] = soln3[j];
					} else if (min) {
						if (soln3[j] < soln2[j]) soln2[j] = soln3[j];
					} else {
						if (soln3[j] > soln2[j]) soln2[j] = soln3[j];
					}
				}
			}
		}
		
		// add state rewards
		if (!compact_r) {
			for (i = 0; i < n; i++) { if(soln2[i] < 0) soln2[i] = 0; soln2[i] += rew_vec[i]; }
		} else {
			for (i = 0; i < n; i++) { if(soln2[i] < 0) soln2[i] = 0; soln2[i] += rew_dist->dist[rew_dist->ptrs[i]]; }
		}

		if (iterationExport)
			iterationExport->exportVector(soln2, n, 0);

		// check convergence
		measure.reset();
		measure.measure(soln, soln2, n);
		if (measure.value() < term_crit_param) {
			done = true;
		}

		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PH_PrintToMainLog(env, "Iteration %d: max %sdiff=%f", iters, measure.isRelative()?"relative ":"", measure.value());
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
	
	// print iterations/timing info
	PH_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	// if the iterative method didn't terminate, this is an error
	if (!done) { delete[] soln; soln = NULL; PH_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters); }
	
	// the difference between vector values is not a reliable error bound
	// but we store it anyway in case it is useful for estimating a bound
	last_error_bound = measure.value();
	
	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PH_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}
	
	// free memory
	if (a) Cudd_RecursiveDeref(ddman, a);
	if (state_rewards) Cudd_RecursiveDeref(ddman, state_rewards);
	if (trans_rewards) Cudd_RecursiveDeref(ddman, trans_rewards);
	if (hddms) delete hddms;
	if (hddms2) delete hddms2;
	if (rew_vec) delete[] rew_vec;
	if (rew_dist) delete rew_dist;
	if (soln2) delete[] soln2;
	if (soln3) delete[] soln3;
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------

static void mult_rec(HDDNode *hdd, int level, int row_offset, int col_offset, int code)
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
			mult_rm((RMSparseMatrix *)hdd->sm.ptr, row_offset, col_offset, code);
		} else {
			mult_cmsr((CMSRSparseMatrix *)hdd->sm.ptr, row_offset, col_offset, code);
		}
		return;
	}
	// or if we've reached the bottom
	else if (level == num_levels) {
		//printf("(%d,%d)=%f\n", row_offset, col_offset, hdd->type.val);
		switch (code) {
		case 1:
			if (soln3[row_offset] < 0) soln3[row_offset] = 0;
			soln3[row_offset] += soln[col_offset] * hdd->type.val;
			break;
		case 2:
			if (soln3[row_offset] < 0) soln3[row_offset] = 0;
			soln3[row_offset] += hdd->type.val;
			break;
		}
		return;
	}
	// otherwise recurse
	e = hdd->type.kids.e;
	if (e != zero) {
		mult_rec(e->type.kids.e, level+1, row_offset, col_offset, code);
		mult_rec(e->type.kids.t, level+1, row_offset, col_offset+e->off.val, code);
	}
	t = hdd->type.kids.t;
	if (t != zero) {
		mult_rec(t->type.kids.e, level+1, row_offset+hdd->off.val, col_offset, code);
		mult_rec(t->type.kids.t, level+1, row_offset+hdd->off.val, col_offset+t->off.val, code);
	}
}

//-----------------------------------------------------------------------------------

static void mult_rm(RMSparseMatrix *rmsm, int row_offset, int col_offset, int code)
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
			switch (code) {
			case 1:
				r = row_offset + i2;
				if (soln3[r] < 0) soln3[r] = 0;
				soln3[r] += soln[col_offset + sm_cols[j2]] * sm_non_zeros[j2];
				break;
			case 2:
				r = row_offset + i2;
				if (soln3[r] < 0) soln3[r] = 0;
				soln3[r] += sm_non_zeros[j2];
				break;
			}
			//printf("(%d,%d)=%f\n", row_offset + i2, col_offset + sm_cols[j2], sm_non_zeros[j2]);
		}
	}
}

//-----------------------------------------------------------------------------------

static void mult_cmsr(CMSRSparseMatrix *cmsrsm, int row_offset, int col_offset, int code)
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
			switch (code) {
			case 1:
				r = row_offset + i2;
				if (soln3[r] < 0) soln3[r] = 0;
				soln3[r] += soln[col_offset + (int)(sm_cols[j2] >> sm_dist_shift)] * sm_dist[(int)(sm_cols[j2] & sm_dist_mask)];
				break;
			case 2:
				r = row_offset + i2;
				if (soln3[r] < 0) soln3[r] = 0;
				soln3[r] += sm_dist[(int)(sm_cols[j2] & sm_dist_mask)];
				break;
			}
			//`("(%d,%d)=%f\n", row_offset + i2, col_offset + (int)(sm_cols[j2] >> sm_dist_shift), sm_dist[(int)(sm_cols[j2] & sm_dist_mask)]);
		}
	}
}

//------------------------------------------------------------------------------
