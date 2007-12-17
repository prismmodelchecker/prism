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

package parser;

import java.util.Vector;

import prism.PrismException;

// class to store list of formulas

public class LabelList
{
	private Vector names;
	private Vector labels;
		
	// constructor
	
	public LabelList()
	{
		// initialise
		names = new Vector();
		labels = new Vector();
	}
	
	// set up methods
	
	public void addLabel(String n, Expression l)
	{
		names.addElement(n);
		labels.addElement(l);
	}
	
	// accessor methods

	public int size()
	{
		return labels.size();
	}

	public String getLabelName(int i)
	{
		return (String)names.elementAt(i);
	}
	
	public Expression getLabel(int i)
	{
		return (Expression)labels.elementAt(i);
	}
	
	// get the index of a label by its name
	// (returns -1 if it does not exist)
	
	public int getLabelIndex(String s)
	{
		return names.indexOf(s);
	}

	// find all formulas (i.e. locate idents which are formulas)
	// (inside the expressions in this label list)
	
	public void findAllFormulas(FormulaList formulaList) throws PrismException
	{
		int i, n;
		Expression e;
		
		n = labels.size();
		for (i = 0; i < n; i++) {
			e = getLabel(i);
			labels.setElementAt(e.findAllFormulas(formulaList), i);
		}
	}

	// expand any formulas
	
	public void expandFormulas(FormulaList formulaList) throws PrismException
	{
		int i, n;
		Expression e;
		
		n = labels.size();
		for (i = 0; i < n; i++) {
			e = getLabel(i);
			labels.setElementAt(e.expandFormulas(formulaList), i);
		}
	}

	// find all constants (i.e. locate idents which are constants)
	
	public void findAllConstants(ConstantList constantList) throws PrismException
	{
		int i, n;
		Expression e;
		
		n = labels.size();
		for (i = 0; i < n; i++) {
			e = getLabel(i);
			labels.setElementAt(e.findAllConstants(constantList), i);
		}
	}

	// find all variables (i.e. locate idents which are variables)
	
	public void findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		int i, n;
		Expression e;
		
		n = labels.size();
		for (i = 0; i < n; i++) {
			e = getLabel(i);
			labels.setElementAt(e.findAllVars(varIdents, varTypes), i);
		}
	}

	// check everything is ok
	
	public void check() throws PrismException
	{
		int i, n;
		String s;
		Expression e;
		
		// go thru all labels
		n = labels.size();
		for (i = 0; i < n; i++) {
			// make sure name doesn't clash with a built-in one
			s = getLabelName(i);
			if ("deadlock".equals(s)) throw new PrismException("Cannot define a label called \"deadlock\" - this is a built-in label");
			if ("init".equals(s)) throw new PrismException("Cannot define a label called \"init\" - this is a built-in label");
			// is expression ok?
			e = getLabel(i);
			e.check();
			// and does it type check ok?
			if (e.getType() != Expression.BOOLEAN) {
				throw new PrismException("Label \"" + getLabelName(i) + "\" is not of type Boolean");
			}
		}
	}

	// convert to string
	
	public String toString()
	{
		String s = "";
		int i, n;
		
		n = labels.size();
		for (i = 0; i < n; i++) {
			s += "label \"" + names.elementAt(i);
			s += "\" = " + labels.elementAt(i) + "\n";
		}
		
		return s;
	}

	// get parse trees as string
	
	public String toTreeString()
	{
		String s = "";
		int i, n;
		
		n = labels.size();
		for (i = 0; i < n; i++) {
			s += "label \"" + names.elementAt(i);
			s += "\" = " + labels.elementAt(i) + "\n";
			s += ((Expression)labels.elementAt(i)).toTreeString(0);
		}
		
		return s;
	}
}

//------------------------------------------------------------------------------
