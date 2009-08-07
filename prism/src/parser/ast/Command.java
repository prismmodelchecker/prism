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

import java.util.ArrayList;

import parser.Values;
import parser.visitor.*;
import prism.PrismLangException;

public class Command extends ASTElement
{
	// Action label
	String synch;
	// Guard
	Expression guard;
	// List of updates
	Updates updates;
	// Parent module
	Module parent;
	
	// Constructor
	
	public Command()
	{
		synch = "";
		guard = null;
		updates = null;
	}
	
	// Set methods
	
	public void setSynch(String s)
	{
		synch = s;
	}
	
	public void setGuard(Expression g)
	{
		guard = g;
	}
	
	public void setUpdates(Updates u)
	{
		updates = u;
		u.setParent(this);
	}
	
	public void setParent(Module m)
	{
		parent = m;
	}

	// Get methods

	public String getSynch()
	{
		return synch;
	}
	
	public Expression getGuard()
	{
		return guard;
	}
	
	public Updates getUpdates()
	{
		return updates;
	}
	
	public Module getParent()
	{
		return parent;
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
		String s = "[" + synch;
		s += "] " + guard + " -> " + updates;
		return s;
	}
	
	/**
	 * Perform a deep copy.
	 */
	public ASTElement deepCopy()
	{
		Command ret = new Command();
		ret.setSynch(getSynch());
		ret.setGuard(getGuard().deepCopy());
		ret.setUpdates((Updates)getUpdates().deepCopy());
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
