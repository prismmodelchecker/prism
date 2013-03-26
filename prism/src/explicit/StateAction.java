//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford)
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

package explicit;

/**
 * Stores a state-action entry.
 */
public final class StateAction
{
	final int state;
	final Object action;

	StateAction(int state, Object action)
	{
		this.state = state;
		this.action = action;
	}
	
	int getState()
	{
		return state;
	}
	
	Object getAction()
	{
		return action;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (!(obj instanceof StateAction)) {
			return false;
		}
		
		StateAction other = (StateAction) obj;
		if (this.state != other.state) {
			return false;
		}
		if ((this.action == null) != (other.action == null)) {
			return false;
		}
		if (this.action == null) {
			return true;
		}
		return (this.action.equals(other.action));
	}
	
	@Override
	public int hashCode()
	{
		if (null != action) {
			return state * 23 + action.hashCode();
		} else {
			return state;
		}
	}
}
