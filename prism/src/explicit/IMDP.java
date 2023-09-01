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
import java.util.Map;

import common.Interval;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismException;

/**
 * Interface for classes that provide (read) access to an explicit-state interval MDP.
 */
public interface IMDP<Value> extends UMDP<Value>
{
	// Accessors (for Model) - default implementations
	
	@Override
	public default ModelType getModelType()
	{
		return ModelType.IMDP;
	}

	// Accessors

	/**
	 * Get an Evaluator for intervals of Value.
	 * A default implementation tries to create one from the main iterator
	 * (which itself by default exists for the (usual) case when Value is Double).
	 */
	@SuppressWarnings("unchecked")
	public default Evaluator<Interval<Value>> getIntervalEvaluator() throws PrismException
	{
		return getEvaluator().createIntervalEvaluator();
	}

	/**
	 * Get an iterator over the (interval) transitions from choice {@code i} of state {@code s}.
	 */
	public Iterator<Map.Entry<Integer, Interval<Value>>> getIntervalTransitionsIterator(int s, int i);

	/**
	 * Get the (interval) MDP representing this IMDP.
	 */
	public MDP<Interval<Value>> getIntervalModel();
}
