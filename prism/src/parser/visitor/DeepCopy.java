//==============================================================================
//	
//	Copyright (c) 2018-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
//	* Johannes Kath <s1799679@mail.zih.tu-dresden.de> (TU Dresden)
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

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import parser.ast.*;
import parser.ast.Module;
import prism.PrismLangException;

/**
 * DeepCopy is a visitor that copies an AST with out duplicating nodes.
 * <p>
 * After certain replacements, e.g., formula substitution,
 * the AST becomes a directed acyclic graph.
 * DeepCopy preserves that structure on copying to prevent
 * a (potentially exponential) blowup in the number of nodes.
 * </p>
 * <p>
 * The implementation uses an equality-based map to identify duplicates.
 * For copying it provides the methods {@code copy} and {@copyAll} and relies
 * on {@code deepCopy} in ASTElement.
 * </p>
 * @see ASTElement#deepCopy(DeepCopy)
 * @see java.util.HashMap
 */
public class DeepCopy extends DAGVisitor
{
	public DeepCopy()
	{
		super(new HashMap<>());
	}
	/**
	 * Wrapper for {@PrismLangException} to enable usage of {@link List#replaceAll}
	 * by masking the exception temporarily as RuntimeException;
	 */
	@SuppressWarnings("serial")
	protected class WrappedPrismLangException extends RuntimeException
	{
		public WrappedPrismLangException(PrismLangException cause)
		{
			super(cause);
		}

		public PrismLangException getCause()
		{
			return (PrismLangException) super.getCause();
		}
	}

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
		return (element == null) ? null :(T) element.accept(this);
	}

	/**
	 * Copy all ASTElements (or null) in the list.
	 *
	 * @param list list of elements to be copied
	 * @return the argument list with all elements copied
	 * @throws PrismLangException
	 */
	public <T extends ASTElement> List<T> copyAll(List<T> list) throws PrismLangException
	{
		if (list != null) {
			try {
				list.replaceAll(d -> copyOrThrow(d));
			}
			catch (WrappedPrismLangException e) {
				throw e.getCause();
			}
		}
		return list;
	}

	/**
	 * Copy all ASTElements (or null) in the vector.
	 *
	 * @param vect vector of elements to be copied
	 * @return the argument vector with all elements copied
	 * @throws PrismLangException
	 */
	public <T extends ASTElement> Vector<T> copyAll(Vector<T> vect) throws PrismLangException
	{
		if (vect != null) {
			try {
				vect.replaceAll(d -> copyOrThrow(d));
			}
			catch (WrappedPrismLangException e) {
				throw e.getCause();
			}
		}
		return vect;
	}

	/**
	 * Copy an ASTElement and mask PrismLangExpecitons as RuntimeExceptions
	 *
	 * @param element the element to be copied
	 * @return copy of the element
	 * @throws WrappedPrismLangException if a PrismLangException occurs internally
	 */
	protected <T extends ASTElement> T copyOrThrow(T element)
	{
		try {
			return copy(element);
		} catch (PrismLangException e) {
			throw new WrappedPrismLangException(e);
		}
	}

	@Override
	public ModulesFile visitNow(ModulesFile e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public PropertiesFile visitNow(PropertiesFile e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Property visitNow(Property e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public FormulaList visitNow(FormulaList e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public LabelList visitNow(LabelList e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ConstantList visitNow(ConstantList e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Declaration visitNow(Declaration e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public DeclarationInt visitNow(DeclarationInt e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public DeclarationBool visitNow(DeclarationBool e) throws PrismLangException
	{
		return e.clone();
	}

	@Override
	public DeclarationArray visitNow(DeclarationArray e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public DeclarationClock visitNow(DeclarationClock e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public DeclarationIntUnbounded visitNow(DeclarationIntUnbounded e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Module visitNow(Module e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Command visitNow(Command e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Updates visitNow(Updates e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Update visitNow(Update e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public RenamedModule visitNow(RenamedModule e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public RewardStruct visitNow(RewardStruct e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public RewardStructItem visitNow(RewardStructItem e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public SystemInterleaved visitNow(SystemInterleaved e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public SystemFullParallel visitNow(SystemFullParallel e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public SystemParallel visitNow(SystemParallel e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public SystemHide visitNow(SystemHide e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public SystemRename visitNow(SystemRename e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public SystemModule visitNow(SystemModule e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public SystemBrackets visitNow(SystemBrackets e) throws PrismLangException
	{
		SystemBrackets copy = e.clone();

		copy.setOperand(copy(copy.getOperand()));

		return copy;
	}

	@Override
	public SystemReference visitNow(SystemReference e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionTemporal visitNow(ExpressionTemporal e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionITE visitNow(ExpressionITE e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionBinaryOp visitNow(ExpressionBinaryOp e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionUnaryOp visitNow(ExpressionUnaryOp e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionFunc visitNow(ExpressionFunc e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionIdent visitNow(ExpressionIdent e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionLiteral visitNow(ExpressionLiteral e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionConstant visitNow(ExpressionConstant e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionFormula visitNow(ExpressionFormula e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionVar visitNow(ExpressionVar e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionProb visitNow(ExpressionProb e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionReward visitNow(ExpressionReward e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionSS visitNow(ExpressionSS e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionExists visitNow(ExpressionExists e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionForAll visitNow(ExpressionForAll e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionStrategy visitNow(ExpressionStrategy e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionLabel visitNow(ExpressionLabel e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionProp visitNow(ExpressionProp e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ExpressionFilter visitNow(ExpressionFilter e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public ForLoop visitNow(ForLoop e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}

	@Override
	public Filter visitNow(Filter e) throws PrismLangException
	{
		return e.clone().deepCopy(this);
	}
}
