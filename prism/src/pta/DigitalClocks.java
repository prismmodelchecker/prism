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

package pta;

import java.util.*;

import parser.*;
import parser.ast.*;
import parser.ast.Module;
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
	// Constants from model
	private Values constantValues;
	// Variable list for model
	private VarList varList;
	// Time bound from property if present
	private int timeBound = -1;

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
	// Translated property to check
	private Expression prop;

	/**
	 * Constructor.
	 */
	public DigitalClocks(Prism prism)
	{
		this.prism = prism;
		mainLog = prism.getMainLog();
		mf = null;
		pf = null;
		prop = null;
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
	 * Get translated property to check.
	 */
	public Expression getNewPropertyToCheck()
	{
		return prop;
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

		// Store some info for global access
		constantValues = modulesFile.getConstantValues();
		if (propertiesFile != null ) {
			constantValues = new Values(constantValues, propertiesFile.getConstantValues());
		}
		varList = modulesFile.createVarList();

		// Check that model does not contain any closed clock constraints
		ast = findAStrictClockConstraint(modulesFile, null);
		if (ast != null) {
			throw new PrismLangException("Strict clock constraints are not allowed when using the digital clocks method", ast);
		}
		// Check that model does not contain any diagonal clock constraints
		// (for now; should be able to relax this later)
		ast = findADiagonalClockConstraint(modulesFile, null);
		if (ast != null) {
			throw new PrismLangException("Diagonal clock constraints are not allowed when using the digital clocks method", ast);
		}
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
		if (propertyToCheck != null) {
			checkProperty(propertyToCheck, propertiesFile);
		}
		
		// Choose a new action label to represent time
		timeAction = "time";
		while (modulesFile.getSynchs().contains(timeAction)) {
			timeAction += "_";
		}

		// Extract information about clocks from the model
		cci = new ComputeClockInformation(modulesFile, propertiesFile, propertyToCheck);
		mainLog.println("Computed clock maximums: " + cci.getClockMaxs());
		if (doScaling) {
			mainLog.println("Computed GCD: " + cci.getScaleFactor());
		}
		
		// Take a copy of the whole model/properties file before translation
		mf = (ModulesFile) modulesFile.deepCopy();
		pf = (propertiesFile == null) ? null : (PropertiesFile) propertiesFile.deepCopy();
		prop = (Expression) propertyToCheck.deepCopy();

		// Change the model type
		mf.setModelType(ModelType.MDP);

		// Change all clock variable declarations to bounded integers
		mf = (ModulesFile) mf.accept(new ASTTraverseModify()
		{
			public Object visit(Declaration e) throws PrismLangException
			{
				if (e.getDeclType() instanceof DeclarationClock) {
					int cMax = cci.getScaledClockMax(e.getName());
					if (cMax < 0) {
						throw new PrismLangException("Clock " + e.getName() + " is unbounded since there are no references to it in the model");
					}
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
			public Object visit(parser.ast.Module e) throws PrismLangException
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
				if (!Expression.isTrue(invar)) {
					allInVariants = (allInVariants == null) ? invar.deepCopy() : Expression.And(allInVariants, invar.deepCopy());
				}
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
		if (pf != null) {
			pf = (PropertiesFile) pf.accept(asttm);
		}
		prop = (Expression) prop.accept(asttm);

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

		// If we are checking a time bounded property... 
		if (timeBound != -1) {
			
			int scaledTimeBound = timeBound / cci.getScaleFactor();
			
			// First add a timer to the model
			
			// Create names for module/variable for timer
			String timerModuleName = "timer";
			while (mf.getModuleIndex(timerModuleName) != -1) {
				timerModuleName = "_" + timerModuleName;
			}
			String timerVarName = "timer";
			while (mf.isIdentUsed(timerVarName) || (pf != null && pf.isIdentUsed(timerVarName))) {
				timerVarName = "_" + timerVarName;
			}
			// Store time bound as a constant
			String timeboundName = "T";
			while (mf.isIdentUsed(timeboundName) || (pf != null && pf.isIdentUsed(timeboundName))) {
				timeboundName = "_" + timeboundName;
			}
			mf.getConstantList().addConstant(new ExpressionIdent(timeboundName), Expression.Int(scaledTimeBound), TypeInt.getInstance());
			// Create module/variable
			Module timerModule = new Module(timerModuleName);
			DeclarationType timerDeclType = new DeclarationInt(Expression.Int(0),
					Expression.Plus(new ExpressionConstant(timeboundName, TypeInt.getInstance()), Expression.Int(1)));
			Declaration timerDecl = new Declaration(timerVarName, timerDeclType);
			timerModule.addDeclaration(timerDecl);
			// Construct command representing progression of time
			Command timeCommand = new Command();
			timeCommand.setSynch(timeAction);
			timeCommand.setGuard(Expression.True());
			// Construct update
			Update up = new Update();
			// Build expression min(timer+1,timerMax)
			ExpressionFunc exprMin = new ExpressionFunc("min");
			exprMin.addOperand(Expression.Plus(new ExpressionVar(timerVarName, TypeInt.getInstance()), Expression.Int(1)));
			exprMin.addOperand(Expression.Plus(new ExpressionConstant(timeboundName, TypeInt.getInstance()), Expression.Literal(1)));
			// Add to update
			up.addElement(new ExpressionIdent(timerVarName), exprMin);
			Updates ups = new Updates();
			ups.addUpdate(Expression.Double(1.0), up);
			timeCommand.setUpdates(ups);
			timerModule.addCommand(timeCommand);
			// Finally add module to model
			mf.addModule(timerModule);
			
			// Then modify the property
			
			// Build time bound (timer <= T)
			Expression timerRef = new ExpressionVar(timerVarName, TypeInt.getInstance()); 
			Expression boundNew = new ExpressionBinaryOp(ExpressionBinaryOp.LE, timerRef, new ExpressionConstant(timeboundName, TypeInt.getInstance()));
			prop.accept(new ASTTraverseModify()
			{
				public Object visit(ExpressionTemporal e) throws PrismLangException
				{
					// Push (new) time bound into target
					e.setUpperBound(null);
					Expression targetNew = Expression.And(e.getOperand2().deepCopy(), boundNew);
					e.setOperand2(targetNew);
					return e;
				}
			});
		}
							
		// Re-do type checking, indexing, etc. on the model/properties
		mf.tidyUp();
		if (pf != null) {
			pf.setModelInfo(mf);
			pf.tidyUp();
		}
		prop.findAllVars(mf.getVarNames(), mf.getVarTypes());
		// Copy across undefined constants since these get lost in the call to tidyUp()
		mf.setSomeUndefinedConstants(modulesFile.getUndefinedConstantValues());
		pf.setSomeUndefinedConstants(propertiesFile.getUndefinedConstantValues());
	}

	/**
	 * Check that a property is checkable with the digital clocks method.
	 * Throw an explanatory exception if not.
	 * Optionally, an enclosing PropertiesFile is provided, to look up
	 * property/label references. Can be null.
	 */
	public void checkProperty(Expression propertyToCheck, PropertiesFile propertiesFile) throws PrismLangException
	{
		ASTElement ast;
		LabelList labelList = (propertiesFile == null) ? null : propertiesFile.getLabelList();

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

		// Bounded properties: check allowable, and store time bound if so
		timeBound = -1;
		try {
			propertyToCheck.accept(new ASTTraverse()
			{
				public void visitPost(ExpressionTemporal e) throws PrismLangException
				{
					if (e.getLowerBound() != null) {
						throw new PrismLangException("The digital clocks method does not yet support lower time bounds");
					}
					if (e.getUpperBound() != null) {
						if (!ExpressionTemporal.isFinally(e)) {
							throw new PrismLangException("The digital clocks method only ssupport time bounds on F");
						}
						timeBound = e.getUpperBound().evaluateInt(constantValues);
					}
				}
			});
		} catch (PrismLangException e) {
			e.setASTElement(propertyToCheck);
			throw e;
		}

		// Check that there are no nested probabilistic operators
		if (propertyToCheck.computeProbNesting(propertiesFile) > 1) {
			throw new PrismLangException("Nested P/R operators are not allowed when using the digital clocks method", propertyToCheck);
		}

		// Check for presence of strict clock constraints
		ast = findAStrictClockConstraint(propertyToCheck, labelList);
		if (ast != null) {
			throw new PrismLangException("Strict clock constraints are not allowed when using the digital clocks method", ast);
		}
		// Check for presence of diagonal clock constraints
		// (for now; should be able to relax this later)
		ast = findADiagonalClockConstraint(propertyToCheck, labelList);
		if (ast != null) {
			throw new PrismLangException("Diagonal clock constraints are not allowed when using the digital clocks method", ast);
		}
	}

	/**
	 * Look for a strict clock constraint. If found return the offending element. Else, return null.
	 * Optionally, pass in a LabelList to look up labels (can be null).
	 */
	public ASTElement findAStrictClockConstraint(ASTElement ast, LabelList labelList) throws PrismLangException
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
				
				// Recurse on labels
				public void visitPost(ExpressionLabel e) throws PrismLangException
				{
					if (labelList != null) {
						int i = labelList.getLabelIndex(e.getName());
						if (i != -1) {
							labelList.getLabel(i).accept(this);
						}
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
	 * Optionally, pass in a LabelList to look up labels (can be null).
	 */
	public ASTElement findADiagonalClockConstraint(ASTElement ast, LabelList labelList) throws PrismLangException
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
				
				// Recurse on labels
				public void visitPost(ExpressionLabel e) throws PrismLangException
				{
					if (labelList != null) {
						int i = labelList.getLabelIndex(e.getName());
						if (i != -1) {
							labelList.getLabel(i).accept(this);
						}
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
	 * Class to extract information about clocks:
	 * - list of clocks for each module;
	 * - maximum value that each clock is compared against or set to;
	 * - g.c.d. of all integers used in clock comparisons or assignments.
	 */
	class ComputeClockInformation extends ASTTraverse
	{
		// Model/property info
		PropertiesFile propertiesFile = null;
		LabelList labelList = null;
		// Clock info
		private Map<String, List<String>> clockLists;
		private List<String> currentClockList;
		private Map<String, Integer> clockMaxs;
		private Set<Integer> allClockVals;
		private int scaleFactor;
		
		public ComputeClockInformation(ModulesFile modulesFile, PropertiesFile propertiesFile, Expression propertyToCheck) throws PrismLangException
		{
			// Extract some info needed for traversal 
			this.propertiesFile = propertiesFile;
			labelList = (propertiesFile == null) ? null : propertiesFile.getLabelList();
			// Set up storage
			clockLists = new HashMap<String, List<String>>();
			clockMaxs = new HashMap<String, Integer>();
			allClockVals = new HashSet<Integer>();
			// Traverse ModulesFile first (further storage created)
			modulesFile.accept(this);
			// Then property (but not whole property file, to maximise possible scaling)
			propertyToCheck.accept(this);
			// Finally, compute GCDs and scale factor
			scaleFactor = computeGCD(allClockVals);
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

		public Map<String, Integer> getClockMaxs()
		{
			return clockMaxs;
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

		// AST traversal
		
		public void visitPre(parser.ast.Module e) throws PrismLangException
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
		
		// Time bounds in properties
		public void visitPost(ExpressionTemporal e) throws PrismLangException
		{
			// This is easier than for clock constraints since they must be constant
			// (so just evaluate directly)
			// We also don't care about the max value - this is done elsewhere;
			// we just want to make sure that the values is used to compute the GCD
			if (e.getLowerBound() != null) {
				allClockVals.add(e.getLowerBound().evaluateInt(constantValues));
			}
			if (e.getUpperBound() != null) {
				allClockVals.add(e.getUpperBound().evaluateInt(constantValues));
			}
		}
		
		// Recurse on labels
		public void visitPost(ExpressionLabel e) throws PrismLangException
		{
			if (labelList != null) {
				int i = labelList.getLabelIndex(e.getName());
				if (i != -1) {
					labelList.getLabel(i).accept(this);
				}
			}
		}
		
		// Recurse on property refs
		public void visitPost(ExpressionProp e) throws PrismLangException
		{
			// If possible, look up property and recurse
			if (propertiesFile != null) {
				Property prop = propertiesFile.lookUpPropertyObjectByName(e.getName());
				if (prop != null) {
					prop.accept(this);
				} else {
					throw new PrismLangException("Unknown property reference " + e, e);
				}
			}
		}
	}
}
