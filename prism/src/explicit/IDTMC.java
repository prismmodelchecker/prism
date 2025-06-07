//==============================================================================
//
//	Copyright (c) 2020-
//	Authors:
//	* Dave Parker <david.parker@cs.oc.ac.uk> (University of Oxford)
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

import common.Interval;
import prism.ModelType;
import prism.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Interface for classes that provide (read) access to an explicit-state interval DTMC.
 */
public interface IDTMC<Value> extends UDTMC<Value>, IntervalModel<Value>
{
	// Accessors (for Model) - default implementations

	@Override
	default ModelType getModelType()
	{
		return ModelType.IDTMC;
	}

	// Accessors

	/**
	 * Get an iterator over the (interval) transitions from state {@code s}.
	 */
	Iterator<Map.Entry<Integer, Interval<Value>>> getIntervalTransitionsIterator(int s);

	/**
	 * Get an iterator over the (interval) transitions from state {@code s}, with their attached actions if present.
	 */
	Iterator<Map.Entry<Integer, Pair<Interval<Value>, Object>>> getIntervalTransitionsAndActionsIterator(int s);

	/**
	 * Get the (interval) DTMC representing this IDTMC.
	 */
	DTMC<Interval<Value>> getIntervalModel();

	/**
	 * Do a single row of matrix-vector multiplication followed by min/max,
	 * i.e. return min/max_P { sum_j P(s,j)*vect[j] }
	 * @param did The transition probability intervals
	 * @param vect Vector to multiply by
	 * @param minMax Min/max uncertainty (via isMinUnc/isMaxUnc)
	 */
	static double mvMultUncSingle(DoubleIntervalDistribution did, double vect[], MinMax minMax)
	{
		// Trivial case: singleton interval (which must be [1.0,1.0])
		if (did.size == 1) {
			return vect[did.index[0]];
		}

		// Avoid enumeration of all extreme distributions using optimisation from:
		// Three-valued abstraction for probabilistic systems,
		// Joost-Pieter Katoen, Daniel Klink, Martin Leucker and Verena Wolf
		// (Defn 17, p.372, and p.380)

		// Get a list of indices for the transitions,
		// sorted according to the successor values
		List<Integer> indices = new ArrayList<>();
		for (int i = 0; i < did.size; i++) {
			indices.add(i);
		}
		if (minMax.isMaxUnc()) {
			Collections.sort(indices, (o1, o2) -> -Double.compare(vect[did.index[o1]], vect[did.index[o2]]));
		} else {
			Collections.sort(indices, (o1, o2) -> Double.compare(vect[did.index[o1]], vect[did.index[o2]]));
		}
		// First add products of probability lower bounds and successor values
		double res = 0.0;
		double totP = 1.0;
		for (int i = 0; i < did.size; i++) {
			res += vect[did.index[i]] * did.lower[i];
			totP -= did.lower[i];
		}
		// Then add remaining ones in descending order
		for (int i = 0; i < did.size; i++) {
			int j = indices.get(i);
			double delta = did.upper[j] - did.lower[j];
			if (delta < totP) {
				res += delta * vect[did.index[j]];
				totP -= delta;
			} else {
				res += totP * vect[did.index[j]];
				break;
			}
		}
		return res;
	}
}
