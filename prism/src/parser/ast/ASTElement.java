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

import java.util.Vector;

import parser.Token;
import parser.visitor.*;
import prism.PrismLangException;
import simulator.SimulatorEngine;

// Abstract class for PRISM language AST elements

public abstract class ASTElement
{
	// Type - default to 0 (unknown)
	protected int type = 0;
	// Position in the file - default to -1s (unknown)
	protected int beginLine = -1;
	protected int beginColumn = -1;
	protected int endLine = -1;
	protected int endColumn = -1;

	// Set methods

	public void setType(int t)
	{
		type = t;
	}

	public void setBeginColumn(int beginColumn)
	{
		this.beginColumn = beginColumn;
	}

	public void setBeginLine(int beginLine)
	{
		this.beginLine = beginLine;
	}

	public void setEndColumn(int endColumn)
	{
		this.endColumn = endColumn;
	}

	public void setEndLine(int endLine)
	{
		this.endLine = endLine;
	}

	public void setPosition(int beginLine, int beginColumn, int endLine, int endColumn)
	{
		this.beginLine = beginLine;
		this.beginColumn = beginColumn;
		this.endLine = endLine;
		this.endColumn = endColumn;
	}

	public void setPosition(Token begin, Token end)
	{
		this.beginLine = begin.beginLine;
		this.beginColumn = begin.beginColumn;
		this.endLine = end.endLine;
		this.endColumn = end.endColumn;
	}

	public void setPosition(Token token)
	{
		setPosition(token, token);
	}

	public void setPosition(ASTElement begin, ASTElement end)
	{
		this.beginLine = begin.getBeginLine();
		this.beginColumn = begin.getBeginColumn();
		this.endLine = end.getEndLine();
		this.endColumn = end.getEndColumn();
	}

	public void setPosition(ASTElement e)
	{
		setPosition(e, e);
	}

	// Get methods

	public int getType()
	{
		return type;
	}

	public boolean hasPosition()
	{
		return beginLine != -1;
	}

	public int getBeginLine()
	{
		return beginLine;
	}

	public int getBeginColumn()
	{
		return beginColumn;
	}

	public String getBeginString()
	{
		return "line " + beginLine + ", column " + beginColumn;
	}

	public int getEndLine()
	{
		return endLine;
	}

	public int getEndColumn()
	{
		return endColumn;
	}

	public String getEndString()
	{
		return "line " + endLine + ", column " + endColumn;
	}

	// Methods required for ASTElement (all subclasses should implement):

	/**
	 * Visitor method.
	 */
	public abstract Object accept(ASTVisitor v) throws PrismLangException;

	/**
	 * Convert to string.
	 */
	public abstract String toString();

	/**
	 * Perform a deep copy.
	 */
	public abstract ASTElement deepCopy();

	// Various methods based on AST traversals (implemented using the visitor
	// pattern):

	/**
	 * Find all idents which are formulas, replace with ExpressionFormula,
	 * return result.
	 */
	public ASTElement findAllFormulas(FormulaList formulaList) throws PrismLangException
	{
		FindAllFormulas visitor = new FindAllFormulas(formulaList);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Expand all formulas, return result.
	 */
	public ASTElement expandFormulas(FormulaList formulaList) throws PrismLangException
	{
		ExpandFormulas visitor = new ExpandFormulas(formulaList);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Get all formulas (i.e. ExpressionFormula objects), store names in set.
	 */
	public Vector<String> getAllFormulas() throws PrismLangException
	{
		Vector<String> v = new Vector<String>();
		GetAllFormulas visitor = new GetAllFormulas(v);
		accept(visitor);
		return v;
	}

	/**
	 * Rename (according to RenamedModule definition), return result.
	 */
	public ASTElement rename(RenamedModule rm) throws PrismLangException
	{
		Rename visitor = new Rename(rm);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Find all idents which are constants, replace with ExpressionConstant,
	 * return result.
	 */
	public ASTElement findAllConstants(ConstantList constantList) throws PrismLangException
	{
		FindAllConstants visitor = new FindAllConstants(constantList);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Get all constants (i.e. ExpressionConstant objects), store names in set.
	 */
	public Vector<String> getAllConstants()
	{
		Vector<String> v= new Vector<String>();
		GetAllConstants visitor = new GetAllConstants(v);
		try {
			accept(visitor);
		} catch (PrismLangException e) {
			// GetAllConstants never throws an exception
			// (but base traversal class is defined so that it can)
		}
		return v;
	}

	/**
	 * Expand all constants, return result.
	 */
	public ASTElement expandConstants(ConstantList constantList) throws PrismLangException
	{
		ExpandConstants visitor = new ExpandConstants(constantList);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Find all idents which are variables, replace with ExpressionVar, return
	 * result. Also make sure all variable references (e.g. in updates) are
	 * valid.
	 */
	public ASTElement findAllVars(Vector varIdents, Vector varTypes) throws PrismLangException
	{
		FindAllVars visitor = new FindAllVars(varIdents, varTypes);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Get all variables (i.e. ExpressionVar objects), store names in set.
	 */
	public Vector<String> getAllVars() throws PrismLangException
	{
		Vector<String> v =new Vector<String>();
		GetAllVars visitor = new GetAllVars(v);
		accept(visitor);
		return v;
	}

	/**
	 * Check for type-correctness and compute type.
	 */
	public void typeCheck() throws PrismLangException
	{
		TypeCheck visitor = new TypeCheck();
		accept(visitor);
	}

	/**
	 * Perform any required semantic checks.
	 */
	public void semanticCheck() throws PrismLangException
	{
		semanticCheck(null, null);
	}

	/**
	 * Perform any required semantic checks.
	 */
	public void semanticCheck(ModulesFile modulesFile) throws PrismLangException
	{
		semanticCheck(modulesFile, null);
	}

	/**
	 * Perform any required semantic checks. Optionally pass in parent
	 * ModulesFile and PropertiesFile for some additional checks (or leave
	 * null);
	 */
	public void semanticCheck(ModulesFile modulesFile, PropertiesFile propertiesFile) throws PrismLangException
	{
		SemanticCheck visitor = new SemanticCheck(modulesFile, propertiesFile);
		accept(visitor);
	}

	/**
	 * Convert to string showing tree representation.
	 */
	public String toTreeString()
	{
		ToTreeString visitor = new ToTreeString();
		try {
			accept(visitor);
		} catch (PrismLangException e) {
			return e.toString();
		}
		return visitor.getString();
	}

	/**
	 * Construct corresponding data structures in the simulator and return
	 * pointer.
	 */
	public long toSimulator(SimulatorEngine sim) throws PrismLangException
	{
		ToSimulator visitor = new ToSimulator(sim);
		accept(visitor);
		return visitor.getPtr(this);
	}
}

// ------------------------------------------------------------------------------
