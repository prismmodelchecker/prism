//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
//	* Dave Parker <david.parker@comlab.ox.ac.uk> (University of Oxford, formerly University of Birmingham)
//	* Mark Kattenbelt <mark.kattenbelt@comlab.ox.ac.uk> (University of Oxford)
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

package userinterface.graph;

import javax.swing.*;

import java.awt.*;

import javax.swing.table.*;

import java.util.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;

import userinterface.*;
import org.jfree.data.xy.*;
import org.jfree.data.general.*;

public class SeriesEditorDialog extends JDialog
{         
	//ATTRIBUTES    
	private Action okAction;
	private Action cancelAction;
	private GUIPrism gui;
	private java.util.List<SeriesEditor> editors;
	
	private boolean cancelled;
		
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel allPanel;
    private javax.swing.JPanel bottomPanel;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JPanel editorPanel;
    private javax.swing.JButton okayButton;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JToolBar toolBar;
    // End of variables declaration//GEN-END:variables
    
    private GUIPlugin plugin;
    
    public static void makeSeriesEditor(GUIPlugin plugin, JFrame parent, Graph graph, java.util.List<Graph.SeriesKey> series)
    {
    	if (graph.getXAxisSettings().isLogarithmic() || graph.getYAxisSettings().isLogarithmic())
    	{
    		plugin.message(
    			"One of your axes has a logarithmic scale. When a logarithmic scale is active we temporarily \n" +
    			"hide negative and zero values. For this reason it is not safe to edit values when either of \n" +
    			"your axes is logarithmic. Please select numerical axes and switch back later.");
    		return;
    	}
    	
    	synchronized (graph.getSeriesLock())
    	{
    		SeriesEditorDialog editor = new SeriesEditorDialog(plugin, parent, graph, series);
    		editor.setVisible(true);
    		
    		// DOESN'T CONTINUE UNTILL DISPOSED
    	}
    }
    
	/** Creates new form GUIConstantsPicker */
	private SeriesEditorDialog(GUIPlugin plugin, JFrame parent, Graph graph, java.util.List<Graph.SeriesKey> series)
	{
		super(parent, "Graph Series Editor", true);
     
		this.plugin = plugin;
		this.editors = new ArrayList<SeriesEditor>();
		
		initComponents();
		
		AbstractAction cut = new AbstractAction()
		{
		    public void actionPerformed(ActionEvent e)
		    {
				editors.get(tabbedPane.getSelectedIndex()).cut();	
		    }
		};
		cut.putValue(Action.LONG_DESCRIPTION, "Cut the current selection to the clipboard");
		//exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit");
		cut.putValue(Action.NAME, "Cut");
		cut.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallCut.png"));
		//cut.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		
		AbstractAction copy = new AbstractAction()
		{
		    public void actionPerformed(ActionEvent e)
		    {
		    	editors.get(tabbedPane.getSelectedIndex()).copy();	
		    }
		};
		copy.putValue(Action.LONG_DESCRIPTION, "Copies the current selection to the clipboard");
		//exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit");
		copy.putValue(Action.NAME, "Copy");
		copy.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallCopy.png"));
		//copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		
		AbstractAction paste = new AbstractAction()
		{
		    public void actionPerformed(ActionEvent e)
		    {
		    	editors.get(tabbedPane.getSelectedIndex()).paste();		
		    }
		};
		paste.putValue(Action.LONG_DESCRIPTION, "Pastes the clipboard to the current selection");
		//exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit");
		paste.putValue(Action.NAME, "Paste");
		paste.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallPaste.png"));
		//paste.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
		
		AbstractAction delete = new AbstractAction()
		{
		    public void actionPerformed(ActionEvent e)
		    {
		    	editors.get(tabbedPane.getSelectedIndex()).delete();			
		    }
		};
		delete.putValue(Action.LONG_DESCRIPTION, "Deletes the current");
		//exitAction.putValue(Action.SHORT_DESCRIPTION, "Exit");
		delete.putValue(Action.NAME, "Delete");
		delete.putValue(Action.SMALL_ICON, GUIPrism.getIconFromImage("smallDelete.png"));
		
		
		for (Graph.SeriesKey key : series)
		{
			SeriesSettings settings = graph.getGraphSeries(key);
			PrismXYSeries xySeries = (PrismXYSeries)graph.getXYSeries(key);
			
			SeriesEditor editor = new SeriesEditor(graph, xySeries, settings, cut, copy, paste, delete);
			editor.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			
			tabbedPane.addTab(settings.getSeriesHeading(), editor);
			editors.add(editor);
		}		
		
		this.getRootPane().setDefaultButton(okayButton);
		
				
		toolBar.add(cut);
		toolBar.add(copy);
		toolBar.add(paste);
		toolBar.add(delete);
		
		
		this.add(toolBar, BorderLayout.NORTH);
		
		this.cancelled = false;					
		
		super.setBounds(new Rectangle(550, 300));
		setResizable(true);
		setLocationRelativeTo(getParent()); // centre
	}
    
	/** This method is called from within the constructor to
	 * initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is
	 * always regenerated by the Form Editor.
	 */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        allPanel = new javax.swing.JPanel();
        bottomPanel = new javax.swing.JPanel();
        buttonPanel = new javax.swing.JPanel();
        okayButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        editorPanel = new javax.swing.JPanel();
        tabbedPane = new javax.swing.JTabbedPane();
        toolBar = new javax.swing.JToolBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Series Data Editor");
        setAlwaysOnTop(true);
        setMinimumSize(new java.awt.Dimension(550, 350));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                closeDialog(evt);
            }
        });

        allPanel.setLayout(new java.awt.BorderLayout());

        allPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bottomPanel.setLayout(new java.awt.BorderLayout());

        buttonPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        okayButton.setText("Okay");
        okayButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okayButtonActionPerformed(evt);
            }
        });

        buttonPanel.add(okayButton);

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        buttonPanel.add(cancelButton);

        bottomPanel.add(buttonPanel, java.awt.BorderLayout.EAST);

        allPanel.add(bottomPanel, java.awt.BorderLayout.SOUTH);

        editorPanel.setLayout(new java.awt.BorderLayout());

        tabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);
        editorPanel.add(tabbedPane, java.awt.BorderLayout.CENTER);

        allPanel.add(editorPanel, java.awt.BorderLayout.CENTER);

        toolBar.setFloatable(false);
        allPanel.add(toolBar, java.awt.BorderLayout.NORTH);

        getContentPane().add(allPanel, java.awt.BorderLayout.CENTER);

    }// </editor-fold>//GEN-END:initComponents

	private void okayButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_okayButtonActionPerformed
	{//GEN-HEADEREND:event_okayButtonActionPerformed
		dispose();
	}//GEN-LAST:event_okayButtonActionPerformed
        
	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt)//GEN-FIRST:event_cancelButtonActionPerformed
	{//GEN-HEADEREND:event_cancelButtonActionPerformed
		
		dispose();
	}//GEN-LAST:event_cancelButtonActionPerformed
        
	/** Closes the dialog */
	private void closeDialog(java.awt.event.WindowEvent evt)//GEN-FIRST:event_closeDialog
	{
		setVisible(false);
		dispose();
	}//GEN-LAST:event_closeDialog
		
	public boolean isCancelled() 
	{
		return cancelled;
	}
	
	/** 
	 * This class represents the data of a single series. Multiple of these classes are combined
	 * in one SeriesEditorDialog.
	 */	
	private class SeriesEditor extends JPanel  implements ActionListener
	{
		private int bufferSize = 30;
		private java.util.List<Double> xAxisBuffer;
		private java.util.List<Double> yAxisBuffer;
		
		private Graph graph;
		private SeriesSettings settings;
		private PrismXYSeries xySeries;
		
		private AbstractTableModel tableModel;
		private JTable table;
		
		private SeriesEditor(Graph graph, PrismXYSeries xySeries, SeriesSettings settings, Action cut, Action copy, Action paste, Action delete)
		{
			super.setLayout(new BorderLayout());
			
			this.graph = graph;
			this.settings = settings;
			this.xySeries = xySeries;
			
			this.xAxisBuffer = new ArrayList<Double>(bufferSize);
			this.yAxisBuffer = new ArrayList<Double>(bufferSize);
			
			for (int b = 0; b < bufferSize; b++)
			{
				xAxisBuffer.add(null);
				yAxisBuffer.add(null);				
			}
			
			this.xySeries.addChangeListener(new SeriesChangeListener() 
			{
				public void seriesChanged(SeriesChangeEvent event) 
				{
					SeriesEditor.this.tableModel.fireTableStructureChanged();
				}				
			});
			
			this.tableModel = new AbstractTableModel()
			{
				public int getColumnCount() { return 2; }
				public int getRowCount() { return SeriesEditor.this.xySeries.getItemCount() + bufferSize; }
				public boolean isCellEditable(int rowIndex, int columnIndex) { return true; }
				
				public Object getValueAt(int rowIndex, int columnIndex) 
				{
					if (rowIndex >= SeriesEditor.this.xySeries.getItemCount())
					{
						int bufferIndex = rowIndex - SeriesEditor.this.xySeries.getItemCount();
						
						Double bufferValue = (columnIndex == 0) ? xAxisBuffer.get(bufferIndex) : yAxisBuffer.get(bufferIndex);
						
						return (bufferValue == null) ? "" : bufferValue;
					}
										
					XYDataItem dataItem = SeriesEditor.this.xySeries.getDataItem(rowIndex);
					
					if (columnIndex == 0)
						return dataItem.getX();
					else
						return dataItem.getY();
				}			
				
				public String getColumnName(int column) 
				{
					if (column == 0)
						return SeriesEditor.this.graph.getXAxisSettings().getHeading();
					else
						return SeriesEditor.this.graph.getYAxisSettings().getHeading();

				}
				
				public void setValueAt(Object aValue, int rowIndex, int columnIndex) 
				{
					Double value = Double.NaN;
					
					try
					{
						value = Double.parseDouble(aValue.toString());
					}
					catch (NumberFormatException e) 
					{
						// Remains NaN
					}
					
					if (aValue.toString().trim().equals(""))
						value = null;
					
					/* If not in current graph. */
					if (rowIndex >= SeriesEditor.this.xySeries.getItemCount())
					{
						int bufferIndex = rowIndex - SeriesEditor.this.xySeries.getItemCount();
						
						// Set buffer
						if (columnIndex == 0)
							xAxisBuffer.set(bufferIndex, value);
						else
							yAxisBuffer.set(bufferIndex, value);
						
						Double otherBufferValue = (columnIndex == 0) ? yAxisBuffer.get(bufferIndex) : xAxisBuffer.get(bufferIndex);
						
						/* If row is filled in, then lets go! */
						if (value != null && otherBufferValue != null)
						{
							if (columnIndex == 0 && Double.isNaN(value) || columnIndex == 1 && otherBufferValue.isNaN())
							{
								// Cannot add yet! 
							}
							else
							{
								clearBufferRow(bufferIndex);
								
								if (columnIndex == 0)
									SeriesEditor.this.xySeries.addOrUpdate(new Double(value), otherBufferValue);
								else
									SeriesEditor.this.xySeries.addOrUpdate(otherBufferValue, new Double(value));
							}
						}
					}
					// Updating graph points...
					else
					{				
						XYDataItem dataItem = SeriesEditor.this.xySeries.getDataItem(rowIndex);
						
						// Null values are for in the buffer only. 
						if (value == null)
							value = Double.NaN;
						
						// Updating point on x-axis
						if (columnIndex == 0)
						{
							if (Double.isNaN(value))
							{
								Object[] options = {"Yes", "No"};
								if (SeriesEditorDialog.this.plugin.question(
										"Invalid value", "You have entered an invalid value on the x-axis. This \n" +
										"will result in deleting the datapoint. Do you want to continue?", options, 1) == 1)
									return;
							}
							
							Double yValue = SeriesEditor.this.xySeries.getY(rowIndex).doubleValue();
							SeriesEditor.this.xySeries.remove(rowIndex);
							SeriesEditor.this.xySeries.addOrUpdate(new Double(value), yValue);							
						}
											
						else
						{
							//	Updating point on y-axis
							SeriesEditor.this.xySeries.updateByIndex(rowIndex, value);						
						}
						
						//super.setValueAt(aValue, rowIndex, columnIndex);
					}
						
					fireTableStructureChanged();
				}
			};
			
			this.table = new JTable(tableModel);			
			
			//Next 3 lines thanks to
			//http://forum.java.sun.com/thread.jsp?thread=529548&forum=57&message=2546795
			//This is to disable to automatic Ctrl-C put onto JTables, as it does
			//not provide the correct functionality.
			InputMap im =  this.table.getInputMap();
			ActionMap am = this.table.getActionMap();
			
			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "cut");
			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "copy");
			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()), "paste");
			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
			
			am.put("cut", cut);
			am.put("copy", copy);
			am.put("paste", paste);
			am.put("delete", delete);			
			
			this.table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
			this.table.setRowSelectionAllowed(true);
			//this.table.setColumnSelectionAllowed(true);
			
			JScrollPane scroll = new JScrollPane();
			scroll.setViewportView(table);
			
			add(scroll, BorderLayout.CENTER);	
		}
		
		public void addBufferRow()
		{	
			xAxisBuffer.add(null);
			yAxisBuffer.add(null);
			
			bufferSize++;
			
			tableModel.fireTableStructureChanged();
		}
		
		public boolean isBufferRow(int rowIndex)
		{	
			return (rowIndex >= xySeries.getItemCount());
		}
		
		public boolean isClearBufferRow(int rowIndex)
		{	
			int bufferRowIndex = rowIndex - xySeries.getItemCount();
			return isBufferRow(rowIndex) &&  xAxisBuffer.get(bufferRowIndex) == null && yAxisBuffer.get(bufferRowIndex) == null;
		}
		
		public void clearBufferRow(int rowIndex)
		{	
			xAxisBuffer.set(rowIndex, null);
			yAxisBuffer.set(rowIndex, null);
			
			tableModel.fireTableStructureChanged();
		}
		
		/**
		 * Returns row index in terms of table.
		 */
		public int firstClearBufferIndex()
		{
			for (int b = 0; b < bufferSize; b++)
			{
				if (isClearBufferRow(b + xySeries.getItemCount()))
					return b + xySeries.getItemCount();
			}
			
			/* No clear buffer rows, lets add new row. */
			addBufferRow();
			return bufferSize + xySeries.getItemCount() - 1;			
		}
		
		public void cut()
		{
			copy();
			delete();
		}
		
		public void copy()
		{
			int[] rows = table.getSelectedRows();
			
			StringBuffer clippy = new StringBuffer();
		    for(int i = 0; i < rows.length ; i++)
		    {
		    	int row = rows[i];
		    	
		    	if (row < xySeries.getItemCount())
		    	{
		    		XYDataItem item = xySeries.getDataItem(row);
		    		clippy.append(item.getX()+"\t"+item.getY()+"\n");
		    	}
		    	else
		    	{
		    		int bufferRow = row - xySeries.getItemCount();
		    		
		    		String x = (xAxisBuffer.get(bufferRow) == null) ? "" : xAxisBuffer.get(bufferRow).toString();
		    		String y = (yAxisBuffer.get(bufferRow) == null) ? "" : yAxisBuffer.get(bufferRow).toString();
		    		
		    		clippy.append(x + "\t" + y + "\n");
		    	}
		    }
			
		    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		    StringSelection gs = new StringSelection(clippy.toString());
		    clipboard.setContents(gs, null);
		}
		
		public void paste()
		{
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable contents = clipboard.getContents(null);
			
			int[] sel = table.getSelectedRows();
			
			int row = -1; 
			
			/* If first selected is a buffer row, then paste from there onwards. */
			if (sel.length > 0 && isBufferRow(sel[0]))
				row = sel[0];
			
			try
			{
			    if(contents.isDataFlavorSupported(DataFlavor.stringFlavor))
			    {
					String str = (String) contents.getTransferData(DataFlavor.stringFlavor);					
					StringTokenizer rows = new StringTokenizer(str, "\n");
										
					while(rows.hasMoreTokens())
					{
						String rowStr = rows.nextToken();
						int tabIndex = rowStr.indexOf("\t");
						
						if (tabIndex != -1)
						{
							String xValue = rowStr.substring(0, tabIndex).trim();
							String yValue = rowStr.substring(tabIndex, rowStr.length()).trim();
					    	
							int bufferRow = (row == -1) ? firstClearBufferIndex() : row;
					    	
					    	tableModel.setValueAt("", bufferRow, 0);
					    	tableModel.setValueAt("", bufferRow, 1);
					    						    	
						    tableModel.setValueAt(xValue, bufferRow, 0);						    
						    tableModel.setValueAt(yValue, bufferRow, 1);
						    
						    if (row != -1)
						    {
						    	row++;
						    	if (row >= tableModel.getRowCount())
						    		addBufferRow();
						    }
						}
					}
			    }
			}
			catch(Exception e)
			{
			    
			}
		}
		
		public void delete()
		{
			int[] selectedRows = table.getSelectedRows();
			
			for (int row = selectedRows.length -1; row >= 0; row--)
			{
				int rowIndex = selectedRows[row];
				
				if (rowIndex >= xySeries.getItemCount())
				{
					clearBufferRow(rowIndex - xySeries.getItemCount());
				}
				else
				{
					xySeries.remove(rowIndex);
				}
			}
		}

		public SeriesSettings getSettings() 
		{
			return settings;
		}

		public void setSettings(SeriesSettings settings) 
		{
			this.settings = settings;
		}

		public PrismXYSeries getXySeries() 
		{
			return xySeries;
		}

		public void setXySeries(PrismXYSeries xySeries) 
		{
			this.xySeries = xySeries;
		}

		public void actionPerformed(ActionEvent e) 
		{
			System.out.println(e);			
			if (e.getActionCommand().equals("cut"))
				cut();
			else if (e.getActionCommand().equals("copy"))
				copy();
			else if (e.getActionCommand().equals("paste"))
				paste();
			else if (e.getActionCommand().equals("delete"))
				delete();
		}
	}
}


