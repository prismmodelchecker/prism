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

import parser.*;
import parser.visitor.*;
import prism.PrismLangException;

// Class representing parsed properties file/list

public class PropertiesFile extends ASTElement
{
	// Associated ModulesFile (for constants, ...)
	private ModulesFile modulesFile;
	
	// Components
	private FormulaList formulaList;
	private LabelList labelList;
	private LabelList combinedLabelList; // Labels from both model/here
	private ConstantList constantList;
	private Vector<Expression> properties; // Properties
	private Vector<String> comments; // Property comments
	
	// list of all identifiers used
	private Vector<String> modulesFileIdents;
	private Vector<String> allIdentsUsed;
	
	// actual values of constants
	private Values constantValues;
	
	// Constructor
	
	public PropertiesFile(ModulesFile mf)
	{
		modulesFile = mf;
		modulesFileIdents = modulesFile.getAllIdentsUsed();
		formulaList = new FormulaList();
		labelList = new LabelList();
		combinedLabelList = new LabelList();
		constantList = new ConstantList();
		properties = new Vector<Expression>();
		comments = new Vector<String>();
		allIdentsUsed = new Vector<String>();
		constantValues = null;
	}
	
	// Set methods
	
	public void setFormulaList(FormulaList fl) { formulaList = fl; }
	
	public void setLabelList(LabelList ll) { labelList = ll; }
	
	public void setConstantList(ConstantList cl) { constantList = cl; }
	
	public void addProperty(Expression p, String c)
	{
		properties.addElement(p);
		comments.addElement(c);
	}
	
	public void setProperty(int i, Expression p) { properties.setElementAt(p, i); }
	
	// Get methods

	public FormulaList getFormulaList() { return formulaList; }
	
	public LabelList getLabelList() { return labelList; }
	
	public LabelList getCombinedLabelList() { return combinedLabelList; }
	
	public ConstantList getConstantList() { return constantList; }
	
	public int getNumProperties() { return properties.size(); }
	
	public Expression getProperty(int i) { return properties.elementAt(i); }
	
	public String getPropertyComment(int i) { return comments.elementAt(i); }
	
	// method to tidy up (called after parsing)
	
	public void tidyUp() throws PrismLangException
	{
		// Check formula identifiers
		checkFormulaIdents();
		// Find all instances of formulas (i.e. locate idents which are formulas),
		// check for any cyclic dependencies in the formula list and then expand all formulas.
		// Note: We have to look for formulas defined both here and in the model.
		findAllFormulas(modulesFile.getFormulaList());
		findAllFormulas(formulaList);
		formulaList.findCycles();
		expandFormulas(modulesFile.getFormulaList());
		expandFormulas(formulaList);
		
		// Check label identifiers
		checkLabelIdents();
		
		// Check constant identifiers
		checkConstantIdents();
		// Find all instances of constants (i.e. locate idents which are constants).
		// Note: We have to look in both constant lists
		findAllConstants(modulesFile.getConstantList());
		findAllConstants(constantList);

		// check constants for cyclic dependencies
		constantList.findCycles();
		
		// Find all instances of variables (i.e. locate idents which are variables).
		findAllVars(modulesFile.getVarNames(), modulesFile.getVarTypes());
		
		// Various semantic checks 
		semanticCheck(modulesFile, this);
		// Type checking
		typeCheck();
	}

	// check formula identifiers
	
	private void checkFormulaIdents() throws PrismLangException
	{
		int i, n;
		String s;

		n = formulaList.size();
		for (i = 0; i < n; i++) {
			s = formulaList.getFormulaName(i);
			// see if ident has been used elsewhere
			if (modulesFileIdents.contains(s)) {
				throw new PrismLangException("Identifier \"" + s + "\" already used in model file", formulaList.getFormulaNameIdent(i));
			}
			else if (allIdentsUsed.contains(s)) {
				throw new PrismLangException("Duplicated identifier \"" + s + "\"", formulaList.getFormulaNameIdent(i));
			}
			else {
				allIdentsUsed.addElement(s);
			}
		}
	}

	// check label identifiers
	// also check reference to these identifiers in properties
	
	private void checkLabelIdents() throws PrismLangException
	{
		int i, n;
		String s;
		Vector<String> labelIdents;
		LabelList mfLabels;
		
		// get label list from model file
		mfLabels = modulesFile.getLabelList();
		// add model file labels to combined label list (cloning them just for good measure)
		n = mfLabels.size();
		for (i = 0; i < n; i++) {
			combinedLabelList.addLabel(mfLabels.getLabelNameIdent(i), mfLabels.getLabel(i).deepCopy());
		}
		// go thru labels
		n = labelList.size();
		labelIdents = new Vector<String>();
		for (i = 0; i < n; i++) {
			s = labelList.getLabelName(i);
			// see if ident has been used already for a label in model file
			if (mfLabels.getLabelIndex(s) != -1) {
				throw new PrismLangException("Label \"" + s + "\" already defined in model file", labelList.getLabelNameIdent(i));
			}
			// see if ident has been used already for a label
			if (labelIdents.contains(s)) {
				throw new PrismLangException("Duplicated label name \"" + s + "\"", labelList.getLabelNameIdent(i));
			}
			// store identifier
			// and add label to combined list
			else {
				labelIdents.addElement(s);
				combinedLabelList.addLabel(labelList.getLabelNameIdent(i), labelList.getLabel(i));
			}
		}
	}

	// check constant identifiers
	
	private void checkConstantIdents() throws PrismLangException
	{
		int i, n;
		String s;
		
		// go thru constants
		n = constantList.size();
		for (i = 0; i < n; i++) {
			s = constantList.getConstantName(i);
			// see if ident has been used elsewhere
			if (modulesFileIdents.contains(s)) {
				throw new PrismLangException("Identifier \"" + s + "\" already used in model file", constantList.getConstantNameIdent(i));
			}
			else if (allIdentsUsed.contains(s)) {
				throw new PrismLangException("Duplicated identifier \"" + s + "\"", constantList.getConstantNameIdent(i));
			}
			else {
				allIdentsUsed.addElement(s);
			}
		}
	}

	// get undefined constants
	
	public Vector<String> getUndefinedConstants()
	{
		return constantList.getUndefinedConstants();
	}
	
	// set values for undefined constants and evaluate all constants
	// always need to call this, even when there are no undefined constants
	// (if this is the case, someValues can be null)
	
	public void setUndefinedConstants(Values someValues) throws PrismLangException
	{
		// might need values for ModulesFile constants too
		constantValues = constantList.evaluateConstants(someValues, modulesFile.getConstantValues());
	}
	
	// get all constant values
	
	public Values getConstantValues()
	{
		return constantValues;
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
		String s = "", tmp;
		int i, n;
		
		tmp = "" + formulaList;
		if (tmp.length() > 0) tmp += "\n";
		s += tmp;
		
		tmp = "" + labelList;
		if (tmp.length() > 0) tmp += "\n";
		s += tmp;
		
		tmp = "" + constantList;
		if (tmp.length() > 0) tmp += "\n";
		s += tmp;
		
		n = getNumProperties();
		for (i = 0; i < n; i++) {
			// add comment (if any)
			tmp = getPropertyComment(i);
			if (tmp != null) {
				if (tmp.length() > 0) {
					s += PrismParser.slashCommentBlock(tmp);
				}
			}
			s += getProperty(i) + "\n";
			if (i < n-1) s += "\n";
		}
		
		return s;
	}
	
	/**
	 * Perform a deep copy.
	 */
	public ASTElement deepCopy()
	{
		// Deep copy not required for whole properties file
		return null;
	}
}

//------------------------------------------------------------------------------
