//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

package pta;

import java.util.*;

import parser.*;
import parser.ast.*;
import parser.type.*;
import parser.visitor.*;
import prism.*;

/**
 * Class that converts a PRISM modelling language description
 * of a PTA into a PRISM model of a PTA, through digital clocks
 */
public class DigitalClocks
{
	// Prism object
	private Prism prism;
	// Log
	private PrismLog mainLog;
	// Model to be converted
	private ModulesFile modulesFile;
	// Properties to be converted
	private PropertiesFile propertiesFile;
	// Constants from model
	private Values constantValues;
	// Variable list for model
	private VarList varList;

	// Flags + settings
	private boolean doScaling = true;

	// Object for computing max clock constraints
	private ComputeClockInformation cci;
	// String to be used for time action 
	private String timeAction;
	// Some invariant info
	private Expression allInVariants = null;

	// Translated model file
	private ModulesFile mf;
	// Translated properties file
	private PropertiesFile pf;

	/**
	 * Constructor.
	 */
	public DigitalClocks(Prism prism)
	{
		this.prism = prism;
		mainLog = prism.getMainLog();
		mf = null;
		pf = null;
	}

	/**
	 * Get translated model file.
	 */
	public ModulesFile getNewModulesFile()
	{
		return mf;
	}

	/**
	 * Get translated properties file.
	 */
	public PropertiesFile getNewPropertiesFile()
	{
		return pf;
	}

	/**
	 * Main method - translate.
	 */
	public void translate(ModulesFile modulesFile, PropertiesFile propertiesFile, Expression propertyToCheck) throws PrismLangException
	{
		int i, n;
		ASTElement ast;
		ASTTraverseModify asttm;

		mainLog.println("\nPerforming digital clocks translation...");

		// Store model/properties files
		this.modulesFile = modulesFile;
		this.propertiesFile = propertiesFile;
		constantValues = modulesFile.getConstantValues();
		// TODO: need property constants too?
		varList = modulesFile.createVarList();

		// Check that model does not contain any closed clock constraints
		ast = findAStrictClockConstraint(modulesFile);
		if (ast != null)
			throw new PrismLangException("Strict clock constraints are not allowed when using the digital clocks method", ast);
		// Check that model does not contain any diagonal clock constraints
		// (for now; should be able to relax this later)
		ast = findADiagonalClockConstraint(modulesFile);
		if (ast != null)
			throw new PrismLangException("Diagonal clock constraints are not allowed when using the digital clocks method", ast);

		// Check for any references to clocks in rewards structures - not allowed.
		for (RewardStruct rs : modulesFile.getRewardStructs()) {
			rs.accept(new ASTTraverseModify()
			{
				public Object visit(ExpressionVar e) throws PrismLangException
				{
					if (e.getType() instanceof TypeClock) {
						throw new PrismLangException("Reward structures cannot contain references to clocks", e);
					} else {
						return e;
					}
				}
			});
		}
		// Check that the property is suitable for checking with digital clocks
		if (propertyToCheck != null)
			checkProperty(propertyToCheck);

		// Choose a new action label to represent time
		timeAction = "time";
		while (modulesFile.getSynchs().contains(timeAction)) {
			timeAction += "_";
		}

		// Extract information about clocks from the model
		cci = new ComputeClockInformation();
		modulesFile.accept(cci);
		mainLog.println("Computed clock maximums: " + cci.clockMaxs);
		if (doScaling)
			mainLog.println("Computed GCD: " + cci.getScaleFactor());

		// Take a copy of the whole model/properties file before translation
		mf = (ModulesFile) modulesFile.deepCopy();
		pf = (propertiesFile == null) ? null : (PropertiesFile) propertiesFile.deepCopy();

		// Change the model type
		mf.setModelType(ModelType.MDP);

		// Change all clock variable declarations to bounded integers
		mf = (ModulesFile) mf.accept(new ASTTraverseModify()
		{
			public Object visit(Declaration e) throws PrismLangException
			{
				if (e.getDeclType() instanceof DeclarationClock) {
					int cMax = cci.getScaledClockMax(e.getName());
					if (cMax < 0)
						throw new PrismLangException("Clock " + e.getName() + " is unbounded since there are no references to it in the model");
					DeclarationType declType = new DeclarationInt(Expression.Int(0), Expression.Int(cMax + 1));
					Declaration decl = new Declaration(e.getName(), declType);
					return decl;
				} else {
					return e;
				}
			}
		});

		// Add time command based on invariant for each module
		// Also build an "invariants" label, as we go
		allInVariants = null;
		mf = (ModulesFile) mf.accept(new ASTTraverseModify()
		{
			public Object visit(Module e) throws PrismLangException
			{
				Command timeCommand;
				Updates ups;
				Update up;
				int cMax;
				Expression invar;
				ExpressionFunc expr;

				// Get (clock) invariant for module; create default if none
				invar = e.getInvariant();
				invar = (invar == null) ? Expression.True() : invar.deepCopy();
				// Collect invariant for "invariants" label
				if (!Expression.isTrue(invar))
					allInVariants = (allInVariants == null) ? invar.deepCopy() : Expression.And(allInVariants, invar.deepCopy());
				// Replace all clocks x with x+1 in invariant
				invar = (Expression) invar.accept(new ASTTraverseModify()
				{
					public Object visit(ExpressionVar e) throws PrismLangException
					{
						if (e.getType() instanceof TypeClock) {
							return Expression.Plus(e, Expression.Int(1));
						} else {
							return e;
						}
					}
				});
				// Construct command representing progression of time
				timeCommand = new Command();
				timeCommand.setSynch(timeAction);
				// Guard comes from invariant
				timeCommand.setGuard(invar);
				// Update is constructed from clocks
				up = new Update();
				for (String x : cci.getClocksForModule(e.getName())) {
					// Get clock max value
					cMax = cci.getScaledClockMax(x);
					// Build expression min(x+1,cMax)
					expr = new ExpressionFunc("min");
					expr.addOperand(Expression.Plus(new ExpressionVar(x, TypeInt.getInstance()), Expression.Int(1)));
					expr.addOperand(Expression.Int(cMax + 1));
					// Add to update
					up.addElement(new ExpressionIdent(x), expr);
				}
				ups = new Updates();
				ups.addUpdate(Expression.Double(1.0), up);
				timeCommand.setUpdates(ups);
				e.addCommand(timeCommand);
				// Finally, remove invariant info
				e.setInvariant(null);
				// Return modified module
				return e;
			}
		});
		// Add "invariants" label
		mf.getLabelList().addLabel(new ExpressionIdent("invariants"), allInVariants == null ? Expression.True() : allInVariants);

		// Change the type of any clock variable references to int
		// and scale the variable appropriately, if required
		// (in both model and properties list)
		asttm = new ASTTraverseModify()
		{
			// Resets
			public Object visit(Update e) throws PrismLangException
			{
				int i, n;
				ExpressionFunc exprFunc;
				n = e.getNumElements();
				for (i = 0; i < n; i++) {
					if (e.getType(i) instanceof TypeClock) {
						// Don't actually need to set the type here since
						// will be done in subsequent call to tidyUp() but do it anyway.
						e.setType(i, TypeInt.getInstance());
						// Scaling is done with division here, rather than multiplying clock like elsewhere
						if (cci.getScaleFactor() > 1) {
							exprFunc = new ExpressionFunc("floor");
							exprFunc.addOperand(Expression.Divide(e.getExpression(i), Expression.Int(cci.getScaleFactor())));
							e.setExpression(i, (Expression) exprFunc.simplify());
						}
					}
				}
				return e;
			}

			// Variable accesses
			public Object visit(ExpressionVar e) throws PrismLangException
			{
				if (e.getType() instanceof TypeClock) {
					e.setType(TypeInt.getInstance());
					if (!doScaling || cci.getScaleFactor() == 1)
						return e;
					return Expression.Times(e, Expression.Int(cci.getScaleFactor()));
				}
				return e;
			}
		};
		mf = (ModulesFile) mf.accept(asttm);
		if (pf != null)
			pf = (PropertiesFile) pf.accept(asttm);

		// Change state rewards in reward structures to use time action)
		// (transition rewards can be left unchanged)
		// Note: only cumulative (F) properties supported currently.
		for (RewardStruct rs : mf.getRewardStructs()) {
			n = rs.getNumItems();
			for (i = 0; i < n; i++) {
				RewardStructItem rsi = rs.getRewardStructItem(i);
				// Convert state rewards
				if (!rsi.isTransitionReward()) {
					// Scale reward by clock GCD
					Expression rew = rsi.getReward().deepCopy();
					if (cci.getScaleFactor() > 1) {
						rew = Expression.Times(rew, Expression.Int(cci.getScaleFactor()));
					}
					rsi = new RewardStructItem(timeAction, rsi.getStates().deepCopy(), rew);
					rs.setRewardStructItem(i, rsi);
				}
			}
		}

		// Re-do type checking etc. on the model/properties
		mf.tidyUp();
		if (pf != null)
			pf.tidyUp();
	}

	/**
	 * Check that a property is checkable with the digital clocks method.
	 * Throw an explanatory exception if not.
	 */
	public void checkProperty(Expression propertyToCheck) throws PrismLangException
	{
		ASTElement ast;

		// LTL not handled (look in any P operators)
		try {
			propertyToCheck.accept(new ASTTraverse()
			{
				public void visitPost(ExpressionProb e) throws PrismLangException
				{
					if (!e.getExpression().isSimplePathFormula()) {
						throw new PrismLangException("The digital clocks method does not support LTL properties");
					}
				}
			});
		} catch (PrismLangException e) {
			e.setASTElement(propertyToCheck);
			throw e;
		}
		
		// Bounded properties not yet handled.
		try {
			propertyToCheck.accept(new ASTTraverse()
			{
				public void visitPost(ExpressionTemporal e) throws PrismLangException
				{
					if (e.getLowerBound() != null || e.getUpperBound() != null)
						throw new PrismLangException("The digital clocks method does not yet support bounded properties");
				}
			});
		} catch (PrismLangException e) {
			e.setASTElement(propertyToCheck);
			throw e;
		}

		// Check that there are no nested probabilistic operators
		if (propertyToCheck.computeProbNesting() > 1) {
			throw new PrismLangException("Nested P operators are not allowed when using the digital clocks method", propertyToCheck);
		}

		// Check for presence of strict clock constraints
		ast = findAStrictClockConstraint(propertyToCheck);
		if (ast != null)
			throw new PrismLangException("Strict clock constraints are not allowed when using the digital clocks method", ast);
		// Check for presence of diagonal clock constraints
		// (for now; should be able to relax this later)
		ast = findADiagonalClockConstraint(modulesFile);
		if (ast != null)
			throw new PrismLangException("Diagonal clock constraints are not allowed when using the digital clocks method", ast);
		// TODO: also need to look in any required properties file labels
		// (currently, these cannot even contain clocks so not an issue)
	}

	/**
	 * Look for a strict clock constraint. If found return the offending element. Else, return null.
	 */
	public ASTElement findAStrictClockConstraint(ASTElement ast) throws PrismLangException
	{
		try {
			ASTTraverse astt = new ASTTraverse()
			{
				// Clock constraints
				public void visitPost(ExpressionBinaryOp e) throws PrismLangException
				{
					if (e.getOperand1().getType() instanceof TypeClock) {
						if (e.getOperator() == ExpressionBinaryOp.GT || e.getOperator() == ExpressionBinaryOp.LT)
							throw new PrismLangException("Found one", e);
					} else if (e.getOperand2().getType() instanceof TypeClock) {
						if (e.getOperator() == ExpressionBinaryOp.GT || e.getOperator() == ExpressionBinaryOp.LT)
							throw new PrismLangException("Found one", e);
					}
				}
			};
			ast.accept(astt);
		} catch (PrismLangException e) {
			return e.getASTElement();
		}
		return null;
	}

	/**
	 * Look for a diagonal clock constraint. If found return the offending element. Else, return null.
	 */
	public ASTElement findADiagonalClockConstraint(ASTElement ast) throws PrismLangException
	{
		try {
			ASTTraverse astt = new ASTTraverse()
			{
				// Clock constraints
				public void visitPost(ExpressionBinaryOp e) throws PrismLangException
				{
					if (e.getOperand1().getType() instanceof TypeClock) {
						if (e.getOperand2().getType() instanceof TypeClock) {
							throw new PrismLangException("Found one", e);
						}
					}
				}
			};
			modulesFile.accept(astt);
		} catch (PrismLangException e) {
			return e.getASTElement();
		}
		return null;
	}

	/**
	 * Class to extract information about clocks:
	 * - list of clocks for each module;
	 * - maximum value that each clock is compared against or set to;
	 * - g.c.d. of all integers used in clock comparisons or assignments.
	 */
	class ComputeClockInformation extends ASTTraverse
	{
		private Map<String, List<String>> clockLists;
		private List<String> currentClockList;
		private Map<String, Integer> clockMaxs;
		private Set<Integer> allClockVals;
		private int scaleFactor;

		public ComputeClockInformation()
		{
			clockLists = new HashMap<String, List<String>>();
			clockMaxs = new HashMap<String, Integer>();
			allClockVals = new HashSet<Integer>();
		}

		private void updateMax(String clock, int val)
		{
			Integer i = clockMaxs.get(clock);
			if (i == null || val > i)
				clockMaxs.put(clock, val);
		}

		public List<String> getClocksForModule(String module)
		{
			List<String> list = clockLists.get(module);
			return (list == null) ? new ArrayList<String>() : list;
		}

		public int getClockMax(String clock)
		{
			Integer i = clockMaxs.get(clock);
			return (i == null) ? -1 : i;
		}

		/**
		 * Get the maximum value of a clock, scaled wrt. GCD (if required)
		 * @param clock
		 * @return
		 */
		public int getScaledClockMax(String clock)
		{
			Integer i = clockMaxs.get(clock);
			return (i == null) ? -1 : doScaling ? i / scaleFactor : i;
		}

		public int getScaleFactor()
		{
			return scaleFactor;
		}

		/**
		 * Compute greatest common divisor of several non-negative ints
		 */
		private int computeGCD(Iterable<Integer> ints)
		{
			int gcd = 0;
			for (int x : ints) {
				gcd = computeGCD(gcd, x);
			}
			if (gcd == 0) {
				// For the case where clock set is empty or all zeros
				gcd = 1;
			}
			return gcd;
		}

		/**
		 * Compute greatest common divisor of 2 non-negative ints
		 */
		private int computeGCD(int x, int y)
		{
			return (y == 0) ? x : computeGCD(y, x % y);
		}

		public void visitPost(ModulesFile e) throws PrismLangException
		{
			// When have traversed the model, compute GCDs and scale factor
			scaleFactor = computeGCD(allClockVals);
		}

		public void visitPre(Module e) throws PrismLangException
		{
			// Create new array to store clocks for this module
			currentClockList = new ArrayList<String>();
			clockLists.put(e.getName(), currentClockList);
		}

		public void visitPost(Declaration e) throws PrismLangException
		{
			// Detect clock variable and store info
			if (e.getDeclType() instanceof DeclarationClock) {
				currentClockList.add(e.getName());
			}
		}

		// Resets
		public Object visit(Update e) throws PrismLangException
		{
			int i, n;
			String clock;
			int maxVal;
			Collection<Integer> allVals;
			n = e.getNumElements();
			for (i = 0; i < n; i++) {
				if (e.getType(i) instanceof TypeClock) {
					clock = e.getVar(i);
					maxVal = ParserUtils.findMaxForIntExpression(e.getExpression(i), varList, constantValues);
					updateMax(clock, maxVal);
					allVals = ParserUtils.findAllValsForIntExpression(e.getExpression(i), varList, constantValues);
					allClockVals.addAll(allVals);
				}
			}
			return e;
		}

		// Clock constraints
		public void visitPost(ExpressionBinaryOp e) throws PrismLangException
		{
			// If this is a clock constraint, get and store max value
			// (only look at x ~ c or c ~ x)
			String clock;
			int maxVal;
			Collection<Integer> allVals;
			if (e.getOperand1().getType() instanceof TypeClock) {
				if (!(e.getOperand2().getType() instanceof TypeClock)) {
					clock = ((ExpressionVar) e.getOperand1()).getName();
					maxVal = ParserUtils.findMaxForIntExpression(e.getOperand2(), varList, constantValues);
					updateMax(clock, maxVal);
					allVals = ParserUtils.findAllValsForIntExpression(e.getOperand2(), varList, constantValues);
					allClockVals.addAll(allVals);
				}
			} else if (e.getOperand2().getType() instanceof TypeClock) {
				clock = ((ExpressionVar) e.getOperand2()).getName();
				maxVal = ParserUtils.findMaxForIntExpression(e.getOperand1(), varList, constantValues);
				updateMax(clock, maxVal);
				allVals = ParserUtils.findAllValsForIntExpression(e.getOperand1(), varList, constantValues);
				allClockVals.addAll(allVals);
			}
		}
	}
}
