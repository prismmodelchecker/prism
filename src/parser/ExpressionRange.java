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

public class ExpressionRange extends Expression
{
	// this class stores expressions of the form:
	// 
	//   a = b..c,d,e,f..g
	//
	// where a-g are expressions. this is shorthand for:
	//
	//   (a>=b & a<=c) | (a=d) | (a=e) & (a>=f & a<=g)
	//
	// = can also be != which negates the above logic.
	
	private Expression operand;
	private String relOp; // "=" or "!="
	private Vector rangeLows;
	private Vector rangeHighs;
	
	// constructor
	
	public ExpressionRange()
	{
		rangeLows = new Vector();
		rangeHighs = new Vector();
	}
	
	// set methods
	
	public void setOperand(Expression e)
	{
		operand = e;
	}
			
	public void setRelOp(String r)
	{
		relOp = r;
	}
	
	public void addRangeOperand(Expression e)
	{
		rangeLows.addElement(e);
		rangeHighs.addElement(e);
	}
		
	public void addRangeOperandPair(Expression e1, Expression e2)
	{
		rangeLows.addElement(e1);
		rangeHighs.addElement(e2);
	}
		
	// get methods
	
	public Expression getOperand()
	{
		return operand;
	}
		
	public String getRelOp()
	{
		return relOp;
	}
		
	public int getNumRangeOperands()
	{
		return rangeLows.size();
	}
	
	public int getRangeOperandSize(int i)
	{
		return (rangeLows.elementAt(i) == rangeHighs.elementAt(i)) ? 1 : 2;
	}
		
	public Expression getRangeOperandLow(int i)
	{
		return (Expression)rangeLows.elementAt(i);
	}
		
	public Expression getRangeOperandHigh(int i)
	{
		return (Expression)rangeHighs.elementAt(i);
	}
		
	// find all formulas (i.e. locate idents which are formulas)
	
	public Expression findAllFormulas(FormulaList formulaList) throws PrismException
	{
		int i, n;
		
		setOperand(getOperand().findAllFormulas(formulaList));
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				rangeLows.setElementAt(getRangeOperandLow(i).findAllFormulas(formulaList), i);
				rangeHighs.setElementAt(rangeLows.elementAt(i), i);
			}
			else {
				rangeLows.setElementAt(getRangeOperandLow(i).findAllFormulas(formulaList), i);
				rangeHighs.setElementAt(getRangeOperandHigh(i).findAllFormulas(formulaList), i);
			}
		}
		
		return this;
	}
	
	// get all formulas (put into vector)
	
	public void getAllFormulas(Vector v) throws PrismException
	{
		int i, n;
		
		getOperand().getAllFormulas(v);
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				getRangeOperandLow(i).getAllFormulas(v);
			}
			else {
				getRangeOperandLow(i).getAllFormulas(v);
				getRangeOperandHigh(i).getAllFormulas(v);
			}
		}
	}

	// expand any formulas
	
	public Expression expandFormulas(FormulaList formulaList) throws PrismException
	{
		int i, n;
		
		setOperand(getOperand().expandFormulas(formulaList));
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				rangeLows.setElementAt(getRangeOperandLow(i).expandFormulas(formulaList), i);
				rangeHighs.setElementAt(rangeLows.elementAt(i), i);
			}
			else {
				rangeLows.setElementAt(getRangeOperandLow(i).expandFormulas(formulaList), i);
				rangeHighs.setElementAt(getRangeOperandHigh(i).expandFormulas(formulaList), i);
			}
		}
		
		return this;
	}
	
	// create and return a new expression by renaming

	public Expression rename(RenamedModule rm) throws PrismException
	{
		int i, n;
		ExpressionRange e;
		
		e = new ExpressionRange();
		e.setOperand(getOperand().rename(rm));
		e.setRelOp(relOp);
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				e.addRangeOperand(getRangeOperandLow(i).rename(rm));
			}
			else {
				e.addRangeOperandPair(getRangeOperandLow(i).rename(rm), getRangeOperandHigh(i).rename(rm));
			}
		}
		
		return e;
	}
	
	// find all constants (i.e. locate idents which are constants)
	
	public Expression findAllConstants(ConstantList constantList) throws PrismException
	{
		int i, n;
		
		setOperand(getOperand().findAllConstants(constantList));
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				rangeLows.setElementAt(getRangeOperandLow(i).findAllConstants(constantList), i);
				rangeHighs.setElementAt(rangeLows.elementAt(i), i);
			}
			else {
				rangeLows.setElementAt(getRangeOperandLow(i).findAllConstants(constantList), i);
				rangeHighs.setElementAt(getRangeOperandHigh(i).findAllConstants(constantList), i);
			}
		}
		
		return this;
	}
	
	// get all constants (put into vector)
	
	public void getAllConstants(Vector v) throws PrismException
	{
		int i, n;
		
		getOperand().getAllConstants(v);
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				getRangeOperandLow(i).getAllConstants(v);
			}
			else {
				getRangeOperandLow(i).getAllConstants(v);
				getRangeOperandHigh(i).getAllConstants(v);
			}
		}
	}

	// create and return a new expression by expanding constants

	public Expression expandConstants(ConstantList constantList) throws PrismException
	{
		int i, n;
		ExpressionRange e;
		
		e = new ExpressionRange();
		e.setOperand(getOperand().expandConstants(constantList));
		e.setRelOp(relOp);
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				e.addRangeOperand(getRangeOperandLow(i).expandConstants(constantList));
			}
			else {
				e.addRangeOperandPair(getRangeOperandLow(i).expandConstants(constantList), getRangeOperandHigh(i).expandConstants(constantList));
			}
		}
		
		return e;
	}

	// find all variables (i.e. locate idents which are variables)
	
	public Expression findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		int i, n;
		
		setOperand(getOperand().findAllVars(varIdents, varTypes));
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				rangeLows.setElementAt(getRangeOperandLow(i).findAllVars(varIdents, varTypes), i);
				rangeHighs.setElementAt(rangeLows.elementAt(i), i);
			}
			else {
				rangeLows.setElementAt(getRangeOperandLow(i).findAllVars(varIdents, varTypes), i);
				rangeHighs.setElementAt(getRangeOperandHigh(i).findAllVars(varIdents, varTypes), i);
			}
		}
		
		return this;
	}
	
	// get all vars (put into vector)
	
	public void getAllVars(Vector v) throws PrismException
	{
		int i, n;
		
		getOperand().getAllVars(v);
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				getRangeOperandLow(i).getAllVars(v);
			}
			else {
				getRangeOperandLow(i).getAllVars(v);
				getRangeOperandHigh(i).getAllVars(v);
			}
		}
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		int i, n;
		
		// make sure that all operands are ints or doubles
		if (getOperand().getType() == Expression.BOOLEAN) {
			throw new PrismException("Type error in expression \"" + toString() + "\"");
		}
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				if (getRangeOperandLow(i).getType() == Expression.BOOLEAN) {
					throw new PrismException("Type error in expression \"" + toString() + "\"");
				}
			}
			else {
				if (getRangeOperandLow(i).getType() == Expression.BOOLEAN) {
					throw new PrismException("Type error in expression \"" + toString() + "\"");
				}
				if (getRangeOperandHigh(i).getType() == Expression.BOOLEAN) {
					throw new PrismException("Type error in expression \"" + toString() + "\"");
				}
			}
		}
		
		// type will always be a boolean
		setType(Expression.BOOLEAN);
	}

	// is the range part all integers?
	
	public boolean rangeIsAllInts() throws PrismException
	{
		int i, n;
		
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				if (getRangeOperandLow(i).getType() != Expression.INT) return false;
			}
			else {
				if (getRangeOperandLow(i).getType() != Expression.INT) return false;
				if (getRangeOperandHigh(i).getType() != Expression.INT) return false;
			}
		}
		
		return true;
	}
	
	// is expression constant?
	
	public boolean isConstant()
	{
		int i, n;
		
		if (!getOperand().isConstant()) return false;
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				if (!getRangeOperandLow(i).isConstant()) return false;
			}
			else {
				if (!getRangeOperandLow(i).isConstant()) return false;
				if (!getRangeOperandHigh(i).isConstant()) return false;
			}
		}
		
		return true;
	}
	
	// is the range part constant?
	
	public boolean rangeIsConstant()
	{
		int i, n;
		
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				if (!getRangeOperandLow(i).isConstant()) return false;
			}
			else {
				if (!getRangeOperandLow(i).isConstant()) return false;
				if (!getRangeOperandHigh(i).isConstant()) return false;
			}
		}
		
		return true;
	}
	
	// check expression is ok
	
	public void check() throws PrismException
	{
		int i, n;
		
		getOperand().check();
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				getRangeOperandLow(i).check();
			}
			else {
				getRangeOperandLow(i).check();
				getRangeOperandHigh(i).check();
			}
		}
	}
	
	// evaluate
	
	public Object evaluate(Values constantValues, Values varValues) throws PrismException
	{
		int i, j, k, n;
		double op, lo[], hi[];
		boolean b;
		Object o;
		
		// convert everything to doubles before the comparison
		o = operand.evaluate(constantValues, varValues);
		if (o instanceof Double) {
			op = ((Double)o).doubleValue();
		}
		else {
			op = ((Integer)o).intValue();
		}
		n = getNumRangeOperands();
		lo = new double[n];
		hi = new double[n];
		for (i = 0; i < n; i++) {
			o = getRangeOperandLow(i).evaluate(constantValues, varValues);
			if (o instanceof Double) {
				lo[i] = ((Double)o).doubleValue();
			}
			else {
				lo[i] = ((Integer)o).intValue();
			}
			if (getRangeOperandSize(i) == 1) {
				hi[i] = lo[i];
			}
			else {
				o = getRangeOperandHigh(i).evaluate(constantValues, varValues);
				if (o instanceof Double) {
					hi[i] = ((Double)o).doubleValue();
				}
				else {
					hi[i] = ((Integer)o).intValue();
				}
			}
		}
		
		// now work out if operand is in the range or not		
		n = getNumRangeOperands();
		b = false;
		for (i = 0; i < n; i++) {
			if (op >= lo[i] && op <= hi[i]) {
				b = true;
				break;
			}
		}
		
		// flip if !=
		if (relOp.equals("!=")) b = !b;
		
		return new Boolean(b);
	}
	
	// convert to string
		
	public String toString()
	{
		String s = "";
		int i, n;
		
		s += getOperand() + relOp;
		n = getNumRangeOperands();
		if (n > 0) {
			s += getRangeOperandLow(0);
			if (getRangeOperandSize(0) == 2) s += ".." + getRangeOperandHigh(0);
		}
		for (i = 1; i < n; i++) {
			s += "," + getRangeOperandLow(i);
			if (getRangeOperandSize(i) == 2) s += ".." + getRangeOperandHigh(i);
		}
		
		return s;
	}

	// convert to apmc data structures
	
	public int toApmc(Apmc apmc) throws ApmcException
	{
		Expression e;
		ExpressionOr eOr;
		ExpressionAnd eAnd;
		int i, n;
		
		// convert to equivalent disjunction of conjunctions of relops
		eOr = new ExpressionOr();
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				eOr.addOperand(new ExpressionRelOp(getOperand(), "=", getRangeOperandLow(i)));
			}
			else {
				eAnd = new ExpressionAnd();
				eAnd.addOperand(new ExpressionRelOp(getOperand(), ">=", getRangeOperandLow(i)));
				eAnd.addOperand(new ExpressionRelOp(getOperand(), "<=", getRangeOperandHigh(i)));
				eOr.addOperand(eAnd);
			}
		}
		// but we don't want a disjunction of one thing 
		e = (n == 1) ? eOr.getOperand(0) : eOr;
		// negate if necessary
		if (relOp.equals("!=")) e = new ExpressionNot(e);
		
		// now can use existing toApmc methods...
		return e.toApmc(apmc);
	}

	/**
	 *	Convert and build simulator expression data structure
	 */
	public int toSimulator(SimulatorEngine sim) throws SimulatorException
	{
		Expression e;
		ExpressionOr eOr;
		ExpressionAnd eAnd;
		int i, n;
		
		// convert to equivalent disjunction of conjunctions of relops
		eOr = new ExpressionOr();
		n = getNumRangeOperands();
		for (i = 0; i < n; i++)
		{
			if (getRangeOperandSize(i) == 1)
			{
				eOr.addOperand(new ExpressionRelOp(getOperand(), "=", getRangeOperandLow(i)));
			}
			else
			{
				eAnd = new ExpressionAnd();
				eAnd.addOperand(new ExpressionRelOp(getOperand(), ">=", getRangeOperandLow(i)));
				eAnd.addOperand(new ExpressionRelOp(getOperand(), "<=", getRangeOperandHigh(i)));
				eOr.addOperand(eAnd);
			}
		}
		// but we don't want a disjunction of one thing 
		e = (n == 1) ? eOr.getOperand(0) : eOr;
		// negate if necessary
		if (relOp.equals("!=")) e = new ExpressionNot(e);
		
		// now can use existing simulator methods...
		return e.toSimulator(sim);
	}

	// get expression tree as string
	
	public String toTreeString(int indent)
	{
		String s;
		int i, n;
		
		s = super.toTreeString(indent);
		s += getOperand().toTreeString(indent+1);
		n = getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (getRangeOperandSize(i) == 1) {
				s += getRangeOperandLow(i).toTreeString(indent+1);
			}
			else {
				s += getRangeOperandLow(i).toTreeString(indent+1);
				s += getRangeOperandHigh(i).toTreeString(indent+1);
			}
		}
		
		return s;
	}
}

//------------------------------------------------------------------------------
