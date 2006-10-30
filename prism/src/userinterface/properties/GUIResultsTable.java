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

package userinterface.properties;

import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.util.*; 
import parser.*;
import prism.*;
import userinterface.*;

public class GUIResultsTable extends javax.swing.JDialog
{
	private GUIMultiProperties guiProps;
	private GUIExperiment exp;
	
	private javax.swing.JPanel topPanel;
	
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
		java.awt.GridBagConstraints gridBagConstraints;
		javax.swing.JLabel jLabel1;
		javax.swing.JPanel jPanel1;
		javax.swing.JPanel jPanel2;
		javax.swing.JPanel jPanel3;
		javax.swing.JPanel jPanel4;
		javax.swing.JPanel jPanel5;
		javax.swing.JPanel jPanel6;
		javax.swing.JButton closeButton;
		
		jPanel1 = new javax.swing.JPanel();
		jPanel2 = new javax.swing.JPanel();
		jPanel3 = new javax.swing.JPanel();
		jPanel4 = new javax.swing.JPanel();
		jPanel5 = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		topPanel = new javax.swing.JPanel();
		jPanel6 = new javax.swing.JPanel();
		closeButton = new javax.swing.JButton();
		
		addWindowListener(new java.awt.event.WindowAdapter()
		{
			public void windowClosing(java.awt.event.WindowEvent evt)
			{
				closeDialog();
			}
		});
		
		jPanel1.setLayout(new java.awt.GridBagLayout());
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		jPanel1.add(jPanel2, gridBagConstraints);
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		jPanel1.add(jPanel3, gridBagConstraints);
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 2;
		jPanel1.add(jPanel4, gridBagConstraints);
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 4;
		jPanel1.add(jPanel5, gridBagConstraints);
		
		jLabel1.setText("Results of \"" + exp.getPropertyString() + "\" for " + exp.getDefinedConstantsString());
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
		jPanel1.add(jLabel1, gridBagConstraints);
		
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 3;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		jPanel1.add(topPanel, gridBagConstraints);
		
		getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);
		
		jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
		
		closeButton.setText("Close");
		closeButton.addActionListener(new java.awt.event.ActionListener()
		{
			public void actionPerformed(java.awt.event.ActionEvent evt)
			{
				closeDialog();
			}
		});
		
		jPanel6.add(closeButton);
		
		topPanel.setBorder(new TitledBorder("Results"));
		topPanel.setLayout(new BorderLayout());
		JScrollPane sp = new JScrollPane();
		sp.setViewportView(table);
		topPanel.add(sp);
		topPanel.setPreferredSize(new Dimension(500,200));
		
		getContentPane().add(jPanel6, java.awt.BorderLayout.SOUTH);
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
