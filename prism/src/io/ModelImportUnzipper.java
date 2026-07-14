//==============================================================================
//
//	Copyright (c) 2026-
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

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;

/**
 * Utility to transparently read a model import file that may be zipped (compressed),
 * detected purely from its filename extension (".gz", ".gzip" or ".xz") - there is no
 * separate/explicit option to request decompression on import.
 * <br>
 * Note: this is not used for UMB imports, which detect compression from the file's
 * content (not its name), as part of reading the (single) UMB file itself.
 */
public class ModelImportUnzipper
{
	/**
	 * Open a file for buffered reading, transparently decompressing it first
	 * if its filename extension indicates that it is zipped (".gz", ".gzip" or ".xz").
	 * @param file The file to open
	 */
	public static BufferedReader openBuffered(File file) throws IOException
	{
		Optional<ModelExportOptions.CompressionFormat> zipFormat = ModelExportOptions.CompressionFormat.fromExtension(extension(file));
		if (zipFormat.isEmpty()) {
			return new BufferedReader(new FileReader(file));
		}
		InputStream in = new BufferedInputStream(new FileInputStream(file));
		try {
			CompressorInputStream zipIn = new CompressorStreamFactory().createCompressorInputStream(zipFormat.get().extension(), in);
			return new BufferedReader(new InputStreamReader(zipIn));
		} catch (CompressorException e) {
			in.close();
			throw new IOException("Could not read zipped file \"" + file + "\": " + e.getMessage());
		}
	}

	/**
	 * Get the file extension (without the dot) of a file's name, or "" if it has none.
	 */
	private static String extension(File file)
	{
		String name = file.getName();
		int dot = name.lastIndexOf('.');
		return dot == -1 ? "" : name.substring(dot + 1);
	}
}
