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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import param.BigRational;
import parser.type.*;

/**
 * Class to manage values for a an undefined constant.
 */
public abstract class DefinedConstant<T>
{
	/* When iterating over doubles it is not unlikely that the value
	 * of the iterators are slightly different to the expected value.
	 * If this difference is within DOUBLE_PRECISION_CORRECTION * ds then
	 * we presume that this is caused by the inprecision of doubles.
	 */
	public static final double DOUBLE_PRECISION_CORRECTION = 0.001;

	/* Basic info about constant. */
	protected final String name;
	protected final Type type;
	/* Definition for constant. */
	protected final T low;
	protected final T high;
	protected final T step;
	protected final int numSteps;
	/* Storage for a (temporary) value of the constant. */
	protected T value;

	/** Creates a new instance of DefinedConstant
	(which is initially undefined, bar a name and type). */
	
	private DefinedConstant(String name, Type type, T low, T high, T step, int numSteps)
	{
		this.name = name;
		this.type = type;
		this.low = low;
		this.high = high;
		this.step = step;
		this.numSteps = numSteps;
	}

	/** Check whether the constant is defined. */
	public boolean isDefined()
	{
		return (low != null);
	}

	/**
	 * Clear the defined value (and low/high/start) for this constant.
	 * The cleared version is returned as a new DefinedConstant object.
	 */
	public Undefined clear()
	{
		return new Undefined(name, type);
	}


	/**
	 * Define the constant (by passing in strings).
	 * The defined version is returned as a new DefinedConstant object.
	 * <br>
	 * If {@code exact} is true, BigRationals are used for real values,
	 * otherwise double arithmetic is used.
	 */
	public DefinedConstant<?> define(String sl, String sh, String ss, boolean exact) throws PrismException
	{
		if (type instanceof TypeInt) {
			return defineInt(name, sl, sh, ss);
		}
		if (type instanceof TypeDouble) {
			return exact ? defineBigRational(name, sl, sh, ss) : defineDouble(name, sl, sh, ss);
		}
		if (type instanceof TypeBool) { 
			return defineBoolean(name, sl, sh, ss);
		}
		throw new PrismException("Unknown type for undefined constant " + name + "");
	}

	public static DefinedInteger defineInt(String name, String sl, String sh, String ss) throws PrismException
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
		} else {
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
			} else {
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
		int numSteps = 0;
		ihNew = ih;
		
		for (i = il; i <= ih; i = il + numSteps * is) {
			numSteps++; 
			ihNew = i; 
		}
		// store 'actual' value for high
		return new DefinedInteger(name, il, ihNew, is, numSteps);
	}

	public static DefinedDouble defineDouble(String name, String sl, String sh, String ss) throws PrismException
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
		} else {
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
			} else {
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
		int numSteps = 0;
		dhNew = dh;
		for (d = dl; d <= dh + DOUBLE_PRECISION_CORRECTION * ds; d = dl + numSteps * ds) {
			numSteps++; 
			dhNew = d; 
		}
		// store 'actual' value for high
		return new DefinedDouble(name, dl, dhNew, ds, numSteps);
	}

	public static DefinedBigRational defineBigRational(String name, String sl, String sh, String ss) throws PrismException
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
		} else {
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
			} else {
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
		int numSteps = 0;
		BigRational rhNew = rh;
		for (r = rl; r.lessThanEquals(rh); r = rl.add(rs.multiply(numSteps))) {
			numSteps++;
			rhNew = r;
		}
		// store 'actual' value for high
		return new DefinedBigRational(name, rl, rhNew, rs, numSteps);
	}

	public static DefinedBoolean defineBoolean(String name, String sl, String sh, String ss) throws PrismException
	{
		// no high or step allowed for booleans
		if (sh != null) {
			throw new PrismException("Cannot define ranges for Boolean constants");
		}
		if (ss != null) {
			throw new PrismException("Cannot define ranges for Boolean constants");
		}
		// parse value (low)
		if (sl.equals("true")) {
			return new DefinedBoolean(name, true);
		} else if (sl.equals("false")) {
			return new DefinedBoolean(name, false);
		}
		throw new PrismException("Value " + sl + " for constant " + name + " is not a valid Boolean");
	}

	/** Set the current value.
	(note that "value" is distinct from "definition") */
	
	public void setValue(T val)
	{
		value = val;
	}

	/* Get the current value for the object.
	(note that "value" is distinct from "definition") */
	
	public Object getValue()
	{
		return value;
	}

	/**
	 * Increment the value, return status of overflow.
	 * 
	 * @return Did an overflow occur?
	 */
	public abstract boolean incr();

	/**
	 * Get the jth value for the constant,
	 * eg. if the constant definition is 3:2:9, the values would be 
	 * [3, 5, 7, 9] and so getValue(1) would return 5.
	 * 
	 * @return the j-th value of the constant
	 */
	public abstract T getValue(int j);

	/**
	 * Get the index of a value of the constant,
	 * eg. if the constant definition is 3:2:9, the values would be 
	 * [3, 5, 7, 9] and so getValueIndex(7) would return 2.
	 * 
	 * @return the index of the value of the constant
	 */
	public abstract int getValueIndex(T v);

	// ACCESS METHODS
	
	public String getName() {  return name; }
	
	public Type getType() { return type; }
	
	public T getLow() { return low; }
	
	public T getHigh() { return high; }
	
	public T getStep() { return step; }
	
	public int getNumSteps() { return numSteps; }

	/**
	 * Convert to string.
	 * Note that the (temporary) value is not included here
	 */
	public String toString()
	{
		String s = "";
		
		s += name + "=";
		s += low;
		if (numSteps > 1) s += ":" + step + ":" + high;
		
		return s;
	}

	/**
	 * Parse a double value (supports a/b fraction syntax)
	 */
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



	public static class Undefined extends DefinedConstant<Object>
	{
		public Undefined(String name, Type type)
		{
			super(name, type, null, null, null, -1);
		}

		public boolean incr()
		{
			throw new NullPointerException("Constant is not defined");
		}

		public Object getValue(int j)
		{
			throw new NullPointerException("Constant is not defined");
		}

		public int getValueIndex(Object v)
		{
			throw new NullPointerException("Constant is not defined");
		}
	}



	public static class DefinedBigRational extends DefinedConstant<BigRational>
	{
		public DefinedBigRational(String name, BigRational low, BigRational high, BigRational step, int numSteps)
		{
			super(name,
			           TypeDouble.getInstance(),
			           Objects.requireNonNull(low),
			           Objects.requireNonNull(high),
			           Objects.requireNonNull(step),
			           numSteps);
		}

		@Override
		public boolean incr()
		{
			BigRational rl = (BigRational) low;
			BigRational rh = (BigRational) high;
			BigRational rs = (BigRational) step;
			BigRational rv = (BigRational) value;
			// if possible, increment
			int index = getValueIndex((BigRational) value) + 1;
			rv = rl.add(rs.multiply(index));
			if (rv.lessThanEquals(rh)) {
				value = rv;
				return false;
			}
			// otherwise, reset to low value, note overflow
			value = low;
			return true;
		}

		@Override
		public BigRational getValue(int j)
		{
			BigRational rl = (BigRational)low;
			BigRational rs = (BigRational)low;
			BigRational rv;
			// rv = rl + j*rs
			rv = rl.add(rs.multiply(j));
			return rv;
		}

		@Override
		public int getValueIndex(BigRational v)
		{
			BigRational rl = (BigRational) low;
			BigRational rs = (BigRational) step;
			BigRational rv = (BigRational) v;
			BigRational index = (rv.subtract(rl)).divide(rs);
			try {
				return index.toInt();
			} catch (PrismLangException e) {
				throw new IllegalArgumentException("Can not compute value index, out of range: " + e);
			}
		}
	}


	public static class DefinedBoolean extends DefinedConstant<Boolean>
	{
		public DefinedBoolean(String name, boolean low)
		{
			super(name, TypeBool.getInstance(), low, null, null, 1);
		}

		@Override
		public boolean incr()
		{
			// booleans can't be incremented
			value = low;
			return true;
		}

		@Override
		public Boolean getValue(int j)
		{
			// Fail if j is neither 0 nor 1
			switch (j) {
			case 0:
				return false;
			case 1:
				return true;
			default:
				return null;
			}
		}

		@Override
		public int getValueIndex(Boolean v)
		{
			// Fail if Object is not a Boolean
			return (Boolean) v ? 1 : 0;
		}
	}



	public static class DefinedDouble extends DefinedConstant<Double>
	{
		public DefinedDouble(String name, double low, double high, double step, int numSteps)
		{
			super(name, TypeDouble.getInstance(), low, high, step, numSteps);
		}

		@Override
		public boolean incr()
		{
			// double arithmetic
			double dl = ((Double)low).doubleValue();
			double dh = ((Double)high).doubleValue();
			double ds = ((Double)step).doubleValue();
			double dv = ((Double)value).doubleValue();
			// if possible, increment
			int index = getValueIndex((Double) value) + 1;
			dv = dl + index * ds;
			if (dv <= dh + DOUBLE_PRECISION_CORRECTION * ds) {
				value = Double.valueOf(dv);
				return false;
			}
			// otherwise, reset to low value, note overflow
			value = low;
			return true;
		}

		@Override
		public Double getValue(int j)
		{
			double dl = ((Double)low).doubleValue();
			double ds = ((Double)step).doubleValue();
			//for (i = 0; i < j; i++) dv += ds;
			double dv = dl + j * ds;
			return Double.valueOf(dv);
		}

		@Override
		public int getValueIndex(Double v)
		{
			double dl  = ((Double)low).doubleValue();
			double ds = ((Double)step).doubleValue();
			double dv = ((Double)v).doubleValue();
			return (int)Math.round((dv-dl)/ds);
		}
	}



	public static class DefinedInteger extends DefinedConstant<Integer>
	{
		public DefinedInteger(String name, int low, int high, int step, int numSteps)
		{
			super(name, TypeInt.getInstance(), low, high, step, numSteps);
		}

		@Override
		public boolean incr()
		{
			int ih = ((Integer)high).intValue();
			int is = ((Integer)step).intValue();
			int iv = ((Integer)value).intValue();
			// if possible, increment
			if (iv + is <= ih) {
				value = Integer.valueOf(iv + is);
				return false;
			}
			// otherwise, reset to low value, note overflow
			value = low;
			return true;
		}

		@Override
		public Integer getValue(int j)
		{
			int il = ((Integer)low).intValue();
			int is = ((Integer)step).intValue();
			int iv = il;
			for (int i = 0; i < j; i++) {
				iv += is;
			}
			return Integer.valueOf(iv);
		}

		@Override
		public int getValueIndex(Integer v)
		{
			int il = ((Integer)low).intValue();
			int is = ((Integer)step).intValue();
			int iv = ((Integer)v).intValue();
			return (iv-il)/is;
		}
	}



	/**
	 * Class for a constant that is defined over a set of possible values, the domain.
	 * 
	 * @param <T> the Java type of the values in the domain
	 */
	public static class DefinedDomain<T extends Number> extends DefinedConstant<T>
	{
		private final T[] domain;

		/**
		 * Define a constant from a collection of possible values.
		 * 
		 * @param <T> the Java type of the values in the domain
		 * @param name the name of the constant
		 * @param type the Prism type of the values
		 * @param values the values of the domain
		 * @param a an empty array as blueprint to store the domain in an array of the correct type
		 * @return The constant defined over the given domain
		 */
		public static <T extends Number> DefinedDomain<T> fromValues(String name, Type type, Collection<T> values, T[] a)
		{
			if (values.size() < 1) {
				throw new IllegalArgumentException("expected at least one element in domain");
			}
			if (!(values instanceof Set)) {
				values =  new HashSet<T>(values);
			}
			T[] domain = values.toArray(a);
			Arrays.sort(domain);
			return new DefinedDomain<>(name, type, domain);
		}

		/**
		 * Define a constant over a set of possible values, the domain.
		 * 
		 * @param name the name of the constant
		 * @param type the Prism type of the values
		 * @param domain the domain of distinct values
		 */
		protected DefinedDomain(String name, Type type, T[] domain)
		{
			super(name, type, domain[0], domain[domain.length-1], null, domain.length);
			this.domain = domain;
			checkType();
		}

		/**
		 * Check that the Java type and the Prism type match.
		 * 
		 * @throws IllegalArgumentException If the types do not match
		 */
		protected void checkType()
		{
			Class<?> sampleClass = domain[0].getClass();
			if (sampleClass == Integer.class && ! (type instanceof TypeInt)) {
				throw new IllegalArgumentException("expected TypeInt but got" + type);
			} else if (sampleClass == Double.class && ! (type instanceof TypeDouble)) {
				throw new IllegalArgumentException("expected TypeDouble but got" + type);
			} else if (sampleClass == BigRational.class && ! (type instanceof TypeDouble)) {
				throw new IllegalArgumentException("expected TypeDouble but got" + type);
			}
		}

		@Override
		public boolean incr()
		{
			int i = getValueIndex(value);
			if (i == numSteps - 1) {
				return true;
			}
			value = domain[i+1];
			return false;
		}

		@Override
		public T getValue(int j)
		{
			return domain[j];
		}

		@Override
		public void setValue(T value)
		{
			if (getValueIndex(value) < 0) {
				throw new IllegalArgumentException("expected value from domain");
			}
			super.setValue((T) value);
		}

		@Override
		public int getValueIndex(T v)
		{
			return Arrays.binarySearch(domain, v);
		}

		@Override
		public String toString()
		{
			return name + "=" + Arrays.toString(domain);
		}
	}
}
