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
 * Expand constants whose definitions are contained in the supplied ConstantList.
 */
public class ExpandConstants extends ASTTraverseModify
{
	private ConstantList constantList;
	private boolean all;
	
	/**
	 * @param constantList The ConstantList containing definitions
	 */
	public ExpandConstants(ConstantList constantList)
	{
		this(constantList, true);
	}
	
	/**
	 * @param constantList The ConstantList containing definitions
	 * @param all If true, an exception is thrown if any constants are undefined
	 */
	public ExpandConstants(ConstantList constantList, boolean all)
	{
		this.constantList = constantList;
		this.all = all;
	}
	
	@Override
	public Object visit(ExpressionConstant e) throws PrismLangException
	{
		// See if identifier corresponds to a constant defined in the list
		int i = constantList.getConstantIndex(e.getName());
		if (i != -1 && constantList.getConstant(i) != null) {
			// If so, replace it with the corresponding expression
			Expression expr = constantList.getConstant(i).deepCopy();
			// But also recursively expand that
			expr = (Expression) expr.expandConstants(constantList, all);
			// Put in brackets so precedence is preserved
			// (for display purposes only; in case of re-parse)
			// This is being done after type-checking so also set type
			Type t = expr.getType();
			expr = Expression.Parenth(expr);
			expr.setType(t);
			// Return replacement expression
			return expr;
		}
		// Otherwise, either return unchanged or complain (depending on 'all')
		if (all) {
			throw new PrismLangException("Undefined constant", e);
		} else {
			return e;
		}
	}
}

