//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker
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

	// translate an  expression

	public JDDNode translateExpression(Expression expr) throws PrismException
	{
		JDDNode res;
		
		// if-then-else
		if (expr instanceof ExpressionITE) {
			res = translateExpressionITE((ExpressionITE)expr);
		}
		// or
		else if (expr instanceof ExpressionOr) {
			res = translateExpressionOr((ExpressionOr)expr);
		}
		// and
		else if (expr instanceof ExpressionAnd) {
			res = translateExpressionAnd((ExpressionAnd)expr);
		}
		// not
		else if (expr instanceof ExpressionNot) {
			res = translateExpressionNot((ExpressionNot)expr);
		}
		// rel. op.
		else if (expr instanceof ExpressionRelOp) {
			res = translateExpressionRelOp((ExpressionRelOp)expr);
		}
		// range
		else if (expr instanceof ExpressionRange) {
			res = translateExpressionRange((ExpressionRange)expr);
		}
		// plus
		else if (expr instanceof ExpressionPlus) {
			res = translateExpressionPlus((ExpressionPlus)expr);
		}
		// minus
		else if (expr instanceof ExpressionMinus) {
			res = translateExpressionMinus((ExpressionMinus)expr);
		}
		// times
		else if (expr instanceof ExpressionTimes) {
			res = translateExpressionTimes((ExpressionTimes)expr);
		}
		// divide
		else if (expr instanceof ExpressionDivide) {
			res = translateExpressionDivide((ExpressionDivide)expr);
		}
		// function
		else if (expr instanceof ExpressionFunc) {
			res = translateExpressionFunc((ExpressionFunc)expr);
		}
		// ident
		else if (expr instanceof ExpressionIdent) {
			// ident should by now have been recognised as var/const/etc.
			// and converted to a different expression type
			throw new PrismException("Unknown identifier \"" + ((ExpressionIdent)expr).getName() + "\"");
		}
		// var
		else if (expr instanceof ExpressionVar) {
			res = translateExpressionVar((ExpressionVar)expr);
		}
		// constant
		else if (expr instanceof ExpressionConstant) {
			res = translateExpressionConstant((ExpressionConstant)expr);
		}
		// int
		else if (expr instanceof ExpressionInt) {
			res = translateExpressionInt((ExpressionInt)expr);
		}
		// double
		else if (expr instanceof ExpressionDouble) {
			res = translateExpressionDouble((ExpressionDouble)expr);
		}
		// true
		else if (expr instanceof ExpressionTrue) {
			res =  JDD.Constant(1);
		}
		// false
		else if (expr instanceof ExpressionFalse) {
			res =  JDD.Constant(0);
		}
		// brackets
		else if (expr instanceof ExpressionBrackets) {
			res = translateExpression(((ExpressionBrackets)expr).getOperand());
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
	
	// translate an 'if-then-else'
	
	private JDDNode translateExpressionITE(ExpressionITE expr) throws PrismException
	{
		JDDNode dd1, dd2, dd3;
		
		dd1 = translateExpression(expr.getOperand(0));
		dd2 = translateExpression(expr.getOperand(1));
		dd3 = translateExpression(expr.getOperand(2));
		
		return JDD.ITE(dd1, dd2, dd3);
	}
	
	// translate an 'or'
	
	private JDDNode translateExpressionOr(ExpressionOr expr) throws PrismException
	{
		int i, n;
		JDDNode dd, tmp;

		dd = JDD.Constant(0);
		n = expr.getNumOperands();
		for (i = 0; i < n; i++) {
			try {
				tmp = translateExpression(expr.getOperand(i));
				dd = JDD.Or(dd, tmp);
			}
			catch (PrismException e) {
				JDD.Deref(dd);
				throw e;
			}
		}
		
		return dd;
	}
	
	// translate an 'and'
	
	private JDDNode translateExpressionAnd(ExpressionAnd expr) throws PrismException
	{
		int i, n;
		JDDNode dd, tmp;

		dd = JDD.Constant(1);
		n = expr.getNumOperands();
		for (i = 0; i < n; i++) {
			try {
				tmp = translateExpression(expr.getOperand(i));
				dd = JDD.And(dd, tmp);
			}
			catch (PrismException e) {
				JDD.Deref(dd);
				throw e;
			}
		}
		
		return dd;
	}
	
	// translate a 'not'
	
	private JDDNode translateExpressionNot(ExpressionNot expr) throws PrismException
	{
		JDDNode dd;

		dd = translateExpression(expr.getOperand());
		dd = JDD.Not(dd);
		
		return dd;
	}
	
	// translate a relational operator
	
	private JDDNode translateExpressionRelOp(ExpressionRelOp expr) throws PrismException
	{
		JDDNode dd, tmp1, tmp2;
		String s;

		// check for some easy (and common) special cases before resorting to the general case
		
		// var relop int
		if (expr.getOperand1() instanceof ExpressionVar && expr.getOperand2().isConstant() && expr.getOperand2().getType()==Expression.INT) {
			ExpressionVar e1;
			Expression e2;
			int i, j, l, h, v;
			e1 = (ExpressionVar)expr.getOperand1();
			e2 = expr.getOperand2();
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
			s = expr.getRelOp();
			i = e2.evaluateInt(constantValues, null);
			if (s.equals("=")) {
				if (i>=l && i <= h) dd = JDD.SetVectorElement(dd, varDDVars[v], i-l, 1);
			}
			else if (s.equals("!=")) {
				if (i>=l && i <= h) dd = JDD.SetVectorElement(dd, varDDVars[v], i-l, 1);
				dd = JDD.Not(dd);
			}
			else if (s.equals(">")) {
				for (j = i+1; j <= h; j++) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
			}
			else if (s.equals(">=")) {
				for (j = i; j <= h; j++) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
			}
			else if (s.equals("<")) {
				for (j = i-1; j >= l; j--) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
			}
			else if (s.equals("<=")) {
				for (j = i; j >= l; j--) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
			}
			else {
				throw new PrismException("Unknown relational operator \"" + s + "\"");
			}
			return dd;
		}
		// int relop var
		else if (expr.getOperand1().isConstant() && expr.getOperand1().getType()==Expression.INT && expr.getOperand2() instanceof ExpressionVar) {
			Expression e1;
			ExpressionVar e2;
			int i, j, l, h, v;
			e1 = expr.getOperand1();
			e2 = (ExpressionVar)expr.getOperand2();
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
			s = expr.getRelOp();
			i = e1.evaluateInt(constantValues, null);
			if (s.equals("=")) {
				if (i>=l && i <= h) dd = JDD.SetVectorElement(dd, varDDVars[v], i-l, 1);
			}
			else if (s.equals("!=")) {
				if (i>=l && i <= h) dd = JDD.SetVectorElement(dd, varDDVars[v], i-l, 1);
				dd = JDD.Not(dd);
			}
			else if (s.equals(">")) {
				for (j = i-1; j >= l; j--) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
			}
			else if (s.equals(">=")) {
				for (j = i; j >= l; j--) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
			}
			else if (s.equals("<")) {
				for (j = i+1; j <= h; j++) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
			}
			else if (s.equals("<=")) {
				for (j = i; j <= h; j++) dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
			}
			else {
				throw new PrismException("Unknown relational operator \"" + s + "\"");
			}
			return dd;
		}

		// general case
		tmp1 = translateExpression(expr.getOperand1());
		tmp2 = translateExpression(expr.getOperand2());
		s = expr.getRelOp();
		if (s.equals("=")) {
			// nb: this case actual redundant because comes under ExpressionRange
			dd = JDD.Apply(JDD.EQUALS, tmp1, tmp2);
		}
		else if (s.equals("!=")) {
			// nb: this case actual redundant because comes under ExpressionRange
			dd = JDD.Apply(JDD.NOTEQUALS, tmp1, tmp2);
		}
		else if (s.equals(">")) {
			dd = JDD.Apply(JDD.GREATERTHAN, tmp1, tmp2);
		}
		else if (s.equals(">=")) {
			dd = JDD.Apply(JDD.GREATERTHANEQUALS, tmp1, tmp2);
		}
		else if (s.equals("<")) {
			dd = JDD.Apply(JDD.LESSTHAN, tmp1, tmp2);
		}
		else if (s.equals("<=")) {
			dd = JDD.Apply(JDD.LESSTHANEQUALS, tmp1, tmp2);
		}
		else {
			throw new PrismException("Unknown relational operator \"" + s + "\"");
		}
		
		return dd;
	}
	
	// translate a 'range'
	
	private JDDNode translateExpressionRange(ExpressionRange expr) throws PrismException
	{
		int i, n;
		JDDNode dd, tmp, tmp1, tmp2;
		String s;

		// check for an easy (and common) special case before resorting to the general case
		
 		// operand = var and everything else constant integers
		if (expr.getOperand() instanceof ExpressionVar && expr.rangeIsConstant() && expr.rangeIsAllInts()) {
			ExpressionVar e1;
			int j, v, l, h, l2, h2;
			e1 = (ExpressionVar)expr.getOperand();
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
			n = expr.getNumRangeOperands();
			for (i = 0; i < n; i++) {
				l2 = expr.getRangeOperandLow(i).evaluateInt(constantValues, null);
				if (l2 < l) l2 = l;
				h2 = expr.getRangeOperandHigh(i).evaluateInt(constantValues, null);
				if (h2 > h) h2 = l;
				for (j = l2; j <= h2; j++) {
					dd = JDD.SetVectorElement(dd, varDDVars[v], j-l, 1);
				}
			}
			if (expr.getRelOp().equals("!=")) {
				dd = JDD.Not(dd);
			}

			return dd;
		}
		
		// general case
		tmp = translateExpression(expr.getOperand());
		dd = JDD.Constant(0);
		n = expr.getNumRangeOperands();
		for (i = 0; i < n; i++) {
			if (expr.getRangeOperandSize(i) == 1) {
				tmp1 = translateExpression(expr.getRangeOperandLow(i));
				JDD.Ref(tmp);
				tmp1 = JDD.Apply(JDD.EQUALS, tmp, tmp1);
				dd = JDD.Or(dd, tmp1);
			}
			else {
				tmp1 = translateExpression(expr.getRangeOperandLow(i));
				JDD.Ref(tmp);
				tmp1 = JDD.Apply(JDD.GREATERTHANEQUALS, tmp, tmp1);
				tmp2 = translateExpression(expr.getRangeOperandHigh(i));
				JDD.Ref(tmp);
				tmp2 = JDD.Apply(JDD.LESSTHANEQUALS, tmp, tmp2);
				tmp1 = JDD.And(tmp1, tmp2);
				dd = JDD.Or(dd, tmp1);
			}
		}
		JDD.Deref(tmp);
		if (expr.getRelOp().equals("!=")) {
			dd = JDD.Not(dd);
		}
		
		return dd;
	}
	
	// translate a 'plus'
	
	private JDDNode translateExpressionPlus(ExpressionPlus expr) throws PrismException
	{
		JDDNode dd, tmp1, tmp2;

		tmp1 = translateExpression(expr.getOperand1());
		try {
			tmp2 = translateExpression(expr.getOperand2());
			dd = JDD.Apply(JDD.PLUS, tmp1, tmp2);
		}
		catch (PrismException e) {
			JDD.Deref(tmp1);
			throw e;
		}
		
		return dd;
	}
	
	// translate a 'minus'
	
	private JDDNode translateExpressionMinus(ExpressionMinus expr) throws PrismException
	{
		JDDNode dd, tmp1, tmp2;

		tmp1 = translateExpression(expr.getOperand1());
		try {
			tmp2 = translateExpression(expr.getOperand2());
			dd = JDD.Apply(JDD.MINUS, tmp1, tmp2);
		}
		catch (PrismException e) {
			JDD.Deref(tmp1);
			throw e;
		}
		
		return dd;
	}
	
	// translate a 'times'
	
	private JDDNode translateExpressionTimes(ExpressionTimes expr) throws PrismException
	{
		JDDNode dd, tmp1, tmp2;

		tmp1 = translateExpression(expr.getOperand1());
		try {
			tmp2 = translateExpression(expr.getOperand2());
			dd = JDD.Apply(JDD.TIMES, tmp1, tmp2);
		}
		catch (PrismException e) {
			JDD.Deref(tmp1);
			throw e;
		}
		
		return dd;
	}
	
	// translate a 'divide'
	
	private JDDNode translateExpressionDivide(ExpressionDivide expr) throws PrismException
	{
		JDDNode dd, tmp1, tmp2;

		tmp1 = translateExpression(expr.getOperand1());
		try {
			tmp2 = translateExpression(expr.getOperand2());
			dd = JDD.Apply(JDD.DIVIDE, tmp1, tmp2);
		}
		catch (PrismException e) {
			JDD.Deref(tmp1);
			throw e;
		}
		
		return dd;
	}

	// translate a 'function'
	
	private JDDNode translateExpressionFunc(ExpressionFunc expr) throws PrismException
	{
		switch (expr.getNameCode()) {
			case ExpressionFunc.MIN: return translateExpressionFuncMin(expr);
			case ExpressionFunc.MAX: return translateExpressionFuncMax(expr);
			case ExpressionFunc.FLOOR: return translateExpressionFuncFloor(expr);
			case ExpressionFunc.CEIL: return translateExpressionFuncCeil(expr);
			case ExpressionFunc.POW: return translateExpressionFuncPow(expr);
			case ExpressionFunc.MOD: return translateExpressionFuncMod(expr);
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

	// translate a 'var'
	
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
	
	// translate a 'constant' expression
	
	private JDDNode translateExpressionConstant(ExpressionConstant expr) throws PrismException
	{
		int i;
		Object o;
		
		i = constantValues.getIndexOf(expr.getName());
		if (i == -1) throw new PrismException("Couldn't evaluate constant \"" + expr.getName() + "\"");
		switch (constantValues.getType(i)) {
			case Expression.INT: return JDD.Constant(constantValues.getIntValue(i));
			case Expression.DOUBLE: return JDD.Constant(constantValues.getDoubleValue(i));
			case Expression.BOOLEAN: return JDD.Constant(constantValues.getBooleanValue(i) ? 1 : 0);
		}
		
		throw new PrismException("Unknown type for \"" + expr.getName() + "\"");
	}
	
	// translate an 'int'
	
	private JDDNode translateExpressionInt(ExpressionInt expr) throws PrismException
	{
		return JDD.Constant(expr.getValue());
	}
	
	// translate a 'double'
	
	private JDDNode translateExpressionDouble(ExpressionDouble expr) throws PrismException
	{
		return JDD.Constant(expr.getValue());
	}
}

//------------------------------------------------------------------------------
