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
import java.util.List;
import java.util.Vector;

import parser.visitor.ASTVisitor;
import prism.PrismLangException;

// class to store list of labels

public class LabelList extends ASTElement
{
	// Name/expression pairs to define labels
	private List<String> names;
	private Vector<Expression> labels;
	// We also store an ExpressionIdent to match each name.
	// This is to just to provide positional info.
	private Vector<ExpressionIdent> nameIdents;
	
	// Constructor
	
	public LabelList()
	{
		names = new ArrayList<String>();
		labels = new Vector<Expression>();
		nameIdents = new Vector<ExpressionIdent>();
	}
	
	// Set methods
	
	public void addLabel(ExpressionIdent n, Expression l)
	{
		names.add(n.getName());
		labels.addElement(l);
		nameIdents.add(n);
	}
	
	public void setLabelName(int i , ExpressionIdent n)
	{
		names.set(i, n.getName());
		nameIdents.set(i, n);
	}
	
	public void setLabel(int i , Expression l)
	{
		labels.set(i, l);
	}
	
	// Get methods

	public int size()
	{
		return labels.size();
	}

	public String getLabelName(int i)
	{
		return names.get(i);
	}
	
	public List<String> getLabelNames()
	{
		return names;
	}
	
	public Expression getLabel(int i)
	{
		return labels.elementAt(i);
	}
	
	public ExpressionIdent getLabelNameIdent(int i)
	{
		return nameIdents.elementAt(i);
	}

	/**
	 * Get the index of a label by its name (returns -1 if it does not exist).
	 */
	public int getLabelIndex(String s)
	{
		return names.indexOf(s);
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
		int i, n;
		
		n = size();
		for (i = 0; i < n; i++) {
			s += "label \"" + getLabelName(i);
			s += "\" = " + getLabel(i) + ";\n";
		}
		
		return s;
	}
	
	/**
	 * Perform a deep copy.
	 */
	public ASTElement deepCopy()
	{
		int i, n;
		LabelList ret = new LabelList();
		n = size();
		for (i = 0; i < n; i++) {
			ret.addLabel((ExpressionIdent)getLabelNameIdent(i).deepCopy(), getLabel(i).deepCopy());
		}
		ret.setPosition(this);
		return ret;
	}
}

//------------------------------------------------------------------------------
