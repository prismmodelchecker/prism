//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford)
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

/**
 * Variable declaration details
 */
public class Declaration extends ASTElement
{
	// Name
	protected String name;
	// Type of declaration
	protected DeclarationType declType;
	// Initial value - null if none specified
	protected Expression start;
		
	public Declaration(String name, DeclarationType declType)
	{
		setName(name);
		setDeclType(declType);
		setStart(null);
	}
	
	// Set methods
	
	public void setName(String name)
	{
		this.name = name;
	}	

	public void setDeclType(DeclarationType declType)
	{
		this.declType = declType;
		// The type stored for a Declaration/DeclarationType object
		// is static - it is not computed during type checking.
		// (But we re-use the existing "type" field for this purpose)
		setType(declType.getType());
	}	

	public void setStart(Expression start)
	{
		this.start = start;
	}

	// Get methods

	public String getName()
	{
		return name;
	}

	public DeclarationType getDeclType()
	{
		return declType;
	}	

	/**
	 * Get the specified initial value of this variable (null if it was not specified).
	 * To get the actual value (defaults to lower bound if appropriate),
	 * use {@link #getStartOrDefault()}.
	 */
	public Expression getStart()
	{
		return start;
	}
	
	/**
	 * Get the specified initial value of this variable,
	* using the default value for its type if not specified.
	 */
	public Expression getStartOrDefault()
	{
		return isStartSpecified() ? start : declType.getDefaultStart();
	}
	
	/**
	 * Get the initial value of this variable, within a ModulesFile.
	 * Will be null if parent ModulesFile passed in has an init...endinit.
	 * Otherwise defaults to lower bound.
	 * Can force lower bound to returned by passing in null. 
	 */
	
	/** TODO public abstract Expression getStart(ModulesFile parent); */

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
	@Override
	public String toString()
	{
		String s  = "";
		s += name + " : ";
		s += declType;
		if (start != null) s += " init " + start;
		return s;
	}

	/**
	 * Perform a deep copy.
	 */
	@Override
	public ASTElement deepCopy()
	{
		Declaration ret = new Declaration(getName(), (DeclarationType)getDeclType().deepCopy());
		if (getStart() != null)
			ret.setStart(getStart().deepCopy());
		ret.setPosition(this);
		return ret;
	}
}

// ------------------------------------------------------------------------------
