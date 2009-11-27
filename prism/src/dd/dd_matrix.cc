//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	Description: DD matrix functions
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

#include <stdio.h>
#include <stdlib.h>
#include <math.h>
#include <util.h>
#include <cudd.h>
#include <cuddInt.h>
#include "dd_matrix.h"
#include "dd_basics.h"

extern FILE *dd_out;

//------------------------------------------------------------------------------

DdNode *DD_SetVectorElement
(
DdManager *ddman,
DdNode *dd,
DdNode **vars,
int num_vars,
long index,
double value
)
{
	DdNode *tmp, *tmp2, *f, *tmp_f, *g, *res;
	int i;
		
	// build a 0-1 ADD to store position of element of the vector
	f = DD_Constant(ddman, 1);
	for (i = 0; i < num_vars; i++) {
		Cudd_Ref(tmp = vars[i]);				
		if ((index & (1l<<(num_vars-i-1))) == 0) {
			tmp2 = Cudd_addCmpl(ddman, tmp);
			Cudd_Ref(tmp2);
			Cudd_RecursiveDeref(ddman, tmp);
			tmp = tmp2;
		}
		tmp_f = Cudd_addApply(ddman, Cudd_addTimes, tmp, f);
		Cudd_Ref(tmp_f);
		Cudd_RecursiveDeref(ddman, tmp);
		Cudd_RecursiveDeref(ddman, f);
		f = tmp_f;
	}
	
	g = DD_Constant(ddman, value);
				
	// compute new vector
	res = Cudd_addIte(ddman, f, g, dd);
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, f);
	Cudd_RecursiveDeref(ddman, g);
	Cudd_RecursiveDeref(ddman, dd);
	
	return res;
}

//------------------------------------------------------------------------------

DdNode *DD_SetMatrixElement
(
DdManager *ddman,
DdNode *dd,
DdNode **rvars,
int num_rvars,
DdNode **cvars,
int num_cvars,
long rindex,
long cindex,
double value
)
{
	DdNode *tmp, *tmp2, *f, *tmp_f, *g, *res;
	int i;
		
	// build a 0-1 ADD to store position of element of the matrix
	f = DD_Constant(ddman, 1);
	for (i = 0; i < num_rvars; i++) {
		Cudd_Ref(tmp = rvars[i]);				
		if ((rindex & (1l<<(num_rvars-i-1))) == 0) {
			tmp2 = Cudd_addCmpl(ddman, tmp);
			Cudd_Ref(tmp2);
			Cudd_RecursiveDeref(ddman, tmp);
			tmp = tmp2;
		}
		tmp_f = Cudd_addApply(ddman, Cudd_addTimes, tmp, f);
		Cudd_Ref(tmp_f);
		Cudd_RecursiveDeref(ddman, tmp);
		Cudd_RecursiveDeref(ddman, f);
		f = tmp_f;
	}
	for (i = 0; i < num_cvars; i++) {
		Cudd_Ref(tmp = cvars[i]);				
		if ((cindex & (1l<<(num_cvars-i-1))) == 0) {
			tmp2 = Cudd_addCmpl(ddman, tmp);
			Cudd_Ref(tmp2);
			Cudd_RecursiveDeref(ddman, tmp);
			tmp = tmp2;
		}
		tmp_f = Cudd_addApply(ddman, Cudd_addTimes, tmp, f);
		Cudd_Ref(tmp_f);
		Cudd_RecursiveDeref(ddman, tmp);
		Cudd_RecursiveDeref(ddman, f);
		f = tmp_f;
	}
	
	g = DD_Constant(ddman, value);
				
	// compute new vector
	res = Cudd_addIte(ddman, f, g, dd);
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, f);
	Cudd_RecursiveDeref(ddman, g);
	Cudd_RecursiveDeref(ddman, dd);
	
	return res;
}

//------------------------------------------------------------------------------

DdNode *DD_Set3DMatrixElement
(
DdManager *ddman,
DdNode *dd,
DdNode **rvars,
int num_rvars,
DdNode **cvars,
int num_cvars,
DdNode **lvars,
int num_lvars,
long rindex,
long cindex,
long lindex,
double value
)
{
	DdNode *tmp, *tmp2, *f, *tmp_f, *g, *res;
	int i;
		
	// build a 0-1 ADD to store position of element of the matrix
	f = DD_Constant(ddman, 1);
	for (i = 0; i < num_rvars; i++) {
		Cudd_Ref(tmp = rvars[i]);				
		if ((rindex & (1l<<(num_rvars-i-1))) == 0) {
			tmp2 = Cudd_addCmpl(ddman, tmp);
			Cudd_Ref(tmp2);
			Cudd_RecursiveDeref(ddman, tmp);
			tmp = tmp2;
		}
		tmp_f = Cudd_addApply(ddman, Cudd_addTimes, tmp, f);
		Cudd_Ref(tmp_f);
		Cudd_RecursiveDeref(ddman, tmp);
		Cudd_RecursiveDeref(ddman, f);
		f = tmp_f;
	}
	for (i = 0; i < num_cvars; i++) {
		Cudd_Ref(tmp = cvars[i]);				
		if ((cindex & (1l<<(num_cvars-i-1))) == 0) {
			tmp2 = Cudd_addCmpl(ddman, tmp);
			Cudd_Ref(tmp2);
			Cudd_RecursiveDeref(ddman, tmp);
			tmp = tmp2;
		}
		tmp_f = Cudd_addApply(ddman, Cudd_addTimes, tmp, f);
		Cudd_Ref(tmp_f);
		Cudd_RecursiveDeref(ddman, tmp);
		Cudd_RecursiveDeref(ddman, f);
		f = tmp_f;
	}
	for (i = 0; i < num_lvars; i++) {
		Cudd_Ref(tmp = lvars[i]);				
		if ((lindex & (1l<<(num_lvars-i-1))) == 0) {
			tmp2 = Cudd_addCmpl(ddman, tmp);
			Cudd_Ref(tmp2);
			Cudd_RecursiveDeref(ddman, tmp);
			tmp = tmp2;
		}
		tmp_f = Cudd_addApply(ddman, Cudd_addTimes, tmp, f);
		Cudd_Ref(tmp_f);
		Cudd_RecursiveDeref(ddman, tmp);
		Cudd_RecursiveDeref(ddman, f);
		f = tmp_f;
	}
	
	g = DD_Constant(ddman, value);
				
	// compute new vector
	res = Cudd_addIte(ddman, f, g, dd);
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, f);
	Cudd_RecursiveDeref(ddman, g);
	Cudd_RecursiveDeref(ddman, dd);
	
	return res;
}

//------------------------------------------------------------------------------

double DD_GetVectorElement
(
DdManager *ddman,
DdNode *dd,
DdNode **vars,
int num_vars,
long x
)
{
	int i;
	DdNode *node;
	int *inputs;
	double val;

	// create array to store 0's & 1's used to query DD
	inputs = new int[Cudd_ReadSize(ddman)];
	
	for (i = 0; i < Cudd_ReadSize(ddman); i++) {
		inputs[i] = 0;
	}
		
	for (i = 0; i < num_vars; i++) {			
		inputs[vars[i]->index] = ((x & (1l<<(num_vars-i-1))) == 0) ? 0 : 1;
	}
	node = Cudd_Eval(ddman, dd, inputs);
	val = Cudd_V(node);
	
	if (inputs != NULL) {
		delete[] inputs;
	}
	
	return val;
}

//------------------------------------------------------------------------------

DdNode *DD_Identity
(
DdManager *ddman,
DdNode **rvars,
DdNode **cvars,
int num_vars
)
{
	DdNode *tmp;
	
	tmp = Cudd_addXeqy(ddman, num_vars, rvars, cvars);
	Cudd_Ref(tmp);

	return tmp;
}

//------------------------------------------------------------------------------

DdNode *DD_MatrixMultiply
(
DdManager *ddman,
DdNode *dd1,
DdNode *dd2,
DdNode **vars,
int num_vars,
int method
)
{
	DdNode *res;
	
	if (method == MM_CMU) {
		res = Cudd_addTimesPlus(ddman, dd1, dd2, vars, num_vars);
	}
	else if (method == MM_BOULDER) {
		res = Cudd_addMatrixMultiply(ddman, dd1, dd2, vars, num_vars);
	}
	else {
		printf("Error: no multiplication algorithm specified\n");
		exit(1);
	}
	if (res == NULL) {
		printf("DD_MatrixMultiply: res is NULL\n");
		exit(1);
	}
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd1);
	Cudd_RecursiveDeref(ddman, dd2);
	
	return res;
}

//------------------------------------------------------------------------------

DdNode *DD_Transpose
(
DdManager *ddman,
DdNode *dd,
DdNode **row_vars,
DdNode **col_vars,
int num_vars
)
{
	int i, *permut;
	DdNode *res;
		
	permut = new int[Cudd_ReadSize(ddman)];
	for (i = 0; i < Cudd_ReadSize(ddman); i++) {
		permut[i] = i;
	}
	for (i = 0; i < num_vars; i++) {
		permut[row_vars[i]->index] = col_vars[i]->index;
		permut[col_vars[i]->index] = row_vars[i]->index;
	}	
	res = Cudd_addPermute(ddman, dd, permut);	
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd);

	if (permut != NULL) {
		delete[] permut;
	}
	
	return res;
}

//------------------------------------------------------------------------------

void DD_PrintVector
(
DdManager *ddman,
DdNode *dd,
DdNode **vars,
int num_vars
)
{
	DD_PrintVector(ddman, dd, vars, num_vars, ACCURACY_NORMAL);
}

//------------------------------------------------------------------------------

void DD_PrintVector
(
DdManager *ddman,
DdNode *dd,
DdNode **vars,
int num_vars,
int accuracy
)
{
	int j;
	long i, length;
	DdNode *node;
	int *inputs;
	double val;

	// create array to store 0's & 1's used to query DD
	inputs = new int[Cudd_ReadSize(ddman)];
	
	for (j = 0; j < Cudd_ReadSize(ddman); j++) {
		inputs[j] = 0;
	}
		
	length = (long)pow(2.0, num_vars);

	for (i = 0; i < length; i++) {
		for (j = 0; j < num_vars; j++) {			
			inputs[vars[j]->index] = ((i & (1l<<(num_vars-j-1))) == 0) ? 0 : 1;
		}
		node = Cudd_Eval(ddman, dd, inputs);
		val = Cudd_V(node);
		switch (accuracy) {
		case ACCURACY_ZERO_ONE: fprintf(dd_out, "%c", val>0?'1':'0'); break;
		case ACCURACY_LOW: fprintf(dd_out, "%.2f ", val); break;
		case ACCURACY_NORMAL: fprintf(dd_out, "%f ", val); break;
		case ACCURACY_HIGH: fprintf(dd_out, "%.10f ", val); break;
		case ACCURACY_LIST: if (val>0) fprintf(dd_out, "%ld:%f ", i, val); break;
		}
	}
	fprintf(dd_out, "\n");
	
	if (inputs != NULL) {
		delete[] inputs;
	}
}

//------------------------------------------------------------------------------

void DD_PrintMatrix
(
DdManager *ddman,
DdNode *dd,
DdNode **rvars,
int num_rvars,
DdNode **cvars,
int num_cvars
)
{
	DD_PrintMatrix(ddman, dd, rvars, num_rvars, cvars, num_cvars, ACCURACY_NORMAL);
}

//------------------------------------------------------------------------------

void DD_PrintMatrix
(
DdManager *ddman,
DdNode *dd,
DdNode **rvars,
int num_rvars,
DdNode **cvars,
int num_cvars,
int accuracy
)
{
	int i, j, rows, cols;
	int k;
	DdNode *node;
	int *inputs;
	double val;

	// create array to store 0's & 1's used to query DD
	inputs = new int[Cudd_ReadSize(ddman)];
	
	for (k = 0; k < Cudd_ReadSize(ddman); k++) {
		inputs[k] = 0;
	}
		
	rows = (long)pow(2.0, num_rvars);
	cols = (long)pow(2.0, num_cvars);
	
	for (i = 0; i < rows; i++) {
		for (j = 0; j < cols; j++) {
			for (k = 0; k < num_rvars; k++) {			
				inputs[rvars[k]->index] = ((i & (1l<<(num_rvars-k-1))) == 0) ? 0 : 1;
			}
			for (k = 0; k < num_cvars; k++) {			
				inputs[cvars[k]->index] = ((j & (1l<<(num_cvars-k-1))) == 0) ? 0 : 1;
			}
			node = Cudd_Eval(ddman, dd, inputs);
			val = Cudd_V(node);
			switch (accuracy) {
			case ACCURACY_ZERO_ONE: fprintf(dd_out, "%c", val>0?'1':'0'); break;
			case ACCURACY_LOW: fprintf(dd_out, "%.2f ", val); break;
			case ACCURACY_NORMAL: fprintf(dd_out, "%f ", val); break;
			case ACCURACY_HIGH: fprintf(dd_out, "%.10f ", val); break;
			case ACCURACY_LIST: if (val>0) fprintf(dd_out, "%d,%d:%f ", i, j, val); break;
			}
		}
		if (accuracy != ACCURACY_LIST) fprintf(dd_out, "\n");
	}
	
	if (inputs != NULL) {
		delete[] inputs;
	}
}

//------------------------------------------------------------------------------

void DD_PrintVectorFiltered
(
DdManager *ddman,
DdNode *dd,
DdNode *filter,
DdNode **vars,
int num_vars
)
{
	DD_PrintVectorFiltered(ddman, dd, filter, vars, num_vars, ACCURACY_NORMAL);
}

//------------------------------------------------------------------------------

void DD_PrintVectorFiltered
(
DdManager *ddman,
DdNode *dd,
DdNode *filter,
DdNode **vars,
int num_vars,
int accuracy
)
{
	int j;
	long i, length, count;
	DdNode *node;
	int *inputs;
	double val;

	// create array to store 0's & 1's used to query DD
	inputs = new int[Cudd_ReadSize(ddman)];
	
	for (j = 0; j < Cudd_ReadSize(ddman); j++) {
		inputs[j] = 0;
	}
		
	length = (long)pow(2.0, num_vars);

	count = -1;
	for (i = 0; i < length; i++) {
		// only print elements which get thru filter
		if (DD_GetVectorElement(ddman, filter, vars, num_vars, i) > 0) {
			count++;
			for (j = 0; j < num_vars; j++) {			
				inputs[vars[j]->index] = ((i & (1l<<(num_vars-j-1))) == 0) ? 0 : 1;
			}
			node = Cudd_Eval(ddman, dd, inputs);
			val = Cudd_V(node);
			switch (accuracy) {
			case ACCURACY_ZERO_ONE: fprintf(dd_out, "%c", val>0?'1':'0'); break;
			case ACCURACY_LOW: fprintf(dd_out, "%.2f ", val); break;
			case ACCURACY_NORMAL: fprintf(dd_out, "%f ", val); break;
			case ACCURACY_HIGH: fprintf(dd_out, "%.10f ", val); break;
			case ACCURACY_LIST: if (val>0) fprintf(dd_out, "%ld:%f ", count, val); break;
			}
		}
	}
	fprintf(dd_out, "\n");
	
	if (inputs != NULL) {
		delete[] inputs;
	}
}

//------------------------------------------------------------------------------
