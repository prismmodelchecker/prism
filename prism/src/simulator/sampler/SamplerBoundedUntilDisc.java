//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package simulator.sampler;

import simulator.*;
import prism.*;
import parser.*;
import parser.ast.*;

public class SamplerBoundedUntilDisc extends SamplerBoolean
{
	private Expression left;
	private Expression right;
	private int lb;
	private int ub;
	private boolean haveUpperBound;

	/**
	 * Construct a sampler for a (discrete-time) bounded until property.
	 * Passed in ExpressionTemporal should be a property of this type.
	 * All constants should have already been evaluated/replaced.
	 */
	public SamplerBoundedUntilDisc(ExpressionTemporal expr) throws PrismException
	{
		// Make sure expression is of the correct type
		// Then extract other required info
		if (expr.getOperator() != ExpressionTemporal.P_U)
			throw new PrismException("Error creating Sampler");
		left = expr.getOperand1();
		right = expr.getOperand2();
		// Lower bound
		if (expr.getLowerBound() != null) {
			lb = expr.getLowerBound().evaluateInt();
			if (expr.lowerBoundIsStrict()) {
				// Convert to non-strict bound:  >lb  <=>  >=lb+1
				lb = lb + 1;
			}
		} else {
			// No lower bound
			lb = 0;
		}
		if (lb < 0) {
			throw new PrismException("Invalid lower bound in "+expr);
		}
		// Upper bound
		if (expr.getUpperBound() != null) {
			haveUpperBound = true;
			ub = expr.getUpperBound().evaluateInt();
			if (expr.upperBoundIsStrict()) {
				// Convert to non-strict bound:  <ub  <=>  <=ub-1
				ub = ub - 1;
			}

			if (ub < 0) {
				throw new PrismException("Invalid upper bound in "+expr);
			}
		} else {
			// No upper bound
			haveUpperBound = false;
		}

		// Initialise sampler info
		reset();
		resetStats();
	}

	@Override
	public boolean update(Path path, TransitionList transList) throws PrismLangException
	{
		// If the answer is already known we should do nothing
		if (valueKnown)
			return true;
		
		long pathSize = path.size();
		// Upper bound exceeded
		if (haveUpperBound && pathSize > ub) {
			valueKnown = true;
			value = false;
		}
		// Lower bound not yet exceeded but LHS of until violated
		// (no need to check RHS because too early)
		else if (pathSize < lb) {
			if (!left.evaluateBoolean(path.getCurrentState())) {
				valueKnown = true;
				value = false;
			}
		}
		// Current time is between lower/upper bounds...
		else {
			State currentState = path.getCurrentState();
			// Have we reached the target (i.e. RHS of until)?
			if (right.evaluateBoolean(currentState)) {
				valueKnown = true;
				value = true;
			}
			// Or, if not, have we violated the LHS of the until?
			else if (!left.evaluateBoolean(currentState)) {
				valueKnown = true;
				value = false;
			}
			// Or, if we are now at a deadlock
			else if (transList != null && transList.isDeadlock()) {
				valueKnown = true;
				value = false;
			}
			// Otherwise, don't know
		}
		
		return valueKnown;
	}
	
	@Override
	public boolean needsBoundedNumSteps()
	{
		// Bounded if there is an upper bound
		return haveUpperBound;
	}
}
