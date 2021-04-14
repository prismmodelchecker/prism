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

import java.io.PrintWriter;
import java.util.List;

import parser.Values;
import parser.ast.Property;

/**
 * Class to export the results of model checking in various formats.
 */
public class ResultsExporter
{
	public enum ResultsExportFormat
	{
		LIST_PLAIN ("plain text", false),
		LIST_CSV("CSV", false),
		MATRIX_PLAIN("plain text", true),
		MATRIX_CSV("CSV", true),
		COMMENT("comment", false);

		private final String fullName;
		private final boolean isMatrix;

		ResultsExportFormat(String fullName, boolean isMatrix)
		{
			this.fullName = fullName;
			this.isMatrix = isMatrix;
		}

		public  boolean isMatrix() {
			return isMatrix;
		}

		public String fullName()
		{
			return fullName;
		}
	}

	// Possible destinations for export
	public enum ResultsExportDestination
	{
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
	};

	private List<DefinedConstant> rangingConstants;
	private Values nonRangingConstantValues;
	private Property property;
	private ResultsExportFormat format;
	private ResultsExportDestination destination = ResultsExportDestination.STRING;

	private boolean printHeader;
	private boolean printNames;
	private String separator;
	private String equals;

	private String exportString = "";

	/**
	 * Print multiple results to a text-output stream.
	 * Results and properties are associated by their list indices.
	 *  
	 * @param properties list of properties associated with the results
	 * @param results list of results to print
	 * @param out target text-output stream
	 * @param format export format
	 * @param shape export type
	 */
	public static void printResults(List<ResultsCollection> results, List<Property> properties, PrintWriter out, ResultsExportFormat format)
	{
		ResultsExporter exporter = new ResultsExporter(format);
		int n = results.size();
		for (int i = 0; i < n; i++) {
			if (i > 0)
				out.println();
			if (n > 1) {
				exporter.setProperty(properties.get(i));
				if (format.isMatrix()) {
					// Print property manually as we do not use the exporter for matrix format 
					out.print(exporter.printPropertyHeader());
				} 
			}
			if (format.isMatrix()) {
				// Select separator manually as we do not use the exporter for matrix format
				out.println(results.get(i).toStringMatrix(exporter.getSeparator()));
			} else {
				out.println(results.get(i).export(exporter).getExportString());
			}
		}
		out.flush();
	}

	// Methods to create and set up a ResultsExporter  
	
	public ResultsExporter()
	{
		this(ResultsExportFormat.LIST_PLAIN);
	}

	public ResultsExporter(ResultsExportFormat format)
	{
		this(format, ResultsExportDestination.STRING);
	}

	public ResultsExporter(ResultsExportFormat format, ResultsExportDestination destination)
	{
		setFormat(format);
		setDestination(destination);
	}

	public ResultsExportFormat getFormat()
	{
		return format;
	}

	public void setFormat(ResultsExportFormat format)
	{
		this.format = format;
		switch (format) {
		case LIST_PLAIN:
		case MATRIX_PLAIN:
			printHeader = true;
			printNames = false;
			separator = "\t";
			equals = "\t";
			break;
		case LIST_CSV:
		case MATRIX_CSV:
			printHeader = true;
			printNames = false;
			separator = ", ";
			equals = ", ";
			break;
		default:
			break;
		}
	}

	public void setDestination(ResultsExportDestination destination)
	{
		this.destination = destination;
	}

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

	public String getSeparator()
	{
		return separator;
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
		if (format.isMatrix()) {
			throw new IllegalArgumentException("Unsupported export format " + format);
		};

		// Reset output string
		exportString = "";
		// Prepend property, if present
		exportString += printPropertyHeader();
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

	public String printPropertyHeader()
	{
		if (property == null) {
			return "";
		}
		switch (format) {
		case LIST_PLAIN:
		case MATRIX_PLAIN:
			return property.toString() + ":\n";
		case LIST_CSV:
		case MATRIX_CSV:
			// Quote property string as it may contain commas (,) and escape double quotes (").
			return "\"" + property.toString().replaceAll("\"", "\"\"") + "\"\n";
		case COMMENT:
			// None - it's printed at the the end in this case
		default:
			return "";
		}
	}

	/**
	 * Export a single result.
	 */
	public void exportResult(final Values values, final Object result)
	{
		switch (format) {
		case LIST_PLAIN:
		case LIST_CSV:
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
		default:
			throw new IllegalArgumentException("Unsupported export format " + format);
		}
	}

	/**
	 * Finish the export process.
	 */
	public void end()
	{
		assert !format.isMatrix();

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
