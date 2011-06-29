//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Carlos S. Bederián (Universidad Nacional de Córdoba)
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

import java.util.Hashtable;

import parser.ast.*;
import prism.PrismLangException;
import jltl2ba.*;
import parser.type.*;
/*
 * Convert a property expression (an LTL formula) into the classes used by
 * the jltl2ba (and jltl2dstar) libraries.
 */
public class ConvertForJltl2ba extends ASTTraverseModify
{
	private Hashtable<ASTElement, SimpleLTL> formulas;

	public ConvertForJltl2ba()
	{
		formulas = new Hashtable<ASTElement, SimpleLTL>();
	}

	public Object setFormula(ASTElement e, SimpleLTL formula)
	{
		return formulas.put(e, formula);
	}

	public SimpleLTL getFormula(ASTElement e)
	{
		return formulas.get(e);
	}

	public void visitPost(ExpressionTemporal e) throws PrismLangException
	{
		SimpleLTL ltl1 = null, ltl2 = null, res = null;
		Expression until;
		if (e.getOperand1() != null) ltl1 = getFormula(e.getOperand1());
		if (e.getOperand2() != null) ltl2 = getFormula(e.getOperand2());
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
			until.accept(this);
			res = getFormula(until);
			break;
		default:
			throw new PrismLangException("Cannot convert expression to jltl2ba form", e);
		}
		setFormula(e, res);
	}
	
	public void visitPost(ExpressionBinaryOp e) throws PrismLangException
	{
		SimpleLTL ltl1 = null, ltl2 = null, res = null;
		if (e.getOperand1() != null) ltl1 = getFormula(e.getOperand1());
		if (e.getOperand2() != null) ltl2 = getFormula(e.getOperand2());
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
		setFormula(e, res);
	}
	
	public void visitPost(ExpressionUnaryOp e) throws PrismLangException
	{
		SimpleLTL ltl1 = null, res = null;
		if (e.getOperand() != null) ltl1 = getFormula(e.getOperand());
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
		setFormula(e, res);
	}
	
	public void visitPost(ExpressionLiteral e) throws PrismLangException
	{
		if (!(e.getType() instanceof TypeBool)) {
			throw new PrismLangException("Cannot convert expression to jltl2ba form", e);
		}
		setFormula(e, new SimpleLTL(e.evaluateBoolean()));
	}
	
	public void visitPost(ExpressionLabel e) throws PrismLangException
	{
		setFormula(e, new SimpleLTL(e.getName()));
	}
}
