//==============================================================================
//	
//	File:		PM_Prob1A.cc
//	Date:		4/5/01
//	Authors:	Dave Parker and Gethin Norman
//	Desc:		PCTL until probability 1 precomputation (for all = min) (nondeterministic/mdp) (mtbdd)
//				(i.e. compute states FOR which ALL probabilities are 1)
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

JNIEXPORT jint JNICALL Java_mtbdd_PrismMTBDD_PM_1Prob1A
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
jint n,			// no
jint psi		// psi(b2)
)
{
	DdNode *trans01 = (DdNode *)t01;	// 0-1 trans matrix
	DdNode *all = (DdNode *)r;		// reachable states
	DdNode *mask = (DdNode *)ndm;		// nondeterminism mask
	DdNode *no = (DdNode *)n;		// no
	DdNode *b2 = (DdNode *)psi;		// b2
	DdNode **rvars = (DdNode **)rv;		// row vars
	DdNode **cvars = (DdNode **)cv;		// col vars
	DdNode **ndvars = (DdNode **)ndv;	// nondet vars
	
	DdNode  *reach,*sol, *tmp, *notno;
	bool done;
	int iters;
	
	// timing stuff
	long start, stop;
	double time_taken, time_for_setup, time_for_iters;
	
	// start clock
	start = util_cpu_time();
	
	// negate set "no" ("there exists an adversary with prob=0") to get set "for all adversaries prob>0"
	Cudd_Ref(all);
	Cudd_Ref(no);
	notno = DD_And(ddman, all, DD_Not(ddman, no));
	
	// greatest fixed point loop
	Cudd_Ref(b2);
	Cudd_Ref(notno);
	sol = DD_Or(ddman, b2, notno);
	
	done = false;
	iters = 0;
	
	while (!done) {
		
		iters++;
		
		Cudd_Ref(sol);
		tmp = DD_SwapVariables(ddman, sol, rvars, cvars, num_rvars);
		Cudd_Ref(trans01);
		tmp = DD_ForAll(ddman, DD_Implies(ddman, trans01, tmp), cvars, num_cvars);
		
		Cudd_Ref(mask);
		tmp = DD_Or(ddman, tmp, mask);
		tmp = DD_ForAll(ddman, tmp, ndvars, num_ndvars);
		
		Cudd_Ref(notno);
		tmp = DD_And(ddman, notno, tmp);		
		Cudd_Ref(b2);
		tmp = DD_Or(ddman, b2, tmp);
		
		if (tmp == sol) {
			done = true;
		}
		Cudd_RecursiveDeref(ddman, sol);
		sol = tmp;
	}
	
	// stop clock
	time_taken = (double)(util_cpu_time() - start)/1000;
	time_for_setup = 0;
	time_for_iters = time_taken;
	
	// print iterations/timing info
	PM_PrintToMainLog(env, "\nProb1A: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
	
	Cudd_RecursiveDeref(ddman, notno);
	
	return (int)sol;
}

//------------------------------------------------------------------------------
