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
#include <cmath>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include <dv.h>
#include <prism.h>
#include "sparse.h"
#include "PrismSparseGlob.h"
#include "jnipointer.h"
#include "Measures.h"
#include <new>

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_PrismSparse_PS_1ProbTransient
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
	long nnz;
	// flags
	bool compact_tr;
	// sparse matrix
	CMSparseMatrix *cmsm = NULL;
	CMSCSparseMatrix *cmscsm = NULL;
	// vectors
	double *soln = NULL, *soln2 = NULL, *tmpsoln = NULL;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	bool done;
	int i, j, l, h, iters;
	double d, kb, kbt;
	// measure for convergence termination check
	MeasureSupNorm measure(term_crit == TERM_CRIT_RELATIVE);
	
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
	cmscsm = NULL;
	if (compact) cmscsm = build_cmsc_sparse_matrix(ddman, trans, rvars, cvars, num_rvars, odd);
	if (cmscsm != NULL) {
		nnz = cmscsm->nnz;
		kb = cmscsm->mem;
	}
	// if not or if it wasn't possible, built a normal one
	else {
		compact_tr = false;
		cmsm = build_cm_sparse_matrix(ddman, trans, rvars, cvars, num_rvars, odd);
		nnz = cmsm->nnz;
		kb = cmsm->mem;
	}
	kbt = kb;
	// print some info
	PS_PrintToMainLog(env, "[n=%d, nnz=%ld%s] ", n, nnz, compact_tr?", compact":"");
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// create solution/iteration vectors
	PS_PrintToMainLog(env, "Allocating iteration vectors... ");
	// for soln, we just use init (since we are free to modify/delete this vector)
	// we also report the memory usage of this vector here, even though it has already been created
	soln = init;
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
	
	// note that we ignore max_iters as we know how any iterations _should_ be performed
	for (iters = 0; iters < time && !done; iters++) {
		
		// store local copies of stuff
		double *non_zeros;
		unsigned char *col_counts;
		int *col_starts;
		bool use_counts;
		unsigned int *rows;
		double *dist;
		int dist_shift;
		int dist_mask;
		if (!compact_tr) {
			non_zeros = cmsm->non_zeros;
			col_counts = cmsm->col_counts;
			col_starts = (int *)cmsm->col_counts;
			use_counts = cmsm->use_counts;
			rows = cmsm->rows;
		} else {
			col_counts = cmscsm->col_counts;
			col_starts = (int *)cmscsm->col_counts;
			use_counts = cmscsm->use_counts;
			rows = cmscsm->rows;
			dist = cmscsm->dist;
			dist_shift = cmscsm->dist_shift;
			dist_mask = cmscsm->dist_mask;
		}
		
		// do matrix vector multiply bit
		h = 0;
		for (i = 0; i < n; i++) {
			d = 0.0;
			if (!use_counts) { l = col_starts[i]; h = col_starts[i+1]; }
			else { l = h; h += col_counts[i]; }
			// "column major" version
			if (!compact_tr) {
				for (j = l; j < h; j++) {
					d += non_zeros[j] * soln[rows[j]];
				}
			// "compact msc" version
			} else {
				for (j = l; j < h; j++) {
					d += dist[(int)(rows[j] & dist_mask)] * soln[(int)(rows[j] >> dist_shift)];
				}
			}
			// set vector element
			soln2[i] = d;
		}
		
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
			PS_PrintToMainLog(env, "Iteration %d (of %d): ", iters, (int)time);
			if (do_ss_detect) PS_PrintToMainLog(env, "max %sdiff=%f, ", measure.isRelative()?"relative ":"", measure.value());
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
	
	// print iters/timing info
	if (done) PS_PrintToMainLog(env, "\nSteady state detected at iteration %d\n", iters);
	PS_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PS_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}
	
	// free memory
	if (cmsm) delete cmsm;
	if (cmscsm) delete cmscsm;
	if (soln2) delete[] soln2;
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------
