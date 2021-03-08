//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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
#include <new>
#include <string>

//The following gives more output on stdout. In fact quite a lot of it, usable only for ~10 state examples 
//#define MORE_OUTPUT

//The following number is used to determine when to consider a number equal to 0.
//Will be multiplied by minimal weights to make sure we don't do too much roundoffs for small weights
#define ZERO_ROUNDOFF 10e-11

JNIEXPORT jdoubleArray __jlongpointer JNICALL Java_sparse_PrismSparse_PS_1NondetMultiObj
(
 JNIEnv *env,
 jclass cls,
 jlong __jlongpointer od, // odd
 jlong __jlongpointer rv, // row vars
 jint num_rvars,
 jlong __jlongpointer cv, // col vars
 jint num_cvars,
 jlong __jlongpointer ndv,  // nondet vars
 jint num_ndvars,
 jboolean min,        // min or max probabilities (true = min, false = max)
 jlong __jlongpointer _start, // initial state(s)
 jlong _adversary,
 jlong __jlongpointer _ndsm, //pointer to trans sparse matrix
 jobject synchs,
 jlongArray _yes_vec, //pointer to yes vector array
 jintArray _prob_step_bounds, //step bounds for probabilistic operators
 jlongArray  _ndsm_r, //pointer to reward sparse matrix array
 jdoubleArray _weights, //weights of rewards and yes_vec vectors
 jintArray _ndsm_r_step_bounds //step bounds for rewards
  )
{
	// cast function parameters
	ODDNode *odd = jlong_to_ODDNode(od);      // reachable states
	DdNode **rvars = jlong_to_DdNode_array(rv);   // row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);   // col vars
	DdNode **ndvars = jlong_to_DdNode_array(ndv); // nondet vars
	DdNode *start = jlong_to_DdNode(_start); // initial state(s)
    
	// mtbdds
	DdNode *a = NULL, *tmp = NULL;
	// model stats
	int n, nc;
	long nnz;
	// sparse matrix
	NDSparseMatrix *ndsm = NULL;
	NDSparseMatrix **ndsm_r = NULL;
	// vectors
	double **yes_vec = NULL;
	double *soln = NULL, *soln2 = NULL, *tmpsoln = NULL;
	double **psoln = NULL, **psoln2 = NULL;  
    
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
	int i, j, k, l1, h1, l2, h2, iters;
	int *k_r = NULL, *l2_r = NULL, *h2_r= NULL;
	double d1, d2, kb, kbt;
	double *pd1 = NULL, *pd2 = NULL;
	bool done, weightedDone, first;
	int num_yes = 0;
	int start_index;
	unsigned int *row_starts1, *predecessors;
	unsigned int extra_node;
	// storage for result array (0 means error)
	jdoubleArray ret = 0;
	// local copy of max_iters, since we will change it
	int max_iters_local = max_iters;
	// whether to export individual solution vectors (with adversaries)
	bool export_vectors = false;
	
	// Extract some info about objectives
	bool has_rewards = _ndsm_r != 0;
	bool has_yes_vec = _yes_vec != 0;
	jsize lenRew = (has_rewards) ? env->GetArrayLength(_ndsm_r) : 0;
	jsize lenProb = (has_yes_vec) ? env->GetArrayLength(_yes_vec) : 0;
	jlong *ptr_ndsm_r = (has_rewards) ? env->GetLongArrayElements(_ndsm_r, 0) : NULL;
	jlong *ptr_yes_vec = (has_yes_vec) ? env->GetLongArrayElements(_yes_vec, 0) : NULL;
	double* weights = env->GetDoubleArrayElements(_weights, 0);
	int* step_bounds_r = (has_rewards) ? (int*)env->GetIntArrayElements(_ndsm_r_step_bounds, 0) : NULL;
	int* step_bounds = (has_yes_vec) ? (int*)env->GetIntArrayElements(_prob_step_bounds, 0) : NULL;
	
	// We will ignore one of the rewards and compute its value from the other ones and
	// from the combined value. We must make sure that this reward has nonzero weight,
	// otherwise we can't compute it.
	int ignoredWeight = -1;
	
	/* HOTFIX: not used for numerical problems
	for (i = lenProb + lenRew - 1; i>=0; i--) {
		if (weights[i] > 0) {
			ignoredWeight = i;
			break;
		}
	}*/
	
	//determine the minimal nonzero weight
	double min_weight = 1;
	for (i = 0; i < lenProb + lenRew; i++)
		if (weights[i] > 0 && weights[i] < min_weight)
			min_weight = weights[i];
	double near_zero = min_weight * ZERO_ROUNDOFF;
	
	// exception handling around whole function
	try {
		// start clocks 
		start1 = start2 = util_cpu_time();
		
		// get number of states
		n = odd->eoff + odd->toff;
		
		// build sparse matrix
		ndsm = (NDSparseMatrix *) jlong_to_NDSparseMatrix(_ndsm);
		
		// if needed, and if info is available, get action names
		if (export_adv_enabled != EXPORT_ADV_NONE) {
			if (synchs != NULL) {
				get_string_array_from_java(env, synchs, action_names_jstrings, action_names, num_actions);
			}
		}
	
		// get number of transitions/choices
		nnz = ndsm->nnz;
		nc = ndsm->nc;
		kb = ndsm->mem;
		kbt = kb;
		
		NDSparseMatrix *ndsm_r[lenRew];
		
		for(int rewi = 0; rewi < lenRew; rewi++)
			ndsm_r[rewi] = (NDSparseMatrix *) jlong_to_NDSparseMatrix(ptr_ndsm_r[rewi]);
		
		int max_step_bound = 0;
		for(int rewi = 0; rewi < lenRew; rewi++) {
			if (step_bounds_r[rewi] == -1)
				step_bounds_r[rewi] = max_iters_local;
			else if (max_step_bound < step_bounds_r[rewi]) {
				max_step_bound = step_bounds_r[rewi];
			}
		}
		
		for(int probi = 0; probi < lenProb; probi++) {
			if (step_bounds[probi] == -1) {
				step_bounds[probi] = max_iters_local;
			} else if (max_step_bound < step_bounds[probi]) {
				max_step_bound = step_bounds[probi];
			}
		}
		
		// get vector for yes
		yes_vec = new double *[lenProb];
		for (int probi = 0; probi < lenProb; probi++) {
			yes_vec[probi] = (double *) jlong_to_ptr(ptr_yes_vec[probi]);
#ifdef MORE_OUTPUT
		PS_PrintToMainLog(env, "yes_vec %d: ", probi);
		for (int o = 0; o < n; o++)
			PS_PrintToMainLog(env, "%f, ", yes_vec[probi][o]);
		PS_PrintToMainLog(env, "\n");
#endif
		}
		
		kb = n*8.0/1024.0;
		kbt += kb;
		
		// create solution/iteration vectors
		soln = new double[n];
		soln2 = new double[n];
		psoln = new double *[lenProb + lenRew];
		psoln2 = new double *[lenProb + lenRew];
		for (int it = 0; it < lenProb + lenRew ; it++) {
			if (it != ignoredWeight) {
				psoln[it] = new double[n];
				psoln2[it] = new double[n];
			}
		}
		pd1 = new double[lenProb + lenRew];
		pd2 = new double[lenProb + lenRew];
		
		kb = n*8.0/1024.0;
		kbt += 2*kb;
		
		// if required, create storage for adversary and initialise
		if (export_adv_enabled != EXPORT_ADV_NONE) {
			adv = new int[n];
			// Initialise all entries to -1 ("don't know")
			for (i = 0; i < n; i++) {
				adv[i] = -1;
			}
		}
	
		// Get index of single (first) initial state
		start_index = get_index_of_first_from_bdd(ddman, start, rvars, num_rvars, odd);
		
		// initial solution
		for (i = 0; i < n; i++) {
			// combined value initialised to weighted sum of yes vectors (for unbounded probability objectives)
			// or 0 (for anything else: step-bounded probabilities, or cumulative rewards)
			soln[i] = 0;
			for (int probi = 0; probi < lenProb; probi++) {
				if (step_bounds[probi] == max_iters_local) {
					soln[i] += weights[probi] * yes_vec[probi][i];
				}
			}
			// individual objectives
			for (int probi = 0; probi < lenProb; probi++) {
				if (probi != ignoredWeight) {
					if (step_bounds[probi] == max_iters_local) {
						psoln[probi][i] = 0;//yes_vec[probi][i];
					}
					else {
						psoln[probi][i] = 0;
					}
				}
			}
			for (int rewi = 0; rewi < lenRew; rewi++) {
				if (lenProb + rewi != ignoredWeight) {
					psoln[rewi + lenProb][i] = 0;
				}
			}
			// soln2 vector(s) just initialised to zero (not read until updated again)
			soln2[i] = 0;
			for (int it = 0; it < lenRew + lenProb; it++) {
				if (it != ignoredWeight) {
					psoln2[it][i] = 0;
				}
			}
		}
		
#ifdef MORE_OUTPUT
		PS_PrintToMainLog(env, "Initial soln: ");
		for (int o = 0; o < n; o++)
			PS_PrintToMainLog(env, "%f, ", soln[o]);
		PS_PrintToMainLog(env, "\n");
		
		
		for (int it = 0; it < lenRew + lenProb; it++) {
			if (it != ignoredWeight) {
				PS_PrintToMainLog(env, "psoln: ");
				for (int o = 0; o < n; o++)
					PS_PrintToMainLog(env, "%f, ", psoln[it][o]);
				PS_PrintToMainLog(env, "\n");
			} else {
				PS_PrintToMainLog(env, "psoln: (ignored)\n");
			}
		}
#endif
		// get setup time
		stop = util_cpu_time();
		time_for_setup = (double)(stop - start2)/1000;
		start2 = stop;
		
		// start iterations
		iters = 0;
		done = false;
		weightedDone = false;
		//PS_PrintToMainLog(env, "Starting iterations...\n");
		
		// open file to store adversary (if required)
		if (export_adv_enabled != EXPORT_ADV_NONE) {
			fp_adv = fopen(export_adv_filename, "w");
			if (!fp_adv) {
				PS_PrintWarningToMainLog(env, "Adversary generation cancelled (could not open file \"%s\").", export_adv_filename);
				export_adv_enabled = EXPORT_ADV_NONE;
			}
		}

		// store local copies of stuff
		double *non_zeros = ndsm->non_zeros;
		unsigned char *row_counts = ndsm->row_counts;
		int *row_starts = (int *)ndsm->row_counts;
		unsigned char *choice_counts = ndsm->choice_counts;
		int *choice_starts = (int *)ndsm->choice_counts;
		bool use_counts = ndsm->use_counts;
		unsigned int *cols = ndsm->cols;
		
		double *non_zeros_r[lenRew];
		for(int rewi = 0; rewi < lenRew; rewi++)
			non_zeros_r[rewi] = ndsm_r[rewi]->non_zeros;
		
		unsigned char *choice_counts_r[lenRew];
		for(int rewi = 0; rewi < lenRew; rewi++)
			choice_counts_r[rewi] = ndsm_r[rewi]->choice_counts;
		
		int *choice_starts_r[lenRew];
		for(int rewi = 0; rewi < lenRew; rewi++)
			choice_starts_r[rewi] = (int*)ndsm_r[rewi]->choice_counts;
		
		unsigned int *cols_r[lenRew];
		for(int rewi = 0; rewi < lenRew; rewi++)
			cols_r[rewi] = ndsm_r[rewi]->cols;
		
		bool doneBeforeBounded = false;
		
		h2_r = new int[lenRew];
		l2_r = new int[lenRew];
		k_r = new int[lenRew];
		while (!done && iters < max_iters_local) {
			iters++;
			
			// do matrix multiplication and min/max
			h1 = h2 = 0;
			for (int rewi = 0; rewi < lenRew; rewi++)
				h2_r[rewi] = 0;
			
			// loop through states
			for (i = 0; i < n; i++) {
				first = true;
				
				// first, get the decision of the adversary optimizing the combined reward
				d1 = -INFINITY;
				for (int it = 0; it < lenRew + lenProb; it++)
					if (it != ignoredWeight)
						pd1[it] = -INFINITY;
				
				// get pointers to nondeterministic choices for state i
				if (!use_counts) { l1 = row_starts[i]; h1 = row_starts[i+1]; }
				else { l1 = h1; h1 += row_counts[i]; }
				// loop through those choices
				for (j = l1; j < h1; j++) {
					// compute, for state i for this iteration,
					// the combined and individual reward values
					// start with 0 (we don't have any state rewards)
					d2 = 0;
					for (int it = 0; it < lenRew + lenProb; it++)
						if (it != ignoredWeight)
							pd2[it] = 0;
					// get pointers to transitions
					if (!use_counts) { l2 = choice_starts[j]; h2 = choice_starts[j+1]; }
					else { l2 = h2; h2 += choice_counts[j]; }
					// and get pointers to transition rewards
					for (int rewi = 0; rewi < lenRew; rewi++) {
						if (!ndsm_r[rewi]->use_counts) {
							l2_r[rewi] = choice_starts_r[rewi][j];
							h2_r[rewi] = choice_starts_r[rewi][j+1];
						} else {
							l2_r[rewi] = h2_r[rewi];
							h2_r[rewi] += choice_counts_r[rewi][j];
						}
					}
					// loop through transitions
					for (k = l2; k < h2; k++) {
						// for each reward structure
						for (int rewi = 0; rewi < lenRew; rewi++) {
							// find corresponding transition reward if any
							k_r[rewi] = l2_r[rewi];
							while (k_r[rewi] < h2_r[rewi] && cols_r[rewi][k_r[rewi]] != cols[k]) k_r[rewi]++;
							// if there is one, add reward * prob to combined and individual reward values
							if (k_r[rewi] < h2_r[rewi] && max_iters_local - iters < step_bounds_r[rewi]) {
								d2 += weights[rewi + lenProb] * non_zeros_r[rewi][k_r[rewi]] * non_zeros[k];
								if (lenProb + rewi != ignoredWeight) {
									pd2[rewi + lenProb] += non_zeros_r[rewi][k_r[rewi]] * non_zeros[k];
								}
								k_r[rewi]++;
							}
						}
						// add prob * corresponding reward from previous iteration
						// (for both combined and individual rewards)
						for (int it = 0; it < lenRew + lenProb; it++) {
							if (it != ignoredWeight) {
								pd2[it] += non_zeros[k] * psoln[it][cols[k]];
							}
						}
						d2 += non_zeros[k] * soln[cols[k]];
					}
					// see if the combined reward value is the min/max so far
					bool pickThis = first || (min&&(d2<d1)) || (!min&&(d2>d1));

					// if it equals the min/max do far for the combined reward value,
					// but it is better for some individual reward, we choose it.
					// not sure why
					if (!pickThis && (d2==d1)) {
						for (int it = 0; it < lenProb + lenRew; it++) {
							if (it != ignoredWeight) {
								if ((min&&(pd2[it]<pd1[it])) || (!min&&(pd2[it]>pd1[it]))) {
									pickThis = true;
									break;
								}
							}
						}
					}
					if (pickThis) {
						// store optimal values for combined and individual rewards
						d1 = d2;
						for (int it = 0; it < lenRew + lenProb; it++)
							if (it != ignoredWeight)
								pd1[it] = pd2[it];
						// if adversary generation is enabled, store optimal choice
						if (export_adv_enabled != EXPORT_ADV_NONE) {
							// if this is the first choice to be picked, always store it
							if (adv[i] == -1) {
								adv[i] = j;
							} else {
								// normally, we extract optimal choice differently for max
								// (only remember strictly better choices)
								// (which resolves problems with end components)
								// but here it's hard to know when it is max, due to the
								// mix of objectives and some being negated
								// so we just always only pick strictly better ones
								if ((min&&(d1<soln[i])) || (!min&&(d1>soln[i]))) {
									adv[i] = j;
								}
							}
						}
					}
					first = false;
				}
				
				// HOTFIX: it seems that on self loops d1 can be unchanged because the other for cycle is not executed, which is not desirable
				if (d1 == -INFINITY) {
					d1 = 0;
					for (int it = 0; it < lenRew + lenProb; it++) {
						pd1[it] = 0;
					}
				}
				
				double val_yes = 0.0;
				for (int probi = 0; probi < lenProb; probi++) {
					if (max_iters_local - iters < step_bounds[probi])
						val_yes += weights[probi] * yes_vec[probi][i];
				}
				
				//TODO: we need to handle val_yes somehow
				if (val_yes == 0 || d1>val_yes) {
					for (int it = 0; it < lenProb + lenRew; it++) {
						if (it != ignoredWeight) {
							psoln2[it][i] = pd1[it];
						}
					}
					soln2[i] = d1;
				} else {
					soln2[i] = 0;
					for (int probi = 0; probi < lenProb; probi++)
						if(max_iters_local - iters < step_bounds[probi])
							soln2[i] += weights[probi] * yes_vec[probi][i];
					
					for (int probi = 0; probi < lenProb; probi++)
						if (probi != ignoredWeight && max_iters_local - iters < step_bounds[probi])
							psoln2[probi][i] = yes_vec[probi][i];

					for (int rewi = 0; rewi < lenRew; rewi++)
						if (lenProb + rewi != ignoredWeight)
							psoln2[rewi + lenProb][i] = 0;
				}
			}

			//round small numbers to zero
			for (int o = 0; o < n; o++) {
				if (fabs(soln[o]) < near_zero) soln[o] = 0;
				if (fabs(soln2[o]) < near_zero) soln2[o] = 0;
			}
			for (int it = 0; it < lenRew + lenProb; it++)
				if (ignoredWeight != it)
					for (int o = 0; o < n; o++) {
						if (fabs(psoln[it][o]) < near_zero) psoln[it][o] = 0;
						if (fabs(psoln2[it][o]) < near_zero) psoln2[it][o] = 0;
					}

			// check convergence
			// (note: doing outside loop means may not need to check all elements)
			switch (term_crit) {
				case TERM_CRIT_ABSOLUTE:
					if (!weightedDone) {
						weightedDone = true;
						for (i = 0; i < n; i++) {
							if (fabs(soln2[i] - soln[i]) > term_crit_param) {
								weightedDone = false;
								goto end_switch;
							}
						}
					} else if (!doneBeforeBounded) {
						done = true;
						doneBeforeBounded = true;
						for (i = 0; i < n; i++) {
							for (int it = 0; it < lenProb + lenRew; it++) {
								if (it != ignoredWeight && fabs(psoln2[it][i] - psoln[it][i]) > term_crit_param) {
									done = false;
									doneBeforeBounded = false;
									goto end_switch;
								}
							}
						}
					}
				case TERM_CRIT_RELATIVE:
					if (!weightedDone) {
						weightedDone = true;
						for (i = 0; i < n; i++) {
							if (fabs(soln2[i] - soln[i])/fabs(soln2[i]) > term_crit_param) {
								weightedDone = false;
								goto end_switch;
							}
						}
					} else if (!doneBeforeBounded) {
						done = true;
						doneBeforeBounded = true;
						for (i = 0; i < n; i++) {
							for (int it = 0; it < lenProb + lenRew; it++) {
								if (it != ignoredWeight && fabs(psoln2[it][i] - psoln[it][i])/fabs(psoln2[it][i]) > term_crit_param) {
									done = false;
									doneBeforeBounded = false;
									goto end_switch;
								}
							}
						}
					}
					break;
			}
			
			//we can't stop if some of the objectives are step bounded,
			//maybe they were deactivated until now, so set the iters count so that
			//max_step_bound more iterations will be performed
			end_switch: if (done && max_step_bound > 0) {
				done = false;
				if (iters < max_iters_local - max_step_bound) {
					max_iters_local = iters + max_step_bound;
				}
			}
			
			// prepare for next iteration
			tmpsoln = soln;
			soln = soln2;
			soln2 = tmpsoln;
			
#ifdef MORE_OUTPUT
			PS_PrintToMainLog(env, "Soln: ");
			for (int o = 0; o < n; o++)
				PS_PrintToMainLog(env, "%e, ", soln[o]);
			PS_PrintToMainLog(env, "\n"); 
			PS_PrintToMainLog(env, "Soln2: ");
			for (int o = 0; o < n; o++)
				PS_PrintToMainLog(env, "%e, ", soln[o]);
			PS_PrintToMainLog(env, "\n");   
#endif
			
			for (int it = 0; it < lenRew + lenProb; it++) {
				if (it != ignoredWeight) {
					tmpsoln = psoln[it];
					psoln[it] = psoln2[it];
					psoln2[it] = tmpsoln;
				}
#ifdef MORE_OUTPUT
				PS_PrintToMainLog(env, "psoln: ");
				if (ignoredWeight != it)
					for (int o = 0; o < n; o++)
						PS_PrintToMainLog(env, "%e, ", psoln[it][o]);
				PS_PrintToMainLog(env, "\n"); 
				PS_PrintToMainLog(env, "psoln2: ");
				if (ignoredWeight != it)
					for (int o = 0; o < n; o++)
						PS_PrintToMainLog(env, "%e, ", psoln2[it][o]);
				PS_PrintToMainLog(env, "\n"); 
#endif          
			}
		}
		
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
		PS_PrintToMainLog(env, "Iterative method: %d iterations in %.2f seconds (average %.6f, setup %.2f)\n", iters, time_taken, time_for_iters/iters, time_for_setup);
		
		// if the iterative method didn't terminate, this is an error
		if (!doneBeforeBounded) { // || !weightedDone) {
			PS_SetErrorMessage("Iterative method did not converge within %d iterations.\nConsider using a different numerical method or increasing the maximum number of iterations", iters);
			throw 1;
		}

		//store the result
		ret = env->NewDoubleArray(lenProb + lenRew);   
		jdouble *retNative = env->GetDoubleArrayElements(ret, 0);
		
		// Display result
		PS_PrintToMainLog(env, "Optimal value for weights [");
		for (int it = 0; it < lenRew + lenProb; it++) {
			PS_PrintToMainLog(env, "%s%f", (it>0?",":""), weights[it]);
		}
		PS_PrintToMainLog(env, "] from initial state: %f\n", soln[start_index]);
		
		//copy all computed elements
		for (int it = 0; it < lenRew + lenProb; it++)
			if (it != ignoredWeight)
				retNative[it] = psoln[it][start_index];
		//compute the last element
		if (ignoredWeight != -1) {
			double last = soln[start_index];
			for (int it = 0; it < lenRew + lenProb; it++)
				if (it != ignoredWeight)
					last -= weights[it] * retNative[it];
			retNative[ignoredWeight] = (weights[ignoredWeight] > 0) ? (last / weights[ignoredWeight]) : 0.0;
		}
		
		env->ReleaseDoubleArrayElements(ret, retNative, 0);

		// close file to store adversary (if required)
		if (export_adv_enabled != EXPORT_ADV_NONE) {
			fclose(fp_adv);
			PS_PrintToMainLog(env, "\nAdversary written to file \"%s\".\n", export_adv_filename);
		}

		// export individual solution vectors
		if (export_adv_enabled != EXPORT_ADV_NONE && export_vectors) {
			for (int it = 0; it < lenRew + lenProb; it++) {
				if (it != ignoredWeight) {
					std::string export_vect_filename(export_adv_filename);
					export_vect_filename += ".vec";
					export_vect_filename += std::to_string(it);
					FILE *fp_vect = fopen(export_vect_filename.c_str(), "w");
					if (fp_vect) {
						PS_PrintWarningToMainLog(env, "Exporting solution vector %d to file %s.", it, export_vect_filename.c_str());
						for (i = 0; i < n; i++) {
							fprintf(fp_vect, "%d %g\n", i, psoln[it][i]);
						}
						fclose(fp_vect);
					}
				}
			}
		}
	
	// catch exceptions: register error, free memory
	} catch (std::bad_alloc e) {
		PS_SetErrorMessage("Out of memory");
		ret = 0;
	} catch (int e) {
		if (e==1) //1 means error was set above and exception was thrown to end the computation
			ret = 0;
		else 
			PS_SetErrorMessage("Unknown error.");
	}

	// free memory
	if (soln2) delete[] soln2;
	if (soln) delete[] soln;
	if (yes_vec) delete[] yes_vec;
	if (h2_r) delete[] h2_r;
	if (l2_r) delete[] l2_r;
	if (k_r) delete[] k_r;
	if (pd1) delete[] pd1;
	if (pd2) delete[] pd2;
	for (int it = 0; it < lenProb + lenRew; it++) {
		if (it != ignoredWeight) {
			if (psoln2[it]) delete[] psoln2[it];
			if (psoln[it]) delete[] psoln[it];
		}
	}
	if (psoln2) delete[] psoln2;
	if (psoln) delete[] psoln;
	if (adv) delete[] adv;
	if (action_names != NULL) {
		release_string_array_from_java(env, action_names_jstrings, action_names, num_actions);
	}

	return ret;
}

//------------------------------------------------------------------------------

