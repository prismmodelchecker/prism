//==============================================================================
//	
//	Copyright (c) 2002-2004, Dave Parker
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
import java.util.HashMap;

import jdd.*;
import mtbdd.*;
import parser.*;

// class to translate an explicit prism model description into an MTBDD model

public class Explicit2MTBDD
{
	// logs
	private PrismLog mainLog;		// main log
	private PrismLog techLog;		// tech log

	// flags/settings
	private boolean doReach = true;	// by default, do reachability (sometimes might want to skip it though)

	// files to read in from
	private File statesFile;
	private File transFile;

	// initial state info
	private String initString;
	private HashMap initVals;

	// ModulesFile object, essentially just to store variable info
	private ModulesFile modulesFile;

	// model info
	
	// type
	private int type;				// model type (prob./nondet./stoch.)
	// vars info
	private int numVars;			// total number of variables
	private String varNames[];		// names of vars
	private	int varMins[];			// min values of vars
	private int varMaxs[];			// max values of vars
	private int varRanges[];		// ranges of vars
	private int varTypes[];			// types of vars
	private VarList varList;		// VarList object to store all var info 
	// explicit storage of states
	private int numStates = 0;
	private int statesArray[][] = null;
	
	// mtbdd stuff
	
	// dds/dd vars - whole system
	private JDDNode trans;			// transition matrix dd
	private JDDNode range;			// dd giving range for system
	private JDDNode trans01;		// 0-1 transition matrix dd
	private JDDNode start;			// dd for start state
	private JDDNode reach;			// dd of reachable states
	private JDDNode deadlocks;		// dd of deadlock states
	private JDDNode stateRewards;	// dd of state rewards
	private JDDNode transRewards;	// dd of transition rewards
	private JDDVars allDDRowVars;		// all dd vars (rows)
	private JDDVars allDDColVars;		// all dd vars (cols)
	private JDDVars allDDSynchVars;		// all dd vars (synchronising actions)
	private JDDVars allDDSchedVars;		// all dd vars (scheduling)
	private JDDVars allDDChoiceVars;	// all dd vars (internal non-det.)
	private JDDVars allDDNondetVars;	// all dd vars (all non-det.)
	// dds/dd vars - modules
	private JDDVars[] moduleDDRowVars;	// dd vars for each module (rows)
	private JDDVars[] moduleDDColVars;	// dd vars for each module (cols)
	private JDDNode[] moduleRangeDDs;	// dd giving range for each module
	private JDDNode[] moduleIdentities;	// identity matrix for each module
	// dds/dd vars - variables
	private JDDVars[] varDDRowVars;		// dd vars (row/col) for each module variable
	private JDDVars[] varDDColVars;
	private JDDNode[] varRangeDDs;		// dd giving range for each module variable
	private JDDNode[] varColRangeDDs;	// dd giving range for each module variable (in col vars)
	private JDDNode[] varIdentities;	// identity matrix for each module variable
	// dds/dd vars - nondeterminism
	private JDDNode[] ddSynchVars;		// individual dd vars for synchronising actions
	private JDDNode[] ddSchedVars;		// individual dd vars for scheduling non-det.
	private JDDNode[] ddChoiceVars;		// individual dd vars for local non-det.
	// names for all dd vars used
	private Vector ddVarNames;
	// flags for keeping track of which variables have been used
	private boolean[] varsUsed;

	private int maxNumChoices = 0;

	// constructor
	
	public Explicit2MTBDD(PrismLog log1, PrismLog log2, File sf, File tf, int t, String is)
	{
		mainLog = log1;
		techLog = log2;
		statesFile = sf;
		transFile = tf;
		// set type at this point
		// if no preference stated, assume default of mdp
		switch (t) {
		case ModulesFile.PROBABILISTIC:
		case ModulesFile.NONDETERMINISTIC:
		case ModulesFile.STOCHASTIC:
			type = t; break;
		default:
			type = ModulesFile.NONDETERMINISTIC; break;
		}
		initString = is;
	}
	
	// set options (generic)
	
	public void setOption(String option, boolean b)
	{
		if (option.equals("doreach")) {
			doReach = b;
		}
		else {
			mainLog.println("\nWarning: option \""+option+"\" not supported by Explicit2MTBDD translator.");
		}
	}
	
	public void setOption(String option, int i)
	{
		mainLog.println("\nWarning: option \""+option+"\" not supported by Explicit2MTBDD translator.");
	}

	public void setOption(String option, String s)
	{
		mainLog.println("\nWarning: option \""+option+"\" not supported by Explicit2MTBDD translator.");
	}

	// build state space
	
	public ModulesFile buildStates() throws PrismException
	{
		Module m;
		Declaration d;
		
		// parse any info about initial state
		parseInitString();
		
		// generate info about variables...
		
		// ...either reading from state list file
		if (statesFile != null) {
			createVarInfoFromStatesFile();
			// in this case, also create explicit table of states
			readStatesFromFile();
		}
		// ...or just creating it from scratch in the trivial case (need transitions file)
		else {
			createVarInfoFromTransFile();
		}
		
		return modulesFile;
	}

	// parse info about initial state
	
	public void parseInitString() throws PrismException
	{
		String ss[], ss2[];
		int i, j;
		
		// create hash map to store var name -> var value mapping
		initVals = new HashMap();
		// parse string
		if (initString == null) return;
		ss = initString.split(",");
		for (i = 0; i < ss.length; i++) {
			ss2 = ss[i].split("=");
			if (ss2.length != 2) throw new PrismException("Badly formatted initial states string");
			if (!ss2[0].matches("[A-Za-z_][A-Za-z_0-9]*")) throw new PrismException("Badly formatted variable name in initial states string");
			if (ss2[1].equals("true")) initVals.put(ss2[0], new Boolean(true));
			else if (ss2[1].equals("false")) initVals.put(ss2[0], new Boolean(false));
			else {
				try {
					j = Integer.parseInt(ss2[1]);
					initVals.put(ss2[0], new Integer(j));
				}
				catch (NumberFormatException e) {
					throw new PrismException("Badly formatted value in initial states string");
				}
			}
		}
	}

	// create info about vars from states file and put into ModulesFile object
	
	public void createVarInfoFromStatesFile() throws PrismException
	{
		BufferedReader in;
		String s, ss[];
		int i, j, lineNum = 0;
		Module m;
		Declaration d;
		Object o;
		
		try {
			// open file for reading
			in = new BufferedReader(new FileReader(statesFile));
			// read first line and extract var names
			s = in.readLine().trim(); lineNum = 1;
			if (s.charAt(0) != '(' || s.charAt(s.length()-1) != ')') throw new PrismException("");
			s = s.substring(1, s.length()-1);
			varNames = s.split(",");
			numVars = varNames.length;
			// create arrays to store info about vars
			varMins = new int[numVars];
			varMaxs = new int[numVars];
			varRanges = new int[numVars];
			varTypes = new int[numVars];
			// read remaining lines
			s = in.readLine(); lineNum++;
			numStates = 0;
			while (s != null) {
				// increment state count
				numStates++;
				// split string
				s = s.trim();
				s = s.substring(s.indexOf('(')+1, s.indexOf(')'));
				ss = s.split(",");
				if (ss.length != numVars) throw new PrismException("");
				// for each variable...
				for (i = 0; i < numVars; i++) {
					// if this is the first state, establish variable type
					if (numStates == 1) {
						if (ss[i].equals("true") || ss[i].equals("false")) varTypes[i] = Expression.BOOLEAN;
						else varTypes[i] = Expression.INT;
					}
					// check for new min/max values (ints only)
					if (varTypes[i] == Expression.INT) {
						j = Integer.parseInt(ss[i]);
						if (numStates == 1) {
							varMins[i] = varMaxs[i] = j;
						} else {
							if (j < varMins[i]) varMins[i] = j;
							if (j > varMaxs[i]) varMaxs[i] = j;
						}
					}
				}
				s = in.readLine(); lineNum++;
			}
			for (i = 0; i < numVars; i++) {
				if (varTypes[i] == Expression.INT) varRanges[i] = varMaxs[i] - varMins[i];
			}
			// close file
			in.close();
		}
		catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + statesFile + "\"");
		}
		catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of states file \"" + statesFile + "\"");
		}
		catch (PrismException e) {
			throw new PrismException("Error detected " + e.getMessage() + "at line " + lineNum + " of states file \"" + statesFile + "\"");
		}
		// create modules file
		modulesFile = new ModulesFile();
		m = new Module("M");
		for (i = 0; i < numVars; i++) {
			if (varTypes[i] == Expression.INT) {
				o = initVals.get(varNames[i]);
				if (o == null)
					d = new Declaration(varNames[i], new ExpressionInt(varMins[i]), new ExpressionInt(varMaxs[i]), new ExpressionInt(varMins[i]));
				else
					d = new Declaration(varNames[i], new ExpressionInt(varMins[i]), new ExpressionInt(varMaxs[i]), new ExpressionInt(((Integer)o).intValue()));
			}
			else {
				o = initVals.get(varNames[i]);
				if (o == null)
					d = new Declaration(varNames[i], new ExpressionFalse());
				else
					d = new Declaration(varNames[i], ((Boolean)o).booleanValue() ? new ExpressionTrue() : new ExpressionFalse());
			}
			m.addDeclaration(d);
		}
		modulesFile.addModule(m);
		modulesFile.tidyUp();
	}

	// create info about vars from trans file and put into ModulesFile object
	
	public void createVarInfoFromTransFile() throws PrismException
	{
		BufferedReader in;
		String s, ss[];
		int lineNum = 0;
		Module m;
		Declaration d;
		Expression init;
		Object o;
		
		try {
			// open file for reading
			in = new BufferedReader(new FileReader(transFile));
			// read first line and extract num states
			s = in.readLine().trim(); lineNum = 1;
			ss = s.split(" ");
			if (ss.length < 2) throw new PrismException("");
			numStates = Integer.parseInt(ss[0]);
			// close file
			in.close();
		}
		catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		}
		catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of transition matrix file \"" + transFile + "\"");
		}
		catch (PrismException e) {
			throw new PrismException("Error detected " + e.getMessage() + "at line " + lineNum + " of transition matrix file \"" + transFile + "\"");
		}
		// determine initial value for variable
		o = initVals.get("x");
		if (o == null) init = new ExpressionInt(0);
		else init = new ExpressionInt(((Integer)o).intValue());
		// create modules file
		modulesFile = new ModulesFile();
		m = new Module("M");
		d = new Declaration("x", new ExpressionInt(0), new ExpressionInt(numStates-1), init);
		m.addDeclaration(d);
		modulesFile.addModule(m);
		modulesFile.tidyUp();
	}

	// read info about reachable state space from file and store explicitly
	
	public void readStatesFromFile() throws PrismException
	{
		BufferedReader in;
		String s, ss[];
		int i, j, k, lineNum = 0;
		double d;
		
		// create arrays for explicit state storage
		statesArray = new int[numStates][];
		try {
			// open file for reading
			in = new BufferedReader(new FileReader(statesFile));
			// skip first line
			in.readLine(); lineNum = 1;
			// read remaining lines
			s = in.readLine(); lineNum++;
			while (s != null) {
				s = s.trim();
				// split into two parts
				ss = s.split(":");
				// determine which state this line describes
				i = Integer.parseInt(ss[0]);
				// now split up middle bit and extract var info
				ss = ss[1].substring(ss[1].indexOf('(')+1, ss[1].indexOf(')')).split(",");
				if (ss.length != numVars) throw new PrismException("(wrong number of variable values) ");
				if (statesArray[i] != null) throw new PrismException("(duplicated state) ");
				statesArray[i] = new int[numVars];
				for (j = 0; j < numVars; j++) {
					if (varTypes[j] == Expression.INT) {
						k = Integer.parseInt(ss[j]);
						statesArray[i][j] = k - varMins[j];
					}
					else {
						if (ss[j].equals("true")) statesArray[i][j] = 1;
						else if (ss[j].equals("false")) statesArray[i][j] = 0;
						else throw new PrismException("(invalid Boolean value \""+ss[j]+"\") ");
					}
				}
				s = in.readLine(); lineNum++;
			}
			// close file
			in.close();
		}
		catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + statesFile + "\"");
		}
		catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of states file \"" + statesFile + "\"");
		}
		catch (PrismException e) {
			throw new PrismException("Error detected " + e.getMessage() + "at line " + lineNum + " of states file \"" + statesFile + "\"");
		}
	}

	// build model
	
	public Model buildModel() throws PrismException
	{
		Model model = null;
		JDDNode tmp, tmp2;
		JDDVars ddv;
		int i;
		
		// get variable info from ModulesFile
		varList = modulesFile.createVarList();
		numVars = varList.getNumVars();
		
		// for an mdp, compute the max number of choices in a state
		if (type == ModulesFile.NONDETERMINISTIC) computeMaxChoicesFromFile();
		
		// allocate dd variables
		allocateDDVars();
		sortDDVars();
		sortIdentities();
		sortRanges();
		
		// construct transition matrix from file
		buildTrans();
		
		// get rid of any nondet dd variables not needed
		if (type == ModulesFile.NONDETERMINISTIC) {
			tmp = JDD.GetSupport(trans);
			tmp = JDD.ThereExists(tmp, allDDRowVars);
			tmp = JDD.ThereExists(tmp, allDDColVars);
			tmp2 = tmp;
			ddv = new JDDVars();
			while (!tmp2.equals(JDD.ONE)) {
				ddv.addVar(JDD.Var(tmp2.getIndex()));
				tmp2 = tmp2.getThen();
			}
			JDD.Deref(tmp);
			allDDNondetVars.derefAll();
			allDDNondetVars = ddv;
		}
		
// 		// print dd variables actually used (support of trans)
// 		mainLog.print("\nMTBDD variables used (" + allDDRowVars.n() + "r, " + allDDRowVars.n() + "c");
// 		if (type == ModulesFile.NONDETERMINISTIC) mainLog.print(", " + allDDNondetVars.n() + "nd");
// 		mainLog.print("):");
// 		tmp = JDD.GetSupport(trans);
// 		tmp2 = tmp;
// 		while (!tmp2.isConstant()) {
// 			//mainLog.print(" " + tmp2.getIndex() + ":" + ddVarNames.elementAt(tmp2.getIndex()));
// 			mainLog.print(" " + ddVarNames.elementAt(tmp2.getIndex()));
// 			tmp2 = tmp2.getThen();
// 		}
// 		mainLog.println();
// 		JDD.Deref(tmp);
		
		// compute state rewards
		computeStateRewards();
		
		// do reachability (or not!)
		if (doReach) {
			doReachability();
		}
		else {
			skipReachability();
		}
		
		// find any deadlocks
		findDeadlocks();
		
		int numModules = 1; // just one module
		String moduleNames[] = modulesFile.getModuleNames(); // whose name is stored here
		Values constantValues = new Values(); // no constants
		
		// create new Model object to be returned
		if (type == ModulesFile.PROBABILISTIC) {
			model = new ProbModel(trans, trans01, start, reach, deadlocks, stateRewards, transRewards, allDDRowVars, allDDColVars, ddVarNames,
						   numModules, moduleNames, moduleDDRowVars, moduleDDColVars,
						   numVars, varList, varDDRowVars, varDDColVars, constantValues);
		}
		else if (type == ModulesFile.NONDETERMINISTIC) {
			model = new NondetModel(trans, trans01, start, reach, deadlocks, stateRewards, transRewards, allDDRowVars, allDDColVars,
						     allDDSynchVars, allDDSchedVars, allDDChoiceVars, allDDNondetVars, ddVarNames,
						     numModules, moduleNames, moduleDDRowVars, moduleDDColVars,
						     numVars, varList, varDDRowVars, varDDColVars, constantValues);
		}
		else if (type == ModulesFile.STOCHASTIC) {
			model = new StochModel(trans, trans01, start, reach, deadlocks, stateRewards, transRewards, allDDRowVars, allDDColVars, ddVarNames,
						    numModules, moduleNames, moduleDDRowVars, moduleDDColVars,
						    numVars, varList, varDDRowVars, varDDColVars, constantValues);
		}
		
		// deref spare dds
		JDD.Deref(moduleIdentities[0]);
		JDD.Deref(moduleRangeDDs[0]);
		for (i = 0; i < numVars; i++) {
			JDD.Deref(varIdentities[i]);
			JDD.Deref(varRangeDDs[i]);
			JDD.Deref(varColRangeDDs[i]);
		}
		JDD.Deref(range);
		if (type == ModulesFile.NONDETERMINISTIC) {
			for (i = 0; i < ddSynchVars.length; i++) {
				JDD.Deref(ddSynchVars[i]);
			}
			for (i = 0; i < ddSchedVars.length; i++) {
				JDD.Deref(ddSchedVars[i]);
			}
			for (i = 0; i < ddChoiceVars.length; i++) {
				JDD.Deref(ddChoiceVars[i]);
			}
		}
		
		return model;
	}

	// for an mdp, compute max number of choices in a state (from transitions file)
	
	public void computeMaxChoicesFromFile() throws PrismException
	{
		BufferedReader in;
		String s, ss[];
		int i, j, lineNum = 0;
		
		try {
			// open file for reading
			in = new BufferedReader(new FileReader(transFile));
			// skip first line
			in.readLine(); lineNum = 1;
			// read remaining lines
			s = in.readLine(); lineNum++;
			maxNumChoices = 0;
			while (s != null) {
				s = s.trim();
				ss = s.split(" ");
				if (ss.length < 4 || ss.length > 5) throw new PrismException("");
				j = Integer.parseInt(ss[1]);
				if (j+1 > maxNumChoices) maxNumChoices = j+1;
				s = in.readLine(); lineNum++;
			}
			// close file
			in.close();
		}
		catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		}
		catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of transition matrix file \"" + transFile + "\"");
		}
		catch (PrismException e) {
			throw new PrismException("Error detected " + e.getMessage() + "at line " + lineNum + " of transition matrix file \"" + transFile + "\"");
		}
	}

	// allocate DD vars for system
	// i.e. decide on variable ordering and request variables from CUDD
	
	private void allocateDDVars()
	{
		JDDNode v, vr, vc;
		int i, j, l, n, last;
		int ddVarsUsed = 0;
		ddVarNames = new Vector();
		
		// create arrays/etc. first
		
		// nondeterministic variables
		if (type == ModulesFile.NONDETERMINISTIC) {
			ddSynchVars = new JDDNode[0];
			ddSchedVars = new JDDNode[0];
			ddChoiceVars = new JDDNode[maxNumChoices];
		}
		// module variable (row/col) vars
		varDDRowVars = new JDDVars[numVars];
		varDDColVars = new JDDVars[numVars];
		for (i = 0; i < numVars; i++) {
			varDDRowVars[i] = new JDDVars();
			varDDColVars[i] = new JDDVars();
		}
		
		// now allocate variables
		
		// allocate nondeterministic variables
		if (type == ModulesFile.NONDETERMINISTIC) {
			for (i = 0; i < maxNumChoices; i++) {
				v = JDD.Var(ddVarsUsed++);
				ddChoiceVars[i] = v;
				ddVarNames.add("l" + i);
			}
		}
		
		// allocate dd variables for module variables (i.e. rows/cols)
		// go through all vars in order (incl. global variables)
		// so overall ordering can be specified by ordering in the input file
		for (i = 0; i < numVars; i++) {
			// get number of dd variables needed
			// (ceiling of log2 of range of variable)
			n = varList.getRangeLogTwo(i);
			// add pairs of variables (row/col)
			for (j = 0; j < n; j++) {
				// new dd row variable
				vr = JDD.Var(ddVarsUsed++);
				// new dd col variable
				vc = JDD.Var(ddVarsUsed++);
				varDDRowVars[i].addVar(vr);
				varDDColVars[i].addVar(vc);
				// add names to list
				ddVarNames.add(varList.getName(i) + "." + j);
				ddVarNames.add(varList.getName(i) + "'." + j);
			}
		}
	}
	
	// sort out DD variables and the arrays they are stored in
	// (more than one copy of most variables is stored)
			
	private void sortDDVars()
	{
		int i;
		
		// put refs for all vars in each module together
		// create arrays
		moduleDDRowVars = new JDDVars[1];
		moduleDDColVars = new JDDVars[1];
		moduleDDRowVars[0] = new JDDVars();
		moduleDDColVars[0] = new JDDVars();
		// go thru all variables
		for (i = 0; i < numVars; i++) {
			varDDRowVars[i].refAll();
			varDDColVars[i].refAll();
			moduleDDRowVars[0].addVars(varDDRowVars[i]);
			moduleDDColVars[0].addVars(varDDColVars[i]);
		}
		
		// put refs for all vars in whole system together
		// create arrays
		allDDRowVars = new JDDVars();
		allDDColVars = new JDDVars();
		if (type == ModulesFile.NONDETERMINISTIC) {
			allDDSynchVars = new JDDVars();
			allDDSchedVars = new JDDVars();
			allDDChoiceVars = new JDDVars();
			allDDNondetVars = new JDDVars();
		}
		// go thru all variables
		for (i = 0; i < numVars; i++) {
			// add to list
			varDDRowVars[i].refAll();
			varDDColVars[i].refAll();
			allDDRowVars.addVars(varDDRowVars[i]);
			allDDColVars.addVars(varDDColVars[i]);
		}
		if (type == ModulesFile.NONDETERMINISTIC) {
			for (i = 0; i < ddChoiceVars.length; i++) {
				// add to list
				JDD.Ref(ddChoiceVars[i]);
				JDD.Ref(ddChoiceVars[i]);
				allDDChoiceVars.addVar(ddChoiceVars[i]);
				allDDNondetVars.addVar(ddChoiceVars[i]);
			}
		}
	}
	
	// sort DDs for identities
	
	private void sortIdentities()
	{
		int i, j;
		JDDNode id;
		
		// variable identities
		varIdentities = new JDDNode[numVars];
		for (i = 0; i < numVars; i++) {
			// set each element of the identity matrix
			id = JDD.Constant(0);
			for (j = 0; j < varList.getRange(i); j++) {
				id = JDD.SetMatrixElement(id, varDDRowVars[i], varDDColVars[i], j, j, 1);
			}
			varIdentities[i] = id;
		}
		// module identities
		moduleIdentities = new JDDNode[1];
		// product of identities for vars in module
		id = JDD.Constant(1);
		for (j = 0; j < numVars; j++) {
			if (varList.getModule(j) == 0) {
				JDD.Ref(varIdentities[j]);
				id = JDD.Apply(JDD.TIMES, id, varIdentities[j]);
			}
		}
		moduleIdentities[0] = id;
	}

	// Sort DDs for ranges
	
	private void sortRanges()
	{
		int i;
		
		// initialise raneg for whole system
		range = JDD.Constant(1);
		
		// variable ranges		
		varRangeDDs = new JDDNode[numVars];
		varColRangeDDs = new JDDNode[numVars];
		for (i = 0; i < numVars; i++) {
			// obtain range dd by abstracting from identity matrix
			JDD.Ref(varIdentities[i]);
			varRangeDDs[i] = JDD.SumAbstract(varIdentities[i], varDDColVars[i]);
			// obtain range dd by abstracting from identity matrix
			JDD.Ref(varIdentities[i]);
			varColRangeDDs[i] = JDD.SumAbstract(varIdentities[i], varDDRowVars[i]);
			// build up range for whole system as we go
			JDD.Ref(varRangeDDs[i]);
			range = JDD.Apply(JDD.TIMES, range, varRangeDDs[i]);
		}
		// module ranges
		moduleRangeDDs = new JDDNode[1];
		// obtain range dd by abstracting from identity matrix
		JDD.Ref(moduleIdentities[0]);
		moduleRangeDDs[0] = JDD.SumAbstract(moduleIdentities[0], moduleDDColVars[0]);
	}

	// construct transition matrix from file
	
	private void buildTrans() throws PrismException
	{
		BufferedReader in;
		String s, ss[];
		int i, j, r, c, k = 0, lineNum = 0;
		double d, x = 0;
		boolean foundReward;
		JDDNode tmp;
		
		// initialise mtbdds
		trans = JDD.Constant(0);
		transRewards = JDD.Constant(0);
		
		try {
			// open file for reading
			in = new BufferedReader(new FileReader(transFile));
			// skip first line
			in.readLine(); lineNum = 1;
			// read remaining lines
			s = in.readLine(); lineNum++;
			while (s != null) {
				foundReward = false;
				// parse line, split into parts
				s = s.trim();
				ss = s.split(" ");
				// case for dtmcs/ctmcs...
				if (type != ModulesFile.NONDETERMINISTIC) {
					if (ss.length < 3 || ss.length > 4) throw new PrismException("");
					r = Integer.parseInt(ss[0]);
					c = Integer.parseInt(ss[1]);
					d = Double.parseDouble(ss[2]);
					if (ss.length == 4) {
						foundReward = true;
						x = Double.parseDouble(ss[3]);
					}
					//System.out.println("("+r+","+c+") = "+d);
				}
				// case for mdps...
				else {
					if (ss.length < 4 || ss.length > 5) throw new PrismException("");
					r = Integer.parseInt(ss[0]);
					k = Integer.parseInt(ss[1]);
					c = Integer.parseInt(ss[2]);
					d = Double.parseDouble(ss[3]);
					if (ss.length == 5) {
						foundReward = true;
						x = Double.parseDouble(ss[4]);
					}
					//System.out.println("("+r+","+k+","+c+") = "+d);
				}
				// construct element of matrix mtbdd
				// case where we don't have a state list...
				if (statesFile == null) {
					/// ...for dtmcs/ctmcs...
					if (type != ModulesFile.NONDETERMINISTIC) {
						tmp = JDD.SetMatrixElement(JDD.Constant(0), varDDRowVars[0], varDDColVars[0], r, c, 1.0);
					}
					/// ...for mdps...
					else {
						tmp = JDD.Set3DMatrixElement(JDD.Constant(0), varDDRowVars[0], varDDColVars[0], allDDChoiceVars, r, c, k, 1.0);
					}
				}
				// case where we do have a state list...
				else {
					tmp = JDD.Constant(1);
					for (i = 0; i < numVars; i++) {
						tmp = JDD.Apply(JDD.TIMES, tmp, JDD.SetVectorElement(JDD.Constant(0), varDDRowVars[i], statesArray[r][i], 1));
						tmp = JDD.Apply(JDD.TIMES, tmp, JDD.SetVectorElement(JDD.Constant(0), varDDColVars[i], statesArray[c][i], 1));
					}
					if (type == ModulesFile.NONDETERMINISTIC) {
						tmp = JDD.Apply(JDD.TIMES, tmp, JDD.SetVectorElement(JDD.Constant(0), allDDChoiceVars, k, 1));
					}
				}
				// add it into mtbdds for transition matrix and transition rewards
				JDD.Ref(tmp);
				trans = JDD.Apply(JDD.PLUS, trans, JDD.Apply(JDD.TIMES, JDD.Constant(d), tmp));
				if (foundReward) {
					JDD.Ref(tmp);
					transRewards = JDD.Apply(JDD.PLUS, transRewards, JDD.Apply(JDD.TIMES, JDD.Constant(x), tmp));
				}
				JDD.Deref(tmp);
				// read next line
				s = in.readLine(); lineNum++;
			}
			// close file
			in.close();
		}
		catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		}
		catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of transition matrix file \"" + transFile + "\"");
		}
		catch (PrismException e) {
			throw new PrismException("Error detected " + e.getMessage() + "at line " + lineNum + " of transition matrix file \"" + transFile + "\"");
		}
	}

	// read info about state rewards from states file
	
	public void computeStateRewards() throws PrismException
	{
		BufferedReader in;
		String s, ss[];
		int i, j, k, lineNum = 0;
		double d;
		JDDNode tmp;
		
		// initialise mtbdd
		stateRewards = JDD.Constant(0);
		
		if (statesFile == null) return;
		
		try {
			// open file for reading
			in = new BufferedReader(new FileReader(statesFile));
			// skip first line
			in.readLine(); lineNum = 1;
			// read remaining lines
			s = in.readLine(); lineNum++;
			while (s != null) {
				s = s.trim();
				// split into two/three parts
				ss = s.split(":");
				// determine which state this line describes
				i = Integer.parseInt(ss[0]);
				// if there is a state reward...
				ss = ss[1].split("=");
				if (ss.length == 2) {
					// determine value
					d = Double.parseDouble(ss[1]);
					// construct element of vector mtbdd
					// case where we don't have a state list...
					if (statesFile == null) {
						tmp = JDD.SetVectorElement(JDD.Constant(0), varDDRowVars[0], i, 1.0);
					}
					// case where we do have a state list...
					else {
						tmp = JDD.Constant(1);
						for (j = 0; j < numVars; j++) {
							tmp = JDD.Apply(JDD.TIMES, tmp, JDD.SetVectorElement(JDD.Constant(0), varDDRowVars[j], statesArray[i][j], 1));
						}
					}
					// add it into mtbdd for state rewards
					stateRewards = JDD.Apply(JDD.PLUS, stateRewards, JDD.Apply(JDD.TIMES, JDD.Constant(d), tmp));
				}
				// read next line
				s = in.readLine(); lineNum++;
			}
			// close file
			in.close();
		}
		catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + statesFile + "\"");
		}
		catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of states file \"" + statesFile + "\"");
		}
	}

	// do reachability
	
	private void doReachability()
	{
		int i;
		JDDNode tmp;
		
		// calculate dd for initial state
		start = JDD.Constant(1);
		for (i = 0; i < numVars; i++) {
			tmp = JDD.SetVectorElement(JDD.Constant(0), varDDRowVars[i], varList.getStart(i)-varList.getLow(i), 1);
			start = JDD.And(start, tmp);
		}
				
		// calculate 0-1 version of trans
		JDD.Ref(trans);
		trans01 = JDD.GreaterThan(trans, 0);
		
		// remove any nondeterminism
		if (type == ModulesFile.NONDETERMINISTIC) {
			JDD.Ref(trans01);
			tmp = JDD.MaxAbstract(trans01, allDDNondetVars);
		}
		else {
			JDD.Ref(trans01);
			tmp = trans01;
		}
		
		// compute reachable states
		mainLog.print("\nComputing reachable states...\n");
		reach = PrismMTBDD.Reachability(tmp, allDDRowVars, allDDColVars, start);
		JDD.Deref(tmp);
		
		// remove non-reachable states from transition matrix
		JDD.Ref(reach);
		trans = JDD.Apply(JDD.TIMES, reach, trans);
		JDD.Ref(reach);
		tmp = JDD.PermuteVariables(reach, allDDRowVars, allDDColVars);
		trans = JDD.Apply(JDD.TIMES, tmp, trans);
		
		// recalculate 0-1 version of trans
		JDD.Deref(trans01);
		JDD.Ref(trans);
		trans01 = JDD.GreaterThan(trans, 0);
		
		// remove non-reachable states from states rewards vector
		JDD.Ref(reach);
		stateRewards = JDD.Apply(JDD.TIMES, reach, stateRewards);
		
		// remove non-reachable states from transition reward matrix
		JDD.Ref(reach);
		transRewards = JDD.Apply(JDD.TIMES, reach, transRewards);
		JDD.Ref(reach);
		tmp = JDD.PermuteVariables(reach, allDDRowVars, allDDColVars);
		transRewards = JDD.Apply(JDD.TIMES, tmp, transRewards);
	}

	// this method allows you to skip the reachability phase
	// there are two versions - one which actually does skip it
	// and one which does it anyway but doesn't filter out the
	// unreachable states from the transition matrix.
	// these are only here for experimental purposes - not general use.
	
	// (do reach but don't filter)

//	private void skipReachability()
//	{
//		int i;
//		JDDNode tmp;
//		
//		// calculate dd for initial state
//		start = JDD.Constant(1);
//		for (i = 0; i < numVars; i++) {
//			tmp = JDD.SetVectorElement(JDD.Constant(0), varDDRowVars[i], varList.getStart(i)-varList.getLow(i), 1);
//			start = JDD.And(start, tmp);
//		}
//				
//		// calculate 0-1 version of trans
//		JDD.Ref(trans);
//		trans01 = JDD.GreaterThan(trans, 0);
//		
//		// remove any nondeterminism
//		if (type == ModulesFile.NONDETERMINISTIC) {
//			JDD.Ref(trans01);
//			tmp = JDD.MaxAbstract(trans01, allDDNondetVars);
//		}
//		else {
//			JDD.Ref(trans01);
//			tmp = trans01;
//		}
//		
//		// compute reachable states
//		mainLog.print("\nComputing reachable states...\n");
//		reach = PrismMTBDD.Reachability(tmp, allDDRowVars, allDDColVars, start);
//		JDD.Deref(tmp);
//	}

//	(no reach and no filter)

	private void skipReachability()
	{
		int i;
		JDDNode tmp;
		
		// calculate dd for initial state
		start = JDD.Constant(1);
		for (i = 0; i < numVars; i++) {
			tmp = JDD.SetVectorElement(JDD.Constant(0), varDDRowVars[i], varList.getStart(i)-varList.getLow(i), 1);
			start = JDD.And(start, tmp);
		}
				
		// calculate 0-1 version of trans
		JDD.Ref(trans);
		trans01 = JDD.GreaterThan(trans, 0);
		
		// don't compute reachable states - assume all reachable
		reach = JDD.Constant(1);
	}

	private void findDeadlocks()
	{
		// find states with at least one transition
		JDD.Ref(trans01);
		deadlocks = JDD.ThereExists(trans01, allDDColVars);
		if (type == ModulesFile.NONDETERMINISTIC) {
			deadlocks = JDD.ThereExists(deadlocks, allDDNondetVars);
		}
		
		// find reachable states with no transitions
		JDD.Ref(reach);
		deadlocks = JDD.And(reach, JDD.Not(deadlocks));
	}
}

//------------------------------------------------------------------------------
