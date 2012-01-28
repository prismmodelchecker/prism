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
import parser.type.*;
import prism.PrismLangException;

/**
	 * Expand property references and labels, return result.
	 * Property expansion is done recursively.
	 * Special labels "deadlock", "init" and any not in label list are left.
 */
public class ExpandPropRefsAndLabels extends ASTTraverseModify
{
	// The PropertiesFile for property lookup
	private PropertiesFile propertiesFile;
	// The LabelList for label definitions
	private LabelList labelList;
	
	public ExpandPropRefsAndLabels(PropertiesFile propertiesFile, LabelList labelList)
	{
		this.propertiesFile = propertiesFile;
		this.labelList = labelList;
	}
	
	public Object visit(ExpressionLabel e) throws PrismLangException
	{
		int i;
		Type t;
		Expression expr;
		
		// See if identifier corresponds to a label
		i = labelList.getLabelIndex(e.getName());
		if (i != -1) {
			// If so, replace it with (a copy of) the corresponding expression
			expr = labelList.getLabel(i).deepCopy();
			// But also recursively expand that
			// (nested labels not currently supported but may be one day)
			// (don't clone it to avoid duplication of work)
			expr = (Expression)expr.expandLabels(labelList);
			// Put in brackets so precedence is preserved
			// (for display purposes only; in case of re-parse)
			// Also, preserve type (this is probably being done before
			// type-checking so unnecessary, but do so just in case)
			t = expr.getType();
			expr = Expression.Parenth(expr);
			expr.setType(t);
			// Return replacement expression
			return expr;
		}
		
		// Couldn't find definition - leave unchanged.
		return e;
	}

	public Object visit(ExpressionProp e) throws PrismLangException
	{
		Property prop;
		Type t;
		Expression expr;
		
		// See if name corresponds to a property
		prop = propertiesFile.lookUpPropertyObjectByName(e.getName());
		if (prop != null) {
			// If so, replace it with (a copy of) the corresponding expression
			expr = prop.getExpression().deepCopy();
			// But also recursively expand that
			// (don't clone it to avoid duplication of work)
			expr = (Expression)expr.expandPropRefsAndLabels(propertiesFile, labelList);
			// Put in brackets so precedence is preserved
			// (for display purposes only; in case of re-parse)
			// Also, preserve type (this is probably being done before
			// type-checking so unnecessary, but do so just in case)
			t = expr.getType();
			expr = Expression.Parenth(expr);
			expr.setType(t);
			// Return replacement expression
			return expr;
		}
		
		// Couldn't find definition - leave unchanged.
		return e;
	}
}

