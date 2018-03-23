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
import parser.visitor.*;
import prism.ModelInfo;
import prism.PrismLangException;
import prism.PrismUtils;

// Class representing parsed properties file/list

public class PropertiesFile extends ASTElement
{
	// Associated ModulesFile (for constants, ...)
	private ModulesFile modulesFile;
	private ModelInfo modelInfo;

	// Components
	private FormulaList formulaList;
	private LabelList labelList;
	private LabelList combinedLabelList; // Labels from both model/here
	private ConstantList constantList;
	private Vector<Property> properties; // Properties

	// list of all identifiers used
	private Vector<String> allIdentsUsed;

	// actual values of (some or all) constants
	private Values constantValues;

	// Constructor

	public PropertiesFile(ModelInfo modelInfo)
	{
		setModelInfo(modelInfo);
		formulaList = new FormulaList();
		labelList = new LabelList();
		combinedLabelList = new LabelList();
		constantList = new ConstantList();
		properties = new Vector<Property>();
		allIdentsUsed = new Vector<String>();
		constantValues = null;
	}

	// Set methods

	/** Attach model information (so can access labels/constants etc.) */
	public void setModelInfo(ModelInfo modelInfo)
	{
		// Store ModelInfo. Need a ModulesFile too for now. Create a dummy one if needed.
		if (modelInfo  == null) {
			this.modelInfo = this.modulesFile = new ModulesFile();
			this.modulesFile.setFormulaList(new FormulaList());
			this.modulesFile.setConstantList(new ConstantList());
		} else if (modelInfo instanceof ModulesFile) {
			this.modelInfo = this.modulesFile = (ModulesFile) modelInfo;
		} else {
			this.modelInfo = modelInfo;
			this.modulesFile = new ModulesFile();
			this.modulesFile.setFormulaList(new FormulaList());
			this.modulesFile.setConstantList(new ConstantList());
		}
	}

	public void setFormulaList(FormulaList fl)
	{
		formulaList = fl;
	}

	public void setLabelList(LabelList ll)
	{
		labelList = ll;
	}

	public void setConstantList(ConstantList cl)
	{
		constantList = cl;
	}

	public void addProperty(Property prop)
	{
		properties.add(prop);
	}

	public void addProperty(Expression p, String c)
	{
		properties.addElement(new Property(p, null, c));
	}

	public void setPropertyObject(int i, Property prop)
	{
		properties.set(i, prop);
	}

	public void setPropertyExpression(int i, Expression p)
	{
		properties.get(i).setExpression(p);
	}

	/**
	 * Insert the contents of another PropertiesFile (just a shallow copy).
	 */
	public void insertPropertiesFile(PropertiesFile pf) throws PrismLangException
	{
		FormulaList fl;
		LabelList ll;
		ConstantList cl;
		int i, n;
		fl = pf.formulaList;
		n = fl.size();
		for (i = 0; i < n; i++) {
			formulaList.addFormula(fl.getFormulaNameIdent(i), fl.getFormula(i));
		}
		ll = pf.labelList;
		n = ll.size();
		for (i = 0; i < n; i++) {
			labelList.addLabel(ll.getLabelNameIdent(i), ll.getLabel(i));
		}
		cl = pf.constantList;
		n = cl.size();
		for (i = 0; i < n; i++) {
			constantList.addConstant(cl.getConstantNameIdent(i), cl.getConstant(i), cl.getConstantType(i));
		}
		n = pf.properties.size();
		for (i = 0; i < n; i++) {
			properties.add(pf.properties.get(i));
		}
		// Need to re-tidy (some checks should be re-done, some new info created)
		tidyUp();
	}

	// Get methods

	public FormulaList getFormulaList()
	{
		return formulaList;
	}

	public LabelList getLabelList()
	{
		return labelList;
	}

	public LabelList getCombinedLabelList()
	{
		return combinedLabelList;
	}

	public ConstantList getConstantList()
	{
		return constantList;
	}

	public int getNumProperties()
	{
		return properties.size();
	}

	public Property getPropertyObject(int i)
	{
		return properties.get(i);
	}

	public Expression getProperty(int i)
	{
		return properties.get(i).getExpression();
	}

	public String getPropertyName(int i)
	{
		return properties.get(i).getName();
	}

	public String getPropertyComment(int i)
	{
		return properties.get(i).getComment();
	}

	/**
	 * Look up a property by name from those listed in this properties file.
	 * (Use {@link #lookUpPropertyObjectByName} to search model file too)
	 * Returns null if not found.
	 */
	public Property getPropertyObjectByName(String name)
	{
		int i, n;
		n = getNumProperties();
		for (i = 0; i < n; i++) {
			if (name.equals(getPropertyName(i))) {
				return getPropertyObject(i);
			}
		}
		return null;
	}

	/**
	 * Look up the index of a property by name from those listed in this properties file.
	 * Returns -1 if not found.
	 */
	public int getPropertyIndexByName(String name)
	{
		int i, n;
		n = getNumProperties();
		for (i = 0; i < n; i++) {
			if (name.equals(getPropertyName(i))) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Look up a property by name, currently just locally like {@link #getPropertyObjectByName}.
	 * Returns null if not found.
	 */
	public Property lookUpPropertyObjectByName(String name)
	{
		return getPropertyObjectByName(name);
	}

	/**
	 * Check if an identifier is used by this properties file 
	 * (as a formula or constant)
	 */
	public boolean isIdentUsed(String ident)
	{
		return allIdentsUsed.contains(ident);
	}

	// method to tidy up (called after parsing)

	public void tidyUp() throws PrismLangException
	{
		// Clear lists that will generated by this method 
		// (in case it has already been called previously).
		allIdentsUsed.clear();

		// Check formula identifiers
		checkFormulaIdents();
		// Find all instances of formulas (i.e. locate idents which are formulas),
		// check for any cyclic dependencies in the formula list and then expand all formulas.
		// Note: We have to look for formulas defined both here and in the model.
		// Note also that we opt not to do actual replacement of formulas in calls to exandFormulas
		// (to improve legibility of properties)
		findAllFormulas(modulesFile.getFormulaList());
		findAllFormulas(formulaList);
		formulaList.findCycles();
		expandFormulas(modulesFile.getFormulaList(), false);
		expandFormulas(formulaList, false);

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

		// Check property names
		checkPropertyNames();

		// Find all instances of variables (i.e. locate idents which are variables).
		findAllVars(modelInfo.getVarNames(), modelInfo.getVarTypes());

		// Find all instances of property refs
		findAllPropRefs(null, this);
		// Check property references for cyclic dependencies
		findCyclesInPropertyReferences();

		// Various semantic checks 
		doSemanticChecks();
		// Type checking
		typeCheck(this);

		// Set up some values for constants
		// (without assuming any info about undefined constants)
		//
		// we use non-exact constant evaluation by default,
		// for exact mode constants will be reevaluated later on
		setSomeUndefinedConstants(null, false);
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
			if (modulesFile.isIdentUsed(s)) {
				throw new PrismLangException("Identifier \"" + s + "\" already used in model file", formulaList.getFormulaNameIdent(i));
			} else if (isIdentUsed(s)) {
				throw new PrismLangException("Duplicated identifier \"" + s + "\"", formulaList.getFormulaNameIdent(i));
			} else {
				allIdentsUsed.add(s);
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
			if (modulesFile.isIdentUsed(s)) {
				throw new PrismLangException("Identifier \"" + s + "\" already used in model file", constantList.getConstantNameIdent(i));
			} else if (isIdentUsed(s)) {
				throw new PrismLangException("Duplicated identifier \"" + s + "\"", constantList.getConstantNameIdent(i));
			} else {
				allIdentsUsed.add(s);
			}
		}
	}

	/**
	 * Check for any duplicate property names (or clashes with labels).
	 */
	private void checkPropertyNames() throws PrismLangException
	{
		int i, n;
		String s;
		Vector<String> propNames;
		LabelList mfLabels;

		// get label list from model file
		mfLabels = modulesFile.getLabelList();
		// Go thru properties
		n = properties.size();
		propNames = new Vector<String>();
		for (i = 0; i < n; i++) {
			s = properties.get(i).getName();
			if (s == null)
				continue;
			// see if ident has been used already for a label in model file
			if (mfLabels.getLabelIndex(s) != -1) {
				throw new PrismLangException("Property name \"" + s + "\" clashes with label in model file", getPropertyObject(i));
			}
			// see if ident has been used already for a label in properties file
			if (labelList.getLabelIndex(s) != -1) {
				throw new PrismLangException("Property name \"" + s + "\" clashes with label", getPropertyObject(i));
			}
			// see if ident has been used already for a property name
			if (propNames.contains(s)) {
				throw new PrismLangException("Duplicated property name \"" + s + "\"", getPropertyObject(i));
			}
			// store identifier
			else {
				propNames.addElement(s);
			}
		}
	}

	/**
	 * Find cyclic dependencies in property references.
	 */
	public void findCyclesInPropertyReferences() throws PrismLangException
	{
		// Create boolean matrix of dependencies
		// (matrix[i][j] is true if prop i contains a ref to prop j)
		int n = properties.size();
		boolean matrix[][] = new boolean[n][n];
		for (int i = 0; i < n; i++) {
			Expression e = properties.get(i).getExpression();
			Vector<String> v = e.getAllPropRefs();
			for (int j = 0; j < v.size(); j++) {
				int k = getPropertyIndexByName(v.elementAt(j));
				if (k != -1) {
					matrix[i][k] = true;
				}
			}
		}
		// Check for and report dependencies
		int firstCycle = PrismUtils.findCycle(matrix);
		if (firstCycle != -1) {
			String s = "Cyclic dependency in property references from property \"" + getPropertyName(firstCycle) + "\"";
			throw new PrismLangException(s, getPropertyObject(firstCycle));
		}
	}

	/**
	  * Perform any required semantic checks.
	  * These checks are done *before* any undefined constants have been defined.
	 */
	private void doSemanticChecks() throws PrismLangException
	{
		PropertiesSemanticCheck visitor = new PropertiesSemanticCheck(this, modelInfo);
		accept(visitor);
	}

	/**
	 * Get a list of all undefined constants in the properties files
	 * ("const int x;" rather than "const int x = 1;") 
	 */
	public Vector<String> getUndefinedConstants()
	{
		return constantList.getUndefinedConstants();
	}

	/**
	 * Get a list of undefined (properties file) constants appearing in labels of the properties file
	 * (including those that appear in definitions of other needed constants)
	 * (undefined constants are those of form "const int x;" rather than "const int x = 1;")
	 */
	public Vector<String> getUndefinedConstantsUsedInLabels()
	{
		int i, n;
		Expression expr;
		Vector<String> consts, tmp;
		consts = new Vector<String>();
		n = labelList.size();
		for (i = 0; i < n; i++) {
			expr = labelList.getLabel(i);
			tmp = expr.getAllUndefinedConstantsRecursively(constantList, combinedLabelList, null);
			for (String s : tmp) {
				if (!consts.contains(s)) {
					consts.add(s);
				}
			}
		}
		return consts;
	}

	/**
	 * Get a list of undefined (properties file) constants used in a property
	 * (including those that appear in definitions of other needed constants and labels/properties)
	 * (undefined constants are those of form "const int x;" rather than "const int x = 1;") 
	 */
	public Vector<String> getUndefinedConstantsUsedInProperty(Property prop)
	{
		return prop.getExpression().getAllUndefinedConstantsRecursively(constantList, combinedLabelList, this);
	}

	/**
	 * Get a list of undefined (properties file) constants used in a list of properties
	 * (including those that appear in definitions of other needed constants and labels/properties)
	 * (undefined constants are those of form "const int x;" rather than "const int x = 1;") 
	 */
	public Vector<String> getUndefinedConstantsUsedInProperties(List<Property> props)
	{
		Vector<String> consts, tmp;
		consts = new Vector<String>();
		for (Property prop : props) {
			tmp = prop.getExpression().getAllUndefinedConstantsRecursively(constantList, combinedLabelList, this);
			for (String s : tmp) {
				if (!consts.contains(s)) {
					consts.add(s);
				}
			}
		}
		return consts;
	}

	/**
	 * Set values for *all* undefined constants and then evaluate all constants.
	 * If there are no undefined constants, {@code someValues} can be null.
	 * Undefined constants can be subsequently redefined to different values with the same method.
	 * The current constant values (if set) are available via {@link #getConstantValues()}.
	 * <br>
	 * Constant values are evaluated using standard (integer, floating-point) arithmetic.
	 */
	public void setUndefinedConstants(Values someValues) throws PrismLangException
	{
		setUndefinedConstants(someValues, false);
	}

	/**
	 * Set values for *all* undefined constants and then evaluate all constants.
	 * If there are no undefined constants, {@code someValues} can be null.
	 * Undefined constants can be subsequently redefined to different values with the same method.
	 * The current constant values (if set) are available via {@link #getConstantValues()}.
	 * <br>
	 * Constant values are evaluated using either standard (integer, floating-point) arithmetic
	 * or exact arithmetic, depending on the value of the {@code exact} flag.
	 */
	public void setUndefinedConstants(Values someValues, boolean exact) throws PrismLangException
	{
		// Might need values for ModulesFile constants too
		constantValues = constantList.evaluateConstants(someValues, modulesFile.getConstantValues(), exact);
		// Note: unlike ModulesFile, we don't trigger any semantic checks at this point
		// This will usually be done on a per-property basis later
	}

	/**
	 * Set values for *some* undefined constants and then evaluate all constants where possible.
	 * If there are no undefined constants, {@code someValues} can be null.
	 * Undefined constants can be subsequently redefined to different values with the same method.
	 * The current constant values (if set) are available via {@link #getConstantValues()}.
	 * <br>
	 * Constant values are evaluated using standard (integer, floating-point) arithmetic.
	 */
	public void setSomeUndefinedConstants(Values someValues) throws PrismLangException
	{
		setSomeUndefinedConstants(someValues, false);
	}

	/**
	 * Set values for *some* undefined constants and then evaluate all constants where possible.
	 * If there are no undefined constants, {@code someValues} can be null.
	 * Undefined constants can be subsequently redefined to different values with the same method.
	 * The current constant values (if set) are available via {@link #getConstantValues()}.
	 * <br>
	 * Constant values are evaluated using either standard (integer, floating-point) arithmetic
	 * or exact arithmetic, depending on the value of the {@code exact} flag.
	 */
	public void setSomeUndefinedConstants(Values someValues, boolean exact) throws PrismLangException
	{
		// Might need values for ModulesFile constants too
		constantValues = constantList.evaluateSomeConstants(someValues, modulesFile.getConstantValues(), exact);
		// Note: unlike ModulesFile, we don't trigger any semantic checks at this point
		// This will usually be done on a per-property basis later
	}

	/**
	 * Check if {@code name} is a *defined* constant in the properties file,
	 * i.e. a constant whose value was *not* left unspecified.
	 */
	public boolean isDefinedConstant(String name)
	{
		return constantList.isDefinedConstant(name);
	}

	/**
	 * Get access to the values for all constants in the properties file, including the
	 * undefined constants set previously via the method {@link #setUndefinedConstants(Values)}
	 * or {@link #setUndefinedConstants(Values)}. If neither method has been called
	 * constant values will have been evaluated assuming that there are no undefined constants.
	 */
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
		if (tmp.length() > 0)
			tmp += "\n";
		s += tmp;

		tmp = "" + labelList;
		if (tmp.length() > 0)
			tmp += "\n";
		s += tmp;

		tmp = "" + constantList;
		if (tmp.length() > 0)
			tmp += "\n";
		s += tmp;

		n = getNumProperties();
		for (i = 0; i < n; i++) {
			s += getPropertyObject(i) + ";\n";
			if (i < n - 1)
				s += "\n";
		}

		return s;
	}

	/**
	 * Perform a deep copy.
	 */
	@SuppressWarnings("unchecked")
	public ASTElement deepCopy()
	{
		int i, n;
		PropertiesFile ret = new PropertiesFile(modelInfo);
		// Copy ASTElement stuff
		ret.setPosition(this);
		// Deep copy main components
		ret.setFormulaList((FormulaList) formulaList.deepCopy());
		ret.setLabelList((LabelList) labelList.deepCopy());
		ret.combinedLabelList = (LabelList) combinedLabelList.deepCopy();
		ret.setConstantList((ConstantList) constantList.deepCopy());
		n = getNumProperties();
		for (i = 0; i < n; i++) {
			ret.addProperty((Property) getPropertyObject(i).deepCopy());
		}
		// Copy other (generated) info
		ret.allIdentsUsed = (allIdentsUsed == null) ? null : (Vector<String>) allIdentsUsed.clone();
		ret.constantValues = (constantValues == null) ? null : new Values(constantValues);

		return ret;
	}
}

//------------------------------------------------------------------------------
