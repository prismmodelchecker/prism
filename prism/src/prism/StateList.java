//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
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

package prism;

import java.util.List;

import jdd.*;
import parser.State;
import parser.Values;

/**
 * Interface for classes that store a list of states.
 */
public interface StateList
{
	/**
	 * Get the number of states in the list.
	 */
	int size();
	
	/**
	 * Get the number of states in the list as a string
	 * (useful when the number is too big to fit in an integer).
	 */
	String sizeString();
	
	/**
	 * Print the states to a log.
	 */
	void print(PrismLog log);
	
	/**
	 * Print the first {@code n} states in the list to a log.
	 */
	void print(PrismLog log, int n);
	
	/**
	 * Print the states to a log, in Matlab format.
	 */
	void printMatlab(PrismLog log);
	
	/**
	 * Print the first {@code n} states in the list to a log, in Matlab format.
	 */
	void printMatlab(PrismLog log, int n);

	/**
	 * Print the states to a log, in Dot format.
	 */
	void printDot(PrismLog log);

	/**
	 * Format the list of states as a list of strings.
	 */
	public List<String> exportToStringList();
	
	/**
	 * Check whether a set of states, specified as a BDD, is *partially* included in this list,
	 * i.e. whether there is any intersection between the two sets. 
	 */
	boolean includes(JDDNode set);
	
	/**
	 * Check whether a set of states, specified as a BDD, is *fully* included in this list,
	 * i.e. whether every state in {@code set} is in this list. 
	 */
	boolean includesAll(JDDNode set);
	
	/**
	 * Get the first state in this list, as a {@link parser.Values} object.
	 */
	Values getFirstAsValues() throws PrismException;
	
	/**
	 * Get the index of a state in the list, specified as a State object.
	 * Returns -1 if the state is not on the list or there is a problem with the lookup. 
	 */
	int getIndexOfState(State state);

	/**
	 * Free any memory associated with storing this list of states.
	 */
	void clear();
}
