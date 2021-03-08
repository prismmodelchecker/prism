//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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
#include "IntervalIteration.h"
#include <memory>
#include <new>

//------------------------------------------------------------------------------

// solve the linear equation system Ax=b with Jacobi/JOR, interval variant

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_PrismSparse_PS_1JORInterval
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
jlong __jlongpointer _lower,	// lower bound values
jlong __jlongpointer _upper,	// upper bound values
jboolean transpose,	// transpose A? (i.e. solve xA=b not Ax=b?)
jboolean row_sums,	// use row sums for diags instead? (strictly speaking: negative sum of non-diagonal row elements)
jdouble omega,		// omega (over-relaxation parameter)
jint flags
)
{
	// cast function parameters
	ODDNode *odd = jlong_to_ODDNode(_odd);		// odd
	DdNode **rvars = jlong_to_DdNode_array(rv); 	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv); 	// col vars
	DdNode *a = jlong_to_DdNode(_a);		// matrix A
	DdNode *b = jlong_to_DdNode(_b);		// vector b
	DdNode *lower = jlong_to_DdNode(_lower);		// lower bound values
	DdNode *upper = jlong_to_DdNode(_upper);		// upper bound values

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
	double *diags_vec = NULL, *b_vec = NULL, *soln_below = NULL, *soln_below2 = NULL, *soln_above = NULL, *soln_above2 = NULL, *tmpsoln = NULL;
	DistVector *diags_dist = NULL, *b_dist = NULL;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, j, l, h, iters;
	double d_below, d_above, kb, kbt;
	bool done;
	// measure for convergence termination check
	MeasureSupNorm measure(term_crit == TERM_CRIT_RELATIVE);

	if (omega <= 0.0 || omega > 1.0) {
		PS_SetErrorMessage("Interval iteration requires 0 < omega <= 1.0, have omega = %g", omega);
		return ptr_to_jlong(NULL);
	}

	IntervalIteration helper(flags);

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
	PS_PrintToMainLog(env, "[n=%d, nnz=%ld%s] ", n, nnz, compact_a?", compact":"");
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
	PS_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln_below = mtbdd_to_double_vector(ddman, lower, rvars, num_rvars, odd);
	soln_above = mtbdd_to_double_vector(ddman, upper, rvars, num_rvars, odd);
	soln_below2 = new double[n];
	soln_above2 = new double[n];
	kb = n*8.0/1024.0;
	kbt += 4*kb;
	PS_PrintMemoryToMainLog(env, "[4 x ", kb, "]\n");
	
	// print total memory usage
	PS_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");

	std::unique_ptr<ExportIterations> iterationExport;
	if (PS_GetFlagExportIterations()) {
		std::string title("PS_JOR (");
		title += (omega == 1.0)?"Jacobi":("JOR omega=" + std::to_string(omega));
		title += "), interval";

		iterationExport.reset(new ExportIterations(title.c_str()));
		PS_PrintToMainLog(env, "Exporting iterations to %s\n", iterationExport->getFileName().c_str());
		iterationExport->exportVector(soln_below, n, 0);
		iterationExport->exportVector(soln_above, n, 1);
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
		
		// matrix multiply (below and above simultaneously)
		h = 0;
		for (i = 0; i < n; i++) {
			
			d_below = (b == NULL) ? 0.0 : ((!compact_b) ? b_vec[i] : b_dist->dist[b_dist->ptrs[i]]);
			d_above = d_below;
			if (!use_counts) { l = row_starts[i]; h = row_starts[i+1]; }
			else { l = h; h += row_counts[i]; }
			// "row major" version
			if (!compact_a) {
				for (j = l; j < h; j++) {
					d_below -= non_zeros[j] * soln_below[cols[j]];
					d_above -= non_zeros[j] * soln_above[cols[j]];
				}
			// "compact msr" version
			} else {
				for (j = l; j < h; j++) {
					d_below -= dist[(int)(cols[j] & dist_mask)] * soln_below[(int)(cols[j] >> dist_shift)];
					d_above -= dist[(int)(cols[j] & dist_mask)] * soln_above[(int)(cols[j] >> dist_shift)];
				}
			}
			// divide by diagonal (multiply by inverted diagonal)
			if (!compact_d) d_below *= diags_vec[i]; else d_below *= diags_dist->dist[diags_dist->ptrs[i]];
			if (!compact_d) d_above *= diags_vec[i]; else d_above *= diags_dist->dist[diags_dist->ptrs[i]];
			// over-relaxation
			if (omega != 1.0) {
				d_below = ((1-omega) * soln_below[i]) + (omega * d_below);
				d_above = ((1-omega) * soln_above[i]) + (omega * d_above);
			}
			// set vector element
			helper.updateValueFromBelow(soln_below2[i], soln_below[i], d_below);
			helper.updateValueFromAbove(soln_above2[i], soln_above[i], d_above);
		}

		if (iterationExport) {
			iterationExport->exportVector(soln_below2, n, 0);
			iterationExport->exportVector(soln_above2, n, 1);
		}

		// check convergence
		measure.reset();
		measure.measure(soln_below2, soln_above2, n);
		if (measure.value() < term_crit_param) {
			PS_PrintToMainLog(env, "Max %sdiff between upper and lower bound on convergence: %G", measure.isRelative()?"relative ":"", measure.value());
			done = true;
		}

		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PS_PrintToMainLog(env, "Iteration %d: max %sdiff=%f", iters, measure.isRelative()?"relative ":"", measure.value());
			PS_PrintToMainLog(env, ", %.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
			start3 = util_cpu_time();
		}
		
		// prepare for next iteration
		tmpsoln = soln_below;
		soln_below = soln_below2;
		soln_below2 = tmpsoln;
		tmpsoln = soln_above;
		soln_above = soln_above2;
		soln_above2 = tmpsoln;
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iters/timing info
	PS_PrintToMainLog(env, "\n%s (interval iteration): %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", (omega == 1.0)?"Jacobi":"JOR", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	// if the iterative method didn't terminate, this is an error
	if (!done) {
		delete[] soln_below;
		soln_below = NULL;
		PS_SetErrorMessage("Iterative method (interval iteration) did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters);
		PS_PrintToMainLog(env, "Max remaining %sdiff between upper and lower bound on convergence: %G", measure.isRelative()?"relative ":"", measure.value());
	}

	if (helper.flag_select_midpoint() && soln_below) { // we did converge, select midpoint
		last_error_bound = measure.value();
		helper.selectMidpoint(soln_below, soln_above, n);

		if (iterationExport) {
			// export result vector as below and above
			iterationExport->exportVector(soln_below, n, 0);
			iterationExport->exportVector(soln_below, n, 1);
		}
	}

	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PS_SetErrorMessage("Out of memory");
		if (soln_below) delete[] soln_below;
		soln_below = 0;
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
	if (soln_below2) delete[] soln_below2;
	if (soln_above) delete[] soln_above;
	if (soln_above2) delete[] soln_above2;

	return ptr_to_jlong(soln_below);
}

//------------------------------------------------------------------------------
