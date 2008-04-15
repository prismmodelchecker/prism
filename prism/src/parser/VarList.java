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

package parser;

import java.util.Vector;

import prism.PrismUtils;

/** Class to store a list of module variables */

public class VarList
{
	int numVars;
	Vector<String> names;
	Vector<Integer> lows;
	Vector<Integer> highs;
	Vector<Integer> ranges;
	Vector<Integer> rangeLogTwos;
	Vector<Integer> starts;
	Vector<Integer> modules;
	Vector<Integer> types;
	
	public VarList()
	{
		numVars = 0;
		names = new Vector<String>();
		lows = new Vector<Integer>();
		highs = new Vector<Integer>();
		ranges = new Vector<Integer>();
		rangeLogTwos = new Vector<Integer>();
		starts = new Vector<Integer>();
		modules = new Vector<Integer>();
		types = new Vector<Integer>();
	}
	
	/**
	 * Add a new variable to the end of the VarList.
	 */
	public void addVar(String n, int l, int h, int s, int m, int t)
	{
		int r, r2;
		
		names.add(n);
		lows.add(l);
		highs.add(h);
		r = h - l + 1;
		ranges.add(r);
		r2 = (int)Math.ceil(PrismUtils.log2(r));
		rangeLogTwos.add(r2);
		starts.add(s);
		modules.add(m);
		types.add(t);
		numVars++;
	}
	
	/**
	 * Add a new variable at position i in the VarList.
	 */
	public void addVar(int i, String n, int l, int h, int s, int m, int t)
	{
		int r, r2;
		
		names.add(i, n);
		lows.add(i, l);
		highs.add(i, h);
		r = h - l + 1;
		ranges.add(i, r);
		r2 = (int)Math.ceil(PrismUtils.log2(r));
		rangeLogTwos.add(i, r2);
		starts.add(i, s);
		modules.add(i, m);
		types.add(i, t);
		numVars++;
	}
	
	public int getNumVars()
	{
		return numVars;
	}
	
	public String getName(int i)
	{
		return (String)names.elementAt(i);
	}

	public int getIndex(String n)
	{
		return names.indexOf(n);
	}

	public boolean exists(String n)
	{
		return getIndex(n) != -1;
	}

	public int getLow(int i)
	{
		return ((Integer)lows.elementAt(i)).intValue();
	}

	public int getHigh(int i)
	{
		return ((Integer)highs.elementAt(i)).intValue();
	}

	public int getRange(int i)
	{
		return ((Integer)ranges.elementAt(i)).intValue();
	}

	public int getRangeLogTwo(int i)
	{
		return ((Integer)rangeLogTwos.elementAt(i)).intValue();
	}

	public int getStart(int i)
	{
		return ((Integer)starts.elementAt(i)).intValue();
	}

	public int getModule(int i)
	{
		return ((Integer)modules.elementAt(i)).intValue();
	}

	public int getType(int i)
	{
		return ((Integer)types.elementAt(i)).intValue();
	}
	
	public Object clone()
	{
		VarList rv = new VarList();
		rv.numVars = numVars;
		rv.names.addAll(names);
		rv.lows.addAll(lows);
		rv.highs.addAll(highs);
		rv.ranges.addAll(ranges);
		rv.rangeLogTwos.addAll(rangeLogTwos);
		rv.starts.addAll(starts);
		rv.modules.addAll(modules);
		rv.types.addAll(types);
		
		return rv;
	}
}

//------------------------------------------------------------------------------
