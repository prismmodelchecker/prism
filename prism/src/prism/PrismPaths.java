//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <joachim.klein@automata.tools> (formerly TU Dresden)
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
import java.nio.file.Path;

import common.PathUtil;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;

/** Helper for resolving relative paths of resources, e.g., HOA automata files in properties */
public class PrismPaths
{
	/** The directory where the model is located (if known) */
	private Path modelDirectory = null;
	/** The directory where the properties file is located (if known) */
	private Path propertyDirectory = null;

	/**
	 * Constructor, initialise via ModulesFile or PropertiesFile location.
	 * All parameters may be {@code null}.
	 */
	public PrismPaths(ModulesFile mf, PropertiesFile pf)
	{
		if (mf != null && mf.getLocation() != null) {
			setModelDirectory(PathUtil.getDirectoryFromFile(mf.getLocation()));
		}
		if (pf != null && pf.getLocation() != null) {
			setPropertyDirectory(PathUtil.getDirectoryFromFile(pf.getLocation()));
		}
	}

	/** Get the model directory (if known, otherwise {@code null} */
	public Path getModelDirectory()
	{
		return modelDirectory;
	}

	/** Set the model directory (may be {@code null}) */
	public void setModelDirectory(Path modelDirectory)
	{
		this.modelDirectory = modelDirectory;
	}

	/** Get the properties file directory (if known, otherwise {@code null} */
	public Path getPropertyDirectory()
	{
		return propertyDirectory;
	}

	/** Set the properties file directory (may be {@code null}) */
	public void setPropertyDirectory(Path propertyDirectory)
	{
		this.propertyDirectory = propertyDirectory;
	}

	/**
	 * Resolve a file system location (in a property).
	 * For a relative location, the property file location or the modules file location
	 * are taken into account, with a fallback of the current working directory.
	 * Resolves the $MODEL_DIR, $PROPERTY_DIR and $WORKING_DIR macros.
	 *
	 * @param log PrismLog for explanation
	 * @param location the location (taken from the property)
	 * @param description a description of the type of the location
	 */
	public Path resolvePropertyResource(PrismLog log, String location, String description) throws PrismException
	{
		// do replacements
		if (location.startsWith("$MODEL_DIR")) {
			if (modelDirectory != null) {
				location = location.replace("$MODEL_DIR", modelDirectory.toString());
			} else {
				throw new PrismException("Can not replace $MODEL_DIR (location is unknown) for path: " + location);
			}
		} else if (location.startsWith("$PROPERTY_DIR")) {
			if (propertyDirectory != null) {
				location = location.replace("$PROPERTY_DIR", propertyDirectory.toString());
			} else {
				throw new PrismException("Can not replace $PROPERTY_DIR (location is unknown) for path: " + location);
			}
		} else if (location.startsWith("$WORKING_DIR")) {
			location = location.replace("$WORKING_DIR", new File(".").toPath().toAbsolutePath().toString());
		}

		Path spec = new File(location).toPath();
		if (spec.isAbsolute()) {
			return spec.normalize();
		} else if (propertyDirectory != null) {
			log.println("Resolving location of " + description + " '"+location+"' relative to properties file directory");
			return PathUtil.resolvePath(propertyDirectory, spec);
		} else if (modelDirectory != null) {
			log.println("Resolving location of " + description + " '"+location+"' relative to model file directory");
			return PathUtil.resolvePath(modelDirectory, spec);
		} else {
			log.println("Resolving location of " + description + " '"+location+"' relative to current working directory");
			return PathUtil.resolvePath(new File(".").toPath().toAbsolutePath(), spec);
		}
	}

}
