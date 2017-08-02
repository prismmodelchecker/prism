//==============================================================================
//  
//  Copyright (c) 2002-
//  Authors:
//  * Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//  * Hongyang Qu <hongyang.qu@comlab.ox.ac.uk> (University of Oxford)
//  
//------------------------------------------------------------------------------
//  
//  This file is part of PRISM.
//  
//  PRISM is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//  
//  PRISM is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//  
//  You should have received a copy of the GNU General Public License
//  along with PRISM; if not, write to the Free Software Foundation,
//  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
#include "sparse_adv.h"
#include "prism.h"
#include "PrismSparseGlob.h"
#include "PrismNativeGlob.h"
#include "jnipointer.h"
#include <new>
#include "lp_lib.h"

//------------------------------------------------------------------------------

JNIEXPORT jdouble __jlongpointer JNICALL Java_sparse_PrismSparse_PS_1NondetMultiReach
(
 JNIEnv *env,
 jclass cls,
 jlong __jlongpointer t, // trans matrix
 jlong __jlongpointer ta, // trans action labels
 jobject synchs,
 jlong __jlongpointer od, // odd
 jlong __jlongpointer rv, // row vars
 jint num_rvars,
 jlong __jlongpointer cv, // col vars
 jint num_cvars,
 jlong __jlongpointer ndv, // nondet vars
 jint num_ndvars,
 jlongArray _targets, // target state sets
 jintArray _relops, // target relops (0:=?, 1:>, 2:>=)
 jdoubleArray _bounds, // target probability bounds
 jlong __jlongpointer m, // 'maybe' states
 jlong __jlongpointer _start // initial state(s)
 )
{
  // cast function parameters
  DdNode *trans = jlong_to_DdNode(t); // trans matrix
  DdNode *trans_actions = jlong_to_DdNode(ta);  // trans action labels
  ODDNode *odd = jlong_to_ODDNode(od); // reachable states
  DdNode **rvars = jlong_to_DdNode_array(rv); // row vars
  DdNode **cvars = jlong_to_DdNode_array(cv); // col vars
  DdNode **ndvars = jlong_to_DdNode_array(ndv); // nondet vars
  DdNode *maybe = jlong_to_DdNode(m); // 'maybe' states
  DdNode *start = jlong_to_DdNode(_start); // initial state(s)

  // target info  
  jlong *target_ptrs = NULL;
  DdNode **targets = NULL;
  jint *relops = NULL;
  double *bounds = NULL;
  // mtbdds
  DdNode *a = NULL, **yes = NULL, *maybe_yes = NULL, *loops = NULL, *tmp = NULL;
  // model stats
  int num_targets, n, nc, nc_r;
  long nnz, nnz_r;
  // sparse matrix
  NDSparseMatrix *ndsm = NULL, *ndsm_r = NULL;
  // action info
  jstring *action_names_jstrings;
  const char** action_names = NULL;
  int num_actions;
  // vectors
  double **yes_vecs, *maybe_vec = NULL/*, *maybe_vec_r = NULL*/;
  // timing stuff
  long start1, start2, start3, stop, stop2;
  double time_taken, time_for_setup, time_for_lp;
  // lp stuff
  lprec *lp = NULL;
  REAL *arr_reals = NULL;// *lp_soln = NULL;
  int *arr_ints = NULL;
  bool selfloop;
  int arr_size, res;
  int num_lp_vars;
  double *lp_soln;
  double lp_result = 0.0;
  bool lp_solved = false;
  // misc
  int i, j, k, k_r, l1, h1, l2, h2, l2_r, h2_r, start_index, count;
  double d1, d2, kb, kbt;
  bool done, first;
  jclass vn_cls;
  jmethodID vn_mid;

  // exception handling around whole function
  try {

    // start clocks 
    start1 = start2 = util_cpu_time();
    
    // Extract arrays of target info from function parameters
    num_targets = (int)env->GetArrayLength(_targets);
    target_ptrs = env->GetLongArrayElements(_targets, 0);
    targets = new DdNode*[num_targets];
    for (i = 0; i < num_targets; i++) targets[i] = jlong_to_DdNode(target_ptrs[i]);
    relops = env->GetIntArrayElements(_relops, 0);
    bounds = env->GetDoubleArrayElements(_bounds, 0);
    yes = new DdNode*[num_targets];
    yes_vecs = new double*[num_targets];
    for(i=0; i<num_targets; i++) yes[i] = targets[i];
  
    // Display some info about the targets
    PS_PrintToMainLog(env, "\n%d Targets:\n", num_targets);
    for (i = 0; i < num_targets; i++) {
      PS_PrintToMainLog(env, "#%d: ", i);
      switch (relops[i]) {
      case 0: PS_PrintToMainLog(env, "Pmax=?"); break;
      case 1: PS_PrintToMainLog(env, "P>%g", bounds[i]); break;
      case 2: PS_PrintToMainLog(env, "P>=%g", bounds[i]); break;
      }
      PS_PrintToMainLog(env, " (%.0f states)\n", DD_GetNumMinterms(ddman, targets[i], num_rvars));
    }
  
    // Filter out rows, store in "a"
    Cudd_Ref(maybe);
    maybe_yes = maybe;
    for (i = 0; i < num_targets; i++) {
      Cudd_Ref(yes[i]);
      maybe_yes = DD_Or(ddman, maybe_yes, yes[i]);
    }
    Cudd_Ref(trans);
    Cudd_Ref(maybe_yes);
    a = DD_Apply(ddman, APPLY_TIMES, trans, maybe_yes);

    // For efficiency, remove any probability 1 self-loops from the model.
    // For multi-objective, we always do maximum reachability, so these do not matter.
    Cudd_Ref(a);
    loops = DD_And(ddman, DD_Equals(ddman, a, 1.0), DD_Identity(ddman, rvars, cvars, num_rvars));
    loops = DD_ThereExists(ddman, loops, cvars, num_rvars);
    Cudd_Ref(loops);
    a = DD_ITE(ddman, loops, DD_Constant(ddman, 0), a);
    
    // Get number of states
    n = odd->eoff + odd->toff;

    // Build sparse matrix
    PS_PrintToMainLog(env, "\nBuilding sparse matrix... ");
    ndsm = build_nd_sparse_matrix(ddman, a, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
    // Get number of transitions/choices
    nnz = ndsm->nnz;
    nc = ndsm->nc;
    kb = ndsm->mem;
    kbt = kb;
    // print out info
    PS_PrintToMainLog(env, "[n=%d, nc=%d, nnz=%ld, k=%d] ", n, nc, nnz, ndsm->k);
    PS_PrintMemoryToMainLog(env, "[", kb, "]\n");

    // If needed, and if info is available, build a vector of action indices for the MDP
    if (export_adv != EXPORT_ADV_NONE) {
      if (trans_actions != NULL) {
        PS_PrintToMainLog(env, "Building action information... ");
        // first need to filter out unwanted rows
        Cudd_Ref(trans_actions);
        Cudd_Ref(maybe_yes);
        tmp = DD_Apply(ddman, APPLY_TIMES, trans_actions, maybe_yes);
        Cudd_Ref(loops);
        tmp = DD_ITE(ddman, loops, DD_Constant(ddman, 0), tmp);
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
  
    // Get vectors for yes/maybe
    PS_PrintToMainLog(env, "Creating vectors for yes... ");
    for(i=0; i<num_targets; i++) {
      yes_vecs[i] = mtbdd_to_double_vector(ddman, yes[i], rvars, num_rvars, odd);
    }
    kb = n*sizeof(int)/1024.0;
    kbt += num_targets * kb;
    PS_PrintToMainLog(env, "[%d x ", num_targets);
    PS_PrintMemoryToMainLog(env, "", kb, "]\n");
    PS_PrintToMainLog(env, "Creating vector for maybe... ");
    maybe_vec = mtbdd_to_double_vector(ddman, maybe, rvars, num_rvars, odd);
    kb = n*8.0/1024.0;
    kbt += kb;
    PS_PrintMemoryToMainLog(env, "[", kb, "]\n");

    // Get index of single (first) initial state
    start_index = get_index_of_first_from_bdd(ddman, start, rvars, num_rvars, odd);
    PS_PrintToMainLog(env, "Initial state index: %1d\n", start_index);

    // Print total memory usage
    PS_PrintMemoryToMainLog(env, "TOTAL: [", kbt, "]\n");

    // Store local copies of sparse matrix stuff
    double *non_zeros = ndsm->non_zeros;
    unsigned char *row_counts = ndsm->row_counts;
    int *row_starts = (int *)ndsm->row_counts;
    unsigned char *choice_counts = ndsm->choice_counts;
    int *choice_starts = (int *)ndsm->choice_counts;
    bool use_counts = ndsm->use_counts;
    unsigned int *cols = ndsm->cols;

    // Set up LP problem...
    PS_PrintToMainLog(env, "\nBuilding LP problem...\n");
    
    int *yes_vec;
    int *map_var;

    yes_vec = new int[n];
    map_var = new int[n+1];

    arr_ints = new int[n];
    int sp;

    
    // Compute the number of LP variables needed
    // (one per choice for each 'maybe'/'yes' state + one extra for each yes state)
    // Also build map_var (mapping from states to first corresponding LP var)
    // Init counters (maybe_nc/yes_nc = num maybe/yes choices, yes_count = num yes, count = num vars so far)
    int maybe_nc = 0;
    int yes_nc = 0;
    int yes_count = 0;
    count = 0;
    // Traverse sparse matrix to get info
    h1 = h2 = 0;
    for (i = 0; i < n; i++) {
      if (!use_counts) {
        l1 = row_starts[i];
        h1 = row_starts[i+1];
      } else {
        l1 = h1;
        h1 += row_counts[i];
      }
      k=0;
      for (j = l1; j < h1; j++) {
        if (!use_counts) {
          l2 = choice_starts[j];
          h2 = choice_starts[j+1];
        } else {
          l2 = h2;
          h2 += choice_counts[j];
        }
        k++;
      }
      // Store first LP var for state i
      map_var[i] = count;
      // If a maybe state...
      if (maybe_vec[i] > 0) {
        maybe_nc += k;
        count += k;
      }
      else {
        for (j=0; j<num_targets; j++) {
          // If a yes state...
          if (yes_vecs[j][i] > 0) {
            yes_nc += k;
            yes_count ++; // each target state has one extra action
            count += k+1;
            // Skip any further targets for this state
            break;
          }
        }
      }
    }
    // Compute total var count
    num_lp_vars = maybe_nc + yes_nc + yes_count;
    // Store first LP var for final state
    map_var[n] = num_lp_vars; // maybe need to be modified.   
    PS_PrintToMainLog(env, "Number of LP variables = %1d\n", num_lp_vars);

    for(i=0; i<n; i++)
      yes_vec[i] = 0;
    for(i=0; i<n; i++) {
      for(j=0; j<num_targets; j++) {
        if(yes_vecs[j][i]> 0) { // For each state, count the number of targets it belongs to.
          yes_vec[i]++;
        }
      }
    }

    /*printf("map_var = ");
      for(i=0; i<=n; i++)
      printf(" %1d", map_var[i]);
      printf("\n");
      fflush(stdout);*/

    h1 = h2 = 0;
    for(i=0; i<n; i++)
      arr_ints[i] = map_var[i+1]-map_var[i];
    for (i = 0; i < n; i++) {
      if (!use_counts) {
        l1 = row_starts[i];
        h1 = row_starts[i+1];
      } else {
        l1 = h1;
        h1 += row_counts[i];
      }
      for (j = l1; j < h1; j++) { // j: choice index; x+j: variable index 
        if (!use_counts) {
          l2 = choice_starts[j];
          h2 = choice_starts[j+1];
        } else {
          l2 = h2;
          h2 += choice_counts[j];
        }
        if (maybe_vec[i]> 0 || yes_vec[i]> 0)
          for(k=l2; k<h2; k++) {
            if(maybe_vec[cols[k]]> 0 || yes_vec[cols[k]]> 0)
              arr_ints[cols[k]]++;
          }
      }
    }
    int *constraints_sp;
    int *constraints_ints;
    double *constraints_reals;
    int **constraints_int_ptr;
    double **constraints_real_ptr;
    constraints_sp = new int[n];
    constraints_int_ptr = new int*[n];
    constraints_real_ptr = new double*[n];
    for(i=0; i<n; i++)
      if(maybe_vec[i]> 0 || yes_vec[i]> 0) {
        //printf("i = %1d, ", i);
        constraints_ints = new int[arr_ints[i]];
        constraints_reals = new REAL[arr_ints[i]];
        constraints_sp[i] = map_var[i+1]-map_var[i];
        constraints_int_ptr[i] = constraints_ints;
        constraints_real_ptr[i] = constraints_reals;
        for(j=0; j<map_var[i+1]-map_var[i]; j++) {
          constraints_ints[j] = map_var[i] + j + 1; // index starts at 1 in lp_solve
          constraints_reals[j] = 1.0;
        }
        for(j=map_var[i+1]-map_var[i]; j<arr_ints[i]; j++) {
          constraints_ints[j] = -1;
          constraints_reals[j] = 0;
        }
      }

    arr_reals = new REAL[num_lp_vars];

    if ((lp=make_lp(0, num_lp_vars)) == NULL) throw "Could not create LP problem";
    // Lower verbosity: Warnings and errors only
    set_verbose(lp, IMPORTANT);
    // Set mode: will create row by row
    set_add_rowmode(lp, true);

    // Add constraints to LP problem
    int x;
    h1 = h2 = 0;
    for (x = 0; x < n; x++) { // h2: row index
      if(maybe_vec[x]> 0 || yes_vec[x]> 0) {
        if (!use_counts) {
          l1 = row_starts[x];
          h1 = row_starts[x+1];
        } else {
          l1 = h1;
          h1 += row_counts[x];
        }
        count = 0;
        for (j = l1; j < h1; j++) { // j: choice index; x+j: variable index 
          if (!use_counts) {
            l2 = choice_starts[j];
            h2 = choice_starts[j+1];
          } else {
            l2 = h2;
            h2 += choice_counts[j];
          }
          for(k=l2; k<h2; k++) { // k: column index
            // get the index of the corresponding variable
            i = cols[k];
            if(maybe_vec[i]> 0 || yes_vec[i]> 0) {
              constraints_ints = constraints_int_ptr[i];
              constraints_reals = constraints_real_ptr[i];
              for(k_r=0; k_r<constraints_sp[i]; k_r++)
                if(constraints_ints[k_r]==map_var[x]+j-l1-count+1)
                  break;

              if(k_r == constraints_sp[i]) {
                //count++;
                constraints_ints[k_r]=map_var[x]+j-l1-count + 1; // index starts at 1 in lp_solve
                constraints_reals[k_r] = -non_zeros[k];
                constraints_sp[i]++;
              } else {
                constraints_reals[k_r] -= non_zeros[k];
                if(constraints_reals[k_r] == 0.0) { // In fact, this cannot happen.
                  //count--;
                  for(l2_r=k_r; l2_r<constraints_sp[i]-1; l2_r++) {
                    constraints_reals[l2_r] = constraints_reals[l2_r+1];
                    constraints_ints[l2_r] = constraints_ints[l2_r+1];
                  }
                  constraints_sp[i]--;
                }
              }
            }
          }
        }
      } else if(use_counts) {
        l1 = h1;
        h1 += row_counts[x];
        for (j = l1; j < h1; j++)
          h2 += choice_counts[j];
      }
    }

    for (i = 0; i < n; i++)
      if (maybe_vec[i]> 0 || yes_vec[i]> 0) {
        constraints_ints = constraints_int_ptr[i];
        constraints_reals = constraints_real_ptr[i];
        add_constraintex(lp, constraints_sp[i], constraints_reals, constraints_ints, EQ, (start_index==i ? 1.0 : 0.0));
        delete[] constraints_ints;
        delete[] constraints_reals;
      }
    delete[] constraints_sp;
    delete[] constraints_int_ptr;
    delete[] constraints_real_ptr;
    
    // Add LP constraints for bounded (non-quantitative) objectives
    PS_PrintToMainLog(env, "Adding extra constraints for bounded objectives...\n");
    constraints_ints = new int[num_lp_vars];
    for (i=0; i<num_targets; i++) {
      // Skip quantitative constraint
      if (relops[i]==0 || relops[i]>2)
        continue;
      count = 0;
      for(k=0; k<n; k++) {
        if(yes_vecs[i][k]> 0) {
          constraints_ints[count] = map_var[k+1];
          arr_reals[count] = 1.0;
          count++;
        }
      }
      add_constraintex(lp, count, arr_reals, constraints_ints, GE, bounds[i]);
    }

    // Set objective function for LP
    PS_PrintToMainLog(env, "Setting objective...\n");
    x = 0;
    if(relops[0]> 0 && relops[0]<=2) {
      for(i=0; i<n; i++) {
        if(yes_vec[i]> 0) {
          constraints_ints[x] = map_var[i+1];
          arr_reals[x++] = 1.0;
        }
      }
    } else if(relops[0] == 0) {
      for(i=0; i<n; i++)
        if(yes_vecs[0][i]> 0) {
          constraints_ints[x] = map_var[i+1];
          arr_reals[x++] = 1.0;
        }
    }
    set_maxim(lp);
    set_obj_fnex(lp, x, arr_reals, constraints_ints);
    delete[] constraints_ints;
    
    // Finished building LP problem
    set_add_rowmode(lp, false);
    
    // Get setup time
    stop = util_cpu_time();
    time_for_setup = (double)(stop - start2)/1000;
    start2 = stop;
    
    // Export the MDP to a dot file
    //export_model(ndsm, n, yes_vec, start_index);
    
    // Solve the LP, extract result
    PS_PrintToMainLog(env, "Solving LP problem...\n");
    res = solve(lp);
      
    //Get LP solving time
    stop2 = util_cpu_time();  
    time_for_lp = (double)(stop2 - start2)/1000;
      
    if (res != 0) {
      PS_PrintToMainLog(env, "No solution\n");
      lp_solved = false;
    } else {
      lp_solved = true;
      lp_result = get_objective(lp);
      lp_soln = new double[num_lp_vars];
      get_ptr_variables(lp, &lp_soln);
        
      // Generate adversary from the solution, if required
      if (export_adv != EXPORT_ADV_NONE) {
		// Adversary generation
		export_adversary_ltl_tra(export_adv_filename, ndsm, ndsm->actions, action_names, yes_vec, maybe_vec, num_lp_vars, map_var, lp_soln, start_index);
		//export_adversary_ltl_dot(env, ndsm, n, nnz, yes_vec, maybe_vec, num_lp_vars, map_var, lp_soln, start_index);
      }
      /*for (i=0; i<num_lp_vars; i++) {
        if(lp_soln[i] != 0) {
          PS_PrintToMainLog(env, "X%d = %g    ", i, lp_soln[i]);
          count++;
        }
        if(count == 8) {
          PS_PrintToMainLog(env, "\n");
          count = 0;
        }
      }
      if(count)
			PS_PrintToMainLog(env, "\n");*/
    }

    // Modify result based on type
    if (relops[0] > 0) {
      // for qualitative queries, return 1/0 for existence of solution or not
      PS_PrintToMainLog(env, "LP problem solution %sfound so result is %s\n",  lp_solved ? "" : "not ", lp_solved ? "true" : "false");
      lp_result = lp_solved ? 1.0 : 0.0;
    } else {
      // return NaN for quantitative queries that can't be solved
      PS_PrintToMainLog(env, "LP problem solution %sfound; result is %f\n",  lp_solved ? "" : "not ", lp_result);
      if (!lp_solved) lp_result = NAN;
    }
  
    // Print timing info
    time_taken = time_for_setup + time_for_lp;
    PS_PrintToMainLog(env, "\nLP problem solved in %.2f seconds (setup %.2f, lpsolve %.2f)\n", time_taken, time_for_setup, time_for_lp);

    delete yes_vec;
    delete map_var;
    
    // Catch exceptions: register error
  } catch (std::bad_alloc e) {
    PS_SetErrorMessage("Out of memory");
    lp_result = NAN;
  }

  // Free memory
  if (lp) delete_lp(lp);
  if (a) Cudd_RecursiveDeref(ddman, a);
  if (maybe_yes) Cudd_RecursiveDeref(ddman, maybe_yes);
  if (loops) Cudd_RecursiveDeref(ddman, loops);
  if (ndsm) delete ndsm;
  if (ndsm_r) delete ndsm_r;
  delete[] maybe_vec;
  for (i = 0; i < num_targets; i++)
    delete[] yes_vecs[i];
  delete[] arr_reals;
  delete[] arr_ints;
  if (target_ptrs) env->ReleaseLongArrayElements(_targets, target_ptrs, 0);
  if (relops) env->ReleaseIntArrayElements(_relops, relops, 0);
  if (bounds) env->ReleaseDoubleArrayElements(_bounds, bounds, 0);
  if (action_names != NULL) {
    release_string_array_from_java(env, action_names_jstrings, action_names, num_actions);
  }

  return lp_result;
}

//------------------------------------------------------------------------------

