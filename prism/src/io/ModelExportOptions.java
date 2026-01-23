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

import io.github.pmctools.umbj.UMBFormat;

import java.util.Optional;

import static prism.PrismSettings.DEFAULT_EXPORT_MODEL_PRECISION;

/**
 * Class to represent options for exporting models.
 */
public class ModelExportOptions implements Cloneable
{
	/** File compression formats */
	public enum CompressionFormat {
		GZIP,
		XZ;
		public String extension()
		{
			switch (this) {
				case GZIP: return "gz";
				case XZ: return "xz";
				default: throw new IllegalStateException("Unknown compression format: " + this);
			}
		}

		public static CompressionFormat fromUMB(UMBFormat.CompressionFormat compressionFormat)
		{
			switch (compressionFormat) {
				case GZIP: return GZIP;
				case XZ: return XZ;
				default: throw new IllegalStateException("Unknown compression format: " + compressionFormat);
			}
		}

		public UMBFormat.CompressionFormat toUMB()
		{
			switch (this) {
				case GZIP: return UMBFormat.CompressionFormat.GZIP;
				case XZ: return UMBFormat.CompressionFormat.XZ;
				default: throw new IllegalStateException("Unknown compression format: " + this);
			}
		}
	}

	/**
	 * Model export format
	 */
	private Optional<ModelExportFormat> format = Optional.empty();

	/**
	 * Precision to export probabilities/etc. (number of significant decimal places)
	 */
	private Optional<Integer> modelPrecision = Optional.empty();

	/**
	 * Whether to show labels
	 */
	private Optional<Boolean> showLabels = Optional.empty();

	/**
	 * Whether to show rewards
	 */
	private Optional<Boolean> showRewards = Optional.empty();

	/**
	 * Whether to show full state details
	 */
	private Optional<Boolean> showStates = Optional.empty();

	/**
	 * Whether to show full observation details
	 */
	private Optional<Boolean> showObservations = Optional.empty();

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

	/**
	 * For binary formats, whether to show in textual form
	 */
	private Optional<Boolean> binaryAsText = Optional.empty();

	/**
	 * For formats that support it, whether to zip
	 */
	private Optional<Boolean> zipped = Optional.empty();

	/**
	 * Compression format to use (if zipping)
	 */
	private Optional<CompressionFormat> zipFormat = Optional.empty();

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
	public ModelExportOptions(ModelExportFormat format)
	{
		setFormat(format);
	}

	/**
	 * Copy constructor.
	 */
	public ModelExportOptions(ModelExportOptions exportOptions)
	{
		apply(exportOptions);
	}

	// Set methods

	/**
	 * Set the model export format.
	 */
	public ModelExportOptions setFormat(ModelExportFormat format)
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
	 * Set whether to show labels
	 */
	public ModelExportOptions setShowLabels(boolean showLabels)
	{
		this.showLabels = Optional.of(showLabels);
		return this;
	}

	/**
	 * Set whether to show rewards
	 */
	public ModelExportOptions setShowRewards(boolean showRewards)
	{
		this.showRewards = Optional.of(showRewards);
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
	 * Set whether to show full observation details.
	 */
	public ModelExportOptions setShowObservations(boolean showObservations)
	{
		this.showObservations = Optional.of(showObservations);
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
	 * Set whether to show binary formats in textual form
	 */
	public ModelExportOptions setBinaryAsText(boolean binaryAsText)
	{
		this.binaryAsText = Optional.of(binaryAsText);
		return this;
	}

	/**
	 * Set whether to zip the output file (for formats that support it)
	 */
	public ModelExportOptions setZipped(boolean zipped)
	{
		this.zipped = Optional.of(zipped);
		return this;
	}

	/**
	 * Set compression format to use (if zipping)
	 */
	public ModelExportOptions setCompressionFormat(CompressionFormat compressionFormat)
	{
		this.zipFormat = Optional.of(compressionFormat);
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
		if (other.showLabels.isPresent()) {
			setShowLabels(other.getShowLabels());
		}
		if (other.showRewards.isPresent()) {
			setShowRewards(other.getShowRewards());
		}
		if (other.showStates.isPresent()) {
			setShowStates(other.getShowStates());
		}
		if (other.showObservations.isPresent()) {
			setShowObservations(other.getShowObservations());
		}
		if (other.showActions.isPresent()) {
			setShowActions(other.getShowActions());
		}
		if (other.printHeaders.isPresent()) {
			setPrintHeaders(other.getPrintHeaders());
		}
		if (other.explicitRows.isPresent()) {
			setExplicitRows(other.getExplicitRows());
		}
		if (other.binaryAsText.isPresent()) {
			setBinaryAsText(other.getBinaryAsText());
		}
		if (other.zipped.isPresent()) {
			setZipped(other.getZipped());
		}
		if (other.zipFormat.isPresent()) {
			setCompressionFormat(other.getCompressionFormat());
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
	public ModelExportFormat getFormat()
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
	 * Whether to show labels.
	 */
	public boolean getShowLabels()
	{
		return showLabels.orElse(true);
	}

	/**
	 * Whether to show rewards.
	 */
	public boolean getShowRewards()
	{
		return showRewards.orElse(true);
	}

	/**
	 * Whether to show full state details.
	 */
	public boolean getShowStates()
	{
		return showStates.orElse(true);
	}

	/**
	 * Whether to show full observation details.
	 */
	public boolean getShowObservations()
	{
		return showObservations.orElse(true);
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
	 * Whether to show binary formats in textual form.
	 */
	public boolean getBinaryAsText()
	{
		return binaryAsText.orElse(false);
	}

	/**
	 * Whether to zip the output file (for formats that support it)
	 */
	public boolean getZipped()
	{
		// Only UMB defaults to zipped
		return zipped.orElse(getFormat() == ModelExportFormat.UMB);
	}

	/**
	 * Compression format to use (if zipping)
	 */
	public CompressionFormat getCompressionFormat()
	{
		return getCompressionFormat(CompressionFormat.GZIP);
	}

	/**
	 * Compression format to use (if zipping)
	 * @param orElse Default to use if has not been specified
	 */
	public CompressionFormat getCompressionFormat(CompressionFormat orElse)
	{
		return zipFormat.orElse(CompressionFormat.GZIP);
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
