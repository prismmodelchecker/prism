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

public class TableResizer extends MouseInputAdapter
{
    
    
    private int col;
    private boolean colSel;
    private boolean colSelectionAllowed;
    private int startWidth;
    private int startX;
    private JTable table;
    private boolean active;
    private boolean rowSelectionAllowed;
    private int row;
    private int startY;
    private int startHeight;
    private static final int PIXELS = 5;
    private Cursor lastCursor;
    private static Cursor S_resizeCursor = Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR);
    private static Cursor E_resizeCursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
    
    
    
    public TableResizer(JTable table)
    {
        this.table = table;
        this.table.addMouseListener(this);
        this.table.addMouseMotionListener(this);
        this.row = -1;
        this.col = -1;
    }
    
    
    public void done()
    {
        if (this.table == null) return;
        this.table.removeMouseListener(this);
        this.table.removeMouseMotionListener(this);
    }
    
    
    public void mouseMoved(MouseEvent e)
    {
        Point p = e.getPoint();
        if (this.isMouseOverColMargin(p))
        {
            if (this.lastCursor == null)
            {
                this.lastCursor = this.table.getCursor();
            }
            this.table.setCursor(E_resizeCursor);
        } 
        else
        {
            this.table.setCursor(this.lastCursor);
        }
    }
    
    public void mousePressed(MouseEvent e)
    {
        Point p = e.getPoint();
        if (this.isMouseOverColMargin(p))
        {
            this.active = true;
            this.startX = p.x;
            //this.startWidth = table.getWidth();
            startWidth = this.table.getColumnModel().getColumn(col).getWidth();
            this.colSelectionAllowed = this.table.getColumnSelectionAllowed();
            this.table.setColumnSelectionAllowed(false);
            colSel = true;
        }
    }
    
    public void mouseDragged(MouseEvent e)
    {
        Point p = e.getPoint();
        if (!active) return;
        if (colSel)
        {
            int newWidth = e.getX();
            newWidth = Math.max(10, newWidth);
            
            if(newWidth > table.getWidth()-10) newWidth = table.getWidth()-10;
            this.table.getColumnModel().getColumn(col).setMinWidth(newWidth);
            this.table.getColumnModel().getColumn(col).setMaxWidth(newWidth);
            this.table.getColumnModel().getColumn(col).setPreferredWidth(newWidth);
        }
    }
    
    public void mouseReleased(MouseEvent e)
    {
        if (!active) return;
        if (colSel)
        {
            this.table.setColumnSelectionAllowed(this.colSelectionAllowed);
            this.col = -1;
            colSel = false;
        }
        this.active = false;
        
    }
    
    private boolean isMouseOverColMargin(Point p)
    {
        if (!table.isEnabled()) return false;
        this.row = table.rowAtPoint(p);
        this.col = table.columnAtPoint(p);
        if (row == -1 || col == -1) return false;
        Rectangle r = table.getCellRect(row, col, true);
        if (p.x >= r.x + r.width - PIXELS)
        {
            return true;
        }
        return false;
    }
    
    public static void main(String[]args)
    {
        JFrame frame = new JFrame("Test");
        
        String [][] data = { {"a", "b"}, {"c", "d"}, {"e","f"}};
        String [] columns = { "1", "2"};
        
        JTable tab = new JTable(data, columns);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(tab, BorderLayout.CENTER);
        
        tab.setPreferredSize(new Dimension(200,200));
        
        TableResizer lll = new TableResizer(tab);
        
        tab.addMouseListener(lll);
        tab.addMouseMotionListener(lll);
        
        tab.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        
        frame.pack();
        frame.show();
    }
}
