package explicit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import prism.PrismException;
import strat.MDStrategyArray;
import strat.StrategyExportOptions.InducedModelMode;

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
}
