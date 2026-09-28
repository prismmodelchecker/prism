package prism;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import parser.ast.RelOp;

public class PointTest
{
	@Test
	public void testToRealPropertiesNegatedZero()
	{
		// Two minimised reward objectives, which are stored negated in solver space
		MultiObjQuery query = new MultiObjQuery();
		query.add(new OpRelOpBound("R", RelOp.MIN, null), Operator.R_MIN, -1.0, -1, 0, null);
		query.add(new OpRelOpBound("R", RelOp.MIN, null), Operator.R_MIN, -1.0, -1, 1, null);
		query.makeAllRewardUp();
		// (assertEquals on doubles distinguishes 0.0 from -0.0)
		for (double zero : new double[] { 0.0, -0.0 }) {
			Point real = new Point(new double[] { -3.0, zero }).toRealProperties(query);
			assertEquals(3.0, real.getCoord(0));
			assertEquals(0.0, real.getCoord(1));
			assertEquals("(3.0, 0.0)", real.toString());
		}
	}
}
