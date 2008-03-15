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

package parser.ast;

import parser.visitor.*;
import prism.PrismLangException;

public class PathExpressionLogical extends PathExpression
{
	// Operator constants
	public static final int IMPLIES = 1;
	public static final int OR = 2;
	public static final int AND = 3;
	public static final int NOT = 4;
	// Operator symbols
	public static final String opSymbols[] = { "", "=>", "|", "&", "!" };
	
	// Operator
	protected int op = 0;
	// Up to two operands (either may be null)
	protected PathExpression operand1 = null; // LHS of operator
	protected PathExpression operand2 = null; // RHS of operator
	
	// Constructors
	
	public PathExpressionLogical()
	{
	}
	
	public PathExpressionLogical(int op, PathExpression operand1, PathExpression operand2)
	{
		this.op = op;
		this.operand1 = operand1;
		this.operand2 = operand2;
	}
	
	// Set methods
	
	public void setOperator(int i)
	{
		op = i;
	}
	
	public void setOperand1(PathExpression e1)
	{
		operand1 = e1;
	}
	
	public void setOperand2(PathExpression e2)
	{
		operand2 = e2;
	}
	
	public int getNumOperands()
	{
		if (operand1 == null) return 0;
		else return (operand2 == null) ? 1 : 2;
	}
	
	// Get methods
	
	public int getOperator()
	{
		return op;
	}
	
	public String getOperatorSymbol()
	{
		return opSymbols[op];
	}
	
	public PathExpression getOperand1()
	{
		return operand1;
	}
	
	public PathExpression getOperand2()
	{
		return operand2;
	}
	
	// Methods required for ASTElement:
	
	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}
	
	/**
	 * Convert to string.
	 */
	public String toString()
	{
		String s = "";
		if (operand1 != null) s += operand1 + " ";
		s += opSymbols[op];
		if (operand2 != null) s += " " + operand2;
		return s;
	}

	/**
	 * Perform a deep copy.
	 */
	public PathExpressionLogical deepCopy()
	{
		PathExpressionLogical expr = new PathExpressionLogical();
		expr.setOperator(op);
		if (operand1 != null) expr.setOperand1(operand1.deepCopy());
		if (operand2 != null) expr.setOperand2(operand1.deepCopy());
		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}
}

//------------------------------------------------------------------------------
