//==============================================================================
//	
//	File:		PM_ProbOA.cc
//	Date:		4/5/01
//	Author:		Dave Parker
//	Desc:		PCTL until probability 0 precomputation (there exists = min) (nondeterministic/mdp) (mtbdd)
//				(i.e. compute states where THERE EXISTS a probability 0)
//				(NB: actually compute states FOR which ALL probabilities are >0 and then do a NOT)
//	
//------------------------------------------------------------------------------
//	
//	Copyright (c) 2002-2004, Dave Parker
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

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_mtbdd_PrismMTBDD_PM_1Prob0E
(
JNIEnv *env,
jclass cls,
jint t01, 		// 0-1 trans matrix
jint r,			// reachable states
jint ndm,		// nondeterminism mask
jint rv,		// row vars
jint num_rvars,
jint cv,		// col vars
jint num_cvars,
jint ndv,		// nondet vars
jint num_ndvars,
jint phi,		// phi(b1)
jint psi		// psi(b2)
)
{
	DdNode *trans01 = (DdNode *)t01;	// 0-1 trans matrix
	DdNode *all = (DdNode *)r;		// reachable states
	DdNode *mask = (DdNode *)ndm;		// nondeterminism mask
	DdNode **rvars = (DdNode **)rv;		// row vars
	DdNode **cvars = (DdNode **)cv;		// col vars	
	DdNode **ndvars = (DdNode **)ndv;	// nondet vars
	DdNode *b1 = (DdNode *)phi;		// b1
	DdNode *b2 = (DdNode *)psi;		// b2
	
	DdNode *reach, *sol, *tmp;
	bool done;
	int iters;

	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;

	// start clock
	start1 = util_cpu_time();
	
	// reachability fixpoint loop
	Cudd_Ref(b2);
	reach = b2;
	done = false;
	iters = 0;
	while (!done) {
	
		iters++;
		
		Cudd_Ref(reach);
		tmp = DD_PermuteVariables(ddman, reach, rvars, cvars, num_cvars);
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
		
		if (tmp == reach) {
			done = true;
		}
		Cudd_RecursiveDeref(ddman, reach);
		reach = tmp;
	}
	reach = DD_PermuteVariables(ddman, reach, cvars, rvars, num_cvars);
	
	// actual answer is states NOT reachable
	Cudd_Ref(all);
	sol = DD_And(ddman, all, DD_Not(ddman, reach));

	// stop clock
	time_taken = (double)(util_cpu_time() - start1)/1000;
	time_for_setup = 0;
	time_for_iters = time_taken;

	// print iterations/timing info
	PM_PrintToMainLog(env, "\nProb0E: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);

	return (int)sol;
}

//------------------------------------------------------------------------------
