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

package explicit;

import java.util.Map;

/**
 * Simple explicit-state representation of a CTMDP.
 */
public class CTMDPSimple extends MDPSimple implements CTMDP
{
	// Constructors

	/**
	 * Constructor: empty CTMDP.
	 */
	public CTMDPSimple()
	{
		initialise(0);
	}

	/**
	 * Constructor: new CTMDP with fixed number of states.
	 */
	public CTMDPSimple(int numStates)
	{
		initialise(numStates);
	}

	/**
	 * Copy constructor.
	 */
	public CTMDPSimple(CTMDPSimple ctmdp)
	{
		super(ctmdp);
	}

	// Accessors (for CTMDP)

	@Override
	public MDP buildImplicitDiscretisedMDP(double tau)
	{
		// TODO
		return null;
	}

	@Override
	public MDPSimple buildDiscretisedMDP(double tau)
	{
		MDPSimple mdp;
		Distribution distrNew;
		int i;
		double d;
		mdp = new MDPSimple(numStates);
		for (int in : getInitialStates()) {
			mdp.addInitialState(in);
		}
		for (i = 0; i < numStates; i++) {
			for (Distribution distr : trans.get(i)) {
				distrNew = new Distribution();
				d = Math.exp(-distr.sum() * tau);
				for (Map.Entry<Integer, Double> e : distr) {
					distrNew.add(e.getKey(), (1 - d) * e.getValue());
				}
				distrNew.add(i, d);
				mdp.addChoice(i, distrNew);
			}
		}
		return mdp;
	}
}
