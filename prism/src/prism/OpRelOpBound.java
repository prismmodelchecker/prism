//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import explicit.MinMax;
import parser.ast.RelOp;
import parser.type.Type;

/**
 * Class to represent info (operator, relational operator, bound, etc.) found in a P/R/S operator.
 */
public class OpRelOpBound
{
	protected String op;
	protected RelOp relOp;
	protected boolean numeric;
	protected double bound;

	public OpRelOpBound(String op, RelOp relOp, Double boundObject)
	{
		this.op = op;
		this.relOp = relOp;
		numeric = (boundObject == null);
		if (boundObject != null)
			bound = boundObject.doubleValue();
	}

	public boolean isProbabilistic()
	{
		return "P".equals(op);
	}

	public boolean isReward()
	{
		return "R".equals(op);
	}

	public RelOp getRelOp()
	{
		return relOp;
	}

	public boolean isNumeric()
	{
		return numeric;
	}

	public double getBound()
	{
		return bound;
	}

	public boolean isQualitative()
	{
		return !isNumeric() && op.equals("P") && (bound == 0 || bound == 1);
	}

	public boolean isTriviallyTrue()
	{
		if (!isNumeric() && op.equals("P")) {
			// >=0
			if (bound == 0 && relOp == RelOp.GEQ)
				return true;
			// <=1
			if (bound == 1 && relOp == RelOp.LEQ)
				return true;
		}
		return false;
	}

	public boolean isTriviallyFalse()
	{
		if (!isNumeric() && op.equals("P")) {
			// <0
			if (bound == 0 && relOp == RelOp.LT)
				return true;
			// >1
			if (bound == 1 && relOp == RelOp.GT)
				return true;
		}
		return false;
	}

	public MinMax getMinMax(ModelType modelType) throws PrismLangException
	{
		return getMinMax(modelType, true);
	}

	public MinMax getMinMax(ModelType modelType, boolean forAll) throws PrismLangException
	{
		MinMax minMax = MinMax.blank();
		if (modelType.nondeterministic()) {
			if (!(modelType == ModelType.MDP || modelType == ModelType.POMDP || modelType == ModelType.CTMDP)) {
				throw new PrismLangException("Don't know how to model check " + getTypeOfOperator() + " properties for " + modelType + "s");
			}
			if (isNumeric()) {
				if (relOp == RelOp.EQ) {
					throw new PrismLangException("Can't use \"" + op + "=?\" for nondeterministic models; use e.g. \"" + op + "min=?\" or \"" + op + "max=?\"");
				}
				minMax = relOp.isMin() ? MinMax.min() : MinMax.max();
			} else {
				if (forAll) {
					minMax = (relOp.isLowerBound() ) ? MinMax.min() : MinMax.max();
				} else {
					minMax = (relOp.isLowerBound() ) ? MinMax.max() : MinMax.min();
				}
			}
		}
		return minMax;
	}

	public String getTypeOfOperator()
	{
		String s = "";
		s += op + relOp;
		s += isNumeric() ? "?" : "p"; // TODO: always "p"?
		return s;
	}

	public String relOpBoundString()
	{
		return relOp.toString() + bound;
	}

	@Override
	public String toString()
	{
		return op + relOp.toString() + (isNumeric() ? "?" : bound);
	}

	/**
	 * Apply this relational operator and bound instance to a value.
	 */
	public boolean apply(double value) throws PrismException
	{
		switch (relOp) {
		case GEQ:
			return (double) value >= bound;
		case GT:
			return (double) value > bound;
		case LEQ:
			return (double) value <= bound;
		case LT:
			return (double) value < bound;
		default:
			throw new PrismException("Cannot apply relational operator " + relOp);
		}
	}
	
	/**
	 * Apply this relational operator and bound instance to a value.
	 * If the value is stored imprecisely (i.e., floating point),
	 * the specified accuracy (if non-null) is taken into account, and an
	 * exception is thrown if the value is not accurate enough to check the bound.
	 */
	public boolean apply(double value, Accuracy accuracy) throws PrismException
	{
		if (accuracy != null) {
			boolean valueLow = apply(accuracy.getResultLowerBound(value));
			boolean valueHigh = apply(accuracy.getResultUpperBound(value));
			if (valueLow != valueHigh) {
				throw new PrismException("Accuracy of value " + value  + " is not enough to compare to bound " + bound);
			}
		}
		return apply(value);
	}
}
