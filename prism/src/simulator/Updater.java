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

package simulator;

import java.util.ArrayList;
import java.util.List;

import parser.*;
import parser.ast.*;
import prism.*;

public class Updater
{
	// Model to which the path corresponds
	protected ModulesFile modulesFile;
	// Model info/stats
	protected int numRewardStructs;
	
	// TODO: apply optimiseForFast or assume called?
	public Updater(ModulesFile modulesFile)
	{
		this.modulesFile = modulesFile;
		numRewardStructs = modulesFile.getNumRewardStructs();
	}
	
	protected void calculateTransitions(State state, TransitionList transitionList)
	{
		int i, j, n, n2;
		int numModules;
		Module module;
		List<Choice> list = null;

		try {
			transitionList.clear();
			System.out.println("Calc updates for " + state);
			numModules = modulesFile.getNumModules();
			for (i = 0; i < numModules; i++) {
				module = modulesFile.getModule(i);
				list = calculateTransitionsForModule(module, state);
				System.out.println(list);
				for (Choice tr : list) {
					transitionList.add(tr);
				}
			}
			
			// For DTMCs, need to randomise
			
			//System.out.println(transitionList.transitionTotal);
			System.out.println(transitionList.transitions);
			//System.out.println(transitionList.transitionIndices);
			//System.out.println(transitionList.transitionOffsets);

		} catch (PrismLangException e) {
			// TODO
		}
	}

	// Model exploration methods (e.g. for simulation)
	
	public List<Choice> calculateTransitionsForModule(Module module, State state) throws PrismLangException
	{
		int i, n;
		Choice tr;
		ArrayList<Choice> res;
		n = module.getNumCommands(); 
		res = new ArrayList<Choice>(n);
		for (i = 0; i < n; i++) {
			Command command = module.getCommand(i);
			if (command.getGuard().evaluateBoolean(state)) {
				tr = calculateTransitionsForUpdates(command.getUpdates(), state);
				tr.setAction(command.getSynch());
				res.add(tr);
			}
		}
		return res;
	}
	
	public Choice calculateTransitionsForUpdates(Updates ups, State state) throws PrismLangException
	{
		int i, n;
		Expression p;
		State newState;
		
		n = ups.getNumUpdates();
		if (n == 1) {
			ChoiceSingleton chSingle = null;
			chSingle = new ChoiceSingleton();
			newState = new State(state);
			calculateTransitionsForUpdate(ups.getUpdate(0), newState);
			chSingle.setTarget(newState);
			p = ups.getProbability(0);
			chSingle.setProbability(p == null ? 1.0 : p.evaluateDouble(state));
			return chSingle;
		} else {
			ChoiceList chList;
			chList = new ChoiceList(n);
			for (i = 0; i < n; i++) {
				newState = new State(state);
				calculateTransitionsForUpdate(ups.getUpdate(i), newState);
				chList.addTarget(newState);
				p = ups.getProbability(i);
				chList.addProbability(p == null ? 1.0 : p.evaluateDouble(state));
			}
			return chList;
		}
	}

	private void calculateTransitionsForUpdate(Update up, State state) throws PrismLangException
	{
		int i, n;
		n = up.getNumElements();
		for (i = 0; i < n; i++) {
			state.varValues[up.getVarIndex(i)] = up.getExpression(i).evaluate(state);
		}
	}

	public void calculateStateRewards(State state, double[] store) throws PrismLangException
	{
		int i, j, n;
		double d;
		RewardStruct rw;
		d = 0.0;
		for (i = 0; i < numRewardStructs; i++) {
			rw = modulesFile.getRewardStruct(i);
			n = rw.getNumItems();
			for (j = 0; j < n; j++) {
				if (!rw.getRewardStructItem(j).isTransitionReward())
					d += rw.getStates(i).evaluateDouble(state);
			}
			store[i] = d;
		}
	}

}
