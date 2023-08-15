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

import javax.swing.text.html.Option;

import static prism.PrismSettings.DEFAULT_EXPORT_MODEL_PRECISION;

import java.util.Optional;

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
	};

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
	 * Precision to export probabilities/etc. (number of significant decimal places)
	 */
	private Optional<Integer> modelPrecision = Optional.empty();

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
	 * Set precision to export probabilities/etc. (number of significant decimal places).
	 */
	public StrategyExportOptions setModelPrecision(int modelPrecision)
	{
		this.modelPrecision = Optional.of(modelPrecision);
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
		return mode.orElse(InducedModelMode.RESTRICT);
	}

	/**
	 * Whether to restrict strategy/model to reachable states.
	 */
	public boolean getReachOnly()
	{
		return reachOnly.orElse(!getType().equals(StrategyExportType.INDUCED_MODEL));
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
	 * Precision to export probabilities/etc. (number of significant decimal places).
	 */
	public int getModelPrecision()
	{
		return modelPrecision.orElse(DEFAULT_EXPORT_MODEL_PRECISION);
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
			return (StrategyExportOptions) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError("Object#clone is expected to work for Cloneable objects", e);
		}
	}
}
