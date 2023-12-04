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

// class to store list of labels

public class LabelList extends ASTElement
{
	// Name/expression pairs to define labels
	protected ArrayList<String> names;
	protected ArrayList<Expression> labels;
	// We also store an ExpressionIdent to match each name.
	// This is to just to provide positional info.
	protected ArrayList<ExpressionIdent> nameIdents;
	
	// Constructor

	public LabelList()
	{
		names = new ArrayList<>();
		labels = new ArrayList<>();
		nameIdents = new ArrayList<>();
	}

	/**
	 * Checking all labels for cyclic dependencies. <br>
	 * Note: This method does not take formulas into account. Therefore, to prevent
	 * cycles completely, it is necessary to expand formulas beforehand.
	 *
	 * @throws PrismLangException In case a cyclic dependency was found.
	 */
	public void findCycles() throws PrismLangException
	{
		boolean[][] matrix = getLabelDependencies();
		// Check for and report dependencies
		int firstCycle = PrismUtils.findCycle(matrix);
		if (firstCycle != -1) {
			String s = "Cyclic dependency in definition of label \"" + names.get(firstCycle) + "\"";
			throw new PrismLangException(s, labels.get(firstCycle));
		}
	}

	/**
	 * 	Create a boolean matrix of dependencies between labels.
	 *
	 * @return a matrix m such that label i depends on label j iff m[i][j]
	 * @throws PrismLangException
	 */
	public boolean[][] getLabelDependencies() throws PrismLangException
	{
		// (matrix[i][j] is true if label i contains label j)
		int n = size();
		int j;
		boolean[][] matrix = new boolean[n][n];
		for (int i = 0; i < n; i++) {
			List<String> iLabels = labels.get(i).getAllLabels();
			for (j = 0; j < n; j++) {
				if (iLabels.contains(names.get(j))) {
					// label i contains j
					matrix[i][j] = true;
				}
			}
		}
		return matrix;
	}

	// Set methods
	public void addLabel(ExpressionIdent n, Expression l)
	{
		names.add(n.getName());
		labels.add(l);
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
		return labels.get(i);
	}

	public ExpressionIdent getLabelNameIdent(int i)
	{
		return nameIdents.get(i);
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

	@Override
	public LabelList deepCopy(DeepCopy copier) throws PrismLangException
	{
		copier.copyAll(labels);
		copier.copyAll(nameIdents);

		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public LabelList clone()
	{
		LabelList clone = (LabelList) super.clone();

		clone.names      = (ArrayList<String>)       	names.clone();
		clone.labels     = (ArrayList<Expression>)      labels.clone();
		clone.nameIdents = (ArrayList<ExpressionIdent>) nameIdents.clone();

		return clone;
	}
}

//------------------------------------------------------------------------------
