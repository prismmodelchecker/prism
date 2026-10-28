package explicit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import prism.PrismException;
import strat.MDStrategyArray;
import strat.MRStrategy;
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

	/**
	 * In "restrict" mode, the choices picked by a randomised decision are merged
	 * into one choice, labelled with the decision's support in choice order.
	 */
	@Test
	public void randomisedDecisionLabels() throws PrismException
	{
		// State 0 has choices "a", "b", "c" (to 1, 2, 3); states 1..3 have self-loops
		MDPSimple<Double> mdp = new MDPSimple<>(4);
		mdp.addInitialState(0);
		mdp.addActionLabelledChoice(0, dirac(1), "a");
		mdp.addActionLabelledChoice(0, dirac(2), "b");
		mdp.addActionLabelledChoice(0, dirac(3), "c");
		for (int s = 1; s < 4; s++) {
			mdp.addActionLabelledChoice(s, dirac(s), "d");
		}
		// Randomised: support in choice order ("a_c"), regardless of the order set
		MRStrategy<Double> strat = new MRStrategy<>(mdp);
		strat.setChoiceProbability(0, 2, 0.25);
		strat.setChoiceProbability(0, 0, 0.75);
		for (int s = 1; s < 4; s++) {
			strat.setChoiceProbability(s, 0, 1.0);
		}
		ConstructInducedModel cim = new ConstructInducedModel().setMode(InducedModelMode.RESTRICT).setReachOnly(false);
		MDPSimple<Double> induced = (MDPSimple<Double>) cim.constructInducedModel(mdp, strat);
		assertEquals(1, induced.getNumChoices(0));
		assertEquals("a_c", induced.getAction(0, 0));
		assertEquals(0.75, induced.getChoice(0, 0).get(1), 1e-12);
		assertEquals(0.25, induced.getChoice(0, 0).get(3), 1e-12);
		// Probability 1 on one action: the action is kept
		assertEquals("d", induced.getAction(1, 0));
	}

	/**
	 * For a randomised strategy, an undefined choice resolved with ALL is
	 * kept separate from the strategy's own randomisation: in "reduce" mode,
	 * all choices are picked uniformly; in "restrict" mode, they are kept
	 * as separate choices (each with its own action, unscaled), whereas
	 * the choices picked by a randomised decision are merged into one.
	 */
	@Test
	public void randomisedStrategyWithAll() throws PrismException
	{
		// State 0 has choices "a", "b" (to 1, 2); state 1 has choices "c", "e" (to 1, 3);
		// states 2, 3 have self-loops
		MDPSimple<Double> mdp = new MDPSimple<>(4);
		mdp.addInitialState(0);
		mdp.addActionLabelledChoice(0, dirac(1), "a");
		mdp.addActionLabelledChoice(0, dirac(2), "b");
		mdp.addActionLabelledChoice(1, dirac(1), "c");
		mdp.addActionLabelledChoice(1, dirac(3), "e");
		mdp.addActionLabelledChoice(2, dirac(2), "f");
		mdp.addActionLabelledChoice(3, dirac(3), "f");
		// Randomised in state 0; undefined (satisfied) in state 1
		MRStrategy<Double> strat = new MRStrategy<>(mdp);
		strat.setChoiceProbability(0, 0, 0.25);
		strat.setChoiceProbability(0, 1, 0.75);
		strat.setUndefinedReason(1, UndefinedReason.SATISFIED);
		strat.setChoiceProbability(2, 0, 1.0);
		strat.setChoiceProbability(3, 0, 1.0);
		UndefinedChoicePolicy policy = new UndefinedChoicePolicy().set(UndefinedReason.SATISFIED, Resolution.ALL);
		// Reduce: strategy's probabilities in state 0, uniform in state 1
		ConstructInducedModel cim = new ConstructInducedModel().setMode(InducedModelMode.REDUCE).setReachOnly(false).setUndefinedChoicePolicy(policy);
		DTMCSimple<Double> dtmc = (DTMCSimple<Double>) cim.constructInducedModel(mdp, strat);
		assertEquals(0.25, dtmc.getTransitions(0).get(1), 1e-12);
		assertEquals(0.75, dtmc.getTransitions(0).get(2), 1e-12);
		assertEquals(0.5, dtmc.getTransitions(1).get(1), 1e-12);
		assertEquals(0.5, dtmc.getTransitions(1).get(3), 1e-12);
		// Restrict: one merged choice in state 0, two separate choices in state 1
		cim = new ConstructInducedModel().setMode(InducedModelMode.RESTRICT).setReachOnly(false).setUndefinedChoicePolicy(policy);
		MDPSimple<Double> induced = (MDPSimple<Double>) cim.constructInducedModel(mdp, strat);
		assertEquals(1, induced.getNumChoices(0));
		assertEquals("a_b", induced.getAction(0, 0));
		assertEquals(0.25, induced.getChoice(0, 0).get(1), 1e-12);
		assertEquals(0.75, induced.getChoice(0, 0).get(2), 1e-12);
		assertEquals(2, induced.getNumChoices(1));
		assertEquals("c", induced.getAction(1, 0));
		assertEquals(1.0, induced.getChoice(1, 0).get(1), 1e-12);
		assertEquals("e", induced.getAction(1, 1));
		assertEquals(1.0, induced.getChoice(1, 1).get(3), 1e-12);
	}

	/**
	 * In "restrict" mode, all choices picked for an undefined choice (ALL) are kept as
	 * separate choices, even if some are identical, with their transition rewards.
	 */
	@Test
	public void allKeepsIdenticalChoices() throws PrismException
	{
		// State 0 has choices "a", "a" (identical, to 1) and "b" (to 2), with transition rewards 2, 7, 11
		MDPSimple<Double> mdp = new MDPSimple<>(3);
		mdp.setAllowDupes(true);
		mdp.addInitialState(0);
		mdp.addActionLabelledChoice(0, dirac(1), "a");
		mdp.addActionLabelledChoice(0, dirac(1), "a");
		mdp.addActionLabelledChoice(0, dirac(2), "b");
		mdp.addActionLabelledChoice(1, dirac(1), "c");
		mdp.addActionLabelledChoice(2, dirac(2), "c");
		explicit.rewards.RewardsSimple<Double> rews = new explicit.rewards.RewardsSimple<>(3);
		rews.setEvaluator(prism.Evaluator.forDouble());
		rews.setTransitionReward(0, 0, 2.0);
		rews.setTransitionReward(0, 1, 7.0);
		rews.setTransitionReward(0, 2, 11.0);
		mdp.addRewards("r", rews);
		// Undefined (satisfied) choice in state 0
		int[] choices = new int[] { UndefinedReason.SATISFIED.getCode(), 0, 0 };
		UndefinedChoicePolicy policy = new UndefinedChoicePolicy().set(UndefinedReason.SATISFIED, Resolution.ALL);
		ConstructInducedModel cim = new ConstructInducedModel().setMode(InducedModelMode.RESTRICT).setReachOnly(false).setUndefinedChoicePolicy(policy);
		MDPSimple<Double> induced = (MDPSimple<Double>) cim.constructInducedModel(mdp, new MDStrategyArray<>(mdp, choices));
		assertEquals(3, induced.getNumChoices(0));
		assertEquals("a", induced.getAction(0, 0));
		assertEquals("a", induced.getAction(0, 1));
		assertEquals("b", induced.getAction(0, 2));
		assertEquals(2.0, induced.getRewards(0).getTransitionReward(0, 0), 1e-12);
		assertEquals(7.0, induced.getRewards(0).getTransitionReward(0, 1), 1e-12);
		assertEquals(11.0, induced.getRewards(0).getTransitionReward(0, 2), 1e-12);
	}
}
