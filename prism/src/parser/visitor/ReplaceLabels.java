//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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


package parser.visitor;

import java.util.HashMap;
import java.util.Map;

import parser.ast.ExpressionLabel;

/** Visitor for replacing some labels in an expression by other labels */
public class ReplaceLabels extends ASTTraverseModify
{
	/** The replacement map */
	private Map<String, String> replacements;

	/** Constructor for multiple replacements (from -> to) */
	public ReplaceLabels(Map<String, String> replacements)
	{
		this.replacements = replacements;
	}

	/** Constructor for a single replacement */
	public ReplaceLabels(String labelFrom, String labelTo)
	{
		replacements = new HashMap<String,String>();
		replacements.put(labelFrom, labelTo);
	}

	@Override
	public Object visit(ExpressionLabel e)
	{
		if (e.getName() == null)
			return e;

		String replacement = replacements.get(e.getName());
		if (replacement != null)
			return new ExpressionLabel(replacement);

		return e;
	}

}
