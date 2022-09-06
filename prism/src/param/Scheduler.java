//==============================================================================
//	
//	Copyright (c) 2013-
//	Authors:
//	* Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
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

package param;

/**
 * Simple scheduler for a parametric model.
 * Assigns a single nonrandomised decision to each state of the model.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
final class Scheduler {
	/** represents the choice taken for each state */
	private int[] choices;
	
	/**
	 * Constructs a new scheduler for {@code model}.
	 * The scheduler is legal, but not necessarily optimal in any sense.
	 * 
	 * @param model model to generate scheduler for
	 */
	Scheduler(ParamModel model)
	{
		choices = new int[model.getNumStates()];
		for (int state = 0; state < model.getNumStates(); state++) {
			choices[state] = model.stateEnd(state) - 1;
		}
	}

	/** Copy constructor */
	Scheduler(Scheduler other)
	{
		choices = other.choices.clone();
	}

	@Override
	public Scheduler clone()
	{
		return new Scheduler(this);
	}

	/**
	 * Set choice for given state.
	 * 
	 * @param state state to set choice for
	 * @param choice choice to set for given state 
	 */
	void setChoice(int state, int choice) {
		choices[state] = choice;
	}
	
	/**
	 * Get choice for given state.
	 * 
	 * @param state state to get choice of
	 * @return choice for given state
	 */
	int getChoice(int state) {
		return choices[state];
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Scheduler)) {
			return false;
		}
		Scheduler other = (Scheduler) obj;
		for (int state = 0; state < choices.length; state++) {
			if (this.choices[state] != other.choices[state]) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		int hash = 0;
		
		for (int state = 0; state < choices.length; state++) {
			hash = choices[state] + (hash << 6) + (hash << 16) - hash;
		}
		
		return hash;
	}
}
