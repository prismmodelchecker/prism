//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford)
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

public class DeclarationArray extends DeclarationType
{
	// Min array index
	protected Expression low;
	// Max array index
	protected Expression high;
	// Type of object contained in this array
	protected DeclarationType subtype;

	public DeclarationArray(Expression low, Expression high, DeclarationType subtype)
	{
		this.low = low;
		this.high = high;
		this.subtype = subtype;
		// The type stored for a Declaration/DeclarationType object
		// is static - it is not computed during type checking.
		// (But we re-use the existing "type" field for this purpose)
		// And we copy the info from DeclarationType across to Declaration for convenience.
		setType(new TypeArray(subtype.getType()));
	}

	public void setLow(Expression l)
	{
		low = l;
	}

	public void setHigh(Expression h)
	{
		high = h;
	}

	public void setSubtype(DeclarationType subtype)
	{
		this.subtype = subtype;
	}

	public Expression getLow()
	{
		return low;
	}

	public Expression getHigh()
	{
		return high;
	}

	public DeclarationType getSubtype()
	{
		return subtype;
	}

	/**
	 * Return the default start value for a variable of this type.
	 */
	public Expression getDefaultStart()
	{
		// TODO: what should be the default?
		return null;
	}
	
	/* TODO
	@Override
	public Expression getStart(ModulesFile parent)
	{
		if (parent != null && parent.getInitialStates() != null)
			return null;

		// TODO: what should be the default?
		return start != null ? start : null;
	}*/

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
		return "array [" + low + ".." + high + "] of " + subtype;
	}

	/**
	 * Perform a deep copy.
	 */
	@Override
	public ASTElement deepCopy()
	{
		Expression lowCopy = (low == null) ? null : low.deepCopy();
		Expression highCopy = (high == null) ? null : high.deepCopy();
		DeclarationType subtypeCopy = (DeclarationType) subtype.deepCopy();
		DeclarationArray ret = new DeclarationArray(lowCopy, highCopy, subtypeCopy);
		ret.setPosition(this);
		return ret;
	}
}
