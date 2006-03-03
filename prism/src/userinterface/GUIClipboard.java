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
    private Action actionCut, actionCopy, actionPaste, actionDelete, actionSelectAll;
    
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
            cut.setAction(actionCut);
            cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
            
            JMenuItem copy = new javax.swing.JMenuItem();
            copy.setAction(actionCopy);
            copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
            
            JMenuItem paste = new javax.swing.JMenuItem();
            paste.setAction(actionPaste);
            paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
            
            JMenuItem delete = new javax.swing.JMenuItem();
            delete.setAction(actionDelete);
            delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK));
            
            JSeparator jSeparator3 = new javax.swing.JSeparator();
            
            JMenuItem selectAll = new javax.swing.JMenuItem();
            selectAll.setAction(actionSelectAll);
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
	    JButton b1 = new JButton(actionCut);
	    b1.setToolTipText("Cut");
	    b1.setText("");
	    
	    editToolBar.add(b1);
	    
	    JButton b2 = new JButton(actionCopy);
	    b2.setToolTipText("Copy");
	    b2.setText("");
	    editToolBar.add(b2);
	    
	    JButton b3 = new JButton(actionPaste);
	    b3.setToolTipText("Paste");
	    b3.setText("");
	    editToolBar.add(b3);
	    
	    JButton b4 = new JButton(actionDelete);
	    b4.setToolTipText("Delete");
	    b4.setText("");
	    editToolBar.add(b4);
        }
        editToolBar.setFloatable(false);
    }
    
    private void setupActions()
    {
        actionCut = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                //System.out.println("cutactionperforemd");
                //System.out.println("cut");
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.CUT, getFocussedComponent()));
            }
        };
        actionCut.putValue(Action.LONG_DESCRIPTION, "Copys the currently selected item/text to the clipboard and then removes it.");
        //actionCut.putValue(Action.SHORT_DESCRIPTION, "Cut");
        actionCut.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
        actionCut.putValue(Action.NAME, "Cut");
        actionCut.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallCut.gif"));
        
        actionCopy = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                //System.out.println("copyactionperforemd");
                //System.out.println("copy");
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.COPY, getFocussedComponent()));
            }
        };
        actionCopy.putValue(Action.LONG_DESCRIPTION, "Copys the currently selected item/text to the clipboard.");
        //actionCopy.putValue(Action.SHORT_DESCRIPTION, "Copy");
        actionCopy.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
        actionCopy.putValue(Action.NAME, "Copy");
        actionCopy.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallCopy.gif"));
        
        actionPaste = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                //System.out.println("pasteactionperofemd");
                //System.out.println("paste");
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.PASTE, getFocussedComponent()));
            }
        };
        actionPaste.putValue(Action.LONG_DESCRIPTION, "Pastes the contents of the clipboard.");
        //actionPaste.putValue(Action.SHORT_DESCRIPTION, "Paste");
        actionPaste.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
        actionPaste.putValue(Action.NAME, "Paste");
        actionPaste.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPaste.gif"));
        
        actionDelete = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.DELETE, getFocussedComponent()));
            }
        };
        actionDelete.putValue(Action.LONG_DESCRIPTION, "Removes the currently selected item");
        //actionDelete.putValue(Action.SHORT_DESCRIPTION, "Delete");
        actionDelete.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
        actionDelete.putValue(Action.NAME, "Delete");
        actionDelete.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.gif"));
        
        actionSelectAll = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.SELECT_ALL, getFocussedComponent()));
            }
        };
        actionSelectAll.putValue(Action.LONG_DESCRIPTION, "Selects all items of the focussed component.");
        //actionSelectAll.putValue(Action.SHORT_DESCRIPTION, "Select All");
        actionSelectAll.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
        actionSelectAll.putValue(Action.NAME, "Select all");
        actionSelectAll.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSelectAll.gif"));
    }
    
    public OptionsPanel getOptions()
    {
        return null;
    }
    
    public void notifySettings(prism.PrismSettings settings)
    {}
    
}
