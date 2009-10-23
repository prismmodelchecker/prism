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

import prism.ModelType;

/**
 * Explicit representation of continuous-time Markov chain (CTMC).
 */
public class CTMC extends DTMC
{
	// Model type
	public static ModelType modelType = ModelType.CTMC;

	// Uniformisation rate used to build CTMC/CTMDP
	public double unif;

	/**
	 * Build the embedded DTMC for this CTMC
	 */
	public DTMC buildEmbeddedDTMC()
	{
		DTMC dtmc;
		Distribution distr;
		int i;
		double d;
		dtmc = new DTMC(numStates);
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

	/**
	 * Uniformise.
	 * @param unif: Unifomisation rate
	 */
	public void uniformise(double unif)
	{
		Distribution distr;
		int i;
		for (i = 0; i < numStates; i++) {
			distr = trans.get(i);
			distr.set(i, unif - distr.sumAllBut(i));
		}
		this.unif = unif;
	}

	/**
	 * Uniformise with an appropriate rate.
	 */
	public void uniformise()
	{
		uniformise(1.02 * maxExitRate());
	}

	/**
	 * Compute the maximum exit rate.
	 */
	public double maxExitRate()
	{
		int i;
		double d, max = Double.NEGATIVE_INFINITY;
		for (i = 0; i < numStates; i++) {
			d = trans.get(i).sumAllBut(i);
			if (d > max)
				max = d;
		}
		return max;
	}
}
