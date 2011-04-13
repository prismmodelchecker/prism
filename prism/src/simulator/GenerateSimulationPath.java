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

import prism.*;
import parser.*;
import parser.ast.*;

public class GenerateSimulationPath
{
	// The simulator engine and a log for output
	private SimulatorEngine engine;
	private PrismLog mainLog;

	// Enums
	private enum PathType {
		SIM_PATH_NUM_STEPS, SIM_PATH_TIME, SIM_PATH_DEADLOCK
	};

	// Basic info needed for path
	private ModulesFile modulesFile;
	private Values initialState;
	private int maxPathLength;
	private File file;
	
	// Path configuration options
	private PathType simPathType = null;
	private int simPathLength = 0;
	private double simPathTime = 0.0;
	private String simPathSep = " ";
	private ArrayList<Integer> simVars = null;
	private boolean simLoopCheck = true;
	private int simPathRepeat = 1;

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
	public void generateSimulationPath(ModulesFile modulesFile, Values initialState, String details, int maxPathLength,
			File file) throws PrismException
	{
		this.modulesFile = modulesFile;
		this.initialState = initialState;
		this.maxPathLength = maxPathLength;
		this.file = file;
		parseDetails(details);
		generatePath();
	}

	/**
	 * Parse a string specifying how to generate a simulation path.
	 */
	private void parseDetails(String details) throws PrismException
	{
		String s, ss[];
		int i, j, n;
		boolean done;

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
				j = varList.getIndex(s);
				if (j == -1)
					throw new PrismException("Unknown variable \"" + s + "\" in \"vars=(...)\" list");
				simVars.add(j);
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
			} else {
				// path of fixed number of steps
				simPathType = PathType.SIM_PATH_NUM_STEPS;
				try {
					simPathLength = Integer.parseInt(ss[i]);
					if (simPathLength < 0)
						throw new NumberFormatException();
				} catch (NumberFormatException e) {
					throw new PrismException("Invalid path length \"" + ss[i] + "\"");
				}
			}
		}
		if (simPathType == null)
			throw new PrismException("Invalid path details \"" + details + "\"");
	}

	/**
	 * Generate a random path using the simulator.
	 */
	private void generatePath() throws PrismException
	{
		int i = 0, j = 0;
		boolean done;
		double t = 0.0;
		boolean stochastic = (modulesFile.getModelType() == ModelType.CTMC);

		// print details
		switch (simPathType) {
		case SIM_PATH_NUM_STEPS:
			mainLog.println("\nGenerating random path of length " + simPathLength + " steps...");
			break;
		case SIM_PATH_TIME:
			mainLog.println("\nGenerating random path with time limit " + simPathTime + "...");
			break;
		case SIM_PATH_DEADLOCK:
			mainLog.println("\nGenerating random path until deadlock state...");
			break;
		}

		// display warning if attempt to use "repeat=" option and not "deadlock" option
		if (simPathRepeat > 1 && simPathType != PathType.SIM_PATH_DEADLOCK) {
			simPathRepeat = 1;
			mainLog.println("\nWarning: Ignoring \"repeat\" option - it is only valid when looking for deadlocks.");
		}

		// generate path
		engine.createNewPath(modulesFile);
		for (j = 0; j < simPathRepeat; j++) {
			engine.initialisePath(new State(initialState));
			i = 0;
			t = 0.0;
			done = false;
			while (!done) {
				// generate a single step of path
				// (no need to do any loop detection: this is done below)
				engine.automaticTransition();
				if (stochastic)
					t += engine.getTimeSpentInPathStep(i++);
				else
					t = ++i;
				// check for termination (depending on type)
				switch (simPathType) {
				case SIM_PATH_NUM_STEPS:
					if (i >= simPathLength || engine.queryIsDeadlock())
						done = true;
					break;
				case SIM_PATH_TIME:
					if (t >= simPathTime || i >= maxPathLength || engine.queryIsDeadlock())
						done = true;
					break;
				case SIM_PATH_DEADLOCK:
					if (engine.queryIsDeadlock() || i >= maxPathLength)
						done = true;
					break;
				}
				// stop if a loop was found (and loop checking was not disabled)
				if (simLoopCheck && engine.isPathLooping())
					break;
			}

			// if we are generating multiple paths (to find a deadlock) only stop if deadlock actually found
			if (simPathType == PathType.SIM_PATH_DEADLOCK && engine.queryIsDeadlock())
				break;
		}
		if (j < simPathRepeat)
			j++;

		// display warning if a deterministic loop was detected (but not in case where multiple paths generated)
		if (simLoopCheck && engine.isPathLooping() && simPathRepeat == 1) {
			mainLog.println("\nWarning: Deterministic loop detected after " + i
					+ " steps (use loopcheck=false option to extend path).");
		}

		// if we needed multiple paths to find a deadlock, say how many
		if (simPathRepeat > 1 && j > 1)
			mainLog.println("\n" + j + " paths were generated.");

		// export path
		if (simPathType == PathType.SIM_PATH_DEADLOCK && !engine.queryIsDeadlock()) {
			mainLog.print("\nNo deadlock state found within " + maxPathLength + " steps");
			if (simPathRepeat > 1)
				mainLog.print(" (generated " + simPathRepeat + " paths)");
			mainLog.println(".");
		} else {
			engine.exportPath(file, true, simPathSep, simVars);
		}

		// warning if stopped early
		if (simPathType == PathType.SIM_PATH_TIME && t < simPathTime) {
			mainLog.println("\nWarning: Path terminated before time " + simPathTime + " because maximum path length ("
					+ maxPathLength + ") reached.");
		}
	}
}
