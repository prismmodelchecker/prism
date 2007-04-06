//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
//	* Dave Parker <dxp@cs.bham.uc.uk> (University of Birmingham)
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

package chart;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
//import userinterface.util.*;
import java.awt.datatransfer.*;
import javax.swing.event.*;
import settings.*;


/**
 *
 * @author  ug60axh
 */
public class GraphListEditor extends JPanel
{
    //The data model
    private GraphList list;
    private AbstractTableModel tableModel;
    private JTable theTable;
    
    private MultiGraphModel.AxisOwner xAxis, yAxis; //must keep track of these to maintain correct table headers.
    
    
    /** Creates a new instance of GraphListEditor */
    public GraphListEditor(GraphList list, MultiGraphModel.AxisOwner xAxis, MultiGraphModel.AxisOwner yAxis)
    {
	this.list = list;
	this.xAxis = xAxis;
	this.yAxis = yAxis;
	
	initComponents();
    }
    
    public void stopEditing()
    {
	if(theTable.getCellEditor() != null) theTable.getCellEditor().stopCellEditing();
    }
    
    private void initComponents()
    {
	setLayout(new BorderLayout());
	
	tableModel = new AbstractTableModel()
	{
	    public Object getValueAt(int i, int j)
	    {
		if(i < list.size())
		{
		    GraphPoint gp = (GraphPoint)list.get(i);
		    switch(j)
		    {
			case 0:
			{
			    double d = gp.getXCoord();
			    if(d == GraphPoint.NULL_COORD) return "";
			    else return ""+d;
			}
			default:
			{
			    double d = gp.getYCoord();
			    if(d == GraphPoint.NULL_COORD) return "";
			    else return ""+d;
			}
		    }
		}
		else
		{
		    return "";
		}
	    }
	    
	    public int getRowCount()
	    {
		return list.size() + 1000;
	    }
	    
	    public int getColumnCount()
	    {
		return 2; //or if we implement 3D list.getNumberOfDimensions();
	    }
	    
	    public String getColumnName(int i)
	    {
		switch(i)
		{
		    case 0: return xAxis.getSetting(0).getValue().toString();
		    default: return yAxis.getSetting(0).getValue().toString();
		}
	    }
	    
	    public boolean isCellEditable(int i, int j)
	    {
		return true;
	    }
	    
	    public void setValueAt(Object obj, int i, int j)
	    {
		double value = 0.0;
		try
		{
		    if(obj.toString().equals(""))
		    {
			value = GraphPoint.NULL_COORD;
		    }
		    else
			value = Double.parseDouble(obj.toString());
		}
		catch(NumberFormatException e)
		{
		    if(obj.toString().equals(""))
		    {
			value = GraphPoint.NULL_COORD;
		    }
		    return;
		}
		if(i < list.size())
		{
		    GraphPoint gp = (GraphPoint)list.get(i);
		    switch(j)
		    {
			case 0: gp.setXCoord(value);break;
			default: gp.setYCoord(value);
		    }
		}
		else
		{
		    //create new points first up to this position
		    while(list.size() <= (i))
		    {
			list.add(new GraphPoint(list.getTheModel()));
		    }
		    GraphPoint gp = (GraphPoint)list.get(i);
		    switch(j)
		    {
			case 0: gp.setXCoord(value);break;
			default: gp.setYCoord(value);
		    }
		    
		    return;
		}
		try
		{
		    if(!blockDoChange) list.doChange();
		}
		catch(SettingException e)
		{
		    list.getTheModel().gop.errorDialog(e.getMessage(), "Setting Error");
		}
	    }
	};
	
	theTable = new JTable(tableModel);
	
	//Next 3 lines thanks to
	//http://forum.java.sun.com/thread.jsp?thread=529548&forum=57&message=2546795
	//This is to disable to automatic Ctrl-C put onto JTables, as it does
	//not provide the correct functionality.
	InputMap im = theTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
	KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.CTRL_MASK);
	im.put(ks, "none");
	
	theTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
	theTable.setColumnSelectionAllowed(true);
	
	JScrollPane scroll = new JScrollPane();
	scroll.setViewportView(theTable);
	
	JToolBar pan = new JToolBar();
	pan.setFloatable(false);
	JButton sortX = new JButton();
	JButton sortY = new JButton();
	
	AbstractAction cut = new AbstractAction()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		cut();
		tableModel.fireTableDataChanged();
		try
		{
		    list.doChange();
		}
		catch(SettingException ee)
		{
		    list.getTheModel().gop.errorDialog(ee.getMessage(), "Setting Error");
		}
	    }
	};
	cut.putValue(Action.LONG_DESCRIPTION, "Cut the current selection to the clipboard");
	//exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit");
	cut.putValue(Action.NAME, "Cut");
	cut.putValue(Action.SMALL_ICON, new ImageIcon(ClassLoader.getSystemResource("images/smallCut.gif")));
	cut.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_MASK));
	
	AbstractAction copy = new AbstractAction()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		copy();
	    }
	};
	copy.putValue(Action.LONG_DESCRIPTION, "Copies the current selection to the clipboard");
	//exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit");
	copy.putValue(Action.NAME, "Copy");
	copy.putValue(Action.SMALL_ICON, new ImageIcon(ClassLoader.getSystemResource("images/smallCopy.gif")));
	copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK));
	
	AbstractAction paste = new AbstractAction()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		paste();
		tableModel.fireTableDataChanged();
		try
		{
		    list.doChange();
		}
		catch(SettingException ee)
		{
		    list.getTheModel().gop.errorDialog(ee.getMessage(), "Setting Error");
		}
	    }
	};
	paste.putValue(Action.LONG_DESCRIPTION, "Pastes the clipboard to the current selection");
	//exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit");
	paste.putValue(Action.NAME, "Paste");
	paste.putValue(Action.SMALL_ICON, new ImageIcon(ClassLoader.getSystemResource("images/smallPaste.gif")));
	paste.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_MASK));
	
	
	JButton delete = new JButton();
	
	sortX.setIcon(new ImageIcon(ClassLoader.getSystemResource("images/smallSortX.gif")));
	sortY.setIcon(new ImageIcon(ClassLoader.getSystemResource("images/smallSortY.gif")));
	delete.setIcon(new ImageIcon(ClassLoader.getSystemResource("images/smallDelete.gif")));
	
	
	
	sortX.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		list.sortPoints(0);
		tableModel.fireTableDataChanged();
		try
		{
		    list.doChange();
		}
		catch(SettingException ee)
		{
		    list.getTheModel().gop.errorDialog(ee.getMessage(), "Setting Error");
		}
	    }
	});
	sortX.setToolTipText("Sort by X-Axis");
	
	sortY.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		list.sortPoints(1);
		tableModel.fireTableDataChanged();
		try
		{
		    list.doChange();
		}
		catch(SettingException ee)
		{
		    list.getTheModel().gop.errorDialog(ee.getMessage(), "Setting Error");
		}
	    }
	});
	sortY.setToolTipText("Sort by Y-Axis");
	
	
	
	delete.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		delete();
		tableModel.fireTableDataChanged();
		try
		{
		    list.doChange();
		}
		catch(SettingException ee)
		{
		    list.getTheModel().gop.errorDialog(ee.getMessage(), "Setting Error");
		}
	    }
	});
	delete.setToolTipText("Delete");
	
	
	
	pan.add(cut);
	pan.add(copy);
	pan.add(paste);
	pan.add(delete);
	pan.addSeparator();
	pan.add(sortX);
	pan.add(sortY);
	
	
	add(scroll, BorderLayout.CENTER);
	add(pan, BorderLayout.NORTH);
    }
    
    public void cut()
    {
	copy();
	delete();
    }
    
    public void copy()
    {
	int cc = theTable.getSelectedColumnCount();
	int[] cs = theTable.getSelectedColumns();
	int[] rs = theTable.getSelectedRows();
	if(cc <= 0) return;
	else if(cc == 1)
	{
	    StringBuffer clippy = new StringBuffer("");
	    for(int i = 0; i< rs.length ; i++)
	    {
		GraphPoint p = list.getPoint(rs[i]);
		if(cs[0] == 0)
		    clippy.append(p.getXCoord()+"\n");
		else
		    clippy.append(p.getYCoord()+"\n");
	    }
	    String str = clippy.toString();
	    
	    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    StringSelection gs = new StringSelection(str);
	    clipboard.setContents(gs, null);
	}
	else //both are selected
	{
	    StringBuffer clippy = new StringBuffer("");
	    for(int i = 0; i< rs.length ; i++)
	    {
		GraphPoint p = list.getPoint(rs[i]);
		
		clippy.append(p.getXCoord()+"\t"+p.getYCoord()+"\n");
	    }
	    String str = clippy.toString();
	    
	    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	    StringSelection gs = new StringSelection(str);
	    clipboard.setContents(gs, null);
	}
    }
    
    public void paste()
    {
	Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	Transferable contents = clipboard.getContents(null);
	
	try
	{
	    if(contents.isDataFlavorSupported(DataFlavor.stringFlavor))
	    {
		String str = (String)contents.getTransferData(DataFlavor.stringFlavor);
		
		StringTokenizer rows = new StringTokenizer(str, "\n");
		
		int currCol = theTable.getSelectedColumn();
		int currRow = theTable.getSelectedRow();
		
		while(rows.hasMoreTokens())
		{
		    StringTokenizer columns = new StringTokenizer(rows.nextToken());
		    if(columns.countTokens() == 1)
		    {
			tableModel.setValueAt(columns.nextToken(), currRow, currCol);
		    }
		    else if(columns.countTokens() > 1)//2 or more
		    {
			if(currCol == 0)
			{
			    tableModel.setValueAt(columns.nextToken(), currRow, 0);
			    
			    tableModel.setValueAt(columns.nextToken(), currRow, 1);
			}
			else if(currCol == 1)
			{
			    tableModel.setValueAt(columns.nextToken(), currRow, 1);
			}
		    }
		    currRow++;
		}
	    }
	}
	catch(Exception e)
	{
	    
	}
    }
    
    private boolean blockDoChange = false;
    
    public void delete()
    {
	int[] selectedCols = theTable.getSelectedColumns();
	int[] selectedRows = theTable.getSelectedRows();
	
	blockDoChange = true;
	for(int i = 0; i < selectedRows.length; i++)
	    for(int j = 0; j < selectedCols.length; j++)
	    {
	    if(selectedRows[i] < tableModel.getRowCount() && selectedCols[j] < 2)
		tableModel.setValueAt("", selectedRows[i], selectedCols[j]);
	    }
	blockDoChange = false;
	try
	{
	    list.doChange();
	}
	catch(SettingException ee)
	{
	    list.getTheModel().gop.errorDialog(ee.getMessage(), "Setting Error");
	}
	
    }
    
    private JDialog d;
    
    public void showEditor(JFrame parent)
    {
	d = new JDialog(parent, "Edit Series: "+list.getGraphTitle(), true);
	
	d.getContentPane().add(this);
	d.getContentPane().setSize(200,200);
	
	JPanel closePanel = new JPanel();
	JButton closeWindow = new JButton("Close");
	closeWindow.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		stopEditing();
		if(d != null)d.setVisible(false);
	    }
	});
	closePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
	closePanel.add(closeWindow);
	
	d.getContentPane().add(closePanel, BorderLayout.SOUTH);
	
	d.pack();
	d.setLocationRelativeTo(parent); // centre
	d.show();
    }
    
    public static void showEditors(JFrame parent, ArrayList editors)
    {
	JDialog sd;
	
	if(editors.size() <= 0) return; //failsafe.
	else if(editors.size() == 1)
	{
	    GraphListEditor gle = (editors.get(0) instanceof SeriesDataSetting) ?
		((SeriesDataSetting)editors.get(0)).getGraphListValue().getEditor() :
		((GraphListEditor)editors.get(0));
	    sd = new JDialog(parent, "Edit Series: "+gle.getList().getGraphTitle(), true);
	    
	    sd.getContentPane().add(gle);
	    
	}
	else
	{
	    sd = new JDialog(parent, "Multiple Series", true);
	    
	    OwnerJTabbedPane tabs = new OwnerJTabbedPane(JTabbedPane.BOTTOM, editors);
	    
	    tabs.addChangeListener(new ChangeListener()
	    {
		/**
		 * Invoked when the target of the listener has changed its state.
		 *
		 * @param e  a ChangeEvent object
		 */
		public void stateChanged(ChangeEvent e)
		{
		    ((OwnerJTabbedPane)e.getSource()).notifyEditors();
		}
	    }
	    );
	    for(int i = 0; i < editors.size(); i++)
	    {
                System.out.println("in graphlisteditor "+i);
                System.out.println("editors.get.(i) " + editors.get(i).toString());
		GraphListEditor gle = (editors.get(i) instanceof GraphList) ?
		    ((GraphList)editors.get(i)).getEditor() :
		    ((GraphListEditor)editors.get(i));
		tabs.addTab(gle.getList().getGraphTitle(), gle);
	    }
	    
	    sd.getContentPane().add(tabs);
	}
	sd.getContentPane().setSize(200,200);
	
	JPanel closePanel = new JPanel();
	OwnerJButton closeWindow = new OwnerJButton("Close",sd, editors);
	closeWindow.addActionListener(new ActionListener()
	{
	    public void actionPerformed(ActionEvent e)
	    {
		((OwnerJButton)e.getSource()).closeOwner();
	    }
	});
	closePanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
	closePanel.add(closeWindow);
	
	sd.getContentPane().add(closePanel, BorderLayout.SOUTH);
	
	
	sd.pack();
	sd.setLocationRelativeTo(parent); // centre
	sd.show();
    }
    
    /**
     * Getter for property list.
     * @return Value of property list.
     */
    public chart.GraphList getList()
    {
	return list;
    }
    
}

class OwnerJButton extends JButton
{
    private JDialog owner;
    private ArrayList editors;
    
    public OwnerJButton(String str, JDialog owner, ArrayList editors)
    {
	super(str);
	this.owner = owner;
	this.editors= editors;
    }
    
    public void closeOwner()
    {
	for(int i = 0; i < editors.size(); i++)
	{
	    GraphListEditor gle = (editors.get(i) instanceof GraphList) ?
		((GraphList)editors.get(i)).getEditor() :
		((GraphListEditor)editors.get(i));
	    gle.stopEditing();
	}
	owner.setVisible(false);
    }
}

class OwnerJTabbedPane extends JTabbedPane
{
    private ArrayList editors;
    
    public OwnerJTabbedPane(int i, ArrayList editors)
    {
	super(i);
	this.editors = editors;
    }
    
    public void notifyEditors()
    {
	for(int i = 0; i < editors.size(); i++)
	{
	    GraphListEditor gle = (editors.get(i) instanceof GraphList) ?
		((GraphList)editors.get(i)).getEditor() :
		((GraphListEditor)editors.get(i));
	    gle.stopEditing();
	}
    }
}
