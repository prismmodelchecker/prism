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

package common;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;

/**
 * Static helpers for dealing with paths.
 */
public class PathUtil
{
	/**
	 * Returns the absolute path for the directory containing
	 * the argument file. Returns {@code null} on error, e.g.,
	 * if the file does not exist.
	 */
	public static File getDirectoryFromFile(File file)
	{
		return getDirectoryFromFile(file.toPath()).toFile();
	}

	/**
	 * Returns the absolute path for the directory containing
	 * the argument file. Returns {@code null} on error, e.g.,
	 * if the file does not exist.
	 */
	public static Path getDirectoryFromFile(Path filePath)
	{
		try {
			return filePath.toRealPath().getParent();
		} catch (IOException e) {
			// E.g., file does not exist
			return null;
		}
	}

	/**
	 * Returns the path for the directory that is used for loading
	 * resources (e.g. a HOA automata file) in a property,
	 * extracting locations from the ModulesFile and PropertiesFile.
	 * <br>
	 * The location of the properties file is preferred, then the location
	 * of the modules file. If there is no location, {@code null} is returned
	 * and should be treated as "current working directory".
	 */
	public static Path getDirectoryForRelativePropertyResource(ModulesFile mf, PropertiesFile pf)
	{
		if (pf != null && pf.getLocation() != null) {
			return getDirectoryFromFile(pf.getLocation());
		}

		if (mf != null && mf.getLocation() != null) {
			return getDirectoryFromFile(mf.getLocation());
		}

		return null;
	}

	/**
	 * Resolves path2 relative to path1 (which has to point to a directory).
	 * If path2 is absolute, path1 is ignored.
	 * If path1 is {@code null}, resolution happens relative
	 * to the current working directory.
	 */
	public static Path resolvePath(Path path1, Path path2)
	{
		if (path1 == null) {
			path1 = new File(".").toPath();
		}
		return path1.resolve(path2);
	}

}
