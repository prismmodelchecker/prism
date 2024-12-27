//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

import java.io.File;

/**
 * Base class for "components" of PRISM, i.e. classes that implement
 * a particular piece of functionality required for model checking.
 * 
 * Stores:
 * <ul>
 * <li> A PrismLog ({@code mainLog}) for output 
 * <li> A PrismSettings object ({@code settings})
 * </ul>
 * 
 * Depending on the (sub)class, the {@code settings} object may either be read
 * from each time that a setting is required (thus respecting any changes that
 * are made to it over time), or read from initially and then ignored.
 * Mostly, these classes are freshly created to perform some task and then discarded,
 * so there is no point making changes to {@code settings} after creation.
 */
public class PrismComponent
{
	/**
	 * Log for output.
	 * Defaults to System.out, so that is always available.
	 */
	protected PrismLog mainLog = new PrismPrintStreamLog(System.out);

	/**
	 * PRISM settings object.
	 * Defaults to null to avoid delays when it will be copied from a parent object.
	 */
	protected PrismSettings settings = null;

	// Constructors
	
	/**
	 * Default constructor.
	 */
	public PrismComponent()
	{
		// Create a fresh PrismSettings() object containing PRISM defaults.
		settings = new PrismSettings();
	}

	/**
	 * Create a PrismComponent object, inheriting state from another ("parent") PrismComponent.
	 */
	public PrismComponent(PrismComponent parent)
	{
		if (parent == null)
			return;
		setLog(parent.getLog());
		setSettings(parent.getSettings());
	}
	
	// Setters (declared as final since they are called from the constructor)
	
	/**
	 * Set log ("mainLog") for output.
	 */
	public final void setLog(PrismLog mainLog)
	{
		this.mainLog = mainLog;
	}

	/**
	 * Set PRISMSettings object.
	 */
	public final void setSettings(PrismSettings settings)
	{
		this.settings = settings;
	}
	
	// Getters
	
	/**
	 * Get log ("mainLog") for output.
	 */
	public PrismLog getLog()
	{
		return mainLog;
	}
	
	/**
	 * Get the locally stored PRISMSettings object.
	 */
	public PrismSettings getSettings()
	{
		return settings;
	}

	/**
	 * Either create a new PrismFileLog for {@code file} or,
	 * if {@code file} is null, return the log.
	 * Throws a {@code PrismException} if there is a problem opening the file.
	 */
	public PrismLog getPrismLogForFile(File file) throws PrismException
	{
		return getPrismLogForFile(file, false);
	}

	/**
	 * Either create a new PrismFileLog for {@code file} or,
	 * if {@code file} is null, return the log.
	 * Throws a {@code PrismException} if there is a problem opening the file.
	 * If {@code append} is true, file should be opened in "append" mode.
	 */
	public PrismLog getPrismLogForFile(File file, boolean append) throws PrismException
	{
		// create new file log or use main log
		PrismLog tmpLog;
		if (file != null) {
			tmpLog = PrismFileLog.create(file.getPath(), append);
		} else {
			tmpLog = mainLog;
		}
		return tmpLog;
	}

	/**
	 * Get a string describing the output destination specified by a File:
	 * "to file \"filename\"..." if non-null; "below:" if null.
	 * See {@link #getPrismLogForFile(File)}.
	 */
	public static String getDestinationStringForFile(File file)
	{
		return (file == null) ? "below:" : "to file \"" + file + "\"...";
	}

}
