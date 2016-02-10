//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package parser.ast;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import param.BigRational;
import parser.Values;
import parser.type.*;
import parser.visitor.*;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismNotSupportedException;
import prism.PrismUtils;
import prism.Point;
import prism.TileList;

/**
 * PRISM property, i.e. a PRISM expression plus other (optional info) such as name, comment, etc.
 */
public class Property extends ASTElement
{
	/** PRISM expression representing property */
	private Expression expr;
	/** Optional name for property (null if absent); */
	private String name;
	/** Optional comment for property (null if absent); */
	private String comment;

	// Constructors

	public Property(Expression expr)
	{
		this(expr, null, null);
	}

	public Property(Expression expr, String name)
	{
		this(expr, name, null);
	}

	public Property(Expression expr, String name, String comment)
	{
		this.expr = expr;
		this.name = name;
		this.comment = comment;
	}

	// Mutators

	public void setExpression(Expression expr)
	{
		this.expr = expr;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public void setComment(String comment)
	{
		this.comment = comment;
	}

	// Accessors

	public Expression getExpression()
	{
		return expr;
	}

	public String getName()
	{
		return name;
	}

	public String getComment()
	{
		return comment;
	}

	/**
	 * Tests a result (specified as an object of the appropriate type: Boolean, Double, etc.)
	 * against the expected result for this Property, specified by an embedded "RESULT: xxx"
	 * string in the accompanying comment (immediately preceding it in the property specification).
	 * If the test fails or something else goes wrong, an explanatory PrismException is thrown.
	 * Otherwise, the method successfully exits, returning a boolean value that indicates
	 * whether or not a check was actually applied (i.e. if the result specification is of the
	 * form "RESULT: ?") then false is returned; otherwise true.   
	 * @param result The actual result
	 * @return Whether or not the check was performed
	 */
	public boolean checkAgainstExpectedResult(Object result) throws PrismException
	{
		return checkAgainstExpectedResult(result, null);
	}

	/**
	 * Tests a result (specified as an object of the appropriate type: Boolean, Double, etc.)
	 * against the expected result for this Property, specified by an embedded "RESULT: xxx"
	 * string in the accompanying comment (immediately preceding it in the property specification).
	 * Different results for different constant values are specified by e.g. "RESULT (x=1): xxx". 
	 * If the test fails or something else goes wrong, an explanatory PrismException is thrown.
	 * Otherwise, the method successfully exits, returning a boolean value that indicates
	 * whether or not a check was actually applied (i.e. if the result specification is of the
	 * form "RESULT: ?") then false is returned; otherwise true.   
	 * @param result The actual result
	 * @param constValues The values of any undefined constants (null if none)
	 * @return Whether or not the check was performed
	 */
	public boolean checkAgainstExpectedResult(Object result, Values constValues) throws PrismException
	{
		String strExpected = getExpectedResultString(constValues);
		return checkAgainstExpectedResultString(strExpected, result);
	}

	/**
	 * Get the expected result by extracting from the appropriate RESULT annotation.
	 * This is done by finding the first RESULT whose constant values (if any) all match those
	 * provided in {@code constValues}. A PrismException is thrown if no matching RESULT is found.
	 * @param constValues The values of any undefined constants (null if none)
	 */
	private String getExpectedResultString(Values constValues) throws PrismException
	{
		String strExpected = null;

		// Extract expected result(s) from comment
		if (comment != null) {
			// Look for "RESULT: val" or "RESULT (x=1,y=2): val"
			Pattern p = Pattern.compile("RESULT[ \t]*(\\(([^\\)]+)\\))?[ \t]*:[ \t]*([^ \t\n\r]+)");
			Matcher matcher = p.matcher(comment);
			// Look at each RESULT specification found
			while (matcher.find()) {
				String constValsSubstring = matcher.group(2) == null ? "" : matcher.group(2);
				boolean allMatch = true;
				// Look at each constant in the list
				String ss[] = constValsSubstring.split(",");
				for (String s : ss) {
					boolean match = true;
					s = s.trim();
					if (s.length() == 0)
						continue;
					String pair[] = s.split("=");
					if (pair.length != 2)
						throw new PrismException("Badly formed RESULT specification \"" + matcher.group() + "\"");
					// Make sure constant/value is in constValues list and matches
					String constName = pair[0].trim();
					String constVal = pair[1].trim();
					Object constValToMatch = constValues.getValueOf(constName);
					if (constValToMatch == null)
						match = false;
					// Check doubles numerically
					else if (constValToMatch instanceof Double)
						match = PrismUtils.doublesAreCloseRel(((Double) constValToMatch).doubleValue(), Double.parseDouble(constVal), 1e-10);
					// Otherwise just check for exact string match for now
					else
						match = constValToMatch.toString().equals(constVal);
					// We need all constants to match
					allMatch &= match;
				}
				// Found it...
				if (allMatch) {
					strExpected = matcher.group(3);
					continue;
				}
			}
		}
		if (strExpected == null) {
			throw new PrismException("Did not find a RESULT specification (for " + constValues + ") to test against");
		}

		return strExpected;
	}

	/**
	 * Get the expected result by extracting from the appropriate RESULT annotation.
	 * This is done by an exact string match against the provided string of constant values.
	 * A PrismException is thrown if no matching RESULT is found.
	 * This method actually looks at all RESULTs and complains if there multiple matches. 
	 * @param constValues The values of any undefined constants (null or "" if none)
	 */
	@SuppressWarnings("unused")
	private String getExpectedResultString(String constValues) throws PrismException
	{
		HashMap<String, String> strExpectedMap = new HashMap<String, String>();
		String strExpected = null;

		// Extract expected result(s) from comment
		if (comment != null) {
			// Look for "RESULT: val" or "RESULT (x=1,y=2): val"
			Pattern p = Pattern.compile("RESULT[ \t]*(\\(([^\\)]+)\\))?[ \t]*:[ \t]*([^ \t\n\r]+)");
			Matcher matcher = p.matcher(comment);
			// Store RESULT specifications found
			while (matcher.find()) {
				String constValsSubstring = matcher.group(2) == null ? "" : matcher.group(2);
				String expResultSubstring = matcher.group(3);
				// Error if there are dupes 
				if (strExpectedMap.put(constValsSubstring, expResultSubstring) != null) {
					if (constValsSubstring.length() == 0)
						throw new PrismException("Multiple RESULT specificiations for test");
					else
						throw new PrismException("Multiple RESULT (" + constValsSubstring + ") specificiations for test");
				}
			}
		}
		if (strExpectedMap.size() == 0) {
			throw new PrismException("Did not find any RESULT specifications to test against");
		}
		// Look up result for the constant values provided
		strExpected = strExpectedMap.get(constValues);
		if (strExpected == null) {
			throw new PrismException("Did not find a RESULT specification (for " + constValues + ") to test against");
		}

		return strExpected;
	}

	/**
	 * Tests a result (specified as an object of the appropriate type: Boolean, Double, etc.)
	 * against the expected result, given by a string extracted from a RESULT: specification.
	 * (As required for {@link #checkAgainstExpectedResult(Object)} and {@link #checkAgainstExpectedResult(Object, String)}) 
	 * @param strExpected Expected result
	 * @param result The actual result
	 * @return Whether or not the check was performed
	 */
	private boolean checkAgainstExpectedResultString(String strExpected, Object result) throws PrismException
	{
		// Check for special "don't case" case
		if (strExpected.equals("?")) {
			return false;
		}

		// Check for exceptions
		if (result instanceof Exception) {
			String errMsg = ((Exception) result).getMessage();
			if (strExpected.startsWith("Error")) {
				// handle expected errors
				if (strExpected.startsWith("Error:")) {
					String words[] = strExpected.substring(6).split(",");
					for (String word : words) {
						if (word.length() == 0) {
							throw new PrismException("Invalid RESULT specification: no expected words immediately following 'Error:'");
						}
						if (!errMsg.toLowerCase().contains(word)) {
							throw new PrismException("Error message should contain \"" + word + "\"");
						}
					}
				}
				return true;
			}
			if (result instanceof PrismNotSupportedException) {
				// not supported -> handle in caller
				throw (PrismNotSupportedException)result;
			}
			throw new PrismException("Unexpected error: " + errMsg);
		} else if (strExpected.startsWith("Error")) {
			throw new PrismException("Was expecting an error");
		}

		// Check expected/actual result
		Type type = expr.getType();

		// Boolean-valued properties
		if (type instanceof TypeBool) {
			// Parse expected result
			boolean boolExp;
			strExpected = strExpected.toLowerCase();
			if (strExpected.equals("true"))
				boolExp = true;
			else if (strExpected.equals("false"))
				boolExp = false;
			else
				throw new PrismException("Invalid RESULT specification \"" + strExpected + "\" for boolean-valued property");
			// Parse actual result
			boolean boolRes;
			if (!(result instanceof Boolean))
				throw new PrismException("Result is wrong type for (boolean-valued) property");
			boolRes = ((Boolean) result).booleanValue();
			if (boolRes != boolExp)
				throw new PrismException("Wrong result (expected " + boolExp + ", got " + boolRes + ")");
		}

		// Integer-valued properties
		else if (type instanceof TypeInt) {
			// Parse expected result
			int intExp;
			try {
				intExp = Integer.parseInt(strExpected);
			} catch (NumberFormatException e) {
				throw new PrismException("Invalid RESULT specification \"" + strExpected + "\" for integer-valued property");
			}
			// Parse actual result
			int intRes;
			if (!(result instanceof Integer))
				throw new PrismException("Result is wrong type for (integer-valued) property");
			intRes = ((Integer) result).intValue();
			if (intRes != intExp)
				throw new PrismException("Wrong result (expected " + intExp + ", got " +intRes + ")");
		}

		// Double-valued properties (non-exact mode)
		else if (type instanceof TypeDouble && !(result instanceof BigRational)) {
			// Parse expected result
			double doubleExp;
			try {
				// See if it's NaN
				if (strExpected.equals("NaN")) {
					doubleExp = Double.NaN;
				}
				// See if it's a fraction
				else if (strExpected.matches("[0-9]+/[0-9]+")) {
					int numer = Integer.parseInt(strExpected.substring(0, strExpected.indexOf('/')));
					int denom = Integer.parseInt(strExpected.substring(strExpected.indexOf('/') + 1));
					doubleExp = ((double) numer) / denom;
				}
				// Otherwise, just a double
				else {
					doubleExp = Double.parseDouble(strExpected);
				}
			} catch (NumberFormatException e) {
				throw new PrismException("Invalid RESULT specification \"" + strExpected + "\" for double-valued property");
			}
			// Parse actual result
			if (!(result instanceof Double))
				throw new PrismException("Result is wrong type for (double-valued) property");
			double doubleRes = ((Double) result).doubleValue();
			// Compare results
			if (Double.isNaN(doubleRes)) {
				if (!Double.isNaN(doubleExp))
					throw new PrismException("Wrong result (expected " + doubleExp + ", got NaN)");
			} else {
				if (!PrismUtils.doublesAreCloseRel(doubleExp, doubleRes, 1e-5))
					throw new PrismException("Wrong result (expected " + doubleExp + ", got " + doubleRes + ")");
			}
		}

		// Double-valued properties (exact mode)
		else if (type instanceof TypeDouble && result instanceof BigRational) {
			// Parse expected result
			BigRational rationalRes = (BigRational) result;
			BigRational rationalExp = null;
			try {
				// See if it's NaN
				if (strExpected.equals("NaN")) {
					if (!rationalRes.isNaN())
						throw new PrismException("Wrong result (expected NaN, got " + rationalRes + ")");
				}
				// For integers/rationals/doubles, parse with BigRational if it's an integer
				else {
					rationalExp = new BigRational(strExpected);
				}
			} catch (NumberFormatException e) {
				throw new PrismException("Invalid RESULT specification \"" + strExpected + "\" for rational-valued property");
			}
			// Compare results
			if (!rationalRes.equals(rationalExp))
				throw new PrismException("Wrong result (expected " + rationalExp + ", got " + rationalRes + ")");
		}
		else if (type instanceof TypeVoid && result instanceof TileList) { //Pareto curve

			//Create the list of points from the expected results
			List<Point> liExpected = new ArrayList<Point>();
			Pattern p = Pattern.compile("\\(([^,]*),([^)]*)\\)");
			Matcher m = p.matcher(strExpected);
			if (!m.find()) {
				throw new PrismException("The expected result does not contain any points, or does not have the required format.");
			}
			
			do {
				double x = Double.parseDouble(m.group(1));
				double y = Double.parseDouble(m.group(2));
				Point point = new Point(new double[] {x,y});
				liExpected.add(point);
			} while(m.find());

			List<Point> liResult = ((TileList) result).getRealPoints();

			if (liResult.size() != liExpected.size())
				throw new PrismException("The expected Pareto curve and the computed Pareto curve have a different number of points.");

			//check if we can find a matching point for every point on the expected Pareto curve
			for(Point point : liExpected) {
				boolean foundClose = false;
				for(Point point2 : liResult) {
					if (point2.isCloseTo(point)) {
						foundClose = true;
						break;
					}
				}
				if (!foundClose)
				{
					throw new PrismException("The point " + point + " in the expected Pareto curve has no match among the points in the computed Pareto curve.");
				}
			}

			//check if we can find a matching point for every point on the computed Pareto curve
			//(we did check if both lists have the same number of points, but that does
			//not rule out the possibility of two very similar points contained in one list)
			for(Point point : liResult) {
				boolean foundClose = false;
				for(Point point2 : liExpected) {
					if (point2.isCloseTo(point)) {
						foundClose = true;
						break;
					}
				}
				if (!foundClose)
				{
					throw new PrismException("The point " + point + " in the computed Pareto curve has no match among the points in the expected Pareto curve");
				}
			}
		}
		
		// Unknown type
		else {
			throw new PrismException("Don't know how to test properties of type " + type);
		}

		return true;
	}

	// Methods required for ASTElement:

	/**
	 * Visitor method.
	 */
	public Object accept(ASTVisitor v) throws PrismLangException
	{
		return v.visit(this);
	}

	@Override
	public String toString()
	{
		// Note: don't print comment
		String s = "";
		//if (comment != null)
		//s += PrismParser.slashCommentBlock(comment);
		if (name != null)
			s += "\"" + name + "\": ";
		s += expr;
		return s;
	}

	@Override
	public Property deepCopy()
	{
		Property prop = new Property(expr, name, comment);
		prop.setType(type);
		prop.setPosition(this);
		return prop;
	}
}

//------------------------------------------------------------------------------
