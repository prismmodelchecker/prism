//==============================================================================
//	
//	Copyright (c) 2016-
//	Authors:
//	* Joachim Klein <klein@tcs.inf.tu-dresden.de> (TU Dresden)
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

package explicit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import prism.PrismComponent;

/**
 * A class for storing and accessing the incoming choices of an explicit NondetModel.
 * This class can be seen as providing more detailed information than PredecessorRelation,
 * as that class only stores information about the states and not the choices linking them.
 * <p>
 * As NondetModel only provide easy access to successors of states,
 * the predecessor relation is computed and stored for subsequent efficient access.
 * <p>
 * Note: Naturally, if the NondetModel changes, the predecessor relation
 * has to be recomputed to remain accurate.
 */
public class IncomingChoiceRelation
{
	/** An outgoing choice from a state, i.e., the source state and the choice index */
	public static final class Choice
	{
		/** the source state*/
		private int state;
		/** the choice index */
		private int choice;

		/** Constructor */
		public Choice(int state, int choice)
		{
			this.state = state;
			this.choice = choice;
		}

		/** The source state of this choice */
		public int getState()
		{
			return state;
		}

		/** The choice index of this choice */
		public int getChoice()
		{
			return choice;
		}

		@Override
		public String toString()
		{
			return "("+state+","+choice+")";
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + choice;
			result = prime * result + state;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Choice other = (Choice) obj;
			if (choice != other.choice)
				return false;
			if (state != other.state)
				return false;
			return true;
		}

	};

	/**
	 * pre[i] provides the list of incoming choices of the state with index i.
	 */
	List<ArrayList<Choice>> pre;

	/**
	 * Constructor. Computes the predecessor relation for the given model
	 * by considering the successors of each state.
	 *
	 * @param model the Model
	 */
	public IncomingChoiceRelation(NondetModel model)
	{
		pre = new ArrayList<ArrayList<Choice>>(model.getNumStates());
		// construct the (empty) array list for all states
		for (int s = 0; s < model.getNumStates(); s++) {
			pre.add(s, new ArrayList<Choice>());
		}

		compute(model);
	}

	/** Compute the predecessor relation using getSuccessorsIterator. */
	private void compute(NondetModel model)
	{
		int n = model.getNumStates();

		for (int s = 0; s < n; s++) {
			for (int c = 0, m = model.getNumChoices(s); c < m; c++) {
				Choice choice = new Choice(s, c);

				Iterator<Integer> it = model.getSuccessorsIterator(s, c);
				while (it.hasNext()) {
					int successor = it.next();

					// Add the current choice s to pre[successor].
					pre.get(successor).add(choice);
				}
			}
		}
	}

	/**
	 * Get an Iterable over the incoming choices of state {@code s}.
	 */
	public Iterable<Choice> getIncomingChoices(int s)
	{
		return pre.get(s);
	}

	/**
	 * Get an Iterator over the incoming choices of state {@code s}.
	 */
	public Iterator<Choice> getIncomingChoicesIterator(int s)
	{
		return getIncomingChoices(s).iterator();
	}

	/**
	 * Static constructor to compute the incoming choices information for the given model.
	 * Logs diagnostic information to the log of the given PrismComponent.
	 *
	 * @param parent a PrismComponent (for obtaining the log and settings)
	 * @param model the non-deterministic model for which the predecessor relation should be computed
	 * @returns the incoming choices information
	 **/
	public static IncomingChoiceRelation forModel(PrismComponent parent, NondetModel model)
	{
		long timer = System.currentTimeMillis();

		parent.getLog().print("Calculating incoming choices relation for "+model.getModelType().fullName()+"...  ");
		parent.getLog().flush();

		IncomingChoiceRelation pre = new IncomingChoiceRelation(model);

		timer = System.currentTimeMillis() - timer;
		parent.getLog().println("done (" + timer / 1000.0 + " seconds)");

		return pre;
	}

}
