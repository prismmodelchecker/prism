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
#include "PrismSparseGlob.h"
#include "jnipointer.h"
#include "prism.h"
#include <new>

//------------------------------------------------------------------------------

// solve the linear equation system Ax=x with the Power method
// in addition, solutions may be provided for additional states in the vector b
// these states are assumed not to have non-zero rows in the matrix A

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_PrismSparse_PS_1Power
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
	long nnz;
	// flags
	bool compact_a, compact_b;
	// sparse matrix
	RMSparseMatrix *rmsm = NULL;
	CMSRSparseMatrix *cmsrsm = NULL;
	// vectors
	double *b_vec = NULL, *soln = NULL, *soln2 = NULL, *tmpsoln = NULL;
	DistVector *b_dist = NULL;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, j, l, h, iters;
	double d, x, sup_norm, kb, kbt;
	bool done;
	
	// exception handling around whole function
	try {
	
	// start clocks
	start1 = start2 = util_cpu_time();
	
	// get number of states
	n = odd->eoff + odd->toff;
	
	// make local copy of a
	Cudd_Ref(a);
	
	// build sparse matrix
	PS_PrintToMainLog(env, "\nBuilding sparse matrix... ");
	// if requested, try and build a "compact" version
	compact_a = true;
	cmsrsm = NULL;
	if (compact) cmsrsm = build_cmsr_sparse_matrix(ddman, a, rvars, cvars, num_rvars, odd, transpose);
	if (cmsrsm != NULL) {
		nnz = cmsrsm->nnz;
		kb = cmsrsm->mem;
	}
	// if not or if it wasn't possible, built a normal one
	else {
		compact_a = false;
		rmsm = build_rm_sparse_matrix(ddman, a, rvars, cvars, num_rvars, odd, transpose);
		nnz = rmsm->nnz;
		kb = rmsm->mem;
	}
	kbt = kb;
	// print some info
	PS_PrintToMainLog(env, "[n=%d, nnz=%d%s] ", n, nnz, compact_a?", compact":"");
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// build b vector (if present)
	if (b != NULL) {
		PS_PrintToMainLog(env, "Creating vector for RHS... ");
		b_vec = mtbdd_to_double_vector(ddman, b, rvars, num_rvars, odd);
		// try and convert to compact form if required
		compact_b = false;
		if (compact) {
			if ((b_dist = double_vector_to_dist(b_vec, n))) {
				compact_b = true;
				delete b_vec; b_vec = NULL;
			}
		}
		kb = (!compact_b) ? n*8.0/1024.0 : (b_dist->num_dist*8.0+n*2.0)/1024.0;
		kbt += kb;
		if (compact_b) PS_PrintToMainLog(env, "[dist=%d, compact] ", b_dist->num_dist);
		PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	}
	
	// create solution/iteration vectors
	PS_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = mtbdd_to_double_vector(ddman, init, rvars, num_rvars, odd);
	soln2 = new double[n];
	kb = n*8.0/1024.0;
	kbt += 2*kb;
	PS_PrintMemoryToMainLog(env, "[2 x ", kb, "]\n");
	
	// print total memory usage
	PS_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");
	
	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;
	
	// start iterations
	iters = 0;
	done = false;
	PS_PrintToMainLog(env, "\nStarting iterations...\n");
	
	while (!done && iters < max_iters) {
		
		iters++;
		
		// store local copies of stuff
		double *non_zeros;
		unsigned char *row_counts;
		int *row_starts;
		bool use_counts;
		unsigned int *cols;
		double *dist;
		int dist_shift;
		int dist_mask;
		if (!compact_a) {
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
		
		// matrix multiply
		h = 0;
		for (i = 0; i < n; i++) {
			
			d = (b == NULL) ? 0.0 : ((!compact_b) ? b_vec[i] : b_dist->dist[b_dist->ptrs[i]]);
			if (!use_counts) { l = row_starts[i]; h = row_starts[i+1]; }
			else { l = h; h += row_counts[i]; }
			// "row major" version
			if (!compact_a) {
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
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iters/timing info
	PS_PrintToMainLog(env, "\nPower method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	// if the iterative method didn't terminate, this is an error
	if (!done) { delete soln; soln = NULL; PS_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters); }
	
	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PS_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}
	
	// free memory
	if (a) Cudd_RecursiveDeref(ddman, a);
	if (rmsm) delete rmsm;
	if (cmsrsm) delete cmsrsm;
	if (b_vec) delete[] b_vec;
	if (b_dist) delete b_dist;
	if (soln2) delete[] soln2;
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------
