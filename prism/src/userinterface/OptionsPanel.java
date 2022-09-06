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

import javax.swing.*;
import prism.*;

public abstract class OptionsPanel extends JPanel
{
    
    private String tab;
    
    public OptionsPanel(String tab)
    {
        this.tab = tab;
    }
    
    //Applies the state of the options panel to the data stored in PrismSettings
    public abstract void apply() throws PrismException;
    
    //Sets up the gui to reflect the data stored in 
    public abstract void synchronizeGUI();
    
    //The following methods are now irrelevent:
    //      All loading/saving is now done in PrismSettings.java
    //      Default settings are handled in PrismSettings.java
    //      and validity is now handled in the apply() method
    
    //public abstract void defaultGUI();
    
    //public abstract boolean valid() throws GUIException;
    
    //public abstract Element saveXMLElement(Document doc) throws DOMException;
    
    //public abstract void loadXMLElement(Document doc, Element element);
    
    //public abstract void saveProperties(Properties properties);
    
    //public abstract void loadProperties(Properties properties);
    
    public String getTabText()
    {
        return tab;
    }
    
}
