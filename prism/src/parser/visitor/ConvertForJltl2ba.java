//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Carlos S. Bederián (Universidad Nacional de Córdoba)
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

import parser.ast.*;
import prism.PrismLangException;
import jltl2ba.*;
import parser.type.*;

/*
 * Convert a property expression (an LTL formula) into the classes used by
 * the jltl2ba (and jltl2dstar) libraries.
 *
 * Optionally, during this conversion, identical subtrees are shared, i.e.,
 * the resulting SimpleLTL structure is a directed acyclic graph (DAG)
 * instead of a tree. This is controlled via the {@code allowSharing} flag.
 *
 * Note that, currently, the jltl2ba LTL to NBA translator requires that the
 * SimpleLTL formula does not share subtrees.
 */
public class ConvertForJltl2ba
{
	/** Flag: allow sharing (produce DAG instead of tree) */
	private boolean allowSharing = true;
	/** Hash map to lookup already translated subformulas (if sharing is allowed) */
	private HashMap<ASTElement, SimpleLTL> formulas = null;

	/** Default constructor (don't allow sharing) */
	public ConvertForJltl2ba()
	{
		this(false);
	}

	/**
	 * Constructor
	 * @param allowSharing Flag: allow sharing (produce DAG instead of tree)
	 */
	public ConvertForJltl2ba(boolean allowSharing)
	{
		this.allowSharing = allowSharing;
		if (allowSharing) {
			formulas = new HashMap<ASTElement, SimpleLTL>();
		}
	}

	/** Convert expression to a SimpleLTL formula */
	public SimpleLTL convert(Expression e) throws PrismLangException
	{
		SimpleLTL res = null;

		if (allowSharing) {
			// if sharing is allowed, lookup the expression and return
			// a previously converted SimpleLTL if available
			res = getFormula(e);
			if (res != null) {
				return res;
			}
		}

		// do the actual conversion
		if (e instanceof ExpressionTemporal) {
			res = convertTemporal((ExpressionTemporal)e);
		} else if (e instanceof ExpressionBinaryOp) {
			res = convertBinaryOp((ExpressionBinaryOp)e);
		} else if (e instanceof ExpressionUnaryOp) {
			res = convertUnaryOp((ExpressionUnaryOp)e);
		} else if (e instanceof ExpressionLiteral) {
			res = convertLiteral((ExpressionLiteral)e);
		} else if (e instanceof ExpressionLabel) {
			res = convertLabel((ExpressionLabel)e);
		}

		if (allowSharing) {
			// store converted formula if sharing is allowed
			setFormula(e, res);
		}
		return res;
	}

	/** Helper: store converted formula in hash map */
	private Object setFormula(ASTElement e, SimpleLTL formula)
	{
		return formulas.put(e, formula);
	}

	/** Helper: lookup formula in hash map */
	private SimpleLTL getFormula(ASTElement e)
	{
		return formulas.get(e);
	}

	/** Convert ExpressionTemporal to a SimpleLTL formula */
	private SimpleLTL convertTemporal(ExpressionTemporal e) throws PrismLangException
	{
		SimpleLTL ltl1 = null, ltl2 = null, res = null;
		Expression until;
		if (e.getOperand1() != null) ltl1 = convert(e.getOperand1());
		if (e.getOperand2() != null) ltl2 = convert(e.getOperand2());
		if (e.hasBounds()) {
			throw new PrismLangException("Can not convert expression with temporal bounds to SimpleLTL: " + e);
		}
		switch (e.getOperator()) {
		case ExpressionTemporal.P_X:
			res = new SimpleLTL(SimpleLTL.LTLType.NEXT, ltl2);
			break;
		case ExpressionTemporal.P_U:
			res = new SimpleLTL(SimpleLTL.LTLType.UNTIL, ltl1, ltl2);
			break;
		case ExpressionTemporal.P_F:
			res = new SimpleLTL(SimpleLTL.LTLType.FINALLY, ltl2);
			break;
		case ExpressionTemporal.P_G:
			res = new SimpleLTL(SimpleLTL.LTLType.GLOBALLY, ltl2);
			break;
		case ExpressionTemporal.P_W:
		case ExpressionTemporal.P_R:
			until = e.convertToUntilForm();
			if (allowSharing) {
				res = getFormula(until);
			}
			if (res == null) {
				// convert normally
				res = convert(until);
			}
			break;
		default:
			throw new PrismLangException("Cannot convert expression to jltl2ba form", e);
		}
		return res;
	}

	/** Convert ExpressionBinaryOp to a SimpleLTL formula */
	private SimpleLTL convertBinaryOp(ExpressionBinaryOp e) throws PrismLangException
	{
		SimpleLTL ltl1 = null, ltl2 = null, res = null;
		if (e.getOperand1() != null) ltl1 = convert(e.getOperand1());
		if (e.getOperand2() != null) ltl2 = convert(e.getOperand2());
		switch (e.getOperator()) {
		case ExpressionBinaryOp.IMPLIES:
			res = new SimpleLTL(SimpleLTL.LTLType.IMPLIES, ltl1, ltl2);
			break;
		case ExpressionBinaryOp.IFF:
			res = new SimpleLTL(SimpleLTL.LTLType.EQUIV, ltl1, ltl2);
			break;
		case ExpressionBinaryOp.OR:
			res = new SimpleLTL(SimpleLTL.LTLType.OR, ltl1, ltl2);
			break;
		case ExpressionBinaryOp.AND:
			res = new SimpleLTL(SimpleLTL.LTLType.AND, ltl1, ltl2);
			break;
		default:
			throw new PrismLangException("Cannot convert expression to jltl2ba form", e);
		}
		return res;
	}

	/** Convert ExpressionUnaryOp to a SimpleLTL formula */
	private SimpleLTL convertUnaryOp(ExpressionUnaryOp e) throws PrismLangException
	{
		SimpleLTL ltl1 = null, res = null;
		if (e.getOperand() != null) ltl1 = convert(e.getOperand());
		switch (e.getOperator()) {
		case ExpressionUnaryOp.NOT:
			res = new SimpleLTL(SimpleLTL.LTLType.NOT, ltl1);
			break;
		case ExpressionUnaryOp.PARENTH:
			res = ltl1;
			break;
		default:
			throw new PrismLangException("Cannot convert expression to jltl2ba form", e);
		}
		return res;
	}

	/** Convert ExpressionLiteral to a SimpleLTL formula */
	private SimpleLTL convertLiteral(ExpressionLiteral e) throws PrismLangException
	{
		if (!(e.getType() instanceof TypeBool)) {
			throw new PrismLangException("Cannot convert expression to jltl2ba form", e);
		}
		return new SimpleLTL(e.evaluateBoolean());
	}

	/** Convert ExpressionLabel to a SimpleLTL formula */
	private SimpleLTL convertLabel(ExpressionLabel e) throws PrismLangException
	{
		return new SimpleLTL(e.getName());
	}
}
