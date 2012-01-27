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

import java.util.Vector;

import parser.ast.*;
import prism.PrismLangException;

/**
 * Get all undefined constants used (i.e. in ExpressionConstant objects) recursively and return as a list.
 * Recursive descent means that we also find constants that are used within other constants, labels, properties.
 * We only recurse into constants/labels/properties in the passed in lists.
 * Any others discovered are ignored (and not descended into).
 * ConstantList must be non-null so that we can determine which constants are undefined;
 * LabelList and PropertiesFile passed in as null are ignored.
 */
public class GetAllUndefinedConstantsRecursively extends ASTTraverse
{
	private Vector<String> v;
	private ConstantList constantList;
	private LabelList labelList;
	private PropertiesFile propertiesFile;

	public GetAllUndefinedConstantsRecursively(Vector<String> v, ConstantList constantList, LabelList labelList, PropertiesFile propertiesFile)
	{
		this.v = v;
		this.constantList = constantList;
		this.labelList = labelList;
		this.propertiesFile = propertiesFile;
	}

	public void visitPost(ExpressionConstant e) throws PrismLangException
	{
		// Look up this constant in the constant list
		int i = constantList.getConstantIndex(e.getName());
		// Ignore constants not in the list 
		if (i == -1)
			return;
		Expression expr = constantList.getConstant(i);
		// If constant is undefined, add to the list
		if (expr == null) {
			if (!v.contains(e.getName())) {
				v.addElement(e.getName());
			}
		}
		// If not, check constant definition recursively for more undefined constants
		else {
			expr.accept(this);
		}
	}

	public void visitPost(ExpressionLabel e) throws PrismLangException
	{
		// Ignore special cases of labels (no constants there)
		if (e.getName().equals("deadlock") || e.getName().equals("init")) {
			return;
		}
		// Look up this label in the label list, if possible
		if (labelList == null)
			return;
		int i = labelList.getLabelIndex(e.getName());
		if (i == -1)
			return;
		Expression expr = labelList.getLabel(i);
		// Check label definition recursively for more undefined constants
		expr.accept(this);
	}

	public void visitPost(ExpressionProp e) throws PrismLangException
	{
		// Look up this property in the properties files, if possible
		if (propertiesFile == null)
			return;
		Property prop = propertiesFile.lookUpPropertyObjectByName(e.getName());
		if (prop == null)
			return;
		// Check property recursively for more undefined constants
		prop.getExpression().accept(this);
	}
}
