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

/**
 * Abstract base class for readers that convert single chars or pairs of chars.
 * 
 * @param <T> the type of the underlying reader
 */
public abstract class ReplacingReader<T extends BasicReader> implements BasicReader
{
	public static final int REPLACE = -2;

	public final T source;

	/**
	 * Create a converting reader on a source reader.
	 * 
	 * @param source the source reader
	 */
	public ReplacingReader(T source)
	{
		this.source = source;
	}

	@Override
	public void close() throws IOException
	{
		source.close();
	}

	@Override
	public abstract int read() throws IOException;

	/**
	 * Replace each occurrence of the replacement integer {@link #REPLACE} with a single character.
	 * 
	 * @param char_1 the character used as replacement
	 * @return A reader that performs the replacement on the fly
	 */
	public ToChar to(int char_1)
	{
		return new ToChar(this, char_1);
	}

	/**
	 * Replace each occurrence of the replacement integer {@link #REPLACE} with a pair of characters.
	 * 
	 * @param char_1 the first character of the pair used as replacement
	 * @param char_2 the second character of the pair used as replacement
	 * @return A reader that performs the replacement on the fly
	 */
	public ToPair to(int char_1, int char_2)
	{
		return new ToPair(this, char_1, char_2);
	}



	/**
	 * A reader that replaces all occurrences of a character with the replacement integer {@link ReplacingReader#REPLACE}.
	 */
	public static class FromChar extends ReplacingReader<BasicReader>
	{
		protected final int char_1;

		/**
		 * Wrap a source reader to perform the replacement.
		 * 
		 * @param source the source reader
		 * @param char_1 the character to be replaced
		 */
		public FromChar(BasicReader source, int char_1)
		{
			super(source);
			this.char_1 = char_1;
		}

		/**
		 * Read a single character from the input.
		 * 
		 * @return The next character as an integer in the range of 0 to 65536, {@link BasicReader#EOF} ({@value BasicReader#EOF}) or {@link #REPLACE} ({@value #REPLACE})
		 * @throws IOException If and I/O error occurs
		 */
		@Override
		public int read() throws IOException
		{
			int current = source.read();
			return current == char_1 ? REPLACE : current;
		}
	}



	/**
	 * A reader that replaces all occurrences of a pair of characters with the replacement integer {@link ReplacingReader#REPLACE}.
	 */
	public static class FromPair extends ReplacingReader<PeekableReader>
	{
		protected final int char_1;
		protected final int char_2;

		/**
		 * Wrap a source reader to perform the replacement.
		 * 
		 * @param source the source reader
		 * @param char_1 the first character to be replaced
		 * @param char_2 the second character to be replaced
		 */
		public FromPair(BasicReader source, int char_1, int char_2) throws IOException
		{
			this(new PeekableReader(source), char_1, char_2);
		}

		/**
		 * Wrap a source reader to perform the replacement.
		 * 
		 * @param source the source reader
		 * @param char_1 the first character to be replaced
		 * @param char_2 the second character to be replaced
		 */
		public FromPair(PeekableReader source, int char_1, int char_2)
		{
			super(source);
			this.char_1 = char_1;
			this.char_2 = char_2;
		}

		/**
		 * Read a single character from the input.
		 * 
		 * @return The next character as an integer in the range of 0 to 65536, {@link BasicReader#EOF} ({@value BasicReader#EOF}) or {@link #REPLACE} ({@value #REPLACE})
		 * @throws IOException If and I/O error occurs
		 */
		@Override
		public int read() throws IOException
		{
			int current = source.read();
			if (current == char_1 && source.peek() == char_2) {
				source.read();
				return REPLACE;
			}
			return current;
		}
	}



	/**
	 * A reader that replaces all occurrences the replacement integer {@link ReplacingReader#REPLACE} with a pair of characters.
	 */
	public static class ToChar extends ReplacingReader<ReplacingReader<?>>
	{
		protected final int repl_1;

		/**
		 * Wrap a source reader to perform the replacement.
		 * 
		 * @param source the source reader
		 * @param char_1 the character used as replacement
		 */
		public ToChar(ReplacingReader<?> source, int repl_1)
		{
			super(source);
			this.repl_1 = repl_1;
		}

		@Override
		public int read() throws IOException
		{
			int current = source.read();
			return current == REPLACE ? repl_1 : current; 
		}
	}



	/**
	 * A reader that replaces all occurrences the replacement integer {@link ReplacingReader#REPLACE} with a pair of characters.
	 */
	public static class ToPair extends ReplacingReader<ReplacingReader<?>>
	{
		protected final int repl_1;
		protected final int repl_2;
		protected boolean replacing = false;

		/**
		 * Wrap a source reader to perform the replacement.
		 * 
		 * @param source the source reader
		 * @param char_1 the first character of the pair used as replacement
		 * @param char_2 the second character of the pair used as replacement
		 */
		public ToPair(ReplacingReader<?> source, int repl_1, int repl_2)
		{
			super(source);
			this.repl_1 = repl_1;
			this.repl_2 = repl_2;
		}

		@Override
		public int read() throws IOException
		{
			if (replacing) {
				replacing = false;
				return repl_2;
			}
			int current = source.read();
			if (current == REPLACE) {
				replacing = true;
				return repl_1;
			}
			return current;
		}
	}
}
