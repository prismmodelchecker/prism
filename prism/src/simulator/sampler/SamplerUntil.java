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

import parser.State;
import parser.ast.Expression;
import parser.ast.ExpressionTemporal;
import prism.ModelGenerator;
import prism.PrismException;
import simulator.Path;

public class SamplerUntil extends SamplerBoolean
{
	private Expression left;
	private Expression right;

	/**
	 * Construct a sampler for a (unbounded) until property.
	 * Passed in ExpressionTemporal should be a property of this type.
	 * All constants should have already been evaluated/replaced.
	 */
	public SamplerUntil(ExpressionTemporal expr) throws PrismException
	{
		// Make sure expression is of the correct type
		// Then extract other required info
		if (expr.getOperator() != ExpressionTemporal.P_U)
			throw new PrismException("Error creating Sampler");
		left = expr.getOperand1();
		right = expr.getOperand2();
		// Initialise sampler info
		reset();
		resetStats();
	}

	@Override
	public boolean update(Path path, ModelGenerator modelGen) throws PrismException
	{
		// If the answer is already known we should do nothing
		if (valueKnown)
			return true;

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
		// Or, if we are now at a deadlock/self-loop
		else if (modelGen != null && (modelGen.isDeadlock() || path.isLooping())) {
		//else if (transList != null && (transList.isDeadlock() || transList.isDeterministicSelfLoop(currentState))) {
			valueKnown = true;
			value = false;
		}
		// Otherwise, don't know

		return valueKnown;
	}
}
