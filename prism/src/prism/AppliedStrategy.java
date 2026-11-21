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

package prism;

import parser.Values;
import parser.ast.Expression;
import strat.Strategy;

/**
 * A strategy applied to the model it is for: the model induced by the strategy,
 * information needed to analyse it (model info, rewards), and how its states
 * relate to those of the original model.
 * <br><br>
 * The induced model is constructed in "reduce" mode, restricted to reachable states,
 * with labels and rewards copied from the original model. Its initial states correspond
 * to those of the original model (with the strategy's initial memory, if any).
 */
public interface AppliedStrategy<Value>
{
	/**
	 * Get the strategy that was applied.
	 */
	public Strategy<Value> getStrategy();

	/**
	 * Get the original model, to which the strategy was applied.
	 */
	public prism.Model<Value> getOriginalModel();

	/**
	 * Get the model induced by the strategy.
	 */
	public prism.Model<Value> getInducedModel();

	/**
	 * Get the model info for the induced model.
	 */
	public ModelInfo getInducedModelInfo();

	/**
	 * Get the reward info for the induced model.
	 */
	public RewardInfo getInducedRewardInfo();

	/**
	 * Get a reward generator for the induced model.
	 */
	public RewardGenerator<Value> getInducedRewardGenerator();

	/**
	 * Get the state of the original model corresponding to state {@code s} of the induced model.
	 */
	public int getOriginalState(int s);

	/**
	 * Does the induced model also record strategy memory (i.e., is the strategy not memoryless)?
	 */
	public boolean hasMemory();

	/**
	 * Get the strategy memory corresponding to state {@code s} of the induced model
	 * (-1 if the strategy is memoryless).
	 */
	public int getMemory(int s);

	/**
	 * Check that a property can be checked on the induced model,
	 * throwing an explanatory exception if not.
	 * @param expr The property
	 * @param constantValues Values of constants (for evaluating reward structure indices)
	 */
	public void checkPropertySupported(Expression expr, Values constantValues) throws PrismException;
}
