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

package symbolic.comp;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Vector;

import io.ModelExportOptions;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import mtbdd.PrismMTBDD;
import parser.ast.Expression;
import parser.ast.RelOp;
import prism.*;
import sparse.NDSparseMatrix;
import sparse.PrismSparse;
import acceptance.AcceptanceRabin;
import acceptance.AcceptanceRabinDD;
import automata.DA;
import automata.LTL2DA;
import dv.DoubleVector;
import symbolic.model.NondetModel;

/**
 * Multi-objective model checking for the symbolic (sparse) engine.
 * Handles product MDP construction, end component computation, and delegates
 * to the engine-agnostic iteration loops in {@link prism.MultiObjModelChecker}.
 */
public class MultiObjModelChecker extends prism.MultiObjModelChecker
{
	/** The Prism instance, needed for symbolic-engine-specific operations. */
	protected Prism prism;

	public MultiObjModelChecker(Prism prism) throws PrismException
	{
		super(prism);
		// Set up native model checking (we can't extend PrismNativeComponent, as we extend prism.MultiObjModelChecker)
		this.prism = prism;
		prism.useNative();
	}

	/**
	 * Construct the DRA for one LTL objective and build the product MDP.
	 * Note: {@code dra[i]} is set as a side-effect (mutation of the array element).
	 */
	protected NondetModel constructDRAandProductMulti(NondetModel model, LTLModelChecker mcLtl, ModelChecker modelChecker, int i,
	                                                   DA<BitSet, AcceptanceRabin>[] dra, Operator operator, Expression pathFormula, JDDVars draDDRowVars,
	                                                   JDDVars draDDColVars, JDDNode ddStateIndex) throws PrismException
	{
		// Model check maximal state formulas
		Vector<JDDNode> labelDDs = new Vector<>();
		Expression ltl = mcLtl.checkMaximalStateFormulas(modelChecker, model, pathFormula.deepCopy(), labelDDs);

		// Convert LTL formula to deterministic Rabin automaton (DRA).
		// For min probabilities, negate the formula.
		if (Operator.isMinOrLe(operator)) {
			ltl = Expression.Not(ltl);
		}
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

		mainLog.println("\nConstructing MDP-DRA product...");
		NondetModel modelNew = mcLtl.constructProductMDP(dra[i], model, labelDDs, draDDRowVars, draDDColVars,
		                                                  (i == 0 ? ddStateIndex : model.getStart()).copy());
		modelNew.printTransInfo(mainLog, prism.getExtraDDInfo());
		for (JDDNode labelDD : labelDDs) {
			JDD.Deref(labelDD);
		}
		return modelNew;
	}

	/**
	 * Remove transitions with non-zero reward for minimising/upper-bounded reward objectives.
	 * @return True if any transitions were removed
	 */
	protected boolean removeNonZeroRewardTrans(NondetModel modelProduct, List<JDDNode> rewardsIndex, MultiObjQuery moQuery)
	{
		boolean transchanged = false;
		for (int i = 0; i < rewardsIndex.size(); i++)
			if (moQuery.getRewardOperator(i) == Operator.R_MIN || moQuery.getRewardOperator(i) == Operator.R_LE) {
				JDD.Ref(rewardsIndex.get(i));
				JDDNode actions = JDD.GreaterThan(rewardsIndex.get(i), 0.0);
				if (!actions.equals(JDD.ZERO)) {
					if (!transchanged)
						JDD.Ref(modelProduct.getTrans());
					modelProduct.resetTrans(JDD.ITE(actions.copy(), JDD.Constant(0), modelProduct.getTrans().copy()));
					if (!transchanged)
						JDD.Ref(modelProduct.getTrans01());
					modelProduct.resetTrans01(JDD.ITE(actions, JDD.Constant(0), modelProduct.getTrans01().copy()));
					transchanged = true;
				}
			}
		return transchanged;
	}

	/**
	 * Find all MECs in the product MDP that are candidates for being accepting ECs across all probability objectives.
	 * Restricts the search to states reachable by transitions staying within the union of all NotL regions
	 * ({@code allStatesNotL}), then keeps only MECs that intersect the union of all InK sets
	 * ({@code allStatesInK}). The result is a coarse pre-filter; per-objective refinement is done
	 * by {@link #computeAcceptingEndComponent}.
	 *
	 * @param allStatesNotL Union of all {@code statesNotL} BDDs across all objectives and pairs
	 * @param allStatesInK  Union of all {@code statesInK} BDDs across all objectives and pairs
	 * @return List of referenced BDDs, one per MEC (caller must deref)
	 */
	protected List<JDDNode> computeCandidateMECs(NondetModel modelProduct, LTLModelChecker mcLtl,
												 JDDNode allStatesNotL, JDDNode allStatesInK,
												 JDDVars[] draDDRowVars, JDDVars[] draDDColVars,
												 MultiObjQuery moQuery)
	        throws PrismException
	{
		// Restrict candidate states to those with transitions entirely within the NotL region,
		// then find all MECs that intersect the InK (good) set.
		JDD.Ref(allStatesNotL);
		JDD.Ref(modelProduct.getTrans01());
		JDDNode candidateStates = JDD.Apply(JDD.TIMES, modelProduct.getTrans01(), allStatesNotL);
		int numTargets = moQuery.numObjectives();
		for (int i = 0; i < numTargets; i++)
			if (moQuery.isProbabilityObjective(i)) {
				allStatesNotL = JDD.PermuteVariables(allStatesNotL, draDDRowVars[i], draDDColVars[i]);
			}
		candidateStates = JDD.Apply(JDD.TIMES, candidateStates, allStatesNotL);
		candidateStates = JDD.ThereExists(candidateStates, modelProduct.getAllDDColVars());
		candidateStates = JDD.ThereExists(candidateStates, modelProduct.getAllDDNondetVars());
		// Find all maximal end components
		List<JDDNode> allecs = mcLtl.findMECStates(modelProduct, candidateStates, allStatesInK);
		JDD.Deref(candidateStates);
		JDD.Deref(allStatesInK);
		return allecs;
	}

	/**
	 * Find the union of states in accepting MECs for a single Rabin objective, using the pre-computed MEC list.
	 * Delegates to {@link LTLModelChecker#findMultiAcceptingStates}. When conflict resolution is active
	 * ({@code conflictformulaeGtOne}), the per-pair BDDs are ref-bumped so they survive being consumed
	 * by the conflict-checking pass that follows.
	 *
	 * @param allecs              Pre-filtered MEC list from {@link #computeCandidateMECs}
	 * @param statesNotL          Per-pair BDDs for states outside the Rabin L set for this objective
	 * @param statesInK           Per-pair BDDs for states inside the Rabin K set for this objective
	 * @param conflictformulaeGtOne True if conflict resolution will run after this call
	 * @return Referenced BDD of accepting EC states for this objective (caller must deref)
	 */
	protected JDDNode computeAcceptingEndComponent(DA<BitSet, AcceptanceRabin> dra, NondetModel modelProduct, JDDVars draDDRowVars, JDDVars draDDColVars,
	                                                List<JDDNode> allecs, List<JDDNode> statesNotL, List<JDDNode> statesInK, LTLModelChecker mcLtl,
	                                                boolean conflictformulaeGtOne) throws PrismException
	{
		long l = System.currentTimeMillis();
		if (conflictformulaeGtOne) {
			for (JDDNode n : statesNotL)
				JDD.Ref(n);
			for (JDDNode n : statesInK)
				JDD.Ref(n);
		}
		JDDNode ret = mcLtl.findMultiAcceptingStates(dra, modelProduct, draDDRowVars, draDDColVars, false, allecs, statesNotL, statesInK);
		l = System.currentTimeMillis() - l;
		mainLog.println("Time for end component identification: " + l / 1000.0 + " seconds.");
		return ret;
	}

	/**
	 * Remove actions inside MECs that carry positive reward under any maximising reward objective.
	 * Such MECs make the max-reward value infinite and are not supported. If any positive-reward MEC
	 * is reachable from the initial state (determined by a subsidiary multi-objective solve), an
	 * exception is thrown. Otherwise the offending actions are zeroed out in the product's transition relation.
	 *
	 * @param rewardsPreProduct Transition reward DDs restricted to the pre-product model, one per reward objective
	 * @param instance          Query and associated engine-specific data (moQuery, dra used here; rewards/targets not used)
	 */
	protected void removeNonZeroMecsForMax(NondetModel modelProduct, LTLModelChecker mcLtl, List<JDDNode> rewardsPreProduct,
	                                        MultiObjQueryInstance<JDDNode, JDDNode> instance, JDDVars[] draDDRowVars, JDDVars[] draDDColVars)
	        throws PrismException
	{
		List<JDDNode> mecs = mcLtl.findMECStates(modelProduct, modelProduct.getReach());
		JDDNode removedActions = JDD.Constant(0);
		JDDNode rmecs = JDD.Constant(0);
		for (int i = 0; i < rewardsPreProduct.size(); i++)
			if (instance.moQuery.getRewardOperator(i) == Operator.R_MAX || instance.moQuery.getRewardOperator(i) == Operator.R_GE) {
				JDD.Ref(rewardsPreProduct.get(i));
				JDDNode actions = JDD.GreaterThan(rewardsPreProduct.get(i), 0.0);
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
				// Find all actions pointing to MECs from outside a
				JDD.Ref(rmecs);
				JDDNode rtarget = JDD.PermuteVariables(rmecs, modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars());
				JDD.Ref(modelProduct.getTrans01());
				rtarget = JDD.And(modelProduct.getTrans01(), rtarget);
				rtarget = JDD.And(rtarget, JDD.Not(rmecs));
				// Find target states for LTL formulae
				List<JDDNode> tmptargetDDs = new ArrayList<>();
				List<JDDNode> tmpmultitargetDDs = new ArrayList<>();
				List<Integer> tmpmultitargetIDs = new ArrayList<>();
				ArrayList<DA<BitSet, AcceptanceRabin>> tmpdra = new ArrayList<>();
				ArrayList<JDDVars> tmpdraDDRowVars = new ArrayList<>();
				ArrayList<JDDVars> tmpdraDDColVars = new ArrayList<>();
				int count = 0;
				for (int i = 0; i < instance.numObjectives(); i++)
					if (instance.moQuery.isProbabilityObjective(i) && instance.moQuery.getOperator(i) != Operator.P_MAX
					        && instance.moQuery.getOperator(i) != Operator.P_MIN) {
						tmpdra.add(instance.dra[i]);
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

					MultiObjQuery tmpMoQuery = new MultiObjQuery();
					for (int i = 0; i < instance.moQuery.numProbObjectives(); i++) {
						if (instance.moQuery.getProbOperator(i) != Operator.P_MAX) {
							tmpMoQuery.add(instance.moQuery.getOpRelOpBound(i), instance.moQuery.getProbOperator(i), instance.moQuery.getProbBound(i),
							               instance.moQuery.getProbStepBound(i), i, null);
						}
					}
					tmpMoQuery.add(new OpRelOpBound("R", RelOp.MAX, -1.0), Operator.R_MAX, -1.0, -1, instance.moQuery.numProbObjectives(), null);

					ArrayList<JDDNode> tmprewards = new ArrayList<>(1);
					tmprewards.add(rtarget);
					MultiObjQueryInstance<JDDNode, JDDNode> tmpInstance = new MultiObjQueryInstance<>(tmpMoQuery, newdra, tmprewards);
					tmpInstance.targets = tmptargetDDs;
					tmpInstance.combinations = tmpmultitargetDDs;
					tmpInstance.combinationIDs = tmpmultitargetIDs;
					tmpInstance.conflictCount = count;
					double prob = (Double) computeMultiObjective(modelProduct, mcLtl, modelProduct.getStart(), tmpInstance);
					if (prob > 0.0) {
						constraintViolated = true;
					} else if (Double.isNaN(prob))
						throw new PrismException("The LTL formulae in multi-objective query cannot be satisfied!\n");
				} else {
					// end components with non-zero rewards can always be reached
					constraintViolated = true;
				}

				for (JDDNode tt : tmptargetDDs)
					JDD.Deref(tt);
				for (JDDNode tt : tmpmultitargetDDs)
					JDD.Deref(tt);
			}
			if (constraintViolated) {
				throw new PrismNotSupportedException("Cannot use multi-objective model checking with maximising objectives and non-zero reward end compoments");
			}

			JDD.Ref(removedActions);
			modelProduct.resetTrans(JDD.Apply(JDD.TIMES, modelProduct.getTrans().copy(), JDD.Not(removedActions)));
			modelProduct.resetTrans01(JDD.Apply(JDD.TIMES, modelProduct.getTrans01().copy(), JDD.Not(removedActions)));
		} else {
			JDD.Deref(rmecs);
			JDD.Deref(removedActions);
		}
	}

	/**
	 * Find the reward objectives, among the minimising ones (R_MIN/R_LE), that are infinite from
	 * the initial state under every strategy (see {@link #computeInfiniteRewardStates}).
	 *
	 * @param rewards Transition reward DDs, one per reward objective
	 * @return Indices (in {@code rewards}) of such objectives
	 */
	protected BitSet findInfiniteMinRewardObjectives(NondetModel modelProduct, LTLModelChecker mcLtl, List<JDDNode> rewards, MultiObjQuery moQuery)
	        throws PrismException
	{
		BitSet infinite = new BitSet();
		for (int i = 0; i < rewards.size(); i++) {
			if (moQuery.getRewardOperator(i) == Operator.R_MIN || moQuery.getRewardOperator(i) == Operator.R_LE) {
				JDDNode inf = computeInfiniteRewardStates(modelProduct, mcLtl, rewards.get(i));
				if (JDD.AreIntersecting(modelProduct.getStart(), inf)) {
					infinite.set(i);
				}
				JDD.Deref(inf);
			}
		}
		return infinite;
	}

	/**
	 * Remove all choices that can lead to a state from which some minimising reward objective
	 * (R_MIN/R_LE) is infinite under every strategy (see {@link #computeInfiniteRewardStates}).
	 * This is repeated until nothing changes, since removing choices for one objective can make
	 * more states infinite for another. Afterwards, from every state that still has choices,
	 * all minimising reward objectives have finite values, as value iteration needs in order
	 * to converge; the states that do not are left with no choices.
	 *
	 * @param rewards Transition reward DDs, one per reward objective
	 * @return True if the initial state has no strategy under which all minimising reward
	 *         objectives are finite (in which case all its choices have been removed)
	 */
	protected boolean removeInfiniteRewardChoicesForMin(NondetModel modelProduct, LTLModelChecker mcLtl, List<JDDNode> rewards, MultiObjQuery moQuery)
	        throws PrismException
	{
		boolean startInfinite = false;
		boolean changed = true;
		while (changed) {
			changed = false;
			for (int i = 0; i < rewards.size(); i++) {
				if (moQuery.getRewardOperator(i) != Operator.R_MIN && moQuery.getRewardOperator(i) != Operator.R_LE) {
					continue;
				}
				JDDNode inf = computeInfiniteRewardStates(modelProduct, mcLtl, rewards.get(i));
				if (JDD.AreIntersecting(modelProduct.getStart(), inf)) {
					startInfinite = true;
				}
				// Choices with a successor in inf
				JDDNode removed = JDD.PermuteVariables(inf, modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars());
				removed = JDD.And(modelProduct.getTrans01().copy(), removed);
				removed = JDD.ThereExists(removed, modelProduct.getAllDDColVars());
				if (!removed.equals(JDD.ZERO)) {
					modelProduct.resetTrans(JDD.Apply(JDD.TIMES, modelProduct.getTrans().copy(), JDD.Not(removed.copy())));
					modelProduct.resetTrans01(JDD.Apply(JDD.TIMES, modelProduct.getTrans01().copy(), JDD.Not(removed.copy())));
					changed = true;
				}
				JDD.Deref(removed);
			}
		}
		return startInfinite;
	}

	/**
	 * Compute the states from which a minimising reward objective is infinite under every
	 * strategy. The reward is finite from a state only if some strategy almost surely reaches an
	 * end component in which it can stay forever with zero reward, as for single-objective
	 * minimum total rewards.
	 *
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 * @param reward Transition reward DD for the objective
	 */
	private JDDNode computeInfiniteRewardStates(NondetModel model, LTLModelChecker mcLtl, JDDNode reward) throws PrismException
	{
		// Zero-reward end components: the MECs once all choices with positive reward are removed
		JDDNode positive = JDD.ThereExists(JDD.GreaterThan(reward.copy(), 0.0), model.getAllDDColVars());
		JDDNode origTrans = model.getTrans().copy();
		JDDNode origTrans01 = model.getTrans01().copy();
		model.resetTrans(JDD.ITE(positive.copy(), JDD.Constant(0), origTrans.copy()));
		model.resetTrans01(JDD.ITE(positive, JDD.Constant(0), origTrans01.copy()));
		List<JDDNode> zeroMecs;
		try {
			zeroMecs = mcLtl.findMECStates(model, model.getReach());
		} finally {
			model.resetTrans(origTrans);
			model.resetTrans01(origTrans01);
		}
		JDDNode zeroEcStates = JDD.Constant(0);
		for (JDDNode mec : zeroMecs) {
			zeroEcStates = JDD.Or(zeroEcStates, mec);
		}
		// Finite: states from which some strategy reaches them with probability 1
		JDDNode no = PrismMTBDD.Prob0A(model.getTrans01(), model.getReach(), model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars(),
		                               model.getReach(), zeroEcStates);
		JDDNode finite = PrismMTBDD.Prob1E(model.getTrans01(), model.getReach(), model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars(),
		                                   model.getReach(), zeroEcStates, no);
		JDD.Deref(no);
		JDD.Deref(zeroEcStates);
		return JDD.And(model.getReach().copy(), JDD.Not(finite));
	}

	/**
	 * Identify ECs that simultaneously satisfy multiple probability objectives (conflicting objectives)
	 * and produce combined target sets for use in the solver.
	 * Delegates to {@link LTLModelChecker#findMultiConflictAcceptingStates}.
	 * On return, {@code instance.targets} is updated with refined per-objective target sets (with
	 * conflict states subtracted), and {@code instance.combinations}/{@code instance.combinationIDs}
	 * are populated with the combined EC sets and their objective bitmasks.
	 * Also derefs the per-pair acceptance BDDs in {@code allStatesNotL}/{@code allStatesInK}.
	 *
	 * @param instance     Query instance ({@code moQuery}, {@code dra}, {@code targets} used/updated;
	 *                     {@code combinations}, {@code combinationIDs} set as output)
	 */
	protected void checkConflictsInObjectives(NondetModel modelProduct, LTLModelChecker mcLtl,
	                                           MultiObjQueryInstance<JDDNode, JDDNode> instance,
	                                           JDDVars[] draDDRowVars, JDDVars[] draDDColVars,
	                                           List<ArrayList<JDDNode>> allStatesNotL, List<ArrayList<JDDNode>> allStatesInK) throws PrismException
	{
		DA<BitSet, AcceptanceRabin>[] tmpdra = new DA[instance.conflictCount];
		JDDVars[] tmpdraDDRowVars = new JDDVars[instance.conflictCount];
		JDDVars[] tmpdraDDColVars = new JDDVars[instance.conflictCount];
		List<JDDNode> tmptargetDDs = new ArrayList<>(instance.conflictCount);
		List<List<JDDNode>> tmpAllStatesNotL = new ArrayList<>(instance.conflictCount);
		List<List<JDDNode>> tmpAllStatesInK = new ArrayList<>(instance.conflictCount);
		int count = 0;
		for (int i = 0; i < instance.numObjectives(); i++)
			if (instance.moQuery.isProbabilityObjective(i)) {
				tmpdra[count] = instance.dra[i];
				tmpdraDDRowVars[count] = draDDRowVars[i];
				tmpdraDDColVars[count] = draDDColVars[i];
				tmptargetDDs.add(instance.targets.get(count));
				tmpAllStatesNotL.add(allStatesNotL.get(i));
				tmpAllStatesInK.add(allStatesInK.get(i));
				count++;
			}
		List<List<Integer>> tmpmultitargetIDs = new ArrayList<>();

		instance.combinations = new ArrayList<>();
		mcLtl.findMultiConflictAcceptingStates(tmpdra, modelProduct, tmpdraDDRowVars, tmpdraDDColVars, tmptargetDDs, tmpAllStatesNotL, tmpAllStatesInK,
		                                        instance.combinations, tmpmultitargetIDs);
		count = 0;
		for (int i = 0; i < instance.numObjectives(); i++)
			if (instance.moQuery.isProbabilityObjective(i)) {
				instance.targets.remove(count);
				instance.targets.add(count, tmptargetDDs.get(count));
				count++;
			}

		instance.combinationIDs = new ArrayList<>();
		for (int i = 0; i < tmpmultitargetIDs.size(); i++) {
			instance.combinationIDs.add(changeToInteger(tmpmultitargetIDs.get(i)));
		}

		for (int i = 0; i < instance.numObjectives(); i++)
			if (instance.moQuery.isProbabilityObjective(i)) {
				for (JDDNode n : allStatesNotL.get(i))
					JDD.Deref(n);
				for (JDDNode n : allStatesInK.get(i))
					JDD.Deref(n);
			}
	}

	/**
	 * Compute accepting EC target states for all probability objectives and, when objectives conflict,
	 * also compute combined target sets. Called internally by {@link #removeNonZeroMecsForMax}.
	 *
	 * @param numTargets          Total number of objectives
	 * @param numConflictFormulas Number of probability objectives that may conflict with each other
	 * @param reachExpr           Per-objective flag: true if the objective uses a simple reachability
	 *                            formula (no EC computation needed)
	 * @param targetDDs           Output: per-objective BDDs of accepting EC states
	 * @param multitargetDDs      Output: BDDs for ECs satisfying multiple objectives simultaneously
	 * @param multitargetIDs      Output: bitmask per entry in {@code multitargetDDs}
	 */
	protected void findTargetStates(NondetModel modelProduct, LTLModelChecker mcLtl, int numTargets, int numConflictFormulas, boolean reachExpr[],
	                                 DA<BitSet, AcceptanceRabin> dra[], JDDVars draDDRowVars[], JDDVars draDDColVars[], List<JDDNode> targetDDs,
	                                 List<JDDNode> multitargetDDs, List<Integer> multitargetIDs) throws PrismException
	{
		int i, j;
		long l;

		// Build per-Rabin-pair acceptance BDDs: statesNotL[k] = states NOT in L_k (forbidden),
		// statesInK[k] = states IN K_k (good/accepting set, must be visited infinitely often).
		ArrayList<ArrayList<JDDNode>> allStatesNotL = new ArrayList<>(numTargets);
		ArrayList<ArrayList<JDDNode>> allStatesInK = new ArrayList<>(numTargets);
		JDDNode acceptanceVectorNotL = JDD.Constant(0);
		JDDNode acceptanceVectorInK = JDD.Constant(0);
		for (i = 0; i < numTargets; i++) {
			if (!reachExpr[i]) {
				ArrayList<JDDNode> statesNotL = new ArrayList<>();
				ArrayList<JDDNode> statesInK = new ArrayList<>();
				AcceptanceRabinDD acc = dra[i].getAcceptance().toAcceptanceDD(draDDRowVars[i]);
				for (AcceptanceRabinDD.RabinPairDD pair : acc) {
					JDDNode tmpNotL = JDD.Not(pair.getL());
					JDDNode tmpInK = pair.getK();
					statesNotL.add(tmpNotL);
					JDD.Ref(tmpNotL);
					acceptanceVectorNotL = JDD.Or(acceptanceVectorNotL, tmpNotL);
					statesInK.add(tmpInK);
					JDD.Ref(tmpInK);
					acceptanceVectorInK = JDD.Or(acceptanceVectorInK, tmpInK);
				}
				acc.clear();
				allStatesNotL.add(i, statesNotL);
				allStatesInK.add(i, statesInK);
			} else {
				allStatesNotL.add(i, null);
				allStatesInK.add(i, null);
			}
		}

		JDD.Ref(acceptanceVectorNotL);
		JDD.Ref(modelProduct.getTrans01());
		JDDNode candidateStates = JDD.Apply(JDD.TIMES, modelProduct.getTrans01(), acceptanceVectorNotL);
		for (i = 0; i < numTargets; i++)
			if (!reachExpr[i]) {
				acceptanceVectorNotL = JDD.PermuteVariables(acceptanceVectorNotL, draDDRowVars[i], draDDColVars[i]);
			}
		candidateStates = JDD.Apply(JDD.TIMES, candidateStates, acceptanceVectorNotL);
		candidateStates = JDD.ThereExists(candidateStates, modelProduct.getAllDDColVars());
		candidateStates = JDD.ThereExists(candidateStates, modelProduct.getAllDDNondetVars());
		List<JDDNode> allecs = mcLtl.findMECStates(modelProduct, candidateStates, acceptanceVectorInK);
		JDD.Deref(candidateStates);
		JDD.Deref(acceptanceVectorInK);

		for (i = 0; i < numTargets; i++) {
			if (!reachExpr[i]) {
				l = System.currentTimeMillis();
				if (numConflictFormulas > 1) {
					for (JDDNode n : allStatesNotL.get(i))
						JDD.Ref(n);
					for (JDDNode n : allStatesInK.get(i))
						JDD.Ref(n);
				}
				targetDDs.add(mcLtl.findMultiAcceptingStates(dra[i], modelProduct, draDDRowVars[i], draDDColVars[i], false, allecs, allStatesNotL.get(i),
				                                              allStatesInK.get(i)));
				l = System.currentTimeMillis() - l;
				mainLog.println("Time for end component identification: " + l / 1000.0 + " seconds.");
			}
		}

		if (numConflictFormulas > 1) {
			DA<BitSet, AcceptanceRabin>[] tmpdra = new DA[numConflictFormulas];
			JDDVars[] tmpdraDDRowVars = new JDDVars[numConflictFormulas];
			JDDVars[] tmpdraDDColVars = new JDDVars[numConflictFormulas];
			List<JDDNode> tmptargetDDs = new ArrayList<>(numConflictFormulas);
			List<List<JDDNode>> tmpAllStatesNotL = new ArrayList<>(numConflictFormulas);
			List<List<JDDNode>> tmpAllStatesInK = new ArrayList<>(numConflictFormulas);
			int count = 0;
			for (i = 0; i < numTargets; i++)
				if (!reachExpr[i]) {
					tmpdra[count] = dra[i];
					tmpdraDDRowVars[count] = draDDRowVars[i];
					tmpdraDDColVars[count] = draDDColVars[i];
					tmptargetDDs.add(targetDDs.get(count));
					tmpAllStatesNotL.add(allStatesNotL.get(i));
					tmpAllStatesInK.add(allStatesInK.get(i));
					count++;
				}
			List<List<Integer>> tmpmultitargetIDs = new ArrayList<>();

			mcLtl.findMultiConflictAcceptingStates(tmpdra, modelProduct, tmpdraDDRowVars, tmpdraDDColVars, tmptargetDDs, tmpAllStatesNotL, tmpAllStatesInK,
			                                        multitargetDDs, tmpmultitargetIDs);
			count = 0;
			for (i = 0; i < numTargets; i++)
				if (!reachExpr[i]) {
					targetDDs.remove(count);
					targetDDs.add(count, tmptargetDDs.get(count));
					count++;
				}

			for (i = 0; i < tmpmultitargetIDs.size(); i++) {
				multitargetIDs.add(changeToInteger(tmpmultitargetIDs.get(i)));
			}

			for (i = 0; i < numTargets; i++)
				if (!reachExpr[i]) {
					for (JDDNode n : allStatesNotL.get(i))
						JDD.Deref(n);
					for (JDDNode n : allStatesInK.get(i))
						JDD.Deref(n);
				}
		}

		for (JDDNode ec : allecs)
			JDD.Deref(ec);
	}

	/**
	 * Encodes a list of objective indices as a bitmask integer.
	 * Index i sets bit i (i.e. the result has bit i set if i is in {@code ids}).
	 */
	private int changeToInteger(List<Integer> ids)
	{
		int k = 0;
		for (int i = 0; i < ids.size(); i++) {
			int bit = 1;
			if (ids.get(i) > 0)
				bit = bit << ids.get(i);
			k += bit;
		}
		return k;
	}

	/**
	 * Perform multi-objective model checking computation.
	 * Solves achievability, numerical or Pareto queries over n objectives.
	 * Dispatches to LP or value-iteration solvers depending on settings.
	 *
	 * <p>Takes ownership of {@code instance.rewards}: all entries are deref'd in the finally block.
	 *
	 * @param model    The product MDP (after LTL-to-DRA product construction)
	 * @param mcLtl    LTL model checker, used for MEC identification
	 * @param start    BDD for the initial state of the product MDP
	 * @param instance Bundles the query and all engine-specific data:
	 *                 {@code rewards} (transition reward DDs, owned by this method),
	 *                 {@code targets} (accepting-EC BDDs, refs owned by caller),
	 *                 {@code combinations}/{@code combinationIDs} (conflict handling, owned by caller)
	 * @return For Pareto queries: a {@link TileList} under-approximation of the Pareto front.
	 *         For achievability queries: {@code true}/{@code false} boxed as {@link Boolean}.
	 *         For numerical queries: the optimal value as {@link Double}.
	 */
	protected Object computeMultiObjective(NondetModel model, LTLModelChecker mcLtl, JDDNode start,
	                                        MultiObjQueryInstance<JDDNode, JDDNode> instance) throws PrismException
	{
		Object value;
		int numTargets = instance.targets.size();

		JDDNode labels[] = new JDDNode[numTargets];
		// Build combined targets: per-objective union of target + any compatible combination ECs
		for (int i = 0; i < numTargets; i++) {
			JDD.Ref(instance.targets.get(i));
			JDDNode tmp = instance.targets.get(i);
			if (instance.combinations != null) {
				for (int j = 0; j < instance.combinations.size(); j++) {
					if ((instance.combinationIDs.get(j) & (1 << i)) > 0) {
						JDD.Ref(instance.combinations.get(j));
						tmp = JDD.Or(tmp, instance.combinations.get(j));
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
			for (int i = 0; i < numTargets; i++) {
				labels2[i + 1] = labels[i];
				labelNames[i + 1] = "target" + i;
			}
			try {
				mainLog.print("\nExporting target states info to file \"" + prism.getExportTargetFilename() + "\"...");
				PrismMTBDD.ExportLabels(labels2, labelNames, "l", model.getAllDDRowVars(), model.getODD(), Prism.EXPORT_PLAIN, prism.getExportTargetFilename(), false, new ModelExportOptions().getPrintHeaders() ? "# Labels\n" : null);
			} catch (FileNotFoundException e) {
				mainLog.println("\nWarning: Could not export target to file \"" + prism.getExportTargetFilename() + "\"");
			}
		}

		mainLog.println("\nComputing remaining probabilities...");

		int engine = settings.getChoice(PrismSettings.PRISM_ENGINE);
		int method = prism.getMDPMultiSolnMethod();

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
			if (method == Prism.MDP_MULTI_LP && instance.moQuery.numberOfNumerical() > 1) {
				throw new PrismNotSupportedException("Pareto curve generation is not currently supported using linear programming");
			}

			// Do computation
			// Linear programming
			if (method == Prism.MDP_MULTI_LP) {
				value = computeMultiObjectiveLP(model, mcLtl, start, instance);
			}
			// Value iteration
			else if (method == Prism.MDP_MULTI_GAUSSSEIDEL || method == Prism.MDP_MULTI_VALITER) {
				double timePre = System.currentTimeMillis();
				value = computeMultiObjectiveValIter(model, start, labels, instance);
				double timePost = System.currentTimeMillis();
				mainLog.println("Multi-objective value iterations took " + ((timePost - timePre) / 1000.0) + " s.");
			}
			// Unknown method (shouldn't happen)
			else {
				throw new PrismException("Unknown multi-objective model checking method");
			}
		} finally {
			for (int i = 0; i < labels.length; i++)
				JDD.Deref(labels[i]);
			for (int i = 0; i < instance.rewards.size(); i++) {
				JDD.Deref(instance.rewards.get(i));
			}
		}

		return value;
	}

	/**
	 * Perform multi-objective model checking computation with linear programming.
	 * Solves achievability or numerical queries over n objectives.
	 * Computes yes/no/maybe state sets from the product model and target DDs, then delegates
	 * to the native sparse solver.
	 *
	 * @param model    The product MDP
	 * @param mcLtl    LTL model checker (used for MEC computation when reward objectives are present)
	 * @param start    BDD for the initial state of the product MDP
	 * @param instance Bundles the query and all engine-specific data (refs owned by caller except rewards)
	 * @return achievability/numerical result
	 * @throws PrismException if computation fails or options are unsupported
	 */
	protected Object computeMultiObjectiveLP(NondetModel model, LTLModelChecker mcLtl, JDDNode start,
	                                          MultiObjQueryInstance<JDDNode, JDDNode> instance) throws PrismException
	{
		if (instance.moQuery.numberOfStepBounded() > 0) {
			throw new PrismNotSupportedException("Step-bounded objectives are not currently supported with linear programming");
		}

		// Compute yes: union of all target and combination states
		JDDNode yes = JDD.Constant(0);
		for (int i = 0; i < instance.targets.size(); i++) {
			JDD.Ref(instance.targets.get(i));
			yes = JDD.Or(yes, instance.targets.get(i));
		}
		if (instance.combinations != null) {
			for (int i = 0; i < instance.combinations.size(); i++) {
				JDD.Ref(instance.combinations.get(i));
				yes = JDD.Or(yes, instance.combinations.get(i));
			}
		}

		// Compute no: states that cannot reach yes.
		// When rewards are present, also compute bottomec: MEC states unreachable from yes,
		// used as explicit absorbing sinks in the LP formulation.
		JDDNode no;
		JDDNode bottomec = null;
		if (instance.moQuery.numRewardObjectives() == 0) {
			no = PrismMTBDD.Prob0A(model.getTrans01(), model.getReach(), model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars(),
					model.getReach(), yes);
		} else {
			no = JDD.Constant(0);
			bottomec = PrismMTBDD.Prob0A(model.getTrans01(), model.getReach(), model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars(),
			                             model.getReach(), yes);
			// Only ECs that can be stayed in forever with zero reward for all minimising objectives
			// can act as sinks: staying in one with positive reward accumulates infinite reward
			JDDNode origTrans = model.getTrans();
			JDDNode origTrans01 = model.getTrans01();
			boolean transChanged = removeNonZeroRewardTrans(model, instance.rewards, instance.moQuery);
			List<JDDNode> becs = mcLtl.findMECStates(model, bottomec);
			if (transChanged) {
				model.resetTrans(origTrans);
				model.resetTrans01(origTrans01);
			}
			JDD.Deref(bottomec);
			bottomec = JDD.Constant(0);
			for (JDDNode ec : becs)
				bottomec = JDD.Or(bottomec, ec);
		}

		JDD.Ref(model.getReach());
		JDD.Ref(yes);
		JDD.Ref(no);
		JDDNode maybe = JDD.And(model.getReach(), JDD.Not(JDD.Or(yes, no)));

		mainLog.print("\nyes = " + JDD.GetNumMintermsString(yes, model.getAllDDRowVars().n()));
		mainLog.print(", no = " + JDD.GetNumMintermsString(no, model.getAllDDRowVars().n()));
		mainLog.print(", maybe = " + JDD.GetNumMintermsString(maybe, model.getAllDDRowVars().n()) + "\n");

		Object value;
		try {
			if (instance.moQuery.numRewardObjectives() > 0) {
				if (instance.hasConflicts()) {
					value = PrismSparse.NondetMultiReachReward1(model.getTrans(), model.getTransActions(), model.getSynchs(), model.getODD(),
							model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars(), instance.targets,
							instance.combinations, instance.combinationIDs, instance.moQuery, maybe, start, instance.rewards, bottomec);
				} else {
					value = PrismSparse.NondetMultiReachReward(model.getTrans(), model.getTransActions(), model.getSynchs(), model.getODD(),
							model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars(), instance.targets,
							instance.moQuery, maybe, start, instance.rewards, bottomec);
				}
			} else {
				if (instance.hasConflicts()) {
					value = PrismSparse.NondetMultiReach1(model.getTrans(), model.getTransActions(), model.getSynchs(), model.getODD(),
							model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars(), instance.targets,
							instance.combinations, instance.combinationIDs, instance.moQuery, maybe, start);
				} else {
					value = PrismSparse.NondetMultiReach(model.getTrans(), model.getTransActions(), model.getSynchs(), model.getODD(),
							model.getAllDDRowVars(), model.getAllDDColVars(), model.getAllDDNondetVars(), instance.targets,
							instance.moQuery, maybe, start);
				}
			}
		} finally {
			JDD.Deref(yes);
			JDD.Deref(no);
			JDD.Deref(maybe);
			if (instance.moQuery.numRewardObjectives() > 0)
				JDD.Deref(bottomec);
		}
		return value;
	}

	/**
	 * Perform multi-objective model checking using value iteration.
	 * Dispatches to {@link #generateParetoCurve} (2 numerical objectives) or
	 * {@link #solveAchievabilityOrNumerical} (all other cases).
	 *
	 * <p><b>Convention on entry:</b> probabilistic operators have already been canonicalised to
	 * P_MAX / P_GE by {@link MultiObjQuery#makeAllProbUp()}, and minimising prob DDs have been
	 * built for the negated formula. Reward operators are still raw (R_MAX, R_MIN, R_GE, R_LE).
	 *
	 * <p>This method negates minimising reward DDs in place and canonicalises reward operators
	 * to R_MAX / R_GE via {@link MultiObjQuery#makeAllRewardUp()} before dispatching, so
	 * all sub-calls operate under the all-maximising convention described by
	 * {@link #runParetoCurveIteration} and {@link #runAchievabilityIteration}.
	 *
	 * @param modelProduct  The product MDP to solve
	 * @param start         BDD for the initial state
	 * @param labels        Combined accepting-EC BDDs (target ∪ compatible combinations), one per probability objective
	 * @param instance      Bundles the query and engine-specific data:
	 *                      {@code moQuery} (mutated: reward operators canonicalised to R_MAX/R_GE),
	 *                      {@code rewards} (mutated: minimising reward DDs are negated in place)
	 */
	protected Object computeMultiObjectiveValIter(NondetModel modelProduct, JDDNode start, JDDNode[] labels,
	                                               MultiObjQueryInstance<JDDNode, JDDNode> instance) throws PrismException
	{
		// Check for unsupported computations
		int numNumericalObjectives = instance.moQuery.numberOfNumerical();
		if (numNumericalObjectives > 2) {
			throw new PrismException("Pareto curve generation is currently only supported for 2 objectives");
		}
		if (numNumericalObjectives >= 2 && instance.moQuery.numProbObjectives() + instance.moQuery.numRewardObjectives() > numNumericalObjectives) {
			throw new PrismException("Pareto curve generation is currently not allowed if there are other (bounded) objectives");
		}

		// Convert minimising reward DDs to maximising by negation, then canonicalise
		// moQuery to use only R_MAX/R_GE. The LP path does not go through here;
		// it handles sign conventions internally via the native solver.
		for (int i = 0; i < instance.moQuery.numRewardObjectives(); i++) {
			if (instance.moQuery.getRewardOperator(i) == Operator.R_LE || instance.moQuery.getRewardOperator(i) == Operator.R_MIN) {
				instance.rewards.set(i, JDD.Apply(JDD.TIMES, JDD.Constant(-1), instance.rewards.get(i)));
			}
		}
		instance.moQuery.makeAllRewardUp();

		// Pareto computation or achievability/numerical computation
		if (numNumericalObjectives >= 2) {
			return generateParetoCurve(modelProduct, start, labels, instance.rewards, instance.moQuery);
		} else {
			return solveAchievabilityOrNumerical(modelProduct, start, labels, instance.rewards, instance.moQuery);
		}
	}

	/**
	 * Generate a Pareto curve under-approximation for exactly 2 numerical objectives.
	 * Builds a sparse weighted-sum solver via {@link #buildSparseWeightedSolver}, then
	 * delegates the iteration loop to {@link #runParetoCurveIteration} in the base class.
	 *
	 * <p><b>Pre-conditions (all-maximising convention):</b>
	 * <ul>
	 *   <li>Probabilistic operators are P_MAX / P_GE (canonicalised by
	 *       {@link MultiObjQuery#makeAllProbUp()}); minimising prob DDs were built for ¬φ.
	 *   <li>Reward operators are R_MAX / R_GE (canonicalised by
	 *       {@link MultiObjQuery#makeAllRewardUp()}); minimising reward DDs have been negated.
	 * </ul>
	 *
	 * @param modelProduct The product MDP to solve
	 * @param start        BDD for a single initial state
	 * @param targets      Accepting EC state BDDs, one per probability objective
	 * @param rewards      Transition reward DDs, one per reward objective (already negated for R_LE/R_MIN)
	 * @param moQuery      Canonicalised objective operators/bounds (P_MAX/P_GE, R_MAX/R_GE)
	 */
	protected TileList generateParetoCurve(NondetModel modelProduct, final JDDNode start, JDDNode[] targets,
	                                        List<JDDNode> rewards, MultiObjQuery moQuery) throws PrismException
	{
		WeightedObjectiveSolver singleSolve = buildSparseWeightedSolver(modelProduct, start, targets, rewards, moQuery);
		double tolerance = settings.getDouble(PrismSettings.PRISM_PARETO_EPSILON);
		int maxIters = settings.getInteger(PrismSettings.PRISM_MULTI_MAX_POINTS);

		// Build axis-direction extreme points (one per objective) to seed the TileList
		List<Point> pointsForInitialTile = buildAxisInitialPoints(singleSolve, targets.length, rewards.size());
		if (verbose) {
			mainLog.println("Points for the initial tile: " + pointsForInitialTile);
		}

		// Delegate the main iteration loop to the engine-agnostic base class
		return runParetoCurveIteration(singleSolve, moQuery, pointsForInitialTile, tolerance, maxIters);
	}

	/**
	 * Achievability or numerical query computation.
	 * Builds a sparse weighted-sum solver via {@link #buildSparseWeightedSolver}, then
	 * delegates the iteration loop to {@link #runAchievabilityIteration} in the base class.
	 *
	 * <p><b>Pre-conditions (all-maximising convention):</b>
	 * <ul>
	 *   <li>Probabilistic operators are P_MAX / P_GE (canonicalised by
	 *       {@link MultiObjQuery#makeAllProbUp()}); minimising prob DDs were built for ¬φ.
	 *   <li>Reward operators are R_MAX / R_GE (canonicalised by
	 *       {@link MultiObjQuery#makeAllRewardUp()}); minimising reward DDs have been negated.
	 * </ul>
	 *
	 * <p>The returned value is in user-space for reward objectives: this method negates the
	 * {@link #runAchievabilityIteration} result for objectives where
	 * {@link MultiObjQuery#isRewardNegated} is true, converting solver-space (max of −reward)
	 * back to user-space (min reward). For a P_MIN numerical query the returned value
	 * is max P(¬φ); the caller must apply the 1−value correction.
	 *
	 * @param modelProduct The product MDP to solve
	 * @param start        BDD for a single initial state
	 * @param targets      Accepting EC state BDDs, one per probability objective
	 * @param rewards      Transition reward DDs, one per reward objective (already negated for R_LE/R_MIN)
	 * @param moQuery      Canonicalised objective operators/bounds (P_MAX/P_GE, R_MAX/R_GE)
	 */
	protected double solveAchievabilityOrNumerical(NondetModel modelProduct, final JDDNode start, JDDNode[] targets,
	                                               List<JDDNode> rewards, MultiObjQuery moQuery) throws PrismException
	{
		WeightedObjectiveSolver solver = buildSparseWeightedSolver(modelProduct, start, targets, rewards, moQuery);
		int maxIters = settings.getInteger(PrismSettings.PRISM_MULTI_MAX_POINTS);
		double result = runAchievabilityIteration(solver, moQuery, maxIters);
		// Convert solver-space result to user-space: negate if the numerical objective is a
		// reward that was originally R_MIN (the solver maximised −reward, so result = −user-space
		// value). Numerical objectives come first, so a numerical reward is always reward 0;
		// an R_LE bound is also negated but is never the numerical objective.
		if (moQuery.numRewardObjectives() > 0 && moQuery.getRewardOperator(0) == Operator.R_MAX && moQuery.isRewardNegated(0)) {
			result = -result;
		}
		return result;
	}

	/**
	 * Build a {@link WeightedObjectiveSolver} backed by sparse data structures for the given product MDP.
	 *
	 * <p>Performs all one-time setup: selects GS vs VI, reads adversary-export settings,
	 * builds the sparse transition and reward matrices, and returns a solver lambda that
	 * closes over those structures.
	 *
	 * <p>Callers must ensure that reward DDs have already been negated for minimising objectives
	 * and that {@code moQuery} has been canonicalised to R_MAX/R_GE via
	 * {@link MultiObjQuery#makeAllRewardUp()} before calling this method.
	 *
	 * @param modelProduct The product MDP to solve
	 * @param start BDD for a single initial state
	 * @param targets BDD target sets for each probability objective
	 * @param rewards Transition-reward BDDs for each reward objective (already negated for R_LE/R_MIN)
	 * @param moQuery Objective operators and bounds (already canonicalised to R_MAX/R_GE)
	 * @return A configured solver ready for repeated weighted-sum queries
	 */
	private WeightedObjectiveSolver buildSparseWeightedSolver(NondetModel modelProduct, final JDDNode start,
	                                                          JDDNode[] targets, List<JDDNode> rewards,
	                                                          MultiObjQuery moQuery) throws PrismException
	{
		int[] rewardStepBounds = moQuery.getRewardStepBounds();
		int[] probStepBounds = moQuery.getProbStepBounds();

		boolean useGS = (settings.getChoice(PrismSettings.PRISM_MDP_SOLN_METHOD) == Prism.MDP_MULTI_GAUSSSEIDEL);
		if (moQuery.numberOfStepBounded() > 0) {
			mainLog.println("Not using Gauss-Seidel since there are step-bounded objectives");
			useGS = false;
		}

		boolean exportAdv = (settings.getChoice(PrismSettings.PRISM_EXPORT_ADV) != Prism.EXPORT_ADV_NONE);
		String advFileNameBase = settings.getString(PrismSettings.PRISM_EXPORT_ADV_FILENAME);

		int dimProb = targets.length;
		int dimReward = rewards.size();

		NativeIntArray adversary = new NativeIntArray(modelProduct.getNumStates());

		// Build sparse matrix for transition matrix,
		// after first removing self-loops for the case of probabilistic objectives only
		JDDNode a = modelProduct.getTrans().copy();
		if (dimReward == 0) {
			JDDNode tmp = JDD.And(JDD.Equals(a.copy(), 1.0), JDD.Identity(modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars()));
			a = JDD.ITE(tmp, JDD.Constant(0), a);
		}
		NDSparseMatrix trans_matrix = NDSparseMatrix.BuildNDSparseMatrix(a, modelProduct.getODD(), modelProduct.getAllDDRowVars(),
		                                                                   modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars());
		if (exportAdv) {
			NDSparseMatrix.AddActionsToNDSparseMatrix(a, modelProduct.getTransActions(), modelProduct.getODD(), modelProduct.getAllDDRowVars(),
			                                          modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(), trans_matrix);
		}

		final DoubleVector[] probDoubleVectors = new DoubleVector[dimProb];
		for (int i = 0; i < dimProb; i++) {
			probDoubleVectors[i] = new DoubleVector(targets[i], modelProduct.getAllDDRowVars(), modelProduct.getODD());
		}

		final NDSparseMatrix[] rewSparseMatrices = new NDSparseMatrix[dimReward];
		for (int i = 0; i < dimReward; i++) {
			rewSparseMatrices[i] = NDSparseMatrix.BuildSubNDSparseMatrix(a, modelProduct.getODD(), modelProduct.getAllDDRowVars(),
			                                                              modelProduct.getAllDDColVars(), modelProduct.getAllDDNondetVars(), rewards.get(i));
		}

		JDD.Deref(a);

		final boolean useGSfinal = useGS;
		final int[] advCounter = {0};

		return weights -> {
			if (exportAdv) {
				PrismNative.setExportAdvFilename(PrismUtils.addCounterSuffixToFilename(advFileNameBase, ++advCounter[0]));
			}
			if (useGSfinal) {
				return PrismSparse.NondetMultiObjGS(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
				                                    modelProduct.getAllDDNondetVars(), false, start, adversary, trans_matrix,
				                                    probDoubleVectors, rewSparseMatrices, weights);
			} else {
				return PrismSparse.NondetMultiObj(modelProduct.getODD(), modelProduct.getAllDDRowVars(), modelProduct.getAllDDColVars(),
				                                  modelProduct.getAllDDNondetVars(), false, start, adversary, trans_matrix, modelProduct.getSynchs(),
				                                  probDoubleVectors, probStepBounds, rewSparseMatrices, weights, rewardStepBounds);
			}
		};
	}
}
