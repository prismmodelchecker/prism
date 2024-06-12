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

import java.util.List;

import parser.visitor.DeepCopy;
import prism.PrismLangException;

public abstract class SystemDefn extends ASTElement
{
	// Overwritten version of deepCopy() and deepCopy(DeepCopy copier) from superclass ASTElement (to reduce casting).

	@Override
	public abstract SystemDefn deepCopy(DeepCopy copier) throws PrismLangException;

	@Override
	public SystemDefn deepCopy()
	{
		return (SystemDefn) super.deepCopy();
	}

	@Override
	public SystemDefn clone()
	{
		return (SystemDefn) super.clone();
	}

	// Methods required for SystemDefn (all subclasses should implement):

	/**
	 * Get list of all modules appearing (recursively).
	 * Duplicates are not removed and will appear multiple times in the list.
	 * @deprecated Use {@link SystemDefn#getModules(List, ModulesFile)} instead.
	 */
	@Deprecated
	public abstract void getModules(List<String> v);

	/**
	 * Get list of all modules appearing (recursively, including descent into references).
	 * Duplicates are not removed and will appear multiple times in the list.
	 */
	public abstract void getModules(List<String> v, ModulesFile modulesFile);

	/**
	 * Get list of all synchronising actions _introduced_ (recursively).
	 * @deprecated Use {@link SystemDefn#getSynchs(List, ModulesFile)} instead.
	 */
	@Deprecated
	public abstract void getSynchs(List<String> v);

	/**
	 * Get list of all synchronising actions _introduced_ (recursively, including descent into references).
	 */
	public abstract void getSynchs(List<String> v, ModulesFile modulesFile);

	/**
	 * Get list of all references to other SystemDefns (recursively, but not following references).
	 */
	public abstract void getReferences(List<String> v);
}

//------------------------------------------------------------------------------
