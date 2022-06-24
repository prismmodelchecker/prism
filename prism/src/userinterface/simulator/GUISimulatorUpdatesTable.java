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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.AbstractListModel;
import javax.swing.ActionMap;
import javax.swing.ComponentInputMap;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;

import userinterface.GUIPrism;

@SuppressWarnings("serial")
public class GUISimulatorUpdatesTable extends JTable implements ListSelectionListener
{
	public static Color[] DISTRIBUTION_COLOURS = {
		new Color(255, 255, 255), // white
		new Color(253, 255, 201) // yellow
	};
	public static Color[] DISABLED_COLOURS = {
		new Color(210, 210, 210), // pale grey
		new Color(190, 190, 190) // pale-ish grey
	};
	
	private GUISimulator.UpdateTableModel utm;

	private UpdateHeaderListModel headerModel;
	private JList<String> header;
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
		JList<String> rowHeader = new JList<>(headerModel);

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

	public JList<String> getUpdateRowHeader()
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
			// Pick colour for row
			Color bgCol;
			// Enabled
			if (!sim.isStrategyShown() || utm.isEnabledByStrategy(row)) {
				// Colours alternate for nondet models
				if (utm.isNondetModel()) {
					bgCol = DISTRIBUTION_COLOURS[utm.getChoiceIndexOf(row) % 2];
				} else {
					bgCol = DISTRIBUTION_COLOURS[0];
				}
			}
			// Disabled
			else {
				// Colours alternate for nondet models
				if (utm.isNondetModel()) {
					bgCol = DISABLED_COLOURS[utm.getChoiceIndexOf(row) % 2];
				} else {
					bgCol = DISABLED_COLOURS[0];
				}
			}
			// Darker (blue) shade if row is selected
			if (isSelected) {
				bgCol = new Color(bgCol.getRed() - 20, bgCol.getGreen() - 20, bgCol.getBlue());
			}
			// Slightly darker if an old update
			if (utm.oldUpdate) {
				bgCol = new Color(bgCol.getRed() - 7, bgCol.getGreen() - 7, bgCol.getBlue() - 7);
			}
			renderer.setText(value == null ? "" : value.toString());
			renderer.setBackground(bgCol);
			renderer.setBorder(new EmptyBorder(1, 1, 1, 1));
			return renderer;
		}

		public void setFont(Font font)
		{
			renderer.setFont(font);
		}
	}

	class UpdateHeaderRenderer extends JButton implements ListCellRenderer<String>
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

		@Override
		public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus)
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

	class UpdateHeaderListModel extends AbstractListModel<String>
	{
		public String getElementAt(int index)
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
		}
	}
}
