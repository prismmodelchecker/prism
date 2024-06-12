//==============================================================================
//	
//	Copyright (c) 2020-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import java.util.BitSet;
import java.util.List;
import java.util.Map;

import explicit.rewards.MDPRewards;
import explicit.rewards.STPGRewards;
import prism.ModelType;
import prism.PrismException;

/**
 * Simple explicit-state representation of a (turn-based) stochastic two-player game (STPG).
 */
public class STPGSimple<Value> extends MDPSimple<Value> implements STPG<Value>
{
	/**
	 * Which player owns each state
	 */
	protected StateOwnersSimple stateOwners;
	
	@Override
	public ModelType getModelType()
	{
		return ModelType.STPG;
	}

	/**
	 * Constructor: empty STPG.
	 */
	public STPGSimple()
	{
		super();
		stateOwners = new StateOwnersSimple();
	}

	/**
	 * Constructor: new STPG with fixed number of states.
	 */
	public STPGSimple(int numStates)
	{
		super(numStates);
		stateOwners = new StateOwnersSimple(numStates);
	}

	/**
	 * Copy constructor
	 */
	public STPGSimple(STPGSimple<Value> stpg)
	{
		super(stpg);
		stateOwners = new StateOwnersSimple(stpg.stateOwners);
	}
	
	/**
	 * Construct an STPG from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 * Player and coalition info is also copied across.
	 */
	public STPGSimple(STPGSimple<Value> stpg, int permut[])
	{
		super(stpg, permut);
		stateOwners = new StateOwnersSimple(stpg.stateOwners, permut);
	}

	// Mutators

	@Override
	public void clearState(int s)
	{
		super.clearState(s);
		stateOwners.clearState(s);
	}

	@Override
	public void addStates(int numToAdd)
	{
		super.addStates(numToAdd);
		// Assume all player 1
		for (int i = 0; i < numToAdd; i++) {
			stateOwners.addState(0);
		}
	}

	/**
	 * Add a new (player {@code p}) state and return its index.
	 * @param p Player who owns the new state (0-indexed)
	 */
	public int addState(int p)
	{
		int s = super.addState();
		stateOwners.setPlayer(s, p);
		return s;
	}

	/**
	 * Set the player that owns state {@code s} to {@code p}.
	 * @param s State to be modified (0-indexed)
	 * @param p Player who owns the state (0-indexed)
	 */
	public void setPlayer(int s, int p)
	{
		stateOwners.setPlayer(s, p);
	}

	// Accessors (for Model)
	
	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (trans.get(i).isEmpty() && (except == null || !except.get(i)))
				throw new PrismException("Game has a deadlock in state " + i + (statesList == null ? "" : ": " + statesList.get(i)));
		}
	}
	
	// Accessors (for STPG)
	
	@Override
	public int getPlayer(int s)
	{
		return stateOwners.getPlayer(s);
	}
	
	@Override
	public void prob0step(BitSet subset, BitSet u, boolean forall1, boolean forall2, BitSet result)
	{
		int i;
		boolean b1, b2;
		boolean forall = false;

		for (i = 0; i < numStates; i++) {
			if (subset.get(i)) {
				forall = (getPlayer(i) == 0) ? forall1 : forall2;
				b1 = forall; // there exists or for all
				for (Distribution<?> distr : trans.get(i)) {
					b2 = distr.containsOneOf(u);
					if (forall) {
						if (!b2) {
							b1 = false;
							continue;
						}
					} else {
						if (b2) {
							b1 = true;
							continue;
						}
					}
				}
				result.set(i, b1);
			}
		}
	}
	
	@Override
	public void prob1step(BitSet subset, BitSet u, BitSet v, boolean forall1, boolean forall2, BitSet result)
	{
		int i;
		boolean b1, b2;
		boolean forall = false;

		for (i = 0; i < numStates; i++) {
			if (subset.get(i)) {
				forall = (getPlayer(i) == 0) ? forall1 : forall2;
				b1 = forall; // there exists or for all
				for (Distribution<?> distr : trans.get(i)) {
					b2 = distr.containsOneOf(v) && distr.isSubsetOf(u);
					if (forall) {
						if (!b2) {
							b1 = false;
							continue;
						}
					} else {
						if (b2) {
							b1 = true;
							continue;
						}
					}
				}
				result.set(i, b1);
			}
		}
	}

	@Override
	public void mvMultMinMax(double vect[], boolean min1, boolean min2, double result[], BitSet subset, boolean complement, int adv[])
	{
		int s;
		boolean min = false;
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++) {
				min = (getPlayer(s) == 0) ? min1 : min2;
				result[s] = mvMultMinMaxSingle(s, vect, min, adv);
			}
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1)) {
				min = (getPlayer(s) == 0) ? min1 : min2;
				result[s] = mvMultMinMaxSingle(s, vect, min, adv);
			}
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1)) {
				min = (getPlayer(s) == 0) ? min1 : min2;
				result[s] = mvMultMinMaxSingle(s, vect, min, adv);
			}
		}
	}

	@Override
	public double mvMultMinMaxSingle(int s, double vect[], boolean min1, boolean min2)
	{
		boolean min = (getPlayer(s) == 0) ? min1 : min2;
		return mvMultMinMaxSingle(s, vect, min, null);
	}

	@Override
	public List<Integer> mvMultMinMaxSingleChoices(int s, double vect[], boolean min1, boolean min2, double val)
	{
		boolean min = (getPlayer(s) == 0) ? min1 : min2;
		return mvMultMinMaxSingleChoices(s, vect, min, val);
	}

	@Override
	public double mvMultGSMinMax(double vect[], boolean min1, boolean min2, BitSet subset, boolean complement, boolean absolute, int[] adv)
	{
		int s;
		double d, diff, maxDiff = 0.0;
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++) {
				d = mvMultJacMinMaxSingle(s, vect, min1, min2, adv);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1)) {
				d = mvMultJacMinMaxSingle(s, vect, min1, min2, adv);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1)) {
				d = mvMultJacMinMaxSingle(s, vect, min1, min2, adv);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		}
		return maxDiff;
	}

	@Override
	public double mvMultJacMinMaxSingle(int s, double vect[], boolean min1, boolean min2, int[] adv)
	{
		boolean min = (getPlayer(s) == 0) ? min1 : min2;
		return mvMultJacMinMaxSingle(s, vect, min, adv);
	}

	@Override
	public void mvMultRewMinMax(double vect[], STPGRewards<Double> rewards, boolean min1, boolean min2, double result[], BitSet subset, boolean complement, int adv[])
	{
		int s;
		boolean min = false;
		MDPRewards<Double> mdpRewards = rewards.buildMDPRewards();
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++) {
				min = (getPlayer(s) == 0) ? min1 : min2;
				result[s] = mvMultRewMinMaxSingle(s, vect, mdpRewards, min, adv, 1.0);
			}
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1)) {
				min = (getPlayer(s) == 0) ? min1 : min2;
				result[s] = mvMultRewMinMaxSingle(s, vect, mdpRewards, min, adv, 1.0);
			}
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1)) {
				min = (getPlayer(s) == 0) ? min1 : min2;
				result[s] = mvMultRewMinMaxSingle(s, vect, mdpRewards, min, adv, 1.0);
			}
		}
	}

	@Override
	public double mvMultRewMinMaxSingle(int s, double vect[], STPGRewards<Double> rewards, boolean min1, boolean min2, int adv[])
	{
		MDPRewards<Double> mdpRewards = rewards.buildMDPRewards();
		boolean min = (getPlayer(s) == 0) ? min1 : min2;
		return mvMultRewMinMaxSingle(s, vect, mdpRewards, min, adv);
	}

	@Override
	public List<Integer> mvMultRewMinMaxSingleChoices(int s, double vect[], STPGRewards<Double> rewards, boolean min1, boolean min2, double val)
	{
		MDPRewards<Double> mdpRewards = rewards.buildMDPRewards();
		boolean min = (getPlayer(s) == 0) ? min1 : min2;
		return mvMultRewMinMaxSingleChoices(s, vect, mdpRewards, min, val);
	}

	@Override
	public void mvMultRewMinMax(double vect[], STPGRewards<Double> rewards, boolean min1, boolean min2, double result[], BitSet subset, boolean complement, int adv[], double disc)
	{
		int s;
		boolean min = false;
		MDPRewards<Double> mdpRewards = rewards.buildMDPRewards();
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++) {
				min = (getPlayer(s) == 0) ? min1 : min2;
				result[s] = mvMultRewMinMaxSingle(s, vect, mdpRewards, min, adv, disc);
			}
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1)) {
				min = (getPlayer(s) == 0) ? min1 : min2;
				result[s] = mvMultRewMinMaxSingle(s, vect, mdpRewards, min, adv, disc);
			}
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1)) {
				min = (getPlayer(s) == 0) ? min1 : min2;
				//System.out.printf("s: %s, min1: %s, min2: %s, min: %s, player: %d\n", s, min1, min2, min, getPlayer(s));
				result[s] = mvMultRewMinMaxSingle(s, vect, mdpRewards, min, adv, disc);
			}
		}
	}

	/**
	 * Do a single row of (discounted) matrix-vector multiplication and sum of action reward followed by min/max.
	 * i.e. return min/max_{k1,k2} { rew(s) + disc * sum_j P_{k1,k2}(s,j)*vect[j] }
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param mdpRewards The rewards
	 * @param min Min or max (true=min, false=max)
	 * @param adv Storage for adversary choice indices (ignored if null)
	 * @param disc Discount factor
	 */
	public double mvMultRewMinMaxSingle(int s, double vect[], MDPRewards<Double> mdpRewards, boolean min, int adv[], double disc)
	{
		int j, k, advCh = -1;
		double d, prob, minmax;
		boolean first;
		List<Distribution<Value>> step;

		minmax = 0;
		first = true;
		j = -1;
		step = trans.get(s);
		for (Distribution<Value> distr : step) {
			j++;
			// Compute sum for this distribution
			d = mdpRewards.getTransitionReward(s, j);

			for (Map.Entry<Integer, Value> e : distr) {
				k = e.getKey();
				prob = getEvaluator().toDouble(e.getValue());
				d += prob * vect[k] * disc;
			}

			// Check whether we have exceeded min/max so far
			if (first || (min && d < minmax) || (!min && d > minmax)) {
				minmax = d;
				// If adversary generation is enabled, remember optimal choice
				if (adv != null) {
					advCh = j;
				}
			}
			first = false;
		}
		// If adversary generation is enabled, store optimal choice
		if (adv != null & !first) {
			// Only remember strictly better choices (required for max)
			if (adv[s] == -1 || (min && minmax < vect[s]) || (!min && minmax > vect[s]) || this instanceof STPG) {
				adv[s] = advCh;
			}
		}

		// Add state reward (doesn't affect min/max)
		minmax += mdpRewards.getStateReward(s);

		return minmax;
	}

	// Standard methods

	@Override
	public String toString()
	{
		int i, j, n;
		Object o;
		String s = "";
		s = "[ ";
		for (i = 0; i < numStates; i++) {
			if (i > 0)
				s += ", ";
			if (statesList.size() > i)
				s += i + "(P-" + (stateOwners.getPlayer(i)+1) + " " + statesList.get(i) + "): ";
			else
				s += i + "(P-" + (stateOwners.getPlayer(i)+1) + "): ";
			s += "[";
			n = getNumChoices(i);
			for (j = 0; j < n; j++) {
				if (j > 0)
					s += ",";
				o = getAction(i, j);
				if (o != null)
					s += o + ":";
				s += trans.get(i).get(j);
			}
			s += "]";
		}
		s += " ]\n";
		return s;
	}
}
