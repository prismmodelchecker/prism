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

import java.util.*;

import parser.*;
import parser.type.*;
import parser.visitor.*;
import prism.*;

// Abstract class for PRISM language AST elements

public abstract class ASTElement
{
	// Type - default to null (unknown)
	protected Type type = null;
	// Position in the file - default to -1s (unknown)
	protected int beginLine = -1;
	protected int beginColumn = -1;
	protected int endLine = -1;
	protected int endColumn = -1;

	// Set methods

	public void setType(Type t)
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

	/**
	 * Get the type for this element. It should have already been computed
	 * by calling typeCheck(). If not, it will be computed first but, in
	 * the case of error, you will get "unknown" (null) type, not the error.
	 */
	public Type getType()
	{
		if (type != null)
			return type;
		try {
			typeCheck();
		} catch (PrismLangException e) {
			// Returns null (unknown) in case of error.
			// If you want to check for errors, use typeCheck().
			return null;
		}
		return type;
	}

	/**
	 * Get the type for this element but, unlike getType(), don't call typeCheck()
	 * if it has not been computed yet - just return null instead.
	 */
	public Type getTypeIfDefined()
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
	 * @param formulaList The FormulaList for formula definitions
	 */
	public ASTElement expandFormulas(FormulaList formulaList) throws PrismLangException
	{
		return expandFormulas(formulaList, true);
	}

	/**
	 * Expand all formulas, return result.
	 * @param formulaList The FormulaList for formula definitions
	 * @param replace Whether to replace formulas outright with their definition
	 * (true for use in models since they may be subjected to renaming afterwards;
	 * false for properties since it is cleaner just to have the name there when displayed)
	 */
	public ASTElement expandFormulas(FormulaList formulaList, boolean replace) throws PrismLangException
	{
		ExpandFormulas visitor = new ExpandFormulas(formulaList, replace);
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
		Vector<String> v = new Vector<String>();
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
	* Get all undefined constants used (i.e. in ExpressionConstant objects) recursively and return as a list.
	* Recursive descent means that we also find constants that are used within other constants, labels, properties.
	* We only recurse into constants/labels/properties in the passed in lists.
	* Any others discovered are ignored (and not descended into).
	* ConstantList must be non-null so that we can determine which constants are undefined;
	* LabelList and PropertiesFile passed in as null are ignored.
	 */
	public Vector<String> getAllUndefinedConstantsRecursively(ConstantList constantList, LabelList labelList, PropertiesFile propertiesFile)
	{
		Vector<String> v = new Vector<String>();
		GetAllUndefinedConstantsRecursively visitor = new GetAllUndefinedConstantsRecursively(v, constantList, labelList, propertiesFile);
		try {
			accept(visitor);
		} catch (PrismLangException e) {
			// Should not happen; ignore. 
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
	 * Replace some constants with values.
	 * Note: This is the same as evaluatePartially(constantValues, null).
	 */
	public ASTElement replaceConstants(Values constantValues) throws PrismLangException
	{
		return evaluatePartially(new EvaluateContextValues(constantValues, null));
	}

	/**
	 * Find all references to variables, replace any identifier objects with variable objects,
	 * check variables exist and store their index (as defined by the containing ModuleFile).
	 */
	public ASTElement findAllVars(Vector<String> varIdents, Vector<Type> varTypes) throws PrismLangException
	{
		FindAllVars visitor = new FindAllVars(varIdents, varTypes);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Get all variables (i.e. ExpressionVar objects), store names in set.
	 */
	public Vector<String> getAllVars() throws PrismLangException
	{
		Vector<String> v = new Vector<String>();
		GetAllVars visitor = new GetAllVars(v);
		accept(visitor);
		return v;
	}

	/**
	 * Replace some variables with values.
	 * Note: This is the same as evaluatePartially(null, varValues).
	 */
	public ASTElement replaceVars(Values varValues) throws PrismLangException
	{
		return evaluatePartially(new EvaluateContextValues(null, varValues));
	}

	/**
	 * Get all labels (i.e. ExpressionLabel objects), store names in set.
	 * Special labels "deadlock", "init" *are* included in the list.
	 */
	public Vector<String> getAllLabels() throws PrismLangException
	{
		Vector<String> v = new Vector<String>();
		GetAllLabels visitor = new GetAllLabels(v);
		accept(visitor);
		return v;
	}

	/**
	 * Expand labels, return result.
	 * Special labels "deadlock", "init" and any not in list are left.
	 * @param labelList The LabelList for label definitions
	 */
	public ASTElement expandLabels(LabelList labelList) throws PrismLangException
	{
		ExpandLabels visitor = new ExpandLabels(labelList);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Find all references to properties (by name), replace the ExpressionLabels with ExpressionProp objects.
	 */
	public ASTElement findAllPropRefs(ModulesFile mf, PropertiesFile pf) throws PrismLangException
	{
		FindAllPropRefs visitor = new FindAllPropRefs(mf, pf);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Get all references to properties (by name) (i.e. ExpressionProp objects), store names in set.
	 */
	public Vector<String> getAllPropRefs() throws PrismLangException
	{
		Vector<String> v = new Vector<String>();
		GetAllPropRefs visitor = new GetAllPropRefs(v);
		accept(visitor);
		return v;
	}

	/**
	 * Get all references to properties (by name) (i.e. ExpressionProp objects) recursively, store names in set.
	 */
	public Vector<String> getAllPropRefsRecursively(PropertiesFile propertiesFile) throws PrismLangException
	{
		Vector<String> v = new Vector<String>();
		GetAllPropRefsRecursively visitor = new GetAllPropRefsRecursively(v, propertiesFile);
		accept(visitor);
		return v;
	}

	/**
	 * Expand property references and labels, return result.
	 * Property expansion is done recursively.
	 * Special labels "deadlock", "init" and any not in label list are left.
	 * @param propertiesFile The PropertiesFile for property lookup
	 * @param labelList The LabelList for label definitions
	 */
	public ASTElement expandPropRefsAndLabels(PropertiesFile propertiesFile, LabelList labelList) throws PrismLangException
	{
		ExpandPropRefsAndLabels visitor = new ExpandPropRefsAndLabels(propertiesFile, labelList);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Find all references to action labels, check they exist and, if required,
	 * store their index locally (as defined by the containing ModuleFile).
	 */
	public ASTElement findAllActions(List<String> synchs) throws PrismLangException
	{
		FindAllActions visitor = new FindAllActions(synchs);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Check for type-correctness and compute type.
	 * Passed in PropertiesFile might be needed to find types for property references.
	 */
	public void typeCheck(PropertiesFile propertiesFile) throws PrismLangException
	{
		TypeCheck visitor = new TypeCheck(propertiesFile);
		accept(visitor);
	}

	/**
	 * Check for type-correctness and compute type.
	 * If you are checking a property that might contain references to other properties, use {@link #typeCheck(PropertiesFile)}.
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
	 * Perform any required semantic checks. Optionally pass in parent ModulesFile
	 * and PropertiesFile for some additional checks (or leave null);
	 * These checks are done *before* any undefined constants have been defined.
	 */
	public void semanticCheck(ModulesFile modulesFile, PropertiesFile propertiesFile) throws PrismLangException
	{
		SemanticCheck visitor = new SemanticCheck(modulesFile, propertiesFile);
		accept(visitor);
	}

	/**
	 * Perform further semantic checks that can only be done once values
	 * for any undefined constants have been defined. Optionally pass in parent
	 * ModulesFile and PropertiesFile for some additional checks (or leave null);
	 */
	public void semanticCheckAfterConstants(ModulesFile modulesFile, PropertiesFile propertiesFile) throws PrismLangException
	{
		SemanticCheckAfterConstants visitor = new SemanticCheckAfterConstants(modulesFile, propertiesFile);
		accept(visitor);
	}

	/**
	 * Evaluate partially: replace some constants and variables with actual values.
	 * Warning: Unlike evaluate(), evaluatePartially() methods modify (and return) the expression. 
	 */
	public ASTElement evaluatePartially(EvaluateContext ec) throws PrismLangException
	{
		EvaluatePartially visitor = new EvaluatePartially(ec);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Evaluate partially: replace some constants and variables with actual values. 
	 * Constants/variables are specified as Values objects; either can be left null.
	 * Warning: Unlike evaluate(), evaluatePartially() methods modify (and return) the expression. 
	 */
	public ASTElement evaluatePartially(Values constantValues, Values varValues) throws PrismLangException
	{
		return evaluatePartially(new EvaluateContextValues(constantValues, varValues));
	}

	/**
	 * Evaluate partially: replace some variables with actual values. 
	 * Variables are specified as a State object, indexed over a subset of all variables,
	 * and a mapping from indices (over all variables) to this subset (-1 if not in subset). 
	 * Warning: Unlike evaluate(), evaluatePartially() methods modify (and return) the expression. 
	 */
	public ASTElement evaluatePartially(State substate, int[] varMap) throws PrismLangException
	{
		return evaluatePartially(new EvaluateContextSubstate(substate, varMap));
	}

	/**
	 * Simplify expressions (constant propagation, ...)
	 */
	public ASTElement simplify() throws PrismLangException
	{
		Simplify visitor = new Simplify();
		return (ASTElement) accept(visitor);
	}

	/**
	 * Compute (maximum) number of nested probabilistic operators (P, S, R).
	 */
	public int computeProbNesting() throws PrismLangException
	{
		ComputeProbNesting visitor = new ComputeProbNesting();
		accept(visitor);
		return visitor.getMaxNesting();
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
}

// ------------------------------------------------------------------------------
