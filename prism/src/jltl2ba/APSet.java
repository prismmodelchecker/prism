//==============================================================================
//	
//	Copyright (c) 2007 Carlos Bederian
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

package jltl2ba;

import java.util.Vector;
import java.util.Iterator;
import java.io.PrintStream;

/**
 * Class representing a set of atomic propositions (AP).
 */
public class APSet implements Iterable<String> {
	
	private Vector<String> vector;
	
	/**
	 * Constructor.
	 */
	public APSet() {
		vector = new Vector<String>();
	}

  /**
   * Adds a new AP to the set.
   * @param name the name of the AP
   * @return the index of the added AP
   */
	public int addAP(String name) {
		int i = vector.indexOf(name);
		
		if (i == -1) {
			vector.add(name);
			return vector.size() - 1;
		}
		else return i;
	}

  /**
   * Gets the name of a certain AP.
   * @param index index of the AP
   * @return string-ref with the name
   */
	public String getAP(int index) {
		return vector.elementAt(index);
	}
	
  /**
   * Searches for an existing AP in the APSet and returns the index.
   * @return the index of the AP, or -1 if not found.
   */
	public int indexOf(String s) {
		return vector.indexOf(s);
	}
	
	public boolean hasAP(String s) {
		return vector.contains(s);
	}

  /**
   * Get the size of this set
   * @return the number of APs in this set.
   */
	public int size() {
		return vector.size();
	}

  /**
   * Get the size of the powerset 2^APSet
   * @return the size of 2^AP
   */
	public int powersetSize() {
		return (1<<size());
	}

  /** 
   * Equality check.
   * @param other the other APSet
   * @return <b>true</b> if this and the other APSet are equal
   */
	public boolean equals(APSet other) 
	{
		return this.vector.equals(other.vector);
	}
	
	public boolean equals(Object other)
	{
		return ((other instanceof APSet) && this.equals((APSet) other));
	}

  /**
   * Create a new APSet with the same number of 
   * atomic propositions, but named 'p0', 'p1', 'p2', ...
   * The caller takes ownership of the memory of the created APSet.
   * @return APSet* to newly created APSet
   */
	public APSet createCanonical() {
		APSet canonical = new APSet();
    	for (int i=0; i < size(); i++)
    		canonical.addAP("p" + i);
    	return canonical;
	}
	
	public Iterator<String> iterator()
	{
		return vector.iterator();
	}
	
	public Iterator<APElement> elementIterator()
	{
		return new APElementIterator(vector.size());
	}
	
	public void print(PrintStream out) {
		for (int i = 0; i < size(); i++) {
			out.println(i + ": " + getAP(i));
		}
	}
	
	public String toString() {
		String rv = "{";
		for (Iterator<String> it = this.iterator(); it.hasNext(); ) {
			rv = rv + it.next();
			if (it.hasNext()) {
				rv = rv + ",";
			}
		}
		return rv + "}";
	}
}
