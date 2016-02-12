//==============================================================================
//
//Copyright (c) 2016-
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

/**
 * A Büchi acceptance condition (based on JDD state sets).
 * The acceptance is defined via a set of "accepting" states
 * (sometimes also called final states) and is accepting if
 *  "infinitely often an accepting state is visited"
 */
public class AcceptanceBuchiDD implements AcceptanceOmegaDD
{
	/** The accepting states */
	private JDDNode acceptingStates;

	/**
	 * Constructor, set acceptingStates.
	 * Becomes owner of the references of acceptingStates.
	 */
	public AcceptanceBuchiDD(JDDNode acceptingStates)
	{
		this.acceptingStates = acceptingStates;
	}
	
	/**
	 * Constructor, from a BitSet-based AcceptanceBuchi.
	 *
	 * @param acceptance the BitSet-based acceptance condition
	 * @param ddRowVars JDDVars of the row variables corresponding to the bits in the bit set
	 */
	public AcceptanceBuchiDD(AcceptanceBuchi acceptance, JDDVars ddRowVars)
	{
		acceptingStates = JDD.Constant(0);
		// get BDD based on the acceptingState bit set
		for (int i : IterableBitSet.getSetBits(acceptance.getAcceptingStates())) {
			acceptingStates = JDD.SetVectorElement(acceptingStates, ddRowVars, i, 1.0);
		}
	}

	/** Get a referenced copy of the state set of the accepting states.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getAcceptingStates()
	{
		JDD.Ref(acceptingStates);
		return acceptingStates;
	}

	/**
	 * Set the accepting states.
	 * Becomes owner of the reference to acceptingStates.
	 */
	public void setAcceptingStates(JDDNode acceptingStates)
	{
		clear();
		this.acceptingStates = acceptingStates;
	}

	@Override
	public boolean isBSCCAccepting(JDDNode bscc_states)
	{
		return JDD.AreIntersecting(acceptingStates, bscc_states);
	}

	@Override
	public String getSizeStatistics()
	{
		return "one set of accepting states";
	}

	@Override
	public AcceptanceType getType()
	{
		return AcceptanceType.BUCHI;
	}

	@Override
	@Deprecated
	public String getTypeAbbreviated()
	{
		return getType().getNameAbbreviated();
	}

	@Override
	@Deprecated
	public String getTypeName()
	{
		return getType().getName();
	}

	@Override
	public void clear()
	{
		if (acceptingStates != null) {
			JDD.Deref(acceptingStates);
		}
	}

}
