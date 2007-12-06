//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford)
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

import java.util.*;
import parser.*;
import userinterface.graph.*;

/**
 * This class stores the results of experiments. It should be unaware what is being done with the results,
 * for instance the plotting of the results.
 * @author  ug60axh, mxk
 */
public class ResultsCollection
{
	// Info about the constants over which these results range
	private Vector<DefinedConstant> rangingConstants;
	
	// Storage of the actual results
	private TreeNode root;
	private int currentIteration = 0;
	private boolean anyErrors = false;
	
	// Listeners to results of this ResultCollection
	private Vector<ResultListener> resultListeners;	
	
	// the "name" of the result (used for y-axis of any graphs plotted)
	private String resultName;
	
	/** Creates a new instance of ResultsCollection */
	public ResultsCollection(UndefinedConstants uCons) 
	{ 
		this(uCons, null); 
	}
	
	public ResultsCollection(UndefinedConstants uCons, String resultName)
	{
		resultListeners = new Vector<ResultListener>();
		rangingConstants = new Vector<DefinedConstant>();
		
		/* TODO: Fix this when/if getRangingConstants() returns Vector<DefinedConstant> */
		Vector tmpRangingConstants = uCons.getRangingConstants();		
		for (int i = 0; i < tmpRangingConstants.size(); i++)
		{
			rangingConstants.add((DefinedConstant)tmpRangingConstants.get(i));
		}
		
		this.root = (rangingConstants.size() > 0) ? new TreeNode(0) : new TreeLeaf();		
		this.resultName = (resultName == null) ? "Result" : resultName;
	}
	
	public Vector<DefinedConstant> getRangingConstants() 
	{ 
		return rangingConstants; 
	}
	
	public int getNumRangingConstants() 
	{
		return rangingConstants.size(); 
	}	
	
	public boolean addResultListener(ResultListener resultListener) 
	{
		return resultListeners.add(resultListener);
	}

	public boolean removeResultListener(ResultListener resultListener) 
	{
		return resultListeners.removeElement(resultListener);
	}

	public int getCurrentIteration() 
	{ 
		return currentIteration; 
	}
	
	public String getResultName() 
	{ 
		return resultName; 
	}
	
	/** Sets the result for a particular set of values. */	
	public int setResult(Values values, Object result) throws PrismException
	{		
		// store result
		int ret = root.setResult(values, result);
		
		// notify listeners
		for(int i = 0; i < resultListeners.size(); i++)
		{	resultListeners.get(i).notifyResult(this, values, result);	}
				
		// modify counters/flags as appropriate
		currentIteration += ret;
		if (result instanceof Exception) anyErrors = true;
		
		return ret;
	}

	/** Sets the result for a particular set of values. */	
	public int setResult(Values mfValues, Values pfValues, Object result) throws PrismException
	{
		// merge mfValues and pfValues
		Values merged = new Values();
		if (mfValues != null) merged.addValues(mfValues);
		if (pfValues != null) merged.addValues(pfValues);
		
		return setResult(merged, result);	
	}

	
	/** Sets the result to an error for a particular set of values.
	  * If any constants are left undefined, the same error will be set for all values of each constant.
	  * Returns the total number of values which were set for the the first time.
	  * Note: individual errors can be set using setResult(). That method could easily be adapted to store
	  * multiple values but the DisplayableData aspect isn't sorted yet. */
	
	public int setMultipleErrors(Values values, Exception error) throws PrismException
	{
		// store result
		int ret = root.setResult(values, error);
		
		// modify counters/flags as appropriate
		currentIteration += ret;
		anyErrors = true;
		
		return ret;
	}
	
	/** Sets the result to an error for a particular set of values.
	  * If any constants are left undefined, the same error will be set for all values of each constant.
	  * Returns the total number of values which were set for the the first time.
	  * Note: individual errors can be set using setResult(). That method could easily be adapted to store
	  * multiple values but the DisplayableData aspect isn't sorted yet. */
	
	public int setMultipleErrors(Values mfValues, Values pfValues, Exception error) throws PrismException
	{
		// merge mfValues and pfValues
		Values merged = new Values();
		if (mfValues != null) merged.addValues(mfValues);
		if (pfValues != null) merged.addValues(pfValues);
		
		return setMultipleErrors(merged, error);
	}

	/** Access a stored result */	
	public Object getResult(Values val) throws PrismException
	{
		return root.getResult(val);
	}

	/** See if there were any errors */	
	public boolean containsErrors()
	{
		return anyErrors;
	}
		
	/** Create array of headings */
	public String[] getHeadingsArray()
	{
		int i;
		String res[] = new String[rangingConstants.size()+1];
		
		// create header
		for (i = 0; i < rangingConstants.size(); i++) {
			res[i] = ((DefinedConstant)rangingConstants.elementAt(i)).getName();
		}
		res[rangingConstants.size()] = "Result";
		
		return res;
	}

	/** Create ArrayList based repesentation of the data */
	public ArrayList toArrayList()
	{
		return root.toArrayList();
	}

	/** Create string representation of the data */
	public String toString()
	{
		return toString(false, ",", ",", true);
	}

	/** Create string representation of the data */
	public String toString(boolean pv, String sep, String eq)
	{
		return toString(pv, sep, eq, true);
	}

	/** Create string representation of the data
	  * @param pv Print the variables in each row?
	  * @param sep String for separating values
	  * @param eq String for seperating values and result
	  * @param header Add a header? */
	public String toString(boolean pv, String sep, String eq, boolean header)
	{
		int i;
		String s = "";
		
		// if there are no variables, override eq separator
		if (rangingConstants.size() == 0) eq = "";
		// create header
		if (header) {
			for (i = 0; i < rangingConstants.size(); i++) {
				if (i > 0) s += sep;
				s += ((DefinedConstant)rangingConstants.elementAt(i)).getName();
			}
			s += eq + "Result\n";
		}
		// create table
		s += root.toString(pv, sep, eq);
		
		return s;
	}

	/** Create string representation of the data for a partial evaluation
	  * @param partial Values for a subset of the constants
	  * @param pv Print the variables in each row?
	  * @param sep String for separating values
	  * @param eq String for seperating values and result
	  * @param header Add a header showing the constant names? */
	public String toStringPartial(Values partial, boolean pv, String sep, String eq, boolean header) throws PrismException
	{
		int i;
		String s = "", name;
		boolean first, noVars;
		
		// use an empty Values object if it is null
		if (partial == null) partial = new Values();
		// if there are no variables, override eq separator
		noVars = true;
		for (i = 0; i < rangingConstants.size(); i++) {
			if (!partial.contains(((DefinedConstant)rangingConstants.elementAt(i)).getName())) {
				noVars = false;
				break;
			}
		}
		if (noVars) eq = "";
		// create header
		if (header) {
			first = true;
			for (i = 0; i < rangingConstants.size(); i++) {
				name = ((DefinedConstant)rangingConstants.elementAt(i)).getName();
				// only print constants for which we haven't been given values
				if (!partial.contains(name)) {
					if (!first) s += sep;
					s += name;
					first = false;
				}
			}
			s += eq + "Result\n";
		}
		// create table
		s += root.toStringPartial(partial, pv, sep, eq);
		
		return s;
	}

	// data structure to store result collection (internal classes)

	private class TreeNode
	{
		private int level;
		private DefinedConstant constant;
		private TreeNode kids[];

		/** Empty constructor */ /* Required by subclass */
		public TreeNode() { }

		/** Actual constructor (recursive) */
		public TreeNode(int l)
		{
			int i, n;
			
			// store level and create children
			level = l;
			constant = (DefinedConstant)rangingConstants.get(level);
			n = constant.getNumSteps();
			kids = new TreeNode[n];
			for (i = 0; i < n; i++) {
				kids[i] = (level == rangingConstants.size()-1) ? new TreeLeaf() : new TreeNode(l+1);
			}
		}

		/** Sets the result for a particular set of values in the data structure.
		  * If any constants are left undefined, the same result will be set for all values of each constant.
		  * Returns the total number of values which were set for the the first time. */
		public int setResult(Values setThese, Object result) throws PrismException
		{
			Object val;
			int valIndex, ret, i, n;
			
			// if a value has been given for this node's constant, just store the result for this value
			if (setThese.contains(constant.getName())) {
				// get value of this node's constant
				val = setThese.getValueOf(constant.getName());
				// and convert to index
				valIndex = constant.getValueIndex(val);
				// store the value
				return kids[valIndex].setResult(setThese, result);
			}
			// if not, iterate over all possible values for it and set them all
			else {
				n = constant.getNumSteps();
				ret = 0;
				for (i = 0; i < n; i++) {
					ret += kids[i].setResult(setThese, result);
				}
				return ret;
			}
		}

		/** Get a result from the data structure */
		public Object getResult(Values getThese) throws PrismException
		{
			Object val;
			int valIndex;
			
			// get value of this node's constant
			val = getThese.getValueOf(constant.getName());
			// and convert to index
			valIndex = constant.getValueIndex(val);
			// return the value
			return kids[valIndex].getResult(getThese);
		}

		/** Create ArrayList representation of the data */
		public ArrayList toArrayList()
		{
			ArrayList a = new ArrayList();
			String line[] = new String[rangingConstants.size()+1];
			toArrayListRec(a, line);
			return a;
		}
		
		public void toArrayListRec(ArrayList a, String line[])
		{
			int i, n;
			String res, s;
			
			n = constant.getNumSteps();
			for (i = 0; i < n; i++) {
				line[level] = constant.getValue(i).toString();
				kids[i].toArrayListRec(a, line);
			}
		}

		/** Create string representation of the data */
		public String toString()
		{
			return toString(false, ",", ",");
		}
		
		/** Create string representation of the data
		  * @param pv Print the variables in each row?
		  * @param sep String for separating values
		  * @param eq String for seperating values and result */
		public String toString(boolean pv, String sep, String eq)
		{
			return toStringRec(pv, sep, eq, "");
		}
		
		public String toStringRec(boolean pv, String sep, String eq, String head)
		{
			int i, n;
			String res, s;
			
			res = "";
			n = constant.getNumSteps();
			for (i = 0; i < n; i++) {
				s = "";
				if (level > 0) s += sep;
				if (pv) s += constant.getName() + "=";
				s += constant.getValue(i);
				res += kids[i].toStringRec(pv, sep, eq, head + s);
			}
			
			return res;
		}
		
		/** Create string representation of the data for a partial evaluation
		  * @param partial Values for a subset of the constants
		  * @param pv Print the variables in each row?
		  * @param sep String for separating values
		  * @param eq String for seperating values and result */
		public String toStringPartial(Values partial, boolean pv, String sep, String eq) throws PrismException
		{
			return toStringPartialRec(partial, true, pv, sep, eq, "");
		}
		
		public String toStringPartialRec(Values partial, boolean first, boolean pv, String sep, String eq, String head) throws PrismException
		{
			int i, n, valIndex;
			String res, s;
			Object val;
			
			res = "";
			// if a value has been given for this node's constant, use it
			if (partial.contains(constant.getName())) {
				// get value of this node's constant
				val = partial.getValueOf(constant.getName());
				// and convert to index
				valIndex = constant.getValueIndex(val);
				// return the value
				res += kids[valIndex].toStringPartialRec(partial, first, pv, sep, eq, head);
			}
			// if not, iterate over all possible values for it
			else {
				n = constant.getNumSteps();
				for (i = 0; i < n; i++) {
					s = "";
					if (!first) s += sep;
					if (pv) s += constant.getName() + "=";
					s += constant.getValue(i);
					res += kids[i].toStringPartialRec(partial, false, pv, sep, eq, head + s);
				}
			}
			
			return res;
		}
	}

	private class TreeLeaf extends TreeNode
	{
		private Object val = null;
		public int setResult(Values setThese, Object result) throws PrismException { int ret = (val==null)?1:0; val = result; return ret; }
		public Object getResult(Values getThese) throws PrismException { return val; }
		public String toStringRec(boolean pv, String sep, String eq, String head) { return head + eq + val + "\n"; }
		public String toStringPartialRec(Values partial, boolean first, boolean pv, String sep, String eq, String head) { return head + eq + val + "\n"; }
		public void toArrayListRec(ArrayList a, String line[]) { line[rangingConstants.size()]=""+val; a.add(line.clone()); }
	}
}
