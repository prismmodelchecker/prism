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

import java.io.*;
import java.util.Vector;

import jdd.*;
import odd.*;
import mtbdd.*;
import parser.*;
import sparse.*;

/*
 * Class for MTBDD-based storage of a PRISM model that is an MDP.
 */
public class NondetModel extends ProbModel
{
	// Extra info
	protected double numChoices; // number of choices

	// Extra dd stuff
	protected JDDNode nondetMask; // mask for nondeterministic choices
	protected JDDVars allDDSynchVars; // synch actions dd vars
	protected JDDVars allDDSchedVars; // scheduler dd vars
	protected JDDVars allDDChoiceVars; // local nondet choice dd vars
	protected JDDVars allDDNondetVars; // all nondet dd vars (union of two above)
	protected JDDNode transInd; // BDD for independent part of trans
	protected JDDNode transSynch[]; // BDD for parts of trans from each action

	// accessor methods

	// type
	public ModelType getModelType()
	{
		return ModelType.MDP;
	}

	public long getNumChoices()
	{
		return (numChoices > Long.MAX_VALUE) ? -1 : Math.round(numChoices);
	}

	public String getNumChoicesString()
	{
		return PrismUtils.bigIntToString(numChoices);
	}

	public JDDNode getNondetMask()
	{
		return nondetMask;
	}

	public JDDVars getAllDDSynchVars()
	{
		return allDDSynchVars;
	}

	public JDDVars getAllDDSchedVars()
	{
		return allDDSchedVars;
	}

	public JDDVars getAllDDChoiceVars()
	{
		return allDDChoiceVars;
	}

	public JDDVars getAllDDNondetVars()
	{
		return allDDNondetVars;
	}

	public JDDNode getTransInd()
	{
		return transInd;
	}

	public JDDNode[] getTransSynch()
	{
		return transSynch;
	}

	// additional useful methods to do with dd vars
	public int getNumDDNondetVars()
	{
		return allDDNondetVars.n();
	}

	public int getNumDDVarsInTrans()
	{
		return allDDRowVars.n() * 2 + allDDNondetVars.n();
	}

	public String getTransName()
	{
		return "Transition matrix";
	}

	public String getTransSymbol()
	{
		return "S";
	}

	/**
	 * Do all choices in in each state have a unique action label?
	 */
	public boolean areAllChoiceActionsUnique()
	{
		// Action labels
		for (int i = 0; i < numSynchs; i++) {
			JDD.Ref(transActions);
			JDDNode tmp = JDD.Equals(transActions, i + 1);
			tmp = JDD.SumAbstract(tmp, allDDNondetVars);
			double max = JDD.FindMax(tmp);
			JDD.Deref(tmp);
			if (max > 1)
				return false;
		}
		// Unlabelled choices
		JDD.Ref(reach);
		JDD.Ref(transActions);
		JDD.Ref(nondetMask);
		JDDNode tmp = JDD.And(reach, JDD.And(JDD.LessThanEquals(transActions, 0), JDD.Not(nondetMask)));
		tmp = JDD.SumAbstract(tmp, allDDNondetVars);
		double max = JDD.FindMax(tmp); 
		JDD.Deref(tmp);
		if (max > 1)
			return false;
		
		return true;
	}
	
	// set methods for things not set up in constructor

	public void setTransInd(JDDNode transInd)
	{
		this.transInd = transInd;
	}

	public void setTransSynch(JDDNode[] transSynch)
	{
		this.transSynch = transSynch;
	}

	// constructor

	public NondetModel(JDDNode tr, JDDNode s, JDDNode sr[], JDDNode trr[], String rsn[], JDDVars arv, JDDVars acv, JDDVars asyv, JDDVars asv, JDDVars achv,
			JDDVars andv, Vector<String> ddvn, int nm, String[] mn, JDDVars[] mrv, JDDVars[] mcv, int nv, VarList vl, JDDVars[] vrv, JDDVars[] vcv, Values cv)
	{
		super(tr, s, sr, trr, rsn, arv, acv, ddvn, nm, mn, mrv, mcv, nv, vl, vrv, vcv, cv);

		allDDSynchVars = asyv;
		allDDSchedVars = asv;
		allDDChoiceVars = achv;
		allDDNondetVars = andv;

		transInd = null;
		transSynch = null;
	}

	// do reachability

	public void doReachability()
	{
		JDDNode tmp;

		// remove any nondeterminism
		JDD.Ref(trans01);
		tmp = JDD.MaxAbstract(trans01, allDDNondetVars);

		// compute reachable states
		reach = PrismMTBDD.Reachability(tmp, allDDRowVars, allDDColVars, start);
		JDD.Deref(tmp);

		// work out number of reachable states
		numStates = JDD.GetNumMinterms(reach, allDDRowVars.n());

		// build odd
		odd = ODDUtils.BuildODD(reach, allDDRowVars);
	}

	// remove non-reachable states from various dds
	// (and calculate num transitions, etc.)
	// (and build mask)

	public void filterReachableStates()
	{
		super.filterReachableStates();

		// also filter transInd/tranSynch DDs (if necessary)
		if (transInd != null) {
			JDD.Ref(reach);
			transInd = JDD.Apply(JDD.TIMES, reach, transInd);
			for (int i = 0; i < numSynchs; i++) {
				JDD.Ref(reach);
				transSynch[i] = JDD.Apply(JDD.TIMES, reach, transSynch[i]);
			}
		}

		// build mask for nondeterminstic choices
		JDD.Ref(trans01);
		JDD.Ref(reach);
		if (this.nondetMask != null)
			JDD.Deref(this.nondetMask);
		// nb: this assumes that there are no deadlock states
		nondetMask = JDD.And(JDD.Not(JDD.ThereExists(trans01, allDDColVars)), reach);

		// work out number of choices
		double d = JDD.GetNumMinterms(nondetMask, getNumDDRowVars() + getNumDDNondetVars());
		numChoices = ((Math.pow(2, getNumDDNondetVars())) * numStates) - d;
	}

	@Override
	public void findDeadlocks(boolean fix)
	{
		// find states with at least one transition
		JDD.Ref(trans01);
		deadlocks = JDD.ThereExists(trans01, allDDColVars);
		deadlocks = JDD.ThereExists(deadlocks, allDDNondetVars);

		// find reachable states with no transitions
		JDD.Ref(reach);
		deadlocks = JDD.And(reach, JDD.Not(deadlocks));
		
		if (fix && !deadlocks.equals(JDD.ZERO)) {
			// remove deadlocks by adding self-loops to trans
			// (also update transInd (if necessary) at same time)
			// (note: we don't need to update transActions since
			//  action-less transitions are encoded as 0 anyway)
			// (note: would need to update transPerAction[0]
			//  but this is not stored for MDPs)
			JDDNode tmp;
			JDD.Ref(deadlocks);
			tmp = JDD.SetVectorElement(JDD.Constant(0), allDDNondetVars, 0, 1);
			tmp = JDD.And(tmp, JDD.Identity(allDDRowVars, allDDColVars));
			tmp = JDD.And(deadlocks, tmp);
			JDD.Ref(tmp);
			trans = JDD.Apply(JDD.PLUS, trans, tmp);
			JDD.Ref(tmp);
			trans01 = JDD.Apply(JDD.PLUS, trans01, tmp);
			if (transInd != null) {
				JDD.Ref(tmp);
				transInd = JDD.Or(transInd, JDD.ThereExists(tmp, allDDColVars));
			}
			JDD.Deref(tmp);
			// update mask
			JDD.Deref(nondetMask);
			JDD.Ref(trans01);
			JDD.Ref(reach);
			nondetMask = JDD.And(JDD.Not(JDD.ThereExists(trans01, allDDColVars)), reach);
			// update model stats
			numTransitions = JDD.GetNumMinterms(trans01, getNumDDVarsInTrans());
			double d = JDD.GetNumMinterms(nondetMask, getNumDDRowVars() + getNumDDNondetVars());
			numChoices = ((Math.pow(2, getNumDDNondetVars())) * numStates) - d;
		}
	}

	public void printTransInfo(PrismLog log, boolean extra)
	{
		int i, j, n;

		log.print("States:      " + getNumStatesString() + " (" + getNumStartStatesString() + " initial)" + "\n");
		log.print("Transitions: " + getNumTransitionsString() + "\n");
		log.print("Choices:     " + getNumChoicesString() + "\n");

		log.println();

		log.print(getTransName() + ": " + JDD.GetInfoString(trans, getNumDDVarsInTrans()));
		log.print(", vars: " + getNumDDRowVars() + "r/" + getNumDDColVars() + "c/" + getNumDDNondetVars() + "nd\n");
		if (extra) {
			log.print("DD vars (nd):");
			n = allDDNondetVars.getNumVars();
			for (i = 0; i < n; i++) {
				j = allDDNondetVars.getVarIndex(i);
				log.print(" " + j + ":" + ddVarNames.get(j));
			}
			log.println();
			log.print("DD vars (r/c):");
			n = allDDRowVars.getNumVars();
			for (i = 0; i < n; i++) {
				j = allDDRowVars.getVarIndex(i);
				log.print(" " + j + ":" + ddVarNames.get(j));
				j = allDDColVars.getVarIndex(i);
				log.print(" " + j + ":" + ddVarNames.get(j));
			}
			log.println();
			log.print(getTransName() + " terminals: " + JDD.GetTerminalsAndNumbersString(trans, getNumDDVarsInTrans()) + "\n");
			log.print("Reach: " + JDD.GetNumNodes(reach) + " nodes\n");
			log.print("ODD: " + ODDUtils.GetNumODDNodes() + " nodes\n");
			log.print("Mask: " + JDD.GetNumNodes(nondetMask) + " nodes, ");
			log.print(JDD.GetNumMintermsString(nondetMask, getNumDDRowVars() + getNumDDNondetVars()) + " minterms\n");

			for (i = 0; i < numRewardStructs; i++) {
				if (stateRewards[i] != null && !stateRewards[i].equals(JDD.ZERO)) {
					log.print("State rewards (" + (i + 1) + (("".equals(rewardStructNames[i])) ? "" : (":\"" + rewardStructNames[i] + "\"")) + "): ");
					log.print(JDD.GetNumNodes(stateRewards[i]) + " nodes (");
					log.print(JDD.GetNumTerminals(stateRewards[i]) + " terminal), ");
					log.print(JDD.GetNumMintermsString(stateRewards[i], getNumDDRowVars()) + " minterms\n");
					if (extra) {
						log.print("State rewards terminals (" + (i + 1) + (("".equals(rewardStructNames[i])) ? "" : (":\"" + rewardStructNames[i] + "\""))
								+ "): ");
						log.print(JDD.GetTerminalsAndNumbersString(stateRewards[i], getNumDDRowVars()) + "\n");
					}
				}
				if (transRewards[i] != null && !transRewards[i].equals(JDD.ZERO)) {
					log.print("Transition rewards (" + (i + 1) + (("".equals(rewardStructNames[i])) ? "" : (":\"" + rewardStructNames[i] + "\"")) + "): ");
					log.print(JDD.GetNumNodes(transRewards[i]) + " nodes (");
					log.print(JDD.GetNumTerminals(transRewards[i]) + " terminal), ");
					log.print(JDD.GetNumMintermsString(transRewards[i], getNumDDVarsInTrans()) + " minterms\n");
					if (extra) {
						log.print("Transition rewards terminals (" + (i + 1) + (("".equals(rewardStructNames[i])) ? "" : (":\"" + rewardStructNames[i] + "\""))
								+ "): ");
						log.print(JDD.GetTerminalsAndNumbersString(transRewards[i], getNumDDVarsInTrans()) + "\n");
					}
				}
			}
			if (transActions != null && !transActions.equals(JDD.ZERO)) {
				log.print("Action label indices: ");
				log.print(JDD.GetNumNodes(transActions) + " nodes (");
				log.print(JDD.GetNumTerminals(transActions) + " terminal)\n");
			}
			// Don't need to print info for transPerAction (not stored for MDPs)
		}
	}

	// export transition matrix to a file

	public void exportToFile(int exportType, boolean explicit, File file) throws FileNotFoundException, PrismException
	{
		if (!explicit) {
			// can only do explicit (sparse matrix based) export for mdps
		} else {
			PrismSparse.ExportMDP(trans, transActions, getSynchs(), getTransSymbol(), allDDRowVars, allDDColVars, allDDNondetVars, odd, exportType,
					(file != null) ? file.getPath() : null);
		}
	}

	// export state rewards vector to a file

	// returns string containing files used if there were more than 1, null otherwise

	public String exportStateRewardsToFile(int exportType, File file) throws FileNotFoundException, PrismException
	{
		if (numRewardStructs == 0)
			throw new PrismException("There are no state rewards to export");
		int i;
		String filename, allFilenames = "";
		for (i = 0; i < numRewardStructs; i++) {
			filename = (file != null) ? file.getPath() : null;
			if (filename != null && numRewardStructs > 1) {
				filename = PrismUtils.addCounterSuffixToFilename(filename, i + 1);
				allFilenames += ((i > 0) ? ", " : "") + filename;
			}
			PrismMTBDD.ExportVector(stateRewards[i], "c" + (i + 1), allDDRowVars, odd, exportType, filename);
		}
		return (allFilenames.length() > 0) ? allFilenames : null;
	}

	// export transition rewards matrix to a file

	// returns string containing files used if there were more than 1, null otherwise

	public String exportTransRewardsToFile(int exportType, boolean explicit, File file) throws FileNotFoundException, PrismException
	{
		if (numRewardStructs == 0)
			throw new PrismException("There are no transition rewards to export");
		int i;
		String filename, allFilenames = "";
		for (i = 0; i < numRewardStructs; i++) {
			filename = (file != null) ? file.getPath() : null;
			if (filename != null && numRewardStructs > 1) {
				filename = PrismUtils.addCounterSuffixToFilename(filename, i + 1);
				allFilenames += ((i > 0) ? ", " : "") + filename;
			}
			if (!explicit) {
				// can only do explicit (sparse matrix based) export for mdps
			} else {
				PrismSparse.ExportSubMDP(trans, transRewards[i], "C" + (i + 1), allDDRowVars, allDDColVars, allDDNondetVars, odd, exportType, filename);
			}
		}
		return (allFilenames.length() > 0) ? allFilenames : null;
	}

	// clear up (deref all dds, dd vars)

	public void clear()
	{
		super.clear();
		allDDSynchVars.derefAll();
		allDDSchedVars.derefAll();
		allDDChoiceVars.derefAll();
		allDDNondetVars.derefAll();
		JDD.Deref(nondetMask);
		if (transInd != null)
			JDD.Deref(transInd);
		if (transSynch != null)
			for (int i = 0; i < numSynchs; i++) {
				JDD.Deref(transSynch[i]);
			}
	}
}

//------------------------------------------------------------------------------
