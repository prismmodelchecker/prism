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
#include <new>

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_PrismSparse_PS_1ProbUntil
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
jlong __jlongpointer m	// 'maybe' states
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(t);		// trans matrix
	ODDNode *odd = jlong_to_ODDNode(od); 		// reachable states
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars
	DdNode *yes = jlong_to_DdNode(y);		// 'yes' states
	DdNode *maybe = jlong_to_DdNode(m);		// 'maybe' states

	// mtbdds
	DdNode *reach = NULL, *a = NULL, *b = NULL, *tmp = NULL;
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
	
	// call iterative method
	soln = NULL;
	switch (lin_eq_method) {
		case LIN_EQ_METHOD_POWER:
			soln = jlong_to_double(Java_sparse_PrismSparse_PS_1Power(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(b), false)); break;
		case LIN_EQ_METHOD_JACOBI:
			soln = jlong_to_double(Java_sparse_PrismSparse_PS_1JOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(b), false, false, 1.0)); break;
		case LIN_EQ_METHOD_GAUSSSEIDEL:
			soln = jlong_to_double(Java_sparse_PrismSparse_PS_1SOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(b), false, false, 1.0, true)); break;
		case LIN_EQ_METHOD_BGAUSSSEIDEL:
			soln = jlong_to_double(Java_sparse_PrismSparse_PS_1SOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(b), false, false, 1.0, false)); break;
		case LIN_EQ_METHOD_JOR:
			soln = jlong_to_double(Java_sparse_PrismSparse_PS_1JOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(b), false, false, lin_eq_method_param)); break;
		case LIN_EQ_METHOD_SOR:
			soln = jlong_to_double(Java_sparse_PrismSparse_PS_1SOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(b), false, false, lin_eq_method_param, true)); break;
		case LIN_EQ_METHOD_BSOR:
			soln = jlong_to_double(Java_sparse_PrismSparse_PS_1SOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(b), false, false, lin_eq_method_param, false)); break;
		default:
			PS_SetErrorMessage("Pseudo Gauss-Seidel/SOR methods are currently not supported by the sparse engine"); return 0;
	}
	
	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PS_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}
	
	// free memory
	if (a) Cudd_RecursiveDeref(ddman, a);
	if (b) Cudd_RecursiveDeref(ddman, b);
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------
