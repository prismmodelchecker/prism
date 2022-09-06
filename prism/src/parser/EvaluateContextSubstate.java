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
 * Information required to evaluate an expression: a subset of a State object.
 * More precisely: a State object, indexed over a subset of all variables,
 * and a mapping from indices (over all variables) to this subset (-1 if not in subset). 
 * Optionally values for constants can also be supplied.
 */
public class EvaluateContextSubstate implements EvaluateContext
{
	private Values constantValues;
	private Object[] varValues;
	private int[] varMap;

	public EvaluateContextSubstate(State substate, int[] varMap)
	{
		this.constantValues = null;
		this.varValues = substate.varValues;
		this.varMap = varMap;
	}

	public EvaluateContextSubstate(Values constantValues, State substate, int[] varMap)
	{
		this.constantValues = constantValues;
		this.varValues = substate.varValues;
		this.varMap = varMap;
	}

	public Object getConstantValue(String name)
	{
		if (constantValues == null)
			return null;
		int i = constantValues.getIndexOf(name);
		if (i == -1)
			return null;
		return constantValues.getValue(i);
	}

	public Object getVarValue(String name, int index)
	{
		// Use indices to look up value
		int newIndex;
		if (index == -1 || (newIndex = varMap[index]) == -1)
			return null;
		return varValues[newIndex];
	}
}
