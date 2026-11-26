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

package parser.visitor;

import parser.ast.ConstantList;
import parser.ast.Expression;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionFilter;
import parser.ast.ExpressionITE;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionSS;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import parser.ast.FormulaList;
import parser.ast.LabelList;
import parser.ast.ModulesFile;
import parser.ast.PropertiesFile;
import prism.ModelInfo;
import prism.PrismLangException;

/**
 * Perform any required semantic checks on a PropertiesFile (or parts of it).
 * These checks are done *before* any undefined constants have been defined.
 * Optionally pass in parent ModulesFile (or leave null);
 */
public class PropertiesSemanticCheck extends SemanticCheck
{
	private PropertiesFile propertiesFile;
	private ModelInfo modelInfo;
	private ModulesFile modulesFile;

	public PropertiesSemanticCheck(PropertiesFile propertiesFile)
	{
		this(propertiesFile, null);
	}

	public PropertiesSemanticCheck(PropertiesFile propertiesFile, ModelInfo modelInfo)
	{
		setPropertiesFile(propertiesFile);
		setModelInfo(modelInfo);
	}

	public void setPropertiesFile(PropertiesFile propertiesFile)
	{
		this.propertiesFile = propertiesFile;
	}

	public void setModelInfo(ModelInfo modelInfo)
	{
		this.modelInfo = modelInfo;
		if (modelInfo instanceof ModulesFile) {
			this.modulesFile = (ModulesFile) modelInfo;
		} else {
			this.modulesFile = null;
		}
	}

	public Object visit(FormulaList e) throws PrismLangException
	{
		// Override - don't need to do any semantic checks on formulas
		// (they will have been expanded in place, where needed)
		// (and we shouldn't check them - e.g. clock vars appearing in errors would show as an error)
		return null;
	}
	
	public void visitPost(LabelList e) throws PrismLangException
	{
		int i, n;
		String s;
		n = e.size();
		for (i = 0; i < n; i++) {
			s = e.getLabelName(i);
			if ("deadlock".equals(s))
				throw new PrismLangException("Cannot define a label called \"deadlock\" - this is a built-in label", e.getLabel(i));
			if ("init".equals(s))
				throw new PrismLangException("Cannot define a label called \"init\" - this is a built-in label", e.getLabel(i));
		}
	}

	public void visitPost(ConstantList e) throws PrismLangException
	{
		int i, n;
		n = e.size();
		for (i = 0; i < n; i++) {
			if (e.getConstant(i) != null && !e.getConstant(i).isConstant()) {
				throw new PrismLangException("Definition of constant \"" + e.getConstantName(i) + "\" is not constant", e.getConstant(i));
			}
		}
	}

	// Checks for unbracketed temporal operators whose scope is ambiguous.
	// A unary temporal operator (X, F, G) can be the operand of a non-temporal
	// operator, e.g. "a & F b", and its scope then extends as far to the right
	// as its precedence allows. This differs from the usual reading in LTL,
	// where prefix operators bind most tightly, so brackets are required if:
	// (a) its operand has a logical connective or ? : at the top,
	//     e.g. "a & F b & c" ("a & F (b & c)", not "(a & F b) & c"); or
	// (b) it is within the unbracketed operand of another temporal operator,
	//     e.g. "F a & G b" ("F (a & G b)", not "(F a) & (G b)").

	@Override
	public void visitPre(ExpressionTemporal e) throws PrismLangException
	{
		if (e.getOperand1() != null && containsTemporalOperandOfNonTemporal(e.getOperand1())) {
			throw ambiguousTemporalScope(e, false);
		}
		if (e.getOperand2() != null && containsTemporalOperandOfNonTemporal(e.getOperand2())) {
			throw ambiguousTemporalScope(e, true);
		}
	}

	@Override
	public void visitPre(ExpressionBinaryOp e) throws PrismLangException
	{
		checkTemporalOperandOfNonTemporal(e.getOperand1());
		checkTemporalOperandOfNonTemporal(e.getOperand2());
	}

	@Override
	public void visitPre(ExpressionUnaryOp e) throws PrismLangException
	{
		if (e.getOperator() != ExpressionUnaryOp.PARENTH) {
			checkTemporalOperandOfNonTemporal(e.getOperand());
		}
	}

	@Override
	public void visitPre(ExpressionITE e) throws PrismLangException
	{
		checkTemporalOperandOfNonTemporal(e.getOperand1());
		checkTemporalOperandOfNonTemporal(e.getOperand2());
		checkTemporalOperandOfNonTemporal(e.getOperand3());
	}

	/**
	 * Check case (a) above for an operand of a non-temporal operator.
	 */
	private void checkTemporalOperandOfNonTemporal(Expression operand) throws PrismLangException
	{
		if (isUnaryTemporal(operand)) {
			Expression body = getUnaryTemporalBody((ExpressionTemporal) operand);
			if (isConnective(body) || body instanceof ExpressionITE) {
				throw ambiguousTemporalScope((ExpressionTemporal) operand, true);
			}
		}
	}

	/**
	 * Does an (unbracketed) operand of a temporal operator contain,
	 * without any intervening brackets, a unary temporal operator
	 * that is the operand of a non-temporal operator? (case (b) above)
	 */
	private static boolean containsTemporalOperandOfNonTemporal(Expression expr)
	{
		if (expr instanceof ExpressionTemporal) {
			ExpressionTemporal exprTemp = (ExpressionTemporal) expr;
			return (exprTemp.getOperand1() != null && containsTemporalOperandOfNonTemporal(exprTemp.getOperand1()))
					|| (exprTemp.getOperand2() != null && containsTemporalOperandOfNonTemporal(exprTemp.getOperand2()));
		}
		Expression[] operands;
		if (expr instanceof ExpressionBinaryOp) {
			operands = new Expression[] { ((ExpressionBinaryOp) expr).getOperand1(), ((ExpressionBinaryOp) expr).getOperand2() };
		} else if (expr instanceof ExpressionUnaryOp && ((ExpressionUnaryOp) expr).getOperator() != ExpressionUnaryOp.PARENTH) {
			operands = new Expression[] { ((ExpressionUnaryOp) expr).getOperand() };
		} else if (expr instanceof ExpressionITE) {
			ExpressionITE exprITE = (ExpressionITE) expr;
			operands = new Expression[] { exprITE.getOperand1(), exprITE.getOperand2(), exprITE.getOperand3() };
		} else {
			return false;
		}
		for (Expression operand : operands) {
			if (isUnaryTemporal(operand) || containsTemporalOperandOfNonTemporal(operand)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Create an error for a temporal operator whose scope is ambiguous,
	 * suggesting bracketed versions: the usual LTL reading of the (unary chain of)
	 * operators, if {@code rotate} is true and it differs, and PRISM's reading.
	 */
	private static PrismLangException ambiguousTemporalScope(ExpressionTemporal e, boolean rotate)
	{
		Expression prismReading = bracketTemporalOperands(e);
		String msg = "Ambiguous scope of temporal operator: add brackets, e.g. ";
		Expression body = getUnaryTemporalBody(e);
		if (rotate && isConnective(body)) {
			msg += "\"" + rotateTemporal(e, body) + "\" or ";
		}
		msg += "\"" + prismReading + "\"";
		// Attach the error to PRISM's (bracketed) reading, at the position of the operator
		prismReading.setPosition(e);
		return new PrismLangException(msg, prismReading);
	}

	/**
	 * Copy a temporal operator (and any chain of unary ones in its second operand),
	 * bracketing operands that have a logical connective or ? : at the top.
	 */
	private static Expression bracketTemporalOperands(ExpressionTemporal e)
	{
		ExpressionTemporal copy = (ExpressionTemporal) e.deepCopy();
		ExpressionTemporal t = copy;
		while (true) {
			if (needsBrackets(t.getOperand1())) {
				t.setOperand1(Expression.Parenth(t.getOperand1()));
			}
			if (isUnaryTemporal(t.getOperand2())) {
				t = (ExpressionTemporal) t.getOperand2();
			} else {
				if (needsBrackets(t.getOperand2())) {
					t.setOperand2(Expression.Parenth(t.getOperand2()));
				}
				return copy;
			}
		}
	}

	/**
	 * Construct the LTL-style reading of temporal operator {@code e} applied to {@code body},
	 * where the operator applies only to the leftmost operand of any logical connectives,
	 * e.g. "(F a) & c" for "F a & c".
	 */
	private static Expression rotateTemporal(ExpressionTemporal e, Expression body)
	{
		if (isConnective(body)) {
			ExpressionBinaryOp binOp = (ExpressionBinaryOp) body;
			return new ExpressionBinaryOp(binOp.getOperator(), rotateTemporal(e, binOp.getOperand1()), binOp.getOperand2().deepCopy());
		}
		ExpressionTemporal copy = (ExpressionTemporal) e.deepCopy();
		ExpressionTemporal t = copy;
		while (isUnaryTemporal(t.getOperand2())) {
			t = (ExpressionTemporal) t.getOperand2();
		}
		t.setOperand2(body.deepCopy());
		return Expression.Parenth(copy);
	}

	private static boolean isUnaryTemporal(Expression expr)
	{
		return expr instanceof ExpressionTemporal && ((ExpressionTemporal) expr).getOperand1() == null && ((ExpressionTemporal) expr).getOperand2() != null;
	}

	/**
	 * Get the operand of a temporal operator, skipping any chain of
	 * (unbracketed) unary temporal operators, e.g. "a" for "G F a".
	 */
	private static Expression getUnaryTemporalBody(ExpressionTemporal e)
	{
		Expression body = e.getOperand2();
		while (isUnaryTemporal(body)) {
			body = ((ExpressionTemporal) body).getOperand2();
		}
		return body;
	}

	private static boolean isConnective(Expression expr)
	{
		if (!(expr instanceof ExpressionBinaryOp)) {
			return false;
		}
		int op = ((ExpressionBinaryOp) expr).getOperator();
		return op == ExpressionBinaryOp.IMPLIES || op == ExpressionBinaryOp.IFF || op == ExpressionBinaryOp.OR || op == ExpressionBinaryOp.AND;
	}

	private static boolean needsBrackets(Expression expr)
	{
		return isConnective(expr) || expr instanceof ExpressionITE;
	}

	public void visitPost(ExpressionTemporal e) throws PrismLangException
	{
		int op = e.getOperator();
		Expression operand1 = e.getOperand1();
		Expression operand2 = e.getOperand2();
		Expression lBound = e.getLowerBound();
		Expression uBound = e.getUpperBound();
		if (lBound != null && !lBound.isConstant()) {
			throw new PrismLangException("Lower bound in " + e.getOperatorSymbol() + " operator is not constant", lBound);
		}
		if (uBound != null && !uBound.isConstant()) {
			throw new PrismLangException("Upper bound in " + e.getOperatorSymbol() + " operator is not constant", uBound);
		}
		// Other checks (which parser should never allow to occur anyway)
		if (op == ExpressionTemporal.P_X && (operand1 != null || operand2 == null || lBound != null || uBound != null)) {
			throw new PrismLangException("Cannot attach bounds to " + e.getOperatorSymbol() + " operator", e);
		}
		if (op == ExpressionTemporal.R_C && (operand1 != null || operand2 != null || lBound != null)) {
			// NB: upper bound is optional (e.g. multi-objective allows R...[C] operator)
			throw new PrismLangException("Badly formed " + e.getOperatorSymbol() + " operator", e);
		}
		if (op == ExpressionTemporal.R_I && (operand1 != null || operand2 != null || lBound != null || uBound == null)) {
			throw new PrismLangException("Badly formed " + e.getOperatorSymbol() + " operator", e);
		}
		if (op == ExpressionTemporal.R_S && (operand1 != null || operand2 != null || lBound != null || uBound != null)) {
			throw new PrismLangException("Badly formed " + e.getOperatorSymbol() + " operator", e);
		}
		if (e.getDiscount() != null) {
			if (!e.getDiscount().isConstant()) {
				throw new PrismLangException("Discount factor in " + e.getOperatorSymbol() + " operator is not constant", e.getDiscount());
			}
			if (op != ExpressionTemporal.R_C && op != ExpressionTemporal.P_F) {
				throw new PrismLangException("Discount factor not supported for " + e.getOperatorSymbol() + " operator", e);
			}
		}
	}

	public void visitPost(ExpressionProb e) throws PrismLangException
	{
		if (e.getModifier() != null) {
			throw new PrismLangException("Modifier \"" + e.getModifier() + "\" not supported for P operator");
		}
		if (e.getProb() != null && !e.getProb().isConstant()) {
			throw new PrismLangException("P operator probability bound is not constant", e.getProb());
		}
	}

	public void visitPost(ExpressionReward e) throws PrismLangException
	{
		if (e.getModifier() != null) {
			throw new PrismLangException("Modifier \"" + e.getModifier() + "\" not supported for R operator");
		}
		if (e.getRewardStructIndex() != null) {
			if (e.getRewardStructIndex() instanceof Expression) {
				Expression rsi = (Expression) e.getRewardStructIndex();
				if (!(rsi.isConstant())) {
					throw new PrismLangException("R operator reward struct index is not constant", rsi);
				}
			} else if (e.getRewardStructIndex() instanceof String) {
				String s = (String) e.getRewardStructIndex();
				if (modulesFile != null && modulesFile.getRewardStructIndex(s) == -1) {
					throw new PrismLangException("R operator reward struct index \"" + s + "\" does not exist", e);
				}
			}
		}
		if (e.getRewardStructIndexDiv() != null) {
			if (e.getRewardStructIndexDiv() instanceof Expression) {
				Expression rsi = (Expression) e.getRewardStructIndexDiv();
				if (!(rsi.isConstant())) {
					throw new PrismLangException("R operator reward struct index is not constant", rsi);
				}
			} else if (e.getRewardStructIndexDiv() instanceof String) {
				String s = (String) e.getRewardStructIndexDiv();
				if (modulesFile != null && modulesFile.getRewardStructIndex(s) == -1) {
					throw new PrismLangException("R operator reward struct index \"" + s + "\" does not exist", e);
				}
			}
		}
		if (e.getReward() != null && !e.getReward().isConstant()) {
			throw new PrismLangException("R operator reward bound is not constant", e.getReward());
		}
	}

	public void visitPost(ExpressionSS e) throws PrismLangException
	{
		if (e.getModifier() != null) {
			throw new PrismLangException("Modifier \"" + e.getModifier() + "\" not supported for S operator");
		}
		if (e.getProb() != null && !e.getProb().isConstant()) {
			throw new PrismLangException("S operator probability bound is not constant", e.getProb());
		}
	}

	public void visitPost(ExpressionLabel e) throws PrismLangException
	{
		String name = e.getName();
		// Allow special cases
		if ("deadlock".equals(name) || "init".equals(name))
			return;
		// Otherwise check if it exists
		if (modelInfo != null && modelInfo.getLabelIndex(name) != -1) { 
			return;
		} else if (propertiesFile != null) {
			LabelList labelList = propertiesFile.getCombinedLabelList();
			if (labelList != null && labelList.getLabelIndex(name) != -1) {
				return;
			}
		}
		throw new PrismLangException("Undeclared label", e);
	}

	public void visitPost(ExpressionFilter e) throws PrismLangException
	{
		// Check filter type is valid
		if (e.getOperatorType() == null) {
			throw new PrismLangException("Unknown filter type \"" + e.getOperatorName() + "\"", e);
		}
	}
}
