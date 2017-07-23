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
#include "PrismHybrid.h"
#include <cmath>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include <dv.h>
#include "sparse.h"
#include "hybrid.h"
#include "PrismHybridGlob.h"
#include "IntervalIteration.h"
#include "jnipointer.h"
#include <new>

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_hybrid_PrismHybrid_PH_1ProbUntilInterval
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer t,	// trans matrix
jlong __jlongpointer od,	// odd
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer y,	// 'yes' states
jlong __jlongpointer m,	// 'maybe' states
jint flags
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(t);		// trans matrix
	ODDNode *odd = jlong_to_ODDNode(od); 		// reachable states
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars
	DdNode *yes = jlong_to_DdNode(y);		// 'yes' states
	DdNode *maybe = jlong_to_DdNode(m); 		// 'maybe' states

	// mtbdds
	DdNode *reach = NULL, *a = NULL, *b = NULL, *lower = NULL, *upper = NULL, *tmp = NULL;
	// vectors
	double *soln = NULL;
	
	// exception handling around whole function
	try {
	
	// get reachable states
	reach = odd->dd;
	
	// filter out rows
	Cudd_Ref(trans);
	Cudd_Ref(maybe);
	a = DD_Apply(ddman, APPLY_TIMES, trans, maybe);
	
	// subtract a from identity (unless we are going to solve with the power method)
	if (lin_eq_method != LIN_EQ_METHOD_POWER) {
		tmp = DD_Identity(ddman, rvars, cvars, num_rvars);
		Cudd_Ref(reach);
		tmp = DD_And(ddman, tmp, reach);
		a = DD_Apply(ddman, APPLY_MINUS, tmp, a);
	}
	
	// build b
	Cudd_Ref(yes);
	b = yes;
	// lower bounds = b
	Cudd_Ref(b);
	lower = b;

	// build upper (yes and maybe = 1)
	Cudd_Ref(yes);
	Cudd_Ref(maybe);
	upper = DD_Or(ddman, yes, maybe);

	IntervalIteration helper(flags);
	if (!helper.flag_ensure_monotonic_from_above()) {
		PH_PrintToMainLog(env, "Note: Interval iteration is configured to not enforce monotonicity from above.\n");
	}
	if (!helper.flag_ensure_monotonic_from_below()) {
		PH_PrintToMainLog(env, "Note: Interval iteration is configured to not enforce monotonicity from below.\n");
	}

	// call iterative method
	soln = NULL;
	switch (lin_eq_method) {
		case LIN_EQ_METHOD_POWER:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1PowerInterval(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(lower), ptr_to_jlong(upper), false, flags)); break;
		case LIN_EQ_METHOD_JACOBI:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1JORInterval(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(lower), ptr_to_jlong(upper), false, false, 1.0, flags)); break;
		case LIN_EQ_METHOD_GAUSSSEIDEL:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1SORInterval(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(lower), ptr_to_jlong(upper), false, false, 1.0, true, flags)); break;
		case LIN_EQ_METHOD_BGAUSSSEIDEL:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1SORInterval(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(lower), ptr_to_jlong(upper), false, false, 1.0, false, flags)); break;
		case LIN_EQ_METHOD_PGAUSSSEIDEL:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1PSORInterval(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(lower), ptr_to_jlong(upper), false, false, 1.0, true, flags)); break;
		case LIN_EQ_METHOD_BPGAUSSSEIDEL:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1PSORInterval(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(lower), ptr_to_jlong(upper), false, false, 1.0, false, flags)); break;
		case LIN_EQ_METHOD_JOR:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1JORInterval(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(lower), ptr_to_jlong(upper), false, false, lin_eq_method_param, flags)); break;
		case LIN_EQ_METHOD_SOR:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1SORInterval(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(lower), ptr_to_jlong(upper), false, false, lin_eq_method_param, true, flags)); break;
		case LIN_EQ_METHOD_BSOR:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1SORInterval(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(lower), ptr_to_jlong(upper), false, false, lin_eq_method_param, false, flags)); break;
		case LIN_EQ_METHOD_PSOR:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1PSORInterval(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(lower), ptr_to_jlong(upper), false, false, lin_eq_method_param, true, flags)); break;
		case LIN_EQ_METHOD_BPSOR:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1PSORInterval(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(lower), ptr_to_jlong(upper), false, false, lin_eq_method_param, false, flags)); break;
	}
	
	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PH_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}
	
	// free memory
	if (a) Cudd_RecursiveDeref(ddman, a);
	if (b) Cudd_RecursiveDeref(ddman, b);
	if (lower) Cudd_RecursiveDeref(ddman, lower);
	if (upper) Cudd_RecursiveDeref(ddman, upper);
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------
