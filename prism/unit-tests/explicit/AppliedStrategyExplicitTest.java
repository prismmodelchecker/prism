package explicit;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import explicit.rewards.RewardsSimple;
import parser.VarList;
import parser.ast.DeclarationInt;
import parser.ast.Expression;
import parser.type.TypeDouble;
import prism.BasicModelInfo;
import prism.ModelInfo;
import prism.ModelType;
import prism.PrismException;
import prism.RewardInfo;
import strat.FMDStrategyStep;
import strat.MDStrategyArray;

/**
 * Tests for {@link AppliedStrategyExplicit}: the model induced by a strategy,
 * its model/reward info, and the map between induced and original states.
 */
public class AppliedStrategyExplicitTest
{
	/**
	 * MDP with one variable s in 0..3: state 0 has choices "a" (to 1 or 2, with
	 * probability 0.5 each) and "b" (to 3); states 1..3 have a self-loop.
	 * Label "goal" is state 3; reward structure "r" has state reward s in state s.
	 */
	private MDPSimple<Double> buildMDP() throws PrismException
	{
		MDPSimple<Double> mdp = new MDPSimple<>(4);
		mdp.addInitialState(0);
		Distribution<Double> distr = new Distribution<>(prism.Evaluator.forDouble());
		distr.add(1, 0.5);
		distr.add(2, 0.5);
		mdp.addActionLabelledChoice(0, distr, "a");
		mdp.addActionLabelledChoice(0, dirac(3), "b");
		for (int s = 1; s < 4; s++) {
			mdp.addActionLabelledChoice(s, dirac(s), null);
		}
		VarList varList = new VarList();
		varList.addVar("s", new DeclarationInt(Expression.Int(0), Expression.Int(3)), -1);
		mdp.setVarList(varList);
		BitSet goal = new BitSet();
		goal.set(3);
		mdp.addLabel("goal", goal);
		RewardsSimple<Double> rews = new RewardsSimple<>(4);
		for (int s = 0; s < 4; s++) {
			rews.setStateReward(s, (double) s);
		}
		mdp.addRewards("r", 0, rews);
		return mdp;
	}

	private Distribution<Double> dirac(int s)
	{
		Distribution<Double> distr = new Distribution<>(prism.Evaluator.forDouble());
		distr.add(s, 1.0);
		return distr;
	}

	private ModelInfo modelInfo(MDPSimple<Double> mdp)
	{
		BasicModelInfo info = new BasicModelInfo(ModelType.MDP);
		info.setVarList(mdp.getVarList());
		info.setLabelNameList(Arrays.asList("goal"));
		return info;
	}

	private RewardInfo rewardInfo()
	{
		return new RewardInfo()
		{
			@Override
			public List<String> getRewardStructNames()
			{
				return Arrays.asList("r");
			}
		};
	}

	@Test
	public void memorylessStrategy() throws PrismException
	{
		// Choice "a" in state 0: (reachable) induced states correspond to 0, 1, 2
		MDPSimple<Double> mdp = buildMDP();
		AppliedStrategyExplicit<Double> applied = new AppliedStrategyExplicit<>(new MDStrategyArray<>(mdp, new int[] { 0, 0, 0, 0 }), modelInfo(mdp), rewardInfo());
		Model<Double> induced = applied.getInducedModel();
		assertSame(mdp, applied.getOriginalModel());
		assertEquals(ModelType.DTMC, induced.getModelType());
		assertEquals(3, induced.getNumStates());
		assertFalse(applied.hasMemory());
		for (int s = 0; s < 3; s++) {
			assertEquals(s, applied.getOriginalState(s));
			assertEquals(-1, applied.getMemory(s));
		}
		// Model info: new type, same variables and labels
		ModelInfo info = applied.getInducedModelInfo();
		assertEquals(ModelType.DTMC, info.getModelType());
		assertEquals(Arrays.asList("s"), info.getVarNames());
		assertEquals(Arrays.asList("goal"), info.getLabelNames());
		// Rewards are those copied to the induced model
		assertEquals(Arrays.asList("r"), applied.getInducedRewardGenerator().getRewardStructNames());
		assertEquals(2.0, applied.getInducedRewardGenerator().getStateReward(0, 2, false), 1e-12);
	}

	@Test
	public void pullBackAndProject() throws PrismException
	{
		// Choice "b" in state 0: induced states correspond to 0, 3
		MDPSimple<Double> mdp = buildMDP();
		AppliedStrategyExplicit<Double> applied = new AppliedStrategyExplicit<>(new MDStrategyArray<>(mdp, new int[] { 1, 0, 0, 0 }), modelInfo(mdp), rewardInfo());
		Model<Double> induced = applied.getInducedModel();
		assertEquals(2, induced.getNumStates());
		assertEquals(3, applied.getOriginalState(1));
		// Labels are copied
		assertEquals(1, induced.getLabelStates("goal").cardinality());
		assertTrue(induced.getLabelStates("goal").get(1));
		// Pull back a set of states and a vector of values
		BitSet orig = new BitSet();
		orig.set(2);
		orig.set(3);
		BitSet pulled = applied.pullBack(orig);
		assertFalse(pulled.get(0));
		assertTrue(pulled.get(1));
		StateValues svOrig = StateValues.createFromDoubleArray(new double[] { 0.5, 1.0, 2.0, 3.0 }, mdp);
		StateValues svPulled = applied.pullBack(svOrig);
		assertEquals(0.5, (Double) svPulled.getValue(0), 1e-12);
		assertEquals(3.0, (Double) svPulled.getValue(1), 1e-12);
		// Project back: only the initial state gets a value
		StateValues svInduced = StateValues.createFromDoubleArray(new double[] { 0.7, 0.9 }, induced);
		StateValues svProj = applied.projectToOriginalModel(svInduced);
		assertEquals(4, svProj.getSize());
		assertEquals(TypeDouble.getInstance(), svProj.getType());
		assertEquals(0.7, (Double) svProj.getValue(0), 1e-12);
		assertEquals(0.0, (Double) svProj.getValue(3), 1e-12);
	}

	@Test
	public void strategyWithMemory() throws PrismException
	{
		// Step-bounded strategy (k=1): "a" in the first step, then the self-loops
		MDPSimple<Double> mdp = buildMDP();
		FMDStrategyStep<Double> strat = new FMDStrategyStep<>(mdp, 1);
		strat.setStepChoices(0, new int[] { 0, 0, 0, 0 });
		strat.setTail(new MDStrategyArray<>(mdp, new int[] { 1, 0, 0, 0 }));
		AppliedStrategyExplicit<Double> applied = new AppliedStrategyExplicit<>(strat, modelInfo(mdp), rewardInfo());
		Model<Double> induced = applied.getInducedModel();
		assertEquals(ModelType.DTMC, induced.getModelType());
		assertTrue(applied.hasMemory());
		assertEquals(3, induced.getNumStates());
		for (int s = 0; s < 3; s++) {
			assertEquals(s, applied.getOriginalState(s));
			assertEquals(s == 0 ? 0 : 1, applied.getMemory(s));
		}
		// The memory variable is added to the model info
		ModelInfo info = applied.getInducedModelInfo();
		assertEquals(Arrays.asList("_mem", "s"), info.getVarNames());
		assertEquals(Arrays.asList("goal"), info.getLabelNames());
	}
}
