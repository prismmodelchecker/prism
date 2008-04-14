/*
 * This file is part of a Java port of the program ltl2dstar
 * (http://www.ltl2dstar.de/) for PRISM (http://www.prismmodelchecker.org/)
 * Copyright (C) 2005-2007 Joachim Klein <j.klein@ltl2dstar.de>
 * Copyright (c) 2007 Carlos Bederian
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as 
 *  published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package jltl2dstar;

import jltl2ba.MyBitSet;

/** A class storing the acceptance signature for a state
 * (for every acceptance pair one color).
 */
public class RabinSignature implements Comparable<RabinSignature> {

	/** The L part */
    private MyBitSet _L;
    /** The U part */
    private MyBitSet _U;    
    /** The number of acceptance pairs */
    private int _size;
    
	/** Constructor 
	 * @param size the number of acceptance pairs 
	 */
	public RabinSignature(int size) {
		_size = size;
		_L = new MyBitSet(size);
		_U = new MyBitSet(size);
	}

	/** Constructor
	 * @param other another RabinSignature
	 */
	public RabinSignature(RabinSignature other) {
		_L = (MyBitSet) other._L.clone();
		_U = (MyBitSet) other._U.clone();
		_size = other._size;
	}

	/** Constructor
	 * @param L the L part of the acceptance signature.
	 * @param U the U part of the acceptance signature.
	 * @param size the number of acceptance pairs
	 */
	public RabinSignature(MyBitSet L, MyBitSet U, int size) { 
	  _L = L;
	  _U = U;
	  _size = size;
	}
	    
	/** Constructor for getting the acceptance signature for a Tree.
	 * @param tree the Tree, get acceptance signature from 
	 *    tree.generateAcceptance(*this).
	 */
	public <Tree extends SafraTree> RabinSignature(Tree tree) {
		this(0);
		tree.generateAcceptance(this);
	}

	/** Clear the acceptance signature */
	public void clear() {
		_L.clear();
		_U.clear();
	}

	/** Get the L part of this acceptance signature */
	public MyBitSet getL() {return _L;}    
	/** Get the U part of this acceptance signature */
	public MyBitSet getU() {return _U;}

	/** Set index to value in the L part of this acceptance signature. */
	public void setL(int index, boolean value) {
		_L.set(index, value);
	}

	/** Set index to value. in the U part of this acceptance signature. */
	public void setU(int index, boolean value) {
		_U.set(index, value);
	}

	/** Set the L and U parts according to RabinColor c. 
	 * @param i The pair index
	 * @param c the RabinColor
	 */
	public void setColor(int i, RabinAcceptance.RabinColor c) {
		switch (c) {
		case RABIN_RED:
			_U.set(i, true);
			_L.set(i, false);
			break;

		case RABIN_GREEN:
			_U.set(i, false);
			_L.set(i, true);
			break;

		case RABIN_WHITE:
			_U.set(i, false);
			_L.set(i, false);
			break;
		}
	}

	/** Get the RabinColor for a pair i */
	public RabinAcceptance.RabinColor getColor(int i) {
		return _U.get(i) ? RabinAcceptance.RabinColor.RABIN_RED : (_L.get(i) ? RabinAcceptance.RabinColor.RABIN_GREEN : RabinAcceptance.RabinColor.RABIN_WHITE);
	}

	/** Get string representation of this signature. */
	public String toString() {
		String a;
		a="{";
		for (int i = 0; i < size(); i++) {
			switch (getColor(i)) {
			case RABIN_RED:
				a += "-" + i;
				break;
			case RABIN_GREEN:
				a += "+" + i;
				break;
			case RABIN_WHITE:
				break;
			}
		}
		a += "}";
	      
		return a;
	}

	    /** Compare to other signature for equality. */
	public boolean equals(RabinSignature other) {
		return (_L.equals(other.getL()) &&  _U.equals(other.getU()));
	}
	
	public boolean equals(Object other)	{
		return ((other instanceof RabinSignature) && this.equals((RabinSignature) other));
	}

	/** Compare to other signature */
	public int compareTo(RabinSignature other) {
		int i = _L.compareTo(other.getL());
		if (i != 0)
			return i;
		else 
			return _U.compareTo(other.getU());
	}
	
	/** Get the number of acceptance pairs */
	public int getSize() {return _size;}
	/** Get the number of acceptance pairs */
	public int size() {return _size;}

	/** Set the number of acceptance pairs */
	public void setSize(int size) {_size=size;}

	/** Merge this acceptance signature with other signature,
	 *  for each tuple element calculate the maximum of the
	 *  colors according to the order 
	 * RABIN_WHITE < RABIN_GREEN < RABIN_RED */
	public void maxMerge(RabinSignature other) {
		for (int i = 0; i < _size; i++) {
			if (getColor(i).compareTo(other.getColor(i)) < 0) {
				setColor(i, other.getColor(i));
			}
		}
	}

	/**
	 * Calculate a hash value using HashFunction
	 * @param hashfunction the HashFunction
	 */
	// public int hashCode() {
	// return _L.hashCode() + _U.hashCode();
	// }
}
