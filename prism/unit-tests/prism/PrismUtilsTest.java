package prism;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static prism.PrismUtils.formatDouble;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class PrismUtilsTest
{
	@Test
	public void testCompareVersions()
	{
		 String v[] =  { "1", "2.0", "2.1.alpha", "2.1.alpha.r5555", "2.1.alpha.r5557", "2.1.beta", "2.1.beta4", "2.1", "2.1.dev", "2.1.dev.r6666", "2.1.dev1", "2.1.dev2", "2.1.2", "2.9", "3", "3.4"};
		 for (int i = 0; i < v.length; i++) {
			 for (int j = 0; j < v.length; j++) {
				 int d = PrismUtils.compareVersions(v[i], v[j]);
				 assertEquals(d, Integer.compare(i, j));
			 }
		 }
	}

	@Test
	public void testFormatDouble()
	{
		// Special values
		assertEquals("NaN", formatDouble(Double.NaN));
		assertEquals("-Infinity", formatDouble(Double.NEGATIVE_INFINITY));
		assertEquals("Infinity", formatDouble(Double.POSITIVE_INFINITY));
		// Rounding if number of significant digits > precision
		assertEquals("123457", formatDouble(6, 123456.7));
		assertEquals("1.23457e-05", formatDouble(6, 0.00001234567));
		// Small numbers
		assertEquals("0.0001", formatDouble(6, 0.0001));
		assertEquals("1e-05", formatDouble(6, 0.00001));
		assertEquals("1.23456e-05", formatDouble(6, 0.0000123456));
		// Big numbers
		assertEquals("999999", formatDouble(6, 999999.0));
		assertEquals("1e+06", formatDouble(6, 999999.9));
		assertEquals("1.23457e+06", formatDouble(6, 1234567.8));
	}

	@ParameterizedTest
	@ValueSource(doubles = {Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.MIN_VALUE, Double.MIN_NORMAL, Double.MAX_VALUE})
	public void testFormatDoubleDefaultPrecision(double d)
	{
		String serialized = PrismUtils.formatDouble(d);
		double parsed = Double.parseDouble(serialized);
		assertEquals(d, parsed);
	}
}
