//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProp;
import parser.ast.PropertiesFile;
import parser.ast.Property;
import prism.PrismLangException;

/**
 * Get all references to properties (by name) (i.e. ExpressionProp objects) recursively, store names in set.
 */
public class GetAllPropRefsRecursively extends ASTTraverse
{
	private Vector<String> v;
	private PropertiesFile pf;

	public GetAllPropRefsRecursively(Vector<String> v, PropertiesFile pf)
	{
		this.v = v;
		this.pf = pf;
	}

	public void visitPost(ExpressionProp e) throws PrismLangException
	{
		if (!v.contains(e.getName())) {
			v.addElement(e.getName());
		}
	}

	public void visitPost(ExpressionLabel e) throws PrismLangException
	{
		String name;
		Property prop = null;
		// See if identifier corresponds to a property
		name = e.getName();
		if (prop == null && pf != null) {
			prop = pf.lookUpPropertyObjectByName(name);
		}
		if (prop != null) {
			// If so, add the name
			v.addElement(e.getName());
		}
	}
}
