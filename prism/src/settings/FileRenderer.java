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
import javax.swing.border.*;
import java.util.*;
import java.io.*;

import userinterface.GUIPrism;

public class FileRenderer implements SettingRenderer
{
	private JLabel renderer;
	private JPanel panel;
	private ImageIcon warningIcon = GUIPrism.getIconFromImage("smallError.png");

	public FileRenderer()
	{
		panel = new JPanel(new BorderLayout());
		renderer = new JLabel();
		panel.add(renderer, BorderLayout.CENTER);
	}

	public Component getTableCellRendererComponent(JTable table, Setting owner, Object value, boolean isSelected, boolean hasFocus, boolean isEnabled, int row,
			int column)
	{
		//renderer.setMargin(new Insets(0, 2, 4, 2));

		if (isSelected) {
			renderer.setForeground(table.getSelectionForeground());
			panel.setBackground(table.getSelectionBackground());
		} else {
			renderer.setForeground(table.getForeground());
			panel.setBackground(table.getBackground());
		}

		if (hasFocus) {
			renderer.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		} else {
			renderer.setBorder(new EmptyBorder(1, 2, 2, 1));
		}

		if (value instanceof File) {
			File file = (File) value;
			renderer.setText(file.toString());
			renderer.setEnabled(isEnabled);
			if (owner instanceof FileSetting) {
				FileSetting fset = (FileSetting) owner;
				if (!fset.isValidFile()) {
					renderer.setIcon(warningIcon);
				} else {
					renderer.setIcon(null);
				}
			}
		} else if (value instanceof ArrayList) {
			ArrayList<?> values = (ArrayList<?>) value;
			if (values.size() > 0) {
				//if we have multiple properties selected.
				File last = null;
				boolean allSame = true;
				for (int i = 0; i < values.size(); i++) {
					if (values.get(i) instanceof File) {
						File str = (File) values.get(i);
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
					File file = last;
					renderer.setText(file.toString());
					renderer.setEnabled(isEnabled);
					if (owner instanceof FileSetting) {
						FileSetting fset = (FileSetting) owner;
						if (!fset.isValidFile()) {
							renderer.setIcon(warningIcon);
						} else {
							renderer.setIcon(null);
						}
					}
				} else {
					renderer.setText("(Different values)");
					renderer.setEnabled(isEnabled);
					renderer.setBackground(Color.lightGray);
					panel.setBackground(Color.lightGray);
				}

			}
		}

		//sort out the height

		return panel;
	}

}
