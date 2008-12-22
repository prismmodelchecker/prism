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
import prism.PrismLangException;

// Class representing parsed model file

public class ModulesFile extends ASTElement
{
	// constants for types of system
	public static final int PROBABILISTIC = 1;
	public static final int NONDETERMINISTIC = 2;
	public static final int STOCHASTIC = 3;
	public static final String typeStrings[] = { "?", "Probabilistic (DTMC)", "Nondeterministic (MDP)",
			"Stochastic (CTMC)" };
	private int type; // type: prob/nondet/stoch

	// Model components
	private FormulaList formulaList;
	private LabelList labelList;
	private ConstantList constantList;
	private Vector<Declaration> globals; // Global variables
	private Vector<Object> modules; // Modules (includes renamed modules)
	private SystemDefn systemDefn; // System definition (system...endsystem
	// construct)
	private ArrayList<RewardStruct> rewardStructs; // Rewards structures
	private Expression initStates; // Initial states specification

	// identifiers/etc.
	private Vector<String> allIdentsUsed;
	private String[] moduleNames;
	private Vector<String> synchs;
	private Vector<String> varNames;
	private Vector<Integer> varTypes;

	// actual values of constants
	private Values constantValues;

	// Constructor

	public ModulesFile()
	{
		formulaList = new FormulaList();
		labelList = new LabelList();
		constantList = new ConstantList();
		type = NONDETERMINISTIC; // default type
		globals = new Vector<Declaration>();
		modules = new Vector<Object>();
		systemDefn = null;
		rewardStructs = new ArrayList<RewardStruct>();
		initStates = null;
		allIdentsUsed = new Vector<String>();
		varNames = new Vector<String>();
		varTypes = new Vector<Integer>();
		constantValues = null;
	}

	// Set methods

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

	public void setType(int t)
	{
		type = t;
	}

	public void addGlobal(Declaration d)
	{
		globals.addElement(d);
	}

	public void setGlobal(int i, Declaration d)
	{
		globals.set(i, d);
	}

	public void addModule(Module m)
	{
		modules.addElement(m);
		m.setParent(this);
	}

	public void setModule(int i, Module m)
	{
		modules.set(i, m);
		m.setParent(this);
	}

	public void addRenamedModule(RenamedModule m)
	{
		modules.addElement(m);
	}

	public void setSystemDefn(SystemDefn s)
	{
		systemDefn = s;
	}

	public void addRewardStruct(RewardStruct r)
	{
		rewardStructs.add(r);
	}

	public void setRewardStruct(int i, RewardStruct r)
	{
		rewardStructs.set(i, r);
	}

	// this method is included for backwards compatibility only
	public void setRewardStruct(RewardStruct r)
	{
		rewardStructs.clear();
		rewardStructs.add(r);
	}

	public void setInitialStates(Expression e)
	{
		initStates = e;
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

	public ConstantList getConstantList()
	{
		return constantList;
	}

	public int getType()
	{
		return type;
	}

	public String getTypeString()
	{
		return typeStrings[type];
	}

	public int getNumGlobals()
	{
		return globals.size();
	}

	public Declaration getGlobal(int i)
	{
		return globals.elementAt(i);
	}

	public int getNumModules()
	{
		return modules.size();
	}

	// get module by index
	// returns null if it is a RenamedModule
	// these will have been replaced with Modules after tidyUp()
	public Module getModule(int i)
	{
		Object o = modules.elementAt(i);
		return (o instanceof Module) ? (Module) o : null;
	}

	// get the index of a module by its name
	// (returns -1 if it does not exist)
	// (or is a RenamedModule and hasn't been turned into a normal Module yet)
	public int getModuleIndex(String s)
	{
		int i;
		Module m;

		for (i = 0; i < modules.size(); i++) {
			m = getModule(i);
			if (m != null)
				if (s.equals(m.getName())) {
					return i;
				}
		}

		return -1;
	}

	public SystemDefn getSystemDefn()
	{
		return systemDefn;
	}

	public int getNumRewardStructs()
	{
		return rewardStructs.size();
	}

	// Get a reward structure by its index
	// (indexed from 0, not from 1 like at the user (property language) level)

	public RewardStruct getRewardStruct(int i)
	{
		return (i < rewardStructs.size()) ? rewardStructs.get(i) : null;
	}

	// Get the index of a module by its name
	// (indexed from 0, not from 1 like at the user (property language) level)

	public int getRewardStructIndex(String name)
	{
		int i, n;
		n = rewardStructs.size();
		for (i = 0; i < n; i++) {
			if ((rewardStructs.get(i)).getName().equals(name))
				return i;
		}
		return -1;
	}

	// this method is included for backwards compatibility only
	public RewardStruct getRewardStruct()
	{
		return getRewardStruct(0);
	}

	public Expression getInitialStates()
	{
		return initStates;
	}

	public Vector<String> getAllIdentsUsed()
	{
		return allIdentsUsed;
	}

	// get individual module name
	public String getModuleName(int i)
	{
		return moduleNames[i];
	}

	// get array of all module names
	public String[] getModuleNames()
	{
		return moduleNames;
	}

	public Vector<String> getSynchs()
	{
		return synchs;
	}

	public boolean isSynch(String s)
	{
		if (synchs == null)
			return false;
		else
			return synchs.contains(s);
	}

	public Vector<String> getVarNames()
	{
		return varNames;
	}

	public Vector<Integer> getVarTypes()
	{
		return varTypes;
	}

	public boolean isGlobalVariable(String s)
	{
		int i, n;

		n = getNumGlobals();
		for (i = 0; i < n; i++) {
			if (getGlobal(i).getName().equals(s))
				return true;
		}
		return false;
	}

	// Method to tidy up
	// (called after parsing to do some checks and extract some information)

	public void tidyUp() throws PrismLangException
	{
		// Expansion of formulas and renaming

		// Check formula identifiers
		checkFormulaIdents();
		// Find all formulas (i.e. locate idents which are formulas).
		// Note: This should all be done before replacing any other identifiers
		// (e.g. with vars) because this relies on module renaming which in turn
		// must be done after formula expansion. Then, check for any cyclic
		// dependencies in the formula list and then expand all formulas.
		findAllFormulas(formulaList);
		formulaList.findCycles();
		expandFormulas(formulaList);
		// Perform module renaming
		sortRenamings();

		// check label identifiers
		checkLabelIdents();

		// check module names
		checkModuleNames();

		// get synch names
		getSynchNames();

		// check constant identifiers
		checkConstantIdents();
		// find all instances of constants
		// (i.e. locate idents which are constants)
		findAllConstants(constantList);
		// check constants for cyclic dependencies
		constantList.findCycles();

		// Check variable names
		checkVarNames();
		// Find all instances of variables (i.e. locate idents which are
		// variables).
		findAllVars(varNames, varTypes);

		// check reward struct names
		checkRewardStructNames();

		// Various semantic checks 
		semanticCheck(this);
		// Type checking
		typeCheck();
	}

	// Check formula identifiers

	private void checkFormulaIdents() throws PrismLangException
	{
		int i, n;
		String s;

		n = formulaList.size();
		for (i = 0; i < n; i++) {
			s = formulaList.getFormulaName(i);
			if (allIdentsUsed.contains(s)) {
				throw new PrismLangException("Duplicated identifier \"" + s + "\"", formulaList.getFormulaNameIdent(i));
			} else {
				allIdentsUsed.addElement(s);
			}
		}
	}

	// sort out modules defined by renaming

	private void sortRenamings() throws PrismLangException
	{
		int i, j, n, i2, n2;
		RenamedModule module;
		Module newModule;
		String s;
		Object o;
		HashSet<String> renamedSoFar;

		// Go through modules and find ones which are defined by renaming
		n = modules.size();
		for (i = 0; i < n; i++) {
			o = modules.elementAt(i);
			if (o instanceof Module)
				continue;
			module = (RenamedModule) o;
			// Check base module exists
			j = getModuleIndex(module.getBaseModule());
			if (j == -1) {
				s = "No such module " + module.getBaseModule();
				s += " in renamed module \"" + module.getName() + "\"";
				throw new PrismLangException(s, module.getBaseModuleASTElement());
			}
			// Check for invalid renames
			n2 = module.getNumRenames();
			renamedSoFar = new HashSet<String>();
			for (i2 = 0; i2 < n2; i2++) {
				s = module.getOldName(i2);
				if (!renamedSoFar.add(s)) {
					throw new PrismLangException("Identifier \""+s+"\" is renamed more than once in module \""+module.getName()+"\"", module.getOldNameASTElement(i2));
				}
				if (formulaList.getFormulaIndex(s) != -1) {
					throw new PrismLangException("Formula \""+s+"\" cannot be renamed since formulas are expanded before module renaming", module.getOldNameASTElement(i2));
				}
			}
			// Then rename (a copy of) base module and replace
			newModule = (Module) getModule(j).deepCopy().rename(module);
			newModule.setNameASTElement(module.getNameASTElement());
			setModule(i, newModule);
		}
	}

	// check label identifiers

	private void checkLabelIdents() throws PrismLangException
	{
		int i, n;
		String s;
		Vector<String> labelIdents;

		// go thru labels
		n = labelList.size();
		labelIdents = new Vector<String>();
		for (i = 0; i < n; i++) {
			s = labelList.getLabelName(i);
			// see if ident has been used already for a label
			if (labelIdents.contains(s)) {
				throw new PrismLangException("Duplicated label name \"" + s + "\"", labelList.getLabelNameIdent(i));
			} else {
				labelIdents.addElement(s);
			}
		}
	}

	// check module names

	private void checkModuleNames() throws PrismLangException
	{
		int i, j, n;
		String s;

		// check we have at least one module
		n = modules.size();
		if (n == 0) {
			throw new PrismLangException("There must be at least one module");
		}

		// compile list of all module names
		// and check as we go through
		moduleNames = new String[n];
		for (i = 0; i < n; i++) {
			s = getModule(i).getName();
			for (j = 0; j < i; j++) {
				if (s.equals(moduleNames[j])) {
					throw new PrismLangException("Duplicated module name \"" + s + "\"", getModule(i).getNameASTElement());
				}
			}
			moduleNames[i] = s;
		}
	}

	// get all synch names

	private void getSynchNames() throws PrismLangException
	{
		Vector<String> v;
		String s;
		int i, j, n, m;

		// create vector to store names
		synchs = new Vector<String>();

		// go thru modules and extract names which appear in their commands
		n = modules.size();
		for (i = 0; i < n; i++) {
			v = getModule(i).getAllSynchs();
			m = v.size();
			for (j = 0; j < m; j++) {
				s = v.elementAt(j);
				if (!synchs.contains(s)) {
					synchs.addElement(s);
				}
			}
		}

		// then extract any which are introduced in system construct (i.e. by
		// renaming)
		if (systemDefn != null) {
			systemDefn.getSynchs(synchs);
		}
	}

	// check constant identifiers

	private void checkConstantIdents() throws PrismLangException
	{
		int i, n;
		String s;

		n = constantList.size();
		for (i = 0; i < n; i++) {
			s = constantList.getConstantName(i);
			if (allIdentsUsed.contains(s)) {
				throw new PrismLangException("Duplicated identifier \"" + s + "\"", constantList.getConstantNameIdent(i));
			} else {
				allIdentsUsed.addElement(s);
			}
		}
	}

	// check variable names

	private void checkVarNames() throws PrismLangException
	{
		int i, j, n, m;
		Module module;
		String s;

		// compile list of all var names
		// and check as we go through

		// globals
		n = getNumGlobals();
		for (i = 0; i < n; i++) {
			s = getGlobal(i).getName();
			if (allIdentsUsed.contains(s)) {
				throw new PrismLangException("Duplicated identifier \"" + s + "\"", getGlobal(i));
			} else {
				allIdentsUsed.addElement(s);
				varNames.addElement(s);
				varTypes.addElement(getGlobal(i).getType());
			}
		}

		// locals
		n = modules.size();
		for (i = 0; i < n; i++) {
			module = getModule(i);
			m = module.getNumDeclarations();
			for (j = 0; j < m; j++) {
				s = module.getDeclaration(j).getName();
				if (allIdentsUsed.contains(s)) {
					throw new PrismLangException("Duplicated identifier \"" + s + "\"",module.getDeclaration(j));
				} else {
					allIdentsUsed.addElement(s);
					varNames.addElement(s);
					varTypes.addElement(module.getDeclaration(j).getType());
				}
			}
		}

		// check there is at least one variable
		if (varNames.size() == 0) {
			throw new PrismLangException("There must be at least one variable");
		}
	}

	// Check there are no duplicate names labelling reward structs

	private void checkRewardStructNames() throws PrismLangException
	{
		int i, n;
		String s;
		HashSet<String> names = new HashSet<String>();
		n = getNumRewardStructs();
		for (i = 0; i < n; i++) {
			s = getRewardStruct(i).getName();
			if (s != null && !"".equals(s))
				if (!names.add(s))
					throw new PrismLangException("Duplicated reward structure name \"" + s + "\"", getRewardStruct(i));
		}
	}

	// get undefined constants

	public Vector getUndefinedConstants()
	{
		return constantList.getUndefinedConstants();
	}

	// set values for undefined constants and evaluate all constants
	// always need to call this, even when there are no undefined constants
	// (if this is the case, someValues can be null)

	public void setUndefinedConstants(Values someValues) throws PrismLangException
	{
		constantValues = constantList.evaluateConstants(someValues, null);
	}

	// get all constant values

	public Values getConstantValues()
	{
		return constantValues;
	}

	// create a Values object to represent (a unique) initial state

	public Values getInitialValues() throws PrismLangException
	{
		int i, j, n, n2;
		Module module;
		Declaration decl;
		Values values;

		if (initStates != null) {
			throw new PrismLangException("There are multiple initial states");
		}

		// set up variable list
		values = new Values();

		// first add all globals
		n = getNumGlobals();
		for (i = 0; i < n; i++) {
			decl = getGlobal(i);
			values.addValue(decl.getName(), decl.getStart(this).evaluate(constantValues, null));
		}
		// then add all module variables
		n = getNumModules();
		for (i = 0; i < n; i++) {
			module = getModule(i);
			n2 = module.getNumDeclarations();
			for (j = 0; j < n2; j++) {
				decl = module.getDeclaration(j);
				values.addValue(decl.getName(), decl.getStart(this).evaluate(constantValues, null));
			}
		}

		return values;
	}

	// Extract information about all variables and return in a VarList object.
	// Note: various checks, e.g. for duplicate var names, have already been done. 

	public VarList createVarList() throws PrismLangException
	{
		int i, j, n, n2, low, high, start;
		String name;
		Module module;
		Declaration decl;
		VarList varList;

		// set up variable list
		varList = new VarList();

		// first add all globals to the list
		n = getNumGlobals();
		for (i = 0; i < n; i++) {
			decl = getGlobal(i);
			name = decl.getName();
			// variable is integer
			if (decl.getType() == Expression.INT) {
				low = decl.getLow().evaluateInt(constantValues, null);
				high = decl.getHigh().evaluateInt(constantValues, null);
				start = decl.getStart(null).evaluateInt(constantValues, null);
			}
			// variable is boolean
			else {
				low = 0;
				high = 1;
				start = (decl.getStart(null).evaluateBoolean(constantValues, null)) ? 1 : 0;
			}
			// check range is valid
			if (high - low <= 0) {
				String s = "Invalid range (" + low + "-" + high + ") for variable \"" + name + "\"";
				throw new PrismLangException(s, decl);
			}
			if ((long)high - (long)low >= Integer.MAX_VALUE) {
				String s = "Range for variable \"" + name + "\" (" + low + "-" + high + ") is too big";
				throw new PrismLangException(s, decl);
			}
			// check start is valid
			if (start < low || start > high) {
				String s = "Invalid initial value (" + start + ") for variable \"" + name + "\"";
				throw new PrismLangException(s, decl);
			}
			varList.addVar(name, low, high, start, -1, decl.getType());
		}

		// then add all module variables to the list
		n = getNumModules();
		for (i = 0; i < n; i++) {
			module = getModule(i);
			n2 = module.getNumDeclarations();
			for (j = 0; j < n2; j++) {
				decl = module.getDeclaration(j);
				name = decl.getName();
				// variable is integer
				if (decl.getType() == Expression.INT) {
					low = decl.getLow().evaluateInt(constantValues, null);
					high = decl.getHigh().evaluateInt(constantValues, null);
					start = decl.getStart(null).evaluateInt(constantValues, null);
				}
				// variable is boolean
				else {
					low = 0;
					high = 1;
					start = (decl.getStart(null).evaluateBoolean(constantValues, null)) ? 1 : 0;
				}
				// check range is valid
				if (high - low <= 0) {
					String s = "Invalid range (" + low + "-" + high + ") for variable \"" + name + "\"";
					throw new PrismLangException(s, decl);
				}
				if ((long)high - (long)low >= Integer.MAX_VALUE) {
					String s = "Range for variable \"" + name + "\" (" + low + "-" + high + ") is too big";
					throw new PrismLangException(s, decl);
				}
				// check start is valid
				if (start < low || start > high) {
					String s = "Invalid initial value (" + start + ") for variable \"" + name + "\"";
					throw new PrismLangException(s, decl);
				}
				varList.addVar(name, low, high, start, i, decl.getType());
			}
		}

		return varList;
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

		switch (type) {
		case PROBABILISTIC:
			s += "dtmc";
			break;
		case NONDETERMINISTIC:
			s += "mdp";
			break;
		case STOCHASTIC:
			s += "ctmc";
			break;
		}
		s += "\n\n";

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

		n = getNumGlobals();
		for (i = 0; i < n; i++) {
			s += "global " + getGlobal(i) + ";\n";
		}
		if (n > 0) {
			s += "\n";
		}

		for (i = 0; i < modules.size() - 1; i++) {
			s += modules.elementAt(i) + "\n\n";
		}
		s += modules.elementAt(modules.size() - 1) + "\n";

		if (systemDefn != null) {
			s += "\nsystem " + systemDefn + " endsystem\n";
		}

		n = getNumRewardStructs();
		for (i = 0; i < n; i++) {
			s += "\n" +getRewardStruct(i);
		}

		if (initStates != null) {
			s += "\n" + "init " + initStates + " endinit\n";
		}

		return s;
	}

	/**
	 * Perform a deep copy.
	 */
	public ASTElement deepCopy()
	{
		// Deep copy not required for whole model file
		return null;
	}
}

// ------------------------------------------------------------------------------
