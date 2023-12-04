package simulator;


import parser.State;
import parser.ast.Expression;
import parser.ast.LabelList;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismUtils;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * This class will evaluate labels. Therefore, it's wrapped around a {@link LabelList}.
 * <br>
 * It also caches the results.
 */
public class LabelEvaluator
{
	/** Map of each evaluated state and a bitset containing the label-values ordered as in labelList */
	Map<State, BitSet> stateValues = new HashMap<>();
	/** The labelList that will be evaluated */
	LabelList labelList;
	int[] evaluationOrder;

	/**
	 * Creates new object that will evaluate and cache the given {@link LabelList}.
	 */
	public LabelEvaluator(LabelList labelList) throws PrismLangException
	{
		this.labelList = labelList;
		this.evaluationOrder = fixEvaluationOrder(labelList.getLabelDependencies());
	}

	/**
	 * Fix an order on the labels such that l1 < l2 if l2 depends on l1.
	 *
	 * @param deps an adjacency matrix with adj[i][j] iff i->j
	 * @return an int array of the label indices in ascending order
	 */
	public int[] fixEvaluationOrder(boolean[][] deps)
	{
		assert deps.length == 0 || deps.length == deps[0].length : "dependency matrix must be square";
		assert PrismUtils.findCycle(deps) == -1 : "label dependencies must not contain a cycle";

		int numLabels = deps.length;
		BitSet done = new BitSet(numLabels);
		int[] order = new int[numLabels];
		for (int l = done.nextClearBit(0), last = -1; l < numLabels; l = done.nextClearBit(l)) {
			last = traverseDfs(deps, l, order, last, done);
		}
		return order;
	}

	/**
	 * Do a depth-first search in a graph given by an adjacency matrix starting at a given node.
	 *
	 * @param adj   an adjacency matrix with adj[i][j] iff i->j
	 * @param start a start node
	 * @param trace an int array to record the trace, will be modified
	 * @param last  index of last element of the trace
	 * @param done  a {@link BitSet} of already visited nodes, will be modified
	 * @return the index of the last element of the extended trace, i.e., of the start node
	 */
	private int traverseDfs(boolean[][] adj, int start, int[] trace, int last, BitSet done)
	{
		if (!done.get(start)) {
			boolean[] isSuccessor = adj[start];
			for (int node = isSuccessor.length - 1; node >= 0; node--) {
				if (isSuccessor[node]) {
					last = traverseDfs(adj, node, trace, last, done);
				}
			}
			trace[++last] = start;
			done.set(start);
		}
		return last;
	}

	/**
	 * Returns the value of a specific label in a specific state.
	 *
	 * @param state The state to evaluate in.
	 * @param label The label that will be evaluated.
	 * @return The value of the Label.
	 * @throws PrismLangException In case a label couldn't be evaluated.
	 */
	public boolean getLabelValue(State state, String label) throws PrismLangException
	{
		return getStateValues(state).get(labelList.getLabelIndex(label));
	}

	/**
	 * This method provides all label values concerning a given state. <br>
	 * It will evaluate all the Labels if necessary.
	 *
	 * @param state The state to evaluate in.
	 * @return Returns a {@link Map} containing all label-values for the given state, indexed by their name.
	 * @throws PrismLangException In case a label couldn't be evaluated.
	 */
	public Predicate<String> getLabelValues(State state) throws PrismLangException
	{
		return asPredicate(getStateValues(state));
	}

	/**
	 * This method provides all values of a certain label. They will be ordered by the given statesList. <br>
	 * States will be evaluated if necessary.
	 *
	 * @param label      The name of the label.
	 * @param statesList The states to evaluate against in correct order.
	 * @return All values of the label.
	 * @throws PrismException In case a label evaluation fails.
	 */
	public BitSet getLabel(String label, List<State> statesList) throws PrismException
	{
		int statesNum = statesList.size();
		int labelIndex = labelList.getLabelIndex(label);
		BitSet labelValues = new BitSet();
		for (int i = statesNum - 1; i >= 0; i--) {
			State state = statesList.get(i);
			BitSet valuesOfState = getStateValues(state);
			labelValues.set(i, valuesOfState.get(labelIndex));
		}
		return labelValues;
	}

	/**
	 * This method provides all label values of a certain state as a {@link BitSet}.
	 * The labels are indexed in the same order as inside the {@link LabelEvaluator#labelList}.
	 *
	 * @param state The state to evaluate in.
	 * @return Always returns a BitSet with the label values.
	 * @throws PrismLangException In case a label evaluation fails.
	 */
	protected BitSet getStateValues(State state) throws PrismLangException
	{
		BitSet values = stateValues.get(state);
		if (values == null) {
			values = evaluateState(state);
			stateValues.put(state, values);
		}
		return values;
	}

	/**
	 * Evaluates all label with the given state.
	 *
	 * @param state The state to evaluate the labels in.
	 * @return A {@link BitSet} of all label values in this state.
	 * @throws PrismLangException In case a label evaluation fails.
	 */
	private BitSet evaluateState(State state) throws PrismLangException
	{
		BitSet values = new BitSet();
		Predicate<String> labelValues = asPredicate(values);
		for (int i : evaluationOrder) {
			Expression label = labelList.getLabel(i);
			boolean labelValue = label.evaluateBoolean(null, labelValues, state);
			values.set(i, labelValue);
		}
		stateValues.put(state, values);
		return values;
	}

	/**
	 * Convert the label values for a state to a predicate on the label names.
	 *
	 * @param values the label values for some state
	 * @return a Predicate evaluating to {@code true} for a label name iff the Bit at the corresponding index in {@code values} is set
	 */
	public Predicate<String> asPredicate(BitSet values)
	{
		return new Predicate<String>()
		{
			@Override
			public boolean test(String name)
			{
				int index = labelList.getLabelIndex(name);
				if (index == -1) {
					throw new NoSuchElementException("Unknown label \"" + name + "\"");
				}
				return values.get(index);
			}
		};
	}
}
