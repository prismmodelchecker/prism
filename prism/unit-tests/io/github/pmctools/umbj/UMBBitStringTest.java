//==============================================================================
//
//	Copyright (c) 2025-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

package io.github.pmctools.umbj;

import java.util.Random;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class UMBBitStringTest
{
	/**
	 * Reference implementation: extract bits one at a time
	 * (bit {@code offset} of the bit string is the least significant bit of the result).
	 */
	private static long referenceBits(byte[] bytes, int offset, int n)
	{
		long value = 0;
		for (int i = offset + n - 1; i >= offset; i--) {
			value = (value << 1) | ((bytes[i >> 3] & (1 << (i & 7))) != 0 ? 1 : 0);
		}
		return value;
	}

	@Test
	public void testGettersMatchReference() throws UMBException
	{
		Random random = new Random(42);
		int numBytes = 24;
		UMBBitString bitString = new UMBBitString(numBytes);
		for (int trial = 0; trial < 200; trial++) {
			random.nextBytes(bitString.bytes);
			for (int n = 1; n <= 64; n++) {
				for (int offset = 0; offset + n <= numBytes * 8; offset += 1 + random.nextInt(5)) {
					long raw = referenceBits(bitString.bytes, offset, n);
					long signed = n < 64 && (raw & (1L << (n - 1))) != 0 ? raw - (1L << n) : raw;
					if (n < 64) {
						assertEquals(raw, bitString.getULong(offset, n), "getULong(" + offset + "," + n + ")");
					}
					assertEquals(signed, bitString.getLong(offset, n), "getLong(" + offset + "," + n + ")");
					if (n < 32) {
						assertEquals((int) raw, bitString.getUInt(offset, n), "getUInt(" + offset + "," + n + ")");
						assertEquals(raw != 0, bitString.getBoolean(offset, n), "getBoolean(" + offset + "," + n + ")");
					}
					if (n <= 32) {
						assertEquals((int) signed, bitString.getInt(offset, n), "getInt(" + offset + "," + n + ")");
					}
					if (n == 64) {
						assertEquals(Double.doubleToRawLongBits(Double.longBitsToDouble(raw)), Double.doubleToRawLongBits(bitString.getDouble(offset, n)), "getDouble(" + offset + "," + n + ")");
					}
				}
			}
		}
	}

	@Test
	public void testSetGetRoundTrip() throws UMBException
	{
		UMBBitString bitString = new UMBBitString(16);
		// Values at the extremes of their ranges, at unaligned offsets
		bitString.setInt(3, 32, Integer.MIN_VALUE);
		assertEquals(Integer.MIN_VALUE, bitString.getInt(3, 32));
		bitString.setInt(3, 32, -1);
		assertEquals(-1, bitString.getInt(3, 32));
		bitString.setInt(5, 7, -64);
		assertEquals(-64, bitString.getInt(5, 7));
		bitString.setLong(7, 64, Long.MIN_VALUE + 12345);
		assertEquals(Long.MIN_VALUE + 12345, bitString.getLong(7, 64));
		bitString.setULong(9, 63, Long.MAX_VALUE);
		assertEquals(Long.MAX_VALUE, bitString.getULong(9, 63));
		bitString.setDouble(1, 64, -Math.PI);
		assertEquals(-Math.PI, bitString.getDouble(1, 64));
		bitString.setBoolean(70, 1, true);
		assertTrue(bitString.getBoolean(70, 1));
	}
}
