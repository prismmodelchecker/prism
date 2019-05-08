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

import java.util.ArrayList;

import parser.visitor.*;
import prism.PrismLangException;

public class RenamedModule extends ASTElement
{
	// Rename info
	private String name;
	private String baseModule;
	private ArrayList<String> oldNames;
	private ArrayList<String> newNames;
	// AST elements (for positional info)
	private ExpressionIdent nameASTElement;
	private ExpressionIdent baseModuleASTElement;
	private ArrayList<ExpressionIdent> oldNameASTElements;
	private ArrayList<ExpressionIdent> newNameASTElements;
	
	// Constructor
	
	public RenamedModule(String n, String b)
	{
		name = n;
		baseModule = b;
		oldNames = new ArrayList<String>();
		newNames = new ArrayList<String>();
		oldNameASTElements = new ArrayList<ExpressionIdent>();
		newNameASTElements = new ArrayList<ExpressionIdent>();
	}
	
	// Set methods
	
	public void setName(String n)
	{
		name = n;
	}

	public void setBaseModule(String b)
	{
		baseModule = b;
	}
	
	public void setNameASTElement(ExpressionIdent e)
	{
		nameASTElement = e;
	}
	
	public void setBaseModuleASTElement(ExpressionIdent e)
	{
		baseModuleASTElement = e;
	}
	
	public void addRename(String s1, String s2)
	{
		addRename(s1, s2, null, null);
	}
		
	public void addRename(String s1, String s2, ExpressionIdent e1, ExpressionIdent e2)
	{
		oldNames.add(s1);
		newNames.add(s2);
		oldNameASTElements.add(e1);
		newNameASTElements.add(e2);
	}
		
	// Get methods
	
	public String getName()
	{
		return name;
	}
	
	public String getBaseModule()
	{
		return baseModule;
	}

	public String getNewName(String s)
	{
		int i = oldNames.indexOf(s);
		if (i == -1) {
			return null;
		}
		return newNames.get(i);
	}
		
	public String getOldName(String s)
	{
		int i = newNames.indexOf(s);
		if (i == -1) {
			return null;
		}
		return oldNames.get(i);
	}
	
	public int getNumRenames()
	{
		return oldNames.size();
	}
	
	public String getOldName(int i)
	{
		return oldNames.get(i); 
	}
	
	public String getNewName(int i)
	{
		return newNames.get(i); 
	}
	
	public ExpressionIdent getNameASTElement()
	{
		return nameASTElement;
	}
	
	public ExpressionIdent getBaseModuleASTElement()
	{
		return baseModuleASTElement;
	}
	
	public ExpressionIdent getOldNameASTElement(int i)
	{
		return oldNameASTElements.get(i);
	}
	
	public ExpressionIdent getNewNameASTElement(int i)
	{
		return newNameASTElements.get(i);
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
		String s = "";
		int i;
		
		s = s + "module " + name + " = " + baseModule + " [";
		for (i = 0; i < oldNames.size() - 1; i++) {
			s = s + oldNames.get(i) + " = " + newNames.get(i) + ", ";
		}
		i = oldNames.size() - 1;
		s = s + oldNames.get(i) + " = " + newNames.get(i) + "] endmodule";
		
		return s;
	}

	@Override
	public RenamedModule deepCopy(DeepCopy copier) throws PrismLangException
	{
		nameASTElement       = copier.copy(nameASTElement);
		baseModuleASTElement = copier.copy(baseModuleASTElement);
		copier.copyAll(newNameASTElements);
		copier.copyAll(oldNameASTElements);

		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public RenamedModule clone()
	{
		RenamedModule clone = (RenamedModule) super.clone();

		clone.newNameASTElements = (ArrayList<ExpressionIdent>) newNameASTElements.clone();
		clone.newNames           = (ArrayList<String>) newNames.clone();
		clone.oldNameASTElements = (ArrayList<ExpressionIdent>) oldNameASTElements.clone();
		clone.oldNames           = (ArrayList<String>) oldNames.clone();
		
		return clone;
	}
}

//------------------------------------------------------------------------------
