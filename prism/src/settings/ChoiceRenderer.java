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

public class ChoiceRenderer implements SettingRenderer
{
    private JPanel panel;
    private JLabel renderer;
    private Font font = new Font("monospaced", Font.PLAIN, 12);
    private Font font2 = new Font("monospaced", Font.ITALIC, 12);
    
    public ChoiceRenderer()
    {
        panel = new JPanel();
        renderer = new JLabel();
        
        panel.setLayout(new BorderLayout());
        panel.add(renderer, BorderLayout.CENTER);
    }
    
    public Component getTableCellRendererComponent(JTable table, Setting owner, Object value, boolean isSelected, boolean hasFocus, boolean isEnabled, int row, int column)
    {
        //renderer.setMargin(new Insets(0, 2, 4, 2));
        
        if (isSelected)
        {
            renderer.setForeground(table.getSelectionForeground());
            renderer.setBackground(table.getSelectionBackground());
            panel.setBackground(table.getSelectionBackground());
        }
        else
        {
            renderer.setForeground(table.getForeground());
            renderer.setBackground(table.getBackground());
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
        
        if(value instanceof String)
        {
            String str = (String)value;
            
            renderer.setText(str);

	    renderer.setEnabled(isEnabled);
            
            renderer.setFont(font);
            
            
        }
        else if(value instanceof ArrayList)
        {
			ArrayList<?> values = (ArrayList<?>)value;
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
                    
                    renderer.setText(last);

                    renderer.setEnabled(isEnabled);
            
                    renderer.setFont(font);
                }
                else
                {
                    
                    renderer.setText("(Different Values)");

                    renderer.setEnabled(isEnabled);
            
                    renderer.setFont(font2);
                    
                    panel.setBackground(Color.lightGray);
                    
                   
                    
                }
                
            }
        }
        
        return panel;
    }
    
}
