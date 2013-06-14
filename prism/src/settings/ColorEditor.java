//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.ac.uk> (University of Birmingham)
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

package settings;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import java.util.*;

public class ColorEditor implements SettingEditor, ActionListener, FocusListener
{
	private JLabel renderer;
	private JPanel panel;
	private Font font = new Font("monospaced", Font.ITALIC, 12);
	private JPanel colourPanel, blank1, blank2;
	private JButton button;
	private JTable lastTable = null;
	private int tableRow = -1;
	private int tableCol = -1;

	private boolean dialogFocus = false;

	private boolean modified = false;

	public ColorEditor()
	{
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		renderer = new JLabel();

		colourPanel = new javax.swing.JPanel();
		renderer = new javax.swing.JLabel();
		blank2 = new javax.swing.JPanel();
		blank1 = new javax.swing.JPanel();
		button = new javax.swing.JButton("...");

		button.setFont(new Font("serif", Font.PLAIN, 7));

		panel.add(renderer, BorderLayout.CENTER);
		blank2.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		blank2.setPreferredSize(new Dimension(20, 2));

		colourPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, false));
		colourPanel.setPreferredSize(new Dimension(12, 12));

		blank2.add(colourPanel);
		panel.add(blank2, BorderLayout.WEST);

		blank1.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		blank1.setPreferredSize(new Dimension(18, 2));

		button.setPreferredSize(new Dimension(16, 12));
		blank1.add(button);
		panel.add(blank1, BorderLayout.EAST);

		button.addActionListener(this);

		button.addFocusListener(this);
	}

	public Object getEditorValue()
	{
		if (modified) {
			modified = false;
			return colourPanel.getBackground();
		} else {
			return NOT_CHANGED_VALUE;
		}
	}

	public Component getTableCellEditorComponent(JTable table, Setting owner, Object value, boolean isSelected, int row, int column)
	{
		if (isSelected) {
			renderer.setForeground(table.getSelectionForeground());
			panel.setBackground(table.getSelectionBackground());
			blank1.setBackground(table.getSelectionBackground());
			blank2.setBackground(table.getSelectionBackground());
			button.setBackground(table.getSelectionBackground());
		} else {
			renderer.setForeground(table.getForeground());
			panel.setBackground(table.getBackground());
			blank1.setBackground(table.getBackground());
			blank2.setBackground(table.getBackground());
			button.setBackground(table.getBackground());
		}

		panel.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));

		if (value instanceof Color) {
			Color col = (Color) value;

			//System.out.println("setting background to "+col.toString());

			renderer.setText("[" + col.getRed() + "," + col.getGreen() + "," + col.getBlue() + "]");

			renderer.setFont(font);

			colourPanel.setBackground(col);
		} else if (value instanceof ArrayList) {
			ArrayList<?> values = (ArrayList<?>) value;
			if (values.size() > 0) {
				//if we have multiple properties selected.
				Color last = null;
				boolean allSame = true;
				for (int i = 0; i < values.size(); i++) {
					if (values.get(i) instanceof Color) {
						Color str = (Color) values.get(i);
						if (last != null) {
							if (!str.equals(last)) {
								allSame = false;
								break;
							}
							last = str;
						} else {
							last = str;
						}
					}
				}
				if (allSame) {
					renderer.setText("[" + last.getRed() + "," + last.getGreen() + "," + last.getBlue() + "]");

					renderer.setFont(font);

					colourPanel.setBackground(last);
				} else {
					renderer.setText("(Different values)");

					renderer.setFont(font);

					colourPanel.setBackground(Color.lightGray);

				}

			}
		}
		lastTable = table;
		tableRow = row;
		tableCol = column;
		return panel;
	}

	public void stopEditing()
	{
	}

	public void actionPerformed(ActionEvent e)
	{
		dialogFocus = true;
		Color col = JColorChooser.showDialog(lastTable, "Choose Colour", colourPanel.getBackground());
		if (col != null) {
			colourPanel.setBackground(col);
			renderer.setText("[" + col.getRed() + "," + col.getGreen() + "," + col.getBlue() + "]");

			if (lastTable != null) {
				panel.setBackground(lastTable.getBackground());
				blank1.setBackground(lastTable.getBackground());
				blank2.setBackground(lastTable.getBackground());
			}

			if (dialogFocus) {
				dialogFocus = false;
				if (lastTable != null)
					lastTable.editingStopped(new ChangeEvent(this));
			} else //must have lost the focus during editing
			{
				if (lastTable != null) {
					lastTable.setValueAt(getEditorValue(), tableRow, tableCol);

				}
			}
			modified = true;
		}
		if (lastTable != null)
			lastTable.editingStopped(new ChangeEvent(this));
		//if(lastTable.getCellEditor() != null) lastTable.removeEditor();
	}

	public void focusGained(FocusEvent e)
	{
	}

	public void focusLost(FocusEvent e)
	{
		dialogFocus = false;
	}

}
