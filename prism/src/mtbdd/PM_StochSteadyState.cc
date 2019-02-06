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

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_mtbdd_PrismMTBDD_PM_1StochSteadyState
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer tr,	// trans matrix
jlong __jlongpointer od,	// odd
jlong __jlongpointer in,	// init soln
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(tr);		// trans matrix
	ODDNode *odd = jlong_to_ODDNode(od);		// odd
	DdNode *init = jlong_to_DdNode(in);		// init soln
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars
	// mtbdds
	DdNode *diags, *q, *a, *b, *soln;
	// misc
	double deltat;
	
	// compute diagonals
	Cudd_Ref(trans);
	diags = DD_SumAbstract(ddman, trans, cvars, num_rvars);
	diags = DD_Apply(ddman, APPLY_TIMES, diags, DD_Constant(ddman, -1));
	
	// if diagonal is 0 set it to -1
	// (fix for when we are solving subsystem e.g. BSCC)
	Cudd_Ref(diags);
	diags = DD_ITE(ddman, DD_LessThan(ddman, diags, 0), diags, DD_Constant(ddman, -1));
	
	// build generator matrix q from trans and diags
	// note that any self loops are effectively removed because we include their rates
	// in the 'diags' row sums and then subtract these from the original rate matrix
	Cudd_Ref(trans);
	Cudd_Ref(diags);
	q = DD_Apply(ddman, APPLY_PLUS, trans, DD_Apply(ddman, APPLY_TIMES, DD_Identity(ddman, rvars, cvars, num_rvars), diags));
	
	// If we are going to solve with the power method, we have to modify the matrix a bit
	// in order to guarantee convergence. Hence, we compute the iteration matrix
	// a = q * deltaT + I
	// where I is the identity matrix.
	// Please refer to "William J. Stewart: Introduction to the Numerical Solution of Markov Chains" p. 124. for details.
	if (lin_eq_method == LIN_EQ_METHOD_POWER) {
		// choose deltat
		deltat = -0.99 / DD_FindMin(ddman, diags);
		// build iteration matrix
		Cudd_Ref(q);
		a = DD_Apply(ddman, APPLY_PLUS, DD_Apply(ddman, APPLY_TIMES, DD_Constant(ddman, deltat), q), DD_Identity(ddman, rvars, cvars, num_rvars));
	}
	else {
		Cudd_Ref(q);
		a = q;
	}
	
	// b vector is all zeros
	b = DD_Constant(ddman, 0);
	
	// call iterative method
	soln = NULL;
	switch (lin_eq_method) {
		case LIN_EQ_METHOD_POWER:
			soln = jlong_to_DdNode(Java_mtbdd_PrismMTBDD_PM_1Power(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(init), true));
			break;
		case LIN_EQ_METHOD_JACOBI:
			soln = jlong_to_DdNode(Java_mtbdd_PrismMTBDD_PM_1JOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(init), true, 1.0));
			break;
		case LIN_EQ_METHOD_JOR:
			soln = jlong_to_DdNode(Java_mtbdd_PrismMTBDD_PM_1JOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), ptr_to_jlong(b), ptr_to_jlong(init), true, lin_eq_method_param));
			break;
		default:
			// set error message and return NULL pointer after cleanup, below
			PM_SetErrorMessage("Gauss-Seidel and its variants are currently not supported by the MTBDD engine");
			break;
	}
	
	// normalise
	if (soln != NULL) {
		Cudd_Ref(soln);
		soln = DD_Apply(ddman, APPLY_DIVIDE, soln, DD_SumAbstract(ddman, soln, rvars, num_rvars));
	}
	
	// free memory
	Cudd_RecursiveDeref(ddman, diags);
	Cudd_RecursiveDeref(ddman, q);
	Cudd_RecursiveDeref(ddman, a);
	Cudd_RecursiveDeref(ddman, b);
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------
