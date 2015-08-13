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
	protected static native boolean DDN_IsConstant(long dd);
	protected static native int DDN_GetIndex(long dd);
	protected static native double DDN_GetValue(long dd);
	protected static native long DDN_GetThen(long dd);
	protected static native long DDN_GetElse(long dd);

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

	/**
	 * Protected constructor from a DdNode pointer.
	 * In general, to get a JDDNode from a pointer,
	 * use JDD.ptrToNode().
	 */
	protected JDDNode(long p)
	{
		ptr = p;
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

	/**
	 * Returns the Then child of a (non-constant) JDDNode.
	 * <br>
	 * This method does NOT increase the reference count of the returned
	 * node, it is therefore illegal to call JDD.Deref on the result.
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getThen()
	{
		assert !this.isConstant();

		// just return the node, even if DebugJDD is enabled
		// DDN_GetThen will return NULL if the current node is a
		// constant, raising an Exception in the JDDNode constructor
		return new JDDNode(DDN_GetThen(ptr));
	}

	/**
	 * Returns the Else child of a (non-constant) JDDNode.
	 * <br>
	 * This method does NOT increase the reference count of the returned
	 * node, it is therefore illegal to call JDD.Deref on the result.
	 * <br>[ REFS: <i>none</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode getElse()
	{
		assert !this.isConstant();

		// just return the node, even if DebugJDD is enabled
		// DDN_GetElse will return NULL if the current node is a
		// constant, raising an Exception in the JDDNode constructor
 		return new JDDNode(DDN_GetElse(ptr));
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

	/**
	 * Returns a referenced copy of this node.
	 * This has the effect of increasing the reference count
	 * for the underlying MTBDD.
	 * <br>[ REFS: <i>result</i>, DEREFS: <i>none</i> ]
	 */
	public JDDNode copy()
	{
		JDDNode result;
		if (DebugJDD.debugEnabled) {
			result = new DebugJDD.DebugJDDNode(ptr, false);
		} else {
			result = new JDDNode(ptr());
		}
		JDD.Ref(result);
		return result;
	}
}

//------------------------------------------------------------------------------
