//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

public abstract class SystemDefn extends ASTElement
{
	// Overrided version of deepCopy() from superclass ASTElement (to reduce casting).

	/**
	 * Perform a deep copy.
	 */
	public abstract SystemDefn deepCopy();

	// Methods required for SystemDefn (all subclasses should implement):

	/**
	 * Get list of all modules appearing (recursively).
	 * Duplicates are not removed and will appear multiple times in the list.
	 * @deprecated Use {@link SystemDefn#getModules(Vector, ModulesFile)} instead.
	 */
	@Deprecated
	public abstract void getModules(Vector<String> v);

	/**
	 * Get list of all modules appearing (recursively, including descent into references).
	 * Duplicates are not removed and will appear multiple times in the list.
	 */
	public abstract void getModules(Vector<String> v, ModulesFile modulesFile);

	/**
	 * Get list of all synchronising actions _introduced_ (recursively).
	 * @deprecated Use {@link SystemDefn#getSynchs(Vector, ModulesFile)} instead.
	 */
	@Deprecated
	public abstract void getSynchs(Vector<String> v);

	/**
	 * Get list of all synchronising actions _introduced_ (recursively, including descent into references).
	 */
	public abstract void getSynchs(Vector<String> v, ModulesFile modulesFile);

	/**
	 * Get list of all references to other SystemDefns (recursively, but not following references).
	 */
	public abstract void getReferences(Vector<String> v);
}

//------------------------------------------------------------------------------
