//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

package explicit;

import parser.Values;
import parser.ast.Expression;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionTemporal;
import parser.ast.LabelList;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import parser.ast.RewardStruct;
import prism.PrismComponent;
import prism.PrismException;
import prism.Result;
import simulator.PrismModelExplorer;
import simulator.SimulatorEngine;

/**
 * CTMC model checker based on fast adaptive uniformisation.
 */
public class FastAdaptiveUniformisationModelChecker extends PrismComponent
{
	// Model file
	private ModulesFile modulesFile;
	// Properties file
	private PropertiesFile propertiesFile;
	// Simulator engine
	private SimulatorEngine engine;
	// Constants from model
	private Values constantValues;
	// Labels from the model
	private LabelList labelListModel;
	// Labels from the property file
	private LabelList labelListProp;
	
	/**
	 * Constructor.
	 */
	public FastAdaptiveUniformisationModelChecker(PrismComponent parent, ModulesFile modulesFile, PropertiesFile propertiesFile, SimulatorEngine engine) throws PrismException
	{
		super(parent);
		this.modulesFile = modulesFile;
		this.propertiesFile = propertiesFile;
		this.engine = engine;

		// Get combined constant values from model/properties
		constantValues = new Values();
		constantValues.addValues(modulesFile.getConstantValues());
		if (propertiesFile != null)
			constantValues.addValues(propertiesFile.getConstantValues());
		this.labelListModel = modulesFile.getLabelList();
		this.labelListProp = propertiesFile.getLabelList();
	}

	/**
	 * Model check a property.
	 */
	public Result check(Expression expr) throws PrismException
	{
		Result res;
		String resultString;
		long timer;

		// Starting model checking
		timer = System.currentTimeMillis();

		// Do model checking
		res = checkExpression(expr);

		// Model checking complete
		timer = System.currentTimeMillis() - timer;
		mainLog.println("\nModel checking completed in " + (timer / 1000.0) + " secs.");

		// Print result to log
		resultString = "Result";
		if (!("Result".equals(expr.getResultName())))
			resultString += " (" + expr.getResultName().toLowerCase() + ")";
		resultString += ": " + res;
		mainLog.print("\n" + resultString + "\n");

		// Return result
		return res;
	}

	/**
	 * Model check an expression (used recursively).
	 */
	private Result checkExpression(Expression expr) throws PrismException
	{
		Result res;

		// Current range of supported properties is quite limited...
		if (expr instanceof ExpressionProb)
			res = checkExpressionProb((ExpressionProb) expr);
		else if (expr instanceof ExpressionReward)
			res = checkExpressionReward((ExpressionReward) expr);
		else
			throw new PrismException("Fast adaptive uniformisation not yet supported for this operator");

		return res;
	}

	/**
	 * Model check a P operator.
	 */
	private Result checkExpressionProb(ExpressionProb expr) throws PrismException
	{
		// Check whether P=? (only case allowed)
		if (expr.getProb() != null) {
			throw new PrismException("Fast adaptive uniformisation model checking currently only supports P=? properties");
		}

		if (!(expr.getExpression() instanceof ExpressionTemporal)) {
			throw new PrismException("Fast adaptive uniformisation model checking currently only supports simple path operators");
		}
		ExpressionTemporal exprTemp = (ExpressionTemporal) expr.getExpression();
		if (!exprTemp.isSimplePathFormula()) {
			throw new PrismException("Fast adaptive uniformisation window model checking currently only supports simple until operators");
		}

		double timeLower = 0.0;
		if (exprTemp.getLowerBound() != null) {
			timeLower = exprTemp.getLowerBound().evaluateDouble(constantValues);
		}
		if (exprTemp.getUpperBound() == null) {
			throw new PrismException("Fast adaptive uniformisation window model checking currently requires an upper time bound");
		}
		double timeUpper = exprTemp.getUpperBound().evaluateDouble(constantValues);

		if (!exprTemp.hasBounds()) {
			throw new PrismException("Fast adaptive uniformisation window model checking currently only supports timed properties");
		}

		mainLog.println("Starting transient probability computation using fast adaptive uniformisation...");
		PrismModelExplorer modelExplorer = new PrismModelExplorer(engine, modulesFile);
		FastAdaptiveUniformisation fau = new FastAdaptiveUniformisation(this, modelExplorer);
		fau.setConstantValues(constantValues);

		Expression op1 = exprTemp.getOperand1();
		if (op1 == null) {
			op1 = Expression.True();
		}
		Expression op2 = exprTemp.getOperand2();
		op1 = (Expression) op1.expandPropRefsAndLabels(propertiesFile, labelListModel);
		op1 = (Expression) op1.expandPropRefsAndLabels(propertiesFile, labelListProp);
		op2 = (Expression) op2.expandPropRefsAndLabels(propertiesFile, labelListModel);
		op2 = (Expression) op2.expandPropRefsAndLabels(propertiesFile, labelListProp);
		int operator = exprTemp.getOperator();

		Expression sink = null;
		Expression target = null;
		switch (operator) {
		case ExpressionTemporal.P_U:
		case ExpressionTemporal.P_F:
			sink = Expression.Not(op1);
			break;
		case ExpressionTemporal.P_G:
			sink = Expression.False();
			break;
		case ExpressionTemporal.P_W:
		case ExpressionTemporal.P_R:
		default:
			throw new PrismException("operator currently not supported for fast adaptive uniformisation");
		}
		fau.setSink(sink);
		fau.computeTransientProbsAdaptive(timeLower);
		fau.clearSinkStates();

		switch (operator) {
		case ExpressionTemporal.P_U:
		case ExpressionTemporal.P_F:
			sink = Expression.Or(Expression.Not(op1), op2);
			target = op2;
			break;
		case ExpressionTemporal.P_G:
			sink = Expression.Not(op2);
			target = op2;
			break;
		case ExpressionTemporal.P_W:
		case ExpressionTemporal.P_R:
		default:
			throw new PrismException("operator currently not supported for fast adaptive uniformisation");
		}
		Values varValues = new Values();
		varValues.addValue("deadlock", "true");
		sink.replaceVars(varValues);
		fau.setAnalysisType(FastAdaptiveUniformisation.AnalysisType.REACH);
		fau.setSink(sink);
		fau.setTarget(target);
		fau.computeTransientProbsAdaptive(timeUpper - timeLower);
		mainLog.println("\nTotal probability lost is : " + fau.getTotalDiscreteLoss());
		mainLog.println("Maximal number of states stored during analysis : " + fau.getMaxNumStates());

		return new Result(new Double(fau.getValue()));
	}

	/**
	 * Model check an R operator.
	 */
	private Result checkExpressionReward(ExpressionReward expr) throws PrismException
	{
		mainLog.println("Starting transient probability computation using fast adaptive uniformisation...");
		PrismModelExplorer modelExplorer = new PrismModelExplorer(engine, modulesFile);
		FastAdaptiveUniformisation fau = new FastAdaptiveUniformisation(this, modelExplorer);
		ExpressionTemporal temporal = (ExpressionTemporal) expr.getExpression();
		switch (temporal.getOperator()) {
		case ExpressionTemporal.R_I:
			fau.setAnalysisType(FastAdaptiveUniformisation.AnalysisType.REW_INST);
			break;
		case ExpressionTemporal.R_C:
			fau.setAnalysisType(FastAdaptiveUniformisation.AnalysisType.REW_CUMUL);
			break;
		default:
			throw new PrismException("Currently only instantaneous or cumulative rewards are allowed.");
		}
		double time = temporal.getUpperBound().evaluateDouble(constantValues);
		RewardStruct rewStruct = expr.getRewardStructByIndexObject(modulesFile, constantValues);
		fau.setRewardStruct(rewStruct);
		fau.setConstantValues(constantValues);
		fau.computeTransientProbsAdaptive(time);
		mainLog.println("\nTotal probability lost is : " + fau.getTotalDiscreteLoss());
		mainLog.println("Maximal number of states stored during analysis : " + fau.getMaxNumStates());
		return new Result(new Double(fau.getValue()));
	}
}
