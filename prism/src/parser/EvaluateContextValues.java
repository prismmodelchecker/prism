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
 * Information required to evaluate an expression: Values objects for constants/variables.
 */
public class EvaluateContextValues implements EvaluateContext
{
	private Values constantValues;
	private Values varValues;

	public EvaluateContextValues(Values constantValues, Values varValues)
	{
		this.constantValues = constantValues;
		this.varValues = varValues;
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
		if (varValues == null)
			return null;
		int i = varValues.getIndexOf(name);
		if (i == -1)
			return null;
		return varValues.getValue(i);
	}
}
