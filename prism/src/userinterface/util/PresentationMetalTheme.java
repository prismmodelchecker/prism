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

import javax.swing.plaf.*;
import javax.swing.plaf.metal.*;
import java.awt.*;

/**
 * This class overrides DefaultMetalTheme to change the fonts for components and
 * menus to a bigger size suitable for presentations.
 */
public class PresentationMetalTheme extends DefaultMetalTheme
{
    
    private FontUIResource f;
    public PresentationMetalTheme(int sizeChange)
    {
        FontUIResource fr = super.getControlTextFont();
        String st = fr.getFontName();
        ////System.out.println("st = "+st);
        st = "Dialog.PLAIN";
        
        f = new FontUIResource(st, Font.PLAIN, 11+sizeChange);
    }
    
    /** Overrides the superclass's getMenuTextFont method by returning a larger font. */    
    public FontUIResource getMenuTextFont() 
    { 
        return f;
    }
    
    /** Overrides the superclass's getControlTextFont() method to return a larger font. */    
    public FontUIResource getControlTextFont() 
    {
        return f;
    }
    
}
