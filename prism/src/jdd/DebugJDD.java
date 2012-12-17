//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class DebugJDD
{
	private static native int DebugJDD_GetRefCount(long dd);

	static {
		try {
			System.loadLibrary("jdd");
		} catch (UnsatisfiedLinkError e) {
			System.out.println(e);
			System.exit(1);
		}
	}

	/**
	 * determines if the debugging of JDD nodes is enabled.
	 */
	public static final boolean debugEnabled = false;

	/**
	 * Where stack traces are kept.
	 * Key: hashcode of a JDDNode
	 * Value: stack trace from the moment the JDDNode was constructed.
	 */
	protected static HashMap<Integer, StackTraceElement[]> stackTraces;

	/**
	 * Keeps track of refs and derefs made in Java
	 * Key: hashcode of a JDDNode
	 * Value: 1 + (number of times the node was refed) - (number of times the node was derefed)
	 */
	protected static HashMap<Integer, Integer> localCounters;

	/**
	 * Keeps track of instances of JDDNode pointing to (native) DdNode
	 * Key: pointer to a DdNode
	 * Value: list of hashcodes of JDDNodes pointing to the DdNode
	 */
	protected static HashMap<Long, List<Integer>> instances;

	protected static void addToSet(JDDNode node, int count)
	{
		//not thread safe
		if (instances == null) {
			instances = new HashMap<Long, List<Integer>>();
			localCounters = new HashMap<Integer, Integer>();
			stackTraces = new HashMap<Integer, StackTraceElement[]>();
		}

		int hc = System.identityHashCode(node);
		if (instances.containsKey(node.ptr())) {
			long ptr = node.ptr();
			instances.get(ptr).add(hc);
		} else {
			long ptr = node.ptr();
			List<Integer> al = new LinkedList<Integer>();
			al.add(hc);
			instances.put(ptr, al);
		}
		localCounters.put(hc, count);

		StackTraceElement[] st = Thread.currentThread().getStackTrace();
		//ignore first two elements which are Thread and this constructor
		int num = st.length - 2;
		StackTraceElement[] creator = new StackTraceElement[num];
		for (int i = 0; i < num; i++) {
			creator[i] = st[i + 2];
		}
		stackTraces.put(hc, creator);
	}

	protected static void decrement(JDDNode node)
	{
		int hc = System.identityHashCode(node);
		int newValue = localCounters.get(hc) - 1;
		int cuddRefCount = DebugJDD_GetRefCount(node.ptr());
		assert cuddRefCount > 0;
		assert cuddRefCount - 1 >= newValue;
		if (newValue < 0 && cuddRefCount == newValue) {
			System.out.println("Dereferencing node " + node + " too often. Printing stack trace where it was created.");
			//print only top 5 methods from stack
			int i = 0;
			for (StackTraceElement st : stackTraces.get(hc)) {
				if (i++ > 5) {
					break;
				}
				System.out.println("  " + st.toString());
			}
			assert false;
		}

		localCounters.put(hc, newValue);
	}

	protected static void increment(JDDNode node)
	{
		int hc = System.identityHashCode(node);
		int newValue = localCounters.get(hc) + 1;
		int cuddRefCount = DebugJDD_GetRefCount(node.ptr());
		assert cuddRefCount + 1 >= newValue; 
		localCounters.put(hc, newValue);
	}

	public static void endLifeCycle()
	{
		// Kick everybody who hasn't got a reference count higher than zero out
		for (Iterator<Long> itt = instances.keySet().iterator(); itt.hasNext(); ) {
			Long ptr = itt.next();
			for (Iterator<Integer> it = instances.get(ptr).iterator(); it.hasNext(); ) {
				int localRefCount = localCounters.get(it.next());
				if (localRefCount != 0) {
					it.remove();
				}
			}
			if (instances.get(ptr).isEmpty()) {
				itt.remove();
			}
		}
		for (Long ptr : instances.keySet()) {
			//check if cudd has nonzero number of references
			int cuddRefCount = DebugJDD_GetRefCount(ptr);
			if (cuddRefCount != 0) {
				//check if there is a JDD object with nonzero refcount
				boolean hasSuspicious = false;
				for (Integer hc : instances.get(ptr)) {
					int localRefCount = localCounters.get(hc);
					if (localRefCount > 0) {
						hasSuspicious = true;
						break;
					}
				}

				//if !hasSuspicious, there is no useful output we could give.
				//Either it's false alarm, or the problem is in c++
				if (!hasSuspicious)
					continue;
				System.out.println("WARNING: there are nodes with nonzero references, printing debug info. "
						+ "Note that the stack traces below are from moments JDDNodes were "
						+ "created. The actual problem can be elsewhere where the node is used");
				//print warning together with suspicious nodes
				System.out.println("Node " + new JDDNode(ptr, false) + " has " + cuddRefCount + " reference(s), printing out stack traces of suspicious node instances:");
				boolean first = true;
				for (Integer hc : instances.get(ptr)) {
					int localRefCount = localCounters.get(hc);
					if (localRefCount != 0) {
						if (!first) {
							System.out.println(" &");
							//first=false;
						} else {
							first = false;
						}
						//print only top 5 methods from stack
						int i = 0;
						for (StackTraceElement st : stackTraces.get(hc)) {
							if (i++ > 8) {
								break;
							}
							System.out.println("  " + st.toString());
						}
					}
				}
			}
		}
	}
	
	public static int getRefCount(JDDNode n) {
		return DebugJDD_GetRefCount(n.ptr());
	}
}
