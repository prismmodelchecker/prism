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
import java.util.*;

public class SingleLineStringEditor implements SettingEditor, CaretListener, FocusListener
{
    private JTextField field;
    private Font font = new Font("monospaced", Font.PLAIN, 12);
    
    private JTable lastTable = null;
    private int tableCol = -1;
    private int tableRow = -1;
    
    private boolean valueGot = true;
    
    private boolean multiDifferent = false;
    
    
    /** Creates a new instance of SingleLineStringEditor */
    public SingleLineStringEditor()
    {
        field = new JTextField();
        field.addCaretListener(this);
        field.addFocusListener(this);
    }
    
    public Object getEditorValue()
    {
        if(multiDifferent && field.getText().equals(""))
        {
            valueGot = true;
            multiDifferent =false;
            return NOT_CHANGED_VALUE;
        }
        else
        {
            valueGot = true;
            return field.getText();
        }
    }
    
    public Component getTableCellEditorComponent(JTable table, Setting owner, Object value, boolean isSelected, int row, int column)
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
        
        
        field.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
        
        multiDifferent = false;
        
        if(value instanceof String)
        {
            String str = (String)value;
            
            field.setText(str);

	    
            field.setCaretColor(Color.black);
            
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
                    
                    field.setFont(font);
                }
                else
                {
                    field.setText("");
                    field.setFont(font);
                    multiDifferent = true;
                }
                
            }
        }
        
        lastTable = table;
        tableRow = row;
        tableCol = column;
        valueGot = false;
        
        return field;
    }
    
    public void stopEditing()
    {
    	
    }
    
    public void caretUpdate(CaretEvent e)
    {
        field.getCaret().setVisible(true);
    }
    
    public void focusGained(FocusEvent e)
    {
    }
    
    public void focusLost(FocusEvent e)
    {
        if(!valueGot && lastTable != null)
        {
            if(multiDifferent && field.getText().equals(""))
            {
                valueGot = true;
                multiDifferent = false;
            }
            else
            {
                valueGot = true;
                lastTable.setValueAt(field.getText(), tableRow, tableCol);
            }
            if(lastTable.getCellEditor() != null) lastTable.removeEditor();
        }
        else
        {
            multiDifferent = false;
        }
    }
    
}
