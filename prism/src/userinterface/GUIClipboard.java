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

package userinterface;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import userinterface.util.*;
/**
 *
 * @author  ug60axh
 */
public class GUIClipboard extends GUIPlugin
{
    private JMenu editMenu;
    private JToolBar editToolBar;
    private Action menuActionCut, menuActionCopy, menuActionPaste, menuActionDelete, menuActionSelectAll;
    private Action toolbarActionCut, toolbarActionCopy, toolbarActionPaste, toolbarActionDelete, toolbarActionSelectAll;
    
    
    /** Creates a new instance of GUIClipboard */
    public GUIClipboard(GUIPrism pr)
    {
        super(pr, false);
        initComponents();
    }
    
	public void takeCLArgs(String args[])
	{
	}
    
    public boolean displaysTab()
    {
        return false;
    }
    
    public javax.swing.JMenu getMenu()
    {
        return editMenu;
    }
    
    public String getTabText()
    {
        return "";
    }
    
    public javax.swing.JToolBar getToolBar()
    {
        return editToolBar;
    }
    
    public String getXMLIDTag()
    {
        return null;
    }
    
    public Object getXMLSaveTree()
    {
        return null;
    }
    
    public void loadXML(Object c)
    {
    }
    
    public boolean processGUIEvent(GUIEvent e)
    {
		return false;
    }
    
    private void initComponents()
    {
        setupActions();
        editMenu = new javax.swing.JMenu();
        {
            JMenuItem cut = new javax.swing.JMenuItem();
            cut.setAction(menuActionCut);
            cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
            
            JMenuItem copy = new javax.swing.JMenuItem();
            copy.setAction(menuActionCopy);
            copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
            
            JMenuItem paste = new javax.swing.JMenuItem();
            paste.setAction(menuActionPaste);
            paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
            
            JMenuItem delete = new javax.swing.JMenuItem();
            delete.setAction(menuActionDelete);
            delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
            
            JSeparator jSeparator3 = new javax.swing.JSeparator();
            
            JMenuItem selectAll = new javax.swing.JMenuItem();
            selectAll.setAction(menuActionSelectAll);
            selectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_MASK));
            
            editMenu.setMnemonic(KeyEvent.VK_E);
            editMenu.setText("Edit");
            editMenu.add(cut);
            editMenu.add(copy);
            editMenu.add(paste);
            editMenu.add(delete);
            editMenu.add(jSeparator3);
            editMenu.add(selectAll);
        }
        
        editToolBar = new JToolBar();
        {
	    JButton b1 = new JButton(toolbarActionCut);
	    b1.setToolTipText("Cut");
	    b1.setText("");
	    
	    editToolBar.add(b1);
	    
	    JButton b2 = new JButton(toolbarActionCopy);
	    b2.setToolTipText("Copy");
	    b2.setText("");
	    editToolBar.add(b2);
	    
	    JButton b3 = new JButton(toolbarActionPaste);
	    b3.setToolTipText("Paste");
	    b3.setText("");
	    editToolBar.add(b3);
	    
	    JButton b4 = new JButton(toolbarActionDelete);
	    b4.setToolTipText("Delete");
	    b4.setText("");
	    editToolBar.add(b4);
        }
        editToolBar.setFloatable(false);
    }
    
    private void setupActions()
    {
        menuActionCut = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.CUT, getFocussedComponent()));
            }
        };
        menuActionCut.putValue(Action.LONG_DESCRIPTION, "Copys the currently selected item/text to the clipboard and then removes it.");
        //actionCut.putValue(Action.SHORT_DESCRIPTION, "Cut");
        menuActionCut.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
        menuActionCut.putValue(Action.NAME, "Cut");
        menuActionCut.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallCut.gif"));
        
        toolbarActionCut = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.CUT, getFocussedComponent()));
            }
        };
        toolbarActionCut.putValue(Action.LONG_DESCRIPTION, "Copys the currently selected item/text to the clipboard and then removes it.");
        toolbarActionCut.putValue(Action.NAME, "Cut");
        toolbarActionCut.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallCut.gif"));
        
        
        
        menuActionCopy = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                 notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.COPY, getFocussedComponent()));
            }
        };
        menuActionCopy.putValue(Action.LONG_DESCRIPTION, "Copys the currently selected item/text to the clipboard.");
        menuActionCopy.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
        menuActionCopy.putValue(Action.NAME, "Copy");
        menuActionCopy.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallCopy.gif"));
        
        toolbarActionCopy = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                 notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.COPY, getFocussedComponent()));
            }
        };
        toolbarActionCopy.putValue(Action.LONG_DESCRIPTION, "Copys the currently selected item/text to the clipboard.");
        toolbarActionCopy.putValue(Action.NAME, "Copy");
        toolbarActionCopy.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallCopy.gif"));
        
        menuActionPaste = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.PASTE, getFocussedComponent()));
            }
        };
        menuActionPaste.putValue(Action.LONG_DESCRIPTION, "Pastes the contents of the clipboard.");
        menuActionPaste.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
        menuActionPaste.putValue(Action.NAME, "Paste");
        menuActionPaste.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPaste.gif"));
        
        toolbarActionPaste = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.PASTE, getFocussedComponent()));
            }
        };
        toolbarActionPaste.putValue(Action.LONG_DESCRIPTION, "Pastes the contents of the clipboard.");
        toolbarActionPaste.putValue(Action.NAME, "Paste");
        toolbarActionPaste.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPaste.gif"));
        
        menuActionDelete = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.DELETE, getFocussedComponent()));
            }
        };
        menuActionDelete.putValue(Action.LONG_DESCRIPTION, "Removes the currently selected item");        
        menuActionDelete.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
        menuActionDelete.putValue(Action.NAME, "Delete");
        menuActionDelete.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.gif"));
        
        toolbarActionDelete = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.DELETE, getFocussedComponent()));
            }
        };
        toolbarActionDelete.putValue(Action.LONG_DESCRIPTION, "Removes the currently selected item");        
        toolbarActionDelete.putValue(Action.NAME, "Delete");
        toolbarActionDelete.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.gif"));
        
        menuActionSelectAll = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.SELECT_ALL, getFocussedComponent()));
            }
        };
        menuActionSelectAll.putValue(Action.LONG_DESCRIPTION, "Selects all items of the focussed component.");
        menuActionSelectAll.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
        menuActionSelectAll.putValue(Action.NAME, "Select all");
        menuActionSelectAll.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSelectAll.gif"));
        
        toolbarActionSelectAll = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.SELECT_ALL, getFocussedComponent()));
            }
        };
        toolbarActionSelectAll.putValue(Action.LONG_DESCRIPTION, "Selects all items of the focussed component.");
        toolbarActionSelectAll.putValue(Action.NAME, "Select all");
        toolbarActionSelectAll.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSelectAll.gif"));
    }
    
    public OptionsPanel getOptions()
    {
        return null;
    }
    
    public void notifySettings(prism.PrismSettings settings)
    {}
    
}
