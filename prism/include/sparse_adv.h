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

#include <util.h>
#include <cudd.h>
#include <odd.h>
#include "sparse.h"

// Flags for building Windows DLLs
#ifdef __MINGW32__
	#define EXPORT __declspec(dllexport)
#else
	#define EXPORT
#endif

// function prototypes

void export_model(NDSparseMatrix *ndsm, int n, int *yes_vec, int start_index);
void export_adversary_ltl_dot(NDSparseMatrix *ndsm, int n, long nnz, int *yes_vec, double *maybe_vec, int num_lp_vars, int *map_var, double *lp_soln, int start_index);
void export_adversary_ltl_dot_reward(const char *export_adv_filename, NDSparseMatrix *ndsm, int *actions, const char** action_names, int n, long nnz, int *yes_vec, double *maybe_vec, int num_lp_vars, int *map_var, double *lp_soln, double *back_arr_reals, int start_index);
void export_adversary_ltl_tra(const char *export_adv_filename, NDSparseMatrix *ndsm, int *actions, const char** action_names, int *yes_vec, double *maybe_vec, int num_lp_vars, int *map_var, double *lp_soln, int start_index);

//------------------------------------------------------------------------------
