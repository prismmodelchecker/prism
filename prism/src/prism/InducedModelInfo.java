//==============================================================================
//
//	Copyright (c) 2026-
//	Authors:
//	* Dave Parker <david.parker@cs.ox.ac.uk> (University of Oxford)
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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import parser.EvaluateContext;
import parser.Values;
import parser.VarList;
import parser.ast.DeclarationType;
import parser.type.Type;

/**
 * Model info for a model induced from another one, e.g., by applying a strategy.
 * Constants, actions, labels and modules are those of the original model,
 * but the model type, and possibly the variables, can differ
 * (e.g., a strategy with memory adds a variable for it).
 * Partial observability is not preserved.
 */
public class InducedModelInfo implements ModelInfo
{
	/** Model info for the original model */
	private ModelInfo origModelInfo;
	/** Type of the induced model */
	private ModelType modelType;
	/** Variables of the induced model (null if the same as the original model) */
	private VarList varList;

	/**
	 * Create model info for a model induced from one with model info {@code origModelInfo}.
	 * @param origModelInfo Model info for the original model
	 * @param modelType Type of the induced model
	 * @param varList Variables of the induced model (null if the same as the original model)
	 */
	public InducedModelInfo(ModelInfo origModelInfo, ModelType modelType, VarList varList)
	{
		this.origModelInfo = origModelInfo;
		this.modelType = modelType;
		this.varList = varList;
	}

	/**
	 * Get the model info for the original model.
	 */
	public ModelInfo getOriginalModelInfo()
	{
		return origModelInfo;
	}

	@Override
	public ModelType getModelType()
	{
		return modelType;
	}

	@Override
	public void setSomeUndefinedConstants(EvaluateContext ecUndefined) throws PrismException
	{
		throw new PrismException("Constants cannot be changed for an induced model");
	}

	@Override
	public Values getConstantValues()
	{
		return origModelInfo.getConstantValues();
	}

	@Override
	public EvaluateContext getEvaluateContext()
	{
		return origModelInfo.getEvaluateContext();
	}

	@Override
	public List<Object> getActions()
	{
		return origModelInfo.getActions();
	}

	@Override
	public boolean containsUnboundedVariables()
	{
		return origModelInfo.containsUnboundedVariables();
	}

	@Override
	public List<String> getVarNames()
	{
		if (varList == null) {
			return origModelInfo.getVarNames();
		}
		return IntStream.range(0, varList.getNumVars()).mapToObj(varList::getName).collect(Collectors.toList());
	}

	@Override
	public List<Type> getVarTypes()
	{
		if (varList == null) {
			return origModelInfo.getVarTypes();
		}
		return IntStream.range(0, varList.getNumVars()).mapToObj(varList::getType).collect(Collectors.toList());
	}

	@Override
	public DeclarationType getVarDeclarationType(int i) throws PrismException
	{
		return varList == null ? origModelInfo.getVarDeclarationType(i) : varList.getDeclarationType(i);
	}

	@Override
	public int getVarModuleIndex(int i)
	{
		return varList == null ? origModelInfo.getVarModuleIndex(i) : varList.getModule(i);
	}

	@Override
	public String getModuleName(int i)
	{
		return origModelInfo.getModuleName(i);
	}

	@Override
	public VarList createVarList() throws PrismException
	{
		return varList == null ? origModelInfo.createVarList() : varList;
	}

	@Override
	public List<String> getLabelNames()
	{
		return origModelInfo.getLabelNames();
	}
}
