//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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


package param;

import java.util.List;

import parser.Values;
import parser.ast.ConstantList;
import parser.ast.Expression;
import parser.ast.ExpressionIdent;
import parser.ast.ExpressionLiteral;
import parser.type.Type;
import parser.type.TypeBool;
import parser.type.TypeDouble;
import prism.Prism;
import prism.PrismException;
import prism.PrismLangException;
import prism.PrismNotSupportedException;

/**
 * Stores the result of a ParamModelChecker run (a RegionValues object)
 * as well as additional information (ModelBuilder, FunctionFactory)
 * that is needed to test the actual result against an expected result
 * (test mode).
 */
public class ParamResult
{
	/** The computation mode (parametric / exact) */
	private ParamMode mode;
	/** The actual result */
	private RegionValues regionValues;
	/** The model builder (for accessing expr2func) */
	private ModelBuilder modelBuilder;
	/** The function factory used for model checking */
	private FunctionFactory factory;

	/**
	 * Constructor
	 * @param regionValues the actual result
	 * @param modelBuilder the model builder used during checking
	 * @param factory the function factory used during checking
	 */
	public ParamResult(ParamMode mode, RegionValues regionValues, ModelBuilder modelBuilder, FunctionFactory factory)
	{
		this.mode = mode;
		this.regionValues = regionValues;
		this.modelBuilder = modelBuilder;
		this.factory = factory;
	}

	/**
	 * Get the region values result.
	 */
	public RegionValues getRegionValues()
	{
		return regionValues;
	}

	/**
	 * Returns the result (region values) as a String.
	 */
	@Override
	public String toString()
	{
		return regionValues.toString();
	}

	/**
	 * If ParamModelChecker ran in exact mode (no parametric constants),
	 * the result will consist of a single region and a single value instead
	 * of a function.
	 * <br>
	 * This method returns this single value (either a Boolean or a BigRational).
	 * @param propertyType the type of the checked property
	 * @return Boolean/BigRational result (for the initial state)
	 * @throws PrismException if there are multiple regions or the value is a function
	 */
	public Object getSimpleResult(Type propertyType) throws PrismException
	{
		if (regionValues.getNumRegions() != 1)
			throw new PrismException("Unexpected result from " + mode + " model checker");

		if (propertyType.equals(TypeBool.getInstance())) {
			// boolean result
			boolean boolResult = regionValues.getResult(0).getInitStateValueAsBoolean();
			return boolResult;
		} else {
			// numeric result
			param.Function func = regionValues.getResult(0).getInitStateValueAsFunction();
			// Evaluate the function at an arbitrary point (should not depend on parameter values)
			BigRational rat = func.evaluate(new param.Point(new BigRational[] { new BigRational(0) }));
			return rat;
		}
	}

	/**
	 * Test the result against the given expected result string.
	 * <br>
	 * Returns true if the test succeeds, throws a PrismException
	 * with an explanation otherwise.
	 * @param propertyType the type of the checked property
	 * @param strExpected the expected result (as a String)
	 * @param constValues the model/property constants used during the checking
	 * @param params The names of any parameters, i.e., still undefined constants (null if none)
	 * @return true if the test succeeds
	 * @throws PrismException on test failure
	 */
	public boolean test(Type propertyType, String strExpected, Values constValues, List<String> params) throws PrismException
	{
		Expression exprExpected = null;
		try {
			if (strExpected.equals("Infinity") || strExpected.equals("+Infinity") || strExpected.equals("Inf") || strExpected.equals("+Inf")) {
				exprExpected = new ExpressionLiteral(TypeDouble.getInstance(), BigRational.INF);
			} else if (strExpected.equals("-Infinity") || strExpected.equals("-Inf")) {
				exprExpected = new ExpressionLiteral(TypeDouble.getInstance(), BigRational.MINF);
			} else if (strExpected.equals("NaN")) {
				exprExpected =  new ExpressionLiteral(TypeDouble.getInstance(), BigRational.NAN);
			} else {
				exprExpected = Prism.parseSingleExpressionString(strExpected);

				// the constants that can be used in the expected result expression:
				// defined constants
				ConstantList constantList = new ConstantList(constValues);
				// and parametric constants
				for (String p : params) {
					constantList.addConstant(new ExpressionIdent(p), null, TypeDouble.getInstance());
				}
				exprExpected = (Expression) exprExpected.findAllConstants(constantList);
				exprExpected.typeCheck();

				// replace constants in the expression that have a value
				// with the value
				exprExpected = (Expression) exprExpected.evaluatePartially(constValues);
			}
		} catch (PrismLangException e) {
			throw new PrismException("Invalid RESULT specification \"" + strExpected + "\" for property: " + e.getMessage());
		}
		return test(propertyType, exprExpected, strExpected);
	}

	/**
	 * Test the result against the given expression.
	 * <br>
	 * Returns true if the test succeeds, throws a PrismException
	 * with an explanation otherwise.
	 * @param expected expression for the expected result
	 * @param strExpected the expected result (as a String, for display in error messages)
	 * @param propertyType the type of the checked property
	 * @return true if the test succeeds
	 * @throws PrismException on test failure
	 */
	private boolean test(Type propertyType, Expression expected, String strExpected) throws PrismException
	{
		if (regionValues.getNumRegions() != 1) {
			throw new PrismNotSupportedException("Testing " + mode + " results with multiple regions not supported");
		}

		if (propertyType.equals(TypeBool.getInstance())) {
			// boolean result
			boolean boolResult = regionValues.getResult(0).getInitStateValueAsBoolean();
			boolean boolExpected = expected.evaluateExact().toBoolean();

			if (boolResult != boolExpected) {
				throw new PrismException("Wrong result (expected " + strExpected + ", got " + boolResult + ")");
			}
		} else {
			// numeric result
			Function funcExpected;
			try {
				funcExpected = modelBuilder.expr2function(factory, expected);
			} catch (PrismException e) {
				throw new PrismException("Invalid (or unsupported) RESULT specification \"" + strExpected + "\" for " + mode + " property");
			}
			param.Function func = regionValues.getResult(0).getInitStateValueAsFunction();

			if (!func.equals(funcExpected)) {
				throw new PrismException("Wrong result (expected " + strExpected + " = " + funcExpected + ", got " + func + ")");
			}
		}

		return true;
	}
}
