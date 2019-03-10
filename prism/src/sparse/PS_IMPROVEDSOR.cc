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
#include "sparse.h"
#include "PrismSparseGlob.h"
#include "jnipointer.h"
#include "prism.h"
#include "Measures.h"
#include "ExportIterations.h"
#include <memory>
#include <new>

//------------------------------------------------------------------------------

// solve the linear equation system Ax=b with Gauss-Seidel/SOR

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_PrismSparse_PS_1IMPROVEDSOR
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
	int maybe_states = 0, M0;	

	// mtbdds
	DdNode *reach = NULL, *diags = NULL, *id = NULL;
	// model stats
	int n;
	long nnz;
	// flags
	bool compact_a, compact_d, compact_b;
	// sparse matrix
	RMSparseMatrix *rmsm = NULL;
	CMSRSparseMatrix *cmsrsm = NULL;
	// vectors
	double *diags_vec = NULL, *b_vec = NULL, *soln = NULL;
	DistVector *diags_dist = NULL, *b_dist = NULL;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, j, fb, l, h, iters;
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
	
	// get vector of diags, either by extracting from mtbdd or
	// by doing (negative, non-diagonal) row sums of original A matrix
	PS_PrintToMainLog(env, "Creating vector for diagonals... ");
	if (!row_sums) {
		diags = DD_MaxAbstract(ddman, diags, cvars, num_cvars);
		diags_vec = mtbdd_to_double_vector(ddman, diags, rvars, num_rvars, odd);
	} else {
		diags_vec = compact_a ? cmsr_negative_row_sums(cmsrsm, transpose) : rm_negative_row_sums(rmsm, transpose);
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
			delete diags_vec; diags_vec = NULL;
		}
	}
	kb = (!compact_d) ? n*8.0/1024.0 : (diags_dist->num_dist*8.0+n*2.0)/1024.0;
	kbt += kb;
	if (compact_d) PS_PrintToMainLog(env, "[dist=%d, compact] ", diags_dist->num_dist);
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// invert diagonal
	if (!compact_d) {
		for (i = 0; i < n; i++) diags_vec[i] = 1.0 / diags_vec[i];
	} else {
		for (i = 0; i < diags_dist->num_dist; i++) diags_dist->dist[i] = 1.0 / diags_dist->dist[i];
	}
	
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
	PS_PrintToMainLog(env, "Allocating iteration vector... ");
	soln = mtbdd_to_double_vector(ddman, init, rvars, num_rvars, odd);
	kb = n*8.0/1024.0;
	kbt += kb;
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// print total memory usage
	PS_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");

	std::unique_ptr<ExportIterations> iterationExport;
	if (PS_GetFlagExportIterations()) {
		std::string title("PS_SOR (");
		title += forwards?"":"Backwards ";
		title += (omega == 1.0)?"Gauss-Seidel":("SOR omega=" + std::to_string(omega));
		title += ")";
		iterationExport.reset(new ExportIterations(title.c_str()));
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
	PS_PrintToMainLog(env, "\nStarting iterations...\n");
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
		
		use_counts = rmsm->use_counts;
		cols = rmsm->cols;
		if(use_counts)
		{
			row_starts = new int[n+1]; 
			row_starts[0] = 0;
			for(i = 1; i <= n; i++)
				row_starts[i] = row_starts[i - 1] + row_counts[i-1]; 
		}
		else
			row_starts = (int *)rmsm->row_counts;;		
		} else {
		row_counts = cmsrsm->row_counts;
		row_starts = (int *)cmsrsm->row_counts;
		use_counts = cmsrsm->use_counts;
		cols = cmsrsm->cols;
		dist = cmsrsm->dist;
		dist_shift = cmsrsm->dist_shift;
		dist_mask = cmsrsm->dist_mask;
	}

	int top, kk, M1, M2, l2, h2, m;
	int num_trans = row_starts[n];
	int* dirac_group = new int[n + 2];
	int* stack = new int[n + 2];
	bool* stacked = new bool[n + 2];
	int* useful_states = new int[n];
	int* uf_choice_strt = new int[n];
	int* uf_cols = new int[num_trans];
	double* uf_nnz = new double[num_trans];
	double sup_norm, x;
	
	for(i = 0; i < n; i++)
	{
		if(row_starts[i+1] <= row_starts[i])
		{
			dirac_group[i] = i;
			stacked[i] = true;		
		}
		else
		{
			dirac_group[i] = -1;
			stacked[i] = false;
		}
	}
	for(i = 0; i < n; i++)
		if(dirac_group[i] == -1)
		{
			top = 0;
			kk = i;
			while(stacked[kk] == false)	
			{
				stacked[kk] = true;
				stack[top++] = kk;
				l2 = row_starts[kk];
				h2 = row_starts[1 + kk];
				if(h2 - l2 == 1)
					kk = cols[l2];
				else
					break;
			}
			if(dirac_group[kk] < 0)
				dirac_group[kk] = kk;
			j = dirac_group[kk];
			while(top-- > 0)
			{
				kk = stack[top];
				dirac_group[kk] = j;
			}
		}

	M0 = M1 = M2 = 0;
	for(i = 0; i < n; i++)
	{			
		if(row_starts[i] >= row_starts[i+1])
			continue;
		if(dirac_group[i] == i)
		{
			useful_states[M0] = i;
			uf_choice_strt[M0] = M1;
			for(j = 0; j < row_starts[1 + i] - row_starts[i]; j++)
			{
				M2 = row_starts[i];
				if(dirac_group[cols[M2 + j]] >= 0)
				   	uf_cols[M1 + j] = dirac_group[cols[M2 + j]];
				else
					uf_cols[M1 + j] = cols[M2 + j];
				uf_nnz[M1 + j] = non_zeros[M2 + j];
			}
			M0++;
			M1 += j;
		}
		uf_choice_strt[M0] = M1;
	}

		while(!done)
		{
			iters++;
			sup_norm = 0;
		
			for(m = 0; m < M0; m++){	
				i = useful_states[m];
				
				d = (b == NULL) ? 0.0 : ((!compact_b) ? b_vec[i] : b_dist->dist[b_dist->ptrs[i]]);
				if(row_starts[i] >= row_starts[i+1] || i != dirac_group[i])
					continue;	

				l = uf_choice_strt[m];
				h = uf_choice_strt[m + 1];

				for(j = l; j < h; j++)
					d -= uf_nnz[j] * soln[uf_cols[j]];

				x = (d - soln[i]);
				soln[i] = d;
				if (term_crit == TERM_CRIT_RELATIVE && soln[i] > 0) {
					x /= soln[i];
				}
				if (x > sup_norm) 
					sup_norm = x;
			}
			if (sup_norm < term_crit_param) 
				done = true;					
		}
		for(i = 0; i < n; i++)		
		{
			if(row_starts[i] < row_starts[i+1])
			if(dirac_group[i] > 0)			
				soln[i] = soln[dirac_group[i]];
		}


	done = false;
	while (!done && iters < max_iters) {
		
		iters++;
		
		measure.reset();
		
		// store local copies of stuff
		
		// matrix multiply
		l = nnz; h = 0;
		for (i = 0; i < n; i++) {
			
			// loop actually over i
			// (can do forwards or backwards sor/gs)
			
			d = (b == NULL) ? 0.0 : ((!compact_b) ? b_vec[i] : b_dist->dist[b_dist->ptrs[i]]);
			l = row_starts[i]; h = row_starts[i+1]; 
			// "row major" version
			if (!compact_a) {
				for (j = l; j < h; j++) {
					d -= non_zeros[j] * soln[cols[j]];
				}
			// "compact msr" version
			} else {
				for (j = l; j < h; j++) {
					d -= dist[(int)(cols[j] & dist_mask)] * soln[(int)(cols[j] >> dist_shift)];
				}
			}
			// divide by diagonal (multiply by inverted diagonal)
			if (!compact_d) d *= diags_vec[i]; else d *= diags_dist->dist[diags_dist->ptrs[i]];
			// over-relaxation
			if (omega != 1.0) {
				d = ((1-omega) * soln[i]) + (omega * d);
			}
			// compute norm for convergence
			// (note we must do this inside the loop because we only store one vector for sor/gauss-seidel)
			measure.measure(soln[i], d);
			// set vector element
			soln[i] = d;
		}

		if (iterationExport)
			iterationExport->exportVector(soln, n, 0);

		// check convergence
		if (measure.value() < term_crit_param) {
			done = true;
		}
		
		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PS_PrintToMainLog(env, "Iteration %d: max %sdiff=%f", iters, measure.isRelative()?"relative ":"", measure.value());
			PS_PrintToMainLog(env, ", %.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
			start3 = util_cpu_time();
		}
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iters/timing info
	PS_PrintToMainLog(env, "\n%s%s: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", forwards?"":"Backwards ", (omega == 1.0)?"Gauss-Seidel":"SOR", iters, time_for_iters, time_for_iters/iters, time_for_setup);
	
	// if the iterative method didn't terminate, this is an error
	if (!done) { delete[] soln; soln = NULL; PS_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters); }
	
	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PS_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}
	
	// free memory
	if (a) Cudd_RecursiveDeref(ddman, a);
	if (id) Cudd_RecursiveDeref(ddman, id);
	if (diags) Cudd_RecursiveDeref(ddman, diags);
	if (rmsm) delete rmsm;
	if (cmsrsm) delete cmsrsm;
	if (diags_vec) delete[] diags_vec;
	if (diags_dist) delete diags_dist;
	if (b_vec) delete[] b_vec;
	if (b_dist) delete b_dist;
	printf("\nNumber of state Updates: %dM \n", (int) ((M0 * iters) / 1000000));
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------
