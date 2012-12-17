//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Vojtech Forejt <vojtech.forejt@cs.ox.ac.uk> (University of Oxford)
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

package prism;

/**
 * Class to store {@code int} elements in an array.
 */
public class NativeIntArray 
{
	private static native long NIA_CreateArray(int size);
	private static native void NIA_DeleteArray(long ptr);
	private static native int NIA_Get(long ptr, int index);
	private static native void NIA_Set(long ptr, int index, int value);
	private static native void NIA_SetAll(long ptr, int indexFrom, int count, int value);
	
	static
	{
		try {
			System.loadLibrary("prism");
		}
		catch (UnsatisfiedLinkError e) {
			System.out.println(e);
			System.exit(1);
		}
	}
	
	private long ptr;
	private int size;
	
	/**
	 * Creates a new native array of size s, and keeps it wrapped in this object.
	 * Note that the elements of the array may not be initialised to 0. Use
	 * {@link #setAllTo(int)} to initialise the array.
	 * @param size The size of the array to be created.
	 */
	public NativeIntArray(int size)
	{
		this.size = size;
		this.ptr = NIA_CreateArray(size);
	}
	
	/**
	 * Returns the pointer to the wrapped native array.
	 * @return Pointer
	 */
	public long getPtr()
	{
		return ptr;
	}
	
	/**
	 * Deletes the native array and frees the memory.
	 */
	public void clear()
	{
		NIA_DeleteArray(ptr);
	}
	
	/**
	 * Returns the {@code index}-th element of the aray
	 * @param index
	 * @return
	 * @throws IndexOutOfBoundsException If the index is outside of the bounds of the array
	 */
	public int get(int index) throws IndexOutOfBoundsException
	{
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException();
		return NIA_Get(ptr, index);
	}
	
	/**
	 * Sets the {@code index}-th element of the array to {@code val}.
	 * @param index
	 * @return
	 * @throws IndexOutOfBoundsException If the index is outside of the bounds of the array
	 */
	public void set(int index, int value) throws IndexOutOfBoundsException
	{
		if (index < 0 || index >= size)
			throw new IndexOutOfBoundsException();
		NIA_Set(ptr, index, value);
	}

	/**
	 * Sets all elements of the array to {@code value}.
	 * @param value Value to be set
	 */
	public void setAllTo(int value) {
		NIA_SetAll(ptr, 0, size, value);
	}
	
	/**
	 * Returns the size of the wrapped array.
	 */
	public int size()
	{
		return this.size;
	}
}
