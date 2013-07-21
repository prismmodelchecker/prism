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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import explicit.SCCComputer.SCCMethod;

/**
 * Maximal end component computer for a nondeterministic model such as MDP.
 */
public class ECComputerDefault extends ECComputer
{
	/* The model to compute (M)ECs for */
	private NondetModel model;
	/* Computed list of MECs */
	private List<BitSet> mecs = new ArrayList<BitSet>();

	/**
	 * Build (M)EC computer for a given model.
	 */
	public ECComputerDefault(NondetModel model)
	{
		this.model = model;
	}

	// Methods for SCCComputer interface
	// Algorithm based on the one found in Luca de Alfaro Ph.D Thesis

	@Override
	public void computeMECs()
	{
		SubNondetModel submodel = null;
		List<BitSet> L = new ArrayList<BitSet>();
		
		BitSet initialModel = new BitSet();
		initialModel.set(0, model.getNumStates());
		L.add(initialModel);
		
		boolean changed = true;
		while(changed) {
			changed = false;
			BitSet E = L.remove(0);
			submodel = restrict(model, E);
			List<BitSet> sccs = translateStates(submodel, computeSCCs(submodel));
			L = replaceEWithSCCs(L, E, sccs);
			changed = canLBeChanged(L,E);
		}
		mecs = L;
	}
	
	private Set<BitSet> processedSCCs = new HashSet<BitSet>();
	
	private boolean canLBeChanged(List<BitSet> L, BitSet E) {
		processedSCCs.add(E);
		for(int i=0;i<L.size();i++) {
			if(!processedSCCs.contains(L.get(i))) {
				return true;
			}
		} 
		return false;
	}
	
	private List<BitSet> replaceEWithSCCs(List<BitSet> L, BitSet E, List<BitSet> sccs) {
		if(sccs.size() > 0) {
			List<BitSet> toAdd = new ArrayList<BitSet>();
			for(int i=0;i<sccs.size();i++) {
				if(!L.contains(sccs.get(i))) {
					toAdd.add(sccs.get(i));
				}
			}
			if(toAdd.size() > 0) {
				L.addAll(toAdd);
			}
		}
		return L;
	}
	
	private SubNondetModel restrict(NondetModel model, BitSet states) {
		Map<Integer,BitSet> actions = new HashMap<Integer,BitSet>();
		BitSet initialStates = new BitSet();
		initialStates.set(states.nextSetBit(0));
		
		boolean changed = true;
		while(changed) {
			changed = false;
			actions.clear();
			for(int i=0;i<model.getNumStates();i++) {
				BitSet act = new BitSet();
				if(states.get(i)) {
					for(int j=0;j<model.getNumChoices(i);j++) {
						if(model.allSuccessorsInSet(i, j, states)) {
							act.set(j);
						}
					}
					if(act.cardinality() == 0) {
						states.clear(i);
						changed = true;
					} 
					actions.put(i,act);
				}
			}
		}
		
		return new SubNondetModel(model, states, actions, initialStates);
	}
	
	private List<BitSet> computeSCCs(NondetModel model) {
		SCCComputer sccc = SCCComputer.createSCCComputer(SCCMethod.TARJAN, model);
		sccc.computeSCCs();
		return sccc.getSCCs();
	}
	
	private List<BitSet> translateStates(SubNondetModel model, List<BitSet> sccs) {
		List<BitSet> r = new ArrayList<BitSet>();
		for(int i=0;i<sccs.size();i++) {
			BitSet set = sccs.get(i);
			BitSet set2 = new BitSet();
			r.add(set2);
			for(int j=set.nextSetBit(0); j>=0; j=set.nextSetBit(j+1)) { 
				set2.set(model.translateState(j));
				
			}
		}
		return r;
	}
	
	@Override
	public List<BitSet> getMECs()
	{
		return mecs;
	}
	
	private boolean isMEC(BitSet b) {
		if(b.cardinality() == 0) return false;
		
		int state = b.nextSetBit(0);
		while(state != -1) {
			boolean atLeastOneAction = false;
			for(int i=0;i<model.getNumChoices(state);i++) {
				if(model.allSuccessorsInSet(state, i, b)) {
					atLeastOneAction = true;
				}
			}
			if(!atLeastOneAction) {
				return false;
			}
			state = b.nextSetBit(state+1);
		}
		
		return true;
	}
}
