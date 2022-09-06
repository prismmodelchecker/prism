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

import prism.PrismException;
import prism.PrismLangException;
import parser.*;
import pta.parser.PTAParser;

public class PTA
{
	// Info about clocks
	// Note: from an external point of view, clocks are indexed from 1
	// (to allow use of index 0 for the zero clock)
	protected int numClocks;
	protected ArrayList<String> clockNames;
	// Locations info
	protected int numLocations;
	protected ArrayList<Object> locationNames;
	protected ArrayList<LinkedHashSet<Constraint>> invariants;
	protected List<String> locationNameVars;
	// Transitions
	protected int numTransitions;
	protected ArrayList<ArrayList<Transition>> transitions;
	// Alphabet
	protected List<String> alphabet;
	// Maximum clock constraint value
	protected int cMax;

	/**
	 * Constructor: build an empty PTA.
	 * 
	 * @param alphabet The set of (non-tau) actions that can label transitions.
	 * This is *syntactically* defined, e.g., in PRISM it's the list of all actions appearing in commands,
	 * but may be a strict superset of the actions that actually label transitions.
	 */
	public PTA(List<String> alphabet)
	{
		numClocks = 0;
		clockNames = new ArrayList<String>();
		numLocations = 0;
		locationNames = new ArrayList<Object>();
		invariants = new ArrayList<LinkedHashSet<Constraint>>();
		locationNameVars = null;
		numTransitions = 0;
		transitions = new ArrayList<ArrayList<Transition>>();
		this.alphabet = alphabet;
		cMax = 0;
	}

	// Setters

	public int addClock(String name)
	{
		numClocks++;
		clockNames.add(name);
		// Note: returned index starts from 1, not 0
		return numClocks;
	}

	public int getOrAddClock(String name)
	{
		int i = clockNames.indexOf(name);
		if (i == -1) {
			return addClock(name);
		} else
			return i + 1;
	}

	public int addLocation()
	{
		return addLocation(null);
	}

	public int addLocation(Object name)
	{
		numLocations++;
		locationNames.add(name == null ? "L" + (numLocations - 1) : name);
		invariants.add(new LinkedHashSet<Constraint>());
		transitions.add(new ArrayList<Transition>());
		return numLocations - 1;
	}

	public int getOrAddLocation(Object name)
	{
		int i = locationNames.indexOf(name);
		if (i == -1) {
			return addLocation(name);
		} else
			return i;
	}

	public void addInvariantCondition(int loc, Constraint c)
	{
		invariants.get(loc).add(c);
		recomputeMaxClockConstraint(c);
	}

	public void setInvariantConditions(int loc, LinkedHashSet<Constraint> cs)
	{
		invariants.set(loc, cs);
		recomputeMaxClockConstraint(cs);
	}

	/**
	 * Add a new "tau" transition (no action label) to the PTA.
	 * @param loc Source location of the new transition.  
	 */
	public Transition addTransition(int loc)
	{
		return addTransition(loc, null);
	}

	/**
	 * Add a new transition to the PTA.
	 * @param loc Source location of the new transition.  
	 * @param action Action label for the transition  
	 */
	public Transition addTransition(int loc, String action)
	{
		if (action == null)
			action = "";
		Transition transition = new Transition(this, loc, action);
		ArrayList<Transition> list = transitions.get(loc);
		list.add(transition);
		numTransitions++;
		return transition;
	}

	/**
	 * Add a previously created transition to the PTA.
	 */
	public void addTransition(Transition transition)
	{
		transition.setParent(this);
		ArrayList<Transition> list = transitions.get(transition.getSource());
		list.add(transition);
		numTransitions++;
	}

	public void removeTransition(int loc, Transition transition)
	{
		ArrayList<Transition> list = transitions.get(loc);
		if (list.remove(transition))
			numTransitions--;
	}

	public int recomputeMaxClockConstraint(Constraint c)
	{
		cMax = Math.max(cMax, Math.abs(DB.getSignedDiff(c.db)));
		return cMax;
	}

	public int recomputeMaxClockConstraint(Iterable<Constraint> cs)
	{
		for (Constraint c : cs)
			recomputeMaxClockConstraint(c);
		return cMax;
	}

	public void setLocationNameVars(List<String> locationNameVars)
	{
		this.locationNameVars = locationNameVars;
	}
	
	// Getters

	public int getNumClocks()
	{
		return numClocks;
	}

	public String getClockName(int x)
	{
		if (x == 0)
			return "0";
		return clockNames.get(x - 1);
	}

	/**
	 * Return index (starting from 1) of a clock, or -1 if non-existent 
	 */
	public int getClockIndex(String name)
	{
		int i = clockNames.indexOf(name);
		return i == -1 ? -1 : i + 1;
	}

	public Object getLocationName(int loc)
	{
		return locationNames.get(loc);
	}

	public Object getLocationNameString(int loc)
	{
		Object name = locationNames.get(loc);
		if (name instanceof State && locationNameVars != null)
			return ((State)name).toString(locationNameVars);
		else
			return name.toString();
	}

	public List<Object> getLocationNameList()
	{
		return locationNames;
	}

	public int getLocationIndex(Object name)
	{
		return locationNames.indexOf(name);
	}

	public int getNumLocations()
	{
		return numLocations;
	}

	public Iterable<Constraint> getInvariantConstraints(int loc)
	{
		return invariants.get(loc);
	}

	public List<Transition> getTransitions(int loc)
	{
		return transitions.get(loc);
	}

	public Iterable<Transition> getTransitionsByAction(final int loc, final String action)
	{
		return new Iterable<Transition>()
		{
			public Iterator<Transition> iterator()
			{
				return new TransitionsByActionIterator(loc, action);
			}
		};
	}

	public int getMaxClockConstraint()
	{
		return cMax;
	}

	public List<String> getAlphabet()
	{
		return alphabet;
	}

	public boolean isActionInAlphabet(String action)
	{
		return alphabet.contains(action);
	}

	/**
	 * Perform some basic syntactic checks.
	 */
	public void check() throws PrismException
	{
		for (int i = 0; i < numLocations; i++) {
			for (Transition t : getTransitions(i)) {
				t.check();
			}
		}
	}

	public String infoString()
	{
		return numClocks + " clocks, " + numLocations + " locations, " + numTransitions + " transitions";
	}

	public String toString()
	{
		int i;
		String s = "";
		s += "PTA: " + numLocations + " locations, " + numTransitions + " transitions\n";
		s += "  cMax = " + cMax + ", clocks = " + clockNames + "\n";
		s += "   alphabet = " + alphabet + "\n";
		for (i = 0; i < numLocations; i++) {
			s += "Location " + i + " (" + locationNames.get(i) + "):\n";
			s += "  Invariant: " + Constraint.toStringList(this, invariants.get(i)) + "\n";
			for (Transition t : getTransitions(i))
				s += "  " + t + "\n";
		}
		return s;
	}

	public void writeToDesFile(String filename) throws PrismException
	{
		int i;
		try {
			FileWriter out = new FileWriter(filename);
			out.write("#no update for variables\n");
			for (i = 0; i < numLocations; i++) {
				out.write("#" + i + "\n{\n");
				if (i == 0)
					out.write("init\n");
				out.write("\tnode " + getLocationName(i).toString().replace(':', '_') + "; ");
				out.write(Constraint.toStringList(this, invariants.get(i)) + "\n");
				for (Transition t : getTransitions(i))
					t.writeToDesFile(out);
				if (transitions.get(i).isEmpty())
					out.write("\t[\n\t]\n");
				out.write(i == numLocations - 1 ? "*" : "}");
				out.write("\n");
			}
			out.close();
		} catch (IOException e) {
			throw new PrismException("Could not write PTA to file \"" + filename + "\"");
		}
	}

	// Static utility methods

	/**
	 * Combine the names of two PTA locations (e.g. to put in parallel).
	 */
	public static Object combineLocationNames(Object name1, Object name2)
	{
		// Different strategies depending on the types of Object used for names
		// Strings: colon-delimited concatenation
		if (name1 instanceof String && name2 instanceof String) {
			return name1 + ":" + name2;
		}
		// Object arrays: concatenation
		else if (name1 instanceof Object[] && name2 instanceof Object[]) {
			Object[] arr1 = (Object[]) name1;
			Object[] arr2 = (Object[]) name2;
			Object[] arr = new Object[arr1.length + arr2.length];
			int i;
			for (i = 0; i < arr1.length; i++)
				arr[i] = arr1[i];
			for (i = 0; i < arr2.length; i++)
				arr[arr1.length + i] = arr2[i];
			return arr;
		}
		// State objects: concatenation
		else if (name1 instanceof State && name2 instanceof State) {
			return new State((State) name1, (State) name2);
		}
		// Values objects: union
		else if (name1 instanceof Values && name2 instanceof Values) {
			Values vals = new Values((Values) name1);
			vals.addValues((Values) name2);
			return vals;
		}
		// Default: convert to strings, colon-delimited concatenation
		return name1 + ":" + name2;
	}

	/**
	 * Compute the index (in newPta) of a clock name (from oldPta)
	 */
	public static int renameClock(PTA oldPta, PTA newPta, int x)
	{
		int xNew = 0;
		if (x > 0) {
			xNew = newPta.getClockIndex(oldPta.getClockName(x));
			if (xNew == -1)
				System.out.println("Warning: Error renaming clock index " + x);
		}
		return xNew;
	}

	private static PTAParser parser = null;

	/**
	 * Construct a PTA by reading in a 'des' file
	 */
	public static PTA buildPTAFromDesFile(String file)
	{
		PTA pta = null;
		try {
			if (parser == null)
				parser = new PTAParser();
			pta = parser.parsePTA(new FileInputStream(file));
		} catch (PrismLangException e) {
			System.out.println("Error in " + file + ": " + e.getMessage() + ".");
			System.exit(1);
		} catch (FileNotFoundException e) {
			System.out.println(e);
			System.exit(1);
		}
		return pta;
	}

	/**
	 * Build a simple test PTA.
	 */
	public static PTA buildTestPTA()
	{
		PTA pta;
		Transition t;
		Edge e;
		pta = new PTA(Collections.emptyList());
		int x = pta.addClock("x");
		int y = pta.addClock("y");
		pta.addLocation(); // L0
		pta.addLocation(); // L1
		pta.addLocation(); // L2
		pta.addLocation(); // L3
		t = pta.addTransition(0);
		e = t.addEdge(0.5, 1);
		e.addReset(x);
		e = t.addEdge(0.5, 2);
		t = pta.addTransition(1);
		t.addGuardConstraint(Constraint.buildLeq(x, 0));
		t.addGuardConstraint(Constraint.buildLeq(y, 1));
		t.addGuardConstraint(Constraint.buildGeq(y, 0));
		e = t.addEdge(1.0, 3);
		e.addReset(y);
		t = pta.addTransition(2);
		t.addGuardConstraint(Constraint.buildLeq(x, 0));
		t.addGuardConstraint(Constraint.buildLeq(y, 0));
		e = t.addEdge(1.0, 3);
		return pta;
	}

	// Inner class used by getTransitionsByAction

	private class TransitionsByActionIterator implements Iterator<Transition>
	{
		private Iterator<Transition> it;
		private String action;
		private Transition next;

		private TransitionsByActionIterator(int loc, String action)
		{
			it = transitions.get(loc).iterator();
			this.action = action;
			computeNext();
		}

		public boolean hasNext()
		{
			return next != null;
		}

		public Transition next()
		{
			if (next == null)
				throw new NoSuchElementException();
			Transition tr = next;
			computeNext();
			return tr;
		}

		public void remove()
		{
			throw new UnsupportedOperationException();
		}

		private void computeNext()
		{
			next = null;
			while (it.hasNext()) {
				Transition tr = it.next();
				if (tr != null && tr.getAction().equals(action)) {
					next = tr;
					break;
				}
			}
		}
	}
}
