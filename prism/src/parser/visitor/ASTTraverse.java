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

import parser.ast.*;
import prism.PrismLangException;

// Performs a depth-first traversal of an asbtract syntax tree (AST).
// Many traversal-based tasks can be implemented by extending and either:
// (a) overriding defaultVisitPre or defaultVisitPost
// (b) overiding visit for leaf (or other selected) nodes
// See also ASTTraverseModify.

public class ASTTraverse implements ASTVisitor
{
	public void defaultVisitPre(ASTElement e) throws PrismLangException {}
	public void defaultVisitPost(ASTElement e) throws PrismLangException {}
	// -----------------------------------------------------------------------------------
	public void visitPre(ModulesFile e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ModulesFile e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		if (e.getFormulaList() != null) e.getFormulaList().accept(this);
		if (e.getLabelList() != null) e.getLabelList().accept(this);
		if (e.getConstantList() != null) e.getConstantList().accept(this);
		n = e.getNumGlobals();
		for (i = 0; i < n; i++) {
			if (e.getGlobal(i) != null) e.getGlobal(i).accept(this);
		}
		n = e.getNumModules();
		for (i = 0; i < n; i++) {
			if (e.getModule(i) != null) e.getModule(i).accept(this);
		}
		if (e.getSystemDefn() != null) e.getSystemDefn().accept(this);
		n = e.getNumRewardStructs();
		for (i = 0; i < n; i++) {
			if (e.getRewardStruct(i) != null) e.getRewardStruct(i).accept(this);
		}
		if (e.getInitialStates() != null) e.getInitialStates().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(ModulesFile e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(PropertiesFile e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(PropertiesFile e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		if (e.getLabelList() != null) e.getLabelList().accept(this);
		if (e.getConstantList() != null) e.getConstantList().accept(this);
		n = e.getNumProperties();
		for (i = 0; i < n; i++) {
			if (e.getPropertyObject(i) != null) e.getPropertyObject(i).accept(this);
		}
		visitPost(e);
		return null;
	}
	public void visitPost(PropertiesFile e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(Property e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(Property e) throws PrismLangException
	{
		visitPre(e);
		if (e.getExpression() != null) e.getExpression().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(Property e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(FormulaList e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(FormulaList e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		n = e.size();
		for (i = 0; i < n; i++) {
			if (e.getFormula(i) != null) e.getFormula(i).accept(this);
		}
		visitPost(e);
		return null;
	}
	public void visitPost(FormulaList e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(LabelList e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(LabelList e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		n = e.size();
		for (i = 0; i < n; i++) {
			if (e.getLabel(i) != null) e.getLabel(i).accept(this);
		}
		visitPost(e);
		return null;
	}
	public void visitPost(LabelList e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ConstantList e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ConstantList e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		n = e.size();
		for (i = 0; i < n; i++) {
			if (e.getConstant(i) != null) e.getConstant(i).accept(this);
		}
		visitPost(e);
		return null;
	}
	public void visitPost(ConstantList e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(Declaration e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(Declaration e) throws PrismLangException
	{
		visitPre(e);
		if (e.getDeclType() != null) e.getDeclType().accept(this);
		if (e.getStart() != null) e.getStart().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(Declaration e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(DeclarationInt e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(DeclarationInt e) throws PrismLangException
	{
		visitPre(e);
		if (e.getLow() != null) e.getLow().accept(this);
		if (e.getHigh() != null) e.getHigh().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(DeclarationInt e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(DeclarationBool e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(DeclarationBool e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return null;
	}
	public void visitPost(DeclarationBool e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(DeclarationArray e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(DeclarationArray e) throws PrismLangException
	{
		visitPre(e);
		if (e.getLow() != null) e.getLow().accept(this);
		if (e.getHigh() != null) e.getHigh().accept(this);
		if (e.getSubtype() != null) e.getSubtype().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(DeclarationArray e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(DeclarationClock e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(DeclarationClock e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return null;
	}
	public void visitPost(DeclarationClock e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(DeclarationIntUnbounded e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(DeclarationIntUnbounded e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return null;
	}
	public void visitPost(DeclarationIntUnbounded e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(parser.ast.Module e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(parser.ast.Module e) throws PrismLangException
	{
		// Note: a few classes override this method (e.g. SemanticCheck)
		// so take care to update those versions if changing this method
		visitPre(e);
		int i, n;
		n = e.getNumDeclarations();
		for (i = 0; i < n; i++) {
			if (e.getDeclaration(i) != null) e.getDeclaration(i).accept(this);
		}
		if (e.getInvariant() != null)
			e.getInvariant().accept(this);
		n = e.getNumCommands();
		for (i = 0; i < n; i++) {
			if (e.getCommand(i) != null) e.getCommand(i).accept(this);
		}
		visitPost(e);
		return null;
	}
	public void visitPost(parser.ast.Module e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(Command e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(Command e) throws PrismLangException
	{
		// Note: a few classes override this method (e.g. SemanticCheck)
		// so take care to update those versions if changing this method
		visitPre(e);
		e.getGuard().accept(this);
		e.getUpdates().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(Command e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(Updates e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(Updates e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		n = e.getNumUpdates();
		for (i = 0; i < n; i++) {
			if (e.getProbability(i) != null) e.getProbability(i).accept(this);
			if (e.getUpdate(i) != null) e.getUpdate(i).accept(this);
		}
		visitPost(e);
		return null;
	}
	public void visitPost(Updates e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(Update e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(Update e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		n = e.getNumElements();
		for (i = 0; i < n; i++) {
			if (e.getExpression(i) != null) e.getExpression(i).accept(this);
		}
		visitPost(e);
		return null;
	}
	public void visitPost(Update e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(RenamedModule e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(RenamedModule e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return null;
	}
	public void visitPost(RenamedModule e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(RewardStruct e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(RewardStruct e) throws PrismLangException
	{
		visitPre(e);
		int i, n;
		n = e.getNumItems();
		for (i = 0; i < n; i++) {
			if (e.getRewardStructItem(i) != null) e.getRewardStructItem(i).accept(this);
		}
		visitPost(e);
		return null;
	}
	public void visitPost(RewardStruct e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(RewardStructItem e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(RewardStructItem e) throws PrismLangException
	{
		visitPre(e);
		e.getStates().accept(this);
		e.getReward().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(RewardStructItem e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemInterleaved e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemInterleaved e) throws PrismLangException
	{
		visitPre(e);
		int i, n = e.getNumOperands();
		for (i = 0; i < n; i++) {
			if (e.getOperand(i) != null) e.getOperand(i).accept(this);
		}
		visitPost(e);
		return null;
	}
	public void visitPost(SystemInterleaved e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemFullParallel e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemFullParallel e) throws PrismLangException
	{
		visitPre(e);
		int i, n = e.getNumOperands();
		for (i = 0; i < n; i++) {
			if (e.getOperand(i) != null) e.getOperand(i).accept(this);
		}
		visitPost(e);
		return null;
	}
	public void visitPost(SystemFullParallel e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemParallel e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemParallel e) throws PrismLangException
	{
		visitPre(e);
		e.getOperand1().accept(this);
		e.getOperand2().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(SystemParallel e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemHide e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemHide e) throws PrismLangException
	{
		visitPre(e);
		e.getOperand().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(SystemHide e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemRename e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemRename e) throws PrismLangException
	{
		visitPre(e);
		e.getOperand().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(SystemRename e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemModule e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemModule e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return null;
	}
	public void visitPost(SystemModule e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemBrackets e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemBrackets e) throws PrismLangException
	{
		visitPre(e);
		e.getOperand().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(SystemBrackets e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(SystemReference e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(SystemReference e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return null;
	}
	public void visitPost(SystemReference e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionTemporal e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionTemporal e) throws PrismLangException
	{
		visitPre(e);
		if (e.getOperand1() != null) e.getOperand1().accept(this);
		if (e.getOperand2() != null) e.getOperand2().accept(this);
		if (e.getLowerBound() != null) e.getLowerBound().accept(this);
		if (e.getUpperBound() != null) e.getUpperBound().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionTemporal e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionITE e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionITE e) throws PrismLangException
	{
		visitPre(e);
		e.getOperand1().accept(this);
		e.getOperand2().accept(this);
		e.getOperand3().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionITE e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionBinaryOp e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionBinaryOp e) throws PrismLangException
	{
		visitPre(e);
		e.getOperand1().accept(this);
		e.getOperand2().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionBinaryOp e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionUnaryOp e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionUnaryOp e) throws PrismLangException
	{
		visitPre(e);
		e.getOperand().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionUnaryOp e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionFunc e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionFunc e) throws PrismLangException
	{
		visitPre(e);
		int i, n = e.getNumOperands();
		for (i = 0; i < n; i++) {
			if (e.getOperand(i) != null) e.getOperand(i).accept(this);
		}
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionFunc e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionIdent e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionIdent e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionIdent e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionLiteral e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionLiteral e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionLiteral e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionConstant e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionConstant e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionConstant e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionFormula e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionFormula e) throws PrismLangException
	{
		visitPre(e);
		if (e.getDefinition() != null) e.getDefinition().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionFormula e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionVar e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionVar e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionVar e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionProb e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionProb e) throws PrismLangException
	{
		visitPre(e);
		if (e.getProb() != null) e.getProb().accept(this);
		if (e.getExpression() != null) e.getExpression().accept(this);
		if (e.getFilter() != null) e.getFilter().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionProb e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionReward e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionReward e) throws PrismLangException
	{
		visitPre(e);
		if (e.getRewardStructIndex() != null && e.getRewardStructIndex() instanceof Expression) ((Expression)e.getRewardStructIndex()).accept(this);
		if (e.getRewardStructIndexDiv() != null && e.getRewardStructIndexDiv() instanceof Expression) ((Expression)e.getRewardStructIndexDiv()).accept(this);
		if (e.getReward() != null) e.getReward().accept(this);
		if (e.getExpression() != null) e.getExpression().accept(this);
		if (e.getFilter() != null) e.getFilter().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionReward e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionSS e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionSS e) throws PrismLangException
	{
		visitPre(e);
		if (e.getProb() != null) e.getProb().accept(this);
		if (e.getExpression() != null) e.getExpression().accept(this);
		if (e.getFilter() != null) e.getFilter().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionSS e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionExists e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionExists e) throws PrismLangException
	{
		visitPre(e);
		if (e.getExpression() != null) e.getExpression().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionExists e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionForAll e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionForAll e) throws PrismLangException
	{
		visitPre(e);
		if (e.getExpression() != null) e.getExpression().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionForAll e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionStrategy e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionStrategy e) throws PrismLangException
	{
		visitPre(e);
		int i, n = e.getNumOperands();
		for (i = 0; i < n; i++) {
			if (e.getOperand(i) != null) e.getOperand(i).accept(this);
		}
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionStrategy e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionLabel e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionLabel e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionLabel e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionProp e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionProp e) throws PrismLangException
	{
		visitPre(e);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionProp e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ExpressionFilter e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ExpressionFilter e) throws PrismLangException
	{
		visitPre(e);
		if (e.getFilter() != null) e.getFilter().accept(this);
		if (e.getOperand() != null) e.getOperand().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(ExpressionFilter e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(Filter e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(Filter e) throws PrismLangException
	{
		visitPre(e);
		if (e.getExpression() != null) e.getExpression().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(Filter e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
	public void visitPre(ForLoop e) throws PrismLangException { defaultVisitPre(e); }
	public Object visit(ForLoop e) throws PrismLangException
	{
		visitPre(e);
		if (e.getFrom() != null) e.getFrom().accept(this);
		if (e.getTo() != null) e.getTo().accept(this);
		if (e.getStep() != null) e.getStep().accept(this);
		visitPost(e);
		return null;
	}
	public void visitPost(ForLoop e) throws PrismLangException { defaultVisitPost(e); }
	// -----------------------------------------------------------------------------------
}
