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
#include "PrismMTBDD.h"
#include <cmath>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include "PrismMTBDDGlob.h"
#include "jnipointer.h"
#include "prism.h"
#include "ExportIterations.h"
#include "IntervalIteration.h"
#include <memory>

//------------------------------------------------------------------------------

// solve the linear equation system Ax=b with Jacobi/JOR (interval variant)

JNIEXPORT jlong __jlongpointer JNICALL Java_mtbdd_PrismMTBDD_PM_1JORInterval
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
jdouble omega,		// omega (jor parameter)
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
	DdNode *reach, *diags, *id, *sol_below, *sol_above, *tmp;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, iters;
	bool done;

	if (omega <= 0.0 || omega > 1.0) {
		PM_SetErrorMessage("Interval iteration requires 0 < omega <= 1.0, have omega = %g", omega);
		return ptr_to_jlong(NULL);
	}

	IntervalIteration helper(flags);

	// start clocks
	start1 = start2 = util_cpu_time();
	
	// get reachable states
	reach = odd->dd;
	
	// make local copy of a,b
	Cudd_Ref(a);
	Cudd_Ref(b);
	
	// remove and keep diagonal entries of matrix A
	id =  DD_Identity(ddman, rvars, cvars, num_rvars);
	Cudd_Ref(reach);
	id = DD_And(ddman, id, reach);
	Cudd_Ref(id);
	Cudd_Ref(a);
	diags = DD_Apply(ddman, APPLY_TIMES, id, a);
	Cudd_Ref(id);
	a = DD_ITE(ddman, id, DD_Constant(ddman, 0), a);
	
	// put diagonals in a vector
	diags = DD_SumAbstract(ddman, diags, (transpose?rvars:cvars), num_cvars);
	
	// negate a
	a = DD_Apply(ddman, APPLY_TIMES, DD_Constant(ddman, -1), a);
	
	// transpose b if necessary
	if (transpose) {
		b = DD_PermuteVariables(ddman, b, rvars, cvars, num_rvars);
	}
	
	// divide a,b by diagonal
	Cudd_Ref(diags);
	a = DD_Apply(ddman, APPLY_DIVIDE, a, diags);
	Cudd_Ref(diags);
	b = DD_Apply(ddman, APPLY_DIVIDE, b, diags);
	
	// print out some memory usage
	i = DD_GetNumNodes(ddman, a);
	PM_PrintToMainLog(env, "\nIteration matrix MTBDD... [nodes=%d] [%.1f Kb]\n", i, i*20.0/1024.0);
	i = DD_GetNumNodes(ddman, diags);
	PM_PrintToMainLog(env, "Diagonals MTBDD... [nodes=%d] [%.1f Kb]\n", i, i*20.0/1024.0);
	
	// store initial solutions, transposing if necessary
	Cudd_Ref(lower);
	sol_below = lower;
	Cudd_Ref(upper);
	sol_above = upper;
	if (transpose) {
		sol_below = DD_PermuteVariables(ddman, sol_below, rvars, cvars, num_rvars);
		sol_above = DD_PermuteVariables(ddman, sol_above, rvars, cvars, num_rvars);
	}

	std::unique_ptr<ExportIterations> iterationExport;
	if (PM_GetFlagExportIterations()) {
		std::string title("PM_JOR (");
		title += (omega == 1.0)?"Jacobi": ("JOR omega=" + std::to_string(omega));
		title += "), interval";

		iterationExport.reset(new ExportIterations(title.c_str()));
		PM_PrintToMainLog(env, "Exporting iterations to %s\n", iterationExport->getFileName().c_str());
		iterationExport->exportVector(sol_below, (transpose?cvars:rvars), num_rvars, odd, 0);
		iterationExport->exportVector(sol_above, (transpose?cvars:rvars), num_rvars, odd, 1);
	}

	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;
	
	// start iterations
	iters = 0;
	done = false;
	PM_PrintToMainLog(env, "\nStarting iterations...\n");

	bool below_unchanged = false, above_unchanged = false;

	while (!done && iters < max_iters) {
		below_unchanged = above_unchanged = false;

		iters++;
		
		// matrix multiply (lower)
		Cudd_Ref(sol_below);
		tmp = DD_PermuteVariables(ddman, sol_below, (transpose?cvars:rvars), (transpose?rvars:cvars), num_rvars);
		Cudd_Ref(a);
		tmp = DD_MatrixMultiply(ddman, a, tmp, (transpose?rvars:cvars), num_cvars, MM_BOULDER);
		Cudd_Ref(b);
		tmp = DD_Apply(ddman, APPLY_PLUS, tmp, b);
		if (omega != 1.0) {
			tmp = DD_Apply(ddman, APPLY_TIMES, tmp, DD_Constant(ddman, omega));
			Cudd_Ref(sol_below);
			tmp = DD_Apply(ddman, APPLY_PLUS, tmp, DD_Apply(ddman, APPLY_TIMES, sol_below, DD_Constant(ddman, 1-omega)));
		}
		if (helper.flag_ensure_monotonic_from_below()) {
			// below: do max of tmp with old solution
			Cudd_Ref(sol_below);
			tmp = DD_Apply(ddman, APPLY_MAX, tmp, sol_below);
		}
		if (sol_below == tmp) below_unchanged = true;
		Cudd_RecursiveDeref(ddman, sol_below);
		sol_below = tmp;

		// matrix multiply (upper)
		Cudd_Ref(sol_above);
		tmp = DD_PermuteVariables(ddman, sol_above, (transpose?cvars:rvars), (transpose?rvars:cvars), num_rvars);
		Cudd_Ref(a);
		tmp = DD_MatrixMultiply(ddman, a, tmp, (transpose?rvars:cvars), num_cvars, MM_BOULDER);
		Cudd_Ref(b);
		tmp = DD_Apply(ddman, APPLY_PLUS, tmp, b);
		if (omega != 1.0) {
			tmp = DD_Apply(ddman, APPLY_TIMES, tmp, DD_Constant(ddman, omega));
			Cudd_Ref(sol_above);
			tmp = DD_Apply(ddman, APPLY_PLUS, tmp, DD_Apply(ddman, APPLY_TIMES, sol_above, DD_Constant(ddman, 1-omega)));
		}
		if (helper.flag_ensure_monotonic_from_above()) {
			// below: do min of tmp with old solution
			Cudd_Ref(sol_above);
			tmp = DD_Apply(ddman, APPLY_MIN, tmp, sol_above);
		}
		if (sol_above == tmp) above_unchanged = true;
		Cudd_RecursiveDeref(ddman, sol_above);
		sol_above = tmp;

		if (iterationExport) {
			iterationExport->exportVector(sol_below, (transpose?cvars:rvars), num_rvars, odd, 0);
			iterationExport->exportVector(sol_above, (transpose?cvars:rvars), num_rvars, odd, 1);
		}

		// check convergence
		switch (term_crit) {
		case TERM_CRIT_ABSOLUTE:
			if (DD_EqualSupNorm(ddman, sol_above, sol_below, term_crit_param)) {
				done = true;
			}
			break;
		case TERM_CRIT_RELATIVE:
			if (DD_EqualSupNormRel(ddman, sol_above, sol_below, term_crit_param)) {
				done = true;
			}
			break;
		}

		if (!done && below_unchanged && above_unchanged) {
			break;
		}

		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PM_PrintToMainLog(env, "Iteration %d: ", iters);
			PM_PrintToMainLog(env, "%.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
			start3 = util_cpu_time();
		}
		
	}
	
	// transpose solution if necessary
	if (transpose) {
		sol_below = DD_PermuteVariables(ddman, sol_below, cvars, rvars, num_rvars);
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iters/timing info
	PM_PrintToMainLog(env, "\n%s (interval iteration): %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", (omega == 1.0)?"Jacobi":"JOR", iters, time_taken, time_for_iters/iters, time_for_setup);

	DdNode *result;
	if (helper.flag_select_midpoint() && done) { // we did converge, select midpoint
		Cudd_Ref(sol_below);
		Cudd_Ref(sol_above);

		// compute midpoint for result
		DdNode* difference = DD_Apply(ddman, APPLY_MINUS, sol_above, sol_below);
		difference = DD_Apply(ddman, APPLY_DIVIDE, difference, DD_Constant(ddman, 2.0));

		Cudd_Ref(sol_below);
		result = DD_Apply(ddman, APPLY_PLUS, sol_below, difference);
		// TODO: ensure that below <= result <= above?

		// export midpoint as vector above and below
		if (iterationExport) {
			iterationExport->exportVector(result, rvars, num_rvars, odd, 0);
			iterationExport->exportVector(result, rvars, num_rvars, odd, 1);
		}
	} else {
		result = sol_below;
		Cudd_Ref(result);
	}

	// free memory
	Cudd_RecursiveDeref(ddman, id);
	Cudd_RecursiveDeref(ddman, diags);
	Cudd_RecursiveDeref(ddman, a);
	Cudd_RecursiveDeref(ddman, b);
	Cudd_RecursiveDeref(ddman, sol_below);
	Cudd_RecursiveDeref(ddman, sol_above);

	// if the iterative method didn't terminate, this is an error
	if (!done) {
		Cudd_RecursiveDeref(ddman, result);
		if (below_unchanged && above_unchanged) {
			PM_SetErrorMessage("In interval iteration, after %d iterations, both lower and upper iteration did not change anymore but don't have the required precision yet.\nThis could be caused by the MTBDD's engine collapsing of similar constants, consider setting a smaller value for -cuddepsilon or -cuddepsilon 0 to disable collapsing", iters);
		} else {
			PM_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters);
		}
		return 0;
	}

	return ptr_to_jlong(result);
}

//------------------------------------------------------------------------------
