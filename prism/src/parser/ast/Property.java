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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import parser.Values;
import parser.type.*;
import parser.visitor.*;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismUtils;

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
	 * @param constValues The values of any undefined constants (null or "" if none)
	 * @return Whether or not the check was performed
	 */
	public boolean checkAgainstExpectedResult(Object result, String constValues) throws PrismException
	{
		HashMap<String,String> strExpectedMap = new HashMap<String, String>();
		String strExpected = null;
		
		if (constValues == null)
			constValues = "";
		
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
		
		return checkAgainstExpectedResultString(strExpected, result);
	}
	
	/**
	 * Tests a result (specified as an object of the appropriate type: Boolean, Double, etc.)
	 * against the expected result, given by a string extracted from a RESULT: specification.
	 * (As required for {@link #checkAgainstExpectedResult(Object)} and {@link #checkAgainstExpectedResult(Object, String)}) 
	 * @param strExpected Expected result
	 * @param result The actual result
	 */
	private boolean checkAgainstExpectedResultString(String strExpected, Object result) throws PrismException
	{
		// Check for special "don't case" case
		if (strExpected.equals("?")) {
			return false;
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
				throw new PrismException("Wrong result (expected " + boolExp + ")");
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
				throw new PrismException("Wrong result (expected " + intExp + ")");
		}
		
		// Double-valued properties
		else if (type instanceof TypeDouble) {
			// Parse expected result
			double doubleExp;
			try {
				// See if it's a fraction
				if (strExpected.matches("[0-9]+/[0-9]+")) {
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
			double doubleRes;
			if (!(result instanceof Double))
				throw new PrismException("Result is wrong type for (double-valued) property");
			doubleRes = ((Double) result).doubleValue();
			if (!PrismUtils.doublesAreCloseRel(doubleRes, doubleExp, 1e-5))
				throw new PrismException("Wrong result (expected " + doubleExp + ")");
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
		prop.setPosition(this);
		return prop;
	}
}

//------------------------------------------------------------------------------
