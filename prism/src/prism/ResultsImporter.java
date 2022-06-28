//==============================================================================
//	
//	Copyright (c) 2021-
//	Authors:
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PrimitiveIterator.OfInt;
import java.util.Set;
import java.util.Vector;

import common.IterableBitSet;
import csv.BasicReader;
import csv.CsvFormatException;
import csv.CsvReader;
import param.BigRational;
import parser.ParseException;
import parser.PrismParser;
import parser.Values;
import parser.ast.Expression;
import parser.ast.ExpressionConstant;
import parser.ast.Property;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import parser.type.TypeInt;
import prism.DefinedConstant.DefinedDomain;
import prism.ResultsImporter.RawResultsCollection;

import static csv.BasicReader.CR;
import static csv.BasicReader.LF;

/**
 * A class to import Prism results collections from a data frame in CSV format.
 * <ul>
 * <li>The header either contains a field "Property" or the first column is considered to contain the property name
 * <li>The header either contains a field "Result" or the last column is considered to contain the results
 * <li>The other fields are names of ranging constants and must Prism identifiers
 * <li>A result collection is formed by all records with the same property field value
 * <li>The fields of the property column must be Prism Identifiers
 * <li>The result column may contain either {@link Boolean} values or numeric values, i.e., {@link Integer}, {@link Double} or {@link BigRational}
 * <li>The columns for ranging constants may contain numeric values only
 * </ul>
 */
public class ResultsImporter implements Iterable<Pair<Property, RawResultsCollection>>
{
	private static final int COMMA = CsvReader.COMMA;

	final Header header;
	final Map<String, RawResultsCollection> rawResults;

	/**
	 * Import the results from a reader on a source for comma-separated values.
	 * Use a {@link BufferedReader} for performant access to external sources such as files.
	 * 
	 * @param input the reader on CSV input
	 * @throws IOException If an I/O error occurs
	 * @throws PrismLangException If the content of the CSV input does not conform to the supported data frame format
	 * @throws CsvFormatException If the CSV input has a syntax error
	 */
	public ResultsImporter(Reader input) throws PrismLangException, IOException, CsvFormatException
	{
		// Convert all line endings to LF for compatibility
		try (BasicReader reader = BasicReader.wrap(input).normalizeLineEndings();
				CsvReader csv = new CsvReader(reader, COMMA, LF)) {
			header = new Header(csv.getHeader());
			rawResults = new LinkedHashMap<>();
			while (csv.hasNextRecord()) {
				// 1. Parse record
				String[] record = csv.nextRecord();
				RawResult result = new RawResult(record, header, csv.getLine());
				String propertyName = result.propertyName;
				// 2. Add to raw results
				if (rawResults.containsKey(result.propertyName)) {
					rawResults.get(result.propertyName).add(result);
				} else {
					rawResults.put(propertyName, new RawResultsCollection(header, result));
				}
			}
		}
	}

	/**
	 * Create an iterator over properties and their associated intermediate results collections.
	 * 
	 *@return The iterator over pairs {@code (property, results)}
	 */
	public Iterator<Pair<Property, RawResultsCollection>> iterator()
	{
		return new Iterator<Pair<Property, RawResultsCollection>>()
		{
			Iterator<RawResultsCollection> collections = rawResults.values().iterator();

			@Override
			public boolean hasNext()
			{
				return collections.hasNext();
			}

			@Override
			public Pair<Property, RawResultsCollection> next()
			{
				RawResultsCollection collection = collections.next();
				String propertyName = collection.getPropertyName();
				Expression dummy;
				switch (collection.getResultType()) {
				case Boolean:
					dummy = new ExpressionConstant(propertyName, TypeBool.getInstance());
					break;
				case Integer:
					dummy = new ExpressionConstant(propertyName, TypeInt.getInstance());
					break;
				case Double:
				case BigRational:
					dummy = new ExpressionConstant(propertyName, TypeDouble.getInstance());
					break;
				default:
					// It is a program error if this branch is taken. Types have to be checked before.
					throw new RuntimeException("Result type not supported: " + collection.getResultType());
				}
				Property property = new Property(dummy, propertyName);
				return new Pair<>(property, collection);
			}
		};
	}

	/**
	 * Parse a string as Prism identifier.
	 * 
	 * @param string the string to be parsed
	 * @return The argument if it is a Prism identifier
	 * @throws PrismLangException If the argument is not a Prism identifier
	 */
	protected static String parseIdentifier(String string) throws PrismLangException
	{
		ByteArrayInputStream stream = new ByteArrayInputStream(string.getBytes());
		try {
			// obtain exclusive access to the prism parser
			// (don't forget to release it afterwards)
			Prism.getPrismParser();
			try {
				// (Re)start parser
				PrismParser.ReInit(stream);
				// Parse
				boolean success = true;
				try {
					success = string.equals(PrismParser.Identifier());
				} catch (ParseException e) {
					success = false;
				}
				if (! success) {
					throw new PrismLangException("Expected identifier but got: " + string);
				}
			} finally {
				// release prism parser
				Prism.releasePrismParser();
			}
		} catch (InterruptedException ie) {
			throw new PrismLangException("Concurrency error in parser");
		}
		return string;
	}

	/**
	 * Parse a string as Java data type {@link Boolean}, {@link Integer}, {@link Double} or {@link BigRational}.
	 * The special values {@code NaN}, {@code +Infinity} and {@code -Infinity} are parsed as Double.
	 * 
	 * @param string the string to be parsed
	 * @return A value corresponding to the argument or the argument itself if it represents no supported type
	 */
	protected static Object parseValue(String string)
	{
		// Boolean
		if ("true".equals(string)) {
			return true;
		}
		if ("false".equals(string)) {
			return false;
		}
		// Integer
		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {}
		// Double
		try {
			return Double.parseDouble(string);
		} catch (NumberFormatException e) {}
		// BigRational
		try {
			return new BigRational(string);
		} catch (NumberFormatException e) {}
		return string;
	}



	/**
	 * Enumeration to support type inference.
	 */
	public enum TypeInfo
	{
		Nil {
			@Override
			public Object convert(Object value)
			{
				if (value == null) {
					return null;
				}
				throw new IllegalArgumentException("Nil type");
			}
	
			@Override
			public TypeInfo infer(Object value)
			{
				if (value == null) {
					return this;
				}
				throw new IllegalArgumentException("Expected null but got: " + value.getClass());
			}
		},
		String {
			@Override
			public String convert(Object value)
			{
				if (value instanceof String) {
					return (String) value;
				}
				throw new IllegalArgumentException("Expected String but got: " + value.getClass());
			}
	
			@Override
			public TypeInfo infer(Object value)
			{
				if (value instanceof String) {
					return this;
				}
				throw new IllegalArgumentException("Expected String but got: " + value.getClass());
			}
		},
		Boolean {
			@Override
			public Boolean convert(Object value)
			{
				if (value instanceof Boolean) {
					return (Boolean) value;
				}
				throw new IllegalArgumentException("Expected Boolean but got: " + value.getClass());
			}
	
			@Override
			public TypeInfo infer(Object value)
			{
				if (value instanceof Boolean) {
					return this;
				}
				throw new IllegalArgumentException("Expected Boolean but got: " + value.getClass());
			}
		},
		Integer {
			@Override
			public Integer convert(Object value)
			{
				if (value instanceof Integer) {
					return (Integer) value;
				}
				throw new IllegalArgumentException("Expected Integer but got: " + value.getClass());
			}
	
			@Override
			public TypeInfo infer(Object value)
			{
				if (value instanceof Integer) {
					return Integer;
				}
				if (value instanceof Double) {
					return Double;
				}
				if (value instanceof BigRational) {
					return BigRational;
				}
				throw new IllegalArgumentException("Expected Integer, Double or BigRational but got: " + value.getClass());
			}
		},
		Double {
			@Override
			public Double convert(Object value)
			{
				if (value instanceof Double) {
					return (Double) value;
				}
				if (value instanceof Integer) {
					return ((Integer) value).doubleValue();
				}
				throw new IllegalArgumentException("Expected Integer but got: " + value.getClass());
			}
	
			@Override
			public TypeInfo infer(Object value)
			{
				if (value instanceof Double || value instanceof Integer) {
					return Double;
				}
				if (value instanceof BigRational) {
					return BigRational;
				}
				throw new IllegalArgumentException("Expected Double or BigRational but got: " + value.getClass());
			}
		},
		BigRational {
			@Override
			public BigRational convert(Object value)
			{
				if (value instanceof BigRational) {
					return (BigRational) value;
				}
				if (value instanceof Double) {
					return param.BigRational.from(value);
				}
				if (value instanceof Integer) {
					return new BigRational((Integer) value);
				}
				throw new IllegalArgumentException("Expected Integer but got: " + value.getClass());
			}
	
			@Override
			public TypeInfo infer(Object value)
			{
				if (value instanceof BigRational || value instanceof Double || value instanceof Integer) {
					return BigRational;
				}
				throw new IllegalArgumentException("Expected BigRational but got: " + value.getClass());
			}
		};
	
		/**
		 * Convert an object into the receivers type.
		 * 
		 * @param value the object to convert
		 * @return the converted object
		 * @throws IllegalArgumentException If the object's type is incompatible
		 */
		public abstract Object convert(Object value);
	
		/**
		 * Infer a super type of receiver that is compatible with an object.
		 * 
		 * @param value the object to infer the common type from
		 * @return The common type
		 * @throws IllegalArgumentException If the object's type is incompatible
		 */
		public abstract TypeInfo infer(Object value);
	
		/**
		 * Get the type info from an object
		 * 
		 * @param value the object
		 * @return the type info of the object
		 * @throws IllegalArgumentException If the object's type is not supported
		 */
		public static TypeInfo from(Object value)
		{
			if (value == null) {
				return Nil;
			}
			if (value instanceof String) {
				return String;
			}
			if (value instanceof Boolean) {
				return Boolean;
			}
			if (value instanceof Integer) {
				return Integer;
			}
			if (value instanceof Double) {
				return Double;
			}
			if (value instanceof BigRational) {
				return BigRational;
			}
			throw new IllegalArgumentException("Unsupported type " + value.getClass());
		}
	}



	/**
	 * The header of a data frame containing result collections.
	 */
	public static class Header
	{
		final int numColumns;
		final String resultName;
		final String[] constantNames;
		int propertyIndex = -1;
		int resultIndex = -1;

		/**
		 * Create a header from an array of at least two header fields.
		 * 
		 * @param fields the header fields containing the column names
		 * @throws PrismLangException If the property or the result is not found or if the constant names are not Prism identifiers
		 */
		public Header(String[] fields) throws PrismLangException
		{
			numColumns = fields.length;
			setPropertyAndResultIndex(fields);
			resultName = fields[resultIndex];
			constantNames = parseConstantNames(fields);
		}

		/**
		 * Find and set the index of the property and the result column.
		 * If no field is named "Property", assume the first column is the property column.
		 * If no field is named "Result", assume the last column is the result column.
		 * If the column indices conflict, throw an exception.
		 * 
		 * @param fields the header fields containing the column names
		 * @throws PrismLangException If the property or the result column is not found
		 */
		protected void setPropertyAndResultIndex(String[] fields) throws PrismLangException
		{
			// Try to find Property and Result field
			for (int i = 0, length = fields.length; i < length; i++) {
				switch (fields[i]) {
				case "Property":
					propertyIndex = i;
					break;
				case "Result":
					resultIndex = i;
					break;
				default:
					// Skip field
				}
			}
			if (propertyIndex < 0) {
				// If no field "Property" was found, assume the first column contains the properties
				if (resultIndex == 0) {
					throw new PrismLangException("Cannot find the property column (either \"Property\" or the first column)");
				}
				propertyIndex = 0;
			}
			if (resultIndex < 0) {
				// If no field "Result" was found, assume the last column contains the results
				if (propertyIndex == fields.length - 1) {
					throw new PrismLangException("Cannot find the result column (either \"Result\" or the last column)");
				}
				resultIndex = fields.length - 1;
			}
		}

		/**
		 * Parse the names of the constant columns as Prism identifiers.
		 * 
		 * @param fields the header fields containing the column names
		 * @return An array of constant names
		 * @throws PrismLangException If a constant name is not a Prism identifier
		 */
		protected String[] parseConstantNames(String[] fields) throws PrismLangException
		{
			String[] constants = new String[fields.length - 2];
			for (int i = 0, c = 0, length = fields.length; i < length; i++) {
				if (i != propertyIndex && i != resultIndex)
					try {
						constants[c++] = parseIdentifier(fields[i]);
					} catch (PrismLangException e) {
						throw new PrismLangException("Header fields for constants must be identifiers but got: " + fields[i] + "(field " + i +")");
					}
			}
			return constants;
		}
	}



	/**
	 * A result obtained from a record of a data frame containing result collections.
	 */
	public static class RawResult
	{
		final String propertyName;
		final Object resultValue;
		final Object[] constantValues;

		/**
		 * Create a result from a record and a the header of the data frame.
		 * 
		 * @param record the record
		 * @param header the header of the data frame
		 * @param line the line number of the record for error messages
		 * @throws PrismLangException If the record has too few fields, if the property name is not a Prism identifier or if the fields are neither Boolean nor numeric
		 */
		public RawResult(String[] record, Header header, int line) throws PrismLangException
		{
			checkNumberOfFields(record, header);
			propertyName = parsePropertyName(record, header, line);
			constantValues = parseConstants(record, header, line);
			resultValue =  parseResult(record, header, line);
		}

		/**
		 * Check that the record has the same number of fields as the header.
		 * 
		 * @param record the record
		 * @param header the header
		 * @return The number of fields
		 * @throws PrismLangException If the record has not the same number of fields as the header
		 */
		protected int checkNumberOfFields(String[] record, Header header) throws PrismLangException
		{
			if (record.length != header.numColumns) {
				throw new PrismLangException("Record must have " + header.numColumns + " columns but got " + record.length);
			}
			return record.length;
		}

		/**
		 * Parse the property name as a Prism identifier.
		 * 
		 * @param record the record
		 * @param header the header
		 * @param line the line number of the record for error messages
		 * @return The property name
		 * @throws PrismLangException If the property name is not a Prism identifier
		 */
		protected String parsePropertyName(String[] record, Header header, int line) throws PrismLangException
		{
			try {
				return parseIdentifier(record[header.propertyIndex]);
			} catch (PrismLangException e) {
				throw new PrismLangException("Property must be an identifier (input line: " + line + ", field: " + (header.propertyIndex + 1) + ")");
			}
		}

		/**
		 * Parse the constant fields as Booleans or numeric values.
		 * 
		 * @param record the record
		 * @param header the header
		 * @param line the line number of the record for error messages
		 * @return The constant values
		 * @throws PrismLangException If the a constant field is neither Boolean nor numeric
		 */
		protected Object[] parseConstants(String[] record, Header header, int line) throws PrismLangException
		{
			Object[] constants = new Object[record.length - 2];
			for (int i = 0, c = 0, length = record.length; i < length; i++ ) {
				if (i != header.propertyIndex && i != header.resultIndex) {
					constants[c++] = parseConstant(record, line, i);
				}
			}
			return constants;
		}

		/**
		 * Parse a single constant field as Boolean or numeric value.
		 * 
		 * @param record the record
		 * @param line the line number of the record for error messages
		 * @param field the field index
		 * @return The constant value
		 * @throws PrismLangException If the field is neither Boolean nor numeric
		 */
		protected Object parseConstant(String[] record, int line, int field) throws PrismLangException
		{
			String string = record[field];
			if (string.isEmpty()) {
				return null;
			}
			Object value =  parseValue(record[field]);
			if (value instanceof Boolean || value instanceof Number) {
				return value;
			}
			throw new PrismLangException("Constant must be either Boolean or numeric (input line: " + line + ", field: " + (field + 1) + ") but got: " + value);
		}

		/**
		 * Parse the result field as Boolean or numeric value.
		 * 
		 * @param record the record
		 * @param header the header
		 * @param line the line number of the record for error messages
		 * @return The result value
		 * @throws PrismLangException If the field is neither Boolean nor numeric
		 */
		protected Object parseResult(String[] record, Header header, int line) throws PrismLangException
		{
			Object value =  parseValue(record[header.resultIndex]);
			if (value instanceof Boolean || value instanceof Number) {
				return value;
			}
			throw new PrismLangException("Result must be either Boolean or numeric (input line: " + line + ", field: " + (header.resultIndex + 1) + ") but got: " + value);
		}
	}



	/**
	 * An intermediate collection of results for which the domains of the constants are not known yet.
	 */
	public static class RawResultsCollection
	{
		final Header header;
		final String propertyName;
		final List<RawResult> results;
		BitSet typeConversions;
		TypeInfo constantTypes[];
		TypeInfo resultType;

		/**
		 * Create an intermediate collection of results.
		 * The non-empty constant fields of the first result determine the ranging constants.
		 * 
		 * @param header the header containing the constant names and the result name
		 * @param result the first result to initialize the collection
		 */
		public RawResultsCollection(Header header, RawResult result)
		{
			this.header = header;
			this.propertyName = result.propertyName;
			this.results = new ArrayList<RawResult>();
			initializeTypes(result);
			inferTypes(result);
			results.add(result);
		}

		/**
		 * Add a result to the collection.
		 * 
		 * @param result the result to be added
		 * @throws PrismLangException If the property field is not the name of the collection
		 */
		public void add(RawResult result) throws PrismLangException
		{
			if (! propertyName.equals(result.propertyName)) {
				throw new PrismLangException("Property field expected to be \"" + propertyName + "\" but got: " + result.propertyName);
			}
			inferTypes(result);
			results.add(result);
		}

		/**
		 * Define the constants and their domains and create a {@link ResultsCollection}.
		 * 
		 * @return The receiver converted to a results collection
		 * @throws PrismLangException 
		 */
		public ResultsCollection toResultsCollection() throws PrismLangException
		{
			convertConstants();
			@SuppressWarnings("rawtypes")
			Vector<DefinedConstant> rangingConstants = getRangingConstants();
			ResultsCollection collection = new ResultsCollection(rangingConstants, 0, rangingConstants.size(), new Values(), header.resultName);
			for (RawResult result : results) {
				Values values = new Values();
				for (int i = 0, length = constantTypes.length; i < length; i++) {
					if (constantTypes[i] != TypeInfo.Nil) {
						values.addValue(header.constantNames[i], result.constantValues[i]);
					}
				}
				int set = collection.setResult(values, result.resultValue);
				if (set < 1) {
					throw new PrismLangException("Result overridden: " + values + " -> " + result);
				}
				if (set > 1) {
					throw new PrismLangException("Multiple results set: " + values + " -> " + result);
				}
			}
			return collection;
		}

		/**
		 * Get the name.
		 * 
		 * @return The name
		 */
		public String getPropertyName()
		{
			return propertyName;
		}

		/**
		 * Get the type of the result fields.
		 * 
		 * @return The type of the result fields
		 */
		public TypeInfo getResultType()
		{
			return resultType;
		}

		/**
		 * Initialize the type information with the first result.
		 *  
		 * @param result the initial result
		 */
		protected void initializeTypes(RawResult result)
		{
			int numConstants = result.constantValues.length;
			typeConversions = new BitSet(numConstants); 
			constantTypes = new TypeInfo[numConstants];
			for (int i = 0; i < numConstants; i++) {
				constantTypes[i] = TypeInfo.from(result.constantValues[i]);
			}
			resultType = TypeInfo.from(result.resultValue);
		}

		/**
		 * Infer the types of the constant values and the type of the result value.
		 * 
		 * @param result the raw result
		 */
		protected void inferTypes(RawResult result)
		{
			for (int i = 0, length = constantTypes.length; i < length; i++) {
				Object value = result.constantValues[i];
				TypeInfo type = TypeInfo.from(value);
				TypeInfo current = constantTypes[i];
				TypeInfo inferred = current.infer(value);
				constantTypes[i] = inferred;
				if (type != current || inferred != current) {
					typeConversions.set(i);
				}
			}
			resultType = resultType.infer(result.resultValue);
		}

		/**
		 * Convert the field values such that all fields of each column have the same type.
		 */
		protected void convertConstants()
		{
			if (typeConversions.isEmpty()) {
				return;
			}
			for (RawResult result : results) {
				for (OfInt bits = IterableBitSet.getSetBits(typeConversions).iterator(); bits.hasNext();) {
					int i = bits.nextInt();
					result.constantValues[i] = constantTypes[i].convert(result.constantValues[i]);
				}
			}
		}

		/**
		 * Define a constant for each value column with its entries as domain.
		 * 
		 * @return A vector containing the defined constants
		 * @throws PrismLangException If a type is not supported for constants
		 */
		@SuppressWarnings({ "rawtypes"})
		protected Vector<DefinedConstant> getRangingConstants() throws PrismLangException
		{
			Set<Object>[] domains = collectConstantDomains();
			Vector<DefinedConstant> rangingConstants = new Vector<>();
			for (int i = 0, length = domains.length; i < length; i++) {
				Set<?> domain = domains[i];
				if (! domain.isEmpty()) {
					DefinedDomain<?> constant = defineConstant(header.constantNames[i], domains[i], constantTypes[i]);
					rangingConstants.add(constant);
				}
			}
			return rangingConstants;
		}

		/**
		 * Define a Prism constant over a domain.
		 * 
		 * @param constantName the name of the constant
		 * @param domain the domain of the constant
		 * @param type the type of the constant
		 * @return A constant defined over the domain of the given type
		 * @throws PrismLangException If the type is not supported for a constant
		 */
		@SuppressWarnings("unchecked")
		protected DefinedDomain<?> defineConstant(String constantName, Set<?> domain, TypeInfo type) throws PrismLangException
		{
			DefinedDomain<?> constant;
			switch (type) {
			case Integer:
				constant = DefinedDomain.fromValues(constantName, TypeInt.getInstance(), (Set<Integer>) domain, new Integer[0]);
				break;
			case Double:
				constant = DefinedDomain.fromValues(constantName, TypeDouble.getInstance(), (Set<Double>) domain, new Double[0]);
				break;
			case BigRational:
				constant = DefinedDomain.fromValues(constantName, TypeDouble.getInstance(), (Set<BigRational>) domain, new BigRational[0]);
				break;
			default:
				throw new PrismLangException("Ranging constants must not be of type" + type);
			}
			return constant;
		}

		/**
		 * Collect for each constant the non-null values in a set
		 * 
		 * @return An array of sets of values for each constant
		 */
		protected Set<Object>[] collectConstantDomains()
		{
			@SuppressWarnings("unchecked")
			Set<Object>[] domains = new Set[constantTypes.length];
			for (int i = 0, length = domains.length; i < length; i++) {
				domains[i] = new HashSet<>();
			}
			for (RawResult result : results) {
				for (int i = 0, length = domains.length; i < length; i++) {
					Object value = result.constantValues[i];
					if (value != null) {
						domains[i].add(value);
					}
				}
			}
			return domains;
		}
	}
}
