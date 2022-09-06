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

public class FontColorEditor implements SettingEditor, ActionListener, FocusListener
{
	private JLabel renderer;
	private JPanel panel;
	private Font font = new Font("monospaced", Font.ITALIC, 12);
	private JButton button;
	private JTable lastTable = null;
	private int tableRow = -1;
	private int tableCol = -1;
	private JPanel blank1;

	private boolean dialogFocus = false;

	private boolean modified = false;

	public FontColorEditor()
	{
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		renderer = new JLabel();

		renderer = new javax.swing.JLabel();
		blank1 = new javax.swing.JPanel();
		button = new javax.swing.JButton("...");

		button.setFont(new Font("serif", Font.PLAIN, 7));

		panel.add(renderer, BorderLayout.CENTER);

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
			return new FontColorPair(renderer.getFont(), renderer.getForeground());
		} else
			return NOT_CHANGED_VALUE;
	}

	public Component getTableCellEditorComponent(JTable table, Setting owner, Object value, boolean isSelected, int row, int column)
	{
		if (isSelected) {
			renderer.setForeground(table.getSelectionForeground());
			renderer.setBackground(table.getSelectionBackground());
			panel.setBackground(table.getSelectionBackground());
			blank1.setBackground(table.getSelectionBackground());
			button.setBackground(table.getSelectionBackground());
		} else {
			renderer.setForeground(table.getForeground());
			renderer.setBackground(table.getBackground());
			panel.setBackground(table.getBackground());
			blank1.setBackground(table.getBackground());
			button.setBackground(table.getBackground());
		}

		panel.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));

		if (value instanceof FontColorPair) {
			FontColorPair fcp = (FontColorPair) value;

			renderer.setForeground(fcp.c);

			renderer.setText(fcp.f.getName() + " " + fcp.f.getSize());

			renderer.setFont(fcp.f);

			int heightWanted = Math.max(table.getRowHeight(), fcp.f.getSize() + 3);
			//if(hasFocus) heightWanted++; //this is a hack (for some reason the UIManager provides a border that is 1 pixel too small for the cell highlight!!)
			if (heightWanted != table.getRowHeight(row))
				table.setRowHeight(row, heightWanted);
		} else if (value instanceof ArrayList) {
			ArrayList<?> values = (ArrayList<?>) value;
			if (values.size() > 0) {
				//if we have multiple properties selected.
				FontColorPair last = null;
				boolean allSame = true;
				for (int i = 0; i < values.size(); i++) {
					if (values.get(i) instanceof FontColorPair) {
						FontColorPair str = (FontColorPair) values.get(i);
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
					renderer.setForeground(last.c);

					renderer.setText(last.f.getName() + " " + last.f.getSize());

					renderer.setFont(last.f);

					int heightWanted = Math.max(table.getRowHeight(), last.f.getSize() + 3);
					//if(hasFocus) heightWanted++; //this is a hack (for some reason the UIManager provides a border that is 1 pixel too small for the cell highlight!!)
					if (heightWanted != table.getRowHeight(row))
						table.setRowHeight(row, heightWanted);
				} else {
					renderer.setText("(Different values)");

					renderer.setFont(new Font("monospaced", Font.ITALIC, 12));

					int heightWanted = Math.max(table.getRowHeight(), last.f.getSize() + 3);
					//if(hasFocus) heightWanted++; //this is a hack (for some reason the UIManager provides a border that is 1 pixel too small for the cell highlight!!)
					if (heightWanted != table.getRowHeight(row))
						table.setRowHeight(row, heightWanted);

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
		FontColorPair fcp = FontChooser.getFont(new Frame(), renderer.getFont(), renderer.getForeground(), font, Color.black);
		if (fcp != null) {
			renderer.setForeground(fcp.c);

			renderer.setText(fcp.f.getName() + " " + fcp.f.getSize());

			renderer.setFont(fcp.f);

			if (dialogFocus) {
				dialogFocus = false;
				if (lastTable != null)
					lastTable.editingStopped(new ChangeEvent(this));
			} else //must have lost the focus during editing
			{
				if (lastTable != null) {
					lastTable.setValueAt(getEditorValue(), tableRow, tableCol);
					int heightWanted = Math.max(lastTable.getRowHeight(), fcp.f.getSize() + 3);
					//if(hasFocus) heightWanted++; //this is a hack (for some reason the UIManager provides a border that is 1 pixel too small for the cell highlight!!)
					if (heightWanted != lastTable.getRowHeight(tableRow))
						lastTable.setRowHeight(tableRow, heightWanted);
				}
			}
			modified = true;
		}
		if (lastTable != null)
			lastTable.editingStopped(new ChangeEvent(this));
	}

	public void focusGained(FocusEvent e)
	{
	}

	public void focusLost(FocusEvent e)
	{
		dialogFocus = false;
	}

}
