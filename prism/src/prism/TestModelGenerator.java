//==============================================================================
//	
//	Copyright (c) 2002-
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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import parser.State;
import parser.VarList;
import parser.ast.Declaration;
import parser.ast.DeclarationInt;
import parser.ast.Expression;
import parser.ast.PropertiesFile;
import parser.type.Type;
import parser.type.TypeInt;
import explicit.ConstructModel;
import explicit.DTMCModelChecker;

public class TestModelGenerator implements ModelGenerator
{
	protected State exploreState;
	protected int x;
	protected int n;
	protected List<String> varNames = Arrays.asList("x");
	protected List<Type> varTypes = Arrays.asList((Type) TypeInt.getInstance());

	public TestModelGenerator(int n)
	{
		this.n = n;
	}

	@Override
	public ModelType getModelType()
	{
		return ModelType.DTMC;
	}

	@Override
	public int getNumVars()
	{
		return 1;
	}
	
	@Override
	public List<String> getVarNames()
	{
		return varNames;
	}

	@Override
	public List<Type> getVarTypes()
	{
		return varTypes;
	}

	@Override
	public int getNumLabels()
	{
		return 1;
	}
	
	@Override
	public List<String> getLabelNames()
	{
		return Arrays.asList("goal");
	}
	
	@Override
	public State getInitialState() throws PrismException
	{
		State s = new State(1);
		s.varValues[0] = n/2;
		return s;
	}

	@Override
	public void exploreState(State exploreState) throws PrismException
	{
		this.exploreState = exploreState;
		x = ((Integer) exploreState.varValues[0]).intValue();
	}

	@Override
	public State getExploreState()
	{
		return exploreState;
	}

	@Override
	public int getNumChoices() throws PrismException
	{
		return 1;
	}

	@Override
	public int getNumTransitions(int i) throws PrismException
	{
		return x > 0 && x < n ? 2 : 1;
	}

	@Override
	public Object getTransitionAction(int i) throws PrismException
	{
		return null;
	}

	@Override
	public Object getTransitionAction(int i, int offset) throws PrismException
	{
		return null;
	}

	@Override
	public double getTransitionProbability(int i, int offset) throws PrismException
	{
		return x > 0 && x < n ? 0.5 : 1.0;
	}

	@Override
	public State computeTransitionTarget(int i, int offset) throws PrismException
	{
		State s = new State(1);
		if (x == 0 || x == n) {
			s.varValues[i] = x;
		} else {
			s.varValues[i] = (offset == 0) ? x -1 : x + 1;
		}
		return s;
	}
	
	@Override
	public boolean isLabelTrue(int i) throws PrismException
	{
		if (i == 0) {
			return x == n;
		} else {
			throw new PrismException("Label number \"" + i + "\" not defined");
		}
	}

	@Override
	public boolean rewardStructHasTransitionRewards(int i)
	{
		return false;
	}

	@Override
	public VarList createVarList()
	{
		VarList varList = new VarList();
		try {
			varList.addVar(new Declaration("x", new DeclarationInt(Expression.Int(0), Expression.Int(n))), 0, null);
		} catch (PrismLangException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return varList;
	}
	
	public static void main(String args[])
	{
		try {
			Prism prism = new Prism(new PrismPrintStreamLog(System.out));
			prism.setMainLog(new PrismFileLog("stdout"));
			prism.initialise();

			int test = 2;

			if (test == 1) {
				// Direct usage of model constructor/checker 
				TestModelGenerator modelGen = new TestModelGenerator(10);
				ConstructModel constructModel = new ConstructModel(prism);
				explicit.Model model = constructModel.constructModel(modelGen);
				model.exportToDotFile(new PrismFileLog("test.dot"), null, true);
				DTMCModelChecker mc = new DTMCModelChecker(prism);
				PropertiesFile pf = prism.parsePropertiesString(modelGen, "P=? [F \"goal\"]");
				Expression expr = pf.getProperty(0);
				Result res = mc.check(model, expr);
				System.out.println(res);
			} else {
				// Perform model construction/checking via Prism
				TestModelGenerator modelGen2 = new TestModelGenerator(10);
				prism.loadModelGenerator(modelGen2);
				prism.exportTransToFile(true, Prism.EXPORT_DOT_STATES, new File("test2.dot"));
				PropertiesFile pf = prism.parsePropertiesString(modelGen2, "P=? [F x=10]");
				Expression expr = pf.getProperty(0);
				Result res = prism.modelCheck(pf, expr);
				System.out.println(res);
			}

			prism.closeDown(true);
		} catch (PrismException e) {
			System.err.println("Error: " + e.getMessage());
		} catch (FileNotFoundException e) {
			System.err.println("Error: " + e.getMessage());
		}

	}
}
