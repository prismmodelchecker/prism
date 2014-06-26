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

public class SystemModule extends SystemDefn
{
	// Module name
	String name;
	
	// Constructors
	
	public SystemModule()
	{
	}
	
	public SystemModule(String n)
	{
		name = n;
	}
	
	// Set method
	
	public void setName(String n)
	{
		name = n;
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
		v.addElement(name);
	}
	
	@Override
	public void getSynchs(Vector<String> v)
	{
		// do nothing
	}
	
	@Override
	public void getSynchs(Vector<String> v, ModulesFile modulesFile)
	{
		// do nothing
	}
	
	@Override
	public void getReferences(Vector<String> v)
	{
		// do nothing
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
		return name;
	}
	
	@Override
	public SystemDefn deepCopy()
	{
		SystemDefn ret = new SystemModule(name);
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
