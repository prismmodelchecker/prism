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

import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import prism.PrismException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility to zip (compress) a model export file after it has been written to disk,
 * for export formats where this is requested via {@link ModelExportOptions#getZipped()}.
 * The compressed file is written alongside the original, with the appropriate extension
 * (e.g. ".gz") appended to its name, and the uncompressed original is then deleted.
 * <br>
 * Note: this is not used for UMB exports, which are zipped (or not) as part of writing
 * the (single) UMB file itself, without altering its filename.
 */
public class ModelExportZipper
{
	/**
	 * If zipping was requested for the given export task, compress the file that was
	 * just exported to. Does nothing if zipping was not requested, if exporting to
	 * standard output (no file), or for UMB exports (zipped separately, see above).
	 * @param exportTask The (already completed) export task
	 */
	public static void zipIfRequested(ModelExportTask exportTask) throws PrismException
	{
		ModelExportOptions exportOptions = exportTask.getExportOptions();
		if (!isZippable(exportOptions)) {
			return;
		}
		zip(exportTask.getFile(), exportOptions, exportTask.getZipFileExtension());
	}

	/**
	 * If zipping was requested in {@code exportOptions}, compress the file that was
	 * just exported to (using the file extension for the requested compression format).
	 * Does nothing if zipping was not requested, if exporting to standard output
	 * (no file), or for UMB exports (zipped separately, see above).
	 * @param file The file that was just exported to
	 * @param exportOptions The options used for the export
	 */
	public static void zipIfRequested(File file, ModelExportOptions exportOptions) throws PrismException
	{
		if (!isZippable(exportOptions)) {
			return;
		}
		zip(file, exportOptions, exportOptions.getCompressionFormat().extension());
	}

	private static boolean isZippable(ModelExportOptions exportOptions)
	{
		// Not zipped for UMB (handled separately, see above) or if not requested;
		// also not zipped if this is just one of several appends to a shared file
		// (e.g. as part of exporting a combined/multi-entity file) - zipping is
		// only done once, for the final/standalone write to a file.
		return exportOptions.getFormat() != ModelExportFormat.UMB && exportOptions.getZipped() && !exportOptions.getAppendToFile();
	}

	private static void zip(File file, ModelExportOptions exportOptions, String zipExtension) throws PrismException
	{
		if (file == null) {
			return;
		}
		File zippedFile = new File(file.getPath() + "." + zipExtension);
		try (
				InputStream in = new BufferedInputStream(new FileInputStream(file));
				OutputStream fileOut = new BufferedOutputStream(new FileOutputStream(zippedFile));
				CompressorOutputStream zipOut = new CompressorStreamFactory().createCompressorOutputStream(exportOptions.getCompressionFormat().extension(), fileOut)
		) {
			byte[] buffer = new byte[65536];
			int len;
			while ((len = in.read(buffer)) != -1) {
				zipOut.write(buffer, 0, len);
			}
		} catch (IOException | CompressorException e) {
			throw new PrismException("Could not zip exported file \"" + file + "\": " + e.getMessage());
		}
		if (!file.delete()) {
			throw new PrismException("Could not delete uncompressed file \"" + file + "\" after zipping");
		}
	}
}
