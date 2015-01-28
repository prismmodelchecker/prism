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

package parser.visitor;

import parser.ast.*;
import parser.type.*;
import prism.PrismException;
import prism.PrismLangException;
import prism.ModelType;

/**
 * Check expression (property) for validity with respect to a particular model type
 * (i.e. whether not it is a property that can be model checked for that model type).
 */
public class CheckValid extends ASTTraverse
{
	private ModelType modelType = null;

	public CheckValid(ModelType modelType)
	{
		this.modelType = modelType;
	}

	public void visitPost(ExpressionTemporal e) throws PrismLangException
	{
		// R operator types restricted for some models
		if (modelType == ModelType.MDP) {
			if (e.getOperator() == ExpressionTemporal.R_S) {
				throw new PrismLangException("Steady-state reward properties cannot be used for MDPs");
			}
		}
		else if (modelType == ModelType.PTA) {
			if (e.getOperator() == ExpressionTemporal.R_C || e.getOperator() == ExpressionTemporal.R_I || e.getOperator() == ExpressionTemporal.R_S) {
				throw new PrismLangException("Only reachability (F) reward properties can be used for PTAs");
			}
		}
		// PTA only support upper time bounds
		if (e.getLowerBound() != null) {
			if (modelType == ModelType.PTA) {
				throw new PrismLangException("Only upper time bounds are allowed on the " + e.getOperatorSymbol()
						+ " operator for PTAs");
			}
		}
		// Apart from CTMCs, we only support integer time bounds
		if ((e.getUpperBound() != null && !(e.getUpperBound().getType() instanceof TypeInt)) ||
		    (e.getLowerBound() != null && !(e.getLowerBound().getType() instanceof TypeInt))) {
			if (modelType == ModelType.DTMC) {
				throw new PrismLangException("Time bounds on the " + e.getOperatorSymbol()
						+ " operator must be integers for DTMCs");
			}
			if (modelType == ModelType.MDP) {
				throw new PrismLangException("Time bounds on the " + e.getOperatorSymbol()
						+ " operator must be integers for MDPs");
			}
			if (modelType == ModelType.PTA) {
				throw new PrismLangException("Time bounds on the " + e.getOperatorSymbol()
						+ " operator must be integers for PTAs");
			}
		}
		// Don't allow lower bounds on weak until - does not have intuitive semantics
		if (e.getOperator() == ExpressionTemporal.P_W && e.getLowerBound() != null) {
			throw new PrismLangException("The weak until operator (W) with lower bounds is not yet supported");
		}
	}

	public void visitPost(ExpressionProb e) throws PrismLangException
	{
		if (modelType.nondeterministic() && e.getRelOp() == RelOp.EQ)
			throw new PrismLangException("Can't use \"P=?\" for nondeterministic models; use \"Pmin=?\" or \"Pmax=?\"");
	}
	
	public void visitPost(ExpressionReward e) throws PrismLangException
	{
		if (modelType.nondeterministic() && e.getRelOp() == RelOp.EQ)
			throw new PrismLangException("Can't use \"R=?\" for nondeterministic models; use \"Rmin=?\" or \"Rmax=?\"");
		if (e.getRewardStructIndexDiv() != null)
			throw new PrismLangException("No support for ratio reward objectives yet");
	}
	
	public void visitPost(ExpressionSS e) throws PrismLangException
	{
		// S operator only works for some models
		if (modelType == ModelType.MDP) {
			throw new PrismLangException("The S operator cannot be used for MDPs");
		}
		if (modelType == ModelType.PTA) {
			throw new PrismLangException("The S operator cannot be used for PTAs");
		}
		/*if (modelType.nondeterministic() && e.getRelOp() == RelOp.EQ)
			throw new PrismLangException("Can't use \"S=?\" for nondeterministic models; use \"Smin=?\" or \"Smax=?\"");*/
	}

	public void visitPost(ExpressionStrategy e) throws PrismLangException
	{
		if (!modelType.nondeterministic())
			throw new PrismLangException("The " + e.getOperatorString() + " operator is only meaningful for models with nondeterminism");
	}
}
