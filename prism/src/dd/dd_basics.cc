//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	Description: Basic DD functions
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

#include <cstdio>
#include <cstdlib>
#include <util.h>
#include <cudd.h>
#include "dd_basics.h"

//------------------------------------------------------------------------------

DdNode *DD_Create(DdManager *ddman)
{
	DdNode *res;

	res = Cudd_addConst(ddman, 0);
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	
	return res;
}

//------------------------------------------------------------------------------

DdNode *DD_Constant(DdManager *ddman, double value)
{
	DdNode *res;

	res = Cudd_addConst(ddman, value);
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	
	return res;
}

//------------------------------------------------------------------------------

DdNode *DD_PlusInfinity(DdManager *ddman)
{
	DdNode *res;

	res = Cudd_ReadPlusInfinity(ddman);
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	
	return res;
}

//------------------------------------------------------------------------------

DdNode *DD_MinusInfinity(DdManager *ddman)
{
	DdNode *res;

	res = Cudd_ReadMinusInfinity(ddman);
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	
	return res;
}

//------------------------------------------------------------------------------

DdNode *DD_Var(DdManager *ddman, int i)
{
	DdNode *res;

	res = Cudd_addIthVar(ddman, i);
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	
	return res;
}

//------------------------------------------------------------------------------

DdNode *DD_Not(DdManager *ddman, DdNode *dd)
{
	DdNode *res;

	if (dd == NULL) return NULL;

	res = Cudd_addCmpl(ddman, dd);
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd);
	
	return res;
}

//------------------------------------------------------------------------------

DdNode *DD_Or(DdManager *ddman, DdNode *dd1, DdNode *dd2)
{
	DdNode *res;

	if (dd1 == NULL) return NULL;
	if (dd2 == NULL) return NULL;

	res = Cudd_addApply(ddman, Cudd_addOr, dd1, dd2);
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd1);
	Cudd_RecursiveDeref(ddman, dd2);
	
	return res;
}

//------------------------------------------------------------------------------

DdNode *DD_And(DdManager *ddman, DdNode *dd1, DdNode *dd2)
{
	if (dd1 == NULL) return NULL;
	if (dd2 == NULL) return NULL;

	return DD_Not(ddman, DD_Or(ddman, DD_Not(ddman, dd1), DD_Not(ddman, dd2))); 
}

//------------------------------------------------------------------------------

DdNode *DD_Xor(DdManager *ddman, DdNode *dd1, DdNode *dd2)
{
	DdNode *res;

	if (dd1 == NULL) return NULL;
	if (dd2 == NULL) return NULL;

	res = Cudd_addApply(ddman, Cudd_addXor, dd1, dd2);
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd1);
	Cudd_RecursiveDeref(ddman, dd2);
	
	return res;
}

//------------------------------------------------------------------------------

DdNode *DD_Implies(DdManager *ddman, DdNode *dd1, DdNode *dd2)
{
	if (dd1 == NULL) return NULL;
	if (dd2 == NULL) return NULL;

	return DD_Or(ddman, DD_Not(ddman, dd1), dd2);
}

//------------------------------------------------------------------------------

DdNode *DD_Apply(DdManager *ddman, int op, DdNode *dd1, DdNode *dd2)
{
	DdNode *res;

	if (dd1 == NULL) return NULL;
	if (dd2 == NULL) return NULL;

	switch (op) {
		case APPLY_PLUS: res = Cudd_addApply(ddman, Cudd_addPlus, dd1, dd2); break;
		case APPLY_MINUS: res = Cudd_addApply(ddman, Cudd_addMinus, dd1, dd2); break;
		case APPLY_TIMES: res = Cudd_addApply(ddman, Cudd_addTimes, dd1, dd2); break;
		case APPLY_DIVIDE: res = Cudd_addApply(ddman, Cudd_addDivide, dd1, dd2); break;
		case APPLY_MIN: res = Cudd_addApply(ddman, Cudd_addMinimum, dd1, dd2); break;
		case APPLY_MAX: res = Cudd_addApply(ddman, Cudd_addMaximum, dd1, dd2); break;
		case APPLY_EQUALS: res = Cudd_addApply(ddman, Cudd_addEquals, dd1, dd2); break;
		case APPLY_NOTEQUALS: res = Cudd_addApply(ddman, Cudd_addNotEquals, dd1, dd2); break;
		case APPLY_GREATERTHAN: res = Cudd_addApply(ddman, Cudd_addGreaterThan, dd1, dd2); break;
		case APPLY_GREATERTHANEQUALS: res = Cudd_addApply(ddman, Cudd_addGreaterThanEquals, dd1, dd2); break;
		case APPLY_LESSTHAN: res = Cudd_addApply(ddman, Cudd_addLessThan, dd1, dd2); break;
		case APPLY_LESSTHANEQUALS: res = Cudd_addApply(ddman, Cudd_addLessThanEquals, dd1, dd2); break;
		case APPLY_POW: res = Cudd_addApply(ddman, Cudd_addPow, dd1, dd2); break;
		case APPLY_MOD: res = Cudd_addApply(ddman, Cudd_addMod, dd1, dd2); break;
		case APPLY_LOGXY: res = Cudd_addApply(ddman, Cudd_addLogXY, dd1, dd2); break;
		default: printf("\nError: Invalid APPLY operator.\n"); exit(1);
	}
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd1);
	Cudd_RecursiveDeref(ddman, dd2);
	
	return res;
}

//------------------------------------------------------------------------------

DdNode *DD_MonadicApply(DdManager *ddman, int op, DdNode *dd)
{
	DdNode *res;

	if (dd == NULL) return NULL;

	switch (op) {
		case APPLY_FLOOR: res = Cudd_addMonadicApply(ddman, Cudd_addFloor, dd); break;
		case APPLY_CEIL: res = Cudd_addMonadicApply(ddman, Cudd_addCeil, dd); break;
		default: printf("\nError: Invalid monadic APPLY operator.\n"); exit(1);
	}
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd);
	
	return res;
}

//------------------------------------------------------------------------------

DdNode *DD_Restrict(DdManager *ddman, DdNode *dd, DdNode *cube)
{
	DdNode *res;

	if (dd == NULL) return NULL;
	if (cube == NULL) return NULL;

	res = Cudd_addRestrict(ddman, dd, cube);
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd);
	Cudd_RecursiveDeref(ddman, cube);
	
	return res;
}

//------------------------------------------------------------------------------

DdNode *DD_ITE(DdManager *ddman, DdNode *dd1, DdNode *dd2, DdNode *dd3)
{
	DdNode *res;

	if (dd1 == NULL) return NULL;
	if (dd2 == NULL) return NULL;
	if (dd3 == NULL) return NULL;

	res = Cudd_addIte(ddman, dd1, dd2, dd3);
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd1);
	Cudd_RecursiveDeref(ddman, dd2);
	Cudd_RecursiveDeref(ddman, dd3);
	
	return res;
}

//------------------------------------------------------------------------------
