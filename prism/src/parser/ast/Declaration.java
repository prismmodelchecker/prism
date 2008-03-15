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

public class Declaration extends ASTElement
{
	// Variable declaration details
	String name; // Name
	Expression low; // Min value - not for bools
	Expression high; // Max value - not for bools
	Expression start; // Initial value - null if none specified

	// Pointless constructor

	public Declaration()
	{
		name = "";
		low = null;
		high = null;
		start = null;
	}

	// Integer variable constructor

	public Declaration(String n, Expression l, Expression h, Expression s)
	{
		name = n;
		low = l;
		high = h;
		start = s;
		setType(Expression.INT);
	}

	// Boolean variable constructor

	public Declaration(String n, Expression s)
	{
		name = n;
		low = null;
		high = null;
		start = s;
		setType(Expression.BOOLEAN);
	}

	// Set methods

	public void setName(String n)
	{
		name = n;
	}

	public void setLow(Expression l)
	{
		low = l;
	}

	public void setHigh(Expression h)
	{
		high = h;
	}

	public void setStart(Expression s)
	{
		start = s;
	}

	// Get methods

	public String getName()
	{
		return name;
	}

	public Expression getLow()
	{
		return low;
	}

	public Expression getHigh()
	{
		return high;
	}

	/**
	 * Get the specified initial value of this variable (null if it was not specified).
	 * To get the actual value (defaults to lower bound if appropriate),
	 * use {@link #getStart(ModulesFile)}.
	 */
	public Expression getStart()
	{
		return start;
	}

	/**
	 * Get the initial value of this variable, within a ModulesFile.
	 * Will be null if parent ModulesFile passed in has an init...endinit.
	 * Otherwise defaults to lower bound.
	 * Can force lower bound to returned by passing in null. 
	 */
	public Expression getStart(ModulesFile parent)
	{
		if (parent != null && parent.getInitialStates() != null) return null;
		switch (type) {
		case Expression.INT: return start != null ? start : low;
		case Expression.BOOLEAN: return start != null ? start : Expression.False();
		}
		return null;
	}

	public boolean isStartSpecified()
	{
		return start != null;
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
		String s  = "";
		if (type == Expression.INT) {
			s = name + " : [" + low + ".." + high + "]";
		} else if (type == Expression.BOOLEAN) {
			s = name + " : bool";
		}
		if (start != null) s += " init " + start;
		return s;
	}

	/**
	 * Perform a deep copy.
	 */
	public ASTElement deepCopy()
	{
		Declaration ret = new Declaration();
		ret.setName(getName());
		if (getLow() != null)
			ret.setLow(getLow().deepCopy());
		if (getHigh() != null)
			ret.setHigh(getHigh().deepCopy());
		if (getStart() != null)
			ret.setStart(getStart().deepCopy());
		ret.setType(getType());
		ret.setPosition(this);
		return ret;
	}
}

// ------------------------------------------------------------------------------
