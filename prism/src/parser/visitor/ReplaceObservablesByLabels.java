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

import java.util.function.Predicate;

import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionFunc;
import parser.ast.ExpressionITE;
import parser.ast.ExpressionObs;
import parser.ast.ExpressionUnaryOp;
import parser.type.TypeBool;
import prism.PrismLangException;

/**
 * Visitor to replace each maximal Boolean-valued propositional subformula that refers to
 * an observable (e.g. {@code "target"} or {@code o=2}) with a new label, e.g., for checking
 * a property of a partially observable model on a model without observations, such as one
 * induced by a strategy. Observables used in a non-Boolean context that cannot be replaced
 * in this way are an error. The expression should have been type-checked.
 */
public class ReplaceObservablesByLabels extends ReplaceSubformulasByLabels
{
	/**
	 * Constructor.
	 * @param prefix Prefix for new label names (followed by 0, 1, ...)
	 * @param nameUsed Which label names are already used (and so should be avoided)
	 */
	public ReplaceObservablesByLabels(String prefix, Predicate<String> nameUsed)
	{
		super(prefix, nameUsed);
	}

	/**
	 * Is {@code e} a Boolean-valued proposition that refers to an observable?
	 */
	private static boolean isObservableProposition(Expression e) throws PrismLangException
	{
		return e.getType() instanceof TypeBool && e.isProposition() && containsObservable(e);
	}

	/**
	 * Does {@code e} refer to an observable?
	 */
	private static boolean containsObservable(Expression e) throws PrismLangException
	{
		boolean[] found = { false };
		e.accept(new ASTTraverse()
		{
			@Override
			public void visitPost(ExpressionObs e)
			{
				found[0] = true;
			}
		});
		return found[0];
	}

	@Override
	public Object visit(ExpressionITE e) throws PrismLangException
	{
		return isObservableProposition(e) ? replace(e) : super.visit(e);
	}

	@Override
	public Object visit(ExpressionBinaryOp e) throws PrismLangException
	{
		return isObservableProposition(e) ? replace(e) : super.visit(e);
	}

	@Override
	public Object visit(ExpressionUnaryOp e) throws PrismLangException
	{
		return isObservableProposition(e) ? replace(e) : super.visit(e);
	}

	@Override
	public Object visit(ExpressionFunc e) throws PrismLangException
	{
		return isObservableProposition(e) ? replace(e) : super.visit(e);
	}

	@Override
	public Object visit(ExpressionObs e) throws PrismLangException
	{
		if (e.getType() instanceof TypeBool) {
			return replace(e);
		}
		throw new PrismLangException("Observable \"" + e.getName() + "\" is not supported here, except in a Boolean-valued proposition", e);
	}
}
