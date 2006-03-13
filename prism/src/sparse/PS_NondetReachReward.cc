//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker
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

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_sparse_PrismSparse_PS_1NondetReachReward
(
JNIEnv *env,
jclass cls,
jint t,			// trans matrix
jint sr,		// state rewards
jint trr,		// transition rewards
jint od,		// odd
jint rv,		// row vars
jint num_rvars,
jint cv,		// col vars
jint num_cvars,
jint ndv,		// nondet vars
jint num_ndvars,
jint g,			// 'goal' states
jint in,		// 'inf' states
jint m,			// 'maybe' states
jboolean min	// min or max probabilities (true = min, false = max)
)
{
	// cast function parameters
	DdNode *trans = (DdNode *)t;		// trans matrix
	DdNode *state_rewards = (DdNode *)sr;	// state rewards
	DdNode *trans_rewards = (DdNode *)trr;	// transition rewards
	ODDNode *odd = (ODDNode *)od; 		// reachable states
	DdNode **rvars = (DdNode **)rv; 	// row vars
	DdNode **cvars = (DdNode **)cv; 	// col vars
	DdNode **ndvars = (DdNode **)ndv;	// nondet vars
	DdNode *goal = (DdNode *)g;	// 'goal' states
	DdNode *inf = (DdNode *)in; 	// 'inf' states
	DdNode *maybe = (DdNode *)m; 	// 'maybe' states
	// mtbdds
	DdNode *reach, *a, *tmp;
	// model stats
	int n, nc;
	long nnz;
	// sparse matrix
	NDSparseMatrix *ndsm, *ndsmr;
	// vectors
	double *yes_vec, *soln, *soln2, *tmpsoln;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, j, k, l1, h1, l2, h2, iters;
	double d1, d2, kb, kbt;
	bool done;
	
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
	
	// also remove goal and infinity states from state rewards vector
	Cudd_Ref(state_rewards);
	Cudd_Ref(maybe);
	state_rewards = DD_Apply(ddman, APPLY_TIMES, state_rewards, maybe);
	
	// and from transition rewards matrix
	Cudd_Ref(trans_rewards);
	Cudd_Ref(maybe);
	trans_rewards = DD_Apply(ddman, APPLY_TIMES, trans_rewards, maybe);
	
	DD_PrintInfo(ddman, a, num_rvars+num_cvars+num_ndvars);
	DD_PrintInfo(ddman, trans_rewards, num_rvars+num_cvars+num_ndvars);
	
	// build sparse matrix (probs)
	PS_PrintToMainLog(env, "\nBuilding sparse probability matrix... ");
	ndsm = build_nd_sparse_matrix(ddman, a, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	// get number of transitions/choices
	nnz = ndsm->nnz;
	nc = ndsm->nc;
	// print out info
	PS_PrintToMainLog(env, "[n=%d, nc=%d, nnz=%d, k=%d] ", n, nc, nnz, ndsm->k);
	kb = (nnz*12.0+nc*4.0+n*4.0)/1024.0;
	kbt = kb;
	PS_PrintToMainLog(env, "[%.1f KB]\n", kb);
	
	// build sparse matrix (rewards)
	PS_PrintToMainLog(env, "\nBuilding sparse reward matrix... ");
	ndsmr = build_nd_sparse_matrix(ddman, trans_rewards, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	// get number of transitions/choices
	nnz = ndsmr->nnz;
	nc = ndsmr->nc;
	// print out info
	PS_PrintToMainLog(env, "[n=%d, nc=%d, nnz=%d, k=%d] ", n, nc, nnz, ndsmr->k);
	kb = (nnz*12.0+nc*4.0+n*4.0)/1024.0;
	kbt = kb;
	PS_PrintToMainLog(env, "[%.1f KB]\n", kb);
	
	return 0;
/*	
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
//		if (soln[i]) printf("yes[%d] := %f;\n", i+1, yes[i]);
	}

	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;

	// start iterations
	iters = 0;
	done = false;
	PS_PrintToMainLog(env, "\nStarting iterations...\n");

	while (!done && iters < max_iters) {
	
		iters++;

//		PS_PrintToMainLog(env, "iter %d\n", iters);
//		start3 = util_cpu_time();

		// do matrix multiplication and min/max
		double *non_zeros = ndsm->non_zeros;
		int *cols = ndsm->cols;
		int *choice_starts = ndsm->choice_starts;
		int *row_starts = ndsm->row_starts;
		for (i = 0; i < n; i++) {
			d1 = min ? 2 : -1;
			l1 = row_starts[i];
			h1 = row_starts[i+1];
			for (j = l1; j < h1; j++) {
				d2 = 0;
				l2 = choice_starts[j];
				h2 = choice_starts[j+1];
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
		
//		Ps_PrintToMainLog(env, "%.2f %.2f sec\n", ((double)(util_cpu_time() - start3)/1000), ((double)(util_cpu_time() - start2)/1000)/iters);
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
	
	// if the iterative method didn't terminate, this is an error
	if (!done) { delete soln; PS_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increase the maximum number of iterations", iters); return 0; }
	
	return (int)soln;*/
}

//------------------------------------------------------------------------------
