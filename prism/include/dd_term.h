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

DdNode *DD_Threshold(DdManager *ddman, DdNode *dd, double threshold);
DdNode *DD_StrictThreshold(DdManager *ddman, DdNode *dd, double threshold);
DdNode *DD_GreaterThan(DdManager *ddman, DdNode *dd, double threshold);
DdNode *DD_GreaterThanEquals(DdManager *ddman, DdNode *dd, double threshold);
DdNode *DD_LessThan(DdManager *ddman, DdNode *dd, double threshold);
DdNode *DD_LessThanEquals(DdManager *ddman, DdNode *dd, double threshold);
DdNode *DD_Equals(DdManager *ddman, DdNode *dd, double value);
DdNode *DD_Interval(DdManager *ddman, DdNode *dd, double lower, double upper);
DdNode *DD_RoundOff(DdManager *ddman, DdNode *dd, int places);
bool DD_EqualSupNorm(DdManager *ddman, DdNode *dd1, DdNode *dd2, double epsilon);
bool DD_EqualSupNormRel(DdManager *ddman, DdNode *dd1, DdNode *dd2, double epsilon);
double DD_FindMin(DdManager *ddman, DdNode *dd);
double DD_FindMinPositive(DdManager *ddman, DdNode *dd);
double DD_FindMax(DdManager *ddman, DdNode *dd);
double DD_FindMaxFinite(DdManager *ddman, DdNode *dd);
DdNode *DD_RestrictToFirst(DdManager *ddman, DdNode *dd, DdNode **vars, int num_vars);
bool DD_IsZeroOneMTBDD(DdManager *ddman, DdNode *dd);


//------------------------------------------------------------------------------
