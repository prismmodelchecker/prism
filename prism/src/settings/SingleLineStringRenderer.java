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

public class SingleLineStringRenderer implements SettingRenderer 
{
    private JTextField field = new JTextField();
    private Font font = new Font("monospaced", Font.PLAIN, 12);
    private Font font2 = new Font("monospaced", Font.ITALIC, 12);
    
    
    public Component getTableCellRendererComponent(JTable table, Setting owner, Object value, boolean isSelected, boolean hasFocus, boolean isEnabled, int row, int column)
    {
        field.setMargin(new Insets(0, 2, 4, 2));
        if (isSelected)
        {
            field.setForeground(table.getSelectionForeground());
            field.setBackground(table.getSelectionBackground());
        }
        else
        {
            field.setForeground(table.getForeground());
            field.setBackground(table.getBackground());
        }
        
        if(hasFocus)
        {
            field.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
        }
        else
        {
            field.setBorder(new EmptyBorder(1, 2, 2, 1));
        }
        
        if(value instanceof String)
        {
            String str = (String)value;
            
            field.setText(str);
            
            field.setEnabled(isEnabled);
            field.setEditable(isEnabled);
            field.setCaretColor(isEnabled?Color.black:Color.white);
            
            field.setFont(font);
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
                    field.setText(last);
                    
                    field.setEnabled(isEnabled);
                    field.setEditable(isEnabled);
                    field.setCaretColor(isEnabled?Color.black:Color.white);
            
                    field.setFont(font);
                }
                else
                {
                    field.setText("(Different values)");
                    
                    field.setEnabled(isEnabled);
                    field.setEditable(isEnabled);
                    field.setCaretColor(isEnabled?Color.black:Color.white);
                    field.setBackground(Color.lightGray);
                    field.setFont(font2);
                }
                
            }
        }
        
        
        return field;
    }
	
}
