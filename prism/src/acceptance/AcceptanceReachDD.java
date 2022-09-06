//==============================================================================
//
//Copyright (c) 2014-
//Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

package acceptance;

import common.IterableBitSet;
import jdd.JDD;
import jdd.JDDNode;
import jdd.JDDVars;
import prism.PrismNotSupportedException;

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
		return JDD.AreIntersecting(goalStates, bscc_states);
	}

	@Override
	public AcceptanceReachDD clone()
	{
		return new AcceptanceReachDD(goalStates.copy());
	}

	@Override
	public void intersect(JDDNode restrict)
	{
		goalStates = JDD.And(goalStates, restrict.copy());
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
	public AcceptanceOmegaDD complement(AcceptanceType... allowedAcceptance) throws PrismNotSupportedException
	{
		if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.RABIN)) {
			return complementToRabin();
		} else if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.STREETT)) {
			return complementToStreett();
		} else if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.GENERIC)) {
			return complementToGeneric();
		}
		throw new PrismNotSupportedException("Can not complement " + getType() + " acceptance to a supported acceptance type");
	}

	/**
	 * Get a Rabin acceptance condition that is the complement of this condition, i.e.,
	 * any word that is accepted by this condition is rejected by the returned Rabin condition.
	 * <br>
	 * Relies on the fact that once the goal states have been reached, all subsequent states
	 * are goal states.
	 *
	 * @return the complement Rabin acceptance condition
	 */
	public AcceptanceRabinDD complementToRabin()
	{
		AcceptanceRabinDD rabin = new AcceptanceRabinDD();
		rabin.add(new AcceptanceRabinDD.RabinPairDD(goalStates.copy(), JDD.Constant(1)));
		return rabin;
	}

	/**
	 * Get a Streett acceptance condition that is the complement of this condition, i.e.,
	 * any word that is accepted by this condition is rejected by the returned Streett condition.
	 * <br>
	 * Relies on the fact that once the goal states have been reached, all subsequent states
	 * are goal states.
	 *
	 * @return the complement Streett acceptance condition
	 */
	public AcceptanceStreettDD complementToStreett()
	{
		AcceptanceStreettDD streett = new AcceptanceStreettDD();
		streett.add(new AcceptanceStreettDD.StreettPairDD(goalStates.copy(), JDD.Constant(0)));
		return streett;
	}

	@Override
	public AcceptanceGenericDD toAcceptanceGeneric()
	{
		return new AcceptanceGenericDD(AcceptanceGeneric.ElementType.INF, goalStates.copy());
	}

	@Override
	@Deprecated
	public String getTypeAbbreviated() {
		return getType().getNameAbbreviated();
	}

	@Override
	@Deprecated
	public String getTypeName() {
		return getType().getName();
	}

	@Override
	public void clear()
	{
		if (goalStates != null) {
			JDD.Deref(goalStates);
		}
	}

}
