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
import javax.swing.event.*;
import java.awt.event.*;
import javax.swing.border.*;
import java.util.*;

public class ChoiceEditor implements SettingEditor, ActionListener, FocusListener
{
	private Font font = new Font("monospaced", Font.PLAIN, 12);
	private String[] choices;
	private JComboBox combo;
	private JTable lastTable;
	private boolean modified = false;

	/** Creates a new instance of ChoiceEditor */
	public ChoiceEditor(String[] choices)
	{
		combo = new JComboBox(choices);
		combo.addActionListener(this);
		combo.addFocusListener(this);
		this.choices = choices;
	}

	public Object getEditorValue() throws SettingException
	{
		if (modified) {
			modified = false;
			if (combo.getSelectedItem() != null)
				return combo.getSelectedItem().toString();
			else
				return NOT_CHANGED_VALUE;
		} else
			return NOT_CHANGED_VALUE;
	}

	public Component getTableCellEditorComponent(JTable table, Setting owner, Object value, boolean isSelected, int row, int column)
	{

		if (isSelected) {
			combo.setForeground(table.getSelectionForeground());
			combo.setBackground(table.getSelectionBackground());
		} else {
			combo.setForeground(table.getForeground());
			combo.setBackground(table.getBackground());
		}

		combo.setBorder(new EmptyBorder(0, 0, 0, 0));//UIManager.getBorder("Table.focusCellHighlightBorder") );
		combo.setFont(font);
		combo.setFocusable(false);

		if (value instanceof String) {
			int index = -1;
			for (int i = 0; i < choices.length; i++) {
				if (choices[i].equals(value.toString())) {
					index = i;
					break;
				}
			}
			combo.setSelectedIndex(index);
		} else if (value instanceof ArrayList) {
			ArrayList<?> values = (ArrayList<?>) value;
			if (values.size() > 0) {
				//if we have multiple properties selected.
				String last = null;
				boolean allSame = true;
				for (int i = 0; i < values.size(); i++) {
					if (values.get(i) instanceof String) {
						String str = (String) values.get(i);
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

					int index = -1;
					for (int i = 0; i < choices.length; i++) {
						if (choices[i].equals(last)) {
							index = i;
							break;
						}
					}
					combo.setSelectedIndex(index);
				} else {
					combo.setSelectedIndex(-1);

				}

			}
		}

		lastTable = table;
		return combo;
	}

	public void stopEditing()
	{
	}

	public void actionPerformed(ActionEvent e)
	{
		modified = true;
		if (lastTable != null)
			lastTable.editingStopped(new ChangeEvent(this));
	}

	public void focusGained(FocusEvent e)
	{
	}

	public void focusLost(FocusEvent e)
	{
		if (lastTable.getCellEditor() != null)
			lastTable.removeEditor();
	}

}
