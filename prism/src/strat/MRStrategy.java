package strat;

import explicit.ConstructInducedModel;
import explicit.Distribution;
import explicit.DistributionOver;
import explicit.Model;
import explicit.NondetModel;
import parser.State;
import prism.PrismException;
import prism.PrismLog;
import prism.PrismNotSupportedException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class to store a memoryless randomised (MR) strategy
 * associated with an explicit engine model.
 */
public class MRStrategy<Value> extends StrategyExplicit<Value>
{
	// Probability of selecting choice indices in each state
	protected List<Distribution<Value>> choiceProbs;

	/**
	 * Create a blank MRStrategy for a specified model.
	 */
	public MRStrategy(NondetModel<Value> model)
	{
		super(model);
		int numStates = model.getNumStates();
		choiceProbs = new ArrayList<>(numStates);
		for (int i = 0; i < numStates; i++) {
			choiceProbs.add(new Distribution<>(model.getEvaluator()));
		}
	}

	/**
	 * Set the probability of selecting choice index i in state s to p
	 */
	public void setChoiceProbability(int s, int i, Value p)
	{
		choiceProbs.get(s).set(i, p);
	}

	@Override
	public boolean isRandomised()
	{
		return true;
	}

	@Override
	public Object getChoiceAction(int s, int m)
	{
		Distribution<Value> probs = choiceProbs.get(s);
		return probs.isEmpty() ? Strategy.UNDEFINED : DistributionOver.create(probs, i -> model.getAction(s, i));
	}

	@Override
	public int getChoiceIndex(int s, int m)
	{
		// N/A for randomised strategy
		return -1;
	}

	@Override
	public explicit.Model<Value> constructInducedModel(StrategyExportOptions options) throws PrismException
	{
		ConstructInducedModel cim = new ConstructInducedModel();
		cim.setMode(options.getMode()).setReachOnly(options.getReachOnly());
		Model<Value> inducedModel = cim.constructInducedModel(model, this);
		return inducedModel;
	}

	@Override
	public void exportActions(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		List<State> states = model.getStatesList();
		boolean showStates = options.getShowStates() && states != null;
		int n = getNumStates();
		for (int s = 0; s < n; s++) {
			Object probs = getChoiceAction(s, -1);
			if (probs != UNDEFINED) {
				out.println((showStates ? states.get(s) : s) + "=" + probs);
			}
		}
	}

	@Override
	public void exportIndices(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		List<State> states = model.getStatesList();
		boolean showStates = options.getShowStates() && states != null;
		int n = getNumStates();
		for (int s = 0; s < n; s++) {
			Distribution<Value> probs = choiceProbs.get(s);
			if (!probs.isEmpty()) {
				out.println((showStates ? states.get(s) : s) + "=" + probs);
			}
		}
	}

	@Override
	public void exportInducedModel(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		Model<Value> inducedModel = constructInducedModel(options);
		inducedModel.exportToPrismExplicitTra(out, options.getModelPrecision());
	}

	@Override
	public void exportDotFile(PrismLog out, StrategyExportOptions options) throws PrismException
	{
		Model<Value> inducedModel = constructInducedModel(options);
		inducedModel.exportToDotFile(out, null, options.getShowStates(), options.getModelPrecision());
	}

	@Override
	public void clear()
	{
		choiceProbs = null;
	}

	@Override
	public String toString()
	{
		return "[" + IntStream.range(0, getNumStates())
				.mapToObj(s -> s + "=" + getChoiceActionString(s, -1))
				.collect(Collectors.joining(",")) + "]";
	}
}
