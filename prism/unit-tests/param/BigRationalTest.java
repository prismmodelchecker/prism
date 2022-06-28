package param;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BigRationalTest
{
	@Test
	public void testBigRationalDouble()
	{
		// constants
		assertEquals(BigRational.ZERO, new BigRational(0.0));
		assertEquals(BigRational.ONE, new BigRational(1.0));
		assertEquals(BigRational.ONE.negate(), new BigRational(-1.0));
		assertEquals(BigRational.HALF, new BigRational(0.5));
		assertEquals(BigRational.HALF.negate(), new BigRational(-0.5));

		// double +/- 0.1: +/- 0x1.999999999999ap-4
		BigRational pointOne = new BigRational(new BigInteger("1999999999999a", 16), BigInteger.ONE.shiftLeft(4+52));
		assertEquals(pointOne, new BigRational(0.1));
		assertEquals(pointOne.negate(), new BigRational(-0.1));

		// largest/smallest fraction: +/- 0x1.fffffffffffffp51
		BigRational maxFrac = new BigRational(new BigInteger("1fffffffffffff", 16), BigInteger.TWO);
		assertEquals(maxFrac, new BigRational(0x1.fffffffffffffp51));
		assertEquals(maxFrac.negate(), new BigRational(-0x1.fffffffffffffp51));

		// largest/smallest non-fraction: +/- 1.0p52
		BigRational nonFrac = new BigRational(new BigInteger("10000000000000", 16));
		assertEquals(nonFrac, new BigRational(0x1.0p52));
		assertEquals(nonFrac.negate(), new BigRational(-0x1.0p52));

		// largest/smallest integer: +/- 0x1.fffffffffffffp+1023
		BigRational maxInt = new BigRational(new BigInteger("1fffffffffffff", 16).shiftLeft(1023-52));
		assertEquals(maxInt, new BigRational(Double.MAX_VALUE));
		assertEquals(maxInt.negate(), new BigRational(-Double.MAX_VALUE));

		// smallest absolute value:  +/- 0x0.0000000000001p-1022
		BigRational absMin = new BigRational(BigInteger.ONE, BigInteger.ONE.shiftLeft(1022+52));
		assertEquals(absMin, new BigRational(Double.MIN_VALUE));
		assertEquals(absMin.negate(), new BigRational(-Double.MIN_VALUE));
	}
}
