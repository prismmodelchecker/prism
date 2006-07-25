//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker, Andrew Hinton
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

import java.util.Vector;

import prism.PrismException;
import apmc.*;
import simulator.*;

public class PCTLReward extends PCTLFormulaUnary
{
	String relOp;
	Expression reward;
	Object rewardStructIndex;
	PCTLFormula filter;
	boolean filterMinReq;
	boolean filterMaxReq;
	
	// constructor
	
	public PCTLReward(PCTLFormula f, String ro, Expression r)
	{
		super(f);
		relOp = ro;
		reward = r;
		rewardStructIndex = null;
		filter = null;
		filterMinReq = false;
		filterMaxReq = false;
	}

	public void setRewardStructIndex(Object o)
	{
		rewardStructIndex = o;
	}

	public void setFilter(PCTLFormula f)
	{
		filter = f;
	}

	public void setFilterMinRequested(boolean b)
	{
		filterMinReq = b;
	}

	public void setFilterMaxRequested(boolean b)
	{
		filterMaxReq = b;
	}

	public String getRelOp()
	{
		return relOp;
	}
	
	public Expression getReward()
	{
		return reward;
	}

	public Object getRewardStructIndex()
	{
		return rewardStructIndex;
	}

	public PCTLFormula getFilter()
	{
		return filter;
	}

	public boolean filterMinRequested()
	{
		return filterMinReq;
	}

	public boolean filterMaxRequested()
	{
		return filterMaxReq;
	}

	public boolean noFilterRequests()
	{
		return !(filterMinReq | filterMaxReq);
	}

	// get "name" of the result of this formula (used for y-axis of any graphs plotted)
	
	public String getResultName()
	{
		// default is just "Result", will be overridden where necessary
		return (reward == null) ? "Expected reward" : "Result";
	}

	// check all labels (make sure the referred labels exist)
	
	public void checkLabelIdents(LabelList labelList) throws PrismException
	{
		// call superclass (unary)
		super.checkLabelIdents(labelList);
		// also do filter
		if (filter != null) filter.checkLabelIdents(labelList);
	}

	// find all constants (i.e. locate idents which are constants)
	
	public PCTLFormula findAllConstants(ConstantList constantList) throws PrismException
	{
		// call superclass (unary)
		super.findAllConstants(constantList);
		// also do reward expression
		if (reward != null) reward = reward.findAllConstants(constantList);
		// also do reward struct index
		if (rewardStructIndex != null) if (rewardStructIndex instanceof Expression) rewardStructIndex = ((Expression)rewardStructIndex).findAllConstants(constantList);
		// also do filter
		if (filter != null) filter = filter.findAllConstants(constantList);
		
		return this;
	}
	
	// find all variables (i.e. locate idents which are variables)
	
	public PCTLFormula findAllVars(Vector varIdents, Vector varTypes) throws PrismException
	{
		// call superclass (unary)
		super.findAllVars(varIdents, varTypes);
		// also do reward expression
		if (reward != null) reward = reward.findAllVars(varIdents, varTypes);
		// also do reward struct index
		if (rewardStructIndex != null) if (rewardStructIndex instanceof Expression) rewardStructIndex = ((Expression)rewardStructIndex).findAllVars(varIdents, varTypes);
		// also do filter
		if (filter != null) filter = filter.findAllVars(varIdents, varTypes);
		
		return this;
	}
	
	// type check
	
	public void typeCheck() throws PrismException
	{
		// call superclass (unary)
		super.typeCheck();
		// check reward is double
		if (reward != null) if (!Expression.canAssignTypes(Expression.DOUBLE, reward.getType())) {
			throw new PrismException("Reward \"" + reward + "\" is the wrong type");
		}
		// check reward struct index is integer
		if (rewardStructIndex != null) if (rewardStructIndex instanceof Expression) if (((Expression)rewardStructIndex).getType() != Expression.INT) {
			throw new PrismException("Reward structure index \"" + rewardStructIndex + "\" must be string or integer");
		}
		// check filter is boolean
		if (filter != null) if (filter.getType() != Expression.BOOLEAN) {
			throw new PrismException("Expression \"" + filter + "\" must be Boolean");
		}
		// set type if necesary (default of Boolean already assigned by superclass)
		if (reward == null) setType(Expression.DOUBLE);
	}

	// check everything is ok
	
	public void check() throws PrismException
	{
		// call superclass (unary)
		super.check();
		// check reward is ok and is constant
		if (reward != null) {
			reward.check();
			if (!reward.isConstant()) {
				throw new PrismException("Reward \"" + reward + "\" is not constant");
			}
		}
		// check reward struct index ok and is constant
		if (rewardStructIndex != null) if (rewardStructIndex instanceof Expression) {
			((Expression)rewardStructIndex).check();
			if (!((Expression)rewardStructIndex).isConstant()) {
				throw new PrismException("Reward structure index \"" + rewardStructIndex + "\" is not constant");
			}
		}
		// check filter
		if (filter != null) filter.check();
	}

	// compute the max depth of prob operator nestings
	
	public int computeMaxNested()
	{
		return 1 + super.computeMaxNested();
	}

	// convert to apmc data structures
	
	public int toApmc(Apmc apmc) throws ApmcException
	{
		throw new ApmcException("Reward operators are not supported by APMC techniques");
	}

	/**
	 *	Convert and build simulator data structures
	 *	Note: Although the simulator supports Reward operators, they are only supported
	 *	if they are the top-most formulae, and so are not handled by a toSimulator method.
	 *	Therefore, this method will only be called in error and hence throws an
	 *	exception.
	 */
	public int toSimulator(SimulatorEngine sim ) throws SimulatorException
	{
		throw new SimulatorException("The PRISM simulator does accept PCTL formulae with nested R operators");
	}

	// convert to string
	
	public String toString()
	{
		String s = "";
		
		s += "R";
		if (rewardStructIndex != null) {
			if (rewardStructIndex instanceof Expression) s += "{"+rewardStructIndex+"}";
			else if (rewardStructIndex instanceof String) s += "{\""+rewardStructIndex+"\"}";
		}
		s += relOp;
		s += (reward==null) ? "?" : reward.toString();
		s += " [ " + operand;
		if (filter != null) {
			s += " {" + filter + "}";
			if (filterMinReq) s += "{min}";
			if (filterMaxReq) s += "{max}";
		}
		s += " ]";
		
		return s;
	}
}

//------------------------------------------------------------------------------
