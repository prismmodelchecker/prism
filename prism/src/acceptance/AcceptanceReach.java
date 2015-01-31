//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
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

package acceptance;

import java.util.BitSet;

import jdd.JDDVars;

/**
 * A reachability acceptance condition (based on BitSet state sets).
 * The acceptance is defined via a set of goal states and
 * has to be "upward-closed", i.e., once a goal state has been reached,
 * all successor states are goal states as well.
 */
public class AcceptanceReach implements AcceptanceOmega
{
	/** The set of goal states */
	private BitSet goalStates = new BitSet();

	/** Constructor (no goal states) */
	public AcceptanceReach()
	{
	}

	/** Constructor (set goal states) */
	public AcceptanceReach(BitSet goalStates)
	{
		this.goalStates = goalStates;
	}

	/** Get the goal state set */
	public BitSet getGoalStates()
	{
		return goalStates;
	}

	/** Set the goal state set */
	public void setGoalStates(BitSet goalStates)
	{
		this.goalStates = goalStates;
	}

	/** Make a copy of the acceptance condition. */
	public AcceptanceReach clone()
	{
		return new AcceptanceReach((BitSet)goalStates.clone());
	}
	
	@Override
	public boolean isBSCCAccepting(BitSet bscc_states)
	{
		return bscc_states.intersects(goalStates);
	}


	@Override
	public void lift(LiftBitSet lifter)
	{
		goalStates=lifter.lift(goalStates);
	}

	@Override
	public AcceptanceReachDD toAcceptanceDD(JDDVars ddRowVars)
	{
		return new AcceptanceReachDD(this, ddRowVars);
	}

	@Override
	public String getSignatureForState(int i)
	{
		return goalStates.get(i) ? "!" : " ";
	}

	@Override
	public String getSizeStatistics()
	{
		return goalStates.cardinality()+" goal states";
	}

	@Override
	public AcceptanceType getType()
	{
		return AcceptanceType.REACH;
	}

	@Override
	public String getTypeAbbreviated()
	{
		return "F";
	}

	@Override
	public String getTypeName()
	{
		return "Finite";
	}

}
