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
}