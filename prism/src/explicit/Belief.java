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
import java.util.List;

import parser.State;
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
	 * @param so Observable part (index of observation)
	 * @param bu Distribution over unobservable part (probability for each unobservation)
	 */
	public Belief(int so, double[] bu)
	{
		this.so = so;
		this.bu = bu;
	}

	/**
	 * Constructor
	 * @param dist Distribution over states of a model (probability for each state)
	 * @param model The (partially observable) model
	 * If {@code dist} is a not a valid distribution, the resulting belief will be invalid too.
	 */
	protected Belief(double[] dist, PartiallyObservableModel model)
	{
		so = -1;
		bu = new double[model.getNumUnobservations()];
		for (int s = 0; s < dist.length; s++) {
			if (dist[s] != 0) {
				so = model.getObservation(s);
				bu[model.getUnobservation(s)] += dist[s];
			}
		}
	}
	
	/**
	 * Construct a point distribution over a single model stete
	 * @param s A model state
	 * @param model The (partially observable) model
	 */
	public static Belief pointDistribution(int s, PartiallyObservableModel model)
	{
		int so = model.getObservation(s);
		double[] bu = new double[model.getNumUnobservations()];
		bu[model.getUnobservation(s)] = 1.0;
		return new Belief(so, bu);
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

	/**
	 * Convert to string representation, using observation info,
	 * e.g. "(6),0.5:(8)+0.5:(9)" for an observable variable equal to 6
	 * and an unobservable variable equally likely to be 8 or 9.
	 */
	public String toString(PartiallyObservableModel poModel)
	{
		List<State> unobs = poModel.getUnobservationsList();
		String s = poModel.getObservationsList().get(so).toString();
		boolean first = true;
		for (int i = 0; i < bu.length; i++) {
			if (bu[i] > 0) {
				s += first ? "," : "+";
				s += bu[i] + ":" + unobs.get(i).toString();
				first = false;
			}
		}
		return s;
	}

	@Override
	public String toString()
	{
		return "{" + so + "}," + Arrays.toString(bu);
	}
}
