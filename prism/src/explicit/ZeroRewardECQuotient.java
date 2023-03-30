//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package explicit;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import common.functions.PairPredicateInt;
import common.IterableBitSet;
import explicit.graphviz.Decorator;
import explicit.graphviz.ShowRewardDecorator;
import explicit.modelviews.EquivalenceRelationInteger;
import explicit.modelviews.MDPDroppedChoicesCached;
import explicit.modelviews.MDPEquiv;
import explicit.modelviews.StateChoicePair;
import explicit.rewards.MDPRewards;
import prism.PrismComponent;
import prism.PrismException;

/**
 * Helper class for obtaining the zero-reward EC quotient of an MDP.
 * <br>
 * In the original MDP, the zero-reward maximal end components are identified, i.e.,
 * those end components where there is a strategy to stay infinitely without ever
 * seeing another reward.
 * <br>
 * In the quotient, those zero-reward MECs are each collapsed to a single state,
 * with choices that have transitions outside the MEC preserved.
 */
public class ZeroRewardECQuotient<Value>
{
	private MDPEquiv<Value> quotient;
	private MDPRewards<Value> quotientRewards;
	private int numberOfZMECs;

	private static final boolean debug = false;

	private ZeroRewardECQuotient(MDPEquiv<Value> quotient, MDPRewards<Value> quotientRewards, int numberOfZMECs)
	{
		this.quotient = quotient;
		this.quotientRewards = quotientRewards;
		this.numberOfZMECs = numberOfZMECs;
	}

	public MDP<Value> getModel()
	{
		return quotient;
	}

	public MDPRewards<Value> getRewards()
	{
		return quotientRewards;
	}

	public int getNumberOfZeroRewardMECs()
	{
		return numberOfZMECs;
	}

	public BitSet getNonRepresentativeStates()
	{
		return quotient.getNonRepresentativeStates();
	}

	public void mapResults(double[] soln) {
		for (int s : new IterableBitSet(quotient.getNonRepresentativeStates())) {
			int representative = quotient.mapStateToRestrictedModel(s);
			soln[s] = soln[representative];
		}
	}

	public static <Value> ZeroRewardECQuotient<Value> getQuotient(PrismComponent parent, MDP<Value> mdp, BitSet restrict, MDPRewards<Value> rewards) throws PrismException
	{
		PairPredicateInt positiveRewardChoice = (int s, int i) -> {
			if (mdp.getEvaluator().gt(rewards.getStateReward(s), mdp.getEvaluator().zero())) {
				return true;
			}
			if (mdp.getEvaluator().gt(rewards.getTransitionReward(s, i), mdp.getEvaluator().zero())) {
				return true;
			}
			return false;
		};

		// drop positive reward choices
		MDPDroppedChoicesCached<Value> zeroRewMDP = new MDPDroppedChoicesCached<>(mdp, positiveRewardChoice);
		// compute the MECs in the zero-reward sub-MDP
		ECComputer ecComputer = ECComputerDefault.createECComputer(parent, zeroRewMDP);
		ecComputer.computeMECStates(restrict);

		List<BitSet> mecs = ecComputer.getMECStates();

		if (mecs.isEmpty()) {
			return null;
		}

		// the equivalence relation on the states
		EquivalenceRelationInteger equiv = new EquivalenceRelationInteger(mecs);

		// we drop zero reward loops on the equivalence classes
		PairPredicateInt zeroRewardECloop = (int s, int i) -> {
			if (positiveRewardChoice.test(s, i)) {
				return false;
			}

			// return true if all successors t of state s for choice i are in the
			// same equivalence class
			boolean rv = mdp.allSuccessorsMatch(s, i, (int t) -> equiv.test(s,t));
			return rv;
		};

		final MDPDroppedChoicesCached<Value> droppedZeroRewardLoops = new MDPDroppedChoicesCached<>(mdp, zeroRewardECloop);
		if (debug)
			droppedZeroRewardLoops.exportToDotFile("zero-mec-loops-dropped.dot");

		BasicModelTransformation<MDP<Value>, MDPEquiv<Value>> transform = MDPEquiv.transform(droppedZeroRewardLoops, equiv);
		final MDPEquiv<Value> quotient = transform.getTransformedModel();

		MDPRewards<Value> quotientRewards = new MDPRewards<Value>() {
			@Override
			public Value getStateReward(int s)
			{
				return rewards.getStateReward(s);
			}

			@Override
			public Value getTransitionReward(int s, int i)
			{
				StateChoicePair mapped = quotient.mapToOriginalModel(s, i);
				int mappedChoiceInOriginal = droppedZeroRewardLoops.mapChoiceToOriginalModel(mapped.getState(), mapped.getChoice());
				return rewards.getTransitionReward(mapped.getState(), mappedChoiceInOriginal);
			}

			@Override
			public MDPRewards<Value> liftFromModel(Product<?> product)
			{
				throw new RuntimeException("Not implemented");
			}

			@Override
			public boolean hasTransitionRewards()
			{
				return rewards.hasTransitionRewards();
			}
		};

		if (debug) {
			List<Decorator> decorators = Arrays.asList(new ShowRewardDecorator<>(quotientRewards));
			quotient.exportToDotFile("zero-mec-quotient.dot", decorators);
		}

		return new ZeroRewardECQuotient<>(quotient, quotientRewards, mecs.size());
	}

}
