//==============================================================================
//
//	Copyright (c) 2025-
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

import parser.VarList;
import parser.ast.DeclarationType;
import parser.type.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Simple storage of basic model info, implementing {@link ModelInfo}.
 * Stores and provides access to mutable info about
 * model type, variables, labels, etc.
 */
public class BasicModelInfo implements ModelInfo
{
	/** Model type */
	private ModelType modelType;
	/** Variable list */
	private VarList varList;
	/** Label names */
	private List<String> labelNameList;

	// Constructors

	/**
	 * Construct a {@link BasicModelInfo} with the specified model type.
	 */
	public BasicModelInfo(ModelType modelType)
	{
		this.modelType = modelType;
		varList = new VarList();
		labelNameList = new ArrayList<>();
	}

	// Setters/getters for basic model info storage

	/**
	 * Set the model type.
	 */
	public void setModelType(ModelType modelType)
	{
		this.modelType = modelType;
	}

	/**
	 * Set the {@link VarList} used to store variable info.
	 */
	public void setVarList(VarList varList)
	{
		this.varList = varList;
	}

	/**
	 * Set the list used to store label names.
	 */
	public void setLabelNameList(List<String> labelNameList)
	{
		this.labelNameList = labelNameList;
	}

	/**
	 * Get the {@link VarList} used to store variable info.
	 */
	public VarList getVarList()
	{
		return varList;
	}

	/**
	 * Get the list used to store label names.
	 */
	public List<String> getLabelNameList()
	{
		return labelNameList;
	}

	// Methods to implement ModelInfo

	@Override
	public ModelType getModelType()
	{
		return modelType;
	}

	@Override
	public List<String> getVarNames()
	{
		return IntStream.range(0, varList.getNumVars())
				.mapToObj(varList::getName)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	@Override
	public List<Type> getVarTypes()
	{
		return IntStream.range(0, varList.getNumVars())
				.mapToObj(varList::getType)
				.collect(Collectors.toCollection(ArrayList::new));
	}

	@Override
	public DeclarationType getVarDeclarationType(int i)
	{
		return varList.getDeclarationType(i);
	}

	@Override
	public List<String> getLabelNames()
	{
		return labelNameList;
	}
}
