//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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
import simulator.SimulatorEngine;
import simulator.SimulatorException;

/**
 * Rename (according to RenamedModule definition), return result.
 */
public class ToSimulator extends ASTTraverseModify
{
	private SimulatorEngine sim;
	private Hashtable<ASTElement, Long> ptrs;

	public ToSimulator(SimulatorEngine sim)
	{
		this.sim = sim;
		ptrs = new Hashtable<ASTElement, Long>();
	}

	public Object setPtr(ASTElement e, long ptr)
	{
		return ptrs.put(e, ptr);
	}

	public long getPtr(ASTElement e)
	{
		return ptrs.get(e);
	}

	public void visitPost(RewardStructItem e) throws PrismLangException
	{
		long ret = 1;
		long ptrS = getPtr(e.getStates());
		long ptrR = getPtr(e.getReward());
		int synchIndex;
		// State reward
		if (e.getSynch() == null) {
			ret = SimulatorEngine.createStateReward(ptrS, ptrR);
		}
		// Transition reward
		else {
			if (e.getSynch().equals("")) {
				ret = SimulatorEngine.createTransitionReward(-1, ptrS, ptrR);
			} else {
				try {
					synchIndex = sim.getIndexOfVar(e.getSynch());
				} catch (SimulatorException ex) {
					throw new PrismLangException("Action label \"" + e.getSynch() + "\" not found in simulator", e);
				}
				ret = SimulatorEngine.createTransitionReward(synchIndex, ptrS, ptrR);
			}
		}
		setPtr(e, ret);
	}

	public void visitPost(ExpressionITE e) throws PrismLangException
	{
		long ret = -1;
		long ptr1 = getPtr(e.getOperand1());
		long ptr2 = getPtr(e.getOperand2());
		long ptr3 = getPtr(e.getOperand3());
		if (e.getType() == Expression.DOUBLE)
			ret = SimulatorEngine.createRealIte(ptr1, ptr2, ptr3);
		else
			ret = SimulatorEngine.createIte(ptr1, ptr2, ptr3);
		setPtr(e, ret);
	}

	public void visitPost(ExpressionBinaryOp e) throws PrismLangException
	{
		long ret = -1;
		long ptr1 = getPtr(e.getOperand1());
		long ptr2 = getPtr(e.getOperand2());
		boolean dbl = e.getType() == Expression.DOUBLE;
		switch (e.getOperator()) {
		case ExpressionBinaryOp.IMPLIES:
			ret = SimulatorEngine.createOr(new long[] { SimulatorEngine.createNot(ptr1), ptr2 });
			break;
		case ExpressionBinaryOp.OR:
			ret = SimulatorEngine.createOr(new long[] { ptr1, ptr2 });
			break;
		case ExpressionBinaryOp.AND:
			ret = SimulatorEngine.createAnd(new long[] { ptr1, ptr2 });
			break;
		case ExpressionBinaryOp.EQ:
			ret = dbl ? SimulatorEngine.createRealEquals(ptr1, ptr2) : SimulatorEngine.createNormalEquals(ptr1, ptr2);
			break;
		case ExpressionBinaryOp.NE:
			ret = dbl ? SimulatorEngine.createRealNotEquals(ptr1, ptr2) : SimulatorEngine.createNormalNotEquals(ptr1,
					ptr2);
			break;
		case ExpressionBinaryOp.GT:
			ret = dbl ? SimulatorEngine.createRealGreaterThan(ptr1, ptr2) : SimulatorEngine.createNormalGreaterThan(
					ptr1, ptr2);
			break;
		case ExpressionBinaryOp.GE:
			ret = dbl ? SimulatorEngine.createRealGreaterThanEqual(ptr1, ptr2) : SimulatorEngine
					.createNormalGreaterThanEqual(ptr1, ptr2);
			break;
		case ExpressionBinaryOp.LT:
			ret = dbl ? SimulatorEngine.createRealLessThan(ptr1, ptr2) : SimulatorEngine.createNormalLessThan(ptr1,
					ptr2);
			break;
		case ExpressionBinaryOp.LE:
			ret = dbl ? SimulatorEngine.createRealLessThanEqual(ptr1, ptr2) : SimulatorEngine
					.createNormalLessThanEqual(ptr1, ptr2);
			break;
		case ExpressionBinaryOp.PLUS:
			ret = dbl ? SimulatorEngine.createRealPlus(ptr1, ptr2) : SimulatorEngine.createNormalPlus(ptr1, ptr2);
			break;
		case ExpressionBinaryOp.MINUS:
			ret = dbl ? SimulatorEngine.createRealMinus(ptr1, ptr2) : SimulatorEngine.createNormalMinus(ptr1, ptr2);
			break;
		case ExpressionBinaryOp.TIMES:
			ret = dbl ? SimulatorEngine.createRealTimes(ptr1, ptr2) : SimulatorEngine.createNormalTimes(ptr1, ptr2);
			break;
		case ExpressionBinaryOp.DIVIDE:
			ret = SimulatorEngine.createDivide(ptr1, ptr2);
			break;
		}
		setPtr(e, ret);
	}

	public void visitPost(ExpressionUnaryOp e) throws PrismLangException
	{
		long ret = -1;
		long ptr = getPtr(e.getOperand());
		boolean dbl = e.getType() == Expression.DOUBLE;
		switch (e.getOperator()) {
		case ExpressionUnaryOp.NOT:
			ret = SimulatorEngine.createNot(ptr);
			break;
		case ExpressionUnaryOp.MINUS:
			if (dbl) {
				ret = SimulatorEngine.createRealMinus(SimulatorEngine.createDouble(0.0), ptr);
			} else {
				ret = SimulatorEngine.createNormalMinus(SimulatorEngine.createInteger(0), ptr);
			}
			break;
		case ExpressionUnaryOp.PARENTH:
			ret = ptr;
			break;
		}
		setPtr(e, ret);
	}

	public void visitPost(ExpressionFunc e) throws PrismLangException
	{
		long ret = -1;
		int i, n = e.getNumOperands();
		long ptr[] = new long[n];
		for (i = 0; i < n; i++) {
			ptr[i] = getPtr(e.getOperand(i));
		}
		boolean dbl = e.getType() == Expression.DOUBLE;
		switch (e.getNameCode()) {
		case ExpressionFunc.MIN:
			ret = dbl ? SimulatorEngine.createRealMin(ptr) : SimulatorEngine.createNormalMin(ptr);
			break;
		case ExpressionFunc.MAX:
			ret = dbl ? SimulatorEngine.createRealMax(ptr) : SimulatorEngine.createNormalMax(ptr);
			break;
		case ExpressionFunc.FLOOR:
			ret = SimulatorEngine.createFloor(ptr[0]);
			break;
		case ExpressionFunc.CEIL:
			ret = SimulatorEngine.createCeil(ptr[0]);
			break;
		case ExpressionFunc.POW:
			ret = dbl ? SimulatorEngine.createRealPow(ptr[0], ptr[1]) : SimulatorEngine.createNormalPow(ptr[0], ptr[1]);
			break;
		case ExpressionFunc.MOD:
			ret = SimulatorEngine.createMod(ptr[0], ptr[1]);
			break;
		case ExpressionFunc.LOG:
			ret = SimulatorEngine.createLog(ptr[0], ptr[1]);
			break;
		default:
			throw new PrismLangException("Cannot convert unknown function", e);
		}
		setPtr(e, ret);
	}

	public void visitPost(ExpressionIdent e) throws PrismLangException
	{
		// Should never happpen
		throw new PrismLangException("Cannot convert unknown identifier", e);
	}

	public void visitPost(ExpressionLiteral e) throws PrismLangException
	{
		long ret = -1;
		switch (e.getType()) {
		case Expression.BOOLEAN:
			ret = SimulatorEngine.createBoolean(e.evaluateBoolean(null, null));
			break;
		case Expression.INT:
			ret = SimulatorEngine.createInteger(e.evaluateInt(null, null));
			break;
		case Expression.DOUBLE:
			ret = SimulatorEngine.createDouble(e.evaluateDouble(null, null));
			break;
		}
		setPtr(e, ret);
	}

	public void visitPost(ExpressionConstant e) throws PrismLangException
	{
		long ret = -1;
		switch (e.getType()) {
		case Expression.BOOLEAN:
			ret = SimulatorEngine.createBoolean(e.evaluateBoolean(sim.getConstants(), null));
			break;
		case Expression.INT:
			ret = SimulatorEngine.createInteger(e.evaluateInt(sim.getConstants(), null));
			break;
		case Expression.DOUBLE:
			ret = SimulatorEngine.createDouble(e.evaluateDouble(sim.getConstants(), null));
			break;
		}
		setPtr(e, ret);
	}

	public void visitPost(ExpressionFormula e) throws PrismLangException
	{
		throw new PrismLangException("Cannot convert formulas", e);
	}

	public void visitPost(ExpressionVar e) throws PrismLangException
	{
		// Precondition: variable indices been populated in simulator engine
		long ret = -1;
		int varIndex;
		try {
			varIndex = sim.getIndexOfVar(e.getName());
		} catch (SimulatorException ex) {
			throw new PrismLangException("Variable \"" + e.getName() + "\" not found in simulator", e);
		}
		switch (e.getType()) {
		case Expression.BOOLEAN:
			ret = SimulatorEngine.createBooleanVar(varIndex);
			break;
		case Expression.INT:
			ret = SimulatorEngine.createIntegerVar(varIndex);
			break;
		}
		setPtr(e, ret);
	}

	public void visitPost(ExpressionProb e) throws PrismLangException
	{

	}

	public void visitPost(ExpressionLabel e) throws PrismLangException
	{
		LabelList labelList;
		Expression expr;
		int i;
		labelList = sim.getLabelList();
		i = labelList.getLabelIndex(e.getName());
		if (i == -1) {
			throw new PrismLangException("Cannot convert unknown label", e);
		}
		expr = labelList.getLabel(i);
		expr.accept(this);
		setPtr(e, getPtr(expr));
	}
}
