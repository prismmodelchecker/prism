package strat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import explicit.Distribution;
import explicit.MDPSimple;
import explicit.Model;
import parser.State;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismException;
import prism.PrismNotSupportedException;
import prism.PrismPrintStreamLog;
import strat.StrategyInfo.UndefinedReason;

/**
 * Tests for strategies that switch between memoryless strategies according to their memory
 * ({@link FMStrategySwitching}), here one that randomises (in memory 0) until it takes
 * a "commit" choice, and then stays (in memory 1, "stay") forever.
 */
public class FMStrategySwitchingTest
{
	/**
	 * MDP: in state 0, "go" (to 1) or "commit" (a self-loop); in state 1, "back" (to 0) or "end" (to 2);
	 * state 2 has a self-loop "loop"; state 3 (unreachable) has a self-loop "loop".
	 */
	private MDPSimple<Double> buildMDP()
	{
		MDPSimple<Double> mdp = new MDPSimple<>(4);
		mdp.addInitialState(0);
		mdp.addActionLabelledChoice(0, dirac(1), "go");
		mdp.addActionLabelledChoice(0, dirac(0), "commit");
		mdp.addActionLabelledChoice(1, dirac(0), "back");
		mdp.addActionLabelledChoice(1, dirac(2), "end");
		mdp.addActionLabelledChoice(2, dirac(2), "loop");
		mdp.addActionLabelledChoice(3, dirac(3), "loop");
		return mdp;
	}

	private static Distribution<Double> dirac(int s)
	{
		Distribution<Double> distr = new Distribution<>(Evaluator.forDouble());
		distr.add(s, 1.0);
		return distr;
	}

	/** Memory 0 until "commit" is taken, then 1 ("stay") forever */
	private static class CommitMemory implements StrategyMemory
	{
		@Override
		public int size()
		{
			return 2;
		}

		@Override
		public int initial(int sInit)
		{
			return 0;
		}

		@Override
		public int update(int m, Object action, int sNext)
		{
			return m == 1 || "commit".equals(action) ? 1 : 0;
		}

		@Override
		public String toString(int m)
		{
			return m == 1 ? "stay" : "0";
		}
	}

	/**
	 * In memory 0 (randomised, so decisions are distributions): "go" or "commit" (1/2 each)
	 * in state 0, "end" in 1, "loop" in 2 (and 3);
	 * in memory 1: "commit" in state 0, unreachable elsewhere.
	 */
	private FMStrategySwitching<Double> buildStrategy(MDPSimple<Double> mdp, StrategyMemory memory)
	{
		MRStrategy<Double> before = new MRStrategy<>(mdp);
		before.setChoiceProbability(0, 0, 0.5);
		before.setChoiceProbability(0, 1, 0.5);
		before.setChoiceProbability(1, 1, 1.0);
		before.setChoiceProbability(2, 0, 1.0);
		before.setChoiceProbability(3, 0, 1.0);
		int unreachable = UndefinedReason.UNREACHABLE.getCode();
		MDStrategyArray<Double> stay = new MDStrategyArray<>(mdp, new int[] { 1, unreachable, unreachable, unreachable });
		return new FMStrategySwitching<>(mdp, memory, Arrays.asList(before, stay));
	}

	private static String export(Strategy<Double> strat, boolean indices, boolean showUndefined) throws PrismException
	{
		return export(strat, indices, showUndefined, false);
	}

	private static String export(Strategy<Double> strat, boolean indices, boolean showUndefined, boolean showStates) throws PrismException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		PrismPrintStreamLog log = new PrismPrintStreamLog(new PrintStream(bytes));
		StrategyExportOptions options = new StrategyExportOptions();
		options.setShowStates(showStates);
		options.setShowUndefined(showUndefined);
		if (indices) {
			strat.exportIndices(log, options);
		} else {
			strat.exportActions(log, options);
		}
		log.flush();
		return bytes.toString().replace(System.lineSeparator(), "\n");
	}

	private static String lines(String... lines)
	{
		return String.join("\n", lines) + "\n";
	}

	/** Memory as for {@link CommitMemory}, but giving value {@code m} on entering state {@code s} */
	private static StrategyMemory memoryEntering(int s, int m)
	{
		return new CommitMemory()
		{
			@Override
			public int update(int mOld, Object action, int sNext)
			{
				return sNext == s ? m : super.update(mOld, action, sNext);
			}
		};
	}

	@Test
	public void testMemory()
	{
		FMStrategySwitching<Double> strat = buildStrategy(buildMDP(), new CommitMemory());
		assertEquals(StrategyInfo.Memory.FINITE, strat.memory());
		assertEquals(2, strat.getMemorySize());
		assertEquals(0, strat.getInitialMemory(0));
		assertEquals(0, strat.getUpdatedMemory(0, "go", 1));
		assertEquals(1, strat.getUpdatedMemory(0, "commit", 0));
		assertEquals(1, strat.getUpdatedMemory(1, "go", 1));
		assertEquals("stay", strat.getMemoryString(1));
		// Unknown memory stays unknown
		assertEquals(-1, strat.getUpdatedMemory(-1, "commit", 0));
		assertEquals("?", strat.getMemoryString(-1));
	}

	@Test
	public void testDecisions()
	{
		FMStrategySwitching<Double> strat = buildStrategy(buildMDP(), new CommitMemory());
		assertEquals(true, strat.isRandomised());
		assertEquals(0.5, strat.getChoiceActionProbability(0, 0, "commit"), 1e-12);
		assertEquals("1.0:end", strat.getChoiceAction(1, 0).toString());
		assertEquals("commit", strat.getChoiceAction(0, 1));
		assertEquals(1, strat.getChoiceIndex(0, 1));
		// Undefined: from the strategy for the memory value, or unknown if the memory isn't known
		assertSame(StrategyInfo.UNDEFINED, strat.getChoiceAction(1, 1));
		assertEquals(UndefinedReason.UNREACHABLE, strat.whyUndefined(1, 1));
		assertNull(strat.whyUndefined(0, 1));
		assertSame(StrategyInfo.UNDEFINED, strat.getChoiceAction(0, -1));
		assertEquals(UndefinedReason.UNKNOWN, strat.whyUndefined(0, -1));
	}

	@Test
	public void testUndefined()
	{
		// Where the strategy leaves a choice undefined, the memory's reason comes first
		StrategyMemory memory = new CommitMemory()
		{
			@Override
			public UndefinedReason whyUndefined(int m)
			{
				return m == 1 ? UndefinedReason.SATISFIED : null;
			}
		};
		MDPSimple<Double> mdp = buildMDP();
		FMStrategySwitching<Double> strat = buildStrategy(mdp, memory);
		assertEquals(UndefinedReason.SATISFIED, strat.whyUndefined(1, 1));
		assertNull(strat.whyUndefined(0, 1));
		assertEquals("commit", strat.getChoiceAction(0, 1));
		// With no strategy: the memory's reason, or else unknown
		List<StrategyExplicit<Double>> noStay = Arrays.asList(strat.getStrategy(0), null);
		FMStrategySwitching<Double> none = new FMStrategySwitching<>(mdp, new CommitMemory(), noStay);
		assertSame(StrategyInfo.UNDEFINED, none.getChoiceAction(0, 1));
		assertEquals(UndefinedReason.UNKNOWN, none.whyUndefined(0, 1));
		assertEquals(UndefinedReason.UNKNOWN.getCode(), none.getChoiceIndex(0, 1));
		FMStrategySwitching<Double> noneSat = new FMStrategySwitching<>(mdp, memory, noStay);
		assertEquals(UndefinedReason.SATISFIED, noneSat.whyUndefined(0, 1));
		assertEquals(UndefinedReason.SATISFIED.getCode(), noneSat.getChoiceIndex(0, 1));
	}

	@Test
	public void testConstructor()
	{
		MDPSimple<Double> mdp = buildMDP();
		FMStrategySwitching<Double> strat = buildStrategy(mdp, new CommitMemory());
		// One memoryless strategy (or null) per memory value, for the same model, and some memory
		List<StrategyExplicit<Double>> one = Arrays.asList(strat.getStrategy(0));
		assertThrows(IllegalArgumentException.class, () -> new FMStrategySwitching<>(mdp, new CommitMemory(), one));
		assertThrows(IllegalArgumentException.class, () -> new FMStrategySwitching<>(mdp, new CommitMemory(), Arrays.asList(strat, strat)));
		MDPSimple<Double> other = buildMDP();
		assertThrows(IllegalArgumentException.class, () -> new FMStrategySwitching<>(other, new CommitMemory(), Arrays.asList(strat.getStrategy(0), null)));
		StrategyMemory empty = new CommitMemory()
		{
			@Override
			public int size()
			{
				return 0;
			}
		};
		assertThrows(IllegalArgumentException.class, () -> new FMStrategySwitching<>(mdp, empty, new ArrayList<StrategyExplicit<Double>>()));
	}

	@Test
	public void testExport() throws PrismException
	{
		FMStrategySwitching<Double> strat = buildStrategy(buildMDP(), new CommitMemory());
		// Only pairs of states and memory values that can occur together (not state 3)
		assertEquals(lines("0,0=0.5:go+0.5:commit", "0,stay=commit", "1,0=1.0:end", "2,0=1.0:loop"),
				export(strat, false, false));
		assertEquals(lines("0,0=0.5:go+0.5:commit", "0,stay=commit", "1,0=1.0:end", "1,stay=?unreachable", "2,0=1.0:loop", "2,stay=?unreachable"),
				export(strat, false, true));
		assertEquals(lines("0,0={0=0.5, 1=0.5}", "0,stay=1", "1,0={1=1.0}", "2,0={0=1.0}"),
				export(strat, true, false));
		assertEquals(lines("0,0={0=0.5, 1=0.5}", "0,stay=1", "1,0={1=1.0}", "1,stay=?unreachable", "2,0={0=1.0}", "2,stay=?unreachable"),
				export(strat, true, true));
	}

	@Test
	public void testExportStatesAndInitial() throws PrismException
	{
		// With states shown, and a second initial state (3)
		MDPSimple<Double> mdp = buildMDP();
		List<State> states = new ArrayList<>();
		for (int s = 0; s < 4; s++) {
			states.add(new State(1).setValue(0, s));
		}
		mdp.setStatesList(states);
		mdp.addInitialState(3);
		FMStrategySwitching<Double> strat = buildStrategy(mdp, new CommitMemory());
		assertEquals(lines("(0),0=0.5:go+0.5:commit", "(0),stay=commit", "(1),0=1.0:end", "(2),0=1.0:loop", "(3),0=1.0:loop"),
				export(strat, false, false, true));
	}

	@Test
	public void testExportDeterministic() throws PrismException
	{
		// "go" then "end" before committing (so "commit" is never taken)
		MDPSimple<Double> mdp = buildMDP();
		MDStrategyArray<Double> before = new MDStrategyArray<>(mdp, new int[] { 0, 1, 0, 0 });
		MDStrategyArray<Double> stay = new MDStrategyArray<>(mdp, new int[] { 1, 0, 0, 0 });
		FMStrategySwitching<Double> strat = new FMStrategySwitching<>(mdp, new CommitMemory(), Arrays.asList(before, stay));
		assertEquals(false, strat.isRandomised());
		// (all pairs that can occur, whatever the choices, so including those after "commit")
		assertEquals(lines("0,0=go", "0,stay=commit", "1,0=end", "1,stay=back", "2,0=loop", "2,stay=loop"),
				export(strat, false, false));
		assertEquals(lines("0,0=0", "0,stay=1", "1,0=1", "1,stay=0", "2,0=0", "2,stay=0"),
				export(strat, true, false));
		assertEquals(3, strat.constructInducedModel(new StrategyExportOptions()).getNumStates());
	}

	@Test
	public void testExportErrors() throws PrismException
	{
		// Unknown or out-of-range memory values are errors
		MDPSimple<Double> mdp = buildMDP();
		assertThrows(PrismException.class, () -> export(buildStrategy(mdp, memoryEntering(2, -1)), false, false));
		assertThrows(PrismException.class, () -> export(buildStrategy(mdp, memoryEntering(2, 2)), false, false));
		// A defined choice with no choice index cannot be exported as an index
		MDStrategyArray<Double> noIndices = new MDStrategyArray<Double>(mdp, new int[] { 1, 0, 0, 0 })
		{
			@Override
			public int getChoiceIndex(int s, int m)
			{
				return UndefinedReason.UNKNOWN.getCode();
			}
		};
		FMStrategySwitching<Double> strat = new FMStrategySwitching<>(mdp, new CommitMemory(), Arrays.asList(noIndices, noIndices));
		assertEquals(lines("0,0=commit", "0,stay=commit", "1,0=back", "1,stay=back", "2,0=loop", "2,stay=loop"), export(strat, false, false));
		assertThrows(PrismNotSupportedException.class, () -> export(strat, true, false));
	}

	@Test
	public void testInducedModel() throws PrismException
	{
		FMStrategySwitching<Double> strat = buildStrategy(buildMDP(), new CommitMemory());
		// Product of the reachable states and memory: (0,0), (1,0), (0,stay), (2,0)
		Model<Double> induced = strat.constructInducedModel(new StrategyExportOptions());
		assertEquals(ModelType.DTMC, induced.getModelType());
		assertEquals(4, induced.getNumStates());
		assertEquals(5, induced.getNumTransitions());
	}
}
