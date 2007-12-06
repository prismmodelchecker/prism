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

// local prototypes
static void jor_rec(HDDNode *hdd, int level, int row_offset, int col_offset, bool transpose);
static void jor_rm(RMSparseMatrix *rmsm, int row_offset, int col_offset);
static void jor_cmsr(CMSRSparseMatrix *cmsrsm, int row_offset, int col_offset);

// globals (used by local functions)
static HDDNode *zero;
static int num_levels;
static bool compact_sm;
static double *sm_dist;
static int sm_dist_shift;
static int sm_dist_mask;
static double *soln, *soln2;

//------------------------------------------------------------------------------

// solve the linear equation system Ax=b with Jacobi/JOR

JNIEXPORT jlong __pointer JNICALL Java_hybrid_PrismHybrid_PH_1JOR
(
JNIEnv *env,
jclass cls,
jlong __pointer _odd,	// odd
jlong __pointer rv,	// row vars
jint num_rvars,
jlong __pointer cv,	// col vars
jint num_cvars,
jlong __pointer _a,	// matrix A
jlong __pointer _b,	// vector b (if null, assume all zero)
jlong __pointer _init,	// init soln
jboolean transpose,	// transpose A? (i.e. solve xA=b not Ax=b?)
jboolean row_sums,	// use row sums for diags instead? (strictly speaking: negative sum of non-diagonal row elements)
jdouble omega		// omega (over-relaxation parameter)
)
{
	// cast function parameters
	ODDNode *odd = jlong_to_ODDNode(_odd);		// odd
	DdNode **rvars = jlong_to_DdNode_array(rv); 	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv); 	// col vars
	DdNode *a = jlong_to_DdNode(_a);		// matrix A
	DdNode *b = jlong_to_DdNode(_b);		// vector b
	DdNode *init = jlong_to_DdNode(_init);		// init soln

	// mtbdds
	DdNode *reach, *diags, *id, *tmp;
	// model stats
	int n;
	long nnz;
	// flags
	bool compact_d, compact_b;
	// matrix mtbdd
	HDDMatrix *hddm;
	HDDNode *hdd;
	// vectors
	double *diags_vec, *b_vec, *tmpsoln;
	DistVector *diags_dist, *b_dist;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, j, l, h, iters;
	double d, kb, kbt;
	bool done;
	
	// start clocks
	start1 = start2 = util_cpu_time();
	
	// get number of states
	n = odd->eoff + odd->toff;
	
	// get reachable states
	reach = odd->dd;
	
	// make local copy of a
	Cudd_Ref(a);
	
	// remove and keep diagonal entries of matrix A
	id =  DD_Identity(ddman, rvars, cvars, num_rvars);
	Cudd_Ref(reach);
	id = DD_And(ddman, id, reach);
	Cudd_Ref(id);
	Cudd_Ref(a);
	diags = DD_Apply(ddman, APPLY_TIMES, id, a);
	Cudd_Ref(id);
	a = DD_ITE(ddman, id, DD_Constant(ddman, 0), a);
	
	// build hdd for matrix
	PH_PrintToMainLog(env, "\nBuilding hybrid MTBDD matrix... ");
	hddm = build_hdd_matrix(a, rvars, cvars, num_rvars, odd, true, transpose);
	hdd = hddm->top;
	zero = hddm->zero;
	num_levels = hddm->num_levels;
	kb = hddm->mem_nodes;
	kbt = kb;
	PH_PrintToMainLog(env, "[levels=%d, nodes=%d] [%.1f KB]\n", hddm->num_levels, hddm->num_nodes, kb);
	
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
	PH_PrintToMainLog(env, "[levels=%d, num=%d%s] [%.1f KB]\n", hddm->l_sm, hddm->num_sm, compact_sm?", compact":"", kb);
	
	// get vector of diags, either by extracting from mtbdd or
	// by doing (negative, non-diagonal) row sums of original A matrix (and then setting to 1 if sum is 0)
	PH_PrintToMainLog(env, "Creating vector for diagonals... ");
	if (!row_sums) {
		diags = DD_MaxAbstract(ddman, diags, cvars, num_cvars);
		diags_vec = mtbdd_to_double_vector(ddman, diags, rvars, num_rvars, odd);
	} else {
		diags_vec = hdd_negative_row_sums(hddm, n, transpose);
	}
	if (!diags_vec) { PH_PrintToMainLog(env, "\n"); PH_SetErrorMessage("Out of memory when creating diagonals vector"); return 0; }
	// if any of the diagonals are zero, set them to one - avoids division by zero errors later
	// strictly speaking, such matrices shouldn't work for this iterative method
	// but they do occur, e.g. for steady-state computation of a bscc, this fixes it
	for (i = 0; i < n; i++) diags_vec[i] = (diags_vec[i] == 0) ? 1.0 : diags_vec[i];
	// try and convert to compact form if required
	compact_d = false;
	if (compact) {
		if (diags_dist = double_vector_to_dist(diags_vec, n)) {
			compact_d = true;
			free(diags_vec);
		}
	}
	kb = (!compact_d) ? n*8.0/1024.0 : (diags_dist->num_dist*8.0+n*2.0)/1024.0;
	kbt += kb;
	if (!compact_d) PH_PrintToMainLog(env, "[%.1f KB]\n", kb);
	else PH_PrintToMainLog(env, "[dist=%d, compact] [%.1f KB]\n", diags_dist->num_dist, kb);
	
	// invert diagonal
	if (!compact_d) {
		for (i = 0; i < n; i++) diags_vec[i] = 1.0 / diags_vec[i];
	} else {
		for (i = 0; i < diags_dist->num_dist; i++) diags_dist->dist[i] = 1.0 / diags_dist->dist[i];
	}
	
	// build b vector (if present)
	if (b != NULL) {
		PH_PrintToMainLog(env, "Creating vector for RHS... ");
		b_vec = mtbdd_to_double_vector(ddman, b, rvars, num_rvars, odd);
		// try and convert to compact form if required
		compact_b = false;
		if (compact) {
			if (b_dist = double_vector_to_dist(b_vec, n)) {
				compact_b = true;
				free(b_vec);
			}
		}
		kb = (!compact_b) ? n*8.0/1024.0 : (b_dist->num_dist*8.0+n*2.0)/1024.0;
		kbt += kb;
		if (!compact_b) PH_PrintToMainLog(env, "[%.1f KB]\n", kb);
		else PH_PrintToMainLog(env, "[dist=%d, compact] [%.1f KB]\n", b_dist->num_dist, kb);
	}
	
	// create solution/iteration vectors
	PH_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = mtbdd_to_double_vector(ddman, init, rvars, num_rvars, odd);
	soln2 = new double[n];
	if (!soln2) { PH_SetErrorMessage("Out of memory"); return 0; }
	kb = n*8.0/1024.0;
	kbt += 2*kb;
	PH_PrintToMainLog(env, "[2 x %.1f KB]\n", kb);
	
	// print total memory usage
	PH_PrintToMainLog(env, "TOTAL: [%.1f KB]\n", kbt);
	
	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	
	// start iterations
	iters = 0;
	done = false;
	PH_PrintToMainLog(env, "\nStarting iterations...\n");
	
	while (!done && iters < max_iters) {
		
		iters++;
		
//		PH_PrintToMainLog(env, "Iteration %d: ", iters);
//		start3 = util_cpu_time();
		
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
		jor_rec(hdd, 0, 0, 0, transpose);
		
		// divide by diagonal
		if (!compact_d) {
			for (i = 0; i < n; i++) { soln2[i] *= diags_vec[i]; }
		} else {
			for (i = 0; i < n; i++) { soln2[i] *= diags_dist->dist[diags_dist->ptrs[i]]; }
		}
		
		// do over-relaxation if necessary
		if (omega != 1) {
			for (i = 0; i < n; i++) {
				soln2[i] = ((1-omega) * soln[i]) + (omega * soln2[i]);
			}
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
		
//		PH_PrintToMainLog(env, "%.2f %.2f sec\n", ((double)(util_cpu_time() - start3)/1000), ((double)(util_cpu_time() - start2)/1000)/iters);
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iters/timing info
	PH_PrintToMainLog(env, "\n%s: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", (omega == 1.0)?"Jacobi":"JOR", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	// free memory
	Cudd_RecursiveDeref(ddman, a);
	Cudd_RecursiveDeref(ddman, id);
	Cudd_RecursiveDeref(ddman, diags);
	free_hdd_matrix(hddm);
	if (compact_d) free_dist_vector(diags_dist); else free(diags_vec);
	if (b != NULL) if (compact_b) free_dist_vector(b_dist); else free(b_vec);
	delete soln2;
	
	// if the iterative method didn't terminate, this is an error
	if (!done) { delete soln; PH_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters); return 0; }
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------

static void jor_rec(HDDNode *hdd, int level, int row_offset, int col_offset, bool transpose)
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
			jor_rm((RMSparseMatrix *)hdd->sm.ptr, row_offset, col_offset);
		} else {
			jor_cmsr((CMSRSparseMatrix *)hdd->sm.ptr, row_offset, col_offset);
		}
		return;
	}
	// or if we've reached the bottom
	else if (level == num_levels) {
		//printf("(%d,%d)=%f\n", row_offset, col_offset, hdd->type.val);
		soln2[row_offset] -= soln[col_offset] * hdd->type.val;
		return;
	}
	// otherwise recurse
	e = hdd->type.kids.e;
	if (e != zero) {
		if (!transpose) {
			jor_rec(e->type.kids.e, level+1, row_offset, col_offset, transpose);
			jor_rec(e->type.kids.t, level+1, row_offset, col_offset+e->off.val, transpose);
		} else {
			jor_rec(e->type.kids.e, level+1, row_offset, col_offset, transpose);
			jor_rec(e->type.kids.t, level+1, row_offset+e->off.val, col_offset, transpose);
		}
	}
	t = hdd->type.kids.t;
	if (t != zero) {
		if (!transpose) {
			jor_rec(t->type.kids.e, level+1, row_offset+hdd->off.val, col_offset, transpose);
			jor_rec(t->type.kids.t, level+1, row_offset+hdd->off.val, col_offset+t->off.val, transpose);
		} else {
			jor_rec(t->type.kids.e, level+1, row_offset, col_offset+hdd->off.val, transpose);
			jor_rec(t->type.kids.t, level+1, row_offset+t->off.val, col_offset+hdd->off.val, transpose);
		}
	}
}

//-----------------------------------------------------------------------------------

static void jor_rm(RMSparseMatrix *rmsm, int row_offset, int col_offset)
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
			soln2[row_offset + i2] -= soln[col_offset + sm_cols[j2]] * sm_non_zeros[j2];
			//printf("(%d,%d)=%f\n", row_offset + i2, col_offset + sm_cols[j2], sm_non_zeros[j2]);
		}
	}
}

//-----------------------------------------------------------------------------------

static void jor_cmsr(CMSRSparseMatrix *cmsrsm, int row_offset, int col_offset)
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
			soln2[row_offset + i2] -= soln[col_offset + (int)(sm_cols[j2] >> sm_dist_shift)] * sm_dist[(int)(sm_cols[j2] & sm_dist_mask)];
			//printf("(%d,%d)=%f\n", row_offset + i2, col_offset + (int)(sm_cols[j2] >> sm_dist_shift), sm_dist[(int)(sm_cols[j2] & sm_dist_mask)]);
		}
	}
}

//------------------------------------------------------------------------------
