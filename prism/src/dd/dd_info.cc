//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

extern void DD_PrintTerminals(DdManager *ddman, DdNode *dd, int num_vars, bool and_numbers);

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
	DD_PrintSupportNames(ddman, dd, NULL);
}

//------------------------------------------------------------------------------

void DD_PrintSupportNames
(
DdManager *ddman,
DdNode *dd,
char **var_names
)
{
	int i;
	DdNode *supp, *tmp;
	
	fprintf(dd_out, "(");
	supp = Cudd_Support(ddman, dd);
	Cudd_Ref(supp);
	tmp = supp;
	while (!Cudd_IsConstant(tmp)) {
		i = Cudd_NodeReadIndex(tmp);
		if (var_names) fprintf(dd_out, " %s", var_names[i]);
		else fprintf(dd_out, " %d", i);
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

void DD_PrintTerminals(DdManager *ddman, DdNode *dd) { DD_PrintTerminals(ddman, dd, 0, false); }

void DD_PrintTerminalsAndNumbers(DdManager *ddman, DdNode *dd, int num_vars) { DD_PrintTerminals(ddman, dd, num_vars, true); }

void DD_PrintTerminals
(
DdManager *ddman,
DdNode *dd,
int num_vars,
bool and_numbers)
{
	DdNode *tmp, *tmp2;
	double min, max, num, count = 0.0;

	// Take a copy of dd	
	Cudd_Ref(dd);
	tmp = dd;
	// Check the min (will use at end)
	min = Cudd_V(Cudd_addFindMin(ddman, tmp));
	// Loop through terminals in descending order
	while (tmp != Cudd_ReadMinusInfinity(ddman)) {
		// Find next (max) terminal and display
		max = Cudd_V(Cudd_addFindMax(ddman, tmp));
		fprintf(dd_out, "%f ", max);
		// Remove the terminals, counting/displaying number if required
		Cudd_Ref(tmp);
		tmp2 = DD_Equals(ddman, tmp, max);
		if (and_numbers) {
			num = Cudd_CountMinterm(ddman, tmp2, num_vars);
			count += num;
			fprintf(dd_out, "(%.0f) ", num);
		}
		tmp = DD_ITE(ddman, tmp2, DD_MinusInfinity(ddman), tmp);
	}
	Cudd_RecursiveDeref(ddman, tmp);
	// Finally, print if there are (and possibly how many) minus infinities
	if (and_numbers) {
		if (count < (1<<num_vars)) fprintf(dd_out, "-inf (%.0f)", pow(2.0, num_vars) - count);
	} else {
		if (min == -HUGE_VAL) fprintf(dd_out, "-inf");
	}
	fprintf(dd_out, "\n");
}

//------------------------------------------------------------------------------
