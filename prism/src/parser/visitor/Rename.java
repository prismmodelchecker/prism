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

package parser.visitor;

import parser.ast.*;
import prism.PrismLangException;

/**
 * Rename (according to RenamedModule definition), return result.
 */
public class Rename extends ASTTraverseModify
{
	private RenamedModule rm;
	
	public Rename(RenamedModule rm)
	{
		this.rm = rm;
	}
	
	public void visitPost(ModulesFile e) throws PrismLangException
	{
		// This renaming is only designed to be applied
		// at the level of an individual module (and below)
		throw new PrismLangException("ModulesFile should never be renamed");
	}
	
	public void visitPost(PropertiesFile e) throws PrismLangException
	{
		// This renaming is only designed to be applied
		// at the level of an individual module (and below)
		throw new PrismLangException("PropertiesFile should never be renamed");
	}
	
	public void visitPost(Declaration e) throws PrismLangException
	{
		// Get new name for variable
		String s = rm.getNewName(e.getName());
		// No new name is an error
		if (s != null) {
			e.setName(s);
		} else {
			throw new PrismLangException("Definition of module \"" + rm.getName() + "\" must rename variable \"" + e.getName() + "\"", rm);
		}
	}
	
	public void visitPost(Module e) throws PrismLangException
	{
		// New name for module is specied in RenamedModule
		e.setName(rm.getName());
	}
	
	public void visitPost(Command e) throws PrismLangException
	{
		// Rename synchronising action of command
		String s = rm.getNewName(e.getSynch());
		if (s != null) e.setSynch(s);
	}
	
	public void visitPost(Update e) throws PrismLangException
	{
		int i, n;
		String s;
		// Rename variables in update
		n = e.getNumElements();
		for (i = 0; i < n; i++) {
			s = rm.getNewName(e.getVar(i));
			if (s != null) e.setVar(i, new ExpressionIdent(s));
		}
	}

	public void visitPost(ExpressionTemporal e) throws PrismLangException
	{
		// This renaming is only designed to be applied
		// at the level of an individual module (and below)
		throw new PrismLangException("Temporal operators should never be renamed");
	}

	public void visitPost(ExpressionFunc e) throws PrismLangException
	{
		// Rename function name (if necessary)
		String s = rm.getNewName(e.getName());
		if (s != null) e.setName(s);
	}

	public void visitPost(ExpressionIdent e) throws PrismLangException
	{
		// Rename identifier (if necessary)
		String s = rm.getNewName(e.getName());
		if (s != null) e.setName(s);
	}

	public void visitPost(ExpressionProb e) throws PrismLangException
	{
		// This renaming is only designed to be applied
		// at the level of an individual module (and below)
		throw new PrismLangException("P operator should never be renamed");
	}

	public void visitPost(ExpressionReward e) throws PrismLangException
	{
		// This renaming is only designed to be applied
		// at the level of an individual module (and below)
		throw new PrismLangException("R operator should never be renamed");
	}

	public void visitPost(ExpressionSS e) throws PrismLangException
	{
		// This renaming is only designed to be applied
		// at the level of an individual module (and below)
		throw new PrismLangException("S operator should never be renamed");
	}

	public void visitPost(ExpressionExists e) throws PrismLangException
	{
		// This renaming is only designed to be applied
		// at the level of an individual module (and below)
		throw new PrismLangException("E operator should never be renamed");
	}

	public void visitPost(ExpressionForAll e) throws PrismLangException
	{
		// This renaming is only designed to be applied
		// at the level of an individual module (and below)
		throw new PrismLangException("A operator should never be renamed");
	}

	public void visitPost(ExpressionStrategy e) throws PrismLangException
	{
		// This renaming is only designed to be applied
		// at the level of an individual module (and below)
		throw new PrismLangException(e.getOperatorString() + " operator should never be renamed");
	}
}

