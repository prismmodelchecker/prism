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
import java.util.Map.Entry;

import explicit.rewards.MCRewards;

/**
 * Interface for classes that provide (read) access to an explicit-state DTMC.
 */
public interface DTMC extends Model
{
	/**
	 * Get the number of transitions from state s.
	 */
	public int getNumTransitions(int s);

	/**
	 * Get an iterator over the transitions from state s.
	 */
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(int s);

	/**
	 * Perform a single step of precomputation algorithm Prob0, i.e., for states i in {@code subset},
	 * set bit i of {@code result} iff there is a transition to a state in {@code u}.
	 * @param subset Only compute for these states
	 * @param u Set of states {@code u}
	 * @param result Store results here
	 */
	public void prob0step(BitSet subset, BitSet u, BitSet result);

	/**
	 * Perform a single step of precomputation algorithm Prob1, i.e., for states i in {@code subset},
	 * set bit i of {@code result} iff there is a transition to a state in {@code v} and all transitions go to states in {@code u}.
	 * @param subset Only compute for these states
	 * @param u Set of states {@code u}
	 * @param v Set of states {@code v}
	 * @param result Store results here
	 */
	public void prob1step(BitSet subset, BitSet u, BitSet v, BitSet result);

	/**
	 * Do a matrix-vector multiplication for
	 * the DTMC's transition probability matrix P and the vector {@code vect} passed in.
	 * i.e. for all s: result[s] = sum_j P(s,j)*vect[j]
	 * @param vect Vector to multiply by
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 */
	public void mvMult(double vect[], double result[], BitSet subset, boolean complement);

	/**
	 * Do a single row of matrix-vector multiplication for
	 * the DTMC's transition probability matrix P and the vector {@code vect} passed in.
	 * i.e. return sum_j P(s,j)*vect[j]
	 * @param s Row index
	 * @param vect Vector to multiply by
	 */
	public double mvMultSingle(int s, double vect[]);

	/**
	 * Do a Gauss-Seidel-style matrix-vector multiplication for
	 * the DTMC's transition probability matrix P and the vector {@code vect} passed in,
	 * storing new values directly in {@code vect} as computed.
	 * i.e. for all s: vect[s] = (sum_{j!=s} P(s,j)*vect[j]) / (1-P(s,s))
	 * The maximum (absolute/relative) difference between old/new
	 * elements of {@code vect} is also returned.
	 * @param vect Vector to multiply by (and store the result in)
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 * @param absolute If true, compute absolute, rather than relative, difference
	 * @return The maximum difference between old/new elements of {@code vect}
	 */
	public double mvMultGS(double vect[], BitSet subset, boolean complement, boolean absolute);

	/**
	 * Do a single row of Jacobi-style matrix-vector multiplication for
	 * the DTMC's transition probability matrix P and the vector {@code vect} passed in.
	 * i.e. return (sum_{j!=s} P(s,j)*vect[j]) / (1-P(s,s))
	 * @param s Row index
	 * @param vect Vector to multiply by
	 */
	public double mvMultJacSingle(int s, double vect[]);

	/**
	 * Do a matrix-vector multiplication and sum of action reward.
	 * @param vect Vector to multiply by
	 * @param mcRewards The rewards
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows (ignored if null)
	 * @param complement If true, {@code subset} is taken to be its complement (ignored if {@code subset} is null)
	 */
	public void mvMultRew(double vect[], MCRewards mcRewards, double result[], BitSet subset, boolean complement);

	/**
	 * Do a single row of matrix-vector multiplication and sum of action reward.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 * @param mcRewards The rewards
	 */
	public double mvMultRewSingle(int s, double vect[], MCRewards mcRewards);

	/**
	 * Do a vector-matrix multiplication for
	 * the DTMC's transition probability matrix P and the vector {@code vect} passed in.
	 * i.e. for all s: result[s] = sum_i P(i,s)*vect[i]
	 * @param vect Vector to multiply by
	 * @param result Vector to store result in
	 */
	public void vmMult(double vect[], double result[]);
}
