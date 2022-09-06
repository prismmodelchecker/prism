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

public interface ASTVisitor
{
	// ASTElement classes (model/properties file)
	public Object visit(ModulesFile e) throws PrismLangException;
	public Object visit(PropertiesFile e) throws PrismLangException;
	public Object visit(Property e) throws PrismLangException;
	public Object visit(FormulaList e) throws PrismLangException;
	public Object visit(LabelList e) throws PrismLangException;
	public Object visit(ConstantList e) throws PrismLangException;
	public Object visit(Declaration e) throws PrismLangException;
	public Object visit(DeclarationInt e) throws PrismLangException;
	public Object visit(DeclarationBool e) throws PrismLangException;
	public Object visit(DeclarationArray e) throws PrismLangException;
	public Object visit(DeclarationClock e) throws PrismLangException;
	public Object visit(DeclarationIntUnbounded e) throws PrismLangException;
	public Object visit(parser.ast.Module e) throws PrismLangException;
	public Object visit(Command e) throws PrismLangException;
	public Object visit(Updates e) throws PrismLangException;
	public Object visit(Update e) throws PrismLangException;
	public Object visit(RenamedModule e) throws PrismLangException;
	public Object visit(RewardStruct e) throws PrismLangException;
	public Object visit(RewardStructItem e) throws PrismLangException;
	// ASTElement/SystemDefn classes
	public Object visit(SystemInterleaved e) throws PrismLangException;
	public Object visit(SystemFullParallel e) throws PrismLangException;
	public Object visit(SystemParallel e) throws PrismLangException;
	public Object visit(SystemHide e) throws PrismLangException;
	public Object visit(SystemRename e) throws PrismLangException;
	public Object visit(SystemModule e) throws PrismLangException;
	public Object visit(SystemBrackets e) throws PrismLangException;
	public Object visit(SystemReference e) throws PrismLangException;
	// ASTElement/Expression classes
	public Object visit(ExpressionTemporal e) throws PrismLangException;
	public Object visit(ExpressionITE e) throws PrismLangException;
	public Object visit(ExpressionBinaryOp e) throws PrismLangException;
	public Object visit(ExpressionUnaryOp e) throws PrismLangException;
	public Object visit(ExpressionFunc e) throws PrismLangException;
	public Object visit(ExpressionIdent e) throws PrismLangException;
	public Object visit(ExpressionLiteral e) throws PrismLangException;
	public Object visit(ExpressionConstant e) throws PrismLangException;
	public Object visit(ExpressionFormula e) throws PrismLangException;
	public Object visit(ExpressionVar e) throws PrismLangException;
	public Object visit(ExpressionProb e) throws PrismLangException;
	public Object visit(ExpressionReward e) throws PrismLangException;
	public Object visit(ExpressionSS e) throws PrismLangException;
	public Object visit(ExpressionExists e) throws PrismLangException;
	public Object visit(ExpressionForAll e) throws PrismLangException;
	public Object visit(ExpressionStrategy e) throws PrismLangException;
	public Object visit(ExpressionLabel e) throws PrismLangException;
	public Object visit(ExpressionProp e) throws PrismLangException;
	public Object visit(ExpressionFilter e) throws PrismLangException;
	// ASTElement classes (misc.)
	public Object visit(Filter e) throws PrismLangException;
	public Object visit(ForLoop e) throws PrismLangException;
}

