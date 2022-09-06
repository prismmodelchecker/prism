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
import java.util.*;
import userinterface.*;

public class GUIEventHandler
{
	public GUIPrism gui;
	public ArrayList<GUIEventListener> listeners;
	
	/** Creates a new instance of GUIEventHandler */
	public GUIEventHandler(GUIPrism gui)
	{
		this.gui = gui;
		listeners = new ArrayList<GUIEventListener>();
	}
	
	public void addListener(GUIEventListener listen)
	{
		listeners.add(listen);
	}
	
	public void notifyListeners(GUIEvent e)
	{
		boolean res = false;
		// notify plugins
		for(int i = 0; i < listeners.size(); i++)
		{
			GUIEventListener lis = listeners.get(i);
			res = lis.processGUIEvent(e);
			if (res) break;
		}
		// notify gui itself
		if (gui != null && !res) gui.processGUIEvent(e);
	}
	
	public boolean removeListener(GUIEventListener listen)
	{
		return listeners.remove(listen);
	}
	
	public void clear()
	{
		listeners.clear();
	}
}
