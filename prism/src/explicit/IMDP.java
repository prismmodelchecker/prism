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

package explicit;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import common.Interval;
import parser.State;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismException;

/**
 * Interface for classes that provide (read) access to an explicit-state interval MDP.
 */
public interface IMDP<Value> extends MDP<Interval<Value>>
{
	// Accessors (for Model) - default implementations
	
	@Override
	public default ModelType getModelType()
	{
		return ModelType.IMDP;
	}

	// Accessors
	
	/**
	 * Checks that transition probability interval lower bounds are positive
	 * and throws an exception if any are not.
	 */
	public default void checkLowerBoundsArePositive() throws PrismException
	{
		Evaluator<Interval<Value>> eval = getEvaluator();
		int numStates = getNumStates();
		for (int s = 0; s < numStates; s++) {
			int numChoices = getNumChoices(s);
			for (int j = 0; j < numChoices; j++) {
				Iterator<Map.Entry<Integer, Interval<Value>>> iter = getTransitionsIterator(s, j);
				while (iter.hasNext()) {
					Map.Entry<Integer, Interval<Value>> e = iter.next();
					// NB: we phrase the check as an operation on intervals, rather than
					// accessing the lower bound directly, to make use of the evaluator
					if (!eval.gt(e.getValue(), eval.zero())) {
						List<State> sl = getStatesList();
						String state = sl == null ? "" + s : sl.get(s).toString();
						throw new PrismException("Transition probability has lower bound of 0 in state " + state);
					}
				}
			}
		}
	}
}
