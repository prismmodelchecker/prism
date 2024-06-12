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

public class SystemRename extends SystemDefn
{
	// Operand
	private SystemDefn operand;
	// lists for pairs of actions to be renamed
	private ArrayList<String> from;
	private ArrayList<String> to;

	// Constructors

	public SystemRename()
	{
		from = new ArrayList<>();
		to = new ArrayList<>();
	}

	public SystemRename(SystemDefn s)
	{
		this();
		operand = s;
	}

	// Set methods

	public void setOperand(SystemDefn s)
	{
		operand = s;
	}

	public void addRename(String s1, String s2)
	{
		from.add(s1);
		to.add(s2);
	}

	public void setRename(int i, String s1, String s2)
	{
		from.set(i, s1);
		to.set(i, s2);
	}

	// Get methods

	public SystemDefn getOperand()
	{
		return operand;
	}

	public int getNumRenames()
	{
		return from.size();
	}

	public String getFrom(int i)
	{
		return from.get(i);
	}

	public String getTo(int i)
	{
		return to.get(i);
	}

	public String getNewName(String s)
	{
		int i;

		i = from.indexOf(s);
		if (i == -1) {
			return s;
		} else {
			return to.get(i);
		}
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
		int i, n;
		String s;

		// add action names in renames
		// (only look in 'to' list because we're only
		//  interested in actions _introduced_)
		n = getNumRenames();
		for (i = 0; i < n; i++) {
			s = getTo(i);
			if (!(v.contains(s)))
				v.add(s);
		}

		// recurse
		operand.getSynchs(v);
	}

	@Override
	public void getSynchs(List<String> v, ModulesFile modulesFile)
	{
		int i, n;
		String s;

		// add action names in renames
		// (only look in 'to' list because we're only
		//  interested in actions _introduced_)
		n = getNumRenames();
		for (i = 0; i < n; i++) {
			s = getTo(i);
			if (!(v.contains(s)))
				v.add(s);
		}

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

		s = s + operand + "{";
		n = getNumRenames();
		for (i = 0; i < n - 1; i++) {
			s = s + getFrom(i) + "<-" + getTo(i) + ",";
		}
		if (n > 0) {
			s = s + getFrom(n - 1) + "<-" + getTo(n - 1);
		}
		s = s + "}";

		return s;
	}

	@Override
	public SystemRename deepCopy(DeepCopy copier) throws PrismLangException
	{
		operand = copier.copy(operand);

		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public SystemRename clone()
	{
		SystemRename clone = (SystemRename) super.clone();

		clone.from = (ArrayList<String>) from.clone();
		clone.to   = (ArrayList<String>) to.clone();

		return clone;
	}
}

//------------------------------------------------------------------------------
