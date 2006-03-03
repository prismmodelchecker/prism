//==============================================================================
//	
//	File:		dd_vars.cc
//	Date:		17/9/99
//	Author:		Dave Parker
//	Desc:		DD variable functions
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

#include <util.h>
#include <cudd.h>
#include "dd_vars.h"

//------------------------------------------------------------------------------

// note: this PERMUTES variables (i.e. x_i -> y_i) cf. DD_SwapVariables

DdNode *DD_PermuteVariables
(
DdManager *ddman,
DdNode *dd,
DdNode **old_vars,
DdNode **new_vars,
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
		permut[old_vars[i]->index] = new_vars[i]->index;
	}	
	res = Cudd_addPermute(ddman, dd, permut);	
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd);

	if (permut != NULL) {
		delete permut;
	}
	
	return res;
}

//------------------------------------------------------------------------------

// note: this SWAPS variables (i.e. x_i <-> y_i) cf. DD_PermuteVariables

DdNode *DD_SwapVariables
(
DdManager *ddman,
DdNode *dd,
DdNode **old_vars,
DdNode **new_vars,
int num_vars
)
{
	DdNode *res;
	
	res = Cudd_addSwapVariables(ddman, dd, old_vars, new_vars, num_vars);
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd);
	
	return res;
}

//------------------------------------------------------------------------------
