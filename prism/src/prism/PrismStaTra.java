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
import java.util.ArrayList;

// Small utility to merge PRISM states and transition files

public class PrismStaTra
{
	// files to read in from
	private File statesFile;
	private File transFile;

	// states info
	private ArrayList<String> states;

	public PrismStaTra(File sf, File tf)
	{
		statesFile = sf;
		transFile = tf;
	}

	// Read info about reachable state space from file and store string

	public void readStatesFromFile() throws PrismException
	{
		BufferedReader in;
		String s, ss[];
		int lineNum = 0;

		// create arrays for explicit state storage
		states = new ArrayList<String>();
		try {
			// open file for reading
			in = new BufferedReader(new FileReader(statesFile));
			// skip first line
			in.readLine();
			lineNum = 1;
			// read remaining lines
			s = in.readLine();
			lineNum++;
			while (s != null) {
				s = s.trim();
				ss = s.split(":");
				//i = Integer.parseInt(ss[0]);
				states.add(ss[1]);
				s = in.readLine();
				lineNum++;
			}
			// close file
			in.close();
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + statesFile + "\"");
		} catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of states file \"" + statesFile + "\"");
		}
	}

	// Read info about transitions from file, insert state info and re-output

	private void readTransitionsFromFile() throws PrismException
	{
		BufferedReader in;
		String s, ss[];
		int i, r, c, lineNum = 0;

		try {
			// open file for reading
			in = new BufferedReader(new FileReader(transFile));
			// skip first line
			s = in.readLine();
			System.out.println(s);
			lineNum = 1;
			// read remaining lines
			s = in.readLine();
			lineNum++;
			while (s != null) {
				// parse line, split into parts
				s = s.trim();
				ss = s.split(" ");
				// then replace any state indices with state description...
				// dtmc/ctmc with no action:
				if (ss.length == 3) {
					ss[0] = "" + states.get(Integer.parseInt(ss[0]));
					ss[1] = "" + states.get(Integer.parseInt(ss[1]));
				} else if (ss.length == 4) {
					// dtmc/ctmc with action:
					if (ss[3].matches("[_a-zA-Z][_a-zA-Z0-9]*")) {
						ss[0] = "" + states.get(Integer.parseInt(ss[0]));
						ss[1] = "" + states.get(Integer.parseInt(ss[1]));
					}
					// mdp with no action:
					else {
						ss[0] = "" + states.get(Integer.parseInt(ss[0]));
						ss[2] = "" + states.get(Integer.parseInt(ss[2]));
					}
				}
				// mdp with action:
				else {
					ss[0] = "" + states.get(Integer.parseInt(ss[0]));
					ss[2] = "" + states.get(Integer.parseInt(ss[2]));
				}
				// print out modified line
				for (i = 0; i < ss.length; i++) {
					if (i > 0)
						System.out.print(" ");
					System.out.print(ss[i]);
				}
				System.out.println();
				// read next line
				s = in.readLine();
				lineNum++;
			}
			// close file
			in.close();
		} catch (IOException e) {
			throw new PrismException("File I/O error reading from \"" + transFile + "\"");
		} catch (NumberFormatException e) {
			throw new PrismException("Error detected at line " + lineNum + " of transition matrix file \"" + transFile
					+ "\"");
		}
	}

	public static void main(String args[])
	{
		if (args.length < 2) {
			System.out.println("Error: Need two arguments (states and transitions file)");
			System.exit(1);
		}
		try {
			PrismStaTra pst = new PrismStaTra(new File(args[0]), new File(args[1]));
			pst.readStatesFromFile();
			pst.readTransitionsFromFile();
		} catch (PrismException e) {
			System.out.println("Error: " + e.getMessage());
			System.exit(1);
		}
	}
}

//------------------------------------------------------------------------------
