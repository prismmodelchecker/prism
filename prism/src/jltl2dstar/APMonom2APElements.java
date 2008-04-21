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

import java.util.Iterator;
import java.util.NoSuchElementException;

import jltl2ba.APElement;
import jltl2ba.APSet;
import jltl2ba.MyBitSet;

/**
 * Iterator over all APElements that are represented by an APMonom.
 * As every APMonom represents a subset of 2^AP, this allows access
 * to all of the members of this subset.<br>
 */
public class APMonom2APElements implements Iterator<APElement> {
	
	/** The underlying APSet. */
	private APSet _ap_set;    
	  
	/** The underlying APMonom. */
	private APMonom _m;       

	/** The current APElement. */
	private APElement _cur_e;
	
	/** Bits that are set */
	private MyBitSet set_mask;

	/** Marker, true if end was reached */
	private boolean _end_marker;

	public APMonom2APElements(APSet s, APMonom m)
	{
		_m = m;
		_ap_set = s;
		_cur_e = new APElement(m.getValueBits());
		_end_marker = m.isFalse();
		
		if (m.isTrue()) {
			set_mask = new MyBitSet(_ap_set.size());
		}
		else {
			set_mask = _m.getSetBits();
		}
	}
	
	private void increment() {
	    for (int i = set_mask.nextClearBit(0); i < _ap_set.size(); i = set_mask.nextClearBit(i+1)) {
	    	if (_cur_e.get(i) == false) {
	    		_cur_e.set(i, true);
	    		return;
	    	} else {
	    		_cur_e.set(i, false);
	    	}
	    }
	    // overflow -> end
	    _end_marker = true;
	}
	
	public boolean hasNext()
	{
		return !_end_marker;
	}
	
	public APElement next() throws NoSuchElementException
	{
		if (hasNext()) {
			APElement rv = (APElement) _cur_e.clone();
			increment();
			return rv;
		}
		else throw new NoSuchElementException();
	}
	
	public void remove() throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}
}
