package parser.ast;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import parser.State;
import parser.type.TypeInt;
import prism.PrismLangException;
import simulator.LabelEvaluator;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class LabelTest
{
	@Nested
	class LabelListTest
	{
		@Test
		public void testFindCycles_emptyList()
		{
			LabelList ll = new LabelList();
			assertDoesNotThrow(ll::findCycles);
		}

		@Test
		public void testFindCycles_1LabelCycle()
		{
			LabelList ll = createLabelListWith1LabelCycle();
			assertThrows(PrismLangException.class, ll::findCycles);
		}

		@Test
		public void testFindCycles_2LabelCycle()
		{
			LabelList ll = createLabelListWith2LabelCycle();
			assertThrows(PrismLangException.class, ll::findCycles);
		}

		@Test
		public void testFindCycles_noCycle()
		{
			LabelList ll = createLabelListWithoutCycle();
			assertDoesNotThrow(ll::findCycles);
		}
	}

	@Nested
	class LabelEvaluterTest
	{
		@Test
		public void testGetLabelValues() throws PrismLangException
		{
			/*
			d is part of the state and will go from [0..5]

			label "l1" = "l2" | d=5
			label "l2" = d<=3
			 */
			LabelList ll = createLabelListWithoutCycle();
			LabelEvaluator labelEvaluator = new LabelEvaluator(ll);

			// loop through all states and check label values
			for (int i = 0; i <= 5; i++) {
				// create the new state with 1 variable
				State state = new State(1);
				// set d to [0..5]
				state.setValue(0, i); // index of d is 0

				// expected labelValues
				boolean l2 = i <= 3;
				boolean l1 = l2 || i == 5;

				// compare
				Predicate<String> actualValues = labelEvaluator.getLabelValues(state);
				assertEquals(l1, actualValues.test("l1"));
				assertEquals(l2, actualValues.test("l2"));
			}
		}
	}

	private LabelList createLabelListWithoutCycle()
	{
		/*
		label "l1" = "l2" | d=5
		label "l2" = d<=3
		*/
		LabelList ll = new LabelList();
		ExpressionVar dVar = new ExpressionVar("d", TypeInt.getInstance());
		dVar.setIndex(0); // index of d is 0
		ll.addLabel(new ExpressionIdent("l1"), new ExpressionBinaryOp(ExpressionBinaryOp.OR,
				new ExpressionLabel("l2"), new ExpressionBinaryOp(ExpressionBinaryOp.EQ,
				dVar, new ExpressionLiteral(TypeInt.getInstance(), 5))));
		ll.addLabel(new ExpressionIdent("l2"), new ExpressionBinaryOp(ExpressionBinaryOp.LE,
				dVar, new ExpressionLiteral(TypeInt.getInstance(), 3)));
		return ll;
	}

	private LabelList createLabelListWith1LabelCycle()
	{
		LabelList ll = new LabelList();
		/* label "l1" = "l1" */
		ll.addLabel(new ExpressionIdent("l1"), new ExpressionLabel("l1"));
		return ll;
	}

	private LabelList createLabelListWith2LabelCycle()
	{
		LabelList ll = new LabelList();
		/*
		label "l1" = "l2"
		label "l2" = "l1" | d=5
		 */
		ll.addLabel(new ExpressionIdent("l1"), new ExpressionLabel("l2"));
		ll.addLabel(new ExpressionIdent("l2"), new ExpressionBinaryOp(ExpressionBinaryOp.OR,
				new ExpressionLabel("l1"), new ExpressionBinaryOp(ExpressionBinaryOp.EQ,
				new ExpressionVar("d", TypeInt.getInstance()), new ExpressionLiteral(TypeInt.getInstance(), 5))));
		return ll;
	}
}
