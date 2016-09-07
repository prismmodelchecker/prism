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

package parser.ast;

import param.BigRational;
import parser.EvaluateContext;
import parser.Values;
import parser.visitor.ASTVisitor;
import prism.ModelInfo;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;

public class ExpressionReward extends ExpressionQuant
{
	protected Object rewardStructIndex = null;
	protected Object rewardStructIndexDiv = null;
	
	// Constructors
	
	public ExpressionReward()
	{
	}
	
	public ExpressionReward(Expression expression, String relOpString, Expression r)
	{
		setExpression(expression);
		setRelOp(relOpString);
		setBound(r);
	}

	// Set methods
	
	public void setRewardStructIndex(Object o)
	{
		rewardStructIndex = o;
	}

	public void setRewardStructIndexDiv(Object o)
	{
		rewardStructIndexDiv = o;
	}

	/**
	 * Set the reward bound. Equivalent to {@code setBound(r)}.
	 */
	public void setReward(Expression r)
	{
		setBound(r);
	}

	// Get methods
	
	public Object getRewardStructIndex()
	{
		return rewardStructIndex;
	}

	public Object getRewardStructIndexDiv()
	{
		return rewardStructIndexDiv;
	}

	/**
	 * Get the reward bound. Equivalent to {@code getBound()}.
	 */
	public Expression getReward()
	{
		return getBound();
	}

	// Other methods
	
	/**
	 * Get a string describing the type of R operator, e.g. "R=?" or "R&lt;r".
	 */
	public String getTypeOfROperator()
	{
		String s = "";
		s += "R" + getRelOp();
		s += (getBound() == null) ? "?" : "r";
		return s;
	}

	/**
	 * Get the index of a reward structure (within a model) corresponding to the index of this R operator.
	 * This is 0-indexed (as used e.g. in ModulesFile), not 1-indexed (as seen by user)
	 * Throws an exception (with explanatory message) if it cannot be found.
	 * This means that, the method always returns a valid index if it finishes.
	 */
	public int getRewardStructIndexByIndexObject(ModelInfo modelInfo, Values constantValues) throws PrismException
	{
		return getRewardStructIndexByIndexObject(rewardStructIndex, modelInfo, constantValues);
	}

	/**
	 * Get the index of a reward structure (within a model) corresponding to the rsi reward structure index object.
	 * This is 0-indexed (as used e.g. in ModulesFile), not 1-indexed (as seen by user)
	 * Throws an exception (with explanatory message) if it cannot be found.
	 * This means that, the method always returns a valid index if it finishes.
	 */
	public static int getRewardStructIndexByIndexObject(Object rsi, ModelInfo modelInfo, Values constantValues) throws PrismException
	{
		int rewStruct = -1;
		// Recall: the index is an Object which is either an Integer, denoting the index (starting from 0) directly,
		// or an expression, which can be evaluated (possibly using the passed in constants) to an index. 
		if (modelInfo == null)
			throw new PrismException("No model info to obtain reward structures");
		if (modelInfo.getNumRewardStructs() == 0)
			throw new PrismException("Model has no rewards specified");
		// No index specified - use the first one
		if (rsi == null) {
			rewStruct = 0;
		}
		// Expression - evaluate to an index
		else if (rsi instanceof Expression) {
			int i = ((Expression) rsi).evaluateInt(constantValues);
			rsi = new Integer(i); // (for better error reporting below)
			rewStruct = i - 1;
		}
		// String - name of reward structure
		else if (rsi instanceof String) {
			rewStruct = modelInfo.getRewardStructIndex((String) rsi);
		}
		if (rewStruct == -1) {
			throw new PrismException("Invalid reward structure index \"" + rsi + "\"");
		}
		return rewStruct;
	}

	/**
	 * Get the reward structure (from a model) corresponding to the index of this R operator.
	 * Throws an exception (with explanatory message) if it cannot be found.
	 */
	public RewardStruct getRewardStructByIndexObject(ModelInfo modelInfo, Values constantValues) throws PrismException
	{
		int rewardStructIndex = getRewardStructIndexByIndexObject(modelInfo, constantValues);
		return modelInfo.getRewardStruct(rewardStructIndex);
	}

	/**
	 * Get the reward structure (from a model) corresponding to a reward structure index object.
	 * Throws an exception (with explanatory message) if it cannot be found.
	 */
	public static RewardStruct getRewardStructByIndexObject(Object rsi, ModelInfo modelInfo, Values constantValues) throws PrismException
	{
		int rewardStructIndex = getRewardStructIndexByIndexObject(rsi, modelInfo, constantValues);
		return modelInfo.getRewardStruct(rewardStructIndex);
	}

	
	/**
	 * Get info about the operator and bound.
	 * @param constantValues Values for constants in order to evaluate any bound
	 */
	public OpRelOpBound getRelopBoundInfo(Values constantValues) throws PrismException
	{
		if (getBound() != null) {
			double boundValue = getBound().evaluateDouble(constantValues);
			return new OpRelOpBound("R", getRelOp(), boundValue);
		} else {
			return new OpRelOpBound("R", getRelOp(), null);
		}
	}
	
	// Methods required for Expression:
	
	@Override
	public boolean isConstant()
	{
		return false;
	}

	@Override
	public boolean isProposition()
	{
		return false;
	}
	
	@Override
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate an R operator without a model");
	}

	@Override
	public BigRational evaluateExact(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate an R operator without a model");
	}

	@Override
	public String getResultName()
	{
		// For R=? properties, use name of reward structure where applicable
		if (getBound() == null) {
			String s = "E";
			if (getRelOp() == RelOp.MIN) s = "Minimum e";
			else if (getRelOp() == RelOp.MAX) s = "Maximum e";
			else s = "E";
			if (rewardStructIndex instanceof String) {
				if (rewardStructIndexDiv instanceof String)
					s += "xpected "+rewardStructIndex + "/" + rewardStructIndexDiv;
				else if (rewardStructIndexDiv == null)
					s += "xpected "+rewardStructIndex;
				else
					s += "xpected reward";
			}
			// Or just call it "Expected reward"
			else s += "xpected reward";
			return s;
		}
		// For R>r etc., just use "Result"
		else {
			return "Result";
		}
	}

	@Override
	public boolean returnsSingleValue()
	{
		return false;
	}

	// Methods required for ASTElement:
	
	@Override
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public Expression deepCopy()
	{
		ExpressionReward expr = new ExpressionReward();
		expr.setExpression(getExpression() == null ? null : getExpression().deepCopy());
		expr.setRelOp(getRelOp());
		expr.setBound(getBound() == null ? null : getBound().deepCopy());
		if (rewardStructIndex != null && rewardStructIndex instanceof Expression) expr.setRewardStructIndex(((Expression)rewardStructIndex).deepCopy());
		else expr.setRewardStructIndex(rewardStructIndex);
		if (rewardStructIndexDiv != null && rewardStructIndexDiv instanceof Expression) expr.setRewardStructIndexDiv(((Expression)rewardStructIndexDiv).deepCopy());
		else expr.setRewardStructIndexDiv(rewardStructIndexDiv);
		expr.setFilter(getFilter() == null ? null : (Filter)getFilter().deepCopy());
		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}

	// Standard methods
	
	@Override
	public String toString()
	{
		String s = "";
		
		s += "R" + getModifierString();
		if (rewardStructIndex != null) {
			if (rewardStructIndex instanceof Expression) s += "{"+rewardStructIndex+"}";
			else if (rewardStructIndex instanceof String) s += "{\""+rewardStructIndex+"\"}";
			if (rewardStructIndexDiv != null) {
				s += "/";
				if (rewardStructIndexDiv instanceof Expression) s += "{"+rewardStructIndexDiv+"}";
				else if (rewardStructIndexDiv instanceof String) s += "{\""+rewardStructIndexDiv+"\"}";
			}
		}
		s += getRelOp();
		s += (getBound()==null) ? "?" : getBound().toString();
		s += " [ " + getExpression();
		if (getFilter() != null) s += " "+getFilter();
		s += " ]";
		
		return s;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((rewardStructIndex == null) ? 0 : rewardStructIndex.hashCode());
		result = prime * result + ((rewardStructIndexDiv == null) ? 0 : rewardStructIndexDiv.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ExpressionReward other = (ExpressionReward) obj;
		if (rewardStructIndex == null) {
			if (other.rewardStructIndex != null)
				return false;
		} else if (!rewardStructIndex.equals(other.rewardStructIndex))
			return false;
		if (rewardStructIndexDiv == null) {
			if (other.rewardStructIndexDiv != null)
				return false;
		} else if (!rewardStructIndexDiv.equals(other.rewardStructIndexDiv))
			return false;
		return true;
	}
}

//------------------------------------------------------------------------------
