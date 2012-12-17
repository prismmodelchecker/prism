//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Christian von Essen <christian.vonessen@imag.fr> (VERIMAG)
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

public class JDDNode
{
	private long ptr;
	
	// native methods (jni)
	private native boolean DDN_IsConstant(long dd);
	private native int DDN_GetIndex(long dd);
	private native double DDN_GetValue(long dd);
	private native long DDN_GetThen(long dd);
	private native long DDN_GetElse(long dd);

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

	public JDDNode(long p, boolean increased_reference)
	{
		ptr = p;
		if (DebugJDD.debugEnabled)
			DebugJDD.addToSet(this, increased_reference ? 1 : 0);
	}
	
	public JDDNode(long p) {
		this(p, true);
	}
	
	public JDDNode(JDDNode dd)
	{
		this(dd.ptr());
	}
	
	public long ptr()
	{
		return ptr;
	}

	public boolean isConstant()
	{
		return DDN_IsConstant(ptr);
	}

	public int getIndex()
	{	
		return DDN_GetIndex(ptr);
	}

	public double getValue()
	{	
		return DDN_GetValue(ptr);
	}

	public JDDNode getThen()
	{
		assert !this.isConstant();
		// TODO I believe that this breaks debug reference counting
		// JDDNode will create a reference to this, but DDN_GetThen will not.
		return new JDDNode(DDN_GetThen(ptr), false);
	}
	
	public JDDNode getElse()
	{
		assert !this.isConstant();
		return new JDDNode(DDN_GetElse(ptr), false);
	}

	public boolean equals(Object o)        
	{
		return (o instanceof JDDNode) && (((JDDNode) o).ptr == ptr);
	}
	
	public int hashCode()
	{
		return (int)ptr; 
	}
	
	public String toString()
	{
		String result = "" + ptr;
		if (ptr != 0) {
			if (this.isConstant()) result += " value=" + this.getValue();
			result += " references=" + DebugJDD.getRefCount(this);
		}
		return result;
	}
}

//------------------------------------------------------------------------------
