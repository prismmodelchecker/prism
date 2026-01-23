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
	 * @param offset The first bit of the bitstring portion
	 * @param n The size (in bits) of the bitstring portion
	 * @param value The value to store
	 */
	public void setInt(int offset, int n, int value) throws UMBException
	{
		if (n > 32) {
			throw new UMBException("Cannot store integer of " + n + " bits (too large for Java int)");
		}
		// Store bits into the byte array
		for (int i = 0; i < n; i++) {
			// Copy the i-th (least significant) bit of value to the appropriate bit in bytes
			char valueBitShifted = (char) (((value >>> i) & 1) << ((offset + i) & 7));
			char byteMask = (char) ('\u0001' << ((offset + i) & 7));
			bytes[(offset + i) >> 3] = (byte) ((bytes[(offset + i) >> 3] & ~(byteMask)) | (valueBitShifted));
		}
	}

	/**
	 * Store the value of an {@code n}-bit unsigned integer in a portion of the bit string.
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
		// Store bits in a long and then copy to the byte array
		long valueLong = Double.doubleToLongBits(value);
		for (int i = 0; i < n; i++) {
			// Copy the i-th (least significant) bit of valueLong to the appropriate bit in bytes
			char valueBitShifted = (char) (((valueLong >>> i) & 1) << ((offset + i) & 7));
			char byteMask = (char) ('\u0001' << ((offset + i) & 7));
			bytes[(offset + i) >> 3] = (byte) ((bytes[(offset + i) >> 3] & ~(byteMask)) | (valueBitShifted));
		}
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
		// Extract bits into an int
		int value = 0;
		for (int i = offset + n - 1; i >= offset; i--) {
			value = (value << 1) | ((bytes[i >> 3] & (1L << (i & 7))) != 0 ? 1 : 0);
		}
		// Sign extend if necessary
		if ((value & (1 << (n - 1))) != 0) {
			value -= (1 << n);
		}
		return value;
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
		// Extract bits into an int
		int value = 0;
		for (int i = offset + n - 1; i >= offset; i--) {
			value = (value << 1) | ((bytes[i >> 3] & (1L << (i & 7))) != 0 ? 1 : 0);
		}
		return value;
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
		// Extract bits into a long
		long value = 0;
		for (int i = offset + n - 1; i >= offset; i--) {
			value = (value << 1) | ((bytes[i >> 3] & (1L << (i & 7))) != 0 ? 1 : 0);
		}
		return Double.longBitsToDouble(value);
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
