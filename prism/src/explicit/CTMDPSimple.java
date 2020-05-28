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

import prism.PrismUtils;

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

	/**
	 * Construct a CTMDP from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 * Note: have to build new Distributions from scratch anyway to do this,
	 * so may as well provide this functionality as a constructor.
	 */
	public CTMDPSimple(CTMDPSimple ctmdp, int permut[])
	{
		super(ctmdp, permut);
	}

	// Accessors (for CTMDP)

	@Override
	public double getMaxExitRate()
	{
		int i;
		double d, max = Double.NEGATIVE_INFINITY;
		for (i = 0; i < numStates; i++) {
			for (Distribution distr : trans.get(i)) {
				d = distr.sum();
				if (d > max)
					max = d;
			}
		}
		return max;
	}

	@Override
	public boolean isLocallyUniform()
	{
		int i, j, n;
		double d;
		for (i = 0; i < numStates; i++) {
			n = trans.get(i).size();
			if (n < 2)
				continue;
			d = trans.get(i).get(0).sum();
			for (j = 1; j < n; j++) {
				if (!PrismUtils.doublesAreCloseAbs(trans.get(i).get(j).sum(), d, 1e-12))
					return false;
			}
		}
		return true;
	}

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
		double sum, d;
		mdp = new MDPSimple(numStates);
		for (int in : getInitialStates()) {
			mdp.addInitialState(in);
		}
		for (i = 0; i < numStates; i++) {
			for (Distribution distr : trans.get(i)) {
				distrNew = new Distribution();
				sum = distr.sum();
				d = Math.exp(-sum * tau);
				for (Map.Entry<Integer, Double> e : distr) {
					distrNew.add(e.getKey(), (1 - d) * (e.getValue() / sum));
				}
				distrNew.add(i, d);
				mdp.addChoice(i, distrNew);
			}
		}
		return mdp;
	}
}
