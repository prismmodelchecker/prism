//==============================================================================
//	
//	File:		PS_ProbExport.cc
//	Date:		17/12/03
//	Author:		Dave Parker
//	Desc:		Export probabilistic model (DTMC) to a file
//	
//------------------------------------------------------------------------------
//	
//	Copyright (c) 2002-2004, Dave Parker
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
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include "sparse.h"
#include "PrismSparseGlob.h"

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_sparse_PrismSparse_PS_1ProbExport
(
JNIEnv *env,
jclass cls,
jint t,			// trans matrix
jint rv,		// row vars
jint num_rvars,
jint cv,		// col vars
jint num_cvars,
jint od,		// odd
jint et,		// export type
jstring fn		// filename
)
{
	DdNode *trans = (DdNode *)t;	// trans matrix
	DdNode **rvars = (DdNode **)rv; // row vars
	DdNode **cvars = (DdNode **)cv; // col vars
	ODDNode *odd = (ODDNode *)od;
	// flags
	bool compact_tr;
	// sparse matrix
	RMSparseMatrix *rmsm;
	CMSRSparseMatrix *cmsrsm;
	// model stats
	int n, nnz, i, j, l, h, r, c;
	double d;
	
	const char *filename = env->GetStringUTFChars(fn, 0);
	FILE *file = fopen(filename, "w");
	if (!file) {
		env->ReleaseStringUTFChars(fn, filename);
		return -1;
	}
	
	// build sparse matrix
	// if requested, try and build a "compact" version
	compact_tr = true;
	cmsrsm = NULL;
	if (compact) cmsrsm = build_cmsr_sparse_matrix(ddman, trans, rvars, cvars, num_rvars, odd);
	if (cmsrsm != NULL) {
		n = cmsrsm->n;
		nnz = cmsrsm->nnz;
	}
	// if not or if it wasn't possible, built a normal one
	else {
		compact_tr = false;
		rmsm = build_rm_sparse_matrix(ddman, trans, rvars, cvars, num_rvars, odd);
		n = rmsm->n;
		nnz = rmsm->nnz;
	}
	
	// print file header
	switch (et) {
	case EXPORT_PLAIN: fprintf(file, "%d %d\n", n, nnz); break;
	case EXPORT_MATLAB: fprintf(file, "P = sparse(%d,%d);\n", n, n); break;
	}
	
	// print main part of file
	// first get data structure info
	double *non_zeros;
	unsigned char *row_counts;
	int *row_starts;
	bool use_counts;
	unsigned int *cols;
	double *dist;
	int dist_shift;
	int dist_mask;
	if (!compact_tr) {
		non_zeros = rmsm->non_zeros;
		row_counts = rmsm->row_counts;
		row_starts = (int *)rmsm->row_counts;
		use_counts = rmsm->use_counts;
		cols = rmsm->cols;
	} else {
		row_counts = cmsrsm->row_counts;
		row_starts = (int *)cmsrsm->row_counts;
		use_counts = cmsrsm->use_counts;
		cols = cmsrsm->cols;
		dist = cmsrsm->dist;
		dist_shift = cmsrsm->dist_shift;
		dist_mask = cmsrsm->dist_mask;
	}
	// then traverse data structure
	h = 0;
	for (i = 0; i < n; i++) {
		if (!use_counts) { l = row_starts[i]; h = row_starts[i+1]; }
		else { l = h; h += row_counts[i]; }
		for (j = l; j < h; j++) {
			r = i;
			// "row major" version
			if (!compact_tr) {
				c = cols[j];
				d = non_zeros[j];
			}
			// "compact msr" version
			else {
				c = (int)(cols[j] >> dist_shift);
				d = dist[(int)(cols[j] & dist_mask)];
			}
			switch (et) {
			case EXPORT_PLAIN: fprintf(file, "%d %d %.12f\n", r, c, d); break;
			case EXPORT_MATLAB: fprintf(file, "P(%d,%d)=%.12f;\n", r+1, c+1, d);
			}
		}
	}
	
	fclose(file);
	env->ReleaseStringUTFChars(fn, filename);
	
	return 0;
}

//------------------------------------------------------------------------------
