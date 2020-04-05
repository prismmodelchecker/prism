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

package prism;

import prism.Accuracy.AccuracyLevel;

/**
 * Class to create {@link Accuracy} objects in a consistent way
 * for various types of numerical computation. 
 */
public class AccuracyFactory
{
	/**
	 * Create an {@link Accuracy} object for value iteration like numerical methods
	 * where there is information about convergence but no guaranteed error bound.
	 * @param convEps Value of epsilon used to check convergence
	 * @param maxDiff Final max difference between successive solution vectors
	 * @param absolute Whether the epsilon/difference are absolute (rather than relative)
	 */
	public static Accuracy valueIteration(double convEps, double maxDiff, boolean absolute)
	{
		// If the difference is actual zero (can happen e.g. in acyclic models), it's precise
		if (maxDiff == 0.0) {
			return new Accuracy(AccuracyLevel.EXACT_FLOATING_POINT);
		}
		// Otherwise, for now, we simply use 10 x the maximum difference
		// as an (unreliable) estimate of the error bound. 
		else {
			double errorBoundEstim = 10 * maxDiff;
			return new Accuracy(AccuracyLevel.ESTIMATED_BOUNDED, errorBoundEstim, absolute);
		}
	}
	
	/**
	 * Create an {@link Accuracy} object for iterative numerical methods which provide
	 * guaranteed error bounds on the results., e.g. interval iteration.
	 * @param errorBound Error bound
	 * @param absolute Whether the error bound is absolute (rather than relative)
	 */
	public static Accuracy guaranteedNumericalIterative(double errorBound, boolean absolute)
	{
		// If the difference is actual zero (can happen e.g. in acyclic models), it's precise
		if (errorBound == 0.0) {
			return new Accuracy(AccuracyLevel.EXACT_FLOATING_POINT);
		}
		// Otherwise use the provided error bound
		else {
			return new Accuracy(AccuracyLevel.BOUNDED, errorBound, absolute);
		}
	}
	
	/**
	 * Create an {@link Accuracy} object for iterative numerical methods which use
	 * a fixed number of iterations (e.g., finite-horizon variants).
	 * We assume this gives "exact" results, ignoring floating point issues.
	 */
	public static Accuracy boundedNumericalIterations()
	{
		return new Accuracy(AccuracyLevel.EXACT_FLOATING_POINT);
	}
	
	/**
	 * Create an {@link Accuracy} object for the case where floating-point results
	 * are returned but have been derived from exact qualitative results, e.g., from
	 * graph-based precomputation methods that determine 0/1-probability cases.
	 */
	public static Accuracy doublesFromQualitative()
	{
		return new Accuracy(AccuracyLevel.EXACT_FLOATING_POINT);
	}
}
