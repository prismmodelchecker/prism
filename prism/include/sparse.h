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

#ifndef SPARSE_H
#define SPARSE_H

#include <util.h>
#include <cudd.h>
#include <odd.h>

// Flags for building Windows DLLs
#ifdef __MINGW32__
	#define EXPORT __declspec(dllexport)
#else
	#define EXPORT
#endif

// data structures

// "row major" sparse matrix

typedef struct RMSparseMatrix RMSparseMatrix;

struct RMSparseMatrix
{
	int n;				// num states
	int nnz;			// num non zeros
	bool use_counts;	// store counts? (as opposed to starts)
	double mem;			// memory used
	
	double *non_zeros;
	unsigned int *cols;
	unsigned char *row_counts;
	
	EXPORT RMSparseMatrix();
	EXPORT ~RMSparseMatrix();
};

// "column major" sparse matrix

typedef struct CMSparseMatrix CMSparseMatrix;

struct CMSparseMatrix
{
	int n;				// num states
	int nnz;			// num non zeros
	bool use_counts;	// store counts? (as opposed to starts)
	double mem;			// memory used
	
	double *non_zeros;
	unsigned int *rows;
	unsigned char *col_counts;
	
	EXPORT CMSparseMatrix();
	EXPORT ~CMSparseMatrix();
};

// "row/column" sparse matrix

typedef struct RCSparseMatrix RCSparseMatrix;

struct RCSparseMatrix
{
	int n;				// num states
	int nnz;			// num non zeros
	bool use_counts;	// store counts? (as opposed to starts)
	double mem;			// memory used
	
	double *non_zeros;
	unsigned int *rows;
	unsigned int *cols;
	
	EXPORT RCSparseMatrix();
	EXPORT ~RCSparseMatrix();
};

// "compact modified sparse row" sparse matrix

typedef struct CMSRSparseMatrix CMSRSparseMatrix;

struct CMSRSparseMatrix
{
	int n;				// num states
	int nnz;			// num non zeros
	bool use_counts;	// store counts? (as opposed to starts)
	double mem; 		// memory used
	
	double *dist;		// distinct values info
	int dist_num;
	int dist_shift;
	int dist_mask;
	
	unsigned int *cols;
	unsigned char *row_counts;
	
	EXPORT CMSRSparseMatrix();
	EXPORT ~CMSRSparseMatrix();
};

// "compact modified sparse column" sparse matrix

typedef struct CMSCSparseMatrix CMSCSparseMatrix;

struct CMSCSparseMatrix
{
	int n;				// num states
	int nnz;			// num non zeros
	bool use_counts;	// store counts? (as opposed to starts)
	double mem; 		// memory used
	
	double *dist;		// distinct values info
	int dist_num;
	int dist_shift;
	int dist_mask;
	
	unsigned int *rows;
	unsigned char *col_counts;
	
	EXPORT CMSCSparseMatrix();
	EXPORT ~CMSCSparseMatrix();
};

// nondeterministic (mdp) sparse matrix

typedef struct NDSparseMatrix NDSparseMatrix;

struct NDSparseMatrix
{
	int n;				// num states
	int nc;				// num choices
	int nnz;			// num non zeros
//	int nm; 			// num matrices (upper bound on max num choices in a state)
	int k;				// max num choices in a state
	bool use_counts;	// store counts? (as opposed to starts)
	double mem;			// memory used
	
	double *non_zeros;
	unsigned int *cols;
	unsigned char *row_counts;
	unsigned char *choice_counts;
	
	int *actions; // indices for actions of each choice
	
	EXPORT NDSparseMatrix();
	EXPORT ~NDSparseMatrix();
};

// function prototypes

RMSparseMatrix *build_rm_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd);
CMSparseMatrix *build_cm_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd);
RCSparseMatrix *build_rc_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd);
CMSRSparseMatrix *build_cmsr_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd);
CMSCSparseMatrix *build_cmsc_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd);
NDSparseMatrix *build_nd_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, DdNode **ndvars, int num_ndvars, ODDNode *odd);
NDSparseMatrix *build_sub_nd_sparse_matrix(DdManager *ddman, DdNode *mdp, DdNode *submdp, DdNode **rvars, DdNode **cvars, int num_vars, DdNode **ndvars, int num_ndvars, ODDNode *odd);
void build_nd_action_vector(DdManager *ddman, DdNode *mdp, DdNode *trans_actions, NDSparseMatrix *mdp_ndsm, DdNode **rvars, DdNode **cvars, int num_vars, DdNode **ndvars, int num_ndvars, ODDNode *odd);

RMSparseMatrix *build_rm_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd, bool transpose);
CMSparseMatrix *build_cm_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd, bool transpose);
RCSparseMatrix *build_rc_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd, bool transpose);
CMSRSparseMatrix *build_cmsr_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd, bool transpose);
CMSCSparseMatrix *build_cmsc_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd, bool transpose);

double *rm_negative_row_sums(RMSparseMatrix *rmsm);
double *cm_negative_row_sums(CMSparseMatrix *cmsm);
double *cmsr_negative_row_sums(CMSRSparseMatrix *cmsrsm);
double *cmsc_negative_row_sums(CMSCSparseMatrix *cmscsm);

double *rm_negative_row_sums(RMSparseMatrix *rmsm, bool transpose);
double *cm_negative_row_sums(CMSparseMatrix *cmsm, bool transpose);
double *cmsr_negative_row_sums(CMSRSparseMatrix *cmsrsm, bool transpose);
double *cmsc_negative_row_sums(CMSCSparseMatrix *cmscsm, bool transpose);

//------------------------------------------------------------------------------

#endif // SPARSE_H
