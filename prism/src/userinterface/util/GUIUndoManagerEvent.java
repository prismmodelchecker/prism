//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//  * Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford)
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

import userinterface.GUIPlugin;

public class GUIUndoManagerEvent extends GUIEvent 
{
	public static final int UNDO = 0;
	public static final int REDO = 1; 
	public static final int UNDOABLE_EVENT = 2;
	public static final int DISCARD_ALL = 3;
	public static final int LIMIT_CHANGE = 4;
	public static final int DIED = 5;
	
	public GUIUndoManagerEvent(int id, GUIPlugin comp) 
	{
		super(id, comp);
	}

	public GUIUndoManagerEvent(int id) 
	{
		super(id);
	}
}
