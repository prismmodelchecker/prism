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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import parser.Values;
import parser.ast.Property;

/**
 * Class to export the results of model checking in various shapes and formats.
 */
public abstract class ResultsExporter
{
	/**
	 * Legal combinations of export shapes and formats.
	 * A shape defines how the data is arranged, e.g., as list or matrix.
	 * Most shapes can be written to an output in different formats, e.g., as plain text or CSV.
	 * Some shapes have an implicit format, e.g., comments.
	 */
	public enum ResultsExportShape
	{
		LIST_PLAIN ("list (plain text)", false, false) {
			public ResultsExporter getExporter() {
				return new ResultsExporterList(ExportFormat.PLAIN);
			}
		},
		LIST_CSV("list (CSV)", false, true) {
			public ResultsExporter getExporter() {
				return new ResultsExporterList(ExportFormat.CSV);
			}
		},
		MATRIX_PLAIN("matrix (plain text)", true, false) {
			public ResultsExporter getExporter() {
				return new ResultsExporterMatrix(ExportFormat.PLAIN);
			}
		},
		MATRIX_CSV("matrix (CSV)", true, true) {
			public ResultsExporter getExporter() {
				return new ResultsExporterMatrix(ExportFormat.CSV);
			}
		},
		DATA_FRAME("data frame (CSV)", true, true) {
			public ResultsExporter getExporter() {
				return new ResultsExporterDataFrame();
			}
		},
		COMMENT("comment", false, false) {
			public ResultsExporter getExporter() {
				return new ResultsExporterComment();
			}
		};

		public final String fullName;
		public final boolean isMatrix;
		public final boolean isCSV;

		ResultsExportShape(String fullName, boolean isMatrix, boolean isCSV)
		{
			this.fullName = fullName;
			this.isMatrix = isMatrix;
			this.isCSV = isCSV;
		}

		public abstract ResultsExporter getExporter();
	}

	/**
	 * An export format defines how data is written to an output.
	 */
	public enum ExportFormat {
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
		CSV(",")
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
	
		ExportFormat(String separator)
		{
			this.separator = separator;
		}
	
		public abstract String quote(String s);

		public abstract String printHeader(String s);
	}

	@SuppressWarnings("rawtypes")
	protected List<DefinedConstant> rangingConstants;
	protected Values nonRangingConstantValues;
	protected PrintWriter target;
	protected Property property;
	protected boolean printProperty;

	// Methods to create and set up a ResultsExporter  

	public void setRangingConstants(@SuppressWarnings("rawtypes") final List<DefinedConstant> rangingConstants)
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


	/**
	 * An exporter that arranges the results in a list.
	 */
	public static class ResultsExporterList extends ResultsExporter
	{
		ExportFormat style;

		public ResultsExporterList(ExportFormat style)
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



	/**
	 * An exporter that arranges the results in a data frame.
	 */
	public static class ResultsExporterDataFrame extends ResultsExporter
	{
		public static final String PROPERTY_COLUMN = "Property";
		public static final String RESULT_COLUMN = "Result";

		boolean multipleResultCollections = true;
		String propertyColumn = PROPERTY_COLUMN;
		String resultColumn = RESULT_COLUMN;
		Set<String>columns = new LinkedHashSet<>();
		Map<String, Integer> columnIndices = new HashMap<>();


		@Override
		public void printResults(List<ResultsCollection> results, List<Property> properties, PrintWriter out)
		{
			multipleResultCollections = true;
			initializeColumns(results);
			printHeader(out);
			super.printResults(results, disambiguatePropertyNames(properties), out);
			resetColumns();
			multipleResultCollections = false;
		}

		/**
		 * Generate columns from the defined constants of all results collections.
		 * 
		 * @param results the list of results collections to be included
		 */
		protected void initializeColumns(List<ResultsCollection> results)
		{
			resetColumns();
			Set<String> resultNames = new HashSet<>();
			for (ResultsCollection col : results) {
				@SuppressWarnings("rawtypes")
				List<DefinedConstant> constants = col.getRangingConstants();
				addConstantsToColumns(constants);
				resultNames.add(col.getResultName());
			}
			propertyColumn = disambiguateName(PROPERTY_COLUMN, columns);
			resultColumn = disambiguateName((resultNames.size() == 1) ? resultNames.iterator().next() : RESULT_COLUMN, columns);
		}

		/**
		 * If {@code names} contains {@code name}, append "_(number)" to make name unique.
		 * @param name name to be made unique
		 * @param names known names
		 * @return unique name
		 */
		protected String disambiguateName(String name, Collection<String> names)
		{
			String unique = name;
			int i = 1;
			while (names.contains(unique)) {
				unique = name + "_" + i++;
			}
			return unique;
		}

		/**
		 * Forget all generated columns.
		 */
		protected void resetColumns()
		{
			columns.clear();
			propertyColumn = PROPERTY_COLUMN;
			resultColumn = RESULT_COLUMN;
		}

		@SuppressWarnings("rawtypes")
		protected void addConstantsToColumns(List<DefinedConstant> constants)
		{
			for (DefinedConstant con : constants) {
				String name = con.getName();
				if (!columns.contains(name)) {
					columns.add(name);
				}
			}
		}

		/**
		 * Print the header:<br>
		 * Property,Constant_1,...,Constant_n,Result
		 * 
		 * @param out the writer to print the header
		 */
		protected void printHeader(PrintWriter out)
		{
			out.print("Property");
			out.print(ExportFormat.CSV.separator);
			for (String col : columns) {
				out.print(col);
				out.print(ExportFormat.CSV.separator);
			}
			out.println(resultColumn);
		}

		/**
		 * First, copy each property and name properties with no name "Property_(number)".
		 * Second, make property names unique by appending "_(number)" if necessary.
		 * 
		 * @param properties
		 * @return list of new properties with distinct names
		 */
		protected List<Property> disambiguatePropertyNames(List<Property> properties)
		{
			List<Property> namedProperties = new ArrayList<>(properties.size());
			Map<String, Integer> nameCounts = new HashMap<>();
			// 1. Ensure each property is given a name and count names
			for (int i=0, size=properties.size(); i<size; i++) {
				Property property = (Property) properties.get(i).deepCopy();
				String name = property.getName();
				if (name == null || name.isEmpty()) {
					// Create copy with new name
					name = "Property_" + (i+1);
					property.setName(name);
				}
				namedProperties.add(property);
				// Count occurrences of name
				int count = nameCounts.getOrDefault(name, 0);
				nameCounts.put(name, count + 1);
			}
			// 2. Make names unique by appending "_<number>"
			for (Property property: namedProperties) {
				String name = property.getName();
				int occurrences = nameCounts.get(name);
				if (occurrences > 1) {
					for (int i=1; i<=occurrences; i++) {
						String newName = name + "_" + i;
						if(!nameCounts.containsKey(newName)) {
							property.setName(newName);
							nameCounts.put(newName, 1);
							break;
						}
					}
				}
			}
			return namedProperties;
		}

		@Override
		protected void printCollectionSeparator(PrintWriter out)
		{
			// None - results are not separated
		}

		@Override
		protected void printPropertyHeading()
		{
			// None - property is printed as a field in each record
		}

		@Override
		public void start()
		{
			initializeColumnIndices();
			if (! multipleResultCollections) {
				// Printing a single result collection: print table header
				addConstantsToColumns(rangingConstants);
				printHeader(target);
				resetColumns();
				// Ensure property has a name
				String name = property.getName();
				if (name == null || name.isEmpty()) {
					property = (Property) property.deepCopy();
					property.setName("Property_1");
				}
			}
			super.start();
		}

		/**
		 * Create a mapping from column name to its index in list of ranging constants.
		 */
		protected void initializeColumnIndices()
		{
			columnIndices.clear();
			int size=rangingConstants.size();
			for (int i=0; i<size; i++) {
				columnIndices.put(rangingConstants.get(i).getName(), i);
			}
		}

		@Override
		public void exportResult(Values values, Object result)
		{
			// Don't quote as property names contain neither commas nor double quotes
			target.print(property.getName().toString());
			target.print(ExportFormat.CSV.separator);
			for (String col : columns) {
				if (columnIndices.containsKey(col)) {
					Object value = values.getValue(columnIndices.get(col));
					target.print(Values.valToString(value));
				} 
				target.print(ExportFormat.CSV.separator);
			}
			target.println(Values.valToString(result));
		}
	}



	/**
	 * An exporter that arranges the results in a matrix.
	 */
	public static class ResultsExporterMatrix extends ResultsExporter
	{
		ExportFormat style;

		public ResultsExporterMatrix(ExportFormat style)
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



	/**
	 * An exporter that arranges the results in a comment.
	 */
	public static class ResultsExporterComment extends ResultsExporter
	{
		@Override
		protected void printPropertyHeading()
		{
			// None - property is printed at the the end for comment shape
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
			target.println(Values.valToString(result));
		}

		@Override
		public void end()
		{
			// For "comment" shape, print the property at the end, if requested
			if (printProperty) {
				target.println(property.toString());
			}
			super.end();
		}
	}
}
