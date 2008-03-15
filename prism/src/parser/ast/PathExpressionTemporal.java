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

public class PathExpressionTemporal extends PathExpression
{
	// Operator constants
	public static final int P_X = 1; // Next (for P operator)
	public static final int P_U = 2; // Until (for P operator)
	public static final int P_F = 3; // Future (for P operator)
	public static final int P_G = 4; // Globally (for P operator)
	public static final int R_C = 11; // Cumulative (for R operator)
	public static final int R_I = 12; // Instantaneous (for R operator)
	public static final int R_F = 13; // Reachability (for R operator)
	public static final int R_S = 14; // Steady-state (for R operator)
	// Operator symbols
	public static final String opSymbols[] = { "",
		"X", "U", "F", "G", "", "", "", "", "", "",
		"C", "I", "F", "S"
	};
	
	// Operator
	protected int op = 0;
	// Up to two operands (either may be null)
	protected PathExpression operand1 = null; // LHS of operator
	protected PathExpression operand2 = null; // RHS of operator
	// Optional (time) bounds
	protected Expression lBound = null; // None if null, i.e. zero
	protected Expression uBound = null; // None if null, i.e. infinity
	
	// Constructors
	
	public PathExpressionTemporal()
	{
	}
	
	public PathExpressionTemporal(int op, PathExpression operand1, PathExpression operand2)
	{
		this.op = op;
		this.operand1 = operand1;
		this.operand2 = operand2;
	}
	
	public PathExpressionTemporal(int op, PathExpression operand1, PathExpression operand2, Expression lBound, Expression uBound)
	{
		this.op = op;
		this.operand1 = operand1;
		this.operand2 = operand2;
		this.lBound = lBound;
		this.uBound = uBound;
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
	
	public void setLowerBound(Expression e)
	{
		lBound = e;
	}

	public void setUpperBound(Expression e)
	{
		uBound = e;
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
	
	public boolean hasBounds()
	{
		return lBound!=null || uBound!=null;
	}

	public Expression getLowerBound()
	{
		return lBound;
	}

	public Expression getUpperBound()
	{
		return uBound;
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
		if (lBound == null) {
			if (uBound != null) {
				if (op != R_I) s += "<=" + uBound;
				else s += "=" + uBound;
			}
		}
		else {
			if (uBound == null) {
				s += ">=" + lBound;
			} else {
				s += "[" + lBound + "," + uBound + "]";
			}
		}
		if (operand2 != null) s += " " + operand2;
		return s;
	}

	/**
	 * Perform a deep copy.
	 */
	public PathExpression deepCopy()
	{
		PathExpressionTemporal expr = new PathExpressionTemporal();
		expr.setOperator(op);
		if (operand1 != null) expr.setOperand1(operand1.deepCopy());
		if (operand2 != null) expr.setOperand2(operand1.deepCopy());
		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}
	
	// Other useful methods
	
	/**
	 * Convert (P operator) path formula to until form
	 * (using standard equivalences: F a=true U a, G a=!(true U!a)
	 */
	public PathExpression convertToUntilForm() throws PrismLangException
	{
		PathExpression op1, op2, ret = null;
		if (operand1 != null && !(operand1 instanceof PathExpressionExpr)) {
			throw new PrismLangException("Cannot convert "+getOperatorSymbol()+" to until form");
		}
		if (operand2 != null && !(operand2 instanceof PathExpressionExpr)) {
			throw new PrismLangException("Cannot convert "+getOperatorSymbol()+" to until form");
		}
		switch (op) {
		case P_U:
			ret = this;
			break;
		case P_F:
			op1 =  new PathExpressionExpr(Expression.True());
			ret = new PathExpressionTemporal(P_U, op1, operand2, lBound, uBound);
			break;
		case P_G:
			op1 = new PathExpressionExpr(Expression.True());
			op2 = new PathExpressionExpr(Expression.Not(((PathExpressionExpr)operand2).getExpression()));
			ret = new PathExpressionTemporal(P_U, op1, op2, lBound, uBound);
			ret = new PathExpressionLogical(PathExpressionLogical.NOT, null, ret);
			break;
		default:
			throw new PrismLangException("Cannot convert "+getOperatorSymbol()+" to until form");
		}
		return ret;
	}
}

//------------------------------------------------------------------------------
