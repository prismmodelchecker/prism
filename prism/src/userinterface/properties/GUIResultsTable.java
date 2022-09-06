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

package userinterface.properties;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.util.*; 
import userinterface.*;

public class GUIResultsTable extends javax.swing.JDialog
{
	private static int noOpen = 0;
	
	private GUIMultiProperties guiProps;
	private GUIExperiment exp;
	
	private JPanel topPanel;
	private JButton closeButton;
	
	private JScrollPane scroller;
	private JTable table;
	private ResultsTableModel tableModel;
	
	/** Creates a new instance of GUIResultsTable */
	public GUIResultsTable(GUIPrism parent, GUIMultiProperties guiProps, GUIExperiment exp)
	{
		super(parent, "Experiment Results", false);
		this.guiProps = guiProps;
		this.exp = exp;
		
		//setup table
		tableModel = new ResultsTableModel(exp);
		table = new JTable();
		table.setModel(tableModel);
		table.setCellSelectionEnabled(true);
		table.setRowHeight(getFontMetrics(table.getFont()).getHeight() + 4);
		
		//initialise
		initComponents();
		getRootPane().setDefaultButton(closeButton);
		setResizable(true);
		pack();
		setLocationRelativeTo(getParent()); // centre
	}

	/** This method is called from within the constructor to initialize the form. */
	
	private void initComponents()
	{
		addWindowListener(new java.awt.event.WindowAdapter()
		{
			public void windowClosing(java.awt.event.WindowEvent evt)
			{
				closeDialog();
			}
		});
		
		JLabel topLabel = new JLabel("Results of \'" + exp.getPropertyString() + "\' for \'" + exp.getDefinedConstantsString() + "\'"); 
				
		JPanel tmpPanel = new JPanel();
		tmpPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		tmpPanel.setLayout(new BorderLayout(10, 10));
		tmpPanel.add(topLabel, BorderLayout.NORTH);
		tmpPanel.add(topLabel, BorderLayout.NORTH);
		
		topPanel = new JPanel();
		topPanel.setBorder(new TitledBorder("Results"));
		topPanel.setLayout(new BorderLayout());
		
		JPanel borderPanel = new JPanel();
		borderPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		borderPanel.setLayout(new BorderLayout());
		
		JScrollPane sp = new JScrollPane();
		sp.setViewportView(table);
		borderPanel.add(sp, BorderLayout.CENTER);
		topPanel.add(borderPanel);
		topPanel.setPreferredSize(new Dimension(500,200));
		
		tmpPanel.add(topPanel, BorderLayout.CENTER);
		
		JPanel tmpPanel2 = new JPanel();
		tmpPanel2.setLayout(new BorderLayout());
			
		closeButton = new JButton("Close");

		closeButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				closeDialog();
			}
		});
		
		tmpPanel2.add(closeButton, BorderLayout.EAST);		
		
		tmpPanel.add(tmpPanel2, BorderLayout.SOUTH);
		this.getContentPane().add(tmpPanel);
	}

	public void show()
	{
		noOpen++;
		setLocation(getX()+(noOpen*50), getY()+(noOpen*50));
		super.show();
	}
	
	public void dispose()
	{
		noOpen--;
		super.dispose();
	}
	
	private void closeDialog()
	{
		setVisible(false);
		dispose();
	}

	class ResultsTableModel extends AbstractTableModel
	{
		String headings[];
		ArrayList cells;
		int numRows, numCols;
		
		public ResultsTableModel(GUIExperiment exp)
		{
			headings = exp.getResults().getHeadingsArray();
			cells = exp.getResults().toArrayList();
			numRows = cells.size();
			numCols = headings.length;
			//prop = new ArrayList();
		}

		public String getColumnName(int j)
		{
			return headings[j];
		}

		public int getColumnCount()
		{
			return numCols;
		}

		public int getRowCount()
		{
			return numRows;
		}

		public Object getValueAt(int i, int j)
		{
			String[] line = (String[])cells.get(i);
			if (j<line.length) return line[j];
			return "?";
		}
	}
}
