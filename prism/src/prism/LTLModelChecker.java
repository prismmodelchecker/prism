//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Carlos S. Bederián (Universidad Nacional de Córdoba)
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

import java.util.*;

import jdd.*;
import parser.*;
import parser.ast.*;
import parser.type.*;
import jltl2ba.APElement;
import jltl2dstar.*;

/*
 * LTL model checking functionality
 */
public class LTLModelChecker
{
	protected Prism prism;
	protected PrismLog mainLog;
	// DRA/product stuff
	protected JDDVars draDDRowVars;
	protected JDDVars draDDColVars;

	public LTLModelChecker(Prism prism) throws PrismException
	{
		this.prism = prism;
		mainLog = prism.getMainLog();
	}

	/**
	 * Extract maximal state formula from an LTL path formula, model check them (with passed in model checker) and
	 * replace them with ExpressionLabel objects L0, L1, etc. Expression passed in is modified directly, but the result
	 * is also returned. As an optimisation, model checking that results in true/false for all states is converted to an
	 * actual true/false, and duplicate results are given the same label.
	 */
	public Expression checkMaximalStateFormulas(ModelChecker mc, Model model, Expression expr, Vector<JDDNode> labelDDs)
			throws PrismException
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
	 * Construct product of DRA and DTMC/CTMC.
	 */
	public ProbModel constructProductMC(DRA dra,ProbModel model, Vector<JDDNode> labelDDs) throws PrismException
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
		n = (int) Math.ceil(PrismUtils.log2(dra.size()));
		before = true;
		if (allDDRowVars.getMinVarIndex() - 1 < 2 * n) {
			before = false;
		}

		// Create the new dd variables
		draDDRowVars = new JDDVars();
		draDDColVars = new JDDVars();
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
		Declaration decl = new Declaration(draVar, new DeclarationInt(Expression.Int(0), Expression.Int(dra.size() - 1)));
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
		// Note that we take product of *all* states of the original MDP, not just its initial states.
		// Initial states are only used for reachability in this instance.
		// We need to ensure that the product model includes states corresponding to all
		// states of the original MDP (because we compute probabilities for all of them)
		// but some of these may not be reachable from the initial state of the product model.
		newStart = buildStartMask(dra, labelDDs, draDDRowVars);
		JDD.Ref(model.getReach());
		newStart = JDD.And(model.getReach(), newStart);

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
		modelProd.doReachability(prism.getExtraReachInfo());
		modelProd.filterReachableStates();
		modelProd.findDeadlocks();
		if (modelProd.getDeadlockStates().size() > 0) {
			throw new PrismException("Model-DRA product has deadlock states");
		}

		return modelProd;
	}

	/**
	 * Construct product of DRA and MDP.
	 */
	public NondetModel constructProductMDP(DRA dra, NondetModel model, Vector<JDDNode> labelDDs) throws PrismException
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
		n = (int) Math.ceil(PrismUtils.log2(dra.size()));
		before = true;
		if ((allDDRowVars.getMinVarIndex() - model.getAllDDNondetVars().getMaxVarIndex()) - 1 < 2 * n) {
			before = false;
		}

		// Create the new dd variables
		draDDRowVars = new JDDVars();
		draDDColVars = new JDDVars();
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
		Declaration decl = new Declaration(draVar, new DeclarationInt(Expression.Int(0), Expression.Int(dra.size() - 1)));
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
		// Note that we take product of *all* states of the original MDP, not just its initial states.
		// Initial states are only used for reachability in this instance.
		// We need to ensure that the product model includes states corresponding to all
		// states of the original MDP (because we compute probabilities for all of them)
		// but some of these may not be reachable from the initial state of the product model.
		newStart = buildStartMask(dra, labelDDs, draDDRowVars);
		JDD.Ref(model.getReach());
		newStart = JDD.And(model.getReach(), newStart);

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

		// Do reachability/etc. for the new model
		modelProd.doReachability(prism.getExtraReachInfo());
		modelProd.filterReachableStates();
		modelProd.findDeadlocks();
		if (modelProd.getDeadlockStates().size() > 0) {
			throw new PrismException("Model-DRA product has deadlock states");
		}

		return modelProd;
	}

	/**
	 * Builds a mask BDD for trans (which contains transitions between every DRA state after adding draRow/ColVars) that
	 * includes only the transitions that exist in the DRA.
	 * 
	 * @return a referenced mask BDD over trans
	 */
	public JDDNode buildTransMask(DRA dra, Vector<JDDNode> labelDDs, JDDVars allDDRowVars, JDDVars allDDColVars, JDDVars draDDRowVars,
			JDDVars draDDColVars)
	{
		Iterator<DA_State> it;
		DA_State state;
		JDDNode draMask, label, exprBDD, transition;
		int i, n;

		draMask = JDD.Constant(0);
		// Iterate through all (states and) transitions of DRA
		for (it = dra.iterator(); it.hasNext();) {
			state = it.next();
			for (Map.Entry<APElement, DA_State> edge : state.edges().entrySet()) {
				// Build a transition label BDD for each edge
				// System.out.println(state.getName() + " to " + edge.getValue().getName() + " through " +
				// edge.getKey().toString(dra.getAPSet(), false));
				label = JDD.Constant(1);
				n = dra.getAPSize();
				for (i = 0; i < n; i++) {
					// Get the expression BDD for label i
					exprBDD = labelDDs.get(Integer.parseInt(dra.getAPSet().getAP(i).substring(1)));
					JDD.Ref(exprBDD);
					if (!edge.getKey().get(i)) {
						exprBDD = JDD.Not(exprBDD);
					}
					label = JDD.And(label, exprBDD);
				}
				// Switch label BDD to col vars
				label = JDD.PermuteVariables(label, allDDRowVars, allDDColVars);
				// Build a BDD for the edge
				transition = JDD.SetMatrixElement(JDD.Constant(0), draDDRowVars, draDDColVars, state.getName(), edge.getValue().getName(),
						1);
				// Now get the conjunction of the two
				transition = JDD.And(transition, label);
				// Add edge BDD to the DRA transition mask
				draMask = JDD.Or(draMask, transition);
			}
		}

		return draMask;
	}

	/**
	 * Builds a mask BDD for start (which contains start nodes for every DRA state after adding draRowVars) that
	 * includes only the start states (s, q) such that q = delta(q_in, label(s)) in the DRA.
	 * 
	 * @return a referenced mask BDD over start
	 */
	public JDDNode buildStartMask(DRA dra, Vector<JDDNode> labelDDs, JDDVars draDDRowVars)
	{
		JDDNode startMask, label, exprBDD, dest, tmp;

		startMask = JDD.Constant(0);
		for (Map.Entry<APElement, DA_State> edge : dra.getStartState().edges().entrySet()) {
			// Build a transition label BDD for each edge
			// System.out.println("To " + edge.getValue().getName() + " through " +
			// edge.getKey().toString(dra.getAPSet(), false));
			label = JDD.Constant(1);
			for (int i = 0; i < dra.getAPSize(); i++) {
				exprBDD = labelDDs.get(Integer.parseInt(dra.getAPSet().getAP(i).substring(1)));
				JDD.Ref(exprBDD);
				if (!edge.getKey().get(i)) {
					exprBDD = JDD.Not(exprBDD);
				}
				label = JDD.Apply(JDD.TIMES, label, exprBDD);
			}
			// Build a BDD for the DRA destination state
			dest = JDD.Constant(0);
			dest = JDD.SetVectorElement(dest, draDDRowVars, edge.getValue().getName(), 1);

			// Now get the conjunction of the two
			tmp = JDD.And(dest, label);

			// Add this destination to our start mask
			startMask = JDD.Or(startMask, tmp);
		}

		return startMask;
	}

	/**
	 * Computes sets of accepting states in a DTMC/CTMC for each Rabin acceptance pair
	 * 
	 * @returns a referenced BDD of the union of all the accepting sets
	 */
	public JDDNode findAcceptingBSCCs(DRA dra, ProbModel model) throws PrismException
	{
		SCCComputer sccComputer ;
		JDDNode acceptingStates, allAcceptingStates;
		Vector<JDDNode> vectBSCCs, newVectBSCCs;
		JDDNode tmp, tmp2;
		int i, j, n;
		
		// Compute BSCCs for model
		sccComputer = prism.getSCCComputer(model);
		sccComputer.computeBSCCs();
		vectBSCCs = sccComputer.getVectBSCCs();
		JDD.Deref(sccComputer.getNotInBSCCs());
		
		allAcceptingStates = JDD.Constant(0);

		// for each acceptance pair (H_i, L_i) in the DRA, build H'_i = S x H_i
		// and compute the BSCCs in H'_i
		for (i = 0; i < dra.acceptance().size(); i++) {

			// build the acceptance vectors H_i and L_i
			JDDNode acceptanceVector_H = JDD.Constant(0);
			JDDNode acceptanceVector_L = JDD.Constant(0);
			for (j = 0; j < dra.size(); j++) {
				/*
				 * [dA97] uses Rabin acceptance pairs (H_i, L_i) such that H_i contains Inf(ρ) The intersection of
				 * Inf(ρ) and L_i is non-empty
				 * 
				 * OTOH ltl2dstar (and our port to java) uses pairs (L_i, U_i) such that The intersection of U_i and
				 * Inf(ρ) is empty (H_i = S - U_i) The intersection of L_i and Inf(ρ) is non-empty
				 */
				if (!dra.acceptance().isStateInAcceptance_U(i, j)) {
					acceptanceVector_H = JDD.SetVectorElement(acceptanceVector_H, draDDRowVars, j, 1.0);
				}
				if (dra.acceptance().isStateInAcceptance_L(i, j)) {
					acceptanceVector_L = JDD.SetVectorElement(acceptanceVector_L, draDDRowVars, j, 1.0);
				}
			}
			
			// Check each BSCC for inclusion in H_i states
			// i.e. restrict each BSCC to H_i states and test if unchanged
			n = vectBSCCs.size();
			newVectBSCCs = new Vector<JDDNode>();
			for (j = 0; j < n; j++) {
				tmp = vectBSCCs.get(j);
				JDD.Ref(tmp);
				JDD.Ref(tmp);
				JDD.Ref(acceptanceVector_H);
				tmp2 = JDD.And(tmp, acceptanceVector_H);
				if (tmp.equals(tmp2)) {
					newVectBSCCs.add(tmp);
				} else {
					JDD.Deref(tmp);
				}
				JDD.Deref(tmp2);
			}
			JDD.Deref(acceptanceVector_H);
			
			// Compute union of BSCCs which overlap with acceptanceVector_L
			acceptingStates = filteredUnion(newVectBSCCs, acceptanceVector_L);
			
			// Add states to our destination BDD
			allAcceptingStates = JDD.Or(allAcceptingStates, acceptingStates);
		}
		
		// Deref BSCCs 
		n = vectBSCCs.size();
		for (j = 0; j < n; j++) {
			JDD.Deref(vectBSCCs.get(j));
		}
		
		return allAcceptingStates;
	}

	/**
	 * Computes sets of accepting states in an MDP for each Rabin acceptance pair
	 * 
	 * @returns a referenced BDD of the union of all the accepting sets
	 */
	public JDDNode findAcceptingStates(DRA dra, NondetModel model, boolean fairness) throws PrismException
	{
		JDDNode acceptingStates, allAcceptingStates;
		int i, j;
		
		allAcceptingStates = JDD.Constant(0);

		// for each acceptance pair (H_i, L_i) in the DRA, build H'_i = S x H_i
		// and compute the maximal ECs in H'_i
		for (i = 0; i < dra.acceptance().size(); i++) {

			// build the acceptance vectors H_i and L_i
			JDDNode acceptanceVector_H = JDD.Constant(0);
			JDDNode acceptanceVector_L = JDD.Constant(0);
			for (j = 0; j < dra.size(); j++) {
				/*
				 * [dA97] uses Rabin acceptance pairs (H_i, L_i) such that H_i contains Inf(ρ) The intersection of
				 * Inf(ρ) and L_i is non-empty
				 * 
				 * OTOH ltl2dstar (and our port to java) uses pairs (L_i, U_i) such that The intersection of U_i and
				 * Inf(ρ) is empty (H_i = S - U_i) The intersection of L_i and Inf(ρ) is non-empty
				 */
				if (!dra.acceptance().isStateInAcceptance_U(i, j)) {
					acceptanceVector_H = JDD.SetVectorElement(acceptanceVector_H, draDDRowVars, j, 1.0);
				}
				if (dra.acceptance().isStateInAcceptance_L(i, j)) {
					acceptanceVector_L = JDD.SetVectorElement(acceptanceVector_L, draDDRowVars, j, 1.0);
				}
			}

			// build bdd of accepting states (under H_i) in the product model
			JDD.Ref(model.getTrans01());
			JDD.Ref(acceptanceVector_H);
			JDDNode candidateStates = JDD.Apply(JDD.TIMES, model.getTrans01(), acceptanceVector_H);
			acceptanceVector_H = JDD.PermuteVariables(acceptanceVector_H, draDDRowVars, draDDColVars);
			candidateStates = JDD.Apply(JDD.TIMES, candidateStates, acceptanceVector_H);
			candidateStates = JDD.ThereExists(candidateStates, model.getAllDDColVars());
			candidateStates = JDD.ThereExists(candidateStates, model.getAllDDNondetVars());

			if (fairness) {
				// compute the backward set of S x L_i
				JDD.Ref(candidateStates);
				JDDNode tmp = JDD.And(candidateStates, acceptanceVector_L);
				JDD.Ref(model.getTrans01());
				JDDNode edges = JDD.ThereExists(model.getTrans01(), model.getAllDDNondetVars());
				JDDNode filterStates = backwardSet(model, tmp, edges);

				// Filter out states that can't reach a state in L'_i
				candidateStates = JDD.And(candidateStates, filterStates);

				// Find accepting states in S x H_i
				acceptingStates = findFairECs(model, candidateStates);
			} else {
				// find ECs in acceptingStates that are accepting under L_i
				acceptingStates = filteredUnion(findEndComponents(model, candidateStates), acceptanceVector_L);
			}

			// Add states to our destination BDD
			allAcceptingStates = JDD.Or(allAcceptingStates, acceptingStates);
		}
		
		return allAcceptingStates;
	}

	/**
	 * Returns all end components in candidateStates under fairness assumptions
	 * 
	 * @param candidateStates
	 *            set of candidate states S x H_i (dereferenced after calling this function)
	 * @return a referenced BDD with the maximal stable set in c
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
	 * Return the set of states that reach nodes through edges Refs: result Derefs: nodes, edges
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
	 * Computes maximal accepting end components in states
	 * 
	 * @param states
	 *            BDD of a set of states (dereferenced after calling this function)
	 * @return a vector of referenced BDDs containing all the ECs in states
	 */
	private Vector<JDDNode> findEndComponents(NondetModel model, JDDNode states) throws PrismException
	{

		boolean initialCandidate = true;
		Stack<JDDNode> candidates = new Stack<JDDNode>();
		candidates.push(states);
		Vector<JDDNode> ecs = new Vector<JDDNode>();
		SCCComputer sccComputer;

		while (!candidates.isEmpty()) {
			JDDNode candidate = candidates.pop();

			// Compute the stable set
			JDD.Ref(candidate);
			JDDNode stableSet = findMaximalStableSet(model, candidate);

			// Drop empty sets
			if (stableSet.equals(JDD.ZERO)) {
				JDD.Deref(stableSet);
				JDD.Deref(candidate);
				continue;
			}

			if (!initialCandidate) {
				// candidate is an SCC, check if it's stable
				if (stableSet.equals(candidate)) {
					ecs.add(candidate);
					JDD.Deref(stableSet);
					continue;
				}
			} else {
				initialCandidate = false;
			}
			JDD.Deref(candidate);

			// Filter bad transitions
			JDD.Ref(stableSet);
			JDDNode stableSetTrans = maxStableSetTrans(model, stableSet);

			// now find the maximal SCCs in (stableSet, stableSetTrans)
			Vector<JDDNode> sccs;
			sccComputer = prism.getSCCComputer(stableSet, stableSetTrans, model.getAllDDRowVars(), model.getAllDDColVars());
			sccComputer.computeSCCs();
			JDD.Deref(stableSet);
			JDD.Deref(stableSetTrans);
			sccs = sccComputer.getVectSCCs();
			JDD.Deref(sccComputer.getNotInSCCs());
			candidates.addAll(sccs);
		}
		return ecs;
	}

	/**
	 * Returns a stable set of states contained in candidateStates
	 * 
	 * @param candidateStates
	 *            set of candidate states S x H_i (dereferenced after calling this function)
	 * @return a referenced BDD with the maximal stable set in c
	 */
	private JDDNode findMaximalStableSet(NondetModel model, JDDNode candidateStates)
	{

		JDDNode old = JDD.Constant(0);
		JDDNode current = candidateStates;

		while (!current.equals(old)) {
			JDD.Deref(old);
			JDD.Ref(current);
			old = current;

			JDD.Ref(current);
			JDD.Ref(model.getTrans());
			// Select transitions starting in current
			JDDNode currTrans = JDD.Apply(JDD.TIMES, model.getTrans(), current);
			// Select transitions starting in current and ending in current
			JDDNode tmp = JDD.PermuteVariables(current, model.getAllDDRowVars(), model.getAllDDColVars());
			tmp = JDD.Apply(JDD.TIMES, currTrans, tmp);
			// Sum all successor probabilities for each (state, action) tuple
			tmp = JDD.SumAbstract(tmp, model.getAllDDColVars());
			// If the sum for a (state,action) tuple is 1,
			// there is an action that remains in the stable set with prob 1
			tmp = JDD.GreaterThan(tmp, 1 - prism.getSumRoundOff());
			// Without fairness, we just need one action per state
			current = JDD.ThereExists(tmp, model.getAllDDNondetVars());
		}
		JDD.Deref(old);
		return current;
	}

	/**
	 * Returns the transition relation of a stable set
	 * 
	 * @param b
	 *            BDD of a stable set (dereferenced after calling this function)
	 * @return referenced BDD of the transition relation restricted to the stable set
	 */
	private JDDNode maxStableSetTrans(NondetModel model, JDDNode b)
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
		mask = JDD.GreaterThan(mask, 1 - prism.getSumRoundOff());
		// select the transitions starting in these tuples
		JDD.Ref(model.getTrans01());
		JDDNode stableTrans01 = JDD.And(model.getTrans01(), mask);
		// Abstract over actions
		return JDD.ThereExists(stableTrans01, model.getAllDDNondetVars());
	}

	/**
	 * Returns the union of each set in the vector that has nonempty intersection with the filter BDD.
	 * 
	 * @param sets
	 *            Vector with sets which are dereferenced after calling this function
	 * @param filter
	 *            filter BDD against which each set is checked for nonempty intersection also dereferenced after calling
	 *            this function
	 * @return Referenced BDD with the filtered union
	 */
	private JDDNode filteredUnion(Vector<JDDNode> sets, JDDNode filter)
	{
		JDDNode union = JDD.Constant(0);
		for (JDDNode set : sets) {
			JDD.Ref(filter);
			union = JDD.Or(union, JDD.And(set, filter));
		}
		JDD.Deref(filter);
		return union;
	}
}
