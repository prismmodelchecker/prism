//==============================================================================
//
//	Copyright (c) 2025-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

package symbolic.build;

import common.SafeCast;
import explicit.CTMCSimple;
import explicit.DTMC;
import explicit.DTMCSimple;
import explicit.Distribution;
import explicit.MDP;
import explicit.MDPSimple;
import explicit.ModelExplicit;
import explicit.SuccessorsIterator;
import explicit.rewards.Rewards;
import explicit.rewards.Rewards2RewardGenerator;
import explicit.rewards.RewardsExplicit;
import explicit.rewards.RewardsSimple;
import io.IOUtils;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import odd.ODDNode;
import prism.Evaluator;
import prism.Pair;
import prism.PrismComponent;
import prism.PrismException;
import prism.RewardGenerator;
import prism.RewardInfo;
import symbolic.model.NondetModel;
import symbolic.model.ProbModel;
import symbolic.states.StateListMTBDD;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Class to convert symbolic representations of models to explicit-state ones.
 */
public class MTBDD2ExplicitModel extends PrismComponent
{
	public MTBDD2ExplicitModel()
	{
		super();
	}

	public MTBDD2ExplicitModel(PrismComponent parent)
	{
		super(parent);
	}

	/**
	 * Convert a symbolically stored model to an explicit-state one.
	 * @param modelSymb The symbolic model
	 */
	public explicit.Model<Double> convertModel(symbolic.model.Model modelSymb) throws PrismException
	{
		ModelExplicit<Double> modelExpl;
		DTMCSimple<Double> dtmcExpl;
		CTMCSimple<Double> ctmcExpl;
		MDPSimple<Double> mdpExpl;
		int numStates = modelSymb.getNumStates();
		// Build new model and convert/add transitions
		switch (modelSymb.getModelType()) {
			case DTMC:
				modelExpl = dtmcExpl = new DTMCSimple<>(numStates);
				convertMarkovChainTransitions((ProbModel) modelSymb, dtmcExpl);
				break;
			case CTMC:
				modelExpl = ctmcExpl = new CTMCSimple<>(numStates);
				convertMarkovChainTransitions((ProbModel) modelSymb, ctmcExpl);
				break;
			case MDP:
				modelExpl = mdpExpl = new MDPSimple<>(numStates);
				convertMDPTransitions((NondetModel) modelSymb, mdpExpl);
				break;
			default:
				throw new PrismException("Can't do symbolic-explicit conversion for " + modelSymb.getModelType() + "s");
		}
		// Convert/add initial states
		traverseStatesBDD(modelSymb.getStart(), modelSymb.getAllDDRowVars(), modelSymb.getODD(), modelExpl::addInitialState);
		// If the symbolic model has labels attached, convert/add them
		for (String label : modelSymb.getLabels()) {
			BitSet labelStates = new BitSet();
			traverseStatesBDD(modelSymb.getLabelDD(label), modelSymb.getAllDDRowVars(), modelSymb.getODD(), labelStates::set);
			modelExpl.addLabel(label, labelStates);
		}
		// If the symbolic model has deadlock info attached, convert/add it
		if (modelSymb.getDeadlocks() != null) {
			BitSet deadlockStates = new BitSet();
			traverseStatesBDD(modelSymb.getDeadlocks(), modelSymb.getAllDDRowVars(), modelSymb.getODD(), deadlockStates::set);
			for (int s = deadlockStates.nextSetBit(0); s >= 0; s = deadlockStates.nextSetBit(s + 1)) {
				modelExpl.addDeadlockState(s);
			}
		}
		// Convert/add state information
		modelExpl.setStatesList(((StateListMTBDD) modelSymb.getReachableStates()).getAsListOfStates());
		return modelExpl;
	}

	/**
	 * Get a {@link RewardGenerator}, which converts the rewards for a
	 * symbolically stored model to explicit-state reward storage.
	 * @param modelSymb The symbolic model
	 * @param modelExpl The corresponding explicit-state model
	 * @param rewardInfo Reward info
	 */
	public RewardGenerator<Double> getRewardConverter(symbolic.model.Model modelSymb, explicit.Model<Double> modelExpl, RewardInfo rewardInfo) throws PrismException
	{
		return new Rewards2RewardGenerator<>(rewardInfo, modelExpl, Evaluator.forDouble())
		{
			@Override
			public Rewards<Double> getTheRewardObject(int r) throws PrismException
			{
				return convertRewards(modelSymb, modelExpl, r, rewardInfo);
			}
		};
	}

	/**
	 * Converts the rewards for a symbolically stored model to explicit-state reward storage.
	 * @param modelSymb The symbolic model
	 * @param modelExpl The corresponding explicit-state model
	 * @param r The index of the reward structure
	 * @param rewardInfo Reward info
	 */
	public explicit.rewards.Rewards<Double> convertRewards(symbolic.model.Model modelSymb, explicit.Model<Double> modelExpl, int r, RewardInfo rewardInfo) throws PrismException
	{
		// Construct new Rewards object
		RewardsSimple<Double> rewards = new RewardsSimple<>(modelSymb.getNumStates());
		// Extract state rewards (if present)
		if (rewardInfo.rewardStructHasStateRewards(r)) {
			JDDNode stateRewards = modelSymb.getStateRewards(r);
			traverseVectorDD(stateRewards, modelSymb.getAllDDRowVars(), modelSymb.getODD(), rewards::addToStateReward);
		}
		// Extract transition rewards (if present)
		if (rewardInfo.rewardStructHasTransitionRewards(r)) {
			JDDNode transRewards = modelSymb.getTransRewards(r);
			switch (modelSymb.getModelType()) {
				case DTMC:
				case CTMC:
					convertMarkovChainTransitionRewards((ProbModel) modelSymb, (DTMC<Double>) modelExpl, transRewards, rewards);
					break;
				case MDP:
					convertMDPTransitionRewards((NondetModel) modelSymb, (MDP<Double>) modelExpl, transRewards, rewards);
					break;
				default:
					throw new PrismException("Can't do symbolic-explicit reward conversion for " + modelSymb.getModelType() + "s");
			}
		}
		return rewards;
	}

	/**
	 * Convert the transitions and actions for a symbolically represented Markov chain
	 * @param mcSymb The symbolic Markov chain
	 * @param mcExpl The explicit-state Markov chain to add to
	 */
	private void convertMarkovChainTransitions(symbolic.model.ProbModel mcSymb, DTMCSimple<Double> mcExpl) throws PrismException
	{
		// Get action info
		List<Object> actions = mcSymb.getActions();
		JDDNode[] transPerAction = mcSymb.getTransPerAction();
		// If action info is stored, extract each DD separately
		if (transPerAction != null) {
			int n = transPerAction.length;
			for (int i = 0; i < n; i++) {
				Object iAct = actions.get(i);
				traverseMatrixDD(transPerAction[i], mcSymb.getAllDDRowVars(), mcSymb.getAllDDColVars(), mcSymb.getODD(), ((s, s2, p, a) -> {
					mcExpl.addToProbability(s, s2, p, iAct);
				}));
			}
		}
		// Otherwise, just use the main transition matrix DD
		else {
			traverseMatrixDD(mcSymb.getTrans(), mcSymb.getAllDDRowVars(), mcSymb.getAllDDColVars(), mcSymb.getODD(), mcExpl::addToProbability);
		}
	}

	/**
	 * Convert the transitions and actions for a symbolically represented MDP
	 * @param mdpSymb The symbolic MDP
	 * @param mdpExpl The explicit-state MDP to add to
	 */
	private void convertMDPTransitions(symbolic.model.NondetModel mdpSymb, MDPSimple<Double> mdpExpl) throws PrismException
	{
		// Get action info
		List<Object> actions = mdpSymb.getActions();
		// Split the transition/action DDs across nondeterministic choices
		List<Pair<JDDNode,JDDNode>> mdpDDs = splitMDPDD(mdpSymb.getTrans(), mdpSymb.getTransActions(), mdpSymb.getAllDDNondetVars());
		// For each one
		for (Pair<JDDNode,JDDNode> ddPair : mdpDDs) {
			// Store the transitions in a map from source state to distribution
			JDDNode dd = ddPair.getKey();
			HashMap<Integer, Distribution<Double>> distrs = new HashMap<>();
			traverseMatrixDD(dd, mdpSymb.getAllDDRowVars(), mdpSymb.getAllDDColVars(), mdpSymb.getODD(), (s,s2,v,a) -> {
				Distribution<Double> distr = distrs.get(s);
				if (distr == null) {
					distrs.put(s, distr = Distribution.ofDouble());
				}
				distr.add(s2, v);
			});
			// Store any actions in a map from state to action
			JDDNode ddActions = ddPair.getValue();
			HashMap<Integer, Object> distrActions = new HashMap<>();
			traverseVectorDD(ddActions, mdpSymb.getAllDDRowVars(), mdpSymb.getODD(), (s, v) -> {
				int a = (int) Math.round(v);
				if (a < actions.size()) {
					Object act = actions.get(a);
					if (act != null) {
						distrActions.put(s, act);
					}
				}
			});
			// Add info to the MDP
			distrs.forEach((s, d) -> mdpExpl.addActionLabelledChoice(s, d, distrActions.get(s)));
			JDD.Deref(dd);
			JDD.Deref(ddActions);
		}
	}

	/**
	 * Convert the transitions and actions for a symbolically represented Markov chain
	 * @param mcSymb The symbolic Markov chain
	 * @param mcExpl The explicit Markov chain
	 * @param rewardsSymb symbolic rewards to convert
	 * @param rewardsExpl The explicit-state reward storage to add to
	 */
	private void convertMarkovChainTransitionRewards(symbolic.model.ProbModel mcSymb, DTMC<Double> mcExpl, JDDNode rewardsSymb, RewardsExplicit<Double> rewardsExpl) throws PrismException
	{
		// Traverse the DD to extract the rewards
		traverseMatrixDD(rewardsSymb, mcSymb.getAllDDRowVars(), mcSymb.getAllDDColVars(), mcSymb.getODD(), (s, s2, v, a) -> {
			// Find successor index for state s2 (from state s)
			SuccessorsIterator it = mcExpl.getSuccessors(s);
			int i = 0;
			while (it.hasNext()) {
				if (it.nextInt() == s2) {
					rewardsExpl.setTransitionReward(s, i, v);
					return;
				}
				i++;
			}
		});
	}

	/**
	 * Convert the transitions and actions for a symbolically represented Markov chain
	 * @param mdpSymb The symbolic MDP
	 * @param mdpExpl The explicit MDP
	 * @param rewardsSymb symbolic rewards to convert
	 * @param rewardsExpl The explicit-state reward storage to add to
	 */
	private void convertMDPTransitionRewards(symbolic.model.NondetModel mdpSymb, MDP<Double> mdpExpl, JDDNode rewardsSymb, RewardsExplicit<Double> rewardsExpl) throws PrismException
	{
		int numStates = mdpSymb.getNumStates();
		int[] choiceIndices = new int[numStates];
		// Split the transition/reward DDs across nondeterministic choices
		List<Pair<JDDNode,JDDNode>> mdpDDs = splitMDPDD(mdpSymb.getTrans01(), rewardsSymb, mdpSymb.getAllDDNondetVars());
		// For each one
		for (Pair<JDDNode,JDDNode> ddPair : mdpDDs) {
			// Update counts of choices for each state
			JDDNode ddChoices = JDD.ThereExists(ddPair.getKey(), mdpSymb.getAllDDColVars());
			traverseStatesBDD(ddChoices, mdpSymb.getAllDDRowVars(), mdpSymb.getODD(), s -> choiceIndices[s]++);
			// Store transition rewards
			// (note: assumes regards are non-negative)
			JDDNode ddRewards = JDD.MaxAbstract(ddPair.getValue(), mdpSymb.getAllDDColVars());
			traverseVectorDD(ddRewards, mdpSymb.getAllDDRowVars(), mdpSymb.getODD(), (s, v) -> {
				rewardsExpl.setTransitionReward(s, choiceIndices[s] - 1, v);
			});
			JDD.Deref(ddChoices);
			JDD.Deref(ddRewards);
		}
	}

	/**
	 * Split the DD for an MDP's transition function,
	 * and a corresponding DD defined for a subset of the transitions,
	 * into (pairs of) separate DDs, resolving nondeterminism.
	 * @param trans MDP transition function DD
	 * @param dd Choice actions DD
	 * @param ddNondetVars DD variables for nondeterminism
	 */
	private List<Pair<JDDNode,JDDNode>> splitMDPDD(JDDNode trans, JDDNode dd, JDDVars ddNondetVars)
	{
		List<Pair<JDDNode,JDDNode>> mdpDDs = new ArrayList<>();
		splitMDPDDRec(trans, dd, ddNondetVars, 0, mdpDDs);
		return mdpDDs;
	}

	/**
	 * Recursive helper for {@link #splitMDPDD(JDDNode, JDDNode, JDDVars)}.
	 * @param trans MDP transition function DD
	 * @param transActions Choice actions DD
	 * @param ddNondetVars DD variables for nondeterminism
	 * @param level Level of recursion
	 * @param mdpDDs List to store DD pairs
	 */
	private void splitMDPDDRec(JDDNode trans, JDDNode transActions, JDDVars ddNondetVars, int level, List<Pair<JDDNode,JDDNode>> mdpDDs)
	{
		// Base case: zero terminal
		if (trans.equals(JDD.ZERO)) {
			return;
		}
		// Base case: non-zero terminal
		if (level == ddNondetVars.n()) {
			// Store DDs
			mdpDDs.add(new Pair<>(trans.copy(), transActions.copy()));
			return;
		}
		// Recurse
		JDDNode e, t;
		if (trans.getIndex() > ddNondetVars.getVarIndex(level)) {
			e = t = trans;
		}
		else {
			e = trans.getElse();
			t = trans.getThen();
		}
		JDDNode eActions, tActions;
		if (transActions.getIndex() > ddNondetVars.getVarIndex(level)) {
			eActions = tActions = transActions;
		}
		else {
			eActions = transActions.getElse();
			tActions = transActions.getThen();
		}
		splitMDPDDRec(e, eActions, ddNondetVars, level + 1, mdpDDs);
		splitMDPDDRec(t, tActions, ddNondetVars, level + 1, mdpDDs);
	}

	/**
	 * Traverse a BDD representing a set of states and extract the indices of those states
	 * @param dd The BDD
	 * @param ddVars The BDD variables
	 * @param odd The ODD
	 * @param consumer Consumer to accept indices
	 */
	private void traverseStatesBDD(JDDNode dd, JDDVars ddVars, ODDNode odd, IntConsumer consumer) throws PrismException
	{
		traverseStatesBDDRec(dd, ddVars, 0, odd, 0, consumer);
	}

	/**
	 * Recursive helper for {@link #traverseStatesBDD(JDDNode, JDDVars, ODDNode, IntConsumer)}.
	 * @param dd The BDD
	 * @param ddVars The BDD variables
	 * @param level Level of recursion
	 * @param odd The ODD
	 * @param i State index counter
	 * @param consumer Consumer to accept indices
	 */
	private void traverseStatesBDDRec(JDDNode dd, JDDVars ddVars, int level, ODDNode odd, long i, IntConsumer consumer) throws PrismException
	{
		// Base case: zero terminal
		if (dd.equals(JDD.ZERO)) {
			return;
		}
		// Base case: non-zero terminal
		if (level == ddVars.n()) {
			consumer.accept(SafeCast.toInt(i));
			return;
		}
		// Recurse
		JDDNode e, t;
		if (dd.getIndex() > ddVars.getVarIndex(level)) {
			e = t = dd;
		}
		else {
			e = dd.getElse();
			t = dd.getThen();
		}
		traverseStatesBDDRec(e, ddVars, level + 1, odd.getElse(), i, consumer);
		traverseStatesBDDRec(t, ddVars, level + 1, odd.getThen(), i + odd.getEOff(), consumer);
	}

	/**
	 * Traverse a DD representing a state-indexed vector of doubles and extract the non-zero elements of the vector
	 * @param dd The DD
	 * @param ddVars The DD variables
	 * @param odd The ODD
	 * @param consumer Consumer to accept indices/values
	 */
	private void traverseVectorDD(JDDNode dd, JDDVars ddVars, ODDNode odd, IOUtils.StateValueConsumer<Double> consumer) throws PrismException
	{
		traverseVectorDDRec(dd, ddVars, 0, odd, 0, consumer);
	}

	/**
	 * Recursive helper for {@link #traverseVectorDD(JDDNode, JDDVars, ODDNode, IOUtils.StateValueConsumer)}
	 * @param dd The DD
	 * @param ddVars The DD variables
	 * @param level Level of recursion
	 * @param odd The ODD
	 * @param i State index counter
	 * @param consumer Consumer to accept indices/values
	 */
	private void traverseVectorDDRec(JDDNode dd, JDDVars ddVars, int level, ODDNode odd, long i, IOUtils.StateValueConsumer<Double> consumer) throws PrismException
	{
		// Base case: zero terminal
		if (dd.equals(JDD.ZERO)) {
			return;
		}
		// Base case: non-zero terminal
		if (level == ddVars.n()) {
			consumer.accept(SafeCast.toInt(i), dd.getValue());
			return;
		}
		// Recurse
		JDDNode e, t;
		if (dd.getIndex() > ddVars.getVarIndex(level)) {
			e = t = dd;
		}
		else {
			e = dd.getElse();
			t = dd.getThen();
		}
		traverseVectorDDRec(e, ddVars, level + 1, odd.getElse(), i, consumer);
		traverseVectorDDRec(t, ddVars, level + 1, odd.getThen(), i + odd.getEOff(), consumer);
	}

	/**
	 * Traverse a DD representing a state-indexed matrix of doubles and extract the non-zero elements of the vector
	 *
	 * @param dd        The DD
	 * @param ddRowVars The DD variables for rows
	 * @param ddColVars The DD variables for cols
	 * @param odd       The ODD
	 * @param consumer  Consumer to accept indices/values
	 */
	private void traverseMatrixDD(JDDNode dd, JDDVars ddRowVars, JDDVars ddColVars, ODDNode odd, IOUtils.MCTransitionConsumer<Double> consumer) throws PrismException
	{
		traverseMatrixDD(dd, ddRowVars, ddColVars, 0, odd, odd, 0, 0, consumer);
	}

	/**
	 * Recursive helper for {@link #traverseMatrixDD(JDDNode, JDDVars, JDDVars, ODDNode, IOUtils.MCTransitionConsumer)}.
	 * @param dd The DD
	 * @param ddRowVars The DD variables for rows
	 * @param ddColVars The DD variables for cols
	 * @param level Level of recursion
	 * @param oddRow The ODD for rows
	 * @param oddCol The ODD for cols
	 * @param r State index counter for rows
	 * @param c State index counter for cols
	 * @param consumer Consumer to accept indices/values
	 */
	private void traverseMatrixDD(JDDNode dd, JDDVars ddRowVars, JDDVars ddColVars, int level, ODDNode oddRow, ODDNode oddCol, long r, long c, IOUtils.MCTransitionConsumer<Double> consumer) throws PrismException
	{
		// Base case: zero terminal
		if (dd.equals(JDD.ZERO)) {
			return;
		}
		// Base case: non-zero terminal
		if (level == ddRowVars.n()) {
			consumer.accept(SafeCast.toInt(r), SafeCast.toInt(c), dd.getValue(), null);
			return;
		}
		// Recurse
		JDDNode e, t, ee, et, te, tt;
		if (dd.getIndex() > ddColVars.getVarIndex(level)) {
			ee = et = te = tt = dd;
		}
		else if (dd.getIndex() > ddRowVars.getVarIndex(level)) {
			ee = te = dd.getElse();
			et = tt = dd.getThen();
		}
		else {
			e = dd.getElse();
			if (e.getIndex() > ddColVars.getVarIndex(level)) {
				ee = et = e;
			}
			else {
				ee = e.getElse();
				et = e.getThen();
			}
			t = dd.getThen();
			if (t.getIndex() > ddColVars.getVarIndex(level)) {
				te = tt = t;
			}
			else {
				te = t.getElse();
				tt = t.getThen();
			}
		}
		traverseMatrixDD(ee, ddRowVars, ddColVars, level + 1, oddRow.getElse(), oddCol.getElse(), r, c, consumer);
		traverseMatrixDD(et, ddRowVars, ddColVars, level + 1, oddRow.getElse(), oddCol.getThen(), r, c + oddCol.getEOff(), consumer);
		traverseMatrixDD(te, ddRowVars, ddColVars, level + 1, oddRow.getThen(), oddCol.getElse(), r + oddRow.getEOff(), c, consumer);
		traverseMatrixDD(tt, ddRowVars, ddColVars, level + 1, oddRow.getThen(), oddCol.getThen(), r + oddRow.getEOff(), c + oddCol.getEOff(), consumer);
	}
}
