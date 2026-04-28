//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Mateusz Ujma <mateusz.ujma@cs.ox.ac.uk> (University of Oxford)
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import prism.PrismComponent;
import prism.PrismException;

/**
 * Explicit maximal end component computer for a nondeterministic model such as an MDP.
 * Implements the algorithm from p.48 of:
 * Luca de Alfaro. Formal Verification of Probabilistic Systems. Ph.D. thesis, Stanford University (1997)
 */
public class ECComputerDefault extends ECComputer
{
	/** The model to compute (M)ECs for **/
	private NondetModel<?> model;

	/** Computed list of MECs **/
	private List<BitSet> mecs = new ArrayList<BitSet>();

	/**
	 * Build (M)EC computer for a given model.
	 */
	public ECComputerDefault(PrismComponent parent, NondetModel<?> model) throws PrismException
	{
		super(parent);
		this.model = model;
	}

	// Methods for ECComputer interface

	@Override
	public void computeMECStates() throws PrismException
	{
		mecs = findEndComponents(null, null);
	}

	@Override
	public void computeMECStates(BitSet restrict) throws PrismException
	{
		mecs = findEndComponents(restrict, null);
	}

	@Override
	public void computeMECStates(BitSet restrict, BitSet accept) throws PrismException
	{
		mecs = findEndComponents(restrict, accept);
	}

	@Override
	public List<BitSet> getMECStates()
	{
		return mecs;
	}

	@Override
	public void computeMECStatesStreaming(MECConsumer consumer) throws PrismException
	{
		BitSet restrict = new BitSet();
		restrict.set(0, model.getNumStates());
		findMECsStreaming(model, restrict, consumer);
	}

	// Computation

	/**
	 * Find all accepting maximal end components (MECs) in the submodel obtained
	 * by restricting this one to the set of states {@code restrict},
	 * where acceptance is defined as those which intersect with {@code accept}.
	 * If {@code restrict} is null, we look at the whole model, not a submodel.
	 * If {@code accept} is null, the acceptance condition is trivially satisfied.
	 * @param restrict BitSet for the set of states to restrict to
	 * @param accept BitSet for the set of accepting states
	 * @return a list of BitSets representing the MECs
	 */
	private List<BitSet> findEndComponents(BitSet restrict, BitSet accept) throws PrismException
	{
		// If restrict is null, look within set of all reachable states
		if (restrict == null) {
			restrict = new BitSet();
			restrict.set(0, model.getNumStates());
		}
		if (restrict.isEmpty())
			return new ArrayList<>();

		List<BitSet> MECs = new ArrayList<>();
		findMECsStreaming(model, restrict, MECs::add);

		// Filter and return those that contain a state in accept
		if (accept != null) {
			MECs.removeIf(mec -> !mec.intersects(accept));
		}
		return MECs;
	}

	/**
	 * Find all MECs within {@code states} of {@code currentModel}, calling
	 * {@code mecConsumer} for each MEC found (in {@code currentModel}'s state space).
	 *
	 * SCCs are processed one at a time as Tarjan emits them, so at most one SCC
	 * is alive at any given moment per recursion level. This avoids accumulating
	 * O(numMECs) full-state-space BitSets simultaneously.
	 */
	private void findMECsStreaming(NondetModel<?> currentModel, BitSet states, MECConsumer mecConsumer)
			throws PrismException
	{
		BitSet E = (BitSet) states.clone();
		SubNondetModel<?> submodel = restrict(currentModel, E);
		if (submodel.getNumStates() == 0)
			return;
		final int submodelSize = submodel.getNumStates();

		// Translate a MEC in submodel's state space back to currentModel's state space.
		MECConsumer childMecConsumer = subModelMEC -> {
			BitSet currentModelMEC = new BitSet();
			for (int i = subModelMEC.nextSetBit(0); i >= 0; i = subModelMEC.nextSetBit(i + 1)) {
				currentModelMEC.set(submodel.translateState(i));
			}
			mecConsumer.accept(currentModelMEC);
		};

		// Streaming SCC consumer: immediately recurse into each completed SCC rather
		// than collecting all SCCs before processing any. This keeps peak memory at
		// O(recursion_depth x stateSpaceSize) instead of O(numMECs x stateSpaceSize).
		int[] sccCount = {0};
		BitSet[] firstSCC = {null};

		SCCConsumer sccConsumer = new SCCConsumer() {
			private BitSet current;

			@Override
			public void notifyStartSCC()
			{
				current = new BitSet();
			}

			@Override
			public void notifyStateInSCC(int s)
			{
				current.set(s);
			}

			@Override
			public void notifyEndSCC() throws PrismException
			{
				sccCount[0]++;
				if (sccCount[0] == 1) {
					// Buffer the first SCC: we don't yet know if it is the only one.
					firstSCC[0] = current;
				} else {
					// A second SCC arrived, so we have multiple SCCs.
					// Process the buffered first SCC immediately, then this one.
					if (firstSCC[0] != null) {
						processECSCC(firstSCC[0], submodel, childMecConsumer);
						firstSCC[0] = null;
					}
					processECSCC(current, submodel, childMecConsumer);
				}
				current = null;
			}

			@Override
			public void notifyDone() throws PrismException
			{
				if (sccCount[0] == 1 && firstSCC[0] != null) {
					if (firstSCC[0].cardinality() == submodelSize) {
						// Single SCC covering all states of the restricted submodel → MEC.
						childMecConsumer.accept(firstSCC[0]);
					} else {
						// Single SCC that doesn't cover all restricted states (transient
						// states survived restrict but are not in any SCC). Recurse.
						processECSCC(firstSCC[0], submodel, childMecConsumer);
					}
					firstSCC[0] = null;
				}
			}
		};

		SCCComputer sccc = SCCComputer.createSCCComputer(this, submodel, sccConsumer);
		sccc.computeSCCs();
	}

	/**
	 * Process one SCC from the streaming consumer: emit it directly if it is a
	 * singleton MEC, or recurse via {@link #findMECsStreaming} otherwise.
	 * Avoids creating a SubNondetModel for singleton non-MECs.
	 */
	private void processECSCC(BitSet scc, NondetModel<?> model, MECConsumer consumer) throws PrismException
	{
		if (scc.cardinality() == 1) {
			int s = scc.nextSetBit(0);
			if (isSingletonMEC(model, s)) {
				consumer.accept(scc);
			}
			// Non-MEC singleton: restrict({s}) will be empty, so no recursion needed.
		} else {
			findMECsStreaming(model, scc, consumer);
		}
	}

	/**
	 * Returns true if state {@code s} in {@code model} has at least one action
	 * whose entire successor set is {@code {s}} (a self-loop action), making it
	 * a valid 1-state MEC on its own.
	 */
	private static boolean isSingletonMEC(NondetModel<?> model, int s)
	{
		BitSet singleton = new BitSet();
		singleton.set(s);
		for (int i = 0; i < model.getNumChoices(s); i++) {
			if (model.allSuccessorsInSet(s, i, singleton)) {
				return true;
			}
		}
		return false;
	}

	private SubNondetModel<?> restrict(NondetModel<?> model, BitSet states)
	{
		Map<Integer, BitSet> actions = new HashMap<Integer, BitSet>();
		BitSet initialStates = new BitSet();
		initialStates.set(states.nextSetBit(0));

		boolean changed = true;
		while (changed) {
			changed = false;
			actions.clear();
			for (int i = states.nextSetBit(0); i >= 0; i = states.nextSetBit(i + 1)) {
				BitSet act = new BitSet();
				for (int j = 0; j < model.getNumChoices(i); j++) {
					if (model.allSuccessorsInSet(i, j, states)) {
						act.set(j);
					}
				}
				if (act.isEmpty()) {
					states.clear(i);
					changed = true;
				} else {
					actions.put(i, act);
				}
			}
		}

		return new SubNondetModel<>(model, states, actions, initialStates);
	}
}
