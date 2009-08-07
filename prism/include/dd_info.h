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

//------------------------------------------------------------------------------

int DD_GetNumNodes(DdManager *ddman, DdNode *dd);
int DD_GetNumTerminals(DdManager *ddman, DdNode *dd);
double DD_GetNumMinterms(DdManager *ddman, DdNode *dd, int num_vars);
double DD_GetNumPaths(DdManager *ddman, DdNode *dd);
void DD_PrintInfo(DdManager *ddman, DdNode *dd, int num_vars);
void DD_PrintInfoBrief(DdManager *ddman, DdNode *dd, int num_vars);
void DD_PrintSupport(DdManager *ddman, DdNode *dd);
void DD_PrintSupportNames(DdManager *ddman, DdNode *dd, char **var_names);
DdNode *DD_GetSupport(DdManager *ddman, DdNode *dd);
void DD_PrintTerminals(DdManager *ddman, DdNode *dd);
void DD_PrintTerminalsAndNumbers(DdManager *ddman, DdNode *dd, int num_vars);

//------------------------------------------------------------------------------
