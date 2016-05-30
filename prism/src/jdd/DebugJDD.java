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


/**
 * Framework for debugging JDDNode and CUDD reference counting.
 * <br>
 * If DebugJDD is enabled (via calling DebugJDD.enabled()),
 * instead of normal JDDNodes, DebugJDDNodes are used. These
 * track the balance of referencing and dereferencing, etc and
 * throw errors or provide warnings if something problematic
 * occurs.
 * <br>
 * In addition, each DebugJDDNode carries a unique ID, which
 * is persistent over multiple invocations of PRISM, in contrast
 * to the DdNode pointers of CUDD, which are raw C/C++ pointers
 * and can differ between multiple PRISM runs, making tracking
 * more difficult.
 * <br>
 * You can enable tracing for a particular ID, which will print
 * status messages for each event for this particular JDDNode,
 * e.g., whenever the node is referenced, dereferenced, consumed
 * in a JDD method, copied, etc.
 * <br>
 * In the end, if there are reference leaks, a list of potential
 * JDDNode IDs is printed. These can then be used for tracing
 * the referencing and dereferencing of these nodes in a subsequent
 * run of PRISM to debug the reference leak.
 * <br>
 * Note: For the JDDNodes that are returned by JDDNode.getThen() and
 * JDDNode.getElse(), a different handling applies. As these are
 * generally only used for traversing an MTBDD and are not referenced
 * (beyond the internal CUDD references that are there due to the
 * fact that they are internal nodes of some MTBDD), they are not
 * wrapped in a DebugJDDNode. They are not fully functional and
 * should never be used for JDD.Ref, JDD.Deref, JDDNode.copy() or
 * as the argument of a JDD method, except to compare for equality,
 * getting the variable index, getThen() and getElse() and obtaining
 * constant value of a terminal node.
 */
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
		 * The ID for this JDDNode. In contrast with the CUDD DdNode* ptr,
		 * this will be stable across PRISM invocations and can thus be
		 * used for tracing.
		 */
		private long id;

		/**
		 * The balance of reference and dereference calls for
		 * this specific JDDNode object.
		 */
		private int nodeRefs = 0;

		/**
		 * Constructor, with DdNode* ptr.
		 * <br>
		 * Registers the node with the DebugJDD infrastructure.
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
			nodeRefs = 0;
			if (isReferenced) {
				incRef();
			}
			// store to keep track of this DebugJDDNode
			DebugJDD.addToSet(this);
		}

		/** Get the ID of this node */
		public long getID()
		{
			return id;
		}

		/** Increment the reference count for this DebugJDDNode */
		public void incRef()
		{
			// increment count for node
			nodeRefs++;
			// increment javaRefs counter for this ptr
			int jrefs = getJavaRefCount(ptr());
			javaRefs.put(ptr(), jrefs + 1);
		}

		/** Decrement the reference count for this DebugJDDNode */
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
				throw new RuntimeException("DebugJDD: The number of Java references is negative for\n " + toStringVerbose());
			}
			// (2) There are more Java references than CUDD references
			if (jrefs > DebugJDD_GetRefCount(ptr())) {
				throw new RuntimeException("DebugJDD: More Java refs than CUDD refs for\n " + toStringVerbose());
			}
			// (3) This node has more refs than there are Java refs in total
			if (jrefs < getNodeRefs()) {
				throw new RuntimeException("DebugJDD: JDDNode has more refs than Java refs in total?!\n " + toStringVerbose());
			}
		}

		/** Get the number of active references for this JDDNode */
		public int getNodeRefs()
		{
			return nodeRefs;
		}

		public String toStringVerbose()
		{
			return "ID = " + getID() +
			       ", CUDD ptr = " + ptrAsHex() +
			       ", refs for this JDDNode = " + getNodeRefs() +
			       ", refs from Java = " + getJavaRefCount(ptr()) +
			       ", refs from CUDD (including internal MTBDD refs) = "+DebugJDD_GetRefCount(ptr());
		}

		public String ptrAsHex()
		{
			return "0x"+Long.toHexString(ptr());
		}
	}

	/* ----- Settings ------ */

	/**
	 * Flag, determines if the debugging of JDD nodes is enabled.
	 * Default off.
	 */
	public static boolean debugEnabled = false;

	/**
	 * Flag, determines if all nodes should be traced (in contrast to only those
	 * specified using enableTracingForID().
	 * Default off.
	 */
	public static boolean traceAll = false;

	/**
	 * Flag, determines if copies of traced nodes should be traced as well.
	 * Default off.
	 */
	public static boolean traceFollowCopies = false;

	/**
	 * Flag, determines if warnings should be promoted to
	 * errors, i.e., throwing an exception.
	 * Default off.
	 */
	public static boolean warningsAreFatal = false;

	/**
	 * Flag, determines if warnings should be suppressed.
	 * Default off.
	 */
	public static boolean warningsOff = false;


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
	public static void enableTracingForID(long id)
	{
		if (traceIDs == null) {
			traceIDs = new HashSet<Long>();
		}
		traceIDs.add(id);
		System.out.println("DebugJDD: Enable tracing for ID "+id);
		enable();  // tracing implies debugging
	}

	/** Returns true if this node should be traced */
	private static boolean isTraced(DebugJDDNode dNode)
	{
		if (traceAll) {
			return true;
		}

		if (traceIDs != null && traceIDs.contains(dNode.getID())) {
			return true;
		}
		return false;
	}

	/** Store a DebugJDDNode for tracking of the references */
	private static void addToSet(DebugJDDNode node)
	{
		if (nodes.put(node.getID(), node) != null) {
			// implementation error, should not happen
			throw new RuntimeException("DebugJDD: Internal error, adding the same JDDNode multiple times, ID="+node.getID());
		}
	}

	/**
	 * Called when CUDD is shutdown.
	 * <br>
	 * Analyzes the reference counting situation
	 * and prints diagnostics in case of a leak.
	 */
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
					System.out.println("  ID="+node.getID()+" with "+node.getNodeRefs()+" references  (" + node.toStringVerbose() + ")");
				}
			} else {
				System.out.println(" No candidates, here are all JDDNodes for that DdNode:");
				for (DebugJDDNode node : matchingNodes) {
					System.out.println("  ID="+node.getID()+" with "+node.getNodeRefs()+" references  (" + node.toStringVerbose() + ")");
				}
			}
		}

		// clean-up data structures
		nodes.clear();
		javaRefs.clear();
		// reset ID counter
		DebugJDDNode.nextId = 0;
		
		if (warningsAreFatal) {
			throw new RuntimeException("DebugJDD: Leaked references");
		}
	}

	/** Get the CUDD reference count for the pointer of the JDDNode */
	public static int getRefCount(JDDNode n) {
		return DebugJDD_GetRefCount(n.ptr());
	}

	/** Get the number of DebugJDDNodes that reference the pointer */
	public static int getJavaRefCount(long ptr) {
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
		System.out.println("\ntrace("+action+", ID="+dNode.getID()+") => " + dNode.getNodeRefs() + " refs for this JDDNode\n " + dNode.toStringVerbose());
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

	/**
	 * DebugJDD implementation of JDD.Ref.
	 * <br>
	 * Increments both the CUDD reference counter and the reference counter for the JDDNode.
	 */
	protected static void Ref(JDDNode node)
	{
		if (!(node instanceof DebugJDDNode)) {
			// ref of a node that is not wrapped in a DebugJDDNode
			//  -> probably came from JDDNode.getThen()/getElse(), should not be refed
			throw new RuntimeException("DebugJDD: Illegal operation, trying to Ref a plain JDDNode (obtained from getThen()/getElse()?)");
		}
		DebugJDDNode dNode = (DebugJDDNode) node;

		// increment reference in DebugJDD
		dNode.incRef();
		// increment reference in CUDD
		JDD.DD_Ref(node.ptr());

		if (isTraced(dNode)) {
			trace("Ref", dNode);
		}
	}

	/**
	 * DebugJDD implementation of JDD.Deref.
	 * <br>
	 * Decrements both the CUDD reference counter and the reference counter for the JDDNode,
	 * performing a variety of sanity checks.
	 */
	protected static void Deref(JDDNode node)
	{
		if (!(node instanceof DebugJDDNode)) {
			// deref of a node that is not wrapped in a DebugJDDNode
			//  -> probably came from JDDNode.getThen()/getElse(), should not be derefed
			throw new RuntimeException("DebugJDD: Illegal operation, trying to Deref a plain JDDNode (obtained from getThen()/getElse()?)");
		}
		DebugJDDNode dNode = (DebugJDDNode) node;
		if (dNode.getNodeRefs() <= 0 && getJavaRefCount(node.ptr()) > 0) {
			// This is only a warning as currently there are places
			// where referencing / dereferencing happens across multiple JDDNodes,
			// which is not pretty but not necessarily problematic, as long as
			// the total number of references from Java remains positive
			String warning = "DebugJDD: Deref of a JDDNode with non-positive ref count:\n " + dNode.toStringVerbose();
			if (!warningsOff && !warningsAreFatal) {
				System.out.println("Warning, " + warning);
				printStack(0);
			} else if (!warningsOff && warningsAreFatal) {
				throw new RuntimeException(warning);
			}
		}

		if (getJavaRefCount(node.ptr()) <= 0) {
			throw new RuntimeException("DebugJDD: Trying to Deref a JDDNode with non-positive Java ref count:\n " + dNode.toStringVerbose());
		}

		int cuddRefCount = DebugJDD_GetRefCount(node.ptr());
		if (cuddRefCount <= 0) {
			throw new RuntimeException("DebugJDD: Trying to Deref a JDDNode with a non-positive CUDD ref count\n " + dNode.toStringVerbose());
		}

		// decrement reference in DebugJDD
		dNode.decRef();
		// decrement reference in CUDD
		JDD.DD_Deref(node.ptr());

		if (isTraced(dNode)) {
			trace("Deref", dNode);
		}
	}

	/**
	 * DebugJDD implementation of JDDNode.copy().
	 * <br>
	 * Increments the reference count for the copy and handles tracing.
	 */
	protected static JDDNode Copy(JDDNode node)
	{
		if (!(node instanceof DebugJDDNode)) {
			// copy of a node that is not wrapped in a DebugJDDNode
			//  -> probably came from JDDNode.getThen()/getElse(), should not be copied
			throw new RuntimeException("DebugJDD: Illegal operation, trying to copy a plain JDDNode (obtained from getThen()/getElse()?)");
		}

		DebugJDDNode dNode = (DebugJDDNode) node;

		if (dNode.getNodeRefs() <= 0) {
			throw new RuntimeException("DebugJDD: Trying to copy a JDDNode with non-positive ref count:\n " + dNode.toStringVerbose());
		}

		if (getRefCount(dNode) <= 0) {
			throw new RuntimeException("DebugJDD: Trying to copy a JDDNode with non-positive CUDD ref count:\n " + dNode.toStringVerbose());
		}

		DebugJDDNode result = new DebugJDD.DebugJDDNode(dNode.ptr(), false);
		JDD.Ref(result);

		if (isTraced(dNode)) {
			trace("Copy to "+result.getID(), dNode);
		}

		if (!traceAll && traceFollowCopies && isTraced(dNode)) {
			// if we are not tracing everything anyway and we should follow copies:
			// enable tracing for the copy if the original node is traced
			enableTracingForID(result.getID());
		}

		if (isTraced(result)) {
			trace("Copied", result);
		}

		return result;
	}

	/**
	 * DebugJDD implementation of JDD.ptrToNode, i.e.,
	 * converting a (referenced) DdNode ptr returned by
	 * the DD_* CUDD abstraction layer into a proper JDDNode.
	 */
	protected static JDDNode ptrToNode(long ptr)
	{
		DebugJDDNode dNode = new DebugJDD.DebugJDDNode(ptr, true);

		if (isTraced(dNode)) {
			trace("ptrToNode", dNode);
		}

		return dNode;
	}

	/**
	 * DebugJDD handling of passing JDDNodes as arguments to the
	 * DD_* layer, where they will be ultimately dereferenced.
	 * <br>
	 * As the actual dereferencing in CUDD will only happen
	 * after the given MTBDD method is finished, we only
	 * decrement the reference counter on the DebugJDD side.
	 */
	protected static void DD_Method_Argument(JDDNode node)
	{
		if (!(node instanceof DebugJDDNode)) {
			// using a node that is not wrapped in a DebugJDDNode in a DD_* method call
			//  -> probably came from JDDNode.getThen()/getElse(), should not be used like this
			throw new RuntimeException("DebugJDD: Illegal operation, trying to use a plain JDDNode (obtained from getThen()/getElse()?) in a method call");
		}

		DebugJDDNode dNode = (DebugJDDNode) node;
		if (dNode.getNodeRefs() <= 0 && getJavaRefCount(node.ptr()) > 0) {
			// This is only a warning as currently there are places
			// where referencing / dereferencing happens across multiple JDDNodes,
			// which is not pretty but not necessarily problematic, as long as
			// the total number of references from Java remains positive
			String warning = "DebugJDD: Trying to use a JDDNode with non-positive ref count:\n " + dNode.toStringVerbose();
			if (!warningsOff && !warningsAreFatal) {
				System.out.println("Warning, " + warning);
				printStack(0);
			} else if (!warningsOff && warningsAreFatal) {
				throw new RuntimeException(warning);
			}
		}

		if (getJavaRefCount(node.ptr()) <= 0) {
			throw new RuntimeException("DebugJDD: Trying to use a JDDNode with non-positive Java ref count in a method call:\n " + dNode.toStringVerbose());
		}

		int cuddRefCount = DebugJDD_GetRefCount(node.ptr());
		if (cuddRefCount <= 0) {
			throw new RuntimeException("DebugJDD: Trying to use a JDDNode with a non-positive CUDD ref count in a method call:\n " + dNode.toStringVerbose());
		}

		// decrement reference in DebugJDD
		dNode.decRef();
		// ... but don't dereference in CUDD yet. This will
		// be done in the DD_* methods after the actual
		// processing has happened and the result is referenced,
		// as then this argument does not have to be protected
		// anymore

		if (isTraced(dNode)) {
			trace("Deref (as method argument)", dNode);
		}
	}

}
