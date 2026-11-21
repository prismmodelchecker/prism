//==============================================================================
//
//	Copyright (c) 2026-
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

package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import explicit.rewards.Rewards;
import explicit.rewards.Rewards2RewardGenerator;
import parser.Values;
import parser.ast.Expression;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionTemporal;
import parser.visitor.ASTTraverse;
import prism.AppliedStrategy;
import prism.InducedModelInfo;
import prism.ModelInfo;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.RewardGenerator;
import prism.RewardInfo;
import strat.StrategyExplicit;
import strat.StrategyExportOptions;
import strat.StrategyExportOptions.InducedModelMode;

/**
 * Explicit-state implementation of {@link AppliedStrategy}:
 * a strategy for an explicit-state model, and the (explicit-state) model it induces.
 */
public class AppliedStrategyExplicit<Value> implements AppliedStrategy<Value>, ModelTransformation<Model<Value>, Model<Value>>
{
	/** The strategy */
	private StrategyExplicit<Value> strat;
	/** The induced model */
	private Model<Value> inducedModel;
	/** Map from induced to original states (and memory) */
	private InducedStateMap stateMap;
	/** Model info for the induced model */
	private ModelInfo inducedModelInfo;
	/** Reward info for the induced model */
	private RewardInfo inducedRewardInfo;
	/** Reward generator for the induced model */
	private RewardGenerator<Value> inducedRewardGen;

	/**
	 * Apply a strategy to the model it is for, constructing the induced model.
	 * Labels and rewards attached to the original model are copied to the induced model.
	 * @param strat The strategy
	 * @param origModelInfo Model info for the original model
	 * @param origRewardInfo Reward info for the original model (null if none)
	 */
	public AppliedStrategyExplicit(StrategyExplicit<Value> strat, ModelInfo origModelInfo, RewardInfo origRewardInfo) throws PrismException
	{
		this.strat = strat;
		StrategyExportOptions options = new StrategyExportOptions();
		options.setMode(InducedModelMode.REDUCE);
		options.setReachOnly(true);
		inducedModel = strat.constructInducedModel(options, map -> stateMap = map);
		inducedModelInfo = new InducedModelInfo(origModelInfo, inducedModel.getModelType(), inducedModel.getVarList());
		inducedRewardInfo = origRewardInfo != null ? origRewardInfo : new RewardInfo() {};
		// Rewards are those attached to the induced model (copied from the original one)
		inducedRewardGen = new Rewards2RewardGenerator<Value>(inducedRewardInfo, inducedModel, inducedModel.getEvaluator())
		{
			@Override
			public Rewards<Value> getTheRewardObject(int r) throws PrismException
			{
				Rewards<Value> rews = inducedModel.getRewardsByPosition(r);
				if (rews == null) {
					rews = inducedModel.getRewardsByName(getRewardStructName(r));
				}
				if (rews == null) {
					throw new PrismException("Reward structure " + (r + 1) + " is not available for the model induced by the strategy");
				}
				return rews;
			}
		};
	}

	// Methods to implement AppliedStrategy

	@Override
	public StrategyExplicit<Value> getStrategy()
	{
		return strat;
	}

	@Override
	public Model<Value> getOriginalModel()
	{
		return strat.getModel();
	}

	@Override
	public Model<Value> getInducedModel()
	{
		return inducedModel;
	}

	@Override
	public ModelInfo getInducedModelInfo()
	{
		return inducedModelInfo;
	}

	@Override
	public RewardInfo getInducedRewardInfo()
	{
		return inducedRewardInfo;
	}

	@Override
	public RewardGenerator<Value> getInducedRewardGenerator()
	{
		return inducedRewardGen;
	}

	@Override
	public int getOriginalState(int s)
	{
		return stateMap.getOriginalState(s);
	}

	@Override
	public boolean hasMemory()
	{
		return stateMap.hasMemory();
	}

	@Override
	public int getMemory(int s)
	{
		return stateMap.getMemory(s);
	}

	/**
	 * {@inheritDoc}
	 * <br><br>
	 * If the induced model has no nondeterminism, the transition rewards of the strategy's
	 * choices are folded into state rewards, which is correct for expected rewards,
	 * but not for instantaneous ones ({@code R[I=k]}), so these are not supported
	 * for reward structures with transition rewards.
	 */
	@Override
	public void checkPropertySupported(Expression expr, Values constantValues) throws PrismException
	{
		if (inducedModel.getModelType().nondeterministic()) {
			return;
		}
		List<ExpressionReward> instRewards = new ArrayList<>();
		expr.accept(new ASTTraverse()
		{
			@Override
			public void visitPost(ExpressionReward e)
			{
				if (e.getExpression() instanceof ExpressionTemporal && ((ExpressionTemporal) e.getExpression()).getOperator() == ExpressionTemporal.R_I) {
					instRewards.add(e);
				}
			}
		});
		for (ExpressionReward e : instRewards) {
			int r = e.getRewardStructIndexByIndexObject(inducedRewardInfo.getRewardStructNames(), constantValues);
			if (inducedRewardInfo.rewardStructHasTransitionRewards(r)) {
				throw new PrismNotSupportedException("Instantaneous rewards for reward structures with transition rewards are not supported under a strategy");
			}
		}
	}

	// Methods to implement ModelTransformation

	@Override
	public Model<Value> getTransformedModel()
	{
		return inducedModel;
	}

	/**
	 * Map values for the induced model to the original model: each initial state of the
	 * induced model gives the value for the corresponding (initial) state of the original model.
	 * Other states of the original model get the default value for the type.
	 */
	@Override
	public StateValues projectToOriginalModel(StateValues svInduced) throws PrismException
	{
		StateValues sv = new StateValues(svInduced.getType(), getOriginalModel());
		for (int s : inducedModel.getInitialStates()) {
			sv.setValue(getOriginalState(s), svInduced.getValue(s));
		}
		sv.setAccuracy(svInduced.getAccuracy());
		return sv;
	}

	// Other methods

	/**
	 * Map values for the original model to the induced model: each state of the induced model
	 * gets the value of the corresponding state of the original model.
	 */
	public StateValues pullBack(StateValues svOrig) throws PrismException
	{
		StateValues sv = StateValues.create(svOrig.getType(), s -> svOrig.getValue(getOriginalState(s)), inducedModel);
		sv.setAccuracy(svOrig.getAccuracy());
		return sv;
	}

	/**
	 * Map a set of states of the original model to the induced model: the induced states
	 * whose corresponding original state is in the set.
	 */
	public BitSet pullBack(BitSet origStates)
	{
		int n = inducedModel.getNumStates();
		BitSet bs = new BitSet(n);
		for (int s = 0; s < n; s++) {
			if (origStates.get(getOriginalState(s))) {
				bs.set(s);
			}
		}
		return bs;
	}

	/**
	 * Attach a label to the induced model, given as a set of states of the original model:
	 * the label holds in the induced states whose corresponding original state is in the set.
	 */
	public void addLabelFromOriginal(String name, BitSet origStates) throws PrismException
	{
		if (!(inducedModel instanceof ModelExplicit)) {
			throw new PrismException("Cannot attach labels to the induced model");
		}
		((ModelExplicit<Value>) inducedModel).addLabel(name, pullBack(origStates));
	}
}
