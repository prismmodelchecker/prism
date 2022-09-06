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

package userinterface.simulator;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.border.*;
import javax.swing.event.*;

import userinterface.GUIPrism;

@SuppressWarnings("serial")
public class GUISimulatorUpdatesTable extends JTable implements ListSelectionListener
{
	public static Color[] DISTRIBUTION_COLOURS = { new Color(255, 255, 255), //white
			new Color(253, 255, 201) }; //yellow
	/*new Color(224,255,224),     //green
	new Color(255,227,255),     //pink
	new Color(255,234,199),     //orange
	new Color(209,217,255),     //blue
	new Color(226,199,255),     //purple
	new Color(212,255,255)} ;*///cyan
	private GUISimulator.UpdateTableModel utm;

	private UpdateHeaderListModel headerModel;
	private JList header;
	private UpdateHeaderRenderer updateHeaderRenderer;
	private UpdateTableRenderer updateTableRenderer;

	private GUISimulator sim;

	/** Creates a new instance of GUISimulatorUpdatesTable */
	public GUISimulatorUpdatesTable(GUISimulator.UpdateTableModel utm, GUISimulator sim)
	{
		super(utm);
		this.sim = sim;
		this.utm = utm;

		this.getSelectionModel().addListSelectionListener(this);

		setColumnSelectionAllowed(false);
		getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		headerModel = new UpdateHeaderListModel();
		JList rowHeader = new JList(headerModel);

		rowHeader.setBackground(new JPanel().getBackground());

		rowHeader.setFixedCellWidth(15);

		rowHeader.setFixedCellHeight(getRowHeight());
		//+ getRowMargin());
		//getIntercellSpacing().height);
		updateHeaderRenderer = new UpdateHeaderRenderer(this);
		rowHeader.setCellRenderer(updateHeaderRenderer);

		this.header = rowHeader;

		updateTableRenderer = new UpdateTableRenderer();
		setDefaultRenderer(Object.class, updateTableRenderer);

		setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		InputMap inputMap = new ComponentInputMap(this);

		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "Down");
		inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "Up");

		ActionMap actionMap = new ActionMap();

		actionMap.put("Down", new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				int selectedRow = GUISimulatorUpdatesTable.this.getSelectedRow();
				if (selectedRow != -1) {
					if (selectedRow < GUISimulatorUpdatesTable.this.getRowCount() - 1)
						GUISimulatorUpdatesTable.this.getSelectionModel().setSelectionInterval(selectedRow + 1, selectedRow + 1);
					else
						GUISimulatorUpdatesTable.this.getSelectionModel().setSelectionInterval(0, 0);
				}
			}
		});

		actionMap.put("Up", new AbstractAction()
		{
			public void actionPerformed(ActionEvent e)
			{
				int selectedRow = GUISimulatorUpdatesTable.this.getSelectedRow();
				if (selectedRow != -1) {
					if (selectedRow >= 1)
						GUISimulatorUpdatesTable.this.getSelectionModel().setSelectionInterval(selectedRow - 1, selectedRow - 1);
					else
						GUISimulatorUpdatesTable.this.getSelectionModel().setSelectionInterval(GUISimulatorUpdatesTable.this.getRowCount() - 1,
								GUISimulatorUpdatesTable.this.getRowCount() - 1);
				}
			}
		});

		this.setInputMap(JComponent.WHEN_FOCUSED, inputMap);
		this.setActionMap(actionMap);

	}

	/** Override set font to pass changes onto renderer(s) and set row height */
	public void setFont(Font font)
	{
		super.setFont(font);
		if (updateTableRenderer != null)
			updateTableRenderer.setFont(font);
		setRowHeight(getFontMetrics(font).getHeight() + 4);
		if (header != null)
			header.setFixedCellHeight(getRowHeight());
	}

	public void valueChanged(ListSelectionEvent e)
	{
		if (headerModel != null)
			headerModel.updateHeader();
		repaint();
	}

	public JList getUpdateRowHeader()
	{
		return header;
	}

	class UpdateTableRenderer implements TableCellRenderer
	{
		JTextField renderer;

		public UpdateTableRenderer()
		{
			renderer = new JTextField("");
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			renderer.setText(value == null ? "" : value.toString());

			int dist;

			// Select default background colour
			// (depends on choice, for nondeterministic models)
			if (sim.getModulesFile().getModelType().nondeterministic())
				dist = utm.getChoiceIndexOf(row);
			else
				dist = 0;
			Color c = DISTRIBUTION_COLOURS[dist % 2];

			if (isSelected) {
				Color newCol = new Color(c.getRed() - 20, c.getGreen() - 20, c.getBlue());
				if (utm.oldUpdate) {
					newCol = new Color(newCol.getRed() - 7, newCol.getGreen() - 7, newCol.getBlue() - 7);
					renderer.setBackground(newCol);
				} else {
					renderer.setBackground(newCol);
				}
			} else {
				if (utm.oldUpdate) {
					Color newCol = new Color(c.getRed() - 7, c.getGreen() - 7, c.getBlue() - 7);
					renderer.setBackground(newCol);
				} else
					renderer.setBackground(c);
			}

			renderer.setBorder(new EmptyBorder(1, 1, 1, 1));
			return renderer;
		}

		public void setFont(Font font)
		{
			renderer.setFont(font);
		}
	}

	class UpdateHeaderRenderer extends JButton implements ListCellRenderer
	{

		ImageIcon selectedIcon;
		ImageIcon selectedDisabledIcon;

		UpdateHeaderRenderer(JTable table)
		{
			/*JTableHeader header = table.getTableHeader();
			 setOpaque(true);
			 setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			 setHorizontalAlignment(CENTER);
			 setForeground(header.getForeground());
			 setBackground(header.getBackground());
			 setFont(header.getFont());*/
			setBorder(null);
			selectedIcon = GUIPrism.getIconFromImage("smallItemSelected.png");
			selectedDisabledIcon = GUIPrism.getIconFromImage("smallItemSelectedDisabled.png");
		}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
		{
			setBorder(null);
			if (getSelectedRow() == index) {
				if (GUISimulatorUpdatesTable.this.isEnabled())
					setIcon(selectedIcon);
				else
					setIcon(selectedDisabledIcon);
			} else {
				setIcon(null);
			}
			return this;
		}

	}

	class UpdateHeaderListModel extends AbstractListModel
	{

		public Object getElementAt(int index)
		{
			return "" + index;
		}

		public int getSize()
		{
			return utm.getRowCount();
		}

		public void updateHeader()
		{
			fireContentsChanged(this, 0, utm.getRowCount());

			//System.out.println("The tables width is "+getWidth());
		}

	}
}
