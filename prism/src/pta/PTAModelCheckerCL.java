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

package pta;

import java.io.*;
import java.util.*;

import explicit.MDP;
import explicit.MDPModelChecker;

import prism.PrismException;

/**
 * Command-line interface to PTA model checker.
 */
public class PTAModelCheckerCL
{
	// Inputs

	// List of PTA files
	ArrayList<String> ptaFiles;
	// Target info
	String targetLocString, targetConstraintString;

	// Flags, settings

	// Compute min probabilities? (as opposed to max)
	boolean min = false;
	// Export the parallel composition PTA after construction?
	boolean exportPTA = false;
	// Do MDP forward reach rather than abstraction-refinement?
	boolean mdpReach = false;

	// Processed target info
	String targetLocRegexp;
	Constraint targetConstraint;
	BitSet targetLocs;
	// PTAs
	PTA pta, pta2;
	// Abstraction refinement object
	PTAAbstractRefine abstractRefine;

	/**
	 * Main method
	 */
	public static void main(String args[])
	{
		new PTAModelCheckerCL().go(args);
	}

	/**
	 * Main method
	 */
	public void go(String args[])
	{
		int i, n, z;
		boolean found;

		try {

			// Build PTAAbstractRefine object
			abstractRefine = new PTAAbstractRefine(null);

			// Parse command-line arguments
			parseCommandLineArgs(args);
			
			// Process target info
			// Strip any accidental quotes
			targetLocString = targetLocString.replaceAll("['\"]", "");
			targetConstraintString = targetConstraintString.replaceAll("['\"]", "");
			// Build regexp for target location spec 
			String targetLocStringSplit[] = targetLocString.split(",");
			targetLocRegexp = "";
			for (String targetLocStringBit : targetLocStringSplit) {
				if (!("".equals(targetLocRegexp)))
					targetLocRegexp += "|";
				targetLocRegexp += "(" + targetLocStringBit.replaceAll("\\*", "[a-zA-Z0-9_]*") + ")";

			}
			//System.out.print("Target locations regexp: " + targetLocRegexp);

			// Display command-line arguments and settings:
			System.out.print("Command:");
			for (i = 0; i < args.length; i++)
				System.out.print(" " + args[i]);
			System.out.println();
			abstractRefine.printSettings();
			
			// Expand any input files that are lists of PTAs
			processPTAFiles();
			
			// Build PTA
			System.out.println("\nBuilding PTA from \"" + ptaFiles.get(0) + "\"");
			pta = PTA.buildPTAFromDesFile(ptaFiles.get(0));
			System.out.println(pta.infoString());
			//System.out.println(pta);
			for (i = 1; i < ptaFiles.size(); i++) {
				System.out.println("Building PTA from \"" + ptaFiles.get(i) + "\"");
				pta2 = PTA.buildPTAFromDesFile(ptaFiles.get(i));
				System.out.println(pta2.infoString());
				//System.out.println(pta2);
				pta = new PTAParallel().compose(pta, pta2);
				//System.out.println(pta);
			}
			System.out.println("Final PTA: " + pta.infoString());
			//System.out.println(pta);
			// Do some checks on PTA
			pta.check();
			// Export PTA if required
			if (exportPTA)
				pta.writeToDesFile("par.des");
			
			// Parse target clock constraint
			if (targetConstraintString.equals("true")) {
				targetConstraint = null;
			} else {
				z = pta.getOrAddClock(targetConstraintString.substring(0, 1));
				if (targetConstraintString.indexOf(">=", 1) != -1)
					targetConstraint = Constraint.buildGeq(z, Integer.parseInt(targetConstraintString.substring(3)));
				else if (targetConstraintString.indexOf("<=", 1) != -1)
					targetConstraint = Constraint.buildLeq(z, Integer.parseInt(targetConstraintString.substring(3)));
				else if (targetConstraintString.indexOf(">", 1) != -1)
					targetConstraint = Constraint.buildGt(z, Integer.parseInt(targetConstraintString.substring(2)));
				else if (targetConstraintString.indexOf("<", 1) != -1)
					targetConstraint = Constraint.buildLt(z, Integer.parseInt(targetConstraintString.substring(2)));
				else
					targetConstraint = null;
			}
			// Determine PTA locations satisfying target specification; check non-empty
			n = pta.getNumLocations();
			targetLocs = new BitSet(n);
			found = false;
			for (i = 0; i < n; i++) {
				// Since we build from des files here, PTA names are strings
				if (((String) pta.getLocationName(i)).matches(targetLocRegexp)) {
					targetLocs.set(i);
					found |= true;
				}
			}
			if (!found)
				throw new PrismException("No matches for PTA target location specification " + targetLocString);

			// Display final target info
			System.out.print("\nTarget locations: " + targetLocString);
			System.out.println(" (" + targetLocs.cardinality() + " locations)");
			System.out.println("Target constraint: "
					+ (targetConstraint == null ? "true" : targetConstraint.toString(pta)));

			// Do forward reach
			// Default case: use game-based abstraction refinement
			if (!mdpReach) {
				abstractRefine.forwardsReachAbstractRefine(pta, targetLocs, targetConstraint, min);
			}
			// But we can also just do standard MDP-based forwards reachability
			else {
				ForwardsReach forwardsReach = new ForwardsReach();
				ReachabilityGraph graph = forwardsReach.buildForwardsGraph(pta, targetLocs, targetConstraint);
				MDP mdp = graph.buildMDP(forwardsReach.getInitialStates());
				new MDPModelChecker(null).computeReachProbs(mdp, forwardsReach.getTarget(), min);
			}

		} catch (PrismException e) {
			System.err.println("\nError: " + e.getMessage());
			System.exit(1);
		}
	}

	/**
	 * Parse command-line arguments
	 */
	private void parseCommandLineArgs(String args[]) throws PrismException
	{
		int i;
		String s;

		if (args.length < 3) {
			System.err.println("Usage: ptamc [options] <des files> <target loc> <target constraint>");
			System.exit(1);
		}

		ptaFiles = new ArrayList<String>();
		for (i = 0; i < args.length; i++) {
			s = args[i];
			// Process a switch
			if (s.charAt(0) == '-') {
				s = s.substring(1);
				
				// Local options
				if (s.equals("min")) {
					min = true;
				} else if (s.equals("max")) {
					min = false;
				} else if (s.equals("exportpta")) {
					exportPTA = true;
				} else if (s.equals("mdp")) {
					mdpReach = true;
				}
				
				// Otherwise, try passing to abstraction-refinement engine
				else {
					abstractRefine.parseOption(s);
				}
			}
			// If not a switch, must be a PTA filename
			else {
				ptaFiles.add(s);
			}
		}
		// Last two arguments are actually target info, not PTA files
		targetLocString = ptaFiles.get(ptaFiles.size() - 2);
		targetConstraintString = ptaFiles.get(ptaFiles.size() - 1);
		ptaFiles.remove(ptaFiles.size() - 1);
		ptaFiles.remove(ptaFiles.size() - 1);
	}
	
	/**
	 * Expand any input files that are lists of PTAs. 
	 */
	private void processPTAFiles() throws PrismException
	{
		String s;
		
		ArrayList<String> ptaFilesNew = new ArrayList<String>();
		for (String ptaFile : ptaFiles) {
			if (ptaFile.endsWith(".deslist")) {
				try {
					BufferedReader in = new BufferedReader(new FileReader(new File(ptaFile)));
					while ((s = in.readLine()) != null) {
						s = s.trim();
						if (s.equals(""))
							continue;
						// Absolute filename
						if (new File(s).isAbsolute()) {
							ptaFilesNew.add(s);
						}
						// Relative filename
						else if (new File(ptaFile).getParent() != null) {
							ptaFilesNew.add(new File(ptaFile).getParent() + File.separator + s);
						}
						// Current directory
						else {
							ptaFilesNew.add(s);
						}
					}
				} catch (IOException e) {
					throw new PrismException("Could not read from file \"" + ptaFile + "\"");
				}
			} else {
				ptaFilesNew.add(ptaFile);
			}
		}
		ptaFiles = ptaFilesNew;
	}
}
