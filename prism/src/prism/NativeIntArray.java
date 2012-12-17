package prism;

/**
 * This class can be used to store {@code int} elements in an array.
 * @author vojfor
 *
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
