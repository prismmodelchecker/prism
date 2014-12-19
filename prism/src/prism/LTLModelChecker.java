//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Carlos S. Bederian (Universidad Nacional de Cordoba)
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

package prism;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Vector;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import parser.VarList;
import parser.ast.Declaration;
import parser.ast.DeclarationInt;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import parser.type.TypeBool;
import parser.type.TypePathBool;

/**
 * LTL model checking functionality
 */
public class LTLModelChecker extends PrismComponent
{
	/**
	 * Create a new DTMCModelChecker, inherit basic state from parent (unless null).
	 */
	public LTLModelChecker(PrismComponent parent) throws PrismException
	{
		super(parent);
	}

	/**
	 * Convert an LTL formula into a DRA. The LTL formula is represented as a PRISM Expression,
	 * in which atomic propositions are represented by ExpressionLabel objects.
	 */
	public static DRA<BitSet> convertLTLFormulaToDRA(Expression ltl) throws PrismException
	{
		return LTL2RabinLibrary.convertLTLFormulaToDRA(ltl);
	}

	/**
	 * Extract maximal state formula from an LTL path formula, model check them (with passed in model checker) and
	 * replace them with ExpressionLabel objects L0, L1, etc. Expression passed in is modified directly, but the result
	 * is also returned. As an optimisation, model checking that results in true/false for all states is converted to an
	 * actual true/false, and duplicate results are given the same label. BDDs giving the states which satisfy each label
	 * are put into the vector  labelDDs, which should be empty when this function is called.
	 */
	public Expression checkMaximalStateFormulas(ModelChecker mc, Model model, Expression expr, Vector<JDDNode> labelDDs) throws PrismException
	{
		// A state formula
		if (expr.getType() instanceof TypeBool) {
			// Model check
			JDDNode dd = mc.checkExpressionDD(expr);
			// Detect special cases (true, false) for optimisation
			if (dd.equals(JDD.ZERO)) {
				JDD.Deref(dd);
				return Expression.False();
			}
			if (dd.equals(model.getReach())) {
				JDD.Deref(dd);
				return Expression.True();
			}
			// See if we already have an identical result
			// (in which case, reuse it)
			int i = labelDDs.indexOf(dd);
			if (i != -1) {
				JDD.Deref(dd);
				return new ExpressionLabel("L" + i);
			}
			// Otherwise, add result to list, return new label
			labelDDs.add(dd);
			return new ExpressionLabel("L" + (labelDDs.size() - 1));
		}
		// A path formula (recurse, modify, return)
		else if (expr.getType() instanceof TypePathBool) {
			if (expr instanceof ExpressionBinaryOp) {
				ExpressionBinaryOp exprBinOp = (ExpressionBinaryOp) expr;
				exprBinOp.setOperand1(checkMaximalStateFormulas(mc, model, exprBinOp.getOperand1(), labelDDs));
				exprBinOp.setOperand2(checkMaximalStateFormulas(mc, model, exprBinOp.getOperand2(), labelDDs));
			} else if (expr instanceof ExpressionUnaryOp) {
				ExpressionUnaryOp exprUnOp = (ExpressionUnaryOp) expr;
				exprUnOp.setOperand(checkMaximalStateFormulas(mc, model, exprUnOp.getOperand(), labelDDs));
			} else if (expr instanceof ExpressionTemporal) {
				ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
				if (exprTemp.getOperand1() != null) {
					exprTemp.setOperand1(checkMaximalStateFormulas(mc, model, exprTemp.getOperand1(), labelDDs));
				}
				if (exprTemp.getOperand2() != null) {
					exprTemp.setOperand2(checkMaximalStateFormulas(mc, model, exprTemp.getOperand2(), labelDDs));
				}
			}
		}
		return expr;
	}

	/**
	 * Construct the product of a DRA and a DTMC/CTMC.
	 * @param dra The DRA
	 * @param model The  DTMC/CTMC
	 * @param labelDDs BDDs giving the set of states for each AP in the DRA
	 */
	public ProbModel constructProductMC(DRA<BitSet> dra, ProbModel model, Vector<JDDNode> labelDDs) throws PrismException
	{
		return constructProductMC(dra, model, labelDDs, null, null, true);
	}

	/**
	 * Construct the product of a DRA and a DTMC/CTMC.
	 * @param dra The DRA
	 * @param model The  DTMC/CTMC
	 * @param labelDDs BDDs giving the set of states for each AP in the DRA
	 * @param draDDRowVarsCopy (Optionally) empty JDDVars object to obtain copy of DD row vars for DRA
	 * @param draDDColVarsCopy (Optionally) empty JDDVars object to obtain copy of DD col vars for DRA
	 */
	public ProbModel constructProductMC(DRA<BitSet> dra, ProbModel model, Vector<JDDNode> labelDDs, JDDVars draDDRowVarsCopy, JDDVars draDDColVarsCopy)
			throws PrismException
	{
		return constructProductMC(dra, model, labelDDs, draDDRowVarsCopy, draDDColVarsCopy, true);
	}

	/**
	 * Construct the product of a DRA and a DTMC/CTMC.
	 * @param dra The DRA
	 * @param model The  DTMC/CTMC
	 * @param labelDDs BDDs giving the set of states for each AP in the DRA
	 * @param draDDRowVarsCopy (Optionally) empty JDDVars object to obtain copy of DD row vars for DRA
	 * @param draDDColVarsCopy (Optionally) empty JDDVars object to obtain copy of DD col vars for DRA
	 * @param allInit Do we assume that all states of the original model are initial states?
	 *        (just for the purposes of reachability)
	 */
	public ProbModel constructProductMC(DRA<BitSet> dra, ProbModel model, Vector<JDDNode> labelDDs, JDDVars draDDRowVarsCopy, JDDVars draDDColVarsCopy,
			boolean allInit) throws PrismException
	{
		// Existing model - dds, vars, etc.
		JDDVars varDDRowVars[];
		JDDVars varDDColVars[];
		JDDVars allDDRowVars;
		JDDVars allDDColVars;
		Vector<String> ddVarNames;
		VarList varList;
		// New (product) model - dds, vars, etc.
		JDDNode newTrans, newStart;
		JDDVars newVarDDRowVars[], newVarDDColVars[];
		JDDVars newAllDDRowVars, newAllDDColVars;
		Vector<String> newDDVarNames;
		VarList newVarList;
		String draVar;
		// DRA stuff
		JDDVars draDDRowVars, draDDColVars;
		// Misc
		int i, j, n;
		boolean before;

		// Get details of old model
		varDDRowVars = model.getVarDDRowVars();
		varDDColVars = model.getVarDDColVars();
		allDDRowVars = model.getAllDDRowVars();
		allDDColVars = model.getAllDDColVars();
		ddVarNames = model.getDDVarNames();
		varList = model.getVarList();

		// Create a (new, unique) name for the variable that will represent DRA states
		draVar = "_dra";
		while (varList.getIndex(draVar) != -1) {
			draVar = "_" + draVar;
		}

		// See how many new dd vars will be needed for DRA
		// and whether there is room to put them before rather than after the existing vars
		// (if DRA only has one state, we add an extra dummy state)
		n = (int) Math.ceil(PrismUtils.log2(dra.size()));
		n = Math.max(n, 1);
		before = true;
		if (allDDRowVars.getMinVarIndex() - 1 < 2 * n) {
			before = false;
		}

		// If passed in var lists are null, create new lists
		// (which won't be accessible later in this case)
		draDDRowVars = (draDDRowVarsCopy == null) ? new JDDVars() : draDDRowVarsCopy;
		draDDColVars = (draDDColVarsCopy == null) ? new JDDVars() : draDDColVarsCopy;
		// Create the new dd variables
		newDDVarNames = new Vector<String>();
		newDDVarNames.addAll(ddVarNames);
		j = before ? allDDRowVars.getMinVarIndex() - 2 * n : model.getAllDDColVars().getMaxVarIndex() + 1;
		for (i = 0; i < n; i++) {
			draDDRowVars.addVar(JDD.Var(j++));
			draDDColVars.addVar(JDD.Var(j++));
			if (!before) {
				newDDVarNames.add("");
				newDDVarNames.add("");
			}
			newDDVarNames.set(j - 2, draVar + "." + i);
			newDDVarNames.set(j - 1, draVar + "'." + i);
		}

		// Create/populate new lists
		newVarDDRowVars = new JDDVars[varDDRowVars.length + 1];
		newVarDDColVars = new JDDVars[varDDRowVars.length + 1];
		newVarDDRowVars[before ? 0 : varDDRowVars.length] = draDDRowVars;
		newVarDDColVars[before ? 0 : varDDColVars.length] = draDDColVars;
		for (i = 0; i < varDDRowVars.length; i++) {
			newVarDDRowVars[before ? i + 1 : i] = new JDDVars();
			newVarDDColVars[before ? i + 1 : i] = new JDDVars();
			newVarDDRowVars[before ? i + 1 : i].addVars(varDDRowVars[i]);
			newVarDDColVars[before ? i + 1 : i].addVars(varDDColVars[i]);
		}
		newAllDDRowVars = new JDDVars();
		newAllDDColVars = new JDDVars();
		if (before) {
			newAllDDRowVars.addVars(draDDRowVars);
			newAllDDColVars.addVars(draDDColVars);
			newAllDDRowVars.addVars(allDDRowVars);
			newAllDDColVars.addVars(allDDColVars);
		} else {
			newAllDDRowVars.addVars(allDDRowVars);
			newAllDDColVars.addVars(allDDColVars);
			newAllDDRowVars.addVars(draDDRowVars);
			newAllDDColVars.addVars(draDDColVars);
		}
		newVarList = (VarList) varList.clone();
		// NB: if DRA only has one state, we add an extra dummy state
		Declaration decl = new Declaration(draVar, new DeclarationInt(Expression.Int(0), Expression.Int(Math.max(dra.size() - 1, 1))));
		newVarList.addVar(before ? 0 : varList.getNumVars(), decl, 1, model.getConstantValues());

		// Extra references (because will get derefed when new model is done with)
		allDDRowVars.refAll();
		allDDRowVars.refAll();
		allDDColVars.refAll();
		allDDColVars.refAll();
		for (i = 0; i < model.getNumModules(); i++) {
			model.getModuleDDRowVars(i).refAll();
			model.getModuleDDColVars(i).refAll();
		}
		draDDRowVars.refAll();
		draDDColVars.refAll();

		// Build transition matrix for product
		newTrans = buildTransMask(dra, labelDDs, allDDRowVars, allDDColVars, draDDRowVars, draDDColVars);
		JDD.Ref(model.getTrans());
		newTrans = JDD.Apply(JDD.TIMES, model.getTrans(), newTrans);

		// Build set of initial states for product
		// Note, by default, we take product of *all* states of the original model, not just its initial states.
		// Initial states are only used for reachability in this instance.
		// We need to ensure that the product model includes states corresponding to all
		// states of the original model (because we compute probabilities for all of them)
		// but some of these may not be reachable from the initial state of the product model.
		// Optionally (if allInit is false), we don't do this - maybe because we only care about result for the initial state
		// Note that we reset the initial states after reachability, corresponding to just the initial states of the original model.  
		newStart = buildStartMask(dra, labelDDs, draDDRowVars);
		JDD.Ref(allInit ? model.getReach() : model.getStart());
		newStart = JDD.And(allInit ? model.getReach() : model.getStart(), newStart);

		// Create a new model model object to store the product model
		ProbModel modelProd = new ProbModel(
		// New transition matrix/start state
				newTrans, newStart,
				// Don't pass in any rewards info
				new JDDNode[0], new JDDNode[0], new String[0],
				// New list of all row/col vars
				newAllDDRowVars, newAllDDColVars,
				// New list of var names
				newDDVarNames,
				// Module info (unchanged)
				model.getNumModules(), model.getModuleNames(), model.getModuleDDRowVars(), model.getModuleDDColVars(),
				// New var info
				model.getNumVars() + 1, newVarList, newVarDDRowVars, newVarDDColVars,
				// Constants (no change)
				model.getConstantValues());

		// Do reachability/etc. for the new model
		modelProd.doReachability();
		modelProd.filterReachableStates();
		modelProd.findDeadlocks(false);
		if (modelProd.getDeadlockStates().size() > 0) {
			// Assuming original model has no deadlocks, neither should product
			throw new PrismException("Model-DRA product has deadlock states");
		}

		// Reset initial state
		newStart = buildStartMask(dra, labelDDs, draDDRowVars);
		JDD.Ref(model.getStart());
		newStart = JDD.And(model.getStart(), newStart);
		modelProd.setStart(newStart);

		// Reference DRA DD vars (if being returned)
		if (draDDRowVarsCopy != null)
			draDDRowVarsCopy.refAll();
		if (draDDColVarsCopy != null)
			draDDColVarsCopy.refAll();

		return modelProd;
	}

	/**
	 * Construct the product of a DRA and an MDP.
	 * @param dra The DRA
	 * @param model The MDP
	 * @param labelDDs BDDs giving the set of states for each AP in the DRA
	 */
	public NondetModel constructProductMDP(DRA<BitSet> dra, NondetModel model, Vector<JDDNode> labelDDs) throws PrismException
	{
		return constructProductMDP(dra, model, labelDDs, null, null, true, null);
	}

	/**
	 * Construct the product of a DRA and an MDP.
	 * @param dra The DRA
	 * @param model The MDP
	 * @param labelDDs BDDs giving the set of states for each AP in the DRA
	 * @param draDDRowVarsCopy (Optionally) empty JDDVars object to obtain copy of DD row vars for DRA
	 * @param draDDColVarsCopy (Optionally) empty JDDVars object to obtain copy of DD col vars for DRA
	 */
	public NondetModel constructProductMDP(DRA<BitSet> dra, NondetModel model, Vector<JDDNode> labelDDs, JDDVars draDDRowVarsCopy, JDDVars draDDColVarsCopy)
			throws PrismException
	{
		return constructProductMDP(dra, model, labelDDs, draDDRowVarsCopy, draDDColVarsCopy, true, null);
	}

	/**
	 * Construct the product of a DRA and an MDP.
	 * @param dra The DRA
	 * @param model The MDP
	 * @param labelDDs BDDs giving the set of states for each AP in the DRA
	 * @param draDDRowVarsCopy (Optionally) empty JDDVars object to obtain copy of DD row vars for DRA
	 * @param draDDColVarsCopy (Optionally) empty JDDVars object to obtain copy of DD col vars for DRA
	 * @param allInit Do we assume that all states of the original model are initial states?
	 *        (just for the purposes of reachability) If not, the required initial states should be given
	 * @param init The initial state(s) (of the original model) used to build the product;
	 *        if null; we just take the existing initial states from model.getStart().
	 */
	public NondetModel constructProductMDP(DRA<BitSet> dra, NondetModel model, Vector<JDDNode> labelDDs, JDDVars draDDRowVarsCopy, JDDVars draDDColVarsCopy,
			boolean allInit, JDDNode init) throws PrismException
	{
		// Existing model - dds, vars, etc.
		JDDVars varDDRowVars[];
		JDDVars varDDColVars[];
		JDDVars allDDRowVars;
		JDDVars allDDColVars;
		Vector<String> ddVarNames;
		VarList varList;
		// New (product) model - dds, vars, etc.
		JDDNode newTrans, newStart;
		JDDVars newVarDDRowVars[], newVarDDColVars[];
		JDDVars newAllDDRowVars, newAllDDColVars;
		Vector<String> newDDVarNames;
		VarList newVarList;
		String draVar;
		// DRA stuff
		JDDVars draDDRowVars, draDDColVars;
		// Misc
		int i, j, n;
		boolean before;

		// Get details of old model
		varDDRowVars = model.getVarDDRowVars();
		varDDColVars = model.getVarDDColVars();
		allDDRowVars = model.getAllDDRowVars();
		allDDColVars = model.getAllDDColVars();
		ddVarNames = model.getDDVarNames();
		varList = model.getVarList();

		// Create a (new, unique) name for the variable that will represent DRA states
		draVar = "_dra";
		while (varList.getIndex(draVar) != -1) {
			draVar = "_" + draVar;
		}

		// See how many new dd vars will be needed for DRA
		// and whether there is room to put them before rather than after the existing vars
		// (if DRA only has one state, we add an extra dummy state)
		n = (int) Math.ceil(PrismUtils.log2(dra.size()));
		n = Math.max(n, 1);
		before = true;
		if ((allDDRowVars.getMinVarIndex() - model.getAllDDNondetVars().getMaxVarIndex()) - 1 < 2 * n) {
			before = false;
		}

		// If passed in var lists are null, create new lists
		// (which won't be accessible later in this case)
		draDDRowVars = (draDDRowVarsCopy == null) ? new JDDVars() : draDDRowVarsCopy;
		draDDColVars = (draDDColVarsCopy == null) ? new JDDVars() : draDDColVarsCopy;
		// Create the new dd variables
		newDDVarNames = new Vector<String>();
		newDDVarNames.addAll(ddVarNames);
		j = before ? allDDRowVars.getMinVarIndex() - 2 * n : model.getAllDDColVars().getMaxVarIndex() + 1;
		for (i = 0; i < n; i++) {
			draDDRowVars.addVar(JDD.Var(j++));
			draDDColVars.addVar(JDD.Var(j++));
			if (!before) {
				newDDVarNames.add("");
				newDDVarNames.add("");
			}
			newDDVarNames.set(j - 2, draVar + "." + i);
			newDDVarNames.set(j - 1, draVar + "'." + i);
		}

		// Create/populate new lists
		newVarDDRowVars = new JDDVars[varDDRowVars.length + 1];
		newVarDDColVars = new JDDVars[varDDRowVars.length + 1];
		newVarDDRowVars[before ? 0 : varDDRowVars.length] = draDDRowVars;
		newVarDDColVars[before ? 0 : varDDColVars.length] = draDDColVars;
		for (i = 0; i < varDDRowVars.length; i++) {
			newVarDDRowVars[before ? i + 1 : i] = new JDDVars();
			newVarDDColVars[before ? i + 1 : i] = new JDDVars();
			newVarDDRowVars[before ? i + 1 : i].addVars(varDDRowVars[i]);
			newVarDDColVars[before ? i + 1 : i].addVars(varDDColVars[i]);
		}
		newAllDDRowVars = new JDDVars();
		newAllDDColVars = new JDDVars();
		if (before) {
			newAllDDRowVars.addVars(draDDRowVars);
			newAllDDColVars.addVars(draDDColVars);
			newAllDDRowVars.addVars(allDDRowVars);
			newAllDDColVars.addVars(allDDColVars);
		} else {
			newAllDDRowVars.addVars(allDDRowVars);
			newAllDDColVars.addVars(allDDColVars);
			newAllDDRowVars.addVars(draDDRowVars);
			newAllDDColVars.addVars(draDDColVars);
		}
		newVarList = (VarList) varList.clone();
		// NB: if DRA only has one state, we add an extra dummy state
		Declaration decl = new Declaration(draVar, new DeclarationInt(Expression.Int(0), Expression.Int(Math.max(dra.size() - 1, 1))));
		newVarList.addVar(before ? 0 : varList.getNumVars(), decl, 1, model.getConstantValues());

		// Extra references (because will get derefed when new model is done with)
		allDDRowVars.refAll();
		allDDRowVars.refAll();
		allDDColVars.refAll();
		allDDColVars.refAll();
		for (i = 0; i < model.getNumModules(); i++) {
			model.getModuleDDRowVars(i).refAll();
			model.getModuleDDColVars(i).refAll();
		}
		draDDRowVars.refAll();
		draDDColVars.refAll();
		model.getAllDDSchedVars().refAll();
		model.getAllDDSynchVars().refAll();
		model.getAllDDChoiceVars().refAll();
		model.getAllDDNondetVars().refAll();

		// Build transition matrix for product
		newTrans = buildTransMask(dra, labelDDs, allDDRowVars, allDDColVars, draDDRowVars, draDDColVars);
		JDD.Ref(model.getTrans());
		newTrans = JDD.Apply(JDD.TIMES, model.getTrans(), newTrans);

		// Build set of initial states for product
		// Note, by default, we take product of *all* states of the original model, not just its initial states.
		// Initial states are only used for reachability in this instance.
		// We need to ensure that the product model includes states corresponding to all
		// states of the original model (because we compute probabilities for all of them)
		// but some of these may not be reachable from the initial state of the product model.
		// Optionally (if allInit is false), we don't do this - maybe because we only care about result for the initial state
		// Note that we reset the initial states after reachability, corresponding to just the initial states of the original model.
		// The initial state of the original model can be overridden by passing in 'init'.
		newStart = buildStartMask(dra, labelDDs, draDDRowVars);
		JDD.Ref(allInit ? model.getReach() : init != null ? init : model.getStart());
		newStart = JDD.And(allInit ? model.getReach() : init != null ? init : model.getStart(), newStart);

		// Create a new model model object to store the product model
		NondetModel modelProd = new NondetModel(
		// New transition matrix/start state
				newTrans, newStart,
				// Don't pass in any rewards info
				new JDDNode[0], new JDDNode[0], new String[0],
				// New list of all row/col vars
				newAllDDRowVars, newAllDDColVars,
				// Nondet variables (unchanged)
				model.getAllDDSchedVars(), model.getAllDDSynchVars(), model.getAllDDChoiceVars(), model.getAllDDNondetVars(),
				// New list of var names
				newDDVarNames,
				// Module info (unchanged)
				model.getNumModules(), model.getModuleNames(), model.getModuleDDRowVars(), model.getModuleDDColVars(),
				// New var info
				model.getNumVars() + 1, newVarList, newVarDDRowVars, newVarDDColVars,
				// Constants (no change)
				model.getConstantValues());

		// Copy action info MTBDD across directly
		// If present, just needs filtering to reachable states,
		// which will get done below.
		if (model.getTransActions() != null) {
			JDD.Ref(model.getTransActions());
			modelProd.setTransActions(model.getTransActions());
		}
		// Also need to copy set of action label strings
		modelProd.setSynchs(new Vector<String>(model.getSynchs()));

		// Do reachability/etc. for the new model
		modelProd.doReachability();
		modelProd.filterReachableStates();
		modelProd.findDeadlocks(false);
		if (modelProd.getDeadlockStates().size() > 0) {
			// Assuming original model has no deadlocks, neither should product
			throw new PrismException("Model-DRA product has deadlock states");
		}

		// Reset initial state
		newStart = buildStartMask(dra, labelDDs, draDDRowVars);
		JDD.Ref(init != null ? init : model.getStart());
		newStart = JDD.And(init != null ? init : model.getStart(), newStart);
		modelProd.setStart(newStart);

		//try { prism.exportStatesToFile(modelProd, Prism.EXPORT_PLAIN, new java.io.File("prod.sta")); }
		//catch (java.io.FileNotFoundException e) {}

		// Reference DRA DD vars (if being returned)
		if (draDDRowVarsCopy != null)
			draDDRowVarsCopy.refAll();
		if (draDDColVarsCopy != null)
			draDDColVarsCopy.refAll();

		return modelProd;
	}

	/**
	 * Builds a (referenced) mask BDD representing all possible transitions in a product built with
	 * DRA {@code dra}, i.e. all the transitions ((s,q),(s',q')) where q' = delta(q, label(s')) in the DRA.
	 * So the BDD is over column variables for model states (permuted from those found in the BDDs in
	 * {@code labelDDs}) and row/col variables for the DRA (from {@code draDDRowVars}, {@code draDDColVars}).
	 */
	public JDDNode buildTransMask(DRA<BitSet> dra, Vector<JDDNode> labelDDs, JDDVars allDDRowVars, JDDVars allDDColVars, JDDVars draDDRowVars,
			JDDVars draDDColVars)
	{
		JDDNode draMask, label, exprBDD, transition;
		int i, j, k, numAPs, numStates, numEdges;

		numAPs = dra.getAPList().size();
		draMask = JDD.Constant(0);
		// Iterate through all (states and) transitions of DRA
		numStates = dra.size();
		for (i = 0; i < numStates; i++) {
			numEdges = dra.getNumEdges(i);
			for (j = 0; j < numEdges; j++) {
				// Build a transition label BDD for each edge
				label = JDD.Constant(1);
				for (k = 0; k < numAPs; k++) {
					// Get the expression BDD for AP k (via label "Lk")
					exprBDD = labelDDs.get(Integer.parseInt(dra.getAPList().get(k).substring(1)));
					JDD.Ref(exprBDD);
					if (!dra.getEdgeLabel(i, j).get(k)) {
						exprBDD = JDD.Not(exprBDD);
					}
					label = JDD.And(label, exprBDD);
				}
				// Switch label BDD to col vars
				label = JDD.PermuteVariables(label, allDDRowVars, allDDColVars);
				// Build a BDD for the edge
				transition = JDD.SetMatrixElement(JDD.Constant(0), draDDRowVars, draDDColVars, i, dra.getEdgeDest(i, j), 1);
				// Now get the conjunction of the two
				transition = JDD.And(transition, label);
				// Add edge BDD to the DRA transition mask
				draMask = JDD.Or(draMask, transition);
			}
		}

		return draMask;
	}

	/**
	 * Builds a (referenced) mask BDD representing all possible "start" states for a product built with
	 * DRA {@code dra}, i.e. all the states (s,q) where q = delta(q_init, label(s)) in the DRA.
	 * So the BDD is over row variables for model states (as found in the BDDs in {@code labelDDs})
	 * and row variables for the DRA (from {@code draDDRowVars}).
	 */
	public JDDNode buildStartMask(DRA<BitSet> dra, Vector<JDDNode> labelDDs, JDDVars draDDRowVars)
	{
		JDDNode startMask, label, exprBDD, dest, tmp;
		int i, j, k, numAPs, numEdges;

		numAPs = dra.getAPList().size();
		startMask = JDD.Constant(0);
		// Iterate through all transitions of start state of DRA
		i = dra.getStartState();
		numEdges = dra.getNumEdges(i);
		for (j = 0; j < numEdges; j++) {
			// Build a transition label BDD for each edge
			label = JDD.Constant(1);
			for (k = 0; k < numAPs; k++) {
				// Get the expression BDD for AP k (via label "Lk")
				exprBDD = labelDDs.get(Integer.parseInt(dra.getAPList().get(k).substring(1)));
				JDD.Ref(exprBDD);
				if (!dra.getEdgeLabel(i, j).get(k)) {
					exprBDD = JDD.Not(exprBDD);
				}
				label = JDD.And(label, exprBDD);
			}
			// Build a BDD for the DRA destination state
			dest = JDD.Constant(0);
			dest = JDD.SetVectorElement(dest, draDDRowVars, dra.getEdgeDest(i, j), 1);

			// Now get the conjunction of the two
			tmp = JDD.And(dest, label);

			// Add this destination to our start mask
			startMask = JDD.Or(startMask, tmp);
		}

		return startMask;
	}

	/**
	 * Build a (referenced) BDD over variables {@code draDDRowVars} representing
	 * the set L_i from the i-th pair (L_i,K_i) of the acceptance condition for a DRA.
	 * @param complement If true, build the complement of the set L_i instead
	 */
	public JDDNode buildLStatesForRabinPair(JDDVars draDDRowVars, DRA<BitSet> dra, int i, boolean complement)
	{
		BitSet bitsetLi = dra.getAcceptanceL(i);
		JDDNode statesLi = JDD.Constant(0);
		for (int j = 0; j < dra.size(); j++) {
			if (bitsetLi.get(j) ^ complement) {
				statesLi = JDD.SetVectorElement(statesLi, draDDRowVars, j, 1.0);
			}
		}
		return statesLi;
	}

	/**
	 * Build a (referenced) BDD over variables {@code draDDRowVars} representing
	 * the set K_i from the i-th pair (L_i,K_i) of the acceptance condition for a DRA.
	 */
	public JDDNode buildKStatesForRabinPair(JDDVars draDDRowVars, DRA<BitSet> dra, int i)
	{
		BitSet bitsetKi = dra.getAcceptanceK(i);
		JDDNode statesKi = JDD.Constant(0);
		for (int j = 0; j < dra.size(); j++) {
			if (bitsetKi.get(j)) {
				statesKi = JDD.SetVectorElement(statesKi, draDDRowVars, j, 1.0);
			}
		}
		return statesKi;
	}

	/**
	 * Find the set of states in a model corresponding to the "target" part of a Rabin acceptance condition,
	 * i.e. just the union of the K_i parts of the (L_i,K_i) pairs.  
	 * @param dra The DRA storing the Rabin acceptance condition
	 * @param model The model
	 * @param draDDRowVars BDD row variables for the DRA part of the model
	 * @param draDDColVars BDD column variables for the DRA part of the model
	 * @return
	 */
	public JDDNode findTargetStatesForRabin(DRA<BitSet> dra, Model model, JDDVars draDDRowVars, JDDVars draDDColVars)
	{
		JDDNode acceptingStates = JDD.Constant(0);
		for (int i = 0; i < dra.getNumAcceptancePairs(); i++) {
			JDDNode tmpK = buildKStatesForRabinPair(draDDRowVars, dra, i);
			acceptingStates = JDD.Or(acceptingStates, tmpK);
		}
		JDD.Ref(model.getReach());
		acceptingStates = JDD.And(model.getReach(), acceptingStates);
		return acceptingStates;
	}
	
	/**
	 * Find the set of accepting BSCCs in a model wrt a Rabin acceptance condition.
	 * @param dra The DRA storing the Rabin acceptance condition
	 * @param draDDRowVars BDD row variables for the DRA part of the model
	 * @param draDDColVars BDD column variables for the DRA part of the model
	 * @param model The model
	 * @return A referenced BDD for the union of all states in accepting BSCCs
	 */
	public JDDNode findAcceptingBSCCsForRabin(DRA<BitSet> dra, ProbModel model, JDDVars draDDRowVars, JDDVars draDDColVars) throws PrismException
	{
		JDDNode allAcceptingStates;
		List<JDDNode> vectBSCCs;
		int i;

		allAcceptingStates = JDD.Constant(0);
		// Compute all BSCCs for model
		SCCComputer sccComputer = SCCComputer.createSCCComputer(this, model);
		sccComputer.computeBSCCs();
		vectBSCCs = sccComputer.getBSCCs();
		JDD.Deref(sccComputer.getNotInBSCCs());

		// Build BDDs for !L_i and K_i
		ArrayList<JDDNode> statesL_not = new ArrayList<JDDNode>();
		ArrayList<JDDNode> statesK = new ArrayList<JDDNode>();
		for (i = 0; i < dra.getNumAcceptancePairs(); i++) {
			statesL_not.add(buildLStatesForRabinPair(draDDRowVars, dra, i, true));
			statesK.add(buildKStatesForRabinPair(draDDRowVars, dra, i));
		}
		
		// Go through the BSCCs
		for (JDDNode bscc : vectBSCCs) {
			// Go through the DRA acceptance pairs (L_i, K_i)
			for (i = 0; i < dra.getNumAcceptancePairs(); i++) {
				// Check each BSCC for inclusion in !L_i and intersection with K_i
				if (JDD.IsContainedIn(bscc, statesL_not.get(i)) && JDD.AreInterecting(bscc, statesK.get(i))) {
					// This BSCC is accepting: add and move on onto next one
					JDD.Ref(bscc);
					allAcceptingStates = JDD.Or(allAcceptingStates, bscc);
					break;
				}
			}
			JDD.Deref(bscc);
		}
		
		// Dereference BDDs for !L_i and K_i
		for (i = 0; i < statesK.size(); i++) {
			JDD.Deref(statesL_not.get(i));
			JDD.Deref(statesK.get(i));
		}

		return allAcceptingStates;
	}

	/**
	 * Find the set of states in accepting end components (ECs) in a nondeterministic model wrt a Rabin acceptance condition.
	 * @param dra The DRA storing the Rabin acceptance condition
	 * @param model The model
	 * @param draDDRowVars BDD row variables for the DRA part of the model
	 * @param draDDColVars BDD column variables for the DRA part of the model
	 * @param fairness Consider fairness?
	 * @return A referenced BDD for the union of all states in accepting MECs
	 */
	public JDDNode findAcceptingECStatesForRabin(DRA<BitSet> dra, NondetModel model, JDDVars draDDRowVars, JDDVars draDDColVars, boolean fairness)
			throws PrismException
	{
		JDDNode acceptingStates = null, allAcceptingStates, acceptanceVector_L_not, acceptanceVector_K, candidateStates;
		int i;

		allAcceptingStates = JDD.Constant(0);

		if (dra.getNumAcceptancePairs() > 1) {
			acceptanceVector_L_not = JDD.Constant(0);
			acceptanceVector_K = JDD.Constant(0);
			ArrayList<JDDNode> statesLnot = new ArrayList<JDDNode>();
			ArrayList<JDDNode> statesK = new ArrayList<JDDNode>();

			for (i = 0; i < dra.getNumAcceptancePairs(); i++) {
				JDDNode tmpLnot = buildLStatesForRabinPair(draDDRowVars, dra, i, true);
				JDDNode tmpK = buildKStatesForRabinPair(draDDRowVars, dra, i);
				statesLnot.add(tmpLnot);
				JDD.Ref(tmpLnot);
				acceptanceVector_L_not = JDD.Or(acceptanceVector_L_not, tmpLnot);
				statesK.add(tmpK);
				JDD.Ref(tmpK);
				acceptanceVector_K = JDD.Or(acceptanceVector_K, tmpK);
			}

			JDD.Ref(model.getTrans01());
			candidateStates = JDD.Apply(JDD.TIMES, model.getTrans01(), acceptanceVector_L_not);
			JDD.Ref(acceptanceVector_L_not);
			acceptanceVector_L_not = JDD.PermuteVariables(acceptanceVector_L_not, draDDRowVars, draDDColVars);
			candidateStates = JDD.Apply(JDD.TIMES, candidateStates, acceptanceVector_L_not);
			candidateStates = JDD.ThereExists(candidateStates, model.getAllDDColVars());
			candidateStates = JDD.ThereExists(candidateStates, model.getAllDDNondetVars());
			// find all maximal end components
			List<JDDNode> allecs = findMECStates(model, candidateStates, acceptanceVector_K);
			JDD.Deref(candidateStates);

			for (i = 0; i < dra.getNumAcceptancePairs(); i++) {
				// build the acceptance vectors L_i and K_i
				acceptanceVector_L_not = statesLnot.get(i);
				acceptanceVector_K = statesK.get(i);
				for (JDDNode ec : allecs) {
					// build bdd of accepting states (under L_i) in the product model
					List<JDDNode> ecs;
					JDD.Ref(ec);
					JDD.Ref(acceptanceVector_L_not);
					candidateStates = JDD.And(ec, acceptanceVector_L_not);
					if (candidateStates.equals(ec)) {
						//mainLog.println(" ------------- ec is not modified ------------- ");
						ecs = new Vector<JDDNode>();
						ecs.add(ec);
					} else if (candidateStates.equals(JDD.ZERO)) {
						//mainLog.println(" ------------- ec is ZERO ------------- ");
						continue;
					} else { // recompute maximal end components
						//mainLog.println(" ------------- ec is recomputed ------------- ");
						JDD.Ref(model.getTrans01());
						JDD.Ref(candidateStates);
						JDDNode newcandidateStates = JDD.Apply(JDD.TIMES, model.getTrans01(), candidateStates);
						candidateStates = JDD.PermuteVariables(candidateStates, draDDRowVars, draDDColVars);
						newcandidateStates = JDD.Apply(JDD.TIMES, candidateStates, newcandidateStates);
						newcandidateStates = JDD.ThereExists(newcandidateStates, model.getAllDDColVars());
						candidateStates = JDD.ThereExists(newcandidateStates, model.getAllDDNondetVars());
						ecs = findMECStates(model, candidateStates, acceptanceVector_K);
					}

					// find ECs in acceptingStates that are accepting under K_i
					acceptingStates = JDD.Constant(0);
					for (JDDNode set : ecs) {
						if (JDD.AreInterecting(set, acceptanceVector_K))
							acceptingStates = JDD.Or(acceptingStates, set);
						else
							JDD.Deref(set);
					}
					allAcceptingStates = JDD.Or(allAcceptingStates, acceptingStates);
				}
				JDD.Deref(acceptanceVector_K);
				JDD.Deref(acceptanceVector_L_not);
			}
			for (JDDNode ec : allecs)
				JDD.Deref(ec);
		} else {
			// Go through the DRA acceptance pairs (L_i, K_i) 
			for (i = 0; i < dra.getNumAcceptancePairs(); i++) {
				// Build BDDs for !L_i and K_i
				JDDNode statesLi_not = buildLStatesForRabinPair(draDDRowVars, dra, i, true);
				JDDNode statesK_i = buildKStatesForRabinPair(draDDRowVars, dra, i);
				// Find states in the model for which there are no transitions leaving !L_i
				// (this will allow us to reduce the problem to finding MECs, not ECs)
				// TODO: I don't think this next step is needed,
				// since the ECComputer restricts the model in this way anyway
				JDD.Ref(model.getTrans01());
				JDD.Ref(statesLi_not);
				candidateStates = JDD.Apply(JDD.TIMES, model.getTrans01(), statesLi_not);
				statesLi_not = JDD.PermuteVariables(statesLi_not, draDDRowVars, draDDColVars);
				candidateStates = JDD.Apply(JDD.TIMES, candidateStates, statesLi_not);
				candidateStates = JDD.ThereExists(candidateStates, model.getAllDDColVars());
				candidateStates = JDD.ThereExists(candidateStates, model.getAllDDNondetVars());
				// Normal case (no fairness): find accepting MECs within !L_i 
				if (!fairness) {
					List<JDDNode> ecs = findMECStates(model, candidateStates);
					JDD.Deref(candidateStates);
					acceptingStates = filteredUnion(ecs, statesK_i);
				}
				// For case of fairness...
				else {
					// Compute the backward set of S x !L_i
					JDD.Ref(candidateStates);
					JDDNode tmp = JDD.And(candidateStates, statesK_i);
					JDD.Ref(model.getTrans01());
					JDDNode edges = JDD.ThereExists(model.getTrans01(), model.getAllDDNondetVars());
					JDDNode filterStates = backwardSet(model, tmp, edges);
					// Filter out states that can't reach a state in !L'_i
					candidateStates = JDD.And(candidateStates, filterStates);
					// Find accepting states in S x !L_i
					acceptingStates = findFairECs(model, candidateStates);
				}

				// Add states to our destination BDD
				allAcceptingStates = JDD.Or(allAcceptingStates, acceptingStates);
			}
		}

		return allAcceptingStates;
	}

	public JDDNode findMultiAcceptingStates(DRA<BitSet> dra, NondetModel model, JDDVars draDDRowVars, JDDVars draDDColVars, boolean fairness,
			List<JDDNode> allecs, List<JDDNode> statesH, List<JDDNode> statesL) throws PrismException
	{
		JDDNode acceptingStates = null, allAcceptingStates, candidateStates;
		JDDNode acceptanceVector_H, acceptanceVector_L;
		int i;

		allAcceptingStates = JDD.Constant(0);

		// for each acceptance pair (H_i, L_i) in the DRA, build H'_i = S x H_i
		// and compute the maximal ECs in H'_i
		for (i = 0; i < dra.getNumAcceptancePairs(); i++) {
			// build the acceptance vectors H_i and L_i
			acceptanceVector_H = statesH.get(i);
			acceptanceVector_L = statesL.get(i);
			for (JDDNode ec : allecs) {
				// build bdd of accepting states (under H_i) in the product model
				List<JDDNode> ecs = null;
				JDD.Ref(ec);
				JDD.Ref(acceptanceVector_H);
				candidateStates = JDD.And(ec, acceptanceVector_H);
				if (candidateStates.equals(ec)) {
					//mainLog.println(" ------------- ec is not modified ------------- ");
					ecs = new Vector<JDDNode>();
					//JDDNode ec1 = ec;
					//JDD.Ref(ec);
					ecs.add(ec);
					//JDD.Deref(candidateStates);
				} else if (candidateStates.equals(JDD.ZERO)) {
					//mainLog.println(" ------------- ec is ZERO ------------- ");
					JDD.Deref(candidateStates);
					continue;
				} else { // recompute maximal end components
					//mainLog.println(" ------------- ec is recomputed ------------- ");
					JDD.Ref(model.getTrans01());
					JDD.Ref(candidateStates);
					JDDNode newcandidateStates = JDD.Apply(JDD.TIMES, model.getTrans01(), candidateStates);
					candidateStates = JDD.PermuteVariables(candidateStates, draDDRowVars, draDDColVars);
					newcandidateStates = JDD.Apply(JDD.TIMES, candidateStates, newcandidateStates);
					newcandidateStates = JDD.ThereExists(newcandidateStates, model.getAllDDColVars());
					candidateStates = JDD.ThereExists(newcandidateStates, model.getAllDDNondetVars());
					ecs = findMECStates(model, candidateStates, acceptanceVector_L);
					JDD.Deref(candidateStates);
				}

				//StateListMTBDD vl;
				//int count = 0;
				acceptingStates = JDD.Constant(0);
				for (JDDNode set : ecs) {
					if (JDD.AreInterecting(set, acceptanceVector_L))
						acceptingStates = JDD.Or(acceptingStates, set);
					else
						JDD.Deref(set);
				}
				// Add states to our destination BDD
				allAcceptingStates = JDD.Or(allAcceptingStates, acceptingStates);
			}
			JDD.Deref(acceptanceVector_L);
			JDD.Deref(acceptanceVector_H);
		}

		return allAcceptingStates;
	}

	public void findMultiConflictAcceptingStates(DRA<BitSet>[] dra, NondetModel model, JDDVars[] draDDRowVars, JDDVars[] draDDColVars, List<JDDNode> targetDDs,
			List<List<JDDNode>> allstatesH, List<List<JDDNode>> allstatesL, List<JDDNode> combinations, List<List<Integer>> combinationIDs)
			throws PrismException
	{
		List<queueElement> queue = new ArrayList<queueElement>();
		int sp = 0;

		for (int i = 0; i < dra.length; i++) {
			List<Integer> ids = new ArrayList<Integer>();
			ids.add(i);
			queueElement e = new queueElement(allstatesH.get(i), allstatesL.get(i), targetDDs.get(i), ids, i + 1);
			queue.add(e);
		}

		while (sp < queue.size()) {
			computeCombinations(dra, model, draDDRowVars, draDDColVars, targetDDs, allstatesH, allstatesL, queue, sp);
			sp++;
		}

		// subtract children from targetDD
		for (queueElement e : queue)
			if (e.children != null) {
				JDDNode newtarget = e.targetDD;
				//JDD.Ref(newtarget);
				for (queueElement e1 : e.children) {
					JDD.Ref(e1.targetDD);
					newtarget = JDD.And(newtarget, JDD.Not(e1.targetDD));
				}
				//JDD.Deref(e.targetDD);
				e.targetDD = newtarget;
			}
		targetDDs.clear();
		for (int i = 0; i < dra.length; i++) {
			targetDDs.add(queue.get(i).targetDD);
		}
		for (int i = dra.length; i < queue.size(); i++) {
			combinations.add(queue.get(i).targetDD);
			combinationIDs.add(queue.get(i).draIDs);
		}
	}

	private void computeCombinations(DRA<BitSet>[] dra, NondetModel model, JDDVars[] draDDRowVars, JDDVars[] draDDColVars, List<JDDNode> targetDDs,
			List<List<JDDNode>> allstatesH, List<List<JDDNode>> allstatesL, List<queueElement> queue, int sp) throws PrismException
	{
		queueElement e = queue.get(sp);
		int bound = queue.size();
		//StateListMTBDD vl = null;
		//mainLog.println("  ------------- Processing " + e.draIDs + ": -------------");

		for (int i = e.next; i < dra.length; i++) {
			List<JDDNode> newstatesH = new ArrayList<JDDNode>();
			List<JDDNode> newstatesL = new ArrayList<JDDNode>();
			//if(e.draIDs.size() >= 2 || sp > 0 /*|| queue.size() > 3*/)
			//	break;
			//mainLog.println("             combinations " + e.draIDs + ", " + i + ": ");
			JDDNode allAcceptingStates = JDD.Constant(0);
			// compute conjunction of e and next
			List<JDDNode> nextstatesH = allstatesH.get(i);
			List<JDDNode> nextstatesL = allstatesL.get(i);
			JDD.Ref(e.targetDD);
			JDD.Ref(targetDDs.get(i));
			JDDNode intersection = JDD.And(e.targetDD, targetDDs.get(i));
			for (int j = 0; j < e.statesH.size(); j++) {
				JDD.Ref(intersection);
				JDD.Ref(e.statesH.get(j));
				JDDNode candidateStates = JDD.And(intersection, e.statesH.get(j));
				for (int k = 0; k < nextstatesH.size(); k++) {
					JDD.Ref(candidateStates);
					JDD.Ref(nextstatesH.get(k));
					JDDNode candidateStates1 = JDD.And(candidateStates, nextstatesH.get(k));

					// Find end components in candidateStates1
					JDD.Ref(model.getTrans01());
					JDD.Ref(candidateStates1);
					JDDNode newcandidateStates = JDD.Apply(JDD.TIMES, model.getTrans01(), candidateStates1);
					/*for(int x=0; x<e.draIDs.size(); x++)
						candidateStates1 = JDD.PermuteVariables(candidateStates1, draDDRowVars[e.draIDs.get(x)], draDDColVars[e.draIDs.get(x)]);
					candidateStates1 = JDD.PermuteVariables(candidateStates1, draDDRowVars[i], draDDColVars[i]);*/
					candidateStates1 = JDD.PermuteVariables(candidateStates1, model.getAllDDRowVars(), model.getAllDDColVars());
					newcandidateStates = JDD.Apply(JDD.TIMES, candidateStates1, newcandidateStates);
					newcandidateStates = JDD.ThereExists(newcandidateStates, model.getAllDDColVars());
					candidateStates1 = JDD.ThereExists(newcandidateStates, model.getAllDDNondetVars());
					JDD.Ref(e.statesL.get(j));
					JDD.Ref(nextstatesL.get(k));
					JDDNode acceptanceVector_L = JDD.And(e.statesL.get(j), nextstatesL.get(k));
					List<JDDNode> ecs = null;
					ecs = findMECStates(model, candidateStates1, acceptanceVector_L);
					JDD.Deref(candidateStates1);

					// For each ec, test if it has non-empty intersection with L states
					if (ecs != null) {
						boolean valid = false;
						for (JDDNode set : ecs) {
							if (JDD.AreInterecting(set, acceptanceVector_L)) {
								allAcceptingStates = JDD.Or(allAcceptingStates, set);
								valid = true;
							} else
								JDD.Deref(set);
						}
						if (valid) {
							//mainLog.println("          adding j = " + j + ", k = " + k + " to nextstateH & L ");
							JDD.Ref(e.statesH.get(j));
							JDD.Ref(nextstatesH.get(k));
							JDDNode ttt = JDD.And(e.statesH.get(j), nextstatesH.get(k));
							newstatesH.add(ttt);
							JDD.Ref(acceptanceVector_L);
							newstatesL.add(acceptanceVector_L);
						}
					}
					//if(!valid)
					JDD.Deref(acceptanceVector_L);
				}
				JDD.Deref(candidateStates);
			}
			JDD.Deref(intersection);

			if (!newstatesH.isEmpty() /*&& i+1 < dra.length*/) {
				// generate a new element and put it into queue
				List<Integer> ids = new ArrayList<Integer>(e.draIDs);
				ids.add(i);
				queueElement e1 = new queueElement(newstatesH, newstatesL, allAcceptingStates, ids, i + 1);
				queue.add(e1);
				// add link to e
				e.addChildren(e1);
			} else
				JDD.Deref(allAcceptingStates);

			/*String s = "";
			for(int j=0; j<e.draIDs.size(); j++) 
				s += e.draIDs.*/
			/*vl = new StateListMTBDD(allAcceptingStates, model);
			vl.print(mainLog);
			mainLog.flush();*/
		}

		// add children generated by other elements to e
		for (int i = bound - 1; i > sp; i--) {
			queueElement e2 = queue.get(i);
			if (e2.draIDs.size() <= e.draIDs.size())
				break;
			if (e2.draIDs.containsAll(e.draIDs))
				e.addChildren(e2);
		}

		if (e.draIDs.size() > 1) {
			//mainLog.println("          releaseing statesH & L ");
			for (int i = 0; i < e.statesH.size(); i++) {
				JDD.Deref(e.statesH.get(i));
				JDD.Deref(e.statesL.get(i));
			}
		}
	}

	/**
	 * Returns all end components in candidateStates under fairness assumptions
	 * 
	 * @param candidateStates Set of candidate states S x H_i (dereferenced after calling this function)
	 * @return S referenced BDD with the maximal stable set in c
	 */
	private JDDNode findFairECs(NondetModel model, JDDNode candidateStates)
	{

		JDDNode old = JDD.Constant(0);
		JDDNode current = candidateStates;

		while (!current.equals(old)) {
			JDD.Deref(old);
			JDD.Ref(current);
			old = current;

			JDD.Ref(current);
			JDD.Ref(model.getTrans01());
			// Select transitions starting in current
			JDDNode currTrans01 = JDD.And(model.getTrans01(), current);
			JDD.Ref(current);
			// mask of transitions that end outside current
			JDDNode mask = JDD.Not(JDD.PermuteVariables(current, model.getAllDDRowVars(), model.getAllDDColVars()));
			mask = JDD.And(currTrans01, mask);
			// mask of states that have bad transitions
			mask = JDD.ThereExists(mask, model.getAllDDColVars());
			mask = JDD.ThereExists(mask, model.getAllDDNondetVars());
			// Filter states with bad transitions
			current = JDD.And(current, JDD.Not(mask));
		}
		JDD.Deref(old);
		return current;
	}

	/**
	 * Return the set of states that reach nodes through edges
	 * Refs: result
	 * Derefs: nodes, edges
	 */
	private JDDNode backwardSet(NondetModel model, JDDNode nodes, JDDNode edges)
	{
		JDDNode old = JDD.Constant(0);
		JDDNode current = nodes;
		while (!current.equals(old)) {
			JDD.Deref(old);
			JDD.Ref(current);
			old = current;
			JDD.Ref(current);
			JDD.Ref(edges);
			current = JDD.Or(current, preimage(model, current, edges));
		}
		JDD.Deref(edges);
		JDD.Deref(old);
		return current;
	}

	/**
	 * Return the preimage of nodes in edges Refs: result Derefs: edges, nodes
	 */
	// FIXME: Refactor this out (duplicated in SCCComputers)
	private JDDNode preimage(NondetModel model, JDDNode nodes, JDDNode edges)
	{
		JDDNode tmp;

		// Get transitions that end at nodes
		tmp = JDD.PermuteVariables(nodes, model.getAllDDRowVars(), model.getAllDDColVars());
		tmp = JDD.And(edges, tmp);
		// Get pre(nodes)
		tmp = JDD.ThereExists(tmp, model.getAllDDColVars());
		return tmp;
	}

	/**
	 * Find (states of) all maximal end components (MECs) contained within {@code states}.
	 * @param states BDD of the set of containing states
	 * @return a vector of (referenced) BDDs representing the ECs
	 */
	public List<JDDNode> findMECStates(NondetModel model, JDDNode states) throws PrismException
	{
		ECComputer ecComputer = ECComputer.createECComputer(this, model);
		ecComputer.computeMECStates(states, null);
		return ecComputer.getMECStates();
	}

	/**
	 * Find (states of) all accepting maximal end components (MECs) contained within {@code states},
	 * where acceptance is defined as those which intersect with {@code filter}.
	 * (If {@code filter} is null, the acceptance condition is trivially satisfied.)
	 * @param states BDD of the set of containing states
	 * @param filter BDD for the set of accepting states
	 * @return a vector of (referenced) BDDs representing the ECs
	 */
	public List<JDDNode> findMECStates(NondetModel model, JDDNode states, JDDNode filter) throws PrismException
	{
		ECComputer ecComputer = ECComputer.createECComputer(this, model);
		ecComputer.computeMECStates(states, filter);
		return ecComputer.getMECStates();
	}

	/**
	 * Find all maximal end components (ECs) contained within {@code states}
	 * and whose states have no outgoing transitions.
	 * @param states BDD of the set of containing states
	 * @return a vector of (referenced) BDDs representing the ECs
	 */
	public List<JDDNode> findBottomEndComponents(NondetModel model, JDDNode states) throws PrismException
	{
		List<JDDNode> ecs = findMECStates(model, states);
		List<JDDNode> becs = new Vector<JDDNode>();
		JDDNode out;

		for (JDDNode scc : ecs) {
			JDD.Ref(model.getTrans01());
			JDD.Ref(scc);
			out = JDD.And(model.getTrans01(), scc);
			JDD.Ref(scc);
			out = JDD.And(out, JDD.Not(JDD.PermuteVariables(scc, model.getAllDDRowVars(), model.getAllDDColVars())));
			if (out.equals(JDD.ZERO)) {
				becs.add(scc);
			} else {
				JDD.Deref(scc);
			}
			JDD.Deref(out);
		}
		return becs;
	}

	public JDDNode maxStableSetTrans1(NondetModel model, JDDNode b)
	{

		JDD.Ref(b);
		JDD.Ref(model.getTrans());
		// Select transitions starting in b
		JDDNode currTrans = JDD.Apply(JDD.TIMES, model.getTrans(), b);
		JDDNode mask = JDD.PermuteVariables(b, model.getAllDDRowVars(), model.getAllDDColVars());
		// Select transitions starting in current and ending in current
		mask = JDD.Apply(JDD.TIMES, currTrans, mask);
		// Sum all successor probabilities for each (state, action) tuple
		mask = JDD.SumAbstract(mask, model.getAllDDColVars());
		// If the sum for a (state,action) tuple is 1,
		// there is an action that remains in the stable set with prob 1
		mask = JDD.GreaterThan(mask, 1 - settings.getDouble(PrismSettings.PRISM_SUM_ROUND_OFF));
		// select the transitions starting in these tuples
		JDD.Ref(model.getTrans01());
		JDDNode stableTrans01 = JDD.And(model.getTrans01(), mask);
		// Abstract over actions
		return stableTrans01;
	}

	/**
	 * Return the union of sets from {@code sets} which have a non-empty intersection with {@code filter}.
	 * @param sets List of BDDs representing sets (dereferenced after calling this function)
	 * @param filter BDD of states to test for intersection (dereferenced after calling)
	 * @return Referenced BDD representing the filtered union
	 */
	private JDDNode filteredUnion(List<JDDNode> sets, JDDNode filter)
	{
		JDDNode union = JDD.Constant(0);
		for (JDDNode set : sets) {
			if (JDD.AreInterecting(set, filter))
				union = JDD.Or(union, set);
			else
				JDD.Deref(set);
		}
		JDD.Deref(filter);
		return union;
	}
}

class queueElement
{
	List<JDDNode> statesH;
	List<JDDNode> statesL;
	JDDNode targetDD;
	List<Integer> draIDs;
	int next;
	List<queueElement> children;

	public queueElement(List<JDDNode> statesH, List<JDDNode> statesL, JDDNode targetDD, List<Integer> draIDs, int next)
	{
		this.statesH = statesH;
		this.statesL = statesL;
		this.targetDD = targetDD;
		this.draIDs = draIDs;
		this.next = next;
	}

	public void addChildren(queueElement child)
	{
		if (children == null)
			children = new ArrayList<queueElement>();
		children.add(child);
	}
}
