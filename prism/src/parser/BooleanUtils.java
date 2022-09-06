//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionLiteral;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import parser.type.TypeBool;
import parser.visitor.ASTTraverseModify;
import prism.PrismException;
import prism.PrismLangException;

public class BooleanUtils
{
	/**
	 * Extract the conjuncts from a conjunction in the form of zero or more nested
	 * binary conjunctions represented by ExpressionBinaryOp objects.
	 * 
	 * @param expr The conjunction to extract from 
	 * @param conjuncts The list to insert conjuncts into
	 */
	public static void extractConjuncts(Expression expr, List<Expression> conjuncts)
	{
		// Traverse depth-first, and add any non-ands to the list
		if (Expression.isAnd(expr)) {
			extractConjuncts(((ExpressionBinaryOp) expr).getOperand1(), conjuncts);
			extractConjuncts(((ExpressionBinaryOp) expr).getOperand2(), conjuncts);
		} else {
			conjuncts.add(expr);
		}
	}

	/**
	 * Extract the disjuncts from a disjunction in the form of zero or more nested
	 * binary disjunctions represented by ExpressionBinaryOp objects.
	 * 
	 * @param expr The disjunction to extract from 
	 * @param disjuncts The list to insert disjuncts into
	 */
	public static void extractDisjuncts(Expression expr, List<Expression> disjuncts)
	{
		// Traverse depth-first, and add any non-ors to the list
		if (Expression.isAnd(expr)) {
			extractDisjuncts(((ExpressionBinaryOp) expr).getOperand1(), disjuncts);
			extractDisjuncts(((ExpressionBinaryOp) expr).getOperand2(), disjuncts);
		} else {
			disjuncts.add(expr);
		}
	}

	/**
	 * Convert a Boolean expression to positive normal form,
	 * i.e., remove any instances of =>, <=> or () and then push
	 * all negation inwards so that it only occurs on "propositions".
	 * A "proposition" is any Expression object that
	 * is not an operator used to define a Boolean expression (!, &, |, =>, <=>, ()).
	 * The passed in expression is modified, and the result is returned. 
	 */
	public static Expression convertToPositiveNormalForm(Expression expr)
	{
		// First expand implies/iff/parentheses
		// Then do conversion to +ve normal form
		return doConversionToPositiveNormalForm(removeImpliesIffAndParentheses(expr), false);
	}
	
	/**
	 * Convert an LTL formula to positive normal form,
	 * i.e., remove any instances of =>, <=> or () and then push
	 * all negation inwards so that it only occurs on "propositions".
	 * A "proposition" is any Expression object that
	 * is not an operator used to define a Boolean expression (!, &, |, =>, <=>, ())
	 * or a temporal operator (X, U, F, G, R, W).
	 * The passed in expression is modified, and the result is returned. 
	 */
	public static Expression convertLTLToPositiveNormalForm(Expression expr)
	{
		// First expand implies/iff/parentheses
		// Then do conversion to +ve normal form
		return doConversionToPositiveNormalForm(removeImpliesIffAndParentheses(expr), true);
	}
	
	/**
	 * Remove any instances of =>, <=> or () by expanding/deleting as appropriate.
	 */
	private static Expression removeImpliesIffAndParentheses(Expression expr)
	{
		Expression exprMod = null;
		try {
			exprMod = (Expression) expr.accept(new ASTTraverseModify()
			{
				public Object visit(ExpressionUnaryOp e) throws PrismLangException
				{
					// Remove parentheses: (a)
					if (Expression.isParenth(e)) {
						Expression a = (Expression) (e.getOperand().accept(this));
						// (a)  ==  a 
						return a;
					}
					return super.visit(e);
				}

				public Object visit(ExpressionBinaryOp e) throws PrismLangException
				{
					// Remove implication: a => b
					if (Expression.isImplies(e)) {
						Expression a = (Expression) (e.getOperand1().accept(this));
						Expression b = (Expression) (e.getOperand2().accept(this));
						// a => b  ==  !a | b 
						return Expression.Or(Expression.Not(a), b);
					}
					// Remove iff: a <=> b
					if (Expression.isIff(e)) {
						Expression a = (Expression) (e.getOperand1().accept(this));
						Expression b = (Expression) (e.getOperand2().accept(this));
						// a <=> b  ==  (a | !b) & (!a | b) 
						return Expression.And(Expression.Or(a, Expression.Not(b)), Expression.Or(Expression.Not(a.deepCopy()), b.deepCopy()));
					}
					return super.visit(e);
				}
			});
		} catch (PrismLangException e) {
			// Shouldn't happen since we do not throw PrismLangException above
		}
		return exprMod;
	}
	
	/**
	 * Do the main part of the conversion of a Boolean expression to positive normal form,
	 * i.e., push all negation inwards to the propositions. If {@code ltl} is false, it is assumed
	 * that the Boolean expression comprises only the Boolean operators !, & and |.
	 * If {@code ltl} is true, the expression can also contain temporal operators (X, U, F, G, R, W). 
	 * Anything else is treated as a proposition.
	 * The passed in expression is modified, and the result is returned. 
	 */
	private static Expression doConversionToPositiveNormalForm(Expression expr, boolean ltl)
	{
		// Remove negation
		if (Expression.isNot(expr)) {
			Expression neg = ((ExpressionUnaryOp) expr).getOperand();
			// Boolean operators
			if (Expression.isTrue(neg)) {
				// ! true  ==  false
				return new ExpressionLiteral(TypeBool.getInstance(), false);
			} else if (Expression.isFalse(neg)) {
				// ! false  ==  true
				return new ExpressionLiteral(TypeBool.getInstance(), true);
			} else if (Expression.isNot(neg)) {
				Expression a = ((ExpressionUnaryOp) neg).getOperand();
				// !(!a)  ==  a 
				return doConversionToPositiveNormalForm(a, ltl);
			} else if (Expression.isOr(neg)) {
				Expression a = ((ExpressionBinaryOp) neg).getOperand1();
				Expression b = ((ExpressionBinaryOp) neg).getOperand2();
				Expression aNeg = doConversionToPositiveNormalForm(Expression.Not(a), ltl);
				Expression bNeg = doConversionToPositiveNormalForm(Expression.Not(b), ltl);
				// !(a | b)  ==  !a & !b 
				return Expression.And(aNeg, bNeg);
			} else if (Expression.isAnd(neg)) {
				Expression a = ((ExpressionBinaryOp) neg).getOperand1();
				Expression b = ((ExpressionBinaryOp) neg).getOperand2();
				Expression aNeg = doConversionToPositiveNormalForm(Expression.Not(a), ltl);
				Expression bNeg = doConversionToPositiveNormalForm(Expression.Not(b), ltl);
				// !(a & b)  ==  !a | !b 
				return Expression.Or(aNeg, bNeg);
			}
			// Temporal operators (if required)
			else if (ltl) {
				if (neg instanceof ExpressionTemporal) {
					ExpressionTemporal exprTemp = (ExpressionTemporal) neg;
					Expression a = exprTemp.getOperand1();
					Expression b = exprTemp.getOperand2();
					Expression aNeg = null, bNeg = null, aCopy = null, bNegCopy = null;
					ExpressionTemporal result = null;
					switch (exprTemp.getOperator()) {
					case ExpressionTemporal.P_X:
						bNeg = doConversionToPositiveNormalForm(Expression.Not(b), ltl);
						// !(X b)  ==  X !b
						return new ExpressionTemporal(ExpressionTemporal.P_X, null, bNeg);
					case ExpressionTemporal.P_U:
						aNeg = doConversionToPositiveNormalForm(Expression.Not(a), ltl);
						bNeg = doConversionToPositiveNormalForm(Expression.Not(b), ltl);
						// !(a U b)  ==  !a R !b
						result = new ExpressionTemporal(ExpressionTemporal.P_R, aNeg, bNeg);
						result.setBoundsFrom(exprTemp);
						return result;
					case ExpressionTemporal.P_F:
						 bNeg = doConversionToPositiveNormalForm(Expression.Not(b), ltl);
						// !(F b)  ==  G !b
						result = new ExpressionTemporal(ExpressionTemporal.P_G, null, bNeg);
						result.setBoundsFrom(exprTemp);
						return result;
					case ExpressionTemporal.P_G:
						 bNeg = doConversionToPositiveNormalForm(Expression.Not(b), ltl);
						// !(G b)  ==  F !b
						result = new ExpressionTemporal(ExpressionTemporal.P_F, null, bNeg);
						result.setBoundsFrom(exprTemp);
						return result;
					case ExpressionTemporal.P_W:
						aCopy = doConversionToPositiveNormalForm(a.deepCopy(), ltl);
						bNeg = doConversionToPositiveNormalForm(Expression.Not(b), ltl);
						aNeg = doConversionToPositiveNormalForm(Expression.Not(a), ltl);
						bNegCopy = doConversionToPositiveNormalForm(Expression.Not(b.deepCopy()), ltl);
						// !(a W b) == a&!b U !a&!b
						result = new ExpressionTemporal(ExpressionTemporal.P_R, Expression.And(aCopy, bNeg), Expression.And(aNeg, bNegCopy));
						result.setBoundsFrom(exprTemp);
						return result;
					case ExpressionTemporal.P_R:
						aNeg = doConversionToPositiveNormalForm(Expression.Not(a), ltl);
						bNeg = doConversionToPositiveNormalForm(Expression.Not(b), ltl);
						// !(a R b)  ==  !a U !b
						result = new ExpressionTemporal(ExpressionTemporal.P_U, aNeg, bNeg);
						result.setBoundsFrom(exprTemp);
						return result;
					default:
						// Don't change (shouldn't happen)
						return expr;
					}
				}
			} else {
				// Proposition (negated)
				return expr;
			}
		}
		// Preserve and
		if (Expression.isAnd(expr)) {
			Expression a = doConversionToPositiveNormalForm(((ExpressionBinaryOp) expr).getOperand1(), ltl);
			Expression b = doConversionToPositiveNormalForm(((ExpressionBinaryOp) expr).getOperand2(), ltl);
			return Expression.And(a, b);
		}
		// Preserve or
		if (Expression.isOr(expr)) {
			Expression a = doConversionToPositiveNormalForm(((ExpressionBinaryOp) expr).getOperand1(), ltl);
			Expression b = doConversionToPositiveNormalForm(((ExpressionBinaryOp) expr).getOperand2(), ltl);
			return Expression.Or(a, b);
		}
		// Preserve temporal operators
		if (ltl && expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal)expr;
			Expression a = exprTemp.getOperand1();
			Expression b = exprTemp.getOperand2();
			Expression aConv = (a == null) ? null : doConversionToPositiveNormalForm(a, ltl);
			Expression bConv = (b == null) ? null : doConversionToPositiveNormalForm(b, ltl);
			ExpressionTemporal result = new ExpressionTemporal(exprTemp.getOperator(), aConv, bConv);
			result.setBoundsFrom(exprTemp);
			return result;
		}
		// Proposition
		return expr;
	}

	/**
	 * Convert an expression to disjunctive normal form (DNF).
	 * The passed in expression is modified, and the result is returned. 
	 */
	public static Expression convertToDNF(Expression expr)
	{
		return convertDNFListsToExpression(doConversionToDNF(convertToPositiveNormalForm(expr)));
	}

	/**
	 * Convert an expression to disjunctive normal form (DNF).
	 * The passed in expression is modified, and the result is returned as a list of lists of Expression;
	 */
	public static List<List<Expression>> convertToDNFLists(Expression expr)
	{
		return doConversionToDNF(convertToPositiveNormalForm(expr));
	}

	private static List<List<Expression>> doConversionToDNF(Expression expr)
	{
		// And
		if (Expression.isAnd(expr)) {
			Expression a = ((ExpressionBinaryOp) expr).getOperand1();
			Expression b = ((ExpressionBinaryOp) expr).getOperand2();
			List<List<Expression>> aDnf = doConversionToDNF(a);
			List<List<Expression>> bDnf = doConversionToDNF(b);
			// a1|a2|... & b1|b2|...  ==  a1&b1 | a1&b2 | ...
			List<List<Expression>> dnf = new ArrayList<List<Expression>>();
			for (List<Expression> ai : aDnf) {
				for (List<Expression> bj : bDnf) {
					List<Expression> aibj = new ArrayList<Expression>();
					aibj.addAll(ai);
					aibj.addAll(bj);
					dnf.add(aibj);
				}
			}
			return dnf;
		}
		// Or
		if (Expression.isOr(expr)) {
			Expression a = ((ExpressionBinaryOp) expr).getOperand1();
			Expression b = ((ExpressionBinaryOp) expr).getOperand2();
			List<List<Expression>> aDnf = doConversionToDNF(a);
			List<List<Expression>> bDnf = doConversionToDNF(b);
			// (a1|a2|...) | (b1|b2|...)  ==  a1|a2|...|b1|b2|...
			aDnf.addAll(bDnf);
			return aDnf;
		}
		// Convert proposition to trivial DNF
		List<List<Expression>> dnf = new ArrayList<List<Expression>>(1);
		List<Expression> disjunct = new ArrayList<Expression>(1);
		disjunct.add(expr);
		dnf.add(disjunct);
		return dnf;
	}

	/**
	 * Convert an expression to conjunctive normal form (CNF).
	 * The passed in expression is modified, and the result is returned. 
	 */
	public static Expression convertToCNF(Expression expr)
	{
		return convertCNFListsToExpression(doConversionToCNF(convertToPositiveNormalForm(expr)));
	}

	/**
	 * Convert an expression to conjunctive normal form (CNF).
	 * The passed in expression is modified, and the result is returned as a list of lists of Expression;
	 */
	public static List<List<Expression>> convertToCNFLists(Expression expr)
	{
		return doConversionToCNF(convertToPositiveNormalForm(expr));
	}

	private static List<List<Expression>> doConversionToCNF(Expression expr)
	{
		// Remove parentheses
		if (Expression.isParenth(expr)) {
			return doConversionToCNF(((ExpressionUnaryOp) expr).getOperand());
		}
		// Or
		if (Expression.isOr(expr)) {
			Expression a = ((ExpressionBinaryOp) expr).getOperand1();
			Expression b = ((ExpressionBinaryOp) expr).getOperand2();
			List<List<Expression>> aCnf = doConversionToCNF(a);
			List<List<Expression>> bCnf = doConversionToCNF(b);
			// a1&a2&... | b1&b2&...  ==  a1|b1 & a1|b2 & ...
			List<List<Expression>> cnf = new ArrayList<List<Expression>>();
			for (List<Expression> ai : aCnf) {
				for (List<Expression> bj : bCnf) {
					List<Expression> aibj = new ArrayList<Expression>();
					aibj.addAll(ai);
					aibj.addAll(bj);
					cnf.add(aibj);
				}
			}
			return cnf;
		}
		// And
		if (Expression.isAnd(expr)) {
			Expression a = ((ExpressionBinaryOp) expr).getOperand1();
			Expression b = ((ExpressionBinaryOp) expr).getOperand2();
			List<List<Expression>> aCnf = doConversionToCNF(a);
			List<List<Expression>> bCnf = doConversionToCNF(b);
			// (a1|a2|...) | (b1|b2|...)  ==  a1|a2|...|b1|b2|...
			aCnf.addAll(bCnf);
			return aCnf;
		}
		// Convert proposition to trivial CNF
		List<List<Expression>> cnf = new ArrayList<List<Expression>>(1);
		List<Expression> conjunct = new ArrayList<Expression>(1);
		conjunct.add(expr);
		cnf.add(conjunct);
		return cnf;
	}

	// Methods to convert And/Or classes to Expressions
	
	public static Expression convertDNFListsToExpression(List<List<Expression>> dnf)
	{
		Expression ret = convertConjunctionListToExpression(dnf.get(0));
		for (int i = 1; i < dnf.size(); i++) {
			ret = Expression.Or(ret, convertConjunctionListToExpression(dnf.get(i)));
		}
		return ret;
	}

	public static Expression convertCNFListsToExpression(List<List<Expression>> cnf)
	{
		Expression ret = convertDisjunctionListToExpression(cnf.get(0));
		for (int i = 1; i < cnf.size(); i++) {
			ret = Expression.And(ret, convertDisjunctionListToExpression(cnf.get(i)));
		}
		return ret;
	}

	public static Expression convertDisjunctionListToExpression(List<Expression> disjunction)
	{
		Expression ret = disjunction.get(0);
		for (int i = 1; i < disjunction.size(); i++) {
			ret = Expression.Or(ret, disjunction.get(i));
		}
		return ret;
	}

	public static Expression convertConjunctionListToExpression(List<Expression> conjunction)
	{
		Expression ret = conjunction.get(0);
		for (int i = 1; i < conjunction.size(); i++) {
			ret = Expression.And(ret, conjunction.get(i));
		}
		return ret;
	}

	// Test code
	
	public static void main(String args[])
	{
		PrismParser parser = new PrismParser();
		String ss[] = new String[] { "a&!(b=>c)", "(a|b)&(c|d|e)" };
		for (String s : ss) {
			try {
				Expression expr = parser.parseSingleExpression(new ByteArrayInputStream(s.getBytes()));
				System.out.println(expr + " in CNF is " + convertToCNF(expr.deepCopy()));
				System.out.println(expr + " in DNF is " + convertToDNF(expr.deepCopy()));
			} catch (PrismException e) {
				System.out.println("Error: " + e.getMessage());
			}
		}
	}
}
