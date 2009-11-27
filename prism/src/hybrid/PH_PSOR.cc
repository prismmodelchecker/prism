//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Rashid Mehmood <rxm@cs.bham.ac.uk> (University of Birmingham)
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
#include <new>

// local prototypes
static void psor_rec(HDDNode *hdd, int level, int row_offset, int col_offset, bool transpose);
static void psor_rm(RMSparseMatrix *rmsm, int row_offset, int col_offset);
static void psor_cmsr(CMSRSparseMatrix *cmsrsm, int row_offset, int col_offset);

// globals (used by local functions)
static HDDNode *zero;
static int num_levels;
static bool compact_sm;
static double *sm_dist;
static int sm_dist_shift;
static int sm_dist_mask;
static double *soln = NULL, *soln2 = NULL;

//------------------------------------------------------------------------------

// solve the linear equation system Ax=b with Pseudo Gauss-Seidel/SOR

JNIEXPORT jlong __jlongpointer JNICALL Java_hybrid_PrismHybrid_PH_1PSOR
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
jboolean transpose,	// transpose A? (i.e. solve xA=b not Ax=b?)
jboolean row_sums,	// use row sums for diags instead? (strictly speaking: negative sum of non-diagonal row elements)
jdouble omega,		// omega (over-relaxation parameter)
jboolean forwards	// forwards or backwards?
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
	DdNode *reach = NULL, *diags = NULL, *id = NULL;
	// model stats
	int n;
	// flags
	bool compact_d, compact_b;
	// matrix mtbdd
	HDDMatrix *hddm = NULL;
	HDDNode *hdd = NULL;
	// vectors
	double *diags_vec = NULL, *b_vec = NULL;
	DistVector *diags_dist = NULL, *b_dist = NULL;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, j, fb, l, h, i2, h2, iters;
	double x, sup_norm, kb, kbt;
	bool done;
	
	// exception handling around whole function
	try {
	
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
	PH_PrintToMainLog(env, "[levels=%d, nodes=%d] ", hddm->num_levels, hddm->num_nodes);
	PH_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// split hdd matrix into blocks
	// nb: in terms of memory, this gets precedence over sparse matrices
	PH_PrintToMainLog(env, "Splitting into blocks... ");
	split_hdd_matrix(hddm, compact, false, transpose);
	compact_b = hddm->compact_b;
	kb = hddm->mem_b;
	kbt += kb;
	PH_PrintToMainLog(env, "[levels=%d, n=%d, nnz=%d%s] ", hddm->l_b, hddm->blocks->n, hddm->blocks->nnz, compact_b?", compact":"");
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
	
	// get vector of diags, either by extracting from mtbdd or
	// by doing (negative, non-diagonal) row sums of original A matrix (and then setting to 1 if sum is 0)
	// (the latter is a fix for steady-state solution of a subsystem e.g. a BSCC)
	PH_PrintToMainLog(env, "Creating vector for diagonals... ");
	if (!row_sums) {
		diags = DD_MaxAbstract(ddman, diags, cvars, num_cvars);
		diags_vec = mtbdd_to_double_vector(ddman, diags, rvars, num_rvars, odd);
	} else {
		diags_vec = hdd_negative_row_sums(hddm, n, transpose);
	}
	// if any of the diagonals are zero, set them to one - avoids division by zero errors later
	// strictly speaking, such matrices shouldn't work for this iterative method
	// but they do occur, e.g. for steady-state computation of a bscc, this fixes it
	for (i = 0; i < n; i++) diags_vec[i] = (diags_vec[i] == 0) ? 1.0 : diags_vec[i];
	// try and convert to compact form if required
	compact_d = false;
	if (compact) {
		if ((diags_dist = double_vector_to_dist(diags_vec, n))) {
			compact_d = true;
			delete[] diags_vec; diags_vec = NULL;
		}
	}
	kb = (!compact_d) ? n*8.0/1024.0 : (diags_dist->num_dist*8.0+n*2.0)/1024.0;
	kbt += kb;
	if (compact_d) PH_PrintToMainLog(env, "[dist=%d, compact] ", diags_dist->num_dist);
	PH_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
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
	soln2 = new double[hddm->blocks->max];
	for (i = 0; i < hddm->blocks->max; i++) soln2[i] = 0;
	kb = (n*8.0/1024.0)+(hddm->blocks->max*8.0/1024.0);
	kbt += kb;
	PH_PrintMemoryToMainLog(env, "[", (n*8.0/1024.0), "");
	PH_PrintMemoryToMainLog(env, " + ", (hddm->blocks->max*8.0/1024.0), "");
	PH_PrintMemoryToMainLog(env, " = ", kb, "]\n");
	
	// print total memory usage
	PH_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");
	
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
		
		sup_norm = 0.0;
		
		// stuff for block storage
		int b_n = hddm->blocks->n;
		int b_nnz = hddm->blocks->nnz;
		HDDNode **b_blocks = hddm->blocks->blocks;
		unsigned int *b_rowscols = hddm->blocks->rowscols;
		unsigned char *b_counts = hddm->blocks->counts;
		int *b_starts = (int *)hddm->blocks->counts;
		bool b_use_counts = hddm->blocks->use_counts;
		int *b_offsets = hddm->blocks->offsets;
		HDDNode **b_nodes = hddm->row_tables[hddm->l_b];
		int b_dist_shift = hddm->blocks->dist_shift;
		int b_dist_mask = hddm->blocks->dist_mask;
		int row_offset, col_offset;
		HDDNode *node;
		
		// loop through rows of blocks
		l = b_nnz; h = 0;
		for(fb = 0; fb < b_n; fb++)
		{
			// loop actually over i (can do forwards or backwards psor/pgs)
			i = (forwards) ? fb : b_n-1-fb;
			
			// store block row offset
			row_offset = b_offsets[i];
			
			// initialise (partial) solution vector
			h2 = b_offsets[i+1] - b_offsets[i];
			// initialise vector
			if (b == NULL) {
				for (i2 = 0; i2 < h2; i2++) { soln2[i2] = 0.0; }
			} else if (!compact_b) {
				for (i2 = 0; i2 < h2; i2++) { soln2[i2] = b_vec[row_offset + i2]; }
			} else {
				for (i2 = 0; i2 < h2; i2++) { soln2[i2] = b_dist->dist[b_dist->ptrs[row_offset + i2]]; }
			}
			
			// loop through blocks in this row of blocks
			if (!b_use_counts) { l = b_starts[i]; h = b_starts[i+1]; }
			else if (forwards) { l = h; h += b_counts[i]; }
			else { h = l; l -= b_counts[i]; }
			for(j = l; j < h; j++)
			{
				// get node for block and its col offset
				if (!compact_b) {
					node = b_blocks[j];
					col_offset = b_offsets[b_rowscols[j]];
				} else {
					node = b_nodes[(int)(b_rowscols[j] & b_dist_mask)];
					col_offset = b_offsets[(int)(b_rowscols[j] >> b_dist_shift)];
				}
				
				// recursively traverse block
				psor_rec(node, hddm->l_b, 0, col_offset, transpose);
			}
			
			// do convergence check/update/etc.
			h2 = b_offsets[i+1] - b_offsets[i];
			for (i2 = 0; i2 < h2; i2++) {
				// divide by diagonal
				if (!compact_d) {
					soln2[i2] *= diags_vec[row_offset + i2];
				} else {
					soln2[i2] *= (diags_dist->dist[(int)diags_dist->ptrs[row_offset + i2]]);
				}
				// do over-relaxation if necessary
				if (omega != 1) {
					soln2[i2] = ((1-omega) * soln[row_offset + i2]) + (omega * soln2[i2]);
				}	
				// compute norm for convergence
				x = fabs(soln2[i2] - soln[row_offset + i2]);
				if (term_crit == TERM_CRIT_RELATIVE) {
					x /= soln2[i2];
				}
				if (x > sup_norm) sup_norm = x;
				// set vector element
				soln[row_offset + i2] = soln2[i2];
			}
		}
		
		// check convergence
		if (sup_norm < term_crit_param) {
			done = true;
		}
		
//		PH_PrintToMainLog(env, "%.2f %.2f sec\n", ((double)(util_cpu_time() - start3)/1000), ((double)(util_cpu_time() - start2)/1000)/iters);
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iters/timing info
	PH_PrintToMainLog(env, "\n%sPseudo %s: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", forwards?"":"Backwards ", (omega == 1.0)?"Gauss-Seidel":"SOR", iters, time_taken, time_for_iters/iters, time_for_setup);
	
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
	if (id) Cudd_RecursiveDeref(ddman, id);
	if (diags) Cudd_RecursiveDeref(ddman, diags);
	if (hddm) delete hddm;
	if (diags_vec) delete[] diags_vec;
	if (diags_dist) delete diags_dist;
	if (b_vec) delete[] b_vec;
	if (b_dist) delete b_dist;
	if (soln2) delete[] soln2;
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------

static void psor_rec(HDDNode *hdd, int level, int row_offset, int col_offset, bool transpose)
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
			psor_rm((RMSparseMatrix *)hdd->sm.ptr, row_offset, col_offset);
		} else {
			psor_cmsr((CMSRSparseMatrix *)hdd->sm.ptr, row_offset, col_offset);
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
			psor_rec(e->type.kids.e, level+1, row_offset, col_offset, transpose);
			psor_rec(e->type.kids.t, level+1, row_offset, col_offset+e->off.val, transpose);
		} else {
			psor_rec(e->type.kids.e, level+1, row_offset, col_offset, transpose);
			psor_rec(e->type.kids.t, level+1, row_offset+e->off.val, col_offset, transpose);
		}
	}
	t = hdd->type.kids.t;
	if (t != zero) {
		if (!transpose) {
			psor_rec(t->type.kids.e, level+1, row_offset+hdd->off.val, col_offset, transpose);
			psor_rec(t->type.kids.t, level+1, row_offset+hdd->off.val, col_offset+t->off.val, transpose);
		} else {
			psor_rec(t->type.kids.e, level+1, row_offset, col_offset+hdd->off.val, transpose);
			psor_rec(t->type.kids.t, level+1, row_offset+t->off.val, col_offset+hdd->off.val, transpose);
		}
	}
}

//-----------------------------------------------------------------------------------

static void psor_rm(RMSparseMatrix *rmsm, int row_offset, int col_offset)
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

static void psor_cmsr(CMSRSparseMatrix *cmsrsm, int row_offset, int col_offset)
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
