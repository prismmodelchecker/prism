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
 * Interface for classes that provide (read-only) access to an explicit-state DTMC.
 */
public interface DTMC extends Model
{
	// Model type
	public static ModelType modelType = ModelType.DTMC;

	/**
	 * Get the transition reward (if any) for the transitions in state s.
	 */
	public double getTransitionReward(int s);

	/**
	 * Do a matrix-vector multiplication.
	 * @param vect Vector to multiply by
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows
	 * @param complement If true, 'subset' is taken to be its complement
	 */
	public void mvMult(double vect[], double result[], BitSet subset, boolean complement);

	/**
	 * Do a single row of matrix-vector multiplication.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 */
	public double mvMultSingle(int s, double vect[]);

	/**
	 * Do a matrix-vector multiplication and sum of action reward.
	 * @param vect Vector to multiply by
	 * @param result Vector to store result in
	 * @param subset Only do multiplication for these rows
	 * @param complement If true, 'subset' is taken to be its complement
	 */
	public void mvMultRew(double vect[], double result[], BitSet subset, boolean complement);

	/**
	 * Do a single row of matrix-vector multiplication and sum of action reward.
	 * @param s Row index
	 * @param vect Vector to multiply by
	 */
	public double mvMultRewSingle(int s, double vect[]);

}
