//==============================================================================
//
//	Copyright (c) 2026-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

import parser.ast.Expression;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionSS;
import parser.ast.ExpressionStrategy;
import prism.PrismLangException;

/**
 * Visitor to replace each subformula nested inside a P/R/S operator (outermost ones only)
 * that is itself a P/R/S operator or strategy quantifier ({@code <<>>}, {@code [[]]})
 * with a new label. For example, {@code P=? [ F P>0.5 [ F "goal" ] ]} becomes
 * {@code P=? [ F "_nested0" ]}. The replaced subformulas are available afterwards
 * via {@link #getReplacements()}, e.g., to evaluate them on a different model.
 */
public class ReplaceNestedOperatorsByLabels extends ASTTraverseModify
{
	/** Prefix for new label names */
	private String prefix;
	/** Which label names are already used (and so should be avoided) */
	private Predicate<String> nameUsed;
	/** Replacements made so far (label name -> subformula), in order */
	private Map<String, Expression> replacements = new LinkedHashMap<>();
	/** Current depth of nesting inside P/R/S operators */
	private int depth = 0;

	/**
	 * Constructor.
	 * @param prefix Prefix for new label names (followed by 0, 1, ...)
	 * @param nameUsed Which label names are already used (and so should be avoided)
	 */
	public ReplaceNestedOperatorsByLabels(String prefix, Predicate<String> nameUsed)
	{
		this.prefix = prefix;
		this.nameUsed = nameUsed;
	}

	/**
	 * Get the replacements made (label name -> subformula), in the order they were made.
	 */
	public Map<String, Expression> getReplacements()
	{
		return Collections.unmodifiableMap(replacements);
	}

	@Override
	public Object visit(ExpressionProb e) throws PrismLangException
	{
		if (depth > 0) {
			return replace(e);
		}
		depth++;
		Object res = super.visit(e);
		depth--;
		return res;
	}

	@Override
	public Object visit(ExpressionReward e) throws PrismLangException
	{
		if (depth > 0) {
			return replace(e);
		}
		depth++;
		Object res = super.visit(e);
		depth--;
		return res;
	}

	@Override
	public Object visit(ExpressionSS e) throws PrismLangException
	{
		if (depth > 0) {
			return replace(e);
		}
		depth++;
		Object res = super.visit(e);
		depth--;
		return res;
	}

	@Override
	public Object visit(ExpressionStrategy e) throws PrismLangException
	{
		// A top-level strategy quantifier is not a nesting level itself
		return depth > 0 ? replace(e) : super.visit(e);
	}

	/**
	 * Replace a subformula with a new label.
	 */
	private Expression replace(Expression e)
	{
		String name;
		int i = replacements.size();
		do {
			name = prefix + i++;
		} while (nameUsed.test(name) || replacements.containsKey(name));
		replacements.put(name, e);
		ExpressionLabel label = new ExpressionLabel(name);
		label.setPosition(e);
		return label;
	}
}
