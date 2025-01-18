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

import parser.ast.PropertiesFile;
import prism.ModelInfo;
import prism.PrismException;

import java.io.File;

/**
 * Class to represent a task related to exporting models.
 */
public class ModelExportTask
{
	/**
	 * Model export entities
	 */
	public enum ModelExportEntity {
		MODEL, // Core part of the model (transition function/matrix)
		STATE_REWARDS, // State rewards
		TRANSITION_REWARDS, // Transition rewards
		STATES, // State definitions (variable values)
		OBSERVATIONS, // Observation definitions (observable values)
		LABELS; // Labels (atomic propositions)
		public String description()
		{
			switch (this) {
				case MODEL:
					return "model";
				case STATE_REWARDS:
					return "state rewards";
				case TRANSITION_REWARDS:
					return "transition rewards";
				case STATES:
					return "reachable states";
				case OBSERVATIONS:
					return "observations";
				case LABELS:
					return "labels and satisfying states";
				default:
					return this.toString();
			}
		}
	}

	/**
	 * Model export entity (what to export)
	 */
	private ModelExportEntity entity;

	/**
	 * File to export to (null means stdout)
	 */
	private File file;

	/**
	 * Model export options
	 */
	private ModelExportOptions exportOptions;

	/**
	 * Which labels to export
	 */
	public enum LabelExportSet {
		MODEL, // Just those in the model
		EXTRA, // Just those in the "extra" source of labels
		ALL // All (model and extra)
	}

	/**
	 * When exporting labels, which ones to include
	 */
	private LabelExportSet labelExportSet = LabelExportSet.MODEL;

	/**
	 * Optionally, a source of extra labels for label export
	 */
	private PropertiesFile extraLabelsSource;

	// Constructors

	/**
	 * Construct a ModelExportTask with default options.
	 * @param entity What to export
	 * @param file File to export to (null means stdout)
	 */
	public ModelExportTask(ModelExportEntity entity, File file)
	{
		this(entity, file, new ModelExportOptions());
	}

	/**
	 * Construct a ModelExportTask with specified options.
	 * @param entity What to export
	 * @param file File to export to (null means stdout)
	 * @param exportOptions The options for export
	 */
	public ModelExportTask(ModelExportEntity entity, File file, ModelExportOptions exportOptions)
	{
		this.entity = entity;
		this.file = file;
		this.exportOptions = exportOptions;
	}

	/**
	 * Construct a ModelExportTask with specified options.
	 * @param entity What to export
	 * @param filename Name of file to export to (can be "stdout")
	 */
	public ModelExportTask(ModelExportEntity entity, String filename)
	{
		this(entity, filename, new ModelExportOptions());
	}

	/**
	 * Construct a ModelExportTask with specified options.
	 * @param entity What to export
	 * @param filename Name of file to export to (can be "stdout")
	 * @param exportOptions The options for export
	 */
	public ModelExportTask(ModelExportEntity entity, String filename, ModelExportOptions exportOptions)
	{
		this.entity = entity;
		this.file = "stdout".equals(filename) ? null : new File(filename);
		this.exportOptions = exportOptions;
	}

	/**
	 * Copy constructor.
	 */
	public ModelExportTask(ModelExportTask exportTask)
	{
		this(exportTask, new ModelExportOptions(exportTask.exportOptions));
	}

	/**
	 * Copy constructor, but with a specified ModelExportOptions.
	 */
	public ModelExportTask(ModelExportTask exportTask, ModelExportOptions exportOptions)
	{
		this.entity = exportTask.entity;
		this.file = exportTask.file;
		this.exportOptions = exportOptions;
		this.labelExportSet = exportTask.labelExportSet;
		this.extraLabelsSource = exportTask.extraLabelsSource;
	}

	/**
	 * Create a ModelExportTask based on a filename, supplied as separate basename and extension.
	 * The basename can also be "stdout". It can also be left empty ("") and later replaced
	 * (e.g. with the model basename) using {@link #replaceEmptyFileBasename(String)}.
	 * An unknown (or missing) extension is treated as ".tra".
	 */
	public static ModelExportTask fromFilename(String basename, String ext) throws PrismException
	{
		if (ext == null || ext.equals("")) {
			return new ModelExportTask(ModelExportEntity.MODEL, basename);
		}
		String filename = "stdout".equals(basename) ? "stdout" : basename + "." + ext;
		switch (ext) {
			case "tra":
				return new ModelExportTask(ModelExportEntity.MODEL, filename);
			case "srew":
				return new ModelExportTask(ModelExportEntity.STATE_REWARDS, filename);
			case "trew":
				return new ModelExportTask(ModelExportEntity.TRANSITION_REWARDS, filename);
			case "sta":
				return new ModelExportTask(ModelExportEntity.STATES, filename);
			case "obs":
				return new ModelExportTask(ModelExportEntity.OBSERVATIONS, filename);
			case "lab":
				return new ModelExportTask(ModelExportEntity.LABELS, filename);
			case "dot":
				return fromFormat(filename, ModelExportFormat.DOT);
			case "drn":
				return fromFormat(filename, ModelExportFormat.DRN);
			default:
				// Treat unknown extensions as .tra
				return new ModelExportTask(ModelExportEntity.MODEL, filename);
		}
	}

	/**
	 * Create a ModelExportTask to export a model to a file in the specified format.
	 * @param filename Name of file to export to (can be "stdout")
	 * @param exportFormat The format to use for export
	 */
	public static ModelExportTask fromFormat(String filename, ModelExportFormat exportFormat) throws PrismException
	{
		File file = "stdout".equals(filename) ? null : new File(filename);
		return fromOptions(file, new ModelExportOptions(exportFormat));
	}

	/**
	 * Create a ModelExportTask to export a model to a file in the specified format.
	 * @param file File to export to (null means stdout)
	 * @param exportFormat The format to use for export
	 */
	public static ModelExportTask fromFormat(File file, ModelExportFormat exportFormat) throws PrismException
	{
		return fromOptions(file, new ModelExportOptions(exportFormat));
	}

	/**
	 * Create a ModelExportTask to export a model to a file,
	 * using the supplied export options (which includes the format).
	 * @param file File to export to (null means stdout)
	 * @param exportOptions The options for export
	 */
	public static ModelExportTask fromOptions(File file, ModelExportOptions exportOptions) throws PrismException
	{
		ModelExportTask exportTask;
		switch (exportOptions.getFormat()) {
			case EXPLICIT:
			case MATLAB:
				exportTask = new ModelExportTask(ModelExportEntity.MODEL, file);
				break;
			case DOT:
				exportTask = new ModelExportTask(ModelExportEntity.MODEL, file);
				exportTask.getExportOptions().setShowStates(true);
				break;
			case DRN:
				exportTask = new ModelExportTask(ModelExportEntity.MODEL, file);
				break;
			default:
				return null;
		}
		exportTask.getExportOptions().apply(exportOptions);
		return exportTask;
	}

	// Set methods

	/**
	 * Set what is to be exported.
	 * @param entity What to export
	 */
	public void setEntity(ModelExportEntity entity)
	{
		this.entity = entity;
	}

	/**
	 * Set where to export to.
	 * @param file File to export to (null means stdout)
	 */
	public void setFile(File file)
	{
		this.file = file;
	}

	/**
	 * Set the export options.
	 * @param exportOptions Export options
	 */
	public void setExportOptions(ModelExportOptions exportOptions)
	{
		this.exportOptions = exportOptions;
	}

	/**
	 * Set, when exporting labels, which ones to export.
	 * @param labelExportSet Which set of labels to export
	 */
	public void setLabelExportSet(LabelExportSet labelExportSet)
	{
		this.labelExportSet = labelExportSet;
	}

	/**
	 * Set a source of extra labels
	 * @param extraLabelsSource Extra labels source
	 */
	public void setExtraLabelsSource(PropertiesFile extraLabelsSource)
	{
		this. extraLabelsSource = extraLabelsSource;
	}

	/**
	 * If the file to be exported to is of the form ".ext", plug in the supplied basename,
	 * i.e., make the new filename "basename.ext".
	 */
	public void replaceEmptyFileBasename(String basename)
	{
		if (file != null && file.getName().matches("\\.[a-zA-Z]+")) {
			file = new File(basename + file.getName());
		}
	}

	// Get methods

	/**
	 * Get what is to be exported.
	 */
	public ModelExportEntity getEntity()
	{
		return entity;
	}

	/**
	 * Is this export task applicable for a given model?
	 */
	public boolean isApplicable(ModelInfo modelInfo)
	{
		if (entity == ModelExportEntity.OBSERVATIONS && !modelInfo.getModelType().partiallyObservable()) {
			return false;
		}
		return true;
	}

	/**
	 * Get where to export to (null means stdout).
	 */
	public File getFile()
	{
		return file;
	}

	/**
	 * Get the export options.
	 */
	public ModelExportOptions getExportOptions()
	{
		return exportOptions;
	}

	/**
	 * Get which ones to export when exporting labels.
	 */
	public LabelExportSet getLabelExportSet()
	{
		return labelExportSet;
	}

	/**
	 * Get an (optional) source of extra labels
	 */
	public PropertiesFile getExtraLabelsSource()
	{
		return extraLabelsSource;
	}

	/**
	 * Get whether extra labels are used for this export task.
	 */
	public boolean extraLabelsUsed()
	{
		return labelExportSet == LabelExportSet.EXTRA || labelExportSet == LabelExportSet.ALL;
	}

	/**
	 * Get whether "init" should be included with model labels.
	 */
	public boolean initLabelIncluded()
	{
		return true;
	}

	/**
	 * Get whether "deadlock" should be included with model labels.
	 */
	public boolean deadlockLabelIncluded()
	{
		return true;
	}

	/**
	 * Get a message describing the export task to be done.
	 */
	public String getMessage()
	{
		String s = "Exporting " + entity.description();
		s += " " + exportOptions.getFormat().description();
		s += " " + getDestinationStringForFile(file);
		return s;
	}

	// Utility methods

	/**
	 * Get a string describing the output destination specified by a File:
	 * "to file \"filename\"..." if non-null; "below:" if null
	 */
	private static String getDestinationStringForFile(File file)
	{
		return (file == null) ? "below:" : "to file \"" + file + "\"...";
	}
}
