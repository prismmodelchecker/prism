package explicit;

import io.ExplicitModelImporter;
import parser.State;
import parser.Values;
import parser.VarList;
import prism.ActionList;
import prism.PrismException;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base class for explicit-state models that wrap around another model,
 * represented by an instance of {@link ModelExplicit<Value>}.
 *
 * Note: this extends  {@link ModelExplicit<Value>} but the fields in that
 * class are completely ignored; instead those of the wrapped model are used.
 * The exception is the evaluator, which may differ and so is stored.
 */
public abstract class ModelExplicitWrapper<Value> extends ModelExplicit<Value>
{
	/** The wrapped model */
	protected ModelExplicit<Value> model;

	// Mutators (for ModelExplicit)

	@Override
	public void copyFrom(Model<?> model)
	{
		this.model.copyFrom(model);
	}

	@Override
	public void copyFrom(Model<Value> model, int permut[])
	{
		this.model.copyFrom(model, permut);
	}

	@Override
	public void initialise(int numStates)
	{
		this.model.initialise(numStates);
	}

	@Override
	public void addInitialState(int i)
	{
		this.model.addInitialState(i);
	}

	@Override
	public void clearInitialStates()
	{
		this.model.clearInitialStates();
	}

	@Override
	public void addDeadlockState(int i)
	{
		this.model.addDeadlockState(i);
	}

	@Override
	public void buildFromExplicitImport(ExplicitModelImporter modelImporter) throws PrismException
	{
		this.model.buildFromExplicitImport(modelImporter);
	}

	@Override
	public void buildFromPrismExplicit(String filename) throws PrismException
	{
		this.model.buildFromPrismExplicit(filename);
	}

	@Override
	public void setStatesList(List<State> statesList)
	{
		this.model.setStatesList(statesList);
	}

	@Override
	public void setConstantValues(Values constantValues)
	{
		this.model.setConstantValues(constantValues);
	}

	@Override
	public void setVarList(VarList varList)
	{
		this.model.setVarList(varList);
	}

	@Override
	public void addLabel(String name, BitSet states)
	{
		this.model.addLabel(name, states);
	}

	@Override
	public String addUniqueLabel(String prefix, BitSet labelStates, Set<String> definedLabelNames)
	{
		return this.model.addUniqueLabel(prefix, labelStates, definedLabelNames);
	}

	// Accessors (for Model)

	@Override
	public ActionList getActionList()
	{
		return this.model.getActionList();
	}

	@Override
	public List<Object> getActions()
	{
		return this.model.getActions();
	}

	@Override
	public List<Object> findActionsUsed()
	{
		return this.model.findActionsUsed();
	}

	@Override
	public boolean onlyNullActionUsed()
	{
		return this.model.onlyNullActionUsed();
	}

	@Override
	public int actionIndex(Object action)
	{
		return this.model.actionIndex(action);
	}

	@Override
	public int getNumStates()
	{
		return this.model.getNumStates();
	}

	@Override
	public int getNumInitialStates()
	{
		return this.model.getNumInitialStates();
	}

	@Override
	public Iterable<Integer> getInitialStates()
	{
		return this.model.getInitialStates();
	}

	@Override
	public int getFirstInitialState()
	{
		return this.model.getFirstInitialState();
	}

	@Override
	public boolean isInitialState(int i)
	{
		return this.model.isInitialState(i);
	}

	@Override
	public int getNumDeadlockStates()
	{
		return this.model.getNumDeadlockStates();
	}

	@Override
	public Iterable<Integer> getDeadlockStates()
	{
		return this.model.getDeadlockStates();
	}

	@Override
	public StateValues getDeadlockStatesList()
	{
		return this.model.getDeadlockStatesList();
	}

	@Override
	public int getFirstDeadlockState()
	{
		return this.model.getFirstDeadlockState();
	}

	@Override
	public boolean isDeadlockState(int i)
	{
		return this.model.isDeadlockState(i);
	}

	@Override
	public List<State> getStatesList()
	{
		return this.model.getStatesList();
	}

	@Override
	public Values getConstantValues()
	{
		return this.model.getConstantValues();
	}

	@Override
	public VarList getVarList()
	{
		return this.model.getVarList();
	}

	@Override
	public BitSet getLabelStates(String name)
	{
		return this.model.getLabelStates(name);
	}

	@Override
	public boolean hasLabel(String name)
	{
		return this.model.hasLabel(name);
	}

	@Override
	public Set<String> getLabels()
	{
		return this.model.getLabels();
	}

	@Override
	public Map<String, BitSet> getLabelToStatesMap()
	{
		return this.model.getLabelToStatesMap();
	}

	@Override
	public SuccessorsIterator getSuccessors(int s)
	{
		return this.model.getSuccessors(s);
	}

	@Override
	public void findDeadlocks(boolean fix) throws PrismException
	{
		this.model.findDeadlocks(fix);
	}

	@Override
	public void checkForDeadlocks() throws PrismException
	{
		this.model.checkForDeadlocks();
	}

	@Override
	public void checkForDeadlocks(BitSet except) throws PrismException
	{
		this.model.checkForDeadlocks(except);
	}

	@Override
	public void exportToPrismLanguage(String filename, int precision) throws PrismException
	{
		this.model.exportToPrismLanguage(filename, precision);
	}
}
