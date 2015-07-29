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
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionQuant;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionUnaryOp;
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
	 * Extract maximal state formula from an LTL path formula, model check them (with passed in model checker) and
	 * replace them with ExpressionLabel objects L0, L1, etc. Expression passed in is modified directly, but the result
	 * is also returned. As an optimisation, expressions that results in true/false for all states are converted to an
	 * actual true/false, and duplicate results (or their negations) reuse the same label. BDDs giving the states which
	 * satisfy each label are put into the vector labelDDs, which should be empty when this function is called.
	 */
	public static Expression extractAtomsFromBooleanExpression(Expression expr, List<ExpressionQuant> labelDDs) throws PrismException
	{
		// A state formula
		if (expr instanceof ExpressionProb || expr instanceof ExpressionReward) {
			labelDDs.add((ExpressionQuant) expr);
			return new ExpressionLabel("L" + (labelDDs.size() - 1));
		}
		// A path formula (recurse, modify, return)
		else if (Expression.isOr(expr)) {
			ExpressionBinaryOp exprOr = (ExpressionBinaryOp) expr;
			exprOr.setOperand1(extractAtomsFromBooleanExpression(exprOr.getOperand1(), labelDDs));
			exprOr.setOperand2(extractAtomsFromBooleanExpression(exprOr.getOperand2(), labelDDs));
		} else if (Expression.isAnd(expr)) {
			ExpressionBinaryOp exprAnd = (ExpressionBinaryOp) expr;
			exprAnd.setOperand1(extractAtomsFromBooleanExpression(exprAnd.getOperand1(), labelDDs));
			exprAnd.setOperand2(extractAtomsFromBooleanExpression(exprAnd.getOperand2(), labelDDs));
		} else if (Expression.isImplies(expr)) {
			ExpressionBinaryOp exprImplies = (ExpressionBinaryOp) expr;
			exprImplies.setOperand1(extractAtomsFromBooleanExpression(exprImplies.getOperand1(), labelDDs));
			exprImplies.setOperand2(extractAtomsFromBooleanExpression(exprImplies.getOperand2(), labelDDs));
		} else if (Expression.isNot(expr)) {
			ExpressionUnaryOp exprNot = (ExpressionUnaryOp) expr;
			exprNot.setOperand(extractAtomsFromBooleanExpression(exprNot.getOperand(), labelDDs));
		} else {
			throw new PrismException("arg " + expr);
		}
		return expr;
	}

	/**
	 * Convert a Boolean expression to positive normal form,
	 * i.e., remove any instances of =>, <=> or () and then push
	 * all negation inwards so that it only occurs on "propositions".
	 * A "proposition" is any Expression object that
	 * is not an operator used to define a Boolean expression (!, &, |, =>, <=>, ()).
	 * The passed in expression is modified, and the result is returned. 
	 */
	public static Expression convertToPositiveNormalForm(Expression expr) throws PrismLangException
	{
		// First expand implies/iff/parentheses
		Expression exprBool = (Expression) expr.accept(new ASTTraverseModify() {
			public Object visit(ExpressionUnaryOp e) throws PrismLangException
			{
				// Remove parentheses: (a)
				if (Expression.isParenth(e)) {
					Expression a = (Expression)(e.getOperand().accept(this));
					// (a)  ==  a 
					return a;
				}
				return super.visit(e);
			}
			public Object visit(ExpressionBinaryOp e) throws PrismLangException
			{
				// Remove implication: a => b
				if (Expression.isImplies(e)) {
					Expression a = (Expression)(e.getOperand1().accept(this));
					Expression b = (Expression)(e.getOperand2().accept(this));
					// a => b  ==  !a | b 
					return Expression.Or(Expression.Not(a), b);
				}
				// Remove iff: a <=> b
				if (Expression.isImplies(e)) {
					Expression a = (Expression)(e.getOperand1().accept(this));
					Expression b = (Expression)(e.getOperand2().accept(this));
					// a <=> b  ==  (a | !b) & (!a | b) 
					return Expression.And(Expression.Or(a, Expression.Not(b)), Expression.Or(Expression.Not(a.deepCopy()), b.deepCopy()));
				}
				return super.visit(e);
			}
		});
		
		// Then do conversion to +ve normal form
		return doConversionToPositiveNormalForm(exprBool);
	}
	
	/**
	 * Do the main part of the conversion of a Boolean expression to positive normal form,
	 * i.e., push all negation inwards to the propositions. It is assumed that the Boolean expression
	 * comprises only the operators !, & and |. Anything else is treated as a proposition.
	 * The passed in expression is modified, and the result is returned. 
	 */
	private static Expression doConversionToPositiveNormalForm(Expression expr)
	{
		// Remove negation
		if (Expression.isNot(expr)) {
			Expression neg = ((ExpressionUnaryOp) expr).getOperand();
			if (Expression.isNot(neg)) {
				Expression a = ((ExpressionUnaryOp) neg).getOperand();
				// !(!a)  ==  a 
				return doConversionToPositiveNormalForm(a);
			} else if (Expression.isOr(neg)) {
				Expression a = ((ExpressionBinaryOp) neg).getOperand1();
				Expression b = ((ExpressionBinaryOp) neg).getOperand2();
				Expression aNeg = Expression.Not(a);
				Expression bNeg = Expression.Not(b);
				// !(a | b)  ==  !a & !b 
				return doConversionToPositiveNormalForm(Expression.And(aNeg, bNeg));
			} else if (Expression.isAnd(neg)) {
				Expression a = ((ExpressionBinaryOp) neg).getOperand1();
				Expression b = ((ExpressionBinaryOp) neg).getOperand2();
				Expression aNeg = Expression.Not(a);
				Expression bNeg = Expression.Not(b);
				// !(a & b)  ==  !a | !b 
				return doConversionToPositiveNormalForm(Expression.Or(aNeg, bNeg));
			} else {
				// Proposition (negated)
				return expr;
			}
		}
		// Preserve and
		if (Expression.isAnd(expr)) {
			Expression a = doConversionToPositiveNormalForm(((ExpressionBinaryOp) expr).getOperand1());
			Expression b = doConversionToPositiveNormalForm(((ExpressionBinaryOp) expr).getOperand2());
			return Expression.And(a, b);
		}
		// Preserve or
		if (Expression.isOr(expr)) {
			Expression a = doConversionToPositiveNormalForm(((ExpressionBinaryOp) expr).getOperand1());
			Expression b = doConversionToPositiveNormalForm(((ExpressionBinaryOp) expr).getOperand2());
			return Expression.Or(a, b);
		}
		// Proposition
		return expr;
	}

	/**
	 * Convert an expression to disjunctive normal form (DNF).
	 * The passed in expression is modified, and the result is returned. 
	 */
	public static Expression convertToDNF(Expression expr) throws PrismException
	{
		return convertDNFListsToExpression(doConversionToDNF(convertToPositiveNormalForm(expr)));
	}

	/**
	 * Convert an expression to disjunctive normal form (DNF).
	 * The passed in expression is modified, and the result is returned as a list of lists of Expression;
	 */
	public static List<List<Expression>> convertToDNFLists(Expression expr) throws PrismException
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
	public static Expression convertToCNF(Expression expr) throws PrismException
	{
		return convertCNFListsToExpression(doConversionToCNF(convertToPositiveNormalForm(expr)));
	}

	/**
	 * Convert an expression to conjunctive normal form (CNF).
	 * The passed in expression is modified, and the result is returned as a list of lists of Expression;
	 */
	public static List<List<Expression>> convertToCNFLists(Expression expr) throws PrismException
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
		// Or
		if (Expression.isOr(expr)) {
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
