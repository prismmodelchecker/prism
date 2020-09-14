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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import param.BigRational;
import parser.Values;
import parser.visitor.ASTVisitor;
import prism.DefinedConstant;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismUtils;
import prism.Result;
import prism.ResultTesting;

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
	 * Tests a result against the expected result for this Property, specified by an embedded "RESULT: xxx"
	 * string in the accompanying comment (immediately preceding it in the property specification).
	 * Different results for different constant values are specified by e.g. "RESULT (x=1): xxx".
	 * The result should ideally be passed in as a {@link prism.Result} object, but can also
	 * be given directly as an object of the appropriate type: Boolean, Double, etc.)
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
	 * Tests a result against the expected result for this Property, specified by an embedded "RESULT: xxx"
	 * string in the accompanying comment (immediately preceding it in the property specification).
	 * Different results for different constant values are specified by e.g. "RESULT (x=1): xxx".
	 * The result should ideally be passed in as a {@link prism.Result} object, but can also
	 * be given directly as an object of the appropriate type: Boolean, Double, etc.)
	 * If the test fails or something else goes wrong, an explanatory PrismException is thrown.
	 * Otherwise, the method successfully exits, returning a boolean value that indicates
	 * whether or not a check was actually applied (i.e. if the result specification is of the
	 * form "RESULT: ?") then false is returned; otherwise true.
	 * @param result The actual result
	 * @param constValues The values of any constants (null if none)
	 * @return Whether or not the check was performed
	 */
	public boolean checkAgainstExpectedResult(Object result, Values constValues) throws PrismException
	{
		return checkAgainstExpectedResult(result, constValues, null);
	}

	/**
	 * Tests a result against the expected result for this Property, specified by an embedded "RESULT: xxx"
	 * string in the accompanying comment (immediately preceding it in the property specification).
	 * Different results for different constant values are specified by e.g. "RESULT (x=1): xxx".
	 * The result should ideally be passed in as a {@link prism.Result} object, but can also
	 * be given directly as an object of the appropriate type: Boolean, Double, etc.)
	 * If the test fails or something else goes wrong, an explanatory PrismException is thrown.
	 * Otherwise, the method successfully exits, returning a boolean value that indicates
	 * whether or not a check was actually applied (i.e. if the result specification is of the
	 * form "RESULT: ?") then false is returned; otherwise true.
	 * @param result The actual result
	 * @param constValues The values of any constants (null if none)
	 * @param params The names of any parameters, for "symbolic" results (null if none)
	 * @return Whether or not the check was performed
	 */
	public boolean checkAgainstExpectedResult(Object result, Values constValues, List<String> params) throws PrismException
	{
		Result resultObj = (result instanceof Result) ? ((Result) result) : new Result(result);
		String strExpected = getExpectedResultString(constValues);
		return ResultTesting.checkAgainstExpectedResultString(strExpected, constValues, params, expr.getType(), resultObj);
	}

	/**
	 * Get the expected result by extracting from the appropriate RESULT annotation.
	 * This is done by finding the first RESULT whose constant values (if any) all match those
	 * provided in {@code constValues}. A PrismException is thrown if no matching RESULT is found.
	 * @param constValues The values of any constants (null if none)
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

					Object constValToMatch;
					if (constValues.getIndexOf(constName) == -1) {
						// there is no constant of that name, might be a parametric constant
						constValToMatch = null;
					} else {
						constValToMatch = constValues.getValueOf(constName);
					}
					if (constValToMatch == null)
						match = false;
					// Check doubles numerically
					else if (constValToMatch instanceof Double)
						match = PrismUtils.doublesAreEqual(((Double) constValToMatch).doubleValue(), DefinedConstant.parseDouble(constVal));
					// if constant is exact rational number, compare exactly
					else if (constValToMatch instanceof BigRational)
						match = BigRational.from(constVal).equals(constValToMatch);
					// Otherwise just check for exact string match for now
					else
						match = constValToMatch.toString().equals(constVal);

					// We need all constants to match
					allMatch &= match;
				}
				// Found it...
				if (allMatch) {
					strExpected = matcher.group(3);
					// we return the expected answer for the first RESULT that matches
					// the constants
					break;
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
	 * @param constValues The values of any constants (null or "" if none)
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
		Property prop = new Property(expr.deepCopy(), name, comment);
		prop.setType(type);
		prop.setPosition(this);
		return prop;
	}
}

//------------------------------------------------------------------------------
