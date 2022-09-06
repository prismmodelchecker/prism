//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	Description: DD variable functions
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
#include "dd_vars.h"
#include "dd_basics.h"

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

	if (dd == NULL) return NULL;

	permut = new int[Cudd_ReadSize(ddman)];
	for (i = 0; i < Cudd_ReadSize(ddman); i++) {
		permut[i] = i;
	}
	for (i = 0; i < num_vars; i++) {
		permut[old_vars[i]->index] = new_vars[i]->index;
	}	
	res = Cudd_addPermute(ddman, dd, permut);	
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd);

	if (permut != NULL) {
		delete[] permut;
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

	if (dd == NULL) return NULL;

	res = Cudd_addSwapVariables(ddman, dd, old_vars, new_vars, num_vars);
	if (res != NULL) Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, dd);
	
	return res;
}

//------------------------------------------------------------------------------

// Generates BDD for the function x > y
// where x, y are num_vars-bit numbers encoded by variables x_vars, y_vars

DdNode *DD_VariablesGreaterThan
(
DdManager *ddman,
DdNode **x_vars,
DdNode **y_vars,
int num_vars
)
{
	DdNode *tmp, *res;
	DdNode **x_bdd_vars, **y_bdd_vars;
	int i;
	
	// create bdd vars from add vars
	x_bdd_vars = new DdNode*[num_vars];
	y_bdd_vars = new DdNode*[num_vars];
	for (i = 0; i < num_vars; i++) {
		x_bdd_vars[i] = Cudd_bddIthVar(ddman, x_vars[i]->index);
		if (x_bdd_vars[i] == NULL) return NULL;
		Cudd_Ref(x_bdd_vars[i]);
		y_bdd_vars[i] = Cudd_bddIthVar(ddman, y_vars[i]->index);
		if (y_bdd_vars[i] == NULL) return NULL;
		Cudd_Ref(y_bdd_vars[i]);
	}
	// call main function
	tmp = Cudd_Xgty(ddman, num_vars, NULL, x_bdd_vars, y_bdd_vars);
	if (tmp == NULL) return NULL;
	Cudd_Ref(tmp);
	// convert bdd to add
	res = Cudd_BddToAdd(ddman, tmp);
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, tmp);
	// free variables
	for (i = 0; i < num_vars; i++) {
		Cudd_RecursiveDeref(ddman, x_bdd_vars[i]);
		Cudd_RecursiveDeref(ddman, y_bdd_vars[i]);
	}
	delete[] x_bdd_vars;
	delete[] y_bdd_vars;
	
	return res;
}

//------------------------------------------------------------------------------

// Generates BDD for the function x >= y
// where x, y are num_vars-bit numbers encoded by variables x_vars, y_vars

DdNode *DD_VariablesGreaterThanEquals
(
DdManager *ddman,
DdNode **x_vars,
DdNode **y_vars,
int num_vars
)
{
	DdNode* res = DD_VariablesLessThan(ddman, x_vars, y_vars, num_vars);
	if (res == NULL) return NULL;
	return DD_Not(ddman, res);
}

//------------------------------------------------------------------------------

// Generates BDD for the function x < y
// where x, y are num_vars-bit numbers encoded by variables x_vars, y_vars

DdNode *DD_VariablesLessThan
(
DdManager *ddman,
DdNode **x_vars,
DdNode **y_vars,
int num_vars
)
{
	DdNode *tmp, *res;
	DdNode **x_bdd_vars, **y_bdd_vars;
	int i;
	
	// create bdd vars from add vars
	x_bdd_vars = new DdNode*[num_vars];
	y_bdd_vars = new DdNode*[num_vars];
	for (i = 0; i < num_vars; i++) {
		x_bdd_vars[i] = Cudd_bddIthVar(ddman, x_vars[i]->index);
		if (x_bdd_vars[i] == NULL) return NULL;
		Cudd_Ref(x_bdd_vars[i]);
		y_bdd_vars[i] = Cudd_bddIthVar(ddman, y_vars[i]->index);
		if (y_bdd_vars[i] == NULL) return NULL;
		Cudd_Ref(y_bdd_vars[i]);
	}
	// call main function
	tmp = Cudd_Xgty(ddman, num_vars, NULL, y_bdd_vars, x_bdd_vars);
	if (tmp == NULL) return NULL;
	Cudd_Ref(tmp);
	// convert bdd to add
	res = Cudd_BddToAdd(ddman, tmp);
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, tmp);
	// free variables
	for (i = 0; i < num_vars; i++) {
		Cudd_RecursiveDeref(ddman, x_bdd_vars[i]);
		Cudd_RecursiveDeref(ddman, y_bdd_vars[i]);
	}
	delete[] x_bdd_vars;
	delete[] y_bdd_vars;
	
	return res;
}

//------------------------------------------------------------------------------

// Generates BDD for the function x <= y
// where x, y are num_vars-bit numbers encoded by variables x_vars, y_vars

DdNode *DD_VariablesLessThanEquals
(
DdManager *ddman,
DdNode **x_vars,
DdNode **y_vars,
int num_vars
)
{
	DdNode* res = DD_VariablesGreaterThan(ddman, x_vars, y_vars, num_vars);
	return DD_Not(ddman, res);
}

//------------------------------------------------------------------------------

// Generates BDD for the function x == y
// where x, y are num_vars-bit numbers encoded by variables x_vars, y_vars

DdNode *DD_VariablesEquals
(
DdManager *ddman,
DdNode **x_vars,
DdNode **y_vars,
int num_vars
)
{
	DdNode *tmp, *res;
	DdNode **x_bdd_vars, **y_bdd_vars;
	int i;
	
	// create bdd vars from add vars
	x_bdd_vars = new DdNode*[num_vars];
	y_bdd_vars = new DdNode*[num_vars];
	for (i = 0; i < num_vars; i++) {
		x_bdd_vars[i] = Cudd_bddIthVar(ddman, x_vars[i]->index);
		if (x_bdd_vars[i] == NULL) return NULL;
		Cudd_Ref(x_bdd_vars[i]);
		y_bdd_vars[i] = Cudd_bddIthVar(ddman, y_vars[i]->index);
		if (y_bdd_vars[i] == NULL) return NULL;
		Cudd_Ref(y_bdd_vars[i]);
	}
	// call main function
	tmp = Cudd_Xeqy(ddman, num_vars, x_bdd_vars, y_bdd_vars);
	if (tmp == NULL) return NULL;
	Cudd_Ref(tmp);
	// convert bdd to add
	res = Cudd_BddToAdd(ddman, tmp);
	if (res == NULL) return NULL;
	Cudd_Ref(res);
	Cudd_RecursiveDeref(ddman, tmp);
	// free variables
	for (i = 0; i < num_vars; i++) {
		Cudd_RecursiveDeref(ddman, x_bdd_vars[i]);
		Cudd_RecursiveDeref(ddman, y_bdd_vars[i]);
	}
	delete[] x_bdd_vars;
	delete[] y_bdd_vars;
	
	return res;
}

//------------------------------------------------------------------------------
