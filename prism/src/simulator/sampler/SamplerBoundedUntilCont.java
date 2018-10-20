//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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
import parser.ast.*;

/**
 * Construct a sampler for a (continuous-time) bounded until property.
 * Passed in ExpressionTemporal should be a formula of this type.
 * All constants should have already been evaluated/replaced.
 */
public class SamplerBoundedUntilCont extends SamplerBoolean
{
	private Expression left;
	private Expression right;
	private double lb;
	private double ub;

	/**
	 * Construct a sampler for a (continuous) time-bounded until formula.
	 * Passed in ExpressionTemporal should be a property of this type.
	 * All constants should have already been evaluated/replaced.
	 */
	public SamplerBoundedUntilCont(ExpressionTemporal expr) throws PrismException
	{
		// Make sure expression is of the correct type
		// Then extract other required info
		if (expr.getOperator() != ExpressionTemporal.P_U)
			throw new PrismException("Error creating Sampler");
		left = expr.getOperand1();
		right = expr.getOperand2();

		lb = expr.getLowerBound() == null ? 0.0 : expr.getLowerBound().evaluateDouble();
		if (lb < 0) {
			throw new PrismException("Invalid lower bound " + lb + " in time-bounded until formula");
		}

		if (expr.getUpperBound() == null) {
			ub = Double.POSITIVE_INFINITY;
		} else {
			ub = expr.getUpperBound().evaluateDouble();
			if (ub < 0 || (ub == 0 && expr.upperBoundIsStrict())) {
				String bound = (expr.upperBoundIsStrict() ? "<" : "<=") + ub;
				throw new PrismException("Invalid upper bound " + bound + " in time-bounded until formula");
			}
			if (ub < lb) {
				throw new PrismException("Upper bound must exceed lower bound in time-bounded until formula");
			}
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
		
		// For continuous-time bounded until, we may need to look back at previous state.
		// So, treat first/subsequent calls to update() differently. 

		// First path state:
		if (path.size() == 0) {
			// Initially, zero time has elapsed so to satisfy the until,
			// we need a lower time bound of 0 and the RHS to be satisfied 
			if (lb == 0.0) {
				if (right.evaluateBoolean(path.getCurrentState())) {
					valueKnown = true;
					value = true;
				}
			}
			// If LHS of the until violated, will never be true
			else {
				if (!left.evaluateBoolean(path.getCurrentState())) {
					valueKnown = true;
					value = false;
				}
			}
		}
		// Second and subsequent path states:
		else {
			double timeSoFar = path.getTotalTime();
			// As soon as upper time bound exceeded, we can decide
			if (timeSoFar > ub) {
				// Upper time bound was exceeded (for first time) in *previous* state
				// Had we reached the target (i.e. RHS of until)?
				if (right.evaluateBoolean(path.getPreviousState())) {
					valueKnown = true;
					value = true;
				}
				// If not, it's too late now
				else {
					valueKnown = true;
					value = false;
				}
			}
			// Lower bound not yet exceeded but LHS of until violated
			// (no need to check RHS because too early)
			else if (timeSoFar <= lb) {
				if (!left.evaluateBoolean(path.getCurrentState())) {
					valueKnown = true;
					value = false;
				}
			}
			// Current time is between lower/upper bounds...
			else {
				// Have we reached the target (i.e. RHS of until)?
				if (right.evaluateBoolean(path.getCurrentState())) {
					valueKnown = true;
					value = true;
				}
				// Or, if not, have we violated the LHS of the until?
				else if (!left.evaluateBoolean(path.getCurrentState())) {
					valueKnown = true;
					value = false;
				}
				// If the lower bound was exceeded for the first time in the
				// previous state, and that one satisfies the RHS of the until...
				else if (timeSoFar - path.getTimeInPreviousState() <= lb) {
					if (right.evaluateBoolean(path.getPreviousState())) {
						valueKnown = true;
						value = true;
					}
				}
				// Or, if we are now at a deadlock
				else if (transList != null && transList.isDeadlock()) {
					valueKnown = true;
					value = false;
				}
				// Otherwise, don't know
			}
		}
		
		return valueKnown;
	}
	
	@Override
	public boolean needsBoundedNumSteps()
	{
		// Bounded if there is a non-finite upper-bound (although we don't know the exact num steps, just the time bound)
		return ub < Double.POSITIVE_INFINITY;
	}
}
