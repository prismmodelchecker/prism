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

#include <dv.h>
#include "sparse.h"
#include "hybrid.h"
#include "PrismHybrid.h"
#include "PrismHybridGlob.h"
#include <math.h>

void fatal( char *s) { fprintf(stderr, "fatal...\n"); perror(s); exit(10); }

// globals (used by local functions)
static HDDMatrix *hddm;
static HDDNode *zero;
static int *starts;
static RMSparseMatrix *rmsm;
static CMSparseMatrix *cmsm;
static CMSRSparseMatrix *cmsrsm;
static CMSCSparseMatrix *cmscsm;
static int num_levels;
static bool row_major;
static bool compact_sm;
static double *sm_dist;
static int sm_dist_shift;
static int sm_dist_mask;

// local prototypes
static HDDNode *build_hdd_matrix_rowrec(DdNode *dd, DdNode **rvars, DdNode **cvars, int num_vars, int level, ODDNode *row, ODDNode *col);
static HDDNode *build_hdd_matrix_colrec(DdNode *dd, DdNode **rvars, DdNode **cvars, int num_vars, int level, ODDNode *row, ODDNode *col);
static void traverse_hdd_rec(HDDNode *hdd, int level, int stop, int r, int c, int code, bool transpose);
static void traverse_odd_rec(ODDNode *odd, int level, int stop, int index, int code);
static int compute_n_and_nnz_rec(HDDNode *hdd, int level, int num_levels, ODDNode *row, ODDNode *col, bool transpose);
static RMSparseMatrix *build_rm_sparse_matrix(HDDNode *hdd, int level, bool transpose);
static CMSparseMatrix *build_cm_sparse_matrix(HDDNode *hdd, int level, bool transpose);
static CMSRSparseMatrix *build_cmsr_sparse_matrix(HDDNode *hdd, int level, bool transpose);
static CMSCSparseMatrix *build_cmsc_sparse_matrix(HDDNode *hdd, int level, bool transpose);
static void build_mdp_cubes_rec(DdNode *dd, DdNode *cube, DdNode **rvars, DdNode **cvars, int num_vars, DdNode **ndvars, int num_ndvars, int level, ODDNode *odd, HDDMatrices *hddms);
static void hdd_negative_row_sums_rec(HDDNode *hdd, int level, int row_offset, int col_offset, double *diags, bool transpose);
static void hdd_negative_row_sums_rm(RMSparseMatrix *rmsm, int row_offset, int col_offset, double *diags, bool transpose);
static void hdd_negative_row_sums_cm(CMSparseMatrix *cmsm, int row_offset, int col_offset, double *diags, bool transpose);
static void hdd_negative_row_sums_cmsr(CMSRSparseMatrix *cmsrsm, int row_offset, int col_offset, double *diags, bool transpose);
static void hdd_negative_row_sums_cmsc(CMSCSparseMatrix *cmscsm, int row_offset, int col_offset, double *diags, bool transpose);
static void free_rmsm(RMSparseMatrix *rmsm);
static void free_cmsm(CMSparseMatrix *cmsm);
static void free_cmsrsm(CMSRSparseMatrix *cmsrsm);
static void free_cmscsm(CMSCSparseMatrix *cmscsm);

//-----------------------------------------------------------------------------------
// Methods for constructing offset-labelled MTBBDs
//-----------------------------------------------------------------------------------

// builds an offset-labelled mtbbd for a matrix (from an mtbdd)

HDDMatrix *build_hdd_matrix(DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd, bool row_major)
{ return build_hdd_matrix(matrix, rvars, cvars, num_vars, odd, row_major, false); }

HDDMatrix *build_hdd_matrix(DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd, bool row_major, bool transpose)
{
	int i, j;
	HDDMatrix *res;
	HDDNode *ptr;
	
	// create data structure and store global pointer to it
	res = new HDDMatrix();
	hddm = res;
	
	// build lists to store hdd nodes
	res->row_lists = new HDDNode*[num_vars+1];
	res->col_lists = new HDDNode*[num_vars];
	res->row_tables = new HDDNode**[num_vars+1];
	res->col_tables = new HDDNode**[num_vars];
	res->row_sizes = new int[num_vars+1];
	res->col_sizes = new int[num_vars];
	for (i = 0; i < num_vars; i++) {
		res->row_lists[i] = NULL;
		res->col_lists[i] = NULL;
		res->row_tables[i] = NULL;
		res->col_tables[i] = NULL;
		res->row_sizes[i] = 0;
		res->col_sizes[i] = 0;
	}
	// extra list for constants
	res->row_lists[num_vars] = NULL;
	res->row_tables[num_vars] = NULL;
	res->row_sizes[num_vars] = 0;
	// reset node counter
	res->num_nodes = 0;
	
	// create zero constant (special case)
	res->num_nodes++;
	res->zero = new HDDNode();
	res->zero->type.kids.e = NULL;
	res->zero->type.kids.t = NULL;
	res->zero->off.val = 0;
	res->zero->off2.val = 0;
	res->zero->sm.ptr = NULL;
	res->zero->next = NULL;
	// and store global copy of pointer
	zero = res->zero;
	
	// call recursive bit
	res->top = build_hdd_matrix_rowrec(matrix, rvars, cvars, num_vars, 0, odd, odd);
	
	// convert node storage from linked lists to arrays
	for (i = 0; i < num_vars+1; i++) {
		res->row_tables[i] = new HDDNode*[res->row_sizes[i]];
		j = 0;
		ptr = res->row_lists[i];
		while (ptr != NULL) {
			res->row_tables[i][j++] = ptr;
			ptr = ptr->next;
		}
	}
	for (i = 0; i < num_vars; i++) {
		res->col_tables[i] = new HDDNode*[res->col_sizes[i]];
		j = 0;
		ptr = res->col_lists[i];
		while (ptr != NULL) {
			res->col_tables[i][j++] = ptr;
			ptr = ptr->next;
		}
	}
	
	// go thru all nodes and
	// (1) store actual offset (int) not odd ptr
	// (2) set sparse matrix pointer to null
	for (i = 0; i < num_vars+1; i++) {
		for (j = 0; j < res->row_sizes[i]; j++) {
			res->row_tables[i][j]->off.val = res->row_tables[i][j]->off.ptr->eoff;
			res->row_tables[i][j]->sm.ptr = NULL;
		}
	}
	for (i = 0; i < num_vars; i++) {
		for (j = 0; j < res->col_sizes[i]; j++) {
			res->col_tables[i][j]->off.val = res->col_tables[i][j]->off.ptr->eoff;
			res->col_tables[i][j]->sm.ptr = NULL;
		}
	}
	
	// fill up other fields with info/defaults
	res->row_major = row_major;
	res->compact_b = true;
	res->compact_sm = true;
	res->num_levels = num_vars;
	res->l_b = 0;
	res->l_sm = 0;
	res->num_b = 0;
	res->num_sm = 0;
	res->mem_nodes = (res->num_nodes * sizeof(HDDNode)) / 1024.0;
	res->mem_b = 0;
	res->mem_sm = 0;
	res->odd = odd;
	res->blocks = NULL;
	res->dist = NULL;
	res->dist_num = 0;
	res->dist_shift = 0;
	res->dist_mask = 0;
	
	return res;
}

//-----------------------------------------------------------------------------------

// recursive part of build_hdd_matrix

HDDNode *build_hdd_matrix_rowrec(DdNode *dd, DdNode **rvars, DdNode **cvars, int num_vars, int level, ODDNode *row, ODDNode *col)
{
	HDDNode *ptr, *hdd_e, *hdd_t;
	DdNode *e, *t;
	
	// check for zero terminal
	if (dd == Cudd_ReadZero(ddman)) {
		return zero;
	}
	
	// see if we already have the required node stored
	ptr = hddm->row_lists[level];
	while (ptr != NULL) {
		if (((DdNode*)(ptr->sm.ptr) == dd) && (ptr->off.ptr == row) && (ptr->off2.ptr == col)) break;
		// use this instead to check effect on node increase
		// if (((DdNode*)(ptr->sm.ptr) == dd)) break;
		ptr = ptr->next;
	}
	// if so, return it
	if (ptr != NULL) {
		return ptr;
	}
	
	// otherwise go on and create it...
	
	// if it's a terminal node, it's easy...
	if (level == num_vars) {
		hddm->num_nodes++;
		ptr = new HDDNode();
		ptr->type.val = Cudd_V(dd);
		ptr->off.ptr = row;
		ptr->off2.ptr = col;
		ptr->sm.ptr = (void *)dd;
		ptr->next = hddm->row_lists[num_vars];
		hddm->row_lists[num_vars] = ptr;
		hddm->row_sizes[num_vars]++;
		return ptr;
	}
	
	// if not, have to recurse before creation
	if (dd->index > rvars[level]->index) {
		e = t = dd;
	}
	else {
		e = Cudd_E(dd);
		t = Cudd_T(dd);
	}
	hdd_e = build_hdd_matrix_colrec(e, rvars, cvars, num_vars, level, row->e, col);
	hdd_t = build_hdd_matrix_colrec(t, rvars, cvars, num_vars, level, row->t, col);
	hddm->num_nodes++;
	ptr = new HDDNode();
	ptr->type.kids.e = hdd_e;
	ptr->type.kids.t = hdd_t;
	ptr->off.ptr = row;
	ptr->off2.ptr = col;
	ptr->sm.ptr = (void *)dd;
	ptr->next = hddm->row_lists[level];
	hddm->row_lists[level] = ptr;
	hddm->row_sizes[level]++;
	return ptr;
}

HDDNode *build_hdd_matrix_colrec(DdNode *dd, DdNode **rvars, DdNode **cvars, int num_vars, int level, ODDNode *row, ODDNode *col)
{
	HDDNode *ptr, *hdd_e, *hdd_t;
	DdNode *e, *t;
	
	// check for zero terminal
	if (dd == Cudd_ReadZero(ddman)) {
		return zero;
	}
	
	// see if we already have the required node stored
	ptr = hddm->col_lists[level];
	while (ptr != NULL) {
		if (((DdNode*)(ptr->sm.ptr) == dd) && (ptr->off.ptr == col) && (ptr->off2.ptr == row)) break;
		// use this instead to check effect on node increase
		// if (((DdNode*)(ptr->sm.ptr) == dd)) break;
		ptr = ptr->next;
	}
	// if so, return it
	if (ptr != NULL) {
		return ptr;
	}
	
	// otherwise go on and create it...
	
	// can't be a terminal node so recurse before creation
	if (dd->index > cvars[level]->index) {
		e = t = dd;
	}
	else {
		e = Cudd_E(dd);
		t = Cudd_T(dd);
	}
	hdd_e = build_hdd_matrix_rowrec(e, rvars, cvars, num_vars, level+1, row, col->e);
	hdd_t = build_hdd_matrix_rowrec(t, rvars, cvars, num_vars, level+1, row, col->t);
	hddm->num_nodes++;
	ptr = new HDDNode();
	ptr->type.kids.e = hdd_e;
	ptr->type.kids.t = hdd_t;
	ptr->off.ptr = col;
	ptr->off2.ptr = row;
	ptr->sm.ptr = (void *)dd;
	ptr->next = hddm->col_lists[level];
	hddm->col_lists[level] = ptr;
	hddm->col_sizes[level]++;
	
	return ptr;
}

//-----------------------------------------------------------------------------------

// split offset-labelled mtbdd into blocks

void split_hdd_matrix(HDDMatrix *hm, bool compact_b, bool meet)
{ return split_hdd_matrix(hm, compact_b, meet, false); }

void split_hdd_matrix(HDDMatrix *hm, bool compact_b, bool meet, bool transpose)
{
	int i, n, max;
	HDDBlocks *blocks;
	
	// store some info globally
	hddm = hm;
	zero = hddm->zero;
	
	// initialise variables
	hddm->compact_b = compact_b;
	hddm->num_b = 0;
	hddm->mem_b = 0;
	
	// now we choose a value for l_b
	// first priority is given to the 'meet' flag: if true, l_b and l_sm must meet
	// failing that, if the user specified a value, we use that (reducing if necessary)
	// lastly, we compute the max level we can afford without exceeding mem limits
	if (meet) {
		hddm->l_b = hddm->num_levels - hddm->l_sm;
	}
	else if (num_sor_levels != -1) {
		hddm->l_b = num_sor_levels;
		if (hddm->l_b + hddm->l_sm > hddm->num_levels) hddm->l_b = hddm->num_levels - hddm->l_sm;
	}
	else {
		// no memory-based heuristic yet - just use 2/5
		hddm->l_b = hddm->num_levels*2/5;
		if (hddm->l_b + hddm->l_sm > hddm->num_levels) hddm->l_b = hddm->num_levels - hddm->l_sm;
	}
	
	// allocate storage
	blocks = new HDDBlocks();
	hddm->blocks = blocks;
	
	// if necessary, store number of distinct pointers and related info
	if (hddm->compact_b) {
		blocks->dist_num = hddm->row_sizes[hddm->l_b];
		blocks->dist_shift = (int)ceil(logtwo(blocks->dist_num));
		if (blocks->dist_shift == 0) blocks->dist_shift++;
		blocks->dist_mask = (1 << blocks->dist_shift) - 1;
	}
	
	// compute n
	blocks->n = 0;
	traverse_odd_rec(hddm->odd, 0, hddm->num_levels, 0, 1);
	n = blocks->n;
	
	// see if compact storage is feasible; if not, abandon it
	if (hddm->compact_b) {
		if (blocks->dist_shift + (int)ceil(logtwo(n)) > 8*sizeof(unsigned int)) {
			hddm->compact_b = false;
		}
	}
	
	// compute block offsets
	blocks->offsets = (int*)calloc(n+1, sizeof(int));
	if (!blocks->offsets) fatal(" error returned while allocating memory for blocks->offsets");
	hddm->mem_b += (((n+1) * sizeof(int)) / 1024.0);
	// last offset will always be num states
	blocks->offsets[n] = hddm->odd->eoff+hddm->odd->toff;
	// compute offsets recursively
	blocks->n = 0;
	traverse_odd_rec(hddm->odd, 0, hddm->num_levels, 0, 2);
	
	// compute max block size (gap b/e offsets)
	blocks->max = blocks->offsets[1] - blocks->offsets[0];
	for (i = 1; i < blocks->n; i++) {
		if( blocks->offsets[i+1] - blocks->offsets[i] > blocks->max) {
			blocks->max = blocks->offsets[i+1] - blocks->offsets[i] ;
		}
	}
	
	// allocate temporary array to store start of each row/col
	starts = (int*)calloc(n+1, sizeof(int));
	if (!starts) fatal(" error returned while allocating memory for starts");
	// see how many nonzeros are in each row/column (depending on row_major flag)
	blocks->nnz = 0;
	traverse_hdd_rec(hddm->top, 0, hddm->l_b, 0, 0, hddm->row_major?1:2, transpose);
	// and use this to compute the starts information
	// (and at same time, compute max num entries in a row/col)
	max = 0;
	for (i = 1; i < n+1; i++) {
		if (starts[i] > max) max = starts[i];
		starts[i] += starts[i-1];
	}
	// max num entries determines whether we store counts or starts:
	blocks->use_counts = (max < (unsigned int)(1 << (8*sizeof(unsigned char))));
	
	// traversal above also computed nnz so now allocate arrays
	blocks->rowscols = (unsigned int*)calloc(blocks->nnz, sizeof(unsigned int));
	if (!blocks->rowscols) fatal(" error returned while allocating memory for blocks->rowscols");
	hddm->mem_b += ((blocks->nnz * sizeof(unsigned int)) / 1024.0);
	if (!hddm->compact_b) {
		blocks->blocks = (HDDNode **)calloc(blocks->nnz, sizeof(HDDNode *));
		if (!blocks->blocks) fatal(" error returned while allocating memory for blocks->blocks");
		hddm->mem_b += ((blocks->nnz * sizeof(HDDNode *)) / 1024.0);
	}
	
	// then fill it up
	if (!hddm->compact_b) {
		traverse_hdd_rec(hddm->top, 0, hddm->l_b, 0, 0, hddm->row_major?3:4, transpose);
	} else {
		traverse_hdd_rec(hddm->top, 0, hddm->l_b, 0, 0, hddm->row_major?5:6, transpose);
	}
	
	// recompute starts info because we've messed with it during previous traversal
	for (i = n; i > 0; i--) {
		starts[i] = starts[i-1];
	}
	starts[0] = 0;
	// assuming it's safe to do so, replace starts with (smaller) array of counts
	// if not, we keep the starts array and use that
	if (blocks->use_counts) {
		blocks->counts = (unsigned char*)calloc(blocks->n, sizeof(unsigned char));
		if (!blocks->counts) fatal(" error returned while allocating memory for blocks->counts");
		hddm->mem_b += ((n * sizeof(unsigned char)) / 1024.0);
		for (i = 0; i < n; i++) {
			blocks->counts[i] = (unsigned char)(starts[i+1] - starts[i]);
		}
		free(starts);
	}
	else {
		blocks->counts = (unsigned char*)starts;
		hddm->mem_b += ((n * sizeof(int)) / 1024.0);
	}
}

//-----------------------------------------------------------------------------------

// add explicit sparse matrices on to the hdd

void add_sparse_matrices(HDDMatrix *hm, bool compact_sm, bool diags_meet)
{ return add_sparse_matrices(hm, compact_sm, diags_meet, false); }

void add_sparse_matrices(HDDMatrix *hm, bool compact_sm, bool diags_meet, bool transpose)
{
	int i, j, k, n, nnz, l, h, i_sm, rowcol;
	double mem_est;
	bool mem_out;
	HDDNode *node;
	
	// store some info globally
	hddm = hm;
	zero = hddm->zero;
	
	// initialise variables
	hddm->compact_sm = compact_sm;
	hddm->num_sm = 0;
	hddm->mem_sm = 0;
	
	// first, we initialise all sm/off2
	// (they may have been used to store other things previously)
	// (sm is set to -1 because we need to know when we visit for the first time, off2 is set to 0)
	for (i = 0; i < hddm->num_levels+1; i++) {
		for (j = 0; j < hddm->row_sizes[i]; j++) {
			hddm->row_tables[i][j]->sm.val = -1;
			hddm->row_tables[i][j]->off2.val = 0;
		}
	}
	for (i = 0; i < hddm->num_levels; i++) {
		for (j = 0; j < hddm->col_sizes[i]; j++) {
			hddm->col_tables[i][j]->sm.val = -1;
			hddm->col_tables[i][j]->off2.val = 0;
		}
	}
	
	// store size (num states and nnz) of each node's matrix
	// (putting them in the sm and off2 fields, respectively)
	compute_n_and_nnz_rec(hddm->top, 0, hddm->num_levels, hddm->odd, hddm->odd, transpose);
	
	// now we choose a value for l_sm
	// if the user specified a value, we use that (reducing if necessary)
	// if not, we compute the max level we can afford without exceeding mem limits
	if (num_sb_levels != -1) {
		hddm->l_sm = num_sb_levels;
		if (hddm->l_sm + hddm->l_b > hddm->num_levels) hddm->l_sm = hddm->num_levels - hddm->l_b;
	}
	else {
		// estimate memory required for each level
		// and select the last level for which we don't exceed the limit
		mem_out = false;
		for (i = 1; i <= hddm->num_levels - hddm->l_b; i++) {
			j = hddm->num_levels - i;
			mem_est = 0;
			for (k = 0; k < hddm->row_sizes[j]; k++) {
				n = hddm->row_tables[j][k]->sm.val;
				nnz = hddm->row_tables[j][k]->off2.val;
				if (!compact_sm) mem_est += ((nnz*(sizeof(double)+sizeof(unsigned int))+n*sizeof(unsigned char)) / 1024.0);
				else mem_est += ((nnz*(sizeof(unsigned int))+n*sizeof(unsigned char)) / 1024.0);
				if (mem_est > sb_max_mem) {
					mem_out = true;
					break;
				}
			}
			if (mem_out) break;
		}
		hddm->l_sm = mem_out ? i-1 : hddm->num_levels - hddm->l_b;
	}
	
	// compute index of level corresponding to l_sm
	i_sm = hddm->num_levels - hddm->l_sm;
	
	// if diagonal blocks will be created anyway, don't bother
	if (diags_meet) if (hddm->l_sm + hddm->l_b >= hddm->num_levels) {
		diags_meet = false;
	}
	
	// only actually add sparse matrices if there are any to add
	if (diags_meet || hddm->l_sm > 0) {
		
		// see if compact storage is feasible; if not, abandon it
		if (hddm->compact_sm) {
			// this is how many bits are free to store row/column indices
			int sparebits = 8*sizeof(unsigned int) - (int)ceil(logtwo(hddm->row_sizes[hddm->num_levels]));
			if (sparebits == 8*sizeof(unsigned int)) sparebits--;
			// so this is the max size of sparse matrix we can afford
			unsigned int maxsize = 1 << sparebits;
			// go thru all sparse matrices we are about to create and make sure they're not too big
			// in fact, for the case where diag_meet=true, this would be complicated and messy
			// hence, in this case, we resort to an over-approximation and look at all matrices on that level (l_b)
			j = diags_meet ? hddm->l_b : i_sm;
			for (i = 0; i < hddm->row_sizes[j]; i++) {
				// size is curently stored in sm pointer
				if (hddm->row_tables[j][i]->sm.val > maxsize) {
					hddm->compact_sm = false;
					break;
				}
			}
		}
		
		// if we're creating a "compact modified" sparse matrix,
		// store info about the distinct values in the matrix
		if (hddm->compact_sm) {
			hddm->dist_num = hddm->row_sizes[hddm->num_levels];
			hddm->dist_shift = (int)ceil(logtwo(hddm->dist_num));
			if (hddm->dist_shift == 0) hddm->dist_shift++;
			hddm->dist_mask = (1 << hddm->dist_shift) - 1;
			// store all the distinct values in the matrix
			hddm->dist = (double*)calloc(hddm->dist_num, sizeof(double));
			if (!hddm->dist) fatal(" error returned while allocating memory for hddm->dist ");
			hddm->mem_sm += ((hddm->dist_num * sizeof(double)) / 1024.0);
			for (j = 0; j < hddm->row_sizes[hddm->num_levels]; j++) {
				hddm->dist[j] = hddm->row_tables[hddm->num_levels][j]->type.val;
			}
		}
		
		// now actually add the sparse matrices
		
		// first the diagonal blocks (if necessary)
		if (diags_meet) {
			
			// stuff for block storage
			int b_n = hddm->blocks->n;
			HDDNode **b_blocks = hddm->blocks->blocks;
			unsigned int *b_rowscols = hddm->blocks->rowscols;
			unsigned char *b_counts = hddm->blocks->counts;
			int *b_starts = (int *)hddm->blocks->counts;
			bool b_use_counts = hddm->blocks->use_counts;
			HDDNode **b_nodes = hddm->row_tables[hddm->l_b];
			int b_dist_shift = hddm->blocks->dist_shift;
			int b_dist_mask = hddm->blocks->dist_mask;
			
			// go through each row/column of blocks
			h = 0;
			for(i = 0; i < b_n; i++) {
				if (!b_use_counts) { l = b_starts[i]; h = b_starts[i+1]; }
				else { l = h; h += b_counts[i]; }
				// go along this row/column
				for(j = l; j < h; j++) {
					// get column/row index of block
					rowcol = (!hddm->compact_b) ? b_rowscols[j] : ((unsigned int)(b_rowscols[j] >> b_dist_shift));
					// if it's the diagonal...
					if (rowcol == i) {
						// if there is no sparse matrix there already, add a sparse matrix
						// (we mark nodes we have done already by setting off2 to -1)
						node = (!hddm->compact_b) ? b_blocks[j] : (b_nodes[(int)(b_rowscols[j] & b_dist_mask)]);
						if (node->off2.val != -1) {
							if (!hddm->compact_sm) {
								if (hddm->row_major) {
									node->sm.ptr = (void *)build_rm_sparse_matrix(node, hddm->l_b, transpose);
								} else {
									node->sm.ptr = (void *)build_cm_sparse_matrix(node, hddm->l_b, transpose);
								}
							} else {
								if (hddm->row_major) {
									node->sm.ptr = (void *)build_cmsr_sparse_matrix(node, hddm->l_b, transpose);
								} else {
									node->sm.ptr = (void *)build_cmsc_sparse_matrix(node, hddm->l_b, transpose);
								}
							}
							// set off2 to -1 to indicate we have added a sparse matrix here
							node->off2.val = -1;
							// increment matrix count
							hddm->num_sm++;
						}
					}
				}
			}
		}
		
		// then all blocks at level i_sm
		if (hddm->l_sm > 0) {
			for (i = 0; i < hddm->row_sizes[i_sm]; i++) {
				node = hddm->row_tables[i_sm][i];
				if (!hddm->compact_sm) {
					if (hddm->row_major) {
						node->sm.ptr = (void *)build_rm_sparse_matrix(node, i_sm, transpose);
					} else {
						node->sm.ptr = (void *)build_cm_sparse_matrix(node, i_sm, transpose);
					}
				} else {
					if (hddm->row_major) {
						node->sm.ptr = (void *)build_cmsr_sparse_matrix(node, i_sm, transpose);
					} else {
						node->sm.ptr = (void *)build_cmsc_sparse_matrix(node, i_sm, transpose);
					}
				}
				// set off2 to -1 to indicate we have added a sparse matrix here
				node->off2.val = -1;
				// increment matrix count
				hddm->num_sm++;
			}
		}
	}
	
	// finally, set all sparse matrix pointers back to null
	// (except on the nodes where we have just put sparse matrices)
	for (i = 0; i < hddm->num_levels+1; i++) {
		for (j = 0; j < hddm->row_sizes[i]; j++) {
			if (hddm->row_tables[i][j]->off2.val != -1) hddm->row_tables[i][j]->sm.ptr = NULL;
		}
	}
	for (i = 0; i < hddm->num_levels; i++) {
		for (j = 0; j < hddm->col_sizes[i]; j++) {
			hddm->col_tables[i][j]->sm.ptr = NULL;
		}
	}
}

//-----------------------------------------------------------------------------------
// Utility methods for methods which construct offset-labelled MTBDDS
//-----------------------------------------------------------------------------------

// generic function for recursive traversal of offset-labelled MTBDD

void traverse_hdd_rec(HDDNode *hdd, int level, int stop, int r, int c, int code, bool transpose)
{
	HDDNode *e, *t;
	int i, l, h, r2, c2, dist_num, dist_shift;
	double *dist;
	
	// if it's zero, return
	if (hdd == zero) {
		return;
	}
	
	// or if we've gone down far enough
	else if (level == stop) {
		
		// for code values of 1-6, we need to determine the row and/or column index
		// of the current block. we do this here to avoid repetition/cluttering in
		// the code below. since we only know the row/column offset for the block,
		// each index is determined via a binary seacrh over the offsets array.
		// the results are stored in r2 and c2, respectively.
		if (code >=1 && code <=6) {
			// determine row index
			l=0; h=hddm->blocks->n;
			while (l<h) {
				r2 = (h-l)/2+l;
				if (hddm->blocks->offsets[r2] == r) break;
				else if (hddm->blocks->offsets[r2] < r) l = r2; else h = r2;
			}
			// determine column index
			l=0; h=hddm->blocks->n;
			while (l<h) {
				c2 = (h-l)/2+l;
				if (hddm->blocks->offsets[c2] == c) break;
				else if (hddm->blocks->offsets[c2] < c) l = c2; else h = c2;
			}
		}
		
		// do different things on the code passed in
		switch (code) {
		
		// count blocks in each row of blocks (rm/cmsr)
		case 1:
			starts[(transpose?c2:r2)+1]++;
			hddm->blocks->nnz++;
			break;
		
		// count blocks in each column of blocks (cm/cmsc)
		case 2:
			starts[(transpose?r2:c2)+1]++;
			hddm->blocks->nnz++;
			break;
		
		// store blocks (rm)
		case 3:
			hddm->blocks->blocks[starts[(transpose?c2:r2)]] = hdd;
			hddm->blocks->rowscols[starts[(transpose?c2:r2)]] = (transpose?r2:c2);
			starts[(transpose?c2:r2)]++;
			break;
		
		// store blocks (cm)
		case 4:
			hddm->blocks->blocks[starts[(transpose?r2:c2)]] = hdd;
			hddm->blocks->rowscols[starts[(transpose?r2:c2)]] = (transpose?c2:r2);
			starts[(transpose?r2:c2)]++;
			break;
		
		// store blocks (cmsr)
		case 5:
			// find block
			for (i = 0; i < hddm->row_sizes[level]; i++) {
				if (hddm->row_tables[level][i] == hdd) break;
			}
			// store block
			hddm->blocks->rowscols[starts[(transpose?c2:r2)]] = (unsigned int)(((unsigned int)(transpose?r2:c2) << (unsigned int)hddm->blocks->dist_shift) + (unsigned int)i);
			starts[(transpose?c2:r2)]++;
			break;
		
		// store blocks (cmsc)
		case 6:
			// find block
			for (i = 0; i < hddm->row_sizes[level]; i++) {
				if (hddm->row_tables[level][i] == hdd) break;
			}
			// store block
			hddm->blocks->rowscols[starts[(transpose?r2:c2)]] = (unsigned int)(((unsigned int)(transpose?c2:r2) << (unsigned int)hddm->blocks->dist_shift) + (unsigned int)i);
			starts[(transpose?r2:c2)]++;
			break;
		
		// count entries in each row (rm)
		case 7:
			starts[(transpose?c:r)+1]++;
			break;
		
		// count entries in each column (cm)
		case 8:
			starts[(transpose?r:c)+1]++;
			break;
		
		// store entries (rm)
		case 9:
			rmsm->non_zeros[starts[(transpose?c:r)]] = hdd->type.val;
			rmsm->cols[starts[(transpose?c:r)]] = (transpose?r:c);
			starts[(transpose?c:r)]++;
			break;
			
		// store entries (cm)
		case 10:
			cmsm->non_zeros[starts[(transpose?r:c)]] = hdd->type.val;
			cmsm->rows[starts[(transpose?r:c)]] = (transpose?c:r);
			starts[(transpose?r:c)]++;
			break;
			
		// store entries (cmsr)
		case 11:
			dist = hddm->dist;
			dist_num = hddm->dist_num;
			dist_shift = hddm->dist_shift;
			for (i = 0; i < dist_num; i++) if (dist[i] == hdd->type.val) break;
			cmsrsm->cols[starts[(transpose?c:r)]] = (unsigned int)(((unsigned int)(transpose?r:c) << dist_shift) + (unsigned int)i);
			starts[(transpose?c:r)]++;
			break;
			
		// store entries (cmsc)
		case 12:
			dist = hddm->dist;
			dist_num = hddm->dist_num;
			dist_shift = hddm->dist_shift;
			for (i = 0; i < dist_num; i++) if (dist[i] == hdd->type.val) break;
			cmscsm->rows[starts[(transpose?r:c)]] = (unsigned int)(((unsigned int)(transpose?c:r) << dist_shift) + (unsigned int)i);
			starts[(transpose?r:c)]++;
			break;
		}
		return;
	}
	
	// recurse - split four ways
	e = hdd->type.kids.e;
	if (e != zero) {
		traverse_hdd_rec(e->type.kids.e, level+1, stop, r, c, code, transpose);
		traverse_hdd_rec(e->type.kids.t, level+1, stop, r, c+e->off.val, code, transpose);
	}
	t = hdd->type.kids.t;
	if (t != zero) {
		traverse_hdd_rec(t->type.kids.e, level+1, stop, r+hdd->off.val, c, code, transpose);
		traverse_hdd_rec(t->type.kids.t, level+1, stop, r+hdd->off.val, c+t->off.val, code, transpose);
	}
}

//-----------------------------------------------------------------------------------

// generic function for recursive traversal of offset-labelled BDD

void traverse_odd_rec(ODDNode *odd, int level, int stop, int index, int code)
{
	// (if either we've gone down enough levels...)
	// (or there's no child node - i.e. points to zero terminal)
	if (odd->dd == Cudd_ReadZero(ddman)) {
		return;
	}
	if (level == hddm->l_b) {
		switch (code) {
		
		// compute n
		case 1:
			hddm->blocks->n++; break;
			
		// fill up offsets array
		case 2:
			hddm->blocks->offsets[hddm->blocks->n] = (int)index;
			hddm->blocks->n++;
			break;
		}
		return;
	}
	
	// recurse
	traverse_odd_rec(odd->e, level+1, stop, index, code);
	traverse_odd_rec(odd->t, level+1, stop, index+odd->eoff, code);
}

//-----------------------------------------------------------------------------------

// compute the size (num states and nnz) of matrix corresponding to each offset-labelled MTBDD node
// (and store in sm and off2 fields, respectively)

int compute_n_and_nnz_rec(HDDNode *hdd, int level, int num_levels, ODDNode *row, ODDNode *col, bool transpose)
{
	HDDNode *e, *t;
	
	// if it's the zero node... 
	if (hdd == zero) {
		hdd->sm.val = 0;
		hdd->off2.val = 0;
		return 0;
	}
	// if it's at the bottom...
	if (level == num_levels) {
		hdd->sm.val = 0;
		hdd->off2.val = 1;
		return 1;
	}
	
	// check if we've already done this node
	if (hdd->sm.val != -1) {
		return hdd->off2.val;
	}
	
	// store n (note we count rows or columns depending on transpose/row_major)
	if ((hddm->row_major && !transpose) || (!hddm->row_major && transpose)) {
		hdd->sm.val = row->eoff + row->toff;
	} else {
		hdd->sm.val = col->eoff + col->toff;
	}
	// recurse and store nnz
	hdd->off2.val = 0;
	e = hdd->type.kids.e;
	if (e != zero) {
		hdd->off2.val += compute_n_and_nnz_rec(e->type.kids.e, level+1, num_levels, row->e, col->e, transpose);
		hdd->off2.val += compute_n_and_nnz_rec(e->type.kids.t, level+1, num_levels, row->e, col->t, transpose);
	}
	t = hdd->type.kids.t;
	if (t != zero) {
		hdd->off2.val += compute_n_and_nnz_rec(t->type.kids.e, level+1, num_levels, row->t, col->e, transpose);
		hdd->off2.val += compute_n_and_nnz_rec(t->type.kids.t, level+1, num_levels, row->t, col->t, transpose);
	}
	return hdd->off2.val;
}

//-----------------------------------------------------------------------------------

// build the explicit sparse matrix for a hdd node: 4 cases, one for each type

RMSparseMatrix *build_rm_sparse_matrix(HDDNode *hdd, int level, bool transpose)
{
	int i, n, nnz, max;
	
	// create the data structure
	rmsm = new RMSparseMatrix();
	rmsm->n = n = hdd->sm.val;
	rmsm->nnz = nnz = hdd->off2.val;
	
	// allocate temporary array to store start of each row
	starts = (int*)calloc(n+1, sizeof(int));
	if (!starts) fatal(" error returned while allocating memory for starts");
	// see how many nonzeros are in each row
	traverse_hdd_rec(hdd, level, hddm->num_levels, 0, 0, 7, transpose);
	// and use this to compute the starts information
	// (and at same time, compute max num entries in a row)
	max = 0;
	for (i = 1; i < n+1; i++) {
		if (starts[i] > max) max = starts[i];
		starts[i] += starts[i-1];
	}
	// max num entries determines whether we store counts or starts:
	rmsm->use_counts = (max < (unsigned int)(1 << (8*sizeof(unsigned char))));
	
	// allocate arrays
	rmsm->non_zeros = (double*)calloc(nnz, sizeof(double));
	if (!rmsm->non_zeros) fatal(" error returned while allocating memory for sm->non_zeros ");
	hddm->mem_sm += ((nnz * sizeof(double)) / 1024.0);
	rmsm->cols = (unsigned int*)calloc(nnz, sizeof(unsigned int));
	if (!rmsm->cols) fatal(" error returned while allocating memory for sm->cols ");
	hddm->mem_sm += ((nnz * sizeof(unsigned int)) / 1024.0);
	
	// fill up arrays
	traverse_hdd_rec(hdd, level, hddm->num_levels, 0, 0, 9, transpose);
	
	// recompute starts info because we've messed with it during previous traversal
	for (i = n; i > 0; i--) {
		starts[i] = starts[i-1];
	}
	starts[0] = 0;
	// assuming it's safe to do so, replace starts with (smaller) array of counts
	// if not, we keep the starts array and use that
	if (rmsm->use_counts) {
		rmsm->row_counts = (unsigned char*)calloc(n, sizeof(unsigned char));
		if (!rmsm->row_counts) fatal(" error returned while allocating memory for sm->row_counts");
		hddm->mem_sm += ((n * sizeof(unsigned char)) / 1024.0);
		for (i = 0; i < n; i++) {
			rmsm->row_counts[i] = (unsigned char)(starts[i+1] - starts[i]);
		}
		free(starts);
	}
	else {
		rmsm->row_counts = (unsigned char*)starts;
		hddm->mem_sm += ((n * sizeof(int)) / 1024.0);
	}
	
	return rmsm;
}

CMSparseMatrix *build_cm_sparse_matrix(HDDNode *hdd, int level, bool transpose)
{
	int i, n, nnz, max;
	
	// create the data structure
	cmsm = new CMSparseMatrix();
	cmsm->n = n = hdd->sm.val;
	cmsm->nnz = nnz = hdd->off2.val;
	
	// allocate temporary array to store start of each col
	starts = (int*)calloc(n+1, sizeof(int));
	if (!starts) fatal(" error returned while allocating memory for starts");
	// see how many nonzeros are in each column
	traverse_hdd_rec(hdd, level, hddm->num_levels, 0, 0, 8, transpose);
	// and use this to compute the starts information
	// (and at same time, compute max num entries in a col)
	max = 0;
	for (i = 1; i < n+1; i++) {
		if (starts[i] > max) max = starts[i];
		starts[i] += starts[i-1];
	}
	// max num entries determines whether we store counts or starts:
	cmsm->use_counts = (max < (unsigned int)(1 << (8*sizeof(unsigned char))));
	
	// allocate arrays
	cmsm->non_zeros = (double*)calloc(nnz, sizeof(double));
	if (!cmsm->non_zeros) fatal(" error returned while allocating memory for sm->non_zeros ");
	hddm->mem_sm += ((nnz * sizeof(double)) / 1024.0);
	cmsm->rows = (unsigned int*)calloc(nnz, sizeof(unsigned int));
	if (!cmsm->rows) fatal(" error returned while allocating memory for sm->rows ");
	hddm->mem_sm += ((nnz * sizeof(unsigned int)) / 1024.0);
	
	// fill up arrays
	traverse_hdd_rec(hdd, level, hddm->num_levels, 0, 0, 10, transpose);
	
	// recompute starts info because we've messed with it during previous traversal
	for (i = n; i > 0; i--) {
		starts[i] = starts[i-1];
	}
	starts[0] = 0;
	// assuming it's safe to do so, replace starts with (smaller) array of counts
	// if not, we keep the starts array and use that
	if (cmsm->use_counts) {
		cmsm->col_counts = (unsigned char*)calloc(n, sizeof(unsigned char));
		if (!cmsm->col_counts) fatal(" error returned while allocating memory for sm->col_counts");
		hddm->mem_sm += ((n * sizeof(unsigned char)) / 1024.0);
		for (i = 0; i < n; i++) {
			cmsm->col_counts[i] = (unsigned char)(starts[i+1] - starts[i]);
		}
		free(starts);
	}
	else {
		cmsm->col_counts = (unsigned char*)starts;
		hddm->mem_sm += ((n * sizeof(int)) / 1024.0);
	}
	
	return cmsm;
}

CMSRSparseMatrix *build_cmsr_sparse_matrix(HDDNode *hdd, int level, bool transpose)
{
	int i, n, nnz, max;
	
	// create the data structure
	cmsrsm = new CMSRSparseMatrix();
	cmsrsm->n = n = hdd->sm.val;
	cmsrsm->nnz = nnz = hdd->off2.val;
	// info about distinct vals will be shared across the sparse matrices
	// so set this array to null to indicate that we don't use it
	cmsrsm->dist = NULL;
	
	// allocate temporary array to store start of each row
	starts = (int*)calloc(n+1, sizeof(int));
	if (!starts) fatal(" error returned while allocating memory for starts");
	// see how many nonzeros are in each row
	traverse_hdd_rec(hdd, level, hddm->num_levels, 0, 0, 7, transpose);
	// and use this to compute the starts information
	// (and at same time, compute max num entries in a row)
	max = 0;
	for (i = 1; i < n+1; i++) {
		if (starts[i] > max) max = starts[i];
		starts[i] += starts[i-1];
	}
	// max num entries determines whether we store counts or starts:
	cmsrsm->use_counts = (max < (unsigned int)(1 << (8*sizeof(unsigned char))));
	
	// allocate arrays
	cmsrsm->cols = (unsigned int*)calloc(nnz, sizeof(unsigned int));
	if (!cmsrsm->cols) fatal(" error returned while allocating memory for sm->cols ");
	hddm->mem_sm += ((nnz * sizeof(unsigned int)) / 1024.0);
	
	// fill up arrays
	traverse_hdd_rec(hdd, level, hddm->num_levels, 0, 0, 11, transpose);
	
	// recompute starts info because we've messed with it during previous traversal
	for (i = n; i > 0; i--) {
		starts[i] = starts[i-1];
	}
	starts[0] = 0;
	// assuming it's safe to do so, replace starts with (smaller) array of counts
	// if not, we keep the starts array and use that
	if (cmsrsm->use_counts) {
		cmsrsm->row_counts = (unsigned char*)calloc(n, sizeof(unsigned char));
		if (!cmsrsm->row_counts) fatal(" error returned while allocating memory for sm->row_counts");
		hddm->mem_sm += ((n * sizeof(unsigned char)) / 1024.0);
		for (i = 0; i < n; i++) {
			cmsrsm->row_counts[i] = (unsigned char)(starts[i+1] - starts[i]);
		}
		free(starts);
	}
	else {
		cmsrsm->row_counts = (unsigned char*)starts;
		hddm->mem_sm += ((n * sizeof(int)) / 1024.0);
	}
	
	return cmsrsm;
}

CMSCSparseMatrix *build_cmsc_sparse_matrix(HDDNode *hdd, int level, bool transpose)
{
	int i, n, nnz, max;
	
	// create the data structure
	cmscsm = new CMSCSparseMatrix();
	cmscsm->n = n = hdd->sm.val;
	cmscsm->nnz = nnz = hdd->off2.val;
	// info about distinct vals will be shared across the sparse matrices
	// so set this array to null to indicate that we don't use it
	cmscsm->dist = NULL;
	
	// allocate temporary array to store start of each col
	starts = (int*)calloc(n+1, sizeof(int));
	if (!starts) fatal(" error returned while allocating memory for starts");
	// see how many nonzeros are in each column
	traverse_hdd_rec(hdd, level, hddm->num_levels, 0, 0, 8, transpose);
	// and use this to compute the starts information
	// (and at same time, compute max num entries in a col)
	max = 0;
	for (i = 1; i < n+1; i++) {
		if (starts[i] > max) max = starts[i];
		starts[i] += starts[i-1];
	}
	// max num entries determines whether we store counts or starts:
	cmscsm->use_counts = (max < (unsigned int)(1 << (8*sizeof(unsigned char))));
	
	// allocate arrays
	cmscsm->rows = (unsigned int*)calloc(nnz, sizeof(unsigned int));
	if (!cmscsm->rows) fatal(" error returned while allocating memory for sm->rows ");
	hddm->mem_sm += ((nnz * sizeof(unsigned int)) / 1024.0);
	
	// fill up arrays
	traverse_hdd_rec(hdd, level, hddm->num_levels, 0, 0, 12, transpose);
	
	// recompute starts info because we've messed with it during previous traversal
	for (i = n; i > 0; i--) {
		starts[i] = starts[i-1];
	}
	starts[0] = 0;
	// assuming it's safe to do so, replace starts with (smaller) array of counts
	// if not, we keep the starts array and use that
	if (cmscsm->use_counts) {
		cmscsm->col_counts = (unsigned char*)calloc(n, sizeof(unsigned char));
		if (!cmscsm->col_counts) fatal(" error returned while allocating memory for sm->col_counts");
		hddm->mem_sm += ((n * sizeof(unsigned char)) / 1024.0);
		for (i = 0; i < n; i++) {
			cmscsm->col_counts[i] = (unsigned char)(starts[i+1] - starts[i]);
		}
		free(starts);
	}
	else {
		cmscsm->col_counts = (unsigned char*)starts;
		hddm->mem_sm += ((n * sizeof(int)) / 1024.0);
	}
	
	return cmscsm;
}

//-----------------------------------------------------------------------------------
// MDP variants of methods for constructing offset-labelled MTBDDs
//-----------------------------------------------------------------------------------

// builds hybrid mtbdd matrices from mtbdd (for mdp models)
// if existing_mdp is non-null, use this for nm, cubes, etc.

HDDMatrices *build_hdd_matrices_mdp(DdNode *mdp, HDDMatrices *existing_mdp, DdNode **rvars, DdNode **cvars, int num_vars, DdNode **ndvars, int num_ndvars, ODDNode *odd)
{
	int i;
	DdNode *tmp;
	HDDMatrix *hddm;
	HDDMatrices *res;
	
	// create new data structure
	res = new HDDMatrices();
	
	// get nm (number of matrices to split into)
	if (existing_mdp != NULL) {
		// just copy existing stats if appropriate
		res->nm = existing_mdp->nm;
	} else {
		// otherwise compute nm from scratch
		Cudd_Ref(mdp);
		tmp = DD_GreaterThan(ddman, mdp, 0);
		tmp = DD_ThereExists(ddman, tmp, rvars, num_vars);
		tmp = DD_ThereExists(ddman, tmp, cvars, num_vars);
		res->nm = (int)DD_GetNumMinterms(ddman, tmp, num_ndvars);
		Cudd_RecursiveDeref(ddman, tmp);
	}
	
	// allocate arrays
	res->choices = new HDDMatrix*[res->nm];
	res->cubes = new DdNode*[res->nm];
	
	// initialise other fields/stats
	res->compact_sm = false;
	res->num_levels = 0;
	res->l_sm_min = 0;
	res->l_sm_max = 0;
	res->num_nodes = 0;
	res->num_sm = 0;
	res->mem_nodes = 0;
	res->mem_sm = 0;
	
	// get cubes to extract sub-mtbdds
	if (existing_mdp != NULL) {
		// just copy existing cubes if appropriate
		for (i = 0; i < res->nm; i++) {
			Cudd_Ref(existing_mdp->cubes[i]);
			res->cubes[i] = existing_mdp->cubes[i];
		}
	} else {
		// otherwise build them
		// (first resetting nm so can use as counter)
		res->nm = 0;
		build_mdp_cubes_rec(mdp, DD_Constant(ddman, 1), rvars, cvars, num_vars, ndvars, num_ndvars, 0, odd, res);
	}
	
	// extract sub-mtbdds using cubes
	for (i = 0; i < res->nm; i++) {
		// extract each part of the mtbdd using its cube
		Cudd_Ref(mdp);
		Cudd_Ref(res->cubes[i]);
		tmp = DD_Apply(ddman, APPLY_TIMES, mdp, res->cubes[i]);
		tmp = DD_SumAbstract(ddman, tmp, ndvars, num_ndvars);
		// then build hybrid mtbdd and store it
		hddm = build_hdd_matrix(tmp, rvars, cvars, num_vars, odd, true);
		//printf("%d -> %d\n", DD_GetNumNodes(ddman, tmp), hddm->num_nodes);
		Cudd_RecursiveDeref(ddman, tmp);
		res->choices[i] = hddm;
		// update stats
		res->num_levels = hddm->num_levels;
		res->num_nodes += hddm->num_nodes;
	}
	
	// compute memory for nodes
	res->mem_nodes = (res->num_nodes * sizeof(HDDNode)) / 1024.0;
	
	return res;
}

//-----------------------------------------------------------------------------------

void build_mdp_cubes_rec(DdNode *dd, DdNode *cube, DdNode **rvars, DdNode **cvars, int num_vars, DdNode **ndvars, int num_ndvars, int level, ODDNode *odd, HDDMatrices *hddms)
{
	DdNode *e, *t, *cube_e, *cube_t;
	
	// base case - empty choice (matrix)
	if (dd == Cudd_ReadZero(ddman)) {
		Cudd_RecursiveDeref(ddman, cube);
		return;
	}
	
	// base case - nonempty choice (matrix)
	if (level == num_ndvars) {
		hddms->cubes[hddms->nm] = cube;
		hddms->nm++;
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
	Cudd_Ref(cube);
	Cudd_Ref(ndvars[level]);
	cube_e = DD_And(ddman, cube, DD_Not(ddman, ndvars[level]));
	Cudd_Ref(cube);
	Cudd_Ref(ndvars[level]);
	cube_t = DD_And(ddman, cube, ndvars[level]);
	Cudd_RecursiveDeref(ddman, cube);
	
	build_mdp_cubes_rec(e, cube_e, rvars, cvars, num_vars, ndvars, num_ndvars, level+1, odd, hddms);
	build_mdp_cubes_rec(t, cube_t, rvars, cvars, num_vars, ndvars, num_ndvars, level+1, odd, hddms);
}

//-----------------------------------------------------------------------------------

// adds sparse matrices on to several hdds (for mdps)

void add_sparse_matrices_mdp(HDDMatrices *hddms, bool compact_sm)
{
	int i;
	
	// initialise sparse matrix stats
	hddms->compact_sm = 0;
	hddms->l_sm_min = 0;
	hddms->l_sm_max = 0;
	hddms->num_sm = 0;
	hddms->mem_sm = 0;
	
	// add sparse matrices separately for each matrix
	for (i = 0; i < hddms->nm; i++) {
		// add sparse matrix
		add_sparse_matrices(hddms->choices[i], compact_sm, false);
		// update stats
		if (hddms->choices[i]->compact_sm) hddms->compact_sm++;
		if (i == 0) {
			hddms->l_sm_min = hddms->l_sm_max = hddms->choices[i]->l_sm;
		} else {
			if (hddms->choices[i]->l_sm < hddms->l_sm_min) hddms->l_sm_min = hddms->choices[i]->l_sm;
			if (hddms->choices[i]->l_sm > hddms->l_sm_max) hddms->l_sm_max = hddms->choices[i]->l_sm;
		}
		hddms->num_sm += hddms->choices[i]->num_sm;
		hddms->mem_sm += hddms->choices[i]->mem_sm;
	}
}

//-----------------------------------------------------------------------------------
// Methods which manipulate/use offset-labelled MTBBDs
//-----------------------------------------------------------------------------------

// rearrange matrix blocks:
// 1. put diagonal block at end of row/col (need like that for gauss-seidel)
// 2. if ooc flag set, put block before diag at start (helps for out-of-core)

void rearrange_hdd_blocks(HDDMatrix *hddm, bool ooc)
{
	int i, j, l, h, rowcol, iminus1;
	
	int b_n = hddm->blocks->n;
	HDDNode **b_blocks = hddm->blocks->blocks;
	unsigned int *b_rowscols = hddm->blocks->rowscols;
	unsigned char *b_counts = hddm->blocks->counts;
	int *b_starts = (int *)hddm->blocks->counts;
	bool b_use_counts = hddm->blocks->use_counts;
	int b_dist_shift = hddm->blocks->dist_shift;
		
	// go through each row/column of blocks
	h = 0;
	for(i = 0; i < b_n; i++) {
		iminus1 = (i==0)?b_n-1:i-1;
		if (!b_use_counts) { l = b_starts[i]; h = b_starts[i+1]; }
		else { l = h; h += b_counts[i]; }
		// go along this row/column
		for(j = l; j < h; j++) {
			
			// get column/row index of block
			if (!hddm->compact_b) {
				rowcol = b_rowscols[j];
			} else {
				rowcol = (unsigned int)(b_rowscols[j] >> b_dist_shift);
			}
			// if it's the diagonal, put it at the end
			if (rowcol == i) {
				unsigned int tmp = b_rowscols[j];
				b_rowscols[j] = b_rowscols[h-1];
				b_rowscols[h-1] = tmp;
				if (!hddm->compact_b) {
					HDDNode *tmp = b_blocks[j];
					b_blocks[j] = b_blocks[h-1];
					b_blocks[h-1] = tmp;
				}
			}
			// if it's the one before the diagonal and the ooc flag is set, put it at the start
			if (rowcol == iminus1) {
				unsigned int tmp = b_rowscols[j];
				b_rowscols[j] = b_rowscols[l];
				b_rowscols[l] = tmp;
				if (!hddm->compact_b) {
					HDDNode *tmp = b_blocks[j];
					b_blocks[j] = b_blocks[l];
					b_blocks[l] = tmp;
				}
			}
		}
	}
}

//-----------------------------------------------------------------------------------

// compute negative sum of elements in each row
// if transpose flag is true, matrix was previously transposed so,
// where sparse matrix based storage used (top-level block storage
// and bottom-level submatrix storage), sum for columns instead

double *hdd_negative_row_sums(HDDMatrix *hddm, int n)
{ return hdd_negative_row_sums(hddm, n, false); }

double *hdd_negative_row_sums(HDDMatrix *hddm, int n, bool transpose)
{
	int i, j, l, h;
	double *diags;
	bool compact_b = hddm->compact_b;
	compact_sm = hddm->compact_sm;
	row_major = hddm->row_major;
	if (compact_sm) {
		sm_dist = hddm->dist;
		sm_dist_shift = hddm->dist_shift;
		sm_dist_mask = hddm->dist_mask;
	}
	zero = hddm->zero;
	num_levels = hddm->num_levels;
	
	// allocate/initialise array
	diags = (double*)calloc(n, sizeof(double));
	if (!diags) return NULL;
	
	// if the matrix hasn't been split into blocks, jump straight to traversal
	if (!hddm->blocks) {
		hdd_negative_row_sums_rec(hddm->top, 0, 0, 0, diags, transpose);
		return diags;
	}
	
	// stuff for block storage
	int b_n = hddm->blocks->n;
	HDDNode **b_blocks = hddm->blocks->blocks;
	unsigned int *b_rowscols = hddm->blocks->rowscols;
	unsigned char *b_counts = hddm->blocks->counts;
	int *b_starts = (int *)hddm->blocks->counts;
	bool b_use_counts = hddm->blocks->use_counts;
	int *b_offsets = hddm->blocks->offsets;
	HDDNode **b_nodes = hddm->row_tables[hddm->l_b];
	int b_dist_shift = hddm->blocks->dist_shift;
	int b_dist_mask = hddm->blocks->dist_mask;
	int row_offset;
	int col_offset;
	HDDNode *node;
	
	// loop through rows/columns of blocks
	h = 0;
	for (i = 0; i < b_n; i++) {
		
		// loop through blocks in this row/column of blocks
		if (!b_use_counts) { l = b_starts[i]; h = b_starts[i+1]; }
		else { l = h; h += b_counts[i]; }
		for (j = l; j < h; j++) {
			
			// get node for block and its row offset
			if (!compact_b) {
				node = b_blocks[j];
				row_offset = b_offsets[b_rowscols[j]];
			} else {
				node = b_nodes[(int)(b_rowscols[j] & b_dist_mask)];
				row_offset = b_offsets[(int)(b_rowscols[j] >> b_dist_shift)];
			}
			
			// recursively traverse block
			hdd_negative_row_sums_rec(node, hddm->l_b, row_offset, col_offset, diags, transpose);
		}
	}
	
	return diags;
}

void hdd_negative_row_sums_rec(HDDNode *hdd, int level, int row_offset, int col_offset, double *diags, bool transpose)
{
	HDDNode *e, *t;
	
	// if it's the zero node
	if (hdd == zero) {
		return;
	}
	// or if we've reached a submatrix
	// (check for non-null ptr but, equivalently, we could just check if level==l_sm)
	else if (hdd->sm.ptr) {
		if (row_major) {
			if (!compact_sm) {
				hdd_negative_row_sums_rm((RMSparseMatrix *)hdd->sm.ptr, (transpose?col_offset:row_offset), (transpose?row_offset:col_offset), diags, transpose);
			} else {
				hdd_negative_row_sums_cmsr((CMSRSparseMatrix *)hdd->sm.ptr, (transpose?col_offset:row_offset), (transpose?row_offset:col_offset), diags, transpose);
			}
		} else {
			if (!compact_sm) {
				hdd_negative_row_sums_cm((CMSparseMatrix *)hdd->sm.ptr, (transpose?col_offset:row_offset), (transpose?row_offset:col_offset), diags, transpose);
			} else {
				hdd_negative_row_sums_cmsc((CMSCSparseMatrix *)hdd->sm.ptr, (transpose?col_offset:row_offset), (transpose?row_offset:col_offset), diags, transpose);
			}
		}
		return;
	}
	// or if we've reached the bottom
	else if (level == num_levels) {
		//printf("(%d,%d)=%f\n", row_offset, col_offset, hdd->type.val);
		diags[row_offset] -= hdd->type.val;
		return;
	}
	// otherwise recurse
	e = hdd->type.kids.e;
	if (e != zero) {
		hdd_negative_row_sums_rec(e->type.kids.e, level+1, row_offset, col_offset, diags, transpose);
		hdd_negative_row_sums_rec(e->type.kids.t, level+1, row_offset, col_offset+e->off.val, diags, transpose);
	}
	t = hdd->type.kids.t;
	if (t != zero) {
		hdd_negative_row_sums_rec(t->type.kids.e, level+1, row_offset+hdd->off.val, col_offset, diags, transpose);
		hdd_negative_row_sums_rec(t->type.kids.t, level+1, row_offset+hdd->off.val, col_offset+t->off.val, diags, transpose);
	}
}

void hdd_negative_row_sums_rm(RMSparseMatrix *rmsm, int row_offset, int col_offset, double *diags, bool transpose)
{
	int i2, j2, l2, h2;
	int sm_n = rmsm->n;
	int sm_nnz = rmsm->nnz;
	double *sm_non_zeros = rmsm->non_zeros;
	unsigned char *sm_row_counts = rmsm->row_counts;
	int *sm_row_starts = (int *)rmsm->row_counts;
	bool sm_use_counts = rmsm->use_counts;
	unsigned int *sm_cols = rmsm->cols;
	
	// loop through rows of submatrix
	l2 = sm_nnz; h2 = 0;
	for (i2 = 0; i2 < sm_n; i2++) {
		
		// loop through entries in this row
		if (!sm_use_counts) { l2 = sm_row_starts[i2]; h2 = sm_row_starts[i2+1]; }
		else { l2 = h2; h2 += sm_row_counts[i2]; }
		for (j2 = l2; j2 < h2; j2++) {
			diags[(transpose?col_offset+sm_cols[j2]:row_offset + i2)] -= sm_non_zeros[j2];
		}
	}
}

void hdd_negative_row_sums_cm(CMSparseMatrix *cmsm, int row_offset, int col_offset, double *diags, bool transpose)
{
	int i2, j2, l2, h2;
	int sm_n = cmsm->n;
	int sm_nnz = cmsm->nnz;
	double *sm_non_zeros = cmsm->non_zeros;
	unsigned char *sm_col_counts = cmsm->col_counts;
	int *sm_col_starts = (int *)cmsm->col_counts;
	bool sm_use_counts = cmsm->use_counts;
	unsigned int *sm_rows = cmsm->rows;
	
	// loop through columns of submatrix
	l2 = sm_nnz; h2 = 0;
	for (i2 = 0; i2 < sm_n; i2++) {
		
		// loop through entries in this column
		if (!sm_use_counts) { l2 = sm_col_starts[i2]; h2 = sm_col_starts[i2+1]; }
		else { l2 = h2; h2 += sm_col_counts[i2]; }
		for (j2 = l2; j2 < h2; j2++) {
			diags[(transpose?col_offset + i2:row_offset + sm_rows[j2])] -= sm_non_zeros[j2];
		}
	}
}

void hdd_negative_row_sums_cmsr(CMSRSparseMatrix *cmsrsm, int row_offset, int col_offset, double *diags, bool transpose)
{
	int i2, j2, l2, h2;
	int sm_n = cmsrsm->n;
	int sm_nnz = cmsrsm->nnz;
	unsigned char *sm_row_counts = cmsrsm->row_counts;
	int *sm_row_starts = (int *)cmsrsm->row_counts;
	bool sm_use_counts = cmsrsm->use_counts;
	unsigned int *sm_cols = cmsrsm->cols;
	
	// loop through rows of submatrix
	l2 = sm_nnz; h2 = 0;
	for (i2 = 0; i2 < sm_n; i2++) {
		
		// loop through entries in this row
		if (!sm_use_counts) { l2 = sm_row_starts[i2]; h2 = sm_row_starts[i2+1]; }
		else { l2 = h2; h2 += sm_row_counts[i2]; }
		for (j2 = l2; j2 < h2; j2++) {
			diags[(transpose?col_offset + ((int)(sm_cols[j2] >> sm_dist_shift)):row_offset + i2)] -= sm_dist[(int)(sm_cols[j2] & sm_dist_mask)];
		}
	}
}

void hdd_negative_row_sums_cmsc(CMSCSparseMatrix *cmscsm, int row_offset, int col_offset, double *diags, bool transpose)
{
	int i2, j2, l2, h2;
	int sm_n = cmscsm->n;
	int sm_nnz = cmscsm->nnz;
	unsigned char *sm_col_counts = cmscsm->col_counts;
	int *sm_col_starts = (int *)cmscsm->col_counts;
	bool sm_use_counts = cmscsm->use_counts;
	unsigned int *sm_rows = cmscsm->rows;
	
	// loop through columns of submatrix
	l2 = sm_nnz; h2 = 0;
	for (i2 = 0; i2 < sm_n; i2++) {
		
		// loop through entries in this column
		if (!sm_use_counts) { l2 = sm_col_starts[i2]; h2 = sm_col_starts[i2+1]; }
		else { l2 = h2; h2 += sm_col_counts[i2]; }
		for (j2 = l2; j2 < h2; j2++) {
			diags[(transpose?col_offset + i2:row_offset + ((int)(sm_rows[j2] >> sm_dist_shift)))] -= sm_dist[(int)(sm_rows[j2] & sm_dist_mask)];
		}
	}
}

//-----------------------------------------------------------------------------------
// Methods to free memory
//-----------------------------------------------------------------------------------

void free_hdd_matrix(HDDMatrix *hddm)
{
	int i, j;
	
	// free all row nodes
	for (i = 0; i < hddm->num_levels; i++) {
		for (j = 0; j < hddm->row_sizes[i]; j++) {
			// free sparse matrix if there is one
			if (hddm->row_tables[i][j]->sm.ptr) {
				if (hddm->row_major) {
					if (!hddm->compact_sm) free_rmsm((RMSparseMatrix *)hddm->row_tables[i][j]->sm.ptr);
					else free_cmsrsm((CMSRSparseMatrix *)hddm->row_tables[i][j]->sm.ptr);
				} else {
					if (!hddm->compact_sm) free_cmsm((CMSparseMatrix *)hddm->row_tables[i][j]->sm.ptr);
					else free_cmscsm((CMSCSparseMatrix *)hddm->row_tables[i][j]->sm.ptr);
				}
			}
			// free node
			delete hddm->row_tables[i][j];
		}
		// free node table
		delete hddm->row_tables[i];
	}
	// free all column nodes
	for (i = 0; i < hddm->num_levels; i++) {
		for (j = 0; j < hddm->col_sizes[i]; j++) {
			delete hddm->col_tables[i][j];
		}
		delete hddm->col_tables[i];
	}
	// free all terminal nodes
	i = hddm->num_levels;
	for (j = 0; j < hddm->row_sizes[i]; j++) {
		delete hddm->row_tables[i][j];
	}
	delete hddm->row_tables[i];
	// free other tables
	delete hddm->row_lists;
	delete hddm->col_lists;
	delete hddm->row_tables;
	delete hddm->col_tables;
	delete hddm->row_sizes;
	delete hddm->col_sizes;
	// free block storage
	if (hddm->blocks) {
		free(hddm->blocks->offsets);
		free(hddm->blocks->rowscols);
		if (!hddm->compact_b) free(hddm->blocks->blocks);
		if (hddm->blocks->use_counts) free(hddm->blocks->counts);
		else free((int*)hddm->blocks->counts);
		delete hddm->blocks;
	}
	// free distinct matrix values
	if (hddm->compact_sm) {
		free(hddm->dist);
	}
	// free whole data structure
	delete hddm;
}

void free_hdd_matrices_mdp(HDDMatrices *hddms)
{
	int i;
	
	// go thru all choices
	for (i = 0; i < hddms->nm; i++) {
		free_hdd_matrix(hddms->choices[i]);
		Cudd_RecursiveDeref(ddman, hddms->cubes[i]);
	}
	
	// free arrays
	delete hddms->choices;
	delete hddms->cubes;
	
	// free data structure
	delete hddms;
}

// nb: once we allocate memory consistently (c vs. c++),
// we can remove these funcs and use the ones in sparse.cc

void free_rmsm(RMSparseMatrix *rmsm)
{
	free(rmsm->non_zeros);
	free(rmsm->cols);
	if (rmsm->use_counts) free(rmsm->row_counts);
	else free((int*)rmsm->row_counts);
	delete rmsm;
}

void free_cmsm(CMSparseMatrix *cmsm)
{
	free(cmsm->non_zeros);
	free(cmsm->rows);
	if (cmsm->use_counts) free(cmsm->col_counts);
	else free((int*)cmsm->col_counts);
	delete cmsm;
}

void free_cmsrsm(CMSRSparseMatrix *cmsrsm)
{
	// note: don't need to free distinct val stuff since we don't use it here
	free(cmsrsm->cols);
	if (cmsrsm->use_counts) free(cmsrsm->row_counts);
	else free((int*)cmsrsm->row_counts);
	delete cmsrsm;
}

void free_cmscsm(CMSCSparseMatrix *cmscsm)
{
	// note: don't need to free distinct val stuff since we don't use it here
	free(cmscsm->rows);
	if (cmscsm->use_counts) free(cmscsm->col_counts);
	else free((int*)cmscsm->col_counts);
	delete cmscsm;
}

//-----------------------------------------------------------------------------------
