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

import parser.EvaluateContext;
import parser.Values;
import parser.visitor.ASTVisitor;
import prism.OpRelOpBound;
import prism.PrismException;
import prism.PrismLangException;

public class ExpressionReward extends Expression implements ExpressionQuant
{
	Object rewardStructIndex = null;
	Object rewardStructIndexDiv = null;
	RelOp relOp = null;
	Expression reward = null;
	Expression expression = null;
	// Note: this "old-style" filter is just for display purposes
	// The parser creates an (invisible) new-style filter around this expression
	Filter filter = null;
	
	// Constructors
	
	public ExpressionReward()
	{
	}
	
	public ExpressionReward(Expression e, String r, Expression p)
	{
		expression = e;
		relOp = RelOp.parseSymbol(r);
		reward = p;
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

	public void setRelOp(RelOp relOp)
	{
		this.relOp = relOp;
	}

	public void setRelOp(String r)
	{
		relOp = RelOp.parseSymbol(r);
	}

	public void setReward(Expression p)
	{
		reward = p;
	}

	public void setExpression(Expression e)
	{
		expression = e;
	}
	
	public void setFilter(Filter f)
	{
		filter = f;
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

	public RelOp getRelOp()
	{
		return relOp;
	}
	
	public Expression getReward()
	{
		return reward;
	}

	public Expression getExpression()
	{
		return expression;
	}
	
	public Filter getFilter()
	{
		return filter;
	}

	// Other methods
	
	/**
	 * Get a string describing the type of R operator, e.g. "R=?" or "R<r".
	 */
	public String getTypeOfROperator()
	{
		String s = "";
		s += "R" + relOp;
		s += (reward == null) ? "?" : "r";
		return s;
	}

	/**
	 * Get the index of a reward structure (within a model) corresponding to the index of this R operator.
	 * This is 0-indexed (as used e.g. in ModulesFile), not 1-indexed (as seen by user)
	 * Throws an exception (with explanatory message) if it cannot be found.
	 * This means that, the method always returns a valid index if it finishes.
	 */
	public int getRewardStructIndexByIndexObject(ModulesFile modulesFile, Values constantValues) throws PrismException
	{
		int rewStruct = -1;
		Object rsi = rewardStructIndex;
		// Recall: the index is an Object which is either an Integer, denoting the index (starting from 0) directly,
		// or an expression, which can be evaluated (possibly using the passed in constants) to an index. 
		if (modulesFile == null)
			throw new PrismException("No model file to obtain reward structures");
		if (modulesFile.getNumRewardStructs() == 0)
			throw new PrismException("Model has no rewards specified");
		// No index specified - use the first one
		if (rsi == null) {
			rewStruct = 0;
		}
		// Expression - evaluate to an index
		else if (rewardStructIndex instanceof Expression) {
			int i = ((Expression) rewardStructIndex).evaluateInt(constantValues);
			rsi = new Integer(i); // (for better error reporting below)
			rewStruct = i - 1;
		}
		// String - name of reward structure
		else if (rsi instanceof String) {
			rewStruct = modulesFile.getRewardStructIndex((String) rsi);
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
	public RewardStruct getRewardStructByIndexObject(ModulesFile modulesFile, Values constantValues) throws PrismException
	{
		int rewardStructIndex = getRewardStructIndexByIndexObject(modulesFile, constantValues);
		return modulesFile.getRewardStruct(rewardStructIndex);
	}
	
	/**
	 * Get info about the operator and bound.
	 * @param constantValues Values for constants in order to evaluate any bound
	 */
	public OpRelOpBound getRelopBoundInfo(Values constantValues) throws PrismException
	{
		if (reward != null) {
			double bound = reward.evaluateDouble(constantValues);
			return new OpRelOpBound("R", relOp, bound);
		} else {
			return new OpRelOpBound("R", relOp, null);
		}
	}
	
	// Methods required for Expression:
	
	/**
	 * Is this expression constant?
	 */
	public boolean isConstant()
	{
		return false;
	}

	@Override
	public boolean isProposition()
	{
		return false;
	}
	
	/**
	 * Evaluate this expression, return result.
	 * Note: assumes that type checking has been done already.
	 */
	public Object evaluate(EvaluateContext ec) throws PrismLangException
	{
		throw new PrismLangException("Cannot evaluate an R operator without a model");
	}

	/**
	  * Get "name" of the result of this expression (used for y-axis of any graphs plotted)
	  */
	public String getResultName()
	{
		// For R=? properties, use name of reward structure where applicable
		if (reward == null) {
			String s = "E";
			if (relOp == RelOp.MIN) s = "Minimum e";
			else if (relOp == RelOp.MAX) s = "Maximum e";
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
	
	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}
	
	/**
	 * Convert to string.
	 */
	public String toString()
	{
		String s = "";
		
		s += "R";
		if (rewardStructIndex != null) {
			if (rewardStructIndex instanceof Expression) s += "{"+rewardStructIndex+"}";
			else if (rewardStructIndex instanceof String) s += "{\""+rewardStructIndex+"\"}";
			if (rewardStructIndexDiv != null) {
				s += "/";
				if (rewardStructIndexDiv instanceof Expression) s += "{"+rewardStructIndexDiv+"}";
				else if (rewardStructIndexDiv instanceof String) s += "{\""+rewardStructIndexDiv+"\"}";
			}
		}
		s += relOp;
		s += (reward==null) ? "?" : reward.toString();
		s += " [ " + expression;
		if (filter != null) s += " "+filter;
		s += " ]";
		
		return s;
	}

	/**
	 * Perform a deep copy.
	 */
	public Expression deepCopy()
	{
		ExpressionReward expr = new ExpressionReward();
		expr.setExpression(expression == null ? null : expression.deepCopy());
		expr.setRelOp(relOp);
		expr.setReward(reward == null ? null : reward.deepCopy());
		if (rewardStructIndex != null && rewardStructIndex instanceof Expression) expr.setRewardStructIndex(((Expression)rewardStructIndex).deepCopy());
		else expr.setRewardStructIndex(rewardStructIndex);
		if (rewardStructIndexDiv != null && rewardStructIndexDiv instanceof Expression) expr.setRewardStructIndexDiv(((Expression)rewardStructIndexDiv).deepCopy());
		else expr.setRewardStructIndexDiv(rewardStructIndexDiv);
		expr.setFilter(filter == null ? null : (Filter)filter.deepCopy());
		expr.setType(type);
		expr.setPosition(this);
		return expr;
	}
}

//------------------------------------------------------------------------------
