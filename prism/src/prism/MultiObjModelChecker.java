//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
//	* Hongyang Qu <hongyang.qu@cs.ox.ac.uk> (University of Oxford)
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

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Vector;

import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import mtbdd.PrismMTBDD;
import parser.ast.Expression;
import parser.ast.RelOp;
import sparse.NDSparseMatrix;
import sparse.PrismSparse;
import acceptance.AcceptanceRabin;
import automata.DA;
import automata.LTL2DA;
import dv.DoubleVector;

/**
 * Multi-objective model checking functionality
 */
public class MultiObjModelChecker extends PrismComponent
{
	protected Prism prism;
	protected boolean verbose;

	/**
	 * Create a new MultiObjModelChecker, inherit basic state from parent (unless null).
	 */
	public MultiObjModelChecker(PrismComponent parent, Prism prism) throws PrismException
	{
		super(parent);
		this.prism = prism;
		this.verbose = settings.getBoolean(PrismSettings.PRISM_VERBOSE);
	}

	//TODO: dra's element is changed here, not neat.
	protected NondetModel constructDRAandProductMulti(NondetModel model, LTLModelChecker mcLtl, ModelChecker modelChecker, Expression ltl, int i,
			DA<BitSet, AcceptanceRabin> dra[], Operator operator, Expression pathFormula, JDDVars draDDRowVars, JDDVars draDDColVars, JDDNode ddStateIndex)
			throws PrismException
	{

		// TODO (JK): Adapt to support simple path formulas with bounds via DRA construction

		// Model check maximal state formulas
		Vector<JDDNode> labelDDs = new Vector<JDDNode>();
		ltl = mcLtl.checkMaximalStateFormulas(modelChecker, model, pathFormula.deepCopy(), labelDDs);

		// Convert LTL formula to deterministic Rabin automaton (DRA)
		// For min probabilities, need to negate the formula
		// (add parentheses to allow re-parsing if required)
		if (Operator.isMinOrLe(operator))
			ltl = Expression.Not(Expression.Parenth(ltl));
		mainLog.println("\nBuilding deterministic Rabin automaton (for " + ltl + ")...");
		long l = System.currentTimeMillis();
		LTL2DA ltl2da = new LTL2DA(this);
		dra[i] = ltl2da.convertLTLFormulaToDRA(ltl, modelChecker.getConstantValues());
		mainLog.print("DRA has " + dra[i].size() + " states, " + dra[i].getAcceptance().getSizeStatistics() + ".");
		l = System.currentTimeMillis() - l;
		mainLog.println("Time for Rabin translation: " + l / 1000.0 + " seconds.");
		// If required, export DRA 
		if (settings.getExportPropAut()) {
			String exportPropAutFilename = PrismUtils.addCounterSuffixToFilename(settings.getExportPropAutFilename(), i + 1);
			mainLog.println("Exporting DRA to file \"" + exportPropAutFilename + "\"...");
			PrintStream out = PrismUtils.newPrintStream(exportPropAutFilename);
			dra[i].print(out, settings.getExportPropAutType());
			out.close();
		}

		// Build product of MDP and automaton
		mainLog.println("\nConstructing MDP-DRA product...");

		NondetModel modelNew = mcLtl.constructProductMDP(dra[i], model, labelDDs, draDDRowVars, draDDColVars, (i == 0 ? ddStateIndex : model.getStart()).copy());

		modelNew.printTransInfo(mainLog, prism.getExtraDDInfo());
		// Deref label BDDs
		for (int j = 0; j < labelDDs.size(); j++) {
			JDD.Deref(labelDDs.get(j));
		}
		return modelNew;
	}

	/**
	 * 
	 * @param modelProduct
	 * @param rewardsIndex
	 * @param opsAndBounds
	 * @return True if some transitions were removed
	 */
	protected boolean removeNonZeroRewardTrans(NondetModel modelProduct, List<JDDNode> rewardsIndex, OpsAndBoundsList opsAndBounds)
	{
		boolean transchanged = false;
		for (int i = 0; i < rewardsIndex.size(); i++)
			if (opsAndBounds.getRewardOperator(i) == Operator.R_MIN || opsAndBounds.getRewardOperator(i) == Operator.R_LE) {
				// Get non-zero reward actions
				JDD.Ref(rewardsIndex.get(i));
				JDDNode actions = JDD.GreaterThan(rewardsIndex.get(i), 0.0);
				if (!actions.equals(JDD.ZERO)) {
					//mainLog.println("Removing non-zero reward actions in reward #" + i);
					JDD.Ref(actions);
					if (!transchanged)
						JDD.Ref(modelProduct.getTrans());
					JDDNode tmp = JDD.ITE(actions, JDD.Constant(0), modelProduct.getTrans());
					modelProduct.trans = tmp;
					if (!transchanged)
						JDD.Ref(modelProduct.getTrans01());
					tmp = JDD.ITE(actions, JDD.Constant(0), modelProduct.getTrans01());
					modelProduct.trans01 = tmp;
					transchanged = true;
				}
			}
		return transchanged;
	}

	protected List<JDDNode> computeAllEcs(NondetModel modelProduct, LTLModelChecker mcLtl, ArrayList<ArrayList<JDDNode>> allstatesH,
			ArrayList<ArrayList<JDDNode>> allstatesL, JDDNode acceptanceVector_H, JDDNode acceptanceVector_L, JDDVars draDDRowVars[], JDDVars draDDColVars[],
			OpsAndBoundsList opsAndBounds, int numTargets) throws PrismException
	{
		//use acceptanceVector_H and acceptanceVector_L to speed up scc computation
		JDD.Ref(acceptanceVector_H);
		JDD.Ref(modelProduct.getTrans01());
		JDDNode candidateStates = JDD.Apply(JDD.TIMES, modelProduct.getTrans01(), acceptanceVector_H);
		for (int i = 0; i < numTargets; i++)
			if (opsAndBounds.isProbabilityObjective(i)) {
				acceptanceVector_H = JDD.PermuteVariables(acceptanceVector_H, draDDRowVars[i], draDDColVars[i]);
			}
		candidateStates = JDD.Apply(JDD.TIMES, candidateStates, acceptanceVector_H);
		candidateStates = JDD.ThereExists(candidateStates, modelProduct.getAllDDColVars());
		candidateStates = JDD.ThereExists(candidateStates, modelProduct.getAllDDNondetVars());
		// find all maximal end components
		List<JDDNode> allecs = mcLtl.findMECStates(modelProduct, candidateStates, acceptanceVector_L);
		JDD.Deref(candidateStates);
		JDD.Deref(acceptanceVector_L);
		return allecs;
	}

	//computes accepting end component for the Rabin automaton dra.
	//Vojta: in addition to calling a method which does the computation
	//there are some other bits which I don't currently understand
	protected JDDNode computeAcceptingEndComponent(DA<BitSet, AcceptanceRabin> dra, NondetModel modelProduct, JDDVars draDDRowVars, JDDVars draDDColVars,
			List<JDDNode> allecs, List<JDDNode> statesH, List<JDDNode> statesL, //Vojta: at the time of writing this I have no idea what these two parameters do, so I don't know how to call them
			LTLModelChecker mcLtl, boolean conflictformulaeGtOne) throws PrismException
	{
		long l = System.currentTimeMillis();
		// increase ref count for checking conflict formulas
		if (conflictformulaeGtOne) {
			for (JDDNode n : statesH)
				JDD.Ref(n);
			for (JDDNode n : statesL)
				JDD.Ref(n);
		}
		JDDNode ret = mcLtl.findMultiAcceptingStates(dra, modelProduct, draDDRowVars, draDDColVars, false, allecs, statesH, statesL);
		//targetDDs.add(mcLtl.findMultiAcceptingStates(dra[i], modelProduct, draDDRowVars[i], draDDColVars[i], false));
		l = System.currentTimeMillis() - l;
		mainLog.println("Time for end component identification: " + l / 1000.0 + " seconds.");
		return ret;
	}

	protected void removeNonZeroMecsForMax(NondetModel modelProduct, LTLModelChecker mcLtl, List<JDDNode> rewardsIndex, OpsAndBoundsList opsAndBounds,
			int numTargets, DA<BitSet, AcceptanceRabin> dra[], JDDVars draDDRowVars[], JDDVars draDDColVars[]) throws PrismException
	{
		List<JDDNode> mecs = mcLtl.findMECStates(modelProduct, modelProduct.getReach());
		JDDNode removedActions = JDD.Constant(0);
		JDDNode rmecs = JDD.Constant(0);
		for (int i = 0; i < rewardsIndex.size(); i++)
			if (opsAndBounds.getRewardOperator(i) == Operator.R_MAX || opsAndBounds.getRewardOperator(i) == Operator.R_GE) {
				JDD.Ref(rewardsIndex.get(i));
				JDDNode actions = JDD.GreaterThan(rewardsIndex.get(i), 0.0);
				if (!actions.equals(JDD.ZERO))
					for (int j = 0; j < mecs.size(); j++) {
						JDDNode mec = mecs.get(j);
						JDD.Ref(mec);
						JDDNode mecactions = mcLtl.maxStableSetTrans1(modelProduct, mec);
						JDD.Ref(actions);
						mecactions = JDD.And(actions, mecactions);
						if (!mecactions.equals(JDD.ZERO)) {
							JDD.Ref(mec);
							rmecs = JDD.Or(rmecs, mec);
						}
						removedActions = JDD.Or(removedActions, mecactions);
					}
				JDD.Deref(actions);
			}
		for (JDDNode mec : mecs)
			JDD.Deref(mec);
		// TODO: check if the model satisfies the LTL constraints 
		if (!rmecs.equals(JDD.ZERO)) {
			boolean constraintViolated = false;
			if (JDD.AreIntersecting(modelProduct.getStart(), rmecs)) {
				constraintViolated = true;
				JDD.Deref(rmecs);
			} else {
				// find all action pointing to mecs from outside a
				JDD.Ref(rmecs);
				JDDNode rtarget = JDD.PermuteVariables(rmecs, modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars());
				JDD.Ref(modelProduct.getTrans01());
				rtarget = JDD.And(modelProduct.getTrans01(), rtarget);
				rtarget = JDD.And(rtarget, JDD.Not(rmecs));
				//rtarget = JDD.ThereExists(rtarget, modelProduct.getAllDDColVars());
				// find target states for LTL formulae
				Vector<JDDNode> tmptargetDDs = new Vector<JDDNode>();
				List<JDDNode> tmpmultitargetDDs = new ArrayList<JDDNode>();
				List<Integer> tmpmultitargetIDs = new ArrayList<Integer>();
				ArrayList<DA<BitSet, AcceptanceRabin>> tmpdra = new ArrayList<DA<BitSet, AcceptanceRabin>>();
				ArrayList<JDDVars> tmpdraDDRowVars = new ArrayList<JDDVars>();
				ArrayList<JDDVars> tmpdraDDColVars = new ArrayList<JDDVars>();
				int count = 0;
				for (int i = 0; i < numTargets; i++)
					if (opsAndBounds.isProbabilityObjective(i) && opsAndBounds.getOperator(i) != Operator.P_MAX
							&& opsAndBounds.getOperator(i) != Operator.P_MIN) {
						tmpdra.add(dra[i]);
						tmpdraDDRowVars.add(draDDRowVars[i]);
						tmpdraDDColVars.add(draDDColVars[i]);
						count++;
					}
				if (count > 0) {
					// TODO: distinguish whether rtarget is empty
					DA<BitSet, AcceptanceRabin> newdra[] = new DA[count];
					tmpdra.toArray(newdra);
					JDDVars newdraDDRowVars[] = new JDDVars[count];
					tmpdraDDRowVars.toArray(newdraDDRowVars);
					JDDVars newdraDDColVars[] = new JDDVars[count];
					tmpdraDDColVars.toArray(newdraDDColVars);

					findTargetStates(modelProduct, mcLtl, count, count, new boolean[count], newdra, newdraDDRowVars, newdraDDColVars, tmptargetDDs,
							tmpmultitargetDDs, tmpmultitargetIDs);

					OpsAndBoundsList tmpOpsAndBounds = new OpsAndBoundsList();

					for (int i = 0; i < opsAndBounds.probSize(); i++) {
						if (opsAndBounds.getProbOperator(i) != Operator.P_MAX) {
							tmpOpsAndBounds.add(opsAndBounds.getOpRelOpBound(i), opsAndBounds.getProbOperator(i), opsAndBounds.getProbBound(i),
									opsAndBounds.getProbStepBound(i), i);
						}
					}
					tmpOpsAndBounds.add(new OpRelOpBound("R", RelOp.MAX, -1.0), Operator.R_MAX, -1.0, -1, opsAndBounds.probSize());

					ArrayList<JDDNode> tmprewards = new ArrayList<JDDNode>(1);
					tmprewards.add(rtarget);
					double prob = (Double) computeMultiReachProbs(modelProduct, mcLtl, tmprewards, modelProduct.getStart(), tmptargetDDs, tmpmultitargetDDs,
							tmpmultitargetIDs, tmpOpsAndBounds, count > 1);
					if (prob > 0.0) { // LTL formulae can be satisfied
						constraintViolated = true;
					} else if (Double.isNaN(prob))
						throw new PrismException("The LTL formulae in multi-objective query cannot be satisfied!\n");
				} else {
					// end components with non-zero rewards can always be reached
					constraintViolated = true;
				}

				//JDD.Deref(rtarget);
				for (JDDNode tt : tmptargetDDs)
					JDD.Deref(tt);
				for (JDDNode tt : tmpmultitargetDDs)
					JDD.Deref(tt);
			}
			if (constraintViolated) {
				throw new PrismNotSupportedException("Cannot use multi-objective model checking with maximising objectives and non-zero reward end compoments");
			}

			JDD.Ref(removedActions);
			modelProduct.trans = JDD.Apply(JDD.TIMES, modelProduct.trans, JDD.Not(removedActions));
			modelProduct.trans01 = JDD.Apply(JDD.TIMES, modelProduct.trans01, JDD.Not(removedActions));
		} else {
			JDD.Deref(rmecs);
			JDD.Deref(removedActions);
		}

	}

	//TODO is conflictformulae actually just no of prob?
	protected void checkConflictsInObjectives(NondetModel modelProduct, LTLModelChecker mcLtl, int conflictformulae, int numTargets,
			OpsAndBoundsList opsAndBounds, DA<BitSet, AcceptanceRabin> dra[], JDDVars draDDRowVars[], JDDVars draDDColVars[], List<JDDNode> targetDDs,
			List<ArrayList<JDDNode>> allstatesH, List<ArrayList<JDDNode>> allstatesL, List<JDDNode> multitargetDDs, List<Integer> multitargetIDs)
			throws PrismException
	{
		DA<BitSet, AcceptanceRabin>[] tmpdra = new DA[conflictformulae];
		JDDVars[] tmpdraDDRowVars = new JDDVars[conflictformulae];
		JDDVars[] tmpdraDDColVars = new JDDVars[conflictformulae];
		List<JDDNode> tmptargetDDs = new ArrayList<JDDNode>(conflictformulae);
		List<List<JDDNode>> tmpallstatesH = new ArrayList<List<JDDNode>>(conflictformulae);
		List<List<JDDNode>> tmpallstatesL = new ArrayList<List<JDDNode>>(conflictformulae);
		int count = 0;
		for (int i = 0; i < numTargets; i++)
			if (opsAndBounds.isProbabilityObjective(i)) {
				tmpdra[count] = dra[i];
				tmpdraDDRowVars[count] = draDDRowVars[i];
				tmpdraDDColVars[count] = draDDColVars[i];
				// tricky part; double check when reachExpr is set for efficiency
				tmptargetDDs.add(targetDDs.get(count));
				tmpallstatesH.add(allstatesH.get(i));
				tmpallstatesL.add(allstatesL.get(i));
				count++;
			}
		List<List<Integer>> tmpmultitargetIDs = new ArrayList<List<Integer>>();

		mcLtl.findMultiConflictAcceptingStates(tmpdra, modelProduct, tmpdraDDRowVars, tmpdraDDColVars, tmptargetDDs, tmpallstatesH, tmpallstatesL,
				multitargetDDs, tmpmultitargetIDs);
		// again. double check when reachExpr is set
		count = 0;
		for (int i = 0; i < numTargets; i++)
			if (opsAndBounds.isProbabilityObjective(i)) {
				targetDDs.remove(count);
				targetDDs.add(count, tmptargetDDs.get(count));
				count++;
			}

		// deal with reachability targets
		for (int i = 0; i < tmpmultitargetIDs.size(); i++) {
			multitargetIDs.add(changeToInteger(tmpmultitargetIDs.get(i)));
			//System.out.println("multitargetIDs["+i+"] = " + multitargetIDs.get(i));
		}

		for (int i = 0; i < numTargets; i++)
			if (opsAndBounds.isProbabilityObjective(i)) {
				List<JDDNode> tmpLH = allstatesH.get(i);
				for (JDDNode n : tmpLH)
					JDD.Deref(n);
				tmpLH = allstatesL.get(i);
				for (JDDNode n : tmpLH)
					JDD.Deref(n);
			}
	}

	protected void findTargetStates(NondetModel modelProduct, LTLModelChecker mcLtl, int numTargets, int conflictformulae, boolean reachExpr[],
			DA<BitSet, AcceptanceRabin> dra[], JDDVars draDDRowVars[], JDDVars draDDColVars[], List<JDDNode> targetDDs, List<JDDNode> multitargetDDs,
			List<Integer> multitargetIDs) throws PrismException
	{
		int i, j;
		long l;

		// Compute all maximal end components
		ArrayList<ArrayList<JDDNode>> allstatesH = new ArrayList<ArrayList<JDDNode>>(numTargets);
		ArrayList<ArrayList<JDDNode>> allstatesL = new ArrayList<ArrayList<JDDNode>>(numTargets);
		JDDNode acceptanceVector_H = JDD.Constant(0);
		JDDNode acceptanceVector_L = JDD.Constant(0);
		for (i = 0; i < numTargets; i++) {
			if (!reachExpr[i]) {
				ArrayList<JDDNode> statesH = new ArrayList<JDDNode>();
				ArrayList<JDDNode> statesL = new ArrayList<JDDNode>();
				for (int k = 0; k < dra[i].getAcceptance().size(); k++) {
					JDDNode tmpH = JDD.Constant(0);
					JDDNode tmpL = JDD.Constant(0);
					for (j = 0; j < dra[i].size(); j++) {
						if (!dra[i].getAcceptance().get(k).getL().get(j)) {
							tmpH = JDD.SetVectorElement(tmpH, draDDRowVars[i], j, 1.0);
						}
						if (dra[i].getAcceptance().get(k).getK().get(j)) {
							tmpL = JDD.SetVectorElement(tmpL, draDDRowVars[i], j, 1.0);
						}
					}
					statesH.add(tmpH);
					JDD.Ref(tmpH);
					acceptanceVector_H = JDD.Or(acceptanceVector_H, tmpH);
					statesL.add(tmpL);
					JDD.Ref(tmpL);
					acceptanceVector_L = JDD.Or(acceptanceVector_L, tmpL);
				}
				allstatesH.add(i, statesH);
				allstatesL.add(i, statesL);
			} else {
				allstatesH.add(i, null);
				allstatesL.add(i, null);
			}
		}

		// Find accepting maximum end components for each LTL formula
		List<JDDNode> allecs = null;
		//use acceptanceVector_H and acceptanceVector_L to speed up scc computation
		/*// check number of states in each scc
		allecs = mcLtl.findEndComponents(modelProduct, modelProduct.getReach());
		StateListMTBDD vl;
		int totalNum = 0;
		for(JDDNode set : allecs) {
			vl = new StateListMTBDD(set, modelProduct);
			totalNum += vl.size() - 1;
		}
		mainLog.println("Total number of states can be saved: " + totalNum);*/

		JDD.Ref(acceptanceVector_H);
		JDD.Ref(modelProduct.getTrans01());
		JDDNode candidateStates = JDD.Apply(JDD.TIMES, modelProduct.getTrans01(), acceptanceVector_H);
		for (i = 0; i < numTargets; i++)
			if (!reachExpr[i]) {
				acceptanceVector_H = JDD.PermuteVariables(acceptanceVector_H, draDDRowVars[i], draDDColVars[i]);
			}
		candidateStates = JDD.Apply(JDD.TIMES, candidateStates, acceptanceVector_H);
		candidateStates = JDD.ThereExists(candidateStates, modelProduct.getAllDDColVars());
		candidateStates = JDD.ThereExists(candidateStates, modelProduct.getAllDDNondetVars());
		// find all maximal end components
		allecs = mcLtl.findMECStates(modelProduct, candidateStates, acceptanceVector_L);
		JDD.Deref(candidateStates);
		JDD.Deref(acceptanceVector_L);

		for (i = 0; i < numTargets; i++) {
			if (!reachExpr[i]) {
				//mainLog.println("\nFinding accepting end components for " + targetName[i] + "...");
				l = System.currentTimeMillis();
				// increase ref count for checking conflict formulas
				if (conflictformulae > 1) {
					List<JDDNode> tmpLH = allstatesH.get(i);
					for (JDDNode n : tmpLH)
						JDD.Ref(n);
					tmpLH = allstatesL.get(i);
					for (JDDNode n : tmpLH)
						JDD.Ref(n);
				}
				targetDDs.add(mcLtl.findMultiAcceptingStates(dra[i], modelProduct, draDDRowVars[i], draDDColVars[i], false, allecs, allstatesH.get(i),
						allstatesL.get(i)));
				//targetDDs.add(mcLtl.findMultiAcceptingStates(dra[i], modelProduct, draDDRowVars[i], draDDColVars[i], false));
				l = System.currentTimeMillis() - l;
				mainLog.println("Time for end component identification: " + l / 1000.0 + " seconds.");
			} /*else {
				// Fixme: maybe not efficient
				if(targetExprs.get(i) != null) {
					dd = checkExpressionDD(targetExprs.get(i));
					JDD.Ref(modelProduct.getReach());
					dd = JDD.And(dd, modelProduct.getReach());
					targetDDs.add(dd);
				}
				}*/
			//mainLog.print("\n    number of targets = " + JDD.GetNumMintermsString(targetDDs.get(i), modelProduct.getAllDDRowVars().n()));
			//if(i>0)
			//	mainLog.print("\n  total number of targets = " + 
			//			JDD.GetNumMintermsString(JDD.Or(targetDDs.get(i), targetDDs.get(i-1)), modelProduct.getAllDDRowVars().n()));
		}

		// check if there are conflicts in objectives
		if (conflictformulae > 1) {
			DA<BitSet, AcceptanceRabin>[] tmpdra = new DA[conflictformulae];
			JDDVars[] tmpdraDDRowVars = new JDDVars[conflictformulae];
			JDDVars[] tmpdraDDColVars = new JDDVars[conflictformulae];
			List<JDDNode> tmptargetDDs = new ArrayList<JDDNode>(conflictformulae);
			List<List<JDDNode>> tmpallstatesH = new ArrayList<List<JDDNode>>(conflictformulae);
			List<List<JDDNode>> tmpallstatesL = new ArrayList<List<JDDNode>>(conflictformulae);
			int count = 0;
			for (i = 0; i < numTargets; i++)
				if (!reachExpr[i]) {
					tmpdra[count] = dra[i];
					tmpdraDDRowVars[count] = draDDRowVars[i];
					tmpdraDDColVars[count] = draDDColVars[i];
					// tricky part; double check when reachExpr is set for efficiency
					tmptargetDDs.add(targetDDs.get(count));
					tmpallstatesH.add(allstatesH.get(i));
					tmpallstatesL.add(allstatesL.get(i));
					count++;
				}
			//multitargetDDs = new ArrayList<JDDNode>();
			List<List<Integer>> tmpmultitargetIDs = new ArrayList<List<Integer>>();

			mcLtl.findMultiConflictAcceptingStates(tmpdra, modelProduct, tmpdraDDRowVars, tmpdraDDColVars, tmptargetDDs, tmpallstatesH, tmpallstatesL,
					multitargetDDs, tmpmultitargetIDs);
			// again. double check when reachExpr is set
			count = 0;
			for (i = 0; i < numTargets; i++)
				if (!reachExpr[i]) {
					targetDDs.remove(count);
					targetDDs.add(count, tmptargetDDs.get(count));
					count++;
				}

			// deal with reachability targets
			//multitargetIDs = new ArrayList<Integer>(tmpmultitargetIDs.size());
			for (i = 0; i < tmpmultitargetIDs.size(); i++) {
				multitargetIDs.add(changeToInteger(tmpmultitargetIDs.get(i)));
			}

			for (i = 0; i < numTargets; i++)
				if (!reachExpr[i]) {
					List<JDDNode> tmpLH = allstatesH.get(i);
					for (JDDNode n : tmpLH)
						JDD.Deref(n);
					tmpLH = allstatesL.get(i);
					for (JDDNode n : tmpLH)
						JDD.Deref(n);
				}
		}

		for (JDDNode ec : allecs)
			JDD.Deref(ec);
	}

	private int changeToInteger(List<Integer> ids)
	{
		int k = 0;
		for (int i = 0; i < ids.size(); i++) {
			int j = 1;
			if (ids.get(i) > 0)
				j = j << ids.get(i);
			k += j;
		}
		return k;
	}

	/**
	 * Perform multi-objective model checking computation.
	 * Solves achievability, numerical or Pareto queries over n objectives.
	 *  
	 * @param model The MDP
	 * @param mcLtl An LTL model checker (for finding end components)
	 * @param transRewards MTBDDs for transition rewards (reward objectives only)  
	 * @param start BDD giving the (initial) state for which values are to be computed
	 * @param targets BDDs giving sets of target states (probability objectives only)
	 * @param combinations
	 * @param combinationIDs
	 * @param opsAndBounds Information about the list of objectives 
	 * @param hasconflictobjectives
	 */
	protected Object computeMultiReachProbs(NondetModel model, LTLModelChecker mcLtl, List<JDDNode> transRewards, JDDNode start, List<JDDNode> targets,
			List<JDDNode> combinations, List<Integer> combinationIDs, OpsAndBoundsList opsAndBounds, boolean hasconflictobjectives) throws PrismException
	{
		JDDNode yes, no, maybe, bottomec = null;
		Object value;
		int i, j, numTargets;
		//JDDNode maybe_r = null; // maybe states for the reward formula
		//JDDNode trr = null; // transition rewards
		//int op1 = relOps.get(0).intValue(); // the operator of the first objective query

		// Get number of targets
		numTargets = targets.size();

		JDDNode labels[] = new JDDNode[numTargets];
		// Build temporary DDs for combined targets
		for (i = 0; i < numTargets; i++) {
			JDD.Ref(targets.get(i));
			JDDNode tmp = targets.get(i);
			if (combinations != null) {
				for (j = 0; j < combinations.size(); j++) {
					if ((combinationIDs.get(j) & (1 << i)) > 0) {
						JDD.Ref(combinations.get(j));
						tmp = JDD.Or(tmp, combinations.get(j));
					}
				}
			}
			labels[i] = tmp;
		}

		// If required, export info about target states 
		if (prism.getExportTarget()) {
			JDDNode labels2[] = new JDDNode[numTargets + 1];
			String labelNames[] = new String[numTargets + 1];
			labels2[0] = model.getStart();
			labelNames[0] = "init";
			for (i = 0; i < numTargets; i++) {
				labels2[i + 1] = labels[i];
				labelNames[i + 1] = "target" + i;
			}
			try {
				mainLog.print("\nExporting target states info to file \"" + prism.getExportTargetFilename() + "\"...");
				PrismMTBDD.ExportLabels(labels2, labelNames, "l", model.getAllDDRowVars(), model.getODD(), Prism.EXPORT_PLAIN, prism.getExportTargetFilename());
			} catch (FileNotFoundException e) {
				mainLog.println("\nWarning: Could not export target to file \"" + prism.getExportTargetFilename() + "\"");
			}
		}

		// yes - union of targets (just to compute no)
		yes = JDD.Constant(0);
		//n = targets.size();
		for (i = 0; i < numTargets; i++) {
			JDD.Ref(targets.get(i));
			yes = JDD.Or(yes, targets.get(i));
		}
		if (combinations != null)
			for (i = 0; i < combinations.size(); i++) {
				JDD.Ref(combinations.get(i));
				yes = JDD.Or(yes, combinations.get(i));
			}

		if (opsAndBounds.rewardSize() == 0)
			no = PrismMTBDD.Prob0A(model.getTrans01(), model.getReach(), model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars(),
					model.getReach(), yes);
		else {
			no = JDD.Constant(0);
			bottomec = PrismMTBDD.Prob0A(model.getTrans01(), model.getReach(), model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars(),
					model.getReach(), yes);
			List<JDDNode> becs = mcLtl.findMECStates(model, bottomec);
			JDD.Deref(bottomec);
			bottomec = JDD.Constant(0);
			for (JDDNode ec : becs)
				bottomec = JDD.Or(bottomec, ec);
		}

		/*if(op1>2) { // the first query is about reward
			JDDNode no_r = PrismMTBDD.Prob0A(modelProduct.getTrans01(), modelProduct.getReach(), 
					modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(), 
					modelProduct.getAllDDNondetVars(), modelProduct.getReach(), targets.get(0));
			JDD.Ref(no_r);
			maybe_r = JDD.And(modelProduct.getReach(), JDD.Not(JDD.Or(targets.get(0), no_r)));
			trr = modelProduct.getTransRewards();
		}*/

		// maybe
		JDD.Ref(model.getReach());
		JDD.Ref(yes);
		JDD.Ref(no);
		maybe = JDD.And(model.getReach(), JDD.Not(JDD.Or(yes, no)));

		for (i = 0; i < transRewards.size(); i++) {
			JDDNode tmp = transRewards.remove(i);
			JDD.Ref(no);
			tmp = JDD.Apply(JDD.TIMES, tmp, JDD.Not(no));
			transRewards.add(i, tmp);
		}

		// print out yes/no/maybe
		mainLog.print("\nyes = " + JDD.GetNumMintermsString(yes, model.getAllDDRowVars().n()));
		mainLog.print(", no = " + JDD.GetNumMintermsString(no, model.getAllDDRowVars().n()));
		mainLog.print(", maybe = " + JDD.GetNumMintermsString(maybe, model.getAllDDRowVars().n()) + "\n");

		// compute probabilities
		mainLog.println("\nComputing remaining probabilities...");

		// Local copies of settings
		int engine = settings.getChoice(PrismSettings.PRISM_ENGINE);
		int method = prism.getMDPMultiSolnMethod();

		// Switch engine, if necessary
		if (engine == Prism.HYBRID) {
			mainLog.println("Switching engine since only sparse engine currently supports this computation...");
			engine = Prism.SPARSE;
		}
		mainLog.println("Engine: " + Prism.getEngineString(engine));

		try {
			// Check for unsupported options
			if (engine != Prism.SPARSE) {
				throw new PrismNotSupportedException("Currently only sparse engine supports multi-objective properties");
			}
			if (method == Prism.MDP_MULTI_LP && opsAndBounds.numberOfNumerical() > 1) {
				throw new PrismNotSupportedException("Pareto curve generation is not currently supported using linear programming");
			}

			// Do computation
			// Linear programming
			if (method == Prism.MDP_MULTI_LP) {

				if (opsAndBounds.numberOfStepBounded() > 0) {
					throw new PrismNotSupportedException("Step-bounded objectives are not currently supported with linear programming");
				}

				if (opsAndBounds.rewardSize() > 0) {
					if (hasconflictobjectives) {
						value = PrismSparse.NondetMultiReachReward1(model.getTrans(), model.getTransActions(), model.getSynchs(), model.getODD(),
								model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars(), targets, combinations, combinationIDs,
								opsAndBounds, maybe, start, transRewards, bottomec);
					} else {
						value = PrismSparse.NondetMultiReachReward(model.getTrans(), model.getTransActions(), model.getSynchs(), model.getODD(),
								model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars(), targets, opsAndBounds, maybe, start, transRewards,
								bottomec);
					}
				} else {
					if (hasconflictobjectives) {
						value = PrismSparse.NondetMultiReach1(model.getTrans(), model.getTransActions(), model.getSynchs(), model.getODD(),
								model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars(), targets, combinations, combinationIDs,
								opsAndBounds, maybe, start);
					} else {
						value = PrismSparse.NondetMultiReach(model.getTrans(), model.getTransActions(), model.getSynchs(), model.getODD(),
								model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars(), targets, opsAndBounds, maybe, start);
					}
				}
			}
			// Value iteration
			else if (method == Prism.MDP_MULTI_GAUSSSEIDEL || method == Prism.MDP_MULTI_VALITER) {
				double timePre = System.currentTimeMillis();
				value = weightedMultiReachProbs(model, yes, maybe, start, labels, transRewards, opsAndBounds);
				double timePost = System.currentTimeMillis();
				double time = ((double) (timePost - timePre)) / 1000.0;
				mainLog.println("Multi-objective value iterations took " + time + " s.");
			}
			// Unknown method (shouldn't happen)
			else {
				throw new PrismException("Unknown multi-objective model checking method");
			}
		} catch (PrismException e) {
			throw e;
		} finally {
			// derefs
			if (opsAndBounds.rewardSize() > 0)
				JDD.Deref(bottomec);
			JDD.Deref(yes);
			JDD.Deref(no);
			JDD.Deref(maybe);
			for (int k = 0; k < labels.length; k++)
				JDD.Deref(labels[k]);
			for (i = 0; i < transRewards.size(); i++) {
				JDD.Deref(transRewards.get(i));
			}
		}

		return value;
	}

	protected Object weightedMultiReachProbs(NondetModel modelProduct, JDDNode yes_ones, JDDNode maybe, JDDNode start, JDDNode[] targets, List<JDDNode> rewards,
			OpsAndBoundsList opsAndBounds) throws PrismException
	{
		int numNumericalObjectives = opsAndBounds.numberOfNumerical();

		// Check for unsupported computations
		if (numNumericalObjectives > 2) {
			throw new PrismException("Pareto curve generation is currently only supported for 2 objectives");
		}
		if (numNumericalObjectives >= 2 && opsAndBounds.probSize() + opsAndBounds.rewardSize() > numNumericalObjectives) {
			throw new PrismException("Pareto curve generation is currently not allowed if there are other (bounded) objectives");
		}

		// Pareto computation or achievability/numerical computation
		if (numNumericalObjectives >= 2) {
			return generateParetoCurve(modelProduct, yes_ones, maybe, start, targets, rewards, opsAndBounds);
		} else {
			return targetDrivenMultiReachProbs(modelProduct, yes_ones, maybe, start, targets, rewards, opsAndBounds);
		}
	}

	protected TileList generateParetoCurve(NondetModel modelProduct, JDDNode yes_ones, JDDNode maybe, final JDDNode st, JDDNode[] targets,
			List<JDDNode> rewards, OpsAndBoundsList opsAndBounds) throws PrismException
	{
		//TODO this method does not work for more than 2 objectives
		int numberOfPoints = 0;
		int rewardStepBounds[] = new int[rewards.size()];
		for (int i = 0; i < rewardStepBounds.length; i++)
			rewardStepBounds[i] = opsAndBounds.getRewardStepBound(i);

		int probStepBounds[] = new int[targets.length];
		for (int i = 0; i < probStepBounds.length; i++)
			probStepBounds[i] = opsAndBounds.getProbStepBound(i);

		double timer = System.currentTimeMillis();
		boolean min = false;
		int advCounter = 0;

		// Determine whether we are using Gauss-Seidel value iteration
		boolean useGS = (settings.getChoice(PrismSettings.PRISM_MDP_SOLN_METHOD) == Prism.MDP_MULTI_GAUSSSEIDEL);
		if (opsAndBounds.numberOfStepBounded() > 0) {
			mainLog.println("Not using Gauss-Seidel since there are step-bounded objectives");
			useGS = false;
		}

		//convert minimizing rewards to maximizing
		for (int i = 0; i < opsAndBounds.rewardSize(); i++) {
			if (opsAndBounds.getRewardOperator(i) == Operator.R_LE) {
				JDDNode negated = JDD.Apply(JDD.TIMES, JDD.Constant(-1), rewards.get(i));
				//JDD.Ref(negated);
				rewards.set(i, negated);
				//boundsRewards.set(i, -1 * boundsRewards.get(i));
			}

			if (opsAndBounds.getRewardOperator(i) == Operator.R_MIN) {
				JDDNode negated = JDD.Apply(JDD.TIMES, JDD.Constant(-1), rewards.get(i));
				//JDD.Ref(negated);
				rewards.set(i, negated);
				//boundsRewards.set(i, -1 * boundsRewards.get(i));
			}
		}

		double tolerance = settings.getDouble(PrismSettings.PRISM_PARETO_EPSILON);
		int maxIters = settings.getInteger(PrismSettings.PRISM_MULTI_MAX_POINTS);

		int exportAdvSetting = settings.getChoice(PrismSettings.PRISM_EXPORT_ADV);

		NativeIntArray adversary = new NativeIntArray((int) modelProduct.getNumStates());
		int dimProb = targets.length;
		int dimReward = rewards.size();
		Point targetPoint = new Point(dimProb + dimReward);
		ArrayList<Point> computedPoints = new ArrayList<Point>();
		ArrayList<Point> computedDirections = new ArrayList<Point>();
		ArrayList<Point> pointsForInitialTile = new ArrayList<Point>();

		//create vectors and sparse matrices for the objectives
		final DoubleVector[] probDoubleVectors = new DoubleVector[dimProb];
		final NDSparseMatrix[] rewSparseMatrices = new NDSparseMatrix[dimReward];

		JDD.Ref(modelProduct.getTrans());
		JDD.Ref(modelProduct.getReach());

		//create a sparse matrix for transitions
		JDDNode a = JDD.Apply(JDD.TIMES, modelProduct.getTrans(), modelProduct.getReach());

		if (!min && dimReward == 0) {
			JDD.Ref(a);
			JDDNode tmp = JDD.And(JDD.Equals(a, 1.0), JDD.Identity(modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars()));
			a = JDD.ITE(tmp, JDD.Constant(0), a);
		}

		NDSparseMatrix trans_matrix = NDSparseMatrix.BuildNDSparseMatrix(a, modelProduct.getODD(), modelProduct.getAllDDRowVars(),
				modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars());

		// If adversary generation is enabled, we build/store action info
		if (settings.getChoice(PrismSettings.PRISM_EXPORT_ADV) != Prism.EXPORT_ADV_NONE) {
			NDSparseMatrix.AddActionsToNDSparseMatrix(a, modelProduct.getTransActions(), modelProduct.getODD(), modelProduct.getAllDDRowVars(),
					modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(), trans_matrix);
		}

		//create double vectors for probabilistic objectives
		for (int i = 0; i < dimProb; i++) {
			probDoubleVectors[i] = new DoubleVector(targets[i], modelProduct.getAllDDRowVars(), modelProduct.getODD());
		}

		//create sparse matrices for reward objectives
		for (int i = 0; i < dimReward; i++) {
			NDSparseMatrix rew_matrix = NDSparseMatrix.BuildSubNDSparseMatrix(a, modelProduct.getODD(), modelProduct.getAllDDRowVars(),
					modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(), rewards.get(i));
			rewSparseMatrices[i] = rew_matrix;
		}

		JDD.Deref(a);

		for (int i = 0; i < dimProb; i++) {
			double[] result = null;

			// Optimise in direction of probability objective i
			Point direction = new Point(dimProb + dimReward);
			direction.setCoord(i, 1);
			try {
				// If adversary generation is enabled, we amend the filename so that multiple adversaries can be exported
				String advFileName = settings.getString(PrismSettings.PRISM_EXPORT_ADV_FILENAME);
				if (settings.getChoice(PrismSettings.PRISM_EXPORT_ADV) != Prism.EXPORT_ADV_NONE) {
					PrismNative.setExportAdvFilename(PrismUtils.addCounterSuffixToFilename(advFileName, ++advCounter));
				}
				mainLog.println("Optimising weighted sum for probability objective " + (i + 1) + "/" + dimProb + ": weights " + direction);
				if (useGS) {
					result = PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
							modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, probDoubleVectors, rewSparseMatrices, direction.getCoords());
				} else {
					result = PrismSparse.NondetMultiObj(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
							modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, modelProduct.getSynchs(), probDoubleVectors, probStepBounds,
							rewSparseMatrices, direction.getCoords(), rewardStepBounds);
				}
			} catch (PrismException e) {
				// If anything went wrong (in particular, non-convergence of the computation), use another direction
				mainLog.println("Ignoring the last multi-objective computation since it did not complete successfully");
				// Optimise in almost the direction of probability objective i
				double large = 10000;
				for (int j = 0; j < dimProb + dimReward; j++) {
					direction.setCoord(j, j == i ? large : 1);
				}
				direction = direction.normalize();
				mainLog.println("Optimising weighted sum for probability objective " + (i + 1) + "/" + dimProb + ": weights " + direction);
				if (useGS) {
					result = PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
							modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, probDoubleVectors, rewSparseMatrices, direction.getCoords());
				} else {
					result = PrismSparse.NondetMultiObj(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
							modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, modelProduct.getSynchs(), probDoubleVectors, probStepBounds,
							rewSparseMatrices, direction.getCoords(), rewardStepBounds);
				}
			}

			//The following is thrown because in this case the i-th dimension is
			//zero and we might have problems when getting an separating hyperplane.
			/*if (result[0] == 0)
				throw new PrismException("The probabilistic objective number " + i + " is degenerate since the optimal value is also the least optimal value." );
			*/
			targetPoint = new Point(result);
			mainLog.println("Computed point: " + targetPoint);
			pointsForInitialTile.add(targetPoint);
		}

		for (int i = 0; i < dimReward; i++) {

			double[] result = null;
			// Optimise in direction of reward objective i
			Point direction = new Point(dimProb + dimReward);
			direction.setCoord(dimProb + i, 1);
			try {
				// If adversary generation is enabled, we amend the filename so that multiple adversaries can be exported
				String advFileName = settings.getString(PrismSettings.PRISM_EXPORT_ADV_FILENAME);
				if (settings.getChoice(PrismSettings.PRISM_EXPORT_ADV) != Prism.EXPORT_ADV_NONE) {
					PrismNative.setExportAdvFilename(PrismUtils.addCounterSuffixToFilename(advFileName, ++advCounter));
				}
				mainLog.println("Optimising weighted sum for reward objective " + (i + 1) + "/" + dimReward + ": weights " + direction);
				if (useGS) {
					result = PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
							modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, probDoubleVectors, rewSparseMatrices, direction.getCoords());
				} else {
					result = PrismSparse.NondetMultiObj(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
							modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, modelProduct.getSynchs(), probDoubleVectors, probStepBounds,
							rewSparseMatrices, direction.getCoords(), rewardStepBounds);
				}
			} catch (PrismException e) {
				// If anything went wrong (in particular, non-convergence of the computation), use another direction
				mainLog.println("Ignoring the last multi-objective computation since it did not complete successfully");
				// Optimise in almost the direction of reward objective i
				double large = 10000;
				for (int j = 0; j < dimProb + dimReward; j++) {
					direction.setCoord(j, j == dimProb + i ? large : 1);
				}
				direction = direction.normalize();
				mainLog.println("Optimising weighted sum for reward objective " + (i + 1) + "/" + dimReward + ": weights " + direction);
				if (useGS) {
					result = PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
							modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, probDoubleVectors, rewSparseMatrices, direction.getCoords());
				} else {
					result = PrismSparse.NondetMultiObj(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
							modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, modelProduct.getSynchs(), probDoubleVectors, probStepBounds,
							rewSparseMatrices, direction.getCoords(), rewardStepBounds);
				}
			}

			numberOfPoints++;
			targetPoint = new Point(result);
			mainLog.println("Computed point: " + targetPoint);
			pointsForInitialTile.add(targetPoint);

			if (verbose) {
				mainLog.println("Upper bound is " + Arrays.toString(result));
			}
		}

		if (verbose)
			mainLog.println("Points for the initial tile: " + pointsForInitialTile);

		Tile initialTile = new Tile(pointsForInitialTile);
		TileList tileList = new TileList(initialTile, opsAndBounds, tolerance);

		Point direction = tileList.getCandidateHyperplane();

		if (verbose) {
			mainLog.println("The initial direction is " + direction);
		}

		boolean decided = false;
		int iters = 0;
		while (iters < maxIters) {
			iters++;

			// If adversary generation is enabled, we amend the filename so that multiple adversaries can be exported
			String advFileName = settings.getString(PrismSettings.PRISM_EXPORT_ADV_FILENAME);
			if (settings.getChoice(PrismSettings.PRISM_EXPORT_ADV) != Prism.EXPORT_ADV_NONE) {
				PrismNative.setExportAdvFilename(PrismUtils.addCounterSuffixToFilename(advFileName, ++advCounter));
			}
			mainLog.println("Optimising weighted sum of objectives: weights " + direction);
			double[] result;
			if (useGS) {
				result = PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
						modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, probDoubleVectors, rewSparseMatrices, direction.getCoords());
			} else {
				result = PrismSparse.NondetMultiObj(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
						modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, modelProduct.getSynchs(), probDoubleVectors, probStepBounds,
						rewSparseMatrices, direction.getCoords(), rewardStepBounds);
			}

			/*	//Minimizing operators are negated, and for Pareto we need to maximize.
				for (int i = 0; i < dimProb; i++) {
					if (opsAndBounds.getOperator(i) == Operator.P_MIN) {
						result[i] = -(1-result[i]);
					}
				} */

			numberOfPoints++;

			//collect the numbers obtained from methods executed above.
			Point newPoint = new Point(result);
			mainLog.println("Computed point: " + newPoint);

			if (verbose) {
				mainLog.println("\n" + numberOfPoints + ": New point is " + newPoint + ".");
				mainLog.println("TileList:" + tileList);
			}

			computedPoints.add(newPoint);
			computedDirections.add(direction);

			tileList.addNewPoint(newPoint);
			//mainLog.println("\nTiles after adding: " + tileList);
			//compute new direction
			direction = tileList.getCandidateHyperplane();

			if (verbose) {
				mainLog.println("New direction is " + direction);
				//mainLog.println("TileList: " + tileList);

			}

			if (direction == null) {
				//no tile could be improved
				decided = true;
				break;
			}
		}

		timer = System.currentTimeMillis() - timer;
		mainLog.println("The value iteration(s) took " + timer / 1000.0 + " seconds altogether.");
		mainLog.println("Number of weight vectors used: " + numberOfPoints);

		if (!decided)
			throw new PrismException("The computation did not finish in " + maxIters
					+ " target point iterations, try increasing this number using the -multimaxpoints switch.");
		else {
			String paretoFile = settings.getString(PrismSettings.PRISM_EXPORT_PARETO_FILENAME);

			//export to file if required
			if (paretoFile != null && !paretoFile.equals("")) {
				MultiObjUtils.exportPareto(tileList, paretoFile);
				mainLog.println("Exported Pareto curve. To see it, run\n etc/scripts/prism-pareto.py " + paretoFile);
			}

			if (verbose) {
				mainLog.print("Computed " + tileList.getNumberOfDifferentPoints() + " points altogether: ");
				mainLog.println(tileList.getPoints().toString());
			}

			return tileList;
		}
	}

	protected double targetDrivenMultiReachProbs(NondetModel modelProduct, JDDNode yes_ones, JDDNode maybe, final JDDNode st, JDDNode[] targets,
			List<JDDNode> rewards, OpsAndBoundsList opsAndBounds) throws PrismException
	{
		int numberOfPoints = 0;
		int rewardStepBounds[] = new int[rewards.size()];
		for (int i = 0; i < rewardStepBounds.length; i++)
			rewardStepBounds[i] = opsAndBounds.getRewardStepBound(i);

		int probStepBounds[] = new int[targets.length];
		for (int i = 0; i < probStepBounds.length; i++)
			probStepBounds[i] = opsAndBounds.getProbStepBound(i);

		double timer = System.currentTimeMillis();
		boolean min = false;

		// Determine whether we are using Gauss-Seidel value iteration
		boolean useGS = (settings.getChoice(PrismSettings.PRISM_MDP_SOLN_METHOD) == Prism.MDP_MULTI_GAUSSSEIDEL);
		if (opsAndBounds.numberOfStepBounded() > 0) {
			mainLog.println("Not using Gauss-Seidel since there are step-bounded objectives");
			useGS = false;
		}

		//convert minimizing rewards to maximizing
		for (int i = 0; i < opsAndBounds.rewardSize(); i++) {
			if (opsAndBounds.getRewardOperator(i) == Operator.R_LE) {
				JDDNode negated = JDD.Apply(JDD.TIMES, JDD.Constant(-1), rewards.get(i));
				//JDD.Ref(negated);
				rewards.set(i, negated);
				//boundsRewards.set(i, -1 * boundsRewards.get(i));
			}

			if (opsAndBounds.getRewardOperator(i) == Operator.R_MIN) {
				JDDNode negated = JDD.Apply(JDD.TIMES, JDD.Constant(-1), rewards.get(i));
				//JDD.Ref(negated);
				rewards.set(i, negated);
				//boundsRewards.set(i, -1 * boundsRewards.get(i));
			}
		}

		boolean maximizingProb = (opsAndBounds.probSize() > 0 && (opsAndBounds.getProbOperator(0) == Operator.P_MAX || opsAndBounds.getProbOperator(0) == Operator.P_MIN));
		boolean maximizingReward = (opsAndBounds.rewardSize() > 0 && (opsAndBounds.getRewardOperator(0) == Operator.R_MAX || opsAndBounds.getRewardOperator(0) == Operator.R_MIN));
		boolean maximizingNegated = (maximizingProb && opsAndBounds.getProbOperator(0) == Operator.P_MIN)
				|| (maximizingReward && opsAndBounds.getRewardOperator(0) == Operator.R_MIN);

		int maxIters = settings.getInteger(PrismSettings.PRISM_MULTI_MAX_POINTS);

		NativeIntArray adversary = new NativeIntArray((int) modelProduct.getNumStates());
		int dimProb = targets.length;
		int dimReward = rewards.size();
		Point targetPoint = new Point(dimProb + dimReward);
		ArrayList<Point> computedPoints = new ArrayList<Point>();
		ArrayList<Point> computedDirections = new ArrayList<Point>();

		//create vectors and sparse matrices for the objectives
		final DoubleVector[] probDoubleVectors = new DoubleVector[dimProb];
		final NDSparseMatrix[] rewSparseMatrices = new NDSparseMatrix[dimReward];

		JDD.Ref(modelProduct.getTrans());
		JDD.Ref(modelProduct.getReach());

		//create a sparse matrix for transitions
		JDDNode a = JDD.Apply(JDD.TIMES, modelProduct.getTrans(), modelProduct.getReach());

		if (!min && dimReward == 0) {
			JDD.Ref(a);
			JDDNode tmp = JDD.And(JDD.Equals(a, 1.0), JDD.Identity(modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars()));
			a = JDD.ITE(tmp, JDD.Constant(0), a);
		}

		NDSparseMatrix trans_matrix = NDSparseMatrix.BuildNDSparseMatrix(a, modelProduct.getODD(), modelProduct.getAllDDRowVars(),
				modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars());

		//create double vectors for probabilistic objectives
		for (int i = 0; i < dimProb; i++) {
			probDoubleVectors[i] = new DoubleVector(targets[i], modelProduct.getAllDDRowVars(), modelProduct.getODD());
		}

		//create sparse matrices for reward objectives
		for (int i = 0; i < dimReward; i++) {
			NDSparseMatrix rew_matrix = NDSparseMatrix.BuildSubNDSparseMatrix(a, modelProduct.getODD(), modelProduct.getAllDDRowVars(),
					modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(), rewards.get(i));
			rewSparseMatrices[i] = rew_matrix;
		}

		JDD.Deref(a);

		//initialize the target point
		for (int i = 0; i < dimProb; i++) {
			targetPoint.setCoord(i, opsAndBounds.getProbBound(i));
		}
		if (maximizingProb) {
			targetPoint.setCoord(0, 1.0);
		}

		for (int i = 0; i < dimReward; i++) {
			//multiply by -1 in case of minimizing, that converts it to maximizing.
			double t = (opsAndBounds.getRewardOperator(i) == Operator.R_LE) ? -opsAndBounds.getRewardBound(i) : opsAndBounds.getRewardBound(i);
			targetPoint.setCoord(i + dimProb, t);
		}
		if (maximizingReward) {
			if (verbose) {
				mainLog.println("Getting an upper bound on maximizing objective");
			}

			double[] result;
			if (useGS) {
				//System.out.println("Doing GS");
				result = PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
						modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, null, new NDSparseMatrix[] { rewSparseMatrices[0] },
						new double[] { 1.0 });
			} else {
				//System.out.println("Not doing GS");
				result = PrismSparse.NondetMultiObj(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
						modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, modelProduct.getSynchs(), null, null,
						new NDSparseMatrix[] { rewSparseMatrices[0] }, new double[] { 1.0 }, new int[] { rewardStepBounds[0] });
			}
			numberOfPoints++;

			targetPoint.setCoord(dimProb, result[0]);

			if (verbose) {
				mainLog.println("Upper bound is " + result[0]);
			}
		}

		Point direction = MultiObjUtils.getWeights(targetPoint, computedPoints);

		if (verbose) {
			mainLog.println("The initial target point is " + targetPoint);
			mainLog.println("The initial direction is " + direction);
		}

		boolean decided = false;
		boolean isAchievable = false;
		int iters = 0;
		while (iters < maxIters) {
			iters++;

			//create the weights array
			double[] weights = new double[dimProb + dimReward];
			for (int i = 0; i < dimProb + dimReward; i++) {
				weights[i] = direction.getCoord(i);
			}

			double[] result;
			if (useGS) {
				result = PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
						modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, probDoubleVectors, rewSparseMatrices, weights);
			} else {
				result = PrismSparse.NondetMultiObj(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
						modelProduct.getAllDDNondetVars(), false, st, adversary, trans_matrix, modelProduct.getSynchs(), probDoubleVectors, probStepBounds,
						rewSparseMatrices, weights, rewardStepBounds);
			}
			numberOfPoints++;

			//collect the numbers obtained from methods executed above.
			Point newPoint = new Point(result);

			if (verbose) {
				mainLog.println("New point is " + newPoint + ".");
			}

			computedPoints.add(newPoint);
			computedDirections.add(direction);

			//if (prism.getExportMultiGraphs())
			//	MultiObjUtils.printGraphFileDebug(targetPoint, computedPoints, computedDirections, prism.getExportMultiGraphsDir(), output++);

			//check if the new point together with the direction shows the point is unreachable
			double dNew = 0.0;
			for (int i = 0; i < dimProb + dimReward; i++) {
				dNew += newPoint.getCoord(i) * direction.getCoord(i);
			}

			double dTarget = 0.0;
			for (int i = 0; i < dimProb + dimReward; i++) {
				dTarget += targetPoint.getCoord(i) * direction.getCoord(i);
			}

			if (dTarget > dNew) {
				if (maximizingProb || maximizingReward) {
					int maximizingCoord = (maximizingProb) ? 0 : dimProb;
					double rest = dNew - (dTarget - direction.getCoord(maximizingCoord) * targetPoint.getCoord(maximizingCoord));
					if ((!maximizingNegated && rest < 0) || (maximizingNegated && rest > 0)) {
						//target can't be lowered
						decided = true;
						targetPoint.setCoord(maximizingCoord, Double.NaN);
						if (verbose)
							mainLog.println("Decided, target is " + targetPoint);
						break;
					} else {
						double lowered = rest / direction.getCoord(maximizingCoord);
						targetPoint.setCoord(maximizingCoord, lowered);
						//HACK
						if (lowered == Double.NEGATIVE_INFINITY) {
							targetPoint.setCoord(maximizingCoord, Double.NaN);
							mainLog.println("\nThe constraints are not achievable!\n");
							decided = true;
							isAchievable = false;
							break;
						}
						if (verbose)
							mainLog.println("Target lowered to " + targetPoint);

						//if (prism.getExportMultiGraphs())
						//	MultiObjUtils.printGraphFileDebug(targetPoint, computedPoints, computedDirections, prism.getExportMultiGraphsDir(), output++);
					}
				} else {
					decided = true;
					isAchievable = false;
					break;
				}
			}

			//compute new direction
			direction = MultiObjUtils.getWeights(targetPoint, computedPoints);

			if (verbose) {
				mainLog.println("New direction is " + direction);
			}

			if (direction == null || computedDirections.contains(direction)) //The second disjunct is for convergence
			{
				//there is no hyperplane strictly separating the target from computed points
				//hence we can conclude that the point is reachable
				decided = true;
				isAchievable = true;
				break;
			}
		}

		timer = System.currentTimeMillis() - timer;
		mainLog.println("The value iteration(s) took " + timer / 1000.0 + " seconds altogether.");
		mainLog.println("Number of weight vectors used: " + numberOfPoints);

		if (!decided)
			throw new PrismException("The computation did not finish in " + maxIters
					+ " target point iterations, try increasing this number using the -multimaxpoints switch.");
		if (maximizingProb || maximizingReward) {
			int maximizingCoord = (maximizingProb) ? 0 : dimProb;
			return (maximizingNegated) ? -targetPoint.getCoord(maximizingCoord) : targetPoint.getCoord(maximizingCoord);
		} else {
			return (isAchievable) ? 1.0 : 0.0;
		}
	}
}
