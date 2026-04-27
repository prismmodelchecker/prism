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
		// Initialise L with set of all candidate sets to process (if non-empty)
		List<BitSet> L = new ArrayList<BitSet>();
		if (restrict.isEmpty())
			return L;
		L.add(restrict);
		// Find MECs: a candidate E is a MEC iff restrict() removes no states and
		// the result is strongly connected (single SCC covering all remaining states).
		// Otherwise split E into its SCCs and process each one further.
		List<BitSet> MECs = new ArrayList<BitSet>();
		while (!L.isEmpty()) {
			BitSet E = L.remove(0);
			SubNondetModel<?> submodel = restrict(model, E);
			if (submodel.getNumStates() == 0)
				continue;
			List<BitSet> sccs = translateStates(submodel, computeSCCs(submodel));
			if (sccs.size() == 1 && sccs.get(0).cardinality() == submodel.getNumStates()) {
				MECs.add(sccs.get(0));
			} else {
				L.addAll(sccs);
			}
		}
		// Filter and return those that contain a state in accept
		if (accept != null) {
			int i = 0;
			while (i < MECs.size()) {
				if (!MECs.get(i).intersects(accept)) {
					MECs.remove(i);
				} else {
					i++;
				}
			}
		}
		return MECs;
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

	private List<BitSet> computeSCCs(NondetModel<?> model) throws PrismException
	{
		SCCConsumerStore sccs = new SCCConsumerStore();
		SCCComputer sccc = SCCComputer.createSCCComputer(this, model, sccs);
		sccc.computeSCCs();
		return sccs.getSCCs();
	}

	private List<BitSet> translateStates(SubNondetModel<?> model, List<BitSet> sccs)
	{
		List<BitSet> r = new ArrayList<BitSet>();
		for (int i = 0; i < sccs.size(); i++) {
			BitSet set = sccs.get(i);
			BitSet set2 = new BitSet();
			r.add(set2);
			for (int j = set.nextSetBit(0); j >= 0; j = set.nextSetBit(j + 1)) {
				set2.set(model.translateState(j));

			}
		}
		return r;
	}

}
