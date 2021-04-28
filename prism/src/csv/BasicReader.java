//==============================================================================
//	
//	Copyright (c) 2021-
//	Authors:
//	* Steffen Märcker <steffen.maercker@tu-dresden.de> (TU Dresden)
//	
//	The original MIT-licensed code can be found at: https://github.com/merkste/SimpleCsvJava
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

package csv;

import java.io.IOException;
import java.io.Reader;

import csv.ReplacingReader.FromChar;
import csv.ReplacingReader.FromPair;

/**
 * Interface to provide simple reader-like access to an input stream
 */
public interface BasicReader extends AutoCloseable
{
	public static final int EOF = -1;

	@Override
	public abstract void close() throws IOException;

	/**
	 * Create a reader that replaces all occurrences of a character with the replacement integer {@link ReplacingReader#REPLACE}.
	 * 
	 * @param char_1 the character to be replaced
	 * @return A reader that performs the replacement on the fly
	 */
	default ReplacingReader<BasicReader> convert(int char_1)
	{
		return new FromChar(this, char_1);
	}

	/**
	 * Create a reader that replaces all occurrences of a pair of characters with the replacement integer {@link ReplacingReader#REPLACE}.
	 * 
	 * @param char_1 the first character of the pair to be replaced
	 * @param char_2 the second character of the pair to be replaced
	 * @return A reader that performs the replacement on the fly
	 */
	default ReplacingReader<PeekableReader> convert(int char_1, int char_2) throws IOException
	{
		return new FromPair(this, char_1, char_2);
	}

	/**
	 * Read a single character from the input.
	 * 
	 * @return returns The next character as an integer in the range of 0 to 65536 or {@link #EOF} ({@value #EOF})
	 * @throws IOException If and I/O error occurs
	 */
	int read() throws IOException;

	/**
	 * Create a reader that can be peeked for the next character.
	 * 
	 * @return A peekable reader
	 * @throws IOException If an I/O error occurs
	 */
	default PeekableReader peekable() throws IOException
	{
		if (this instanceof PeekableReader) {
			return (PeekableReader) this;
		}
		return new PeekableReader(this);
	}



	/**
	 * Wrapper to adapt a {@link Reader} to the interface of {@link BasicReader}.
	 */
	public static class Wrapper implements BasicReader
	{
		protected final Reader source;

		/**
		 * Wrap a source reader.
		 * 
		 * @param source the reader
		 */
		public Wrapper(Reader source)
		{
			this.source = source;
		}

		@Override
		public void close() throws IOException
		{
			source.close();
		}

		@Override
		public int read() throws IOException
		{
			return source.read();
		}
	}



	/**
	 * Wrapper to enable peeking the next character without advancing the underlying reader.
	 */
	public static class PeekableReader implements BasicReader
	{
		protected final BasicReader source;
		protected int next;

		/**
		 * Wrap a source reader.
		 * 
		 * @param source the reader
		 */
		public PeekableReader(Reader source) throws IOException
		{
			this.source = new BasicReader.Wrapper(source);
			this.next = source.read();
		}

		/**
		 * Wrap a source reader.
		 * 
		 * @param source the reader to be wrapped
		 */
		public PeekableReader(BasicReader source) throws IOException
		{
			this.source = source;
			this.next = source.read();
		}

		@Override
		public void close() throws IOException
		{
			source.close();
		}

		@Override
		public int read() throws IOException
		{
			int current = next;
			next = source.read();
			return current;
		}

		/**
		 * Peek the next character without advancing the underlying reader.
		 * 
		 * @return returns The next character as an integer in the range of 0 to 65536 or {@code EOF} (-1)
		 */
		public int peek()
		{
			return next;
		}
	}
}
