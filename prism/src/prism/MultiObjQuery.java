//==============================================================================
//
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a multi-objective model checking query as an ordered list of objectives.
 *
 * <p>Each objective is a typed record ({@link ProbObjective} or {@link RewardObjective})
 * that bundles the original query spec ({@link Objective#opInfo}), the current canonical
 * operator ({@link Objective#op}) and bound ({@link Objective#bound}), a step bound, the
 * original user-facing position, and a negation flag set during canonicalisation.
 *
 * <p>Three views are maintained: all objectives in original order ({@link #getOperator},
 * {@link #getBound}, …), the probabilistic sub-sequence ({@link #getProbOperator}, …),
 * and the reward sub-sequence ({@link #getRewardOperator}, …).
 */
public class MultiObjQuery
{
	// -------------------------------------------------------------------------
	// Inner classes
	// -------------------------------------------------------------------------

	/**
	 * Base class for a single multi-objective operand.
	 */
	public static abstract class Objective
	{
		/** Original query spec (operator type, relop, numeric flag, raw bound). */
		public final OpRelOpBound opInfo;
		/** Position (0-based) of this objective in the user's {@code multi(...)} call. */
		public final int origPosition;
		/** Time/step bound, or -1 if unbounded. */
		public final int stepBound;
		/** Current canonical operator; mutated by {@link MultiObjQuery#makeAllProbUp()} /
		 *  {@link MultiObjQuery#makeAllRewardUp()}. */
		Operator op;
		/** Current bound; may be negated by {@link MultiObjQuery#makeAllRewardUp()} for R_LE. */
		double bound;
		/** True if this objective's operator was flipped during canonicalisation. */
		boolean negated;

		protected Objective(OpRelOpBound opInfo, Operator op, double bound, int stepBound, int origPosition)
		{
			this.opInfo = opInfo;
			this.op = op;
			this.bound = bound;
			this.stepBound = stepBound;
			this.origPosition = origPosition;
		}

		/** Returns true iff this is a probabilistic (P) objective. */
		public abstract boolean isProbabilistic();
	}

	/** A probabilistic (P) objective. */
	public static final class ProbObjective extends Objective
	{
		public ProbObjective(OpRelOpBound opInfo, Operator op, double bound, int stepBound, int origPosition)
		{
			super(opInfo, op, bound, stepBound, origPosition);
		}

		@Override
		public boolean isProbabilistic() { return true; }
	}

	/** A reward (R) objective. */
	public static final class RewardObjective extends Objective
	{
		public RewardObjective(OpRelOpBound opInfo, Operator op, double bound, int stepBound, int origPosition)
		{
			super(opInfo, op, bound, stepBound, origPosition);
		}

		@Override
		public boolean isProbabilistic() { return false; }
	}

	// -------------------------------------------------------------------------
	// State
	// -------------------------------------------------------------------------

	private final List<Objective> objectives;
	private final List<ProbObjective> probObjectives;
	private final List<RewardObjective> rewardObjectives;

	// -------------------------------------------------------------------------
	// Construction
	// -------------------------------------------------------------------------

	public MultiObjQuery()
	{
		this(1);
	}

	public MultiObjQuery(int numObjectives)
	{
		objectives = new ArrayList<>(numObjectives);
		probObjectives = new ArrayList<>(numObjectives);
		rewardObjectives = new ArrayList<>(numObjectives);
	}

	/**
	 * Add one objective to the query.
	 *
	 * @param opInfo       Original operator/relop/bound spec
	 * @param op           Initial canonical operator
	 * @param bound        Working bound (may differ from {@code opInfo.getBound()} after
	 *                     normalisation, e.g. P_LE is flipped to {@code 1-bound})
	 * @param stepBound    Step bound, or -1 if unbounded
	 * @param origPosition 0-based position in the {@code multi(...)} argument list
	 */
	public void add(OpRelOpBound opInfo, Operator op, double bound, int stepBound, int origPosition)
	{
		if (opInfo.isProbabilistic()) {
			ProbObjective obj = new ProbObjective(opInfo, op, bound, stepBound, origPosition);
			objectives.add(obj);
			probObjectives.add(obj);
		} else {
			RewardObjective obj = new RewardObjective(opInfo, op, bound, stepBound, origPosition);
			objectives.add(obj);
			rewardObjectives.add(obj);
		}
	}

	// -------------------------------------------------------------------------
	// Size
	// -------------------------------------------------------------------------

	/** Returns the total number of objectives. */
	public int size() { return objectives.size(); }

	/** Returns the number of probabilistic objectives. */
	public int probSize() { return probObjectives.size(); }

	/** Returns the number of reward objectives. */
	public int rewardSize() { return rewardObjectives.size(); }

	// -------------------------------------------------------------------------
	// Full-list accessors (indexed over all objectives in original order)
	// -------------------------------------------------------------------------

	/** Returns true iff the i-th objective is probabilistic. */
	public boolean isProbabilityObjective(int i) { return objectives.get(i).isProbabilistic(); }

	/** Returns the current canonical operator of the i-th objective. */
	public Operator getOperator(int i) { return objectives.get(i).op; }

	/** Returns the current bound of the i-th objective. */
	public double getBound(int i) { return objectives.get(i).bound; }

	/** Returns the step bound of the i-th objective (-1 if unbounded). */
	public int getStepBound(int i) { return objectives.get(i).stepBound; }

	/** Returns the original query spec of the i-th objective. */
	public OpRelOpBound getOpRelOpBound(int i) { return objectives.get(i).opInfo; }

	// -------------------------------------------------------------------------
	// Prob-only accessors (indexed within the probabilistic sub-sequence)
	// -------------------------------------------------------------------------

	/** Returns the current canonical operator of the i-th probabilistic objective. */
	public Operator getProbOperator(int i) { return probObjectives.get(i).op; }

	/** Returns the current bound of the i-th probabilistic objective. */
	public double getProbBound(int i) { return probObjectives.get(i).bound; }

	/** Returns the step bound of the i-th probabilistic objective. */
	public int getProbStepBound(int i) { return probObjectives.get(i).stepBound; }

	/** Returns the original {@code multi(...)} position of the i-th probabilistic objective. */
	public int getOrigPositionProb(int i) { return probObjectives.get(i).origPosition; }

	/**
	 * True if the i-th probabilistic objective was negated during canonicalisation
	 * (i.e. it was originally P_MIN or P_LE; see {@link #makeAllProbUp()}).
	 */
	public boolean isProbNegated(int i) { return probObjectives.get(i).negated; }

	/** Returns step bounds for all probabilistic objectives as a primitive array. */
	public int[] getProbStepBounds()
	{
		int[] result = new int[probObjectives.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = probObjectives.get(i).stepBound;
		return result;
	}

	// -------------------------------------------------------------------------
	// Reward-only accessors (indexed within the reward sub-sequence)
	// -------------------------------------------------------------------------

	/** Returns the current canonical operator of the i-th reward objective. */
	public Operator getRewardOperator(int i) { return rewardObjectives.get(i).op; }

	/** Returns the current bound of the i-th reward objective. */
	public double getRewardBound(int i) { return rewardObjectives.get(i).bound; }

	/** Returns the step bound of the i-th reward objective. */
	public int getRewardStepBound(int i) { return rewardObjectives.get(i).stepBound; }

	/** Returns the original {@code multi(...)} position of the i-th reward objective. */
	public int getOrigPositionReward(int i) { return rewardObjectives.get(i).origPosition; }

	/**
	 * True if the i-th reward objective was negated during canonicalisation
	 * (i.e. it was originally R_MIN or R_LE; see {@link #makeAllRewardUp()}).
	 */
	public boolean isRewardNegated(int i) { return rewardObjectives.get(i).negated; }

	/** Returns step bounds for all reward objectives as a primitive array. */
	public int[] getRewardStepBounds()
	{
		int[] result = new int[rewardObjectives.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = rewardObjectives.get(i).stepBound;
		return result;
	}

	// -------------------------------------------------------------------------
	// Canonicalisation
	// -------------------------------------------------------------------------

	/**
	 * Replace P_MIN with P_MAX and P_LE with P_GE in probabilistic objectives.
	 * Records which objectives were flipped via {@link #isProbNegated}.
	 */
	public void makeAllProbUp()
	{
		for (ProbObjective obj : probObjectives) {
			if (obj.op == Operator.P_MIN) {
				obj.op = Operator.P_MAX;
				obj.negated = true;
			} else if (obj.op == Operator.P_LE) {
				obj.op = Operator.P_GE;
				obj.negated = true;
			}
		}
	}

	/**
	 * Replace R_MIN with R_MAX and R_LE with R_GE in reward objectives, negating the
	 * stored bound for R_LE objectives. Records which objectives were flipped via
	 * {@link #isRewardNegated}. Must be called after the corresponding reward DDs have
	 * been negated (multiplied by -1).
	 */
	public void makeAllRewardUp()
	{
		for (RewardObjective obj : rewardObjectives) {
			if (obj.op == Operator.R_MIN) {
				obj.op = Operator.R_MAX;
				obj.negated = true;
			} else if (obj.op == Operator.R_LE) {
				obj.op = Operator.R_GE;
				obj.bound = -obj.bound;
				obj.negated = true;
			}
		}
	}

	// -------------------------------------------------------------------------
	// Miscellaneous
	// -------------------------------------------------------------------------

	/** Returns true if any objective currently has canonical operator {@code op}. */
	public boolean contains(Operator op)
	{
		for (Objective obj : objectives)
			if (obj.op == op) return true;
		return false;
	}

	/** Returns the number of numerical (=?) objectives. */
	public int numberOfNumerical()
	{
		int num = 0;
		for (Objective obj : objectives)
			if (obj.opInfo.isNumeric()) num++;
		return num;
	}

	/** Returns the number of step-bounded (≤k) objectives. */
	public int numberOfStepBounded()
	{
		int num = 0;
		for (Objective obj : objectives)
			if (obj.stepBound != -1) num++;
		return num;
	}

	@Override
	public String toString()
	{
		StringBuilder ret = new StringBuilder();
		for (int i = 0; i < objectives.size(); i++) {
			if (i > 0) ret.append(",");
			ret.append(objectives.get(i).opInfo);
			ret.append(objectives.get(i).stepBound);
		}
		return ret.toString();
	}
}
