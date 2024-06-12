//==============================================================================
//	
//	Copyright (c) 2022-
//	Authors:
//	* Max Kurze <Max.Kurze@mailbox.tu-dresden.de> (TU Dresden)
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


import java.util.List;
import java.util.ListIterator;

import parser.ast.ASTElement;
import parser.ast.Command;
import parser.ast.ConstantList;
import parser.ast.Declaration;
import parser.ast.DeclarationArray;
import parser.ast.DeclarationBool;
import parser.ast.DeclarationClock;
import parser.ast.DeclarationDoubleUnbounded;
import parser.ast.DeclarationInt;
import parser.ast.DeclarationIntUnbounded;
import parser.ast.ExpressionBinaryOp;
import parser.ast.ExpressionConstant;
import parser.ast.ExpressionExists;
import parser.ast.ExpressionFilter;
import parser.ast.ExpressionForAll;
import parser.ast.ExpressionFormula;
import parser.ast.ExpressionFunc;
import parser.ast.ExpressionITE;
import parser.ast.ExpressionIdent;
import parser.ast.ExpressionInterval;
import parser.ast.ExpressionLabel;
import parser.ast.ExpressionLiteral;
import parser.ast.ExpressionObs;
import parser.ast.ExpressionProb;
import parser.ast.ExpressionProp;
import parser.ast.ExpressionReward;
import parser.ast.ExpressionSS;
import parser.ast.ExpressionStrategy;
import parser.ast.ExpressionTemporal;
import parser.ast.ExpressionUnaryOp;
import parser.ast.ExpressionVar;
import parser.ast.Filter;
import parser.ast.ForLoop;
import parser.ast.FormulaList;
import parser.ast.LabelList;
import parser.ast.Module;
import parser.ast.ModulesFile;
import parser.ast.Observable;
import parser.ast.ObservableVars;
import parser.ast.PropertiesFile;
import parser.ast.Property;
import parser.ast.RenamedModule;
import parser.ast.RewardStruct;
import parser.ast.RewardStructItem;
import parser.ast.SystemBrackets;
import parser.ast.SystemFullParallel;
import parser.ast.SystemHide;
import parser.ast.SystemInterleaved;
import parser.ast.SystemModule;
import parser.ast.SystemParallel;
import parser.ast.SystemReference;
import parser.ast.SystemRename;
import parser.ast.Update;
import parser.ast.UpdateElement;
import parser.ast.Updates;
import prism.PrismLangException;

/**
 * DeepCopy is a visitor that copies an AST.
 * <p>
 * For copying it provides the methods {@link DeepCopy#copy} and {@link DeepCopy#copyAll} and relies
 * on {@link ASTElement#deepCopy(DeepCopy)}.
 * </p>
 *
 * @see ASTElement#deepCopy(DeepCopy)
 */
public class DeepCopy implements ASTVisitor
{
	/**
	 * Copy an ASTElement or null.
	 *
	 * @param element the element to be copied or null
	 * @return copy of the element or null
	 * @throws PrismLangException
	 */
	@SuppressWarnings("unchecked")
	public <T extends ASTElement> T copy(T element) throws PrismLangException
	{
		return (element == null) ? null : (T) element.accept(this);
	}

	/**
	 * Copy all ASTElements (or null) in the collection.
	 *
	 * @param list list of elements to be copied
	 * @return the argument list with all elements copied
	 * @throws PrismLangException
	 */
	public <T extends ASTElement> List<T> copyAll(List<T> list) throws PrismLangException
	{
		if (list == null)
			return null;

		ListIterator<T> iter = list.listIterator();
		while (iter.hasNext()) {
			iter.set(copy(iter.next()));
		}
		return list;
	}

	@Override
	public Object visit(ModulesFile e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(PropertiesFile e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(Property e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(FormulaList e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(LabelList e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ConstantList e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(Declaration e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(DeclarationInt e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(DeclarationBool e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(DeclarationArray e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(DeclarationClock e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(DeclarationIntUnbounded e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(DeclarationDoubleUnbounded e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(Module e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(Command e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(Updates e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(Update e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(UpdateElement e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(RenamedModule e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(RewardStruct e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(RewardStructItem e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ObservableVars e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(Observable e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(SystemInterleaved e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(SystemFullParallel e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(SystemParallel e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(SystemHide e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(SystemRename e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(SystemModule e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(SystemBrackets e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(SystemReference e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionTemporal e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionITE e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionBinaryOp e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionUnaryOp e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionFunc e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionIdent e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionLiteral e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionConstant e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionFormula e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionVar e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionInterval e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionProb e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionReward e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionSS e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionExists e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionForAll e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionStrategy e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionLabel e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionObs e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionProp e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ExpressionFilter e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(Filter e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Object visit(ForLoop e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}
}
