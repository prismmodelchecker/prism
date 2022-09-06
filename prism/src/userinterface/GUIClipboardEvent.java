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

package userinterface;

import java.awt.*;

import userinterface.util.*;

public class GUIClipboardEvent extends GUIEvent
{
    public static final int COPY = 0;
    public static final int CUT = 1;
    public static final int PASTE = 2;
    public static final int DELETE = 3;
    public static final int SELECT_ALL = 4;
    public static final int UNDO = 5;
    public static final int REDO = 6;    
    
    static int counter =0;
    /** Creates a new instance of GUIClipboardEvent */
    public GUIClipboardEvent(int id, GUIPlugin comp)
    {
        super(id, comp);
        counter++;
        //System.out.println("clipboard event "+counter);
    }
    
    public Component getComponent()
    {
        return (GUIPlugin)getData();
    }
}
