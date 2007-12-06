//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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
import prism.PrismUtils;
import simulator.*;

public class ExpressionFunc extends ExpressionNary
{
	// codes for each function name
	public static final int MIN = 0;
	public static final int MAX = 1;
	public static final int FLOOR = 2;
	public static final int CEIL = 3;
	public static final int POW = 4;
	public static final int MOD = 5;
	public static final int LOG = 6;
	// strings for names
	public static final String names[] = {"min", "max", "floor", "ceil", "pow", "mod", "log"};
	// min/max function arities
	public static final int minArities[] = {2, 2, 1, 1, 2, 2, 2};
	public static final int maxArities[] = {-1, -1, 1, 1, 2, 2, 2};

	// function name
	private String name = "";
	private int code = -1;

	// was function written in old style notation (using built-in keywords)
	// (as opposed to new style, i.e. with "func" keyword)
	private boolean oldStyle;

	// set methods
	
	public void setName(String s)
	{
		int i, n;
		
		// set string
		name = s;
		// determine and set code
		n = names.length;
		code = -1;
		for (i = 0; i < n; i++) {
			if (s.equals(names[i])) {
				code = i;
				break;
			}
		}
	}
	
	public void setOldStyle(boolean b)
	{
		oldStyle = b;
	}
	
	// get methods
	
	public String getName()
	{
		return name;
	}
	
	public int getNameCode()
	{
		return code;
	}
	
	public boolean getOldStyle()
	{
		return oldStyle;
	}

	// create and return a new expression by renaming
	// this includes the function name
	
	public Expression rename(RenamedModule rm) throws PrismException
	{
		int i, n;
		String s;
		ExpressionFunc e;
		
		e = new ExpressionFunc();
		s = rm.getNewName(name);
		s = (s == null) ? name : s;
		e.setName(s);
		e.setOldStyle(oldStyle);
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			e.addOperand(getOperand(i).rename(rm));
		}
		
		return e;
	}

	// create and return a new expression by expanding constants
	
	public Expression expandConstants(ConstantList constantList) throws PrismException
	{
		int i, n;
		ExpressionFunc e;
		
		e = new ExpressionFunc();
		e.setName(getName());
		e.setOldStyle(getOldStyle());
		n = getNumOperands();
		for (i = 0; i < n; i++) {
			e.addOperand(getOperand(i).expandConstants(constantList));
		}
		
		return e;
	}

	// check expression is ok
	
	public void check() throws PrismException
	{
		// check function name is valid
		if (code == -1)
			throw new PrismException("Unrecognised function \"" + name + "\"");
		// check arity
		if (getNumOperands() < minArities[code]) {
			throw new PrismException("Not enough arguments to \"" + name + "\" function in \"" + this + "\"");
		}
		if (maxArities[code] != -1 && getNumOperands() > maxArities[code]) {
			throw new PrismException("Too many arguments to \"" + name + "\" function in \"" + this + "\"");
		}
		// check operands
		super.check();
	}

	// type check
	
	public void typeCheck() throws PrismException
	{
		int i, n, types[];
		
		// check function name is valid
		if (code == -1)
			throw new PrismException("Unrecognised function \"" + name + "\"");
		
		// compute types of operands
		n = getNumOperands();
		types = new int[n];
		for (i = 0; i < n; i++) {
			types[i] = getOperand(i).getType();
		}
		
		// check types of operands are ok
		switch (code) {
			case MIN:
			case MAX:
			case FLOOR:
			case CEIL:
			case POW:
			case LOG:
				// all operands must be ints or doubles
				for (i = 0; i < n; i++) {
					if (types[i] == Expression.BOOLEAN) {
						throw new PrismException("Type error in expression \"" + toString() + "\"");
					}
				}
				break;
			case MOD:
				// all operands must be ints
				for (i = 0; i < n; i++) {
					if (types[i] != Expression.INT) {
						throw new PrismException("Type error in expression \"" + toString() + "\"");
					}
				}
				break;
		}
		
		// determine type of this function
		switch (code) {
			case MIN:
			case MAX:
				// int if all ints, double otherwise
				for (i = 0; i < n; i++) {
					if (types[i] == Expression.DOUBLE) {
						setType(Expression.DOUBLE);
						return;
					}
				}
				setType(Expression.INT);
				break;
				
			case FLOOR:
			case CEIL:
			case MOD:
				// resulting type is always int
				setType(Expression.INT);
				break;
				
			case POW:
			case LOG:
				// resulting type is always double
				setType(Expression.DOUBLE);
				break;
		}
	}

	// evaluate
	
	public Object evaluate(Values constantValues, Values varValues) throws PrismException
	{
		// check function name is valid
		if (code == -1)
			throw new PrismException("Unrecognised function \"" + name + "\"");
		
		// split into cases
		switch (code) {
			case MIN: return evaluateMin(constantValues, varValues);
			case MAX: return evaluateMax(constantValues, varValues);
			case FLOOR: return evaluateFloor(constantValues, varValues);
			case CEIL: return evaluateCeil(constantValues, varValues);
			case POW: return evaluatePow(constantValues, varValues);
			case MOD: return evaluateMod(constantValues, varValues);
			case LOG: return evaluateLog(constantValues, varValues);
			default: return null;
		}
	}
	
	private Object evaluateMin(Values constantValues, Values varValues) throws PrismException
	{
		int i, j, n, iMin;
		double d, dMin;
		boolean allInts;
		Object o[];
		
		// evaluate operands
		n = getNumOperands();
		o = new Object[n];
		for (i = 0; i < n; i++) {
			o[i] = getOperand(i).evaluate(constantValues, varValues);
		}
		
		// see if they're all ints
		allInts = true;
		for (i = 0; i < n; i++) {
			if (!(o[i] instanceof Integer)) {
				allInts = false;
			}
		}
		
		// case where all are ints
		if (allInts) {
			iMin = ((Integer)o[0]).intValue();
			n = getNumOperands();
			for (i = 1; i < n; i++) {
				j = ((Integer)o[i]).intValue();
				iMin = (j < iMin) ? j : iMin;
			}
			return new Integer(iMin);
		}
		// case where at least one is a double
		else {
			if (o[0] instanceof Double) {
				dMin = ((Double)o[0]).doubleValue();
			}
			else {
				dMin = ((Integer)o[0]).intValue();
			}
			n = getNumOperands();
			for (i = 1; i < n; i++) {
				if (o[i] instanceof Double) {
					d = ((Double)o[i]).doubleValue();
				}
				else {
					d = ((Integer)o[i]).intValue();
				}
				dMin = (d < dMin) ? d : dMin;
			}
			return new Double(dMin);
		}
	}
	
	public Object evaluateMax(Values constantValues, Values varValues) throws PrismException
	{
		int i, j, n, iMax;
		double d, dMax;
		boolean allInts;
		Object o[];
		
		// evaluate operands
		n = getNumOperands();
		o = new Object[n];
		for (i = 0; i < n; i++) {
			o[i] = getOperand(i).evaluate(constantValues, varValues);
		}
		
		// see if they're all ints
		allInts = true;
		for (i = 0; i < n; i++) {
			if (!(o[i] instanceof Integer)) {
				allInts = false;
			}
		}
		
		// case where all are ints
		if (allInts) {
			iMax = ((Integer)o[0]).intValue();
			n = getNumOperands();
			for (i = 1; i < n; i++) {
				j = ((Integer)o[i]).intValue();
				iMax = (j > iMax) ? j : iMax;
			}
			return new Integer(iMax);
		}
		// case where at least one is a double
		else {
			if (o[0] instanceof Double) {
				dMax = ((Double)o[0]).doubleValue();
			}
			else {
				dMax = ((Integer)o[0]).intValue();
			}
			n = getNumOperands();
			for (i = 1; i < n; i++) {
				if (o[i] instanceof Double) {
					d = ((Double)o[i]).doubleValue();
				}
				else {
					d = ((Integer)o[i]).intValue();
				}
				dMax = (d > dMax) ? d : dMax;
			}
			return new Double(dMax);
		}
	}
	
	public Object evaluateFloor(Values constantValues, Values varValues) throws PrismException
	{
		return new Integer((int)Math.floor(getOperand(0).evaluateDouble(constantValues, varValues)));
	}
	
	public Object evaluateCeil(Values constantValues, Values varValues) throws PrismException
	{
		return new Integer((int)Math.ceil(getOperand(0).evaluateDouble(constantValues, varValues)));
	}
	
	public Object evaluatePow(Values constantValues, Values varValues) throws PrismException
	{
		return new Double(Math.pow(getOperand(0).evaluateDouble(constantValues, varValues), getOperand(1).evaluateDouble(constantValues, varValues)));
	}
	
	public Object evaluateMod(Values constantValues, Values varValues) throws PrismException
	{
		int i = ((Integer)getOperand(0).evaluate(constantValues, varValues)).intValue();
		int j = ((Integer)getOperand(1).evaluate(constantValues, varValues)).intValue();
		if (j == 0) throw new PrismException("Attempt to compute modulo zero in expression \"" + toString() + "\"");
		return new Integer(i % j);
		
// 		Object o1, o2;
// 		
// 		// evaluate operands
// 		o1 = getOperand(0).evaluate(constantValues, varValues);
// 		o2 = getOperand(1).evaluate(constantValues, varValues);
// 		
// 		// if both are ints
// 		if (o1 instanceof Integer && o2 instanceof Integer) {
// 			return new Integer(((Integer)o1).intValue() % ((Integer)o2).intValue());
// 		}
// 		// if either is a double
// 		else {
// 			return new Double(((Double)o1).doubleValue() % ((Double)o2).doubleValue());
// 		}
	}

	public Object evaluateLog(Values constantValues, Values varValues) throws PrismException
	{
		double x, b;
		
		x = getOperand(0).evaluateDouble(constantValues, varValues);
		b = getOperand(1).evaluateDouble(constantValues, varValues);
		return new Double(PrismUtils.log(x, b));
	}
	
	/**
	 *	Convert and build simulator expression data structure
	 */
	public long toSimulator(SimulatorEngine sim) throws SimulatorException
	{
		try {
			switch(code)
			{
				case MIN:
				{
					int theType = getType();
					int n = getNumOperands();
					if( n < 1 )
						throw new SimulatorException("Expression \"" + toString() + "\" has zero operands");
					long[] exprs = new long[n];
					
					//Collect operands pointers in an array
					for (int i = 0; i < n; i++) 
						exprs[i] = getOperand(i).toSimulator(sim);
					//This should be sufficient, providing type
					if(theType == Expression.DOUBLE)	//checking has been performed
						return SimulatorEngine.createRealMin(exprs);
					else
						return SimulatorEngine.createNormalMin(exprs);
				}
				
				case MAX:
				{
					int theType = getType();
					int n = getNumOperands();
					if( n < 1 )
						throw new SimulatorException("Expression \"" + toString() + "\" has zero operands");
					long[] exprs = new long[n];
					
					//Collect operands pointers in an array
					for (int i = 0; i < n; i++) 
						exprs[i] = getOperand(i).toSimulator(sim);
					//This should be sufficient, providing type
					if(theType == Expression.DOUBLE)			//checking has been performed
						return SimulatorEngine.createRealMax(exprs);
					else
						return SimulatorEngine.createNormalMax(exprs);
				}
				
				case FLOOR:
				{
					return SimulatorEngine.createFloor(getOperand(0).toSimulator(sim));
				}
				
				case CEIL:
				{
					return SimulatorEngine.createCeil(getOperand(0).toSimulator(sim));
				}
				
				case POW:
				{
					int theType = getType();
					if(theType == Expression.DOUBLE)
					{
						return SimulatorEngine.createRealPow(getOperand(0).toSimulator(sim), getOperand(1).toSimulator(sim));
					}
					else
					{
						return SimulatorEngine.createNormalPow(getOperand(0).toSimulator(sim), getOperand(1).toSimulator(sim));
					}
				}
				
				case MOD:
				{
					return SimulatorEngine.createMod(getOperand(0).toSimulator(sim), getOperand(1).toSimulator(sim));
				}
				
				case LOG:
				{
					throw new SimulatorException("The log function is not yet supported in PRISM");
				}
				
				default:
					throw new SimulatorException("Unrecognised function \"" + name + "\"");
			}
		}
		catch(PrismException e)
		{
			throw new SimulatorException(e.getMessage());
		}
	}

	// convert to string
	
	public String toString()
	{
		int i , n;
		String s = "";
		
		if (oldStyle) s += name + "(";
		else s += "func(" + name + ",";
		
		n = operands.size();
		for (i = 0; i < n-1; i++) {
			s = s + getOperand(i) + ",";
		}
		if (n > 0) {
			s = s + getOperand(n-1);
		}
		s += ")";
		
		return s;
	}
}

//------------------------------------------------------------------------------
