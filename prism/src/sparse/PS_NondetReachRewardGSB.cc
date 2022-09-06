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
#include <cmath>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include <dv.h>
#include "sparse.h"
#include "prism.h"
#include "PrismNativeGlob.h"
#include "PrismSparseGlob.h"
#include "jnipointer.h"
#include "ExportIterations.h"
#include <memory>
#include "Measures.h"
#include <new>

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_PrismSparse_PS_1NondetReachRewardGSB
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer t,		// trans matrix
jlong __jlongpointer ta,	// trans action labels
jobject synchs,
jlong __jlongpointer sr,	// state rewards
jlong __jlongpointer trr,	// transition rewards
jlong __jlongpointer od,	// odd
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer ndv,	// nondet vars
jint num_ndvars,
jlong __jlongpointer g,		// 'goal' states
jlong __jlongpointer in,	// 'inf' states
jlong __jlongpointer m,		// 'maybe' states
jboolean min				// min or max probabilities (true = min, false = max)
)
{
	// cast function parameters
	DdNode *trans = jlong_to_DdNode(t);				// trans matrix
	DdNode *trans_actions = jlong_to_DdNode(ta);	// trans action labels
	DdNode *state_rewards = jlong_to_DdNode(sr);	// state rewards
	DdNode *trans_rewards = jlong_to_DdNode(trr);	// transition rewards
	ODDNode *odd = jlong_to_ODDNode(od); 			// reachable states
	DdNode **rvars = jlong_to_DdNode_array(rv); 	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv); 	// col vars
	DdNode **ndvars = jlong_to_DdNode_array(ndv);	// nondet vars
	DdNode *goal = jlong_to_DdNode(g);				// 'goal' states
	DdNode *inf = jlong_to_DdNode(in); 				// 'inf' states
	DdNode *maybe = jlong_to_DdNode(m); 			// 'maybe' states 


	// mtbdds
	DdNode *a, *tmp = NULL;
	// model stats
	int n, nc, nc_r;
	long nnz, nnz_r;
	// sparse matrix
	NDSparseMatrix *ndsm = NULL, *ndsm_r = NULL;
	// vectors
	double *sr_vec = NULL, *soln_below = NULL, *soln_below2 = NULL, *soln_above = NULL, *soln_above2 = NULL, *tmpsoln = NULL, *inf_vec = NULL, *lower_vec = NULL, *upper_vec = NULL;
	// timing stuff
	long start1, start2, start3, stop;
	double time_taken, time_for_setup, time_for_iters;
	// adversary stuff
	int export_adv_enabled = export_adv;
	FILE *fp_adv = NULL;
	int adv_j;
	int *adv = NULL;
	// action info
	jstring *action_names_jstrings;
	const char** action_names = NULL;
	int num_actions;
	// misc
	int i, j, k, k_r, l1, h1, l2, h2, l2_r, h2_r, iters;
	double d1, d2, x, sup_norm, kb, kbt;
	bool done, first;
	// measure for convergence termination check
	MeasureSupNorm measure(term_crit == TERM_CRIT_RELATIVE);


	// exception handling around whole function
	try {
	
	// start clocks	
	start1 = start2 = util_cpu_time();
	
	// get number of states
	n = odd->eoff + odd->toff;
	
	// filter out rows (goal states and infinity states) from matrix
	Cudd_Ref(trans);
	Cudd_Ref(maybe);
	a = DD_Apply(ddman, APPLY_TIMES, trans, maybe);
	
	// also remove goal and infinity states from state rewards vector
	Cudd_Ref(state_rewards);
	Cudd_Ref(maybe);
	state_rewards = DD_Apply(ddman, APPLY_TIMES, state_rewards, maybe);
	
	// and from transition rewards matrix
	Cudd_Ref(trans_rewards);
	Cudd_Ref(maybe);
	trans_rewards = DD_Apply(ddman, APPLY_TIMES, trans_rewards, maybe);
	
	// build sparse matrix (probs)
	PS_PrintToMainLog(env, "\nBuilding sparse matrix (transitions)... ");
	ndsm = build_nd_sparse_matrix(ddman, a, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	// get number of transitions/choices
	nnz = ndsm->nnz;
	nc = ndsm->nc;
	kb = (nnz*12.0+nc*4.0+n*4.0)/1024.0;
	kbt = kb;
	// print out info
	PS_PrintToMainLog(env, "[n=%d, nc=%d, nnz=%ld, k=%d] ", n, nc, nnz, ndsm->k);
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// if needed, and if info is available, build a vector of action indices for the MDP
	if (export_adv_enabled != EXPORT_ADV_NONE) {
		if (trans_actions != NULL) {
			PS_PrintToMainLog(env, "Building action information... ");
			// first need to filter out unwanted rows
			Cudd_Ref(trans_actions);
			Cudd_Ref(maybe);
			tmp = DD_Apply(ddman, APPLY_TIMES, trans_actions, maybe);
			// then convert to a vector of integer indices
			build_nd_action_vector(ddman, a, tmp, ndsm, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
			Cudd_RecursiveDeref(ddman, tmp);
			kb = n*4.0/1024.0;
			kbt += kb;
			PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
			// also extract list of action names from 'synchs'
			get_string_array_from_java(env, synchs, action_names_jstrings, action_names, num_actions);
		} else {
			PS_PrintWarningToMainLog(env, "Action labels are not available for adversary generation.");
		}
	}
	
	// build sparse matrix (rewards)
	PS_PrintToMainLog(env, "Building sparse matrix (transition rewards)... ");
	ndsm_r = build_sub_nd_sparse_matrix(ddman, a, trans_rewards, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	// get number of transitions/choices
	nnz_r = ndsm_r->nnz;
	nc_r = ndsm_r->nc;
	// print out info
	PS_PrintToMainLog(env, "[n=%d, nc=%d, nnz=%ld, k=%d] ", n, nc_r, nnz_r, ndsm_r->k);
	kb = (nnz_r*12.0+nc_r*4.0+n*4.0)/1024.0;
	kbt += kb;
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// get vector for state rewards
	PS_PrintToMainLog(env, "Creating vector for state rewards... ");
	sr_vec = mtbdd_to_double_vector(ddman, state_rewards, rvars, num_rvars, odd);
	kb = n*8.0/1024.0;
	kbt += kb;
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
	
	// get vector for yes
	PS_PrintToMainLog(env, "Creating vector for inf... ");
	inf_vec = mtbdd_to_double_vector(ddman, inf, rvars, num_rvars, odd);
	kb = n*8.0/1024.0;
	kbt += kb;
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");

	// get vector for lower bounds
	PS_PrintToMainLog(env, "Creating vector for lower bounds... ");

	kb = n*8.0/1024.0;
	kbt += kb;
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");

	// get vector for lower bounds
	PS_PrintToMainLog(env, "Creating vector for upper bounds... ");
	kb = n*8.0/1024.0;
	kbt += kb;
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");

	// create solution/iteration vectors
	PS_PrintToMainLog(env, "Allocating iteration vectors... ");
	soln_below = new double[n];
	soln_below2 = new double[n];
	soln_above = new double[n];
	soln_above2 = new double[n];
	kb = n*8.0/1024.0;
	kbt += 4*kb;
	PS_PrintMemoryToMainLog(env, "[4 x ", kb, "]\n");

	// if required, create storage for adversary and initialise
	if (export_adv_enabled != EXPORT_ADV_NONE) {
		PS_PrintToMainLog(env, "Allocating adversary vector... ");
		adv = new int[n];
		kb = n*sizeof(int)/1024.0;
		kbt += kb;
		PS_PrintMemoryToMainLog(env, "[", kb, "]\n");
		// Initialise all entries to -1 ("don't know")
		for (i = 0; i < n; i++) {
			adv[i] = -1;
		}
	}
	
	// print total memory usage
	PS_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");

	// initial solution from below is infinity in 'inf' states, lower elsewhere
	// initial solution from above is infinity in 'inf' states, upper elsewhere
	for (i = 0; i < n; i++) 
		soln_below[i] = (inf_vec[i] > 0) ? HUGE_VAL : 0;

	std::unique_ptr<ExportIterations> iterationExport;
	if (PS_GetFlagExportIterations()) {
		iterationExport.reset(new ExportIterations("PS_NondetReachReward (interval)"));
		iterationExport->exportVector(soln_below, n, 0);
		iterationExport->exportVector(soln_above, n, 1);
	}

	// get setup time
	stop = util_cpu_time();
	time_for_setup = (double)(stop - start2)/1000;
	start2 = stop;
	start3 = stop;

	// start iterations
	iters = 0;
	done = false;
	PS_PrintToMainLog(env, "\nStarting iterations (interval iteration)...\n");

	// open file to store adversary (if required)
	if (export_adv_enabled != EXPORT_ADV_NONE) {
		fp_adv = fopen(export_adv_filename, "w");
		if (!fp_adv) {
			PS_PrintWarningToMainLog(env, "Adversary generation cancelled (could not open file \"%s\").", export_adv_filename);
			export_adv_enabled = EXPORT_ADV_NONE;
		}
	}

	// store local copies of stuff
	// firstly for transition matrix
	double *non_zeros = ndsm->non_zeros;
	unsigned char *row_counts = ndsm->row_counts;
	int *row_starts = (int *)ndsm->row_counts;
	unsigned char *choice_counts = ndsm->choice_counts;
	int *choice_starts = (int *)ndsm->choice_counts;
	bool use_counts = ndsm->use_counts;
	unsigned int *cols = ndsm->cols;
	// and then for transition rewards matrix
	// (note: we don't need row_counts/row_starts for
	// this since choice structure mirrors transition matrix)
	double *non_zeros_r = ndsm_r->non_zeros;
	unsigned char *choice_counts_r = ndsm_r->choice_counts;
	int *choice_starts_r = (int *)ndsm_r->choice_counts;
	bool use_counts_r = ndsm_r->use_counts;
	unsigned int *cols_r = ndsm_r->cols;

	int* order = new int[n];
	int* selected = new int[n];

	if(use_counts)
	{
		row_starts = new int[n + 1];
		//choice_starts;
		row_starts[0] = 0;
		for(i = 1; i <= n; i++)
			row_starts[i] = row_starts[i - 1] + row_counts[i-1]; 

		choice_starts = new int[row_starts[n] + 1]; 
		choice_starts[0] = 0;
		for(i = 1; i <= row_starts[n]; i++)
			choice_starts[i] = choice_starts[i - 1] + choice_counts[i - 1]; 
	}
	else
	{
		row_starts = (int *)ndsm->row_counts;
		choice_starts = (int *)ndsm->choice_counts;
	}
	if(use_counts_r)
	{
		choice_starts_r = new int[row_starts[n] + 1]; 
		for(i = 1; i <= row_starts[n]; i++)
			choice_starts_r[i] = choice_starts_r[i - 1] + choice_counts_r[i - 1]; 
	}


	int numTransitions = choice_starts[row_starts[n]];
	int *dns_row_starts = new int[n + 1];
	int *dns_choice_starts = new int[row_starts[n] + 50];
	int *dns_cols = new int[numTransitions];
	int *dns_choice_starts_r = new int[row_starts[n] + 50];
	int *dns_cols_r = new int[numTransitions];
	double *dns_sr_vec = new double[n];
	double *dns_nnz = new double[numTransitions];
	double *dns_nnz_r = new double[numTransitions];
	for(i = 0; i < n; i++)
		selected[i] = -1;
	int _ind = 0;
	int low_itr = 0;
	l1 = 1;
	bool flg = true, fl2;
	//order[0] = 0;
	//selected[0] = true;
	for(i = 0; i < n; i++)
		if(row_starts[i+1] <= row_starts[i])
			selected[i] = 0;
	iters = 0;
	while(flg)
	{
		iters++;
		//printf("\n %d   %d", iters, _ind);
		flg = false;
		for(i = 0; i < n; i++)
		if(selected[i] == -1)
		{
			fl2 = false;
			for(j = row_starts[i]; j < row_starts[i+1] && !fl2; j++)
				for(k = choice_starts[j]; k < choice_starts[j+1] && !fl2; k++)
					if(selected[cols[k]] == iters-1)
					{
						order[_ind++] = i;
						selected[i] = iters;
						flg = true;
						fl2 = true;
						break;
					}	
		}
	}
	printf("\n Time for pre-computation: %f ", (util_cpu_time() - start2)/1000.);
	start2 = util_cpu_time();

	double diff, d3, d4;	
	int ind;
	iters = 0;
	int m1, m2, m3, m_r;	
	dns_row_starts[0] = 0;
	m1 = m2 = m3 = m_r = 0;
	for(ind = 0; ind < _ind; ind++)
	{
		i = order[ind];
		dns_sr_vec[m1] = sr_vec[i];
		dns_row_starts[m1++] = m2;
		for(j = row_starts[i]; j < row_starts[i+1]; j++)
		{
			//dns_choice_starts_r[m2] = m_r;	
			dns_choice_starts[m2++] = m3;	
			l2 = choice_starts[j]; h2 = choice_starts[j+1]; 
			l2_r = choice_starts_r[j]; h2_r = choice_starts_r[j+1]; 
			for(k = l2; k < h2; k++)
			{
				dns_nnz[m3] = non_zeros[k];
				dns_cols[m3] = cols[k];
				k_r = l2_r; while (k_r < h2_r && cols_r[k_r] != cols[k]) k_r++;
				// if there is one, add reward * prob to reward value
				if (k_r < h2_r)
					dns_nnz_r[m3++] = non_zeros_r[k_r];
				else
					dns_nnz_r[m3++] = 0;		
			}
		}
	}
	dns_row_starts[m1++] = m2;
	dns_choice_starts[m2++] = m3;	
	double tmpd;
	int tt;
	while (iters < max_iters) 
	{	tt = 0;
		iters++;
		diff = 0;	
		// do matrix multiplication and min/max (from below)
		h1 = h2 = h2_r = 0;
		// loop through states
		for (ind = 0; ind < _ind; ind++) {
			//i = order[ind];
			d1 = d3 = 0.0; // initial value doesn't matter
			first = true; // (because we also remember 'first')
			 { l1 = dns_row_starts[ind]; h1 = dns_row_starts[ind+1]; }
			for (j = l1; j < h1; j++) {
				d2 = dns_sr_vec[ind];
				//d4 = dns_sr_vec[ind];
				{ l2 = dns_choice_starts[j]; h2 = dns_choice_starts[j+1]; }

				for (k = l2; k < h2; k++) {
					tmpd = dns_nnz_r[k] * dns_nnz[k];
					d2 += tmpd;				
					d2 += dns_nnz[k] * soln_below[dns_cols[k]];
				}
				if (first || (min&&(d2<d1)) || (!min&&(d2>d1))) {
					d1 = d2;					
				}
								
				first = false;
			}
			i = order[ind];
			if(d1 - soln_below[i] > diff)
				diff = d1 - soln_below[i];

			soln_below[i] = (h1 > l1) ? d1 : inf_vec[i] > 0 ? HUGE_VAL : 0;			

			if(h1 <= l1 && inf_vec[i] > 0)
				printf("GGGGGGGFDDDSSS");
		}
	
		// do matrix multiplication and min/max (from above)
		h1 = h2 = h2_r = 0;
		// loop through states
                                       
		if (diff < term_crit_param) 
			done = true;		
		
		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PS_PrintToMainLog(env, "Iteration %d: max %sdiff=%f", iters, measure.isRelative()?"relative ":"", diff);
			PS_PrintToMainLog(env, ", %.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
			start3 = util_cpu_time();
		}	
	}
	done = true;
	iters--;
	printf("\nFirst round (updates from below) finished after %d iters.", iters);

	// Traverse matrix to extract adversary
	if (export_adv_enabled != EXPORT_ADV_NONE) {
		// Do two passes: first to compute the number of transitions,
		// the second to actually do the export
		int num_trans = 0;
		for (int pass = 1; pass <= 2; pass++) {
			if (pass == 2) {
				fprintf(fp_adv, "%d %d\n", n, num_trans);
			}
			h1 = h2 = 0;
			for (i = 0; i < n; i++) {
				if (!use_counts) { l1 = row_starts[i]; h1 = row_starts[i+1]; }
				else { l1 = h1; h1 += row_counts[i]; }
				// Have to loop through all choices (to compute offsets)
				for (j = l1; j < h1; j++) {
					if (!use_counts) { l2 = choice_starts[j]; h2 = choice_starts[j+1]; }
					else { l2 = h2; h2 += choice_counts[j]; }
					// But only output a choice if it is in the adversary
					if (j == adv[i]) {
						switch (pass) {
						case 1:
							num_trans += (h2-l2);
							break;
						case 2:
							for (k = l2; k < h2; k++) {
								switch (export_adv_enabled) {
								case EXPORT_ADV_DTMC:
									fprintf(fp_adv, "%d %d %g", i, cols[k], non_zeros[k]); break;
								case EXPORT_ADV_MDP:
									fprintf(fp_adv, "%d 0 %d %g", i, cols[k], non_zeros[k]); break;
								}
								if (ndsm->actions != NULL) fprintf(fp_adv, " %s", ndsm->actions[j]>0?action_names[ndsm->actions[j]-1]:"");
								fprintf(fp_adv, "\n");
							}
						}
					}
				}
			}
		}
	}
	
	// stop clocks
	stop = util_cpu_time();
	time_for_iters = (double)(stop - start2)/1000;
	time_taken = (double)(stop - start1)/1000;
	
	// print iterations/timing info
	PS_PrintToMainLog(env, "\nIterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_for_iters, time_for_iters/iters, time_for_setup);
	
	// if the iterative method didn't terminate, this is an error
	if (!done) {
		delete[] soln_below;
		soln_below = NULL;
		PS_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters);
		PS_PrintToMainLog(env, "Max remaining %sdiff between upper and lower bound on convergence: %G", measure.isRelative()?"relative ":"", measure.value());
	}
	
	// close file to store adversary (if required)
	if (export_adv_enabled != EXPORT_ADV_NONE) {
		fclose(fp_adv);
		PS_PrintToMainLog(env, "\nAdversary written to file \"%s\".\n", export_adv_filename);
	}

	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PS_SetErrorMessage("Out of memory");
		if (soln_below) delete[] soln_below;
		soln_below = 0;
	}
	
	// free memory
	if (a) Cudd_RecursiveDeref(ddman, a);
	if (state_rewards) Cudd_RecursiveDeref(ddman, state_rewards);
	if (trans_rewards) Cudd_RecursiveDeref(ddman, trans_rewards);
	if (ndsm) delete ndsm;
	if (ndsm_r) delete ndsm_r;
	if (inf_vec) delete[] inf_vec;
	if (lower_vec) delete[] lower_vec;
	if (upper_vec) delete[] upper_vec;
	if (sr_vec) delete[] sr_vec;
	if (soln_above) delete[] soln_above;
	if (soln_above2) delete[] soln_above2;
	if (soln_below2) delete[] soln_below2;
	if (adv) delete[] adv;
	if (action_names != NULL) {
		release_string_array_from_java(env, action_names_jstrings, action_names, num_actions);
	}
	return ptr_to_jlong(soln_below);
}	

//------------------------------------------------------------------------------
