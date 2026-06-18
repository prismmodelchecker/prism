//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	
//------------------------------------------------------------------------------
//	
//	This file is part of PRISM.
//	
//	PRISM is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//	
//	PRISM is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//	
//	You should have received a copy of the GNU General Public License
//	along with PRISM; if not, write to the Free Software Foundation,
//	Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//	
//==============================================================================

package explicit;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

import explicit.rewards.Rewards;
import io.ExplicitModelImporter;
import io.PrismExplicitImporter;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.ActionList;
import prism.ActionListOwner;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismException;

/**
 * Base class for explicit-state model representations.
 */
public abstract class ModelExplicit<Value> implements Model<Value>, ActionListOwner
{
	/**
	 * Evaluator for manipulating values in the model (of type {@code Value})
	 */
	@SuppressWarnings("unchecked")
	protected Evaluator<Value> eval = (Evaluator<Value>) Evaluator.forDouble();

	// Basic model information

	/**
	 * Action list
	 */
	protected ActionList actionList = new ActionList(this::findActionsUsed);;
	/**
	 * Number of states
	 */
	protected int numStates;
	/**
	 * Which states are initial states
	 */
	protected List<Integer> initialStates; // TODO: should be a (linkedhash?) set really
	/**
	 * States that are/were deadlocks. Where requested and where appropriate (DTMCs/MDPs),
	 * these states may have been fixed at build time by adding self-loops.
	 */
	protected TreeSet<Integer> deadlocks;

	// Additional, optional information associated with the model

	/**
	 * (Optionally) information about the states of this model,
	 * i.e. the State object corresponding to each state index.
	 */
	protected List<State> statesList;
	/**
	 * (Optionally) a list of values for constants associated with this model.
	 */
	protected Values constantValues;
	/**
	 * (Optionally) the list of variables
	 */
	protected VarList varList;
	/**
	 * (Optionally) some labels (atomic propositions) associated with the model,
	 * represented as a String->BitSet mapping from their names to the states that satisfy them.
	 */
	protected Map<String, BitSet> labels = new TreeMap<String, BitSet>();

	/**
	 * (Optionally) some attached reward structures.
	 * Each can have a name (set to "" if it is unnamed).
	 * Each can have a position, referring to the order within its original source,
	 * e.g., a properties file (null if undefined).
	 * By convention, positions are assumed to be indexed from 0.
	 * Both names and positions must be unique if defined.
	 */
	protected List<IndexedRewards<Value>> rewards = new ArrayList<>();

	static protected class IndexedRewards<Value>
	{
		String name = "";
		Integer position = null;
		Rewards<Value> rews;
	}

	/**
	 * (Optionally) the stored predecessor relation. Becomes inaccurate after the model is changed!
	 */
	protected PredecessorRelation predecessorRelation = null;

	// Mutators

	/**
	 * Set the {@link Evaluator} object for manipulating values in the model
	 */
	public void setEvaluator(Evaluator<Value> eval)
	{
		this.eval = eval;
	}

	/**
	 * Copy data from another Model (used by superclass copy constructors).
	 * Assumes that this has already been initialise()ed.
	 */
	@SuppressWarnings("unchecked")
	public void copyFrom(Model<?> model)
	{
		setEvaluator((Evaluator<Value>) model.getEvaluator());
		if (model instanceof ActionListOwner) {
			actionList.copyFrom(((ActionListOwner) model).getActionList());
		}
		numStates = model.getNumStates();
		for (int in : model.getInitialStates()) {
			addInitialState(in);
		}
		for (int dl : model.getDeadlockStates()) {
			addDeadlockState(dl);
		}
		// Shallow copy of read-only stuff
		statesList = model.getStatesList();
		constantValues = model.getConstantValues();
		labels = model.getLabelToStatesMap();
		copyRewards((Model<Value>) model);
		varList = model.getVarList();
	}

	/**
	 * Copy data from another Model and a state index permutation,
	 * i.e. state index i becomes index permut[i]
	 * (used by superclass copy constructors).
	 * Assumes that this has already been initialise()ed.
	 * Pointer to states list is NOT copied (since now wrong).
	 */
	public void copyFrom(Model<Value> model, int permut[])
	{
		setEvaluator(model.getEvaluator());
		if (model instanceof ActionListOwner) {
			actionList.copyFrom(((ActionListOwner) model).getActionList());
		}
		numStates = model.getNumStates();
		for (int in : model.getInitialStates()) {
			addInitialState(permut[in]);
		}
		for (int dl : model.getDeadlockStates()) {
			addDeadlockState(permut[dl]);
		}
		// Shallow copy of (some) read-only stuff
		// (i.e. info that is not broken by permute)
		statesList = null;
		constantValues = model.getConstantValues();
		labels.clear();
		rewards.clear();
		varList = model.getVarList();
	}

	/**
	 * Initialise: create new model with fixed number of states.
	 */
	public void initialise(int numStates)
	{
		this.numStates = numStates;
		initialStates = new ArrayList<Integer>();
		deadlocks = new TreeSet<Integer>();
		statesList = null;
		constantValues = null;
		varList = null;
		labels = new TreeMap<String, BitSet>();
		rewards = new ArrayList<>();
	}

	/**
	 * Set the list of all action labels
	 */
	public void setActions(List<Object> actions)
	{
		actionList.setActions(actions);
	}

	/**
	 * Add a state to the list of initial states.
	 */
	public void addInitialState(int i)
	{
		initialStates.add(i);
	}

	/**
	 * Empty the list of initial states.
	 */
	public void clearInitialStates()
	{
		initialStates.clear();
	}

	/**
	 * Add a state to the list of deadlock states.
	 */
	public void addDeadlockState(int i)
	{
		deadlocks.add(i);
	}

	/**
	 * Build (anew) from a list of transitions provided by an explicit model importer.
	 * Note that initial states are not configured
	 * so this needs to be done separately (using {@link #addInitialState(int)}.
	 */
	public void buildFromExplicitImport(ExplicitModelImporter modelImporter) throws PrismException
	{
		// Not implemented by default
		throw new PrismException("Explicit model import not yet supported for this model");
	}

	/**
	 * Build (anew) from a list of transitions exported explicitly by PRISM (i.e. a .tra file).
	 * Note that initial states are not configured (since this info is not in the file),
	 * so this needs to be done separately (using {@link #addInitialState(int)}.
	 */
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		ExplicitModelImporter modelImporter = new PrismExplicitImporter(null, new File(filename), null, null, null, ModelType.DTMC);
		buildFromExplicitImport(modelImporter);
	}


	/**
	 * Set the associated (read-only) state list.
	 */
	public void setStatesList(List<State> statesList)
	{
		this.statesList = statesList;
	}

	/**
	 * Set the associated (read-only) constant values.
	 */
	public void setConstantValues(Values constantValues)
	{
		this.constantValues = constantValues;
	}

	/**
	 * Sets the VarList for this model (may be {@code null}).
	 */
	public void setVarList(VarList varList)
	{
		this.varList = varList;
	}

	/**
	 * Adds a label and the set the states that satisfy it.
	 * Any existing label with the same name is overwritten.
	 *
	 * @param name   The name of the label
	 * @param states The states that satisfy the label
	 */
	public void addLabel(String name, BitSet states)
	{
		labels.put(name, states);
	}

	/**
	 * Add a label with corresponding state set, ensuring a unique, non-existing label.
	 * The label will be either "X" or "X_i" where X is the content of the {@code prefix} argument
	 * and i is a non-negative integer.
	 * <br>
	 * Optionally, a set of defined label names can be passed so that those labels
	 * can be avoided. This can be obtained from the model checker via {@code getDefinedLabelNames()}.
	 * <br>
	 * Note that a stored label takes precedence over the on-the-fly calculation
	 * of an ExpressionLabel, cf. {@link explicit.StateModelChecker#checkExpressionLabel}
	 *
	 * @param prefix            the prefix for the unique label
	 * @param labelStates       the BitSet with the state set for the label
	 * @param definedLabelNames set of names (optional, may be {@code null}) to check for existing labels
	 * @return the generated unique label
	 */
	public String addUniqueLabel(String prefix, BitSet labelStates, Set<String> definedLabelNames)
	{
		String label;
		int i = 0;
		label = prefix;  // first, try without appending _i
		while (true) {
			boolean labelOk = !hasLabel(label);  // not directly attached to model
			if (definedLabelNames != null) {
				labelOk &= !definedLabelNames.contains(label);  // not defined
			}

			if (labelOk) {
				break;
			}

			// prepare next label to try
			label = prefix + "_" + i;
			if (i == Integer.MAX_VALUE)
				throw new UnsupportedOperationException("Integer overflow trying to add unique label");

			i++;
		}

		addLabel(label, labelStates);
		return label;
	}

	/**
	 * Attach a reward structure with optional name and position.
	 * If a reward structure already exists with the same (non-empty) name or the same
	 * (non-null) position, it is overwritten. If both name and position are provided,
	 * and they do not identify the same existing reward structure (or lack thereof),
	 * this is an error.
	 * @param name Name of reward structure ("" for unnamed; null also accepted)
	 * @param position Position of reward structure (0-indexed; null if not stored)
	 * @param rews The rewards
	 * @return The index of the attached reward
	 */
	public int addRewards(String name, Integer position, Rewards<Value> rews)
	{
		String rewName = name == null ? "" : name;
		int i = findRewardsIndex(rewName, position);
		if (i == -1) {
			i = rewards.size();
			rewards.add(new IndexedRewards<>());
		}
		IndexedRewards<Value> ir = rewards.get(i);
		ir.name = rewName;
		ir.position = position;
		ir.rews = rews;
		return i;
	}

	/**
	 * Find the index of a stored reward structure matching a (non-empty) name
	 * or a (non-null) position, if any. Returns -1 if there is no match.
	 * If both name and position are provided, but match different existing reward
	 * structures, this is an error (they should either both be absent or agree).
	 */
	private int findRewardsIndex(String name, Integer position)
	{
		int nameMatch = -1;
		int positionMatch = -1;
		for (int i = 0; i < rewards.size(); i++) {
			IndexedRewards<Value> ir = rewards.get(i);
			if (!name.isEmpty() && name.equals(ir.name)) {
				nameMatch = i;
			}
			if (position != null && position.equals(ir.position)) {
				positionMatch = i;
			}
		}
		if (nameMatch != -1 && positionMatch != -1 && nameMatch != positionMatch) {
			throw new RuntimeException("Reward structure name \"" + name + "\" and position " + position + " refer to different existing reward structures");
		}
		return nameMatch != -1 ? nameMatch : positionMatch;
	}

	/**
	 * Attach a reward structure with optional name.
	 * @param name Name of reward structure ("" for unnamed; null also accepted)
	 * @param rews The rewards
	 * @return The index of the attached reward
	 */
	public int addRewards(String name, Rewards<Value> rews)
	{
		return addRewards(name, null, rews);
	}

	/**
	 * Copy the rewards from an existing model.
	 */
	public void copyRewards(Model<Value> model)
	{
		copyRewardsMapped(model, rews -> rews);
	}

	/**
	 * Copy the rewards from an existing model after first applying a function to them.
	 */
	public void copyRewardsMapped(Model<Value> model, Function<Rewards<Value>, Rewards<Value>> map)
	{
		int numRewards = model.getNumRewards();
		for (int i = 0; i < numRewards; i++) {
			addRewards(model.getRewardName(i), model.getRewardPosition(i), map.apply(model.getRewards(i)));
		}
	}

	// Accessors (for Model interface)

	@Override
	public Evaluator<Value> getEvaluator()
	{
		return eval;
	}

	@Override
	public ActionList getActionList()
	{
		return actionList;
	}

	@Override
	public List<Object> getActions()
	{
		return actionList.getActions();
	}

	@Override
	public int actionIndex(Object action)
	{
		int actionIndex = actionList.actionIndex(action);
		if (actionIndex == -1) {
			throw new RuntimeException("Action storage error: " + action + " not found");
		}
		return actionIndex;
	}

	@Override
	public int getNumStates()
	{
		return numStates;
	}

	@Override
	public int getNumInitialStates()
	{
		return initialStates.size();
	}

	@Override
	public Iterable<Integer> getInitialStates()
	{
		return initialStates;
	}

	@Override
	public int getFirstInitialState()
	{
		return initialStates.isEmpty() ? -1 : initialStates.get(0);
	}

	@Override
	public boolean isInitialState(int i)
	{
		return initialStates.contains(i);
	}

	@Override
	public int getNumDeadlockStates()
	{
		return deadlocks.size();
	}

	@Override
	public Iterable<Integer> getDeadlockStates()
	{
		return deadlocks;
	}

	@Override
	public StateValues getDeadlockStatesList()
	{
		BitSet bs = new BitSet();
		for (int dl : deadlocks) {
			bs.set(dl);
		}

		return StateValues.createFromBitSet(bs, this);
	}

	@Override
	public int getFirstDeadlockState()
	{
		return deadlocks.isEmpty() ? -1 : deadlocks.first();
	}

	@Override
	public boolean isDeadlockState(int i)
	{
		return deadlocks.contains(i);
	}

	@Override
	public List<State> getStatesList()
	{
		return statesList;
	}

	@Override
	public Values getConstantValues()
	{
		return constantValues;
	}

	@Override
	public VarList getVarList()
	{
		return varList;
	}

	@Override
	public BitSet getLabelStates(String name)
	{
		return labels.get(name);
	}

	@Override
	public boolean hasLabel(String name)
	{
		return labels.containsKey(name);
	}

	@Override
	public Set<String> getLabels()
	{
		return labels.keySet();
	}

	@Override
	public Map<String, BitSet> getLabelToStatesMap()
	{
		return labels;
	}

	@Override
	public Rewards<Value> getRewardsByName(String name)
	{
		if (name == null || name.isEmpty()) {
			return null;
		}
		for (IndexedRewards<Value> ir : rewards) {
			if (name.equals(ir.name)) {
				return ir.rews;
			}
		}
		return null;
	}

	@Override
	public Rewards<Value> getRewardsByPosition(int r)
	{
		for (IndexedRewards<Value> ir : rewards) {
			if (ir.position != null && ir.position == r) {
				return ir.rews;
			}
		}
		return null;
	}

	@Override
	public int getNumRewards()
	{
		return rewards.size();
	}

	@Override
	public Rewards<Value> getRewards(int i)
	{
		return rewards.get(i).rews;
	}

	@Override
	public String getRewardName(int i)
	{
		return rewards.get(i).name;
	}

	@Override
	public Integer getRewardPosition(int i)
	{
		return rewards.get(i).position;
	}

	@Override
	public void checkForDeadlocks() throws PrismException
	{
		checkForDeadlocks(null);
	}

	@Override
	public abstract void checkForDeadlocks(BitSet except) throws PrismException;

	@Override
	public boolean equals(Object o)
	{
		if (o == null || !(o instanceof ModelExplicit))
			return false;
		ModelExplicit<?> model = (ModelExplicit<?>) o;
		if (numStates != model.numStates)
			return false;
		if (!initialStates.equals(model.initialStates))
			return false;
		return true;
	}

	@Override
	public boolean hasStoredPredecessorRelation()
	{
		return (predecessorRelation != null);
	}

	@Override
	public PredecessorRelation getPredecessorRelation(prism.PrismComponent parent, boolean storeIfNew)
	{
		if (predecessorRelation != null) {
			return predecessorRelation;
		}

		PredecessorRelation pre = PredecessorRelation.forModel(parent, this);

		if (storeIfNew) {
			predecessorRelation = pre;
		}
		return pre;
	}

	@Override
	public void clearPredecessorRelation()
	{
		predecessorRelation = null;
	}
}
