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

import java.util.BitSet;
import java.util.Map;

/**
 * Simple explicit-state representation of a CTMC.
 */
public class CTMCSimple extends DTMCSimple implements CTMC
{
	/**
	 * The cached embedded DTMC.
	 * <p>
	 * Will become invalid if the CTMC is changed. In this case
	 * construct a new one by calling buildImplicitEmbeddedDTMC()
	 * <p>
	 * We cache this so that the PredecessorRelation of the
	 * embedded DTMC is cached.
	 */
	private DTMCEmbeddedSimple cachedEmbeddedDTMC = null;

	// Constructors

	/**
	 * Constructor: empty CTMC.
	 */
	public CTMCSimple()
	{
		super();
	}

	/**
	 * Constructor: new CTMC with fixed number of states.
	 */
	public CTMCSimple(int numStates)
	{
		super(numStates);
	}

	/**
	 * Copy constructor.
	 */
	public CTMCSimple(CTMCSimple ctmc)
	{
		super(ctmc);
	}
	
	/**
	 * Construct a CTMC from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 * Note: have to build new Distributions from scratch anyway to do this,
	 * so may as well provide this functionality as a constructor.
	 */
	public CTMCSimple(CTMCSimple ctmc, int permut[])
	{
		super(ctmc, permut);
	}

	// Accessors (for CTMC)
	
	@Override
	public double getExitRate(int i)
	{
		return trans.get(i).sum();
	}
	
	@Override
	public double getMaxExitRate()
	{
		int i;
		double d, max = Double.NEGATIVE_INFINITY;
		for (i = 0; i < numStates; i++) {
			d = trans.get(i).sum();
			if (d > max)
				max = d;
		}
		return max;
	}
	
	@Override
	public double getMaxExitRate(BitSet subset)
	{
		int i;
		double d, max = Double.NEGATIVE_INFINITY;
		for (i = subset.nextSetBit(0); i >= 0; i = subset.nextSetBit(i + 1)) {
			d = trans.get(i).sum();
			if (d > max)
				max = d;
		}
		return max;
	}
	
	@Override
	public double getDefaultUniformisationRate()
	{
		return 1.02 * getMaxExitRate(); 
	}
	
	@Override
	public double getDefaultUniformisationRate(BitSet nonAbs)
	{
		return 1.02 * getMaxExitRate(nonAbs); 
	}
	
	@Override
	public DTMC buildImplicitEmbeddedDTMC()
	{
		DTMCEmbeddedSimple dtmc = new DTMCEmbeddedSimple(this);
		if (cachedEmbeddedDTMC != null) {
			// replace cached DTMC
			cachedEmbeddedDTMC = dtmc;
		}
		return dtmc;
	}
	
	@Override
	public DTMC getImplicitEmbeddedDTMC()
	{
		if (cachedEmbeddedDTMC == null) {
			cachedEmbeddedDTMC = new DTMCEmbeddedSimple(this);
		}
		return cachedEmbeddedDTMC;
	}

	
	@Override
	public DTMCSimple buildEmbeddedDTMC()
	{
		DTMCSimple dtmc;
		Distribution distr;
		int i;
		double d;
		dtmc = new DTMCSimple(numStates);
		for (int in : getInitialStates()) {
			dtmc.addInitialState(in);
		}
		for (i = 0; i < numStates; i++) {
			distr = trans.get(i);
			d = distr.sum();
			if (d == 0) {
				dtmc.setProbability(i, i, 1.0);
			} else {
				for (Map.Entry<Integer, Double> e : distr) {
					dtmc.setProbability(i, e.getKey(), e.getValue() / d);
				}
			}
		}
		return dtmc;
	}

	@Override
	public void uniformise(double q)
	{
		Distribution distr;
		int i;
		for (i = 0; i < numStates; i++) {
			distr = trans.get(i);
			distr.set(i, q - distr.sumAllBut(i));
		}
	}

	@Override
	public DTMC buildImplicitUniformisedDTMC(double q)
	{
		return new DTMCUniformisedSimple(this, q);
	}
	
	@Override
	public DTMCSimple buildUniformisedDTMC(double q)
	{
		DTMCSimple dtmc;
		Distribution distr;
		int i;
		double d;
		dtmc = new DTMCSimple(numStates);
		for (int in : getInitialStates()) {
			dtmc.addInitialState(in);
		}
		for (i = 0; i < numStates; i++) {
			// Add scaled off-diagonal entries
			distr = trans.get(i);
			for (Map.Entry<Integer, Double> e : distr) {
				dtmc.setProbability(i, e.getKey(), e.getValue() / q);
			}
			// Add diagonal, if needed
			d = distr.sumAllBut(i);
			if (d < q) {
				dtmc.setProbability(i, i, 1 - (d / q));
			}
		}
		return dtmc;
	}
}
