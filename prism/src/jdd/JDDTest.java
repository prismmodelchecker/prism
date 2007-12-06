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

package jdd;

public class JDDTest
{
	public static void main(String[] args)
	{
		JDDNode a, b, c;
		JDDVars vars;		
		
		System.out.println("\nTest program for JDD\n====================");
		
		// initialise cudd
		JDD.InitialiseCUDD();
		
		// set up some variables
		vars = new JDDVars();
		vars.addVar(JDD.Var(0));
		vars.addVar(JDD.Var(2));
		
		// blank dd
		a = JDD.Create();
		System.out.println("\nCreate()");
		JDD.PrintVector(a, vars);
		JDD.Deref(a);
		
		// constant
		a = JDD.Constant(1.5);
		System.out.println("\nConstant(1.5)");
		JDD.PrintVector(a, vars);
		JDD.Deref(a);
		
		// plus infinity
		a = JDD.PlusInfinity();
		System.out.println("\nPlusInfinity()");
		JDD.PrintVector(a, vars);
		JDD.Deref(a);
		
		// minus infinity
		a = JDD.MinusInfinity();
		System.out.println("\nMinusInfinity()");
		JDD.PrintVector(a, vars);
		JDD.Deref(a);
		
		// var
		a = JDD.Var(0);
		System.out.println("\nVar(0)");
		JDD.PrintVector(a, vars);
		JDD.Deref(a);
				
		a = JDD.Var(0);
		b = JDD.Var(2);
		System.out.print("\na: ");
		JDD.PrintVector(a, vars);
		System.out.print("b: ");
		JDD.PrintVector(b, vars);

		// not
		JDD.Ref(a);
		c = JDD.Not(a);
		System.out.println("\nNot(a)");
		JDD.PrintVector(c, vars);
		JDD.Deref(c);
		
		// or
		JDD.Ref(a);
		JDD.Ref(b);
		c = JDD.Or(a, b);
		System.out.println("\nOr(a, b)");
		JDD.PrintVector(c, vars);
		JDD.Deref(c);
		
		// and
		JDD.Ref(a);
		JDD.Ref(b);
		c = JDD.And(a, b);
		System.out.println("\nAnd(a, b)");
		JDD.PrintVector(c, vars);
		JDD.Deref(c);
		
		// xor
		JDD.Ref(a);
		JDD.Ref(b);
		c = JDD.Xor(a, b);
		System.out.println("\nXor(a, b)");
		JDD.PrintVector(c, vars);
		JDD.Deref(c);
		
		// implies
		JDD.Ref(a);
		JDD.Ref(b);
		c = JDD.Implies(a, b);
		System.out.println("\nImplies(a, b)");
		JDD.PrintVector(c, vars);
		JDD.Deref(c);
		
		JDD.Deref(a);
		JDD.Deref(b);
		
		a = JDD.Create();
		a = JDD.SetVectorElement(a, vars, 0, 1);
		a = JDD.SetVectorElement(a, vars, 1, 2);
		a = JDD.SetVectorElement(a, vars, 2, 3);
		a = JDD.SetVectorElement(a, vars, 3, 4);
		b = JDD.Create();
		b = JDD.SetVectorElement(b, vars, 0, 0);
		b = JDD.SetVectorElement(b, vars, 1, 2);
		b = JDD.SetVectorElement(b, vars, 2, 4);
		b = JDD.SetVectorElement(b, vars, 3, 6);
		System.out.print("\na: ");
		JDD.PrintVector(a, vars);
		System.out.print("b: ");
		JDD.PrintVector(b, vars);
		
		// apply
		JDD.Ref(a);
		JDD.Ref(b);
		c = JDD.Apply(JDD.PLUS, a, b);
		System.out.println("\nApply(+, a, b)");
		JDD.PrintVector(c, vars);
		JDD.Deref(c);
		JDD.Ref(a);
		JDD.Ref(b);
		c = JDD.Apply(JDD.MINUS, a, b);
		System.out.println("\nApply(-, a, b)");
		JDD.PrintVector(c, vars);
		JDD.Deref(c);
		JDD.Ref(a);
		JDD.Ref(b);
		c = JDD.Apply(JDD.TIMES, a, b);
		System.out.println("\nApply(*, a, b)");
		JDD.PrintVector(c, vars);
		JDD.Deref(c);
		JDD.Ref(a);
		JDD.Ref(b);
		c = JDD.Apply(JDD.DIVIDE, a, b);
		System.out.println("\nApply(/, a, b)");
		JDD.PrintVector(c, vars);
		JDD.Deref(c);
		
		
		JDD.Deref(a);
		JDD.Deref(b);

		// dereference variables
		vars.derefAll();
		
		// close down cudd
		JDD.CloseDownCUDD();
		System.out.println();
	}
}

//------------------------------------------------------------------------------
