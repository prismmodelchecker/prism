//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	Description: Small test program
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

#include <stdio.h>

#include <util.h>
#include <cudd.h>
#include "dd.h"

int main()
{
	DdManager *ddman;
	DdNode *a, *b, *c;
	DdNode **vars;
	
	printf("\nTest program for DD\n===================\n");
	
	// initialise cudd
	ddman = DD_InitialiseCUDD();
	
	// set up some variables
	vars = new DdNode*[2];
	vars[0] = DD_Var(ddman, 0);
	vars[1] = DD_Var(ddman, 2);
	
	// blank dd
	a = DD_Create(ddman);
	printf("\nCreate()\n");
	DD_PrintVector(ddman, a, vars, 2);
	Cudd_RecursiveDeref(ddman, a);

	// constant
	a = DD_Constant(ddman, 1.5);
	printf("\nConstant(1.5)\n");
	DD_PrintVector(ddman, a, vars, 2);
	Cudd_RecursiveDeref(ddman, a);

	// plus infinity
	a = DD_PlusInfinity(ddman);
	printf("\nPlusInfinity()\n");
	DD_PrintVector(ddman, a, vars, 2);
	Cudd_RecursiveDeref(ddman, a);

	// minus infinity
	a = DD_MinusInfinity(ddman);
	printf("\nMinusInfinity()\n");
	DD_PrintVector(ddman, a, vars, 2);
	Cudd_RecursiveDeref(ddman, a);

	// var
	a = DD_Var(ddman, 0);
	printf("\nVar(0)\n");
	DD_PrintVector(ddman, a, vars, 2);
	Cudd_RecursiveDeref(ddman, a);

	a = DD_Var(ddman, 0);
	b = DD_Var(ddman, 2);
	printf("\na: ");
	DD_PrintVector(ddman, a, vars, 2);
	printf("b: ");
	DD_PrintVector(ddman, b, vars, 2);

	// not
	Cudd_Ref(a);
	c = DD_Not(ddman, a);
	printf("\nNot(a)\n");
	DD_PrintVector(ddman, c, vars, 2);
	Cudd_RecursiveDeref(ddman, c);

	// or
	Cudd_Ref(a);
	Cudd_Ref(b);
	c = DD_Or(ddman, a, b);
	printf("\nOr(a, b)\n");
	DD_PrintVector(ddman, c, vars, 2);
	Cudd_RecursiveDeref(ddman, c);

	// and
	Cudd_Ref(a);
	Cudd_Ref(b);
	c = DD_And(ddman, a, b);
	printf("\nAnd(a, b)\n");
	DD_PrintVector(ddman, c, vars, 2);
	Cudd_RecursiveDeref(ddman, c);

	// xor
	Cudd_Ref(a);
	Cudd_Ref(b);
	c = DD_Xor(ddman, a, b);
	printf("\nXor(a, b)\n");
	DD_PrintVector(ddman, c, vars, 2);
	Cudd_RecursiveDeref(ddman, c);

	// implies
	Cudd_Ref(a);
	Cudd_Ref(b);
	c = DD_Implies(ddman, a, b);
	printf("\nImplies(a, b)\n");
	DD_PrintVector(ddman, c, vars, 2);
	Cudd_RecursiveDeref(ddman, c);

	Cudd_RecursiveDeref(ddman, a);
	Cudd_RecursiveDeref(ddman, b);

	a = DD_Create(ddman);
	a = DD_SetVectorElement(ddman, a, vars, 2, 0, 1);
	a = DD_SetVectorElement(ddman, a, vars, 2, 1, 2);
	a = DD_SetVectorElement(ddman, a, vars, 2, 2, 3);
	a = DD_SetVectorElement(ddman, a, vars, 2, 3, 4);
	b = DD_Create(ddman);
	b = DD_SetVectorElement(ddman, b, vars, 2, 0, 0);
	b = DD_SetVectorElement(ddman, b, vars, 2, 1, 2);
	b = DD_SetVectorElement(ddman, b, vars, 2, 2, 4);
	b = DD_SetVectorElement(ddman, b, vars, 2, 3, 6);
	printf("\na: ");
	DD_PrintVector(ddman, a, vars, 2);
	printf("b: ");
	DD_PrintVector(ddman, b, vars, 2);

	// apply
	Cudd_Ref(a);
	Cudd_Ref(b);
	c = DD_Apply(ddman, APPLY_PLUS, a, b);
	printf("\nApply(+, a, b)\n");
	DD_PrintVector(ddman, c, vars, 2);
	Cudd_RecursiveDeref(ddman, c);
	Cudd_Ref(a);
	Cudd_Ref(b);
	c = DD_Apply(ddman, APPLY_MINUS, a, b);
	printf("\nApply(-, a, b)\n");
	DD_PrintVector(ddman, c, vars, 2);
	Cudd_RecursiveDeref(ddman, c);
	Cudd_Ref(a);
	Cudd_Ref(b);
	c = DD_Apply(ddman, APPLY_TIMES, a, b);
	printf("\nApply(*, a, b)\n");
	DD_PrintVector(ddman, c, vars, 2);
	Cudd_RecursiveDeref(ddman, c);
	Cudd_Ref(a);
	Cudd_Ref(b);
	c = DD_Apply(ddman, APPLY_DIVIDE, a, b);
	printf("\nApply(/, a, b)\n");
	DD_PrintVector(ddman, c, vars, 2);
	Cudd_RecursiveDeref(ddman, c);

	Cudd_RecursiveDeref(ddman, a);
	Cudd_RecursiveDeref(ddman, b);

	// dereference variables
	Cudd_RecursiveDeref(ddman,vars[0]);
	Cudd_RecursiveDeref(ddman,vars[1]);

	// close down cudd
	DD_CloseDownCUDD(ddman);
	printf("\n");
}
