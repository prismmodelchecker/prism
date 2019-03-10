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

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_PrismSparse_PS_1ImprovedNondetBoundedUntil
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer t,	// trans matrix
jlong __jlongpointer od,	// odd
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer ndv,	// nondet vars
jint num_ndvars,
jlong __jlongpointer y,	// 'yes' states
jlong __jlongpointer m,	// 'maybe' states
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
	DdNode *a = NULL;
	// model stats
	int n, nc;
	long nnz;
	// sparse matrix
	NDSparseMatrix *ndsm = NULL;
	// vectors
	double *yes_vec = NULL, *soln = NULL, *soln2 = NULL, *tmpsoln = NULL;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, j, k, l1, h1, l2, h2, iters, counter;
	double d1, d2, kb, kbt;
	
	// exception handling around whole function
	try {
	
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
	kb = ndsm->mem;
	kbt = kb;
	// print out info
	PS_PrintToMainLog(env, "[n=%d, nc=%d, nnz=%d, k=%d] ", n, nc, nnz, ndsm->k);
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// get vector for yes
	PS_PrintToMainLog(env, "Creating vector for yes... ");
	yes_vec = mtbdd_to_double_vector(ddman, yes, rvars, num_rvars, odd);
	kb = n*8.0/1024.0;
	kbt += kb;
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// create solution/iteration vectors
	PS_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln = new double[n];
	soln2 = new double[n];
	kb = n*8.0/1024.0;
	kbt += 2*kb;
	PS_PrintMemoryToMainLog(env, "[2 x ", kb, "]\n");
	
	// print total memory usage
	PS_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");
	
	// initial solution is yes
	for (i = 0; i < n; i++) {
		soln2[i] = soln[i] = yes_vec[i];
	}
	
	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;
	
	// start iterations
	PS_PrintToMainLog(env, "\nStarting iterations...\n");
	
	// store local copies of stuff
	// store local copies of stuff
	double *non_zeros = ndsm->non_zeros;
	unsigned char *row_counts = ndsm->row_counts;
	bool use_counts = ndsm->use_counts;
	unsigned char *choice_counts = ndsm->choice_counts;
	unsigned int *cols = ndsm->cols;

	
	int *row_starts; 
	int *adv_starts; 
	int *choice_starts;
	
	if(use_counts)
	{
		row_starts = new int[n+1]; //(int *)ndsm->row_counts;  
		choice_starts;
		row_starts[0] = 0;
		for(i = 1; i <= n; i++)
			row_starts[i] = row_starts[i - 1] + row_counts[i-1]; 

		choice_starts = new int[row_starts[n]+1]; 
		choice_starts[0] = 0;

		for(i = 1; i <= row_starts[n]; i++)
			choice_starts[i] = choice_starts[i - 1] + choice_counts[i-1]; 

	}
	else
	{
		row_starts = (int *)ndsm->row_counts;
		choice_starts = (int *)ndsm->choice_counts;
	}


	int* useful_state = new int[n];
	int top = 0;
	int num_of_trans = choice_starts[row_starts[n]];

	int* back_start, *back_state;
	back_start = new int[n + 1];
	int *back_freq = new int[n + 1];
	back_state = new int[num_of_trans+10];

	int _not = 0;

	for(i = 0; i < n; i++)
		back_freq[i] = 0;

	for(i = 0; i < n; i++)
	{
		for(j = row_starts[i]; j < row_starts[i + 1]; j++)
			for(k = choice_starts[j]; k < choice_starts[j+1]; k++)
				back_freq[cols[k]]++;	
	}

	back_start[0] = 0;
	for(i = 1; i <= n; i++)
		back_start[i] = back_start[i-1] + back_freq[i-1];

	for(i = 0; i <= n; i++)
		back_freq[i] = back_start[i];

	for(i = 0; i < n; i++)
		for(j = row_starts[i]; j < row_starts[i+1]; j++)
			for(k = choice_starts[j]; k < choice_starts[j+1]; k++){
				back_state[back_freq[cols[k]]++] = i;				
			}

	int *Dir_row_starts = new int[n+1];
	int *Non_Dir_row_starts = new int[n+1];
	Dir_row_starts[0] = 0;
	Non_Dir_row_starts[0] = 0;

	int *selected = new int[n];
	for(i = 0; i < n; i++)
		selected[i] = 0;
//	for(i = 0; i < n; i++)
//		if(yes_vec[i] >= 1)
//			for(j = back_start[i]; j < back_start[i+1]; j++)
//				selected[back_state[j]] = 0;
	

	double* Dir_nnz = new double[row_starts[n]+1];
	double* Non_Dir_nnz = new double[row_starts[n]+1];
	
	int* Dir_cols = new int[row_starts[n]+1];
	int* Non_Dir_cols = new int[row_starts[n]+1];	

	int *Dir_choice_starts = new int[row_starts[n]+1];
	int *Non_Dir_choice_starts = new int[row_starts[n]+1];
	Non_Dir_choice_starts[0] = 0;
	
	int Dir_ind, Non_Dir_ind, Non_Dir_chs, m;
	Dir_ind = Non_Dir_ind = Non_Dir_chs = 0;
	for(i = 0; i < n; i++)
	{
		if(row_starts[i] < row_starts[i+1])
			useful_state[top++] = i;

		for(j = row_starts[i]; j < row_starts[i+1]; j++)
			if(choice_starts[j+1] - choice_starts[j] == 1)
			{
				Dir_cols[Dir_ind++] = cols[choice_starts[j]];
			}
			else
			{
				for(k = choice_starts[j]; k < choice_starts[j+1]; k++)
				{	
					Non_Dir_cols[Non_Dir_ind] = cols[k];
					Non_Dir_nnz[Non_Dir_ind++] = non_zeros[k];				
				}
				Non_Dir_choice_starts[++Non_Dir_chs] = Non_Dir_ind;
			}
		Dir_row_starts[i + 1] = Dir_ind;
		Non_Dir_row_starts[i + 1] = Non_Dir_chs;
 
	}
	bool flag = true;
	double d3;
	// note that we ignore max_iters as we know how any iterations _should_ be performed
	for (iters = 0; iters < bound; iters++) {
		
		if(flag && iters % 10 == 5)
		{
			counter = 0;
			for(i = 0; i < n; i++)
				if(selected[i] == iters) counter++;
			if(counter > top * .3)
				flag = false;
		}	
		//printf("\n %d", counter);	
		// do matrix multiplication and min/max
		h1 = h2 = 0;
		if(flag)
		for (m = 0; m < top; m++) {
			i = useful_state[m];
			if(selected[i] < iters)
			{
				soln2[i] = soln[i];
				continue;
			}	
			d1 = min ? 2 : -1;
			for(j = Non_Dir_row_starts[i]; j < Non_Dir_row_starts[i+1]; j++)
			{
				d2 = 0;
				for(k = Non_Dir_choice_starts[j]; k < Non_Dir_choice_starts[j + 1]; k++)
					d2 += Non_Dir_nnz[k] * soln[Non_Dir_cols[k]];
				if (min){ 
					if (d2 < d1) d1 = d2;}
				else 
					if (d2 > d1) d1 = d2;				
			}
			for(j = Dir_row_starts[i]; j < Dir_row_starts[i + 1]; j++)
			{
				d2 = soln[Dir_cols[j]];
				if (min) 
					{if (d2 < d1) d1 = d2;}
				else
					if (d2 > d1) d1 = d2;
			}

			// set vector element
			// (if no choices, use value of yes)
			l1 = row_starts[i]; h1 = row_starts[i+1];
			if(d1 > soln2[i])
			{
				for(j = back_start[i]; j < back_start[i+1]; j++)
					selected[back_state[j]] = iters + 1;		
			}
			soln2[i] = (h1 > l1) ? d1 : yes_vec[i];
		}
		else
			for (i = 0; i < n; i++) {
			d1 = min ? 2 : -1;
			for(j = Non_Dir_row_starts[i]; j < Non_Dir_row_starts[i+1]; j++)
			{
				d2 = 0;
				for(k = Non_Dir_choice_starts[j]; k < Non_Dir_choice_starts[j + 1]; k++)
					d2 += Non_Dir_nnz[k] * soln[Non_Dir_cols[k]];
				if (min){ 
					if (d2 < d1) d1 = d2;}
				else 
					if (d2 > d1) d1 = d2;				
			}
			for(j = Dir_row_starts[i]; j < Dir_row_starts[i + 1]; j++)
			{
				d2 = soln[Dir_cols[j]];
				if (min) 
					{if (d2 < d1) d1 = d2;}
				else
					if (d2 > d1) d1 = d2;
			}

			// set vector element
			// (if no choices, use value of yes)
			l1 = row_starts[i]; h1 = row_starts[i+1];
			soln2[i] = (h1 > l1) ? d1 : yes_vec[i];
		}


		
		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PS_PrintToMainLog(env, "Iteration %d (of %d): ", iters, bound);
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
	
	// print iterations/timing info
	PS_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_for_iters, time_for_iters/iters, time_for_setup);
	
	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PS_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}
	
	// free memory
	if (a) Cudd_RecursiveDeref(ddman, a);
	if (ndsm) delete ndsm;
	if (yes_vec) delete[] yes_vec;
	if (soln2) delete[] soln2;
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------
