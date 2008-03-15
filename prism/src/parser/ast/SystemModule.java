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

import java.util.Vector;

import parser.visitor.*;
import prism.PrismLangException;

public class SystemModule extends SystemDefn
{
	// Module name
	String name;
	
	// Constructors
	
	public SystemModule()
	{
	}
	
	public SystemModule(String n)
	{
		name = n;
	}
	
	// Set method
	
	public void setName(String n)
	{
		name = n;
	}
	
	// Get method
	
	public String getName()
	{
		return name;
	}
	
	// Methods required for SystemDefn (all subclasses should implement):
	
	/**
	 * Get list of all modules appearing (recursively).
	 */
	public void getModules(Vector<String> v)
	{
		v.addElement(name);
	}
	
	/**
	 * Get list of all synchronising actions _introduced_ (recursively).
	 */
	public void getSynchs(Vector<String> v)
	{
		// do nothing
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
		return name;
	}
	
	/**
	 * Perform a deep copy.
	 */
	public SystemDefn deepCopy()
	{
		SystemDefn ret = new SystemModule(name);
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
