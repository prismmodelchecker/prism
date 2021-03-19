//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import param.BigRational;
import parser.IdentUsage;
import parser.State;
import parser.Values;
import parser.VarList;
import parser.type.Type;
import parser.visitor.ASTTraverse;
import parser.visitor.ASTVisitor;
import parser.visitor.ModulesFileSemanticCheck;
import parser.visitor.ModulesFileSemanticCheckAfterConstants;
import prism.ModelInfo;
import prism.ModelType;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismUtils;
import prism.RewardGenerator;

// Class representing parsed model file

public class ModulesFile extends ASTElement implements ModelInfo, RewardGenerator
{
	// Model type, as specified in the model file
	private ModelType modelTypeInFile;
	// Model type (actual, may differ)
	private ModelType modelType;

	// Model components
	private FormulaList formulaList;
	private LabelList labelList;
	private ConstantList constantList;
	private Vector<Declaration> globals; // Global variables
	private Vector<Object> modules; // Modules (includes renamed modules)
	private ArrayList<SystemDefn> systemDefns; // System definitions (system...endsystem constructs)
	private ArrayList<String> systemDefnNames; // System definition names (system...endsystem constructs)
	private ArrayList<RewardStruct> rewardStructs; // Rewards structures
	private List<String> rewardStructNames; // Names of reward structures
	private Expression initStates; // Initial states specification
	private List<ObservableVars> observableVarLists; // Observable variables lists
	private List<Observable> observableDefns; // Standalone observable definitions
	
	// Info about all identifiers used
	private IdentUsage identUsage;
	private IdentUsage quotedIdentUsage;
	// List of all module names
	private String[] moduleNames;
	// List of synchronising actions
	private Vector<String> synchs;
	// Lists of variable info (declaration, name, type, module index)
	private Vector<Declaration> varDecls;
	private Vector<String> varNames;
	private Vector<Type> varTypes;
	private Vector<Integer> varModules;
	// Lists of observable info
	private List<Observable> observables;
	private List<String> observableNames;
	private List<Type> observableTypes;
	private List<String> observableVars;

	// Values set for undefined constants (null if none)
	private Values undefinedConstantValues;
	// Actual values of (some or all) constants
	private Values constantValues;

	// Constructor

	public ModulesFile()
	{
		formulaList = new FormulaList();
		labelList = new LabelList();
		constantList = new ConstantList();
		modelTypeInFile = modelType = null; // Unspecified
		globals = new Vector<Declaration>();
		modules = new Vector<Object>();
		systemDefns = new ArrayList<SystemDefn>();
		systemDefnNames = new ArrayList<String>();
		rewardStructs = new ArrayList<RewardStruct>();
		rewardStructNames = new ArrayList<String>();
		initStates = null;
		observableVarLists = new ArrayList<>();
		observableDefns = new ArrayList<>();
		identUsage = new IdentUsage();
		quotedIdentUsage = new IdentUsage(true);
		varDecls = new Vector<Declaration>();
		varNames = new Vector<String>();
		varTypes = new Vector<Type>();
		varModules = new Vector<Integer>();
		observables = new ArrayList<>();
		observableNames = new ArrayList<>();
		observableTypes = new ArrayList<>();
		observableVars = new ArrayList<>();
		undefinedConstantValues = null;
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

	/**
	 * Set the model type that is specified in the model file.
	 * Can be null, denoting that it is unspecified.
	 */
	public void setModelTypeInFile(ModelType t)
	{
		modelTypeInFile = t;
		// As a default, set the actual type to be the same
		modelType = modelTypeInFile;
	}

	/**
	 * Set the actual model type,
	 * which may differ from the type specified in the model file.
	 * Note: if {@link #tidyUp()} is called, this may be overwritten.
	 */
	public void setModelType(ModelType t)
	{
		modelType = t;
	}

	public void addGlobal(Declaration d)
	{
		globals.add(d);
	}

	public void setGlobal(int i, Declaration d)
	{
		globals.set(i, d);
	}

	public void addModule(Module m)
	{
		modules.add(m);
		m.setParent(this);
	}

	public void setModule(int i, Module m)
	{
		modules.set(i, m);
		m.setParent(this);
	}

	public void addRenamedModule(RenamedModule m)
	{
		modules.add(m);
	}

	/**
	 * Set a (single, un-named) "system...endsystem" construct for this model.
	 * @param systemDefn SystemDefn object for the "system...endsystem" construct 
	 */
	public void setSystemDefn(SystemDefn systemDefn)
	{
		clearSystemDefns();
		addSystemDefn(systemDefn);
	}

	/**
	 * Remove any "system...endsystem" constructs for this model.
	 */
	public void clearSystemDefns()
	{
		systemDefns.clear();
		systemDefnNames.clear();
	}

	/**
	 * Add an (un-named) "system...endsystem" construct to this model.
	 * @param systemDefn SystemDefn object for the "system...endsystem" construct 
	 */
	public void addSystemDefn(SystemDefn systemDefn)
	{
		addSystemDefn(systemDefn, null);
	}

	/**
	 * Add a "system...endsystem" construct to this model.
	 * @param systemDefn SystemDefn object for the "system...endsystem" construct 
	 * @param name Optional name for the construct (null if un-named) 
	 */
	public void addSystemDefn(SystemDefn systemDefn, String name)
	{
		systemDefns.add(systemDefn);
		systemDefnNames.add(name);
	}

	/**
	 * Set the {@code i}th  "system...endsystem" construct for this model.
	 * @param i Index (starting from 0) 
	 * @param systemDefn SystemDefn object for the "system...endsystem" construct 
	 * @param name Optional name for the construct (null if un-named) 
	 */
	public void setSystemDefn(int i, SystemDefn systemDefn, String name)
	{
		systemDefns.set(i, systemDefn);
		systemDefnNames.set(i, name);
	}

	public void clearRewardStructs()
	{
		rewardStructs.clear();
		rewardStructNames.clear();
	}

	public void addRewardStruct(RewardStruct r)
	{
		rewardStructs.add(r);
		rewardStructNames.add(r.getName());
	}

	public void setRewardStruct(int i, RewardStruct r)
	{
		rewardStructs.set(i, r);
		rewardStructNames.set(i, r.getName());
	}

	// this method is included for backwards compatibility only
	public void setRewardStruct(RewardStruct r)
	{
		clearRewardStructs();
		addRewardStruct(r);
	}

	public void setInitialStates(Expression e)
	{
		initStates = e;
	}

	/**
	 * Add an observable variables list
	 */
	public void addObservableVarList(ObservableVars obsVars)
	{
		observableVarLists.add(obsVars);
	}
	
	/**
	 * Set the ith observable variables list
	 */
	public void setObservableVarList(int i, ObservableVars obsVars)
	{
		observableVarLists.set(i, obsVars);
	}
	
	/**
	 * Add a (standalone) observable definition
	 */
	public void addObservableDefinition(Observable obs)
	{
		observableDefns.add(obs);
	}
	
	/**
	 * Set the ith (standalone) observable definition
	 */
	public void setObservableDefinition(int i, Observable obs)
	{
		observableDefns.set(i, obs);
	}
	
	// Get methods

	public FormulaList getFormulaList()
	{
		return formulaList;
	}

	@Override
	public int getNumLabels()
	{
		return labelList.size();
	}

	@Override
	public List<String> getLabelNames()
	{
		return labelList.getLabelNames();
	}

	@Override
	public String getLabelName(int i) throws PrismException
	{
		return labelList.getLabelName(i);
	}

	@Override
	public int getLabelIndex(String label)
	{
		return labelList.getLabelIndex(label);
	}
	
	public LabelList getLabelList()
	{
		return labelList;
	}

	public ConstantList getConstantList()
	{
		return constantList;
	}

	public ModelType getModelTypeInFile()
	{
		return modelTypeInFile;
	}
	
	@Override
	public ModelType getModelType()
	{
		return modelType;
	}

	public String getTypeString()
	{
		return "" + modelType;
	}

	public String getTypeFullString()
	{
		return modelType.fullName();
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

	/**
	 * Get the default "system...endsystem" construct for this model, if it exists.
	 * If there is an un-named "system...endsystem" (there can be at most one),
	 * this is the default. If not, the first (named) one is taken as the default.
	 * If there are no  "system...endsystem" constructs, this method returns null.
	 */
	public SystemDefn getSystemDefn()
	{
		int n = systemDefns.size();
		if (n == 0)
			return null;
		for (int i = 0; i < n; i++) {
			if (systemDefnNames.get(i) == null)
				return systemDefns.get(i);
		}
		return systemDefns.get(0);
	}
	
	/**
	 * Get the number of "system...endsystem" constructs for this model.
	 */
	public int getNumSystemDefns()
	{
		return systemDefns.size();
	}

	/**
	 * Get the {@code i}th "system...endsystem" construct for this model (0-indexed).
	 */
	public SystemDefn getSystemDefn(int i)
	{
		return systemDefns.get(i);
	}

	/**
	 * Get the name of the {@code i}th "system...endsystem" construct for this model (0-indexed).
	 * Returns null if that construct is un-named.
	 */
	public String getSystemDefnName(int i)
	{
		return systemDefnNames.get(i);
	}

	/**
	 * Get the index of a "system...endsystem" construct by its name (indexed from 0).
	 * Passing null for the name looks up an un-named construct. 
	 * Returns null if the requested construct does not exist.
	 */
	public int getSystemDefnIndex(String name)
	{
		int n = systemDefns.size();
		for (int i = 0; i < n; i++) {
			String s = systemDefnNames.get(i); 
			if ((s == null && name == null) || (s != null && s.equals(name)))
				return i;
		}
		return -1;
	}

	/**
	 * Get a "system...endsystem" construct by its name.
	 * Passing null for the name looks up an un-named construct. 
	 * Returns null if the requested construct does not exist.
	 */
	public SystemDefn getSystemDefnByName(String name)
	{
		int i = getSystemDefnIndex(name);
		return i == -1 ? null : getSystemDefn(i);
	}

	/**
	 * Get the number of reward structures in the model.
	 */
	public int getNumRewardStructs()
	{
		return rewardStructs.size();
	}

	/**
	 * Get a list of the names of the reward structures in the model.
	 */
	public List<String> getRewardStructNames()
	{
		return rewardStructNames;
	}
	
	/**
	 * Get the index of a module by its name
	 * (indexed from 0, not from 1 like at the user (property language) level).
	 * Returns -1 if name does not exist.
	 */
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

	/**
	 * Returns true if the {@code r}th reward structure defines state rewards.
	 * ({@code r} is indexed from 0, not from 1 like at the user (property language) level).
	 */
	public boolean rewardStructHasStateRewards(int r)
	{
		RewardStruct rewStr = getRewardStruct(r);
		return rewStr.getNumStateItems() > 0;
	}

	/**
	 * Returns true if the {@code r}th reward structure defines transition rewards.
	 * ({@code r} is indexed from 0, not from 1 like at the user (property language) level).
	 */
	public boolean rewardStructHasTransitionRewards(int r)
	{
		RewardStruct rewStr = getRewardStruct(r);
		return rewStr.getNumTransItems() > 0;
	}

	/**
	 * Get a reward structure by its index
	 * (indexed from 0, not from 1 like at the user (property language) level).
	 * Returns null if index is out of range.
	 */
	public RewardStruct getRewardStruct(int i)
	{
		return (i < rewardStructs.size()) ? rewardStructs.get(i) : null;
	}

	/**
	 * Get access to the list of reward structures
	 */
	public List<RewardStruct> getRewardStructs()
	{
		return rewardStructs;
	}

	/**
	 * Get a reward structure by its name
	 * Returns null if name does not exist.
	 */
	public RewardStruct getRewardStructByName(String name)
	{
		int i = getRewardStructIndex(name);
		return i == -1 ? null : getRewardStruct(i);
	}

	/**
	 * Get the first reward structure (exists for backwards compatibility only).
	 */
	public RewardStruct getRewardStruct()
	{
		return getRewardStruct(0);
	}

	@Override
	public boolean isRewardLookupSupported(RewardLookup lookup)
	{
		return lookup == RewardLookup.BY_REWARD_STRUCT;
	}
	
	/**
	 * Get the expression used in init...endinit to define the initial states of the model.
	 * This is null if absent, i.e. the model has a single initial state defined by the
	 * initial values for each individual variable.  
	 * If non-null, we have to assume that there may be multiple initial states. 
	 */
	public Expression getInitialStates()
	{
		return initStates;
	}

	/**
	 * Does this model define observables?
	 * (i.e., is it a partially observable model?)
	 */
	public boolean hasObservables()
	{
		return observableVarLists.size() > 0 || observableDefns.size() > 0;
	}
	
	/**
	 * Get the number of lists of observable variables
	 */
	public int getNumObservableVarLists()
	{
		return observableVarLists.size();
	}
	
	/**
	 * Get the ith list of observable variables
	 */
	public ObservableVars getObservableVarList(int i)
	{
		return observableVarLists.get(i);
	}
	
	/**
	 * Get the number of (standalone) observable definitions
	 */
	public int getNumObservableDefinitions()
	{
		return observableDefns.size();
	}
	
	/**
	 * Get the ith (standalone) observable definition
	 */
	public Observable getObservableDefinition(int i)
	{
	 return observableDefns.get(i);
	}

	/**
	 * Look up a property by name.
	 * Returns null if not found.
	 * Currently only exists for forwards compatibility.
	 */
	public Property getPropertyByName(String name)
	{
		return null;
	}
	
	/**
	 * Check if an identifier is already used somewhere in the model
	 * (as a formula, constant or variable)
	 * and throw an exception if it is. Otherwise, add it to the list.
	 * @param ident The name of the (new) identifier
	 * @param decl Where the identifier is declared in the model
	 * @param use Optionally, the identifier's usage (e.g. "constant")
	 */
	private void checkAndAddIdentifier(String ident, ASTElement decl, String use) throws PrismLangException
	{
		identUsage.checkAndAddIdentifier(ident, decl, use, "the model");
	}
	
	@Override
	public boolean isIdentUsed(String ident)
	{
		// Goes beyond default implementation in ModelInfo:
		// also looks at formulas
		return identUsage.isIdentUsed(ident);
	}

	@Override
	public void checkIdent(String ident, ASTElement decl, String use) throws PrismLangException
	{
		// Goes beyond default implementation in ModelInfo:
		// also looks at formulas, and produces better error messages
		identUsage.checkIdent(ident, decl, use);
	}

	/**
	 * Check if a quoted identifier is already used somewhere in the model
	 * (as a label)
	 * and throw an exception if it is. Otherwise, add it to the list.
	 * @param ident The name of the (new) identifier, without quotes
	 * @param decl Where the identifier is declared in the model
	 * @param use Optionally, the identifier's usage (e.g. "label")
	 */
	private void checkAndAddQuotedIdentifier(String ident, ASTElement decl, String use) throws PrismLangException
	{
		quotedIdentUsage.checkAndAddIdentifier(ident, decl, use, "the model");
	}
	
	@Override
	public boolean isQuotedIdentUsed(String ident)
	{
		return quotedIdentUsage.isIdentUsed(ident);
	}

	@Override
	public void checkQuotedIdent(String ident, ASTElement decl, String use) throws PrismLangException
	{
		// Goes beyond default implementation in ModelInfo:
		// produces better error messages
		quotedIdentUsage.checkIdent(ident, decl, use);
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

	/**
	 * Get the list of action names.
	 */
	public Vector<String> getSynchs()
	{
		return synchs;
	}

	/**
	 * Get the {@code i}th action name (0-indexed).
	 */
	public String getSynch(int i)
	{
		return synchs.get(i);
	}

	public boolean isSynch(String s)
	{
		if (synchs == null)
			return false;
		else
			return synchs.contains(s);
	}

	// Variable query methods

	/**
	 * Get the total number of variables (global and local).
	 */
	public int getNumVars()
	{
		return varNames.size();
	}

	/**
	 * Look up the index of a variable in the model by name.
	 * Returns -1 if there is no such variable. 
	 */
	public int getVarIndex(String name)
	{
		return varNames.indexOf(name);
	}

	/**
	 * Get the declaration of the ith variable.
	 */
	public Declaration getVarDeclaration(int i)
	{
		return varDecls.get(i);
	}

	/**
	 * Get the name of the ith variable.
	 */
	public String getVarName(int i)
	{
		return varNames.get(i);
	}

	/**
	 * Get the type of the ith variable.
	 */
	public Type getVarType(int i)
	{
		return varTypes.get(i);
	}

	public Vector<String> getVarNames()
	{
		return varNames;
	}

	public Vector<Type> getVarTypes()
	{
		return varTypes;
	}

	@Override
	public DeclarationType getVarDeclarationType(int i)
	{
		return varDecls.get(i).getDeclType();
	}

	@Override
	public int getVarModuleIndex(int i)
	{
		return varModules.get(i);
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

	@Override
	public boolean containsUnboundedVariables()
	{
		int n = getNumVars();
		for (int i = 0; i < n; i++) {
			DeclarationType declType = getVarDeclaration(i).getDeclType();
			if (declType instanceof DeclarationClock || declType instanceof DeclarationIntUnbounded) {
				return true;
			}
		}
		return false;
	}
	
	public boolean containsClockVariables()
	{
		int n = getNumVars();
		for (int i = 0; i < n; i++) {
			if (getVarDeclaration(i).getDeclType() instanceof DeclarationClock) {
				return true;
			}
		}
		return false;
	}
	
	public Observable getObservable(int i)
	{
		return observables.get(i);
	}
	
	@Override
	public boolean isVarObservable(int i)
	{
		return observableVars.contains(getVarName(i));
	}
	
	public boolean isVarObservable(String varName)
	{
		return observableVars.contains(varName);
	}
	
	@Override
	public List<String> getObservableNames()
	{
		return observableNames;
	}
	
	@Override
	public List<Type> getObservableTypes()
	{
		return observableTypes;
	}
	
	/**
	 * Method to "tidy up" after parsing (must be called)
	 * (do some checks and extract some information)
	 */
	public void tidyUp() throws PrismLangException
	{
		// Clear data that will be generated by this method 
		// (in case it has already been called previously).
		identUsage.clear();
		quotedIdentUsage.clear();
		varDecls.clear();
		varNames.clear();
		varTypes.clear();
		varModules.clear();
		
		// Expansion of formulas and renaming

		// Check formula identifiers
		checkFormulaIdents();
		// Find all formulas (i.e. locate identifiers which are formulas).
		// Note: This should all be done before replacing any other identifiers
		// (e.g. with variables) because this relies on module renaming which in turn
		// must be done after formula expansion. Then, check for any cyclic
		// dependencies in the formula list and then expand all formulas.
		findAllFormulas(formulaList);
		formulaList.findCycles();
		expandFormulas(formulaList);
		// Perform module renaming
		sortRenamings();

		// Check label identifiers
		checkLabelIdents();

		// Check module names
		checkModuleNames();

		// Check constant identifiers
		checkConstantIdents();
		// Find all instances of constants
		// (i.e. locate identifiers which are constants)
		findAllConstants(constantList);
		// Check constants for cyclic dependencies
		constantList.findCycles();

		// Check variable names, etc.
		checkVarNames();
		// Find all instances of variables, replace identifiers with variables.
		// Also check variables valid, store indices, etc.
		findAllVars(varNames, varTypes);

		// Find all instances of property refs
		findAllPropRefs(this, null);
		
		// Check reward structure names
		checkRewardStructNames();

		// Check "system...endsystem" constructs
		checkSystemDefns();
		
		// Get synchronising action names
		// (NB: Do this *after* checking for cycles in system defns above)
		getSynchNames();
		// Then identify/check any references to action names
		findAllActions(synchs);

		// Determine actual model type
		// (checks/processing below this point can assume that modelType
		//  is non-null; methods before this point cannot)
		finaliseModelType();
		
		// Various semantic checks 
		doSemanticChecks();
		// Type checking
		typeCheck();
		// Check observables
		checkObservables();
		
		// If there are no undefined constants, set up values for constants
		// (to avoid need for a later call to setUndefinedConstants).
		// NB: Can't call setUndefinedConstants if there are undefined constants
		// because semanticCheckAfterConstants may fail. 
		if (getUndefinedConstants().isEmpty()) {
			// we use non-exact constant evaluation by default,
			// for exact mode constants will be reevaluated later on
			setUndefinedConstants(null, false);
		}
	}

	// Check formula identifiers

	private void checkFormulaIdents() throws PrismLangException
	{
		int n = formulaList.size();
		for (int i = 0; i < n; i++) {
			String s = formulaList.getFormulaName(i);
			checkAndAddIdentifier(s, formulaList.getFormulaNameIdent(i), "formula");
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
					throw new PrismLangException("Identifier \"" + s + "\" is renamed more than once in module \""
							+ module.getName() + "\"", module.getOldNameASTElement(i2));
				}
				if (formulaList.getFormulaIndex(s) != -1) {
					throw new PrismLangException("Formula \"" + s
							+ "\" cannot be renamed since formulas are expanded before module renaming", module
							.getOldNameASTElement(i2));
				}
			}
			// Then rename (a copy of) base module and replace
			// (note: also store name of base module for later reference)
			newModule = (Module) getModule(j).deepCopy().rename(module);
			newModule.setNameASTElement(module.getNameASTElement());
			newModule.setBaseModule(module.getBaseModule());
			setModule(i, newModule);
		}
	}

	// check label identifiers

	private void checkLabelIdents() throws PrismLangException
	{
		int n = labelList.size();
		for (int i = 0; i < n; i++) {
			String s = labelList.getLabelName(i);
			checkAndAddQuotedIdentifier(s, labelList.getLabelNameIdent(i), "label");
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
					throw new PrismLangException("Duplicated module name \"" + s + "\"", getModule(i)
							.getNameASTElement());
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
					synchs.add(s);
				}
			}
		}

		// then extract any which are introduced in the (default) system construct (by renaming)
		SystemDefn defaultSystemDefn = getSystemDefn();
		if (defaultSystemDefn != null) {
			defaultSystemDefn.getSynchs(synchs, this);
		}
	}

	@Override
	public String getActionStringDescription()
	{
		return "Module/[action]";
	}
	
	// check constant identifiers

	private void checkConstantIdents() throws PrismLangException
	{
		int n = constantList.size();
		for (int i = 0; i < n; i++) {
			String s = constantList.getConstantName(i);
			checkAndAddIdentifier(s, constantList.getConstantNameIdent(i), "constant");
		}
	}

	// check variable names

	private void checkVarNames() throws PrismLangException
	{
		// compile list of all var names
		// and check as we go through

		// globals
		int n = getNumGlobals();
		for (int i = 0; i < n; i++) {
			String s = getGlobal(i).getName();
			checkAndAddIdentifier(s, getGlobal(i), "variable");
			varDecls.add(getGlobal(i));
			varNames.add(s);
			varTypes.add(getGlobal(i).getType());
			varModules.add(-1);
		}

		// locals
		int numModules = modules.size();
		for (int i = 0; i < numModules; i++) {
			Module module = getModule(i);
			int numLocals = module.getNumDeclarations();
			for (int j = 0; j < numLocals; j++) {
				String s = module.getDeclaration(j).getName();
				checkAndAddIdentifier(s, module.getDeclaration(j), "variable");
				varDecls.add(module.getDeclaration(j));
				varNames.add(s);
				varTypes.add(module.getDeclaration(j).getType());
				varModules.add(i);
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

	/**
	 * Check "system...endsystem" constructs, if present.
	 */
	private void checkSystemDefns() throws PrismLangException
	{
		int n = systemDefns.size();
		
		// Check there is a most one un-named system...endsystem...
		
		// None is ok
		if (n == 0)
			return;
		// If there are any, at most one should be un-named
		// and names should be unique 
		int numUnnamed = 0;
		HashSet<String> names = new HashSet<String>();
		for (int i = 0; i < n; i++) {
			String s = systemDefnNames.get(i); 
			if (s == null) {
				numUnnamed++;
			} else {
				if (!names.add(s))
					throw new PrismLangException("Duplicated system...endystem name \"" + s + "\"", getSystemDefn(i));
			}
			if (numUnnamed > 1)
				throw new PrismLangException("There can be at most one un-named system...endsystem construct", getSystemDefn(i));
		}

		// Check for cyclic dependencies...
		
		// Create boolean matrix of dependencies
		// (matrix[i][j] is true if prop i contains a ref to prop j)
		boolean matrix[][] = new boolean[n][n];
		for (int i = 0; i < n; i++) {
			SystemDefn sys = systemDefns.get(i);
			Vector<String> v = new Vector<String>();
			sys.getReferences(v);
			for (int j = 0; j < v.size(); j++) {
				int k = getSystemDefnIndex(v.elementAt(j));
				if (k != -1) {
					matrix[i][k] = true;
				}
			}
		}
		// Check for and report dependencies
		int firstCycle = PrismUtils.findCycle(matrix);
		if (firstCycle != -1) {
			String s = "Cyclic dependency from references in system...endsystem definition \"" + getSystemDefnName(firstCycle) + "\"";
			throw new PrismLangException(s, getSystemDefn(firstCycle));
		}
	}
	
	/**
	 * Check definitions of observables,
	 * and extract/store information about them.
	 */
	private void checkObservables() throws PrismLangException
	{
		// Check observable definitions are present/absent, as required
		if (getModelType().partiallyObservable() && !hasObservables()) {
			throw new PrismLangException(getModelType() + "s must specify observables");
		}
		if (hasObservables() && !getModelType().partiallyObservable()) {
			throw new PrismLangException(getModelType() + "s cannot specify observables");
		}
		// Extract info about observables from
		// observable variable lists and/or observable definitions
		for (ObservableVars obsVars : observableVarLists) {
			int n = obsVars.getNumVars();
			for (int i = 0; i < n; i++) {
				if (!(obsVars.getVar(i) instanceof ExpressionVar)) {
					throw new PrismLangException("Observable variables list can only contain variables", obsVars.getVar(i));
				}
				ExpressionVar exprVar = (ExpressionVar) obsVars.getVar(i);
				String name = exprVar.getName();
				addObservable(name, obsVars.getVar(i), exprVar, exprVar);
			}
		}
		for (Observable obs : observableDefns) {
			String name = obs.getName();
			ExpressionVar exprVar = null;
			if (obs.getDefinition() instanceof ExpressionVar) {
				exprVar = (ExpressionVar) obs.getDefinition();
			}
			addObservable(name, obs, obs.getDefinition(), exprVar);
		}
		// For real-time models with partial observability (i.e. POPTAs), check that all clocks are observable
		if (getModelType().partiallyObservable() && containsClockVariables()) {
			int n = getNumVars();
			for (int i = 0; i < n; i++) {
				if (getVarDeclaration(i).getDeclType() instanceof DeclarationClock) {
					if (!observableVars.contains(getVarName(i))) {
						throw new PrismLangException("All clocks in " + modelType + "s must be observable" , getVarDeclaration(i));
					}
				}
			}
		}
	}
	
	/**
	 * Add info about an observable, from an
	 * observable variable list or observable definition
	 * @param name Observable name
	 * @param decl Where the observable is declared
	 * @param defn Observable definition expression
	 * @param exprVar If observable is a variable, the variable reference
	 */
	private void addObservable(String name, ASTElement decl, Expression defn, ExpressionVar exprVar) throws PrismLangException
	{
		checkAndAddQuotedIdentifier(name, decl, "observable");
		observables.add(new Observable(name, defn));
		observableNames.add(name);
		observableTypes.add(defn.getType());
		if (exprVar != null) {
			observableVars.add(exprVar.getName());
		}
	}
	
	/**
	  * Perform any required semantic checks.
	  * These checks are done *before* any undefined constants have been defined.
	 */
	private void doSemanticChecks() throws PrismLangException
	{
		ModulesFileSemanticCheck visitor = new ModulesFileSemanticCheck(this);
		accept(visitor);
		
	}
	
	/**
	 * Perform further semantic checks that can only be done once values
	 * for any undefined constants have been defined.
	 */
	public void doSemanticChecksAfterConstants() throws PrismLangException
	{
		ModulesFileSemanticCheckAfterConstants visitor = new ModulesFileSemanticCheckAfterConstants(this);
		accept(visitor);
	}

	/**
	 * Get  a list of constants in the model that are undefined
	 * ("const int x;" rather than "const int x = 1;") 
	 */
	public Vector<String> getUndefinedConstants()
	{
		return constantList.getUndefinedConstants();
	}

	/**
	 * Set values for *all* undefined constants and then evaluate all constants.
	 * If there are no undefined constants, {@code someValues} can be null.
	 * Undefined constants can be subsequently redefined to different values with the same method.
	 * The current constant values (if set) are available via {@link #getConstantValues()}. 
	 * Calling this method also triggers some additional semantic checks
	 * that can only be done once constant values have been specified.
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
	 * Calling this method also triggers some additional semantic checks
	 * that can only be done once constant values have been specified.
	 * <br>
	 * Constant values are evaluated using either standard (integer, floating-point) arithmetic
	 * or exact arithmetic, depending on the value of the {@code exact} flag.
	 */
	public void setUndefinedConstants(Values someValues, boolean exact) throws PrismLangException
	{
		undefinedConstantValues = someValues == null ? null : new Values(someValues);
		constantValues = constantList.evaluateConstants(someValues, null, exact);
		doSemanticChecksAfterConstants();
	}

	@Override
	public void setSomeUndefinedConstants(Values someValues) throws PrismLangException
	{
		setSomeUndefinedConstants(someValues, false);
	}

	@Override
	public void setSomeUndefinedConstants(Values someValues, boolean exact) throws PrismLangException
	{
		undefinedConstantValues = someValues == null ? null : new Values(someValues);
		constantValues = constantList.evaluateSomeConstants(someValues, null, exact);
		doSemanticChecksAfterConstants();
	}

	/**
	 * Check if {@code name} is a *defined* constant in the model,
	 * i.e. a constant whose value was *not* left unspecified.
	 */
	public boolean isDefinedConstant(String name)
	{
		return constantList.isDefinedConstant(name);
	}
	
	/**
	 * Get access to the values that have been provided for undefined constants in the model 
	 * (e.g. via the method {@link #setUndefinedConstants(Values)}).
	 */
	public Values getUndefinedConstantValues()
	{
		return undefinedConstantValues;
	}

	/**
	 * Get access to the values for all constants in the model, including the 
	 * undefined constants set previously via the method {@link #setUndefinedConstants(Values)}.
	 * Until they are set for the first time, this method returns null.  
	 */
	public Values getConstantValues()
	{
		return constantValues;
	}

	/**
	 * Create a State object representing the default initial state of this model.
	 * If there are potentially multiple initial states (because the model has an 
	 * init...endinit specification), this method returns null;
	 * Assumes that values for constants have been provided for the model.
	 * Note: This method replaces the old getInitialValues() method,
	 * since State objects are now preferred to Values objects for efficiency.
	 * <br>
	 * The init expression is evaluated using the default evaluate, i.e.,
	 * not using exact arithmetic.
	 */
	public State getDefaultInitialState() throws PrismLangException
	{
		return getDefaultInitialState(false);
	}

	/**
	 * Create a State object representing the default initial state of this model.
	 * If there are potentially multiple initial states (because the model has an
	 * init...endinit specification), this method returns null;
	 * Assumes that values for constants have been provided for the model.
	 * Note: This method replaces the old getInitialValues() method,
	 * since State objects are now preferred to Values objects for efficiency.
	 * @param exact use exact arithmetic in evaluation of init expression?
	 */
	public State getDefaultInitialState(boolean exact) throws PrismLangException
	{
		int i, j, count, n, n2;
		Module module;
		Declaration decl;
		State initialState;
		Object initialValue;

		if (initStates != null) {
			return null;
		}

		// Create State object
		initialState = new State(getNumVars());
		// Then add values for all globals and all locals, in that order
		count = 0;
		n = getNumGlobals();
		for (i = 0; i < n; i++) {
			decl = getGlobal(i);
			if (exact) {
				BigRational r = decl.getStartOrDefault().evaluateExact(constantValues);
				initialValue = getGlobal(i).getType().castFromBigRational(r);
			} else {
				initialValue = decl.getStartOrDefault().evaluate(constantValues);
				initialValue = getGlobal(i).getType().castValueTo(initialValue);
			}
			initialState.setValue(count++, initialValue);
		}
		n = getNumModules();
		for (i = 0; i < n; i++) {
			module = getModule(i);
			n2 = module.getNumDeclarations();
			for (j = 0; j < n2; j++) {
				decl = module.getDeclaration(j);
				if (exact) {
					BigRational r = decl.getStartOrDefault().evaluateExact(constantValues);
					initialValue = module.getDeclaration(j).getType().castFromBigRational(r);
				} else {
					initialValue = decl.getStartOrDefault().evaluate(constantValues);
					initialValue = module.getDeclaration(j).getType().castValueTo(initialValue);
				}
				initialState.setValue(count++, initialValue);
			}
		}

		return initialState;
	}

	/**
	 * Create a Values object representing the default initial state of this model.
	 * Deprecated: Use getDefaultInitialState() instead
	 * (or new Values(getDefaultInitialState(), modulesFile)).
	 */
	@Deprecated
	public Values getInitialValues() throws PrismLangException
	{
		State stateInit = getDefaultInitialState();
		return (stateInit == null) ? null : new Values(stateInit, this);
	}

	/**
	 * Recompute all information about variables.
	 * More precisely... TODO
	 * Note: This does not re-compute the list of all identifiers used. 
	 */
	public void recomputeVariableinformation() throws PrismLangException
	{
		int i, n;

		// Recompute lists of all variables and types
		varDecls = new Vector<Declaration>();
		varNames = new Vector<String>();
		varTypes = new Vector<Type>();
		varModules = new Vector<Integer>();
		// Globals
		for (Declaration decl : globals) {
			varDecls.add(decl);
			varNames.add(decl.getName());
			varTypes.add(decl.getType());
			varModules.add(-1);
		}
		// Locals
		n = modules.size();
		for (i = 0; i < n; i++) {
			for (Declaration decl : getModule(i).getDeclarations()) {
				varDecls.add(decl);
				varNames.add(decl.getName());
				varTypes.add(decl.getType());
				varModules.add(i);
			}
		}
		// Find all instances of variables, replace identifiers with variables.
		// Also check variables valid, store indices, etc.
		findAllVars(varNames, varTypes);
	}

	/**
	 * Create a VarList object storing information about all variables in this model.
	 * Assumes that values for constants have been provided for the model.
	 * Also performs various syntactic checks on the variables.   
	 */
	public VarList createVarList() throws PrismException
	{
		return new VarList(this);
	}

	/**
	 * Determine the actual model type
	 */
	private void finaliseModelType()
	{
		// First, fix a "base" model type
		// If unspecified, auto-detect
		if (modelTypeInFile == null) {
			boolean isNonProb = isNonProbabilistic();
			modelType = isNonProb ? ModelType.LTS : ModelType.MDP;
		}
		// Otherwise, it's just whatever was specified
		else {
			modelType = modelTypeInFile;
		}
		// Then, even if already specified, update the model type
		// based on the existence of certain features
		boolean isRealTime = containsClockVariables();
		boolean isPartObs = hasObservables();
		if (isRealTime) {
			if (modelType == ModelType.MDP || modelType == ModelType.LTS) {
				modelType = ModelType.PTA;
			}
		}
		if (isPartObs) {
			if (modelType == ModelType.MDP || modelType == ModelType.LTS) {
				modelType = ModelType.POMDP;
			} else if (modelType == ModelType.PTA) {
				modelType = ModelType.POPTA;
			}
		}
	}
	
	/**
	 * Check whether this model is non-probabilistic,
	 * i.e., whether none of the commands are probabilistic. 
	 */
	private boolean isNonProbabilistic()
	{
		try {
			// Search through commands, checking for probabilities
			accept(new ASTTraverse()
			{
				public Object visit(Updates e) throws PrismLangException
				{
					int n = e.getNumUpdates();
					for (int i = 0; i < n; i++) {
						if (e.getProbability(i) != null) {
							throw new PrismLangException("Found one");
						}
					}
					visitPost(e);
					return null;
				}
			});
		} catch (PrismLangException e) {
			return false;
		}
		return true;
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

		if (modelTypeInFile != null) {
			s += modelTypeInFile.toString().toLowerCase() + "\n\n";
		}

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

		for (ObservableVars obsVars : observableVarLists) {
			s += obsVars + "\n\n";
		}
		for (Observable obs : observableDefns) {
			s += obs + "\n";
		}
		if (!observableDefns.isEmpty()) {
			s += "\n";
		}
		
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

		for (i = 0; i < systemDefns.size(); i++) {
			s += "\nsystem ";
			if (systemDefnNames.get(i) != null)
				s += "\"" + systemDefnNames.get(i) + "\" ";
			s += systemDefns.get(i) + " endsystem\n";
		}

		n = getNumRewardStructs();
		for (i = 0; i < n; i++) {
			s += "\n" + getRewardStruct(i);
		}

		if (initStates != null) {
			s += "\n" + "init " + initStates + " endinit\n";
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
		ModulesFile ret = new ModulesFile();
		
		// Copy ASTElement stuff
		ret.setPosition(this);
		// Copy type
		ret.setModelTypeInFile(modelTypeInFile);
		ret.setModelType(modelType);
		// Deep copy main components
		ret.setFormulaList((FormulaList) formulaList.deepCopy());
		ret.setLabelList((LabelList) labelList.deepCopy());
		ret.setConstantList((ConstantList) constantList.deepCopy());
		n = getNumGlobals();
		for (i = 0; i < n; i++) {
			ret.addGlobal((Declaration) getGlobal(i).deepCopy());
		}
		n = getNumModules();
		for (i = 0; i < n; i++) {
			ret.addModule((Module) getModule(i).deepCopy());
		}
		n = getNumSystemDefns();
		for (i = 0; i < n; i++) {
			ret.addSystemDefn(getSystemDefn(i).deepCopy(), getSystemDefnName(i));
		}
		n = getNumRewardStructs();
		for (i = 0; i < n; i++) {
			ret.addRewardStruct((RewardStruct) getRewardStruct(i).deepCopy());
		}
		if (initStates != null)
			ret.setInitialStates(initStates.deepCopy());
		for (ObservableVars obsVars : observableVarLists)
			ret.observableVarLists.add(obsVars.deepCopy());
		for (Observable obs : observableDefns)
			ret.observableDefns.add(obs.deepCopy());
		// Copy other (generated) info
		ret.identUsage = (identUsage == null) ? null : identUsage.deepCopy();
		ret.quotedIdentUsage = (quotedIdentUsage == null) ? null : quotedIdentUsage.deepCopy();
		ret.moduleNames = (moduleNames == null) ? null : moduleNames.clone();
		ret.synchs = (synchs == null) ? null : (Vector<String>)synchs.clone();
		if (varDecls != null) {
			ret.varDecls = new Vector<Declaration>();
			for (Declaration d : varDecls)
				ret.varDecls.add((Declaration) d.deepCopy());
		}
		ret.varNames = (varNames == null) ? null : (Vector<String>)varNames.clone();
		ret.varTypes = (varTypes == null) ? null : (Vector<Type>)varTypes.clone();
		ret.varModules = (varModules == null) ? null : (Vector<Integer>)varModules.clone();
		for (Observable obs : observables)
			ret.observables.add(obs.deepCopy());
		ret.observableNames = (observableNames == null) ? null : new ArrayList<>(observableNames);
		ret.observableTypes = (observableTypes == null) ? null : new ArrayList<>(observableTypes);
		ret.observableVars = (observableVars == null) ? null : new ArrayList<>(observableVars);
		ret.constantValues = (constantValues == null) ? null : new Values(constantValues);
		
		return ret;
	}
}

// ------------------------------------------------------------------------------
