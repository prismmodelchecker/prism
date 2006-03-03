//==============================================================================
//	
//	File:		PM_Reachability.cc
//	Date:		4/5/01
//	Author:		Dave Parker
//	Desc:		Calculates states reachable from the given subset (s)
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

JNIEXPORT jint JNICALL Java_mtbdd_PrismMTBDD_PM_1Reachability
(
JNIEnv *env,
jclass cls,
jint t01,	// 0-1 trans matrix
jint rv,	// row vars
jint num_rvars,
jint cv,	// col vars
jint num_cvars,
jint s		// start state
)
{
	DdNode *trans01 = (DdNode *)t01;	// 0-1 trans matrix
	DdNode *init = (DdNode *)s;		// start state
	DdNode **rvars = (DdNode **)rv;		// row vars
	DdNode **cvars = (DdNode **)cv;		// col vars
	
	DdNode *reach, *frontier, *tmp;
	bool done;
	int iters;

	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;

	// start clocks	
	start1 = util_cpu_time();
	
	// reachability fixpoint loop - option 1
	// here...

	done = false;
	iters = 0;
	Cudd_Ref(init);
	reach = DD_PermuteVariables(ddman, init, rvars, cvars, num_rvars);
	
//	PM_PrintToMainLog(env, "Reachability:\n");
	while (!done) {
		iters++;
//		PM_PrintToMainLog(env, "Iteration %d: ", iters);
//		PM_PrintToMainLog(env, "%0.f (%d) ", DD_GetNumMinterms(ddman, reach, num_rvars), DD_GetNumNodes(ddman, reach));
//		start2 = util_cpu_time();
		
//		PM_PrintToMainLog(env, "[permute(%d)", DD_GetNumNodes(ddman, reach));
//		start3 = util_cpu_time();
		Cudd_Ref(reach);
		tmp = DD_PermuteVariables(ddman, reach, cvars, rvars, num_cvars);
//		stop = util_cpu_time();
//		PM_PrintToMainLog(env, "=%fs]", (double)(stop - start3)/1000);
		
//		PM_PrintToMainLog(env, "[and(%d,%d)", DD_GetNumNodes(ddman, tmp), DD_GetNumNodes(ddman, trans01));
//		start3 = util_cpu_time();
		Cudd_Ref(trans01);
		tmp = DD_And(ddman, tmp, trans01);
//		stop = util_cpu_time();
//		PM_PrintToMainLog(env, "=%fs]", (double)(stop - start3)/1000);
		
//		PM_PrintToMainLog(env, "[thereexists(%d)", DD_GetNumNodes(ddman, tmp));
//		start3 = util_cpu_time();
		tmp = DD_ThereExists(ddman, tmp, rvars, num_rvars);
//		stop = util_cpu_time();
//		PM_PrintToMainLog(env, "=%fs]", (double)(stop - start3)/1000);
		
//		PM_PrintToMainLog(env, "[or(%d,%d)", DD_GetNumNodes(ddman, reach), DD_GetNumNodes(ddman, tmp));
//		start3 = util_cpu_time();
		Cudd_Ref(reach);
		tmp = DD_Or(ddman, reach, tmp);
//		stop = util_cpu_time();
//		PM_PrintToMainLog(env, "=%fs]", (double)(stop - start3)/1000);

		if (tmp == reach) {
			done = true;
		}
		Cudd_RecursiveDeref(ddman, reach);
		reach = tmp;
//		stop = util_cpu_time();
//		PM_PrintToMainLog(env, " = %f\n", (double)(stop - start2)/1000);
	}
	reach = DD_PermuteVariables(ddman, reach, cvars, rvars, num_cvars);
	// ...to here
	
	// reachability fixpoint loop - option 2
	// here...
	/*done = false;
	iters = 0;
	Cudd_Ref(init);
	reach = init;
	Cudd_Ref(reach);
	frontier = reach;
//	PM_PrintToMainLog(env, "Reachability:\n");
	while (!done) {
//		start2 = util_cpu_time();
		iters++;
//		PM_PrintToMainLog(env, "Iteration %d: ", iters);
//		PM_PrintToMainLog(env, "%0.f (%d) ", DD_GetNumMinterms(ddman, reach, num_rvars), DD_GetNumNodes(ddman, reach));
//		PM_PrintToMainLog(env, "%0.f (%d) ", DD_GetNumMinterms(ddman, frontier, num_rvars), DD_GetNumNodes(ddman, frontier));
		
//		start3 = util_cpu_time();
//		PM_PrintToMainLog(env, "[permute(%d)", DD_GetNumNodes(ddman, frontier));
		Cudd_Ref(frontier);
		tmp = DD_PermuteVariables(ddman, frontier, cvars, rvars, num_cvars);
//		stop = util_cpu_time();
//		PM_PrintToMainLog(env, "=%fs]", (double)(stop - start3)/1000);
		
//		start3 = util_cpu_time();
//		PM_PrintToMainLog(env, "[and(%d,%d)", DD_GetNumNodes(ddman, tmp), DD_GetNumNodes(ddman, trans01));
		Cudd_Ref(trans01);
		tmp = DD_And(ddman, tmp, trans01);
//		stop = util_cpu_time();
//		PM_PrintToMainLog(env, "=%fs]", (double)(stop - start3)/1000);
		
//		start3 = util_cpu_time();
//		PM_PrintToMainLog(env, "[thereexists(%d)", DD_GetNumNodes(ddman, tmp));
		tmp = DD_ThereExists(ddman, tmp, rvars, num_rvars);
//		stop = util_cpu_time();
//		PM_PrintToMainLog(env, "=%fs]", (double)(stop - start3)/1000);
		
//		start3 = util_cpu_time();
//		PM_PrintToMainLog(env, "[or(%d,%d)", DD_GetNumNodes(ddman, reach), DD_GetNumNodes(ddman, tmp));
		Cudd_Ref(reach);
		tmp = DD_Or(ddman, reach, tmp);
//		stop = util_cpu_time();
//		PM_PrintToMainLog(env, "=%fs]", (double)(stop - start3)/1000);

//		start3 = util_cpu_time();
//		PM_PrintToMainLog(env, "[and(%d,not(%d))", DD_GetNumNodes(ddman, tmp), DD_GetNumNodes(ddman, reach));
		Cudd_RecursiveDeref(ddman, frontier);
		Cudd_Ref(tmp);
		Cudd_Ref(reach);
		frontier = DD_And(ddman, tmp, DD_Not(ddman, reach));
//		stop = util_cpu_time();
//		PM_PrintToMainLog(env, "=%fs]", (double)(stop - start3)/1000);

		if (frontier == Cudd_ReadZero(ddman)) {
			done = true;
		}
		Cudd_RecursiveDeref(ddman, reach);
		reach = tmp;
//		stop = util_cpu_time();
//		PM_PrintToMainLog(env, " total=%fs\n", (double)(stop - start2)/1000);
	}
	reach = DD_PermuteVariables(ddman, reach, cvars, rvars, num_cvars);
	Cudd_RecursiveDeref(ddman, frontier);*/
	// ...to here
	
	// stop clock
	stop = util_cpu_time();
	time_taken = (double)(stop - start1)/1000;
	time_for_setup = 0;
	time_for_iters = time_taken;

	// print iterations/timing info
	PM_PrintToMainLog(env, "\nReachability: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);

	return (int)reach;
}

//------------------------------------------------------------------------------
