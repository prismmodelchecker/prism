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

import java.util.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

public class MultipleLineStringRenderer implements SettingRenderer
{
    private JTextArea area = new JTextArea();
    private Font font = new Font("monospaced", Font.PLAIN, 12);
    private Font font2 = new Font("monospaced", Font.ITALIC, 12);
    
    
    public Component getTableCellRendererComponent(JTable table, Setting owner, Object value, boolean isSelected, boolean hasFocus, boolean isEnabled, int row, int column)
    {
        area.setMargin(new Insets(0, 2, 4, 2));
        if (isSelected)
        {
            area.setForeground(table.getSelectionForeground());
            area.setBackground(table.getSelectionBackground());
        }
        else
        {
            area.setForeground(table.getForeground());
            area.setBackground(table.getBackground());
        }
        
        if(hasFocus)
        {
            area.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
        }
        else
        {
            area.setBorder(new EmptyBorder(1, 2, 2, 1));
        }
        
        if(value instanceof String)
        {
            String str = (String)value;
            
            area.setText(str);

	    area.setEnabled(isEnabled);
            area.setEditable(isEnabled);
            area.setCaretColor(isEnabled?Color.black:Color.white);
            
            area.setFont(font);
            
        }
        else if(value instanceof ArrayList)
        {
            ArrayList values = (ArrayList)value;
            if(values.size() > 0)
            {
                //if we have multiple properties selected.
                String last = null;
                boolean allSame = true;
                for(int i = 0; i < values.size(); i++)
                {
                    if(values.get(i) instanceof String)
                    {
                        String str = (String)values.get(i);
                        if(last != null)
                        {
                            if(!str.equals(last))
                            {
                                allSame = false; break;
                            }
                            last = str;
                        }
                        else
                        {
                            last = str;
                        }
                    }
                }
                if(allSame)
                {
                    area.setText(last);
                    
                    area.setEnabled(isEnabled);
                    area.setEditable(isEnabled);
                    area.setCaretColor(isEnabled?Color.black:Color.white);
            
                    area.setFont(font);
                }
                else
                {
                    area.setText("(Different values)");
                    
                    area.setEnabled(isEnabled);
                    area.setEditable(isEnabled);
                    area.setCaretColor(isEnabled?Color.black:Color.white);
                    area.setBackground(Color.lightGray);
                    area.setFont(font2);
                }
                
            }
        }
        //sort out the height
        int heightWanted = (int)area.getPreferredSize().getHeight();
        if(hasFocus) heightWanted++; //this is a hack (for some reason the UIManager provides a border that is 1 pixel too small for the cell highlight!!)
        if(heightWanted != table.getRowHeight(row))
            table.setRowHeight(row, heightWanted);
        
        return area;
    }
    
}
