package strat;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import explicit.Distribution;
import explicit.MDPSimple;
import prism.Evaluator;
import strat.StrategyInfo.UndefinedReason;

/**
 * Tests for reasons why strategy choices are undefined ({@link StrategyInfo.UndefinedReason}),
 * as stored (as negative codes) in strategies with arrays of choice indices.
 */
public class UndefinedReasonTest
{
	/** MDP with 2 states, each with a single self-loop */
	private MDPSimple<Double> buildMDP()
	{
		MDPSimple<Double> mdp = new MDPSimple<>(2);
		mdp.addInitialState(0);
		for (int s = 0; s < 2; s++) {
			Distribution<Double> distr = new Distribution<>(Evaluator.forDouble());
			distr.add(s, 1.0);
			mdp.addActionLabelledChoice(s, distr, "a");
		}
		return mdp;
	}

	@Test
	public void codesRoundTrip()
	{
		for (UndefinedReason reason : UndefinedReason.values()) {
			assertTrue(reason.getCode() < 0);
			assertEquals(reason, UndefinedReason.fromCode(reason.getCode()));
		}
		assertNull(UndefinedReason.fromCode(0));
		assertEquals(UndefinedReason.UNKNOWN, UndefinedReason.fromCode(-1000));
	}

	@Test
	public void memorylessArray()
	{
		MDPSimple<Double> mdp = buildMDP();
		MDStrategyArray<Double> strat = new MDStrategyArray<>(mdp, new int[] { 0, UndefinedReason.SATISFIED.getCode() });
		assertNull(strat.whyUndefined(0, -1));
		assertEquals(UndefinedReason.SATISFIED, strat.whyUndefined(1, -1));
		assertFalse(strat.isChoiceDefined(1));
	}

	@Test
	public void stepBounded()
	{
		MDPSimple<Double> mdp = buildMDP();
		FMDStrategyStep<Double> strat = new FMDStrategyStep<>(mdp, 1);
		strat.setStepChoices(0, new int[] { 0, UndefinedReason.UNSATISFIABLE.getCode() });
		assertNull(strat.whyUndefined(0, 0));
		assertEquals(UndefinedReason.UNSATISFIABLE, strat.whyUndefined(1, 0));
		// After the step bound
		assertEquals(UndefinedReason.HORIZON, strat.whyUndefined(0, 1));
		// ... unless there is a (memoryless) tail strategy
		strat.setTail(new MDStrategyArray<>(mdp, new int[] { 0, UndefinedReason.ARBITRARY.getCode() }));
		assertNull(strat.whyUndefined(0, 1));
		assertEquals(0, strat.getChoiceIndex(0, 1));
		assertEquals(UndefinedReason.ARBITRARY, strat.whyUndefined(1, 1));
	}
	@Test
	public void randomised()
	{
		MDPSimple<Double> mdp = buildMDP();
		MRStrategy<Double> strat = new MRStrategy<>(mdp);
		strat.setChoiceProbability(0, 0, 1.0);
		assertNull(strat.whyUndefined(0, -1));
		// No choice probabilities: UNKNOWN unless a reason is set
		assertEquals(UndefinedReason.UNKNOWN, strat.whyUndefined(1, -1));
		strat.setUndefinedReason(1, UndefinedReason.UNREACHABLE);
		assertEquals(UndefinedReason.UNREACHABLE, strat.whyUndefined(1, -1));
		assertEquals("?unreachable", strat.getUndefinedChoiceString(1, -1));
	}

	@Test
	public void randomisedUnlabelledActions()
	{
		MDPSimple<Double> mdp = new MDPSimple<>(1);
		for (int i = 0; i < 2; i++) {
			Distribution<Double> distr = new Distribution<>(Evaluator.forDouble());
			distr.add(0, 1.0);
			mdp.addActionLabelledChoice(0, distr, i == 0 ? null : "b");
		}
		MRStrategy<Double> strat = new MRStrategy<>(mdp);
		strat.setChoiceProbability(0, 0, 0.5);
		strat.setChoiceProbability(0, 1, 0.5);
		// Unlabelled actions are shown as "", not "null"
		assertEquals("0.5:+0.5:b", strat.getChoiceAction(0, -1).toString());
	}
}
