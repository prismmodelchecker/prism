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

/**
 * Class to signal syntax errors in CSV files. 
 */
public class CsvFormatException extends Exception
{
	private static final long serialVersionUID = -445612337957123597L;

	private final int line;
	private final int column;

	/**
	 * Create an exception with a line number.
	 * 
	 * @param message the description of the error
	 * @param line the line number of the error
	 */
	public CsvFormatException(String message, int line)
	{
		this(message, line, 0);
	}

	/**
	 * Create an exception with a line number and a column number.
	 * 
	 * @param message the description of the error
	 * @param line the line number of the error
	 * @param column the column number of the error
	 */
	public CsvFormatException(String message, int line, int column)
	{
		super(message + inputPosition(line, column));
		this.line = line;
		this.column = column;
	}

	/**
	 * Get the line number of the error.
	 * 
	 * @return The line number of the error or {@code null}
	 */
	public int getLine()
	{
		return line;
	}

	/**
	 * Get the line number of the error.
	 * 
	 * @return The column number of the error or {@code null}
	 */
	public int getColumn()
	{
		return column;
	}

	/**
	 * Print a string describing the position of the error.
	 * 
	 * @param line the line number of the error
	 * @param column the column number of the error
	 * @return The position the error if {@code line > 0}.
	 */
	protected static String inputPosition(int line, int column)
	{
		String position = "";
		if (line > 0) {
			position += " (input line: " + line;
			if (column > 0) {
				position += ", column: " + column + ")";
			}
			position += ")";
		}
		return position;
	}
}
