//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
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

import java.util.Vector;

import jdd.*;
import parser.*;

/*
 * Class to store a PRISM model which is a CTMC
 */
public class StochModel extends ProbModel
{
	// accessor methods

	public int getType()
	{
		return Model.CTMC;
	}

	public String getTypeString()
	{
		return "Stochastic (CTMC)"; // TODO: Change this after regression testing
		//return "CTMC";
	}

	public String getTransName()
	{
		return "Rate matrix"; 
	}
	
	public String getTransSymbol()
	{
		return "R";
	}
	
	// constructor

	public StochModel(JDDNode tr, JDDNode tr01, JDDNode s, JDDNode r, JDDNode dl, JDDNode sr[], JDDNode trr[],
			String rsn[], JDDVars arv, JDDVars acv, Vector ddvn, int nm, String[] mn, JDDVars[] mrv, JDDVars[] mcv,
			int nv, VarList vl, JDDVars[] vrv, JDDVars[] vcv, Values cv)
	{
		super(tr, tr01, s, r, dl, sr, trr, rsn, arv, acv, ddvn, nm, mn, mrv, mcv, nv, vl, vrv, vcv, cv);
	}
}
