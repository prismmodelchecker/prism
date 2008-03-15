//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford)
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

import parser.*;

/**
 * This class is an interface between the chart package and ResultCollection.
 * From a design point of view it is nice if the ResultCollection is oblivious to the fact that
 * charts are generated from the results, and we allow the implementation of ResultListeners.
 * 
 * See {@link  prism.ResultsCollection  ResultCollection}. 
 */
public interface ResultListener 
{
	/**
	 * This method is called whenever a ResultCollection discovers a new result. 
	 * @param resultsCollection The ResultsCollection from which this result is a member.
	 * @param values The parameters of the experiment.
	 * @param result The result of the experiment (any of Exception, Integer, Boolean, Double, null). Be aware that valid Double values include NaN, and plus and minus infinity.
	 */
	public void notifyResult(ResultsCollection resultsCollection, Values values, Object result);
}
