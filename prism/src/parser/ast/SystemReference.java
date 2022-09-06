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

public class SystemReference extends SystemDefn
{
	// Name of SystemDefn referenced
	private String name;
	
	// Constructors
	
	public SystemReference(String name)
	{
		this.name = name;
	}
	
	// Set method
	
	public void setName(String name)
	{
		this.name = name;
	}
	
	// Get method
	
	public String getName()
	{
		return name;
	}
	
	// Methods required for SystemDefn (all subclasses should implement):
	
	@Override
	public void getModules(Vector<String> v)
	{
		v.addElement(name);
	}
	
	@Override
	public void getModules(Vector<String> v, ModulesFile modulesFile)
	{
		// Recurse into referenced SystemDefn
		SystemDefn ref = modulesFile.getSystemDefnByName(name);
		if (ref != null) {
			ref.getModules(v, modulesFile);
		}
	}
	
	@Override
	public void getSynchs(Vector<String> v)
	{
		// do nothing
	}
	
	@Override
	public void getSynchs(Vector<String> v, ModulesFile modulesFile)
	{
		// Recurse into referenced SystemDefn
		SystemDefn ref = modulesFile.getSystemDefnByName(name);
		if (ref != null) {
			ref.getSynchs(v, modulesFile);
		}
	}
	
	@Override
	public void getReferences(Vector<String> v)
	{
		if (!v.contains(name))
			v.add(name);
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
		return "\"" + name + "\"";
	}
	
	@Override
	public SystemDefn deepCopy()
	{
		SystemDefn ret = new SystemReference(name);
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
