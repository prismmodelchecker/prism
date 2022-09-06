// ==============================================================================
//	
// Copyright (c) 2002-
// Authors:
// * Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	
// ------------------------------------------------------------------------------
//	
// This file is part of PRISM.
//	
// PRISM is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//	
// PRISM is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//	
// You should have received a copy of the GNU General Public License
// along with PRISM; if not, write to the Free Software Foundation,
// Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
//	
// ==============================================================================

package explicit;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import prism.ModelType;
import explicit.rewards.MDPRewards;
import explicit.rewards.STPGRewards;

/**
 * Simple explicit-state representation of a (turn-based) stochastic two-player game (STPG).
 */
public class STPGExplicit extends MDPSimple implements STPG
{
	/** Which player owns each state, i.e. stateOwners[i] is owned by player i (1 or 2) */
	protected List<Integer> stateOwners;

	// Constructors

	/**
	 * Constructor: empty STPG.
	 */
	public STPGExplicit()
	{
		super();
		stateOwners = new ArrayList<Integer>(0);
	}

	/**
	 * Constructor: new STPG with fixed number of states.
	 */
	public STPGExplicit(int numStates)
	{
		super(numStates);
		stateOwners = new ArrayList<Integer>(numStates);
	}

	/**
	 * Construct an STPG from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 */
	public STPGExplicit(STPGExplicit stpg, int permut[])
	{
		super(stpg, permut);
		stateOwners = new ArrayList<Integer>(numStates);
		// Create blank array of correct size
		for (int i = 0; i < numStates; i++) {
			stateOwners.add(0);
		}
		// Copy permuted player info
		for (int i = 0; i < numStates; i++) {
			stateOwners.set(permut[i], stpg.stateOwners.get(i));
		}
	}

	/**
	 * Copy constructor
	 */
	public STPGExplicit(STPGExplicit stpg)
	{
		super(stpg);
		stateOwners = new ArrayList<Integer>(stpg.stateOwners);
	}

	// Mutators (for ModelSimple)
	
	/**
	 * Add a new (player 1) state and return its index.
	 */
	@Override
	public int addState()
	{
		return addState(1);
	}

	/**
	 * Add multiple new (player 1) states.
	 */
	@Override
	public void addStates(int numToAdd)
	{
		super.addStates(numToAdd);
		for (int i = 0; i < numToAdd; i++)
			stateOwners.add(1);
	}

	/**
	 * Add a new (player {@code p}) state and return its index. For an STPG, {@code p} should be 1 or 2. 
	 * @param p Player who owns the new state.
	 */
	public int addState(int p)
	{
		super.addStates(1);
		stateOwners.add(p);
		return numStates - 1;
	}

	/**
	 * Add multiple new states, with owners as given in the list {@code p}
	 * (the number of states to add is dictated by the length of the list).
	 * For an STPG, player indices should be 1 or 2.
	 * @param p List of players owning each new state
	 */
	public void addStates(List<Integer> p)
	{
		super.addStates(p.size());
		stateOwners.addAll(p);
	}

	/**
	 * Set player {@code p} to own state {@code s}. For an STPG, {@code} should be 1 or 2.
	 * It is not checked whether {@code s} or {@code p} are in the correct range.
	 */
	public void setPlayer(int s, int p)
	{
		stateOwners.set(s, p);
	}

	// Accessors (for Model)

	@Override
	public ModelType getModelType()
	{
		return ModelType.STPG;
	}

	// Accessors (for STPG)

	@Override
	public int getPlayer(int s)
	{
		return stateOwners.get(s);
	}

	@Override
	public int getNumTransitions(int s, int i)
	{
		return super.getNumTransitions(s, i);
	}

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(int s, int i)
	{
		return super.getTransitionsIterator(s, i);
	}

	@Override
	public boolean isChoiceNested(int s, int i)
	{
		// No nested choices
		return false;
	}

	@Override
	public int getNumNestedChoices(int s, int i)
	{
		// No nested choices
		return 0;
	}

	@Override
	public Object getNestedAction(int s, int i, int j)
	{
		// No nested choices
		return null;
	}

	@Override
	public int getNumNestedTransitions(int s, int i, int j)
	{
		// No nested choices
		return 0;
	}

	@Override
	public Iterator<Entry<Integer, Double>> getNestedTransitionsIterator(int s, int i, int j)
	{
		// No nested choices
		return null;
	}

	@Override
	public void prob0step(BitSet subset, BitSet u, boolean forall1, boolean forall2, BitSet result)
	{
		int i;
		boolean b1, b2;
		boolean forall = false;

		for (i = 0; i < numStates; i++) {
			if (subset.get(i)) {
				forall = (getPlayer(i) == 1) ? forall1 : forall2;
				b1 = forall; // there exists or for all
				for (Distribution distr : trans.get(i)) {
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
				forall = (getPlayer(i) == 1) ? forall1 : forall2;
				b1 = forall; // there exists or for all
				for (Distribution distr : trans.get(i)) {
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
				min = (getPlayer(s) == 1) ? min1 : min2;
				result[s] = mvMultMinMaxSingle(s, vect, min, adv);
			}
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1)) {
				min = (getPlayer(s) == 1) ? min1 : min2;
				result[s] = mvMultMinMaxSingle(s, vect, min, adv);
			}
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1)) {
				min = (getPlayer(s) == 1) ? min1 : min2;
				result[s] = mvMultMinMaxSingle(s, vect, min, adv);
			}
		}
	}

	@Override
	public double mvMultMinMaxSingle(int s, double vect[], boolean min1, boolean min2)
	{
		boolean min = (getPlayer(s) == 1) ? min1 : min2;
		return mvMultMinMaxSingle(s, vect, min, null);
	}

	@Override
	public List<Integer> mvMultMinMaxSingleChoices(int s, double vect[], boolean min1, boolean min2, double val)
	{
		boolean min = (getPlayer(s) == 1) ? min1 : min2;
		return mvMultMinMaxSingleChoices(s, vect, min, val);
	}

	@Override
	public double mvMultGSMinMax(double vect[], boolean min1, boolean min2, BitSet subset, boolean complement, boolean absolute)
	{
		int s;
		double d, diff, maxDiff = 0.0;
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++) {
				d = mvMultJacMinMaxSingle(s, vect, min1, min2);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1)) {
				d = mvMultJacMinMaxSingle(s, vect, min1, min2);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1)) {
				d = mvMultJacMinMaxSingle(s, vect, min1, min2);
				diff = absolute ? (Math.abs(d - vect[s])) : (Math.abs(d - vect[s]) / d);
				maxDiff = diff > maxDiff ? diff : maxDiff;
				vect[s] = d;
			}
		}
		return maxDiff;
	}

	@Override
	public double mvMultJacMinMaxSingle(int s, double vect[], boolean min1, boolean min2)
	{
		boolean min = (getPlayer(s) == 1) ? min1 : min2;
		return mvMultJacMinMaxSingle(s, vect, min, null);
	}

	@Override
	public void mvMultRewMinMax(double vect[], STPGRewards rewards, boolean min1, boolean min2, double result[], BitSet subset, boolean complement, int adv[])
	{
		int s;
		boolean min = false;
		MDPRewards mdpRewards = rewards.buildMDPRewards();
		// Loop depends on subset/complement arguments
		if (subset == null) {
			for (s = 0; s < numStates; s++) {
				min = (getPlayer(s) == 1) ? min1 : min2;
				result[s] = mvMultRewMinMaxSingle(s, vect, mdpRewards, min, adv, 1.0);
			}
		} else if (complement) {
			for (s = subset.nextClearBit(0); s < numStates; s = subset.nextClearBit(s + 1)) {
				min = (getPlayer(s) == 1) ? min1 : min2;
				result[s] = mvMultRewMinMaxSingle(s, vect, mdpRewards, min, adv, 1.0);
			}
		} else {
			for (s = subset.nextSetBit(0); s >= 0; s = subset.nextSetBit(s + 1)) {
				min = (getPlayer(s) == 1) ? min1 : min2;
				result[s] = mvMultRewMinMaxSingle(s, vect, mdpRewards, min, adv, 1.0);
			}
		}
	}

	@Override
	public double mvMultRewMinMaxSingle(int s, double vect[], STPGRewards rewards, boolean min1, boolean min2, int adv[])
	{
		MDPRewards mdpRewards = rewards.buildMDPRewards();
		boolean min = (getPlayer(s) == 1) ? min1 : min2;
		return mvMultRewMinMaxSingle(s, vect, mdpRewards, min, adv);
	}

	@Override
	public List<Integer> mvMultRewMinMaxSingleChoices(int s, double vect[], STPGRewards rewards, boolean min1, boolean min2, double val)
	{
		MDPRewards mdpRewards = rewards.buildMDPRewards();
		boolean min = (getPlayer(s) == 1) ? min1 : min2;
		return mvMultRewMinMaxSingleChoices(s, vect, mdpRewards, min, val);
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
			s += i + "(P-" + getPlayer(i) + "): ";
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

	/**
	 * Allows discounting
	 * @param s
	 * @param vect
	 * @param mdpRewards
	 * @param min
	 * @param adv
	 * @param disc
	 * @return
	 */
	public double mvMultRewMinMaxSingle(int s, double vect[], MDPRewards mdpRewards, boolean min, int adv[], double disc)
	{
		int j, k, advCh = -1;
		double d, prob, minmax;
		boolean first;
		List<Distribution> step;

		minmax = 0;
		first = true;
		j = -1;
		step = trans.get(s);
		for (Distribution distr : step) {
			j++;

			// Compute sum for this distribution
			d = mdpRewards.getTransitionReward(s, j);

			for (Map.Entry<Integer, Double> e : distr) {
				k = (Integer) e.getKey();
				prob = (Double) e.getValue();
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
}
