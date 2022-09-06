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

package parser.visitor;

import java.util.Vector;

import parser.ast.*;
import prism.PrismLangException;

/**
 * Perform further  semantic checks on a ModulesFile (or parts of it)
 * that can only be done once values for (at least some) undefined constants have been defined.
 */
public class ModulesFileSemanticCheckAfterConstants extends ASTTraverse
{
	@SuppressWarnings("unused")
	private ModulesFile modulesFile;

	public ModulesFileSemanticCheckAfterConstants(ModulesFile modulesFile)
	{
		setModulesFile(modulesFile);
	}

	public void setModulesFile(ModulesFile modulesFile)
	{
		this.modulesFile = modulesFile;
	}

	public void visitPost(Update e) throws PrismLangException
	{
		int i, n;
		String var;
		Vector<String> varsUsed = new Vector<String>();

		// Check that no variables are set twice in the same update
		// Currently, could do this *before* constants are defined,
		// but one day we might need to worry about e.g. array indices...
		n = e.getNumElements();
		for (i = 0; i < n; i++) {
			var = e.getVar(i);
			if (varsUsed.contains(var)) {
				throw new PrismLangException("Variable \"" + var + "\" is set twice in the same update", e.getVarIdent(i));
			}
			varsUsed.add(var);
		}
		varsUsed.clear();
	}
}
