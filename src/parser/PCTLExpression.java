//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker, Andrew Hinton
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

package parser;

import java.util.Vector;

import prism.PrismException;
import apmc.*;
import simulator.*;

public class PCTLExpression extends PCTLFormula
{
	Expression expr;
	
	// constructors
	
	public PCTLExpression()
	{
	}
	
	public PCTLExpression(Expression e)
	{
		expr = e;
	}
	
	// set methods
	
	public void setExpression(Expression e)
	{
		expr = e;
	}
	
	// get methods
	
	public Expression getExpression()
	{
		return expr;
	}
	
	// find all constants (i.e. locate idents which are constants)
	
	public PCTLFormula findAllConstants(ConstantList constantList) throws PrismException
	{
		expr = expr.findAllConstants(constantList);
		
		return this;
	}
	
	// find all variables (i.e. locate idents which are variables)
	
	public PCTLFormula findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		expr = expr.findAllVars(varIdents, varTypes);
		
		return this;
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		// check expression is of type boolean
		if (expr.getType() != Expression.BOOLEAN) {
			throw new PrismException("Expression \"" + expr + "\" in formula must be of type boolean");
		}
		// set type
		setType(Expression.BOOLEAN);
	}
	
	// check everything is ok
	
	public void check() throws PrismException
	{
		// check expression
		expr.check();
	}
	
	// convert to string
		
	public String toString()
	{
		return expr.toString();
	}

	// convert to apmc data structures
	
	public int toApmc(Apmc apmc) throws ApmcException
	{
		return expr.toApmc(apmc);
	}

	/**
	 *	Convert and build simulator data structures
	 */
	public int toSimulator(SimulatorEngine sim ) throws SimulatorException
	{
		return expr.toSimulator(sim);
	}

	// get parse tree as string
	
	public String toTreeString(int indent)
	{
		String s;
		int i, n;
		
		s = super.toTreeString(indent);
		s += expr.toTreeString(indent+1);
		
		return s;
	}
}

//------------------------------------------------------------------------------
