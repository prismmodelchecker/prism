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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import parser.State;
import parser.Values;
import prism.ModelType;
import prism.PrismException;
import explicit.rewards.MCRewards;

/**
* Simple explicit-state representation of a DTMC, constructed (implicitly) as the uniformised DTMC of a CTMC.
* This class is read-only: most of data is pointers to other model info.
*/
public class DTMCUniformisedSimple extends DTMCExplicit
{
	// Parent CTMC
	protected CTMCSimple ctmc;
	// Uniformisation rate
	protected double q;
	// Number of extra transitions added (just for stats)
	protected int numExtraTransitions;

	/**
	 * Constructor: create from CTMC and uniformisation rate q.
	 */
	public DTMCUniformisedSimple(CTMCSimple ctmc, double q)
	{
		this.ctmc = ctmc;
		this.numStates = ctmc.getNumStates();
		this.q = q;
		numExtraTransitions = 0;
		for (int i = 0; i < numStates; i++) {
			if (ctmc.getTransitions(i).get(i) == 0 && ctmc.getTransitions(i).sumAllBut(i) < q) {
				numExtraTransitions++;
			}
		}
	}

	/**
	 * Constructor: create from CTMC and its default uniformisation rate.
	 */
	public DTMCUniformisedSimple(CTMCSimple ctmc)
	{
		this(ctmc, ctmc.getDefaultUniformisationRate());
	}

	@Override
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		throw new PrismException("Not supported");
	}
	
	// Accessors (for Model)

	public ModelType getModelType()
	{
		return ModelType.DTMC;
	}

	public int getNumStates()
	{
		return ctmc.getNumStates();
	}

	public int getNumInitialStates()
	{
		return ctmc.getNumInitialStates();
	}

	public Iterable<Integer> getInitialStates()
	{
		return ctmc.getInitialStates();
	}

	public int getFirstInitialState()
	{
		return ctmc.getFirstInitialState();
	}

	public boolean isInitialState(int i)
	{
		return ctmc.isInitialState(i);
	}

	public boolean isDeadlockState(int i)
	{
		return ctmc.isDeadlockState(i);
	}

	public List<State> getStatesList()
	{
		return ctmc.getStatesList();
	}
	
	public Values getConstantValues()
	{
		return ctmc.getConstantValues();
	}
	
	public int getNumTransitions()
	{
		return ctmc.getNumTransitions() + numExtraTransitions;
	}

	public Iterator<Integer> getSuccessorsIterator(final int s)
	{
		// TODO
		throw new Error("Not yet supported");
	}
	
	public boolean isSuccessor(int s1, int s2)
	{
		// TODO
		throw new Error("Not yet supported");
	}

	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		// TODO
		throw new Error("Not yet supported");
	}

	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		// TODO
		throw new Error("Not yet supported");
	}

	public int getNumChoices(int s)
	{
		// Always 1 for a DTMC
		return 1;
	}

	public void findDeadlocks(boolean fix) throws PrismException
	{
		// No deadlocks by definition
	}

	public void checkForDeadlocks() throws PrismException
	{
		// No deadlocks by definition
	}

	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		// No deadlocks by definition
	}

	@Override
	public String infoString()
	{
		String s = "";
		s += numStates + " states (" + getNumInitialStates() + " initial)";
		s += ", " + getNumTransitions() + " transitions (incl. " + numExtraTransitions + " self-loops)";
		return s;
	}

	@Override
	public String infoStringTable()
	{
		String s = "";
		s += "States:      " + numStates + " (" + getNumInitialStates() + " initial)\n";
		s += "Transitions: " + getNumTransitions() + "\n";
		return s;
	}

	// Accessors (for DTMC)

	public int getNumTransitions(int s)
	{
		// TODO
		throw new RuntimeException("Not implemented yet");
	}

	public Iterator<Entry<Integer,Double>> getTransitionsIterator(int s)
	{
		// TODO
		throw new RuntimeException("Not implemented yet");
	}

	public void prob0step(BitSet subset, BitSet u, BitSet result)
	{
		// TODO
		throw new Error("Not yet supported");
	}

	public void prob1step(BitSet subset, BitSet u, BitSet v, BitSet result)
	{
		// TODO
		throw new Error("Not yet supported");
	}

	@Override
	public double mvMultSingle(int s, double vect[])
	{
		int k;
		double sum, d, prob;
		Distribution distr;

		distr = ctmc.getTransitions(s);
		sum = d = 0.0;
		for (Map.Entry<Integer, Double> e : distr) {
			k = (Integer) e.getKey();
			prob = (Double) e.getValue();
			// Non-diagonal entries
			if (k != s) {
				sum += prob;
				d += (prob / q) * vect[k];
			}
		}
		// Diagonal entry
		if (sum < q) {
			d += (1 - sum/q) * vect[s];
		}

		return d;
	}

	@Override
	public double mvMultJacSingle(int s, double vect[])
	{
		int k;
		double sum, d, prob;
		Distribution distr;

		distr = ctmc.getTransitions(s);
		sum = d = 0.0;
		for (Map.Entry<Integer, Double> e : distr) {
			k = (Integer) e.getKey();
			prob = (Double) e.getValue();
			// Non-diagonal entries only
			if (k != s) {
				sum += prob;
				d += (prob / q) * vect[k];
			}
		}
		// Diagonal entry is 1 - sum/q
		d /= (sum / q);

		return d;
	}

	public double mvMultRewSingle(int s, double vect[], MCRewards mcRewards)
	{
		// TODO
		throw new Error("Not yet supported");
	}

	@Override
	public void vmMult(double vect[], double result[])
	{
		int i, j;
		double prob, sum;
		Distribution distr;
		
		// Initialise result to 0
		for (j = 0; j < numStates; j++) {
			result[j] = 0;
		}
		// Go through matrix elements (by row)
		for (i = 0; i < numStates; i++) {
			distr = ctmc.getTransitions(i);
			sum = 0.0;
			for (Map.Entry<Integer, Double> e : distr) {
				j = (Integer) e.getKey();
				prob = (Double) e.getValue();
				// Non-diagonal entries only
				if (j != i) {
					sum += prob;
					result[j] += (prob / q) * vect[i];
				}
			}
			// Diagonal entry is 1 - sum/q
			result[i] += (1 - sum / q) * vect[i];
		}
	}

	@Override
	public String toString()
	{
		String s = "";
		s += "ctmc: " + ctmc;
		s = ", q: " + q;
		return s;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof DTMCUniformisedSimple))
			return false;
		DTMCUniformisedSimple dtmc = (DTMCUniformisedSimple) o;
		if (!ctmc.equals(dtmc.ctmc))
			return false;
		if (q != dtmc.q)
			return false;
		if (numExtraTransitions != dtmc.numExtraTransitions)
			return false;
		return true;
	}
}
