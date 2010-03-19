//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

import java.util.*;

import prism.ModelType;

/**
 * Interface for classes that provide (read-only) access to an explicit-state MDP.
 */
public interface MDP extends Model
{
	// Model type
	public static ModelType modelType = ModelType.MDP;

	/**
	 * Check if any successor states are in a set for all choices of a state.
	 * @param s The state to check
	 * @param set The set to test for inclusion
	 */
	public boolean someSuccessorsInSetForAllChoices(int s, BitSet set);
	
	/**
	 * Check if any successor states are in set1 and all successor states
	 * are in set2 for some choices of a state.
	 * @param s The state to check
	 * @param set1 The set to test for inclusion (some)
	 * @param set2 The set to test for inclusion (all)
	 */
	public boolean someAllSuccessorsInSetForSomeChoices(int s, BitSet set1, BitSet set2);
	
	/**
	 * Check if any successor states are in set1 and all successor states
	 * are in set2 for all choices of a state.
	 * @param s The state to check
	 * @param set1 The set to test for inclusion (some)
	 * @param set2 The set to test for inclusion (all)
	 */
	public boolean someAllSuccessorsInSetForAllChoices(int s, BitSet set1, BitSet set2);
	
	/**
	 * Get the transition reward (if any) for choice i of state s.
	 */
	public double getTransitionReward(int s, int i);

	/**
	 * Do a matrix-vector multiplication followed by min/max, i.e. one step of value iteration.
	 * @param vect Vector to multiply by
	 * @param min Min or max for (true=min, false=max)
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows
	 * @param complement If true, 'subset' is taken to be its complement
	 */
	public void mvMultMinMax(double vect[], boolean min, double result[], BitSet subset, boolean complement);

	/**
	 * Do a single row of matrix-vector multiplication followed by min/max.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param min Min or max for (true=min, false=max)
	 */
	public double mvMultMinMaxSingle(int s, double vect[], boolean min);

	/**
	 * Determine which choices result in min/max after a single row of matrix-vector multiplication.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param min Min or max (true=min, false=max)
	 * @param val Min or max value to match
	 */
	public List<Integer> mvMultMinMaxSingleChoices(int s, double vect[], boolean min, double val);

	/**
	 * Do a matrix-vector multiplication and sum of action reward followed by min/max, i.e. one step of value iteration.
	 * @param vect Vector to multiply by
	 * @param min Min or max for (true=min, false=max)
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows
	 * @param complement If true, 'subset' is taken to be its complement
	 */
	public void mvMultRewMinMax(double vect[], boolean min, double result[], BitSet subset, boolean complement);

	/**
	 * Do a single row of matrix-vector multiplication and sum of action reward followed by min/max.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param min Min or max for (true=min, false=max)
	 */
	public double mvMultRewMinMaxSingle(int s, double vect[], boolean min);

	/**
	 * Determine which choices result in min/max after a single row of matrix-vector multiplication and sum of action reward.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param min Min or max (true=min, false=max)
	 * @param val Min or max value to match
	 */
	public List<Integer> mvMultRewMinMaxSingleChoices(int s, double vect[], boolean min, double val);
}
