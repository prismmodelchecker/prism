//==============================================================================
//
//	Copyright (c) 2025-
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

import common.Interval;
import prism.Evaluator;
import prism.PrismException;

public interface IntervalModel<Value> extends Model<Value>
{
	/**
	 * Get an Evaluator for intervals of Value.
	 * A default implementation tries to create one from the main iterator
	 * (which itself by default exists for the (usual) case when Value is Double).
	 */
	default Evaluator<Interval<Value>> getIntervalEvaluator() throws PrismException
	{
		return getEvaluator().createIntervalEvaluator();
	}

	/**
	 * Get the underlying model over Interval<Value>.
	 */
	Model<Interval<Value>> getIntervalModel();
}
