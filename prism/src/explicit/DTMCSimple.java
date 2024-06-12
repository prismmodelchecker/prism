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
import java.io.*;
import java.util.function.Function;

import prism.Evaluator;
import prism.PrismException;

/**
 * Simple explicit-state representation of a DTMC.
 */
public class DTMCSimple<Value> extends DTMCExplicit<Value> implements ModelSimple<Value>
{
	// Transition matrix (distribution list) 
	protected List<Distribution<Value>> trans;

	// Other statistics
	protected int numTransitions;

	// Constructors

	/**
	 * Constructor: empty DTMC.
	 */
	public DTMCSimple()
	{
		initialise(0);
	}

	/**
	 * Constructor: new DTMC with fixed number of states.
	 */
	public DTMCSimple(int numStates)
	{
		initialise(numStates);
	}

	/**
	 * Copy constructor.
	 */
	public DTMCSimple(DTMCSimple<Value> dtmc)
	{
		this(dtmc.numStates);
		copyFrom(dtmc);
		for (int i = 0; i < numStates; i++) {
			trans.set(i, new Distribution<Value>(dtmc.trans.get(i)));
		}
		numTransitions = dtmc.numTransitions;
	}

	/**
	 * Construct a DTMC from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 * Pointer to states list is NOT copied (since now wrong).
	 * Note: have to build new Distributions from scratch anyway to do this,
	 * so may as well provide this functionality as a constructor.
	 */
	public DTMCSimple(DTMCSimple<Value> dtmc, int permut[])
	{
		this(dtmc.numStates);
		copyFrom(dtmc, permut);
		for (int i = 0; i < numStates; i++) {
			trans.set(permut[i], new Distribution<Value>(dtmc.trans.get(i), permut));
		}
		numTransitions = dtmc.numTransitions;
	}

	/**
	 * Construct a DTMCSimple object from a DTMC object.
	 */
	public DTMCSimple(DTMC<Value> dtmc)
	{
		this(dtmc, p -> p);
	}

	/**
	 * Construct a DTMCSimple object from a DTMC object,
	 * mapping probability values using the provided function.
	 * There is no attempt to check that distributions sum to one.
	 */
	public DTMCSimple(DTMC<Value> dtmc, Function<? super Value, ? extends Value> probMap)
	{
		this(dtmc, probMap, dtmc.getEvaluator());
	}

	/**
	 * Construct a DTMCSimple object from a DTMC object,
	 * mapping probability values using the provided function.
	 * There is no attempt to check that distributions sum to one.
	 * Since the type changes (T -> Value), an Evaluator for Value must be given.
	 */
	public <T> DTMCSimple(DTMC<T> dtmc, Function<? super T, ? extends Value> probMap, Evaluator<Value> eval)
	{
		this(dtmc.getNumStates());
		copyFrom(dtmc);
		setEvaluator(eval);
		int numStates = getNumStates();
		for (int i = 0; i < numStates; i++) {
			Iterator<Map.Entry<Integer, T>> iter = dtmc.getTransitionsIterator(i);
			while (iter.hasNext()) {
				Map.Entry<Integer, T> e = iter.next();
				addToProbability(i, e.getKey(), probMap.apply(e.getValue()));
			}
		}
	}

	// Mutators (for ModelSimple)

	@Override
	public void initialise(int numStates)
	{
		super.initialise(numStates);
		trans = new ArrayList<Distribution<Value>>(numStates);
		for (int i = 0; i < numStates; i++) {
			trans.add(new Distribution<Value>(getEvaluator()));
		}
	}

	@Override
	public void clearState(int i)
	{
		// Do nothing if state does not exist
		if (i >= numStates || i < 0)
			return;
		// Clear data structures and update stats
		numTransitions -= trans.get(i).size();
		trans.get(i).clear();
	}

	@Override
	public int addState()
	{
		addStates(1);
		return numStates - 1;
	}

	@Override
	public void addStates(int numToAdd)
	{
		for (int i = 0; i < numToAdd; i++) {
			trans.add(new Distribution<Value>(getEvaluator()));
			numStates++;
		}
	}

	@Override
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		String s, ss[];
		int i, j, n, lineNum = 0;
		Value prob;

		// Open file for reading, automatic close when done
		try (BufferedReader in = new BufferedReader(new FileReader(new File(filename)))) {
			// Parse first line to get num states
			s = in.readLine();
			lineNum = 1;
			if (s == null) {
				throw new PrismException("Missing first line of .tra file");
			}
			ss = s.split(" ");
			n = Integer.parseInt(ss[0]);
			// Initialise
			initialise(n);
			// Go though list of transitions in file
			s = in.readLine();
			lineNum++;
			while (s != null) {
				s = s.trim();
				if (s.length() > 0) {
					ss = s.split(" ");
					i = Integer.parseInt(ss[0]);
					j = Integer.parseInt(ss[1]);
					prob = getEvaluator().fromString(ss[2]);
					setProbability(i, j, prob);
				}
				s = in.readLine();
				lineNum++;
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + filename + "\": " + e.getMessage());
		} catch (NumberFormatException e) {
			throw new PrismException("Problem in .tra file (line " + lineNum + ") for " + getModelType());
		}
	}

	// Mutators (other)

	/**
	 * Set the probability for a transition. 
	 */
	public void setProbability(int i, int j, Value prob)
	{
		Distribution<Value> distr = trans.get(i);
		if (!getEvaluator().isZero(distr.get(j)))
			numTransitions--;
		if (!getEvaluator().isZero(prob))
			numTransitions++;
		distr.set(j, prob);
	}

	/**
	 * Add to the probability for a transition. 
	 */
	public void addToProbability(int i, int j, Value prob)
	{
		if (!trans.get(i).add(j, prob)) {
			if (!getEvaluator().isZero(prob))
				numTransitions++;
		}
	}

	// Accessors (for Model)

	@Override
	public int getNumTransitions()
	{
		return numTransitions;
	}

	@Override
	public int getNumTransitions(int s)
	{
		return trans.get(s).size();
	}

	/** Get an iterator over the successors of state s */
	@Override
	public Iterator<Integer> getSuccessorsIterator(final int s)
	{
		return trans.get(s).getSupport().iterator();
	}

	@Override
	public SuccessorsIterator getSuccessors(int s)
	{
		return SuccessorsIterator.from(getSuccessorsIterator(s), true);
	}

	@Override
	public boolean isSuccessor(int s1, int s2)
	{
		return trans.get(s1).contains(s2);
	}

	@Override
	public boolean allSuccessorsInSet(int s, BitSet set)
	{
		return (trans.get(s).isSubsetOf(set));
	}

	@Override
	public boolean someSuccessorsInSet(int s, BitSet set)
	{
		return (trans.get(s).containsOneOf(set));
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (trans.get(i).isEmpty()) {
				addDeadlockState(i);
				if (fix)
					setProbability(i, i, getEvaluator().one());
			}
		}
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (trans.get(i).isEmpty() && (except == null || !except.get(i)))
				throw new PrismException("DTMC has a deadlock in state " + i);
		}
	}

	// Accessors (for DTMC)

	@Override
	public Iterator<Entry<Integer, Value>> getTransitionsIterator(int s)
	{
		return trans.get(s).iterator();
	}

	// Accessors (other)

	/**
	 * Get the transitions (a distribution) for state s.
	 */
	public Distribution<Value> getTransitions(int s)
	{
		return trans.get(s);
	}

	// Standard methods

	@Override
	public String toString()
	{
		int i;
		boolean first;
		String s = "";
		first = true;
		s = "trans: [ ";
		for (i = 0; i < numStates; i++) {
			if (first)
				first = false;
			else
				s += ", ";
			s += i + ": " + trans.get(i);
		}
		s += " ]";
		return s;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof DTMCSimple))
			return false;
		if (!super.equals(o))
			return false;
		DTMCSimple<?> dtmc = (DTMCSimple<?>) o;
		if (!trans.equals(dtmc.trans))
			return false;
		if (numTransitions != dtmc.numTransitions)
			return false;
		return true;
	}
}
