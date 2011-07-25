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
 * Recursive decent means that we find e.g. constants that are used within other constants, labels.
 */
public class GetAllUndefinedConstantsRecursively extends ASTTraverse
{
	private Vector<String> v;
	private ConstantList constantList;
	private LabelList labelList;
	
	public GetAllUndefinedConstantsRecursively(Vector<String> v, ConstantList constantList, LabelList labelList)
	{
		this.v = v;
		this.constantList = constantList;
		this.labelList = labelList;
	}
	
	public void visitPost(ExpressionConstant e) throws PrismLangException
	{
		// Look up this constant in the constant list
		int i = constantList.getConstantIndex(e.getName());
		if (i == -1)
			throw new PrismLangException("Unknown constant \"" + e.getName() + "\"");
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
		// Look up this label in the label list
		int i = labelList.getLabelIndex(e.getName());
		if (i == -1)
			throw new PrismLangException("Unknown label \"" + e.getName() + "\"");
		Expression expr = labelList.getLabel(i);
		// Check label definition recursively for more undefined constants
		expr.accept(this);
	}
}

