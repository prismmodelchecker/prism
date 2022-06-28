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
 * where the values for variables are stored in a Values object.
 * Optionally, values for constants can also be stored and used.
 */
public class EvaluateContextValues extends EvaluateContext
{
	private Values varValues;

	public EvaluateContextValues(Values constantValues, Values varValues)
	{
		setConstantValues(constantValues);
		this.varValues = varValues;
	}

	@Override
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
