//==============================================================================
//
//	Copyright (c) 2023-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

package strat;

import io.ModelExportOptions;

import java.util.Optional;

import strat.StrategyInfo.UndefinedReason;

/**
 * Class to represent options for exporting strategies.
 */
public class StrategyExportOptions implements Cloneable
{
	/**
	 * Types of strategy export
	 */
	public enum StrategyExportType {
		ACTIONS, INDICES, INDUCED_MODEL, DOT_FILE;
		public String description()
		{
			switch (this) {
				case ACTIONS:
					return "as actions";
				case INDICES:
					return "as indices";
				case INDUCED_MODEL:
					return "as an induced model";
				case DOT_FILE:
					return "as a dot file";
				default:
					return this.toString();
			}
		}
	}

	/**
	 * Type of strategy export
	 */
	private StrategyExportType type;

	/**
	 * Modes of construction for an induced model:
	 * "restrict" (same model type but restrict to selected action choices); or
	 * "reduce" (change mode type by removing nondeterminism)
	 */
	public enum InducedModelMode {
		RESTRICT, REDUCE;
		public String description()
		{
			switch (this) {
				case RESTRICT:
					return "restricted";
				case REDUCE:
					return "reduced";
				default:
					return this.toString();
			}
		}
	}

	/**
	 * Mode of construction for an induced model.
	 */
	private Optional<InducedModelMode> mode = Optional.empty();

	/**
	 * Whether to restrict strategy/model to reachable states
	 */
	private Optional<Boolean> reachOnly = Optional.empty();

	/**
	 * Whether to show full state details
	 */
	private Optional<Boolean> showStates = Optional.empty();

	/**
	 * Whether to merge observationally equivalent states in partially observable models
	 */
	private Optional<Boolean> mergeObs = Optional.empty();

	/**
	 * For induced model export, the model export options.
	 */
	private Optional<ModelExportOptions> inducedModelExportOptions = Optional.empty();

	/**
	 * Policy for resolving undefined strategy choices (e.g., for induced model construction)
	 */
	private Optional<UndefinedChoicePolicy> undefinedChoicePolicy = Optional.empty();

	/**
	 * Whether to show (reasons for) undefined strategy choices (for export of actions/indices)
	 */
	private Optional<Boolean> showUndefined = Optional.empty();

	// Constructors

	/**
	 * Construct a StrategyExportOptions with default options.
	 */
	public StrategyExportOptions()
	{
		this(StrategyExportType.ACTIONS);
	}

	/**
	 * Construct a StrategyExportOptions with specified export type and default options.
	 */
	public StrategyExportOptions(StrategyExportType type)
	{
		this.type = type;
	}

	// Set methods

	/**
	 * Set the type of strategy export.
	 */
	public StrategyExportOptions setType(StrategyExportType type)
	{
		this.type = type;
		return this;
	}

	/**
	 * Set the mode of construction for an induced model:
	 * "restrict" (same model type but restrict to selected action choices); or
	 * "reduce" (change mode type by removing nondeterminism)
	 */
	public StrategyExportOptions setMode(InducedModelMode mode)
	{
		this.mode = Optional.of(mode);
		return this;
	}

	/**
	 * Set whether to restrict strategy/model to reachable states.
	 */
	public StrategyExportOptions setReachOnly(boolean reachOnly)
	{
		this.reachOnly = Optional.of(reachOnly);
		return this;
	}

	/**
	 * Set whether to show full state details.
	 */
	public StrategyExportOptions setShowStates(boolean showStates)
	{
		this.showStates = Optional.of(showStates);
		return this;
	}

	/**
	 * Set whether to merge observationally equivalent states in partially observable models.
	 */
	public StrategyExportOptions setMergeObservations(boolean mergeObs)
	{
		this.mergeObs = Optional.of(mergeObs);
		return this;
	}

	/**
	 * Set the model export options specifying details for induced model export.
	 */
	public StrategyExportOptions setInducedModelExportOptions(ModelExportOptions inducedModelExportOptions)
	{
		this.inducedModelExportOptions = Optional.of(inducedModelExportOptions);
		return this;
	}

	/**
	 * Set the policy for resolving undefined strategy choices (e.g., for induced model construction).
	 */
	public StrategyExportOptions setUndefinedChoicePolicy(UndefinedChoicePolicy undefinedChoicePolicy)
	{
		this.undefinedChoicePolicy = Optional.of(undefinedChoicePolicy);
		return this;
	}

	/**
	 * Set how to resolve undefined strategy choices for a given reason
	 * (other reasons are resolved as before, or as by default if not yet set).
	 */
	public StrategyExportOptions setUndefinedChoiceResolution(UndefinedReason reason, UndefinedChoicePolicy.Resolution resolution)
	{
		undefinedChoicePolicy = Optional.of(getUndefinedChoicePolicy().set(reason, resolution));
		return this;
	}

	/**
	 * Set how to resolve undefined strategy choices for all reasons except UNKNOWN
	 * (see {@link UndefinedChoicePolicy#setForAllKnownReasons}).
	 */
	public StrategyExportOptions setUndefinedChoiceResolutionForAllKnownReasons(UndefinedChoicePolicy.Resolution resolution)
	{
		undefinedChoicePolicy = Optional.of(getUndefinedChoicePolicy().setForAllKnownReasons(resolution));
		return this;
	}

	// Get methods

	/**
	 * Get the type of strategy export.
	 */
	public StrategyExportType getType()
	{
		return type;
	}

	/**
	 * Get the mode of construction for an induced model.
	 */
	public InducedModelMode getMode()
	{
		return mode.orElse(InducedModelMode.REDUCE);
	}

	/**
	 * Whether to restrict strategy/model to reachable states.
	 */
	public boolean getReachOnly()
	{
		return reachOnly.orElse(true);
	}

	/**
	 * Whether to show full state details.
	 */
	public boolean getShowStates()
	{
		return showStates.orElse(true);
	}

	/**
	 * Whether to merge observationally equivalent states in partially observable models.
	 */
	public boolean getMergeObservations()
	{
		return mergeObs.orElse(true);
	}

	/**
	 * Get the policy for resolving undefined strategy choices (e.g., for induced model construction).
	 */
	public UndefinedChoicePolicy getUndefinedChoicePolicy()
	{
		return undefinedChoicePolicy.orElseGet(UndefinedChoicePolicy::new);
	}

	/**
	 * Whether to show (reasons for) undefined strategy choices (for export of actions/indices).
	 */
	public boolean getShowUndefined()
	{
		return showUndefined.orElse(false);
	}

	/**
	 * Set whether to show (reasons for) undefined strategy choices (for export of actions/indices).
	 */
	public StrategyExportOptions setShowUndefined(boolean showUndefined)
	{
		this.showUndefined = Optional.of(showUndefined);
		return this;
	}

	/**
	 * Precision to export probabilities/etc. (number of significant decimal places).
	 */
	public int getModelPrecision()
	{
		return getInducedModelExportOptions().getModelPrecision();
	}

	/**
	 * Get the model export options for induced model export.
	 */
	public ModelExportOptions getInducedModelExportOptions()
	{
		return inducedModelExportOptions.orElse(new ModelExportOptions());
	}

	/**
	 * Get string description for type of strategy export.
	 */
	public String description()
	{
		String s = getType().description();
		if (getType() == StrategyExportType.INDUCED_MODEL) {
			s += " (" + getMode().description() + ")";
		}
		return s;
	}

	/**
	 * Perform a shallow copy of the options.
	 */
	@Override
	public StrategyExportOptions clone()
	{
		try {
			StrategyExportOptions copy = (StrategyExportOptions) super.clone();
			copy.undefinedChoicePolicy = undefinedChoicePolicy.map(UndefinedChoicePolicy::clone);
			return copy;
		} catch (CloneNotSupportedException e) {
			throw new InternalError("Object#clone is expected to work for Cloneable objects", e);
		}
	}
}
