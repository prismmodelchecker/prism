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
	
	/**
	 * Create a pair of a (double) value and an associated {@link Accuracy} object
	 * representing an interval of values, specified by its lower and upper bounds.
	 * Optionally, the accuracy of each bound can be specified. 
	 * @param loVal Lower bound
	 * @param loAcc Lower bound accuracy
	 * @param hiVal Upper bound
	 * @param hiAcc Upper bound accuracy
	 */
	public static Pair<Double,Accuracy> valueAndAccuracyFromInterval(double loVal, Accuracy loAcc, double hiVal, Accuracy hiAcc) throws PrismException 
	{
		// Don't support probabilistic accuracy bounds
		if (loAcc != null && loAcc.getLevel() == AccuracyLevel.PROBABLY_BOUNDED) {
			throw new PrismException("Cannot create interval accuracy from probabilistic bounds");
		}
		if (loAcc != null && loAcc.getLevel() == AccuracyLevel.PROBABLY_BOUNDED) {
			throw new PrismException("Cannot create interval accuracy from probabilistic bounds");
		}
		// Extract lower/upper bounds (taking into account accuracy if present)
		double lo = loAcc == null ? loVal : loAcc.getResultLowerBound(loVal);
		double hi = hiAcc == null ? hiVal : hiAcc.getResultLowerBound(hiVal);
		// Compute new mid point value and error bound
		double mid, err;
		if (Double.isFinite(lo) && Double.isFinite(hi)) {
			mid = (lo + hi) / 2.0;
			err = (hi - lo) / 2.0;
		} else if (Double.isInfinite(lo) && Double.isInfinite(hi)) {
			mid = lo == hi ? lo : 0.0;
			err = lo == hi ? 0.0 : Double.POSITIVE_INFINITY;
		} else {
			mid = err = Double.NaN;
		}
		// Compute accuracy of new result value:
		// "bounded" if lower/upper bounds were provided with bounded accuracy;
		// "estimated bounded" if either bound was estimated or missing;
		// "exactfp" if "bounded" with error 0
		AccuracyLevel accLev;
		if (loAcc == null || loAcc.getLevel() == AccuracyLevel.ESTIMATED_BOUNDED) {
			accLev = AccuracyLevel.ESTIMATED_BOUNDED;
		} else if (hiAcc == null || hiAcc.getLevel() == AccuracyLevel.ESTIMATED_BOUNDED) {
			accLev = AccuracyLevel.ESTIMATED_BOUNDED;
		} else if (err == 0.0) {
			accLev = AccuracyLevel.EXACT_FLOATING_POINT;
		} else {
			accLev = AccuracyLevel.BOUNDED;
		}
		// Return pair
		Accuracy acc = new Accuracy(accLev, err, true);
		return new Pair<>(mid, acc);
	}
}
