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
#include <math.h>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include "PrismMTBDDGlob.h"
#include "jnipointer.h"

//------------------------------------------------------------------------------

// PCTL until probability 0 precomputation
// (there exists = min) (nondeterministic/mdp) (mtbdd)
// (i.e. compute states where THERE EXISTS a probability 0)
// (NB: actually compute states FOR which ALL probabilities are >0 and then do a NOT)

JNIEXPORT jlong __jlongpointer JNICALL Java_mtbdd_PrismMTBDD_PM_1Prob0E
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer t01, 	// 0-1 trans matrix
jlong __jlongpointer r,	// reachable states
jlong __jlongpointer ndm,	// nondeterminism mask
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer ndv,	// nondet vars
jint num_ndvars,
jlong __jlongpointer phi,	// phi(b1)
jlong __jlongpointer psi	// psi(b2)
)
{
	DdNode *trans01 = jlong_to_DdNode(t01);		// 0-1 trans matrix
	DdNode *reach = jlong_to_DdNode(r);		// reachable states
	DdNode *mask = jlong_to_DdNode(ndm);		// nondeterminism mask
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars	
	DdNode **ndvars = jlong_to_DdNode_array(ndv);	// nondet vars
	DdNode *b1 = jlong_to_DdNode(phi);		// b1
	DdNode *b2 = jlong_to_DdNode(psi);		// b2
	
	DdNode *sol, *tmp;
	bool done;
	int iters;

	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;

	// start clock
	start1 = util_cpu_time();
	
	// reachability fixpoint loop
	Cudd_Ref(b2);
	sol = b2;
	done = false;
	iters = 0;
	while (!done) {
	
		iters++;
		
		Cudd_Ref(sol);
		tmp = DD_PermuteVariables(ddman, sol, rvars, cvars, num_cvars);
		Cudd_Ref(trans01);
		tmp = DD_And(ddman, tmp, trans01);
		tmp = DD_ThereExists(ddman, tmp, cvars, num_cvars);
		
		Cudd_Ref(mask);
		tmp = DD_Or(ddman, tmp, mask);
		tmp = DD_ForAll(ddman, tmp, ndvars, num_ndvars);
		
		Cudd_Ref(b1);
		tmp = DD_And(ddman, b1, tmp);		
		Cudd_Ref(b2);
		tmp = DD_Or(ddman, b2, tmp);
		
		if (tmp == sol) {
			done = true;
		}
		Cudd_RecursiveDeref(ddman, sol);
		sol = tmp;
	}
	sol = DD_PermuteVariables(ddman, sol, cvars, rvars, num_cvars);
	
	// actual answer is states NOT reachable
	Cudd_Ref(reach);
	sol = DD_And(ddman, reach, DD_Not(ddman, sol));

	// stop clock
	time_taken = (double)(util_cpu_time() - start1)/1000;
	time_for_setup = 0;
	time_for_iters = time_taken;

	// print iterations/timing info
	PM_PrintToMainLog(env, "\nProb0E: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);

	return ptr_to_jlong(sol);
}

//------------------------------------------------------------------------------
