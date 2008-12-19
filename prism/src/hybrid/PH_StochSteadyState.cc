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
#include "PrismHybrid.h"
#include <math.h>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include <dv.h>
#include "sparse.h"
#include "hybrid.h"
#include "PrismHybridGlob.h"
#include "jnipointer.h"
#include <new>

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_hybrid_PrismHybrid_PH_1StochSteadyState
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
	DdNode *diags = NULL, *q = NULL, *a = NULL, *tmp = NULL;
	// model stats
	int n;
	// vectors
	double *soln = NULL;
	// misc
	int i;
	double deltat, d;
	
	// exception handling around whole function
	try {
	
	// get number of states
	n = odd->eoff + odd->toff;
	
	// if we are going to solve with the power method, we have to modify the matrix a bit
	if (lin_eq_method == LIN_EQ_METHOD_POWER) {
		
		// technically, this is a little bit wasteful
		// for sparse/hybrid, we can avoid constructing the diagonals and rate matrix as mtbdds
		// (as used to be done in version <=2.1 before the power method was generic)
		// shouldn't be too disasterous though
		// and if you are that bothered about efficiency, you won't be using the power method anyway...
		
		// compute diagonals
		Cudd_Ref(trans);
		diags = DD_SumAbstract(ddman, trans, cvars, num_rvars);
		diags = DD_Apply(ddman, APPLY_TIMES, diags, DD_Constant(ddman, -1));
		
		// choose deltat
		deltat = -0.99 / DD_FindMin(ddman, diags);
		
		// build generator matrix q from trans and diags
		// note that any self loops are effectively removed because we include their rates
		// in the 'diags' row sums and then subtract these from the original rate matrix
		Cudd_Ref(trans);
		Cudd_Ref(diags);
		q = DD_Apply(ddman, APPLY_PLUS, trans, DD_Apply(ddman, APPLY_TIMES, DD_Identity(ddman, rvars, cvars, num_rvars), diags));
		
		// build iteration matrix
		PH_PrintToMainLog(env, "\nBuilding power method iteration matrix MTBDD... ");
		// (includes a "fix" for when we are solving a subsystem e.g. BSCC)
		// (although i don't think we actually need this for the power method)
		Cudd_Ref(diags);
		tmp = DD_LessThan(ddman, diags, 0);
		Cudd_Ref(q);
		a = DD_Apply(ddman, APPLY_PLUS, DD_Apply(ddman, APPLY_TIMES, DD_Constant(ddman, deltat), q), DD_Apply(ddman, APPLY_TIMES, DD_Identity(ddman, rvars, cvars, num_rvars), tmp));
		i = DD_GetNumNodes(ddman, a);
		PH_PrintToMainLog(env, "[nodes=%d] [%.1f Kb]", i, i*20.0/1024.0);
		
		// deref unneeded mtbdds
		Cudd_RecursiveDeref(ddman, diags);
		Cudd_RecursiveDeref(ddman, q);
	}
	else {
		// technically, we should remove self-loops (i.e. diagonals) from the rate matrix
		// but the iterative solution methods remove all diagonals before doing
		// solution (and before doing row sums) so we don't need to bother
		Cudd_Ref(trans);
		a = trans;
	}
	
	// call iterative method
	soln = NULL;
	switch (lin_eq_method) {
		case LIN_EQ_METHOD_POWER:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1Power(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), 0, ptr_to_jlong(init), true)); break;
		case LIN_EQ_METHOD_JACOBI:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1JOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), 0, ptr_to_jlong(init), true, true, 1.0)); break;
		case LIN_EQ_METHOD_GAUSSSEIDEL:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1SOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), 0, ptr_to_jlong(init), true, true, 1.0, true)); break;
		case LIN_EQ_METHOD_BGAUSSSEIDEL:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1SOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), 0, ptr_to_jlong(init), true, true, 1.0, false)); break;
		case LIN_EQ_METHOD_PGAUSSSEIDEL:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1PSOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), 0, ptr_to_jlong(init), true, true, 1.0, true)); break;
		case LIN_EQ_METHOD_BPGAUSSSEIDEL:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1PSOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), 0, ptr_to_jlong(init), true, true, 1.0, false)); break;
		case LIN_EQ_METHOD_JOR:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1JOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), 0, ptr_to_jlong(init), true, true, lin_eq_method_param)); break;
		case LIN_EQ_METHOD_SOR:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1SOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), 0, ptr_to_jlong(init), true, true, lin_eq_method_param, true)); break;
		case LIN_EQ_METHOD_BSOR:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1SOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), 0, ptr_to_jlong(init), true, true, lin_eq_method_param, false)); break;
		case LIN_EQ_METHOD_PSOR:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1PSOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), 0, ptr_to_jlong(init), true, true, lin_eq_method_param, true)); break;
		case LIN_EQ_METHOD_BPSOR:
			soln = jlong_to_double(Java_hybrid_PrismHybrid_PH_1PSOR(env, cls, ptr_to_jlong(odd), ptr_to_jlong(rvars), num_rvars, ptr_to_jlong(cvars), num_cvars, ptr_to_jlong(a), 0, ptr_to_jlong(init), true, true, lin_eq_method_param, false)); break;
	}
	
	// normalise
	if (soln != NULL) {
		d = 0;
		for (i = 0; i < n; i++) {
			d += soln[i];
		}
		for (i = 0; i < n; i++) {
			soln[i] /= d;
		}
	}
	
	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PH_SetErrorMessage("Out of memory");
		if (soln) delete[] soln;
		soln = 0;
	}
	
	// free memory
	if (a) Cudd_RecursiveDeref(ddman, a);
	
	return ptr_to_jlong(soln);
}

//------------------------------------------------------------------------------
