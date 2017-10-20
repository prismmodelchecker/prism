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
#include "PrismMTBDDGlob.h"
#include "jnipointer.h"

//------------------------------------------------------------------------------

// Calculates states reachable from the given subset (s)

JNIEXPORT jlong __jlongpointer JNICALL Java_mtbdd_PrismMTBDD_PM_1Reachability
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer t01,	// 0-1 trans matrix
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer s	// start state
)
{
	DdNode *trans01 = jlong_to_DdNode(t01);		// 0-1 trans matrix
	DdNode *init = jlong_to_DdNode(s);		// start state
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars
	
	DdNode *reach, *frontier, *tmp;
	bool done;
	int iters;
	
	// get PRISM options
	int reach_method = env->CallIntMethod(prism_obj, env->GetMethodID(prism_cls, "getReachMethod", "()I"));
	int info = env->CallIntMethod(prism_obj, env->GetMethodID(prism_cls, "getExtraReachInfo", "()Z"));

	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;

	// start clocks
	start1 = util_cpu_time();
	
	if (reach_method == REACH_BFS) {
	
		// initialise
		done = false;
		iters = 0;
		Cudd_Ref(init);
		reach = DD_PermuteVariables(ddman, init, rvars, cvars, num_rvars);
		if (reach == NULL) return ptr_to_jlong(NULL);
		
		while (!done) {
			iters++;
			// output info on progress
			if (info > 0) {
				PM_PrintToMainLog(env, "Iteration %d:", iters);
				PM_PrintToMainLog(env, " %0.f states", DD_GetNumMinterms(ddman, reach, num_rvars));
				PM_PrintToMainLog(env, " (%d nodes)", DD_GetNumNodes(ddman, reach));
				start2 = util_cpu_time();
			}
			// perform iteration
			Cudd_Ref(reach);
			tmp = DD_PermuteVariables(ddman, reach, cvars, rvars, num_cvars);
			if (tmp == NULL) return ptr_to_jlong(NULL);
			Cudd_Ref(trans01);
			tmp = DD_And(ddman, tmp, trans01);
			if (tmp == NULL) return ptr_to_jlong(NULL);
			tmp = DD_ThereExists(ddman, tmp, rvars, num_rvars);
			if (tmp == NULL) return ptr_to_jlong(NULL);
			Cudd_Ref(reach);
			tmp = DD_Or(ddman, reach, tmp);
			if (tmp == NULL) return ptr_to_jlong(NULL);
			// check convergence
			if (tmp == reach) {
				done = true;
			}
			Cudd_RecursiveDeref(ddman, reach);
			reach = tmp;
			// output info on progress
			if (info > 0) {
				stop = util_cpu_time();
				PM_PrintToMainLog(env, " (%.2f seconds)\n", (double)(stop - start2)/1000);
			}
		}
		reach = DD_PermuteVariables(ddman, reach, cvars, rvars, num_cvars);
		if (reach == NULL) return ptr_to_jlong(NULL);
	}
	else {
		// initialise
		done = false;
		iters = 0;
		Cudd_Ref(init);
		reach = init;
		Cudd_Ref(reach);
		frontier = reach;
		
		while (!done) {
			iters++;
			// output info on progress
			if (info > 0) {
				PM_PrintToMainLog(env, "Iteration %d:", iters);
				PM_PrintToMainLog(env, " %0.f states", DD_GetNumMinterms(ddman, reach, num_rvars));
				PM_PrintToMainLog(env, " (%d nodes)", DD_GetNumNodes(ddman, reach));
				start2 = util_cpu_time();
			}
			// perform iteration
			Cudd_Ref(frontier);
			tmp = DD_PermuteVariables(ddman, frontier, cvars, rvars, num_cvars);
			if (tmp == NULL) return ptr_to_jlong(NULL);
			Cudd_Ref(trans01);
			tmp = DD_And(ddman, tmp, trans01);
			if (tmp == NULL) return ptr_to_jlong(NULL);
			tmp = DD_ThereExists(ddman, tmp, rvars, num_rvars);
			if (tmp == NULL) return ptr_to_jlong(NULL);
			Cudd_Ref(reach);
			tmp = DD_Or(ddman, reach, tmp);
			if (tmp == NULL) return ptr_to_jlong(NULL);
			Cudd_RecursiveDeref(ddman, frontier);
			Cudd_Ref(tmp);
			Cudd_Ref(reach);
			frontier = DD_And(ddman, tmp, DD_Not(ddman, reach));
			if (frontier) return ptr_to_jlong(NULL);
			// check convergence
			if (frontier == Cudd_ReadZero(ddman)) {
				done = true;
			}
			Cudd_RecursiveDeref(ddman, reach);
			reach = tmp;
			// output info on progress
			if (info > 0) {
				stop = util_cpu_time();
				PM_PrintToMainLog(env, " (%.2f seconds)\n", (double)(stop - start2)/1000);
			}
		}
		reach = DD_PermuteVariables(ddman, reach, cvars, rvars, num_cvars);
		if (reach == NULL) return ptr_to_jlong(NULL);
		Cudd_RecursiveDeref(ddman, frontier);
	}
	
	// stop clock
	stop = util_cpu_time();
	time_taken = (double)(stop - start1)/1000;
	time_for_setup = 0;
	time_for_iters = time_taken;

	// print iterations/timing info
	PM_PrintToMainLog(env, "\nReachability (%s): %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", reach_method==REACH_BFS?"BFS":"frontier", iters, time_taken, time_for_iters/iters, time_for_setup);

	return ptr_to_jlong(reach);
}

//------------------------------------------------------------------------------
