//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package parser.type;

import java.util.HashMap;
import java.util.Map;

import common.Interval;
import parser.EvaluateContext.EvalMode;
import prism.PrismLangException;

public class TypeInterval extends Type
{
	private static Map<Type, TypeInterval> singletons;
	
	static
	{
		singletons = new HashMap<Type, TypeInterval>();
	}
	
	private Type subType;
	
	private TypeInterval(Type subType)
	{
		this.subType = subType;
	}

	public static TypeInterval getInstance(Type subType)
	{
		return singletons.computeIfAbsent(subType, TypeInterval::new);
	}

	public Type getSubType()
	{
		return subType;
	}

	public void setSubType(Type subType)
	{
		this.subType = subType;
	}

	// Methods required for Type:
	
	@Override
	public String getTypeString()
	{
		return "interval of " + subType.getTypeString();
	}
	
	@Override
	public boolean isPrimitive()
	{
		return false;
	}
	
	@Override
	public Object defaultValue()
	{
		return new Interval<Object>(subType.defaultValue(), subType.defaultValue());
	}
	
	@Override
	public boolean canCastTypeTo(Type type)
	{
		return type instanceof TypeDouble || type instanceof TypeInt || (type instanceof TypeInterval && getSubType().canCastTypeTo(((TypeInterval) type).getSubType()));
	}
	
	@Override
	public Interval<?> castValueTo(Object value) throws PrismLangException
	{
		// Scalar converts to singleton interval
		if (value instanceof Double || value instanceof Integer) {
			Object subValue = getSubType().castValueTo(value);
			return new Interval<>(subValue, subValue);
		}
		// For interval, cast low/high
		else if (value instanceof Interval) {
			Object lower = getSubType().castValueTo(((Interval<?>) value).getLower());
			Object upper = getSubType().castValueTo(((Interval<?>) value).getUpper());
			return new Interval<>(lower, upper);
		}
		else {
			throw new PrismLangException("Can't convert " + value.getClass() + " to type " + getTypeString());
		}
	}

	@Override
	public Interval<?> castValueTo(Object value, EvalMode evalMode) throws PrismLangException
	{
		// Scalar converts to singleton interval
		if (value instanceof Double || value instanceof Integer) {
			Object subValue = getSubType().castValueTo(value, evalMode);
			return new Interval<Object>(subValue, subValue);
		}
		// For interval, cast low/high
		else if (value instanceof Interval) {
			Object lower = getSubType().castValueTo(((Interval<?>) value).getLower(), evalMode);
			Object upper = getSubType().castValueTo(((Interval<?>) value).getUpper(), evalMode);
			return new Interval<Object>(lower, upper);
		}
		else {
			throw new PrismLangException("Can't convert " + value.getClass() + " to type " + getTypeString());
		}
	}

	// Standard methods:
	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof TypeInterval)
		{
			TypeInterval oi = (TypeInterval)o;
			return (subType.equals(oi.getSubType()));
		}
		
		return false;
	}
}
