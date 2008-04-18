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
import jltl2ba.APElement;
import jltl2dstar.*;

/*
 * LTL model checking functionality
 */
public class LTLModelChecker
{
	protected Prism prism;
	protected PrismLog mainLog;
	protected ModelChecker parent;
	// DRA/product stuff
	protected JDDVars draDDRowVars;
	protected JDDVars draDDColVars;

	public LTLModelChecker(Prism prism, ModelChecker parent) throws PrismException
	{
		this.prism = prism;
		mainLog = prism.getMainLog();
		this.parent = parent;
	}

	/* Extract maximal state formula from an LTL path formula, model check them (with
	 * parent model checker) and replace them with ExpressionLabel objects L0, L1, etc.
	 * As an optimisation, model checking that results in true/false for all states is
	 * converted to an actual true/false, and duplicate results are given the same label.
	 */
	public Expression checkMaximalStateFormulas(Model model, Expression expr, Vector<JDDNode> labelDDs)
			throws PrismException
	{
		// A state formula
		if (expr.getType() == Expression.BOOLEAN) {
			// Model check
			JDDNode dd = parent.checkExpressionDD(expr);
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
		else if (expr.getType() == Expression.PATH_BOOLEAN) {
			if (expr instanceof ExpressionBinaryOp) {
				ExpressionBinaryOp exprBinOp = (ExpressionBinaryOp) expr;
				exprBinOp.setOperand1(checkMaximalStateFormulas(model, exprBinOp.getOperand1(), labelDDs));
				exprBinOp.setOperand2(checkMaximalStateFormulas(model, exprBinOp.getOperand2(), labelDDs));
			} else if (expr instanceof ExpressionUnaryOp) {
				ExpressionUnaryOp exprUnOp = (ExpressionUnaryOp) expr;
				exprUnOp.setOperand(checkMaximalStateFormulas(model, exprUnOp.getOperand(), labelDDs));
			} else if (expr instanceof ExpressionTemporal) {
				ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
				if (exprTemp.getOperand1() != null) {
					exprTemp.setOperand1(checkMaximalStateFormulas(model, exprTemp.getOperand1(), labelDDs));
				}
				if (exprTemp.getOperand2() != null) {
					exprTemp.setOperand2(checkMaximalStateFormulas(model, exprTemp.getOperand2(), labelDDs));
				}
			}
		}
		return expr;
	}

	public NondetModel constructProductModel(DRA dra, Model model, Vector<JDDNode> labelDDs) throws PrismException
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
		if ((allDDRowVars.getMinVarIndex() - ((NondetModel) model).getAllDDNondetVars().getMaxVarIndex()) - 1 < 2 * n) {
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
		newVarList.addVar(before ? 0 : varList.getNumVars(), draVar, 0, dra.size() - 1, 0, 1, Expression.INT);

		// Extra references (because will get derefed when new model is done with)
		// TODO: tidy this up, make it corresond to model.clear()
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
		((NondetModel) model).getAllDDSchedVars().refAll();
		((NondetModel) model).getAllDDSynchVars().refAll();
		((NondetModel) model).getAllDDChoiceVars().refAll();
		((NondetModel) model).getAllDDNondetVars().refAll();

		newTrans = buildTransMask(dra, labelDDs, allDDRowVars, allDDColVars, draDDRowVars, draDDColVars);
		JDD.Ref(model.getTrans());
		newTrans = JDD.Apply(JDD.TIMES, model.getTrans(), newTrans);
		
		// Note: ... TODO
		newStart = buildStartMask(dra, labelDDs);
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
				((NondetModel) model).getAllDDSchedVars(), ((NondetModel) model).getAllDDSynchVars(),
				((NondetModel) model).getAllDDChoiceVars(), ((NondetModel) model).getAllDDNondetVars(),
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
	 * Builds a mask BDD for trans (which contains transitions between every
	 * DRA state after adding draRow/Colvars) that includes only the transitions
	 * that exist in the DRA.
	 * @return		a referenced mask BDD over trans
	 */
	public JDDNode buildTransMask(DRA dra, Vector<JDDNode> labelDDs, JDDVars allDDRowVars, JDDVars allDDColVars,
			JDDVars draDDRowVars, JDDVars draDDColVars)
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
				//System.out.println(state.getName() + " to " + edge.getValue().getName() + " through " + edge.getKey().toString(dra.getAPSet(), false));
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
				transition = JDD.SetMatrixElement(JDD.Constant(0), draDDRowVars, draDDColVars, state.getName(), edge
						.getValue().getName(), 1);
				// Now get the conjunction of the two
				transition = JDD.And(transition, label);
				// Add edge BDD to the DRA transition mask
				draMask = JDD.Or(draMask, transition);
			}
		}

		return draMask;
	}

	/** 
	 * Builds a mask BDD for start (which contains start nodes for every
	 * DRA state after adding draRow/ColVars) that includes only the start states
	 * (s, q) such that q = delta(q_in, label(s)) in the DRA.
	 * @return		a referenced mask BDD over start
	 */
	public JDDNode buildStartMask(DRA dra, Vector<JDDNode> labelDDs)
	{
		JDDNode startMask, label, exprBDD, dest, tmp;

		startMask = JDD.Constant(0);
		for (Map.Entry<APElement, DA_State> edge : dra.getStartState().edges().entrySet()) {
			// Build a transition label BDD for each edge
			//System.out.println("To " + edge.getValue().getName() + " through " + edge.getKey().toString(dra.getAPSet(), false));
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
	 * computes maximal accepting SCSSs for each Rabin acceptance pair
	 * 
	 * @returns a referenced BDD of the union of all the accepting SCSSs
	 */
	public JDDNode findAcceptingSCSSs(DRA dra, NondetModel model) throws PrismException
	{

		JDDNode allAcceptingSCSSs = JDD.Constant(0);

		// for each acceptance pair (H_i, L_i) in the DRA, build H'_i = S x H_i
		// and compute the SCSS maximals in H'_i
		for (int i = 0; i < dra.acceptance().size(); i++) {

			// build the acceptance vectors H_i and L_i
			JDDNode acceptanceVector_H = JDD.Constant(0);
			JDDNode acceptanceVector_L = JDD.Constant(0);
			for (int j = 0; j < dra.size(); j++) {
				/* [dA97] uses Rabin acceptance pairs (H_i, L_i) such that
				 * H_i contains Inf(ρ)
				 * The intersection of Inf(ρ) and L_i is non-empty
				 *
				 * OTOH ltl2dstar (and our port to java) uses pairs (L_i, U_i) such that
				 * The intersection of U_i and Inf(ρ) is empty (H_i = S - U_i)
				 * The intersection of L_i and Inf(ρ) is non-empty
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
			JDDNode acceptingStates = JDD.Apply(JDD.TIMES, model.getTrans01(), acceptanceVector_H);
			acceptanceVector_H = JDD.PermuteVariables(acceptanceVector_H, draDDRowVars, draDDColVars);
			acceptingStates = JDD.Apply(JDD.TIMES, acceptingStates, acceptanceVector_H);
			acceptingStates = JDD.ThereExists(acceptingStates, model.getAllDDColVars());
			acceptingStates = JDD.ThereExists(acceptingStates, model.getAllDDNondetVars());

			// find SCSSs in acceptingStates that are accepting under L_i
			JDDNode acceptingSCSSs = filteredUnion(findMaximalSCSSs(model, acceptingStates), acceptanceVector_L);

			// Add SCSSs to our destination bdd
			allAcceptingSCSSs = JDD.Or(allAcceptingSCSSs, acceptingSCSSs);
		}
		return allAcceptingSCSSs;
	}

	/**
	 * Computes maximal accepting SCSSs in states
	 * @param states	BDD of a set of states (dereferenced after calling this function)
	 * @return			a vector of referenced BDDs containing all the maximal SCSSs in states
	 */
	private Vector<JDDNode> findMaximalSCSSs(NondetModel model, JDDNode states) throws PrismException
	{

		boolean initialCandidate = true;
		Stack<JDDNode> candidates = new Stack<JDDNode>();
		candidates.push(states);
		Vector<JDDNode> scsss = new Vector<JDDNode>();

		while (!candidates.isEmpty()) {
			JDDNode candidate = candidates.pop();

			// Compute the stable set
			JDD.Ref(candidate);
			JDDNode stableSet = findMaxStableSet(model, candidate);

			if (!initialCandidate) {
				// candidate is an SCC, check if it's stable
				if (stableSet.equals(candidate)) {
					scsss.add(candidate);
					JDD.Deref(stableSet);
					continue;
				}
			} else
				initialCandidate = false;
			JDD.Deref(candidate);

			// Filter bad transitions
			JDD.Ref(stableSet);
			JDDNode stableSetTrans = maxStableSetTrans(model, stableSet);

			// now find the maximal SCCs in (stableSet, stableSetTrans)
			Vector<JDDNode> sccs;
			SCCComputer sccComputer;
			switch (prism.getSCCMethod()) {
			case Prism.LOCKSTEP:
				sccComputer = new SCCComputerLockstep(prism, stableSet, stableSetTrans, model.getAllDDRowVars(), model
						.getAllDDColVars());
				break;
			case Prism.SCCFIND:
				sccComputer = new SCCComputerSCCFind(prism, stableSet, stableSetTrans, model.getAllDDRowVars(), model
						.getAllDDColVars());
				break;
			case Prism.XIEBEEREL:
				sccComputer = new SCCComputerXB(prism, stableSet, stableSetTrans, model.getAllDDRowVars(), model
						.getAllDDColVars());
				break;
			default:
				sccComputer = new SCCComputerLockstep(prism, stableSet, stableSetTrans, model.getAllDDRowVars(), model
						.getAllDDColVars());
			}
			sccComputer.computeBSCCs();
			JDD.Deref(stableSet);
			JDD.Deref(stableSetTrans);
			sccs = sccComputer.getVectBSCCs();
			JDD.Deref(sccComputer.getNotInBSCCs());
			candidates.addAll(sccs);
		}
		return scsss;
	}

	/** 
	 * Returns the maximal stable set in c
	 * @param c		a set of nodes where we want to find a stable set 
	 * 				(dereferenced after calling this function)
	 * @return		a referenced BDD with the maximal stable set in c
	 */
	private JDDNode findMaxStableSet(NondetModel model, JDDNode c)
	{
		JDDNode old;
		JDDNode current;
		JDDNode mask;

		JDD.Ref(c);
		current = c;

		do {
			/* if (verbose) {
			 mainLog.println("Stable set pass " + i + ":");
			 } */
			old = current;
			// states that aren't in B (column vector)
			JDD.Ref(current);
			mask = JDD.Not(JDD.PermuteVariables(current, model.getAllDDRowVars(), model.getAllDDColVars()));
			JDD.Ref(model.getTrans01());
			// transitions that end outside of B
			mask = JDD.Apply(JDD.TIMES, model.getTrans01(), mask);
			// mask of transitions that end in B
			mask = JDD.Not(mask);
			// mask of states that always transition to other states in B through a certain action
			mask = JDD.ForAll(mask, model.getAllDDColVars());
			// mask of states that have an action that always transitions to other states in B
			mask = JDD.ThereExists(mask, model.getAllDDNondetVars());
			// states in B that have an action that always transitions to other states in B
			current = JDD.Apply(JDD.TIMES, current, mask);
			/* if (verbose) {
			 mainLog.println("Stable set search pass " + i);
			 JDD.PrintVector(current, allDDRowVars);
			 mainLog.println();
			 i++;
			 } */
		} while (!current.equals(old));
		JDD.Deref(c);
		return current;
	}

	/**
	 * Returns the transition relation of a stable set
	 * @param b		BDD of a stable set (dereferenced after calling this function)
	 * @return		referenced BDD of the transition relation restricted to the stable set
	 */
	private JDDNode maxStableSetTrans(NondetModel model, JDDNode b)
	{

		JDDNode ssTrans;
		JDDNode mask;

		// Restrict threshold to transitions that start in the stable set
		JDD.Ref(model.getTrans01());
		JDD.Ref(b);
		ssTrans = JDD.Apply(JDD.TIMES, model.getTrans01(), b);
		// states that aren't in B (column vector)
		mask = JDD.Not(JDD.PermuteVariables(b, model.getAllDDRowVars(), model.getAllDDColVars()));
		JDD.Ref(ssTrans);
		// transitions that land outside of B
		mask = JDD.Apply(JDD.TIMES, ssTrans, mask);
		// mask of transitions that land in B
		mask = JDD.Not(mask);
		// mask of states that always transition to other states in B through a certain action
		mask = JDD.ForAll(mask, model.getAllDDColVars());
		// transitions that always land in B through a certain action
		ssTrans = JDD.Apply(JDD.TIMES, ssTrans, mask);
		// valid transitions in the stable set regardless of action 
		ssTrans = JDD.ThereExists(ssTrans, model.getAllDDNondetVars());

		return ssTrans;
	}

	/**
	 * Returns the union of each set in the vector that has nonempty intersection
	 * with the filter BDD.
	 * @param sets		Vector with sets which are dereferenced after calling this function
	 * @param filter	filter BDD against which each set is checked for nonempty intersection
	 * 					also dereferenced after calling this function
	 * @return		Referenced BDD with the filtered union
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
