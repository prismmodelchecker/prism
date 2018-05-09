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

package prism;

import parser.ast.ASTElement;

public class PrismLangException extends PrismException
{
	public static final int MAX_ERR_STR = 20;
	
	protected ASTElement e = null;
	
	public PrismLangException(String s)
	{
		super(s);
	}
	
	public PrismLangException(String s, ASTElement e)
	{
		super(s);
		this.e =e ;
	}
	
	public void setASTElement(ASTElement e)
	{
		this.e =e ;
	}
	
	public boolean hasASTElement()
	{
		return e != null ;
	}
	
	public ASTElement getASTElement()
	{
		return e ;
	}
	
	public String getMessage()
	{
		String msg = super.getMessage();
		if (e == null) return msg;
		String s = null;
		try {
			s = e.toString();
		} catch (Exception ex) {
			// in case there is a problem converting the AST element to a string
			// we ignore it
		}
		if (s != null && s.length() < MAX_ERR_STR) {
			if (e.hasPosition()) msg += " (\"" + s + "\", " + e.getBeginString() +")";
			else msg += " (\"" + s + "\")";
		}
		else {
			if (e.hasPosition()) msg += " (" + e.getBeginString() +")";
		}
		return msg;
	}
	
	public String toString()
	{
		return "Error: " + getMessage() + ".";
	}
	
	public boolean hasLineNumbers()
	{
		if (hasASTElement())
		{
			if (e.getBeginColumn() != -1
			 && e.getBeginLine() != -1
			 && e.getEndLine() != -1
			 && e.getEndColumn() != -1)
			return true;
		}
		
		return false;
	}
	
	public int getBeginColumn()
	{
		if (hasLineNumbers())
			return e.getBeginColumn();
		else
			return -1;
	}
	
	public int getEndColumn()
	{
		if (hasLineNumbers())
			return e.getEndColumn();
		else
			return -1;
	}
	
	public int getBeginLine()
	{
		if (hasLineNumbers())
			return e.getBeginLine();
		else
			return -1;
	}
	
	public int getEndLine()
	{
		if (hasLineNumbers())
			return e.getEndLine();
		else
			return -1;
	}
}

//------------------------------------------------------------------------------
