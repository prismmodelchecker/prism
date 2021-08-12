//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package parser;

/**
 * Information required to evaluate an expression,
 * where the values for variables are stored in a State object.
 * A State is basically just an array of Objects, with no variable name info,
 * so variable indices (i.e., offsets into the State array) need to be provided.
 * Optionally, values for constants can also be stored and used.
 */
public class EvaluateContextState extends EvaluateContext
{
	/**
	 * Variable values for state
	 */
	private Object[] varValues;

	public EvaluateContextState(State state)
	{
		setState(state);
	}

	public EvaluateContextState(Values constantValues, State state)
	{
		setConstantValues(constantValues);
		setState(state);
	}

	/**
	 * Set the state variable values. The array of state values is extracted and stored, not copied.
	 * Returns a copy of this EvaluateContext to allow chaining of method calls.
	 */
	public EvaluateContext setState(State state)
	{
		this.varValues = state.varValues;
		return this;
	}

	@Override
	public Object getVarValue(String name, int index)
	{
		// There is no variable name info available,
		// so use index if provided; otherwise unknown
		return index == -1 ? null : varValues[index];
	}
}
