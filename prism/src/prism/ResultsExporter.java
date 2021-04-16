//==============================================================================
//	
//	Copyright (c) 2015-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Steffen Märcker <steffen.maercker@tu-dresden.de> (TU Dresden)
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
public abstract class ResultsExporter
{
	public enum ResultsExportFormat
	{
		LIST_PLAIN ("plain text", false) {
			public ResultsExporter getExporter() {
				return new ResultsExporterList(ExportStyle.PLAIN);
			}
		},
		LIST_CSV("CSV", false) {
			public ResultsExporter getExporter() {
				return new ResultsExporterList(ExportStyle.CSV);
			}
		},
		MATRIX_PLAIN("plain text", true) {
			public ResultsExporter getExporter() {
				return new ResultsExporterMatrix(ExportStyle.PLAIN);
			}
		},
		MATRIX_CSV("CSV", true) {
			public ResultsExporter getExporter() {
				return new ResultsExporterMatrix(ExportStyle.CSV);
			}
		},
		COMMENT("comment", false) {
			public ResultsExporter getExporter() {
				return new ResultsExporterComment();
			}
		};

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

		public abstract ResultsExporter getExporter();
	}

	public enum ExportStyle {
		PLAIN("\t")
		{
			public String quote(String s)
			{
				// Nothing to quote
				return s;
			}

			public String printHeader(String s)
			{
				return s + ":";
			}
		},
		CSV(", ")
		{
			public String quote(String s)
			{
				// Quote and escape double quotes (").
				return "\"" + s.replaceAll("\"", "\"\"") + "\"";
			}

			public String printHeader(String s)
			{
				return quote(s);
			}
		};
	
		public final String separator;
	
		ExportStyle(String separator)
		{
			this.separator = separator;
		}
	
		public abstract String quote(String s);

		public abstract String printHeader(String s);
	}

	protected List<DefinedConstant> rangingConstants;
	protected Values nonRangingConstantValues;
	protected String exportString = "";
	protected Property property;
	boolean printProperty;

	// Methods to create and set up a ResultsExporter  

	public void setRangingConstants(final List<DefinedConstant> rangingConstants)
	{
		this.rangingConstants = rangingConstants;
	}

	public void setNonRangingConstantValues(final Values nonRangingConstantValues)
	{
		this.nonRangingConstantValues = nonRangingConstantValues;
	}

	/**
	 * Get the exported results as a string (if the destination was specified to be a string).
	 */
	public String getExportString()
	{
		return exportString;
	}

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
	public void printResults(List<ResultsCollection> results, List<Property> properties, PrintWriter out)
	{
		int n = results.size();
		// print property before results
		printProperty = n > 1;
		for (int i = 0; i < n; i++) {
			property = properties.get(i);
			if (i > 0) {
				printCollectionSeparator(out);
			}
			exportResultsCollection(results.get(i));
			out.println(exportString);
		}
		out.flush();
	}

	protected void printCollectionSeparator(PrintWriter out)
	{
		// print separator
		out.println();
	}

	// Main interface for the actual export:

	public void exportResultsCollection(ResultsCollection collection)
	{
		collection.export(this);
	}

	// methods to be called by the class that has the results

	/**
	 * Start the export process.
	 */
	public void start()
	{
		// Reset output string
		exportString = "";
		// Prepend property, if requested
		if (printProperty) {
			exportString += printPropertyHeader();
		}
	}

	public abstract String printPropertyHeader();

	/**
	 * Export a single result.
	 */
	public abstract void exportResult(final Values values, final Object result);

	/**
	 * Finish the export process.
	 */
	public void end()
	{
		// strip off last \n before returning 
		if (exportString.charAt(exportString.length() - 1) == '\n') {
			exportString = exportString.substring(0, exportString.length() - 1);
		}
	}



	public static class ResultsExporterList extends ResultsExporter
	{
		ExportStyle style;

		public ResultsExporterList(ExportStyle style)
		{
			this.style = style;
		}

		@Override
		public String printPropertyHeader()
		{
			return property == null ? "" : style.printHeader(property.toString()) + "\n";
		}

		@Override
		public void start()
		{
			super.start();
			// Print table header, if needed
			if (rangingConstants != null) {
				String namesString = "";
				for (int i = 0; i < rangingConstants.size(); i++) {
					if (i > 0) {
						namesString += style.separator;
					}
					namesString += rangingConstants.get(i).getName();
				}
				exportString += namesString + (namesString.length() > 0 ? style.separator : "") + "Result\n";
			}
		}

		@Override
		public void exportResult(final Values values, final Object result)
		{
			String valuesString = values.toString(false, style.separator);
			exportString += valuesString + (valuesString.length() > 0 ? style.separator : "") + result + "\n";
		}
	}

	public static class ResultsExporterMatrix extends ResultsExporter
	{
		ExportStyle style;

		public ResultsExporterMatrix(ExportStyle style)
		{
			this.style = style;
		}

		@Override
		public void exportResultsCollection(ResultsCollection collection)
		{
			start();
			exportString += collection.toStringMatrix(style.separator);
			end();
		}

		@Override
		public String printPropertyHeader()
		{
			return property == null ? "" : style.printHeader(property.toString()) + "\n";
		}

		@Override
		public void exportResult(final Values values, final Object result)
		{
			// dummy method, we rely on legacy code for matrix export
		}
	}

	public static class ResultsExporterComment extends ResultsExporter
	{
		@Override
		public String printPropertyHeader()
		{
			// None - it's printed at the the end for comment format
			return "";
		}

		@Override
		public void exportResult(final Values values, final Object result)
		{
			Values mergedValues = new Values(nonRangingConstantValues, values);
			exportString += "// RESULT";
			if (mergedValues.getNumValues() > 0) {
				exportString += " (" + mergedValues.toString(true, ",") + ")";
			}
			exportString += ": " + result + "\n";
		}

		@Override
		public void end()
		{
			// For "comment" format, print the property at the end, if requested
			if (printProperty) {
				exportString +=  property.toString() + "\n";
			}
			super.end();
		}
	}
}
