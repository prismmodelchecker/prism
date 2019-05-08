//==============================================================================
//	
//	Copyright (c) 2018-
//	Authors:
//	* Steffen Maercker <maercker@tcs.inf.tu-dresden.de> (TU Dresden)
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

import java.util.IdentityHashMap;
import java.util.Map;

import parser.ast.*;
import parser.ast.Module;
import prism.PrismLangException;

/**
 * Performs a depth-first traversal of an abstract syntax tree (AST).
 * Each node is visited exactly once, even if the AST is a directed acyclic graph.
 * <p>
 * By default, the implementation uses an identity-based map to identify duplicates.
 * However, this can be configured by providing other map types.
 * To implement the visiting behavior, subclasses should override {@code visitNow}.
 * To visit subsequent nodes, {@code visit} has to be called to ensure that nodes
 * are visited only once.
 * </p>
 * @see ASTTraverse
 * @see java.util.IdentityHashMap
 */
public abstract class DAGVisitor implements ASTVisitor
{
	protected Map<ASTElement, Object> visited;

	/**
	 * Create and configure a DAG-aware visitor with an IdentityHashMap.
	 * This is a conservative choice to ensure that nodes are considered
	 * visited if and only if they are the same object.
	 * 
	 * @see java.util.IdentityHashMap
	 */
	protected DAGVisitor()
	{
		this(new IdentityHashMap<>());
	}

	/**
	 * Create and configure a DAG-aware visitor.
	 * If no map is provided, nodes are visited multiple times.
	 * 
	 * @param visited a map to keep track of duplicates or null
	 * @see java.util.Map
	 */
	protected DAGVisitor(Map<ASTElement, Object> visited)
	{
		this.visited = visited;
	}

	protected <T extends ASTElement> boolean isVisited(T element)
	{
		return visited != null && visited.containsKey(element);
	}

	protected Object getStored(ASTElement element)
	{
		if (visited != null) {
			return visited.get(element);
		}
		return null;
	}

	protected <T extends ASTElement> Object store(T element, Object result)
	{
		if (visited != null) {
			visited.put(element, result);
		}
		return result;
	}

	@Override
	public Object visit(ModulesFile e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(PropertiesFile e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(Property e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(FormulaList e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(LabelList e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ConstantList e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(Declaration e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(DeclarationInt e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(DeclarationBool e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(DeclarationArray e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(DeclarationClock e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(DeclarationIntUnbounded e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(Module e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(Command e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(Updates e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(Update e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(RenamedModule e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(RewardStruct e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(RewardStructItem e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(SystemInterleaved e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(SystemFullParallel e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(SystemParallel e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(SystemHide e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(SystemRename e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(SystemModule e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(SystemBrackets e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(SystemReference e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionTemporal e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionITE e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionBinaryOp e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionUnaryOp e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionFunc e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionIdent e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionLiteral e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionConstant e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionFormula e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionVar e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionProb e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionReward e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionSS e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionExists e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionForAll e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionStrategy e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionLabel e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionProp e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ExpressionFilter e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(Filter e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	@Override
	public Object visit(ForLoop e) throws PrismLangException
	{
		return isVisited(e) ? getStored(e) : store(e, visitNow(e));
	}

	public abstract Object visitNow(ModulesFile e) throws PrismLangException;

	public abstract Object visitNow(PropertiesFile e) throws PrismLangException;

	public abstract Object visitNow(Property e) throws PrismLangException;

	public abstract Object visitNow(FormulaList e) throws PrismLangException;

	public abstract Object visitNow(LabelList e) throws PrismLangException;

	public abstract Object visitNow(ConstantList e) throws PrismLangException;

	public abstract Object visitNow(Declaration e) throws PrismLangException;

	public abstract Object visitNow(DeclarationInt e) throws PrismLangException;

	public abstract Object visitNow(DeclarationBool e) throws PrismLangException;

	public abstract Object visitNow(DeclarationArray e) throws PrismLangException;

	public abstract Object visitNow(DeclarationClock e) throws PrismLangException;

	public abstract Object visitNow(DeclarationIntUnbounded e) throws PrismLangException;

	public abstract Object visitNow(Module e) throws PrismLangException;

	public abstract Object visitNow(Command e) throws PrismLangException;

	public abstract Object visitNow(Updates e) throws PrismLangException;

	public abstract Object visitNow(Update e) throws PrismLangException;

	public abstract Object visitNow(RenamedModule e) throws PrismLangException;

	public abstract Object visitNow(RewardStruct e) throws PrismLangException;

	public abstract Object visitNow(RewardStructItem e) throws PrismLangException;

	public abstract Object visitNow(SystemInterleaved e) throws PrismLangException;

	public abstract Object visitNow(SystemFullParallel e) throws PrismLangException;

	public abstract Object visitNow(SystemParallel e) throws PrismLangException;

	public abstract Object visitNow(SystemHide e) throws PrismLangException;

	public abstract Object visitNow(SystemRename e) throws PrismLangException;

	public abstract Object visitNow(SystemModule e) throws PrismLangException;

	public abstract Object visitNow(SystemBrackets e) throws PrismLangException;

	public abstract Object visitNow(SystemReference e) throws PrismLangException;

	public abstract Object visitNow(ExpressionTemporal e) throws PrismLangException;

	public abstract Object visitNow(ExpressionITE e) throws PrismLangException;

	public abstract Object visitNow(ExpressionBinaryOp e) throws PrismLangException;

	public abstract Object visitNow(ExpressionUnaryOp e) throws PrismLangException;

	public abstract Object visitNow(ExpressionFunc e) throws PrismLangException;

	public abstract Object visitNow(ExpressionIdent e) throws PrismLangException;

	public abstract Object visitNow(ExpressionLiteral e) throws PrismLangException;

	public abstract Object visitNow(ExpressionConstant e) throws PrismLangException;

	public abstract Object visitNow(ExpressionFormula e) throws PrismLangException;

	public abstract Object visitNow(ExpressionVar e) throws PrismLangException;

	public abstract Object visitNow(ExpressionProb e) throws PrismLangException;

	public abstract Object visitNow(ExpressionReward e) throws PrismLangException;

	public abstract Object visitNow(ExpressionSS e) throws PrismLangException;

	public abstract Object visitNow(ExpressionExists e) throws PrismLangException;

	public abstract Object visitNow(ExpressionForAll e) throws PrismLangException;

	public abstract Object visitNow(ExpressionStrategy e) throws PrismLangException;

	public abstract Object visitNow(ExpressionLabel e) throws PrismLangException;

	public abstract Object visitNow(ExpressionProp e) throws PrismLangException;

	public abstract Object visitNow(ExpressionFilter e) throws PrismLangException;

	public abstract Object visitNow(Filter e) throws PrismLangException;

	public abstract Object visitNow(ForLoop e) throws PrismLangException;
}
