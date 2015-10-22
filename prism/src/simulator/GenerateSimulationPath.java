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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import parser.State;
import parser.VarList;
import parser.ast.ModulesFile;
import prism.PrismException;
import prism.PrismFileLog;
import prism.PrismLog;
import userinterface.graph.Graph;

public class GenerateSimulationPath
{
	// The simulator engine and a log for output
	private SimulatorEngine engine;
	private PrismLog mainLog;

	// Store warnings
	private List<String> warnings = new ArrayList<String>();

	// Enums
	private enum PathType {
		SIM_PATH_NUM_STEPS, SIM_PATH_TIME, SIM_PATH_DEADLOCK
	};

	// Basic info needed for path
	private ModulesFile modulesFile;
	private State initialState;
	private long maxPathLength;
	private File file;

	// Path configuration options
	private PathType simPathType = null;
	private int simPathLength = 0;
	private double simPathTime = 0.0;
	private String simPathSep = " ";
	private ArrayList<Integer> simVars = null;
	private boolean simLoopCheck = true;
	private int simPathRepeat = 1;
	private boolean simPathShowProbs = false;
	private boolean simPathShowRewards = false;
	private boolean simPathShowChangesOnly = false;
	private boolean simPathSnapshots = false;
	private double simPathSnapshotTime = 0.0;

	public int getNumWarnings()
	{
		return warnings.size();
	}

	public List<String> getWarnings()
	{
		return warnings;
	}

	/**
	 * Send a warning messages to the log;
	 * also, store a copy for later retrieval.
	 */
	private void warning(String msg)
	{
		mainLog.printWarning(msg + ".");
		warnings.add(msg);
	}

	public GenerateSimulationPath(SimulatorEngine engine, PrismLog mainLog)
	{
		this.engine = engine;
		this.mainLog = mainLog;
	}

	/**
	 * Generate and export a random path through a model with the simulator.
	 * @param modulesFile The model
	 * @param initialState Initial state (if null, is selected randomly)
	 * @param details Information about the path to be generated
	 * @param file File to output the path to (stdout if null)
	 */
	public void generateSimulationPath(ModulesFile modulesFile, State initialState, String details, long maxPathLength, File file) throws PrismException
	{
		this.modulesFile = modulesFile;
		this.initialState = initialState;
		this.maxPathLength = maxPathLength;
		this.file = file;
		warnings.clear();

		parseDetails(details);
		PathDisplayer displayer = generateDisplayerForExport();
		if (simPathType == PathType.SIM_PATH_DEADLOCK)
			generateMultiplePaths(displayer);
		else
			generatePath(displayer);
		displayer.close();
	}

	/**
	 * Generate and plot a random path through a model with the simulator.
	 * @param modulesFile The model
	 * @param initialState Initial state (if null, is selected randomly)
	 * @param details Information about the path to be generated
	 */
	public void generateAndPlotSimulationPath(ModulesFile modulesFile, State initialState, String details, long maxPathLength, Graph graphModel)
			throws PrismException
	{
		this.modulesFile = modulesFile;
		this.initialState = initialState;
		this.maxPathLength = maxPathLength;

		parseDetails(details);
		PathDisplayer displayer = generateDisplayerForPlotting(graphModel);
		if (simPathType == PathType.SIM_PATH_DEADLOCK)
			generateMultiplePaths(displayer);
		else
			generatePath(displayer);
	}

	/**
	 * Generate and plot a random path through a model with the simulator, in a separate thread.
	 * @param modulesFile The model
	 * @param initialState Initial state (if null, is selected randomly)
	 * @param details Information about the path to be generated
	 */
	public void generateAndPlotSimulationPathInThread(ModulesFile modulesFile, State initialState, String details, long maxPathLength, Graph graphModel)
			throws PrismException
	{
		new GenerateAndPlotThread(modulesFile, initialState, details, maxPathLength, graphModel).start();
	}

	/**
	 * Parse a string specifying how to generate a simulation path.
	 */
	private void parseDetails(String details) throws PrismException
	{
		String s, ss[];
		int i, j, n;
		boolean done;
		boolean varsOptionGiven = false;
		boolean changesFalseOptionGiven = false;

		ss = details.split(",");
		n = ss.length;
		for (i = 0; i < n; i++) {
			if (ss[i].indexOf("time=") == 0) {
				// path with upper time limit
				simPathType = PathType.SIM_PATH_TIME;
				try {
					simPathTime = Double.parseDouble(ss[i].substring(5));
					if (simPathTime < 0.0)
						throw new NumberFormatException();
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid path time limit \"" + ss[i] + "\"");
				}
			} else if (ss[i].equals("deadlock")) {
				// path until deadlock
				simPathType = PathType.SIM_PATH_DEADLOCK;
			} else if (ss[i].indexOf("sep=") == 0) {
				// specify column separator to display path
				simPathSep = ss[i].substring(4);
				if (simPathSep.equals("space")) {
					simPathSep = " ";
					continue;
				}
				if (simPathSep.equals("tab")) {
					simPathSep = "\t";
					continue;
				}
				if (simPathSep.equals("comma")) {
					simPathSep = ",";
					continue;
				}
				throw new PrismException("Separator must be one of: \"space\", \"tab\", \"comma\"");
			} else if (ss[i].indexOf("vars=") == 0) {
				varsOptionGiven = true;
				// Build list of indices of variables to display
				VarList varList = modulesFile.createVarList();
				simVars = new ArrayList<Integer>();
				done = false;
				s = ss[i].substring(5);
				if (s.length() < 1 || s.charAt(0) != '(')
					throw new PrismException("Invalid format for \"vars=(...)\"");
				s = s.substring(1);
				if (s.indexOf(')') > -1) {
					s = s.substring(0, s.length() - 1);
					done = true;
				}
				if (s.length() > 0) {
					j = varList.getIndex(s);
					if (j == -1)
						throw new PrismException("Unknown variable \"" + s + "\" in \"vars=(...)\" list");
					simVars.add(j);
				}
				while (i < n && !done) {
					s = ss[++i];
					if (s.indexOf(')') > -1) {
						s = s.substring(0, s.length() - 1);
						done = true;
					}
					j = varList.getIndex(s);
					if (j == -1)
						throw new PrismException("Unknown variable \"" + s + "\" in \"vars=(...)\" list");
					simVars.add(j);
				}
			} else if (ss[i].indexOf("loopcheck=") == 0) {
				// switch loop detection on/off (default is on)
				s = ss[i].substring(10);
				if (s.equals("true")) {
					simLoopCheck = true;
					continue;
				}
				if (s.equals("false")) {
					simLoopCheck = false;
					continue;
				}
				throw new PrismException("Value for \"loopcheck\" flag must be \"true\" or \"false\"");
			} else if (ss[i].indexOf("repeat=") == 0) {
				// how many times to repeat path generation until successful (for "deadlock" option)
				try {
					simPathRepeat = Integer.parseInt(ss[i].substring(7));
					if (simPathRepeat < 1)
						throw new NumberFormatException();
				} catch (NumberFormatException e) {
					throw new PrismException("Value for \"repeat\" option must be a positive integer");
				}
			} else if (ss[i].indexOf("snapshot=") == 0) {
				// print timed snapshots of path
				try {
					simPathSnapshots = true;
					simPathSnapshotTime = Double.parseDouble(ss[i].substring(9));
					if (simPathSnapshotTime <= 0)
						throw new NumberFormatException();
				} catch (NumberFormatException e) {
					throw new PrismException("Value for \"snapshot\" option must be a positive double");
				}
			} else if (ss[i].indexOf("probs=") == 0) {
				// display probabilities/rates?
				String bool = ss[i].substring(6).toLowerCase();
				if (bool.equals("true"))
					simPathShowProbs = true;
				else if (bool.equals("false"))
					simPathShowProbs = false;
				else
					throw new PrismException("Value for \"rewards\" option must \"true\" or \"false\"");
			} else if (ss[i].indexOf("rewards=") == 0) {
				// display rewards?
				String bool = ss[i].substring(8).toLowerCase();
				if (bool.equals("true"))
					simPathShowRewards = true;
				else if (bool.equals("false"))
					simPathShowRewards = false;
				else
					throw new PrismException("Value for \"rewards\" option must \"true\" or \"false\"");
			} else if (ss[i].indexOf("changes=") == 0) {
				// display changes only?
				String bool = ss[i].substring(8).toLowerCase();
				if (bool.equals("true")) {
					simPathShowChangesOnly = true;
				} else if (bool.equals("false")) {
					changesFalseOptionGiven = true;
					simPathShowChangesOnly = false;
				}
				else
					throw new PrismException("Value for \"changes\" option must \"true\" or \"false\"");
			} else {
				// path of fixed number of steps
				simPathType = PathType.SIM_PATH_NUM_STEPS;
				try {
					simPathLength = Integer.parseInt(ss[i]);
					if (simPathLength < 0)
						throw new NumberFormatException();
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid path option \"" + ss[i] + "\"");
				}
			}
		}
		if (simPathType == null)
			throw new PrismException("Invalid path details \"" + details + "\"");

		// Be default, set changes=true if vars=() was specified
		if (varsOptionGiven && !changesFalseOptionGiven) {
			simPathShowChangesOnly = true;
		}
		
		// Display warning if attempt to use "repeat=" option and not "deadlock" option
		if (simPathRepeat > 1 && simPathType != PathType.SIM_PATH_DEADLOCK) {
			simPathRepeat = 1;
			mainLog.printWarning("Ignoring \"repeat\" option - it is only valid when looking for deadlocks.");
		}
	}

	/**
	 * Print bulleted list of options to a log (used by -help switch). 
	 */
	public static void printOptions(PrismLog mainLog)
	{
		mainLog.println(" * <n> - generate a path of <n> steps");
		mainLog.println(" * time=<x> - generate a path of at least <x> time units");
		mainLog.println(" * deadlock - generate a path until a deadlock is reached");
		mainLog.println(" * repeat=<n> - try <n> paths until a deadlock is found");
		
		mainLog.println(" * sep=<val> - use <val> as column separator (space, tab, comma)");
		mainLog.println(" * vars=<x1,x2,...> - show values for variables x1,x2,.. only");
		mainLog.println(" * loopcheck=<true|false> - whether to detect deterministic loops");
		mainLog.println(" * snapshot=<x> - view states at fixed timepoints of interval <x>");
		mainLog.println(" * probs=<true|false> - display probability (or rate) of transitions taken");
		mainLog.println(" * rewards=<true|false> - display state/transition rewards");
		mainLog.println(" * changes=<true|false> - only display states where displayed variables change");
	}

	/**
	 * Create a PathDisplayer object for file export
	 */
	private PathDisplayer generateDisplayerForExport() throws PrismException
	{
		PrismLog log;
		PathToText displayer;

		if (file != null) {
			log = new PrismFileLog(file.getPath());
			if (!log.ready()) {
				throw new PrismException("Could not open file \"" + file + "\" for output");
			}
		} else {
			log = mainLog;
		}
		displayer = new PathToText(log, modulesFile);
		displayer.setColSep(simPathSep);
		displayer.setVarsToShow(simVars);
		displayer.setShowProbs(simPathShowProbs);
		displayer.setShowRewards(simPathShowRewards);
		displayer.setShowChangesOnly(simPathShowChangesOnly);
		if (simPathSnapshots)
			displayer.setToShowSnapShots(simPathSnapshotTime);

		return displayer;
	}

	/**
	 * Create a PathDisplayer object for graph plotting
	 */
	private PathDisplayer generateDisplayerForPlotting(Graph graphModel) throws PrismException
	{
		PathToGraph displayer;

		displayer = new PathToGraph(graphModel, modulesFile);
		displayer.setVarsToShow(simVars);
		displayer.setShowProbs(simPathShowProbs);
		displayer.setShowRewards(simPathShowRewards);
		displayer.setShowChangesOnly(simPathShowChangesOnly);
		if (simPathSnapshots)
			displayer.setToShowSnapShots(simPathSnapshotTime);

		return displayer;
	}

	/**
	 * Generate a random (on-the-fly) path using the simulator.
	 */
	private void generatePath(PathDisplayer displayer) throws PrismException
	{
		Path path = null;
		long i = 0;
		boolean done;

		// Print details
		switch (simPathType) {
		case SIM_PATH_NUM_STEPS:
			mainLog.println("\nGenerating random path of length " + simPathLength + " steps...");
			break;
		case SIM_PATH_TIME:
			mainLog.println("\nGenerating random path with time limit " + simPathTime + "...");
			break;
		}
		if (displayer instanceof PathToText && file == null)
			mainLog.println();

		// Create path
		engine.createNewOnTheFlyPath(modulesFile);
		// Build path
		path = engine.getPath();
		engine.initialisePath(initialState);
		displayer.start(path.getCurrentState(), path.getCurrentStateRewards());
		i = 0;
		done = false;
		while (!done) {
			// Generate a single step of path
			engine.automaticTransition();
			i++;
			if (simPathType != PathType.SIM_PATH_DEADLOCK) {
				displayer.step(path.getTimeInPreviousState(), path.getTotalTime(), path.getPreviousModuleOrAction(), path.getPreviousProbability(),
						path.getPreviousTransitionRewards(), path.size(), path.getCurrentState(), path.getCurrentStateRewards());
			}
			// Check for termination (depending on type)
			switch (simPathType) {
			case SIM_PATH_NUM_STEPS:
				if (i >= simPathLength || engine.queryIsDeadlock())
					done = true;
				break;
			case SIM_PATH_TIME:
				if (path.getTotalTime() >= simPathTime || i >= maxPathLength || engine.queryIsDeadlock())
					done = true;
				break;
			}
			// Stop if a loop was found (and loop checking was not disabled)
			if (simLoopCheck && engine.isPathLooping())
				break;
		}
		displayer.end();

		// Display warnings if needed
		if (simLoopCheck && engine.isPathLooping()) {
			warning("Deterministic loop detected after " + engine.getPathSize() + " steps (use loopcheck=false option to extend path)");
		}
		if (simPathType == PathType.SIM_PATH_TIME && path.getTotalTime() < simPathTime) {
			warning("Path terminated before time " + simPathTime + " because maximum path length (" + maxPathLength + ") was reached");
		}

		// Print summary of path
		mainLog.print("\nGenerated path: " + path.size() + " step" + (path.size() == 1 ? "" : "s"));
		if (modulesFile.getModelType().continuousTime()) {
			mainLog.print(", total time " + path.getTotalTime());
		}
		if (file != null) {
			mainLog.println(" (exported to " + file + ")");
		} else {
			mainLog.println();
		}
	}

	/**
	 * Generate multiple random paths using the simulator.
	 * Note: these are not on-the-fly paths since we don't in advance if they are to be displayed.
	 */
	private void generateMultiplePaths(PathDisplayer displayer) throws PrismException
	{
		Path path = null;
		int i = 0, j = 0;
		boolean done;

		// Print details
		switch (simPathType) {
		case SIM_PATH_DEADLOCK:
			mainLog.println("\nGenerating random path(s) until deadlock state...");
			break;
		}

		// Create path
		engine.createNewPath(modulesFile);
		// Build path
		for (j = 0; j < simPathRepeat; j++) {
			path = engine.getPath();
			engine.initialisePath(initialState);
			i = 0;
			done = false;
			while (!done) {
				// Generate a single step of path
				engine.automaticTransition();
				i++;
				// Check for termination (depending on type)
				switch (simPathType) {
				case SIM_PATH_DEADLOCK:
					if (engine.queryIsDeadlock() || i >= maxPathLength)
						done = true;
					break;
				}
				// Stop if a loop was found (and loop checking was not disabled)
				if (simLoopCheck && engine.isPathLooping())
					break;
			}
			// Stop generating paths if done
			if (engine.queryIsDeadlock())
				break;
		}
		if (j < simPathRepeat)
			j++;

		// Bail out if we didn't build a suitable path 
		if (!engine.queryIsDeadlock()) {
			mainLog.print("\nNo deadlock state found within " + maxPathLength + " steps");
			if (simPathRepeat > 1)
				mainLog.print(" (generated " + simPathRepeat + " paths)");
			mainLog.println(".");
			return;
		}

		// Display path
		if (file == null)
			mainLog.println();
		engine.getPathFull().display(displayer);

		// Print summary of path(s)
		if (simPathRepeat > 1 && j > 1)
			mainLog.print("\nGenerated " + j + " paths. Final path: ");
		else
			mainLog.print("\nGenerated path: ");
		mainLog.print(path.size() + " steps");
		if (modulesFile.getModelType().continuousTime()) {
			mainLog.print(", total time " + path.getTotalTime());
		}
		if (file != null) {
			mainLog.println(" (exported to " + file + ")");
		} else {
			mainLog.println();
		}
	}

	class GenerateAndPlotThread extends Thread
	{
		private ModulesFile modulesFile;
		private parser.State initialState;
		private String details;
		private long maxPathLength;
		private Graph graphModel;

		public GenerateAndPlotThread(ModulesFile modulesFile, parser.State initialState, String details, long maxPathLength, Graph graphModel)
		{
			this.modulesFile = modulesFile;
			this.initialState = initialState;
			this.details = details;
			this.maxPathLength = maxPathLength;
			this.graphModel = graphModel;
		}

		public void run()
		{
			try {
				generateAndPlotSimulationPath(modulesFile, initialState, details, maxPathLength, graphModel);
			} catch (PrismException e) {
				// Just report errors passively to log
				mainLog.printWarning("Error occured during path plot: " + e.getMessage());
			}
		}
	}
}
