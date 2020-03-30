//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Xueyi Zou <xz972@york.ac.uk> (University of York)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

import java.util.Arrays;

import prism.PrismUtils;

/**
 * A belief state for a partially observable model.
 * This assumes that states are split into observable and unobservable parts,
 * and so stores (separately) a fixed value for the observable part,
 * and a distribution over the unobservable part.
 * This class just uses indices - the actual observations/unobservations
 * are assumed to be stored elsewhere (in a {@link PartiallyObservableModel}). 
 */
public class Belief implements Comparable<Belief>
{
	/** The state of the observable part of the model **/
	public int so;
	
	/** The probability distribution over the unobservable part of the model **/
	public double[] bu;

	/**
	 * Constructor
	 * @param Observable part (index of observation)
	 * @param Distribution over unobservable part (probability for each unobservation) 
	 */
	public Belief(int so, double[] bu)
	{
		this.so = so;
		this.bu = bu;
	}

	/**
	 * Convert to a probability distribution over all model states
	 * (represented as an array of probabilities). 
	 * @param model The (partially observable) model
	 */
	public double[] toDistributionOverStates(PartiallyObservableModel model)
	{
		double[] distributionOverStates = new double[model.getNumStates()];
		int n = model.getNumStates();
		for (int s = 0; s < n; s++) {
			if (model.getObservation(s) == so) {
				int unobserv = model.getUnobservation(s);
				distributionOverStates[s] = bu[unobserv];
			}
		}
		PrismUtils.normalise(distributionOverStates);
		return distributionOverStates;
	}

	@Override
	public int hashCode()
	{
		return 13 * Arrays.hashCode(bu) + so;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;

		Belief other = (Belief) obj;

		if (so != other.so) {
			return false;
		} else {
			if (bu.length != other.bu.length) {
				return false;
			} else {
				for (int i = 0; i < other.bu.length; i++) {
					if (Math.abs(bu[i] - other.bu[i]) > 1e-6)
						return false;
				}
			}
		}
		return true;
	}

	@Override
	public int compareTo(Belief b)
	{
		// Can't compare to null
		if (b == null)
			throw new NullPointerException();
		// Beliefs of different size are incomparable 
		double[] bbu = b.bu;
		int n = bu.length;
		if (n != bbu.length)
			throw new ClassCastException("Beliefs are different sizes");
		// Compare
		if (so > b.so)
			return 1;
		if (so < b.so)
			return -1;
		for (int i = 0; i < n; i++) {
			if (bu[i] - bbu[i] > 1e-6)
				return 1;
			if (bbu[i] - bu[i] > 1e-6)
				return -1;
		}
		return 0;
	}

	@Override
	public String toString()
	{
		return "{" + so + "}," + Arrays.toString(bu);
	}
}
