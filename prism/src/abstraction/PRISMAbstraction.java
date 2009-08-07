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

package abstraction;

public abstract class PRISMAbstraction
{
	public String filename;

	/* Configure based on command-line arguments */
	abstract public String modelArgs(String arg);

	/* Return string listing abstract variable names */
	abstract protected String abstractVarsString();

	/* Convert concrete to concrete state to abstract state */
	abstract protected AbstractState concreteToAbstract(int vars[]);

	/* Identifies (concrete) target states for reachability */
	abstract protected boolean isTargetConcreteState(int vars[]);

	/* Identifies (single) (concrete) initial state */
	abstract protected boolean isInitialConcreteState(int vars[]);

	/* Identifies (abstract) initial state (for which results will be shown) */
	abstract protected AbstractState getInitialAbstractState();

	/* If required, process result (e.g. to subtract from one) */

	public double processResult(int property, double res)
	{
		return res;
	}

	/* Compute min probs? (default is "yes", i.e. true) */

	public boolean computeMinProbs()
	{
		return true;
	}

	/* Compute min expected costs? (default is "no", i.e. false) */

	public boolean computeMinExp()
	{
		return false;
	}
}
