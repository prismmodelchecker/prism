//==============================================================================
//	
//	Copyright (c) 2015-
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

import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionSS;
import prism.PrismLangException;

/**
 * Base class for recursively traversing an Expression, but not
 * entering nested P/R/SS subexpressions.
 *
 * By default, will not recurse into the P/R/SS elements, but
 * can be configured to allow a certain amount of nested P/R/SS
 * elements before stopping.
 *
 * <br/>
 * Subclasses should not override<br/>
 * <ul>
 *  <li>{@code visit(ExpressionProb)}</li>
 *  <li>{@code visit(ExpressionReward)}</li>
 *  <li>{@code visit(ExpressionSS)}</li>
 * </ul>
 */
public class ExpressionTraverseNonNested extends ASTTraverse
{
	/** the current nesting level */
	private int currentNesting;
	/** the maximal nesting level that still allows recursion */
	private int nestingLimit;

	/** Constructor, defaulting to "no nesting" */
	public ExpressionTraverseNonNested()
	{
		this(0);
	}

	/** Constructor, with "stop recursion after {@code nestingLimit} nestings" */
	public ExpressionTraverseNonNested(int nestingLimit)
	{
		currentNesting = 0;
		this.nestingLimit = 0;
	}

	/** Are we still allowed to recurse? */
	private boolean inLimit()
	{
		return currentNesting <= nestingLimit;
	}

	@Override
	public Object visit(ExpressionProb e) throws PrismLangException
	{
		currentNesting++;
		// only visit if we are still in limit
		if (!inLimit()) {
			currentNesting--;
			return null;
		}
		Object rv = super.visit(e);
		currentNesting--;
		return rv;
	}

	@Override
	public Object visit(ExpressionReward e) throws PrismLangException
	{
		currentNesting++;
		// only visit if we are still in limit
		if (!inLimit()) {
			currentNesting--;
			return null;
		}
		Object rv = super.visit(e);
		currentNesting--;
		return rv;
	}

	@Override
	public Object visit(ExpressionSS e) throws PrismLangException
	{
		currentNesting++;
		// only visit if we are still in limit
		if (!inLimit()) {
			currentNesting--;
			return null;
		}
		Object rv = super.visit(e);
		currentNesting--;
		return rv;
	}
}
