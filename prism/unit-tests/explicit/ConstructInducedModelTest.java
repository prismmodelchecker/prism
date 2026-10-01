package explicit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import prism.PrismException;
import strat.MDStrategyArray;
import strat.StrategyExportOptions.InducedModelMode;
import strat.StrategyInfo.UndefinedReason;
import strat.UndefinedChoicePolicy;
import strat.UndefinedChoicePolicy.Resolution;

/**
 * Tests for {@link ConstructInducedModel} where several choices in a state
 * have the same action label. Strategy decisions are action-based, so a decision
 * picking such an action is ambiguous, and should be reported as an error
 * (unless the strategy is undefined, in which case any one choice can be used).
 */
public class ConstructInducedModelTest
{
	/**
	 * MDP with states 0..3; state 0 has two unlabelled choices
	 * (to 1, and to 2) and one choice labelled "b" (to 3);
	 * states 1..3 have a single (unlabelled) self-loop.
	 */
	private MDPSimple<Double> buildMDP()
	{
		MDPSimple<Double> mdp = new MDPSimple<>(4);
		mdp.addInitialState(0);
		mdp.addActionLabelledChoice(0, dirac(1), null);
		mdp.addActionLabelledChoice(0, dirac(2), null);
		mdp.addActionLabelledChoice(0, dirac(3), "b");
		for (int s = 1; s < 4; s++) {
			mdp.addActionLabelledChoice(s, dirac(s), null);
		}
		return mdp;
	}

	private Distribution<Double> dirac(int s)
	{
		Distribution<Double> distr = new Distribution<>(prism.Evaluator.forDouble());
		distr.add(s, 1.0);
		return distr;
	}

	private DTMCSimple<Double> induce(MDPSimple<Double> mdp, int[] choices) throws PrismException
	{
		ConstructInducedModel cim = new ConstructInducedModel().setMode(InducedModelMode.REDUCE).setReachOnly(false);
		return (DTMCSimple<Double>) cim.constructInducedModel(mdp, new MDStrategyArray<>(mdp, choices));
	}

	@Test
	public void uniqueActionIsInduced() throws PrismException
	{
		DTMCSimple<Double> dtmc = induce(buildMDP(), new int[] { 2, 0, 0, 0 });
		assertEquals(1, dtmc.getNumTransitions(0));
		assertEquals(1.0, dtmc.getTransitions(0).get(3), 1e-12);
	}

	@Test
	public void ambiguousActionIsAnError()
	{
		PrismException e = assertThrows(PrismException.class, () -> induce(buildMDP(), new int[] { 1, 0, 0, 0 }));
		assertTrue(e.getMessage().contains("ambiguous"), e.getMessage());
	}

	@Test
	public void undefinedChoiceUsesOneChoice() throws PrismException
	{
		// Undefined (arbitrary) choice in state 0: the first choice is used, but only one
		DTMCSimple<Double> dtmc = induce(buildMDP(), new int[] { -2, 0, 0, 0 });
		assertEquals(1, dtmc.getNumTransitions(0));
		assertEquals(1.0, dtmc.getTransitions(0).get(1), 1e-12);
	}
	private DTMCSimple<Double> induce(MDPSimple<Double> mdp, int[] choices, UndefinedChoicePolicy policy) throws PrismException
	{
		ConstructInducedModel cim = new ConstructInducedModel().setMode(InducedModelMode.REDUCE).setReachOnly(false).setUndefinedChoicePolicy(policy);
		return (DTMCSimple<Double>) cim.constructInducedModel(mdp, new MDStrategyArray<>(mdp, choices));
	}

	@Test
	public void undefinedChoicePolicies() throws PrismException
	{
		int[] choices = new int[] { UndefinedReason.SATISFIED.getCode(), 0, 0, 0 };
		// STOP: no transitions
		DTMCSimple<Double> dtmc = induce(buildMDP(), choices, new UndefinedChoicePolicy().set(UndefinedReason.SATISFIED, Resolution.STOP));
		assertEquals(0, dtmc.getNumTransitions(0));
		// LOOP: a self-loop (marked as a deadlock, as for fixed deadlocks)
		dtmc = induce(buildMDP(), choices, new UndefinedChoicePolicy().set(UndefinedReason.SATISFIED, Resolution.LOOP));
		assertEquals(1, dtmc.getNumTransitions(0));
		assertEquals(1.0, dtmc.getTransitions(0).get(0), 1e-12);
		assertTrue(dtmc.isDeadlockState(0));
		// ALL: uniform over all 3 choices
		dtmc = induce(buildMDP(), choices, new UndefinedChoicePolicy().set(UndefinedReason.SATISFIED, Resolution.ALL));
		assertEquals(3, dtmc.getNumTransitions(0));
		assertEquals(1.0 / 3, dtmc.getTransitions(0).get(3), 1e-12);
		// ERROR
		assertThrows(PrismException.class, () -> induce(buildMDP(), choices, new UndefinedChoicePolicy().set(UndefinedReason.SATISFIED, Resolution.ERROR)));
		// Default: UNKNOWN gives an error
		assertThrows(PrismException.class, () -> induce(buildMDP(), new int[] { UndefinedReason.UNKNOWN.getCode(), 0, 0, 0 }, new UndefinedChoicePolicy()));
	}
}
