//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Aistis Simaitis <aistis.aimaitis@cs.ox.ac.uk> (University of Oxford)
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

package strat;

import prism.PrismLog;

/**
 * Interface for classes to store strategies (for MDPs, games, etc.)
 */
public interface Strategy
{
	// Types of info stored for each choice
	public enum Choice {
		INDEX, ACTION, UNKNOWN, ARBITRARY, UNREACHABLE;
	};
	
	/**
	 * Export the strategy to a PrismLog, displaying strategy choices as action names.
	 */
	public void exportActions(PrismLog out);
	
	/**
	 * Clear storage of the strategy.
	 */
	public void clear();
}
