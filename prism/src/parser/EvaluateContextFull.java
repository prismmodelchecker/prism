//==============================================================================
//	
//	Copyright (c) 2022-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ox.ac.uk> (University of Birmingham)
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

/**
 * Information required to evaluate an expression,
 * supporting all aspects defined in {@link EvaluateContext}.
 */
public class EvaluateContextFull extends EvaluateContextState
{
	/**
	 * Observable values
	 */
	private Object[] obsValues;

	/**
	 * Constructor
	 */
	public EvaluateContextFull(State state, State obs)
	{
		super(state);
		if (obs != null) {
			setObservation(obs);
		}
	}

	/**
	 * Set the observable values. The array of observable values is extracted and stored, not copied.
	 * Returns a copy of this EvaluateContext to allow chaining of method calls.
	 */
	public EvaluateContext setObservation(State obs)
	{
		this.obsValues = obs.varValues;
		return this;
	}

	@Override
	public Object getObservableValue(String name, int index)
	{
		// There is no observable name info available,
		// so use index if provided; otherwise unknown
		return (index == -1 || obsValues == null) ? null : obsValues[index];
	}
}
