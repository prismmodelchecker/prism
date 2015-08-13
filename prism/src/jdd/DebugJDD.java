//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
//	* Christian von Essen <christian.vonessen@imag.fr> (VERIMAG)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class DebugJDD
{
	private static native int DebugJDD_GetRefCount(long dd);
	private static native long[] DebugJDD_GetExternalRefCounts();

	static {
		try {
			System.loadLibrary("jdd");
		} catch (UnsatisfiedLinkError e) {
			System.out.println(e);
			System.exit(1);
		}
	}

	/**
	 * A DebugJDDNode extends a JDDNode with additional information
	 * useful for tracking refs/derefs and tracing.
	 */
	protected static class DebugJDDNode extends JDDNode {

		/** A static counter that is used to provide a unique ID for each JDDNode */
		private static long nextId = 0;

		/**
		 * The ID for this JDDNode. In contrast with the DdNode* ptr, this will be
		 * stable across PRISM invocations and can thus be used for tracing.
		 */
		private long id;

		/** The balance of reference/dereference calls this specific JDDNode */
		private int nodeRefs = 0;

		/**
		 * Constructor, with DdNode* ptr.
		 * @param ptr the DdNode pointer in CUDD
		 * @param isReferenced Does this JDDNode already have a reference
		 *                     (like for ptrToNode) or not (like for getThen / getElse)
		 */
		public DebugJDDNode(long ptr, boolean isReferenced)
		{
			// instantiate underlying JDDNode
			super(ptr);
			// generate and store globally unique ID
			id = nextId++;
			// the initial number of references for this JDDNode
			nodeRefs = (isReferenced ? 1 : 0);
			if (isReferenced) {
				// increase javaRefs counter for this ptr
				int jrefs = getJavaRefCount(ptr());
				javaRefs.put(ptr(), jrefs+1);
			}
			// store to keep track of this DebugJDDNode
			DebugJDD.addToSet(this);
		}

		/** Get the ID of this node */
		public long getID()
		{
			return id;
		}

		/** Increment the reference count for this JDDNode */
		public void incRef()
		{
			// increment count for node
			nodeRefs++;
			// increment javaRefs counter for this ptr
			int jrefs = getJavaRefCount(ptr());
			javaRefs.put(ptr(), jrefs + 1);
		}

		/** Decrement the reference count for this JDDNode */
		public void decRef()
		{
			// decrement count for node
			nodeRefs--;
			// decrement javaRefs counter for this ptr
			int jrefs = getJavaRefCount(ptr()) - 1;
			javaRefs.put(ptr(), jrefs);

			// Checks:
			// (1) There is a negative number of Java references for the DdNode pointer
			if (jrefs < 0) {
				throw new RuntimeException("DebugJDD: The number of Java references is negative for ptr=0x"+Long.toHexString(ptr())+", ID="+getID()+", Java refs = "+jrefs+", CUDD refs = "+DebugJDD_GetRefCount(ptr()));
			}
			// (2) There are more Java references than Cudd references
			if (jrefs > DebugJDD_GetRefCount(ptr())) {
				throw new RuntimeException("DebugJDD: More Java refs than CUDD refs for ptr=0x"+Long.toHexString(ptr())+", ID="+getID()+", Java refs = "+jrefs+", CUDD refs = "+DebugJDD_GetRefCount(ptr()));
			}
			// (3) This node has more refs than there are Java refs in total
			if (jrefs < getNodeRefs()) {
				throw new RuntimeException("DebugJDD: JDDNode has more refs than Java refs in total?! ID="+getID()+", refs = "+getNodeRefs()+", Java refs = "+jrefs+", CUDD refs = "+DebugJDD_GetRefCount(ptr()));
			}
		}

		/** Get the number of active references for this JDDNode */
		public int getNodeRefs()
		{
			return nodeRefs;
		}
	}

	/**
	 * Flag, determines if the debugging of JDD nodes is enabled.
	 */
	public static boolean debugEnabled = false;

	/**
	 * Map from DebugJDDNode IDs to DebugJDDNode.
	 * LinkedHashMap to ensure that iterating over the map returns the
	 * DebugJDDNodes in a consistent order, sorted by ID.
	 */
	protected static LinkedHashMap<Long, DebugJDDNode> nodes = new LinkedHashMap<Long, DebugJDDNode>();

	/** A map from DdNode pointers to the current sum of the nodeRefs of all JDDNodes for that pointer */
	protected static HashMap<Long, Integer> javaRefs = new HashMap<Long, Integer>();

	/** An optional set of DebugJDDNode IDs that will be traced */
	protected static HashSet<Long> traceIDs = null;

	/** Enable debugging */
	public static void enable()
	{
		debugEnabled = true;
	}
	/** Activate tracing for the DebugJDDNode with ID id */
	public static void enableTracingForID(long id) {
		if (traceIDs == null) {
			traceIDs = new HashSet<Long>();
		}
		traceIDs.add(id);
		System.out.println("DebugJDD: Enable tracing for "+id);
		enable();  // tracing implies debugging
	}
	/** Store a DebugJDDNode for tracking of the references */
	protected static void addToSet(DebugJDDNode node)
	{
		if (nodes.put(node.getID(), node) != null) {
			// implementation error, should not happen
			throw new RuntimeException("DebugJDD: Adding the same JDDNode multiple times, ID="+node.getID());
		}
		if (traceIDs != null && traceIDs.contains(node.id)) {
			trace("create", node);
		}
	}

	/** Notification from JDD.Deref(node) */
	protected static void decrement(JDDNode node)
	{
		if (!(node instanceof DebugJDDNode)) {
			// deref of a node that is not wrapped in a DebugJDDNode
			//  -> probably came from JDDNode.getThen()/getElse(), should not be derefed
			throw new RuntimeException("DebugJDD: Trying to Deref a plain JDDNode (obtained from getThen()/getElse()?)");
		}
		DebugJDDNode dNode = (DebugJDDNode) node;
		if (dNode.getNodeRefs() == 0) {
			// This is only a warning as currently there are places
			// where referencing / derefencing happens across multiple JDDNodes
			System.out.println("Warning, DebugJDD: Deref of a JDDNode that has 0 references, ID = "+dNode.getID());
			printStack(0);
		}
		dNode.decRef();
		int cuddRefCount = DebugJDD_GetRefCount(node.ptr());
		if (cuddRefCount <= 0) {
			throw new RuntimeException("DebugJDD: Trying to deref a JDDNode with a non-positive CUDD ref count, ptr=0x"+Long.toHexString(dNode.ptr())+", ID = "+dNode.getID()+", Java ref = "+getJavaRefCount(dNode.ptr())+", CUDD refs = "+cuddRefCount);
		}
		if (traceIDs != null && traceIDs.contains(dNode.id)) {
			trace("deref", dNode);
		}
	}

	/** Notification from JDD.Ref(node) */
	protected static void increment(JDDNode node)
	{
		if (!(node instanceof DebugJDDNode)) {
			// ref of a node that is not wrapped in a DebugJDDNode
			//  -> probably came from JDDNode.getThen()/getElse(), should not be refed
			throw new RuntimeException("DebugJDD: Trying to Ref a plain JDDNode (obtained from getThen()/getElse()?)");
		}
		DebugJDDNode dNode = (DebugJDDNode) node;
		dNode.incRef();
		if (traceIDs != null && traceIDs.contains(dNode.id)) {
			trace("ref", dNode);
		}
	}

	public static void endLifeCycle()
	{
		// Get the external reference count from CUDD
		Map<Long, Integer> externalRefCounts = getExternalRefCounts();
		if (externalRefCounts.size() == 0) {
			// everthing is fine
			return;
		}

		System.out.println("\nWarning: Found " + externalRefCounts.size() + " leaked JDDNode references.");
		System.out.flush();
		for (Entry<Long, Integer> extRef : externalRefCounts.entrySet()) {
			long ptr = extRef.getKey();
			List<DebugJDDNode> matchingNodes = new ArrayList<DebugJDDNode>();
			List<DebugJDDNode> posRewNodes = new ArrayList<DebugJDDNode>();
			for (DebugJDDNode node : nodes.values()) {
				if (node.ptr() == ptr) {
					// node matches
					matchingNodes.add(node);
					// node still has positive reference count
					if (node.getNodeRefs() > 0) {
						posRewNodes.add(node);
					}
				}
			}

			System.out.println("DdNode ptr=0x" + Long.toHexString(ptr)+", "+nodeInfo(ptr)+" has "+extRef.getValue()+" remaining external references.");
			if (posRewNodes.size() > 0) {
				System.out.println(" Candidates:");
				for (DebugJDDNode node : posRewNodes) {
					System.out.println("  ID="+node.getID()+" with "+node.getNodeRefs()+" references");
				}
			} else {
				System.out.println(" No candidates, here are all JDDNodes for that DdNode:");
				for (DebugJDDNode node : matchingNodes) {
					System.out.println("  ID="+node.getID()+" with "+node.getNodeRefs()+" references");
				}
			}
		}
	}

	/** Get the CUDD reference count for the pointer of the JDDNode */
	public static int getRefCount(JDDNode n) {
		return DebugJDD_GetRefCount(n.ptr());
	}

	/** Get the number of DebugJDDNodes that reference the pointer */
	private static int getJavaRefCount(long ptr) {
		Integer jrefs = javaRefs.get(ptr);
		if (jrefs == null) return 0;
		return jrefs;
	}

	/** Get a map, mapping DdNode pointers to the external reference counts expected by CUDD */
	public static Map<Long, Integer> getExternalRefCounts()
	{
		Map<Long, Integer> result = new TreeMap<Long, Integer>();
		// Array consists of (pointer, count) pairs
		long[] externalRefCounts = DebugJDD_GetExternalRefCounts();
		int i=0;
		while (i<externalRefCounts.length) {
			long node = externalRefCounts[i++];
			int count = (int)externalRefCounts[i++];
			result.put(node, count);

		}

		return result;
	}

	/** Get a string with more detailed info for a DdNode pointer */
	private static String nodeInfo(long ptr)
	{
		if (JDDNode.DDN_IsConstant(ptr)) {
			return "constant("+JDDNode.DDN_GetValue(ptr)+"), CUDD refs="+DebugJDD_GetRefCount(ptr);
		} else {
			return "var("+JDDNode.DDN_GetIndex(ptr)+"), CUDD refs="+DebugJDD_GetRefCount(ptr);
		}
	}

	/** Log information about the action performed on the DebugJDDNode */
	private static void trace(String action, DebugJDDNode dNode)
	{
		System.out.println("\ntrace("+action+",ID="+dNode.getID()+") = "+dNode.getNodeRefs()+" refs (total = "+DebugJDD_GetRefCount(dNode.ptr())+", local="+javaRefs.get(dNode.ptr())+")");
		printStack(0);
	}

	/** Print the current stack trace, omitting initial java.lang and jdd.DebugJDD frames.
	 * @param limit Limit on the number of frames to print, 0 = no limit
	 */
	private static void printStack(int limit)
	{
		// apparently, the following is more efficient than calling Thread.currentThread.getStackTrace()
		StackTraceElement[] stack = (new Throwable()).getStackTrace();
		boolean foundStart = false;
		int printed = 0;
		for (StackTraceElement ste : stack) {
			String st = ste.toString();
			if (!foundStart) {
				if (st.startsWith("java.lang") || st.startsWith("jdd.DebugJDD")) {
					// skip
					continue;
				} else {
					foundStart = true;
					// continue below
				}
			}
			if (limit > 0 && printed++ >= limit) {
				System.out.println("  ...");
				break;
			} else {
				System.out.println("  at " + st);
			}
		}
		System.out.println();
		System.out.flush();
	}

}
