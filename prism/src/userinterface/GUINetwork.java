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

package userinterface;

public class GUINetwork extends GUIPlugin
{
    private GUINetworkOptions options;
    
    /** Creates a new instance of GUINetwork */
    public GUINetwork(GUIPrism gui)
    {
        super(gui, true);
        
        options = new GUINetworkOptions();
    }
    
    
    
    //PLUGIN INTERFACE METHODS
    
    public boolean displaysTab()
    {
        return false;
    }
    
    public javax.swing.JMenu getMenu()
    {
        return null;
    }
    
    public OptionsPanel getOptions()
    {
        return options;
    }
    
    public String getTabText()
    {
        return "";
    }
    
    public javax.swing.JToolBar getToolBar()
    {
        return null;
    }
    
    public String getXMLIDTag()
    {
        return "";
    }
    
    public Object getXMLSaveTree()
    {
        return null;
    }
    
    public void loadXML(Object c)
    {
    }
    
    public boolean processGUIEvent(userinterface.util.GUIEvent e)
    {
        return false;
    }
    
    public void takeCLArgs(String[] args)
    {
    }
    
    public void notifySettings(prism.PrismSettings settings)
    {}
    
}
