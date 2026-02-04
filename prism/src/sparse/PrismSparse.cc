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

#include "PrismSparse.h"
#include "NDSparseMatrix.h"
#include <cstdio>
#include <cstdarg>
#include <climits>
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include "sparse.h"
#include "PrismNativeGlob.h"
#include "jnipointer.h"

//------------------------------------------------------------------------------
// Sparse matrix
//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_sparse_NDSparseMatrix_PS_1NDGetActionIndex
(JNIEnv *env, jclass cls,
 jlong __jlongpointer _ndsm, // NDSparseMatrix ptr
 jint __jlongpointer s, // state index
 jint __jlongpointer i // choice index
 )
{
    NDSparseMatrix * ndsm = (NDSparseMatrix *) jlong_to_NDSparseMatrix(_ndsm);
	bool use_counts = ndsm->use_counts;
	int *row_starts = (int *)ndsm->row_counts;
	unsigned char *row_counts = ndsm->row_counts;
	
	int l1 = 0;
	if (!use_counts) { l1 = row_starts[s]; }
	else { for (int i = 0; i < s; i++) l1 += row_counts[i]; }
	return ndsm->actions[l1];
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_NDSparseMatrix_PS_1BuildNDSparseMatrix
(JNIEnv *env, jclass cls,
 jlong __jlongpointer t,    // trans
 jlong __jlongpointer od, // odd
 jlong __jlongpointer rv, // row vars
 jint num_rvars,
 jlong __jlongpointer cv, // col vars
 jint num_cvars,
 jlong __jlongpointer ndv,  // nondet vars
 jint num_ndvars
 )
{
    NDSparseMatrix *ndsm = NULL;
    
    DdNode *trans = jlong_to_DdNode(t); //trans/reward matrix
    DdNode **rvars = jlong_to_DdNode_array(rv);   // row vars
    DdNode **cvars = jlong_to_DdNode_array(cv);   // col vars
    DdNode **ndvars = jlong_to_DdNode_array(ndv); // nondet vars
    ODDNode *odd = jlong_to_ODDNode(od);      // reachable states
    
	ndsm = build_nd_sparse_matrix(ddman, trans, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
    
    return ptr_to_jlong(ndsm);
}

//------------------------------------------------------------------------------

JNIEXPORT jlong __jlongpointer JNICALL Java_sparse_NDSparseMatrix_PS_1BuildSubNDSparseMatrix
(JNIEnv *env, jclass cls,
 jlong __jlongpointer t,    // trans
 jlong __jlongpointer od, // odd
 jlong __jlongpointer rv, // row vars
 jint num_rvars,
 jlong __jlongpointer cv, // col vars
 jint num_cvars,
 jlong __jlongpointer ndv,  // nondet vars
 jint num_ndvars,
 jlong __jlongpointer r    // reward
 )
{
    NDSparseMatrix *ndsm = NULL;
    
    DdNode *trans = jlong_to_DdNode(t); //trans/reward matrix
    DdNode **rvars = jlong_to_DdNode_array(rv);   // row vars
    DdNode **cvars = jlong_to_DdNode_array(cv);   // col vars
    DdNode **ndvars = jlong_to_DdNode_array(ndv); // nondet vars
    ODDNode *odd = jlong_to_ODDNode(od);      // reachable states
    DdNode *rewards = jlong_to_DdNode(r);
    
	ndsm = build_sub_nd_sparse_matrix(ddman, trans, rewards, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
    
    return ptr_to_jlong(ndsm);
}

//------------------------------------------------------------------------------

JNIEXPORT void JNICALL Java_sparse_NDSparseMatrix_PS_1AddActionsToNDSparseMatrix
(JNIEnv *env, jclass cls,
 jlong __jlongpointer t,    // trans
 jlong __jlongpointer ta,    // trans action labels
 jlong __jlongpointer od, // odd
 jlong __jlongpointer rv, // row vars
 jint num_rvars,
 jlong __jlongpointer cv, // col vars
 jint num_cvars,
 jlong __jlongpointer ndv,  // nondet vars
 jint num_ndvars,
 jlong __jlongpointer nd    // sparse matrix
 )
{
    DdNode *trans = jlong_to_DdNode(t); 			//trans/reward matrix
	DdNode *trans_actions = jlong_to_DdNode(ta);	// trans action labels
    ODDNode *odd = jlong_to_ODDNode(od);      // reachable states
    DdNode **rvars = jlong_to_DdNode_array(rv);   // row vars
    DdNode **cvars = jlong_to_DdNode_array(cv);   // col vars
    DdNode **ndvars = jlong_to_DdNode_array(ndv); // nondet vars
	NDSparseMatrix *ndsm = (NDSparseMatrix *) jlong_to_NDSparseMatrix(nd); // sparse matrix
    
	jstring *action_names_jstrings;
	const char** action_names = NULL;
	int num_actions;
	
	if (trans_actions != NULL) {
		build_nd_action_vector(ddman, trans, trans_actions, ndsm, rvars, cvars, num_rvars, ndvars, num_ndvars, odd);
	}
}

//------------------------------------------------------------------------------

JNIEXPORT void __jlongpointer JNICALL Java_sparse_NDSparseMatrix_PS_1DeleteNDSparseMatrix
(JNIEnv *env, jclass cls,
 jlong __jlongpointer _ndsm)
{
    NDSparseMatrix * ndsm = (NDSparseMatrix *) jlong_to_NDSparseMatrix(_ndsm);
    if (ndsm) delete ndsm;
}

//------------------------------------------------------------------------------
