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

public class BooleanRenderer implements SettingRenderer
{
	private JCheckBox renderer;
	private JPanel panel;

	public BooleanRenderer()
	{
		panel = new JPanel();
		//FlowLayout flow = new FlowLayout(FlowLayout.CENTER);
		//flow.setHgap(0);
		panel.setLayout(new BorderLayout());//new GridBagLayout());//flow);//new FlowLayout(FlowLayout.CENTER));
		renderer = new JCheckBox();
		renderer.setPreferredSize(new Dimension(17, 12));
		panel.add(renderer);
	}

	public Component getTableCellRendererComponent(JTable table, Setting owner, Object value, boolean isSelected, boolean hasFocus, boolean isEnabled, int row,
			int column)
	{
		//renderer.setMargin(new Insets(0, 2, 4, 2));
		if (isSelected) {
			panel.setBackground(table.getSelectionBackground());
			renderer.setBackground(table.getSelectionBackground());
		} else {
			panel.setBackground(table.getBackground());
			renderer.setBackground(table.getBackground());
		}

		if (hasFocus) {
			panel.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
		} else {
			panel.setBorder(new EmptyBorder(1, 2, 2, 1));
		}

		if (value instanceof Boolean) {
			Boolean boo = (Boolean) value;

			renderer.setSelected(boo.booleanValue());

			renderer.setEnabled(isEnabled);
		} else if (value instanceof ArrayList) {
			ArrayList<?> values = (ArrayList<?>) value;
			if (values.size() > 0) {
				//if we have multiple properties selected.
				Boolean last = null;
				boolean allSame = true;
				for (int i = 0; i < values.size(); i++) {
					if (values.get(i) instanceof Boolean) {
						Boolean str = (Boolean) values.get(i);
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
					renderer.setSelected(last.booleanValue());
				} else {
					renderer.setSelected(false);
					renderer.setBackground(Color.lightGray);
					panel.setBackground(Color.lightGray);

				}

			}
		}
		renderer.setOpaque(true);

		return panel;

	}

}
