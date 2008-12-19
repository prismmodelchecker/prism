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

JNIEXPORT jint JNICALL Java_sparse_PrismSparse_PS_1ExportSubMDP
(
JNIEnv *env,
jclass cls,
jlong __jlongpointer m,	// mdp
jlong __jlongpointer sm,	// sub mdp
jstring na,		// sub mdp name
jlong __jlongpointer rv,	// row vars
jint num_rvars,
jlong __jlongpointer cv,	// col vars
jint num_cvars,
jlong __jlongpointer ndv,	// nondet vars
jint num_ndvars,
jlong __jlongpointer od,	// odd
jint et,		// export type
jstring fn		// filename
)
{
	DdNode *mdp = jlong_to_DdNode(m);		// mdp
	DdNode *submdp = jlong_to_DdNode(sm);		// sub-mdp
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars
	DdNode **ndvars = jlong_to_DdNode_array(ndv);	// nondet vars
	ODDNode *odd = jlong_to_ODDNode(od);

	// sparse matrix
	NDSparseMatrix *ndsm = NULL;
	// model stats
	int i, j, k, n, nc, l1, h1, l2, h2;
	long nnz;
	const char *export_name;
	
	// exception handling around whole function
	try {
	
	// store export info
	if (!store_export_info(et, fn, env)) return -1;
	export_name = na ? env->GetStringUTFChars(na, 0) : "S";
	
	// build sparse matrix
	ndsm = build_sub_nd_sparse_matrix(ddman, mdp, submdp, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	n = ndsm->n;
	nnz = ndsm->nnz;
	nc = ndsm->nc;
	
	// print file header
	switch (export_type) {
	case EXPORT_PLAIN: export_string("%d %d %d\n", n, nc, nnz); break;
	case EXPORT_MATLAB: for (i = 0; i < ndsm->k; i++) export_string("%s%d = sparse(%d,%d);\n", export_name, i+1, n, n); break;
	case EXPORT_ROWS: export_string("%d %d %d\n", n, nc, nnz); break;
	}
	
	// print main part of file
	// first get data structure info
	double *non_zeros = ndsm->non_zeros;
	unsigned char *row_counts = ndsm->row_counts;
	int *row_starts = (int *)ndsm->row_counts;
	unsigned char *choice_counts = ndsm->choice_counts;
	int *choice_starts = (int *)ndsm->choice_counts;
	bool use_counts = ndsm->use_counts;
	unsigned int *cols = ndsm->cols;
	// then traverse data structure
	h1 = h2 = 0;
	for (i = 0; i < n; i++) {
		if (!use_counts) { l1 = row_starts[i]; h1 = row_starts[i+1]; }
		else { l1 = h1; h1 += row_counts[i]; }
		for (j = l1; j < h1; j++) {
			if (!use_counts) { l2 = choice_starts[j]; h2 = choice_starts[j+1]; }
			else { l2 = h2; h2 += choice_counts[j]; }
			if (export_type == EXPORT_ROWS) export_string("%d", i);
			for (k = l2; k < h2; k++) {
				switch (export_type) {
				case EXPORT_PLAIN: export_string("%d %d %d %.12g\n", i, j-l1, cols[k], non_zeros[k]); break;
				case EXPORT_MATLAB: export_string("%s%d(%d,%d)=%.12g;\n", export_name, j-l1+1, i+1, cols[k]+1, non_zeros[k]); break;
				case EXPORT_ROWS: export_string(" %.12g:%d", non_zeros[k], cols[k]); break;
				}
			}
			if (export_type == EXPORT_ROWS) export_string("\n");
		}
	}
	
	// close file, etc.
	if (export_file) fclose(export_file);
	env->ReleaseStringUTFChars(na, export_name);
	
	// catch exceptions: return (undocumented) error code for memout
	} catch (std::bad_alloc e) {
		return -2;
	}
	
	// free memory
	if (ndsm) delete ndsm;
	
	return 0;
}

//------------------------------------------------------------------------------
