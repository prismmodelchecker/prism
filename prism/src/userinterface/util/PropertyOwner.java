//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

package userinterface.util;

import java.util.*;

public interface PropertyOwner extends Comparable<Object>
{
	// One for each type of owner
	public int getUniquePropertyID();

	// One for each owner
	// This will be displayed when only
	// this owner is being displayed.
	public String getDescriptor();

	// One for each type of property collection
	// When only one owner is being displayed,
	// we see the result of this method, and then
	// the result of getDescriptor.
	// If there is more than one owner being
	// displayed, we see the number of
	// owners, then the result of this method
	// followed by an "s".
	public String getClassDescriptor();

	public int getNumProperties();

	public SingleProperty getProperty(int index);

	public void registerObserver(Observer obs);
}
