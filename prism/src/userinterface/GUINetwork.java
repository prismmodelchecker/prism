/*
 * GUINetwork.java
 *
 * Created on 25 July 2005, 20:07
 */

package userinterface;

/**
 *
 * @author  ug60axh
 */
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
