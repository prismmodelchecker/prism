//==============================================================================
//
//	Copyright (c) 2002-2004, Andrew Hinton, Dave Parker
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

package chart;

import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.io.*;
import java.util.*;

import javax.swing.colorchooser.*;
import javax.swing.event.*;

/**
 *
 * @author  Andrew
 */
public class ChartColorChooser 
{
    public static Color showDialog(Component component, String title, Color initialColor) throws HeadlessException 
    {
        JColorChooser pane = new JColorChooser(initialColor != null?
                                               initialColor : Color.white);
        
        //ColorTracker ok = new ColorTracker(pane);
        
        AbstractColorChooserPanel[] pans = pane.getChooserPanels();
        AbstractColorChooserPanel[] newpans = { new PrismColorChooserPanel(), pans[1], pans[2] };
        pane.setChooserPanels(newpans);
        
        JDialog dialog = JColorChooser.createDialog(component, title, true, pane, null, null);
        
        dialog.show(); // blocks until user brings dialog down...

        Color c = pane.getColor();
        return c;
    }
}
