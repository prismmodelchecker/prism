//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

package prism;

import param.BigRational;
import parser.type.*;

/**
 * Class to manage values for a an undefined constant.
 */
public class DefinedConstant
{
	/* When iterating over doubles it is not unlikely that the value
	 * of the iterators are slightly different to the expected value.
	 * If this difference is within DOUBLE_PRECISION_CORRECTION * ds then
	 * we presume that this is caused by the inprecision of doubles.
	 */
	public static final double DOUBLE_PRECISION_CORRECTION = 0.001;
	
	/* Basic info about constant. */
	private String name;
	private Type type;
	/* Definition for constant. */
	private Object low;
	private Object high;
	private Object step;
	private int numSteps;
	/* Storage for a (temporary) value of the constant. */
	private Object value;

	/** Creates a new instance of DefinedConstant
	(which is initially undefined, bar a name and type). */
	
	public DefinedConstant(String n, Type t)
	{
		name = n;
		type = t;
		clear();
	}

	/** Clear any definition of the constant. */
	
	public void clear()
	{
		low = null;
		high = null;
		step = null;
		numSteps = -1;
		value = null;
	}

	/** Check whether the constant is defined. */
	
	public boolean isDefined()
	{
		return (low != null);
	}

	/**
	 * Define the constant (by passing in strings).
	 * <br>
	 * If {@code exact} is true, BigRationals are used for real values,
	 * otherwise double arithmetic is used.
	 */
	public void define(String sl, String sh, String ss, boolean exact) throws PrismException
	{
		if (type instanceof TypeInt) {
			defineInt(sl, sh, ss);
		} else if (type instanceof TypeDouble) {
			if (exact) {
				defineBigRational(sl, sh, ss);
			} else {
				defineDouble(sl, sh, ss);
			}
		} else if (type instanceof TypeBool) { 
			defineBoolean(sl, sh, ss);
		} else {
			throw new PrismException("Unknown type for undefined constant " + name + "");
		}
	}
	
	public void defineInt(String sl, String sh, String ss) throws PrismException
	{
		int i, il, ih, is, ihNew;
		
		// parse low value
		try {
			il = Integer.parseInt(sl);
		}
		catch (NumberFormatException e) {
			throw new PrismException("Value " + sl + " for constant " + name + " is not a valid integer");
		}
		// if no high value given, use low value, default step is 1
		if (sh == null) {
			ih = il;
			is = 1;
		}
		else {
			// parse high value
			try {
				ih = Integer.parseInt(sh);
			}
			catch (NumberFormatException e) {
				throw new PrismException("Value " + sh + " for constant " + name + " is not a valid integer");
			}
			if (ih < il) throw new PrismException("Low value "+il+" for constant " + name + " is higher than the high value "+ih);
			if (ss == null) {
				// default step is 1
				is = 1;
			}
			else {
				// parse step
				try {
					is = Integer.parseInt(ss);
				}
				catch (NumberFormatException e) {
					throw new PrismException("Value " + ss + " for constant " + name + " is not a valid integer");
				}
				if (is == 0) throw new PrismException("Step value for constant " + name + " cannot be zero");
				if (is < 0) throw new PrismException("Step value for constant " + name + " must be positive");
				if (is > ih-il) throw new PrismException("Step value "+is+" for constant " + name + " is bigger than the difference between "+il+" and "+ih);
			}
		}
		// compute num steps
		numSteps = 0;
		ihNew = ih;
		
		for (i = il; i <= ih; i = il + numSteps * is)
		{ 			
			numSteps++; 
			ihNew = i; 
		}	
		
		// store 'actual' value for high
		ih = ihNew;
		// store Object versions
		low = new Integer(il);
		high = new Integer(ih);
		step = new Integer(is);
	}
	
	public void defineDouble(String sl, String sh, String ss) throws PrismException
	{
		double d, dl, dh, ds, dhNew;
		
		// parse low value
		try {
			dl = parseDouble(sl);
			if (!Double.isFinite(dl)) {
				throw new NumberFormatException("Value is not finite");
			}
		}
		catch (NumberFormatException e) {
			throw new PrismException("Value " + sl + " for constant " + name + " is not a valid double");
		}
		// if no high value given, use low value, default step is 1.0
		if (sh == null) {
			dh = dl;
			ds = 1.0;
		}
		else {
			// parse high value
			try {
				dh = parseDouble(sh);
				if (!Double.isFinite(dh)) {
					throw new NumberFormatException("Value is not finite");
				}
			}
			catch (NumberFormatException e) {
				throw new PrismException("Value " + sh + " for constant " + name + " is not a valid double");
			}
			if (dh < dl) throw new PrismException("Low value "+dl+" for constant " + name + " is higher than the high value "+dh);
			if (ss == null) {
				// default step is 1.0
				ds = 1.0;
			}
			else {
				// parse step
				try {
					ds = parseDouble(ss);
					if (!Double.isFinite(ds)) {
						throw new NumberFormatException("Value is not finite");
					}
				}
				catch (NumberFormatException e) {
					throw new PrismException("Value " + ss + " for constant " + name + " is not a valid double");
				}
				if (ds == 0) throw new PrismException("Step value for constant " + name + " cannot be zero");
				if (ds < 0) throw new PrismException("Step value for constant " + name + " must be positive");
				if (ds > dh-dl) throw new PrismException("Step value "+ds+" for constant " + name + " is bigger than the difference between "+dl+" and "+dh);
			}
		}
		// compute num steps
		numSteps = 0;
		dhNew = dh;
		for (d = dl; d <= dh + DOUBLE_PRECISION_CORRECTION * ds; d = dl + numSteps * ds) 
		{ 
			numSteps++; 
			dhNew = d; 
		}
		// store 'actual' value for high
		dh = dhNew;
		// store Object versions
		low = new Double(dl);
		high = new Double(dh);
		step = new Double(ds);
	}

	public void defineBigRational(String sl, String sh, String ss) throws PrismException
	{
		BigRational r, rl, rh, rs;

		// parse low value
		try {
			rl = BigRational.from(sl);
			if (rl.isSpecial()) {
				throw new NumberFormatException("Value is not finite");
			}
		}
		catch (NumberFormatException e) {
			throw new PrismException("Value " + sl + " for constant " + name + " is not a valid rational number");
		}
		// if no high value given, use low value, default step is 1.0
		if (sh == null) {
			rh = rl;
			rs = BigRational.from(1);
		}
		else {
			// parse high value
			try {
				rh = BigRational.from(sh);
				if (rh.isSpecial()) {
					throw new NumberFormatException("Value is not finite");
				}
			}
			catch (NumberFormatException e) {
				throw new PrismException("Value " + sh + " for constant " + name + " is not a valid rational number");
			}
			if (rh.lessThan(rl)) throw new PrismException("Low value "+rl+" for constant " + name + " is higher than the high value "+rh);
			if (ss == null) {
				// default step is 1.0
				rs = BigRational.from(1);
			}
			else {
				// parse step
				try {
					rs = BigRational.from(ss);
					if (rs.isSpecial()) {
						throw new NumberFormatException("Value is not finite");
					}
				}
				catch (NumberFormatException e) {
					throw new PrismException("Value " + ss + " for constant " + name + " is not a valid double");
				}
				if (rs.isZero()) throw new PrismException("Step value for constant " + name + " cannot be zero");
				if (rs.lessThan(BigRational.ZERO)) throw new PrismException("Step value for constant " + name + " must be positive");
				if (rs.greaterThan(rh.subtract(rl))) throw new PrismException("Step value "+rs+" for constant " + name + " is bigger than the difference between "+rl+" and "+rh);
			}
		}
		// compute num steps
		numSteps = 0;
		BigRational rhNew = rh;
		for (r = rl; r.lessThanEquals(rh); r = rl.add(rs.multiply(numSteps)))
		{
			numSteps++;
			rhNew = r;
		}
		// store 'actual' value for high
		rh = rhNew;
		// store
		low = rl;
		high = rh;
		step = rs;
	}

	public void defineBoolean(String sl, String sh, String ss) throws PrismException
	{
		// parse value (low)
		if (sl.equals("true")) low = new Boolean(true);
		else if (sl.equals("false")) low = new Boolean(false);
		else throw new PrismException("Value " + sl + " for constant " + name + " is not a valid Boolean");
		// no high or step allowed for booleans
		if (sh != null) throw new PrismException("Cannot define ranges for Boolean constants");
		if (ss != null) throw new PrismException("Cannot define ranges for Boolean constants");
		// num steps is always 1
		numSteps = 1;
	}

	/** Set the current value.
	(note that "value" is distinct from "definition") */
	
	public void setValue(Object val)
	{
		value = val;
	}

	/** Increment the value, return status of overflow. */
	
	public boolean incr()
	{
		int i;
		int il, ih, is, iv;
		double dl, dh, ds, dv;
		boolean overflow = false;
		
		// int
		if (type instanceof TypeInt) {
			il = ((Integer)low).intValue();
			ih = ((Integer)high).intValue();
			is = ((Integer)step).intValue();
			iv = ((Integer)value).intValue();
			// if possible, increment
			if (iv+is<=ih) {
				value = new Integer(iv+is);
			}
			// otherwise, reset to low value, note overflow
			else {
				value = low;
				overflow = true;
			}
		}
		// double
		else if (type instanceof TypeDouble) {
			if (low instanceof BigRational) {
				BigRational rl = (BigRational) low;
				BigRational rh = (BigRational) high;
				BigRational rs = (BigRational) step;
				BigRational rv = (BigRational) value;
				// if possible, increment
				int index = getValueIndex(value) + 1;
				rv = rl.add(rs.multiply(index));
				if (rv.lessThanEquals(rh)) {
					value = rv;
				}
				// otherwise, reset to low value, note overflow
				else {
					value = low;
					overflow = true;
				}
			} else {
				// double arithmetic
				dl = ((Double)low).doubleValue();
				dh = ((Double)high).doubleValue();
				ds = ((Double)step).doubleValue();
				dv = ((Double)value).doubleValue();
				// if possible, increment
				int index = getValueIndex(value) + 1;
				dv = dl + index * ds;
				if (dv <= dh + DOUBLE_PRECISION_CORRECTION * ds) {
					value = new Double(dv);
				}
				// otherwise, reset to low value, note overflow
				else {
					value = low;
					overflow = true;
				}
			}
		}
		// boolean
		else if (type instanceof TypeBool) { 
			// booleans can't be incremented
			value = low;
			overflow = true;
		}
		
		return overflow;
	}

	/* Get the current value for the object.
	(note that "value" is distinct from "definition") */
	
	public Object getValue()
	{
		return value;
	}

	/* Get the jth value for the constant,
	 * eg. if the constant definition is 3:2:9, the values would be 
	 * [3, 5, 7, 9] and so getValue(1) would return 5. */
	
	public Object getValue(int j)
	{
		int i;
		int il, is, iv;
		double dl, ds, dv;
		
		// int
		if (type instanceof TypeInt) {
			il = ((Integer)low).intValue();
			is = ((Integer)step).intValue();
			iv = il;
			for (i = 0; i < j; i++) iv += is;
			return new Integer(iv);
		}
		// double
		else if (type instanceof TypeDouble) {
			if (low instanceof BigRational) {
				BigRational rl = (BigRational)low;
				BigRational rs = (BigRational)low;
				BigRational rv;
				// rv = rl + j*rs
				rv = rl.add(rs.multiply(j));
				return rv;
			} else {
				dl = ((Double)low).doubleValue();
				ds = ((Double)step).doubleValue();
				dv = dl;
				//for (i = 0; i < j; i++) dv += ds;
				dv = dl + j * ds;
				return new Double(dv);
			}
		} 
		// boolean (case should be redundant)
		else if (type instanceof TypeBool) { 
			if (j == 0) return new Boolean(false);
			else if (j == 1) return new Boolean(true);
		}
		
		// should never get here
		return null;
	}

	/* Get the index of a value of the constant,
	 * eg. if the constant definition is 3:2:9, the values would be 
	 * [3, 5, 7, 9] and so getValueIndex(7) would return 2. */
	
	public int getValueIndex(Object v)
	{
		int il, is, iv;
		double dl, ds, dv;
		
		// int
		if (type instanceof TypeInt) {
			il = ((Integer)low).intValue();
			is = ((Integer)step).intValue();
			iv = ((Integer)v).intValue();
			return (iv-il)/is;
		}
		// double
		else if (type instanceof TypeDouble) {
			if (low instanceof BigRational) {
				BigRational rl = (BigRational) low;
				BigRational rs = (BigRational) step;
				BigRational rv = (BigRational) value;
				BigRational index = (rv.subtract(rl)).divide(rs);
				try {
					return index.toInt();
				} catch (PrismLangException e) {
					throw new IllegalArgumentException("Can not compute value index, out of range: " + e);
				}
			} else {
				dl = ((Double)low).doubleValue();
				ds = ((Double)step).doubleValue();
				dv = ((Double)v).doubleValue();
				return (int)Math.round((dv-dl)/ds);
			}
		} 
		// boolean (case should be redundant)
		else if (type instanceof TypeBool) { 
		} else {
			if (((Boolean)v).booleanValue()) return 1; else return 0;
		}
		
		// should never get here
		return -1;
	}

	// ACCESS METHODS
	
	public String getName() {  return name; }
	
	public Type getType() { return type; }
	
	public Object getLow() { return low; }
	
	public Object getHigh() { return high; }
	
	public Object getStep() { return step; }
	
	public int getNumSteps() { return numSteps; }

	/** Convert to string. */
	/*  Note that the (temorary) value is not included here */
	
	public String toString()
	{
		String s = "";
		
		s += name + "=";
		s += low;
		if (numSteps > 1) s += ":" + step + ":" + high;
		
		return s;
	}

	/** Parse a double value (supports a/b fraction syntax) */
	public static double parseDouble(String s)
	{
		int slashIdx = s.lastIndexOf('/');
		if (slashIdx < 0) {
			return Double.parseDouble(s);
		} else {
			// fraction
			// because we use lastIndexOf, we obtain left-associativity,
			// i.e. a/b/c is interpreted as (a/b)/c
			if (slashIdx == 0 || slashIdx == s.length()-1) {
				throw new NumberFormatException("Illegal fraction syntax");
			}
			double num = parseDouble(s.substring(0, slashIdx));
			double den = parseDouble(s.substring(slashIdx+1));
			return num / den;
		}
	}

	/**
	 * Check that the string can be parsed as a double value
	 * (double, or fraction).
	 */
	public static boolean isValidDouble(String s)
	{
		try {
			parseDouble(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

}
