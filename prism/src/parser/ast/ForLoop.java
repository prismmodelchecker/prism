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

package parser.ast;

import parser.visitor.*;
import prism.PrismLangException;
import parser.type.*;
public class ForLoop extends ASTElement
{
	// For loop info
	private String lhs;
	private Expression from;
	private Expression to;
	private Expression step;
	// Optional storage...
	private int pc;
	private String between;
	
	// Constructor
	
	public ForLoop()
	{
		// for loop info
		lhs = null;
		from = null;
		to = null;
		step = null;
		// optional stuff
		pc = 0;
		between = "";
	}

	// Set methods
	
	public void setLHS(String s)
	{
		lhs = s;
	}
	
	public void setFrom(Expression e)
	{
		from = e;
	}
	
	public void setTo(Expression e)
	{
		to = e;
	}
	
	public void setStep(Expression e)
	{
		step = e;
	}
	
	public void setPC(int i)
	{
		pc = i;
	}
	
	public void setBetween(String s)
	{
		between = s;
	}

	// Get methods
	
	public String getLHS()
	{
		return lhs;
	}
	
	public Expression getFrom()
	{
		return from;
	}
	
	public Expression getTo()
	{
		return to;
	}
	
	public Expression getStep()
	{
		return (step != null) ? step : new ExpressionLiteral(TypeInt.getInstance(), 1);
	}
	
	public int getPC()
	{
		return pc;
	}
	
	public String getBetween()
	{
		return between;
	}

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
	public String toString()
	{
		String s = lhs + "=" + from;
		if (step != null) s += ":" + step;
		s += ":" + to;
		return s;
	}
	
	/**
	 * Perform a deep copy.
	 */
	public ASTElement deepCopy()
	{
		ForLoop ret = new ForLoop();
		ret.lhs = lhs;
		ret.from = from.deepCopy();
		ret.to = to.deepCopy();
		ret.step = step.deepCopy();
		ret.pc = pc;
		ret.between = between;
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
