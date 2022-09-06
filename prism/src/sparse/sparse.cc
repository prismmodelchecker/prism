//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Rashid Mehmood <rxm@cs.bham.ac.uk> (University of Birmingham)
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

#include <cmath>
#include "dv.h"
#include "sparse.h"
#include "PrismSparseGlob.h"
#include <new>

//------------------------------------------------------------------------------

// local function prototypes

static void split_mdp_rec(DdManager *ddman, DdNode *dd, DdNode **ndvars, int num_ndvars, int level, DdNode **matrices);
static void split_mdp_and_sub_mdp_rec(DdManager *ddman, DdNode *dd, DdNode *subdd, DdNode **ndvars, int num_ndvars, int level, DdNode **matrices, DdNode **submatrices);
static void traverse_mtbdd_matr_rec(DdManager *ddman, DdNode *dd, DdNode **rvars, DdNode **cvars, int num_vars, int level, ODDNode *row, ODDNode *col, int r, int c, int code, bool transpose);
static void traverse_mtbdd_vect_rec(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, int level, ODDNode *odd, int i, int code);

// global variables (used by local functions)
static int count;
static int *starts, *starts2;
static int *actions;
static RMSparseMatrix *rmsm;
static CMSparseMatrix *cmsm;
static RCSparseMatrix *rcsm;
static CMSRSparseMatrix *cmsrsm;
static CMSCSparseMatrix *cmscsm;
static NDSparseMatrix *ndsm;

//------------------------------------------------------------------------------
// Data structure constructors/deconstructors
//------------------------------------------------------------------------------

EXPORT RMSparseMatrix::RMSparseMatrix()
{
	n = 0;
	nnz = 0;
	use_counts = false;
	mem = 0.0;
	non_zeros = NULL;
	cols = NULL;
	row_counts = NULL;
}

EXPORT RMSparseMatrix::~RMSparseMatrix()
{
	if (non_zeros) delete[] non_zeros;
	if (cols) delete[] cols;
	if (row_counts) delete[] row_counts;
}

//------------------------------------------------------------------------------

EXPORT CMSparseMatrix::CMSparseMatrix()
{
	n = 0;
	nnz = 0;
	use_counts = false;
	mem = 0.0;
	non_zeros = NULL;
	rows = NULL;
	col_counts = NULL;
}

EXPORT CMSparseMatrix::~CMSparseMatrix()
{
	if (non_zeros) delete[] non_zeros;
	if (rows) delete[] rows;
	if (col_counts) delete[] col_counts;
}

//------------------------------------------------------------------------------

EXPORT RCSparseMatrix::RCSparseMatrix()
{
	n = 0;
	nnz = 0;
	use_counts = false;
	mem = 0.0;
	non_zeros = NULL;
	rows = NULL;
	cols = NULL;
}

EXPORT RCSparseMatrix::~RCSparseMatrix()
{
	if (non_zeros) delete[] non_zeros;
	if (rows) delete[] rows;
	if (cols) delete[] cols;
}

//------------------------------------------------------------------------------

EXPORT CMSRSparseMatrix::CMSRSparseMatrix()
{
	n = 0;
	nnz = 0;
	use_counts = false;
	mem = 0.0;
	dist = NULL;
	dist_num = 0;
	dist_shift = 0;
	dist_mask = 0;
	cols = NULL;
	row_counts = NULL;
}

EXPORT CMSRSparseMatrix::~CMSRSparseMatrix()
{
	if (dist) delete[] dist;
	if (cols) delete[] cols;
	use_counts ? delete[] row_counts : delete[] (int*)row_counts;
}

//------------------------------------------------------------------------------

EXPORT CMSCSparseMatrix::CMSCSparseMatrix()
{
	n = 0;
	nnz = 0;
	use_counts = false;
	mem = 0.0;
	dist = NULL;
	dist_num = 0;
	dist_shift = 0;
	dist_mask = 0;
	rows = NULL;
	col_counts = NULL;
}

EXPORT CMSCSparseMatrix::~CMSCSparseMatrix()
{
	if (dist) delete[] dist;
	if (rows) delete[] rows;
	use_counts ? delete[] col_counts : delete[] (int*)col_counts;
}

//------------------------------------------------------------------------------

EXPORT NDSparseMatrix::NDSparseMatrix()
{
	n = 0;
	nc = 0;
	nnz = 0;
	k = 0;
	use_counts = false;
	mem = 0.0;
	non_zeros = NULL;
	cols = NULL;
	row_counts = NULL;
	choice_counts = NULL;
	actions = NULL;
}

EXPORT NDSparseMatrix::~NDSparseMatrix()
{
	if (non_zeros) delete[] non_zeros;
	if (cols) delete[] cols;
	if (row_counts) delete[] row_counts;
	if (choice_counts) delete[] choice_counts;
	if (actions) delete[] actions;
}

//------------------------------------------------------------------------------
// sparse utility functions
//------------------------------------------------------------------------------

// build row major (rm) sparse matrix
// if tranpose flag is true, actually construct for tranpose
// throws std::bad_alloc on out-of-memory

RMSparseMatrix *build_rm_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd)
{ return build_rm_sparse_matrix(ddman, matrix, rvars, cvars, num_vars, odd, false); }

RMSparseMatrix *build_rm_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd, bool transpose)
{
	int i, n, nnz, max;
	
	// try/catch for memory allocation/deallocation
	try {
	
	// create new data structure
	rmsm = NULL; rmsm = new RMSparseMatrix();
	
	// get number of states from odd
	n = rmsm->n = odd->eoff+odd->toff;
	// get num of transitions
	nnz = rmsm->nnz = (int)DD_GetNumMinterms(ddman, matrix, num_vars*2);
	
	// create arrays
	rmsm->non_zeros = new double[nnz];
	rmsm->cols = new unsigned int[nnz];
	starts = NULL; starts = new int[n+1];
	
	// first traverse the mtbdd to compute how many entries are in each row
	for (i = 0; i < n+1; i++) starts[i] = 0;
	traverse_mtbdd_matr_rec(ddman, matrix, rvars, cvars, num_vars, 0, odd, odd, 0, 0, 1, transpose);
	// and use this to compute the starts information
	// (and at same time, compute max num entries in a row)
	max = 0;
	for (i = 1; i < n+1; i++) {
		if (starts[i] > max) max = starts[i];
		starts[i] += starts[i-1];
	}
	// max num entries determines whether we store counts or starts:
	rmsm->use_counts = (max < (unsigned int)(1 << (8*sizeof(unsigned char))));
	
	// now traverse the mtbdd again to get the actual matrix entries
	traverse_mtbdd_matr_rec(ddman, matrix, rvars, cvars, num_vars, 0, odd, odd, 0, 0, 2, transpose);
	// and recompute starts info because we've messed with it during previous traversal
	for (i = n; i > 0; i--) {
		starts[i] = starts[i-1];
	}
	starts[0] = 0;
	// and if it's safe to do so, replace starts with (smaller) array of counts
	if (rmsm->use_counts) {
		rmsm->row_counts = new unsigned char[n];
		for (i = 0; i < n; i++) rmsm->row_counts[i] = (unsigned char)(starts[i+1] - starts[i]);
		delete[] starts; starts = NULL;
		rmsm->mem = (nnz * (sizeof(double) + sizeof(unsigned int)) + n * sizeof(unsigned char)) / 1024.0;
	} else {
		rmsm->row_counts = (unsigned char*)starts;
		rmsm->mem = (nnz * (sizeof(double) + sizeof(unsigned int)) + n * sizeof(int)) / 1024.0;
	}
	
	// try/catch for memory allocation/deallocation
	} catch(std::bad_alloc e) {
		if (rmsm) delete rmsm;
		if (starts) delete[] starts;
		throw e;
	}
	
	return rmsm;
}

//------------------------------------------------------------------------------

// build column major (cm) sparse matrix
// if tranpose flag is true, actually construct for tranpose
// throws std::bad_alloc on out-of-memory

CMSparseMatrix *build_cm_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd)
{ return build_cm_sparse_matrix(ddman, matrix, rvars, cvars, num_vars, odd, false); }

CMSparseMatrix *build_cm_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd, bool transpose)
{
	int i, n, nnz, max;
	
	// try/catch for memory allocation/deallocation
	try {
	
	// create new data structure
	cmsm = NULL; cmsm = new CMSparseMatrix();
	
	// get number of states from odd
	n = cmsm->n = odd->eoff+odd->toff;
	// get num of transitions
	nnz = cmsm->nnz = (int)DD_GetNumMinterms(ddman, matrix, num_vars*2);
	
	// create arrays
	cmsm->non_zeros = new double[nnz];
	cmsm->rows = new unsigned int[nnz];
	starts = NULL; starts = new int[n+1];
	
	// first traverse the mtbdd to compute how many entries are in each column
	for (i = 0; i < n+1; i++) starts[i] = 0;
	traverse_mtbdd_matr_rec(ddman, matrix, rvars, cvars, num_vars, 0, odd, odd, 0, 0, 3, transpose);
	// and use this to compute the starts information
	// (and at same time, compute max num entries in a column)
	max = 0;
	for (i = 1; i < n+1; i++) {
		if (starts[i] > max) max = starts[i];
		starts[i] += starts[i-1];
	}
	// max num entries determines whether we store counts or starts:
	cmsm->use_counts = (max < (unsigned int)(1 << (8*sizeof(unsigned char))));
	
	// now traverse the mtbdd again to get the actual matrix entries
	traverse_mtbdd_matr_rec(ddman, matrix, rvars, cvars, num_vars, 0, odd, odd, 0, 0, 4, transpose);
	// and recompute starts info because we've messed with it during previous traversal
	for (i = n; i > 0; i--) {
		starts[i] = starts[i-1];
	}
	starts[0] = 0;
	// and if it's safe to do so, replace starts with (smaller) array of counts
	if (cmsm->use_counts) {
		cmsm->col_counts = new unsigned char[n];
		for (i = 0; i < n; i++) cmsm->col_counts[i] = (unsigned char)(starts[i+1] - starts[i]);
		delete[] starts; starts = NULL;
		cmsm->mem = (nnz * (sizeof(double) + sizeof(unsigned int)) + n * sizeof(unsigned char)) / 1024.0;
	} else {
		cmsm->col_counts = (unsigned char*)starts;
		cmsm->mem = (nnz * (sizeof(double) + sizeof(unsigned int)) + n * sizeof(int)) / 1024.0;
	}
	
	// try/catch for memory allocation/deallocation
	} catch(std::bad_alloc e) {
		if (cmsm) delete cmsm;
		if (starts) delete[] starts;
		throw e;
	}
	
	return cmsm;
}

//------------------------------------------------------------------------------

// build row/column (rc) sparse matrix
// if tranpose flag is true, actually construct for tranpose
// throws std::bad_alloc on out-of-memory

RCSparseMatrix *build_rc_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd)
{ return build_rc_sparse_matrix(ddman, matrix, rvars, cvars, num_vars, odd, false); }

RCSparseMatrix *build_rc_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd, bool transpose)
{
	int n, nnz;
	
	// try/catch for memory allocation/deallocation
	try {
	
	// create new data structure
	rcsm = NULL; rcsm = new RCSparseMatrix();
	
	// get number of states from odd
	n = rcsm->n = odd->eoff+odd->toff;
	// get num of transitions
	nnz = rcsm->nnz = (int)DD_GetNumMinterms(ddman, matrix, num_vars*2);
	
	// create arrays
	rcsm->non_zeros = new double[nnz];
	rcsm->rows = new unsigned int[nnz];
	rcsm->cols = new unsigned int[nnz];
	
	// traverse the mtbdd to get the matrix entries
	count = 0;
	traverse_mtbdd_matr_rec(ddman, matrix, rvars, cvars, num_vars, 0, odd, odd, 0, 0, 5, transpose);
	
	// try/catch for memory allocation/deallocation
	} catch(std::bad_alloc e) {
		if (rcsm) delete rcsm;
		throw e;
	}
	
	return rcsm;
}

//------------------------------------------------------------------------------

// build compact modified sparse row (cmsr) sparse matrix
// if tranpose flag is true, actually construct for tranpose
// throws std::bad_alloc on out-of-memory

CMSRSparseMatrix *build_cmsr_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd)
{ return build_cmsr_sparse_matrix(ddman, matrix, rvars, cvars, num_vars, odd, false); }

CMSRSparseMatrix *build_cmsr_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd, bool transpose)
{
	int i, n, nnz, max, sparebits;
	unsigned int maxsize;
	
	// try/catch for memory allocation/deallocation
	try {
	
	// create new data structure
	cmsrsm = NULL; cmsrsm = new CMSRSparseMatrix();
	
	// get number of states from odd
	n = cmsrsm->n = odd->eoff+odd->toff;
	
	// determine number of distinct values
	// and see if compact storage is feasible; if not, abandon it
	cmsrsm->dist_num = DD_GetNumTerminals(ddman, matrix);
	cmsrsm->dist_shift = (int)ceil(logtwo(cmsrsm->dist_num));
	if (cmsrsm->dist_shift == 0) cmsrsm->dist_shift++;
	// this is how many bits are free to store row/column indices
	sparebits = 8*sizeof(unsigned int) - cmsrsm->dist_shift;
	if (sparebits == 8*sizeof(unsigned int)) sparebits--;
	// so this is the max size of sparse matrix we can afford
	maxsize = 1 << sparebits;
	if (n > maxsize) {
		delete cmsrsm;
		return NULL;
	}
	 
	// compute mask, allocate array for distinct vals
	cmsrsm->dist_mask = (1 << cmsrsm->dist_shift) - 1;
	cmsrsm->dist = new double[cmsrsm->dist_num];
	// reset total to zero because we haven't actually stored any values yet
	cmsrsm->dist_num = 0;
	
	// get num of transitions
	nnz = cmsrsm->nnz = (int)DD_GetNumMinterms(ddman, matrix, num_vars*2);
	
	// allocate temporary array to store start of each row
	starts = NULL; starts = new int[n+1];
	
	// first traverse the mtbdd to compute how many entries are in each row
	for (i = 0; i < n+1; i++) starts[i] = 0;
	traverse_mtbdd_matr_rec(ddman, matrix, rvars, cvars, num_vars, 0, odd, odd, 0, 0, 6, transpose);
	// and use this to compute the starts information
	// (and at same time, compute max num entries in a row)
	max = 0;
	for (i = 1; i < n+1; i++) {
		if (starts[i] > max) max = starts[i];
		starts[i] += starts[i-1];
	}
	// max num entries determines whether we store counts or starts:
	cmsrsm->use_counts = (max < (unsigned int)(1 << (8*sizeof(unsigned char))));
	
	// allocate other array
	cmsrsm->cols = new unsigned int[nnz];
	
	// now traverse the mtbdd again to get the actual matrix entries
	traverse_mtbdd_matr_rec(ddman, matrix, rvars, cvars, num_vars, 0, odd, odd, 0, 0, 7, transpose);
	// and recompute starts info because we've messed with it during previous traversal
	for (i = n; i > 0; i--) {
		starts[i] = starts[i-1];
	}
	starts[0] = 0;
	// and if it's safe to do so, replace starts with (smaller) array of counts
	if (cmsrsm->use_counts) {
		cmsrsm->row_counts = new unsigned char[n];
		for (i = 0; i < n; i++) cmsrsm->row_counts[i] = (unsigned char)(starts[i+1] - starts[i]);
		delete[] starts; starts = NULL;
		cmsrsm->mem = (cmsrsm->dist_num * sizeof(double) + nnz * sizeof(unsigned int) + n * sizeof(unsigned char)) / 1024.0;
	} else {
		cmsrsm->row_counts = (unsigned char*)starts;
		cmsrsm->mem = (cmsrsm->dist_num * sizeof(double) + nnz * sizeof(unsigned int) + n * sizeof(int)) / 1024.0;
	}
	
	// try/catch for memory allocation/deallocation
	} catch(std::bad_alloc e) {
		if (cmsrsm) delete cmsrsm;
		if (starts) delete[] starts;
		throw e;
	}
	
	return cmsrsm;
}

//------------------------------------------------------------------------------

// build compact modified sparse column (cmsc) sparse matrix
// if tranpose flag is true, actually construct for tranpose
// throws std::bad_alloc on out-of-memory

CMSCSparseMatrix *build_cmsc_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd)
{ return build_cmsc_sparse_matrix(ddman, matrix, rvars, cvars, num_vars, odd, false); }

CMSCSparseMatrix *build_cmsc_sparse_matrix(DdManager *ddman, DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd, bool transpose)
{
	int i, n, nnz, max, sparebits;
	unsigned int maxsize;
	
	// try/catch for memory allocation/deallocation
	try {
	
	// create new data structure
	cmscsm = NULL; cmscsm = new CMSCSparseMatrix();
	
	// get number of states from odd
	n = cmscsm->n = odd->eoff+odd->toff;
	
	// determine number of distinct values
	// and see if compact storage is feasible; if not, abandon it
	cmscsm->dist_num = DD_GetNumTerminals(ddman, matrix);
	cmscsm->dist_shift = (int)ceil(logtwo(cmscsm->dist_num));
	if (cmscsm->dist_shift == 0) cmscsm->dist_shift++;
	// this is how many bits are free to store row/column indices
	sparebits = 8*sizeof(unsigned int) - cmscsm->dist_shift;
	if (sparebits == 8*sizeof(unsigned int)) sparebits--;
	// so this is the max size of sparse matrix we can afford
	maxsize = 1 << sparebits;
	if (n > maxsize) {
		delete cmscsm;
		return NULL;
	}
	 
	// compute mask, allocate array for distinct vals
	cmscsm->dist_mask = (1 << cmscsm->dist_shift) - 1;
	cmscsm->dist = new double[cmscsm->dist_num];
	// reset total to zero because we haven't actually stored any values yet
	cmscsm->dist_num = 0;
	
	// get num of transitions
	nnz = cmscsm->nnz = (int)DD_GetNumMinterms(ddman, matrix, num_vars*2);
	
	// allocate temporary array to store start of each row
	starts = NULL; starts = new int[n+1];
	
	// first traverse the mtbdd to compute how many entries are in each column
	for (i = 0; i < n+1; i++) starts[i] = 0;
	traverse_mtbdd_matr_rec(ddman, matrix, rvars, cvars, num_vars, 0, odd, odd, 0, 0, 8, transpose);
	// and use this to compute the starts information
	// (and at same time, compute max num entries in a column)
	max = 0;
	for (i = 1; i < n+1; i++) {
		if (starts[i] > max) max = starts[i];
		starts[i] += starts[i-1];
	}
	// max num entries determines whether we store counts or starts:
	cmscsm->use_counts = (max < (unsigned int)(1 << (8*sizeof(unsigned char))));
	
	// allocate other array
	cmscsm->rows = new unsigned int[nnz];
	
	// now traverse the mtbdd again to get the actual matrix entries
	traverse_mtbdd_matr_rec(ddman, matrix, rvars, cvars, num_vars, 0, odd, odd, 0, 0, 9, transpose);
	// and recompute starts info because we've messed with it during previous traversal
	for (i = n; i > 0; i--) {
		starts[i] = starts[i-1];
	}
	starts[0] = 0;
	// and if it's safe to do so, replace starts with (smaller) array of counts
	if (cmscsm->use_counts) {
		cmscsm->col_counts = new unsigned char[n];
		for (i = 0; i < n; i++) cmscsm->col_counts[i] = (unsigned char)(starts[i+1] - starts[i]);
		delete[] starts; starts = NULL;
		cmscsm->mem = (cmscsm->dist_num * sizeof(double) + nnz * sizeof(unsigned int) + n * sizeof(unsigned char)) / 1024.0;
	} else {
		cmscsm->col_counts = (unsigned char*)starts;
		cmscsm->mem = (cmscsm->dist_num * sizeof(double) + nnz * sizeof(unsigned int) + n * sizeof(int)) / 1024.0;
	}
	
	// try/catch for memory allocation/deallocation
	} catch(std::bad_alloc e) {
		if (cmscsm) delete cmscsm;
		if (starts) delete[] starts;
		throw e;
	}
	
	return cmscsm;
}

//-----------------------------------------------------------------------------------

// build nondeterministic (mdp) sparse matrix
// throws std::bad_alloc on out-of-memory

NDSparseMatrix *build_nd_sparse_matrix(DdManager *ddman, DdNode *mdp, DdNode **rvars, DdNode **cvars, int num_vars, DdNode **ndvars, int num_ndvars, ODDNode *odd)
{
	int i, n, nm, nc, nnz, max, max2;
	DdNode *tmp = NULL, **matrices = NULL, **matrices_bdds = NULL;
	
	// try/catch for memory allocation/deallocation
	try {
	
	// create new data structure
	ndsm = NULL; ndsm = new NDSparseMatrix();
	
	// get number of states from odd
	n = ndsm->n = odd->eoff+odd->toff;
	// get num of choices (prob. distributions)
	Cudd_Ref(mdp);
	tmp = DD_ThereExists(ddman, DD_Not(ddman, DD_Equals(ddman, mdp, 0)), cvars, num_vars);
	nc = ndsm->nc = (int)DD_GetNumMinterms(ddman, tmp, num_vars+num_ndvars);
	// get num of transitions
	nnz = ndsm->nnz = (int)DD_GetNumMinterms(ddman, mdp, num_vars*2+num_ndvars);
	// break the mdp mtbdd into several (nm) mtbdds
	tmp = DD_ThereExists(ddman, tmp, rvars, num_vars);
	nm = (int)DD_GetNumMinterms(ddman, tmp, num_ndvars);
	Cudd_RecursiveDeref(ddman, tmp);
	matrices = new DdNode*[nm];
	count = 0;
	split_mdp_rec(ddman, mdp, ndvars, num_ndvars, 0, matrices);
	// and for each one create a bdd storing which rows/choices are non-empty
	matrices_bdds = new DdNode*[nm];
	for (i = 0; i < nm; i++) {
		Cudd_Ref(matrices[i]);
		matrices_bdds[i] = DD_ThereExists(ddman, DD_Not(ddman, DD_Equals(ddman, matrices[i], 0)), cvars, num_vars);
	}
	
	// create arrays
	ndsm->non_zeros = new double[nnz];
	ndsm->cols = new unsigned int[nnz];
	starts = NULL; starts = new int[n+1];
	starts2 = NULL; starts2 = new int[nc+1];
	
	// first traverse mtbdds to compute how many choices are in each row
	for (i = 0; i < n+1; i++) starts[i] = 0;
	for (i = 0; i < nm; i++) {
		traverse_mtbdd_vect_rec(ddman, matrices_bdds[i], rvars, num_vars, 0, odd, 0, 1);
	}
	// and use this to compute the starts information
	// (and at same time, compute max num choices in a state)
	max = 0;
	for (i = 1 ; i < n+1; i++) {
		if (starts[i] > max) max = starts[i];
		starts[i] += starts[i-1];
	}
	ndsm->k = max;
	
	// now traverse mtbdds to compute how many transitions in each choice
	for (i = 0; i < nc+1; i++) starts2[i] = 0;
	for (i = 0; i < nm; i++) {
		traverse_mtbdd_matr_rec(ddman, matrices[i], rvars, cvars, num_vars, 0, odd, odd, 0, 0, 10, false);
		traverse_mtbdd_vect_rec(ddman, matrices_bdds[i], rvars, num_vars, 0, odd, 0, 2);
	}
	// and use this to compute the starts2 information
	// (and at same time, compute max num transitions in a choice)
	max2 = 0;
	for (i = 1; i < nc+1; i++) {
		if (starts2[i] > max2) max2 = starts2[i];
		starts2[i] += starts2[i-1];
	}
	// recompute starts (because we altered them during last traversal)
	for (i = n; i > 0; i--) {
		starts[i] = starts[i-1];
	}
	starts[0] = 0;
	
	// max num choices/transitions determines whether we store counts or starts:
	ndsm->use_counts = (max < (unsigned int)(1 << (8*sizeof(unsigned char))));
	ndsm->use_counts &= (max2 < (unsigned int)(1 << (8*sizeof(unsigned char))));
	
	// now traverse the mtbdd again to get the actual matrix entries
	for (i = 0; i < nm; i++) {
		traverse_mtbdd_matr_rec(ddman, matrices[i], rvars, cvars, num_vars, 0, odd, odd, 0, 0, 11, false);
		traverse_mtbdd_vect_rec(ddman, matrices_bdds[i], rvars, num_vars, 0, odd, 0, 2);
	}
	// recompute starts (because we altered them during last traversal)
	for (i = n; i > 0; i--) {
		starts[i] = starts[i-1];
	}
	starts[0] = 0;
	// recompute starts2 (likewise)
	for (i = nc; i > 0; i--) {
		starts2[i] = starts2[i-1];
	}
	starts2[0] = 0;
	
	// if it's safe to do so, replace starts/starts2 with (smaller) arrays of counts
	if (ndsm->use_counts) {
		ndsm->row_counts = new unsigned char[n];
		for (i = 0; i < n; i++) ndsm->row_counts[i] = (unsigned char)(starts[i+1] - starts[i]);
		delete[] starts; starts = NULL;
		ndsm->choice_counts = new unsigned char[nc];
		for (i = 0; i < nc; i++) ndsm->choice_counts[i] = (unsigned char)(starts2[i+1] - starts2[i]);
		delete[] starts2; starts2 = NULL;
		ndsm->mem = (nnz * (sizeof(double) + sizeof(unsigned int)) + (n+nc) * sizeof(unsigned char)) / 1024.0;
	} else {
		ndsm->row_counts = (unsigned char*)starts;
		ndsm->choice_counts = (unsigned char*)starts2;
		ndsm->mem = (nnz * (sizeof(double) + sizeof(unsigned int)) + (n+nc) * sizeof(int)) / 1024.0;
	}
	
	// try/catch for memory allocation/deallocation
	} catch(std::bad_alloc e) {
		if (ndsm) delete ndsm;
		if (matrices) delete[] matrices;
		if (matrices_bdds) {
			for (i = 0; i < nm; i++) Cudd_RecursiveDeref(ddman, matrices_bdds[i]);
			delete[] matrices_bdds;
		}
		if (starts) delete[] starts;
		if (starts2) delete[] starts2;
		throw e;
	}
	
	// clear up memory
	for (i = 0; i < nm; i++) {
		Cudd_RecursiveDeref(ddman, matrices_bdds[i]);
		// nb: don't deref matrices array because that was just pointers, not new copies
	}
	delete[] matrices;
	delete[] matrices_bdds;
	
	return ndsm;
}

//-----------------------------------------------------------------------------------

// Build nondeterministic (MDP) sparse matrix for "sub-MDP".
// This function basically exists to construct a sparse matrix representing the transition rewards for an MDP.
// The complication is that we need to use the nondeterministic choice indexing of the main
// MDP matrix, not the rewards matrix, otherwise we can't tell which reward is on which transition.
// throws std::bad_alloc on out-of-memory

NDSparseMatrix *build_sub_nd_sparse_matrix(DdManager *ddman, DdNode *mdp, DdNode *submdp, DdNode **rvars, DdNode **cvars, int num_vars, DdNode **ndvars, int num_ndvars, ODDNode *odd)
{
	int i, n, nm, nc, nnz, max, max2;
	DdNode *tmp = NULL, **matrices = NULL, **submatrices = NULL, **matrices_bdds = NULL;
	
	// try/catch for memory allocation/deallocation
	try {
	
	// create new data structure
	ndsm = NULL; ndsm = new NDSparseMatrix();
	
	// get number of states from odd
	n = ndsm->n = odd->eoff+odd->toff;
	// get num of choices (prob. distributions) (USING MDP)
	Cudd_Ref(mdp);
	tmp = DD_ThereExists(ddman, DD_Not(ddman, DD_Equals(ddman, mdp, 0)), cvars, num_vars);
	nc = ndsm->nc = (int)DD_GetNumMinterms(ddman, tmp, num_vars+num_ndvars);
	// get num of transitions (USING SUB-MDP)
	nnz = ndsm->nnz = (int)DD_GetNumMinterms(ddman, submdp, num_vars*2+num_ndvars);
	// break the two mtbdds (MDP AND SUB-MDP) into several (nm) mtbdds
	tmp = DD_ThereExists(ddman, tmp, rvars, num_vars);
	nm = (int)DD_GetNumMinterms(ddman, tmp, num_ndvars);
	Cudd_RecursiveDeref(ddman, tmp);
	matrices = new DdNode*[nm];
	submatrices = new DdNode*[nm];
	count = 0;
	split_mdp_and_sub_mdp_rec(ddman, mdp, submdp, ndvars, num_ndvars, 0, matrices, submatrices);
	// and for each one create a bdd storing which rows/choices are non-empty (USING MDP)
	matrices_bdds = new DdNode*[nm];
	for (i = 0; i < nm; i++) {
		Cudd_Ref(matrices[i]);
		matrices_bdds[i] = DD_ThereExists(ddman, DD_Not(ddman, DD_Equals(ddman, matrices[i], 0)), cvars, num_vars);
	}
	
	// create arrays
	ndsm->non_zeros = new double[nnz];
	ndsm->cols = new unsigned int[nnz];
	starts = NULL; starts = new int[n+1];
	starts2 = NULL; starts2 = new int[nc+1];
	
	// first traverse mtbdds to compute how many choices are in each row (USING MDP)
	for (i = 0; i < n+1; i++) starts[i] = 0;
	for (i = 0; i < nm; i++) {
		traverse_mtbdd_vect_rec(ddman, matrices_bdds[i], rvars, num_vars, 0, odd, 0, 1);
	}
	// and use this to compute the starts information
	// (and at same time, compute max num choices in a state)
	max = 0;
	for (i = 1 ; i < n+1; i++) {
		if (starts[i] > max) max = starts[i];
		starts[i] += starts[i-1];
	}
	ndsm->k = max;
	
	// now traverse mtbdds to compute how many transitions in each choice (USING SUB-MDP)
	for (i = 0; i < nc+1; i++) starts2[i] = 0;
	for (i = 0; i < nm; i++) {
		traverse_mtbdd_matr_rec(ddman, submatrices[i], rvars, cvars, num_vars, 0, odd, odd, 0, 0, 10, false);
		traverse_mtbdd_vect_rec(ddman, matrices_bdds[i], rvars, num_vars, 0, odd, 0, 2);
	}
	// and use this to compute the starts2 information
	// (and at same time, compute max num transitions in a choice)
	max2 = 0;
	for (i = 1; i < nc+1; i++) {
		if (starts2[i] > max2) max2 = starts2[i];
		starts2[i] += starts2[i-1];
	}
	// recompute starts (because we altered them during last traversal)
	for (i = n; i > 0; i--) {
		starts[i] = starts[i-1];
	}
	starts[0] = 0;
	
	// max num choices/transitions determines whether we store counts or starts:
	ndsm->use_counts = (max < (unsigned int)(1 << (8*sizeof(unsigned char))));
	ndsm->use_counts &= (max2 < (unsigned int)(1 << (8*sizeof(unsigned char))));
	
	// now traverse the mtbdd again to get the actual matrix entries (USING SUB-MDP)
	for (i = 0; i < nm; i++) {
		traverse_mtbdd_matr_rec(ddman, submatrices[i], rvars, cvars, num_vars, 0, odd, odd, 0, 0, 11, false);
		traverse_mtbdd_vect_rec(ddman, matrices_bdds[i], rvars, num_vars, 0, odd, 0, 2);
	}
	// recompute starts (because we altered them during last traversal)
	for (i = n; i > 0; i--) {
		starts[i] = starts[i-1];
	}
	starts[0] = 0;
	// recompute starts2 (likewise)
	for (i = nc; i > 0; i--) {
		starts2[i] = starts2[i-1];
	}
	starts2[0] = 0;
	
	// if it's safe to do so, replace starts/starts2 with (smaller) arrays of counts
	if (ndsm->use_counts) {
		ndsm->row_counts = new unsigned char[n];
		for (i = 0; i < n; i++) ndsm->row_counts[i] = (unsigned char)(starts[i+1] - starts[i]);
		delete[] starts; starts = NULL;
		ndsm->choice_counts = new unsigned char[nc];
		for (i = 0; i < nc; i++) ndsm->choice_counts[i] = (unsigned char)(starts2[i+1] - starts2[i]);
		delete[] starts2; starts2 = NULL;
		ndsm->mem = (nnz * (sizeof(double) + sizeof(unsigned int)) + (n+nc) * sizeof(unsigned char)) / 1024.0;
	} else {
		ndsm->row_counts = (unsigned char*)starts;
		ndsm->choice_counts = (unsigned char*)starts2;
		ndsm->mem = (nnz * (sizeof(double) + sizeof(unsigned int)) + (n+nc) * sizeof(int)) / 1024.0;
	}
	
	// try/catch for memory allocation/deallocation
	} catch(std::bad_alloc e) {
		if (ndsm) delete ndsm;
		if (matrices) delete[] matrices;
		if (submatrices) delete[] submatrices;
		if (matrices_bdds) {
			for (i = 0; i < nm; i++) Cudd_RecursiveDeref(ddman, matrices_bdds[i]);
			delete[] matrices_bdds;
		}
		if (starts) delete[] starts;
		if (starts2) delete[] starts2;
		throw e;
	}
	
	// clear up memory
	for (i = 0; i < nm; i++) {
		Cudd_RecursiveDeref(ddman, matrices_bdds[i]);
		// nb: don't deref matrices/submatrices array because that was just pointers, not new copies
	}
	delete[] matrices;
	delete[] submatrices;
	delete[] matrices_bdds;
	
	return ndsm;
}

//------------------------------------------------------------------------------

// Build nondeterministic (mdp) action vector to accompany a sparse matrix
// (i.e. a vector containing for every state and nondet choice, an index
// into the list of all action labels).
// Store the resulting vector in the 'actions' member of the passed in NDSparseMatrix.
// throws std::bad_alloc on out-of-memory
void build_nd_action_vector(DdManager *ddman, DdNode *mdp, DdNode *trans_actions, NDSparseMatrix *mdp_ndsm, DdNode **rvars, DdNode **cvars, int num_vars, DdNode **ndvars, int num_ndvars, ODDNode *odd)
{
	int i, n, nm, nc;
	DdNode *tmp = NULL, **matrices = NULL, **submatrices = NULL, **matrices_bdds = NULL;
	
	// try/catch for memory allocation/deallocation
	try {
	
	// get stats from mdp sparse storage (num states/choices)
	n = mdp_ndsm->n;
	nc = mdp_ndsm->nc;
	// break the mtbdd storing the action info into several (nm) mtbdds
	// like for build_sub_nd_sparse_matrix above, we have to simultaneously traverse and
	// split the mtbdd for the mdp itself - this to make sure that all our indices match up
	// (more precisely, things go wrong where mtbdd storing the action info has a zero - meaning
	// there is no action label - where there is a non-zero probability in the mdp)
	Cudd_Ref(mdp);
	tmp = DD_ThereExists(ddman, DD_Not(ddman, DD_Equals(ddman, mdp, 0)), cvars, num_vars);
	tmp = DD_ThereExists(ddman, tmp, rvars, num_vars);
	nm = (int)DD_GetNumMinterms(ddman, tmp, num_ndvars);
	Cudd_RecursiveDeref(ddman, tmp);
	matrices = new DdNode*[nm];
	submatrices = new DdNode*[nm];
	count = 0;
	split_mdp_and_sub_mdp_rec(ddman, mdp, trans_actions, ndvars, num_ndvars, 0, matrices, submatrices);
	// and for each one create a bdd storing which rows are non-empty
	matrices_bdds = new DdNode*[nm];
	for (i = 0; i < nm; i++) {
		Cudd_Ref(matrices[i]);
		matrices_bdds[i] = DD_ThereExists(ddman, DD_Not(ddman, DD_Equals(ddman, matrices[i], 0)), cvars, num_vars);
	}
	
	// create arrays
	actions = NULL; actions = new int[nc];
	starts = NULL; starts = new int[n+1];
	
	// build the (temporary) array 'starts' (like was done when building the sparse matrix for the mdp).
	// in fact, this information is retrievable from the sparse matrix, but it may have
	// been converted to counts, rather than offsets, so its easier to rebuild it.
	// first traverse mtbdds to compute how many choices are in each row
	for (i = 0; i < n+1; i++) starts[i] = 0;
	for (i = 0; i < nm; i++) {
		traverse_mtbdd_vect_rec(ddman, matrices_bdds[i], rvars, num_vars, 0, odd, 0, 1);
	}
	// and use this to compute the starts information
	for (i = 1 ; i < n+1; i++) {
		starts[i] += starts[i-1];
	}
	
	// initialise the 'actions' array
	// (necessary because tau actions, with index 0, are not discovered
	//  by the call to traverse_mtbdd_vect_rec(..., 3) below)
	for (i = 0; i < nc; i++) actions[i] = 0;
	// now traverse the mtbdd to get the actual entries (action indices)
	for (i = 0; i < nm; i++) {
		traverse_mtbdd_vect_rec(ddman, submatrices[i], rvars, num_vars, 0, odd, 0, 3);
		traverse_mtbdd_vect_rec(ddman, matrices_bdds[i], rvars, num_vars, 0, odd, 0, 2);
	}
	
	// try/catch for memory allocation/deallocation
	} catch(std::bad_alloc e) {
		if (actions) delete[] actions;
		if (matrices) delete[] matrices;
		if (submatrices) delete[] submatrices;
		if (matrices_bdds) {
			for (i = 0; i < nm; i++) Cudd_RecursiveDeref(ddman, matrices_bdds[i]);
			delete[] matrices_bdds;
		}
		if (starts) delete[] starts;
		throw e;
	}
	
	// clear up memory
	for (i = 0; i < nm; i++) {
		Cudd_RecursiveDeref(ddman, matrices_bdds[i]);
		// nb: don't deref matrices/submatrices array because that was just pointers, not new copies
	}
	delete[] starts;
	delete[] matrices;
	delete[] submatrices;
	delete[] matrices_bdds;

	mdp_ndsm->actions = actions;
}

//------------------------------------------------------------------------------

void split_mdp_rec(DdManager *ddman, DdNode *dd, DdNode **ndvars, int num_ndvars, int level, DdNode **matrices)
{
	DdNode *e, *t;
	
	// base case - empty matrix
	if (dd == Cudd_ReadZero(ddman)) return;
	
	// base case - nonempty matrix
	if (level == num_ndvars) {
		matrices[count++] = dd;
		return;
	}
	
	// recurse
	if (dd->index > ndvars[level]->index) {
		e = t = dd;
	}
	else {
		e = Cudd_E(dd);
		t = Cudd_T(dd);
	}
	
	split_mdp_rec(ddman, e, ndvars, num_ndvars, level+1, matrices);
	split_mdp_rec(ddman, t, ndvars, num_ndvars, level+1, matrices);
}

void split_mdp_and_sub_mdp_rec(DdManager *ddman, DdNode *dd, DdNode *subdd, DdNode **ndvars, int num_ndvars, int level, DdNode **matrices, DdNode **submatrices)
{
	DdNode *e, *t, *e2, *t2;
	
	// base case - empty matrix
	if (dd == Cudd_ReadZero(ddman)) return;
	
	// base case - nonempty matrix
	if (level == num_ndvars) {
		matrices[count] = dd;
		submatrices[count] = subdd;
		count++;
		return;
	}
	
	// recurse
	if (dd->index > ndvars[level]->index) {
		e = t = dd;
	}
	else {
		e = Cudd_E(dd);
		t = Cudd_T(dd);
	}
	if (subdd->index > ndvars[level]->index) {
		e2 = t2 = subdd;
	}
	else {
		e2 = Cudd_E(subdd);
		t2 = Cudd_T(subdd);
	}
	
	split_mdp_and_sub_mdp_rec(ddman, e, e2, ndvars, num_ndvars, level+1, matrices, submatrices);
	split_mdp_and_sub_mdp_rec(ddman, t, t2, ndvars, num_ndvars, level+1, matrices, submatrices);
}

//------------------------------------------------------------------------------

// traverses the mtbdd and gets all the MATRIX entries out
// does different things depending on the value of 'code'
// if tranpose flag is true, actually extract from tranpose of matrix

void traverse_mtbdd_matr_rec(DdManager *ddman, DdNode *dd, DdNode **rvars, DdNode **cvars, int num_vars, int level, ODDNode *row, ODDNode *col, int r, int c, int code, bool transpose)
{
	DdNode *e, *t, *ee, *et, *te, *tt;
	int i, dist_num;
	double *dist, d;
	
	// base case - zero terminal
	if (dd == Cudd_ReadZero(ddman)) return;
	
	// base case - non zero terminal
	if (level == num_vars) {
		switch (code) {
		
		// row major - first pass
		case 1:
			starts[(transpose?c:r)+1]++;
			break;
			
		// row major - second pass
		case 2:
			rmsm->non_zeros[starts[(transpose?c:r)]] = Cudd_V(dd);
			rmsm->cols[starts[(transpose?c:r)]] = (transpose?r:c);
			starts[(transpose?c:r)]++;
			break;
			
		// column major - first pass
		case 3:
			starts[(transpose?r:c)+1]++;
			break;
			
		// column major - second pass
		case 4:
			cmsm->non_zeros[starts[(transpose?r:c)]] = Cudd_V(dd);
			cmsm->rows[starts[(transpose?r:c)]] = (transpose?c:r);
			starts[(transpose?r:c)]++;
			break;
			
		// row/column - only pass
		case 5:
			rcsm->non_zeros[count] = Cudd_V(dd);
			rcsm->rows[count] = (transpose?c:r);
			rcsm->cols[count] = (transpose?r:c);
			count++;
			break;
			
		// compact modified sparse row - first pass
		case 6:
			starts[(transpose?c:r)+1]++;
			break;
			
		// compact modified sparse row - second pass
		case 7:
			// try and find value
			dist = cmsrsm->dist;
			dist_num = cmsrsm->dist_num;
			d = Cudd_V(dd);
			for (i = 0; i < dist_num; i++) if (dist[i] == d) break;
			// if it's not there, add it
			if (i == dist_num) {
				dist[i] = d;
				cmsrsm->dist_num++;
			}
			// store info
			cmsrsm->cols[starts[(transpose?c:r)]] = (unsigned int)(((unsigned int)(transpose?r:c) << cmsrsm->dist_shift) + (unsigned int)i);
			starts[(transpose?c:r)]++;
			break;
			
		// compact modified sparse column - first pass
		case 8:
			starts[(transpose?r:c)+1]++;
			break;
			
		// compact modified sparse column - second pass
		case 9:
			// try and find value
			dist = cmscsm->dist;
			dist_num = cmscsm->dist_num;
			d = Cudd_V(dd);
			for (i = 0; i < dist_num; i++) if (dist[i] == d) break;
			// if it's not there, add it
			if (i == dist_num) {
				dist[i] = d;
				cmscsm->dist_num++;
			}
			// store info
			cmscsm->rows[starts[(transpose?r:c)]] = (unsigned int)(((unsigned int)(transpose?c:r) << cmscsm->dist_shift) + (unsigned int)i);
			starts[(transpose?r:c)]++;
			break;
			
		// mdp - first pass
		case 10:
			starts2[starts[(transpose?c:r)]+1]++;
			break;
			
		// mdp - second pass
		case 11:
			ndsm->non_zeros[starts2[starts[(transpose?c:r)]]] = Cudd_V(dd);
			ndsm->cols[starts2[starts[(transpose?c:r)]]] = (transpose?r:c);
			starts2[starts[(transpose?c:r)]]++;
			break;
		}
		return;
	}
	
	// recurse
	if (dd->index > cvars[level]->index) {
		ee = et = te = tt = dd;
	}
	else if (dd->index > rvars[level]->index) {
		ee = te = Cudd_E(dd);
		et = tt = Cudd_T(dd);
	}
	else {
		e = Cudd_E(dd);
		if (e->index > cvars[level]->index) {
			ee = et = e;
		}
		else {
			ee = Cudd_E(e);
			et = Cudd_T(e);
		}
		t = Cudd_T(dd);
		if (t->index > cvars[level]->index) {
			te = tt = t;
		}
		else {
			te = Cudd_E(t);
			tt = Cudd_T(t);
		}
	}

	traverse_mtbdd_matr_rec(ddman, ee, rvars, cvars, num_vars, level+1, row->e, col->e, r, c, code, transpose);
	traverse_mtbdd_matr_rec(ddman, et, rvars, cvars, num_vars, level+1, row->e, col->t, r, c+col->eoff, code, transpose);
	traverse_mtbdd_matr_rec(ddman, te, rvars, cvars, num_vars, level+1, row->t, col->e, r+row->eoff, c, code, transpose);
	traverse_mtbdd_matr_rec(ddman, tt, rvars, cvars, num_vars, level+1, row->t, col->t, r+row->eoff, c+col->eoff, code, transpose);
}

//------------------------------------------------------------------------------

// traverses the mtbdd and gets all the VECTOR entries out
// does different things depending on the value of 'code'

void traverse_mtbdd_vect_rec(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, int level, ODDNode *odd, int i, int code)
{
	DdNode *e, *t;
	
	// base case - zero terminal
	if (dd == Cudd_ReadZero(ddman)) return;
	
	// base case - non zero terminal
	if (level == num_vars) {
		switch (code) {
		
		// mdp - first pass
		case 1:
			starts[i+1]++;
			break;
			
		// mdp - second pass
		case 2:
			starts[i]++;
			break;
			
		// mdp action vector - single pass
		case 3:
			actions[starts[i]] = (int)Cudd_V(dd);
			break;
		}
		
		return;
	}
	
	// recurse
	if (dd->index > vars[level]->index) {
		e = t = dd;
	}
	else {
		e = Cudd_E(dd);
		t = Cudd_T(dd);
	}

	traverse_mtbdd_vect_rec(ddman, e, vars, num_vars, level+1, odd->e, i, code);
	traverse_mtbdd_vect_rec(ddman, t, vars, num_vars, level+1, odd->t, i+odd->eoff, code);
}

//-----------------------------------------------------------------------------------

// compute negative sum of elements in each row
// if transpose flag is true, matrix was previously transposed so sum for columns instead
// throws std::bad_alloc on out-of-memory

double *rm_negative_row_sums(RMSparseMatrix *rmsm) { return rm_negative_row_sums(rmsm, false); }
double *cm_negative_row_sums(CMSparseMatrix *cmsm) { return cm_negative_row_sums(cmsm, false); }
double *cmsr_negative_row_sums(CMSRSparseMatrix *cmsrsm) { return cmsr_negative_row_sums(cmsrsm, false); }
double *cmsc_negative_row_sums(CMSCSparseMatrix *cmscsm) { return cmsc_negative_row_sums(cmscsm, false); }

double *rm_negative_row_sums(RMSparseMatrix *rmsm, bool transpose)
{
	int i, j, l, h;
	double *diags = NULL;
	int n = rmsm->n;
	double *non_zeros = rmsm->non_zeros;
	unsigned char *row_counts = rmsm->row_counts;
	int *row_starts = (int *)rmsm->row_counts;
	bool use_counts = rmsm->use_counts;
	unsigned int *cols = rmsm->cols;
	
	// allocate new vector
	diags = new double[n];
	for (i = 0; i < n; i++) {
		diags[i] = 0;
	}
	// loop through rows
	h = 0;
	for (i = 0; i < n; i++) {
		// loop through entries in this row
		if (!use_counts) { l = row_starts[i]; h = row_starts[i+1]; }
		else { l = h; h += row_counts[i]; }
		for (j = l; j < h; j++) {
			diags[(transpose?cols[j]:i)] -= non_zeros[j];
		}
	}
	
	return diags;
}

double *cm_negative_row_sums(CMSparseMatrix *cmsm, bool transpose)
{
	int i, j, l, h;
	double *diags = NULL;
	int n = cmsm->n;
	double *non_zeros = cmsm->non_zeros;
	unsigned char *col_counts = cmsm->col_counts;
	int *col_starts = (int *)cmsm->col_counts;
	bool use_counts = cmsm->use_counts;
	unsigned int *rows = cmsm->rows;
	
	// allocate new vector
	diags = new double[n];
	for (i = 0; i < n; i++) {
		diags[i] = 0;
	}
	// loop through columns
	h = 0;
	for (i = 0; i < n; i++) {
		// loop through entries in this column
		if (!use_counts) { l = col_starts[i]; h = col_starts[i+1]; }
		else { l = h; h += col_counts[i]; }
		for (j = l; j < h; j++) {
			diags[(transpose?i:rows[j])] -= non_zeros[j];
		}
	}
	
	return diags;
}

double *cmsr_negative_row_sums(CMSRSparseMatrix *cmsrsm, bool transpose)
{
	int i, j, l, h;
	double *diags = NULL;
	int n = cmsrsm->n;
	unsigned char *row_counts = cmsrsm->row_counts;
	int *row_starts = (int *)cmsrsm->row_counts;
	bool use_counts = cmsrsm->use_counts;
	unsigned int *cols = cmsrsm->cols;
	double *dist = cmsrsm->dist;
	int dist_shift = cmsrsm->dist_shift;
	int dist_mask = cmsrsm->dist_mask;
	
	// allocate new vector
	diags = new double[n];
	for (i = 0; i < n; i++) {
		diags[i] = 0;
	}
	// loop through rows of submatrix
	h = 0;
	for (i = 0; i < n; i++) {
		// loop through entries in this row
		if (!use_counts) { l = row_starts[i]; h = row_starts[i+1]; }
		else { l = h; h += row_counts[i]; }
		for (j = l; j < h; j++) {
			diags[(transpose?((int)(cols[j] >> dist_shift)):i)] -= dist[(int)(cols[j] & dist_mask)];
		}
	}
	
	return diags;
}

double *cmsc_negative_row_sums(CMSCSparseMatrix *cmscsm, bool transpose)
{
	int i, j, l, h;
	double *diags = NULL;
	int n = cmscsm->n;
	unsigned char *col_counts = cmscsm->col_counts;
	int *col_starts = (int *)cmscsm->col_counts;
	bool use_counts = cmscsm->use_counts;
	unsigned int *rows = cmscsm->rows;
	double *dist = cmscsm->dist;
	int dist_shift = cmscsm->dist_shift;
	int dist_mask = cmscsm->dist_mask;
	
	// allocate new vector
	diags = new double[n];
	for (i = 0; i < n; i++) {
		diags[i] = 0;
	}
	// loop through columns of submatrix
	h = 0;
	for (i = 0; i < n; i++) {
		// loop through entries in this column
		if (!use_counts) { l = col_starts[i]; h = col_starts[i+1]; }
		else { l = h; h += col_counts[i]; }
		for (j = l; j < h; j++) {
			diags[(transpose?i:((int)(rows[j] >> dist_shift)))] -= dist[(int)(rows[j] & dist_mask)];
		}
	}
	
	return diags;
}

//------------------------------------------------------------------------------

