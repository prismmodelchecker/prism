//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package prism;

import java.util.Map.Entry;
import java.util.Vector;

import jdd.*;
import parser.*;

/*
 * Class for MTBDD-based storage of a PRISM model that is a CTMC.
 */
public class StochModel extends ProbModel
{
	// accessor methods

	public ModelType getModelType()
	{
		return ModelType.CTMC;
	}

	public String getTransName()
	{
		return "Rate matrix";
	}

	public String getTransSymbol()
	{
		return "R";
	}

	// constructor

	public StochModel(JDDNode tr, JDDNode s, JDDNode sr[], JDDNode trr[], String rsn[], JDDVars arv, JDDVars acv,
			ModelVariablesDD mvdd, int nm, String[] mn, JDDVars[] mrv, JDDVars[] mcv, int nv, VarList vl, JDDVars[] vrv,
			JDDVars[] vcv, Values cv)
	{
		super(tr, s, sr, trr, rsn, arv, acv, mvdd, nm, mn, mrv, mcv, nv, vl, vrv, vcv, cv);
	}

	/**
	 * Get the embedded DTMC for this CTMC.
	 * <br>
	 * If parameter {@code log} is non-{@code null}, print some statistics.
	 * <br>
	 * If parameter {@code convertRewards} is {@code true}, transforms the rewards as well,
	 * otherwise the reward structures are stripped.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>nothing</i> ]
	 * @param log the log for printing statistics (may be {@code null})
	 * @param convertRewards if true, rewards are transformed, otherwise they are stripped
	 * @return the embedded DTMC
	 */
	public ProbModel getEmbeddedDTMC(PrismLog log, boolean convertRewards) throws PrismException
	{
		// Compute embedded Markov chain
		JDDNode diags = JDD.SumAbstract(trans.copy(), allDDColVars);
		JDDNode embeddedTrans = JDD.Apply(JDD.DIVIDE, trans.copy(), diags.copy());
		log.println("\nDiagonals vector: " + JDD.GetInfoString(diags, allDDRowVars.n()));
		log.println("Embedded Markov chain: " + JDD.GetInfoString(embeddedTrans, allDDRowVars.n() * 2));

		// Convert rewards
		JDDNode[] embStateRewards;
		JDDNode[] embTransRewards;
		String[] embRewardStructNames;
		if (convertRewards) {
			embStateRewards = new JDDNode[stateRewards.length];
			embTransRewards = new JDDNode[stateRewards.length];
			embRewardStructNames = rewardStructNames.clone();
			for (int i = 0; i < stateRewards.length; i++) {
				// state rewards are scaled
				embStateRewards[i] = JDD.Apply(JDD.DIVIDE, stateRewards[i].copy(), diags.copy());
				// trans rewards are simply copied
				embTransRewards[i] = transRewards[i].copy();
			}
		} else {
			// strip reward information
			embStateRewards = new JDDNode[0];
			embTransRewards = new JDDNode[0];
			embRewardStructNames = new String[0];
		}

		JDD.Deref(diags);

		ProbModel result = new ProbModel(embeddedTrans,
		                                 start.copy(),
		                                 embStateRewards,
		                                 embTransRewards,
		                                 embRewardStructNames,
		                                 allDDRowVars.copy(),
		                                 allDDColVars.copy(),
		                                 modelVariables.copy(),
		                                 numModules,
		                                 moduleNames,  // pass by reference, will not be changed
		                                 JDDVars.copyArray(moduleDDRowVars),
		                                 JDDVars.copyArray(moduleDDColVars),
		                                 numModules,
		                                 varList, // pass by reference, will not be changed
		                                 JDDVars.copyArray(moduleDDColVars),
		                                 JDDVars.copyArray(moduleDDColVars),
		                                 constantValues // pass by reference, will not be changed
		                                );

		// set reachable states to be the same as for the CTMC
		result.setReach(getReach().copy());

		// copy labels
		for (Entry<String, JDDNode> label : labelsDD.entrySet()) {
			result.addLabelDD(label.getKey(), label.getValue().copy());
		}

		return result;
	}
}
