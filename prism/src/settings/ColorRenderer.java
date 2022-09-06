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

public class ColorRenderer implements SettingRenderer
{
	private JLabel renderer;
	private JPanel panel;
	private Font font = new Font("monospaced", Font.ITALIC, 12);
	private JPanel colourPanel, blank1, blank2;

	public ColorRenderer()
	{
		panel = new JPanel();
		panel.setLayout(new BorderLayout());
		renderer = new JLabel();

		colourPanel = new javax.swing.JPanel();
		renderer = new javax.swing.JLabel();
		blank2 = new javax.swing.JPanel();
		blank1 = new javax.swing.JPanel();

		panel.add(renderer, BorderLayout.CENTER);
		blank2.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
		blank2.setPreferredSize(new Dimension(20, 2));

		colourPanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 1, false));
		colourPanel.setPreferredSize(new Dimension(12, 12));

		blank2.add(colourPanel);
		panel.add(blank2, BorderLayout.WEST);
	}

	public Component getTableCellRendererComponent(JTable table, Setting owner, Object value, boolean isSelected, boolean hasFocus, boolean isEnabled, int row,
			int column)
	{
		//renderer.setMargin(new Insets(0, 2, 4, 2));

		if (isSelected) {
			renderer.setForeground(table.getSelectionForeground());
			panel.setBackground(table.getSelectionBackground());
			blank1.setBackground(table.getSelectionBackground());
			blank2.setBackground(table.getSelectionBackground());
		} else {
			renderer.setForeground(table.getForeground());
			panel.setBackground(table.getBackground());
			blank1.setBackground(table.getBackground());
			blank2.setBackground(table.getBackground());
		}

		if (hasFocus) {
			panel.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		} else {
			panel.setBorder(new EmptyBorder(1, 2, 2, 1));
		}

		if (value instanceof Color) {
			Color col = (Color) value;

			//System.out.println("setting background to "+col.toString());

			renderer.setText("[" + col.getRed() + "," + col.getGreen() + "," + col.getBlue() + "]");

			renderer.setEnabled(isEnabled);

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

					renderer.setEnabled(isEnabled);

					renderer.setFont(font);

					colourPanel.setBackground(last);
				} else {
					renderer.setText("(Different values)");

					renderer.setEnabled(isEnabled);

					renderer.setFont(font);

					colourPanel.setBackground(Color.lightGray);
					panel.setBackground(Color.lightGray);
					blank1.setBackground(Color.lightGray);
					blank2.setBackground(Color.lightGray);

				}

			}
		}

		return panel;
	}

}
