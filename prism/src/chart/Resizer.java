//==============================================================================
//	
//	Copyright (c) 2002-
//	Authors:
//	* Andrew Hinton <ug60axh@cs.bham.uc.uk> (University of Birmingham)
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

package chart;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.*;
import settings.*;

public class Resizer extends Rectangle2D.Double implements MouseListener, MouseMotionListener
{
    public static final int HORIZONTAL_RESIZE = 0;
    public static final int VERTICAL_RESIZE = 1;
    public static final int BOTH_RESIZE = 2;
    
    private static ArrayList sizers;
    
    static
    {
        sizers = new ArrayList();
    }
    
    public static void deSelectAll()
    {
        for(int i = 0 ; i < sizers.size(); i++)
        ((Resizer)sizers.get(i)).setSelected(false);
    }
    
    
    private boolean selected;
    private ChartObject owner;
    private int style;
    
    private boolean shiftDown = false;
    
    /** Creates a new instance of Resizers */
    public Resizer(double x, double y, double size, ChartObject owner, int style)
    {
        super.x = x-(size/2.0);
        super.y = y-(size/2.0);
        super.width = size;
        super.height = size;
        selected = false;
        this.owner = owner;
        this.style = style;
        
        sizers.add(this);
    }
    
    public void render(Graphics2D g2)
    {
        g2.setColor(Color.white);
        g2.fill(this);
        g2.setColor(Color.black);
        g2.draw(this);
    }
    
    public boolean isSelected()
    {
        return selected;
    }
    
    public void updatePosition(double x, double y)
    {
        super.x = x-(super.width/2.0);
        super.y = y-(super.height/2.0);
    }
    
    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }
    
    public void setShiftDown(boolean shiftDown)
    {
        this.shiftDown = shiftDown;
    }
    
    public void mouseClicked(MouseEvent e)
    {
    }
    
    public void mouseDragged(MouseEvent e)
    {
        if(selected)
        {
	    try
	    {
            switch(style)
            {
                case Resizer.HORIZONTAL_RESIZE:
                    owner.resize(this, e.getX(), super.y);
                    break;
                case Resizer.VERTICAL_RESIZE:
                    owner.resize(this, super.x, e.getY());
                    break;
                case Resizer.BOTH_RESIZE:
                    owner.resize(this, e.getX(), e.getY());
            }
	    }
	    catch(SettingException eee)
	    { // do nothing
	    }
        }
    }
    
    public void mouseEntered(MouseEvent e)
    {
    }
    
    public void mouseExited(MouseEvent e)
    {
    }
    
    public void mouseMoved(MouseEvent e)
    {
    }
    
    public void mousePressed(MouseEvent e)
    {
        Rectangle2D.Double click = new Rectangle2D.Double(e.getX()-2, e.getY()-2, 4, 4);
        
        if(click.intersects(this))
        {
            
            setSelected(true);
        }
    }
    
    public void mouseReleased(MouseEvent e)
    {
    }
    
}
