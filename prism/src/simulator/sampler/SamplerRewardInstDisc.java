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

public class SamplerRewardInstDisc extends SamplerDouble
{
	private int time;
	private int rewardStructIndex;

	/**
	 * Construct a sampler for a (discrete-time) instantaneous reward property.
	 * Passed in ExpressionTemporal should be a property of this type.
	 * Reward structure index should also be specified.
	 * All constants should have already been evaluated/replaced.
	 */
	public SamplerRewardInstDisc(ExpressionTemporal expr, int rewardStructIndex) throws PrismException
	{
		// Make sure expression is of the correct type
		// Then extract other required info
		if (expr.getOperator() != ExpressionTemporal.R_I)
			throw new PrismException("Error creating Sampler");
		time = expr.getUpperBound().evaluateInt();
		this.rewardStructIndex = rewardStructIndex;
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
		
		// As soon as time bound reached, store current state reward
		if (path.size() == time) {
			valueKnown = true;
			value = path.getCurrentStateReward(rewardStructIndex);
		}
		// Or, if we are now at a deadlock
		else if (transList != null && transList.isDeadlock()) {
			valueKnown = true;
			value = path.getCurrentStateReward(rewardStructIndex);
		}
		// Otherwise, don't know
		
		return valueKnown;
	}
	
	@Override
	public boolean needsBoundedNumSteps()
	{
		// Always bounded
		return true;
	}
}
