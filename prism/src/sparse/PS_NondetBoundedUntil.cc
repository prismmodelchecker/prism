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

//------------------------------------------------------------------------------

JNIEXPORT jlong __pointer JNICALL Java_sparse_PrismSparse_PS_1NondetBoundedUntil
(
JNIEnv *env,
jclass cls,
jlong __pointer t,	// trans matrix
jlong __pointer od,	// odd
jlong __pointer rv,	// row vars
jint num_rvars,
jlong __pointer cv,	// col vars
jint num_cvars,
jlong __pointer ndv,	// nondet vars
jint num_ndvars,
jlong __pointer y,	// 'yes' states
jlong __pointer m,	// 'maybe' states
jint bound,		// time bound
jboolean min		// min or max probabilities (true = min, false = max)
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(t);		// trans matrix
	ODDNode *odd = jlong_to_ODDNode(od); 		// reachable states
	DdNode **rvars = jlong_to_DdNode_array(rv); 	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv); 	// col vars
	DdNode **ndvars = jlong_to_DdNode_array(ndv);	// nondet vars
	DdNode *yes = jlong_to_DdNode(y);		// 'yes' states
	DdNode *maybe = jlong_to_DdNode(m); 		// 'maybe' states

	// mtbdds
	DdNode *a;
	// model stats
	int n, nc;
	long nnz;
	// sparse matrix
	NDSparseMatrix *ndsm;
	// vectors
	double *yes_vec, *soln, *soln2, *tmpsoln;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, j, k, l1, h1, l2, h2, iters;
	double d1, d2, kb, kbt;
	
	// start clocks	
	start1 = start2 = util_cpu_time();
	
	// get a - filter out rows
	Cudd_Ref(trans);
	Cudd_Ref(maybe);
	a = DD_Apply(ddman, APPLY_TIMES, trans, maybe);
	
	// get number of states
	n = odd->eoff + odd->toff;
	
	// build sparse matrix
	PS_PrintToMainLog(env, "\nBuilding sparse matrix... ");
	ndsm = build_nd_sparse_matrix(ddman, a, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	// get number of transitions/choices
	nnz = ndsm->nnz;
	nc = ndsm->nc;
	// print out info
	PS_PrintToMainLog(env, "[n=%d, nc=%d, nnz=%d, k=%d] ", n, nc, nnz, ndsm->k);
	kb = ndsm->mem;
	kbt = kb;
	PS_PrintToMainLog(env, "[%.1f KB]\n", kb);
	
	// get vector for yes
	PS_PrintToMainLog(env, "Creating vector for yes... ");
	yes_vec = mtbdd_to_double_vector(ddman, yes, rvars, num_rvars, odd);
	kb = n*8.0/1024.0;
	kbt += kb;
	PS_PrintToMainLog(env, "[%.1f KB]\n", kb);
	
	// create solution/iteration vectors
	PS_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = new double[n];
	soln2 = new double[n];
	kb = n*8.0/1024.0;
	kbt += 2*kb;
	PS_PrintToMainLog(env, "[2 x %.1f KB]\n", kb);
	
	// print total memory usage
	PS_PrintToMainLog(env, "TOTAL: [%.1f KB]\n", kbt);
	
	// initial solution is yes
	for (i = 0; i < n; i++) {
		soln[i] = yes_vec[i];
	}
	
	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	
	// start iterations
	PS_PrintToMainLog(env, "\nStarting iterations...\n");
	
	// note that we ignore max_iters as we know how any iterations _should_ be performed
	for (iters = 0; iters < bound; iters++) {
		
//		PS_PrintToMainLog(env, "iter %d\n", iters);
//		start3 = util_cpu_time();
		
		// store local copies of stuff
		double *non_zeros = ndsm->non_zeros;
		unsigned char *row_counts = ndsm->row_counts;
		int *row_starts = (int *)ndsm->row_counts;
		unsigned char *choice_counts = ndsm->choice_counts;
		int *choice_starts = (int *)ndsm->choice_counts;
		bool use_counts = ndsm->use_counts;
		unsigned int *cols = ndsm->cols;
		
		// do matrix multiplication and min/max
		h1 = h2 = 0;
		for (i = 0; i < n; i++) {
			d1 = min ? 2 : -1;
			if (!use_counts) { l1 = row_starts[i]; h1 = row_starts[i+1]; }
			else { l1 = h1; h1 += row_counts[i]; }
			for (j = l1; j < h1; j++) {
				d2 = 0;
				if (!use_counts) { l2 = choice_starts[j]; h2 = choice_starts[j+1]; }
				else { l2 = h2; h2 += choice_counts[j]; }
				for (k = l2; k < h2; k++) {
					d2 += non_zeros[k] * soln[cols[k]];
				}
				if (min) {
					if (d2 < d1) d1 = d2;
				} else {
					if (d2 > d1) d1 = d2;
				}
			}
			// set vector element
			// (if no choices, use value of yes)
			soln2[i] = (h1 > l1) ? d1 : yes_vec[i];
		}
		
		// prepare for next iteration
		tmpsoln = soln;
		soln = soln2;
		soln2 = tmpsoln;
		
//		PS_PrintToMainLog(env, "%.2f %.2f sec\n", ((double)(util_cpu_time() - start3)/1000), ((double)(util_cpu_time() - start2)/1000)/iters);
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iterations/timing info
	PS_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	// free memory
	Cudd_RecursiveDeref(ddman, a);
	free_nd_sparse_matrix(ndsm);
	delete yes_vec;
	delete soln2;
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------
