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
#include "PrismMTBDD.h"
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include "PrismMTBDDGlob.h"
#include "jnipointer.h"

//------------------------------------------------------------------------------

// local function prototypes
static void export_rec(DdNode *dd, DdNode **vars, int num_vars, int level, ODDNode *odd, long index);

// globals
static const char *export_name;
static int num_labels;
static const char **label_strings;
static DdNode ***dd_array;

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_mtbdd_PrismMTBDD_PM_1ExportLabels
(
JNIEnv *env,
jclass cls,
jlongArray __jlongpointer la,		// labels
jobjectArray ln,	// label names
jstring na,		// export name
jlong __jlongpointer v,	// (row) vars
jint num_vars,
jlong __jlongpointer od,	// odd
jint et,		// export type
jstring fn		// filename
)
{
	jlong *labels;
	jobject *label_names;
	DdNode **vars = jlong_to_DdNode_array(v);
	ODDNode *odd = jlong_to_ODDNode(od);
	int i;
	
	// unpack jni arrays
	num_labels = env->GetArrayLength(la);
	labels = env->GetLongArrayElements(la, 0);
	label_names = new jobject[num_labels];
	for (i = 0; i < num_labels; i++) label_names[i] = env->GetObjectArrayElement(ln, i);
	label_strings = new const char*[num_labels];
	for (i = 0; i < num_labels; i++) label_strings[i] = env->GetStringUTFChars((jstring)label_names[i], 0);
	// should be fine to cast jobject to jstring as we know type is ok
	// could check for failure with env->ExceptionCheck() if necessary
	
	// store export info
	if (!store_export_info(et, fn, env)) return -1;
	export_name = na ? env->GetStringUTFChars(na, 0) : "V";
	
	// print file header
	switch (export_type) {
	case EXPORT_PLAIN:
		for (i = 0; i < num_labels; i++)
			export_string("%s%d=\"%s\"", ((i>0)?" ":""), i, label_strings[i]);
		export_string("\n");
		break;
	case EXPORT_MATLAB:
		for (i = 0; i < num_labels; i++)
			export_string("%s_%s=sparse(%d,1);\n", export_name, label_strings[i], odd->eoff+odd->toff);
		export_string("\n");
		break;
	case EXPORT_MRMC:
		export_string("#DECLARATION\n");
		for (i = 0; i < num_labels; i++)
			export_string("%s%s", ((i>0)?" ":""), label_strings[i]);
		export_string("\n#END\n");
		break;
	}
	
	// set up global arrays used for recursive traversal of set of dds
	dd_array = new DdNode**[num_vars+1];
	for (i = 0; i < num_vars+1; i++) {
		dd_array[i] = new DdNode*[num_labels];
	}
	for (i = 0; i < num_labels; i++) {
		dd_array[0][i] = jlong_to_DdNode(labels[i]);
	}
	
	// print main part of file
	export_rec(jlong_to_DdNode(labels[0]), vars, num_vars, 0, odd, 0);
	
	// free memory
	for (i = 0; i < num_vars+1; i++) {
		delete[] dd_array[i];
	}
	delete[] dd_array;
	
	// clean up jni stuff
	for (i = 0; i < num_labels; i++) env->ReleaseStringUTFChars((jstring)label_names[i], label_strings[i]);
	delete[] label_strings;
	delete[] label_names;
	env->ReleaseLongArrayElements(la, labels, 0);
	
	// close file, etc.
	if (export_file) fclose(export_file);
	env->ReleaseStringUTFChars(na, export_name);
	
	return 0;
}

//------------------------------------------------------------------------------

static void export_rec(DdNode *dd, DdNode **vars, int num_vars, int level, ODDNode *odd, long index)
{
	DdNode *e, *t;
	int i;
	bool all_zero;
	
	// base case - all zero terminals
	all_zero = true;
	for (i = 0; i < num_labels; i++) {
		if (dd_array[level][i] != Cudd_ReadZero(ddman)) { all_zero = false; break; }
	}
	if (all_zero) return;
	
	// base case - non zero terminal
	if (level == num_vars) {
		// print state index
		switch (export_type) {
		case EXPORT_PLAIN: export_string("%d:", index); break;
		case EXPORT_MRMC: export_string("%d", index+1); break;
		}
		// print labels
		for (i = 0; i < num_labels; i++) {
			if (dd_array[level][i] != Cudd_ReadZero(ddman)) {
				switch (export_type) {
				case EXPORT_PLAIN: export_string(" %d", i); break;
				case EXPORT_MATLAB: export_string("%s_%s(%d)=1;\n", export_name, label_strings[i], index+1); break;
				case EXPORT_MRMC: export_string(" %s", label_strings[i]); break;
				}
			}
		}
		switch (export_type) {
		case EXPORT_PLAIN: export_string("\n"); break;
		case EXPORT_MRMC: export_string("\n"); break;
		}
		return;
	}
	
	// recurse - elses
	for (i = 0; i < num_labels; i++) {
		if (dd_array[level][i]->index > vars[level]->index) {
			dd_array[level+1][i] = dd_array[level][i];
		} else {
			dd_array[level+1][i] = Cudd_E(dd_array[level][i]);
		}
	}
	export_rec(e, vars, num_vars, level+1, odd->e, index);
	
	// recurse - thens
	for (i = 0; i < num_labels; i++) {
		if (dd_array[level][i]->index > vars[level]->index) {
			dd_array[level+1][i] = dd_array[level][i];
		} else {
			dd_array[level+1][i] = Cudd_T(dd_array[level][i]);
		}
	}
	export_rec(t, vars, num_vars, level+1, odd->t, index+odd->eoff);
}

//------------------------------------------------------------------------------
