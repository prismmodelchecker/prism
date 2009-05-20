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

package jdd;

import java.util.Vector;

public class JDDVars
{
	private Vector vars;
	private long array;
	private boolean arrayBuilt;
	
	private native long DDV_BuildArray();
	private native void DDV_FreeArray(long a);
	private native int DDV_GetIndex(long dd);

	static
	{
		try {
			System.loadLibrary("jdd");
		}
		catch (UnsatisfiedLinkError e) {
			System.out.println(e);
			System.exit(1);
		}
	}

	public JDDVars()
	{
		vars = new Vector();
		array = 0;
		arrayBuilt = false;
	}
		
	public void addVar(JDDNode var)
	{
		vars.addElement(var);
		if (arrayBuilt) DDV_FreeArray(array);
		arrayBuilt = false;
	}
	
	public void addVars(JDDVars ddv)
	{
		int i;
		
		vars.addAll(ddv.vars);
		if (arrayBuilt) DDV_FreeArray(array);
		arrayBuilt = false;
	}
	
	public void removeVars(JDDVars ddv)
	{
		int i;
		
		vars.removeAll(ddv.vars);
		if (arrayBuilt) DDV_FreeArray(array);
		arrayBuilt = false;
	}
	
	public int getNumVars()
	{
		return vars.size();
	}

	public JDDNode getVar(int i)
	{
		return (JDDNode)vars.elementAt(i);
	}
	
	public long getVarPtr(int i)
	{
		return ((JDDNode)vars.elementAt(i)).ptr();
	}
	
	public int getVarIndex(int i)
	{
		return DDV_GetIndex(((JDDNode)vars.elementAt(i)).ptr());
	}
	
	public int getMinVarIndex()
	{
		int i, j, n, min;
		n = vars.size();
		if (n == 0) return -1;
		min = getVarIndex(0);
		for (i = 1; i < n; i++) {
			j = getVarIndex(i);
			if (j < min) min = j;
		}
		return min;
	}
	
	public int getMaxVarIndex()
	{
		int i, j, n, max;
		n = vars.size();
		if (n == 0) return -1;
		max = getVarIndex(0);
		for (i = 1; i < n; i++) {
			j = getVarIndex(i);
			if (j > max) max = j;
		}
		return max;
	}
	
	public void refAll()
	{
		int i;
		
		for (i = 0; i < vars.size(); i++) {
			JDD.Ref((JDDNode)vars.elementAt(i));
		}
	}
	
	public void derefAll()
	{
		int i;
		
		for (i = 0; i < vars.size(); i++) {
			JDD.Deref((JDDNode)vars.elementAt(i));
		}
	}
	
	public long array()
	{
		if (arrayBuilt) {
			return array;
		}
		else {
			array = DDV_BuildArray();
			arrayBuilt = true;
			return array;
		}
	}
	
	public int n()
	{
		return vars.size();
	}
	
	public String toString()
	{
		int i;
		String s = "{";
		
		for (i = 0; i < vars.size() - 1; i++) {
			s = s + getVarIndex(i) + ", ";
		}
		if (vars.size() > 0) {
			s = s + getVarIndex(vars.size() - 1);
		}
		s += "}";
		
		return s;
	}
}

//------------------------------------------------------------------------------
