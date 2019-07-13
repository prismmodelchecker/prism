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

package parser.visitor;

import parser.ast.*;
import prism.PrismLangException;

/**
 * Compute (maximum) number of nested probabilistic operators (P, S, R).
 */
public class ComputeProbNesting extends ASTTraverse
{
	// Optional properties file to look up property references;
	PropertiesFile propertiesFile = null;
	
	private int currentNesting;
	private int maxNesting;
	
	public ComputeProbNesting()
	{
		this(null);
	}

	public ComputeProbNesting(PropertiesFile propertiesFile)
	{
		this.propertiesFile = propertiesFile;
		currentNesting = 0;
		maxNesting = 0;
	}

	public int getMaxNesting()
	{
		return maxNesting;
	}
	
	public void visitPre(ExpressionProb e) throws PrismLangException
	{
		currentNesting++;
		maxNesting = Math.max(maxNesting, currentNesting);
	}

	public void visitPost(ExpressionProb e) throws PrismLangException
	{
		currentNesting--;
	}

	public void visitPre(ExpressionReward e) throws PrismLangException
	{
		currentNesting++;
		maxNesting = Math.max(maxNesting, currentNesting);
	}

	public void visitPost(ExpressionReward e) throws PrismLangException
	{
		currentNesting--;
	}

	public void visitPre(ExpressionSS e) throws PrismLangException
	{
		currentNesting++;
		maxNesting = Math.max(maxNesting, currentNesting);
	}
	
	public void visitPost(ExpressionSS e) throws PrismLangException
	{
		currentNesting--;
	}
	
	public void visitPost(ExpressionProp e) throws PrismLangException
	{
		// If possible, look up property and recurse
		if (propertiesFile != null) {
			Property prop = propertiesFile.lookUpPropertyObjectByName(e.getName());
			if (prop != null) {
				prop.accept(this);
			} else {
				throw new PrismLangException("Unknown property reference " + e, e);
			}
		}
	}
}
