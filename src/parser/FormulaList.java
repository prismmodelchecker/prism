//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker
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

public class FormulaList
{
	private Vector names;
	private Vector formulas;
		
	// constructor
	
	public FormulaList()
	{
		// initialise
		names = new Vector();
		formulas = new Vector();
	}
	
	// set up methods
	
	public void addFormula(String n, Expression f)
	{
		names.addElement(n);
		formulas.addElement(f);
	}
	
	// accessor methods

	public int size()
	{
		return formulas.size();
	}

	public String getFormulaName(int i)
	{
		return (String)names.elementAt(i);
	}
	
	public Expression getFormula(int i)
	{
		return (Expression)formulas.elementAt(i);
	}
	
	// get the index of a formula by its name
	// (returns -1 if it does not exist)
	
	public int getFormulaIndex(String s)
	{
		return names.indexOf(s);
	}

	// find all formulas (i.e. locate idents which are formulas)
	// (inside the formulas in this formula list)
	
	public void findAllFormulas() throws PrismException
	{
		int i, n;
		
		n = formulas.size();
		for (i = 0; i < n; i++) {
			formulas.setElementAt(getFormula(i).findAllFormulas(this), i);
		}
	}

	// find cyclic dependencies
	
	public void findCycles() throws PrismException
	{
		int i, j, k, l, n, firstCycle = -1;
		Vector v;
		boolean matrix[][];
		boolean foundCycle = false;
		
		// initialise boolean matrix
		n = formulas.size();
		matrix = new boolean[n][n];
		for (i = 0; i < n; i++) {
			for (j = 0; j < n; j++) {
				matrix[i][j] = false;
			}
		}
		
		// determine which formulas contain which other formulas
		// and store this info in boolean matrix
		for (i = 0; i < n; i++) {
			v = new Vector();
			getFormula(i).getAllFormulas(v);
			for (j = 0; j < v.size(); j++) {
				k = getFormulaIndex((String)v.elementAt(j));
				if (k != -1) {
					matrix[i][k] = true;
				}
			}
		}
		
		// check for dependencies
		// (loop a maximum of n times)
		// (n = max length of possible cycle)
		for (i = 0 ; i < n; i++) {
			// see if there is a cycle yet
			for (j = 0; j < n; j++) {
				if (matrix[j][j]) {
					foundCycle = true;
					firstCycle = j;
					break;
				}
			}
			// if so, stop
			if (foundCycle) break;
			// extend dependencies
			for (j = 0; j < n; j++) {
				for (k = 0; k < n; k++) {
					if (matrix[j][k]) {
						for (l = 0; l < n; l++) {
							matrix[j][l] |= matrix[k][l];
						}
					}
				}
			}
		}
		
		// report dependency
		if (foundCycle) {
			String s = "Cyclic dependency found in formula \"" + getFormulaName(firstCycle) + "\"";
			throw new PrismException(s);
		}
	}
	
	// expand any formulas
	
	public void expandFormulas() throws PrismException
	{
		int i, n;
		
		n = formulas.size();
		for (i = 0; i < n; i++) {
			formulas.setElementAt(getFormula(i).expandFormulas(this), i);
		}
	}

	// convert to string
	
	public String toString()
	{
		String s = "";
		int i, n;
		
		n = formulas.size();
		for (i = 0; i < n; i++) {
			s += "formula " + names.elementAt(i);
			s += " = " + formulas.elementAt(i) + "\n";
		}
		
		return s;
	}
}

//------------------------------------------------------------------------------
