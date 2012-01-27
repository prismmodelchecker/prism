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
 * Find all references to properties (by name), replace the ExpressionLabels with ExpressionProp objects.
 */
public class FindAllPropRefs extends ASTTraverseModify
{
	private ModulesFile mf;
	private PropertiesFile pf;
	
	public FindAllPropRefs(ModulesFile mf, PropertiesFile pf)
	{
		this.mf = mf;
		this.pf = pf;
	}
	
	public Object visit(ExpressionLabel e) throws PrismLangException
	{
		String name;
		Property prop = null;
		// See if identifier corresponds to a property
		name = e.getName();
		if (mf != null) {
			prop = mf.getPropertyByName(name);
		}
		if (prop == null && pf != null) {
			prop = pf.getPropertyObjectByName(name);
		}
		if (prop != null) {
			// If so, replace it with an ExpressionProp object
			ExpressionProp expr = new ExpressionProp(e.getName());
			expr.setPosition(e);
			return expr;
		}
		// Otherwise, leave it unchanged
		return e;
	}
}

