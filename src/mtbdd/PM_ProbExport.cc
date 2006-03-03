//==============================================================================
//	
//	File:		PM_ProbExport.cc
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
#include "PrismMTBDD.h"
#include <util.h>
#include <cudd.h>
#include <dd.h>
#include <odd.h>
#include "PrismMTBDDGlob.h"

//------------------------------------------------------------------------------

// local function prototypes

static void prob_export_rec(DdNode *dd, DdNode **rvars, DdNode **cvars, int num_vars, int level, ODDNode *row, ODDNode *col, long r, long c, FILE *file);
static int exporttype;

//------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_mtbdd_PrismMTBDD_PM_1ProbExport
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
	
	const char *filename = env->GetStringUTFChars(fn, 0);
	FILE *file = fopen(filename, "w");
	if (!file) {
		env->ReleaseStringUTFChars(fn, filename);
		return -1;
	}
	exporttype = et;
	
	// print file header
	switch (exporttype) {
	case EXPORT_PLAIN: fprintf(file, "%d %.0f\n", odd->eoff+odd->toff, DD_GetNumMinterms(ddman, trans, num_rvars+num_cvars)); break;
	case EXPORT_MATLAB: fprintf(file, "P = sparse(%d,%d);\n", odd->eoff+odd->toff, odd->eoff+odd->toff); break;
	}
	
	// print main part of file
	prob_export_rec(trans, rvars, cvars, num_rvars, 0, odd, odd, 0, 0, file);
	
	fclose(file);
	env->ReleaseStringUTFChars(fn, filename);
	
	return 0;
}

//------------------------------------------------------------------------------

void prob_export_rec(DdNode *dd, DdNode **rvars, DdNode **cvars, int num_vars, int level, ODDNode *row, ODDNode *col, long r, long c, FILE *file)
{
	DdNode *e, *t, *ee, *et, *te, *tt;

	// base case - zero terminal
	if (dd == Cudd_ReadZero(ddman)) return;
	
	// base case - non zero terminal
	if (level == num_vars) {
		switch (exporttype) {
			case EXPORT_PLAIN: fprintf(file, "%d %d %.12f\n", r, c, Cudd_V(dd)); break;
			case EXPORT_MATLAB: fprintf(file, "P(%d,%d)=%.12f;\n", r+1, c+1, Cudd_V(dd)); break;
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

	prob_export_rec(ee, rvars, cvars, num_vars, level+1, row->e, col->e, r, c, file);
	prob_export_rec(et, rvars, cvars, num_vars, level+1, row->e, col->t, r, c+col->eoff, file);
	prob_export_rec(te, rvars, cvars, num_vars, level+1, row->t, col->e, r+row->eoff, c, file);
	prob_export_rec(tt, rvars, cvars, num_vars, level+1, row->t, col->t, r+row->eoff, c+col->eoff, file);
}

//------------------------------------------------------------------------------
