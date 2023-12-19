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
import java.util.Objects;

import parser.EvaluateContext;
import parser.IdentUsage;
import parser.State;
import parser.Values;
import parser.VarList;
import parser.type.Type;
import parser.type.TypeInterval;
import parser.visitor.ASTTraverse;
import parser.visitor.ASTVisitor;
import parser.visitor.DeepCopy;
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
	private ArrayList<Declaration> globals; // Global variables
	private ArrayList<Object> modules; // Modules (includes renamed modules)
	private ArrayList<SystemDefn> systemDefns; // System definitions (system...endsystem constructs)
	private ArrayList<String> systemDefnNames; // System definition names (system...endsystem constructs)
	private ArrayList<RewardStruct> rewardStructs; // Rewards structures
	private ArrayList<String> rewardStructNames; // Names of reward structures
	private Expression initStates; // Initial states specification
	private ArrayList<ObservableVars> observableVarLists; // Observable variables lists
	private ArrayList<Observable> observableDefns; // Standalone observable definitions
	
	// Info about all identifiers used
	private IdentUsage identUsage;
	private IdentUsage quotedIdentUsage;
	// List of all module names
	private String[] moduleNames;
	// List of synchronising actions
	private ArrayList<String> synchs;
	// Lists of variable info (declaration, name, type, module index)
	private ArrayList<Declaration> varDecls;
	private ArrayList<String> varNames;
	private ArrayList<Type> varTypes;
	private ArrayList<Integer> varModules;
	// Lists of observable info
	private ArrayList<Observable> observables;
	private ArrayList<String> observableNames;
	private ArrayList<Type> observableTypes;
	private ArrayList<String> observableVars;

	// Copy of the evaluation context used to defined undefined constants (null if none)
	private EvaluateContext ecUndefined;
	
	// Actual values of (some or all) constants
	private Values constantValues;
	// Evaluation context (all constant values + evaluation mode)
	private EvaluateContext ec;

	// Constructor

	public ModulesFile()
	{
		formulaList = new FormulaList();
		labelList = new LabelList();
		constantList = new ConstantList();
		modelTypeInFile = modelType = null; // Unspecified
		globals = new ArrayList<>();
		modules = new ArrayList<>();
		systemDefns = new ArrayList<SystemDefn>();
		systemDefnNames = new ArrayList<String>();
		rewardStructs = new ArrayList<RewardStruct>();
		rewardStructNames = new ArrayList<String>();
		initStates = null;
		observableVarLists = new ArrayList<>();
		observableDefns = new ArrayList<>();
		identUsage = new IdentUsage();
		quotedIdentUsage = new IdentUsage(true);
		varDecls = new ArrayList<>();
		varNames = new ArrayList<>();
		varTypes = new ArrayList<>();
		varModules = new ArrayList<>();
		observables = new ArrayList<>();
		observableNames = new ArrayList<>();
		observableTypes = new ArrayList<>();
		observableVars = new ArrayList<>();
		ecUndefined = null;
		constantValues = null;
		ec = EvaluateContext.create();
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
		return globals.get(i);
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
		Object o = modules.get(i);
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
	public List<String> getSynchs()
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

	public List<String> getVarNames()
	{
		return varNames;
	}

	public List<Type> getVarTypes()
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
		// (to avoid need for a later call to setSomeUndefinedConstants).
		// NB: Can't call setSomeUndefinedConstants now if there are undefined constants
		// because semanticCheckAfterConstants may fail. 
		if (getUndefinedConstants().isEmpty()) {
			// NB: we use non-exact constant evaluation by default,
			// for exact mode constants will be reevaluated later on
			setSomeUndefinedConstants(EvaluateContext.create());
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
			o = modules.get(i);
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
		List<String> v;
		String s;
		int i, j, n, m;

		// create list to store names
		synchs = new ArrayList<>();

		// go thru modules and extract names which appear in their commands
		n = modules.size();
		for (i = 0; i < n; i++) {
			v = getModule(i).getAllSynchs();
			m = v.size();
			for (j = 0; j < m; j++) {
				s = v.get(j);
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
			List<String> v = new ArrayList<>();
			sys.getReferences(v);
			for (int j = 0; j < v.size(); j++) {
				int k = getSystemDefnIndex(v.get(j));
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
		if (getModelType().partiallyObservable() && getModelType().realTime()) {
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
	public List<String> getUndefinedConstants()
	{
		return constantList.getUndefinedConstants();
	}

	@Override
	public void setSomeUndefinedConstants(EvaluateContext ecUndefined) throws PrismLangException
	{
		this.ecUndefined = ecUndefined == null ? EvaluateContext.create() : EvaluateContext.create(ecUndefined);
		constantValues = constantList.evaluateSomeConstants(ecUndefined);
		ec = EvaluateContext.create(constantValues, ecUndefined.getEvaluationMode());
		doSemanticChecksAfterConstants();
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
	 * Check if {@code name} is a *defined* constant in the model,
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

	@Override
	public Values getConstantValues()
	{
		return constantValues;
	}

	@Override
	public EvaluateContext getEvaluateContext()
	{
		return ec;
	}

	/**
	 * Create a State object representing the default initial state of this model.
	 * If there are potentially multiple initial states (because the model has an
	 * init...endinit specification), this method returns null.
	 * Assumes that values for constants (and evaluation mode) have been set for the model.
	 */
	public State getDefaultInitialState() throws PrismLangException
	{
		if (initStates != null) {
			return null;
		}

		// Create State object
		State initialState = new State(getNumVars());
		// Then add values for all globals and all locals, in that order
		int count = 0;
		int n = getNumGlobals();
		for (int i = 0; i < n; i++) {
			Declaration decl = getGlobal(i);
			Object initialValue = decl.getType().castValueTo(decl.getStartOrDefault().evaluate(ec));
			initialState.setValue(count++, initialValue);
		}
		n = getNumModules();
		for (int i = 0; i < n; i++) {
			Module module = getModule(i);
			int n2 = module.getNumDeclarations();
			for (int j = 0; j < n2; j++) {
				Declaration decl = module.getDeclaration(j);
				Object initialValue = decl.getType().castValueTo(decl.getStartOrDefault().evaluate(ec));
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
		varDecls = new ArrayList<>();
		varNames = new ArrayList<>();
		varTypes = new ArrayList<>();
		varModules = new ArrayList<>();
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
	private void finaliseModelType() throws PrismLangException
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
		boolean isInterval = probabilitiesContainIntervals();
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
		if (isInterval) {
			if (modelType == ModelType.DTMC) {
				modelType = ModelType.IDTMC;
			} else if (modelType == ModelType.MDP) {
				modelType = ModelType.IMDP;
			} else {
				throw new PrismLangException("Intervals only allowed in DTMCs and MDPs currently");
			}
		}
	}
	
	/**
	 * Returns true if one or more of the probabilities in a guarded command contains an interval.
	 */
	public boolean probabilitiesContainIntervals()
	{
		return findIntervalInProbabilities() != null;
	}
	
	/**
	 * If one or more of the probabilities in a guarded command contains an interval,
	 * return it; otherwise return null.
	 */
	public ASTElement findIntervalInProbabilities()
	{
		try {
			ASTTraverse astt = new ASTTraverse()
			{
				public void visitPost(Updates e) throws PrismLangException
				{
					int n = e.getNumUpdates();
					for (int i = 0; i < n; i++) {
						if (e.getProbability(i) != null && e.getProbability(i).getType() instanceof TypeInterval) {
							throw new PrismLangException("Found one", e);
						}
					}
				}
			};
			accept(astt);
		} catch (PrismLangException e) {
			return e.getASTElement();
		}
		return null;
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
			s += modules.get(i) + "\n\n";
		}
		s += modules.get(modules.size() - 1) + "\n";

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

	@Override
	public ModulesFile deepCopy(DeepCopy copier) throws PrismLangException
	{
		// Deep copy main components
		labelList = copier.copy(labelList);
		formulaList = copier.copy(formulaList);
		constantList = copier.copy(constantList);
		initStates = copier.copy(initStates);
		identUsage = identUsage.deepCopy();
		quotedIdentUsage = quotedIdentUsage.deepCopy();

		copier.copyAll(globals);
		copier.copyAll(varDecls);
		copier.copyAll(systemDefns);
		copier.copyAll(observables);
		copier.copyAll(rewardStructs);
		copier.copyAll(observableDefns);
		copier.copyAll(observableVarLists);

		for (int i = 0, n = getNumModules(); i < n; i++) {
			Module mod = Objects.requireNonNull(getModule(i));
			setModule(i, copier.copy(mod));
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public ModulesFile clone()
	{
		ModulesFile clone = (ModulesFile) super.clone();

		// clone main components
		clone.globals            = (ArrayList<Declaration>) globals.clone();
		clone.modules            = (ArrayList<Object>) modules.clone();
		clone.systemDefns        = (ArrayList<SystemDefn>) systemDefns.clone();
		clone.systemDefnNames    = (ArrayList<String>) systemDefnNames.clone();
		clone.rewardStructs      = (ArrayList<RewardStruct>) rewardStructs.clone();
		clone.rewardStructNames  = (ArrayList<String>) rewardStructNames.clone();
		clone.observableVarLists = (ArrayList<ObservableVars>) observableVarLists.clone();
		clone.observableDefns    = (ArrayList<Observable>) observableDefns.clone();
		clone.varDecls           = (ArrayList<Declaration>) varDecls.clone();
		clone.varNames           = (ArrayList<String>) varNames.clone();
		clone.varTypes           = (ArrayList<Type>) varTypes.clone();
		clone.varModules         = (ArrayList<Integer>) varModules.clone();
		clone.observables        = (ArrayList<Observable>) observables.clone();
		clone.observableNames    = (ArrayList<String>) observableNames.clone();
		clone.observableTypes    = (ArrayList<Type>) observableTypes.clone();
		clone.observableVars     = (ArrayList<String>) observableVars.clone();

		// clone other (generated) info
		if (constantValues != null)
			clone.constantValues = constantValues.clone();
		if (moduleNames != null)
			clone.moduleNames = moduleNames.clone();
		if (synchs != null)
			clone.synchs =  (ArrayList<String>) synchs.clone();
		if (ecUndefined != null)
			ecUndefined = EvaluateContext.create(ecUndefined);
		if (ec != null)
			ec = EvaluateContext.create(ec);

		return clone;
	}
}

// ------------------------------------------------------------------------------
