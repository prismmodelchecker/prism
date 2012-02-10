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

package userinterface.log;

import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import userinterface.*;
import prism.PrismLog;
import userinterface.util.*;
import prism.*;

@SuppressWarnings("serial")
public class GUILog extends GUIPlugin implements MouseListener, PrismSettingsListener
{
	private PrismLog theLog;
	private JTextArea text;
	private JPopupMenu popupMenu;
	private JMenu logMenu;
	
	//private GUILogOptions options;
	private GUIPrismFileFilter textFilter[];
	private Action clearAction, saveAction;
	
	/** Creates a new instance of GUILog */
	public GUILog(GUIPrism pr)
	{
		super(pr, true);
		theLog = pr.getLog();
		if(theLog instanceof GUIWindowLog)
		{
			GUIWindowLog win = (GUIWindowLog)theLog;
			initComponentsAsWindowLog(win);
		}
		else if(theLog instanceof GUIVisualLogModel)
		{
			GUIVisualLogModel vis = (GUIVisualLogModel)theLog;
			initComponentsAsVisualLog(vis);
		}
		else
		{
			//other types of log handles here
		}
	}
	
	public void takeCLArgs(String args[])
	{
	}
	
	public boolean displaysTab()
	{
		return true;
	}
	
	public javax.swing.JMenu getMenu()
	{
		return logMenu;
	}
	
	public String getTabText()
	{
		return "Log";
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
	
	public boolean processGUIEvent(GUIEvent e)
	{
		if(e instanceof GUILogEvent)
		{
			GUILogEvent le = (GUILogEvent)e;
			if(le.getID() == GUILogEvent.PRINTLN)
			{
				theLog.println(le.getData());
			}
			else if(le.getID() == GUILogEvent.PRINT)
			{
				theLog.print(le.getData());
			}
			else if(le.getID() == GUILogEvent.PRINTSEPARATOR)
			{
				theLog.printSeparator();
			}
			else if(le.getID() == GUILogEvent.PRINTWARNING)
			{
				theLog.printWarning((String) le.getData());
			}
		}
		else if (e instanceof GUIClipboardEvent && super.getGUI().getFocussedPlugin() == this)
		{
			GUIClipboardEvent ce = (GUIClipboardEvent)e;
			if(ce.getID() == GUIClipboardEvent.COPY)
			{
				((GUIWindowLog)theLog).copy();
				return true;
			}
			else if(ce.getID() == GUIClipboardEvent.SELECT_ALL)
			{
				((GUIWindowLog)theLog).selectAll();
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean canDoClipBoardAction(Action action) 
	{
		if (action == GUIPrism.getClipboardPlugin().getCopyAction())
		{
			return ((GUIWindowLog)theLog).hasSelectedText();
		}
		else if (action == GUIPrism.getClipboardPlugin().getSelectAllAction())
		{
			return true;
		}
		return false;
	}

	private void initComponentsAsWindowLog(GUIWindowLog log)
	{
		JScrollPane logScroller = new JScrollPane();
		{
			text = new javax.swing.JTextArea();
			text.addMouseListener(this);
			text.setFont(new Font("monospaced", Font.PLAIN, 12));
			text.setBackground(Color.lightGray);
			log.open(text, this);
			text.addMouseListener(this);
			text.setEditable(false);
			text.setBorder(new javax.swing.border.TitledBorder("Log Output"));
			
			logScroller.add(text);
			logScroller.setViewportView(text);
		}
		setLayout(new BorderLayout());
		add(logScroller, BorderLayout.CENTER);
		
		popupMenu = new JPopupMenu();
		logMenu = new JMenu("Log");
		
		
		clearAction = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				((GUIWindowLog)theLog).clear();
			}
		};
		clearAction.putValue(Action.SHORT_DESCRIPTION, "Clear log");
		clearAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_C));
		clearAction.putValue(Action.NAME, "Clear log");
		clearAction.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.png"));
		
		saveAction = new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (showSaveFileDialog(textFilter, textFilter[0]) == JFileChooser.APPROVE_OPTION)
				{
					File file = getChooserFile();
					// do save...
					try
					{
						PrintWriter out = new PrintWriter(new FileWriter(file));
						out.print(text.getText());
						out.flush();
						out.close();
					}
					catch(IOException ex)
					{
						error("Could not save to file \"" + file + "\"");
						return;
					}
				}
			}
		};
		saveAction.putValue(Action.SHORT_DESCRIPTION, "Save log as...");
		saveAction.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_S));
		saveAction.putValue(Action.NAME, "Save log as...");
		saveAction.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallSave.png"));
		
		
		//popupMenu.add(new JSeparator());
		popupMenu.add(saveAction);
		popupMenu.add(new JSeparator());
		popupMenu.add(GUIPrism.getClipboardPlugin().getCopyAction());
		popupMenu.add(clearAction);
		popupMenu.add(new JSeparator());
		popupMenu.add(GUIPrism.getClipboardPlugin().getSelectAllAction());
		
		
		logMenu.setMnemonic('L');
		logMenu.add(saveAction);
		logMenu.add(new JSeparator());
		logMenu.add(clearAction);
		
		textFilter = new GUIPrismFileFilter[1];
		textFilter[0] = new GUIPrismFileFilter("Plain text files (*.txt)");
		textFilter[0].addExtension("txt");
	}
	
	private void initComponentsAsVisualLog(GUIVisualLogModel log)
	{
		GUIVisualLogger logger = new GUIVisualLogger(log);
		setLayout(new BorderLayout());
		add(logger,BorderLayout.CENTER);
	}
	
	public void mouseClicked(MouseEvent e)
	{
	}
	
	public void mouseEntered(MouseEvent e)
	{
	}
	
	public void mouseExited(MouseEvent e)
	{
	}
	
	public void mousePressed(MouseEvent e)
	{
		if(e.isPopupTrigger())
		{
			if(e.getSource() == text)
			{
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
	
	public void mouseReleased(MouseEvent e)
	{
		if(e.isPopupTrigger())
		{
			if(e.getSource() == text)
			{
				popupMenu.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
	
	public OptionsPanel getOptions()
	{
		return null;
	}
	
	public void notifySettings(PrismSettings settings)
	{
		if(theLog instanceof GUIWindowLog)
			((GUIWindowLog)theLog).notifySettings(settings);
	}
}
