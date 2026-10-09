/*
 * Copyright 2025 Dave Parker (University of Oxford)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pmctools.umbj;

/**
 * Class representing a bit string, allowing portions of it to be read/written as various data types.
 */
public class UMBBitString
{
	/** Bytes storing the bit string */
	public byte[] bytes;

	/**
	 * Construct a new UMBBitString object, with space for the given number of bytes.
	 * @param numBytes The number of bytes to allocate
	 */
	public UMBBitString(int numBytes)
	{
		bytes = new byte[numBytes];
	}

	/**
	 * Store the value of an {@code n}-bit (signed) integer in a portion of the bit string.
	 * This assumes that the integer needs at most 32 bits, so that it can be stored in an {@code int}.
	 * @param offset The first bit of the bitstring portion
	 * @param n The size (in bits) of the bitstring portion
	 * @param value The value to store
	 */
	public void setInt(int offset, int n, int value) throws UMBException
	{
		if (n > 32) {
			throw new UMBException("Cannot store integer of " + n + " bits (too large for Java int)");
		}
		setBits(offset, n, value);
	}

	/**
	 * Store the value of an {@code n}-bit unsigned integer in a portion of the bit string.
	 * This assumes that the integer needs at most 32 bits, so that it can be stored in an {@code int}.
	 * @param offset The first bit of the bitstring portion
	 * @param n The size (in bits) of the bitstring portion
	 * @param value The value to store
	 */
	public void setUInt(int offset, int n, int value) throws UMBException
	{
		if (n >= 32) {
			throw new UMBException("Cannot store unsigned integer of " + n + " bits (too large for Java int)");
		}
		// Storing (including bit truncation) is the same as for signed integers
		setInt(offset, n, value);
	}

	/**
	 * Store the value of an {@code n}-bit (signed) integer in a portion of the bit string.
	 * This assumes that the integer needs at most 64 bits, so that it can be stored in a {@code long}.
	 * @param offset The first bit of the bitstring portion
	 * @param n The size (in bits) of the bitstring portion
	 * @param value The value to store
	 */
	public void setLong(int offset, int n, long value) throws UMBException
	{
		if (n > 64) {
			throw new UMBException("Cannot store integer of " + n + " bits (too large for Java long)");
		}
		setBits(offset, n, value);
	}

	/**
	 * Store the value of an {@code n}-bit unsigned integer in a portion of the bit string.
	 * This assumes that the integer needs at most 64 bits, so that it can be stored in a {@code long}.
	 * @param offset The first bit of the bitstring portion
	 * @param n The size (in bits) of the bitstring portion
	 * @param value The value to store
	 */
	public void setULong(int offset, int n, long value) throws UMBException
	{
		if (n >= 64) {
			throw new UMBException("Cannot store unsigned integer of " + n + " bits (too large for Java long)");
		}
		// Storing (including bit truncation) is the same as for signed integers
		setLong(offset, n, value);
	}

	/**
	 * Store the value of a (64-bit) double in a portion of the bit string.
	 * @param offset The first bit of the bitstring portion
	 * @param n The size (in bits) of the bitstring portion (should be 64)
	 * @param value The value to store
	 */
	public void setDouble(int offset, int n, double value) throws UMBException
	{
		if (n != 64) {
			throw new UMBException("Cannot store double of " + n + " bits (should be 64)");
		}
		setBits(offset, n, Double.doubleToLongBits(value));
	}

	/**
	 * Store the value of a boolean in a portion of the bit string.
	 * The boolean is stored as an {@code n}-bit unsigned integer and is false iff == 0
	 * @param offset The first bit of the bitstring portion
	 * @param n The size (in bits) of the bitstring portion
	 * @param value The value to store
	 */
	public void setBoolean(int offset, int n, boolean value) throws UMBException
	{
		if (n >= 32) {
			throw new UMBException("Cannot store unsigned integer of " + n + " bits (too large for Java int)");
		}
		setUInt(offset, n, value ? 1 : 0);
	}

	/**
	 * Get the value of an {@code n}-bit (signed) integer, extracted from a portion of the bit string.
	 * If the value does not fit into a standard Java (32-bit) signed int, an exception is thrown.
	 * @param offset The first bit of the bitstring portion
	 * @param n The size (in bits) of the bitstring portion
	 */
	public int getInt(int offset, int n) throws UMBException
	{
		if (n > 32) {
			throw new UMBException("Cannot extract integer of " + n + " bits (too large for Java int)");
		}
		// Extract bits and sign extend
		return (int) signExtend(getBits(offset, n), n);
	}

	/**
	 * Get the value of an {@code n}-bit unsigned integer, extracted from a portion of the bit string.
	 * If the value does not fit into a standard Java (32-bit) signed int, an exception is thrown.
	 * @param offset The first bit of the bitstring portion
	 * @param n The size (in bits) of the bitstring portion
	 */
	public int getUInt(int offset, int n) throws UMBException
	{
		if (n >= 32) {
			throw new UMBException("Cannot extract unsigned integer of " + n + " bits (too large for Java int)");
		}
		return (int) getBits(offset, n);
	}

	/**
	 * Get the value of an {@code n}-bit (signed) integer, extracted from a portion of the bit string.
	 * If the value does not fit into a standard Java (64-bit) signed long, an exception is thrown.
	 * @param offset The first bit of the bitstring portion
	 * @param n The size (in bits) of the bitstring portion
	 */
	public long getLong(int offset, int n) throws UMBException
	{
		if (n > 64) {
			throw new UMBException("Cannot extract integer of " + n + " bits (too large for Java long)");
		}
		// Extract bits and sign extend
		return signExtend(getBits(offset, n), n);
	}

	/**
	 * Get the value of an {@code n}-bit unsigned integer, extracted from a portion of the bit string.
	 * If the value does not fit into a standard Java (64-bit) signed long, an exception is thrown.
	 * @param offset The first bit of the bitstring portion
	 * @param n The size (in bits) of the bitstring portion
	 */
	public long getULong(int offset, int n) throws UMBException
	{
		if (n >= 64) {
			throw new UMBException("Cannot extract unsigned integer of " + n + " bits (too large for Java long)");
		}
		return getBits(offset, n);
	}

	/**
	 * Get the value of a (64-bit) double, extracted from a portion of the bit string.
	 * @param offset The first bit of the bitstring portion
	 * @param n The size (in bits) of the bitstring portion (should be 64)
	 */
	public double getDouble(int offset, int n) throws UMBException
	{
		if (n != 64) {
			throw new UMBException("Cannot extract double of " + n + " bits (should be 64)");
		}
		return Double.longBitsToDouble(getBits(offset, n));
	}

	/**
	 * Get the value of a boolean, extracted from a portion of the bit string.
	 * The boolean is stored as an {@code n}-bit unsigned integer and is false iff == 0
	 * @param offset The first bit of the bitstring portion
	 * @param n The size (in bits) of the bitstring portion
	 */
	public boolean getBoolean(int offset, int n) throws UMBException
	{
		if (n >= 32) {
			throw new UMBException("Cannot extract boolean of " + n + " bits (too large for Java int)");
		}
		return getUInt(offset, n) != 0;
	}

	/**
	 * Store the low {@code n} bits of a {@code long} in a portion of the bit string
	 * (the least significant bit goes to bit {@code offset} of the bit string).
	 * Other bits of the bit string are left unchanged.
	 * @param offset The first bit of the bitstring portion
	 * @param n The size (in bits) of the bitstring portion (at most 64)
	 * @param value The bits to store
	 */
	private void setBits(int offset, int n, long value)
	{
		// Write (part of) a byte at a time
		while (n > 0) {
			int byteIndex = offset >> 3;
			int shift = offset & 7;
			int numBits = Math.min(8 - shift, n);
			int mask = ((1 << numBits) - 1) << shift;
			bytes[byteIndex] = (byte) ((bytes[byteIndex] & ~mask) | (((int) value << shift) & mask));
			value >>>= numBits;
			offset += numBits;
			n -= numBits;
		}
	}

	/**
	 * Get the bits from a portion of the bit string, as the low {@code n} bits of a {@code long}
	 * (bit {@code offset} of the bit string becomes the least significant bit). Higher bits are zero.
	 * @param offset The first bit of the bitstring portion
	 * @param n The size (in bits) of the bitstring portion (at most 64)
	 */
	private long getBits(int offset, int n)
	{
		if (n == 0) {
			return 0;
		}
		int firstByte = offset >> 3;
		int shift = offset & 7;
		// Number of bytes spanned (up to 9, for 64 bits not aligned to a byte)
		int numBytes = ((offset + n - 1) >> 3) - firstByte + 1;
		// Assemble (up to) the first 8 bytes, little-endian
		long value = 0;
		for (int b = Math.min(numBytes, 8) - 1; b >= 0; b--) {
			value = (value << 8) | (bytes[firstByte + b] & 0xFF);
		}
		value >>>= shift;
		// Add any bits from a 9th byte
		if (numBytes > 8) {
			value |= (long) (bytes[firstByte + 8] & 0xFF) << (64 - shift);
		}
		// Mask off bits beyond the portion
		if (n < 64) {
			value &= (1L << n) - 1;
		}
		return value;
	}

	/**
	 * Sign extend the {@code n}-bit two's complement value stored in the low bits of {@code value}.
	 */
	private static long signExtend(long value, int n)
	{
		return n == 0 ? 0 : (value << (64 - n)) >> (64 - n);
	}

	/**
	 * Create a string representation of a portion of the bit string.
	 * @param offset The first bit of the portion
	 * @param size The size (in bits) of the portion
	 */
	public String toString(int offset, int size)
	{
		StringBuilder sb = new StringBuilder();
		int end = offset + size;
		for (int i = end - 1; i >= offset; i--) {
			sb.append((bytes[i >> 3] & (1L << (i & 7))) != 0 ? "1" : "0");
		}
		return sb.toString();
	}

	@Override
	public String toString()
	{
		return toString(0, bytes.length * 8);
	}
}
