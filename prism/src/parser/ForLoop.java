//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

package parser;

import java.util.Vector;

import prism.PrismException;

public class ForLoop
{
	// for loop info
	private String lhs;
	private Expression from;
	private Expression to;
	private Expression step;
	// optional storage...
	private int pc;
	private String between;
	
	// constructor
	
	public ForLoop()
	{
		// for loop info
		lhs = null;
		from = null;
		to = null;
		step = null;
		// optional stuff
		pc = 0;
		between = "";
	}

	// set methods
	
	public void setLHS(String s)
	{
		lhs = s;
	}
	
	public void setFrom(Expression e)
	{
		from = e;
	}
	
	public void setTo(Expression e)
	{
		to = e;
	}
	
	public void setStep(Expression e)
	{
		step = e;
	}
	
	public void setPC(int i)
	{
		pc = i;
	}
	
	public void setBetween(String s)
	{
		between = s;
	}

	// get methods
	
	public String getLHS()
	{
		return lhs;
	}
	
	public Expression getFrom()
	{
		return from;
	}
	
	public Expression getTo()
	{
		return to;
	}
	
	public Expression getStep()
	{
		return (step != null) ? step : new ExpressionInt(1);
	}
	
	public int getPC()
	{
		return pc;
	}
	
	public String getBetween()
	{
		return between;
	}

// 	public ForLoop findAllConstants(ConstantList constantList) throws PrismException
// 	public void getAllConstants(Vector v) throws PrismException
// 	public ForLoop expandConstants(ConstantList constantList) throws PrismException

	// find all variables (i.e. locate idents which are variables)
	
	public ForLoop findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		int i, n;
		
		from = from.findAllVars(varIdents, varTypes);
		to = to.findAllVars(varIdents, varTypes);
		if (step != null) step = step.findAllVars(varIdents, varTypes);
		
		return this;
	}


// 	public void getAllVars(Vector v) throws PrismException

	// type check
	
	public void typeCheck() throws PrismException
	{
// 		int i, n;
// 		
// 		// make sure that all operands are ints or doubles
// 		if (getOperand().getType() == Expression.BOOLEAN) {
// 			throw new PrismException("Type error in expression \"" + toString() + "\"");
// 		}
// 		n = getNumRangeOperands();
// 		for (i = 0; i < n; i++) {
// 			if (getRangeOperandSize(i) == 1) {
// 				if (getRangeOperandLow(i).getType() == Expression.BOOLEAN) {
// 					throw new PrismException("Type error in expression \"" + toString() + "\"");
// 				}
// 			}
// 			else {
// 				if (getRangeOperandLow(i).getType() == Expression.BOOLEAN) {
// 					throw new PrismException("Type error in expression \"" + toString() + "\"");
// 				}
// 				if (getRangeOperandHigh(i).getType() == Expression.BOOLEAN) {
// 					throw new PrismException("Type error in expression \"" + toString() + "\"");
// 				}
// 			}
// 		}
// 		
// 		// type will always be a boolean
// 		setType(Expression.BOOLEAN);
	}

	// is the range part all integers?
	
	public boolean rangeIsAllInts() throws PrismException
	{
// 		int i, n;
// 		
// 		n = getNumRangeOperands();
// 		for (i = 0; i < n; i++) {
// 			if (getRangeOperandSize(i) == 1) {
// 				if (getRangeOperandLow(i).getType() != Expression.INT) return false;
// 			}
// 			else {
// 				if (getRangeOperandLow(i).getType() != Expression.INT) return false;
// 				if (getRangeOperandHigh(i).getType() != Expression.INT) return false;
// 			}
// 		}
// 		
		return true;
	}
	
	// is expression constant?
	
	public boolean isConstant()
	{
// 		int i, n;
// 		
// 		if (!getOperand().isConstant()) return false;
// 		n = getNumRangeOperands();
// 		for (i = 0; i < n; i++) {
// 			if (getRangeOperandSize(i) == 1) {
// 				if (!getRangeOperandLow(i).isConstant()) return false;
// 			}
// 			else {
// 				if (!getRangeOperandLow(i).isConstant()) return false;
// 				if (!getRangeOperandHigh(i).isConstant()) return false;
// 			}
// 		}
// 		
		return true;
	}
	
	// is the range part constant?
	
	public boolean rangeIsConstant()
	{
// 		int i, n;
// 		
// 		n = getNumRangeOperands();
// 		for (i = 0; i < n; i++) {
// 			if (getRangeOperandSize(i) == 1) {
// 				if (!getRangeOperandLow(i).isConstant()) return false;
// 			}
// 			else {
// 				if (!getRangeOperandLow(i).isConstant()) return false;
// 				if (!getRangeOperandHigh(i).isConstant()) return false;
// 			}
// 		}
// 		
		return true;
	}

	// check expression is ok
	
	public void check() throws PrismException
	{
		from.check();
		to.check();
		if (step != null) step.check();
	}

	// convert to string
	
	public String toString()
	{
		String s = "";
		int i, n;
		
		s += lhs + "=" + from;
		if (step != null) s += ":" + step;
		s += ":" + to;
		
		return s;
	}
}

//------------------------------------------------------------------------------
