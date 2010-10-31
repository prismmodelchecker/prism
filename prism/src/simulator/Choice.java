//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package simulator;

import parser.*;
import prism.*;

public interface Choice
{
//	public void setAction(String action);
	public int getModuleOrActionIndex();
	public String getModuleOrAction();
	public int size();
	public double getProbability();
	public double getProbability(int i);
	public double getProbabilitySum();
	public String getUpdateString(int i, State currentState) throws PrismLangException;
	public String getUpdateStringFull(int i);
	public State computeTarget(State currentState) throws PrismLangException;
	public void computeTarget(State currentState, State newState) throws PrismLangException;
	public State computeTarget(int i, State currentState) throws PrismLangException;
	public void computeTarget(int i, State currentState, State newState) throws PrismLangException;
	public int getIndexByProbabilitySum(double x);
	public void checkValid(ModelType modelType) throws PrismException;
	
	/**
	 * Check whether the transitions in this choice (from a particular state)
	 * would cause any errors, mainly variable overflows.
	 * Variable ranges are specified in the passed in VarList.
	 * Throws an exception if such an error occurs.
	 */
	public void checkForErrors(State currentState, VarList varList) throws PrismException;
}
