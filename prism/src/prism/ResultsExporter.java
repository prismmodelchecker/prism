//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

import java.util.List;

import parser.Values;
import parser.ast.Property;

/**
 * Class to export the results of model checking in various formats.
 */
public class ResultsExporter
{
	// Formats for export
	public enum ResultsExportFormat {
		PLAIN, CSV, COMMENT;
		public String fullName()
		{
			switch (this) {
			case PLAIN:
				return "plain text";
			case CSV:
				return "CSV";
			case COMMENT:
				return "comment";
			default:
				return this.toString();
			}
		}

		public static ResultsExportFormat parse(String formatName)
		{
			switch (formatName) {
			case "plain":
				return ResultsExportFormat.PLAIN;
			case "csv":
				return ResultsExportFormat.CSV;
			case "comment":
				return ResultsExportFormat.COMMENT;
			default:
				// Default to plain if unknown format
				return ResultsExportFormat.PLAIN;
			}
		}
	};

	// Possible destinations for export
	public enum ResultsExportDestination {
		STRING;
		public String fullName()
		{
			switch (this) {
			case STRING:
				return "string";
			default:
				return this.toString();
			}
		}
		public static ResultsExportDestination parse(String formatName)
		{
			switch (formatName) {
			case "string":
				return ResultsExportDestination.STRING;
			default:
				// Default to string if unknown format
				return ResultsExportDestination.STRING;
			}
		}
	};

	private List<DefinedConstant> rangingConstants;
	private Values nonRangingConstantValues;
	private Property property;
	private ResultsExportFormat format = ResultsExportFormat.PLAIN;

	private boolean printHeader;
	private boolean printNames;
	private String separator;
	private String equals;

	private ResultsExportDestination destination = ResultsExportDestination.STRING;

	private String exportString = "";

	// Methods to create and set up a ResultsExporter  
	
	public ResultsExporter()
	{
		setFormat(ResultsExportFormat.PLAIN);
		setDestination(ResultsExportDestination.STRING);
	}

	public ResultsExporter(ResultsExportFormat format, ResultsExportDestination destination)
	{
		setFormat(format);
		setDestination(destination);
	}

	public ResultsExporter(String formatName, String destinationName)
	{
		setFormatByName(formatName);
		setDestinationByName(destinationName);
	}

	public void setFormat(ResultsExportFormat format)
	{
		this.format = format;
		switch (format) {
		case PLAIN:
			printHeader = true;
			printNames = false;
			separator = "\t";
			equals = "\t";
			break;
		case CSV:
			printHeader = true;
			printNames = false;
			separator = ", ";
			equals = ", ";
			break;
		default:
			break;
		}
	}

	public void setFormatByName(String formatName)
	{
		setFormat(ResultsExportFormat.parse(formatName));
	}

	public void setDestination(ResultsExportDestination destination)
	{
		this.destination = destination;
	}
	
	public void setDestinationByName(String destinationName)
	{
		setDestination(ResultsExportDestination.parse(destinationName));
	}

	/*public void setCustomFormat(boolean printHeader, boolean printNames, String separator, String equals)
	{
		this.printHeader = printHeader;
		this.printNames = printNames;
		this.separator = separator;
		this.equals = equals;
	}*/

	public void setRangingConstants(final List<DefinedConstant> rangingConstants)
	{
		this.rangingConstants = rangingConstants;
	}

	public void setNonRangingConstantValues(final Values nonRangingConstantValues)
	{
		this.nonRangingConstantValues = nonRangingConstantValues;
	}

	public void setProperty(final Property property)
	{
		this.property = property;
	}

	/**
	 * Get the exported results as a string (if the destination was specified to be a string).
	 */
	public String getExportString()
	{
		return exportString;
	}

	// Main interface for the actual export:
	// methods to be called by the class that has the results
	
	/**
	 * Start the export process.
	 */
	public void start()
	{
		// Reset output string
		exportString = "";
		// Prepend property, if present
		if (property != null) {
			switch (format) {
			case PLAIN:
				exportString += property.toString() + ":\n";
				break;
			case CSV:
				exportString += "\"" + property.toString().replaceAll("\"", "\"\"") + "\"\n";
				break;
			case COMMENT:
				// None - it's printed at the the end in this case
				break;
			default:
				break;
			}
		}
		// Print header, if needed
		if (printHeader && rangingConstants != null) {
			String namesString = "";
			for (int i = 0; i < rangingConstants.size(); i++) {
				if (i > 0) {
					namesString += separator;
				}
				namesString += rangingConstants.get(i).getName();
			}
			exportString += namesString + (namesString.length() > 0 ? equals : "") + "Result\n";
		}
	}

	/**
	 * Export a single result.
	 */
	public void exportResult(final Values values, final Object result)
	{
		switch (format) {
		case PLAIN:
		case CSV:
			String valuesString = values.toString(printNames, separator); 
			exportString += valuesString + (valuesString.length() > 0 ? equals : "") + result + "\n";
			break;
		case COMMENT:
			Values mergedValues = new Values(nonRangingConstantValues, values);
			exportString += "// RESULT";
			if (mergedValues.getNumValues() > 0) {
				exportString += " (" + mergedValues.toString(true, ",") + ")";
			}
			exportString += ": " + result + "\n";
		}
	}
	
	/**
	 * Finish the export process.
	 */
	public void end()
	{
		// For "comment" format, print the property at the end, if present 
		if (property != null && format == ResultsExportFormat.COMMENT) {
			exportString +=  property.toString() + "\n";
		}
		// If writing to a string, strip off last \n before returning 
		if (destination == ResultsExportDestination.STRING) {
			if (exportString.charAt(exportString.length() - 1) == '\n') {
				exportString = exportString.substring(0, exportString.length() - 1);
			}
		}
	}
}
