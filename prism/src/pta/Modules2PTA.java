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

package pta;

import java.util.*;

import explicit.IndexedSet;
import explicit.StateStorage;

import parser.*;
import parser.ast.*;
import parser.type.*;
import parser.visitor.ASTTraverse;
import prism.*;

/**
 * Class that converts a PRISM modelling language description
 * of a PTA into data structures in the pta package.
 */
public class Modules2PTA extends PrismComponent
{
	// Setting(s)
	protected double sumRoundOff;

	// Model to be converted
	private ModulesFile modulesFile;
	// Constants from model
	private Values constantValues;

	/**
	 * Constructor.
	 */
	public Modules2PTA(PrismComponent parent, ModulesFile modulesFile) throws PrismException
	{
		super(parent);
		sumRoundOff = settings.getDouble(PrismSettings.PRISM_SUM_ROUND_OFF);
		this.modulesFile = modulesFile;
		constantValues = modulesFile.getConstantValues();
	}

	/**
	 * Main method - translate.
	 */
	public PTA translate() throws PrismLangException
	{
		int i, numModules;
		parser.ast.Module module, moduleNew;
		ArrayList<String> nonClocks;
		ArrayList<String> allNonClocks = new ArrayList<String>();
		ArrayList<ArrayList<State>> pcStates;
		PTA pta, pta2;

		// Do a few basic checks on the model
		if (modulesFile.getModelType() != ModelType.PTA)
			throw new PrismLangException("Model is not a PTA");
		if (modulesFile.getNumGlobals() > 0)
			throw new PrismLangException("PTA models cannot have global variables");
		if (modulesFile.getInitialStates() != null)
			throw new PrismLangException("PTA models cannot use init...endinit");

		// Check for inter-module variable references 
		modulesFile.accept(new ASTTraverse()
		{
			private parser.ast.Module inModule = null;

			public void visitPre(parser.ast.Module e) throws PrismLangException
			{
				// Register the fact we are entering a module
				inModule = e;
			}

			public void visitPost(parser.ast.Module e) throws PrismLangException
			{
				// Register the fact we are leaving a module
				inModule = null;
			}

			public void visitPost(ExpressionVar e) throws PrismLangException
			{
				// For PTAs, references to variables in modules have to be local
				if (inModule != null) {
					if (!inModule.isLocalVariable(e.getName())) {
						throw new PrismLangException("Modules in a PTA cannot access non-local variables", e);
					}
				}
			}
		});
		
		// Clone the model file, replace any constants with values,
		// and simplify any expressions as much as possible.
		modulesFile = (ModulesFile) modulesFile.deepCopy().replaceConstants(constantValues).simplify();

		// Remove formulas/labels from (cloned) model - these are not translated.
		modulesFile.setFormulaList(new FormulaList());
		modulesFile.setLabelList(new LabelList());

		// Also remove reward structures - these are not currently used
		modulesFile.clearRewardStructs();

		// Go through list of modules
		numModules = modulesFile.getNumModules();
		pcStates = new ArrayList<ArrayList<State>>(numModules);
		for (i = 0; i < numModules; i++) {
			// Find non-clock variables in module
			module = modulesFile.getModule(i);
			nonClocks = new ArrayList<String>();
			for (Declaration decl : module.getDeclarations()) {
				if (!(decl.getType() instanceof TypeClock)) {
					nonClocks.add(decl.getName());
				}
			}
			allNonClocks.addAll(nonClocks);
			// Convert module to program counter form
			pcStates.add(new ArrayList<State>());
			module = modulesFile.getModule(i);
			moduleNew = convertModuleToPCForm(module, nonClocks, pcStates.get(i));
			// Replace module in model
			modulesFile.setModule(i, moduleNew);
		}
		// Re-compute all variable related info in model
		modulesFile.recomputeVariableinformation();

		// Convert each module to a PTA and do parallel composition
		numModules = modulesFile.getNumModules();
		pta = null;
		for (i = 0; i < numModules; i++) {
			module = modulesFile.getModule(i);
			pta2 = translateModule(module, pcStates.get(i));
			//mainLog.println(pta2);
			pta = (pta == null) ? pta2 : new PTAParallel().compose(pta, pta2);
		}
		//mainLog.println(pta);

		// Pass the list of non-clock variables to the PTA  
		pta.setLocationNameVars(allNonClocks);

		return pta;
	}

	/**
	 * Translate a single module.
	 * (Which has been transformed using convertModuleToPCForm)
	 */
	private PTA translateModule(parser.ast.Module module, ArrayList<State> pcStates) throws PrismLangException
	{
		// Clocks and PC variable stuff
		ArrayList<String> clocks;
		String pc;
		int pcMax, pcVal;
		Values pcValues;
		// Model components
		Update up;
		Expression expr, invar;
		List<Expression> exprs;
		int numVars, numUpdates, numElements;
		// PTA stuff
		PTA pta;
		Transition tr;
		Edge edge;
		// Misc
		int i, j, k;
		double prob, probSum;

		// Determine PC variable and clock variables in module
		pc = module.getDeclaration(0).getName();
		pcMax = ((DeclarationInt) module.getDeclaration(0).getDeclType()).getHigh().evaluateInt();
		numVars = module.getNumDeclarations();
		clocks = new ArrayList<String>();
		for (i = 1; i < numVars; i++) {
			clocks.add(module.getDeclaration(i).getName());
		}

		// Create new PTA and add a clock for each clock variable
		pta = new PTA(new ArrayList<String>(module.getAllSynchs()));
		for (String clockName : clocks)
			pta.addClock(clockName);

		// Add locations corresponding to PC
		// (labels for locations come from the State objects generated
		// when converting the module to PC form).
		for (i = 0; i < pcMax + 1; i++) {
			pta.addLocation(pcStates.get(i));
		}

		// Process invariant to determine guard for each location
		for (i = 0; i < pcMax + 1; i++) {
			invar = module.getInvariant();
			if (invar != null) {
				// Get (copy of) existing invariant for module
				invar = module.getInvariant().deepCopy();
				// Evaluate (partially) invariant for this pc value
				pcValues = new Values();
				pcValues.setValue(pc, i);
				// Evaluate (partially) invariant for this PC value
				invar = (Expression) invar.evaluatePartially(null, pcValues).simplify();
				// The (partial) invariant should now be a conjunction of clock constraints (or true)
				// Split into parts, convert to constraints and add to PTA (unless "true")
				// If expression is not (syntactically) convex, complain
				exprs = ParserUtils.splitConjunction(invar);
				for (Expression ex : exprs) {
					if (!(Expression.isTrue(ex) || Expression.isFalse(ex))) {
						checkIsSimpleClockConstraint(ex);
					}
				}
				for (Expression ex : exprs) {
					if (!Expression.isTrue(ex)) {
						for (Constraint c : exprToConstraint(ex, pta)) {
							pta.addInvariantCondition(i, c);
						}
					}
				}
			}
		}

		// For each command in the module
		for (Command command : module.getCommands()) {

			// Guard is known to be a conjunction where LHS is of form pc=i
			// Extract value i from this and put in pcVal and pcValues
			expr = ((ExpressionBinaryOp) command.getGuard()).getOperand1();
			pcVal = ((ExpressionBinaryOp) expr).getOperand2().evaluateInt();
			pcValues = new Values();
			pcValues.setValue(pc, pcVal);
			// RHS of guard should be conjunction of clock constraints (or true)
			// Split into parts, convert to constraints and add to new PTA transition
			// If expression is not (syntactically) convex, complain
			tr = pta.addTransition(pcVal, command.getSynch());
			exprs = ParserUtils.splitConjunction(((ExpressionBinaryOp) command.getGuard()).getOperand2());
			for (Expression ex : exprs) {
				if (!(Expression.isTrue(ex) || Expression.isFalse(ex))) {
					checkIsSimpleClockConstraint(ex);
				}
			}
			for (Expression ex2 : exprs) {
				if (!Expression.isTrue(ex2)) {
					for (Constraint c : exprToConstraint(ex2, pta)) {
						tr.addGuardConstraint(c);
					}
				}
			}

			// Go through all updates
			numUpdates = command.getUpdates().getNumUpdates();
			probSum = 0.0;
			for (j = 0; j < numUpdates; j++) {
				up = command.getUpdates().getUpdate(j);
				// Compute probability
				expr = command.getUpdates().getProbability(j);
				prob = (expr == null) ? 1.0 : expr.evaluateDouble(constantValues, pcValues);
				if (prob < 0)
					throw new PrismLangException("Negative probability (" + prob + ") found in PTA");
				if (prob > 1)
					throw new PrismLangException("Probability greater than 1 (" + prob + ") found in PTA");
				probSum += prob;
				// Create edge (destination is temporarily -1 since not known yet)
				edge = tr.addEdge(prob, -1);
				// Go through elements of update
				numElements = up.getNumElements();
				for (k = 0; k < numElements; k++) {
					// Determine destination location and add to edge
					if (up.getVar(k).equals(pc))
						edge.setDestination(up.getExpression(k).evaluateInt(constantValues, pcValues));
					else {
						if (!clocks.contains(up.getVar(k)))
							throw new PrismLangException("Update to non-clock found", up);
						int val = up.getExpression(k).evaluateInt(constantValues, pcValues);
						edge.addReset(pta.getClockIndex(up.getVar(k)), val);
					}
				}
				// If no destination found, must be a loop
				if (edge.getDestination() == -1)
					edge.setDestination(tr.getSource());
			}
			// Check probabilities sum to one (ish)
			if (!PrismUtils.doublesAreCloseAbs(probSum, 1.0, sumRoundOff)) {
				throw new PrismLangException("Probabilities do not sum to one (" + probSum + ") in PTA");
			}
		}

		return pta;
	}

	/**
	 * Check whether a PRISM expression (over clock variables) is a "simple" clock constraint, i.e. of the form
	 * x~c or x~y where x and y are clocks, c is an integer-valued expression and ~ is one of <, <=, >=, >, =.
	 * Throws an explanatory exception if not.
	 * @param expr: The expression to be checked.
	 */
	private void checkIsSimpleClockConstraint(Expression expr) throws PrismLangException
	{
		ExpressionBinaryOp exprRelOp;
		Expression expr1, expr2;
		int op, clocks = 0;

		// Check is rel op
		if (!Expression.isRelOp(expr))
			throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
		// Split into parts
		exprRelOp = (ExpressionBinaryOp) expr;
		op = exprRelOp.getOperator();
		expr1 = exprRelOp.getOperand1();
		expr2 = exprRelOp.getOperand2();
		// Check operator is of allowed type
		if (!ExpressionBinaryOp.isRelOp(op))
			throw new PrismLangException("Can't use operator " + exprRelOp.getOperatorSymbol() + " in clock constraint \"" + expr + "\"", expr);
		if (op == ExpressionBinaryOp.NE)
			throw new PrismLangException("Can't use negation in clock constraint \"" + expr + "\"", expr);
		// LHS
		if (expr1.getType() instanceof TypeClock) {
			if (!(expr1 instanceof ExpressionVar)) {
				throw new PrismLangException("Invalid clock expression \"" + expr1 + "\"", expr1);
			}
			clocks++;
		} else if (expr1.getType() instanceof TypeInt) {
			if (!expr1.isConstant()) {
				throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
			}
		} else {
			throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
		}
		// RHS
		if (expr2.getType() instanceof TypeClock) {
			if (!(expr2 instanceof ExpressionVar)) {
				throw new PrismLangException("Invalid clock expression \"" + expr2 + "\"", expr2);
			}
			clocks++;
		} else if (expr2.getType() instanceof TypeInt) {
			if (!expr2.isConstant()) {
				throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
			}
		} else {
			throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
		}
		// Should be at least one clock
		if (clocks == 0)
			throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
	}

	/**
	 * Convert a PRISM expression representing a (simple) clock constraint into
	 * the Constraint data structures used in the pta package.
	 * Actually creates a list of constraints (since e.g. x=c maps to multiple constraints) 
	 * @param expr: The expression to be converted.
	 * @param pta: The PTA for which this constraint will be used. 
	 */
	private List<Constraint> exprToConstraint(Expression expr, PTA pta) throws PrismLangException
	{
		ExpressionBinaryOp exprRelOp;
		Expression expr1, expr2;
		int x, y, v;
		List<Constraint> res = new ArrayList<Constraint>();

		// Check is rel op and split into parts
		if (!Expression.isRelOp(expr))
			throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
		exprRelOp = (ExpressionBinaryOp) expr;
		expr1 = exprRelOp.getOperand1();
		expr2 = exprRelOp.getOperand2();
		// 3 cases...
		if (expr1.getType() instanceof TypeClock) {
			// Comparison of two clocks (x ~ y)
			if (expr2.getType() instanceof TypeClock) {
				x = pta.getClockIndex(((ExpressionVar) expr1).getName());
				if (x < 0)
					throw new PrismLangException("Unknown clock \"" + ((ExpressionVar) expr1).getName() + "\"", expr);
				y = pta.getClockIndex(((ExpressionVar) expr2).getName());
				if (y < 0)
					throw new PrismLangException("Unknown clock \"" + ((ExpressionVar) expr2).getName() + "\"", expr);
				switch (exprRelOp.getOperator()) {
				case ExpressionBinaryOp.EQ:
					res.add(Constraint.buildXGeqY(x, y));
					res.add(Constraint.buildXLeqY(x, y));
					break;
				case ExpressionBinaryOp.NE:
					throw new PrismLangException("Can't use negation in clock constraint \"" + expr + "\"", expr);
				case ExpressionBinaryOp.GT:
					res.add(Constraint.buildXGtY(x, y));
					break;
				case ExpressionBinaryOp.GE:
					res.add(Constraint.buildXGeqY(x, y));
					break;
				case ExpressionBinaryOp.LT:
					res.add(Constraint.buildXLtY(x, y));
					break;
				case ExpressionBinaryOp.LE:
					res.add(Constraint.buildXLeqY(x, y));
					break;
				}
				return res;
			}
			// Comparison of clock and integer (x ~ v)
			else {
				x = pta.getClockIndex(((ExpressionVar) expr1).getName());
				if (x < 0)
					throw new PrismLangException("Unknown clock \"" + ((ExpressionVar) expr1).getName() + "\"", expr);
				v = expr2.evaluateInt(constantValues);
				switch (exprRelOp.getOperator()) {
				case ExpressionBinaryOp.EQ:
					res.add(Constraint.buildGeq(x, v));
					res.add(Constraint.buildLeq(x, v));
					break;
				case ExpressionBinaryOp.NE:
					throw new PrismLangException("Can't use negation in clock constraint \"" + expr + "\"", expr);
				case ExpressionBinaryOp.GT:
					res.add(Constraint.buildGt(x, v));
					break;
				case ExpressionBinaryOp.GE:
					res.add(Constraint.buildGeq(x, v));
					break;
				case ExpressionBinaryOp.LT:
					res.add(Constraint.buildLt(x, v));
					break;
				case ExpressionBinaryOp.LE:
					res.add(Constraint.buildLeq(x, v));
					break;
				}
				return res;
			}
		}
		// Comparison of integer and clock (v ~ x)
		else if (expr2.getType() instanceof TypeClock) {
			x = pta.getClockIndex(((ExpressionVar) expr2).getName());
			if (x < 0)
				throw new PrismLangException("Unknown clock \"" + ((ExpressionVar) expr2).getName() + "\"", expr);
			v = expr1.evaluateInt(constantValues);
			switch (exprRelOp.getOperator()) {
			case ExpressionBinaryOp.EQ:
				res.add(Constraint.buildGeq(x, v));
				res.add(Constraint.buildLeq(x, v));
				break;
			case ExpressionBinaryOp.NE:
				throw new PrismLangException("Can't use negation in clock constraint \"" + expr + "\"", expr);
			case ExpressionBinaryOp.GT:
				res.add(Constraint.buildLt(x, v));
				break;
			case ExpressionBinaryOp.GE:
				res.add(Constraint.buildLeq(x, v));
				break;
			case ExpressionBinaryOp.LT:
				res.add(Constraint.buildGt(x, v));
				break;
			case ExpressionBinaryOp.LE:
				res.add(Constraint.buildGeq(x, v));
				break;
			}
			return res;
		}
		throw new PrismLangException("Invalid clock constraint \"" + expr + "\"", expr);
	}

	/**
	 * Modify a module, converting a set of variables into a single 0-indexed program counter (PC) variable.
	 * The PC variable is guaranteed to be first in the list of new module variables.
	 * Every new guard takes the form of a conjunction with the LHS of the form pc=i,
	 * where pc is the PC variable and i is an integer literal.
	 * Similarly, the first element of every update is of the form (pc'=i).
	 * The original module is not changed; the new one is returned.
	 * In addition, a list of State objects, representing the meaning of each PC value is stored in pcStates.
	 * The method recomputeVariableinformation() will need to be called on the ModulesFile
	 * containing this module afterwards, since this changes the global indices of variables.
	 * @param module: The module to convert.
	 * @param pcVars: The variables that should be converted to a PC.
	 * @param pcStates: An empty ArrayList into which PC value states will be stored.
	 */
	private parser.ast.Module convertModuleToPCForm(parser.ast.Module module, List<String> pcVars, ArrayList<State> pcStates) throws PrismLangException
	{
		// Info about variables in model to be used as program counter
		int pcNumVars;
		State pcInit;
		// info about new program counter var
		String pc;
		// Components of old and new module 
		parser.ast.Module moduleNew;
		Declaration decl, declNew;
		Command command, commandNew;
		Expression guard, guardNew;
		Updates updates, updatesNew;
		Update update, updateNew;
		Expression invar, invarNew;
		Expression exprPc;
		int numUpdates, numCommands, numElements;
		// Stuff needed to do local reachability search
		int src, dest;
		StateStorage<State> states;
		LinkedList<State> explore;
		State state, stateNew;
		int[] varMap;
		// Misc
		int i, j, k, numVars;

		// For now, assume all constant values have been replaced
		// Later, can assume that just some required subset have. 

		// Store number of PC variables and get their initial values 
		pcNumVars = pcVars.size();
		pcInit = new State(pcNumVars);
		for (i = 0; i < pcNumVars; i++) {
			decl = modulesFile.getVarDeclaration(modulesFile.getVarIndex(pcVars.get(i)));
			pcInit.setValue(i, decl.getStartOrDefault().evaluate(constantValues));
		}

		// Build variable index mapping
		numVars = modulesFile.getNumVars();
		varMap = new int[numVars];
		for (i = 0; i < numVars; i++) {
			varMap[i] = -1;
		}
		for (i = 0; i < pcNumVars; i++) {
			varMap[modulesFile.getVarIndex(pcVars.get(i))] = i;
		}

		// Choose name for new program counter
		// (concatenate replaced variables, prefixed with underscores,
		// (and append more underscores until unique)
		pc = "";
		for (i = 0; i < pcNumVars; i++) {
			pc += "_" + pcVars.get(i);
		}
		while (!identIsUnused(pc)) {
			pc += "_";
		}

		// Create a new module
		moduleNew = new parser.ast.Module(module.getName());
		
		// Preserve alphabet of old module (might change if some commands are not enabled)
		moduleNew.setAlphabet(module.getAllSynchs());

		// Create invariant - will be constructed below
		invarNew = null;

		// Explore local state space of module

		// Initialise states storage
		states = new IndexedSet<State>();
		explore = new LinkedList<State>();
		// Add initial state
		state = pcInit;
		states.add(state);
		explore.add(state);
		src = -1;
		try {
			while (!explore.isEmpty()) {
				// Pick next state to explore
				// (they are stored in order found so know index is src+1)
				state = explore.removeFirst();
				src++;
				// Build expression for this PC state
				exprPc = new ExpressionVar(pc, TypeInt.getInstance());
				exprPc = new ExpressionBinaryOp(ExpressionBinaryOp.EQ, exprPc, Expression.Int(src));
				// For each command in the module
				numCommands = module.getNumCommands();
				for (i = 0; i < numCommands; i++) {
					command = module.getCommand(i);
					// See if guard is potentially true for this PC value
					guard = command.getGuard();
					guard = (Expression) guard.deepCopy().evaluatePartially(state, varMap).simplify();
					if (!Expression.isFalse(guard)) {
						// If so, build a new command
						commandNew = new Command();
						commandNew.setSynch(command.getSynch());
						guardNew = Expression.And(exprPc, guard);
						commandNew.setGuard(guardNew);
						// Go through updates, modifying them 
						updates = command.getUpdates();
						updatesNew = new Updates();
						numUpdates = updates.getNumUpdates();
						for (j = 0; j < numUpdates; j++) {
							update = updates.getUpdate(j);
							// Determine successor (PC) state
							stateNew = new State(state);
							update.updatePartially(state, stateNew, varMap);
							// If new, add it to explore list
							if (states.add(stateNew)) {
								explore.add(stateNew);
							}
							dest = states.getIndexOfLastAdd();
							// Build new update
							updateNew = new Update();
							updateNew.addElement(new ExpressionIdent(pc), Expression.Int(dest));
							numElements = update.getNumElements();
							// Copy across rest of old update  
							for (k = 0; k < numElements; k++) {
								if (varMap[update.getVarIndex(k)] == -1) {
									updateNew.addElement(update.getVarIdent(k), update.getExpression(k));
								}
							}
							// we translate the probability as well, as it may reference states
							Expression probNew = updates.getProbability(j);
							if (probNew != null) {
								// if probability expression is null, it implicitly encodes probability 1,
								// so we don't have to change anything
								probNew = probNew.deepCopy();
								probNew = (Expression) probNew.evaluatePartially(state, varMap).simplify();
							}
							updatesNew.addUpdate(probNew, updateNew);
						}
						// Add new stuff to new module
						commandNew.setUpdates(updatesNew);
						moduleNew.addCommand(commandNew);
					}
				}

				// Also generate the (clock) invariant for this state
				invar = module.getInvariant();
				if (invar != null) {
					// Get (copy of) existing invariant for module
					invar = invar.deepCopy();
					// Evaluate (partially) invariant for this PC value
					invar = (Expression) invar.evaluatePartially(state, varMap).simplify();
					// If not "true", add into new invariant
					if (!Expression.isTrue(invar)) {
						invar = Expression.Parenth(Expression.Implies(exprPc, invar));
						invarNew = (invarNew == null) ? invar : Expression.And(invarNew, invar);
					}
				}
			}
		}
		// Catch a (possibly) common source of mem-out errors during explicit-state reachability
		catch (OutOfMemoryError e) {
			states.clear();
			System.gc();
			throw new PrismLangException("Out of memory after exploring " + (src + 1) + " states of module " + module.getName(), module);
		}

		// Set the invariant for the new module
		moduleNew.setInvariant(invarNew);

		// Add variables to module
		// (one for PC, then all original non-PC variables)
		declNew = new Declaration(pc, new DeclarationInt(Expression.Int(0), Expression.Int(states.size() - 1)));
		moduleNew.addDeclaration(declNew);
		for (Declaration d : module.getDeclarations()) {
			if (!pcVars.contains(d.getName())) {
				moduleNew.addDeclaration((Declaration) d.deepCopy());
			}
		}

		// Store the list of states representing the values of the PC
		if (pcStates != null) {
			states.toArrayList(pcStates);
			//Collections.sort(pcStates);
			//mainLog.println(pcStates);
		}

		return moduleNew;
	}

	/**
	 * Local utility method to test if a new identifier is safe to use,
	 * i.e. does not appear in model file or (if present) property file.
	 */
	private boolean identIsUnused(String id)
	{
		if (modulesFile.isIdentUsed(id))
			return false;
		// TODO: fix when have added prop file
		//if (propertiesFile != null && propertiesFile.getAllIdentsUsed().indexOf(id) != -1)
		//return false;
		return true;
	}
}
