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

import java.util.Vector;

import parser.ast.*;
import prism.PrismLangException;

/**
 * Perform further semantic checks that can only be done once values
 * for (at least some) undefined constants have been defined. Optionally pass in parent
 * ModulesFile and PropertiesFile for some additional checks (or leave null);
 */
public class SemanticCheckAfterConstants extends ASTTraverse
{
	private ModulesFile modulesFile;
	private PropertiesFile propertiesFile;

	public SemanticCheckAfterConstants()
	{
		this(null, null);
	}

	public SemanticCheckAfterConstants(ModulesFile modulesFile)
	{
		this(modulesFile, null);
	}

	public SemanticCheckAfterConstants(ModulesFile modulesFile, PropertiesFile propertiesFile)
	{
		setModulesFile(modulesFile);
		setPropertiesFile(propertiesFile);
	}

	public void setModulesFile(ModulesFile modulesFile)
	{
		this.modulesFile = modulesFile;
	}

	public void setPropertiesFile(PropertiesFile propertiesFile)
	{
		this.propertiesFile = propertiesFile;
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
