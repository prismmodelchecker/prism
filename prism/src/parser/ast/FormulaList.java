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

import parser.visitor.ASTVisitor;
import parser.visitor.DeepCopy;
import prism.PrismLangException;
import prism.PrismUtils;

// class to store list of formulas

public class FormulaList extends ASTElement
{
	// Name/expression pairs to define formulas
	private ArrayList<String> names;
	private ArrayList<Expression> formulas;
	// We also store an ExpressionIdent to match each name.
	// This is to just to provide positional info.
	private ArrayList<ExpressionIdent> nameIdents;

	// Constructor

	public FormulaList()
	{
		names = new ArrayList<>();
		formulas = new ArrayList<>();
		nameIdents = new ArrayList<>();
	}

	// Set methods

	public void addFormula(ExpressionIdent n, Expression f)
	{
		names.add(n.getName());
		formulas.add(f);
		nameIdents.add(n);
	}

	public void setFormulaName(int i, ExpressionIdent n)
	{
		names.set(i, n.getName());
		nameIdents.set(i, n);
	}

	public void setFormula(int i, Expression f)
	{
		formulas.set(i, f);
	}

	// Get methods

	public int size()
	{
		return formulas.size();
	}

	public String getFormulaName(int i)
	{
		return names.get(i);
	}

	public Expression getFormula(int i)
	{
		return formulas.get(i);
	}

	public ExpressionIdent getFormulaNameIdent(int i)
	{
		return nameIdents.get(i);
	}

	/**
	 * Get the index of a formula by its name (returns -1 if it does not exist).
	 */
	public int getFormulaIndex(String s)
	{
		return names.indexOf(s);
	}

	/**
	 * Find cyclic dependencies.
	 */
	public void findCycles() throws PrismLangException
	{
		// Create boolean matrix of dependencies
		// (matrix[i][j] is true if formula i contains formula j)
		int n = size();
		boolean matrix[][] = new boolean[n][n];
		for (int i = 0; i < n; i++) {
			List<String> v = getFormula(i).getAllFormulas();
			for (int j = 0; j < v.size(); j++) {
				int k = getFormulaIndex(v.get(j));
				if (k != -1) {
					matrix[i][k] = true;
				}
			}
		}
		// Check for and report dependencies
		int firstCycle = PrismUtils.findCycle(matrix);
		if (firstCycle != -1) {
			String s = "Cyclic dependency in definition of formula \"" + getFormulaName(firstCycle) + "\"";
			throw new PrismLangException(s, getFormula(firstCycle));
		}
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
			s += "formula " + getFormulaName(i);
			s += " = " + getFormula(i) + ";\n";
		}

		return s;
	}

	@Override
	public FormulaList deepCopy(DeepCopy copier) throws PrismLangException
	{
		copier.copyAll(formulas);
		copier.copyAll(nameIdents);

		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public FormulaList clone()
	{
		FormulaList clone = (FormulaList) super.clone();

		clone.names      = (ArrayList<String>)          names.clone();
		clone.formulas   = (ArrayList<Expression>)      formulas.clone();
		clone.nameIdents = (ArrayList<ExpressionIdent>) nameIdents.clone();

		return clone;
	}
}

// ------------------------------------------------------------------------------
