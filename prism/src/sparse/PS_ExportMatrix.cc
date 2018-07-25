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

// includes
#include "PrismSparse.h"
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include "sparse.h"
#include "PrismSparseGlob.h"
#include "jnipointer.h"
#include <new>

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_sparse_PrismSparse_PS_1ExportMatrix
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer m,	// matrix
jstring na,		// matrix name
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer od,	// odd
jint et,		// export type
jstring fn		// filename
)
{
	DdNode *matrix = jlong_to_DdNode(m);		// matrix
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars
	ODDNode *odd = jlong_to_ODDNode(od);

	// flags
	bool compact_tr;
	// sparse matrix
	RMSparseMatrix *rmsm = NULL;
	CMSRSparseMatrix *cmsrsm = NULL;
	// model stats
	int n, nnz, i, j, l, h, r, c;
	double d;
	const char *export_name;
	
	// exception handling around whole function
	try {
	
	// store export info
	if (!store_export_info(et, fn, env)) return -1;
	export_name = na ? env->GetStringUTFChars(na, 0) : "M";
	
	// build sparse matrix
	// if requested, try and build a "compact" version
	compact_tr = true;
	cmsrsm = NULL;
	if (compact) cmsrsm = build_cmsr_sparse_matrix(ddman, matrix, rvars, cvars, num_rvars, odd);
	if (cmsrsm != NULL) {
		n = cmsrsm->n;
		nnz = cmsrsm->nnz;
	}
	// if not or if it wasn't possible, built a normal one
	else {
		compact_tr = false;
		rmsm = build_rm_sparse_matrix(ddman, matrix, rvars, cvars, num_rvars, odd);
		n = rmsm->n;
		nnz = rmsm->nnz;
	}
	
	// print file header
	switch (export_type) {
	case EXPORT_PLAIN: export_string("%d %d\n", n, nnz); break;
	case EXPORT_MATLAB: export_string("%s = sparse(%d,%d);\n", export_name, n, n); break;
	case EXPORT_DOT: case EXPORT_DOT_STATES: export_string("digraph %s {\nsize=\"8,5\"\nnode [shape=box];\n", export_name); break;
	case EXPORT_MRMC: export_string("STATES %d\nTRANSITIONS %d\n", n, nnz); break;
	case EXPORT_ROWS: export_string("%d %d\n", n, nnz); break;
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
		if (export_type == EXPORT_ROWS) export_string("%d", i);
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
			switch (export_type) {
			case EXPORT_PLAIN: export_string("%d %d %.12g\n", r, c, d); break;
			case EXPORT_MATLAB: export_string("%s(%d,%d)=%.12g;\n", export_name, r+1, c+1, d); break;
			case EXPORT_DOT: case EXPORT_DOT_STATES: export_string("%d -> %d [ label=\"%.12g\" ];\n", r, c, d); break;
			case EXPORT_MRMC: export_string("%d %d %.12g\n", r+1, c+1, d); break;
			case EXPORT_ROWS: export_string(" %.12g:%d", d, c); break;
			}
		}
		if (export_type == EXPORT_ROWS) export_string("\n");
	}
	
	// print file footer
	switch (export_type) {
	// Note: no footer for EXPORT_DOT_STATES
	case EXPORT_DOT: export_string("}\n"); break;
	}
	
	// close file, etc.
	if (export_file) fclose(export_file);
	if (na) env->ReleaseStringUTFChars(na, export_name);
	
	// catch exceptions: return (undocumented) error code for memout
	} catch (std::bad_alloc e) {
		return -2;
	}
	
	// free memory
	if (rmsm) delete rmsm;
	if (cmsrsm) delete cmsrsm;
	
	return 0;
}

//------------------------------------------------------------------------------
