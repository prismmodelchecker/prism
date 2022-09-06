//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Rashid Mehmood <rxm@cs.bham.uc.uk> (University of Birmingham)
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

#include <inttypes.h>
#include <util.h>
#include <cudd.h>
#include <odd.h>

//------------------------------------------------------------------------------

// hdd data structure definitions

// hybrid mtbdd node (based on structure of CUDD nodes)

typedef struct HDDNode HDDNode;

typedef struct HDDKids {
	HDDNode *e;
	HDDNode *t;
} HDDKids;

struct HDDNode
{
	union {
		double val;		// for terminals
		HDDKids kids;	// for non-terminals
	} type;
	union {				// offset info (else edge, used most often)
		int val;		// integer offset
		ODDNode* ptr;	// temporary pointer storage used during construction
	} off;
	union {				// 2nd offset info (then edge, rarely used)
		int val;		// integer offset
		ODDNode* ptr;	// temporary pointer storage used during construction
	} off2;
	union {				// sparse matrix pointer
		void *ptr;		// the pointer (void* because can be of different types)
		int val;		// temporary storage for integer used during construction
	} sm;
	HDDNode *next;
};

// hybrid mtbdd block storage (for sor/gs)
// (sparse storage - either row/col major)

typedef struct HDDBlocks HDDBlocks;

struct HDDBlocks
{
	// stats
	int n;
	int nnz;
	int max;
	// arrays for sparse storage
	HDDNode **blocks;
	unsigned int *rowscols;
	unsigned char *counts;
	int *offsets;
	// is counts array used for counts?
	// (as opposed to starts)
	bool use_counts;
	// distinct pointers info
	int dist_num;
	int dist_shift;
	int dist_mask;
	
	HDDBlocks();
	~HDDBlocks();
};

// hybrid mtbdd matrix

typedef struct HDDMatrix HDDMatrix;

struct HDDMatrix
{
	// flags
	bool row_major; 
	bool compact_b;
	bool compact_sm;
	// stats (levels)
	int num_levels;
	int l_b;
	int l_sm;
	// stats (counters)
	int num_nodes;
	int num_b;
	int num_sm;
	// stats (memory)
	double mem_nodes;
	double mem_b;
	double mem_sm;
	// node storage
	HDDNode **row_lists;
	HDDNode **col_lists;
	HDDNode ***row_tables;
	HDDNode ***col_tables;
	int *row_sizes;
	int *col_sizes;
	// pointers to special nodes
	HDDNode *top;
	HDDNode *zero;
	// odd
	ODDNode *odd;
	// block stuff
	HDDBlocks *blocks;
	// distinct values info
	double *dist;
	int dist_num;
	int dist_shift;
	int dist_mask;
	
	HDDMatrix();
	~HDDMatrix();
};

// hybrid mtbdd matrices

typedef struct HDDMatrices HDDMatrices;

struct HDDMatrices
{
	// flags (counts of)
	int compact_sm;
	// stats
	int num_levels;
	int l_sm_min;
	int l_sm_max;
	int num_nodes;
	int num_sm;
	double mem_nodes;
	double mem_sm;
	// num matrices (choices)
	int nm;
	// matrices (choices)
	HDDMatrix **choices;
	// and their bdd cubes
	DdNode **cubes;
	
	HDDMatrices();
	~HDDMatrices();
};

//------------------------------------------------------------------------------

// function prototypes

HDDMatrix *build_hdd_matrix(DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd, bool row_major);
void split_hdd_matrix(HDDMatrix *hddm, bool compact_b, bool meet);
void add_sparse_matrices(HDDMatrix *hm, bool compact_sm, bool diags_meet);

HDDMatrix *build_hdd_matrix(DdNode *matrix, DdNode **rvars, DdNode **cvars, int num_vars, ODDNode *odd, bool row_major, bool transpose);
void split_hdd_matrix(HDDMatrix *hddm, bool compact_b, bool meet, bool transpose);
void add_sparse_matrices(HDDMatrix *hm, bool compact_sm, bool diags_meet, bool transpose);

HDDMatrices *build_hdd_matrices_mdp(DdNode *mdp, HDDMatrices *existing_mdp, DdNode **rvars, DdNode **cvars, int num_vars, DdNode **ndvars, int num_ndvars, ODDNode *odd);
void add_sparse_matrices_mdp(HDDMatrices *hddms, bool compact_sm);

void rearrange_hdd_blocks(HDDMatrix *hddm, bool ooc);
double *hdd_negative_row_sums(HDDMatrix *hddm, int n);

double *hdd_negative_row_sums(HDDMatrix *hddm, int n, bool transpose);

//------------------------------------------------------------------------------
