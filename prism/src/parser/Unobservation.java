//==============================================================================
//	
//	Copyright (c) 2014-
//	Authors:
//	* Xueyi Zou <xz972@york.ac.uk> (University of York)
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

package parser;

import prism.ModelInfo;
import prism.PrismLangException;

public class Unobservation extends State
{
	/**
	 * @param n
	 */
	public Unobservation(int n)
	{
		super(n);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param s
	 */
	public Unobservation(State s)
	{
		super(s);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param s1
	 * @param s2
	 */
	public Unobservation(State s1, State s2)
	{
		super(s1, s2);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param v
	 * @param mf
	 * @throws PrismLangException
	 */
	public Unobservation(Values v, ModelInfo modelInfo) throws PrismLangException
	{
		super(v, modelInfo, false);
		// TODO Auto-generated constructor stub
	}
}
