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
#include <memory>

//------------------------------------------------------------------------------

// solve the linear equation system Ax=b with Jacobi/JOR

JNIEXPORT jlong __jlongpointer JNICALL Java_mtbdd_PrismMTBDD_PM_1JOR
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
jdouble omega		// omega (jor parameter)
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
	DdNode *reach, *diags, *id, *sol, *tmp;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// misc
	int i, iters;
	bool done;
	
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
	
	// store initial solution, transposing if necessary
	Cudd_Ref(init);
	sol = init;
	if (transpose) {
		sol = DD_PermuteVariables(ddman, sol, rvars, cvars, num_rvars);
	}
	
	std::unique_ptr<ExportIterations> iterationExport;
	if (PM_GetFlagExportIterations()) {
		std::string title("PM_JOR (");
		title += (omega == 1.0)?"Jacobi": ("JOR omega=" + std::to_string(omega));
		title += ")";

		iterationExport.reset(new ExportIterations(title.c_str()));
		PM_PrintToMainLog(env, "Exporting iterations to %s\n", iterationExport->getFileName().c_str());
		iterationExport->exportVector(sol, (transpose?cvars:rvars), num_rvars, odd, 0);
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
	
	while (!done && iters < max_iters) {
		
		iters++;
		
		// matrix multiply
		Cudd_Ref(sol);
		tmp = DD_PermuteVariables(ddman, sol, (transpose?cvars:rvars), (transpose?rvars:cvars), num_rvars);
		Cudd_Ref(a);
		tmp = DD_MatrixMultiply(ddman, a, tmp, (transpose?rvars:cvars), num_cvars, MM_BOULDER);
		Cudd_Ref(b);
		tmp = DD_Apply(ddman, APPLY_PLUS, tmp, b);
		if (omega != 1.0) {
			tmp = DD_Apply(ddman, APPLY_TIMES, tmp, DD_Constant(ddman, omega));
			Cudd_Ref(sol);
			tmp = DD_Apply(ddman, APPLY_PLUS, tmp, DD_Apply(ddman, APPLY_TIMES, sol, DD_Constant(ddman, 1-omega)));
		}

		if (iterationExport)
			iterationExport->exportVector(tmp, (transpose?cvars:rvars), num_rvars, odd, 0);

		// check convergence
		switch (term_crit) {
		case TERM_CRIT_ABSOLUTE:
			if (DD_EqualSupNorm(ddman, tmp, sol, term_crit_param)) {
				done = true;
			}
			break;
		case TERM_CRIT_RELATIVE:
			if (DD_EqualSupNormRel(ddman, tmp, sol, term_crit_param)) {
				done = true;
			}
			break;
		}
		
		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PM_PrintToMainLog(env, "Iteration %d: ", iters);
			PM_PrintToMainLog(env, "%.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
			start3 = util_cpu_time();
		}
		
		// store accuracy info, once converged
		// the difference between vector values is not a reliable error bound
		// but we store it anyway in case it is useful for estimating a bound
		// TODO: handle cases where result is zero
		if (done) {
			Cudd_Ref(tmp);
			Cudd_Ref(sol);
			DdNode* difference = DD_Apply(ddman, APPLY_MINUS, tmp, sol);
			if (term_crit == TERM_CRIT_RELATIVE) {
				Cudd_Ref(tmp);
				difference = DD_Apply(ddman, APPLY_DIVIDE, difference, tmp);
			}
			// No DD absolute operator so check most +ve/-ve
			double max_diff = fabs(DD_FindMax(ddman, difference));
			double min_diff = fabs(DD_FindMin(ddman, difference));
			last_error_bound = max_diff > min_diff ? max_diff : min_diff;
			Cudd_RecursiveDeref(ddman, difference);
		}
		
		// prepare for next iteration
		Cudd_RecursiveDeref(ddman, sol);
		sol = tmp;
	}
	
	// transpose solution if necessary
	if (transpose) {
		sol = DD_PermuteVariables(ddman, sol, cvars, rvars, num_rvars);
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iters/timing info
	PM_PrintToMainLog(env, "\n%s: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", (omega == 1.0)?"Jacobi":"JOR", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	// free memory
	Cudd_RecursiveDeref(ddman, id);
	Cudd_RecursiveDeref(ddman, diags);
	Cudd_RecursiveDeref(ddman, a);
	Cudd_RecursiveDeref(ddman, b);
	
	// if the iterative method didn't terminate, this is an error
	if (!done) { Cudd_RecursiveDeref(ddman, sol); PM_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters); return 0; }
	
	return ptr_to_jlong(sol);
}

//------------------------------------------------------------------------------
