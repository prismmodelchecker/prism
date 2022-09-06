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

package parser.ast;

import parser.type.*;
import parser.visitor.ASTVisitor;
import prism.PrismLangException;

public class DeclarationClock extends DeclarationType
{
	public DeclarationClock()
	{
		// The type stored for a Declaration/DeclarationType object
		// is static - it is not computed during type checking.
		// (But we re-use the existing "type" field for this purpose)
		setType(TypeClock.getInstance());
	}

	/**
	 * Return the default start value for a variable of this type.
	 */
	public Expression getDefaultStart()
	{
		return Expression.Double(0);
	}
	
	/* TODO:
	@Override
	public Expression getStart(ModulesFile parent)
	{
		if (parent != null && parent.getInitialStates() != null)
			return null;

		return start != null ? start : low;
	}
	*/
	
	// Methods required for ASTElement:
	
	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	/**
	 * Convert to string.
	 */
	@Override
	public String toString()
	{
		return "clock";
	}

	/**
	 * Perform a deep copy.
	 */
	@Override
	public ASTElement deepCopy()
	{
		DeclarationClock ret = new DeclarationClock();
		ret.setPosition(this);
		return ret;
	}
}
