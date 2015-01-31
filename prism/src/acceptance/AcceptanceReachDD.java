package acceptance;
//==============================================================================
//
//Copyright (c) 2014-
//Authors:
//* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//
//------------------------------------------------------------------------------
//
//This file is part of PRISM.
//
//PRISM is free software; you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation; either version 2 of the License, or
//(at your option) any later version.
//
//PRISM is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with PRISM; if not, write to the Free Software Foundation,
//Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
//==============================================================================

import common.IterableBitSet;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;

/**
 * A reachability acceptance condition (based on JDD state sets).
 * The acceptance is defined via a set of goal states and
 * has to be "upward-closed", i.e., once a goal state has been reached,
 * all successor states are goal states as well.
 */
public class AcceptanceReachDD implements AcceptanceOmegaDD
{
	/** The goal states */
	private JDDNode goalStates;

	/**
	 * Constructor, set goalStates.
	 * Becomes owner of the references of goalStates.
	 */
	public AcceptanceReachDD(JDDNode goalStates)
	{
		this.goalStates = goalStates;
	}
	
	/**
	 * Constructor, from a BitSet-based AcceptanceReach.
	 *
	 * @param acceptance the BitSet-based acceptance condition
	 * @param ddRowVars JDDVars of the row variables corresponding to the bits in the bit set
	 */
	public AcceptanceReachDD(AcceptanceReach acceptance, JDDVars ddRowVars)
	{
		goalStates = JDD.Constant(0);
		// get BDD based on the goalState bit set
		for (int i : IterableBitSet.getSetBits(acceptance.getGoalStates())) {
			goalStates = JDD.SetVectorElement(goalStates, ddRowVars, i, 1.0);
		}
	}

	/** Get a referenced copy of the state set of the goal states.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getGoalStates()
	{
		JDD.Ref(goalStates);
		return goalStates;
	}

	/**
	 * Set the goal states.
	 * Becomes owner of the reference to goalStates.
	 */
	public void setGoalStates(JDDNode goalStates)
	{
		clear();
		this.goalStates = goalStates;
	}

	@Override
	public boolean isBSCCAccepting(JDDNode bscc_states)
	{
		return JDD.AreInterecting(goalStates, bscc_states);
	}

	@Override
	public String getSizeStatistics()
	{
		return "one set of goal states";
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

	@Override
	public void clear()
	{
		if (goalStates != null) {
			JDD.Deref(goalStates);
		}
	}

}
