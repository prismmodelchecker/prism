//==============================================================================
//	
//	Copyright (c) 2020-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

public class Observable extends ASTElement
{
	/**
	 * Name of the observable
	 */
	private String name;
	/**
	 * Expression defining the observable.
	 */
	private Expression definition;
	
	// Constructor
	
	/**
	 * Create a new observable with a given name and definition.
	 * @param name Name of the observable
	 * @param expr Expression defining the observable
	 */
	public Observable(String name, Expression definition)
	{
		setName(name);
		setDefinition(definition);
	}

	// Set methods
	
	public void setName(String name)
	{
		this.name = name;
	}

	public void setDefinition(Expression definition)
	{
		this.definition = definition;
	}

	// Get methods
	
	/**
	 * Get the name of the observable.
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Get the expression defining the observable.
	 */
	public Expression getDefinition()
	{
		return definition;
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
		return "observable \"" + name + "\" = " + definition + ";";
	}
	
	/**
	 * Perform a deep copy.
	 */
	public Observable deepCopy()
	{
		Observable ret = new Observable(name, definition.deepCopy());
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
