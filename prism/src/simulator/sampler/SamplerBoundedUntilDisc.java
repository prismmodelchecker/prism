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
import parser.*;
import parser.ast.*;

public class SamplerBoundedUntilDisc extends SamplerBoolean
{
	private Expression left;
	private Expression right;
	private int lb;
	private int ub;

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
		lb = expr.getLowerBound() == null ? 0 : expr.getLowerBound().evaluateInt();
		ub = expr.getUpperBound() == null ? Integer.MAX_VALUE : expr.getUpperBound().evaluateInt();
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
		if (pathSize > ub) {
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
			// Or, if not, have we violated the LJS of the until?
			else if (!left.evaluateBoolean(currentState)) {
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
		// Always bounded
		return true;
	}
}
