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

#define APPLY_PLUS 1
#define APPLY_MINUS 2
#define APPLY_TIMES 3
#define APPLY_DIVIDE 4
#define APPLY_MIN 5
#define APPLY_MAX 6
#define APPLY_EQUALS 7
#define APPLY_NOTEQUALS 8
#define APPLY_GREATERTHAN 9
#define APPLY_GREATERTHANEQUALS 10
#define APPLY_LESSTHAN 11
#define APPLY_LESSTHANEQUALS 12
#define APPLY_FLOOR 13
#define APPLY_CEIL 14
#define APPLY_POW 15
#define APPLY_MOD 16
#define APPLY_LOGXY 17

//------------------------------------------------------------------------------

DdNode *DD_Create(DdManager *ddman);
DdNode *DD_Constant(DdManager *ddman, double value); 
DdNode *DD_PlusInfinity(DdManager *ddman);
DdNode *DD_MinusInfinity(DdManager *ddman);
DdNode *DD_Var(DdManager *ddman, int i);
DdNode *DD_Not(DdManager *ddman, DdNode *dd);
DdNode *DD_Or(DdManager *ddman, DdNode *dd1, DdNode *dd2);
DdNode *DD_And(DdManager *ddman, DdNode *dd1, DdNode *dd2);
DdNode *DD_Xor(DdManager *ddman, DdNode *dd1, DdNode *dd2);
DdNode *DD_Implies(DdManager *ddman, DdNode *dd1, DdNode *dd2);
DdNode *DD_Apply(DdManager *ddman, int op, DdNode *dd1, DdNode *dd2);
DdNode *DD_MonadicApply(DdManager *ddman, int op, DdNode *dd);
DdNode *DD_Restrict(DdManager *ddman, DdNode *dd, DdNode *cube);
DdNode *DD_ITE(DdManager *ddman, DdNode *dd1, DdNode *dd2, DdNode *dd3);

//------------------------------------------------------------------------------
