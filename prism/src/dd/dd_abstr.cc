//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	Description: DD abstraction functions
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
#include "dd_abstr.h"

//------------------------------------------------------------------------------

DdNode *DD_ThereExists
(
DdManager *ddman,
DdNode *dd,
DdNode **vars,
int num_vars
)
{
	DdNode *cube, *res;

	if (dd == NULL) return NULL;

	cube = Cudd_addComputeCube(ddman, vars, NULL, num_vars);
	if (cube == NULL) return NULL;
	Cudd_Ref(cube);
	res = Cudd_addOrAbstract(ddman, dd, cube);
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd);
	Cudd_RecursiveDeref(ddman,cube);

	return res;	
}

//------------------------------------------------------------------------------

DdNode *DD_ForAll
(
DdManager *ddman,
DdNode *dd,
DdNode **vars,
int num_vars
)
{
	DdNode *cube, *res;

	if (dd == NULL) return NULL;

	cube = Cudd_addComputeCube(ddman, vars, NULL, num_vars);
	if (cube == NULL) return NULL;
	Cudd_Ref(cube);
	res = Cudd_addUnivAbstract(ddman, dd, cube);
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd);
	Cudd_RecursiveDeref(ddman, cube);

	return res;	
}

//------------------------------------------------------------------------------

DdNode *DD_SumAbstract
(
DdManager *ddman,
DdNode *dd,
DdNode **vars,
int num_vars
)
{
	DdNode *cube, *res;

	if (dd == NULL) return NULL;

	cube = Cudd_addComputeCube(ddman, vars, NULL, num_vars);
	if (cube == NULL) return NULL;
	Cudd_Ref(cube);
	res = Cudd_addExistAbstract(ddman, dd, cube);	
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd);
	Cudd_RecursiveDeref(ddman, cube);

	return res;	
}

//------------------------------------------------------------------------------

DdNode *DD_ProductAbstract
(
DdManager *ddman,
DdNode *dd,
DdNode **vars,
int num_vars
)
{
	DdNode *cube, *res;

	if (dd == NULL) return NULL;

	cube = Cudd_addComputeCube(ddman, vars, NULL, num_vars);
	if (cube == NULL) return NULL;
	Cudd_Ref(cube);
	res = Cudd_addUnivAbstract(ddman, dd, cube);	
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd);
	Cudd_RecursiveDeref(ddman, cube);

	return res;	
}

//------------------------------------------------------------------------------

DdNode *DD_MinAbstract
(
DdManager *ddman,
DdNode *dd,
DdNode **vars,
int num_vars
)
{
	DdNode *cube, *res;

	if (dd == NULL) return NULL;

	cube = Cudd_addComputeCube(ddman, vars, NULL, num_vars);
	if (cube == NULL) return NULL;
	Cudd_Ref(cube);
	res = Cudd_addMinAbstract(ddman, dd, cube);	
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd);
	Cudd_RecursiveDeref(ddman, cube);

	return res;	
}

//------------------------------------------------------------------------------

DdNode *DD_MaxAbstract
(
DdManager *ddman,
DdNode *dd,
DdNode **vars,
int num_vars
)
{
	DdNode *cube, *res;

	if (dd == NULL) return NULL;

	cube = Cudd_addComputeCube(ddman, vars, NULL, num_vars);
	if (cube == NULL) return NULL;
	Cudd_Ref(cube);
	res = Cudd_addMaxAbstract(ddman, dd, cube);	
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd);
	Cudd_RecursiveDeref(ddman, cube);

	return res;	
}

//------------------------------------------------------------------------------
