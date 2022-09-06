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
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.text.BadLocationException;
import java.util.*;

public class MultipleLineStringEditor implements SettingEditor, KeyListener, CaretListener, FocusListener
{
    private JTextArea area;
    private Font font;
    
    private JTable lastTable = null;
    private int tableRow = -1;
    private int tableCol = -1;
    
    private boolean valueGot = true;
    
    private boolean multiDifferent = false;
    
    public MultipleLineStringEditor()
    {
        area = new JTextArea();
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        font = new Font("monospaced", Font.PLAIN, 12);
        
        area.addCaretListener(this);
        area.addKeyListener(this);
        area.addFocusListener(this);
    }
    
    public Object getEditorValue()
    {
        if(multiDifferent && area.getText().equals(""))
        {
            valueGot = true;
            multiDifferent = false;
            return NOT_CHANGED_VALUE;
        }
        else
        {
            valueGot = true;
            return area.getText();
        }
    }
    
    public Component getTableCellEditorComponent(JTable table, Setting owner, Object value, boolean isSelected, int row, int column)
    {
        
        area.setMargin(new Insets(0, 2, 4, 2));
        if (isSelected)
        {
            //unselect if selected!
            table.getSelectionModel().setSelectionInterval(-1, -1);
            area.setForeground(table.getSelectionForeground());
            area.setBackground(table.getSelectionBackground());
        }
        else
        {
            area.setForeground(table.getForeground());
            area.setBackground(table.getBackground());
        }
        
        
        area.setBorder( UIManager.getBorder("Table.focusCellHighlightBorder") );
        multiDifferent = false;
        
        if(value instanceof String)
        {
            String str = (String)value;
            
            area.setText(str);
            
            area.setCaretColor(Color.black);
            
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
                    
                    area.setFont(font);
                }
                else
                {
                    area.setText("");
                    multiDifferent = true;
                }
                
            }
        }
        //sort out the height
        int heightWanted = (int)area.getPreferredSize().getHeight();
        heightWanted++; //this is a hack (for some reason the UIManager provides a border that is 1 pixel too small for the cell highlight!!)
        if(heightWanted != table.getRowHeight(row))
            table.setRowHeight(row, heightWanted);
        
        lastTable = table;
        tableRow = row;
        tableCol = column;
        valueGot = false;
        return area;
    }
    
    public void stopEditing()
    {
    }
    
    public void keyPressed(KeyEvent e)
    {
        
        try
        {
            int startOfLastLine = area.getLineStartOffset(area.getLineCount()-1);
            int endOfFirstLine = area.getLineEndOffset(0);
            int currentPosition = area.getCaretPosition();
            
            //override the behaviour of pressing the up key, if we are on the top row of the multiline text.
            if(e.getKeyCode() == KeyEvent.VK_UP)
            {
                if(currentPosition < endOfFirstLine)
                {
                    if(lastTable != null)
                    {
                        lastTable.editingStopped(new ChangeEvent(this));
                        
                        int newPosition = tableRow-1;
                        if(newPosition <0) newPosition = lastTable.getRowCount()-1;
                        
                        lastTable.getSelectionModel().setSelectionInterval(newPosition, newPosition);
                        
                    }
                }
            }
            //override the behaviour of pressing the down key, if we on the bottom row of multiline text
            else if(e.getKeyCode() == KeyEvent.VK_DOWN)
            {
                if(currentPosition >= startOfLastLine || area.getText().length() == 0)
                {
                    lastTable.editingStopped(new ChangeEvent(this));
                    
                    int newPosition = (tableRow+1)%lastTable.getRowCount();
                    
                    lastTable.getSelectionModel().setSelectionInterval(newPosition, newPosition);
                }
            }
            else if(e.getKeyCode() == KeyEvent.VK_ENTER)
            {
                
                //is the shift key being pressed?
                if((e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0)
                {
                    e.setModifiers(0); //consume the shift, so that it does not stop the editing
                }
                else
                {
                    lastTable.editingStopped(new ChangeEvent(this));
                    int newPosition = (tableRow+1)%lastTable.getRowCount();
                    
                    lastTable.getSelectionModel().setSelectionInterval(newPosition, newPosition);
                }
            }
        }
        catch(BadLocationException ex)
        {
            //ignore
        }
    }
    
    public void keyReleased(KeyEvent e)
    {
    }
    
    public void keyTyped(KeyEvent e)
    {
    }
    
    public void caretUpdate(CaretEvent e)
    {
        if(lastTable == null) return;
        area.getCaret().setVisible(true);
        int heightWanted = (int)area.getPreferredSize().getHeight();
        heightWanted++; //this is a hack (for some reason the UIManager provides a border that is 1 pixel too small for the cell highlight!!)
        if(heightWanted != lastTable.getRowHeight(tableRow))
            lastTable.setRowHeight(tableRow, heightWanted);
    }
    
    public void focusGained(FocusEvent e)
    {
    }
    
    public void focusLost(FocusEvent e)
    {
        if(!valueGot && lastTable != null)
        {
            if(multiDifferent && area.getText().equals(""))
            {
                valueGot = true;
                multiDifferent = false;
            }
            else
            {
                valueGot = true;
                lastTable.setValueAt(area.getText(), tableRow, tableCol);
            }
            if(lastTable.getCellEditor() != null) lastTable.removeEditor();
        }
    }
    
}
