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
import java.util.Vector;
import java.util.Iterator;
import java.io.PrintStream;


public class MyBitSet extends BitSet implements Comparable<BitSet>, Iterable<Integer> {

	private int _size;	// workaround for default bitset size, resizing breaks stuff later!
	
	public MyBitSet()
	{
		super();
		_size = 0;
	}

	public MyBitSet(int size)
	{
		super(size);
		if (size > 0)
			_size = size;
		else _size = 0;
	}
	
	public int size() {
		if (_size > 0)
			return _size;
		else return super.size();
	}

	public boolean containsAll(BitSet b) {
		BitSet tmp = (BitSet)this.clone();
		tmp.or(b);
		return tmp.equals(this);
	}

	// ugly
	public Vector<Integer> IntegerList()
	{
		Vector<Integer> tmp = new Vector<Integer>();
		for (int i = 0; i < this.size(); i++)
			if (this.get(i))
				tmp.add(new Integer(i));
		return tmp;
	}

	public void print(PrintStream out)
	{				/* prints the content of a set */
		int i;
		boolean start = true;

		out.print("{");
		for (i = 0; i < size(); i++) {
			if (get(i)) {
				if (!start)
					out.print(",");
				out.print(i);

				start = false;
			}
		}
		out.print("}");
	}

	public void print(PrintStream out, APSet symtable, boolean pos)
	{				/* prints the content of an ap set */
		int i;
		boolean start = true;
		for (i = 0; i < size(); i++) {
			if (get(i)) {
				if (!start)
					out.print(" & ");
				if (!pos)
					out.print("!");
				out.print(symtable.getAP(i));
				start = false;
			}
		}
	}

	public int compareTo(BitSet o)
	{
		BitSet tmp1 = (BitSet) this.clone();
		tmp1.andNot(o);
		BitSet tmp2 = (BitSet) o.clone();
		tmp2.andNot(this);
		return (tmp1.length() - tmp2.length());
	}
	
	public void increment() {
		int i = this.nextClearBit(0);
		this.clear(0,i);
		this.set(i);
	}

	/* An iterator over the set bits of a bitset */
	public Iterator<Integer> iterator() {
		return new MyBitSetIterator(this);
	}

	public static class MyBitSetIterator implements Iterator<Integer> {

		private MyBitSet _bitset;
		private int index;
		
		public MyBitSetIterator(MyBitSet bitset) {
			_bitset = bitset;
			index = _bitset.nextSetBit(0);
		}
		
		public boolean hasNext() {
			return (index >= 0);
		}
		
		public Integer next() {
			Integer rv = new Integer(index);
			index = _bitset.nextSetBit(index + 1);
			return rv;
		}
		
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
