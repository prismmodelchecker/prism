//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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
#include <math.h>
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
	bool adv_loop = false;
	FILE *fp_adv = NULL;
	int adv_j;
	//int *adv = NULL;
	// action info
	int *actions;
	jstring *action_names_jstrings;
	int num_actions;
	// misc
	int i, j, k, l1, h1, l2, h2, iters;
	int *k_r = NULL, *l2_r = NULL, *h2_r= NULL;
	double d1, d2, kb, kbt;
	double *pd1 = NULL, *pd2 = NULL;
	bool done, weightedDone, first;
	const char** action_names = NULL;
	int num_yes = 0;
	int start_index;
	unsigned int *row_starts1, *predecessors;
	unsigned int extra_node;

	//in this we will store the result array, 0 returned means error
	jdoubleArray ret = 0;
	
	bool has_rewards = _ndsm_r != 0;
	bool has_yes_vec = _yes_vec != 0;
    
	//we will change maximal number of iters, so make sure we don't change the global number
	int max_iters_local = max_iters;
    
	jsize lenRew = (has_rewards) ? env->GetArrayLength(_ndsm_r) : 0;
	jsize lenProb = (has_yes_vec) ? env->GetArrayLength(_yes_vec) : 0;
    
	double* weights = env->GetDoubleArrayElements(_weights, 0);
	
	//We will ignore one of the rewards and compute it's value from the other ones and
	//from the combined value. We must make sure that this reward has nonzero weight,
	//otherwise we can't compute it.
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
		
		// get number of transitions/choices
		nnz = ndsm->nnz;
		nc = ndsm->nc;
		kb = ndsm->mem;
		kbt = kb;
		
		jlong *ptr_ndsm_r = (has_rewards) ? env->GetLongArrayElements(_ndsm_r, 0) : NULL;
		jlong *ptr_yes_vec = (has_yes_vec) ? env->GetLongArrayElements(_yes_vec, 0) : NULL;
		
		NDSparseMatrix *ndsm_r[lenRew];
		
		for(int rewi = 0; rewi < lenRew; rewi++)
			ndsm_r[rewi] = (NDSparseMatrix *) jlong_to_NDSparseMatrix(ptr_ndsm_r[rewi]);
		
		double* weights = env->GetDoubleArrayElements(_weights, 0);
		
		int* step_bounds_r = (has_rewards) ? (int*)env->GetIntArrayElements(_ndsm_r_step_bounds, 0) : NULL;
		int* step_bounds = (has_yes_vec) ? (int*)env->GetIntArrayElements(_prob_step_bounds, 0) : NULL;

		
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
		
		// if needed, and if info is available, build a vector of action indices for the MDP
		actions = NULL;
		
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
		
		// Get index of single (first) initial state
		start_index = get_index_of_first_from_bdd(ddman, start, rvars, num_rvars, odd);
		
		// initial solution is yes
		for (i = 0; i < n; i++) {
			soln[i] = 0;
			soln2[i] = 0;
			for (int probi = 0; probi < lenProb; probi++) {
				if (step_bounds[probi] == max_iters_local)
					soln[i] += weights[probi] * yes_vec[probi][i];
				//PS_PrintToMainLog(env, "s: %d %d %f %f %f\n", i, probi, soln[i], weights[probi], yes_vec[probi][i]);
			}
			for (int probi = 0; probi < lenProb; probi++) {
				if (probi != ignoredWeight) {
					if (step_bounds[probi] == max_iters_local) {
						//PS_PrintToMainLog(env, "setting %d psoln to some number\n", probi);
						psoln[probi][i] = 0;//yes_vec[probi][i];
					}
					else {
						psoln[probi][i] = 0;
						//PS_PrintToMainLog(env, "setting %d psoln to 0\n", probi);
					}
					psoln2[probi][i] = 0;
				}
			}
			for (int rewi = 0; rewi < lenRew; rewi++) {
				//soln[rewi + lenProb][i] = 0;
				if (lenProb + rewi != ignoredWeight) {
					psoln[rewi + lenProb][i] = 0;
					psoln2[rewi + lenProb][i] = 0;
				}
			}
		}
		
#ifdef MORE_OUTPUT
		PS_PrintToMainLog(env, "Initial soln: ");
		for (int o = 0; o < n; o++)
			PS_PrintToMainLog(env, "%f, ", soln[o]);
		PS_PrintToMainLog(env, "\n");
		
		
		for (int it = 0; it < lenRew + lenProb; it++) {
			PS_PrintToMainLog(env, "Initial psoln: ");
			if (ignoredWeight != it)
				for (int o = 0; o < n; o++)
					PS_PrintToMainLog(env, "%f, ", psoln[it][o]);
			PS_PrintToMainLog(env, "\n");  
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
			
			for (i = 0; i < n; i++) {
				//first, get the decision of the adversary optimizing the combined reward
				d1 = -INFINITY;
				for (int it = 0; it < lenRew + lenProb; it++)
					if (it != ignoredWeight)
						pd1[it] = -INFINITY;
				
				first = true; // (because we also remember 'first')
				if (!use_counts) { l1 = row_starts[i]; h1 = row_starts[i+1]; }
				else { l1 = h1; h1 += row_counts[i]; }
				for (j = l1; j < h1; j++) {
					d2 = 0;
					for (int it = 0; it < lenRew + lenProb; it++)
						if (it != ignoredWeight)
							pd2[it] = 0;
					if (!use_counts) { l2 = choice_starts[j]; h2 = choice_starts[j+1]; }
					else { l2 = h2; h2 += choice_counts[j]; }
					for (int rewi = 0; rewi < lenRew; rewi++) {
						if (!ndsm_r[rewi]->use_counts) {
							l2_r[rewi] = choice_starts_r[rewi][j];
							h2_r[rewi] = choice_starts_r[rewi][j+1];
						} else {
							l2_r[rewi] = h2_r[rewi];
							h2_r[rewi] += choice_counts_r[rewi][j];
						}
					}
					for (k = l2; k < h2; k++) {
						for (int rewi = 0; rewi < lenRew; rewi++) {
							k_r[rewi] = l2_r[rewi];
							while (k_r[rewi] < h2_r[rewi] && cols_r[rewi][k_r[rewi]] != cols[k])
								k_r[rewi]++;
							// if there is one, add reward * prob to reward value
							if (k_r[rewi] < h2_r[rewi] && max_iters_local - iters < step_bounds_r[rewi]) {
								d2 += weights[rewi + lenProb] * non_zeros_r[rewi][k_r[rewi]] * non_zeros[k];
								if (lenProb + rewi != ignoredWeight) {
									pd2[rewi + lenProb] += non_zeros_r[rewi][k_r[rewi]] * non_zeros[k];
								}
								k_r[rewi]++;
							}
						}
						
						//add value of successors
						for (int it = 0; it < lenRew + lenProb; it++) {
							if (it != ignoredWeight)
								pd2[it] += non_zeros[k] * psoln[it][cols[k]];
						}
						
						d2 += non_zeros[k] * soln[cols[k]];
					}
					
					bool pickThis = first || (min&&(d2<d1)) || (!min&&(d2>d1));
					
					//HOTFIX for cumulative reward
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
						d1 = d2;
						for (int it = 0; it < lenRew + lenProb; it++)
							if (it != ignoredWeight)
								pd1[it] = pd2[it];
					}
					first = false;
				}
				
				double val_yes = 0.0;
				for (int probi = 0; probi < lenProb; probi++) {
					if (max_iters_local - iters < step_bounds[probi])
						val_yes += weights[probi] * yes_vec[probi][i];
				}
				
				//HOTFIX: it seems that on self loops d1 can be unchanged because the other for cycle is not executed, which is not desirable
				if (d1==-INFINITY) {
					d1 = 0;
					for (int it = 0; it < lenRew + lenProb; it++)
						pd1[it] = 0;
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
		
		//copy all computed elements
		for (int it = 0; it < lenRew + lenProb; it++)
			if (it != ignoredWeight)
				retNative[it] = max_double_vector_over_bdd(ddman, psoln[it], start, rvars, num_rvars, odd);
		//compute the last element
		if (ignoredWeight != -1) {
			double last = max_double_vector_over_bdd(ddman, soln, start, rvars, num_rvars, odd);
			for (int it = 0; it < lenRew + lenProb; it++)
				if (it != ignoredWeight)
					last -= weights[it] * retNative[it];
			retNative[ignoredWeight] = (weights[ignoredWeight] > 0) ? (last / weights[ignoredWeight]) : 0.0;
		}
		
		env->ReleaseDoubleArrayElements(ret, retNative, 0);

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
	    
	if (actions != NULL) {
		delete[] actions;
		release_string_array_from_java(env, action_names_jstrings, action_names, num_actions);
	}

	return ret;
}

//------------------------------------------------------------------------------

