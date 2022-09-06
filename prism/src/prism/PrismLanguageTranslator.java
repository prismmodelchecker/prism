//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham)
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * Base class for classes that convert to PRISM language models from other languages.
 */
public abstract class PrismLanguageTranslator
{
	/**
	 * Get a name associated with the language to be translated from.
	 */
	public abstract String getName();
	
	/**
	 * Load a model to be translated from an input stream.
	 */
	public abstract void load(InputStream in) throws PrismException;
	
	/**
	 * Load a model to be translated from a string.
	 */
	public void load(String modelString) throws PrismException
	{
		load(new ByteArrayInputStream(modelString.getBytes()));
	}
	
	/**
	 * Load a model to be translated from a file.
	 */
	public void load(File file) throws PrismException
	{
		FileInputStream in;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new PrismException("File \"" + file.getPath() + "\" not found");
		}
		load(in);
	}
	
	/**
	 * Translate the loaded model to a PRISM language model and write it to an output stream.
	 */
	public abstract void translate(PrintStream out) throws PrismException;
	
	/**
	 * Translate the loaded model to a PRISM language model and return it as a string.
	 */
	public String translateToString() throws PrismException
	{
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(os);
		translate(out);
		try {
			return os.toString("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new PrismException("Error translating output stream to string: " + e.getMessage());
		}
	}
}
