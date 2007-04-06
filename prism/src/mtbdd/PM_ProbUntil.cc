//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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
#include <math.h>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include "PrismMTBDDGlob.h"

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_mtbdd_PrismMTBDD_PM_1ProbUntil
(
JNIEnv *env,
jclass cls,
jint t,			// trans matrix
jint od,		// odd
jint rv,		// row vars
jint num_rvars,
jint cv,		// col vars
jint num_cvars,
jint y,			// 'yes' states
jint m			// 'maybe' states
)
{
	// cast function parameters
	DdNode *trans = (DdNode *)t;	// trans matrix
	ODDNode *odd = (ODDNode *)od; 	// reachable states
	DdNode **rvars = (DdNode **)rv; // row vars
	DdNode **cvars = (DdNode **)cv; // col vars
	DdNode *yes = (DdNode *)y;		// 'yes' states
	DdNode *maybe = (DdNode *)m; 	// 'maybe' states
	// mtbdds
	DdNode *reach, *a, *b, *soln, *tmp;
	
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
			soln = (DdNode *)Java_mtbdd_PrismMTBDD_PM_1Power(env, cls, (jint)odd, (jint)rvars, num_rvars, (jint)cvars, num_cvars, (jint)a, (jint)b, (jint)b, false); break;
		case LIN_EQ_METHOD_JACOBI:
			soln = (DdNode *)Java_mtbdd_PrismMTBDD_PM_1JOR(env, cls, (jint)odd, (jint)rvars, num_rvars, (jint)cvars, num_cvars, (jint)a, (jint)b, (jint)b, false, 1.0); break;
		case LIN_EQ_METHOD_JOR:
			soln = (DdNode *)Java_mtbdd_PrismMTBDD_PM_1JOR(env, cls, (jint)odd, (jint)rvars, num_rvars, (jint)cvars, num_cvars, (jint)a, (jint)b, (jint)b, false, lin_eq_method_param); break;
	}
	
	// free memory
	Cudd_RecursiveDeref(ddman, a);
	Cudd_RecursiveDeref(ddman, b);
	
	return (int)soln;
}

//------------------------------------------------------------------------------
