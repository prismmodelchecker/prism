package parser.ast;

import org.junit.jupiter.api.Test;
import parser.State;
import prism.PrismLangException;
import simulator.LabelEvaluator;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static parser.ast.LabelTest.createLabelListWithDependencyChain;
import static parser.ast.LabelTest.createLabelListWithoutCycle;

public class LabelEvaluatorTest
{
	@Test
	public void testGetLabelValues() throws PrismLangException
	{
		/*
		creating LabelList with

		label "l2" = d<=3;
		label "l1" = "l2" | d=5;

		and checking for [0..5] of int d
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

	@Test
	public void testLabelChainEvaluation() throws PrismLangException
	{
		LabelList ll = createLabelListWithDependencyChain();
		LabelEvaluator labelEvaluator = new LabelEvaluator(ll);

		State state = new State(0);
		assertTrue(labelEvaluator.getLabelValue(state, "l1"));
		assertTrue(labelEvaluator.getLabelValue(state, "l2"));
		assertTrue(labelEvaluator.getLabelValue(state, "l3"));
	}

	@Test
	public void testLabelDFS_chain() throws PrismLangException
	{
		/*
		 * label "l0" = "l2"
		 * label "l2" = "l4"
		 * label "l4" = "l3"
		 * label "l3" = "l1"
		 */
		boolean[][] dependencies = new boolean[5][5];
		dependencies[0][2] = true;
		dependencies[2][4] = true;
		dependencies[4][3] = true;
		dependencies[3][1] = true;

		List<Integer> order = getEvaluationOrder(dependencies);

		assertTrue(order.indexOf(2) < order.indexOf(0));
		assertTrue(order.indexOf(4) < order.indexOf(2));
		assertTrue(order.indexOf(3) < order.indexOf(4));
		assertTrue(order.indexOf(1) < order.indexOf(3));
	}

	@Test
	public void testLabelDFS_unrelatedLabels() throws PrismLangException
	{
		// all false -- no dependencies
		boolean[][] dependencies = new boolean[5][5];

		List<Integer> order = getEvaluationOrder(dependencies);

		assertEquals(5, order.size());
	}

	@Test
	public void testLabelDFS_multipleCorrelationComponents() throws PrismLangException
	{
		/*
		 * label "l0" = "l1" & "l3"
		 * label "l3" = "l1"
		 *
		 * label "l4" = "l2" & "l5"
		 * label "l2" = "l6"
		 */
		boolean[][] dependencies = new boolean[7][7];
		dependencies[0][1] = dependencies[0][3] = true;
		dependencies[3][1] = true;
		dependencies[4][2] = dependencies[4][5] = true;
		dependencies[2][6] = true;

		List<Integer> order = getEvaluationOrder(dependencies);

		assertTrue(order.indexOf(1) < order.indexOf(0));
		assertTrue(order.indexOf(3) < order.indexOf(0));

		assertTrue(order.indexOf(1) < order.indexOf(3));

		assertTrue(order.indexOf(2) < order.indexOf(4));
		assertTrue(order.indexOf(5) < order.indexOf(4));

		assertTrue(order.indexOf(6) < order.indexOf(2));
	}

	private List<Integer> getEvaluationOrder(boolean[][] deps) throws PrismLangException
	{
		LabelEvaluator labelEvaluator = new LabelEvaluator(new LabelList());
		return Arrays.stream(labelEvaluator.fixEvaluationOrder(deps)).boxed().collect(Collectors.toList());
	}

}
