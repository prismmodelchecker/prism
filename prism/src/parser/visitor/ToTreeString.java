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

package parser.visitor;

import parser.ast.*;
import prism.PrismLangException;

public class ToTreeString extends ASTTraverse
{
	private int depth = 0;
	private StringBuffer buf = new StringBuffer();
	
	/**
	 * Get the created string representing the tree.
	 */
	public String getString()
	{
		return buf.toString();
	}
	
	public void defaultVisitPre(ASTElement node) throws PrismLangException
	{
		// Don't print full toString() for multi-line objects
		String s = node.toString();
		if (s.indexOf('\n') != -1) s = "";
		print(node.getClass().getName() + " : " + s);
		depth++;
	}
	
	public void defaultVisitPost(ASTElement node) throws PrismLangException
	{
		depth--;
	}

	/**
	 * Utility function to add an indented string to the string buffer
	 */
	private void print(String s)
	{
		for (int i = 0; i < depth; i++) buf.append(" ");
		buf.append(s + "\n");
	}
}

