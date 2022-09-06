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

#include <util.h>
#include <cudd.h>

// DD_PrintVector/Matrix accuracy flags

#define ACCURACY_ZERO_ONE 1
#define ACCURACY_LOW 2
#define ACCURACY_NORMAL 3
#define ACCURACY_HIGH 4
#define ACCURACY_LIST 5

// DD_MatrixMultiply method flags

#define MM_CMU 1
#define MM_BOULDER 2

//------------------------------------------------------------------------------

DdNode *DD_SetVectorElement(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, long index, double value);
DdNode *DD_SetMatrixElement(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars, long rindex, long cindex, double value);
DdNode *DD_Set3DMatrixElement(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars, DdNode **lvars, int num_lvars, long rindex, long cindex, long lindex, double value);
double DD_GetVectorElement(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, long index);
DdNode *DD_Identity(DdManager *ddman, DdNode **rvars, DdNode **cvars, int num_vars);
DdNode *DD_Transpose(DdManager *ddman, DdNode *dd, DdNode **row_vars, DdNode **col_vars, int num_vars);
DdNode *DD_MatrixMultiply(DdManager *ddman, DdNode *dd1, DdNode *dd2, DdNode **vars, int num_vars, int method);
void DD_PrintVector(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars);
void DD_PrintVector(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars, int accuracy);
void DD_PrintMatrix(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars);
void DD_PrintMatrix(DdManager *ddman, DdNode *dd, DdNode **rvars, int num_rvars, DdNode **cvars, int num_cvars, int accuracy);
void DD_PrintVectorFiltered(DdManager *ddman, DdNode *dd, DdNode *filter, DdNode **vars, int num_vars);
void DD_PrintVectorFiltered(DdManager *ddman, DdNode *dd, DdNode *filter, DdNode **vars, int num_vars, int accuracy);

//------------------------------------------------------------------------------
