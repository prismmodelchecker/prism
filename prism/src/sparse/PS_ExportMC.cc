//==============================================================================
//	
//	Copyright (c) 2025-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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
#include "prism.h"
#include "PrismSparseGlob.h"
#include "jnipointer.h"
#include <new>

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_sparse_PrismSparse_PS_1ExportMC
  (JNIEnv *, jclass, jlongArray, jstring, jlong, jint, jlong, jint, jlong, jint, jstring, jint);


JNIEXPORT jint JNICALL Java_sparse_PrismSparse_PS_1ExportMC
(
JNIEnv *env,
jclass cls,
jlongArray __jlongpointerArray tpa,	// actions
jobject synchs,
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
	DdNode **rvars = jlong_to_DdNode_array(rv);	// row vars
	DdNode **cvars = jlong_to_DdNode_array(cv);	// col vars
	ODDNode *odd = jlong_to_ODDNode(od);

	// sparse matrix
	RMSparseMatrix *rmsm = NULL;
	// action info
	jstring *action_names_jstrings = NULL;
	const char** action_names = NULL;
	int num_actions;
	// model stats
	int n, nnz, i, j, l, h, r, c, a;
	double d;
	const char *export_name;
	
	DdNode **trans_per_action;
	
	// exception handling around whole function
	try {
	
	// store export info
	if (!store_export_info(et, fn, env)) return -1;
	export_name = na ? env->GetStringUTFChars(na, 0) : "M";
	
	// extract matrices for each action
    int num_matrices = env->GetArrayLength(tpa);
    jlong *tpa_longs = env->GetLongArrayElements(tpa, NULL);
    trans_per_action = new DdNode*[num_matrices];
    for (a = 0; a < num_matrices; a++) {
	    trans_per_action[a] = jlong_to_DdNode(tpa_longs[a]);
    }
    env->ReleaseLongArrayElements(tpa, tpa_longs, 0);
	
	// build sparse matrix
	rmsm = build_rm_sparse_matrix_act(ddman, trans_per_action, num_matrices, rvars, cvars, num_rvars, odd);
	n = rmsm->n;
	nnz = rmsm->nnz;
	
	// also extract list of action names
	if (synchs != NULL) {
		get_string_array_from_java(env, synchs, action_names_jstrings, action_names, num_actions);
	}
	
	// print file header
	switch (export_type) {
	case EXPORT_PLAIN: export_string("%d %d\n", n, nnz); break;
	case EXPORT_MATLAB: export_string("%s = sparse(%d,%d);\n", export_name, n, n); break;
	case EXPORT_DOT: case EXPORT_DOT_STATES: export_string("digraph %s {\nnode [shape=box];\n", export_name); break;
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
	unsigned int *actions;
	double *dist;
	int dist_shift;
	int dist_mask;
	non_zeros = rmsm->non_zeros;
	row_counts = rmsm->row_counts;
	row_starts = (int *)rmsm->row_counts;
	use_counts = rmsm->use_counts;
	cols = rmsm->cols;
	actions = rmsm->actions;
	// then traverse data structure
	h = 0;
	for (i = 0; i < n; i++) {
		if (!use_counts) { l = row_starts[i]; h = row_starts[i+1]; }
		else { l = h; h += row_counts[i]; }
		if (export_type == EXPORT_ROWS) export_string("%d", i);
		for (j = l; j < h; j++) {
			r = i;
			c = cols[j];
			d = non_zeros[j];
			switch (export_type) {
			case EXPORT_PLAIN:
				export_string("%d %d %.*g", r, c, export_model_precision, d);
				if (actions != NULL && actions[j]>0) export_string(" %s", action_names[actions[j]-1]);
				export_string("\n");
				break;
			case EXPORT_MATLAB: export_string("%s(%d,%d)=%.*g;\n", export_name, r+1, c+1, export_model_precision, d); break;
			case EXPORT_DOT: case EXPORT_DOT_STATES:
				export_string("%d -> %d [ label=\"%.*g", r, c, export_model_precision, d);
				if (actions != NULL && actions[j]>0) export_string(":%s", (actions[j]>0?action_names[actions[j]-1]:""));
				export_string("\" ];\n", r, c, export_model_precision, d);
				break;
			case EXPORT_MRMC: export_string("%d %d %.*g\n", r+1, c+1, export_model_precision, d); break;
			case EXPORT_ROWS: export_string(" %.*g:%d", export_model_precision, d, c); break;
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
	if (trans_per_action) delete trans_per_action;
	if (rmsm) delete rmsm;
	//if (action_names != NULL) release_string_array_from_java(env, action_names_jstrings, action_names, num_actions);
	
	return 0;
}

//------------------------------------------------------------------------------
