//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Christian von Essen <christian.vonessen@imag.fr> (Verimag, Grenoble)
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import prism.PrismException;


/**
 * Simple explicit-state representation of an MDP.
 * The implementation is far from optimal, both in terms of memory usage and speed of access.
 * The model is, however, easy to manipulate. For a static model (i.e. one that does not change
 * after creation), consider MDPSparse, which is more efficient. 
 */
public class MDPSimple extends MDPExplicit implements NondetModelSimple
{
	// Transition function (Steps)
	protected List<List<Distribution>> trans;

	// Action labels
	// (null list means no actions; null in element s means no actions for state s)
	protected List<List<Object>> actions;

	// Flag: allow duplicates in distribution sets?
	protected boolean allowDupes = false;

	// Other statistics
	protected int numDistrs;
	protected int numTransitions;
	protected int maxNumDistrs;
	protected boolean maxNumDistrsOk;

	// Constructors

	/**
	 * Constructor: empty MDP.
	 */
	public MDPSimple()
	{
		initialise(0);
	}

	/**
	 * Constructor: new MDP with fixed number of states.
	 */
	public MDPSimple(int numStates)
	{
		initialise(numStates);
	}

	/**
	 * Copy constructor.
	 */
	public MDPSimple(MDPSimple mdp)
	{
		this(mdp.numStates);
		copyFrom(mdp);
		// Copy storage directly to avoid worrying about duplicate distributions (and for efficiency) 
		for (int s = 0; s < numStates; s++) {
			List<Distribution> distrs = trans.get(s);
			for (Distribution distr : mdp.trans.get(s)) {
				distrs.add(new Distribution(distr));
			}
		}
		if (mdp.actions != null) {
			actions = new ArrayList<List<Object>>(numStates);
			for (int s = 0; s < numStates; s++)
				actions.add(null);
			for (int s = 0; s < numStates; s++) {
				if (mdp.actions.get(s) != null) {
					int n = mdp.trans.get(s).size();
					List<Object> list = new ArrayList<Object>(n);
					for (int i = 0; i < n; i++) {
						list.add(mdp.actions.get(s).get(i));
					}
					actions.set(s, list);
				}
			}
		}
		// Copy flags/stats too
		allowDupes = mdp.allowDupes;
		numDistrs = mdp.numDistrs;
		numTransitions = mdp.numTransitions;
		maxNumDistrs = mdp.maxNumDistrs;
		maxNumDistrsOk = mdp.maxNumDistrsOk;
	}

	/**
	 * Constructor: new MDP copied from an existing DTMC.
	 */
	public MDPSimple(DTMCSimple dtmc)
	{
		this(dtmc.getNumStates());
		copyFrom(dtmc);
		for (int s = 0; s < numStates; s++) {
			// Note: DTMCSimple has no actions so can ignore these
			addChoice(s, new Distribution(dtmc.getTransitions(s)));
		}
	}

	/**
	 * Construct an MDP from an existing one and a state index permutation,
	 * i.e. in which state index i becomes index permut[i].
	 * Note: have to build new Distributions from scratch anyway to do this,
	 * so may as well provide this functionality as a constructor.
	 */
	public MDPSimple(MDPSimple mdp, int permut[])
	{
		this(mdp.numStates);
		copyFrom(mdp, permut);
		// Copy storage directly to avoid worrying about duplicate distributions (and for efficiency)
		// (Since permut is a bijection, all structures and statistics are identical)
		for (int s = 0; s < numStates; s++) {
			List<Distribution> distrs = trans.get(permut[s]);
			for (Distribution distr : mdp.trans.get(s)) {
				distrs.add(new Distribution(distr, permut));
			}
		}
		if (mdp.actions != null) {
			actions = new ArrayList<List<Object>>(numStates);
			for (int s = 0; s < numStates; s++)
				actions.add(null);
			for (int s = 0; s < numStates; s++) {
				if (mdp.actions.get(s) != null) {
					int n = mdp.trans.get(s).size();
					List<Object> list = new ArrayList<Object>(n);
					for (int i = 0; i < n; i++) {
						list.add(mdp.actions.get(s).get(i));
					}
					actions.set(permut[s], list);
				}
			}
		}
		// Copy flags/stats too
		allowDupes = mdp.allowDupes;
		numDistrs = mdp.numDistrs;
		numTransitions = mdp.numTransitions;
		maxNumDistrs = mdp.maxNumDistrs;
		maxNumDistrsOk = mdp.maxNumDistrsOk;
	}

	/**
	 * Construct an MDPSimple object from an MDPSparse one.
	 */
	public MDPSimple(MDPSparse mdp)
	{
		this(mdp.numStates);
		copyFrom(mdp);
		// Copy storage directly to avoid worrying about duplicate distributions (and for efficiency)
		for (int s = 0; s < numStates; s++) {
			for (int c = 0; c < mdp.getNumChoices(s); c++) {
				Distribution distr = new Distribution();
				Iterator<Entry<Integer, Double>> it = mdp.getTransitionsIterator(s, c);
				while (it.hasNext()) {
					Entry<Integer, Double> entry = it.next();
					distr.add(entry.getKey(), entry.getValue());
				}
				this.addChoice(s, distr);
			}
		}

		if (mdp.actions != null) {
			actions = new ArrayList<List<Object>>(numStates);
			for (int s = 0; s < numStates; s++)
				actions.add(null);
			for (int s = 0; s < numStates; s++) {
				int n = mdp.getNumChoices(s);
				List<Object> list = new ArrayList<Object>(n);
				for (int i = 0; i < n; i++) {
					list.add(mdp.getAction(s, i));
				}
				actions.set(s, list);
			}
		}
		// Copy flags/stats too
		allowDupes = false; // TODO check this
		numDistrs = mdp.numDistrs;
		numTransitions = mdp.numTransitions;
		maxNumDistrs = mdp.maxNumDistrs;
		maxNumDistrsOk = true; // TODO not sure
	}

	// Mutators (for ModelSimple)

	@Override
	public void initialise(int numStates)
	{
		super.initialise(numStates);
		numDistrs = numTransitions = maxNumDistrs = 0;
		maxNumDistrsOk = true;
		trans = new ArrayList<List<Distribution>>(numStates);
		for (int i = 0; i < numStates; i++) {
			trans.add(new ArrayList<Distribution>());
		}
		actions = null;
	}

	@Override
	public void clearState(int s)
	{
		// Do nothing if state does not exist
		if (s >= numStates || s < 0)
			return;
		// Clear data structures and update stats
		List<Distribution> list = trans.get(s);
		numDistrs -= list.size();
		for (Distribution distr : list) {
			numTransitions -= distr.size();
		}
		maxNumDistrsOk = false;
		trans.get(s).clear();
		if (actions != null && actions.get(s) != null)
			actions.get(s).clear();
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
			trans.add(new ArrayList<Distribution>());
			if (actions != null)
				actions.add(null);
			numStates++;
		}
	}

	@Override
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		// we want to accurately store the model as it appears
		// in the file, so we allow dupes
		allowDupes = true;

		int lineNum = 0;
		// Open file for reading, automatic close
		try (BufferedReader in = new BufferedReader(new FileReader(new File(filename)))) {
			// Parse first line to get num states
			String info = in.readLine();
			lineNum = 1;
			if (info == null) {
				throw new PrismException("Missing first line of .tra file");
			}
			String[] infos = info.split(" ");
			if (infos.length < 3) {
				throw new PrismException("First line of .tra file must read #states, #choices, #transitions");
			}
			int n = Integer.parseInt(infos[0]);
			int expectedNumChoices = Integer.parseInt(infos[1]);
			int expectedNumTransitions = Integer.parseInt(infos[2]);

			int emptyDistributions = 0;
			
			// Initialise
			initialise(n);
			// Go though list of transitions in file
			String s = in.readLine();
			lineNum++;
			while (s != null) {
				s = s.trim();
				if (s.length() > 0) {
					String[] transition = s.split(" ");
					int source = Integer.parseInt(transition[0]);
					int choice = Integer.parseInt(transition[1]);
					int target = Integer.parseInt(transition[2]);
					double prob = Double.parseDouble(transition[3]);

					if (source < 0 || source >= numStates) {
						throw new PrismException("Problem in .tra file (line " + lineNum + "): illegal source state index " + source);
					}
					if (target < 0 || target >= numStates) {
						throw new PrismException("Problem in .tra file (line " + lineNum + "): illegal target state index " + target);
					}

					// ensure distributions for all choices up to choice (inclusive) exist
					// this potentially creates empty distributions that are never defined
					// so we keep track of the number of distributions that are still empty
					// and provide an error message if there are still empty distributions
					// after having read the full .tra file
					while (choice >= getNumChoices(source)) {
						addChoice(source, new Distribution());
						emptyDistributions++;
					}

					if (trans.get(source).get(choice).isEmpty()) {
						// was empty distribution, becomes non-empty below
						emptyDistributions--;
					}
					// add transition
					if (! trans.get(source).get(choice).add(target, prob)) {
						numTransitions++;
					} else {
						throw new PrismException("Problem in .tra file (line " + lineNum + "): redefinition of probability for " + source + " " + choice + " " + target);
					}

					// add action
					if (transition.length > 4) {
						String action = transition[4];
						Object oldAction = getAction(source, choice);
						if (oldAction != null && !action.equals(oldAction)) {
							throw new PrismException("Problem in .tra file (line " + lineNum + "):"
							                       + "inconsistent action label for " + source + ", " + choice + ": "
									               + oldAction + " and " + action);
						}
						setAction(source, choice, action);
					}
				}
				s = in.readLine();
				lineNum++;
			}
			// check integrity
			if (getNumChoices() != expectedNumChoices) {
				throw new PrismException("Problem in .tra file: unexpected number of choices: " + getNumChoices());
			}
			if (getNumTransitions() != expectedNumTransitions) {
				throw new PrismException("Problem in .tra file: unexpected number of transitions: " + getNumTransitions());
			}
			assert(emptyDistributions >= 0);
			if (emptyDistributions > 0) {
				throw new PrismException("Problem in .tra file: there are " + emptyDistributions + " empty distribution, are there gaps in the choice indices?");
			}
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + filename + "\": " + e.getMessage());
		} catch (NumberFormatException e) {
			throw new PrismException("Problem in .tra file (line " + lineNum + ") for " + getModelType());
		}
	}

	// Mutators (other)

	/**
	 * Add a choice (distribution {@code distr}) to state {@code s} (which must exist).
	 * Distribution is only actually added if it does not already exists for state {@code s}.
	 * (Assuming {@code allowDupes} flag is not enabled.)
	 * Returns the index of the (existing or newly added) distribution.
	 * Returns -1 in case of error.
	 */
	public int addChoice(int s, Distribution distr)
	{
		List<Distribution> set;
		// Check state exists
		if (s >= numStates || s < 0)
			return -1;
		// Add distribution (if new)
		if (!allowDupes) {
			int i = indexOfChoice(s, distr);
			if (i != -1)
				return i;
		}
		set = trans.get(s);
		set.add(distr);
		// Add null action if necessary
		if (actions != null && actions.get(s) != null)
			actions.get(s).add(null);
		// Update stats
		numDistrs++;
		maxNumDistrs = Math.max(maxNumDistrs, set.size());
		numTransitions += distr.size();
		return set.size() - 1;
	}

	/**
	 * Add a choice (distribution {@code distr}) labelled with {@code action} to state {@code s} (which must exist).
	 * Action/distribution is only actually added if it does not already exists for state {@code s}.
	 * (Assuming {@code allowDupes} flag is not enabled.)
	 * Returns the index of the (existing or newly added) distribution.
	 * Returns -1 in case of error.
	 */
	public int addActionLabelledChoice(int s, Distribution distr, Object action)
	{
		List<Distribution> set;
		// Check state exists
		if (s >= numStates || s < 0)
			return -1;
		// Add distribution/action (if new)
		if (!allowDupes) {
			int i = indexOfActionLabelledChoice(s, distr, action);
			if (i != -1)
				return i;
		}
		set = trans.get(s);
		set.add(distr);
		// Add null action if necessary
		if (actions != null && actions.get(s) != null)
			actions.get(s).add(null);
		// Set action
		setAction(s, set.size() - 1, action);
		// Update stats
		numDistrs++;
		maxNumDistrs = Math.max(maxNumDistrs, set.size());
		numTransitions += distr.size();
		return set.size() - 1;
	}

	/**
	 * Set the action label for choice i in some state s.
	 * This method does not know about duplicates (i.e. if setting an action causes
	 * two choices to be identical, one will not be removed).
	 * Use {@link #addActionLabelledChoice(int, Distribution, Object)} which is more reliable.
	 */
	public void setAction(int s, int i, Object o)
	{
		// If action to be set is null, nothing to do
		if (o == null)
			return;
		// If no actions array created yet, create it
		if (actions == null) {
			actions = new ArrayList<List<Object>>(numStates);
			for (int j = 0; j < numStates; j++)
				actions.add(null);
		}
		// If no actions for state i yet, create list
		if (actions.get(s) == null) {
			int n = trans.get(s).size();
			List<Object> list = new ArrayList<Object>(n);
			for (int j = 0; j < n; j++) {
				list.add(null);
			}
			actions.set(s, list);
		}
		// Set actions
		actions.get(s).set(i, o);
	}

	// Accessors (for Model)

	@Override
	public int getNumTransitions()
	{
		return numTransitions;
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			// Note that no distributions is a deadlock, not an empty distribution
			if (trans.get(i).isEmpty()) {
				addDeadlockState(i);
				if (fix) {
					Distribution distr = new Distribution();
					distr.add(i, 1.0);
					addChoice(i, distr);
				}
			}
		}
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		for (int i = 0; i < numStates; i++) {
			if (trans.get(i).isEmpty() && (except == null || !except.get(i)))
				throw new PrismException("MDP has a deadlock in state " + i);
		}
	}

	// Accessors (for NondetModel)

	@Override
	public int getNumChoices(int s)
	{
		return trans.get(s).size();
	}

	@Override
	public int getMaxNumChoices()
	{
		// Recompute if necessary
		if (!maxNumDistrsOk) {
			maxNumDistrs = 0;
			for (int s = 0; s < numStates; s++)
				maxNumDistrs = Math.max(maxNumDistrs, getNumChoices(s));
		}
		return maxNumDistrs;
	}

	@Override
	public int getNumChoices()
	{
		return numDistrs;
	}

	@Override
	public Object getAction(int s, int i)
	{
		List<Object> list;
		if (i < 0 || actions == null || (list = actions.get(s)) == null)
			return null;
		return list.get(i);
	}

	@Override
	public boolean allSuccessorsInSet(int s, int i, BitSet set)
	{
		return trans.get(s).get(i).isSubsetOf(set);
	}

	@Override
	public boolean someSuccessorsInSet(int s, int i, BitSet set)
	{
		return trans.get(s).get(i).containsOneOf(set);
	}

	@Override
	public Iterator<Integer> getSuccessorsIterator(final int s, final int i)
	{
		return trans.get(s).get(i).getSupport().iterator();
	}

	@Override
	public SuccessorsIterator getSuccessors(final int s, final int i)
	{
		return SuccessorsIterator.from(getSuccessorsIterator(s, i), true);
	}

	// Accessors (for MDP)

	@Override
	public int getNumTransitions(int s, int i)
	{
		return trans.get(s).get(i).size();
	}

	@Override
	public Iterator<Entry<Integer, Double>> getTransitionsIterator(int s, int i)
	{
		return trans.get(s).get(i).iterator();
	}

	

	// Accessors (other)

	/**
	 * Get the list of choices (distributions) for state s.
	 */
	public List<Distribution> getChoices(int s)
	{
		return trans.get(s);
	}

	/**
	 * Get the ith choice (distribution) for state s.
	 */
	public Distribution getChoice(int s, int i)
	{
		return trans.get(s).get(i);
	}

	/**
	 * Returns the index of the choice {@code distr} for state {@code s}, if it exists.
	 * If none, -1 is returned. If there are multiple (i.e. allowDupes is true), the first is returned. 
	 */
	public int indexOfChoice(int s, Distribution distr)
	{
		return trans.get(s).indexOf(distr);
	}

	/**
	 * Returns the index of the {@code action}-labelled choice {@code distr} for state {@code s}, if it exists.
	 * If none, -1 is returned. If there are multiple (i.e. allowDupes is true), the first is returned. 
	 */
	public int indexOfActionLabelledChoice(int s, Distribution distr, Object action)
	{
		List<Distribution> set = trans.get(s);
		int i, n = set.size();
		if (distr == null) {
			for (i = 0; i < n; i++) {
				if (set.get(i) == null) {
					Object a = getAction(s, i);
					if (action == null) {
						if (a == null)
							return i;
					} else {
						if (action.equals(a))
							return i;
					}
				}
			}
		} else {
			for (i = 0; i < n; i++) {
				if (distr.equals(set.get(i))) {
					Object a = getAction(s, i);
					if (action == null) {
						if (a == null)
							return i;
					} else {
						if (action.equals(a))
							return i;
					}
				}
			}
		}
		return -1;
	}

	// Standard methods

	@Override
	public String toString()
	{
		int i, j, n;
		Object o;
		String s = "";
		s = "[ ";
		for (i = 0; i < numStates; i++) {
			if (i > 0)
				s += ", ";
			s += i + ": ";
			s += "[";
			n = getNumChoices(i);
			for (j = 0; j < n; j++) {
				if (j > 0)
					s += ",";
				o = getAction(i, j);
				if (o != null)
					s += o + ":";
				s += trans.get(i).get(j);
			}
			s += "]";
		}
		s += " ]\n";
		return s;
	}

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof MDPSimple))
			return false;
		MDPSimple mdp = (MDPSimple) o;
		if (numStates != mdp.numStates)
			return false;
		if (!initialStates.equals(mdp.initialStates))
			return false;
		if (!trans.equals(mdp.trans))
			return false;
		// TODO: compare actions (complicated: null = null,null,null,...)
		return true;
	}
}
