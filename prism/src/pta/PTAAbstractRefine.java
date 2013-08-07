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

package pta;

import java.util.*;

import prism.ModelType;
import prism.PrismComponent;
import prism.PrismException;
import explicit.*;

/**
 * Probabilistic reachability for PTAs, using abstraction/refinement of stochastic games.
 * See: "Stochastic Games for Verification of Probabilistic Timed Automata" (FORMATS'09).
 */
public class PTAAbstractRefine extends QuantAbstractRefine
{
	// PTA, target info
	protected PTA pta = null;
	protected BitSet targetLocs;
	protected Constraint targetConstraint;
	// Forwards reachability graph
	protected ReachabilityGraph graph;

	// Flags + options

	// Do we cache zones representing validity of transition subsets
	// for reuse when splitting that zone later on? (definitely saves a little
	// bit of time, but at the expense of some space). 
	boolean storeValidZones = true;

	/**
	 * Default constructor.
	 */
	public PTAAbstractRefine(PrismComponent parent) throws PrismException
	{
		super(parent);
		// Just do basic config for QuantAbstractRefine
		setModelType(ModelType.MDP);
		setPropertyType(QuantAbstractRefine.PropertyType.PROB_REACH);
	}
	
	/**
	 * Compute min/max PTA reachability probabilities using STPG abstraction refinement. 
	 */
	public double forwardsReachAbstractRefine(PTA pta, BitSet targetLocs, Constraint targetConstraint, boolean min)
			throws PrismException
	{
		// Store PTA/target info
		this.pta = pta;
		this.targetLocs = targetLocs;
		this.targetConstraint = targetConstraint;

		// Execute abstraction-refinement loop
		return abstractRefine(min);
	}

	// Implementation of initialise() for abstraction-refinement loop; see superclass for details 

	@Override
	protected void initialise() throws PrismException
	{
		ForwardsReach forwardsReach;
		List<Integer> initialStates;
		int src, numStates;

		// Build forwards reachability graph
		forwardsReach = new ForwardsReach(mainLog);
		graph = forwardsReach.buildForwardsGraph(pta, targetLocs, targetConstraint);
		// Store the set of target/initial states from forwards reachability
		target = forwardsReach.getTarget();
		initialStates = forwardsReach.getInitialStates();

		// Compute validities for all symbolic transitions in the graph
		graph.computeAllValidities();

		// Display states, graph, etc.
		if (verbosity >= 5) {
			mainLog.println("\nStates: ");
			graph.printStates(mainLog);
			mainLog.println("\nGraph: " + graph);
			mainLog.println("Target states: " + target);
		}

		// Build empty game
		abstraction = new STPGAbstrSimple();

		// Add all states
		numStates = graph.states.size();
		abstraction.addStates(numStates);
		// Set initial states
		for (int i : initialStates) {
			abstraction.addInitialState(i);
		}
		// Build each state individually
		for (src = 0; src < numStates; src++) {
			buildSTPGState(src);
		}
	}

	// Implementation of rebuildAbstraction(...) for abstraction-refinement loop; see superclass for details 

	@Override
	protected void rebuildAbstraction(Set<Integer> rebuildStates) throws PrismException
	{
		for (int src : rebuildStates) {
			abstraction.clearState(src);
			buildSTPGState(src);
		}
	}

	/**
	 * Build a single state of the STPG.
	 */
	protected void buildSTPGState(int src) throws PrismException
	{
		LocZone lz;
		NCZone z;
		ArrayList<NCZone> valids;
		SymbolicTransition st;
		int i, numTransitions, numValids, map[];

		// Get state and num transitions
		lz = graph.states.get(src);
		numTransitions = graph.trans.get(src).size();

		// Skip build of state if no transitions (should just be target states)
		if (numTransitions == 0) {
			if (!target.get(src))
				mainLog.printWarning("Building STPG state (" + src + ") with no transitions");
			return;
		}

		// Compute validity of each outgoing transition from this state.
		// In fact, since guards for multiple transitions are often identical,
		// we only do this for distinct ones, and build a 'map' between them.
		valids = new ArrayList<NCZone>(numTransitions);
		map = new int[numTransitions];
		for (i = 0; i < numTransitions; i++) {
			st = graph.trans.get(src).get(i);
			z = (NCZone) st.valid;
			// Check that validity constraint is non-empty
			if (z.isEmpty())
				throw new PrismException("Found invalid symbolic transition");
			// See if we already have a copy of this zone z
			map[i] = valids.indexOf(z);
			// If not, add to list and store index in map
			if (map[i] == -1) {
				map[i] = valids.size();
				valids.add(z);
			}
		}

		// Recursive construction of this state
		numValids = valids.size();
		buildSTPGStateRec(src, new DBMList(lz.zone), new BitSet(numValids), valids, map, 0, numValids);

		// Check for deadlocks in the constructed STPG
		// (should never occur because of the restrictions we impose on PTAs)
		if (abstraction.getNumChoices(src) == 0) {
			throw new PrismException("STPG has deadlock in state #" + src + ":" + graph.states.get(src));
		}
	}

	/**
	 * Recursive construction of STPG state 'src'.
	 * Consider each combination of outgoing transitions (combination stored in 'bitSet'),
	 * and build validity constraint ('valid') for each combination.
	 * (Note: actually only consider transitions with distinct validity - see above.)
	 */
	protected void buildSTPGStateRec(int src, NCZone valid, BitSet bitSet, ArrayList<NCZone> valids, int[] map,
			int level, int numValids) throws PrismException
	{
		STPGAbstrSimple stpg;
		ArrayList<SymbolicTransition> sts;
		DistributionSet distrSet;
		Distribution distr;
		BitSet actionBitSet = null;
		int count, i, dest;

		// Cast abstraction to STPG since we know the type
		stpg = (STPGAbstrSimple) abstraction;

		// Bottom of recursion: check if valid and, if so, add appropriate distribution set to game
		if (level == numValids) {
			// Check this combination of transitions is non-empty  
			if (!valid.isEmpty()) {
				// Ignore the case where no transitions are enabled
				// (has been dealt with earlier by adding explicit "diverge" transition)
				if (bitSet.cardinality() == 0)
					return;
				// Create distribution set for this combination of transitions
				distrSet = stpg.newDistributionSet(null);
				// If using BitSets for action labels (as opposed to storing the zones directly)
				// create a bit set that will represent the actual combination of transitions
				// (the bit set passed into this function is indexed over distinct validity transitions only)
				sts = graph.trans.get(src);
				if (!storeValidZones)
					actionBitSet = new BitSet(sts.size());
				i = 0;
				for (SymbolicTransition st : sts) {
					// Add distribution corresponding to ith transition if
					// it is included in the set we are considering in this iteration
					if (bitSet.get(map[i])) {
						distr = new Distribution();
						count = 0;
						for (Edge edge : st.tr.getEdges()) {
							dest = st.dests[count];
							if (dest != -1)
								distr.add(dest, edge.getProbability());
							count++;
						}
						if (!distr.isEmpty())
							distrSet.add(distr);
						// If using BitSets for action labels, modify the bit set
						// (setting kth bit to denote that kth transition is included)
						if (!storeValidZones)
							actionBitSet.set(i);
					}
					i++;
				}
				// Set the (BitSet or Zone) action label for the distribution set and add it to the game
				if (storeValidZones)
					distrSet.setAction(valid);
				else
					distrSet.setAction(actionBitSet);
				stpg.addDistributionSet(src, distrSet);
			}
		} else {
			// Recursive step
			// Note that the construction of the validity constraint for
			// transition combinations is done recursively - this gives big gains in efficiency.
			// Note also that, the first thing added to the conjunction (i.e. the validity constraint)
			// before recursion starts is the symbolic state zone. We can only do this because at
			// least one transition is enabled in this combination, so its validity will need to include
			// the symbolic state zone. Generally, it is better to
			// add non-complemented zones, like this, early to avoid blowups
			// (in terms of conjunctions on complements, which are typically large DBM lists).
			// Finally, note that use of combined intersectComplement operation is, like
			// for the refinement state-split set, crucial for efficiency.
			// bitSet[level] = 0
			NCZone validNew;
			validNew = valid.deepCopy();
			validNew.intersectComplement(valids.get(level));
			bitSet.set(level, false);
			if (!validNew.isEmpty())
				buildSTPGStateRec(src, validNew, bitSet, valids, map, level + 1, numValids);
			// bitSet[level] = 1
			validNew = valid.deepCopy();
			validNew.intersect(valids.get(level));
			bitSet.set(level, true);
			if (!validNew.isEmpty())
				buildSTPGStateRec(src, validNew, bitSet, valids, map, level + 1, numValids);
		}
	}

	// Implementation of splitState(...) for abstraction-refinement loop; see superclass for details

	// Needs to update:
	//  - local storage about abstraction (i.e. states and symbolic transitions in reachability graph)
	//  - abstraction (new states, initial states and transitions)
	//  - target set 

	@Override
	protected int splitState(int splitState, List<List<Integer>> choiceLists, Set<Integer> rebuiltStates,
			Set<Integer> rebuildStates) throws PrismException
	{
		LocZone lzSplit;
		DBMList z, valid1;
		int[] newStateMap;
		int i, n, numStates, numNewStates, choiceCount;
		Object action;
		BitSet actionBitSet;
		ArrayList<DBMList> partition;
		boolean rebuild;

		// Get abstract state to split 
		lzSplit = graph.states.get(splitState);
		if (verbosity >= 1)
			mainLog.println("Splitting: #" + splitState + "=" + lzSplit);

		// Compute zones that this state will be split into; store in 'partition'
		partition = new ArrayList<DBMList>();
		choiceCount = 0;
		for (List<Integer> choiceList : choiceLists) {
			choiceCount += choiceList.size();
			z = new DBMList(pta);
			for (int in : choiceList) {
				// If using BitSets for action labels (i.e. have not cached zones for reuse)
				// need to build the corresponding zone
				action = ((STPGAbstrSimple) abstraction).getChoice(splitState, in).action;
				if (!storeValidZones) {
					actionBitSet = (BitSet) action;
					List<SymbolicTransition> sts = graph.trans.get(splitState);
					n = sts.size();
					Set<NCZone> valids = new LinkedHashSet<NCZone>();
					Set<NCZone> validCmpls = new LinkedHashSet<NCZone>();
					for (i = 0; i < n; i++) {
						SymbolicTransition st = sts.get(i);
						// Store in appropriate set 
						(actionBitSet.get(i) ? valids : validCmpls).add((DBMList) st.valid);
					}
					//log.println("valids: " + valids);
					//log.println("validCmpls: " + validCmpls);
					// Build zone
					valid1 = new DBMList(DBM.createTrue(pta));
					for (Zone z2 : valids) {
						valid1.intersect(z2);
					}
					for (NCZone z2 : validCmpls) {
						valid1.intersectComplement(z2);
					}
					valid1.intersect(lzSplit.zone);
				}
				// If using zones, then we just use the stored zone
				else {
					valid1 = (DBMList) action;
				}
				z.addDBMs(valid1);
			}
			partition.add(z);
		}
		// Compute zone for remainder of the partition
		// (NB: another way to do this is to build a union of remaining player 1 choices)
		// (but, empirically, this seems to be bad in terms of DBM list efficiency)
		// (Note, though, we can check whether we need to do this at all, which is good)
		if (choiceCount < abstraction.getNumChoices(splitState)) {
			// Compute union of all partition elements from above 
			DBMList all = new DBMList(pta);
			for (DBMList dbml : partition) {
				all.addDBMs(dbml.deepCopy());
			}
			// Subtract union from symbolic state zone 
			DBMList rest = new DBMList(lzSplit.zone);
			rest.intersectComplement(all);
			if (!rest.isEmpty())
				partition.add(rest);
		}
		// Check we actually got a strict split of the partition
		if (partition.size() <= 1) {
			mainLog.printWarning("failed to split state #" + splitState + "=" + lzSplit);
			return 1;
		}

		// Update symbolic state set info (graph.states) and store info about indices of new states
		numNewStates = partition.size();
		newStateMap = new int[numNewStates];
		for (i = 0; i < numNewStates; i++) {
			// First new state overwrites one that is being split
			if (i == 0) {
				graph.states.set(splitState, new LocZone(lzSplit.loc, partition.get(i)));
				newStateMap[i] = splitState;
			}
			// Other new states are appended to end of list
			else {
				graph.states.add(new LocZone(lzSplit.loc, partition.get(i)));
				newStateMap[i] = graph.states.size() - 1;
				graph.copyState(splitState);
			}
		}

		// Display info
		if (verbosity >= 1) {
			mainLog.println("Splitting: #" + splitState + "=" + lzSplit);
			mainLog.println("into " + numNewStates + ":");
			for (i = 0; i < numNewStates; i++)
				mainLog.println("#" + newStateMap[i] + "=" + partition.get(i));
		}
		if (verbosity >= 5)
			mainLog.println("New states: " + graph.states);

		// Add new states to the abstraction
		abstraction.addStates(numNewStates - 1);

		// Add new states to initial state set if needed
		// Note: we assume any abstract state contains either all/no initial states
		if (abstraction.isInitialState(splitState)) {
			for (i = 1; i < numNewStates; i++) {
				abstraction.addInitialState(newStateMap[i]);
			}
		}

		// Rebuild target states
		for (i = 0; i < numNewStates; i++) {
			LocZone lz = graph.states.get(newStateMap[i]);
			target.set(newStateMap[i], isTarget(lz));
		}

		// Update symbolic transitions and abstraction
		Set<SymbolicTransition> oldSTs = new LinkedHashSet<SymbolicTransition>();
		Set<SymbolicTransition> newSTs = new LinkedHashSet<SymbolicTransition>();
		// Go through all abstract states
		numStates = graph.states.size();
		for (i = 0; i < numStates; i++) {
			oldSTs.clear();
			newSTs.clear();
			// Do we need to rebuild this state?
			// (i.e. is it a new state or a successor of the split state?)
			rebuild = false;
			// For a new state...
			if (i == splitState || i > numStates - numNewStates) {
				// Split all symbolic transitions from this state
				for (SymbolicTransition st : graph.trans.get(i)) {
					oldSTs.add(st);
					splitSymbolicTransition(i, st, splitState, newStateMap, newSTs);
				}
				rebuild = true;
			}
			// For a successor state
			else {
				// Split symbolic transitions that go to the split state
				for (SymbolicTransition st : graph.trans.get(i)) {
					if (st.hasSuccessor(splitState)) {
						oldSTs.add(st);
						splitSymbolicTransition(i, st, splitState, newStateMap, newSTs);
						rebuild = true;
					}
				}
			}
			// Bail out, if we didn't need to rebuild for this state
			if (!rebuild)
				continue;
			// Now, actually modify the graph
			// (didn't do this on the fly because don't went to change
			// the list that we are iterating over)
			for (SymbolicTransition st : oldSTs)
				graph.trans.get(i).remove(st);
			for (SymbolicTransition st : newSTs)
				graph.trans.get(i).add(st);
			if ((verbosity >= 1) && !oldSTs.isEmpty()) {
				mainLog.print("Replacing symbolic transitions: " + i + ":" + oldSTs);
				mainLog.println(" with: " + i + ":" + newSTs);
			}
			// Rebuild this state of the abstraction
			abstraction.clearState(i);
			buildSTPGState(i);
			rebuiltStates.add(i);
		}

		if (verbosity >= 5) {
			mainLog.println("New graph: " + graph);
		}

		return numNewStates;
	}

	/**
	 * Split a symbolic transition, based on the division of a symbolic state into several parts.
	 * @param src: The source of the symbolic transition to be split 
	 * @param st: The symbolic transition to be split
	 * @param splitState: The index of the symbolic state that is being split 
	 * @param newStateMap: The indices of the new states
	 * @param newSTs: Where to put the new symbolic transitions
	 */
	private void splitSymbolicTransition(int src, SymbolicTransition st, int splitState, int newStateMap[],
			Set<SymbolicTransition> newSTs)
	{
		// Take a copy of the transition, because we will modify it when analysing it
		SymbolicTransition stNew = new SymbolicTransition(st);
		// Recursively...
		splitSymbolicTransition(src, stNew, splitState, newStateMap, newSTs, 0, st.dests.length);
	}

	private void splitSymbolicTransition(int src, SymbolicTransition st, int splitState, int newStateMap[],
			Set<SymbolicTransition> newSTs, int level, int n)
	{
		if (level == n) {
			Zone valid = graph.computeValidity(src, st.tr, st.dests);
			if (!valid.isEmpty()) {
				SymbolicTransition stNew = new SymbolicTransition(st);
				stNew.valid = valid;
				newSTs.add(stNew);
			}
		} else {
			if (st.dests[level] == splitState) {
				int m = newStateMap.length;
				for (int i = 0; i < m; i++) {
					st.dests[level] = newStateMap[i];
					splitSymbolicTransition(src, st, splitState, newStateMap, newSTs, level + 1, n);
				}
				st.dests[level] = splitState;
			} else {
				splitSymbolicTransition(src, st, splitState, newStateMap, newSTs, level + 1, n);
			}
		}
	}

	// TODO: why does this not work?
	private Zone valid2new(LocZone lz, Transition tr, int[] dests)
	{
		int count;
		Zone z, z2;

		z = new DBMList(DBM.createTrue(pta));
		// Conjunction over edges in transition
		count = 0;
		for (Edge edge : tr.getEdges()) {
			// Get (copy of) edge target
			z2 = graph.states.get(dests[count]).zone.deepCopy();
			// Backwards reset of clocks for edge
			for (Map.Entry<Integer, Integer> e : edge.getResets()) {
				z2.backReset(e.getKey(), e.getValue());
			}
			// Intersect
			z.intersect(z2);
			count++;
		}
		// Intersect with transition guard 
		for (Constraint c : tr.getGuardConstraints()) {
			z.addConstraint(c);
		}
		// Time pre
		z.down();
		// Intersect with symbolic state zone
		z.intersect(lz.zone);

		return z;
	}

	private boolean isTarget(LocZone lz)
	{
		return targetLocs.get(lz.loc) && (targetConstraint == null || lz.zone.isSatisfied(targetConstraint));
	}
}
