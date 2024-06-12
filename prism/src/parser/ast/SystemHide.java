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

import java.util.ArrayList;
import java.util.List;

import parser.visitor.ASTVisitor;
import parser.visitor.DeepCopy;
import prism.PrismLangException;

public class SystemHide extends SystemDefn
{
	// Operand
	private SystemDefn operand;
	// Actions to be hidden
	private ArrayList<String> actions;
	
	// Constructors
	
	public SystemHide()
	{
		actions = new ArrayList<>();
	}
	
	public SystemHide(SystemDefn s)
	{
		this();
		operand = s;
	}
	
	// Set methods
	
	public void setOperand(SystemDefn s)
	{
		operand = s;
	}
	
	public void addAction(String s)
	{
		actions.add(s);
	}
		
	public void setAction(int i, String s)
	{
		actions.set(i, s);
	}
	
	// Get methods
	
	public SystemDefn getOperand()
	{
		return operand;
	}
	
	public int getNumActions()
	{
		return actions.size();
	}
	
	public String getAction(int i)
	{
		return actions.get(i);
	}
		
	public boolean containsAction(String s)
	{
		return actions.contains(s);
	}
	
	// Methods required for SystemDefn (all subclasses should implement):
	
	@Override
	@SuppressWarnings("deprecation")
	public void getModules(List<String> v)
	{
		operand.getModules(v);
	}
	
	@Override
	public void getModules(List<String> v, ModulesFile modulesFile)
	{
		operand.getModules(v, modulesFile);
	}
	
	@Override
	@SuppressWarnings("deprecation")
	public void getSynchs(List<String> v)
	{
		// recurse
		operand.getSynchs(v);
	}
	
	@Override
	public void getSynchs(List<String> v, ModulesFile modulesFile)
	{
		// recurse
		operand.getSynchs(v, modulesFile);
	}
	
	@Override
	public void getReferences(List<String> v)
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
		int i, n;
		String s = "";
		
		s = s + operand + "/{";
		n = getNumActions();
		for (i = 0; i < n-1; i++) {
			s = s + getAction(i) + ",";
		}
		if (n > 0) {
			s = s + getAction(n-1);
		}
		s = s + "}";
		
		return s;
	}
	
	@Override
	public SystemHide deepCopy(DeepCopy copier) throws PrismLangException
	{
		operand = copier.copy(operand);

		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public SystemHide clone()
	{
		SystemHide clone = (SystemHide) super.clone();

		clone.actions = (ArrayList<String>) actions.clone();

		return clone;
	}
}

//------------------------------------------------------------------------------
