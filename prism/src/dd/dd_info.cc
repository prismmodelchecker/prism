//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
//	Description: DD analysis/information functions
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

#include <math.h>
#include <util.h>
#include <cudd.h>
#include "dd_basics.h"
#include "dd_term.h"
#include "dd_info.h"

extern FILE *dd_out;

//------------------------------------------------------------------------------

int DD_GetNumNodes
(
DdManager *ddman,
DdNode *dd
)
{
	return Cudd_DagSize(dd);
}

//------------------------------------------------------------------------------

int DD_GetNumTerminals
(
DdManager *ddman,
DdNode *dd
)
{
	return Cudd_CountLeaves(dd);
}

//------------------------------------------------------------------------------

double DD_GetNumMinterms
(
DdManager *ddman,
DdNode *dd,
int num_vars
)
{
	return Cudd_CountMinterm(ddman, dd, num_vars);
}

//------------------------------------------------------------------------------

double DD_GetNumPaths
(
DdManager *ddman,
DdNode *dd
)
{
	return Cudd_CountPath(dd);
}

//------------------------------------------------------------------------------

void DD_PrintInfo
(
DdManager *ddman,
DdNode *dd,
int num_vars)
{
	int nodes, terminals;
	double minterms;
	
	nodes = Cudd_DagSize(dd);
	terminals = Cudd_CountLeaves(dd);
	minterms = Cudd_CountMinterm(ddman, dd, num_vars);
	fprintf(dd_out, "%d nodes (%d terminal), %.0f minterms\n", nodes, terminals, minterms);
}

//------------------------------------------------------------------------------

void DD_PrintInfoBrief
(
DdManager *ddman,
DdNode *dd,
int num_vars)
{
	int nodes, terminals;
	double minterms;
	
	nodes = Cudd_DagSize(dd);
	terminals = Cudd_CountLeaves(dd);
	minterms = Cudd_CountMinterm(ddman, dd, num_vars);
	fprintf(dd_out, "[%d,%d,%.0f]\n", nodes, terminals, minterms);
}

//------------------------------------------------------------------------------

void DD_PrintSupport
(
DdManager *ddman,
DdNode *dd
)
{
	DdNode *supp, *tmp;
	
	fprintf(dd_out, "(");
	supp = Cudd_Support(ddman, dd);
	Cudd_Ref(supp);
	tmp = supp;
	while (!Cudd_IsConstant(tmp)) {
		fprintf(dd_out, " %d", Cudd_NodeReadIndex(tmp));
		tmp = Cudd_T(tmp);
	}
	fprintf(dd_out, " )\n");
	Cudd_RecursiveDeref(ddman, supp);
}

//------------------------------------------------------------------------------

DdNode *DD_GetSupport
(
DdManager *ddman,
DdNode *dd
)
{
	DdNode *supp, *supp2;
	
	supp = Cudd_Support(ddman, dd);
	Cudd_Ref(supp);
	supp2 = Cudd_BddToAdd(ddman, supp);	
	Cudd_Ref(supp2);	
	Cudd_RecursiveDeref(ddman, supp);
	
	return supp2;
}

//------------------------------------------------------------------------------

void DD_PrintTerminals
(
DdManager *ddman,
DdNode *dd
)
{
	DdNode *tmp;
	double max;
	int count = 0;
	
	Cudd_Ref(dd);
	tmp = dd;

	// remove any negative terminals (if any)
	// and warn the user
	if (Cudd_V(Cudd_addFindMin(ddman, tmp)) < 0) {
		fprintf(dd_out, "Warning: DD_PrintTerminals is ignoring negative terminals.\n");
		Cudd_Ref(tmp);
		tmp = DD_Apply(ddman, APPLY_TIMES, DD_GreaterThan(ddman, tmp, 0), tmp);
	}
	
	while (tmp != Cudd_addConst(ddman, 0)) {
		max = Cudd_V(Cudd_addFindMax(ddman, tmp));
		fprintf(dd_out, "%f ", max);
		count++;
		Cudd_Ref(tmp);
		tmp = DD_ITE(ddman, DD_Equals(ddman, tmp, max), DD_Constant(ddman, 0), tmp);
	}
	Cudd_RecursiveDeref(ddman, tmp);
	if (count < Cudd_CountLeaves(dd)) {
		fprintf(dd_out, "%f", 0.0);
	}
	fprintf(dd_out, "\n");
}

//------------------------------------------------------------------------------

void DD_PrintTerminalsAndNumbers
(
DdManager *ddman,
DdNode *dd,
int num_vars)
{
	DdNode *tmp, *tmp2;
	double max, num, count = 0.0;
	
	Cudd_Ref(dd);
	tmp = dd;

	// remove any negative terminals (if any)
	// and warn the user
	if (Cudd_V(Cudd_addFindMin(ddman, tmp)) < 0) {
		fprintf(dd_out, "Warning: DD_PrintTerminals is ignoring negative terminals.\n");
		Cudd_Ref(tmp);
		tmp = DD_Apply(ddman, APPLY_TIMES, DD_GreaterThan(ddman, tmp, 0), tmp);
	}
	
	while (tmp != Cudd_addConst(ddman, 0)) {
		max = Cudd_V(Cudd_addFindMax(ddman, tmp));
		fprintf(dd_out, "%f ", max);
		Cudd_Ref(tmp);
		tmp2 = DD_Equals(ddman, tmp, max);
		num = Cudd_CountMinterm(ddman, tmp2, num_vars);
		count += num;
		fprintf(dd_out, "(%.0f) ", num);
		tmp = DD_ITE(ddman, tmp2, DD_Constant(ddman, 0), tmp);
	}
	Cudd_RecursiveDeref(ddman, tmp);
	if (count < pow(2, num_vars)) {
		fprintf(dd_out, "%f (%.0f)", 0.0, pow(2, num_vars) - count);
	}
	fprintf(dd_out, "\n");
}

//------------------------------------------------------------------------------
