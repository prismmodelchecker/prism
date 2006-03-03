//==============================================================================
//
//	Copyright (c) 2005, Andrew Hinton
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

package chart;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import java.util.*;
import settings.*;

/**
 *
 * @author  Andrew Hinton
 */
public class SeriesDataRenderer implements SettingRenderer
{
    private JLabel renderer;
    private JPanel panel;
    private ImageIcon icon;
    
    public SeriesDataRenderer()
    {
	panel = new JPanel(new BorderLayout());
	renderer = new JLabel();
	panel.add(renderer, BorderLayout.CENTER);
	//icon = new ImageIcon(ClassLoader.getSystemResource("images/gridSnap.gif"));
    }
    
    public Component getTableCellRendererComponent(JTable table, Setting owner, Object value, boolean isSelected, boolean hasFocus, boolean isEnabled, int row, int column)
    {
	//renderer.setMargin(new Insets(0, 2, 4, 2));
	
	if (isSelected)
	{
	    renderer.setForeground(table.getSelectionForeground());
	    panel.setBackground(table.getSelectionBackground());
	}
	else
	{
	    renderer.setForeground(table.getForeground());
	    panel.setBackground(table.getBackground());
	}
	
	if(hasFocus)
	{
	    renderer.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
	}
	else
	{
	    renderer.setBorder(new EmptyBorder(1, 2, 2, 1));
	}
	
	if(value instanceof GraphList)
	{
	    
	    renderer.setText("Series Data");
	    renderer.setIcon(icon);
	    
	    renderer.setEnabled(isEnabled);
	    
	}
	else if(value instanceof ArrayList)
	{
	    
	    renderer.setText("(Multiple Series)");
	    renderer.setIcon(icon);
	    renderer.setEnabled(isEnabled);
	    
	    renderer.setFont(new Font("monospaced", Font.ITALIC, 12));
	    
	    renderer.setBackground(Color.lightGray);
	    panel.setBackground(Color.lightGray);
	    
	}
	
	//sort out the height
	
	
	return panel;
    }
  
	
}
