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

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.event.*;
import javax.swing.*;

import userinterface.util.*;

/**
 * This class gets notified through pluginChanged when the plugin in
 * focus has changed. The undo/redo actions can be implemented cleanly
 * by implementing the getUndoManager() function in GUIPlugin. Once this 
 * is done it should work automatically, including enabledness of these actions.
 * 
 * The clipboard functions paste/copy/cut/select all/delete work slightly more
 * complicated. This class listens on the focussed plugin's selectionChangeManager 
 * and calls canDoClipboardFunction on this plugin to determine enabledness. 
 * There is no need to pass changes in the clipboard state to the 
 * selectionChangeManager, as this is done automatically. Hence, only notify when 
 * a change in selection occurs.
 * 
 * All actions can be and should be used as menu items.
 */
public class GUIClipboard extends GUIPlugin
{
	/* the current GUIPlugin undoManager */
	private GUIPrism prism;
	private GUIPlugin plugin;
	private GUIUndoManager undoManager;
    private JMenu editMenu;
    private JToolBar editToolBar;
    private Action actionUndo, actionRedo, actionCut, actionCopy, actionPaste, actionDelete, actionSelectAll;
        
    /** Creates a new instance of GUIClipboard */
    public GUIClipboard(GUIPrism pr)
    {
        super(pr, false);
        this.prism = pr;
        initComponents();
        doUndoManagerEnables();
        doClipboardEnables();
        
        /* Listen to clipboard events. */
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.addFlavorListener(new FlavorListener() {
        	public void flavorsChanged(FlavorEvent e) {
        		doClipboardEnables();
        	}
        });
    }
    
    /* gets called when plugin changes. should implement this as an
     * GUIEvent really */
    public void pluginChanged(GUIPlugin plugin)
    {
    	if (plugin != null)
    		plugin.getSelectionChangeHandler().clear();
    	
    	// remove listener
		if (undoManager != null)
			undoManager.clear();
		
		this.plugin = null;
		this.undoManager = null;		
		
    	if (plugin != null)
    	{
    		this.plugin = plugin;
    		/* get notified when enabledness of clipboard actions may change */
    		this.plugin.getSelectionChangeHandler().addListener(new GUIEventListener() {
    			public boolean processGUIEvent(GUIEvent e) {
    				doClipboardEnables();
    				return true;
    			}
    		});    
    		
    		if (plugin.getUndoManager() != null)
    		{
    			undoManager = plugin.getUndoManager();
	    		
	    		/* get notified when undo history may change */
	    		undoManager.addListener(new GUIEventListener() {
	    			public boolean processGUIEvent(GUIEvent e) {
	    				if (e instanceof GUIUndoManagerEvent)
	    				{
	    					doUndoManagerEnables();
	    					return true;
	    				}
	    				
	    				return false;    					
	    			}
	    		});
    		}
    	}
    	
    	doUndoManagerEnables();
    	doClipboardEnables();
    }
    
	private void doClipboardEnables() 
	{ 
		actionCopy.setEnabled(plugin != null && plugin.canDoClipBoardAction(actionCopy));
		actionCut.setEnabled(plugin != null && plugin.canDoClipBoardAction(actionCut));
		actionPaste.setEnabled(plugin != null && plugin.canDoClipBoardAction(actionPaste));
		actionDelete.setEnabled(plugin != null && plugin.canDoClipBoardAction(actionDelete));
		actionSelectAll.setEnabled(plugin != null && plugin.canDoClipBoardAction(actionSelectAll));		
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
    
    public void doUndoManagerEnables()
    {
    	actionUndo.setEnabled(undoManager != null && undoManager.canUndo());
    	actionRedo.setEnabled(undoManager != null && undoManager.canRedo());
    }
    
    private void initComponents()
    {
        setupActions();
        editMenu = new javax.swing.JMenu();
        {
        	editMenu.setMnemonic(KeyEvent.VK_E);
            editMenu.setText("Edit");
            
            editMenu.add(new JMenuItem(actionUndo));
            editMenu.add(new JMenuItem(actionRedo));
            editMenu.add(new JSeparator());
            editMenu.add(new JMenuItem(actionCut));
            editMenu.add(new JMenuItem(actionCopy));
            editMenu.add(new JMenuItem(actionPaste));
            editMenu.add(new JMenuItem(actionDelete));
            editMenu.add(new JSeparator());
            editMenu.add(new JMenuItem(actionSelectAll));
        }
        
        editToolBar = new JToolBar();
        {
        	
    	JButton b6 = new JButton(actionUndo);
	    b6.setToolTipText("Undo");
	    b6.setText("");
	    editToolBar.add(b6);
	    
	    JButton b7 = new JButton(actionRedo);
	    b7.setToolTipText("Redo");
	    b7.setText("");
	    editToolBar.add(b7);
	    
	    //editToolBar.add(new JSeparator());
        	
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
	    
	    //editToolBar.add(new JSeparator());
	    
	    JButton b5 = new JButton(actionSelectAll);
	    b5.setToolTipText("Select all");
	    b5.setText("");
	    editToolBar.add(b5);
        }
        editToolBar.setFloatable(false);
    }
    
    /* Send GUIClipboardEvents to the focussed GUIPlugin */
    
    private void setupActions()
    {
    	actionUndo = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.UNDO, getFocussedComponent()));
            }
        };
        actionUndo.putValue(Action.LONG_DESCRIPTION, "Undo the last edit.");
        actionUndo.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_U));
        actionUndo.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        actionUndo.putValue(Action.NAME, "Undo");
        actionUndo.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallUndo.png"));
        
        actionRedo = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.REDO, getFocussedComponent()));
            }
        };
        actionRedo.putValue(Action.LONG_DESCRIPTION, "Redo the last edit.");
        actionRedo.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_R));
        actionRedo.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        actionRedo.putValue(Action.NAME, "Redo");
        actionRedo.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallRedo.png"));
       
    	
        actionCut = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.CUT, getFocussedComponent()));
            }
        };
        actionCut.putValue(Action.LONG_DESCRIPTION, "Copys the currently selected item/text to the clipboard and then removes it.");
        actionCut.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
        actionCut.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        actionCut.putValue(Action.NAME, "Cut");
        actionCut.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallCut.png"));
         
        actionCopy = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                 notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.COPY, getFocussedComponent()));
            }
        };
        actionCopy.putValue(Action.LONG_DESCRIPTION, "Copys the currently selected item/text to the clipboard.");
        actionCopy.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_O));
        actionCopy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        actionCopy.putValue(Action.NAME, "Copy");
        actionCopy.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallCopy.png"));
                
        actionPaste = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.PASTE, getFocussedComponent()));
            }
        };
        actionPaste.putValue(Action.LONG_DESCRIPTION, "Pastes the contents of the clipboard.");
        actionPaste.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_P));
        actionPaste.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        actionPaste.putValue(Action.NAME, "Paste");
        actionPaste.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPaste.png"));
              
        
        actionDelete = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.DELETE, getFocussedComponent()));
            }
        };
        actionDelete.putValue(Action.LONG_DESCRIPTION, "Removes the currently selected item");        
        actionDelete.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
        actionDelete.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_D, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        actionDelete.putValue(Action.NAME, "Delete");
        actionDelete.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.png"));
        
        actionSelectAll = new AbstractAction()
        {
            public void actionPerformed(ActionEvent e)
            {
                notifyEventListeners(new GUIClipboardEvent(GUIClipboardEvent.SELECT_ALL, getFocussedComponent()));
            }
        };
        actionSelectAll.putValue(Action.LONG_DESCRIPTION, "Selects all items of the focussed component.");
        actionSelectAll.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
        actionSelectAll.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
        actionSelectAll.putValue(Action.NAME, "Select all");
        actionSelectAll.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSelectAll.png"));     
    }
    
    public void notifySettings(prism.PrismSettings settings)
    {}

    public Action getUndoAction() {
		return actionUndo;
	}
    
	public Action getRedoAction() {
		return actionRedo;
	}

	public Action getCutAction() {
		return actionCut;
	}

	public Action getCopyAction() {
		return actionCopy;
	}

	public Action getPasteAction() {
		return actionPaste;
	}

	public Action getDeleteAction() {
		return actionDelete;
	}

	public Action getSelectAllAction() {
		return actionSelectAll;
	}

	
	
	
	
	
    
    
}
