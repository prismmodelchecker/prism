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

import java.io.PrintStream;
import java.util.BitSet;

import prism.PrismException;
import prism.PrismNotSupportedException;
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

	/**
	 * Get a Rabin acceptance condition that is the complement of this condition, i.e.,
	 * any word that is accepted by this condition is rejected by the returned Rabin condition.
	 * <br>
	 * Relies on the fact that once the goal states have been reached, all subsequent states
	 * are goal states.
	 *
	 * @param numStates the number of states in the underlying model / automaton (needed for complementing BitSets)
	 * @return the complement Rabin acceptance condition
	 */
	public AcceptanceRabin complementToRabin(int numStates)
	{
		AcceptanceRabin rabin = new AcceptanceRabin();
		BitSet allStates = new BitSet();
		allStates.set(0, numStates);
		rabin.add(new AcceptanceRabin.RabinPair((BitSet) goalStates.clone(), allStates));
		return rabin;
	}

	/**
	 * Get a Streett acceptance condition that is the complement of this condition, i.e.,
	 * any word that is accepted by this condition is rejected by the returned Streett condition.
	 * <br>
	 * Relies on the fact that once the goal states have been reached, all subsequent states
	 * are goal states.
	 *
	 * @param numStates the number of states in the underlying model / automaton (needed for complementing BitSets)
	 * @return the complement Streett acceptance condition
	 */
	public AcceptanceStreett complementToStreett(int numStates)
	{
		AcceptanceStreett streett = new AcceptanceStreett();
		streett.add(new AcceptanceStreett.StreettPair((BitSet) goalStates.clone(), new BitSet()));
		return streett;
	}

	/** Complement this acceptance condition, return as AcceptanceGeneric. */
	public AcceptanceGeneric complementToGeneric()
	{
		return toAcceptanceGeneric().complementToGeneric();
	}

	@Override
	public AcceptanceOmega complement(int numStates, AcceptanceType... allowedAcceptance) throws PrismException
	{
		if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.RABIN)) {
			return complementToRabin(numStates);
		} else if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.STREETT)) {
			return complementToStreett(numStates);
		} else if (AcceptanceType.contains(allowedAcceptance, AcceptanceType.GENERIC)) {
			return complementToGeneric();
		}
		throw new PrismNotSupportedException("Can not complement " + getType() + " acceptance to a supported acceptance type");
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
	public AcceptanceGeneric toAcceptanceGeneric()
	{
		return new AcceptanceGeneric(AcceptanceGeneric.ElementType.INF, (BitSet) goalStates.clone());
	}

	@Override
	public String getSignatureForState(int i)
	{
		return goalStates.get(i) ? "!" : " ";
	}
	
	@Override
	public String getSignatureForStateHOA(int stateIndex)
	{
		if (goalStates.get(stateIndex)) {
			return "{0}";
		} else {
			return "";
		}
	}

	/** Returns a textual representation of this acceptance condition. */
	@Override
	public String toString()
	{
		return goalStates.toString();
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
	public void outputHOAHeader(PrintStream out)
	{
		out.println("acc-name: Buchi");
		out.println("Acceptance: 1 Inf(0)");
	}
}
