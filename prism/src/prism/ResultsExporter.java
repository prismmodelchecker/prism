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
		LIST_PLAIN ("list (plain text)", false) {
			public ResultsExporter getExporter() {
				return new ResultsExporterList(ExportStyle.PLAIN);
			}
		},
		LIST_CSV("list (CSV)", false) {
			public ResultsExporter getExporter() {
				return new ResultsExporterList(ExportStyle.CSV);
			}
		},
		MATRIX_PLAIN("matrix (plain text)", true) {
			public ResultsExporter getExporter() {
				return new ResultsExporterMatrix(ExportStyle.PLAIN);
			}
		},
		MATRIX_CSV("matrix (CSV)", true) {
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
	protected PrintWriter target;
	protected Property property;
	protected boolean printProperty;

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
	 * Print multiple results to a text-output stream.
	 * Results and properties are associated by their list indices.
	 *  
	 * @param results list of results to print
	 * @param properties list of properties associated with the results
	 * @param out target text-output stream
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
			exportResultsCollection(results.get(i), out);
		}
		out.flush();
	}

	/**
	 * Print a separator between subsequent results collections
	 * @param out
	 */
	protected void printCollectionSeparator(PrintWriter out)
	{
		// print separator
		out.println();
	}

	protected abstract void printPropertyHeading();

	// Main interface for the actual export:

	public void exportResultsCollection(ResultsCollection collection, PrintWriter out)
	{
		target = out;
		collection.export(this);
		target = null;
	}

	// methods to be called by the class that has the results

	/**
	 * Start the export process.
	 */
	public void start()
	{
		// Prepend property, if requested
		if (printProperty) {
			printPropertyHeading();
		}
	}

	/**
	 * Export a single result.
	 */
	public abstract void exportResult(final Values values, final Object result);

	/**
	 * Finish the export process.
	 */
	public void end()
	{
		// None
	}



	public static class ResultsExporterList extends ResultsExporter
	{
		ExportStyle style;

		public ResultsExporterList(ExportStyle style)
		{
			this.style = style;
		}

		@Override
		protected void printPropertyHeading()
		{
			if (property != null) {
				target.println(style.printHeader(property.toString()));
			}
		}

		@Override
		public void start()
		{
			super.start();
			// Print table header, if needed
			if (rangingConstants != null) {
				for (int i = 0; i < rangingConstants.size(); i++) {
					if (i > 0) {
						target.print(style.separator);
					}
					target.print(rangingConstants.get(i).getName());
				}
				if (rangingConstants.size() > 0) {
					target.print(style.separator);
				}
				target.println("Result");
			}
		}

		@Override
		public void exportResult(final Values values, final Object result)
		{
			target.print(values.toString(false, style.separator));
			if (values.getNumValues() > 0) {
				target.print(style.separator);
			}
			target.println(Values.valToString(result));
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
		protected void printPropertyHeading()
		{
			if (property != null) {
				target.println(style.printHeader(property.toString()));
			}
			
		}

		@Override
		public void exportResultsCollection(ResultsCollection collection, PrintWriter out)
		{
			this.target = out;
			start();
			target.println(collection.toStringMatrix(style.separator));
			end();
		}

		@Override
		public void exportResult(final Values values, final Object result)
		{
			// Dummy method, we rely on legacy code for matrix export
		}
	}

	public static class ResultsExporterComment extends ResultsExporter
	{
		@Override
		protected void printPropertyHeading()
		{
			// None - property is printed at the the end for comment format
		}

		@Override
		public void exportResult(final Values values, final Object result)
		{
			Values mergedValues = new Values(nonRangingConstantValues, values);
			target.print("// RESULT");
			if (mergedValues.getNumValues() > 0) {
				target.print(" (");
				target.print(mergedValues.toString(true, ","));
				target.print(")");
			}
			target.print(": ");
			target.println( Values.valToString(result));
		}

		@Override
		public void end()
		{
			// For "comment" format, print the property at the end, if requested
			if (printProperty) {
				target.println(property.toString());
			}
			super.end();
		}
	}
}
