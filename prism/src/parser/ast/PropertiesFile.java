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
import parser.IdentUsage;
import parser.Values;
import parser.visitor.ASTVisitor;
import parser.visitor.DeepCopy;
import parser.visitor.PropertiesSemanticCheck;
import prism.ModelInfo;
import prism.PrismException;
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
	private ArrayList<Property> properties; // Properties

	// Info about all identifiers used
	private IdentUsage identUsage;
	private IdentUsage quotedIdentUsage;

	// Copy of the evaluation context used to defined undefined constants (null if none)
	private EvaluateContext ecUndefined;
	
	// Actual values of (some or all) constants
	private Values constantValues;

	// Constructor

	public PropertiesFile(ModelInfo modelInfo)
	{
		setModelInfo(modelInfo);
		formulaList = new FormulaList();
		labelList = new LabelList();
		combinedLabelList = new LabelList();
		constantList = new ConstantList();
		properties = new ArrayList<>();
		identUsage = new IdentUsage();
		quotedIdentUsage = new IdentUsage(true);
		ecUndefined = null;
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
		properties.add(new Property(p, null, c));
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
	 * Check if an identifier is already used somewhere here or in the model
	 * (as a formula, constant or variable)
	 * and throw an exception if it is. Otherwise, add it to the list.
	 * @param ident The name of the (new) identifier
	 * @param decl Where the identifier is declared
	 * @param use Optionally, the identifier's usage (e.g. "constant")
	 */
	private void checkAndAddIdentifier(String ident, ASTElement decl, String use) throws PrismLangException
	{
		// Check model first
		modelInfo.checkIdent(ident, decl, use);
		// Then check/add here in the properties file
		identUsage.checkAndAddIdentifier(ident, decl, use, "the properties");
	}
	
	/**
	 * Check if an identifier is used by this properties file 
	 * (as a formula or constant)
	 */
	public boolean isIdentUsed(String ident)
	{
		return identUsage.isIdentUsed(ident);
	}

	/**
	 * Check if a quoted identifier is already used somewhere here or in the model
	 * (as a label or property name)
	 * and throw an exception if it is. Otherwise, add it to the list.
	 * @param ident The name of the (new) identifier
	 * @param decl Where the identifier is declared
	 * @param use Optionally, the identifier's usage (e.g. "constant")
	 */
	private void checkAndAddQuotedIdentifier(String ident, ASTElement decl, String use) throws PrismLangException
	{
		// Check model first
		modelInfo.checkQuotedIdent(ident, decl, use);
		// Then check/add here in the properties file
		quotedIdentUsage.checkAndAddIdentifier(ident, decl, use, "the properties");
	}
	
	// method to tidy up (called after parsing)

	public void tidyUp() throws PrismLangException
	{
		// Clear data that will generated by this method 
		// (in case it has already been called previously).
		identUsage.clear();

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

		// Find all references to observables (i.e. locate "labels" which are observables).
		findAllObsRefs(modelInfo.getObservableNames(), modelInfo.getObservableTypes());
		
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
		// NB: we use non-exact constant evaluation by default,
		// for exact mode constants will be reevaluated later on
		setSomeUndefinedConstants(EvaluateContext.create());
	}

	// check formula identifiers

	private void checkFormulaIdents() throws PrismLangException
	{
		int n = formulaList.size();
		for (int i = 0; i < n; i++) {
			String s = formulaList.getFormulaName(i);
			checkAndAddIdentifier(s, formulaList.getFormulaNameIdent(i), "formula");
		}
	}

	// check label identifiers
	// also check reference to these identifiers in properties

	private void checkLabelIdents() throws PrismLangException
	{
		// check for identifier clashes
		int n = labelList.size();
		for (int i = 0; i < n; i++) {
			String s = labelList.getLabelName(i);
			checkAndAddQuotedIdentifier(s, labelList.getLabelNameIdent(i), "label");
		}
		
		// build combined label list
		combinedLabelList = new LabelList();
		// first add model file labels to combined label list (cloning them just for good measure)
		LabelList mfLabels = modulesFile.getLabelList();
		n = mfLabels.size();
		for (int i = 0; i < n; i++) {
			combinedLabelList.addLabel(mfLabels.getLabelNameIdent(i), mfLabels.getLabel(i).deepCopy());
		}
		// then add labels from here
		n = labelList.size();
		for (int i = 0; i < n; i++) {
			combinedLabelList.addLabel(labelList.getLabelNameIdent(i), labelList.getLabel(i));
		}
	}

	// check constant identifiers

	private void checkConstantIdents() throws PrismLangException
	{
		// go thru constants
		int n = constantList.size();
		for (int i = 0; i < n; i++) {
			String s = constantList.getConstantName(i);
			checkAndAddIdentifier(s, constantList.getConstantNameIdent(i), "constant");
		}
	}

	/**
	 * Check for any duplicate property names (or clashes with labels).
	 */
	private void checkPropertyNames() throws PrismLangException
	{
		int n = properties.size();
		for (int i = 0; i < n; i++) {
			String s = properties.get(i).getName();
			if (s != null) {
				checkAndAddQuotedIdentifier(s, getPropertyObject(i), "property");
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
			List<String> v = e.getAllPropRefs();
			for (int j = 0; j < v.size(); j++) {
				int k = getPropertyIndexByName(v.get(j));
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
	public List<String> getUndefinedConstants()
	{
		return constantList.getUndefinedConstants();
	}

	/**
	 * Get a list of undefined (properties file) constants appearing in labels of the properties file
	 * (including those that appear in definitions of other needed constants)
	 * (undefined constants are those of form "const int x;" rather than "const int x = 1;")
	 */
	public List<String> getUndefinedConstantsUsedInLabels()
	{
		int i, n;
		Expression expr;
		List<String> consts, tmp;
		consts = new ArrayList<>();
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
	public List<String> getUndefinedConstantsUsedInProperty(Property prop)
	{
		return prop.getExpression().getAllUndefinedConstantsRecursively(constantList, combinedLabelList, this);
	}

	/**
	 * Get a list of undefined (properties file) constants used in a list of properties
	 * (including those that appear in definitions of other needed constants and labels/properties)
	 * (undefined constants are those of form "const int x;" rather than "const int x = 1;") 
	 */
	public List<String> getUndefinedConstantsUsedInProperties(List<Property> props)
	{
		List<String> consts, tmp;
		consts = new ArrayList<>();
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
	 * Set values for some undefined constants.
	 * The values being provided for these constants, as well as any other constants needed,
	 * are provided in an EvaluateContext object. This also determines the evaluation mode.
	 * If there are no undefined constants, {@code ecUndefined} can be null.
	 * Undefined constants can be subsequently redefined to different values with the same method.
	 * This may result in the values for other model constants now being known;
	 * the values for all current constant values (if set) are available via {@link #getConstantValues()}.
	 */
	public void setSomeUndefinedConstants(EvaluateContext ecUndefined) throws PrismLangException
	{
		this.ecUndefined = ecUndefined == null ? EvaluateContext.create() : EvaluateContext.create(ecUndefined);
		// Might need values for ModulesFile constants too
		EvaluateContext ecUndefinedPlusMF = EvaluateContext.create(this.ecUndefined).addConstantValues(modulesFile.getConstantValues());
		constantValues = constantList.evaluateSomeConstants(ecUndefinedPlusMF);
		// Note: unlike ModulesFile, we don't trigger any semantic checks at this point
		// This will usually be done on a per-property basis later
	}

	/**
	 * Set values for some undefined constants.
	 * It is preferable to use {@link #setSomeUndefinedConstants(EvaluateContext)} instead.
	 * By default, this method creates an {@link EvaluateContext} via {@link EvaluateContext#create(someValues)}.
	 * If this will be called frequently, it is better to maintain your own {@link EvaluateContext}.
	 * Also, this method can only handle the default (floating point) evaluation mode.
	 */
	public void setSomeUndefinedConstants(Values someValues) throws PrismException
	{
		setSomeUndefinedConstants(EvaluateContext.create(someValues));
	}

	/**
	 * Set values for some undefined constants.
	 * Deprecated. Better to use {@link #setSomeUndefinedConstants(EvaluateContext)}.
	 * @deprecated
	 */
	@Deprecated
	public void setSomeUndefinedConstants(Values someValues, boolean exact) throws PrismException
	{
		setSomeUndefinedConstants(EvaluateContext.create(someValues, exact));
	}

	/**
	 * Same as {@link #setSomeUndefinedConstants(Values)}.
	 * Note: This method no longer throws an exception if some constants are undefined.
	 * Deprecated: Just use {@link #setSomeUndefinedConstants(Values)}.
	 * @deprecated
	 */
	@Deprecated
	public void setUndefinedConstants(Values someValues) throws PrismException
	{
		setSomeUndefinedConstants(someValues);
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
	 * Get the evaluation context that was used to provide values for undefined constants in the model
	 * (e.g. via the method {@link #setSomeUndefinedConstants(EvaluateContext)}).
	 */
	public EvaluateContext getUndefinedEvaluateContext()
	{
		return ecUndefined;
	}

	/**
	 * Get access to the values for all constants in the properties file, including the
	 * undefined constants set previously via the method {@link #setSomeUndefinedConstants(Values)}
	 * If this has been called, constant values will have been evaluated assuming that there are no undefined constants.
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

	@Override
	public PropertiesFile deepCopy(DeepCopy copier) throws PrismLangException
	{
		quotedIdentUsage = new IdentUsage(true);
		labelList = copier.copy(labelList);
		formulaList = copier.copy(formulaList);
		constantList = copier.copy(constantList);
		combinedLabelList = copier.copy(combinedLabelList);
		identUsage = identUsage.deepCopy();

		copier.copyAll(properties);

		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public PropertiesFile clone()
	{
		PropertiesFile clone = (PropertiesFile) super.clone();

		// clone main components
		clone.properties = (ArrayList<Property>) properties.clone();

		// clone other (generated) info
		if (constantValues != null)
			clone.constantValues = constantValues.clone();
		if (ecUndefined != null)
			ecUndefined = EvaluateContext.create(ecUndefined);

		return clone;
	}
}

//------------------------------------------------------------------------------
