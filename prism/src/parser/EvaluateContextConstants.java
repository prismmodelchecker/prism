//==============================================================================
//	
//	Copyright (c) 2018-
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

package parser;

/**
 * Information required to evaluate an expression, where no variable value info is available.
 * Constant values (if needed/present) are stored in a Values object.
 */
public class EvaluateContextConstants implements EvaluateContext
{
	private Values constantValues;

	public EvaluateContextConstants(Values constantValues)
	{
		this.constantValues = constantValues;
	}

	@Override
	public Object getConstantValue(String name)
	{
		if (constantValues == null)
			return null;
		int i = constantValues.getIndexOf(name);
		if (i == -1)
			return null;
		return constantValues.getValue(i);
	}

	@Override
	public Object getVarValue(String name, int index)
	{
		// No variable info available
		return null;
	}
}
