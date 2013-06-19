//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

package userinterface.util;

import java.util.ArrayList;
import java.util.*;

/**
 * Handles the selection of PropertyOwners
 */
public abstract class SelectionModel extends Observable
{

	private ArrayList<SelectionListener> listeners;
	private ArrayList<PropertyOwner> currentSelection;

	public SelectionModel()
	{
		listeners = new ArrayList<SelectionListener>();
		currentSelection = new ArrayList<PropertyOwner>();
	}

	public void addSelectionListener(SelectionListener s)
	{
		listeners.add(s);
	}

	public void removeSelectionListener(SelectionListener s)
	{
		listeners.remove(s);
	}

	public void fireSelectionChanged()
	{
		SelectionEvent event = new SelectionEvent(getCurrentSelection());
		SelectionListener sl;

		for (int i = 0; i < listeners.size(); i++) {
			sl = listeners.get(i);
			sl.selectionPerformed(event);
		}
	}

	public ArrayList<PropertyOwner> getCurrentSelection()
	{
		return currentSelection;
	}

	public PropertyOwner getSelectedItem(int i)
	{
		return currentSelection.get(i);
	}

	public int getSelectionSize()
	{
		return currentSelection.size();
	}

	protected void addToSelection(PropertyOwner owner, boolean fireEvent)
	{
		if (!currentSelection.contains(owner))
			currentSelection.add(owner);

		if (fireEvent)
			fireSelectionChanged();
	}

	protected void removeFromSelection(PropertyOwner owner, boolean fireEvent)
	{
		if (currentSelection.contains(owner))
			currentSelection.remove(owner);

		if (fireEvent)
			fireSelectionChanged();
	}

	protected void clearSelection(boolean fireEvent)
	{
		currentSelection.clear();

		if (fireEvent)
			fireSelectionChanged();
	}

	protected void setSelection(ArrayList<PropertyOwner> a, boolean fireEvent)
	{
		currentSelection = a;

		if (fireEvent)
			fireSelectionChanged();
	}
}
