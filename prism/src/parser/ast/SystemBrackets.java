//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

import java.util.Vector;

import parser.visitor.*;
import prism.PrismLangException;

// note: although this makes no difference to the meaning
// of the expression, it means we can keep the user's
// original bracketting for display purposes

public class SystemBrackets extends SystemDefn
{
	// Operand
	private SystemDefn operand;

	// Constructors
	
	public SystemBrackets()
	{
	}

	public SystemBrackets(SystemDefn s)
	{
		operand = s;
	}

	// Set method
	
	public void setOperand(SystemDefn s)
	{
		operand = s;
	}
	
	// Get method
	
	public SystemDefn getOperand()
	{
		return operand;
	}
	
	// Methods required for SystemDefn (all subclasses should implement):
	
	@Override
	@SuppressWarnings("deprecation")
	public void getModules(Vector<String> v)
	{
		operand.getModules(v);
	}

	@Override
	public void getModules(Vector<String> v, ModulesFile modulesFile)
	{
		operand.getModules(v, modulesFile);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void getSynchs(Vector<String> v)
	{
		operand.getSynchs(v);
	}
	
	@Override
	public void getSynchs(Vector<String> v, ModulesFile modulesFile)
	{
		operand.getSynchs(v, modulesFile);
	}
	
	@Override
	public void getReferences(Vector<String> v)
	{
		operand.getReferences(v);
	}
	
	// Methods required for ASTElement:
	
	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}
	
	@Override
	public String toString()
	{
		return "(" + operand + ")";
	}
	
	@Override
	public SystemDefn deepCopy()
	{
		SystemDefn ret = new SystemBrackets(getOperand().deepCopy());
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
