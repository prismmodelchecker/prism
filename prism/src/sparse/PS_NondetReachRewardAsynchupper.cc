//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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
#include "IntervalIteration.h"
#include "Measures.h"
#include <memory>
#include <new>

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_PrismSparse_PS_1NondetReachRewardAsynchupper
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
jlong __jlongpointer l,		// lower bound
jlong __jlongpointer u,		// upper bound
jboolean min,				// min or max probabilities (true = min, false = max)
jint flags
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
	DdNode *lower = jlong_to_DdNode(l); 			// lower bound
	DdNode *upper = jlong_to_DdNode(u); 			// upper bound

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

	IntervalIteration helper(flags);
	if (!helper.flag_ensure_monotonic_from_above()) {
		PS_PrintToMainLog(env, "Note: Interval iteration is configured to not enforce monotonicity from above.\n");
	}
	if (!helper.flag_ensure_monotonic_from_below()) {
		PS_PrintToMainLog(env, "Note: Interval iteration is configured to not enforce monotonicity from below.\n");
	}

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
	lower_vec = mtbdd_to_double_vector(ddman, lower, rvars, num_rvars, odd);
	kb = n*8.0/1024.0;
	kbt += kb;
	PS_PrintMemoryToMainLog(env, "[", kb, "]\n");

	// get vector for lower bounds
	PS_PrintToMainLog(env, "Creating vector for upper bounds... ");
	upper_vec = mtbdd_to_double_vector(ddman, upper, rvars, num_rvars, odd);
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
	for (i = 0; i < n; i++) {
		soln_below[i] = (inf_vec[i] > 0) ? HUGE_VAL : lower_vec[i];
		soln_above[i] = (inf_vec[i] > 0) ? HUGE_VAL : upper_vec[i];
	}

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

	int *pre_start = new int[n];
	int *pre_end = new int[n];
	int *pre_freq = new int[n];
 	int *pre_state = new int[choice_starts[row_starts[n]-1]+500];
	int *state_order = new int[n];
	bool* selected = new bool[n];
	int top, l;
	top = 0;

	for(i = 0; i < n; i++)
	{	
		selected[i] = false;
		pre_freq[i] = 0;
	}

	for(i = 0; i < n; i++)
		for(j = row_starts[i]; j < row_starts[i+1]; j++)
			for(k = choice_starts[j]; k < choice_starts[j+1]; k++)
				if((non_zeros[j]) >= .005/(choice_starts[i+1] - choice_starts[i]))	
					pre_freq[cols[k]]++;

	pre_start[0] = 0;
	pre_end[0] = pre_freq[0];
	for(i = 1; i < n; i++)
	{
		pre_start[i] = pre_end[i-1];
		pre_end[i] = pre_end[i-1] + pre_freq[i];
	}		

	for(i = 0; i < n; i++)
		pre_freq[i] = pre_start[i];

	for(i = 0; i < n; i++)
		for(j = row_starts[i]; j < row_starts[i+1]; j++)
			for(k = choice_starts[j]; k < choice_starts[j+1]; k++)
				if((non_zeros[j]) >= .005/(choice_starts[i+1] - choice_starts[i]))	
					pre_state[pre_freq[cols[k]]++] = i;

	for(i = 0; i < n; i++)
		if(row_starts[i] >= row_starts[i+1])
			for(m = pre_start[i]; m < pre_end[i]; m++)
			{
				j = pre_state[m];
				if(!selected[j])
				{
					state_order[top++] = j;
					selected[j] = true;
				}
			}
		
	l = 0;//printf("  top  = %d   ", top);

	for(int k = 0; k < n; k++)
	{
		if(selected[k] == false && row_starts[k+1] > row_starts[k])
		{
			selected[k] = true;
			state_order[top++] = k;
		}
		while(l < top)
		{
			i = state_order[l];
			for(m = pre_start[i]; m < pre_end[i]; m++)
			{
				j = pre_state[m];
				if(!selected[j])
				{
					state_order[top++] = j;
					selected[j] = true;
				}
			}
			l++;	
		}
	}

	start2 = util_cpu_time();
	double diff, d3, d4;	
	int ind;
	iters = 0;
	int m1, m2, m3, m_r;	
	dns_row_starts[0] = 0;
	m1 = m2 = m3 = m_r = 0;
	for(ind = 0; ind < top; ind++)
	{
		i = state_order[ind];
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
	int tt, up_itr, low_itr;
	double up_diff, low_diff;
	up_diff = low_diff = 1;
	up_itr = low_itr = 0;

	while(up_diff > term_crit_param)	
	{
		up_itr++;
		diff = 0;
		up_diff = 0;
		for (ind = 0; ind < top; ind++) {
			//i = order[ind];
			d1 = d3 = 0.0; // initial value doesn't matter
			first = true; // (because we also remember 'first')
			 { l1 = dns_row_starts[ind]; h1 = dns_row_starts[ind+1]; }
			for (j = l1; j < h1; j++) {
				d4 = dns_sr_vec[ind];
				{ 
					l2 = dns_choice_starts[j]; 
					h2 = dns_choice_starts[j+1]; 
				}

				for (k = l2; k < h2; k++) {
					tmpd = dns_nnz_r[k] * dns_nnz[k];
					d4 += tmpd;
					d4 += dns_nnz[k] * soln_above[dns_cols[k]];
				}
				if (first || (min&&(d4<d3)) || (!min&&(d4>d3))) {
					d3 = d4;					
				}
				if(d4 <  soln_below[state_order[ind]])
					tt++;
				first = false;
			}
			i = state_order[ind];
			if(up_diff < soln_above[i] - d3)
				up_diff = soln_above[i] - d3;
			soln_above[i] = (row_starts[i+1] > row_starts[i]) ? d3 : inf_vec[i] > 0 ? HUGE_VAL : 0;
		}
	}

	bool flg = true;
	for(i = 0; i < n; i++)
		if(row_starts[i] < row_starts[i+1] && soln_above[i] >= upper_vec[i])
			flg = false;

	printf("\nSoln above finished after %d iterations, while flag is %s.\n", up_itr, (flg?"True":"False"));
	low_itr = 0;

	for(i = 0; i < n; i++)
		soln_below[i] = 0.9 * soln_above[i];

	flg = false;
	while(low_itr < n)
	{
		low_itr++;
		for (ind = 0; ind < top; ind++) {
			//i = order[ind];
			d1 = d3 = 0.0; // initial value doesn't matter
			first = true; // (because we also remember 'first')
			{ l1 = dns_row_starts[ind]; h1 = dns_row_starts[ind+1]; }
			for (j = l1; j < h1; j++) {
				d2 = dns_sr_vec[ind];
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
			i = state_order[ind];
			if(low_diff < d1 - soln_below[i])
				low_diff = d1 - soln_below[i];
			soln_below[i] = (row_starts[i+1] > row_starts[i]) ? d1 : inf_vec[i] > 0 ? HUGE_VAL : 0;
		}
		if(low_itr % 20 == 19)
		{
			flg = true;
			for(i = 0; i < n; i++)
				if(row_starts[i] < row_starts[i+1] && soln_below[i] < 0.9 * soln_above[i]){
					flg = false;}
		}
		if(flg)
			break;
	}	
	printf("\nChecking for lower bound passed after %d iterations.\n", low_itr);
	if(!flg)
		for(i = 0; i < n; i++)
			soln_below[i] = (inf_vec[i] > 0) ? HUGE_VAL : lower_vec[i];

	while (!done && iters < max_iters) 
	{	
		tt = 0;
		iters++;
		// do matrix multiplication and min/max (from below)
		h1 = h2 = h2_r = 0;
		// loop through states
		if(low_diff > .1 * up_diff)
		{
			low_itr++;
			diff = 1;
			low_diff = 0;
			for (ind = 0; ind < top; ind++) {
				//i = order[ind];
				d1 = d3 = 0.0; // initial value doesn't matter
				first = true; // (because we also remember 'first')
				{ l1 = dns_row_starts[ind]; h1 = dns_row_starts[ind+1]; }
				for (j = l1; j < h1; j++) {
					d2 = dns_sr_vec[ind];
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
				i = state_order[ind];
				if(low_diff < d1 - soln_below[i])
					low_diff = d1 - soln_below[i];
				soln_below[i] = (h1 > l1) ? d1 : inf_vec[i] > 0 ? HUGE_VAL : 0;
			}
		}
		if(low_diff * .1 < up_diff)
		{
			up_itr++;
			diff = 0;
			up_diff = 0;
			for (ind = 0; ind < top; ind++) {
				//i = order[ind];
				d1 = d3 = 0.0; // initial value doesn't matter
				first = true; // (because we also remember 'first')
				 { l1 = dns_row_starts[ind]; h1 = dns_row_starts[ind+1]; }
				for (j = l1; j < h1; j++) {
					d4 = dns_sr_vec[ind];
					{ l2 = dns_choice_starts[j]; h2 = dns_choice_starts[j+1]; }

					for (k = l2; k < h2; k++) {
						tmpd = dns_nnz_r[k] * dns_nnz[k];
						d4 += tmpd;
						d4 += dns_nnz[k] * soln_above[dns_cols[k]];
					}
					if (first || (min&&(d4<d3)) || (!min&&(d4>d3))) {
						d3 = d4;					
					}
					if(d4 <  soln_below[state_order[ind]])
						tt++;
					first = false;
				}
				i = state_order[ind];
				if(up_diff < soln_above[i] - d3)
					up_diff = soln_above[i] - d3;

				soln_above[i] = (h1 > l1) ? d3 : inf_vec[i] > 0 ? HUGE_VAL : 0;
				if(soln_above[i] - soln_below[i] > diff)
					diff = soln_above[i] - soln_below[i];
			}
		}
	                                     
		if (diff < term_crit_param) {
			PS_PrintToMainLog(env, "Max %sdiff between upper and lower bound on convergence: %G", measure.isRelative()?"relative ":"", diff);
			done = true;
		}
		
		// print occasional status update
		if ((util_cpu_time() - start3) > UPDATE_DELAY) {
			PS_PrintToMainLog(env, "Iteration %d: max %sdiff=%f", iters, measure.isRelative()?"relative ":"", diff);
			PS_PrintToMainLog(env, ", %.2f sec so far\n", ((double)(util_cpu_time() - start2)/1000));
			start3 = util_cpu_time();
		}
	}
	printf("\nLowItr:%d , UpItr:%d  \n", low_itr, up_itr);
	
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

	if (helper.flag_select_midpoint() && soln_below) { // we did converge, select midpoint
		helper.selectMidpoint(soln_below, soln_above, n);

		if (iterationExport) {
			// export result vector as below and above
			iterationExport->exportVector(soln_below, n, 0);
			iterationExport->exportVector(soln_below, n, 1);
		}
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
