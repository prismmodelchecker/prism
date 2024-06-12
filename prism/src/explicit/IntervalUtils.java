//==============================================================================
//	
//	Copyright (c) 2022-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

import java.util.Iterator;
import java.util.Map;

import common.Interval;
import prism.Evaluator;

/**
 * Various utility methods for working with intervals
 */
public class IntervalUtils
{
	/**
	 * Extract the details of a distribution over double-valued intervals of probabilities
	 */
	public static DoubleIntervalDistribution extractDoubleIntervalDistribution(Distribution<Interval<Double>> distr)
	{
		return extractDoubleIntervalDistribution(distr.iterator(), distr.size());
	}
	
	/**
	 * Extract the details of a distribution over double-valued intervals of probabilities
	 */
	public static DoubleIntervalDistribution extractDoubleIntervalDistribution(Iterator<Map.Entry<Integer, Interval<Double>>> iter, int size)
	{
		DoubleIntervalDistribution did = new DoubleIntervalDistribution(size);
		int i = 0;
		while (iter.hasNext()) {
			Map.Entry<Integer, Interval<Double>> e = iter.next();
			did.lower[i] = e.getValue().getLower();
			did.upper[i] = e.getValue().getUpper();
			did.index[i] = e.getKey();
			i++;
		}
		return did;
	}
	
	/**
	 * Delimit a distribution with over intervals of probabilities, i.e., trim the bounds of the
	 * intervals such that at least one possible distribution takes each of the extremal values.
	 * The Distribution is modified directly.
	 * @param distr The distribution to delimit
	 * @param eval An evaluator for the interval's child type (Value)
	 */
	public static <Value> void delimit(Distribution<Interval<Value>> distr, Evaluator<Value> eval)
	{
		// Compute 1 minus the sum of all bounds
		Evaluator<Interval<Value>> evalInt = distr.getEvaluator();
		Interval<Value> oneMinusSum = evalInt.subtract(evalInt.one(), distr.sum());
		Value oneMinusSumLower = oneMinusSum.getLower();
		Value oneMinusSumUpper = oneMinusSum.getUpper();
		// For each interval
		Iterator<Map.Entry<Integer, Interval<Value>>> iter = distr.iterator();
		while (iter.hasNext()) {
			Map.Entry<Integer, Interval<Value>> e = iter.next();
			Interval<Value> ival = e.getValue();
			Value lower = ival.getLower();
			Value upper = ival.getUpper();
			// Adjust lower bound if needed
			Value upperTight = eval.add(oneMinusSumUpper, upper);
			if (eval.gt(upperTight, lower)) {
				ival.setLower(upperTight);
			}
			// Adjust upper bound if needed
			Value lowerTight = eval.add(oneMinusSumLower, lower);
			if (eval.gt(upper, lowerTight)) {
				ival.setUpper(lowerTight);
			}
		}
	}
	
	/**
	 * Delimit a distribution with over double-valued intervals of probabilities, i.e., trim the bounds of the
	 * intervals such that at least one possible distribution takes each of the extremal values.
	 * Returns a new Distribution if it changed, and the old one if not.
	 * @param distr The distribution to delimit
	 */
	public Distribution<Interval<Double>> delimitDoubles(Distribution<Interval<Double>> distr)
	{
		DoubleIntervalDistribution did = extractDoubleIntervalDistribution(distr);
		if (delimitDoubles(did)) {
			// Create a new Distribution if delimiting changed it
			Distribution<Interval<Double>> distrNew = new Distribution<>(distr.getEvaluator());
			for (int i = 0; i < did.size; i++) {
				distrNew.add(did.index[i], new Interval<Double>(did.lower[i], did.upper[i]));
			}
			return distrNew;
		}
		return distr;
	}
	
	/**
	 * Delimit a distribution with over double-valued intervals of probabilities, i.e., trim the bounds of the
	 * intervals such that at least one possible distribution takes each of the extremal values.
	 * The DoubleIntervalDistribution is modified directly.
	 * Returns true if changes were needed.
	 * @param did The distribution to delimit
	 */
	public static boolean delimitDoubles(DoubleIntervalDistribution did)
	{
		boolean changed = false;
		for (int i = 0; i < did.size; i++) {
			// Adjust lower bound if needed
			double sumRest = 0.0;
			for (int j = 0; j < did.size; j++) {
				if (j != i) sumRest += did.upper[i];
			}
			if (did.lower[i] < 1.0 - sumRest) {
				did.lower[i] = 1.0 - sumRest;
				changed |= true;
			}
			// Adjust upper bound if needed
			sumRest = 0.0;
			for (int j = 0; j < did.size; j++) {
				if (j != i) sumRest += did.lower[i];
			}
			if (did.upper[i] > 1.0 - sumRest) {
				did.upper[i] = 1.0 - sumRest;
				changed |= true;
			}
		}
		return changed;
	}
}
