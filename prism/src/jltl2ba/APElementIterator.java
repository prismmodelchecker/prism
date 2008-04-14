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

import java.util.Iterator;
import java.util.NoSuchElementException;

public class APElementIterator implements Iterator<APElement> {
	
	private APElement it;
	private int _size;
	
	// Iterator linked to a bitset
	public APElementIterator(APElement ap_elem) {
		_size = ap_elem.size();
		it = new APElement(_size);
	}
	
	// Iterator to 2^size elements
	public APElementIterator(int size) {
		_size = size;
		it = new APElement(size);
	}
	
	public boolean hasNext() {
		return !it.get(_size); 
	}
	
	public APElement next() throws NoSuchElementException {
		if (hasNext()) {
			APElement tmp = (APElement) it.clone();
			it.increment();
			return tmp;
		}
		else throw new NoSuchElementException();
	}
	
	public void remove() throws UnsupportedOperationException
	{
		throw new UnsupportedOperationException();
	}
}
