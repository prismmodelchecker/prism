//==============================================================================
//	
//	File:		JDDNode.java
//	Date:		22/5/00
//	Author:		Dave Parker
//	
//------------------------------------------------------------------------------
//	
//	Copyright (c) 2002-2004, Dave Parker
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
	private int ptr;
	
	// native methods (jni)
	private native boolean DDN_IsConstant(int dd);
	private native int DDN_GetIndex(int dd);
	private native double DDN_GetValue(int dd);
	private native int DDN_GetThen(int dd);
	private native int DDN_GetElse(int dd);

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

	public JDDNode(int p)
	{
		ptr = p;
	}
	
	public JDDNode(JDDNode dd)
	{
		ptr = dd.ptr;
	}
	
	public int ptr()
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
		return new JDDNode(DDN_GetThen(ptr));
	}
	
	public JDDNode getElse()
	{
		return new JDDNode(DDN_GetElse(ptr));
	}

	public boolean equals(JDDNode dd)
	{
		return ptr == dd.ptr;
	}
	
	public String toString()
	{
		return "" + ptr;
	}
}

//------------------------------------------------------------------------------
