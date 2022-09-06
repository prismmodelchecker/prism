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
 * A state value which can be represented as a truth value.
 * The truth value the object represents cannot be changed once this object
 * is created.
 * 
 * @author Ernst Moritz Hahn <emhahn@cs.ox.ac.uk> (University of Oxford)
 */
final class StateBoolean extends StateValue {
	private boolean value;

	public StateBoolean()
	{
		value = new Boolean(false);
	}
	
	public StateBoolean(boolean value)
	{
		this.value = value;
	}
	
	boolean getValue()
	{
		return value;
	}
	
	@Override
	public String toString()
	{
		return value ? "true" : "false";
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof StateBoolean)) {
			return false;
		}
		return this.value == ((StateBoolean) obj).value;
	}
	
	@Override
	public int hashCode() {
		return value ? 17 : 13;
	}
}
