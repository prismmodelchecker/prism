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

/**
 * Base class for "components" of PRISM, i.e. classes that implement
 * a particular piece of functionality required for model checking.
 * Stores:
 * <ul>
 * <li> A PrismLog ({@code mainLog}) for output 
 * <li> A PrismSettings object ({@code settings})
 * </ul>
 * These are usually freshly created to perform some task and then discarded.
 * In particular, settings are usually extracted from a PrismSettings object
 * upon creation, which is stored (to pass on to child PrismComponent objects)
 * but is not then monitored to detect when changes are made to settings later. 
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
	 * Defaults to a fresh PrismSettings() object containing PRISM defaults.
	 * 
	 * The idea is that settings are not extracted into local storage of
	 * Used (optionally) to initialise settings.
	 * Retained to allow it to be passed on to child PrismComponent objects.
	 * It is not monitored to detect when changes are made to settings later.
	 */
	protected PrismSettings settings = new PrismSettings();

	// Constructors
	
	/**
	 * Default constructor.
	 */
	public PrismComponent()
	{
	}

	/**
	 * Create a PrismComponent object, inheriting state from another ("parent") PrismComponent.
	 */
	public PrismComponent(PrismComponent parent) throws PrismException
	{
		if (parent == null)
			return;
		
		setLog(parent.getLog());
		setSettings(parent.getSettings());
	}
	
	// Log
	
	/**
	 * Set log ("mainLog") for output.
	 */
	public void setLog(PrismLog log)
	{
		this.mainLog = log;
	}

	/**
	 * Get log ("mainLog") for output.
	 */
	public PrismLog getLog()
	{
		return mainLog;
	}

	// Settings
	
	/**
	 * Set settings from a PRISMSettings object.
	 */
	public void setSettings(PrismSettings settings) throws PrismException
	{
		this.settings = settings;
	}
	
	/**
	 * Get the locally stored PRISMSettings object.
	 */
	public PrismSettings getSettings()
	{
		return settings;
	}
}
