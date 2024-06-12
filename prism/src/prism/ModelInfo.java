//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Nishan Kamaleson <nxk249@bham.ac.uk> (University of Birmingham)
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

package prism;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import parser.EvaluateContext;
import parser.EvaluateContext.EvalMode;
import parser.Values;
import parser.VarList;
import parser.ast.ASTElement;
import parser.ast.DeclarationType;
import parser.type.Type;

/**
 * Interface for classes that provide some basic (syntactic) information about a probabilistic model.
 */
public interface ModelInfo
{
	/**
	 * Get the type of probabilistic model.
	 */
	public ModelType getModelType();

	/**
	 * Set values for some undefined constants.
	 * The values being provided for these constants, as well as any other constants needed,
	 * are provided in an EvaluateContext object. This also determines the evaluation mode.
	 * If there are no undefined constants, {@code ecUndefined} can be null.
	 * Undefined constants can be subsequently redefined to different values with the same method.
	 * This may result in the values for other model constants now being known;
	 * the values for all current constant values (if set) are available via {@link #getConstantValues()}.
	 */
	public default void setSomeUndefinedConstants(EvaluateContext ecUndefined) throws PrismException
	{
		// Default implementation: assume no constants and do nothing.
		// So if constants are actually being provided, this is a problem.
		if (ecUndefined != null && ecUndefined.getConstantValues() != null && ecUndefined.getConstantValues().getNumValues() > 0) {
			throw new PrismLangException("This model has no constants to set");
		}
		// Also assume no support for exact evaluation by default
		if (ecUndefined != null && ecUndefined.getEvaluationMode() != EvalMode.FP) {
			throw new PrismLangException("Evaluation mode " + ecUndefined.getEvaluationMode() + " not supported");
		}
	}

	/**
	 * Set values for some undefined constants.
	 * It is preferable to use {@link #setSomeUndefinedConstants(EvaluateContext)} instead.
	 * By default, this method creates an {@link EvaluateContext} via {@link EvaluateContext#create(Values)}.
	 * If this will be called frequently, it is better to maintain your own {@link EvaluateContext}.
	 * Also, this method can only handle the default (floating point) evaluation mode.
	 */
	public default void setSomeUndefinedConstants(Values someValues) throws PrismException
	{
		setSomeUndefinedConstants(EvaluateContext.create(someValues));
	}

	/**
	 * Set values for some undefined constants.
	 * Deprecated. Better to use {@link #setSomeUndefinedConstants(EvaluateContext)}.
	 * @deprecated
	 */
	@Deprecated
	public default void setSomeUndefinedConstants(Values someValues, boolean exact) throws PrismException
	{
		setSomeUndefinedConstants(EvaluateContext.create(someValues, exact));
	}

	/**
	 * Get access to the values for all constants in the model, including the 
	 * undefined constants set previously via the method {@link #setSomeUndefinedConstants(EvaluateContext)}.
	 * Until they are set for the first time, this method may return null.
	 */
	public default Values getConstantValues()
	{
		// Default, get via getEvaluateContext()
		return getEvaluateContext().getConstantValues();
	}

	/**
	 * Get access to an EvaluateContext object defining the values
	 * for all constants in the model, including the undefined constants
	 * set previously via the method {@link #setSomeUndefinedConstants(EvaluateContext)}.
	 * This also specified the evaluation mode being used.
	 */
	public default EvaluateContext getEvaluateContext()
	{
		// By default, assume there are no constants to define (and FP)
		return EvaluateContext.create(new Values());
	}

	/**
	 * Does the model contain unbounded variables?
	 */
	public default boolean containsUnboundedVariables()
	{
		// By default, assume all variables are finite-ranging
		return false;
	}

	/**
	 * Get the number of variables in the model. 
	 */
	public default int getNumVars()
	{
		// Default implementation just extracts from getVarNames() 
		return getVarNames().size();
	}
	
	/**
	 * Get the names of all the variables in the model.
	 */
	public List<String> getVarNames();
	
	/**
	 * Look up the index of a variable in the model by name.
	 * Returns -1 if there is no such variable.
	 */
	public default int getVarIndex(String name)
	{
		// Default implementation just extracts from getVarNames() 
		return getVarNames().indexOf(name);
	}

	/**
	 * Get the name of the {@code i}th variable in the model.
	 * {@code i} should always be between 0 and getNumVars() - 1. 
	 */
	public default String getVarName(int i)
	{
		// Default implementation just extracts from getVarNames() 
		return getVarNames().get(i);
	}

	/**
	 * Get the types of all the variables in the model.
	 */
	public List<Type> getVarTypes();

	/**
	 * Get the type of the {@code i}th variable in the model.
	 * {@code i} should always be between 0 and getNumVars() - 1. 
	 */
	public default Type getVarType(int i) throws PrismException
	{
		// Default implementation just extracts from getVarTypes() 
		return getVarTypes().get(i);
	}

	/**
	 * Get a declaration providing more information about
	 * the type of the {@code i}th variable in the model.
	 * For example, for integer variables, this can define
	 * the lower and upper bounds of the range of the variable.
	 * This is specified using a subclass of {@link DeclarationType},
	 * which specifies info such as bounds using {@link parser.ast.Expression} objects.
	 * These can use constants which will later be supplied,
	 * e.g., via the {@link #setSomeUndefinedConstants(Values)} method.
	 * If this method is not provided, a default implementation supplies sensible
	 * declarations, but these are _unbounded_ for integers.
	 * {@code i} should always be between 0 and getNumVars() - 1.
	 */
	public default DeclarationType getVarDeclarationType(int i) throws PrismException
	{
		try {
			return getVarType(i).defaultDeclarationType();
		} catch (PrismLangException e) {
			throw new PrismException("No default declaration available for type " + getVarType(i));
		}
	}
	
	/**
	 * Get the (optionally specified) "module" that the {@code i}th variable in
	 * the model belongs to (e.g., the PRISM language divides models into such modules).
	 * This method returns the index of module; use {@link #getModuleName(int)}
	 * to obtain the name of the corresponding module.
	 * If there is no module info, or the variable is "global" and does not belong
	 * to a specific model, this returns -1. A default implementation always returns -1.
	 * {@code i} should always be between 0 and getNumVars() - 1.
	 */
	public default int getVarModuleIndex(int i)
	{
		// Default is -1 (unspecified or "global")
		return -1;
	}

	/**
	 * Get the name of the {@code i}th "module"; these are optionally used to
	 * organise variables within the model, e.g., in the PRISM modelling language.
	 * The module containing a variable is available via {@link #getVarModuleIndex(int)}.
	 * This method should return a valid module name for any (non -1)
	 * value returned by {@link #getVarModuleIndex(int)}.
	 */
	public default String getModuleName(int i)
	{
		// No names needed by default
		return null;
	}

	/**
	 * Create a {@link VarList} object, collating information about all the variables
	 * in the model. This provides various helper functions to work with variables.
	 * This list is created once all undefined constant values have been provided.
	 * A default implementation of this method creates a {@link VarList} object
	 * automatically from the variable info supplied by {@link ModelInfo}.
	 * Generally, this requires {@link #getVarDeclarationType(int)} to
	 * be properly implemented (beyond the default implementation) so that
	 * variable ranges can be determined.
	 */
	public default VarList createVarList() throws PrismException
	{
		return new VarList(this); 
	}
	
	/**
	 * Is the {@code i}th variable declared as observable?
	 * (for partially observable models)
	 * Technically, the only info needed to verify partially observable
	 * models is {@link #getObservableNames()} and {@link #getObservableTypes()}
	 * but in practice this is needed too to construct belief states efficiently.
	 */
	public default boolean isVarObservable(int i)
	{
		// Assume false (inefficient but safe)
		return false;
	}
	
	/**
	 * Get the number of variables declared as observable
	 * (for partially observable models)
	 * (derived from {@link #isVarObservable(int)} by default)
	 */
	public default int getNumObservableVars()
	{
		int count = 0;
		int numVars = getNumVars();
		for (int i = 0; i < numVars; i++) {
			if (isVarObservable(i)) {
				count++;
			}
		}
		return count;
	}
	
	/**
	 * Get the number of variables _not_ declared as observable
	 * (for partially observable models)
	 * (derived from {@link #isVarObservable(int)} by default)
	 */
	public default int getNumUnobservableVars()
	{
		return getNumVars() - getNumObservableVars();
	}
	
	/**
	 * Get the names of the variables declared as observable
	 * (for partially observable models)
	 * (derived from {@link #isVarObservable(int)} by default)
	 */
	public default List<String> getObservableVars()
	{
		List<String> list = new ArrayList<>();
		int numVars = getNumVars();
		for (int i = 0; i < numVars; i++) {
			if (isVarObservable(i)) {
				list.add(getVarName(i));
			}
		}
		return list;
	}
	
	/**
	 * Get the names of the variables _not_ declared as observable
	 * (for partially observable models)
	 * (derived from {@link #isVarObservable(int)} by default)
	 */
	public default List<String> getUnobservableVars()
	{
		List<String> list = new ArrayList<>();
		int numVars = getNumVars();
		for (int i = 0; i < numVars; i++) {
			if (!isVarObservable(i)) {
				list.add(getVarName(i));
			}
		}
		return list;
	}
	
	/**
	 * Get a short description of the action strings associated with transitions/choices.
	 * For example, for a PRISM model, this is "Module/[action]".
	 * The default implementation just returns "Action".
  	 */
	public default String getActionStringDescription()
	{
		return "Action";
	}
	
	/**
	 * Get the number of labels (atomic propositions) defined for the model. 
	 */
	public default int getNumLabels()
	{
		// Default implementation just extracts from getLabelNames() 
		return getLabelNames().size();
	}
	
	/**
	 * Get the names of all the labels in the model.
	 */
	public default List<String> getLabelNames()
	{
		// No labels by default
		return Collections.emptyList();
	}
	
	/**
	 * Get the name of the {@code i}th label of the model.
	 * {@code i} should always be between 0 and getNumLabels() - 1. 
	 */
	public default String getLabelName(int i) throws PrismException
	{
		// Default implementation just extracts from getLabelNames() 
		try {
			return getLabelNames().get(i);
		} catch (IndexOutOfBoundsException e) {
			throw new PrismException("Label number " + i + " not defined");
		}
	}
	
	/**
	 * Get the index of the label with name {@code name}.
	 * Indexed from 0. Returns -1 if label of that name does not exist.
	 */
	public default int getLabelIndex(String name)
	{
		// Default implementation just extracts from getLabelNames() 
		return getLabelNames().indexOf(name);
	}
	
	/**
	 * Get the number of observables defined for the model
	 * (for partially observable models only)
	 */
	public default int getNumObservables()
	{
		// Default implementation just extracts from getObservableNames()
		return getObservableNames().size();
	}
	
	/**
	 * Get the names of all the observables in the model
	 * (for partially observable models only)
	 */
	public default List<String> getObservableNames()
	{
		// No observables by default
		return Collections.emptyList();
	}
	
	/**
	 * Get the name of the {@code i}th observable of the model
	 * (for partially observable models only)
	 * {@code i} should always be between 0 and getNumObservables() - 1.
	 */
	public default String getObservableName(int i) throws PrismException
	{
		// Default implementation just extracts from getObservableNames()
		try {
			return getObservableNames().get(i);
		} catch (IndexOutOfBoundsException e) {
			throw new PrismException("Observable number " + i + " not defined");
		}
	}
	
	/**
	 * Get the index of the observable with name {@code name}
	 * (for partially observable models only)
	 * Indexed from 0. Returns -1 if observable of that name does not exist.
	 */
	public default int getObservableIndex(String name)
	{
		// Default implementation just extracts from getObservableNames()
		return getObservableNames().indexOf(name);
	}
	
	/**
	 * Get the types of all the observables in the model.
	 * (for partially observable models only)
	 */
	public default List<Type> getObservableTypes()
	{
		// No observables by default
		return Collections.emptyList();
	}

	/**
	 * Get the type of the {@code i}th observable in the model.
	 * (for partially observable models only)
	 * {@code i} should always be between 0 and getNumObservables() - 1.
	 */
	public default Type getObservableType(int i)
	{
		// Default implementation just extracts from getObservableTypes()
		return getObservableTypes().get(i);
	}
	
	/**
	 * Check if an identifier is used in the model
	 * (e.g., as a constant or variable)
	 */
	public default boolean isIdentUsed(String ident)
	{
		// Default implementation looks up any vars/consts
		if (getVarIndex(ident) != -1) {
			return true;
		}
		Values v = getConstantValues();
		if (v != null & v.contains(ident)) {
			return true;
		}
		return false;
	}

	/**
	 * Check if an identifier is already used in the model
	 * (e.g., as a constant or variable) and throw an exception if it is.
	 * @param ident The name of the identifier to check
	 * @param decl Where the identifier is declared in the model (for the error message)
	 * @param use Optionally, the identifier's usage (e.g. "constant")
	 */
	public default void checkIdent(String ident, ASTElement decl, String use) throws PrismLangException
	{
		// Default implementation via isIdentUsed
		if (isIdentUsed(ident)) {
			throw new PrismLangException("Identifier " + ident + " is already used in the model", decl);
		}
	}
	
	/**
	 * Check if an identifier (in double quotes) is used in the model
	 * (e.g., as a label)
	 */
	public default boolean isQuotedIdentUsed(String ident)
	{
		// Default implementation looks up any labels
		if (getLabelIndex(ident) != -1) {
			return true;
		}
		return false;
	}

	/**
	 * Check if an identifier (in double quotes) is already used in the model
	 * (e.g., as a label) and throw an exception if it is.
	 * @param ident The name of the identifier to check
	 * @param decl Where the identifier is declared in the model (for the error message)
	 * @param use Optionally, the identifier's usage (e.g. "constant")
	 */
	public default void checkQuotedIdent(String ident, ASTElement decl, String use) throws PrismLangException
	{
		// Default implementation via isQuotedIdentUsed
		if (isQuotedIdentUsed(ident)) {
			throw new PrismLangException("Identifier \"" + ident + "\" is already used in the model", decl);
		}
	}
}
