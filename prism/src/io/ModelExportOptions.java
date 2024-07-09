//==============================================================================
//
//	Copyright (c) 2024-
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

package io;

import java.util.Optional;

import static prism.PrismSettings.DEFAULT_EXPORT_MODEL_PRECISION;

/**
 * Class to represented options for exporting models.
 */
public class ModelExportOptions implements Cloneable
{
	/**
	 * Model export formats
	 */
	public enum ModelExportFormat {
		EXPLICIT, MATLAB, DOT, DRN;
		public String description()
		{
			switch (this) {
				case EXPLICIT:
					return "in plain text format";
				case MATLAB:
					return "in Matlab format";
				case DOT:
					return "in Dot format";
				case DRN:
					return "in DRN format";
				default:
					return this.toString();
			}
		}
	}

	/**
	 * Model export format
	 */
	private Optional<ModelExportOptions.ModelExportFormat> format = Optional.empty();

	/**
	 * Precision to export probabilities/etc. (number of significant decimal places)
	 */
	private Optional<Integer> modelPrecision = Optional.empty();

	/**
	 * Whether to show full state details
	 */
	private Optional<Boolean> showStates = Optional.empty();

	/**
	 * Whether to show actions
	 */
	private Optional<Boolean> showActions = Optional.empty();

	/**
	 * Whether to print (optional, commented) headers
	 */
	private Optional<Boolean> printHeaders = Optional.empty();

	/**
	 * Whether to show transitions in rows in explicit format
	 */
	private Optional<Boolean> explicitRows = Optional.empty();

	// Constructors

	/**
	 * Construct a StrategyExportOptions with default options.
	 */
	public ModelExportOptions()
	{
	}

	/**
	 * Construct a StrategyExportOptions with specified format and default options.
	 */
	public ModelExportOptions(ModelExportOptions.ModelExportFormat format)
	{
		setFormat(format);
	}

	// Set methods

	/**
	 * Set the model export format.
	 */
	public ModelExportOptions setFormat(ModelExportOptions.ModelExportFormat format)
	{
		this.format = Optional.of(format);
		return this;
	}

	/**
	 * Set precision to export probabilities/etc. (number of significant decimal places).
	 */
	public ModelExportOptions setModelPrecision(int modelPrecision)
	{
		this.modelPrecision = Optional.of(modelPrecision);
		return this;
	}

	/**
	 * Set whether to show full state details.
	 */
	public ModelExportOptions setShowStates(boolean showStates)
	{
		this.showStates = Optional.of(showStates);
		return this;
	}

	/**
	 * Set whether to show actions.
	 */
	public ModelExportOptions setShowActions(boolean showActions)
	{
		this.showActions = Optional.of(showActions);
		return this;
	}

	/**
	 * Set whether to print (optional, commented) headers.
	 */
	public ModelExportOptions setPrintHeaders(boolean printHeaders)
	{
		this.printHeaders = Optional.of(printHeaders);
		return this;
	}

	/**
	 * Set whether to show transitions in rows in explicit format
	 */
	public ModelExportOptions setExplicitRows(boolean explicitRows)
	{
		this.explicitRows = Optional.of(explicitRows);
		return this;
	}

	/**
	 * Apply any options that have been set in another {@link ModelExportOptions} to this one.
	 */
	public void apply(ModelExportOptions other)
	{
		if (other.format.isPresent()) {
			setFormat(other.getFormat());
		}
		if (other.modelPrecision.isPresent()) {
			setModelPrecision(other.getModelPrecision());
		}
		if (other.showStates.isPresent()) {
			setShowStates(other.getShowStates());
		}
		if (other.showActions.isPresent()) {
			setShowActions(other.getShowActions());
		}
		if (other.explicitRows.isPresent()) {
			setExplicitRows(other.getExplicitRows());
		}
	}

	/**
	 * Apply any options set here on top of those set in another {@link ModelExportOptions}.
	 * The resulting (fresh) {@link ModelExportOptions} is returned.
	 */
	public ModelExportOptions applyTo(ModelExportOptions other)
	{
		ModelExportOptions optionsNew = other.clone();
		optionsNew.apply(this);
		return optionsNew;
	}

	// Get methods

	/**
	 * Get the model export format.
	 */
	public ModelExportOptions.ModelExportFormat getFormat()
	{
		return format.orElse(ModelExportFormat.EXPLICIT);
	}

	/**
	 * Precision to export probabilities/etc. (number of significant decimal places).
	 */
	public int getModelPrecision()
	{
		return modelPrecision.orElse(DEFAULT_EXPORT_MODEL_PRECISION);
	}

	/**
	 * Whether to show full state details.
	 */
	public boolean getShowStates()
	{
		return showStates.orElse(true);
	}

	/**
	 * Whether to show actions.
	 */
	public boolean getShowActions()
	{
		return showActions.orElse(true);
	}

	/**
	 * Whether to show actions.
	 * If has not been set, returned {@code orElse} as a default.
	 */
	public boolean getShowActions(boolean orElse)
	{
		return showActions.orElse(orElse);
	}

	/**
	 * Whether to print (optional, commented) headers.
	 */
	public boolean getPrintHeaders()
	{
		return printHeaders.orElse(true);
	}

	/**
	 * Whether to show transitions in rows in explicit format
	 */
	public boolean getExplicitRows()
	{
		return explicitRows.orElse(false);
	}

	/**
	 * Perform a shallow copy of the options.
	 */
	@Override
	public ModelExportOptions clone()
	{
		try {
			return (ModelExportOptions) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError("Object#clone is expected to work for Cloneable objects", e);
		}
	}
}
