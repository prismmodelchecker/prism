//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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
	Vector names;
	Vector lows;
	Vector highs;
	Vector ranges;
	Vector rangeLogTwos;
	Vector starts;
	Vector modules;
	Vector types;
	
	public VarList()
	{
		numVars = 0;
		names = new Vector();
		lows = new Vector();
		highs = new Vector();
		ranges = new Vector();
		rangeLogTwos = new Vector();
		starts = new Vector();
		modules = new Vector();
		types = new Vector();
	}
	
	public void addVar(String n, int l, int h, int s, int m, int t)
	{
		int r, r2;
		
		names.addElement(n);
		lows.addElement(new Integer(l));
		highs.addElement(new Integer(h));
		r = h - l + 1;
		ranges.addElement(new Integer(r));
		r2 = (int)Math.ceil(PrismUtils.log2(r));
		rangeLogTwos.addElement(new Integer(r2));
		starts.addElement(new Integer(s));
		modules.addElement(new Integer(m));
		types.addElement(new Integer(t));
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
}

//------------------------------------------------------------------------------
