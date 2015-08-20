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

import java.util.BitSet;

/**
 * Class representing an element of 2^AP. 
 */
public class APElement extends MyBitSet {
	
	public APElement() {
		super();
	}
	
	public APElement(int size) {
		super(size);
	}
	
	public APElement(MyBitSet m) {
		super(m.size());
		for (Integer i : m) {
			this.set(i);
		}
	}
	
	public String toString(APSet ap_set, boolean spaces) 
	{
		if (ap_set.size()==0) {return "true";}
		String r = "";
		for (int i = 0; i < ap_set.size(); i++) {
			if (i>=1) {
				r += "&";
			}
			if (!this.get(i)) {
				r += "!";
			} else {
				if (spaces) {
					r +=" ";
				}
			}
			r += ap_set.getAP(i);
		}
		return r;
	}

	/**
	 * Return a string representation of this APElement in LBTT format (transition label)
	 * @param ap_set the underlying AP set
	 */
	public String toStringLBTT(APSet ap_set)
	{
		if (ap_set.size()==0) {return "t";}
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i < ap_set.size(); i++) {
			sb.append("& ");
		}
		for (int i = 0; i < ap_set.size(); i++) {
			if (!this.get(i)) {
				sb.append("!");
			}
			sb.append(ap_set.getAP(i));
			sb.append(" ");
		}
		return sb.toString();
	}

	/**
	 * Return a string representation of this APElement in HOA format (transition label)
	 * @param apSetSize the size of the underlying AP set
	 */
	public String toStringHOA(int apSetSize)
	{
		return toStringHOA(this, apSetSize);
	}

	/**
	 * Return a string representation of a transition label in HOA format,
	 * where the active/inactive APs are given as a BitSet.
	 * @param label the set of APs that are active
	 * @param apSetSize the size of the underlying AP set
	 */
	public static String toStringHOA(BitSet label, int apSetSize)
	{
		if (apSetSize == 0) {
			return "t";
		}

		StringBuilder sb = new StringBuilder();
		for (int i=0; i < apSetSize; i++) {
			if (i > 0) sb.append("&");
			if (label.get(i)) {
				sb.append(" "+i);
			} else {
				sb.append("!"+i);
			}
		}
		return sb.toString();
	}
}
