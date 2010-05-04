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
		ub = expr.getUpperBound() == null ? Double.POSITIVE_INFINITY: expr.getUpperBound().evaluateDouble();
		// Initialise sampler info
		reset();
		resetStats();
	}

	@Override
	public void update(Path path) throws PrismLangException
	{
		if (path.size() > 0) {
			double timeSoFar = path.getTimeSoFar();
			// As soon as upper time bound exceeded, we can decide
			if (timeSoFar > ub) {
				// must take into account the possibility of missing lower bound

				// could have missed out evaluation of right
				if (timeSoFar - path.getTimeInPreviousState() <= lb) {
					if (right.evaluateBoolean(path.getPreviousState())) {
						valueKnown = true;
						value = true;
					} else {
						valueKnown = true;
						value = false;
					}
				} else {
					valueKnown = true;
					value = false;
				}
			} else if (timeSoFar <= lb) {
				if (!left.evaluateBoolean(path.getCurrentState())) {
					valueKnown = true;
					value = false;
				}
			} else {
				if (right.evaluateBoolean(path.getCurrentState())) {
					valueKnown = true;
					value = true;
				} else if (!left.evaluateBoolean(path.getCurrentState())) {
					valueKnown = true;
					value = false;
				}
			}
		} else {
			if (lb == 0.0) {
				if (right.evaluateBoolean(path.getCurrentState())) {
					valueKnown = true;
					value = true;
				}
			} else {
				if (!left.evaluateBoolean(path.getCurrentState())) {
					valueKnown = true;
					value = false;
				}
			}
		}
	}
}
