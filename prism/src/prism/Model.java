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
import parser.*;

public interface Model
{
	public static final int DTMC = 1; 
	public static final int CTMC = 2; 
	public static final int MDP = 3; 
	
	int getType();
	String getTypeString();

	int getNumModules();
	String[] getModuleNames();
	String getModuleName(int i);
	
	int getNumVars();
	VarList getVarList();
	String getVarName(int i);
	int getVarIndex(String n);
	int getVarModule(int i);
	int getVarLow(int i);
	int getVarHigh(int i);
	int getVarRange(int i);
	Values getConstantValues();
	String globalToLocal(long x);
	int globalToLocal(long x, int l);
	
	StateList getReachableStates();
	StateList getDeadlockStates();
	StateList getStartStates();
	int getNumRewardStructs();
	long getNumStates();
	long getNumTransitions();
	long getNumStartStates();
	String getNumStatesString();
	String getNumTransitionsString();
	String getNumStartStatesString();

	JDDNode getTrans();
	JDDNode getTrans01();
	JDDNode getStart();
	JDDNode getReach();
	JDDNode getDeadlocks();
	JDDNode getFixedDeadlocks();
	JDDNode getStateRewards();
	JDDNode getStateRewards(int i);
	JDDNode getStateRewards(String s);
	JDDNode getTransRewards();
	JDDNode getTransRewards(int i);
	JDDNode getTransRewards(String s);
	JDDVars[] getVarDDRowVars();
	JDDVars[] getVarDDColVars();
	JDDVars getVarDDRowVars(int i);
	JDDVars getVarDDColVars(int i);
	JDDVars[] getModuleDDRowVars();
	JDDVars[] getModuleDDColVars();
	JDDVars getModuleDDRowVars(int i);
	JDDVars getModuleDDColVars(int i);
	JDDVars getAllDDRowVars();
	JDDVars getAllDDColVars();
	int getNumDDRowVars();
	int getNumDDColVars();
	int getNumDDVarsInTrans();
	Vector getDDVarNames();
	
	ODDNode getODD();

	void doReachability();
	void doReachability(boolean extraReachInfo);
	void skipReachability();
	void filterReachableStates();
	void findDeadlocks();
	void fixDeadlocks();
	void printTrans();
	void printTrans01();
	public void printTransInfo(PrismLog log);
	public void printTransInfo(PrismLog log, boolean extra);
	void exportToFile(int exportType, boolean explicit, File file) throws FileNotFoundException;
	String exportStateRewardsToFile(int exportType, File file) throws FileNotFoundException, PrismException;
	String exportTransRewardsToFile(int exportType, boolean explicit, File file) throws FileNotFoundException, PrismException;

	void clear();
}

//------------------------------------------------------------------------------
