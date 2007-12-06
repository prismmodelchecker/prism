//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	Description: DD functions on terminals
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
#include "dd_basics.h"
#include "dd_term.h"
#include "dd_export.h"

//------------------------------------------------------------------------------

DdNode *DD_Threshold
(
DdManager *ddman,
DdNode *dd,
double threshold
)
{
	DdNode *tmp, *tmp2;
	
	tmp = Cudd_addBddThreshold(ddman, dd, threshold);
	Cudd_Ref(tmp);
	Cudd_RecursiveDeref(ddman, dd);
	tmp2 = Cudd_BddToAdd(ddman, tmp);	
	Cudd_Ref(tmp2);	
	Cudd_RecursiveDeref(ddman, tmp);
	
	return tmp2;
}

//------------------------------------------------------------------------------

DdNode *DD_StrictThreshold
(
DdManager *ddman,
DdNode *dd,
double threshold
)
{
	DdNode *tmp, *tmp2;
	
	tmp = Cudd_addBddStrictThreshold(ddman, dd, threshold);
	Cudd_Ref(tmp);
	Cudd_RecursiveDeref(ddman, dd);
	tmp2 = Cudd_BddToAdd(ddman, tmp);	
	Cudd_Ref(tmp2);	
	Cudd_RecursiveDeref(ddman, tmp);
	
	return tmp2;
}

//------------------------------------------------------------------------------

DdNode *DD_GreaterThan
(
DdManager *ddman,
DdNode *dd,
double threshold
)
{
	return DD_StrictThreshold(ddman, dd, threshold);
}

//------------------------------------------------------------------------------

DdNode *DD_GreaterThanEquals
(
DdManager *ddman,
DdNode *dd,
double threshold
)
{
	return DD_Threshold(ddman, dd, threshold);
}

//------------------------------------------------------------------------------

DdNode *DD_LessThan
(
DdManager *ddman,
DdNode *dd,
double threshold
)
{
	return DD_Not(ddman, DD_Threshold(ddman, dd, threshold));
}

//------------------------------------------------------------------------------

DdNode *DD_LessThanEquals
(
DdManager *ddman,
DdNode *dd,
double threshold
)
{
	return DD_Not(ddman, DD_StrictThreshold(ddman, dd, threshold));
}

//------------------------------------------------------------------------------

DdNode *DD_Equals
(
DdManager *ddman,
DdNode *dd,
double value
)
{
	return DD_Interval(ddman, dd, value, value);
}

//------------------------------------------------------------------------------

DdNode *DD_Interval
(
DdManager *ddman,
DdNode *dd,
double lower,
double upper
)
{
	DdNode *tmp, *tmp2;
	
	tmp = Cudd_addBddInterval(ddman, dd, lower, upper);
	Cudd_Ref(tmp);
	Cudd_RecursiveDeref(ddman, dd);
	tmp2 = Cudd_BddToAdd(ddman, tmp);	
	Cudd_Ref(tmp2);	
	Cudd_RecursiveDeref(ddman, tmp);
	
	return tmp2;
}

//------------------------------------------------------------------------------

DdNode *DD_RoundOff
(
DdManager *ddman,
DdNode *dd,
int places
)
{
	DdNode *res;
	
	res = Cudd_addRoundOff(ddman, dd, places);
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd);
	
	return res;
}

//------------------------------------------------------------------------------

bool DD_EqualSupNorm
(
DdManager *ddman,
DdNode *dd1,
DdNode *dd2,
double epsilon
)
{
	if (Cudd_EqualSupNorm(ddman, dd1, dd2, epsilon, 0)) {
		return true;
	}
	return false;
}	

//------------------------------------------------------------------------------

bool DD_EqualSupNormRel
(
DdManager *ddman,
DdNode *dd1,
DdNode *dd2,
double epsilon
)
{
	if (Cudd_EqualSupNormRel(ddman, dd1, dd2, epsilon, 0)) {
		return true;
	}
	return false;
}	

//------------------------------------------------------------------------------

double DD_FindMin
(
DdManager *ddman,
DdNode *dd	
)
{
	return Cudd_V(Cudd_addFindMin(ddman, dd));
}

//------------------------------------------------------------------------------

double DD_FindMax
(
DdManager *ddman,
DdNode *dd	
)
{
	return Cudd_V(Cudd_addFindMax(ddman, dd));
}

//------------------------------------------------------------------------------

DdNode *DD_RestrictToFirst
(
DdManager *ddman,
DdNode *dd,
DdNode **vars,
int num_vars
)
{
	int i;
	DdNode *ptr, *next_ptr, *filter, *res;
	
	// construct filter to get first non-zero element
	ptr = dd;
	filter = DD_Constant(ddman, 1);
	for (i = 0; i < num_vars; i++) {
		next_ptr = (ptr->index > vars[i]->index) ? ptr : Cudd_E(ptr);
		if (next_ptr != Cudd_ReadZero(ddman)) {
			Cudd_Ref(vars[i]);
			filter = DD_And(ddman, filter, DD_Not(ddman, vars[i]));
		}
		else {
			next_ptr = (ptr->index > vars[i]->index) ? ptr : Cudd_T(ptr);
			Cudd_Ref(vars[i]);
			filter = DD_And(ddman, filter, vars[i]);
		}
		ptr = next_ptr;
	}
	
	// filter
	res = DD_Apply(ddman, APPLY_TIMES, dd, filter);
	
	return res;
}

//------------------------------------------------------------------------------
