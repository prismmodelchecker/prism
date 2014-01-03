//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Dave Parker <d.a.parker@cs.bham.ac.uk> (University of Birmingham/Oxford)
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

import java.awt.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

public class LongRenderer implements SettingRenderer
{
    private JTextField renderer = new JTextField();
    private Font font = new Font("monospaced", Font.PLAIN, 12);
    private Font font2 = new Font("monospaced", Font.ITALIC, 12);
    
    
    public Component getTableCellRendererComponent(JTable table, Setting owner, Object value, boolean isSelected, boolean hasFocus, boolean isEnabled, int row, int column)
    {
        renderer.setMargin(new Insets(0, 2, 4, 2));
        if (isSelected)
        {
            renderer.setForeground(table.getSelectionForeground());
            renderer.setBackground(table.getSelectionBackground());
        }
        else
        {
            renderer.setForeground(table.getForeground());
            renderer.setBackground(table.getBackground());
        }
        
        if(hasFocus)
        {
            renderer.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
        }
        else
        {
            renderer.setBorder(new EmptyBorder(1, 2, 2, 1));
        }
        
        if(value instanceof Long)
        {
            Long dob = (Long)value;
            
            renderer.setText(""+dob.longValue());

	    renderer.setEnabled(isEnabled);
            
            renderer.setFont(font);
        }
        else if(value instanceof ArrayList)
        {
            ArrayList values = (ArrayList)value;
            if(values.size() > 0)
            {
                //if we have multiple properties selected.
                Long last = null;
                boolean allSame = true;
                for(int i = 0; i < values.size(); i++)
                {
                    if(values.get(i) instanceof Long)
                    {
                        Long str = (Long)values.get(i);
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
                    renderer.setText(""+last.longValue());

                    renderer.setEnabled(isEnabled);
            
                    renderer.setFont(font);
                }
                else
                {
                    renderer.setText("(Different values)");
                    
                    renderer.setEnabled(isEnabled);
            
                    renderer.setFont(font2);
                    
                    renderer.setBackground(Color.lightGray);
                }
                
            }
        }
        
        
        return renderer;
    }
    
}
