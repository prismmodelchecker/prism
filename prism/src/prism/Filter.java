//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

/**
 * Class to store basic information about how results of model checking will be filtered.
 */
public class Filter
{
	// Enums for types of filter
	public enum FilterOperator {
		STATE;
	};
	// Filter operator
	private FilterOperator op = null;
	// State of interest
	private int stateIndex = -1; 

	// Constructors
	
	public Filter(FilterOperator op, int stateIndex)
	{
		setOperator(op);
		setStateIndex(stateIndex);
	}
	
	// Getters/setters
	
	public FilterOperator getOperator()
	{
		return op;
	}
	
	public int getStateIndex()
	{
		return stateIndex;
	}
	
	public void setOperator(FilterOperator op)
	{
		this.op = op;
	}
	
	public void setStateIndex(int stateIndex)
	{
		this.stateIndex = stateIndex;
	}
}

// ------------------------------------------------------------------------------
