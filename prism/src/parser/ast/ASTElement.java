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

import parser.EvaluateContext;
import parser.EvaluateContextConstants;
import parser.EvaluateContextState;
import parser.EvaluateContextSubstate;
import parser.EvaluateContextValues;
import parser.State;
import parser.Token;
import parser.Values;
import parser.type.Type;
import parser.visitor.ASTTraverse;
import parser.visitor.ASTVisitor;
import parser.visitor.ComputeProbNesting;
import parser.visitor.DeepCopy;
import parser.visitor.EvaluatePartially;
import parser.visitor.ExpandConstants;
import parser.visitor.ExpandFormulas;
import parser.visitor.ExpandLabels;
import parser.visitor.ExpandPropRefsAndLabels;
import parser.visitor.FindAllActions;
import parser.visitor.FindAllConstants;
import parser.visitor.FindAllFormulas;
import parser.visitor.FindAllObsRefs;
import parser.visitor.FindAllPropRefs;
import parser.visitor.FindAllVars;
import parser.visitor.GetAllConstants;
import parser.visitor.GetAllFormulas;
import parser.visitor.GetAllLabels;
import parser.visitor.GetAllPropRefs;
import parser.visitor.GetAllPropRefsRecursively;
import parser.visitor.GetAllUndefinedConstantsRecursively;
import parser.visitor.GetAllVars;
import parser.visitor.Rename;
import parser.visitor.SemanticCheck;
import parser.visitor.Simplify;
import parser.visitor.ToTreeString;
import parser.visitor.TypeCheck;
import prism.PrismLangException;

// Abstract class for PRISM language AST elements

public abstract class ASTElement implements Cloneable
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

	/**
	 * Remove any positional info, i.e., set begin/end line/column numbers to -1
	 */
	public void clearPosition()
	{
		setPosition(-1, -1, -1, -1);
	}

	/**
	 * Remove any positional info, i.e., set begin/end line/column numbers to -1,
	 * in this ASTElement and all of its children.
	 */
	public void clearPositionRecursively()
	{
		try {
			// NB: Don't need ASTTraverse since the structure is never changed
			accept(new ASTTraverse()
			{
				@Override
				public void defaultVisitPost(ASTElement e) throws PrismLangException
				{
					clearPosition();
				}
			});
		} catch (PrismLangException e) {
			// Ignore any errors during traversal
		}
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
	public ASTElement deepCopy()
	{
		try {
			return new DeepCopy().copy(this);
		} catch (PrismLangException e) {
			throw new Error(e);
		}
	}

	/**
	 * Perform a deep copy of all internal ASTElements using a deep copy visitor.
	 * This method is usually called after {@code clone()} and must return the receiver.
	 *
	 * @param copier the copy visitor
	 * @return the receiver with deep-copied subcomponents
	 * @throws PrismLangException
	 * @see #clone()
	 */
	public abstract ASTElement deepCopy(DeepCopy copier) throws PrismLangException;

	/**
	 * Perform a shallow copy of the receiver and
	 * clone all internal containers, e.g., lists and vectors, too.
	 */
	@Override
	public ASTElement clone()
	{
		try {
			return (ASTElement) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError("Object#clone is expected to work for Cloneable objects", e);
		}
	}

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
	public List<String> getAllFormulas() throws PrismLangException
	{
		List<String> v = new ArrayList<>();
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
	 * Find all idents which are constants, replace with ExpressionConstant, return result.
	 */
	public ASTElement findAllConstants(ConstantList constantList) throws PrismLangException
	{
		FindAllConstants visitor = new FindAllConstants(constantList);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Find all idents which are constants, replace with ExpressionConstant, return result.
	 */
	public ASTElement findAllConstants(List<String> constIdents, List<Type> constTypes) throws PrismLangException
	{
		FindAllConstants visitor = new FindAllConstants(constIdents, constTypes);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Get all constants (i.e. ExpressionConstant objects), store names in set.
	 */
	public List<String> getAllConstants()
	{
		List<String> v = new ArrayList<>();
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
	public List<String> getAllUndefinedConstantsRecursively(ConstantList constantList, LabelList labelList, PropertiesFile propertiesFile)
	{
		List<String> v = new ArrayList<>();
		GetAllUndefinedConstantsRecursively visitor = new GetAllUndefinedConstantsRecursively(v, constantList, labelList, propertiesFile);
		try {
			accept(visitor);
		} catch (PrismLangException e) {
			// Should not happen; ignore. 
		}
		return v;
	}

	/**
	 * Expand constants whose definitions are contained in the supplied ConstantList.
	 * Throw an exception if constants are found with no definitions.
	 * @param constantList The ConstantList containing definitions
	 */
	public ASTElement expandConstants(ConstantList constantList) throws PrismLangException
	{
		return expandConstants(constantList, true);
	}

	/**
	 * Expand constants whose definitions are contained in the supplied ConstantList.
	 * @param constantList The ConstantList containing definitions
	 * @param all If true, an exception is thrown if any constants are undefined
	 */
	public ASTElement expandConstants(ConstantList constantList, boolean all) throws PrismLangException
	{
		ExpandConstants visitor = new ExpandConstants(constantList, all);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Replace some constants with values.
	 * Note: This is the same as evaluatePartially(constantValues, null).
	 */
	public ASTElement replaceConstants(Values constantValues) throws PrismLangException
	{
		return evaluatePartially(new EvaluateContextConstants(constantValues));
	}

	/**
	 * Find all references to variables, replace any identifier objects with variable objects,
	 * check variables exist and store their index (as defined by the containing ModuleFile).
	 */
	public ASTElement findAllVars(List<String> varIdents, List<Type> varTypes) throws PrismLangException
	{
		FindAllVars visitor = new FindAllVars(varIdents, varTypes);
		return (ASTElement) accept(visitor);
	}

	/**
	 * Get all variables (i.e. ExpressionVar objects), store names in set.
	 */
	public List<String> getAllVars() throws PrismLangException
	{
		List<String> v = new ArrayList<>();
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
	public List<String> getAllLabels() throws PrismLangException
	{
		List<String> v = new ArrayList<>();
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
	public List<String> getAllPropRefs() throws PrismLangException
	{
		List<String> v = new ArrayList<>();
		GetAllPropRefs visitor = new GetAllPropRefs(v);
		accept(visitor);
		return v;
	}

	/**
	 * Get all references to properties (by name) (i.e. ExpressionProp objects) recursively, store names in set.
	 */
	public List<String> getAllPropRefsRecursively(PropertiesFile propertiesFile) throws PrismLangException
	{
		List<String> v = new ArrayList<>();
		GetAllPropRefsRecursively visitor = new GetAllPropRefsRecursively(v, propertiesFile);
		accept(visitor);
		return v;
	}

	/**
	 * Find all references to observables (by name), replace the ExpressionLabels with ExpressionObs objects.
	 */
	public ASTElement findAllObsRefs(List<String> observableNames, List<Type> observableTypes) throws PrismLangException
	{
		FindAllObsRefs visitor = new FindAllObsRefs(observableNames, observableTypes);
		return (ASTElement) accept(visitor);
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
	 * Perform any required semantic checks. These are just simple checks on expressions, mostly.
	 * For semantic checks on models and properties, specifically, see:
	 * {@link parser.visitor.ModulesFileSemanticCheck} and {@link parser.visitor.PropertiesSemanticCheck}. 
	 * These checks are done *before* any undefined constants have been defined.
	 */
	public void semanticCheck() throws PrismLangException
	{
		SemanticCheck visitor = new SemanticCheck();
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
	 * Evaluate partially: replace some constants with actual values. 
	 * Constants are specified as a Values object.
	 * Warning: Unlike evaluate(), evaluatePartially() methods modify (and return) the expression. 
	 */
	public ASTElement evaluatePartially(Values constantValues) throws PrismLangException
	{
		return evaluatePartially(new EvaluateContextValues(constantValues, null));
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
	 * Evaluate partially: replace variables with actual values, specified as a State object.
	 * Warning: Unlike evaluate(), evaluatePartially() methods modify (and return) the expression. 
	 */
	public ASTElement evaluatePartially(State state) throws PrismLangException
	{
		return evaluatePartially(new EvaluateContextState(state));
	}

	/**
	 * Evaluate partially: replace variables with actual values, specified as a State object.
	 * Constant values are supplied as a Values object. 
	 * Warning: Unlike evaluate(), evaluatePartially() methods modify (and return) the expression. 
	 */
	public ASTElement evaluatePartially(Values constantValues, State state) throws PrismLangException
	{
		return evaluatePartially(new EvaluateContextState(constantValues, state));
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
	 * Compute (maximum) number of nested probabilistic operators (P, S, R).
	 * Optionally, pass a properties file for looking up property references.
	 */
	public int computeProbNesting(PropertiesFile propertiesFile) throws PrismLangException
	{
		ComputeProbNesting visitor = new ComputeProbNesting(propertiesFile);
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
