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

public class FontColorRenderer implements SettingRenderer
{
	private JLabel renderer;
	private JPanel panel;

	public FontColorRenderer()
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

		if (value instanceof FontColorPair) {
			FontColorPair fcp = (FontColorPair) value;

			renderer.setForeground(fcp.c);

			renderer.setText(fcp.f.getName() + " " + fcp.f.getSize());

			renderer.setEnabled(isEnabled);

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

					renderer.setEnabled(isEnabled);

					renderer.setFont(last.f);

					int heightWanted = Math.max(table.getRowHeight(), last.f.getSize() + 3);
					//if(hasFocus) heightWanted++; //this is a hack (for some reason the UIManager provides a border that is 1 pixel too small for the cell highlight!!)
					if (heightWanted != table.getRowHeight(row))
						table.setRowHeight(row, heightWanted);
				} else {
					renderer.setText("(Different values)");

					renderer.setEnabled(isEnabled);

					renderer.setFont(new Font("monospaced", Font.ITALIC, 12));

					renderer.setBackground(Color.lightGray);
					panel.setBackground(Color.lightGray);

					int heightWanted = Math.max(table.getRowHeight(), last.f.getSize() + 3);
					//if(hasFocus) heightWanted++; //this is a hack (for some reason the UIManager provides a border that is 1 pixel too small for the cell highlight!!)
					if (heightWanted != table.getRowHeight(row))
						table.setRowHeight(row, heightWanted);

				}

			}
		}

		//sort out the height

		return panel;
	}

}
