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
import java.util.BitSet;
import java.util.List;
import java.util.function.Function;

import io.ExplicitModelImporter;
import io.PrismExplicitImporter;
import parser.State;
import prism.Evaluator;
import prism.ModelType;
import prism.PrismException;
import prism.PrismNotSupportedException;

/**
 * Interface for simple mutable explicit-state model representations.
 */
public interface ModelSimple<Value> extends Model<Value>
{
	/**
	 * Add a state to the list of initial states.
	 */
	public abstract void addInitialState(int i);

	/**
	 * Build (anew) from a list of transitions provided by an explicit model importer.
	 * Note that initial states are not configured
	 * so this needs to be done separately (using {@link #addInitialState(int)}.
	 */
	default void buildFromExplicitImport(ExplicitModelImporter modelImporter) throws PrismException
	{
		// Not implemented by default
		throw new PrismException("Explicit model not yet supported for this model");
	}

	/**
	 * Build (anew) from a list of transitions exported explicitly by PRISM (i.e. a .tra file).
	 * Note that initial states are not configured (since this info is not in the file),
	 * so this needs to be done separately (using {@link #addInitialState(int)}.
	 */
	default void buildFromPrismExplicit(String filename) throws PrismException
	{
		ExplicitModelImporter modelImporter = new PrismExplicitImporter(null, new File(filename), null, null, null, ModelType.DTMC);
		buildFromExplicitImport(modelImporter);
	}

	/**
	 * Clear all information for a state (i.e. remove all transitions).
	 */
	public abstract void clearState(int i);

	/**
	 * Add a new state and return its index.
	 */
	public abstract int addState();

	/**
	 * Add multiple new states.
	 */
	public abstract void addStates(int numToAdd);

	/**
	 * Set the associated (read-only) state list.
	 */
	public void setStatesList(List<State> statesList);

	/**
	 * Adds a label and the set the states that satisfy it.
	 * Any existing label with the same name is overwritten.
	 * @param name The name of the label
	 * @param states The states that satisfy the label 
	 */
	public void addLabel(String name, BitSet states);

	// Static helper methods

	/**
	 * Create a new ModelSimple object of the appropriate kind for a given model type
	 */
	public static ModelSimple<?> forModelType(ModelType modelType) throws PrismException
	{
		ModelSimple<?> prodModel = null;
		switch (modelType) {
			case DTMC:
				prodModel = new DTMCSimple<>();
				break;
			case MDP:
				prodModel = new MDPSimple<>();
				break;
			case POMDP:
				prodModel = new POMDPSimple<>();
				break;
			case IDTMC:
				prodModel = new IDTMCSimple<>();
				break;
			case IMDP:
				prodModel = new IMDPSimple<>();
				break;
			case STPG:
				prodModel = new STPGSimple<>();
				break;
			default:
				throw new PrismNotSupportedException("Model construction not supported for " + modelType + "s");
		}
		return prodModel;
	}

	/**
	 * Copy a model, creating a new {@link ModelSimple} of the appropriate type.
	 * @param model The model to copy
	 */
	static <V> ModelSimple<V> copy(Model<V> model) throws PrismException
	{
		ModelType modelType = model.getModelType();
		switch (modelType) {
			case DTMC:
				return new DTMCSimple<>((DTMC<V>) model);
			case CTMC:
				return new DTMCSimple<>((CTMC<V>) model);
			case MDP:
				return new MDPSimple<>((MDP<V>) model);
			default:
				throw new PrismNotSupportedException("Model copy not supported for " + modelType + "s");
		}

	}

	/**
	 * Copy a model, mapping probability values using the provided function.
	 * creating a new {@link ModelSimple} of the appropriate type.
	 * There is no attempt to check that distributions sum to one.
	 * @param model The model to copy
	 */
	static <V> ModelSimple<V> copy(Model<V> model, Function<? super V, ? extends V> probMap) throws PrismException
	{
		ModelType modelType = model.getModelType();
		switch (modelType) {
			case DTMC:
				return new DTMCSimple<>((DTMC<V>) model, probMap);
			case CTMC:
				return new CTMCSimple<>((CTMC<V>) model, probMap);
			case MDP:
				return new MDPSimple<>((MDP<V>) model, probMap);
			default:
				throw new PrismNotSupportedException("Model copy not supported for " + modelType + "s");
		}

	}

	/**
	 * Copy a model, mapping probability values using the provided function,
	 * creating a new {@link ModelSimple} of the appropriate type.
	 * There is no attempt to check that distributions sum to one.
	 * Since the type changes (V -> V2), an Evaluator for Value must be given.
	 * creating a new {@link ModelSimple} of the appropriate type.
	 * @param model The model to copy
	 */
	static <V, V2> ModelSimple<V2> copy(Model<V> model, Function<? super V, ? extends V2> probMap, Evaluator<V2> eval) throws PrismException
	{
		ModelType modelType = model.getModelType();
		switch (modelType) {
			case DTMC:
				return new DTMCSimple<>((DTMC<V>) model, probMap, eval);
			case CTMC:
				return new CTMCSimple<>((CTMC<V>) model, probMap, eval);
			case MDP:
				return new MDPSimple<>((MDP<V>) model, probMap, eval);
			default:
				throw new PrismNotSupportedException("Model copy not supported for " + modelType + "s");
		}

	}
}