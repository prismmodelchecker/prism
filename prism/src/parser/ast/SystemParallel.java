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

public class SystemParallel extends SystemDefn
{
	// Pair of operands
	private SystemDefn operand1;
	private SystemDefn operand2;
	// List of synchronising actions
	private ArrayList<String> actions;
	
	// Constructors
	
	public SystemParallel()
	{
		actions = new ArrayList<>();
	}
	
	public SystemParallel(SystemDefn s1, SystemDefn s2)
	{
		this();
		operand1 = s1;
		operand2 = s2;
	}
	
	// Set methods
	
	public void setOperand1(SystemDefn s1)
	{
		operand1 = s1;
	}
	
	public void setOperand2(SystemDefn s2)
	{
		operand2 = s2;
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
	
	public SystemDefn getOperand1()
	{
		return operand1;
	}
	
	public SystemDefn getOperand2()
	{
		return operand2;
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
		operand1.getModules(v);
		operand2.getModules(v);
	}

	@Override
	public void getModules(List<String> v, ModulesFile modulesFile)
	{
		operand1.getModules(v, modulesFile);
		operand2.getModules(v, modulesFile);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void getSynchs(List<String> v)
	{
		operand1.getSynchs(v);
		operand2.getSynchs(v);
	}
	
	@Override
	public void getSynchs(List<String> v, ModulesFile modulesFile)
	{
		operand1.getSynchs(v, modulesFile);
		operand2.getSynchs(v, modulesFile);
	}
	
	@Override
	public void getReferences(List<String> v)
	{
		operand1.getReferences(v);
		operand2.getReferences(v);
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
		
		s = s + operand1 + " |[";
		n = getNumActions();
		for (i = 0; i < n-1; i++) {
			s = s + getAction(i) + ",";
		}
		if (n > 0) {
			s = s + getAction(n-1);
		}
		s = s + "]| " + operand2;
		
		return s;
	}
	
	@Override
	public SystemParallel deepCopy(DeepCopy copier) throws PrismLangException
	{
		operand1 = copier.copy(operand1);
		operand2 = copier.copy(operand2);

		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public SystemParallel clone()
	{
		SystemParallel clone = (SystemParallel) super.clone();

		clone.actions = (ArrayList<String>) actions.clone();

		return clone;
	}
}

//------------------------------------------------------------------------------
