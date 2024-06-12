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

import java.util.*;
import java.util.Map.Entry;

import explicit.rewards.MCRewards;
import parser.State;
import parser.Values;
import prism.PrismException;
import prism.PrismNotSupportedException;

/**
 * Simple explicit-state representation of a DTMC, constructed (implicitly) as the embedded DTMC of a CTMC.
 * i.e. P(i,j) = R(i,j) / E(i) if E(i) > 0 and P(i,i) = 1 otherwise
 * where E(i) is the exit rate for state i: sum_j R(i,j).
 * This class is read-only: most of data is pointers to other model info.
 */
public class DTMCEmbeddedSimple<Value> extends DTMCExplicit<Value>
{
	// Parent CTMC
	protected CTMCSimple<Value> ctmc;
	// Exit rates vector
	protected List<Value> exitRates;
	// Number of extra transitions added (just for stats)
	protected int numExtraTransitions;

	/**
	 * Constructor: create from CTMC.
	 */
	public DTMCEmbeddedSimple(CTMCSimple<Value> ctmc)
	{
		// Initialise/copy basic model info
		initialise(ctmc.getNumStates());
		copyFrom(ctmc);
		// Store CTMC info
		this.ctmc = ctmc;
		exitRates = new ArrayList<>(numStates);
		numExtraTransitions = 0;
		for (int i = 0; i < numStates; i++) {
			Value sum = ctmc.getTransitions(i).sum();
			exitRates.add(sum);
			if (getEvaluator().isZero(sum))
				numExtraTransitions++;
		}
	}

	@Override
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		throw new PrismNotSupportedException("Not supported");
	}
	
	// Accessors (for Model)

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

	public int getNumTransitions(int s)
	{
		if (getEvaluator().isZero(exitRates.get(s))) {
			return 1;
		} else {
			return ctmc.getNumTransitions(s);
		}
	}

	@Override
	public SuccessorsIterator getSuccessors(final int s)
	{
		if (getEvaluator().isZero(exitRates.get(s))) {
			return SuccessorsIterator.fromSingleton(s);
		} else {
			return ctmc.getSuccessors(s);
		}
	}

	public int getNumChoices(int s)
	{
		// Always 1 for a DTMC
		return 1;
	}

	@Override
	public BitSet getLabelStates(String name)
	{
		return ctmc.getLabelStates(name);
	}

	@Override
	public boolean hasLabel(String name)
	{
		return ctmc.hasLabel(name);
	}

	@Override
	public Set<String> getLabels()
	{
		return ctmc.getLabels();
	}

	@Override
	public void addLabel(String name, BitSet states)
	{
		throw new RuntimeException("Can not add label to DTMCEmbeddedSimple");
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

	public Iterator<Entry<Integer,Value>> getTransitionsIterator(int s)
	{
		if (getEvaluator().isZero(exitRates.get(s))) {
			// return prob-1 self-loop
			return Collections.singletonMap(s, getEvaluator().one()).entrySet().iterator();
		} else {
			final Iterator<Entry<Integer,Value>> ctmcIterator = ctmc.getTransitionsIterator(s);
			
			// return iterator over entries, with probabilities divided by exitRates[s]
			final Value er = exitRates.get(s);
			return new Iterator<Entry<Integer,Value>>() {
				@Override
				public boolean hasNext()
				{
					return ctmcIterator.hasNext();
				}

				@Override
				public Entry<Integer, Value> next()
				{
					final Entry<Integer, Value> ctmcEntry = ctmcIterator.next();
					
					return new Entry<Integer, Value>() {
						@Override
						public Integer getKey()
						{
							return ctmcEntry.getKey();
						}

						@Override
						public Value getValue()
						{
							return getEvaluator().divide(ctmcEntry.getValue(), er);
						}

						@Override
						public Value setValue(Value value)
						{
							throw new UnsupportedOperationException();
						}
					};
				}
			};
		}
	}

	@Override
	public void forEachTransition(int s, TransitionConsumer<Value> c)
	{
		final Value er = exitRates.get(s);
		if (getEvaluator().isZero(er)) {
			// exit rate = 0 -> prob 1 self loop
			c.accept(s, s, getEvaluator().one());
		} else {
			ctmc.forEachTransition(s, (s_,t,rate) -> {
				c.accept(s_, t, getEvaluator().divide(rate, er));
			});
		}
	}

	@Override
	public void forEachDoubleTransition(int s, DoubleTransitionConsumer c)
	{
		final double er = getEvaluator().toDouble(exitRates.get(s));
		if (er == 0) {
			// exit rate = 0 -> prob 1 self loop
			c.accept(s, s, 1.0);
		} else {
			ctmc.forEachDoubleTransition(s, (s_,t,rate) -> {
				c.accept(s_, t, rate / er);
			});
		}
	}

	public double mvMultSingle(int s, double vect[])
	{
		Distribution<Value> distr = ctmc.getTransitions(s);
		double d = 0.0;
		double er = getEvaluator().toDouble(exitRates.get(s));
		// Exit rate 0: prob 1 self-loop
		if (er == 0) {
			d += vect[s];
		}
		// Exit rate > 0
		else {
			for (Map.Entry<Integer, Value> e : distr) {
				int k = e.getKey();
				double prob = getEvaluator().toDouble(e.getValue());
				d += prob * vect[k];
			}
			d /= er;
		}

		return d;
	}

	@Override
	public double mvMultJacSingle(int s, double vect[])
	{
		Distribution<Value> distr = ctmc.getTransitions(s);
		double diag = 0.0, d = 0.0;
		double er = getEvaluator().toDouble(exitRates.get(s));
		// Exit rate 0: prob 1 self-loop
		if (er == 0) {
			return 0.0;
		}
		// Exit rate > 0
		else {
			// (sum_{j!=s} P(s,j)*vect[j]) / (1-P(s,s))
			// = (sum_{j!=s} (R(s,j)/E(s))*vect[j]) / (1-(P(s,s)/E(s)))
			// = (sum_{j!=s} R(s,j)*vect[j]) / (E(s)-P(s,s))
			for (Map.Entry<Integer, Value> e : distr) {
				int k = e.getKey();
				double prob = getEvaluator().toDouble(e.getValue());
				// Non-diagonal entries only
				if (k != s) {
					d += prob * vect[k];
				} else {
					diag = prob;
				}
			}
			d /= (er - diag);
		}
		
		return d;
	}

	public double mvMultRewSingle(int s, double vect[], MCRewards<Double> mcRewards)
	{
		Distribution<Value> distr = ctmc.getTransitions(s);
		double er = getEvaluator().toDouble(exitRates.get(s));
		double d = 0;
		// Exit rate 0: prob 1 self-loop
		if (er == 0) {
			d += vect[s];
		}
		// Exit rate > 0
		else {
			for (Map.Entry<Integer, Value> e : distr) {
				int k = e.getKey();
				double prob = getEvaluator().toDouble(e.getValue());
				d += prob * vect[k];
			}
			d /= er;
		}
		d += mcRewards.getStateReward(s);

		return d;
	}

	//@Override
	public double mvMultRewJacSingle(int s, double vect[], MCRewards<Double> mcRewards)
	{
		Distribution<Value> distr = ctmc.getTransitions(s);
		double diag = 0.0, d = 0.0;
		double er = getEvaluator().toDouble(exitRates.get(s));
		// Exit rate 0: prob 1 self-loop
		if (er == 0) {
			return mcRewards.getStateReward(s);
		}
		// Exit rate > 0
		else {
			// (rew(s) + sum_{j!=s} P(s,j)*vect[j]) / (1-P(s,s))
			// = (rew(s) + sum_{j!=s} (R(s,j)/E(s))*vect[j]) / (1-(P(s,s)/E(s)))
			// = (E(s)*rew(s) + sum_{j!=s} R(s,j)*vect[j]) / (E(s)-P(s,s))
			d = er * mcRewards.getStateReward(s);
			for (Map.Entry<Integer, Value> e : distr) {
				int k = e.getKey();
				double prob = getEvaluator().toDouble(e.getValue());
				// Non-diagonal entries only
				if (k != s) {
					d += prob * vect[k];
				} else {
					diag = prob;
				}
			}
			d /= (er - diag);
		}
		
		return d;
	}

	@Override
	public void vmMult(double vect[], double result[])
	{
		// Initialise result to 0
		Arrays.fill(result, 0);
		// Go through matrix elements (by row)
		for (int state = 0; state < numStates; state++) {
			double er = getEvaluator().toDouble(exitRates.get(state));
			// Exit rate 0: prob 1 self-loop
			if (er == 0) {
				result[state] += vect[state];
				continue;
			}
			// Exit rate > 0
			for (Iterator<Entry<Integer, Value>> transitions = ctmc.getTransitionsIterator(state); transitions.hasNext();) {
				Entry<Integer, Value> trans = transitions.next();
				int target  = trans.getKey();
				double prob = getEvaluator().toDouble(trans.getValue()) / er;
				result[target] += prob * vect[state];
			}
		}
	}

	@Override
	public String toString()
	{
		int i, numStates;
		boolean first;
		String s = "";
		s += "ctmc: " + ctmc;
		first = true;
		s = ", exitRates: [ ";
		numStates = getNumStates();
		for (i = 0; i < numStates; i++) {
			if (first)
				first = false;
			else
				s += ", ";
			s += i + ": " + exitRates.get(i);
		}
		s += " ]";
		return s;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof DTMCEmbeddedSimple))
			return false;
		DTMCEmbeddedSimple<?> dtmc = (DTMCEmbeddedSimple<?>) o;
		if (!ctmc.equals(dtmc.ctmc))
			return false;
		if (!exitRates.equals(dtmc.exitRates))
			return false;
		if (numExtraTransitions != dtmc.numExtraTransitions)
			return false;
		return true;
	}
}
