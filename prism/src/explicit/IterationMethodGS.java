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

import common.IntSet;
import explicit.rewards.MCRewards;
import explicit.rewards.MDPRewards;
import prism.PrismException;

/**
 * IterationMethod that encapsulates the functionality of the Gauss-Seidel method (forward/backward).
 */
public class IterationMethodGS extends IterationMethod {
	/** Do backwards Gauss-Seidel? */
	private boolean backwards;

	/**
	 * Constructor.
	 * @param absolute For convergence check, perform absolute comparison?
	 * @param termCritParam For convergence check, the epsilon value to use
	 * @param backwards Do backwards Gauss-Seidel?
	 */
	IterationMethodGS(boolean absolute, double epsilon, boolean backwards)
	{
		super(absolute, epsilon);
		this.backwards = backwards;
	}

	@Override
	public IterationValIter forMvMult(DTMC dtmc)
	{
		return new SingleVectorIterationValIter(dtmc) {
			@Override
			public boolean iterateAndCheckConvergence(IntSet states)
			{
				// Matrix-vector multiply
				error = dtmc.mvMultGS(soln,
				                               backwards ? states.reversedIterator() : states.iterator(),
				                               absolute);

				// Check termination
				return (error < termCritParam);
			}
		};
	}

	@Override
	public IterationIntervalIter forMvMultInterval(DTMC dtmc, boolean fromBelow, boolean enforceMonotonicity, boolean checkMonotonicity)
	{
		return new SingleVectorIterationIntervalIter(dtmc) {
			@Override
			public void iterate(IntSet states) throws PrismException
			{
				// Matrix-vector multiply
				dtmc.mvMultGSIntervalIter(soln,
				                          backwards ? states.reversedIterator() : states.iterator(),
				                          enforceMonotonicity,
				                          checkMonotonicity,
				                          fromBelow);
			}
		};
	}

	@Override
	public IterationValIter forMvMultRew(DTMC dtmc, MCRewards rew)
	{
		return new SingleVectorIterationValIter(dtmc) {
			@Override
			public boolean iterateAndCheckConvergence(IntSet states)
			{
				// Matrix-vector multiply
				error = dtmc.mvMultRewGS(soln,
				                                  rew,
				                                  backwards ? states.reversedIterator() : states.iterator(),
				                                  absolute);

				// Check termination
				return (error < termCritParam);
			}
		};
	}

	@Override
	public IterationIntervalIter forMvMultRewInterval(DTMC dtmc, MCRewards rew, boolean fromBelow, boolean enforceMonotonicity, boolean checkMonotonicity)
	{
		return new SingleVectorIterationIntervalIter(dtmc) {
			@Override
			public void iterate(IntSet states) throws PrismException
			{
				// Matrix-vector multiply
				dtmc.mvMultRewGSIntervalIter(soln,
					                          rew,
					                          backwards ? states.reversedIterator() : states.iterator(),
					                          enforceMonotonicity,
					                          checkMonotonicity,
					                          fromBelow);
			}
		};
	}

	@Override
	public IterationValIter forMvMultMinMax(MDP mdp, boolean min, int[] strat)
	{
		return new SingleVectorIterationValIter(mdp) {
			@Override
			public boolean iterateAndCheckConvergence(IntSet states)
			{
				// Matrix-vector multiply
				error = mdp.mvMultGSMinMax(soln,
				                                    min,
				                                    backwards ? states.reversedIterator() : states.iterator(),
				                                    absolute,
				                                    strat);

				// Check termination
				return (error < termCritParam);
			}
		};
	}

	@Override
	public IterationIntervalIter forMvMultMinMaxInterval(MDP mdp, boolean min, int[] strat, boolean fromBelow, boolean enforceMonotonicity,
			boolean checkMonotonicity) throws PrismException
	{
		return new SingleVectorIterationIntervalIter(mdp) {
			@Override
			public void iterate(IntSet states)
			{
				// TODO: check monotonic not yet supported

				// Matrix-vector multiply
				mdp.mvMultGSMinMaxIntervalIter(soln,
				                               min,
				                               backwards ? states.reversedIterator() : states.iterator(),
				                               strat,
				                               enforceMonotonicity,
				                               fromBelow);
			}
		};
	}

	@Override
	public IterationValIter forMvMultRewMinMax(MDP mdp, MDPRewards rewards, boolean min, int[] strat) throws PrismException
	{
		return new SingleVectorIterationValIter(mdp) {
			@Override
			public boolean iterateAndCheckConvergence(IntSet states)
			{
				// Matrix-vector multiply
				error = mdp.mvMultRewGSMinMax(soln,
				                                       rewards,
				                                       min,
				                                       backwards ? states.reversedIterator() : states.iterator(),
				                                       absolute,
				                                       strat);

				// Check termination
				return (error < termCritParam);
			}
		};
	}

	@Override
	public IterationIntervalIter forMvMultRewMinMaxInterval(MDP mdp, MDPRewards rewards, boolean min, int[] strat, boolean fromBelow,
			boolean enforceMonotonicity, boolean checkMonotonicity) throws PrismException
	{
		return new SingleVectorIterationIntervalIter(mdp) {
			@Override
			public void iterate(IntSet states)
			{
				// TODO: check monotonic not yet supported

				// Matrix-vector multiply
				mdp.mvMultRewGSMinMaxIntervalIter(soln,
				                                  rewards,
				                                  min,
				                                  backwards ? states.reversedIterator() : states.iterator(),
				                                  strat,
				                                  enforceMonotonicity,
				                                  fromBelow);
			}
		};
	}

	@Override
	public String getDescriptionShort()
	{
		return (backwards ? "Backwards " : "") + "Gauss-Seidel";
	}
}
