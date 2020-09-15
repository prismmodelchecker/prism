package prism;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

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
}
