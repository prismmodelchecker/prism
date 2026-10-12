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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;

import org.junit.jupiter.api.Test;

import prism.PrismException;

/**
 * Tests for {@link ModelExportTask#setRequestedZipFileExtension}: when an export filename
 * is given with an explicit zip extension (e.g. "-exportmodel out.umb.gz"), which is stripped off
 * during command-line switch parsing, the file actually exported to should have exactly that name,
 * whether or not zipping is done, and for UMB (zipped in place) as well as other formats
 * (zipped afterwards by {@link ModelExportZipper}, which appends the extension).
 */
public class ModelExportTaskZipExtensionTest
{
	/**
	 * Create an export task as for "-exportmodel basename.ext.zipExt[:zip=false]",
	 * and return the name of the file that will actually exist after export.
	 */
	private static String exportedFilename(String basename, String ext, String zipExt, boolean zipped) throws PrismException
	{
		ModelExportTask task = ModelExportTask.fromFilename(basename, ext);
		task.getExportOptions().setZipped(zipped).setCompressionFormat(ModelExportOptions.CompressionFormat.fromExtension(zipExt).get());
		task.setRequestedZipFileExtension(zipExt);
		if (task.getFile() == null) {
			return null;
		}
		String filename = task.getFile().getPath();
		// Mimic ModelExportZipper, which appends the zip extension when it zips a (non-UMB) file
		if (task.getExportOptions().getFormat() != ModelExportFormat.UMB && task.getExportOptions().getZipped()) {
			filename += "." + task.getZipFileExtension();
		}
		return filename;
	}

	@Test
	void umbZipped() throws PrismException
	{
		assertEquals("out.umb.gz", exportedFilename("out", "umb", "gz", true));
		assertEquals("out.umb.xz", exportedFilename("out", "umb", "xz", true));
	}

	@Test
	void umbNotZipped() throws PrismException
	{
		assertEquals("out.umb.gz", exportedFilename("out", "umb", "gz", false));
	}

	@Test
	void explicitZipped() throws PrismException
	{
		assertEquals("out.tra.gz", exportedFilename("out", "tra", "gz", true));
	}

	@Test
	void explicitNotZipped() throws PrismException
	{
		assertEquals("out.tra.gz", exportedFilename("out", "tra", "gz", false));
	}

	@Test
	void stdout() throws PrismException
	{
		assertNull(exportedFilename("stdout", "umb", "gz", true));
		assertNull(exportedFilename("stdout", "tra", "gz", false));
	}

	@Test
	void emptyBasenameReplaced() throws PrismException
	{
		ModelExportTask task = ModelExportTask.fromFilename("", "umb");
		task.getExportOptions().setZipped(true);
		task.setRequestedZipFileExtension("gz");
		task.replaceEmptyFileBasename("poll2");
		assertEquals(new File("poll2.umb.gz"), task.getFile());
	}
}
