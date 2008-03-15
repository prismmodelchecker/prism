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

package prism;

import java.io.*;
import java.util.Vector;

import jdd.*;
import parser.*;
import parser.ast.*;

// class to translate an expression into an MTBDD

public class Expression2MTBDD
{
	// logs
	private PrismLog mainLog;		// main log
	private PrismLog techLog;		// tech log

	// info needed about model
	private VarList varList;
	private JDDVars[] varDDVars;
	private Values constantValues;
	
	// filter to use at each recursive translate
	// (e.g. 'reach' when model checking)
	private JDDNode filter = null;
	
	// constructor
	
	public Expression2MTBDD(PrismLog log1, PrismLog log2, VarList vl, JDDVars[] vddv, Values cv)
	{
		mainLog = log1;
		techLog = log2;
		varList = vl;
		varDDVars = vddv;
		constantValues = cv;
	}

	// set filter
	
	public void setFilter(JDDNode f)
	{
		filter = f;
	}

	// Translate an  expression

	public JDDNode translateExpression(Expression expr) throws PrismException
	{
		JDDNode res;
		
		// if-then-else
		if (expr instanceof ExpressionITE) {
			res = translateExpressionITE((ExpressionITE)expr);
		}
		// binary ops
		else if (expr instanceof ExpressionBinaryOp) {
			res = translateExpressionBinaryOp((ExpressionBinaryOp)expr);
		}
		// unary ops
		else if (expr instanceof ExpressionUnaryOp) {
			res = translateExpressionUnaryOp((ExpressionUnaryOp)expr);
		}
		// function
		else if (expr instanceof ExpressionFunc) {
			res = translateExpressionFunc((ExpressionFunc)expr);
		}
		// ident
		else if (expr instanceof ExpressionIdent) {
			// Should never happen
			throw new PrismException("Unknown identifier \"" + ((ExpressionIdent)expr).getName() + "\"");
		}
		// literal
		else if (expr instanceof ExpressionLiteral) {
			res = translateExpressionLiteral((ExpressionLiteral)expr);
		}
		// constant
		else if (expr instanceof ExpressionConstant) {
			res = translateExpressionConstant((ExpressionConstant)expr);
		}
		// formula
		else if (expr instanceof ExpressionFormula) {
			// Should never happen
			throw new PrismException("Unexpanded formula \"" + ((ExpressionFormula)expr).getName() + "\"");
		}
		// var
		else if (expr instanceof ExpressionVar) {
			res = translateExpressionVar((ExpressionVar)expr);
		}
		else {
			throw new PrismException("Couldn't translate " + expr.getClass());
		}
		
		// apply filter if present
		if (filter != null) {
			JDD.Ref(filter);
			res = JDD.Apply(JDD.TIMES, res, filter);
		}
		
		return res;
	}
	
	// Translate an 'if-then-else'
	
	private JDDNode translateExpressionITE(ExpressionITE expr) throws PrismException
	{
		JDDNode dd1, dd2, dd3;
		
		dd1 = translateExpression(expr.getOperand1());
		dd2 = translateExpression(expr.getOperand2());
		dd3 = translateExpression(expr.getOperand3());
		
		return JDD.ITE(dd1, dd2, dd3);
	}
	
	// Translate a binary operator
	
	private JDDNode translateExpressionBinaryOp(ExpressionBinaryOp expr) throws PrismException
	{
		JDDNode dd, tmp1, tmp2;
		int op = expr.getOperator();
		
		// Optimisations are possible for relational operators
		// (note dubious use of knowledge that op IDs are consecutive)
		if (op >= ExpressionBinaryOp.EQ && op <= ExpressionBinaryOp.LE) {
			return translateExpressionRelOp(op, expr.getOperand1(), expr.getOperand2());
		}
		
		// Translate operands
		tmp1 = translateExpression(expr.getOperand1());
		try {
			tmp2 = translateExpression(expr.getOperand2());
		}
		catch (PrismException e) {
			JDD.Deref(tmp1);
			throw e;
		}
		
		// Apply operation
		switch (op) {
		case ExpressionBinaryOp.IMPLIES: dd = JDD.Or(JDD.Not(tmp1), tmp2); break;
		case ExpressionBinaryOp.OR: dd = JDD.Or(tmp1, tmp2); break;
		case ExpressionBinaryOp.AND: dd = JDD.And(tmp1, tmp2); break;
		case ExpressionBinaryOp.PLUS: dd = JDD.Apply(JDD.PLUS, tmp1, tmp2); break;
		case ExpressionBinaryOp.MINUS: dd = JDD.Apply(JDD.MINUS, tmp1, tmp2); break;
		case ExpressionBinaryOp.TIMES: dd = JDD.Apply(JDD.TIMES, tmp1, tmp2); break;
		case ExpressionBinaryOp.DIVIDE: dd = JDD.Apply(JDD.DIVIDE, tmp1, tmp2); break;
		default: throw new PrismException("Unknown binary operator");
		}

		return dd;
	}
	
	// Translate a relational operator (=, !=, >, >=, < <=)
	
	private JDDNode translateExpressionRelOp(int op, Expression expr1, Expression expr2) throws PrismException
	{
		JDDNode dd, tmp1, tmp2;
		String s;

		// check for some easy (and common) special cases before resorting to the general case
		
		// var relop int
		if (expr1 instanceof ExpressionVar && expr2.isConstant() && expr2.getType()==Expression.INT) {
			ExpressionVar e1;
			Expression e2;
			int i, j, l, h, v;
			e1 = (ExpressionVar)expr1;
			e2 = expr2;
			// get var's index
			s = e1.getName();
			v = varList.getIndex(s);
			if (v == -1) {
				throw new PrismException("Unknown variable \"" + s + "\"");
			}
			// get some info on the variable
			l = varList.getLow(v);
			h = varList.getHigh(v);
			// create dd
			dd = JDD.Constant(0);
			i = e2.evaluateInt(constantValues, null);
			switch (op) {
			case ExpressionBinaryOp.EQ:
				if (i>=l && i <= h) dd = JDD.SetVectorElement(dd, varDDVars[v], i-l, 1);
				break;
			case ExpressionBinaryOp.NE:
				if (i>=l && i <= h) dd = JDD.SetVectorElement(dd, varDDVars[v], i-l, 1);
				dd = JDD.Not(dd);
				break;
			case ExpressionBinaryOp.GT:
				for (j = i+1; j <= h; j++) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
				break;
			case ExpressionBinaryOp.GE:
				for (j = i; j <= h; j++) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
				break;
			case ExpressionBinaryOp.LT:
				for (j = i-1; j >= l; j--) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
				break;
			case ExpressionBinaryOp.LE:
				for (j = i; j >= l; j--) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
				break;
			default: throw new PrismException("Unknown relational operator");
			}
			return dd;
		}
		// int relop var
		else if (expr1.isConstant() && expr1.getType()==Expression.INT && expr2 instanceof ExpressionVar) {
			Expression e1;
			ExpressionVar e2;
			int i, j, l, h, v;
			e1 = expr1;
			e2 = (ExpressionVar)expr2;
			// get var's index
			s = e2.getName();
			v = varList.getIndex(s);
			if (v == -1) {
				throw new PrismException("Unknown variable \"" + s + "\"");
			}
			// get some info on the variable
			l = varList.getLow(v);
			h = varList.getHigh(v);
			// create dd
			dd = JDD.Constant(0);
			i = e1.evaluateInt(constantValues, null);
			switch (op) {
			case ExpressionBinaryOp.EQ:
				if (i>=l && i <= h) dd = JDD.SetVectorElement(dd, varDDVars[v], i-l, 1);
				break;
			case ExpressionBinaryOp.NE:
				if (i>=l && i <= h) dd = JDD.SetVectorElement(dd, varDDVars[v], i-l, 1);
				dd = JDD.Not(dd);
				break;
			case ExpressionBinaryOp.GT:
				for (j = i-1; j >= l; j--) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
				break;
			case ExpressionBinaryOp.GE:
				for (j = i; j >= l; j--) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
				break;
			case ExpressionBinaryOp.LT:
				for (j = i+1; j <= h; j++) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
				break;
			case ExpressionBinaryOp.LE:
				for (j = i; j <= h; j++) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
				break;
			default: throw new PrismException("Unknown relational operator");
			}
			return dd;
		}

		// general case
		tmp1 = translateExpression(expr1);
		tmp2 = translateExpression(expr2);
		switch (op) {
		case ExpressionBinaryOp.EQ:
			dd = JDD.Apply(JDD.EQUALS, tmp1, tmp2);
			break;
		case ExpressionBinaryOp.NE:
			dd = JDD.Apply(JDD.NOTEQUALS, tmp1, tmp2);
			break;
		case ExpressionBinaryOp.GT:
			dd = JDD.Apply(JDD.GREATERTHAN, tmp1, tmp2);
			break;
		case ExpressionBinaryOp.GE:
			dd = JDD.Apply(JDD.GREATERTHANEQUALS, tmp1, tmp2);
			break;
		case ExpressionBinaryOp.LT:
			dd = JDD.Apply(JDD.LESSTHAN, tmp1, tmp2);
			break;
		case ExpressionBinaryOp.LE:
			dd = JDD.Apply(JDD.LESSTHANEQUALS, tmp1, tmp2);
			break;
		default: throw new PrismException("Unknown relational operator");
		}
		
		return dd;
	}
	
	// Translate a unary operator
	
	private JDDNode translateExpressionUnaryOp(ExpressionUnaryOp expr) throws PrismException
	{
		JDDNode dd, tmp;
		int op = expr.getOperator();
		
		// Translate operand
		tmp = translateExpression(expr.getOperand());
		
		// Apply operation
		switch (op) {
		case ExpressionUnaryOp.NOT: dd = JDD.Not(tmp); break;
		case ExpressionUnaryOp.MINUS: dd = JDD.Apply(JDD.MINUS, JDD.Constant(0), tmp); break;
		case ExpressionUnaryOp.PARENTH: dd = tmp; break;
		default: throw new PrismException("Unknown unary operator");
		}

		return dd;
	}
	
	// Translate a 'function'
	
	private JDDNode translateExpressionFunc(ExpressionFunc expr) throws PrismException
	{
		switch (expr.getNameCode()) {
			case ExpressionFunc.MIN: return translateExpressionFuncMin(expr);
			case ExpressionFunc.MAX: return translateExpressionFuncMax(expr);
			case ExpressionFunc.FLOOR: return translateExpressionFuncFloor(expr);
			case ExpressionFunc.CEIL: return translateExpressionFuncCeil(expr);
			case ExpressionFunc.POW: return translateExpressionFuncPow(expr);
			case ExpressionFunc.MOD: return translateExpressionFuncMod(expr);
			case ExpressionFunc.LOG: return translateExpressionFuncLog(expr);
			default: throw new PrismException("Unrecognised function \"" + expr.getName() + "\"");
		}
	}
	
	private JDDNode translateExpressionFuncMin(ExpressionFunc expr) throws PrismException
	{
		int i, n;
		JDDNode dd, tmp;
		
		dd = translateExpression(expr.getOperand(0));
		n = expr.getNumOperands();
		for (i = 1; i < n; i++) {
			try {
				tmp = translateExpression(expr.getOperand(i));
				dd = JDD.Apply(JDD.MIN, dd, tmp);
			}
			catch (PrismException e) {
				JDD.Deref(dd);
				throw e;
			}
		}
		
		return dd;
	}
	
	private JDDNode translateExpressionFuncMax(ExpressionFunc expr) throws PrismException
	{
		int i, n;
		JDDNode dd, tmp;
		
		dd = translateExpression(expr.getOperand(0));
		n = expr.getNumOperands();
		for (i = 1; i < n; i++) {
			try {
				tmp = translateExpression(expr.getOperand(i));
				dd = JDD.Apply(JDD.MAX, dd, tmp);
			}
			catch (PrismException e) {
				JDD.Deref(dd);
				throw e;
			}
		}
		
		return dd;
	}
	
	private JDDNode translateExpressionFuncFloor(ExpressionFunc expr) throws PrismException
	{
		JDDNode dd;
		
		dd = translateExpression(expr.getOperand(0));
		dd = JDD.MonadicApply(JDD.FLOOR, dd);
		
		return dd;
	}
	
	private JDDNode translateExpressionFuncCeil(ExpressionFunc expr) throws PrismException
	{
		JDDNode dd;
		
		dd = translateExpression(expr.getOperand(0));
		dd = JDD.MonadicApply(JDD.CEIL, dd);
		
		return dd;
	}
	
	private JDDNode translateExpressionFuncPow(ExpressionFunc expr) throws PrismException
	{
		JDDNode dd1, dd2, dd;
		
		dd1 = translateExpression(expr.getOperand(0));
		dd2 = translateExpression(expr.getOperand(1));
		dd = JDD.Apply(JDD.POW, dd1, dd2);
		
		return dd;
	}
	
	private JDDNode translateExpressionFuncMod(ExpressionFunc expr) throws PrismException
	{
		JDDNode dd1, dd2, dd;
		
		dd1 = translateExpression(expr.getOperand(0));
		dd2 = translateExpression(expr.getOperand(1));
		dd = JDD.Apply(JDD.MOD, dd1, dd2);
		
		return dd;
	}

	private JDDNode translateExpressionFuncLog(ExpressionFunc expr) throws PrismException
	{
		JDDNode dd1, dd2, dd;
		
		dd1 = translateExpression(expr.getOperand(0));
		dd2 = translateExpression(expr.getOperand(1));
		dd = JDD.Apply(JDD.LOGXY, dd1, dd2);
		
		return dd;
	}

	// Translate a literal
	
	private JDDNode translateExpressionLiteral(ExpressionLiteral expr) throws PrismException
	{
		switch (expr.getType()) {
			case Expression.BOOLEAN: return JDD.Constant(expr.evaluateBoolean(null, null) ? 1.0 : 0.0);
			case Expression.INT: return JDD.Constant(expr.evaluateInt(null, null));
			case Expression.DOUBLE: return JDD.Constant(expr.evaluateDouble(null, null));
			default: throw new PrismException("Unknown literal type");
		}
	}
	
	// Translate a constant
	
	private JDDNode translateExpressionConstant(ExpressionConstant expr) throws PrismException
	{
		int i;
		Object o;
		
		i = constantValues.getIndexOf(expr.getName());
		if (i == -1) throw new PrismException("Couldn't evaluate constant \"" + expr.getName() + "\"");
		switch (constantValues.getType(i)) {
			case Expression.INT: return JDD.Constant(constantValues.getIntValue(i));
			case Expression.DOUBLE: return JDD.Constant(constantValues.getDoubleValue(i));
			case Expression.BOOLEAN: return JDD.Constant(constantValues.getBooleanValue(i) ? 1.0 : 0.0);
			default: throw new PrismException("Unknown type for constant \"" + expr.getName() + "\"");
		}
	}
	
	// Translate a variable reference
	
	private JDDNode translateExpressionVar(ExpressionVar expr) throws PrismException
	{
		String s;
		int v, l, h, i;
		JDDNode dd, tmp;
		
		s = expr.getName();
		// get the variable's index
		v = varList.getIndex(s);
		if (v == -1) {
			throw new PrismException("Unknown variable \"" + s + "\"");
		}
		// get some info on the variable
		l = varList.getLow(v);
		h = varList.getHigh(v);
		// create dd
		dd = JDD.Constant(0);
		for (i = l; i <= h; i++) {
			dd = JDD.SetVectorElement(dd, varDDVars[v], i-l, i);
		}
				
		return dd;
	}
}

//------------------------------------------------------------------------------
