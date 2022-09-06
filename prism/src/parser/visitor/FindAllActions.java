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

import java.util.*;

import parser.ast.*;
import prism.PrismLangException;

/**
 * Find all references to action labels, check they exist and, if required,
 * store their index locally (as defined by the containing ModuleFile).
 */
public class FindAllActions extends ASTTraverseModify
{
	private List<String> synchs;
	
	public FindAllActions(List<String> synchs)
	{
		this.synchs = synchs;
	}
	
	// Note that this is done with VisitPost, i.e. after recursively visiting children.
	// This is ok because we can modify rather than create a new object so don't need to return it.
	public void visitPost(Command e) throws PrismLangException
	{
		int i;
		// For independent actions, the index is 0
		if (e.getSynch().equals("")) {
			e.setSynchIndex(0);
			return;
		}
		// Otherwise, see if action name exists
		i = synchs.indexOf(e.getSynch());
		if (i != -1) {
			// If so, set the index (starts from 1)
			e.setSynchIndex(i + 1);
			return;
		}
		// Otherwise, there is a problem.
		// In theory, this can never occur since the set of action names is defined by existence in commands
		throw new PrismLangException("Unknown action name " + e.getSynch() + " in command", e);
	}
	
	// Note that this is done with VisitPost, i.e. after recursively visiting children.
	// This is ok because we can modify rather than create a new Command so don't need to return it.
	public void visitPost(RewardStructItem e) throws PrismLangException
	{
		int i;
		// Ignore state rewards
		// (Note: This guarantees getSynch() is non-null)
		if (!e.isTransitionReward())
			return;
		// For independent actions, the index is 0
		if (e.getSynch().equals("")) {
			e.setSynchIndex(0);
			return;
		}
		// Otherwise, see if action name exists
		i = synchs.indexOf(e.getSynch());
		if (i != -1) {
			// If so, set the index (starts from 1)
			e.setSynchIndex(i + 1);
			return;
		}
		// Otherwise, there is a problem.
		throw new PrismLangException("Unknown action name " + e.getSynch() + " in reward structure item", e);
	}
}

