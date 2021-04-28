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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import csv.BasicReader.PeekableReader;

/**
 * A class that implements a simple CSV reader according to RFC 4180.
 * The field separator can be configured to another character than a comma.
 * The line ending can be configured to either {@link #CR} ({@value #CR}), {@link #LF} ({@value #LF})  or {@link #CR_LF} ({@value #CR_LF}) using the provided constants.
 * 
 * @see <a href="https://tools.ietf.org/html/rfc4180">RFC 4180</a>
 */
public class CsvReader implements AutoCloseable
{
	public static final int COMMA = ',';
	public static final int CR_LF = ReplacingReader.REPLACE;
	public static final int CR = ReplacingReader.CR;
	public static final int LF = ReplacingReader.LF;
	public static final int EOL = ReplacingReader.REPLACE;
	public static final int EOF = ReplacingReader.EOF;
	public static final char DOUBLE_QUOTES = '"';
	public static final String[] STRING_ARRAY = new String[0];

	protected final PeekableReader input;
	protected final String lineSeparator;
	protected final int fieldSeparator;
	protected final String[] header;
	protected boolean hasNextRecord = true;
	protected int numFields;
	protected int line =1;
	protected int column = 0;

	/**
	 * Create a CSV reader on an input.
	 * Assume the file starts with a header of distinct fields and all records have the same number of fields.
	 * Use a comma as field separator and CR;LF as line ending.
	 * 
	 * @param reader the underlying reader
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	public CsvReader(BasicReader reader) throws IOException, CsvFormatException
	{
		this(reader, true, true, true, COMMA, CR_LF);
	}

	/**
	 * Create a CSV reader on an input.
	 * Assume the file starts with a header of distinct fields and all records have the same number of fields.
	 * 
	 * @param reader the underlying reader
	 * @param fieldSeparator the field separator, e.g., a comma or semicolon
	 * @param lineSeparator the line ending, i.e., either {@link #CR} ({@value #CR}), {@link #LF} ({@value #LF})  or {@link #CR_LF} ({@value #CR_LF})
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	public CsvReader(BasicReader reader, int fieldSeparator, int lineSeparator) throws IOException, CsvFormatException
	{
		this(reader, true, true, true, fieldSeparator, lineSeparator);
	}

	/**
	 * Create a CSV reader on an input.
	 * Use a comma as field separator and CR;LF as line ending.
	 * 
	 * @param reader the underlying reader
	 * @param hasHeader treat the first line as header
	 * @param fixNumFields ensure all records have the same number of fields
	 * @param distinctFieldNames check that the header fields are distinct
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	public CsvReader(BasicReader reader, boolean hasHeader, boolean fixNumFields, boolean distinctFieldNames) throws IOException, CsvFormatException
	{
		this(reader,  hasHeader, fixNumFields, distinctFieldNames, COMMA, CR_LF);
	}

	/**
	 * Create a CSV reader on an input.
	 * 
	 * @param reader the underlying reader
	 * @param hasHeader treat the first line as header
	 * @param fixNumFields ensure all records have the same number of fields
	 * @param distinctFieldNames check that the header fields are distinct
	 * @param fieldSeparator the field separator, e.g., a comma or semicolon
	 * @param lineSeparator the line ending, i.e., either {@link #CR} ({@value #CR}), {@link #LF} ({@value #LF})  or {@link #CR_LF} ({@value #CR_LF})
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	public CsvReader(BasicReader reader, boolean hasHeader, boolean fixNumFields, boolean distinctFieldNames, int fieldSeparator, int lineSeparator) throws IOException, CsvFormatException
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
			if (! hasNextRecord()) {
				throw new CsvFormatException("no record found except for header", line);
			}
			if (distinctFieldNames) {
				Set<String>fieldNames = new HashSet<String>();
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
	 * @return The header of {@code null} if no header was expected
	 */
	public String[] getHeader()
	{
		return header;
	}

	/**
	 * Get the number of fields if fixed.
	 * 
	 * @return The number of fields or -1 if it is not fixed
	 */
	public int getNumberOfFields()
	{
		return numFields;
	}

	/**
	 * Get the current line number.
	 * 
	 * @return The current line number as an integer > 0
	 */
	public int getLine()
	{
		return line;
	}

	/**
	 * Read a single character from the input and update the counters {@code line} and {@code column}.
	 * 
	 * @return returns The next character as an integer in the range of 0 to 65536 or {@link #EOF} ({@value #EOF})
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
	 * Peek the next character without advancing the underlying reader.
	 * 
	 * @return returns The next character as an integer in the range of 0 to 65536 or {@link #EOF} ({@value #EOF})
	 */
	public int peek()
	{
		return input.peek();
	}

	/**
	 * Return whether the CSV file has another record.
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
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	public String[] nextRecord() throws IOException, CsvFormatException
	{
		if (! hasNextRecord()) {
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
	 * Read a record from the input.
	 * 
	 * @return The record as an array of strings, possibly empty
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	protected String[] readRecord() throws IOException, CsvFormatException
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
	 * Read a field from the input.
	 * 
	 * @return The field as string, possibly empty
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	protected String readField() throws IOException, CsvFormatException
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
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	protected String readQuotedField() throws IOException, CsvFormatException
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
	 * @throws IOException If an I/O error occurs
	 * @throws CsvFormatException If a CSV syntax error occurs
	 */
	protected String readPlainField() throws IOException, CsvFormatException
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
	 * Simple test to show how to use the CSV reader.
	 */
	public static void main(String[] args) throws IOException, CsvFormatException
	{
		String emptyCsv = "";
		System.out.println("CSV with single empty record:\n---");
		System.out.println(emptyCsv);
		System.out.println("---\nRecords:");
		try (BasicReader reader = new BasicReader.Wrapper(new StringReader(emptyCsv));
				BasicReader normalized = reader.convert(CR, LF).convert(CR).to(LF);
				CsvReader emptyRecords = new CsvReader(normalized, false, true, true, COMMA, LF)) {
			int i = 1;
			while (emptyRecords.hasNextRecord()) {
				System.out.println((i++) + ": " + Arrays.toString(emptyRecords.nextRecord()));
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
		try (BasicReader reader = new BasicReader.Wrapper(new StringReader(mixedCsv));
				BasicReader normalized = reader.convert(CR, LF).convert(CR).to(LF);
				CsvReader mixedRecords = new CsvReader(normalized, true, false, true, ';', LF)) {
			System.out.println("H: " + Arrays.toString(mixedRecords.getHeader()));
			int j = 1;
			while (mixedRecords.hasNextRecord()) {
				System.out.println((j++) + ": " + Arrays.toString(mixedRecords.nextRecord()));
			}
		}
	}
}
