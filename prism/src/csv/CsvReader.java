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
import java.io.StringReader;
import java.util.*;

import csv.BasicReader.PeekableReader;

import static csv.BasicReader.CR;
import static csv.BasicReader.LF;
import static csv.BasicReader.EOF;

/**
 * A class that implements a simple CSV reader according to RFC 4180.
 * The field separator can be configured to another character than a comma.
 * The line endings can be configured to either {@link BasicReader#CR} ({@value BasicReader#CR}), {@link BasicReader#LF} ({@value BasicReader#LF})  or {@link #CR_LF} ({@value #CR_LF}) using the provided constants.
 * 
 * @see <a href="https://tools.ietf.org/html/rfc4180">RFC 4180</a>
 */
public class CsvReader implements AutoCloseable, Iterable<String[]>
{
	public static final int CR_LF = ReplacingReader.REPLACE;
	public static final int EOL = ReplacingReader.REPLACE;
	public static final int COMMA = (int) ',';
	public static final char DOUBLE_QUOTES = (int) '"';
	public static final String[] STRING_ARRAY = new String[0];

	protected final PeekableReader input;
	protected final String lineSeparator;
	protected final int fieldSeparator;
	protected final String[] header;
	protected boolean hasNextRecord = true;
	protected int numFields;
	protected int line = 1;
	protected int column = 0;

	/**
	 * Create a CSV reader on an input.
	 * Assume the file starts with a header of distinct fields and all records have the same number of fields.
	 * Use a comma as field separator and CR;LF as line ending.
	 *
	 * @param reader the underlying reader
	 * @throws CsvFormatException If a CSV syntax error occurs
	 * @throws IOException        If an I/O error occurs
	 */
	public CsvReader(BasicReader reader) throws CsvFormatException, IOException
	{
		this(reader, true, true, true, COMMA, CR_LF);
	}

	/**
	 * Create a CSV reader on an input.
	 * Assume the file starts with a header of distinct fields and all records have the same number of fields.
	 *
	 * @param reader         the underlying reader
	 * @param fieldSeparator the field separator, e.g., a comma or semicolon
	 * @param lineSeparator  the line ending, i.e., either {@link BasicReader#CR} ({@value BasicReader#CR}), {@link BasicReader#LF} ({@value BasicReader#LF})  or {@link #CR_LF} ({@value #CR_LF})
	 * @throws CsvFormatException If a CSV syntax error occurs
	 * @throws IOException        If an I/O error occurs
	 */
	public CsvReader(BasicReader reader, int fieldSeparator, int lineSeparator) throws CsvFormatException, IOException
	{
		this(reader, true, true, true, fieldSeparator, lineSeparator);
	}

	/**
	 * Create a CSV reader on an input.
	 * Use a comma as field separator and CR;LF as line ending.
	 *
	 * @param reader             the underlying reader
	 * @param hasHeader          treat the first line as header
	 * @param fixNumFields       ensure all records have the same number of fields
	 * @param distinctFieldNames check that the header fields are distinct
	 * @throws CsvFormatException If a CSV syntax error occurs
	 * @throws IOException        If an I/O error occurs
	 */
	public CsvReader(BasicReader reader, boolean hasHeader, boolean fixNumFields, boolean distinctFieldNames) throws CsvFormatException, IOException
	{
		this(reader, hasHeader, fixNumFields, distinctFieldNames, COMMA, CR_LF);
	}

	/**
	 * Create a CSV reader on an input.
	 *
	 * @param reader             the underlying reader
	 * @param hasHeader          treat the first line as header
	 * @param fixNumFields       ensure all records have the same number of fields
	 * @param distinctFieldNames check that the header fields are distinct
	 * @param fieldSeparator     the field separator, e.g., a comma or semicolon
	 * @param lineSeparator      the line ending, i.e., either {@link BasicReader#CR} ({@value BasicReader#CR}), {@link BasicReader#LF} ({@value BasicReader#LF})  or {@link #CR_LF} ({@value #CR_LF})
	 * @throws CsvFormatException If a CSV syntax error occurs
	 * @throws IOException        If an I/O error occurs
	 */
	public CsvReader(BasicReader reader, boolean hasHeader, boolean fixNumFields, boolean distinctFieldNames, int fieldSeparator, int lineSeparator) throws CsvFormatException, IOException
	{
		this.fieldSeparator = fieldSeparator;
		switch (lineSeparator) {
			case CR:
				this.lineSeparator = "\r";
				this.input = reader.convert(CR).to(EOL).peekable();
				break;
			case LF:
				this.lineSeparator = "\n";
				this.input = reader.convert(LF).to(EOL).peekable();
				break;
			case CR_LF:
				this.lineSeparator = "\r\n";
				this.input = reader.convert(CR, LF).to(EOL).peekable();
				break;
			default:
				throw new IllegalArgumentException("Expected lineSeparator to be either CR (" + CR + "), LF (" + LF + ") or CR_LF" + (CR_LF) + ") but got: " + lineSeparator);
		}
		this.line = 1;
		this.column = 0;
		this.numFields = fixNumFields ? 0 : -1;
		if (hasHeader) {
			header = nextRecord();
			if (!hasNextRecord()) {
				throw new CsvFormatException("no record found except for header", line);
			}
			if (distinctFieldNames) {
				Set<String> fieldNames = new HashSet<String>();
				Collections.addAll(fieldNames, header);
				if (fieldNames.size() != header.length) {
					throw new CsvFormatException("duplicated field names: " + Arrays.toString(header), 1);
				}
			}
		} else {
			header = null;
		}
	}

	@Override
	public void close() throws IOException
	{
		input.close();
	}

	/**
	 * Get the header if present.
	 *
	 * @return The header or {@code null} if no header was expected
	 */
	public String[] getHeader()
	{
		return header;
	}

	/**
	 * Get the number of fields in each record.
	 *
	 * @return The number of fields or {@code -1} if the number is not fixed or {@code 0} if the number is not known yet
	 */
	public int getNumberOfFields()
	{
		return numFields;
	}

	/**
	 * Get the line number of the last read character for error reporting.
	 * {@code EOL} is read if {@code column==0}.
	 *
	 * @return The current line number as an integer > 0
	 */
	public int getLine()
	{
		return line;
	}

	/**
	 * Get the current column number of the last character read  for error reporting.
	 * {@code EOL} is read if {@code column==0}.
	 *
	 * @return The current line number as an integer >= 0
	 */
	public int getColumn ()
	{
		return column;
	}


	/**
	 * Read a single character from the input and update the counters {@code line} and {@code column}.
	 *
	 * @return The next character as an integer in the range of 0 to 65536 or {@link BasicReader#EOF} ({@value BasicReader#EOF})
	 * @throws IOException If and I/O error occurs
	 */
	protected int read() throws IOException
	{
		int current = input.read();
		if (current == EOL) {
			line += 1;
			column = 0;
		} else {
			column += 1;
		}
		return current;
	}

	/**
	 * Answer whether the CSV file has another record.
	 *
	 * @return {@code true} if the file has another record
	 */
	public boolean hasNextRecord()
	{
		return hasNextRecord;
	}

	/**
	 * Get the next record.
	 *
	 * @return The next record as an array of strings, possibly empty
	 * @throws CsvFormatException If a CSV syntax error occurs
	 * @throws IOException        If an I/O error occurs
	 */
	public String[] nextRecord() throws CsvFormatException, IOException
	{
		if (!hasNextRecord()) {
			throw new NoSuchElementException();
		}
		String[] nextRecord = readRecord();
		if (peek() == EOL) {
			read();
		}
		if (peek() == EOF) {
			hasNextRecord = false;
		}
		if (numFields > 0 && nextRecord.length != numFields) {
			throw new CsvFormatException("records contain different numbers of fields", line);
		} else if (numFields == 0) {
			numFields = nextRecord.length;
		}
		return nextRecord;
	}

	/**
	 * Returns an iterator over the records.
	 * Calling {@link Iterator#next} may throw an {@link IOException} or a {@link CsvFormatException} as unchecked exception.
	 *
	 * @return The iterator over the records
	 * @see #nextRecord()
	 */
	@Override
	public Iterator<String[]> iterator()
	{
		return new Iterator<String[]>()
		{
			@Override
			public boolean hasNext()
			{
				return hasNextRecord();
			}

			/**
			 * Returns the next record in the iteration.
			 *
			 * @return The next record in the iteration
			 * @throws CsvFormatException If a CSV syntax error occurs
			 * @throws IOException        If an I/O error occurs
			 */
			@Override
			public String[] next()
			{
				try {
					return nextRecord();
				} catch (Exception e) {
					throw throwUnchecked(e);
				}
			}
		};
	}

	/**
	 * Read a record from the input.
	 * 
	 * @return The record as an array of strings, possibly empty
	 * @throws CsvFormatException If a CSV syntax error occurs
	 * @throws IOException If an I/O error occurs
	 */
	protected String[] readRecord() throws CsvFormatException, IOException
	{
		List<String> record = new ArrayList<>();
		int next;
		do {
			String field = readField();
			record.add(field);
			next = peek();
			if (next == fieldSeparator) {
				read();
			}
		} while (! isEndOfRecord(next));
		return record.toArray(STRING_ARRAY);
	}

	/**
	 * Peek the next character without advancing the underlying reader.
	 *
	 * @return The next character as an integer in the range of 0 to 65536 or {@link BasicReader#EOF} ({@value BasicReader#EOF})
	 */
	protected int peek()
	{
		return input.peek();
	}

	/**
	 * Read a field from the input.
	 * 
	 * @return The field as string, possibly empty
	 * @throws CsvFormatException If a CSV syntax error occurs
	 * @throws IOException If an I/O error occurs
	 */
	protected String readField() throws CsvFormatException, IOException
	{
		String quoted = readQuotedField();
		if (quoted == null) {
			return readPlainField();
		} else {
			return quoted;
		}
	}

	/**
	 * Read a quoted field from the input and strip the quotes.
	 * 
	 * @return The field as string, possibly empty
	 * @throws CsvFormatException If a CSV syntax error occurs
	 * @throws IOException If an I/O error occurs
	 */
	protected String readQuotedField() throws CsvFormatException, IOException
	{
		if (peek() != DOUBLE_QUOTES) {
			return null;
		}
		read(); // Skip opening double quotes
		StringBuilder field = new StringBuilder();
		while (peek() != EOF) {
			int character = read();
			if (character == DOUBLE_QUOTES) {
				int next = peek();
				if (next == DOUBLE_QUOTES) {
					// 1. Escaped double quotes
					read();
				} else if (isEndOfField(next)) {
					// 2. Closing double quotes
					return field.toString();
				} else {
					// 3. Error
					throw new CsvFormatException("double quotes (\") in quoted field not escaped (\"\")", line, column);
				}
			}
			if (character == EOL) {
				field.append(lineSeparator);
			} else {
				field.append((char) character);
			}
		}
		throw new CsvFormatException("double quotes (\") missing to close quoted field", line, column);
	}

	/**
	 * Read a non-quoted field from the input.
	 * 
	 * @return The field as string, possibly empty
	 * @throws CsvFormatException If a CSV syntax error occurs
	 * @throws IOException If an I/O error occurs
	 */
	protected String readPlainField() throws CsvFormatException, IOException
	{
		StringBuilder field = new StringBuilder();
		while (! isEndOfField(peek())) {
			int character = read();
			if (character == DOUBLE_QUOTES) {
				throw new CsvFormatException("double quotes (\") found in non-quoted field", line, column);
			}
			field.append((char) character);
		}
		return field.toString();
	}

	/**
	 * Check whether a character is the end of a field.
	 * 
	 * @param character the character to check
	 * @return {@code true} if the character is the end of a field
	 */
	protected boolean isEndOfField(int character)
	{
		return character == fieldSeparator || isEndOfRecord(character);
	}

	/**
	 * Check whether a character is the end of a record.
	 * 
	 * @param character the character to check
	 * @return {@code true} if the character is the end of a record
	 */
	protected boolean isEndOfRecord(int character)
	{
		return character == EOL || character == EOF;
	}

	/**
	 * Print a record with quoted fields.
	 *
	 * @param record the record to be printed
	 * @return A string representation of the records with quoted fields.
	 */
	public static String printRecord(String[] record)
	{
		String[] quoted = new String[record.length];
		for (int i=record.length-1; i>=0; i--) {
			quoted[i] = "\"" + record[i].replaceAll("\\\"", "\\\\\"") + "\"";
		}
		return Arrays.toString(quoted);
	}


	/**
	 * Cast a CheckedException as an unchecked one.
	 *
	 * @param throwable to cast
	 * @param <T>       the type of the Throwable
	 * @return this method will never return a Throwable instance, it will just throw it.
	 * @throws T the throwable as an unchecked throwable
	 */
	@SuppressWarnings("unchecked")
	protected static <T extends Throwable> RuntimeException throwUnchecked(Throwable throwable) throws T
	{
		throw (T) throwable; // rely on vacuous cast
	}

	/**
	 * Simple test to show how to use the CSV reader.
	 */
	public static void main(String[] args) throws CsvFormatException, IOException
	{
		String emptyCsv = "";
		System.out.println("CSV with single empty line:\n---");
		System.out.println(emptyCsv);
		System.out.println("---\nRecords:");
		try (BasicReader reader = BasicReader.wrap(new StringReader(emptyCsv)).normalizeLineEndings();
				CsvReader emptyRecords = new CsvReader(reader, false, true, true, COMMA, LF)) {
			int i = 1;
			while (emptyRecords.hasNextRecord()) {
				System.out.println((i++) + ": " + printRecord(emptyRecords.nextRecord()));
			}
		}

		String mixedCsv = "h1;h2;h3\r"
				+ "plain;\"quoted\";\"quotes\"\"\";\"\"\n"
				+ ";1;2;3;4\n"
				+ "\r\n"
				+ ";;\n";
		System.out.println();
		System.out.println("CSV with mixed and quoted records:\n---");
		System.out.println(mixedCsv);
		System.out.println("---\nRecords:");
		try (BasicReader reader = BasicReader.wrap(new StringReader(mixedCsv)).normalizeLineEndings();
				CsvReader mixedRecords = new CsvReader(reader, true, false, true, ';', LF)) {
			System.out.println("H: " + Arrays.toString(mixedRecords.getHeader()));
			int j = 1;
			while (mixedRecords.hasNextRecord()) {
				System.out.println((j++) + ": " + printRecord(mixedRecords.nextRecord()));
			}
		}
	}
}
