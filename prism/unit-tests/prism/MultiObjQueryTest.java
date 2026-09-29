package prism;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import parser.ast.RelOp;

public class MultiObjQueryTest
{
	@Test
	public void testIsUnboundedMinReward()
	{
		MultiObjQuery query = new MultiObjQuery();
		query.add(new OpRelOpBound("R", RelOp.MIN, null), Operator.R_MIN, -1.0, -1, 0, null);
		query.add(new OpRelOpBound("R", RelOp.LEQ, 5.0), Operator.R_LE, 5.0, -1, 1, null);
		query.add(new OpRelOpBound("R", RelOp.LEQ, 5.0), Operator.R_LE, 5.0, 3, 2, null);
		query.add(new OpRelOpBound("R", RelOp.GEQ, 1.0), Operator.R_GE, 1.0, -1, 3, null);
		// Same answers before and after minimising objectives are negated to maximising ones
		for (int pass = 0; pass < 2; pass++) {
			assertTrue(query.isUnboundedMinReward(0));
			assertTrue(query.isUnboundedMinReward(1));
			assertFalse(query.isUnboundedMinReward(2)); // step-bounded
			assertFalse(query.isUnboundedMinReward(3)); // maximising
			query.makeAllRewardUp();
		}
	}
}
